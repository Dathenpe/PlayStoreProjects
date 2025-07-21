package com.f9ld3.xavier.ai.V2;

import com.f9ld3.xavier.ai.V2.handlers.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;

/**
 * The central core of the Xavier AI, version 2.
 * This class orchestrates the training and prediction process using a scalable,
 * resilient, and API-driven handler system.
 */
public class XavierCoreV2 {

// --- Dependencies ---
private final NaiveBayesClassifier classifier;
private final Map<String, IntentHandler> intentHandlers;
private final Map<String, String> directMatches;
private WolframAlphaClient wolframAlphaClient; // Handles all knowledge queries

// --- State ---
private boolean isTrained = false;

// --- Configuration ---
private static final double CONFIDENCE_THRESHOLD = 0.5;

public XavierCoreV2() {
	this.classifier = new NaiveBayesClassifier();
	this.intentHandlers = new HashMap<>();
	this.directMatches = new HashMap<>();
	// These methods must be called in order
	loadApiKeysAndClients();
	registerHandlers();
	registerDirectMatches();
}

/**
 * Loads all API keys from the properties file and initializes API clients.
 * This centralizes configuration management for better performance and maintainability.
 */
private void loadApiKeysAndClients() {
	try (InputStream input = XavierCoreV2.class.getClassLoader().getResourceAsStream("api.properties")) {
		Properties prop = new Properties();
		if (input == null) {
			System.err.println("FATAL: Unable to find api.properties. API-based functionality will be disabled.");
			// Initialize the client as non-functional
			this.wolframAlphaClient = new WolframAlphaClient(null, null);
			return;
		}
		prop.load(input);
		
		// Load both primary and backup keys for Wolfram|Alpha and create the resilient client
		String primaryWolframId = prop.getProperty("wolframalpha.appid");
		String backupWolframId = prop.getProperty("wolframalpha.appid.backup");
		this.wolframAlphaClient = new WolframAlphaClient(primaryWolframId, backupWolframId);
		
	} catch (IOException ex) {
		System.err.println("FATAL: Error loading api.properties.");
		ex.printStackTrace();
		// Ensure client is non-functional on error
		this.wolframAlphaClient = new WolframAlphaClient(null, null);
	}
}

/**
 * Registers all the available intent handlers.
 * This method uses dependency injection to provide API keys and other handlers where needed.
 */
private void registerHandlers() {
	// --- Handlers that don't need dependencies ---
	intentHandlers.put("greeting", new GreetingHandler());
	intentHandlers.put("goodbye", new GoodbyeHandler());
	intentHandlers.put("time_query", new TimeQueryHandler());
	intentHandlers.put("date_query", new DateQueryHandler());
	intentHandlers.put("calculator_query", new CalculatorHandler());
	intentHandlers.put("set_username", new SetUsernameHandler());
	intentHandlers.put("get_username", new GetUsernameHandler());
	intentHandlers.put("about_bot", new AboutBotHandler());
	intentHandlers.put("chitchat", new ChitChatHandler());
	intentHandlers.put("default", new DefaultHandler());
	
	// --- Handlers that require API keys or other dependencies ---
	String openWeatherMapApiKey = getApiKeyFromProperties("openweathermap.apikey");
	
	// Create handlers that need the OpenWeatherMap key
	WeatherQueryHandler weatherHandler = new WeatherQueryHandler(openWeatherMapApiKey);
	TimezoneQueryHandler timezoneHandler = new TimezoneQueryHandler(openWeatherMapApiKey);
	intentHandlers.put("weather_query", weatherHandler);
	intentHandlers.put("timezone_query", timezoneHandler);
	
	// Create the handler that needs the WolframAlpha client
	intentHandlers.put("knowledge_query", new KnowledgeQueryHandler(this.wolframAlphaClient));
	
	// Create the FollowUpHandler and inject the handlers it depends on
	intentHandlers.put("follow_up", new FollowUpHandler(weatherHandler, timezoneHandler, this.wolframAlphaClient));
}

/**
 * Helper method to read a single key from the properties file.
 * @param key The property key to look for.
 * @return The key's value, or null if not found or an error occurs.
 */
private String getApiKeyFromProperties(String key) {
	try (InputStream input = XavierCoreV2.class.getClassLoader().getResourceAsStream("api.properties")) {
		if (input == null) return null;
		Properties prop = new Properties();
		prop.load(input);
		String value = prop.getProperty(key);
		// Return null if the key is a placeholder or empty
		return (value != null && !value.contains("YOUR_") && !value.trim().isEmpty()) ? value : null;
	} catch (IOException e) {
		System.err.println("WARN: Could not read property '" + key + "' from api.properties.");
		return null;
	}
}

/**
 * Registers simple, unambiguous phrases for direct intent matching.
 */
private void registerDirectMatches() {
	// Greetings
	directMatches.put("hi", "greeting");
	directMatches.put("hello", "greeting");
	directMatches.put("hey", "greeting");
	// ... (add other direct matches as needed)
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
		System.err.println("FATAL: Failed to train the model. The AI will not be functional.");
		e.printStackTrace();
	}
}

/**
 * Processes user input using a multi-layered approach for maximum accuracy and intelligence:
 * 1. Direct Match: For perfect, unambiguous commands.
 * 2. Fuzzy Match: To handle common typos in simple commands.
 * 3. Wolfram|Alpha Recognizer: A fast external check for general knowledge questions.
 * 4. Internal Statistical Classifier: For all other nuanced, trained skills.
 *
 * @param userInput The raw text from the user.
 * @param context The current conversation's memory.
 * @return A response from the AI.
 */
public String getResponse(String userInput, ConversationContext context) {
	if (!isTrained) {
		return "I'm sorry, I haven't been trained yet. Please train me first.";
	}
	
	String predictedIntent = null;
	double confidence = 1.0;
	
	String cleanedInput = userInput.toLowerCase().trim();
	String directMatchIntent = directMatches.get(cleanedInput);
	
	if (directMatchIntent != null) {
		predictedIntent = directMatchIntent;
		System.out.printf("[DEBUG] Direct match found. Intent: %s%n", predictedIntent);
	} else {
		String fuzzyMatch = FuzzyMatcher.getBestMatch(cleanedInput, directMatches.keySet());
		if (fuzzyMatch != null) {
			predictedIntent = directMatches.get(fuzzyMatch);
			System.out.printf("[DEBUG] Fuzzy match found for '%s' -> '%s'. Intent: %s%n", cleanedInput, fuzzyMatch, predictedIntent);
		} else {
			// Use Wolfram|Alpha as an intelligent pre-classifier
			if (this.wolframAlphaClient.canAnswer(cleanedInput)) {
				predictedIntent = "knowledge_query";
				confidence = 1.0; // We are confident if the recognizer says yes.
				System.out.println("[DEBUG] Wolfram|Alpha Recognizer success. Routing to KnowledgeQueryHandler.");
			} else {
				// Fallback to our internal classifier for specific skills
				List<String> tokens = TextProcessor.tokenize(userInput);
				if (tokens.isEmpty()) {
					return intentHandlers.get("default").handle(userInput, context);
				}
				PredictionResult result = classifier.predict(tokens);
				predictedIntent = result.getPredictedLabel();
				confidence = result.getConfidence();
				System.out.printf("[DEBUG] Classifier result. Intent: %s, Confidence: %.2f%%%n", predictedIntent, confidence * 100);
			}
		}
	}
	
	// --- Handler Selection Logic ---
	IntentHandler handler;
	// Special case for knowledge queries. If the classifier's best guess is
	// knowledge_query, we should always try it, regardless of confidence,
	// because the KnowledgeQueryHandler has its own robust error handling.
	if ("knowledge_query".equals(predictedIntent)) {
		System.out.println("[DEBUG] Classifier suggested knowledge_query. Routing to handler as a fallback.");
		handler = intentHandlers.get("knowledge_query");
	} else if (confidence >= CONFIDENCE_THRESHOLD && predictedIntent != null) {
		handler = intentHandlers.getOrDefault(predictedIntent, intentHandlers.get("default"));
	} else {
		if (confidence < CONFIDENCE_THRESHOLD) {
			System.out.println("[DEBUG] Confidence below threshold. Using default handler.");
		}
		handler = intentHandlers.get("default");
	}
	
	String response = handler.handle(userInput, context);
	
	// --- Context Management ---
	// Don't update context for follow-ups, as they depend on the *previous* intent.
	if (!"follow_up".equals(predictedIntent)) {
		context.setLastIntent(predictedIntent);
		
		// **NEW**: If it was a knowledge query, remember the subject for future follow-ups.
		if ("knowledge_query".equals(predictedIntent)) {
			context.setLastSubject(userInput);
		}
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
		ConversationContext conversation = new ConversationContext();
		
		try (Scanner scanner = new Scanner(System.in)) {
			while (true) {
				System.out.print("You: ");
				String input = scanner.nextLine();
				
				if ("exit".equalsIgnoreCase(input)) {
					System.out.println("Xavier: " + xavier.getResponse("bye", conversation));
					break;
				}
				
				String response = xavier.getResponse(input, conversation);
				System.out.println("Xavier: " + response);
			}
		}
	} else {
		System.out.println("\n--- Xavier could not be started due to a training error. ---");
	}
}
}