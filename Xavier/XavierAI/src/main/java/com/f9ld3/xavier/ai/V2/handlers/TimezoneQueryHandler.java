package com.f9ld3.xavier.ai.V2.handlers;

import com.f9ld3.xavier.ai.V2.ConversationContext;
import com.f9ld3.xavier.ai.V2.XavierCoreV2;
import com.f9ld3.xavier.ai.V2.services.IPGeolocationService;
import com.f9ld3.xavier.ai.V2.services.LocationResolverService;
import com.f9ld3.xavier.ai.V2.utils.SharedHttpClient;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.Optional;

/**
 * A unified and intelligent handler for all time-related queries.
 * REFACTORED: Now fully integrated with the new context stack and entity system.
 */
public class TimezoneQueryHandler implements IntentHandler {

private final LocationResolverService locationResolver;
private final IPGeolocationService ipGeolocationService;
private final String apiKey; // For OpenWeatherMap API

private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("h:mm a");
private static final String WEATHER_API_URL_FOR_TIMEZONE = "https://api.openweathermap.org/data/2.5/weather?lat=%s&lon=%s&appid=%s";

public TimezoneQueryHandler(LocationResolverService locationResolver, IPGeolocationService ipGeolocationService, String apiKey) {
	this.locationResolver = locationResolver;
	this.ipGeolocationService = ipGeolocationService;
	this.apiKey = apiKey;
}

@Override
public String handle(String userInput, ConversationContext context) {
	// UPDATED: Use the new context API to safely get the location entity.
	// The core pipeline now uses the generic "subject" key for location-based queries.
	Optional<String> locationOpt = context.getEntityFromCurrentContext("subject")
			                               .map(String::valueOf);
	
	// Case 1: A specific location was extracted by the core pipeline.
	if (locationOpt.isPresent() && !locationOpt.get().isBlank()) {
		return getTimeForLocation(locationOpt.get(), context);
	}
	
	// Case 2: No location mentioned. Try to get it automatically via IP.
	if (XavierCoreV2.DEBUG_MODE) System.out.println("[DEBUG] No location in time query context. Attempting IP Geolocation.");
	Optional<JsonObject> geoDataOpt = ipGeolocationService.getCurrentLocation();
	
	if (geoDataOpt.isPresent()) {
		JsonObject geoData = geoDataOpt.get();
		JsonElement timezoneElement = geoData.get("timezone");
		if (timezoneElement != null && !timezoneElement.isJsonNull()) {
			try {
				String timezoneId = timezoneElement.getAsString();
				ZoneId zoneId = ZoneId.of(timezoneId);
				if (XavierCoreV2.DEBUG_MODE) System.out.printf("[DEBUG] IP Geolocation success. Found timezone: %s%n", timezoneId);
				String locationName = geoData.get("city").getAsString() + ", " + geoData.get("country_code").getAsString();
				return formatTimeResponse(ZonedDateTime.now(zoneId), locationName);
			} catch (Exception e) {
				if (XavierCoreV2.DEBUG_MODE) System.err.println("IP Geolocation returned invalid timezone. Falling back. Error: " + e.getMessage());
			}
		}
	}
	
	// Case 3: IP Geolocation failed or no location was found. Fallback to server's system time.
	if (XavierCoreV2.DEBUG_MODE) System.out.println("[DEBUG] IP Geolocation failed. Falling back to system default time.");
	ZonedDateTime systemTime = ZonedDateTime.now(ZoneId.systemDefault());
	String zoneName = systemTime.getZone().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
	return formatTimeResponse(systemTime, "my current location (" + zoneName + ")");
}

private String getTimeForLocation(String location, ConversationContext context) {
	try {
		JsonObject geoData = locationResolver.resolve(location);
		JsonElement timezoneElement = geoData.get("timezone");
		
		ZoneId zoneId;
		
		// Handle geocoding results that don't include a timezone ID by fetching it via coordinates.
		if (timezoneElement == null || timezoneElement.isJsonNull()) {
			if (XavierCoreV2.DEBUG_MODE) {
				System.out.println("[DEBUG] Timezone ID missing for '" + location + "'. Fetching offset from coordinates.");
			}
			if (apiKey == null || apiKey.isBlank()) {
				return "I can find '" + location + "', but I need a configured API key to determine its timezone from coordinates. My apologies.";
			}
			String lat = geoData.get("lat").getAsString();
			String lon = geoData.get("lon").getAsString();
			
			Optional<Integer> offsetSecondsOpt = fetchTimezoneOffsetFromCoords(lat, lon);
			if (offsetSecondsOpt.isPresent()) {
				zoneId = ZoneOffset.ofTotalSeconds(offsetSecondsOpt.get());
			} else {
				return "I found '" + location + "', but I'm having trouble determining its timezone from its coordinates. My apologies.";
			}
		} else {
			String timezoneIdStr = timezoneElement.getAsString();
			zoneId = ZoneId.of(timezoneIdStr);
		}
		
		// Get a user-friendly display name from the geocoding data
		String displayName = geoData.has("name") ? geoData.get("name").getAsString() : location;
		if (geoData.has("country")) {
			displayName += ", " + geoData.get("country").getAsString();
		}
		
		return formatTimeResponse(ZonedDateTime.now(zoneId), displayName);
		
	} catch (Exception e) {
		System.err.println("Time Handler Error: " + e.getMessage());
		// UPDATED: The 'setLastFailedInput' method is no longer needed.
		// The context stack handles conversational state.
		return "I'm sorry, I ran into an issue trying to find the time for '" + location + "'. Please try again.";
	}
}

private Optional<Integer> fetchTimezoneOffsetFromCoords(String lat, String lon) {
	try {
		String url = String.format(WEATHER_API_URL_FOR_TIMEZONE, lat, lon, apiKey);
		HttpRequest request = HttpRequest.newBuilder()
				                      .uri(URI.create(url))
				                      .timeout(Duration.ofSeconds(10))
				                      .GET()
				                      .build();
		
		HttpResponse<String> response = SharedHttpClient.get().send(request, HttpResponse.BodyHandlers.ofString());
		
		if (response.statusCode() != 200) {
			if (XavierCoreV2.DEBUG_MODE) System.err.println("Timezone offset fetch failed with status code: " + response.statusCode());
			return Optional.empty();
		}
		
		JsonObject weatherData = JsonParser.parseString(response.body()).getAsJsonObject();
		if (weatherData.has("timezone")) {
			return Optional.of(weatherData.get("timezone").getAsInt());
		}
	} catch (IOException | InterruptedException e) {
		System.err.println("Error fetching timezone offset from coordinates: " + e.getMessage());
	}
	return Optional.empty();
}

/**
 * Formats the final time response in a clear, human-readable way.
 */
private String formatTimeResponse(ZonedDateTime zonedDateTime, String locationName) {
	String time = zonedDateTime.format(TIME_FORMATTER);
	String day = zonedDateTime.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
	String zoneAbbreviation = zonedDateTime.getZone().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
	return String.format("In %s, the current time is %s (%s) on %s.", locationName, time, zoneAbbreviation, day);
}
}