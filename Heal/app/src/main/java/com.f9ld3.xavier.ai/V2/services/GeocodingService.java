package com.f9ld3.xavier.ai.V2.services;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * A dedicated, resilient service for handling geocoding API calls.
 * This version supports multiple API keys and provides automatic fallback.
 */
public class GeocodingService {

	private static final String TAG = "GeocodingService";
	private static final String API_URL_TEMPLATE = "https://api.openweathermap.org/geo/1.0/direct?q=%s&limit=1&appid=%s";
	private static final long API_TIMEOUT_SECONDS = 5;

	/**
	 * Custom exception for when geocoding ultimately fails after retries or for non-retryable errors.
	 */
	public static class GeocodingException extends Exception { // Keep as checked for critical failures
		public GeocodingException(String message) {
			super(message);
		}
		public GeocodingException(String message, Throwable cause) {
			super(message, cause);
		}
	}

	private final List<String> apiKeys;
	private final OkHttpClient okHttpClient; // Use OkHttpClient
	private final Gson gson;
	private int currentKeyIndex = 0; // For cycling through keys, if preferred over iterating directly

	public GeocodingService(List<String> apiKeys) {
		this(apiKeys, new OkHttpClient.Builder() // Default OkHttpClient
				.connectTimeout(API_TIMEOUT_SECONDS, TimeUnit.SECONDS)
				.readTimeout(API_TIMEOUT_SECONDS, TimeUnit.SECONDS)
				.writeTimeout(API_TIMEOUT_SECONDS, TimeUnit.SECONDS)
				.build());
	}

	// Constructor allowing OkHttpClient injection
	public GeocodingService(List<String> apiKeys, OkHttpClient client) {
		if (apiKeys == null || apiKeys.isEmpty()) {
			throw new IllegalArgumentException("At least one OpenWeatherMap API key is required.");
		}
		this.apiKeys = apiKeys;
		this.okHttpClient = client; // Use injected or default client
		this.gson = new Gson();
	}


	/**
	 * Converts a location name into geographic coordinates, trying multiple API keys if necessary.
	 * This method performs network operations and MUST be called from a background thread.
	 *
	 * @param location The name of the location.
	 * @return A JsonObject containing the geocoding data for the first match.
	 * @throws GeocodingException if all API keys fail, the location is definitively not found by the API,
	 *                            or a non-retryable error occurs.
	 */
	public JsonObject getCoordinates(String location) throws GeocodingException {
		if (location == null || location.isBlank()) {
			throw new GeocodingException("Location name cannot be null or empty.");
		}

		String encodedLocation;
		try {
			encodedLocation = URLEncoder.encode(location, StandardCharsets.UTF_8.name());
		} catch (IOException e) { // Covers UnsupportedEncodingException
			throw new GeocodingException("Failed to URL encode location: " + location, e);
		}

		Exception lastException = null;

		// Loop through each provided API key
		for (String apiKey : apiKeys) {
			String url = String.format(API_URL_TEMPLATE, encodedLocation, apiKey);
			Log.d(TAG, "Attempting geocoding for '" + location + "' with key ending in ..." + (apiKey.length() > 4 ? apiKey.substring(apiKey.length() - 4) : apiKey) + " URL: " + url);

			Request request = new Request.Builder()
					.url(url)
					.get() // Default, but explicit
					.build();

			// THIS IS A SYNCHRONOUS (BLOCKING) NETWORK CALL.
			try (Response response = okHttpClient.newCall(request).execute()) {
				ResponseBody responseBody = response.body();
				String responseBodyString = (responseBody != null) ? responseBody.string() : null; // Consume once

				// Success Case: If we get a 200 OK
				if (response.isSuccessful() && responseBodyString != null) {
					// OpenWeatherMap Geocoding API returns a JSON array
					JsonArray resultsArray = gson.fromJson(responseBodyString, JsonArray.class);
					if (resultsArray == null || resultsArray.size() == 0) {
						// This is a valid response, but the location wasn't found by the API.
						// This is a definitive "not found" for this API, so we don't retry with other keys for this specific case.
						Log.i(TAG, "Location not found by API for: " + location + " (API returned empty array)");
						throw new GeocodingException("Location not found by API: " + location);
					}
					// Return the first result from the array
					return resultsArray.get(0).getAsJsonObject();
				}

				// Retryable Failure Case: 401 (Unauthorized) or 429 (Too Many Requests)
				if (response.code() == 401 || response.code() == 429) {
					Log.w(TAG, String.format("Geocoding API key ending in '...%s' failed with status %d. Trying next key.",
							(apiKey.length() > 4 ? apiKey.substring(apiKey.length() - 4) : apiKey), response.code()));
					lastException = new IOException("API key failed with status " + response.code()); // Use IOException for retries
					continue; // Move to the next key
				}

				// Non-Retryable Failure Case for other HTTP errors
				Log.e(TAG, "Failed to geocode location. Status: " + response.code() + ", Body: " + responseBodyString);
				throw new GeocodingException("Failed to geocode location. Status: " + response.code() + ", Body: " + (responseBodyString != null ? responseBodyString : "N/A"));

			} catch (JsonSyntaxException e) {
				Log.e(TAG, "Error parsing geocoding JSON for location '" + location + "'. Key ending in ..." + (apiKey.length() > 4 ? apiKey.substring(apiKey.length() - 4) : apiKey), e);
				lastException = e;
				// Treat JSON parsing error for a specific key as a failure for that key, try next
			} catch (IOException e) { // Covers network issues
				Log.e(TAG, "Network error during geocoding for location '" + location + "'. Key ending in ..." + (apiKey.length() > 4 ? apiKey.substring(apiKey.length() - 4) : apiKey), e);
				lastException = e;
				// If it's a network error, we might want to try the next key
			} catch (GeocodingException e) { // Catch the re-thrown "Location not found by API"
				throw e; // Re-throw immediately, do not try other keys
			} catch (Exception e) { // Catch any other unexpected errors
				Log.e(TAG, "Unexpected error during geocoding for location '" + location + "'. Key ending in ..." + (apiKey.length() > 4 ? apiKey.substring(apiKey.length() - 4) : apiKey), e);
				lastException = e;
			}
		} // End of for loop (API key retry)

		// If the loop completes without returning or throwing "Location not found by API", all keys have failed for other reasons.
		String errorMessage = "All geocoding API keys failed to get a valid response for location: " + location;
		if (lastException != null) {
			throw new GeocodingException(errorMessage + ". Last error: " + lastException.getMessage(), lastException);
		} else {
			throw new GeocodingException(errorMessage);
		}
	}
}
