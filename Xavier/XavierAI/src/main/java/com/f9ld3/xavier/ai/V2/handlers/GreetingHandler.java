package com.f9ld3.xavier.ai.V2.handlers;

import com.f9ld3.xavier.ai.V2.ConversationContext;
import com.f9ld3.xavier.ai.V2.ResponseGenerator; // Import the new class

public class GreetingHandler implements IntentHandler {
@Override
public String handle(String userInput, ConversationContext context) {
	// Delegate the response generation to our new utility
	return ResponseGenerator.getGreeting(context);
}
}