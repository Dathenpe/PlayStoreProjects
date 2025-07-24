package com.f9ld3.xavier.ai.V2.handlers;

import com.f9ld3.xavier.ai.V2.ConversationContext;
import com.f9ld3.xavier.ai.V2.services.DictionaryService;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handles requests for word definitions using the DictionaryService.
 */
public class DictionaryHandler implements IntentHandler {

private final DictionaryService dictionaryService;

// A pattern to extract the word from various definition-request phrases.
private static final Pattern WORD_PATTERN = Pattern.compile(
		"^(?:define|what's|what is|what does)(?: the meaning of)?(?: the word)?\\s+(.+)",
		Pattern.CASE_INSENSITIVE
);

public DictionaryHandler(DictionaryService dictionaryService) {
	this.dictionaryService = dictionaryService;
}

@Override
public String handle(String userInput, ConversationContext context) {
	String word = extractWord(userInput);
	if (word == null || word.isEmpty()) {
		return "I didn't quite catch that. To define a word, you can say 'define serendipity'.";
	}
	
	try {
		Optional<String> definition = dictionaryService.getDefinition(word);
		
		if (definition.isPresent()) {
			context.setLastSubject(word); // Set context for potential follow-ups
			return String.format("The definition of '%s' is: %s", word, definition.get());
		} else {
			context.setLastFailedInput(userInput); // Set context for correction
			return String.format("I'm sorry, I couldn't find a definition for '%s'. Please check the spelling.", word);
		}
	} catch (Exception e) {
		System.err.println("Dictionary Handler Error: " + e.getMessage());
		return "I seem to be having trouble with my dictionary service at the moment. Please try again later.";
	}
}

private String extractWord(String input) {
	Matcher matcher = WORD_PATTERN.matcher(input.trim());
	if (matcher.find()) {
		return matcher.group(1).replace("?", "").trim();
	}
	// If the pattern doesn't match, the intent was likely classified from a single word.
	return input.trim();
}
}