package com.f9ld3.xavier.ai.V2.services;

import java.util.List;
import java.util.Set;

/**
 * A service to analyze the sentiment of user input.
 * This allows the AI to understand the user's emotional tone and potentially
 * adapt its responses accordingly.
 */
public class SentimentAnalysisService {

// Using Sets for efficient O(1) lookups.
private static final Set<String> POSITIVE_WORDS = Set.of(
		"good", "great", "excellent", "awesome", "fantastic", "love", "happy",
		"pleased", "satisfied", "correct", "perfect", "nice", "thanks", "thank you"
);

private static final Set<String> NEGATIVE_WORDS = Set.of(
		"bad", "terrible", "awful", "hate", "sad", "angry", "wrong", "incorrect",
		"stupid", "useless", "disappointed", "no", "nope"
);

/**
 * Calculates a sentiment score for a given text.
 *
 * @param userInput The text to analyze.
 * @return A double value from -1.0 (very negative) to 1.0 (very positive).
 *         A score of 0.0 is neutral.
 */
public double getSentimentScore(String userInput) {
	if (userInput == null || userInput.isBlank()) {
		return 0.0;
	}
	
	String[] words = userInput.toLowerCase().split("\\s+");
	int positiveCount = 0;
	int negativeCount = 0;
	
	for (String word : words) {
		if (POSITIVE_WORDS.contains(word)) {
			positiveCount++;
		} else if (NEGATIVE_WORDS.contains(word)) {
			negativeCount++;
		}
	}
	
	int totalWords = words.length;
	if (totalWords == 0) {
		return 0.0;
	}
	
	// Normalize the score based on the number of emotional words found.
	int emotionalWordCount = positiveCount + negativeCount;
	if (emotionalWordCount == 0) {
		return 0.0; // Neutral sentiment
	}
	
	return (double) (positiveCount - negativeCount) / emotionalWordCount;
}
}