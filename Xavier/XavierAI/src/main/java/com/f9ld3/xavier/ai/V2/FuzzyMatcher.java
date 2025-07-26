package com.f9ld3.xavier.ai.V2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A sophisticated fuzzy string matcher that finds the most likely intent
 * for a given input, even with typos. It is trained on the AI's full
 * dataset to provide a robust "best guess" capability.
 */
public class FuzzyMatcher {

// A record to cleanly store the result of a fuzzy match.
public record MatchResult(String matchedPhrase, String intent, double confidenceScore) {}

private final List<String> phrases = new ArrayList<>();
private final Map<String, String> phraseToIntentMap = new HashMap<>();

/**
 * NEW STATIC UTILITY METHOD: Finds the best match for an input from a given
 * list of candidates. Ideal for one-off checks like matching against direct commands.
 *
 * @param input The user's input string.
 * @param candidates An iterable of strings to match against.
 * @param similarityThreshold The minimum similarity score for a match to be considered valid.
 * @return An Optional containing the best-matching candidate string.
 */
public static Optional<String> findBestCandidate(String input, Iterable<String> candidates, double similarityThreshold) {
	String bestMatch = null;
	int lowestDistance = Integer.MAX_VALUE;
	
	for (String candidate : candidates) {
		int distance = calculateLevenshteinDistance(input, candidate);
		if (distance < lowestDistance) {
			lowestDistance = distance;
			bestMatch = candidate;
		}
	}
	
	if (bestMatch == null) {
		return Optional.empty();
	}
	
	double similarity = 1.0 - ((double) lowestDistance / Math.max(input.length(), bestMatch.length()));
	
	if (similarity >= similarityThreshold) {
		return Optional.of(bestMatch);
	}
	
	return Optional.empty();
}

/**
 * Trains the fuzzy matcher by loading it with all known phrases and their intents.
 * @param phrases A list of all raw training phrases.
 * @param intents The corresponding list of intents for each phrase.
 */
public void train(List<String> phrases, List<String> intents) {
	if (phrases.size() != intents.size()) {
		throw new IllegalArgumentException("Phrases and intents lists must be the same size.");
	}
	this.phrases.addAll(phrases);
	for (int i = 0; i < phrases.size(); i++) {
		this.phraseToIntentMap.put(phrases.get(i), intents.get(i));
	}
}

/**
 * Finds the best match for the input string from the trained phrases.
 * @param input The user's input string.
 * @param similarityThreshold A value between 0.0 and 1.0. A match is only returned if
 *                            its similarity score is above this threshold.
 * @return An Optional containing the MatchResult if a suitable match is found.
 */
public Optional<MatchResult> findBestMatch(String input, double similarityThreshold) {
	if (phrases.isEmpty()) {
		return Optional.empty();
	}
	
	String bestMatch = null;
	int lowestDistance = Integer.MAX_VALUE;
	
	for (String phrase : phrases) {
		int distance = calculateLevenshteinDistance(input, phrase);
		if (distance < lowestDistance) {
			lowestDistance = distance;
			bestMatch = phrase;
		}
	}
	
	if (bestMatch == null) {
		return Optional.empty();
	}
	
	double similarity = 1.0 - ((double) lowestDistance / Math.max(input.length(), bestMatch.length()));
	
	if (similarity >= similarityThreshold) {
		String intent = phraseToIntentMap.get(bestMatch);
		return Optional.of(new MatchResult(bestMatch, intent, similarity));
	}
	
	return Optional.empty();
}

/**
 * Calculates the Levenshtein distance between two strings, which is a measure of their difference.
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
