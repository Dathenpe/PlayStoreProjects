// C:/Users/Music_Minister/Desktop/PlayStore/PlayStoreProjects/Xavier/XavierAI/src/main/java/com/f9ld3/xavier/ai/V2/XavierCoreV2.java
package com.f9ld3.xavier.ai.V2;

import com.f9ld3.xavier.ai.V2.FuzzyMatcher.MatchResult;
import com.f9ld3.xavier.ai.V2.handlers.*;
import com.f9ld3.xavier.ai.V2.handlers.PatternHandler.IntentMatch;
import com.f9ld3.xavier.ai.V2.services.*;
import com.f9ld3.xavier.ai.V2.utils.ProgressBar;
import com.f9ld3.xavier.ai.V2.utils.ResponseGenerator;

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
 * Processes user input using a re-architected, multi-layered approach for maximum accuracy and intelligence.
 * The pipeline prioritizes specific, internal handlers before falling back to general knowledge APIs.
 */
public String getResponse(String userInput, ConversationContext context) {
	// --- Add a guard clause for empty or whitespace-only input ---
	if (userInput == null || userInput.isBlank()) {
		// Immediately return a helpful fallback message without processing further.
		// This prevents unpredictable behavior and improves efficiency.
		return ResponseGenerator.getIntelligentFallback();
	}
	
	if (!isTrained) {
		return "I'm sorry, I haven't been trained yet. Please train me first.";
	}
	context.setLastUserInput(userInput);
	// --- RE-ARCHITECTED REASONING PIPELINE ---
	
	// STEP 1: Check for a pending intent (e.g., asking for a location after a weather query)
	String pendingIntent = context.getPendingIntent();
	if (pendingIntent != null) {
		if (DEBUG_MODE) System.out.printf("[DEBUG] Pipeline Step 1: Found pending intent '%s'.%n", pendingIntent);
		IntentHandler handler = intentHandlers.get(pendingIntent);
		if (handler != null) {
			context.clearPendingIntent();
			return handler.handle(userInput, context);
		}
		context.clearPendingIntent(); // Clean up even if handler is missing
	}
	
	String predictedIntent = null;
	String cleanedInput = userInput.toLowerCase().trim();
	
	// STEP 2: Direct Match (for simple, exact commands like "hi" or "thanks")
	String directMatchIntent = directMatches.get(cleanedInput);
	if (directMatchIntent != null) {
		predictedIntent = directMatchIntent;
		if (DEBUG_MODE) System.out.printf("[DEBUG] Pipeline Step 2: Direct match found. Intent: %s%n", predictedIntent);
	}
	
	// STEP 3: Fuzzy Direct Match (for typos of simple commands, e.g., "helo")
	if (predictedIntent == null) {
		Optional<String> fuzzyDirectMatch = FuzzyMatcher.findBestCandidate(cleanedInput, directMatches.keySet(), DIRECT_MATCH_FUZZY_THRESHOLD);
		if (fuzzyDirectMatch.isPresent()) {
			String matchedKey = fuzzyDirectMatch.get();
			predictedIntent = directMatches.get(matchedKey);
			if (DEBUG_MODE) System.out.printf("[DEBUG] Pipeline Step 3: Fuzzy Direct match found for '%s' -> '%s'. Intent: %s%n", cleanedInput, matchedKey, predictedIntent);
		}
	}
	
	// STEP 4: Pattern Match (for structured queries like "what is the weather in japan")
	if (predictedIntent == null) {
		Optional<IntentMatch> patternMatch = patternHandler.match(cleanedInput);
		if (patternMatch.isPresent()) {
			IntentMatch match = patternMatch.get();
			predictedIntent = match.getIntent();
			// This allows new patterns (like the calculator) to work without modifying this block again.
			if (match.getEntity() != null) {
				String entityKey;
				if (predictedIntent.contains("weather") || predictedIntent.contains("time")) {
					entityKey = "location";
				} else if (predictedIntent.contains("username")) {
					entityKey = "username";
				} else {
					// Default to using the intent name as the key (e.g., "calculator_query")
					entityKey = predictedIntent;
				}
				context.setEntity(entityKey, match.getEntity());
			} else if ("calculator_query".equals(predictedIntent)) {
				// For patterns that match the whole string (like "2+2"), the entity is the input itself.
				context.setEntity("calculator_query", userInput);
			}
			
			if (DEBUG_MODE) System.out.printf("[DEBUG] Pipeline Step 4: Pattern match found. Intent: %s, Entity: %s%n", predictedIntent, match.getEntity());
		}
	}
	
	// STEP 5: Statistical Classifier (for general conversational intents like "tell me a joke")
	double confidence = 0.0;
	if (predictedIntent == null) {
		List<String> tokens = TextProcessor.tokenize(userInput);
		if (!tokens.isEmpty()) {
			PredictionResult result = classifier.predict(tokens);
			predictedIntent = result.getPredictedLabel();
			confidence = result.getConfidence();
			if (DEBUG_MODE) System.out.printf("[DEBUG] Pipeline Step 5: Classifier result. Intent: %s, Confidence: %.2f%%%n", predictedIntent, confidence * 100);
			
			// If confidence is too low, we don't trust the result and will try other methods.
			if (confidence < CONFIDENCE_THRESHOLD) {
				predictedIntent = null;
			}
		}
	}
	
	// STEP 6: Fuzzy Rescue (if classifier was uncertain, try to find a close phrase match)
	if (predictedIntent == null) {
		if (DEBUG_MODE) System.out.println("[DEBUG] Pipeline Step 6: Classifier confidence low. Attempting Fuzzy Rescue...");
		Optional<MatchResult> fuzzyResult = fuzzyMatcher.findBestMatch(userInput, FUZZY_RESCUE_THRESHOLD);
		if (fuzzyResult.isPresent()) {
			MatchResult match = fuzzyResult.get();
			predictedIntent = match.intent();
			if (DEBUG_MODE) System.out.printf("[DEBUG] Fuzzy Rescue success! Matched '%s' to '%s'. Intent: %s%n", userInput, match.matchedPhrase(), predictedIntent);
		}
	}
	
	// STEP 7: Knowledge Query Fallback (The last resort before giving up)
	if (predictedIntent == null) {
		if (DEBUG_MODE) System.out.println("[DEBUG] Pipeline Step 7: All internal handlers failed. Checking Wolfram|Alpha as a fallback.");
		if (this.wolframAlphaClient.canAnswer(cleanedInput)) {
			predictedIntent = "knowledge_query";
		}
	}
	// STEP 8: Sanity Check for Tool-Based Intents
	// This acts as a final guardrail to prevent the classifier from confidently
	// misrouting a query to a specific tool if the query is missing the tool's trigger word.
	if ("time_query".equals(predictedIntent) && !cleanedInput.contains("time")) {
		if (DEBUG_MODE) System.out.printf("[DEBUG] Sanity Check: Rejecting intent '%s' because trigger word 'time' is missing. Falling back.%n", predictedIntent);
		predictedIntent = null; // Reject the classifier's prediction
	}
	if ("weather_query".equals(predictedIntent) && !cleanedInput.contains("weather") && !cleanedInput.contains("forecast") && !cleanedInput.contains("temperature")) {
		if (DEBUG_MODE) System.out.printf("[DEBUG] Sanity Check: Rejecting intent '%s' because trigger words are missing. Falling back.%n", predictedIntent);
		predictedIntent = null; // Reject the classifier's prediction
	}
	
	// If the sanity check rejected the intent, try the knowledge fallback one last time.
	if (predictedIntent == null) {
		if (DEBUG_MODE) System.out.println("[DEBUG] Sanity Check failed. Re-checking Wolfram|Alpha as a final fallback.");
		if (this.wolframAlphaClient.canAnswer(cleanedInput)) {
			predictedIntent = "knowledge_query";
		}
	}
	
	
	// This prevents stale results if the user changes topics.
	if (!"how_to_query".equals(predictedIntent)) {
		context.clearSearchContext();
	} else {
		// If the intent IS how_to_query, we need to check if it's a *new* search.
		// If it is, we also clear the context to start fresh.
		boolean isNewSearch = userInput.matches("(?i)(?:xavier\\s+)?(?:how to|how do i|tell me how to|explain how to|what are the steps to)\\s*(.+)");
		if (isNewSearch) {
			context.clearSearchContext();
		}
	}
	
	// --- Handler Selection and Execution ---
	IntentHandler handler = intentHandlers.getOrDefault(predictedIntent, intentHandlers.get("default"));
	String response = handler.handle(userInput, context);
	
	// --- Context Management ---
	context.setLastIntent(predictedIntent);
	return response;
}

private void registerPatterns() {
	// --- User Management ---
	patternHandler.registerPattern("set_username", "(?i)(?:my name is|call me) (.+)");
	patternHandler.registerPattern("get_username", "(?i)(?:what is|what's|do you know) my name\\??");
	
	// --- Add a pattern for pronoun-based follow-up questions ---
	// This catches questions that are clearly dependent on context by looking for a question
	// word at the start and a pronoun later in the sentence.
	patternHandler.registerPattern("follow_up", "(?i)^(what|where|when|how|why|is|are|was|were|do|does|did) .*\\b(he|she|it|they|his|her|its|their|there)\\b.*");
	
	patternHandler.registerPattern("correction", "(?i)(?:i mean|i meant|no, i mean|no, i meant|no,)\\s*(.+)");
	
	// --- Knowledge & Information ---
	// "how to" is more specific than a generic "tell me"
	patternHandler.registerPattern("how_to_query", "(?i)(?:xavier\\s+)?(?:how to|how do i|tell me how to|explain how to|what are the steps to) (.+)");
	
	// "list of" or "what are" are strong signals for a list
	patternHandler.registerPattern("list_query", "(?i)(?:xavier\\s+)?(?:list of|tell me|give me|name|what are) (?:the |some |\\d+ )?(?:common |popular |top )?(.+)");
	
	// More flexible patterns for general knowledge queries
	patternHandler.registerPattern("knowledge_query", "(?i)(?:xavier\\s+)?(?:tell me about|what is|what's|explain|can you tell me about|do you know about|lets talk about|can we discuss) (.+)");
	
	// This refinement query does not need the prefix as it's a direct command.
	patternHandler.registerPattern("how_to_query", "(?i)^(try another|another one|next one|more info|more details|show me another|give me another)$");
	
	patternHandler.registerPattern("dictionary_query", "(?i)(?:define|meaning of|what does)(?: the word)? (.+?)(?: mean)?$");
	
	// --- Internal Tools ---
	patternHandler.registerPattern("calculator_query", "(?i)(?:what is|what's|calculate|compute|the sum of) (.*(\\d|plus|minus|times|divided|root|squared|power|percent).*)");
	patternHandler.registerPattern("calculator_query", "^[\\d\\s\\+\\-\\*\\/\\(\\)\\^\\.x]+$");
	
	// --- Location-Based Services (Time & Weather) ---
	patternHandler.registerPattern("weather_query", "(?i)(?:what's|how's|tell me|check) (?:the )?(?:weather|forecast|temperature)$");
	patternHandler.registerPattern("weather_query", "(?i)(?:what's|how's|tell me|check) (?:the )?(?:weather|forecast|temperature) (?:in|for|at) (.+)");
	patternHandler.registerPattern("time_query", "(?i)(?:what's|what is|tell me) (?:the )?time$");
	patternHandler.registerPattern("time_query", "(?i)(?:what's|what is|tell me) (?:the )?time (?:in|for|at) (.+)");
	patternHandler.registerPattern("time_query", "(?i)(?:what's|what is) (?:the )?timezone (?:in|for|at) (.+)");
	
	patternHandler.registerPattern("internet_status_query", "(?i)(?:what is|what's|check|do you have)(?: an?| your| my| the)?(?: current)? (?:internet|network) (?:status|connection)|are you online\\??");
	// --- Entertainment ---
	patternHandler.registerPattern("joke_query", "(?i)(?:tell me|give me|i want to hear) (?:a|another|\\d+)?\\s*joke(?:s)?");
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
		String tertiaryId = getApiKeyFromProperties("wolframalpha.appid.tertiary");
		
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
	intentHandlers.put("internet_status_query", new InternetStatusHandler());
	
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
	
	List<String> serperApiKeys = new ArrayList<>();
	String primarySerperApiKey = getApiKeyFromProperties("serper.apikey");
	String backupSerperApiKey = getApiKeyFromProperties("serper.apikey.backup");
	
	if (primarySerperApiKey != null)serperApiKeys.add(primarySerperApiKey);
	if (backupSerperApiKey != null)serperApiKeys.add(backupSerperApiKey);
	
	SearchService searchService = new SearchService(serperApiKeys);
	intentHandlers.put("how_to_query", new HowToQueryHandler(searchService));
	
	String generativeApiUrl = getApiKeyFromProperties("generative.api.url");
	String generativeModel = getApiKeyFromProperties("generative.api.model");
	
	List<String> generativeApiKeys = new ArrayList<>();
	String primaryGenerativeKey = getApiKeyFromProperties("generative.api.key");
	String backupGenerativeKey = getApiKeyFromProperties("generative.api.key.backup");
	
	if (primaryGenerativeKey != null) generativeApiKeys.add(primaryGenerativeKey);
	if (backupGenerativeKey != null) generativeApiKeys.add(backupGenerativeKey);
	
	GenerativeService generativeService = new GenerativeService(generativeApiUrl, generativeApiKeys, generativeModel);
	
	// The ListQueryHandler now uses both services
	intentHandlers.put("list_query", new ListQueryHandler(generativeService, searchService));
	
	
	intentHandlers.put("knowledge_query", new KnowledgeQueryHandler(this.wolframAlphaClient, searchService));
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
	
	directMatches.put("what is your name", "about_bot");
	directMatches.put("what's your name", "about_bot");
	directMatches.put("who are you", "about_bot");
	directMatches.put("what are you", "about_bot");
	directMatches.put("tell me about yourself", "about_bot");
	
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

private static void printWithTypingEffect(String text, int delayInMillis) {
	System.out.print("Xavier: ");
	String[] words = text.split("\\s+");
	for (int i = 0; i < words.length; i++) {
		System.out.print(words[i]);
		if (i < words.length - 1) {
			System.out.print(" ");
		}
		System.out.flush();
		try {
			Thread.sleep(delayInMillis);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			System.err.println("Typing effect was interrupted.");
			break;
		}
	}
	System.out.println();
}

public static void main(String[] args) {
	XavierCoreV2 xavier = new XavierCoreV2();
	xavier.train("training_data.txt");
	
	if (xavier.isTrained) {
		System.out.println("\n--- Xavier is ready. Ask a question or say 'exit' to quit. ---");
		ConversationContext conversation = new ConversationContext();
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
				
				ProgressBar progressBar = new ProgressBar();
				Thread progressThread = new Thread(progressBar);
				if (!DEBUG_MODE) {
					progressThread.start();
				}
				
				CompletableFuture<String> futureResponse = CompletableFuture.supplyAsync(() ->
						                                                                         xavier.getResponse(input, conversation), executor
				);
				
				futureResponse.whenComplete((response, throwable) -> {
					if (!DEBUG_MODE) {
						progressBar.stop();
						try {
							progressThread.join();
						} catch (InterruptedException e) {
							Thread.currentThread().interrupt();
						}
					}
					
					if (throwable != null) {
						printWithTypingEffect("I seem to have encountered an internal error. Please try again.", TYPING_DELAY_MS);
						System.err.println("Error in getResponse: " + throwable.getCause());
					} else {
						printWithTypingEffect(response, TYPING_DELAY_MS);
					}
				});
				
				futureResponse.join();
			}
		} finally {
			executor.shutdown();
		}
	} else {
		System.out.println("\n--- Xavier could not be started due to a training error. ---");
	}
}
}