/*
 * Mars Simulation Project
 * MicroInventory.java
 * @date 2021-10-10
 * @author Manny Kung
 */
package org.mars_sim.msp.core.data;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.mars_sim.msp.core.Unit;
import org.mars_sim.msp.core.UnitType;
import org.mars_sim.msp.core.logging.SimLogger;
import org.mars_sim.msp.core.resource.ResourceUtil;

/**
 * The MicroInventory class represents a simple resource storage solution.
 */
public class MicroInventory implements Serializable {

	/** default serial id. */
	private static final long serialVersionUID = 1L;

	/* default logger. */
	private static final SimLogger logger = SimLogger.getLogger(MicroInventory.class.getName());

	public static final int o2 = ResourceUtil.oxygenID; 
	public static final int h2o = ResourceUtil.waterID;
	public static final int co2 = ResourceUtil.co2ID;
	
	private Unit owner;
	/** A map of resources with quantity. */
	private Map<Integer, Double> quantityMap = new HashMap<>();
	/** A map of resources with capacity. */
	private Map<Integer, Double> capacityMap = new HashMap<>();
	
	public MicroInventory(Unit owner) {
		this.owner = owner;
	}
	
	public Unit getOwner() {
		return owner;
	}
	
	/**
	 * Gets the amount in quantity of a particular resource
	 * 
	 * @param resource
	 * @return quantity
	 */
	public double getQuantity(int resource) {
		if (quantityMap.containsKey(resource))
			return quantityMap.get(resource);
		return 0;
	}
	
	/**
	 * Sets the amount in quantity of a particular resource
	 * 
	 * @param resource
	 * @param quantity
	 */
	public void setQuantity(int resource, double quantity) {
		quantityMap.put(resource, quantity);
	}
	
	/**
     * Gets the capacity of this resource that this container can hold.
     * @return capacity (kg).
     */
    public double getCapacity(int resource) {
		if (capacityMap.containsKey(resource)) {
			return capacityMap.get(resource);
		}
		return 0;
    }
    
	/**
	 * Sets the capacity of a particular resource
	 * 
	 * @param resource
	 * @param capacity
	 */
	public void setCapacity(int resource, double capacity) {
		capacityMap.put(resource, capacity);
	}
	 
	/**
	 * Gets the total weight of the stored resources
	 * 
	 * @return mass [kg]
	 */
	public double getStoredMass() {
		double total = 0;
		for (double m: quantityMap.values()) {
			total += m;
		}
		return total;
	}
	
	/**
	 * Is this suit empty ?
	 * 
	 * @return
	 */
	public boolean isEmpty() {
		for (double m: quantityMap.values()) {
			if (m > 0)
				return false;
		}
		return true;
	}

	/**
	 * Is this suit empty of this resource ?
	 * 
	 * @param resource
	 * @return
	 */
	public boolean isEmpty(int resource) {
		if (quantityMap.containsKey(resource)
				&& quantityMap.get(resource) > 0
				) {
			return false;
		}
		return true;
	}
	
	/**
	 * Stores the resource
	 * 
	 * @param resource
	 * @param quantity
	 * @return excess quantity that cannot be stored
	 */
	public double storeAmountResource(int resource, double quantity) {
		if (quantity == 0)
			return 0;
		double newQ = getQuantity(resource) + quantity;
		double cap = getCapacity(resource);
		if (newQ > cap) {
			String name = owner.findAmountResourceName(resource);
			double excess = newQ - cap;
			logger.warning(owner, "Storage is full. Excess " + Math.round(excess * 1_000.0)/1_000.0 + " kg " + name + ".");
			setQuantity(resource, cap);
			return excess;
		}
		else {
			setQuantity(resource, newQ);
//			System.out.println("microI's storeAR. " + " resource: " + resource + " quantity: " + getQuantity(resource));
			return 0;
		}
	}
	
	/**
	 * Retrieves the resource 
	 * 
	 * @param resource
	 * @param quantity
	 * @return quantity that cannot be retrieved
	 */
	public double retrieveAmountResource(int resource, double quantity) {
		double q = getQuantity(resource);
		double diff = q - quantity;
		if (diff < 0) {
			String name = owner.findAmountResourceName(resource);
			logger.warning(owner, 10_000L, "Just retrieved all " + q + " kg of " 
					+ name + ". Lacking " + Math.round(-diff * 10.0)/10.0 + " kg.");
			setQuantity(resource, 0);
			return diff;
		}
		else {
			setQuantity(resource, diff);
			return 0;
		}
	}
	
	/**
	 * Gets the capacity of a particular amount resource
	 * 
	 * @param resource
	 * @return capacity
	 */
	public double getAmountResourceCapacity(int resource) {
		return getCapacity(resource);
	}
	
	/**
	 * Obtains the remaining storage space of a particular amount resource
	 * 
	 * @param resource
	 * @return quantity
	 */
	public double getAmountResourceRemainingCapacity(int resource) {
		return getCapacity(resource) - getQuantity(resource);
	}
	
	/**
	 * Gets the amount resource stored
	 * 
	 * @param resource
	 * @return quantity
	 */
	public double getAmountResourceStored(int resource) {
		return getQuantity(resource);
	}

	/**
     * Gets the total capacity of resource that this container can hold.
     * @return total capacity (kg).
     */
    public double getTotalCapacity() {
        return owner.getTotalCapacity();
    }
    
	/**
	 * Clean this container for future use
	 */
	public void clean() {
		capacityMap.clear();
		quantityMap.clear();
	}
	
	public void destroy() {
		capacityMap = null;
		quantityMap = null;
	}
}
