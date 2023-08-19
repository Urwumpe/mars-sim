/**
 * Mars Simulation Project
 * YogaMeta.java
 * @version 3.2.0 2021-06-20
 * @author Scott Davis
 */
package org.mars_sim.msp.core.person.ai.task.meta;

import org.mars.sim.tools.Msg;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.PhysicalCondition;
import org.mars_sim.msp.core.person.ai.task.Yoga;
import org.mars_sim.msp.core.person.ai.task.util.FactoryMetaTask;
import org.mars_sim.msp.core.person.ai.task.util.Task;
import org.mars_sim.msp.core.person.ai.task.util.TaskTrait;
import org.mars_sim.msp.core.structure.building.Building;
import org.mars_sim.msp.core.structure.building.BuildingManager;
import org.mars_sim.msp.core.structure.building.function.FunctionType;

/**
 * Meta task for the Yoga task.
 */
public class YogaMeta extends FactoryMetaTask {

    /** Task name */
    private static final String NAME = Msg.getString(
            "Task.description.yoga"); //$NON-NLS-1$
 
    public YogaMeta() {
		super(NAME, WorkerType.PERSON, TaskScope.NONWORK_HOUR);
		setTrait(TaskTrait.TREATMENT, TaskTrait.AGILITY, TaskTrait.RELAXATION);
	}
    
    @Override
    public Task constructInstance(Person person) {
        return new Yoga(person);
    }

    @Override
    public double getProbability(Person person) {

        double result = 0D;

        if (person.isInVehicle()) {	
        	return result;
        }
        
        else if (!person.getPreference().isTaskDue(this) && person.isInSettlement()) {

        	 // Probability affected by the person's stress and fatigue.
            PhysicalCondition condition = person.getPhysicalCondition();
            double stress = condition.getStress();
            double fatigue = condition.getFatigue();
            double kJ = condition.getEnergy();
            double hunger = condition.getHunger();
            double[] muscle = condition.getMusculoskeletal();

            double exerciseMillisols = person.getCircadianClock().getTodayExerciseTime();
            
            if (kJ < 500 || fatigue > 750 || hunger > 750)
            	return 0;
 
            result = kJ/2000 
            		// Note: The desire to exercise increases linearly right after waking up
            		// from bed up to the first 333 msols
            		// After the first 333 msols, it decreases linearly for the rest of the day
            		+ Math.max(333 - fatigue, -666)
            		// Note: muscle condition affects the desire to exercise
            		+ muscle[0]/2.5 - muscle[2]/2.5
            		+ stress / 10
            		- exerciseMillisols * 5;
            
            if (result < 0) 
            	return 0;
            
            double pref = person.getPreference().getPreferenceScore(this);
         	result += result * pref / 2D;

            if (result < 0) 
            	return 0;
            
            // Get an available gym.
            Building building =  BuildingManager.getAvailableFunctionTypeBuilding(person, FunctionType.EXERCISE);
            result *= getBuildingModifier(building, person);
        }
        
        return result;
    }
}
