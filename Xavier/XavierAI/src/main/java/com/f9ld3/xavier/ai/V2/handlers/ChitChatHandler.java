package com.f9ld3.xavier.ai.V2.handlers;

import com.f9ld3.xavier.ai.V2.ConversationContext;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Handles simple conversational fillers and affirmations.
 */
public class ChitChatHandler implements IntentHandler {

private static final List<String> RESPONSES = Arrays.asList(
		"Got it.",
		"Okay.",
		"Acknowledged."
);
private final Random random = new Random();

@Override
public String handle(String userInput, ConversationContext context) {
	// Return a random response to feel less robotic.
	return RESPONSES.get(random.nextInt(RESPONSES.size()));
}
}