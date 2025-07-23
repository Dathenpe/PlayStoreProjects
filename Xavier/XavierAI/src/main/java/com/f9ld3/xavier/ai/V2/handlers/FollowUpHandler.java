package com.f9ld3.xavier.ai.V2.handlers;

import com.f9ld3.xavier.ai.V2.ConversationContext;
import com.f9ld3.xavier.ai.V2.XavierCoreV2;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
		return "I'm not sure how to follow up on that. Could you please be more specific?";
	}
	
	String newQuery;
	switch (lastIntent) {
		case "weather_query":
			newQuery = "what is the weather in " + newSubject;
			break;
		case "timezone_query":
			newQuery = "what is the time in " + newSubject;
			break;
		case "knowledge_query":
			// For knowledge queries, combine the last subject with the new one.
			// This allows for "who is obama" -> "where was he born"
			if (lastSubject != null) {
				// A simple heuristic to see if the follow-up is a question.
				if (newSubject.matches("^(who|what|where|when|why|how|is|are|was|were|do|does|did).*")) {
					newQuery = newSubject + " " + lastSubject;
				} else {
					newQuery = newSubject;
				}
			} else {
				newQuery = newSubject;
			}
			break;
		default:
			return "I can't do a follow-up on that type of command. Please ask a new question.";
	}
	
	System.out.printf("[DEBUG] Follow-up: Rerouting to full pipeline with new query: '%s'%n", newQuery);
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