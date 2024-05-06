/*
 * Mars Simulation Project
 * DigLocalIce.java
 * @date 2023-07-04
 * @author Scott Davis
 */
package com.mars_sim.core.structure.task;

import com.mars_sim.core.equipment.EquipmentType;
import com.mars_sim.core.person.Person;
import com.mars_sim.core.person.ai.task.util.TaskPhase;
import com.mars_sim.core.resource.ResourceUtil;
import com.mars_sim.tools.Msg;

/**
 * The DigLocalRegolith class is a task for digging and collecting
 * ice right outside in the vicinity of a settlement.
 */
public class DigLocalIce
extends DigLocal {

	/** default serial id. */
	private static final long serialVersionUID = 1L;
	
	/** Task name */
    private static final String NAME = Msg.getString(
            "Task.description.digLocalIce"); //$NON-NLS-1$

    /** Task phases. */
    private static final TaskPhase COLLECT_ICE = new TaskPhase(Msg.getString(
            "Task.phase.collectIce")); //$NON-NLS-1$

	/**
	 * Constructor 1.
	 * 
	 * @param person the person performing the task.
	 * @throws Exception if error constructing the task.
	 */
	public DigLocalIce(Person person) {
        // Use EVAOperation constructor.
        super(NAME, COLLECT_ICE, ResourceUtil.iceID, EquipmentType.BAG, person, 150); 
        if (!isDone()) {
        	setCollectionRate(person.getAssociatedSettlement().getIceCollectionRate());
        }
	}
	
	/**
	 * Constructor 2.
	 * 
	 * @param person the person performing the task.
	 * @param duration
	 * @throws Exception if error constructing the task.
	 */
	public DigLocalIce(Person person, int duration) {
        // Use EVAOperation constructor.
        super(NAME, COLLECT_ICE, ResourceUtil.iceID, EquipmentType.BAG, person, duration); 
        if (!isDone()) {
        	setCollectionRate(person.getAssociatedSettlement().getIceCollectionRate());
        }
	}
}
