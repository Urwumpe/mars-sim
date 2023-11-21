/*
 * Mars Simulation Project
 * LivingAccommodations.java
 * @date 2021-08-20
 * @author Scott Davis
 */
package com.mars_sim.core.structure.building.function;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.stream.Collectors;

import com.mars_sim.core.LocalAreaUtil;
import com.mars_sim.core.data.SolSingleMetricDataLogger;
import com.mars_sim.core.data.UnitSet;
import com.mars_sim.core.logging.SimLogger;
import com.mars_sim.core.person.Person;
import com.mars_sim.core.structure.Settlement;
import com.mars_sim.core.structure.building.Building;
import com.mars_sim.core.structure.building.BuildingException;
import com.mars_sim.core.structure.building.BuildingManager;
import com.mars_sim.core.structure.building.FunctionSpec;
import com.mars_sim.core.time.ClockPulse;
import com.mars_sim.mapdata.location.LocalPosition;
import com.mars_sim.tools.util.RandomUtil;

/**
 * The LivingAccommodations class is a building function for a living
 * accommodations.
 */
public class LivingAccommodations extends Function {

	/** default serial id. */
	private static final long serialVersionUID = 1L;
	/* default logger. */
	private static SimLogger logger = SimLogger.getLogger(LivingAccommodations.class.getName());

	public static final int MAX_NUM_SOLS = 14;

	public static final double TOILET_WASTE_PERSON_SOL = .02D;
	public static final double WASH_AND_WASTE_WATER_RATIO = .85D;
	/** The minimal amount of resource to be retrieved. */
	private static final double MIN = 0.0001;
	/** The chance of going to a restroom. */
	public static final int TOILET_CHANCE = 50;
	
	public static final String wasteName = "LivingAccomodation::generateWaste";
	
	/** max # of beds. */
	private int maxNumBeds;
	/** The # of registered sleepers. */
	private int registeredSleepers;
	/** The average water used per person for washing (showers, washing clothes, hands, dishes, etc) [kg/sol].*/
	private double washWaterUsage;
	// private double wasteWaterProduced; // Waste water produced by
	// urination/defecation per person per millisol (avg over Sol).
	/** percent portion of grey water generated from waste water.*/
	private double greyWaterFraction;

	/** The bed registry in this facility (it uses settlement-wide reference coordinate). */
	private Map<Integer, LocalPosition> assignedBeds = new ConcurrentHashMap<>();

	/** The daily water usage in this facility [kg/sol]. */
	private SolSingleMetricDataLogger dailyWaterUsage;
	
	/** The daily grey water generated in this facility [kg/sol]. */
	private SolSingleMetricDataLogger greyWaterGen;

	/**
	 * Constructor.
	 *
	 * @param building the building this function is for.
	 * @param spec Details of the Living details
	 * @throws BuildingException if error in constructing function.
	 */
	public LivingAccommodations(Building building, FunctionSpec spec) {
		// Call Function constructor.
		super(FunctionType.LIVING_ACCOMMODATIONS, spec, building);

		dailyWaterUsage = new SolSingleMetricDataLogger(MAX_NUM_SOLS);
		
		greyWaterGen = new SolSingleMetricDataLogger(MAX_NUM_SOLS);
		// Loads the max # of beds available
		maxNumBeds = spec.getCapacity();
		// Loads the wash water usage kg/sol
		washWaterUsage = personConfig.getWaterUsageRate();
		// Loads the grey to black water ratio
		double grey2BlackWaterRatio = personConfig.getGrey2BlackWaterRatio();
		// Calculate the grey water fraction
		greyWaterFraction = grey2BlackWaterRatio / (grey2BlackWaterRatio + 1);
	}

	/**
	 * Gets the value of the function for a named building.
	 *
	 * @param buildingName the building name.
	 * @param newBuilding  true if adding a new building.
	 * @param settlement   the settlement.
	 * @return value (VP) of building function.
	 * @throws Exception if error getting function value.
	 */
	public static double getFunctionValue(String buildingName, boolean newBuilding, Settlement settlement) {

		// Demand is two beds for every inhabitant (with population expansion in mind).
		double demand = settlement.getNumCitizens() * 2D;

		double supply = 0D;
		boolean removedBuilding = false;
		Iterator<Building> i = settlement.getBuildingManager().getBuildingSet(FunctionType.LIVING_ACCOMMODATIONS).iterator();
		while (i.hasNext()) {
			Building building = i.next();
			if (!newBuilding && building.getBuildingType().equalsIgnoreCase(buildingName) && !removedBuilding) {
				removedBuilding = true;
			} else {
				double wearModifier = (building.getMalfunctionManager().getWearCondition() / 100D) * .75D + .25D;
				supply += building.getLivingAccommodations().maxNumBeds * wearModifier;
			}
		}

		double bedCapacityValue = demand / (supply + 1D);

		double bedCapacity = buildingConfig.getFunctionSpec(buildingName, FunctionType.LIVING_ACCOMMODATIONS).getCapacity();

		return bedCapacity * bedCapacityValue;
	}

	/**
	 * Gets the number of beds in the living accommodations.
	 *
	 * @return number of beds.
	 */
	public int getBedCap() {
		return maxNumBeds;
	}

	/**
	 * Gets the number of assigned beds in this building.
	 *
	 * @return number of assigned beds
	 */
	public int getNumAssignedBeds() {
		return assignedBeds.size();
	}

	/**
	 * Gets the number of people registered to sleep in this building.
	 *
	 * @return number of registered sleepers
	 */
	public int getRegisteredSleepers() {
		return registeredSleepers;
	}

	/**
	 * Checks if all the beds have been taken/registered.
	 * 
	 * @return
	 */
	public boolean areAllBedsTaken() {
        return registeredSleepers >= maxNumBeds;
    }
	
	/**
	 * Assigns standard living necessity.
	 */
	public void	assignStandardNecessity(Person person) {

		// Obtain a standard set of clothing items
		person.wearGarment(building.getSettlement());

		// Assign thermal bottle
		person.assignThermalBottle();
	}
	
	/**
	 * Registers a sleeper with a bed.
	 *
	 * @param person
	 * @param isAGuest is this person a guest (not inhabitant) of this settlement
	 * @return the bed registered with the given person
	 */
	public LocalPosition registerSleeper(Person person, boolean isAGuest) {
		// Assign standard necessity
		assignStandardNecessity(person);	
		
		LocalPosition registeredBed = person.getBed();

		if (registeredBed == null) {

			if (areAllBedsTaken()) {
				 logger.log(building, Level.WARNING, 5000,  " All beds have been taken"
						 		+ " (# Registered Beds: " + registeredSleepers
						 		+ ", Bed Capacity: " + maxNumBeds + ").");
			}

			else if (!assignedBeds.containsKey(person.getIdentifier())) {
				LocalPosition bed = designateABed(person, isAGuest);
				if (bed != null) {
					if (!isAGuest) {
						registeredSleepers++;
					}
					return bed;
				}

				logger.log(building, person, Level.WARNING, 2000,
								   "Could not find a temporary bed.", null);
			}
		}

		else {
			// Ensure the person's registered bed has been added to the assignedBeds map
			if (!assignedBeds.containsValue(registeredBed)) {
				assignedBeds.put(person.getIdentifier(), registeredBed);
			}
			return registeredBed;
		}

		return null;
	}

	/**
	 * Assigns a given bed to a given person.
	 *
	 * @param person
	 * @param bed
	 */
	public void assignABed(Person person, LocalPosition bed) {
		assignedBeds.put(person.getIdentifier(), bed);
		person.setBed(bed);
		person.setQuarters(building);

		logger.log(building, person, Level.INFO, 0, "Designated a bed at "
					+ bed.getShortFormat() + ".");
	}

	/**
	 * Assigns/designates an available bed to a person.
	 *
	 * @param person
	 * @return
	 */
	public LocalPosition designateABed(Person person, boolean guest) {
		LocalPosition bedLoc = null;

		int numDesignated = getNumAssignedBeds();
		if (numDesignated >= maxNumBeds) {
			return null;
		}
		
		// TODO: How do we allocate guest beds for a
		// traveler to sleep on it.
		
		List<LocalPosition> locs = getActivitySpotsList();
		for (LocalPosition loc : locs) {
			
			// Convert the activity spot (the bed location) to the settlement reference coordinate
			bedLoc = LocalAreaUtil.getLocalRelativePosition(loc, building);
	
			if (!assignedBeds.containsValue(bedLoc)) {
				if (!guest) {
					assignABed(person, bedLoc);
				}
				else { // is a guest
					logger.log(building, person, Level.INFO, 0, "Given a temporary bed at ("
							+ bedLoc + ").", null);
				}
				break;
			}
		}

		return bedLoc;
	}

	/**
	 * Time passing for the building.
	 *
	 * @param time amount of time passing (in millisols)
	 * @throws BuildingException if error occurs.
	 */
	@Override
	public boolean timePassing(ClockPulse pulse) {
		boolean valid = isValid(pulse);
		if (valid) {
			int rand = RandomUtil.getRandomInt(TOILET_CHANCE);
			if (rand == 0) {
				generateWaste(pulse.getElapsed());
			}
		}
		return valid;
	}

	/**
	 * Adds to the daily water usage.
	 *
	 * @param amount
	 */
	public void addDailyWaterUsage(double amount) {
		dailyWaterUsage.increaseDataPoint(amount);
	}

	/**
	 * Gets the daily average water usage of the last 5 sols.
	 * Note: most weight on yesterday's usage. Least weight on usage from 5 sols ago.
	 *
	 * @return
	 */
	public double getDailyAverageWaterUsage() {
		return dailyWaterUsage.getDailyAverage();
	}

	/**
	 * Adds to the daily grey water generated.
	 *
	 * @param amount
	 */
	public void addDailyGreyWaterGen(double amount) {
		greyWaterGen.increaseDataPoint(amount);
	}

	/**
	 * Gets the daily average grey water generated of the last 5 sols.
	 * Note: most weight on yesterday's usage. Least weight on usage from 5 sols ago.
	 *
	 * @return
	 */
	public double getDailyAverageGreyWaterGen() {
		return greyWaterGen.getDailyAverage();
	}
	
	/**
	 * Utilizes water for bathing, washing, etc based on population.
	 *
	 * @param time amount of time passing (millisols)
	 */
	private void generateWaste(double time) {
		double random = 1 + RandomUtil.getRandomDouble(0.5);
		int numBed = getNumAssignedBeds();
		// int pop = settlement.getNumCurrentPopulation();
		// Total average wash water used at the settlement over this time period.
		// This includes showering, washing hands, washing dishes, etc.
		Settlement settlement = building.getSettlement();

		double ration = 1;
		// If settlement is rationing water, reduce water usage according to its level
		int level = settlement.getWaterRationLevel();
		if (level != 0)
			ration = 1.0 / level;
		// Account for people who are out there in an excursion and NOT in the
		// settlement
		double absenteeFactor = (double)settlement.getIndoorPeopleCount() / settlement.getPopulationCapacity();

		double usage =  washWaterUsage * time / 1_000 * numBed * absenteeFactor;
		double waterUsed = usage * RandomUtil.getRandomDouble(TOILET_CHANCE) * random * ration;
		double wasteWaterProduced = waterUsed * WASH_AND_WASTE_WATER_RATIO;

		// Remove wash water from settlement.
		if (waterUsed> MIN) {
			retrieve(waterUsed, WATER_ID, true);
			// Track daily average
			addDailyWaterUsage(waterUsed);
		}

		// Grey water is produced by wash water.
		double greyWaterProduced = wasteWaterProduced * greyWaterFraction;
		// Black water is only produced by waste water.
		double blackWaterProduced = wasteWaterProduced * (1 - greyWaterFraction);

		if (greyWaterProduced > MIN) {
			store(greyWaterProduced, GREY_WATER_ID, wasteName);
			// Track daily average
			addDailyGreyWaterGen(greyWaterProduced);
		}
		
		if (blackWaterProduced > MIN)
			store(blackWaterProduced, BLACK_WATER_ID, wasteName);

		// Use toilet paper and generate toxic waste (used toilet paper).
		double toiletPaperUsagePerMillisol = TOILET_WASTE_PERSON_SOL / 1000;

		double toiletPaperUsageBuilding = toiletPaperUsagePerMillisol * time * numBed * random;		
		// buildingProportionCap;
		if (toiletPaperUsageBuilding > MIN) {
			retrieve(toiletPaperUsageBuilding, TOILET_TISSUE_ID, true);
			store(toiletPaperUsageBuilding, TOXIC_WASTE_ID, wasteName);
		}
	}

	/**
	 * Gets living accommodations with empty beds from a list of buildings with the
	 * living accommodations function.
	 *
	 * @param buildingList list of buildings with the living accommodations
	 *                     function.
	 * @param unmarked     does the person wants an unmarked(aka undesignated) bed
	 *                     or not.
	 * @return list of buildings with empty beds.
	 */
	private static Set<Building> getQuartersWithEmptyBeds(Set<Building> buildingList, boolean unmarked) {
		Set<Building> result = new UnitSet<>();

		for (Building building : buildingList) {
			LivingAccommodations quarters = building.getLivingAccommodations();
			boolean notFull = quarters.getNumEmptyActivitySpots() > 0;//quarters.getRegisteredSleepers() < quarters.getBedCap();
			// Check if an unmarked bed is wanted
			if (unmarked) {
				if (quarters.hasAnUnmarkedBed() && notFull) {
					result.add(building);
				}
			}
			else if (notFull) {
				result.add(building);
			}
		}

		return result;
	}
	
	/**
	 * Gets the best available living accommodations building that the person can
	 * use. Returns null if no living accommodations building is currently
	 * available.
	 *
	 * @param person   the person
	 * @param unmarked does the person wants an unmarked(aka undesignated) bed ?
	 * @param sameZone must be in the same zone. True if it's optional to be in the same zone.                
	 * @return a building with available bed(s)
	 */
	public static Building getBestAvailableQuarters(Person person, boolean unmarked, boolean sameZone) {

		Building result = null;

		if (person.isInSettlement()) {
			Set<Building> set0 = person.getSettlement().getBuildingManager()
					.getBuildingSet(FunctionType.LIVING_ACCOMMODATIONS);

			if (sameZone && person.getBuildingLocation() != null) {
				set0 = set0
						.stream()
						.filter(b -> b.getZone() == person.getBuildingLocation().getZone()
								&& !b.getMalfunctionManager().hasMalfunction())
						.collect(Collectors.toSet());
			}
			else if (!sameZone) {
				set0 = set0
						.stream()
						.filter(b -> !b.getMalfunctionManager().hasMalfunction())
						.collect(Collectors.toSet());		
			}
			
			Set<Building> set1 = BuildingManager.getNonMalfunctioningBuildings(set0);
			if (!set1.isEmpty()) {
				return getBestQuarters(person, set0);
			}
			
			Set<Building> set2 = getQuartersWithEmptyBeds(set1, unmarked);
			if (!set2.isEmpty()) {
				return getBestQuarters(person, set1);
			}
			
			Set<Building> set3 = BuildingManager.getLeastCrowdedBuildings(set2);
			if (!set3.isEmpty()) {
				return getBestQuarters(person, set2);
			}

			return getBestQuarters(person, set3);
		}

		return result;
	}

	/**
	 * Gets the best quarters.
	 * 
	 * @param person
	 * @param set
	 * @return
	 */
	private static Building getBestQuarters(Person person, Set<Building> set) {
		Map<Building, Double> probs = BuildingManager.getBestRelationshipBuildings(person,
				set);
		return RandomUtil.getWeightedRandomObject(probs);
	}
	
	/**
	 * Releases any bed assigned to a Person.
	 * 
	 * @param person
	 * @return
	 */
	public void releaseBed(Person person) {
		assignedBeds.remove(person.getIdentifier());
	}

	/*
	 * Checks if an unmarked or unassigned bed is available
	 */
	public boolean hasAnUnmarkedBed() {
        return getNumAssignedBeds() < maxNumBeds;
	}

	@Override
	public double getMaintenanceTime() {
		return maxNumBeds * 7D;
	}

	public void destroy() {
		building = null;
		assignedBeds.clear();
		assignedBeds = null;
		dailyWaterUsage = null;
	}
}
