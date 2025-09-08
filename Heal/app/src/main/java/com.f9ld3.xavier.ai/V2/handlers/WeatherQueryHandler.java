package com.f9ld3.xavier.ai.V2.handlers;

import android.util.Log;

import com.f9ld3.xavier.ai.V2.ConversationContext;
import com.f9ld3.xavier.ai.V2.XavierCoreV2;
import com.f9ld3.xavier.ai.V2.services.IPGeolocationService;
import com.f9ld3.xavier.ai.V2.services.LocationResolverService;
import com.f9ld3.xavier.ai.V2.utils.EntityExtractor;
import com.f9ld3.xavier.ai.V2.utils.SharedHttpClient;
import com.f9ld3.xavier.ai.V2.utils.WeatherParser;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.util.Optional;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
// import java.util.concurrent.TimeUnit; // Not directly needed here if SharedHttpClient configures timeouts

/**
 * Handles weather queries. It can get a location from the context,
 * automatically detect the user's location via IP if none is provided, or
 * ask for a location as a final fallback.
 * REFACTORED: Now fully integrated with the new context stack and entity system.
 */
public class WeatherQueryHandler implements IntentHandler {

	private static final String TAG = "WeatherQueryHandler";
	private final LocationResolverService locationResolver;
	private final IPGeolocationService ipGeolocationService;
	private final String apiKey; // For OpenWeatherMap API
	private final OkHttpClient httpClient; // Store the client

	private static final String WEATHER_API_URL = "https://api.openweathermap.org/data/2.5/weather?lat=%s&lon=%s&appid=%s&units=metric";

	public WeatherQueryHandler(LocationResolverService locationResolver,
							   IPGeolocationService ipGeolocationService,
							   String apiKey) {
		if (locationResolver == null) {
			throw new IllegalArgumentException("LocationResolverService cannot be null.");
		}
		if (ipGeolocationService == null) {
			throw new IllegalArgumentException("IPGeolocationService cannot be null.");
		}
		// apiKey can be null/blank if not used for all weather providers, but this handler relies on it for OpenWeatherMap
		if (apiKey == null || apiKey.isBlank()) {
			Log.w(TAG, "WeatherQueryHandler initialized without an API key. Weather functionality will be limited.");
			// Consider throwing an IllegalArgumentException if API key is essential for all operations
		}
		this.locationResolver = locationResolver;
		this.ipGeolocationService = ipGeolocationService;
		this.apiKey = apiKey;
		this.httpClient = SharedHttpClient.get(); // Get OkHttpClient instance
	}

	/**
	 * This method involves network calls via its helper methods and MUST be run on a background thread.
	 */
	@Override
	public String handle(String userInput, ConversationContext context) {
		if (userInput == null || context == null) {
			Log.w(TAG, "User input or context is null.");
			return "I'm sorry, something went wrong.";
		}

		// UPDATED: Use the new context API to safely get the location entity.
		Optional<String> locationOpt = context.getEntityFromCurrentContext("subject")
				.filter(obj -> obj instanceof String) // Ensure it's a String
				.map(String::valueOf)
				.filter(s -> !s.isBlank()); // Ensure it's not blank

		// Case 1: A specific location was extracted by the core pipeline.
		if (locationOpt.isPresent()) {
			Log.d(TAG, "Location found in context: " + locationOpt.get());
			return getWeatherForLocation(locationOpt.get(), context);
		}

		// FIX: Add a fallback to manually extract the location if it's not in the context.
		String cleanedInput = userInput.toLowerCase().trim();
		boolean isGenericWeatherQuery = cleanedInput.matches("(?i).*(what's|what is|tell me|the)?\\s*(weather|forecast|temperature)\\??$");

		if (!isGenericWeatherQuery) {
			String extractedLocation = EntityExtractor.extractLocation(userInput); // Assuming EntityExtractor is safe
			if (extractedLocation != null && !extractedLocation.isBlank()) {
				if (XavierCoreV2.DEBUG_MODE) { // Assuming DEBUG_MODE is public static final
					Log.d(TAG, "No entity in context, but manually extracted '" + extractedLocation + "'.");
				}
				return getWeatherForLocation(extractedLocation, context);
			}
		}

		// Case 2: No location mentioned. Try to get it automatically via IP.
		// This part (ipGeolocationService.getCurrentLocation()) is expected to run on a background thread.
		if (XavierCoreV2.DEBUG_MODE) {
			Log.d(TAG, "No valid location in weather query or generic query. Attempting IP Geolocation.");
		}
		try {
			Optional<JsonObject> geoDataOpt = ipGeolocationService.getCurrentLocation();

			if (geoDataOpt.isPresent()) {
				JsonObject geoData = geoDataOpt.get();
				JsonElement cityElement = geoData.get("city");
				if (cityElement != null && !cityElement.isJsonNull()) {
					String city = cityElement.getAsString();
					if (XavierCoreV2.DEBUG_MODE) {
						Log.d(TAG, String.format("IP Geolocation success. Found city: %s", city));
					}
					// Consider also getting country for less ambiguity, e.g., Paris, US vs Paris, FR
					// String countryCode = geoData.has("country_code") && !geoData.get("country_code").isJsonNull() ?
					//                        geoData.get("country_code").getAsString() : "";
					// String locationForWeather = city + (countryCode.isEmpty() ? "" : ", " + countryCode);
					return getWeatherForLocation(city, context);
				} else {
					if (XavierCoreV2.DEBUG_MODE) Log.d(TAG, "IP Geolocation data missing 'city' field.");
				}
			} else {
				if (XavierCoreV2.DEBUG_MODE) Log.d(TAG, "IP Geolocation service returned no data.");
			}
		} catch (Exception e) { // Catch any exception from IPGeolocationService
			if (XavierCoreV2.DEBUG_MODE) {
				Log.e(TAG, "Error during IP Geolocation for weather: " + e.getMessage(), e);
			}
		}

		// Case 3: IP Geolocation failed or no location was found. Fallback to asking the user.
		if (XavierCoreV2.DEBUG_MODE) {
			Log.d(TAG, "IP Geolocation failed for weather. Asking user for location.");
		}
		return "Of course. For which location would you like the weather forecast?";
	}

	/**
	 * Private helper to resolve a location name and fetch weather data.
	 * This method involves network calls and MUST be run on a background thread.
	 * @param location The name of the location (e.g., "paris", "ontario").
	 * @param context The current conversation context.
	 * @return A formatted string with the weather information or an error message.
	 */
	private String getWeatherForLocation(String location, ConversationContext context) {
		if (location == null || location.isBlank()) {
			Log.w(TAG, "getWeatherForLocation called with null or blank location.");
			return "I need a location to get the weather.";
		}
		if (this.apiKey == null || this.apiKey.isBlank()) {
			Log.e(TAG, "Cannot fetch weather for " + location + ". API key is missing.");
			return "I'm sorry, I'm not configured to fetch weather information at the moment.";
		}

		// This part (locationResolver.resolve) is expected to run on a background thread.
		try {
			// 1. Resolve the location name to coordinates (lat, lon).
			JsonObject geoData = locationResolver.resolve(location); // Can throw if service fails
			if (geoData == null) {
				Log.w(TAG, "LocationResolver returned null for: " + location);
				return "I couldn't find information for the location: " + location;
			}

			if (!geoData.has("lat") || !geoData.has("lon") ||
					geoData.get("lat").isJsonNull() || geoData.get("lon").isJsonNull()) {
				Log.w(TAG, "Latitude or longitude missing in geodata for " + location);
				return "I found '" + location + "', but I'm missing its coordinates to get the weather.";
			}
			String lat = geoData.get("lat").getAsString();
			String lon = geoData.get("lon").getAsString();

			// 2. Build the request and call the weather API using the shared client.
			String url = String.format(WEATHER_API_URL, lat, lon, this.apiKey);
			Log.d(TAG, "Fetching weather from URL: " + url);

			Request okHttpRequest = new Request.Builder()
					.url(url)
					.get() // Default, but explicit
					.build();

			// THIS IS A SYNCHRONOUS (BLOCKING) NETWORK CALL.
			try (Response okHttpResponse = httpClient.newCall(okHttpRequest).execute()) { // httpClient is from SharedHttpClient

				if (!okHttpResponse.isSuccessful()) {
					// Log the response body for more details on errors if possible
					String errorBody = "";
					try (ResponseBody tempBody = okHttpResponse.body()){ // ensure body is closed
						if (tempBody != null) errorBody = tempBody.string();
					} catch (Exception bodyEx) { /* ignore */ }

					Log.w(TAG, "Weather API request failed with status code: " + okHttpResponse.code() +
							" for URL: " + url + ". Response: " + errorBody);
					// Specific error message for 401 Unauthorized (likely bad API key)
					if (okHttpResponse.code() == 401) {
						return "I'm having trouble authenticating with the weather service. Please check the API key.";
					}
					return "I'm having trouble connecting to my weather service right now. Please try again a bit later.";
				}

				ResponseBody responseBody = okHttpResponse.body();
				if (responseBody == null) {
					Log.w(TAG, "Weather API response body was null for URL: " + url);
					return "I received an empty response from the weather service.";
				}

				String jsonResponse = responseBody.string();
				JsonObject weatherData = JsonParser.parseString(jsonResponse).getAsJsonObject();

				// 3. Parse the complex JSON into a simple, user-friendly string.
				// Assuming WeatherParser.parse is Android compatible and handles potential errors
				return WeatherParser.parse(weatherData);

			} // Response is closed here

		} catch (JsonParseException | IllegalStateException | ClassCastException e) {
			Log.e(TAG, "Error parsing JSON for weather for '" + location + "': " + e.getMessage(), e);
			return "I received weather data, but couldn't understand it. Sorry!";
		} catch (IOException e) { // Covers OkHttp network issues
			Log.e(TAG, "IOException getting weather for '" + location + "': " + e.getMessage(), e);
			return "I'm having trouble connecting to the weather service. Please check your network connection.";
		} catch (Exception e) { // Catch-all for other service errors (e.g., from locationResolver)
			Log.e(TAG, "General error in getWeatherForLocation for '" + location + "': " + e.getMessage(), e);
			// More specific error handling based on message content
			String errorMessage = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
			if (errorMessage.contains("api") || errorMessage.contains("401") || errorMessage.contains("failed") || errorMessage.contains("unauthorized")) {
				return "I'm having trouble connecting to my weather service at the moment. This could be due to an invalid API key or a network issue.";
			}
			return "I ran into an unexpected error trying to get the weather for '" + location + "'. My apologies.";
		}
	}
}
