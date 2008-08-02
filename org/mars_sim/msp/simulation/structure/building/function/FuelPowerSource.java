/**
 * Mars Simulation Project
 * StandardPowerSource.java
 * @version 2.85 26.7.2008
 * @author Sebastien Venot
 */
package org.mars_sim.msp.simulation.structure.building.function;

import java.io.Serializable;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.mars_sim.msp.simulation.Inventory;
import org.mars_sim.msp.simulation.InventoryException;
import org.mars_sim.msp.simulation.resource.AmountResource;
import org.mars_sim.msp.simulation.resource.ResourceException;
import org.mars_sim.msp.simulation.structure.building.Building;


public class FuelPowerSource extends PowerSource implements Serializable {

    private static final long serialVersionUID = 1L;
    
    private static String CLASS_NAME = 
    "org.mars_sim.msp.simulation.structure.building.function.FuelPowerSource";
	
    private static Logger logger = Logger.getLogger(CLASS_NAME);
    
    private final static String TYPE = "Fuel Power Source";
    private boolean toggle = false;

    
    //A fuelpower source works only with one kind of fuel
    //similar to cars
    private AmountResource resource;
    private double consumptionSpeed;
  

    /**
     * @param type
     * @param maxPower
     */
    public FuelPowerSource(double _maxPower, boolean _toggle, 
	    String fuelType, double _consumptionSpeed) {
	super(TYPE, _maxPower);
	consumptionSpeed = _consumptionSpeed;
	toggle = _toggle;
	
	
	try {
	    resource = AmountResource.findAmountResource(fuelType);
	} catch (ResourceException e) {
	    logger.log(Level.SEVERE, "Could not get fuel resource", e);
	}
    }

    /* 
     * 
     */
    @Override
    public double getCurrentPower(Building building) {
	try {
	    if(isToggleON()) {
	        double fuelStored = building.getInventory().getAmountResourceStored(resource);
	        if(fuelStored > 0) {
	    	return getMaxPower();
	        } else {
	    	return 0;
	        }
	    } else {
	        return 0;
	    }
	} catch (InventoryException e) {
	    logger.log(Level.SEVERE, "Issues when getting power frong fuel source", e);
	    return 0;
	}
    }
    
    public void toggleON() {
	toggle = true;
    }
    
    public void toggleOFF() {
	toggle = false;
    }
    
    public boolean isToggleON() {
	return toggle;
    }
    
    
    public void consumeFuel(double time, Inventory inv) {
	try {
	    double consumptionRateMillisol = consumptionSpeed / 1000D;
	    double consumedFuel = time  * consumptionRateMillisol; 
	    double fuelStored = inv.getAmountResourceStored(resource);
	    
	    if (fuelStored < consumedFuel) {
		consumedFuel = fuelStored;
	    }
	    
	    inv.retrieveAmountResource(resource, consumedFuel);
	} catch (InventoryException e) {
	   logger.log(Level.SEVERE, "Issues when consuming fuel", e);
	}
	
    }
}
