package com.f9ld3.xavier.ai.V2.handlers;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A powerful utility to match user input against predefined regex patterns.
 * This provides a reliable, rule-based layer for structured commands.
 */
public final class PatternHandler {

private final Map<String, Pattern> intentPatterns = new HashMap<>();

/**
 * Registers a regex pattern for a specific intent.
 * @param intent The name of the intent (e.g., "set_username").
 * @param regex The regular expression to match.
 */
public void registerPattern(String intent, String regex) {
	intentPatterns.put(intent, Pattern.compile(regex, Pattern.CASE_INSENSITIVE));
}

/**
 * Attempts to match the user input against all registered patterns.
 * @param userInput The raw text from the user.
 * @return An Optional containing an IntentMatch if found, otherwise empty.
 */
public Optional<IntentMatch> match(String userInput) {
	for (Map.Entry<String, Pattern> entry : intentPatterns.entrySet()) {
		Matcher matcher = entry.getValue().matcher(userInput);
		if (matcher.find()) {
			// Return the intent and the first captured group (the entity)
			String entity = matcher.groupCount() > 0 ? matcher.group(1).trim() : null;
			return Optional.of(new IntentMatch(entry.getKey(), entity));
		}
	}
	return Optional.empty();
}

/**
 * A simple data class to hold the result of a successful pattern match.
 */
public static class IntentMatch {
	private final String intent;
	private final String entity;
	
	public IntentMatch(String intent, String entity) {
		this.intent = intent;
		this.entity = entity;
	}
	
	public String getIntent() {
		return intent;
	}
	
	public String getEntity() {
		return entity;
	}
}
}