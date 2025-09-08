package com.f9ld3.xavier.ai.V2.utils;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * A utility class to filter user input for unsafe or inappropriate content.
 * This acts as a first line of defense before processing any requests.
 * This version is significantly more robust, incorporating a multi-layered
 * approach to content safety with expanded keywords and advanced regex.
 */
public class ContentSafetyFilter {

/**
 * A highly comprehensive and categorized list of keywords, phrases,
 * and coded language related to prohibited topics. This list is
 * dynamic and would be regularly updated in a production environment.
 */
private static final List<String> UNSAFE_KEYWORDS = Arrays.asList(
		// Explicit Content & Sexual Violence
		"porn", "sex", "nude", "naked", "xxx", "erotic", "hentai", "lust", "kink", "fetish", "bdsm",
		"orgasm", "climax", "intercourse", "sexual", "masturbate", "cunnilingus", "fellatio", "incest",
		"jizz", "prostitute", "escort", "trafficking", "sex slave", "groomer", "paedo",
		"rape", "sexual assault", "nonconsensual", "pedophile", "cp" // Child Pornography - acronyms are critical
		
		// Extreme Violence & Hate Speech
		, "kill", "murder", "slaughter", "torture", "abuse", "assault", "rape", "bomb", "terrorist",
		"massacre", "behead", "gory", "bloody", "violence", "shoot", "stab", "lynch", "genocide",
		"nazi", "supremacist", "racist", "kkk", "white power", "islamophobic", "antisemitic", "jihadist",
		"isis", "al-qaeda", "neo-nazi", "incel", "misogynist"
		
		// Explicit Self-Harm & Suicide
		, "suicide", "kill myself", "self harm", "self-harm", "overdose", "noose", "hang myself",
		"cut myself", "cutting", "razor blade", "burn myself", "anorexia", "bulimia", "pro-ana"
		
		// Illicit & Regulated Goods
		, "cocaine", "heroin", "meth", "fentanyl", "lsd", "mdma", "crack", "trafficking", "illegal gambling",
		"darknet market", "ghost gun", "unregistered gun", "molotov", "tnt"
);

/**
 * A more robust set of regex patterns to catch complex cases,
 * including URLs, phone numbers, and common misspellings.
 */
private static final List<String> UNSAFE_REGEX_PATTERNS = Arrays.asList(
		// Common misspellings and creative spelling
		"(?i)s\\W*e\\W*x", "(?i)p\\W*o\\W*r\\W*n", "(?i)n\\W*u\\W*d\\W*e"
		
		// URLs to known unsafe sites (This list would be massive and dynamic in production)
		, "(?i)(?:https?://)?(?:www\\.)?(pornhub\\.com|xvideos\\.com|stormfront\\.org|dailystormer\\.su|8chan\\.net|onlyfans\\.com|silkroad|darknet)"
		
		// Slurs (case-insensitive with flexible whitespace)
		, "(?i)n\\s*i\\s*g\\s*g\\s*e\\s*r", "(?i)k\\s*i\\s*k\\s*e"
		
		// Suicide & Self-Harm phrases
		, "(?i)k(?:i|1)l{2}\s+m(?:y|i)s(?:e|3)l{2}f" // Catching common typos
		
		// Coordinated hate or violence
		, "(?i)we\s+should\s+kill\s+them"

);

// We compile a single regex pattern for efficiency, combining both keywords and more complex regex.
private static final Pattern UNSAFE_PATTERN = Pattern.compile(
		"\\b(" + UNSAFE_KEYWORDS.stream().map(Pattern::quote).collect(Collectors.joining("|")) + ")\\b"
				+ "|"
				+ UNSAFE_REGEX_PATTERNS.stream().collect(Collectors.joining("|")),
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