package org.mars.sim.console.chat.simcommand;

import org.mars.sim.console.chat.ChatCommand;
import org.mars.sim.console.chat.Conversation;
import org.mars_sim.msp.core.time.MasterClock;

public class SpeedCommand extends ChatCommand {

	public SpeedCommand() {
		super(TopLevel.SIMULATION_GROUP, "sp", "speed", "Change the simulation speed");
		setInteractive(true);
	}

	@Override
	public boolean execute(Conversation context, String input) {
		MasterClock clock = context.getSim().getMasterClock();
        int currentSpeed = clock.getActualRatio();
        context.println("The current simulation speed is " + currentSpeed); 

		String change = context.getInput("Change (Y/N)?");
        
        if ("Y".equalsIgnoreCase(change)) {
            int newSpeed = context.getIntInput("Enter the new simulation speed [1..10]");
            if ((1 <= newSpeed) && (newSpeed <= 10)) {
            	int ratio = (int) Math.pow(2, newSpeed);
            	clock.setTimeRatio(ratio);
            	context.println("New speed is " + newSpeed + ", ratio x" + ratio);
            }
        }
        return true;
	}     
}
