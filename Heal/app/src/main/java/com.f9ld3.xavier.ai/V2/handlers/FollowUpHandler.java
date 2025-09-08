package com.f9ld3.xavier.ai.V2.handlers;

import android.util.Log;

import com.f9ld3.xavier.ai.V2.ConversationContext;
import com.f9ld3.xavier.ai.V2.XavierCoreV2;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Handles follow-up questions by intelligently resolving pronouns and combining
 * the user's new query with the last known entities from the conversation context.
 */
public class FollowUpHandler implements IntentHandler {

	private static final String TAG = "FollowUpHandler";
	private final XavierCoreV2 core;

	// MODIFIED: Replaced Map.of() with a static initializer block for broader compatibility
	private static final Map<String, String> PRONOUN_REPLACEMENTS;

	static {
		Map<String, String> tempMap = new HashMap<>();
		tempMap.put("his", "'s");
		tempMap.put("her", "'s");
		tempMap.put("its", "'s");
		tempMap.put("their", "'s");
		tempMap.put("he", "");
		tempMap.put("she", "");
		tempMap.put("it", "");
		tempMap.put("they", "");
		tempMap.put("him", "");
		tempMap.put("them", "");
		PRONOUN_REPLACEMENTS = Collections.unmodifiableMap(tempMap);
	}

	public FollowUpHandler(XavierCoreV2 core) {
		this.core = core;
	}

	@Override
	public String handle(String userInput, ConversationContext context) {
		// Get the primary subject from the current context's entities.
		Optional<Object> lastSubjectOpt = context.getEntityFromCurrentContext("subject");

		// MODIFIED: Replaced instanceof pattern matching
		String lastSubject;
		if (lastSubjectOpt.isPresent() && lastSubjectOpt.get() instanceof String) {
			lastSubject = (String) lastSubjectOpt.get();
			if (lastSubject.isBlank()) {
				Log.d(TAG, "Last subject was present but blank.");
				return "I'm sorry, I've lost the context. What were we talking about?";
			}
		} else {
			Log.d(TAG, "Last subject not found in context or not a String.");
			return "I'm sorry, I've lost the context. What were we talking about?";
		}

		String newQuery = userInput;
		boolean pronounFound = false;

		// Using case-insensitive matching for "there"
		if (userInput.toLowerCase().matches(".*\\bthere\\b.*")) {
			// ReplaceAll should also be case-insensitive if that's the intent
			newQuery = userInput.replaceAll("(?i)\\bthere\\b", "in " + lastSubject);
			pronounFound = true;
		} else {
			for (Map.Entry<String, String> entry : PRONOUN_REPLACEMENTS.entrySet()) {
				String pronoun = entry.getKey();
				// Ensure pronoun matching is case-insensitive and whole word
				if (userInput.toLowerCase().matches(".*\\b" + Pattern.quote(pronoun.toLowerCase()) + "\\b.*")) {
					String replacementText = lastSubject + entry.getValue();
					// ReplaceAll should also be case-insensitive for the pronoun
					newQuery = userInput.replaceAll("(?i)\\b" + Pattern.quote(pronoun) + "\\b", replacementText);
					pronounFound = true;
					break;
				}
			}
		}

		if (!pronounFound) {
			// If no pronoun is found, assume the user is asking about the last subject.
			// e.g., "Tell me about France." -> "And the capital city?" -> "capital city of France"
			newQuery = userInput + " of " + lastSubject;
		}

		// MODIFIED: Replaced System.out.printf with Log.d
		if (XavierCoreV2.DEBUG_MODE) { // Assuming DEBUG_MODE is a public static final boolean
			Log.d(TAG, String.format("FollowUpHandler: Last subject: '%s'", lastSubject));
			Log.d(TAG, String.format("FollowUpHandler: Original input: '%s'", userInput));
			Log.d(TAG, String.format("FollowUpHandler: Rerouting with new query: '%s'", newQuery));
		}

		// Re-route the new, complete query.
		// We don't create a new context, but instead let the core decide if this is an interruption.
		return core.getResponse(newQuery, context);
	}
}
