/*
 * Mars Simulation Project
 * ListenToMusicMeta.java
 * @date 2022-08-31
 * @author Manny Kung
 */
package org.mars_sim.msp.core.person.ai.task.meta;

import org.mars.sim.tools.Msg;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.PhysicalCondition;
import org.mars_sim.msp.core.person.ai.task.ListenToMusic;
import org.mars_sim.msp.core.person.ai.task.util.FactoryMetaTask;
import org.mars_sim.msp.core.person.ai.task.util.Task;
import org.mars_sim.msp.core.person.ai.task.util.TaskTrait;
import org.mars_sim.msp.core.structure.building.Building;
import org.mars_sim.msp.core.structure.building.BuildingManager;
import org.mars_sim.msp.core.structure.building.function.FunctionType;
import org.mars_sim.msp.core.vehicle.Vehicle;

/**
 * Meta task for the ListenToMusic task.
 */
public class ListenToMusicMeta extends FactoryMetaTask {

    /** Task name */
    private static final String NAME = Msg.getString(
            "Task.description.listenToMusic"); //$NON-NLS-1$

    private static final double CAP = 1_000D;
    
    /** Modifier if during person's work shift. */
    private static final double WORK_SHIFT_MODIFIER = .2D;

    /** default logger. */
//  May bring back private static final Logger logger = Logger.getLogger(ListenToMusicMeta.class.getName())

    public ListenToMusicMeta() {
		super(NAME, WorkerType.PERSON, TaskScope.ANY_HOUR);
		setTrait(TaskTrait.RELAXATION);

	}

    @Override
    public Task constructInstance(Person person) {
        return new ListenToMusic(person);
    }

    @Override
    public double getProbability(Person person) {
        double result = 0D;

        if (person.isInside()) {
	        double pref = person.getPreference().getPreferenceScore(this);

	     	result = pref * 2.5D;

	        // Probability affected by the person's stress and fatigue.
	        PhysicalCondition condition = person.getPhysicalCondition();
	        double stress = condition.getStress();

	        if (pref > 0) {
	         	if (stress > 45D)
	         		result*=1.5;
	         	else if (stress > 65D)
	         		result*=2D;
	         	else if (stress > 85D)
	         		result*=3D;
	         	else
	         		result*=4D;
	        }

	        if (person.isInSettlement()) {
				// Check if a person has a designated bed
				Building recBuilding = BuildingManager.getAvailableFunctionTypeBuilding(person, FunctionType.RECREATION);
				result *= getBuildingModifier(recBuilding, person);

	            // Modify probability if during person's work shift.
	            if (person.isOnDuty()) {
	                result*= WORK_SHIFT_MODIFIER;
	            }
	        }

	        else if (person.isInVehicle()) {
		        // Check if person is in a moving rover.
		        if (Vehicle.inMovingRover(person)) {
		        	result += 20D;
		        }
	        }
        }

        if (result > CAP)
        	result = CAP;
        
        if (result < 0) result = 0;

        return result;
    }
}
