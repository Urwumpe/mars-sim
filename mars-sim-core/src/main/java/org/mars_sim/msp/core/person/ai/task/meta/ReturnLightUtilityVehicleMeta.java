/**
 * Mars Simulation Project
 * ReturnLightUtilityVehicleMeta.java
 * @version 3.2.0 2021-06-20
 * @author Scott Davis
 */
package org.mars_sim.msp.core.person.ai.task.meta;

import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.ai.task.ReturnLightUtilityVehicle;
import org.mars_sim.msp.core.person.ai.task.util.FactoryMetaTask;
import org.mars_sim.msp.core.person.ai.task.util.Task;
import org.mars_sim.msp.core.vehicle.LightUtilityVehicle;
import org.mars_sim.tools.Msg;

/**
 * Meta task for the ReturnLightUtilityVehicle task.
 */
public class ReturnLightUtilityVehicleMeta extends FactoryMetaTask {
    
    /** Task name */
    private static final String NAME = Msg.getString(
            "Task.description.returnLightUtilityVehicle"); //$NON-NLS-1$
    
    public ReturnLightUtilityVehicleMeta() {
		super(NAME, WorkerType.PERSON, TaskScope.ANY_HOUR);
	}

    @Override
    public Task constructInstance(Person person) {
        return new ReturnLightUtilityVehicle(person);
    }

    @Override
    public double getProbability(Person person) {
        double result = 0D;

        if (person.isInVehicle() && person.getVehicle() instanceof LightUtilityVehicle) {
            result = 500D;

	        if (result > 0)
            	result = result + result * person.getPreference().getPreferenceScore(this)/5D;

	        if (result < 0) result = 0;
        }

        return result;
    }
}
