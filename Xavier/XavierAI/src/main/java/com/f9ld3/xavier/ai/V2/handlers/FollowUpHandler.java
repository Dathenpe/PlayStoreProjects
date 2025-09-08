// C:/Users/Music_Minister/Desktop/PlayStore/PlayStoreProjects/Xavier/XavierAI/src/main/java/com/f9ld3/xavier/ai/V2/handlers/FollowUpHandler.java
package com.f9ld3.xavier.ai.V2.handlers;

import com.f9ld3.xavier.ai.V2.ConversationContext;
import com.f9ld3.xavier.ai.V2.XavierCoreV2;

import java.util.Map;
import java.util.Optional;

/**
 * Handles follow-up questions by intelligently resolving pronouns and combining
 * the user's new query with the last known entities from the conversation context.
 */
public class FollowUpHandler implements IntentHandler {

private final XavierCoreV2 core;

private static final Map<String, String> PRONOUN_REPLACEMENTS = Map.of(
		"his", "'s", "her", "'s", "its", "'s", "their", "'s",
		"he", "", "she", "", "it", "", "they", "", "him", "", "them", ""
);

public FollowUpHandler(XavierCoreV2 core) {
	this.core = core;
}

@Override
public String handle(String userInput, ConversationContext context) {
	// Get the primary subject from the current context's entities.
	Optional<Object> lastSubjectOpt = context.getEntityFromCurrentContext("subject");
	
	if (lastSubjectOpt.isEmpty() || !(lastSubjectOpt.get() instanceof String lastSubject) || lastSubject.isBlank()) {
		return "I'm sorry, I've lost the context. What were we talking about?";
	}
	
	String newQuery = userInput;
	boolean pronounFound = false;
	
	if (userInput.matches("(?i).*\\bthere\\b.*")) {
		newQuery = userInput.replaceAll("(?i)\\bthere\\b", "in " + lastSubject);
		pronounFound = true;
	} else {
		for (Map.Entry<String, String> entry : PRONOUN_REPLACEMENTS.entrySet()) {
			String pronoun = entry.getKey();
			if (userInput.matches("(?i).*\\b" + pronoun + "\\b.*")) {
				String replacementText = lastSubject + entry.getValue();
				newQuery = userInput.replaceAll("(?i)\\b" + pronoun + "\\b", replacementText);
				pronounFound = true;
				break;
			}
		}
	}
	
	if (!pronounFound) {
		// If no pronoun is found, assume the user is asking about the last subject.
		// e.g., "Tell me about France." -> "And the capital city?" -> "capital city of France"
		newQuery = userInput + " of " + lastSubject;
	}
	
	if (XavierCoreV2.DEBUG_MODE) {
		System.out.printf("[DEBUG] FollowUpHandler: Last subject: '%s'%n", lastSubject);
		System.out.printf("[DEBUG] FollowUpHandler: Original input: '%s'%n", userInput);
		System.out.printf("[DEBUG] FollowUpHandler: Rerouting with new query: '%s'%n", newQuery);
	}
	
	// Re-route the new, complete query.
	// We don't create a new context, but instead let the core decide if this is an interruption.
	return core.getResponse(newQuery, context);
}
}