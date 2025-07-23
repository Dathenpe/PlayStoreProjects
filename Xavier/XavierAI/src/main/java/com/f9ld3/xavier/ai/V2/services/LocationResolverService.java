package com.f9ld3.xavier.ai.V2.services;

import com.f9ld3.xavier.ai.V2.FuzzyMatcher;
import com.f9ld3.xavier.ai.V2.utils.LocationCache;
import com.google.gson.JsonObject;
import java.util.Optional;

/**
 * A high-level service that resolves a location string into geographic data.
 * It encapsulates the "cache-then-API" fallback logic, providing a single,
 * reliable source for location resolution throughout the application.
 */
public class LocationResolverService {

private final GeocodingService geocodingService;

public LocationResolverService(GeocodingService geocodingService) {
	this.geocodingService = geocodingService;
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
	
	// 2. If no exact match, check for a fuzzy match against the cache keys.
	// We wrap the potentially null result in an Optional to handle it safely.
	Optional<String> bestMatch = Optional.ofNullable(FuzzyMatcher.getBestMatch(location, LocationCache.getAllKeys()));
	if (bestMatch.isPresent()) {
		System.out.printf("[DEBUG] LocationResolver: Fuzzy cache hit for '%s' -> '%s'.%n", location, bestMatch.get());
		// .get() is safe here because we've already checked isPresent()
		return LocationCache.get(bestMatch.get()).get();
	}
	
	// 3. If not in cache by any means, use the GeocodingService API as a fallback.
	System.out.printf("[DEBUG] LocationResolver: Cache miss for '%s'. Calling API.%n", location);
	return geocodingService.getCoordinates(location);
}
}