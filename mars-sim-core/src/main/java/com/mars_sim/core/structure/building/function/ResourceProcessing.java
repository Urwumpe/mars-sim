/*
 * Mars Simulation Project
 * ResourceProcessing.java
 * @date 2022-07-11
 * @author Scott Davis
 */
package com.mars_sim.core.structure.building.function;

import com.mars_sim.core.structure.Settlement;
import com.mars_sim.core.structure.building.Building;
import com.mars_sim.core.structure.building.BuildingException;
import com.mars_sim.core.structure.building.FunctionSpec;

/**
 * The ResourceProcessing class is a building function indicating that the
 * building has a set of resource processes.
 */
public class ResourceProcessing extends ResourceProcessor {
	
	private static final long serialVersionUID = 1L;

	// These are a very fragile implementation
	public static final String OLEFIN = "Methanol-to-olefin";
	public static final String SELECTIVE = "Selective Partial Oxidation";
	public static final String SABATIER = "sabatier";
	public static final String REGOLITH = "regolith"; // Do not use "convert regolith to ores with sand"
	public static final String ICE = "melt and filter ice";
	public static final String PPA = "PPA";
	public static final String CFR = "Carbon Formation Reactor";
	public static final String OGS = "Oxygen Generation System";
	
	/**
	 * Constructor.
	 *
	 * @param building the building the function is for.
	 * @param spec Definition of the Function
	 * @throws BuildingException if function cannot be constructed.
	 */
	public ResourceProcessing(Building building, FunctionSpec spec) {
		// Use Function constructor
		super(FunctionType.RESOURCE_PROCESSING, spec, building, buildingConfig.getResourceProcesses(building.getBuildingType()));
	}

	/**
	 * Gets the value of the function for a named building.
	 *
	 * @param type the building name.
	 * @param newBuilding  true if adding a new building.
	 * @param settlement   the settlement.
	 * @return value (VP) of building function.
	 * @throws Exception if error getting function value.
	 */
	public static double getFunctionValue(String type, boolean newBuilding, Settlement settlement) {
		return calculateFunctionValue(settlement, buildingConfig.getResourceProcesses(type));
	}
}
