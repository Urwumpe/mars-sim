/*
 * Mars Simulation Project
 * ProposeScientificStudy.java
 * @date 2022-07-18
 * @author Scott Davis
 */
package com.mars_sim.core.person.ai.task;

import java.util.logging.Level;

import com.mars_sim.core.logging.SimLogger;
import com.mars_sim.core.person.Person;
import com.mars_sim.core.person.ai.NaturalAttributeType;
import com.mars_sim.core.person.ai.SkillType;
import com.mars_sim.core.person.ai.job.util.JobType;
import com.mars_sim.core.person.ai.task.util.Task;
import com.mars_sim.core.person.ai.task.util.TaskPhase;
import com.mars_sim.core.science.ScienceType;
import com.mars_sim.core.science.ScientificStudy;
import com.mars_sim.core.structure.building.Building;
import com.mars_sim.core.structure.building.BuildingManager;
import com.mars_sim.core.vehicle.Rover;
import com.mars_sim.tools.Msg;
import com.mars_sim.tools.util.RandomUtil;

/**
 * A task for proposing a new scientific study.
 */
public class ProposeScientificStudy extends Task {

	/** default serial id. */
	private static final long serialVersionUID = 1L;

	/** default logger. */
	private static SimLogger logger = SimLogger.getLogger(ProposeScientificStudy.class.getName());

	/** Task name */
	private static final String NAME = Msg.getString("Task.description.proposeScientificStudy"); //$NON-NLS-1$

	/** The stress modified per millisol. */
	private static final double STRESS_MODIFIER = .2D;

	/** Task phases. */
	private static final TaskPhase PROPOSAL_PHASE = new TaskPhase(Msg.getString("Task.phase.proposalPhase")); //$NON-NLS-1$

	/** The scientific study to propose. */
	private ScientificStudy study;

	/**
	 * Constructor.
	 * 
	 * @param person the person performing the task.
	 */
	public ProposeScientificStudy(Person person) {
		// Skill set set later on based on Study
		super(NAME, person, false, true, STRESS_MODIFIER, null, 25D, 10D + RandomUtil.getRandomDouble(50D));
		setExperienceAttribute(NaturalAttributeType.ACADEMIC_APTITUDE);
		
		study = person.getStudy();
		if (study == null) {		
			// Create new scientific study.
			JobType job = person.getMind().getJob();
			ScienceType science = ScienceType.getJobScience(job);
			if (science != null) {
				SkillType skill = science.getSkill();
				int level = person.getSkillManager().getSkillLevel(skill);
				study = scientificStudyManager.createScientificStudy(person, science, level);

				
			} else {
				logger.severe(person, "Not a scientist.");
				endTask();
			}
		}

		if (study != null) {
			
			addAdditionSkill(study.getScience().getSkill());
			setDescription(
					Msg.getString("Task.description.proposeScientificStudy.detail", study.getScience().getName())); // $NON-NLS-1$

			// If person is in a settlement, try to find a building.
			boolean walk = false;
			if (person.isInSettlement()) {
				Building b = BuildingManager.getAvailableBuilding(study, person);
				if (b != null) {
					// Walk to this specific building.
					walkToResearchSpotInBuilding(b, false);
					walk = true;
				}
			}

			if (!walk) {

				if (person.isInVehicle()) {
					// If person is in rover, walk to passenger activity spot.
					if (person.getVehicle() instanceof Rover rover) {
						walkToPassengerActivitySpotInRover(rover, false);
					}
				} else {
					// Walk to random location.
					walkToRandomLocation(true);
				}
			}
		} else {
			endTask();
		}

		// Initialize phase
		addPhase(PROPOSAL_PHASE);
		setPhase(PROPOSAL_PHASE);
	}

	@Override
	protected double performMappedPhase(double time) {
		if (getPhase() == null) {
			throw new IllegalArgumentException("Task phase is null");
		} else if (PROPOSAL_PHASE.equals(getPhase())) {
			return proposingPhase(time);
		} else {
			return time;
		}
	}

	/**
	 * Performs the writing study proposal phase.
	 * 
	 * @param time the amount of time (millisols) to perform the phase.
	 * @return the amount of time (millisols) left over after performing the phase.
	 */
	private double proposingPhase(double time) {
		double remainingTime = 0;
		
		if (!study.getPhase().equals(ScientificStudy.PROPOSAL_PHASE)) {
			endTask();
			return time;
		}

		if (isDone()) {
			logger.log(person, Level.INFO, 10_000, "Proposed " + study + ".");
			endTask();
			return time;
		}

		if (person.getPhysicalCondition().computeFitnessLevel() < 2) {
			logger.log(person, Level.FINE, 10_000, "Ended proposing scientific study. Not feeling well.");
			endTask();
			return time;
		}
		
		// Determine amount of effective work time based on science skill.
		double workTime = time;
		int scienceSkill = getEffectiveSkillLevel();
		if (scienceSkill == 0) {
			workTime /= 2;
		} else {
			workTime += workTime * (.2D * (double) scienceSkill);
		}

		study.addProposalWorkTime(workTime);

		// Add experience
		addExperience(time);
		
		if (study.isProposalCompleted()) {
			logger.log(worker, Level.INFO, 0, "Finished writing a study proposal for " 
					+ study.getName() + "."); 

			endTask();
			return remainingTime;
		}

		return remainingTime;
	}
}
