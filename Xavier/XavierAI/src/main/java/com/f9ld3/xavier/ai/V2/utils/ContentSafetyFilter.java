package com.f9ld3.xavier.ai.V2.utils;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * A utility class to filter user input for unsafe or inappropriate content.
 * This acts as a first line of defense before processing any requests.
 */
public class ContentSafetyFilter {

// A list of keywords related to prohibited topics.
// In a production system, this list would be much more extensive.
private static final List<String> UNSAFE_KEYWORDS = List.of(
		// Explicit Content
		"porn", "sex", "nude", "naked", "xxx", "erotic", "hentai", "lust", "kink", "fetish", "bdsm",
		"orgasm", "climax", "intercourse", "sexual",
		// Extreme Violence & Hate Speech
		"kill", "murder", "slaughter", "torture", "abuse", "assault", "rape", "bomb", "terrorist",
		"massacre", "behead", "gory", "bloody", "violence", "nazi", "supremacist", "racist",
		// Explicit Self-Harm
		"suicide", "kill myself", "self harm", "self-harm"
);

// We compile a single regex pattern for efficiency.
// The \b ensures we match whole words only, preventing false positives (e.g., "grape" in "g-rape").
private static final Pattern UNSAFE_PATTERN = Pattern.compile(
		"\\b(" + UNSAFE_KEYWORDS.stream().map(Pattern::quote).collect(Collectors.joining("|")) + ")\\b",
		Pattern.CASE_INSENSITIVE
);

/**
 * Checks if the user input contains any prohibited content.
 *
 * @param userInput The text to check.
 * @return {@code true} if the input is considered safe, {@code false} otherwise.
 */
public static boolean isSafe(String userInput) {
	if (userInput == null || userInput.isBlank()) {
		return true; // Empty or null input is considered safe.
	}
	// If the pattern finds a match, the content is not safe.
	return !UNSAFE_PATTERN.matcher(userInput).find();
}
}