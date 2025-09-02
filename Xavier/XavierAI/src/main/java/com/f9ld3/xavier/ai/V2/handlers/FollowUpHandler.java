package com.f9ld3.xavier.ai.V2.handlers;

import com.f9ld3.xavier.ai.V2.ConversationContext;
import com.f9ld3.xavier.ai.V2.XavierCoreV2;

import java.util.Map;

/**
 * Handles follow-up questions by intelligently resolving pronouns and combining
 * the user's new query with the last known subject from the conversation context.
 */
public class FollowUpHandler implements IntentHandler {

private final XavierCoreV2 core;

// A map for intelligent pronoun replacement for people/things.
private static final Map<String, String> PRONOUN_REPLACEMENTS = Map.of(
		// Possessive pronouns -> replace with "[subject]'s"
		"his", "'s",
		"her", "'s",
		"its", "'s",
		"their", "'s",
		// Subject/Object pronouns -> replace with "[subject]"
		"he", "",
		"she", "",
		"it", "",
		"they", "",
		"him", "",
		"them", ""
);

public FollowUpHandler(XavierCoreV2 core) {
	this.core = core;
}

@Override
public String handle(String userInput, ConversationContext context) {
	String lastSubject = context.getLastSubject();
	
	if (lastSubject == null || lastSubject.isBlank()) {
		return "I'm sorry, I've lost the context. What were we talking about?";
	}
	
	String newQuery = userInput;
	boolean pronounFound = false;
	
	// --- UPDATED LOGIC: Add a special case for the locative pronoun "there" ---
	if (userInput.matches("(?i).*\\bthere\\b.*")) {
		// Replace "there" with "in [lastSubject]" to form a complete query.
		// Example: "what language is spoken there" -> "what language is spoken in France"
		newQuery = userInput.replaceAll("(?i)\\bthere\\b", "in " + lastSubject);
		pronounFound = true;
	} else {
		// Fallback to the existing logic for other pronouns.
		for (Map.Entry<String, String> entry : PRONOUN_REPLACEMENTS.entrySet()) {
			String pronoun = entry.getKey();
			String replacementSuffix = entry.getValue();
			
			if (userInput.matches("(?i).*\\b" + pronoun + "\\b.*")) {
				String replacementText = lastSubject + replacementSuffix;
				newQuery = userInput.replaceAll("(?i)\\b" + pronoun + "\\b", replacementText);
				pronounFound = true;
				break; // Stop after the first pronoun is found and replaced
			}
		}
	}
	
	// If no pronoun was found (which should be rare given the core pattern), return a helpful message.
	if (!pronounFound) {
		return "I can see you're asking a follow-up, but I'm not sure how to connect it to our last topic. Could you please ask a full question?";
	}
	
	if (XavierCoreV2.DEBUG_MODE) {
		System.out.printf("[DEBUG] FollowUpHandler: Last subject: '%s'%n", lastSubject);
		System.out.printf("[DEBUG] FollowUpHandler: Original input: '%s'%n", userInput);
		System.out.printf("[DEBUG] FollowUpHandler: Rerouting with new query: '%s'%n", newQuery);
	}
	
	// Re-route the new, complete query back through the main pipeline.
	// Create a new context to avoid infinite loops and carry over the username.
	ConversationContext newContext = new ConversationContext();
	newContext.setUsername(context.getUsername());
	return core.getResponse(newQuery, newContext);
}
}