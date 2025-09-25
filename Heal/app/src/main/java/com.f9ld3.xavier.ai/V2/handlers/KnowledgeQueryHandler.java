package com.f9ld3.xavier.ai.V2.handlers;

import android.util.Log;

import com.f9ld3.xavier.ai.V2.ConversationContext;
import com.f9ld3.xavier.ai.V2.WolframAlphaClient;
import com.f9ld3.xavier.ai.V2.WolframAlphaResult;
import com.f9ld3.xavier.ai.V2.XavierCoreV2;
import com.f9ld3.xavier.ai.V2.services.SearchService;
import com.f9ld3.xavier.ai.V2.services.SearchService.FullSearchResult; // Import new class
import com.f9ld3.xavier.ai.V2.services.SearchService.Snippet; // Import new class
import com.f9ld3.xavier.ai.V2.services.SearchService.SearchServiceException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class KnowledgeQueryHandler implements IntentHandler {

	private static final String TAG = "KnowledgeQueryHandler";
	private final WolframAlphaClient wolframClient;
	private final SearchService searchService;

	private static final Pattern SUBJECT_PATTERN = Pattern.compile("^([^,(]+)");
	private static final List<String> PREFIXES_TO_REMOVE;

	static {
		List<String> prefixes = new ArrayList<>(Arrays.asList(
				"tell me about", "can you tell me about", "do you know about",
				"give me information on", "information about", "search for",
				"look up", "find out about", "what do you know about",
				"tell me", "explain", "what is", "what's", "who is", "who's"
		));
		prefixes.sort(Comparator.comparingInt(String::length).reversed());
		PREFIXES_TO_REMOVE = Collections.unmodifiableList(prefixes);
	}

	public KnowledgeQueryHandler(WolframAlphaClient wolframClient, SearchService searchService) {
		if (wolframClient == null) {
			throw new IllegalArgumentException("WolframAlphaClient cannot be null.");
		}
		if (searchService == null) {
			throw new IllegalArgumentException("SearchService cannot be null.");
		}
		this.wolframClient = wolframClient;
		this.searchService = searchService;
	}

	@Override
	public String handle(String userInput, ConversationContext context) {
		try {
			if (userInput == null || context == null) {
				Log.w(TAG, "User input or context is null.");
				return "I'm sorry, something went wrong.";
			}

			String queryToSend = extractQuery(userInput);
			if (queryToSend.isBlank()) {
				Log.d(TAG, "Extracted query is blank. Original input: " + userInput);
			}

			if (XavierCoreV2.DEBUG_MODE) {
				Log.d(TAG, String.format("KnowledgeQueryHandler: Original input: '%s'", userInput));
				Log.d(TAG, String.format("KnowledgeQueryHandler: Sending cleaned query to Wolfram|Alpha: '%s'", queryToSend));
			}

			// --- PRIMARY STRATEGY: Wolfram|Alpha ---
			Optional<WolframAlphaResult> resultOpt = wolframClient.getFullResult(queryToSend);

			if (resultOpt.isPresent()) {
				WolframAlphaResult result = resultOpt.get();
				String answer = result.getAnswer() != null ? result.getAnswer() : "";
				String interpretation = result.getInterpretation() != null ? result.getInterpretation() : "";
				String subject = extractSubjectFromAnswer(answer);
				context.addEntityToCurrentContext("subject", subject);

				StringBuilder responseBuilder = new StringBuilder();
				if (!interpretation.isEmpty() && !interpretation.equalsIgnoreCase(queryToSend)) {
					String formattedInterpretation = formatInterpretation(interpretation);
					responseBuilder.append(String.format("Assuming you meant '%s':\n", formattedInterpretation));
				}
				String cleanedAnswer = answer.replace(" | ", ": ").replace("... | ", ". ");
				responseBuilder.append(cleanedAnswer);
				responseBuilder.append("\n(Source: Wolfram|Alpha)");

				return responseBuilder.toString();
			} else {
				// --- FALLBACK STRATEGY: Web Search (using full structured result) ---
				Log.d(TAG, "KnowledgeQueryHandler: Wolfram|Alpha failed. Falling back to web search for: " + queryToSend);
				try {
					// Use the new getFullSearchResult method
					Optional<FullSearchResult> fullSearchResultOpt = searchService.getFullSearchResult(userInput);

					if (fullSearchResultOpt.isPresent()) {
						FullSearchResult fullResult = fullSearchResultOpt.get();

						// 1. Prioritize Answer Box (most direct answer)
						if (fullResult.answerBox != null && fullResult.answerBox.snippet != null) {
							String answer = fullResult.answerBox.snippet;
							context.addEntityToCurrentContext("subject", fullResult.answerBox.title);
							return String.format("%s\n\n(Source: %s)", answer, fullResult.answerBox.title);
						}

						// 2. Fallback to Knowledge Graph (structured detail)
						if (fullResult.knowledgeGraph != null && fullResult.knowledgeGraph.description != null) {
							String answer = fullResult.knowledgeGraph.description;
							context.addEntityToCurrentContext("subject", fullResult.knowledgeGraph.title);

							StringBuilder kbResponse = new StringBuilder(answer);
							if (fullResult.knowledgeGraph.attributes != null && !fullResult.knowledgeGraph.attributes.isEmpty()) {
								kbResponse.append("\n\nKey Facts:");
								fullResult.knowledgeGraph.attributes.forEach((key, value) ->
										kbResponse.append(String.format("\n- %s: %s", key, value))
								);
							}
							return kbResponse.toString();
						}

						// 3. Fallback to first organic result (simple snippet)
						if (fullResult.organic != null && !fullResult.organic.isEmpty()) {
							Snippet firstOrganic = fullResult.organic.get(0);
							String answer = firstOrganic.snippet;
							context.addEntityToCurrentContext("subject", firstOrganic.title);
							return String.format(
									"%s... (Read more: %s)",
									answer,
									firstOrganic.link
							);
						}
					}

				} catch (SearchServiceException e) {
					Log.e(TAG, "KnowledgeQueryHandler fallback search failed. Reason: " + e.getMessage(), e);
				}
				// --- FINAL FALLBACK ---
				return "That's a great question, but I couldn't find a specific answer for it at the moment.";
			}
		} finally {
			if (context != null) {
				Log.d(TAG, "KnowledgeQueryHandler finished. Popping context.");
				context.popContext();
			}
		}
	}

	// ... (helper methods remain the same) ...
	public String extractQuery(String userInput) {
		if (userInput == null) return "";

		String query = userInput.toLowerCase().replaceAll("\\?$", "").trim();

		if (query.startsWith("xavier ")) {
			query = query.substring("xavier ".length()).trim();
		}

		for (String prefix : PREFIXES_TO_REMOVE) {
			if (query.startsWith(prefix + " ")) {
				return query.substring(prefix.length()).trim();
			}
		}
		return query;
	}

	private String extractSubjectFromAnswer(String answer) {
		if (answer == null || answer.isBlank()) {
			return "";
		}
		Matcher matcher = SUBJECT_PATTERN.matcher(answer);
		if (matcher.find()) {
			return matcher.group(1).trim();
		}
		return answer.trim();
	}

	private String formatInterpretation(String interpretation) {
		if (interpretation == null || interpretation.isBlank()) {
			return "";
		}
		String[] parts = interpretation.split("\\s*\\|\\s*");
		if (parts.length == 2) {
			String topic = parts[0].trim();
			String property = parts[1].trim();
			if (!topic.isEmpty()) {
				topic = topic.substring(0, 1).toUpperCase() + topic.substring(1);
			}
			if (property.endsWith("s")) {
				return String.format("the %s in %s", property, topic);
			}
			return String.format("the %s of %s", property, topic);
		}
		return interpretation;
	}
}