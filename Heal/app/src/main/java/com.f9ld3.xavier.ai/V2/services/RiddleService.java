// C:/Users/Music_Minister/Desktop/PlayStore/PlayStoreProjects/Xavier/XavierAI/src/main/java/com/f9ld3/xavier/ai/V2/services/RiddleService.java
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
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class RiddleService {

	private static final String TAG = "RiddleService"; // For logging

	// Traditional class equivalent of the record
	public static final class Riddle { // Make it static if it's an inner class and doesn't need outer instance
		private final String question;
		private final String answer;

		public Riddle(String question, String answer) {
			this.question = question;
			this.answer = answer;
		}

		public String getQuestion() {
			return question;
		}

		public String getAnswer() {
			return answer;
		}

		// Optional: Implement equals(), hashCode(), and toString() if needed
		// The 'record' keyword would have generated these automatically.
		// For simple data comparison or use in Sets/Maps, you'd want them.

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			Riddle riddle = (Riddle) o;
			if (question != null ? !question.equals(riddle.question) : riddle.question != null) return false;
			return answer != null ? answer.equals(riddle.answer) : riddle.answer == null;
		}

		@Override
		public int hashCode() {
			int result = question != null ? question.hashCode() : 0;
			result = 31 * result + (answer != null ? answer.hashCode() : 0);
			return result;
		}

		@Override
		public String toString() {
			return "Riddle{" +
					"question='" + question + '\'' +
					", answer='" + answer + '\'' +
					'}';
		}
	}


	private static final String LOCAL_RIDDLES_PATH = "responses/riddles.txt";
	private static final String RIDDLE_API_URL = "https://riddles-api.vercel.app/random";
	private static final long API_TIMEOUT_SECONDS = 5; // OkHttp timeout in seconds

	private final Random random = new Random();
	private final Gson gson = new Gson();
	private final List<Riddle> localRiddles;
	private final OkHttpClient httpClient; // Shared OkHttpClient instance

	public RiddleService(Context context) {
		this.localRiddles = loadRiddlesFromFile(context);
		// Initialize OkHttpClient - you might want to share this instance across your app
		this.httpClient = new OkHttpClient.Builder()
				.connectTimeout(API_TIMEOUT_SECONDS, TimeUnit.SECONDS)
				.readTimeout(API_TIMEOUT_SECONDS, TimeUnit.SECONDS)
				.writeTimeout(API_TIMEOUT_SECONDS, TimeUnit.SECONDS)
				.build();
	}


	public Optional<Riddle> getRiddle() {
		boolean tryApiFirst = random.nextBoolean();
		Optional<Riddle> riddle;
		if (tryApiFirst) {
			riddle = fetchFromApi();
			if (riddle.isPresent()) {
				return riddle;
			}
			return fetchFromLocalFile();
		} else {
			riddle = fetchFromLocalFile();
			if (riddle.isPresent()) {
				return riddle;
			}
			return fetchFromApi();
		}
	}

	private Optional<Riddle> fetchFromLocalFile() {
		if (localRiddles.isEmpty()) {
			return Optional.empty();
		}
		return Optional.of(localRiddles.get(random.nextInt(localRiddles.size())));
	}

	private Optional<Riddle> fetchFromApi() {
		Request request = new Request.Builder()
				.url(RIDDLE_API_URL)
				.header("Accept", "application/json")
				.get() // Default, but good to be explicit
				.build();

		// Using OkHttp's synchronous execute() method.
		// THIS MUST BE CALLED ON A BACKGROUND THREAD IN ANDROID.
		try (Response response = httpClient.newCall(request).execute()) { // try-with-resources for Response
			if (!response.isSuccessful()) {
				Log.w(TAG, "WARN: Riddle API request failed with status code: " + response.code() + " - " + response.message());
				return Optional.empty();
			}

			ResponseBody responseBody = response.body();
			if (responseBody == null) {
				Log.w(TAG, "WARN: Riddle API response body was null.");
				return Optional.empty();
			}

			String responseBodyString = responseBody.string(); // This consumes the response body
			JsonObject riddleJson = gson.fromJson(responseBodyString, JsonObject.class);

			if (riddleJson == null || !riddleJson.has("riddle") || !riddleJson.has("answer")) {
				Log.w(TAG, "WARN: Riddle API response JSON is invalid or missing fields. Body: " + responseBodyString);
				return Optional.empty();
			}

			String question = riddleJson.get("riddle").getAsString();
			String answer = riddleJson.get("answer").getAsString();

			return Optional.of(new Riddle(question, answer));

		} catch (IOException e) {
			// Includes SocketTimeoutException, UnknownHostException, etc.
			Log.e(TAG, "WARN: External Riddle API (OkHttp) failed. Reason: " + e.getMessage(), e);
			return Optional.empty();
		} catch (JsonSyntaxException e) {
			Log.e(TAG, "WARN: Failed to parse JSON from Riddle API. Reason: " + e.getMessage(), e);
			return Optional.empty();
		} catch (IllegalStateException e) {
			// Can happen if responseBody.string() is called more than once
			Log.e(TAG, "WARN: IllegalStateException from Riddle API (OkHttp). Reason: " + e.getMessage(), e);
			return Optional.empty();
		}
		// No InterruptedException directly from OkHttp's execute(), but good practice if you introduce other interruptible operations.
		// If you were using OkHttp's enqueue with a Callback, InterruptedException could be relevant if you manage threads yourself.
	}

	private List<Riddle> loadRiddlesFromFile(Context context) {
		// Using Android Log for consistency
		try (InputStream is = context.getAssets().open(LOCAL_RIDDLES_PATH);
			 BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {

			return reader.lines()
					.map(line -> line.split("\\|", 2))
					.filter(parts -> parts.length == 2 && !parts[0].isBlank() && !parts[1].isBlank())
					.map(parts -> new Riddle(parts[0].trim(), parts[1].trim()))
					.collect(Collectors.toList());
		} catch (IOException e) { // Catch specific IOException first
			Log.e(TAG, "WARN: Could not load local riddles from " + LOCAL_RIDDLES_PATH + ". Riddle feature will rely on API. Reason: " + e.getMessage(), e);
			return Collections.emptyList();
		} catch (NullPointerException e) { // If context.getAssets().open returns null (though it throws IOException for not found)
			Log.e(TAG, "WARN: Resource file not found (InputStream was null): " + LOCAL_RIDDLES_PATH + ". Local riddles will be unavailable.", e);
			return Collections.emptyList();
		} catch (Exception e) { // Catch-all for other unexpected issues
			Log.e(TAG, "WARN: An unexpected error occurred while loading local riddles. Reason: " + e.getMessage(), e);
			return Collections.emptyList();
		}
	}
}
