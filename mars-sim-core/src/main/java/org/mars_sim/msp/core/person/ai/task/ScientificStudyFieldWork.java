/*
 * Mars Simulation Project
 * AreologyStudyFieldWork.java
 * @date 2021-08-28
 * @author Scott Davis
 */
package org.mars_sim.msp.core.person.ai.task;

import java.awt.geom.Point2D;
import java.io.Serializable;
import java.util.logging.Level;

import org.mars_sim.msp.core.LocalAreaUtil;
import org.mars_sim.msp.core.logging.SimLogger;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.ai.mission.MissionMember;
import org.mars_sim.msp.core.person.ai.task.utils.TaskPhase;
import org.mars_sim.msp.core.science.ScientificStudy;
import org.mars_sim.msp.core.tool.RandomUtil;
import org.mars_sim.msp.core.vehicle.Rover;

/**
 * A task for the EVA operation of performing field work at a research
 * site for a scientific study.
 */
public abstract class ScientificStudyFieldWork extends EVAOperation implements Serializable {

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
	 * Constructor
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
		Point2D fieldWorkLoc = determineFieldWorkLocation();
		if (fieldWorkLoc != null)
			setOutsideSiteLocation(fieldWorkLoc.getX(), fieldWorkLoc.getY());

		// Add task phases
		this.fieldWork = fieldwork;
		addPhase(fieldWork);
	}

	/**
	 * Determine location for field work.
	 * 
	 * @return field work X and Y location outside rover.
	 */
	private Point2D determineFieldWorkLocation() {

		Point2D newLocation = null;
		boolean goodLocation = false;
		for (int x = 0; (x < 5) && !goodLocation; x++) {
			for (int y = 0; (y < 10) && !goodLocation; y++) {

				double distance = RandomUtil.getRandomDouble(100D) + (x * 100D) + 50D;
				double radianDirection = RandomUtil.getRandomDouble(Math.PI * 2D);
				double newXLoc = rover.getXLocation() - (distance * Math.sin(radianDirection));
				double newYLoc = rover.getYLocation() + (distance * Math.cos(radianDirection));
				Point2D boundedLocalPoint = new Point2D.Double(newXLoc, newYLoc);

				newLocation = LocalAreaUtil.getLocalRelativeLocation(boundedLocalPoint.getX(), boundedLocalPoint.getY(),
						rover);
				goodLocation = LocalAreaUtil.isLocationCollisionFree(newLocation.getX(), newLocation.getY(),
						person.getCoordinates());
			}
		}

		return newLocation;
	}

	/**
	 * Checks if a person can research a site.
	 * 
	 * @param member the member.
	 * @param rover  the rover
	 * @return true if person can research a site.
	 */
	public static boolean canResearchSite(MissionMember member, Rover rover) {

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
	 * Perform the field work phase of the task.
	 * 
	 * @param time the time available (millisols).
	 * @return remaining time after performing phase (millisols).
	 */
	private double fieldWorkPhase(double time) {

		// Check all condition to carry on
		// 1. radiation exposure/detection
		// 2. Site duration has ended or there is reason to stop the field work
		// 3. The study activities are completed
		if (isDone() || isRadiationDetected(time)
				|| shouldEndEVAOperation() || addTimeOnSite(time)
				|| performStudy(time)) {
			if (person.isOutside())
        		setPhase(WALK_BACK_INSIDE);
        	else
        		endTask();
			return time;
		}

		// Add research work to the scientific study for lead researcher.
		addResearchWorkTime(time);

		// Add experience points
		addExperience(time);

		// Check for an accident during the EVA operation.
		checkForAccident(time);

		return 0D;
	}

	/**
	 * Performs any specific study activities
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
	 * Transfer the Specimen box to the Vehicle
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
