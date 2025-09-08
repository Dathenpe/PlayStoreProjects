package com.f9ld3.xavier.ai.V2.services;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * A resilient service for performing web searches using the Serper API.
 * It cycles through a list of API keys to provide fault tolerance.
 */
public class SearchService {

	private static final String TAG = "SearchService";
	private static final String API_URL = "https://google.serper.dev/search";
	private static final long API_TIMEOUT_SECONDS = 5;
	public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

	/**
	 * A custom exception for when the search service fails.
	 * This allows calling handlers to catch and manage errors gracefully.
	 */
	public static class SearchServiceException extends RuntimeException {
		public SearchServiceException(String message) {
			super(message);
		}
		public SearchServiceException(String message, Throwable cause) {
			super(message, cause);
		}
	}

	/**
	 * A class to hold structured search results. (Converted from record)
	 */
	public static final class SearchResult { // Made static as it doesn't need outer instance
		private final String title;
		private final String link;
		private final String snippet;

		public SearchResult(String title, String link, String snippet) {
			this.title = title;
			this.link = link;
			this.snippet = snippet;
		}

		public String getTitle() {
			return title;
		}

		public String getLink() {
			return link;
		}

		public String getSnippet() {
			return snippet;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			SearchResult that = (SearchResult) o;
			return Objects.equals(title, that.title) &&
					Objects.equals(link, that.link) &&
					Objects.equals(snippet, that.snippet);
		}

		@Override
		public int hashCode() {
			return Objects.hash(title, link, snippet);
		}

		@Override
		public String toString() {
			return "SearchResult{" +
					"title='" + title + '\'' +
					", link='" + link + '\'' +
					", snippet='" + snippet + '\'' +
					'}';
		}
	}

	private final List<String> apiKeys;
	private final Gson gson = new Gson();
	private final OkHttpClient httpClient;
	private int currentKeyIndex = 0;

	public SearchService(List<String> apiKeys) {
		this(apiKeys, new OkHttpClient.Builder() // Default OkHttpClient
				.connectTimeout(API_TIMEOUT_SECONDS, TimeUnit.SECONDS)
				.readTimeout(API_TIMEOUT_SECONDS, TimeUnit.SECONDS)
				.writeTimeout(API_TIMEOUT_SECONDS, TimeUnit.SECONDS)
				.build());
	}

	// Constructor allowing OkHttpClient injection
	public SearchService(List<String> apiKeys, OkHttpClient client) {
		this.httpClient = client;
		if (apiKeys == null || apiKeys.isEmpty()) {
			this.apiKeys = new ArrayList<>(); // Ensure not null
			Log.w(TAG, "WARN: No Serper API keys provided. Search functionality will be disabled.");
		} else {
			this.apiKeys = apiKeys;
		}
	}

	/**
	 * Retrieves search results for a given query.
	 * This method performs network operations and MUST be called from a background thread.
	 *
	 * @param query The search query.
	 * @return An Optional containing a list of search results.
	 * @throws SearchServiceException if all API keys fail or return no results.
	 */
	public Optional<List<SearchResult>> getSearchResults(String query) throws SearchServiceException {
		if (this.apiKeys.isEmpty()) { // Check the initialized this.apiKeys
			throw new SearchServiceException("Search service is disabled; no API keys are configured.");
		}
		if (query == null || query.isBlank()) {
			Log.w(TAG, "getSearchResults called with empty or null query.");
			return Optional.empty(); // Or throw SearchServiceException depending on desired behavior
		}

		String encodedQuery;
		try {
			encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8.name());
		} catch (IOException e) { // Covers UnsupportedEncodingException
			Log.e(TAG, "Failed to URL encode query: " + query, e);
			throw new SearchServiceException("Failed to encode search query.", e);
		}

		String lastError = "No attempts made with available API keys.";
		String requestJsonBody = "{\"q\":\"" + encodedQuery + "\"}"; // Serper uses q, not query

		// Loop through the number of available keys as attempts
		for (int i = 0; i < apiKeys.size(); i++) {
			String apiKey = getNextApiKey(); // This cycles through the keys
			Log.d(TAG, "Attempting search for '" + query + "' with key ending in: ..." + (apiKey.length() > 4 ? apiKey.substring(apiKey.length() - 4) : apiKey));

			RequestBody body = RequestBody.create(requestJsonBody, JSON);
			Request request = new Request.Builder()
					.url(API_URL)
					.header("X-API-KEY", apiKey)
					// .header("Content-Type", "application/json") // Handled by RequestBody.create with MediaType
					.post(body)
					.build();

			// THIS IS A SYNCHRONOUS (BLOCKING) NETWORK CALL.
			try (Response response = httpClient.newCall(request).execute()) {
				ResponseBody responseBody = response.body();
				String responseBodyString = (responseBody != null) ? responseBody.string() : null; // Consume once

				if (response.isSuccessful() && responseBodyString != null) {
					Log.d(TAG, "Successful search response. Body: " + responseBodyString);
					JsonObject jsonResponse = gson.fromJson(responseBodyString, JsonObject.class);
					JsonArray organicResults = jsonResponse.getAsJsonArray("organic");

					if (organicResults != null && !organicResults.isEmpty()) {
						List<SearchResult> results = new ArrayList<>();
						for (int j = 0; j < organicResults.size(); j++) {
							JsonObject result = organicResults.get(j).getAsJsonObject();
							// Add null checks for robustness if API might omit fields
							String title = result.has("title") ? result.get("title").getAsString() : "N/A";
							String link = result.has("link") ? result.get("link").getAsString() : "#";
							String snippet = result.has("snippet") ? result.get("snippet").getAsString() : "";

							results.add(new SearchResult(title, link, snippet));
						}
						return Optional.of(results);
					} else {
						// This might be a valid 200 but no organic results
						lastError = "API key returned 200 but no 'organic' results found. Body: " + responseBodyString;
						Log.w(TAG, lastError);
					}
				} else {
					lastError = "API key returned status " + response.code() + ". Body: " + (responseBodyString != null ? responseBodyString : "N/A");
					Log.w(TAG, "Search API request failed: " + lastError);
				}
			} catch (JsonSyntaxException e) {
				lastError = "JSON parsing error: " + e.getMessage();
				Log.e(TAG, "Search API JSON parsing error for key ending in ..." + (apiKey.length() > 4 ? apiKey.substring(apiKey.length() - 4) : apiKey) + ": " + lastError, e);
			} catch (IOException e) {
				lastError = "Network error: " + e.getClass().getSimpleName() + ": " + e.getMessage();
				Log.e(TAG, "Search API network error for key ending in ..." + (apiKey.length() > 4 ? apiKey.substring(apiKey.length() - 4) : apiKey) + ": " + lastError, e);
			} catch (Exception e) { // Catch any other unexpected errors
				lastError = "Unexpected error: " + e.getClass().getSimpleName() + ": " + e.getMessage();
				Log.e(TAG, "Search API unexpected error for key ending in ..." + (apiKey.length() > 4 ? apiKey.substring(apiKey.length() - 4) : apiKey) + ": " + lastError, e);
			}
		} // End of for loop (API key retry)

		throw new SearchServiceException("All Search API keys failed for query '" + query + "'. Last error: " + lastError);
	}

	private synchronized String getNextApiKey() {
		if (apiKeys.isEmpty()) { // Should be caught by the check in getSearchResults(), but defensive
			throw new IllegalStateException("No API keys available to cycle.");
		}
		String key = apiKeys.get(currentKeyIndex);
		currentKeyIndex = (currentKeyIndex + 1) % apiKeys.size();
		return key;
	}
}
