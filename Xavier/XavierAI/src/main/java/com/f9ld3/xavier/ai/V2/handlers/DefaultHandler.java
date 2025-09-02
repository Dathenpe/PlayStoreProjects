package com.f9ld3.xavier.ai.V2.handlers;

import com.f9ld3.xavier.ai.V2.ConversationContext;
import com.f9ld3.xavier.ai.V2.utils.ResponseGenerator; // Correctly points to the utility class

public class DefaultHandler implements IntentHandler {
@Override
public String handle(String userInput, ConversationContext context) {
	// This line creates the dependency.
	// If ResponseGenerator is deleted, this code will fail to compile.
	return ResponseGenerator.getIntelligentFallback();
}
}