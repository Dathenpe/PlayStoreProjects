package com.f9ld3.xavier.ai.V2.handlers;

import com.f9ld3.xavier.ai.V2.ConversationContext;
import com.f9ld3.xavier.ai.V2.WolframAlphaClient;

public class KnowledgeQueryHandler implements IntentHandler {

private final WolframAlphaClient client;

public KnowledgeQueryHandler(WolframAlphaClient client) {
	this.client = client;
}

@Override
public String handle(String userInput, ConversationContext context) {
	String processedInput = preprocessQuery(userInput);
	return client.getShortAnswer(processedInput);
}

/**
 * Pre-processes a query to make it more explicit for the API.
 * This helps avoid issues where the API implicitly uses its own context.
 *
 * @param query The raw user input.
 * @return A more explicit query string.
 */
private String preprocessQuery(String query) {
	String lowerQuery = query.toLowerCase().trim();
	String[] words = lowerQuery.split("\\s+");
	
	// If the query is very short (e.g., "apple", "a president") and doesn't
	// seem to be a full question, prepend "what is" to make it unambiguous.
	if (words.length <= 2 && !lowerQuery.startsWith("what") && !lowerQuery.startsWith("who") && !lowerQuery.startsWith("where")) {
		String newQuery = "what is " + query;
		System.out.println("[DEBUG] Pre-processed ambiguous query. New query: '" + newQuery + "'");
		return newQuery;
	}
	
	// For longer or more explicit questions, use them as-is.
	return query;
}
}