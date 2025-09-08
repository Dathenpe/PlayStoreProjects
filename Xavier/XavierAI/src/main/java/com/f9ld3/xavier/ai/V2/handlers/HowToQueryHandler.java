package com.f9ld3.xavier.ai.V2.handlers;

import com.f9ld3.xavier.ai.V2.ConversationContext;
import com.f9ld3.xavier.ai.V2.services.SearchService;
import com.f9ld3.xavier.ai.V2.services.SearchService.SearchServiceException;
import com.f9ld3.xavier.ai.V2.services.SearchService.SearchResult;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A stateful, specialized handler for "how-to" queries. It performs a web search,
 * stores the results in the conversation context, and can serve subsequent results
 * upon user request (e.g., "try another").
 * REFACTORED: Now fully integrated with the new context stack and entity system.
 */
public class HowToQueryHandler implements IntentHandler {

private final SearchService searchService;

// Pattern to identify if the user is starting a NEW search.
private static final Pattern NEW_SEARCH_PATTERN = Pattern.compile(
		"(?i)(?:how to|how do i|tell me how to|explain how to|what are the steps to)\\s*(.+)",
		Pattern.CASE_INSENSITIVE
);

public HowToQueryHandler(SearchService searchService) {
	this.searchService = searchService;
}

@Override
public String handle(String userInput, ConversationContext context) {
	Matcher newSearchMatcher = NEW_SEARCH_PATTERN.matcher(userInput);
	
	// The core has already pushed a 'how_to_query' context.
	// We just need to decide if we're starting a new search within this context
	// or continuing an existing one.
	if (newSearchMatcher.find()) {
		// This is a NEW search query.
		String topic = newSearchMatcher.group(1).trim().replaceAll("\\?$", "");
		return performNewSearch(topic, context);
	} else {
		// This is a REFINEMENT query (e.g., "try another", "more info").
		return getNextResult(context);
	}
}

private String performNewSearch(String topic, ConversationContext context) {
	try {
		Optional<List<SearchResult>> resultsOpt = searchService.getSearchResults(topic);
		
		if (resultsOpt.isPresent() && !resultsOpt.get().isEmpty()) {
			List<SearchResult> results = resultsOpt.get();
			// UPDATED: Store results in the current context's entity map.
			context.addEntityToCurrentContext("searchResults", results);
			context.addEntityToCurrentContext("searchIndex", 0);
			return formatResult(results.get(0));
		} else {
			// No results found, so this conversational state is over.
			context.popContext();
			return String.format(
					"That's a great question about how to %s. I couldn't find any quick instructions for that right now. I would recommend a web search for the most detailed guides.",
					topic
			);
		}
	} catch (SearchServiceException e) {
		// An error occurred, so this conversational state is over.
		context.popContext();
		// Return a clean, user-friendly error message.
		return "I'm sorry, my search service seems to be unavailable at the moment. Please try again later.";
	}
}

@SuppressWarnings("unchecked") // We are confident about the type stored in the context.
private String getNextResult(ConversationContext context) {
	// UPDATED: Retrieve results and index from the current context's entity map.
	List<SearchResult> lastResults = (List<SearchResult>) context.getEntityFromCurrentContext("searchResults")
			                                                      .orElse(null);
	int lastIndex = (int) context.getEntityFromCurrentContext("searchIndex")
			                      .orElse(-1);
	
	if (lastResults == null || lastIndex == -1) {
		// The context is active, but has no search results. This can happen if a user says "next"
		// without a prior search. We pop the context because it's invalid.
		context.popContext();
		return "I'm not sure what you'd like another result for. Please ask a new 'how to' question first.";
	}
	
	int nextIndex = lastIndex + 1;
	
	if (nextIndex < lastResults.size()) {
		// We have another result to show.
		// UPDATED: Update the index in the current context.
		context.addEntityToCurrentContext("searchIndex", nextIndex);
		return "Here's another one:\n\n" + formatResult(lastResults.get(nextIndex));
	} else {
		// We've run out of results. This conversational state is over.
		context.popContext();
		return "I don't have any more results for that topic, sorry!";
	}
}

/**
 * Formats a SearchResult object into a comprehensive, user-friendly string.
 */
private String formatResult(SearchResult result) {
	// Ensure snippet is not null before trying to use it.
	String snippetText = result.snippet() != null ? result.snippet() : "No snippet available.";
	return String.format(
			"Title: %s\nSnippet: \"%s...\"\nSource: %s",
			result.title(),
			snippetText,
			result.link()
	);
}
}