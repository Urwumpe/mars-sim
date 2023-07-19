/*
 * Mars Simulation Project
 * Relation.java
 * @date 2023-05-24
 * @author Manny Kung
 */
package org.mars_sim.msp.core.person.ai.social;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.mars.sim.tools.util.RandomUtil;
import org.mars_sim.msp.core.UnitManager;
import org.mars_sim.msp.core.person.Person;

/**
 * The Relation class models the relational connection of a person toward others.
 */
public class Relation implements Serializable {

	/** default serial id. */
	private static final long serialVersionUID = 1L;
	
	// How trustworthy is that person toward this person
	private static final String TRUST = "trust";
	// How caring is that person  toward this person
	private static final String CARE = "care";
	// How respecting is that person  toward this person 
	private static final String RESPECT = "respect";
	
	/** The person's opinion of another person. */
	private Map<Integer, Map<String, Double>> opinionMap = new HashMap<>();
	
	/** The Unit Manager instance. */
	private static UnitManager unitManager;

	/**
	 * Constructor.
	 * 
	 * @param person
	 */
	public Relation(Person person)  {
	}
	

	/**
	 * Gets the opinion regarding a person.
	 * 
	 * @param personID
	 * @return
	 */
	public double getOpinion(int personID) {
		Map<String, Double> dimensionMap = null;
		if (opinionMap.containsKey(personID)) {
			dimensionMap = opinionMap.get(personID);
		}
		else {
			return -1.0;
		}
		double average = (dimensionMap.get(TRUST) 
				+ dimensionMap.get(CARE)
				+ dimensionMap.get(RESPECT)) / 3.0;
		return average;
	}

	/**
	 * Gets the opinion array regarding a person.
	 * 
	 * @param personID
	 * @return
	 */
	double[] getOpinions(int personID) {
		Map<String, Double> dimensionMap = null;
		if (opinionMap.containsKey(personID)) {
			dimensionMap = opinionMap.get(personID);
		}
		else {
			return new double[]{-1.0, -1.0, -1.0};
		}
		double[] dim = new double[3];
		dim[0] = dimensionMap.get(TRUST);
		dim[1] = dimensionMap.get(CARE);
		dim[2] = dimensionMap.get(RESPECT);
		return dim;
	}
	
	/**
	 * Sets the opinion regarding a person.
	 * 
	 * @param personID
	 * @param opinion
	 */
	void setOpinion(int personID, double opinion) {
		if (opinion < 1)
			opinion = 1;
		if (opinion > 100)
			opinion = 100;
		
		if (!opinionMap.containsKey(personID)) {
			Map<String, Double> dimensionMap = new HashMap<>(3);
			dimensionMap.put(TRUST, 50.0 + RandomUtil.getRandomDouble(-10, 10));
			dimensionMap.put(CARE, 50.0 + RandomUtil.getRandomDouble(-10, 10));
			dimensionMap.put(RESPECT, 50.0 + RandomUtil.getRandomDouble(-10, 10));
			opinionMap.put(personID, dimensionMap);
		}
		else {
			Map<String, Double> dimensionMap = opinionMap.get(personID);
			int rand = RandomUtil.getRandomInt(2);
			if (rand == 0) {
				dimensionMap.put(TRUST, opinion);
			}
			else if (rand == 1) {
				dimensionMap.put(CARE, opinion);
			}
			else {
				dimensionMap.put(RESPECT, opinion);
			}
		}
	}
	
	
	/**
	 * Changes the opinion regarding a person.
	 * 
	 * @param personID
	 * @param mod
	 */
	void changeOpinion(int personID, double mod) {
		double result = getOpinion(personID) + mod;
		if (result < 1)
			result = 1;
		if (result > 100)
			result = 100;
		setOpinion(personID, result);
	}
	
	/**
	 * Gets all the people that a person knows (has met).
	 * 
	 * @param person the person
	 * @return a list of the people the person knows.
	 */
	Set<Person> getAllKnownPeople(Person person) {
		return opinionMap.keySet().stream()
				.map(id -> unitManager.getPersonByID(id))
				.collect(Collectors.toUnmodifiableSet());
	}

	/**
	 * Initializes instances.
	 * 
	 * @param um the unitManager instance
	 */
	public static void initializeInstances(UnitManager um) {
		unitManager = um;		
	}
	
	/**
	 * Prepares object for garbage collection.
	 */
	public void destroy() {
		opinionMap.clear();
		opinionMap = null;
	}
}
