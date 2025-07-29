package com.f9ld3.xavier.ai.V2;

import com.f9ld3.xavier.ai.V2.FuzzyMatcher.MatchResult;
import com.f9ld3.xavier.ai.V2.handlers.*;
import com.f9ld3.xavier.ai.V2.handlers.PatternHandler.IntentMatch;
import com.f9ld3.xavier.ai.V2.services.DictionaryService;
import com.f9ld3.xavier.ai.V2.services.FunFactService;
import com.f9ld3.xavier.ai.V2.services.GeocodingService;
import com.f9ld3.xavier.ai.V2.services.IPGeolocationService;
import com.f9ld3.xavier.ai.V2.services.JokeService;
import com.f9ld3.xavier.ai.V2.services.LocationResolverService;
import com.f9ld3.xavier.ai.V2.utils.ProgressBar;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The central core of the Xavier AI, version 2.
 * This class orchestrates the training and prediction process using a scalable,
 * resilient, and API-driven handler system with pattern-matching and self-correction.
 */
public class XavierCoreV2 {

// --- Dependencies ---
private final NaiveBayesClassifier classifier;
private final FuzzyMatcher fuzzyMatcher;
private final Map<String, IntentHandler> intentHandlers;
private final Map<String, String> directMatches;
private final PatternHandler patternHandler;
private WolframAlphaClient wolframAlphaClient;

// --- State ---
private boolean isTrained = false;

// --- Configuration ---
public static final boolean DEBUG_MODE = false; // Set to true to see debug logs
private static final double CONFIDENCE_THRESHOLD = 0.5;
private static final double FUZZY_RESCUE_THRESHOLD = 0.75;
private static final double DIRECT_MATCH_FUZZY_THRESHOLD = 0.80; // High threshold for common word typos
private static final int TYPING_DELAY_MS = 80;

public XavierCoreV2() {
	this.classifier = new NaiveBayesClassifier();
	this.fuzzyMatcher = new FuzzyMatcher();
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
		if (DEBUG_MODE) System.out.printf("[DEBUG] Found pending intent '%s'. Using input '%s' as the missing entity.%n", pendingIntent, userInput);
		IntentHandler handler = intentHandlers.get(pendingIntent);
		if (handler != null) {
			context.clearPendingIntent();
			return handler.handle(userInput, context);
		} else {
			context.clearPendingIntent();
		}
	}
	
	// STEP 0.5: Check for a query refinement after a failure.
	// This feature is temporarily disabled as it was causing cascading failures.
	// A more robust implementation is needed.
	/*
	String lastFailed = context.getLastFailedInput();
	boolean isRefinementAttempt = userInput.trim().split("\\s+").length <= 4;
	if (lastFailed != null && isRefinementAttempt) {
		if (DEBUG_MODE) System.out.printf("[DEBUG] Found previous failed input. Combining with new input for refinement.%n");
		String refinedQuery = lastFailed + " " + userInput;
		context.clearLastFailedInput();
		return this.getResponse(refinedQuery, context);
	}
	*/
	
	String predictedIntent = null;
	double confidence = 1.0;
	String cleanedInput = userInput.toLowerCase().trim();
	
	// STEP 1: Direct Match
	String directMatchIntent = directMatches.get(cleanedInput);
	if (directMatchIntent != null) {
		predictedIntent = directMatchIntent;
		if (DEBUG_MODE) System.out.printf("[DEBUG] Direct match found. Intent: %s%n", predictedIntent);
	} else {
		// STEP 1.5: Fuzzy Direct Match (NEW) - For typos of common words like "hi" -> "hit"
		Optional<String> fuzzyDirectMatch = FuzzyMatcher.findBestCandidate(cleanedInput, directMatches.keySet(), DIRECT_MATCH_FUZZY_THRESHOLD);
		if (fuzzyDirectMatch.isPresent()) {
			String matchedKey = fuzzyDirectMatch.get();
			predictedIntent = directMatches.get(matchedKey);
			if (DEBUG_MODE) System.out.printf("[DEBUG] Fuzzy Direct match found for '%s' -> '%s'. Intent: %s%n", cleanedInput, matchedKey, predictedIntent);
		} else {
			// STEP 2: Pattern Match
			Optional<IntentMatch> patternMatch = patternHandler.match(userInput);
			if (patternMatch.isPresent()) {
				IntentMatch match = patternMatch.get();
				predictedIntent = match.getIntent();
				// For weather patterns, the extracted group is the location, not the username.
				if ("weather_query".equals(predictedIntent)) {
					context.setEntity("location", match.getEntity());
				} else {
					context.setEntity("username", match.getEntity());
				}
				if (DEBUG_MODE) System.out.printf("[DEBUG] Pattern match found. Intent: %s, Entity: %s%n", predictedIntent, match.getEntity());
			} else {
				// STEP 3: API Pre-classifier
				if (this.wolframAlphaClient.canAnswer(cleanedInput)) {
					predictedIntent = "knowledge_query";
					confidence = 1.0;
					if (DEBUG_MODE) System.out.println("[DEBUG] Wolfram|Alpha Recognizer success. Routing to KnowledgeQueryHandler.");
				} else {
					// STEP 4: Statistical Classifier
					List<String> tokens = TextProcessor.tokenize(userInput);
					if (tokens.isEmpty()) {
						return intentHandlers.get("default").handle(userInput, context);
					}
					PredictionResult result = classifier.predict(tokens);
					predictedIntent = result.getPredictedLabel();
					confidence = result.getConfidence();
					if (DEBUG_MODE) System.out.printf("[DEBUG] Classifier result. Intent: %s, Confidence: %.2f%%%n", predictedIntent, confidence * 100);
				}
			}
		}
	}
	
	// --- Handler Selection Logic ---
	IntentHandler handler;
	if (predictedIntent != null && confidence >= CONFIDENCE_THRESHOLD) {
		handler = intentHandlers.getOrDefault(predictedIntent, intentHandlers.get("default"));
		context.clearLastFailedInput();
	} else {
		// STEP 5: FUZZY RESCUE - The classifier is not confident, let's try to find a close match.
		if (DEBUG_MODE) System.out.printf("[DEBUG] Confidence below threshold. Attempting Fuzzy Rescue...%n");
		Optional<MatchResult> fuzzyResult = fuzzyMatcher.findBestMatch(userInput, FUZZY_RESCUE_THRESHOLD);
		
		if (fuzzyResult.isPresent()) {
			MatchResult match = fuzzyResult.get();
			if (DEBUG_MODE) System.out.printf("[DEBUG] Fuzzy Rescue success! Matched '%s' to '%s'. Intent: %s, Confidence: %.2f%%%n",
					userInput, match.matchedPhrase(), match.intent(), match.confidenceScore() * 100);
			predictedIntent = match.intent();
			handler = intentHandlers.get(predictedIntent);
			context.clearLastFailedInput();
		} else {
			// STEP 6: KNOWLEDGE QUERY FALLBACK
			// If no other intent fits, it's likely a general knowledge question.
			// Let's try Wolfram|Alpha as a last resort before giving up.
			if (DEBUG_MODE) System.out.println("[DEBUG] Fuzzy Rescue failed. Attempting Knowledge Query as a fallback.");
			handler = intentHandlers.get("knowledge_query");
		}
	}
	
	String response = handler.handle(userInput, context);
	
	// --- Context Management ---
	if (!"follow_up".equals(predictedIntent) && !"correction".equals(predictedIntent)) {
		context.setLastIntent(predictedIntent);
		if ("knowledge_query".equals(predictedIntent) || "dictionary_query".equals(predictedIntent) || "fact_query".equals(predictedIntent)) {
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
			this.wolframAlphaClient = new WolframAlphaClient(); // No keys available
			return;
		}
		prop.load(input);
		
		// Load all available Wolfram|Alpha keys for fault tolerance.
		List<String> wolframKeys = new ArrayList<>();
		String primaryId = getApiKeyFromProperties("wolframalpha.appid");
		String backupId = getApiKeyFromProperties("wolframalpha.appid.backup");
		String tertiaryId = getApiKeyFromProperties("wolframalpha.appid.tertiary"); // Read the new key
		
		if (primaryId != null) wolframKeys.add(primaryId);
		if (backupId != null) wolframKeys.add(backupId);
		if (tertiaryId != null) wolframKeys.add(tertiaryId);
		
		// Pass all found keys to the client constructor.
		this.wolframAlphaClient = new WolframAlphaClient(wolframKeys.toArray(new String[0]));
		
	} catch (IOException ex) {
		System.err.println("FATAL: Error loading api.properties.");
		ex.printStackTrace();
		this.wolframAlphaClient = new WolframAlphaClient(); // No keys available
	}
}

private void registerHandlers() {
	// --- Group 1: Simple, dependency-free handlers ---
	intentHandlers.put("greeting", new GreetingHandler());
	intentHandlers.put("goodbye", new GoodbyeHandler());
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
		GeocodingService geocodingService = new GeocodingService(weatherApiKeys);
		IPGeolocationService ipGeolocationService = new IPGeolocationService();
		LocationResolverService locationResolver = new LocationResolverService(geocodingService);
		
		// Register the new, unified handlers that use the location services
		intentHandlers.put("time_query", new TimeQueryHandler(locationResolver, ipGeolocationService, primaryWeatherKey));
		intentHandlers.put("weather_query", new WeatherQueryHandler(locationResolver, ipGeolocationService, primaryWeatherKey));
		
	} catch (IllegalArgumentException e) {
		System.err.println("WARN: Location services could not be initialized. Weather and Timezone features will be disabled. Reason: " + e.getMessage());
		IntentHandler unavailableHandler = (userInput, context) -> "I'm sorry, my location-based services are currently unavailable.";
		intentHandlers.put("weather_query", unavailableHandler);
		intentHandlers.put("time_query", unavailableHandler);
	}
	
	// --- Group 4: Other API-based handlers ---
	DictionaryService dictionaryService = new DictionaryService();
	intentHandlers.put("dictionary_query", new DictionaryHandler(dictionaryService));
	
	JokeService jokeService = new JokeService();
	intentHandlers.put("joke_query", new JokeHandler(jokeService));
	
	FunFactService funFactService = new FunFactService();
	intentHandlers.put("fact_query", new FunFactHandler(funFactService));
	
	intentHandlers.put("knowledge_query", new KnowledgeQueryHandler(this.wolframAlphaClient));
}

private void registerPatterns() {
	patternHandler.registerPattern("set_username", "(?:my name is|call me|i am)\\s+(.+)");
	patternHandler.registerPattern("get_username", "(?:what is|what's|do you know) my name\\??");
	
	// NEW: Add more robust patterns for weather to catch implicit queries.
	// The (?i) flag makes the pattern case-insensitive.
	patternHandler.registerPattern("weather_query", "(?i)(?:what's|how's|tell me|check) (?:the )?(?:weather|forecast|temperature) (?:in|for|at) (.+)");
	patternHandler.registerPattern("weather_query", "(?i)^check (?:for )?(.+)");
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
	
	// ChitChat & Acknowledgements
	directMatches.put("ok", "chitchat");
	directMatches.put("okay", "chitchat");
	directMatches.put("cool", "chitchat");
	directMatches.put("nice", "chitchat");
	directMatches.put("great", "chitchat");
	directMatches.put("good", "chitchat");
	directMatches.put("alright", "chitchat");
	
	// Gratitude
	directMatches.put("thanks", "gratitude");
	directMatches.put("thank you", "gratitude");
	
	// Confirmation
	directMatches.put("yes", "confirmation");
	directMatches.put("yep", "confirmation");
	directMatches.put("yeah", "confirmation");
	directMatches.put("no", "confirmation");
	directMatches.put("nope", "confirmation");
}

public void train(String resourceFileName) {
	DataLoader dataLoader = new DataLoader();
	try {
		System.out.println("Starting training process...");
		dataLoader.loadDataFromResource(resourceFileName);
		
		// Train the primary classifier
		List<List<String>> documents = dataLoader.getDocuments();
		List<String> labels = dataLoader.getLabels();
		classifier.fit(documents, labels);
		System.out.println("Classifier training complete!");
		
		// Train the fuzzy matcher with the raw phrases
		List<String> rawPhrases = dataLoader.getRawPhrases();
		fuzzyMatcher.train(rawPhrases, labels);
		System.out.println("Fuzzy Matcher training complete!");
		
		isTrained = true;
		System.out.println("Training complete!");
		
	} catch (IOException e) {
		System.err.println("FATAL: Failed to train the model. The AI will not be functional.");
		e.printStackTrace();
	}
}

/**
 * Prints a string to the console with a more graceful "typing" effect.
 * @param text The text to print.
 * @param delayInMillis The delay between each word.
 */
private static void printWithTypingEffect(String text, int delayInMillis) {
	// The progress bar has already cleared the line. We start printing.
	System.out.print("Xavier: ");
	String[] words = text.split("\\s+");
	for (int i = 0; i < words.length; i++) {
		System.out.print(words[i]);
		if (i < words.length - 1) { // Add a space only if it's not the last word
			System.out.print(" ");
		}
		System.out.flush(); // Ensure the word is printed immediately
		try {
			Thread.sleep(delayInMillis);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt(); // Restore the interrupted status
			System.err.println("Typing effect was interrupted.");
			break; // Exit the loop if interrupted
		}
	}
	System.out.println(); // Move to the next line for the user's input
}

public static void main(String[] args) {
	XavierCoreV2 xavier = new XavierCoreV2();
	xavier.train("training_data.txt");
	
	if (xavier.isTrained) {
		System.out.println("\n--- Xavier is ready. Ask a question or say 'exit' to quit. ---");
		ConversationContext conversation = new ConversationContext();
		// Create a single-threaded executor to run our AI logic
		ExecutorService executor = Executors.newSingleThreadExecutor();
		
		try (Scanner scanner = new Scanner(System.in)) {
			while (true) {
				System.out.print("You: ");
				String input = scanner.nextLine();
				
				if ("exit".equalsIgnoreCase(input)) {
					String goodbyeResponse = xavier.getResponse("bye", conversation);
					printWithTypingEffect(goodbyeResponse, TYPING_DELAY_MS);
					break;
				}
				
				// --- NEW Progress Bar Logic ---
				ProgressBar progressBar = new ProgressBar();
				Thread progressThread = new Thread(progressBar);
				if (!DEBUG_MODE) { // Only show the progress bar if not in debug mode
					progressThread.start();
				}
				
				// Run getResponse asynchronously
				CompletableFuture<String> futureResponse = CompletableFuture.supplyAsync(() ->
						                                                                         xavier.getResponse(input, conversation), executor
				);
				
				// Wait for the response and then handle the result
				futureResponse.whenComplete((response, throwable) -> {
					if (!DEBUG_MODE) {
						progressBar.stop(); // Stop the animation
						try {
							progressThread.join(); // Wait for the progress thread to finish cleaning up the line
						} catch (InterruptedException e) {
							Thread.currentThread().interrupt();
						}
					}
					
					if (throwable != null) {
						// Handle any exceptions from the AI core
						printWithTypingEffect("I seem to have encountered an internal error. Please try again.", TYPING_DELAY_MS);
						System.err.println("Error in getResponse: " + throwable.getCause());
					} else {
						// Print the final response with the typing effect
						printWithTypingEffect(response, TYPING_DELAY_MS);
					}
				});
				
				// Block here to wait for the future to complete before asking for new input
				futureResponse.join();
			}
		} finally {
			executor.shutdown(); // Clean up the executor service
		}
	} else {
		System.out.println("\n--- Xavier could not be started due to a training error. ---");
	}
}
}