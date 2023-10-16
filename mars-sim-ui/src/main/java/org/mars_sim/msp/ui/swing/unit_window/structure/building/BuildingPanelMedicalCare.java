/*
 * Mars Simulation Project
 * BuildingPanelMedicalCare.java
 * @date 2022-07-10
 * @author Scott Davis
 */
package org.mars_sim.msp.ui.swing.unit_window.structure.building;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

import org.mars_sim.msp.core.person.health.HealthProblem;
import org.mars_sim.msp.core.structure.building.function.MedicalCare;
import org.mars_sim.msp.ui.swing.ImageLoader;
import org.mars_sim.msp.ui.swing.MainDesktopPane;
import org.mars_sim.msp.ui.swing.utils.AttributePanel;
import org.mars_sim.tools.Msg;


/**
 * The MedicalCareBuildingPanel class is a building function panel representing
 * the medical info of a settlement building.
 */
@SuppressWarnings("serial")
public class BuildingPanelMedicalCare
extends BuildingFunctionPanel {

	private static final String MEDICAL_ICON = "medical";

	// Data members
	/** The medical care. */
	private MedicalCare medical;
	/** Label of number of physicians. */
	private JLabel physicianLabel;
	/** Table of medical info. */
	private MedicalTableModel medicalTableModel;

	// Data cache
	/** Cache of number of physicians. */
	private int physicianCache;

	/**
	 * Constructor.
	 * @param medical the medical care building this panel is for.
	 * @param desktop The main desktop.
	 */
	public BuildingPanelMedicalCare(MedicalCare medical, MainDesktopPane desktop) {

		// Use BuildingFunctionPanel constructor
		super(
			Msg.getString("BuildingPanelMedicalCare.title"), 
			ImageLoader.getIconByName(MEDICAL_ICON),
			medical.getBuilding(), 
			desktop
		);

		// Initialize data members
		this.medical = medical;
	}
	
	/**
	 * Build the UI
	 */
	@Override
	protected void buildUI(JPanel center) {

		// Create label panel
		AttributePanel labelPanel = new AttributePanel(2);
		center.add(labelPanel, BorderLayout.NORTH);
		
		// Create sick bed label
		labelPanel.addTextField(Msg.getString("BuildingPanelMedicalCare.numberOfsickBeds"),
					 				Integer.toString(medical.getSickBedNum()), null);

		// Create physician label
		physicianCache = medical.getPhysicianNum();
		physicianLabel = labelPanel.addTextField(Msg.getString("BuildingPanelMedicalCare.numberOfPhysicians"),
									  Integer.toString(physicianCache), null);

		// Create scroll panel for medical table
		JScrollPane scrollPanel = new JScrollPane();
		scrollPanel.setPreferredSize(new Dimension(160, 80));
		center.add(scrollPanel, BorderLayout.CENTER);
	    scrollPanel.getViewport().setOpaque(false);
	    scrollPanel.setOpaque(false);

		// Prepare medical table model
		medicalTableModel = new MedicalTableModel(medical);

		// Prepare medical table
		JTable medicalTable = new JTable(medicalTableModel);
		medicalTable.setCellSelectionEnabled(false);
		scrollPanel.setViewportView(medicalTable);
	}

	/**
	 * Update this panel
	 */
	@Override
	public void update() {

		// Update physician label
		if (physicianCache != medical.getPhysicianNum()) {
			physicianCache = medical.getPhysicianNum();
			physicianLabel.setText(Integer.toString(physicianCache));
		}

		// Update medical table model.
		medicalTableModel.update();
	}

	/**
	 * Internal class used as model for the medical table.
	 */
	private static class MedicalTableModel extends AbstractTableModel {

		/** default serial id. */
		private static final long serialVersionUID = 1L;

		private MedicalCare medical;
		private java.util.List<?> healthProblems;

		private MedicalTableModel(MedicalCare medical) {
			this.medical = medical;
			healthProblems = medical.getProblemsBeingTreated();
		}

		public int getRowCount() {
			return healthProblems.size();
		}

		public int getColumnCount() {
			return 2;
		}

		public Class<?> getColumnClass(int columnIndex) {
			Class<?> dataType = super.getColumnClass(columnIndex);
			if (columnIndex == 0) dataType = String.class;
			else if (columnIndex == 1) dataType = String.class;
			return dataType;
		}

		public String getColumnName(int columnIndex) {
			if (columnIndex == 0) return "Patient";
			else if (columnIndex == 1) return "Condition";
			else return "unknown";
		}

		public Object getValueAt(int row, int column) {

			HealthProblem problem = (HealthProblem) healthProblems.get(row);

			if (column == 0) return problem.getSufferer().getName();
			else if (column == 1) return problem.toString();
			else return "unknown";
		}

		public void update() {
			if (!healthProblems.equals(medical.getProblemsBeingTreated()))
				healthProblems = medical.getProblemsBeingTreated();

			fireTableDataChanged();
		}
	}
}
