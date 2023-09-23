/*
 * Mars Simulation Project
 * MaintainVehicleMeta.java
 * @date 2022-09-20
 * @author Scott Davis
 */
package org.mars_sim.msp.core.person.ai.task.meta;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.mars.sim.tools.Msg;
import org.mars_sim.msp.core.data.RatingScore;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.ai.job.util.JobType;
import org.mars_sim.msp.core.person.ai.task.MaintainEVAVehicle;
import org.mars_sim.msp.core.person.ai.task.MaintainGarageVehicle;
import org.mars_sim.msp.core.person.ai.task.util.MetaTask;
import org.mars_sim.msp.core.person.ai.task.util.SettlementMetaTask;
import org.mars_sim.msp.core.person.ai.task.util.SettlementTask;
import org.mars_sim.msp.core.person.ai.task.util.Task;
import org.mars_sim.msp.core.person.ai.task.util.TaskProbabilityUtil;
import org.mars_sim.msp.core.robot.Robot;
import org.mars_sim.msp.core.robot.RobotType;
import org.mars_sim.msp.core.structure.Settlement;
import org.mars_sim.msp.core.structure.building.Building;
import org.mars_sim.msp.core.structure.building.function.FunctionType;
import org.mars_sim.msp.core.structure.building.function.VehicleMaintenance;
import org.mars_sim.msp.core.vehicle.Vehicle;

/**
 * Meta task for the MaintainGarageVehicle task.
 */
public class MaintainVehicleMeta extends MetaTask implements SettlementMetaTask {
	private static class VehicleMaintenanceJob extends SettlementTask {

		private static final long serialVersionUID = 1L;

        public VehicleMaintenanceJob(SettlementMetaTask owner, Vehicle target, boolean eva, RatingScore score) {
            super(owner, "Vehicle Maintenance " + (eva ? "via EVA " : ""), target, score);
			setEVA(eva);
        }

		/**
         * The vehicle needing maintenance is the focus.
         */
        private Vehicle getVehicle() {
            return (Vehicle) getFocus();
        }

        @Override
        public Task createTask(Person person) {
			if (isEVA()) {
				return new MaintainEVAVehicle(person, getVehicle());
			}
            return new MaintainGarageVehicle(person, getVehicle());
        }

        @Override
        public Task createTask(Robot robot) {
			if (isEVA()) {
				throw new IllegalStateException("Robots can not do EVA Vehicel maintenance");
			}
            return new MaintainGarageVehicle(robot, getVehicle());
        }
    }

	/** Task name */
	private static final String NAME = Msg.getString("Task.description.maintainGarageVehicle"); //$NON-NLS-1$
	
    public MaintainVehicleMeta() {
		super(NAME, WorkerType.BOTH, TaskScope.WORK_HOUR);
		
		setPreferredJob(JobType.MECHANICS);

		addPreferredRobot(RobotType.REPAIRBOT);
	}

    /**
     * For a robot can not do EVA tasks so will return a zero factor in this case.
     * 
	 * @param t Task being scored
	 * @parma r Robot requesting work.
	 * @return The factor to adjust task score; 0 means task is not applicable
     */
	@Override
	public RatingScore assessRobotSuitability(SettlementTask t, Robot r)  {
		return TaskProbabilityUtil.assessRobot(t, r);
    }

	/**
	 * Gets a collection of Tasks for any Vehicle maintenance that is required.
	 * 
	 * @param settlement Settlement to scan for vehicles
	 */
	@Override
	public List<SettlementTask> getSettlementTasks(Settlement settlement) {
		List<SettlementTask> tasks = new ArrayList<>();

		boolean insideTasks = getGarageSpaces(settlement) > 0;

		for (Vehicle vehicle : getAllVehicleCandidates(settlement, false)) {
			RatingScore score = MaintainBuildingMeta.scoreMaintenance(vehicle);

			// Vehicle in need of maintenance
			if (score.getScore() > 0) {
				tasks.add(new VehicleMaintenanceJob(this, vehicle, !insideTasks, score));
			}
		}

		return tasks;
	}
	
	/**
	 * Gets all ground vehicles requiring maintenance. Candidate list be filtered
	 * for just outside Vehicles.
	 * 
	 * @param home Settlement checking.
	 * @param mustBeOutside
	 * @return collection of ground vehicles available for maintenance.
	 */
	private static List<Vehicle> getAllVehicleCandidates(Settlement home, boolean mustBeOutside) {
		// Vehicle must not be reserved for Mission nor maintenance
		return home.getParkedVehicles().stream()
			.filter(v -> (!v.isReserved()
						&& (!mustBeOutside || !v.isInAGarage())))
			.collect(Collectors.toList());
	}

	/**
	 * Counts the number of available garages spaces in a Settlement.
	 * 
	 * @param settlement Location to check.
	 */
	public static int getGarageSpaces(Settlement settlement) {
		int garageSpaces = 0;
		for(Building j : settlement.getBuildingManager().getBuildingSet(FunctionType.VEHICLE_MAINTENANCE)) {
			VehicleMaintenance garage = j.getVehicleParking();
			
			garageSpaces += (garage.getAvailableCapacity() + garage.getAvailableFlyerCapacity());
		}
		return garageSpaces;
	}
}
