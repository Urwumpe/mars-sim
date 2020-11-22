/**
 * Mars Simulation Project
 * VehicleAirlock.java
 * @version 3.1.2 2020-09-02
 * @author Scott Davis
 */
package org.mars_sim.msp.core.vehicle;

import java.awt.geom.Point2D;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.mars_sim.msp.core.Inventory;
import org.mars_sim.msp.core.LifeSupportInterface;
import org.mars_sim.msp.core.LocalAreaUtil;
import org.mars_sim.msp.core.LogConsolidated;
import org.mars_sim.msp.core.Msg;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.structure.Airlock;

/**
 * This class represents an airlock for a vehicle.
 */
public class VehicleAirlock
extends Airlock {

	/** default serial id. */
	private static final long serialVersionUID = 1L;

	/** default logger. */
	private static Logger logger = Logger.getLogger(VehicleAirlock.class.getName());
	private static String loggerName = logger.getName();
	private static String sourceName = loggerName.substring(loggerName.lastIndexOf(".") + 1, loggerName.length());
	
	// Data members.
	/** The vehicle this airlock is for. */
	private Vehicle vehicle;
	private Point2D airlockInsidePos;
	private Point2D airlockInteriorPos;
	private Point2D airlockExteriorPos;

	/**
	 * Constructor.
	 * @param vehicle the vehicle this airlock of for.
	 * @param capacity number of people airlock can hold.
	 */
	public VehicleAirlock(
		Vehicle vehicle, int capacity, double xLoc, double yLoc,
		double interiorXLoc, double interiorYLoc, double exteriorXLoc,
		double exteriorYLoc
	) {
		// User Airlock constructor
		super(capacity);//, vehicle);

		if (vehicle == null) {
			throw new IllegalArgumentException(Msg.getString("VehicleAirlock.error.null")); //$NON-NLS-1$
		}
		else if (!(vehicle instanceof Crewable)) {
			throw new IllegalArgumentException(Msg.getString("VehicleAirlock.error.notCrewable")); //$NON-NLS-1$
		}
		else if (!(vehicle instanceof LifeSupportInterface)) {
			throw new IllegalArgumentException(Msg.getString("VehicleAirlock.error.noLifeSupport")); //$NON-NLS-1$
		}
		else {
			this.vehicle = vehicle;
		}

		// Determine airlock interior position.
//		airlockInteriorPos = new Point2D.Double(interiorXLoc, interiorYLoc);
		airlockInteriorPos = LocalAreaUtil.getLocalRelativeLocation(interiorXLoc,interiorYLoc, vehicle);

		// Determine airlock exterior position.
//		airlockExteriorPos = new Point2D.Double(exteriorXLoc, exteriorYLoc);
		airlockExteriorPos = LocalAreaUtil.getLocalRelativeLocation(exteriorXLoc, exteriorYLoc, vehicle);

		// Determine airlock inside position.
//		airlockInsidePos = new Point2D.Double(xLoc, yLoc);
		airlockInsidePos = LocalAreaUtil.getLocalRelativeLocation(xLoc, yLoc, vehicle);

	}

//	/**
//	 * Causes a person within the airlock to exit either inside or outside.
//	 *
//	 * @param person the person to exit.
//	 * @throws Exception if person is not in the airlock.
//	 */
//	protected boolean exitAirlock(Person person) {
//    	boolean successful = false;
//		// TODO: how to detect and bypass going through the airlock if a vehicle is inside a garage in a settlement
//		// see exitingRoverGaragePhase() in Walk
//		
//		if (inAirlock(person)) {
//			if (AirlockState.PRESSURIZED == getState()) {
//				// check if the airlock has been sealed from outside and pressurized, ready to 
//            	// open the inner door to release the person into the vehicle
//				successful = stepIntoAirlock(person);
//				
//			}
//			else if (AirlockState.DEPRESSURIZED == getState()) {
//            	// check if the airlock has been de-pressurized, ready to open the outer door to 
//            	// get exposed to the outside air and release the person
//				successful = stepIntoMarsSurface(person);
//
//			}
//			else {
//				logger.severe(Msg.getString("VehicleAirlock.error.badState", getState())); //$NON-NLS-1$
//			}
//		}
//		else {
//			throw new IllegalStateException(Msg.getString("VehicleAirlock.error.notInAirlock",person.getName(),getEntityName())); //$NON-NLS-1$
//		}
//		
//		return successful;
//	}

   @Override
    protected boolean egress(Person person) {
    	boolean successful = false;
      	LogConsolidated.log(logger, Level.INFO, 0, sourceName,
	  				"[" + person.getLocale() 
	  				 + "] " + person + " was calling egress.");
      	
        if (inAirlock(person)) {
            // check if the airlock has been de-pressurized, ready to open the outer door to 
            // get exposed to the outside air and release the person
            successful = stepOnMars(person);
        }
        else {
            throw new IllegalStateException(person.getName() + " not in " + getEntityName());
        }
        
        return successful;
    }

    @Override
    protected boolean ingress(Person person) {
    	boolean successful = false;
      	LogConsolidated.log(logger, Level.INFO, 0, sourceName,
	  				"[" + person.getLocale() 
	  				 + "] " + person + " was calling ingress.");
      	
        if (inAirlock(person)) {
            // check if the airlock has been sealed from outside and pressurized, ready to 
            // open the inner door to release the person into the settlement
            successful = stepInside(person);
        }

        else {
            throw new IllegalStateException(person.getName() + " not in airlock of " + getEntityName());
        }
        
        return successful;
    }
	    
	   /**
     * Steps back into an airlock of a vehicle
     * 
     * @param person
     */
    public boolean stepInside(Person person) {
    	boolean successful = false;
    	if (person.isOutside()) {						
            // 1.1. Transfer a person from the surface of Mars to the vehicle
    		successful = person.transfer(marsSurface, vehicle);
        
			if (successful)
				LogConsolidated.log(logger, Level.FINER, 0, sourceName, 
					"[" + person.getLocale() + "] "
					+ person.getName() + " had just stepped inside rover " + vehicle.getName());
			else
				LogConsolidated.log(logger, Level.SEVERE, 0, sourceName, 
						"[" + person.getLocale() + "] "
						+ person.getName() + " could not step inside rover " + vehicle.getName());

		}
    	
		else if (person.isInSettlement()) {
			LogConsolidated.log(logger, Level.SEVERE, 0, sourceName, 
					Msg.getString("VehicleAirlock.error.notOutside", person.getName(), getEntityName()));
			//throw new IllegalStateException(Msg.getString("VehicleAirlock.error.notOutside",person.getName(),getEntityName())); //$NON-NLS-1$
		}
    	
		return successful;
    }
    
    /**
     * Gets outside of the airlock and step into the surface of Mars
     * 
     * @param person
     */
    public boolean stepOnMars(Person person) {
    	boolean successful = false;
		if (person.isInVehicle()) {

            // 5.1. Transfer a person from the vehicle to the surface of Mars
			successful = person.transfer(vehicle, marsSurface);
			
			if (successful) {
				// 5.2 Set the person's coordinates to that of the settlement's
				person.setCoordinates(vehicle.getCoordinates());
						
				LogConsolidated.log(logger, Level.FINER, 0, sourceName, 
					"[" + person.getLocale() + "] "
					+ person.getName() + " had just stepped outside rover " + vehicle.getName());
			}
			else
				LogConsolidated.log(logger, Level.SEVERE, 0, sourceName, 
						"[" + person.getLocale() + "] "
						+ person.getName() + " could not step outside rover " + vehicle.getName());
		}
		else if (person.isOutside()) {
			LogConsolidated.log(logger, Level.SEVERE, 0, sourceName, 
					Msg.getString("VehicleAirlock.error.notInside", person.getName(), getEntityName()));
			//throw new IllegalStateException(Msg.getString("VehicleAirlock.error.notInside",person.getName(),getEntityName())); //$NON-NLS-1$
		}
		
		return successful;
    }
    
    
	/**
	 * Gets the name of the entity this airlock is attached to.
	 * @return name {@link String}
	 */
	public String getEntityName() {
		return vehicle.getName();
	}

	/**
	 * Gets the inventory of the entity this airlock is attached to.
	 * @return inventory {@link Inventory}
	 */
	@Override
	public Inventory getEntityInventory() {
		return vehicle.getInventory();
	}

	@Override
	public Object getEntity() {
		return vehicle;
	}

	@Override
	public String getLocale() {
		return vehicle.getLocale();
	}

	@Override
	public Point2D getAvailableInteriorPosition(boolean value) {
		return getAvailableInteriorPosition();
	}

	@Override
	public Point2D getAvailableExteriorPosition(boolean value) {
		return getAvailableExteriorPosition();
	}
	
	@Override
	public Point2D getAvailableInteriorPosition() {
//		if (airlockInteriorPos == null)
//			airlockInteriorPos = LocalAreaUtil.getLocalRelativeLocation(interiorXLoc, interiorYLoc, vehicle);
		return airlockInteriorPos;
	}

	@Override
	public Point2D getAvailableExteriorPosition() {
//		if (airlockExteriorPos == null)
//			airlockExteriorPos = LocalAreaUtil.getLocalRelativeLocation(airlockExteriorPos.getX(),airlockExteriorPos.getY(),vehicle);
		return airlockExteriorPos;
	}

	@Override
	public Point2D getAvailableAirlockPosition() {
//		if (airlockExteriorPos == null)
//			airlockExteriorPos = LocalAreaUtil.getLocalRelativeLocation(airlockInsidePos.getX(),airlockInsidePos.getY(),vehicle);
		return airlockInsidePos;
	}
	
	public void destroy() {
	    vehicle = null; 
	    airlockInsidePos = null;
	    airlockInteriorPos = null;
	    airlockExteriorPos = null;
	}
}
