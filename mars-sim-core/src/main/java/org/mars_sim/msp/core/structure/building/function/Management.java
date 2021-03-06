/**
 * Mars Simulation Project
 * Management.java
 * @version 3.1.2 2020-09-02
 * @author Scott Davis
 */
package org.mars_sim.msp.core.structure.building.function;

import java.io.Serializable;
import java.util.Iterator;

import org.mars_sim.msp.core.structure.Settlement;
import org.mars_sim.msp.core.structure.building.Building;

/**
 * A management building function.  The building facilitates management
 * of a settlement population.
 */
public class Management
extends Function
implements Serializable {

    /** default serial id. */
    private static final long serialVersionUID = 1L;

    // Data members
    private int populationSupport;

    /**
     * Constructor.
     * @param building the building this function is for.
     */
    public Management(Building building) {
        // Use Function constructor.
        super(FunctionType.MANAGEMENT, building);
        // Populate data members.
        populationSupport = buildingConfig.getManagementPopulationSupport(building.getBuildingType());
        // Load activity spots
        loadActivitySpots(buildingConfig.getManagementActivitySpots(building.getBuildingType()));
    }

    /**
     * Gets the value of the function for a named building.
     * 
     * @param buildingName the building name.
     * @param newBuilding true if adding a new building.
     * @param settlement the settlement.
     * @return value (VP) of building function.
     */
    public static double getFunctionValue(String buildingName, boolean newBuilding,
            Settlement settlement) {

        // Settlements need enough management buildings to support population.
        double demand = settlement.getNumCitizens();

        // Supply based on wear condition of buildings.
        double supply = 0D;
        Iterator<Building> i = settlement.getBuildingManager().getBuildings(FunctionType.MANAGEMENT).iterator();
        while (i.hasNext()) {
            Building managementBuilding = i.next();
            Management management = managementBuilding.getManagement();
            double populationSupport = management.getPopulationSupport();
            double wearFactor = ((managementBuilding.getMalfunctionManager().getWearCondition() / 100D) * .75D) + .25D;
            supply += populationSupport * wearFactor;
        }

        if (!newBuilding) {
//            BuildingConfig config = SimulationConfig.instance().getBuildingConfiguration();
            supply -= buildingConfig.getManagementPopulationSupport(buildingName);
            if (supply < 0D) supply = 0D;
        }

        return demand / (supply + 1D);
    }

    /**
     * Gets the number of people this management facility can support.
     * @return population that can be supported.
     */
    public int getPopulationSupport() {
        return populationSupport;
    }

    @Override
    public double getMaintenanceTime() {
        return populationSupport * 1D;
    }
}
