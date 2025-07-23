package com.f9ld3.xavier.ai.V2.services;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List; // Import List

/**
 * A dedicated, resilient service for handling geocoding API calls.
 * This version supports multiple API keys and provides automatic fallback.
 */
public class GeocodingService {

private final List<String> apiKeys; // Now a list of keys
private final HttpClient httpClient;
private final Gson gson;

public GeocodingService(List<String> apiKeys) {
	if (apiKeys == null || apiKeys.isEmpty()) {
		throw new IllegalArgumentException("At least one OpenWeatherMap API key is required.");
	}
	this.apiKeys = apiKeys;
	this.httpClient = HttpClient.newHttpClient();
	this.gson = new Gson();
}

/**
 * Converts a location name into geographic coordinates, trying multiple API keys if necessary.
 * @param location The name of the location.
 * @return A JsonObject containing the geocoding data.
 * @throws Exception if all API keys fail or the location is not found.
 */
public JsonObject getCoordinates(String location) throws Exception {
	String encodedLocation = URLEncoder.encode(location, StandardCharsets.UTF_8);
	Exception lastException = null;
	
	// Loop through each provided API key
	for (String apiKey : apiKeys) {
		String url = String.format("https://api.openweathermap.org/geo/1.0/direct?q=%s&limit=1&appid=%s", encodedLocation, apiKey);
		HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).build();
		
		try {
			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
			
			// Success Case: If we get a 200 OK, we process and return immediately.
			if (response.statusCode() == 200) {
				JsonObject[] results = gson.fromJson(response.body(), JsonObject[].class);
				if (results == null || results.length == 0) {
					// This is a valid response, but the location wasn't found. Don't retry.
					throw new RuntimeException("Location not found by API.");
				}
				return results[0];
			}
			
			// Retryable Failure Case: 401 (Unauthorized) or 429 (Too Many Requests)
			if (response.statusCode() == 401 || response.statusCode() == 429) {
				System.err.printf("[WARN] Geocoding API key ending in '...%s' failed with status %d. Trying next key.%n",
						apiKey.substring(Math.max(0, apiKey.length() - 4)), response.statusCode());
				lastException = new RuntimeException("API key failed with status " + response.statusCode());
				continue; // Move to the next key
			}
			
			// Non-Retryable Failure Case: For any other error, fail immediately.
			throw new RuntimeException("Failed to geocode location. Status: " + response.statusCode() + ", Body: " + response.body());
			
		} catch (Exception e) {
			lastException = e;
			// If the exception is "Location not found", we should not try other keys.
			if (e.getMessage().contains("Location not found")) {
				break;
			}
		}
	}
	
	// If the loop completes without returning, all keys have failed.
	throw new RuntimeException("All geocoding API keys failed to get a valid response.", lastException);
}
}