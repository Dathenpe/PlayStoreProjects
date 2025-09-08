package com.f9ld3.xavier.ai.V2.handlers;

import android.util.Log; // Using Android's Log

import com.f9ld3.xavier.ai.V2.ConversationContext;
import com.f9ld3.xavier.ai.V2.services.SearchService;
import com.f9ld3.xavier.ai.V2.services.SearchService.SearchServiceException;
import com.f9ld3.xavier.ai.V2.services.SearchService.SearchResult; // Assuming SearchResult is a class now

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

	private static final String TAG = "HowToQueryHandler";
	private final SearchService searchService;

	// Pattern to identify if the user is starting a NEW search.
	private static final Pattern NEW_SEARCH_PATTERN = Pattern.compile(
			"(?i)(?:how to|how do i|tell me how to|explain how to|what are the steps to)\\s*(.+)",
			Pattern.CASE_INSENSITIVE
	);

	public HowToQueryHandler(SearchService searchService) {
		if (searchService == null) {
			// Or handle this more gracefully, but a SearchService is essential here.
			throw new IllegalArgumentException("SearchService cannot be null.");
		}
		this.searchService = searchService;
	}

	@Override
	public String handle(String userInput, ConversationContext context) {
		if (userInput == null || context == null) {
			Log.w(TAG, "User input or context is null.");
			return "I'm sorry, something went wrong."; // Or a more appropriate default
		}

		Matcher newSearchMatcher = NEW_SEARCH_PATTERN.matcher(userInput);

		// The core has already pushed a 'how_to_query' context.
		// We just need to decide if we're starting a new search within this context
		// or continuing an existing one.
		if (newSearchMatcher.find()) {
			// This is a NEW search query.
			String topic = newSearchMatcher.group(1).trim().replaceAll("\\?$", "");
			Log.d(TAG, "Performing new 'how to' search for topic: " + topic);
			return performNewSearch(topic, context);
		} else {
			// This is a REFINEMENT query (e.g., "try another", "more info").
			Log.d(TAG, "Getting next 'how to' result for existing query.");
			return getNextResult(context);
		}
	}

	private String performNewSearch(String topic, ConversationContext context) {
		try {
			Optional<List<SearchResult>> resultsOpt = searchService.getSearchResults(topic);

			if (resultsOpt.isPresent() && !resultsOpt.get().isEmpty()) {
				List<SearchResult> results = resultsOpt.get();
				Log.d(TAG, "Found " + results.size() + " search results for '" + topic + "'.");
				context.addEntityToCurrentContext("searchResults", results);
				context.addEntityToCurrentContext("searchIndex", 0);
				if (results.get(0) == null) {
					Log.w(TAG, "First search result was null for topic: " + topic);
					context.popContext(); // Invalid state
					return "I found some results, but there was an issue displaying the first one.";
				}
				return formatResult(results.get(0));
			} else {
				Log.d(TAG, "No search results found for topic: " + topic);
				// No results found, so this conversational state is over.
				context.popContext();
				return String.format(
						"That's a great question about how to %s. I couldn't find any quick instructions for that right now. I would recommend a web search for the most detailed guides.",
						topic
				);
			}
		} catch (SearchServiceException e) {
			Log.e(TAG, "SearchServiceException while searching for topic '" + topic + "': " + e.getMessage(), e);
			// An error occurred, so this conversational state is over.
			context.popContext();
			// Return a clean, user-friendly error message.
			return "I'm sorry, my search service seems to be unavailable at the moment. Please try again later.";
		}
	}

	@SuppressWarnings("unchecked") // We are confident about the type stored in the context.
	private String getNextResult(ConversationContext context) {
		// UPDATED: Retrieve results and index from the current context's entity map.
		Optional<Object> searchResultsEntity = context.getEntityFromCurrentContext("searchResults");
		Optional<Object> searchIndexEntity = context.getEntityFromCurrentContext("searchIndex");

		List<SearchResult> lastResults = null;
		if (searchResultsEntity.isPresent() && searchResultsEntity.get() instanceof List) {
			// Additional check to see if elements are SearchResult (though type erasure limits this)
			try {
				lastResults = (List<SearchResult>) searchResultsEntity.get();
			} catch (ClassCastException cce) {
				Log.e(TAG, "ClassCastException for searchResults in context. Expected List<SearchResult>.", cce);
			}
		}

		int lastIndex = -1;
		if (searchIndexEntity.isPresent() && searchIndexEntity.get() instanceof Integer) {
			lastIndex = (Integer) searchIndexEntity.get();
		}


		if (lastResults == null || lastIndex == -1) {
			Log.w(TAG, "Attempted to get next result, but context is missing searchResults or searchIndex, or types are incorrect.");
			// The context is active, but has no search results or valid index.
			// This can happen if a user says "next" without a prior search.
			// We pop the context because it's invalid.
			context.popContext();
			return "I'm not sure what you'd like another result for. Please ask a new 'how to' question first.";
		}

		int nextIndex = lastIndex + 1;

		if (nextIndex < lastResults.size()) {
			// We have another result to show.
			Log.d(TAG, "Showing next search result at index: " + nextIndex);
			context.addEntityToCurrentContext("searchIndex", nextIndex);
			SearchResult nextResult = lastResults.get(nextIndex);
			if (nextResult == null) {
				Log.w(TAG, "Search result at index " + nextIndex + " was null.");
				context.popContext(); // Invalid state
				return "I found another result, but there was an issue displaying it.";
			}
			return "Here's another one:\n\n" + formatResult(nextResult);
		} else {
			Log.d(TAG, "No more search results to show.");
			// We've run out of results. This conversational state is over.
			context.popContext();
			return "I don't have any more results for that topic, sorry!";
		}
	}

	/**
	 * Formats a SearchResult object into a comprehensive, user-friendly string.
	 */
	private String formatResult(SearchResult result) {
		// ADDED: Null check for the result object itself.
		if (result == null) {
			Log.w(TAG, "formatResult called with a null SearchResult object.");
			return "I found a result, but there was an issue displaying its details.";
		}

		// Ensure snippet is not null before trying to use it.
		String titleText = result.getTitle() != null ? result.getTitle() : "No title available.";
		String snippetText = result.getSnippet() != null ? result.getSnippet() : "No snippet available.";
		String linkText = result.getLink() != null ? result.getLink() : "No link available.";

		return String.format(
				"Title: %s\nSnippet: \"%s...\"\nSource: %s",
				titleText,
				snippetText,
				linkText
		);
	}
}
