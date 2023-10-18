/*
 * Mars Simulation Project
 * AnalyzeMapDataMeta.java
 * @date 2023-07-04
 * @author Manny Kung
 */

package com.mars_sim.core.person.ai.task.meta;

import java.util.List;
import java.util.stream.Collectors;

import com.mars_sim.core.environment.ExploredLocation;
import com.mars_sim.core.person.Person;
import com.mars_sim.core.person.ai.fav.FavoriteType;
import com.mars_sim.core.person.ai.job.util.JobType;
import com.mars_sim.core.person.ai.role.RoleType;
import com.mars_sim.core.person.ai.task.AnalyzeMapData;
import com.mars_sim.core.person.ai.task.util.FactoryMetaTask;
import com.mars_sim.core.person.ai.task.util.Task;
import com.mars_sim.core.person.ai.task.util.TaskTrait;
import com.mars_sim.core.vehicle.Vehicle;
import com.mars_sim.mapdata.location.Coordinates;
import com.mars_sim.tools.Msg;

/**
 * Meta task for the AnalyzeMapData task.
 */
public class AnalyzeMapDataMeta extends FactoryMetaTask {
    
	/** Task name */
	private static final double VALUE = 0.5D;
	
    /** Task name */
    private static final String NAME = Msg.getString(
            "Task.description.analyzeMapData"); //$NON-NLS-1$

    /** default logger. */
//	May add back private static SimLogger logger = SimLogger.getLogger(AnalyzeMapDataMeta.class.getName())

    public AnalyzeMapDataMeta() {
		super(NAME, WorkerType.PERSON, TaskScope.WORK_HOUR);
		setFavorite(FavoriteType.RESEARCH, FavoriteType.OPERATION);
		setTrait(TaskTrait.ACADEMIC);
		
		setPreferredJob(JobType.AREOLOGIST, JobType.PHYSICIST, 
				JobType.COMPUTER_SCIENTIST, JobType.ENGINEER,
				JobType.MATHEMATICIAN, JobType.PILOT);
		setPreferredRole(RoleType.CHIEF_OF_SCIENCE, RoleType.SCIENCE_SPECIALIST);
	}

    @Override
    public Task constructInstance(Person person) {
        return new AnalyzeMapData(person);
    }

    @Override
    public double getProbability(Person person) {
        
        double result = 0D;
	
        // Probability affected by the person's stress and fatigue.
        if (!person.getPhysicalCondition().isFitByLevel(1000, 80, 1000))
        	return 0;
        
        if (person.isInside()) {

        	int numUnimproved = 0;
        	
        	List<Coordinates> coords = person.getAssociatedSettlement()
        			.getNearbyMineralLocations()
        			.stream()
        			.collect(Collectors.toList());  	
        	
        	int numCoords = coords.size();
        	
    		List<ExploredLocation> siteList = surfaceFeatures
        			.getAllRegionOfInterestLocations().stream()
        			.filter(site -> site.isMinable()
        					&& coords.contains(site.getLocation()))
        			.collect(Collectors.toList());
        	
        	for (ExploredLocation el: siteList) {
	        	int est = el.getNumEstimationImprovement();
	        	numUnimproved += ExploredLocation.IMPROVEMENT_THRESHOLD - est;
        	}
        	    	
        	int num = siteList.size();
        	
        	if (num == 0)
        		return 0;
        	
    		result += VALUE * numUnimproved / num + numCoords / VALUE;
	
            // Check if person is in a moving rover.
            if (person.isInVehicle() && Vehicle.inMovingRover(person)) {
    	        // the bonus for being inside a vehicle since there's little things to do
                result += 20D;
            }
            
            if (JobType.COMPUTER_SCIENTIST == person.getMind().getJob())
            	result *= 1.5D;
            
            if (RoleType.COMPUTING_SPECIALIST == person.getRole().getType())
            	result *= 1.5D;
            
            else if (RoleType.CHIEF_OF_COMPUTING == person.getRole().getType())
            	result *= 1.25D;
        }

        if (result == 0) return 0;
		result *= person.getAssociatedSettlement().getGoodsManager().getResearchFactor();

        result *= getPersonModifier(person);

        if (result < 0) result = 0;

        return result;
    }
}
