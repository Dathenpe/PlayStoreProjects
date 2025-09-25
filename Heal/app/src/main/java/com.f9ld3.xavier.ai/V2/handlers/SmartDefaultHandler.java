package com.f9ld3.xavier.ai.V2.handlers;

import com.f9ld3.xavier.ai.V2.ConversationContext;
import com.f9ld3.xavier.ai.V2.WolframAlphaClient;
import com.f9ld3.xavier.ai.V2.WolframAlphaResult;
import com.f9ld3.xavier.ai.V2.services.SearchService;
import com.f9ld3.xavier.ai.V2.services.SearchService.Snippet; // <<<< FIX: Changed from SearchResult to Snippet
import com.f9ld3.xavier.ai.V2.utils.ResponseGenerator;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * An intelligent default handler that acts as a final safety net.
 * Instead of simply giving up, it actively tries to answer unrecognized input
 * by treating it as a general knowledge question.
 */
public class SmartDefaultHandler implements IntentHandler {

	private final WolframAlphaClient wolframClient;
	private final SearchService searchService;

	// REFINED: Compile the pattern once for performance.
	private static final Pattern SUBJECT_PATTERN = Pattern.compile("^([^,(]+)");

	private static final List<String> PREFIXES_TO_REMOVE;

	static {
		// REFINED: Use a stream for a more modern and declarative initialization.
		PREFIXES_TO_REMOVE = Arrays.asList(
						"tell me about", "can you tell me about", "do you know about",
						"give me information on", "information about", "search for",
						"look up", "find out about", "what do you know about",
						"tell me", "explain", "what is", "what's", "who is", "who's"
				).stream()
				.sorted(Comparator.comparingInt(String::length).reversed())
				.collect(Collectors.toList());
	}

	public SmartDefaultHandler(WolframAlphaClient wolframClient, SearchService searchService) {
		this.wolframClient = wolframClient;
		this.searchService = searchService;
	}

	@Override
	public String handle(String userInput, ConversationContext context) {
		// FIX: Extract a clean query to prevent passing conversational phrases
		// like "whats the weather" to the API.
		String queryToSend = extractQuery(userInput);

		// --- PRIMARY STRATEGY: Attempt to get a factual answer from Wolfram|Alpha ---
		Optional<WolframAlphaResult> resultOpt = wolframClient.getFullResult(queryToSend);

		if (resultOpt.isPresent()) {
			WolframAlphaResult result = resultOpt.get();
			// Format the response similarly to the KnowledgeQueryHandler for consistency.
			return result.getAnswer() + "\n(Source: Wolfram|Alpha)";
		}

		// --- FALLBACK STRATEGY: If Wolfram fails, try a web search ---
		try {
			// FIX: Use List<Snippet> instead of List<SearchResult>
			Optional<List<Snippet>> searchResultsOpt = searchService.getSearchResults(queryToSend);
			if (searchResultsOpt.isPresent() && !searchResultsOpt.get().isEmpty()) {
				// FIX: Use Snippet instead of SearchResult
				Snippet firstResult = searchResultsOpt.get().get(0);
				return String.format(
						"I wasn't sure how to respond, but I found a web page that might be related:\n\nTitle: %s\nSource: %s",
						firstResult.title, // Accessing public field 'title'
						firstResult.link   // Accessing public field 'link'
				);
			}
		} catch (SearchService.SearchServiceException e) {
			// If the search service itself fails, we log it and fall through to the final response.
			System.err.println("WARN: SmartDefaultHandler fallback search failed. Reason: " + e.getMessage());
		}

		// --- FINAL FALLBACK: If all attempts fail, give a generic response ---
		return ResponseGenerator.getIntelligentFallback();
	}

	/**
	 * Helper to strip common conversational prefixes.
	 */
	public String extractQuery(String userInput) {
		String query = userInput.toLowerCase().replaceAll("\\?$", "").trim();

		if (query.startsWith("xavier ")) {
			query = query.substring("xavier ".length());
		}

		for (String prefix : PREFIXES_TO_REMOVE) {
			if (query.startsWith(prefix + " ")) {
				return query.substring(prefix.length()).trim();
			}
		}

		return query;
	}
}