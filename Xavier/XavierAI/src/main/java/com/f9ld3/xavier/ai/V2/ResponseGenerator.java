package com.f9ld3.xavier.ai.V2;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * A utility class for generating dynamic and varied responses.
 * This moves us away from static, hardcoded replies and makes Xavier feel more alive.
 */
public final class ResponseGenerator {

private static final Random random = new Random();
private static final List<String> GREETING_TEMPLATES = loadTemplates("responses/greetings.txt");
private static final List<String> GRATITUDE_TEMPLATES = loadTemplates("responses/gratitude.txt");


// Private constructor to prevent instantiation of this utility class
private ResponseGenerator() {}

/**
 * Loads response templates from a resource file.
 * @param resourcePath The path to the file in the resources folder.
 * @return A list of templates.
 */
private static List<String> loadTemplates(String resourcePath) {
	List<String> templates = new ArrayList<>();
	try (InputStream is = ResponseGenerator.class.getClassLoader().getResourceAsStream(resourcePath);
	     BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
		String line;
		while ((line = reader.readLine()) != null) {
			if (!line.trim().isEmpty()) {
				templates.add(line.trim());
			}
		}
	} catch (Exception e) {
		System.err.println("ERROR: Could not load templates from " + resourcePath);
		// Add a single fallback template
		templates.add("I'm having a bit of trouble thinking of a response right now.");
	}
	return templates;
}

/**
 * Generates a personalized greeting.
 * @param context The current conversation context to check for a user's name.
 * @return A friendly, varied greeting.
 */
public static String getGreeting(ConversationContext context) {
	String userName = (String) context.getEntity("username");
	String baseGreeting = GREETING_TEMPLATES.get(random.nextInt(GREETING_TEMPLATES.size()));
	
	if (userName != null) {
		// Personalize the greeting if we know the user's name
		return baseGreeting.replace("there", userName)
				       .replace("you", userName)
				       .replace("Welcome back", "Welcome back, " + userName);
	}
	return baseGreeting;
}

/**
 * Generates an intelligent "I don't understand" response that offers help.
 * @return A helpful, guiding response.
 */
public static String getIntelligentFallback() {
	List<String> templates = List.of(
			"I'm not quite sure how to help with that. You could ask me for the time, the date, or to calculate something like '5 plus 7'.",
			"That's a bit beyond my current skills. I can tell you the weather, the time, or do simple math for you.",
			"I didn't quite understand that. I'm best at tasks like telling the time, the date, or calculating expressions.",
			"My apologies, I can't process that request. I can, however, provide the weather, the current time, or solve math problems."
	);
	return templates.get(random.nextInt(templates.size()));
}
}