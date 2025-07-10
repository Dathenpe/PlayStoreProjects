package com.f9ld3.xavier.ai.V2.handlers;

import com.f9ld3.xavier.ai.V2.ConversationContext;
import com.f9ld3.xavier.ai.V2.EntityExtractor; // Import the new class

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class FollowUpHandler implements IntentHandler {
@Override
public String handle(String userInput, ConversationContext context) {
	String lastIntent = context.getLastIntent();
	
	// Handle a follow-up about weather
	if ("weather_query".equals(lastIntent)) {
		String location = EntityExtractor.extractLocation(userInput);
		if (location != null) {
			// A new location was mentioned in the follow-up.
			context.setEntity("location", location); // Update context
			WeatherQueryHandler weatherHandler = new WeatherQueryHandler();
			return weatherHandler.getSimulatedWeatherFor(location);
		} else {
			// No new location, maybe they are asking about the previous one?
			String previousLocation = (String) context.getEntity("location");
			if (previousLocation != null) {
				return "I'm not sure what you mean. Are you still asking about " + previousLocation + "?";
			}
		}
	}
	
	// Handle a follow-up about date
	if ("date_query".equals(lastIntent) && userInput.toLowerCase().contains("tomorrow")) {
		LocalDate tomorrow = LocalDate.now().plusDays(1);
		return "Tomorrow's date will be " + tomorrow.format(DateTimeFormatter.ofPattern("MMMM d, yyyy"));
	}
	
	return "I'm not sure how to follow up on that. Could you please ask a full question?";
}
}