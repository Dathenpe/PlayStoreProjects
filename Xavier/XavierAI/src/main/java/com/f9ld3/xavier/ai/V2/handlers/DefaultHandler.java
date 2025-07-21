package com.f9ld3.xavier.ai.V2.handlers;

import com.f9ld3.xavier.ai.V2.ConversationContext;
import com.f9ld3.xavier.ai.V2.ResponseGenerator; // Import the new class

public class DefaultHandler implements IntentHandler {
@Override
public String handle(String userInput, ConversationContext context) {
	// Use the new intelligent fallback to guide the user
	return ResponseGenerator.getIntelligentFallback();
}
}