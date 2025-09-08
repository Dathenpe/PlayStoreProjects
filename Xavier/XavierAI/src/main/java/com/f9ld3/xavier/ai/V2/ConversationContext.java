// C:/Users/Music_Minister/Desktop/PlayStore/PlayStoreProjects/Xavier/XavierAI/src/main/java/com/f9ld3/xavier/ai/V2/ConversationContext.java
package com.f9ld3.xavier.ai.V2;

import com.f9ld3.xavier.ai.V2.services.SearchService;

import java.util.*;

/**
 * Manages the state and memory of a conversation with the user.
 * ENHANCED: Now uses a context stack to handle interruptions and nested conversations.
 * Also tracks sentiment and a more robust entity map.
 */
public class ConversationContext {

/**
 * Represents a single layer of conversation, like an active riddle or a follow-up prompt.
 */
private static class SubContext {
	private final String intent;
	private final Map<String, Object> entities = new HashMap<>();
	
	SubContext(String intent) {
		this.intent = intent;
	}
	
	String getIntent() { return intent; }
	Map<String, Object> getEntities() { return entities; }
	void addEntity(String key, Object value) { entities.put(key, value); }
	Object getEntity(String key) { return entities.get(key); }
}

private String username;
private String lastUserInput;
private double lastSentimentScore; // NEW: For tracking user sentiment

// The core of the new architecture: a stack for managing conversation states.
private final Deque<SubContext> contextStack = new ArrayDeque<>();

// --- Core Context Management ---

/**
 * Pushes a new conversational state onto the stack.
 * @param intent The intent of the new context (e.g., "riddle_query").
 */
public void pushContext(String intent) {
	contextStack.push(new SubContext(intent));
}

/**
 * Pops the current conversational state off the stack, returning to the previous state.
 */
public void popContext() {
	if (!contextStack.isEmpty()) {
		contextStack.pop();
	}
}

/**
 * Gets the current active intent without removing it from the stack.
 * @return An Optional containing the current intent string.
 */
public Optional<String> getCurrentIntent() {
	if (contextStack.isEmpty()) {
		return Optional.empty();
	}
	return Optional.of(contextStack.peek().getIntent());
}

/**
 * Adds an entity (like a subject or an answer) to the current conversational state.
 * @param key The name of the entity (e.g., "subject").
 * @param value The value of the entity (e.g., "Arnold Schwarzenegger").
 */
public void addEntityToCurrentContext(String key, Object value) {
	if (!contextStack.isEmpty()) {
		contextStack.peek().addEntity(key, value);
	}
}

/**
 * Retrieves all entities from the current conversational state.
 * @return A map of entities, or an empty map if no context is active.
 */
public Map<String, Object> getCurrentEntities() {
	if (contextStack.isEmpty()) {
		return Collections.emptyMap();
	}
	return contextStack.peek().getEntities();
}

/**
 * Retrieves a specific entity from the current conversational state.
 * @param key The name of the entity to retrieve.
 * @return An Optional containing the entity's value.
 */
public Optional<Object> getEntityFromCurrentContext(String key) {
	if (contextStack.isEmpty()) {
		return Optional.empty();
	}
	return Optional.ofNullable(contextStack.peek().getEntity(key));
}

// --- Getters and Setters for general conversation properties ---

public String getUsername() { return username; }
public void setUsername(String username) { this.username = username; }

public String getLastUserInput() { return lastUserInput; }
public void setLastUserInput(String lastUserInput) { this.lastUserInput = lastUserInput; }

public double getLastSentimentScore() { return lastSentimentScore; }
public void setLastSentimentScore(double score) { this.lastSentimentScore = score; }

/**
 * Clears all stacked contexts, effectively resetting the conversation's memory.
 */
public void resetConversation() {
	contextStack.clear();
}
}