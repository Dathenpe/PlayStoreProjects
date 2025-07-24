package com.f9ld3.xavier.ai.V2.services;

import com.f9ld3.xavier.ai.V2.utils.SharedHttpClient;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * A service to fetch word definitions from the free DictionaryAPI.
 * Encapsulates network communication and JSON parsing for dictionary lookups.
 */
public class DictionaryService {

private static final String API_URL_TEMPLATE = "https://api.dictionaryapi.dev/api/v2/entries/en/%s";
private final Gson gson = new Gson();

/**
 * Fetches the first available definition for a given word.
 *
 * @param word The word to define.
 * @return An Optional containing the definition, or empty if not found.
 * @throws Exception if the API request fails for reasons other than 'not found'.
 */
public Optional<String> getDefinition(String word) throws Exception {
	String encodedWord = URLEncoder.encode(word, StandardCharsets.UTF_8);
	String requestUrl = String.format(API_URL_TEMPLATE, encodedWord);
	
	HttpRequest request = HttpRequest.newBuilder().uri(URI.create(requestUrl)).build();
	HttpResponse<String> response = SharedHttpClient.get().send(request, HttpResponse.BodyHandlers.ofString());
	
	if (response.statusCode() == 404) {
		return Optional.empty(); // The API returns 404 for words not found.
	}
	if (response.statusCode() != 200) {
		throw new RuntimeException("Dictionary API request failed with status: " + response.statusCode());
	}
	
	JsonArray entries = gson.fromJson(response.body(), JsonArray.class);
	if (entries == null || entries.size() == 0) {
		return Optional.empty();
	}
	
	// Navigate the JSON structure to find the first definition.
	// Path: [0] -> "meanings" -> [0] -> "definitions" -> [0] -> "definition"
	try {
		JsonObject firstEntry = entries.get(0).getAsJsonObject();
		JsonArray meanings = firstEntry.getAsJsonArray("meanings");
		if (meanings == null || meanings.size() == 0) return Optional.empty();
		
		JsonObject firstMeaning = meanings.get(0).getAsJsonObject();
		JsonArray definitions = firstMeaning.getAsJsonArray("definitions");
		if (definitions == null || definitions.size() == 0) return Optional.empty();
		
		JsonObject firstDefinitionObject = definitions.get(0).getAsJsonObject();
		JsonElement definitionElement = firstDefinitionObject.get("definition");
		
		return Optional.of(definitionElement.getAsString());
	} catch (Exception e) {
		System.err.println("Error parsing dictionary JSON for word '" + word + "': " + e.getMessage());
		return Optional.empty(); // Treat parsing errors as 'not found'.
	}
}
}