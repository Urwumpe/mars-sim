/*
 * Mars Simulation Project
 * TabPanelMaintenance.java
 * @date 2023-06-15
 * @author Scott Davis
 */
package com.mars_sim.ui.swing.unit_window.structure;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumnModel;

import com.mars_sim.core.Unit;
import com.mars_sim.core.malfunction.MalfunctionManager;
import com.mars_sim.core.structure.Settlement;
import com.mars_sim.core.structure.building.Building;
import com.mars_sim.ui.swing.ImageLoader;
import com.mars_sim.ui.swing.MainDesktopPane;
import com.mars_sim.ui.swing.NumberCellRenderer;
import com.mars_sim.ui.swing.unit_window.TabPanel;
import com.mars_sim.ui.swing.utils.PercentageCellRenderer;
import com.mars_sim.ui.swing.utils.UnitModel;
import com.mars_sim.ui.swing.utils.UnitTableLauncher;

/**
 * The TabPanelMaintenance is a tab panel for settlement's building maintenance.
 */
@SuppressWarnings("serial")
public class TabPanelMaintenance extends TabPanel {

	private static final String SPANNER_ICON = "maintenance";

	private BuildingMaintModel tableModel;
	
	protected String[] columnToolTips = {
		    "The Building name", 
		    "The Building Wear and Tear Condition",
		    "The # of Sols since last Inspection",
		    "The Percentage of Completion of Current Inspection"};
		
	/**
	 * Constructor.
	 * 
	 * @param unit    the unit (currently for settlements only)
	 * @param desktop the main desktop.
	 */
	public TabPanelMaintenance(Unit unit, MainDesktopPane desktop) {
		// Use the TabPanel constructor
		super(null, ImageLoader.getIconByName(SPANNER_ICON), "Maintenance", unit, desktop);

		tableModel = new BuildingMaintModel((Settlement) unit);
	}

	@Override
	protected void buildUI(JPanel content) {
		
		JScrollPane maintPane = new JScrollPane();
		maintPane.setPreferredSize(new Dimension(160, 80));
		content.add(maintPane, BorderLayout.CENTER);

		// Create the parts table
		JTable table = new JTable(tableModel) {
		    //Implement table header tool tips.
		    protected JTableHeader createDefaultTableHeader() {
		        return new JTableHeader(columnModel) {
		            public String getToolTipText(MouseEvent e) {
		                java.awt.Point p = e.getPoint();
		                int index = columnModel.getColumnIndexAtX(p.x);
		                int realIndex = 
		                        columnModel.getColumn(index).getModelIndex();
		                return columnToolTips[realIndex];
		            }
		        };
		    }
		};
		
		table.setRowSelectionAllowed(true);
		table.addMouseListener(new UnitTableLauncher(getDesktop()));
		table.setAutoCreateRowSorter(true);

		TableColumnModel tc = table.getColumnModel();
		tc.getColumn(0).setPreferredWidth(120);
		tc.getColumn(1).setPreferredWidth(60);
		tc.getColumn(1).setCellRenderer(new PercentageCellRenderer(false));
		tc.getColumn(2).setCellRenderer(new NumberCellRenderer(2));
		tc.getColumn(2).setPreferredWidth(60);
		tc.getColumn(3).setCellRenderer(new PercentageCellRenderer(false));
		tc.getColumn(3).setPreferredWidth(60);
		maintPane.setViewportView(table);
	}

	/**
	 * Updates the tab panel.
	 */
	@Override
	public void update() {
		tableModel.update();
	}

	private static class BuildingMaintModel extends AbstractTableModel
			implements UnitModel {

		List<Building> buildings;

		public BuildingMaintModel(Settlement settlement) {
			this.buildings = new ArrayList<>(settlement.getBuildingManager().getBuildingSet());
		}

		public void update() {
			fireTableRowsUpdated(0, buildings.size()-1);
		}

		@Override
		public int getRowCount() {
			return buildings.size();
		}

		@Override
		public Class<?> getColumnClass(int columnIndex) {
			switch (columnIndex) {
            case 0:
				return String.class;
			case 1:
				return Integer.class;
			case 2:
				return Double.class;
			case 3:
				return Integer.class;
			default:
                return null;
            }
		}

		@Override
		public String getColumnName(int columnIndex) {
			switch(columnIndex) {
				case 0: 
					return "Building";
				case 1:
					return "Condition";
				case 2:
					return "Last Inspected.";
				case 3:
					return "% Done";
				default:
					return "";
			}
		}

		@Override
		public int getColumnCount() {
			return 4;
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			Building building = buildings.get(rowIndex);
			MalfunctionManager manager = building.getMalfunctionManager();
			switch(columnIndex) {
			case 0: 
				return building.getName();
			case 1:
				return (int)manager.getWearCondition();
			case 2:
				return manager.getTimeSinceLastMaintenance()/1000D;
			case 3: {
				double completed = manager.getMaintenanceWorkTimeCompleted();
				double total = manager.getMaintenanceWorkTime();
				return (int)(100.0 * completed / total);
				}
			default:
				return "";
			}
		}

		@Override
		public Unit getAssociatedUnit(int row) {
			return buildings.get(row);
		}
	}
}
