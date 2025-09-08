// C:/Users/Music_Minister/Desktop/PlayStore/PlayStoreProjects/Xavier/XavierAI/src/main/java/com/f9ld3/xavier/ai/V2/handlers/RiddleHandler.java
package com.f9ld3.xavier.ai.V2.handlers;

import com.f9ld3.xavier.ai.V2.ConversationContext;
import com.f9ld3.xavier.ai.V2.services.RiddleService;
import com.f9ld3.xavier.ai.V2.services.RiddleService.Riddle;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * A stateful handler for telling riddles. It uses the ConversationContext stack
 * to manage its state, making it fully interruptible.
 */
public class RiddleHandler implements IntentHandler {

private final RiddleService riddleService;

private static final Pattern GIVE_UP_PATTERN = Pattern.compile(
		"(?i)what'?s the answer|what is the answer|i give up|i do[n']?t know" // MODIFIED: Added the optional apostrophe
);
private static final Set<String> POSITIVE_KEYWORDS = Set.of(
		"yes", "yep", "yeah", "ok", "okay", "sure", "please", "another"
);

public RiddleHandler(RiddleService riddleService) {
	this.riddleService = riddleService;
}

@Override
public String handle(String userInput, ConversationContext context) {
	String currentIntent = context.getCurrentIntent().orElse("");
	String cleanedInput = userInput.trim().toLowerCase();
	
	// --- State 1: A riddle is currently active ---
	if ("riddle_query".equals(currentIntent)) {
		String pendingAnswer = context.getEntityFromCurrentContext("answer").map(String::valueOf).orElse("");
		
		if (GIVE_UP_PATTERN.matcher(cleanedInput).find()) {
			return revealAnswerAndPromptForNext(context, pendingAnswer);
		}
		
		if (isGuessCorrect(cleanedInput, pendingAnswer)) {
			context.popContext(); // End the riddle
			context.pushContext("riddle_confirmation"); // Ask if they want another
			return "You got it! The answer was: " + pendingAnswer + ".\n\nWould you like to try another one?";
		}
		
		return "Not quite! Try again, or say 'I give up' if you're stumped.";
	}
	
	// --- State 2: Waiting for confirmation to play again ---
	if ("riddle_confirmation".equals(currentIntent)) {
		context.popContext(); // This confirmation turn is now over.
		boolean wantsAnother = POSITIVE_KEYWORDS.stream().anyMatch(cleanedInput::contains);
		if (wantsAnother) {
			return tellNewRiddle(context); // Start a new riddle
		} else {
			return "Okay, maybe next time!";
		}
	}
	
	// --- State 3: No riddle is active, so tell a new one ---
	return tellNewRiddle(context);
}

private String tellNewRiddle(ConversationContext context) {
	Optional<Riddle> riddleOpt = riddleService.getRiddle();
	if (riddleOpt.isPresent()) {
		Riddle riddle = riddleOpt.get();
		context.pushContext("riddle_query"); // Push the new riddle state onto the stack
		context.addEntityToCurrentContext("answer", riddle.answer());
		return riddle.question() + "\n\n(Say 'what's the answer?' or guess when you're ready.)";
	} else {
		return "I tried to think of a riddle, but I'm stumped! Please try again later.";
	}
}

private String revealAnswerAndPromptForNext(ConversationContext context, String answer) {
	context.popContext(); // End the riddle
	context.pushContext("riddle_confirmation"); // Set state to wait for "yes/no"
	return "The answer was: " + answer + ".\n\nWould you like another one?";
}

private boolean isGuessCorrect(String guess, String correctAnswer) {
	String normalizedGuess = guess.toLowerCase().replaceAll("[^a-z0-9\\s]", "").trim();
	String normalizedAnswer = correctAnswer.toLowerCase().replaceAll("[^a-z0-9\\s]", "").trim();
	if (normalizedGuess.isEmpty()) return false;
	Set<String> guessWords = Arrays.stream(normalizedGuess.split("\\s+")).filter(s -> !s.isEmpty()).collect(Collectors.toSet());
	Set<String> answerWords = Arrays.stream(normalizedAnswer.split("\\s+")).filter(s -> !s.isEmpty()).collect(Collectors.toSet());
	return answerWords.containsAll(guessWords);
}
}