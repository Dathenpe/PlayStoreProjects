// C:/Users/Music_Minister/Desktop/PlayStore/PlayStoreProjects/Xavier/XavierAI/src/main/java/com/f9ld3/xavier/ai/V2/XavierCoreV2.java
package com.f9ld3.xavier.ai.V2;

import com.f9ld3.xavier.ai.V2.FuzzyMatcher.MatchResult;
import com.f9ld3.xavier.ai.V2.handlers.*;
import com.f9ld3.xavier.ai.V2.handlers.PatternHandler.IntentMatch;
import com.f9ld3.xavier.ai.V2.services.*;
import com.f9ld3.xavier.ai.V2.utils.ResponseGenerator;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

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
private Properties apiProperties; // Caches API properties to avoid redundant file reads

// --- State ---
private boolean isTrained = false;

// --- Configuration ---
public static final boolean DEBUG_MODE = false; // Set to true to see debug logs
private static final double CONFIDENCE_THRESHOLD = 0.5;
private static final double FUZZY_RESCUE_THRESHOLD = 0.75;
private static final double DIRECT_MATCH_FUZZY_THRESHOLD = 0.80; // High threshold for common word typos

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
 * Public getter to check if the model has been trained.
 * This allows an external application to know if the core is ready.
 * @return true if the model is trained, false otherwise.
 */
public boolean isTrained() {
	return isTrained;
}

/**
 * REFACTORED: Processes user input using a more powerful, context-aware pipeline.
 * It intelligently handles multi-turn conversations, allowing users to continue a flow or break out of it naturally.
 */
public String getResponse(String userInput, ConversationContext context) {
	// --- Guard clause for empty or whitespace-only input ---
	if (userInput == null || userInput.isBlank()) {
		return ResponseGenerator.getIntelligentFallback();
	}
	
	if (!isTrained) {
		return "I'm sorry, I haven't been trained yet. Please train me first.";
	}
	context.setLastUserInput(userInput);
	
	// --- HIGHEST PRIORITY: Check for active, multi-turn contexts like riddles ---
	if (context.getLastRiddleAnswer() != null || "riddle_confirmation".equals(context.getPendingIntent())) {
		IntentHandler riddleHandler = intentHandlers.get("riddle_query");
		String currentIntent = runPredictionPipeline(userInput, context);
		
		// If the user asks for something completely different, break the riddle flow.
		boolean isBreakingFlow = currentIntent != null &&
				                         !currentIntent.equals("riddle_query") &&
				                         !currentIntent.equals("confirmation") &&
				                         !currentIntent.equals("default");
		
		if (isBreakingFlow) {
			if (DEBUG_MODE) System.out.println("[DEBUG] User interrupted riddle with new intent: " + currentIntent);
			context.clearRiddleContext();
			context.clearPendingIntent();
			// Let the request fall through to the standard pipeline below.
		} else {
			// The input is a guess, a "give up", or a "yes/no". The RiddleHandler will figure it out.
			if (DEBUG_MODE) System.out.println("[DEBUG] Routing to RiddleHandler for active riddle context.");
			return riddleHandler.handle(userInput, context);
		}
	}
	
	// --- STANDARD PIPELINE for new requests or requests that broke a flow ---
	String predictedIntent = runPredictionPipeline(userInput, context);
	
	// This prevents stale results if the user changes topics.
	if (!"how_to_query".equals(predictedIntent)) {
		context.clearSearchContext();
	} else {
		// If the intent IS how_to_query, we need to check if it's a *new* search.
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

/**
 * Encapsulates the entire multi-step intent recognition process.
 * This allows the main getResponse method to be cleaner and more focused on flow control.
 *
 * @param userInput The raw user input.
 * @param context   The current conversation context.
 * @return The final predicted intent as a string.
 */
private String runPredictionPipeline(String userInput, ConversationContext context) {
	String predictedIntent = null;
	String cleanedInput = userInput.toLowerCase().trim();
	
	// STEP 1: Direct Match (for simple, exact commands like "hi" or "thanks")
	String directMatchIntent = directMatches.get(cleanedInput);
	if (directMatchIntent != null) {
		predictedIntent = directMatchIntent;
		if (DEBUG_MODE) System.out.printf("[DEBUG] Pipeline Step 1: Direct match found. Intent: %s%n", predictedIntent);
	}
	
	// STEP 2: Fuzzy Direct Match (for typos of simple commands, e.g., "helo")
	if (predictedIntent == null) {
		Optional<String> fuzzyDirectMatch = FuzzyMatcher.findBestCandidate(cleanedInput, directMatches.keySet(), DIRECT_MATCH_FUZZY_THRESHOLD);
		if (fuzzyDirectMatch.isPresent()) {
			String matchedKey = fuzzyDirectMatch.get();
			predictedIntent = directMatches.get(matchedKey);
			if (DEBUG_MODE) System.out.printf("[DEBUG] Pipeline Step 2: Fuzzy Direct match found for '%s' -> '%s'. Intent: %s%n", cleanedInput, matchedKey, predictedIntent);
		}
	}
	
	// STEP 3: Pattern Match (for structured queries like "what is the weather in japan")
	if (predictedIntent == null) {
		Optional<IntentMatch> patternMatch = patternHandler.match(cleanedInput);
		if (patternMatch.isPresent()) {
			IntentMatch match = patternMatch.get();
			predictedIntent = match.getIntent();
			if (match.getEntity() != null) {
				String entityKey;
				if (predictedIntent.contains("weather") || predictedIntent.contains("time")) {
					entityKey = "location";
				} else if (predictedIntent.contains("username")) {
					entityKey = "username";
				} else {
					entityKey = predictedIntent;
				}
				context.setEntity(entityKey, match.getEntity());
			} else if ("calculator_query".equals(predictedIntent)) {
				context.setEntity("calculator_query", userInput);
			}
			if (DEBUG_MODE) System.out.printf("[DEBUG] Pipeline Step 3: Pattern match found. Intent: %s, Entity: %s%n", predictedIntent, match.getEntity());
		}
	}
	
	// STEP 4: Statistical Classifier (for general conversational intents like "tell me a joke")
	if (predictedIntent == null) {
		List<String> tokens = TextProcessor.tokenize(userInput);
		if (!tokens.isEmpty()) {
			PredictionResult result = classifier.predict(tokens);
			double confidence = result.getConfidence();
			if (DEBUG_MODE) System.out.printf("[DEBUG] Pipeline Step 4: Classifier result. Intent: %s, Confidence: %.2f%%%n", result.getPredictedLabel(), confidence * 100);
			if (confidence >= CONFIDENCE_THRESHOLD) {
				predictedIntent = result.getPredictedLabel();
			}
		}
	}
	
	// STEP 5: Fuzzy Rescue (if classifier was uncertain, try to find a close phrase match)
	if (predictedIntent == null) {
		if (DEBUG_MODE) System.out.println("[DEBUG] Pipeline Step 5: Classifier confidence low. Attempting Fuzzy Rescue...");
		Optional<MatchResult> fuzzyResult = fuzzyMatcher.findBestMatch(userInput, FUZZY_RESCUE_THRESHOLD);
		if (fuzzyResult.isPresent()) {
			MatchResult match = fuzzyResult.get();
			predictedIntent = match.intent();
			if (DEBUG_MODE) System.out.printf("[DEBUG] Fuzzy Rescue success! Matched '%s' to '%s'. Intent: %s%n", userInput, match.matchedPhrase(), predictedIntent);
		}
	}
	
	return predictedIntent;
}

private void registerPatterns() {
	// --- User Management & Conversational Flow ---
	patternHandler.registerPattern("set_username", "(?i)(?:my name is|call me) (.+)");
	patternHandler.registerPattern("get_username", "(?i)(?:what is|what's|do you know) my name\\??");
	patternHandler.registerPattern("follow_up", "(?i)^(what|where|when|how|why|is|are|was|were|do|does|did) .*\\b(he|she|it|they|his|her|its|their|there)\\b.*");
	patternHandler.registerPattern("correction", "(?i)(?:i mean|i meant|no, i mean|no, i meant|no,)\\s*(.+)");
	
	// --- Group 1: Highly Specific Tools & Entertainment ---
	patternHandler.registerPattern("joke_query", "(?i)(?:tell me|give me|i want to hear) (?:a|another|\\d+)?\\s*(joke|something funny)(?:s)?");
	patternHandler.registerPattern("riddle_query", "(?i)(?:tell me a|ask me a|give me a)?\\s*riddl?e?|what's the answer|what is the answer|i give up|i do[n']?t know");
	patternHandler.registerPattern("dictionary_query", "(?i)(?:define|meaning of|what does)(?: the word)? (.+?)(?: mean)?$");
	patternHandler.registerPattern("calculator_query", "(?i)^\\d+\\s+(?:plus|minus|times|divided by)\\s+\\d+.*");
	patternHandler.registerPattern("calculator_query", "(?i)(?:what is|what's|calculate|compute|the sum of) (.*(\\d|plus|minus|times|divided|root|squared|power|percent).*)");
	patternHandler.registerPattern("calculator_query", "^[\\d\\s\\+\\-\\*\\/\\(\\)\\^\\.x]+$");
	patternHandler.registerPattern("internet_status_query", "(?i)(?:what is|what's|check|do you have)(?: an?| your| my| the)?(?: current)? (?:internet|network) (?:status|connection)|are you online\\??");
	
	// --- Group 2: Location-Based Services ---
	patternHandler.registerPattern("weather_query", "(?i)(?:what's|how's|tell me|check) (?:the )?(?:weather|forecast|temperature)$");
	patternHandler.registerPattern("weather_query", "(?i)(?:what's|how's|tell me|check) (?:the )?(?:weather|forecast|temperature) (?:in|for|at) (.+)");
	patternHandler.registerPattern("time_query", "(?i)(?:what's|what is|tell me) (?:the )?time$");
	patternHandler.registerPattern("time_query", "(?i)(?:what's|what is|tell me) (?:the )?time (?:in|for|at) (.+)");
	patternHandler.registerPattern("time_query", "(?i)(?:what's|what is) (?:the )?timezone (?:in|for|at) (.+)");
	
	// --- Group 3: General Information Queries (Broadest) ---
	patternHandler.registerPattern("how_to_query", "(?i)(?:xavier\\s+)?(?:how to|how do i|tell me how to|explain how to|what are the steps to) (.+)");
	patternHandler.registerPattern("how_to_query", "(?i)^(try another|another one|next one|more info|more details|show me another|give me another)$");
	patternHandler.registerPattern("knowledge_query", "(?i)(?:xavier\\s+)?(?:tell me about|what is|what's|who is|where is|explain|can you tell me about|do you know about|lets talk about|can we discuss) (.+)");
	patternHandler.registerPattern("list_query", "(?i)(?:xavier\\s+)?(?:list of|give me a list of|name some|what are some) (?:the |some |\\d+ )?(?:common |popular |top )?(.+)");
}

private void loadApiKeysAndClients() {
	this.apiProperties = new Properties();
	try (InputStream input = XavierCoreV2.class.getClassLoader().getResourceAsStream("api.properties")) {
		if (input == null) {
			System.err.println("FATAL: Unable to find api.properties. API-based functionality will be disabled.");
			this.wolframAlphaClient = new WolframAlphaClient();
			return;
		}
		apiProperties.load(input);
		
		List<String> wolframKeys = new ArrayList<>();
		String primaryId = getApiKeyFromProperties("wolframalpha.appid");
		String backupId = getApiKeyFromProperties("wolframalpha.appid.backup");
		String tertiaryId = getApiKeyFromProperties("wolframalpha.appid.tertiary");
		
		if (primaryId != null) wolframKeys.add(primaryId);
		if (backupId != null) wolframKeys.add(backupId);
		if (tertiaryId != null) wolframKeys.add(tertiaryId);
		
		this.wolframAlphaClient = new WolframAlphaClient(wolframKeys.toArray(new String[0]));
		
	} catch (IOException ex) {
		System.err.println("FATAL: Error loading api.properties.");
		ex.printStackTrace();
		this.wolframAlphaClient = new WolframAlphaClient();
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
	intentHandlers.put("internet_status_query", new InternetStatusHandler());
	
	// --- Group 2: Handlers that require the core instance for re-routing ---
	intentHandlers.put("follow_up", new FollowUpHandler(this));
	intentHandlers.put("correction", new CorrectionHandler(this));
	intentHandlers.put("user_status_query", new UserStatusHandler(this));
	
	// --- Group 3 & 4: API-driven handlers ---
	// Create shared services first
	List<String> serperApiKeys = new ArrayList<>();
	String primarySerperApiKey = getApiKeyFromProperties("serper.apikey");
	String backupSerperApiKey = getApiKeyFromProperties("serper.apikey.backup");
	if (primarySerperApiKey != null) serperApiKeys.add(primarySerperApiKey);
	if (backupSerperApiKey != null) serperApiKeys.add(backupSerperApiKey);
	SearchService searchService = new SearchService(serperApiKeys);
	
	// UPDATED: Register the new SmartDefaultHandler
	intentHandlers.put("default", new SmartDefaultHandler(this.wolframAlphaClient, searchService));
	
	// Register other API handlers
	String primaryWeatherKey = getApiKeyFromProperties("openweathermap.apikey");
	String backupWeatherKey = getApiKeyFromProperties("openweathermap.apikey.backup");
	List<String> weatherApiKeys = new ArrayList<>();
	if (primaryWeatherKey != null) weatherApiKeys.add(primaryWeatherKey);
	if (backupWeatherKey != null) weatherApiKeys.add(backupWeatherKey);
	
	try {
		GeocodingService geocodingService = new GeocodingService(weatherApiKeys);
		IPGeolocationService ipGeolocationService = new IPGeolocationService();
		LocationResolverService locationResolver = new LocationResolverService(geocodingService);
		intentHandlers.put("time_query", new TimeQueryHandler(locationResolver, ipGeolocationService, primaryWeatherKey));
		intentHandlers.put("weather_query", new WeatherQueryHandler(locationResolver, ipGeolocationService, primaryWeatherKey));
	} catch (IllegalArgumentException e) {
		System.err.println("WARN: Location services could not be initialized. Weather and Timezone features will be disabled. Reason: " + e.getMessage());
		IntentHandler unavailableHandler = (userInput, context) -> "I'm sorry, my location-based services are currently unavailable.";
		intentHandlers.put("weather_query", unavailableHandler);
		intentHandlers.put("time_query", unavailableHandler);
	}
	
	DictionaryService dictionaryService = new DictionaryService();
	intentHandlers.put("dictionary_query", new DictionaryHandler(dictionaryService));
	
	JokeService jokeService = new JokeService();
	intentHandlers.put("joke_query", new JokeHandler(jokeService));
	
	RiddleService riddleService = new RiddleService();
	intentHandlers.put("riddle_query", new RiddleHandler(riddleService));
	
	FunFactService funFactService = new FunFactService();
	intentHandlers.put("fact_query", new FunFactHandler(funFactService));
	
	intentHandlers.put("how_to_query", new HowToQueryHandler(searchService));
	
	String generativeApiUrl = getApiKeyFromProperties("generative.api.url");
	String generativeModel = getApiKeyFromProperties("generative.api.model");
	List<String> generativeApiKeys = new ArrayList<>();
	String primaryGenerativeKey = getApiKeyFromProperties("generative.api.key");
	String backupGenerativeKey = getApiKeyFromProperties("generative.api.key.backup");
	if (primaryGenerativeKey != null) generativeApiKeys.add(primaryGenerativeKey);
	if (backupGenerativeKey != null) generativeApiKeys.add(backupGenerativeKey);
	GenerativeService generativeService = new GenerativeService(generativeApiUrl, generativeApiKeys, generativeModel);
	
	intentHandlers.put("list_query", new ListQueryHandler(generativeService, searchService));
	intentHandlers.put("knowledge_query", new KnowledgeQueryHandler(this.wolframAlphaClient, searchService));
}

private String getApiKeyFromProperties(String key) {
	if (apiProperties == null) {
		return null;
	}
	String value = apiProperties.getProperty(key);
	return (value != null && !value.contains("YOUR_") && !value.trim().isEmpty()) ? value : null;
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
	directMatches.put("how do you do", "greeting");
	
	// Bot Identity
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
	directMatches.put("tnx", "gratitude");
	directMatches.put("thx", "gratitude");
	
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
		
		List<List<String>> documents = dataLoader.getDocuments();
		List<String> labels = dataLoader.getLabels();
		classifier.fit(documents, labels);
		System.out.println("Classifier training complete!");
		
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
}