package com.f9ld3.xavier.ai.V2.handlers;

import com.f9ld3.xavier.ai.V2.ConversationContext;
import com.f9ld3.xavier.ai.V2.services.DictionaryService;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handles requests for word definitions using the DictionaryService.
 * REFACTORED: Now fully integrated with the new context stack and entity system.
 */
public class DictionaryHandler implements IntentHandler {

private final DictionaryService dictionaryService;

// This pattern is now a fallback, as the core pipeline extracts the entity first.
private static final Pattern WORD_PATTERN = Pattern.compile(
		"^(?:define|what's|what is|what does)(?: the meaning of)?(?: the word)?\\s+(.+)",
		Pattern.CASE_INSENSITIVE
);

public DictionaryHandler(DictionaryService dictionaryService) {
	this.dictionaryService = dictionaryService;
}

@Override
public String handle(String userInput, ConversationContext context) {
	// UPDATED: Prioritize getting the term from the context, as extracted by the core pipeline.
	String word = context.getEntityFromCurrentContext("term")
			              .map(String::valueOf)
			              .orElse(null); // Fallback to null if not found
	
	// If the core didn't find an entity, try to extract it manually as a fallback.
	if (word == null || word.isBlank()) {
		word = extractWord(userInput);
	}
	
	if (word == null || word.isBlank()) {
		return "I didn't quite catch that. To define a word, you can say 'define serendipity'.";
	}
	
	try {
		Optional<String> definition = dictionaryService.getDefinition(word);
		
		if (definition.isPresent()) {
			// The core has already set the context and the entity ('term').
			// We just need to return the successful response.
			return String.format("The definition of '%s' is: %s", word, definition.get());
		} else {
			// UPDATED: The 'setLastFailedInput' method is no longer needed.
			// We just return the failure message.
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
		// The group(1) will capture the word itself.
		return matcher.group(1).replace("?", "").trim();
	}
	// If the pattern doesn't match, the intent was likely classified from a single word.
	return input.trim();
}
}