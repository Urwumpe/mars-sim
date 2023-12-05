/*
 * Mars Simulation Project
 * RoboticStation.java
 * @date 2023-11-20
 * @author Manny Kung
 */
package com.mars_sim.core.structure.building.function;

import java.util.Collection;
import java.util.Iterator;

import com.mars_sim.core.data.UnitSet;
import com.mars_sim.core.logging.SimLogger;
import com.mars_sim.core.robot.Robot;
import com.mars_sim.core.structure.Settlement;
import com.mars_sim.core.structure.building.Building;
import com.mars_sim.core.structure.building.BuildingException;
import com.mars_sim.core.structure.building.BuildingManager;
import com.mars_sim.core.structure.building.FunctionSpec;
import com.mars_sim.core.time.ClockPulse;

/**
 * The RoboticStation class is a building function for a Robotic Station.
 */
public class RoboticStation extends Function {

	/** default serial id. */
	private static final long serialVersionUID = 1L;

	/** default logger. */
	private static final SimLogger logger = SimLogger.getLogger(RoboticStation.class.getName());
	
	/** The charge rate of the bot in kW. */
	public final static double CHARGE_RATE = 10D;

	private int slots;
	private int sleepers;
	private int occupantCapacity;
	
	private double powerToDraw;

	private Collection<Robot> robotOccupants;

	/**
	 * Constructor.
	 * 
	 * @param building the building this function is for.
	 * @param spec Spec of the Robot station
	 * @throws BuildingException if error in constructing function.
	 */
	public RoboticStation(Building building, FunctionSpec spec) {
		// Call Function constructor.
		super(FunctionType.ROBOTIC_STATION, spec, building);

		robotOccupants = new UnitSet<>();
		// Set occupant capacity.
		occupantCapacity = buildingConfig.getFunctionSpec(building.getBuildingType(), FunctionType.ROBOTIC_STATION).getCapacity();

		slots = spec.getCapacity();
	}

	/**
	 * Gets the value of the function for a named building.
	 * 
	 * @param buildingName the building name.
	 * @param newBuilding  true if adding a new building.
	 * @param settlement   the settlement.
	 * @return value (VP) of building function.
	 * @throws Exception if error getting function value.
	 */
	public static double getFunctionValue(String buildingName, boolean newBuilding, Settlement settlement) {

		// Demand is one stations for every robot
		double demand = settlement.getNumBots() * 1D;

		double supply = 0D;
		boolean removedBuilding = false;
		Iterator<Building> i = settlement.getBuildingManager().getBuildingSet(FunctionType.ROBOTIC_STATION).iterator();
		while (i.hasNext()) {
			Building building = i.next();
			if (!newBuilding && building.getBuildingType().equalsIgnoreCase(buildingName) && !removedBuilding) {
				removedBuilding = true;
			} else {
				RoboticStation station = building.getRoboticStation();
				double wearModifier = (building.getMalfunctionManager().getWearCondition() / 100D) * .75D + .25D;
				supply += station.slots * wearModifier;
			}
		}

		double stationCapacityValue = demand / (supply + 1D);
		int stationCapacity = buildingConfig.getFunctionSpec(buildingName, FunctionType.ROBOTIC_STATION).getCapacity();
		return stationCapacity * stationCapacityValue;
	}

	/**
	 * Gets the number of slots in the living accommodations.
	 * 
	 * @return number of slots.
	 */
	public int getSlots() {
		return slots;
	}

	/**
	 * Gets the number of robots sleeping in the stations.
	 * 
	 * @return number of robots
	 */
	public int getSleepers() {
		return sleepers;
	}

	/**
	 * Adds a sleeper to a station.
	 * 
	 * @throws BuildingException if stations are already in use.
	 */
	public void addSleeper() {
		sleepers++;
		if (sleepers > slots) {
			sleepers = slots;
			throw new IllegalStateException("All slots are full.");
		}
	}

	/**
	 * Removes a sleeper from a station.
	 * 
	 * @throws BuildingException if no sleepers to remove.
	 */
	public void removeSleeper() {
		sleepers--;
		if (sleepers < 0) {
			sleepers = 0;
			throw new IllegalStateException("Slots are empty.");
		}
	}

	/**
	 * Time passing for the building.
	 * 
	 * @param time amount of time passing (in millisols)
	 * @throws BuildingException if error occurs.
	 */
	@Override
	public boolean timePassing(ClockPulse pulse) {
		boolean valid = isValid(pulse);
		if (valid) {
			// nothing
		}
		return valid;
	}

    public void setPowerLoad(double value) {
    	powerToDraw += value;
    }
	
	@Override
	public double getMaintenanceTime() {
		return slots * 7D;
	}

	/**
	 * Gets the building's capacity for supporting occupants.
	 * 
	 * @return number of inhabitants.
	 */
	public int getOccupantCapacity() {
		return occupantCapacity;
	}

	/**
	 * Gets a collection of robotOccupants in the building.
	 * 
	 * @return collection of robotOccupants
	 */
	public Collection<Robot> getRobotOccupants() {
		return robotOccupants;
	}

	public int getRobotOccupantNumber() {
		return robotOccupants.size();
	}

	/**
	 * Gets the available occupancy room.
	 * 
	 * @return occupancy room
	 */
	public int getAvailableOccupancy() {
		int available = occupantCapacity - getRobotOccupantNumber();
        return Math.max(available, 0);
	}

	/**
	 * Checks if the building contains a particular unit.
	 * 
	 * @return true if unit is in building.
	 */
	public boolean containsRobotOccupant(Robot robot) {
		return robotOccupants.contains(robot);

	}

	/**
	 * Adds a robot to the building. Note: robot capacity can be exceeded
	 * 
	 * @param robot new robot to add to building.
	 * @throws BuildingException if robot is already building occupant.
	 */
	public void addRobot(Robot robot) {
		if (!robotOccupants.contains(robot)) {

			if (robot.getBuildingLocation() != null) {
				// Remove this person from the old building first
				BuildingManager.removeRobotFromBuilding(robot, robot.getBuildingLocation());
			}
			
			// Add robot to this building.
			logger.fine(robot,  10_000L, "Added to " + getBuilding() + "'s robotic station.");
			
			robotOccupants.add(robot);
		} else {
			throw new IllegalStateException("This robot is already in this building.");
		}
	}

	/**
	 * Removes a robot from the building.
	 * 
	 * @param occupant the robot to remove from building.
	 * @throws BuildingException if robot is not building occupant.
	 */
	public void removeRobot(Robot robot) {
		if (robotOccupants.contains(robot)) {
			robotOccupants.remove(robot);
			logger.fine(robot, 10_000L, "Removed from " + getBuilding() + "'s robotic station.");
		} else {
			throw new IllegalStateException("The robot is not in this building.");
		}
	}

	/**
	 * Gets the amount of power required, based on the current load.
	 *
	 * @return power (kW) default zero
	 */
	@Override
	public double getFullPowerRequired() {
		double power = 0;
		if (powerToDraw > 0) {
			// Set the power load this time to the power load to draw
			power = powerToDraw;
			// Reset it back to zero
			powerToDraw = 0;
		}
		return power;
	}
	
	/**
	 * Prepares object for garbage collection.
	 */
	public void destroy() {
		super.destroy();
		robotOccupants.clear();
		robotOccupants = null;
	}
}
