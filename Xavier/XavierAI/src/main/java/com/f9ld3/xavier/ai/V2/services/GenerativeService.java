package com.f9ld3.xavier.ai.V2.services;

import com.f9ld3.xavier.ai.V2.utils.NetworkStatusChecker;
import com.f9ld3.xavier.ai.V2.utils.SharedHttpClient;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Optional;

/**
 * A service to interact with a generative AI model (like OpenAI's GPT series)
 * to generate creative text, such as lists, summaries, or explanations.
 * This version is configurable and fault-tolerant, supporting multiple API keys.
 */
public class GenerativeService {

private final String apiUrl;
private final List<String> apiKeys; // Changed from String to List<String>
private final String modelName;
private final Gson gson = new Gson();

/**
 * @param apiUrl The base URL for the API endpoint.
 * @param apiKeys A list of API keys to try in order.
 * @param modelName The name of the model to use.
 */
public GenerativeService(String apiUrl, List<String> apiKeys, String modelName) {
	this.apiUrl = apiUrl;
	this.apiKeys = apiKeys;
	this.modelName = modelName;
}

/**
 * Sends a prompt to the generative AI and returns the model's response.
 * It will iterate through the provided API keys until one succeeds.
 *
 * @param prompt The instruction for the AI (e.g., "List 10 common Nigerian foods").
 * @return An Optional containing the generated text, or empty on failure.
 */
public Optional<String> generate(String prompt) {
	if (apiKeys == null || apiKeys.isEmpty() || apiUrl == null || apiUrl.isBlank() || modelName == null || modelName.isBlank()) {
		System.err.println("WARN: GenerativeService is not fully configured (URL, Keys, or Model is missing).");
		return Optional.empty();
	}
	if (!NetworkStatusChecker.isOnline()) {
		System.err.println("GenerativeService: Network is offline. Aborting request.");
		return Optional.empty();
	}
	
	Exception lastException = null;
	
	// Loop through each key to provide fault tolerance
	for (String apiKey : apiKeys) {
		try {
			// Construct the request body for an OpenAI-compatible Chat Completions API
			var userMessage = new Message("user", prompt);
			var systemMessage = new Message("system", "You are a helpful assistant. Be concise and directly answer the user's question.");
			var requestPayload = new ChatRequest(this.modelName, List.of(systemMessage, userMessage), 0.7);
			String requestBody = gson.toJson(requestPayload);
			
			HttpRequest request = HttpRequest.newBuilder()
					                      .uri(URI.create(this.apiUrl))
					                      .header("Authorization", "Bearer " + apiKey) // Use the current key from the loop
					                      .header("Content-Type", "application/json")
					                      .POST(HttpRequest.BodyPublishers.ofString(requestBody))
					                      .build();
			
			HttpResponse<String> response = SharedHttpClient.get().send(request, HttpResponse.BodyHandlers.ofString());
			
			if (response.statusCode() == 200) {
				JsonObject responseJson = gson.fromJson(response.body(), JsonObject.class);
				JsonArray choices = responseJson.getAsJsonArray("choices");
				if (choices != null && !choices.isEmpty()) {
					JsonObject firstChoice = choices.get(0).getAsJsonObject();
					JsonObject message = firstChoice.getAsJsonObject("message");
					// Success! Return the result.
					return Optional.of(message.get("content").getAsString());
				}
			} else {
				// This key failed, but we can try the next one. Log the error.
				System.err.printf("Generative AI API key failed with status %d. Trying next key if available. Response: %s%n", response.statusCode(), response.body());
			}
			
		} catch (Exception e) {
			// Store the exception to log it later if all keys fail.
			lastException = e;
			System.err.println("Exception with one of the GenerativeService keys: " + e.getMessage());
		}
	}
	
	// If the loop completes without returning, all keys have failed.
	System.err.println("ERROR: All Generative AI API keys failed or returned no results. Last error: " +
			                   (lastException != null ? lastException.getMessage() : "No exception, API may have returned empty results."));
	
	return Optional.empty();
}

// Inner records for structuring the JSON request payload
private record Message(String role, String content) {}
private record ChatRequest(String model, List<Message> messages, double temperature) {}
}