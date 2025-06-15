package com.f9ld3.xavier.ai; // IMPORTANT: Ensure this matches your package name

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
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime; // NEW: For time_query intent
import java.time.format.DateTimeFormatter; // NEW: For formatting time
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

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

// A simple, predefined list of locations Xavier knows about for extraction
private static final HashSet<String> KNOWN_LOCATIONS = new HashSet<>(Arrays.asList(
		"lagos", "london", "new york", "paris", "abuja", "tokyo", "beijing", "dubai", "sydney"
));

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
	
	if (OPENWEATHER_API_KEY.equals("YOUR_OPENWEATHER_API_KEY") || OPENWEATHER_API_KEY.isEmpty()){
		System.err.println("WARNING: OpenWeatherMap API key not set. Weather queries will not work. Please update OPENWEATHER_API_KEY in XavierCoreAI.java");
	}
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
 * @param message The text message to extract from.
 * @return The extracted location (e.g., "lagos") or null if not found.
 */
private String extractLocation(String message) {
	String lowerCaseMessage = message.toLowerCase();
	for (String location : KNOWN_LOCATIONS) {
		if (lowerCaseMessage.matches(".*\\b" + location + "\\b.*")) {
			return location;
		}
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
private String generateResponse(String predictedIntent, String userMessage) {
	String response;
	
	// Attempt to extract location from the current message right away.
	// This updates the global 'extractedLocation' for potential future use in dialogue.
	String currentMessageLocation = extractLocation(userMessage);
	if (currentMessageLocation != null) {
		this.extractedLocation = currentMessageLocation;
	}
	
	switch (predictedIntent) {
		case "greeting":
			response = "Hello there! How can I assist you today?";
			break;
		case "weather_query":
			if (this.extractedLocation != null) {
				response = getWeatherForLocation(this.extractedLocation); // Call API here!
				this.extractedLocation = null;
			} else {
				response = "I can tell you the weather. Which location are you interested in?";
			}
			break;
		case "joke_request":
			response = "Why don't scientists trust atoms? Because they make up everything!";
			break;
		case "goodbye":
			response = "Goodbye! It was nice chatting with you.";
			break;
		case "followup_location":
			// This intent is typically triggered by a standalone location.
			// Check if it's a valid follow-up to a weather query and a location was found.
			if (this.lastIntent.equals("weather_query") && this.extractedLocation != null) {
				response = "Ah, so you'd like the weather in " + this.extractedLocation + ". Let me check... (This would involve a real weather API call!)";
				this.extractedLocation = null;
			} else if (this.extractedLocation != null) {
				// Location was found, but not in a weather context.
				response = "You mentioned " + this.extractedLocation + ", but I'm not sure what you want to do with that information right now. Could you clarify?";
				this.extractedLocation = null;
			} else {
				// Follow-up location intent but no clear location found in message or context.
				response = "You mentioned a location, but I couldn't identify it or relate it to our previous conversation. Can you please specify a city?";
			}
			break;
		case "gratitude": // NEW INTENT RESPONSE
			response = "You're very welcome! I'm glad I could help.";
			break;
		case "affirmation": // NEW INTENT RESPONSE
			response = "Understood. Is there anything else I can assist with?";
			break;
		case "negation": // NEW INTENT RESPONSE
			response = "Alright. Please let me know if you change your mind or need something else.";
			break;
		case "personal_question": // NEW INTENT RESPONSE
			// Basic responses for personal questions. You can expand these!
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
		case "time_query": // NEW INTENT RESPONSE
			// Get current local time and format it
			LocalTime currentTime = LocalTime.now();
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("hh:mm:ss a"); // e.g., 03:30:15 PM
			response = "The current time is " + currentTime.format(formatter) + ".";
			break;
		case "feeling_status": // NEW INTENT RESPONSE
			response = "I see. While I don't experience feelings, I hope you feel better/continue to feel good! How can I help you further?";
			break;
		case "unknown":
		default: // Handles "unknown" and any other unexpected intent string
			response = "I'm sorry, I didn't quite understand that. Could you rephrase?";
			break;
	}
	return response;
}
}
