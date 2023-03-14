/*
 * Mars Simulation Project
 * ResupplyDetailPanel.java
 * @date 2022-07-19
 * @author Scott Davis
 */
package org.mars_sim.msp.ui.swing.tool.resupply;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;

import org.mars_sim.msp.core.Simulation;
import org.mars_sim.msp.core.events.HistoricalEvent;
import org.mars_sim.msp.core.events.HistoricalEventCategory;
import org.mars_sim.msp.core.events.HistoricalEventListener;
import org.mars_sim.msp.core.events.SimpleEvent;
import org.mars_sim.msp.core.interplanetary.transport.resupply.Resupply;
import org.mars_sim.msp.core.person.EventType;
import org.mars_sim.msp.core.resource.AmountResource;
import org.mars_sim.msp.core.resource.Part;
import org.mars_sim.msp.core.structure.BuildingTemplate;
import org.mars_sim.msp.core.time.ClockPulse;
import org.mars_sim.msp.core.time.MarsClock;
import org.mars_sim.msp.ui.swing.MainDesktopPane;
import org.mars_sim.msp.ui.swing.NumberCellRenderer;
import org.mars_sim.msp.ui.swing.StyleManager;
import org.mars_sim.msp.ui.swing.utils.AttributePanel;


/**
 * A panel showing a selected resupply mission details.
 */
@SuppressWarnings("serial")
public class ResupplyDetailPanel
extends JPanel
implements HistoricalEventListener {

	// Data members
	private int solsToArrival = -1;
	
	private JLabel templateLabel;
	private JLabel destinationValueLabel;
	private JLabel stateValueLabel;
	private JLabel arrivalDateValueLabel;
	private JLabel launchDateValueLabel;
	private JLabel timeArrivalValueLabel;
	private JLabel immigrantsValueLabel;
	
	private JTabbedPane innerSupplyPane;

	private MainDesktopPane desktop;

	private Resupply resupply;

	
	/**
	 * Constructor.
	 */
	public ResupplyDetailPanel(MainDesktopPane desktop) {

		// Use JPanel constructor
		super();

		this.desktop = desktop;
		
		Simulation sim = desktop.getSimulation();
	
		// Initialize data members.
		resupply = null;

		setLayout(new BorderLayout(0, 10));

		// Create the title label.
		JLabel titleLabel = new JLabel("Resupply Mission", SwingConstants.CENTER);
		StyleManager.applyHeading(titleLabel);
		titleLabel.setPreferredSize(new Dimension(-1, 25));
		add(titleLabel, BorderLayout.NORTH);

		JPanel infoPane = new JPanel(new BorderLayout());
		infoPane.setBorder(BorderFactory.createEtchedBorder());
		add(infoPane, BorderLayout.CENTER);

		// Create the details panel.
		AttributePanel detailsPane = new AttributePanel(7);
		infoPane.add(detailsPane, BorderLayout.NORTH);

		templateLabel = detailsPane.addTextField("Template", "", null);
		destinationValueLabel = detailsPane.addTextField("Destination", "", null);
		stateValueLabel = detailsPane.addTextField("State", "", null);
		launchDateValueLabel = detailsPane.addTextField("Launch Date", "", null);
		arrivalDateValueLabel = detailsPane.addTextField("Arrival Date", "", null);
		timeArrivalValueLabel = detailsPane.addTextField("Time Until Arrival", "", null);
		immigrantsValueLabel = detailsPane.addTextField("Immigrants", "", null);

		innerSupplyPane = new JTabbedPane();
		infoPane.add(innerSupplyPane, BorderLayout.CENTER);

		add(infoPane, BorderLayout.CENTER);

		// Set as historical event listener.
		sim.getEventManager().addListener(this);
	}

	/**
	 * Set the resupply mission to show.
	 * If resupply is null, clear displayed info.
	 * 
	 * @param resupply the resupply mission.
	 */
	public void setResupply(Resupply resupply) {
		if (this.resupply != resupply) {
			this.resupply = resupply;
			if (resupply == null) {
				clearInfo();
			}
			else {
				updateResupplyInfo();
			}
		}
	}

	/**
	 * Clear the resupply info.
	 */
	private void clearInfo() {
		templateLabel.setText("");
		destinationValueLabel.setText("");
		stateValueLabel.setText("");
		launchDateValueLabel.setText("");
		arrivalDateValueLabel.setText("");
		timeArrivalValueLabel.setText("");
		immigrantsValueLabel.setText("");
		innerSupplyPane.removeAll();
	}

	/**
	 * Updates the resupply info with the current resupply mission.
	 */
	private void updateResupplyInfo() {

		templateLabel.setText(resupply.getTemplate().getName());
		destinationValueLabel.setText(resupply.getSettlement().getName());
		stateValueLabel.setText(resupply.getTransitState().getName());
		launchDateValueLabel.setText(resupply.getLaunchDate().getDateString());
		arrivalDateValueLabel.setText(resupply.getArrivalDate().getDateTimeStamp());
		immigrantsValueLabel.setText(Integer.toString(resupply.getNewImmigrantNum()));
		
		updateTimeToArrival(desktop.getSimulation().getMasterClock().getMarsClock());
		updateSupplyPanel();

		validate();
	}

	/**
	 * Updates the supply panel with the current resupply mission.
	 */
	private void updateSupplyPanel() {

		// Clear any previous data.
		innerSupplyPane.removeAll();

		// Create buildings panel.
		JPanel buildingsPanel = createBuildingsDisplayPanel();
		if (buildingsPanel != null) {
			innerSupplyPane.addTab("Building", buildingsPanel);
		}

		// Create vehicles panel.
		JPanel vehiclesPanel = createVehiclesDisplayPanel();
		if (vehiclesPanel != null) {
			innerSupplyPane.addTab("Vehicles", vehiclesPanel);
		}

		// Create equipment panel.
		JPanel equipmentPanel = createEquipmentDisplayPanel();
		if (equipmentPanel != null) {
			innerSupplyPane.addTab("Equipment", equipmentPanel);
		}

		// Create resources panel.
		JPanel resourcesPanel = createResourcesDisplayPanel();
		if (resourcesPanel != null) {
			innerSupplyPane.addTab("Resources", resourcesPanel);
		}

		// Create parts panel.
		JPanel partsPanel = createPartsDisplayPanel();
		if (partsPanel != null) {
			innerSupplyPane.addTab("Parts", partsPanel);
		}
	}

	/**
	 * Creates the building display panel.
	 * 
	 * @return panel.
	 */
	private JPanel createBuildingsDisplayPanel() {

		JPanel buildingsPanel = null;

		List<BuildingTemplate> buildings = resupply.getNewBuildings();
		if (buildings.size() > 0) {
			// Create buildings panel.
			buildingsPanel = new JPanel(new BorderLayout());

			// Create table data.
			Map<String, Integer> buildingMap = new HashMap<>(buildings.size());
			Iterator<BuildingTemplate> i = buildings.iterator();
			while (i.hasNext()) {
				BuildingTemplate buildingTemplate = i.next();
				if (buildingMap.containsKey(buildingTemplate.getBuildingType())) {
					int num = buildingMap.get(buildingTemplate.getBuildingType()) + 1;
					buildingMap.put(buildingTemplate.getBuildingType(), num);
				}
				else {
					buildingMap.put(buildingTemplate.getBuildingType(), 1);
				}
			}

			// Create table model.
			DefaultTableModel tableModel = new DefaultTableModel() {
			    @Override
			    public boolean isCellEditable(int row, int column) {
			       //all cells false
			       return false;
			    }
			};
			tableModel.addColumn("Building Type");
			tableModel.addColumn("Quantity");

			// Populate table model with data.
			List<String> buildingTypes = new ArrayList<>(buildingMap.keySet());
			Collections.sort(buildingTypes);
			Iterator<String> j = buildingTypes.iterator();
			while (j.hasNext()) {
				String buildingName = j.next();
				int num = buildingMap.get(buildingName);
				Vector<Comparable<?>> rowData = new Vector<Comparable<?>>(2);
				rowData.add(buildingName);
				rowData.add(num);
				tableModel.addRow(rowData);
			}

			// Create table
			JTable buildingTable = new JTable(tableModel);
			buildingTable.setAutoCreateRowSorter(true);
			buildingTable.setCellSelectionEnabled(false);
			buildingTable.getColumnModel().getColumn(1).setMaxWidth(100);
			buildingTable.getColumnModel().getColumn(1).setCellRenderer(new NumberCellRenderer(0));
			buildingsPanel.add(new JScrollPane(buildingTable), BorderLayout.CENTER);

			// Set preferred height for panel to show all of table.
			int panelHeight = buildingTable.getPreferredSize().height +
					buildingTable.getTableHeader().getPreferredSize().height + 7;
			buildingsPanel.setPreferredSize(new Dimension(100, panelHeight));
		}

		return buildingsPanel;
	}

	/**
	 * Creates the vehicle display panel.
	 * 
	 * @return panel.
	 */
	private JPanel createVehiclesDisplayPanel() {

		JPanel vehiclesPanel = null;

		List<String> vehicles = resupply.getNewVehicles();
		if (vehicles.size() > 0) {
			// Create vehicles panel.
			vehiclesPanel = new JPanel(new BorderLayout());

			// Create table data.
			Map<String, Integer> vehicleMap = new HashMap<>(vehicles.size());
			Iterator<String> i = vehicles.iterator();
			while (i.hasNext()) {
				String vehicle = i.next();
				if (vehicleMap.containsKey(vehicle)) {
					int num = vehicleMap.get(vehicle) + 1;
					vehicleMap.put(vehicle, num);
				}
				else {
					vehicleMap.put(vehicle, 1);
				}
			}

			// Create table model.
			DefaultTableModel tableModel = new DefaultTableModel(){
			    @Override
			    public boolean isCellEditable(int row, int column) {
			       //all cells false
			       return false;
			    }
			};
			tableModel.addColumn("Vehicle Type");
			tableModel.addColumn("Quantity");

			// Populate table model with data.
			List<String> vehicleTypes = new ArrayList<>(vehicleMap.keySet());
			Collections.sort(vehicleTypes);
			Iterator<String> j = vehicleTypes.iterator();
			while (j.hasNext()) {
				String vehicleName = j.next();
				int num = vehicleMap.get(vehicleName);
				Vector<Comparable<?>> rowData = new Vector<Comparable<?>>(2);
				rowData.add(vehicleName);
				rowData.add(num);
				tableModel.addRow(rowData);
			}

			// Create table
			JTable vehicleTable = new JTable(tableModel);
			vehicleTable.setAutoCreateRowSorter(true);
			vehicleTable.setCellSelectionEnabled(false);
			vehicleTable.getColumnModel().getColumn(1).setMaxWidth(100);
			vehicleTable.getColumnModel().getColumn(1).setCellRenderer(new NumberCellRenderer(0));
			vehiclesPanel.add(new JScrollPane(vehicleTable), BorderLayout.CENTER);

			// Set preferred height for panel to show all of table.
			int panelHeight = vehicleTable.getPreferredSize().height +
					vehicleTable.getTableHeader().getPreferredSize().height + 7;
			vehiclesPanel.setPreferredSize(new Dimension(100, panelHeight));
		}

		return vehiclesPanel;
	}

	/**
	 * Creates the equipment display panel.
	 * 
	 * @return panel.
	 */
	private JPanel createEquipmentDisplayPanel() {

		JPanel equipmentPanel = null;

		Map<String, Integer> equipment = resupply.getNewEquipment();
		if (equipment.size() > 0) {
			// Create equipment panel.
			equipmentPanel = new JPanel(new BorderLayout());

			// Create table model.
			DefaultTableModel tableModel = new DefaultTableModel() {
			    @Override
			    public boolean isCellEditable(int row, int column) {
			       //all cells false
			       return false;
			    }
			};
			tableModel.addColumn("Equipment Type");
			tableModel.addColumn("Quantity");

			// Populate table model with data.
			List<String> equipmentTypes = new ArrayList<>(equipment.keySet());
			Collections.sort(equipmentTypes);
			Iterator<String> j = equipmentTypes.iterator();
			while (j.hasNext()) {
				String equipmentType = j.next();
				int num = equipment.get(equipmentType);
				Vector<Comparable<?>> rowData = new Vector<Comparable<?>>(2);
				rowData.add(equipmentType);
				rowData.add(num);
				tableModel.addRow(rowData);
			}

			// Create table
			JTable equipmentTable = new JTable(tableModel);
			equipmentTable.setAutoCreateRowSorter(true);
			equipmentTable.setCellSelectionEnabled(false);
			equipmentTable.getColumnModel().getColumn(1).setMaxWidth(100);
			equipmentTable.getColumnModel().getColumn(1).setCellRenderer(new NumberCellRenderer(0));
			equipmentPanel.add(new JScrollPane(equipmentTable), BorderLayout.CENTER);

			// Set preferred height for panel to show all of table.
			int panelHeight = equipmentTable.getPreferredSize().height +
					equipmentTable.getTableHeader().getPreferredSize().height + 7;
			equipmentPanel.setPreferredSize(new Dimension(100, panelHeight));
		}

		return equipmentPanel;
	}

	/**
	 * Creates the resources display panel.
	 * 
	 * @return panel.
	 */
	private JPanel createResourcesDisplayPanel() {

		JPanel resourcesPanel = null;

		Map<AmountResource, Double> resources = resupply.getNewResources();
		if (resources.size() > 0) {
			// Create resources panel.
			resourcesPanel = new JPanel(new BorderLayout());

			// Create table model.
			DefaultTableModel tableModel = new DefaultTableModel(){
			    @Override
			    public boolean isCellEditable(int row, int column) {
			       //all cells false
			       return false;
			    }
			};
			tableModel.addColumn("Resource Type");
			tableModel.addColumn("Amount [kg]");

			// Populate table model with data.
			List<AmountResource> resourceTypes = new ArrayList<>(resources.keySet());
			Collections.sort(resourceTypes);
			Iterator<AmountResource> j = resourceTypes.iterator();
			while (j.hasNext()) {
				AmountResource resourceType = j.next();
				double amount = resources.get(resourceType);
				String resourceName = resourceType.getName();
				Vector<Comparable<?>> rowData = new Vector<Comparable<?>>(2);
				//rowData.add(resourceType);
				rowData.add(resourceName);
				rowData.add(amount);
				tableModel.addRow(rowData);
			}

			// Create table
			JTable resourcesTable = new JTable(tableModel);
			resourcesTable.setAutoCreateRowSorter(true);
			resourcesTable.setCellSelectionEnabled(false);
			resourcesTable.getColumnModel().getColumn(1).setMaxWidth(120);
			resourcesTable.getColumnModel().getColumn(1).setCellRenderer(new NumberCellRenderer(1));
			resourcesPanel.add(new JScrollPane(resourcesTable), BorderLayout.CENTER);

			// Set preferred height for panel to show all of table.
			int panelHeight = resourcesTable.getPreferredSize().height +
					resourcesTable.getTableHeader().getPreferredSize().height + 7;
			resourcesPanel.setPreferredSize(new Dimension(100, panelHeight));
		}

		return resourcesPanel;
	}

	/**
	 * Creates the parts display panel.
	 * 
	 * @return panel.
	 */
	private JPanel createPartsDisplayPanel() {

		JPanel partsPanel = null;

		Map<Part, Integer> parts = resupply.getNewParts();
		if (parts.size() > 0) {
			// Create parts panel.
			partsPanel = new JPanel(new BorderLayout());

			// Create table model.
			DefaultTableModel tableModel = new DefaultTableModel() {
			    @Override
			    public boolean isCellEditable(int row, int column) {
			       //all cells false
			       return false;
			    }
			};
			tableModel.addColumn("Part Type");
			tableModel.addColumn("Quantity");

			// Populate table model with data.
			List<Part> partTypes = new ArrayList<>(parts.keySet());
			Collections.sort(partTypes);
			Iterator<Part> j = partTypes.iterator();
			while (j.hasNext()) {
				Part partType = j.next();
				int num = parts.get(partType);
				Vector<Comparable<?>> rowData = new Vector<Comparable<?>>(2);
				String partName = partType.getName();
				rowData.add(partName);
				//rowData.add(partType);
				rowData.add(num);
				tableModel.addRow(rowData);
			}

			// Create table
			JTable partsTable = new JTable(tableModel);
			partsTable.setAutoCreateRowSorter(true);
			partsTable.setCellSelectionEnabled(false);
			partsTable.getColumnModel().getColumn(1).setMaxWidth(100);
			partsTable.getColumnModel().getColumn(1).setCellRenderer(new NumberCellRenderer(0));
			partsPanel.add(new JScrollPane(partsTable), BorderLayout.CENTER);

			// Set preferred height for panel to show all of table.
			int panelHeight = partsTable.getPreferredSize().height +
					partsTable.getTableHeader().getPreferredSize().height + 7;
			partsPanel.setPreferredSize(new Dimension(100, panelHeight));
		}

		return partsPanel;
	}

	/**
	 * Updates the time to arrival label.
	 * @param currentTime 
	 */
	private void updateTimeToArrival(MarsClock currentTime) {
		String timeArrival = "---";
		solsToArrival = -1;
		double timeDiff = MarsClock.getTimeDiff(resupply.getArrivalDate(), currentTime);
		if (timeDiff > 0D) {
			solsToArrival = (int) Math.abs(timeDiff / 1000D);
			timeArrival = Integer.toString(solsToArrival) + " Sols";
		}
		timeArrivalValueLabel.setText(timeArrival);
	}

	@Override
	public void eventAdded(int index, SimpleEvent se, HistoricalEvent he) {
		if (HistoricalEventCategory.TRANSPORT == he.getCategory() &&
				EventType.TRANSPORT_ITEM_MODIFIED.equals(he.getType())) {
			if ((resupply != null) && he.getSource().equals(resupply)) {
				if (resupply != null) {
					updateResupplyInfo();
				}
			}
		}
	}

	@Override
	public void eventsRemoved(int startIndex, int endIndex) {
		// Do nothing.
	}

	private void updateArrival(MarsClock currentTime) {
		// Determine if change in time to arrival display value.
		if ((resupply != null) && (solsToArrival >= 0)) {
			double timeDiff = MarsClock.getTimeDiff(resupply.getArrivalDate(), currentTime);
			double newSolsToArrival = (int) Math.abs(timeDiff / 1000D);
			if (newSolsToArrival != solsToArrival) {
				if (resupply != null) {
					updateTimeToArrival(currentTime);
				}
			}
		}
	}

	void update(ClockPulse pulse) {
		updateArrival(pulse.getMarsTime());
	}
	

	/**
	 * Prepares the panel for deletion.
	 */
	public void destroy() {
		Simulation sim = desktop.getSimulation();
		sim.getEventManager().removeListener(this);
		
		resupply = null;
		templateLabel = null;
		destinationValueLabel = null;
		stateValueLabel = null;
		arrivalDateValueLabel = null;
		launchDateValueLabel = null;
		timeArrivalValueLabel = null;
		immigrantsValueLabel = null;
		innerSupplyPane = null;
		desktop = null;
	}
}
