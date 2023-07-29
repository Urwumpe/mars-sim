/*
 * Mars Simulation Project
 * TendFishTank.java
 * @date 2022-06-25
 * @author Barry Evans
 */
package org.mars_sim.msp.core.person.ai.task;

import java.util.List;
import java.util.logging.Level;

import org.mars.sim.tools.Msg;
import org.mars.sim.tools.util.RandomUtil;
import org.mars_sim.msp.core.logging.SimLogger;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.ai.SkillType;
import org.mars_sim.msp.core.person.ai.task.util.Task;
import org.mars_sim.msp.core.person.ai.task.util.TaskPhase;
import org.mars_sim.msp.core.robot.Robot;
import org.mars_sim.msp.core.structure.building.Building;
import org.mars_sim.msp.core.structure.building.function.FunctionType;
import org.mars_sim.msp.core.structure.building.function.farming.Fishery;

/**
 * The TendFishTank class is a task for tending the fishery in a
 * settlement. This is an effort driven task.
 */
public class TendFishTank extends Task {

	/** default serial id. */
	private static final long serialVersionUID = 1L;

	/** default logger. */
	private static SimLogger logger = SimLogger.getLogger(TendFishTank.class.getName());

	/** Task name */
	private static final String NAME = Msg.getString("Task.description.tendFishTank"); //$NON-NLS-1$
	/** Task phases. */
	private static final TaskPhase TENDING = new TaskPhase(Msg.getString("Task.phase.tending")); //$NON-NLS-1$
	/** Task phases. */
	private static final TaskPhase INSPECTING = new TaskPhase(Msg.getString("Task.phase.inspecting")); //$NON-NLS-1$
	/** Task phases. */
	private static final TaskPhase CLEANING = new TaskPhase(Msg.getString("Task.phase.cleaning")); //$NON-NLS-1$
	/** Task phases. */
	private static final TaskPhase CATCHING = new TaskPhase(Msg.getString("Task.phase.catching")); //$NON-NLS-1$	

	// Limit the maximum time spent on a phase
	private static final double MAX_FISHING = 100D;
	private static final double MAX_TEND = 100D;
	
	// Static members
	/** The stress modified per millisol. */
	private static final double STRESS_MODIFIER = -1.1D;

	// Data members
	private double fishingTime = 0D;
	private double tendTime = 0D;
	/** The goal of the task at hand. */
	private String cleanGoal;
	/** The goal of the task at hand. */
	private String inspectGoal;
	/** The fish tank the person is tending. */
	private Fishery fishTank;
	/** The building where the fish tank is. */	
	private Building building;
	
	/**
	 * Constructor.
	 * 
	 * @param person the person performing the task.
	 */
	public TendFishTank(Person person, Fishery fishTank) {
		// Use Task constructor
		super(NAME, person, false, false, STRESS_MODIFIER, SkillType.BIOLOGY, 100D);

		if (person.isOutside()) {
			endTask();
			return;
		}

		// Get available greenhouse if any.
		this.fishTank = fishTank;
		this.building = fishTank.getBuilding();

		// Walk to fish tank.
		walkToTaskSpecificActivitySpotInBuilding(building, FunctionType.FISHERY, false);	

		if (fishTank.getSurplusStock() > 0) {
			// Do fishing
			setPhase(CATCHING);
			addPhase(CATCHING);
		}
		else if (fishTank.getWeedDemand() > 0) {
			setPhase(TENDING);
			addPhase(TENDING);
			addPhase(INSPECTING);
			addPhase(CLEANING);
		}
		else {
			setPhase(INSPECTING);
			addPhase(INSPECTING);
			addPhase(CLEANING);
		}
	}

	/**
	 * Constructor 2.
	 * 
	 * @param robot the robot performing the task.
	 */
	public TendFishTank(Robot robot, Fishery fishTank) {
		// Use Task constructor
		super(NAME, robot, false, false, 0, SkillType.BIOLOGY, 50D);

		// Initialize data members
		if (robot.isOutside()) {
			endTask();
			return;
		}

		// Get available greenhouse if any.
		this.fishTank = fishTank;
		this.building = fishTank.getBuilding();

		// Walk to fishtank.
		walkToTaskSpecificActivitySpotInBuilding(building, FunctionType.FISHERY, false);
		
		// Initialize phase
		// Robots do not do anything with water
		setPhase(CLEANING);
		addPhase(CLEANING);
	}

	@Override
	protected double performMappedPhase(double time) {
		if (getPhase() == null) {
			return 0;
		} else if (TENDING.equals(getPhase())) {
			return tendingPhase(time);
		} else if (INSPECTING.equals(getPhase())) {
			return inspectingPhase(time);
		} else if (CLEANING.equals(getPhase())) {
			return cleaningPhase(time);
		} else if (CATCHING.equals(getPhase())) {
			return catchingPhase(time);
		} else {
			return time;
		}
	}

	/**
	 * Performs the tending phase.
	 * 
	 * @param time the amount of time (millisols) to perform the phase.
	 * @return the amount of time (millisols) left over after performing the phase.
	 */
	private double catchingPhase(double time) {

		double workTime = time;

		if (isDone()) {
			endTask();
			return time;
		}

		// Check if building has malfunction.
		if (building.getMalfunctionManager() != null && building.getMalfunctionManager().hasMalfunction()) {
			endTask();
			return time;
		}

		double mod = 0;

		if (person != null) {
			mod = 6D;
		}

		else {
			mod = 4D;
		}

		// Determine amount of effective work time based on "Botany" skill
		int skill = getEffectiveSkillLevel();
		if (skill <= 0) {
			mod += RandomUtil.getRandomDouble(.25);
		} else {
			mod += RandomUtil.getRandomDouble(.25) + 1.25 * skill;
		}

		workTime *= mod;

		double remainingTime = fishTank.catchFish(person, workTime);

		// Add experience
		addExperience(time);

		// Check for accident
		checkForAccident(building, time, 0.003);

		if ((remainingTime > 0) || (fishTank.getSurplusStock() == 0)) {
			endTask();

			// Scale it back to the. Calculate used time 
			double usedTime = workTime - remainingTime;
			return time - (usedTime / mod);
		}
		else {
			fishingTime += time;
			if (fishingTime > MAX_FISHING) {
				logger.log(building, person, Level.INFO, 0, "Giving up on fishing.", null);
				endTask();
			}
		}
		
		return 0;
	}
	
	
	/**
	 * Performs the tending phase.
	 * 
	 * @param time the amount of time (millisols) to perform the phase.
	 * @return the amount of time (millisols) left over after performing the phase.
	 */
	private double tendingPhase(double time) {

		double workTime = time;

		if (isDone()) {
			endTask();
			return time;
		}

		// Check if building has malfunction.
		if (building.getMalfunctionManager() != null && building.getMalfunctionManager().hasMalfunction()) {
			endTask();
			return time;
		}

		double mod = 1;

		// Determine amount of effective work time based on "Botany" skill
		int skill = getEffectiveSkillLevel();
		if (skill > 0) {
			mod += RandomUtil.getRandomDouble(.25) + 1.25 * skill;
		}

		workTime *= mod;

		double remainingTime = fishTank.tendWeeds(workTime);

		// Add experience
		addExperience(time);

		// Check for accident
		checkForAccident(building, time, 0.005D);

		if (remainingTime > 0) {
			setPhase(INSPECTING);

			// Scale it back to the. Calculate used time 
			double usedTime = workTime - remainingTime;
			return time - (usedTime / mod);
		}
		else if (tendTime > MAX_TEND) {
			logger.log(building, person, Level.INFO, 0, "Giving up on tending.", null);
			endTask();
		}
		tendTime += time;
		
		return 0;
	}
	
	/**
	 * Performs the inspecting phase.
	 * 
	 * @param time the amount of time (millisols) to perform the phase.
	 * @return the amount of time (millisols) left over after performing the phase.
	 */
	private double inspectingPhase(double time) {
		if (inspectGoal == null) {
			List<String> uninspected = fishTank.getUninspected();
			int size = uninspected.size();
	
			if (size > 0) {
				int rand = RandomUtil.getRandomInt(size - 1);
	
				inspectGoal = uninspected.get(rand);
			}
		}

		if (inspectGoal != null) {
			printDescription(Msg.getString("Task.description.tendFishTank.inspect.detail", 
					inspectGoal.toLowerCase()));

			double mod = 0;
			// Determine amount of effective work time based on "Botany" skill
			int greenhouseSkill = getEffectiveSkillLevel();
			if (greenhouseSkill <= 0) {
				mod *= RandomUtil.getRandomDouble(.5, 1.0);
			} else {
				mod *= RandomUtil.getRandomDouble(.5, 1.0) * greenhouseSkill * 1.2;
			}
	
			double workTime = time * mod;
			
			addExperience(workTime);
			
			if (getDuration() <= (getTimeCompleted() + time)) {
				fishTank.markInspected(inspectGoal);
				endTask();
			}
		}
			
		return 0;
	}


	/**
	 * Sets the description and print the log.
	 * 
	 * @param text
	 */
	private void printDescription(String text) {
		setDescription(text);
		logger.log(fishTank.getBuilding(), worker, Level.FINE, 30_000L, text + ".");
	}
	
	/**
	 * Performs the cleaning phase.
	 * 
	 * @param time the amount of time (millisols) to perform the phase.
	 * @return the amount of time (millisols) left over after performing the phase.
	 */
	private double cleaningPhase(double time) {

		if (cleanGoal == null) {
			List<String> uncleaned = fishTank.getUncleaned();
			int size = uncleaned.size();
	
			if (size > 0) {
				int rand = RandomUtil.getRandomInt(size - 1);
	
				cleanGoal = uncleaned.get(rand);
			}
		}
		
		if (cleanGoal != null) {
			printDescription(Msg.getString("Task.description.tendFishTank.clean.detail", 
					cleanGoal.toLowerCase()));
				
			double mod = 0;
			// Determine amount of effective work time based on "Botany" skill
			int greenhouseSkill = getEffectiveSkillLevel();
			if (greenhouseSkill <= 0) {
				mod *= RandomUtil.getRandomDouble(.5, 1.0);
			} else {
				mod *= RandomUtil.getRandomDouble(.5, 1.0) * greenhouseSkill * 1.2;
			}
	
			double workTime = time * mod;
			
			addExperience(workTime);
			
			if (getDuration() <= (getTimeCompleted() + time)) {
				fishTank.markCleaned(cleanGoal);
				endTask();
			}
		}
		else
			endTask();
		
		return 0;
	}
}
