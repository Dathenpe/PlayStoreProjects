package com.f9ld3.xavier.ai.V2;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * A utility for extracting named entities from text.
 * For now, it focuses on extracting locations.
 */
public class EntityExtractor {

private static final Set<String> KNOWN_LOCATIONS = new HashSet<>();

// A static initializer block to load the locations when the class is first used.
static {
	loadLocations("known_locations.txt");
}

private static void loadLocations(String resourceFileName) {
	try (InputStream is = EntityExtractor.class.getClassLoader().getResourceAsStream(resourceFileName);
	     BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
		String line;
		while ((line = reader.readLine()) != null) {
			if (!line.trim().isEmpty()) {
				KNOWN_LOCATIONS.add(line.trim().toLowerCase());
			}
		}
	} catch (Exception e) {
		System.err.println("FATAL: Could not load known locations from " + resourceFileName);
		e.printStackTrace();
	}
}

/**
 * Extracts the first known location found in a given text.
 * This is a simple dictionary-based approach.
 *
 * @param text The user's input sentence.
 * @return The name of the found location (e.g., "london"), or null if none are found.
 */
public static String extractLocation(String text) {
	String lowerCaseText = text.toLowerCase();
	for (String location : KNOWN_LOCATIONS) {
		// Use word boundaries (\b) to ensure we match whole words.
		// This prevents "paris" from matching inside "comparison".
		if (lowerCaseText.matches(".*\\b" + Pattern.quote(location) + "\\b.*")) {
			return location;
		}
	}
	return null;
}
}