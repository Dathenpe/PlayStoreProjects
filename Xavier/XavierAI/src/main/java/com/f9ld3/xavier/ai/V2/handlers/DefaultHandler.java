package com.f9ld3.xavier.ai.V2.handlers;

import com.f9ld3.xavier.ai.V2.ConversationContext;

public class DefaultHandler implements IntentHandler {
@Override
public String handle(String userInput, ConversationContext context) {
	return "I'm not quite sure what you mean. Could you rephrase?";
}
}