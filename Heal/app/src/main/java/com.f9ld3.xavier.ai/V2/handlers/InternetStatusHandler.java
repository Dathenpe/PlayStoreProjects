package com.f9ld3.xavier.ai.V2.handlers;

import android.content.Context; // <<<<<< IMPORT
import com.f9ld3.xavier.ai.V2.ConversationContext;
import com.f9ld3.xavier.ai.V2.utils.NetworkStatusChecker;

public class InternetStatusHandler implements IntentHandler {
	private final Context handlerContext; // <<<<<< STORE CONTEXT

	public InternetStatusHandler(Context context) { // <<<<<< CONSTRUCTOR
		this.handlerContext = context;
	}

	@Override
	public String handle(String userInput, ConversationContext context) {
		// Now use handlerContext with NetworkStatusChecker.isOnline(Context)
		if (NetworkStatusChecker.isOnline(this.handlerContext)) {
			return "My connection to the internet appears to be stable and active.";
		} else {
			return "I am currently unable to connect to the internet. My capabilities will be limited.";
		}
	}
}
