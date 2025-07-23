package com.f9ld3.xavier.ai.V2.handlers;

import com.f9ld3.xavier.ai.V2.ConversationContext;
// Import the new resolver service
import com.f9ld3.xavier.ai.V2.services.LocationResolverService;
import com.f9ld3.xavier.ai.V2.utils.EntityExtractor;
import com.f9ld3.xavier.ai.V2.utils.SharedHttpClient;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * A modern, robust weather handler that delegates all location resolution
 * to a dedicated service, making it architecturally consistent.
 */
public class WeatherQueryHandler implements IntentHandler {

private static final String API_BASE_URL = "https://api.openweathermap.org/data/2.5/weather";
private final LocationResolverService locationResolver; // New dependency
private final String apiKey; // Still needed for the weather call itself
private final Gson gson;

public WeatherQueryHandler(LocationResolverService locationResolver, String apiKey) {
	this.locationResolver = locationResolver;
	this.apiKey = apiKey;
	this.gson = new Gson();
}

@Override
public String handle(String userInput, ConversationContext context) {
	// Fail-fast: Check for valid API key first.
	if (apiKey == null || apiKey.isEmpty() || apiKey.contains("YOUR_")) {
		return "I'm sorry, my weather service is not configured correctly. I can't fetch forecasts right now.";
	}
	
	String location = EntityExtractor.extractLocation(userInput);
	
	// Check if the extracted "location" is actually a question asking for a location.
	if (location.toLowerCase().startsWith("what") || location.toLowerCase().startsWith("is it") || location.toLowerCase().startsWith("do i")) {
		context.setPendingIntent("weather_query"); // Set the pending intent
		return "I can certainly check the weather for you. Which city are you interested in?";
	}
	
	try {
		// The complex if/else logic is now gone, replaced by a single, clean call.
		JsonObject geoData = locationResolver.resolve(location);
		
		// Use the coordinates to get the weather data.
		return getWeatherFromAPI(geoData);
		
	} catch (Exception e) {
		System.err.println("Weather Handler Error: " + e.getMessage());
		if (e.getMessage().contains("Location not found")) {
			return String.format("I couldn't find a city named '%s' in my weather database. Please check the spelling.", location);
		}
		return "I ran into an unexpected error trying to get the weather. My apologies.";
	}
}

private String getWeatherFromAPI(JsonObject geoData) throws Exception {
	double lat = geoData.get("lat").getAsDouble();
	double lon = geoData.get("lon").getAsDouble();
	
	String requestUrl = String.format("%s?lat=%.4f&lon=%.4f&appid=%s&units=metric", API_BASE_URL, lat, lon, apiKey);
	
	HttpRequest request = HttpRequest.newBuilder().uri(URI.create(requestUrl)).build();
	HttpResponse<String> response = SharedHttpClient.get().send(request, HttpResponse.BodyHandlers.ofString());
	
	if (response.statusCode() != 200) {
		throw new RuntimeException("Failed to get weather data: " + response.body());
	}
	
	JsonObject jsonResponse = gson.fromJson(response.body(), JsonObject.class);
	String cityName = jsonResponse.get("name").getAsString();
	String description = jsonResponse.getAsJsonArray("weather").get(0).getAsJsonObject().get("description").getAsString();
	double temp = jsonResponse.getAsJsonObject("main").get("temp").getAsDouble();
	
	return String.format("The current weather in %s is %.0f°C with %s.", cityName, temp, description);
}
}