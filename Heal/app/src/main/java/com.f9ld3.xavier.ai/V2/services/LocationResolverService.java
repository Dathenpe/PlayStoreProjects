package com.f9ld3.xavier.ai.V2.services;

import android.util.Log; // Using Android's Log

import com.f9ld3.xavier.ai.V2.FuzzyMatcher;
import com.f9ld3.xavier.ai.V2.FuzzyMatcher.MatchResult; // Assuming MatchResult is a public static class in FuzzyMatcher or a top-level class
import com.f9ld3.xavier.ai.V2.utils.LocationCache; // Assuming this is your custom cache
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A high-level service that resolves a location string into geographic data.
 * It encapsulates the "cache-then-API" fallback logic, providing a single,
 * reliable source for location resolution throughout the application.
 */
public class LocationResolverService {

	private static final String TAG = "LocationResolverSvc";

	private final GeocodingService geocodingService;
	private final FuzzyMatcher locationFuzzyMatcher;

	/**
	 * Custom exception for when location resolution ultimately fails.
	 */
	public static class LocationResolverException extends Exception {
		public LocationResolverException(String message) {
			super(message);
		}
		public LocationResolverException(String message, Throwable cause) {
			super(message, cause);
		}
	}

	public LocationResolverService(GeocodingService geocodingService) {
		if (geocodingService == null) {
			throw new IllegalArgumentException("GeocodingService cannot be null.");
		}
		this.geocodingService = geocodingService;

		// Initialize and train a dedicated fuzzy matcher just for the location cache.
		this.locationFuzzyMatcher = new FuzzyMatcher();
		try {
			// Assuming LocationCache.getAllKeys() and FuzzyMatcher.train() are robust
			List<String> locationKeys = new ArrayList<>(LocationCache.getAllKeys());
			if (!locationKeys.isEmpty()) {
				// The intents list is required for training, but we don't use it here for intent matching.
				List<String> dummyIntents = new ArrayList<>();
				locationKeys.forEach(key -> dummyIntents.add("location_cache_item")); // Use a specific dummy intent
				this.locationFuzzyMatcher.train(locationKeys, dummyIntents);
				Log.i(TAG, "LocationFuzzyMatcher trained with " + locationKeys.size() + " cache keys.");
			} else {
				Log.w(TAG, "LocationCache provided no keys for FuzzyMatcher training.");
			}
		} catch (Exception e) {
			// Log the error but allow the service to continue; fuzzy matching on cache will be disabled.
			Log.e(TAG, "Error initializing or training LocationFuzzyMatcher for cache. Fuzzy cache lookups may not work.", e);
		}
	}

	/**
	 * Resolves a location name into geocoding data, using the cache first
	 * and falling back to the Geocoding API if necessary.
	 * This method may perform network operations via GeocodingService and
	 * MUST be called from a background thread.
	 *
	 * @param location The location name (e.g., "paris", "ontario").
	 * @return A JsonObject with the location's geographic data.
	 * @throws LocationResolverException if the location cannot be found by any means or an error occurs.
	 */
	public JsonObject resolve(String location) throws LocationResolverException {
		if (location == null || location.isBlank()) {
			throw new LocationResolverException("Location name cannot be null or blank.");
		}
		String lowerLocation = location.toLowerCase(); // Normalize for cache lookups

		// 1. Check the hardcoded location cache for an exact match. This is the fastest path.
		Optional<JsonObject> cachedGeoData = LocationCache.get(lowerLocation); // Use lowerLocation for cache
		if (cachedGeoData.isPresent()) {
			Log.d(TAG, String.format("LocationResolver: Exact cache hit for '%s'.", location));
			return cachedGeoData.get();
		}

		// 2. If no exact match, use our dedicated fuzzy matcher against the cache keys.
		// We use a slightly lower threshold here to be more lenient with location names.
		if (this.locationFuzzyMatcher != null) { // Check if matcher was initialized
			// Pass the original location for matching, as FuzzyMatcher internally handles case if needed
			Optional<MatchResult> bestMatchResult = locationFuzzyMatcher.findBestMatch(location, 0.70);
			if (bestMatchResult.isPresent()) {
				String bestMatchKey = bestMatchResult.get().getMatchedPhrase(); // Assuming MatchResult has getMatchedPhrase()
				// Fetch from cache using the matched key (which should be in its original casing as stored)
				Optional<JsonObject> fuzzyCachedData = LocationCache.get(bestMatchKey);
				if (fuzzyCachedData.isPresent()) {
					Log.d(TAG, String.format("LocationResolver: Fuzzy cache hit for '%s' -> '%s'.", location, bestMatchKey));
					return fuzzyCachedData.get();
				} else {
					// This case is unlikely if fuzzy matcher is trained on actual keys from LocationCache,
					// but good to log if it happens.
					Log.w(TAG, String.format("LocationResolver: Fuzzy match found key '%s' but no data in cache for it.", bestMatchKey));
				}
			}
		} else {
			Log.w(TAG, "LocationFuzzyMatcher is not available for cache lookup.");
		}

		// 3. If not in cache by any means, use the GeocodingService API as a fallback.
		Log.d(TAG, String.format("LocationResolver: Cache miss for '%s'. Calling Geocoding API.", location));
		try {
			return geocodingService.getCoordinates(location);
		} catch (GeocodingService.GeocodingException e) {
			Log.e(TAG, "GeocodingService failed to resolve location '" + location + "': " + e.getMessage(), e);
			// Re-throw as a LocationResolverException to abstract the underlying service error.
			throw new LocationResolverException("Failed to resolve location '" + location + "' using Geocoding API. " + e.getMessage(), e);
		} catch (Exception e) { // Catch any other unexpected exceptions from geocodingService
			Log.e(TAG, "Unexpected error from GeocodingService for location '" + location + "': " + e.getMessage(), e);
			throw new LocationResolverException("An unexpected error occurred while resolving location '" + location + "'.", e);
		}
	}
}
