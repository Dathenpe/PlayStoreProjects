package com.f9ld3.xavier.ai.V2;

import com.f9ld3.xavier.ai.V2.handlers.*;
import com.f9ld3.xavier.ai.V2.handlers.PatternHandler.IntentMatch;
import com.f9ld3.xavier.ai.V2.services.GeocodingService;
import com.f9ld3.xavier.ai.V2.services.LocationResolverService;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Scanner;

/**
 * The central core of the Xavier AI, version 2.
 * This class orchestrates the training and prediction process using a scalable,
 * resilient, and API-driven handler system with pattern-matching and self-correction.
 */
public class XavierCoreV2 {

// --- Dependencies ---
private final NaiveBayesClassifier classifier;
private final Map<String, IntentHandler> intentHandlers;
private final Map<String, String> directMatches;
private final PatternHandler patternHandler;
private WolframAlphaClient wolframAlphaClient;

// --- State ---
private boolean isTrained = false;

// --- Configuration ---
private static final double CONFIDENCE_THRESHOLD = 0.5;

public XavierCoreV2() {
	this.classifier = new NaiveBayesClassifier();
	this.intentHandlers = new HashMap<>();
	this.directMatches = new HashMap<>();
	this.patternHandler = new PatternHandler();
	// These methods must be called in order
	loadApiKeysAndClients();
	registerHandlers();
	registerDirectMatches();
	registerPatterns();
}

/**
 * Processes user input using a multi-layered approach for maximum accuracy and intelligence.
 */
public String getResponse(String userInput, ConversationContext context) {
	if (!isTrained) {
		return "I'm sorry, I haven't been trained yet. Please train me first.";
	}
	
	// --- Reasoning Pipeline ---
	
	// STEP 0: Check for a pending intent from a previous question (HIGHEST PRIORITY)
	String pendingIntent = context.getPendingIntent();
	if (pendingIntent != null) {
		System.out.printf("[DEBUG] Found pending intent '%s'. Using input '%s' as the missing entity.%n", pendingIntent, userInput);
		IntentHandler handler = intentHandlers.get(pendingIntent);
		if (handler != null) {
			// Clear the pending intent *before* handling it to prevent loops.
			context.clearPendingIntent();
			// The user's input is the missing information (e.g., the location).
			// We pass it directly to the correct handler.
			return handler.handle(userInput, context);
		} else {
			// This case is unlikely, but as a safeguard, we clear the intent and proceed.
			context.clearPendingIntent();
		}
	}
	
	// STEP 0.5: Check for a query refinement after a failure.
	String lastFailed = context.getLastFailedInput();
	// A "refinement" is likely a short phrase. We'll use a word count heuristic.
	boolean isRefinementAttempt = userInput.trim().split("\\s+").length <= 4;
	
	if (lastFailed != null && isRefinementAttempt) {
		System.out.printf("[DEBUG] Found previous failed input. Combining with new input for refinement.%n");
		String refinedQuery = lastFailed + " " + userInput;
		context.clearLastFailedInput(); // Consume the failed input state
		
		// Recursively call getResponse with the new, combined query.
		// This is a powerful way to re-leverage the entire reasoning pipeline.
		return this.getResponse(refinedQuery, context);
	}
	
	String predictedIntent = null;
	double confidence = 1.0;
	String cleanedInput = userInput.toLowerCase().trim();
	
	// STEP 1: Direct Match
	String directMatchIntent = directMatches.get(cleanedInput);
	if (directMatchIntent != null) {
		predictedIntent = directMatchIntent;
		System.out.printf("[DEBUG] Direct match found. Intent: %s%n", predictedIntent);
	} else {
		// STEP 2: Pattern Match
		Optional<IntentMatch> patternMatch = patternHandler.match(userInput);
		if (patternMatch.isPresent()) {
			IntentMatch match = patternMatch.get();
			predictedIntent = match.getIntent();
			context.setEntity("username", match.getEntity());
			System.out.printf("[DEBUG] Pattern match found. Intent: %s, Entity: %s%n", predictedIntent, match.getEntity());
		} else {
			// STEP 3: Fuzzy Match
			String fuzzyMatch = FuzzyMatcher.getBestMatch(cleanedInput, directMatches.keySet());
			if (fuzzyMatch != null) {
				predictedIntent = directMatches.get(fuzzyMatch);
				System.out.printf("[DEBUG] Fuzzy match found for '%s' -> '%s'. Intent: %s%n", cleanedInput, fuzzyMatch, predictedIntent);
			} else {
				// STEP 4: API Pre-classifier
				if (this.wolframAlphaClient.canAnswer(cleanedInput)) {
					predictedIntent = "knowledge_query";
					confidence = 1.0;
					System.out.println("[DEBUG] Wolfram|Alpha Recognizer success. Routing to KnowledgeQueryHandler.");
				} else {
					// STEP 5: Statistical Classifier
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
	}
	
	// --- Handler Selection Logic ---
	IntentHandler handler;
	if ("knowledge_query".equals(predictedIntent)) {
		System.out.println("[DEBUG] Classifier suggested knowledge_query. Routing to handler as a fallback.");
		handler = intentHandlers.get("knowledge_query");
		context.clearLastFailedInput(); // Clear any previous failure before handling.
	} else if (confidence >= CONFIDENCE_THRESHOLD && predictedIntent != null) {
		handler = intentHandlers.getOrDefault(predictedIntent, intentHandlers.get("default"));
		context.clearLastFailedInput(); // Clear any previous failure before handling.
	} else {
		if (confidence < CONFIDENCE_THRESHOLD) {
			System.out.println("[DEBUG] Confidence below threshold. Using default handler.");
		}
		handler = intentHandlers.get("default");
	}
	
	String response = handler.handle(userInput, context);
	
	// --- Context Management ---
	if (!"follow_up".equals(predictedIntent) && !"correction".equals(predictedIntent)) {
		context.setLastIntent(predictedIntent);
		if ("knowledge_query".equals(predictedIntent)) {
			context.setLastSubject(userInput);
		}
	}
	
	return response;
}

private void loadApiKeysAndClients() {
	try (InputStream input = XavierCoreV2.class.getClassLoader().getResourceAsStream("api.properties")) {
		Properties prop = new Properties();
		if (input == null) {
			System.err.println("FATAL: Unable to find api.properties. API-based functionality will be disabled.");
			this.wolframAlphaClient = new WolframAlphaClient(null, null);
			return;
		}
		prop.load(input);
		
		String primaryWolframId = prop.getProperty("wolframalpha.appid");
		String backupWolframId = prop.getProperty("wolframalpha.appid.backup");
		this.wolframAlphaClient = new WolframAlphaClient(primaryWolframId, backupWolframId);
		
	} catch (IOException ex) {
		System.err.println("FATAL: Error loading api.properties.");
		ex.printStackTrace();
		this.wolframAlphaClient = new WolframAlphaClient(null, null);
	}
}

private void registerHandlers() {
	// --- Group 1: Simple, dependency-free handlers ---
	intentHandlers.put("greeting", new GreetingHandler());
	intentHandlers.put("goodbye", new GoodbyeHandler());
	intentHandlers.put("time_query", new TimeQueryHandler());
	intentHandlers.put("date_query", new DateQueryHandler());
	intentHandlers.put("gratitude", new GratitudeHandler());
	intentHandlers.put("confirmation", new ConfirmationHandler());
	intentHandlers.put("calculator_query", new CalculatorHandler());
	intentHandlers.put("set_username", new SetUsernameHandler());
	intentHandlers.put("get_username", new GetUsernameHandler());
	intentHandlers.put("about_bot", new AboutBotHandler());
	intentHandlers.put("chitchat", new ChitChatHandler());
	intentHandlers.put("default", new DefaultHandler());
	
	// --- Group 2: Handlers that require the core instance for re-routing ---
	intentHandlers.put("follow_up", new FollowUpHandler(this));
	intentHandlers.put("correction", new CorrectionHandler(this));
	
	// --- Group 3: Handlers that require API keys or other dependencies ---
	String primaryWeatherKey = getApiKeyFromProperties("openweathermap.apikey");
	String backupWeatherKey = getApiKeyFromProperties("openweathermap.apikey.backup");
	
	List<String> weatherApiKeys = new ArrayList<>();
	if (primaryWeatherKey != null) weatherApiKeys.add(primaryWeatherKey);
	if (backupWeatherKey != null) weatherApiKeys.add(backupWeatherKey);
	
	try {
		// 1. Create the low-level geocoding service.
		GeocodingService geocodingService = new GeocodingService(weatherApiKeys);
		// 2. Create the high-level resolver service that encapsulates the cache-then-API logic.
		LocationResolverService locationResolver = new LocationResolverService(geocodingService);
		
		// 3. Inject the new resolver service into the handlers that need it.
		intentHandlers.put("weather_query", new WeatherQueryHandler(locationResolver, primaryWeatherKey));
		intentHandlers.put("timezone_query", new TimezoneQueryHandler(locationResolver));
		
	} catch (IllegalArgumentException e) {
		System.err.println("WARN: Location services could not be initialized. Weather and Timezone features will be disabled. Reason: " + e.getMessage());
		IntentHandler unavailableHandler = (userInput, context) -> "I'm sorry, my location-based services are currently unavailable.";
		intentHandlers.put("weather_query", unavailableHandler);
		intentHandlers.put("timezone_query", unavailableHandler);
	}
	
	intentHandlers.put("knowledge_query", new KnowledgeQueryHandler(this.wolframAlphaClient));
}

private void registerPatterns() {
	// This pattern looks for "my name is", "call me", or "i am", and captures everything after it.
	patternHandler.registerPattern("set_username", "(?:my name is|call me|i am)\\s+(.+)");
	patternHandler.registerPattern("get_username", "(?:what is|what's|do you know) my name\\??");
}

private String getApiKeyFromProperties(String key) {
	try (InputStream input = XavierCoreV2.class.getClassLoader().getResourceAsStream("api.properties")) {
		if (input == null) return null;
		Properties prop = new Properties();
		prop.load(input);
		String value = prop.getProperty(key);
		return (value != null && !value.contains("YOUR_") && !value.trim().isEmpty()) ? value : null;
	} catch (IOException e) {
		System.err.println("WARN: Could not read property '" + key + "' from api.properties.");
		return null;
	}
}

private void registerDirectMatches() {
	// Greetings
	directMatches.put("hi", "greeting");
	directMatches.put("hello", "greeting");
	directMatches.put("hey", "greeting");
	directMatches.put("yo", "greeting");
	directMatches.put("sup", "greeting");
	directMatches.put("wassup", "greeting");
	directMatches.put("what's up", "greeting");
}

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