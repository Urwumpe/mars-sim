/*
 * Mars Simulation Project
 * ListenToMusic.java
 * @date 2022-06-30
 * @author Manny Kung
 */
package com.mars_sim.core.person.ai.task;

import java.util.logging.Level;

import com.mars_sim.core.logging.SimLogger;
import com.mars_sim.core.person.Person;
import com.mars_sim.core.person.ai.task.util.Task;
import com.mars_sim.core.person.ai.task.util.TaskPhase;
import com.mars_sim.core.structure.building.Building;
import com.mars_sim.core.structure.building.BuildingManager;
import com.mars_sim.core.structure.building.function.FunctionType;
import com.mars_sim.core.vehicle.Rover;
import com.mars_sim.tools.Msg;
import com.mars_sim.tools.util.RandomUtil;

/**
 * This task lowers the stress and fatigue.
 * The duration of the task is by default chosen randomly, up to 100 millisols.
 */
public class ListenToMusic
extends Task {

	/** default serial id. */
	private static final long serialVersionUID = 1L;

	/** default logger. */
	private static final SimLogger logger = SimLogger.getLogger(ListenToMusic.class.getName());

	/** Task name */
    private static final String NAME = Msg.getString(
            "Task.description.listenToMusic"); //$NON-NLS-1$

    /** Task phases. */
    private static final TaskPhase LISTENING_TO_MUSIC = new TaskPhase(Msg.getString(
            "Task.phase.listeningToMusic")); //$NON-NLS-1$

    private static final TaskPhase FINDING_A_SONG = new TaskPhase(Msg.getString(
            "Task.phase.findingASong")); //$NON-NLS-1$

	// Static members
	/** The stress modified per millisol. */
	private static final double STRESS_MODIFIER = -.9D;

	/**
	 * Constructor.
	 * 
	 * @param person the person to perform the task
	 */
	public ListenToMusic(Person person) {
		super(NAME, person, false, false, STRESS_MODIFIER, 
				10D + RandomUtil.getRandomDouble(-2.5, 2.5));
		
		if (person.isOutside()) {
			endTask();
			return;
		}
		
		// If during person's work shift, reduce the time to 1/4.
        boolean isShiftHour = person.isOnDuty();
		if (isShiftHour) {
		    setDuration(getDuration()/4D);
		}

		// If person is in a settlement, try to find a place to relax.	
		if (person.isInSettlement()) {
			try {
				Building rec = BuildingManager.getAvailableFunctionTypeBuilding(person, FunctionType.RECREATION);
				if (rec != null) {
					// Walk to recreation building.
				    walkToActivitySpotInBuilding(rec, FunctionType.RECREATION, true);
				} else {
                	// if rec building is not available, go to a gym
                	Building gym = BuildingManager.getAvailableFunctionTypeBuilding(person, FunctionType.EXERCISE);
                	if (gym != null) {
	                	walkToActivitySpotInBuilding(gym, FunctionType.EXERCISE, true);
	                } else {
	    				// Go back to his bed
	    				if (person.hasBed()) {
	    					// Walk to the bed
	    					walkToBed(person.getQuarters(), person, true);
		                }
	                }
				}
				
			} catch (Exception e) {
				logger.log(Level.SEVERE,"ListenToMusic's constructor(): " + e.getMessage());
				endTask();
			}
		}

		else if (person.isInVehicle()
                && person.getVehicle() instanceof Rover r) {
				// If person is in rover, walk to passenger activity spot.
				walkToPassengerActivitySpotInRover(r, true);
		}
		    
        setDescription(Msg.getString("Task.description.listenToMusic"));
	
    
		// Initialize phase
		addPhase(FINDING_A_SONG);
		addPhase(LISTENING_TO_MUSIC);
	
		setPhase(FINDING_A_SONG);
	
	}

	@Override
	protected double performMappedPhase(double time) {
		if (getPhase() == null) {
			throw new IllegalArgumentException("ListenToMusic. Task phase is null");
		}
		else if (FINDING_A_SONG.equals(getPhase())) {
			return findingPhase(time);
		}
		else if (LISTENING_TO_MUSIC.equals(getPhase())) {
			return listeningPhase(time);
		}
		else {
			return time;
		}
	}

	/**
	 * Performs the listening phase of the task.
	 * 
	 * @param time the amount of time (millisol) to perform the phase.
	 * @return the amount of time (millisol) left after performing the phase.
	 */
	private double listeningPhase(double time) {
		double remainingTime = 0;
		
        // Reduce person's fatigue
        person.getPhysicalCondition().reduceFatigue(.5 * time);

        setDescription(Msg.getString("Task.description.listenToMusic")); //$NON-NLS-1$
        
		return remainingTime;
	}

	/**
	 * Performs the finding phase of the task.
	 * 
	 * @param time the amount of time (millisol) to perform the phase.
	 * @return the amount of time (millisol) left after performing the phase.
	 */
	private double findingPhase(double time) {
		double remainingTime = 0;
		
		if (person.isOutside()) {
			endTask();
			return time;
		}
		
		setDescription(Msg.getString("Task.description.listenToMusic.findingSong"));//$NON-NLS-1$
		// Note: add codes for selecting a particular type of music		
		setPhase(LISTENING_TO_MUSIC);
		return remainingTime;
	}
}
