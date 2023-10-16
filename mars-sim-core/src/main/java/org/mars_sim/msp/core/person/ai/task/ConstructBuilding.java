/*
 * Mars Simulation Project
 * ConstructBuilding.java
 * @date 2023-07-24
 * @author Scott Davis
 */
package org.mars_sim.msp.core.person.ai.task;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.mars_sim.mapdata.location.LocalPosition;
import org.mars_sim.msp.core.LocalAreaUtil;
import org.mars_sim.msp.core.logging.SimLogger;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.ai.NaturalAttributeType;
import org.mars_sim.msp.core.person.ai.SkillType;
import org.mars_sim.msp.core.person.ai.mission.ConstructionMission;
import org.mars_sim.msp.core.person.ai.mission.Mission;
import org.mars_sim.msp.core.person.ai.task.util.TaskPhase;
import org.mars_sim.msp.core.structure.Airlock;
import org.mars_sim.msp.core.structure.Settlement;
import org.mars_sim.msp.core.structure.construction.ConstructionSite;
import org.mars_sim.msp.core.structure.construction.ConstructionStage;
import org.mars_sim.msp.core.vehicle.Crewable;
import org.mars_sim.msp.core.vehicle.GroundVehicle;
import org.mars_sim.msp.core.vehicle.LightUtilityVehicle;
import org.mars_sim.tools.Msg;
import org.mars_sim.tools.util.RandomUtil;

/**
 * Task for constructing a building construction site stage.
 */
public class ConstructBuilding extends EVAOperation {

	/** default serial id. */
	private static final long serialVersionUID = 1L;

	/** default logger. */
	private static final SimLogger logger = SimLogger.getLogger(ConstructBuilding.class.getName());

	/** Task name */
	private static final String NAME = Msg.getString("Task.description.constructBuilding"); //$NON-NLS-1$

	/** Task phases. */
	private static final TaskPhase CONSTRUCTION = new TaskPhase(Msg.getString("Task.phase.construction")); //$NON-NLS-1$

	// The base chance of an accident while operating LUV per millisol.
	public static final double BASE_LUV_ACCIDENT_CHANCE = .001;

	// Data members.
	private boolean operatingLUV;
	
	private double cumulativeWorkTime;

	private ConstructionStage stage;
	private ConstructionSite site;
	private LightUtilityVehicle luv;

	private List<GroundVehicle> vehicles;

	/**
	 * Constructor.
	 *
	 * @param person the person performing the task.
	 */
	public ConstructBuilding(Person person) {
		// Use EVAOperation parent constructor.
		super(NAME, person, true, RandomUtil.getRandomDouble(10) + 150D, SkillType.CONSTRUCTION);

		if (person.isSuperUnFit()) {
			checkLocation("Person is unfit.");
        	return;
		}

		ConstructionMission mission = getMissionNeedingAssistance(person);

		if ((mission != null) && canConstruct(person, mission.getConstructionSite())) {

			// Initialize data members.
			this.stage = mission.getConstructionStage();
			this.site = mission.getConstructionSite();
			this.vehicles = mission.getConstructionVehicles();

			// Determine location for construction site.
			LocalPosition constructionSiteLoc = determineConstructionLocation();
			setOutsideSiteLocation(constructionSiteLoc);

			// Add task phase
			addPhase(CONSTRUCTION);
		}

		else {
			endTask();
		}
	}

	/**
	 * Constructor.
	 *
	 * @param person   the person performing the task.
	 * @param stage    the construction site stage.
	 * @param vehicles the construction vehicles.
	 * @throws Exception if error constructing task.
	 */
	public ConstructBuilding(Person person, ConstructionStage stage, ConstructionSite site,
			List<GroundVehicle> vehicles) {
		// Use EVAOperation parent constructor.
		super(NAME, person, true, RandomUtil.getRandomDouble(5D) + 100D, SkillType.CONSTRUCTION);

		// Initialize data members.
		this.stage = stage;
		this.site = site;
		this.vehicles = vehicles;

		if (person.isSuperUnFit()) {
			checkLocation("Person is unfit.");
        	return;
		}

		// Determine location for construction site.
		LocalPosition constructionSiteLoc = determineConstructionLocation();
		setOutsideSiteLocation(constructionSiteLoc);

		// Add task phase
		addPhase(CONSTRUCTION);
	}

	/**
	 * Checks if a given person can work on construction at this time.
	 *
	 * @param person the person.
	 * @return true if person can construct.
	 */
	public static boolean canConstruct(Person person, ConstructionSite site) {

		// Check if person can exit the settlement airlock.
		Airlock airlock = getClosestWalkableAvailableAirlock(person, site.getPosition(), false);
		if (airlock != null && !ExitAirlock.canExitAirlock(person, airlock))
			return false;

		// Check if person's medical condition will not allow task.
		if (person.getPerformanceRating() < .3D)
			return false;

		if (person.isSuperUnFit())
			return false;
		
		// Check if there is work that can be done on the construction stage.
		ConstructionStage stage = site.getCurrentConstructionStage();

		boolean workAvailable = false;

		// Checking stage for NullPointerException
		if (stage != null)
			workAvailable = stage.getCompletableWorkTime() > stage.getCompletedWorkTime();

		return (workAvailable);
	}

	/**
	 * Gets a random building construction mission that needs assistance.
	 *
	 * @return construction mission or null if none found.
	 */
	public static ConstructionMission getMissionNeedingAssistance(Person person) {

		ConstructionMission result = null;

		List<ConstructionMission> constructionMissions = getAllMissionsNeedingAssistance(person.getAssociatedSettlement());

		if (constructionMissions.size() > 0) {
			int index = RandomUtil.getRandomInt(constructionMissions.size() - 1);
			result = (ConstructionMission) constructionMissions.get(index);
		}

		return result;
	}

	/**
	 * Gets a list of all building construction missions that need assistance at a
	 * settlement.
	 *
	 * @param settlement the settlement.
	 * @return list of building construction missions.
	 */
	public static List<ConstructionMission> getAllMissionsNeedingAssistance(Settlement settlement) {

		List<ConstructionMission> result = new CopyOnWriteArrayList<>();

		Iterator<Mission> i = missionManager.getMissionsForSettlement(settlement).iterator();
		while (i.hasNext()) {
			Mission mission = (Mission) i.next();
			if (mission instanceof ConstructionMission bcMission) {
				result.add(bcMission);
			}
		}

		return result;
	}

	/**
	 * Determines location to go to at construction site.
	 *
	 * @return location.
	 */
	private LocalPosition determineConstructionLocation() {
		return LocalAreaUtil.getRandomLocalRelativePosition(site);
	}

	@Override
	protected TaskPhase getOutsideSitePhase() {
		return CONSTRUCTION;
	}

	@Override
	protected double performMappedPhase(double time) {

		time = super.performMappedPhase(time);
		if (!isDone()) {
			if (getPhase() == null) {
				throw new IllegalArgumentException("Task phase is null");
			} else if (CONSTRUCTION.equals(getPhase())) {
				time = constructionPhase(time);
			}
		}
		return time;
	}

	/**
	 * Performs the construction phase of the task.
	 *
	 * @param time amount (millisols) of time to perform the phase.
	 * @return time (millisols) remaining after performing the phase.
	 * @throws Exception
	 */
	private double constructionPhase(double time) {
		double remainingTime = 0;
		
		if (checkReadiness(time, true) > 0)
			return remainingTime;
		
		// Operate light utility vehicle if no one else is operating it.
		if (!operatingLUV) {
			obtainVehicle();
		}

		// Determine effective work time based on "Construction" and "EVA Operations"
		// skills.
		double workTime = time;
		int skill = getEffectiveSkillLevel();
		if (skill == 0) {
			workTime /= 2;
		} else if (skill > 1) {
			workTime += workTime * (.2D * skill);
		}

		// Work on construction.
		stage.addWorkTime(workTime);

		// Keep track of cumulative work time
		cumulativeWorkTime += workTime;
		
		// Add experience points
		addExperience(workTime);

		// Check if an accident happens during construction.
		checkForAccident(workTime);
		
		boolean availableWork = stage.getCompletableWorkTime() > stage.getCompletedWorkTime();

		// Check if site duration has ended or there is reason to cut the construction
		// phase short and return to the rover.
		if (person != null
			&& (stage.isComplete() || !availableWork)) {

			logger.info(site, person, "cumulativeWorkTime: " + Math.round(cumulativeWorkTime * 10.0)/10.0);
			
			// End operating light utility vehicle.
			if (luv != null
				&& ((Crewable)luv).isCrewmember(person)) {
				returnVehicle();
			}

			checkLocation("Stage completed.");
			return workTime;
		}

		return 0;
	}

	/**
	 * Obtains a construction vehicle from the settlement if possible.
	 *
	 * @throws Exception if error obtaining construction vehicle.
	 */
	private void obtainVehicle() {
		Iterator<GroundVehicle> i = vehicles.iterator();
		while (i.hasNext() && (luv == null)) {
			GroundVehicle vehicle = i.next();
			if (!vehicle.getMalfunctionManager().hasMalfunction()) {
				if (vehicle instanceof LightUtilityVehicle) {
					LightUtilityVehicle tempLuv = (LightUtilityVehicle) vehicle;
					if (tempLuv.getOperator() == null) {
						tempLuv.addPerson(person);
						tempLuv.setOperator(person);

						luv = tempLuv;
						operatingLUV = true;

						// Place light utility vehicles at random location in construction site.
						LocalPosition settlementLocSite = LocalAreaUtil.getRandomLocalRelativePosition(site);
						luv.setParkedLocation(settlementLocSite, RandomUtil.getRandomDouble(360D));

						break;
					}
				}
			}
		}
	}

	/**
	 * Returns the construction vehicle used to the settlement.
	 *
	 * @throws Exception if error returning construction vehicle.
	 */
	private void returnVehicle() {
		luv.removePerson(person);
		luv.setOperator(null);
		operatingLUV = false;
	}

	@Override
	protected void addExperience(double time) {
		super.addExperience(time);

		// If person is driving the light utility vehicle, add experience to driving
		// skill.
		// 1 base experience point per 10 millisols of mining time spent.
		// Experience points adjusted by person's "Experience Aptitude" attribute.
		if ((CONSTRUCTION.equals(getPhase())) && operatingLUV) {
			int experienceAptitude = worker.getNaturalAttributeManager().getAttribute(NaturalAttributeType.EXPERIENCE_APTITUDE);

			double experienceAptitudeModifier = (((double) experienceAptitude) - 50D) / 100D;
			double drivingExperience = time / 10D;
			drivingExperience += drivingExperience * experienceAptitudeModifier;
			worker.getSkillManager().addExperience(SkillType.PILOTING, drivingExperience, time);
		}
	}

	protected void checkForAccident(double time) {
		super.checkForAccident(time);

		// Check for light utility vehicle accident if operating one.
		if (operatingLUV) {
			// Driving skill modification.
			int skill = worker.getSkillManager().getEffectiveSkillLevel(SkillType.EVA_OPERATIONS);
			checkForAccident(luv, time, BASE_LUV_ACCIDENT_CHANCE, skill, site.getName());
		}
	}

	@Override
	protected boolean shouldEndEVAOperation(boolean checkLight) {
		boolean result = super.shouldEndEVAOperation(checkLight);

		// If operating LUV, check if LUV has malfunction.
		if (operatingLUV && luv.getMalfunctionManager().hasMalfunction()) {
			result = true;
		}

		return result;
	}

	/**
	 * Gets the construction stage that is being worked on.
	 *
	 * @return construction stage.
	 */
	public ConstructionStage getConstructionStage() {
		return stage;
	}

	public void destroy() {
		stage = null;
		site = null;
		luv = null;
		
		vehicles.clear();
		vehicles = null;
	}
}
