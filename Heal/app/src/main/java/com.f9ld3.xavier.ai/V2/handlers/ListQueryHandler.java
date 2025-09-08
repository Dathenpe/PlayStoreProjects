package com.f9ld3.xavier.ai.V2.handlers;

import android.util.Log; // Using Android's Log

import com.f9ld3.xavier.ai.V2.ConversationContext;
import com.f9ld3.xavier.ai.V2.services.GenerativeService;
import com.f9ld3.xavier.ai.V2.services.GenerativeService.GenerativeServiceException;
import com.f9ld3.xavier.ai.V2.services.SearchService;
import com.f9ld3.xavier.ai.V2.services.SearchService.SearchServiceException;
import com.f9ld3.xavier.ai.V2.services.SearchService.SearchResult; // Assuming SearchResult is a class with getters

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A specialized handler for queries that ask for a list of items.
 * It first attempts to use a GenerativeService to create the list directly.
 * If that fails, it falls back to using the SearchService.
 */
public class ListQueryHandler implements IntentHandler {

	private static final String TAG = "ListQueryHandler";
	private final GenerativeService generativeService;
	private final SearchService searchService;

	// Pattern to identify trigger phrases for list queries.
	private static final Pattern TRIGGER_PATTERN = Pattern.compile(
			"^(?i)(?:list of|give me a list of|name some|what are some)\\s+",
			Pattern.CASE_INSENSITIVE
	);

	public ListQueryHandler(GenerativeService generativeService, SearchService searchService) {
		if (generativeService == null) {
			throw new IllegalArgumentException("GenerativeService cannot be null.");
		}
		if (searchService == null) {
			throw new IllegalArgumentException("SearchService cannot be null.");
		}
		this.generativeService = generativeService;
		this.searchService = searchService;
	}

	@Override
	public String handle(String userInput, ConversationContext context) {
		if (userInput == null) {
			Log.w(TAG, "User input is null.");
			// Or return a default error message if context can be null too
			return "I'm sorry, I didn't understand that.";
		}

		String topic = extractTopic(userInput);
		if (topic.isBlank()) {
			Log.d(TAG, "Extracted topic is blank from input: " + userInput);
			// Default response if no meaningful topic could be extracted
			return "What kind of list are you looking for?";
		}

		// --- PRIMARY STRATEGY: Attempt to generate the list directly ---
		// This part (generativeService.generate) is expected to run on a background thread.
		try {
			String prompt = "Generate a concise, bulleted list of " + topic;
			Log.d(TAG, "Attempting to generate list for topic: " + topic + " with prompt: \"" + prompt + "\"");
			Optional<String> generatedListOpt = generativeService.generate(prompt);

			if (generatedListOpt.isPresent()) {
				String generatedList = generatedListOpt.get();
				Log.d(TAG, "Successfully generated list for topic: " + topic);
				// Intelligently add a prefix only if the response doesn't already have one.
				String lowerGeneratedList = generatedList.toLowerCase();
				if (lowerGeneratedList.startsWith("here") || lowerGeneratedList.startsWith("sure")) {
					return generatedList;
				} else {
					return "Here is a list I generated for you:\n\n" + generatedList;
				}
			}
			Log.d(TAG, "Generative service returned no list for topic: " + topic);
		} catch (GenerativeServiceException e) {
			Log.w(TAG, "Generative list failed for topic '" + topic + "', falling back to web search. Reason: " + e.getMessage(), e);
			// Fall through to the search service fallback.
		} catch (Exception e) { // Catch any other unexpected error from generative service
			Log.e(TAG, "Unexpected error from GenerativeService for topic '" + topic + "': " + e.getMessage(), e);
		}


		// --- FALLBACK STRATEGY: If generation fails, find a web link ---
		// This part (searchService.getSearchResults) is expected to run on a background thread.
		try {
			String searchQuery = createSearchQuery(topic);
			Log.d(TAG, "Falling back to web search for topic: " + topic + " with search query: \"" + searchQuery + "\"");
			Optional<List<SearchResult>> resultsOpt = searchService.getSearchResults(searchQuery);

			if (resultsOpt.isPresent() && !resultsOpt.get().isEmpty()) {
				SearchResult firstResult = resultsOpt.get().get(0);
				if (firstResult != null) {
					// Assuming SearchResult has getTitle() and getLink()
					String title = firstResult.getTitle() != null ? firstResult.getTitle() : "No title";
					String link = firstResult.getLink() != null ? firstResult.getLink() : "No link";
					Log.d(TAG, "Found search result for fallback: " + title);

					return String.format(
							"I couldn't generate that list for you, but I found a web page that might help with '%s'.\n\nTitle: %s\nSource: %s",
							topic,
							title,
							link
					);
				} else {
					Log.w(TAG, "First search result was null for topic: " + topic);
				}
			} else {
				Log.d(TAG, "Search service returned no results for fallback on topic: " + topic);
			}
		} catch (SearchServiceException e) {
			Log.w(TAG, "Search service fallback also failed for list query on topic '" + topic + "'. Reason: " + e.getMessage(), e);
		} catch (Exception e) { // Catch any other unexpected error from search service
			Log.e(TAG, "Unexpected error from SearchService for topic '" + topic + "': " + e.getMessage(), e);
		}

		Log.d(TAG, "Both generative and search fallbacks failed for topic: " + topic);
		return "I tried to generate a list of '" + topic + "' and search for it, but I couldn't find anything. Sorry about that!";
	}

	/**
	 * Extracts the core topic from the user input by removing trigger phrases.
	 * Example: "list of popular dog breeds" -> "popular dog breeds"
	 *
	 * @param userInput The full user input string.
	 * @return The extracted topic, or the original input if no trigger phrase is matched.
	 */
	private String extractTopic(String userInput) {
		if (userInput == null) return ""; // Handle null input

		Matcher matcher = TRIGGER_PATTERN.matcher(userInput);
		String topic = userInput; // Default to original input

		if (matcher.find()) {
			// Get the part after the matched trigger phrase
			topic = userInput.substring(matcher.end()).trim();
		}
		// Remove trailing question mark and trim again
		topic = topic.replaceAll("\\?$", "").trim();

		// If after processing the topic is blank, it means the input was likely just the trigger phrase
		// or trigger phrase + question mark. In this case, it's better to consider the original input
		// (minus question mark) as the topic, or decide if it's too vague.
		// For now, if blank, we let it be blank, and the caller (handle method) can decide.
		return topic;
	}

	/**
	 * Creates a more targeted search query from the extracted topic,
	 * often by removing common adjectives like "top", "best".
	 * Example: "popular dog breeds" -> "dog breeds"
	 *
	 * @param topic The topic extracted from the user's list query.
	 * @return A refined search query string.
	 */
	private String createSearchQuery(String topic) {
		if (topic == null) return ""; // Handle null input
		// Removes leading digits (e.g., "10 common...") and common adjectives.
		return topic.replaceAll("(?i)^(\\d+\\s+)?(common|popular|top|best)\\s+", "").trim();
	}
}
