package com.f9ld3.xavier.ai.V2.services;

import com.f9ld3.xavier.ai.V2.utils.SharedHttpClient;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;

/**
 * A service to determine the approximate geographic location of the server
 * by using a public IP geolocation API. This is used to provide local context
 * for queries when no location is specified.
 */
public class IPGeolocationService {
private static final String API_URL = "http://ip-api.com/json";

/**
 * Fetches the current location based on the machine's public IP address.
 * @return An Optional containing a JsonObject with location data if successful.
 */
public Optional<JsonObject> getCurrentLocation() {
	try {
		HttpRequest request = HttpRequest.newBuilder()
				                      .uri(URI.create(API_URL))
				                      .timeout(Duration.ofSeconds(5)) // Use a shorter timeout for this service
				                      .GET()
				                      .build();
		
		HttpResponse<String> response = SharedHttpClient.get().send(request, HttpResponse.BodyHandlers.ofString());
		
		if (response.statusCode() != 200) {
			System.err.println("IP Geolocation API failed with status: " + response.statusCode());
			return Optional.empty();
		}
		
		return Optional.of(JsonParser.parseString(response.body()).getAsJsonObject());
	} catch (Exception e) {
		System.err.println("Error during IP Geolocation lookup: " + e.getMessage());
		return Optional.empty();
	}
}
}