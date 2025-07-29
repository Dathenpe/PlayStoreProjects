package com.f9ld3.xavier.ai.V2;

import com.f9ld3.xavier.ai.V2.utils.SharedHttpClient;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * A resilient client for interacting with the Wolfram|Alpha Full Results API v2.0.
 * It manages multiple AppIDs for fault tolerance, and parses the XML response
 * to extract a short, definitive answer.
 */
public final class WolframAlphaClient {

private static final String API_BASE_URL = "https://api.wolframalpha.com/v2/query";
private final List<String> appIds;
private int currentAppIdIndex = 0;

/**
 * Initializes the client with a variable number of AppIDs.
 * @param appIds An array of Wolfram|Alpha AppIDs.
 */
public WolframAlphaClient(String... appIds) {
	// Create a list of AppIDs, filtering out any that are null or empty.
	this.appIds = Arrays.stream(appIds)
			              .filter(Objects::nonNull)
			              .filter(id -> !id.trim().isEmpty())
			              .collect(Collectors.toList());
}

/**
 * A simple heuristic to quickly check if a query is suitable for Wolfram|Alpha.
 * This is used as a pre-classifier to avoid sending conversational text and making
 * unnecessary API calls.
 *
 * @param userInput The user's question.
 * @return true if the query is likely a factual question, false otherwise.
 */
public boolean canAnswer(String userInput) {
	if (userInput == null || userInput.trim().length() < 5) {
		return false; // Too short to be a meaningful question
	}
	String[] words = userInput.trim().split("\\s+");
	if (words.length < 2) {
		return false; // Unlikely to be a factual question
	}
	// Check if it starts with a common question word.
	String firstWord = words[0].toLowerCase();
	return firstWord.equals("who") || firstWord.equals("what") || firstWord.equals("when") ||
			       firstWord.equals("where") || firstWord.equals("why") || firstWord.equals("how") ||
			       Character.isDigit(firstWord.charAt(0)); // Also good for calculations
}

/**
 * Queries the Wolfram|Alpha API and attempts to find a direct answer from the XML response.
 *
 * @param query The user's question (e.g., "who is the richest man in nigeria").
 * @return An Optional containing the answer string, or empty if no answer is found.
 */
public Optional<String> getShortAnswer(String query) {
	if (appIds.isEmpty()) {
		if (XavierCoreV2.DEBUG_MODE) System.err.println("[DEBUG] WolframAlphaClient: No App IDs configured.");
		return Optional.empty();
	}
	
	// Cycle through API keys if one fails.
	String appId = appIds.get(currentAppIdIndex);
	
	try {
		String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
		// Use the v2.0 API and request a plaintext-formatted XML response
		String requestUrl = String.format("%s?appid=%s&input=%s&output=xml&format=plaintext", API_BASE_URL, appId, encodedQuery);
		
		HttpRequest request = HttpRequest.newBuilder()
				                      .uri(URI.create(requestUrl))
				                      .timeout(Duration.ofSeconds(15))
				                      .GET()
				                      .build();
		
		// Assumes a SharedHttpClient exists for connection pooling, a good practice.
		HttpResponse<String> response = SharedHttpClient.get().send(request, HttpResponse.BodyHandlers.ofString());
		
		if (response.statusCode() != 200) {
			System.err.printf("Wolfram|Alpha API error. Status: %d, Query: %s%n", response.statusCode(), query);
			// Rotate key on failure
			currentAppIdIndex = (currentAppIdIndex + 1) % appIds.size();
			return Optional.empty();
		}
		
		return parseXMLResponse(response.body());
		
	} catch (Exception e) {
		System.err.println("Exception in WolframAlphaClient: " + e.getMessage());
		return Optional.empty();
	}
}

/**
 * Parses the XML response from the API to find the most relevant answer.
 */
private Optional<String> parseXMLResponse(String xml) throws Exception {
	DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	// Prevent XXE (XML External Entity) attacks for security
	factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
	DocumentBuilder builder = factory.newDocumentBuilder();
	Document doc = builder.parse(new InputSource(new StringReader(xml)));
	
	Element queryResult = doc.getDocumentElement();
	if (!"true".equals(queryResult.getAttribute("success"))) {
		// --- NEW: Log the specific error from the API ---
		if ("true".equals(queryResult.getAttribute("error"))) {
			NodeList errors = doc.getElementsByTagName("error");
			if (errors.getLength() > 0) {
				Element error = (Element) errors.item(0);
				String errorMsg = error.getElementsByTagName("msg").item(0).getTextContent();
				// Always print this error, even if not in DEBUG_MODE, as it's critical for diagnostics.
				System.err.println("[ERROR] Wolfram|Alpha API Error: " + errorMsg);
			}
		}
		return Optional.empty(); // Query was not successful
	}
	
	// Strategy 1: Find the "Result" pod first, as it's the most likely direct answer.
	Optional<String> result = findPodText(doc, "Result");
	if (result.isPresent()) {
		return result;
	}
	
	// Strategy 2: If no "Result" pod, find the first pod after "Input interpretation".
	// This is often the primary definition or data.
	NodeList pods = doc.getElementsByTagName("pod");
	if (pods.getLength() > 1) {
		Element secondPod = (Element) pods.item(1); // Item 0 is usually "Input interpretation"
		NodeList plaintexts = secondPod.getElementsByTagName("plaintext");
		if (plaintexts.getLength() > 0) {
			String text = plaintexts.item(0).getTextContent();
			if (text != null && !text.isBlank()) {
				return Optional.of(text.trim());
			}
		}
	}
	
	return Optional.empty(); // No suitable answer found
}

/**
 * Helper method to find the text content of a specific pod by its title.
 */
private Optional<String> findPodText(Document doc, String podTitle) {
	NodeList pods = doc.getElementsByTagName("pod");
	for (int i = 0; i < pods.getLength(); i++) {
		Element pod = (Element) pods.item(i);
		if (podTitle.equals(pod.getAttribute("title"))) {
			NodeList plaintexts = pod.getElementsByTagName("plaintext");
			if (plaintexts.getLength() > 0) {
				String text = plaintexts.item(0).getTextContent();
				if (text != null && !text.isBlank()) {
					return Optional.of(text.trim());
				}
			}
		}
	}
	return Optional.empty();
}
}