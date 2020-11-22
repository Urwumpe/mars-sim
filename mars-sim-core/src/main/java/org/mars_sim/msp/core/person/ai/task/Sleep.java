/**
 * Mars Simulation Project
 * Sleep.java
 * @version 3.1.2 2020-09-02
 * @author Scott Davis
 */
package org.mars_sim.msp.core.person.ai.task;

import java.awt.geom.Point2D;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.mars_sim.msp.core.Msg;
import org.mars_sim.msp.core.person.CircadianClock;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.PhysicalCondition;
import org.mars_sim.msp.core.person.ShiftType;
import org.mars_sim.msp.core.person.ai.SkillType;
import org.mars_sim.msp.core.person.ai.task.utils.Task;
import org.mars_sim.msp.core.person.ai.task.utils.TaskPhase;
import org.mars_sim.msp.core.person.ai.task.utils.TaskSchedule;
import org.mars_sim.msp.core.robot.Robot;
import org.mars_sim.msp.core.structure.Settlement;
import org.mars_sim.msp.core.structure.building.Building;
import org.mars_sim.msp.core.structure.building.BuildingManager;
import org.mars_sim.msp.core.structure.building.function.FunctionType;
import org.mars_sim.msp.core.structure.building.function.LivingAccommodations;
import org.mars_sim.msp.core.structure.building.function.RoboticStation;
import org.mars_sim.msp.core.tool.RandomUtil;
import org.mars_sim.msp.core.vehicle.Rover;

/**
 * The Sleep class is a task for sleeping. The duration of the task is by
 * default chosen randomly, between 250 - 330 millisols. Note: Sleeping reduces
 * fatigue and stress.
 */
public class Sleep extends Task implements Serializable {

	/** default serial id. */
	private static final long serialVersionUID = 1L;

	/** default logger. */
	private static Logger logger = Logger.getLogger(Sleep.class.getName());
//	private static String sourceName = logger.getName().substring(logger.getName().lastIndexOf(".") + 1,
//			logger.getName().length());
	
	private static final int MAX_FATIGUE = 2500;
    private static final int MAX_SUPPRESSION = 100;

	/** Task name */
	private static final String NAME = Msg.getString("Task.description.sleep"); //$NON-NLS-1$

	/** Task phases. */
	private static final TaskPhase SLEEPING = new TaskPhase(Msg.getString("Task.phase.sleeping")); //$NON-NLS-1$

	/** Task name for robot */
	private static final String SLEEP_MODE = Msg.getString("Task.description.sleepMode"); //$NON-NLS-1$

	/** Task phases for robot. */
	private static final TaskPhase SLEEPING_MODE = new TaskPhase(Msg.getString("Task.phase.sleepMode")); //$NON-NLS-1$

	// Static members
	/** The stress modified per millisol. */
	private static final double STRESS_MODIFIER = -2.2D;
	/** The base alarm time (millisols) at 0 degrees longitude. */
	private static final double BASE_ALARM_TIME = 300D;

	// Data members
	private boolean arrived = false;
	/** The previous time (millisols). */
	private double previousTime;
	private double timeFactor = 2.0; // TODO: should vary this factor by person

	/**
	 * Constructor.
	 * 
	 * @param person the person to perform the task
	 */
	public Sleep(Person person) {
		super(NAME, person, false, false, STRESS_MODIFIER, true, 
				(50 + RandomUtil.getRandomDouble(5) - RandomUtil.getRandomDouble(5)));
		
		if (person.isOutside()) {
			logger.warning(person + " was not supposed to be falling asleep outside.");
			endTask();
		}
		
		else {
			// Initialize phase
			addPhase(SLEEPING);
			setPhase(SLEEPING);
		}
//		logger.info(person + "  End of Sleep");
	}

	public Sleep(Robot robot) {
		super(SLEEP_MODE, robot, false, false, STRESS_MODIFIER, true, 10D);
		
		// If robot is in a settlement, try to find a living accommodations building.
		if (robot.isInSettlement()) {

			// TODO: if power is below a certain threshold, go to robotic station for
			// recharge, else stay at the same place

			// If currently in a building with a robotic station, go to a station activity
			// spot.
			// boolean atStation = false;
			Building currentBuilding = BuildingManager.getBuilding(robot);
			if (currentBuilding != null) {
//				if (currentBuilding.hasFunction(FunctionType.ROBOTIC_STATION)) {
					RoboticStation station = currentBuilding.getRoboticStation();
					if (station.getSleepers() < station.getSlots()) {
						station.addSleeper();

						// Check if robot is currently at an activity spot for the robotic station.
						if (station.hasActivitySpots() && !station.isAtActivitySpot(robot)) {
							// Walk to an available activity spot.
							walkToActivitySpotInBuilding(currentBuilding, FunctionType.ROBOTIC_STATION, true);
//							walkToTaskFunctionActivitySpot(currentBuilding, true);
						}
					}
//				}
			} else {
				// if (!atStation) {
				Building building = getAvailableRoboticStationBuilding(robot);
				if (building != null) {
					// System.out.println("building.toString() is " + building.toString() );
					RoboticStation station = building.getRoboticStation();
					if (station != null) {
						// TODO: see https://github.com/mars-sim/mars-sim/issues/22
						// Question: why would the method below cause RepairBot to walk outside the
						// settlement to a vehicle ?
						walkToActivitySpotInBuilding(building, FunctionType.ROBOTIC_STATION, true);
//						walkToTaskFunctionActivitySpot(building, true);
						// TODO: need to add activity spots in every building or
						// walkToActivitySpotInBuilding(building, false) will fail
						// and create java.lang.NullPointerException
						station.addSleeper();
					}
				}
			}
		}

		previousTime = marsClock.getMillisol();

		// Initialize phase
		addPhase(SLEEPING_MODE);
		setPhase(SLEEPING_MODE);
	}

	@Override
	public FunctionType getLivingFunction() {
		return FunctionType.LIVING_ACCOMMODATIONS;
	}

	@Override
	public FunctionType getRoboticFunction() {
		return FunctionType.ROBOTIC_STATION;
	}

	@Override
	protected double performMappedPhase(double time) {
		if (person != null) {
			if (getPhase() == null)
				throw new IllegalArgumentException("Task phase is null");
			else if (SLEEPING.equals(getPhase())) {
				return sleepingPhase(time);
			} else
				return time;
		}

		else if (robot != null) {
			if (getPhase() == null)
				throw new IllegalArgumentException("Task phase is null");
			else if (SLEEPING_MODE.equals(getPhase()))
				return sleepingPhase(time);
			else
				return time;
		}
		return time;
	}

	private void walkToDestination() {
		if (!arrived) {
			// If person is in rover, walk to passenger activity spot.
			if (person.isInVehicle() && person.getVehicle() instanceof Rover) {
				
				walkToPassengerActivitySpotInRover((Rover) person.getVehicle(), true);
			
				previousTime = marsClock.getMillisol();
	
	//			logger.info(person + " will sleep at " + person.getVehicle());
			}
	
			// If person is in a settlement, try to find a living accommodations building.
			else if (person.isInSettlement()) {
				// Double the sleep duration
				setDuration(getDuration() * 2);
				
	//	    	if (BuildingManager.isInBuildingAirlock(person))
	//	    		;
		    	
				// Note: A bed can be either unmarked(U) or marked(M); and either empty(E) or occupied(O).
				// 4 possibilities : ME, MO, UE, or UO
	
				Settlement s1 = person.getSettlement();
				Settlement s0 = person.getAssociatedSettlement();
				
				if (s1 != null && !s1.equals(s0)) {
					// This person is a trader, a tourist, or a guest to this settlement
					
	//				logger.info(person + " (from " + person.getAssociatedSettlement() + ") is in " + person.getSettlement());
					
					//////////////////// Case 1 - 3 /////////////////////////
	
					Building q0 = getBestAvailableQuarters(person, true);
					
					if (q0 != null) {
						// Case 1 : (the BEST case for a guest) unmarked, empty (UE) bed(s)
						
	//					accommodations = q0.getLivingAccommodations();
						// TODO: need to figure out first how to deregister a trader/tourist once he departs 
	//					accommodations.registerSleeper(person, true);
						
						walkToActivitySpotInBuilding(q0, FunctionType.LIVING_ACCOMMODATIONS, true);
	//					walkToAnUnoccupiedBed(q0, true);
						// addSubTask(new WalkSettlementInterior(person, quarters, bed.getX(), bed.getY()));
					} 
					
					else { // no unmarked bed
						
						q0 = getBestAvailableQuarters(person, false);
						
						if (q0 != null) {
							// Case 2 : marked, empty (ME) bed(s)
							
	//						accommodations = q0.getLivingAccommodations();
							// TODO: need to figure out first how to deregister a trader/tourist once he departs 
	//						accommodations.registerSleeper(person, true);
							
							walkToActivitySpotInBuilding(q0, FunctionType.LIVING_ACCOMMODATIONS, true);
	//						walkToAnUnoccupiedBed(q0, true);
						}
						else { 
							// Case 3 :  NO empty bed(s)
							
							walkToRandomLocation(true);
							// TODO: should allow him/her to sleep in gym or anywhere based on his/her usual
							// preferences
						}
					}
				}
				
				else {
					//////////////////// Case 4 - ? /////////////////////////
					
					// He/she is an inhabitant in this settlement
	//				logger.info(person + " (from " + person.getAssociatedSettlement() + ") is also in " + person.getSettlement());
					
					// Check if a person has a designated quarters and a marked bed
					Building q1 = person.getQuarters();
	
					if (q1 != null) {
						// This person has his quarters and have a designated bed
						// logger.fine(person + " does have a designated bed at " + pq.getNickName());
	
						// check if this bed is currently empty or occupied (either ED or OD)
						Point2D bed = person.getBed();
						double x = bed.getX();
						double y = bed.getY();
						
	//					accommodations = q1.getLivingAccommodations();
						// Concert the coordinate back 
						Point2D spot = new Point2D.Double(x - q1.getXLocation(), y - q1.getYLocation());
						
						boolean empty = q1.getLivingAccommodations().isActivitySpotEmpty(spot);
	
						if (empty) {
							// the BEST case for an inhabitant
							// Case 4: marked and empty (ME)
	
							// addSubTask(new WalkSettlementInterior(person, quarters, bed.getX(),
							// bed.getY()));
	
							walkToBed(q1, person, true);
						} 
						
						else { // unfortunately his/her marked bed is not empty
	
							q1 = getBestAvailableQuarters(person, true);
							
							if (q1 != null)
								// Case 5: unmarked empty (UE)
							
								walkToActivitySpotInBuilding(q1, FunctionType.LIVING_ACCOMMODATIONS, true);
	//							walkToAnUnoccupiedBed(q1, true);
							
							else { // no unmarked bed
								
								q1 = getBestAvailableQuarters(person, false);
								
								if (q1 != null) {
									// Case 6: this marked bed is currently empty (ME) 	
									
									walkToActivitySpotInBuilding(q1, FunctionType.LIVING_ACCOMMODATIONS, true);
	//								walkToAnUnoccupiedBed(q1, true);
								}
								
								else {
									// Case 7: No beds available, go to any activity spots
									
									walkToRandomLocation(true);
									
									// TODO: should allow him/her to sleep in gym or anywhere based on his/her usual
									// preferences
								}
							}
						}
					}
					
					else {
						// this inhabitant has never registered a bed and have no designated quarter
						// logger.fine(person + " has never been designated a bed so far");
	
						q1 = getBestAvailableQuarters(person, true);
						
	//					accommodations = q1.getLivingAccommodations();
	//					
	//					Point2D bed = q1.getLivingAccommodations().registerSleeper(person, false);
						
						if (q1 != null && q1.getLivingAccommodations().registerSleeper(person, false) != null) {
							// Case 8: unmarked, empty (UE) bed
							
							walkToBed(q1, person, true);
							
						}
						
						else { // no unmarked bed
							
							q1 = getBestAvailableQuarters(person, false);
							
							if (q1 != null)
								// Case 9: marked, empty (ME)
			
								walkToActivitySpotInBuilding(q1, FunctionType.LIVING_ACCOMMODATIONS, true);
	//							walkToAnUnoccupiedBed(q1, true);
							else
								// Case 10: No beds available, go to any activity spots
								
								walkToRandomLocation(true);
							
							// TODO: should allow him/her to sleep in gym or anywhere based on his/her usual
							// preferences
						}
					}
				}
	
				previousTime = marsClock.getMillisol();

	//			logger.info(person + " will sleep at " + person.getSettlement());
			}
			
			arrived = true;
		}
	}
	
	/**
	 * Performs the sleeping phase.
	 * 
	 * @param time the amount of time (millisols) to perform the phase.
	 * @return the amount of time (millisols) left over after performing the phase.
	 */
	private double sleepingPhase(double time) {
//		logger.info(person + " at sleepingPhase()");
		
		if (person != null) {

			// Clear the sub task to avoid getting stuck at walkin
			endSubTask();
			
			// Walk to a bed if possible
			walkToDestination();
			
			PhysicalCondition pc = person.getPhysicalCondition();
			CircadianClock circadian = person.getCircadianClock();
			
			pc.recoverFromSoreness(.05);

			double fractionOfRest = time * timeFactor;
			double newFatigue = 0;
			// Calculate the residualFatigue to speed up the recuperation for the high fatigue case
			double residualFatigue = 0;
			double f = pc.getFatigue();

			if (f < 62.5)
				residualFatigue = f / 150.0;

			else if (f < 125)
				residualFatigue = (f - 62.5) / 140.0;

			else if (f < 250)
				residualFatigue = (f - 125) / 130.0;

			else if (f < 500)
				residualFatigue = (f - 250) / 120.0;

			else if (f < 750)
				residualFatigue = (f - 500) / 110.0;

			else if (f < 1000)
				residualFatigue = (f - 750) / 100.0;

			else if (f < 1250)
				residualFatigue = (f - 1000) / 90.0;
			
			else if (f < 1500)
				residualFatigue = (f - 1250) / 80.0;

			else if (f < 1750)
				residualFatigue = (f - 1500) / 70.0;
			
			else if (f < 2000)
				residualFatigue = (f - 1750) / 60.0;

			else if (f < MAX_FATIGUE) 
				residualFatigue = (f - 2000) / 50.0;
			
			else 
				residualFatigue = (f - MAX_FATIGUE) / 40.0;
			
			newFatigue = f - fractionOfRest - residualFatigue;	
//			logger.info(person + " f : " + Math.round(f*10.0)/10.0
//					+ "   time : " + Math.round(time*1000.0)/1000.0
//					+ "   residualFatigue : " + Math.round(residualFatigue*10.0)/10.0  
//					+ "   fractionOfRest : " + Math.round(fractionOfRest*10.0)/10.0  
//							+ "   newFatigue : " + Math.round(newFatigue*10.0)/10.0);
				
			if (newFatigue < 0)
				newFatigue = 0;
			
			pc.setFatigue(newFatigue);
				
			circadian.setAwake(false);
			
//			totalSleepTime += time;
//			logger.info(person + "  time: " + Math.round(time*1000.0)/1000.0
//					+ "  totalSleepTime: " + Math.round(totalSleepTime*1000.0)/1000.0);
			
			// Adjust the leptin and ghrelin level
			circadian.getRested(time);

			// Record the sleep time [in millisols]
			circadian.recordSleep(time);
			
			if (person.getTaskSchedule().isShiftHour(marsClock.getMillisolInt())) {
				// Reduce the probability if it's not the right time to sleep
				refreshSleepHabit(person, circadian);
			}
			
			double newTime = marsClock.getMillisol();
			
			// Check if fatigue is zero
			if (newFatigue <= 0) {
				logger.info(person.getName() + " woke up, totally refreshed from a good sleep at " + (int)newTime + " millisols.");;
				circadian.setAwake(true);
				endTask();
			}
			
			double alarmTime = getAlarmTime();

			// Check if alarm went off
			if ((previousTime <= alarmTime) && (newTime >= alarmTime)) {
				circadian.setNumSleep(circadian.getNumSleep() + 1);
				circadian.updateSleepCycle((int) marsClock.getMillisol(), true);
				logger.finer(person.getName() + " woke up by the alarm at " + (int)alarmTime + " millisols.");
				circadian.setAwake(true);
				endTask();
			} else {
				previousTime = newTime;
			}

		}

		else if (robot != null) {
			double newTime = marsClock.getMillisol();
			double alarmTime = getAlarmTime();
			
			// Check if alarm went off
			if ((previousTime <= alarmTime) && (newTime >= alarmTime)) {
				logger.finer(robot.getName() + " woke up by the alarm at " + (int)alarmTime + " millisols.");
				endTask();
			} else {
				previousTime = newTime;
			}
		}

		return 0D;
	}

	@Override
	protected void addExperience(double time) {
		// This task adds no experience.
	}

	@Override
	public void endTask() {
		if (person != null) {
//	    	logger.info(person.getNickName() + " called endTask() in " + this);
			// Remove person from living accommodations bed so others can use it.
//			if (accommodations != null && accommodations.getRegisteredSleepers() > 0) {
//				accommodations.removeSleeper(person);
//			}

		} else if (robot != null) {
			// Remove robot from stations so other robots can use it.
//			if (station != null && station.getSleepers() > 0) {
//				station.removeSleeper();
				// TODO: assess how well this work
//			}
//    		logger.info(robot.getNickName() + " was done sleeping and waking up.");
			walkToAssignedDutyLocation(robot, true);
		}
		
		super.endTask();
	}

	/**
	 * Gets the best available living accommodations building that the person can
	 * use. Returns null if no living accommodations building is currently
	 * available.
	 * 
	 * @param person   the person
	 * @param unmarked does the person wants an unmarked(aka undesignated) bed or
	 *                 not.
	 * @return a building with available bed(s)
	 */
	public static Building getBestAvailableQuarters(Person person, boolean unmarked) {

		Building result = null;

		if (person.isInSettlement()) {
			// BuildingManager manager = person.getSettlement().getBuildingManager();
			List<Building> quartersBuildings = person.getSettlement().getBuildingManager()
					.getBuildings(FunctionType.LIVING_ACCOMMODATIONS);
			quartersBuildings = BuildingManager.getNonMalfunctioningBuildings(quartersBuildings);
			quartersBuildings = getQuartersWithEmptyBeds(quartersBuildings, unmarked);
			if (quartersBuildings.size() > 0) {
				quartersBuildings = BuildingManager.getLeastCrowdedBuildings(quartersBuildings);
			}
			if (quartersBuildings.size() == 1) {
				return quartersBuildings.get(0);
			}
			else if (quartersBuildings.size() > 1) {
				Map<Building, Double> quartersBuildingProbs = BuildingManager.getBestRelationshipBuildings(person,
						quartersBuildings);
				result = RandomUtil.getWeightedRandomObject(quartersBuildingProbs);
			}
		}

		return result;
	}

	public static Building getAvailableRoboticStationBuilding(Robot robot) {

		Building result = null;

		if (robot.isInSettlement()) {
			BuildingManager manager = robot.getSettlement().getBuildingManager();
			List<Building> buildings = manager.getBuildings(FunctionType.ROBOTIC_STATION);
			buildings = BuildingManager.getNonMalfunctioningBuildings(buildings);
			buildings = getRoboticStationsWithEmptySlots(buildings);
			if (RandomUtil.getRandomInt(2) == 0) // robot is not as inclined to move around
				buildings = BuildingManager.getLeastCrowded4BotBuildings(buildings);
			int size = buildings.size();
			// System.out.println("size is "+size);
			int selected = 0;
			if (size == 0)
				result = null;
			else if (size >= 1) {
				selected = RandomUtil.getRandomInt(size - 1);
				result = buildings.get(selected);
			}
		}

		return result;
	}

	/**
	 * Gets living accommodations with empty beds from a list of buildings with the
	 * living accommodations function.
	 * 
	 * @param buildingList list of buildings with the living accommodations
	 *                     function.
	 * @param unmarked     does the person wants an unmarked(aka undesignated) bed
	 *                     or not.
	 * @return list of buildings with empty beds.
	 */
	private static List<Building> getQuartersWithEmptyBeds(List<Building> buildingList, boolean unmarked) {
		List<Building> result = new ArrayList<Building>();

		for (Building building : buildingList) {
			LivingAccommodations quarters = building.getLivingAccommodations();
			boolean notFull = quarters.getNumEmptyActivitySpots() > 0;//quarters.getRegisteredSleepers() < quarters.getBedCap();
			// Check if an unmarked bed is wanted
			if (unmarked) {
				if (quarters.hasAnUnmarkedBed() && notFull) {
					result.add(building);
				}
			}
			else if (notFull) {
				result.add(building);
			}
		}

		return result;
	}

	/***
	 * Gets a list of robotic stations with empty spots
	 * 
	 * @param buildingList
	 * @return
	 */
	private static List<Building> getRoboticStationsWithEmptySlots(List<Building> buildingList) {
		List<Building> result = new ArrayList<Building>();

		Iterator<Building> i = buildingList.iterator();
		while (i.hasNext()) {
			Building building = i.next();
			RoboticStation station = building.getRoboticStation();
			if (station.getSleepers() < station.getSlots()) {
				result.add(building);
			}
		}

		return result;
	}

	/**
	 * Gets the wake-up alarm time, based on a person's shift
	 * 
	 * @return alarm time in millisols.
	 */
	private double getAlarmTime() {
		double timeDiff = 0;
		double time = 0;

		if (person != null) {
			ShiftType shiftType = person.getTaskSchedule().getShiftType();
			// Set to 30 millisols prior to the beginning of the duty shift hour
			if (shiftType == ShiftType.A)
				time = TaskSchedule.A_START - 30; // 220
			else if (shiftType == ShiftType.B)
				time = TaskSchedule.B_START - 30; // 721;
			else if (shiftType == ShiftType.X)
				time = TaskSchedule.X_START - 30; // 970;
			else if (shiftType == ShiftType.Y)
				time = TaskSchedule.Y_START - 30; // 304;
			else if (shiftType == ShiftType.Z)
				time = TaskSchedule.Z_START - 30; // 637;
			else if (shiftType == ShiftType.ON_CALL) { 
				// if a person is on a mission outside, assume the day begins with 
				// the sun rises at ~250 milisols at 0 longitude
				timeDiff = 1000D * (person.getCoordinates().getTheta() / (2D * Math.PI));
				time = BASE_ALARM_TIME - timeDiff;
				if (time < 0D) {
					time += 1000D;
				}
			}

		} else if (robot != null) {
			timeDiff = 1000D * (robot.getCoordinates().getTheta() / (2D * Math.PI));
			time = BASE_ALARM_TIME - timeDiff;
			if (time < 0D) {
				time += 1000D;
			}
		}

		return time;
	}

	/**
     * Refreshes a person's sleep habit based on his/her latest work shift 
     * 
     * @param person
     */
    public void refreshSleepHabit(Person person, CircadianClock circadian) {
    	int now = marsClock.getMillisolInt();

        // if a person is NOT on-call
        if (person.getTaskSchedule().getShiftType() != ShiftType.ON_CALL) {
	        // if a person is on shift right now
           	if (person.getTaskSchedule().isShiftHour(now)) {

           		int habit = circadian.getSuppressHabit();
           		int spaceOut = circadian.getSpaceOut();
	           	// limit adjustment to 10 times and space it out to at least 50 millisols apart
           		if (spaceOut < now && habit < MAX_SUPPRESSION) {
	           		// Discourage the person from forming the sleep habit at this time
		  	  		person.updateSleepCycle(now, false);
			    	//System.out.println("spaceOut : " + spaceOut + "   now : " + now + "  suppressHabit : " + habit);

		  	  		int rand = RandomUtil.getRandomInt(2);
		  	  		if (rand == 2) {
				    	circadian.setSuppressHabit(habit+1);
				    	spaceOut = now + 20;
				    	if (spaceOut > 1000) {
				    		spaceOut = spaceOut - 1000;
				    	}
				    	circadian.setSpaceOut(spaceOut);
		  	  		}
           		}
		    }

//           	else {
//           		int future = now;
//                // Check if person's work shift will begin within the next 50 millisols.
//           		future += 50;
//	            if (future > 1000)
//	            	future = future - 1000;
//
//	            boolean willBeShiftHour = person.getTaskSchedule().isShiftHour(future);
//	            if (willBeShiftHour) {
//	            	//if work shift is slated to begin in the next 50 millisols, probability of sleep reduces to one tenth of its value
//	                result = result / 10D;
//	            }
//	            //else
//	            	//result = result * 2D;
//           	}
	    }
        
//        else {
//        	// if he's on-call
//        	result = result * 1.1D;
//        }

    }
    
//    /**
//     * Walks back inside
//     * 
//     * @return true if this unit can walk back inside
//     */
//	public boolean walkBackInside() {
//		if (!person.isOutside())
//			return false;
//		LocalBoundedObject interiorObject = null;
//		boolean canWalkInside = true;
//		// Get closest airlock building at settlement.
//		Settlement s = person.getLocationTag().findSettlementVicinity();
//		if (s != null) {
//			interiorObject = (Building)(s.getClosestAvailableAirlock(person).getEntity()); 
////			System.out.println("interiorObject is " + interiorObject);
//			if (interiorObject == null)
//				interiorObject = (LocalBoundedObject)(s.getClosestAvailableAirlock(person).getEntity());
////			System.out.println("interiorObject is " + interiorObject);
//			LogConsolidated.log(logger, Level.INFO, 4000, sourceName,
//					"[" + person.getLocationTag().getLocale() + "] " + person.getName()
//					+ " at "
//					+ person.getLocationTag().getImmediateLocation()
//					+ " found " + ((Building)interiorObject).getNickName()
//					+ " as the closest building with an airlock to enter.");
//		}
//		else {
//			// near a vehicle
//			Rover r = (Rover)person.getVehicle();
//			interiorObject = (LocalBoundedObject) (r.getAirlock()).getEntity();
//			LogConsolidated.log(logger, Level.INFO, 4000, sourceName,
//					"[" + person.getLocationTag().getLocale() + "] " + person.getName()
//					+ " was near " + r.getName()
//					+ " and had to walk back inside the vehicle.");
//		}
//		
//		 Point2D returnInsideLoc;
//		 
//		if (interiorObject == null) {
//			LogConsolidated.log(logger, Level.WARNING, 4000, sourceName,
//				"[" + person.getLocationTag().getLocale() + "] " + person.getName()
//				+ " was near " + person.getLocationTag().getImmediateLocation()
//				+ " at (" + Math.round(returnInsideLoc.getX()*10.0)/10.0 + ", " 
//				+ Math.round(returnInsideLoc.getY()*10.0)/10.0 + ") "
//				+ " but interiorObject is null.");
//			canWalkInside = false;
//		}
//		else {
//			// Set return location.
//			Point2D rawReturnInsideLoc = LocalAreaUtil.getRandomInteriorLocation(interiorObject);
//			returnInsideLoc = LocalAreaUtil.getLocalRelativeLocation(rawReturnInsideLoc.getX(),
//					rawReturnInsideLoc.getY(), interiorObject);
//			
//			if (returnInsideLoc != null && !LocalAreaUtil.checkLocationWithinLocalBoundedObject(returnInsideLoc.getX(),
//						returnInsideLoc.getY(), interiorObject)) {
//				LogConsolidated.log(logger, Level.WARNING, 4000, sourceName,
//						"[" + person.getLocationTag().getLocale() + "] " + person.getName()
//						+ " was near " + ((Building)interiorObject).getNickName() //person.getLocationTag().getImmediateLocation()
//						+ " at (" + Math.round(returnInsideLoc.getX()*10.0)/10.0 + ", " 
//						+ Math.round(returnInsideLoc.getY()*10.0)/10.0 + ") "
//						+ " but could not be found inside " + interiorObject);
//				canWalkInside = false;
//			}
//		}
//
//		// If not at return inside location, create walk inside subtask.
//        Point2D personLocation = new Point2D.Double(person.getXLocation(), person.getYLocation());
//        boolean closeToLocation = LocalAreaUtil.areLocationsClose(personLocation, returnInsideLoc);
//        
//		// If not inside, create walk inside subtask.
//		if (interiorObject != null && !closeToLocation) {
//			String name = "";
//			if (interiorObject instanceof Building) {
//				name = ((Building)interiorObject).getNickName();
//			}
//			else if (interiorObject instanceof Vehicle) {
//				name = ((Vehicle)interiorObject).getNickName();
//			}
//					
//			LogConsolidated.log(logger, Level.FINEST, 10_000, sourceName,
//						"[" + person.getLocationTag().getLocale() + "] " + person.getName()
//						+ " was near " +  name 
//						+ " at (" + Math.round(returnInsideLoc.getX()*10.0)/10.0 + ", " 
//						+ Math.round(returnInsideLoc.getY()*10.0)/10.0 
//						+ ") and was attempting to enter its airlock.");
//			
//			if (Walk.canWalkAllSteps(person, returnInsideLoc.getX(), returnInsideLoc.getY(), 0, interiorObject)) {
//				Task walkingTask = new Walk(person, returnInsideLoc.getX(), returnInsideLoc.getY(), 0, interiorObject);
//				addSubTask(walkingTask);
//			} 
//			
//			else {
//				LogConsolidated.log(logger, Level.SEVERE, 4000, sourceName,
//						person.getName() + " was " + person.getTaskDescription().toLowerCase() 
//						+ " and cannot find a valid path to enter an airlock. Will see what to do.");
//				canWalkInside = false;
//			}
//		} else {
//			LogConsolidated.log(logger, Level.SEVERE, 4000, sourceName,
//					person.getName() + " was " + person.getTaskDescription().toLowerCase() 
//					+ " and cannot find the building airlock to walk back inside. Will see what to do.");
//			canWalkInside = false;
//		}
//		
//		return canWalkInside;
//	}
	
	@Override
	public int getEffectiveSkillLevel() {
		return 0;
	}

	@Override
	public List<SkillType> getAssociatedSkills() {
		List<SkillType> results = new ArrayList<SkillType>(0);
		return results;
	}

	@Override
	public void destroy() {
		super.destroy();
		
		SLEEPING_MODE.destroy();
		SLEEPING.destroy();
	}
}
