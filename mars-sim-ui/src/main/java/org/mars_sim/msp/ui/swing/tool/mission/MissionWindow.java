/**
 * Mars Simulation Project
 * MissionWindow.java
 * @version 3.2.0 2021-06-20
 * @author Scott Davis
 */
package org.mars_sim.msp.ui.swing.tool.mission;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JList;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.mars_sim.msp.core.CollectionUtils;
import org.mars_sim.msp.core.person.ai.mission.Mission;
import org.mars_sim.msp.core.structure.Settlement;
import org.mars_sim.msp.ui.swing.MainDesktopPane;
import org.mars_sim.msp.ui.swing.tool.mission.create.CreateMissionWizard;
import org.mars_sim.msp.ui.swing.toolWindow.ToolWindow;

import com.alee.laf.button.WebButton;
import com.alee.laf.panel.WebPanel;
import com.alee.laf.scroll.WebScrollPane;
import com.alee.laf.tabbedpane.WebTabbedPane;

/**
 * Window for the mission tool.
 */
@SuppressWarnings("serial")
public class MissionWindow extends ToolWindow implements ListSelectionListener {

	/** Tool name. */
	public static final String NAME = "Mission Tool";
	public static final int WIDTH = 640;
	public static final int HEIGHT = 640;
	
	// Private members
	private WebTabbedPane tabPane;
	private JList<Settlement> settlementList;
	private JList<Mission> missionList;

	private SettlementListModel settlementListModel;
	private MissionListModel missionListModel;
	
	private Settlement settlement;
	private Mission mission;
	
	private NavpointPanel navpointPane;

	private CreateMissionWizard createMissionWizard;
//	private EditMissionDialog editMissionDialog;

	/**
	 * Constructor.
	 * 
	 * @param desktop {@link MainDesktopPane} the main desktop panel.
	 */
	public MissionWindow(MainDesktopPane desktop) {

		// Use ToolWindow constructor
		super(NAME, desktop);

		// Create content panel.
		WebPanel mainPane = new WebPanel(new BorderLayout());
		mainPane.setBorder(MainDesktopPane.newEmptyBorder());
		setContentPane(mainPane);

		// Create the left panel.
		WebPanel leftPane = new WebPanel(new BorderLayout());
		mainPane.add(leftPane, BorderLayout.WEST);
		
		// Create the settlement list panel.
		WebPanel settlementListPane = new WebPanel(new BorderLayout());
		settlementListPane.setPreferredSize(new Dimension(200, 200));
		leftPane.add(settlementListPane, BorderLayout.NORTH);
		
		// Create the mission list panel.
		WebPanel missionListPane = new WebPanel(new BorderLayout());
		missionListPane.setPreferredSize(new Dimension(200, 400));
		leftPane.add(missionListPane, BorderLayout.CENTER);

		// Create the settlement list.
		settlementListModel = new SettlementListModel();
		settlementList = new JList<Settlement>(settlementListModel);
		settlementList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		settlementListPane.add(new WebScrollPane(settlementList), BorderLayout.CENTER);
		settlementList.addListSelectionListener(this);
		
		// Create the mission list.
		missionListModel = new MissionListModel(this);
		missionList = new JList<Mission>(missionListModel);
		missionList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		missionListPane.add(new WebScrollPane(missionList), BorderLayout.CENTER);
	
		// Create the info tab panel.
		tabPane = new WebTabbedPane();
		mainPane.add(tabPane, BorderLayout.CENTER);

		// Create the main detail panel.
		MainDetailPanel infoPane = new MainDetailPanel(desktop, this);
		missionList.addListSelectionListener(infoPane);
		tabPane.add("Info", infoPane);

		// Create the navpoint panel.
		navpointPane = new NavpointPanel(desktop);
		missionList.addListSelectionListener(navpointPane);
		tabPane.add("Navigation", navpointPane);

		// Create the button panel.
		WebPanel buttonPane = new WebPanel(new FlowLayout());
		mainPane.add(buttonPane, BorderLayout.SOUTH);

		// Create the create mission button.
		WebButton createButton = new WebButton("Create New Mission");
		createButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// Create new mission.
				createNewMission();
			}
		});
		buttonPane.add(createButton);

		// Create the edit mission button.
		final WebButton editButton = new WebButton("Modify Mission");
		editButton.setEnabled(false);

//		editButton.addActionListener(
//				new ActionListener() {
//					public void actionPerformed(ActionEvent e) {
//						// Edit the mission.
//						mission = (Mission) missionList.getSelectedValue();
//						if (mission != null) editMission(mission);
//					}
//				});
//		missionList.addListSelectionListener(
//			new ListSelectionListener() {
//				public void valueChanged(ListSelectionEvent e) {
//					// Enable button if mission is selected in list.
//					editButton.setEnabled(missionList.getSelectedValue() != null);
//				}
//			}
//		);

		buttonPane.add(editButton);

		// Create the abort mission button.
		final WebButton abortButton = new WebButton("Abort Mission");
		abortButton.setEnabled(false);

//		abortButton.addActionListener(
//				new ActionListener() {
//					public void actionPerformed(ActionEvent e) {
//						// End the mission.
//						mission = (Mission) missionList.getSelectedValue();
//						if (mission != null) endMission(mission);
//					}
//				});
//		missionList.addListSelectionListener(
//				new ListSelectionListener() {
//					public void valueChanged(ListSelectionEvent e) {
//						abortButton.setEnabled(missionList.getSelectedValue() != null);
//					}
//				});

		buttonPane.add(abortButton);

		setSize(new Dimension(WIDTH, HEIGHT));
		setMaximizable(true);
		setResizable(false);

		setVisible(true);
		// pack();

		Dimension desktopSize = desktop.getSize();
		Dimension jInternalFrameSize = this.getSize();
		int width = (desktopSize.width - jInternalFrameSize.width) / 2;
		int height = (desktopSize.height - jInternalFrameSize.height) / 2;
		setLocation(width, height);

	}

	/**
	 * Selects a mission for display.
	 * 
	 * @param mission the mission to select.
	 */
	public void selectMission(Mission mission) {	
		// when clicking elsewhere to open up the Mission Tool
		Settlement s = mission.getAssociatedSettlement();
		if (s == null) {
			// Since the mission is completed, use the recorded settlement name 
			// to get back the settlement instance
			s = CollectionUtils.findSettlement(mission.getSettlmentName());
		}
		
		// Call selectSettlement() to highlight the mission
		selectSettlement(s);
		
//		if (this.mission == null || !missionListModel.containsMission(mission)
//				|| !this.mission.equals(mission)) {
			this.mission = mission;			
			// Call setSelectedValue() to highlight the mission
//			if (missionList.getSelectedValue() != null 
//					&& !missionList.getSelectedValue().equals(mission))
				missionList.setSelectedValue(mission, true);
//			System.out.println("Which one was selected ? " + missionList.getSelectedValue());
//			System.out.println("selectMission() : " + mission);
//		}
	}

	/**
	 * Selects a mission for display.
	 * 
	 * @param mission the mission to select.
	 */
	public void selectSettlement(Settlement settlement) {
//		if (settlement != null 
//				&& (this.settlement == null 
//				|| !settlementListModel.containsSettlement(settlement)
//				|| !this.settlement.equals(settlement))) {
			this.settlement = settlement;
			// Call setSelectedValue to highlight the settlement
//			if (settlementList.getSelectedValue() != null 
//					&& !settlementList.getSelectedValue().equals(settlement))
				settlementList.setSelectedValue(settlement, true);

//			System.out.println("selectSettlement() : " + settlement);
			// List all the missions under this settlement
			// Note that this.settlement is also equal to missionWindow.getSettlement()
//			if (this.settlement != null)
				missionListModel.populateMissions();
//		}
				
		// Update Nav tab's map
		navpointPane.updateCoords(settlement.getCoordinates());
	}

	/**
	 * Open wizard to create a new mission.
	 */
	private void createNewMission() {
		createMissionWizard = new CreateMissionWizard(desktop, this);
	}
	
	@Override
	public void valueChanged(ListSelectionEvent e){
		// when clicking on a settlement in the Mission Tool
		if ((JList<?>) e.getSource() == settlementList) {
			this.settlement = settlementList.getSelectedValue();
//			System.out.println("valueChanged() : " + settlement);
		    // List all the missions under this settlement
			// Note that the settlement is also equal to missionWindow.getSettlement()
			if (this.settlement != null) {
				missionListModel.populateMissions();
				
				// Update Nav tab's map
				navpointPane.updateCoords(settlement.getCoordinates());	
			}
		}
	}
	    
//	/**
//	 * Open wizard to edit a mission.
//	 * @param mission the mission to edit.
//	 */
//	private void editMission(Mission mission) {
//
//		if (ms != null)  {
//			// Track the current pause state
//			boolean previous = ms.startPause();
//
//			editMissionDialog = new EditMissionDialog(desktop, mission, this);
//
//			ms.endPause(previous);
//
//		} else
//
//			editMissionDialog = new EditMissionDialog(desktop, mission, this);
//	}

//	/**
//	 * Ends the mission.
//	 * @param mission the mission to end.
//	 */
//	private void endMission(Mission mission) {
//		//logger.info("End mission: " + mission.getName());
//		mission.endMission(Mission.USER_ABORTED_MISSION);
//		repaint();
//	}

	public CreateMissionWizard getCreateMissionWizard() {
		return createMissionWizard;
	}

	public MainDesktopPane getDesktop() {
		return desktop;
	}

	public boolean isNavPointsMapTabOpen() {
		if (tabPane.getSelectedIndex() == 1)
			return true;
		else
			return false;
	}

	public Settlement getSettlement() {
		return settlement;
	}
	
	public Mission getMission() {
		return mission;
	}
	
	public void selectFirstIndex() {
		missionList.setSelectedIndex(0);
	}
	
	/**
	 * Prepares tool window for deletion.
	 */
	@Override
	public void destroy() {
		missionList.clearSelection();
		missionListModel.destroy();
		settlementList.clearSelection();
		settlementListModel.destroy();
		navpointPane.destroy();
	}
}
