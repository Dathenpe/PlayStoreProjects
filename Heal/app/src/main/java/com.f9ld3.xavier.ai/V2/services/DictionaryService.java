package com.f9ld3.xavier.ai.V2.services;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * A service to fetch word definitions from the free DictionaryAPI.
 * Encapsulates network communication and JSON parsing for dictionary lookups.
 */
public class DictionaryService {

	private static final String TAG = "DictionaryService";
	private static final String API_URL_TEMPLATE = "https://api.dictionaryapi.dev/api/v2/entries/en/%s";
	private static final long API_TIMEOUT_SECONDS = 5; // OkHttp timeout

	private final Gson gson = new Gson();
	private final OkHttpClient httpClient;

	// Default constructor using a new OkHttpClient instance.
	// Consider using a shared OkHttpClient instance (e.g., from SharedHttpClient)
	// if you have one configured for your application.
	public DictionaryService() {
		// Initialize OkHttpClient - consider sharing this instance across your app
		this.httpClient = new OkHttpClient.Builder()
				.connectTimeout(API_TIMEOUT_SECONDS, TimeUnit.SECONDS)
				.readTimeout(API_TIMEOUT_SECONDS, TimeUnit.SECONDS)
				.writeTimeout(API_TIMEOUT_SECONDS, TimeUnit.SECONDS)
				.build();
		// If SharedHttpClient.get() is designed to return an OkHttpClient:
		// this.httpClient = SharedHttpClient.get();
	}

	// Constructor that allows injecting an OkHttpClient (good for testing and sharing)
	public DictionaryService(OkHttpClient client) {
		this.httpClient = client;
	}


	/**
	 * Fetches the first available definition for a given word.
	 * This method performs a network operation and MUST be called from a background thread.
	 *
	 * @param word The word to define.
	 * @return An Optional containing the definition, or empty if not found or an error occurs.
	 */
	public Optional<String> getDefinition(String word) {
		if (word == null || word.isBlank()) {
			return Optional.empty();
		}

		String encodedWord;
		try {
			encodedWord = URLEncoder.encode(word, StandardCharsets.UTF_8.name());
		} catch (IOException e) { // URLEncoder.encode throws UnsupportedEncodingException, which is an IOException
			Log.e(TAG, "Failed to URL encode word: " + word, e);
			return Optional.empty();
		}

		String requestUrl = String.format(API_URL_TEMPLATE, encodedWord);

		Request okHttpRequest = new Request.Builder()
				.url(requestUrl)
				.header("Accept", "application/json") // Common practice, though API might not require it
				.get()
				.build();

		// THIS IS A SYNCHRONOUS (BLOCKING) NETWORK CALL.
		// IT MUST BE EXECUTED ON A BACKGROUND THREAD IN ANDROID.
		try (Response okHttpResponse = httpClient.newCall(okHttpRequest).execute()) {

			if (okHttpResponse.code() == 404) {
				Log.d(TAG, "Definition not found for word (404): " + word);
				return Optional.empty(); // The API returns 404 for words not found.
			}

			if (!okHttpResponse.isSuccessful()) {
				Log.e(TAG, "Dictionary API request failed for word '" + word + "' with status: " + okHttpResponse.code() + " - " + okHttpResponse.message());
				return Optional.empty(); // Treat other non-successful responses as errors
			}

			ResponseBody responseBody = okHttpResponse.body();
			if (responseBody == null) {
				Log.w(TAG, "Dictionary API response body was null for word: " + word);
				return Optional.empty();
			}

			String responseBodyString = responseBody.string();
			JsonArray entries = gson.fromJson(responseBodyString, JsonArray.class);

			if (entries == null || entries.size() == 0) {
				Log.d(TAG, "No entries found in Dictionary API response for word: " + word);
				return Optional.empty();
			}

			// Navigate the JSON structure to find the first definition.
			// Path: [0] -> "meanings" -> [0] -> "definitions" -> [0] -> "definition"
			JsonObject firstEntry = entries.get(0).getAsJsonObject();
			JsonArray meanings = firstEntry.getAsJsonArray("meanings");
			if (meanings == null || meanings.size() == 0) return Optional.empty();

			JsonObject firstMeaning = meanings.get(0).getAsJsonObject();
			JsonArray definitions = firstMeaning.getAsJsonArray("definitions");
			if (definitions == null || definitions.size() == 0) return Optional.empty();

			JsonObject firstDefinitionObject = definitions.get(0).getAsJsonObject();
			JsonElement definitionElement = firstDefinitionObject.get("definition");

			if (definitionElement != null && !definitionElement.isJsonNull()) {
				return Optional.of(definitionElement.getAsString());
			} else {
				Log.w(TAG, "Definition element was null or not found for word: " + word + " in JSON: " + firstDefinitionObject);
				return Optional.empty();
			}

		} catch (JsonSyntaxException e) {
			Log.e(TAG, "Error parsing dictionary JSON for word '" + word + "'. Reason: " + e.getMessage(), e);
			return Optional.empty(); // Treat parsing errors as 'not found' or error
		} catch (IOException e) {
			Log.e(TAG, "Network error fetching definition for word '" + word + "'. Reason: " + e.getMessage(), e);
			return Optional.empty(); // Treat network errors as 'not found' or error
		} catch (IllegalStateException e) {
			Log.e(TAG, "IllegalStateException during API call for '" + word + "' (e.g. body already read). Reason: " + e.getMessage(), e);
			return Optional.empty();
		} catch (Exception e) { // Catch-all for any other unexpected issues during JSON navigation
			Log.e(TAG, "Unexpected error processing definition for word '" + word + "'. Reason: " + e.getMessage(), e);
			return Optional.empty();
		}
	}
}
