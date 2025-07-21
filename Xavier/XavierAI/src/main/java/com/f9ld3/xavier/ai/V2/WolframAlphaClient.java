package com.f9ld3.xavier.ai.V2;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * A resilient client for interacting with Wolfram|Alpha APIs.
 * It manages primary and backup AppIDs to provide fault tolerance.
 */
public final class WolframAlphaClient {

private static final String RECOGNIZER_API_URL = "https://api.wolframalpha.com/v1/queryrecognizer";
private static final String SHORT_ANSWERS_API_URL = "https://api.wolframalpha.com/v1/result";

private final List<String> appIds;

public WolframAlphaClient(String primaryAppId, String backupAppId) {
	// Create a list of AppIDs, filtering out any that are null or empty.
	this.appIds = Arrays.asList(primaryAppId, backupAppId)
			              .stream()
			              .filter(Objects::nonNull)
			              .filter(id -> !id.trim().isEmpty())
			              .collect(Collectors.toList());
}

/**
 * Checks if Wolfram|Alpha can likely answer the query using the Recognizer API.
 * It will try the primary AppID first, then the backup if the primary fails.
 *
 * @param userInput The user's question.
 * @return true if the query is recognized, false otherwise.
 */
public boolean canAnswer(String userInput) {
	if (appIds.isEmpty() || userInput == null || userInput.trim().isEmpty()) {
		return false;
	}
	String encodedInput;
	try {
		encodedInput = URLEncoder.encode(userInput, StandardCharsets.UTF_8.toString());
	} catch (Exception e) {
		return false;
	}
	
	// Try each AppID in order until one succeeds.
	for (String appId : appIds) {
		try {
			String requestUrl = String.format("%s?i=%s&mode=Default&appid=%s", RECOGNIZER_API_URL, encodedInput, appId);
			HttpURLConnection conn = (HttpURLConnection) new URL(requestUrl).openConnection();
			conn.setRequestMethod("GET");
			conn.setConnectTimeout(2000); // Short timeout for a quick check
			conn.setReadTimeout(2000);
			
			if (conn.getResponseCode() == 200) {
				conn.disconnect();
				return true; // Success!
			}
			conn.disconnect();
		} catch (Exception e) {
			System.err.printf("[DEBUG] Wolfram|Alpha Recognizer failed for AppID %s... Trying next.%n", appId.substring(0, 6));
		}
	}
	return false; // All AppIDs failed.
}

/**
 * Gets a short answer for a query from the Short Answers API.
 * It will try the primary AppID first, then the backup if the primary fails.
 *
 * @param userInput The user's question.
 * @return The string response from the API, or an error message.
 */
public String getShortAnswer(String userInput) {
	if (appIds.isEmpty()) {
		return "I'm sorry, my knowledge base service is not configured correctly.";
	}
	String encodedInput;
	try {
		encodedInput = URLEncoder.encode(userInput, StandardCharsets.UTF_8.toString());
	} catch (Exception e) {
		return "I had trouble understanding that input format.";
	}
	
	// Try each AppID in order until one succeeds.
	for (String appId : appIds) {
		try {
			String requestUrl = String.format("%s?i=%s&appid=%s", SHORT_ANSWERS_API_URL, encodedInput, appId);
			HttpURLConnection conn = (HttpURLConnection) new URL(requestUrl).openConnection();
			conn.setRequestMethod("GET");
			
			int responseCode = conn.getResponseCode();
			if (responseCode == 200) {
				StringBuilder response = new StringBuilder();
				try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
					String line;
					while ((line = reader.readLine()) != null) {
						response.append(line);
					}
				}
				conn.disconnect();
				return response.toString(); // Success!
			} else if (responseCode == 501) {
				conn.disconnect();
				return "That's a great question, but I couldn't find a specific answer for it.";
			}
			// For other errors (like 401 Unauthorized), we'll let it loop and try the next key.
			conn.disconnect();
			
		} catch (Exception e) {
			System.err.printf("[DEBUG] Wolfram|Alpha Short Answer failed for AppID %s... Trying next.%n", appId.substring(0, 6));
		}
	}
	
	// If all keys failed.
	return "I'm sorry, I'm having trouble connecting to my knowledge base right now. Please try again later.";
}
}