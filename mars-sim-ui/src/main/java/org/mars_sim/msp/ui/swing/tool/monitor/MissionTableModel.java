/*
 * Mars Simulation Project
 * MissionTableModel.java
 * @date 2022-07-28
 * @author Scott Davis
 */
package org.mars_sim.msp.ui.swing.tool.monitor;

import java.util.ArrayList;
import java.util.List;

import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;

import org.mars.sim.tools.Msg;
import org.mars_sim.msp.core.GameManager;
import org.mars_sim.msp.core.GameManager.GameMode;
import org.mars_sim.msp.core.Simulation;
import org.mars_sim.msp.core.person.ai.mission.ConstructionMission;
import org.mars_sim.msp.core.person.ai.mission.Mission;
import org.mars_sim.msp.core.person.ai.mission.MissionEvent;
import org.mars_sim.msp.core.person.ai.mission.MissionEventType;
import org.mars_sim.msp.core.person.ai.mission.MissionListener;
import org.mars_sim.msp.core.person.ai.mission.MissionManager;
import org.mars_sim.msp.core.person.ai.mission.MissionManagerListener;
import org.mars_sim.msp.core.person.ai.mission.MissionPlanning;
import org.mars_sim.msp.core.person.ai.mission.PlanType;
import org.mars_sim.msp.core.person.ai.mission.VehicleMission;
import org.mars_sim.msp.core.structure.Settlement;
import org.mars_sim.msp.core.time.MarsTime;
import org.mars_sim.msp.core.vehicle.GroundVehicle;
import org.mars_sim.msp.core.vehicle.Vehicle;

/**
 * This class model how mission data is organized and displayed
 * within the Monitor Window for all settlements.
 */
@SuppressWarnings("serial")
public class MissionTableModel extends AbstractTableModel
		implements MonitorModel, MissionManagerListener, MissionListener {

	// Column indexes
	/** Date filed column. */
	private static final int DATE_FILED = 0;
	/** Date Embarked column. */
	private static final int DATE_EMBARKED = 1;
	/** Date Returned column. */
	private static final int DATE_RETURNED = 2;
	/** Starting member column. */
	private static final int STARTING_MEMBER = 3;
	/** Name ID column. */
	private static final int TYPE_ID = 4;
	/** Description column. */
	private static final int DESIGNATION = 5;
	/** Phase column. */
	private static final int PHASE = 6;
	/** Mission vehicle column. */
	private static final int VEHICLE = 7;
	/** Starting settlement column. */
	private static final int STARTING_SETTLEMENT = 8;
	/** Member number column. */
	private static final int MEMBER_NUM = 9;
	/** Navpoint number column. */
	private static final int NAVPOINT_NUM = 10;
	/** Remaining distance to next navpoint column. */
	private static final int REMAINING_DISTANCE_TO_NEXT_NAVPOINT = 11;
	/** Remaining distance column. */
	private static final int TOTAL_REMAINING_DISTANCE = 12;
	/** Travelled distance column. */
	private static final int TRAVELLED_DISTANCE = 13;
	/** Proposed route distance column. */
	private static final int PROPOSED_ROUTE_DISTANCE = 14;
	/** The number of Columns. */
	private static final int COLUMNCOUNT = 15;
	/** Names of Columns. */
	private static String[] columnNames;
	/** Types of Columns. */
	private static Class<?>[] columnTypes;

	private GameMode mode = GameManager.getGameMode();
	
	private List<Mission> missionCache;

	private Settlement commanderSettlement;

	private boolean monitorMissions = false;

	private MissionManager missionManager;

	static {
		columnNames = new String[COLUMNCOUNT];
		columnTypes = new Class[COLUMNCOUNT];
		columnNames[DATE_FILED] = Msg.getString("MissionTableModel.column.filed"); //$NON-NLS-1$
		columnTypes[DATE_FILED] = MarsTime.class;
		columnNames[DATE_EMBARKED] = Msg.getString("MissionTableModel.column.embarked"); //$NON-NLS-1$
		columnTypes[DATE_EMBARKED] = MarsTime.class;
		columnNames[DATE_RETURNED] = Msg.getString("MissionTableModel.column.returned"); //$NON-NLS-1$
		columnTypes[DATE_RETURNED] = MarsTime.class;
		columnNames[STARTING_MEMBER] = Msg.getString("MissionTableModel.column.name"); //$NON-NLS-1$
		columnTypes[STARTING_MEMBER] = String.class;
		columnNames[TYPE_ID] = Msg.getString("MissionTableModel.column.typeID"); //$NON-NLS-1$
		columnTypes[TYPE_ID] = String.class;
		columnNames[DESIGNATION] = Msg.getString("MissionTableModel.column.designation"); //$NON-NLS-1$
		columnTypes[DESIGNATION] = String.class;
		columnNames[PHASE] = Msg.getString("MissionTableModel.column.phase"); //$NON-NLS-1$
		columnTypes[PHASE] = String.class;
		columnNames[STARTING_SETTLEMENT] = Msg.getString("MissionTableModel.column.startingSettlement"); //$NON-NLS-1$
		columnTypes[STARTING_SETTLEMENT] = String.class;
		columnNames[VEHICLE] = Msg.getString("MissionTableModel.column.vehicle"); //$NON-NLS-1$
		columnTypes[VEHICLE] = String.class;
		columnNames[MEMBER_NUM] = Msg.getString("MissionTableModel.column.members"); //$NON-NLS-1$
		columnTypes[MEMBER_NUM] = Integer.class;
		columnNames[NAVPOINT_NUM] = Msg.getString("MissionTableModel.column.navpoints"); //$NON-NLS-1$
		columnTypes[NAVPOINT_NUM] = Integer.class;
		columnNames[TRAVELLED_DISTANCE] = Msg.getString("MissionTableModel.column.distanceTravelled"); //$NON-NLS-1$
		columnTypes[TRAVELLED_DISTANCE] = Integer.class;
		columnNames[TOTAL_REMAINING_DISTANCE] = Msg.getString("MissionTableModel.column.totalRemaining"); //$NON-NLS-1$
		columnTypes[TOTAL_REMAINING_DISTANCE] = Integer.class;
		columnNames[REMAINING_DISTANCE_TO_NEXT_NAVPOINT] = Msg.getString("MissionTableModel.column.legRemaining"); //$NON-NLS-1$
		columnTypes[REMAINING_DISTANCE_TO_NEXT_NAVPOINT] = Integer.class;		
		columnNames[PROPOSED_ROUTE_DISTANCE] = Msg.getString("MissionTableModel.column.proposedDistance"); //$NON-NLS-1$
		columnTypes[PROPOSED_ROUTE_DISTANCE] = Integer.class;
	}

	/**
	 * Constructor.
	 */
	public MissionTableModel(Simulation sim) {

		missionManager = sim.getMissionManager();
		if (mode == GameMode.COMMAND) {
			commanderSettlement = sim.getUnitManager().getCommanderSettlement();

			// Must take a copy
			missionCache = new ArrayList<>(missionManager.getMissionsForSettlement(commanderSettlement));
		}
		else {
			// Must take my own copy
			missionCache = new ArrayList<>(missionManager.getMissions());
		}

		missionManager.addListener(this);
	}
		
	/**
	 * Set whether the changes to the Missions should be monitor for change. Set up the 
	 * Missionlisteners for the Mission in the table.
	 * @param activate 
	 */
    public void setMonitorEntites(boolean activate) {
		if (activate != monitorMissions) {
			if (activate) {
				for(Mission m : missionCache) {
					if (!m.isDone()) {
						m.addMissionListener(this);
					}
				}
			}
			else {
				for(Mission m : missionCache) {
					m.removeMissionListener(this);
				}
			}
			monitorMissions = activate;
		}
	}


	/**
	 * Cannot filter missions by Settlement although is should be possible.
	 */
	@Override
	public boolean setSettlementFilter(Settlement filter) {
		// Mission doesn't support filtering ???
		return false;
	}

	/**
	 * Gets the name of this model. The name will be a description helping the user
	 * understand the contents.
	 *
	 * @return Descriptive name.
	 */
	@Override
	public String getName() {
		return Msg.getString("MissionTableModel.tabName"); //$NON-NLS-1$
	}

	/**
	 * Adds a new mission.
	 *
	 * @param mission the new mission.
	 */
	@Override
	public void addMission(Mission mission) {
		if (missionCache.contains(mission))
			return;

		boolean goodToGo = false;
		if (mode == GameMode.COMMAND) {
			if (mission.getStartingPerson().getAssociatedSettlement().getName()
					.equals(commanderSettlement.getName())) {
				goodToGo = true;
			}
		}

		else {
			goodToGo = true;
		}

		if (goodToGo) {
			missionCache.add(mission);
			mission.addMissionListener(this);

			// Inform listeners of new row
			fireTableRowsInserted(0, missionCache.size() - 1);
		}
	}

	/**
	 * Removes an old mission.
	 *
	 * @param mission the old mission.
	 */
	@Override
	public void removeMission(Mission mission) {
		if (missionCache.contains(mission)) {
			int index = missionCache.indexOf(mission);
			missionCache.remove(mission);
			mission.removeMissionListener(this);

			// Delete a particular row
			SwingUtilities.invokeLater(new MissionTableRowDeleter(index));
		}
	}

	/**
	 * Returns the type of the column requested.
	 *
	 * @param columnIndex Index of column.
	 * @return Class of specified column.
	 */
	@Override
	public Class<?> getColumnClass(int columnIndex) {
		if ((columnIndex >= 0) && (columnIndex < columnTypes.length)) {
			return columnTypes[columnIndex];
		}
		return Object.class;
	}

	/**
	 * Returns the name of the column requested.
	 *
	 * @param columnIndex Index of column.
	 * @return name of specified column.
	 */
	@Override
	public String getColumnName(int columnIndex) {
		if ((columnIndex >= 0) && (columnIndex < columnNames.length)) {
			return columnNames[columnIndex];
		}
		return Msg.getString("unknown"); //$NON-NLS-1$
	}

	/**
	 * Returns the object at the specified row indexes.
	 *
	 * @param row Index of the row object.
	 * @return Object at the specified row.
	 */
	@Override
	public Object getObject(int row) {
		return missionCache.get(row);
	}

	/**
	 * Has this model got a natural order that the model conforms to. If this value
	 * is true, then it implies that the user should not be allowed to order.
	 */
	public boolean getOrdered() {
		return false;
	}

	/**
	 * Gets the model count string.
	 */
	@Override
	public String getCountString() {
		return "  " + Msg.getString("MissionTableModel.numberOfMissions", //$NON-NLS-2$
				missionCache.size());
	}

	/**
	 * Catch mission update event.
	 *
	 * @param event the mission event.
	 */
	@Override
	public void missionUpdate(MissionEvent event) {

		int index = missionCache.indexOf(event.getSource());

		if (index > -1) {
			MissionEventType eventType = event.getType();

			int column0 = -1;

			if (eventType == MissionEventType.VEHICLE_EVENT)
				column0 = VEHICLE;
			else if (eventType == MissionEventType.STARTING_SETTLEMENT_EVENT)
				column0 = STARTING_SETTLEMENT;
			else if (eventType == MissionEventType.TYPE_ID_EVENT)
				column0 = TYPE_ID;
			else if (eventType == MissionEventType.DESIGNATION_EVENT)
				column0 = DESIGNATION;
			else if (eventType == MissionEventType.ADD_MEMBER_EVENT
					|| eventType == MissionEventType.REMOVE_MEMBER_EVENT)
				column0 = MEMBER_NUM;
			else if (eventType == MissionEventType.DATE_EVENT)
				column0 = DATE_FILED;
			else if (eventType == MissionEventType.NAME_EVENT)
				column0 = STARTING_MEMBER;

			if (column0 > -1)
				SwingUtilities.invokeLater(new MissionTableCellUpdater(index, column0));

			if (event.getSource() instanceof VehicleMission) {

				int column1 = -1;
				int column2 = -1;
				int column3 = -1;
				int column4 = -1;
				int column5 = -1;
				int column6 = -1;
				
				if (eventType == MissionEventType.DISTANCE_EVENT) {
					column1 = TRAVELLED_DISTANCE;
					column2 = TOTAL_REMAINING_DISTANCE;
					column3 = REMAINING_DISTANCE_TO_NEXT_NAVPOINT;
					column4 = PROPOSED_ROUTE_DISTANCE;
				}

				if (eventType == MissionEventType.NAVPOINTS_EVENT)
					column5 = NAVPOINT_NUM;

				if (eventType == MissionEventType.PHASE_EVENT
						|| eventType == MissionEventType.PHASE_DESCRIPTION_EVENT)
					column6 = PHASE;

				// TODO THis is pretty bad. Shoudl rework to only fire a single invokeLater uisng a columns range
				if (column0 > -1)
					SwingUtilities.invokeLater(new MissionTableCellUpdater(index, column1));
				if (column1 > -1)
					SwingUtilities.invokeLater(new MissionTableCellUpdater(index, column2));
				if (column2 > -1)
					SwingUtilities.invokeLater(new MissionTableCellUpdater(index, column3));
				if (column3 > -1)
					SwingUtilities.invokeLater(new MissionTableCellUpdater(index, column4));
				if (column4 > -1)
					SwingUtilities.invokeLater(new MissionTableCellUpdater(index, column5));
				if (column5 > -1)
					SwingUtilities.invokeLater(new MissionTableCellUpdater(index, column6));
			}
		}
	}

	@Override
	public int getRowCount() {
		return missionCache.size();
	}

	/**
	 * Return the number of columns
	 *
	 * @return column count.
	 */
	@Override
	public int getColumnCount() {
		return COLUMNCOUNT;
	}

	/**
     * Default implementation return null as no tooltips are supported by default
     * @param rowIndex Row index of cell
     * @param columnIndex Column index of cell
     * @return Return null by default
     */
    @Override
    public String getToolTipAt(int rowIndex, int columnIndex) {
        return null;
    }

	/**
	 * Returns the value of a Cell.
	 *
	 * @param rowIndex    Row index of the cell.
	 * @param columnIndex Column index of the cell.
	 */
	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		Object result = null;

		if (rowIndex < missionCache.size()) {
			Mission mission = missionCache.get(rowIndex);

			if (mission != null) {
				switch (columnIndex) {

				case DATE_FILED: {
					result = mission.getLog().getDateCreated();
				}
					break;

				case DATE_EMBARKED: {
					result = mission.getLog().getDateStarted();
				}
					break;

				case DATE_RETURNED : {
					result = mission.getLog().getDateFinished();

				}
					break;

				case STARTING_MEMBER: {
					result = mission.getStartingPerson().getName();
				}
					break;

				case TYPE_ID: {
					result = mission.getName();
				}
					break;

				case DESIGNATION: {
					result = mission.getFullMissionDesignation();
				}
					break;

				case PHASE: {
					MissionPlanning plan = mission.getPlan();
					if ((plan != null) && plan.getStatus() == PlanType.PENDING) {
						int percent = (int) plan.getPercentComplete();
						if (percent > 100)
							percent = 100;
						int score = (int)plan.getScore();
						int min = (int)mission.getAssociatedSettlement().getMinimumPassingScore();
						result = percent + "% Reviewed - Score: " + score + " [Min: " + min + "]";
					}
					else
						result = mission.getPhaseDescription();

				}
					break;

				case VEHICLE: {
					result = "";
					Vehicle reserved = null;
					if (mission instanceof VehicleMission vm) {
						reserved = vm.getVehicle();
					} else if (mission instanceof ConstructionMission constructionMission) {
						List<GroundVehicle> constVehicles = constructionMission.getConstructionVehicles();
						if (!constVehicles.isEmpty()) {
							reserved = constVehicles.get(0);
						}
					}
					if (reserved != null) {
						result = reserved.getName();
					}
				}
					break;

				case STARTING_SETTLEMENT: {
					Settlement s = mission.getAssociatedSettlement();
					result = (s != null? s.getName() : null);
				}
					break;

				case MEMBER_NUM: {
					result = mission.getSignup().size();
				}
					break;

				case NAVPOINT_NUM: {
					if (mission instanceof VehicleMission vm) {
						result = vm.getNavpoints().size();
					} else
						result = 0;
				}
					break;

				case TRAVELLED_DISTANCE: {
					if (mission instanceof VehicleMission vm) {
						result = vm.getTotalDistanceTravelled();
					} else
						result = 0;
				}
					break;

				case TOTAL_REMAINING_DISTANCE: {
					if (mission instanceof VehicleMission vm) {
						result = vm.getTotalDistanceRemaining();
					} else
						result = 0;
				}

				break;

				case REMAINING_DISTANCE_TO_NEXT_NAVPOINT: {
					if (mission instanceof VehicleMission vm) {
						result = vm.getDistanceCurrentLegRemaining();
					} else
						result = 0;
				}

				break;
				
				case PROPOSED_ROUTE_DISTANCE: {
					if (mission instanceof VehicleMission vm) {
						result = vm.getDistanceProposed();
					} else
						result = 0;
				}

				break;
				
				default:
					result = "";
				}
			}
		}

		return result;
	}

	/**
	 * Prepares the model for deletion.
	 */
	@Override
	public void destroy() {
		Object[] missions = missionCache.toArray();
		for (int x = 0; x < missions.length; x++) {
			removeMission((Mission) missions[x]);
		}

		missionManager.removeListener(this);
	}

	/**
	 * Inner class for updating mission table cell.
	 */
	private class MissionTableCellUpdater implements Runnable {

		private int row;
		private int column;

		private MissionTableCellUpdater(int row, int column) {
			this.row = row;
			this.column = column;
		}

		public void run() {
			if ((row < getRowCount()) && (column < getColumnCount()))
				fireTableCellUpdated(row, column);
		}
	}

	private class MissionTableRowDeleter implements Runnable {

		private int row;

		private MissionTableRowDeleter(int row) {
			this.row = row;
		}

		public void run() {
			if (row < getRowCount())
				fireTableRowsDeleted(row, row);
		}
	}
}
