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
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * A robust handler for finding the current time in any location worldwide.
 * It now delegates all location resolution to a dedicated service.
 */
public class TimezoneQueryHandler implements IntentHandler {

private final LocationResolverService locationResolver; // New dependency
private final Gson gson;

public TimezoneQueryHandler(LocationResolverService locationResolver) {
	this.locationResolver = locationResolver;
	this.gson = new Gson();
}

@Override
public String handle(String userInput, ConversationContext context) {
	String location = EntityExtractor.extractLocation(userInput);
	
	if (location == null || location.isEmpty()) {
		return "I'm sorry, I didn't catch the location. Where would you like to know the time?";
	}
	
	if (location.toLowerCase().startsWith("what") || location.toLowerCase().startsWith("is it") || location.toLowerCase().startsWith("do you")) {
		context.setPendingIntent("timezone_query");
		return "I can certainly tell you the time. Which city or country are you interested in?";
	}
	
	try {
		// The complex if/else logic is now gone, replaced by a single, clean call.
		JsonObject geoData = locationResolver.resolve(location);
		
		double lat = geoData.get("lat").getAsDouble();
		double lon = geoData.get("lon").getAsDouble();
		String locationName = geoData.get("name").getAsString() + ", " + geoData.get("country").getAsString();
		
		JsonObject timeData = getTimezoneData(lat, lon);
		String timezoneId = timeData.get("timezone").getAsString();
		
		String timeString = timeData.getAsJsonObject("current_weather").get("time").getAsString();
		LocalDateTime localDateTime = LocalDateTime.parse(timeString);
		ZonedDateTime zonedDateTime = localDateTime.atZone(ZoneId.of(timezoneId));
		
		String formattedTime = zonedDateTime.format(DateTimeFormatter.ofPattern("h:mm a"));
		
		context.setEntity("location", locationName);
		return String.format("The current time in %s is %s.", locationName, formattedTime);
		
	} catch (Exception e) {
		System.err.println("Timezone API Error: " + e.getMessage());
		if (e.getMessage().contains("Location not found")) {
			return String.format("I couldn't find a specific city in my database for '%s'. Could you be more specific?", location);
		}
		return String.format("I'm sorry, I ran into an issue trying to find the time for '%s'. Please try again.", location);
	}
}

private JsonObject getTimezoneData(double lat, double lon) throws Exception {
	String url = String.format("https://api.open-meteo.com/v1/forecast?latitude=%.4f&longitude=%.4f&current_weather=true", lat, lon);
	HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).build();
	HttpResponse<String> response = SharedHttpClient.get().send(request, HttpResponse.BodyHandlers.ofString());
	if (response.statusCode() != 200) {
		throw new RuntimeException("Failed to get timezone data: " + response.body());
	}
	return gson.fromJson(response.body(), JsonObject.class);
}
}