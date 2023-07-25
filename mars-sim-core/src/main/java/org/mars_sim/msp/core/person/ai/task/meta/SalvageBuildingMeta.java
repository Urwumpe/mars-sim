/**
 * Mars Simulation Project
 * SalvageBuildingMeta.java
 * @version 3.2.0 2021-06-20
 * @author Scott Davis
 */
package org.mars_sim.msp.core.person.ai.task.meta;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.mars.sim.tools.Msg;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.ai.fav.FavoriteType;
import org.mars_sim.msp.core.person.ai.job.util.JobType;
import org.mars_sim.msp.core.person.ai.mission.SalvageMission;
import org.mars_sim.msp.core.person.ai.task.EVAOperation;
import org.mars_sim.msp.core.person.ai.task.SalvageBuilding;
import org.mars_sim.msp.core.person.ai.task.util.FactoryMetaTask;
import org.mars_sim.msp.core.person.ai.task.util.Task;
import org.mars_sim.msp.core.person.ai.task.util.TaskTrait;
import org.mars_sim.msp.core.robot.Robot;
import org.mars_sim.msp.core.structure.Settlement;

/**
 * Meta task for the SalvageBuilding task.
 */
public class SalvageBuildingMeta extends FactoryMetaTask {

    /** Task name */
    private static final String NAME = Msg.getString(
            "Task.description.salvageBuilding"); //$NON-NLS-1$

    /** default logger. */
    private static final Logger logger = Logger.getLogger(SalvageBuildingMeta.class.getName());

    public SalvageBuildingMeta() {
		super(NAME, WorkerType.PERSON, TaskScope.WORK_HOUR);
		setFavorite(FavoriteType.OPERATION, FavoriteType.TINKERING);
		setTrait(TaskTrait.STRENGTH);
		setPreferredJob(JobType.ARCHITECT);
	}

    @Override
    public Task constructInstance(Person person) {
        return new SalvageBuilding(person);
    }

    @Override
    public double getProbability(Person person) {

        double result = 0D;

        // Probability affected by the person's stress and fatigue.
        if (!person.getPhysicalCondition().isFitByLevel(1000, 70, 1000))
        	return 0;
        
        // Check if an airlock is available
        if (EVAOperation.getWalkableAvailableAirlock(person, false) == null) {
            return 0;
        }

        // Check if it is night time.
        if (EVAOperation.isGettingDark(person)) {
                return 0;
        }

        if (person.isInSettlement()) {

            // Check all building salvage missions occurring at the settlement.
            try {
                List<SalvageMission> missions = SalvageBuilding.
                        getAllMissionsNeedingAssistance(person.getSettlement());
                result = 100D * missions.size();
            }
            catch (Exception e) {
                logger.log(Level.SEVERE, "Error finding building salvage missions.", e);
            }

            // Crowded settlement modifier
            Settlement settlement = person.getSettlement();
            if (settlement.getIndoorPeopleCount() > settlement.getPopulationCapacity()) {
                result *= 2D;
            }

            result *= getPersonModifier(person);
        }

        return result;
    }

	@Override
	public double getProbability(Robot robot) {

        double result = 0D;
/*
        if (robot.getBotMind().getRobotJob() instanceof Constructionbot) {

	        if (robot.getLocationSituation() == LocationSituation.IN_SETTLEMENT) {

	            // Check all building salvage missions occurring at the settlement.
	            try {
	                List<BuildingSalvageMission> missions = SalvageBuilding.
	                        getAllMissionsNeedingAssistance(robot.getSettlement());
	                result = 100D * missions.size();
	            }
	            catch (Exception e) {
	                logger.log(Level.SEVERE, "Error finding building salvage missions.", e);
	            }
	        }

	        // Effort-driven task modifier.
            result *= robot.getPerformanceRating();

	        // Check if an airlock is available
            if (EVAOperation.getWalkableAvailableAirlock(robot) == null) {
                result = 0D;
            }

	        // Check if it is night time.
            SurfaceFeatures surface = Simulation.instance().getMars().getSurfaceFeatures();
            if (surface.getSolarIrradiance(robot.getCoordinates()) == 0D) {
                if (!surface.inDarkPolarRegion(robot.getCoordinates())) {
                    result = 0D;
                }
            }

        }
*/
        return result;
    }
}
