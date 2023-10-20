/*
 * Mars Simulation Project
 * MetaTaskUtilTest.java
 * @date 2022-11-15
 * @author Barry Evans
 */

package com.mars_sim.core.person.ai.task;

import com.mars_sim.core.AbstractMarsSimUnitTest;
import com.mars_sim.core.malfunction.Malfunction;
import com.mars_sim.core.malfunction.MalfunctionManager;
import com.mars_sim.core.malfunction.MalfunctionMeta;
import com.mars_sim.core.malfunction.MalfunctionMeta.EffortSpec;
import com.mars_sim.core.malfunction.MalfunctionRepairWork;
import com.mars_sim.core.person.Person;
import com.mars_sim.core.person.ai.task.util.MetaTask;
import com.mars_sim.core.person.ai.task.util.MetaTaskUtil;
import com.mars_sim.core.person.ai.task.util.Task;
import com.mars_sim.core.structure.Settlement;
import com.mars_sim.core.structure.building.Building;
import com.mars_sim.core.vehicle.Vehicle;
import com.mars_sim.mapdata.location.LocalPosition;

/**
 * Tests the ability of a person to initiate various meta tasks.
 */
public class MetaTaskUtilTest
extends AbstractMarsSimUnitTest {

	private static final LocalPosition POS = new LocalPosition(0, 0);
	private Settlement settlement;
	private Person person;

	@Override
	public void setUp() {
		super.setUp();

		MetaTaskUtil.initializeMetaTasks();

		settlement = buildSettlement();

		person = buildPerson("Test Body", settlement);

	}

	public void testEatDrink() throws Exception {
		EatDrink eatDrink = new EatDrink(person);
		assertMetaTask(eatDrink, "Eating");
	}

	public void testLoadVehicleGarage() throws Exception {
		LoadVehicleGarage evaLoad = new LoadVehicleGarage(person, buildMission(settlement, person));
		assertMetaTask(evaLoad, "Loading Vehicle");
	}

	public void testLoadVehicleEVA() throws Exception {
		LoadVehicleEVA evaLoad = new LoadVehicleEVA(person, buildMission(settlement, person));
		assertMetaTask(evaLoad, "Loading Vehicle");
	}

	public void testUnloadVehicleGarage() throws Exception {
		Vehicle vehicle = buildRover(settlement, "loader", null);

		UnloadVehicleGarage evaLoad = new UnloadVehicleGarage(person, vehicle);
		assertMetaTask(evaLoad, "Unloading Vehicle");
	}


	public void testUnloadVehicleEVA() throws Exception {
		Vehicle vehicle = buildRover(settlement, "loader", null);

		UnloadVehicleEVA evaLoad = new UnloadVehicleEVA(person, vehicle);
		assertMetaTask(evaLoad, "Unloading Vehicle");
	}

	public void testRepairInside() throws Exception {
		Building garage = buildBuilding(settlement.getBuildingManager(), POS, 0D, 1);
		Malfunction m = createMalfunction(garage, person, MalfunctionRepairWork.INSIDE);

		Task evaLoad = new RepairInsideMalfunction(person, garage, m);
		assertMetaTask(evaLoad, "Repairing Malfunction");
	}

	public void testRepairEVA() throws Exception {
		Building garage = buildBuilding(settlement.getBuildingManager(), POS, 0D, 1);

		Malfunction m = createMalfunction(garage, person, MalfunctionRepairWork.EVA);
		Task evaLoad = new RepairEVAMalfunction(person, garage, m);
		assertMetaTask(evaLoad, "Repairing Malfunction");
	}

	private Malfunction createMalfunction(Building b, Person p, MalfunctionRepairWork work) {
		MalfunctionManager mm = b.getMalfunctionManager(); 
//		mm.getMaintenanceParts();
		for(MalfunctionMeta mMeta : simConfig.getMalfunctionConfiguration().getMalfunctionList()) {
			EffortSpec w = mMeta.getRepairEffort().get(work);
			if ((w != null) && (w.getWorkTime() > 0D)) {
				return mm.triggerMalfunction(mMeta, false, p);
			}
		}

		return null;
	}

	public void testMaintainBuilding() throws Exception {
		Building garage = buildBuilding(settlement.getBuildingManager(), POS, 0D, 1);
//		garage.getMalfunctionManager().getMaintenanceParts();

		MaintainBuilding evaLoad = new MaintainBuilding(person, garage);
		assertMetaTask(evaLoad, "Maintaining Building");
	}

	public void testMaintainEVABuilding() throws Exception {
		Building garage = buildBuilding(settlement.getBuildingManager(), POS, 0D, 1);
//		garage.getMalfunctionManager().getMaintenanceParts();

		MaintainBuildingEVA evaLoad = new MaintainBuildingEVA(person, garage);
		assertMetaTask(evaLoad, "Maintaining Building");
	}


	/**
	 * Find the associated MetatTask from a specific Task and make sure it is the
	 * expected one.
	 * 
	 * @param task Seed for search
	 * @param name Expected MetaTask name
	 */
	private void assertMetaTask(Task task, String name) {
		MetaTask mt = MetaTaskUtil.getMetaTypeFromTask(task);

		assertNotNull("No MetaTask for " + task.getName(), mt);
		assertEquals("Metatask name for " + task.getName(), name, mt.getName());
	}
}