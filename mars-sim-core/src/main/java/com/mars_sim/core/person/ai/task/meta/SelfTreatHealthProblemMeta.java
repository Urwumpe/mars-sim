/*
 * Mars Simulation Project
 * SelfTreatMedicalProblemMeta.java
 * @date 2021-12-05
 * @author Scott Davis
 */
package com.mars_sim.core.person.ai.task.meta;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.mars_sim.core.person.Person;
import com.mars_sim.core.person.ai.SkillType;
import com.mars_sim.core.person.ai.task.SelfTreatHealthProblem;
import com.mars_sim.core.person.ai.task.util.FactoryMetaTask;
import com.mars_sim.core.person.ai.task.util.Task;
import com.mars_sim.core.person.health.HealthProblem;
import com.mars_sim.core.person.health.Treatment;
import com.mars_sim.core.structure.building.Building;
import com.mars_sim.core.structure.building.function.FunctionType;
import com.mars_sim.core.structure.building.function.MedicalCare;
import com.mars_sim.core.vehicle.Rover;
import com.mars_sim.core.vehicle.SickBay;
import com.mars_sim.core.vehicle.VehicleType;
import com.mars_sim.tools.Msg;

/**
 * Meta task for the SelfTreatHealthProblem task.
 */
public class SelfTreatHealthProblemMeta extends FactoryMetaTask {

	private static final double VALUE = 1000.0;

    /** Task name */
    private static final String NAME = Msg.getString(
            "Task.description.selfTreatHealthProblem"); //$NON-NLS-1$

    public SelfTreatHealthProblemMeta() {
		super(NAME, WorkerType.PERSON, TaskScope.ANY_HOUR);
	}


    @Override
    public Task constructInstance(Person person) {
        return new SelfTreatHealthProblem(person);
    }

    @Override
    public double getProbability(Person person) {

        double result = 0D;

        if (person.isInside()) {
	        // Check if person has health problems that can be self-treated.
        	int size = getSelfTreatableHealthProblems(person).size();

	        boolean hasSelfTreatableProblems = (size > 0);

	        // Check if person has available medical aids.
	        boolean hasAvailableMedicalAids = hasAvailableMedicalAids(person);

	        if (hasSelfTreatableProblems && hasAvailableMedicalAids) {
	            result = VALUE * size;
	        }

	        double pref = person.getPreference().getPreferenceScore(this);

	        if (pref > 0)
	        	result = result * 3D;

	        // Effort-driven task modifier.
	        result *= person.getPerformanceRating();

	        if (result < 0) result = 0;

        }

        return result;
    }

    /**
     * Gets a list of health problems the person can self-treat.
     * @param person the person.
     * @return list of health problems (may be empty).
     */
    private List<HealthProblem> getSelfTreatableHealthProblems(Person person) {

        List<HealthProblem> result = new ArrayList<>();

        Iterator<HealthProblem> i = person.getPhysicalCondition().getProblems().iterator();
        while (i.hasNext()) {
            HealthProblem problem = i.next();
            if (problem.isDegrading()) {
                Treatment treatment = problem.getComplaint().getRecoveryTreatment();
                if (treatment != null) {
                    boolean selfTreatable = treatment.getSelfAdminister();
                    int skill = person.getSkillManager().getEffectiveSkillLevel(SkillType.MEDICINE);
                    int requiredSkill = treatment.getSkill();
                    if (selfTreatable && (skill >= requiredSkill)) {
                        result.add(problem);
                    }
                }
            }
        }

        return result;
    }

    /**
     * Checks if a person has any available local medical aids for self treating health problems.
     * @param person the person.
     * @return true if available medical aids.
     */
    private boolean hasAvailableMedicalAids(Person person) {

        boolean result = false;

        if (person.isInSettlement()) {
            result = hasAvailableMedicalAidsAtSettlement(person);
        }
        else if (person.isInVehicle()) {
            result = hasAvailableMedicalAidInVehicle(person);
        }

        return result;
    }

    /**
     * Checks if a person has any available medical aids at a settlement.
     * @param person the person.
     * @return true if available medical aids.
     */
    private boolean hasAvailableMedicalAidsAtSettlement(Person person) {

        boolean result = false;

        // Check all medical care buildings.
        Iterator<Building> i = person.getSettlement().getBuildingManager().getBuildingSet(
                FunctionType.MEDICAL_CARE).iterator();
        while (i.hasNext() && !result) {
            Building building = i.next();

            // Check if building currently has a malfunction.
            boolean malfunction = building.getMalfunctionManager().hasMalfunction();

            // Check if enough beds for patient.
            MedicalCare medicalCare = building.getMedical();
            int numPatients = medicalCare.getPatientNum();
            int numBeds = medicalCare.getSickBedNum();

            if ((numPatients < numBeds) && !malfunction) {

                // Check if any of person's self-treatable health problems can be treated in building.
                boolean canTreat = false;
                Iterator<HealthProblem> j = getSelfTreatableHealthProblems(person).iterator();
                while (j.hasNext() && !canTreat) {
                    HealthProblem problem = j.next();
                    if (medicalCare.canTreatProblem(problem)) {
                        canTreat = true;
                    }
                }

                if (canTreat) {
                    result = true;
                }
            }
        }

        return result;
    }

    /**
     * Checks if a person has an available medical aid in a vehicle.
     * @param person the person.
     * @return true if available medical aids.
     */
    private boolean hasAvailableMedicalAidInVehicle(Person person) {

        boolean result = false;

        if (VehicleType.isRover(person.getVehicle().getVehicleType())) {
            Rover rover = (Rover) person.getVehicle();
            if (rover.hasSickBay()) {
                SickBay sickBay = rover.getSickBay();

                // Check if enough beds for patient.
                int numPatients = sickBay.getPatientNum();
                int numBeds = sickBay.getSickBedNum();

                if (numPatients < numBeds) {

                    // Check if any of person's self-treatable health problems can be treated in sick bay.
                    boolean canTreat = false;
                    Iterator<HealthProblem> j = getSelfTreatableHealthProblems(person).iterator();
                    while (j.hasNext() && !canTreat) {
                        HealthProblem problem = j.next();
                        if (sickBay.canTreatProblem(problem)) {
                            canTreat = true;
                        }
                    }

                    if (canTreat) {
                        result = true;
                    }
                }
            }
        }

        return result;
    }
}
