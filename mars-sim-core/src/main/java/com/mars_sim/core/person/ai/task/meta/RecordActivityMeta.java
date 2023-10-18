/*
 * Mars Simulation Project
 * RecordActivityMeta.java
 * @date 2021-09-27
 * @author Manny Kung
 */
package com.mars_sim.core.person.ai.task.meta;

import com.mars_sim.core.person.Person;
import com.mars_sim.core.person.PhysicalCondition;
import com.mars_sim.core.person.ai.job.util.JobType;
import com.mars_sim.core.person.ai.role.RoleType;
import com.mars_sim.core.person.ai.task.RecordActivity;
import com.mars_sim.core.person.ai.task.util.FactoryMetaTask;
import com.mars_sim.core.person.ai.task.util.Task;
import com.mars_sim.core.person.ai.task.util.TaskTrait;
import com.mars_sim.tools.Msg;
import com.mars_sim.tools.util.RandomUtil;

/**
 * Meta task for the RecordActivity task.
 */
public class RecordActivityMeta extends FactoryMetaTask {

    /** Task name */
    private static final String NAME = Msg.getString(
            "Task.description.recordActivity"); //$NON-NLS-1$

    public RecordActivityMeta() {
		super(NAME, WorkerType.PERSON, TaskScope.WORK_HOUR);
		setTrait(TaskTrait.ARTISTIC);
		setPreferredJob(JobType.REPORTER);
	}

    @Override
    public Task constructInstance(Person person) {
        return new RecordActivity(person);
    }

    @Override
    public double getProbability(Person person) {

    	// Do not allow to record activity outside for now
    	if (person.isOutside())
    		return 0;

    	double result = 0D;

        // Probability affected by the person's stress and fatigue.
        PhysicalCondition condition = person.getPhysicalCondition();
        double fatigue = condition.getFatigue();
        double stress = condition.getStress();
        double hunger = condition.getHunger();

        if (fatigue > 1500 || stress > 75 || hunger > 750)
        	return 0;

        if (person.getMind().getJob() == JobType.REPORTER) {
        	result += RandomUtil.getRandomDouble(200);
        }

        result *= getPersonModifier(person);

        if (person.isInside()) {

            if (fatigue < 1200D || stress < 75D || hunger < 750D) {

            	result -= (fatigue/50 + stress/15 + hunger/50);
            }

            // NOTE: what drives a person go to a particular building ?

    	}

        else {
            if (fatigue < 600D && stress< 25D|| hunger < 500D) {
            	result -= (fatigue/100 + stress/10 + hunger/50);
            }
            else
            	result = 0;
        }

        // Effort-driven task modifier.
        result *= .5 * person.getAssociatedSettlement().getGoodsManager().getTourismFactor();

        if (result > 0) {
            RoleType roleType = person.getRole().getType();

            if (roleType != null) {
            	if (roleType == RoleType.PRESIDENT)
	            	result -= 300D;

	        	else if (roleType == RoleType.MAYOR)
	            	result -= 200D;

	        	else if (roleType == RoleType.COMMANDER)
	                result -= 100D;

	        	else if (roleType == RoleType.SUB_COMMANDER)
	        		result -= 50D;

		        else if (roleType.isChief())
	            	result -= 10D;
            }
        }

        if (result < 0) result = 0;

        return result;
    }
}
