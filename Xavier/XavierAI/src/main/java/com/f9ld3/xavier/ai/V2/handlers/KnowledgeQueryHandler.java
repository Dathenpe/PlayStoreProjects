// C:/Users/Music_Minister/Desktop/PlayStore/PlayStoreProjects/Xavier/XavierAI/src/main/java/com/f9ld3/xavier/ai/V2/handlers/KnowledgeQueryHandler.java

package com.f9ld3.xavier.ai.V2.handlers;

import com.f9ld3.xavier.ai.V2.ConversationContext;
import com.f9ld3.xavier.ai.V2.WolframAlphaClient;
import com.f9ld3.xavier.ai.V2.WolframAlphaResult;
import com.f9ld3.xavier.ai.V2.XavierCoreV2;
// --- NEW: Import search service components ---
import com.f9ld3.xavier.ai.V2.services.SearchService;
import com.f9ld3.xavier.ai.V2.services.SearchService.SearchResult;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handles general knowledge questions by first querying the Wolfram|Alpha API for factual data.
 * If that fails, it falls back to a general web search for broader topics.
 */
public class KnowledgeQueryHandler implements IntentHandler {

private final WolframAlphaClient wolframClient;
private final SearchService searchService; // NEW: Add SearchService as a dependency

// This list is static, final, and initialized only once for efficiency.
private static final List<String> PREFIXES_TO_REMOVE;

static {
	List<String> prefixes = Arrays.asList(
			"tell me about", "can you tell me about", "do you know about",
			"give me information on", "information about", "search for",
			"look up", "find out about", "what do you know about",
			"tell me", "explain"
	);
	prefixes.sort(Comparator.comparingInt(String::length).reversed());
	PREFIXES_TO_REMOVE = prefixes;
}
private static final Set<String> STOP_WORDS = Set.of(
		"a", "an", "the", "of", "in", "on", "at", "for", "to", "is", "are", "was", "were",
		"who", "what", "when", "where", "why", "how", "do", "does", "did", "can", "could",
		"would", "should", "tell", "me", "about", "republic"
);

// --- UPDATED: Constructor now accepts both clients ---
public KnowledgeQueryHandler(WolframAlphaClient wolframClient, SearchService searchService) {
	this.wolframClient = wolframClient;
	this.searchService = searchService;
}

@Override
public String handle(String userInput, ConversationContext context) {
	if (wolframClient == null) {
		return "I'm sorry, my knowledge base is currently unavailable.";
	}
	
	String queryToSend = extractQuery(userInput);
	if (queryToSend.isEmpty()) {
		queryToSend = userInput;
	}
	
	if (XavierCoreV2.DEBUG_MODE) {
		System.out.println("[DEBUG] KnowledgeQueryHandler: Original input: '" + userInput + "'");
		System.out.println("[DEBUG] KnowledgeQueryHandler: Sending cleaned query to Wolfram|Alpha: '" + queryToSend + "'");
	}
	
	// --- PRIMARY STRATEGY: Attempt to get a factual answer from Wolfram|Alpha ---
	Optional<WolframAlphaResult> resultOpt = wolframClient.getFullResult(queryToSend);
	
	if (resultOpt.isPresent()) {
		context.clearLastFailedInput();
		WolframAlphaResult result = resultOpt.get();
		String answer = result.getAnswer();
		String interpretation = result.getInterpretation();
		
		String subject = extractSubjectFromAnswer(answer);
		context.setLastSubject(subject);
		if (XavierCoreV2.DEBUG_MODE) System.out.printf("[DEBUG] KnowledgeQueryHandler: Setting last subject from answer: '%s'%n", subject);
		
		StringBuilder responseBuilder = new StringBuilder();
		
		if (!interpretation.isEmpty() && !interpretation.equalsIgnoreCase(queryToSend)) {
			responseBuilder.append(String.format("Assuming you meant '%s':\n", interpretation));
		}
		
		String cleanedAnswer = answer.replace(" | ", ": ").replace("... | ", ". ");
		responseBuilder.append(cleanedAnswer);
		
		responseBuilder.append("\n(Source: Wolfram|Alpha)");
		
		return responseBuilder.toString();
	} else {
		// --- NEW: FALLBACK STRATEGY - If Wolfram fails, try a web search ---
		if (XavierCoreV2.DEBUG_MODE) {
			System.out.println("[DEBUG] KnowledgeQueryHandler: Wolfram|Alpha failed. Falling back to web search.");
		}
		
		// Use the original userInput for the web search for better context.
		Optional<List<SearchResult>> searchResultsOpt = searchService.getSearchResults(userInput);
		
		if (searchResultsOpt.isPresent() && !searchResultsOpt.get().isEmpty()) {
			SearchResult firstResult = searchResultsOpt.get().get(0);
			context.clearLastFailedInput();
			// Provide a slightly different response to indicate it's a web result
			return String.format(
					"I couldn't find a direct answer, but I found a web page that might help:\n\nTitle: %s\nSource: %s",
					firstResult.title(),
					firstResult.link()
			);
		} else {
			// --- FINAL FALLBACK: Both Wolfram and Web Search failed ---
			context.setLastFailedInput(userInput);
			return "That's a great question, but I couldn't find a specific answer for it.";
		}
	}
}

/**
 * A helper to strip common conversational prefixes while preserving the core question.
 */
public String extractQuery(String userInput) {
	String query = userInput.toLowerCase().replaceAll("\\?$", "").trim();
	
	if (query.startsWith("xavier ")) {
		query = query.substring("xavier ".length());
	}
	
	for (String prefix : PREFIXES_TO_REMOVE) {
		if (query.startsWith(prefix + " ")) {
			query = query.substring(prefix.length()).trim();
			break;
		}
	}
	
	String[] words = query.split("\\s+");
	StringBuilder finalQuery = new StringBuilder();
	for (String word : words) {
		if (!STOP_WORDS.contains(word)) {
			finalQuery.append(word).append(" ");
		}
	}
	
	return finalQuery.toString().trim();
}

/**
 * A new helper method to extract the primary subject from a typical API answer string.
 * @param answer The answer string from the API.
 * @return The cleaned subject.
 */
private String extractSubjectFromAnswer(String answer) {
	if (answer == null || answer.isBlank()) {
		return "";
	}
	Pattern subjectPattern = Pattern.compile("^([^,(]+)");
	Matcher matcher = subjectPattern.matcher(answer);
	if (matcher.find()) {
		return matcher.group(1).trim();
	}
	return answer.trim();
}
}