package com.f9ld3.xavier.ai.V2.handlers;

import com.f9ld3.xavier.ai.V2.ConversationContext;

/**
 * Defines the contract for a class that can handle a specific user intent.
 * This is the "Strategy" in the Strategy Pattern.
 */
public interface IntentHandler {
/**
 * Executes the logic for the intent and returns a response.
 * @param userInput The original, unprocessed user input.
 * @param context The current state of the conversation.
 * @return A string response from the AI.
 */
String handle(String userInput, ConversationContext context);
}