package com.f9ld3.xavier.ai.V2.services;

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
 * A resilient service for calling a generative AI API (like Groq).
 * It cycles through a list of API keys to provide fault tolerance.
 */
public class GenerativeService {

/**
 * A custom exception for when the generative service fails.
 */
public static class GenerativeServiceException extends RuntimeException {
	public GenerativeServiceException(String message) {
		super(message);
	}
}

private final String apiUrl;
private final List<String> apiKeys;
private final String model;
private final Gson gson = new Gson();
private int currentKeyIndex = 0;

public GenerativeService(String apiUrl, List<String> apiKeys, String model) {
	this.apiUrl = apiUrl;
	this.model = model;
	if (apiKeys == null || apiKeys.isEmpty()) {
		this.apiKeys = new ArrayList<>();
		System.err.println("WARN: No Generative AI API keys provided. Generative functionality will be disabled.");
	} else {
		this.apiKeys = apiKeys;
	}
}

/**
 * Generates a response from the configured generative AI model.
 *
 * @param prompt The prompt to send to the model.
 * @return An Optional containing the generated text.
 * @throws GenerativeServiceException if all API keys fail or the service is misconfigured.
 */
public Optional<String> generate(String prompt) throws GenerativeServiceException {
	if (apiKeys.isEmpty() || apiUrl == null || model == null) {
		throw new GenerativeServiceException("Generative service is disabled; missing API URL, keys, or model name.");
	}
	
	String lastError = "No attempts made.";
	for (int i = 0; i < apiKeys.size(); i++) {
		try {
			String apiKey = getNextApiKey();
			String requestBody = buildRequestBody(prompt);
			
			HttpRequest request = HttpRequest.newBuilder()
					                      .uri(URI.create(apiUrl))
					                      .header("Authorization", "Bearer " + apiKey)
					                      .header("Content-Type", "application/json")
					                      .POST(HttpRequest.BodyPublishers.ofString(requestBody))
					                      .build();
			
			HttpResponse<String> response = SharedHttpClient.get().send(request, HttpResponse.BodyHandlers.ofString());
			
			if (response.statusCode() == 200) {
				JsonObject jsonResponse = gson.fromJson(response.body(), JsonObject.class);
				JsonArray choices = jsonResponse.getAsJsonArray("choices");
				if (choices != null && !choices.isEmpty()) {
					JsonObject firstChoice = choices.get(0).getAsJsonObject();
					String content = firstChoice.getAsJsonObject("message").get("content").getAsString();
					return Optional.of(content.trim());
				}
			}
			lastError = "API key returned status " + response.statusCode() + ". Body: " + response.body();
		} catch (Exception e) {
			lastError = e.getClass().getSimpleName() + ": " + e.getMessage();
		}
	}
	
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
	String key = apiKeys.get(currentKeyIndex);
	currentKeyIndex = (currentKeyIndex + 1) % apiKeys.size();
	return key;
}
}