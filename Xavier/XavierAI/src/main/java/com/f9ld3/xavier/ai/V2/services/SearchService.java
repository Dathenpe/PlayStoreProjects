package com.f9ld3.xavier.ai.V2.services;

import com.f9ld3.xavier.ai.V2.utils.NetworkStatusChecker;
import com.f9ld3.xavier.ai.V2.utils.SharedHttpClient;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A resilient service to fetch instructional snippets from a web search API (Serper.dev).
 * This is used to answer "how-to" style questions and supports fallback API keys.
 */
public class SearchService {

private static final String API_URL = "https://google.serper.dev/search";
private final List<String> apiKeys;
private final Gson gson = new Gson();

/**
 * A record to hold structured data for a single search result.
 */
public record SearchResult(String title, String link, String snippet) {}

public SearchService(List<String> apiKeys) {
	this.apiKeys = apiKeys;
}

/**
 * Searches for a query and returns a list of structured search results.
 * It will automatically try the next API key if the current one fails.
 *
 * @param query The user's search query (e.g., "bake a chocolate cake").
 * @return An Optional containing a list of SearchResult objects, or empty if none are available.
 */
public Optional<List<SearchResult>> getSearchResults(String query) {
	if (!NetworkStatusChecker.isOnline()) {
		System.err.println("SearchService: Network is offline. Aborting request.");
		return Optional.empty();
	}
	if (apiKeys == null || apiKeys.isEmpty()) {
		System.err.println("ERROR: SearchService cannot function without an API key.");
		return Optional.empty();
	}
	
	Exception lastException = null;
	
	for (String apiKey : apiKeys) {
		try {
			String requestBody = gson.toJson(new SearchRequest(query));
			
			HttpRequest request = HttpRequest.newBuilder()
					                      .uri(URI.create(API_URL))
					                      .header("X-API-KEY", apiKey)
					                      .header("Content-Type", "application/json")
					                      .POST(HttpRequest.BodyPublishers.ofString(requestBody))
					                      .build();
			
			HttpResponse<String> response = SharedHttpClient.get().send(request, HttpResponse.BodyHandlers.ofString());
			
			if (response.statusCode() == 200) {
				JsonObject resultJson = gson.fromJson(response.body(), JsonObject.class);
				JsonArray organicResults = resultJson.getAsJsonArray("organic");
				
				if (organicResults != null && organicResults.size() > 0) {
					List<SearchResult> results = new ArrayList<>();
					for (var resultEl : organicResults) {
						JsonObject resultObj = resultEl.getAsJsonObject();
						if (resultObj.has("snippet") && resultObj.has("title") && resultObj.has("link")) {
							results.add(new SearchResult(
									resultObj.get("title").getAsString(),
									resultObj.get("link").getAsString(),
									resultObj.get("snippet").getAsString()
							));
						}
					}
					if (!results.isEmpty()) {
						return Optional.of(results);
					}
				}
				return Optional.empty(); // Successful request but no results
			}
			
			if (response.statusCode() == 401 || response.statusCode() == 402 || response.statusCode() == 403 || response.statusCode() == 429) {
				System.err.printf("[WARN] Search API key ending in '...%s' failed with status %d. Trying next key.%n",
						apiKey.substring(Math.max(0, apiKey.length() - 4)), response.statusCode());
				lastException = new RuntimeException("API key failed with status " + response.statusCode());
				continue;
			}
			
			throw new RuntimeException("Search API request failed with status: " + response.statusCode());
			
		} catch (Exception e) {
			lastException = e;
		}
	}
	
	
	System.err.println("ERROR: All Search API keys failed or returned no results. Last error: " +
			                   (lastException != null ? lastException.getMessage() : "No exception, API may have returned empty results."));
	return Optional.empty();
}

private record SearchRequest(String q) {}
}