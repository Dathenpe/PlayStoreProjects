package com.f9ld3.xavier.ai.V2.handlers;

import com.f9ld3.xavier.ai.V2.ConversationContext;
import com.f9ld3.xavier.ai.V2.EntityExtractor;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

/**
 * An intelligent handler that can determine the current time in any city
 * by using the OpenWeatherMap API to get timezone information.
 */
public class TimezoneQueryHandler implements IntentHandler {

private static final String API_BASE_URL = "https://api.openweathermap.org/data/2.5/weather";
private final String apiKey;

public TimezoneQueryHandler(String apiKey) {
	this.apiKey = apiKey;
}

@Override
public String handle(String userInput, ConversationContext context) {
	if (apiKey == null) {
		return "I'm sorry, my time service is not configured correctly. I can't fetch timezone data right now.";
	}
	
	String location = EntityExtractor.extractLocation(userInput);
	
	if (location != null) {
		context.setEntity("location", location);
		return getTimeFromAPI(location);
	} else {
		// If no location is found, fall back to a simple time query.
		return new TimeQueryHandler().handle(userInput, context);
	}
}

private String getTimeFromAPI(String location) {
	try {
		// Use modern, explicit charset for encoding
		String encodedLocation = URLEncoder.encode(location, StandardCharsets.UTF_8);
		String requestUrl = String.format("%s?q=%s&appid=%s", API_BASE_URL, encodedLocation, apiKey);
		
		URL url = new URL(requestUrl);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestMethod("GET");
		
		int responseCode = conn.getResponseCode();
		if (responseCode == 401) {
			return "I'm sorry, there seems to be an issue with my time service credentials.";
		} else if (responseCode == 404) {
			return "I'm sorry, I couldn't find a city named '" + location + "' to get the time for.";
		} else if (responseCode != 200) {
			return "I'm sorry, I'm having trouble connecting to the time service right now.";
		}
		
		// Ensure we read the response with the correct UTF-8 encoding
		StringBuilder response = new StringBuilder();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
			String line;
			while ((line = reader.readLine()) != null) {
				response.append(line);
			}
		}
		conn.disconnect();
		
		JSONObject jsonResponse = new JSONObject(response.toString());
		String cityName = jsonResponse.getString("name");
		int timezoneOffsetSeconds = jsonResponse.getInt("timezone");
		
		ZoneOffset zoneOffset = ZoneOffset.ofTotalSeconds(timezoneOffsetSeconds);
		ZonedDateTime locationTime = Instant.now().atZone(zoneOffset);
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("h:mm a");
		
		return String.format("The current time in %s is %s.", cityName, locationTime.format(formatter));
		
	} catch (Exception e) {
		System.err.println("Timezone API Error for location '" + location + "': " + e.getMessage());
		return "I ran into an unexpected error trying to get the time for that location.";
	}
}
}