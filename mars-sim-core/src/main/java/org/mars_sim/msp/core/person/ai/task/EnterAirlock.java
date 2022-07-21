/*
 * Mars Simulation Project
 * EnterAirlock.java
 * @date 2021-11-04
 * @author Scott Davis
 */
package org.mars_sim.msp.core.person.ai.task;

import java.io.Serializable;
import java.util.Set;
import java.util.logging.Level;

import org.mars_sim.msp.core.LocalPosition;
import org.mars_sim.msp.core.Msg;
import org.mars_sim.msp.core.Unit;
import org.mars_sim.msp.core.equipment.EVASuit;
import org.mars_sim.msp.core.equipment.EquipmentOwner;
import org.mars_sim.msp.core.logging.SimLogger;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.ai.SkillType;
import org.mars_sim.msp.core.person.ai.task.utils.Task;
import org.mars_sim.msp.core.person.ai.task.utils.TaskPhase;
import org.mars_sim.msp.core.structure.Airlock;
import org.mars_sim.msp.core.structure.AirlockType;
import org.mars_sim.msp.core.structure.building.Building;
import org.mars_sim.msp.core.tool.RandomUtil;
import org.mars_sim.msp.core.vehicle.Rover;
import org.mars_sim.msp.core.vehicle.Vehicle;

/**
 * The EnterAirlock class is a Task for EVA ingress, namely, entering an airlock
 * of a settlement or vehicle after an EVA operation outside have been
 * accomplished.
 */
public class EnterAirlock extends Task implements Serializable {

	/** default serial id. */
	private static final long serialVersionUID = 1L;

	/** default logger. */
	private static SimLogger logger = SimLogger.getLogger(EnterAirlock.class.getName());

	/** Task name */
	private static final String NAME = Msg.getString("Task.description.enterAirlock"); //$NON-NLS-1$

	/** Task phases. */
	private static final TaskPhase REQUEST_INGRESS = new TaskPhase(Msg.getString("Task.phase.requestIngress")); //$NON-NLS-1$
	private static final TaskPhase DEPRESSURIZE_CHAMBER = new TaskPhase(Msg.getString("Task.phase.depressurizeChamber")); //$NON-NLS-1$
	private static final TaskPhase ENTER_AIRLOCK = new TaskPhase(Msg.getString("Task.phase.enterAirlock")); //$NON-NLS-1$
	private static final TaskPhase WALK_TO_CHAMBER = new TaskPhase(Msg.getString("Task.phase.walkToChamber")); //$NON-NLS-1$
	private static final TaskPhase PRESSURIZE_CHAMBER = new TaskPhase(Msg.getString("Task.phase.pressurizeChamber")); //$NON-NLS-1$
	private static final TaskPhase DOFF_EVA_SUIT = new TaskPhase(Msg.getString("Task.phase.doffEVASuit")); //$NON-NLS-1$
	private static final TaskPhase CLEAN_UP = new TaskPhase(Msg.getString("Task.phase.cleanUp")); //$NON-NLS-1$
	private static final TaskPhase LEAVE_AIRLOCK = new TaskPhase(Msg.getString("Task.phase.leaveAirlock")); //$NON-NLS-1$

	// Static members
	/** The standard time for doffing the EVA suit. */
	private static final double STANDARD_DOFFING_TIME = 10;
	/** The standard time for cleaning oneself and the EVA suit. */
	private static final double STANDARD_CLEANINNG_TIME = 15;
	/** The stress modified per millisol. */
	private static final double STRESS_MODIFIER = .1D;

	/** Is this a building airlock in a settlement? */
	private boolean inSettlement;
	/** The time it takes to clean up oneself and the EVA suit. */
	private double remainingCleaningTime;
	/** The time it takes to doff an EVA suit. */
	private double remainingDoffingTime;

	// Data members
	/** The airlock to be used. */
	private Airlock airlock;
	/** The inside airlock position. */
	private LocalPosition insideAirlockPos = null;
	/** The exterior airlock position. */
	private LocalPosition exteriorDoorPos = null;
	/** The interior airlock position. */
	private LocalPosition interiorDoorPos = null;

	/**
	 * Constructor.
	 *
	 * @param person  the person to perform the task
	 * @param airlock to be used.
	 */
	public EnterAirlock(Person person, Airlock airlock) {
		super(NAME, person, false, false, STRESS_MODIFIER, SkillType.EVA_OPERATIONS, 100D);

		this.airlock = airlock;

		if (airlock.getAirlockType() == AirlockType.BUILDING_AIRLOCK) {
			inSettlement = true;
		}
		else
			inSettlement = false;

		// Initialize data members
		setDescription(Msg.getString("Task.description.enterAirlock.detail", airlock.getEntityName())); // $NON-NLS-1$
		// Initialize task phase
		addPhase(REQUEST_INGRESS);
		addPhase(DEPRESSURIZE_CHAMBER);
		addPhase(ENTER_AIRLOCK);
		addPhase(WALK_TO_CHAMBER);
		addPhase(PRESSURIZE_CHAMBER);
		addPhase(DOFF_EVA_SUIT);
		addPhase(CLEAN_UP);
		addPhase(LEAVE_AIRLOCK);

		setPhase(REQUEST_INGRESS);

		logger.log((Unit)airlock.getEntity(), person, Level.FINER, 4000, "Starting EVA ingress in " + airlock.getEntityName() + ".");
	}

	/**
	 * Performs the method mapped to the task's current phase.
	 *
	 * @param time the amount of time (millisols) the phase is to be performed.
	 * @return the remaining time (millisols) after the phase has been performed.
	 */
	protected double performMappedPhase(double time) {
		if (getPhase() == null) {
			throw new IllegalArgumentException("Task phase is null");
		} else if (REQUEST_INGRESS.equals(getPhase())) {
			return requestIngress(time);
		} else if (DEPRESSURIZE_CHAMBER.equals(getPhase())) {
			return depressurizeChamber(time); 
		} else if (ENTER_AIRLOCK.equals(getPhase())) {
			return enterAirlock(time);
		} else if (WALK_TO_CHAMBER.equals(getPhase())) {
			return walkToChamber(time);
		} else if (PRESSURIZE_CHAMBER.equals(getPhase())) {
			return pressurizeChamber(time);
		} else if (DOFF_EVA_SUIT.equals(getPhase())) {
			return doffEVASuit(time);
		} else if (CLEAN_UP.equals(getPhase())) {
			return cleanUp(time);
		} else if (LEAVE_AIRLOCK.equals(getPhase())) {
			return leaveAirlock(time);
		} else {
			return time;
		}
	}

	/**
	 * Transitions the person into a particular zone.
	 *
	 * @param zone the destination
	 * @return true if the transition is successful
	 */
	private boolean transitionTo(int zone) {

		// Is the person already in this zone
		if (isInZone(zone))
			return true;
		// For ingress, the previous zone # is the more this zone
		int previousZone = zone + 1;
		LocalPosition newPos = fetchNewPos(zone);
		if (newPos != null && airlock.occupy(zone, newPos, id)) {
			if (previousZone <= 4) {
				if (airlock.vacate(previousZone, id)) {
					moveThere(newPos, zone);
					return true;
				}
				else
					return false;
			}
			else {
				moveThere(newPos, zone);
				return true;
			}
		}

		return false;
	}
	
	/**
	 * Checks if the person is already in a particular zone
	 *
	 * @param zone
	 * @return true if the person is a particular zone
	 */
	private boolean isInZone(int zone) {
		return airlock.isInZone(person, zone);
	}

	/**
	 * Obtains a new position in the target zone.
	 *
	 * @param zone the destination
	 * @param id   the id of the person
	 * @return LocalPosition
	 */
	private LocalPosition fetchNewPos(int zone) {
		LocalPosition newPos = null;

		if (zone == 0) {
			newPos = airlock.getAvailableInteriorPosition(false);
		}
		else if (zone == 1) {
			newPos = airlock.getAvailableInteriorPosition(true);
		}
		else if (zone == 2) {
			newPos = ((Building) airlock.getEntity()).getEVA().getAvailableActivitySpot(person);
		}
		else if (zone == 3) {
			newPos = airlock.getAvailableExteriorPosition(true);
		}
		else if (zone == 4) {
			newPos = airlock.getAvailableExteriorPosition(false);
		}

		return newPos;
	}

	/**
	 * Moves the person to a particular zone
	 *
	 * @param newPos the target position in that zone
	 * @param zone
	 */
	private void moveThere(LocalPosition newPos, int zone) {
		if (zone == 2) {
			walkToEVASpot((Building) airlock.getEntity());
		}

		else if (zone == 4) {
			addSubTask(new WalkOutside(person, person.getPosition(), newPos, true));
		}

		else {
			person.setPosition(newPos);
		}

		logger.log((Unit)airlock.getEntity(), person, Level.FINE, 4000, "Arrived at "
				+ newPos.getShortFormat() + " in airlock zone " + zone + ".");
	}

	/**
	 * Requests the entry of the airlock.
	 *
	 * @param time
	 * @return
	 */
	private double requestIngress(double time) {

		double remainingTime = 0;

		logger.log((Unit)airlock.getEntity(), person, Level.FINE, 20_000, "Requested EVA ingress in " + airlock.getEntity().toString() + ".");

		if (!airlock.isActivated()) {
			// Only the airlock operator may activate the airlock
			airlock.setActivated(true);
		}

		boolean canProceed = false;

		if (inSettlement) {
			// Load up the EVA activity spots
			airlock.loadEVAActivitySpots();

			if (!airlock.addAwaitingOuterDoor(id)) {
				logger.log((Unit)airlock.getEntity(), person, Level.FINE, 60_000,
						"Cannot get a spot outside the outer door in " + airlock.getEntity().toString() + ".");
				return remainingTime;
			}

			if (airlock.isChamberFull() || !airlock.hasSpace()) {
				logger.log((Unit)airlock.getEntity(), person, Level.FINE, 60_000,
						"Chamber full in " + airlock.getEntity().toString() + ".");
				return remainingTime;
			}
				
			if (!transitionTo(4)) {
				logger.log((Unit)airlock.getEntity(), person, Level.FINE, 60_000,
						"Cannot transition to zone 4 in " + airlock.getEntity().toString() + ".");
				return remainingTime;
			}
						
			if (!airlock.isOuterDoorLocked() || airlock.isEmpty()) {
				// The outer door will stay locked if the chamber is NOT depressurized
				// If the airlock is empty, it means no one is using it
				canProceed = true;
			}
		}

		else {

			if (exteriorDoorPos == null) {
				exteriorDoorPos = airlock.getAvailableExteriorPosition();
			}

			if (exteriorDoorPos.isClose(person.getPosition())) {

				if (airlock.addAwaitingOuterDoor(id)) {
					canProceed = true;
				}
			}

			else {
				Rover airlockRover = (Rover) airlock.getEntity();

				// Walk to exterior door position.
				addSubTask(new WalkOutside(person, person.getPosition(),
											exteriorDoorPos, true));

				logger.log((Unit)airlock.getEntity(), person, Level.FINE, 4_000,
						"Attempted to step closer to " + airlockRover.getNickName() + "'s exterior door.");
			}
		}

		if (canProceed) {

			if (airlock.isDepressurized() && !airlock.isOuterDoorLocked()) {
				// If airlock has already been depressurized,
				// then it's ready for entry

				logger.log((Unit)airlock.getEntity(), person, Level.FINE, 4_000,
						"Chamber already depressurized for entry in " + airlock.getEntity().toString() + ".");

				// Skip DEPRESSURIZE_CHAMBER phase and go to the ENTER_AIRLOCK phase
				setPhase(ENTER_AIRLOCK);
			}

			else {

				if (!airlock.isActivated()) {
					// Only the airlock operator may activate the airlock
					airlock.setActivated(true);
				}
				
				if (airlock.isOperator(id)) {
					
//					// since it's not depressurized, will need to depressurize the chamber first
//					if (!airlock.isActivated()) {
//						// Only the airlock operator may activate the airlock
//						airlock.setActivated(true);
//					}

					logger.log((Unit)airlock.getEntity(), person, Level.FINE, 4_000, "Ready to depressurize the chamber.");

					if (!airlock.isDepressurized() || !airlock.isDepressurizing()) {
						// Only the operator has the authority to start the depressurization
						setPhase(DEPRESSURIZE_CHAMBER);
					}
				}
			}
		}

		return remainingTime;
	}

	/**
	 * Depressurizes the chamber.
	 *
	 * @param time
	 * @return
	 */
	private double depressurizeChamber(double time) {

		double remainingTime = 0;

		if (airlock.isDepressurized() && !airlock.isOuterDoorLocked()) {

			logger.log((Unit)airlock.getEntity(), person, Level.FINE, 4_000,
					"Chamber already depressurized for entry in " + airlock.getEntity().toString() + ".");

			// Add experience
			addExperience(time);

			setPhase(ENTER_AIRLOCK);
		}

		else if (airlock.isDepressurizing()) {
			// just wait for depressurizing to finish
		}

		else {

			Set<Person> list = airlock.noEVASuit();
			if (list.size() > 0) {
				logger.log((Unit)airlock.getEntity(), person, Level.WARNING, 4_000,
						"Could not depressurize " + airlock.getEntity().toString() + ". "
						+ list + " still inside not wearing EVA suit.");
				return remainingTime;
			}

			if (!airlock.isActivated()) {
				// Only the airlock operator may activate the airlock
				airlock.setActivated(true);
			}
			
			if (airlock.isOperator(id)) {
				// Command the airlock state to be transitioned to "depressurizing"
				airlock.setTransitioning(true);
				
				// Depressurizing the chamber
//				boolean succeed = airlock.setDepressurizing();
//				if (!succeed) {
//					logger.log((Unit)airlock.getEntity(), person, Level.WARNING, 4_000,
//							"Could not depressurize " + airlock.getEntity().toString() + ".");
//				}
//
//				else {
//					logger.log((Unit)airlock.getEntity(), person, Level.FINE, 4_000,
//							"Depressurizing " + airlock.getEntity().toString() + ".");
//				}
			}
		}

		return remainingTime;
	}

	/**
	 * Enters through the outer door into the chamber of the airlock.
	 *
	 * @param time
	 * @return
	 */
	private double enterAirlock(double time) {

		double remainingTime = 0;

		boolean canProceed = false;

		if (!airlock.isDepressurized()) {
			// Go back to the previous phase
			setPhase(DEPRESSURIZE_CHAMBER);
			
			return remainingTime;
		}

		if (inSettlement) {

//			if (exteriorDoorPos == null) {
//				exteriorDoorPos = airlock.getAvailableExteriorPosition();
//			}

//			if (LocalAreaUtil.areLocationsClose(new LocalPosition.Double(person.getXLocation(), person.getYLocation()),
//					exteriorDoorPos)) {

				if (airlock.isOuterDoorLocked()) {
					logger.log((Unit)airlock.getEntity(), person, Level.WARNING, 4_000,
							"Outer door locked in " + airlock.getEntity() + ".");
					return remainingTime;
				}
				
				if (airlock.isChamberFull() || !airlock.hasSpace())  {
					logger.log((Unit)airlock.getEntity(), person, Level.WARNING, 4_000,
							"Chamber full in " + airlock.getEntity() + ".");
					return remainingTime;
				}
				
				if (!airlock.inAirlock(person)) {
					canProceed = airlock.enterAirlock(person, id, false);
				}
				else // true if the person is already inside the airlock from previous cycle
					canProceed = true;

				if (canProceed && transitionTo(3)) {
					canProceed = true;
				}

				else {
                    // true if the person is already inside the chamber from previous cycle
                    canProceed = isInZone(2);
				}
//			}
//
//			else {
//
//				// Walk to exterior door position.
//				addSubTask(new WalkOutside(person, person.getXLocation(), person.getYLocation(), exteriorDoorPos.getX(),
//						exteriorDoorPos.getY(), true));
//				logger.log((Unit)airlock.getEntity(), person, Level.FINE, 4_000,
//						"Attempted to come closer to the airlock's exterior door in " + airlock.getEntity());
//			}
		}

		else {

//			if (exteriorDoorPos == null) {
//				exteriorDoorPos = airlock.getAvailableExteriorPosition();
//			}

//			if (LocalAreaUtil.areLocationsClose(new LocalPosition.Double(person.getXLocation(), person.getYLocation()),
//					exteriorDoorPos)) {

				if (!airlock.isOuterDoorLocked()) {

					if (!airlock.inAirlock(person)) {
						canProceed = airlock.enterAirlock(person, id, false);
					}
					else // the person is already inside the airlock from previous cycle
						canProceed = true;
				}

				else {
					setPhase(REQUEST_INGRESS);
					return remainingTime;
				}
//			}
//
//			else {
//				Rover airlockRover = (Rover) airlock.getEntity();
//
//				// Walk to exterior door position.
//				addSubTask(new WalkOutside(person, person.getXLocation(), person.getYLocation(), exteriorDoorPos.getX(),
//						exteriorDoorPos.getY(), true));
//
//				logger.log((Unit)airlock.getEntity(), person, Level.FINE, 4_000,
//						"Attempted to come closer to " + airlockRover.getNickName() + "'s exterior door.");
//			}
		}

		if (canProceed) {
			logger.log((Unit)airlock.getEntity(), person, Level.FINE, 4_000,
					"Just entered through the outer door into " + airlock.getEntity().toString() + ".");

			// Add experience
			addExperience(time);

			setPhase(WALK_TO_CHAMBER);
		}

		return remainingTime;
	}

	/**
	 * Walks to the chamber.
	 *
	 * @param time
	 * @return
	 */
	private double walkToChamber(double time) {

		double remainingTime = 0;
		
		logger.log((Unit)airlock.getEntity(), person, Level.FINE, 4_000,
				"Walking to a chamber in " + airlock.getEntity().toString() + ".");

		boolean canProceed = false;

		if (inSettlement) {

			if (transitionTo(2)) {
				canProceed = true;
			}
			else {
				setPhase(ENTER_AIRLOCK);
				return 0;
			}
		}

		else {

			if (insideAirlockPos == null) {
				insideAirlockPos = airlock.getAvailableAirlockPosition();
			}

			if (insideAirlockPos.isClose(person.getPosition())) {
				canProceed = true;
			}

			else {
				Rover airlockRover = (Rover) airlock.getEntity();
				logger.log((Unit)airlock.getEntity(), person, Level.FINE, 4_000, "Walked to the reference position.");
 
				// Walk to interior airlock position.
				addSubTask(new WalkRoverInterior(person, airlockRover, insideAirlockPos));
			}
		}

		if (canProceed) {

			if (!airlock.isActivated()) {
				// Only the airlock operator may activate the airlock
				airlock.setActivated(true);
			}
			
			if (airlock.isOperator(id)) {
				// Elect an operator to handle this task
				if (!airlock.isPressurized() || !airlock.isPressurizing()) {
					// Get ready for pressurization
					setPhase(PRESSURIZE_CHAMBER);
				}
			}

			if (airlock.isPressurized()) {
				logger.log((Unit)airlock.getEntity(), person, Level.FINE, 4_000,
						"Chamber alraedy pressurized for entry in " + airlock.getEntity().toString() + ".");

				// Reset the count down doffing time
				remainingDoffingTime = STANDARD_DOFFING_TIME + RandomUtil.getRandomInt(-2, 2);

				setPhase(DOFF_EVA_SUIT);
			}

			// Add experience
			addExperience(time);
		}

		return remainingTime;

	}

	/**
	 * Pressurizes the chamber.
	 *
	 * @param time
	 * @return
	 */
	private double pressurizeChamber(double time) {

		double remainingTime = 0;

		if (airlock.isPressurized()) {

			logger.log((Unit)airlock.getEntity(), person, Level.FINE, 4_000,
					"Chamber alraedy pressurized for entry in " + airlock.getEntity().toString() + ".");

			// Add experience
			addExperience(time);
			// Start the count down doffing time
			remainingDoffingTime = STANDARD_DOFFING_TIME + RandomUtil.getRandomInt(-2, 2);

			setPhase(DOFF_EVA_SUIT);
		}

		else if (airlock.isPressurizing()) {
			// just wait for pressurizing to finish
		}

		else {

			if (!airlock.isActivated()) {
				// Only the airlock operator may activate the airlock
				airlock.setActivated(true);
			}
			
			if (airlock.isOperator(id)) {
				// Command the airlock state to be transitioned to "pressurized"
				airlock.setTransitioning(true);
				
				// TODO: if someone is waiting outside the outer door, ask the C2 to unlock
				// outer door to let him in before pressurizing

				// Pressurizing the chamber
//				boolean succeed = airlock.setPressurizing();
//				
//				if (!succeed) {
//					logger.log((Unit)airlock.getEntity(), person, Level.WARNING, 4_000,
//							"Could not pressurize " + airlock.getEntity().toString() + ".");
//				}
//
//				else {
//					logger.log((Unit)airlock.getEntity(), person, Level.FINE, 4_000,
//							"Pressurizing " + airlock.getEntity().toString() + ".");
//				}
			}
		}

		return remainingTime;
	}

	/**
	 * Doffs the EVA suit.
	 *
	 * @param time
	 * @return
	 */
	private double doffEVASuit(double time) {

		double remainingTime = 0;

		if (!airlock.isPressurized()) {
			// Go back to the previous phase
			setPhase(PRESSURIZE_CHAMBER);
			return remainingTime;
		}

		if (airlock.isPressurized()) {

			// 1. Gets the suit instance
			EVASuit suit = person.getSuit();

			if (suit != null) {

				remainingDoffingTime -= time;

				if (remainingDoffingTime <= 0) {

					EquipmentOwner housing = null;

					if (inSettlement)
						housing = ((Building)airlock.getEntity()).getSettlement();
					else
						housing = (Vehicle)airlock.getEntity();
					// 2. Doff this suit
					// 2a. Records the person as the owner (if it hasn't been done)
					suit.setLastOwner(person);
					// 2b. Doff this suit. Deregister the suit from the person
					person.registerSuit(null);
					// Print log
					logger.log((Unit)housing, person, Level.FINE, 4_000, "Just doffed the " + suit.getName() + ".");
					// 2c. Transfer the EVA suit from person to the new destination
					suit.transfer((Unit)housing);
					// 2d. Remove pressure suit and put on garment
					if (inSettlement) {
						if (person.unwearPressureSuit(housing)) {
							person.wearGarment(housing);
						}
					}
					// Note: vehicle may or may not have garment available
					else if (((Rover)housing).hasGarment() && person.unwearPressureSuit(housing)) {
						person.wearGarment(housing);
					}
					// 2e. Unload any waste
					suit.unloadWaste(housing);
					// Add experience
					addExperience(time);

					remainingCleaningTime = STANDARD_CLEANINNG_TIME + RandomUtil.getRandomInt(-2, 2);

					setPhase(CLEAN_UP);
				}
			}

			else {
//				logger.log((Unit)airlock.getEntity(), person, Level.WARNING, 4_000,
//						"did not possess an EVA suit in " + airlock.getEntity().toString()
//						+ ".");

				remainingCleaningTime = STANDARD_CLEANINNG_TIME + RandomUtil.getRandomInt(-2, 2);

				setPhase(CLEAN_UP);
			}
		}

		else {
			logger.log((Unit)airlock.getEntity(), person, Level.WARNING, 4_000,
				"Not pressurized. Walking back to the chamber and wait.");

			// It's not pressurized yet, go back to the WALK_TO_CHAMBER phase and wait
			setPhase(WALK_TO_CHAMBER);
		}

		return remainingTime;
	}


	/**
	 * Performs cleaning up of EVA suit and onself.
	 *
	 * @param time
	 * @return
	 */
	private double cleanUp(double time) {

		double remainingTime = 0;

		if (!airlock.isPressurized()) {
			// Go back to the previous phase
			setPhase(PRESSURIZE_CHAMBER);
			return remainingTime;
		}

		boolean doneCleaning = false;

		remainingCleaningTime -= time;

		if (remainingCleaningTime <= 0) {
			logger.log((Unit)airlock.getEntity(), person, Level.FINE, 4_000, "Completed the clean-up.");
			doneCleaning = true;
		}

		if (doneCleaning) {

			if (inSettlement) {
				if (transitionTo(1)) {
	//				// do nothing
				}
			}

			else {
	//			// do nothing
			}

			// Add experience
			addExperience(time);

			setPhase(LEAVE_AIRLOCK);
		}

		return remainingTime;
	}

	/**
	 * Departs the chamber through the inner door of the airlock.
	 *
	 * @param time
	 * @return
	 */
	private double leaveAirlock(double time) {

		double remainingTime = 0;

		boolean canExit = false;

		if (inSettlement) {

			if (transitionTo(0)) {

				if (airlock.inAirlock(person)) {

					canExit = airlock.exitAirlock(person, id, false);

					if (canExit)
						// Remove the position at zone 0 before calling endTask
						airlock.vacate(0, id);
				}
			}
		}

		else {

			if (interiorDoorPos == null) {
				interiorDoorPos = airlock.getAvailableInteriorPosition();
			}

			if (interiorDoorPos.isClose(person.getPosition())) {
				if (airlock.inAirlock(person)) {
					canExit = airlock.exitAirlock(person, id, false);
				}
			}

			else {
				Rover airlockRover = (Rover) airlock.getEntity();
				logger.log((Unit)airlock.getEntity(), person, Level.FINE, 4_000,
						"Attempted to step closer to " + airlockRover.getNickName() + "'s inner door.");

				addSubTask(new WalkRoverInterior(person, airlockRover, interiorDoorPos));
			}
		}

		if (canExit) {

			// Add experience
			addExperience(time);

			logger.log((Unit)airlock.getEntity(), person, Level.FINE, 4_000,
					"Departing " + airlock.getEntity().toString() + ".");

			// This completes the EVA ingress through the airlock
			completeAirlockTask();
		}

		return remainingTime;
	}

	/**
	 * Checks if a person can enter an airlock from an EVA.
	 *
	 * @param person  the person trying to enter
	 * @param airlock the airlock to be used.
	 * @return true if person can enter the airlock
	 */
	public static boolean canEnterAirlock(Person person, Airlock airlock) {

		boolean result = true;

		if (person.isInside()) {
			logger.log((Unit)airlock.getEntity(), person, Level.WARNING, 4_000,
					"Could not enter " + airlock.getEntityName()
					+ ". Already inside and not outside.");
			result = false;
		}

		else if (airlock.isChamberFull() || !airlock.hasSpace()) {
			logger.log((Unit)airlock.getEntity(), person, Level.INFO, 20_000,
					"Could not enter " + airlock.getEntityName()
					+ ". Already full.");
			result = false;
		}

		return result;
	}

	@Override
	protected void clearDown() {
		// Clear the person as the airlock operator if task ended prematurely.
		if (airlock != null && person.getName().equals(airlock.getOperatorName())) {
			if (inSettlement) {
				logger.log((Unit)airlock.getEntity(), person, Level.FINE, 4_000,
							"Concluded the airlock operator task.");
			}
			else {
				logger.log((Unit)airlock.getEntity(), person, Level.FINE, 4_000,
						"Concluded the vehicle airlock operator task.");
			}

			airlock.removeID(id);
		}
	}

	/**
	 * Removes the person from airlock and walk away and ends the airlock and walk
	 * tasks.
	 */
	public void completeAirlockTask() {
		// Clear the person as the airlock operator if task ended prematurely.
		if (airlock != null && person.getName().equals(airlock.getOperatorName())) {
			if (airlock.getEntity() instanceof Building) {
				logger.log((Unit)airlock.getEntity(), person, Level.FINE, 4_000,
						"Concluded the building airlock operator task.");
			}
			else {
				logger.log((Unit)airlock.getEntity(), person, Level.FINE, 4_000,
						"Concluded the vehicle airlock operator task.");
			}

			airlock.removeID(id);
		}

		// Ends the sub task 2 within the EnterAirlock task
		// Note: when is calling endSubTask2() needed ?
//		endSubTask2();

		// Remove all lingering tasks to avoid any unfinished walking tasks
//		person.getMind().getTaskManager().endSubTask();

		// Walk away from this airlock anywhere in the settlement
		walkToRandomLocation(false);

		super.endTask();
	}

	/**
	 * Can this Task be recorded ?
	 * 
	 * @return false
	 */
	@Override
	protected boolean canRecord() {
		return false;
	}
}
