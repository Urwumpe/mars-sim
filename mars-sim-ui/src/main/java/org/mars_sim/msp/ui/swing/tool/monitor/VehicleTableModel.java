/*
 * Mars Simulation Project
 * VehicleTableModel.java
 * @date 2021-10-23
 * @author Barry Evans
 */
package org.mars_sim.msp.ui.swing.tool.monitor;

import java.util.List;

import org.mars.sim.mapdata.location.Coordinates;
import org.mars.sim.tools.Msg;
import org.mars_sim.msp.core.Simulation;
import org.mars_sim.msp.core.Unit;
import org.mars_sim.msp.core.UnitEvent;
import org.mars_sim.msp.core.UnitEventType;
import org.mars_sim.msp.core.UnitType;
import org.mars_sim.msp.core.malfunction.Malfunction;
import org.mars_sim.msp.core.person.ai.mission.Mission;
import org.mars_sim.msp.core.person.ai.mission.MissionEvent;
import org.mars_sim.msp.core.person.ai.mission.MissionEventType;
import org.mars_sim.msp.core.person.ai.mission.MissionListener;
import org.mars_sim.msp.core.person.ai.mission.MissionManager;
import org.mars_sim.msp.core.person.ai.mission.MissionManagerListener;
import org.mars_sim.msp.core.person.ai.mission.NavPoint;
import org.mars_sim.msp.core.person.ai.mission.VehicleMission;
import org.mars_sim.msp.core.resource.AmountResource;
import org.mars_sim.msp.core.resource.ResourceUtil;
import org.mars_sim.msp.core.structure.Settlement;
import org.mars_sim.msp.core.structure.building.function.cooking.PreparingDessert;
import org.mars_sim.msp.core.vehicle.Crewable;
import org.mars_sim.msp.core.vehicle.Vehicle;

/**
 * The VehicleTableModel that maintains a list of Vehicle objects.
 * It maps key attributes of the Vehicle into Columns.
 */
@SuppressWarnings("serial")
public class VehicleTableModel extends UnitTableModel<Vehicle> {

	private static final String ON = "On";
	private static final String OFF = "Off";
	private static final String TRUE = "True";
	private static final String FALSE = "False";

	// Column indexes
	private static final int NAME = 0;
	private static final int TYPE = 1;
	private static final int LOCATION = 2;
	private static final int DESTINATION = 3;
	private static final int DESTDIST = 4;
	private static final int MISSION = 5;
	private static final int CREW = 6;
	private static final int DRIVER = 7;
	private static final int STATUS = 8;
	private static final int BEACON = 9;
	private static final int RESERVED = 10;
	private static final int SPEED = 11;
	private static final int MALFUNCTION = 12;
	private static final int OXYGEN = 13;
	private static final int METHANOL = 14;
	private static final int WATER = 15;
	private static final int FOOD = 16;
	private static final int DESSERT = 17;
	private static final int ROCK_SAMPLES = 18;
	private static final int ICE = 19;
	/** The number of Columns. */
	private static final int COLUMNCOUNT = 20;
	/** Names of Columns. */
	private static final ColumnSpec[] COLUMNS;

	/**
	 * Class initialiser creates the static names and classes.
	 */
	static {
		COLUMNS = new ColumnSpec[COLUMNCOUNT];
		COLUMNS[NAME] = new ColumnSpec("Name", String.class);
		COLUMNS[TYPE] = new ColumnSpec("Type", String.class);
		COLUMNS[LOCATION] = new ColumnSpec("Location", String.class);
		COLUMNS[DESTINATION] = new ColumnSpec("Next Waypoint", Coordinates.class);
		COLUMNS[DESTDIST] = new ColumnSpec("Dist. to next [km]", Double.class);
		COLUMNS[MISSION] = new ColumnSpec("Mission", String.class);
		COLUMNS[CREW] = new ColumnSpec("Crew", Integer.class);
		COLUMNS[DRIVER] = new ColumnSpec("Driver", String.class);
		COLUMNS[STATUS] = new ColumnSpec("Status", String.class);
		COLUMNS[BEACON] = new ColumnSpec("Beacon", String.class);
		COLUMNS[RESERVED] = new ColumnSpec("Reserved", String.class);
		COLUMNS[SPEED] = new ColumnSpec("Speed", Double.class);
		COLUMNS[MALFUNCTION] = new ColumnSpec("Malfunction", String.class);
		COLUMNS[OXYGEN] = new ColumnSpec("Oxygen", Double.class);
		COLUMNS[METHANOL] = new ColumnSpec("Methanol", Double.class);
		COLUMNS[WATER] = new ColumnSpec("Water", Double.class);
		COLUMNS[FOOD] = new ColumnSpec("Food", Double.class);
		COLUMNS[DESSERT] = new ColumnSpec("Dessert", Double.class);
		COLUMNS[ROCK_SAMPLES] = new ColumnSpec("Rock Samples", Double.class);
		COLUMNS[ICE] = new ColumnSpec("Ice", Double.class);
	}

	private static final int FOOD_ID = ResourceUtil.foodID;
	private static final int OXYGEN_ID = ResourceUtil.oxygenID;
	private static final int WATER_ID = ResourceUtil.waterID;
	private static final int METHANOL_ID = ResourceUtil.methanolID;
	private static final int ROCK_SAMPLES_ID = ResourceUtil.rockSamplesID;
	private static final int ICE_ID = ResourceUtil.iceID;

	private static final AmountResource [] availableDesserts = PreparingDessert.getArrayOfDessertsAR();

	private static MissionManager missionManager = Simulation.instance().getMissionManager();

	private transient LocalMissionManagerListener missionManagerListener;
	
	public VehicleTableModel(Settlement settlement) {
		super(UnitType.VEHICLE,
			Msg.getString("VehicleTableModel.tabName"),
			"VehicleTableModel.countingVehicles", //$NON-NLS-1$
			COLUMNS
		);

		setSettlementFilter(settlement);

		setCachedColumns(OXYGEN, ICE);

		missionManagerListener = new LocalMissionManagerListener();
	}

	/**
	 * Filter the vehicles to a settlement
	 */
	@Override
	public boolean setSettlementFilter(Settlement filter) {
		resetEntities(filter.getAllAssociatedVehicles());

		return true;
	}

	/**
	 * Returns the value of a Cell.
	 * 
	 * @param rowIndex Row index of the cell.
	 * @param columnIndex Column index of the cell.
	 */
	@Override
	protected Object getEntityValue(Vehicle vehicle, int columnIndex) {
		Object result = null;

		switch (columnIndex) {
			case NAME : {
				result = vehicle.getName();
			} break;

			case TYPE : {
				result = vehicle.getSpecName();
			} break;

			case LOCATION : {
				Settlement settle = vehicle.getSettlement();
				if (settle != null) {
					result = settle.getName();
				}
				else {
					result = vehicle.getCoordinates().getFormattedString();
				}
			} break;

			case DESTINATION : {
				Mission mission = vehicle.getMission();
				if (mission instanceof VehicleMission) {
					VehicleMission vehicleMission = (VehicleMission) mission;

					NavPoint destination = vehicleMission.getCurrentDestination();
					if (destination.isSettlementAtNavpoint())
						result = destination.getSettlement().getName();
					else
						result = destination.getDescription()
							+ " - " + destination.getLocation().getFormattedString();
				}
			} break;

			case DESTDIST : {
				Mission mission = vehicle.getMission();
				if (mission instanceof VehicleMission) {
					VehicleMission vehicleMission = (VehicleMission) mission;
					result = vehicleMission.getDistanceCurrentLegRemaining();
				}
				else result = null;
			} break;

			case MISSION : {
				Mission mission = vehicle.getMission();
				if (mission != null) {
					result = mission.getFullMissionDesignation();
				}
				else result = null;
			} break;

			case CREW : {
				if (vehicle instanceof Crewable)
					result = ((Crewable) vehicle).getCrewNum();
				else result = 0;
			} break;

			case DRIVER : {
				if (vehicle.getOperator() != null) {
					result = vehicle.getOperator().getName();
				}
				else {
					result = null;
				}
			} break;

			case SPEED : {
				result = vehicle.getSpeed();
			} break;

			// Status is a combination of Mechanical failure and maintenance
			case STATUS : {
				result = vehicle.printStatusTypes();
			} break;

			case BEACON : {
				if (vehicle.isBeaconOn()) result = ON;
				else result = OFF;
			} break;

			case RESERVED : {
				if (vehicle.isReserved()) result = TRUE;
				else result = FALSE;
			} break;

			case MALFUNCTION: {
				Malfunction failure = vehicle.getMalfunctionManager().getMostSeriousMalfunction();
				if (failure != null) result = failure.getName();
			} break;


			case WATER : 
				result = vehicle.getAmountResourceStored(WATER_ID);
				break;

			case FOOD : 
				result = vehicle.getAmountResourceStored(FOOD_ID);
				break;

			case DESSERT : 
				double sum = 0;
				for (AmountResource n : availableDesserts) {
					double amount = vehicle.getAmountResourceStored(n.getID());
					sum += amount;
				}
				result = sum;
				break;

			case OXYGEN : 
				result = vehicle.getAmountResourceStored(OXYGEN_ID);
				break;

			case METHANOL : 
				result = vehicle.getAmountResourceStored(METHANOL_ID);
				break;

			case ROCK_SAMPLES : 
				result = vehicle.getAmountResourceStored(ROCK_SAMPLES_ID);
				break;

			case ICE : 
				result = vehicle.getAmountResourceStored(ICE_ID);
				break;
			
			default:
				throw new IllegalArgumentException("Unknown column");
		}

		return result;
	}

	/**
	 * Catches unit update event.
	 * 
	 * @param event the unit event.
	 */
	@Override
	public void unitUpdate(UnitEvent event) {
		Unit unit = (Unit) event.getSource();

		if (unit.getUnitType() == UnitType.VEHICLE) {
			Vehicle vehicle = (Vehicle) unit;
			Object target = event.getTarget();
			UnitEventType eventType = event.getType();

			int columnNum = -1;
			if (eventType == UnitEventType.NAME_EVENT) columnNum = NAME;
			else if (eventType == UnitEventType.LOCATION_EVENT) columnNum = LOCATION;
			else if (eventType == UnitEventType.INVENTORY_STORING_UNIT_EVENT ||
					eventType == UnitEventType.INVENTORY_RETRIEVING_UNIT_EVENT) {
				if (((Unit)target).getUnitType() == UnitType.PERSON) columnNum = CREW;
			}
			else if (eventType == UnitEventType.OPERATOR_EVENT) columnNum = DRIVER;
			else if (eventType == UnitEventType.STATUS_EVENT) columnNum = STATUS;
			else if (eventType == UnitEventType.EMERGENCY_BEACON_EVENT) columnNum = BEACON;
			else if (eventType == UnitEventType.RESERVED_EVENT) columnNum = RESERVED;
			else if (eventType == UnitEventType.SPEED_EVENT) columnNum = SPEED;
			else if (eventType == UnitEventType.MALFUNCTION_EVENT) columnNum = MALFUNCTION;
			else if (eventType == UnitEventType.INVENTORY_RESOURCE_EVENT) {
				int resourceId = -1;
				if (target instanceof AmountResource) {
					resourceId = ((AmountResource)target).getID();
				}
				else if (target instanceof Integer) {
					resourceId = (Integer)target;
					if (resourceId >= ResourceUtil.FIRST_ITEM_RESOURCE_ID)
						// if it's an item resource, quit
						return;
				}

				if (resourceId == OXYGEN_ID) 
					columnNum = OXYGEN;
				else if (resourceId == METHANOL_ID)
					columnNum = METHANOL;
				else if (resourceId == FOOD_ID)
					columnNum = FOOD;
				else if (resourceId == WATER_ID)
					columnNum = WATER;
				else if (resourceId == ROCK_SAMPLES_ID)
					columnNum = ROCK_SAMPLES;
				else if (resourceId == ICE_ID)
					columnNum = ICE;
				else {
					// Put together a list of available dessert
					for(AmountResource ar : availableDesserts) {
						if (resourceId == ar.getID()) {
							columnNum = DESSERT;
						}
					}
				}
			}

			if (columnNum > -1) {
				entityValueUpdated(vehicle, columnNum, columnNum);
			}
		}
	}

	
	
	/**
	 * Prepares the model for deletion.
	 */
	@Override
	public void destroy() {
		super.destroy();

		if (missionManagerListener != null) {
			missionManagerListener.destroy();
		}
		missionManagerListener = null;
	}


	private class LocalMissionManagerListener implements MissionManagerListener {

		private List<Mission> missions;
		private MissionListener missionListener;

		LocalMissionManagerListener() {
			missionListener = new LocalMissionListener();

			missions = missionManager.getMissions();


			for (Mission m : missions)
				addMission(m);
		}

		/**
		 * Adds a new mission.
		 * 
		 * @param mission the new mission.
		 */
		public void addMission(Mission mission) {
			mission.addMissionListener(missionListener);
			updateVehicleMissionCell(mission);
		}

		/**
		 * Removes an old mission.
		 * 
		 * @param mission the old mission.
		 */
		public void removeMission(Mission mission){
			mission.removeMissionListener(missionListener);
			updateVehicleMissionCell(mission);
		}

		private void updateVehicleMissionCell(Mission mission) {
			// Update all table cells because construction/salvage mission may affect more than one vehicle.
			fireTableDataChanged();
		}

		/**
		 * Prepares for deletion.
		 */
		public void destroy() {
			for (Mission m : missions) removeMission(m);
			missions = null;
			missionListener = null;
		}
	}

	/**
	 * MissionListener inner class.
	 */
	private class LocalMissionListener implements MissionListener {

		/**
		 * Catch mission update event.
		 * @param event the mission event.
		 */
		public void missionUpdate(MissionEvent event) {
			Mission mission = (Mission) event.getSource();
			MissionEventType eventType = event.getType();
			int columnNum = -1;
			if (eventType == MissionEventType.TRAVEL_STATUS_EVENT ||
					eventType == MissionEventType.NAVPOINTS_EVENT
					) columnNum = DESTINATION;
			else if (eventType == MissionEventType.DISTANCE_EVENT) columnNum = DESTDIST;
			else if (eventType == MissionEventType.VEHICLE_EVENT) columnNum = MISSION;

			if (columnNum > -1) {
				if (mission instanceof VehicleMission) {
					Vehicle vehicle = ((VehicleMission) mission).getVehicle();
					if (vehicle != null) {
						entityValueUpdated(vehicle, columnNum, columnNum);
					}
				}
			}
		}
	}
}
