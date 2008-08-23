/**
 * Mars Simulation Project
 * Engineer.java
 * @version 2.85 2008-08-20
 * @author Scott Davis
 */
package org.mars_sim.msp.simulation.person.ai.job;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.mars_sim.msp.simulation.person.NaturalAttributeManager;
import org.mars_sim.msp.simulation.person.Person;
import org.mars_sim.msp.simulation.person.ai.Skill;
import org.mars_sim.msp.simulation.person.ai.mission.RescueSalvageVehicle;
import org.mars_sim.msp.simulation.person.ai.mission.TravelToSettlement;
import org.mars_sim.msp.simulation.person.ai.task.ManufactureGood;
import org.mars_sim.msp.simulation.person.ai.task.ResearchMaterialsScience;
import org.mars_sim.msp.simulation.structure.Settlement;
import org.mars_sim.msp.simulation.structure.building.Building;
import org.mars_sim.msp.simulation.structure.building.BuildingException;
import org.mars_sim.msp.simulation.structure.building.function.Manufacture;

/** 
 * The Engineer class represents an engineer job focusing on repair and maintenance of buildings and vehicles.
 */
public class Engineer extends Job implements Serializable {
    
    	private static String CLASS_NAME = "org.mars_sim.msp.simulation.person.ai.job.Engineer";
	
	private static Logger logger = Logger.getLogger(CLASS_NAME);

	/**
	 * Constructor
	 */
	public Engineer() {
		// Use Job constructor
		super("Engineer");
		
		// Add engineer-related tasks.
		jobTasks.add(ManufactureGood.class);
		jobTasks.add(ResearchMaterialsScience.class);
		
		// Add engineer-related missions.
		jobMissionJoins.add(TravelToSettlement.class);	
		jobMissionStarts.add(RescueSalvageVehicle.class);
		jobMissionJoins.add(RescueSalvageVehicle.class);
	}

	/**
	 * Gets a person's capability to perform this job.
	 * @param person the person to check.
	 * @return capability (min 0.0).
	 */
	public double getCapability(Person person) {
		
		double result = 0D;
		
		int materialsScienceSkill = person.getMind().getSkillManager().getSkillLevel(Skill.MATERIALS_SCIENCE);
		result = materialsScienceSkill;
		
		NaturalAttributeManager attributes = person.getNaturalAttributeManager();
		int academicAptitude = attributes.getAttribute(NaturalAttributeManager.ACADEMIC_APTITUDE);
		int experienceAptitude = attributes.getAttribute(NaturalAttributeManager.EXPERIENCE_APTITUDE);
		double averageAptitude = (academicAptitude + experienceAptitude) / 2D;
		result+= result * ((averageAptitude - 50D) / 100D);
		
		if (person.getPhysicalCondition().hasSeriousMedicalProblems()) result = 0D;
		
		return result;
	}
	
	/**
	 * Gets the base settlement need for this job.
	 * @param settlement the settlement in need.
	 * @return the base need >= 0
	 */
	public double getSettlementNeed(Settlement settlement) {
		
		double result = 0D;
		
		// Add (tech level * process number) for all manufacture buildings.
		List manufactureBuildings = settlement.getBuildingManager().getBuildings(Manufacture.NAME);
		Iterator i = manufactureBuildings.iterator();
		while (i.hasNext()) {
			Building building = (Building) i.next();
			try {
				Manufacture workshop = (Manufacture) building.getFunction(Manufacture.NAME);
				result += workshop.getTechLevel() * workshop.getConcurrentProcesses();
			}
			catch (BuildingException e) {
			    logger.log(Level.SEVERE,"Engineer.getSettlementNeed()",e);
			}
		}
		
		return result;	
	}
}