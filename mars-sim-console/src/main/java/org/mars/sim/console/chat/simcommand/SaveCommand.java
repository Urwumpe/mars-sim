package org.mars.sim.console.chat.simcommand;

import org.mars.sim.console.chat.ChatCommand;
import org.mars.sim.console.chat.Conversation;
import org.mars_sim.msp.core.Simulation.SaveType;

public class SaveCommand extends ChatCommand {

	public SaveCommand() {
		super(TopLevel.SIMULATION_GROUP, "s", "save", "Save the simulation");
		setInteractive(true);
	}

	@Override
	public boolean execute(Conversation context, String input) {
		String toSave = context.getInput("Save simulation (Y/N)?");
	
        if ("Y".equalsIgnoreCase(toSave)) {
            context.println("Saving Simulation...");
            context.getSim().getMasterClock().setSaveSim(SaveType.SAVE_DEFAULT, null); 
        }

		context.println("Done");
		return true;
	}
}
