/**
 * Mars Simulation Project
 * DeathInfo.java
 * @version 2.75 2002-06-08
 * @author Barry Evans
 */

package org.mars_sim.msp.simulation.person.medical;

import org.mars_sim.msp.simulation.*;
import org.mars_sim.msp.simulation.person.*;
import org.mars_sim.msp.simulation.person.ai.*;
import org.mars_sim.msp.simulation.malfunction.*;
import java.util.Iterator;

/**
 * This class represents the status of a Person when death occurs. It records
 * the Complaint that caused the death to occur, the time of death and
 * the Location.
 * The Location is recorded as a dead body may be moved from the place of death.
 * This class is immutable since once Death occurs it is final.
 */
public class DeathInfo implements java.io.Serializable {

    // Data members
    private String timeOfDeath; // The time of death.
    private String illness; // Medical cause of death.
    private String placeOfDeath; // Place of death.
    private Unit containerUnit; // Container unit at death.
    private Coordinates locationOfDeath; // location of death.
    private String mission; // Name of mission at time of death.
    private String missionPhase; // Phase of mission at time of death.
    private String task; // Name of task at time of death.
    private String taskPhase; // Phase of task at time of death.
    private String malfunction; // Name of the most serious local emergency malfunction. 

    /**
     * The construct creates an instance of a DeathInfo class.
     * @param person the dead person
     */
    public DeathInfo(Person person) {

        // Initialize data members
	timeOfDeath = person.getMars().getMasterClock().getMarsClock().getTimeStamp();
	
	Complaint serious = person.getPhysicalCondition().getMostSerious();
	if (serious != null) illness = serious.getName();

	if (person.getLocationSituation().equals(Person.OUTSIDE)) placeOfDeath = "Outside";
	else {
	    containerUnit = person.getContainerUnit();	
	    placeOfDeath = containerUnit.getName();
	}

        locationOfDeath = person.getCoordinates();

	Mind mind = person.getMind();
	if (mind.getMission() != null) {
	    mission = mind.getMission().getName();
	    missionPhase = mind.getMission().getPhase();
	}

	TaskManager taskMgr = mind.getTaskManager();
	if (taskMgr.hasTask()) {
            task = taskMgr.getTaskName();
	    taskPhase = taskMgr.getPhase();
	}

	Iterator i = MalfunctionFactory.getMalfunctionables(person).iterator();
	Malfunction mostSerious = null;
	int severity = 0;
	while (i.hasNext()) {
	    Malfunctionable entity = (Malfunctionable) i.next();
	    MalfunctionManager malfunctionMgr = entity.getMalfunctionManager();
	    if (malfunctionMgr.hasEmergencyMalfunction()) {
                Malfunction m = malfunctionMgr.getMostSeriousEmergencyMalfunction();
		if (m.getSeverity() > severity) {
                    mostSerious = m;
		    severity = m.getSeverity();
		}
            }
        }
	if (mostSerious != null) malfunction = mostSerious.getName();
    }

    /**
     * Get the time death happened.
     * @return formatted time.
     */
    public String getTimeOfDeath() {
        if (timeOfDeath != null) return timeOfDeath;
	else return "";
    }

    /**
     * Gets the place the death happened.  
     * Either the name of the unit the person was in, or 'outside' if
     * the person died on an EVA.
     * @return place of death.
     */
    public String getPlaceOfDeath() {
        if (placeOfDeath != null) return placeOfDeath;
	else return "";
    }

    /** 
     * Gets the container unit at the time of death.
     * Returns null if none.
     * @return container unit
     */
    public Unit getContainerUnit() {
        return containerUnit;
    }

    /**
     * Get the name of the illness that caused the death.
     * @return name of the illness.
     */
    public String getIllness() {
        if (illness != null) return illness;
	else return "";
    }

    /**
     * Gets the location of death.
     * @return coordinates
     */
    public Coordinates getLocationOfDeath() {
        return locationOfDeath;
    }

    /**
     * Gets the mission the person was on at time of death.
     * @return mission name
     */
    public String getMission() {
        if (mission != null) return mission;
	else return "";
    }

    /**
     * Gets the mission phase at time of death.
     * @return mission phase
     */
    public String getMissionPhase() {
        if (missionPhase != null) return missionPhase;
	else return "";
    }

    /**
     * Gets the task the person was doing at time of death.
     * @return task name
     */
    public String getTask() {
        if (task != null) return task;
	else return "";
    }

    /**
     * Gets the task phase at time of death.
     * @return task phase
     */
    public String getTaskPhase() {
        if (taskPhase != null) return taskPhase;
	else return "";
    }

    /**
     * Gets the most serious emergency malfunction
     * local to the person at time of death.
     * @return malfunction name
     */
    public String getMalfunction() {
        if (malfunction != null) return malfunction;
	else return "";
    }
}
