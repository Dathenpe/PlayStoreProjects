// C:/Users/Music_Minister/Desktop/PlayStore/PlayStoreProjects/Xavier/XavierAI/src/main/java/com/f9ld3/xavier/ai/V2/WolframAlphaClient.java

package com.f9ld3.xavier.ai.V2;

import com.f9ld3.xavier.ai.V2.utils.NetworkStatusChecker;
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
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * A resilient client for interacting with the Wolfram|Alpha Full Results API v2.0.
 * It manages multiple AppIDs for fault tolerance and returns a structured result
 * containing both the answer and the API's interpretation of the query.
 */
public final class WolframAlphaClient {

private static final String API_BASE_URL = "https://api.wolframalpha.com/v2/query";
private final List<String> appIds;
private int currentAppIdIndex = 0;

// --- UPDATED: A more comprehensive list of words that start factual questions ---
private static final List<String> KNOWLEDGE_QUERY_TRIGGERS = List.of(
		"who", "what", "when", "where", "why", "how", "define", "explain",
		"is", "are", "was", "were", "do", "does", "did", "can", "could", "would", "should"
);

public WolframAlphaClient(String... appIds) {
	this.appIds = Arrays.stream(appIds)
			              .filter(Objects::nonNull)
			              .filter(id -> !id.trim().isEmpty())
			              .collect(Collectors.toList());
}

/**
 * A more intelligent heuristic to quickly check if a query is suitable for Wolfram|Alpha.
 * This is used as a pre-classifier in the core pipeline's fallback step.
 */
public boolean canAnswer(String userInput) {
	if (userInput == null || userInput.trim().length() < 3) {
		return false;
	}
	String cleanedInput = userInput.trim().toLowerCase();
	
	// This prevents the client from hijacking simple greetings or commands.
	List<String> exclusions = List.of("how are you", "how are you doing", "how's it going", "how do you do");
	if (exclusions.stream().anyMatch(cleanedInput::startsWith)) {
		return false;
	}
	
	String[] words = cleanedInput.split("\\s+");
	if (words.length > 0 && KNOWLEDGE_QUERY_TRIGGERS.contains(words[0])) {
		return true;
	}
	
	// Fallback for mathematical expressions that start with a number.
	return Character.isDigit(cleanedInput.charAt(0));
}

public Optional<WolframAlphaResult> getFullResult(String query) {
	
	if (!NetworkStatusChecker.isOnline()) {
		System.err.println("WolframAlphaClient: Network is offline. Aborting request.");
		return Optional.empty();
	}
	if (appIds.isEmpty()) {
		if (XavierCoreV2.DEBUG_MODE) System.err.println("[DEBUG] WolframAlphaClient: No App IDs configured.");
		return Optional.empty();
	}
	
	String appId = appIds.get(currentAppIdIndex);
	
	try {
		String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
		String requestUrl = String.format("%s?appid=%s&input=%s&output=xml&format=plaintext", API_BASE_URL, appId, encodedQuery);
		
		HttpRequest request = HttpRequest.newBuilder()
				                      .uri(URI.create(requestUrl))
				                      .timeout(Duration.ofSeconds(15))
				                      .GET()
				                      .build();
		
		HttpResponse<String> response = SharedHttpClient.get().send(request, HttpResponse.BodyHandlers.ofString());
		
		if (response.statusCode() != 200) {
			System.err.printf("Wolfram|Alpha API error. Status: %d, Query: %s%n", response.statusCode(), query);
			currentAppIdIndex = (currentAppIdIndex + 1) % appIds.size();
			return Optional.empty();
		}
		
		return parseFullResult(response.body());
		
	} catch (Exception e) {
		// --- UPDATED: More user-friendly error logging ---
		// Check for common, non-critical network errors and log a cleaner message.
		if (e instanceof java.net.ConnectException || e.getCause() instanceof java.nio.channels.UnresolvedAddressException) {
			System.err.println("WARN: Could not connect to Wolfram|Alpha. This may be a network or DNS issue. Message: " + e.getMessage());
		} else {
			// For other, unexpected exceptions, print the full trace for debugging.
			System.err.println("Exception in WolframAlphaClient while processing query: '" + query + "'");
			e.printStackTrace();
		}
		return Optional.empty();
	}
}

private Optional<WolframAlphaResult> parseFullResult(String xml) throws Exception {
	DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
	DocumentBuilder builder = factory.newDocumentBuilder();
	Document doc = builder.parse(new InputSource(new StringReader(xml)));
	
	Element queryResult = doc.getDocumentElement();
	if (!"true".equals(queryResult.getAttribute("success"))) {
		// ... (error handling) ...
		return Optional.empty();
	}
	
	String interpretation = findPodText(doc, "Input interpretation").orElse("");
	
	Optional<String> answer = findPodText(doc, "Result")
			                          .or(() -> findPodText(doc, "Definition"))
			                          .or(() -> findPodText(doc, "Summary"))
			                          .or(() -> findPodText(doc, "Recipe"))
			                          .or(() -> findPodText(doc, "Instructions"));
	
	if (answer.isEmpty()) {
		NodeList pods = doc.getElementsByTagName("pod");
		for (int i = 0; i < pods.getLength(); i++) {
			Element pod = (Element) pods.item(i);
			String title = pod.getAttribute("title");
			if (!"Input interpretation".equals(title) && pod.hasChildNodes()) {
				answer = findPodText(doc, title);
				if (answer.isPresent()) break;
			}
		}
	}
	
	return answer.map(ans -> new WolframAlphaResult(ans, interpretation));
}

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