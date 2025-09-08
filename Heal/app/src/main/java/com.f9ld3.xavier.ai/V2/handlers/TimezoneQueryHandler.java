package com.f9ld3.xavier.ai.V2.handlers;

import android.util.Log;

import com.f9ld3.xavier.ai.V2.ConversationContext;
import com.f9ld3.xavier.ai.V2.XavierCoreV2;
import com.f9ld3.xavier.ai.V2.services.IPGeolocationService;
import com.f9ld3.xavier.ai.V2.services.LocationResolverService;
import com.f9ld3.xavier.ai.V2.utils.SharedHttpClient;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.Optional;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * A unified and intelligent handler for all time-related queries.
 * REFACTORED: Now fully integrated with the new context stack and entity system.
 * (Original file name was TimezoneQueryHandler.java)
 */
public class TimezoneQueryHandler implements IntentHandler {

	private static final String TAG = "TimezoneQueryHandler";
	private final LocationResolverService locationResolver;
	private final IPGeolocationService ipGeolocationService;
	private final String apiKey; // For OpenWeatherMap API
	private final OkHttpClient httpClient;

	private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);
	private static final String WEATHER_API_URL_FOR_TIMEZONE = "https://api.openweathermap.org/data/2.5/weather?lat=%s&lon=%s&appid=%s";

	public TimezoneQueryHandler(LocationResolverService locationResolver,
								IPGeolocationService ipGeolocationService,
								String apiKey) {
		if (locationResolver == null) {
			throw new IllegalArgumentException("LocationResolverService cannot be null.");
		}
		if (ipGeolocationService == null) {
			throw new IllegalArgumentException("IPGeolocationService cannot be null.");
		}
		this.locationResolver = locationResolver;
		this.ipGeolocationService = ipGeolocationService;
		this.apiKey = apiKey; // Can be null/blank if not all paths use it
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

		// The core pipeline now uses the generic "subject" key for location-based queries.
		Optional<String> locationOpt = context.getEntityFromCurrentContext("subject")
				.filter(obj -> obj instanceof String) // Ensure it's a String
				.map(String::valueOf);

		// Case 1: A specific location was extracted by the core pipeline.
		if (locationOpt.isPresent() && !locationOpt.get().isBlank()) {
			Log.d(TAG, "Location found in context: " + locationOpt.get());
			return getTimeForLocation(locationOpt.get(), context);
		}

		// Case 2: No location mentioned. Try to get it automatically via IP.
		// This part (ipGeolocationService.getCurrentLocation()) is expected to run on a background thread.
		if (XavierCoreV2.DEBUG_MODE) {
			Log.d(TAG, "No location in time query context. Attempting IP Geolocation.");
		}
		try {
			Optional<JsonObject> geoDataOpt = ipGeolocationService.getCurrentLocation();

			if (geoDataOpt.isPresent()) {
				JsonObject geoData = geoDataOpt.get();
				JsonElement timezoneElement = geoData.get("timezone");
				if (timezoneElement != null && !timezoneElement.isJsonNull()) {
					try {
						String timezoneIdStr = timezoneElement.getAsString();
						ZoneId zoneId = ZoneId.of(timezoneIdStr);

						String cityName = geoData.has("city") && !geoData.get("city").isJsonNull() ? geoData.get("city").getAsString() : "your current area";
						String countryCode = geoData.has("country_code") && !geoData.get("country_code").isJsonNull() ? geoData.get("country_code").getAsString() : "";
						String locationName = cityName + (countryCode.isEmpty() ? "" : ", " + countryCode);

						if (XavierCoreV2.DEBUG_MODE) {
							Log.d(TAG, String.format("IP Geolocation success. Found timezone: %s for %s", timezoneIdStr, locationName));
						}
						return formatTimeResponse(ZonedDateTime.now(zoneId), locationName);
					} catch (DateTimeException | ClassCastException | IllegalStateException e) {
						if (XavierCoreV2.DEBUG_MODE) {
							Log.w(TAG, "IP Geolocation returned invalid timezone or data. Falling back. Error: " + e.getMessage(), e);
						}
					}
				} else {
					if (XavierCoreV2.DEBUG_MODE) Log.d(TAG, "IP Geolocation data missing 'timezone' field.");
				}
			} else {
				if (XavierCoreV2.DEBUG_MODE) Log.d(TAG, "IP Geolocation service returned no data.");
			}
		} catch (Exception e) { // Catch any exception from IPGeolocationService
			if (XavierCoreV2.DEBUG_MODE) {
				Log.e(TAG, "Error during IP Geolocation for time query: " + e.getMessage(), e);
			}
		}

		// Case 3: IP Geolocation failed or no location was found. Fallback to server's system time.
		if (XavierCoreV2.DEBUG_MODE) {
			Log.d(TAG, "IP Geolocation failed or no timezone found. Falling back to system default time.");
		}
		try {
			ZonedDateTime systemTime = ZonedDateTime.now(ZoneId.systemDefault());
			String zoneName = systemTime.getZone().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
			return formatTimeResponse(systemTime, "my current location (" + zoneName + ")");
		} catch (DateTimeException e) {
			Log.e(TAG, "Error getting system default time: " + e.getMessage(), e);
			return "I'm having trouble determining the current time right now.";
		}
	}

	/**
	 * This method involves network calls and MUST be run on a background thread.
	 */
	private String getTimeForLocation(String location, ConversationContext context) {
		if (location == null || location.isBlank()) {
			Log.w(TAG, "getTimeForLocation called with null or blank location.");
			return "I need a location to find the time.";
		}
		// This part (locationResolver.resolve) is expected to run on a background thread.
		try {
			JsonObject geoData = locationResolver.resolve(location);
			if (geoData == null) {
				Log.w(TAG, "LocationResolver returned null for: " + location);
				return "I couldn't find information for the location: " + location;
			}

			JsonElement timezoneElement = geoData.get("timezone");
			ZoneId zoneId;

			if (timezoneElement == null || timezoneElement.isJsonNull()) {
				if (XavierCoreV2.DEBUG_MODE) {
					Log.d(TAG, "Timezone ID missing for '" + location + "'. Fetching offset from coordinates.");
				}
				if (this.apiKey == null || this.apiKey.isBlank()) {
					Log.w(TAG, "OpenWeatherMap API key is missing. Cannot fetch timezone by coordinates for " + location);
					return "I can find '" + location + "', but I need a configured API key to determine its timezone from coordinates. My apologies.";
				}

				if (!geoData.has("lat") || !geoData.has("lon") ||
						geoData.get("lat").isJsonNull() || geoData.get("lon").isJsonNull()) {
					Log.w(TAG, "Latitude or longitude missing in geodata for " + location);
					return "I found '" + location + "', but I'm missing its coordinates to determine the timezone.";
				}
				String lat = geoData.get("lat").getAsString();
				String lon = geoData.get("lon").getAsString();

				// This part (fetchTimezoneOffsetFromCoords) involves a network call.
				Optional<Integer> offsetSecondsOpt = fetchTimezoneOffsetFromCoords(lat, lon, this.apiKey);
				if (offsetSecondsOpt.isPresent()) {
					zoneId = ZoneOffset.ofTotalSeconds(offsetSecondsOpt.get());
				} else {
					return "I found '" + location + "', but I'm having trouble determining its timezone from its coordinates. My apologies.";
				}
			} else {
				String timezoneIdStr = timezoneElement.getAsString();
				zoneId = ZoneId.of(timezoneIdStr);
			}

			String name = geoData.has("name") && !geoData.get("name").isJsonNull() ? geoData.get("name").getAsString() : location;
			String country = geoData.has("country") && !geoData.get("country").isJsonNull() ? geoData.get("country").getAsString() : "";
			String displayName = name + (country.isEmpty() || name.equalsIgnoreCase(country) || name.contains(country) ? "" : ", " + country);

			return formatTimeResponse(ZonedDateTime.now(zoneId), displayName);

		} catch (DateTimeException | JsonParseException | IllegalStateException | ClassCastException e) {
			Log.e(TAG, "Error processing location or time data for '" + location + "': " + e.getMessage(), e);
			return "I'm sorry, I ran into an issue trying to find the time for '" + location + "'. Please try again.";
		} catch (Exception e) { // Catch-all for other service errors e.g. from locationResolver
			Log.e(TAG, "General error in getTimeForLocation for '" + location + "': " + e.getMessage(), e);
			return "I'm sorry, an unexpected error occurred while finding the time for '" + location + "'.";
		}
	}

	/**
	 * This method performs a network call and MUST be run on a background thread.
	 */
	private Optional<Integer> fetchTimezoneOffsetFromCoords(String lat, String lon, String apiKey) {
		if (apiKey == null || apiKey.isBlank()) {
			Log.w(TAG, "fetchTimezoneOffsetFromCoords called without an API key.");
			return Optional.empty();
		}
		String url = String.format(WEATHER_API_URL_FOR_TIMEZONE, lat, lon, apiKey);
		Log.d(TAG, "Fetching timezone offset from URL: " + url);

		Request okHttpRequest = new Request.Builder()
				.url(url)
				.get()
				.build();

		try (Response okHttpResponse = httpClient.newCall(okHttpRequest).execute()) {

			if (!okHttpResponse.isSuccessful()) {
				Log.w(TAG, "Timezone offset fetch failed with status code: " + okHttpResponse.code() + " for URL: " + url);
				return Optional.empty();
			}

			ResponseBody responseBody = okHttpResponse.body();
			if (responseBody == null) {
				Log.w(TAG, "Timezone offset response body was null for URL: " + url);
				return Optional.empty();
			}

			String responseBodyString = responseBody.string();
			JsonObject weatherData = JsonParser.parseString(responseBodyString).getAsJsonObject();

			// OpenWeatherMap returns 'timezone' as offset in seconds from UTC
			if (weatherData.has("timezone")) {
				JsonElement tzElement = weatherData.get("timezone");
				if (tzElement != null && !tzElement.isJsonNull()) {
					Log.d(TAG, "Successfully fetched timezone offset: " + tzElement.getAsInt());
					return Optional.of(tzElement.getAsInt());
				} else {
					Log.w(TAG, "'timezone' field is null in weather data from " + url);
				}
			} else {
				Log.w(TAG, "'timezone' field missing in weather data from " + url + ". Response: " + responseBodyString.substring(0, Math.min(responseBodyString.length(), 200)));
			}
		} catch (IOException e) {
			Log.e(TAG, "IOException fetching timezone offset from coordinates for URL: " + url, e);
		} catch (JsonParseException | IllegalStateException | ClassCastException e) {
			Log.e(TAG, "Error parsing JSON for timezone offset from URL: " + url, e);
		} catch (Exception e) {
			Log.e(TAG, "Unexpected error fetching timezone offset from URL: " + url, e);
		}
		return Optional.empty();
	}

	/**
	 * Formats the final time response in a clear, human-readable way.
	 */
	private String formatTimeResponse(ZonedDateTime zonedDateTime, String locationName) {
		if (zonedDateTime == null || locationName == null) {
			Log.w(TAG, "formatTimeResponse called with null zonedDateTime or locationName.");
			return "I found the time, but there was an issue displaying it.";
		}
		try {
			String time = zonedDateTime.format(TIME_FORMATTER);
			String day = zonedDateTime.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
			String zoneAbbreviation = zonedDateTime.getZone().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
			return String.format("In %s, the current time is %s (%s) on %s.", locationName, time, zoneAbbreviation, day);
		} catch (DateTimeException e) {
			Log.e(TAG, "Error formatting ZonedDateTime: " + zonedDateTime + " for location: " + locationName, e);
			return String.format("The current time in %s is %s.", locationName, zonedDateTime.toLocalTime().format(TIME_FORMATTER));
		}
	}
}
