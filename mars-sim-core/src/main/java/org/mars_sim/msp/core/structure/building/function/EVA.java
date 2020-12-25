/**
 * Mars Simulation Project
 * EVA.java
 * @version 3.1.2 2020-09-02
 * @author Scott Davis
 */
package org.mars_sim.msp.core.structure.building.function;

import java.io.Serializable;
import java.util.Iterator;

import org.mars_sim.msp.core.structure.Airlock;
import org.mars_sim.msp.core.structure.Settlement;
import org.mars_sim.msp.core.structure.building.Building;
import org.mars_sim.msp.core.structure.building.BuildingException;
import org.mars_sim.msp.core.time.ClockPulse;

/**
 * This class is a building function for extra vehicular activity.
 */
public class EVA
extends Function
implements Serializable {

	/** default serial id. */
	private static final long serialVersionUID = 1L;

	private static final FunctionType FUNCTION = FunctionType.EVA;

	private static final double MAINTENANCE_FACTOR = 5D; 
	
	private int airlockCapacity;
	
	private Airlock airlock;

	/**
	 * Constructor
	 * 
	 * @param building the building this function is for.
	 */
	public EVA(Building building) {
		// Use Function constructor.
		super(FUNCTION, building);

		String buildingType = building.getBuildingType();
		// Add a building airlock.
		airlockCapacity = buildingConfig.getAirlockCapacity(buildingType);
		double airlockXLoc = buildingConfig.getAirlockXLoc(buildingType);
		double airlockYLoc = buildingConfig.getAirlockYLoc(buildingType);
		double interiorXLoc = buildingConfig.getAirlockInteriorXLoc(buildingType);
		double interiorYLoc = buildingConfig.getAirlockInteriorYLoc(buildingType);
		double exteriorXLoc = buildingConfig.getAirlockExteriorXLoc(buildingType);
		double exteriorYLoc = buildingConfig.getAirlockExteriorYLoc(buildingType);

		airlock = new BuildingAirlock(building, airlockCapacity, airlockXLoc, airlockYLoc,
				interiorXLoc, interiorYLoc, exteriorXLoc, exteriorYLoc);
		
		// Load activity spots
		loadActivitySpots(buildingConfig.getEVAActivitySpots(building.getBuildingType()));

	}

	/**
	 * Constructor with airlock parameter for testing
	 * 
	 * @param building the building this function is for.
	 * @param airlock the building airlock.
	 */
	public EVA(Building building, BuildingAirlock airlock) {
		// Use Function constructor.
		super(FUNCTION, building);

		// Add building airlock
		this.airlock = airlock;
	}

	/**
	 * Gets the value of the function for a named building.
	 * @param buildingName the building name.
	 * @param newBuilding true if adding a new building.
	 * @param settlement the settlement.
	 * @return value (VP) of building function.
	 * @throws Exception if error getting function value.
	 */
	public static double getFunctionValue(String buildingName, boolean newBuilding,
			Settlement settlement) {

		// Demand is one airlock capacity for every four inhabitants.
		double demand = settlement.getNumCitizens() / 4D;

		double supply = 0D;
		boolean removedBuilding = false;
		Iterator<Building> i = settlement.getBuildingManager().getBuildings(FUNCTION).iterator();
		while (i.hasNext()) {
			Building building = i.next();
			if (!newBuilding && building.getBuildingType().equalsIgnoreCase(buildingName) && !removedBuilding) {
				removedBuilding = true;
			}
			else {
				double wearModifier = (building.getMalfunctionManager().getWearCondition() / 100D) * .75D + .25D;
				supply += building.getEVA().airlock.getCapacity() * wearModifier;
			}
		}

		double airlockCapacityValue = demand / (supply + 1D);

		// Note: building.getEVA().airlock.getCapacity() is the same as the airlockCapacity below
		double airlockCapacity = buildingConfig.getAirlockCapacity(buildingName);

		return airlockCapacity * airlockCapacityValue;
	}

	public int getAirlockCapacity() {
		return airlockCapacity;
	}
	
	/**
	 * Gets the building's airlock.
	 * @return airlock
	 */
	public Airlock getAirlock() {
		return airlock;
	}

	public int getNumAwaitingInnerDoor() {
		return airlock.getNumAwaitingInnerDoor();
	}
	
	public int getNumAwaitingOuterDoor() {
		return airlock.getNumAwaitingOuterDoor();
	}
	
	public int getNumEmptied() {
		return airlockCapacity - airlock.getNumOccupants();
	}
	
	public int getNumOccupied() {
		return airlock.getNumOccupants();
	}
	
	public String getOperatorName() {
		return airlock.getOperatorName();
	}
	
	/**
	 * Time passing for the building.
	 * @param time amount of time passing (in millisols)
	 * @throws BuildingException if error occurs.
	 */
	@Override
	public boolean timePassing(ClockPulse pulse) {
		boolean valid = isValid(pulse);
		if (valid) {
			airlock.timePassing(pulse.getElapsed());
		}
		return valid;
	}

	/**
	 * Gets the amount of power required when function is at full power.
	 * @return power (kW)
	 */
	public double getFullPowerRequired() {
		return 0.5;
	}

	/**
	 * Gets the amount of power required when function is at power down level.
	 * @return power (kW)
	 */
	public double getPoweredDownPowerRequired() {
		return 0.05;
	}

	@Override
	public double getMaintenanceTime() {
		return airlock.getCapacity() * MAINTENANCE_FACTOR;
	}
	
	@Override
	public void destroy() {
		super.destroy();

		airlock = null;
	}
}
