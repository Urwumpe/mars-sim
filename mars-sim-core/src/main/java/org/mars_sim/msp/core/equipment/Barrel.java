/**
 * Mars Simulation Project
 * Barrel.java
 * @version 3.2.0 2021-06-20
 * @author Scott Davis
 */
package org.mars_sim.msp.core.equipment;

import org.mars_sim.msp.core.Coordinates;
import org.mars_sim.msp.core.resource.PhaseType;
import org.mars_sim.msp.core.structure.Settlement;
import org.mars_sim.msp.core.structure.building.Building;

import java.io.Serializable;

/**
 * A barrel container for holding liquid amount resources.
 */
public class Barrel
extends Equipment
implements Container, Serializable {

	
	/** default serial id. */
	private static final long serialVersionUID = 1L;

	// Static data members
	public static final String TYPE = "Barrel";
	public static final double CAPACITY = 50D;
	public static final double EMPTY_MASS = 1D;
	/** The phase type that this container can hold */
	public static final PhaseType phaseType = PhaseType.LIQUID;
	
	/**
	 * Constructor
	 * @param location the location of the barrel.
	 * @throws Exception if error creating barrel.
	 */
	public Barrel(Coordinates location) {
		// Use Equipment constructor
		super(TYPE, TYPE, location);

		// Sets the base mass of the barrel.
		setBaseMass(EMPTY_MASS);

		// Set the liquid capacity.
		getInventory().addAmountResourcePhaseCapacity(phaseType, CAPACITY);
	}

	/**
	 * Gets the phase of resources this container can hold.
	 * @return resource phase.
	 */
	public PhaseType getContainingResourcePhase() {
		return phaseType;
	}

	/**
	 * Gets the total capacity of resource that this container can hold.
	 * @return total capacity (kg).
	 */
	public double getTotalCapacity() {
		return CAPACITY;
	}

	@Override
	public Building getBuildingLocation() {
		return getContainerUnit().getBuildingLocation();
	}

	@Override
	public Settlement getAssociatedSettlement() {
		return getContainerUnit().getAssociatedSettlement();
	}
}
