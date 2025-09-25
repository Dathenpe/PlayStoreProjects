package com.f9ld3.xavier.ai.V2.handlers;

import android.util.Log;

import com.f9ld3.xavier.ai.V2.ConversationContext;
import com.f9ld3.xavier.ai.V2.services.SearchService;
import com.f9ld3.xavier.ai.V2.services.SearchService.SearchServiceException;
import com.f9ld3.xavier.ai.V2.services.SearchService.Snippet; // <<<<<< FIX: Changed from SearchResult to Snippet

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HowToQueryHandler implements IntentHandler {

	private static final String TAG = "HowToQueryHandler";
	private final SearchService searchService;
	private static final Pattern NEW_SEARCH_PATTERN = Pattern.compile(
			"(?i)(?:how to|how do i|tell me how to|explain how to|what are the steps to)\\s*(.+)",
			Pattern.CASE_INSENSITIVE
	);

	public HowToQueryHandler(SearchService searchService) {
		if (searchService == null) {
			throw new IllegalArgumentException("SearchService cannot be null.");
		}
		this.searchService = searchService;
	}

	@Override
	public String handle(String userInput, ConversationContext context) {
		// THE FIX: Wrap the entire method body in a try-finally block.
		try {
			if (userInput == null || context == null) {
				Log.w(TAG, "User input or context is null.");
				return "I'm sorry, something went wrong.";
			}

			Matcher newSearchMatcher = NEW_SEARCH_PATTERN.matcher(userInput);

			if (newSearchMatcher.find()) {
				String topic = newSearchMatcher.group(1).trim().replaceAll("\\?$", "");
				Log.d(TAG, "Performing 'how to' search for topic: " + topic);
				return performNewSearch(topic);
			} else {
				// With the new one-shot logic, "try another" will be treated as a new search.
				// This path becomes a fallback.
				Log.d(TAG, "Input did not match 'how to' pattern, treating as new search: " + userInput);
				return performNewSearch(userInput);
			}
		} finally {
			// This block ensures the context is cleared, fixing the trap.
			if (context != null) {
				Log.d(TAG, "HowToQueryHandler finished. Popping context.");
				context.popContext();
			}
		}
	}

	private String performNewSearch(String topic) {
		try {
			// FIX: Changed List<SearchResult> to List<Snippet>
			Optional<List<Snippet>> resultsOpt = searchService.getSearchResults(topic);

			if (resultsOpt.isPresent() && !resultsOpt.get().isEmpty()) {
				List<Snippet> results = resultsOpt.get();
				Log.d(TAG, "Found " + results.size() + " search results for '" + topic + "'.");
				if (results.get(0) == null) {
					Log.w(TAG, "First search result was null for topic: " + topic);
					return "I found some results, but there was an issue displaying the first one.";
				}
				// Now only returns the first result.
				return formatResult(results.get(0));
			} else {
				Log.d(TAG, "No search results found for topic: " + topic);
				return String.format(
						"That's a great question about how to %s. I couldn't find any quick instructions for that right now.",
						topic
				);
			}
		} catch (SearchServiceException e) {
			Log.e(TAG, "SearchServiceException while searching for topic '" + topic + "': " + e.getMessage(), e);
			return "I'm sorry, my search service seems to be unavailable at the moment. Please try again later.";
		}
	}

	// FIX: Changed SearchResult to Snippet
	private String formatResult(Snippet result) {
		if (result == null) {
			Log.w(TAG, "formatResult called with a null Snippet object.");
			return "I found a result, but there was an issue displaying its details.";
		}
		String titleText = result.title != null ? result.title : "No title available.";
		String snippetText = result.snippet != null ? result.snippet : "No snippet available.";
		String linkText = result.link != null ? result.link : "No link available.";

		return String.format(
				"Title: %s\nSnippet: \"%s...\"\nSource: %s",
				titleText, snippetText, linkText
		);
	}
}