/*
 * Mars Simulation Project
 * PhysicalCondition.java
 * @date 2023-07-21
 * @author Barry Evans
 */
package com.mars_sim.core.person;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;

import com.mars_sim.core.LifeSupportInterface;
import com.mars_sim.core.UnitEventType;
import com.mars_sim.core.data.SolMetricDataLogger;
import com.mars_sim.core.logging.SimLogger;
import com.mars_sim.core.person.ai.NaturalAttributeManager;
import com.mars_sim.core.person.ai.NaturalAttributeType;
import com.mars_sim.core.person.ai.task.meta.EatDrinkMeta;
import com.mars_sim.core.person.health.Complaint;
import com.mars_sim.core.person.health.ComplaintType;
import com.mars_sim.core.person.health.DeathInfo;
import com.mars_sim.core.person.health.HealthProblem;
import com.mars_sim.core.person.health.HealthRiskType;
import com.mars_sim.core.person.health.MedicalManager;
import com.mars_sim.core.person.health.Medication;
import com.mars_sim.core.person.health.RadiationExposure;
import com.mars_sim.core.person.health.RadioProtectiveAgent;
import com.mars_sim.core.resource.ResourceUtil;
import com.mars_sim.core.time.ClockPulse;
import com.mars_sim.core.time.MasterClock;
import com.mars_sim.tools.Msg;
import com.mars_sim.tools.util.RandomUtil;

/**
 * This class represents the Physical Condition of a Person. It models a
 */
public class PhysicalCondition implements Serializable {

	/** default serial id. */
	private static final long serialVersionUID = 1L;

	/** default logger. */
	private static final SimLogger logger = SimLogger.getLogger(PhysicalCondition.class.getName());

	/** The maximum number of sols for storing stats. */
	public static final int MAX_NUM_SOLS = 7;
	/** The maximum number of sols in fatigue [millisols]. */
	public static final int MAX_FATIGUE = 40_000;
	/** The maximum number of sols in hunger [millisols]. */
	public static final int MAX_HUNGER = 40_000;
	/** Reset to hunger [millisols] immediately upon eating. */
	public static final int HUNGER_CEILING_UPON_EATING = 750;
	/** The maximum number of sols in thirst [millisols]. */
	public static final int MAX_THIRST = 7_000;
	/** The maximum number of sols in thirst [millisols]. */
	public static final int THIRST_CEILING_UPON_DRINKING = 500;
	/** The amount of thirst threshold [millisols]. */
	public static final int THIRST_THRESHOLD = 150;
	/** The amount of thirst threshold [millisols]. */
	public static final int HUNGER_THRESHOLD = 250;
	/** The amount of thirst threshold [millisols]. */
	public static final int ENERGY_THRESHOLD = 2525;
	/** The amount of fatigue threshold [millisols]. */
	private static final int FATIGUE_THRESHOLD = 750;
	/** The amount of fatigue threshold [millisols]. */
	public static final int FATIGUE_MIN = 150;
	/** The amount of stress threshold [millisols]. */
	private static final int STRESS_THRESHOLD = 60;
	/** Life support minimum value. */
	private static final int MIN_VALUE = 0;
	/** Life support maximum value. */
	private static final int MAX_VALUE = 1;

	private static final int OXYGEN_ID = ResourceUtil.oxygenID;

	/** Performance modifier for thirst. */
	private static final double THIRST_PERFORMANCE_MODIFIER = .00015D;
	/** Performance modifier for hunger. */
	private static final double HUNGER_PERFORMANCE_MODIFIER = .0001D;
	/** Performance modifier for fatigue. */
	private static final double FATIGUE_PERFORMANCE_MODIFIER = .0005D;
	/** Performance modifier for stress. */
	private static final double STRESS_PERFORMANCE_MODIFIER = .001D;
	/** Performance modifier for energy. */
	private static final double ENERGY_PERFORMANCE_MODIFIER = .0001D;
	/** The average maximum daily energy intake */
	private static final double MAX_DAILY_ENERGY_INTAKE = 10100D;
	/** The average kJ of a 1kg food. Assume each meal has 0.1550 kg and has 2525 kJ. */
	public static final double FOOD_COMPOSITION_ENERGY_RATIO = 16290.323;
	// public static int MAX_KJ = 16290; // 1kg of food has ~16290 kJ (see notes on
	// people.xml under <food-consumption-rate value="0.62" />)
	public static final double ENERGY_FACTOR = 15D;
	/** The maximum air pressure a person can live without harm in kPa. (somewhat arbitrary). */
	public static final double MAXIMUM_AIR_PRESSURE = 68D; // Assume 68 kPa time dependent
	/** Period of time (millisols) over which random ailments may happen. */
	private static final double RANDOM_AILMENT_PROBABILITY_TIME = 100_000D;
	/** The standard pre-breathing time in the EVA suit. */
	private static final double STANDARD_PREBREATHING_TIME = 40;

	public static final String STANDARD_QUOTE_0 = "Thousands have lived without love, not one without water. – W.H.Auden.";
	public static final String STANDARD_QUOTE_1 = "Remember that no child should go empty stomach in the 21st century.";
	
	/** The default string for degree celsius */
	public static final String DEGREE_CELSIUS = Msg.getString("temperature.sign.degreeCelsius");
	
	/** The default string for energy level 1 */
	public static final String ENERGY_LEVEL_1 = Msg.getString("PersonTableModel.column.energy.level1");
	/** The default string for energy level 2 */
	public static final String ENERGY_LEVEL_2 = Msg.getString("PersonTableModel.column.energy.level2");
	/** The default string for energy level 3 */
	public static final String ENERGY_LEVEL_3 = Msg.getString("PersonTableModel.column.energy.level3");
	/** The default string for energy level 4 */
	public static final String ENERGY_LEVEL_4 = Msg.getString("PersonTableModel.column.energy.level4");
	/** The default string for energy level 5 */
	public static final String ENERGY_LEVEL_5 = Msg.getString("PersonTableModel.column.energy.level5");
	/** The default string for energy level 6 */
	public static final String ENERGY_LEVEL_6 = Msg.getString("PersonTableModel.column.energy.level6");
	/** The default string for energy level 7 */
	public static final String ENERGY_LEVEL_7 = Msg.getString("PersonTableModel.column.energy.level7");

	/** The default string for water level 1 */
	public static final String WATER_LEVEL_1 = Msg.getString("PersonTableModel.column.water.level1");
	/** The default string for water level 2 */
	public static final String WATER_LEVEL_2 = Msg.getString("PersonTableModel.column.water.level2");
	/** The default string for water level 3 */
	public static final String WATER_LEVEL_3 = Msg.getString("PersonTableModel.column.water.level3");
	/** The default string for water level 4 */
	public static final String WATER_LEVEL_4 = Msg.getString("PersonTableModel.column.water.level4");
	/** The default string for water level 5 */
	public static final String WATER_LEVEL_5 = Msg.getString("PersonTableModel.column.water.level5");

	/** The default string for fatigue level 1 */
	public static final String FATIGUE_LEVEL_1 = Msg.getString("PersonTableModel.column.fatigue.level1");
	/** The default string for fatigue level 2 */
	public static final String FATIGUE_LEVEL_2 = Msg.getString("PersonTableModel.column.fatigue.level2");
	/** The default string for fatigue level 3 */
	public static final String FATIGUE_LEVEL_3 = Msg.getString("PersonTableModel.column.fatigue.level3");
	/** The default string for fatigue level 4 */
	public static final String FATIGUE_LEVEL_4 = Msg.getString("PersonTableModel.column.fatigue.level4");
	/** The default string for fatigue level 5 */
	public static final String FATIGUE_LEVEL_5 = Msg.getString("PersonTableModel.column.fatigue.level5");
	
	/** The default string for stress level 1 */
	public static final String STRESS_LEVEL_1 = Msg.getString("PersonTableModel.column.stress.level1");
	/** The default string for stress level 2 */
	public static final String STRESS_LEVEL_2 = Msg.getString("PersonTableModel.column.stress.level2");
	/** The default string for stress level 3 */
	public static final String STRESS_LEVEL_3 = Msg.getString("PersonTableModel.column.stress.level3");
	/** The default string for stress level 4 */
	public static final String STRESS_LEVEL_4 = Msg.getString("PersonTableModel.column.stress.level4");
	/** The default string for stress level 5 */
	public static final String STRESS_LEVEL_5 = Msg.getString("PersonTableModel.column.stress.level5");

	/** The default string for performance level 1 */
	public static final String PERF_LEVEL_1 = Msg.getString("PersonTableModel.column.performance.level1");
	/** The default string for performance level 2 */
	public static final String PERF_LEVEL_2 = Msg.getString("PersonTableModel.column.performance.level2");
	/** The default string for performance level 3 */
	public static final String PERF_LEVEL_3 = Msg.getString("PersonTableModel.column.performance.level3");
	/** The default string for performance level 4 */
	public static final String PERF_LEVEL_4 = Msg.getString("PersonTableModel.column.performance.level4");
	/** The default string for performance level 5 */
	public static final String PERF_LEVEL_5 = Msg.getString("PersonTableModel.column.performance.level5");
	
	private static final String WELL = "Well";
	private static final String DEAD_COLON = "Dead : ";
	private static final String SICK_COLON = "Sick : ";
	public static final String TBD = "[To Be Determined]";
	private static final String TRIGGERED_DEATH = "[Player Triggered Death]";
	
	private static double o2Consumption;
	private static double h20Consumption;
	private static double minAirPressure;
	private static double minTemperature;
	private static double maxTemperature;
	private static double foodConsumption;


	/** True if person is starving. */
	private boolean isStarving;
	/** True if person is stressed out. */
	private boolean isStressedOut;
	/** True if person is dehydrated. */
	private boolean isDehydrated;
	/** True if person is alive. */
	private boolean alive;
	/** True if person is radiation Poisoned. */
	private boolean isRadiationPoisoned;
	/** True if person is doing a task that's considered resting. */
	private boolean restingTask;

	private int endurance;
	private int strength;
	private int agility;

	/**
	 * Person's Musculoskeletal system from 0 to 100 (muscle pain tolerance, muscle
	 * health, muscle soreness).
	 */
	private double musclePainTolerance;
	private double muscleHealth;
	private double muscleSoreness;
	
	/** Person's thirst level. [in millisols]. */
	private double thirst;
	/** Person's fatigue level from 0 to infinity. */
	private double fatigue;
	/** Person's hunger level [in millisols]. */
	private double hunger;
	/** Person's stress level (0.0 % - 100.0 %). */
	private double stress;
	/** Performance factor 0.0 to 1.0. */
	private double performance;
	/** Person's energy level [in kJ] */
	private double kJoules;
	/** Person's food appetite (0.0 to 1.0) */
	private double appetite;

	/** The time it takes to prebreathe the air mixture in the EVA suit. */
	private double remainingPrebreathingTime = STANDARD_PREBREATHING_TIME + RandomUtil.getRandomInt(-5, 5);

	private double starvationStartTime;
	private double dehydrationStartTime;
	
	private double personalMaxEnergy;
	private double bodyMassDeviation;
	
	/**  The amount of water this person would consume each time (assuming drinking water 10 times a day). */
	private double waterConsumedPerServing;
	
	private double waterConsumedPerSol;

	/** Person owning this physical. */
	private Person person;
	/** Details of persons death. */
	private DeathInfo deathDetails;
	/** Radiation Exposure. */
	private RadiationExposure radiation;

	/** List of medications affecting the person. */
	private List<Medication> medicationList;
	/** Injury/Illness effecting person. */
	private List<HealthProblem> problems;
	/** Record of Illness frequency. */
	private Map<ComplaintType, Integer> healthLog;

	/** Health Risk probability. */
	private Map<HealthRiskType, Double> healthRisks;
	
	
	/** 
	 * The amount a person consumes on each sol.
	 * 0: food (kg), 1: meal (kg), 2: dessert (kg), 3: water (kg), 4: oxygen (kg)
	 */
	private SolMetricDataLogger<Integer> consumption;
	
	/** The CircadianClock instance. */
	private transient CircadianClock circadian;
	/** The NaturalAttributeManager instance. */
	private transient NaturalAttributeManager naturalAttributeManager;

	/** The HealthProblem instance. */
	private HealthProblem starved;
	/** The HealthProblem instance. */
	private HealthProblem dehydrated;
	/** Most mostSeriousProblem problem. */
	private HealthProblem mostSeriousProblem;

	private static MasterClock master;

	private static EatDrinkMeta eatMealMeta = new EatDrinkMeta();
	private static MedicalManager medicalManager;

	private static PersonConfig personConfig;

	/**
	 * Constructor 1.
	 *
	 * @param newPerson The person requiring a physical presence.
	 */
	public PhysicalCondition(Person newPerson) {
		person = newPerson;

		circadian = person.getCircadianClock();
		naturalAttributeManager = person.getNaturalAttributeManager();

		alive = true;

		radiation = new RadiationExposure(newPerson);

		deathDetails = null;

		problems = new CopyOnWriteArrayList<>();
		healthLog = new ConcurrentHashMap<>();
		healthRisks = new EnumMap<>(HealthRiskType.class);
		medicationList = new CopyOnWriteArrayList<>();

		endurance = naturalAttributeManager.getAttribute(NaturalAttributeType.ENDURANCE);
		strength = naturalAttributeManager.getAttribute(NaturalAttributeType.STRENGTH);
		agility = naturalAttributeManager.getAttribute(NaturalAttributeType.AGILITY);

		// Computes the adjustment from a person's natural attributes
		double es = (endurance + strength + agility) / 300D;

		// Note: may incorporate real world parameters such as areal density in g cm−2,
		// T-score and Z-score (see https://en.wikipedia.org/wiki/Bone_density)
		musclePainTolerance = RandomUtil.getRandomInt(-10, 10) + es; // pain tolerance
		muscleHealth = 50D; // muscle health index; 50 being the average
		muscleSoreness = RandomUtil.getRandomRegressionInteger(100); // muscle soreness

		personalMaxEnergy = MAX_DAILY_ENERGY_INTAKE;
		appetite = personalMaxEnergy / MAX_DAILY_ENERGY_INTAKE;

		bodyMassDeviation = Math.sqrt(person.getBaseMass() / Person.getAverageWeight()
				* person.getHeight() / Person.getAverageHeight());
		// Note: p = mean + RandomUtil.getGaussianDouble() * standardDeviation
		double mod2 = RandomUtil.computeGaussianWithLimit(1, .5, .2);
		// bodyMassDeviation average around 0.7 to 1.3
		bodyMassDeviation = bodyMassDeviation * mod2;

		// Assume a person drinks 10 times a day, each time ~375 mL
		waterConsumedPerSol = h20Consumption * bodyMassDeviation ;
		// waterConsumedPerServing is ~ 0.19 kg
		waterConsumedPerServing = waterConsumedPerSol / 10; 

		double sTime = personConfig.getStarvationStartTime();
		starvationStartTime = 1000D * RandomUtil.computeGaussianWithLimit(sTime, 0.3, bodyMassDeviation / 5);
		
		double dTime = personConfig.getDehydrationStartTime();
		dehydrationStartTime = 1000D * RandomUtil.computeGaussianWithLimit(dTime, 0.3, bodyMassDeviation / 5);

		isStarving = false;
		isStressedOut = false;
		isDehydrated = false;
		// Initially set performance to 1.0 (=100%) to avoid issues at startup
		performance = 1.0D;

		// Initialize the food consumption logger
		consumption = new SolMetricDataLogger<>(MAX_NUM_SOLS);
		consumption.increaseDataPoint(0, 0.0);
		consumption.increaseDataPoint(1, 0.0);
		consumption.increaseDataPoint(2, 0.0);
		consumption.increaseDataPoint(3, 0.0);
		consumption.increaseDataPoint(4, 0.0);
		
		initialize();
	}

	private void initializeHealthIndices() {
		// Set up random physical health index
		thirst = RandomUtil.getRandomRegressionInteger(50);
		fatigue = RandomUtil.getRandomRegressionInteger(50);
		stress = RandomUtil.getRandomRegressionInteger(20);
		hunger = RandomUtil.getRandomRegressionInteger(50);
		// kJoules somewhat co-relates with hunger
		kJoules = 10000 + (50 - hunger) * 100;
		performance = 1.0D - (50 - fatigue) * .002 
				- (20 - stress) * .002 
				- (50 - hunger) * .002
				- (50 - thirst) * .002;
	}

	
	/**
	 * Initializes the health risk probability map.
	 * 
	 * @return
	 */
	public void initializeHealthRisks() {
		
		for (HealthRiskType type: HealthRiskType.values()) {
			double probability = RandomUtil.getRandomDouble(5);
			healthRisks.put(type, probability);
		}
	}
	
	
	/**
	 * Initialize values and instances at the beginning of sol 1
	 * (Note : Must skip this when running maven test or else having exceptions)
	 */
	private void initialize() {
		// Set up the initial values for each physical health index
		initializeHealthIndices();
		
		// Modify personalMaxEnergy at the start of the sim
		int d1 = 2 * (35 - person.getAge());
		// Assume that after age 35, metabolism slows down
		double d2 = person.getBaseMass() - Person.getAverageWeight();
		double preference = person.getPreference().getPreferenceScore(eatMealMeta) * 10D;

		// Update the personal max energy and appetite based on one's age and weight
		personalMaxEnergy = personalMaxEnergy + 50 * (d1 + d2 + preference);
		appetite = personalMaxEnergy / MAX_DAILY_ENERGY_INTAKE;
		
		// Set up the initial values for health risks
		initializeHealthRisks();
	}

	/**
	 * The Physical condition should be updated to reflect a passing of time. This
	 * method has to check the recover or degradation of any current illness. The
	 * progression of this time period may result in the illness turning fatal. It
	 * also updated the hunger and fatigue status
	 *
	 * @param time    amount of time passing (in millisols)
	 * @param support life support system.
	 * @return True still alive.
	 */
	public void timePassing(ClockPulse pulse, LifeSupportInterface support) {
		if (alive) {
			
			double time = pulse.getElapsed();

			// Check once a day only
			if (pulse.isNewSol()) {
				// reduce the muscle soreness
				recoverFromSoreness(1);
				// Update the entropy in muscles
				entropy(time * -20);
			}
			
			// Check once per msol (millisol integer)
			if (pulse.isNewMSol()) {

				// Calculate performance and most mostSeriousProblem illness.
				recalculatePerformance();
				// Update radiation counter
				radiation.timePassing(pulse);
				
				// Get stress factor due to settlement overcrowding
				if (person.isInSettlement()) {
					// Note: this stress factor is different from LifeSupport's timePassing's
					//       stress modifier for each particular building
					double stressFactor = person.getSettlement().getStressFactor(time);
					// Update stress
					addStress(stressFactor);
				}
				
				int msol = pulse.getMarsTime().getMillisolInt();
				if (msol % 7 == 0) {

					// Update starvation
					checkStarvation(hunger);
					// Update dehydration
					checkDehydration(thirst);
					
					// Check for mental breakdown if person is at high stress
					
					// Check if person is at very high fatigue may collapse.

					if (!isRadiationPoisoned)
						checkRadiationPoisoning(time);
				}
			}

            double currentO2Consumption;
			if (person.isRestingTask())
				currentO2Consumption = personConfig.getLowO2ConsumptionRate();
			else
				currentO2Consumption = personConfig.getNominalO2ConsumptionRate();

			// Check life support system
			checkLifeSupport(time, currentO2Consumption, support);
			// Update the existing health problems
			checkHealth(pulse);
			// Update thirst
			increaseThirst(time * bodyMassDeviation * .75);
			// Update fatigue
			increaseFatigue(time);
			// Update hunger
			increaseHunger(time * bodyMassDeviation * .75);
			// Update energy via PersonTaskManager's executeTask()
			// since it can discern if it's a resting task or a labor-intensive (effort-driven) task
		}
	}


	 /**
	  * Checks and updates existing health problems
	  *
	  * @param time
	  */
	private void checkHealth(ClockPulse pulse) {
		boolean illnessEvent = false;

		if (!problems.isEmpty()) {
			// Throw illness event if any problems already exist.
			illnessEvent = true;
			// A list of complaints (Type of illnesses)
			List<Complaint> newComplaints = new CopyOnWriteArrayList<>();

			Iterator<HealthProblem> hp = problems.iterator();
			while (hp.hasNext()) {
				HealthProblem problem = hp.next();
				// Advance each problem, they may change into a worse problem.
				// If the current is completed or a new problem exists then
				// remove this one.
				Complaint nextComplaintPhase = problem.timePassing(pulse.getElapsed(), this);

				// After sleeping sufficiently, the high fatigue collapse should no longer exist.

				if (problem.isCured() || (nextComplaintPhase != null)) {

					ComplaintType type = problem.getType();

					logger.log(person, Level.INFO, 20_000,
							"Cured from " + type + ".");

					if (type == ComplaintType.DEHYDRATION)
						isDehydrated = false;

					else if (type == ComplaintType.STARVATION)
						isStarving = false;

					else if (type == ComplaintType.RADIATION_SICKNESS)
						isRadiationPoisoned = false;

					// If nextPhase is not null, remove this problem so that it can
					// properly be transitioned into the next.
					problems.remove(problem);

				}

				// If a new problem, check it doesn't exist already
				if (nextComplaintPhase != null) {
					newComplaints.add(nextComplaintPhase);
				}
			}

			// Add the new problems
			for (Complaint c : newComplaints) {
				addMedicalComplaint(c);
				illnessEvent = true;
			}
		}

		// Generates any random illnesses.
		if (!restingTask) {
			List<Complaint> randomAilments = checkForRandomAilments(pulse);
			if (randomAilments.size() > 0) {
				illnessEvent = true;
			}
		}

		if (illnessEvent) {
			person.fireUnitUpdate(UnitEventType.ILLNESS_EVENT);
		}

		// Add time to all medications affecting the person.
		Iterator<Medication> i = medicationList.iterator();
		while (i.hasNext()) {
			Medication med = i.next();
			med.timePassing(pulse);
			if (!med.isMedicated()) {
				i.remove();
			}
		}
	}

	/**
	 * Checks on the life support.
	 *
	 * @param time
	 * @param currentO2Consumption
	 * @param support
	 */
	private void checkLifeSupport(double time, double currentO2Consumption, LifeSupportInterface support) {
		if (time > 0) {
			try {
				if (lackOxygen(support, currentO2Consumption * (time / 1000D)))
					logger.log(person, Level.SEVERE, 60_000, "Reported lack of oxygen.");
				if (badAirPressure(support, minAirPressure))
					logger.log(person, Level.SEVERE, 60_000, "Reported non-optimal air pressure.");
				if (badTemperature(support, minTemperature, maxTemperature))
					logger.log(person, Level.SEVERE, 60_000, "Reported non-optimal temperature.");

			} catch (Exception e) {
				logger.log(person, Level.SEVERE, 60_000, "Reported anomaly in the life support system: ", e);
			}
		}
	}

	/**
	 * Gets the person's fatigue level.
	 *
	 * @return the value from 0 to infinity.
	 */
	public double getFatigue() {
		return fatigue;
	}

	public double getThirst() {
		return thirst;
	}

	/**
	 * Gets the person's caloric energy.
	 *
	 * @return person's caloric energy in kilojoules Note: one large calorie is
	 *         about 4.2 kilojoules
	 */
	public double getEnergy() {
		return kJoules;
	}

	/**
	 * Reduces the person's energy.
	 *
	 * @param time the amount of time (millisols).
	 */
	public void reduceEnergy(double time) {
		double xdelta = time * MAX_DAILY_ENERGY_INTAKE / 1000D;

		// Changing this to a more linear reduction of energy.
		// We may want to change it back to exponential. - Scott

		if (kJoules < 250) {
			// 250 kJ is the lowest possible energy level
			kJoules = 250;
		}
		else if (kJoules < 500) {
			kJoules -= xdelta * .2;
		} else if (kJoules < 1000) {
			kJoules -= xdelta * .25;
		} else if (kJoules < 3000) {
			kJoules -= xdelta * .3;
		} else if (kJoules < 5000) {
			kJoules -= xdelta * .35;
		} else if (kJoules < 7000) {
			kJoules -= xdelta * .4;
		} else if (kJoules < 9000) {
			kJoules -= xdelta * .45;
		} else if (kJoules < 11000) {
			kJoules -= xdelta * .5;
		} else if (kJoules < 13000) {
			kJoules -= xdelta * .55;
		} else if (kJoules < 15000) {
			kJoules -= xdelta * .6;			
		} else if (kJoules < 17000) {
			kJoules -= xdelta * .65;	
		} else
			kJoules -= xdelta * .7;

		person.fireUnitUpdate(UnitEventType.HUNGER_EVENT);
	}

	/**
	 * Adds to the person's energy intake by eating.
	 *
	 * @param person's energy level in kilojoules
	 */
	public void addEnergy(double foodAmount) {
		// 1 calorie = 4.1858 kJ
		// Should vary MAX_KJ according to the individual's physical profile strength,
		// endurance, etc..
		// FOOD_COMPOSITION_ENERGY_RATIO = 16290
		
		// Note: 1kg of food has ~16290 kJ 
		// See notes on people.xml under <food-consumption-rate value="0.62" />
		
		// Each meal (.155 kg = .62/4) has an average of 2525 kJ

		// Note: changing this to a more linear addition of energy.
		// We may want to change it back to exponential. - Scott

		double xdelta = foodAmount * FOOD_COMPOSITION_ENERGY_RATIO / appetite / ENERGY_FACTOR;

		if (hunger <= 0)
			kJoules = personalMaxEnergy;
		else if (kJoules > 19_000D) {
			kJoules += xdelta * .035;
		} else if (kJoules > 17_000D) {
			kJoules += xdelta * .06;
		} else if (kJoules > 15_000D) {
			kJoules += xdelta * .15;
		} else if (kJoules > 13_000D) {
			kJoules += xdelta * .2;
		} else if (kJoules > 11_000D) {
			kJoules += xdelta * .25;
		} else if (kJoules > 9_000D) {
			kJoules += xdelta * .3;
		} else if (kJoules > 7_000D) {
			kJoules += xdelta * .45;
		} else if (kJoules > 5_000D) {
			kJoules += xdelta * .55;
		} else if (kJoules > 4_000D) {
			kJoules += xdelta * .65;
		} else if (kJoules > 3_000D) {
			kJoules += xdelta * .75;			
		} else if (kJoules > ENERGY_THRESHOLD) {
			kJoules += xdelta * .85;	
		} else if (kJoules > ENERGY_THRESHOLD / 2) {
			kJoules += xdelta * .95;	
		} else if (kJoules > ENERGY_THRESHOLD / 4) {
			kJoules += xdelta * 1.1;	
		} else if (kJoules > ENERGY_THRESHOLD / 8) {
			kJoules += xdelta * 1.3;	
		} else
			kJoules = ENERGY_THRESHOLD / 8.0;

		circadian.eatFood(xdelta / 1000D);

		if (kJoules > personalMaxEnergy * 1.25) {
			kJoules = personalMaxEnergy * 1.25;
		}
		
		person.fireUnitUpdate(UnitEventType.HUNGER_EVENT);
	}

	/**
	 * Gets the performance factor that effect Person with the complaint.
	 *
	 * @return The value is between 0 -> 1.
	 */
	public double getPerformanceFactor() {
		return performance;
	}

	/**
	 * Sets the performance factor.
	 *
	 * @param newPerformance new performance (between 0 and 1).
	 */
	public void setPerformanceFactor(double p) {
		double pp = p;
		if (pp > 1D)
			pp = 1D;
		else if (pp < 0)
			pp = 0;
		if (performance != pp) {
			performance = pp;
			person.fireUnitUpdate(UnitEventType.PERFORMANCE_EVENT);
		}
	}

	/**
	 * Sets the fatigue value for this person.
	 *
	 * @param newFatigue New fatigue.
	 */
	public void setFatigue(double f) {
		double ff = f;
		if (ff > MAX_FATIGUE)
			ff = MAX_FATIGUE;
		else if (ff < -100)
			ff = -100;

		fatigue = ff;
		person.fireUnitUpdate(UnitEventType.FATIGUE_EVENT);
	}
	
	/**
	 * Increases the fatigue for this person.
	 *
	 * @param delta
	 */
	public void increaseFatigue(double delta) {
		double f = fatigue + delta;
		if (f > MAX_FATIGUE)
			f = MAX_FATIGUE;

		fatigue = f;	
		person.fireUnitUpdate(UnitEventType.FATIGUE_EVENT);
	}
	
	/**
	 * Reduces the fatigue for this person.
	 *
	 * @param delta
	 */
	public void reduceFatigue(double delta) {
		double f = fatigue - delta;
		if (f < -100) 
			f = -100;
		
		fatigue = f;
		person.fireUnitUpdate(UnitEventType.FATIGUE_EVENT);
	}
	
	/**
	 * Sets the thirst value for this person.
	 * 
	 * @param t
	 */
	public void setThirst(double t) {
		double tt = t;
		if (tt > MAX_THIRST)
			tt = MAX_THIRST;
		else if (tt < -50)
			tt = -50;

		thirst = tt;
		person.fireUnitUpdate(UnitEventType.THIRST_EVENT);
	}

	/**
	 * Reduces the thirst setting for this person.
	 *
	 * @param thirstRelieved
	 */
	public void reduceThirst(double delta) {
		double t = thirst - delta;
		if (t < -50)
			t = -50;
		else if (t > THIRST_CEILING_UPON_DRINKING)
			t = THIRST_CEILING_UPON_DRINKING;
		
		thirst = t;
		person.fireUnitUpdate(UnitEventType.THIRST_EVENT);
	}
	
	/**
	 * Increases the hunger setting for this person.
	 *
	 * @param delta
	 */
	public void increaseThirst(double delta) {
		double t = thirst + delta;
		if (t > MAX_THIRST)
			t = MAX_THIRST;
		
		thirst = t;
		person.fireUnitUpdate(UnitEventType.THIRST_EVENT);
	}

	/**
	 * Defines the hunger setting for this person.
	 *
	 * @param newHunger New hunger.
	 */
	public void setHunger(double newHunger) {
		double h = newHunger;
		if (h > MAX_HUNGER)
			h = MAX_HUNGER;
		else if (h < -100)
			h = -100;

		hunger = h;
		person.fireUnitUpdate(UnitEventType.HUNGER_EVENT);
	}

	/**
	 * Reduces the hunger setting for this person.
	 *
	 * @param hungerRelieved
	 */
	public void reduceHunger(double hungerRelieved) {
		double h = hunger - hungerRelieved;
		if (h < -100)
			h = -100;
		else if (h > HUNGER_CEILING_UPON_EATING)
			h = HUNGER_CEILING_UPON_EATING;
		
		hunger = h;
		person.fireUnitUpdate(UnitEventType.HUNGER_EVENT);
	}
	
	/**
	 * Increases the hunger setting for this person.
	 *
	 * @param hungerAdded
	 */
	public void increaseHunger(double hungerAdded) {
		double h = hunger + hungerAdded;
		if (h > MAX_HUNGER)
			h = MAX_HUNGER;

		hunger = h;
		person.fireUnitUpdate(UnitEventType.HUNGER_EVENT);
	}
	
	/**
	 * Gets the person's hunger level.
	 *
	 * @return person's hunger
	 */
	public double getHunger() {
		return hunger;
	}

	/**
	 * Sets the person's stress level.
	 *
	 * @param newStress the new stress level (0.0 to 100.0)
	 */
	public void setStress(double s) {
		double ss = s;
		if (ss > 100)
			ss = 100;
		else if (ss < 0
				|| Double.isNaN(stress))
			ss = 0D;
		
		stress = ss;
		person.fireUnitUpdate(UnitEventType.STRESS_EVENT);
	}
	
	/**
	 * Adds to a person's stress level.
	 *
	 * @param d
	 */
	public void addStress(double d) {
		double ss = stress + d;
		if (ss > 100)
			ss = 100;
		else if (ss < 0
			|| Double.isNaN(ss))
			ss = 0;
		
		stress = ss;
		person.fireUnitUpdate(UnitEventType.STRESS_EVENT);
	}

	/**
	 * Reduces to a person's stress level.
	 *
	 * @param d
	 */
	public void reduceStress(double d) {
		double ss = stress - d;
		if (ss > 100)
			ss = 100;
		else if (ss < 0
			|| Double.isNaN(ss))
			ss = 0;
		
		stress = ss;
		person.fireUnitUpdate(UnitEventType.STRESS_EVENT);
	}
	
	/**
	 * Gets the person's stress level.
	 *
	 * @return stress (0.0 to 100.0)
	 */
	public double getStress() {
		return stress;
	}
	
	/**
	 * Checks if a person is starving or no longer starving.
	 *
	 * @param hunger
	 */
	private void checkStarvation(double hunger) {

		starved = getProblemByType(ComplaintType.STARVATION);
		
		if (!isStarving && hunger > starvationStartTime) {

			// if problems doesn't have starvation, execute the following
			if (starved == null || !problems.contains(starved)) {
				addMedicalComplaint(medicalManager.getStarvation());
				isStarving = true;
				person.fireUnitUpdate(UnitEventType.ILLNESS_EVENT);
				logger.log(person, Level.INFO, 20_000, "Starting starving.");
			}

			// Note : how to tell a person to walk back to the settlement ?
			// Note : should check if a person is on a critical mission,
		}

		else if (starved != null && isStarving) {

			if (hunger < HUNGER_THRESHOLD || kJoules > ENERGY_THRESHOLD) {

				starved.setCured();
				// Set isStarving to false
				isStarving = false;

				logger.log(person, Level.INFO, 20_000, "Cured of starving (case 2).");
			}

			// If this person's hunger has reached the buffer zone
			else if (hunger < HUNGER_THRESHOLD * 2 || kJoules > ENERGY_THRESHOLD * 2) {

				starved.startRecovery();
				String status = starved.getStateString();
				// Set to not starving
				isStarving = false;

				logger.log(person, Level.INFO, 20_000, "Recovering from hunger. "
						 + "  Hunger: " + (int)hunger
						 + ";  kJ: " + Math.round(kJoules*10.0)/10.0
						 + ";  isStarving: " + isStarving
						 + ";  Status: " + status);
			}
			
			else if (hunger >= MAX_HUNGER) {
				starved.setState(HealthProblem.DEAD);
				recordDead(starved, false, STANDARD_QUOTE_1);
			}
		}
	}


	/**
	 * Checks if a person is dehydrated.
	 *
	 * @param hunger
	 */
	private void checkDehydration(double thirst) {

		dehydrated = getProblemByType(ComplaintType.DEHYDRATION);
		
		// If the person's thirst is greater than dehydrationStartTime
		if (!isDehydrated && thirst > dehydrationStartTime) {

			if (dehydrated == null || !problems.contains(dehydrated)) {
				addMedicalComplaint(medicalManager.getDehydration());
				isDehydrated = true;
				person.fireUnitUpdate(UnitEventType.ILLNESS_EVENT);
			}
		}

		else if (dehydrated != null && isDehydrated) {

			if (thirst < THIRST_THRESHOLD / 2) {
				dehydrated.setCured();
				// Set dehydrated to false
				isDehydrated = false;

				logger.log(person, Level.INFO, 0, "Cured of dehydrated (case 2).");
			}

			// If this person's thirst has reached the buffer zone
			else if (thirst < THIRST_THRESHOLD * 2) {

				dehydrated.startRecovery();
				String status  = dehydrated.getStateString();
				// Set dehydrated to false
				isDehydrated = false;

				logger.log(person, Level.INFO, 20_000, "Recovering from dehydration. "
						 + "  Thirst: " + (int)thirst
						 + ";  isDehydrated: " + isDehydrated
						 + ";  Status: " + status);
			}
			else if (thirst >= MAX_THIRST) {
				dehydrated.setState(HealthProblem.DEAD);
				recordDead(dehydrated, false, STANDARD_QUOTE_0);
			}
		}
	}

	/**
	 * Gets the health problem by a certain complaint type.
	 *
	 * @return Health problem or null if the Person does not have it
	 */
	private HealthProblem getProblemByType(ComplaintType type) {
		for (HealthProblem p: problems) {
			if (p.getType() == type) {
				return p;
			}
		}
		return null;
	}


	/**
	 * Checks if person has very high fatigue.
	 *
	 * @param time the time passing (millisols)
	 */
	private void checkRadiationPoisoning(double time) {
		
		var radiationPoisoned = getProblemByType(ComplaintType.RADIATION_SICKNESS);

		if (!isRadiationPoisoned && radiation.isSick()) {

			if (radiationPoisoned == null || !problems.contains(radiationPoisoned)) {
				addMedicalComplaint(medicalManager.getComplaintByName(ComplaintType.RADIATION_SICKNESS));
				isRadiationPoisoned = true;
				person.fireUnitUpdate(UnitEventType.ILLNESS_EVENT);
				logger.log(person, Level.INFO, 3000, "Collapsed because of radiation poisoning.");
			}

			else if (radiationPoisoned != null) {

				radiationPoisoned.setCured();
				// Set isStarving to false
				isRadiationPoisoned = false;

				logger.log(person, Level.INFO, 20_000, "Cured of radiation poisoning (case 1).");
			}
		}

		else if (isRadiationPoisoned) {

			if (!radiation.isSick()) {
				if (radiationPoisoned != null) {
					radiationPoisoned.setCured();
					// Set isRadiationPoisoned to false
					isRadiationPoisoned = false;

					logger.log(person, Level.INFO, 20_000, "Cured of radiation poisoning (case 2).");
				}
			}

			// If this person is taking anti-rad meds
			else if (hasMedication(RadioProtectiveAgent.NAME)) {

				if (radiationPoisoned == null)
					radiationPoisoned = getProblemByType(ComplaintType.RADIATION_SICKNESS);

				String status = "Unknown";
				if (radiationPoisoned != null) {
					radiationPoisoned.startRecovery();
					status  = radiationPoisoned.getStateString();
					// Set to not starving
					isRadiationPoisoned = false;
				}

				logger.log(person, Level.INFO, 20_000, "Taking anti-rad meds and recovering from radiation poisoning. "
						 + ";  isRadiationPoisoned: " + radiationPoisoned
						 + ";  Status: " + status);
			}
		}

		else if (radiationPoisoned != null) {

			radiationPoisoned.setCured();
			// Set isRadiationPoisoned to false
			isRadiationPoisoned = false;

			logger.log(person, Level.INFO, 20_000, "Cured of radiationPoisoning (case 3).");
		}
	}


	/**
	 * Checks for any random ailments that a person comes down with over a period of
	 * time.
	 *
	 * @param time the time period (millisols).
	 * @return list of ailments occurring. May be empty.
	 */
	private List<Complaint> checkForRandomAilments(ClockPulse pulse) {
		double time  = pulse.getElapsed();
		List<Complaint> result = new ArrayList<>();
		List<Complaint> list = medicalManager.getAllMedicalComplaints();
		for (Complaint complaint : list) {
			// Check each possible medical complaint.
			ComplaintType ct = complaint.getType();

			boolean noGo = false;

			if (hasComplaint(complaint)) {

				if (ct == ComplaintType.LACERATION || ct == ComplaintType.BROKEN_BONE
						|| ct == ComplaintType.PULLED_MUSCLE_TENDON || ct == ComplaintType.RUPTURED_APPENDIX) {
					if (person.getTaskDescription().toLowerCase().contains("assist")
							|| person.getTaskDescription().toLowerCase().contains("compil")
							|| person.getTaskDescription().toLowerCase().contains("peer")
							|| person.getTaskDescription().toLowerCase().contains("teach")
						|| restingTask
					) {
						// If a person is performing a resting task, then it is impossible to suffer
						// from laceration.
						noGo = true;
					}
				}

				// Check that person does not already have a health problem with this complaint.

				// Note : the following complaints are being initiated in their own methods
				else if (ct == ComplaintType.HIGH_FATIGUE_COLLAPSE || ct == ComplaintType.PANIC_ATTACK
						|| ct == ComplaintType.DEPRESSION
						// Exclude the following 6 environmentally induced complaints
						|| ct == ComplaintType.DEHYDRATION || ct == ComplaintType.STARVATION
						|| ct == ComplaintType.SUFFOCATION || ct == ComplaintType.FREEZING
						|| ct == ComplaintType.HEAT_STROKE || ct == ComplaintType.DECOMPRESSION
						//
						|| ct == ComplaintType.RADIATION_SICKNESS
						// not meaningful to implement suicide until emotional/mood state is in place
						|| ct == ComplaintType.SUICIDE) {
					noGo = true;
				}

				if (!noGo) {
					double probability = complaint.getProbability();
					// Check that medical complaint has a probability > zero
					// since some complaints are secondary complaints and cannot be started
					// by itself
					if (probability > 0D) {
						double taskModifier = 1;
						double tendency = 1;

						int msol = pulse.getMarsTime().getMissionSol();

						if (healthLog.get(ct) != null && msol > 3)
							tendency = 0.5 + 1.0 * healthLog.get(ct) / msol;
						else
							tendency = 1.0;
						double immunity = 1.0 * endurance + strength;

						if (immunity > 100)
							tendency = .75 * tendency - .25 * immunity / 100.0;
						else
							tendency = .75 * tendency + .25 * (100 - immunity) / 100.0;

						if (tendency < 0)
							tendency = 0.0001;

						if (tendency > 2)
							tendency = 2;

						if (ct == ComplaintType.PULLED_MUSCLE_TENDON
								|| ct == ComplaintType.BROKEN_BONE) {
							// Note: at the time of workout, pulled muscle can happen
							// Note: how to make a person less prone to pulled muscle while doing other tasks
							// if having consistent workout.
							String taskDes = person.getTaskDescription().toLowerCase();
							String taskPhase = person.getTaskPhase().toLowerCase();
							if (taskPhase.contains("exercising") || taskDes.contains("yoga"))
								taskModifier = 1.1;

							else if (taskPhase.contains("loading") || taskPhase.contains("unloading")) {
								// Doing outdoor field work increases the risk of having pulled muscle.
								taskModifier = 1.2;

								if (agility > 50)
									taskModifier = .75 * taskModifier - .25 * agility / 100.0;
								else
									taskModifier = .75 * taskModifier + .25 * (50 - agility) / 50.0;
							}
							else if (person.getTaskDescription().contains("EVA"))
								// match the uppercase EVA
								taskModifier = 1.3;

							else if (taskDes.contains("digging") || taskDes.contains("mining")
									|| taskDes.contains("excavating")) {
								taskModifier = 1.4;

								int avoidAccident = strength + agility;
								if (avoidAccident > 50)
									taskModifier = .75 * taskModifier - .25 * avoidAccident / 100.0;
								else
									taskModifier = .75 * taskModifier + .25 * (100 - avoidAccident) / 100.0;
							}

						} else if (ct == ComplaintType.MINOR_BURNS// || ct == ComplaintType.MAJOR_BURNS
								|| ct == ComplaintType.BURNS
								|| ct == ComplaintType.LACERATION) {
							if (agility > 50)
								taskModifier = .75 * taskModifier - .25 * agility / 100.0;
							else
								taskModifier = .75 * taskModifier + .25 * (50 - agility) / 50.0;
						}

						if (taskModifier < 0)
							taskModifier = 0.0001;
						if (taskModifier > 2)
							taskModifier = 2;

						// Randomly determine if person suffers from ailment.
						double rand = RandomUtil.getRandomDouble(100D);
						double timeModifier = time / RANDOM_AILMENT_PROBABILITY_TIME;

						if (rand <= probability * taskModifier * tendency * timeModifier) {
							addMedicalComplaint(complaint);
							result.add(complaint);
						}
					}
				}
			}
		}
		return result;
	}

	/**
	 * Does he have any complaints ?
	 * 
	 * @param c
	 * @return
	 */
	private boolean hasComplaint(Complaint c) {
		for (HealthProblem problem : problems) {
			if (problem.getType() == c.getType()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Adds a new medical complaint to the person.
	 *
	 * @param complaint the new medical complaint
	 */
	public void addMedicalComplaint(Complaint c) {
		if (!hasComplaint(c)) {
			ComplaintType type = c.getType();
			// Create a new health problem
			HealthProblem newProblem = new HealthProblem(type, person);
			problems.add(newProblem);

			// Record this complaint type
			int freq = 0;
			if (healthLog.get(type) != null)
				freq = healthLog.get(type);
			healthLog.put(type, freq + 1);
			logger.log(person, Level.INFO, 1_000L, "Suffered from " + type.getName() + ".");
			recalculatePerformance();
		}
	}


	/**
	 * Does the person consume enough oxygen ?
	 *
	 * @param support Life support system providing oxygen.
	 * @param amount  amount of oxygen to consume (in kg)
	 * @return new problem added.
	 * @throws Exception if error consuming oxygen.
	 */
	private boolean lackOxygen(LifeSupportInterface support, double amount) {
		if (amount > 0) {
			if (support == null) {
				logger.log(person, Level.SEVERE, 1000, "Had no life support.");
				return true;
			}
			else {
				double received = support.provideOxygen(amount);
				// Track the amount consumed
				addGasConsumed(OXYGEN_ID, received);
				// Note: how to model how much oxygen we need properly ?
				// Assume one half as the bare minimum
				double required = amount / 2D;

				return checkResourceConsumption(received, required, MIN_VALUE, ComplaintType.SUFFOCATION);
			}
		}

		return false;
	}

	/**
	 * This method checks the consume values of a resource. If the actual is less
	 * than the required then a HealthProblem is generated. If the required amount
	 * is satisfied, then any problem is recovered.
	 *
	 * @param actual    The amount of resource provided.
	 * @param require   The amount of resource required.
	 * @param complaint Problem associated to this resource.
	 * @return Has a new problem been added.
	 */
	private boolean checkResourceConsumption(double actual, double required, int bounds,
					ComplaintType complaint) {

		boolean newProblem = false;
		if (actual - required > 0.000_1 || required - actual > 0.000_1)
			newProblem = false;
		else if ((bounds == MIN_VALUE) && (actual < required))
			newProblem = true;
		else if ((bounds == MAX_VALUE) && (actual > required))
			newProblem = true;

		if (newProblem) {
			String reading = "";
			String unit = "";
			double decimals = 10.0;
			switch (complaint) {
				case SUFFOCATION: 
					reading = "Oxygen";
					unit = " kg";
					decimals = 10000.0;
					break;
				
				case DECOMPRESSION:
					reading = "Pressure";
					unit = " kPa";
					break;

				case FREEZING:
					reading = "Low Temperature";
					unit = " " + DEGREE_CELSIUS;
					break;

				case HEAT_STROKE:
					reading = "High Temperature";
					unit = " " + DEGREE_CELSIUS;
					break;
					
				default:
			}
			String s = reading + " sensor triggered. "
					+ "  Actual: " + Math.round(actual*decimals)/decimals + unit
					+ "  Required: " + Math.round(required*decimals)/decimals + unit;
			logger.log(person, Level.SEVERE, 60_000, s);

			addMedicalComplaint(medicalManager.getComplaintByName(complaint));
			person.fireUnitUpdate(UnitEventType.ILLNESS_EVENT);
		}

		else {
			// Is the person suffering from the illness, if so recovery
			// as the amount has been provided
			HealthProblem illness = getProblemByType(complaint);
			if (illness != null) {
				illness.startRecovery();
				person.fireUnitUpdate(UnitEventType.ILLNESS_EVENT);
			}
		}
		return newProblem;
	}

	/**
	 * Person requires minimum air pressure.
	 *
	 * @param support  Life support system providing air pressure.
	 * @param pressure minimum air pressure person requires (in Pa)
	 * @return new problem added.
	 */
	private boolean badAirPressure(LifeSupportInterface support, double pressure) {
		return checkResourceConsumption(support.getAirPressure(), pressure, MIN_VALUE, ComplaintType.DECOMPRESSION);
	}

	/**
	 * Person requires minimum temperature.
	 *
	 * @param support     Life support system providing temperature.
	 * @param temperature minimum temperature person requires (in degrees Celsius)
	 * @return new problem added.
	 */
	private boolean badTemperature(LifeSupportInterface support, double minTemperature, double maxTemperature) {
		boolean freeze = checkResourceConsumption(support.getTemperature(), minTemperature, MIN_VALUE, ComplaintType.FREEZING);
		boolean hot = checkResourceConsumption(support.getTemperature(), maxTemperature, MAX_VALUE, ComplaintType.HEAT_STROKE);
		return freeze || hot;
	}

	/**
	 * Gets the details of this Person's death.
	 *
	 * @return Detail of the death, will be null if person is still alive.
	 */
	public DeathInfo getDeathDetails() {
		return deathDetails;
	}

	/**
	 * Renders this Person dead, creates DeathInfo, and process the changes
	 *
	 * @param problem      The health problem that contributes to his death.
	 * @param triggeredByPlayer True if it's caused by users
	 */
	public void recordDead(HealthProblem problem, boolean triggeredByPlayer, String lastWord) {
		alive = false;
		String reason = TBD;
		if (triggeredByPlayer) {
			reason = TRIGGERED_DEATH;
			logger.log(person, Level.WARNING, 0, reason);
		}
		else {
			this.mostSeriousProblem = problem;
		}

		deathDetails = new DeathInfo(person, problem, reason, lastWord, master.getMarsTime());
		// Declare the person dead
		person.setDeclaredDead();

		// Deregister the person's quarters
		person.deregisterBed();
		// Set work shift to OFF
		person.getShiftSlot().getShift().leaveShift();
		// Backup the role type
		deathDetails.setRoleType(person.getRole().getType());
		// Relinquish his role
		person.getRole().relinquishOldRoleType();
		// Re-elect any vacated top leaders or chiefs role
		person.getAssociatedSettlement().getChainOfCommand().reelectLeadership(deathDetails.getRoleType());
		// Set the state of the health problem to DEAD
		problem.setState(HealthProblem.DEAD);
		// Remove the person from the airlock's record
		person.getAssociatedSettlement().removeAirlockRecord(person);
		// Set the mind of the person to inactive
		person.getMind().setInactive();
		// Add the person's death info to the postmortem exam waiting list
		// Note: what if a person died in a settlement outside of home town ?
		medicalManager.addPostmortemExam(person.getAssociatedSettlement(), deathDetails);
	}

	/**
	 * Checks if the person is dead.
	 *
	 * @return true if dead
	 */
	public boolean isDead() {
		return !alive;
	}

	/**
	 * Checks if the person is starving.
	 *
	 * @return true if starving
	 */
	public boolean isStarving() {
		return isStarving;
	}

	/**
	 * Checks if the person is dehydrated.
	 * 
	 * @return
	 */
	public boolean isDehydrated() {
		return isDehydrated;
	}

	/**
	 * Gets a string description of the most mostSeriousProblem health situation.
	 *
	 * @return A string containing the current illness if any.
	 */
	public String getHealthSituation() {
		String situation = WELL;
		if (mostSeriousProblem != null) {
			if (isDead()) {
				situation = DEAD_COLON + mostSeriousProblem.getIllness().getType().toString();
			} else {
				situation = SICK_COLON + mostSeriousProblem.toString();
			}
		}
		return situation;
	}

	/**
	 * Gets the most mostSeriousProblem illness.
	 *
	 * @return most mostSeriousProblem illness
	 */
	public Complaint getMostSerious() {
		return mostSeriousProblem.getIllness();
	}

	/**
	 * Returns a collection of known medical problems.
	 */
	public Collection<HealthProblem> getProblems() {
		return problems;
	}

	/**
	 * Calculates how the most mostSeriousProblem problem and other metrics would affect a
	 * person's performance.
	 */
	private void recalculatePerformance() {

		double maxPerformance = 1.0D;

		mostSeriousProblem = null;

		// Check the existing problems. find most mostSeriousProblem problem and how it
		// affects performance. This is the performance baseline
		for (HealthProblem problem : problems) {
			double factor = problem.getPerformanceFactor();
			if (factor < maxPerformance) {
				maxPerformance = factor;
			}

			if ((mostSeriousProblem == null) || (mostSeriousProblem.getIllness().getSeriousness() < problem.getIllness().getSeriousness())) {
				mostSeriousProblem = problem;
			}
		}

		double tempPerformance = maxPerformance;

		// High thirst reduces performance.
		if (thirst > 800D) {
			tempPerformance -= (thirst - 800D) * THIRST_PERFORMANCE_MODIFIER / 2;
		} else if (thirst > 400D) {
			tempPerformance -= (thirst - 400D) * THIRST_PERFORMANCE_MODIFIER / 4;
		}

		// High hunger reduces performance.
		if (hunger > 1600D) {
			tempPerformance -= (hunger - 1600D) * HUNGER_PERFORMANCE_MODIFIER / 2;
		} else if (hunger > 800D) {
			tempPerformance -= (hunger - 800D) * HUNGER_PERFORMANCE_MODIFIER / 4;
		}

		// High fatigue reduces performance.
		if (fatigue > 1500D) {
			tempPerformance -= (fatigue - 1500D) * FATIGUE_PERFORMANCE_MODIFIER / 2;
		} else if (fatigue > 700D) {
			tempPerformance -= (fatigue - 700D) * FATIGUE_PERFORMANCE_MODIFIER / 4;
		}

		// High stress reduces performance.
		if (stress > 70D) {
			tempPerformance -= (stress - 70D) * STRESS_PERFORMANCE_MODIFIER / 8;
		} else if (stress > 50D) {
			tempPerformance -= (stress - 50D) * STRESS_PERFORMANCE_MODIFIER / 4;
		}

		// High kJoules improves performance and low kJoules hurts performance.
		if (kJoules > 7500) {
			tempPerformance += (kJoules - 7500) * ENERGY_PERFORMANCE_MODIFIER / 8;
		} else if (kJoules < 400) {
			tempPerformance -= 400_000 / kJoules * ENERGY_PERFORMANCE_MODIFIER / 4;
		}

		// The adjusted performance can not be more than the baseline max
		if (tempPerformance > maxPerformance) {
			tempPerformance = maxPerformance;
		}
		setPerformanceFactor(tempPerformance);

	}

	/**
	 * Gives the status of a person's hunger level.
	 *
	 * @param hunger
	 * @return status
	 */
	public static String getHungerStatus(double hunger, double energy) {
		String status;
		if (hunger < 50 && energy > 15000) // Full
			status = ENERGY_LEVEL_1;
		else if (hunger < 250 && energy > 10000) // Satisfied
			status = ENERGY_LEVEL_2;
		else if (hunger < 500 && energy > 5000) // Comfy
			status = ENERGY_LEVEL_3;
		else if (hunger < 750 && energy > ENERGY_THRESHOLD) // Adequate
			status = ENERGY_LEVEL_4;
		else if (hunger < 1000 && energy > 1000) // Rumbling
			status = ENERGY_LEVEL_5;
		else if (hunger < 1500 && energy > 500) // Ravenous
			status = ENERGY_LEVEL_6;
		else // Famished
			status = ENERGY_LEVEL_7;
		return status;
	}

	/**
	 * Gives the status of a person's water level.
	 *
	 * @param water
	 * @return status
	 */
	public static String getThirstyStatus(double thirst) {
		String status;
		if (thirst < 150)
			status = WATER_LEVEL_1;
		else if (thirst < 500)
			status = WATER_LEVEL_2;
		else if (thirst < 1000)
			status = WATER_LEVEL_3;
		else if (thirst < 1600)
			// Note : Use getDehydrationStartTime()
			status = WATER_LEVEL_4;
		else
			status = WATER_LEVEL_5;
		return status;
	}

	/**
	 * Gives the status of a person's fatigue level.
	 *
	 * @param fatigue
	 * @return status
	 */
	public static String getFatigueStatus(double value) {
		String status;
		if (value < 500)
			status = FATIGUE_LEVEL_1;
		else if (value < 800)
			status = FATIGUE_LEVEL_2;
		else if (value < 1200)
			status = FATIGUE_LEVEL_3;
		else if (value < 1600)
			status = FATIGUE_LEVEL_4;
		else
			status = FATIGUE_LEVEL_5;
		return status;
	}

	/**
	 * Gives the status of a person's stress level.
	 *
	 * @param hunger
	 * @return status
	 */
	public static String getStressStatus(double value) {
		String status;
		if (value < 10)
			status = STRESS_LEVEL_1;
		else if (value < 40)
			status = STRESS_LEVEL_2;
		else if (value < 75)
			status = STRESS_LEVEL_3;
		else if (value < 95)
			status = STRESS_LEVEL_4;
		else
			status = STRESS_LEVEL_5;
		return status;
	}

	/**
	 * Gives the status of a person's hunger level.
	 *
	 * @param hunger
	 * @return status
	 */
	public static String getPerformanceStatus(double value) {
		String status;
		if (value > .95)
			status = PERF_LEVEL_1;
		else if (value > .75)
			status = PERF_LEVEL_2;
		else if (value > .50)
			status = PERF_LEVEL_3;
		else if (value > .25)
			status = PERF_LEVEL_4;
		else
			status = PERF_LEVEL_5;
		return status;
	}

	/**
	 * Checks if the person has any mostSeriousProblem medical problems.
	 *
	 * @return true if mostSeriousProblem medical problems
	 */
	public boolean hasSeriousMedicalProblems() {
		boolean result = false;
		Iterator<HealthProblem> meds = getProblems().iterator();
		while (meds.hasNext()) {
			if (meds.next().getIllness().getSeriousness() >= 50)
				result = true;
		}
		return result;
	}

	/**
	 * Checks if a person is super unfit.
	 *
	 * @return true if a person is super fit
	 */
	public boolean isSuperUnFit() {
        return (fatigue > 1000) || (stress > 90) || (hunger > 1000) || (thirst > 1000) || (kJoules < 2000)
                || hasSeriousMedicalProblems();
    }
	
	/**
	 * Checks if a person is unfit.
	 *
	 * @return true if a person is unfit
	 */
	public boolean isUnFit() {
        return (fatigue > 500) || (stress > 50) || (hunger > 500) || (thirst > 500) || (kJoules < 6000)
                || hasSeriousMedicalProblems();
    }
	
	/**
	 * Checks if a person is nominally fit.
	 *
	 * @return true if a person is nominally fit
	 */
	public boolean isNominallyFit() {
        return (fatigue <= 500) && (stress <= 50) && (hunger <= 500) && (thirst <= 500) && (kJoules > 6000)
                && !hasSeriousMedicalProblems();
    }
	
	/**
	 * Checks fitness against some maximum levels.
	 * 
	 * @param fatMax
	 * @param stressMax
	 * @param hunMax
	 * @return
	 */
	public boolean isFitByLevel(int fatMax, int stressMax, int hunMax) {
        return isFitByLevel(fatMax, stressMax, hunMax, hunMax/2);
	}

	/**
	 *  Checks fitness against some maximum levels.
	 *  
	 * @param fatMax
	 * @param stressMax
	 * @param hunMax
	 * @param thirstMax
	 * @return
	 */
	public boolean isFitByLevel(int fatMax, int stressMax, int hunMax, int thirstMax) {
        return ((fatigue < fatMax) && (stress < stressMax)
        		&& (hunger < hunMax) && (thirst < thirstMax));
	}
	
	/**
	 * Screens if the person is fit for an heavy duty EVA task.
	 * 
	 * @return
	 */
	public boolean isFitEVAScreening() {
        return isFitByLevel(350, 35, 350, 200);
	}
	
	/**
	 * Returns the health score of a person. 0 being lowest. 100 being the highest.
	 * 
	 * @return
	 */
	public double computeHealthScore() {
		return (Math.max(100 - fatigue/10, 0) 
				+ Math.max(100 - stress, 0) 
				+ Math.max(100 - hunger/10, 0) 
				+ Math.max(100 - thirst/10, 0) 
				+ Math.max(100 - performance * 100, 0))
				/ 5.0;
	}
	
	/**
	 * Computes the fitness level.
	 *
	 * @return
	 */
	public int computeFitnessLevel() {
		int level = 0;
		if (hasSeriousMedicalProblems()) {
			return 0;
		}

		if (fatigue < 100 && stress < 10 && hunger < 100 && thirst < 50 && kJoules > 12000)
        	level = 5;
		else if (fatigue < 250 && stress < 25 && hunger < 250 && thirst < 125 && kJoules > 10000)
        	level = 4;
        else if (fatigue < 500 && stress < 50 && hunger < 500 && thirst < 250 && kJoules > 8000)
        	level = 3;
        else if (fatigue < 800 && stress < 65 && hunger < 800 && thirst < 400 && kJoules > 6000)
        	level = 2;
        else if (fatigue < 1200 && stress < 80 && hunger < 1200 && thirst < 600 && kJoules > 4000)
        	level = 1;
        else if (fatigue < 1800 && stress < 95 && hunger < 1800 && thirst < 900 && kJoules > 2000)
        	level = 0;

        return level;
	}

	/**
	 * Gets a list of medication affecting the person.
	 *
	 * @return list of medication.
	 */
	public List<Medication> getMedicationList() {
		return new CopyOnWriteArrayList<>(medicationList);
	}
	
	/**
	 * Checks if the person is affected by the given medication.
	 *
	 * @param medicationName the name of the medication.
	 * @return true if person is affected by it.
	 */
	public boolean hasMedication(String medicationName) {
		if (medicationName == null)
			throw new IllegalArgumentException("medicationName is null");

		boolean result = false;

		Iterator<Medication> i = medicationList.iterator();
		while (i.hasNext()) {
			if (medicationName.equals(i.next().getName()))
				result = true;
		}

		return result;
	}

	/**
	 * Adds a medication that affects the person.
	 *
	 * @param medication the medication to add.
	 */
	public void addMedication(Medication medication) {
		if (medication == null)
			throw new IllegalArgumentException("medication is null");
		medicationList.add(medication);
	}

	
	/**
	 * Gets the health risk probability map.
	 * 
	 * @return
	 */
	public Map<HealthRiskType, Double> getHealthRisks() {
		return healthRisks;
	}
	
	
	/**
	 * Gets the oxygen consumption rate per Sol.
	 *
	 * @return oxygen consumed (kg/Sol)
	 * @throws Exception if error in configuration.
	 */
	public static double getOxygenConsumptionRate() {
		return o2Consumption;
	}

	/**
	 * Gets the water consumption rate per Sol.
	 *
	 * @return water consumed (kg/Sol)
	 * @throws Exception if error in configuration.
	 */
	public static double getWaterConsumptionRate() {
		return h20Consumption;
	}

	public double getWaterConsumedPerServing() {
		return waterConsumedPerServing;
	}

	/**
	 * Gets the food consumption rate per Sol.
	 *
	 * @return food consumed (kg/Sol)
	 * @throws Exception if error in configuration.
	 */
	public static double getFoodConsumptionRate() {
		return foodConsumption;
	}


	public RadiationExposure getRadiationExposure() {
		return radiation;
	}


	public double getBodyMassDeviation() {
		return bodyMassDeviation;
	}

	public boolean isStressedOut() {
		return isStressedOut;
	}

	public boolean isRadiationPoisoned() {
		return isRadiationPoisoned;
	}
	
	public void workout(double time) {
		// Regulates hormones
		circadian.exercise(time);
		// Improves musculoskeletal systems
		exerciseMuscle(time);
		// Record the sleep time [in millisols]
		circadian.recordExercise(time);
	}
	
	/**
	 * Stress out the musculoskeletal systems.
	 * 
	 * @param time
	 */
	public void stressMuscle(double time) {
		muscleHealth -= .01 * time; // musculoskeletal health
		if (muscleHealth < 0)
			muscleHealth = 0;
		muscleSoreness += .005 * time; // musculoskeletal soreness
		if (muscleSoreness > 100)
			muscleSoreness = 100;
	}
	
	/**
	 * Works out the musculoskeletal systems.
	 * 
	 * @param time
	 */
	public void exerciseMuscle(double time) {
		musclePainTolerance += .001 * time; // musculoskeletal pain tolerance
		muscleHealth += .01 * time; // musculoskeletal health
		muscleSoreness -= .001 * time; // musculoskeletal soreness
		if (musclePainTolerance > 100)
			musclePainTolerance = 100;
		if (muscleHealth > 100)
			muscleHealth = 100;
		if (muscleSoreness < 0)
			muscleSoreness = 0;
		// Increase thirst
		increaseThirst(-time/4.5); 
	}

	/**
	 * Puts the muscle to rest.
	 * 
	 * @param time
	 */
	public void relaxMuscle(double time) {
		muscleHealth += .01 * time; // musculoskeletal health
		muscleSoreness -= .01 * time; // musculoskeletal soreness
		if (muscleHealth > 100)
			muscleHealth = 100;
		if (muscleSoreness < 0)
			muscleSoreness = 0;
	}
	
	/**
	 * Represent the deterioration of musculoskeletal systems.
	 */
	public void entropy(double time) {
		musclePainTolerance -= .001 * time; // muscle health
		muscleHealth -= .001 * time; // muscle health
		muscleSoreness += .001 * time; // muscle health
		if (muscleSoreness > 100)
			muscleSoreness = 100;
		if (muscleHealth < 0)
			muscleHealth = 0;
		if (musclePainTolerance < 0)
			musclePainTolerance = 0;
	}
	

	/**
	 * Reduces the muscle soreness.
	 * 
	 * @param value
	 */
	public void recoverFromSoreness(double value) {
		// Reduce the muscle soreness by 1 point at the end of the day
		double soreness = muscleSoreness;
		soreness = soreness - value * 0.01;
		if (soreness < 0)
			soreness = 0;
		else if (soreness > 100)
			soreness = 100;
		muscleSoreness = soreness;
	}


	/**
	 * Checks if it passes the hunger threshold
	 *
	 * @return
	 */
	public boolean isHungry() {
		return hunger > HUNGER_THRESHOLD || kJoules < ENERGY_THRESHOLD * 2;
	}

	/**
	 * Checks if it passes the thirst threshold
	 *
	 * @return
	 */
	public boolean isThirsty() {
		return thirst > THIRST_THRESHOLD;
	}

	/**
	 * Checks if it passes the fatigue threshold
	 *
	 * @return
	 */
	public boolean isSleepy() {
		return fatigue > FATIGUE_THRESHOLD;
	}

	/**
	 * Checks if it passes the stress threshold
	 *
	 * @return
	 */
	public boolean isStressed() {
		return stress > STRESS_THRESHOLD;
	}

	public double getStrengthMod() {
		return (endurance * .6 - strength * .4) / 100D;
	}

	public void reduceRemainingPrebreathingTime(double time) {
		remainingPrebreathingTime -= time;
	}

	public boolean isDonePrebreathing() {
        return remainingPrebreathingTime <= 0;
    }

	public boolean isAtLeast3QuartersDonePrebreathing() {
        return remainingPrebreathingTime <= .25 * STANDARD_PREBREATHING_TIME;
	}
	
	public boolean isAtLeastHalfDonePrebreathing() {
        return remainingPrebreathingTime <= .5 * STANDARD_PREBREATHING_TIME;
    }
	
	public boolean isAtLeastAQuarterDonePrebreathing() {
        return remainingPrebreathingTime <= .75 * STANDARD_PREBREATHING_TIME;
	}
	
	public void resetRemainingPrebreathingTime() {
		remainingPrebreathingTime = STANDARD_PREBREATHING_TIME + RandomUtil.getRandomInt(-5, 5);
	}

	
	/**
	 * Has this person eaten the amount of food that exceed the max daily limits ? 
	 * 
	 * @return
	 */
	public boolean eatTooMuch() {
		double foodEaten = 0;
		Double f = consumption.getDataPoint(0);
		if (f != null)
			foodEaten = f.doubleValue();
		
		double mealEaten = 0;
		Double m = consumption.getDataPoint(1);
		if (m != null)
			mealEaten = m.doubleValue();
		
		double dessertEaten = 0;
		Double d = consumption.getDataPoint(2);
		if (d != null)
			dessertEaten = d.doubleValue();
		if (foodEaten + mealEaten + dessertEaten >= foodConsumption * 1.5
				&& hunger < HUNGER_THRESHOLD)
			return true;

		return false;
	}
	
	/**
	 * Has this person drank the amount of water that exceed the max daily limits ? 
	 * 
	 * @return
	 */
	public boolean drinkEnoughWater() {
		Double w = consumption.getDataPoint(3);
		if (w != null) {
			if (w.doubleValue() >= h20Consumption * 1.5
					&& thirst < THIRST_THRESHOLD)
				return true;
		}

		return false;
	}
	
	/**
	 * Records the amount of food/water consumption in kg.
	 * Types :
	 * 0 = preserved food
	 * 1 = meal
	 * 2 = dessert
	 * 3 = water
	 * 
	 * @param amount in kg
	 * @param type
	 */
	public void recordFoodConsumption(double amount, int type) {
//		if (type == 0)
//			logger.info(person, "Eaten " + Math.round(amount * 1000D)/1000D + " kg preserved food.");
		consumption.increaseDataPoint(type, amount);
	}
	
	/**
	 * Returns the consumption history.
	 * 
	 * @return
	 */
	public Map<Integer, Map<Integer, Double>> getConsumptionHistory() {
		return consumption.getHistory();
	}
	
	/**
	 * Gets the daily average water usage of the last x sols Not: most weight on
	 * yesterday's usage. Least weight on usage from x sols ago
	 *
	 * @param type the id of the resource
	 * @return the amount of resource consumed in a day
	 */
	public double getDailyFoodUsage(int type) {
		return consumption.getDailyAverage(type);
	}
	
	/**
	 * Adds the amount of gas consumed.
	 *
	 * @param type
	 * @param amount
	 */
	public void addGasConsumed(int type, double amount) {
		if (type == OXYGEN_ID)
			consumption.increaseDataPoint(4, amount);
	}
	
	/**
	 * Gets the daily gas consumption.
	 *
	 * @param type the id of the resource
	 * @return the amount of resource consumed in a day
	 */
	public double getGasUsage(int type) {
		if (type == OXYGEN_ID)
			return consumption.getDailyAverage(4);
		return 0;
	}

	public double getMuscleSoreness() {
		return muscleSoreness;
	}

    public double getMusclePainTolerance() {
        return musclePainTolerance;
    }

	/**
	 * Initializes that static instances.
	 * 
	 * @param s
	 * @param c0
	 * @param c1
	 * @param m
	 */
	public static void initializeInstances(MasterClock c0, MedicalManager m,
											PersonConfig pc) {
		medicalManager = m;
		personConfig = pc;
		master = c0;

		h20Consumption = personConfig.getWaterConsumptionRate(); // 3 kg per sol
		o2Consumption = personConfig.getNominalO2ConsumptionRate();

		minAirPressure = personConfig.getMinAirPressure();
		minTemperature = personConfig.getMinTemperature();
		maxTemperature = personConfig.getMaxTemperature();
		foodConsumption = personConfig.getFoodConsumptionRate();

		RadiationExposure.initializeInstances(c0);
	}

	public void reinit() {
		circadian = person.getCircadianClock();
		naturalAttributeManager = person.getNaturalAttributeManager();
	}

	/**
	 * Prepares object for garbage collection.
	 */
	public void destroy() {

		deathDetails = null;
		problems = null;
		mostSeriousProblem = null;
		person = null;
		radiation = null;
		circadian = null;
		starved = null;
		dehydrated = null;
		medicationList = null;
	}

}
