/*
 * Mars Simulation Project
 * PerformMathematicalModelingMeta.java
 * @Date 2021-12-20
 * @author Scott Davis
 */
package com.mars_sim.core.person.ai.task.meta;

import java.util.Iterator;
import java.util.logging.Logger;

import com.mars_sim.core.person.Person;
import com.mars_sim.core.person.ai.fav.FavoriteType;
import com.mars_sim.core.person.ai.job.util.JobType;
import com.mars_sim.core.person.ai.role.RoleType;
import com.mars_sim.core.person.ai.task.PerformMathematicalModeling;
import com.mars_sim.core.person.ai.task.util.FactoryMetaTask;
import com.mars_sim.core.person.ai.task.util.Task;
import com.mars_sim.core.person.ai.task.util.TaskTrait;
import com.mars_sim.core.science.ScienceType;
import com.mars_sim.core.science.ScientificStudy;
import com.mars_sim.core.structure.Lab;
import com.mars_sim.core.vehicle.Vehicle;
import com.mars_sim.tools.Msg;

/**
 * Meta task for the PerformMathematicalModeling task.
 */
public class PerformMathematicalModelingMeta extends FactoryMetaTask {
    
    /** Task name */
    private static final String NAME = Msg.getString(
            "Task.description.performMathematicalModeling"); //$NON-NLS-1$

    /** default logger. */
    private static final Logger logger = Logger.getLogger(PerformMathematicalModelingMeta.class.getName()); 
    
    public PerformMathematicalModelingMeta() {
		super(NAME, WorkerType.PERSON, TaskScope.WORK_HOUR);
		setFavorite(FavoriteType.RESEARCH);
		setTrait(TaskTrait.ACADEMIC);
		setPreferredJob(JobType.MATHEMATICIAN, JobType.PHYSICIST, JobType.COMPUTER_SCIENTIST, JobType.ENGINEER);
		setPreferredRole(RoleType.CHIEF_OF_SCIENCE, RoleType.SCIENCE_SPECIALIST);
	}


    @Override
    public Task constructInstance(Person person) {
        return new PerformMathematicalModeling(person);
    }

    @Override
    public double getProbability(Person person) {

        ScientificStudy primaryStudy = person.getStudy();
        if (primaryStudy == null)
        	return 0;
        
        double result = 0D;

        // Probability affected by the person's stress and fatigue.
        if (!person.getPhysicalCondition().isFitByLevel(1000, 70, 1000))
        	return 0;
        
        if (person.isInside()) {

	        ScienceType mathematics = ScienceType.MATHEMATICS;

	        // Add probability for researcher's primary study (if any).
	        if (primaryStudy != null && ScientificStudy.RESEARCH_PHASE.equals(primaryStudy.getPhase())
	            && !primaryStudy.isPrimaryResearchCompleted()
	            && mathematics == primaryStudy.getScience()) {
                try {
                    Lab lab = PerformMathematicalModeling.getLocalLab(person);
                    if (lab != null) {
                        double primaryResult = 50D;
                        // Get lab building crowding modifier.
                        primaryResult *= PerformMathematicalModeling.getLabCrowdingModifier(person, lab);
                        // If researcher's current job isn't related to study science, divide by two.
                        JobType job = person.getMind().getJob();
                        if (job != null && primaryStudy.getScience() != ScienceType.getJobScience(job)) {
                        	primaryResult /= 2D;
                        }

                        result += primaryResult;
                        
                        // Check if person is in a moving rover.
                        if (person.isInVehicle() && Vehicle.inMovingRover(person)) {
                	        // the bonus for being inside a vehicle since there's little things to do
                            result += 20D;
                        }
                    }
                }
                catch (Exception e) {
                    logger.severe("getProbability(): " + e.getMessage());
                }
	        }

	        // Add probability for each study researcher is collaborating on.
	        Iterator<ScientificStudy> i = person.getCollabStudies().iterator();
	        while (i.hasNext()) {
	            ScientificStudy collabStudy = i.next();
	            if (ScientificStudy.RESEARCH_PHASE.equals(collabStudy.getPhase())
	                 && !collabStudy.isCollaborativeResearchCompleted(person)) {
                    ScienceType collabScience = collabStudy.getContribution(person);
                    if (mathematics == collabScience) {
                        try {
                            Lab lab = PerformMathematicalModeling.getLocalLab(person);
                            if (lab != null) {
                                double collabResult = 25D;
                                // Get lab building crowding modifier.
                                collabResult *= PerformMathematicalModeling.getLabCrowdingModifier(person, lab);
                                // If researcher's current job isn't related to study science, divide by two.
                                JobType job = person.getMind().getJob();
                                if (job != null && collabScience != ScienceType.getJobScience(job)) {
                                	collabResult /= 2D;
                                }

                                result += collabResult;
                            }
                        }
                        catch (Exception e) {
                            logger.severe("getProbability(): " + e.getMessage());
                        }
                    }
	            }
	        }

	        if (result == 0) return 0;
    		result *= person.getAssociatedSettlement().getGoodsManager().getResearchFactor();

	        result *= getPersonModifier(person);
        }

        if (result < 0) result = 0;
        
        return result;
    }
}
