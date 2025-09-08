package com.f9ld3.xavier.ai.V2.services;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;


public class JokeService {

	private static final String TAG = "JokeService"; // For logging

	private static final String LOCAL_JOKES_PATH = "responses/jokes.txt";
	private static final String OFFICIAL_JOKE_API_URL = "https://official-joke-api.appspot.com/random_joke";
	private static final String JOKEAPI_DEV_URL = "https://v2.jokeapi.dev/joke/Any?blacklistFlags=nsfw,religious,political,racist,sexist,explicit";
	private static final long API_TIMEOUT_SECONDS = 5; // OkHttp timeout

	private final Gson gson = new Gson();
	private final Random random = new Random();
	private final List<String> localJokes;
	private final OkHttpClient httpClient; // Shared OkHttpClient instance

	public JokeService(Context context) {
		this.localJokes = loadJokesFromFile(context);
		// Initialize OkHttpClient - consider sharing this instance across your app
		// (e.g., via your SharedHttpClient or a Dependency Injection framework)
		this.httpClient = new OkHttpClient.Builder()
				.connectTimeout(API_TIMEOUT_SECONDS, TimeUnit.SECONDS)
				.readTimeout(API_TIMEOUT_SECONDS, TimeUnit.SECONDS)
				.writeTimeout(API_TIMEOUT_SECONDS, TimeUnit.SECONDS)
				.build();
		// If SharedHttpClient.get() was supposed to return an OkHttpClient, integrate that here.
		// For example: this.httpClient = SharedHttpClient.get();
	}

	// Method to get a single joke, trying local then API or vice-versa
	// This method MUST be called from a background thread due to network operations.
	public Optional<String> getJoke() {
		// Fallback logic without Optional.or() for broader Java version compatibility
		Optional<String> joke = fetchFromLocalFile();
		if (joke.isPresent()) {
			return joke;
		}
		joke = fetchFromOfficialApi();
		if (joke.isPresent()) {
			return joke;
		}
		return fetchFromJokeApiDev(); // Last resort
	}


	// This method also performs network calls indirectly via getJoke(),
	// so it too should be called from a background thread.
	public List<String> getJokes(int count) {
		List<String> jokes = new ArrayList<>();
		// Note: IntStream.range().forEach() will execute sequentially.
		// If you need parallel fetching and have many jokes, consider other approaches.
		IntStream.range(0, count).forEach(i -> getJoke().ifPresent(jokes::add));
		return jokes;
	}

	private Optional<String> fetchFromLocalFile() {
		if (localJokes != null && !localJokes.isEmpty()) {
			return Optional.of(localJokes.get(random.nextInt(localJokes.size())));
		}
		return Optional.empty();
	}

	// THIS METHOD PERFORMS A NETWORK CALL AND MUST BE EXECUTED ON A BACKGROUND THREAD.
	private Optional<String> fetchFromOfficialApi() {
		Log.d(TAG, "Attempting to fetch joke from Official Joke API...");
		Request request = new Request.Builder()
				.url(OFFICIAL_JOKE_API_URL)
				.header("Accept", "application/json")
				.get()
				.build();

		try (Response response = httpClient.newCall(request).execute()) { // Synchronous call
			if (!response.isSuccessful()) {
				Log.w(TAG, "Primary Joke API (Official) request failed with status code: " + response.code() + " - " + response.message());
				return Optional.empty();
			}

			ResponseBody responseBody = response.body();
			if (responseBody == null) {
				Log.w(TAG, "Primary Joke API (Official) response body was null.");
				return Optional.empty();
			}

			String responseBodyString = responseBody.string();
			JsonObject jokeJson = gson.fromJson(responseBodyString, JsonObject.class);

			if (jokeJson == null || !jokeJson.has("setup") || !jokeJson.has("punchline")) {
				Log.w(TAG, "Primary Joke API (Official) response JSON is invalid or missing fields. Body: " + responseBodyString);
				return Optional.empty();
			}

			return Optional.of(formatOfficialJoke(jokeJson));

		} catch (IOException e) {
			Log.e(TAG, "Primary Joke API (Official) failed (OkHttp). Reason: " + e.getMessage(), e);
			return Optional.empty();
		} catch (JsonSyntaxException e) {
			Log.e(TAG, "Failed to parse JSON from Primary Joke API (Official). Reason: " + e.getMessage(), e);
			return Optional.empty();
		} catch (IllegalStateException e) {
			Log.e(TAG, "IllegalStateException from Primary Joke API (Official) (OkHttp). Reason: " + e.getMessage(), e);
			return Optional.empty();
		}
	}

	// THIS METHOD PERFORMS A NETWORK CALL AND MUST BE EXECUTED ON A BACKGROUND THREAD.
	private Optional<String> fetchFromJokeApiDev() {
		Log.d(TAG, "Local jokes and primary API failed. Trying backup API (JokeAPI.dev)...");
		Request request = new Request.Builder()
				.url(JOKEAPI_DEV_URL)
				.header("Accept", "application/json")
				.get()
				.build();

		try (Response response = httpClient.newCall(request).execute()) { // Synchronous call
			if (!response.isSuccessful()) {
				Log.w(TAG, "Backup Joke API (JokeAPI.dev) request failed with status code: " + response.code() + " - " + response.message());
				return Optional.empty();
			}

			ResponseBody responseBody = response.body();
			if (responseBody == null) {
				Log.w(TAG, "Backup Joke API (JokeAPI.dev) response body was null.");
				return Optional.empty();
			}

			String responseBodyString = responseBody.string();
			JsonObject jokeJson = gson.fromJson(responseBodyString, JsonObject.class);

			if (jokeJson == null) {
				Log.w(TAG, "Backup Joke API (JokeAPI.dev) response JSON was null. Body: " + responseBodyString);
				return Optional.empty();
			}

			if (jokeJson.has("error") && jokeJson.get("error").getAsBoolean()) {
				String errorMessage = jokeJson.has("message") ? jokeJson.get("message").getAsString() : "Unknown error";
				Log.e(TAG, "Backup Joke API (JokeAPI.dev) returned an error object: " + errorMessage);
				// Depending on the API, "causedBy" and "additionalInfo" might also be useful for logging.
				return Optional.empty();
			}

			// Check for expected fields based on API structure (single or twopart)
			boolean isTwoPart = "twopart".equals(jokeJson.has("type") ? jokeJson.get("type").getAsString() : "");
			boolean hasSingleJoke = jokeJson.has("joke");
			boolean hasSetup = jokeJson.has("setup");
			boolean hasDelivery = jokeJson.has("delivery");

			if (!((isTwoPart && hasSetup && hasDelivery) || (!isTwoPart && hasSingleJoke))) {
				Log.w(TAG, "Backup Joke API (JokeAPI.dev) response JSON is invalid or missing expected joke fields. Body: " + responseBodyString);
				return Optional.empty();
			}


			return Optional.of(formatJokeApiDevJoke(jokeJson));

		} catch (IOException e) {
			Log.e(TAG, "Backup Joke API (JokeAPI.dev) failed (OkHttp). Reason: " + e.getMessage(), e);
			return Optional.empty();
		} catch (JsonSyntaxException e) {
			Log.e(TAG, "Failed to parse JSON from Backup Joke API (JokeAPI.dev). Reason: " + e.getMessage(), e);
			return Optional.empty();
		} catch (IllegalStateException e) {
			Log.e(TAG, "IllegalStateException from Backup Joke API (JokeAPI.dev) (OkHttp). Reason: " + e.getMessage(), e);
			return Optional.empty();
		}
	}

	private String formatOfficialJoke(JsonObject jokeJson) {
		// Add null checks for safety, though previous checks in fetchFromOfficialApi should cover this.
		String setup = jokeJson.has("setup") ? jokeJson.get("setup").getAsString() : "Error: Missing setup";
		String punchline = jokeJson.has("punchline") ? jokeJson.get("punchline").getAsString() : "Error: Missing punchline";
		return String.format("Q: %s\nA: %s", setup, punchline);
	}

	private String formatJokeApiDevJoke(JsonObject jokeJson) {
		String type = jokeJson.has("type") ? jokeJson.get("type").getAsString() : "single"; // Default to single if type is missing

		if ("twopart".equals(type)) {
			String setup = jokeJson.has("setup") ? jokeJson.get("setup").getAsString() : "Error: Missing setup";
			String delivery = jokeJson.has("delivery") ? jokeJson.get("delivery").getAsString() : "Error: Missing delivery";
			return String.format("Q: %s\nA: %s", setup, delivery);
		} else { // "single" or unknown type treated as single
			return jokeJson.has("joke") ? jokeJson.get("joke").getAsString() : "Error: Missing joke";
		}
	}

	private List<String> loadJokesFromFile(Context context) {
		List<String> templates = new ArrayList<>();
		try (InputStream is = context.getAssets().open(LOCAL_JOKES_PATH);
			 BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
			String line;
			while ((line = reader.readLine()) != null) {
				if (!line.trim().isEmpty() && !line.startsWith("#")) {
					templates.add(line.trim());
				}
			}
		} catch (IOException e) { // More specific exception
			Log.e(TAG, "Could not load local jokes from " + LOCAL_JOKES_PATH + ". " + e.getMessage(), e);
			return Collections.emptyList(); // Return immutable empty list
		} catch (Exception e) { // Catch any other unexpected exceptions
			Log.e(TAG, "Unexpected error loading local jokes from " + LOCAL_JOKES_PATH + ". " + e.getMessage(), e);
			return Collections.emptyList();
		}
		return templates;
	}
}
