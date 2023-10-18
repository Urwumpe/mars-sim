/**
 * Mars Simulation Project
 * SelfTreatHealthProblem.java
 * @date 2021-12-22
 * @author Scott Davis
 */
package com.mars_sim.core.person.ai.task;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;

import com.mars_sim.core.logging.SimLogger;
import com.mars_sim.core.malfunction.Malfunctionable;
import com.mars_sim.core.person.EventType;
import com.mars_sim.core.person.Person;
import com.mars_sim.core.person.ai.SkillType;
import com.mars_sim.core.person.ai.task.util.Task;
import com.mars_sim.core.person.ai.task.util.TaskEvent;
import com.mars_sim.core.person.ai.task.util.TaskPhase;
import com.mars_sim.core.person.health.HealthProblem;
import com.mars_sim.core.person.health.MedicalAid;
import com.mars_sim.core.person.health.Treatment;
import com.mars_sim.core.structure.building.Building;
import com.mars_sim.core.structure.building.function.FunctionType;
import com.mars_sim.core.structure.building.function.MedicalCare;
import com.mars_sim.core.vehicle.Rover;
import com.mars_sim.core.vehicle.SickBay;
import com.mars_sim.core.vehicle.Vehicle;
import com.mars_sim.core.vehicle.VehicleType;
import com.mars_sim.tools.Msg;
import com.mars_sim.tools.util.RandomUtil;

/**
 * A task for performing a medical self-treatment at a medical station.
 */
public class SelfTreatHealthProblem extends Task {

    /** default serial id. */
    private static final long serialVersionUID = 1L;

    /** default logger. */
    private static SimLogger logger = SimLogger.getLogger(SelfTreatHealthProblem.class.getName());
    
    /** Task name */
    private static final String NAME = Msg.getString(
            "Task.description.selfTreatHealthProblem"); //$NON-NLS-1$

    /** Task phases. */
    private static final TaskPhase TREATMENT = new TaskPhase(Msg.getString(
            "Task.phase.treatingHealthProblem")); //$NON-NLS-1$

    /** The stress modified per millisol. */
    private static final double STRESS_MODIFIER = .5D;

    // Data members.
    private double duration;
    private double treatmentTime;
    
    private MedicalAid medicalAid;
    private HealthProblem healthProblem;

    /**
     * Constructor.
     * @param person the person to perform the task
     */
    public SelfTreatHealthProblem(Person person) {
        super(NAME, person, false, true, STRESS_MODIFIER, SkillType.MEDICINE, 25D);

        treatmentTime = 0D;

        // Choose available medical aid for treatment.
        medicalAid = determineMedicalAid();

        if (medicalAid != null) {

            // Determine health problem to treat.
            healthProblem = determineHealthProblemToTreat();
            if (healthProblem != null) {

                // Get the person's medical skill.
                int skill = person.getSkillManager().getEffectiveSkillLevel(SkillType.MEDICINE);

                // Determine medical treatment.
                Treatment treatment = healthProblem.getIllness().getRecoveryTreatment();
                if (treatment != null) {
                    duration = treatment.getAdjustedDuration(skill);
                    setStressModifier(STRESS_MODIFIER * treatment.getSkill());
                }
                else {
            		logger.warning(person, healthProblem + " does not have treatment.");
                    endTask();
                }
            }
            else {
            	logger.warning(person, "Could not self-treat a health problem at " + medicalAid);
                endTask();
            }

            // Walk to medical aid.
            if (medicalAid instanceof MedicalCare) {
                // Walk to medical care building.
                MedicalCare medicalCare = (MedicalCare) medicalAid;

                // Walk to medical care building.
                walkToTaskSpecificActivitySpotInBuilding(medicalCare.getBuilding(), FunctionType.MEDICAL_CARE, false);
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
            logger.severe(person, "Medical aid could not be determined.");
            endTask();
        }

        // Initialize phase.
        addPhase(TREATMENT);
        setPhase(TREATMENT);
    }

    /**
     * Determine a local medical aid to use for self-treating a health problem.
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
     * Determine a medical aid at a settlement to use for self-treating a health problem.
     * @return medical aid or null if none found.
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

                // Check if any of person's self-treatable health problems can be treated in building.
                boolean canTreat = false;
                Iterator<HealthProblem> j = getSelfTreatableHealthProblems().iterator();
                while (j.hasNext() && !canTreat) {
                    HealthProblem problem = j.next();
                    if (medicalCare.canTreatProblem(problem)) {
                        canTreat = true;
                    }
                }

                if (canTreat) {
                    goodMedicalAids.add(medicalCare);
                }
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
     * Determine a medical aid on a vehicle to use for self-treating a health problem.
     * @return medical aid or null if none found.
     */
    private MedicalAid determineMedicalAidInVehicle() {

        MedicalAid result = null;

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
                    Iterator<HealthProblem> j = getSelfTreatableHealthProblems().iterator();
                    while (j.hasNext() && !canTreat) {
                        HealthProblem problem = j.next();
                        if (sickBay.canTreatProblem(problem)) {
                            canTreat = true;
                        }
                    }

                    if (canTreat) {
                        result = sickBay;
                    }
                }
            }
        }

        return result;
    }

    /**
     * Determines the health problem to self-treat.
     * @return health problem or null if none found.
     */
    private HealthProblem determineHealthProblemToTreat() {

        HealthProblem result = null;

        if (medicalAid != null) {

            // Choose most severe health problem that can be self-treated.
            int highestSeverity = Integer.MIN_VALUE;
            Iterator<HealthProblem> i = getSelfTreatableHealthProblems().iterator();
            while (i.hasNext()) {
                HealthProblem problem = i.next();
                if (medicalAid.canTreatProblem(problem)) {
                    int severity = problem.getIllness().getSeriousness();
                    if (severity > highestSeverity) {
                        result = problem;
                        highestSeverity = severity;
                    }
                }
            }
        }
        else {
            logger.severe(person, "Medical aid is null.");
        }

        return result;
    }

    /**
     * Gets a list of health problems the person can self-treat.
     * @return list of health problems (may be empty).
     */
    private List<HealthProblem> getSelfTreatableHealthProblems() {

        List<HealthProblem> result = new ArrayList<>();

        Iterator<HealthProblem> i = person.getPhysicalCondition().getProblems().iterator();
        while (i.hasNext()) {
            HealthProblem problem = i.next();
            if (problem.isDegrading()) {
                Treatment treatment = problem.getIllness().getRecoveryTreatment();
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

    @Override
    protected double performMappedPhase(double time) {
        if (getPhase() == null) {
            throw new IllegalArgumentException("Task phase is null");
        }
        else if (TREATMENT.equals(getPhase())) {
            return treatmentPhase(time);
        }
        else {
            return time;
        }
    }

    /**
     * Performs the treatment phase of the task.
     * @param time the amount of time (millisol) to perform the phase.
     * @return the amount of time (millisol) left over after performing the phase.
     */
    private double treatmentPhase(double time) {

        double timeLeft = 0D;

        // If medical aid has malfunction, end task.
        if (getMalfunctionable().getMalfunctionManager().hasMalfunction()) {
            endTask();
        }

        if (isDone()) {
			endTask();
            return time;
        }

        // Start treatment if not already started.
        if (!medicalAid.getProblemsBeingTreated().contains(healthProblem)) {
            medicalAid.requestTreatment(healthProblem);
            medicalAid.startTreatment(healthProblem, duration);

        	logger.log(person, Level.INFO, 0, "Self-treating " 
        			+ healthProblem.getIllness().getType().getName());

            // Create starting task event if needed.
            if (getCreateEvents()) {
                TaskEvent startingEvent = new TaskEvent(person,
                		this, 
                		person,
                		EventType.TASK_START,
                		NAME
                );
                registerNewEvent(startingEvent);
            }
        }

        // Check for accident in medical aid.
        checkForAccident(getMalfunctionable(), time, 0.005);

        treatmentTime += time;
        if (treatmentTime >= duration) {
            healthProblem.startRecovery();
            timeLeft = treatmentTime - duration;
            endTask();
        }

        // Add experience.
        addExperience(time);

        return timeLeft;
    }

    /**
     * Gets the malfunctionable associated with the medical aid.
     * @return the associated Malfunctionable
     */
    private Malfunctionable getMalfunctionable() {
        Malfunctionable result = null;

        if (medicalAid instanceof SickBay) {
            result = ((SickBay) medicalAid).getVehicle();
        }
        else if (medicalAid instanceof MedicalCare) {
            result = ((MedicalCare) medicalAid).getBuilding();
        }
        else {
            result = (Malfunctionable) medicalAid;
        }

        return result;
    }

 
    /**
     * Stop mediical treatment
     */
    @Override
    protected void clearDown() {
        // Stop treatment.
        if (medicalAid.getProblemsBeingTreated().contains(healthProblem)) {
            medicalAid.stopTreatment(healthProblem);
        }
    }
}
