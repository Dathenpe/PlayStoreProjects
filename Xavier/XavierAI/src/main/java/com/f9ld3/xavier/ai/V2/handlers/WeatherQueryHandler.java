// C:/Users/Music_Minister/Desktop/PlayStore/PlayStoreProjects/Xavier/XavierAI/src/main/java/com/f9ld3/xavier/ai/V2/handlers/WeatherQueryHandler.java
package com.f9ld3.xavier.ai.V2.handlers;

import com.f9ld3.xavier.ai.V2.ConversationContext;
import com.f9ld3.xavier.ai.V2.XavierCoreV2;
import com.f9ld3.xavier.ai.V2.services.IPGeolocationService;
import com.f9ld3.xavier.ai.V2.services.LocationResolverService;
import com.f9ld3.xavier.ai.V2.utils.EntityExtractor;
import com.f9ld3.xavier.ai.V2.utils.SharedHttpClient;
import com.f9ld3.xavier.ai.V2.utils.WeatherParser;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;

/**
 * Handles weather queries. It can get a location from the context,
 * automatically detect the user's location via IP if none is provided, or
 * ask for a location as a final fallback.
 * REFACTORED: Now fully integrated with the new context stack and entity system.
 */
public class WeatherQueryHandler implements IntentHandler {

private final LocationResolverService locationResolver;
private final IPGeolocationService ipGeolocationService;
private final String apiKey;
private static final String WEATHER_API_URL = "https://api.openweathermap.org/data/2.5/weather?lat=%s&lon=%s&appid=%s&units=metric";

public WeatherQueryHandler(LocationResolverService locationResolver, IPGeolocationService ipGeolocationService, String apiKey) {
	this.locationResolver = locationResolver;
	this.ipGeolocationService = ipGeolocationService;
	this.apiKey = apiKey;
}

@Override
public String handle(String userInput, ConversationContext context) {
	// UPDATED: Use the new context API to safely get the location entity.
	Optional<String> locationOpt = context.getEntityFromCurrentContext("subject")
			                               .map(String::valueOf)
			                               .filter(s -> !s.isBlank()); // Ensure it's not blank
	
	// Case 1: A specific location was extracted by the core pipeline.
	if (locationOpt.isPresent()) {
		return getWeatherForLocation(locationOpt.get(), context);
	}
	
	// FIX: Add a fallback to manually extract the location if it's not in the context.
	String cleanedInput = userInput.toLowerCase().trim();
	boolean isGenericWeatherQuery = cleanedInput.matches("(?i).*(what's|what is|tell me|the)?\\s*(weather|forecast|temperature)\\??$");
	
	if (!isGenericWeatherQuery) {
		String extractedLocation = EntityExtractor.extractLocation(userInput);
		if (extractedLocation != null && !extractedLocation.isBlank()) {
			if (XavierCoreV2.DEBUG_MODE) System.out.println("[DEBUG] WeatherQueryHandler: No entity in context, but manually extracted '" + extractedLocation + "'.");
			return getWeatherForLocation(extractedLocation, context);
		}
	}
	
	// Case 2: No location mentioned. Try to get it automatically via IP.
	if (XavierCoreV2.DEBUG_MODE) System.out.println("[DEBUG] No valid location in weather query. Attempting IP Geolocation.");
	Optional<JsonObject> geoDataOpt = ipGeolocationService.getCurrentLocation();
	
	if (geoDataOpt.isPresent()) {
		JsonObject geoData = geoDataOpt.get();
		JsonElement cityElement = geoData.get("city");
		if (cityElement != null && !cityElement.isJsonNull()) {
			String city = cityElement.getAsString();
			if (XavierCoreV2.DEBUG_MODE) System.out.printf("[DEBUG] IP Geolocation success. Found city: %s%n", city);
			return getWeatherForLocation(city, context);
		}
	}
	
	// Case 3: IP Geolocation failed or no location was found. Fallback to asking the user.
	if (XavierCoreV2.DEBUG_MODE) System.out.println("[DEBUG] IP Geolocation failed. Asking user for location.");
	return "Of course. For which location would you like the weather forecast?";
}

/**
 * Private helper to resolve a location name and fetch weather data.
 * @param location The name of the location (e.g., "paris", "ontario").
 * @param context The current conversation context.
 * @return A formatted string with the weather information or an error message.
 */
private String getWeatherForLocation(String location, ConversationContext context) {
	try {
		// 1. Resolve the location name to coordinates (lat, lon).
		JsonObject geoData = locationResolver.resolve(location);
		String lat = geoData.get("lat").getAsString();
		String lon = geoData.get("lon").getAsString();
		
		// 2. Build the request and call the weather API using the shared client.
		String url = String.format(WEATHER_API_URL, lat, lon, apiKey);
		
		HttpRequest request = HttpRequest.newBuilder()
				                      .uri(URI.create(url))
				                      .timeout(Duration.ofSeconds(10))
				                      .GET()
				                      .build();
		
		HttpResponse<String> response = SharedHttpClient.get().send(request, HttpResponse.BodyHandlers.ofString());
		
		if (response.statusCode() != 200) {
			throw new IOException("Weather API request failed with status code: " + response.statusCode());
		}
		
		String jsonResponse = response.body();
		JsonObject weatherData = JsonParser.parseString(jsonResponse).getAsJsonObject();
		
		// 3. Parse the complex JSON into a simple, user-friendly string.
		return WeatherParser.parse(weatherData);
		
	} catch (Exception e) {
		System.err.println("Weather Handler Error: " + e.getMessage());
		
		// More specific error handling
		String errorMessage = e.getMessage().toLowerCase();
		if (errorMessage.contains("api") || errorMessage.contains("401") || errorMessage.contains("failed")) {
			return "I'm having trouble connecting to my weather service at the moment. This could be due to an invalid API key or a network issue.";
		}
		return "I ran into an unexpected error trying to get the weather for '" + location + "'. My apologies.";
	}
}
}