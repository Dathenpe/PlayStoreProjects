// C:/Users/Music_Minister/Desktop/PlayStore/PlayStoreProjects/Xavier/XavierAI/src/main/java/com/f9ld3/xavier/ai/V2/handlers/ChitChatHandler.java
package com.f9ld3.xavier.ai.V2.handlers;

import com.f9ld3.xavier.ai.V2.ConversationContext;

import java.util.List;
import java.util.Random;

/**
 * A more dynamic and engaging handler for simple chit-chat, acknowledgements,
 * and questions about the bot's status. This handler provides varied and
 * contextually-aware responses to make the AI feel less repetitive.
 */
public class ChitChatHandler implements IntentHandler {

// Responses for general acknowledgements (e.g., "ok", "cool", "i see")
private static final List<String> ACKNOWLEDGEMENT_RESPONSES = List.of(
		"Got it.",
		"Okay, sounds good.",
		"Understood!",
		"Right on.",
		"I'm with you."
);

// Responses for when the user asks about the bot's status (e.g., "how are you?")
private static final List<String> BOT_STATUS_RESPONSES = List.of(
		"I'm operating at full capacity! Thanks for asking.",
		"All systems are online and ready to assist.",
		"I'm a program, so I don't have feelings, but I'm fully operational and here to help!",
		"Functioning perfectly, thank you. What can I do for you?"
);

private final Random random = new Random();

@Override
public String handle(String userInput, ConversationContext context) {
	String cleanedInput = userInput.toLowerCase().trim();
	
	// Check if the user is asking about the bot's status.
	if (isBotStatusQuery(cleanedInput)) {
		return BOT_STATUS_RESPONSES.get(random.nextInt(BOT_STATUS_RESPONSES.size()));
	}
	
	// Otherwise, provide a general acknowledgement.
	return ACKNOWLEDGEMENT_RESPONSES.get(random.nextInt(ACKNOWLEDGEMENT_RESPONSES.size()));
}

/**
 * Checks if the input is a query about the bot's well-being.
 * @param cleanedInput The lowercased, trimmed user input.
 * @return true if it's a status query, false otherwise.
 */
private boolean isBotStatusQuery(String cleanedInput) {
	// This check is more robust than a simple keyword list.
	return cleanedInput.startsWith("how are you") ||
			       cleanedInput.startsWith("how's it going") ||
			       cleanedInput.equals("how about you");
}
}