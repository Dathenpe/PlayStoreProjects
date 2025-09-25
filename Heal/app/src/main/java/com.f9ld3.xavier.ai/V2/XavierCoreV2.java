package com.f9ld3.xavier.ai.V2;

import android.content.Context;
import android.util.Log;

import com.f9ld3.xavier.ai.V2.FuzzyMatcher.MatchResult;
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
import com.f9ld3.xavier.ai.V2.utils.ContentSafetyFilter;
import com.f9ld3.xavier.ai.V2.utils.ContentSafetyFilter.ContentSafetyResult; // Import ContentSafetyResult explicitly
import com.f9ld3.xavier.ai.V2.utils.ResponseGenerator;
import com.f9ld3.xavier.ai.V2.utils.SharedHttpClient;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

import okhttp3.OkHttpClient;

public class XavierCoreV2 {

    private static final String TAG = "XavierCoreV2"; // CORE_TAG

    private final NaiveBayesClassifier classifier;
    private final FuzzyMatcher fuzzyMatcher;
    private final Map<String, IntentHandler> intentHandlers;
    private final Map<String, String> directMatches;
    private final PatternHandler patternHandler;
    private final SentimentAnalysisService sentimentService;
    private WolframAlphaClient wolframAlphaClient;
    private Properties apiProperties;
    private final Context appContext;
    private final OkHttpClient sharedOkHttpClient;

    // --- NEW: ContentSafetyFilter instance ---
    private final ContentSafetyFilter contentSafetyFilter;

    private boolean isTrained = false;
    public static final boolean DEBUG_MODE = true; // KEEP TRUE FOR LOGGING
    private static final double CONFIDENCE_THRESHOLD = 0.5;
    private static final double FUZZY_RESCUE_THRESHOLD = 0.75;
    private static final double DIRECT_MATCH_FUZZY_THRESHOLD = 0.80;

    private static final Set<String> STATEFUL_INTENTS;

    static {
        Set<String> tempSet = new HashSet<>();
        tempSet.add("knowledge_query");
        tempSet.add("list_query");
        tempSet.add("how_to_query");
        STATEFUL_INTENTS = Collections.unmodifiableSet(tempSet);
    }

    public static final class Prediction {
        private final String intent;
        private final String entity;

        public Prediction(String intent, String entity) {
            this.intent = intent;
            this.entity = entity;
        }

        public String getIntent() { return intent; }
        public String getEntity() { return entity; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Prediction that = (Prediction) o;
            return Objects.equals(intent, that.intent) && Objects.equals(entity, that.entity);
        }

        @Override
        public int hashCode() { return Objects.hash(intent, entity); }

        @Override
        public String toString() { return "Prediction{intent='" + intent + '\'' + ", entity='" + entity + '\'' + '}'; }
    }

    // --- UPDATED: Constructor now accepts ContentSafetyFilter ---
    public XavierCoreV2(Context context, ContentSafetyFilter contentSafetyFilter) {
        Log.i(TAG, "XavierCoreV2_Constructor: START");
        this.appContext = context.getApplicationContext(); // Use application context to avoid activity leaks
        this.classifier = new NaiveBayesClassifier();
        this.fuzzyMatcher = new FuzzyMatcher();
        this.intentHandlers = new HashMap<>();
        this.directMatches = new HashMap<>();
        this.patternHandler = new PatternHandler();
        this.sentimentService = new SentimentAnalysisService(); // Assuming default constructor is fine

        // --- NEW: Assign passed ContentSafetyFilter instance ---
        this.contentSafetyFilter = contentSafetyFilter;
        Log.d(TAG, "XavierCoreV2_Constructor: ContentSafetyFilter instance assigned.");
        // --- END NEW ---

        Log.d(TAG, "XavierCoreV2_Constructor: Getting SharedHttpClient instance...");
        this.sharedOkHttpClient = SharedHttpClient.get(); // Relies on SharedHttpClient being robust
        Log.d(TAG, "XavierCoreV2_Constructor: SharedHttpClient instance obtained: " + (this.sharedOkHttpClient != null));

        // Optional: Initialize NetworkStatusChecker if it requires global context setup
        // Log.d(TAG, "XavierCoreV2_Constructor: Initializing NetworkStatusChecker (if applicable)...");
        // NetworkStatusChecker.initialize(this.appContext); // If you implement this pattern

        Log.d(TAG, "XavierCoreV2_Constructor: Loading API keys and clients...");
        loadApiKeysAndClients();
        Log.d(TAG, "XavierCoreV2_Constructor: Registering handlers...");
        registerHandlers();
        Log.d(TAG, "XavierCoreV2_Constructor: Registering direct matches...");
        registerDirectMatches();
        Log.d(TAG, "XavierCoreV2_Constructor: Registering patterns...");
        registerPatterns();

        if (DEBUG_MODE) {
            Log.d(TAG, "XavierCoreV2_Constructor: Direct Matches Initialized. Size: " + directMatches.size());
            for (Map.Entry<String, String> entry : directMatches.entrySet()) {
                Log.v(TAG, "XavierCoreV2_Constructor: Direct Match Entry: '" + entry.getKey() + "' -> '" + entry.getValue() + "'");
            }
        }
        Log.i(TAG, "XavierCoreV2_Constructor: END");
    }

    // --- For backward compatibility if needed, but the AIFragment uses the new constructor ---
    // public XavierCoreV2(Context context) {
    //     this(context, new ContentSafetyFilter()); // Creates its own filter instance
    // }

    public boolean isTrained() {
        return isTrained;
    }

    public String getResponse(String userInput, ConversationContext context) {
        Log.i(TAG, "getResponse: START for userInput: '" + userInput + "'");
        // Log.d(TAG, "getResponse: ConversationContext ID: " + (context != null ? context.hashCode() : "null"));

        if (userInput == null || userInput.isBlank()) {
            Log.w(TAG, "getResponse: User input is null or blank. Returning fallback.");
            return ResponseGenerator.getIntelligentFallback(); // Make sure this is never null
        }

        if (!isTrained) {
            Log.e(TAG, "getResponse: AI is NOT TRAINED. isTrained = " + isTrained + ". Returning placeholder error message.");
            return "I'm sorry, I'm still learning the ropes. Please try again in a moment.";
        }

        Log.d(TAG, "getResponse: AI is trained. Proceeding with input: '" + userInput + "'");

        try { // Wrap the core logic in a try-catch to log any unexpected runtime exceptions
            context.setLastUserInput(userInput);
            double sentiment = sentimentService.getSentimentScore(userInput); // Ensure this is safe
            context.setLastSentimentScore(sentiment);
            if (DEBUG_MODE) Log.d(TAG, String.format("getResponse: Sentiment Score for '%s': %.2f", userInput, sentiment));

            Log.d(TAG, "getResponse: Running prediction pipeline for: '" + userInput + "'");
            Prediction prediction = runPredictionPipeline(userInput); // Ensure this is safe
            if (prediction == null) {
                Log.e(TAG, "getResponse: CRITICAL - runPredictionPipeline returned NULL for input: '" + userInput + "'. Defaulting intent.");
                prediction = new Prediction("default", null); // Failsafe
            }

            String predictedIntent = prediction.getIntent();
            String entity = prediction.getEntity();

            if (DEBUG_MODE) Log.d(TAG, String.format("getResponse: Predicted Intent: '%s', Entity: '%s'", predictedIntent, entity));

            Optional<String> activeIntentOpt = context.getCurrentIntent();
            boolean isNewStatefulTopic = STATEFUL_INTENTS.contains(predictedIntent) &&
                    activeIntentOpt.map(active -> !active.equals(predictedIntent)).orElse(true);

            if (isNewStatefulTopic) {
                if (DEBUG_MODE) Log.d(TAG, "getResponse: Pushing new stateful context: " + predictedIntent);
                context.pushContext(predictedIntent);
            }

            if (entity != null) {
                String entityKey;
                switch (predictedIntent) { // Assuming predictedIntent is never null here
                    case "calculator_query": entityKey = "expression"; break;
                    case "dictionary_query": entityKey = "term"; break;
                    case "set_username": entityKey = "username"; break;
                    default: entityKey = "subject"; break;
                }
                context.addEntityToCurrentContext(entityKey, entity);
                if (DEBUG_MODE) Log.d(TAG, String.format("getResponse: Added entity '%s':'%s' to context", entityKey, entity));
            }

            String intentToHandle = context.getCurrentIntent().orElse(predictedIntent);
            Log.d(TAG, "getResponse: Intent to handle (after context): '" + intentToHandle + "'");

            IntentHandler handler = intentHandlers.get(intentToHandle); // Get by potentially stateful intent
            if (handler == null) { // Fallback if stateful intent has no specific handler (should not happen with good registration)
                Log.w(TAG, "getResponse: No handler found for stateful intent '" + intentToHandle + "', falling back to predicted intent handler: '" + predictedIntent + "'");
                handler = intentHandlers.get(predictedIntent);
            }
            if (handler == null) { // Ultimate fallback to "default"
                Log.e(TAG, "getResponse: CRITICAL - Handler is NULL for intentToHandle: '" + intentToHandle + "' AND predictedIntent: '" + predictedIntent + "'. Using 'default' handler.");
                handler = intentHandlers.get("default");
            }
            if (handler == null) {
                Log.e(TAG, "getResponse: CRITICAL - 'default' handler is ALSO NULL. This should not happen if registered. Returning fallback string.");
                return ResponseGenerator.getIntelligentFallback();
            }

            if (DEBUG_MODE) Log.d(TAG, String.format("getResponse: Final Intent to Handle: '%s' | Using Handler: %s", intentToHandle, handler.getClass().getSimpleName()));
            Log.d(TAG, "getResponse: Calling handler.handle() for intent: '" + intentToHandle + "' with input: '" + userInput + "'");
            String response = handler.handle(userInput, context); // Ensure handler.handle() is robust

            // --- UPDATED: Apply content safety filter to AI's generated response ---
            String finalResponse = response == null ? ResponseGenerator.getIntelligentFallback() : response; // Ensure response is not null

            if (finalResponse != null) {
                finalResponse = finalResponse.replaceAll("\\*\\*", "").replaceAll("\\*", "");
                if (DEBUG_MODE) Log.d(TAG, "getResponse: Applied asterisk filter. Filtered response: '" + finalResponse + "'");
            }

            ContentSafetyResult aiOutputSafetyResult = contentSafetyFilter.analyzeContent(finalResponse);
            if (!aiOutputSafetyResult.isSafe()) {
                Log.w(TAG, "getResponse: AI generated unsafe content! Original response: '" + finalResponse + "'. Details: " + aiOutputSafetyResult);
                return "I'm sorry, I cannot provide this response as it violates content safety guidelines. Please ask another question.";
            }
            // --- END UPDATED FILTER ---

            Log.i(TAG, "getResponse: END. Response from handler for '" + userInput + "': '" + finalResponse + "'");
            return finalResponse;

        } catch (Exception e) {
            Log.e(TAG, "getResponse: CRITICAL UNCAUGHT EXCEPTION in getResponse for input '" + userInput + "'", e);
            return "I'm sorry, I ran into an unexpected problem processing that.";
        }
    }

    private Prediction runPredictionPipeline(String userInput) {
        Log.i(TAG, "[PIPELINE_START] Processing Raw User Input: '" + userInput + "'");

        String cleanedInput = userInput.toLowerCase().trim().replaceAll("[\\p{Punct}]", "");
        if (DEBUG_MODE) Log.d(TAG, "[PIPELINE_INFO] Cleaned User Input: '" + cleanedInput + "'");

        Log.d(TAG, "[PIPELINE_STEP] Checking Direct Match for: '" + cleanedInput + "'");
        String directMatchIntent = directMatches.get(cleanedInput);
        if (directMatchIntent != null) {
            if (DEBUG_MODE) Log.d(TAG, "[PIPELINE_RESULT] Direct exact match. Input: '" + cleanedInput + "', Intent: " + directMatchIntent);
            Log.i(TAG, "[PIPELINE_END] Path: Direct Match.");
            return new Prediction(directMatchIntent, null);
        }

        Log.d(TAG, "[PIPELINE_STEP] Checking Fuzzy Direct Match for: '" + cleanedInput + "'");
        Optional<String> fuzzyDirectMatchResult = FuzzyMatcher.findBestCandidate(cleanedInput, directMatches.keySet(), DIRECT_MATCH_FUZZY_THRESHOLD);
        if (fuzzyDirectMatchResult.isPresent()) {
            String matchedKey = fuzzyDirectMatchResult.get();
            String intent = directMatches.get(matchedKey);
            if (DEBUG_MODE) Log.d(TAG, "[PIPELINE_RESULT] Fuzzy Direct match. Input: '" + cleanedInput + "', MatchedKey: '" + matchedKey + "', Intent: " + intent);
            Log.i(TAG, "[PIPELINE_END] Path: Fuzzy Direct Match.");
            return new Prediction(intent, null);
        }

        Log.d(TAG, "[PIPELINE_STEP] Checking Pattern Match for: '" + cleanedInput + "'");
        Optional<PatternHandler.IntentMatch> patternMatch = patternHandler.match(cleanedInput);
        if (patternMatch.isPresent()) {
            PatternHandler.IntentMatch match = patternMatch.get();
            String intent = match.getIntent();
            String entity = match.getEntity();
            if (entity == null && "calculator_query".equals(intent)) {
                entity = userInput; // Use raw input for calculator if pattern didn't extract
            }
            if (DEBUG_MODE) Log.d(TAG, "[PIPELINE_RESULT] Pattern match. Intent: " + intent + ", Entity: " + entity);
            Log.i(TAG, "[PIPELINE_END] Path: Pattern Match.");
            return new Prediction(intent, entity);
        }

        Log.d(TAG, "[PIPELINE_STEP] Tokenizing and Checking Classifier for: '" + userInput + "' (using original for TextProcessor)");
        List<String> tokens = TextProcessor.tokenize(userInput);
        if (tokens != null && !tokens.isEmpty()) {
            Log.d(TAG, "[PIPELINE_INFO] Tokens for classifier: " + tokens.toString());
            com.f9ld3.xavier.ai.V2.PredictionResult result = classifier.predict(tokens); // Ensure NaiveBayesClassifier & PredictionResult are robust
            if (result == null) {
                Log.e(TAG, "[PIPELINE_ERROR] Classifier.predict returned NULL!");
            } else {
                if (DEBUG_MODE) Log.d(TAG, "[PIPELINE_INFO] Classifier raw result - Predicted: " + result.getPredictedLabel() + ", Confidence: " + result.getConfidence());
                if (result.getConfidence() >= CONFIDENCE_THRESHOLD) {
                    String intent = result.getPredictedLabel();
                    if (DEBUG_MODE) Log.d(TAG, "[PIPELINE_RESULT] Classifier success. Intent: " + intent);
                    Log.i(TAG, "[PIPELINE_END] Path: Classifier Match.");
                    return new Prediction(intent, null);
                } else {
                    if (DEBUG_MODE) Log.d(TAG, "[PIPELINE_INFO] Classifier low confidence for '" + cleanedInput + "'.");
                }
            }
        } else {
            if (DEBUG_MODE) Log.d(TAG, "[PIPELINE_INFO] No tokens generated for classifier from input: '" + userInput + "' or tokens list was null.");
        }

        Log.d(TAG, "[PIPELINE_STEP] Checking Fuzzy Rescue for: '" + userInput + "'");
        Optional<MatchResult> fuzzyResult = fuzzyMatcher.findBestMatch(userInput, FUZZY_RESCUE_THRESHOLD);
        if (fuzzyResult.isPresent()) {
            String intent = fuzzyResult.get().getIntent(); // Assumes MatchResult and getIntent() are safe
            if (DEBUG_MODE) Log.d(TAG, "[PIPELINE_RESULT] Fuzzy Rescue. Intent: " + intent);
            Log.i(TAG, "[PIPELINE_END] Path: Fuzzy Rescue Match.");
            return new Prediction(intent, null);
        }

        if (DEBUG_MODE) Log.d(TAG, "[PIPELINE_RESULT] Falling through to DEFAULT for input: '" + userInput + "'");
        Log.i(TAG, "[PIPELINE_END] Path: Default.");
        return new Prediction("default", null); // Ensure "default" is a registered intent
    }

    private void registerPatterns() {
        Log.i(TAG, "registerPatterns: START");

        // --- HIGH PRIORITY / CONTEXTUAL PATTERNS ---
        patternHandler.registerPattern("set_username", "(?i)^(?:my name is|call me|please call me|you can call me)\\s+(.+)$");
        patternHandler.registerPattern("get_username", "(?i)^(?:what is|what's|do you know) my name\\??$");
        patternHandler.registerPattern("correction", "(?i)^(?:no,?|nope,?|actually,?|i mean|i meant|what i meant was|i meant to say|no i meant|no i said)\\s*(.+)$");
        patternHandler.registerPattern("follow_up", "(?i)^(and )?(what|how) about (him|her|it|them|there)\\??$");
        patternHandler.registerPattern("follow_up", "(?i)^(tell me more|go on|what else|more details|can you elaborate|and its population|and its history|tell me another)\\??$");
        patternHandler.registerPattern("how_to_query", "(?i)^(try another|another one|next one|show me another|give me another|more info)$"); // Follow-up for lists/facts

        // --- SPECIFIC UTILITY QUERIES (Should be high priority to avoid overlap) ---
        patternHandler.registerPattern("joke_query", "(?i)^(?:tell me|give me|i want to hear) (?:a|another|\\d+)?\\s*joke$");
        patternHandler.registerPattern("riddle_query", "(?i)^(?:tell me a|ask me a|give me a)?\\s*riddle$");
        patternHandler.registerPattern("fact_query", "(?i)^(?:tell me|give me) (?:a|another)?\\s*(?:fun|interesting|random)?\\s*fact$");
        patternHandler.registerPattern("internet_status_query", "(?i)^(?:check|what's) (?:your|the) (?:internet|network) (?:status|connection)|are you online\\??$");
        String userStatusKeywords = "bored|boring|hungry|hungey|starving|famished|sad|unhappy|down|depressed|tired|sleepy|exhausted|happy|excited|great|fantastic|curious|confused|worried|scared";
        patternHandler.registerPattern("user_status_query", "(?i)^(?:i am|i'm|i feel|feeling|that's|this is)\\s+(" + userStatusKeywords + ")$");
        patternHandler.registerPattern("user_status_query", "(?i)^(entertain me|cheer me up|i need food)$");

        // --- CALCULATION (Should be before knowledge_query as it uses 'what is/what's') ---
        // Note: I am not changing the 'calculator_query' patterns, but their placement is key.
        patternHandler.registerPattern("calculator_query", "(?i)^(?:calculate|compute|what is|what's)\\s+(.*(?:\\d|plus|minus|times|divided|root|power|percent|\\^|\\*|\\/|\\+|-).*)$");
        patternHandler.registerPattern("calculator_query", "^[\\d\\s()+\\-*/.^x]+$");

        // --- DICTIONARY (Should be before knowledge_query as it uses 'what is/what's') ---
        patternHandler.registerPattern("dictionary_query", "(?i)^(?:what is|what's|what does)?(?: the)? (?:meaning of|definition of) (?:the word )?(.+?)(?: mean)?\\??$");
        patternHandler.registerPattern("dictionary_query", "(?i)^(?:define) (?:the word )?(.+)$");

        // --- TIME / WEATHER (Should be before knowledge_query as it uses 'what is/what's') ---
        patternHandler.registerPattern("time_query", "(?i)^(?:what's|what is|tell me) (?:the )?time$");
        patternHandler.registerPattern("time_query", "(?i)^(?:what's|what is|tell me) (?:the )?time (?:in|for|at) (.+)$");
        patternHandler.registerPattern("time_query", "(?i)^(?:what's|what is) (?:the )?timezone (?:in|for|at) (.+)$");
        patternHandler.registerPattern("weather_query", "(?i)^(?:what's|what is|how's|tell me|check) (?:the )?(?:weather|forecast|temperature)$");
        patternHandler.registerPattern("weather_query", "(?i)^(?:what's|what is|how's|tell me|check) (?:the )?(?:weather|forecast|temperature) (?:in|for|at) (.+)$");

        // --- KNOWLEDGE / LIST / HOW-TO QUERIES (General, placed last) ---
        // New: Place specific list/how-to before the very general knowledge query
        patternHandler.registerPattern("how_to_query", "(?i)^(?:xavier\\s+)?(?:how to|how do i|explain how to|what are the steps to) (.+)$");
        patternHandler.registerPattern("list_query", "(?i)^(?:xavier\\s+)?(?:list of|give me a list of|name some|what are some) (?:the |some |\\d+ )?(?:common |popular |top )?(.+)$");

        // This pattern handles "tell me about X" and "who is X"
        patternHandler.registerPattern("knowledge_query", "(?i)^(?:xavier\\s+)?(?:who is|who are|where is|tell me about|explain|can you tell me about|do you know about|lets talk about|can we discuss) (.+)$");

        // New: Reintroduce a filtered 'what is' for knowledge_query that only matches if no other specific intent matched.
        // The original: patternHandler.registerPattern("knowledge_query", "(?i)^(?:what is|what are|what's) (.+)$"); IS REMOVED.
        // The more specific queries above now handle the 'what is' cases for time, weather, calculator, and dictionary.

        // Final fallback pattern for general 'what is'/'what are' that was removed. It is generally safer to let the
        // Naive Bayes Classifier and Fuzzy Matcher handle the remaining generic 'what is' knowledge questions.
        // If you absolutely must have a pattern, it should be the final one and is what will be caught by the patternHandler.
        // Re-adding the broad 'what is' pattern, but making it the ABSOLUTE LAST pattern registered to ensure all other specific patterns run first.
        patternHandler.registerPattern("knowledge_query", "(?i)^(?:what is|what are|what's) (.+)$");

        Log.i(TAG, "registerPatterns: END. Pattern registration complete.");
    }

    private void loadApiKeysAndClients() {
        Log.i(TAG, "loadApiKeysAndClients: START");
        this.apiProperties = new Properties();
        try (InputStream input = appContext.getAssets().open("api.properties")) {
            apiProperties.load(input);
            Log.d(TAG, "loadApiKeysAndClients: api.properties loaded successfully.");

            List<String> wolframKeysList = new ArrayList<>();
            String primaryId = getApiKeyFromProperties("wolframalpha.appid");
            String backupId = getApiKeyFromProperties("wolframalpha.appid.backup");
            String tertiaryId = getApiKeyFromProperties("wolframalpha.appid.tertiary");
            if (primaryId != null) wolframKeysList.add(primaryId);
            if (backupId != null) wolframKeysList.add(backupId);
            if (tertiaryId != null) wolframKeysList.add(tertiaryId);

            Log.d(TAG, "loadApiKeysAndClients: Initializing WolframAlphaClient with " + wolframKeysList.size() + " keys.");
            this.wolframAlphaClient = new WolframAlphaClient(
                    appContext,
                    sharedOkHttpClient, // Ensure sharedOkHttpClient is initialized before this line
                    wolframKeysList.toArray(new String[0])
            );
            Log.i(TAG, "loadApiKeysAndClients: WolframAlphaClient initialized.");
        } catch (IOException ex) {
            Log.e(TAG, "loadApiKeysAndClients: FATAL Error loading api.properties or initializing WolframAlphaClient.", ex);
            // Fallback: Pass appContext and the shared OkHttpClient
            this.wolframAlphaClient = new WolframAlphaClient(appContext, sharedOkHttpClient);
        }
        Log.i(TAG, "loadApiKeysAndClients: END");
    }

    private void registerHandlers() {
        Log.i(TAG, "registerHandlers: START");

        Log.d(TAG, "registerHandlers: Registering JokeService...");
        JokeService jokeService = new JokeService(appContext);
        intentHandlers.put("joke_query", new JokeHandler(jokeService));

        Log.d(TAG, "registerHandlers: Registering RiddleService...");
        RiddleService riddleService = new RiddleService(appContext);
        RiddleHandler riddleHandlerInternal = new RiddleHandler(riddleService); // Use different name to avoid conflict if any
        intentHandlers.put("riddle_query", riddleHandlerInternal);
        intentHandlers.put("riddle_confirmation", riddleHandlerInternal);

        Log.d(TAG, "registerHandlers: Registering FunFactService...");
        FunFactService funFactService = new FunFactService(sharedOkHttpClient);
        intentHandlers.put("fact_query", new FunFactHandler(funFactService));

        Log.d(TAG, "registerHandlers: Registering simple handlers...");
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

        Log.d(TAG, "registerHandlers: Registering InternetStatusHandler...");
        intentHandlers.put("internet_status_query", new InternetStatusHandler(appContext)); // This now has Context constructor

        Log.d(TAG, "registerHandlers: Registering core logic handlers...");
        intentHandlers.put("follow_up", new FollowUpHandler(this));
        intentHandlers.put("correction", new CorrectionHandler(this));
        intentHandlers.put("user_status_query", new UserStatusHandler(this));

        Log.d(TAG, "registerHandlers: Registering SearchService and default handler...");
        List<String> serperApiKeys = new ArrayList<>();
        String primarySerperApiKey = getApiKeyFromProperties("serper.apikey");
        String backupSerperApiKey = getApiKeyFromProperties("serper.apikey.backup");
        if (primarySerperApiKey != null) serperApiKeys.add(primarySerperApiKey);
        if (backupSerperApiKey != null) serperApiKeys.add(backupSerperApiKey);
        SearchService searchService = new SearchService(serperApiKeys, sharedOkHttpClient);
        intentHandlers.put("default", new SmartDefaultHandler(this.wolframAlphaClient, searchService));

        Log.d(TAG, "registerHandlers: Registering location-based services (Weather/Time)...");
        List<String> weatherApiKeys = new ArrayList<>();
        String primaryWeatherKey = getApiKeyFromProperties("openweathermap.apikey");
        String backupWeatherKey = getApiKeyFromProperties("openweathermap.apikey.backup");
        if (primaryWeatherKey != null) weatherApiKeys.add(primaryWeatherKey);
        if (backupWeatherKey != null) weatherApiKeys.add(backupWeatherKey);

        try {
            GeocodingService geocodingService = new GeocodingService(weatherApiKeys, sharedOkHttpClient);
            IPGeolocationService ipGeolocationService = new IPGeolocationService(sharedOkHttpClient);
            LocationResolverService locationResolver = new LocationResolverService(geocodingService);
            intentHandlers.put("time_query", new TimeQueryHandler(locationResolver, ipGeolocationService, primaryWeatherKey));
            intentHandlers.put("weather_query", new WeatherQueryHandler(locationResolver, ipGeolocationService, primaryWeatherKey));
            Log.d(TAG, "registerHandlers: Location services (Weather/Time) registered successfully.");
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "registerHandlers: Location services could not be initialized. Weather/Time features will be disabled.", e);
            IntentHandler unavailableHandler = (userInput, ctx) -> "I'm sorry, my location-based services are currently unavailable.";
            intentHandlers.put("weather_query", unavailableHandler);
            intentHandlers.put("time_query", unavailableHandler);
        }

        Log.d(TAG, "registerHandlers: Registering DictionaryService...");
        DictionaryService dictionaryService = new DictionaryService(sharedOkHttpClient);
        intentHandlers.put("dictionary_query", new DictionaryHandler(dictionaryService));

        Log.d(TAG, "registerHandlers: Registering HowToQueryHandler...");
        intentHandlers.put("how_to_query", new HowToQueryHandler(searchService));

        Log.d(TAG, "registerHandlers: Registering GenerativeService for List/Knowledge queries...");
        String generativeApiUrl = getApiKeyFromProperties("generative.api.url");
        String generativeModel = getApiKeyFromProperties("generative.api.model");
        List<String> generativeApiKeysList = new ArrayList<>();
        String primaryGenerativeKey = getApiKeyFromProperties("generative.api.key");
        String backupGenerativeKey = getApiKeyFromProperties("generative.api.key.backup");
        if (primaryGenerativeKey != null) generativeApiKeysList.add(primaryGenerativeKey);
        if (backupGenerativeKey != null) generativeApiKeysList.add(backupGenerativeKey);
        GenerativeService generativeService = new GenerativeService(generativeApiUrl, generativeApiKeysList, generativeModel, sharedOkHttpClient);
        intentHandlers.put("list_query", new ListQueryHandler(generativeService, searchService));
        intentHandlers.put("knowledge_query", new KnowledgeQueryHandler(this.wolframAlphaClient, searchService));

        Log.i(TAG, "registerHandlers: END. Handler registration complete.");
    }

    private String getApiKeyFromProperties(String key) {
        // Log.v(TAG, "getApiKeyFromProperties: Requesting key: '" + key + "'"); // Very verbose, enable if needed
        if (apiProperties == null) {
            Log.w(TAG, "getApiKeyFromProperties: apiProperties is null when trying to get key: '" + key + "'");
            return null;
        }
        String value = apiProperties.getProperty(key);
        if (value == null || value.contains("YOUR_") || value.trim().isEmpty()) {
            if (DEBUG_MODE) Log.d(TAG, "getApiKeyFromProperties: API Key for '" + key + "' is missing, a placeholder, or empty.");
            return null;
        }
        // Log.v(TAG, "getApiKeyFromProperties: Found key '" + key + "'"); // Very verbose
        return value;
    }

    private void registerDirectMatches() {
        Log.i(TAG, "registerDirectMatches: START");
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
        Log.i(TAG, "registerDirectMatches: END. Direct match registration complete. Size: " + directMatches.size());
    }

    public void train(String assetFileName) {
        Log.i(TAG, "train: START - Training from asset: '" + assetFileName + "'");
        isTrained = false; // Set to false before attempting to train
        DataLoader dataLoader = new DataLoader(); // Assuming DataLoader constructor is safe
        try {
            Log.d(TAG, "train: Loading data from asset '" + assetFileName + "' using appContext...");
            dataLoader.loadDataFromAsset(appContext, assetFileName); // Ensure appContext is not null
            Log.i(TAG, "train: Data loaded. Documents: " + (dataLoader.getDocuments() != null ? dataLoader.getDocuments().size() : "null") +
                    ", Labels: " + (dataLoader.getLabels() != null ? dataLoader.getLabels().size() : "null"));

            List<List<String>> documents = dataLoader.getDocuments();
            List<String> labels = dataLoader.getLabels();

            if (documents == null || labels == null || documents.isEmpty() || labels.isEmpty()) {
                Log.e(TAG, "train: Training data is NULL or EMPTY after loading from asset: '" + assetFileName + "'. Aborting training.");
                return; // isTrained remains false
            }

            if (documents.size() != labels.size()) {
                Log.e(TAG, "train: Mismatch between number of documents ("+documents.size()+") and labels ("+labels.size()+"). Aborting.");
                return; // isTrained remains false
            }

            Log.d(TAG, "train: Fitting NaiveBayesClassifier...");
            classifier.fit(documents, labels); // Ensure NaiveBayesClassifier is robust
            Log.i(TAG, "train: NaiveBayesClassifier training complete!");

            List<String> rawPhrases = dataLoader.getRawPhrases();
            if (rawPhrases == null || rawPhrases.isEmpty()) {
                Log.w(TAG, "train: No raw phrases loaded (or list is null) for FuzzyMatcher training.");
            } else {
                Log.d(TAG, "train: Training FuzzyMatcher with " + rawPhrases.size() + " phrases...");
                fuzzyMatcher.train(rawPhrases, labels); // Ensure FuzzyMatcher is robust
                Log.i(TAG, "train: Fuzzy Matcher training complete!");
            }

            isTrained = true; // Set to true ONLY if all steps complete successfully
            Log.i(TAG, "train: END - XavierCoreV2 training successfully COMPLETE!");

        } catch (IOException e) {
            Log.e(TAG, "train: FATAL IOException during training from asset: '" + assetFileName + "'.", e);
            // isTrained remains false
        } catch (Exception e) {
            Log.e(TAG, "train: FATAL Unexpected Exception during training from asset: '" + assetFileName + "'.", e);
            // isTrained remains false
        }

        // Log final training state
        Log.i(TAG, "train: Final isTrained state after attempt: " + isTrained);
    }
}