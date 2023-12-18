/*
 * Mars Simulation Project
 * AlgaeFarming.java
 * @date 2023-12-07
 * @author Manny Kung
 */

package com.mars_sim.core.structure.building.function.farming;

import java.util.logging.Level;

import com.mars_sim.core.data.SolMetricDataLogger;
import com.mars_sim.core.logging.SimLogger;
import com.mars_sim.core.person.ai.task.util.Worker;
import com.mars_sim.core.resource.ResourceUtil;
import com.mars_sim.core.structure.Settlement;
import com.mars_sim.core.structure.building.Building;
import com.mars_sim.core.structure.building.BuildingException;
import com.mars_sim.core.structure.building.FunctionSpec;
import com.mars_sim.core.structure.building.function.Function;
import com.mars_sim.core.structure.building.function.FunctionType;
import com.mars_sim.core.structure.building.function.HouseKeeping;
import com.mars_sim.core.time.ClockPulse;
import com.mars_sim.core.time.MarsTime;
import com.mars_sim.core.time.MasterClock;
import com.mars_sim.tools.util.RandomUtil;

/**
 * This function that is responsible for farming algae.
 */
public class AlgaeFarming extends Function {	

	/** default serial id. */
	private static final long serialVersionUID = 1L;
	/** default logger. */
	private static SimLogger logger = SimLogger.getLogger(AlgaeFarming.class.getName());

	private static final int MAX_NUM_SOLS = 14;
	
	private static final String [] INSPECTION_LIST = Fishery.INSPECTION_LIST;
	private static final String [] CLEANING_LIST = Fishery.CLEANING_LIST;
	
	/** The average water needed [in kg]. */
//	private static double averageWaterNeeded;
	/** The average O2 needed [in kg]. */
	private static double averageOxygenNeeded;
	/** The average CO2 needed [in kg]. */
	private static double averageCarbonDioxideNeeded;
	
	private static final int WATER_ID = ResourceUtil.waterID;
	private static final int OXYGEN_ID = ResourceUtil.oxygenID;
	private static final int CO2_ID = ResourceUtil.co2ID;
	private static final int GREY_WATER_ID = ResourceUtil.greyWaterID;

	public static final int HARVESTED_ALGAE_ID = ResourceUtil.spirulinaID;
	public static final int PRODUCED_ALGAE_ID = HARVESTED_ALGAE_ID + 1; // id: 362
	
	private static final int LIGHT_FACTOR = 0;
//	private static final int FERTILIZER_FACTOR = 1;
	private static final int TEMPERATURE_FACTOR = 2;	
//	private static final int WATER_FACTOR = 3;
	private static final int O2_FACTOR = 4;
	private static final int CO2_FACTOR = 5;
	
	// Initial amount of water [in liters] per kg algae
//	private static final int INITIAL_WATER_NEED_PER_KG_ALGAE = 333; 
	/** The food nutrient demand of algae. **/
	private static final int NUTRIENT_DEMAND = 250;
	private static final double GAS_MODIFIER = 1.5;
//	private static final double WATER_MODIFIER = 1.1;
	private static final double TUNING_FACTOR = 3.5;
	
// When grown in darkness or light intensity below 1000 lux, the algal cultures 
// produced a very small amount of biomass. 
// On the contrary, a large amount of biomass was produced with higher light 
// intensities of 1500 to 3500 lux. With a light intensity of 2500 lux, 
// the best growth rates were achieved.

	/**
	 * The limiting factor that determines how fast and how much PAR can be absorbed
	 * in one frame.
	 * Note: 1 is max. if set to 1, a lot of lights will toggle on and off undesirably.
	 */
	private static final double PHYSIOLOGICAL_LIMIT = 0.9; 
	/** The wattage of a 400W high pressure sodium (HPS) lamp. */
	private static final double KW_PER_HPS = .4;
	/** The lamp efficiency of the high pressure sodium (HPS) lamp. */
	private static final double VISIBLE_RADIATION_HPS = 0.4;
	/** The ballast loss of the high pressure sodium (HPS) lamp. */
	private static final double BALLAST_LOSS_HPS = 0.1;
	/** The non-visible radiation loss of the high pressure sodium (HPS) lamp. */
	private static final double NON_VISIBLE_RADIATION_HPS = .37;
	/** The conduction convection loss of the high pressure sodium (HPS) lamp. */
	private static final double CONDUCTION_CONVECTION_HPS = .13;
	/** The total loss of the high pressure sodium (HPS) lamp. */
	public static final double LOSS_FACTOR_HPS = NON_VISIBLE_RADIATION_HPS * .75 + CONDUCTION_CONVECTION_HPS / 2D;
	
	/**
	 * The daily PAR [in mol / m^2 / day ].
	 * Note: not umol / m^2 / s
	 */
	private static double dailyPAR = 20; 
	/**
	 * The watt to photon conversion ratio on Mars as defined in crops.xml [in umol
	 * /m^2 /s /(Wm^-2)].
	 */
	private static double wattToPhotonConversionRatio;
	/**
	 * The converted value of the watt to photon conversion ratio on Mars as defined
	 * in crops.xml [in umol /m^2 /millisols /(Wm^-2)].
	 */
	private static double conversionFactor;

	// The best growth and biomass concentration were obtained at 32 °C with 12.28 g/l. 
	// However, growth rates and production of biomass were significantly lower when 
	// the temperature above 35 °C and below 25 °C. 

	
	/** The average temperature tolerance [in C]. */
	private static final double T_TOLERANCE = 7D;
	
	/**
	 * The ratio of oxygen to carbon during the day when photosynthesis is taking
	 * place and CO2 is absorbed and O2 is generated by the crop.
	 */
	private static final double O2_TO_CO2_RATIO = 44 / 32D;
	/**
	 * The rate of carbon dioxide to oxygen during night time when O2 is absorbed
	 * and CO2 is released by the crop.
	 * <p> Note: 6CO2 --> 6O2 since
	 * <p> 6nCO2 + 5nH2O ⇒ (C6H10O5)n + 6nO2
	 */
	private static final double CO2_TO_O2_RATIO = 32 / 44D; 

	// By providing the ideal conditions (ph 10.5, temp 32 Cel, 70% light)  
	// and a proper nutrient balance), Spirulina can multiply by 25% every day
	// growth rate per millisol = 25% / 1000 = 0.00025
	
	// Birth or growth rate of algae in kg/millisol
	public static final double BIRTH_RATE = 0.25 / 1000;
	// The ideal ratio of the mass of food nutrient to mass of algae
	public static final double NUTRIENT_RATIO = 0.05;
	// Average amount of food nibbled by 1 kg of algae per sol
	private static final double AVERAGE_NIBBLES = 0.001;
	// Average amount of fresh water supplied to 1 kg of algae per sol
	private static final double AVERAGE_FRESH_WATER = 0.2;
	// kW per litre of water
	private static final double POWER_PER_LITRE = 0.0001D;
	// kW per kg algae
	private static final double POWER_PER_KG_ALGAE = 0.001D;
	// kW per kg food mass
	private static final double POWER_PER_KG_FOOD = 0.0001D;
	// The rate of adding food when tending the pond 
	private static final double ADD_FOOD_RATE = 0.75;
	// Tend time for food
	private static final double TEND_TIME_FOR_FOOD = 0.2D;
	/** The ideal amount of algae as a percentage. **/
	private static final double IDEAL_PERCENTAGE = 0.8D;
	
	
	// For proper growth, add 30 grams of spirulina for every 10 liters of water.
	// https://krishijagran.com/agripedia/all-about-organic-spirulina-cultivation-basic-requirements-water-quality-nutrient-requirements-economics-much-more/
	// The initial ratio of spirulina and water [in kg/L]
	public static final double ALGAE_TO_WATER_RATIO = 0.03 / 10; 
	

	// Based on https://www.sciencedirect.com/science/article/pii/S2352484718303974,
	// Biomass yield obtained from an open pond system was 11.34 g/l, 
	// and 12.28 g/l in the closed reactor system. 
	
	// Based on https://grow-organic-spirulina.com/blog/how-much-spirulina-can-i-harvest-per-day-free-harvesting-calculator-included/
	// ~ 10 grams per CM per day (or between 6 to 15 grams per cubic meter per day
	// The best spirulina growing practices improve production.
	// Note: 1 CM = 1000 L
	
	// The expected biomass yield in an open pond system [kg/L/millisols]
	private static final double EXPECTED_YIELD_RATE = .01134; 
	/** The amount of algae per harvest. **/
//	private static final double KG_PER_HARVEST_PER_MILLISOL = .05;

	// Note: 1000 L = 1 m^3; 10000 L = 10 m^3 or 10 CM;
	// Assuming 0.5 m deep with room utilization 40 %
	// 6 m * 9 m * 40% * 0.5 m = 10.8 m^3
	/** The size of tank [in liters]. **/
	private double tankSize;
	/** The area of tank [in m^2]. **/
	private double tankArea;
	/** The depth of tank [in m]. **/
	private double tankDepth;
	/** The amount of water [in m]. **/
	private double waterMass;
	
	// By providing the ideal conditions (ph 10.5, temp 32 Cel, 70% light)  
	// and a proper nutrient balance), Spirulina can multiply by 25% every day
	
	/** The amount iteration for growing new spirulina. */
	private double birthIterationCache;
	/** The total mass in the tank. */
//	private double totalMass;
	/** Maximum amount of algae. */
	private double maxAlgae;
	/** Current amount of algae. */
	private double currentAlgae;
	/** Current amount of food. */
	private double currentFood;
	/** Optimal amount of algae. */
	private double idealAlgae;
	/** Current health of algae (from 0 to 1). */	
	private double health = 1;
	/** How old are the food nutrient since the last tending ? */
	private double foodAge = 0;
	/** How long it has been tendered. */
	private double tendertime;
	/** The cache for co2. */
	private double co2Cache = 0;
	/** The cache for o2. */	
	private double o2Cache = 0;

	/** The cumulative time spent in this greenhouse [in sols]. */
	private double cumulativeWorkTime;	
	/** The cumulative value of the daily PAR so far. */
	private double cumulativeDailyPAR = 0;
	/** The required power for lighting [in kW]. */
	private double lightingPower = 0;
	/** The total amount of light received by this crop. */
	private double effectivePAR;
	
	/** The cache values of the past environment factors influencing the crop */
	private double[] environmentalFactor = new double[CO2_FACTOR + 1];

	// Future: will include the amount of water and the need to replenish water over time
	
	/** Keep track of cleaning and inspections. */
	private HouseKeeping houseKeeping;
	
	/** The resource logs for growing algae in this facility [kg/sol]. */
	private SolMetricDataLogger<Integer> resourceLog = new SolMetricDataLogger<>(MAX_NUM_SOLS);
	
	private static CropConfig cropConfig;
	
	/**
	 * Constructor.
	 * 
	 * @param building the building the function is for.
	 * @param spec Definition of the AlgaeFarming properties
	 * @throws BuildingException if error in constructing function.
	 */
	public AlgaeFarming(Building building, FunctionSpec spec) {
		// Use Function constructor.
		super(FunctionType.ALGAE_FARMING, spec, building);
	
//		averageWaterNeeded = cropConfig.getWaterConsumptionRate();
		averageOxygenNeeded = cropConfig.getOxygenConsumptionRate();
		averageCarbonDioxideNeeded = cropConfig.getCarbonDioxideConsumptionRate();
		wattToPhotonConversionRatio = cropConfig.getWattToPhotonConversionRatio();
		// Note: conversionFactor is 51.45578648029399;
		conversionFactor = 1000D * wattToPhotonConversionRatio / MarsTime.SECONDS_PER_MILLISOL;
		
		houseKeeping = new HouseKeeping(CLEANING_LIST, INSPECTION_LIST);

		tankArea = spec.getArea();
		tankDepth = spec.getDepth();
		
		// Calculate the tank size in Liter 
		// Note:  1000 L = 1 m^3; 10000 L = 10 m^3;
		tankSize = tankArea * tankDepth * 1000;
		// Calculate max algae based on tank size
		maxAlgae = tankSize * ALGAE_TO_WATER_RATIO;
		
		idealAlgae = maxAlgae * IDEAL_PERCENTAGE;
	    
		currentAlgae = RandomUtil.getRandomDouble(idealAlgae * 0.05, idealAlgae * 0.15);
		// The amount of water in kg
		waterMass = tankSize * currentAlgae / maxAlgae;
		
		// Retrieve water to create pond
		// Note that 1 L of water is 1 kg
//		building.getSettlement().retrieveAmountResource(ResourceUtil.waterID, waterMass);
		
	    double initalFood = currentAlgae * NUTRIENT_RATIO;
	    
		tendertime = initalFood * TEND_TIME_FOR_FOOD;
    
		// Give variation to the amount of food nutrient for algae at the start for each tank
		initalFood = RandomUtil.getRandomDouble(initalFood * 0.95, initalFood * 1.05);
		currentFood = initalFood;
		
	    logger.log(building, Level.CONFIG, 0, "Spirulina: " 
	    		+ Math.round(currentAlgae * 10.0)/10.0 
	    		+ " kg.  nutrient: " + Math.round(initalFood * 10.0)/10.0);
	}


	/**
	 * Gets the value of the function for a named building type.
	 * 
	 * @param type the building type.
	 * @param newBuilding  true if adding a new building.
	 * @param settlement   the settlement.
	 * @return value (VP) of building function. Called by BuildingManager.java
	 *         getBuildingValue()
	 */
	public static double getFunctionValue(String type, boolean newBuilding, Settlement settlement) {

		// The demand should be the amount of algae needed to produce food for settlement population
		double demand = 2D * settlement.getNumCitizens();

		// The supply is the production capability and the amount of algae at settlement.
		double supply = 0D;
		boolean removedBuilding = false;
		for (Building building : settlement.getBuildingManager().getBuildingSet(FunctionType.ALGAE_FARMING)) {
			if (!newBuilding && building.getBuildingType().equalsIgnoreCase(type) && !removedBuilding) {
				removedBuilding = true;
			} else {
				double wearModifier = (building.getMalfunctionManager().getWearCondition() / 100D) * .75D + .25D;
				supply += building.getAlgae().getCurrentAlgae() * wearModifier;
			}
		}

		// Modify result by value (VP) of spirulina at the settlement.
		double foodValue = settlement.getGoodsManager().getGoodValuePoint(ResourceUtil.spirulinaID);

		return (demand / (supply + 1D)) * foodValue;
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
			double time = pulse.getElapsed();
			
		    // Simulate the pond activity
		    simulatePond(pulse);

		    double degradeValue = time / 1000;
			// degrade the cleanliness
			houseKeeping.degradeCleaning(degradeValue);
			// degrade the housekeeping item
			houseKeeping.degradeInspected(degradeValue);
			
			foodAge += time;
			
			// Resets thing at the end of a sol.
			if (resetEndOfSol(pulse)) {
				// Add tasks
			}
			
		}
		return valid;
	}
	
	/**
	 * Gets the cumulative work time [in sols].
	 * 
	 * @return
	 */
	public double getCumulativeWorkTime() {
		return cumulativeWorkTime;
	}
	
	/**
	 * Adds the cumulative work time.
	 * 
	 * @return
	 */
	public void addCumulativeWorkTime(double value) {
		cumulativeWorkTime += value;
	}
	
	/**
	 * Records the average resource usage of a resource
	 *
	 * @param amount    average consumption/production in kg/sol
	 * @Note positive usage amount means consumption 
	 * @Note negative usage amount means generation
	 * @param id The resource id
	 */
	public void addResourceLog(double amount, int id) {
		resourceLog.increaseDataPoint(id, amount);
	}
	

	/**
	 * Computes the daily average of a particular resource.
	 * 
	 * @param id The resource id
	 * @return average consumed or produced in kg/sol
	 */
	public double computeDaily(int id) {
		return resourceLog.getDailyAverage(id);
	}
	
	/**
	 * Computes the effects of the concentration of O2 and CO2.
	 *
	 * @param watt
	 * @param compositeFactor
	 */
	private void computeGases(double watt, double compositeFactor) {
		// Note: uPAR includes both sunlight and artificial light
		// Calculate O2 and CO2 usage kg per sol
		double o2Modifier = 0;
		double co2Modifier = 0;

		// A. During the night when light level is low
		if (watt < 40) {

			double o2Required = compositeFactor * averageOxygenNeeded;
			double o2Available = building.getSettlement().getAmountResourceStored(OXYGEN_ID);
			double o2Used = o2Required;

			o2Modifier = o2Available / o2Required;

			if (o2Used > o2Available)
				o2Used = o2Available;
			o2Cache = retrieveGas(o2Used, o2Cache, OXYGEN_ID);

			adjustEnvironmentFactor(o2Modifier, O2_FACTOR);

			// Determine the amount of co2 generated via gas exchange.
			double cO2Gen = o2Used * CO2_TO_O2_RATIO;
			co2Cache = storeGas(cO2Gen, co2Cache, CO2_ID);
		}

		else {
			// B. During the day

			// Determine harvest modifier by amount of carbon dioxide available.
			double cO2Req = compositeFactor * averageCarbonDioxideNeeded;
			double cO2Available = building.getSettlement().getAmountResourceStored(CO2_ID);
			double cO2Used = cO2Req;

			// Future: allow higher concentration of co2 to be pumped to increase the harvest
			// modifier to the harvest.

			co2Modifier = cO2Available / cO2Req;

			if (cO2Used > cO2Available)
				cO2Used = cO2Available;
			co2Cache = retrieveGas(cO2Used, co2Cache, CO2_ID);
			
			// Note: research how much high amount of CO2 may facilitate the crop growth and
			// reverse past bad health

			adjustEnvironmentFactor(co2Modifier, CO2_FACTOR);

			// 6CO2 + 6H2O + sunlight -> C6H12O6 + 6O2
			//
			// Determine the amount of oxygen generated during the day when photosynthesis
			// is taking place .
			double o2Gen = cO2Used * O2_TO_CO2_RATIO;
			o2Cache = storeGas(o2Gen, o2Cache, OXYGEN_ID);
		}
	}

	/**
	 * Adjusts the environmental factors.
	 *
	 * @param mod the modifier of interest
	 * @param type the
	 */
	private void adjustEnvironmentFactor(double mod, int type) {
		double f = environmentalFactor[type];
		f = 0.01 * mod + 0.99 * f;
		if (f > 1.25)
			f = 1.25;
		else if (f < 0.1 || Double.isNaN(f))
			f = 0.1;
		environmentalFactor[type] = f;
	}

	/**
	 * Retrieves water from the Settlement and record the usage in the Farm.
	 * 
	 * @param amount Amount being retrieved
	 * @param id Resource id
	 */
	private void retrieveWater(double amount, int id) {
		if (amount > 0) {
			retrieve(amount, WATER_ID, true);
			// Record the amount of water consumed
			addResourceLog(amount, id);		
		}
	}
	
	/**
	 * Retrieves the gas from a settlement.
	 *
	 * @param amount
	 * @param gasCache Any gas cached from the last call
	 * @param gasId resource id
	 * @return
	 */
	private double retrieveGas(double amount, double gasCache, int gasId) {
		if (amount > 0) {
			if (gasCache - amount < -getGasThreshold()) {
				retrieve(gasCache, gasId, true);
				gasCache = -amount;
			}
			else {
				gasCache -= amount;
			}
			addResourceLog(amount, gasId);
		}
		return gasCache;
	}

	/**
	 * Stores grey water in the Settlement and record the usage.
	 * 
	 * @param amount Amount being retrieved
	 * @param id Resource id
	 */
	private void storeGreyWater(double amount, int id) {
		if (amount > 0) {
			store(amount, GREY_WATER_ID, "AlgaeFarming::storeGreyWater");
			// Record the amount of grey water produced
			addResourceLog(amount, id);		
		}
	}
	
	/**
	 * Stores the gas.
	 *
	 * @param amount
	 * @param gasCache
	 * @param gasId resource id
	 * @return
	 */
	private double storeGas(double amount, double gasCache, int gasId) {
		if (amount > 0) {
			if (gasCache + amount > getGasThreshold()) {
				store(gasCache, gasId, "AlgaeFarming::storeGas");
				gasCache = amount;
			}
			else {
				gasCache -= amount;
			}
			addResourceLog(-amount, gasId);
		}
		return gasCache;
	}

	private double getGasThreshold() {
		return (waterMass + currentAlgae + currentFood) / 100; 
	}
	
	/**
	 * Gets the algae-to-water ratio [in g/L].
	 * 
	 * @return
	 */
	public double getAlgaeWaterRatio() {
		return currentAlgae/waterMass * 1000;
	}
	
	/**
	* Simulates life in the pond, using the values indicated in the
	* documentation.
	*
	* @param pulse
	**/
	private void simulatePond(ClockPulse pulse) {
		double time = pulse.getElapsed();
		// per gram of algae per millisol
		double timeFactor =  time / 1000;
		// amount of food per millisol
		double nibbleAmount = RandomUtil.getRandomDouble(.9, 1.1) 
				* currentAlgae * AVERAGE_NIBBLES * timeFactor;
	   
		// Compute the amount of food nutrient to be consumed per millisol
		currentFood = currentFood - nibbleAmount;	   
		
		// Future: treat this as a task for turning on and off inlet and outlet
		double new2Old = currentAlgae/waterMass / ALGAE_TO_WATER_RATIO;
		// Check if it's too concentrated (more than 1% of its target ratio) and need more fresh water
		if (new2Old > 1.01) {
			// Compute the amount of fresh water to be added at the inlet
			double freshWater = new2Old * AVERAGE_FRESH_WATER * currentAlgae * timeFactor;
			// Consume fresh water
			retrieveWater(freshWater, ResourceUtil.waterID);
		    // Add fresh water to the existing tank water
			waterMass += freshWater;
		}
		
		// Check if it's too diluted (less than 90% of its target ratio) and need to release water
		else if (new2Old < 0.9) {
			// Compute the amount of fresh water to be released at the outlet
			double greyWater = AVERAGE_FRESH_WATER / new2Old * currentAlgae * timeFactor;
			// Produce grey water
			storeGreyWater(greyWater, ResourceUtil.greyWaterID);
		    // Retrieve grey water from the existing tank water
			waterMass -= greyWater;
		}
		
		
		// Estimate 0.01% water evaporated
		double greyWater = waterMass * .0001 * timeFactor;
		// Produce grey water
		storeGreyWater(greyWater, ResourceUtil.greyWaterID);
	    // Retrieve grey water from the existing tank water
		waterMass -= greyWater;
		
		// STEP 1 : COMPUTE THE EFFECTS OF THE SUNLIGHT AND ARTIFICIAL LIGHT
		
		adjustEnvironmentFactor(1D, LIGHT_FACTOR);
		
		double solarIrradiance = surface.getSolarIrradiance(building.getSettlement().getCoordinates());
		computeLight(pulse, time, solarIrradiance);
		
		// STEP 2 : COMPUTE THE EFFECTS OF THE TEMPERATURE
		// Compute the effect of the temperature
		double temperatureModifier = 1D;
		double tempNow = building.getCurrentTemperature();
		double tempInitial = building.getPresetTemperature();
		if (tempNow > (tempInitial + T_TOLERANCE))
			temperatureModifier = tempInitial / tempNow;
		else if (tempNow < (tempInitial - T_TOLERANCE))
			temperatureModifier = tempNow / tempInitial;
		
		adjustEnvironmentFactor(temperatureModifier, TEMPERATURE_FACTOR);

		// STEP 3 : COMPUTE THE NEED FACTOR AND COMPOSITE FACTOR (BASED ON LIGHT AND GROWTH FACTOR)
		double watt = effectivePAR / time / conversionFactor * tankArea * 1000;
		// Note: effectivePAR already includes both sunlight and artificial light

		// Note: needFactor aims to give a better modeling of the amount of water
		// and O2 and CO2 produced/consumed based on the growthFactor and
		// how much the amount of light available (which change the rate of photosynthesis)
		double needFactor = 2.5;
		if (watt >= 40) {
			needFactor = .0185 * watt + 1.76;
			// needFactor ranges from growthFactor * 2.5 to growthFactor * 11
		}
		
		double compositeFactor = TUNING_FACTOR * needFactor * time / 1000.0;
		
		// COMPUTE THE EFFECTS OF GASES (O2 and CO2 USAGE)
		
		// Note: computeGases takes up 25% of all cpu utilization
		computeGases(watt, compositeFactor * GAS_MODIFIER);	
		
		// By providing the ideal conditions (ph 10.5, temp 32 Cel, 70% light)  
		// and a proper nutrient balance), Spirulina can multiply by 25% every day
		// https://grow-organic-spirulina.com/blog/how-much-spirulina-can-i-harvest-per-day-free-harvesting-calculator-included/
		
		// Create new spirulina
		birthSpirulina(time);
	}
	
	/**
	 * Births spirulina.
	 * 
	 * @param time
	 */
	private void birthSpirulina(double time) {

		// Create new spirulina, using BIRTH_RATE
		if (currentAlgae < maxAlgae * 1.25 && currentFood > 0) {
			birthIterationCache += BIRTH_RATE * time * currentAlgae * health
					   * (1 + .01 * RandomUtil.getRandomInt(-25, 25));
		   if (birthIterationCache > 1) {
			   double newAlgae = birthIterationCache;
			   birthIterationCache = birthIterationCache - newAlgae;
			   currentAlgae += newAlgae;
		   
			   // Record the freshly produced spirulina
			   addResourceLog(newAlgae, PRODUCED_ALGAE_ID);
			   
			   // Compute the amount of fresh water to be replenished at the inlet
			   double freshWater = newAlgae / ALGAE_TO_WATER_RATIO * RandomUtil.getRandomDouble(.9, 1.1);
			   // Consume fresh water
			   retrieveWater(freshWater, ResourceUtil.waterID);
			   // Add fresh water to the existing tank water
			   waterMass += freshWater;
		   }
		}
	}

	/**
	 * Resets things at the end of a sol.
	 *
	 * @param pulse
	 * @return
	 */
	public boolean resetEndOfSol(ClockPulse pulse) {
		if (pulse.isNewSol()) {

			// Note: is it better off doing the actualHarvest computation once a day or
			// every time
			// Reset the daily work counter currentPhaseWorkCompleted back to zero
			// currentPhaseWorkCompleted = 0D;
			cumulativeDailyPAR = 0;
			
			return true;
		}

		return false;
	}
 
	public void resetPAR() {
		cumulativeDailyPAR = 0;
	}
	
	public double getLightingPower() {
		return lightingPower;
	}
	
	/**
	 * Turns on lighting.
	 *
	 * @param kW
	 */
	private void turnOnLighting(double kW) {
		lightingPower = kW;
	}

	/**
	 * Turns off lighting.
	 */
	private void turnOffLighting() {
		lightingPower = 0;
	}

	/**
	 * Computes the effects of the available sunlight and artificial light.
	 *
	 * @param time
	 * @param solarIrradiance
	 * @return instantaneous PAR or uPAR
	 */
	private double computeLight(ClockPulse pulse, double time, double solarIrradiance) {
		double lightModifier = 0;

		// Note : The average PAR is estimated to be 20.8 mol/(m² day) (Gertner, 1999)
		// Calculate instantaneous PAR from solar irradiance
		double uPAR = wattToPhotonConversionRatio * solarIrradiance;
		// [umol /m^2 /s] = [u mol /m^2 /s /(Wm^-2)] * [Wm^-2]
		double PARInterval = uPAR / 1_000_000D * time * MarsTime.SECONDS_PER_MILLISOL; // in mol / m^2 within this

		double dailyPARRequired = dailyPAR;
		// period of time
		// [mol /m^2] = [umol /m^2 /s] / u * [millisols] * [s /millisols]
		// 1 u = 1 micro = 1/1_000_000
		// Note : daily-PAR has the unit of [mol /m^2 /day]
		// Gauge if there is enough sunlight
//		double progress = cumulativeDailyPAR / dailyPARRequired; // [max is 1]
//		double clock = time / 1000D; // [max is 1]

		// When enough PAR have been administered to the crop, HPS_LAMP will turn off.
		
		// Future: what if the time zone of a settlement causes sunlight to shine at near
		// the tail end of the currentMillisols time ?
		
		// Compared cumulativeDailyPAR / dailyPARRequired vs. current time /
		// 1000D
		
		// Reduce the frequent toggling on and off of lamp and to check on
		// the time of day to anticipate the need of sunlight.
//		double millisols = pulse.getMarsTime().getMillisol();
//		if (0.5 * progress < clock && millisols <= 333 || 0.7 * progress < clock 
//				&& millisols > 333 && millisols <= 666
//				|| progress < clock && millisols > 666) {
			// Future: also compare also how much more sunlight will still be available
			if (uPAR > 40) { // if sunlight is available
				turnOffLighting();
				cumulativeDailyPAR = cumulativeDailyPAR + PARInterval;
				// Gets the effectivePAR
				effectivePAR = PARInterval;
			}

			else { // if no sunlight, turn on artificial lighting
					// double conversionFactor = 1000D * wattToPhotonConversionRatio /
					// MarsTime.SECONDS_IN_MILLISOL ;
					// DLI is Daily Light Integral is the unit for for cumulative light -- the
					// accumulation of all the PAR received during a day.
				double DLI = dailyPARRequired - cumulativeDailyPAR; // [in mol / m^2 / day]
				double deltaPAROutstanding = DLI * (time / 1000D) * tankArea;
				// in mol needed at this delta time [mol] = [mol /m^2 /day] * [millisol] /
				// [millisols /day] * m^2
				double deltakW = deltaPAROutstanding / time / conversionFactor;
				// [kW] = [mol] / [u mol /m^2 /s /(Wm^-2)] / [millisols] / [s /millisols] = [W
				// /u] * u * k/10e-3 = [kW]; since 1 u = 10e-6
				
				// Future: Typically, 5 lamps per square meter for a level of ~1000 mol/ m^2 /s
				// Added PHYSIOLOGICAL_LIMIT sets a realistic limit for tuning how
				// much PAR a food crop can absorb per frame.
				
				// Note 1 : PHYSIOLOGICAL_LIMIT minimize too many lights turned on and off too
				// frequently
				
				// Note 2 : It serves to smooth out the instantaneous power demand over a period
				// of time
				
				// each HPS_LAMP lamp supplies 400W has only 40% visible radiation efficiency
				int numLamp = (int) (Math.ceil(
						deltakW / KW_PER_HPS / VISIBLE_RADIATION_HPS / (1 - BALLAST_LOSS_HPS) * PHYSIOLOGICAL_LIMIT));
				// Future: should also allow the use of LED_KIT for lighting
				// For converting lumens to PAR/PPF, see
				// http://www.thctalk.com/cannabis-forum/showthread.php?55580-Converting-lumens-to-PAR-PPF
				// Note: do NOT include any losses below
				double supplykW = numLamp * KW_PER_HPS * VISIBLE_RADIATION_HPS * (1 - BALLAST_LOSS_HPS)
						/ PHYSIOLOGICAL_LIMIT;
				turnOnLighting(supplykW);
				double deltaPARSupplied = supplykW * time * conversionFactor / tankArea; // in mol / m2
				// [ mol / m^2] = [kW] * [u mol /m^2 /s /(Wm^-2)] * [millisols] * [s /millisols]
				// / [m^2] = k u mol / W / m^2 * (10e-3 / u / k) = [mol / m^-2]
				cumulativeDailyPAR = cumulativeDailyPAR + deltaPARSupplied + PARInterval;
				// [mol /m^2 /d]

				// Gets the effectivePAR
				effectivePAR = deltaPARSupplied + PARInterval;
			}

		// check for the passing of each day
		int newSol = pulse.getMarsTime().getMissionSol();
		// the crop has memory of the past lighting condition
		lightModifier = cumulativeDailyPAR / (dailyPARRequired + .0001) * 1000D / ( time  + .0001);
		// Future: If too much light, the crop's health may suffer unless a person comes
		// to intervene
		if (newSol == 1) {
			// if this crop is generated at the start of the sim,
			// lightModifier should start from 1, rather than 0
			lightModifier = 1;
		}

		adjustEnvironmentFactor(lightModifier, LIGHT_FACTOR);

		return uPAR;
	}
	
	/**
	 * Gets the amount of power required when function is at full power.
	 * 
	 * @return power (kW)
	 */
	@Override
	public double getFullPowerRequired() {
		// Power (kW) required for normal operations.
		return waterMass * POWER_PER_LITRE 
				+ getCurrentAlgae() * POWER_PER_KG_ALGAE 
				+ getFoodMass() * POWER_PER_KG_FOOD
				+ getLightingPower();
	}

	/**
	 * Gets the amount of power required when function is at power down level.
	 * 
	 * @return power (kW)
	 */
	@Override
	public double getPoweredDownPowerRequired() {
		return getFullPowerRequired() * .1;
	}

	@Override
	public double getMaintenanceTime() {
		return tankSize * 5D;
	}

	public String getUninspected() {
		return houseKeeping.getLeastInspected();
	}

	public String getUncleaned() {
		return houseKeeping.getLeastCleaned();
	}

	public double getInspectionScore() {
		return houseKeeping.getAverageInspectionScore();
	}
	
	public double getCleaningScore() {
		return houseKeeping.getAverageCleaningScore();
	}
	
	public void markInspected(String s, double value) {
		// Record the work time
		addCumulativeWorkTime(value);
		
		houseKeeping.inspected(s, value);
	}

	public void markCleaned(String s, double value) {
		// Record the work time
		addCumulativeWorkTime(value);
		
		houseKeeping.cleaned(s, value);
	}
	
	public double getCurrentAlgae() {
		return currentAlgae;
	}

	public double getIdealAlgae() {
		return idealAlgae;
	}
	
	public double getMaxAlgae() {
		return maxAlgae;
	}
	
	public double getTankSize() {
		return tankSize;
	}
	
	public double getWaterMass() {
		return waterMass;
	}
	
	public double getFoodMass() {
		return currentFood;
	}

	/**
	 * Gets the ratio of current algae mass to ideal algae mass.
	 * Note: this ratio should be open for player to adjust.
	 * 
	 * @return
	 */
	public double getSurplusRatio() {
		return currentAlgae / idealAlgae;
	}

	/**
	 * Spends some time on tending algae.
	 * 
	 * @param workTime
	 * @return
	 */
	public double tending(double workTime) {
		double surplus = 0;
		
		// Record the work time
		addCumulativeWorkTime(workTime);
		
		if (getCurrentNutrientRatio() < NUTRIENT_RATIO) {
			currentFood += workTime * ADD_FOOD_RATE * NUTRIENT_RATIO / getCurrentNutrientRatio();
		}

		tendertime -= workTime;

		if (tendertime < 0) {
			surplus = Math.abs(tendertime);
			tendertime = currentFood * TEND_TIME_FOR_FOOD;
			logger.log(building, Level.INFO, 10_000, 
					"Algae fully tended for " 
					+ Math.round(tendertime * 100.0)/100.0 + " millisols.");
			foodAge = 0;
		}
		
		return surplus;
	}


	/**
	 * Harvests some algae.
	 *  
	 * @param worker
	 * @param workTime
	 * @return spirulinaExtracted
	 */
	public double harvestAlgae(Worker worker, double workTime) {
		if (getSurplusRatio() < 0.5) {
			return 0;
		}

		// With adequate amount of sunshine, it takes a minimum of 10 to 15 days time 
		// for the spirulina to develop.
		// https://krishijagran.com/agripedia/all-about-organic-spirulina-cultivation-basic-requirements-water-quality-nutrient-requirements-economics-much-more/
		
		addCumulativeWorkTime(workTime);
		
		// Should define the algae liquid flow rate, not currentAlgae
				
		// Harvesting a certain amount (~ 1 kg)
		double harvestedWetBiomass = RandomUtil.getRandomDouble(.9, 1.1)
				* EXPECTED_YIELD_RATE * waterMass * workTime / 1000;
		
		logger.log(building, worker, Level.INFO, 5000, "harvestedWetBiomass: " 
				+ Math.round(harvestedWetBiomass * 100.0)/100.0 
				+ " kg algae. Pond stock: " +  Math.round(currentAlgae * 100.0)/100.0);
		
		if (currentAlgae < harvestedWetBiomass)
			return 0;
			
		currentAlgae = currentAlgae - harvestedWetBiomass;

		// Assuming the dry mass is ~15% 
		double spirulinaExtracted = harvestedWetBiomass 
				* RandomUtil.getRandomDouble(.1, .2) * health;
		
		// Future: specify harvesting equipment and sieving parameter
		
		// See https://grow-organic-spirulina.com/blog/the-complete-guide-to-harvesting-spirulina/
		// regarding the use of harvesting mesh to filter spirulina
		
		double filteredMass = harvestedWetBiomass - spirulinaExtracted;
		
		double returnAlgae = filteredMass * .8;
		
		double greyWaterWaste = filteredMass * .15;
		
		double foodWaste = filteredMass * .05;
		
		currentAlgae += returnAlgae;

		store(foodWaste, ResourceUtil.foodWasteID, "AlgaeFarming::foodWaste");
		
		storeGreyWater(greyWaterWaste, GREY_WATER_ID);

		// Record the grey water amount
		addResourceLog(greyWaterWaste, GREY_WATER_ID);
		
		store(spirulinaExtracted, HARVESTED_ALGAE_ID, "AlgaeFarming::harvestAlgae");
		// Record the harvest amount
		addResourceLog(spirulinaExtracted, HARVESTED_ALGAE_ID);

		logger.log(building, worker, Level.INFO, 5000, "spirulinaExtracted: " 
				+ Math.round(spirulinaExtracted * 100.0)/100.0 
				+ " kg algae.");
		
		
		return spirulinaExtracted;
	}


	/**
	 * What is algae's demand for food nutrient ?
	 * 
	 * @return
	 */
	public double getNutrientDemand() {
		return foodAge / NUTRIENT_DEMAND;
	}
	
	public double getCurrentNutrientRatio() {
		return currentFood / currentAlgae;
	}
	
	/**
	 * Reloads instances after loading from a saved sim.
	 * 
	 * @param cropConfig2
	 *
	 * @param {@link MasterClock}
	 * @param {{@link MarsClock}
	 */
	public static void initializeInstances(CropConfig cropConfig2) {
		cropConfig = cropConfig2;
	}
	
	/**
	 * Prepares object for garbage collection.
	 */
	public void destroy() {
		resourceLog = null;
		houseKeeping = null;
	}
}
