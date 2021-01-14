package org.mars_sim.msp.core.time;

import org.mars_sim.msp.core.Simulation;

public class ClockPulse {
	/**
	 * The real time passed since last pulse
	 */
	private double time;
	
	/**
	 * Updated Mars time for this simulation
	 */
	private MarsClock marsTime;

	/**
	 * Master clock
	 */
	private MasterClock master;
	
	/**
	 * Has this pulse crossed into a new Sol
	 */
	private boolean newSol;

	/**
	 * Time back on Earth
	 */
	private EarthClock earthTime;

	/**
	 * Pulse id
	 */
	private long id;

	private Simulation context;

	/**
	 * Create a pulse defining a step forward in the simulation.
	 * @param sim Context of the simulation being advanced
	 * @param id Unique pulse ID. Sequential.
	 * @param elapsed This must be a finate & positive number.
	 * @param marsTime
	 * @param earthTime
	 * @param master
	 * @param newSol Has a new Mars day started with this pulse?
	 */
	ClockPulse(Simulation sim, long id, double elapsed, MarsClock marsTime, EarthClock earthTime, MasterClock master, boolean newSol) {
		super();
		
		if ((elapsed <= 0) || !Double.isFinite(elapsed)) {
			throw new IllegalArgumentException("Elapsed time must be positive : " + elapsed);
		}
		
		this.context = sim;
		this.id = id;
		this.time = elapsed;
		this.marsTime = marsTime;
		this.earthTime = earthTime;
		this.master = master;
		this.newSol = newSol;
	}

	public long getId() {
		return id;
	}
	
	public double getElapsed() {
		return time;
	}

	public MarsClock getMarsTime() {
		return marsTime;
	}

	public MasterClock getMasterClock() {
		return master;
	}
	
	public boolean isNewSol() {
		return newSol;
	}

	public EarthClock getEarthTime() {
		return earthTime;
	}

	public Simulation getContext() {
		return context;
	}
	
}
