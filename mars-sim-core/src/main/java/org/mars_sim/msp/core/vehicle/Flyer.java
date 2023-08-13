/*
 * Mars Simulation Project
 * Flyer.java
 * @date 2023-06-05
 * @author Manny
 */

package org.mars_sim.msp.core.vehicle;

import java.util.List;

import org.mars.sim.mapdata.location.Direction;
import org.mars.sim.mapdata.location.LocalPosition;
import org.mars.sim.tools.util.RandomUtil;
import org.mars_sim.msp.core.LocalAreaUtil;
import org.mars_sim.msp.core.environment.TerrainElevation;
import org.mars_sim.msp.core.logging.SimLogger;
import org.mars_sim.msp.core.structure.Settlement;
import org.mars_sim.msp.core.structure.building.Building;
import org.mars_sim.msp.core.structure.building.BuildingCategory;
import org.mars_sim.msp.core.structure.building.BuildingManager;
import org.mars_sim.msp.core.structure.building.function.FunctionType;

/**
 * The Flyer class represents an airborne.
 */
public abstract class Flyer extends Vehicle {

	/** default serial id. */
	private static final long serialVersionUID = 1L;

	// default logger.
	private static final SimLogger logger = SimLogger.getLogger(Flyer.class.getName());
	
	/** Comparison to indicate a small but non-zero amount of fuel (methane) in kg that can still work on the fuel cell to propel the engine. */
    public static final double LEAST_AMOUNT = .001D;
    
    /** Ideal hovering elevation. */
	public final static double ELEVATION_ABOVE_GROUND = .1; // in km
	
	// Data members
	/** Current total elevation above the sea level in km. */
	private double elevation;

	/** Current hovering height in km. */
	private double hoveringHeight;

//	/** Current Angle of Attack in degree. */
//	private double AoA;

	// NASA Space Shuttle Fuel Cell Power Plant 7.6 kg/kW
	// The targeted space systems feature power outputs of 1 to 10 kW systems 
	// (eventually scalable up to 100 kW), compact sizing (250 to 350 watts per kg),
	// and high reliability for long lives (10,000 hours).
	
	/**
	 * Constructs a {@link Flyer} object at a given settlement.
	 * 
	 * @param name                name of the airborne vehicle
	 * @param spec         the configuration description of the vehicle.
	 * @param settlement          settlement the airborne vehicle is parked at
	 * @param maintenanceWorkTime the work time required for maintenance (millisols)
	 */
	protected Flyer(String name, VehicleSpec spec, Settlement settlement, double maintenanceWorkTime) {
		// use Vehicle constructor
		super(name, spec, settlement, maintenanceWorkTime);
	}

	/**
	 * Returns the hovering height of the vehicle above ground [in km].
	 * 
	 * @return height
	 */
	public double getHoveringHeight() {
		return hoveringHeight;
	}

	/**
	 * Sets the hovering height of the vehicle above ground [in km].
	 * 
	 * @param height 
	 */
	public void setHoveringHeight(double height) {
		this.hoveringHeight = height;
	}

	/**
	 * Returns the elevation of the vehicle [in km].
	 * 
	 * @return elevation
	 */
	public double getElevation() {
		return elevation;
	}

	/**
	 * Sets the elevation of the vehicle [in km].
	 * 
	 * @param elevation
	 */
	public void setElevation(double elevation) {
		this.elevation = elevation;
	}

	/**
	 * Gets the average angle of attack over over a sample distance in direction
	 * vehicle is traveling.
	 * 
	 * @return airborne vehicle's current angle of attack in radians from horizontal plane
	 */
	public double getAngleOfAttack() {
		return getTerrainGrade();
	}

	/**
	 * Gets the average angle of terrain over over a sample distance in direction
	 * vehicle is traveling.
	 * 
	 * @return vehicle's current terrain grade angle from horizontal
	 *         (radians)
	 */
	public double getTerrainGrade() {
		return getTerrainGrade(getDirection());
	}

	/**
	 * Gets the average angle of terrain over over a sample distance in a given
	 * direction from the vehicle.
	 * 
	 * @return vehicle's current terrain grade angle from horizontal
	 *         (radians)
	 */
	public double getTerrainGrade(Direction direction) {
		// Determine the terrain grade in a given direction from the vehicle.
		return TerrainElevation.determineTerrainSteepness(getCoordinates(), direction);
	}
	
	/**
	 * Find a new parking location and facing
	 */
	@Override
	public void findNewParkingLoc() {

		Settlement settlement = getSettlement();
		if (settlement == null) {
			logger.severe(this, "Not found to be parked in a settlement.");
		}

		else {
			LocalPosition centerLoc = LocalPosition.DEFAULT_POSITION;
			
			// Place the vehicle starting from the settlement center (0,0).

			int oX = 15;
			int oY = 0;

			int weight = 2;

			List<Building> evas = settlement.getBuildingManager().getBuildingsOfSameCategory(BuildingCategory.EVA_AIRLOCK);
			int numGarages = settlement.getBuildingManager().getBuildingSet(FunctionType.VEHICLE_MAINTENANCE)
					.size();
			int total = (int)(evas.size() + numGarages * weight - 1);
			if (total < 0)
				total = 0;
			int rand = RandomUtil.getRandomInt(total);

			if (rand != 0) {

				// Try parking near the eva for short walk
				if (rand < evas.size()) {
					Building eva = evas.get(rand);
					centerLoc = eva.getPosition();
				}

				else {
					// Try parking near a garage
					
					Building garage = BuildingManager.getAGarage(getSettlement());
					centerLoc = garage.getPosition();
				}
			}

			double newFacing = 0D;
			LocalPosition newLoc = null;
			double step = 10D;
			boolean foundGoodLocation = false;

			// Try iteratively outward from 10m to 500m distance range.
			for (int x = oX; (x < 500) && !foundGoodLocation; x += step) {
				// Try ten random locations at each distance range.
				for (int y = oY; (y < step) && !foundGoodLocation; y++) {
					double distance = RandomUtil.getRandomDouble(step) + x;
					double radianDirection = RandomUtil.getRandomDouble(Math.PI * 2D);
					
					newLoc = centerLoc.getPosition(distance, radianDirection);
					newFacing = RandomUtil.getRandomDouble(360D);

					// Check if new vehicle location collides with anything.
					foundGoodLocation = 
							LocalAreaUtil.isObjectCollisionFree(this, this.getWidth() * 1.3, this.getLength() * 1.3, newLoc.getX(),
							newLoc.getY(), newFacing, getCoordinates());
					// Note: Enlarge the collision surface of a vehicle to avoid getting trapped within those enclosed space 
					// surrounded by buildings or hallways.
					// This is just a temporary solution to stop the vehicle from acquiring a parking between buildings.
					// TODO: need a permanent solution by figuring out how to detect those enclosed space
				}
			}

			setParkedLocation(newLoc, newFacing);
		}
	}
}
