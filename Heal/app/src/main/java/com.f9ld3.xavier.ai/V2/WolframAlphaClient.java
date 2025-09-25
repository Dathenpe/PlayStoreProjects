package com.f9ld3.xavier.ai.V2;

import android.content.Context;
import android.util.Log;

import com.f9ld3.xavier.ai.V2.utils.NetworkStatusChecker;
import com.f9ld3.xavier.ai.V2.utils.SharedHttpClient;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.StringReader;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public final class WolframAlphaClient {

	private static final String TAG = "WolframAlphaClient";
	private static final String API_BASE_URL = "https://api.wolframalpha.com/v2/query";
	private static final long API_TIMEOUT_SECONDS = 15;

	private final List<String> appIds;
	private int currentAppIdIndex = 0;
	private final OkHttpClient httpClient;
	private final Context context;

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

	public WolframAlphaClient(Context context, OkHttpClient client, String... appIds) {
		this.context = context;
		this.httpClient = client;
		if (appIds == null || appIds.length == 0) {
			this.appIds = Collections.emptyList();
			Log.w(TAG, "No WolframAlpha App IDs provided.");
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

	public WolframAlphaClient(Context context, String... appIds) {
		this(context, SharedHttpClient.get(), appIds);
	}


	public boolean canAnswer(String userInput) {
		if (userInput == null || userInput.trim().length() < 3) {
			return false;
		}
		String cleanedInput = userInput.trim().toLowerCase();

		if (EXCLUSIONS.stream().anyMatch(cleanedInput::startsWith)) {
			return false;
		}

		String[] words = cleanedInput.split("\\s+");
		if (words.length > 0 && KNOWLEDGE_QUERY_TRIGGERS.contains(words[0])) {
			return true;
		}

		return !cleanedInput.isEmpty() && Character.isDigit(cleanedInput.charAt(0));
	}

	public Optional<WolframAlphaResult> getFullResult(String query) {
		if (!NetworkStatusChecker.isOnline(this.context)) {
			Log.w(TAG, "WolframAlphaClient: Network is offline. Aborting request.");
			return Optional.empty();
		}
		if (appIds.isEmpty()) {
			Log.w(TAG, "WolframAlphaClient: No App IDs configured.");
			return Optional.empty();
		}
		if (query == null || query.isBlank()) {
			Log.w(TAG, "WolframAlphaClient: Query is null or blank.");
			return Optional.empty();
		}

		for (int i = 0; i < appIds.size(); i++) {
			String appId = appIds.get(currentAppIdIndex);
			String currentKeyForLogging = "..." + (appId.length() > 4 ? appId.substring(appId.length() - 4) : appId);

			try {
				String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8.name());
				String requestUrl = String.format("%s?appid=%s&input=%s&output=xml&format=plaintext", API_BASE_URL, appId, encodedQuery);

				Log.d(TAG, "Requesting WolframAlpha with AppID ending " + currentKeyForLogging + ", URL: " + requestUrl);

				Request okHttpRequest = new Request.Builder()
						.url(requestUrl)
						.get()
						.build();

				try (Response okHttpResponse = httpClient.newCall(okHttpRequest).execute()) {

					if (!okHttpResponse.isSuccessful()) {
						Log.w(TAG, String.format("Wolfram|Alpha API error with AppID ending %s. Status: %d, Query: %s",
								currentKeyForLogging, okHttpResponse.code(), query));
						currentAppIdIndex = (currentAppIdIndex + 1) % appIds.size();
						if (i < appIds.size() -1) continue;
						else return Optional.empty();
					}

					ResponseBody responseBody = okHttpResponse.body();
					if (responseBody == null) {
						Log.w(TAG, "WolframAlpha response body was null for AppID ending " + currentKeyForLogging);
						currentAppIdIndex = (currentAppIdIndex + 1) % appIds.size();
						if (i < appIds.size() -1) continue;
						else return Optional.empty();
					}

					String xmlResponse = responseBody.string();
					return parseFullResult(xmlResponse);

				}

			} catch (IOException e) {
				Log.e(TAG, "IOException with WolframAlpha AppID ending " + currentKeyForLogging + " for query: '" + query + "'. Reason: " + e.getMessage(), e);
				currentAppIdIndex = (currentAppIdIndex + 1) % appIds.size();
			} catch (ParserConfigurationException | SAXException e) {
				Log.e(TAG, "XML Parsing error with WolframAlpha response for query: '" + query + "'. Reason: " + e.getMessage(), e);
				return Optional.empty();
			} catch (Exception e) {
				Log.e(TAG, "Unexpected exception with WolframAlpha AppID ending " + currentKeyForLogging + " while processing query: '" + query + "'. Reason: " + e.getMessage(), e);
				currentAppIdIndex = (currentAppIdIndex + 1) % appIds.size();
			}
		}

		Log.e(TAG, "All WolframAlpha AppIDs failed for query: '" + query + "'");
		return Optional.empty();
	}

	private Optional<WolframAlphaResult> parseFullResult(String xml) throws ParserConfigurationException, IOException, SAXException {
		if (xml == null || xml.isBlank()) {
			Log.w(TAG, "XML for parsing is null or blank.");
			return Optional.empty();
		}
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

		// --- CRITICAL FIXES FOR PARSING INCONSISTENCIES ---

		// 1. Wrap FEATURE_SECURE_PROCESSING
		try {
			// Standard way to prevent DTD and external entity processing for security
			factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
		} catch (ParserConfigurationException e) {
			Log.w(TAG, "Could not set FEATURE_SECURE_PROCESSING on DocumentBuilderFactory. Continuing.", e);
		}

		// 2. Wrap setXIncludeAware (This is the line that caused the 'UnsupportedOperationException' in the logs)
		try {
			factory.setXIncludeAware(false);
		} catch (UnsupportedOperationException e) {
			Log.w(TAG, "Parser does not support setXIncludeAware. Continuing without it.", e);
		}

		// 3. Wrap setExpandEntityReferences
		try {
			factory.setExpandEntityReferences(false);
		} catch (UnsupportedOperationException e) {
			Log.w(TAG, "Parser does not support setExpandEntityReferences. Continuing without it.", e);
		}
		// --- END CRITICAL FIXES ---

		DocumentBuilder builder = factory.newDocumentBuilder();
		Document doc = builder.parse(new InputSource(new StringReader(xml)));

		Element queryResult = doc.getDocumentElement();
		if (!"true".equals(queryResult.getAttribute("success"))) {
			Log.w(TAG, "WolframAlpha query was not successful according to XML. Error: " + queryResult.getAttribute("error"));
			return Optional.empty();
		}

		String interpretation = findPodText(doc, "Input interpretation").orElse("");

		Optional<String> answerOpt = findPodText(doc, "Result");
		if (!answerOpt.isPresent()) answerOpt = findPodText(doc, "Definition");
		if (!answerOpt.isPresent()) answerOpt = findPodText(doc, "Summary");
		if (!answerOpt.isPresent()) answerOpt = findPodText(doc, "Recipe");
		if (!answerOpt.isPresent()) answerOpt = findPodText(doc, "Instructions");


		if (!answerOpt.isPresent()) {
			NodeList pods = doc.getElementsByTagName("pod");
			for (int i = 0; i < pods.getLength(); i++) {
				Element pod = (Element) pods.item(i);
				String title = pod.getAttribute("title");
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
		NodeList pods = doc.getDocumentElement().getElementsByTagName("pod");
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