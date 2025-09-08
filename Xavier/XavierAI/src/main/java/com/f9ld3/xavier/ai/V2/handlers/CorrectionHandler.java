package com.f9ld3.xavier.ai.V2.handlers;

import com.f9ld3.xavier.ai.V2.ConversationContext;
import com.f9ld3.xavier.ai.V2.XavierCoreV2;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handles user corrections (e.g., "no, I meant...").
 * This handler intelligently extracts the user's intended query and re-routes it
 * through the main AI core for a fresh analysis, respecting the user's explicit intent.
 */
public class CorrectionHandler implements IntentHandler {

private final XavierCoreV2 core;

// REFINED: This pattern is now more robust and handles a wider variety of natural language corrections.
private static final Pattern CORRECTION_PATTERN = Pattern.compile(
		"(?i)(?:no,?|nope,?|actually,?|i mean|i meant|what i meant was|i meant to say)\\s*(.+)"
);

public CorrectionHandler(XavierCoreV2 core) {
	this.core = core;
}

@Override
public String handle(String userInput, ConversationContext context) {
	Matcher matcher = CORRECTION_PATTERN.matcher(userInput);
	
	if (matcher.find()) {
		// Extract the corrected query (e.g., "the word artichoke").
		String correctedQuery = matcher.group(1).trim();
		
		if (correctedQuery.isEmpty()) {
			return "What did you mean instead?";
		}
		
		// Create a new, clean context for the re-routed query to avoid state conflicts.
		// We preserve the username for a personal touch.
		ConversationContext newContext = new ConversationContext();
		newContext.setUsername(context.getUsername());
		
		if (XavierCoreV2.DEBUG_MODE) {
			System.out.println("[DEBUG] CorrectionHandler: Rerouting new query: '" + correctedQuery + "'");
		}
		
		// Re-route the corrected query through the main getResponse method.
		// This ensures it goes through the entire prediction pipeline again.
		String newResponse = core.getResponse(correctedQuery, newContext);
		
		// Prepend a conversational acknowledgement to the new response.
		return "My mistake. Let's try this instead:\n\n" + newResponse;
	}
	
	// Fallback if the pattern somehow fails to match, though it's unlikely.
	return "I'm not sure what you meant. Could you please rephrase your question?";
}
}