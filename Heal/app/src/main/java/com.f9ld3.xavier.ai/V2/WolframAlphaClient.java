package com.f9ld3.xavier.ai.V2;

import android.util.Log; // Using Android's Log

// OkHttp imports
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response; // This is okhttp3.Response
import okhttp3.ResponseBody;

import com.f9ld3.xavier.ai.V2.utils.NetworkStatusChecker;
import com.f9ld3.xavier.ai.V2.utils.SharedHttpClient; // Assuming this now provides an OkHttpClient
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException; // For DocumentBuilder parsing

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException; // For DocumentBuilderFactory
import java.io.IOException; // For OkHttp, URLEncoder, and XML parsing
import java.io.StringReader;
// Removed: java.net.URI;
import java.net.URLEncoder;
// Removed: java.net.http.HttpRequest;
// Removed: java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
// Removed: java.time.Duration;
import java.util.ArrayList; // For Collections.unmodifiableList if needed
import java.util.Arrays;
import java.util.Collections; // For Collections.unmodifiableList
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit; // For OkHttp timeouts
import java.util.stream.Collectors;

/**
 * A resilient client for interacting with the Wolfram|Alpha Full Results API v2.0.
 * It manages multiple AppIDs for fault tolerance and returns a structured result
 * containing both the answer and the API's interpretation of the query.
 */
public final class WolframAlphaClient {

	private static final String TAG = "WolframAlphaClient";
	private static final String API_BASE_URL = "https://api.wolframalpha.com/v2/query";
	private static final long API_TIMEOUT_SECONDS = 15;

	private final List<String> appIds;
	private int currentAppIdIndex = 0;
	private final OkHttpClient httpClient; // Use OkHttpClient

	// For Java 8 compatibility if List.of() is not available/desugared for KNOWLEDGE_QUERY_TRIGGERS
	private static final List<String> KNOWLEDGE_QUERY_TRIGGERS;
	private static final List<String> EXCLUSIONS;

	static {
		List<String> triggers = new ArrayList<>();
		triggers.add("who");
		triggers.add("what");
		triggers.add("when");
		triggers.add("where");
		triggers.add("why");
		triggers.add("how");
		triggers.add("define");
		triggers.add("explain");
		triggers.add("is");
		triggers.add("are");
		triggers.add("was");
		triggers.add("were");
		triggers.add("do");
		triggers.add("does");
		triggers.add("did");
		triggers.add("can");
		triggers.add("could");
		triggers.add("would");
		triggers.add("should");
		KNOWLEDGE_QUERY_TRIGGERS = Collections.unmodifiableList(triggers);

		List<String> exclusions = new ArrayList<>();
		exclusions.add("how are you");
		exclusions.add("how are you doing");
		exclusions.add("how's it going");
		exclusions.add("how do you do");
		EXCLUSIONS = Collections.unmodifiableList(exclusions);
	}
	// If List.of() IS available (Java 9+ features or desugaring active):
	// private static final List<String> KNOWLEDGE_QUERY_TRIGGERS = List.of(
	//     "who", "what", "when", "where", "why", "how", "define", "explain",
	//     "is", "are", "was", "were", "do", "does", "did", "can", "could", "would", "should"
	// );
	// private static final List<String> EXCLUSIONS = List.of(
	//     "how are you", "how are you doing", "how's it going", "how do you do"
	// );


	// Constructor expecting an OkHttpClient (can be from SharedHttpClient.get())
	public WolframAlphaClient(OkHttpClient client, String... appIds) {
		this.httpClient = client;
		if (appIds == null || appIds.length == 0) {
			this.appIds = Collections.emptyList();
			Log.w(TAG, "No WolframAlpha App IDs provided during initialization.");
		} else {
			this.appIds = Arrays.stream(appIds)
					.filter(Objects::nonNull)
					.filter(id -> !id.trim().isEmpty())
					.collect(Collectors.toList());
			if (this.appIds.isEmpty()) {
				Log.w(TAG, "All provided WolframAlpha App IDs were null or empty.");
			}
		}
	}

	// Default constructor that gets OkHttpClient from SharedHttpClient
	// Ensure SharedHttpClient.get() returns an OkHttpClient
	public WolframAlphaClient(String... appIds) {
		this(SharedHttpClient.get(), appIds);
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
		if (EXCLUSIONS.stream().anyMatch(cleanedInput::startsWith)) {
			return false;
		}

		String[] words = cleanedInput.split("\\s+");
		if (words.length > 0 && KNOWLEDGE_QUERY_TRIGGERS.contains(words[0])) {
			return true;
		}

		// Fallback for mathematical expressions that start with a number.
		return !cleanedInput.isEmpty() && Character.isDigit(cleanedInput.charAt(0));
	}

	/**
	 * This method performs network operations and MUST be called from a background thread.
	 */
	public Optional<WolframAlphaResult> getFullResult(String query) {
		if (!NetworkStatusChecker.isOnline()) { // Assuming NetworkStatusChecker is Android context aware
			Log.w(TAG, "WolframAlphaClient: Network is offline. Aborting request.");
			return Optional.empty();
		}
		if (appIds.isEmpty()) {
			// Check for XavierCoreV2.DEBUG_MODE might not be ideal here if this client is used elsewhere.
			// Consider a local debug flag or just log always if it's a warning.
			Log.w(TAG, "WolframAlphaClient: No App IDs configured.");
			return Optional.empty();
		}
		if (query == null || query.isBlank()){
			Log.w(TAG, "WolframAlphaClient: Query is null or blank.");
			return Optional.empty();
		}


		// Try each App ID until one succeeds or all fail
		for (int i = 0; i < appIds.size(); i++) {
			String appId = appIds.get(currentAppIdIndex);
			String currentKeyForLogging = "..." + (appId.length() > 4 ? appId.substring(appId.length() - 4) : appId);

			try {
				String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8.name());
				String requestUrl = String.format("%s?appid=%s&input=%s&output=xml&format=plaintext", API_BASE_URL, appId, encodedQuery);

				Log.d(TAG, "Requesting WolframAlpha with AppID ending " + currentKeyForLogging + ", URL: " + requestUrl);

				Request okHttpRequest = new Request.Builder()
						.url(requestUrl)
						.get() // Default, but explicit
						.build();

				// THIS IS A SYNCHRONOUS (BLOCKING) NETWORK CALL.
				// IT MUST BE EXECUTED ON A BACKGROUND THREAD IN ANDROID.
				try (Response okHttpResponse = httpClient.newCall(okHttpRequest).execute()) {

					if (!okHttpResponse.isSuccessful()) {
						Log.w(TAG, String.format("Wolfram|Alpha API error with AppID ending %s. Status: %d, Query: %s",
								currentKeyForLogging, okHttpResponse.code(), query));
						// Cycle to the next App ID for the next call (not necessarily the next attempt in this loop)
						currentAppIdIndex = (currentAppIdIndex + 1) % appIds.size();
						// Continue to the next key IF this error is considered retryable with a different key
						// For WolframAlpha, a non-200 might mean the key is bad, or the query is malformed for that key type
						// For simplicity here, we try the next key for any non-200.
						if (i < appIds.size() -1) continue; // Try next key if available
						else return Optional.empty(); // All keys tried for this particular error type
					}

					ResponseBody responseBody = okHttpResponse.body();
					if (responseBody == null) {
						Log.w(TAG, "WolframAlpha response body was null for AppID ending " + currentKeyForLogging);
						currentAppIdIndex = (currentAppIdIndex + 1) % appIds.size();
						if (i < appIds.size() -1) continue;
						else return Optional.empty();
					}

					String xmlResponse = responseBody.string();
					return parseFullResult(xmlResponse); // If successful, parse and return

				} // Response is closed here

			} catch (IOException e) { // Covers OkHttp network issues and URLEncoder
				Log.e(TAG, "IOException with WolframAlpha AppID ending " + currentKeyForLogging + " for query: '" + query + "'. Reason: " + e.getMessage(), e);
				// Cycle to the next App ID
				currentAppIdIndex = (currentAppIdIndex + 1) % appIds.size();
				// Continue to try the next key if it was a network-related issue for this key
			} catch (ParserConfigurationException | SAXException e) {
				Log.e(TAG, "XML Parsing error with WolframAlpha response for query: '" + query + "'. Reason: " + e.getMessage(), e);
				// This is a critical error with the response format, probably not key-specific. Stop.
				return Optional.empty();
			} catch (Exception e) { // Catch-all for other unexpected errors during the process for one key
				Log.e(TAG, "Unexpected exception with WolframAlpha AppID ending " + currentKeyForLogging + " while processing query: '" + query + "'. Reason: " + e.getMessage(), e);
				currentAppIdIndex = (currentAppIdIndex + 1) % appIds.size();
			}
		} // End of for loop trying all AppIDs

		Log.e(TAG, "All WolframAlpha AppIDs failed for query: '" + query + "'");
		return Optional.empty(); // All AppIDs failed
	}

	private Optional<WolframAlphaResult> parseFullResult(String xml) throws ParserConfigurationException, IOException, SAXException {
		if (xml == null || xml.isBlank()) {
			Log.w(TAG, "XML for parsing is null or blank.");
			return Optional.empty();
		}
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		// Secure XML processing
		factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
		factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
		factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
		factory.setXIncludeAware(false);
		factory.setExpandEntityReferences(false);

		DocumentBuilder builder = factory.newDocumentBuilder();
		Document doc = builder.parse(new InputSource(new StringReader(xml)));

		Element queryResult = doc.getDocumentElement();
		if (!"true".equals(queryResult.getAttribute("success"))) {
			Log.w(TAG, "WolframAlpha query was not successful according to XML. Error: " + queryResult.getAttribute("error"));
			// You might want to parse the <error> tag here for more details
			return Optional.empty();
		}

		String interpretation = findPodText(doc, "Input interpretation").orElse("");

		// Fallback chain for finding the answer pod
		Optional<String> answerOpt = findPodText(doc, "Result");
		if (!answerOpt.isPresent()) answerOpt = findPodText(doc, "Definition");
		if (!answerOpt.isPresent()) answerOpt = findPodText(doc, "Summary");
		if (!answerOpt.isPresent()) answerOpt = findPodText(doc, "Recipe");
		if (!answerOpt.isPresent()) answerOpt = findPodText(doc, "Instructions");


		if (!answerOpt.isPresent()) {
			// If common pods are not found, iterate through others (excluding Input interpretation)
			NodeList pods = doc.getElementsByTagName("pod");
			for (int i = 0; i < pods.getLength(); i++) {
				Element pod = (Element) pods.item(i);
				String title = pod.getAttribute("title");
				// Skip common ones already checked, and the interpretation pod
				if (!"Input interpretation".equals(title) && !"Result".equals(title) &&
						!"Definition".equals(title) && !"Summary".equals(title) &&
						!"Recipe".equals(title) && !"Instructions".equals(title) &&
						pod.hasChildNodes()) {
					answerOpt = findPodText(doc, title);
					if (answerOpt.isPresent()) break;
				}
			}
		}
		return answerOpt.map(ans -> new WolframAlphaResult(ans, interpretation));
	}


	private Optional<String> findPodText(Document doc, String podTitle) {
		NodeList pods = doc.getElementsByTagName("pod");
		for (int i = 0; i < pods.getLength(); i++) {
			Element pod = (Element) pods.item(i);
			if (podTitle.equals(pod.getAttribute("title"))) {
				NodeList plaintexts = pod.getElementsByTagName("plaintext");
				if (plaintexts.getLength() > 0 && plaintexts.item(0) != null) {
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
