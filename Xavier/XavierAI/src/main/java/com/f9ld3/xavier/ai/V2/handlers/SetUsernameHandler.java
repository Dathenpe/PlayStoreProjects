package com.f9ld3.xavier.ai.V2.handlers;

import com.f9ld3.xavier.ai.V2.ConversationContext;

/**
 * Sets the user's name in the conversation context.
 * This handler now relies on an entity being extracted by the PatternHandler.
 */
public class SetUsernameHandler implements IntentHandler {

@Override
public String handle(String userInput, ConversationContext context) {
	String name = (String) context.getEntity("username");
	if (name != null && !name.isEmpty()) {
		// Capitalize the first letter for a nice touch
		String capitalizedName = name.substring(0, 1).toUpperCase() + name.substring(1);
		context.setUsername(capitalizedName);
		return "Got it. I'll call you " + capitalizedName + " from now on.";
	}
	return "I heard you want me to call you something, but I didn't catch the name.";
}
}