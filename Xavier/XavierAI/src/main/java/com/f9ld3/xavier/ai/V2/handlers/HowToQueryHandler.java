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
			// Store results in context for future "try another" requests.
			context.setLastSearchResults(results);
			context.setLastSearchResultIndex(0);
			return formatResult(results.get(0));
		} else {
			// This case is now a fallback for when the API returns an empty list but no error.
			context.clearSearchContext();
			return String.format(
					"That's a great question about how to %s. I couldn't find any quick instructions for that right now. I would recommend a web search for the most detailed guides.",
					topic
			);
		}
	} catch (SearchServiceException e) {
		// If the service throws an exception (e.g., all keys fail), catch it here.
		context.clearSearchContext();
		// Return a clean, specific error message that other handlers can check for.
		return "SEARCH_FAILED: " + e.getMessage();
	}
}

private String getNextResult(ConversationContext context) {
	List<SearchResult> lastResults = context.getLastSearchResults();
	int lastIndex = context.getLastSearchResultIndex();
	
	if (lastResults == null || lastResults.isEmpty()) {
		return "I'm not sure what you'd like another result for. Please ask a new 'how to' question first.";
	}
	
	int nextIndex = lastIndex + 1;
	
	if (nextIndex < lastResults.size()) {
		// We have another result to show.
		context.setLastSearchResultIndex(nextIndex);
		return "Here's another one:\n\n" + formatResult(lastResults.get(nextIndex));
	} else {
		// We've run out of results.
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