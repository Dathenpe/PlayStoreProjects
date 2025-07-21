package com.f9ld3.xavier.ai.V2.handlers;

import com.f9ld3.xavier.ai.V2.ConversationContext;

/**
 * Handles questions about the AI's identity and capabilities.
 * This gives Xavier a sense of self-awareness.
 */
public class AboutBotHandler implements IntentHandler {

@Override
public String handle(String userInput, ConversationContext context) {
	return "I am Xavier, a conversational AI assistant. I can tell you the time, date, and weather, or perform calculations for you. How can I help?";
}
}