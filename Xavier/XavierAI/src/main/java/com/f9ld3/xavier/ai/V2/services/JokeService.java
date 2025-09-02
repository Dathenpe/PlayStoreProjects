// C:/Users/Music_Minister/Desktop/PlayStore/PlayStoreProjects/Xavier/XavierAI/src/main/java/com/f9ld3/xavier/ai/V2/services/JokeService.java

package com.f9ld3.xavier.ai.V2.services;

import com.f9ld3.xavier.ai.V2.utils.SharedHttpClient;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.IntStream;

/**
 * A resilient, multi-source service to fetch jokes.
 * It prioritizes a local file for speed and reliability, falling back to
 * external APIs if the local source is unavailable.
 */
public class JokeService {

// --- Local and API Endpoints ---
private static final String LOCAL_JOKES_PATH = "responses/jokes.txt";
private static final String OFFICIAL_JOKE_API_URL = "https://official-joke-api.appspot.com/random_joke";
private static final String JOKEAPI_DEV_URL = "https://v2.jokeapi.dev/joke/Any?blacklistFlags=nsfw,religious,political,racist,sexist,explicit";

private final Gson gson = new Gson();
private final Random random = new Random();
private final List<String> localJokes;

public JokeService() {
	this.localJokes = loadJokesFromFile();
}

/**
 * Fetches a single random joke, trying the local file first, then the primary API,
 * and finally the backup API. This fallback chain makes the service highly resilient.
 *
 * @return An Optional containing the formatted joke string, or empty if all sources fail.
 */
public Optional<String> getJoke() {
	// The .or() method is a clean, functional way to implement the fallback chain.
	return fetchFromLocalFile()
			       .or(this::fetchFromOfficialApi)
			       .or(this::fetchFromJokeApiDev);
}

/**
 * Fetches a specified number of jokes by calling the resilient getJoke() method repeatedly.
 *
 * @param count The desired number of jokes.
 * @return A list of formatted joke strings.
 */
public List<String> getJokes(int count) {
	List<String> jokes = new ArrayList<>();
	IntStream.range(0, count).forEach(i -> getJoke().ifPresent(jokes::add));
	return jokes;
}

/**
 * Attempts to get a random joke from the pre-loaded local list.
 */
private Optional<String> fetchFromLocalFile() {
	if (localJokes != null && !localJokes.isEmpty()) {
		return Optional.of(localJokes.get(random.nextInt(localJokes.size())));
	}
	return Optional.empty();
}

/**
 * Attempts to fetch and parse a joke from the primary external source (Official Joke API).
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
		return Optional.empty();
	}
}

/**
 * Attempts to fetch and parse a joke from the backup external source (JokeAPI.dev).
 */
private Optional<String> fetchFromJokeApiDev() {
	try {
		System.out.println("[DEBUG] Local jokes and primary API failed. Trying backup API...");
		HttpRequest request = HttpRequest.newBuilder().uri(URI.create(JOKEAPI_DEV_URL)).build();
		HttpResponse<String> response = SharedHttpClient.get().send(request, HttpResponse.BodyHandlers.ofString());
		
		if (response.statusCode() != 200) {
			throw new RuntimeException("Received status code " + response.statusCode());
		}
		
		JsonObject jokeJson = gson.fromJson(response.body(), JsonObject.class);
		if (jokeJson.has("error") && jokeJson.get("error").getAsBoolean()) {
			throw new RuntimeException("API returned an error object: " + jokeJson.get("message").getAsString());
		}
		
		return Optional.of(formatJokeApiDevJoke(jokeJson));
	} catch (Exception e) {
		System.err.println("ERROR: All joke sources (local and APIs) failed. Last error: " + e.getMessage());
		return Optional.empty();
	}
}

private String formatOfficialJoke(JsonObject jokeJson) {
	String setup = jokeJson.get("setup").getAsString();
	String punchline = jokeJson.get("punchline").getAsString();
	return String.format("Q: %s\nA: %s", setup, punchline);
}

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

/**
 * Loads jokes from the local resource file at startup.
 */
private List<String> loadJokesFromFile() {
	List<String> templates = new ArrayList<>();
	try (InputStream is = JokeService.class.getClassLoader().getResourceAsStream(LOCAL_JOKES_PATH);
	     BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
		String line;
		while ((line = reader.readLine()) != null) {
			if (!line.trim().isEmpty() && !line.startsWith("#")) {
				templates.add(line.trim());
			}
		}
	} catch (Exception e) {
		System.err.println("WARN: Could not load local jokes from " + LOCAL_JOKES_PATH + ". " + e.getMessage());
		return Collections.emptyList(); // Return an empty list on failure
	}
	return templates;
}
}