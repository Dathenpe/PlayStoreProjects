package com.f9ld3.xavier.ai.V2.utils;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * Provides a single, shared, and robust HttpClient instance for the entire application.
 * This follows best practices for performance and stability by enabling connection pooling
 * and setting reasonable timeouts.
 */
public final class SharedHttpClient {

// The single, static instance of the HttpClient.
private static final HttpClient INSTANCE = HttpClient.newBuilder()
		                                           .version(HttpClient.Version.HTTP_2)
		                                           .connectTimeout(Duration.ofSeconds(15)) // Set a connection timeout
		                                           .build();

/**
 * Private constructor to prevent instantiation.
 */
private SharedHttpClient() {}

/**
 * Gets the shared HttpClient instance.
 * @return The singleton HttpClient.
 */
public static HttpClient get() {
	return INSTANCE;
}
}