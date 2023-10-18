/*
 * Mars Simulation Project
 * PersonConfig.java
 * @date 2021-09-04
 * @author Scott Davis
 */
package com.mars_sim.core.person;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jdom2.Document;
import org.jdom2.Element;
import com.mars_sim.core.configuration.ConfigHelper;
import com.mars_sim.core.person.ai.role.RoleType;
import com.mars_sim.core.person.ai.training.TrainingType;

/**
 * Provides configuration information about people units. Uses a JDOM document
 * to get the information.
 */
public class PersonConfig {

	// Key class to combine role & training types
	private static final record KeyClass(RoleType r, TrainingType t) {};

	// Element names
	private static final String NAME = "name";

	private static final String PERSON_ATTRIBUTES = "person-attributes";

	private static final String BASE_CAPACITY = "base-carrying-capacity";
	private static final String AVERAGE_TALL_HEIGHT = "average-tall-height";// 176.5;
	private static final String AVERAGE_SHORT_HEIGHT = "average-short-height";// 162.5;

	private static final String AVERAGE_HIGH_WEIGHT = "average-high-weight";// 68.5;
	private static final String AVERAGE_LOW_WEIGHT = "average-low-weight";

	private static final String LOW_O2_RATE = "low-activity-metaboic-load-o2-consumption-rate";
	private static final String NOMINAL_O2_RATE = "nominal-activity-metaboic-load-o2-consumption-rate";
	private static final String HIGH_O2_RATE = "high-activity-metaboic-load-o2-consumption-rate";

	private static final String CO2_EXPELLED_RATE = "co2-expelled-rate";

	private static final String WATER_CONSUMPTION_RATE = "water-consumption-rate";
	private static final String WATER_USAGE_RATE = "water-usage-rate";
	private static final String GREY_TO_BLACK_WATER_RATIO = "grey-to-black-water-ratio";

	private static final String FOOD_CONSUMPTION_RATE = "food-consumption-rate";
	private static final String DESSERT_CONSUMPTION_RATE = "dessert-consumption-rate";

	private static final String OXYGEN_DEPRIVATION_TIME = "oxygen-deprivation-time";
	private static final String WATER_DEPRIVATION_TIME = "water-deprivation-time";
	private static final String FOOD_DEPRIVATION_TIME = "food-deprivation-time";

	private static final String DEHYDRATION_START_TIME = "dehydration-start-time";
	private static final String STARVATION_START_TIME = "starvation-start-time";

	private static final String MIN_AIR_PRESSURE = "min-air-pressure";
	private static final String MIN_O2_PARTIAL_PRESSURE = "min-o2-partial-pressure";

	private static final String MIN_TEMPERATURE = "min-temperature";
	private static final String MAX_TEMPERATURE = "max-temperature";

	private static final String DECOMPRESSION_TIME = "decompression-time";
	private static final String FREEZING_TIME = "freezing-time";

	private static final String STRESS_BREAKDOWN_CHANCE = "stress-breakdown-chance";
	private static final String HIGH_FATIGUE_COLLAPSE = "high-fatigue-collapse-chance";

	private static final String PERSONALITY_TYPES = "personality-types";
	private static final String MBTI = "mbti";

	private static final String TYPE = "type";
	private static final String VALUE = "value";

	private static final String PERCENTAGE = "percentage";

	/** Default Country to use for name creation */
	private static final String TRAINING_LIST = "training-list";
	private static final String TRAINING = "training";
	private static final String BENEFITS = "benefit";
	private static final String MODIFIER = "modifier";
	private static final String ROLE = "role";

	/** The base load-carrying capacity. */
	private transient double baseCap = -1;
	/** The upper and lower height. */
	private transient double[] height = new double[] { -1, -1 };
	/** The high and lor weight. */
	private transient double[] weight = new double[] { -1, -1 };
	/** The 3 types of metabolic loads. */
	private transient double[] o2ConsumptionRate = new double[] { -1, -1, -1 };
	/** The consumption rate for water, dessert, food. */
	private transient double[] consumptionRates = new double[] { -1, -1, -1 };
	/** The stress breakdown and high fatigue collapse chance. */
	private transient double[] chance = new double[] { -1, -1 };
	/** Various time values. */
	private transient double[] time = new double[] { -1, -1, -1, -1, -1, -1, -1 };
	/** The min and max temperature. */
	private transient double[] temperature = new double[] { -1, -1 };
	/** The grey2BlackWater ratio. */
	private transient double grey2BlackWaterRatio = -1;
	/** The average rate of water usage [kg/sol]. */
	private transient double waterUsage = -1;
	/** The min air pressure [kPa]. */
	private transient double pressure = -1;
	/** The min o2 partial pressure [kPa]. */
	private transient double o2pressure = -1;
	/** The co2 expulsion rate [kg/sol]. */
	private transient double co2Rate = -1;

	/** The personality distribution map. */
	private transient Map<String, Double> personalityDistribution;

	private transient Commander commander;

	private transient Map<String, String> personAttributes = new HashMap<>();
	private Map<KeyClass, Integer> trainingMods = new HashMap<>();


	/**
	 * Constructor.
	 * 
	 * @param personDoc the person config DOM document.
	 */
	public PersonConfig(Document personDoc) {
		commander = new Commander();

		parsePersonAttrs(personDoc);
		createPersonalityDistribution(personDoc);
		loadTrainingMods(personDoc);
	}

	private void loadTrainingMods(Document doc) {
		Element trainingListEl = doc.getRootElement().getChild(TRAINING_LIST);
		List<Element> trainingsList = trainingListEl.getChildren(TRAINING);
		for (Element trainingElement : trainingsList) {
			String trainingName = trainingElement.getAttributeValue(NAME);
			TrainingType tType = TrainingType.valueOf(ConfigHelper.convertToEnumName(trainingName));
			for(Element benefit : trainingElement.getChildren(BENEFITS)) {
				RoleType rType = RoleType.valueOf(ConfigHelper.convertToEnumName(benefit.getAttributeValue(ROLE)));
				int mod = ConfigHelper.getOptionalAttributeInt(benefit, MODIFIER, 0);
				trainingMods.put(new KeyClass(rType, tType), mod);
			}
		}
	}

	private void parsePersonAttrs(Document personDoc) {
		// Scan the attributes
		Element personAttributeEl = personDoc.getRootElement().getChild(PERSON_ATTRIBUTES);
		for (Element personAttr : personAttributeEl.getChildren()) {
			String str = personAttr.getAttributeValue(VALUE);

			personAttributes.put(personAttr.getName(), str);
		}
	}

	/**
	 * Gets the base load capacity of a person.
	 * 
	 * @return capacity in kg
	 */
	public double getBaseCapacity() {
		if (baseCap >= 0)
			return baseCap;
		else {
			baseCap = getValueAsDouble(BASE_CAPACITY);
			return baseCap;
		}
	}

	/**
	 * Gets the upper average height of a person.
	 * 
	 * @return height in cm
	 */
	public double getTallAverageHeight() {
		double r = height[0];
		if (r >= 0)
			return r;
		else {
			r = getValueAsDouble(AVERAGE_TALL_HEIGHT);
			height[0] = r;
			return r;
		}
	}

	/**
	 * Gets the lower average height of a person.
	 * 
	 * @return height in cm
	 */
	public double getShortAverageHeight() {
		double r = height[1];
		if (r >= 0)
			return r;
		else {
			r = getValueAsDouble(AVERAGE_SHORT_HEIGHT);
			height[1] = r;
			return r;
		}
	}

	/**
	 * Gets the high average weight of a person.
	 * 
	 * @return weight in kg
	 */
	public double getHighAverageWeight() {
		double r = weight[0];
		if (r >= 0)
			return r;
		else {
			r = getValueAsDouble(AVERAGE_HIGH_WEIGHT);
			weight[0] = r;
			return r;
		}
	}

	/**
	 * Gets the low average weight of a person.
	 * 
	 * @return weight in kg
	 */
	public double getLowAverageWeight() {
		double r = weight[1];
		if (r >= 0)
			return r;
		else {
			r = getValueAsDouble(AVERAGE_LOW_WEIGHT);
			weight[1] = r;
			return r;
		}
	}

	/**
	 * Gets the nominal oxygen consumption rate.
	 * 
	 * @return oxygen rate (kg/sol)
	 * @throws Exception if consumption rate could not be found.
	 */
	public double getNominalO2ConsumptionRate() {
		double r = o2ConsumptionRate[1];
		if (r >= 0)
			return r;
		else {
			r = getValueAsDouble(NOMINAL_O2_RATE);
			o2ConsumptionRate[1] = r;
			return r;
		}
	}

	/**
	 * Gets the low oxygen consumption rate.
	 * 
	 * @return oxygen rate (kg/sol)
	 * @throws Exception if consumption rate could not be found.
	 */
	public double getLowO2ConsumptionRate() {
		double r = o2ConsumptionRate[0];
		if (r >= 0)
			return r;
		else {
			r = getValueAsDouble(LOW_O2_RATE);
			o2ConsumptionRate[0] = r;
			return r;
		}
	}

	/**
	 * Gets the high oxygen consumption rate.
	 * 
	 * @return oxygen rate (kg/sol)
	 * @throws Exception if consumption rate could not be found.
	 */
	public double getHighO2ConsumptionRate() {
		double r = o2ConsumptionRate[2];
		if (r >= 0)
			return r;
		else {
			r = getValueAsDouble(HIGH_O2_RATE);
			o2ConsumptionRate[2] = r;
			return r;
		}
	}

	/**
	 * Gets the carbon dioxide expelled rate.
	 * 
	 * @return carbon dioxide expelled rate (kg/sol)
	 * @throws Exception if consumption rate could not be found.
	 */
	public double getCO2ExpelledRate() {
		if (co2Rate >= 0)
			return co2Rate;
		else {
			co2Rate = getValueAsDouble(CO2_EXPELLED_RATE);
			return co2Rate;
		}
	}

	/**
	 * Gets the water consumption rate.
	 * 
	 * @return water rate (kg/sol)
	 * @throws Exception if consumption rate could not be found.
	 */
	public double getWaterConsumptionRate() {
		double r = consumptionRates[0];
		if (r >= 0)
			return r;
		else {
			r = getValueAsDouble(WATER_CONSUMPTION_RATE);
			consumptionRates[0] = r;
			return r;
		}
	}

	/**
	 * Gets the water usage rate.
	 * 
	 * @return water rate (kg/sol)
	 * @throws Exception if usage rate could not be found.
	 */
	public double getWaterUsageRate() {
		if (waterUsage >= 0)
			return waterUsage;
		else {
			waterUsage = getValueAsDouble(WATER_USAGE_RATE);
			return waterUsage;
		}
	}

	/**
	 * Gets the grey to black water ratio.
	 * 
	 * @return ratio
	 * @throws Exception if the ratio could not be found.
	 */
	public double getGrey2BlackWaterRatio() {
		if (grey2BlackWaterRatio < 0) {
			grey2BlackWaterRatio = getValueAsDouble(GREY_TO_BLACK_WATER_RATIO);
		}
		return grey2BlackWaterRatio;
	}

	/**
	 * Gets the food consumption rate.
	 * 
	 * @return food rate (kg/sol)
	 * @throws Exception if consumption rate could not be found.
	 */
	public double getFoodConsumptionRate() {
		double r = consumptionRates[2];
		if (r >= 0)
			return r;
		else {
			r = getValueAsDouble(FOOD_CONSUMPTION_RATE);
			consumptionRates[2] = r;
			return r;
		}
	}

	/**
	 * Gets the dessert consumption rate.
	 * 
	 * @return dessert rate (kg/sol)
	 * @throws Exception if consumption rate could not be found.
	 */
	public double getDessertConsumptionRate() {
		double r = consumptionRates[1];
		if (r >= 0)
			return r;
		else {
			r = getValueAsDouble(DESSERT_CONSUMPTION_RATE);
			consumptionRates[1] = r;
			return r;
		}
	}

	/**
	 * Gets the oxygen deprivation time.
	 * 
	 * @return oxygen time in millisols.
	 * @throws Exception if oxygen deprivation time could not be found.
	 */
	public double getOxygenDeprivationTime() {
		double r = time[0];
		if (r >= 0)
			return r;
		else {
			r = getValueAsDouble(OXYGEN_DEPRIVATION_TIME);
			time[0] = r;
			return r;
		}
	}

	/**
	 * Gets the water deprivation time.
	 * 
	 * @return water time in sols.
	 * @throws Exception if water deprivation time could not be found.
	 */
	public double getWaterDeprivationTime() {
		double r = time[1];
		if (r >= 0)
			return r;
		else {
			r = getValueAsDouble(WATER_DEPRIVATION_TIME);
			time[1] = r;
			return r;
		}
	}

	/**
	 * Gets the dehydration start time.
	 * 
	 * @return dehydration time in sols.
	 * @throws Exception if dehydration start time could not be found.
	 */
	public double getDehydrationStartTime() {
		double r = time[2];
		if (r >= 0)
			return r;
		else {
			r = getValueAsDouble(DEHYDRATION_START_TIME);
			time[2] = r;
			return r;
		}
	}

	/**
	 * Gets the food deprivation time.
	 * 
	 * @return food time in sols.
	 * @throws Exception if food deprivation time could not be found.
	 */
	public double getFoodDeprivationTime() {
		double r = time[3];
		if (r >= 0)
			return r;
		else {
			r = getValueAsDouble(FOOD_DEPRIVATION_TIME);
			time[3] = r;
			return r;
		}
	}

	/**
	 * Gets the starvation start time.
	 * 
	 * @return starvation time in sols.
	 * @throws Exception if starvation start time could not be found.
	 */
	public double getStarvationStartTime() {
		double r = time[4];
		if (r >= 0)
			return r;
		else {
			r = getValueAsDouble(STARVATION_START_TIME);
			time[4] = r;
			return r;
		}
	}

	/**
	 * Gets the minimum air pressure.
	 * 
	 * @return air pressure in kPa.
	 * @throws Exception if air pressure could not be found.
	 */
	public double getMinAirPressure() {
		if (pressure >= 0)
			return pressure;
		else {
			pressure = getValueAsDouble(MIN_AIR_PRESSURE);
			return pressure;
		}
	}

	/**
	 * Gets the absolute minimum oxygen partial pressure of a spacesuit.
	 * 
	 * @return partial pressure in kPa.
	 * @throws Exception if air pressure could not be found.
	 */
	public double getMinSuitO2Pressure() {
		if (o2pressure >= 0)
			return o2pressure;
		else {
			o2pressure = getValueAsDouble(MIN_O2_PARTIAL_PRESSURE);
			return o2pressure;
		}
	}

	/**
	 * Gets the max decompression time a person can survive.
	 * 
	 * @return decompression time in millisols.
	 * @throws Exception if decompression time could not be found.
	 */
	public double getDecompressionTime() {
		double r = time[5];
		if (r >= 0)
			return r;
		else {
			r = getValueAsDouble(DECOMPRESSION_TIME);
			time[5] = r;
			return r;
		}
	}

	/**
	 * Gets the minimum temperature a person can tolerate.
	 * 
	 * @return temperature in celsius
	 * @throws Exception if min temperature cannot be found.
	 */
	public double getMinTemperature() {
		double r = temperature[0];
		if (r >= 0)
			return r;
		else {
			r = getValueAsDouble(MIN_TEMPERATURE);
			temperature[0] = r;
			return r;
		}
	}

	/**
	 * Gets the maximum temperature a person can tolerate.
	 * 
	 * @return temperature in celsius
	 * @throws Exception if max temperature cannot be found.
	 */
	public double getMaxTemperature() {
		double r = temperature[1];
		if (r >= 0)
			return r;
		else {
			r = getValueAsDouble(MAX_TEMPERATURE);
			temperature[1] = r;
			return r;
		}
	}

	/**
	 * Gets the time a person can survive below minimum temperature.
	 * 
	 * @return freezing time in millisols.
	 * @throws Exception if freezing time could not be found.
	 */
	public double getFreezingTime() {
		double r = time[6];
		if (r >= 0)
			return r;
		else {
			r = getValueAsDouble(FREEZING_TIME);
			time[6] = r;
			return r;
		}
	}

	/**
	 * Gets the base percent chance that a person will have a stress breakdown when
	 * at maximum stress.
	 * 
	 * @return percent chance of a breakdown per millisol.
	 * @throws Exception if stress breakdown time could not be found.
	 */
	public double getStressBreakdownChance() {
		double r = chance[0];
		if (r >= 0)
			return r;
		else {
			r = getValueAsDouble(STRESS_BREAKDOWN_CHANCE);
			chance[0] = r;
			return r;
		}
	}

	/**
	 * Gets the base percent chance that a person will collapse under high fatigue.
	 * 
	 * @return percent chance of a collapse per millisol.
	 * @throws Exception if collapse time could not be found.
	 */
	public double getHighFatigueCollapseChance() {
		double r = chance[1];
		if (r >= 0)
			return r;
		else {
			r = getValueAsDouble(HIGH_FATIGUE_COLLAPSE);
			chance[1] = r;
			return r;
		}
	}

	/**
	 * Gets the average percentages for personality types
	 * 
	 * @param personalityDistribution map
	 */
	public Map<String, Double> loadPersonalityDistribution() {
		return personalityDistribution;
	}

	/**
	 * Loads the average percentages for personality types into a map.
	 * 
	 * @throws Exception if personality type cannot be found or percentages don't
	 *                   add up to 100%.
	 */
	private void createPersonalityDistribution(Document personDoc) {
		personalityDistribution = new HashMap<>();

		double total = 0D;

		Element personalityTypeList = personDoc.getRootElement().getChild(PERSONALITY_TYPES);
		List<Element> personalityTypes = personalityTypeList.getChildren(MBTI);

		for (Element mbtiElement : personalityTypes) {
			String type = mbtiElement.getAttributeValue(TYPE);
			double result = Double.parseDouble(mbtiElement.getAttributeValue(PERCENTAGE));

			personalityDistribution.put(type, result);
			total += result;
		}

		if (total != 100D)
			throw new IllegalStateException(
					"PersonalityType.loadPersonalityTypes(): percentages don't add up to 100%. (total: " + total + ")");
	}

	/**
	 * Gets the value of an element as a double
	 * 
	 * @param an element
	 * 
	 * @return a double
	 */
	private double getValueAsDouble(String attr) {
		String str = personAttributes.get(attr);
		return Double.parseDouble(str);
	}

	/**
	 * Get the Commander's profile
	 * 
	 * @return profile
	 */
	public Commander getCommander() {
		return commander;
	}

	/**
	 * Finds the training modifiers for a combination of training and role.
	 * 
	 * @param role
	 * @param tt
	 * @return
	 */
	public int getTrainingModifier(RoleType role, TrainingType tt) {

		// lookup in modifier table
		KeyClass k = new KeyClass(role, tt);
		Integer v = trainingMods.get(k);
		if (v == null) {
			return 0;
		}
		else {
			return v;
		}
	}
}
