/**
 * Mars Simulation Project
 * UnitMalfunctionCommand.java
 * @version 3.1.2 2020-12-30
 * @author Barry Evans
 */

package org.mars_sim.console.chat.simcommand.unit;

import java.util.Collection;

import org.mars_sim.console.chat.Conversation;
import org.mars_sim.console.chat.simcommand.CommandHelper;
import org.mars_sim.console.chat.simcommand.StructuredResponse;
import org.mars_sim.msp.core.Unit;
import org.mars_sim.msp.core.malfunction.Malfunction;
import org.mars_sim.msp.core.malfunction.MalfunctionFactory;
import org.mars_sim.msp.core.malfunction.MalfunctionManager;
import org.mars_sim.msp.core.malfunction.Malfunctionable;

/**
 * Command to create a malfunction in a Malfunctionable.
 */
public class UnitMalfunctionCommand extends AbstractUnitCommand {

	/**
	 * Create a command that is assigned to a command group
	 * @param group
	 */
	public UnitMalfunctionCommand(String group) {
		super(group, "ml", "malfunction", "Display any malfunctions");
	}
	
	@Override
	protected boolean execute(Conversation context, String input, Unit source) {
		
		if (!(source instanceof Malfunctionable)) {
			context.println("Sorry ! Can't connect to a Malfunctionable Unit");
			return false;
		}

		StructuredResponse response = new StructuredResponse();
		Collection<Malfunctionable> entities = MalfunctionFactory.getMalfunctionables((Malfunctionable) source);

		for( Malfunctionable e : entities) {
			MalfunctionManager mgr = e.getMalfunctionManager();
			for (Malfunction m : mgr.getMalfunctions()) {
				if (!m.isFixed()) {
					CommandHelper.outputMalfunction(response, e, m);
					response.appendBlankLine();
				}
			}
		}
		context.println(response.getOutput());
		return true;
	}
}
