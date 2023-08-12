/*
 * Mars Simulation Project
 * FuelPowerSource.java
 * @date 2023-05-31
 * @author Sebastien Venot
 */
package org.mars_sim.msp.core.structure.building.function;

import org.mars.sim.tools.Msg;
import org.mars_sim.msp.core.logging.SimLogger;
import org.mars_sim.msp.core.resource.ResourceUtil;
import org.mars_sim.msp.core.structure.Settlement;
import org.mars_sim.msp.core.structure.building.Building;

/**
 * A fuel power source that gives a steady supply of power.
 */
public class FuelPowerSource extends PowerSource {

	/** default serial id. */
	private static final long serialVersionUID = 1L;
	/** default logger. */
	private static final SimLogger logger = SimLogger.getLogger(FuelPowerSource.class.getName());

	private static final double MAINTENANCE_FACTOR = 2D;
	
	/** The ratio of fuel to oxidizer by mass. */
	private static final int RATIO = 4;
	/** The size of the fuel reserve tank. */
	private static final int STANDARD_RESERVE = 30;
	private static final int OXYGEN_ID = ResourceUtil.oxygenID;
	private static final int METHANE_ID = ResourceUtil.methaneID;
	
	/** The work time (millisol) required to toggle this power source on or off. */
	private static final double TOGGLE_RUNNING_WORK_TIME_REQUIRED = 2D;

	private static final double ELECTRICAL_EFFICIENCY = .7125;

	private boolean toggle = false;
	
	/** The fuel consumption rate [kg/sol]. */
	private double rate;

	private double toggleRunningWorkTime;
	/** The amount of reserved fuel. */
	private double reserveFuel;
	/** The amount of reserved oxidizer. */
	private double reserveOxidizer;

	private double time;

	private double modRate;


	/**
	 * Constructor.
	 * 
	 * @param maxPower the maximum power (kW) of the power source.
	 * @param toggle if the power source is toggled on or off.
	 * @param fuelType the fuel type.
	 * @param consumptionSpeed the rate of fuel consumption (kg/sol).
	 */
	public FuelPowerSource(double maxPower, boolean toggle, String fuelType,
			double consumptionSpeed) {
		super(PowerSourceType.FUEL_POWER, maxPower);
		rate = consumptionSpeed;
		this.toggle = toggle;
		
		modRate = rate / 1000.0;
	}
	
//	 Note : every mole of methane (16 g) releases 810 KJ of energy if burning with 2 moles of oxygen (64 g)
//	 CH4(g) + 2O2(g) -> CO2(g) + 2 H2O(g), deltaH = -890 kJ 
//	 
//	 CnH2n+2 + (3n + 1)O2 -> nCO2 + (n + 1)H2O + (6n + 2)e- 

//	it produces 890kW at the consumption rate of 16 g/s
//  or it produces 1kW at .018 g/s
	
//	Since each martian sol has 88775 earth seconds (=24*60*60 + 39*60 + 35.244),
	
//	1kW needs 1.59795 kg/sol. 
//	5kW needs 7.9897 kg/sol. 
//	60kW needs 95.877 kg/sol.
	
//	 SOFC uses methane with 1100 W-hr/kg, 
//	 This translate to 71.25 % efficiency
		
	/**
	 * Consumes the fuel.
	 * 
	 * @param time
	 * @param inv
	 * @return the amount of fuel consumed
	 */
	public double consumeFuel(double time, Settlement settlement) {
		double consumed = 0;

		double deltaFuel = time * modRate;
		
		if (deltaFuel <= reserveFuel && deltaFuel * RATIO <= reserveOxidizer) {
			reserveFuel -= deltaFuel;
			reserveOxidizer -= deltaFuel * RATIO;
			
			consumed = deltaFuel;
		}
		else {
			double fuelStored = settlement.getAmountResourceStored(METHANE_ID);
			double o2Stored = settlement.getAmountResourceStored(OXYGEN_ID);
			
			if (STANDARD_RESERVE <= fuelStored && STANDARD_RESERVE * RATIO <= o2Stored) {
				reserveFuel += STANDARD_RESERVE;
				reserveOxidizer += STANDARD_RESERVE * RATIO;
				settlement.retrieveAmountResource(METHANE_ID, STANDARD_RESERVE);
				settlement.retrieveAmountResource(OXYGEN_ID, STANDARD_RESERVE * RATIO * 1.0);
				
				reserveFuel -= deltaFuel;
				reserveOxidizer -= deltaFuel * RATIO;
				
				consumed = deltaFuel;
			}
			
			else {
				// Note that 16 g of methane requires 64 g of oxygen, a 1-to-4 ratio
				consumed = Math.min(deltaFuel, Math.min(fuelStored, o2Stored / RATIO));

				settlement.retrieveAmountResource(METHANE_ID, consumed);
				settlement.retrieveAmountResource(OXYGEN_ID, RATIO * consumed);
			}
		}

		return consumed;
	}
	
	@Override
	public void setTime(double time) {
		this.time = time;
	}
	 
	@Override
	public double getCurrentPower(Building building) {
		double power = 0D;
		if (toggle) {
			double spentFuel = consumeFuel(time, building.getSettlement());	 
			power = getMaxPower() * spentFuel * ELECTRICAL_EFFICIENCY;
		}
		 
		return power;
	}
	 

	public void toggleON() {
		toggle = true;
	}

	public void toggleOFF() {
		toggle = false;
	}

	public boolean isToggleON() {
		return toggle;
	}

	/**
	 * Gets the amount resource used as fuel.
	 * 
	 * @return amount resource.
	 */
	 public int getFuelResourceID() {
		return METHANE_ID;
	}
		 
	/**
	 * Gets the rate the fuel is consumed.
	 * 
	 * @return rate (kg/sol).
	 */
	 public double getFuelConsumptionRate() {
		 return rate;
	 }

	 /**
	  * Adds work time to toggling the power source on or off.
	  * Called by ToggleFuelPowerSource.
	  * 
	  * @param time the amount (millisols) of time to add.
	  */
	 public void addToggleWorkTime(double time) {
		 toggleRunningWorkTime += time;
		 if (toggleRunningWorkTime >= TOGGLE_RUNNING_WORK_TIME_REQUIRED) {
			 toggleRunningWorkTime = 0D;
			 toggle = !toggle;

			 String msgKey = (toggle ? "FuelPowerSource.log.turnedOn" : "FuelPowerSource.log.turnedOff");
			 logger.fine(Msg.getString(msgKey,getType().getName())); //$NON-NLS-1$
		 }
	 }

	 @Override
	 public double getAveragePower(Settlement settlement) {
		double fuelPower = getMaxPower();
		double fuelValue = settlement.getGoodsManager().getGoodValuePoint(METHANE_ID);
		fuelValue *= getFuelConsumptionRate() / 1000D * time;
		fuelPower -= fuelValue;
		if (fuelPower < 0D) fuelPower = 0D;
		return fuelPower;
	 }

	 @Override
	 public double getMaintenanceTime() {
	    return getMaxPower() * MAINTENANCE_FACTOR;
	 }
}
