/*
 * Mars Simulation Project
 * TabPanelEVA.java
 * @date 2022-09-05
 * @author Manny Kung
 */

package com.mars_sim.ui.swing.unit_window.vehicle;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.Collection;

import javax.swing.JLabel;
import javax.swing.JPanel;

import com.mars_sim.core.person.Person;
import com.mars_sim.core.structure.Airlock.AirlockMode;
import com.mars_sim.core.vehicle.Rover;
import com.mars_sim.core.vehicle.VehicleAirlock;
import com.mars_sim.tools.Msg;
import com.mars_sim.ui.swing.ImageLoader;
import com.mars_sim.ui.swing.MainDesktopPane;
import com.mars_sim.ui.swing.StyleManager;
import com.mars_sim.ui.swing.unit_window.TabPanel;
import com.mars_sim.ui.swing.unit_window.UnitListPanel;
import com.mars_sim.ui.swing.utils.AttributePanel;

/**
 * The TabPanelEVA class represents the EVA airlock function of a vehicle.
 */
@SuppressWarnings("serial")
public class TabPanelEVA extends TabPanel {

	private static final String EVA_ICON = "eva"; 
	
	private static final String UNLOCKED = "Unlocked";
	private static final String LOCKED = "Locked";
	
	private boolean activationCache;
	private boolean transitionCache;
	
	private int innerDoorCache;
	private int outerDoorCache;
	private int occupiedCache;
	private int emptyCache;
	
	private double cycleTimeCache;
	
	private AirlockMode airlockModeCache;
	
	private String operatorCache = "";
	private String airlockStateCache = "";
	private String innerDoorStateCache = "";
	private String outerDoorStateCache = "";

	private JLabel innerDoorLabel;
	private JLabel outerDoorLabel;
	private JLabel occupiedLabel;
	private JLabel emptyLabel;
	private JLabel operatorLabel;
	private JLabel airlockStateLabel;
	private JLabel activationLabel;
	private JLabel transitionLabel;
	private JLabel cycleTimeLabel;
	private JLabel innerDoorStateLabel;
	private JLabel outerDoorStateLabel;
	private JLabel airlockModeLabel;
	
	private	UnitListPanel<Person> occupants;

	private VehicleAirlock vehicleAirlock;


    /**
     * Constructor.
     * @param vehicle the vehicle.
     * @param desktop The main desktop.
     */
    public TabPanelEVA(Rover vehicle, MainDesktopPane desktop) {
        // Use the TabPanel constructor
        super(
            Msg.getString("TabPanelEVA.title"), //$NON-NLS-1$
			ImageLoader.getIconByName(EVA_ICON),        	
        	Msg.getString("TabPanelEVA.title"), //$NON-NLS-1$
        	desktop
        );

        vehicleAirlock = (VehicleAirlock)vehicle.getAirlock();
    }
    
	/**
	 * Build the UI
	 * 
	 * @param content
	 */
    @Override
    protected void buildUI(JPanel content) {
    	if (vehicleAirlock == null) {
    		return;
    	}

        // Create top panel
        JPanel topPanel = new JPanel(new BorderLayout());
        content.add(topPanel, BorderLayout.CENTER);

		AttributePanel labelGrid = new AttributePanel(6, 2);
		topPanel.add(labelGrid, BorderLayout.NORTH);
		
		// Create innerDoorLabel
		innerDoorLabel = labelGrid.addTextField(Msg.getString("TabPanelEVA.innerDoor.number"),
				Integer.toString(vehicleAirlock.getNumAwaitingInnerDoor()), null);

		if (vehicleAirlock.isInnerDoorLocked())
			innerDoorStateCache = LOCKED;
		else {
			innerDoorStateCache = UNLOCKED;
		}
		// Create innerDoorStateLabel
		innerDoorStateLabel = labelGrid.addTextField(Msg.getString("TabPanelEVA.innerDoor.state"),
										   innerDoorStateCache, null);

		// Create outerDoorLabel
		outerDoorLabel = labelGrid.addTextField(Msg.getString("TabPanelEVA.outerDoor.number"),
											Integer.toString(vehicleAirlock.getNumAwaitingOuterDoor()), null);

		if (vehicleAirlock.isOuterDoorLocked())
			outerDoorStateCache = LOCKED;
		else {
			outerDoorStateCache = UNLOCKED;
		}
		// Create outerDoorStateLabel
		outerDoorStateLabel = labelGrid.addTextField(Msg.getString("TabPanelEVA.outerDoor.state"),
										   outerDoorStateCache, null);

		// Create occupiedLabel
		occupiedLabel = labelGrid.addTextField(Msg.getString("TabPanelEVA.occupied"),
											Integer.toString(vehicleAirlock.getNumInChamber()), null);

		// Create airlockModeLabel
		airlockModeLabel = labelGrid.addTextField(Msg.getString("TabPanelEVA.airlock.mode"),
											vehicleAirlock.getAirlockMode().getName(), null);

		// Create emptyLabel
		emptyLabel = labelGrid.addTextField(Msg.getString("TabPanelEVA.empty"),
											Integer.toString(vehicleAirlock.getNumEmptied()), null);

		// Create airlockStateLabel
		airlockStateLabel = labelGrid.addTextField(Msg.getString("TabPanelEVA.airlock.state"),
											vehicleAirlock.getState().toString(), null);

		// Create cycleTimeLabel
		cycleTimeLabel = labelGrid.addTextField(Msg.getString("TabPanelEVA.airlock.cycleTime"),
											StyleManager.DECIMAL_PLACES1.format(vehicleAirlock.getRemainingCycleTime()), null);
		
		// Create transitionLabel
		transitionLabel = labelGrid.addTextField(Msg.getString("TabPanelEVA.airlock.transition"),
				 							Boolean.toString(vehicleAirlock.isTransitioning()), null);

		// Create activationLabel
		activationLabel = labelGrid.addTextField(Msg.getString("TabPanelEVA.airlock.activation"),
				Boolean.toString(vehicleAirlock.isActivated()), null);
		
		// Create OperatorLabel
		operatorLabel = labelGrid.addTextField(Msg.getString("TabPanelEVA.operator"),
				vehicleAirlock.getOperatorName(), null);

		// Create occupant panel
		JPanel occupantPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
		addBorder(occupantPanel, Msg.getString("TabPanelEVA.titledB.occupants"));
		content.add(occupantPanel, BorderLayout.SOUTH);
		
        // Create occupant list
        occupants = new UnitListPanel<>(getDesktop()) {
			@Override
			protected Collection<Person> getData() {
				return getUnitsFromIds(vehicleAirlock.getAllInsideOccupants());
			}
        };
        occupantPanel.add(occupants);
    }

    @Override
    public void update() {

		// Update innerDoorLabel
		int inner = vehicleAirlock.getNumAwaitingInnerDoor();
		if (innerDoorCache != inner) {
			innerDoorCache = inner;
			innerDoorLabel.setText(Integer.toString(inner));
		}

		// Update outerDoorLabel
		int outer = vehicleAirlock.getNumAwaitingOuterDoor();
		if (outerDoorCache != outer) {
			outerDoorCache = outer;
			outerDoorLabel.setText(Integer.toString(outer));
		}

		// Update occupiedLabel
		int numChamber = vehicleAirlock.getNumInChamber();
		if (occupiedCache != numChamber) {
			occupiedCache = numChamber;
			occupiedLabel.setText(Integer.toString(numChamber));
		}

		// Update emptyLabel
		int emptyNumChamber = vehicleAirlock.getNumEmptied();
		if (emptyCache != emptyNumChamber) {
			emptyCache = emptyNumChamber;
			emptyLabel.setText(Integer.toString(emptyNumChamber));
		}

		// Update operatorLabel
		String name = vehicleAirlock.getOperatorName();
		if (!operatorCache.equalsIgnoreCase(name)) {
			operatorCache = name;
			operatorLabel.setText(name);
		}

		// Update airlockStateLabel
		String state = vehicleAirlock.getState().toString();
		if (!airlockStateCache.equalsIgnoreCase(state)) {
			airlockStateCache = state;
			airlockStateLabel.setText(state);
		}
		
		// Update activationLabel
		boolean activated = vehicleAirlock.isActivated();
		if (activationCache != activated) {
			activationCache = activated;
			activationLabel.setText(Boolean.toString(activated));
		}

		// Update activationLabel
		boolean transition = vehicleAirlock.isTransitioning();
		if (transitionCache != transition) {
			transitionCache = transition;
			transitionLabel.setText(Boolean.toString(transition));
		}
		
		// Update airlockModeLabel
		AirlockMode airlockMode = vehicleAirlock.getAirlockMode();
		if (airlockModeCache != airlockMode) {
			airlockModeCache = airlockMode;
			airlockModeLabel.setText(airlockMode.getName());
		}
		
		// Update cycleTimeLabel
		double time = vehicleAirlock.getRemainingCycleTime();
		if (cycleTimeCache != time) {
			cycleTimeCache = time;
			cycleTimeLabel.setText(StyleManager.DECIMAL_PLACES1.format(cycleTimeCache));
		}

		String innerDoorState = "";
		if (vehicleAirlock.isInnerDoorLocked())
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
		if (vehicleAirlock.isOuterDoorLocked())
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
    }

    @Override
    public void destroy() {
        super.destroy();
        
        occupiedLabel = null;
        emptyLabel = null;
        operatorLabel = null;
        airlockStateLabel = null;
        cycleTimeLabel = null;

        occupants = null;

        vehicleAirlock = null;
    }
}