package com.f9ld3.xavier.ai.V2.services;

import com.f9ld3.xavier.ai.V2.utils.SharedHttpClient;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/**
 * A resilient, multi-source service to fetch random, interesting facts.
 * It automatically falls back to a backup API if the primary one fails.
 */
public class FunFactService {

// Primary API for facts.
private static final String PRIMARY_FACT_API_URL = "https://uselessfacts.jsph.pl/api/v2/facts/random?language=en";
// Backup API for facts.
private static final String BACKUP_FACT_API_URL = "https://api.aakhilv.me/fun/facts";

private final Gson gson = new Gson();

public FunFactService() {
	// This service is now self-contained and requires no external configuration.
}

/**
 * Fetches a single random fun fact, trying the primary API first and then the backup.
 *
 * @return An Optional containing the fact, or empty if all sources fail.
 */
public Optional<String> getFact() {
	// The .or() method is a clean, functional way to implement the fallback chain.
	return fetchFromPrimaryFactApi().or(this::fetchFromBackupFactApi);
}

/**
 * Attempts to fetch and parse a fact from the primary source (uselessfacts.jsph.pl).
 */
private Optional<String> fetchFromPrimaryFactApi() {
	try {
		HttpRequest request = HttpRequest.newBuilder()
				                      .uri(URI.create(PRIMARY_FACT_API_URL))
				                      .header("Accept", "application/json")
				                      .build();
		
		HttpResponse<String> response = SharedHttpClient.get().send(request, HttpResponse.BodyHandlers.ofString());
		
		if (response.statusCode() != 200) {
			throw new RuntimeException("Received status code " + response.statusCode());
		}
		
		JsonObject factJson = gson.fromJson(response.body(), JsonObject.class);
		if (factJson != null && factJson.has("text")) {
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
private Optional<String> fetchFromBackupFactApi() {
	try {
		System.out.println("[DEBUG] Primary fact API failed. Trying backup...");
		HttpRequest request = HttpRequest.newBuilder()
				                      .uri(URI.create(BACKUP_FACT_API_URL))
				                      .header("Accept", "application/json")
				                      .build();
		
		HttpResponse<String> response = SharedHttpClient.get().send(request, HttpResponse.BodyHandlers.ofString());
		
		if (response.statusCode() != 200) {
			throw new RuntimeException("Received status code " + response.statusCode());
		}
		
		// FIX: Handle cases where the API returns a single string OR an array.
		JsonElement jsonElement = JsonParser.parseString(response.body());
		if (jsonElement.isJsonArray()) {
			JsonArray factArray = jsonElement.getAsJsonArray();
			if (factArray.size() > 0) {
				int randomIndex = ThreadLocalRandom.current().nextInt(factArray.size());
				return Optional.of(factArray.get(randomIndex).getAsString());
			}
		} else if (jsonElement.isJsonPrimitive()) {
			// If it's just a single string, return that.
			return Optional.of(jsonElement.getAsString());
		}
		
		return Optional.empty();
		
	} catch (Exception e) {
		System.err.println("ERROR: Backup FunFact API also failed: " + e.getMessage());
		return Optional.empty(); // All sources have failed.
	}
}
}