package com.f9ld3.xavier.ai.V2.handlers;

import android.util.Log;

import com.f9ld3.xavier.ai.V2.ConversationContext;
import com.f9ld3.xavier.ai.V2.WolframAlphaClient;
import com.f9ld3.xavier.ai.V2.WolframAlphaResult;
import com.f9ld3.xavier.ai.V2.XavierCoreV2;
import com.f9ld3.xavier.ai.V2.services.SearchService;
import com.f9ld3.xavier.ai.V2.services.SearchService.SearchResult;
import com.f9ld3.xavier.ai.V2.services.SearchService.SearchServiceException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handles general knowledge questions by first querying the Wolfram|Alpha API for factual data.
 * If that fails, it falls back to a general web search for broader topics.
 * REFACTORED: Now fully integrated with the new context stack and entity system.
 */
public class KnowledgeQueryHandler implements IntentHandler {

	private static final String TAG = "KnowledgeQueryHandler";
	private final WolframAlphaClient wolframClient;
	private final SearchService searchService;

	// REFINED: Compile the pattern once for performance.
	private static final Pattern SUBJECT_PATTERN = Pattern.compile("^([^,(]+)");

	// MODIFIED: Use ArrayList and Collections.unmodifiableList for broader Java 8 compatibility
	// if List.of() and streams directly on Arrays.asList() for initialization are an issue
	// for the specific Android/Java environment without full desugaring for these.
	// However, the original stream-based initialization is generally fine with API 24+.
	private static final List<String> PREFIXES_TO_REMOVE;

	static {
		List<String> prefixes = new ArrayList<>(Arrays.asList(
				"tell me about", "can you tell me about", "do you know about",
				"give me information on", "information about", "search for",
				"look up", "find out about", "what do you know about",
				"tell me", "explain", "what is", "what's", "who is", "who's"
		));
		// Sort by length in descending order to match longer prefixes first
		prefixes.sort(Comparator.comparingInt(String::length).reversed());
		PREFIXES_TO_REMOVE = Collections.unmodifiableList(prefixes);
	}
	// Original way (usually fine for API 24+):
	// static {
	//     PREFIXES_TO_REMOVE = Arrays.asList(
	//                     "tell me about", "can you tell me about", "do you know about",
	//                     "give me information on", "information about", "search for",
	//                     "look up", "find out about", "what do you know about",
	//                     "tell me", "explain", "what is", "what's", "who is", "who's"
	//             ).stream()
	//                                  .sorted(Comparator.comparingInt(String::length).reversed())
	//                                  .collect(Collectors.toList());
	// }


	public KnowledgeQueryHandler(WolframAlphaClient wolframClient, SearchService searchService) {
		if (wolframClient == null) {
			throw new IllegalArgumentException("WolframAlphaClient cannot be null.");
		}
		if (searchService == null) {
			throw new IllegalArgumentException("SearchService cannot be null.");
		}
		this.wolframClient = wolframClient;
		this.searchService = searchService;
	}

	@Override
	public String handle(String userInput, ConversationContext context) {
		if (userInput == null || context == null) {
			Log.w(TAG, "User input or context is null.");
			return "I'm sorry, something went wrong.";
		}

		// Note: wolframClient is checked for null in the constructor now.
		// If it could be disabled later, this check is fine.
		// if (wolframClient == null) {
		//     Log.w(TAG, "WolframAlphaClient is not available.");
		//     return "I'm sorry, my knowledge base is currently unavailable.";
		// }

		String queryToSend = extractQuery(userInput);
		if (queryToSend.isBlank()) {
			Log.d(TAG, "Extracted query is blank. Original input: " + userInput);
			// Decide how to handle a blank query after extraction.
			// For now, let it proceed, Wolfram/Search might handle it or return empty.
		}

		if (XavierCoreV2.DEBUG_MODE) { // Assuming DEBUG_MODE is a public static final boolean
			Log.d(TAG, String.format("KnowledgeQueryHandler: Original input: '%s'", userInput));
			Log.d(TAG, String.format("KnowledgeQueryHandler: Sending cleaned query to Wolfram|Alpha: '%s'", queryToSend));
		}

		// --- PRIMARY STRATEGY: Attempt to get a factual answer from Wolfram|Alpha ---
		// `getFullResult` is expected to be called on a background thread.
		Optional<WolframAlphaResult> resultOpt = wolframClient.getFullResult(queryToSend);

		if (resultOpt.isPresent()) {
			WolframAlphaResult result = resultOpt.get();
			// Assuming WolframAlphaResult has getAnswer() and getInterpretation()
			String answer = result.getAnswer() != null ? result.getAnswer() : "";
			String interpretation = result.getInterpretation() != null ? result.getInterpretation() : "";

			String subject = extractSubjectFromAnswer(answer);
			context.addEntityToCurrentContext("subject", subject);
			if (XavierCoreV2.DEBUG_MODE) {
				Log.d(TAG, String.format("KnowledgeQueryHandler: Added entity 'subject': '%s'", subject));
			}

			StringBuilder responseBuilder = new StringBuilder();

			if (!interpretation.isEmpty() && !interpretation.equalsIgnoreCase(queryToSend)) {
				String formattedInterpretation = formatInterpretation(interpretation);
				responseBuilder.append(String.format("Assuming you meant '%s':\n", formattedInterpretation));
			}

			String cleanedAnswer = answer.replace(" | ", ": ").replace("... | ", ". ");
			responseBuilder.append(cleanedAnswer);
			responseBuilder.append("\n(Source: Wolfram|Alpha)");

			return responseBuilder.toString();
		} else {
			// --- FALLBACK STRATEGY - If Wolfram fails, try a web search ---
			if (XavierCoreV2.DEBUG_MODE) {
				Log.d(TAG, "KnowledgeQueryHandler: Wolfram|Alpha failed. Falling back to web search for: " + queryToSend);
			}

			try {
				// `getSearchResults` is expected to be called on a background thread.
				// The original userInput is passed to search service, not the cleaned queryToSend,
				// which might be intentional for broader search context.
				Optional<List<SearchResult>> searchResultsOpt = searchService.getSearchResults(userInput);

				if (searchResultsOpt.isPresent() && !searchResultsOpt.get().isEmpty()) {
					SearchResult firstResult = searchResultsOpt.get().get(0);
					if (firstResult != null) {
						// Assuming SearchResult has getTitle() and getLink()
						String title = firstResult.getTitle() != null ? firstResult.getTitle() : "No title";
						String link = firstResult.getLink() != null ? firstResult.getLink() : "No link";

						context.addEntityToCurrentContext("subject", title);
						if (XavierCoreV2.DEBUG_MODE) {
							Log.d(TAG, String.format("KnowledgeQueryHandler: Added entity from search: '%s'", title));
						}

						return String.format(
								"I couldn't find a direct answer, but I found a web page that might help:\n\nTitle: %s\nSource: %s",
								title,
								link
						);
					} else {
						Log.w(TAG, "First search result was null from SearchService for query: " + userInput);
					}
				} else {
					Log.d(TAG, "SearchService returned no results for query: " + userInput);
				}
			} catch (SearchServiceException e) {
				Log.e(TAG, "KnowledgeQueryHandler fallback search failed. Reason: " + e.getMessage(), e);
				// Fall through to the final fallback message.
			} catch (Exception e) { // Catch any other unexpected exception from searchService
				Log.e(TAG, "Unexpected error during SearchService call: " + e.getMessage(), e);
			}

			// --- FINAL FALLBACK: Both Wolfram and Web Search failed ---
			return "That's a great question, but I couldn't find a specific answer for it at the moment.";
		}
	}

	/**
	 * REFINED: A helper to strip common conversational prefixes while preserving the core question.
	 * This version no longer uses a broad stop-word list, which is less likely to remove
	 * important keywords from the query.
	 */
	public String extractQuery(String userInput) {
		if (userInput == null) return ""; // Handle null input

		String query = userInput.toLowerCase().replaceAll("\\?$", "").trim();

		if (query.startsWith("xavier ")) { // "xavier" should probably be dynamic or configurable
			query = query.substring("xavier ".length()).trim();
		}

		for (String prefix : PREFIXES_TO_REMOVE) {
			// Use startsWith to check for the prefix followed by a space to avoid partial matches.
			if (query.startsWith(prefix + " ")) {
				return query.substring(prefix.length()).trim();
			}
		}
		// If no prefix was found, return the cleaned query.
		return query;
	}

	/**
	 * A helper method to extract the primary subject from a typical API answer string.
	 * @param answer The answer string from the API.
	 * @return The cleaned subject.
	 */
	private String extractSubjectFromAnswer(String answer) {
		if (answer == null || answer.isBlank()) {
			return "";
		}
		// Use the pre-compiled pattern for better performance.
		Matcher matcher = SUBJECT_PATTERN.matcher(answer);
		if (matcher.find()) {
			return matcher.group(1).trim();
		}
		return answer.trim();
	}

	/**
	 * A helper method to format the interpretation string from Wolfram|Alpha into a more readable format.
	 * For example, it turns "Nigeria | continent" into "the continent of Nigeria".
	 *
	 * @param interpretation The raw interpretation string from the API.
	 * @return A formatted, more conversational string.
	 */
	private String formatInterpretation(String interpretation) {
		if (interpretation == null || interpretation.isBlank()) {
			return ""; // Or return original interpretation if that's preferred for blank
		}

		String[] parts = interpretation.split("\\s*\\|\\s*");
		if (parts.length == 2) {
			String topic = parts[0].trim();
			String property = parts[1].trim();

			// Ensure the topic isn't empty before trying to capitalize it.
			if (!topic.isEmpty()) {
				topic = topic.substring(0, 1).toUpperCase() + topic.substring(1);
			}

			// Handle special cases for better grammar
			if (property.endsWith("s")) { // e.g., "actors" -> "the actors in"
				return String.format("the %s in %s", property, topic);
			}
			// Simple "the property of Topic"
			return String.format("the %s of %s", property, topic);
		}
		// Fallback for interpretations that don't fit the "topic | property" pattern.
		return interpretation;
	}
}

