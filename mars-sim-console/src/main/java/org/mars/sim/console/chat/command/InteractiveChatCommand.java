package org.mars.sim.console.chat.command;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.mars.sim.console.chat.ChatCommand;
import org.mars.sim.console.chat.Conversation;
import org.mars.sim.console.chat.simcommand.QuitCommand;


public class InteractiveChatCommand extends ChatCommand {
	// Simple POJO to hold results of command parsing
	public static class ParseResult {
		ChatCommand command;
		String parameter;
		private String matchedCommand;
		
		public ParseResult(ChatCommand command, String parameter, String matchedCommand) {
			this.command = command;
			this.parameter = parameter;
			this.matchedCommand = matchedCommand;
		}
	};
	
    private static final Logger LOGGER = Logger.getLogger(InteractiveChatCommand.class.getName());
	
	// Shared Standard command
	private static final ChatCommand HELP = new HelpCommand();
	private static final ChatCommand QUIT = new QuitCommand();
	private static final ChatCommand INTRO = new IntroCommand();
	
	// Prefix add by the user when using the short commands
	public static final String SHORT_PREFIX = "/";
	
	private Map<String,ChatCommand> longCommands;
	private Map<String,ChatCommand> shortCommands;

	private String prompt;
	
	public InteractiveChatCommand(String commandGroup, String shortCommand, String longCommand, String description,
								  String prompt, List<ChatCommand> commands) {
		super(commandGroup, shortCommand, longCommand, description);
		this.prompt = prompt;
		this.longCommands = new HashMap<>();
		this.shortCommands = new HashMap<>();
		
		setInteractive(true);
		
		addSubCommand(INTRO);
		addSubCommand(QUIT);
		addSubCommand(HELP);
		
		// Must create a dedicated RepeatCommand
		addSubCommand(new RepeatCommand());
		
		for (ChatCommand chatCommand : commands) {
			addSubCommand(chatCommand);
		}	
	}

	/**
	 * Add a new sub-command to this interaction. It is registered with both the short & long command.
	 * @param command New command
	 */
	protected void addSubCommand(ChatCommand command) {
		if (longCommands.containsKey(command.getLongCommand())) {
			LOGGER.warning("Command is already registered with " + command.getLongCommand());
		}
		else {
			longCommands.put(command.getLongCommand(), command);
		}
		
		if (shortCommands.containsKey(command.getShortCommand())) {
			LOGGER.warning("Command is already registered with " + command.getShortCommand());
		}
		else {
			shortCommands.put(command.getShortCommand(), command);		
		}
	}
	
	/**
	 * Default implementation check if the command matches any of the subcommands.
	 * @param context
	 * @param input 
	 * @return Did it execute
	 */
	@Override
	public boolean execute(Conversation context, String input) {

		ParseResult result = parseInput(input);
		
		// Found a matching command
		if (result.command != null) {
			String preamble = result.command.getIntroduction();
			if (preamble == null) {
				preamble = result.command.getDescription();
			}
			context.println(preamble);
			return result.command.execute(context, result.parameter);
		}
		else {
			// Don't know the command so prompt the help
			context.println("Sorry I didn't understand you. Here is what I know about");
			HELP.execute(context, null);
			
			return false;
		}
	}

	/**
	 * Get the list of options that match the partial input.
	 * @param partialInput
	 * @return List of potential full commands.
	 */
	public List<String> getAutoComplete(Conversation context, String partialInput) {
		List<String> result = null;
		ParseResult parseOutcome = parseInput(partialInput);
		
		// Partial has found a command
		if (parseOutcome.command != null) {
			result = parseOutcome.command.getAutoComplete(context, parseOutcome.parameter);
			
			// Must add the command in
			String commandText = parseOutcome.matchedCommand;
			result = result.stream().map(m -> commandText + " " + m).collect(Collectors.toList());
		}
		else {
			Set<String> targetList = null;
			
			// Partial command has to scan commands now
			String prefix = "";
			if (partialInput.startsWith(SHORT_PREFIX)) {
				targetList = shortCommands.keySet();
				partialInput = partialInput.substring(SHORT_PREFIX.length());
				prefix = SHORT_PREFIX;
			}
			else {
				targetList = longCommands.keySet();
			}
			
			// Find any matches that start with the partial input
			final String partialCommand = partialInput;
			final String finalPrefix = prefix;
			result = targetList.stream()                // convert list to stream
	                .filter(line -> line.startsWith(partialCommand))     // Any command starting with my partial input
	                .map(s -> finalPrefix + s)
	                .collect(Collectors.toList()); 
			
		}
		
		return result;
	}

	/**
	 * Get the prompt for interactive Chat Commands
	 * @return
	 */
	public String getPrompt() {
		return prompt;
	}
	
	
	/**
	 * Get a list of the commands 
	 * @return
	 */
	public List<ChatCommand> getSubCommands() {
		return new ArrayList<ChatCommand>(longCommands.values());
	}

	/**
	 * Parse the input and find any commands either via the long or short word. Also extract any remaining parameters.
	 * @param input User entry.
	 * @return
	 */
	public ParseResult parseInput(String input) {
		ChatCommand found = null;
		int tailIndex = 0;
		String matchedCommand = null;
		
		if (input.startsWith(SHORT_PREFIX)) {
			tailIndex = input.indexOf(' ');
			String command = input;
			if (tailIndex > 0) {
				command = input.substring(SHORT_PREFIX.length(), tailIndex);
			}
			else {
				command = input.substring(SHORT_PREFIX.length());
			}
			found = shortCommands.get(command);
			matchedCommand = SHORT_PREFIX + command;
		}
		else {
			// Try to find a match using the words
			while ((tailIndex >= 0) && (found == null)) {
				tailIndex = input.indexOf(' ', tailIndex + 1);
				matchedCommand = input;
				if (tailIndex > 0) {
					matchedCommand = input.substring(0, tailIndex);
				}
				found = longCommands.get(matchedCommand);
			}
		}
		
		// Was there are tail left as the parameter ?
		String tail = null;
		if (tailIndex >= 0) {
			tail = input.substring(tailIndex+1);
		}

		return new ParseResult(found, tail, matchedCommand);
	}

	@Override
	public String toString() {
		return "InteractiveChatCommand [keyword=" + getLongCommand() + "]";
	}
}
