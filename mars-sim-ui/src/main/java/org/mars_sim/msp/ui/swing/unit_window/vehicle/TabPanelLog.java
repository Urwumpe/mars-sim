/*
 * Mars Simulation Project
 * TabPanelLog.java
 * @date 2023-01-09
 * @author Scott Davis
 */
package org.mars_sim.msp.ui.swing.unit_window.vehicle;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Set;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

import org.mars.sim.tools.Msg;
import org.mars_sim.msp.core.data.History;
import org.mars_sim.msp.core.data.History.HistoryItem;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.time.MarsDate;
import org.mars_sim.msp.core.time.MarsTime;
import org.mars_sim.msp.core.tool.Conversion;
import org.mars_sim.msp.core.vehicle.StatusType;
import org.mars_sim.msp.core.vehicle.Vehicle;
import org.mars_sim.msp.ui.swing.ImageLoader;
import org.mars_sim.msp.ui.swing.MainDesktopPane;
import org.mars_sim.msp.ui.swing.StyleManager;
import org.mars_sim.msp.ui.swing.unit_window.TabPanel;
import org.mars_sim.msp.ui.swing.utils.AttributePanel;


@SuppressWarnings("serial")
public class TabPanelLog extends TabPanel {

	private static final String LOG_ICON = "log"; //$NON-NLS-1$

	private static final String SOL = "   Sol ";
		
	private JTable table;
	private JComboBox<MarsDate> solBox;
	private DefaultComboBoxModel<MarsDate> solModel;
	
	private JLabel odometerTF;
	private JLabel maintTF;
	
	private ScheduleTableModel scheduleTableModel;
	
	/** The Vehicle instance. */
	private Vehicle vehicle;

	private int lastSol = -1;
	
	public TabPanelLog(Vehicle vehicle, MainDesktopPane desktop) {
		// Use TabPanel constructor.
		super(
			Msg.getString("TabPanelLog.title"),
			ImageLoader.getIconByName(LOG_ICON),
			Msg.getString("TabPanelLog.title"), //$NON-NLS-1$
			desktop
		);
		
		this.vehicle = vehicle;

	}

	@Override
	protected void buildUI(JPanel content) {
		
        JPanel northPanel = new JPanel();
        northPanel.setLayout(new BoxLayout(northPanel, BoxLayout.Y_AXIS));
		
        // Create spring layout dataPanel
        AttributePanel springPanel = new AttributePanel(2);
        northPanel.add(springPanel);

		odometerTF = springPanel.addTextField( Msg.getString("TabPanelLog.label.odometer"),
								  	StyleManager.DECIMAL_KM.format(vehicle.getOdometerMileage()), null);

		maintTF = springPanel.addTextField(Msg.getString("TabPanelLog.label.maintDist"),
				 					StyleManager.DECIMAL_KM.format(vehicle.getDistanceLastMaintenance()), null);	
		
		solModel = new DefaultComboBoxModel<>();

		// Create comboBox
		solBox = new JComboBox<>(solModel);
		//solBox.setPreferredSize(new Dimension(80, 25));
		//solBox.setPrototypeDisplayValue(new Dimension(80, 25));
						
		JPanel solPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
		solPanel.add(solBox);
        northPanel.add(solPanel);
				
		Box box = Box.createHorizontalBox();
		northPanel.add(box);
		
        content.add(northPanel, BorderLayout.NORTH);

		box.add(Box.createHorizontalGlue());

		// Create schedule table model
		scheduleTableModel = new ScheduleTableModel();

		// Create attribute scroll panel
		JScrollPane scrollPanel = new JScrollPane();
		content.add(scrollPanel, BorderLayout.CENTER);

		// Create schedule table
		table = new JTable(scheduleTableModel);
		table.setPreferredScrollableViewportSize(new Dimension(225, 100));
		table.getColumnModel().getColumn(0).setPreferredWidth(30);
		table.getColumnModel().getColumn(1).setPreferredWidth(150);
		table.setRowSelectionAllowed(true);

		scrollPanel.setViewportView(table);


		solBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				setSolDisplayed((MarsDate) solBox.getSelectedItem());
			}
		});

		// Update will refresh data
		update();
	}

	protected void setSolDisplayed(MarsDate marsDate) {
		scheduleTableModel.update(vehicle.getVehicleLog().getChanges(), marsDate);
	}

	/**
	 * Gets the mars clock instance.
	 * 
	 * @return
	 */
	private MarsTime getMarsClock() {
		return getSimulation().getMasterClock().getMarsTime();
	}


	@Override
	public void update() {

		// Update the odometer reading
		odometerTF.setText(StyleManager.DECIMAL_PLACES2.format(vehicle.getOdometerMileage()));
				
		// Update distance last maintenance 
		maintTF.setText(StyleManager.DECIMAL_PLACES2.format(vehicle.getDistanceLastMaintenance()));
				
		int currentSol = getMarsClock().getMissionSol();
		History<Set<StatusType>> solStatus = vehicle.getVehicleLog();

		// Update the sol combobox at the beginning of a new sol
		if (lastSol != currentSol) {

			// Update the solList comboBox
			Object currentSelection = solBox.getSelectedItem();
			solModel.removeAllElements();
			solModel.addAll(solStatus.getRange());
			lastSol = currentSol;

			if (currentSelection == null) {
				currentSelection = getMarsClock().getDate();
			}
			solBox.setSelectedItem(currentSelection);
		}
		
		MarsDate selected = (MarsDate) solBox.getSelectedItem();
		scheduleTableModel.update(solStatus.getChanges(), selected);

	}
	
	
	/**
	 * Internal class used as model for the attribute table.
	 */
	private class ScheduleTableModel extends AbstractTableModel {

		private List<HistoryItem<Set<StatusType>>> oneDayStatuses;
		
		/**
		 * hidden constructor.
		 * 
		 * @param person {@link Person}
		 */
		ScheduleTableModel() {
		}

		@Override
		public int getRowCount() {
			if (oneDayStatuses != null)
				return oneDayStatuses.size();
			else
				return 0;
		}

		@Override
		public int getColumnCount() {
			return 2;
		}

		@Override
		public Class<?> getColumnClass(int columnIndex) {
			Class<?> dataType = super.getColumnClass(columnIndex);
			if (columnIndex == 0)
				dataType = String.class;
			if (columnIndex == 1)
				dataType = String.class;
			return dataType;
		}

		@Override
		public String getColumnName(int columnIndex) {
			if (columnIndex == 0)
				return Msg.getString("TabPanelLog.column.time"); //$NON-NLS-1$
			else if (columnIndex == 1)
				return Msg.getString("TabPanelLog.column.status"); //$NON-NLS-1$
			else
				return null;
		}

		@Override
		public Object getValueAt(int row, int column) {	
			if (row < oneDayStatuses.size()) {
				HistoryItem<Set<StatusType>> item = oneDayStatuses.get(row);
				if (column == 0) {
					return item.getWhen();
				} 
				else if (column == 1) {
					return printStatusTypes(item.getWhat());
				}
			}

			return column;
		}

		/**
		 * Prints a string list of status types.
		 * 
		 * @param statusTypes the set of status types
		 * @return
		 */
		public String printStatusTypes(Set<StatusType> statusTypes) {
			String s = Conversion.capitalize(statusTypes.toString());
			return s.substring(1 , s.length() - 1).toLowerCase();
		}
		
		/**
		 * Prepares a list of activities done on the selected day.
		 * 
		 * @param solStatus
		 * @param selected
		 */
		public void update(List<HistoryItem<Set<StatusType>>> solStatus, MarsDate selected) {
				
			// TODO Only update when something has changed
			oneDayStatuses = solStatus.stream().filter(i -> i.getWhen().getDate().equals(selected)).toList();

			fireTableDataChanged();
		}
	}
}
