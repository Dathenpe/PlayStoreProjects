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
import java.util.stream.IntStream;

/**
 * A resilient, multi-source service to fetch jokes from various free APIs.
 * It provides a specialized and reliable source for the joke-telling skill
 * by automatically falling back to a backup API if the primary one fails.
 */
public class JokeService {

// --- API Endpoints ---
// Primary API: Simple and fast.
private static final String OFFICIAL_JOKE_API_URL = "https://official-joke-api.appspot.com/random_joke";
// Backup API: More complex but highly configurable.
private static final String JOKEAPI_DEV_URL = "https://v2.jokeapi.dev/joke/Any?blacklistFlags=nsfw,religious,political,racist,sexist,explicit";

private final Gson gson = new Gson();

/**
 * Fetches a single random joke, trying the primary API first and then the backup.
 * This fallback mechanism makes the service highly resilient.
 *
 * @return An Optional containing the formatted joke string, or empty if all sources fail.
 */
public Optional<String> getJoke() {
	// The .or() method is a clean, functional way to implement fallback logic.
	return fetchFromOfficialApi().or(this::fetchFromJokeApiDev);
}

/**
 * Fetches a specified number of jokes by calling the resilient getJoke() method repeatedly.
 *
 * @param count The desired number of jokes.
 * @return A list of formatted joke strings.
 */
public List<String> getJokes(int count) {
	List<String> jokes = new ArrayList<>();
	// We call our resilient getJoke() method 'count' times.
	IntStream.range(0, count).forEach(i -> getJoke().ifPresent(jokes::add));
	return jokes;
}

/**
 * Attempts to fetch and parse a joke from the primary source (Official Joke API).
 */
private Optional<String> fetchFromOfficialApi() {
	try {
		HttpRequest request = HttpRequest.newBuilder().uri(URI.create(OFFICIAL_JOKE_API_URL)).build();
		HttpResponse<String> response = SharedHttpClient.get().send(request, HttpResponse.BodyHandlers.ofString());
		
		if (response.statusCode() != 200) {
			throw new RuntimeException("Received status code " + response.statusCode());
		}
		
		JsonObject jokeJson = gson.fromJson(response.body(), JsonObject.class);
		return Optional.of(formatOfficialJoke(jokeJson));
	} catch (Exception e) {
		System.err.println("WARN: Primary Joke API (Official) failed: " + e.getMessage());
		return Optional.empty(); // Return empty on any failure, allowing fallback.
	}
}

/**
 * Attempts to fetch and parse a joke from the backup source (JokeAPI.dev).
 */
private Optional<String> fetchFromJokeApiDev() {
	try {
		System.out.println("[DEBUG] Primary joke API failed. Trying backup...");
		HttpRequest request = HttpRequest.newBuilder().uri(URI.create(JOKEAPI_DEV_URL)).build();
		HttpResponse<String> response = SharedHttpClient.get().send(request, HttpResponse.BodyHandlers.ofString());
		
		if (response.statusCode() != 200) {
			throw new RuntimeException("Received status code " + response.statusCode());
		}
		
		JsonObject jokeJson = gson.fromJson(response.body(), JsonObject.class);
		// This API can return an error object in a 200 response.
		if (jokeJson.has("error") && jokeJson.get("error").getAsBoolean()) {
			throw new RuntimeException("API returned an error object: " + jokeJson.get("message").getAsString());
		}
		
		return Optional.of(formatJokeApiDevJoke(jokeJson));
	} catch (Exception e) {
		System.err.println("ERROR: Backup Joke API (JokeApi.dev) also failed: " + e.getMessage());
		return Optional.empty(); // All sources have failed.
	}
}

/**
 * Formats a joke from the Official Joke API's JSON structure.
 */
private String formatOfficialJoke(JsonObject jokeJson) {
	String setup = jokeJson.get("setup").getAsString();
	String punchline = jokeJson.get("punchline").getAsString();
	return String.format("Q: %s\nA: %s", setup, punchline);
}

/**
 * Formats a joke from the JokeAPI.dev's JSON structure, which can be single or two-part.
 */
private String formatJokeApiDevJoke(JsonObject jokeJson) {
	String type = jokeJson.get("type").getAsString();
	if ("twopart".equals(type)) {
		String setup = jokeJson.get("setup").getAsString();
		String delivery = jokeJson.get("delivery").getAsString();
		return String.format("Q: %s\nA: %s", setup, delivery);
	} else { // "single"
		return jokeJson.get("joke").getAsString();
	}
}
}