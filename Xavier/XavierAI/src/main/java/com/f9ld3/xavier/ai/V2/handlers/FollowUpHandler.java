package com.f9ld3.xavier.ai.V2.handlers;

import com.f9ld3.xavier.ai.V2.ConversationContext;
import com.f9ld3.xavier.ai.V2.XavierCoreV2;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.f9ld3.xavier.ai.V2.XavierCoreV2.DEBUG_MODE;

/**
 * Handles conversational follow-up questions by using the context of the last
 * command to construct and execute a new, more specific query.
 */
public class FollowUpHandler implements IntentHandler {

private final XavierCoreV2 xavierCore;

// A pattern to extract the new subject from various follow-up phrases.
private static final Pattern FOLLOW_UP_SUBJECT_PATTERN = Pattern.compile(
		"^(?:what about|how about|and|tell me more about)?(?: in| for| the weather in| the time in)?\\s*(.+)",
		Pattern.CASE_INSENSITIVE
);

/**
 * The constructor requires a reference to the main Xavier core to be able
 * to re-trigger the full reasoning pipeline.
 * @param xavierCore The main instance of the AI core.
 */
public FollowUpHandler(XavierCoreV2 xavierCore) {
	this.xavierCore = xavierCore;
}

@Override
public String handle(String userInput, ConversationContext context) {
	String lastIntent = context.getLastIntent();
	String lastSubject = context.getLastSubject();
	
	if (lastIntent == null) {
		return "I'm not sure what we were talking about. Could you please ask a full question?";
	}
	
	String newSubject = extractNewSubject(userInput);
	if (newSubject == null) {
		// This handles "tell me another one" where the subject is the action itself.
		newSubject = userInput;
	}
	
	String newQuery;
	switch (lastIntent) {
		case "weather_query":
			newQuery = "what is the weather in " + newSubject;
			break;
		case "timezone_query":
			newQuery = "what is the time in " + newSubject;
			break;
		case "joke_query":
			newQuery = "tell me a joke";
			break;
		case "fact_query":
			newQuery = "tell me a fun fact";
			break;
		case "knowledge_query":
			// For knowledge queries, intelligently combine the last subject with the new one.
			if (lastSubject != null) {
				// Heuristic 1: The follow-up is a full question about the last subject.
				// e.g., "who is obama" -> "where was he born" -> "where was he born who is obama"
				if (newSubject.matches("^(who|what|where|when|why|how|is|are|was|were|do|does|did).*")) {
					newQuery = newSubject + " " + lastSubject;
				} else {
					// Heuristic 2: The follow-up replaces a descriptor.
					// e.g., "tallest building" -> "how about the shortest" -> "shortest building"
					// This splits the last subject and replaces the first word (often an adjective).
					String[] lastSubjectParts = lastSubject.split("\\s+", 2);
					if (lastSubjectParts.length > 1) {
						// Reconstructs "shortest" + " " + "building in the world"
						newQuery = newSubject + " " + lastSubjectParts[1];
					} else {
						// Fallback if the last subject was just one word or the heuristic fails.
						newQuery = newSubject;
					}
				}
			} else {
				newQuery = newSubject;
			}
			break;
		default:
			return "I can't do a follow-up on that type of command. Please ask a new question.";
	}
	
	if (DEBUG_MODE) System.out.printf("[DEBUG] Follow-up: Rerouting to full pipeline with new query: '%s'%n", newQuery);
	// Re-invoke the entire reasoning pipeline with the new, improved query.
	return xavierCore.getResponse(newQuery, context);
}

/**
 * Uses regex to strip away conversational phrases and isolate the new subject.
 * @param input The raw user input.
 * @return The extracted subject, or null if it's empty.
 */
private String extractNewSubject(String input) {
	String trimmedInput = input.trim();
	Matcher matcher = FOLLOW_UP_SUBJECT_PATTERN.matcher(trimmedInput);
	if (matcher.matches()) {
		String subject = matcher.group(1).replace("?", "").trim();
		return subject.isEmpty() ? null : subject;
	}
	// If no pattern matches, the input itself is the subject.
	return trimmedInput.isEmpty() ? null : trimmedInput;
}
}