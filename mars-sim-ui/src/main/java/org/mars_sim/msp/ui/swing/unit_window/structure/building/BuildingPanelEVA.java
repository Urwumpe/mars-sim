/*
 * Mars Simulation Project
 * BuildingPanelEVA.java
 * @date 2022-07-10
 * @author Manny Kung
 */
package org.mars_sim.msp.ui.swing.unit_window.structure.building;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.Collection;

import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SpringLayout;

import org.mars_sim.msp.core.Msg;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.structure.building.function.BuildingAirlock;
import org.mars_sim.msp.core.structure.building.function.EVA;
import org.mars_sim.msp.ui.swing.ImageLoader;
import org.mars_sim.msp.ui.swing.MainDesktopPane;
import org.mars_sim.msp.ui.swing.tool.SpringUtilities;
import org.mars_sim.msp.ui.swing.unit_window.UnitListPanel;

import com.alee.laf.panel.WebPanel;


/**
 * The BuildingPanelEVA class presents the EVA activities
 * of a building.
 */
@SuppressWarnings("serial")
public class BuildingPanelEVA extends BuildingFunctionPanel {
	
	private static final String SUIT_ICON = Msg.getString("icon.suit"); //$NON-NLS-1$

	private static final String UNLOCKED = "UNLOCKED";
	private static final String LOCKED = "LOCKED";

	private int innerDoorCache;
	private int outerDoorCache;
	private int occupiedCache;
	private int emptyCache;
	private double cycleTimeCache;
	private boolean activationCache;

	private String operatorCache = "";
	private String airlockStateCache = "";
	private String innerDoorStateCache = "";
	private String outerDoorStateCache = "";

	private JTextField innerDoorLabel;
	private JTextField outerDoorLabel;
	private JTextField occupiedLabel;
	private JTextField emptyLabel;
	private JTextField operatorLabel;
	private JTextField airlockStateLabel;
	private JTextField activationLabel;
	private JTextField cycleTimeLabel;
	private JTextField innerDoorStateLabel;
	private JTextField outerDoorStateLabel;

	private UnitListPanel<Person> occupants;
	private UnitListPanel<Person> reservationList;

	private EVA eva;
	private BuildingAirlock buildingAirlock;

	/**
	 * Constructor.
	 * @param eva the eva function of a building this panel is for.
	 * @param desktop The main desktop.
	 */
	public BuildingPanelEVA(EVA eva, MainDesktopPane desktop) {

		// Use BuildingFunctionPanel constructor
		super(
			Msg.getString("BuildingPanelEVA.title"), 
			ImageLoader.getNewIcon(SUIT_ICON), 
			eva.getBuilding(), 
			desktop
		);

		// Initialize data members
		this.eva = eva;
		this.buildingAirlock = (BuildingAirlock)eva.getAirlock();
	}
	
	/**
	 * Build the UI
	 */
	@Override
	protected void buildUI(JPanel center) {

		// Create label panel
		WebPanel labelPanel = new WebPanel(new SpringLayout());
		center.add(labelPanel, BorderLayout.NORTH);


		// Create innerDoorLabel
		innerDoorLabel = addTextField(labelPanel, Msg.getString("BuildingPanelEVA.innerDoor.number"),
									  eva.getNumAwaitingInnerDoor(), 4, null);

		if (eva.getAirlock().isInnerDoorLocked())
			innerDoorStateCache = LOCKED;
		else {
			innerDoorStateCache = UNLOCKED;
		}
		// Create innerDoorStateLabel
		innerDoorStateLabel = addTextField(labelPanel, Msg.getString("BuildingPanelEVA.innerDoor.state"),
										   innerDoorStateCache, 8, null);

		// Create outerDoorLabel
		outerDoorLabel = addTextField(labelPanel, Msg.getString("BuildingPanelEVA.outerDoor.number"),
									  eva.getNumAwaitingOuterDoor(), 4, null);

		
		if (eva.getAirlock().isOuterDoorLocked())
			outerDoorStateCache = LOCKED;
		else {
			outerDoorStateCache = UNLOCKED;
		}
		// Create outerDoorStateLabel
		outerDoorStateLabel = addTextField(labelPanel, Msg.getString("BuildingPanelEVA.outerDoor.state"),
										   outerDoorStateCache, 8, null);

		
		// Create occupiedLabel
		occupiedLabel = addTextField(labelPanel, Msg.getString("BuildingPanelEVA.occupied"),
									 eva.getNumInChamber(), 4, null);


		// Create activationLabel
		activationLabel = addTextField(labelPanel, Msg.getString("BuildingPanelEVA.airlock.activation"),
										 buildingAirlock.isActivated() + "", 8, null);

		// Create emptyLabel
		emptyLabel = addTextField(labelPanel, Msg.getString("BuildingPanelEVA.empty"),
								  eva.getNumEmptied(), 4, null);

		// Create airlockStateLabel
		airlockStateLabel = addTextField(labelPanel, Msg.getString("BuildingPanelEVA.airlock.state"),
										 buildingAirlock.getState().toString(), 8, null);

		// Create cycleTimeLabel
		cycleTimeLabel = addTextField(labelPanel, Msg.getString("BuildingPanelEVA.airlock.cycleTime"),
									  DECIMAL_PLACES1.format(buildingAirlock.getRemainingCycleTime()), 4, null);
		
		// Create OperatorLabel
		operatorLabel = addTextField(labelPanel, Msg.getString("BuildingPanelEVA.operator"),
									 eva.getOperatorName(), 10, null);
		
		SpringUtilities.makeCompactGrid(labelPanel,
                5, 4, //rows, cols
                10, INITY_DEFAULT,        //initX, initY
                XPAD_DEFAULT, YPAD_DEFAULT);       //xPad, yPad	
		
		// Create occupant panel
		WebPanel occupantPanel = new WebPanel(new FlowLayout(FlowLayout.CENTER));
		addBorder(occupantPanel, Msg.getString("BuildingPanelEVA.titledB.occupants"));
		center.add(occupantPanel, BorderLayout.CENTER);

		// Create occupant list 
		MainDesktopPane desktop = getDesktop();
		occupants = new UnitListPanel<>(desktop, new Dimension(150, 100)) {
			@Override
			protected Collection<Person> getData() {
				return getUnitsFromIds(buildingAirlock.getAllInsideOccupants());
			}
		};
		occupantPanel.add(occupants);

		// Create reservation panel
		WebPanel reservationPanel = new WebPanel(new FlowLayout(FlowLayout.CENTER));
		addBorder(reservationPanel, Msg.getString("BuildingPanelEVA.titledB.Reserved"));
		center.add(reservationPanel, BorderLayout.SOUTH);

		reservationList = new UnitListPanel<>(desktop, new Dimension(150, 100)) {
			@Override
			protected Collection<Person> getData() {
				return getUnitsFromIds(buildingAirlock.getReserved());
			}		
		};	
		reservationPanel.add(reservationList);
	}

	@Override
	public void update() {

		// Update innerDoorLabel
		if (innerDoorCache != eva.getNumAwaitingInnerDoor()) {
			innerDoorCache = eva.getNumAwaitingInnerDoor();
			innerDoorLabel.setText(Integer.toString(innerDoorCache));
		}

		// Update outerDoorLabel
		if (outerDoorCache != eva.getNumAwaitingOuterDoor()) {
			outerDoorCache = eva.getNumAwaitingOuterDoor();
			outerDoorLabel.setText(Integer.toString(outerDoorCache));
		}

		// Update occupiedLabel
		if (occupiedCache != eva.getNumInChamber()) {
			occupiedCache = eva.getNumInChamber();
			occupiedLabel.setText(Integer.toString(occupiedCache));
		}

		// Update emptyLabel
		if (emptyCache != eva.getNumEmptied()) {
			emptyCache = eva.getNumEmptied();
			emptyLabel.setText(Integer.toString(emptyCache));
		}

		// Update operatorLabel
		if (!operatorCache.equalsIgnoreCase(eva.getOperatorName())) {
			operatorCache = eva.getOperatorName();
			operatorLabel.setText(operatorCache);
		}

		// Update airlockStateLabel
		String state = buildingAirlock.getState().toString();
		if (!airlockStateCache.equalsIgnoreCase(state)) {
			airlockStateCache = state;
			airlockStateLabel.setText(state);
		}
		
		// Update activationLabel
		boolean activated = buildingAirlock.isActivated();
		if (activationCache != activated) {
			activationCache = activated;
			activationLabel.setText(activated + "");
		}

		// Update cycleTimeLabel
		double time = buildingAirlock.getRemainingCycleTime();
		if (cycleTimeCache != time) {
			cycleTimeCache = time;
			cycleTimeLabel.setText(DECIMAL_PLACES1.format(cycleTimeCache));
		}

		String innerDoorState = "";
		if (buildingAirlock.isInnerDoorLocked())
			innerDoorState = LOCKED;
		else {
			innerDoorState = UNLOCKED;
		}

		// Update innerDoorStateLabel
		if (!innerDoorStateCache.equalsIgnoreCase(innerDoorState)) {
			innerDoorStateCache = innerDoorState;
			innerDoorStateLabel.setText(innerDoorState);
		}

		String outerDoorState = "";
		if (buildingAirlock.isOuterDoorLocked())
			outerDoorState = LOCKED;
		else {
			outerDoorState = UNLOCKED;
		}

		// Update outerDoorStateLabel
		if (!outerDoorStateCache.equalsIgnoreCase(outerDoorState)) {
			outerDoorStateCache = outerDoorState;
			outerDoorStateLabel.setText(outerDoorState);
		}

		// Update occupant list
		occupants.update();
		reservationList.update();
	}

	@Override
	public void destroy() {
		super.destroy();

		occupants = null;
		reservationList = null;
		
		eva = null;
		buildingAirlock = null;
	}
}