package com.f9ld3.xavier.ai.V2.utils;
/**
 * A simple, text-based progress bar for the console that is compatible
 * with Android's logcat or a standard terminal. It displays a spinning
 * character to indicate that a process is running.
 */
public class ProgressBar implements Runnable {

private volatile boolean running = true;
private final char[] spinner = new char[]{'|', '/', '-', '\\'};

@Override
public void run() {
	int i = 0;
	System.out.print("Xavier: "); // Start on the same line as the prompt
	while (running) {
		// Use \r (carriage return) to move the cursor to the beginning of the line
		System.out.print("\rXavier: " + spinner[i % spinner.length] + " Thinking...");
		i++;
		try {
			Thread.sleep(150); // Animation speed
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			break;
		}
	}
	// Clear the line by overwriting it with spaces and returning the cursor
	System.out.print("\r" + " ".repeat(25) + "\r");
}

public void stop() {
	this.running = false;
}
}