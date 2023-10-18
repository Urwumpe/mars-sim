/*
 * Mars Simulation Project
 * ManufactureProcessItem.java
 * @date 2023-06-12
 * @author Scott Davis
 */

package com.mars_sim.core.manufacture;

import java.io.Serializable;

import com.mars_sim.core.resource.ItemType;

/**
 * A manufacturing process input or output item.
 */
public class ManufactureProcessItem implements Serializable {

	/** default serial id. */
	private static final long serialVersionUID = 1L;

	// Data members
	private String name;
	private ItemType type;
	private double amount;

	public void setName(String name) {
		this.name = name;
	}
	
	public String getName() {
		return name;
	}
	
	
	public ItemType getType() {
		return type;
	}

	public void setType(ItemType type) {
		this.type = type;
	}

	public double getAmount() {
		return amount;
	}

	public void setAmount(double amount) {
		this.amount = amount;
	}

	@Override
	public String toString() {
		return name;
	}

	/**
	 * Checks if another object is equal to this one.
	 */
	public boolean equals(Object object) {
		boolean result = false;
		if (object instanceof ManufactureProcessItem) {
			ManufactureProcessItem item = (ManufactureProcessItem) object;
			result = true;
			if (!name.equals(item.getName())) {
				result = false;
			} else if (!type.equals(item.getType())) {
				result = false;
			} else if (amount != item.getAmount()) {
				result = false;
			}
		}

		return result;
	}

	/**
	 * Gets the hash code for this object.
	 * 
	 * @return hash code.
	 */
	public int hashCode() {
		StringBuffer buff = new StringBuffer("");
		buff.append(name);
		buff.append(type);
		buff.append(amount);
		return buff.hashCode();
	}
}
