/**
 * Mars Simulation Project
 * RestingMedicalRecovery.java
 * @version 3.2.0 2021-06-20
 * @author Scott Davis
 */
package com.mars_sim.core.person.ai.task;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;

import com.mars_sim.core.logging.SimLogger;
import com.mars_sim.core.person.Person;
import com.mars_sim.core.person.ai.task.util.Task;
import com.mars_sim.core.person.ai.task.util.TaskPhase;
import com.mars_sim.core.person.health.HealthProblem;
import com.mars_sim.core.person.health.MedicalAid;
import com.mars_sim.core.structure.building.Building;
import com.mars_sim.core.structure.building.function.FunctionType;
import com.mars_sim.core.structure.building.function.MedicalCare;
import com.mars_sim.core.vehicle.Rover;
import com.mars_sim.core.vehicle.SickBay;
import com.mars_sim.core.vehicle.Vehicle;
import com.mars_sim.tools.Msg;
import com.mars_sim.tools.util.RandomUtil;

/**
 * A task for resting at a medical station bed to recover from a health problem
 * which requires bed rest.
 */
public class RestingMedicalRecovery extends Task {

    /** default serial id. */
    private static final long serialVersionUID = 1L;

    /** default logger. */
    private static SimLogger logger = SimLogger.getLogger(RestingMedicalRecovery.class.getName());

    /** Task name */
    private static final String NAME = Msg.getString(
            "Task.description.restingMedicalRecovery"); //$NON-NLS-1$

    /** Task phases. */
    private static final TaskPhase RESTING = new TaskPhase(Msg.getString(
            "Task.phase.restingInBed")); //$NON-NLS-1$
	
    /** Maximum resting duration (millisols) */
    private static final double RESTING_DURATION = 300D;

    /** The stress modified per millisol. */
    private static final double STRESS_MODIFIER = -2D;

    // Data members
    private MedicalAid medicalAid;
    private double restingTime;

    /**
     * Constructor.
     * @param person the person to perform the task
     */
    public RestingMedicalRecovery(Person person) {
        super(NAME, person, false, false, STRESS_MODIFIER, null, 10D);

        // Initialize data members.
        restingTime = 0D;

        // Choose available medical aid to rest at.
        medicalAid = determineMedicalAid();

        if (medicalAid != null) {

            if (medicalAid instanceof MedicalCare) {
                // Walk to medical care building.
                MedicalCare medicalCare = (MedicalCare) medicalAid;

                // Walk to medical care building.
                Building b = medicalCare.getBuilding();
                if (b != null)
                	walkToActivitySpotInBuilding(b, FunctionType.MEDICAL_CARE, false);
                //else
                //	endTask();
                
            }
            else if (medicalAid instanceof SickBay) {
                // Walk to medical activity spot in rover.
                Vehicle vehicle = ((SickBay) medicalAid).getVehicle();
                if (vehicle instanceof Rover) {

                    // Walk to rover sick bay activity spot.
                    walkToSickBayActivitySpotInRover((Rover) vehicle, false);
                }
            }
        }
        else {
      		logger.severe(worker, "Can't find any medical aid.");
      		
            endTask();
        }

        // Initialize phase.
        addPhase(RESTING);
        setPhase(RESTING);
    }

    /**
     * Determines a medical aid to rest at.
     * @return medical aid or null if none found.
     */
    private MedicalAid determineMedicalAid() {

        MedicalAid result = null;

        if (person.isInSettlement()) {
            result = determineMedicalAidAtSettlement();
        }
        else if (person.isInVehicle()) {
            result = determineMedicalAidInVehicle();
        }

        return result;
    }

    /**
     * Determines a medical aid at a settlement.
     * @return medical aid.
     */
    private MedicalAid determineMedicalAidAtSettlement() {

        MedicalAid result = null;

        List<MedicalAid> goodMedicalAids = new ArrayList<>();

        // Check all medical care buildings.
        Iterator<Building> i = person.getSettlement().getBuildingManager().getBuildingSet(
                FunctionType.MEDICAL_CARE).iterator();
        while (i.hasNext()) {
            Building building = i.next();

            // Check if building currently has a malfunction.
            boolean malfunction = building.getMalfunctionManager().hasMalfunction();

            // Check if enough beds for patient.
            MedicalCare medicalCare = building.getMedical();
            int numPatients = medicalCare.getPatientNum();
            int numBeds = medicalCare.getSickBedNum();
            if ((numPatients < numBeds) && !malfunction) {
                goodMedicalAids.add(medicalCare);
            }
        }

        // Randomly select an valid medical care building.
        if (goodMedicalAids.size() > 0) {
            int index = RandomUtil.getRandomInt(goodMedicalAids.size() - 1);
            result = goodMedicalAids.get(index);
        }

        return result;
    }

    /**
     * Determines a medical aid in a vehicle.
     * @return medical aid.
     */
    private MedicalAid determineMedicalAidInVehicle() {

        MedicalAid result = null;

        if (person.getVehicle() instanceof Rover) {
            Rover rover = (Rover) person.getVehicle();
            if (rover.hasSickBay()) {
                SickBay sickBay = rover.getSickBay();
                int numPatients = sickBay.getPatientNum();
                int numBeds = sickBay.getSickBedNum();
                if (numPatients < numBeds) {
                    result = sickBay;
                }
            }
        }

        return result;
    }

    @Override
    protected double performMappedPhase(double time) {
        if (getPhase() == null) {
            throw new IllegalArgumentException("Task phase is null");
        }
        else if (RESTING.equals(getPhase())) {
            return restingPhase(time);
        }
        else {
            return time;
        }
    }

    /**
     * Performs the resting phase of the task.
     * @param time the amount of time (millisol) to perform the phase.
     * @return the amount of time (millisol) left over after performing the phase.
     */
    private double restingPhase(double time) {

        double remainingTime = 0D;

        // Add person to medical aid resting recovery people if not already on it.
        if (!medicalAid.getRestingRecoveryPeople().contains(person)) {
            medicalAid.startRestingRecovery(person);
        }

        // Check if exceeding maximum bed rest duration.
        boolean timeOver = false;
        if ((restingTime + time) >= RESTING_DURATION) {
            remainingTime = (restingTime + time) - RESTING_DURATION;
            time = RESTING_DURATION - restingTime;
            restingTime = RESTING_DURATION;
            timeOver = true;
        }
        else {
            restingTime += time;
        }

        // Add bed rest to all health problems that require it.
        boolean remainingBedRest = false;
        Iterator<HealthProblem> i = person.getPhysicalCondition().getProblems().iterator();
        while (i.hasNext()) {
            HealthProblem problem = i.next();
            if (problem.getRecovering() && problem.requiresBedRest()) {
                problem.addBedRestRecoveryTime(time);
    			logger.log(worker, Level.FINE, 20_000, "Was taking a medical leave and resting");	
                if (!problem.isCured()) {
                    remainingBedRest = true;
                }
            }
        }

        // If person has no more health problems requiring bed rest, end task.
        if (!remainingBedRest) {
			logger.log(worker, Level.FINE, 0, "Ended the medical leave.");
            endTask();
        }

        // Reduce person's fatigue due to bed rest.
        person.getPhysicalCondition().reduceFatigue(3D * time);

        // If out of bed rest time, end task.
        if (timeOver) {
            endTask();
        }

        return remainingTime;
    }

    /**
     * Stop the resting period for this person if still active
     */
    @Override
    protected void clearDown() {
        // Remove person from medical aid.
        if (medicalAid != null) {

            // Stop resting recovery for person at medical aid.
            if (medicalAid.getRestingRecoveryPeople().contains(person)) {
                medicalAid.stopRestingRecovery(person);
            }
        }
    }
}
