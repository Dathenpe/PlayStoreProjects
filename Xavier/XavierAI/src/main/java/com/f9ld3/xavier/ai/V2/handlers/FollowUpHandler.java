package com.f9ld3.xavier.ai.V2.handlers;

import com.f9ld3.xavier.ai.V2.ConversationContext;
import com.f9ld3.xavier.ai.V2.WolframAlphaClient;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

/**
 * A highly intelligent handler for contextual follow-up questions.
 * It now understands location, date, and general knowledge context.
 */
public class FollowUpHandler implements IntentHandler {

private final WeatherQueryHandler weatherHandler;
private final TimezoneQueryHandler timezoneHandler;
private final WolframAlphaClient wolframAlphaClient; // NEW: For knowledge follow-ups

// Keywords that signal a request for more detail.
private static final List<String> DETAIL_KEYWORDS = Arrays.asList(
		"more", "detail", "comprehensive", "elaborate", "deeper"
);

public FollowUpHandler(WeatherQueryHandler weatherHandler, TimezoneQueryHandler timezoneHandler, WolframAlphaClient wolframAlphaClient) {
	this.weatherHandler = weatherHandler;
	this.timezoneHandler = timezoneHandler;
	this.wolframAlphaClient = wolframAlphaClient; // NEW
}

@Override
public String handle(String userInput, ConversationContext context) {
	String lastIntent = context.getLastIntent();
	String lowerInput = userInput.toLowerCase();
	
	// --- Knowledge-Based Follow-up ---
	if ("knowledge_query".equals(lastIntent) && context.getLastSubject() != null) {
		// Check if the user is asking for more details about the last subject.
		if (DETAIL_KEYWORDS.stream().anyMatch(lowerInput::contains)) {
			// Construct a new, more explicit query.
			String newQuery = userInput + " about " + context.getLastSubject();
			System.out.println("[DEBUG] Follow-up constructed new query: '" + newQuery + "'");
			return this.wolframAlphaClient.getShortAnswer(newQuery);
		}
	}
	
	// --- Location-Based Follow-up ---
	String location = (String) context.getEntity("location");
	if (location != null) {
		if (lowerInput.contains("weather")) {
			return this.weatherHandler.handle("weather in " + location, context);
		}
		if (lowerInput.contains("time")) {
			return this.timezoneHandler.handle("time in " + location, context);
		}
	}
	
	// --- Date-Based Follow-up ---
	if ("date_query".equals(lastIntent) && lowerInput.contains("tomorrow")) {
		LocalDate tomorrow = LocalDate.now().plusDays(1);
		return "Tomorrow's date will be " + tomorrow.format(DateTimeFormatter.ofPattern("MMMM d, yyyy"));
	}
	
	return "I'm not sure how to follow up on that. Could you please ask a full question?";
}
}