package com.f9ld3.xavier.ai.V2.handlers;

import com.f9ld3.xavier.ai.V2.ConversationContext;
import com.f9ld3.xavier.ai.V2.XavierCoreV2;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handles user corrections by understanding the last intent and re-running the
 * core reasoning pipeline with a new, reconstructed query. This creates a
 * powerful and intuitive conversational repair mechanism.
 */
public class CorrectionHandler implements IntentHandler {

private final XavierCoreV2 xavierCore;

// A more robust pattern to extract the corrected term from various conversational phrases.
private static final Pattern CORRECTION_PATTERN = Pattern.compile(
		"(?:i mean|no i meant|i meant to say|correction:?)\\s*(.+)",
		Pattern.CASE_INSENSITIVE
);

/**
 * The constructor requires a reference to the main Xavier core to be able
 * to re-trigger the full reasoning pipeline.
 * @param xavierCore The main instance of the AI core.
 */
public CorrectionHandler(XavierCoreV2 xavierCore) {
	this.xavierCore = xavierCore;
}

@Override
public String handle(String userInput, ConversationContext context) {
	// 1. Extract the core corrected term from the user's input.
	String correctedTerm = extractCorrection(userInput);
	if (correctedTerm == null) {
		// If the pattern doesn't match, the user's input might be the correction itself.
		// Example: User: "dijbouti" -> AI fails. User: "djibouti"
		// We treat the whole input as the correction.
		correctedTerm = userInput;
	}
	
	// 2. Get the last successful or attempted intent from the context.
	String lastIntent = context.getLastIntent();
	if (lastIntent == null || lastIntent.equals("correction")) {
		return "I don't have a previous command to correct. What would you like to do?";
	}
	
	// 3. Intelligently reconstruct a full query based on the last intent.
	// This is the key to a robust correction system.
	String newQuery;
	switch (lastIntent) {
		case "weather_query":
			newQuery = "what is the weather in " + correctedTerm;
			break;
		case "timezone_query":
			newQuery = "what is the time in " + correctedTerm;
			break;
		default:
			// For knowledge queries or other intents, the term itself is often the best query.
			newQuery = correctedTerm;
			break;
	}
	
	System.out.printf("[DEBUG] Correction: Rerouting to full pipeline with new query: '%s'%n", newQuery);
	
	// 4. Re-invoke the entire reasoning pipeline with the new, improved query.
	// This powerful pattern reuses all existing logic for maximum consistency.
	return xavierCore.getResponse(newQuery, context);
}

/**
 * Uses regex to strip away conversational phrases and isolate the corrected entity.
 * @param input The raw user input.
 * @return The extracted term, or null if the pattern doesn't match.
 */
private String extractCorrection(String input) {
	Matcher matcher = CORRECTION_PATTERN.matcher(input);
	if (matcher.find()) {
		// group(1) will contain the corrected term.
		return matcher.group(1).trim();
	}
	// Return null if no conversational filler phrase was found.
	return null;
}
}