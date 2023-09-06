/*
 * Mars Simulation Project
 * TaskManager.java
 * @date 2022-06-24
 * @author Scott Davis
 */

package org.mars_sim.msp.core.person.ai.task.util;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.mars_sim.msp.core.Simulation;
import org.mars_sim.msp.core.SimulationConfig;
import org.mars_sim.msp.core.SimulationFiles;
import org.mars_sim.msp.core.Unit;
import org.mars_sim.msp.core.UnitEventType;
import org.mars_sim.msp.core.data.History;
import org.mars_sim.msp.core.logging.SimLogger;
import org.mars_sim.msp.core.person.ai.mission.Mission;
import org.mars_sim.msp.core.person.ai.task.Walk;
import org.mars_sim.msp.core.structure.Settlement;
import org.mars_sim.msp.core.structure.building.Building;
import org.mars_sim.msp.core.time.MarsTime;
import org.mars_sim.msp.core.time.MasterClock;
import org.mars_sim.msp.core.vehicle.Vehicle;

/*
 * The TaskManager class keeps track of a Worker's current task and can randomly
 * assign a new task based on a list of possible tasks and the current situation.
 */
public abstract class TaskManager implements Serializable {

	/** default serial id. */
	private static final long serialVersionUID = 1L;
	/** default logger. */
	private static final SimLogger logger = SimLogger.getLogger(TaskManager.class.getName());
	
	private static final String SLEEPING = "Sleeping";
	private static final String EVA = "EVA";
	private static final String AIRLOCK = "Airlock";
	private static final String DIG = "Digging";
	
	/*
	 * This class represents a record of a given activity (task or mission)
	 * undertaken by a person or a robot
	 */
	public final class OneActivity implements Serializable {

		/** default serial id. */
		private static final long serialVersionUID = 1L;

		// Data members
		private String taskName;
		private String missionName;
		private String description;
		private String phase;

		public OneActivity(String taskName, String description, String phase, String missionName) {
			this.taskName = taskName;
			this.description = description;
			this.phase = phase;
		}

		/**
		 * Gets the task name.
		 * 
		 * @return task name
		 */
		public String getTaskName() {
			return taskName;
		}

		/**
		 * Gets the description what the actor is doing.
		 * 
		 * @return description
		 */
		public String getDescription() {
			return description;
		}

		/**
		 * Gets the task phase.
		 * 
		 * @return task phase
		 */
		public String getPhase() {
			return phase;
		}

		public String getMission() {
			return missionName;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((taskName == null) ? 0 : taskName.hashCode());
			result = prime * result + ((phase == null) ? 0 : phase.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			OneActivity other = (OneActivity) obj;
			if (taskName == null) {
				if (other.taskName != null)
					return false;
			} else if (!taskName.equals(other.taskName))
				return false;
			if (missionName == null) {
				if (other.missionName != null)
					return false;
			} else if (!missionName.equals(other.missionName))
				return false;
			if (description == null) {
				if (other.description != null)
					return false;
			} else if (!description.equals(other.description))
				return false;
			if (phase == null) {
				if (other.phase != null)
					return false;
			} else if (!phase.equals(other.phase))
				return false;
			return true;
		}
	}

	/** Number of days to record Tack Activities. */	
	private static MasterClock master;

	private static PrintWriter diagnosticFile = null;

	/**
	 * Enables the detailed diagnostics.
	 * 
	 * @throws FileNotFoundException 
	 */
	public static void setDiagnostics(boolean diagnostics) throws FileNotFoundException {
		if (diagnostics) {
			if (diagnosticFile == null) {
				String filename = SimulationFiles.getLogDir() + "/task-cache.txt";
				diagnosticFile  = new PrintWriter(filename);
				logger.config("Diagnostics enabled to " + filename);
			}
		}
		else if (diagnosticFile != null){
			diagnosticFile.close();
			diagnosticFile = null;
		}
	}

	/**The worker **/
	protected transient Unit worker;
	/** The current task the worker is doing. */
	protected Task currentTask;
	/** The last task the person was doing. */
	private Task lastTask;

	private transient TaskCache taskProbCache = null;


	/** The history of tasks. */
	private History<OneActivity> allActivities;
	/** The list of pending of tasks. */
	private List<TaskJob> pendingTasks;

	// THese are for metric capture
	private static int totalRebuild = 0;
	private static int reuseRebuild = 0;
	private static Map<Settlement,MarsTime> metrics;
	
	/**
	 * Constructor.
	 * 
	 * @param worker
	 */
	protected TaskManager(Unit worker) {
		this.worker = worker;
		allActivities = new History<>(150);   // Equivalent of 3 days
		pendingTasks = new CopyOnWriteArrayList<>();
	}

	/**
	 * Returns true if person has a task (may be inactive).
	 * 
	 * @return true if person has a task
	 */
	public boolean hasTask() {
		return currentTask != null;
	}

	/**
	 * Returns the name of the current task for UI purposes. Returns a blank string
	 * if there is no current task.
	 * 
	 * @return name of the current task
	 */
	public String getTaskName() {
		if (currentTask != null) {
			return currentTask.getName();
		} else {
			return "";
		}
	}

	public String getSubTaskName() {
		if (currentTask != null && currentTask.getSubTask() != null) {
			return currentTask.getSubTask().getName();
		} else {
			return "";
		}
	}

	public String getSubTask2Name() {
		Task task = getRealTask();
		if (task != null) {
			return task.getName();
		} else {
			return "";
		}
	}

	/**
	 * Gets the bottom-most real-time task. 
	 * 
	 * @return
	 */
	public Task getRealTask() {
		if (currentTask == null) {
			return null;
		}
		
		Task subtask1 = currentTask.getSubTask();
		if (subtask1 == null) {
			return currentTask;
		}
		
		if (subtask1.getSubTask() == null) {
			return subtask1;
		}
		
		Task subtask2 = subtask1.getSubTask();
		if (subtask2 == null) {
			return subtask1;
		}
		
		if (subtask2.getSubTask() == null) {
			return subtask2;
		}
		
		return subtask2.getSubTask();
	}

	/**
	 * Returns the name of the current task for UI purposes. Returns a blank string
	 * if there is no current task.
	 * 
	 * @return name of the current task
	 */
	public String getTaskClassName() {
		if (currentTask != null) {
			return currentTask.getTaskSimpleName();
		} else {
			return "";
		}
	}

	/**
	 * Returns a description of current task for UI purposes. Returns a blank string
	 * if there is no current task.
	 * 
	 * @return a description of the current task
	 */
	public String getTaskDescription(boolean subTask) {
		if (currentTask != null) {
			return currentTask.getDescription(subTask);
		}
		return "";
	}

	public String getSubTaskDescription() {
		if (currentTask != null && currentTask.getSubTask() != null) {
			String t = currentTask.getSubTask().getDescription();
			if (t != null && !t.equals(""))
				return t;
			else
				return "";
		} else
			return "";
	}

	public String getSubTask2Description() {
		if (currentTask != null && currentTask.getSubTask() != null
				&& currentTask.getSubTask().getSubTask() != null) {
			String t = currentTask.getSubTask().getSubTask().getDescription();
			if (t != null) // || !t.equals(""))
				return t;
			else
				return "";
		} else
			return "";
	}

	/**
	 * Returns the current task phase if there is one. Returns null if current task
	 * has no phase. Returns null if there is no current task.
	 * 
	 * @return the current task phase
	 */
	public TaskPhase getPhase() {
		if (currentTask != null) {
			return currentTask.getPhase();
		} else {
			return null;
		}
	}

	public TaskPhase getSubTaskPhase() {
		if (currentTask != null && currentTask.getSubTask() != null) {
			return currentTask.getSubTask().getPhase();
		} else {
			return null;
		}
	}

	public TaskPhase getSubTask2Phase() {
		if (currentTask != null && currentTask.getSubTask() != null
				&& currentTask.getSubTask().getSubTask() != null) {
			return currentTask.getSubTask().getSubTask().getPhase();
		} else {
			return null;
		}
	}

	/**
	 * Returns the current task. Return null if there is no current task.
	 * 
	 * @return the current task
	 */
	public Task getTask() {
		return currentTask;
	}

	public String getLastTaskName() {
		return (lastTask != null ? lastTask.getTaskSimpleName() : "");
	}

	public String getLastTaskDescription() {
		return (lastTask != null ? lastTask.getDescription() : "");
	}

	/**
	 * Returns true if person has an active task.
	 * 
	 * @return true if person has an active task
	 */
	public boolean hasActiveTask() {
		return (currentTask != null && !currentTask.isDone());
	}

	/**
	 * Ends all sub tasks.
	 */
	public void endSubTask() {
		if (currentTask != null && currentTask.getSubTask() != null) {
			currentTask.getSubTask().endTask();
		}
	}
	


	/**
	 * Sets the current task to null.
	 * 
	 * @param reason May be used in an override method
	 */
	public void clearAllTasks(String reason) {
		endCurrentTask();
	}

	/**
	 * Ends the current task.
	 */
	public void endCurrentTask() {
		if (currentTask != null) {
			currentTask.endTask();
			currentTask = null;
			worker.fireUnitUpdate(UnitEventType.TASK_EVENT);
		}
	}

	/**
	 * Clears a specific task.
	 * 
	 * @param taskString
	 */
	public void clearSpecificTask(String taskString) {
		
		Task subTask1 = currentTask.getSubTask();
		
		if (currentTask != null && subTask1 != null) {
			
			Task subTask2 = subTask1.getSubTask();
			
			if (subTask2 != null) {
				String taskName2 = subTask2.getClass().getSimpleName();
				if (taskName2.equalsIgnoreCase(taskString)) {
					subTask2.endTask();
				}
			}
			
			else {				
				String taskName1 = subTask1.getClass().getSimpleName();
				if (taskName1.equalsIgnoreCase(taskString)) {
					subTask1.endTask();
				}
			}
		}
		
		else {
			String taskName0 = currentTask.getClass().getSimpleName();
			if (taskName0.equalsIgnoreCase(taskString)) {
				endCurrentTask();
			}
		}
	}


	/**
	 * Calculates and caches the probabilities.
	 * 
	 * This will NOT use the cache but assumes the callers know when a cahce can be used or not used. 
	 */
	protected abstract TaskCache rebuildTaskCache();

	
	/**
	 * Simple method to capture some stats/metrics on cache rebuilds.
	 */
	private void captureStats() {
		Settlement scope = worker.getAssociatedSettlement();
		if (metrics == null) {
			metrics = new HashMap<>();
		}
		synchronized(metrics) {
			MarsTime now = getMarsTime();
			MarsTime lastRebuild = metrics.get(scope);
			totalRebuild++;

			// If time has not changed since last rebuild; count as a reuse
			if ((lastRebuild != null) && lastRebuild.equals(now)) {
				reuseRebuild++;
			}
			else {
				metrics.put(scope, now);
			}

			// Limit output
			if ((totalRebuild % 1000) == 0) {
				String message = String.format("---- Cache Reuse stats %d/%d (%d%%)",
									reuseRebuild, totalRebuild, (100*reuseRebuild/totalRebuild));
				logger.info(message);
			}
		}
	}

	/**
	 * Constructs a new Task of the specified type.
	 * 
	 * @param selectedMetaTask Type of task to create.
	 * @return New Task.
	 */
	protected abstract Task createTask(TaskJob selectedMetaTask);

	/**
	 * Returns the last calculated probability map.
	 * 
	 * @return
	 */
	public TaskCache getLatestTaskProbability() {
		return taskProbCache;
	}
	
	/**
	 * Outputs the cache to a file for diagnostics.
	 * 
	 * @param extras Extra details about Task
	 */
	private void outputCache(TaskCache current) {	
		synchronized (diagnosticFile) {	
			diagnosticFile.println(current.getCreatedOn().getDateTimeStamp());
			diagnosticFile.println("Worker:" + worker.getName());
			diagnosticFile.println(current.getContext());				
			diagnosticFile.println("Total:" + current.getTotal());
			for (TaskJob task : taskProbCache.getTasks()) {
				diagnosticFile.println(task.getDescription() + ":" + task.getScore());
			}
			
			diagnosticFile.println();
			diagnosticFile.flush();
		}
	}

	/**
	 * Records a task onto the schedule.
	 * 
	 * @param changed The active task.
	 * @param mission Associated mission.
	 */
	void recordTask(Task changed, Mission mission) {
		String newDescription = changed.getDescription();
		String newPhase = "";
		if (changed.getPhase() != null)
			newPhase = changed.getPhase().getName();
		
		// If there is no details; then skip it
		if (!newDescription.equals("") && !newPhase.equals("")) {
			String newTask = changed.getName(false);

			recordActivity(newTask, newPhase, newDescription, mission);
		}
	}

	/**
	 * Record an activity on the Task Activity log.
	 */
	public void recordActivity(String newTask, String newPhase, String newDescription, Mission mission) {
		String missionName = (mission != null ? mission.getName() : null);
		
		// This is temp.
		String location = " in";
		if (worker.isInVehicle()) {
			location += " V";
		}
		if (worker.isInSettlement()) {
			location += " S";
		}
		if (worker.isOutside()) {
			location += " O";
		}

		OneActivity newActivity = new OneActivity(
											newTask + location,
											newDescription,
											newPhase, 
											missionName);

		allActivities.add(newActivity);
	}
	
	
	/**
	 * Gets all activities of all days a person.
	 * 
	 * @return all activity schedules
	 */
	public History<OneActivity> getAllActivities() {
		return allActivities;
	}
	
	/**
	 * Checks if the person or robot is walking through a given vehicle.
	 * 
	 * @param vehicle the vehicle.
	 * @return true if walking through vehicle.
	 */
	public boolean isWalkingThroughVehicle(Vehicle vehicle) {
	
	    boolean result = false;
	
	    Task task = currentTask;
	    while ((task != null) && !result) {
	        if (task instanceof Walk walkTask) {
	            if (walkTask.isWalkingThroughVehicle(vehicle)) {
	                result = true;
	            }
	        }
	        task = task.getSubTask();
	    }
	
	    return result;
	}

	/**
	 * Checks if the person or robot is walking through a given building.
	 * 
	 * @param building the building.
	 * @return true if walking through building.
	 */
	public boolean isWalkingThroughBuilding(Building building) {
	
		boolean result = false;
	
		Task task = currentTask;
		while ((task != null) && !result) {
			if (task instanceof Walk walkTask) {
				if (walkTask.isWalkingThroughBuilding(building)) {
					result = true;
				}
			}
			task = task.getSubTask();
		}
	
		return result;
	}
	

	/**
	 * Starts a new task for the worker based on tasks available at their location.
	 * Uses the task probability cache. If a task is found; then it is assigned
	 * to the manager to start working.
	 */
	public void startNewTask() {
		Task selectedTask = null;
		TaskJob selectedJob = null;

		// If cache is not current, calculate the probabilities. If it is a static cache, i.e. no createdOn then
		// ignore the cache
		MarsTime now = getMarsTime();
		if ((taskProbCache == null)  || (taskProbCache.getCreatedOn() == null) || taskProbCache.getTasks().isEmpty()
				|| (now.getMillisol() != taskProbCache.getCreatedOn().getMillisol())) {
			taskProbCache = rebuildTaskCache();
			
			// Comment out to stop capturing stats
			//captureStats();
			
			// Output shift
			if (diagnosticFile != null) {
				outputCache(taskProbCache);
			}
		}

		if (taskProbCache.getTasks().isEmpty()) { 
			// Should never happen since TaskManagers have to return a populated list
			// with doable defaults if needed
			logger.severe(worker, "No normal Tasks available in " + taskProbCache.getContext());
		}
		else {
			selectedJob = taskProbCache.getRandomSelection();

			// Call constructInstance of the selected Meta Task to commence the ai task
			selectedTask = createTask(selectedJob);

			// Start this newly selected task
			replaceTask(selectedTask);
		}
	}

	/**
	 * Checks to see if it's okay to replace a task.
	 * 
	 * @param newTask the task to be executed
	 */
	public boolean checkReplaceTask(Task newTask) {
		
		if (newTask == null) {
			return false;
		}
		
		if (hasActiveTask()) {
			
			String currentDes = currentTask.getDescription();

			if (newTask.getDescription().equalsIgnoreCase(currentDes))
				return false;	
		
			if (isFilteredTask(currentDes))
				return false;
			
			if (newTask.getName().equals(getTaskName())) {
				return false;
			}
		}
		
		// Records current task as last task and replaces it with a new task.
		replaceTask(newTask);
		
		return true;
	}
	

	protected boolean isFilteredTask(String des) {
		if (des.contains(SLEEPING)
				|| des.contains(EVA)
				|| des.contains(AIRLOCK)
				|| des.contains(DIG))
				return true;
		
		return false;
	}
	
	
	/**
	 * Records current task as last task and replaces it with a new task.
	 * 
	 * @param newTask
	 */
	public void replaceTask(Task newTask) {
		if (newTask != null) {
			
			// Backup the current task as last task
			if (currentTask != null)
				lastTask = currentTask;
			
			// Inform that the current task will be terminated
			if (hasActiveTask()) {
				String des = currentTask.getDescription();
				
				currentTask.endTask();
				
				logger.info(worker, 20_000, "Quit '" + des + "' to start the new task of '"
							+ newTask.getName() + "'.");
			}
			
			// Make the new task as the current task
			currentTask = newTask;
			
			// Send out the task event
			worker.fireUnitUpdate(UnitEventType.TASK_EVENT, newTask);
		}
	}
	
	/**
	 * Gets the current mars time.
	 */
	protected static MarsTime getMarsTime() {
		return master.getMarsTime();
	}

	/**
	 * Gets all pending tasks.
	 *
	 * @return
	 */
	public List<TaskJob> getPendingTasks() {
		return pendingTasks;
	}
	
	/**
	 * Adds a pending task if it is not in the pendingTask list yet.
	 *
	 * @param task
	 * @return
	 */
	public boolean addPendingTask(String taskName) {
		return addPendingTask(taskName, false, 0, 0);
	}
	
	/**
	 * Adds a pending task if it is not in the pendingTask list yet.
	 *
	 * @param task				the pending task 
	 * @param allowDuplicate	Can this pending task be repeated in the queue
	 * @param countDownTime 	the count down time for executing the new pending task
	 * @param duration 			the duration of the new task
	 * @return
	 */
	public boolean addPendingTask(String taskName, boolean allowDuplicate, int countDownTime, int duration) {
		
		// Shorten the remaining duration of the current task so as to execute the pending time right after the countDownTime in a timely manner
		if (currentTask != null && countDownTime > 0) {
			double oldDuration = currentTask.getDuration();
			double newDuration = countDownTime + currentTask.getTimeCompleted();
			
			if (newDuration < oldDuration) {
				currentTask.setDuration(newDuration);
				logger.info(worker, "Updating current task '" + currentTask.getName() 
					+ "'  duration: " + Math.round(oldDuration * 10.0)/10.0 + " -> " + Math.round(newDuration * 10.0)/10.0 + ".");
			}
		}
		
		// Potential ClassCast but only temp. measure
		FactoryMetaTask mt = (FactoryMetaTask) MetaTaskUtil.getMetaTask(taskName);
		if (mt == null) {
			logger.warning(worker, "Cannot find pending task '" + taskName + "'.");
			return false;
		}

		BasicTaskJob task = new BasicTaskJob(mt, 0, duration);
		return addPendingTask(task, allowDuplicate);
	}
	
	/**
	 * Adds a pending task if it is not in the pendingTask list yet.
	 *
	 * @param task
	 * @param allowDuplicate
	 * @return
	 */
	public boolean addPendingTask(TaskJob task, boolean allowDuplicate) {
		if (allowDuplicate || !pendingTasks.contains(task)) {
			boolean success = pendingTasks.add(task);
			if (success) 
				logger.info(worker, 20_000L, "Successfully added pending task '" + task.getDescription() + "'.");
			else
				logger.info(worker, 20_000L, "Failed to add pending task '" + task.getDescription() + "'.");
			return success;
		}
		return false;
	}

	/**
	 * Gets the first pending meta task in the queue.
	 * Note: It no longer automatically remove the retrieved task. 
	 * Must call removePendingTask() separately to remove it.
	 *
	 * @return
	 */
	protected TaskJob getPendingTask() {
		if (!pendingTasks.isEmpty()) {
			TaskJob firstTask = pendingTasks.get(0);
			return firstTask;
		}
		return null;
	}

	/**
	 * Removes a pending task in the queue.
	 *
	 * @return
	 */
	public boolean removePendingTask(TaskJob taskJob) {
		boolean success = false;
		if (!pendingTasks.isEmpty() && pendingTasks.contains(taskJob)) {
			success = pendingTasks.remove(taskJob);
			if (success)
				logger.info(worker, "Successfully removed the pending task '" + taskJob.getDescription() + "'.");
			else
				logger.info(worker, "Failed to remove the pending task '" + taskJob.getDescription() + "'.");
		}
		return success;
	}
	
	/**
	 * Checks if the worker is currently performing this task.
	 * 
	 * @param task
	 * @return
	 */
	public boolean hasSameTask(String task) {
		if (getTaskName().equalsIgnoreCase(task))
			return true;
		
		return false;
	}
	
	/**
	 * Reloads instances after loading from a saved sim.
	 * 
	 * @param sim
	 */
	public static void initializeInstances(Simulation sim, SimulationConfig conf) {

		MetaTaskUtil.initialiseInstances(sim);
		Task.initializeInstances(sim, conf.getPersonConfig());
		master = sim.getMasterClock();
	}
	

	/**
	 * Re-initializes instances when loading from a saved sim
	 */
	public void reinit() {
		if (currentTask != null)		
			currentTask.reinit();
		if (lastTask != null)
			lastTask.reinit();
	}
	
	/**
	 * Prepares object for garbage collection.
	 */
	public void destroy() {
		worker = null;
		currentTask = null;
		lastTask = null;
		taskProbCache = null;
		allActivities = null;
		pendingTasks.clear();
		pendingTasks = null;
		metrics.clear();
		metrics = null;
	}
}
