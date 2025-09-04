// C:/Users/Music_Minister/Desktop/PlayStore/PlayStoreProjects/Xavier/XavierAI/src/main/java/com/f9ld3/xavier/ai/V2/services/SearchService.java
package com.f9ld3.xavier.ai.V2.services;

import com.f9ld3.xavier.ai.V2.utils.SharedHttpClient;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A resilient service for performing web searches using the Serper API.
 * It cycles through a list of API keys to provide fault tolerance.
 */
public class SearchService {

/**
 * A custom exception for when the search service fails.
 * This allows calling handlers to catch and manage errors gracefully.
 */
public static class SearchServiceException extends RuntimeException {
	public SearchServiceException(String message) {
		super(message);
	}
}

/**
 * A record to hold structured search results.
 */
public record SearchResult(String title, String link, String snippet) {}

private static final String API_URL = "https://google.serper.dev/search";
private final List<String> apiKeys;
private final Gson gson = new Gson();
private int currentKeyIndex = 0;

public SearchService(List<String> apiKeys) {
	if (apiKeys == null || apiKeys.isEmpty()) {
		this.apiKeys = new ArrayList<>();
		System.err.println("WARN: No Serper API keys provided. Search functionality will be disabled.");
	} else {
		this.apiKeys = apiKeys;
	}
}

/**
 * Retrieves search results for a given query.
 *
 * @param query The search query.
 * @return An Optional containing a list of search results.
 * @throws SearchServiceException if all API keys fail or return no results.
 */
public Optional<List<SearchResult>> getSearchResults(String query) throws SearchServiceException {
	if (apiKeys.isEmpty()) {
		throw new SearchServiceException("Search service is disabled; no API keys are configured.");
	}
	
	String lastError = "No attempts made.";
	for (int i = 0; i < apiKeys.size(); i++) {
		try {
			String apiKey = getNextApiKey();
			HttpRequest request = HttpRequest.newBuilder()
					                      .uri(URI.create(API_URL))
					                      .header("X-API-KEY", apiKey)
					                      .header("Content-Type", "application/json")
					                      .POST(HttpRequest.BodyPublishers.ofString("{\"q\":\"" + URLEncoder.encode(query, StandardCharsets.UTF_8) + "\"}"))
					                      .build();
			
			HttpResponse<String> response = SharedHttpClient.get().send(request, HttpResponse.BodyHandlers.ofString());
			
			if (response.statusCode() == 200) {
				JsonObject jsonResponse = gson.fromJson(response.body(), JsonObject.class);
				JsonArray organicResults = jsonResponse.getAsJsonArray("organic");
				if (organicResults != null && !organicResults.isEmpty()) {
					List<SearchResult> results = new ArrayList<>();
					for (int j = 0; j < organicResults.size(); j++) {
						JsonObject result = organicResults.get(j).getAsJsonObject();
						results.add(new SearchResult(
								result.get("title").getAsString(),
								result.get("link").getAsString(),
								result.has("snippet") ? result.get("snippet").getAsString() : ""
						));
					}
					return Optional.of(results);
				}
			}
			lastError = "API key returned status " + response.statusCode() + " with no valid results.";
			// If we get here, the key worked but returned no results or a non-200 code. Try the next key.
		} catch (Exception e) {
			lastError = e.getMessage();
			// This key failed, log it and try the next one.
			System.err.println("WARN: Search API key ending in " + apiKeys.get(currentKeyIndex > 0 ? currentKeyIndex - 1 : apiKeys.size() - 1).substring(Math.max(0, apiKeys.get(currentKeyIndex > 0 ? currentKeyIndex - 1 : apiKeys.size() - 1).length() - 4)) + " failed. Reason: " + lastError);
		}
	}
	
	// If the loop completes without returning, all keys have failed.
	throw new SearchServiceException("All Search API keys failed. Last error: " + lastError);
}

private synchronized String getNextApiKey() {
	String key = apiKeys.get(currentKeyIndex);
	currentKeyIndex = (currentKeyIndex + 1) % apiKeys.size();
	return key;
}
}