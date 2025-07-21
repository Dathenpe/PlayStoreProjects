package com.f9ld3.xavier.ai.V2;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A more robust utility class for extracting named entities from user input.
 */
public final class EntityExtractor {

// This pattern looks for common prepositions and captures the word(s) that follow.
// It's designed to be resilient to typos in other parts of the sentence.
// Example: It will find "japan" in "what os the time in japan"
private static final Pattern LOCATION_PATTERN = Pattern.compile(
		"\\b(in|for|of|at)\\s+([a-zA-Z\\s]+?)(?:\\s+(right now|currently|today))?$",
		Pattern.CASE_INSENSITIVE
);

private EntityExtractor() {}

/**
 * Extracts a location (like a city or country) from a user's query.
 * @param userInput The full text from the user.
 * @return The extracted location as a String, or null if none is found.
 */
public static String extractLocation(String userInput) {
	Matcher matcher = LOCATION_PATTERN.matcher(userInput);
	if (matcher.find()) {
		// Group 2 of our pattern contains the captured location name.
		return matcher.group(2).trim();
	}
	// This fallback helps with simple, one-word follow-up questions like "london".
	if (userInput.trim().matches("^[a-zA-Z\\s]+$")) {
		return userInput.trim();
	}
	return null;
}
}