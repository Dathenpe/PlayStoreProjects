package com.f9ld3.xavier.ai.V2.utils;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;

/**
 * Provides a single, shared, and robust instance of OkHttpClient for the entire application.
 * This ensures efficient connection reuse and consistent configuration.
 */
public class SharedHttpClient {

	// Configure the timeout values as needed
	private static final long CONNECT_TIMEOUT_SECONDS = 15;
	private static final long READ_TIMEOUT_SECONDS = 15;
	private static final long WRITE_TIMEOUT_SECONDS = 15;

	// Lazy initialized singleton instance
	private static volatile OkHttpClient clientInstance;

	// Private constructor to prevent instantiation
	private SharedHttpClient() {}

	/**
	 * Gets the shared OkHttpClient instance.
	 * This instance is configured with reasonable defaults for timeouts and redirect handling.
	 *
	 * @return The singleton OkHttpClient.
	 */
	public static OkHttpClient get() {
		// Double-checked locking for thread-safe lazy initialization
		if (clientInstance == null) {
			synchronized (SharedHttpClient.class) {
				if (clientInstance == null) {
					// Optional: Add a logging interceptor for debugging network calls
					// HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
					// loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY); // Or Level.BASIC

					clientInstance = new OkHttpClient.Builder()
							.connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
							.readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
							.writeTimeout(WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
							.followRedirects(true)       // OkHttp follows redirects by default
							.followSslRedirects(true)  // OkHttp follows SSL redirects by default
							// .addInterceptor(loggingInterceptor) // Uncomment to add logging
							// Other configurations can be added here (e.g., connection pool, protocols)
							.build();
				}
			}
		}
		return clientInstance;
	}
}
