package com.f9ld3.xavier.ai.V2.handlers;

import com.f9ld3.xavier.ai.V2.ConversationContext;

/**
 * Handles simple confirmation phrases like "yes" or "no".
 * This provides a basic acknowledgment that can be expanded for more complex dialogs.
 */
public class ConfirmationHandler implements IntentHandler {
@Override
public String handle(String userInput, ConversationContext context) {
	// For now, just acknowledge the confirmation.
	return "Understood.";
}
}