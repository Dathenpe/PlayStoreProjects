// C:/Users/Music_Minister/Desktop/PlayStore/PlayStoreProjects/Xavier/XavierAI/src/main/java/com/f9ld3/xavier/ai/V2/Application.java
package com.f9ld3.xavier.ai.V2;

import com.f9ld3.xavier.ai.V2.utils.ProgressBar;

/**
 * The main entry point for the Xavier AI console application.
 * This class is responsible for initializing the AI core, handling the
 * user interaction loop, and managing the application's lifecycle.
 */
public class Application {

private static final int TYPING_DELAY_MS = 80;

//public static void main(String[] args) {
//	//XavierCoreV2 xavier = new XavierCoreV2();
////	xavier.train("training_data.txt");
//
//	if (xavier.isTrained()) {
//		System.out.println("\n--- Xavier is ready. Ask a question or say 'exit' to quit. ---");
//		ConversationContext conversation = new ConversationContext();
//		ExecutorService executor = Executors.newSingleThreadExecutor();
//
//		try (Scanner scanner = new Scanner(System.in)) {
//			while (true) {
//				System.out.print("You: ");
//				String input = scanner.nextLine();
//
//				// --- NEW: Content Safety Filter ---
//				// Before processing, check if the input is appropriate.
//				if (!ContentSafetyFilter.isSafe(input)) {
//					printWithTypingEffect("I cannot respond to that. Please ask about a different topic.");
//					continue; // Skip the rest of the loop and prompt for new input.
//				}
//
//				if ("exit".equalsIgnoreCase(input)) {
//					String goodbyeResponse = xavier.getResponse("bye", conversation);
//					printWithTypingEffect(goodbyeResponse);
//					break;
//				}
//
//				ProgressBar progressBar = new ProgressBar();
//				Thread progressThread = new Thread(progressBar);
//				if (!XavierCoreV2.DEBUG_MODE) {
//					progressThread.start();
//				}
//
//				CompletableFuture<String> futureResponse = CompletableFuture.supplyAsync(() ->
//						                                                                         xavier.getResponse(input, conversation), executor
//				);
//
//				// UPDATED: Switched from .whenComplete to a more robust try-catch around .join().
//				// This is simpler for a console app and correctly centralizes all error handling,
//				// preventing the UI race condition.
//				try {
//					String response = futureResponse.join(); // This will block and re-throw any exception from the future.
//					stopProgressBar(progressBar, progressThread);
//					printWithTypingEffect(response);
//				} catch (Exception e) {
//					// Catch any exception that was thrown inside the async task.
//					stopProgressBar(progressBar, progressThread);
//					// Now, all error output is centralized and synchronized.
//					printWithTypingEffect("I seem to have encountered an internal error. Please try again.");
//					System.err.println("Error during response generation: " + e.getMessage());
//					if (e.getCause() != null) {
//						System.err.println("Cause: " + e.getCause().getMessage());
//					}
//				}
//			}
//		} finally {
//			executor.shutdown();
//		}
//	} else {
//		System.out.println("\n--- Xavier could not be started due to a training error. ---");
//	}
//}

private static void stopProgressBar(ProgressBar progressBar, Thread progressThread) {
	if (!XavierCoreV2.DEBUG_MODE) {
		progressBar.stop();
		try {
			progressThread.join();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}
}

/**
 * Prints text to the console with a simulated typing effect.
 * @param text The text to display.
 */
private static void printWithTypingEffect(String text) {
	System.out.print("Xavier: ");
	String[] words = text.split("\\s+");
	for (int i = 0; i < words.length; i++) {
		System.out.print(words[i]);
		if (i < words.length - 1) {
			System.out.print(" ");
		}
		System.out.flush();
		try {
			Thread.sleep(TYPING_DELAY_MS);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			System.err.println("Typing effect was interrupted.");
			break;
		}
	}
	System.out.println();
}
}