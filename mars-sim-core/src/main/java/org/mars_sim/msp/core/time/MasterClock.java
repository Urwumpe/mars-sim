/**
 * Mars Simulation Project
 * MasterClock.java
 * @version 3.1.2 2020-09-02
 * @author Scott Davis
 */

package org.mars_sim.msp.core.time;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.mars_sim.msp.core.LogConsolidated;
import org.mars_sim.msp.core.Simulation;
import org.mars_sim.msp.core.Simulation.SaveType;
import org.mars_sim.msp.core.SimulationConfig;

/**
 * The MasterClock represents the simulated time clock on virtual Mars and
 * delivers a clock pulse for each frame.
 */
public class MasterClock implements Serializable {

	/** default serial id. */
	static final long serialVersionUID = 1L;

	/** Initialized logger. */
	private static Logger logger = Logger.getLogger(MasterClock.class.getName());
	private static String loggerName = logger.getName();
	private static String sourceName = loggerName.substring(loggerName.lastIndexOf(".") + 1, loggerName.length());
	
	private static final int FACTOR = 4;
	public static final int MAX_SPEED = 10;
	
	
	/** The number of milliseconds for each millisols.  */	
	private static final double MILLISECONDS_PER_MILLISOL = MarsClock.SECONDS_PER_MILLISOL * 1000.0;

	// Maximum number of pulses in the log
	private static final int MAX_PULSE_LOG = 10;

	// What is a reasonable jump in the observed real time
	// Allow for long simulation steps. 15 seconds
	// Note if debugging this triggers but the next pulse will reactivate
	private static final long MAX_ELAPSED = 30000;
	
	// Data members
	/** Runnable flag. */
	private transient volatile boolean keepRunning = false;
	/** Pausing clock. */
	private transient volatile boolean isPaused = false;
	/** Flag for ending the simulation program. */
	private transient volatile boolean exitProgram;
	/** Flag for getting ready for autosaving. */
//	private transient volatile boolean autosave;
	/** Mode for saving a simulation. */
	private transient volatile SaveType saveType = SaveType.NONE;
	
	/** Simulation time ratio. */
	private volatile double targetTR = 0;
	
	private volatile int actualTR = 0;

	/** Adjusted time between updates in seconds. */
	private volatile double baseTBU_s = 0;

	/** The time taken to execute one frame in the game loop */
	private volatile long executionTime;	
	/** The last uptime in terms of number of pulses. */
	private transient long tLast;
	/** The cache for accumulating millisols up to a limit before sending out a clock pulse. */
	private transient double timeCache;

	/** The counts for ui pulses. */	
	private transient int count;

	// Records the real milli time when a pulse is execited
	private long[] pulseLog = new long[MAX_PULSE_LOG];
	
	/** Is FXGL is in use. */
	public boolean isFXGL = false;
	/** Is pausing millisol in use. */
	public boolean canPauseTime = false;
	private double pausingMillisols;
	
	/** Next Clock Pulse ID. Start on 1 as all Unit are primed as 0 for the last **/
	private long nextPulseId = 1;
	
	/** Mode for saving a simulation. */
	private double tpfCache = 0;

	/** The file to save or load the simulation. */
	private transient volatile File file;
	/** The thread for running the clock listeners. */
	private transient ExecutorService clockExecutor;
	
	/** A list of clock listeners. */
	private transient List<ClockListener> clockListeners;
	/** A list of clock listener tasks. */
	private transient List<ClockListenerTask> clockListenerTasks;
	
	/** The Martian Clock. */
	private MarsClock marsClock;
	/** A copy of the initial martian clock at the start of the sim. */
	private MarsClock initialMarsTime;
	/** The Earth Clock. */
	private EarthClock earthClock;
	/** The Uptime Timer. */
	private UpTimer uptimer;
	/** The thread for running the game loop. */
	private ClockThreadTask clockThreadTask;
	/** Sol day on the last fireEvent */
	private int lastSol = -1;
	// Number of MilliSols covered in the last pulse
	private double marsMSol;
	// Duration of last sleep
	public long sleepTime;

	private double minMilliSolPerPulse;

	private double maxMilliSolPerPulse;

	private double accuracyBias;

	private int maxMilliSecPerPulse;



	private static Simulation sim = Simulation.instance();

	/**
	 * Constructor
	 * 
	 * @param isFXGL        true if FXGL is used for generating clock pulse
	 * @param userTimeRatio the time ratio defined by user
	 * @throws Exception if clock could not be constructed.
	 */
	public MasterClock(boolean isFXGL, int userTimeRatio) {
		this.isFXGL = isFXGL;
		// logger.config("MasterClock's constructor is on " + Thread.currentThread().getName() + " Thread");
		
		// Gets an instance of the SimulationConfig singleton 
		SimulationConfig simulationConfig = SimulationConfig.instance();

		// Create a martian clock
		marsClock = new MarsClock(simulationConfig.getMarsStartDateTime());
		// Save a copy of the initial mars time
		initialMarsTime = (MarsClock) marsClock.clone();
		

		// Create an Earth clock
		earthClock = new EarthClock(simulationConfig.getEarthStartDateTime());

		// Create an Uptime Timer
		uptimer = new UpTimer();

		// Create listener list.
		clockListeners = new CopyOnWriteArrayList<ClockListener>();
		
		// Calculate elapsedLast
		timestampPulseStart();

		// Check if FXGL is used
		if (!isFXGL)
			clockThreadTask = new ClockThreadTask();

		logger.config("-----------------------------------------------------");
		minMilliSolPerPulse = simulationConfig.getMinSimulatedPulse(); 
		maxMilliSolPerPulse = simulationConfig.getMaxSimulatedPulse(); 
		accuracyBias = simulationConfig.getAccuracyBias(); 
		maxMilliSecPerPulse = simulationConfig.getDefaultPulsePeriod(); 
		targetTR = simulationConfig.getTimeRatio(); 
		
		// Safety check
		if (minMilliSolPerPulse > maxMilliSolPerPulse) {
			logger.severe("The min pulse msols is higher than the max.");
			throw new IllegalStateException("The min MilliSol per pulse can not be higher than the max");
		}
		
		logger.config("         User Defined Time Ratio : " + targetTR + "x");
		logger.config("       Min MSol per pulse (mSol) : " + minMilliSolPerPulse);
		logger.config("       Max MSol per pulse (mSol) : " + maxMilliSolPerPulse);
		logger.config(" Max Elapsed time between pulses : " + maxMilliSecPerPulse + "ms");
		logger.config("                   Accuracy Bias : " + accuracyBias);

		logger.config("-----------------------------------------------------");
	}
	
	/**
	 * Returns the Martian clock
	 *
	 * @return Martian clock instance
	 */
	public MarsClock getMarsClock() {
		return marsClock;
	}

	/**
	 * Gets the initial Mars time at the start of the simulation.
	 *
	 * @return initial Mars time.
	 */
	public MarsClock getInitialMarsTime() {
		return initialMarsTime;
	}

	/**
	 * Returns the Earth clock
	 *
	 * @return Earth clock instance
	 */
	public EarthClock getEarthClock() {
		return earthClock;
	}

	/**
	 * Returns uptime timer
	 *
	 * @return uptimer instance
	 */
	public UpTimer getUpTimer() {
		return uptimer;
	}

	/**
	 * Adds a clock listener
	 * 
	 * @param newListener the listener to add.
	 */
	public final void addClockListener(ClockListener newListener) {
		// if listeners list does not exist, create one
		if (clockListeners == null)
			clockListeners = Collections.synchronizedList(new CopyOnWriteArrayList<ClockListener>());
		// if the listeners list does not contain newListener, add it to the list
		if (!clockListeners.contains(newListener))
			clockListeners.add(newListener);
		// will check if clockListenerTaskList already contain the newListener's task,
		// if it doesn't, create one
		addClockListenerTask(newListener);
	}

	/**
	 * Removes a clock listener
	 * 
	 * @param oldListener the listener to remove.
	 */
	public final void removeClockListener(ClockListener oldListener) {
//		if (clockListeners == null)
//			clockListeners = Collections.synchronizedList(new CopyOnWriteArrayList<ClockListener>());
		if (clockListeners != null && clockListeners.contains(oldListener))
			clockListeners.remove(oldListener);
//		 logger.config("just called clockListeners.remove(oldListener)");
		// Check if clockListenerTaskList contain the newListener's task, if it does,
		// delete it
		ClockListenerTask task = retrieveClockListenerTask(oldListener);
//		 logger.config("just get task");
		if (task != null)
			clockListenerTasks.remove(task);
	}

	/**
	 * Adds a clock listener task
	 *
	 * @param newListener the clock listener task to add.
	 */
	public void addClockListenerTask(ClockListener listener) {
		boolean hasIt = false;
		if (clockListenerTasks == null)
			clockListenerTasks = new CopyOnWriteArrayList<ClockListenerTask>();
		Iterator<ClockListenerTask> i = clockListenerTasks.iterator();
		while (i.hasNext()) {
			ClockListenerTask c = i.next();
			if (c.getClockListener().equals(listener))
				hasIt = true;
		}
		if (!hasIt) {
			ClockListenerTask clt = new ClockListenerTask(listener);
			clockListenerTasks.add(clt);
//			logger.config(clt.getClockListener().getClass().getSimpleName() + "'s clock listener added.");
		}
	}

	/**
	 * Retrieve the clock listener task instance, given its clock listener
	 * 
	 * @param listener the clock listener
	 */
	public ClockListenerTask retrieveClockListenerTask(ClockListener listener) {	
		if (clockListenerTasks != null) {
			Iterator<ClockListenerTask> i = clockListenerTasks.iterator();
			while (i.hasNext()) {
				ClockListenerTask c = i.next();
				if (c.getClockListener().equals(listener))
					return c;
			}
		}
		return null;
	}

	/**
	 * Sets the load simulation flag and the file to load from.
	 *
	 * @param file the file to load from.
	 */
	public void loadSimulation(File file) {
		this.setPaused(false, false);
		this.file = file;
	}

	/**
	 * Sets the save simulation flag and the file to save to.
	 * 
	 * @param file save to file or null if default file.
	 */
	public void setSaveSim(SaveType type, File file) {
		saveType = type;
		this.file = file;
//		logger.config("setSaveSim(" + type + ", " + file + ");  saveType is " + saveType);
	}

//	/**
//	 * Sets the value of autosave
//	 * 
//	 * @param value
//	 */
//	public void setAutosave(boolean value) {
//		autosave = value;
//	}

//	/**
//	 * Gets the value of autosave
//	 * 
//	 * @return autosave
//	 */
//	public boolean getAutosave() {
//		return autosave;
//	}

	/**
	 * Checks if in the process of saving a simulation.
	 * 
	 * @return true if saving simulation.
	 */
	public boolean isSavingSimulation() {
		if (saveType == SaveType.NONE)
			return false;
		else
			return true;
	}

	public void setSaveType() {
		saveType = SaveType.NONE;
	}
	
	/**
	 * Sets the exit program flag.
	 */
	public void exitProgram() {
		this.setPaused(true, false);
		exitProgram = true;
	}

	/*
	 * Gets the total number of pulses since the start of the sim
	 */
	public long getTotalPulses() {
		return nextPulseId;
	}

	/**
	 * Resets the clock listener thread
	 */
	public void resetClockListeners() {
		// If the clockListenerExecutor is not working, need to restart it
//		LogConsolidated.log(Level.CONFIG, 0, sourceName, "The Clock Thread has died. Restarting...");
		
		// Re-instantiate clockListenerExecutor
		clockExecutor = Executors.newSingleThreadExecutor();
		// Re-instantiate clockListeners
		clockListeners = Collections.synchronizedList(new CopyOnWriteArrayList<ClockListener>());

		setupClockListenerTask();
			
		addClockListenerTask(sim);

//		sim.restartClockExecutor();
	}
	
	
	/**
	 * Sets the simulation time ratio and adjust the value of time between update
	 * (TBU)
	 * 
	 * @param ratio
	 */
	public void setTimeRatio(int ratio) {
		if (ratio >= 0D && ratio <= Math.pow(2, MAX_SPEED) && targetTR != ratio) {

			logger.config("Time-ratio : " + targetTR + "x -> " + (int)ratio + "x");
				
			targetTR = ratio;
		}
	}

	/**
	 * Gets the simulation time ratio.
	 * 
	 * @return ratio
	 */
	public double getTimeRatio() {
		return targetTR;
	}
	
	/**
	 * Returns the instance of ClockThreadTask
	 * 
	 * @return ClockThreadTask
	 */
	public ClockThreadTask getClockThreadTask() {
		return clockThreadTask;
	}

	/**
	 * Runs master clock's thread using ThreadPoolExecutor
	 */
	class ClockThreadTask implements Runnable, Serializable {

		private static final long serialVersionUID = 1L;

		private ClockThreadTask() {
		}

		@Override
		public void run() {
			
			// Keep running until told not to by calling stop()
			keepRunning = true;

			if (sim.isDoneInitializing() && !isFXGL) {
				while (keepRunning) {
					long startTime = System.currentTimeMillis();
					
					// Call addTime() to increment time in EarthClock and MarsClock
					if (addTime()) {
						// If a can was applied then potentially adjust the sleep
						executionTime = System.currentTimeMillis() - startTime;
					
						calculateSleepTime();
					}
					else {
						// If on pause or acceptablePulse is false
//						logger.info("AddTime not accepted: lastPulse " + tLast);
						sleepTime = maxMilliSecPerPulse;
					}
					
					// If still going then wait
					if (keepRunning) {
						if (sleepTime > MAX_ELAPSED) {
							// This should not happen
							logger.warning("Sleep too long: clipped to " + maxMilliSecPerPulse);
							sleepTime = maxMilliSecPerPulse;
						}
						if (sleepTime > 0) {
							// Pause simulation to allow other threads to complete.
							try {
								Thread.sleep(sleepTime);;
							} catch (InterruptedException e) {
								Thread.currentThread().interrupt();
							}
						}
					}

					// Exit program if exitProgram flag is true.
					if (exitProgram) {
						AutosaveScheduler.cancel();
						System.exit(0);
					}
					
					// Check to see if the simulation should be saved at this point.
					checkSave();
					
				} // end of while
			} // if fxgl is not used
		} // end of run

		private void calculateSleepTime() {
			// Max number of pulses this environment can handle
			double predictedMaxPulses = (double)maxMilliSecPerPulse/executionTime;
			
			// The Desired simulation period 
			double desiredMSol = (maxMilliSecPerPulse * targetTR) / MILLISECONDS_PER_MILLISOL;
			
			// Most accurate simulation is with the pulse duration; will be highest rate
			double mostAccurateRate = desiredMSol/minMilliSolPerPulse;
			
			// Least accurate is with the largest pulse duration; will be lower rate
			double leastAccurateRate = desiredMSol/maxMilliSolPerPulse;
			
			// Lowest pulse rate can not be less than 1
			double lowestPulseRate = Math.max(leastAccurateRate, 1D);
			
			// Highest pulse rate can not be higher than predicted max
			double highestPulseRate = Math.min(mostAccurateRate, predictedMaxPulses);
			
			// Desired rate is between the low & high and use the accurancy to bias between the 2 limits
			double newRate = lowestPulseRate + ((highestPulseRate - lowestPulseRate) * accuracyBias);
			
			// Sleep time allows for the execution time
			sleepTime = (long)(maxMilliSecPerPulse/newRate) - executionTime;
			
			// What has happened?
//			String msg = String.format("Sleep calcs d=%.2f msol, p=%.3f, l=%.3f, m=%.3f, r=%.3f, s=%d ms, e=%d ms",
//				    desiredMSol, predictedMaxPulses, leastAccurateRate, mostAccurateRate, newRate, sleepTime,
//				    executionTime);
//		    logger.info(msg);
		}
	}

	public void setCommandPause(boolean value0, double value1) {
		// Check GameManager.mode == GameMode.COMMAND ?
		canPauseTime = value0;
		pausingMillisols = value1;
	}
	
	/*
	 * Add earth time and mars time.
	 * @return Was teh pulse accepted?
	 */
	private boolean addTime() {
		boolean acceptablePulse = false;
		
		if (!isPaused) {	
			// Find the new up time
			long tnow = System.currentTimeMillis();
		
			// Calculate the elapsed time in milli-seconds
			long realElaspedMilliSec = tnow - tLast;
			
			// Make sure there is not a big jump; suggest power save so skip it
			if (realElaspedMilliSec > MAX_ELAPSED) {
				// Reset the elapsed clock to ignore this pulse
				logger.warning("Elapsed real time " + realElaspedMilliSec + "ms is longer than max "
			                   + MAX_ELAPSED + "ms; maybe power event?");
				timestampPulseStart();
			}
			else {
				// Get the time pulse length in millisols.
				marsMSol = (realElaspedMilliSec * targetTR) / MILLISECONDS_PER_MILLISOL; 
	
				// Pulse must be less than the max and positive
				if (marsMSol > 0) {
					acceptablePulse = true;
					if (marsMSol > maxMilliSecPerPulse) {
						logger.warning("Proposed pulse " + marsMSol + " clipped to max " + maxMilliSecPerPulse);
						marsMSol = maxMilliSecPerPulse;
					}
					else if (marsMSol < minMilliSolPerPulse) {
						logger.warning("Proposed pulse " + marsMSol + " increased to min " + minMilliSolPerPulse);
						marsMSol = minMilliSolPerPulse;			
					}
				}
			}

			// Can we do something ?
			if (acceptablePulse && keepRunning) {
				// Elapsed time is acceptable
				// The time elapsed for the EarthClock aligned to adjusted Mars time
				long earthMillisec = (long)(marsMSol * MILLISECONDS_PER_MILLISOL);
				
				// Calculate the actual rate for feedback
				actualTR = (int) (earthMillisec / realElaspedMilliSec);
				
				if (!clockExecutor.isTerminated()
					&& !clockExecutor.isShutdown()) {	
					// Do the pulse
					timestampPulseStart();
					
					uptimer.updateTime(realElaspedMilliSec);
					
					// Add time to the Earth clock.
					earthClock.addTime(earthMillisec);
					
					// Add time pulse to Mars clock.
					marsClock.addTime(marsMSol);
					
					// Run the clock listener tasks that are in other package
					fireClockPulse(marsMSol);
				}
				else {
					// NOTE: when resuming from power saving, timePulse becomes zero
					LogConsolidated.flog(Level.CONFIG, 0, sourceName, "The clockListenerExecutor has died. Restarting...");
					resetClockListeners();
				}
			}
		}
		return acceptablePulse;
	}
		   
	/**
	 * Checks if it is on pause or a saving process has been requested. Keeps track
	 * of the time pulse
	 * 
	 * @return true if it's saving
	 */
	private boolean checkSave() {

		if (saveType != SaveType.NONE) {
			try {
				sim.saveSimulation(saveType, file);
			} catch (NullPointerException e) {
				logger.log(Level.SEVERE,
						"NullPointerException. Could not save the simulation.", e);// as " + (file == null ? "null" : file.getPath()), e);
				e.printStackTrace();	
			} catch (IOException e) {
				logger.log(Level.SEVERE,
						"IOException. Could not save the simulation.", e);//  as " + (file == null ? "null" : file.getPath()), e);
				e.printStackTrace();

			} catch (Exception e) {
				logger.log(Level.SEVERE,
						"Exception. Could not save the simulation.", e);//  as " + (file == null ? "null" : file.getPath()), e1);
				e.printStackTrace();
			}
			
			// Reset saveType back to zero
			saveType = SaveType.NONE;

			return true;
		}

		else
			return false;
	}

	/**
	 * Looks at the clock listener list and checks if each listener has already had
	 * a corresponding task in the clock listener task list.
	 */
	public void setupClockListenerTask() {
		clockListeners.forEach(t -> {
			// Check if it has a corresponding task or not, 
			// if it doesn't, create a task for t
			addClockListenerTask(t);
		});
	}


	/**
	 * Prepares clock listener tasks for setting up threads.
	 */
	public class ClockListenerTask implements Callable<String>{

		private ClockPulse currentPulse;
		private ClockListener listener;

		public ClockListener getClockListener() {
			return listener;
		}

		private ClockListenerTask(ClockListener listener) {
			this.listener = listener;
		}

		public void setCurrentPulse(ClockPulse pulse) {
			this.currentPulse = pulse;
		}
		
		@Override
		//public void run() {
		public String call() throws Exception {
			if (sim.isDoneInitializing()) {
				try {
					// The most important job for CLockListener is to send a clock pulse to listener
				
					// gets updated.
					listener.clockPulse(currentPulse);
					timeCache += currentPulse.getElapsed();
					count++;
	
					if (count > FACTOR) {
						count = 0;
						
						// Note: on a typical PC, approximately one ui pulse is sent out each second
						listener.uiPulse(timeCache);
						timeCache = 0;
	
					}
	
				} catch (ConcurrentModificationException e) {
					e.printStackTrace();
				}
			}
			return "done";
		}
	}

	   /**
     * Gets the simulation speed
     * 
     * @return
     */
    public int getActualRatio() {
    	return actualTR;
    }
    
	/**
	 * Fires the clock pulse to each clock listener
	 * 
	 * @param time
	 */
	public void fireClockPulse(double time) {

		// Identify if it's a new Sol
		int currentSol = marsClock.getMissionSol();
		boolean isNewSol = ((lastSol >= 0) && (lastSol != currentSol));
		lastSol  = currentSol;
		
		// Log the pulse
		long newPulseId = nextPulseId++;
		int logIndex = (int)(newPulseId % MAX_PULSE_LOG);
		pulseLog[logIndex] = System.currentTimeMillis();
		
		ClockPulse pulse = new ClockPulse(sim, newPulseId, time, marsClock, earthClock, this, isNewSol);
		try {
			clockListenerTasks.forEach(s -> {
				s.setCurrentPulse(pulse);
				Future<String> result = clockExecutor.submit(s);
				// Wait for it to complete so the listeners doesn't get queued up if the MasterClock races ahead
				try {
					result.get();
				} catch (ExecutionException e) {
					logger.log(Level.SEVERE, "Problem in clock listener", e);
					e.printStackTrace();
				} catch (InterruptedException e) {
					// Program closing down
					Thread.currentThread().interrupt();
				}
			});
		} catch (RejectedExecutionException ree) {
			// Executor is shutdown and cannot complete queued tasks
		}
	}

	/**
	 * Stop the clock
	 */
	public void stop() {
		keepRunning = false;
	}

	/**
	 * Restarts the clock
	 */
	public void restart() {
		keepRunning = true;
		timestampPulseStart();
	}

	/**
	 * Timestamps the last pulse, used to calculate elapsed pulse time
	 */
	private void timestampPulseStart() {
		tLast = System.currentTimeMillis();
	}

	/**
	 * Starts the clock
	 */
	public void start() {
		keepRunning = true;
		timestampPulseStart();
	}

	
	/**
	 * Set if the simulation is paused or not.
	 *
	 * @param value the state to be set.
	 * @param showPane true if the pane should be shown.
	 */
	public void setPaused(boolean value, boolean showPane) {
		if (this.isPaused != value) {
			this.isPaused = value;
	
			if (value) {
				AutosaveScheduler.cancel();	
				actualTR = 0; // Clear the actual rate
			}
			else {
				AutosaveScheduler.start();
				
				// Reset the last pulse time
				timestampPulseStart();
			}
			
			// Fire pause change to all clock listeners.
			firePauseChange(value, showPane);
		}
	}

	/**
	 * Checks if the simulation is paused or not.
	 *
	 * @return true if paused.
	 */
	public boolean isPaused() {
		return isPaused;
	}

	/**
	 * Send a pulse change event to all clock listeners.
	 * 
	 * @param isPaused
	 * @param showPane
	 */
	public void firePauseChange(boolean isPaused, boolean showPane) {

		clockListeners.forEach(cl -> cl.pauseChange(isPaused, showPane));		 
	}

	/**
	 * Starts clock listener thread pool executor
	 */
	public void startClockListenerExecutor() {
		if (clockExecutor == null)
			clockExecutor = Executors.newSingleThreadExecutor(); //(ThreadPoolExecutor) Executors.newFixedThreadPool(1);
	}

	/**
	 * Shuts down clock listener thread pool executor
	 */
	public void endClockListenerExecutor() {
		if (clockExecutor != null)
			clockExecutor.shutdownNow();
	}


	/**
	 * Gets the clock listener executor. To be called by TransportWizard and ConstructionWizard
	 * 
	 * @return
	 */
	public ExecutorService getClockListenerExecutor() {
		return clockExecutor;
	}


	/**
	 * Gets the Frame per second
	 * 
	 * @return
	 */
	public double getFPS() {
		// How to check xFGL version ?
		return 0;
	}

	/**
	 * Sends out a clock pulse if using FXGL
	 * 
	 * @param tpf
	 */
	public void onUpdate(double tpf) {
		if (!isPaused) {
			tpfCache += tpf;
			if (tpfCache >= baseTBU_s) {
				
				addTime();
				
				// Set tpfCache back to zero
				tpfCache = 0;
			}

			checkSave();

			// Exit program if exitProgram flag is true.
			if (exitProgram) {
				AutosaveScheduler.cancel();
				System.exit(0);
			}
		}
	}

	/**
	 * Gets the sleep time in milliseconds
	 * 
	 * @return
	 */
	public long getSleepTime() {
		return sleepTime;
	}
	
	/**
	 * Gets the MilliSol covered in the last pulse
	 * 
	 * @return
	 */
	public double getMarsPulseTime() {
		return marsMSol;
	}
	
	/** 
	 * Gets the time [in microseconds] taken to execute one frame in the game loop 
	 * 
	 * @return
	 */
	public long getExecutionTime() {
		return executionTime;	
	}

	
	/**
	 * Reloads instances after loading from a saved sim
	 * 
	 * @param clock
	 */
	public static void initializeInstances(Simulation s) {
		sim = s;//Simulation.instance();
	}
	
	/**
	 * Prepare object for garbage collection.
	 */
	public void destroy() {
		sim = null;
		marsClock.destroy();
		marsClock = null;
		initialMarsTime = null;
		earthClock.destroy();
		earthClock = null;
		uptimer = null;
		clockThreadTask = null;
		clockExecutor = null;
		file = null;

		clockListeners = null;
		clockExecutor = null;
	}

	/**
	 * How many pulses per second
	 * @return
	 */
	public double getPulsesPerSecond() {
		double ticksPerSecond = 0;
	
		// Make sure enough pulses have passed
		if (nextPulseId >= MAX_PULSE_LOG) {
			// Recent idx will be the previous pulse id but check it is not negative
			int recentIdx = (int)((nextPulseId-1) % MAX_PULSE_LOG);
			recentIdx = (recentIdx < 0 ? (MAX_PULSE_LOG-1) : recentIdx);
			
			// Oldest id will be the next pulse as it will be overwrite on next tick
			int oldestIdx = (int)(nextPulseId % MAX_PULSE_LOG);
			long elaspsedMilli = (pulseLog[recentIdx] - pulseLog[oldestIdx]);
			ticksPerSecond = (MAX_PULSE_LOG * 1000D)/elaspsedMilli;
		}
		
		return ticksPerSecond;
	}
}
