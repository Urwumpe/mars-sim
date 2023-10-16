/**
 * Mars Simulation Project
 * RoleCommand.java
 * @version 3.1.2 2020-12-30
 * @author Barry Evans
 */

package org.mars_sim.console.chat.simcommand.settlement;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.mars_sim.console.chat.ChatCommand;
import org.mars_sim.console.chat.Conversation;
import org.mars_sim.console.chat.simcommand.CommandHelper;
import org.mars_sim.console.chat.simcommand.StructuredResponse;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.structure.Settlement;

public class RoleCommand extends AbstractSettlementCommand {
	public static final ChatCommand ROLE = new RoleCommand();

	private RoleCommand() {
		super("rl", "role", "Settlement allocaiton of Roles");
	}

	@Override
	protected boolean execute(Conversation context, String input, Settlement settlement) {
		StructuredResponse response = new StructuredResponse();
		
		response.appendTableHeading("Role", CommandHelper.ROLE_WIDTH, "Name");
		
		List<Person> list = settlement.getAllAssociatedPeople().stream()
				.sorted(Comparator.comparing(o -> o.getRole().getType().ordinal())).collect(Collectors.toList());

		for (Person p : list) {
			String role = p.getRole().getType().getName();
			response.appendTableRow(role, p.getName());
		}
		
		context.println(response.getOutput());
		return true;
	}

}
