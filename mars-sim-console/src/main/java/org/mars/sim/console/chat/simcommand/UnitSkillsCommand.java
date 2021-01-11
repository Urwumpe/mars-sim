package org.mars.sim.console.chat.simcommand;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.mars.sim.console.chat.ChatCommand;
import org.mars.sim.console.chat.Conversation;
import org.mars_sim.msp.core.Unit;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.ai.SkillManager;
import org.mars_sim.msp.core.robot.Robot;

/**
 * Command to display the Skills of a Person or Robot
 * This is a singleton.
 */
public class UnitSkillsCommand extends ChatCommand {

	public UnitSkillsCommand(String group) {
		super(group, "sk", "skills", "What skills to I have?");
	}

	/** 
	 * Output the current immediate location of the Unit
	 */
	@Override
	public boolean execute(Conversation context, String input) {
		ConnectedUnitCommand parent = (ConnectedUnitCommand) context.getCurrentCommand();	
		Unit target = parent.getUnit();

		SkillManager skillManager = null;
		if (target instanceof Person) {
			skillManager = ((Person)target).getSkillManager();
		}
		else if (target instanceof Robot) {
			skillManager = ((Robot)target).getSkillManager();
		}

		boolean result = false;
		if (skillManager != null) {
			StringBuilder responseText = new StringBuilder();
			responseText.append("here's a list of my skills, current level, and labor time and experience points needed for the next level: ");
			responseText.append(System.lineSeparator());
			responseText.append("       Type of Skill | Level | Exp Needed | Labor Time [sols]");
			responseText.append(System.lineSeparator());
			responseText.append("     ---------------------------------------------------------");
			responseText.append(System.lineSeparator());

			Map<String, Integer> levels = skillManager.getSkillLevelMap();
			Map<String, Integer> exps = skillManager.getSkillDeltaExpMap();
			Map<String, Integer> times = skillManager.getSkillTimeMap();
			List<String> skillNames = skillManager.getKeyStrings();
			Collections.sort(skillNames);

			for (String n : skillNames) {
				responseText.append(String.format("%20s %5d %12d %14f%n", n, levels.get(n),
												  exps.get(n), Math.round(100.0 * times.get(n))/100000.0));	
			}
			context.println(responseText.toString());
			
			result = true;
		}
		else {
			context.println("Sorry I can not provide that information");
		}
		
		return result;
	}
}
