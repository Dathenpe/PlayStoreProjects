package com.f9ld3.xavier.ai.V2.handlers;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A powerful utility to match user input against a prioritized list of regex patterns.
 * This provides a reliable, rule-based layer for structured commands.
 * It checks patterns in the exact order they were registered, allowing for more
 * specific patterns to be prioritized over general ones.
 */
public final class PatternHandler {

// A private inner class to hold the intent and its compiled pattern.
private static class PatternEntry {
	final String intent;
	final Pattern pattern;
	
	PatternEntry(String intent, Pattern pattern) {
		this.intent = intent;
		this.pattern = pattern;
	}
}

// Use a List to preserve global registration order, which is crucial for priority.
private final List<PatternEntry> intentPatterns = new ArrayList<>();

/**
 * Registers a regex pattern for a specific intent. Patterns will be checked
 * in the order they are registered. Register more specific patterns first.
 * @param intent The name of the intent (e.g., "calculator_query").
 * @param regex The regular expression to match.
 */
public void registerPattern(String intent, String regex) {
	// The CASE_INSENSITIVE flag is crucial for natural language.
	Pattern compiledPattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
	this.intentPatterns.add(new PatternEntry(intent, compiledPattern));
}

/**
 * Attempts to match the user input against all registered patterns in order.
 * @param userInput The raw text from the user.
 * @return An Optional containing an IntentMatch if found, otherwise empty.
 */
public Optional<IntentMatch> match(String userInput) {
	for (PatternEntry entry : intentPatterns) {
		Matcher matcher = entry.pattern.matcher(userInput);
		if (matcher.find()) {
			// Return the intent and the first captured group (the entity).
			String entity = null;
			// Ensure a capturing group exists in the regex before trying to access it.
			if (matcher.groupCount() > 0) {
				String capturedGroup = matcher.group(1);
				if (capturedGroup != null && !capturedGroup.trim().isEmpty()) {
					entity = capturedGroup.trim();
				}
			}
			return Optional.of(new IntentMatch(entry.intent, entity));
		}
	}
	return Optional.empty(); // No pattern matched.
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