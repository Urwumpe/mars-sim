/**
 * Mars Simulation Project
 * PersonConfig.java
 * @version 3.1.0 2017-01-24
 * @author Scott Davis
 */
package org.mars_sim.msp.core.person;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.jdom2.Document;
import org.jdom2.Element;
import org.mars_sim.msp.core.reportingAuthority.ReportingAuthorityType;

/**
 * Provides configuration information about people units. Uses a JDOM document
 * to get the information.
 */
public class PersonConfig implements Serializable {

	/** default serial id. */
	private static final long serialVersionUID = 1L;

	// private static Logger logger = Logger.getLogger(PersonConfig.class.getName());

	public static final int SIZE_OF_CREW = 4;
	public static final int ALPHA_CREW = 0;;

	// Element names
	private static final String LAST_NAME_LIST = "last-name-list";
	private static final String FIRST_NAME_LIST = "first-name-list";
	private static final String LAST_NAME = "last-name";
	private static final String FIRST_NAME = "first-name";
	private static final String PERSON_NAME_LIST = "person-name-list";
	private static final String PERSON_NAME = "person-name";

	private static final String GENDER = "gender";

	private static final String SPONSOR = "sponsor";
	private static final String COUNTRY = "country";

	/** The base carrying capacity (kg) of a person. */
	private final static String BASE_CAPACITY = "base-carrying-capacity";
	private final static String AVERAGE_TALL_HEIGHT = "average-tall-height";//176.5;
	private final static String AVERAGE_SHORT_HEIGHT = "average-short-height";//162.5;
//	private final static String AVERAGE_HEIGHT = "average_height"; // 169.5;// (AVERAGE_TALL_HEIGHT + AVERAGE_SHORT_HEIGHT)/2D;

	private final static String AVERAGE_HIGH_WEIGHT = "average-high-weight";// 68.5;
	private final static String AVERAGE_LOW_WEIGHT = "average-low-weight";
//	private	final static String AVERAGE_WEIGHT = "average_low_weight"; //62.85;
	
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
	private static final String DECOMPRESSION_TIME = "decompression-time";
	private static final String MIN_TEMPERATURE = "min-temperature";
	private static final String MAX_TEMPERATURE = "max-temperature";
	private static final String FREEZING_TIME = "freezing-time";
	private static final String STRESS_BREAKDOWN_CHANCE = "stress-breakdown-chance";
	private static final String HIGH_FATIGUE_COLLAPSE = "high-fatigue-collapse-chance";

	private static final String GENDER_MALE_PERCENTAGE = "gender-male-percentage";
	private static final String PERSONALITY_TYPES = "personality-types";
	private static final String MBTI = "mbti";

	private static final String TYPE = "type";
	private static final String VALUE = "value";

	private static final String PERCENTAGE = "percentage";

	/** The base load-carrying capacity. */
	private static double baseCap = 0;
	/** The upper and lower height. */
	private static double[] height = new double[] { 0, 0 };
	/** The high and lor weight. */
	private static double[] weight = new double[] { 0, 0 };
	/** The 3 types of metabolic loads. */
	private static double[] o2ConsumptionRate = new double[] { 0, 0, 0 };
	/** The consumption rate for water, dessert, food. */
	private static double[] consumptionRates = new double[] { 0, 0, 0 };
	/** The grey2BlackWaterRatio and the gender ratio. */
	private static double[] ratio = new double[] { 0, 0 };
	/** The stress breakdown and high fatigue collapse chance. */
	private static double[] chance = new double[] { 0, 0 };
	/** Various time values. */
	private static double[] time = new double[] { 0, 0, 0, 0, 0, 0, 0 };
	/** The min and max temperature. */
	private static double[] temperature = new double[] { 0, 0 };

	private static double waterUsage = 0;

	private static double pressure = 0;

//	private static Document personDoc;
	private static Element root;

	private static Map<String, Double> personalityDistribution;

	private static List<String> personNameList;
	private static List<String> allCountries;
	private static List<String> ESACountries;
	private static List<String> sponsors;
	private static List<String> longSponsors;
	
	private static List<Map<Integer, List<String>>> lastNames;
	private static List<Map<Integer, List<String>>> firstNames;

	private static Commander commander;

	/**
	 * Constructor
	 * 
	 * @param personDoc the person config DOM document.
	 */
	public PersonConfig(Document personDoc) {
		root = personDoc.getRootElement();
		commander = new Commander();

		getPersonNameList();
		retrieveLastNameList();
		retrieveFirstNameList();
		createPersonalityDistribution();

	}

	/**
	 * Gets a list of person names for settlers.
	 * 
	 * @return List of person names.
	 * @throws Exception if person names could not be found.
	 */
	public List<String> getPersonNameList() {

		if (personNameList == null) {
			personNameList = new ArrayList<String>();
			Element personNameEl = root.getChild(PERSON_NAME_LIST);
			List<Element> personNames = personNameEl.getChildren(PERSON_NAME);

			for (Element nameElement : personNames) {
				personNameList.add(nameElement.getAttributeValue(VALUE));
			}
		}

		return personNameList;
	}

	/**
	 * Retrieves a list of settlers' last names by sponsors and by countries.
	 * 
	 * @return List of last names.
	 * @throws Exception if last names could not be found.
	 */
	public List<Map<Integer, List<String>>> retrieveLastNameList() {

		if (lastNames == null) {
			lastNames = new ArrayList<Map<Integer, List<String>>>();

			List<List<String>> sponsors = new ArrayList<>();
			for (int i = 0; i < 9; i++) {
				List<String> list = new ArrayList<String>();
				sponsors.add(list);
			}

			// Add lists for countries
			List<List<String>> countries = new ArrayList<>();
			for (int i = 0; i < 28; i++) {
				List<String> countryList = new ArrayList<String>();
				countries.add(countryList);
			}

			// Element root = personDoc.getRootElement();
			Element lastNameEl = root.getChild(LAST_NAME_LIST);
			List<Element> lastNamesList = lastNameEl.getChildren(LAST_NAME);

			for (Element nameElement : lastNamesList) {

				String sponsor = nameElement.getAttributeValue(SPONSOR);
				String name = nameElement.getAttributeValue(VALUE);
				String country = nameElement.getAttributeValue(COUNTRY);

				if (ReportingAuthorityType.getType(sponsor) == ReportingAuthorityType.CNSA
						|| ReportingAuthorityType.getType(sponsor) == ReportingAuthorityType.CNSA_L)
					sponsors.get(0).add(name);
				else if (ReportingAuthorityType.getType(sponsor) == ReportingAuthorityType.CSA
						|| ReportingAuthorityType.getType(sponsor) == ReportingAuthorityType.CSA_L)
					sponsors.get(1).add(name);
				else if (ReportingAuthorityType.getType(sponsor) == ReportingAuthorityType.ISRO
						|| ReportingAuthorityType.getType(sponsor) == ReportingAuthorityType.ISRO_L)
					sponsors.get(2).add(name);
				else if (ReportingAuthorityType.getType(sponsor) == ReportingAuthorityType.JAXA
						|| ReportingAuthorityType.getType(sponsor) == ReportingAuthorityType.JAXA_L)
					sponsors.get(3).add(name);
				else if (ReportingAuthorityType.getType(sponsor) == ReportingAuthorityType.NASA
						|| ReportingAuthorityType.getType(sponsor) == ReportingAuthorityType.NASA_L)
					sponsors.get(4).add(name);
				else if (ReportingAuthorityType.getType(sponsor) == ReportingAuthorityType.RKA
						|| ReportingAuthorityType.getType(sponsor) == ReportingAuthorityType.RKA_L)
					sponsors.get(5).add(name);
				else if (ReportingAuthorityType.getType(sponsor) == ReportingAuthorityType.ESA
						|| ReportingAuthorityType.getType(sponsor) == ReportingAuthorityType.ESA_L)
					sponsors.get(6).add(name);
				else if (ReportingAuthorityType.getType(sponsor) == ReportingAuthorityType.MS
						|| ReportingAuthorityType.getType(sponsor) == ReportingAuthorityType.MARS_SOCIETY_L)
					sponsors.get(7).add(name);
				else if (ReportingAuthorityType.getType(sponsor) == ReportingAuthorityType.SPACEX
						|| ReportingAuthorityType.getType(sponsor) == ReportingAuthorityType.SPACEX_L)
					sponsors.get(8).add(name);

				/* CNSA,CSA,ISRO,JAXA,NASA,RKA */
				if (country.equals("China"))
					countries.get(0).add(name);
				else if (country.equals("Canada"))
					countries.get(1).add(name);
				else if (country.equals("India"))
					countries.get(2).add(name);
				else if (country.equals("Japan"))
					countries.get(3).add(name);
				else if (country.equals("USA"))
					countries.get(4).add(name);
				else if (country.equals("Russia"))
					countries.get(5).add(name);

				/*
				 * ESA has 22 Member States. The national bodies responsible for space in these
				 * countries sit on ESA�s governing Council: Austria, Belgium, Czech Republic,
				 * Denmark, Estonia, Finland, France, Germany, Greece, Hungary, Ireland, Italy,
				 * Luxembourg, The Netherlands, Norway, Poland, Portugal, Romania, Spain,
				 * Sweden, Switzerland and the United Kingdom.
				 */
				
				else if (country.equals("Austria"))
					countries.get(6).add(name);
				else if (country.equals("Belgium"))
					countries.get(7).add(name);
				else if (country.equals("Czech Republic"))
					countries.get(8).add(name);
				else if (country.equals("Denmark"))
					countries.get(9).add(name);
				else if (country.equals("Estonia"))
					countries.get(10).add(name);
				else if (country.equals("Finland"))
					countries.get(11).add(name);
				else if (country.equals("France"))
					countries.get(12).add(name);
				else if (country.equals("Germany"))
					countries.get(13).add(name);
				else if (country.equals("Greece"))
					countries.get(14).add(name);
				else if (country.equals("Hungary"))
					countries.get(15).add(name);
				else if (country.equals("Ireland"))
					countries.get(16).add(name);
				else if (country.equals("Italy"))
					countries.get(17).add(name);
				else if (country.equals("Luxembourg"))
					countries.get(18).add(name);
				else if (country.equals("The Netherlands"))
					countries.get(19).add(name);
				else if (country.equals("Norway"))
					countries.get(20).add(name);
				else if (country.equals("Poland"))
					countries.get(21).add(name);
				else if (country.equals("Portugal"))
					countries.get(22).add(name);
				else if (country.equals("Romania"))
					countries.get(23).add(name);
				else if (country.equals("Spain"))
					countries.get(24).add(name);
				else if (country.equals("Sweden"))
					countries.get(25).add(name);
				else if (country.equals("Switzerland"))
					countries.get(26).add(name);
				else if (country.equals("UK"))
					countries.get(27).add(name);

			}

			Map<Integer, List<String>> lastNamesBySponsor = new HashMap<>();
			Map<Integer, List<String>> lastNamesByCountry = new HashMap<>();

			for (int i = 0; i < 9; i++) {
				lastNamesBySponsor.put(i, sponsors.get(i));
			}

			for (int i = 0; i < 28; i++) {
				lastNamesByCountry.put(i, countries.get(i));
			}

			lastNames.add(lastNamesBySponsor);
			lastNames.add(lastNamesByCountry);

		}

		return lastNames;
	}

	/**
	 * Retrieves a list of settlers' male and female first names by sponsors and by
	 * countries.
	 * 
	 * @return List of first names.
	 * @throws Exception if first names could not be found.
	 */
	public List<Map<Integer, List<String>>> retrieveFirstNameList() {

		if (firstNames == null) {

			firstNames = new ArrayList<Map<Integer, List<String>>>();

			List<List<String>> malesBySponsor = new ArrayList<>();
			for (int i = 0; i < 9; i++) {
				List<String> list = new ArrayList<String>();
				malesBySponsor.add(list);
			}

			List<List<String>> femalesBySponsor = new ArrayList<>();
			for (int i = 0; i < 9; i++) {
				List<String> list = new ArrayList<String>();
				femalesBySponsor.add(list);
			}

			// Add lists for countries
			List<List<String>> malesByCountry = new ArrayList<>();
			for (int i = 0; i < 28; i++) {
				List<String> countryList = new ArrayList<String>();
				malesByCountry.add(countryList);
			}

			List<List<String>> femalesByCountry = new ArrayList<>();
			for (int i = 0; i < 28; i++) {
				List<String> countryList = new ArrayList<String>();
				femalesByCountry.add(countryList);
			}

			Element firstNameEl = root.getChild(FIRST_NAME_LIST);
			List<Element> firstNamesList = firstNameEl.getChildren(FIRST_NAME);

			for (Element nameElement : firstNamesList) {

				String gender = nameElement.getAttributeValue(GENDER);
				String sponsor = nameElement.getAttributeValue(SPONSOR);
				String name = nameElement.getAttributeValue(VALUE);
				String country = nameElement.getAttributeValue(COUNTRY);

				if (gender.equals("male")) {

					if (sponsor.contains("CNSA"))// && type[i] == ReportingAuthorityType.CNSA)
						malesBySponsor.get(0).add(name);

					else if (sponsor.contains("CSA"))// && type[i] == ReportingAuthorityType.CSA)
						malesBySponsor.get(1).add(name);

					else if (sponsor.contains("ISRO"))// && type[i] == ReportingAuthorityType.ISRO)
						malesBySponsor.get(2).add(name);
					
					else if (sponsor.contains("JAXA"))// && type[i] == ReportingAuthorityType.JAXA)
						malesBySponsor.get(3).add(name);

					else if (sponsor.contains("NASA"))// && type[i] == ReportingAuthorityType.NASA)
						malesBySponsor.get(4).add(name);

					else if (sponsor.contains("RKA"))// && type[i] == ReportingAuthorityType.RKA)
						malesBySponsor.get(5).add(name);

					else if (sponsor.contains("ESA"))// && type[i] == ReportingAuthorityType.ESA)
						malesBySponsor.get(5).add(name);

					else if (sponsor.contains("Mars Society")
							|| sponsor.contains("MS"))// && type[i] == ReportingAuthorityType.NASA)
						malesBySponsor.get(7).add(name);

					else if (sponsor.contains("SpaceX"))// && type[i] == ReportingAuthorityType.RKA)
						malesBySponsor.get(8).add(name);

					/* CNSA,CSA,ISRO,JAXA,NASA,RKA */
					if (country.equals("China"))
						malesByCountry.get(0).add(name);
					else if (country.equals("Canada"))
						malesByCountry.get(1).add(name);
					else if (country.equals("India"))
						malesByCountry.get(2).add(name);
					else if (country.equals("Japan"))
						malesByCountry.get(3).add(name);
					else if (country.equals("USA"))
						malesByCountry.get(4).add(name);
					else if (country.equals("Russia"))
						malesByCountry.get(5).add(name);

					/*
					 * ESA has 22 Member States. The national bodies responsible for space in these
					 * countries sit on ESA�s governing Council: Austria, Belgium, Czech Republic,
					 * Denmark, Estonia, Finland, France, Germany, Greece, Hungary, Ireland, Italy,
					 * Luxembourg, The Netherlands, Norway, Poland, Portugal, Romania, Spain,
					 * Sweden, Switzerland and the United Kingdom.
					 */
					else if (country.equals("Austria"))
						malesByCountry.get(6).add(name);
					else if (country.equals("Belgium"))
						malesByCountry.get(7).add(name);
					else if (country.equals("Czech Republic"))
						malesByCountry.get(8).add(name);
					else if (country.equals("Denmark"))
						malesByCountry.get(9).add(name);
					else if (country.equals("Estonia"))
						malesByCountry.get(10).add(name);
					else if (country.equals("Finland"))
						malesByCountry.get(11).add(name);
					else if (country.equals("France"))
						malesByCountry.get(12).add(name);
					else if (country.equals("Germany"))
						malesByCountry.get(13).add(name);
					else if (country.equals("Greece"))
						malesByCountry.get(14).add(name);
					else if (country.equals("Hungary"))
						malesByCountry.get(15).add(name);
					else if (country.equals("Ireland"))
						malesByCountry.get(16).add(name);
					else if (country.equals("Italy"))
						malesByCountry.get(17).add(name);
					else if (country.equals("Luxembourg"))
						malesByCountry.get(18).add(name);
					else if (country.equals("The Netherlands"))
						malesByCountry.get(19).add(name);
					else if (country.equals("Norway"))
						malesByCountry.get(20).add(name);
					else if (country.equals("Poland"))
						malesByCountry.get(21).add(name);
					else if (country.equals("Portugal"))
						malesByCountry.get(22).add(name);
					else if (country.equals("Romania"))
						malesByCountry.get(23).add(name);
					else if (country.equals("Spain"))
						malesByCountry.get(24).add(name);
					else if (country.equals("Sweden"))
						malesByCountry.get(25).add(name);
					else if (country.equals("Switzerland"))
						malesByCountry.get(26).add(name);
					else if (country.equals("UK"))
						malesByCountry.get(27).add(name);

				} else if (gender.equals("female")) {

					if (sponsor.contains("CNSA"))// && type[i] == ReportingAuthorityType.CNSA)
						femalesBySponsor.get(0).add(name);

					else if (sponsor.contains("CSA"))// && type[i] == ReportingAuthorityType.CSA)
						femalesBySponsor.get(1).add(name);

					else if (sponsor.contains("ISRO"))// && type[i] == ReportingAuthorityType.ISRO)
						femalesBySponsor.get(2).add(name);

					else if (sponsor.contains("JAXA"))// && type[i] == ReportingAuthorityType.JAXA)
						femalesBySponsor.get(3).add(name);

					else if (sponsor.contains("NASA"))// && type[i] == ReportingAuthorityType.NASA)
						femalesBySponsor.get(4).add(name);

					else if (sponsor.contains("RKA"))// && type[i] == ReportingAuthorityType.RKA)
						femalesBySponsor.get(5).add(name);

					else if (sponsor.contains("ESA"))// && type[i] == ReportingAuthorityType.ESA)
						femalesBySponsor.get(6).add(name);

					else if (sponsor.contains("Mars Society")
							|| sponsor.contains("MS"))// && type[i] == ReportingAuthorityType.NASA)
						femalesBySponsor.get(7).add(name);

					else if (sponsor.contains("SpaceX"))// && type[i] == ReportingAuthorityType.RKA)
						femalesBySponsor.get(8).add(name);

					/* CNSA,CSA,ISRO,JAXA,NASA,RKA */
					if (country.equals("China"))
						femalesByCountry.get(0).add(name);
					else if (country.equals("Canada"))
						femalesByCountry.get(1).add(name);
					else if (country.equals("India"))
						femalesByCountry.get(2).add(name);
					else if (country.equals("Japan"))
						femalesByCountry.get(3).add(name);
					else if (country.equals("USA"))
						femalesByCountry.get(4).add(name);
					else if (country.equals("Russia"))
						femalesByCountry.get(5).add(name);

					/*
					 * ESA has 22 Member States. The national bodies responsible for space in these
					 * countries sit on ESA�s governing Council: Austria, Belgium, Czech Republic,
					 * Denmark, Estonia, Finland, France, Germany, Greece, Hungary, Ireland, Italy,
					 * Luxembourg, The Netherlands, Norway, Poland, Portugal, Romania, Spain,
					 * Sweden, Switzerland and the United Kingdom.
					 */
					else if (country.equals("Austria"))
						femalesByCountry.get(6).add(name);
					else if (country.equals("Belgium"))
						femalesByCountry.get(7).add(name);
					else if (country.equals("Czech Republic"))
						femalesByCountry.get(8).add(name);
					else if (country.equals("Denmark"))
						femalesByCountry.get(9).add(name);
					else if (country.equals("Estonia"))
						femalesByCountry.get(10).add(name);
					else if (country.equals("Finland"))
						femalesByCountry.get(11).add(name);
					else if (country.equals("France"))
						femalesByCountry.get(12).add(name);
					else if (country.equals("Germany"))
						femalesByCountry.get(13).add(name);
					else if (country.equals("Greece"))
						femalesByCountry.get(14).add(name);
					else if (country.equals("Hungary"))
						femalesByCountry.get(15).add(name);
					else if (country.equals("Ireland"))
						femalesByCountry.get(16).add(name);
					else if (country.equals("Italy"))
						femalesByCountry.get(17).add(name);
					else if (country.equals("Luxembourg"))
						femalesByCountry.get(18).add(name);
					else if (country.equals("The Netherlands"))
						femalesByCountry.get(19).add(name);
					else if (country.equals("Norway"))
						femalesByCountry.get(20).add(name);
					else if (country.equals("Poland"))
						femalesByCountry.get(21).add(name);
					else if (country.equals("Portugal"))
						femalesByCountry.get(22).add(name);
					else if (country.equals("Romania"))
						femalesByCountry.get(23).add(name);
					else if (country.equals("Spain"))
						femalesByCountry.get(24).add(name);
					else if (country.equals("Sweden"))
						femalesByCountry.get(25).add(name);
					else if (country.equals("Switzerland"))
						femalesByCountry.get(26).add(name);
					else if (country.equals("UK"))
						femalesByCountry.get(27).add(name);

				}
			}

			Map<Integer, List<String>> maleFirstNamesBySponsor = new HashMap<>();
			Map<Integer, List<String>> femaleFirstNamesBySponsor = new HashMap<>();
			Map<Integer, List<String>> maleFirstNamesByCountry = new HashMap<>();
			Map<Integer, List<String>> femaleFirstNamesByCountry = new HashMap<>();

			for (int i = 0; i < 7; i++) {
				maleFirstNamesBySponsor.put(i, malesBySponsor.get(i));
				femaleFirstNamesBySponsor.put(i, femalesBySponsor.get(i));
			}

			firstNames.add(maleFirstNamesBySponsor);
			firstNames.add(femaleFirstNamesBySponsor);

			for (int i = 0; i < 28; i++) {
				maleFirstNamesByCountry.put(i, malesByCountry.get(i));
				femaleFirstNamesByCountry.put(i, femalesByCountry.get(i));
			}

			firstNames.add(maleFirstNamesByCountry);
			firstNames.add(femaleFirstNamesByCountry);

		}

		return firstNames;
	}

	/**
	 * Gets the sponsor of a given person name.
	 * 
	 * @param name the name of the person
	 * @return the sponsor of the person
	 */
	public ReportingAuthorityType getMarsSocietySponsor(String name) {
		ReportingAuthorityType type = null;

		Element personNameList = root.getChild(PERSON_NAME_LIST);
		List<Element> personNames = personNameList.getChildren(PERSON_NAME);
		for (Element nameElement : personNames) {
			String personName = nameElement.getAttributeValue(VALUE);
			String sponsor = null;
			if (personName.equals(name)) {
				sponsor = nameElement.getAttributeValue(SPONSOR);

				if (sponsor.contains("Mars Society") || sponsor.contains("MS"))
					type = ReportingAuthorityType.MS;

			}

		}

		return type;
	}

	/**
	 * Gets the gender of a given person name.
	 * 
	 * @param name the name of the person
	 * @return {@link GenderType} the gender of the person name
	 * @throws Exception if person names could not be found.
	 */
	public GenderType getPersonGender(String name) {
		GenderType result = GenderType.UNKNOWN;

		// Element root = personDoc.getRootElement();
		Element personNameList = root.getChild(PERSON_NAME_LIST);
		List<Element> personNames = personNameList.getChildren(PERSON_NAME);
		for (Element nameElement : personNames) {
			String personName = nameElement.getAttributeValue(VALUE);
			if (personName.equals(name))
				result = GenderType.valueOfIgnoreCase(nameElement.getAttributeValue(GENDER));
		}

		return result;
	}
	
	/**
	 * Gets the base load capacity of a person.
	 * 
	 * @return capacity in kg
	 */
	public double getBaseCapacity() {
		if (baseCap != 0)
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
		if (height[0] != 0)
			return height[0];
		else {
			height[0] = getValueAsDouble(AVERAGE_TALL_HEIGHT);
			return height[0];
		}
	}
	
	/**
	 * Gets the lower average height of a person.
	 * 
	 * @return height in cm
	 */
	public double getShortAverageHeight() {
		if (height[1] != 0)
			return height[1];
		else {
			height[1] = getValueAsDouble(AVERAGE_SHORT_HEIGHT);
			return height[1];
		}
	}
	
	
	
	
	/**
	 * Gets the high average weight of a person.
	 * 
	 * @return weight in kg
	 */
	public double getHighAverageWeight() {
		if (weight[0] != 0)
			return weight[0];
		else {
			weight[0] = getValueAsDouble(AVERAGE_HIGH_WEIGHT);
			return weight[0];
		}
	}
	
	/**
	 * Gets the low average weight of a person.
	 * 
	 * @return weight in kg
	 */
	public double getLowAverageWeight() {
		if (weight[1] != 0)
			return weight[1];
		else {
			weight[1] = getValueAsDouble(AVERAGE_LOW_WEIGHT);
			return weight[1];
		}
	}

	/**
	 * Gets the nominal oxygen consumption rate.
	 * 
	 * @return oxygen rate (kg/sol)
	 * @throws Exception if consumption rate could not be found.
	 */
	public double getNominalO2ConsumptionRate() {
		if (o2ConsumptionRate[1] != 0)
			return o2ConsumptionRate[1];
		else {
			o2ConsumptionRate[1] = getValueAsDouble(NOMINAL_O2_RATE);
			return o2ConsumptionRate[1];
		}
	}

	/**
	 * Gets the low oxygen consumption rate.
	 * 
	 * @return oxygen rate (kg/sol)
	 * @throws Exception if consumption rate could not be found.
	 */
	public double getLowO2ConsumptionRate() {
		if (o2ConsumptionRate[0] != 0)
			return o2ConsumptionRate[0];
		else {
			o2ConsumptionRate[0] = getValueAsDouble(LOW_O2_RATE);
			return o2ConsumptionRate[0];
		}
	}

	/**
	 * Gets the high oxygen consumption rate.
	 * 
	 * @return oxygen rate (kg/sol)
	 * @throws Exception if consumption rate could not be found.
	 */
	public double getHighO2ConsumptionRate() {
		if (o2ConsumptionRate[2] != 0)
			return o2ConsumptionRate[2];
		else {
			o2ConsumptionRate[2] = getValueAsDouble(HIGH_O2_RATE);
			return o2ConsumptionRate[2];
		}
	}

	/**
	 * Gets the carbon dioxide expelled rate.
	 * 
	 * @return carbon dioxide expelled rate (kg/sol)
	 * @throws Exception if consumption rate could not be found.
	 */
	public double getCO2ExpelledRate() {
		if (o2ConsumptionRate[2] != 0)
			return o2ConsumptionRate[2];
		else {
			o2ConsumptionRate[2] = getValueAsDouble(CO2_EXPELLED_RATE);
			return o2ConsumptionRate[2];
		}
	}

	/**
	 * Gets the water consumption rate.
	 * 
	 * @return water rate (kg/sol)
	 * @throws Exception if consumption rate could not be found.
	 */
	public double getWaterConsumptionRate() {
		if (consumptionRates[0] != 0)
			return consumptionRates[0];
		else {
			consumptionRates[0] = getValueAsDouble(WATER_CONSUMPTION_RATE);
			return consumptionRates[0];
		}
	}

	/**
	 * Gets the water usage rate.
	 * 
	 * @return water rate (kg/sol)
	 * @throws Exception if usage rate could not be found.
	 */
	public double getWaterUsageRate() {
		if (waterUsage != 0)
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
		if (ratio[0] != 0)
			return ratio[0];
		else {
			ratio[0] = getValueAsDouble(GREY_TO_BLACK_WATER_RATIO);
			return ratio[0];
		}
	}

	/**
	 * Gets the food consumption rate.
	 * 
	 * @return food rate (kg/sol)
	 * @throws Exception if consumption rate could not be found.
	 */
	public double getFoodConsumptionRate() {
		if (consumptionRates[2] != 0)
			return consumptionRates[2];
		else {
			consumptionRates[2] = getValueAsDouble(FOOD_CONSUMPTION_RATE);
			return consumptionRates[2];
		}
	}

	/**
	 * Gets the dessert consumption rate.
	 * 
	 * @return dessert rate (kg/sol)
	 * @throws Exception if consumption rate could not be found.
	 */
	public double getDessertConsumptionRate() {
		if (consumptionRates[1] != 0)
			return consumptionRates[1];
		else {
			consumptionRates[1] = getValueAsDouble(DESSERT_CONSUMPTION_RATE);
			return consumptionRates[1];
		}
	}

	/**
	 * Gets the oxygen deprivation time.
	 * 
	 * @return oxygen time in millisols.
	 * @throws Exception if oxygen deprivation time could not be found.
	 */
	public double getOxygenDeprivationTime() {
		if (time[0] != 0)
			return time[0];
		else {
			time[0] = getValueAsDouble(OXYGEN_DEPRIVATION_TIME);
			return time[0];
		}
	}

	/**
	 * Gets the water deprivation time.
	 * 
	 * @return water time in sols.
	 * @throws Exception if water deprivation time could not be found.
	 */
	public double getWaterDeprivationTime() {
		if (time[1] != 0)
			return time[1];
		else {
			time[1] = getValueAsDouble(WATER_DEPRIVATION_TIME);
			return time[1];
		}
	}

	/**
	 * Gets the dehydration start time.
	 * 
	 * @return dehydration time in sols.
	 * @throws Exception if dehydration start time could not be found.
	 */
	public double getDehydrationStartTime() {
		if (time[2] != 0)
			return time[2];
		else {
			time[2] = getValueAsDouble(DEHYDRATION_START_TIME);
			return time[2];
		}
	}

	/**
	 * Gets the food deprivation time.
	 * 
	 * @return food time in sols.
	 * @throws Exception if food deprivation time could not be found.
	 */
	public double getFoodDeprivationTime() {
		if (time[3] != 0)
			return time[3];
		else {
			time[3] = getValueAsDouble(FOOD_DEPRIVATION_TIME);
			return time[3];
		}
	}

	/**
	 * Gets the starvation start time.
	 * 
	 * @return starvation time in sols.
	 * @throws Exception if starvation start time could not be found.
	 */
	public double getStarvationStartTime() {
		if (time[4] != 0)
			return time[4];
		else {
			time[4] = getValueAsDouble(STARVATION_START_TIME);
			return time[4];
		}
	}

	/**
	 * Gets the required air pressure.
	 * 
	 * @return air pressure in kPa.
	 * @throws Exception if air pressure could not be found.
	 */
	public double getMinAirPressure() {
		if (pressure != 0)
			return pressure;
		else {
			pressure = getValueAsDouble(MIN_AIR_PRESSURE);
			return pressure;
		}
	}
			
	/**
	 * Gets the max decompression time a person can survive.
	 * 
	 * @return decompression time in millisols.
	 * @throws Exception if decompression time could not be found.
	 */
	public double getDecompressionTime() {
		if (time[5] != 0)
			return time[5];
		else {
			time[5] = getValueAsDouble(DECOMPRESSION_TIME);
			return time[5];
		}
	}

	/**
	 * Gets the minimum temperature a person can tolerate.
	 * 
	 * @return temperature in celsius
	 * @throws Exception if min temperature cannot be found.
	 */
	public double getMinTemperature() {
		if (temperature[0] != 0)
			return temperature[0];
		else {
			temperature[0] = getValueAsDouble(MIN_TEMPERATURE);
			return temperature[0];
		}
	}

	/**
	 * Gets the maximum temperature a person can tolerate.
	 * 
	 * @return temperature in celsius
	 * @throws Exception if max temperature cannot be found.
	 */
	public double getMaxTemperature() {
		if (temperature[1] != 0)
			return temperature[1];
		else {
			temperature[1] = getValueAsDouble(MAX_TEMPERATURE);
			return temperature[1];
		}
	}

	/**
	 * Gets the time a person can survive below minimum temperature.
	 * 
	 * @return freezing time in millisols.
	 * @throws Exception if freezing time could not be found.
	 */
	public double getFreezingTime() {
		if (time[6] != 0)
			return time[6];
		else {
			time[6] = getValueAsDouble(FREEZING_TIME);
			return time[6];
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
		if (chance[0] != 0)
			return chance[0];
		else {
			chance[0] = getValueAsDouble(STRESS_BREAKDOWN_CHANCE);
			return chance[0];
		}
	}

	/**
	 * Gets the base percent chance that a person will collapse under high fatigue.
	 * 
	 * @return percent chance of a collapse per millisol.
	 * @throws Exception if collapse time could not be found.
	 */
	public double getHighFatigueCollapseChance() {
		if (chance[1] != 0)
			return chance[1];
		else {
			chance[1] = getValueAsDouble(HIGH_FATIGUE_COLLAPSE);
			return chance[1];
		}
	}

	/**
	 * Gets the gender ratio between males and the total population on Mars.
	 * 
	 * @return gender ratio between males and total population.
	 * @throws Exception if gender ratio could not be found.
	 */
	public double getGenderRatio() {
		if (ratio[1] != 0)
			return ratio[1];
		else {
			ratio[1] = getValueAsDouble(GENDER_MALE_PERCENTAGE) / 100D;
			return ratio[1];
		}
	}

	/**
	 * Gets the average percentage for a particular MBTI personality type for
	 * settlers.
	 * 
	 * @param personalityType the MBTI personality type
	 * @return percentage
	 * @throws Exception if personality type could not be found.
	 */
	public double getPersonalityTypePercentage(String personalityType) {
		double result = 0D;

		// Element root = personDoc.getRootElement();
		Element personalityTypeList = root.getChild(PERSONALITY_TYPES);
		List<Element> personalityTypes = personalityTypeList.getChildren(MBTI);

		for (Element mbtiElement : personalityTypes) {
			String type = mbtiElement.getAttributeValue(TYPE);
			if (type.equals(personalityType)) {
				result = Double.parseDouble(mbtiElement.getAttributeValue(PERCENTAGE));
				break;
			}
		}

		return result;
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
	// Relocate createPersonalityDistribution() from MBTI to here
	public void createPersonalityDistribution() {

		personalityDistribution = new HashMap<String, Double>(16);

		personalityDistribution.put("ISTP", getPersonalityTypePercentage("ISTP"));
		personalityDistribution.put("ISTJ", getPersonalityTypePercentage("ISTJ"));
		personalityDistribution.put("ISFP", getPersonalityTypePercentage("ISFP"));
		personalityDistribution.put("ISFJ", getPersonalityTypePercentage("ISFJ"));
		personalityDistribution.put("INTP", getPersonalityTypePercentage("INTP"));
		personalityDistribution.put("INTJ", getPersonalityTypePercentage("INTJ"));
		personalityDistribution.put("INFP", getPersonalityTypePercentage("INFP"));
		personalityDistribution.put("INFJ", getPersonalityTypePercentage("INFJ"));
		personalityDistribution.put("ESTP", getPersonalityTypePercentage("ESTP"));
		personalityDistribution.put("ESTJ", getPersonalityTypePercentage("ESTJ"));
		personalityDistribution.put("ESFP", getPersonalityTypePercentage("ESFP"));
		personalityDistribution.put("ESFJ", getPersonalityTypePercentage("ESFJ"));
		personalityDistribution.put("ENTP", getPersonalityTypePercentage("ENTP"));
		personalityDistribution.put("ENTJ", getPersonalityTypePercentage("ENTJ"));
		personalityDistribution.put("ENFP", getPersonalityTypePercentage("ENFP"));
		personalityDistribution.put("ENFJ", getPersonalityTypePercentage("ENFJ"));


		Iterator<String> i = personalityDistribution.keySet().iterator();
		double count = 0D;
		while (i.hasNext())
			count += personalityDistribution.get(i.next());
		if (count != 100D)
			throw new IllegalStateException(
					"PersonalityType.loadPersonalityTypes(): percentages don't add up to 100%. (total: " + count + ")");

	}


	/**
	 * Gets the value of an element as a double
	 * 
	 * @param an element
	 * 
	 * @return a double
	 */
	private double getValueAsDouble(String child) {
		// Element root = personDoc.getRootElement();
		Element element = root.getChild(child);
		String str = element.getAttributeValue(VALUE);
		return Double.parseDouble(str);
	}


	/**
	 * Create country list
	 * 
	 * @return
	 */
	public List<String> createAllCountryList() {

		if (allCountries == null) {
			allCountries = new ArrayList<>();

			allCountries.add("China"); // 0
			allCountries.add("Canada"); // 1
			allCountries.add("India"); // 2
			allCountries.add("Japan"); // 3
			allCountries.add("USA"); // 4
			allCountries.add("Russia"); // 5

			allCountries.addAll(createESACountryList()); // 6

		}

		return allCountries;
	}

	public String getCountry(int id) {
		if (allCountries == null) {
			allCountries = createAllCountryList();
		}
		return allCountries.get(id);
	}

	public int getCountryNum(String c) {
		if (allCountries == null) {
			allCountries = createAllCountryList();
		}
		for (int i=0; i<allCountries.size(); i++) {
			if (allCountries.get(i).equalsIgnoreCase(c))
				return i;
		}
		
		return -1;
	}
	
	/**
	 * Create ESA 22 country list
	 * 
	 * @return
	 */
	public List<String> createESACountryList() {

		if (ESACountries == null) {
			ESACountries = new ArrayList<>();

			ESACountries.add("Austria");
			ESACountries.add("Belgium");
			ESACountries.add("Czech Republic");
			ESACountries.add("Denmark");
			ESACountries.add("Estonia");
			
			ESACountries.add("Finland");
			ESACountries.add("France");
			ESACountries.add("Germany");
			ESACountries.add("Greece");
			ESACountries.add("Hungary");
			
			ESACountries.add("Ireland");
			ESACountries.add("Italy");
			ESACountries.add("Luxembourg");
			ESACountries.add("The Netherlands");
			ESACountries.add("Norway");
			
			ESACountries.add("Poland");
			ESACountries.add("Portugal");
			ESACountries.add("Romania");
			ESACountries.add("Spain");
			ESACountries.add("Sweden");
			
			ESACountries.add("Switzerland");
			ESACountries.add("UK");

		}

		return ESACountries;
	}

	/**
	 * Computes the country id. If none, return -1.
	 * 
	 * @param country
	 * @return
	 */
	public int computeCountryID(String country) {
		if (allCountries.contains(country))
			return allCountries.indexOf(country);
		else 
			return -1;
	}

	/**
	 * Create sponsor list
	 * 
	 * @return
	 */
	public List<String> createLongSponsorList() {

		if (longSponsors == null) {
			longSponsors = new ArrayList<>();

			longSponsors.add(ReportingAuthorityType.CNSA_L.getName());
			longSponsors.add(ReportingAuthorityType.CSA_L.getName());
			longSponsors.add(ReportingAuthorityType.ISRO_L.getName());
			longSponsors.add(ReportingAuthorityType.JAXA_L.getName());
			longSponsors.add(ReportingAuthorityType.NASA_L.getName());
			longSponsors.add(ReportingAuthorityType.RKA_L.getName());
			longSponsors.add(ReportingAuthorityType.ESA_L.getName());
			longSponsors.add(ReportingAuthorityType.MARS_SOCIETY_L.getName());
			longSponsors.add(ReportingAuthorityType.SPACEX_L.getName());
		}

		return longSponsors;
	}
	
	/**
	 * Create sponsor list
	 * 
	 * @return
	 */
	public List<String> createSponsorList() {

		if (sponsors == null) {
			sponsors = new ArrayList<>();

			sponsors.add(ReportingAuthorityType.CNSA.getName());
			sponsors.add(ReportingAuthorityType.CSA.getName());
			sponsors.add(ReportingAuthorityType.ISRO.getName());
			sponsors.add(ReportingAuthorityType.JAXA.getName());
			sponsors.add(ReportingAuthorityType.NASA.getName());
			sponsors.add(ReportingAuthorityType.RKA.getName());
			sponsors.add(ReportingAuthorityType.ESA.getName());
			sponsors.add(ReportingAuthorityType.MS.getName());
			sponsors.add(ReportingAuthorityType.SPACEX.getName());
		}

		return sponsors;
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
	 * Prepare object for garbage collection.
	 */
	public void destroy() {
		height = null;
		weight = null;
		o2ConsumptionRate = null;
		consumptionRates = null;
		ratio = null;
		chance = null;
		time = null;
		temperature = null;
		root = null;
		personalityDistribution = null;
		personNameList = null;
		allCountries = null;
		ESACountries = null;
		sponsors = null;
		longSponsors = null;
		lastNames = null;
		firstNames = null;
		commander = null;
		if (personNameList != null) {
			personNameList.clear();
			personNameList = null;
		}
	}
}
