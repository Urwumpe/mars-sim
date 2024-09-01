package com.mars_sim.core.vehicle.task;

import com.mars_sim.core.AbstractMarsSimUnitTest;
import com.mars_sim.core.map.location.LocalPosition;
import com.mars_sim.core.person.ai.SkillType;
import com.mars_sim.core.person.ai.job.util.JobType;
import com.mars_sim.core.resource.ResourceUtil;
import com.mars_sim.core.resource.SuppliesManifest;
import com.mars_sim.core.structure.building.function.FunctionType;

public class LoadVehicleGarageTest extends AbstractMarsSimUnitTest {
    public void testCreateTask() {
        var s = buildSettlement("Vehicle base");
        var g = buildGarage(s.getBuildingManager(), LocalPosition.DEFAULT_POSITION, 0D, 0);

        // Load the vehicle
        var v = buildRover(s, "rover1", new LocalPosition(10, 10));
        var p = buildPerson("Mechanic", s, JobType.TECHNICIAN, g.getBuilding(), FunctionType.VEHICLE_MAINTENANCE);
        p.getSkillManager().addNewSkill(SkillType.MECHANICS, 10); // Skilled

        // Create a loading plan and preload Settlement
        var resources = new SuppliesManifest();
        resources.addAmount(ResourceUtil.oxygenID, 10D, true);
        resources.addAmount(ResourceUtil.waterID, 10D, true);
        resources.addAmount(ResourceUtil.foodID, 10D, true);
        LoadControllerTest.loadSettlement(s, resources);
        LoadingController lc = new LoadingController(s, v, resources);

        var task = new LoadVehicleGarage(p, lc);
        assertFalse("Task created", task.isDone()); 

        // Do maintenance and advance to return
        executeTaskUntilPhase(p, task, 1000);
        assertGreaterThan("Final stored mass", 0D, v.getStoredMass());

        // Return to base
        assertTrue("Task completed", task.isDone()); 
    }
}
