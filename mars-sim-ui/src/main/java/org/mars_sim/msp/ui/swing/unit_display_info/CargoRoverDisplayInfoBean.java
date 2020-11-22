/**
 * Mars Simulation Project
 * TransportRoverDisplayInfoBean.java
 * @version 3.1.2 2020-09-02
 * @author Scott Davis
 */

package org.mars_sim.msp.ui.swing.unit_display_info;

import org.mars_sim.msp.ui.swing.ImageLoader;

import javax.swing.*;

/**
 * Provides display information about a cargo rover.
 */
class CargoRoverDisplayInfoBean extends RoverDisplayInfoBean {
	
    // Data members
    private Icon buttonIcon = ImageLoader.getIcon("CargoRoverIcon", ImageLoader.VEHICLE_ICON_DIR);
    
    /**
     * Constructor
     */
    CargoRoverDisplayInfoBean() {
        super();
//        buttonIcon = ImageLoader.getIcon("CargoRoverIcon", ImageLoader.VEHICLE_ICON_DIR);
    }
    
    /** 
     * Gets icon for unit button.
     * @return icon
     */
    public Icon getButtonIcon() {
        return buttonIcon;
    }
}
