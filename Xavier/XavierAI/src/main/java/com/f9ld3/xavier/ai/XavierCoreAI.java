package com.f9ld3.xavier.ai; // IMPORTANT: Ensure this matches your package name

import org.json.JSONArray;
import weka.classifiers.Classifier;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.StringToWordVector;
import weka.core.SerializationHelper;

import org.json.JSONObject;
import org.json.JSONException;


import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime; // NEW: For time_query intent
import java.time.format.DateTimeFormatter; // NEW: For formatting time
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * XavierCoreAI: Encapsulates the core AI logic for intent classification,
 * simple entity extraction, and dialogue management using Weka.
 * This class handles loading the trained model and filter, making predictions,
 * and generating contextual responses.
 */
public class XavierCoreAI {

//API  CONFIGURATION
private static final String OPENWEATHER_API_KEY = "a05a0c427992d0bee9a9624548399407";
private static final String OPENWEATHER_BASE_URL = "http://api.openweathermap.org/data/2.5/weather";

private static final String NOMINATIM_BASE_URL = "https://nominatim.openstreetmap.org/search";
private static final String USER_AGENT  = "XavierAI/1.0 (adetayoadedamola6@gmail.com)";
private static long  lastNominatimRequestTime = 0;
private static final long NOMINATIM_RATE_LIMIT_MS = 1000;

// --- AI Brain Components ---
private Classifier classifier; // The trained Weka classification model
private StringToWordVector filter; // The trained Weka text preprocessing filter

// --- Dialogue State Variables ---
private String userName = null;
private String lastIntent = "start"; // Tracks the intent of the previous user message
private String extractedLocation = null; // Stores location if extracted from current/previous turn

// --- AI Configuration Data (These values must match your training data ARFF @ATTRIBUTE intent) ---
private static final ArrayList<String> INTENT_POSSIBLE_VALUES = new ArrayList<>(Arrays.asList(
		"greeting", "goodbye", "unknown", "weather_query", "joke_request", "followup_location",
		"gratitude", "affirmation", "negation", "personal_question", "time_query", "feeling_status" // All defined intents
));

// A simple, predefined list of locations Xavier knows about for extraction, for faster accessibility
private static final HashSet<String> KNOWN_LOCATIONS = new HashSet<>(Arrays.asList(
		"china", "india", "united states", "indonesia", "pakistan", "nigeria", "brazil", "bangladesh",
		"russia", "mexico", "japan", "ethiopia", "philippines", "egypt", "vietnam", "dr congo",
		"iran", "turkey", "germany", "united kingdom", "france", "italy", "tanzania", "south africa",
		"myanmar", "kenya", "south korea", "colombia", "spain", "uganda", "argentina", "algeria",
		"sudan", "ukraine", "iraq", "afghanistan", "poland", "canada", "morocco", "uzbekistan",
		"peru", "saudi arabia", "venezuela", "ghana", "nepal", "yemen", "australia", "madagascar",
		"ivory coast", "north korea", "california", "texas", "florida", "new york", "pennsylvania", "illinois", "ohio", "georgia",
		"north carolina", "michigan", "new jersey", "virginia", "washington", "arizona", "massachusetts",
		"tennessee", "indiana", "maryland", "missouri", "colorado", "wisconsin", "minnesota", "south carolina",
		"alabama", "louisiana", "kentucky", "oregon", "oklahoma", "connecticut", "utah", "iowa",
		"nevada", "arkansas", "mississippi", "kansas", "new mexico", "nebraska", "idaho", "west virginia",
		"hawaii", "new hampshire", "maine", "montana", "rhode island", "delaware", "south dakota",
		"north dakota", "alaska", "district of columbia", "vermont", "wyoming",
		"netherlands", "belgium", "sweden", "greece", "portugal", "czech republic", "hungary", "austria",
		"switzerland", "denmark", "finland", "norway", "ireland", "new zealand", "chile", "ecuador",
		"bolivia", "guatemala", "cuba", "dominican republic", "haiti", "cambodia", "sri lanka",
		"kazakhstan", "azerbaijan", "georgia", "armenia", "tunisia", "libya", "jordan", "lebanon",
		"syria", "oman", "qatar", "kuwait", "bahrain", "eritrea", "somalia", "liberia", "sierra leone",
		"senegal", "mali", "burkina faso", "niger", "chad", "cameroon", "angola", "mozambique",
		"zambia", "zimbabwe", "botswana", "namibia", "democratic republic of congo", "republic of congo",
		"gabon", "equatorial guinea", "eritrea", "djibouti", "benin", "togo", "guinea", "guinea-bissau",
		"gambia", "cape verde", "comoros", "mauritius", "seychelles", "maldives", "brunei", "east timor",
		"papua new guinea", "fiji", "solomon islands", "vanuatu", "samoa", "tonga", "kiribati", "micronesia",
		"marshall islands", "palau", "nauru", "tuvalu", "san marino", "monaco", "vatican city", "liechtenstein",
		"andorra", "luxembourg", "malta", "cyprus", "iceland", "estonia", "latvia", "lithuania",
		"slovakia", "slovenia", "croatia", "bosnia and herzegovina", "serbia", "kosovo", "montenegro",
		"north macedonia", "albania", "bulgaria", "romania", "moldova", "belarus",
		"los angeles", "chicago", "houston", "phoenix", "philadelphia", "san antonio", "san diego",
		"dallas", "san jose", "austin", "jacksonville", "fort worth", "columbus", "indianapolis",
		"charlotte", "san francisco", "seattle", "denver", "washington dc", "boston", "el paso",
		"detroit", "nashville", "portland", "memphis", "louisville", "milwaukee", "baltimore",
		"albuquerque", "tucson", "fresno", "sacramento", "mesa", "kansas city", "atlanta",
		"virginia beach", "raleigh", "omaha", "miami", "oakland", "minneapolis", "tulsa",
		"arlington", "new orleans", "wichita", "cleveland", "tampa", "cincinnati", "pittsburgh",
		"alberta", "british columbia", "manitoba", "new brunswick", "newfoundland and labrador",
		"nova scotia", "prince edward island", "saskatchewan", "yukon", "northwest territories", "nunavut",
		"queensland", "western australia", "south australia", "tasmania", "act", "northern territory",
		"bengaluru", "mumbai", "delhi", "tokyo", "osaka", "sao paulo", "rio de janeiro", "buenos aires",
		"london", "manchester", "birmingham", "glasgow", "edinburgh", "dublin", "barcelona", "madrid",
		"rome", "milan", "munich", "berlin", "hamburg", "amsterdam", "brussels", "stockholm",
		"copenhagen", "oslo", "helsinki", "zurich", "geneva", "vienna", "prague", "warsaw", "kiev",
		"moscow", "saint petersburg", "beijing", "shanghai", "hong kong", "singapore", "seoul", "bangkok",
		"ho chi minh city", "jakarta", "manila", "kuala lumpur", "dubai", "abu dhabi", "doha", "riyadh",
		"johannesburg", "cape town", "nairobi", "casablanca", "lagos", "abuja", "accra", "cairo",
		"sydney", "melbourne", "auckland", "wellington", "montevideo", "santiago", "lima", "caracas",
		"bogota", "quito", "panama city", "san jose costa rica", "guadalajara", "monterrey", "istanbul"
));
private List<String> greetingResponses;
private List<String> goodbyeResponses;
private List<String> jokeResponses;
private List<String> gratitudeResponses;
private List<String> affirmationResponses;
private List<String> negationResponses;
private List<String> feelingResponses;
private List<String> unknownResponses;

private Random randomGenerator = new Random();
/**
 * Constructor for XavierCoreAI.
 * Loads the trained Weka classifier and StringToWordVector filter from specified file paths.
 * This is typically called once when your application starts.
 *
 * @param modelPath Path to the serialized Weka classifier model file.
 * @param filterPath Path to the serialized Weka StringToWordVector filter file.
 * @throws Exception If the model or filter files cannot be found or loaded.
 */
public XavierCoreAI(String modelPath, String filterPath) throws Exception {
	System.out.println("XavierCoreAI: Attempting to load AI brain from files...");
	this.classifier = (Classifier) SerializationHelper.read(modelPath);
	this.filter = (StringToWordVector) SerializationHelper.read(filterPath);
	System.out.println("XavierCoreAI: AI brain (model and filter) loaded successfully.");
	System.out.println("XavierCoreAI: Loaded model type: " + this.classifier.getClass().getSimpleName());
	// Load dialogue responses from files
	System.out.println("XavierCoreAI: Loading dialogue responses...");
	this.greetingResponses = loadResponsesFromFile("responses/greetings.txt");
	this.goodbyeResponses = loadResponsesFromFile("responses/goodbyes.txt");
	this.jokeResponses = loadResponsesFromFile("responses/jokes.txt"); // Added joke responses
	this.gratitudeResponses = loadResponsesFromFile("responses/gratitude.txt");
	this.affirmationResponses = loadResponsesFromFile("responses/affirmations.txt");
	this.negationResponses = loadResponsesFromFile("responses/negations.txt");
	this.feelingResponses = loadResponsesFromFile("responses/feelings.txt"); // Added feeling responses
	this.unknownResponses = loadResponsesFromFile("responses/unknowns.txt");
	System.out.println("XavierCoreAI: Dialogue responses loaded.");
}

/**
 * Processes a user's message, predicts its intent, and generates a contextual response.
 * This is the main public method to interact with Xavier AI.
 *
 * @param userMessage The raw text message from the user.
 * @return Xavier's friendly response.
 * @throws Exception If an error occurs during prediction or response generation.
 */
public String processMessage(String userMessage) throws Exception {
	// Reset extracted location at the start of each new message processing.
	// This ensures extractedLocation is only carried over if explicitly managed by dialogue flow
	// (e.g., in a follow-up scenario).
	this.extractedLocation = null;
	
	// Predict the intent of the user's message using the loaded classifier and filter.
	String predictedIntent = predictIntent(userMessage);
	
	// Generate a response, passing the predicted intent AND the original user message
	// for potential entity extraction within the response logic.
	String aiResponse = generateResponse(predictedIntent, userMessage);
	
	// Update the last intent for the next turn, to maintain simple context for dialogue management.
	this.lastIntent = predictedIntent;
	
	return aiResponse;
}

/**
 * Predicts the intent of a user's message using the loaded classifier and filter.
 * This method is robustly designed for single instance prediction with StringToWordVector.
 *
 * @param userMessage The raw text message from the user.
 * @return The predicted intent as a String (e.g., "greeting", "weather_query").
 * @throws Exception If an error occurs during prediction.
 */
private String predictIntent(String userMessage) throws Exception {
	// 1. Create a "header" (Instances object without actual data) that matches the *input*
	//    format expected by the StringToWordVector filter. This is crucial for consistency.
	ArrayList<Attribute> rawInputAttributes = new ArrayList<>();
	rawInputAttributes.add(new Attribute("text", (ArrayList<String>) null)); // String attribute for user's text
	rawInputAttributes.add(new Attribute("intent", INTENT_POSSIBLE_VALUES));   // Nominal attribute for intent (definition needed)
	
	Instances rawInputDataHeader = new Instances("RawInputDataForFiltering", rawInputAttributes, 0);
	rawInputDataHeader.setClassIndex(rawInputDataHeader.attribute("intent").index());
	
	// 2. Create a single Weka Instance for the user's message using this header.
	Instance rawInstance = new DenseInstance(rawInputDataHeader.numAttributes());
	rawInstance.setDataset(rawInputDataHeader); // Associate instance with the dataset structure
	rawInstance.setValue(rawInputDataHeader.attribute("text"), userMessage);
	
	// 3. Create a temporary Instances object containing only this raw instance.
	Instances tempDatasetForFiltering = new Instances(rawInputDataHeader, 0);
	tempDatasetForFiltering.add(rawInstance);
	
	// 4.  Apply the *already trained* filter (this.filter) directly.
	//     DO NOT create a new filter or call setInputFormat again.
	Instances filteredPredictionDataset = Filter.useFilter(tempDatasetForFiltering, this.filter); // Use this.filter
	
	// 5. Check if any instance was produced by the filter.
	if (filteredPredictionDataset.numInstances() == 0) {
		System.err.println("Warning: Filter produced no output for user message: " + userMessage);
		return "unknown"; // Fallback if filtering results in no instance
	}
	// Get the first (and only) transformed instance for classification.
	Instance instanceToPredict = filteredPredictionDataset.firstInstance();
	
	// 6. Classify the transformed instance using the loaded classifier.
	double predictedClassValueIndex = this.classifier.classifyInstance(instanceToPredict);
	
	// 7. Return the predicted class label.
	return filteredPredictionDataset.classAttribute().value((int) predictedClassValueIndex);
}
/**
 * Attempts to extract a known location from a message.
 * This is a simple dictionary-based entity extraction.
 * "@param "message The text message to extract from.
 * @return The extracted location (e.g., "lagos") or null if not found.
 */

private String extractLocation(String message) {
	String lowerCaseMessage = message.toLowerCase();
	String foundLocation = null;
	for (String location : KNOWN_LOCATIONS) {
		if (lowerCaseMessage.matches(".*\\b" + location.toLowerCase() + "\\b.*")) {
			foundLocation = location;
			break;
		}
		
	}
	if (foundLocation == null){
		try{
			String resolvedLocation = resolveLocationViaNominatim(message);
			if (resolvedLocation != null){
				foundLocation = resolvedLocation;
			}
		}
		catch(IOException | JSONException | InterruptedException  e){
			System.err.println("Error resolving location via Nominatim: " + e.getMessage());
		}
	}
	return foundLocation;
}
private String resolveLocationViaNominatim(String query) throws IOException,JSONException,InterruptedException{
	long now = System.currentTimeMillis();
	long timeSinceLastRequest = now - lastNominatimRequestTime;
	if (timeSinceLastRequest < NOMINATIM_RATE_LIMIT_MS){
		long sleeptime = NOMINATIM_RATE_LIMIT_MS - timeSinceLastRequest;
		TimeUnit.MILLISECONDS.sleep(sleeptime);
	}
	String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
	String apiUrl = String.format("%s?q=%s&format=json&limit=1", NOMINATIM_BASE_URL, encodedQuery);
	URL url = new URL(apiUrl);
	HttpURLConnection connection = (HttpURLConnection) url.openConnection();
	connection.setRequestMethod("GET");
	connection.setRequestProperty("User-Agent",USER_AGENT);
	
	int responseCode = connection.getResponseCode();
	if (responseCode == HttpURLConnection.HTTP_OK){
		BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
		String inputLine;
		StringBuilder response = new StringBuilder();
		while ((inputLine = in.readLine()) != null){
			response.append(inputLine);
		}
		in.close();
		
		lastNominatimRequestTime = System.currentTimeMillis();
		
		JSONArray jsonResponseArray = new JSONArray(response.toString());
		if (!jsonResponseArray.isEmpty()){
			JSONObject firstResult = jsonResponseArray.getJSONObject(0);
			return firstResult.getString("display_name").trim();
		}
	}else {
		System.err.println("Error: Nominatim API returned status code " + responseCode);
	}
	return null;
}
private String getWeatherForLocation(String city){
	if (OPENWEATHER_API_KEY.equals("YOUR_OPENWEATHER_API_KEY") || OPENWEATHER_API_KEY.isEmpty()){
		return "I cannot fetch the weather My API key is not configured";
		
	}
	try{
		String encodedCity = URLEncoder.encode(city, StandardCharsets.UTF_8);
		String apiUrl = String.format("%s?q=%s&appid=%s&units=metric",OPENWEATHER_BASE_URL, encodedCity, OPENWEATHER_API_KEY);
		
		URL url = new URL(apiUrl);
		HttpURLConnection connection =  (HttpURLConnection) url.openConnection();
		connection.setRequestMethod("GET");
		
		int responseCode = connection.getResponseCode();
		
		if (responseCode == HttpURLConnection.HTTP_OK){
			BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			String inputLine;
			StringBuilder response = new StringBuilder();
			while ((inputLine = in.readLine()) != null){
				response.append(inputLine);
			}
			in.close();
			JSONObject jsonResponse = new JSONObject(response.toString());
			
			String description = jsonResponse.getJSONArray("weather").getJSONObject(0).getString("description");
			double temp = jsonResponse.getJSONObject("main").getDouble("temp");
			String cityName = jsonResponse.getString("name");
			
			return String.format("The weather in %s is %s with a temperature of %.1f°C.",
					cityName, description, temp);
		} else if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
			return  "I couldn't find weather data for " + city + ". Please check the city name.";
		}else {
			System.err.println("OpenWeatherMap API Error. Response Code: " + responseCode);
			return "I encountered an issue fetching weather data. Please try again later.";
		}
	} catch (IOException e) {
		System.err.println("Network error while fetching weather: " + e.getMessage());
		return "I'm having trouble connecting to the weather service. Please check your internet connection.";
	}catch (JSONException e){
		System.err.println("Error parsing weather JSON: " + e.getMessage());
		return "I received unexpected data from the weather service. Please try again later.";
	}
}

/**
 * Generates Xavier's response based on the predicted intent and conversational context.
 * @param predictedIntent The intent predicted by the Weka classifier.
 * @param userMessage The original user message (used for real-time entity extraction).
 * @return Xavier's friendly response.
 */

/**
 * Loads a list of responses from a text file in the classpath.
 * Each line in the file is treated as a separate response.
 *
 * @param resourcePath The path to the resource file (e.g., "responses/greetings.txt").
 * @return A list of strings, where each string is a response.
 */
private List<String> loadResponsesFromFile(String resourcePath) {
	List<String> responses = new ArrayList<>();
	try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath);
	     BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
		
		if (is == null) {
			System.err.println("ERROR: Cannot find resource file: " + resourcePath);
			responses.add("Sorry, I'm having a bit of trouble formulating a response for that right now.");
			return responses;
		}
		
		String line;
		while ((line = reader.readLine()) != null) {
			line = line.trim();
			if (!line.isEmpty()) {
				responses.add(line);
			}
		}
	} catch (IOException | NullPointerException e) { // Catch NullPointerException if resource not found
		System.err.println("ERROR loading responses from " + resourcePath + ": " + e.getMessage());
		e.printStackTrace(); // Good for debugging
		responses.add("My apologies, I seem to be at a loss for words for that specific case.");
	}
	
	if (responses.isEmpty()) {
		System.err.println("WARNING: No responses loaded from " + resourcePath + ". File might be empty. Using a default.");
		responses.add("I'm not quite sure how to reply to that."); // A generic default
	}
	return responses;
}
// Add this member variable to XavierCoreAI
private String awaitingInformationType = null; // e.g., "expression_for_arithmetic", "location_for_weather"
// You might also want to store the original query that led to the follow-up
private String originalQueryForFollowUp = null;


// Modify generateResponse
private String generateResponse(String predictedIntent, String userMessage) {
	String response;
	String currentMessageLocation = null; // Initialize
	
	// --- Step 1: Handle Follow-up Input if AI is Awaiting Information ---
	if (this.awaitingInformationType != null) {
		String providedInfo = userMessage.trim();
		String intentToExecute = null;
		
		if ("expression_for_arithmetic".equals(this.awaitingInformationType)) {
			try {
				double result = evaluateArithmeticExpression(providedInfo);
				response = "The result of " + providedInfo + " is " + result + ".";
				this.awaitingInformationType = null; // Reset state
				this.originalQueryForFollowUp = null;
				this.lastIntent = "arithmetic_query"; // Set last intent to the fulfilled one
				return response;
			} catch (IllegalArgumentException | UnsupportedOperationException e) {
				System.err.println("Follow-up arithmetic evaluation error: " + e.getMessage());
				if (e.getMessage() != null && e.getMessage().startsWith("EMPTY_EXPRESSION")) {
					response = "That still seems to be empty. Please provide the arithmetic expression you'd like me to evaluate.";
				} else {
					response = "That still doesn't look like a valid arithmetic expression. Please provide an expression like '2 + 2'.";
				}
				// Keep awaitingInformationType set, as we still need the expression
				return response;
			}
		} else if ("location_for_weather".equals(this.awaitingInformationType)) {
			currentMessageLocation = extractLocation(providedInfo);
			if (currentMessageLocation != null) {
				this.extractedLocation = currentMessageLocation;
				response = getWeatherForLocation(this.extractedLocation);
				this.awaitingInformationType = null; // Reset state
				this.originalQueryForFollowUp = null;
				this.extractedLocation = null; // Clear after use
				this.lastIntent = "weather_query"; // Set last intent
				return response;
			} else {
				response = "I still couldn't identify a location in '" + providedInfo + "'. Could you please provide a city name?";
				// Keep awaitingInformationType set
				return response;
			}
		}
		// Add more else-if blocks for other types of awaited information
	}
	
	// --- Step 2: Normal Intent Processing if Not Handling an Immediate Follow-up ---
	// Extract location from the current message if it's relevant for the predicted intent
	// This is a bit tricky because extractLocation might be called again if it's a weather_query
	// We only want to set this.extractedLocation if it's a new context.
	if (!"followup_location".equals(predictedIntent)) { // Avoid re-extracting if it's a direct followup_location intent
		currentMessageLocation = extractLocation(userMessage);
		if (currentMessageLocation != null) {
			this.extractedLocation = currentMessageLocation;
		}
	}
	
	
	switch (predictedIntent) {
		case "greeting":
			if (!this.greetingResponses.isEmpty()) {
				response = this.greetingResponses.get(this.randomGenerator.nextInt(this.greetingResponses.size()));
				if (this.userName != null) {
					response += " " + this.userName + "!"; // Append name if known
				}
			} else {
				response = "Hello there" + (this.userName != null ? ", " + this.userName : "") + "!";
			}
			break;
		case "goodbye":
		if (!this.goodbyeResponses.isEmpty()) {
			response = this.goodbyeResponses.get(this.randomGenerator.nextInt(this.goodbyeResponses.size()));
			if (this.userName != null) {
				response += " " + this.userName + "!"; // Append name if known
			}
		} else {
			response = "Goodbye" + (this.userName != null ? ", " + this.userName : "") + "!";
		}
		break;
		
		
		case "weather_query":
			if (this.extractedLocation != null) { // Location found in current message or carried from previous turn (if logic allows)
				response = getWeatherForLocation(this.extractedLocation);
				this.extractedLocation = null; // Clear after use for this turn
			} else {
				response = "I can tell you the weather. Which location are you interested in?";
				this.awaitingInformationType = "location_for_weather"; // Set state
				this.originalQueryForFollowUp = userMessage;
			}
			break;
		
		case "arithmetic_query":
			String expressionToEvaluate = userMessage;
			String lowerUserMessage = userMessage.toLowerCase();
			String[] prefixes = {"calculate ", "what is ", "evaluate ", "compute ", "solve ", "for "};
			boolean prefixFound = false;
			for (String prefix : prefixes) {
				if (lowerUserMessage.startsWith(prefix)) {
					expressionToEvaluate = userMessage.substring(prefix.length()).trim();
					prefixFound = true;
					break;
				}
			}
			// If it's just numbers and operators, or if a prefix was found and expression is not empty
			if (!expressionToEvaluate.isEmpty() && (prefixFound || expressionToEvaluate.matches(".*[0-9].*"))) {
				try {
					double result = evaluateArithmeticExpression(expressionToEvaluate);
					String sanitizedExpression = userMessage.replaceAll("[^0-9.+\\-*/()\\s]", "");
					sanitizedExpression.trim();
					response = "The result of " + sanitizedExpression + " is " + result + ".";
				} catch (IllegalArgumentException e) {
					System.err.println("Arithmetic evaluation error: " + e.getMessage());
					if (e.getMessage() != null && e.getMessage().startsWith("EMPTY_EXPRESSION")) {
						response = "It seems the expression you provided was empty or didn't contain valid characters. What arithmetic expression would you like me to evaluate?";
						this.awaitingInformationType = "expression_for_arithmetic"; // Set state
						this.originalQueryForFollowUp = userMessage;
					} else {
						response = "I'm sorry, I couldn't evaluate that. Please make sure it's a valid arithmetic expression (e.g., '2 + 2').";
						
					}
				} catch (UnsupportedOperationException e) {
					System.err.println("Arithmetic evaluation failed: " + e.getMessage());
					response = "I'm currently unable to perform calculations due to an internal configuration issue. My apologies.";
				}
			} else { // The initial query was something like "calculate" or "evaluate something" without an expression
				response = "Certainly. What arithmetic expression would you like me to calculate?";
				this.awaitingInformationType = "expression_for_arithmetic"; // Set state
				this.originalQueryForFollowUp = userMessage;
			}
			break;
		
		case "followup_location":
			// This intent is specifically for when the user provides a location after being asked.
			// The `awaitingInformationType` check at the beginning of the method should ideally handle this.
			// However, if `predictIntent` strongly classifies a location name as `followup_location`
			// even when not explicitly awaiting, we can add logic here.
			
			currentMessageLocation = extractLocation(userMessage); // Extract from the current message
			if (currentMessageLocation != null) {
				this.extractedLocation = currentMessageLocation;
				if (this.lastIntent.equals("weather_query") || "location_for_weather".equals(this.awaitingInformationType)) {
					response = getWeatherForLocation(this.extractedLocation);
					this.awaitingInformationType = null; // Reset state
					this.originalQueryForFollowUp = null;
					this.extractedLocation = null; // Clear after use
				} else {
					// User provided a location, but we weren't specifically asking for it for weather.
					response = "You mentioned " + this.extractedLocation + ". What would you like to do regarding this location?";
					// You might set a new awaitingInformationType here, e.g., "action_for_location"
					// and store this.extractedLocation for that context.
				}
			} else {
				response = "I'm not sure which location you're referring to. Could you please specify a city?";
				if (this.lastIntent.equals("weather_query")) { // If the last intent was weather, we are still waiting for a location
					this.awaitingInformationType = "location_for_weather";
				}
			}
			break;
		case "set_user_name":
			String extracted = extractNameFromMessage(userMessage);
			if (extracted != null && !extracted.isEmpty()) {
				this.userName = extracted;
				response = "Okay, I'll call you " + this.userName + ". Nice to meet you, " + this.userName + "!";
			} else {
				response = "I'm sorry, I didn't quite catch your name. Could you please tell me again, for example, by saying 'My name is [Your Name]'?";
				// Optionally, you could set awaitingInformationType here if you have that system
				// this.awaitingInformationType = "user_name";
			}
			break;
		// ... other cases ...
		case "feeling_status":
			if (!this.feelingResponses.isEmpty()) {
				response = this.feelingResponses.get(this.randomGenerator.nextInt(this.feelingResponses.size()));
			} else {
				response = "I understand. How can I help?";
			}
			break;
		case "unknown":
		default:
			if (!this.unknownResponses.isEmpty()) {
				response = this.unknownResponses.get(this.randomGenerator.nextInt(this.unknownResponses.size()));
			} else {
				response = "I'm sorry, I didn't understand that.";
			}
			break;
	}
	
	
	if (this.awaitingInformationType == null && !"weather_query".equals(predictedIntent) && !"followup_location".equals(predictedIntent)) {
		// If we are not waiting for info, and the current intent didn't use the location,
		// and it wasn't a followup_location itself, clear any passively extracted location.
		// This prevents "You mentioned London" if the next query is "tell me a joke".
		// However, if `this.extractedLocation` was set by the current `userMessage` and `predictedIntent`
		// is something like `weather_query` that *will* use it, it should not be nulled here yet.
		// The clearing is now mostly handled within the cases or when `awaitingInformationType` is reset.
	}
	
	return response;
}

private double evaluateArithmeticExpression(String userMessage) {
	
	ScriptEngineManager  manager = new ScriptEngineManager();
	ScriptEngine engine = manager.getEngineByName("JavaScript");
	
	if (engine == null) {
		System.err.println("Engine not found, cannot evaluate expression");
		throw new UnsupportedOperationException("JavaScript engine (e.g., Nashorn or Graal.js) not available to evaluate expression.");
	}
	try {
		String sanitizedExpression = userMessage.replaceAll("[^0-9.+\\-*/()\\s]", "");
		if (sanitizedExpression.trim().isEmpty()) {
			// THROW an exception instead of re-prompting
			throw new IllegalArgumentException("EMPTY_EXPRESSION_AFTER_SANITIZATION: " + userMessage);
		}
		Object result = engine.eval(sanitizedExpression);
		
		if (result instanceof Number) {
			return ((Number) result).doubleValue();
		} else {
			System.err.println("Expression did not evaluate to a numeric value: " + sanitizedExpression + " (Evaluated to: " + result + ")");
			throw new IllegalArgumentException("NON_NUMERIC_RESULT: " + sanitizedExpression);
		}
	} catch (ScriptException e) {
		System.err.println("Error evaluating arithmetic expression '" + userMessage + "': " + e.getMessage());
		throw new IllegalArgumentException("SYNTAX_ERROR: " + userMessage, e);
	}
	
}
// In XavierCoreAI.java
private String extractNameFromMessage(String userMessage) {
	String lowerUserMessage = userMessage.toLowerCase();
	String extractedName = null;
	
	// Patterns to identify name introductions.
	// More specific patterns can be added or ordered by preference.
	String[] patterns = {
			"my full name is ",
			"my name is ",
			"please remember my name is ",
			"you can call me ",
			"just call me ",
			"call me ",
			"i go by ",
			"i'm known as ",
			"people call me ",
			// "i am ", // Can be ambiguous, handle with care or more context
			// "i'm "   // Similar to "i am"
	};
	
	for (String pattern : patterns) {
		int startIndex = lowerUserMessage.indexOf(pattern);
		if (startIndex != -1) {
			// Extract the part after the pattern
			String potentialNameSegment = userMessage.substring(startIndex + pattern.length()).trim();
			
			// Heuristic: take the first 1-3 words after the pattern as the name.
			// This helps avoid grabbing a whole sentence if the user continues talking.
			String[] words = potentialNameSegment.split("\\s+");
			if (words.length > 0) {
				extractedName = words[0];
				if (words.length > 1 && (extractedName.length() + words[1].length() < 20)) {
					// Check if the second word is likely part of a name (e.g., not a common conjunction)
					boolean isSecondWordConjunction = Arrays.asList("and", "or", "but", "so", "is", "was", "the", "a", "for", "to", "in", "on", "at", "by", "from", "with", "who", "which", "that", "because").contains(words[1].toLowerCase());
					if (!isSecondWordConjunction) {
						extractedName += " " + words[1];
						if (words.length > 2 && (extractedName.length() + words[2].length() < 25)) {
							boolean isThirdWordConjunction = Arrays.asList("and", "or", "but", "so", "is", "was", "the", "a", "for", "to", "in", "on", "at", "by", "from", "with", "who", "which", "that", "because").contains(words[2].toLowerCase());
							if(!isThirdWordConjunction) {
								extractedName += " " + words[2];
							}
						}
					}
				}
			}
			
			if (extractedName != null && !extractedName.isEmpty()) {
				// Remove trailing punctuation from the extracted segment
				if (extractedName.endsWith(".") || extractedName.endsWith("!") || extractedName.endsWith("?")) {
					extractedName = extractedName.substring(0, extractedName.length() - 1).trim();
				}
				
				// Capitalize each part of the name
				String[] nameTokens = extractedName.split("\\s+");
				StringBuilder capitalizedNameBuilder = new StringBuilder();
				for (String token : nameTokens) {
					if (!token.isEmpty()) {
						capitalizedNameBuilder.append(Character.toUpperCase(token.charAt(0)))
								.append(token.length() > 1 ? token.substring(1).toLowerCase() : "")
								.append(" ");
					}
				}
				extractedName = capitalizedNameBuilder.toString().trim();
				break; // Found a name using a primary pattern, stop searching
			} else {
				extractedName = null; // Reset if processing led to an empty name
			}
		}
	}
	
	// Fallback: If no pattern matched (e.g., user just says "Dammy", perhaps after being prompted)
	// This part assumes the intent is already classified as 'set_user_name'.
	if (extractedName == null || extractedName.isEmpty()) {
		String trimmedMessage = userMessage.trim();
		
		// Remove common leading conversational fluff if the message is short
		String[] leadingFluff = {"hi ", "hello ", "hey ", "it's ", "i'm ", "i am "}; // Added "i'm", "i am" here for direct name case
		for (String fluff : leadingFluff) {
			if (trimmedMessage.toLowerCase().startsWith(fluff)) {
				trimmedMessage = trimmedMessage.substring(fluff.length()).trim();
				break;
			}
		}
		
		String[] words = trimmedMessage.split("\\s+");
		// Consider it a name if it's 1-3 words after stripping fluff and isn't a pattern itself
		if (words.length > 0 && words.length <= 3) {
			boolean isPatternItself = false;
			for(String p : patterns) { // Check if the trimmed message is one of the patterns
				if (trimmedMessage.toLowerCase().equals(p.trim())) {
					isPatternItself = true;
					break;
				}
			}
			if(!isPatternItself) {
				extractedName = trimmedMessage;
				if (extractedName.endsWith(".") || extractedName.endsWith("!") || extractedName.endsWith("?")) {
					extractedName = extractedName.substring(0, extractedName.length() - 1).trim();
				}
				if (!extractedName.isEmpty()) {
					String[] nameTokens = extractedName.split("\\s+");
					StringBuilder capitalizedNameBuilder = new StringBuilder();
					for (String token : nameTokens) {
						if (!token.isEmpty()) {
							capitalizedNameBuilder.append(Character.toUpperCase(token.charAt(0)))
									.append(token.length() > 1 ? token.substring(1).toLowerCase() : "")
									.append(" ");
						}
					}
					extractedName = capitalizedNameBuilder.toString().trim();
				}
			}
		}
	}
	
	// Final validation (length, common non-name words, special characters)
	if (extractedName != null && !extractedName.isEmpty()) {
		String lowerExtractedName = extractedName.toLowerCase();
		List<String> stopWords = Arrays.asList( // Words that are unlikely to be names on their own
				"what", "how", "who", "when", "why", "is", "are", "am", "the", "a", "an",
				"yes", "no", "ok", "okay", "hi", "hello", "hey", "thanks", "thank", "you",
				"please", "sorry", "bye", "goodbye", "me", "my", "i", "it", "name",
				"and", "but", "or", "so", "for", "to", "in", "on", "at", "by", "from", "with",
				"calculate", "weather", "joke", "time", "feeling" // common command words
		);
		
		boolean isInvalid = extractedName.split("\\s+").length > 3 || // Max 3 words for a name
				                    extractedName.length() > 35 || // Max 35 chars overall
				                    (extractedName.length() == 1 && !Character.isUpperCase(extractedName.charAt(0))) ||
				                    lowerExtractedName.matches(".*[0-9!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*");
		
		if (isInvalid) {
			return null;
		}
		// If the extracted name (as a whole) is a stop word, it's likely not a name.
		if (stopWords.contains(lowerExtractedName)) {
			return null;
		}
		
	} else {
		return null; // No valid name extracted
	}
	
	return extractedName;
}
}
