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
private String generateResponse(String predictedIntent, String userMessage) {
	String response;
	
	String currentMessageLocation = extractLocation(userMessage);
	if (currentMessageLocation != null) {
		this.extractedLocation = currentMessageLocation;
	}
	
	// Random generator is now a member variable: this.randomGenerator
	
	switch (predictedIntent) {
		case "greeting":
			// Use the pre-loaded list
			if (!this.greetingResponses.isEmpty()) {
				response = this.greetingResponses.get(this.randomGenerator.nextInt(this.greetingResponses.size()));
			} else {
				response = "Hello there!"; // Fallback if list is somehow empty after loading attempt
			}
			break;
		case "weather_query":
			if (this.extractedLocation != null) {
				response = getWeatherForLocation(this.extractedLocation);
				this.extractedLocation = null;
			} else {
				response = "I can tell you the weather. Which location are you interested in?";
			}
			break;
		case "joke_request":
			// Use the pre-loaded list
			if (!this.jokeResponses.isEmpty()) {
				response = this.jokeResponses.get(this.randomGenerator.nextInt(this.jokeResponses.size()));
			} else {
				response = "I tried to think of a joke, but I'm drawing a blank!"; // Fallback
			}
			break;
		case "goodbye":
			// Use the pre-loaded list
			if (!this.goodbyeResponses.isEmpty()) {
				response = this.goodbyeResponses.get(this.randomGenerator.nextInt(this.goodbyeResponses.size()));
			} else {
				response = "Goodbye!"; // Fallback
			}
			break;
		case "followup_location":
			if (this.lastIntent.equals("weather_query") && this.extractedLocation != null) {
				response = "Ah, so you'd like the weather in " + " " + this.extractedLocation + " " + getWeatherForLocation(this.extractedLocation);
				this.extractedLocation = null;
			} else if (this.extractedLocation != null) {
				response = "You mentioned " + this.extractedLocation + ", but I'm not sure what you want to do with that information right now. Could you clarify?";
				this.extractedLocation = null;
			} else {
				response = "You mentioned a location, but I couldn't identify it or relate it to our previous conversation. Can you please specify a city?";
			}
			break;
		case "gratitude":
			// Use the pre-loaded list
			if (!this.gratitudeResponses.isEmpty()) {
				response = this.gratitudeResponses.get(this.randomGenerator.nextInt(this.gratitudeResponses.size()));
			} else {
				response = "You're welcome!"; // Fallback
			}
			break;
		case "affirmation":
			// Use the pre-loaded list
			if (!this.affirmationResponses.isEmpty()) {
				response = this.affirmationResponses.get(this.randomGenerator.nextInt(this.affirmationResponses.size()));
			} else {
				response = "Okay."; // Fallback
			}
			break;
		case "negation":
			// Use the pre-loaded list
			if (!this.negationResponses.isEmpty()) {
				response = this.negationResponses.get(this.randomGenerator.nextInt(this.negationResponses.size()));
			} else {
				response = "Understood."; // Fallback
			}
			break;
		case "personal_question":
			// This logic can remain or also be moved to a file if it grows complex
			if (userMessage.toLowerCase().contains("name")) {
				response = "I am Xavier, your personal AI companion.";
			} else if (userMessage.toLowerCase().contains("how are you")) {
				response = "As an AI, I don't have feelings, but I'm operating perfectly and ready to assist you!";
			} else if (userMessage.toLowerCase().contains("who are you")) {
				response = "I am Xavier, an AI designed to assist you.";
			} else if (userMessage.toLowerCase().contains("purpose")) {
				response = "My purpose is to understand your requests and help you efficiently.";
			} else {
				response = "I am an AI, designed to assist you. Is there something specific you'd like to know about me?";
			}
			break;
		case "time_query":
			LocalTime currentTime = LocalTime.now();
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("hh:mm:ss a");
			response = "The current time is " + currentTime.format(formatter) + ".";
			break;
		case "feeling_status":
			// Use the pre-loaded list
			if (!this.feelingResponses.isEmpty()) {
				response = this.feelingResponses.get(this.randomGenerator.nextInt(this.feelingResponses.size()));
			} else {
				response = "I understand. How can I help?"; // Fallback
			}
			break;
		case "unknown":
		default:
			// Use the pre-loaded list
			if (!this.unknownResponses.isEmpty()) {
				response = this.unknownResponses.get(this.randomGenerator.nextInt(this.unknownResponses.size()));
			} else {
				response = "I'm sorry, I didn't understand that."; // Fallback
			}
			break;
	}
	return response;
}
}
