/*
 * Mars Simulation Project
 * RestingMedicalRecoveryMeta.java
 * @date 2021-12-22
 * @author Scott Davis
 */
package org.mars_sim.msp.core.person.ai.task.meta;

import java.util.Iterator;

import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.ai.task.RestingMedicalRecovery;
import org.mars_sim.msp.core.person.ai.task.util.FactoryMetaTask;
import org.mars_sim.msp.core.person.ai.task.util.Task;
import org.mars_sim.msp.core.person.health.HealthProblem;
import org.mars_sim.msp.core.structure.building.Building;
import org.mars_sim.msp.core.structure.building.function.FunctionType;
import org.mars_sim.msp.core.structure.building.function.MedicalCare;
import org.mars_sim.msp.core.vehicle.Rover;
import org.mars_sim.msp.core.vehicle.SickBay;
import org.mars_sim.msp.core.vehicle.VehicleType;
import org.mars_sim.tools.Msg;

/**
 * Meta task for the RestingMedicalRecoveryMeta task.
 */
public class RestingMedicalRecoveryMeta extends FactoryMetaTask {

    /** Task name */
    private static final String NAME = Msg.getString(
            "Task.description.restingMedicalRecovery"); //$NON-NLS-1$

    public RestingMedicalRecoveryMeta() {
		super(NAME, WorkerType.PERSON, TaskScope.ANY_HOUR);
	}

    @Override
    public Task constructInstance(Person person) {
        return new RestingMedicalRecovery(person);
    }

    @Override
    public double getProbability(Person person) {

        double result = 0D;

        if (person.isOutside())
        	return 0;

        // Check if person has a health problem that requires bed rest for recovery.
        boolean bedRestNeeded = false;
        Iterator<HealthProblem> i = person.getPhysicalCondition().getProblems().iterator();
        while (i.hasNext()) {
            HealthProblem problem = i.next();
            if (problem.getRecovering() && problem.requiresBedRest()) {
                bedRestNeeded = true;
            }
        }

        if (bedRestNeeded) {
            result = 500D;

            int hunger = (int) person.getPhysicalCondition().getHunger();
            result = result - (hunger - 333) / 3.0;

            // Determine if any available medical aids can be used for bed rest.
            if (hasUsefulMedicalAids(person)) {
                result+= 100D;
            }

	        double pref = person.getPreference().getPreferenceScore(this);

	        if (pref > 0)
	        	result = result + pref * 10;

            if (result < 0) result = 0;
        }

        return result;
    }

    /**
     * Checks if there is a useful medical aid at person's location for bed rest.
     * @param person the person.
     * @return true if useful medical aid.
     */
    private boolean hasUsefulMedicalAids(Person person) {

        boolean result = false;

        if (person.isInSettlement()) {
            result = hasUsefulMedicalAidsAtSettlement(person);
        }
        else if (person.isInVehicle()) {
            result = hasUsefulMedicalAidsInVehicle(person);
        }

        return result;
    }

    /**
     * Checks if there is a useful medical aid at person's settlement for bed rest.
     * @param person the person.
     * @return true if useful medical aid.
     */
    private boolean hasUsefulMedicalAidsAtSettlement(Person person) {

        boolean result = false;

        // Check all medical care buildings.
        Iterator<Building> i = person.getSettlement().getBuildingManager().getBuildingSet(
                FunctionType.MEDICAL_CARE).iterator();
        while (i.hasNext() && !result) {
            Building building = i.next();

            // Check if building currently has a malfunction.
            boolean malfunction = building.getMalfunctionManager().hasMalfunction();

            // Check if building has enough bed space.
            MedicalCare medicalCare = building.getMedical();
            int numPatients = medicalCare.getPatientNum();
            int numBeds = medicalCare.getSickBedNum();
            boolean enoughBedSpace = (numPatients < numBeds);

            if (!malfunction && enoughBedSpace) {
                result = true;
            }
        }

        return result;
    }

    /**
     * Checks if there is a useful medical aid in person's vehicle for bed rest.
     * @param person the person.
     * @return true if useful medical aid.
     */
    private boolean hasUsefulMedicalAidsInVehicle(Person person) {

        boolean result = false;

        if (VehicleType.isRover(person.getVehicle().getVehicleType())) {
            Rover rover = (Rover) person.getVehicle();
            if (rover.hasSickBay()) {
                SickBay sickBay = rover.getSickBay();
                int numPatients = sickBay.getPatientNum();
                int numBeds = sickBay.getSickBedNum();
                if (numPatients < numBeds) {
                    result = true;
                }
            }
        }

        return result;
    }
}
