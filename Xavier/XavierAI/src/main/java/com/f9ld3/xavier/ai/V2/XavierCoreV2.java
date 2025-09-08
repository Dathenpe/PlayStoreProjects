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
import java.util.Set;

/**
 * The central core of the Xavier AI, version 2.
 * REFACTORED: Now orchestrates a context-stacking architecture for advanced,
 * interruptible conversations, with integrated sentiment analysis.
 */
public class XavierCoreV2 {

// --- Dependencies ---
private final NaiveBayesClassifier classifier;
private final FuzzyMatcher fuzzyMatcher;
private final Map<String, IntentHandler> intentHandlers;
private final Map<String, String> directMatches;
private final PatternHandler patternHandler;
private final SentimentAnalysisService sentimentService; // NEW: For sentiment analysis
private WolframAlphaClient wolframAlphaClient;
private Properties apiProperties;

// --- State ---
private boolean isTrained = false;

// --- Configuration ---
public static final boolean DEBUG_MODE = false;
private static final double CONFIDENCE_THRESHOLD = 0.5;
private static final double FUZZY_RESCUE_THRESHOLD = 0.75;
private static final double DIRECT_MATCH_FUZZY_THRESHOLD = 0.80;

// UPDATED: Removed "riddle_query" to give the RiddleHandler full control of its state.
private static final Set<String> STATEFUL_INTENTS = Set.of(
		"knowledge_query", "list_query", "how_to_query"
);

// NEW: A small record to cleanly pass prediction results from the pipeline.
private record Prediction(String intent, String entity) {
}

public XavierCoreV2() {
	this.classifier = new NaiveBayesClassifier();
	this.fuzzyMatcher = new FuzzyMatcher();
	this.intentHandlers = new HashMap<>();
	this.directMatches = new HashMap<>();
	this.patternHandler = new PatternHandler();
	this.sentimentService = new SentimentAnalysisService(); // NEW
	
	// These methods must be called in order
	loadApiKeysAndClients();
	registerHandlers();
	registerDirectMatches();
	registerPatterns();
}

public boolean isTrained() {
	return isTrained;
}

/**
 * REFACTORED: Processes user input using the new context-stacking pipeline.
 * It intelligently handles multi-turn conversations and interruptions.
 */
public String getResponse(String userInput, ConversationContext context) {
	if (userInput == null || userInput.isBlank()) {
		return ResponseGenerator.getIntelligentFallback();
	}
	if (!isTrained) {
		return "I'm sorry, I haven't been trained yet. Please train me first.";
	}
	
	// --- Pipeline Step 1: Update Context ---
	context.setLastUserInput(userInput);
	double sentiment = sentimentService.getSentimentScore(userInput);
	context.setLastSentimentScore(sentiment);
	if (DEBUG_MODE) System.out.printf("[DEBUG] Sentiment Score: %.2f%n", sentiment);
	
	// --- Pipeline Step 2: Predict Intent and Entity ---
	Prediction prediction = runPredictionPipeline(userInput);
	String predictedIntent = prediction.intent();
	String entity = prediction.entity();
	if (DEBUG_MODE) System.out.printf("[DEBUG] Predicted Intent: %s, Entity: %s%n", predictedIntent, entity);
	
	// --- Pipeline Step 3: Manage Context Stack ---
	Optional<String> activeIntentOpt = context.getCurrentIntent();
	boolean isNewStatefulTopic = STATEFUL_INTENTS.contains(predictedIntent) &&
			                             activeIntentOpt.map(active -> !active.equals(predictedIntent)).orElse(true);
	
	if (isNewStatefulTopic) {
		if (DEBUG_MODE) System.out.println("[DEBUG] Pushing new stateful context: " + predictedIntent);
		context.pushContext(predictedIntent);
	}
	
	// --- NEW: Add the extracted entity to the current context ---
	if (entity != null) {
		String entityKey = switch (predictedIntent) {
			case "calculator_query" -> "expression";
			case "dictionary_query" -> "term";
			case "set_username" -> "username";
			default -> "subject"; // A sensible default for knowledge, weather, time, etc.
		};
		context.addEntityToCurrentContext(entityKey, entity);
		if (DEBUG_MODE) System.out.printf("[DEBUG] Added entity '%s' to context: '%s'%n", entityKey, entity);
	}
	
	// --- Pipeline Step 4: Select Handler and Execute ---
	// If there's an active context (like a riddle), its handler takes priority.
	// Otherwise, use the handler for the newly predicted intent.
	String intentToHandle = context.getCurrentIntent().orElse(predictedIntent);
	IntentHandler handler = intentHandlers.getOrDefault(intentToHandle, intentHandlers.get("default"));
	
	if (DEBUG_MODE) System.out.printf("[DEBUG] Intent to Handle: %s | Handler: %s%n", intentToHandle, handler.getClass().getSimpleName());
	
	return handler.handle(userInput, context);
}

/**
 * REFACTORED: Encapsulates the intent recognition process.
 * Its sole responsibility is now to return the best-guess intent and any extracted entity.
 */
private Prediction runPredictionPipeline(String userInput) {
	String cleanedInput = userInput.toLowerCase().trim().replaceAll("[\\p{Punct}]", "");
	
	// Step 1: Direct Match
	String directMatchIntent = directMatches.get(cleanedInput);
	if (directMatchIntent != null) {
		if (DEBUG_MODE) System.out.printf("[DEBUG] Pipeline: Direct match. Intent: %s%n", directMatchIntent);
		return new Prediction(directMatchIntent, null);
	}
	
	// Step 2: Fuzzy Direct Match
	Optional<String> fuzzyDirectMatch = FuzzyMatcher.findBestCandidate(cleanedInput, directMatches.keySet(), DIRECT_MATCH_FUZZY_THRESHOLD);
	if (fuzzyDirectMatch.isPresent()) {
		String intent = directMatches.get(fuzzyDirectMatch.get());
		if (DEBUG_MODE) System.out.printf("[DEBUG] Pipeline: Fuzzy Direct match. Intent: %s%n", intent);
		return new Prediction(intent, null);
	}
	
	// Step 3: Pattern Match
	Optional<IntentMatch> patternMatch = patternHandler.match(cleanedInput);
	if (patternMatch.isPresent()) {
		IntentMatch match = patternMatch.get();
		String intent = match.getIntent();
		String entity = match.getEntity();
		
		// Special case for calculator where the whole input might be the expression
		if (entity == null && "calculator_query".equals(intent)) {
			entity = userInput;
		}
		
		if (DEBUG_MODE) System.out.printf("[DEBUG] Pipeline: Pattern match. Intent: %s, Entity: %s%n", intent, entity);
		return new Prediction(intent, entity);
	}
	
	// Step 4: Statistical Classifier
	List<String> tokens = TextProcessor.tokenize(userInput);
	if (!tokens.isEmpty()) {
		PredictionResult result = classifier.predict(tokens);
		if (result.getConfidence() >= CONFIDENCE_THRESHOLD) {
			String intent = result.getPredictedLabel();
			if (DEBUG_MODE) System.out.printf("[DEBUG] Pipeline: Classifier success. Intent: %s%n", intent);
			return new Prediction(intent, null);
		}
	}
	
	// Step 5: Fuzzy Rescue
	Optional<MatchResult> fuzzyResult = fuzzyMatcher.findBestMatch(userInput, FUZZY_RESCUE_THRESHOLD);
	if (fuzzyResult.isPresent()) {
		String intent = fuzzyResult.get().intent();
		if (DEBUG_MODE) System.out.printf("[DEBUG] Pipeline: Fuzzy Rescue. Intent: %s%n", intent);
		return new Prediction(intent, null);
	}
	
	// Final fallback
	return new Prediction("default", null);
}

/**
 * REFINED: A "best-of-both-worlds" pattern registration.
 * This combines the robust ordering from the previous fix with the explicit
 * conversational patterns from your older version for maximum accuracy.
 * The registration order is critical: from most specific to most general.
 */
private void registerPatterns() {
	// The registration order is critical: from most specific to most general.
	// Anchors (^ and $) are used to ensure patterns match the whole input for precision.
	
	// --- Group 1: High-Priority Conversational Flow & User Management ---
	// These should be checked first as they direct the conversation.
	patternHandler.registerPattern("set_username", "(?i)^(?:my name is|call me|please call me|you can call me)\\s+(.+)$");
	patternHandler.registerPattern("get_username", "(?i)^(?:what is|what's|do you know) my name\\??$");
	patternHandler.registerPattern("correction", "(?i)^(?:no,?|nope,?|actually,?|i mean|i meant|what i meant was|i meant to say|no i meant|no i said)\\s*(.+)$");
	
	// --- Group 2: Specific Tools & Entertainment (High Priority) ---
	// These are unambiguous commands.
	patternHandler.registerPattern("joke_query", "(?i)^(?:tell me|give me|i want to hear) (?:a|another|\\d+)?\\s*joke$");
	patternHandler.registerPattern("riddle_query", "(?i)^(?:tell me a|ask me a|give me a)?\\s*riddle$");
	patternHandler.registerPattern("fact_query", "(?i)^(?:tell me|give me) (?:a|another)?\\s*(?:fun|interesting|random)?\\s*fact$");
	patternHandler.registerPattern("internet_status_query", "(?i)^(?:check|what's) (?:your|the) (?:internet|network) (?:status|connection)|are you online\\??$");
	
	// --- Group 3: Tool-like Queries with potential keyword overlap ---
	// These often start with "what is" but have specific keywords that give them priority over general knowledge.
	patternHandler.registerPattern("dictionary_query", "(?i)^(?:what is|what's|what does)?(?: the)? (?:meaning of|definition of) (?:the word )?(.+?)(?: mean)?\\??$");
	patternHandler.registerPattern("dictionary_query", "(?i)^(?:define) (?:the word )?(.+)$");
	patternHandler.registerPattern("time_query", "(?i)^(?:what's|what is|tell me) (?:the )?time$");
	patternHandler.registerPattern("time_query", "(?i)^(?:what's|what is|tell me) (?:the )?time (?:in|for|at) (.+)$");
	patternHandler.registerPattern("time_query", "(?i)^(?:what's|what is) (?:the )?timezone (?:in|for|at) (.+)$");
	patternHandler.registerPattern("weather_query", "(?i)^(?:what's|what is|how's|tell me|check) (?:the )?(?:weather|forecast|temperature)$");
	patternHandler.registerPattern("weather_query", "(?i)^(?:what's|what is|how's|tell me|check) (?:the )?(?:weather|forecast|temperature) (?:in|for|at) (.+)$");
	
	// FIX: This pattern is now much more specific. It requires the presence of at least one digit or a math-related word.
	// This prevents it from greedily matching general "what is" questions.
	patternHandler.registerPattern("calculator_query", "(?i)^(?:calculate|compute|what is|what's)\\s+(.*(?:\\d|plus|minus|times|divided|root|power|percent|\\^|\\*|\\/|\\+|-).*)$");
	patternHandler.registerPattern("calculator_query", "^[\\d\\s()+\\-*/.^x]+$");
	
	// --- Group 4: User Status & Follow-up Queries ---
	// FIX: Added more emotional states to prevent them from falling through to the default handler.
	String userStatusKeywords = "bored|boring|hungry|hungey|starving|famished|sad|unhappy|down|depressed|tired|sleepy|exhausted|happy|excited|great|fantastic|curious|confused|worried|scared";
	patternHandler.registerPattern("user_status_query", "(?i)^(?:i am|i'm|i feel|feeling|that's|this is)\\s+(" + userStatusKeywords + ")$");
	patternHandler.registerPattern("user_status_query", "(?i)^(entertain me|cheer me up|i need food)$");
	
	// Generic follow-up phrases that rely on an active context.
	patternHandler.registerPattern("follow_up", "(?i)^(and )?(what|how) about (him|her|it|them|there)\\??$");
	patternHandler.registerPattern("follow_up", "(?i)^(tell me more|go on|what else|more details|can you elaborate|and its population|and its history|tell me another)\\??$");
	
	// Follow-up for 'how_to_query' specifically. The handler will differentiate this from a new query.
	patternHandler.registerPattern("how_to_query", "(?i)^(try another|another one|next one|show me another|give me another|more info)$");
	
	// FIX: New patterns to handle list queries that start with "tell me"
	patternHandler.registerPattern("list_query", "(?i)^(?:tell me|give me|show me) (?:a |\\d+ )?(?:common |popular |top )?(.+)$");
	
	// --- Group 5: General Information Queries (Broadest, Lowest Priority) ---
	// These are the final catch-alls. They must be registered last.
	patternHandler.registerPattern("how_to_query", "(?i)^(?:xavier\\s+)?(?:how to|how do i|explain how to|what are the steps to) (.+)$");
	patternHandler.registerPattern("list_query", "(?i)^(?:xavier\\s+)?(?:list of|give me a list of|name some|what are some) (?:the |some |\\d+ )?(?:common |popular |top )?(.+)$");
	
	// This is the final, broad catch-all for general questions, incorporating keywords from your old version.
	// Its low priority prevents it from overriding specific handlers that also use "what is".
	patternHandler.registerPattern("knowledge_query", "(?i)^(?:xavier\\s+)?(?:who is|who are|where is|tell me about|explain|can you tell me about|do you know about|lets talk about|can we discuss) (.+)$");
	patternHandler.registerPattern("knowledge_query", "(?i)^(?:what is|what are|what's) (.+)$");
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
	List<String> serperApiKeys = new ArrayList<>();
	String primarySerperApiKey = getApiKeyFromProperties("serper.apikey");
	String backupSerperApiKey = getApiKeyFromProperties("serper.apikey.backup");
	if (primarySerperApiKey != null) serperApiKeys.add(primarySerperApiKey);
	if (backupSerperApiKey != null) serperApiKeys.add(backupSerperApiKey);
	SearchService searchService = new SearchService(serperApiKeys);
	
	intentHandlers.put("default", new SmartDefaultHandler(this.wolframAlphaClient, searchService));
	
	List<String> weatherApiKeys = new ArrayList<>();
	String primaryWeatherKey = getApiKeyFromProperties("openweathermap.apikey");
	String backupWeatherKey = getApiKeyFromProperties("openweathermap.apikey.backup");
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
	
	// FIX: Register the RiddleHandler for both its initial query and its confirmation state.
	// This ensures the same handler manages the entire conversation flow.
	RiddleService riddleService = new RiddleService();
	RiddleHandler riddleHandler = new RiddleHandler(riddleService);
	intentHandlers.put("riddle_query", riddleHandler);
	intentHandlers.put("riddle_confirmation", riddleHandler);
	
	// FIX: The FunFactService constructor is now parameterless.
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
	// FIX: Add "good" to prevent it from being misclassified as a dictionary query.
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
	directMatches.put("yh", "confirmation");
	directMatches.put("no", "confirmation");
	directMatches.put("nope", "confirmation");
	// FIX: Add a direct match for the riddle follow-up to ensure it's classified correctly.
	directMatches.put("yes please", "confirmation");
	
	// FIX: Add direct matches for goodbye to make it more robust.
	directMatches.put("bye", "goodbye");
	directMatches.put("exit", "goodbye");
	directMatches.put("quit", "goodbye");
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