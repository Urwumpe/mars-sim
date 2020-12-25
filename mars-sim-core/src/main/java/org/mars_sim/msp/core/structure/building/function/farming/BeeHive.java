/**
 * Mars Simulation Project
 * BeeHive.java
 * @version 3.1.2 2020-09-02
 * @author Manny Kung
 */

package org.mars_sim.msp.core.structure.building.function.farming;

import java.io.Serializable;
import java.util.logging.Logger;

import org.mars_sim.msp.core.structure.building.function.Function;
import org.mars_sim.msp.core.structure.building.function.FunctionType;
import org.mars_sim.msp.core.time.ClockPulse;

public class BeeHive
extends Function
implements Serializable {
	
	/** default serial id. */
    private static final long serialVersionUID = 1L;
    /** default logger. */
	private static Logger logger = Logger.getLogger(BeeHive.class.getName());

    public static final int QUEEN = 0;
    public static final int WORKER = 1;
    public static final int MALE = 2;
    public static final int EGG = 3;
    public static final int LARVAE = 4;
    public static final int PUPA = 5;
    
    private BeeGrowing beeGrowing;
    
    private double propolisAmount;
    private double beeswaxAmount;
    private double honeycombCells;
    private double honeyAmount;
    
    private String beeSpecies;

    private Bee queenBee;
    private Bee workerBee;
    private Bee maleBee;       
    private Bee beeEgg; 
    private Bee beeLarvae;
    private Bee beePupa;
    

    public BeeHive(BeeGrowing beeGrowing, String beeSpecies) {
        // Use Function constructor.
        super(FunctionType.FARMING, beeGrowing.getFarming().getBuilding());
		
    	this.beeGrowing = beeGrowing;
    	this.beeSpecies = beeSpecies; 	
        		
        queenBee = new Bee(this, QUEEN, beeSpecies);        
        //workerBee = new Bee(this, WORKER);
        //maleBee = new Bee(this, MALE);       
        //beeEgg = new Bee(this, EGG); 
        beeLarvae = new Bee(this, LARVAE, beeSpecies);
        //beePupa = new Bee(this, PUPA);
        
	}


    public BeeGrowing getBeeGrowing() {
    	return beeGrowing;
    }
    
	@Override
	public boolean timePassing(ClockPulse pulse) {	
		boolean valid = isValid(pulse);
		if (valid) {
			double time = pulse.getElapsed();
	        if (queenBee !=null) queenBee.timePassing(time);        
	        if (workerBee !=null) workerBee.timePassing(time);
	        if (maleBee !=null) maleBee.timePassing(time);      
	        //if (beeEgg !=null) beeEgg.timePassing(time);
	        if (beeLarvae !=null) beeLarvae.timePassing(time);
	        if (beePupa !=null) beePupa.timePassing(time);
		}
        return valid;
	}

	public Bee getQueen() {
		return queenBee;
	}

	public Bee getWorkerBee() {
		return workerBee;
	}

	public Bee getMaleBee() {
		return maleBee;
	}

	public Bee getLarvae() {
		return beeLarvae;
	}

	public Bee getPupa() {
		return beePupa;
	}	
}
