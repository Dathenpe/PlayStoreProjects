package com.f9ld3.xavier.ai.V2.utils;

import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * A utility to check for a live internet connection.
 * It attempts to open a socket to a reliable, high-availability host.
 */
public final class NetworkStatusChecker {

// Google's public DNS server is an excellent choice for this check.
private static final String TEST_HOST = "8.8.8.8";
private static final int TEST_PORT = 53; // DNS port
private static final int TIMEOUT_MS = 2000; // 2-second timeout

private NetworkStatusChecker() {}

/**
 * Checks if there is a live internet connection.
 * @return true if a connection can be established, false otherwise.
 */
public static boolean isOnline() {
	try (Socket socket = new Socket()) {
		socket.connect(new InetSocketAddress(TEST_HOST, TEST_PORT), TIMEOUT_MS);
		return true;
	} catch (Exception e) {
		return false; // Any exception means we're likely offline.
	}
}
}