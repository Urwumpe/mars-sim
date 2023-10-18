/*
 * Mars Simulation Project
 * MaintenanceTabPanel.java
 * @date 2023-06-15
 * @author Scott Davis
 */
package com.mars_sim.ui.swing.unit_window;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumnModel;

import com.mars_sim.core.SimulationConfig;
import com.mars_sim.core.malfunction.MalfunctionManager;
import com.mars_sim.core.malfunction.Malfunctionable;
import com.mars_sim.core.resource.MaintenanceScope;
import com.mars_sim.core.resource.Part;
import com.mars_sim.core.resource.PartConfig;
import com.mars_sim.core.tool.Conversion;
import com.mars_sim.tools.Msg;
import com.mars_sim.ui.swing.ImageLoader;
import com.mars_sim.ui.swing.MainDesktopPane;
import com.mars_sim.ui.swing.StyleManager;
import com.mars_sim.ui.swing.utils.AttributePanel;
import com.mars_sim.ui.swing.utils.PercentageCellRenderer;

/**
 * The MaintenanceTabPanel is a tab panel for maintenance information.
 */
@SuppressWarnings("serial")
public class MaintenanceTabPanel extends TabPanel {
    private static final String SPANNER_ICON = "maintenance";
	private static final String REPAIR_PARTS_NEEDED = "Parts Needed: ";

	protected String[] columnToolTips = {
		    "The Part name", 
		    "The System Function",
		    "The # of Parts",
		    "The Probability that Triggers Maintenance"};
	
	/** The malfunction manager instance. */
	private MalfunctionManager manager;
	
	private JProgressBar wearCondition;
	private JProgressBar currentMaintenance;
	
	private JLabel lastCompletedLabel;
	private JLabel partsLabel;
	private JLabel malPLabel;
	private JLabel maintPLabel;

	/** The parts table model. */
	private PartTableModel tableModel;

	private static PartConfig partConfig = SimulationConfig.instance().getPartConfiguration();

	/**
	 * Constructor.
	 * 
	 * @param malfunctionable the malfunctionable instance of the unit
	 * @param desktop         The main desktop
	 */
	public MaintenanceTabPanel(Malfunctionable malfunctionable, MainDesktopPane desktop) {
		super(
			Msg.getString("MaintenanceTabPanel.title"), 
			ImageLoader.getIconByName(SPANNER_ICON), 
			Msg.getString("MaintenanceTabPanel.tooltip"),             
			desktop
		);

		// Initialize data members.
		manager = malfunctionable.getMalfunctionManager();

        tableModel = new PartTableModel(manager);
	}
	
	/**
	 * Builds the UI.
	 */
	@Override
	protected void buildUI(JPanel center) {
	
		JPanel topPanel = new JPanel(new BorderLayout());
		center.add(topPanel, BorderLayout.NORTH);
		
		AttributePanel labelPanel = new AttributePanel(4, 1);
		topPanel.add(labelPanel, BorderLayout.NORTH);
		
		Dimension barSize = new Dimension(100, 15);

		wearCondition = new JProgressBar();
		wearCondition.setStringPainted(true);
		wearCondition.setToolTipText(Msg.getString("MaintenanceTabPanel.wear.toolTip"));
		wearCondition.setMaximumSize(barSize);
		labelPanel.addLabelledItem(Msg.getString("MaintenanceTabPanel.wearCondition"), wearCondition);

		lastCompletedLabel = labelPanel.addTextField(Msg.getString("MaintenanceTabPanel.lastCompleted"), "", 
												null);
		currentMaintenance = new JProgressBar();
		currentMaintenance.setStringPainted(true);		
		currentMaintenance.setMaximumSize(barSize);
		currentMaintenance.setToolTipText(Msg.getString("MaintenanceTabPanel.current.toolTip"));
		labelPanel.addLabelledItem(Msg.getString("MaintenanceTabPanel.currentMaintenance"), currentMaintenance);

		
		partsLabel = labelPanel.addTextField(Msg.getString("MaintenanceTabPanel.partsNeeded"), "", null);
		
		topPanel.add(new JPanel(), BorderLayout.CENTER);
		
		AttributePanel dataPanel = new AttributePanel(2, 1);
		topPanel.add(dataPanel, BorderLayout.SOUTH);
	
		malPLabel = dataPanel.addTextField(Msg.getString("MaintenanceTabPanel.malfunctionProbaility"), "", null);	
		maintPLabel = dataPanel.addTextField(Msg.getString("MaintenanceTabPanel.maintenanceProbaility"), "", null);
		
		
		// Create the parts panel
		JScrollPane partsPane = new JScrollPane();
		partsPane.setPreferredSize(new Dimension(160, 80));
		center.add(partsPane, BorderLayout.CENTER);
		addBorder(partsPane, Msg.getString("MaintenanceTabPanel.tableBorder"));

		// Create the parts table
		JTable table = new JTable(tableModel){
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
		partsPane.setViewportView(table);

		TableColumnModel columnModel = table.getColumnModel();
        columnModel.getColumn(0).setPreferredWidth(140);
        columnModel.getColumn(1).setPreferredWidth(140);
		columnModel.getColumn(2).setPreferredWidth(25);
		columnModel.getColumn(3).setPreferredWidth(30);	
		
		// Add percentage format
		columnModel.getColumn(3).setCellRenderer(new PercentageCellRenderer(false));

		// Add sorting
		table.setAutoCreateRowSorter(true);

        // Set up values
        update();
	}

	/**
	 * Updates this panel.
	 */
	@Override
	public void update() {

		// Update the wear condition label.
		wearCondition.setValue((int) manager.getWearCondition());

		// Update last completed label.
		StringBuilder text = new StringBuilder();
		text.append(StyleManager.DECIMAL_SOLS.format(manager.getTimeSinceLastMaintenance()/1000D));
		text.append(" (Per Period: ");
		text.append(StyleManager.DECIMAL_SOLS.format(manager.getMaintenancePeriod()/1000D));
		text.append(")");

		lastCompletedLabel.setText(text.toString());

		// Update progress bar.
		double completed = manager.getMaintenanceWorkTimeCompleted();
		double total = manager.getMaintenanceWorkTime();
		currentMaintenance.setValue((int)(100.0 * completed / total));

		// TODO: compare what parts are missing and what parts are 
		// available for swapping out (just need time)
		Map<Integer, Integer> parts = manager.getMaintenanceParts();
		int size = 0; 
		
		if (parts != null)
			size = parts.size();
		else
			parts = new HashMap<>();
		
		// Update parts label.
		partsLabel.setText(Integer.toString(size));
		// Generate tool tip.
		String tooltip = "<html>" + getPartsString(parts, true) + "</html>";
		// Update tool tip.
		partsLabel.setToolTipText(tooltip);
		
		malPLabel.setText(Math.round(manager.getMalfunctionProbabilityPerOrbit() * 1000.0)/1000.0 + " % Per Orbit");
		
		maintPLabel.setText(Math.round(manager.getMaintenanceProbabilityPerOrbit() * 1000.0)/1000.0 + " % Per Orbit");
	}

	/**
	 * Gets the parts string.
	 * 
	 * @return string.
	 */
	private String getPartsString(Map<Integer, Integer> parts, boolean useHtml) {
		return MalfunctionTabPanel.getPartsString(REPAIR_PARTS_NEEDED, parts, useHtml).toString();
	}

	/**
	 * Internal class used as model for the equipment table.
	 */
	private static class PartTableModel extends AbstractTableModel {
		
		private List<Part> parts = new ArrayList<>();
		private List<String> functions = new ArrayList<>();
		private List<Integer> max = new ArrayList<>();
		private List<Double> probability = new ArrayList<>();

		/**
		 * hidden constructor.
		 */
		private PartTableModel(MalfunctionManager mm) {
            // Find parts for each scope
            for (MaintenanceScope maintenance : partConfig.getMaintenance(mm.getScopes())) {

                parts.add(maintenance.getPart());
                functions.add(Conversion.capitalize(maintenance.getName()));
                max.add(maintenance.getMaxNumber());
                probability.add(maintenance.getProbability());
            }	
		}

		public int getRowCount() {
			return parts.size();
		}

		public int getColumnCount() {
			return 4;
		}

		public Class<?> getColumnClass(int columnIndex) {
			switch (columnIndex) {
            case 0:
				return String.class;
			case 1:
				return String.class;
			case 2:
				return Integer.class;
			case 3:
				return Double.class;
			default:
                return String.class;
            }
		}

		public String getColumnName(int columnIndex) {
            switch(columnIndex) {
			case 0:
				return Msg.getString("MaintenanceTabPanel.header.part"); //$NON-NLS-1$
			case 1:
				return Msg.getString("MaintenanceTabPanel.header.function"); //$NON-NLS-1$
			case 2:
				return Msg.getString("MaintenanceTabPanel.header.max"); //$NON-NLS-1$
			case 3:
				return Msg.getString("MaintenanceTabPanel.header.probability"); //$NON-NLS-1$
			default:
				return "unknown";
            }
		}

		public Object getValueAt(int row, int column) {
			if (row >= 0 && row < parts.size()) {
                switch(column) {
				case 0:
					return parts.get(row).getName();
				case 1:
					return functions.get(row);
				case 2:
					return max.get(row);
				case 3:
					return probability.get(row);
                }
			}
			return "unknown";
		}
	}
}
