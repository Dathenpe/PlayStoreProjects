package com.f9ld3.xavier.ai.V2.services;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * A resilient, multi-source service to fetch random, interesting facts.
 * It automatically falls back to a backup API if the primary one fails.
 */
public class FunFactService {

	private static final String TAG = "FunFactService";

	// Primary API for facts.
	private static final String PRIMARY_FACT_API_URL = "https://uselessfacts.jsph.pl/api/v2/facts/random?language=en";
	// Backup API for facts.
	private static final String BACKUP_FACT_API_URL = "https://api.aakhilv.me/fun/facts";
	private static final long API_TIMEOUT_SECONDS = 5; // OkHttp timeout

	private final Gson gson = new Gson();
	private final OkHttpClient httpClient;

	// Default constructor using a new OkHttpClient instance.
	// Consider using a shared OkHttpClient instance (e.g., from SharedHttpClient)
	public FunFactService() {
		this.httpClient = new OkHttpClient.Builder()
				.connectTimeout(API_TIMEOUT_SECONDS, TimeUnit.SECONDS)
				.readTimeout(API_TIMEOUT_SECONDS, TimeUnit.SECONDS)
				.writeTimeout(API_TIMEOUT_SECONDS, TimeUnit.SECONDS)
				.build();
		// If SharedHttpClient.get() is designed to return an OkHttpClient:
		// this.httpClient = SharedHttpClient.get();
	}

	// Constructor that allows injecting an OkHttpClient (good for testing and sharing)
	public FunFactService(OkHttpClient client) {
		this.httpClient = client;
	}

	/**
	 * Fetches a single random fun fact, trying the primary API first and then the backup.
	 * This method performs network operations and MUST be called from a background thread.
	 *
	 * @return An Optional containing the fact, or empty if all sources fail.
	 */
	public Optional<String> getFact() {
		// Fallback logic compatible with older Java versions (without Optional.or())
		Optional<String> fact = fetchFromPrimaryFactApi();
		if (fact.isPresent()) {
			return fact;
		}
		Log.d(TAG, "Primary API failed, trying backup API for fun fact.");
		return fetchFromBackupFactApi();
	}

	/**
	 * Attempts to fetch and parse a fact from the primary source (uselessfacts.jsph.pl).
	 * This method performs a network operation and MUST be called from a background thread.
	 */
	private Optional<String> fetchFromPrimaryFactApi() {
		Log.d(TAG, "Attempting to fetch fact from Primary API...");
		Request request = new Request.Builder()
				.url(PRIMARY_FACT_API_URL)
				.header("Accept", "application/json")
				.get()
				.build();

		try (Response response = httpClient.newCall(request).execute()) { // Synchronous call
			if (!response.isSuccessful()) {
				Log.w(TAG, "Primary FunFact API request failed with status code: " + response.code() + " - " + response.message());
				return Optional.empty();
			}

			ResponseBody responseBody = response.body();
			if (responseBody == null) {
				Log.w(TAG, "Primary FunFact API response body was null.");
				return Optional.empty();
			}

			String responseBodyString = responseBody.string();
			JsonObject factJson = gson.fromJson(responseBodyString, JsonObject.class);

			if (factJson != null && factJson.has("text") && factJson.get("text").isJsonPrimitive()) {
				return Optional.of(factJson.get("text").getAsString());
			} else {
				Log.w(TAG, "Primary FunFact API JSON response is invalid or missing 'text' field. Body: " + responseBodyString);
				return Optional.empty();
			}

		} catch (IOException e) {
			Log.e(TAG, "Primary FunFact API failed (OkHttp). Reason: " + e.getMessage(), e);
			return Optional.empty();
		} catch (JsonSyntaxException e) {
			Log.e(TAG, "Failed to parse JSON from Primary FunFact API. Reason: " + e.getMessage(), e);
			return Optional.empty();
		} catch (IllegalStateException e) {
			Log.e(TAG, "IllegalStateException from Primary FunFact API (OkHttp). Reason: " + e.getMessage(), e);
			return Optional.empty();
		}
	}

	/**
	 * Attempts to fetch and parse a fact from the backup source (api.aakhilv.me).
	 * This method performs a network operation and MUST be called from a background thread.
	 */
	private Optional<String> fetchFromBackupFactApi() {
		Log.d(TAG, "Attempting to fetch fact from Backup API...");
		Request request = new Request.Builder()
				.url(BACKUP_FACT_API_URL)
				.header("Accept", "application/json")
				.get()
				.build();

		try (Response response = httpClient.newCall(request).execute()) { // Synchronous call
			if (!response.isSuccessful()) {
				Log.w(TAG, "Backup FunFact API request failed with status code: " + response.code() + " - " + response.message());
				return Optional.empty();
			}

			ResponseBody responseBody = response.body();
			if (responseBody == null) {
				Log.w(TAG, "Backup FunFact API response body was null.");
				return Optional.empty();
			}

			String responseBodyString = responseBody.string();
			// Using JsonParser as the response can be a direct string or a JSON array/object
			JsonElement jsonElement = JsonParser.parseString(responseBodyString);

			if (jsonElement.isJsonArray()) {
				JsonArray factArray = jsonElement.getAsJsonArray();
				if (factArray.size() > 0) {
					// Get a random element if it's an array
					JsonElement randomElement = factArray.get(ThreadLocalRandom.current().nextInt(factArray.size()));
					if (randomElement.isJsonPrimitive()) { // Ensure the element itself is a primitive string
						return Optional.of(randomElement.getAsString());
					} else {
						Log.w(TAG, "Element in Backup FunFact API array is not a primitive string: " + randomElement.toString());
						return Optional.empty();
					}
				} else {
					Log.w(TAG, "Backup FunFact API returned an empty array. Body: " + responseBodyString);
					return Optional.empty();
				}
			} else if (jsonElement.isJsonPrimitive()) {
				// If it's just a single string, return that.
				return Optional.of(jsonElement.getAsString());
			} else if (jsonElement.isJsonObject()) {
				// Some APIs might wrap a single fact in an object, e.g., {"fact": "..."}
				// Adapt this if the backup API has a known object structure for single facts
				JsonObject factObject = jsonElement.getAsJsonObject();
				if (factObject.has("fact") && factObject.get("fact").isJsonPrimitive()) { // Example key "fact"
					return Optional.of(factObject.get("fact").getAsString());
				} else {
					Log.w(TAG, "Backup FunFact API returned an object without a known 'fact' field. Body: " + responseBodyString);
					return Optional.empty();
				}
			} else {
				Log.w(TAG, "Backup FunFact API returned an unexpected JSON structure. Body: " + responseBodyString);
				return Optional.empty();
			}

		} catch (IOException e) {
			Log.e(TAG, "Backup FunFact API failed (OkHttp). Reason: " + e.getMessage(), e);
			return Optional.empty();
		} catch (JsonSyntaxException e) {
			Log.e(TAG, "Failed to parse JSON from Backup FunFact API. Reason: " + e.getMessage(), e);
			return Optional.empty();
		} catch (IllegalStateException e) {
			Log.e(TAG, "IllegalStateException from Backup FunFact API (OkHttp). Reason: " + e.getMessage(), e);
			return Optional.empty();
		}
	}
}
