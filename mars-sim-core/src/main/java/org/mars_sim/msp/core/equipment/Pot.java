/*
 * Mars Simulation Project
 * Pot.java
 * @date 2023-07-30
 * @author Manny Kung
 */

package org.mars_sim.msp.core.equipment;

import org.mars_sim.msp.core.Entity;

public class Pot extends AmountResourceBin {
	
	/** default serial id. */
	private static final long serialVersionUID = 1L;

	public Pot(Entity entity, double cap) {
		super(entity, cap);

		setBinType(BinType.POT);
	}
}
