package com.f9ld3.xavier.ai.V2;

import android.content.Context;
import android.util.Log; // For Android logging

// Ensure FuzzyMatcher.MatchResult is accessible. If it's a static inner class of FuzzyMatcher,
// this import is fine. If it's a separate file in that package, also fine.
import com.f9ld3.xavier.ai.V2.FuzzyMatcher.MatchResult;
// Wildcard import for other potential classes in FuzzyMatcher package (if any)
// import com.f9ld3.xavier.ai.V2.FuzzyMatcher.*; // This is okay but less explicit than importing MatchResult directly if that's all you need from the package.

import com.f9ld3.xavier.ai.V2.handlers.AboutBotHandler;
import com.f9ld3.xavier.ai.V2.handlers.CalculatorHandler;
import com.f9ld3.xavier.ai.V2.handlers.ChitChatHandler;
import com.f9ld3.xavier.ai.V2.handlers.ConfirmationHandler;
import com.f9ld3.xavier.ai.V2.handlers.CorrectionHandler;
import com.f9ld3.xavier.ai.V2.handlers.DateQueryHandler;
import com.f9ld3.xavier.ai.V2.handlers.DictionaryHandler;
import com.f9ld3.xavier.ai.V2.handlers.FollowUpHandler;
import com.f9ld3.xavier.ai.V2.handlers.FunFactHandler;
import com.f9ld3.xavier.ai.V2.handlers.GetUsernameHandler;
import com.f9ld3.xavier.ai.V2.handlers.GoodbyeHandler;
import com.f9ld3.xavier.ai.V2.handlers.GratitudeHandler;
import com.f9ld3.xavier.ai.V2.handlers.GreetingHandler;
import com.f9ld3.xavier.ai.V2.handlers.HowToQueryHandler;
import com.f9ld3.xavier.ai.V2.handlers.IntentHandler;
import com.f9ld3.xavier.ai.V2.handlers.InternetStatusHandler;
import com.f9ld3.xavier.ai.V2.handlers.JokeHandler;
import com.f9ld3.xavier.ai.V2.handlers.KnowledgeQueryHandler;
import com.f9ld3.xavier.ai.V2.handlers.ListQueryHandler;
import com.f9ld3.xavier.ai.V2.handlers.PatternHandler;
import com.f9ld3.xavier.ai.V2.handlers.PatternHandler.IntentMatch;
import com.f9ld3.xavier.ai.V2.handlers.RiddleHandler;
import com.f9ld3.xavier.ai.V2.handlers.SetUsernameHandler;
import com.f9ld3.xavier.ai.V2.handlers.SmartDefaultHandler;
import com.f9ld3.xavier.ai.V2.handlers.TimeQueryHandler;
import com.f9ld3.xavier.ai.V2.handlers.UserStatusHandler;
import com.f9ld3.xavier.ai.V2.handlers.WeatherQueryHandler;
import com.f9ld3.xavier.ai.V2.services.DictionaryService;
import com.f9ld3.xavier.ai.V2.services.FunFactService;
import com.f9ld3.xavier.ai.V2.services.GenerativeService;
import com.f9ld3.xavier.ai.V2.services.GeocodingService;
import com.f9ld3.xavier.ai.V2.services.IPGeolocationService;
import com.f9ld3.xavier.ai.V2.services.JokeService;
import com.f9ld3.xavier.ai.V2.services.LocationResolverService;
import com.f9ld3.xavier.ai.V2.services.RiddleService;
import com.f9ld3.xavier.ai.V2.services.SearchService;
import com.f9ld3.xavier.ai.V2.services.SentimentAnalysisService;
import com.f9ld3.xavier.ai.V2.utils.ResponseGenerator;
// Assuming TextProcessor and PredictionResult are your custom classes and correctly defined
// You'll need to ensure these imports are correct if they are in different packages or are inner classes.
// e.g., import com.f9ld3.xavier.ai.V2.nlp.TextProcessor;
// e.g., import com.f9ld3.xavier.ai.V2.classification.PredictionResult;


import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections; // For Collections.unmodifiableSet if needed for STATEFUL_INTENTS
import java.util.HashMap;
import java.util.HashSet; // For Collections.unmodifiableSet if needed for STATEFUL_INTENTS
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

public class XavierCoreV2 {

	private static final String TAG = "XavierCoreV2";

	private final NaiveBayesClassifier classifier;
	private final FuzzyMatcher fuzzyMatcher;
	private final Map<String, IntentHandler> intentHandlers;
	private final Map<String, String> directMatches;
	private final PatternHandler patternHandler;
	private final SentimentAnalysisService sentimentService;

	private WolframAlphaClient wolframAlphaClient;
	private Properties apiProperties;

	private boolean isTrained = false;

	public static final boolean DEBUG_MODE = true;
	private static final double CONFIDENCE_THRESHOLD = 0.5;
	private static final double FUZZY_RESCUE_THRESHOLD = 0.75;
	private static final double DIRECT_MATCH_FUZZY_THRESHOLD = 0.80;

	// For Java 8 compatibility if Set.of() is not available:
	private static final Set<String> STATEFUL_INTENTS;
	static {
		Set<String> tempSet = new HashSet<>();
		tempSet.add("knowledge_query");
		tempSet.add("list_query");
		tempSet.add("how_to_query");
		STATEFUL_INTENTS = Collections.unmodifiableSet(tempSet);
	}
	// If Set.of() IS available (Java 9+ configured):
	// private static final Set<String> STATEFUL_INTENTS = Set.of(
	//         "knowledge_query", "list_query", "how_to_query"
	// );


	public static final class Prediction {
		private final String intent;
		private final String entity;

		public Prediction(String intent, String entity) {
			this.intent = intent;
			this.entity = entity;
		}

		public String getIntent() {
			return intent;
		}

		public String getEntity() {
			return entity;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			Prediction that = (Prediction) o;
			return Objects.equals(intent, that.intent) &&
					Objects.equals(entity, that.entity);
		}

		@Override
		public int hashCode() {
			return Objects.hash(intent, entity);
		}

		@Override
		public String toString() {
			return "Prediction{" +
					"intent='" + intent + '\'' +
					", entity='" + entity + '\'' +
					'}';
		}
	}


	public XavierCoreV2(Context context) {
		this.classifier = new NaiveBayesClassifier();
		this.fuzzyMatcher = new FuzzyMatcher();
		this.intentHandlers = new HashMap<>();
		this.directMatches = new HashMap<>();
		this.patternHandler = new PatternHandler();
		this.sentimentService = new SentimentAnalysisService();

		loadApiKeysAndClients(context);
		registerHandlers(context);
		registerDirectMatches();
		registerPatterns();
	}

	private void loadApiKeysAndClients(Context context) {
		this.apiProperties = new Properties();
		try (InputStream input = context.getAssets().open("api.properties")) {
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
			Log.e(TAG, "FATAL: Error loading api.properties from assets.", ex);
			this.wolframAlphaClient = new WolframAlphaClient(); // Fallback
		}
	}

	private void registerHandlers(Context context) {
		JokeService jokeService = new JokeService(context);
		intentHandlers.put("joke_query", new JokeHandler(jokeService));

		RiddleService riddleService = new RiddleService(context);
		RiddleHandler riddleHandler = new RiddleHandler(riddleService);
		intentHandlers.put("riddle_query", riddleHandler);
		intentHandlers.put("riddle_confirmation", riddleHandler);

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

		intentHandlers.put("follow_up", new FollowUpHandler(this));
		intentHandlers.put("correction", new CorrectionHandler(this));
		intentHandlers.put("user_status_query", new UserStatusHandler(this));

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
			Log.w(TAG, "Location services could not be initialized. Weather and Timezone features will be disabled. Reason: " + e.getMessage());
			IntentHandler unavailableHandler = (userInput, ctx) -> "I'm sorry, my location-based services are currently unavailable.";
			intentHandlers.put("weather_query", unavailableHandler);
			intentHandlers.put("time_query", unavailableHandler);
		}

		DictionaryService dictionaryService = new DictionaryService();
		intentHandlers.put("dictionary_query", new DictionaryHandler(dictionaryService));

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

	public void train(Context context, String assetFileName) {
		DataLoader dataLoader = new DataLoader();
		try {
			Log.i(TAG, "Starting training process...");
			dataLoader.loadDataFromAsset(context, assetFileName);

			List<List<String>> documents = dataLoader.getDocuments();
			List<String> labels = dataLoader.getLabels();
			classifier.fit(documents, labels);
			Log.i(TAG, "Classifier training complete!");

			List<String> rawPhrases = dataLoader.getRawPhrases();
			fuzzyMatcher.train(rawPhrases, labels);
			Log.i(TAG, "Fuzzy Matcher training complete!");

			isTrained = true;
			Log.i(TAG, "Training complete!");

		} catch (IOException e) {
			Log.e(TAG, "FATAL: Failed to train the model. The AI will not be functional.", e);
		}
	}

	public boolean isTrained() {
		return isTrained;
	}

	public String getResponse(String userInput, ConversationContext context) {
		if (userInput == null || userInput.isBlank()) {
			return ResponseGenerator.getIntelligentFallback();
		}
		if (!isTrained) {
			Log.w(TAG, "getResponse called before model is trained.");
			return "I'm sorry, I haven't been trained yet. Please train me first.";
		}

		context.setLastUserInput(userInput);
		double sentiment = sentimentService.getSentimentScore(userInput);
		context.setLastSentimentScore(sentiment);
		if (DEBUG_MODE) Log.d(TAG, String.format("[DEBUG] Sentiment Score: %.2f", sentiment));

		Prediction prediction = runPredictionPipeline(userInput);
		String predictedIntent = prediction.getIntent();
		String entity = prediction.getEntity();
		if (DEBUG_MODE) Log.d(TAG, String.format("[DEBUG] Predicted Intent: %s, Entity: %s", predictedIntent, entity));

		Optional<String> activeIntentOpt = context.getCurrentIntent();
		boolean isNewStatefulTopic = STATEFUL_INTENTS.contains(predictedIntent) &&
				activeIntentOpt.map(active -> !active.equals(predictedIntent)).orElse(true);

		if (isNewStatefulTopic) {
			if (DEBUG_MODE) Log.d(TAG, "[DEBUG] Pushing new stateful context: " + predictedIntent);
			context.pushContext(predictedIntent);
		}

		if (entity != null) {
			String entityKey; // Declare entityKey here
			switch (predictedIntent) {
				case "calculator_query":
					entityKey = "expression";
					break;
				case "dictionary_query":
					entityKey = "term";
					break;
				case "set_username":
					entityKey = "username";
					break;
				default:
					entityKey = "subject";
					break;
			}
			context.addEntityToCurrentContext(entityKey, entity);
			if (DEBUG_MODE) Log.d(TAG, String.format("[DEBUG] Added entity '%s' to context: '%s'", entityKey, entity));
		}

		String intentToHandle = context.getCurrentIntent().orElse(predictedIntent);
		IntentHandler handler = intentHandlers.getOrDefault(intentToHandle, intentHandlers.get("default"));

		if (DEBUG_MODE) Log.d(TAG, String.format("[DEBUG] Intent to Handle: %s | Handler: %s", intentToHandle, handler.getClass().getSimpleName()));

		return handler.handle(userInput, context);
	}

	private Prediction runPredictionPipeline(String userInput) {
		String cleanedInput = userInput.toLowerCase().trim().replaceAll("[\\p{Punct}]", "");

		String directMatchIntent = directMatches.get(cleanedInput);
		if (directMatchIntent != null) {
			if (DEBUG_MODE) Log.d(TAG, String.format("[DEBUG] Pipeline: Direct match. Intent: %s", directMatchIntent));
			return new Prediction(directMatchIntent, null);
		}

		Optional<String> fuzzyDirectMatchResult = FuzzyMatcher.findBestCandidate(cleanedInput, directMatches.keySet(), DIRECT_MATCH_FUZZY_THRESHOLD);
		if (fuzzyDirectMatchResult.isPresent()) {
			String intent = directMatches.get(fuzzyDirectMatchResult.get());
			if (DEBUG_MODE) Log.d(TAG, String.format("[DEBUG] Pipeline: Fuzzy Direct match. Intent: %s", intent));
			return new Prediction(intent, null);
		}

		Optional<IntentMatch> patternMatch = patternHandler.match(cleanedInput);
		if (patternMatch.isPresent()) {
			IntentMatch match = patternMatch.get();
			String intent = match.getIntent();
			String entity = match.getEntity();

			if (entity == null && "calculator_query".equals(intent)) {
				entity = userInput;
			}

			if (DEBUG_MODE) Log.d(TAG, String.format("[DEBUG] Pipeline: Pattern match. Intent: %s, Entity: %s", intent, entity));
			return new Prediction(intent, entity);
		}

		List<String> tokens = TextProcessor.tokenize(userInput); // Ensure TextProcessor and its methods are defined
		if (!tokens.isEmpty()) {
			com.f9ld3.xavier.ai.V2.PredictionResult result = classifier.predict(tokens); // Ensure PredictionResult and classifier methods are defined
			if (result.getConfidence() >= CONFIDENCE_THRESHOLD) {
				String intent = result.getPredictedLabel();
				if (DEBUG_MODE) Log.d(TAG, String.format("[DEBUG] Pipeline: Classifier success. Intent: %s", intent));
				return new Prediction(intent, null);
			}
		}

		Optional<MatchResult> fuzzyResult = fuzzyMatcher.findBestMatch(userInput, FUZZY_RESCUE_THRESHOLD);
		if (fuzzyResult.isPresent()) {
			String intent = fuzzyResult.get().getIntent();
			if (DEBUG_MODE) Log.d(TAG, String.format("[DEBUG] Pipeline: Fuzzy Rescue. Intent: %s", intent));
			return new Prediction(intent, null);
		}

		return new Prediction("default", null);
	}

	private String getApiKeyFromProperties(String key) {
		if (apiProperties == null) {
			Log.w(TAG, "apiProperties is null in getApiKeyFromProperties for key: " + key);
			return null;
		}
		String value = apiProperties.getProperty(key);
		if (value == null || value.contains("YOUR_") || value.trim().isEmpty()) {
			return null;
		}
		return value;
	}

	private void registerDirectMatches() {
		directMatches.put("hi", "greeting");
		directMatches.put("hello", "greeting");
		directMatches.put("hey", "greeting");
		directMatches.put("yo", "greeting");
		directMatches.put("sup", "greeting");
		directMatches.put("wassup", "greeting");
		directMatches.put("what's up", "greeting");
		directMatches.put("how do you do", "greeting");

		directMatches.put("what is your name", "about_bot");
		directMatches.put("what's your name", "about_bot");
		directMatches.put("who are you", "about_bot");
		directMatches.put("what are you", "about_bot");
		directMatches.put("tell me about yourself", "about_bot");

		directMatches.put("ok", "chitchat");
		directMatches.put("okay", "chitchat");
		directMatches.put("cool", "chitchat");
		directMatches.put("nice", "chitchat");
		directMatches.put("great", "chitchat");
		directMatches.put("good", "chitchat");
		directMatches.put("alright", "chitchat");

		directMatches.put("thanks", "gratitude");
		directMatches.put("thank you", "gratitude");
		directMatches.put("tnx", "gratitude");
		directMatches.put("thx", "gratitude");

		directMatches.put("yes", "confirmation");
		directMatches.put("yep", "confirmation");
		directMatches.put("yeah", "confirmation");
		directMatches.put("yh", "confirmation");
		directMatches.put("no", "confirmation");
		directMatches.put("nope", "confirmation");
		directMatches.put("yes please", "confirmation");

		directMatches.put("bye", "goodbye");
		directMatches.put("exit", "goodbye");
		directMatches.put("quit", "goodbye");
	}

	private void registerPatterns() {
		patternHandler.registerPattern("set_username", "(?i)^(?:my name is|call me|please call me|you can call me)\\s+(.+)$");
		patternHandler.registerPattern("get_username", "(?i)^(?:what is|what's|do you know) my name\\??$");
		patternHandler.registerPattern("correction", "(?i)^(?:no,?|nope,?|actually,?|i mean|i meant|what i meant was|i meant to say|no i meant|no i said)\\s*(.+)$");

		patternHandler.registerPattern("joke_query", "(?i)^(?:tell me|give me|i want to hear) (?:a|another|\\d+)?\\s*joke$");
		patternHandler.registerPattern("riddle_query", "(?i)^(?:tell me a|ask me a|give me a)?\\s*riddle$");
		patternHandler.registerPattern("fact_query", "(?i)^(?:tell me|give me) (?:a|another)?\\s*(?:fun|interesting|random)?\\s*fact$");
		patternHandler.registerPattern("internet_status_query", "(?i)^(?:check|what's) (?:your|the) (?:internet|network) (?:status|connection)|are you online\\??$");

		patternHandler.registerPattern("dictionary_query", "(?i)^(?:what is|what's|what does)?(?: the)? (?:meaning of|definition of) (?:the word )?(.+?)(?: mean)?\\??$");
		patternHandler.registerPattern("dictionary_query", "(?i)^(?:define) (?:the word )?(.+)$");
		patternHandler.registerPattern("time_query", "(?i)^(?:what's|what is|tell me) (?:the )?time$");
		patternHandler.registerPattern("time_query", "(?i)^(?:what's|what is|tell me) (?:the )?time (?:in|for|at) (.+)$");
		patternHandler.registerPattern("time_query", "(?i)^(?:what's|what is) (?:the )?timezone (?:in|for|at) (.+)$");
		patternHandler.registerPattern("weather_query", "(?i)^(?:what's|what is|how's|tell me|check) (?:the )?(?:weather|forecast|temperature)$");
		patternHandler.registerPattern("weather_query", "(?i)^(?:what's|what is|how's|tell me|check) (?:the )?(?:weather|forecast|temperature) (?:in|for|at) (.+)$");
		patternHandler.registerPattern("calculator_query", "(?i)^(?:calculate|compute|what is|what's)\\s+(.*(?:\\d|plus|minus|times|divided|root|power|percent|\\^|\\*|\\/|\\+|-).*)$");
		patternHandler.registerPattern("calculator_query", "^[\\d\\s()+\\-*/.^x]+$");

		String userStatusKeywords = "bored|boring|hungry|hungey|starving|famished|sad|unhappy|down|depressed|tired|sleepy|exhausted|happy|excited|great|fantastic|curious|confused|worried|scared";
		patternHandler.registerPattern("user_status_query", "(?i)^(?:i am|i'm|i feel|feeling|that's|this is)\\s+(" + userStatusKeywords + ")$");
		patternHandler.registerPattern("user_status_query", "(?i)^(entertain me|cheer me up|i need food)$");
		patternHandler.registerPattern("follow_up", "(?i)^(and )?(what|how) about (him|her|it|them|there)\\??$");
		patternHandler.registerPattern("follow_up", "(?i)^(tell me more|go on|what else|more details|can you elaborate|and its population|and its history|tell me another)\\??$");
		patternHandler.registerPattern("how_to_query", "(?i)^(try another|another one|next one|show me another|give me another|more info)$");
		patternHandler.registerPattern("list_query", "(?i)^(?:tell me|give me|show me) (?:a |\\d+ )?(?:common |popular |top )?(.+)$");

		patternHandler.registerPattern("how_to_query", "(?i)^(?:xavier\\s+)?(?:how to|how do i|explain how to|what are the steps to) (.+)$");
		patternHandler.registerPattern("list_query", "(?i)^(?:xavier\\s+)?(?:list of|give me a list of|name some|what are some) (?:the |some |\\d+ )?(?:common |popular |top )?(.+)$");
		patternHandler.registerPattern("knowledge_query", "(?i)^(?:xavier\\s+)?(?:who is|who are|where is|tell me about|explain|can you tell me about|do you know about|lets talk about|can we discuss) (.+)$");
		patternHandler.registerPattern("knowledge_query", "(?i)^(?:what is|what are|what's) (.+)$");
	}

	// You'll need to define these classes or ensure they are correctly imported
	// if they are not inner classes of XavierCoreV2.
	// For example:
	private static class TextProcessor { // Placeholder
		public static List<String> tokenize(String input) {
			if (input == null || input.isBlank()) {
				return Collections.emptyList();
			}
			// Basic tokenization: split by space, could be more sophisticated
			return List.of(input.toLowerCase().split("\\s+"));
		}
	}

	private static class PredictionResult { // Placeholder
		private final String predictedLabel;
		private final double confidence;

		public PredictionResult(String predictedLabel, double confidence) {
			this.predictedLabel = predictedLabel;
			this.confidence = confidence;
		}

		public String getPredictedLabel() {
			return predictedLabel;
		}

		public double getConfidence() {
			return confidence;
		}
	}
	// Assuming NaiveBayesClassifier, DataLoader, WolframAlphaClient etc. are also defined elsewhere
	// and have the methods you're calling on them.
}
