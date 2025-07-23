package com.f9ld3.xavier.ai.V2.handlers;

import com.f9ld3.xavier.ai.V2.ConversationContext;
import java.util.List;
import java.util.Random;

/**
 * Handles expressions of gratitude from the user with a polite, varied response.
 */
public class GratitudeHandler implements IntentHandler {

private static final List<String> RESPONSES = List.of(
		"You're welcome!",
		"Happy to help!",
		"Anytime!",
		"No problem at all.",
		"My pleasure.",
		"Glad I could assist!",
		"You're very welcome!",
		"It's my pleasure to help."
);
private final Random random = new Random();

@Override
public String handle(String userInput, ConversationContext context) {
	// Return a random response to make the conversation feel more natural.
	return RESPONSES.get(random.nextInt(RESPONSES.size()));
}
}