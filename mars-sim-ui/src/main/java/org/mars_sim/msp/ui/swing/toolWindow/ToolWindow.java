/**
 * Mars Simulation Project
 * ToolWindow.java
 * @version 3.1.0 2017-02-03
 * @author Scott Davis
 */
package org.mars_sim.msp.ui.swing.toolWindow;

import javax.swing.JInternalFrame;
import javax.swing.WindowConstants;

import org.mars_sim.msp.core.Simulation;
import org.mars_sim.msp.core.UnitManager;
import org.mars_sim.msp.core.time.MasterClock;
import org.mars_sim.msp.ui.swing.MainDesktopPane;
import org.mars_sim.msp.ui.swing.tool.commander.CommanderWindow;
import org.mars_sim.msp.ui.swing.tool.monitor.MonitorWindow;

/**
 * The ToolWindow class is an abstract UI window for a tool. Particular tool
 * windows should be derived from this.
 */
public abstract class ToolWindow extends JInternalFrame {

	// Data members

	/** True if window is open. */
	protected boolean opened;
	
	/** The name of the tool the window is for. */
	protected String name;

	/** The main desktop. */
	protected MainDesktopPane desktop;
	protected MonitorWindow monitorWindow;
	protected CommanderWindow commanderWindow;
	// private SingleSelectionModel<?> ssm;

	protected static MasterClock masterClock = Simulation.instance().getMasterClock();
	protected static UnitManager unitManager = Simulation.instance().getUnitManager();

	/**
	 * Constructor.
	 * 
	 * @param name    the name of the tool
	 * @param desktop the main desktop.
	 */
	public ToolWindow(String name, MainDesktopPane desktop) {

		// use JInternalFrame constructor
		super(name, true, // resizable
				true, // closable
				false, // maximizable
				false // iconifiable
		);

		// Initialize data members
		this.name = name;
		this.desktop = desktop;
//		this.mainScene = desktop.getMainScene();

		// Remove title bar
		// putClientProperty("JInternalFrame.isPalette", Boolean.TRUE);
//	    getRootPane().setWindowDecorationStyle(JRootPane.NONE);
//	    BasicInternalFrameUI bi = (BasicInternalFrameUI)super.getUI();
//	    bi.setNorthPane(null);
//	    setBorder(null);

		// getRootPane().setOpaque(false);
		// getRootPane().setBackground(new Color(0,0,0,128));
		// setOpaque(false);
		// setBackground(new Color(0,0,0,128));

		if (this instanceof MonitorWindow)
			this.monitorWindow = (MonitorWindow) this;

		else if (this instanceof CommanderWindow)
			this.commanderWindow = (CommanderWindow) this;
		
		opened = false;

//		if (mainScene != null) {
//			msm = mainScene.getMainSceneMenu();
//			// ssm = mainScene.getJFXTabPane().getSelectionModel();
//		}

		setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
		// setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

		// Set internal frame listener
		addInternalFrameListener(new ToolFrameListener());
	}

	/**
	 * Gets the tool name.
	 * 
	 * @return tool name
	 */
	public String getToolName() {
		return name;
	}

	/**
	 * Sets the tool name.
	 * 
	 * @param tool name
	 */
	public void setTitleName(String value) {
		setName(value);
	}

	/**
	 * Checks if the tool window has previously been opened.
	 * 
	 * @return true if tool window has previously been opened.
	 */
	public boolean wasOpened() {
		return opened;
	}

	/**
	 * Sets if the window has previously been opened.
	 * 
	 * @param opened true if previously opened.
	 */
	public void setWasOpened(boolean opened) {
		this.opened = opened;
	}

	public void closeMaps() {
		// if (desktop.isToolWindowOpen(SettlementWindow.NAME)) {
//		mainScene.closeMaps();
		// }
	}

	/**
	 * Update window.
	 */
	public void update() {

//		if (mainScene != null && !masterClock.isPaused()) {

			if (this.isVisible() || this.isShowing()) {
				// Note: need to update the table color style after the theme is changed
				if (this.getToolName().equals(MonitorWindow.NAME))
					monitorWindow.refreshTableStyle();
					// pack(); // create time lag, and draw artifact
				
				else if (this.getToolName().equals(CommanderWindow.NAME))
					commanderWindow.update();
			}
//
//			else if (!this.isVisible() || !this.isShowing()) {
//				Platform.runLater(() -> {
//					if (msm == null)
//						msm = mainScene.getMainSceneMenu();
//					item = msm.getCheckMenuItem(name);
//					if (item != null) {
//						// Note: need to accommodate if item is a guide window, as it is always null
//						if (item.isSelected()) {
//							msm.uncheckToolWindow(name);
//						}
//					}
//				});
//			}
//		}
	}

	/**
	 * Prepares tool window for deletion.
	 */
	public void destroy() {
		desktop = null;
		masterClock = null;
		monitorWindow = null;
	}
}