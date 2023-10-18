/**
 * Mars Simulation Project
 * Objective.java
 * @version 3.2.0 2021-06-20
 * @author Manny Kung
 */

package com.mars_sim.core.structure;

public interface Objective {

	public void setObjective(ObjectiveType objectiveType, int level);

	public ObjectiveType getObjective();
	
}
