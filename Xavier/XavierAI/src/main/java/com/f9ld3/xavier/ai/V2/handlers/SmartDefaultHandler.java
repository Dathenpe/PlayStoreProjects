package com.f9ld3.xavier.ai.V2.handlers;

import com.f9ld3.xavier.ai.V2.ConversationContext;
import com.f9ld3.xavier.ai.V2.WolframAlphaClient;
import com.f9ld3.xavier.ai.V2.WolframAlphaResult;
import com.f9ld3.xavier.ai.V2.services.SearchService;
import com.f9ld3.xavier.ai.V2.services.SearchService.SearchResult;
import com.f9ld3.xavier.ai.V2.utils.ResponseGenerator;

import java.util.List;
import java.util.Optional;

/**
 * An intelligent default handler that acts as a final safety net.
 * Instead of simply giving up, it actively tries to answer unrecognized input
 * by treating it as a general knowledge question.
 */
public class SmartDefaultHandler implements IntentHandler {

private final WolframAlphaClient wolframClient;
private final SearchService searchService;

public SmartDefaultHandler(WolframAlphaClient wolframClient, SearchService searchService) {
	this.wolframClient = wolframClient;
	this.searchService = searchService;
}

@Override
public String handle(String userInput, ConversationContext context) {
	// --- PRIMARY STRATEGY: Attempt to get a factual answer from Wolfram|Alpha ---
	Optional<WolframAlphaResult> resultOpt = wolframClient.getFullResult(userInput);
	
	if (resultOpt.isPresent()) {
		WolframAlphaResult result = resultOpt.get();
		// Format the response similarly to the KnowledgeQueryHandler for consistency.
		return result.getAnswer() + "\n(Source: Wolfram|Alpha)";
	}
	
	// --- FALLBACK STRATEGY: If Wolfram fails, try a web search ---
	try {
		Optional<List<SearchResult>> searchResultsOpt = searchService.getSearchResults(userInput);
		if (searchResultsOpt.isPresent() && !searchResultsOpt.get().isEmpty()) {
			SearchResult firstResult = searchResultsOpt.get().get(0);
			return String.format(
					"I wasn't sure how to respond, but I found a web page that might be related:\n\nTitle: %s\nSource: %s",
					firstResult.title(),
					firstResult.link()
			);
		}
	} catch (SearchService.SearchServiceException e) {
		// If the search service itself fails, we log it and fall through to the final response.
		System.err.println("WARN: SmartDefaultHandler fallback search failed. Reason: " + e.getMessage());
	}
	
	// --- FINAL FALLBACK: If all attempts fail, give a generic response ---
	return ResponseGenerator.getIntelligentFallback();
}
}