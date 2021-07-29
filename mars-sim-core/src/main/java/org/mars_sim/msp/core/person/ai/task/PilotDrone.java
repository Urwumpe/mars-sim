/**
 * Mars Simulation Project
 * PilotDrone.java
 * @version 3.2.0 2021-06-20
 * @author Manny
 */

package org.mars_sim.msp.core.person.ai.task;

import java.io.Serializable;
import java.util.logging.Level;

import org.mars_sim.msp.core.Coordinates;
import org.mars_sim.msp.core.Direction;
import org.mars_sim.msp.core.Msg;
import org.mars_sim.msp.core.logging.SimLogger;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.ai.NaturalAttributeType;
import org.mars_sim.msp.core.person.ai.SkillType;
import org.mars_sim.msp.core.person.ai.task.utils.TaskPhase;
import org.mars_sim.msp.core.robot.Robot;
import org.mars_sim.msp.core.time.ClockUtils;
import org.mars_sim.msp.core.time.MarsClock;
import org.mars_sim.msp.core.tool.RandomUtil;
import org.mars_sim.msp.core.vehicle.Flyer;

/**
 * The PilotDrone class is a task for piloting a drone to a
 * destination.
 */
public class PilotDrone extends OperateVehicle implements Serializable {

	/** default serial id. */
	private static final long serialVersionUID = 1L;

	/** default logger. */
	private static final SimLogger logger = SimLogger.getLogger(PilotDrone.class.getName());

	/** Task name */
	private static final String NAME = Msg.getString("Task.description.pilotDrone"); //$NON-NLS-1$

	/** Task phases. */
	private static final TaskPhase AVOID_COLLISION = new TaskPhase(Msg.getString("Task.phase.avoidObstacle")); //$NON-NLS-1$
//	private static final TaskPhase WINCH_VEHICLE = new TaskPhase(Msg.getString("Task.phase.winchVehicle")); //$NON-NLS-1$

	/** The stress modified per millisol. */
	private static final double STRESS_MODIFIER = .2D;
	/** Half the PI. */
	private static final double HALF_PI = Math.PI / 2D;
	/** The speed at which the obstacle / winching phase commence. */
	private static final double LOW_SPEED = .5;
	
	// Side directions.
	private final static int NONE = 0;
	private final static int LEFT = 1;
	private final static int RIGHT = 2;

	
	// Data members
	private int sideDirection = NONE;
	
	/**
	 * Default Constructor.
	 * 
	 * @param person            the person to perform the task
	 * @param flyer             the flyer to be driven
	 * @param destination       location to be driven to
	 * @param startTripTime     the starting time of the trip
	 * @param startTripDistance the starting distance to destination for the trip
	 */
	public PilotDrone(Person person, Flyer flyer, Coordinates destination, MarsClock startTripTime,
			double startTripDistance) {

		// Use OperateVehicle constructor
		super(NAME, person, flyer, destination, startTripTime, startTripDistance, STRESS_MODIFIER, 
				150D + RandomUtil.getRandomDouble(10D) - RandomUtil.getRandomDouble(10D));
		
		// Set initial parameters
		setDescription(Msg.getString("Task.description.pilotDrone.detail", flyer.getName())); // $NON-NLS-1$
		addPhase(AVOID_COLLISION);
//		addPhase(WINCH_VEHICLE);

		logger.log(flyer, person, Level.INFO, 20_000, "Took control of the drone.");
	}

	public PilotDrone(Robot robot, Flyer flyer, Coordinates destination, MarsClock startTripTime,
			double startTripDistance) {

		// Use OperateVehicle constructor
		super(NAME, robot, flyer, destination, startTripTime, startTripDistance, STRESS_MODIFIER, true,
				1000D);
		
		// Set initial parameters
		setDescription(Msg.getString("Task.description.pilotDrone.detail", flyer.getName())); // $NON-NLS-1$
		addPhase(AVOID_COLLISION);
//		addPhase(WINCH_VEHICLE);

		logger.log(flyer, robot, Level.INFO, 20_000, "Took control of the drone.");
	}

	/**
	 * Constructs with a given starting phase.
	 * 
	 * @param person            the person to perform the task
	 * @param vehicle           the vehicle to be driven
	 * @param destination       location to be driven to
	 * @param startTripTime     the starting time of the trip
	 * @param startTripDistance the starting distance to destination for the trip
	 * @param startingPhase     the starting phase for the task
	 */
	public PilotDrone(Person person, Flyer flyer, Coordinates destination, MarsClock startTripTime,
			double startTripDistance, TaskPhase startingPhase) {

		// Use OperateVehicle constructor
		super(NAME, person, flyer, destination, startTripTime, startTripDistance, STRESS_MODIFIER, 
				150D + RandomUtil.getRandomDouble(10D) - RandomUtil.getRandomDouble(10D));
		
		// Set initial parameters
		setDescription(Msg.getString("Task.description.pilotDrone.detail", flyer.getName())); // $NON-NLS-1$
		addPhase(AVOID_COLLISION);
//		addPhase(WINCH_VEHICLE);
		if (startingPhase != null)
			setPhase(startingPhase);

		logger.log(person, Level.INFO, 20_000, "Took control of the drone at phase '"
					+ startingPhase + "'.");

	}

	public PilotDrone(Robot robot, Flyer flyer, Coordinates destination, MarsClock startTripTime,
			double startTripDistance, TaskPhase startingPhase) {

		// Use OperateVehicle constructor
		super(NAME, robot, flyer, destination, startTripTime, startTripDistance, STRESS_MODIFIER, true,
				1000D);
		
		// Set initial parameters
		setDescription(Msg.getString("Task.description.pilotDrone.detail", flyer.getName())); // $NON-NLS-1$
		addPhase(AVOID_COLLISION);
//		addPhase(WINCH_VEHICLE);
		if (startingPhase != null)
			setPhase(startingPhase);

		logger.log(robot, Level.INFO, 0, "Took control of the drone at phase '"
					+ startingPhase + "'.");
	}

	/**
	 * Performs the method mapped to the task's current phase.
	 * 
	 * @param time the amount of time the phase is to be performed.
	 * @return the remaining time after the phase has been performed.
	 */
	protected double performMappedPhase(double time) {

		time = super.performMappedPhase(time);

		if (getPhase() == null) {
//			throw new IllegalArgumentException("Task phase is null");
			logger.log(worker, Level.INFO, 10_000, "Had an unknown phase when piloting");
			// If it called endTask() in OperateVehicle, then Task is no longer available
			// WARNING: do NOT call endTask() here or it will end up calling endTask() 
			// recursively.
			return time;		
			
		} else if (AVOID_COLLISION.equals(getPhase())) {
			return obstaclePhase(time);
//		} else if (WINCH_VEHICLE.equals(getPhase())) {
//			return winchingPhase(time);
		} else {
			return time;
		}
	}

	/**
	 * Move the vehicle in its direction at its speed for the amount of time given.
	 * Stop if reached destination.
	 * 
	 * @param time the amount of time (ms) to drive.
	 * @return the amount of time (ms) left over after driving (if any)
	 */
	protected double mobilizeVehicle(double time) {

		// If vehicle is stuck, try winching.
//		if ((Drone) getDrone().isStalled()) {// && (!WINCH_VEHICLE.equals(getPhase()))) {
////			setPhase(WINCH_VEHICLE);
//			return (time);
//		}

		// If speed is less than or equal to the .5 kph, change to avoiding obstacle phase.
		if ((getVehicle().getSpeed() <= LOW_SPEED) 
				&& !AVOID_COLLISION.equals(getPhase())) {
//				&& (!WINCH_VEHICLE.equals(getPhase()))) {
			setPhase(AVOID_COLLISION);
			return (time);
		} else
			return super.mobilizeVehicle(time);
	}

	/**
	 * Perform task in obstacle phase.
	 * 
	 * @param time the amount of time to perform the task (in millisols)
	 * @return time remaining after performing phase (in millisols)
	 */
	private double obstaclePhase(double time) {

		double timeUsed = 0D;
		Flyer flyer = (Flyer) getVehicle();

		// Get the direction to the destination.
		Direction destinationDirection = flyer.getCoordinates().getDirectionToPoint(getDestination());

		// If speed in destination direction is good, change to mobilize phase.
		double destinationSpeed = getSpeed(destinationDirection);
		
		if (destinationSpeed > LOW_SPEED) {
			// Set new direction
			flyer.setDirection(destinationDirection);
			// Update vehicle elevation.
			updateVehicleElevationAltitude(true, time);
			
			setPhase(PilotDrone.MOBILIZE);
			sideDirection = NONE;
			return time;
		}

		// Determine the direction to avoid the obstacle.
		Direction travelDirection = getObstacleAvoidanceDirection();

		// If an direction could not be found, change the elevation
		if (travelDirection == null) {
			// Update vehicle elevation.
			updateVehicleElevationAltitude(false, time);
			
			sideDirection = NONE;
			return time;
		}

		// Set the vehicle's direction.
		flyer.setDirection(travelDirection);

		// Update vehicle speed.
		flyer.setSpeed(getSpeed(flyer.getDirection()));

		// Drive in the direction
		timeUsed = time - mobilizeVehicle(time);

		// Add experience points
		addExperience(time);

		// Check for accident.
//		if (!isDone())
//			checkForAccident(timeUsed);

		// If vehicle has malfunction, end task.
		if (flyer.getMalfunctionManager().hasMalfunction())
			endTask();

		return time - timeUsed;
	}


	/**
	 * Gets the direction for obstacle avoidance.
	 * 
	 * @return direction for obstacle avoidance in radians or null if none found.
	 */
	private Direction getObstacleAvoidanceDirection() {
		Direction result = null;

		Flyer flyer = (Flyer) getVehicle();
		boolean foundGoodPath = false;

		double initialDirection = flyer.getCoordinates().getDirectionToPoint(getDestination()).getDirection();

		if (sideDirection == NONE) {
			for (int x = 1; (x < 11) && !foundGoodPath; x++) {
				double modAngle = x * (Math.PI / 10D);
				for (int y = 1; (y < 3) && !foundGoodPath; y++) {
					Direction testDirection = null;
					if (y == 1)
						testDirection = new Direction(initialDirection - modAngle);
					else
						testDirection = new Direction(initialDirection + modAngle);
					double testSpeed = getSpeed(testDirection);
					if (testSpeed > 1D) {
						result = testDirection;
						if (y == 1)
							sideDirection = LEFT;
						else
							sideDirection = RIGHT;
						foundGoodPath = true;
					}
				}
			}
		} else {
			for (int x = 1; (x < 21) && !foundGoodPath; x++) {
				double modAngle = x * (Math.PI / 10D);
				Direction testDirection = null;
				if (sideDirection == LEFT)
					testDirection = new Direction(initialDirection - modAngle);
				else
					testDirection = new Direction(initialDirection + modAngle);
				double testSpeed = getSpeed(testDirection);
				if (testSpeed > 1D) {
					result = testDirection;
					foundGoodPath = true;
				}
			}
		}

		return result;
	}

	@Override
	protected void updateVehicleElevationAltitude() {
		// TODO Auto-generated method stub
	}
	
	/**
	 * Update vehicle with its current elevation or altitude.
	 */
	protected void updateVehicleElevationAltitude(boolean horizontalMovement, double time) {
		int mod = 1;
		if (!horizontalMovement)
			mod = 4;
			
		double currentE = ((Flyer)getVehicle()).getHoveringElevation();
		double oldGroundE = ((Flyer)getVehicle()).getElevation();
		double newGroundE = getGroundElevation();
		
		double ascentE = (Flyer.ELEVATION_ABOVE_GROUND - currentE) + (newGroundE - oldGroundE);
		
		if (ascentE > 0) {
			// TODO: Use Newton's law to determine the amount of height the flyer can climb 
			double tSec = time * ClockUtils.SECONDS_PER_MILLISOL;
			double speed = .0025 * mod;
			double climbE = speed * tSec;
			
			((Flyer) getVehicle()).setElevation(climbE + oldGroundE);
		}
		else if (ascentE < 0) {
			// TODO: Use Newton's law to determine the amount of height the flyer can climb 
			double tSec = time * ClockUtils.SECONDS_PER_MILLISOL;
			double speed = -.02 * mod;
			double climbE = speed * tSec;
				
			((Flyer) getVehicle()).setElevation(climbE + oldGroundE);
		}
	}

	/**
	 * Determine vehicle speed for a given direction.
	 * 
	 * @param direction the direction of travel
	 * @return speed in km/hr
	 */
	@Override
	protected double getSpeed(Direction direction) {
		double result = super.getSpeed(direction);
		double lightModifier = getSpeedLightConditionModifier();
		double terrainModifer = getTerrainModifier(direction);
		logger.warning(getVehicle(), " terrainModifer: " + terrainModifer);
		
		result = result * lightModifier * terrainModifer;
		if (Double.isNaN(result)) {
			// Temp to track down driving problem
			logger.warning(getVehicle(), "getSpeed isNaN: light=" + lightModifier);
//					        + ", terrain=" + terrainModifer);
		}
		
		return result;
	}

	/**
	 * Gets the lighting condition speed modifier.
	 * 
	 * @return speed modifier
	 */
	protected double getSpeedLightConditionModifier() {
		// Ground vehicles travel at 30% speed at night.
		double light = surfaceFeatures.getSolarIrradiance(getVehicle().getCoordinates());
		if (light >= 30)
			return 1;
		else //if (light > 0 && light <= 30)
			return light/37.5 + .2;
	}

	/**
	 * Gets the terrain steepness speed modifier.
	 * 
	 * @param direction the direction of travel.
	 * @return speed modifier (0D - 1D)
	 */
	protected double getTerrainModifier(Direction direction) {
		Flyer vehicle = (Flyer) getVehicle();

		// Determine modifier.
		double angleModifier = getEffectiveSkillLevel();
		if (angleModifier < 0D)
			angleModifier = Math.abs(1D / angleModifier);
		else if (angleModifier == 0D) {
			// Will produce a divide by zero otherwise
			angleModifier = 1D;
		}
		double tempAngle = Math.abs(vehicle.getTerrainGrade(direction) / angleModifier);
		if (tempAngle > HALF_PI)
			tempAngle = HALF_PI;
		return Math.cos(tempAngle);
	}

	/**
	 * Check if vehicle has had an accident.
	 * 
	 * @param time the amount of time vehicle is driven (millisols)
	 */
	protected void checkForAccident(double time) {

//		Flyer vehicle = (Flyer) getVehicle();
//
//		double chance = PilotDrone.BASE_ACCIDENT_CHANCE;
//
//		// Driver skill modification.
//		int skill = getEffectiveSkillLevel();
//		if (skill <= 3)
//			chance *= (4 - skill);
//		else
//			chance /= (skill - 2);
//
//		// Get task phase modification.
//		if (AVOID_COLLISION.equals(getPhase()))
//			chance *= 1.2D;
////		else if (WINCH_VEHICLE.equals(getPhase()))
////			chance *= 1.3D;
//
////		// Terrain modification.
////		chance *= (1D + Math.sin(vehicle.getTerrainGrade()));
////
////		// Vehicle handling modification.
////		chance /= (1D + vehicle.getTerrainHandlingCapability());
//
//		// Light condition modification.
//		double lightConditions = surfaceFeatures.getSunlightRatio(vehicle.getCoordinates());
//		chance *= (5D * (1D - lightConditions)) + 1D;
//		if (chance < 0D) {
//			chance = 0D;
//		}
//
//		// if (malfunctionManager == null)
//		MalfunctionManager malfunctionManager = vehicle.getMalfunctionManager();
//		// Modify based on the vehicle's wear condition.
//		chance *= malfunctionManager.getWearConditionAccidentModifier();
//
//		if (RandomUtil.lessThanRandPercent(chance * time)) {
//			malfunctionManager.createASeriesOfMalfunctions(vehicle.getName(), worker);
//		}
	}


	/**
	 * Adds experience to the worker skills used in this task.
	 * 
	 * @param time the amount of time (ms) the person performed this task.
	 */
	protected void addExperience(double time) {
		// Add experience points for driver's 'Driving' skill.
		// Add one point for every 100 millisols.
		double newPoints = time / 100D;
		int experienceAptitude = worker.getNaturalAttributeManager()
					.getAttribute(NaturalAttributeType.EXPERIENCE_APTITUDE);

		newPoints += newPoints * ((double) experienceAptitude - 50D) / 100D;
		newPoints *= getTeachingExperienceModifier();
		double phaseModifier = 1D;
		if (AVOID_COLLISION.equals(getPhase()))
			phaseModifier = 4D;
		newPoints *= phaseModifier;
		worker.getSkillManager().addExperience(SkillType.PILOTING, newPoints, time);
	}

	/**
	 * Stop the vehicle
	 */
	protected void clearDown() {
		if (getVehicle() != null) {
//			getVehicle().setSpeed(0D);
		    // Need to set the vehicle operator to null before clearing the driving task 
	        getVehicle().setOperator(null);
//        	System.out.println("just called setOperator(null) in PilotDrone:clearDown");
		}
	}
}
