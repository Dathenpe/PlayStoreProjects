package com.f9ld3.xavier.ai.V2.services;

import com.f9ld3.xavier.ai.V2.FuzzyMatcher;
import com.f9ld3.xavier.ai.V2.FuzzyMatcher.MatchResult;
import com.f9ld3.xavier.ai.V2.utils.LocationCache;
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

private final GeocodingService geocodingService;
private final FuzzyMatcher locationFuzzyMatcher; // A dedicated matcher for locations

public LocationResolverService(GeocodingService geocodingService) {
	this.geocodingService = geocodingService;
	
	// Initialize and train a dedicated fuzzy matcher just for the location cache.
	this.locationFuzzyMatcher = new FuzzyMatcher();
	List<String> locationKeys = new ArrayList<>(LocationCache.getAllKeys());
	// The intents list is required for training, but we don't use it here.
	List<String> dummyIntents = new ArrayList<>();
	locationKeys.forEach(key -> dummyIntents.add("location"));
	this.locationFuzzyMatcher.train(locationKeys, dummyIntents);
}

/**
 * Resolves a location name into geocoding data, using the cache first
 * and falling back to the Geocoding API if necessary.
 *
 * @param location The location name (e.g., "paris", "ontario").
 * @return A JsonObject with the location's geographic data.
 * @throws Exception if the location cannot be found by any means.
 */
public JsonObject resolve(String location) throws Exception {
	// 1. Check the hardcoded location cache for an exact match. This is the fastest path.
	Optional<JsonObject> cachedGeoData = LocationCache.get(location);
	if (cachedGeoData.isPresent()) {
		System.out.printf("[DEBUG] LocationResolver: Exact cache hit for '%s'.%n", location);
		return cachedGeoData.get();
	}
	
	// 2. If no exact match, use our dedicated fuzzy matcher against the cache keys.
	// We use a slightly lower threshold here to be more lenient with location names.
	Optional<MatchResult> bestMatchResult = locationFuzzyMatcher.findBestMatch(location, 0.70);
	if (bestMatchResult.isPresent()) {
		String bestMatch = bestMatchResult.get().matchedPhrase();
		System.out.printf("[DEBUG] LocationResolver: Fuzzy cache hit for '%s' -> '%s'.%n", location, bestMatch);
		// .get() is safe here because we know the key exists from the match.
		return LocationCache.get(bestMatch).get();
	}
	
	// 3. If not in cache by any means, use the GeocodingService API as a fallback.
	System.out.printf("[DEBUG] LocationResolver: Cache miss for '%s'. Calling API.%n", location);
	return geocodingService.getCoordinates(location);
}
}