package com.f9ld3.xavier.ai.V2.handlers;

import com.f9ld3.xavier.ai.V2.ConversationContext;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * A more dynamic and engaging handler for simple chit-chat and acknowledgements.
 * This handler provides varied responses to make the AI feel less repetitive.
 */
public class ChitChatHandler implements IntentHandler {

private static final List<String> RESPONSES = Arrays.asList(
		"Got it.",
		"Okay, sounds good.",
		"Understood!",
		"Right on.",
		"Cool.",
		"I'm with you."
);

private final Random random = new Random();

@Override
public String handle(String userInput, ConversationContext context) {
	// Return a random response from the list to seem more natural.
	return RESPONSES.get(random.nextInt(RESPONSES.size()));
}
}