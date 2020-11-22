/**
 * Mars Simulation Project
 * LabelMapLayer.java
 * @version 3.1.2 2020-09-02
 * @author Scott Davis
 */
package org.mars_sim.msp.ui.swing.tool.settlement;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.mars_sim.msp.core.CollectionUtils;
import org.mars_sim.msp.core.Coordinates;
import org.mars_sim.msp.core.Msg;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.ai.mission.Mission;
import org.mars_sim.msp.core.robot.Robot;
import org.mars_sim.msp.core.structure.Settlement;
import org.mars_sim.msp.core.structure.building.Building;
import org.mars_sim.msp.core.structure.construction.ConstructionSite;
import org.mars_sim.msp.core.structure.construction.ConstructionStage;
import org.mars_sim.msp.core.vehicle.Vehicle;

/**
 * A settlement map layer for displaying labels for map objects.
 */
public class LabelMapLayer
implements SettlementMapLayer {
 
	// Static members
	private static final Color HALLWAY_LABEL_COLOR = Color.gray;; //Color.blue;//new Color (79, 108, 44); // dull sage green
	private static final Color BUILDING_LABEL_COLOR = new Color(0, 0, 255);; //dark bright blue //Color.blue;//new Color (79, 108, 44); // dull sage green

	private static final Color BLACK_LABEL_OUTLINE_COLOR = new Color(0, 0, 0, 190); //new Color(0, 0, 0, 150);
	private static final Color WHITE_LABEL_OUTLINE_COLOR = new Color(255, 255, 255, 190);

	private static final Color SHOP_LABEL_COLOR = new Color (146, 112, 255); // pale purple
	private static final Color LAB_LABEL_COLOR = new Color (40, 54, 95); // navy blue
	private static final Color HAB_LABEL_COLOR = new Color (92, 23, 0); // BURGUNDY
	private static final Color REACTOR_LABEL_COLOR = Color.red.darker(); // red
	private static final Color GARAGE_LABEL_COLOR = Color.yellow;//new Color (255, 222, 122); // pale yellow
	private static final Color GREENHOUSE_LABEL_COLOR = new Color (153, 234, 37); //(153, 234, 37) is bright green; (79, 108, 44) is dull sage green //(69, 92, 0) is dark sage //  // new Color(0, 255, 64); //bright green;//
	private static final Color MEDICAL_LABEL_COLOR = new Color (0, 69, 92); // dull blue
	private static final Color LIVING_LABEL_COLOR = new Color (236, 255, 179); // pale yellow
	private static final Color RESOURCE_LABEL_COLOR = new Color (182, 201, 255); // pale blue

	private static final Color CONSTRUCTION_SITE_LABEL_COLOR = new Color(237, 114, 38); //greyish orange
	private static final Color CONSTRUCTION_SITE_LABEL_OUTLINE_COLOR = new Color(0, 0, 0, 150);

	private static final Color VEHICLE_LABEL_COLOR = new Color(249, 134, 134); // light-red //127, 0, 127); // magenta-purple
	private static final Color VEHICLE_LABEL_OUTLINE_COLOR = new Color(0, 0, 0, 150);//(255, 255, 255, 190);

	static final Color FEMALE_COLOR = new Color(255, 153, 225); // light bright pink
	static final Color FEMALE_SELECTED_COLOR = FEMALE_COLOR.darker();
	static final Color FEMALE_OUTLINE_COLOR = Color.MAGENTA;
	static final Color FEMALE_SELECTED_OUTLINE_COLOR = FEMALE_OUTLINE_COLOR.darker();
	
	static final Color MALE_COLOR = new Color(154, 204, 255); // light blue
	static final Color MALE_SELECTED_COLOR = MALE_COLOR.darker();
	static final Color MALE_OUTLINE_COLOR = Color.cyan.darker(); // (210, 210, 210, 190).brighter();
	static final Color MALE_SELECTED_OUTLINE_COLOR = MALE_OUTLINE_COLOR.darker();

	static final Color ROBOT_COLOR = Color.ORANGE;//.darker();
	static final Color ROBOT_SELECTED_COLOR = ROBOT_COLOR.brighter();//.darker();
	static final Color ROBOT_OUTLINE_COLOR = new Color(210, 210, 210, 190);
	static final Color ROBOT_SELECTED_OUTLINE_COLOR = ROBOT_OUTLINE_COLOR.darker();

//	private Font font = new Font("Courier New", Font.PLAIN, 11); 

	// Data members
	private SettlementMapPanel mapPanel;
	
	private Map<String, BufferedImage> labelImageCache;

	/**
	 * Constructor.
	 * @param mapPanel the settlement map panel.
	 */
	public LabelMapLayer(SettlementMapPanel mapPanel) {
		// Initialize data members.
		this.mapPanel = mapPanel;
		labelImageCache = new HashMap<String, BufferedImage>(30);
	}

	@Override
	public void displayLayer(
		Graphics2D g2d, Settlement settlement, Building building,
		double xPos, double yPos, int mapWidth, int mapHeight,
		double rotation, double scale
	) {

		// Save original graphics transforms.
		AffineTransform saveTransform = g2d.getTransform();

		// Get the map center point.
		double mapCenterX = mapWidth / 2D;
		double mapCenterY = mapHeight / 2D;

		// Translate map from settlement center point.
		g2d.translate(mapCenterX + (xPos * scale), mapCenterY + (yPos * scale));

		// Rotate map from North.
		g2d.rotate(rotation, 0D - (xPos * scale), 0D - (yPos * scale));

		// Draw all building labels.
		if (mapPanel.isShowBuildingLabels()) {
			//mapPanel.getSettlementTransparentPanel().getBuildingLabelMenuItem().setState(true);
			drawBuildingLabels(g2d, settlement);
		}

		// Draw all construction site labels.
		if (mapPanel.isShowConstructionLabels()) {
			drawConstructionSiteLabels(g2d, settlement);
		}

		// Draw all vehicle labels.
		if (mapPanel.isShowVehicleLabels()) {
			drawVehicleLabels(g2d, settlement);
		}

		// Draw all people labels.
		drawPersonLabels(g2d, settlement, mapPanel.isShowPersonLabels());

		// Draw all people labels.
		drawRobotLabels(g2d, settlement, mapPanel.isShowRobotLabels());

		// Restore original graphic transforms.
		g2d.setTransform(saveTransform);
	}

	/**
	 * Draw labels for all of the buildings in the settlement.
	 * @param g2d the graphics context.
	 * @param settlement the settlement.
	 */
	private void drawBuildingLabels(Graphics2D g2d, Settlement settlement) {
		if (settlement != null) {
			
			double scale = mapPanel.getScale();
			int size = (int)(scale / 2.0);
			size = Math.max(size, 12);
			
			Iterator<Building> i = settlement.getBuildingManager().getBuildings().iterator();
			while (i.hasNext()) {
				Building building = i.next();
				String name = building.getNickName();
				String words[] = name.split(" ");
				int s = words.length;
				
				if (name.contains("Hallway")) {
					// Shrink the size of a hallway label.
					//e.g. Turned "Hallway 12 " into "H12"
					String newName = "H " + words[1];
					drawStructureLabel(g2d, newName, building.getXLocation(), building.getYLocation(),
							HALLWAY_LABEL_COLOR, WHITE_LABEL_OUTLINE_COLOR, 0);
				}
				else if (name.contains("Tunnel")) {
					// Shrink the size of a hallway label.
					//e.g. Turned "Hallway 12 " into "H12"
					String newName = "T " + words[1];
					drawStructureLabel(g2d, newName, building.getXLocation(), building.getYLocation(),
							HALLWAY_LABEL_COLOR, WHITE_LABEL_OUTLINE_COLOR, 0);
				}
				else {
					String last_1 = words[s-1];
					String last_2 = words[s-2];		
					words[s-2] = last_2 + " " + last_1;
					s = s-1;
					// Split up the name into multiple lines
					if (name.contains("Reactor") || name.contains("Solar")
							|| name.contains("Wind") || name.contains("Power")
							|| name.contains("Generator") || name.contains("Battery")
							|| name.contains("Areothermal")){
						for (int j = 0; j < s; j++) {
							drawStructureLabel(g2d, words[j], building.getXLocation(), building.getYLocation(),
									REACTOR_LABEL_COLOR, BLACK_LABEL_OUTLINE_COLOR, j * (size + 0));
						}
					}
					else if (name.contains("Greenhouse")) {
						for (int j = 0; j < s; j++) {
							drawStructureLabel(g2d, words[j], building.getXLocation(), building.getYLocation(),
									GREENHOUSE_LABEL_COLOR, BLACK_LABEL_OUTLINE_COLOR, j * (size + 0));
						}
					}
					else if (name.contains("Processor") || name.contains("Base")) {
						for (int j = 0; j < s; j++) {
							drawStructureLabel(g2d, words[j], building.getXLocation(), building.getYLocation(),
									RESOURCE_LABEL_COLOR, BLACK_LABEL_OUTLINE_COLOR, j * (size + 0));
						}
					}
					else if (name.contains("Command") || name.contains("Hub")
							|| name.contains("Lander")) {
						for (int j = 0; j < s; j++) {
							drawStructureLabel(g2d, words[j], building.getXLocation(), building.getYLocation(),
									HAB_LABEL_COLOR, WHITE_LABEL_OUTLINE_COLOR, j * (size + 0));
						}
					}
					else if (name.contains("Bunkhouse") || name.contains("Residential")
							|| name.contains("Lounge")) {
						for (int j = 0; j < s; j++) {
							drawStructureLabel(g2d, words[j], building.getXLocation(), building.getYLocation(),
									LIVING_LABEL_COLOR, BLACK_LABEL_OUTLINE_COLOR, j * (size + 0));
						}
					}
					else if (name.contains("Bin") || name.contains("Workshop")
							|| name.contains("Manu") || name.contains("Storage")
							|| name.contains("Machin")
							) {
						for (int j = 0; j < s; j++) {
							drawStructureLabel(g2d, words[j], building.getXLocation(), building.getYLocation(),
									SHOP_LABEL_COLOR, WHITE_LABEL_OUTLINE_COLOR, j * (size + 0));
						}
					}
					else if (name.contains("Garage")) {
						for (int j = 0; j < s; j++) {
							drawStructureLabel(g2d, words[j], building.getXLocation(), building.getYLocation(),
									GARAGE_LABEL_COLOR, BLACK_LABEL_OUTLINE_COLOR, j * (size + 0));
						}
					}
					else if (name.contains("Medical") || name.contains("Infirmary")) {
						for (int j = 0; j < s; j++) {
							drawStructureLabel(g2d, words[j], building.getXLocation(), building.getYLocation(),
									MEDICAL_LABEL_COLOR, WHITE_LABEL_OUTLINE_COLOR, j * (size + 0));
						}
					}
					else if (name.contains("Lab") || name.contains("Observatory")
							|| name.contains("Research")) {
						for (int j = 0; j < s; j++) {
							drawStructureLabel(g2d, words[j], building.getXLocation(), building.getYLocation(),
									LAB_LABEL_COLOR, WHITE_LABEL_OUTLINE_COLOR, j * (size + 0));
						}
					}

					else {
						for (int j = 0; j < s; j++) {
							drawStructureLabel(g2d, words[j], building.getXLocation(), building.getYLocation(),
									BUILDING_LABEL_COLOR, WHITE_LABEL_OUTLINE_COLOR, j * (size + 0));
						}
					}
				}
			}
		}
	}
 
	/**
	 * Draw labels for all of the construction sites in the settlement.
	 * @param g2d the graphics context.
	 * @param settlement the settlement.
	 */
	private void drawConstructionSiteLabels(Graphics2D g2d, Settlement settlement) {
		if (settlement != null) {
			
			double scale = mapPanel.getScale();
			int size = (int)(scale / 2.0);
			size = Math.max(size, 12);
					
			Iterator<ConstructionSite> i = settlement
			.getConstructionManager()
			.getConstructionSites()
			.iterator();
			while (i.hasNext()) {
				ConstructionSite site = i.next();
				String siteLabel = getConstructionLabel(site);
				// Split up the name into multiple lines except with the whitespace after character 'm'
				String words[] = siteLabel.split(" ");
				int s = words.length;
				String last_1 = words[s-1];
				String last_2 = words[s-2];
				String last_3 = words[s-3];
				//System.out.print("last_1 : " + last_1 + "   last_2 : " + last_2);
				//System.out.print(" [" + last_3 + " " + last_2 + " " + last_1 + "] ");
				int s_1 = last_1.length();
				String test_1 = last_1.substring(s_1-1);
				int s_2 = last_2.length();
				String test_2 = last_2.substring(s_2-1);
				int s_3 = last_3.length();
				String test_3 = last_3.substring(s_3-1);
				//System.out.println("   test_1 : " + test_1 + "   test_2 : " + test_2);
				words[s-3] = last_3 + " " + last_2 + " " + last_1;
				s = s-2;
				if (test_1.equalsIgnoreCase("m") && test_2.equalsIgnoreCase("x") && test_3.equalsIgnoreCase("m")) {
					for (int j = 0; j < s; j++) {
						drawStructureLabel(g2d, words[j], site.getXLocation(), site.getYLocation(),
							CONSTRUCTION_SITE_LABEL_COLOR, CONSTRUCTION_SITE_LABEL_OUTLINE_COLOR, j * (size + 0));
					}
				}
				else {
					for (int j = 0; j < s; j++) {
						drawStructureLabel(g2d, words[j], site.getXLocation(), site.getYLocation(),
							CONSTRUCTION_SITE_LABEL_COLOR, CONSTRUCTION_SITE_LABEL_OUTLINE_COLOR, j * (size + 0));
					}
				}
			}
		}
	}

	/**
	 * Gets the label for a construction site.
	 * @param site the construction site.
	 * @return the construction label.
	 */
	public static String getConstructionLabel(ConstructionSite site) {
		String label = ""; //$NON-NLS-1$
		ConstructionStage stage = site.getCurrentConstructionStage();
		if (stage != null) {
			if (site.isUndergoingConstruction()) {
				label = Msg.getString("LabelMapLayer.constructing", stage.getInfo().getName()); //$NON-NLS-1$
			} else if (site.isUndergoingSalvage()) {
				label = Msg.getString("LabelMapLayer.salvaging", stage.getInfo().getName()); //$NON-NLS-1$
			} else if (site.hasUnfinishedStage()) {
				if (stage.isSalvaging()) {
					label = Msg.getString("LabelMapLayer.salvagingUnfinished", stage.getInfo().getName()); //$NON-NLS-1$
				} else {
					label = Msg.getString("LabelMapLayer.constructingUnfinished", stage.getInfo().getName()); //$NON-NLS-1$
				}
			} else {
				label = Msg.getString("LabelMapLayer.completed", stage.getInfo().getName()); //$NON-NLS-1$
			}
		} else {
			label = Msg.getString("LabelMapLayer.noConstruction"); //$NON-NLS-1$
		}
		return label;
	}

	/**
	 * Draw labels for all of the vehicles parked at the settlement.
	 * @param g2d the graphics context.
	 * @param settlement the settlement.
	 */
	private void drawVehicleLabels(Graphics2D g2d, Settlement settlement) {
		if (settlement != null) {
			double scale = mapPanel.getScale();
			int size = (int)(scale / 2.0);
			size = Math.max(size, 12);
					
			// Draw all vehicles that are at the settlement location.
			Iterator<Vehicle> i = unitManager.getVehicles().iterator();
			while (i.hasNext()) {
				Vehicle vehicle = i.next();
				// Draw vehicles that are at the settlement location.
				Coordinates settlementLoc = settlement.getCoordinates();
				Coordinates vehicleLoc = vehicle.getCoordinates();
				if (vehicleLoc.equals(settlementLoc)) {
					
					if (vehicle.getName().contains("LUV")) {
						drawStructureLabel(g2d,vehicle.getName(), vehicle.getXLocation(), vehicle.getYLocation(),
							VEHICLE_LABEL_COLOR, VEHICLE_LABEL_OUTLINE_COLOR, 0);
					}
					
					else {
						// Split up the name into multiple lines
						String words[] = vehicle.getName().split(" ");
						int s = words.length;
						for (int j = 0; j < s; j++) {
							drawStructureLabel(g2d, words[j], vehicle.getXLocation(), vehicle.getYLocation(),
								VEHICLE_LABEL_COLOR, VEHICLE_LABEL_OUTLINE_COLOR, j * (size + 0));
						}
					}
				}
			}
		}
	}

	/**
	 * Draw labels for all people at the settlement.
	 * 
	 * @param g2d the graphics context.
	 * @param settlement the settlement.
	 * @param showNonSelectedPeople true if showing non-selected person labels.
	 */
	private void drawPersonLabels(
		Graphics2D g2d, Settlement settlement,
		boolean showNonSelectedPeople
	) {

		List<Person> people = CollectionUtils.getPeopleToDisplay(settlement);
		Person selectedPerson = mapPanel.getSelectedPerson();
		int xoffset = 10;
		double scale = mapPanel.getScale();
		int size = (int)(scale / 2.0);
		size = Math.max(size, 12);
		
//		Color color = FEMALE_COLOR;
//		Color oColor = FEMALE_OUTLINE_COLOR;
		Color sColor = FEMALE_SELECTED_COLOR;
		Color soColor = FEMALE_SELECTED_OUTLINE_COLOR;
		
		if (selectedPerson != null) {
			
			if (selectedPerson.isMale()) {
//				color = MALE_COLOR;
//				oColor = MALE_OUTLINE_COLOR;
				sColor = MALE_SELECTED_COLOR;
				soColor = MALE_SELECTED_OUTLINE_COLOR;
			}
		
			// Draw selected person.
			if (people.contains(selectedPerson)) {
				// Draw person name.
				drawPersonRobotLabel(g2d, selectedPerson.getName(), selectedPerson.getXLocation(),
					selectedPerson.getYLocation(), sColor, soColor,
					xoffset, 0);

				// Draw task.
				String taskString = Msg.getString("LabelMapLayer.activity", selectedPerson.getMind().getTaskManager().getTaskDescription(false)); //$NON-NLS-1$
				if (taskString != null && !taskString.equals(""))
					drawPersonRobotLabel(
//						g2d, selectedPerson.getMind().getTaskManager().getTaskDescription(false), selectedPerson.getXLocation(),
						g2d, taskString, selectedPerson.getXLocation(),
						selectedPerson.getYLocation(), sColor, soColor,
						xoffset, size + 0);

				// Draw mission.
				Mission mission = selectedPerson.getMind().getMission();
				if (mission != null) {
					String missionString = Msg.getString("LabelMapLayer.mission", mission.getDescription(), mission.getPhaseDescription()); //$NON-NLS-1$
					if (missionString != null && !missionString.equals(""))
						drawPersonRobotLabel(
							g2d, missionString, selectedPerson.getXLocation(),
							selectedPerson.getYLocation(), sColor, soColor,
							xoffset, 2 * (size + 0));
				}
			}
		}

		// Draw all people except selected person.
		if (showNonSelectedPeople) {
			Iterator<Person> i = people.iterator();
			while (i.hasNext()) {
				Person person = i.next();
				
				if (!person.equals(selectedPerson)) {
					
					// Split up the name into 2 lines
					String words[] = person.getName().split(" ");
					int s = words.length;
					String n = "";
					for (int j = 0; j < s; j++) {
						if (j == 0) n = words[0];
						else n += " " + words[j].substring(0, 1) + ".";
					}				
					if (person.isMale())
						drawPersonRobotLabel(g2d, n, person.getXLocation(), person.getYLocation(),
								MALE_COLOR, MALE_OUTLINE_COLOR, xoffset, 0);
					else
						drawPersonRobotLabel(g2d, n, person.getXLocation(), person.getYLocation(),
								FEMALE_COLOR, FEMALE_OUTLINE_COLOR, xoffset, 0);
				}
			}
		}
	}


	/**
	 * Draw labels for all robots at the settlement.
	 * 
	 * @param g2d the graphics context.
	 * @param settlement the settlement.
	 * @param showNonSelectedRobots true if showing non-selected robot labels.
	 */
	private void drawRobotLabels(
		Graphics2D g2d, Settlement settlement,
		boolean showNonSelectedRobots
	) {

		List<Robot> robots = RobotMapLayer.getRobotsToDisplay(settlement);
		Robot selectedRobot = mapPanel.getSelectedRobot();
		int xoffset = 10;
		double scale = mapPanel.getScale();
		int size = (int)(scale);
		size = Math.max(size, 12);
		
		// Draw all robots except selected robot.
		if (showNonSelectedRobots) {
			Iterator<Robot> i = robots.iterator();
			while (i.hasNext()) {
				Robot robot = i.next();

				if (!robot.equals(selectedRobot)) {
//					String words[] = robot.getName().split(" ");
//					int s = words.length;
//					for (int j = 0; j < s; j++)
//						drawPersonRobotLabel(g2d, words[j], robot.getXLocation(), robot.getYLocation(),
//								ROBOT_LABEL_COLOR, ROBOT_LABEL_OUTLINE_COLOR, xoffset, j * (size + 0));
					drawPersonRobotLabel(g2d, robot.getName(), robot.getXLocation(), robot.getYLocation(),
							ROBOT_COLOR, ROBOT_OUTLINE_COLOR, xoffset, 0);
				}
			}
		}

		// Draw selected robot.
		if (robots.contains(selectedRobot)) {
			// Draw robot name.
			drawPersonRobotLabel(
				g2d, selectedRobot.getName(), selectedRobot.getXLocation(),
				selectedRobot.getYLocation(), ROBOT_SELECTED_COLOR, ROBOT_SELECTED_OUTLINE_COLOR,
				xoffset, 0);

			// Draw task.
			String taskString = Msg.getString("LabelMapLayer.activity", selectedRobot.getBotMind().getBotTaskManager().getTaskDescription(false)); //$NON-NLS-1$
			if (taskString != null && !taskString.equals(""))
				drawPersonRobotLabel(
					g2d, taskString, selectedRobot.getXLocation(),
					selectedRobot.getYLocation(), ROBOT_SELECTED_COLOR, ROBOT_SELECTED_OUTLINE_COLOR,
					xoffset, size + 0);

			// Draw mission.
			Mission mission = selectedRobot.getBotMind().getMission();
			if (mission != null) {
				String missionString = Msg.getString("LabelMapLayer.mission", mission.getDescription(), mission.getPhaseDescription()); //$NON-NLS-1$
				if (missionString != null && !missionString.equals(""))
					drawPersonRobotLabel(
						g2d, missionString, selectedRobot.getXLocation(),
						selectedRobot.getYLocation(), ROBOT_SELECTED_COLOR, ROBOT_SELECTED_OUTLINE_COLOR,
						xoffset, 2 * (size + 0));
			}
		}
	}
	
	/**
	 * Draws a label centered at the X, Y location.
	 * 
	 * @param g2d the graphics 2D context.
	 * @param label the label string.
	 * @param xLoc the X location from center of settlement (meters).
	 * @param yLoc the y Location from center of settlement (meters).
	 * @param labelColor the color of the label.
	 * @param labelOutlineColor the color of the outline of the label.
	 */
	private void drawStructureLabel(
		Graphics2D g2d, String label, double xLoc, double yLoc,
		Color labelColor, Color labelOutlineColor, int yOffset
	) {
		double scale = mapPanel.getScale();
		float fontSize = Math.round(scale / 2.5);
		int size = (int)(fontSize / 2.0);
		size = Math.max(size, 2);
		
		// Save original graphics transforms.
		AffineTransform saveTransform = g2d.getTransform();
		Font saveFont = g2d.getFont();

		// Get the label image.
		Font font = g2d.getFont().deriveFont(Font.BOLD, 8f + fontSize);
		g2d.setFont(font);
		
		BufferedImage labelImage = getLabelImage(
			label, font, g2d.getFontRenderContext(),
			labelColor, labelOutlineColor
		);

		// Determine transform information.
		double centerX = labelImage.getWidth() / 2D;
		double centerY = labelImage.getHeight() / 2D;
		double translationX = (-1D * xLoc * mapPanel.getScale()) - centerX;
		double translationY = (-1D * yLoc * mapPanel.getScale()) - centerY;

		// Apply graphic transforms for label.
		AffineTransform newTransform = new AffineTransform(saveTransform);
		newTransform.translate(translationX, translationY);
		newTransform.rotate(mapPanel.getRotation() * -1D, centerX, centerY);
		g2d.setTransform(newTransform);

		// Draw image label with yOffset
		g2d.drawImage(labelImage, 0, yOffset, mapPanel);
		
		// Restore original graphic transforms.
		g2d.setTransform(saveTransform);
		g2d.setFont(saveFont);
	}

	/**
	 * Draws a label to the right of an X, Y location.
	 * @param g2d the graphics 2D context.
	 * @param label the label string.
	 * @param xLoc the X location from center of settlement (meters).
	 * @param yLoc the y Location from center of settlement (meters).
	 * @param labelColor the color of the label.
	 * @param labelOutlineColor the color of the outline of the label.
	 * @param xOffset the X pixel offset from the center point.
	 * @param yOffset the Y pixel offset from the center point.
	 */
	private void drawPersonRobotLabel(
		Graphics2D g2d, String label, double xLoc, double yLoc,
		Color labelColor, Color labelOutlineColor, int xOffset, int yOffset
	) {

		double scale = mapPanel.getScale();
		float fontSize = Math.round(scale / 2.5);
		int size = (int)(fontSize / 2.0);
		size = Math.max(size, 1);
		
		// Save original graphics transforms.
		AffineTransform saveTransform = g2d.getTransform();
		Font saveFont = g2d.getFont();

		// Get the label image.
		Font font = g2d.getFont().deriveFont(Font.PLAIN, 12f + fontSize);
		g2d.setFont(font);
		
		BufferedImage labelImage = getLabelImage(
			label, font, g2d.getFontRenderContext(),
			labelColor, labelOutlineColor
		);

		// Determine transform information.
		double centerX = labelImage.getWidth() / 2D ;
		double centerY = labelImage.getHeight() / 2D ;
		double translationX = (-1D * xLoc * scale) - centerX;
		double translationY = (-1D * yLoc * scale) - centerY;

		// Apply graphic transforms for label.
		AffineTransform newTransform = new AffineTransform(saveTransform);
		newTransform.translate(translationX, translationY);
		newTransform.rotate(mapPanel.getRotation() * -1D, centerX, centerY);
		g2d.setTransform(newTransform);

		// Draw image label.
		int widthOffset = (int)((centerX + fontSize) / 1.5) + xOffset;
		int heightOffset = (int)((centerY + fontSize) / 1.5 ) + yOffset;
		g2d.drawImage(labelImage, widthOffset, heightOffset, mapPanel);

		// Restore original graphic transforms.
		g2d.setTransform(saveTransform);
		g2d.setFont(saveFont);
	}

	/**
	 * Gets an image of the label from cache or creates one if it doesn't exist.
	 * @param label the label string.
	 * @param font the font to use.
	 * @param fontRenderContext the font render context to use.
	 * @param labelColor the color of the label.
	 * @param labelOutlineColor the color of the outline of the label.
	 * @return buffered image of label.
	 */
	private BufferedImage getLabelImage(
		String label, Font font, FontRenderContext fontRenderContext, Color labelColor,
		Color labelOutlineColor
	) { 
		BufferedImage labelImage = null;
		String labelId = label + font.toString() + labelColor.toString() + labelOutlineColor.toString();
		if (labelImageCache.containsKey(labelId)) {
			labelImage = labelImageCache.get(labelId);
		} else {
			labelImage = createLabelImage(label, font, fontRenderContext, labelColor, labelOutlineColor);
			labelImageCache.put(labelId, labelImage);
		}
		return labelImage;
	}

//	/**
//	 * Creates a label image.
//	 * @param label the label string.
//	 * @param font the font to use.
//	 * @param fontRenderContext the font render context to use.
//	 * @param labelColor the color of the label.
//	 * @param labelOutlineColor the color of the outline of the label.
//	 * @return buffered image of label.
//   */
//	private BufferedImage createLabelImage(
//		String label, Font font, FontRenderContext fontRenderContext, Color labelColor,
//		Color labelOutlineColor) {
//
//		// Determine bounds.
//		TextLayout textLayout1 = new TextLayout(label, font, fontRenderContext);
//		Rectangle2D bounds1 = textLayout1.getBounds();
//
//		// Get label shape.
//		Shape labelShape = textLayout1.getOutline(null);
//
//		// Create buffered image for label.
//		int width = (int) (bounds1.getWidth() + bounds1.getX()) + 4;
//		int height = (int) (bounds1.getHeight()) + 4;
//		BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
//
//		// Get graphics context from buffered image.
//		Graphics2D g2d = (Graphics2D) bufferedImage.getGraphics();
//		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
//		g2d.translate(2D - bounds1.getX(), 2D - bounds1.getY());
//
//		// Draw label outline.
//		Stroke saveStroke = g2d.getStroke();
//		g2d.setColor(labelOutlineColor);
//		g2d.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
//		g2d.draw(labelShape);
//		g2d.setStroke(saveStroke);
//
//		// Fill label
//		g2d.setColor(labelColor);
//		g2d.fill(labelShape);
//
//		// Dispose of image graphics context.
//		g2d.dispose();
//
//		return bufferedImage;
//	}


	/**
	 * Creates a label image.
	 * @param label the label string.
	 * @param font the font to use.
	 * @param fontRenderContext the font render context to use.
	 * @param labelColor the color of the label.
	 * @param labelOutlineColor the color of the outline of the label.
	 * @return buffered image of label.
	 */
	private BufferedImage createLabelImage(
		String label, Font font, FontRenderContext fontRenderContext, Color labelColor,
		Color labelOutlineColor) {

		// Determine bounds.
		TextLayout textLayout1 = new TextLayout(label, font, fontRenderContext);
		Rectangle2D bounds1 = textLayout1.getBounds();

		// Get label shape.
		Shape labelShape = textLayout1.getOutline(null);

		// Create buffered image for label.
		int width = (int) (bounds1.getWidth() + bounds1.getX()) + 4;
		int height = (int) (bounds1.getHeight()) + 4;
		BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

		// Get graphics context from buffered image.
		Graphics2D g2d = (Graphics2D) bufferedImage.getGraphics();
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2d.translate(2D - bounds1.getX(), 2D - bounds1.getY());

		// Draw label outline.
		Stroke saveStroke = g2d.getStroke();
		g2d.setColor(labelOutlineColor);
		g2d.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

		// Draw outline
		g2d.draw(labelShape);

		// Restore stroke
		g2d.setStroke(saveStroke);
		
		g2d.setColor(labelColor);
		// Fill label
		g2d.fill(labelShape);

		// Dispose of image graphics context.
		g2d.dispose();

		return bufferedImage;
	}


	@Override
	public void destroy() {
		// Clear label image cache.
		labelImageCache.clear();
	}
}
