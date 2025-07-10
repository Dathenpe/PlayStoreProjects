package com.f9ld3.xavier.ai.V2.handlers;

import com.f9ld3.xavier.ai.V2.ConversationContext;

public class GreetingHandler implements IntentHandler {
@Override
public String handle(String userInput, ConversationContext context) {
	return "Hello there! How can I help you today?";
}
}