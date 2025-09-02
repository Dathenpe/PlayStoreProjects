package com.f9ld3.xavier.ai.V2.handlers;

import com.f9ld3.xavier.ai.V2.ConversationContext;
import com.f9ld3.xavier.ai.V2.handlers.IntentHandler;
import com.f9ld3.xavier.ai.V2.services.GenerativeService;
import com.f9ld3.xavier.ai.V2.services.SearchService;
import com.f9ld3.xavier.ai.V2.services.SearchService.SearchResult;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A specialized handler for queries that ask for a list of items.
 * It first attempts to use a GenerativeService to create the list directly.
 * If that fails, it falls back to using the SearchService to find a relevant web page.
 */
public class ListQueryHandler implements IntentHandler {

private final GenerativeService generativeService;
private final SearchService searchService; // Kept as a fallback

private static final Pattern TRIGGER_PATTERN = Pattern.compile(
		"^(?i)(?:list of|tell me|give me|name|what are)\\s+",
		Pattern.CASE_INSENSITIVE
);

public ListQueryHandler(GenerativeService generativeService, SearchService searchService) {
	this.generativeService = generativeService;
	this.searchService = searchService;
}

@Override
public String handle(String userInput, ConversationContext context) {
	String topic = extractTopic(userInput);
	
	// --- PRIMARY STRATEGY: Attempt to generate the list directly ---
	String prompt = "Generate a list of " + topic;
	Optional<String> generatedList = generativeService.generate(prompt);
	
	if (generatedList.isPresent()) {
		return "Here is a list I generated for you:\n\n" + generatedList.get();
	}
	
	// --- FALLBACK STRATEGY: If generation fails, find a web link ---
	System.out.println("Generative list failed, falling back to web search...");
	String searchQuery = createSearchQuery(topic);
	Optional<List<SearchResult>> resultsOpt = searchService.getSearchResults(searchQuery);
	
	if (resultsOpt.isPresent() && !resultsOpt.get().isEmpty()) {
		SearchResult firstResult = resultsOpt.get().get(0);
		return String.format(
				"I couldn't generate that list for you, but I found a web page that might help with '%s'.\n\nTitle: %s\nSource: %s",
				topic,
				firstResult.title(),
				firstResult.link()
		);
	} else {
		return "I tried to generate a list of '" + topic + "' and search for it, but I couldn't find anything. Sorry about that!";
	}
}

private String extractTopic(String userInput) {
	Matcher matcher = TRIGGER_PATTERN.matcher(userInput);
	String topic = userInput;
	
	if (matcher.find()) {
		topic = userInput.substring(matcher.end()).trim();
	}
	topic = topic.replaceAll("\\?$", "").trim();
	return topic.isBlank() ? userInput : topic;
}

private String createSearchQuery(String topic) {
	String searchQuery = topic
			                     .replaceAll("(?i)^(\\d+\\s+)?(common|popular|top|best)\\s+", "")
			                     .trim();
	return searchQuery.isBlank() ? topic : searchQuery;
}
}