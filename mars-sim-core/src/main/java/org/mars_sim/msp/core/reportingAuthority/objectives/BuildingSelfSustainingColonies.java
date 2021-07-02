/**
 * Mars Simulation Project
 * DeterminingHabitability.java
 * @version 3.2.0 2021-06-20
 * @author Manny Kung
 */
package org.mars_sim.msp.core.reportingAuthority.objectives;

import java.io.Serializable;

import org.mars_sim.msp.core.logging.SimLogger;
import org.mars_sim.msp.core.person.ai.task.utils.Worker;
import org.mars_sim.msp.core.reportingAuthority.MissionAgenda;

public class BuildingSelfSustainingColonies implements MissionAgenda, Serializable  {
	/** default serial id. */
	private static final long serialVersionUID = 1L;
	
	private static SimLogger logger = SimLogger.getLogger(BuildingSelfSustainingColonies.class.getName());

	private final String name = "Building Self-Sustaining Colonies";

	private final String[] agendas = new String[] {
								"Study meteorological environmental factors",
								"Fortify building structural integrity",
								"Refine techniques for ISRU polymer synthesis"
//								"Analyze Medical Data"
								};

	// Note : index for missionModifiers : 
	//	0 : AreologyFieldStudy
	//	1 : BiologyFieldStudy
	//	2 : CollectIce
	//	3 : CollectRegolith	
	//	4 : Exploration
	//	5 : MeteorologyFieldStudy
	//	6 : Mining
	//  7 : Trade
	//  8 : TravelToSettlement

	private final int[][] missionModifiers = new int[][] {
			{3, 0, 0, 0, 0, 9, 0, 0, 0},
			{0, 0, 0, 3, 0, 0, 3, 0, 0},
			{0, 0, 0, 9, 0, 0, 3, 0, 0}
	};
		
	public BuildingSelfSustainingColonies() {
	}		
	
	@Override	
	public int[][] getMissionModifiers() {
		return missionModifiers;
	}
	
	@Override
	public String[] getAgendas() {
		return agendas;
	}
	
	@Override
	public String getObjectiveName() {
		return name;
	}

	@Override
	public void reportFindings(Worker unit) {
		logger.info(unit, 20_000L, "Updating the report of the best practices in resource utilization.");
	}

	@Override
	public void gatherSamples(Worker unit) {
		logger.info(unit, 20_000L, "Analyzing various geological and environment factors affecting how one may build several self-sustainable colonies in this region.");
	}
}
