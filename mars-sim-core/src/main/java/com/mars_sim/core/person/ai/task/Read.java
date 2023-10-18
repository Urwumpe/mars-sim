/*
 * Mars Simulation Project
 * Read.java
 * @date 2022-07-16
 * @author Manny Kung
 */
package com.mars_sim.core.person.ai.task;

import com.mars_sim.core.logging.SimLogger;
import com.mars_sim.core.person.Person;
import com.mars_sim.core.person.ai.NaturalAttributeManager;
import com.mars_sim.core.person.ai.NaturalAttributeType;
import com.mars_sim.core.person.ai.SkillType;
import com.mars_sim.core.person.ai.task.meta.ReadMeta;
import com.mars_sim.core.person.ai.task.util.Task;
import com.mars_sim.core.person.ai.task.util.TaskPhase;
import com.mars_sim.core.structure.building.Building;
import com.mars_sim.core.structure.building.BuildingManager;
import com.mars_sim.core.structure.building.function.FunctionType;
import com.mars_sim.core.vehicle.Rover;
import com.mars_sim.tools.Msg;
import com.mars_sim.tools.util.RandomUtil;

/**
 * The Read class is the task of reading
 */
public class Read extends Task {

	/** default serial id. */
	private static final long serialVersionUID = 1L;
	
    /** default logger. */
	private static SimLogger logger = SimLogger.getLogger(Read.class.getName());

	/** Task name */
	private static final String NAME = Msg.getString("Task.description.read"); //$NON-NLS-1$

	/** Task phases. */
	private static final TaskPhase READING = new TaskPhase(Msg.getString("Task.phase.reading")); //$NON-NLS-1$

	// Static members
	/** The stress modified per millisol. */
	private static final double STRESS_MODIFIER = -.1D;
	
	/** The selected skill type for this reading session. */
	private SkillType selectedSkill;
	
	/**
	 * Constructor. This is an effort-driven task.
	 *
	 * @param person the person performing the task.
	 */
	public Read(Person person) {
		// Use Task constructor. Skill is set later
		super(NAME, person, true, false, STRESS_MODIFIER, RandomUtil.getRandomInt(5, 20));

		if (person.isInside()) {

			int score = person.getPreference().getPreferenceScore(new ReadMeta());
			// Modify the duration based on the preference score
			setDuration(Math.max(5, score + getDuration()));
			// Factor in a person's preference for the new stress modifier
			setStressModifier(-score / 20D + STRESS_MODIFIER);

			// Set the boolean to true so that it won't be done again today
			// person.getPreference().setTaskStatus(this, false);

			if (person.isInSettlement()) {

				int rand = RandomUtil.getRandomInt(2);

				if (rand == 0) {
					// Find a dining place
					Building dining = BuildingManager.getAvailableDiningBuilding(person, false);
					if (dining != null) {
						walkToActivitySpotInBuilding(dining, FunctionType.DINING, true);
					}
					else {
						// Go back to his quarters
						Building quarters = person.getQuarters();
						if (quarters != null) {
							walkToBed(quarters, person, true);
						}
					}
				}

				else if (rand == 1) {
					Building rec = BuildingManager.getAvailableFunctionTypeBuilding(person, FunctionType.RECREATION);
					if (rec != null) {
						walkToActivitySpotInBuilding(rec, FunctionType.RECREATION, true);
					}
					else {
						// Go back to his quarters
						Building quarters = person.getQuarters();
						if (quarters != null) {
							walkToBed(quarters, person, true);
						}
					}
				}

				else {
					// Go back to his quarters
					Building quarters = person.getQuarters();
					if (quarters != null) {
						walkToBed(quarters, person, true);
					}
				}
			}

			else if (person.isInVehicle()) {
				// If person is in rover, walk to passenger activity spot.
				if (person.getVehicle() instanceof Rover rover) {
					walkToPassengerActivitySpotInRover(rover, true);
				} else {
					// Walk to random location.
					walkToRandomLocation(true);
				}
			}

			// Initialize phase
			addPhase(READING);
			setPhase(READING);
			
		} else {
			endTask();
		}
	}

	@Override
	protected double performMappedPhase(double time) {
		if (getPhase() == null) {
			throw new IllegalArgumentException("Task phase is null");
		} else if (READING.equals(getPhase())) {
			return reading(time);
		} else {
			return time;
		}
	}

	/**
	 * Performs reading phase.
	 *
	 * @param time the amount of time (millisols) to perform the phase.
	 * @return the amount of time (millisols) left over after performing the phase.
	 */
	private double reading(double time) {
		double remainingTime = 0;
		
		if ((getTimeCompleted() + time > getDuration()) || isDone()) {
	        String s = Msg.getString("Task.description.read.detail", selectedSkill.getName()); //$NON-NLS-1$
	        setDescription(s);
	        logger.fine(person, 4_000, "Done " + s.toLowerCase() + ".");
			endTask();
			return time;
		}

    	// Pick one skill randomly to improve upon
        if (selectedSkill == null) {
        	selectedSkill = person.getSkillManager().getARandomSkillType();
        
        	// Future: get this person's most favorite topics
	        String s = Msg.getString("Task.description.read.detail", selectedSkill.getName()); //$NON-NLS-1$
	    	// Display reading on a particular subject (skill type)
			setDescription(s);	
	        logger.fine(person, 4_000, "Started " + s.toLowerCase() + ".");
        }

		// Reading serves to improve skill
		addExperience(time);
		
		return remainingTime;
	}
	
	@Override
	protected void addExperience(double time) {
        // Experience points adjusted by person's "Experience Aptitude" attribute.
        NaturalAttributeManager nManager = person.getNaturalAttributeManager();
        int aptitude = nManager.getAttribute(NaturalAttributeType.ACADEMIC_APTITUDE);

		double learned = 2 * time * (aptitude / 100D) * RandomUtil.getRandomDouble(1);

		person.getSkillManager().addExperience(selectedSkill, learned, time);

	}
}
