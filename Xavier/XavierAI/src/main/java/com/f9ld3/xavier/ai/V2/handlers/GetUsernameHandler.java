package com.f9ld3.xavier.ai.V2.handlers;

import com.f9ld3.xavier.ai.V2.ConversationContext;

public class GetUsernameHandler implements IntentHandler {
@Override
public String handle(String userInput, ConversationContext context) {
	// Retrieve the username from the conversation's memory
	String userName = (String) context.getEntity("username");
	
	if (userName != null) {
		return "Your name is " + userName + ".";
	} else {
		return "I don't believe you've told me your name yet. You can tell me by saying 'my name is...'";
	}
}
}