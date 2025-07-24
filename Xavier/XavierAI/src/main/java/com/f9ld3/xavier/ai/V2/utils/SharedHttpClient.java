package com.f9ld3.xavier.ai.V2.utils;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * Provides a single, shared, and robust instance of HttpClient for the entire application.
 * This ensures efficient connection reuse and consistent configuration.
 */
public class SharedHttpClient {

private static final HttpClient client = HttpClient.newBuilder()
		                                         .version(HttpClient.Version.HTTP_2)
		                                         // This is the critical fix: It tells the client to always follow redirects (e.g., 301, 302, 308).
		                                         .followRedirects(HttpClient.Redirect.ALWAYS)
		                                         .connectTimeout(Duration.ofSeconds(15)) // A reasonable timeout for external services.
		                                         .build();

/**
 * Gets the shared HttpClient instance.
 * @return The singleton HttpClient.
 */
public static HttpClient get() {
	return client;
}

// Private constructor to prevent instantiation of this utility class.
private SharedHttpClient() {}
}