// C:/Users/Music_Minister/Desktop/PlayStore/PlayStoreProjects/Xavier/XavierAI/src/main/java/com/f9ld3/xavier/ai/V2/handlers/SetUsernameHandler.java
package com.f9ld3.xavier.ai.V2.handlers;

import com.f9ld3.xavier.ai.V2.ConversationContext;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Sets the user's name in the conversation context.
 * REFACTORED: Now fully integrated with the new context stack and entity system.
 */
public class SetUsernameHandler implements IntentHandler {

// FIX: Add a fallback pattern directly in the handler for robustness.
private static final Pattern NAME_PATTERN = Pattern.compile(
		"(?i)(?:my name is|call me|please call me|you can call me)\\s+(.+)"
);

@Override
public String handle(String userInput, ConversationContext context) {
	// First, try to get the name from the context (the preferred way).
	Optional<String> nameOpt = context.getEntityFromCurrentContext("username")
			                           .map(String::valueOf);
	
	// If the context doesn't have the name, try to extract it manually as a fallback.
	if (nameOpt.isEmpty()) {
		Matcher matcher = NAME_PATTERN.matcher(userInput);
		if (matcher.find()) {
			nameOpt = Optional.of(matcher.group(1).trim());
		}
	}
	
	if (nameOpt.isPresent() && !nameOpt.get().isBlank()) {
		String name = nameOpt.get();
		// Capitalize the first letter for a nice touch
		String capitalizedName = name.substring(0, 1).toUpperCase() + name.substring(1);
		context.setUsername(capitalizedName);
		return "Got it. I'll call you " + capitalizedName + " from now on.";
	}
	
	// This fallback is triggered if both the context and the manual pattern fail.
	return "I heard you want me to call you something, but I didn't catch the name.";
}
}