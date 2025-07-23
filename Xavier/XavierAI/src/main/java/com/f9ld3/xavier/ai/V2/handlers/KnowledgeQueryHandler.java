package com.f9ld3.xavier.ai.V2.handlers;

import com.f9ld3.xavier.ai.V2.ConversationContext;
import com.f9ld3.xavier.ai.V2.WolframAlphaClient;

/**
 * Handles general knowledge questions by querying the Wolfram|Alpha API.
 * It now intelligently records failed queries to allow for user refinement.
 */
public class KnowledgeQueryHandler implements IntentHandler {

private final WolframAlphaClient wolframClient;

public KnowledgeQueryHandler(WolframAlphaClient wolframClient) {
	this.wolframClient = wolframClient;
}

@Override
public String handle(String userInput, ConversationContext context) {
	try {
		String answer = wolframClient.getShortAnswer(userInput);
		
		// If the API returns a valid, non-empty answer, it's a success.
		if (answer != null && !answer.trim().isEmpty() && !answer.contains("did not understand")) {
			context.clearLastFailedInput(); // Success, so clear any previous failure.
			return answer;
		} else {
			// The API was reached, but it couldn't answer. This is a "soft" failure.
			// CRITICAL: Record the failed query to allow for refinement.
			context.setLastFailedInput(userInput);
			return "That's a great question, but I couldn't find a specific answer for it.";
		}
	} catch (Exception e) {
		// The API could not be reached. This is a "hard" failure.
		System.err.println("Wolfram|Alpha Error: " + e.getMessage());
		// We also record this failure, as the user might try to rephrase.
		context.setLastFailedInput(userInput);
		return "I'm sorry, I had trouble connecting to my knowledge base at the moment.";
	}
}
}