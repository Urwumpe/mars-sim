/*
 * Mars Simulation Project
 * PersonReviveCommand.java
 * @date 2022-08-24
 * @author Barry Evans
 */

package org.mars_sim.console.chat.simcommand.person;

import org.mars_sim.console.chat.ChatCommand;
import org.mars_sim.console.chat.Conversation;
import org.mars_sim.console.chat.ConversationRole;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.PhysicalCondition;

/** 
 * Expert command to revive a Person 
 */
public class PersonReviveCommand extends AbstractPersonCommand {
	public static final ChatCommand REVIVE = new PersonReviveCommand();
	
	private PersonReviveCommand() {
		super("rv", "revive", "Revive a Person");
		
		addRequiredRole(ConversationRole.EXPERT);
	}

	@Override
	public boolean execute(Conversation context, String input, Person person) {
		context.println("Revive the Person's health");
		
		PhysicalCondition pc = person.getPhysicalCondition();
		pc.setFatigue(0D);
		pc.setHunger(0D);
		pc.setStress(0D);
		pc.setThirst(0D);
		pc.setPerformanceFactor(1D);
		return true;
	}
}
