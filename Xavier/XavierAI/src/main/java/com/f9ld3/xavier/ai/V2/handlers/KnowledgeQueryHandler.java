// C:/Users/Music_Minister/Desktop/PlayStore/PlayStoreProjects/Xavier/XavierAI/src/main/java/com/f9ld3/xavier/ai/V2/handlers/KnowledgeQueryHandler.java
package com.f9ld3.xavier.ai.V2.handlers;

import com.f9ld3.xavier.ai.V2.ConversationContext;
import com.f9ld3.xavier.ai.V2.WolframAlphaClient;
import com.f9ld3.xavier.ai.V2.WolframAlphaResult;
import com.f9ld3.xavier.ai.V2.XavierCoreV2;
import com.f9ld3.xavier.ai.V2.services.SearchService;
import com.f9ld3.xavier.ai.V2.services.SearchService.SearchResult;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Handles general knowledge questions by first querying the Wolfram|Alpha API for factual data.
 * If that fails, it falls back to a general web search for broader topics.
 * REFACTORED: Now fully integrated with the new context stack and entity system.
 */
public class KnowledgeQueryHandler implements IntentHandler {

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
	
	if (XavierCoreV2.DEBUG_MODE) {
		System.out.println("[DEBUG] KnowledgeQueryHandler: Original input: '" + userInput + "'");
		System.out.println("[DEBUG] KnowledgeQueryHandler: Sending cleaned query to Wolfram|Alpha: '" + queryToSend + "'");
	}
	
	// --- PRIMARY STRATEGY: Attempt to get a factual answer from Wolfram|Alpha ---
	Optional<WolframAlphaResult> resultOpt = wolframClient.getFullResult(queryToSend);
	
	if (resultOpt.isPresent()) {
		WolframAlphaResult result = resultOpt.get();
		String answer = result.getAnswer();
		String interpretation = result.getInterpretation();
		
		// UPDATED: Use the new entity system to store the subject in the current context.
		String subject = extractSubjectFromAnswer(answer);
		context.addEntityToCurrentContext("subject", subject);
		if (XavierCoreV2.DEBUG_MODE) System.out.printf("[DEBUG] KnowledgeQueryHandler: Added entity 'subject': '%s'%n", subject);
		
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
			System.out.println("[DEBUG] KnowledgeQueryHandler: Wolfram|Alpha failed. Falling back to web search.");
		}
		
		// FIX: Wrap the search service call in a try-catch block for graceful failure.
		try {
			Optional<List<SearchResult>> searchResultsOpt = searchService.getSearchResults(userInput);
			
			if (searchResultsOpt.isPresent() && !searchResultsOpt.get().isEmpty()) {
				SearchResult firstResult = searchResultsOpt.get().get(0);
				
				context.addEntityToCurrentContext("subject", firstResult.title());
				if (XavierCoreV2.DEBUG_MODE) System.out.printf("[DEBUG] KnowledgeQueryHandler: Added entity from search: '%s'%n", firstResult.title());
				
				return String.format(
						"I couldn't find a direct answer, but I found a web page that might help:\n\nTitle: %s\nSource: %s",
						firstResult.title(),
						firstResult.link()
				);
			}
		} catch (SearchService.SearchServiceException e) {
			System.err.println("WARN: KnowledgeQueryHandler fallback search failed. Reason: " + e.getMessage());
			// Fall through to the final fallback message.
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
	String query = userInput.toLowerCase().replaceAll("\\?$", "").trim();
	
	if (query.startsWith("xavier ")) {
		query = query.substring("xavier ".length());
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
		
		return String.format("the %s of %s", property, topic);
	}
	// Fallback for interpretations that don't fit the "topic | property" pattern.
	return interpretation;
}
}