/**
 * Mars Simulation Project
 * ConsoleApp.java
 * @version 3.1.2 2020-12-30
 * @author Barry Evans
 */

package com.mars_sim.console.chat;

import java.util.HashSet;
import java.util.Set;

import org.beryx.textio.TextIO;
import org.beryx.textio.TextIoFactory;

import com.mars_sim.console.chat.simcommand.TopLevel;

public class ConsoleApp {
	
	public static void main(String... args) {
    
		UserChannel channel = null;
		
		if ((args.length > 1) && args[1].equals("--swing")) {
			TextIO term =  TextIoFactory.getTextIO();
			channel = new TextIOChannel(term);
		}
		else {
			channel = new StreamChannel(System.in, System.out);
		}
		
		Set<ConversationRole> roles = new HashSet<>();
		roles.add(ConversationRole.ADMIN);
        Conversation conversation = new Conversation(channel, new TopLevel(), roles, null);
        conversation.interact();
        
    }

}
