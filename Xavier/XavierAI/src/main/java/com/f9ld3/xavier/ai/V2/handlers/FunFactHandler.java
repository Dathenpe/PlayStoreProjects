package com.f9ld3.xavier.ai.V2.handlers;

import com.f9ld3.xavier.ai.V2.ConversationContext;
import com.f9ld3.xavier.ai.V2.services.FunFactService;

import java.util.Optional;

/**
 * Handles requests for fun facts using the FunFactService.
 */
public class FunFactHandler implements IntentHandler {

private final FunFactService funFactService;

public FunFactHandler(FunFactService funFactService) {
	this.funFactService = funFactService;
}

@Override
public String handle(String userInput, ConversationContext context) {
	Optional<String> fact = funFactService.getFact();
	
	// Use .map() for a clean, functional way to handle the Optional
	return fact.map(s -> "Here's a fun fact: " + s)
			       .orElse("I'm all out of fun facts at the moment. Please try again later.");
}
}