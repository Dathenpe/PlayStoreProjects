package com.f9ld3.xavier.ai.V2.handlers;

import com.f9ld3.xavier.ai.V2.ConversationContext;
import com.f9ld3.xavier.ai.V2.EntityExtractor; // Import the new class

import java.util.Random;

public class WeatherQueryHandler implements IntentHandler {
private final Random random = new Random();

@Override
public String handle(String userInput, ConversationContext context) {
	// 1. Try to extract a location from the current user input.
	String location = EntityExtractor.extractLocation(userInput);
	
	if (location != null) {
		// 2. If a location is found, store it in the context for future reference.
		context.setEntity("location", location);
		// 3. Provide the forecast for the found location.
		return getSimulatedWeatherFor(location);
	} else {
		// 4. If no location is found, ask the user for one.
		return "I can tell you the weather. Which location are you interested in?";
	}
}

// This method remains the same, but is now called with a specific location.
public String getSimulatedWeatherFor(String location) {
	String[] conditions = {"sunny", "cloudy", "rainy", "windy", "partly cloudy"};
	String[] temperatures = {"a pleasant 22°C", "a chilly 15°C", "a warm 28°C", "a crisp 18°C"};
	
	String condition = conditions[random.nextInt(conditions.length)];
	String temperature = temperatures[random.nextInt(temperatures.length)];
	
	// Capitalize the first letter of the location for a nicer response.
	String formattedLocation = location.substring(0, 1).toUpperCase() + location.substring(1);
	
	return "The forecast for " + formattedLocation + " looks " + condition + " with a temperature of " + temperature + ".";
}
}