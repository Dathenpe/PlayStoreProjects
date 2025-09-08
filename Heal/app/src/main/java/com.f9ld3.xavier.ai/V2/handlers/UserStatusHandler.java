// C:/Users/Music_Minister/Desktop/PlayStore/PlayStoreProjects/Xavier/XavierAI/src/main/java/com/f9ld3/xavier/ai/V2/handlers/UserStatusHandler.java
package com.f9ld3.xavier.ai.V2.handlers;

import com.f9ld3.xavier.ai.V2.ConversationContext;
import com.f9ld3.xavier.ai.V2.XavierCoreV2;

import java.util.List;
import java.util.Random;

/**
 * A handler to provide more engaging and helpful responses to user statements
 * about their state (e.g., "I'm bored", "I'm hungry", "I'm sad").
 * This handler is designed to be proactive and empathetic.
 */
public class UserStatusHandler implements IntentHandler {

private final XavierCoreV2 core;
private final Random random = new Random();

// --- Keyword Lists for Better Matching ---
private static final List<String> SAD_KEYWORDS = List.of("sad", "unhappy", "down", "depressed");
private static final List<String> DISTRESS_KEYWORDS = List.of("broke up", "heartbroken", "lonely", "grieving");
private static final List<String> BORED_KEYWORDS = List.of("bored", "boring");
private static final List<String> HUNGRY_KEYWORDS = List.of("hungry", "starving", "famished");
private static final List<String> TIRED_KEYWORDS = List.of("tired", "sleepy", "exhausted");
private static final List<String> HAPPY_KEYWORDS = List.of("happy", "excited", "great", "fantastic");
private static final List<String> CURIOUS_KEYWORDS = List.of("curious");
private static final List<String> CONFUSED_KEYWORDS = List.of("confused", "don't understand");

public UserStatusHandler(XavierCoreV2 core) {
	this.core = core;
}

private boolean containsKeyword(String input, List<String> keywords) {
	return keywords.stream().anyMatch(input::contains);
}

@Override
public String handle(String userInput, ConversationContext context) {
	String cleanedInput = userInput.toLowerCase();
	
	// Prioritize more serious topics first
	if (containsKeyword(cleanedInput, DISTRESS_KEYWORDS)) {
		return handleDistress(context);
	}
	if (containsKeyword(cleanedInput, SAD_KEYWORDS)) {
		return handleSad(context);
	}
	if (containsKeyword(cleanedInput, BORED_KEYWORDS)) {
		return handleBored(context);
	}
	if (containsKeyword(cleanedInput, HUNGRY_KEYWORDS)) {
		return handleHungry(context);
	}
	if (containsKeyword(cleanedInput, TIRED_KEYWORDS)) {
		return handleTired(context);
	}
	if (containsKeyword(cleanedInput, HAPPY_KEYWORDS)) {
		return handleHappy(context);
	}
	if (containsKeyword(cleanedInput, CURIOUS_KEYWORDS)) {
		return handleCurious(context);
	}
	if (containsKeyword(cleanedInput, CONFUSED_KEYWORDS)) {
		return "I'm sorry to hear that. Is there something I said that I can try to explain differently?";
	}
	
	// A general fallback for other user statuses that might be added in the future.
	return "I understand. Is there anything I can help you with?";
}

/**
 * NEW: A more careful and empathetic handler for serious emotional distress.
 * This avoids giving unqualified advice or dismissive links.
 */
private String handleDistress(ConversationContext context) {
	List<String> responses = List.of(
			"I'm really sorry to hear you're going through that. It sounds incredibly difficult. Please remember to be kind to yourself and that it's okay to reach out to friends, family, or a professional for support.",
			"That sounds very painful, and I'm sorry you're hurting. While I can't fully understand what you're feeling, please know that your feelings are valid. Talking to someone you trust can sometimes help.",
			"I'm so sorry you're dealing with this. It's important to have support during tough times. Please consider talking to a friend, family member, or a mental health professional. You don't have to go through this alone."
	);
	return responses.get(random.nextInt(responses.size()));
}

private String handleBored(ConversationContext context) {
	List<String> suggestions = List.of(
			"Boredom is no fun! How about a fun fact to pass the time? Just say 'tell me a fun fact'.",
			"I can tell you a joke if you'd like! Just ask.",
			"Perhaps we could learn about a new topic? You can ask me to tell you about anything, like 'tell me about the planet Mars'.",
			"How about a riddle to challenge your mind? Just say 'tell me a riddle'."
	);
	return suggestions.get(random.nextInt(suggestions.size()));
}

private String handleHungry(ConversationContext context) {
	// Proactively reroute to a more helpful handler to find a recipe.
	String newQuery = "how to make a simple snack";
	ConversationContext newContext = new ConversationContext();
	newContext.setUsername(context.getUsername()); // Preserve the user's name
	
	String searchResponse = core.getResponse(newQuery, newContext);
	
	// Check for the specific failure message from the handler.
	if (searchResponse.startsWith("SEARCH_FAILED:")) {
		// Provide a more graceful, self-contained fallback message.
		return "I tried to look up a recipe for you, but my search service is having trouble at the moment. I'd recommend a web search for the most detailed guides.";
	} else {
		// The search was successful, so prepend the intro.
		return "I can't get you food, but I can help you find a recipe! Let's see...\n\n" + searchResponse;
	}
}

private String handleSad(ConversationContext context) {
	List<String> suggestions = List.of(
			"I'm sorry to hear you're feeling down. Sometimes a little distraction can help. Would you like to hear a joke?",
			"I'm sorry you're feeling that way. Remember that feelings are temporary. Maybe we can find a fun fact to take your mind off things?",
			"That sounds tough. While I'm best at providing information, I hope you feel better soon."
	);
	return suggestions.get(random.nextInt(suggestions.size()));
}

private String handleTired(ConversationContext context) {
	int choice = random.nextInt(3);
	if (choice == 0) {
		return "It sounds like you could use a break. Make sure to get some rest!";
	} else if (choice == 1) {
		String newQuery = "how to find relaxing instrumental music";
		ConversationContext newContext = new ConversationContext();
		newContext.setUsername(context.getUsername());
		return "Being tired is tough. Maybe some relaxing music would help? Let me look that up for you...\n\n" + core.getResponse(newQuery, newContext);
	} else {
		return "I understand. I'll be here if you need me after you've had a chance to rest.";
	}
}

private String handleHappy(ConversationContext context) {
	List<String> suggestions = List.of(
			"That's great to hear! I'm happy you're happy.",
			"Awesome! That puts a smile on my face... metaphorically, of course.",
			"Fantastic! Let's keep the good vibes going. What's on your mind?"
	);
	return suggestions.get(random.nextInt(suggestions.size()));
}

private String handleCurious(ConversationContext context) {
	return "Curiosity is a great thing! What topic has piqued your interest? You can ask me anything from 'what is the capital of France' to 'how do computers work?'.";
}
}