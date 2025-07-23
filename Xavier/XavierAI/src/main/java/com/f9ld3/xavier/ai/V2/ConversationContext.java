package com.f9ld3.xavier.ai.V2;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages the state and memory of a conversation with the user.
 */
public class ConversationContext {

private String username;
private String lastIntent;
private String lastSubject;
private String lastFailedInput;
private String pendingIntent; // NEW: Tracks intents waiting for more info
private final Map<String, Object> entities = new HashMap<>();

public String getUsername() { return username; }
public void setUsername(String username) { this.username = username; }

public String getLastIntent() { return lastIntent; }
public void setLastIntent(String lastIntent) { this.lastIntent = lastIntent; }

public String getLastSubject() { return lastSubject; }
public void setLastSubject(String lastSubject) { this.lastSubject = lastSubject; }

public String getLastFailedInput() { return lastFailedInput; }
public void setLastFailedInput(String lastFailedInput) { this.lastFailedInput = lastFailedInput; }

public String getPendingIntent() { return pendingIntent; }
public void setPendingIntent(String pendingIntent) { this.pendingIntent = pendingIntent; }
public void clearPendingIntent() { this.pendingIntent = null; }

public void clearLastFailedInput() { this.lastFailedInput = null; }

public void setEntity(String key, Object value) { entities.put(key, value); }
public Object getEntity(String key) { return entities.get(key); }
}