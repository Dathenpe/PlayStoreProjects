package com.f9ld3.xavier.ai.V2.handlers;

import com.f9ld3.xavier.ai.V2.ConversationContext;
import com.f9ld3.xavier.ai.V2.services.RiddleService;
import com.f9ld3.xavier.ai.V2.services.RiddleService.Riddle;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * A stateful handler for telling riddles. It asks a question, evaluates user guesses,
 * handles "give up" commands, and manages the conversation flow via the context.
 */
public class RiddleHandler implements IntentHandler {

private final RiddleService riddleService;

// UPDATED: This pattern is now more flexible, handling both "don't" and "dont".
private static final Pattern GIVE_UP_PATTERN = Pattern.compile(
		"(?i)what's the answer|what is the answer|i give up|i do[n']?t know"
);
private static final Set<String> POSITIVE_KEYWORDS = Set.of(
		"yes", "yep", "yeah", "ok", "okay", "sure", "please", "another"
);

public RiddleHandler(RiddleService riddleService) {
	this.riddleService = riddleService;
}

@Override
public String handle(String userInput, ConversationContext context) {
	String pendingAnswer = context.getLastRiddleAnswer();
	
	// --- State 1: A riddle is currently active ---
	if (pendingAnswer != null) {
		String cleanedInput = userInput.trim().toLowerCase();
		
		// Case 1a: User is giving up.
		if (GIVE_UP_PATTERN.matcher(cleanedInput).find()) {
			return revealAnswerAndPromptForNext(context);
		}
		
		// Case 1b: User guessed correctly using the robust token-based logic.
		if (isGuessCorrect(cleanedInput, pendingAnswer)) {
			context.clearRiddleContext();
			context.setPendingIntent("riddle_confirmation"); // Ask if they want another
			return "You got it! The answer was: " + pendingAnswer + ".\n\nWould you like to try another one?";
		}
		
		// Case 1c: User guessed incorrectly.
		return "Not quite! Try again, or say 'I give up' if you're stumped.";
	}
	
	// --- State 2: Waiting for confirmation to play again ---
	if ("riddle_confirmation".equals(context.getPendingIntent())) {
		String cleanedInput = userInput.trim().toLowerCase();
		// UPDATED: More flexible check for positive confirmation. Handles "yes please".
		String[] positiveKeywords = {"yes", "yep", "yeah", "ok", "okay", "sure", "please", "another"};
		boolean wantsAnother = POSITIVE_KEYWORDS.stream().anyMatch(cleanedInput::contains);
		
		if (wantsAnother) {
			// Fall through to tell a new riddle
			context.clearPendingIntent();
		} else {
			// Assume they don't want another.
			context.clearPendingIntent();
			return "Okay, maybe next time!";
		}
	}
	
	// --- State 3: No riddle is active, so tell a new one ---
	return tellNewRiddle(context);
}

/**
 * Helper method to tell a new riddle and set the context.
 */
private String tellNewRiddle(ConversationContext context) {
	Optional<Riddle> riddleOpt = riddleService.getRiddle();
	if (riddleOpt.isPresent()) {
		Riddle riddle = riddleOpt.get();
		context.setLastRiddleAnswer(riddle.answer());
		context.setPendingIntent("riddle_query"); // Set the pending intent for the next turn
		return riddle.question() + "\n\n(Say 'what's the answer?' or guess when you're ready.)";
	} else {
		return "I tried to think of a riddle, but I'm stumped! Please try again later.";
	}
}

/**
 * Helper method to reveal the answer and set the context to ask for another.
 */
private String revealAnswerAndPromptForNext(ConversationContext context) {
	String answer = context.getLastRiddleAnswer();
	context.clearRiddleContext();
	context.setPendingIntent("riddle_confirmation"); // Set state to wait for "yes/no"
	return "The answer was: " + answer + ".\n\nWould you like another one?";
}

/**
 * A more robust method to check if a user's guess is correct.
 * It tokenizes both the guess and the answer and checks if the answer's words
 * contain all of the guess's words, avoiding simple substring pitfalls.
 *
 * @param guess         The user's input.
 * @param correctAnswer The correct answer.
 * @return True if the guess is deemed correct, false otherwise.
 */
private boolean isGuessCorrect(String guess, String correctAnswer) {
	// Normalize both strings: lowercase, remove all non-alphanumeric characters except spaces.
	String normalizedGuess = guess.toLowerCase().replaceAll("[^a-z0-9\\s]", "").trim();
	String normalizedAnswer = correctAnswer.toLowerCase().replaceAll("[^a-z0-9\\s]", "").trim();
	
	if (normalizedGuess.isEmpty()) {
		return false;
	}
	
	// Create sets of unique words from the guess and the answer.
	Set<String> guessWords = Arrays.stream(normalizedGuess.split("\\s+"))
			                         .filter(s -> !s.isEmpty())
			                         .collect(Collectors.toSet());
	
	Set<String> answerWords = Arrays.stream(normalizedAnswer.split("\\s+"))
			                          .filter(s -> !s.isEmpty())
			                          .collect(Collectors.toSet());
	
	// The guess is correct if the set of answer words contains all of the words from the guess.
	return answerWords.containsAll(guessWords);
}
}