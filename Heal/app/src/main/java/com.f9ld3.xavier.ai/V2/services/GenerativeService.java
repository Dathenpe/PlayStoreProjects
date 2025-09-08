package com.f9ld3.xavier.ai.V2.services;

import android.util.Log; // Using Android's Log

// OkHttp imports
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response; // This is okhttp3.Response
import okhttp3.ResponseBody;

// Assuming SharedHttpClient can be adapted or provides an OkHttpClient instance
// import com.f9ld3.xavier.ai.V2.utils.SharedHttpClient;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

// Removed: java.net.URI;
// Removed: java.net.http.HttpRequest;
// Removed: java.net.http.HttpResponse;
import java.io.IOException; // For OkHttp exceptions
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit; // For OkHttp timeouts

/**
 * A resilient service for calling a generative AI API (like Groq).
 * It cycles through a list of API keys to provide fault tolerance.
 */
public class GenerativeService {

	private static final String TAG = "GenerativeService";
	private static final long API_TIMEOUT_SECONDS = 15; // OkHttp timeout, potentially longer for generative APIs
	public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");


	/**
	 * A custom exception for when the generative service fails.
	 */
	public static class GenerativeServiceException extends RuntimeException {
		public GenerativeServiceException(String message) {
			super(message);
		}
		public GenerativeServiceException(String message, Throwable cause) {
			super(message, cause);
		}
	}

	private final String apiUrl;
	private final List<String> apiKeys;
	private final String model;
	private final Gson gson = new Gson();
	private final OkHttpClient httpClient;
	private int currentKeyIndex = 0;

	public GenerativeService(String apiUrl, List<String> apiKeys, String model) {
		this(apiUrl, apiKeys, model, new OkHttpClient.Builder() // Default OkHttpClient
				.connectTimeout(API_TIMEOUT_SECONDS, TimeUnit.SECONDS)
				.readTimeout(API_TIMEOUT_SECONDS, TimeUnit.SECONDS)
				.writeTimeout(API_TIMEOUT_SECONDS, TimeUnit.SECONDS)
				.build());
	}

	// Constructor allowing OkHttpClient injection (for testing or shared client)
	public GenerativeService(String apiUrl, List<String> apiKeys, String model, OkHttpClient client) {
		this.apiUrl = apiUrl;
		this.model = model;
		this.httpClient = client; // Use injected or default client

		if (apiKeys == null || apiKeys.isEmpty()) {
			this.apiKeys = new ArrayList<>(); // Ensure it's not null
			Log.w(TAG, "WARN: No Generative AI API keys provided. Generative functionality will be disabled.");
		} else {
			this.apiKeys = apiKeys;
		}
	}


	/**
	 * Generates a response from the configured generative AI model.
	 * This method performs network operations and MUST be called from a background thread.
	 *
	 * @param prompt The prompt to send to the model.
	 * @return An Optional containing the generated text.
	 * @throws GenerativeServiceException if all API keys fail or the service is misconfigured.
	 */
	public Optional<String> generate(String prompt) throws GenerativeServiceException {
		if (this.apiUrl == null || this.apiUrl.isEmpty()) {
			throw new GenerativeServiceException("Generative service is disabled; API URL is missing.");
		}
		if (this.apiKeys.isEmpty()) { // Check the initialized this.apiKeys
			throw new GenerativeServiceException("Generative service is disabled; API keys are missing.");
		}
		if (this.model == null || this.model.isEmpty()) {
			throw new GenerativeServiceException("Generative service is disabled; model name is missing.");
		}
		if (prompt == null || prompt.isBlank()){
			Log.w(TAG,"Generate called with empty or null prompt.");
			return Optional.empty();
		}

		String lastError = "No attempts made with available API keys.";

		// Loop through the number of available keys as attempts
		for (int i = 0; i < apiKeys.size(); i++) {
			String apiKey = getNextApiKey(); // This cycles through the keys
			String requestBodyJson = buildRequestBody(prompt);
			Log.d(TAG, "Attempting generative AI request with key ending in: ..." + (apiKey.length() > 4 ? apiKey.substring(apiKey.length() - 4) : apiKey));
			Log.d(TAG, "Request Body: " + requestBodyJson);


			RequestBody body = RequestBody.create(requestBodyJson, JSON);
			Request request = new Request.Builder()
					.url(this.apiUrl)
					.header("Authorization", "Bearer " + apiKey)
					.header("Content-Type", "application/json") // OkHttp's RequestBody.create with MediaType handles this
					.post(body)
					.build();

			// THIS IS A SYNCHRONOUS (BLOCKING) NETWORK CALL.
			// IT MUST BE EXECUTED ON A BACKGROUND THREAD IN ANDROID.
			try (Response response = httpClient.newCall(request).execute()) {
				ResponseBody responseBody = response.body();
				String responseBodyString = (responseBody != null) ? responseBody.string() : null; // Consume body once

				if (response.isSuccessful() && responseBodyString != null) {
					Log.d(TAG, "Successful response from generative API. Body: " + responseBodyString);
					JsonObject jsonResponse = gson.fromJson(responseBodyString, JsonObject.class);
					JsonArray choices = jsonResponse.getAsJsonArray("choices");

					if (choices != null && !choices.isEmpty()) {
						JsonObject firstChoice = choices.get(0).getAsJsonObject();
						if (firstChoice.has("message") && firstChoice.getAsJsonObject("message").has("content")) {
							String content = firstChoice.getAsJsonObject("message").get("content").getAsString();
							return Optional.of(content.trim());
						} else {
							lastError = "Response JSON 'choices[0].message' missing 'content'. Body: " + responseBodyString;
							Log.w(TAG, lastError);
						}
					} else {
						lastError = "Response JSON missing 'choices' array or it's empty. Body: " + responseBodyString;
						Log.w(TAG, lastError);
					}
				} else {
					lastError = "API key returned status " + response.code() + ". Body: " + (responseBodyString != null ? responseBodyString : "N/A");
					Log.w(TAG, "Generative API request failed: " + lastError);
					// Specific error handling for common issues if needed (e.g., 401, 403, 429)
					if (response.code() == 401 || response.code() == 403) {
						Log.e(TAG, "Authentication/Authorization error with API key. Check the key.");
					} else if (response.code() == 429) {
						Log.w(TAG, "Rate limit hit for current API key.");
						// Optionally, you could add logic here to wait or immediately try the next key
					}
				}
			} catch (JsonSyntaxException e) {
				lastError = "JSON parsing error: " + e.getMessage();
				Log.e(TAG, "Generative API JSON parsing error: " + lastError, e);
			} catch (IOException e) {
				lastError = "Network error: " + e.getClass().getSimpleName() + ": " + e.getMessage();
				Log.e(TAG, "Generative API network error: " + lastError, e);
			} catch (Exception e) { // Catch any other unexpected errors
				lastError = "Unexpected error: " + e.getClass().getSimpleName() + ": " + e.getMessage();
				Log.e(TAG, "Generative API unexpected error: " + lastError, e);
			}
		} // End of for loop (API key retry)

		throw new GenerativeServiceException("All Generative AI API keys failed. Last error: " + lastError);
	}

	private String buildRequestBody(String prompt) {
		JsonObject message = new JsonObject();
		message.addProperty("role", "user");
		message.addProperty("content", prompt);

		JsonArray messages = new JsonArray();
		messages.add(message);

		JsonObject body = new JsonObject();
		body.add("messages", messages);
		body.addProperty("model", this.model);

		return gson.toJson(body);
	}

	private synchronized String getNextApiKey() {
		if (apiKeys.isEmpty()) { // Should be caught by the check in generate(), but defensive
			throw new IllegalStateException("No API keys available to cycle.");
		}
		String key = apiKeys.get(currentKeyIndex);
		currentKeyIndex = (currentKeyIndex + 1) % apiKeys.size();
		return key;
	}
}
