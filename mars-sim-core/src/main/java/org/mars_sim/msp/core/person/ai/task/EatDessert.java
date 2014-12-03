/**
 * Mars Simulation Project
 * EatDessert.java
 * @version 3.07 2014-11-28
 * @author Manny Kung
 */
package org.mars_sim.msp.core.person.ai.task;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.mars_sim.msp.core.Inventory;
import org.mars_sim.msp.core.Msg;
import org.mars_sim.msp.core.RandomUtil;
import org.mars_sim.msp.core.SimulationConfig;
import org.mars_sim.msp.core.Unit;
import org.mars_sim.msp.core.person.LocationSituation;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.PersonConfig;
import org.mars_sim.msp.core.person.PhysicalCondition;
import org.mars_sim.msp.core.person.ai.SkillType;
import org.mars_sim.msp.core.resource.AmountResource;
import org.mars_sim.msp.core.structure.Settlement;
import org.mars_sim.msp.core.structure.building.Building;
import org.mars_sim.msp.core.structure.building.BuildingException;
import org.mars_sim.msp.core.structure.building.BuildingManager;
import org.mars_sim.msp.core.structure.building.function.BuildingFunction;
import org.mars_sim.msp.core.structure.building.function.cooking.PreparedDessert;
import org.mars_sim.msp.core.structure.building.function.cooking.PreparingDessert;
import org.mars_sim.msp.core.vehicle.Rover;

/**
 * The EatDessert class is a task for eating soymilk.
 * The duration of the task is 40 millisols.
 * Note: Eating soymilk reduces hunger to 0 and reduce stress.
 */
public class EatDessert 
extends Task 
implements Serializable {

    /** default serial id. */
    private static final long serialVersionUID = 1L;

	/** default logger. */
	private static Logger logger = Logger.getLogger(EatDessert.class.getName());

    /** Task name */
    private static final String NAME = Msg.getString(
            "Task.description.eatDessert"); //$NON-NLS-1$

    /** Task phases. */
    private static final TaskPhase EATING = new TaskPhase(Msg.getString(
            "Task.phase.eatingDessert")); //$NON-NLS-1$
    
    // Static members
    /** The stress modified per millisol. */
    private static final double STRESS_MODIFIER = -.2D;

    // Data members
    private PreparedDessert aServingOfDessert;

    // 2014-11-28 Added HUNGER_REDUCTION_PERCENT
    private static final double HUNGER_REDUCTION_PERCENT = 20D;
    
    /** 
     * Constructs a EatMeal object, hence a constructor.
     * @param person the person to perform the task
     */
    public EatDessert(Person person) {
        super(NAME, person, false, false, STRESS_MODIFIER, true, 10D + 
                RandomUtil.getRandomDouble(30D));

        //logger.info("just called EatDessert's constructor");

        boolean walkSite = false;

        LocationSituation location = person.getLocationSituation();
        if (location == LocationSituation.IN_SETTLEMENT) {
            // If person is in a settlement, try to find a dining area.
            Building diningBuilding = getAvailableDiningBuilding(person);
            if (diningBuilding != null) {

                // Walk to dining building.
                walkToActivitySpotInBuilding(diningBuilding);
                walkSite = true;
            }

            // If fresh dessert is available in a local kitchen, go there.
            PreparingDessert kitchen = getKitchenWithDessert(person);
            if (kitchen != null) {
                aServingOfDessert = kitchen.getFreshDessert();
                //TODO: check if dessert is deleted from AmountResource                
            }
            if (aServingOfDessert != null) {
                setDescription(Msg.getString("Task.description.eatDessert.made")); //$NON-NLS-1$
            }
        }
        else if (location == LocationSituation.OUTSIDE) {
            endTask();
        }

        if (!walkSite) {
            if (person.getLocationSituation() == LocationSituation.IN_VEHICLE) {
                // If person is in rover, walk to passenger activity spot.
                if (person.getVehicle() instanceof Rover) {
                    walkToPassengerActivitySpotInRover((Rover) person.getVehicle());
                }
            }
            else {
                // Walk to random location.
                walkToRandomLocation();
            }
        }

        // Initialize task phase.
        addPhase(EATING);
        setPhase(EATING);
    }

    @Override
    protected BuildingFunction getRelatedBuildingFunction() {
        return BuildingFunction.DINING;
    }

    /**
     * Performs the method mapped to the task's current phase.
     * @param time the amount of time (millisol) the phase is to be performed.
     * @return the remaining time (millisol) after the phase has been performed.
     */
    protected double performMappedPhase(double time) {
        if (getPhase() == null) {
            throw new IllegalArgumentException("The task phase is null");
        }
        else if (EATING.equals(getPhase())) {
            return eatingPhase(time);
        }
        else {
            return time;
        }
    }

    /**
     * Performs the eating phase of the task.
     * @param time the amount of time (millisol) to perform the eating phase.
     * @return the amount of time (millisol) left after performing the eating phase.
     */
    private double eatingPhase(double time) {

        PhysicalCondition condition = person.getPhysicalCondition();

        // If person drinks fresh soymilk, additional stress is reduced.
        if (aServingOfDessert != null) {
            double stress = condition.getStress();
            condition.setStress(stress - (STRESS_MODIFIER * (aServingOfDessert.getQuality() + 1D)));
        }

        if (getDuration() <= (getTimeCompleted() + time)) {
            PersonConfig config = SimulationConfig.instance().getPersonConfiguration();
            try {
            	//logger.info("eatingPhase() : (aServingOfDessert == null) is " + (aServingOfDessert == null));
                person.consumeDessert(config.getFoodConsumptionRate() * (1D / 3D), (aServingOfDessert == null));
                // 2014-11-28 Computed new hunger level
                double hunger = condition.getHunger();
                hunger = hunger * (1 - HUNGER_REDUCTION_PERCENT/100);
                condition.setHunger(hunger);
            }
            catch (Exception e) {
                // If person can't obtain soymilk from container, end the task.
                endTask();
            }
        }

        return 0D; 
    }

    /**
     * Adds experience to the person's skills used in this task.
     * @param time the amount of time (ms) the person performed this task.
     */
    protected void addExperience(double time) {
        // This task adds no experience.
    }

    /**
     * Gets an available dining building that the person can use.
     * Returns null if no dining building is currently available.
     *
     * @param person the person
     * @return available dining building
     * @throws BuildingException if error finding dining building.
     */
    //TODO: For now, get soymilk from refrigerator only from dining building  
    public static Building getAvailableDiningBuilding(Person person) {

        Building result = null;

        if (person.getLocationSituation() == LocationSituation.IN_SETTLEMENT) {
            Settlement settlement = person.getSettlement();
            BuildingManager manager = settlement.getBuildingManager();
            List<Building> diningBuildings = manager.getBuildings(BuildingFunction.DINING);
            diningBuildings = BuildingManager.getWalkableBuildings(person, diningBuildings);
            diningBuildings = BuildingManager.getNonMalfunctioningBuildings(diningBuildings);
            diningBuildings = BuildingManager.getLeastCrowdedBuildings(diningBuildings);

            if (diningBuildings.size() > 0) {
                Map<Building, Double> diningBuildingProbs = BuildingManager.getBestRelationshipBuildings(
                        person, diningBuildings);
                result = RandomUtil.getWeightedRandomObject(diningBuildingProbs);
            }
        }

        return result;
    }

    /**
     * Gets a kitchen in the person's settlement that currently has fresh soymilk.
     * @param person the person to check for
     * @return the kitchen or null if none.
     */
    public static PreparingDessert getKitchenWithDessert(Person person) {
    	PreparingDessert result = null;

        if (person.getLocationSituation() == LocationSituation.IN_SETTLEMENT) {
            Settlement settlement = person.getSettlement();
            BuildingManager manager = settlement.getBuildingManager();
            List<Building> cookingBuildings = manager.getBuildings(BuildingFunction.PREPARING_DESSERT);
            Iterator<Building> i = cookingBuildings.iterator();
            while (i.hasNext()) {
                Building building = i.next();
                PreparingDessert kitchen = (PreparingDessert) building.getFunction(BuildingFunction.PREPARING_DESSERT);
                if (kitchen.hasFreshDessert()) result = kitchen;
            }
        }

        return result;
    }

    /**
     * Checks if there is soymilk available for the person.
     * @param person the person to check.
     * @return true if soymilk is available.
     */
    public static boolean isDessertAvailable(Person person) {
        boolean result = false;
        Unit containerUnit = person.getContainerUnit();
        if (containerUnit != null) {
            try {
                Inventory inv = containerUnit.getInventory();
                AmountResource soymilk = AmountResource.findAmountResource("Soymilk");
                if (inv.getAmountResourceStored(soymilk, false) > 0D) result = true;;
            }
            catch (Exception e) {
                e.printStackTrace(System.err);
            }
        }
        return result;
    }

    @Override
    public int getEffectiveSkillLevel() {
        return 0;
    }

    @Override
    public List<SkillType> getAssociatedSkills() {
        List<SkillType> results = new ArrayList<SkillType>(0);
        return results;
    }

    @Override
    public void destroy() {
        super.destroy();

        aServingOfDessert = null;
    }
}