package com.f9ld3.xavier.ai.V2.handlers;

import com.f9ld3.xavier.ai.V2.ConversationContext;

import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Handles simple confirmation phrases like "yes" or "no".
 * REFINED: This handler is now more robust, distinguishing between positive
 * and negative confirmations and providing varied, appropriate responses.
 */
public class ConfirmationHandler implements IntentHandler {

// Keywords to identify the type of confirmation. Using Sets for efficient O(1) lookups.
private static final Set<String> POSITIVE_KEYWORDS = Set.of(
		"yes", "yep", "yeah", "correct", "right", "exactly", "affirmative", "you got it"
);

private static final Set<String> NEGATIVE_KEYWORDS = Set.of(
		"no", "nope", "wrong", "incorrect", "negative", "not quite", "not it"
);

// Varied responses to make the AI feel more natural.
private static final List<String> POSITIVE_RESPONSES = List.of(
		"Excellent!",
		"Great!",
		"Perfect, got it.",
		"Okay, sounds good."
);

private static final List<String> NEGATIVE_RESPONSES = List.of(
		"Okay, my mistake.",
		"Understood. Thanks for the correction.",
		"Noted. I'll adjust.",
		"My apologies."
);

private final Random random = new Random();

@Override
public String handle(String userInput, ConversationContext context) {
	String cleanedInput = userInput.toLowerCase().trim();
	
	// Check if any positive keyword is present in the user's input.
	boolean isPositive = POSITIVE_KEYWORDS.stream().anyMatch(cleanedInput::contains);
	if (isPositive) {
		return POSITIVE_RESPONSES.get(random.nextInt(POSITIVE_RESPONSES.size()));
	}
	
	// Check if any negative keyword is present.
	boolean isNegative = NEGATIVE_KEYWORDS.stream().anyMatch(cleanedInput::contains);
	if (isNegative) {
		return NEGATIVE_RESPONSES.get(random.nextInt(NEGATIVE_RESPONSES.size()));
	}
	
	// A sensible fallback if the input is ambiguous, though unlikely given the training data.
	return "Understood.";
}
}