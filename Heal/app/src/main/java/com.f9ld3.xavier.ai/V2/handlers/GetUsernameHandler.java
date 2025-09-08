package com.f9ld3.xavier.ai.V2.handlers;

import com.f9ld3.xavier.ai.V2.ConversationContext;

/**
 * Retrieves and states the user's name from the conversation context.
 */
public class GetUsernameHandler implements IntentHandler {
@Override
public String handle(String userInput, ConversationContext context) {
	String username = context.getUsername();
	if (username != null) {
		return "Your name is " + username + ".";
	} else {
		return "I don't believe you've told me your name yet. You can tell me by saying 'my name is...'";
	}
}
}