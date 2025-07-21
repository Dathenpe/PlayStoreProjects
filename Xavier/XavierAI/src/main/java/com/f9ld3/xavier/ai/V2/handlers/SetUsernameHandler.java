package com.f9ld3.xavier.ai.V2.handlers;

import com.f9ld3.xavier.ai.V2.ConversationContext;
import java.util.Arrays;
import java.util.List;

/**
 * A more intelligent handler for setting the user's name.
 * It identifies the name by looking for common keywords that precede it,
 * making it highly resilient to typos in the rest of the sentence.
 */
public class SetUsernameHandler implements IntentHandler {

// A list of common words that signal a name is about to be mentioned.
private static final List<String> INTRO_KEYWORDS = Arrays.asList(
		"is", "am", "i'm", "me"
);

@Override
public String handle(String userInput, ConversationContext context) {
	String[] words = userInput.toLowerCase().split("\\s+");
	String extractedName = null;
	
	// Find the last occurrence of one of our keywords in the sentence.
	int keywordIndex = -1;
	for (int i = 0; i < words.length; i++) {
		if (INTRO_KEYWORDS.contains(words[i])) {
			keywordIndex = i;
		}
	}
	
	// If we found a keyword and there's at least one word after it,
	// we assume that next word is the name.
	if (keywordIndex != -1 && keywordIndex < words.length - 1) {
		extractedName = words[keywordIndex + 1];
		
		// --- NEW: Sanitize the extracted name ---
		// Remove any characters that are not letters.
		extractedName = extractedName.replaceAll("[^a-zA-Z]", "");
	}
	
	if (extractedName != null && !extractedName.isEmpty()) {
		// Capitalize the name for a nice, clean look.
		String formattedName = extractedName.substring(0, 1).toUpperCase() + extractedName.substring(1);
		
		// Store the name in the conversation's memory
		context.setEntity("username", formattedName);
		
		return "Got it! I'll call you " + formattedName + " from now on.";
	} else {
		// This can happen if the intent was misclassified or the sentence structure was unexpected.
		return "I'm sorry, I didn't quite catch your name. Could you try again using a phrase like 'My name is...'?";
	}
}
}