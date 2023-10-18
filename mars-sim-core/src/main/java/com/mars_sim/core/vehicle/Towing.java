/**
 * Mars Simulation Project
 * Towing.java
 * @version 3.2.0 2021-06-20
 * @author Scott Davis
 */

package com.mars_sim.core.vehicle;

public interface Towing {

	/**
	 * Sets the vehicle this rover is currently towing.
	 * 
	 * @param towedVehicle the vehicle being towed.
	 */
	public void setTowedVehicle(Vehicle towedVehicle);

	/**
	 * Gets the vehicle this rover is currently towing.
	 * 
	 * @return towed vehicle.
	 */
	public Vehicle getTowedVehicle();
}
