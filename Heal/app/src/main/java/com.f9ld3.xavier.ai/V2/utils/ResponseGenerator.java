// C:/Users/Music_Minister/Desktop/PlayStore/PlayStoreProjects/Xavier/XavierAI/src/main/java/com/f9ld3/xavier/ai/V2/utils/ResponseGenerator.java
package com.f9ld3.xavier.ai.V2.utils;

import android.content.Context;
import com.f9ld3.xavier.ai.V2.ConversationContext;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class ResponseGenerator {

	private static final Random random = new Random();

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

	// MODIFIED: These are no longer final and are initialized in init()
	private static List<String> GREETING_TEMPLATES;
	private static List<String> STATUS_CHECK_TEMPLATES;
	private static List<String> GOODBYE_TEMPLATES;
	private static List<String> FALLBACK_TEMPLATES;

	// Private constructor to prevent instantiation
	private ResponseGenerator() {}

	// NEW: Initialization method to be called once from the app
	public static void init(Context context) {
		GREETING_TEMPLATES = loadHybridTemplates(context, "responses/greetings.txt", DEFAULT_GREETINGS);
		STATUS_CHECK_TEMPLATES = loadHybridTemplates(context, "responses/status_checks.txt", DEFAULT_STATUS_CHECKS);
		GOODBYE_TEMPLATES = loadHybridTemplates(context, "responses/goodbyes.txt", DEFAULT_GOODBYES);
		FALLBACK_TEMPLATES = loadHybridTemplates(context, "responses/fallbacks.txt", DEFAULT_FALLBACKS);
	}

	public static String getGreeting(ConversationContext context) {
		String userInput = context.getLastUserInput().toLowerCase();

		if (userInput.contains("how are you") || userInput.contains("how's it going") || userInput.contains("how do you do")) {
			return getRandomString(STATUS_CHECK_TEMPLATES);
		}

		String baseGreeting = getRandomString(GREETING_TEMPLATES);
		String username = context.getUsername();

		if (username != null && !username.isBlank()) {
			return baseGreeting.replace("there", username);
		}
		return baseGreeting;
	}

	public static String getGoodbye(ConversationContext context) {
		String baseGoodbye = getRandomString(GOODBYE_TEMPLATES);
		String username = context.getUsername();

		if (username != null && !username.isBlank()) {
			return baseGoodbye.replace("!", ", " + username + "!");
		}
		return baseGoodbye;
	}

	public static String getIntelligentFallback() {
		return getRandomString(FALLBACK_TEMPLATES);
	}

	private static String getRandomString(List<String> options) {
		if (options == null || options.isEmpty()) {
			return "I'm at a loss for words right now.";
		}
		return options.get(random.nextInt(options.size()));
	}

	// MODIFIED: Method now takes Context
	private static List<String> loadHybridTemplates(Context context, String assetPath, List<String> defaultList) {
		List<String> templates = new ArrayList<>();
		try (InputStream is = context.getAssets().open(assetPath)) {
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
				String line;
				while ((line = reader.readLine()) != null) {
					if (!line.trim().isEmpty() && !line.startsWith("#")) {
						templates.add(line.trim());
					}
				}
			}
		} catch (Exception e) {
			System.err.println("WARN: Could not load templates from " + assetPath + ". Using hardcoded defaults. Reason: " + e.getMessage());
			return defaultList;
		}

		if (templates.isEmpty()) {
			System.err.println("WARN: Template file " + assetPath + " was empty. Using hardcoded defaults.");
			return defaultList;
		}
		return templates;
	}
}