// C:/Users/Music_Minister/Desktop/PlayStore/PlayStoreProjects/Xavier/XavierAI/src/main/java/com/f9ld3/xavier/ai/V2/utils/ResponseGenerator.java
package com.f9ld3.xavier.ai.V2.utils;

import com.f9ld3.xavier.ai.V2.ConversationContext;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * A centralized utility for generating dynamic and context-aware conversational responses.
 * It uses a hybrid loading strategy: attempting to load from resource files first,
 * and falling back to hardcoded defaults for maximum robustness.
 */
public final class ResponseGenerator {

private static final Random random = new Random();

// --- Hardcoded Default Responses ---
// These act as a reliable fallback if the external .txt files are missing or empty.
private static final List<String> DEFAULT_GREETINGS = List.of(
		"Hello there! How can I assist you today?",
		"Hi! What can I do for you?",
		"Greetings! How may I help?",
		"Hey! Ready to assist."
);

private static final List<String> DEFAULT_STATUS_CHECKS = List.of(
		"I'm doing great, thanks for asking! I'm ready to help.",
		"I'm operating at 100% efficiency! What can I do for you?",
		"I'm doing well, thank you! How can I assist you today?"
);

private static final List<String> DEFAULT_GOODBYES = List.of(
		"Goodbye! It was nice chatting with you.",
		"Farewell! Hope to talk again soon.",
		"See you later! Have a great day.",
		"Bye for now! Let me know if you need anything else."
);

private static final List<String> DEFAULT_FALLBACKS = List.of(
		"I'm not sure how to help with that. You can ask me for a list of things, a fun fact, a joke, or a definition. I can also answer general knowledge questions.",
		"I didn't quite catch that. Try asking me something like 'tell me about the planet mars', 'list 10 popular dog breeds', or 'how to bake a cake'.",
		"My apologies, I don't have that capability yet. I'm best at providing information, generating lists, telling jokes, and defining words. How can I help?",
		"That's a bit beyond my current skills. I can answer questions, generate lists, tell you the weather, or do simple math for you."
);

// --- Hybrid-Loaded Response Templates ---
// These will use the .txt files if available, otherwise they use the defaults above.
private static final List<String> GREETING_TEMPLATES = loadHybridTemplates("responses/greetings.txt", DEFAULT_GREETINGS);
private static final List<String> STATUS_CHECK_TEMPLATES = loadHybridTemplates("responses/status_checks.txt", DEFAULT_STATUS_CHECKS);
private static final List<String> GOODBYE_TEMPLATES = loadHybridTemplates("responses/goodbyes.txt", DEFAULT_GOODBYES);
private static final List<String> FALLBACK_TEMPLATES = loadHybridTemplates("responses/fallbacks.txt", DEFAULT_FALLBACKS);


// Private constructor to prevent instantiation of this utility class
private ResponseGenerator() {}

/**
 * Generates an intelligent greeting. It distinguishes between a simple "hi"
 * and a question like "how are you" to provide a more appropriate response.
 *
 * @param context The current conversation context.
 * @return A context-aware greeting string.
 */
public static String getGreeting(ConversationContext context) {
	String userInput = context.getLastUserInput().toLowerCase();
	
	// Check if the user is asking how the AI is doing.
	if (userInput.contains("how are you") || userInput.contains("how's it going") || userInput.contains("how do you do")) {
		return getRandomString(STATUS_CHECK_TEMPLATES);
	}
	
	// Otherwise, provide a standard greeting.
	String baseGreeting = getRandomString(GREETING_TEMPLATES);
	String username = context.getUsername();
	
	if (username != null && !username.isBlank()) {
		// A simple and effective way to personalize.
		return baseGreeting.replace("there", username);
	}
	return baseGreeting;
}

/**
 * Generates a personalized farewell.
 * @param context The current conversation context to check for a user's name.
 * @return A friendly, varied farewell.
 */
public static String getGoodbye(ConversationContext context) {
	String baseGoodbye = getRandomString(GOODBYE_TEMPLATES);
	String username = context.getUsername();
	
	if (username != null && !username.isBlank()) {
		return baseGoodbye.replace("!", ", " + username + "!");
	}
	return baseGoodbye;
}

/**
 * Generates an intelligent "I don't understand" response that offers help.
 * @return A helpful, guiding response.
 */
public static String getIntelligentFallback() {
	return getRandomString(FALLBACK_TEMPLATES);
}

/**
 * A helper method to pick a random string from a list.
 */
private static String getRandomString(List<String> options) {
	if (options == null || options.isEmpty()) {
		// This is a final, hardcoded failsafe.
		return "I'm at a loss for words right now.";
	}
	return options.get(random.nextInt(options.size()));
}

/**
 * A robust method to load response templates. It tries to load from a resource file
 * and, if that fails or the file is empty, it returns a provided default list.
 *
 * @param resourcePath The path to the resource file (e.g., "responses/greetings.txt").
 * @param defaultList  A hardcoded list of strings to use as a fallback.
 * @return The loaded list from the file, or the default list on failure.
 */
private static List<String> loadHybridTemplates(String resourcePath, List<String> defaultList) {
	List<String> templates = new ArrayList<>();
	try (InputStream is = ResponseGenerator.class.getClassLoader().getResourceAsStream(resourcePath)) {
		// If the resource file doesn't exist, 'is' will be null.
		if (is == null) {
			throw new NullPointerException("Resource file not found: " + resourcePath);
		}
		
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
			String line;
			while ((line = reader.readLine()) != null) {
				if (!line.trim().isEmpty() && !line.startsWith("#")) { // Ignore empty lines and comments
					templates.add(line.trim());
				}
			}
		}
	} catch (Exception e) {
		System.err.println("WARN: Could not load templates from " + resourcePath + ". Using hardcoded defaults. Reason: " + e.getMessage());
		return defaultList; // On any exception, return the safe, hardcoded list.
	}
	
	// If the file was found but was empty, also use the defaults.
	if (templates.isEmpty()) {
		System.err.println("WARN: Template file " + resourcePath + " was empty. Using hardcoded defaults.");
		return defaultList;
	}
	
	return templates;
}
}