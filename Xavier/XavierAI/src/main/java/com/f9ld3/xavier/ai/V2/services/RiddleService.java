// C:/Users/Music_Minister/Desktop/PlayStore/PlayStoreProjects/Xavier/XavierAI/src/main/java/com/f9ld3/xavier/ai/V2/services/RiddleService.java
package com.f9ld3.xavier.ai.V2.services;

import com.f9ld3.xavier.ai.V2.utils.SharedHttpClient;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * A resilient, multi-source service to provide riddles.
 * It randomly chooses between a local file and an external API, with the other as a fallback.
 */
public class RiddleService {

/**
 * A record to cleanly store a riddle's question and answer.
 */
public record Riddle(String question, String answer) {}

private static final String LOCAL_RIDDLES_PATH = "responses/riddles.txt";
private static final String RIDDLE_API_URL = "https://riddles-api.vercel.app/random";
private static final Duration API_TIMEOUT = Duration.ofSeconds(5);

private final Random random = new Random();
private final Gson gson = new Gson();
private final List<Riddle> localRiddles;

public RiddleService() {
	this.localRiddles = loadRiddlesFromFile();
}

/**
 * Fetches a single random riddle by randomly choosing between the local file and the API as the primary source.
 * If the chosen primary source fails, it automatically falls back to the other.
 *
 * @return An Optional containing a Riddle object, or empty if all sources fail.
 */
public Optional<Riddle> getRiddle() {
	// This random selection is a nice way to balance API calls with local speed.
	boolean tryApiFirst = random.nextBoolean();
	if (tryApiFirst) {
		return fetchFromApi().or(this::fetchFromLocalFile);
	} else {
		return fetchFromLocalFile().or(this::fetchFromApi);
	}
}

private Optional<Riddle> fetchFromLocalFile() {
	if (localRiddles.isEmpty()) {
		return Optional.empty();
	}
	return Optional.of(localRiddles.get(random.nextInt(localRiddles.size())));
}

/**
 * REFINED: Simplified error handling and added a request-specific timeout.
 */
private Optional<Riddle> fetchFromApi() {
	try {
		HttpRequest request = HttpRequest.newBuilder()
				                      .uri(URI.create(RIDDLE_API_URL))
				                      .header("Accept", "application/json")
				                      .timeout(API_TIMEOUT) // Add request-specific timeout for robustness
				                      .build();
		
		HttpResponse<String> response = SharedHttpClient.get().send(request, HttpResponse.BodyHandlers.ofString());
		
		if (response.statusCode() != 200) {
			// Log the failure and return empty to trigger the fallback.
			System.err.println("WARN: Riddle API request failed with status code: " + response.statusCode());
			return Optional.empty();
		}
		
		JsonObject riddleJson = gson.fromJson(response.body(), JsonObject.class);
		String question = riddleJson.get("riddle").getAsString();
		String answer = riddleJson.get("answer").getAsString();
		
		return Optional.of(new Riddle(question, answer));
		
	} catch (IOException | InterruptedException | JsonSyntaxException e) {
		// Catch specific, expected exceptions for clearer error handling.
		System.err.println("WARN: External Riddle API failed. Reason: " + e.getMessage());
		if (e instanceof InterruptedException) {
			// Preserve the interrupted status for the calling thread.
			Thread.currentThread().interrupt();
		}
		return Optional.empty(); // Return empty on any failure to allow fallback.
	}
}

/**
 * REFINED: Uses a more concise stream-based approach for loading the file.
 */
private List<Riddle> loadRiddlesFromFile() {
	try (InputStream is = RiddleService.class.getClassLoader().getResourceAsStream(LOCAL_RIDDLES_PATH)) {
		if (is == null) {
			System.err.println("WARN: Resource file not found: " + LOCAL_RIDDLES_PATH + ". Local riddles will be unavailable.");
			return Collections.emptyList();
		}
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
			return reader.lines()
					       .map(line -> line.split("\\|", 2))
					       .filter(parts -> parts.length == 2 && !parts[0].isBlank() && !parts[1].isBlank())
					       .map(parts -> new Riddle(parts[0].trim(), parts[1].trim()))
					       .collect(Collectors.toList());
		}
	} catch (Exception e) {
		System.err.println("WARN: Could not load local riddles from " + LOCAL_RIDDLES_PATH + ". Riddle feature will rely on API. Reason: " + e.getMessage());
		return Collections.emptyList();
	}
}
}