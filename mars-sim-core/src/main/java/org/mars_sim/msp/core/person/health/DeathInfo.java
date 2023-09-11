/**
 * Mars Simulation Project
 * DeathInfo.java
 * @date 2021-12-22
 * @author Barry Evans
 */

package org.mars_sim.msp.core.person.health;

import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.mars.sim.mapdata.location.Coordinates;
import org.mars.sim.tools.util.RandomUtil;
import org.mars_sim.msp.core.Unit;
import org.mars_sim.msp.core.malfunction.Malfunction;
import org.mars_sim.msp.core.malfunction.MalfunctionManager;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.ai.Mind;
import org.mars_sim.msp.core.person.ai.job.util.JobType;
import org.mars_sim.msp.core.person.ai.mission.Mission;
import org.mars_sim.msp.core.person.ai.role.RoleType;
import org.mars_sim.msp.core.person.ai.task.util.TaskManager;
import org.mars_sim.msp.core.person.ai.task.util.TaskPhase;
import org.mars_sim.msp.core.robot.Robot;
import org.mars_sim.msp.core.time.MarsTime;

/**
 * This class represents the status of a Person when death occurs. It records
 * the Complaint that caused the death to occur, the time of death and the
 * Location.<br/>
 * The Location is recorded as a dead body may be moved from the place of death.
 * This class is immutable since once Death occurs it is final.
 */
public class DeathInfo implements Serializable {

	/** default serial id. */
	private static final long serialVersionUID = 1L;

	private static final Logger logger = Logger.getLogger(DeathInfo.class.getName());

	// Quotes from http://www.phrases.org.uk/quotes/last-words/suicide-notes.html
	// https://www.goodreads.com/quotes/tag/suicide-note
	private static final String[] LAST_WORDS = {"Take care of my family.",
						"One day, send my ashes back to Earth and give it to my family.",
						"Send my ashes to orbit around Mars one day.",
						"I have to move on. Farewell.",
						"I want to be buried outside the settlement.",
						"Take care my friend.",
						"I will be the patron saint for the future Mars generations.",
						"I'm leaving this world. No more sorrow to bear. See ya."};

	// Data members
	/** Has the body been retrieved for exam */	
	private boolean bodyRetrieved = false;	
	/** Is the postmortem exam done ? */	
	private boolean examDone = false;	
	/** Amount of time performed so far in postmortem exam [in Millisols]. */	
	private double timeSpentExam;
	/** Estimated time the postmortem exam should take [in Millisols]. */	
	private double estTotExamTime;
	/** Percent of illness*/	
	private double healthCondition;	
	/** Cause of death. */	
	private String causeOfDeath;
	/** Time of death. */
	private MarsTime timeOfDeath;
	/** Place of death. */
	private String placeOfDeath = "";
	/** Name of the doctor who did the postmortem. */	
	private String doctorName = "(Postmortem Exam not done yet)";
	/** Name of mission at time of death. */
	private String mission;
	/** Phase of mission at time of death. */
	private String missionPhase;
	/** Name of task at time of death. */
	private String task;
	/** Phase of task at time of death. */
	private String taskPhase;
	/** Name of sub task at time of death. */
	private String subTask;
	/** Name of sub task 2 at time of death. */
	private String subTask2;
	/** Phase of sub task at time of death. */
	private String subTaskPhase;
	/** Phase of sub task 2 at time of death. */
	private String subTask2Phase;
	/** Name of the most serious local emergency malfunction. */
	private String malfunction;
	/** The person's last word before departing. */
	private String lastWord = "None";

	/** Medical problem contributing to the death. */
	private HealthProblem problem;
	/** Container unit at time of death. */
	private Unit containerUnit;
	/** Coordinate at time of death. */
	private Coordinates locationOfDeath;
	/** The person's job at time of death. */
	private JobType job;
	/** The person. */
	private Person person;
	/** The robot. */
	private Robot robot;
	
	/** Medical cause of death. */
	private ComplaintType illness;
	/** Person's role type. */
	private RoleType roleType;

	/**
	 * The construct creates an instance of a DeathInfo class.
	 * 
	 * @param person the dead person
	 */
	public DeathInfo(Person person, HealthProblem problem, String cause, String lastWord,
							MarsTime martianTime) {
		this.person = person;
		this.problem = problem;
		this.causeOfDeath = cause;

		// Initialize data members
		if (lastWord == null) {
			int rand = RandomUtil.getRandomInt(LAST_WORDS.length);
			this.lastWord = LAST_WORDS[rand];
		}
		else {
			this.lastWord = lastWord;
		}
		
		timeOfDeath = martianTime;	

		if (problem == null) {
			Complaint serious = person.getPhysicalCondition().getMostSerious();
			if (serious != null) {
				this.illness = serious.getType();
				healthCondition = 0;
				cause = "Non-Illness Related";
			}
		} else {
			this.illness = problem.getIllness().getType();
			healthCondition = problem.getHealthRating();
		}

		// Record the place of death
		if (person.isInVehicle()) {
			// such as died inside a vehicle
			containerUnit = person.getContainerUnit();
			placeOfDeath = person.getVehicle().getName();
		}

		else if (person.isOutside()) {
			placeOfDeath = person.getCoordinates().toString();// "An known location on Mars";
		}

		else if (person.isInSettlement()) {
			placeOfDeath = person.getSettlement().getName();
			// It's eligible for retrieval
			bodyRetrieved = true;
		}

		else if (person.isBuried()) {
			placeOfDeath = person.getBuriedSettlement().getName();
		}

		else {
			placeOfDeath = "Unspecified Location";
		}

		locationOfDeath = person.getCoordinates();

		logger.log(Level.WARNING, person + " passed away in " + placeOfDeath);

		Mind mind = person.getMind();
		
		job = mind.getJob();

		TaskManager taskMgr = mind.getTaskManager();

		task = taskMgr.getTaskName();
		if (task == null || task.equals(""))
			task = taskMgr.getLastTaskName();

		taskPhase = taskMgr.getTaskDescription(false);
		if (taskPhase.equals(""))
			taskPhase = taskMgr.getLastTaskDescription();

		subTask = taskMgr.getSubTaskName();

		subTaskPhase = taskMgr.getSubTaskDescription();
		
		subTask2 = taskMgr.getSubTask2Name();

		subTask2Phase = taskMgr.getSubTask1Description();

		Mission mm = mind.getMission();
		if (mm != null) {
			mission = mm.getName();
			missionPhase = mm.getPhaseDescription();
		}
	}

	public DeathInfo(Robot robot, MarsTime martianTime) {
		// Initialize data members
		this.robot = robot;
		
		timeOfDeath = martianTime;

		TaskManager taskMgr = robot.getBotMind().getBotTaskManager();
		if (taskMgr.hasTask()) {

			if (task == null)
				task = taskMgr.getTaskName();

			if (taskPhase == null) {
				TaskPhase phase = taskMgr.getPhase();
				if (phase != null) {
					taskPhase = phase.getName();
				}
			}
		}

		MalfunctionManager malfunctionMgr = robot.getMalfunctionManager();
		if (malfunctionMgr.hasMalfunction()) {
			Malfunction m = malfunctionMgr.getMostSeriousMalfunction();
			malfunction = m.getName();
		}
	}

	/**
	 * Get the time of death.
	 * 
	 * @return formatted time.
	 */
	public MarsTime getTimeOfDeath() {
		return timeOfDeath;
	}

	/**
	 * Gets the place the death happened. Either the name of the unit the person was
	 * in, or 'outside' if the person died on an EVA.
	 * 
	 * @return place of death.
	 */
	public String getPlaceOfDeath() {
		if (placeOfDeath != null)
			return placeOfDeath;
		else
			return "";
	}

	/**
	 * Gets the container unit at the time of death. Returns null if none.
	 * 
	 * @return container unit
	 */
	public Unit getContainerUnit() {
		return containerUnit;
	}

	/**
	 * Backs up the container unit.
	 * 
	 * @param c
	 */
	public void backupContainerUnit(Unit c) {
		containerUnit = c;
	}
	
	/**
	 * Get the type of the illness that caused the death.
	 * 
	 * @return type of the illness.
	 */
	public ComplaintType getIllness() {
		if (illness != null)
			return illness;
		else
			return null;// "";
	}

	/**
	 * Gets the location of death.
	 * 
	 * @return coordinates
	 */
	public Coordinates getLocationOfDeath() {
		return locationOfDeath;
	}

	/**
	 * Gets the person's job at the time of death.
	 * 
	 * @return job
	 */
	public JobType getJob() {
		return job;
	}

	/**
	 * Gets the mission the person was on at time of death.
	 * 
	 * @return mission name
	 */
	public String getMission() {
		if (mission != null)
			return mission;
		else
			return "   --";
	}

	/**
	 * Gets the mission phase at time of death.
	 * 
	 * @return mission phase
	 */
	public String getMissionPhase() {
		if (missionPhase != null)
			return missionPhase;
		else
			return "   --";
	}

	/**
	 * Gets the task the person was doing at time of death.
	 * 
	 * @return task name
	 */
	public String getTask() {
		if (task != null)
			return task;
		else
			return "   --";
	}

	/**
	 * Gets the sub task the person was doing at time of death.
	 * 
	 * @return sub task name
	 */
	public String getSubTask() {
		if (subTask != null)
			return subTask;
		else
			return "   --";
	}
	
	/**
	 * Gets the sub task 2 the person was doing at time of death.
	 * 
	 * @return sub task 2 name
	 */
	public String getSubTask2() {
		if (subTask2 != null)
			return subTask2;
		else
			return "   --";
	}
	
	/**
	 * Gets the task phase at time of death.
	 * 
	 * @return task phase
	 */
	public String getTaskPhase() {
		if (taskPhase != null)
			return taskPhase;
		else
			return "   --";
	}

	/**
	 * Gets the sub task phase at time of death.
	 * 
	 * @return sub task phase
	 */
	public String getSubTaskPhase() {
		if (subTaskPhase != null)
			return subTaskPhase;
		else
			return "   --";
	}
	

	/**
	 * Gets the sub task 2 phase at time of death.
	 * 
	 * @return sub task 2 phase
	 */
	public String getSubTask2Phase() {
		if (subTask2Phase != null)
			return subTask2Phase;
		else
			return "   --";
	}
	
	/**
	 * Gets the most serious emergency malfunction local to the person at time of
	 * death.
	 * 
	 * @return malfunction name
	 */
	public String getMalfunction() {
		if (malfunction != null)
			return malfunction;
		else
			return "   --";
	}

	public void setBodyRetrieved(boolean b) {
		bodyRetrieved = b;
	}

	public boolean getBodyRetrieved() {
		return bodyRetrieved;
	}

	public void setExamDone(boolean value) {
		examDone = value;
	}
	
	public boolean getExamDone() {
		return examDone;
	}
	
	public HealthProblem getProblem() {
		return problem;
	}
	
	/**
	 * Gets the cause of death.
	 * 
	 * @return
	 */
	public String getCause() {
		return causeOfDeath;
	}
	
	/**
	 * Gets the cause of death.
	 * 
	 * @return
	 */
	public void setCause(String cause) {
		causeOfDeath = cause;
	}
	
	/**
	 * Gets the doctor's name.
	 * 
	 * @return
	 */
	public String getDoctor() {
		return doctorName;
	}
	
	/**
	 * Gets the person. 
	 */
	public Person getPerson() {
		return person;
	}
	
	/**
	 * Gets the robot. 
	 */
	public Robot getRobot() {
		return robot;
	}
	
	public double getTimeExam() {
		return timeSpentExam;
	}

	public void addTimeExam(double time) {
		timeSpentExam += time;
	}
	
	public double getEstTimeExam() {
		return estTotExamTime;
	}
	
	public void setEstTimeExam(double time) {
		estTotExamTime = time;
	}
	
	public double getHealth() {
		return healthCondition;
	}
	
	public void setLastWord(String s) {
		lastWord = s;
	}

	public String getLastWord() {
		return lastWord;
	}
	
	public RoleType getRoleType() {
		return roleType;
	}
	
	public void setRoleType(RoleType type) {
		roleType = type;
	}
}
