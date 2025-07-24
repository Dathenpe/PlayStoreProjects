package com.f9ld3.xavier.ai.V2.services;

import com.f9ld3.xavier.ai.V2.utils.SharedHttpClient;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;

/**
 * A resilient, multi-source service to fetch random, interesting facts.
 * It automatically falls back to a backup API if the primary one fails.
 */
public class FunFactService {

// Primary API: Provides structured data.
private static final String PRIMARY_API_URL = "https://uselessfacts.jsph.pl/random.json?language=en";
// Backup API: Simple and lightweight.
private static final String BACKUP_API_URL = "https://api.aakhilv.me/fun/facts";

private final Gson gson = new Gson();

/**
 * Fetches a single random fun fact, trying the primary API first and then the backup.
 * This fallback mechanism makes the service highly resilient.
 *
 * @return An Optional containing the fact, or empty if all sources fail.
 */
public Optional<String> getFact() {
	// The .or() method is a clean, functional way to implement fallback logic.
	return fetchFromPrimaryApi().or(this::fetchFromBackupApi);
}

/**
 * Attempts to fetch and parse a fact from the primary source (uselessfacts.jsph.pl).
 */
private Optional<String> fetchFromPrimaryApi() {
	try {
		HttpRequest request = HttpRequest.newBuilder()
				                      .uri(URI.create(PRIMARY_API_URL))
				                      .header("Accept", "application/json")
				                      .build();
		
		HttpResponse<String> response = SharedHttpClient.get().send(request, HttpResponse.BodyHandlers.ofString());
		
		if (response.statusCode() != 200) {
			throw new RuntimeException("Received status code " + response.statusCode());
		}
		
		JsonObject factJson = gson.fromJson(response.body(), JsonObject.class);
		if (factJson.has("text")) {
			return Optional.of(factJson.get("text").getAsString());
		}
		return Optional.empty();
		
	} catch (Exception e) {
		System.err.println("WARN: Primary FunFact API failed: " + e.getMessage());
		return Optional.empty(); // Return empty on any failure, allowing fallback.
	}
}

/**
 * Attempts to fetch and parse a fact from the backup source (api.aakhilv.me).
 */
private Optional<String> fetchFromBackupApi() {
	try {
		System.out.println("[DEBUG] Primary fact API failed. Trying backup...");
		HttpRequest request = HttpRequest.newBuilder()
				                      .uri(URI.create(BACKUP_API_URL))
				                      .header("Accept", "application/json")
				                      .build();
		
		HttpResponse<String> response = SharedHttpClient.get().send(request, HttpResponse.BodyHandlers.ofString());
		
		if (response.statusCode() != 200) {
			throw new RuntimeException("Received status code " + response.statusCode());
		}
		
		// This API returns a simple array of strings.
		JsonArray factArray = gson.fromJson(response.body(), JsonArray.class);
		if (factArray != null && !factArray.isJsonNull() && factArray.size() > 0) {
			return Optional.of(factArray.get(0).getAsString());
		}
		return Optional.empty();
		
	} catch (Exception e) {
		System.err.println("ERROR: Backup FunFact API also failed: " + e.getMessage());
		return Optional.empty(); // All sources have failed.
	}
}
}