package com.f9ld3.xavier.ai.V2.handlers;

import com.f9ld3.xavier.ai.V2.ConversationContext;
import com.f9ld3.xavier.ai.V2.utils.ResponseGenerator; // Make sure it points to the 'utils' package

public class GoodbyeHandler implements IntentHandler {
@Override
public String handle(String userInput, ConversationContext context) {
	// Delegate the response generation to our single, unified utility
	return ResponseGenerator.getGoodbye(context);
}
}