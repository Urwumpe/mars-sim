/*
 * Mars Simulation Project
 * LocationTabPanel.java
 * @date 2021-12-20
 * @author Scott Davis
 */
package org.mars_sim.msp.ui.swing.unit_window;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import org.mars.sim.mapdata.location.Coordinates;
import org.mars.sim.tools.Msg;
import org.mars_sim.msp.core.Unit;
import org.mars_sim.msp.core.UnitType;
import org.mars_sim.msp.core.environment.TerrainElevation;
import org.mars_sim.msp.core.equipment.Equipment;
import org.mars_sim.msp.core.location.LocationStateType;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.robot.Robot;
import org.mars_sim.msp.core.structure.Settlement;
import org.mars_sim.msp.core.vehicle.Vehicle;
import org.mars_sim.msp.ui.swing.ImageLoader;
import org.mars_sim.msp.ui.swing.MainDesktopPane;
import org.mars_sim.msp.ui.swing.MarsPanelBorder;
import org.mars_sim.msp.ui.swing.tool.navigator.NavigatorWindow;
import org.mars_sim.msp.ui.swing.tool.settlement.SettlementWindow;
import org.mars_sim.msp.ui.swing.utils.AttributePanel;

import eu.hansolo.steelseries.gauges.DisplayCircular;
import eu.hansolo.steelseries.gauges.DisplaySingle;
import eu.hansolo.steelseries.tools.BackgroundColor;
import eu.hansolo.steelseries.tools.FrameDesign;
import eu.hansolo.steelseries.tools.LcdColor;

/**
 * The LocationTabPanel is a tab panel for location information.
 */
@SuppressWarnings("serial")
public class LocationTabPanel extends TabPanel implements ActionListener{

	/** default logger. */
	private static final Logger logger = Logger.getLogger(LocationTabPanel.class.getName());

	private static final String MAP_ICON = NavigatorWindow.ICON;

	private static final String N = "N";
	private static final String S = "S";
	private static final String E = "E";
	private static final String W = "W";

	private double elevationCache;

	private String locationStringCache;

	private Unit containerCache;
	private Unit topContainerCache;
	private Settlement settlementCache;
	private LocationStateType locationStateTypeCache;
	
	private JLabel topContainerLabel;
	private JLabel containerLabel;
	private JLabel settlementLabel;
	private JLabel locationStateLabel;
	
	private Coordinates locationCache;

	private JButton locatorButton;

	private DisplaySingle lcdLong;
	private DisplaySingle lcdLat;
	private DisplaySingle lcdText; 
	private DisplayCircular gauge;

	/**
	 * Constructor.
	 *
	 * @param unit    the unit to display.
	 * @param desktop the main desktop.
	 */
	public LocationTabPanel(Unit unit, MainDesktopPane desktop) {
		// Use the TabPanel constructor
		super(null, ImageLoader.getIconByName(MAP_ICON), Msg.getString("LocationTabPanel.title"), unit, desktop);

		locationStringCache = unit.getLocationTag().getExtendedLocation();
		containerCache = unit.getContainerUnit();
		topContainerCache = unit.getTopContainerUnit();
	}

	@Override
	protected void buildUI(JPanel content) {

		Unit unit = getUnit();
		Unit container = unit.getContainerUnit();
		if (containerCache != container) {
			containerCache = container;
		}
		String n1 = container != null ? container.getName() : "";
		
		Unit topContainer = unit.getTopContainerUnit();
		if (topContainerCache != topContainer) {
			topContainerCache = topContainer;
		}
		String n0 = topContainer != null ? topContainer.getName() : "";
		
		// Create location panel
		JPanel locationPanel = new JPanel(new BorderLayout(5, 5));
		locationPanel.setBorder(new MarsPanelBorder());
		locationPanel.setBorder(new EmptyBorder(1, 1, 1, 1));
		content.add(locationPanel, BorderLayout.NORTH);

		// Initialize location cache
		locationCache = unit.getCoordinates();

		String dir_N_S = null;
		String dir_E_W = null;
		if (locationCache.getLatitudeDouble() >= 0)
			dir_N_S = Msg.getString("direction.degreeSign") + "N";
		else
			dir_N_S = Msg.getString("direction.degreeSign") + "S";

		if (locationCache.getLongitudeDouble() >= 0)
			dir_E_W = Msg.getString("direction.degreeSign") + "E";
		else
			dir_E_W = Msg.getString("direction.degreeSign") + "W";

		JPanel northPanel = new JPanel(new FlowLayout());
		locationPanel.add(northPanel, BorderLayout.NORTH);

		lcdLat = new DisplaySingle();
		lcdLat.setLcdUnitString(dir_N_S);
		lcdLat.setLcdValueAnimated(Math.abs(locationCache.getLatitudeDouble()));
		lcdLat.setLcdInfoString("Latitude");
		// lcd1.setLcdColor(LcdColor.BLUELIGHTBLUE_LCD);
		lcdLat.setLcdColor(LcdColor.BEIGE_LCD);
		// lcdLat.setBackground(BackgroundColor.NOISY_PLASTIC);
		lcdLat.setGlowColor(Color.orange);
		// lcd1.setBorder(new EmptyBorder(5, 5, 5, 5));
		lcdLat.setDigitalFont(true);
		lcdLat.setLcdDecimals(4);
		lcdLat.setSize(new Dimension(150, 45));
		lcdLat.setMaximumSize(new Dimension(150, 45));
		lcdLat.setPreferredSize(new Dimension(150, 45));
		lcdLat.setVisible(true);

		northPanel.add(lcdLat);

		elevationCache = Math.round(TerrainElevation.getAverageElevation(unit.getCoordinates()) * 1000.0) / 1000.0;

		logger.info(unit.getName()
				+ "'s coordinates: " + unit.getCoordinates()
				+ "   Elevation: " + elevationCache + " km.");

//        lcdElev = new DisplaySingle();
//        lcdElev.setLcdValueFont(new Font("Serif", Font.ITALIC, 12));
//        lcdElev.setLcdUnitString("km");
//        lcdElev.setLcdValueAnimated(elevationCache);
//        lcdElev.setLcdDecimals(3);
//        lcdElev.setLcdInfoString("Elevation");
//        //lcd0.setLcdColor(LcdColor.DARKBLUE_LCD);
//        lcdElev.setLcdColor(LcdColor.YELLOW_LCD);//REDDARKRED_LCD);
//        lcdElev.setDigitalFont(true);
//        //lcd0.setBorder(new EmptyBorder(5, 5, 5, 5));
//        lcdElev.setSize(new Dimension(150, 60));
//        lcdElev.setMaximumSize(new Dimension(150, 60));
//        lcdElev.setPreferredSize(new Dimension(150, 60));
//        lcdElev.setVisible(true);
//        locationPanel.add(lcdElev, BorderLayout.NORTH);

		// Create center map button
		locatorButton = new JButton(ImageLoader.getIconByName(NavigatorWindow.ICON));

		locatorButton.setBorder(new EmptyBorder(1, 1, 1, 1));
		locatorButton.addActionListener(this);
		locatorButton.setOpaque(false);
		locatorButton.setToolTipText("Locate the unit on Mars Navigator");
		locatorButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));// new Cursor(Cursor.HAND_CURSOR));

		JPanel locatorPane = new JPanel(new FlowLayout());
		locatorPane.add(locatorButton);

		northPanel.add(locatorPane);

		JPanel lcdPanel = new JPanel();
		lcdLong = new DisplaySingle();
		// lcdLong.setCustomLcdForeground(getForeground());
		lcdLong.setLcdUnitString(dir_E_W);
		lcdLong.setLcdValueAnimated(Math.abs(locationCache.getLongitudeDouble()));
		lcdLong.setLcdInfoString("Longitude");
		lcdLong.setLcdColor(LcdColor.BEIGE_LCD);
//		lcdLong.setBackgroundColor(BackgroundColor.LINEN);
		lcdLong.setGlowColor(Color.yellow);
		lcdLong.setDigitalFont(true);
		lcdLong.setLcdDecimals(4);
		lcdLong.setSize(new Dimension(150, 40));
		lcdLong.setMaximumSize(new Dimension(150, 40));
		lcdLong.setPreferredSize(new Dimension(150, 40));
		lcdLong.setVisible(true);
		lcdPanel.add(lcdLong);
		northPanel.add(lcdPanel);

		JPanel gaugePanel = new JPanel();
		gauge = new DisplayCircular();
		gauge.setSize(new Dimension(100, 100));
		gauge.setMaximumSize(new Dimension(100, 100));
		gauge.setPreferredSize(new Dimension(100, 100));
		setGauge(gauge, elevationCache);
		gaugePanel.add(gauge);
		
		locationPanel.add(gaugePanel, BorderLayout.CENTER);

		lcdText = new DisplaySingle();
		lcdText.setLcdInfoString("Last Known Position");
		// lcdText.setLcdColor(LcdColor.REDDARKRED_LCD);
		lcdText.setGlowColor(Color.ORANGE);
		// lcdText.setBackground(Background.SATIN_GRAY);
		lcdText.setDigitalFont(true);
		lcdText.setSize(new Dimension(150, 30));
		lcdText.setMaximumSize(new Dimension(150, 30));
		lcdText.setPreferredSize(new Dimension(150, 30));
		lcdText.setVisible(true);
		lcdText.setLcdNumericValues(false);
		lcdText.setLcdValueFont(new Font("Serif", Font.ITALIC, 8));

		lcdText.setLcdText(locationStringCache);

		// Pause the location lcd text the sim is pause
        lcdText.setLcdTextScrolling(true);

		locationPanel.add(lcdText, BorderLayout.SOUTH);

		/////
		
		settlementCache = unit.getSettlement();
		String n2 = settlementCache != null ? settlementCache.getName() : "";
		LocationStateType locationStateTypeCache = unit.getLocationStateType();
		String n3 = locationStateTypeCache != null ? locationStateTypeCache.getName() : "";
		
		// Prepare info panel.
		AttributePanel containerPanel = new AttributePanel(4);
		content.add(containerPanel, BorderLayout.CENTER);
		topContainerLabel = containerPanel.addRow("Top Container Unit", n0);
		containerLabel = containerPanel.addRow("Container Unit", n1);
		settlementLabel = containerPanel.addRow("Settlement Container", n2);
		locationStateLabel = containerPanel.addRow("Location State", n3);
		
		updateLocationBanner(unit);

		checkTheme(true);
	}

	public void checkTheme(boolean firstRun) {
		lcdText.setLcdColor(LcdColor.DARKBLUE_LCD);
		gauge.setFrameDesign(FrameDesign.STEEL);
//		locatorButton.setIcon(ImageLoader.getIcon(FIND_ORANGE));
	}

	private void setGauge(DisplayCircular gauge, double elevationCache) {

		// Note: The peak of Olympus Mons is 21,229 meters (69,649 feet) above the Mars
		// areoid (a reference datum similar to Earth's sea level). The lowest point is
		// within the Hellas Impact Crater (marked by a flag with the letter "L").
		// The lowest point in the Hellas Impact Crater is 8,200 meters (26,902 feet)
		// below the Mars areoid.

		int max = -1;
		int min = 2;

		if (elevationCache < -8) {
			max = -8;
			min = -9;
		} else if (elevationCache < -5) {
			max = -5;
			min = -9;
		} else if (elevationCache < -3) {
			max = -3;
			min = -5;
		} else if (elevationCache < 0) {
			max = 1;
			min = -1;
		} else if (elevationCache < 1) {
			max = 2;
			min = 0;
		} else if (elevationCache < 3) {
			max = 5;
			min = 0;
		} else if (elevationCache < 10) {
			max = 10;
			min = 5;
		} else if (elevationCache < 20) {
			max = 20;
			min = 10;
		} else if (elevationCache < 30) {
			max = 30;
			min = 20;
		}

		gauge.setDisplayMulti(false);
		gauge.setDigitalFont(true);
		// gauge.setFrameDesign(FrameDesign.GOLD);
		// gauge.setOrientation(Orientation.EAST);//.NORTH);//.VERTICAL);
		// gauge.setPointerType(PointerType.TYPE5);
		// gauge.setTextureColor(Color.yellow);//, Texture_Color BRUSHED_METAL and
		// PUNCHED_SHEET);
		gauge.setUnitString("km");
		gauge.setTitle("Elevation");
		gauge.setMinValue(min);
		gauge.setMaxValue(max);
		// gauge.setTicklabelsVisible(true);
		// gauge.setMaxNoOfMajorTicks(10);
		// gauge.setMaxNoOfMinorTicks(10);
		gauge.setBackgroundColor(BackgroundColor.NOISY_PLASTIC);// .BRUSHED_METAL);
		// alt.setGlowColor(Color.yellow);
		// gauge.setLcdColor(LcdColor.BEIGE_LCD);//.BLACK_LCD);
		// gauge.setLcdInfoString("Elevation");
		// gauge.setLcdUnitString("km");
		gauge.setLcdValueAnimated(elevationCache);
		gauge.setValueAnimated(elevationCache);
		// gauge.setValue(elevationCache);
		gauge.setLcdDecimals(4);

		// alt.setMajorTickmarkType(TICKMARK_TYPE);
		// gauge.setSize(new Dimension(250, 250));
		// gauge.setMaximumSize(new Dimension(250, 250));
		// gauge.setPreferredSize(new Dimension(250, 250));

		gauge.setSize(new Dimension(250, 250));
		gauge.setMaximumSize(new Dimension(250, 250));
		gauge.setPreferredSize(new Dimension(250, 250));

		gauge.setVisible(true);

	}

	private void personUpdate(Person p) {
		MainDesktopPane desktop = getDesktop();
		boolean useSettlementTool = p.isInSettlement();
		
		if (p.isInVehicle()) {

			Vehicle vv = p.getVehicle();
			useSettlementTool = (vv.getSettlement() != null);
		}

		else if (p.isOutside()) {
			Vehicle vv = p.getVehicle();
			if (vv == null) {
				Settlement s = p.findSettlementVicinity();
				useSettlementTool = (s != null);
			}
		}

		if (useSettlementTool) {
			SettlementWindow sw = (SettlementWindow) desktop.openToolWindow(SettlementWindow.NAME);
			sw.displayPerson(p);
		}
		else {
			NavigatorWindow nw = (NavigatorWindow) desktop.openToolWindow(NavigatorWindow.NAME);
			nw.updateCoordsMaps(p.getCoordinates());
		}
	}

	private void robotUpdate(Robot r) {
		MainDesktopPane desktop = getDesktop();
		boolean useSettlementTool = r.isInSettlement();

		if (!useSettlementTool && r.isInVehicle()) {

			Vehicle vv = r.getVehicle();
			useSettlementTool = (vv.getSettlement() != null);
		}
		else if (r.isOutside()) {
			Settlement s = r.findSettlementVicinity();

			useSettlementTool = (s != null);
		}

		if (useSettlementTool) {
			SettlementWindow sw = (SettlementWindow) desktop.openToolWindow(SettlementWindow.NAME);
			sw.displayRobot(r);
		}
		else {
			NavigatorWindow nw = (NavigatorWindow) desktop.openToolWindow(NavigatorWindow.NAME);
			nw.updateCoordsMaps(r.getCoordinates());
		}
	}

	private void vehicleUpdate(Vehicle v) {
		MainDesktopPane desktop = getDesktop();

		if (v.getSettlement() != null) {
			// still parked inside a garage or within the premise of a settlement
			SettlementWindow sw = (SettlementWindow) desktop.openToolWindow(SettlementWindow.NAME);
			sw.displayVehicle(v);
		} else {
			// out there on a mission
			NavigatorWindow nw = (NavigatorWindow) desktop.openToolWindow(NavigatorWindow.NAME);
			nw.updateCoordsMaps(v.getCoordinates());
		}
	}

	private void equipmentUpdate(Equipment e) {
		MainDesktopPane desktop = getDesktop();
		Vehicle owner = e.getVehicle();

		if (owner == null) {
			// out there on a mission
			NavigatorWindow nw = (NavigatorWindow) desktop.openToolWindow(NavigatorWindow.NAME);
			nw.updateCoordsMaps(e.getCoordinates());
		} else {
			// still parked inside a garage or within the premise of a settlement
			SettlementWindow sw = (SettlementWindow) desktop.openToolWindow(SettlementWindow.NAME);
			sw.displayVehicle(owner);
		}
	}

	/**
	 * Action event occurs.
	 *
	 * @param event the action event
	 */
	public void actionPerformed(ActionEvent event) {
		JComponent source = (JComponent) event.getSource();
		// If the center map button was pressed, update navigator tool.
		if (source == locatorButton) {
			// Add codes to open the settlement map tool and center the map to
			// show the exact/building location inside a settlement if possible

			update();

			Unit unit = getUnit();
			if (unit.getUnitType() == UnitType.PERSON) {
				personUpdate((Person) unit);
			}

			else if (unit.getUnitType() == UnitType.ROBOT) {
				robotUpdate((Robot) unit);
			}

			else if (unit.getUnitType() == UnitType.VEHICLE) {
				vehicleUpdate((Vehicle) unit);
			}

			else if (unit.getUnitType() == UnitType.CONTAINER
					|| unit.getUnitType() == UnitType.EVA_SUIT) {
				equipmentUpdate((Equipment) unit);
			}
		}
	}

	/**
	 * Updates the info on this panel.
	 */
	@Override
	public void update() {
		Unit unit = getUnit();
		
		// If unit's location has changed, update location display.
		// TODO: if a person goes outside the settlement for servicing an equipment
		// does the coordinate (down to how many decimal) change?
		Coordinates location = unit.getCoordinates();
		if (!locationCache.equals(location)) {
			locationCache = location;

			String dir_N_S = null;
			String dir_E_W = null;

			if (locationCache.getLatitudeDouble() >= 0)
				dir_N_S = Msg.getString("direction.degreeSign") + N;
			else
				dir_N_S = Msg.getString("direction.degreeSign") + S;

			if (locationCache.getLongitudeDouble() >= 0)
				dir_E_W = Msg.getString("direction.degreeSign") + E;
			else
				dir_E_W = Msg.getString("direction.degreeSign") + W;

			lcdLat.setLcdUnitString(dir_N_S);
			lcdLong.setLcdUnitString(dir_E_W);
			lcdLat.setLcdValueAnimated(Math.abs(locationCache.getLatitudeDouble()));
			lcdLong.setLcdValueAnimated(Math.abs(locationCache.getLongitudeDouble()));

			double elevationCache = Math.round(TerrainElevation.getAverageElevation(location)
					* 1000.0) / 1000.0;

			setGauge(gauge, elevationCache);

		}

		/////////////////
		
		// Update labels as necessary
		
		Unit container = unit.getContainerUnit();
		if (containerCache != container) {
			containerCache = container;
			String n = container != null ? container.getName() : "";
			containerLabel.setText(n);
		}

		Unit topContainer = unit.getTopContainerUnit();
		if (topContainerCache != topContainer) {
			topContainerCache = topContainer;
			String n = topContainer != null ? topContainer.getName() : "";
			topContainerLabel.setText(n);
		}
		
		Settlement settlement = unit.getSettlement();
		if (settlementCache != settlement) {
			settlementCache = settlement;
			String n = settlement != null ? settlement.getName() : "";
			settlementLabel.setText(n);
		}
		
		LocationStateType locationStateType = unit.getLocationStateType();
		if (locationStateTypeCache != locationStateType) {
			locationStateTypeCache = locationStateType;
			String n = locationStateType != null ? locationStateType.getName() : "";
			locationStateLabel.setText(n);
		}
		
		updateLocationBanner(unit);
	}

	/**
	 * Tracks the location of a person, bot, vehicle, or equipment
	 */
	private void updateLocationBanner(Unit unit) {

		String loc = unit.getLocationTag().getExtendedLocation();

		if (!locationStringCache.equalsIgnoreCase(loc)) {
			locationStringCache = loc;
			lcdText.setLcdText(loc);
		}
	}

	/**
	 * Prepare object for garbage collection.
	 */
	@Override
	public void destroy() {
		super.destroy();
		
		containerCache = null;
		topContainerCache = null;
		locationCache = null;
		locatorButton = null;
		lcdLong = null;
		lcdLat = null;
		lcdText = null;
		gauge = null;

	}
}
