/**
 * Mars Simulation Project
 * Mind.java
 * @version 3.1.2 2020-09-02
 * @author Scott Davis
 */
package org.mars_sim.msp.core.person.ai;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.mars_sim.msp.core.LogConsolidated;
import org.mars_sim.msp.core.Simulation;
import org.mars_sim.msp.core.UnitEventType;
import org.mars_sim.msp.core.mars.SurfaceFeatures;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.ai.job.Job;
import org.mars_sim.msp.core.person.ai.job.JobAssignmentType;
import org.mars_sim.msp.core.person.ai.job.JobHistory;
import org.mars_sim.msp.core.person.ai.job.JobUtil;
import org.mars_sim.msp.core.person.ai.job.Politician;
import org.mars_sim.msp.core.person.ai.mission.Mission;
import org.mars_sim.msp.core.person.ai.mission.MissionManager;
import org.mars_sim.msp.core.person.ai.mission.VehicleMission;
import org.mars_sim.msp.core.person.ai.social.RelationshipManager;
import org.mars_sim.msp.core.person.ai.task.utils.Task;
import org.mars_sim.msp.core.person.ai.task.utils.TaskManager;
import org.mars_sim.msp.core.person.ai.task.utils.TaskSchedule;
import org.mars_sim.msp.core.time.ClockPulse;
import org.mars_sim.msp.core.time.Temporal;
import org.mars_sim.msp.core.tool.MathUtils;
import org.mars_sim.msp.core.tool.RandomUtil;

/**
 * The Mind class represents a person's mind. It keeps track of missions and
 * tasks which the person is involved.
 */
public class Mind implements Serializable, Temporal {

	/** default serial id. */
	private static final long serialVersionUID = 1L;

	/** default logger. */
	private static Logger logger = Logger.getLogger(Mind.class.getName());
	private static String loggerName = logger.getName();
	private static String sourceName = loggerName.substring(loggerName.lastIndexOf(".") + 1, loggerName.length());
	
	private static final int MAX_ZERO_EXECUTE = 10; // Maximum number of executeTask action that consume no time
	private static final int STRESS_UPDATE_CYCLE = 10;
	private static final double MINIMUM_MISSION_PERFORMANCE = 0.3;
	private static final double FACTOR = .05;
	private static final double SMALL_AMOUNT_OF_TIME = 0.001;

	// Data members
	/** Is the job locked so another can't be chosen? */
	private boolean jobLock;
	
	/** The person owning this mind. */
	private Person person = null;
	
	/** The person's task manager. */
	private TaskManager taskManager;
	/** The person's current mission (if any). */
	private Mission mission;
	/** The person's job. */
	private Job job;
	/** The person's personality. */
	private MBTIPersonality mbti;
	/** The person's emotional states. */	
	private EmotionManager emotion;
	/** The person's personality trait manager. */
	private PersonalityTraitManager trait;
	
//	/** The person's core mind. */
//	private CoreMind coreMind;

	private static MissionManager missionManager;
	private static RelationshipManager relationshipManager;
	private static SurfaceFeatures surfaceFeatures;

	static {
		Simulation sim = Simulation.instance();
		// Load the mission manager
		missionManager = sim.getMissionManager();
		// Load the relationship manager
		relationshipManager = sim.getRelationshipManager();
		// Load SurfaceFeatures
		surfaceFeatures = sim.getMars().getSurfaceFeatures();
	}
	
	/**
	 * Constructor 1.
	 * 
	 * @param person the person owning this mind
	 * @throws Exception if mind could not be created.
	 */
	public Mind(Person person) {
		// Initialize data members
		this.person = person;
		mission = null;
		job = null;
		jobLock = false;

//		Simulation sim = Simulation.instance();
//		// Load the marsClock
//		marsClock = sim.getMasterClock().getMarsClock();
//		// Load the mission manager
//		missionManager = sim.getMissionManager();
//		// Load the relationship manager
//		relationshipManager = sim.getRelationshipManager();
		
//		// Create CoreMind
//		coreMind = new CoreMind();
		// Construct the Big Five personality trait.
		trait = new PersonalityTraitManager(person);
		// Construct the MBTI personality type.
		mbti = new MBTIPersonality(person);
		// Construct the emotion states.
		emotion = new EmotionManager(person);
		// Construct the task manager
		taskManager = new TaskManager(this);
	}
	
	/**
	 * Time passing.
	 * 
	 * @param time the time passing (millisols)
	 * @throws Exception if error.
	 */
	@Override
	public boolean timePassing(ClockPulse pulse) {
		if (taskManager != null) {
			// Take action as necessary.
			takeAction(pulse.getElapsed());
			// Record the action (task/mission)
//			taskManager.recordFilterTask(time);
		}
		
		int msol = pulse.getMarsTime().getMillisolInt();
		if (msol % STRESS_UPDATE_CYCLE == 0) {

			// Update stress based on personality.
			mbti.updateStress(pulse.getElapsed());

			// Update emotion
			updateEmotion();
			
			// Update relationships.
			relationshipManager.timePassing(person, pulse.getElapsed());
		}

		// Note : for now a Mayor/Manager cannot switch job
		if (job instanceof Politician)
			jobLock = true;

		else {
			if (jobLock) {
				// Note: for non-manager, the new job will be locked in until the beginning of
				// the next day
				// check for the passing of each day
				if (pulse.isNewSol()) {
					jobLock = false;
				}
			} else
				checkJob();
		}
		
		return true;
	}

	/*
	 * Checks if a person has a job. If not, get a new one.
	 */
	private void checkJob() {
		// Check if this person needs to get a new job or change jobs.
		if (job == null) { // removing !jobLock
			// Note: getNewJob() is checking if existing job is "good enough"/ or has good
			// prospect
			Job newJob = JobUtil.getNewJob(person);
			// Already excluded mayor/manager job from being assigned in
			// JobManager.getNewJob()
			String newJobStr = newJob.getName(person.getGender());
			String jobStr = null;
			if (job != null)
				jobStr = job.getName(person.getGender());
			if (newJob != null) {
				if (!newJobStr.equals(jobStr)) {
					// job = newJob;
					setJob(newJob, false, JobUtil.SETTLEMENT, JobAssignmentType.APPROVED, JobUtil.SETTLEMENT);
				}
			}
		}
	}

	/**
	 * Assigns the first job at the start of the sim
	 * 
	 * @param assignedBy the authority that assigns the job
	 */
	public void getInitialJob(String assignedBy) {
		Job newJob = JobUtil.getNewJob(person);
		if (newJob != null)
			setJob(newJob, true, assignedBy, JobAssignmentType.APPROVED, assignedBy);
	}

	/**
	 * Take appropriate action for a given amount of time.
	 * 
	 * @param time time in millisols
	 * @throws Exception if error during action.
	 */
	private void takeAction(double time) {
		double remainingTime = time;
		int zeroCount = 0; // Count the number of conseq. zero executions
		
		// Loop around using up time; recursion can blow stack memory
		do {			
			// Perform a task if the person has one, or determine a new task/mission.
			if (taskManager.hasActiveTask()) {
				double newRemain = taskManager.executeTask(remainingTime, person.getPerformanceRating());

				// A task is return a bad remaining time. Cause of Issue#290
				if (!Double.isFinite(newRemain)) {
					// Likely to be a defect in a Task
					LogConsolidated.log(logger, Level.SEVERE, 20_000, sourceName,
							person + " doing '" 
							+ taskManager.getTaskName() + "' return an invalid time " + newRemain);
					return;
				}
				
				// Consumed time then reset the idle counter
				if (remainingTime == newRemain) {
					zeroCount++;
				}
				else {
					zeroCount = 0;
				}
				remainingTime = newRemain;
			}
			else {
				// don't have an active task
				lookForATask();
				if (!taskManager.hasActiveTask()) {
					// Didn't find a new Task so abort action
					remainingTime = 0;
				}
				zeroCount = 0;
			}
		}
		while ((zeroCount < MAX_ZERO_EXECUTE) && (remainingTime > SMALL_AMOUNT_OF_TIME));
	}

	/**
	 * Looks for a new task
	 */
	private void lookForATask() {
		
//		LogConsolidated.log(Level.INFO, 20_000, sourceName,
//				person + " had no active task.");

		if ((mission != null) && mission.isDone()) {
			// Set the mission to null since it is done
			mission = null;
		}

		boolean hasActiveMission = hasActiveMission();

		if (hasActiveMission) {

			// If the mission vehicle has embarked but the person is not on board, 
			// then release the person from the mission
			if (!(mission.getCurrentMissionLocation().equals(person.getCoordinates()))) {
				mission.removeMember(person);
				mission = null;
				selectNewTask();
			}
				
			else {
		        boolean inDarkPolarRegion = surfaceFeatures.inDarkPolarRegion(mission.getCurrentMissionLocation());
				double sunlight = surfaceFeatures.getSolarIrradiance(mission.getCurrentMissionLocation());
				if ((sunlight == 0) && !inDarkPolarRegion) {
					if (mission.getPhase() != null)
						resumeMission(-2);
					else
						selectNewTask();
				}
				
				// Checks if a person is tired, too stressful or hungry and need 
				// to take break, eat and/or sleep
				else if (!person.getPhysicalCondition().isFit()
		        	&& !mission.hasDangerousMedicalProblemsAllCrew()) {
		        	// Cannot perform the mission if a person is not well
		        	// Note: If everyone has dangerous medical condition during a mission, 
		        	// then it won't matter and someone needs to drive the rover home.
					// Add penalty in resuming the mission
					if (mission.getPhase() != null)
						resumeMission(-1);
					else
						selectNewTask();
				}
				
		        else if (VehicleMission.REVIEWING.equals(mission.getPhase())) {
		        	if (!mission.getStartingMember().equals(person)) {
			        	// If the mission is still pending upon approving, then only the mission lead
			        	// needs to perform the mission and rest of the crew can do something else to 
			        	// get themselves ready.
						selectNewTask();
		        	}
		        	else
		        		resumeMission(0);		
				}
				
				else if (mission.getPhase() != null) {
					resumeMission(0);
				}
				else
					selectNewTask();
			}
		}
		
		else {  // don't have an active mission
			selectNewTask();
		}
	}
	
	public void resumeMission(int modifier) {
		if (VehicleMission.TRAVELLING.equals(mission.getPhase())) {
			if (!taskManager.hasActiveTask() && mission.getVehicle().getOperator() == null) {
				// if no one is driving the vehicle and nobody is NOT doing field work, 
				// need to elect a driver right away
				checkMissionFitness(modifier);
			}
			else 
				selectNewTask();
		}
		// A Task is assigned but could be done
		//else if (taskManager.getPhase() != null) {
		else if (!taskManager.hasActiveTask()) {
			checkMissionFitness(modifier);
		}
		else
			selectNewTask();
	}
	
	
	public void checkMissionFitness(int modifier) {
		int fitness = person.getPhysicalCondition().computeFitnessLevel();
		int priority = mission.getPriority();
		int rand = RandomUtil.getRandomInt(6);
		if (rand - (fitness)/1.5D <= priority + modifier) {
//					// See if this person can ask for a mission
//					boolean newMission = !hasActiveMission && !hasAMission && !overrideMission && isInMissionWindow;							
			mission.performMission(person);
//					logger.info(person + " was to perform the " + mission + " mission");
		}
		
		else {
			selectNewTask();
		}
	}
	
	/**
	 * Checks if a person can start a new mission
	 * 
	 * @return
	 */
	public boolean canStartNewMission() {
		boolean hasAMission = hasAMission();
//		if (hasAMission)
//			logger.info(person + " had the " + mission + " mission");;
		
		boolean hasActiveMission = hasActiveMission();

		boolean overrideMission = false;

		// Check if mission creation at settlement (if any) is overridden.
		overrideMission = person.getAssociatedSettlement().getMissionCreationOverride();

		// Check if it's within the mission request window 
		// Within 100 millisols at the start of the work shift
		boolean isInMissionWindow = taskManager.getTaskSchedule().isPersonAtStartOfWorkShift(TaskSchedule.MISSION_WINDOW);

		// See if this person can ask for a mission
		return !hasActiveMission && !hasAMission && !overrideMission && isInMissionWindow;
	}
	
	/**
	 * Reassign the person's job.
	 * 
	 * @param newJob           the new job
	 * @param bypassingJobLock
	 */
	public void reassignJob(String newJobStr, boolean bypassingJobLock, String assignedBy, JobAssignmentType status,
			String approvedBy) {
		// TODO: Add position of the one who approve 
		// Called by
		// (1) ReviewJobReassignment's constructor or
		// (2) TabPanelCareer's actionPerformed() [for a pop <= 4 settlement]
		Job newJob = null;
		Iterator<Job> i = JobUtil.getJobs().iterator();
		while (i.hasNext()) {
			Job job = i.next();
			String n = job.getName(person.getGender());
			if (newJobStr.equals(n))
				// gets selectedJob by running through iterator to match it
				newJob = job;
		}

		assignJob(newJob, newJobStr, bypassingJobLock, assignedBy, status, approvedBy);
	}

	/**
	 * Sets the person's job.
	 * 
	 * @param newJob           the new job
	 * @param bypassingJobLock
	 * @param assignedBy
	 * @param status           of JobAssignmentType
	 * @param approvedBy
	 */
	public void setJob(Job newJob, boolean bypassingJobLock, String assignedBy, 
			JobAssignmentType status, String approvedBy) {
		// Called by
		// (1) setRole() in Person.java (if a person has a Manager role type)
		// (2) checkJob() in Mind.java
		// (3) getInitialJob() in Mind.java
		// TODO : if jobLock is true, will it allow the job to be changed?
//		System.out.println("Mind's setJob(). newJob is " + newJob);
		String newJobStr = newJob.getName(person.getGender());
		assignJob(newJob, newJobStr, bypassingJobLock, assignedBy, status, approvedBy);
	}

	/**
	 * Assigns a person a new job.
	 * 
	 * @param newJob           the new job
	 * @param bypassingJobLock
	 * @param assignedBy
	 * @param status           of JobAssignmentType
	 * @param approvedBy
	 */
	public void assignJob(Job newJob, String newJobStr, boolean bypassingJobLock, String assignedBy,
			JobAssignmentType status, String approvedBy) {
		String jobStr = null;
		JobHistory jh = person.getJobHistory();
//		Settlement s = person.getSettlement();

		if (job == null)
			jobStr = null;
		else
			jobStr = job.getName(person.getGender());

		// TODO : check if the initiator's role allows the job to be changed
		if (!newJobStr.equals(jobStr)) {

			if (bypassingJobLock || !jobLock) {
//				System.out.println("1 " + person + " " + person.getJobName() + " " + jobStr);
				job = newJob;
//				logger.info("2 " + person + " " + person.getJobName() + " " + newJobStr);
				// Set up 4 approvedBy conditions
				if (approvedBy.equals(JobUtil.SETTLEMENT)) { // automatically approved if pop <= 4
					jh.saveJob(newJob, assignedBy, status, approvedBy, true);
				} else if (approvedBy.equals(JobUtil.USER)) {
					jh.saveJob(newJob, assignedBy, status, approvedBy, true);
				} else if (approvedBy.equals(JobUtil.MISSION_CONTROL)) { // at the start of sim
					jh.saveJob(newJob, assignedBy, status, approvedBy, false);
				} else { // Call JobHistory's saveJob(),
						// approved by a Senior Official");
					jh.saveJob(newJob, assignedBy, status, approvedBy, false);
				}
				
				String s = String.format("[%s] %25s (Job) -> %s",
						person.getLocationTag().getLocale(), 
						person.getName(), 
						newJobStr);
				
				LogConsolidated.log(logger, Level.CONFIG, 0, sourceName, s);

				person.fireUnitUpdate(UnitEventType.JOB_EVENT, newJob);

				// Assign a new role type after the change of job
//				if (s != null 
//						// Exclude the person if he's a head
//						&& person.getRole().getType() != RoleType.COMMANDER
//						&& person.getRole().getType() != RoleType.SUB_COMMANDER
//						&& person.getRole().getType() != RoleType.MAYOR
//						&& person.getRole().getType() != RoleType.PRESIDENT) {
//					person.getRole().obtainRole(s);
//				}
				
				// the new job will be Locked in until the beginning of the next day
				jobLock = true;
			}
		}
	}
		
	/**
	 * Returns true if person has an active mission.
	 * 
	 * @return true for active mission
	 */
	public boolean hasActiveMission() {
        return (mission != null) && !mission.isDone();
	}

	/**
	 * Returns true if person has a mission.
	 * 
	 * @return true for active mission
	 */
	public boolean hasAMission() {
		if (mission != null) {
//			// has a mission but need to determine if this mission is active or not
//			if (mission.isApproved()
//				|| (mission.getPlan() != null 
//					&& mission.getPlan().getStatus() != PlanType.NOT_APPROVED))
				return true;
		}
		return false;
	}
	
	/**
	 * Set this mind as inactive. Needs move work on this; has to abort the Task can
	 * not just close it. This abort action would then allow the Mission to be also
	 * aborted.
	 */
	public void setInactive() {
		taskManager.clearAllTasks();
		if (hasActiveMission()) {
			mission.removeMember(person);
			mission = null;
		}
	}

	/**
	 * Sets the person's current mission.
	 * 
	 * @param newMission the new mission
	 */
	public void setMission(Mission newMission) {
		if (newMission != mission) {
			if (mission != null) {
				mission.removeMember(person);
			}
			mission = newMission;

			if (newMission != null) {
				newMission.addMember(person);
			}

			person.fireUnitUpdate(UnitEventType.MISSION_EVENT, newMission);
		}
	}

	/**
	 * Stops the person's current mission.
	 * 
	 */
	public void stopMission() {
		mission = null;
	}

	/**
	 * Determines a new task for the person.
	 */
	public void selectNewTask() {
		// Check if there are any assigned tasks that are pending
		if (taskManager.hasPendingTask()) {
			Task newTask = taskManager.getAPendingMetaTask().constructInstance(person);

			LogConsolidated.log(logger, Level.INFO, 0, sourceName,
					person.getName() + " had been given a task order of " + newTask.getName());
			taskManager.addTask(newTask, false);
			return;
		}

		// Just go direct to the Mind and get a new Task
		Task newTask = taskManager.getNewTask();
		
		if (newTask != null) {
			taskManager.addTask(newTask, false);
		}
		else
			logger.severe(person + "'s newTask is null.");
	}

	/**
	 * Determines a new mission for the person.
	 */
	public void getNewMission() {
		// If this Person is too weak then they can not do Missions
		if (person.getPerformanceRating() < MINIMUM_MISSION_PERFORMANCE) {
			return;
		}

		// The previous code was using an extra random that did not add any value because the extra weight was always 0
		Mission newMission = missionManager.getNewMission(person);
		if (newMission != null) {
			missionManager.addMission(newMission);
			setMission(newMission);
		}
	}

	
	/**
	 * Calls the psi function
	 * 
	 * @param av
	 * @param pv
	 * @return
	 */
	public double[] callPsi(double[] av, double[] pv) {
		double[] v = new double[2];
		
		for (int i=0; i<pv.length; i++) {
			if (i == 0) { // Openness
				if (pv[0] > .5) {
					v[0] = av[0] + pv[0]/2D * FACTOR; // Engagement
					v[1] = av[1] + pv[0]/2D * FACTOR; // Valence
				}
				else if (pv[0] < .5) {
					v[0] = av[0] - pv[0] * FACTOR; // Engagement
					v[1] = av[1] - pv[0] * FACTOR; // Valence
				}
			}
			else if (i == 1) { // Conscientiousness
				if (pv[1] > .5) {
//					v[0] = av[0] + pv[1]/2D * FACTOR; // Engagement
					v[1] = av[1] + pv[1]/2D * FACTOR; // Valence
				}
				else if (pv[1] < .5) {
//					v[0] = av[0] - pv[1] * FACTOR; // Engagement
					v[1] = av[1] - pv[1] * FACTOR; // Valence
				}

			}
			else if (i == 2) { // Extraversion
				if (pv[2] > .5) {
					v[0] = av[0] + pv[2]/2D * FACTOR;
//					v[1] = av[1] + pv[2]/2D * FACTOR;
				}
				else if (pv[2] < .5) {
					v[0] = av[0] - pv[2] * FACTOR;
//					v[1] = av[1] - pv[2] * FACTOR;
				}

			}
			else if (i == 3) { // Agreeableness
				if (pv[3] > .5) {
//					v[0] = av[0] + pv[3]/2D * FACTOR; // Engagement
					v[1] = av[1] + pv[3]/2D * FACTOR; // Valence
				}
				else if (pv[3] < .5) {
//					v[0] = av[0] - pv[3] * FACTOR;
					v[1] = av[1] - pv[3] * FACTOR;
				}
			}
			else if (i == 4) { // Neuroticism
				if (pv[4] > .5) {
					v[0] = av[0] - pv[4]/2D * FACTOR;
					v[1] = av[1] - pv[4]/2D * FACTOR;
				}
				else if (pv[4] < .5) {
					v[0] = av[0] + pv[4] * FACTOR;
					v[1] = av[1] + pv[4] * FACTOR;
				}
			}

			if (v[0] > .8)
				v[0] = .8;
			else if (v[0] < 0)
				v[0] = 0;

			if (v[1] > .8)
				v[1] = .8;
			else if (v[1] < 0)
				v[1] = 0;

		}
		
		return v;
	}
	
	/**
	 * Updates the emotion states
	 */
	public void updateEmotion() {
		// Check for stimulus
		emotion.checkStimulus();
		
//		int dim = emotion.getDimension();
		
		List<double[]> wVector = emotion.getOmegaVector(); // prior history
		double[] pVector = trait.getPersonalityVector(); // personality
		double[] aVector = emotion.getEmotionInfoVector(); // new stimulus
		double[] eVector = emotion.getEmotionVector();

		// Call Psi Function - with desire changes aVector
		double[] psi = callPsi(aVector, pVector);//new double[dim];
		
		// Call Omega Function - internal changes such as decay of emotional states
		// for normalize vectors
		double[] omega = MathUtils.normalize(wVector.get(wVector.size()-1)); //new double[dim];
		
		double[] newE = new double[2];
		
		for (int i=0; i<2; i++) {
			newE[i] = (eVector[i] + psi[i] + omega[i]) / 2.05;			
		}
		
		// Find the new emotion vector
		// java.lang.OutOfMemoryError: Java heap space
//		double[] e_tt = DoubleStream.concat(Arrays.stream(eVector),
//				Arrays.stream(psi)).toArray();
//		double[] e_tt2 = DoubleStream.concat(Arrays.stream(e_tt),
//				Arrays.stream(omega)).toArray();		
		// java.lang.OutOfMemoryError: Java heap space
//		double[] e_tt = MathUtils.concatAll(eVector, psi, omega);
		
		if (newE[0] > .8)
			newE[0] = .8;
		else if (newE[0] < 0)
			newE[0] = 0;

		if (newE[1] > .8)
			newE[1] = .8;
		else if (newE[1] < 0)
			newE[1] = 0;

		// Update the emotional states
		emotion.updateEmotion(newE);
		
	}
	
	public EmotionManager getEmotion() {
		return emotion;
	}
	
	/**
	 * Gets the person's MBTI (personality type).
	 * 
	 * @return personality type.
	 */
	public MBTIPersonality getMBTI() {
		return mbti;
	}

	/**
	 * Returns the person owning this mind.
	 * 
	 * @return person
	 */
	public Person getPerson() {
		return person;
	}

	/**
	 * Returns the person's task manager
	 * 
	 * @return task manager
	 */
	public TaskManager getTaskManager() {
		return taskManager;
	}

	/**
	 * Returns the person's current mission. Returns null if there is no current
	 * mission.
	 * 
	 * @return current mission
	 */
	public Mission getMission() {
		return mission;
	}

	/**
	 * Gets the person's job
	 * 
	 * @return job or null if none.
	 */
	public Job getJob() {
		return job;
	}

	/**
	 * Checks if the person's job is locked and can't be changed.
	 * 
	 * @return true if job lock.
	 */
	public boolean getJobLock() {
		return jobLock;
	}

	/*
	 * Set the value of jobLock so that the job can or cannot be changed
	 */
	public void setJobLock(boolean value) {
		jobLock = value;
	}

	public PersonalityTraitManager getTraitManager() {
		return trait;
	}

//	public void setCoreMind(String career) {
//		coreMind.create(career);	
//	}

	/**
	 * Reloads instances after loading from a saved sim
	 * 
	 * @param clock
	 */
	public static void initializeInstances(MissionManager m, RelationshipManager r) {
		relationshipManager = r;
		missionManager = m;
	}
	
	public void reinit() {
//		trait.reinit();
//		mbti.reinit();
//		emotion.reinit();
		taskManager.reinit();
	}
	
	/**
	 * Prepare object for garbage collection.
	 */
	public void destroy() {
		person = null;
		taskManager.destroy();
		if (mission != null)
			mission.destroy();
		mission = null;
		job = null;
		if (mbti != null)
			mbti.destroy();
		mbti = null;
//		skillManager.destroy();
//		skillManager = null;
	}
}
