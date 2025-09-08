package com.f9ld3.xavier.ai.V2.handlers;

import com.f9ld3.xavier.ai.V2.ConversationContext;
import java.util.Map;

/**
 * Handles questions about the bot's identity, capabilities, and comparisons.
 */
public class AboutBotHandler implements IntentHandler {

private static final String STANDARD_ABOUT_RESPONSE = "My designation is Xavier. I'm a Java-based AI. " +
		                                                      "My primary function is to assist with tasks by understanding natural language. To do this, I process input " +
		                                                      "through a pipeline: first checking for simple commands, then for complex but structured patterns, and finally " +
		                                                      "using a statistical model to classify everything else. This allows me to be both precise and flexible. What can I do for you?";

// Pre-defined answers for common meta-questions
private static final Map<String, String> META_RESPONSES = Map.of(
		"compare_gpt", "As a locally-run, rule-based AI, I specialize in providing fast, factual, and private responses based on my programming and direct API connections. " +
				               "Large language models like ChatGPT are trained on vast datasets and excel at generative, creative, and nuanced conversational tasks. We're built for different purposes!",
		"research", "I can access and present factual information from my connected knowledge bases like Wolfram|Alpha and perform targeted web searches to find relevant documents. " +
				            "However, I don't conduct original research or synthesize new conclusions from multiple sources.",
		"creator", "I was created by my developer using Java and a custom-built AI pipeline. My design focuses on being a responsive and helpful assistant."
);

@Override
public String handle(String userInput, ConversationContext context) {
	String cleanedInput = userInput.toLowerCase();
	
	// Check for meta-questions first
	if (cleanedInput.contains("compare") && (cleanedInput.contains("gpt") || cleanedInput.contains("chatgpt"))) {
		return META_RESPONSES.get("compare_gpt");
	}
	if (cleanedInput.contains("research")) {
		return META_RESPONSES.get("research");
	}
	if (cleanedInput.contains("who made you") || cleanedInput.contains("who created you")) {
		return META_RESPONSES.get("creator");
	}
	
	// Fallback to the standard "about me" response
	return STANDARD_ABOUT_RESPONSE;
}
}