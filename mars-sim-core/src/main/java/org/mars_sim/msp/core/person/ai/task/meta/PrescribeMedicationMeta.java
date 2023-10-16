/**
 * Mars Simulation Project
 * PrescribeMedicationMeta.java
 * @date 2021-12-22
 * @author Scott Davis
 */
package org.mars_sim.msp.core.person.ai.task.meta;

import java.util.Collection;
import java.util.Iterator;

import org.mars_sim.msp.core.Unit;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.PhysicalCondition;
import org.mars_sim.msp.core.person.ai.job.util.JobType;
import org.mars_sim.msp.core.person.ai.task.PrescribeMedication;
import org.mars_sim.msp.core.person.ai.task.util.FactoryMetaTask;
import org.mars_sim.msp.core.person.ai.task.util.Task;
import org.mars_sim.msp.core.person.health.AnxietyMedication;
import org.mars_sim.msp.core.person.health.RadiationExposure;
import org.mars_sim.msp.core.person.health.RadioProtectiveAgent;
import org.mars_sim.msp.core.robot.Robot;
import org.mars_sim.msp.core.robot.RobotType;
import org.mars_sim.msp.core.vehicle.Crewable;
import org.mars_sim.msp.core.vehicle.Rover;
import org.mars_sim.msp.core.vehicle.Vehicle;
import org.mars_sim.tools.Msg;

/**
 * Meta task for the PrescribeMedication task.
 */
public class PrescribeMedicationMeta extends FactoryMetaTask {

    /** Task name */
    private static final String NAME = Msg.getString(
            "Task.description.prescribeMedication"); //$NON-NLS-1$

    private int numPatients;
    
    public PrescribeMedicationMeta() {
		super(NAME, WorkerType.BOTH, TaskScope.ANY_HOUR);
		
		setPreferredJob(JobType.MEDICS);

        addPreferredRobot(RobotType.MEDICBOT);
	}
    
    @Override
    public Task constructInstance(Person person) {
        return new PrescribeMedication(person);
    }

    @Override
    public Task constructInstance(Robot robot) {
        return new PrescribeMedication(robot);
    }

    @Override
    public double getProbability(Person person) {

        double result = 0D;

        if (person.isOutside())
        	return 0;	
        
        Person patient = determinePatients(person);
        if (patient == null || numPatients == 0) {
        	return 0;
        }
        	
        JobType job = person.getMind().getJob();
        
        if (job == JobType.DOCTOR) {
            result = numPatients * 300D;
        }
        
        else {
        	boolean hasDoctor = hasADoctor(patient);
            if (hasDoctor) {
            	return 0;
            }
            else {
                result = numPatients * 150D;
            }
        }
            
        double pref = person.getPreference().getPreferenceScore(this);
        
        if (pref > 0)
        	result = result * 3D;
        
        if (result < 0) result = 0;
        
        // Effort-driven task modifier.
        result *= person.getPerformanceRating();

        return result;
    }

    public boolean hasADoctor(Person patient) {
    	Collection<Person> list = null;
        if (patient.isInSettlement()) {
            list = patient.getSettlement().getIndoorPeople();

        }
        else if (patient.isInVehicle()) {
        	Rover rover = (Rover)patient.getContainerUnit();
        	list = rover.getCrew();
        	
        }
        
        if (list != null) {
	        for (Person person : list) {
	        	JobType job = person.getMind().getJob();
	        	if (job == JobType.DOCTOR)
	        		return true;
	        }
        }
        return false;
    }
    
    @Override
    public double getProbability(Robot robot) {

        double result = 0D;

        if (robot.isOutside())
        	return 0;
        
        // Determine patient needing medication.
        Person patient = determinePatients(robot);
        if (patient == null || numPatients == 0) {
            return 0;
        }

        else {//if (patient != null) {
            result = numPatients * 100D;             
        }

        // Effort-driven task modifier.
        result *= robot.getPerformanceRating();

        return result;
    }


	public Person determinePatients(Unit doctor) {
		Person patient = null;
        Person p = null;
        Robot r = null;
        if (doctor instanceof Person)
        	p = (Person) doctor;
        else
        	r = (Robot) doctor;
        
        // Get possible patient list.
        // Note: Doctor can also prescribe medication for himself.
        Collection<Person> patientList = null;
        
        if (p != null) {
	        if (p.isInSettlement()) {
	            patientList = p.getSettlement().getIndoorPeople();
	        }
	        else if (p.isInVehicle()) {
	            Vehicle vehicle = p.getVehicle();
	            if (vehicle instanceof Crewable) {
	                Crewable crewVehicle = (Crewable) vehicle;
	                patientList = crewVehicle.getCrew();
	            }
	        }
        }
        
        else if (r != null) {
	        if (r.isInSettlement()) {
	            patientList = r.getSettlement().getIndoorPeople();
	        }
	        else if (r.isInVehicle()) {
	            Vehicle vehicle = r.getVehicle();
	            if (vehicle instanceof Crewable) {
	                Crewable crewVehicle = (Crewable) vehicle;
	                patientList = crewVehicle.getCrew();
	            }
	        }
        }

        // Determine patient.
        if (patientList != null) {
            Iterator<Person> i = patientList.iterator();
            while (i.hasNext()) {
                Person person = i.next();
                PhysicalCondition condition = person.getPhysicalCondition();
                RadiationExposure exposure = condition.getRadiationExposure();
                if (!condition.isDead()) {
                	if (condition.isStressedOut()) {
                        // Only prescribing anti-stress medication at the moment.
                        if (!condition.hasMedication(AnxietyMedication.NAME)) {
                        	patient = person;
                            numPatients++;
                        }
                	}
                	else if (exposure.isSick()) {
                        if (!condition.hasMedication(RadioProtectiveAgent.NAME)) {
                        	patient = person;
                        	numPatients++;
                        }
                	}
                }
            }
        }

        return patient;
	}

}
