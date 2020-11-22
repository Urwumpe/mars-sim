/**
 * Mars Simulation Project
 * TransportEvent.java
 * @version 3.1.2 2020-09-02
 * @author Scott Davis
 */
package org.mars_sim.msp.core.interplanetary.transport;

import java.io.Serializable;

import org.mars_sim.msp.core.events.HistoricalEvent;
import org.mars_sim.msp.core.events.HistoricalEventCategory;
import org.mars_sim.msp.core.person.EventType;

/**
 * A historical event for interplanetary transportation.
 */
public class TransportEvent extends HistoricalEvent implements Serializable {

	/** default serial id. */
	private static final long serialVersionUID = 1L;

	/**
	 * Constructor.
	 * 
	 * @param transportItem the transport item.
	 * @param eventType     the event type string.
	 * @param cause         The cause for this event.
	 * @param location   the settlement/coordinate where it occurs.
	 */
	public TransportEvent(Transportable transportItem, EventType eventType, String cause, String location) {
		super(HistoricalEventCategory.TRANSPORT, eventType, transportItem, transportItem.getName(), "N/A", cause, "N/A",
				location, location);
		// TODO: Add the type of rocket
	}
}
