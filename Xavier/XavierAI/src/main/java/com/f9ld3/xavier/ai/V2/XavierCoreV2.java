package com.f9ld3.xavier.ai.V2;

import com.f9ld3.xavier.ai.V2.handlers.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * The central core of the Xavier AI, version 2.
 * This class orchestrates the training and prediction process using a scalable handler system.
 */
public class XavierCoreV2 {

// --- Dependencies ---
private final NaiveBayesClassifier classifier;
private final Map<String, IntentHandler> intentHandlers;
// A map for fast, rule-based matching of simple phrases.
private final Map<String, String> directMatches;

// --- State ---
private boolean isTrained = false;

// --- Configuration ---
/**
 * Defines the threshold for how confident the AI needs to be.
 * A value of 0.5 means it must be at least 50% confident to act on its prediction.
 */
private static final double CONFIDENCE_THRESHOLD = 0.5;

public XavierCoreV2() {
	this.classifier = new NaiveBayesClassifier();
	this.intentHandlers = new HashMap<>();
	this.directMatches = new HashMap<>();
	registerHandlers();
	registerDirectMatches();
}

/**
 * Registers all the available intent handlers.
 * To add a new skill, create a handler and add it here.
 */
private void registerHandlers() {
	intentHandlers.put("greeting", new GreetingHandler());
	intentHandlers.put("goodbye", new GoodbyeHandler());
	intentHandlers.put("time_query", new TimeQueryHandler());
	intentHandlers.put("date_query", new DateQueryHandler());
	intentHandlers.put("weather_query", new WeatherQueryHandler());
	intentHandlers.put("calculator_query", new CalculatorHandler());
	intentHandlers.put("follow_up", new FollowUpHandler()); // Register the new handler
	
	// The default handler for unrecognized intents
	intentHandlers.put("default", new DefaultHandler());
}

/**
 * Registers simple, unambiguous phrases for direct intent matching.
 * This includes common variations and misspellings.
 */
private void registerDirectMatches() {
	// Greetings
	directMatches.put("hi", "greeting");
	directMatches.put("hello", "greeting");
	directMatches.put("hey", "greeting");
	directMatches.put("hey there", "greeting");
	directMatches.put("yo", "greeting");
	directMatches.put("hallo", "greeting"); // Common misspelling
	directMatches.put("helo", "greeting");  // Common misspelling
	directMatches.put("greetings", "greeting");
	
	// Goodbyes
	directMatches.put("bye", "goodbye");
	directMatches.put("goodbye", "goodbye");
	directMatches.put("see you", "goodbye");
	directMatches.put("see ya", "goodbye");
	directMatches.put("cya", "goodbye"); // Common slang
	
	// --- NEW: Time and Date Queries ---
	directMatches.put("time", "time_query");
	directMatches.put("what time is it", "time_query");
	directMatches.put("what is the time", "time_query");
	directMatches.put("tell me the time", "time_query");
	
	directMatches.put("date", "date_query");
	directMatches.put("what is the date", "date_query");
	directMatches.put("what's today's date", "date_query");
	directMatches.put("today's date", "date_query");
}

/**
 * Loads data from the specified resource file and trains the classifier.
 * @param resourceFileName The name of the training data file in the resources folder.
 */
public void train(String resourceFileName) {
	DataLoader dataLoader = new DataLoader();
	try {
		System.out.println("Starting training process...");
		dataLoader.loadDataFromResource(resourceFileName);
		List<List<String>> documents = dataLoader.getDocuments();
		List<String> labels = dataLoader.getLabels();
		
		classifier.fit(documents, labels);
		isTrained = true;
		System.out.println("Training complete!");
		
	} catch (IOException e) {
		// A failure during training is critical. Log it clearly.
		System.err.println("FATAL: Failed to train the model. The AI will not be functional.");
		e.printStackTrace();
	}
}

/**
 * Processes user input, predicts the intent with confidence, and generates a response.
 * @param userInput The raw text from the user.
 * @return A response from the AI.
 */
public String getResponse(String userInput, ConversationContext context) { // Signature changed
	if (!isTrained) {
		return "I'm sorry, I haven't been trained yet. Please train me first.";
	}
	
	String predictedIntent = null;
	double confidence = 1.0;
	
	String directMatchIntent = directMatches.get(userInput.toLowerCase().trim());
	
	if (directMatchIntent != null) {
		predictedIntent = directMatchIntent;
		System.out.printf("[DEBUG] Direct match found. Intent: %s, Confidence: %.2f%%%n", predictedIntent, confidence * 100);
	} else {
		List<String> tokens = TextProcessor.tokenize(userInput);
		if (tokens.isEmpty()) {
			System.out.println("[DEBUG] No valid tokens found. Using default handler.");
			return intentHandlers.get("default").handle(userInput, context);
		}
		PredictionResult result = classifier.predict(tokens);
		predictedIntent = result.getPredictedLabel();
		confidence = result.getConfidence();
		System.out.printf("[DEBUG] Classifier result. Intent: %s, Confidence: %.2f%%%n", predictedIntent, confidence * 100);
	}
	
	IntentHandler handler;
	if (confidence >= CONFIDENCE_THRESHOLD && predictedIntent != null) {
		handler = intentHandlers.getOrDefault(predictedIntent, intentHandlers.get("default"));
	} else {
		if (confidence < CONFIDENCE_THRESHOLD) {
			System.out.println("[DEBUG] Confidence below threshold. Using default handler.");
		}
		handler = intentHandlers.get("default");
	}
	
	// Let the handler generate the response
	String response = handler.handle(userInput, context);
	
	// *** UPDATE THE CONTEXT ***
	// Don't update context for follow-ups, as they depend on the *previous* intent.
	if (!"follow_up".equals(predictedIntent)) {
		context.setLastIntent(predictedIntent);
	}
	
	return response;
}

/**
 * The main entry point to run and interact with the AI from the console.
 */
public static void main(String[] args) {
	XavierCoreV2 xavier = new XavierCoreV2();
	xavier.train("training_data.txt");
	
	if (xavier.isTrained) {
		System.out.println("\n--- Xavier is ready. Ask a question or say 'exit' to quit. ---");
		// *** CREATE THE CONTEXT for the session ***
		ConversationContext conversation = new ConversationContext();
		
		try (Scanner scanner = new Scanner(System.in)) {
			while (true) {
				System.out.print("You: ");
				String input = scanner.nextLine();
				
				if ("exit".equalsIgnoreCase(input)) {
					System.out.println("Xavier: " + xavier.getResponse("bye", conversation));
					break;
				}
				
				// *** PASS THE CONTEXT into getResponse ***
				String response = xavier.getResponse(input, conversation);
				System.out.println("Xavier: " + response);
			}
		}
	} else {
		System.out.println("\n--- Xavier could not be started due to a training error. ---");
	}
}
}