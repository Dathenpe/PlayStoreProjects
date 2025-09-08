package com.f9ld3.xavier.ai.V2.handlers;

import com.f9ld3.xavier.ai.V2.ConversationContext;
import com.f9ld3.xavier.ai.V2.utils.NetworkStatusChecker;

/**
 * Handles user queries about the current internet connection status.
 */
public class InternetStatusHandler implements IntentHandler {
@Override
public String handle(String userInput, ConversationContext context) {
	if (NetworkStatusChecker.isOnline()) {
		return "My connection to the internet appears to be stable and active.";
	} else {
		return "I am currently unable to connect to the internet. My capabilities will be limited.";
	}
}
}