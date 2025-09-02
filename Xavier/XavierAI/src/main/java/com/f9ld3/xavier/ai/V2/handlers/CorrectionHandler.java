package com.f9ld3.xavier.ai.V2.handlers;

import com.f9ld3.xavier.ai.V2.ConversationContext;
import com.f9ld3.xavier.ai.V2.XavierCoreV2;

/**
 * Handles user corrections by taking a new subject and re-running the last failed query
 * with the updated context. This enables the AI to recover from misunderstandings.
 */
public class CorrectionHandler implements IntentHandler {

private final XavierCoreV2 core;

public CorrectionHandler(XavierCoreV2 core) {
	this.core = core;
}

@Override
public String handle(String userInput, ConversationContext context) {
	String lastFailedQuery = context.getLastFailedInput();
	
	if (lastFailedQuery == null || lastFailedQuery.isBlank()) {
		return "I'm not sure what we were trying to do. Could you please ask your question again?";
	}
	
	// The entity for the correction is the new subject, extracted by the PatternHandler in XavierCoreV2.
	String newSubject = (String) context.getEntity("correction");
	
	if (newSubject == null || newSubject.isBlank()) {
		// If the pattern didn't capture an entity (e.g., user just says "no, france"),
		// we treat the whole input as the correction.
		newSubject = userInput.replaceAll("(?i)^(no,|i mean|i meant)", "").trim();
	}
	
	if (newSubject.isBlank()) {
		return "I see you're trying to correct something, but I didn't catch the new topic. What did you mean?";
	}
	
	if (XavierCoreV2.DEBUG_MODE) {
		System.out.printf("[DEBUG] CorrectionHandler: Last failed query: '%s'%n", lastFailedQuery);
		System.out.printf("[DEBUG] CorrectionHandler: New subject from correction: '%s'%n", newSubject);
	}
	
	// Update the context with the new, corrected subject.
	context.setLastSubject(newSubject);
	
	// Clear the last failed input so we don't get stuck in a correction loop.
	context.clearLastFailedInput();
	
	// Re-route the *original failed query* back through the main pipeline.
	// This will likely trigger the FollowUpHandler again, which will now have
	// the correct subject ("france") to work with.
	return core.getResponse(lastFailedQuery, context);
}
}