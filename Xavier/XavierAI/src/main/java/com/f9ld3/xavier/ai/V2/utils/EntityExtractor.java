// C:/Users/Music_Minister/Desktop/PlayStore/PlayStoreProjects/Xavier/XavierAI/src/main/java/com/f9ld3/xavier/ai/V2/utils/EntityExtractor.java
package com.f9ld3.xavier.ai.V2.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A utility class for extracting specific pieces of information (entities)
 * from raw user input.
 */
public final class EntityExtractor {

// FIX: This pattern is now non-greedy (.+?) and optionally matches (but does not capture)
// common temporal adverbs at the end of the string.
private static final Pattern LOCATION_PATTERN = Pattern.compile(
		".*\\b(?:in|for|at|of|near)\\b\\s+(.+?)(?:\\s+(?:right now|currently|today|now))?$",
		Pattern.CASE_INSENSITIVE
);

private EntityExtractor() {
	// Private constructor for utility class
}

/**
 * Extracts a location name from a user's query.
 * It first tries to find a location following a preposition (e.g., "in", "for").
 * If that fails, it falls back to stripping common command phrases.
 *
 * @param userInput The raw input from the user.
 * @return The extracted location name.
 */
public static String extractLocation(String userInput) {
	String cleanedInput = userInput.trim().replaceAll("\\?$", ""); // Remove trailing question mark
	Matcher matcher = LOCATION_PATTERN.matcher(cleanedInput);
	
	if (matcher.matches()) {
		// If a preposition is found, return the text that follows it.
		// The optional group for temporal adverbs is ignored, cleaning the output.
		return matcher.group(1).trim();
	} else {
		// If no preposition is found, try to strip known command phrases as a fallback.
		String stripped = cleanedInput
				                  .replaceFirst("(?i)^what's the weather like\\s*", "")
				                  .replaceFirst("(?i)^what is the weather\\s*", "")
				                  .replaceFirst("(?i)^how is the weather\\s*", "")
				                  .replaceFirst("(?i)^what is the time\\s*", "")
				                  .replaceFirst("(?i)^weather\\s*", "")
				                  .replaceFirst("(?i)^time\\s*", "")
				                  .replaceFirst("(?i)^forecast\\s*", "")
				                  .trim();
		// If stripping results in a non-empty string, use it. Otherwise, fall back to the original input.
		return !stripped.isEmpty() ? stripped : cleanedInput;
	}
}
}