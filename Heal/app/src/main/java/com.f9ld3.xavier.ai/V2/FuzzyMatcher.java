package com.f9ld3.xavier.ai.V2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects; // For Objects.equals and Objects.hash
import java.util.Optional;

/**
 * A sophisticated fuzzy string matcher that finds the most likely intent
 * for a given input, even with typos. It is trained on the AI's full
 * dataset to provide a robust "best guess" capability.
 */
public class FuzzyMatcher {

	// MODIFIED: Replaced 'record' with a traditional final class
	public static final class MatchResult {
		private final String matchedPhrase;
		private final String intent;
		private final double confidenceScore;

		public MatchResult(String matchedPhrase, String intent, double confidenceScore) {
			this.matchedPhrase = matchedPhrase;
			this.intent = intent;
			this.confidenceScore = confidenceScore;
		}

		public String getMatchedPhrase() {
			return matchedPhrase;
		}

		public String getIntent() {
			return intent;
		}

		public double getConfidenceScore() {
			return confidenceScore;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			MatchResult that = (MatchResult) o;
			return Double.compare(that.confidenceScore, confidenceScore) == 0 &&
					Objects.equals(matchedPhrase, that.matchedPhrase) &&
					Objects.equals(intent, that.intent);
		}

		@Override
		public int hashCode() {
			return Objects.hash(matchedPhrase, intent, confidenceScore);
		}

		@Override
		public String toString() {
			return "MatchResult{" +
					"matchedPhrase='" + matchedPhrase + '\'' +
					", intent='" + intent + '\'' +
					", confidenceScore=" + confidenceScore +
					'}';
		}
	}

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

		if (input == null || candidates == null) { // Added null checks for robustness
			return Optional.empty();
		}
		String lowerInput = input.toLowerCase(); // Lowercase once

		for (String candidate : candidates) {
			if (candidate == null) continue; // Skip null candidates
			int distance = calculateLevenshteinDistance(lowerInput, candidate.toLowerCase());
			if (distance < lowestDistance) {
				lowestDistance = distance;
				bestMatch = candidate;
			}
		}

		if (bestMatch == null) {
			return Optional.empty();
		}

		// Ensure no division by zero if input or bestMatch is empty
		int inputLength = lowerInput.length();
		int bestMatchLength = bestMatch.length();
		if (Math.max(inputLength, bestMatchLength) == 0) {
			// If both are empty, they are perfectly similar if lowestDistance is 0
			return lowestDistance == 0 && similarityThreshold <= 1.0 ? Optional.of(bestMatch) : Optional.empty();
		}


		double similarity = 1.0 - ((double) lowestDistance / Math.max(inputLength, bestMatchLength));

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
		if (phrases == null || intents == null || phrases.size() != intents.size()) { // Added null checks
			throw new IllegalArgumentException("Phrases and intents lists must not be null and must be the same size.");
		}
		this.phrases.clear(); // Clear previous training data
		this.phraseToIntentMap.clear();

		this.phrases.addAll(phrases);
		for (int i = 0; i < phrases.size(); i++) {
			if (phrases.get(i) != null && intents.get(i) != null) { // Ensure keys and values are not null
				this.phraseToIntentMap.put(phrases.get(i), intents.get(i));
			}
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
		if (phrases.isEmpty() || input == null) { // Added null check for input
			return Optional.empty();
		}

		String bestMatchPhrase = null;
		int lowestDistance = Integer.MAX_VALUE;
		String lowerInput = input.toLowerCase(); // Lowercase once

		for (String phrase : phrases) {
			if (phrase == null) continue; // Skip null phrases in training data
			int distance = calculateLevenshteinDistance(lowerInput, phrase.toLowerCase());
			if (distance < lowestDistance) {
				lowestDistance = distance;
				bestMatchPhrase = phrase;
			}
		}

		if (bestMatchPhrase == null) {
			return Optional.empty();
		}

		int inputLength = lowerInput.length();
		int bestMatchPhraseLength = bestMatchPhrase.length();

		if (Math.max(inputLength, bestMatchPhraseLength) == 0) {
			// If both are empty, they are perfectly similar if lowestDistance is 0
			if (lowestDistance == 0 && similarityThreshold <= 1.0) {
				String intent = phraseToIntentMap.get(bestMatchPhrase);
				if (intent != null) { // Check if intent exists for the (empty) phrase
					return Optional.of(new MatchResult(bestMatchPhrase, intent, 1.0));
				}
			}
			return Optional.empty();
		}


		double similarity = 1.0 - ((double) lowestDistance / Math.max(inputLength, bestMatchPhraseLength));

		if (similarity >= similarityThreshold) {
			String intent = phraseToIntentMap.get(bestMatchPhrase);
			if (intent == null) {
				// This case should ideally not happen if training data is consistent,
				// but good to handle. It means a phrase was found but had no corresponding intent.
				// System.err.println("Warning: No intent found for matched phrase: " + bestMatchPhrase);
				return Optional.empty();
			}
			return Optional.of(new MatchResult(bestMatchPhrase, intent, similarity));
		}

		return Optional.empty();
	}

	/**
	 * Calculates the Levenshtein distance between two strings, which is a measure of their difference.
	 * Handles null or empty strings gracefully.
	 */
	private static int calculateLevenshteinDistance(String s1, String s2) {
		// Handle nulls by treating them as empty strings for distance calculation
		String str1 = (s1 == null) ? "" : s1;
		String str2 = (s2 == null) ? "" : s2;

		// If one string is empty, the distance is the length of the other string
		if (str1.isEmpty()) {
			return str2.length();
		}
		if (str2.isEmpty()) {
			return str1.length();
		}

		int[] costs = new int[str2.length() + 1];
		for (int i = 0; i <= str1.length(); i++) {
			int lastValue = i;
			for (int j = 0; j <= str2.length(); j++) {
				if (i == 0) {
					costs[j] = j;
				} else {
					if (j > 0) {
						int newValue = costs[j - 1];
						if (str1.charAt(i - 1) != str2.charAt(j - 1)) {
							newValue = Math.min(Math.min(newValue, lastValue), costs[j]) + 1;
						}
						costs[j - 1] = lastValue;
						lastValue = newValue;
					}
				}
			}
			if (i > 0) {
				costs[str2.length()] = lastValue;
			}
		}
		return costs[str2.length()];
	}
}

