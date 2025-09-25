package com.f9ld3.xavier.ai.V2.services;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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

	public static class SearchServiceException extends RuntimeException {
		public SearchServiceException(String message) {
			super(message);
		}
		public SearchServiceException(String message, Throwable cause) {
			super(message, cause);
		}
	}

	// --- RESULT CLASSES TO HANDLE RICH SNIPPETS (Matching Serper API JSON) ---

	/**
	 * Represents the data structure for a standard organic search result or link.
	 */
	public static final class Snippet {
		public final String title;
		public final String link;
		public final String snippet;

		public Snippet(String title, String link, String snippet) {
			this.title = title;
			this.link = link;
			this.snippet = snippet;
		}
	}

	/**
	 * Represents the data structure for the AnswerBox.
	 */
	public static final class AnswerBox {
		public final String snippet;
		public final String title;
		public final String link;

		// FIX: Added constructor to initialize final fields
		public AnswerBox(String snippet, String title, String link) {
			this.snippet = snippet;
			this.title = title;
			this.link = link;
		}
	}

	/**
	 * Represents the data structure for the KnowledgeGraph.
	 */
	public static final class KnowledgeGraph {
		public final String title;
		public final String type;
		public final String description;
		public final String descriptionSource;
		public final String descriptionLink;
		public final Map<String, String> attributes;

		// FIX: Added constructor to initialize final fields
		public KnowledgeGraph(String title, String type, String description, String descriptionSource, String descriptionLink, Map<String, String> attributes) {
			this.title = title;
			this.type = type;
			this.description = description;
			this.descriptionSource = descriptionSource;
			this.descriptionLink = descriptionLink;
			this.attributes = attributes;
		}
	}

	/**
	 * The comprehensive class holding the full structured API response.
	 */
	public static final class FullSearchResult {
		public final AnswerBox answerBox;
		public final KnowledgeGraph knowledgeGraph;
		public final List<Snippet> organic; // List of Snippet

		// FIX: Added constructor to initialize final fields
		public FullSearchResult(AnswerBox answerBox, KnowledgeGraph knowledgeGraph, List<Snippet> organic) {
			this.answerBox = answerBox;
			this.knowledgeGraph = knowledgeGraph;
			this.organic = organic;
		}
	}

	// --- END NEW RESULT CLASSES ---

	private final List<String> apiKeys;
	private final Gson gson = new Gson();
	private final OkHttpClient httpClient;
	private int currentKeyIndex = 0;

	public SearchService(List<String> apiKeys) {
		this(apiKeys, new OkHttpClient.Builder()
				.connectTimeout(API_TIMEOUT_SECONDS, TimeUnit.SECONDS)
				.readTimeout(API_TIMEOUT_SECONDS, TimeUnit.SECONDS)
				.writeTimeout(API_TIMEOUT_SECONDS, TimeUnit.SECONDS)
				.build());
	}

	public SearchService(List<String> apiKeys, OkHttpClient client) {
		this.httpClient = client;
		if (apiKeys == null || apiKeys.isEmpty()) {
			this.apiKeys = new ArrayList<>();
			Log.w(TAG, "WARN: No Serper API keys provided. Search functionality will be disabled.");
		} else {
			this.apiKeys = apiKeys;
		}
	}

	/**
	 * !!! DEPRECATED for KnowledgeQueryHandler. Use getFullSearchResult() instead.
	 */
	@Deprecated
	public Optional<List<Snippet>> getSearchResults(String query) throws SearchServiceException {
		Optional<FullSearchResult> fullResultOpt = getFullSearchResult(query);
		return fullResultOpt.map(fullResult -> fullResult.organic);
	}

	/**
	 * Retrieves the FULL structured search response for intelligent answer extraction.
	 * This method performs network operations and MUST be called from a background thread.
	 *
	 * @param query The search query.
	 * @return An Optional containing the full structured search response.
	 * @throws SearchServiceException if all API keys fail.
	 */
	public Optional<FullSearchResult> getFullSearchResult(String query) throws SearchServiceException {
		if (this.apiKeys.isEmpty()) {
			throw new SearchServiceException("Search service is disabled; no API keys are configured.");
		}
		if (query == null || query.isBlank()) {
			Log.w(TAG, "getFullSearchResult called with empty or null query.");
			return Optional.empty();
		}

		String encodedQuery;
		try {
			encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8.name());
		} catch (IOException e) {
			Log.e(TAG, "Failed to URL encode query: " + query, e);
			throw new SearchServiceException("Failed to encode search query.", e);
		}

		String lastError = "No attempts made with available API keys.";
		String requestJsonBody = "{\"q\":\"" + encodedQuery + "\"}";

		// Loop through the number of available keys as attempts
		for (int i = 0; i < apiKeys.size(); i++) {
			String apiKey = getNextApiKey();
			Log.d(TAG, "Attempting search for '" + query + "' with key ending in: ..." + (apiKey.length() > 4 ? apiKey.substring(apiKey.length() - 4) : apiKey));

			RequestBody body = RequestBody.create(requestJsonBody, JSON);
			Request request = new Request.Builder()
					.url(API_URL)
					.header("X-API-KEY", apiKey)
					.post(body)
					.build();

			try (Response response = httpClient.newCall(request).execute()) {
				ResponseBody responseBody = response.body();
				String responseBodyString = (responseBody != null) ? responseBody.string() : null;

				if (response.isSuccessful() && responseBodyString != null) {
					// Return the entire structured result object, letting GSON map everything.
					Log.d(TAG, "Successful search response.");
					try {
						// CRITICAL FIX: Map the entire body to the new FullSearchResult class
						FullSearchResult fullResult = gson.fromJson(responseBodyString, FullSearchResult.class);

						if (fullResult.answerBox != null || fullResult.knowledgeGraph != null || (fullResult.organic != null && !fullResult.organic.isEmpty())) {
							return Optional.of(fullResult);
						} else {
							lastError = "API key returned 200 but no useful results found (no snippets/links).";
							// Log.w(TAG, lastError + " Body: " + responseBodyString); // Removed body to clean up logs
							Log.w(TAG, lastError);
						}

					} catch (JsonSyntaxException e) {
						lastError = "JSON parsing error: " + e.getMessage();
						Log.e(TAG, "Search API JSON parsing error for key ending in ..." + (apiKey.length() > 4 ? apiKey.substring(apiKey.length() - 4) : apiKey), e);
					}
				} else {
					lastError = "API key returned status " + response.code() + ". Body: " + (responseBodyString != null ? responseBodyString : "N/A");
					Log.w(TAG, "Search API request failed: " + lastError);
				}
			} catch (IOException e) {
				lastError = "Network error: " + e.getClass().getSimpleName() + ": " + e.getMessage();
				Log.e(TAG, "Search API network error for key ending in ..." + (apiKey.length() > 4 ? apiKey.substring(apiKey.length() - 4) : apiKey), e);
			} catch (Exception e) {
				lastError = "Unexpected error: " + e.getClass().getSimpleName() + ": " + e.getMessage();
				Log.e(TAG, "Search API unexpected error for key ending in ..." + (apiKey.length() > 4 ? apiKey.substring(apiKey.length() - 4) : apiKey), e);
			}
		}

		throw new SearchServiceException("All Search API keys failed for query '" + query + "'. Last error: " + lastError);
	}

	private synchronized String getNextApiKey() {
		if (apiKeys.isEmpty()) {
			throw new IllegalStateException("No API keys available to cycle.");
		}
		String key = apiKeys.get(currentKeyIndex);
		currentKeyIndex = (currentKeyIndex + 1) % apiKeys.size();
		return key;
	}
}