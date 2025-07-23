package com.f9ld3.xavier.ai.V2.handlers;

import com.f9ld3.xavier.ai.V2.ConversationContext;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Provides a more detailed and dynamic summary of the AI's capabilities and architecture.
 */
public class AboutBotHandler implements IntentHandler {

private static final List<String> ABOUT_SUMMARIES = Arrays.asList(
		"I am Xavier, a conversational AI assistant. My core logic is written in Java, and I operate using a multi-layered reasoning pipeline. This includes direct-matching for simple commands, pattern-recognition for structured queries, and a statistical classifier for more nuanced, conversational requests. I can also connect to external APIs like Wolfram|Alpha for general knowledge.",
		"You're speaking with Xavier. I'm an AI assistant built on the Java platform. My architecture is designed for robustness, featuring several layers of intent recognition: precise pattern matching for things like setting your name, and a Naive Bayes classifier for understanding the context of more general conversation. I can also perform calculations, get the time, and fetch weather data.",
		"My designation is Xavier. I'm a Java-based AI. My primary function is to assist with tasks by understanding natural language. To do this, I process input through a pipeline: first checking for simple commands, then for complex but structured patterns, and finally using a statistical model to classify everything else. This allows me to be both precise and flexible. What can I do for you?"
);

private final Random random = new Random();

@Override
public String handle(String userInput, ConversationContext context) {
	// Return a random detailed summary to keep the interaction fresh.
	return ABOUT_SUMMARIES.get(random.nextInt(ABOUT_SUMMARIES.size()));
}
}