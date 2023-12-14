/*
 * Mars Simulation Project
 * Researcher.java
 * @date 2022-10-05
 * @author Manny Kung
 */

package com.mars_sim.core.moon.project;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.mars_sim.core.logging.SimLogger;
import com.mars_sim.core.moon.Colonist;
import com.mars_sim.core.moon.Colony;
import com.mars_sim.core.person.PersonBuilderImpl;
import com.mars_sim.core.person.ai.SkillManager;
import com.mars_sim.core.person.ai.SkillOwner;
import com.mars_sim.core.person.ai.SkillType;
import com.mars_sim.core.science.ResearcherInterface;
import com.mars_sim.core.science.ScienceType;
import com.mars_sim.core.science.ScientificStudy;
import com.mars_sim.core.time.ClockPulse;
import com.mars_sim.core.time.Temporal;
import com.mars_sim.mapdata.location.Coordinates;
import com.mars_sim.tools.util.RandomUtil;

public class Researcher extends Colonist implements ResearcherInterface, Serializable, Temporal, SkillOwner {
	
	/** default serial id. */
	private static final long serialVersionUID = 1L;
	
	/** default logger. */
	private static final SimLogger logger = SimLogger.getLogger(Researcher.class.getName());
	
	private int age;
	
	private int numResearch = 0;
	
	private double experience = 0;
	
	private double activeness = 10;
	
	private double performance = 100;
	
	private String name;
	
	/** The person's current scientific study. */
	private ScientificStudy study;
	
	private Colony colony;
	
	private ScienceType mainScienceType;
	/** The researcher's skill manager. */
	private SkillManager skillManager;
	
	/** The person's achievement in scientific fields. */
	private Map<ScienceType, Double> scientificAchievement = new ConcurrentHashMap<>();
	/** The person's list of collaborative scientific studies. */
	private Set<ScientificStudy> collabStudies;
	/** A set of research projects this researcher engage in. */
	private Set<ResearchProject> researchProjects = new HashSet<>();
	
	public Researcher(String name, Colony colony) {
		super(name, colony);
		this.name = name;
		this.colony = colony;
		this.age = RandomUtil.getRandomInt(18, 70);
		
		// Determine the main science type
		mainScienceType = ScienceType.getRandomScienceType();
		// Construct the SkillManager instance
		skillManager = new SkillManager(this);
		// Determine skills
		determineSkills();
		
		experience = getTotalSkillExperience();
	}
    
	/**
	 * Creates a research project.
	 */
	public void createProject() {
		numResearch++;
		ResearchProject proj = new ResearchProject(this, mainScienceType.getName() + numResearch, mainScienceType);
		colony.addResearchProject(proj);
		researchProjects.add(proj);
	}
	
	public void joinProject() {
		ResearchProject proj = colony.getOneResearchProject(this);
		if (proj != null && proj.canAddParticipants()) {
			numResearch++;
			proj.addParticipant(this);
			researchProjects.add(proj);
		}
	}
	
	@Override
	public boolean timePassing(ClockPulse pulse) {
		int num = researchProjects.size();

		int numResearchers = colony.getPopulation().getNumResearchers();
		
		int numResearchProjects = colony.getNumResearchProjects();
		
		double aveProjPerResearcher = 1.0 * numResearchProjects / (.5 + numResearchers);
		
		double motivation = RandomUtil.getRandomDouble(pulse.getElapsed() / (1 + num));
		
		addActiveness(motivation);
		
		if (RandomUtil.getRandomDouble(100) <= activeness) {

			int rand = RandomUtil.getRandomInt((int)(5 + aveProjPerResearcher));
		
			// Limit the number of research projects that can be carried out by each researcher
			if (rand == 0 && num < 4) {
				createProject();
				activeness = 0;
			}
			else {
				joinProject();
				activeness = 0;
			}
		}
		
		if (pulse.isNewHalfSol()) {
			// Update the experience once every half sol
			experience = getTotalSkillExperience();
			logger.info(colony.getName() + " " + name + " experience: " + Math.round(experience * 100.0)/100.0);

			double timeValue = pulse.getElapsed() / 100;
			double expertiseValue = Math.log10(1 + experience) * activeness / (1 + num);
			double resourceValue = getResearchArea() / numResearchProjects;
			double compositeValue = timeValue * expertiseValue * resourceValue; 
					
			for (ResearchProject p: researchProjects) {
				if (p.getLead().equals(this)) {
					double value = RandomUtil.getRandomDouble(compositeValue);
					p.addResearchValue(value);
				}
				else {
					int numParticipants = p.getNumParticipants();
					double value = RandomUtil.getRandomDouble(compositeValue / numParticipants);
					p.addResearchValue(value);
				}
			}
		}
	
		return true;
	}

	private double getResearchArea() {
		return colony.getResearchArea();
	}
	
	/**
	 * Determines random skills for this researcher.
	 */
	private void determineSkills() {

		// Add starting skills randomly for a person.
		for (SkillType startingSkill : SkillType.values()) {
			int skillLevel = -1;

			switch (startingSkill) {
				case PILOTING: 
					// Checks to see if a person has a pilot license/certification
					skillLevel = PersonBuilderImpl.getInitialSkillLevel(0, 35);
					break;
			
				// Medicine skill is highly needed for diagnosing sickness and prescribing medication 
				case MEDICINE:
					skillLevel = PersonBuilderImpl.getInitialSkillLevel(0, 35);
					break;

				// psychology skill is sought after for living in confined environment
				case PSYCHOLOGY: 
					skillLevel = PersonBuilderImpl.getInitialSkillLevel(0, 35);
					break;
	
				// Mechanics skill is sought after for repairing malfunctions
				case MATERIALS_SCIENCE:
				case MECHANICS:
					skillLevel = PersonBuilderImpl.getInitialSkillLevel(0, 45);
					break;

				default: {
					int rand = RandomUtil.getRandomInt(0, 3);
					if (rand == 0) {
						skillLevel = PersonBuilderImpl.getInitialSkillLevel(0, (int)(10 + age/10.0));
					}
					else if (rand == 1) {
						skillLevel = PersonBuilderImpl.getInitialSkillLevel(1, (int)(5 + age/8.0));
					}
					else if (rand == 2) {
						skillLevel = PersonBuilderImpl.getInitialSkillLevel(2, (int)(2.5 + age/6.0));
					}
				} break;
			}

			// If a initial skill level then add it and assign experience
			if (skillLevel >= 0) {
				int exp = RandomUtil.getRandomInt(0, 24);
				skillManager.addNewSkill(startingSkill, skillLevel);
				skillManager.addExperience(startingSkill, exp, 0D);
			}
		}
	}
	
	
	public double getTotalSkillExperience() {
		return skillManager.getTotalSkillExperiences();
	}
	
	public void addActiveness(double value) {
		if (activeness + value > 100) {
			activeness = 100;
		}
		else {
			activeness += value;
		}
	}
	
	public double getActiveness() {
		return activeness;
	}
	
	public void setColony(Colony newColony) {
		colony = newColony;
	}
	
	/**
	 * Sets the study that this researcher is the lead on.
	 * 
	 * @param scientificStudy
	 */
	@Override
	public void setStudy(ScientificStudy scientificStudy) {
		this.study = scientificStudy;
	}

	
	/**
	 * Gets the scientific study instance.		
	 */
	@Override
	public ScientificStudy getStudy() {
		return study;
	}

	/**
	 * Gets the collaborative study sets.
	 */
	@Override
	public Set<ScientificStudy> getCollabStudies() {
		return collabStudies;
	}
	
	/**
	 * Adds the collaborative study.
	 * 
	 * @param study
	 */
	@Override
	public void addCollabStudy(ScientificStudy study) {
		this.collabStudies.add(study);
	}

	/**
	 * Removes the collaborative study.
	 * 
	 * @param study
	 */
	@Override
	public void removeCollabStudy(ScientificStudy study) {
		this.collabStudies.remove(study);
	}

	/**
	 * Gets the researcher's achievement credit for a given scientific field.
	 *
	 * @param science the scientific field.
	 * @return achievement credit.
	 */
	@Override
	public double getScientificAchievement(ScienceType science) {
		double result = 0D;
		if (science == null)
			return result;
		if (scientificAchievement.containsKey(science)) {
			result = scientificAchievement.get(science);
		}
		return result;
	}

	/**
	 * Gets the researcher's total scientific achievement credit.
	 *
	 * @return achievement credit.
	 */
	@Override
	public double getTotalScientificAchievement() {
		double result = 0d;
		for (double value : scientificAchievement.values()) {
			result += value;
		}
		return result;
	}

	/**
	 * Adds achievement credit to the researcher in a scientific field.
	 *
	 * @param achievementCredit the achievement credit.
	 * @param science           the scientific field.
	 */
	@Override
	public void addScientificAchievement(double achievementCredit, ScienceType science) {
		if (scientificAchievement.containsKey(science)) {
			achievementCredit += scientificAchievement.get(science);
		}
		scientificAchievement.put(science, achievementCredit);
	}

	@Override
	public SkillManager getSkillManager() {
		return skillManager;
	}

	@Override
	public Coordinates getCoordinates() {
		if (colony != null) {
			return colony.getCoordinates();
		}
		return null;
	}

	@Override
	public double getPerformanceRating() {
		return performance;
	}

}
