package com.f9ld3.xavier.ai.V2;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents the short-term memory for a single conversation.
 * This class holds stateful information that can be used across multiple turns
 * of dialogue to provide more intelligent and context-aware responses.
 */
public class ConversationContext {

private String lastIntent;
private final Map<String, Object> entities;
private String lastSubject; // Field to remember the topic of conversation

public ConversationContext() {
	this.entities = new HashMap<>();
	// Initialize with a generic start intent
	this.lastIntent = "start";
	this.lastSubject = null;
}

public String getLastIntent() {
	return lastIntent;
}

public void setLastIntent(String lastIntent) {
	this.lastIntent = lastIntent;
}

/**
 * Stores an extracted piece of information (an entity).
 * @param key The type of entity (e.g., "location").
 * @param value The extracted value (e.g., "London").
 */
public void setEntity(String key, Object value) {
	this.entities.put(key, value);
}

/**
 * Retrieves a stored entity.
 * @param key The type of entity to retrieve.
 * @return The entity's value, or null if not found.
 */
public Object getEntity(String key) {
	return entities.get(key);
}

/**
 * Retrieves the last subject of a knowledge query.
 * @return The last subject as a String, or null if not set.
 */
public String getLastSubject() {
	return lastSubject;
}

/**
 * Sets the last subject of a knowledge query.
 * @param lastSubject The user's original query text.
 */
public void setLastSubject(String lastSubject) {
	this.lastSubject = lastSubject;
}

/**
 * Clears a specific entity from memory.
 * @param key The key of the entity to remove.
 */
public void clearEntity(String key) {
	this.entities.remove(key);
}
}