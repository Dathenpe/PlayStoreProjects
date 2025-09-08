package com.f9ld3.xavier.ai.V2.services;

import android.util.Log;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * A service to determine the approximate geographic location of the server
 * by using a public IP geolocation API. This is used to provide local context
 * for queries when no location is specified.
 */
public class IPGeolocationService {

	private static final String TAG = "IPGeolocationService";
	private static final String API_URL = "http://ip-api.com/json";
	private static final long API_TIMEOUT_SECONDS = 5; // OkHttp timeout

	private final OkHttpClient httpClient;

	// Default constructor using a new OkHttpClient instance.
	// Consider using a shared OkHttpClient instance (e.g., from SharedHttpClient)
	public IPGeolocationService() {
		this.httpClient = new OkHttpClient.Builder()
				.connectTimeout(API_TIMEOUT_SECONDS, TimeUnit.SECONDS)
				.readTimeout(API_TIMEOUT_SECONDS, TimeUnit.SECONDS)
				.writeTimeout(API_TIMEOUT_SECONDS, TimeUnit.SECONDS)
				.build();
		// If SharedHttpClient.get() is designed to return an OkHttpClient:
		// this.httpClient = SharedHttpClient.get();
	}

	// Constructor that allows injecting an OkHttpClient (good for testing and sharing)
	public IPGeolocationService(OkHttpClient client) {
		this.httpClient = client;
	}

	/**
	 * Fetches the current location based on the machine's public IP address.
	 * This method performs a network operation and MUST be called from a background thread.
	 *
	 * @return An Optional containing a JsonObject with location data if successful,
	 *         or empty otherwise.
	 */
	public Optional<JsonObject> getCurrentLocation() {
		Log.d(TAG, "Attempting to fetch current IP geolocation...");

		Request request = new Request.Builder()
				.url(API_URL)
				.get() // Default, but explicit
				.build();

		// THIS IS A SYNCHRONOUS (BLOCKING) NETWORK CALL.
		// IT MUST BE EXECUTED ON A BACKGROUND THREAD IN ANDROID.
		try (Response response = httpClient.newCall(request).execute()) {

			if (!response.isSuccessful()) {
				Log.w(TAG, "IP Geolocation API request failed with status: " + response.code() + " - " + response.message());
				return Optional.empty();
			}

			ResponseBody responseBody = response.body();
			if (responseBody == null) {
				Log.w(TAG, "IP Geolocation API response body was null.");
				return Optional.empty();
			}

			String responseBodyString = responseBody.string(); // Consume the body once
			Log.d(TAG, "IP Geolocation API response: " + responseBodyString);

			// JsonParser.parseString().getAsJsonObject() is correct here
			return Optional.of(JsonParser.parseString(responseBodyString).getAsJsonObject());

		} catch (JsonSyntaxException e) {
			Log.e(TAG, "Error parsing IP Geolocation JSON. Reason: " + e.getMessage(), e);
			return Optional.empty();
		} catch (IOException e) {
			Log.e(TAG, "Network error during IP Geolocation lookup. Reason: " + e.getMessage(), e);
			return Optional.empty();
		} catch (IllegalStateException e) { // e.g. if response body is attempted to be read more than once
			Log.e(TAG, "IllegalStateException during IP Geolocation lookup. Reason: " + e.getMessage(), e);
			return Optional.empty();
		} catch (Exception e) { // Catch any other unexpected errors
			Log.e(TAG, "Unexpected error during IP Geolocation lookup. Reason: " + e.getMessage(), e);
			return Optional.empty();
		}
	}
}
