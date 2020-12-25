/**
 * Mars Simulation Project
 * Recreation.java
 * @version 3.1.2 2020-09-02
 * @author Scott Davis
 */
package org.mars_sim.msp.core.structure.building.function;

import java.io.Serializable;
import java.util.Iterator;

import org.mars_sim.msp.core.structure.Settlement;
import org.mars_sim.msp.core.structure.building.Building;
import org.mars_sim.msp.core.structure.building.BuildingException;

/**
 * The Recreation class is a building function for recreation.
 */
public class Recreation
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
    public Recreation(Building building) {
        // Use Function constructor.
        super(FunctionType.RECREATION, building);

        // Populate data members.
        populationSupport = buildingConfig.getRecreationPopulationSupport(building.getBuildingType());

        // Load activity spots
        loadActivitySpots(buildingConfig.getRecreationActivitySpots(building.getBuildingType()));
    }

    /**
     * Gets the value of the function for a named building.
     * @param buildingName the building name.
     * @param newBuilding true if adding a new building.
     * @param settlement the settlement.
     * @return value (VP) of building function.
     */
    public static double getFunctionValue(String buildingName, boolean newBuilding,
            Settlement settlement) {

        // Settlements need enough recreation buildings to support population.
        double demand = settlement.getNumCitizens();

        // Supply based on wear condition of buildings.
        double supply = 0D;
        Iterator<Building> i = settlement.getBuildingManager().getBuildings(FunctionType.RECREATION).iterator();
        while (i.hasNext()) {
            Building recreationBuilding = i.next();
            Recreation recreation = recreationBuilding.getRecreation();
            double populationSupport = recreation.getPopulationSupport();
            double wearFactor = ((recreationBuilding.getMalfunctionManager().getWearCondition() / 100D) * .75D) + .25D;
            supply += populationSupport * wearFactor;
        }

        if (!newBuilding) {
            supply -= buildingConfig.getRecreationPopulationSupport(buildingName);
            if (supply < 0D) supply = 0D;
        }

        return demand / (supply + 1D);
    }

    /**
     * Gets the number of people this recreation facility can support.
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
