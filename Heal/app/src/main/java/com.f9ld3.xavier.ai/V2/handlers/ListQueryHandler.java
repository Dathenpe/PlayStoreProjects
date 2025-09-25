package com.f9ld3.xavier.ai.V2.handlers;

import android.util.Log;

import com.f9ld3.xavier.ai.V2.ConversationContext;
import com.f9ld3.xavier.ai.V2.services.GenerativeService;
import com.f9ld3.xavier.ai.V2.services.GenerativeService.GenerativeServiceException;
import com.f9ld3.xavier.ai.V2.services.SearchService;
import com.f9ld3.xavier.ai.V2.services.SearchService.SearchServiceException;
import com.f9ld3.xavier.ai.V2.services.SearchService.Snippet; // <<<<<< FIX: Changed from SearchResult to Snippet

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class ListQueryHandler implements IntentHandler {

	private static final String TAG = "ListQueryHandler";
	private final GenerativeService generativeService;
	private final SearchService searchService;

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
			context.popContext(); // Pop context even on error
			return "I'm sorry, I didn't understand that.";
		}

		String topic = extractTopic(userInput);

		// **THE FIX IS HERE**: Use a try-finally block to guarantee cleanup.
		try {
			if (topic.isBlank()) {
				Log.d(TAG, "Extracted topic is blank from input: " + userInput);
				return "What kind of list are you looking for?";
			}

			// --- PRIMARY STRATEGY: Attempt to generate the list directly ---
			try {
				String prompt = "Generate a concise, bulleted list of " + topic;
				Log.d(TAG, "Attempting to generate list for topic: " + topic + " with prompt: \"" + prompt + "\"");
				Optional<String> generatedListOpt = generativeService.generate(prompt);

				if (generatedListOpt.isPresent()) {
					String generatedList = generatedListOpt.get();
					Log.d(TAG, "Successfully generated list for topic: " + topic);
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
			} catch (Exception e) {
				Log.e(TAG, "Unexpected error from GenerativeService for topic '" + topic + "': " + e.getMessage(), e);
			}

			// --- FALLBACK STRATEGY: If generation fails, find a web link ---
			try {
				String searchQuery = createSearchQuery(topic);
				Log.d(TAG, "Falling back to web search for topic: " + topic + " with search query: \"" + searchQuery + "\"");

				// FIX: Use List<Snippet> instead of List<SearchResult>
				Optional<List<Snippet>> resultsOpt = searchService.getSearchResults(searchQuery);

				if (resultsOpt.isPresent() && !resultsOpt.get().isEmpty()) {
					// FIX: Use Snippet instead of SearchResult
					Snippet firstResult = resultsOpt.get().get(0);
					if (firstResult != null) {
						String title = firstResult.title != null ? firstResult.title : "No title";
						String link = firstResult.link != null ? firstResult.link : "No link";
						Log.d(TAG, "Found search result for fallback: " + title);
						return String.format(
								"I couldn't generate that list for you, but I found a web page that might help with '%s'.\n\nTitle: %s\nSource: %s",
								topic, title, link
						);
					}
				}
			} catch (SearchServiceException e) {
				Log.w(TAG, "Search service fallback also failed for list query on topic '" + topic + "'. Reason: " + e.getMessage(), e);
			} catch (Exception e) {
				Log.e(TAG, "Unexpected error from SearchService for topic '" + topic + "': " + e.getMessage(), e);
			}

			Log.d(TAG, "Both generative and search fallbacks failed for topic: " + topic);
			return "I tried to generate a list of '" + topic + "' and search for it, but I couldn't find anything. Sorry about that!";

		} finally {
			// This code will always run before the method returns, ensuring the context is cleared.
			Log.d(TAG, "ListQueryHandler finished. Popping context to prevent getting stuck.");
			context.popContext();
		}
	}

	private String extractTopic(String userInput) {
		if (userInput == null) return "";

		Matcher matcher = TRIGGER_PATTERN.matcher(userInput);
		String topic = userInput;

		if (matcher.find()) {
			topic = userInput.substring(matcher.end()).trim();
		}
		topic = topic.replaceAll("\\?$", "").trim();
		return topic;
	}

	private String createSearchQuery(String topic) {
		if (topic == null) return "";
		return topic.replaceAll("(?i)^(\\d+\\s+)?(common|popular|top|best)\\s+", "").trim();
	}
}