/**
 * Mars Simulation Project
 * Function.java
 * @version 3.1.2 2020-09-02
 * @author Scott Davis
 */
package org.mars_sim.msp.core.structure.building.function;

import java.awt.geom.Point2D;
import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

import org.mars_sim.msp.core.LocalAreaUtil;
import org.mars_sim.msp.core.Simulation;
import org.mars_sim.msp.core.SimulationConfig;
import org.mars_sim.msp.core.Unit;
import org.mars_sim.msp.core.UnitManager;
import org.mars_sim.msp.core.mars.Mars;
import org.mars_sim.msp.core.mars.SurfaceFeatures;
import org.mars_sim.msp.core.mars.Weather;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.PersonConfig;
import org.mars_sim.msp.core.robot.Robot;
import org.mars_sim.msp.core.structure.Settlement;
import org.mars_sim.msp.core.structure.building.Building;
import org.mars_sim.msp.core.structure.building.BuildingConfig;
import org.mars_sim.msp.core.time.ClockPulse;
import org.mars_sim.msp.core.time.MarsClock;
import org.mars_sim.msp.core.time.MasterClock;
import org.mars_sim.msp.core.time.Temporal;
import org.mars_sim.msp.core.tool.RandomUtil;

/**
 * A settlement building function.
 */
public abstract class Function implements Serializable, Temporal {

	/** default serial id. */
	private static final long serialVersionUID = 1L;
	/** default logger. */
	private static Logger logger = Logger.getLogger(Function.class.getName());
	
	private FunctionType type;
	protected Building building;
	private List<Point2D> activitySpots;
	private List<Point2D> bedLocations;

	private long lastPulse = 0; // First initial pulse is always 1
	
	protected static BuildingConfig buildingConfig;
	protected static MarsClock marsClock;
	protected static PersonConfig personConfig;
	protected static Weather weather;
	protected static SurfaceFeatures surface;
	protected static UnitManager unitManager;

	/**
	 * Constructor.
	 * 
	 * @param type {@link FunctionType}
	 * @param      {@link Building}
	 */
	protected Function(FunctionType type, Building building) {
		this.type = type;
		this.building = building;
	}

	
	/**
	 * Gets the function.
	 * 
	 * @return {@link FunctionType}
	 */
	public FunctionType getFunctionType() {
		return type;
	}

	/**
	 * Gets the function's building.
	 * 
	 * @return {@link Building}
	 */
	public Building getBuilding() {
		return building;
	}

	/**
	 * Gets the maintenance time for this building function.
	 * 
	 * @return maintenance work time (millisols). Default zero
	 */
	public double getMaintenanceTime() {
		return 0;
	}

	/**
	 * Gets the function's malfunction scope strings.
	 * 
	 * @return array of scope strings.
	 */
	public String[] getMalfunctionScopeStrings() {
		String[] result = { type.getName() };
		return result;
	}

	/**
	 * Is this time pulse valid for the Unit.Has it been already applied?
	 * The logic on this method can be commented out later on
	 * @param pulse Pulse to apply
	 * @return Valid to accept
	 */
	protected boolean isValid(ClockPulse pulse) {
		long newPulse = pulse.getId();
		boolean result = (newPulse > lastPulse);
		if (!result) {
			// Seen already
			logger.severe(type + "@" + building.getName() + " rejected pulse #" + newPulse
						+ ", last pulse was " + lastPulse);
		}
		lastPulse = newPulse;
		return result;
	}
	
	/**
	 * Time passing for the function. By default this does nothing.
	 * 
	 * @param time amount of time passing (in millisols)
	 */
	public boolean timePassing(ClockPulse pulse) {
		return true;
	}

	/**
	 * Gets the amount of heat required when function is at full heat.
	 * 
	 * @return heat (kW) default 0
	 */
	public double getFullHeatRequired() {
		return 0;
	}
	/**
	 * Gets the amount of heat required when function is at heat down level.
	 * 
	 * @return heat (kW) default zero
	 */
	public double getPoweredDownHeatRequired() {
		return 0;
	}

	/**
	 * Gets the amount of power required when function is at full power.
	 * 
	 * @return power (kW) default zero
	 */
	public double getFullPowerRequired() {
		return 0;
	}

	/**
	 * Gets the amount of power required when function is at power down level.
	 * 
	 * @return power (kW) default zero
	 */
	public double getPoweredDownPowerRequired() {
		return 0;
	}

	/**
	 * Perform any actions needed when removing this building function from the
	 * settlement.
	 */
	public void removeFromSettlement() {
		// Override as needed.
	}

	/**
	 * Loads activity spots into the building function.
	 * 
	 * @param newActivitySpots activity spots to add.
	 */
	protected void loadActivitySpots(Collection<Point2D> newActivitySpots) {

		if (activitySpots == null) {
			activitySpots = new CopyOnWriteArrayList<Point2D>(newActivitySpots);
		} else {
			activitySpots.addAll(newActivitySpots);
		}
	}

	/**
	 * Loads bed locations into the building function.
	 * 
	 * @param newBedLocations bed locations to add.
	 */
	protected void loadBedLocations(Collection<Point2D> newBedLocations) {

		if (bedLocations == null) {
			bedLocations = new CopyOnWriteArrayList<Point2D>(newBedLocations);
		} else {
			bedLocations.addAll(newBedLocations);
		}
	}
	
	/**
	 * Gets an available activity spot for the person.
	 * 
	 * @param person the person looking for the activity spot.
	 * @return activity spot as {@link Point2D} or null if none found.
	 */
	public Point2D getAvailableActivitySpot(Person person) {

		Point2D result = null;

		if (activitySpots != null) {

			List<Point2D> availableActivitySpots = new CopyOnWriteArrayList<Point2D>();
			Iterator<Point2D> i = activitySpots.iterator();
			while (i.hasNext()) {
				Point2D activitySpot = i.next();
				// Convert activity spot from building local to settlement local.
				Point2D settlementActivitySpot = LocalAreaUtil.getLocalRelativeLocation(activitySpot.getX(),
						activitySpot.getY(), getBuilding());

				// Check if spot is unoccupied.
				boolean available = true;
				Settlement settlement = getBuilding().getSettlement();
				Iterator<Person> j = settlement.getIndoorPeople().iterator();
				while (j.hasNext() && available) {
					Person tempPerson = j.next();
					if (!tempPerson.equals(person)) {

						// Check if person's location is very close to activity spot.
						Point2D personLoc = new Point2D.Double(tempPerson.getXLocation(), tempPerson.getYLocation());
						if (LocalAreaUtil.areLocationsClose(settlementActivitySpot, personLoc)) {
							available = false;
						}
					}
				}

				// If available, add activity spot to available list.
				if (available) {
					availableActivitySpots.add(settlementActivitySpot);
				}
			}

			if (!availableActivitySpots.isEmpty()) {

				// Choose a random available activity spot.
				int index = RandomUtil.getRandomInt(availableActivitySpots.size() - 1);
				result = availableActivitySpots.get(index);
			}
		}

		return result;
	}

	/**
	 * Gets an available activity spot for the robot.
	 * 
	 * @param robot the bot looking for the activity spot.
	 * @return activity spot as {@link Point2D} or null if none found.
	 */
	public Point2D getAvailableActivitySpot(Robot robot) {

		Point2D result = null;

		if (activitySpots != null) {

			if (isAtActivitySpot(robot)) {
				result = new Point2D.Double(robot.getXLocation(), robot.getYLocation());
			} else {
				List<Point2D> availableActivitySpots = new CopyOnWriteArrayList<Point2D>();
				Iterator<Point2D> i = activitySpots.iterator();
				while (i.hasNext()) {
					Point2D activitySpot = i.next();
					// Convert activity spot from building local to settlement local.
					Point2D settlementActivitySpot = LocalAreaUtil.getLocalRelativeLocation(activitySpot.getX(),
							activitySpot.getY(), getBuilding());

					// Check if spot is unoccupied.
					boolean available = true;
					Settlement settlement = getBuilding().getSettlement();
					Iterator<Robot> j = settlement.getRobots().iterator();
					while (j.hasNext() && available) {
						Robot tempRobot = j.next();
						if (!tempRobot.equals(robot)) {

							// Check if robot's location is very close to activity spot.
							Point2D robotLoc = new Point2D.Double(tempRobot.getXLocation(), tempRobot.getYLocation());
							if (LocalAreaUtil.areLocationsClose(settlementActivitySpot, robotLoc)) {
								available = false;
							}
						}
					}

					// If available, add activity spot to available list.
					if (available) {
						availableActivitySpots.add(settlementActivitySpot);
					}
				}

				if (!availableActivitySpots.isEmpty()) {

					// Choose a random available activity spot.
					int index = RandomUtil.getRandomInt(availableActivitySpots.size() - 1);
					result = availableActivitySpots.get(index);
				}
			}
		}

		return result;
	}

	/**
	 * Checks if an activity spot is empty/unoccupied
	 * 
	 * @param spot as a {@link Point2D}
	 * @return true if this activity spot is empty.
	 */
	public boolean isActivitySpotEmpty(Point2D spot) {
		if (activitySpots == null || activitySpots.isEmpty())
			return true;
		
		boolean result = false;

		Iterator<Point2D> i = activitySpots.iterator();
		while (i.hasNext()) {
			Point2D activitySpot = i.next();

			if (activitySpot == spot) {
				// Convert activity spot from building local to settlement local.
				Building b = getBuilding();
				Point2D settlementActivitySpot = LocalAreaUtil.getLocalRelativeLocation(activitySpot.getX(),
						activitySpot.getY(), b);

				for (Person person : b.getInhabitants()) {
					// Check if person's location is identical or very very close (1e-5 meters) to
					// activity spot.
					Point2D personLoc = new Point2D.Double(person.getXLocation(), person.getYLocation());
					if (!LocalAreaUtil.areLocationsClose(settlementActivitySpot, personLoc)) {
						return true;
					}
				}
				
				for (Robot robot : b.getRobots()) {
					// Check if robot location is identical or very very close (1e-5 meters) to
					// activity spot.
					Point2D loc = new Point2D.Double(robot.getXLocation(), robot.getYLocation());
					if (!LocalAreaUtil.areLocationsClose(settlementActivitySpot, loc)) {
						return true;
					}
				}
			}
		}

		return result;
	}

	/**
	 * Checks if a person is at an activity spot for this building function.
	 * 
	 * @param person the person.
	 * @return true if the person is currently at an activity spot.
	 */
	public boolean isAtActivitySpot(Person person) {
		boolean result = false;

		Iterator<Point2D> i = activitySpots.iterator();
		while (i.hasNext() && !result) {
			Point2D activitySpot = i.next();
			// Convert activity spot from building local to settlement local.
			Point2D settlementActivitySpot = LocalAreaUtil.getLocalRelativeLocation(activitySpot.getX(),
					activitySpot.getY(), getBuilding());

			// Check if person's location is very close to activity spot.
			Point2D personLoc = new Point2D.Double(person.getXLocation(), person.getYLocation());
			if (LocalAreaUtil.areLocationsClose(settlementActivitySpot, personLoc)) {
				result = true;
			}
		}

		return result;
	}

	/**
	 * Checks if a robot is at an activity spot for this building function.
	 * 
	 * @param robot the robot.
	 * @return true if the robot is currently at an activity spot.
	 */
	public boolean isAtActivitySpot(Robot robot) {
		boolean result = false;

		Iterator<Point2D> i = activitySpots.iterator();
		while (i.hasNext() && !result) {
			Point2D activitySpot = i.next();
			// Convert activity spot from building local to settlement local.
			Point2D settlementActivitySpot = LocalAreaUtil.getLocalRelativeLocation(activitySpot.getX(),
					activitySpot.getY(), getBuilding());

			// Check if robot's location is very close to activity spot.
			Point2D robotLoc = new Point2D.Double(robot.getXLocation(), robot.getYLocation());
			if (LocalAreaUtil.areLocationsClose(settlementActivitySpot, robotLoc)) {
				result = true;
			}
		}

		return result;
	}

	/**
	 * Check if this building function has any activity spots.
	 * 
	 * @return true if building function has activity spots.
	 */
	public boolean hasActivitySpots() {
		return (activitySpots.size() > 0);
	}

	public List<Point2D> getActivitySpotsList() {
		return activitySpots;
	}

	/**
	 * Gets the number of currently empty activity spots
	 * 
	 * @return
	 */
	public int getNumEmptyActivitySpots() {
		int empty = 0;
		if (activitySpots != null && !activitySpots.isEmpty()) {
			for (Point2D s: activitySpots) {
				if (isActivitySpotEmpty(s))
					empty++;		
			}	
		}
		return empty;
	}
	
	/**
	 * Checks if an empty activity spot is available
	 * 
	 * @return
	 */
	public boolean hasEmptyActivitySpot() {
		if (activitySpots != null && !activitySpots.isEmpty()) {
			for (Point2D s: activitySpots) {
				if (isActivitySpotEmpty(s))
					return true;		
			}	
		}
		return false;
	}
	
	/**
	 * Gets the number of currently occupied activity spots
	 * 
	 * @return
	 */
	public int getNumOccupiedActivitySpots() {
		int occupied = 0;
		if (activitySpots != null && !activitySpots.isEmpty()) {
			for (Point2D s: activitySpots) {
				if (!isActivitySpotEmpty(s))
					occupied++;		
			}	
		}
		return occupied;

	}
	
	/**
	 * Reloads instances after loading from a saved sim
	 * 
	 * @param bc {@link BuildingConfig}
	 * @param c0 {@link MasterClock}
	 * @param c1 {@link MarsClock}
	 * @param pc {@link PersonConfig}
	 */
	public static void initializeInstances(BuildingConfig bc, MarsClock c1, PersonConfig pc,
			SurfaceFeatures sf, Weather w, UnitManager u) {
		marsClock = c1;
		personConfig = pc;
		buildingConfig = bc;
		weather = w;
		surface = sf;
		unitManager = u;
	}
	
	/**
	 * Prepare object for garbage collection.
	 */
	public void destroy() {
		type = null;
		building = null;
		if (activitySpots != null) {
			activitySpots.clear();
			activitySpots = null;
		}
	}
}
