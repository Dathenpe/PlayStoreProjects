package com.f9ld3.xavier.ai.V2;

import java.util.Set;

/**
 * A utility to find the best approximate match for a string from a set of candidates.
 * Uses the Levenshtein distance algorithm to provide typo tolerance.
 */
public final class FuzzyMatcher {

// Increase the threshold to 2 to allow for more forgiving typo correction.
private static final int MAX_DISTANCE_THRESHOLD = 2;

private FuzzyMatcher() {}

/**
 * Finds the best fuzzy match for an input string from a set of known phrases.
 *
 * @param input The user's input string.
 * @param candidates A set of known phrases to match against.
 * @return The best matching phrase from the candidates, or null if no close match is found.
 */
public static String getBestMatch(String input, Set<String> candidates) {
	String bestMatch = null;
	int minDistance = Integer.MAX_VALUE;
	
	for (String candidate : candidates) {
		int distance = calculateLevenshteinDistance(input, candidate);
		if (distance < minDistance) {
			minDistance = distance;
			bestMatch = candidate;
		}
	}
	
	// Only return the match if it's within our acceptable typo threshold.
	if (minDistance <= MAX_DISTANCE_THRESHOLD) {
		return bestMatch;
	}
	
	return null;
}

/**
 * Calculates the Levenshtein distance between two strings.
 * This is the number of edits (insertions, deletions, substitutions) to change s1 to s2.
 */
private static int calculateLevenshteinDistance(String s1, String s2) {
	s1 = s1.toLowerCase();
	s2 = s2.toLowerCase();
	int[] costs = new int[s2.length() + 1];
	for (int i = 0; i <= s1.length(); i++) {
		int lastValue = i;
		for (int j = 0; j <= s2.length(); j++) {
			if (i == 0) {
				costs[j] = j;
			} else {
				if (j > 0) {
					int newValue = costs[j - 1];
					if (s1.charAt(i - 1) != s2.charAt(j - 1)) {
						newValue = Math.min(Math.min(newValue, lastValue), costs[j]) + 1;
					}
					costs[j - 1] = lastValue;
					lastValue = newValue;
				}
			}
		}
		if (i > 0) {
			costs[s2.length()] = lastValue;
		}
	}
	return costs[s2.length()];
}
}