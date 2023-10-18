/**
 * Mars Simulation Project
 * MalfunctionEvent.java
 * @version 3.2.0 2021-06-20
 * @author Scott Davis
 */
package com.mars_sim.core.malfunction;

import com.mars_sim.core.Unit;
import com.mars_sim.core.events.HistoricalEvent;
import com.mars_sim.core.events.HistoricalEventCategory;
import com.mars_sim.core.person.EventType;

/**
 * This class represents the historical action of a Malfunction occurring or
 * being resolved.
 */
public class MalfunctionEvent extends HistoricalEvent {

	/** default serial id. */
	private static final long serialVersionUID = 1L;

	/**
	 * Create an event associated to a Malfunction.
	 * 
	 * @param type        {@link EventType} Type of event.
	 * @param whileDoing  the activity the person was engaging.
	 * @param whoAffected Who is being primarily affected by this event.
	 * @param container		the building/vehicle where it occurs
	 * @param homeTown		the associated settlement where it belongs
	 * @param coordinates	the coordinates where it belongs
	 */
	public MalfunctionEvent(EventType type, Malfunction malfunction, String whileDoing,
			String whoAffected, Unit container) {
		super(HistoricalEventCategory.MALFUNCTION, type, malfunction, malfunction.getName(), whileDoing, whoAffected, container);
	}
}
