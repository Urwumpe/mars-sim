/**
 * Mars Simulation Project
 * EditMissionDialog.java
 * @version 3.08 2015-03-23
 * @author Scott Davis
 */

package org.mars_sim.msp.ui.swing.tool.mission.edit;

import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.ai.mission.*;
import org.mars_sim.msp.core.structure.Settlement;
import org.mars_sim.msp.ui.swing.MainDesktopPane;
import org.mars_sim.msp.ui.swing.MarsPanelBorder;

import javax.swing.*;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Iterator;

/**
 * The edit mission dialog for the mission tool.
 */
public class EditMissionDialog extends JInternalFrame {

	// Private members
	private Mission mission;
	private InfoPanel infoPane;
	protected MainDesktopPane desktop;
	
	/**
	 * Constructor
	 * @param owner the owner frame.
	 * @param mission the mission to edit.
	 */
	public EditMissionDialog(MainDesktopPane desktop, Mission mission) {
		// Use JInternalFrame constructor
        super("Edit Mission", false, true, false, true);
        
		// Initialize data members.
		this.mission = mission;
		this.desktop = desktop;
		
		// Set the layout.
		setLayout(new BorderLayout(0, 0));
		
		// Set the border.
		((JComponent) getContentPane()).setBorder(new MarsPanelBorder());
        
		// Create the info panel.
        infoPane = new InfoPanel(mission, desktop, this);
        add(infoPane, BorderLayout.CENTER);
        
        // Create the button panel.
        JPanel buttonPane = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        add(buttonPane, BorderLayout.SOUTH);
        
        // Create the modify button.
        JButton modifyButton = new JButton("Modify");
        modifyButton.addActionListener(
        		new ActionListener() {
        			public void actionPerformed(ActionEvent e) {
        				// Commit the mission modification and close dialog.
        				modifyMission();
        				dispose();
        			}
				});
        buttonPane.add(modifyButton);
        
        // Create the cancel button.
        JButton cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(
				new ActionListener() {
        			public void actionPerformed(ActionEvent e) {
        				// Close the dialog.
        				dispose();
        			}
				});
        buttonPane.add(cancelButton);
		
		// Finish and display dialog.
		//pack();
		//setLocationRelativeTo(owner);
		//setResizable(false);
		//setVisible(true);
		
	    desktop.add(this);	    
	    
        setSize(new Dimension(700, 550));
		Dimension desktopSize = desktop.getParent().getSize();
	    Dimension jInternalFrameSize = this.getSize();
	    int width = (desktopSize.width - jInternalFrameSize.width) / 2;
	    int height = (desktopSize.height - jInternalFrameSize.height) / 2;
	    setLocation(width, height);
	    setVisible(true);
	    		
	}
	
	/**
	 * Commit the modification of the mission.
	 */
	private void modifyMission() {
		// Set the mission description.
		mission.setDescription(infoPane.descriptionField.getText());
		
		// Change the mission's action.
		setAction((String) infoPane.actionDropDown.getSelectedItem());
		
		// Set mission members.
		setMissionMembers();
	}
	
	/**
	 * Sets the mission action.
	 * @param action the action string.
	 */
	private void setAction(String action) {
		if (action.equals(InfoPanel.ACTION_CONTINUE)) endCollectionPhase();
		else if (action.equals(InfoPanel.ACTION_HOME)) returnHome();
		else if (action.equals(InfoPanel.ACTION_NEAREST)) goToNearestSettlement();
	}
	
	/**
	 * End the mission collection phase at the current site.
	 */
	private void endCollectionPhase() {
		if (mission instanceof CollectResourcesMission) 
			((CollectResourcesMission) mission).endCollectingAtSite();
	}
	
	/**
	 * Have the mission return home and end collection phase if necessary.
	 */
	private void returnHome() {
		if (mission instanceof TravelMission) {
			TravelMission travelMission = (TravelMission) mission;
//			try {
				int offset = 2;
				if (travelMission.getPhase().equals(VehicleMission.TRAVELLING)) offset = 1;
				travelMission.setNextNavpointIndex(travelMission.getNumberOfNavpoints() - offset);
				travelMission.updateTravelDestination();
				endCollectionPhase();
//			}
//			catch (MissionException e) {}
		}
	}
	
	/**
	 * Go to the nearest settlement and end collection phase if necessary.
	 */
	private void goToNearestSettlement() {
		if (mission instanceof VehicleMission) {
			VehicleMission vehicleMission = (VehicleMission) mission;
			try {
				Settlement nearestSettlement = vehicleMission.findClosestSettlement();
				if (nearestSettlement != null) {
					vehicleMission.clearRemainingNavpoints();
		    		vehicleMission.addNavpoint(new NavPoint(nearestSettlement.getCoordinates(), nearestSettlement, 
		    				nearestSettlement.getName()));
		    		vehicleMission.associateAllMembersWithSettlement(nearestSettlement);
		    		vehicleMission.updateTravelDestination();
		    		endCollectionPhase();
				}
			}
			catch (Exception e) {}
		}
	}
	
	/**
	 * Sets the mission members.
	 */
	private void setMissionMembers() {
		// Add new members.
		for (int x = 0; x < infoPane.memberListModel.size(); x++) {
			Person person = (Person) infoPane.memberListModel.elementAt(x);
			if (!mission.hasPerson(person)) person.getMind().setMission(mission);
		}
		
		// Remove old members.
		Iterator<Person> i = mission.getPeople().iterator();
		while (i.hasNext()) {
			Person person = i.next();
			if (!infoPane.memberListModel.contains(person)) person.getMind().setMission(null);
		}
	}
}