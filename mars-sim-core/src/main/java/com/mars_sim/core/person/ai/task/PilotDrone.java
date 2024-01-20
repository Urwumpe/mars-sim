/*
 * Mars Simulation Project
 * PilotDrone.java
 * @date 2022-06-17
 * @author Manny
 */
package com.mars_sim.core.person.ai.task;

import java.util.logging.Level;

import com.mars_sim.core.logging.SimLogger;
import com.mars_sim.core.person.Person;
import com.mars_sim.core.person.ai.NaturalAttributeType;
import com.mars_sim.core.person.ai.SkillType;
import com.mars_sim.core.person.ai.task.util.TaskPhase;
import com.mars_sim.core.robot.Robot;
import com.mars_sim.core.structure.building.function.Computation;
import com.mars_sim.core.time.MarsTime;
import com.mars_sim.core.vehicle.Flyer;
import com.mars_sim.mapdata.location.Coordinates;
import com.mars_sim.mapdata.location.Direction;
import com.mars_sim.tools.Msg;
import com.mars_sim.tools.util.RandomUtil;

/**
 * The PilotDrone class is a task for piloting a drone to a
 * destination.
 */
public class PilotDrone extends OperateVehicle {

	/** default serial id. */
	private static final long serialVersionUID = 1L;

	/** default logger. */
	private static final SimLogger logger = SimLogger.getLogger(PilotDrone.class.getName());

	/** Task name */
	private static final String NAME = Msg.getString("Task.description.pilotDrone"); //$NON-NLS-1$

	/** Task phases. */
	private static final TaskPhase AVOID_COLLISION = new TaskPhase(Msg.getString("Task.phase.avoidObstacle")); //$NON-NLS-1$

	/** The speed at which the obstacle / winching phase commence. */
	private static final double LOW_SPEED = .5;
	/** The computing resources [in CUs] needed per km. */
	private static final double CU_PER_KM = .05;
	
	// Side directions.
	private final static int NONE = 0;
	private final static int LEFT = 1;
	private final static int RIGHT = 2;

	// Data members
	private int sideDirection = NONE;
    /** Computing Units used per millisol. */		
	private double computingUsed = 0; 
			
	/**
	 * Default Constructor.
	 * 
	 * @param person            the person to perform the task
	 * @param flyer             the flyer to be driven
	 * @param destination       location to be driven to
	 * @param startTripTime     the starting time of the trip
	 * @param startTripDistance the starting distance to destination for the trip
	 */
	public PilotDrone(Person person, Flyer flyer, Coordinates destination, MarsTime startTripTime,
			double startTripDistance) {

		// Use OperateVehicle constructor
		super(NAME, person, flyer, destination, startTripTime, startTripDistance, 
				150D + RandomUtil.getRandomDouble(10D) - RandomUtil.getRandomDouble(10D));
		
		// Set initial parameters
		setDescription(Msg.getString("Task.description.pilotDrone.detail", flyer.getName())); // $NON-NLS-1$
		addPhase(AVOID_COLLISION);

		logger.info(person, 20_000, "Took control of the drone " + flyer.getName());
	}

	public PilotDrone(Robot robot, Flyer flyer, Coordinates destination, MarsTime startTripTime,
			double startTripDistance) {

		// Use OperateVehicle constructor
		super(NAME, robot, flyer, destination, startTripTime, startTripDistance, 1000D);
		
		// Set initial parameters
		setDescription(Msg.getString("Task.description.pilotDrone.detail", flyer.getName())); // $NON-NLS-1$
		addPhase(AVOID_COLLISION);

		logger.info(robot, 20_000, "Took control of the drone " + flyer.getName());
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
	public PilotDrone(Person person, Flyer flyer, Coordinates destination, MarsTime startTripTime,
			double startTripDistance, TaskPhase startingPhase) {

		// Use OperateVehicle constructor
		super(NAME, person, flyer, destination, startTripTime, startTripDistance, 100D + RandomUtil.getRandomDouble(-20D, 20D));
		
		// Set initial parameters
		setDescription(Msg.getString("Task.description.pilotDrone.detail", flyer.getName())); // $NON-NLS-1$
		addPhase(AVOID_COLLISION);

		if (startingPhase != null)
			setPhase(startingPhase);

		logger.log(person, Level.INFO, 20_000, "Took control of the drone at phase '"
					+ startingPhase + "'.");

	}

	public PilotDrone(Robot robot, Flyer flyer, Coordinates destination, MarsTime startTripTime,
			double startTripDistance, TaskPhase startingPhase) {

		// Use OperateVehicle constructor
		super(NAME, robot, flyer, destination, startTripTime, startTripDistance, 250D);
		
		// Set initial parameters
		setDescription(Msg.getString("Task.description.pilotDrone.detail", flyer.getName())); // $NON-NLS-1$
		addPhase(AVOID_COLLISION);

		if (startingPhase != null)
			setPhase(startingPhase);

		logger.log(robot, Level.INFO, 20_000, "Took control of the drone at phase '"
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
			logger.log(worker, Level.INFO, 10_000, "Had an unknown phase when piloting");
			// If it called endTask() in OperateVehicle, then Task is no longer available
			// WARNING: do NOT call endTask() here or it will end up calling endTask() 
			// recursively.
			return time;		
			
		} else if (AVOID_COLLISION.equals(getPhase())) {
			return obstaclePhase(time);
		} else {
			return time;
		}
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
		double destinationSpeed = testSpeed(destinationDirection);
		
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
		Direction travelDirection = getObstacleAvoidanceDirection(time);

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
		flyer.setSpeed(testSpeed(flyer.getDirection()));

		// Drive in the direction
		timeUsed = time - mobilizeVehicle(time);
		
		int msol = getMarsTime().getMillisolInt();       
        boolean successful = false; 
        
        double lastDistance = flyer.getLastDistanceTravelled();
        double workPerMillisol = lastDistance * CU_PER_KM * time;
        
    	// Submit his request for computing resources
    	Computation center = person.getAssociatedSettlement().getBuildingManager().getMostFreeComputingNode(workPerMillisol, msol + 1, msol + 2);
    	if (center != null)
    		successful = center.scheduleTask(workPerMillisol, msol + 1, msol + 2);
    	if (successful) {
    		computingUsed += timeUsed;
      	}
    	else {
    		logger.info(person, 30_000L, "No computing resources available for " 
    			+ Msg.getString("Task.description.pilotDrone.detail", // $NON-NLS-1$
    					flyer.getName()) + "."); 
    	}
		
		// Add experience points
		addExperience(timeUsed);

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
	private Direction getObstacleAvoidanceDirection(double time) {
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
					double testSpeed = testSpeed(testDirection);
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
				double testSpeed = testSpeed(testDirection);
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
		// Not needed for some reason !!
	}
	
	/**
	 * Update vehicle with its current elevation or altitude.
	 */
	protected void updateVehicleElevationAltitude(boolean horizontalMovement, double time) {
		int mod = 1;
		if (!horizontalMovement)
			mod = 4;
			
		double currentE = ((Flyer)getVehicle()).getHoveringHeight();
		double oldGroundE = ((Flyer)getVehicle()).getElevation();
		double newGroundE = getGroundElevation();
		
		double ascentE = (Flyer.ELEVATION_ABOVE_GROUND - currentE) + (newGroundE - oldGroundE);
		double climbE = 0;
		
		if (ascentE > 0) {
			// Future: Use Newton's law to determine the amount of height the flyer can climb 
			double tSec = time * MarsTime.SECONDS_PER_MILLISOL;
			double speed = .0025 * mod;
			climbE = speed * tSec;
			
		}
		else if (ascentE < 0) {
			// Future: Use Newton's law to determine the amount of height the flyer can climb 
			double tSec = time * MarsTime.SECONDS_PER_MILLISOL;
			double speed = -.02 * mod;
			climbE = speed * tSec;
		}
		
		double elev = climbE + oldGroundE;
		((Flyer) getVehicle()).setElevation(elev);
	}

	/**
	 * Check if vehicle has had an accident.
	 * 
	 * @param time the amount of time vehicle is driven (millisols)
	 */
	@Override
	protected void checkForAccident(double time) {
	}

	/**
	 * Adds experience to the worker skills used in this task.
	 * 
	 * @param time the amount of time (ms) the person performed this task.
	 */
	@Override
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
	@Override
	protected void clearDown() {
		if (getVehicle() != null) {
		    // Need to set the vehicle operator to null before clearing the driving task 
	        getVehicle().setOperator(null);
		}
	}
}
