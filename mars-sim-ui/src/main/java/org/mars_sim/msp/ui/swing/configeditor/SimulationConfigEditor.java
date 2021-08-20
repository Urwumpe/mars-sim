/**
 * Mars Simulation Project
 * SimulationConfigEditor.java
 * @version 3.3.0 2021-06-20
 * @author Scott Davis
 */
package org.mars_sim.msp.ui.swing.configeditor;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListDataListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumn;

import org.mars_sim.msp.core.Coordinates;
import org.mars_sim.msp.core.GameManager;
import org.mars_sim.msp.core.GameManager.GameMode;
import org.mars_sim.msp.core.Msg;
import org.mars_sim.msp.core.SimulationConfig;
import org.mars_sim.msp.core.configuration.Scenario;
import org.mars_sim.msp.core.configuration.ScenarioConfig;
import org.mars_sim.msp.core.configuration.UserConfigurableConfig;
import org.mars_sim.msp.core.logging.SimLogger;
import org.mars_sim.msp.core.person.Crew;
import org.mars_sim.msp.core.person.CrewConfig;
import org.mars_sim.msp.core.person.PersonConfig;
import org.mars_sim.msp.core.reportingAuthority.ReportingAuthorityFactory;
import org.mars_sim.msp.core.structure.InitialSettlement;
import org.mars_sim.msp.core.structure.SettlementConfig;
import org.mars_sim.msp.core.structure.SettlementTemplate;
import org.mars_sim.msp.core.tool.RandomUtil;
import org.mars_sim.msp.ui.swing.JComboBoxMW;
import org.mars_sim.msp.ui.swing.MainWindow;
import org.mars_sim.msp.ui.swing.tool.TableStyle;

import com.alee.laf.WebLookAndFeel;
import com.alee.laf.combobox.WebComboBox;
import com.alee.laf.label.WebLabel;
import com.alee.laf.window.WebFrame;
import com.alee.managers.UIManagers;
import com.alee.managers.style.StyleId;

/**
 * A temporary simulation configuration editor dialog. Will be replaced by
 * SimulationConfigEditor later when it is finished.
 */
public class SimulationConfigEditor {

	/**
	 * Adapter for the CreConfig to appear as a ComboModel
	 */
	private final class CrewComboModel implements ComboBoxModel<String> {

		private UserConfigurableConfig<Crew> crewConfig;
		private String selectedCrew = null;

		public CrewComboModel(UserConfigurableConfig<Crew> crewConfig) {
			this.crewConfig = crewConfig;
		}

		@Override
		public int getSize() {
			return crewConfig.getItemNames().size();
		}

		@Override
		public String getElementAt(int index) {
			return crewConfig.getItemNames().get(index);
		}

		@Override
		public void addListDataListener(ListDataListener l) {
		}

		@Override
		public void removeListDataListener(ListDataListener l) {
		}

		@Override
		public void setSelectedItem(Object anItem) {
			this.selectedCrew = (String) anItem;
		}

		@Override
		public Object getSelectedItem() {
			return this.selectedCrew;
		}
	}
	
	/**
	 * Inner class for the settlement table model.
	 */
	@SuppressWarnings("serial")
	private class SettlementTableModel extends AbstractTableModel {

		// Inner class representing a settlement configuration.
		private final class SettlementInfo {
			String name;
			String sponsor;
			String template;
			String population;
			String numOfRobots;
			String latitude;
			String longitude;
			String crew;
		}

		private String[] columns;
		private List<SettlementInfo> settlementInfoList;
		private String sponsorCache;
		
		/**
		 * Hidden Constructor.
		 */
		private SettlementTableModel() {
			super();

			// Add table columns.
			columns = new String[] { 
					Msg.getString("SimulationConfigEditor.column.name"), //$NON-NLS-1$
					Msg.getString("SimulationConfigEditor.column.sponsor"), //$NON-NLS-1$
					Msg.getString("SimulationConfigEditor.column.template"), //$NON-NLS-1$
					Msg.getString("SimulationConfigEditor.column.population"), //$NON-NLS-1$
					Msg.getString("SimulationConfigEditor.column.crew"),
					Msg.getString("SimulationConfigEditor.column.numOfRobots"), //$NON-NLS-1$
					Msg.getString("SimulationConfigEditor.column.latitude"), //$NON-NLS-1$
					Msg.getString("SimulationConfigEditor.column.longitude") //$NON-NLS-1$
			};
			
			this.settlementInfoList = new ArrayList<>();
		}

		
		/**
		 * Load the default settlements in the table.
		 */
		public void loadDefaultSettlements(Scenario selected) {
			settlementInfoList.clear();
			boolean hasSponsor = false;
			String sponsorCC = null;
			List<String> usedNames = new ArrayList<>();
			
			if (mode == GameMode.COMMAND) {
				sponsorCC = personConfig.getCommander().getSponsorStr();
				logger.config("The commander's sponsor is " + sponsorCC + ".");
			}
			
			for (InitialSettlement spec : selected.getSettlements()) {
				SettlementInfo info = toSettlementInfo(spec);

				// Save this name to the list
				usedNames.add(info.name);
							
				// Modify the sponsor in case of the Commander Mode
				if (mode == GameMode.COMMAND) {
					if (sponsorCC == info.sponsor) {
						hasSponsor = true;
					}
				}
					
				settlementInfoList.add(info);
			}
			
			if (mode == GameMode.COMMAND) {
				
				if (!hasSponsor) {
					// Change the 1st settlement's sponsor to match that of the commander
					settlementInfoList.get(0).sponsor = sponsorCC;
					
					// Gets a list of settlement names that are tailored to this country
					List<String> candidateNames = settlementConfig.getSettlementNameList(sponsorCC);
					Collections.shuffle(candidateNames);
					for (String c: candidateNames) {
						for (String u: usedNames) {
							if (!c.equalsIgnoreCase(u)) {
								// Change the 1st settlement's name to this country's preferred name
								settlementInfoList.get(0).name = c;
							}			
						}
						break;
					}
					
					logger.config( 
							"The 1st settlement's sponsor has just been changed to match the commander's sponsor.");
				}
				
				else {
					logger.config( 
							"The commander's sponsor will sponsor one of the settlements in the site editor.");
				}
			}
				
			fireTableDataChanged();
		}

		private SettlementInfo toSettlementInfo(InitialSettlement spec) {
			SettlementInfo info = new SettlementInfo();
			info.name = spec.getName();
			info.sponsor = spec.getSponsor();
			info.crew = spec.getCrew();
			info.template = spec.getSettlementTemplate();
			info.population = Integer.toString(spec.getPopulationNumber());
			info.numOfRobots = Integer.toString(spec.getNumOfRobots());
			Coordinates location = spec.getLocation();
			if (location != null) {
				info.latitude = location.getFormattedLatitudeString();
				info.longitude = location.getFormattedLongitudeString();
			}	
			return info;
		}


		/**
		 * Get the rows as InitialSettlements.
		 * @return
		 */
		public List<InitialSettlement> getSettlements() {

			List<InitialSettlement> is = new ArrayList<>();

			// Add configuration settlements from table data.
			for (SettlementInfo info : settlementInfoList) {
				int populationNum = Integer.parseInt(info.population);
				int numOfRobots = Integer.parseInt(info.numOfRobots);
				
				// take care to internationalize the coordinates
				String latitude = info.latitude.replace("N", Msg.getString("direction.northShort")); //$NON-NLS-1$ //$NON-NLS-2$
				latitude = latitude.replace("S", Msg.getString("direction.southShort")); //$NON-NLS-1$ //$NON-NLS-2$
				String longitude = info.longitude.replace("E", Msg.getString("direction.eastShort")); //$NON-NLS-1$ //$NON-NLS-2$
				longitude = longitude.replace("W", Msg.getString("direction.westShort")); //$NON-NLS-1$ //$NON-NLS-2$

				Coordinates location = new Coordinates(latitude, longitude);

				is.add(new InitialSettlement(info.name, info.sponsor, info.template,
													populationNum, numOfRobots, location, info.crew));

			}
			
			return is;
		}
		
		@Override
		public int getRowCount() {
			return settlementInfoList.size();
		}

		@Override
		public int getColumnCount() {
			return columns.length;
		}

		/*
		 * JTable uses this method to determine the default renderer/ editor for each
		 * cell. If we didn't implement this method, then the last column would contain
		 * text ("true"/"false"), rather than a check box.
		 */
		public Class<?> getColumnClass(int c) {
			return getValueAt(0, c).getClass();
		}

		@Override
		public String getColumnName(int columnIndex) {
			if ((columnIndex > -1) && (columnIndex < columns.length)) {
				return columns[columnIndex];
			} else {
				return Msg.getString("SimulationConfigEditor.log.invalidColumn"); //$NON-NLS-1$
			}
		}

		@Override
		public boolean isCellEditable(int row, int column) {
			return true;
		}

		@Override
		public Object getValueAt(int row, int column) {
			Object result = Msg.getString("unknown"); //$NON-NLS-1$

			if ((row > -1) && (row < getRowCount())) {
				SettlementInfo info = settlementInfoList.get(row);
				if ((column > -1) && (column < getColumnCount())) {
					switch (column) {
					case SETTLEMENT_COL:
						result = info.name;
						break;
					case SPONSOR_COL:
						result = info.sponsor;
						break;
					case PHASE_COL:
						result = info.template;
						break;
					case SETTLER_COL:
						result = info.population;
						break;
					case CREW_COL:
						result = info.crew;
						break;
					case BOT_COL:
						result = info.numOfRobots;
						break;
					case LAT_COL:
						result = info.latitude;
						break;
					case LON_COL:
						result = info.longitude;
						break;
					}
				} else {
					result = Msg.getString("SimulationConfigEditor.log.invalidColumn"); //$NON-NLS-1$
				}
			} else {
				result = Msg.getString("SimulationConfigEditor.log.invalidRow"); //$NON-NLS-1$
			}

			return result;
		}

		@Override
		public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
			if ((rowIndex > -1) && (rowIndex < getRowCount())) {
				SettlementInfo info = settlementInfoList.get(rowIndex);
				if ((columnIndex > -1) && (columnIndex < getColumnCount())) {
					switch (columnIndex) {
					
					case SETTLEMENT_COL:
						info.name = (String) aValue;
						break;
						
					case SPONSOR_COL:
						info.sponsor = (String) aValue;
						if (sponsorCache != info.sponsor) {
							sponsorCache = info.sponsor;
							String newName = tailorSettlementNameBySponsor(info.sponsor);
							if (newName != null) {
								info.name = newName;
							}
						}
						break;	
						
					case PHASE_COL:
						info.template = (String) aValue;
						info.population = Integer.toString(determineNewSettlementPopulation(info.template));
						info.numOfRobots = Integer.toString(determineNewSettlementNumOfRobots(info.template));
						break;
						
					case SETTLER_COL:
						info.population = (String) aValue;
						break;
						
					case CREW_COL:
						info.crew = (String) aValue;
						break;
						
					case BOT_COL:
						info.numOfRobots = (String) aValue;
						break;

					case LAT_COL:
						String latStr = ((String) aValue).trim();
						double doubleLat = 0;
						String dir1 = latStr.substring(latStr.length() - 1, latStr.length());
						dir1.toUpperCase();
						if (dir1.toUpperCase().equalsIgnoreCase("N") || dir1.toUpperCase().equalsIgnoreCase("S")) {
							if (latStr.length() > 2) {
								doubleLat = Double.parseDouble(latStr.substring(0, latStr.length() - 1));
								doubleLat = Math.round(doubleLat * 100.0) / 100.0;
								info.latitude = doubleLat + " " + dir1.toUpperCase();
							}
							else {
								info.latitude = (String) aValue;
							}
						}
						else {
							info.latitude = (String) aValue;
						}
						checkLat(info.latitude);
						checkRepeatingLatLon();
						break;

					case LON_COL:
						String longStr = ((String) aValue).trim();
						double doubleLong = 0;
						String dir2 = longStr.substring(longStr.length() - 1, longStr.length());
						dir2.toUpperCase();
						if (dir2.toUpperCase().equalsIgnoreCase("E") || dir2.toUpperCase().equalsIgnoreCase("W")) {
							if (longStr.length() > 2) {
								doubleLong = Double.parseDouble(longStr.substring(0, longStr.length() - 1));
								doubleLong = Math.round(doubleLong * 100.0) / 100.0;
								info.longitude = doubleLong + " " + dir2.toUpperCase();
							}
							else {
								info.longitude = (String) aValue;
							}
						}
						else {
							info.longitude = (String) aValue;
						}
						checkLon(info.longitude);
						checkRepeatingLatLon();
						break;
					}
				}

				if (columnIndex != SPONSOR_COL || columnIndex != PHASE_COL)
					checkForAllErrors();

				fireTableDataChanged();
			}
		}

		/**
		 * Remove a set of settlements from the table.
		 * 
		 * @param rowIndexes
		 *            an array of row indexes of the settlements to remove.
		 */
		private void removeSettlements(int[] rowIndexes) {
			List<SettlementInfo> removedSettlements = new ArrayList<SettlementInfo>(rowIndexes.length);

			for (int x = 0; x < rowIndexes.length; x++) {
				if ((rowIndexes[x] > -1) && (rowIndexes[x] < getRowCount())) {
					removedSettlements.add(settlementInfoList.get(rowIndexes[x]));
				}
			}

			Iterator<SettlementInfo> i = removedSettlements.iterator();
			while (i.hasNext()) {
				SettlementInfo s = i.next();
				settlementInfoList.remove(s);
			}

			fireTableDataChanged();
		}

		/**
		 * Adds a new settlement to the table.
		 * 
		 * @param settlement
		 *            the settlement configuration.
		 */
		private void addSettlement(InitialSettlement settlement) {
			settlementInfoList.add(toSettlementInfo(settlement));
			fireTableDataChanged();
		}

		/**
		 * Check for all errors in the table.
		 */
		private void checkForAllErrors() {
			clearError();

			Iterator<SettlementInfo> i = settlementInfoList.iterator();
			while (i.hasNext()) {
				SettlementInfo settlement = i.next();

				// Check that settlement name is valid.
				if ((settlement.name == null) || (settlement.name.isEmpty())) {
					setError(Msg.getString("SimulationConfigEditor.error.nameMissing")); //$NON-NLS-1$
				}

				// Check if population is valid.
				if ((settlement.population == null) || (settlement.population.isEmpty())) {
					setError(Msg.getString("SimulationConfigEditor.error.populationMissing")); //$NON-NLS-1$
				} else {
					try {
						int popInt = Integer.parseInt(settlement.population);
						if (popInt < 0) {
							setError(Msg.getString("SimulationConfigEditor.error.populationTooFew")); //$NON-NLS-1$
						}
					} catch (NumberFormatException e) {
						setError(Msg.getString("SimulationConfigEditor.error.populationInvalid")); //$NON-NLS-1$
					}
				}

				// Check if number of robots is valid.
				if ((settlement.numOfRobots == null) || (settlement.numOfRobots.isEmpty())) {
					setError(Msg.getString("SimulationConfigEditor.error.numOfRobotsMissing")); //$NON-NLS-1$
				} else {
					try {
						int num = Integer.parseInt(settlement.numOfRobots);
						if (num < 0) {
							setError(Msg.getString("SimulationConfigEditor.error.numOfRobotsTooFew")); //$NON-NLS-1$
						}
					} catch (NumberFormatException e) {
						setError(Msg.getString("SimulationConfigEditor.error.numOfRobotsInvalid")); //$NON-NLS-1$
					}
				}

				checkLatLon(settlement);
			}
		}

		/**
		 * Check for the validity of the input latitude and longitude
		 * 
		 * @param settlement
		 */
		private void checkLatLon(SettlementInfo settlement) {
			boolean hasError = true;
			String error0 = Coordinates.checkLat(settlement.latitude);
			if (error0 != null)
				setError(error0);
			else
				hasError = false;
			
			if (!hasError) {
				String error1 = Coordinates.checkLon(settlement.longitude);
				if (error1 != null)
					setError(error1);
				else
					hasError = false;
				
			if (!hasError)	
				clearError();
			}
			
//			checkLat(settlement);
//			checkLon(settlement);
		}
		
		/**
		 * Check for the validity of the input latitude and longitude
		 * 
		 * @param settlement
		 */
		private void checkLat(String latitude) {
			String error = Coordinates.checkLat(latitude);
			if (error != null)
				setError(error);
			else
				clearError();
		}
		
		/**
		 * Check for the validity of the input latitude and longitude
		 * 
		 * @param settlement
		 */
		private void checkLon(String longitude) {
			String error = Coordinates.checkLon(longitude);
			if (error != null)
				setError(error);
			else
				clearError();
		}

		/***
		 * Checks for any repeating latitude and longitude
		 */
		private void checkRepeatingLatLon() {
			// Ensure the latitude/longitude is not being taken already in the table by
			// another settlement
			boolean repeated = false;
			int size = settlementTableModel.getRowCount();
			
			Set<Coordinates> coordinatesSet = new HashSet<>();
			
			for (int x = 0; x < size; x++) {

				String latStr = ((String) (settlementTableModel.getValueAt(x, LAT_COL))).trim().toUpperCase();
				String longStr = ((String) (settlementTableModel.getValueAt(x, LON_COL))).trim().toUpperCase();				
				
				if (latStr == null || latStr.length() < 2) {
					setError(Msg.getString("Coodinates.error.latitudeMissing")); //$NON-NLS-1$
					return;
				}

				if (longStr == null || longStr.length() < 2) {
					setError(Msg.getString("Coodinates.error.longitudeMissing")); //$NON-NLS-1$
					return;
				}

				Coordinates c = new Coordinates(latStr, longStr);
				if (!coordinatesSet.add(c))
					repeated = true;

			}

			if (repeated) {
				setError(Msg.getString("Coodinates.error.latitudeLongitudeRepeating")); //$NON-NLS-1$
				return;
			}
		}
	}
	
	/** default logger. */
	private static final SimLogger logger = SimLogger.getLogger(SimulationConfigEditor.class.getName());

	private static final int HORIZONTAL_SIZE = 1024;
	
	private static final int SETTLEMENT_COL = 0;
	private static final int SPONSOR_COL = 1;
	private static final int PHASE_COL = 2;
	private static final int SETTLER_COL = 3;
	private static final int CREW_COL = 4;
	private static final int BOT_COL = 5;
	private static final int LAT_COL = 6;
	private static final int LON_COL = 7;

	private static final int NUM_COL = 8;
	
	// Data members.
	private boolean hasError, isCrewEditorOpen = true;

	private Font DIALOG_14 = new Font("Dialog", Font.PLAIN, 14);
	private Font DIALOG_16 = new Font("Dialog", Font.BOLD, 16);
	
	private SettlementTableModel settlementTableModel;
	private JTable settlementTable;
	private JLabel errorLabel;
	private JButton startButton;
	private WebFrame<?> f;

	private CrewEditor crewEditor;
	
	private GameMode mode;
	private  SettlementConfig settlementConfig;
	private  PersonConfig personConfig;
	
	private boolean completed = false;
	private boolean useCrew = true;
	private UserConfigurableConfig<Crew> crewConfig;

	private Scenario selectedScenario;
	private ScenarioConfig scenarioConfig;
	
	/**
	 * Constructor
	 * @param config the simulation configuration.
	 */
	public SimulationConfigEditor(SimulationConfig config) {

		// Initialize data members.
		settlementConfig = config.getSettlementConfiguration();
		personConfig = config.getPersonConfig();
		crewConfig = new CrewConfig();
		scenarioConfig = new ScenarioConfig();

		
		hasError = false;

		try {
			// use the weblaf skin
			WebLookAndFeel.install();
			UIManagers.initialize();
		} catch (Exception ex) {
			logger.log(Level.WARNING, Msg.getString("MainWindow.log.lookAndFeelError"), ex); //$NON-NLS-1$
		}

		// Setup weblaf's IconManager
		MainWindow.initIconManager();
				
		f = new WebFrame();
		
		f.setIconImage(MainWindow.getIconImage());
	
		f.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent event) {
				System.exit(0);
			}
		});
		
		f.setSize(HORIZONTAL_SIZE, 360);
		f.setTitle(Msg.getString("SimulationConfigEditor.title")); //$NON-NLS-1$
		
		// Sets the dialog content panel.
		JPanel contentPanel = new JPanel(new BorderLayout(10, 10));
		contentPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
		f.setContentPane(contentPanel);

		JPanel topPanel = null;
		
		if (GameManager.mode == GameMode.COMMAND) {
			mode = GameMode.COMMAND;
			topPanel = new JPanel(new GridLayout(2, 1));
			f.add(topPanel, BorderLayout.NORTH);
		}
		
		else {
			mode = GameMode.SANDBOX;
			topPanel = new JPanel(new GridLayout(1, 1));
			f.add(topPanel, BorderLayout.NORTH);
		}

		// Create the title label.
		if (mode == GameMode.COMMAND) {

			String commanderName = personConfig.getCommander().getFullName();
			String sponsor = personConfig.getCommander().getSponsorStr();
			WebLabel gameModeLabel = new WebLabel(Msg.getString("SimulationConfigEditor.gameMode", "Command Mode"), JLabel.CENTER); //$NON-NLS-1$
			gameModeLabel.setStyleId(StyleId.labelShadow);
			gameModeLabel.setFont(DIALOG_16);
			topPanel.add(gameModeLabel);
			
			JPanel ccPanel = new JPanel(new GridLayout(1, 3));
			topPanel.add(ccPanel);
			
			WebLabel commanderLabel = new WebLabel("   " + Msg.getString("SimulationConfigEditor.commanderName", 
					commanderName), JLabel.LEFT); //$NON-NLS-1$
			commanderLabel.setFont(DIALOG_14);
			commanderLabel.setStyleId(StyleId.labelShadow);
			ccPanel.add(commanderLabel);
			
			ccPanel.add(new JLabel());
			
			WebLabel sponsorLabel = new WebLabel(Msg.getString("SimulationConfigEditor.sponsorInfo", 
					sponsor)  + "                 ", JLabel.RIGHT); //$NON-NLS-1$
			sponsorLabel.setFont(DIALOG_14);
			sponsorLabel.setStyleId(StyleId.labelShadow);
			ccPanel.add(sponsorLabel);
			
		}
		
		else {
			WebLabel gameModeLabel = new WebLabel(Msg.getString("SimulationConfigEditor.gameMode", "Sandbox Mode"), JLabel.CENTER); //$NON-NLS-1$
			gameModeLabel.setFont(DIALOG_16);
			gameModeLabel.setStyleId(StyleId.labelShadow);
			topPanel.add(gameModeLabel);
		}
		
		// Create settlement scroll panel.
		JScrollPane settlementScrollPane = new JScrollPane();
		settlementScrollPane.setPreferredSize(new Dimension(HORIZONTAL_SIZE, 250));// 585, 200));
		f.add(settlementScrollPane, BorderLayout.CENTER);

		// Create settlement table.
		settlementTableModel = new SettlementTableModel();
		
		settlementTable = new JTable(settlementTableModel);
		settlementTable.setRowSelectionAllowed(true);
		settlementTable.getColumnModel().getColumn(SETTLEMENT_COL).setPreferredWidth(80);
		settlementTable.getColumnModel().getColumn(SPONSOR_COL).setPreferredWidth(80);
		settlementTable.getColumnModel().getColumn(PHASE_COL).setPreferredWidth(40);
		settlementTable.getColumnModel().getColumn(SETTLER_COL).setPreferredWidth(30);
		settlementTable.getColumnModel().getColumn(CREW_COL).setPreferredWidth(30);
		settlementTable.getColumnModel().getColumn(BOT_COL).setPreferredWidth(30);
		settlementTable.getColumnModel().getColumn(LAT_COL).setPreferredWidth(35);
		settlementTable.getColumnModel().getColumn(LON_COL).setPreferredWidth(35);
		settlementTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		settlementTable.setBackground(java.awt.Color.WHITE);

		TableStyle.setTableStyle(settlementTable);

		settlementScrollPane.setViewportView(settlementTable);

		// Create combo box for editing sponsor column in settlement table.
		TableColumn sponsorColumn = settlementTable.getColumnModel().getColumn(SPONSOR_COL);
		WebComboBox sponsorCB = new WebComboBox();
		for (String s : ReportingAuthorityFactory.getSupportedCodes()) {
			sponsorCB.addItem(s);
		}
		sponsorColumn.setCellEditor(new DefaultCellEditor(sponsorCB));
		
		// Create combo box for editing crew column in settlement table.
		// Use a custom model to inherit new Crews
		TableColumn crewColumn = settlementTable.getColumnModel().getColumn(CREW_COL);
		JComboBoxMW<String> crewCB = new JComboBoxMW<String>();
		crewCB.setModel(new CrewComboModel(crewConfig));
		crewColumn.setCellEditor(new DefaultCellEditor(crewCB));
		
		// Create combo box for editing template column in settlement table.
		TableColumn templateColumn = settlementTable.getColumnModel().getColumn(PHASE_COL);
		JComboBoxMW<String> templateCB = new JComboBoxMW<String>();
		for (SettlementTemplate st : settlementConfig.getSettlementTemplates()) {
			templateCB.addItem(st.getTemplateName());
		}
		templateColumn.setCellEditor(new DefaultCellEditor(templateCB));
		
		
		// Align content to center of cell
		DefaultTableCellRenderer defaultTableCellRenderer = new DefaultTableCellRenderer();
		defaultTableCellRenderer.setHorizontalAlignment(SwingConstants.LEFT);
		TableColumn column = null;
		for (int ii = 0; ii < NUM_COL; ii++) {
			column = settlementTable.getColumnModel().getColumn(ii);
			column.setCellRenderer(defaultTableCellRenderer);
		}

		// Create configuration button outer panel.
		JPanel configurationButtonOuterPanel = new JPanel(new BorderLayout(0, 0));
		f.add(configurationButtonOuterPanel, BorderLayout.EAST);

		// Create configuration button inner top panel.
		JPanel configurationButtonInnerTopPanel = new JPanel(new GridLayout(3, 1));
		configurationButtonOuterPanel.add(configurationButtonInnerTopPanel, BorderLayout.NORTH);

		// Create add settlement button.
		JButton addButton = new JButton(Msg.getString("SimulationConfigEditor.button.add")); //$NON-NLS-1$
//		TooltipManager.setTooltip(addButton, Msg.getString("SimulationConfigEditor.button.add"), TooltipWay.up);
		addButton.setToolTipText(Msg.getString("SimulationConfigEditor.tooltip.add")); //$NON-NLS-1$
		addButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				addDefaultNewSettlement();
			}
		});
		configurationButtonInnerTopPanel.add(addButton);

		// Create remove settlement button.
		JButton removeButton = new JButton(Msg.getString("SimulationConfigEditor.button.remove")); //$NON-NLS-1$
//		TooltipManager.setTooltip(removeButton, Msg.getString("SimulationConfigEditor.button.remove"), TooltipWay.up);
		removeButton.setToolTipText(Msg.getString("SimulationConfigEditor.tooltip.remove")); //$NON-NLS-1$
		removeButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				removeSelectedSettlements();
			}
		});
		configurationButtonInnerTopPanel.add(removeButton);

		// Create configuration button inner bottom panel.
		JPanel configurationButtonInnerBottomPanel = new JPanel(new GridLayout(1, 1));
		configurationButtonOuterPanel.add(configurationButtonInnerBottomPanel, BorderLayout.SOUTH);

		// Create default button.
		JButton defaultButton = new JButton(" " + Msg.getString("SimulationConfigEditor.button.undo") + " "); //$NON-NLS-1$
//		TooltipManager.setTooltip(defaultButton, Msg.getString("SimulationConfigEditor.button.undo"), TooltipWay.up);
		defaultButton.setToolTipText(Msg.getString("SimulationConfigEditor.tooltip.undo")); //$NON-NLS-1$
		defaultButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				settlementTableModel.loadDefaultSettlements(selectedScenario);
			}
		});
		configurationButtonInnerBottomPanel.add(defaultButton);

		// Create bottom panel.
		JPanel bottomPanel = new JPanel(new BorderLayout(0, 0));
		f.add(bottomPanel, BorderLayout.SOUTH);

		// Create error label.
		errorLabel = new JLabel("", JLabel.CENTER); //$NON-NLS-1$
		errorLabel.setForeground(Color.RED);
		bottomPanel.add(errorLabel, BorderLayout.NORTH);
		
		// Create the config control
		UserConfigurableControl<Scenario> control = new UserConfigurableControl<Scenario>(f, "Scenario", scenarioConfig) {

			@Override
			protected void displayItem(Scenario newDisplay) {
				settlementTableModel.loadDefaultSettlements(newDisplay);
			}

			@Override
			protected Scenario createItem(String newName) {
				return finalizeSettlementConfig(newName);
			}
		};
		bottomPanel.add(control.getPane(), BorderLayout.CENTER);

		
		// Create the bottom button panel.
		JPanel bottomButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
		bottomPanel.add(bottomButtonPanel, BorderLayout.SOUTH);

		if (mode == GameMode.COMMAND) {
			// Create the sponsor note label
			JLabel noteLabel = new JLabel("    " + Msg.getString("SimulationConfigEditor.sponsorNote"), JLabel.LEFT); //$NON-NLS-1$
			noteLabel.setFont(new Font("Serif", Font.ITALIC, 14));
			noteLabel.setForeground(java.awt.Color.BLUE);
			bottomPanel.add(noteLabel, BorderLayout.SOUTH);
		}
		
		// Create the start button.
		startButton = new JButton("  " + Msg.getString("SimulationConfigEditor.button.newSim") + "  "); //$NON-NLS-1$
//		TooltipManager.setTooltip(startButton, Msg.getString("SimulationConfigEditor.button.newSim"), TooltipWay.up);
		startButton.setToolTipText(Msg.getString("SimulationConfigEditor.tooltip.newSim")); //$NON-NLS-1$
		startButton.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent evt) {
				// Make sure any editing cell is completed, then check if error.
				TableCellEditor editor = settlementTable.getCellEditor();
				if (editor != null) {
					editor.stopCellEditing();
				}
				if (!hasError) {

					f.setVisible(false);
					
					// Recalculate the Scenario in case user has made unsaved changes 
					selectedScenario = finalizeSettlementConfig(control.getSelectItemName());
					
					// Close simulation config editor
					closeWindow();
				}
			}
		});

		bottomButtonPanel.add(startButton);
		//bottomButtonPanel.add(new JLabel("    "));
		 
		// Edit Alpha Crew button.
		JButton crewButton = new JButton("  " + Msg.getString("SimulationConfigEditor.button.crewEditor") + "  "); //$NON-NLS-1$
		crewButton.setToolTipText(Msg.getString("SimulationConfigEditor.tooltip.crewEditor")); //$NON-NLS-1$
		crewButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				editCrewProfile();
			}
		});

		// Set a check box for enabling/disable the alpha crew button
		JCheckBox cb = new JCheckBox("Load Crews");
		cb.setSelected(useCrew);
		cb.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
            	 useCrew = (e.getStateChange() == ItemEvent.SELECTED);
        		 crewButton.setEnabled(useCrew);
             }     
        });

		bottomButtonPanel.add(cb);
		bottomButtonPanel.add(crewButton);

		// Load it
		//settlementTableModel.loadDefaultSettlements(selectedScenario);
		
		// Set the location of the dialog at the center of the screen.
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		f.setLocation((screenSize.width - f.getWidth()) / 2, (screenSize.height - f.getHeight()) / 2);
		f.setVisible(true);

		f.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		f.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent ev) {
				if (isCrewEditorOpen) {
					crewEditor.getJFrame().dispose();
				}
				f.dispose();
			}
		});
		
		JRootPane rootPane = SwingUtilities.getRootPane(defaultButton); 
		rootPane.setDefaultButton(defaultButton);
	}
	

	/**
	 * Adds a new settlement with default values.
	 */
	private void addDefaultNewSettlement() {
		InitialSettlement settlement = determineNewDefaultSettlement();
		settlementTableModel.addSettlement(settlement);
	}

	/**
	 * Removes the settlements selected on the table.
	 */
	private void removeSelectedSettlements() {
		int[] rows = settlementTable.getSelectedRows();
		settlementTableModel.removeSettlements(rows);
	}

	/**
	 * Edits team profile.
	 * 
	 * @param crew
	 */
	private void editCrewProfile() {
		if (crewEditor == null || !isCrewEditorOpen) {
			crewEditor = new CrewEditor(this, crewConfig);
		} 
		
		else {
			crewEditor.getJFrame().setVisible(true);
		}
	}

	void setCrewEditorOpen(boolean value) {
		isCrewEditorOpen = value;
		crewEditor = null;
	}

	/**
	 * Finalizes the simulation configuration based on dialog choices.
	 */
	private Scenario finalizeSettlementConfig(String newName) {
		List<InitialSettlement> is = settlementTableModel.getSettlements();
		return new Scenario(newName, "", is, false);
	}

	/**
	 * Close and dispose dialog window.
	 */
	private void closeWindow() {
		f.dispose();
		wakeUpWaiters();
	}

	/**
	 * Method must be synchronized to register locks
	 */
	private synchronized void wakeUpWaiters() {
		// Wake up the waiters
		logger.config("Site Editor closed. Waking up.");
		completed = true;
		notifyAll();
	}
	
	/**
	 * Sets an edit check error.
	 * 
	 * @param errorString
	 *            the error description.
	 */
	private void setError(String errorString) {
		if (!hasError) {
			hasError = true;
			errorLabel.setText(errorString);
			startButton.setEnabled(false);
		}
	}

	/**
	 * Clears all edit-check errors.
	 */
	private void clearError() {
		hasError = false;
		errorLabel.setText(""); //$NON-NLS-1$
		startButton.setEnabled(true);
	}

	/**
	 * Configures a new default settlement.
	 * 
	 * @return settlement configuration.
	 */
	private InitialSettlement determineNewDefaultSettlement() {
			
		String sponsor = ReportingAuthorityFactory.MS_CODE;
		String template = determineNewSettlementTemplate();
		Coordinates location = determineNewSettlementLocation();
		
		return new InitialSettlement(determineNewSettlementName(sponsor),
				sponsor, template, 
				determineNewSettlementPopulation(template),
				determineNewSettlementNumOfRobots(template), location, null);
	}

	/**
	 * Determines a new settlement's name.
	 * 
	 * @return name.
	 */
	private String determineNewSettlementName(String sponsor) {
		String result = null;

		// TODO: should load a list of names custom-tailored to a sponsor, not just the default name list
		List<String> settlementNames = new ArrayList<>(settlementConfig.getSettlementNameList(sponsor));
		// Randomly shuffle settlement name list first.
		Collections.shuffle(settlementNames);
		
		Iterator<String> i = settlementNames.iterator();
		while (i.hasNext()) {
			String name = i.next();

			// Make sure settlement name isn't already being used in table.
			boolean nameUsed = false;
			for (int x = 0; x < settlementTableModel.getRowCount(); x++) {
				if (name.equals(settlementTableModel.getValueAt(x, SETTLEMENT_COL))) {
					// Label it as being used already in the table.
					nameUsed = true;
				}
			}

			// If not being used already, use this settlement name.
			if (!nameUsed) {
				result = name;
				break;
			}
		}

		// If no name found, create numbered settlement name: "Settlement 1",
		// "Settlement 2", etc.
		int count = 1;
		while (result == null) {
			String name = Msg.getString("SimulationConfigEditor.settlement", //$NON-NLS-1$
					Integer.toString(count));

			// Make sure settlement name isn't already being used in table.
			boolean nameUsed = false;
			for (int x = 0; x < settlementTableModel.getRowCount(); x++) {
				if (name.equals(settlementTableModel.getValueAt(x, SETTLEMENT_COL))) {
					nameUsed = true;
				}
			}

			// If not being used already, use this settlement name.
			if (!nameUsed) {
				result = name;
			}

			count++;
		}

		return result;
	}

	/**
	 * Determines a new settlement's template.
	 * 
	 * @return template name.
	 */
	private String determineNewSettlementTemplate() {
		String result = null;

		List<SettlementTemplate> templates = settlementConfig.getSettlementTemplates();
		if (templates.size() > 0) {
			int index = RandomUtil.getRandomInt(templates.size() - 1);
			result = templates.get(index).getTemplateName();
		} else
			logger.log(Level.WARNING, Msg.getString("SimulationConfigEditor.log.settlementTemplateNotFound")); //$NON-NLS-1$

		return result;
	}

	/**
	 * Determines the new settlement population.
	 * 
	 * @param templateName
	 *            the settlement template name.
	 * @return the new population number.
	 */
	private int determineNewSettlementPopulation(String templateName) {

		int result = 0; 

		if (templateName != null) {
			Iterator<SettlementTemplate> i = settlementConfig.getSettlementTemplates().iterator();
			while (i.hasNext()) {
				SettlementTemplate template = i.next();
				if (template.getTemplateName().equals(templateName)) {
					result = template.getDefaultPopulation();
				}
			}
		}

		return result;
	}

	/**
	 * Determines the new settlement number of robots.
	 * 
	 * @param templateName
	 *            the settlement template name.
	 * @return number of robots.
	 */
	private int determineNewSettlementNumOfRobots(String templateName) {

		int result = 0;

		if (templateName != null) {
			Iterator<SettlementTemplate> i = settlementConfig.getSettlementTemplates().iterator();
			while (i.hasNext()) {
				SettlementTemplate template = i.next();
				if (template.getTemplateName().equals(templateName)) {
					result = template.getDefaultNumOfRobots();

				}
			}
		}

		return result;
	}

	/**
	 * Returns a random settlement name tailored by the sponsor
	 * 
	 * @param sponsor
	 * @return
	 */
	private String tailorSettlementNameBySponsor(String sponsor) {
		
		List<String> usedNames = new ArrayList<>();
		
		// Add configuration settlements from table data.
		for (int x = 0; x < settlementTableModel.getRowCount(); x++) {
			String name = (String) settlementTableModel.getValueAt(x, SETTLEMENT_COL);
			usedNames.add(name);

		}

		// Gets a list of settlement names that are tailored to this country
		List<String> candidateNames = new ArrayList<String>(settlementConfig.getSettlementNameList(sponsor));
		candidateNames.removeAll(usedNames);

		if (candidateNames.isEmpty())
			return "[Type in a name]";
		else
			return candidateNames.get(RandomUtil.getRandomInt(candidateNames.size()-1));
	}
	
	/** 
	 * Get a new Location ofr a Settlement 
	 * @return
	 */
	private Coordinates determineNewSettlementLocation() {
		return new Coordinates(Coordinates.getRandomLatitude(), Coordinates.getRandomLongitude());
	}

	public WebFrame<?> getFrame() {
		return f;
	}

	/**
	 * Wait for the user to complete the configuration
	 */
	public synchronized void waitForCompletion() {
        while (!completed ) {
            try {
            	logger.config("Waiting for the Site Editor to complete...");
                wait();
            } catch (InterruptedException e)  {
                Thread.currentThread().interrupt(); 
            }
        }
        logger.config("Site Editor completed.");
	}
	
	/**
	 * Crew configuration if to be used. If returns null then no Crews
	 * @return
	 */
	public UserConfigurableConfig<Crew> getCrewConfig() {
		return crewConfig;
	}

	/**
	 * The Scenario created in the editor.
	 */
	public Scenario getScenario() {
		return selectedScenario;
	}
}
