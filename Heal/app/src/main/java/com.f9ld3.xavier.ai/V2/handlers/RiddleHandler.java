package com.f9ld3.xavier.ai.V2.handlers;

import android.util.Log; // Using Android's Log

import com.f9ld3.xavier.ai.V2.ConversationContext;
import com.f9ld3.xavier.ai.V2.services.RiddleService;
import com.f9ld3.xavier.ai.V2.services.RiddleService.Riddle; // Assuming Riddle is a class with answer() and question()

import java.util.Arrays;
import java.util.Collections; // For unmodifiableSet
import java.util.HashSet;     // For creating the set
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * A stateful handler for telling riddles. It uses the ConversationContext stack
 * to manage its state, making it fully interruptible.
 */
public class RiddleHandler implements IntentHandler {

	private static final String TAG = "RiddleHandler";
	private final RiddleService riddleService;

	private static final Pattern GIVE_UP_PATTERN = Pattern.compile(
			"(?i)what'?s the answer|what is the answer|i give up|i do[n']?t know"
	);

	// MODIFIED: Replaced Set.of() with a static initializer block for broader compatibility
	private static final Set<String> POSITIVE_KEYWORDS;

	static {
		Set<String> tempSet = new HashSet<>();
		tempSet.add("yes");
		tempSet.add("yep");
		tempSet.add("yeah");
		tempSet.add("ok");
		tempSet.add("okay");
		tempSet.add("sure");
		tempSet.add("please");
		tempSet.add("another");
		POSITIVE_KEYWORDS = Collections.unmodifiableSet(tempSet);
	}
	// Original way (requires Java 9+ or desugaring for Set.of()):
	// private static final Set<String> POSITIVE_KEYWORDS = Set.of(
	//     "yes", "yep", "yeah", "ok", "okay", "sure", "please", "another"
	// );


	public RiddleHandler(RiddleService riddleService) {
		if (riddleService == null) {
			throw new IllegalArgumentException("RiddleService cannot be null.");
		}
		this.riddleService = riddleService;
	}

	@Override
	public String handle(String userInput, ConversationContext context) {
		if (userInput == null || context == null) {
			Log.w(TAG, "User input or context is null.");
			return "I'm sorry, something went wrong."; // Or a more appropriate default
		}

		String currentIntent = context.getCurrentIntent().orElse("");
		String cleanedInput = userInput.trim().toLowerCase();
		Log.d(TAG, String.format("Handling intent: '%s', input: '%s'", currentIntent, cleanedInput));


		// --- State 1: A riddle is currently active ---
		if ("riddle_query".equals(currentIntent)) {
			Log.d(TAG, "State: riddle_query");
			// Ensure entity from context is handled safely if it's not a String or is null
			String pendingAnswer = "";
			Optional<Object> answerEntity = context.getEntityFromCurrentContext("answer");
			if (answerEntity.isPresent() && answerEntity.get() instanceof String) {
				pendingAnswer = (String) answerEntity.get();
			} else if (answerEntity.isPresent()) {
				pendingAnswer = String.valueOf(answerEntity.get()); // Fallback if not string
			}

			if (pendingAnswer.isEmpty()){
				Log.w(TAG, "Pending answer is empty in riddle_query state. This should not happen if a riddle is active.");
				// Potentially reset context or return an error
				context.popContext(); // Remove potentially corrupt state
				return "I seem to have forgotten the riddle. Let's try a new one!";
			}


			if (GIVE_UP_PATTERN.matcher(cleanedInput).find()) {
				Log.d(TAG, "User gives up.");
				return revealAnswerAndPromptForNext(context, pendingAnswer);
			}

			if (isGuessCorrect(cleanedInput, pendingAnswer)) {
				Log.d(TAG, "User guessed correctly: " + cleanedInput);
				context.popContext(); // End the riddle
				context.pushContext("riddle_confirmation"); // Ask if they want another
				return "You got it! The answer was: " + pendingAnswer + ".\n\nWould you like to try another one?";
			}
			Log.d(TAG, "User guess incorrect: " + cleanedInput);
			return "Not quite! Try again, or say 'I give up' if you're stumped.";
		}

		// --- State 2: Waiting for confirmation to play again ---
		if ("riddle_confirmation".equals(currentIntent)) {
			Log.d(TAG, "State: riddle_confirmation");
			context.popContext(); // This confirmation turn is now over.
			boolean wantsAnother = POSITIVE_KEYWORDS.stream().anyMatch(cleanedInput::contains);
			if (wantsAnother) {
				Log.d(TAG, "User wants another riddle.");
				return tellNewRiddle(context); // Start a new riddle
			} else {
				Log.d(TAG, "User does not want another riddle.");
				return "Okay, maybe next time!";
			}
		}

		// --- State 3: No riddle is active, so tell a new one ---
		Log.d(TAG, "State: No active riddle, telling a new one.");
		return tellNewRiddle(context);
	}

	private String tellNewRiddle(ConversationContext context) {
		// Assuming riddleService.getRiddle() does not perform network calls or is handled async by service
		Optional<Riddle> riddleOpt = riddleService.getRiddle();
		if (riddleOpt.isPresent()) {
			Riddle riddle = riddleOpt.get();
			// Assuming riddle.answer() and riddle.question() return non-null strings
			String answer = riddle.getAnswer() != null ? riddle.getAnswer() : "";
			String question = riddle.getQuestion() != null ? riddle.getQuestion() : "I thought of a riddle, but the question is missing!";

			if (answer.isEmpty() || question.isEmpty() || question.equals("I thought of a riddle, but the question is missing!")){
				Log.e(TAG, "Riddle object from service has missing answer or question. Riddle: " + riddle);
				return "I tried to think of a riddle, but it seems to be incomplete! Please try again later.";
			}

			context.pushContext("riddle_query"); // Push the new riddle state onto the stack
			context.addEntityToCurrentContext("answer", answer);
			Log.i(TAG, "Telling new riddle. Question: " + question.substring(0, Math.min(question.length(), 50)) + "...");
			return question + "\n\n(Say 'what's the answer?' or guess when you're ready.)";
		} else {
			Log.w(TAG, "RiddleService could not provide a riddle.");
			return "I tried to think of a riddle, but I'm stumped! Please try again later.";
		}
	}

	private String revealAnswerAndPromptForNext(ConversationContext context, String answer) {
		context.popContext(); // End the riddle
		context.pushContext("riddle_confirmation"); // Set state to wait for "yes/no"
		Log.d(TAG, "Revealing answer: " + answer);
		return "The answer was: " + answer + ".\n\nWould you like another one?";
	}

	private boolean isGuessCorrect(String guess, String correctAnswer) {
		if (guess == null || correctAnswer == null) return false;

		String normalizedGuess = guess.toLowerCase().replaceAll("[^a-z0-9\\s]", "").trim();
		String normalizedAnswer = correctAnswer.toLowerCase().replaceAll("[^a-z0-9\\s]", "").trim();

		if (normalizedGuess.isEmpty()) return false;
		if (normalizedAnswer.isEmpty()) {
			Log.w(TAG, "Correct answer normalized to empty string. Original: " + correctAnswer);
			return false; // Cannot be correct if the answer is effectively empty
		}

		// Split into words and convert to sets for comparison
		Set<String> guessWords = Arrays.stream(normalizedGuess.split("\\s+"))
				.filter(s -> !s.isEmpty())
				.collect(Collectors.toSet());
		Set<String> answerWords = Arrays.stream(normalizedAnswer.split("\\s+"))
				.filter(s -> !s.isEmpty())
				.collect(Collectors.toSet());

		// The guess is correct if all words in the answer are present in the guess.
		// This is a common way to check, but you might want all guess words to be in answer words,
		// or an exact match depending on how strict you want to be.
		// The original logic: return answerWords.containsAll(guessWords); means all guess words must be in the answer.
		// If "a big red car" is answer, "red car" is correct. "big car red" is correct. "car" is correct.
		// "big red car fast" is not correct.
		// Let's stick to the original logic:
		return answerWords.containsAll(guessWords);
	}
}

