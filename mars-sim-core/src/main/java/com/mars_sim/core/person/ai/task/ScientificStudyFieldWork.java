/*
 * Mars Simulation Project
 * ScientificStudyFieldWork.java
 * @date 2023-09-17
 * @author Barry Evans
 */
package com.mars_sim.core.person.ai.task;

import java.util.logging.Level;

import com.mars_sim.core.logging.SimLogger;
import com.mars_sim.core.person.Person;
import com.mars_sim.core.person.ai.task.util.TaskPhase;
import com.mars_sim.core.person.ai.task.util.Worker;
import com.mars_sim.core.science.ScientificStudy;
import com.mars_sim.core.vehicle.Rover;
import com.mars_sim.tools.util.RandomUtil;

/**
 * A task for the EVA operation of performing field work at a research
 * site for a scientific study.
 */
public abstract class ScientificStudyFieldWork extends EVAOperation {

	/** default serial id. */
	private static final long serialVersionUID = 1L;
	/** default logger. */
	private static final SimLogger logger = SimLogger.getLogger(ScientificStudyFieldWork.class.getName());

	// Data members
	private Person leadResearcher;
	private ScientificStudy study;
	private Rover rover;
	private TaskPhase fieldWork;

	/**
	 * Constructor.
	 * 
	 * @param person         the person performing the task.
	 * @param leadResearcher the researcher leading the field work.
	 * @param study          the scientific study the field work is for.
	 * @param rover          the rover
	 */
	protected ScientificStudyFieldWork(String name, TaskPhase fieldwork, Person person, Person leadResearcher,
									   ScientificStudy study, Rover rover) {

		// Use EVAOperation parent constructor.
		super(name, person, true, RandomUtil.getRandomDouble(50D) + 10D, 
			  study.getScience().getSkill());

		// Initialize data members.
		this.leadResearcher = leadResearcher;
		this.study = study;
		this.rover = rover;

		// Determine location for field work.
		setRandomOutsideLocation(rover);

		// Add task phases
		this.fieldWork = fieldwork;
		addPhase(fieldWork);
	}

	/**
	 * Checks if a person can research a site.
	 * 
	 * @param member the member.
	 * @param rover  the rover
	 * @return true if person can research a site.
	 */
	public static boolean canResearchSite(Worker member, Rover rover) {

		if (member instanceof Person) {
			Person person = (Person) member;

			// Check if person can exit the rover.
			if (!ExitAirlock.canExitAirlock(person, rover.getAirlock()))
				return false;

			if (isGettingDark(person)) {
				logger.log(person, Level.FINE, 5_000,
						" ended " + person.getTaskDescription() + " due to getting too dark "
						+ " at " + person.getCoordinates().getFormattedString());
				return false;
			}

			// Check if person's medical condition will not allow task.
			return (person.getPerformanceRating() >= .3D);
		}

		return true;
	}

	@Override
	protected TaskPhase getOutsideSitePhase() {
		return fieldWork;
	}

	@Override
	protected double performMappedPhase(double time) {

		time = super.performMappedPhase(time);
		if (!isDone()) {
			if (getPhase() == null) {
				throw new IllegalArgumentException("Task phase is null");
			} else if (fieldWork.equals(getPhase())) {
				time = fieldWorkPhase(time);
			} 
		}
		return time;
	}

	/**
	 * Performs the field work phase of the task.
	 * 
	 * @param time the time available (millisols).
	 * @return remaining time after performing phase (millisols).
	 */
	private double fieldWorkPhase(double time) {
		double remainingTime = 0;
		
		// Check for radiation exposure during the EVA operation.
		if (isDone()) {
			checkLocation("Task duration ended.");
			return time;
		}
		
		// Check for radiation exposure during the EVA operation.
		if (isRadiationDetected(time)) {
			checkLocation("Radiation detected.");
			return time;
		}
		
		// Check if site duration has ended or there is reason to cut the collect
		// minerals phase short and return to the rover.
		if (shouldEndEVAOperation(false)) {
			checkLocation("EVA ended.");
			return time;
		}

        // Check time on site
		if (addTimeOnSite(time)) {
			checkLocation("Time on site expired.");
			return time;
		}	
		
		// Check if the study is completed
		if (performStudy(time)) {
			checkLocation("Study completed.");
			return time;
		}

		// Add research work to the scientific study for lead researcher.
		addResearchWorkTime(time);

		// Add experience points
		addExperience(time);

		// Check for an accident during the EVA operation.
		checkForAccident(time);

		return remainingTime;
	}

	/**
	 * Performs any specific study activities.
	 * 
	 * @param time Time to do the study; maybe used by overriding classes
	 * @return Is the field work completed
	 */
	protected boolean performStudy(double time) {
		return false;
	}
	
	/**
	 * Adds research work time to the scientific study for the lead researcher.
	 * 
	 * @param time the time (millisols) performing field work.
	 */
	private void addResearchWorkTime(double time) {
		// Determine effective field work time.
		double effectiveFieldWorkTime = time;
		int skill = getEffectiveSkillLevel();
		if (skill == 0) {
			effectiveFieldWorkTime /= 2D;
		} else if (skill > 1) {
			effectiveFieldWorkTime += effectiveFieldWorkTime * (.2D * skill);
		}

		// If person isn't lead researcher, divide field work time by two.
		if (!person.equals(leadResearcher)) {
			effectiveFieldWorkTime /= 2D;
		}

		// Add research to study for primary or collaborative researcher.
		if (study.getPrimaryResearcher().equals(leadResearcher)) {
			study.addPrimaryResearchWorkTime(effectiveFieldWorkTime);
		} else {
			study.addCollaborativeResearchWorkTime(leadResearcher, effectiveFieldWorkTime);
		}
	}
	
	protected Rover getRover() {
		return rover;
	}
	
	/**
	 * Transfers the Specimen box to the Vehicle.
	 */
	@Override
	protected void clearDown() {
		super.clearDown();
		
		if (rover != null) {
			// Task may end early before a Rover is selected
			returnEquipmentToVehicle(rover);
		}
	}
}
