/**
 * Mars Simulation Project
 * Crew.java
 * @version 3.1.2 2020-09-02
 * @author Manny Kung
 */
package org.mars_sim.msp.core.person;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Crew implements Serializable {

    /** default serial id. */
    private static final long serialVersionUID = 1L;

    //private static Logger logger = Logger.getLogger(Crew.class.getName());

	private String name;
    private String crewName;
	private String destination;
	
	private List<Member> team = new ArrayList<>();
	
	public Crew(String name) {
		this.name = name;	
	}

	public void setCrewName(String value) {
		crewName = value;
	}
	
	public String getCrewName() {
		return crewName;
	} 
	
	public void addMember(Member m) {
		team.add(m);
	}

	//public void add(Set<Member> members) {
	//	team = members;
	//}
	
	//public void add(List<Member> members) {
	//	members = members;
	//}
	
	//public Set<Member> getTeam() {
	//	return team;
	//}

	public List<Member> getTeam() {
		return team;
	}

	public String getName() {
		return name;
	}

	public void setDestination(String value) {
		destination = value;
		// TODO: set destination for all members 
	}
	
	public String getDestination() {
		return destination;
	}
}
