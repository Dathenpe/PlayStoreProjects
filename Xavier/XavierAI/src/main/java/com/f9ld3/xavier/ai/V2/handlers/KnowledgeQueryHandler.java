package com.f9ld3.xavier.ai.V2.handlers;

import com.f9ld3.xavier.ai.V2.ConversationContext;
import com.f9ld3.xavier.ai.V2.WolframAlphaClient;
import com.f9ld3.xavier.ai.V2.XavierCoreV2;
import java.util.Optional;

/**
 * Handles general knowledge questions by querying the Wolfram|Alpha API.
 */
public class KnowledgeQueryHandler implements IntentHandler {

private final WolframAlphaClient wolframClient;

public KnowledgeQueryHandler(WolframAlphaClient wolframClient) {
	this.wolframClient = wolframClient;
}

@Override
public String handle(String userInput, ConversationContext context) {
	if (wolframClient == null) {
		return "I'm sorry, my knowledge base is currently unavailable.";
	}
	
	if (XavierCoreV2.DEBUG_MODE) {
		System.out.println("[DEBUG] KnowledgeQueryHandler: Sending query to Wolfram|Alpha: '" + userInput + "'");
	}
	
	// The client now returns an Optional, which is cleaner to handle.
	Optional<String> answerOpt = wolframClient.getShortAnswer(userInput);
	
	// The handler is now responsible for the final user-facing message.
	if (answerOpt.isPresent()) {
		context.clearLastFailedInput(); // Success, so clear any previous failure.
		// Clean up the response a bit for better readability
		return answerOpt.get().replace(" | ", ": ").replace("... | ", ". ");
	} else {
		// The API was reached, but it couldn't answer. This is a "soft" failure.
		context.setLastFailedInput(userInput); // Record the failed query for potential refinement.
		return "That's a great question, but I couldn't find a specific answer for it.";
	}
}
}