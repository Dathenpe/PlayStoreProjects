// C:/Users/Music_Minister/Desktop/PlayStore/PlayStoreProjects/Xavier/XavierAI/src/main/java/com/f9ld3/xavier/ai/V2/handlers/FunFactHandler.java
package com.f9ld3.xavier.ai.V2.handlers;

import com.f9ld3.xavier.ai.V2.ConversationContext;
import com.f9ld3.xavier.ai.V2.services.FunFactService;

import java.util.Optional;

/**
 * Handles requests for fun facts by calling the FunFactService.
 * REFACTORED: Now uses a properly configured, resilient service.
 */
public class FunFactHandler implements IntentHandler {

private final FunFactService funFactService;

public FunFactHandler(FunFactService funFactService) {
	this.funFactService = funFactService;
}

@Override
public String handle(String userInput, ConversationContext context) {
	// The "Thinking..." message should be handled by the main application loop if desired.
	// This handler's only job is to get the fact and format the response.
	// FIX: Call the new, dedicated getFact() method.
	Optional<String> fact = funFactService.getFact();
	
	return fact.map(f -> "Here's a fun fact: " + f)
			       .orElse("I'm all out of fun facts at the moment. Please try again later.");
}
}