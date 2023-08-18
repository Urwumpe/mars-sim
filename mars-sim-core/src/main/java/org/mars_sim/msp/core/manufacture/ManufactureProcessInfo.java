/*
 * Mars Simulation Project
 * ManufactureProcessInfo.java
 * @date 2022-07-26
 * @author Scott Davis
 */

package org.mars_sim.msp.core.manufacture;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.mars_sim.msp.core.resource.AmountResource;
import org.mars_sim.msp.core.resource.ItemResource;
import org.mars_sim.msp.core.resource.ItemResourceUtil;
import org.mars_sim.msp.core.resource.ResourceUtil;

/**
 * Information about a type of manufacturing process.
 */
public class ManufactureProcessInfo implements Serializable, Comparable<ManufactureProcessInfo> {

	/** default serial id. */
	private static final long serialVersionUID = 1L;

	// Data members
	private String name;
	private String description;
	
	private int techLevelRequired;
	private int skillLevelRequired;
	private int effortLevel = 2;
	
	private double workTimeRequired;
	private double processTimeRequired;
	private double powerRequired;
	
	private List<ManufactureProcessItem> inputList = new ArrayList<>();
	private List<ManufactureProcessItem> outputList = new ArrayList<>();
	
	/*
	 * Constructor 1.
	 */
	public ManufactureProcessInfo() {
	}
	
	/*
	 * Copy constructor.
	 */
	public ManufactureProcessInfo(ManufactureProcessInfo another) {
	    this.name = another.name;
	    this.description = another.description;
	    this.techLevelRequired = another.techLevelRequired;
	    this.skillLevelRequired = another.skillLevelRequired;
	    this.effortLevel = another.effortLevel;
	    this.workTimeRequired = another.workTimeRequired;
	    this.processTimeRequired = another.processTimeRequired;
	    this.powerRequired = another.powerRequired;
		
	    // Warning: below is shallow copy only, NOT deep copy 
	    this.inputList = List.copyOf(another.inputList);
	    this.outputList = List.copyOf(another.outputList);
	}
	
	/**
	 * Gets the process name.
	 * 
	 * @return name.
	 */
	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	/**
	 * Sets the process name.
	 * 
	 * @param name the name.
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Sets the process description.
	 * 
	 * @param description {@link String}
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * Gets the manufacturing tech level required for the process.
	 * 
	 * @return tech level.
	 */
	public int getTechLevelRequired() {
		return techLevelRequired;
	}

	/**
	 * Sets the manufacturing tech level required for the process.
	 * 
	 * @param techLevelRequired the required tech level.
	 */
	public void setTechLevelRequired(int techLevelRequired) {
		this.techLevelRequired = techLevelRequired;
	}

	/**
	 * Gets the material science skill level required to work on the process.
	 * 
	 * @return skill level.
	 */
	public int getSkillLevelRequired() {
		return skillLevelRequired;
	}

	public int getEffortLevel() {
		return effortLevel;
	}
	
	/**
	 * Sets the material science skill level required to work on the process.
	 * 
	 * @param skillLevelRequired skill level.
	 */
	public void setSkillLevelRequired(int skillLevelRequired) {
		this.skillLevelRequired = skillLevelRequired;
	}

	/**
	 * Gets the work time required to complete the process.
	 * 
	 * @return work time (millisols).
	 */
	public double getWorkTimeRequired() {
		return workTimeRequired;
	}

	/**
	 * Sets the work time required to complete the process.
	 * 
	 * @param workTimeRequired work time (millisols).
	 */
	public void setWorkTimeRequired(double workTimeRequired) {
		this.workTimeRequired = workTimeRequired;
	}

	/**
	 * Gets the process time required to complete the process.
	 * 
	 * @return process time (millisols).
	 */
	public double getProcessTimeRequired() {
		return processTimeRequired;
	}

	/**
	 * Sets the process time required to complete the process.
	 * 
	 * @param processTimeRequired process time (millisols).
	 */
	public void setProcessTimeRequired(double processTimeRequired) {
		this.processTimeRequired = processTimeRequired;
	}

	/**
	 * Gets the power required for the process.
	 * 
	 * @return power (kW hr).
	 */
	public double getPowerRequired() {
		return powerRequired;
	}

	/**
	 * Sets the power required for the process.
	 * 
	 * @param powerRequired power (kW hr).
	 */
	public void setPowerRequired(double powerRequired) {
		this.powerRequired = powerRequired;
	}

	/**
	 * Gets a list of the input items required for the process.
	 * 
	 * @return input items.
	 */
	public List<ManufactureProcessItem> getInputList() {
		return inputList;
	}

	/**
	 * Sets the list of the input items required for the process.
	 * 
	 * @param inputList the input items.
	 */
	public void setInputList(List<ManufactureProcessItem> inputList) {
		this.inputList = inputList;
	}

	/**
	 * Gets a list of the output items produced by the process.
	 * 
	 * @return output items.
	 */
	public List<ManufactureProcessItem> getOutputList() {
		if (outputList == null)
			return new ArrayList<>();
		return outputList;
	}

	/**
	 * Gets a list of ManufactureProcessItem having the given output resource name
	 * 
	 * @param name
	 * @return
	 */
	public List<ManufactureProcessItem> getManufactureProcessItem(String name) {
		if (outputList == null)
			return new ArrayList<>();
		List<ManufactureProcessItem> list = new ArrayList<>();
		for (ManufactureProcessItem item : outputList) {
			if (name.equalsIgnoreCase(item.getName()))
				list.add(item);
		}
		return list;
	}
	
	/**
	 * Gives back a list of strings of the output items' names.
	 * 
	 * @return {@link List}<{@link String}>
	 */
	public List<String> getOutputNames() {
		if (outputList == null)
			return new ArrayList<>();
		List<String> list = new ArrayList<>();
		for (ManufactureProcessItem item : outputList) {
			list.add(item.getName());
		}
		return list;
	}

	/**
	 * Gives back a list of strings of the input items' names.
	 * 
	 * @return {@link List}<{@link String}>
	 */
	public List<String> getInputNames() {
		if (inputList == null)
			return new ArrayList<>();
		List<String> list = new ArrayList<>();
		for (ManufactureProcessItem item : inputList) {
			list.add(item.getName());
		}
		return list;
	}

	/**
	 * Sets the list of the output items produced by the process.
	 * 
	 * @param outputList the output items.
	 */
	public void setOutputList(List<ManufactureProcessItem> outputList) {
		this.outputList = outputList;
	}

	/**
	 * Calculates the total input mass.
	 * 
	 * @return
	 */
	public double calculateTotalInputMass() {
		double mass = 0;
		
		for (ManufactureProcessItem item : inputList) {
			String name = item.getName();
	
			AmountResource ar = ResourceUtil.findAmountResource(name);
			if (ar != null) {
				mass += item.getAmount();
			}		
			else {
				ItemResource ir = ItemResourceUtil.findItemResource(name);
				if (ir != null) {
					double quantity = item.getAmount();
					mass += quantity * ir.getMassPerItem();
				}
//				else {
//					BinType type = BinType.convertName2Enum(name);
//					if (type != null) {
//						double quantity = item.getAmount();
//						// Will need to find getMassPerBin
//						mass += quantity * 2;
//					}
//				}
			}
		}
		
		return mass;
	}
	
	/**
	 * Calculates the quantity of a output product.
	 * 
	 * @return
	 */
	public double calculateOutputQuantity(String productName) {
		for (ManufactureProcessItem item : outputList) {
			String name = item.getName();
			if (productName.equalsIgnoreCase(name)) {
				return item.getAmount();
			}
		}
		
		return 1;
	}
	
	
	
	@Override
	public String toString() {
		return name;
	}

	/**
	 * Compares this object with the specified object for order.
	 * 
	 * @param o the Object to be compared.
	 * @return a negative integer, zero, or a positive integer as this object is
	 *         less than, equal to, or greater than the specified object.
	 */
	public int compareTo(ManufactureProcessInfo p) {
		return name.compareToIgnoreCase(p.name);
	}

	/**
	 * Prepares object for garbage collection.
	 */
	public void destroy() {
		if (inputList != null)
			inputList.clear();
		inputList = null;
		if (outputList != null)
			outputList.clear();
		outputList = null;
	}
}
