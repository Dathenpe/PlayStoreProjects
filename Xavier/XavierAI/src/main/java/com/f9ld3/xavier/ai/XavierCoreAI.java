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
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors; // Added for stream operations

/**
 * XavierCoreAI: Encapsulates the core AI logic for intent classification,
 * simple entity extraction, and dialogue management using Weka.
 * This class handles loading the trained model and filter, making predictions,
 * and generating contextual responses.
 */
public class XavierCoreAI {

// API CONFIGURATION
private static final String OPENWEATHER_API_KEY = "a05a0c427992d0bee9a9624548399407";
private static final String OPENWEATHER_BASE_URL = "http://api.openweathermap.org/data/2.5/weather";

// DuckDuckGo Instant Answer API - No API Key needed
private static final String DUCKDUCKGO_API_BASE_URL = "https://api.duckduckgo.com/";

// Free Dictionary API - No API Key needed
private static final String FREE_DICTIONARY_API_BASE_URL = "https://api.dictionaryapi.dev/api/v2/entries/en/";

// Numbers API - No API Key needed
private static final String NUMBERS_API_BASE_URL = "http://numbersapi.com/";

private static final String NOMINATIM_BASE_URL = "https://nominatim.openstreetmap.org/search";
private static final String USER_AGENT = "XavierAI/1.0 (adetayoadedamola6@gmail.com)";
private static long lastNominatimRequestTime = 0;
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
		"gratitude", "affirmation", "negation", "personal_question", "time_query", "feeling_status",
		"arithmetic_query", "set_user_name", "factual_query"
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
		"bogota", "quito", "panama city", "san jose costa rica", "guadalajara", "monterrey", "istanbul",
		"quebec" // Added common spelling for Quebec
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
 *
 * @param message The text message to extract from.
 * @return The extracted location (e.g., "lagos") or null if not found.
 */

private String extractLocation(String message) {
	String lowerCaseMessage = message.toLowerCase();
	String foundLocation = null;
	
	// 1. Try matching against KNOWN_LOCATIONS first (fastest)
	for (String location : KNOWN_LOCATIONS) {
		if (lowerCaseMessage.matches(".*\\b" + Pattern.quote(location.toLowerCase()) + "\\b.*")) { // Use Pattern.quote for literal matching
			foundLocation = location;
			break;
		}
	}
	
	// 2. If no direct match, try to extract a plausible location from common phrases
	if (foundLocation == null) {
		Pattern locationPattern = Pattern.compile("in\\s+([a-zA-Z\\s]+(?:\\s+(?:city|state|country|republic))?)\\b", Pattern.CASE_INSENSITIVE);
		Matcher matcher = locationPattern.matcher(lowerCaseMessage);
		if (matcher.find()) {
			String extracted = matcher.group(1).trim();
			// Basic cleaning for common trailing words/punctuation
			extracted = extracted.replaceAll("\\b(city|state|country|republic)\\b$", "").trim();
			extracted = extracted.replaceAll("[.!?]$", "").trim(); // Remove trailing punctuation
			if (!extracted.isEmpty()) {
				foundLocation = extracted;
			}
		}
	}
	
	// 3. If still not found, try resolving via Nominatim API (most powerful but slowest, rate-limited)
	if (foundLocation == null && message != null && !message.trim().isEmpty()) {
		try {
			// Only query Nominatim if the message itself is not too short or generic
			if (message.split("\\s+").length > 1 || !KNOWN_LOCATIONS.contains(lowerCaseMessage)) { // Avoid querying for single common words
				String resolvedLocation = resolveLocationViaNominatim(message);
				if (resolvedLocation != null) {
					// Nominatim returns a full display name, which might be long.
					// For time zone lookup, we often need a more concise city/country name.
					// This heuristic attempts to get the first part or a more relevant part.
					// Example: "Paris, France" -> "Paris"
					// Example: "Lagos, Nigeria" -> "Lagos"
					// Example: "California, USA" -> "California"
					String[] parts = resolvedLocation.split(", ");
					if (parts.length > 0) {
						// Prioritize first part (city), or try to find a known part
						String primaryPart = parts[0].trim();
						if (KNOWN_LOCATIONS.contains(primaryPart.toLowerCase())) {
							foundLocation = primaryPart;
						} else {
							// If primary part isn't known, try the full resolved name or the first part
							foundLocation = primaryPart; // Default to the first part
						}
					} else {
						foundLocation = resolvedLocation; // Use full resolved name if no comma
					}
				}
			}
		} catch (IOException | JSONException | InterruptedException e) {
			System.err.println("Error resolving location via Nominatim: " + e.getMessage() + "Please Check your internet connection");
		}
	}
	return foundLocation;
}

private String resolveLocationViaNominatim(String query) throws IOException, JSONException, InterruptedException {
	long now = System.currentTimeMillis();
	long timeSinceLastRequest = now - lastNominatimRequestTime;
	if (timeSinceLastRequest < NOMINATIM_RATE_LIMIT_MS) {
		long sleeptime = NOMINATIM_RATE_LIMIT_MS - timeSinceLastRequest;
		TimeUnit.MILLISECONDS.sleep(sleeptime);
	}
	String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
	String apiUrl = String.format("%s?q=%s&format=json&limit=1", NOMINATIM_BASE_URL, encodedQuery);
	URL url = new URL(apiUrl);
	HttpURLConnection connection = (HttpURLConnection) url.openConnection();
	connection.setRequestMethod("GET");
	connection.setRequestProperty("User-Agent", USER_AGENT);
	
	int responseCode = connection.getResponseCode();
	if (responseCode == HttpURLConnection.HTTP_OK) {
		BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
		String inputLine;
		StringBuilder response = new StringBuilder();
		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}
		in.close();
		
		lastNominatimRequestTime = System.currentTimeMillis();
		
		JSONArray jsonResponseArray = new JSONArray(response.toString());
		if (!jsonResponseArray.isEmpty()) {
			JSONObject firstResult = jsonResponseArray.getJSONObject(0);
			return firstResult.getString("display_name").trim();
		}
	} else {
		System.err.println("Error: Nominatim API returned status code " + responseCode);
	}
	return null;
}

private String getWeatherForLocation(String city) {
	if (OPENWEATHER_API_KEY.equals("YOUR_OPENWEATHER_API_KEY") || OPENWEATHER_API_KEY.isEmpty()) {
		return "I cannot fetch the weather. My API key is not configured.";
	}
	try {
		String encodedCity = URLEncoder.encode(city, StandardCharsets.UTF_8);
		String apiUrl = String.format("%s?q=%s&appid=%s&units=metric", OPENWEATHER_BASE_URL, encodedCity, OPENWEATHER_API_KEY);
		
		URL url = new URL(apiUrl);
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod("GET");
		
		int responseCode = connection.getResponseCode();
		
		if (responseCode == HttpURLConnection.HTTP_OK) {
			BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			String inputLine;
			StringBuilder response = new StringBuilder();
			while ((inputLine = in.readLine()) != null) {
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
			return "I couldn't find weather data for " + city + ". Please check the city name.";
		} else {
			System.err.println("OpenWeatherMap API Error. Response Code: " + responseCode);
			return "I encountered an issue fetching weather data. Please try again later.";
		}
	} catch (IOException e) {
		System.err.println("Network error while fetching weather: " + e.getMessage());
		return "I'm having trouble connecting to the weather service. Please check your internet connection.";
	} catch (JSONException e) {
		System.err.println("Error parsing weather JSON: " + e.getMessage());
		return "I received unexpected data from the weather service. Please try again later.";
	}
}


/**
 * Fetches an answer from the DuckDuckGo Instant Answer API.
 *
 * @param query The factual query.
 * @return The answer from DuckDuckGo, or an error message.
 */
private String fetchDuckDuckGoAnswer(String query) {
	try {
		// Added no_html=1 and skip_disambig=1 for cleaner, more direct answers
		String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
		String apiUrl = String.format("%s?q=%s&format=json&t=XavierAI&no_html=1&skip_disambig=1", DUCKDUCKGO_API_BASE_URL, encodedQuery);
		
		URL url = new URL(apiUrl);
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod("GET");
		connection.setRequestProperty("User-Agent", USER_AGENT);
		
		int responseCode = connection.getResponseCode();
		if (responseCode == HttpURLConnection.HTTP_OK) {
			BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			String inputLine;
			StringBuilder response = new StringBuilder();
			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
			in.close();
			
			JSONObject jsonResponse = new JSONObject(response.toString());
			
			// Prioritize 'Answer' field if available (often for direct calculations or quick facts)
			String answer = jsonResponse.optString("Answer", "");
			if (!answer.isEmpty()) {
				return answer;
			}
			
			// *** FIX: Changed "AbstractText" to "Abstract" to match the DuckDuckGo API response ***
			String abstractText = jsonResponse.optString("Abstract", "");
			String abstractSource = jsonResponse.optString("AbstractSource", "");
			String abstractURL = jsonResponse.optString("AbstractURL", "");
			
			if (!abstractText.isEmpty()) {
				StringBuilder result = new StringBuilder(abstractText);
				if (!abstractSource.isEmpty()) {
					result.append(" (Source: ").append(abstractSource);
					if (!abstractURL.isEmpty()) {
						result.append(", ").append(abstractURL);
					}
					result.append(")");
				}
				return result.toString();
			}
			
			// Fallback to 'Definition'
			String definition = jsonResponse.optString("Definition", "");
			if (!definition.isEmpty()) {
				return definition;
			}
			
			// Fallback to 'Result' (often contains HTML, but we use no_html=1)
			String resultHtml = jsonResponse.optString("Result", "");
			if (!resultHtml.isEmpty()) {
				// This might still contain some formatting, but for now we'll return it directly
				return resultHtml;
			}
			
			// Fallback to 'RelatedTopics' if nothing else is found
			if (jsonResponse.has("RelatedTopics") && jsonResponse.getJSONArray("RelatedTopics").length() > 0) {
				JSONArray relatedTopics = jsonResponse.getJSONArray("RelatedTopics");
				if (relatedTopics.length() > 0 && relatedTopics.optJSONObject(0) != null) {
					JSONObject firstRelated = relatedTopics.optJSONObject(0);
					if (firstRelated.has("Text")) {
						return "According to a related topic: " + firstRelated.getString("Text");
					}
				}
			}
			return "I couldn't find a direct answer for '" + query + "'.";
		} else {
			System.err.println("DuckDuckGo API Error. Response Code: " + responseCode);
			return "I encountered an issue fetching factual data. Please try again later.";
		}
	} catch (IOException e) {
		System.err.println("Network error while fetching DuckDuckGo answer: " + e.getMessage());
		return "I'm having trouble connecting to the knowledge service. Please check your internet connection.";
	} catch (JSONException e) {
		System.err.println("Error parsing DuckDuckGo JSON: " + e.getMessage());
		return "I received unexpected data from the knowledge service. Please try again later.";
	}
}
/**
 * Fetches a definition from the Free Dictionary API.
 *
 * @param word The word to define.
 * @return The definition, or an error message.
 */
private String fetchDictionaryDefinition(String word) {
	try {
		String encodedWord = URLEncoder.encode(word, StandardCharsets.UTF_8);
		String apiUrl = FREE_DICTIONARY_API_BASE_URL + encodedWord;
		
		URL url = new URL(apiUrl);
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod("GET");
		connection.setRequestProperty("User-Agent", USER_AGENT);
		
		int responseCode = connection.getResponseCode();
		if (responseCode == HttpURLConnection.HTTP_OK) {
			BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			String inputLine;
			StringBuilder response = new StringBuilder();
			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
			in.close();
			
			JSONArray jsonResponse = new JSONArray(response.toString());
			if (jsonResponse.length() > 0) {
				JSONObject entry = jsonResponse.getJSONObject(0);
				JSONArray meanings = entry.getJSONArray("meanings");
				if (meanings.length() > 0) {
					StringBuilder definition = new StringBuilder("Meaning of '" + word + "':\n");
					for (int i = 0; i < meanings.length(); i++) {
						JSONObject meaning = meanings.getJSONObject(i);
						String partOfSpeech = meaning.getString("partOfSpeech");
						definition.append("- ").append(partOfSpeech).append(":\n");
						JSONArray definitionsArray = meaning.getJSONArray("definitions");
						for (int j = 0; j < Math.min(definitionsArray.length(), 2); j++) { // Get up to 2 definitions
							definition.append("  ").append(j + 1).append(". ")
									.append(definitionsArray.getJSONObject(j).getString("definition")).append("\n");
						}
					}
					return definition.toString().trim();
				}
			}
			return "I couldn't find a definition for '" + word + "'.";
		} else if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
			return "I couldn't find a definition for '" + word + "'. It might be a proper noun or a very obscure word.";
		} else {
			System.err.println("Free Dictionary API Error. Response Code: " + responseCode);
			return "I encountered an issue fetching dictionary data. Please try again later.";
		}
	} catch (IOException e) {
		System.err.println("Network error while fetching dictionary definition: " + e.getMessage());
		return "I'm having trouble connecting to the dictionary service. Please check your internet connection.";
	} catch (JSONException e) {
		System.err.println("Error parsing Free Dictionary JSON: " + e.getMessage());
		return "I received unexpected data from the dictionary service. Please try again later.";
	}
}

/**
 * Fetches a fact about a number from the Numbers API.
 *
 * @param number The number to get a fact about.
 * @param type   The type of fact (trivia, math, date, year).
 * @return The number fact, or an error message.
 */
private String fetchNumberFact(String number, String type) {
	try {
		String apiUrl = String.format("%s%s/%s", NUMBERS_API_BASE_URL, URLEncoder.encode(number, StandardCharsets.UTF_8), type);
		
		URL url = new URL(apiUrl);
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod("GET");
		connection.setRequestProperty("User-Agent", USER_AGENT);
		
		int responseCode = connection.getResponseCode();
		if (responseCode == HttpURLConnection.HTTP_OK) {
			BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			String inputLine;
			StringBuilder response = new StringBuilder();
			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
			in.close();
			return response.toString().trim(); // Numbers API returns plain text fact
		} else {
			System.err.println("Numbers API Error. Response Code: " + responseCode);
			return "I couldn't find a fact about the number '" + number + "'.";
		}
	} catch (IOException e) {
		System.err.println("Network error while fetching number fact: " + e.getMessage());
		return "I'm having trouble connecting to the numbers fact service. Please check your internet connection.";
	}
}


/**
 * Generates Xavier's response based on the predicted intent and conversational context.
 *
 * @param predictedIntent The intent predicted by the Weka classifier.
 * @param userMessage     The original user message (used for real-time entity extraction).
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


// New method to handle time queries
private String handleTimeQuery(String userMessage) {
	String response;
	// Use the extractLocation method that leverages KNOWN_LOCATIONS and Nominatim
	String locationForTime = extractLocation(userMessage);
	ZoneId zoneId = null;
	
	if (locationForTime != null) {
		// Normalize the location for lookup
		String normalizedLocation = locationForTime.toLowerCase().replace(" ", "_");
		
		// 1. Try to find a direct mapping for common locations
		// This acts like your old switch statement but is more scalable if you externalize the map
		// For now, let's keep a small internal map for highly common cases for speed
		switch (normalizedLocation) {
			case "nigeria":
			case "lagos":
			case "abuja":
				zoneId = ZoneId.of("Africa/Lagos");
				break;
			case "london":
			case "united_kingdom": // This requires normalized input from extractLocation
			case "uk":
				zoneId = ZoneId.of("Europe/London");
				break;
			case "new_york":
			case "new_york_city":
			case "nyc":
				zoneId = ZoneId.of("America/New_York");
				break;
			case "tokyo":
			case "japan":
				zoneId = ZoneId.of("Asia/Tokyo");
				break;
			case "paris":
			case "france":
				zoneId = ZoneId.of("Europe/Paris");
				break;
			case "berlin":
			case "germany":
				zoneId = ZoneId.of("Europe/Berlin");
				break;
			case "sydney":
			case "australia":
				zoneId = ZoneId.of("Australia/Sydney"); // Default to a major city for country
				break;
			case "los_angeles":
			case "california":
				zoneId = ZoneId.of("America/Los_Angeles");
				break;
			case "toronto":
			case "ontario":
			case "canada":
			case "quebec": // Added for direct match
				zoneId = ZoneId.of("America/Toronto"); // Default to a major city for country
				break;
			case "dubai":
			case "united_arab_emirates":
			case "uae":
				zoneId = ZoneId.of("Asia/Dubai");
				break;
			case "beijing":
			case "shanghai":
			case "china":
				zoneId = ZoneId.of("Asia/Shanghai"); // China uses a single timezone
				break;
			case "delhi":
			case "mumbai":
			case "india":
				zoneId = ZoneId.of("Asia/Kolkata");
				break;
			case "mexico_city":
			case "mexico":
				zoneId = ZoneId.of("America/Mexico_City");
				break;
			case "moscow":
			case "russia":
				zoneId = ZoneId.of("Europe/Moscow"); // Default to a major city
				break;
			case "seoul":
			case "south_korea":
				zoneId = ZoneId.of("Asia/Seoul");
				break;
			case "rome":
			case "italy":
				zoneId = ZoneId.of("Europe/Rome");
				break;
			case "madrid":
			case "spain":
				zoneId = ZoneId.of("Europe/Madrid");
				break;
			case "cairo":
			case "egypt":
				zoneId = ZoneId.of("Africa/Cairo");
				break;
			case "cape_town":
			case "johannesburg":
			case "south_africa":
				zoneId = ZoneId.of("Africa/Johannesburg");
				break;
			case "singapore":
			case "hong_kong":
				zoneId = ZoneId.of("Asia/Singapore");
				break;
			case "brazil":
			case "saopaulo":
			case "rio": // Rio de Janeiro
				zoneId = ZoneId.of("America/Sao_Paulo");
				break;
			case "argentina":
			case "buenos_aires":
				zoneId = ZoneId.of("America/Argentina/Buenos_Aires");
				break;
			default:
				// 2. Try to directly interpret the extracted location as a ZoneId (e.g., "America/Los_Angeles")
				try {
					zoneId = ZoneId.of(locationForTime);
				} catch (java.time.DateTimeException e) {
					System.err.println("Location '" + locationForTime + "' is not a direct ZoneId.");
					// Not a direct ZoneId, proceed to flexible search
				}
				
				// 3. If still no ZoneId, perform fuzzy matching against all available ZoneIds
				if (zoneId == null) {
					Set<String> availableZoneIds = ZoneId.getAvailableZoneIds();
					
					// Prioritize exact matches of city or country parts after normalization
					// e.g., "lisbon" -> Europe/Lisbon, "kolkata" -> Asia/Kolkata
					String bestGuessZoneId = availableZoneIds.stream()
							                         .filter(id -> id.toLowerCase().endsWith("/" + normalizedLocation) ||
									                                       id.toLowerCase().equals(normalizedLocation) || // For root IDs like "GMT" or direct country names if they exist as ZoneIds
									                                       id.toLowerCase().contains("/" + normalizedLocation + "_") || // Handles "New_York" in "America/New_York"
									                                       id.toLowerCase().startsWith(normalizedLocation + "/")) // Handles "America" or "Europe" if user searches broadly
							                         .findFirst().orElse(null);
					
					// A more robust search might consider word boundaries and tokenizing the location
					// For instance, "united kingdom" might become "united_kingdom", then search for that in IDs.
					
					if (bestGuessZoneId != null) {
						zoneId = ZoneId.of(bestGuessZoneId);
					} else {
						// Final fallback: check for partial matches anywhere in the ZoneId
						// This is broad and might give less precise results but increases coverage
						bestGuessZoneId = availableZoneIds.stream()
								                  .filter(id -> id.toLowerCase().contains(normalizedLocation.replace("_", ""))) // Remove underscores for broader matching
								                  .findFirst().orElse(null);
						
						if (bestGuessZoneId != null) {
							zoneId = ZoneId.of(bestGuessZoneId);
						}
					}
				}
				break; // Break from the switch after the default logic
		}
	}
	
	DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("h:mm a");
	if (zoneId != null) {
		ZonedDateTime zonedDateTime = ZonedDateTime.now(zoneId);
		// Use the original locationForTime for the response to keep it user-friendly
		response = "The current time in " + capitalizeFirstLetterOfEachWord(locationForTime) + " is " + zonedDateTime.format(timeFormatter) + ".";
	} else {
		if (locationForTime != null) {
			// A location was mentioned but not mapped or found
			response = "I can tell you the current time, but I'm not sure about the specific time zone for " +
					           capitalizeFirstLetterOfEachWord(locationForTime) +
					           ". The current server time is " + LocalTime.now().format(timeFormatter) + ".";
		} else {
			if (userMessage.toLowerCase().contains("the time there")) {
				return "Please specify 'there' as a location, for example tell me a location after the question"; // Or "Where is 'there'?"
			}
			// No location mentioned, give server local time
			response = "The current server time is " + LocalTime.now().format(timeFormatter) + ".";
		}
	}
	return response;
}


// Modify generateResponse
private String generateResponse(String predictedIntent, String userMessage) {
	String response;
	String currentMessageLocation = null; // Initialize
	
	String lowerUserMessage = userMessage.toLowerCase();
	if (lowerUserMessage.contains("current president") ||
			    lowerUserMessage.contains("current population") ||
			    lowerUserMessage.contains("current leader")) {
		predictedIntent = "factual_query";
	} else if (lowerUserMessage.contains("how to") || // Broadened to 'contains' for more flexibility
			           lowerUserMessage.startsWith("what is the best way to") ||
			           lowerUserMessage.startsWith("show me how to") ||
			           lowerUserMessage.startsWith("explain how to") ||
			           lowerUserMessage.startsWith("tell me how to")) { // Explicitly added this common phrase
		predictedIntent = "factual_query";
	}
	// --- Step 1: Handle Follow-up Input if AI is Awaiting Information ---
	if (this.awaitingInformationType != null) {
		String providedInfo = userMessage.trim();
		// String intentToExecute = null; // intentToExecute was declared but not used
		
		if ("expression_for_arithmetic".equals(this.awaitingInformationType)) {
			try {
				double result = evaluateArithmeticExpression(providedInfo);
				// Use original user message part for response, as `providedInfo` might be heavily sanitized
				response = "The result of " + providedInfo + " is " + result + ".";
				this.awaitingInformationType = null; // Reset state
				this.originalQueryForFollowUp = null;
				this.lastIntent = null; // Set last intent to the fulfilled one
				return response;
			} catch (IllegalArgumentException | UnsupportedOperationException e) {
				System.err.println("Follow-up arithmetic evaluation error: " + e.getMessage());
				if (e.getMessage() != null && e.getMessage().startsWith("EMPTY_EXPRESSION")) {
					response = "That still seems to be empty. Please provide the arithmetic expression you'd like me to evaluate.";
					
				} else {
					response = "That still doesn't look like a valid arithmetic expression. Please provide an expression like '2 + 2, 2 * 2'.";
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
		case "gratitude":
			if (!this.gratitudeResponses.isEmpty()) {
				response = this.gratitudeResponses.get(this.randomGenerator.nextInt(this.gratitudeResponses.size()));
				if (this.userName != null) {
					response += " " + this.userName + "!"; // Append name if known
				}
			} else {
				response = "Thank you very much" + (this.userName != null ? ", " + this.userName : "") + "!";
			}
			break;
		case "affirmation":
			if (!this.affirmationResponses.isEmpty()) {
				response = this.affirmationResponses.get(this.randomGenerator.nextInt(this.affirmationResponses.size()));
				if (this.userName != null) {
					response += " " + this.userName + "!"; // Append name if known
				}
			} else {
				response = "That's great then" + (this.userName != null ? ", " + this.userName : "") + "!";
			}
			break;
		case "negation":
			if (!this.negationResponses.isEmpty()) {
				response = this.negationResponses.get(this.randomGenerator.nextInt(this.negationResponses.size()));
				if (this.userName != null) {
					response += " " + this.userName + "!"; // Append name if known
				}
			} else {
				response = "Alright" + (this.userName != null ? ", " + this.userName : "") + "!";
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
		case "joke_request":
			if (!this.jokeResponses.isEmpty()) {
				// Fetch a random joke if available
				response = this.jokeResponses.get(this.randomGenerator.nextInt(this.jokeResponses.size()));
				// Optionally, add a conversational touch if the user's name is known
				if (this.userName != null) {
					response += " How about that one, " + this.userName + "?";
				}
			} else {
				// Fallback if both greetingResponses and jokeResponses are empty
				response = "Hello!" + (this.userName != null ? " " + this.userName + "!" : " I'm a bit short on material today.");
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
			String cleanedExpression = extractArithmeticExpression(userMessage);
			
			if (cleanedExpression != null && !cleanedExpression.trim().isEmpty()) {
				try {
					double result = evaluateArithmeticExpression(cleanedExpression);
					// For display, use the original user message but remove common prefixes for cleaner output
					String responseDisplay = userMessage.toLowerCase();
					responseDisplay = responseDisplay.replaceAll("^(what is the |what is |calculate |compute |solve |evaluate |how much is |how many is |for )", "").trim();
					response = "The result of " + responseDisplay + " is " + result + ".";
				} catch (IllegalArgumentException e) {
					System.err.println("Arithmetic evaluation error: " + e.getMessage());
					if (e.getMessage() != null && e.getMessage().startsWith("EMPTY_EXPRESSION")) {
						response = "It seems the expression you provided was empty or didn't contain valid characters. What arithmetic expression would you like me to evaluate?";
						this.awaitingInformationType = "expression_for_arithmetic"; // Set state
						this.originalQueryForFollowUp = userMessage;
					} else {
						response = "I'm sorry, I couldn't evaluate that. Please make sure it's a valid arithmetic expression (e.g., '2 + 2' or 'square root of 16').";
						// Keep awaitingInformationType set if the error is syntax related and we want to re-prompt.
						this.awaitingInformationType = "expression_for_arithmetic";
						this.originalQueryForFollowUp = userMessage;
					}
				} catch (UnsupportedOperationException e) {
					System.err.println("Arithmetic evaluation failed: " + e.getMessage());
					response = "I'm currently unable to perform calculations due to an internal configuration issue. My apologies.";
				}
			} else { // The initial query didn't yield a usable expression
				response = "Certainly. What arithmetic expression would you like me to calculate?";
				this.awaitingInformationType = "expression_for_arithmetic"; // Set state
				this.originalQueryForFollowUp = userMessage;
			}
			break;
		case "time_query":
			response = handleTimeQuery(userMessage); // Call the new method
			break;
		case "factual_query":
			String extractedQuery = extractFactualQuery(userMessage);
			
			if (extractedQuery == null || extractedQuery.trim().isEmpty()) {
				response = "I can answer factual questions, but I didn't quite understand what fact you're looking for. Could you rephrase?";
			} else {
				// Prioritize specific factual queries first based on original user message phrasing
				if (lowerUserMessage.contains("define ") || lowerUserMessage.contains("meaning of ") || lowerUserMessage.contains("explain the word ")) {
					// Extract just the word to define, removing "the word", "define", "meaning of", "explain the word"
					String wordToDefine = extractedQuery.replace("the word", "").trim(); // Use extractedQuery here
					// Additional cleanup for definition queries specifically
					wordToDefine = wordToDefine.replaceAll("^(define|meaning of|what is the meaning of|explain the word)\\s+", "").trim();
					
					
					if (!wordToDefine.isEmpty()) {
						response = fetchDictionaryDefinition(wordToDefine);
					} else {
						response = "What word would you like me to define?";
					}
				} else if (lowerUserMessage.matches(".*\\b\\d+\\b.*") &&
						           (lowerUserMessage.contains("fact about") || lowerUserMessage.contains("tell me about the number"))) {
					// Attempt to extract the number
					Pattern numberPattern = Pattern.compile("(\\d+)");
					Matcher numberMatcher = numberPattern.matcher(lowerUserMessage);
					if (numberMatcher.find()) {
						String number = numberMatcher.group(1);
						response = fetchNumberFact(number, "trivia"); // Default to 'trivia' for now
					} else {
						response = "Could you please specify which number you'd like a fact about?";
					}
				} else {
					// Default to DuckDuckGo for general factual queries
					response = fetchDuckDuckGoAnswer(extractedQuery);
				}
			}
			break;
		case "followup_location":
			currentMessageLocation = extractLocation(userMessage);
			if (currentMessageLocation != null) {
				this.extractedLocation = currentMessageLocation;
				if (this.lastIntent.equals("weather_query") || "location_for_weather".equals(this.awaitingInformationType)) {
					response = getWeatherForLocation(this.extractedLocation);
					this.awaitingInformationType = null; // Reset state
					this.originalQueryForFollowUp = null;
					this.extractedLocation = null; // Clear after use
				} else {
					response = "You mentioned " + this.extractedLocation + ". What would you like to do regarding this location?";
				}
			} else {
				response = "I'm not sure which location you're referring to. Could you please specify a city?";
				if (this.lastIntent.equals("weather_query")) {
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
			}
			break;
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
	
	// This clearing logic might be redundant or could be refined based on how `extractedLocation`
	// is used across different intents and follow-ups.
	// if (this.awaitingInformationType == null && !"weather_query".equals(predictedIntent) && !"followup_location".equals(predictedIntent)) {
	// }
	
	return response;
}

/**
 * Evaluates an arithmetic expression using JavaScript's ScriptEngine.
 * Handles basic arithmetic operations and conversions for "square root" and "power of".
 *
 * @param expression The arithmetic expression as a string (e.g., "16", "square root 16", "5 + 3").
 * @return The double result of the evaluation.
 * @throws IllegalArgumentException If the expression is invalid or cannot be evaluated.
 * @throws UnsupportedOperationException If the JavaScript engine is not available.
 */
private double evaluateArithmeticExpression(String expression) {
	ScriptEngineManager manager = new ScriptEngineManager();
	ScriptEngine engine = manager.getEngineByName("JavaScript");
	
	if (engine == null) {
		System.err.println("Engine not found, cannot evaluate expression");
		throw new UnsupportedOperationException("JavaScript engine (e.g., Nashorn or Graal.js) not available to evaluate expression.");
	}
	
	String processedExpression = expression.toLowerCase();
	
	// Convert natural language mathematical phrases to JavaScript `Math` functions
	// Using word boundaries (\b) makes these replacements safer and more accurate.
	processedExpression = processedExpression.replaceAll("\\b(square root of|square root|sqrt of|sqrt)\\s+(\\d+(\\.\\d+)?)", "Math.sqrt($2)");
	processedExpression = processedExpression.replaceAll("(\\d+(\\.\\d+)?)\\s+(to the power of|power of|power)\\s+(\\d+(\\.\\d+)?)", "Math.pow($1,$4)");
	processedExpression = processedExpression.replaceAll("(\\d+(\\.\\d+)?)\\s+cubed", "Math.pow($1,3)");
	processedExpression = processedExpression.replaceAll("(\\d+(\\.\\d+)?)\\s+squared", "Math.pow($1,2)");
	
	// Replace common words for operators, ensuring spaces for clarity
	processedExpression = processedExpression.replaceAll("\\s+plus\\s+", " + ");
	processedExpression = processedExpression.replaceAll("\\s+minus\\s+", " - ");
	processedExpression = processedExpression.replaceAll("\\s+times\\s+", " * ");
	processedExpression = processedExpression.replaceAll("\\s+multiplied by\\s+", " * ");
	processedExpression = processedExpression.replaceAll("\\s+divided by\\s+", " / ");
	processedExpression = processedExpression.replaceAll("\\s+over\\s+", " / ");
	processedExpression = processedExpression.replaceAll("\\s+mod\\s+", " % ");
	
	// Handle the power operator if written with '^'
	processedExpression = processedExpression.replaceAll("\\^", "**");
	
	try {
		// *** REVISED SANITIZATION LOGIC ***
		// This new regex is simpler and more reliable. It removes any "word"
		// (containing letters and apostrophes) that is NOT one of our allowed keywords.
		// `(?i)` makes it case-insensitive. `E` is included for scientific notation (e.g., 1.2E3).
		String finalSanitizedExpression = processedExpression
				                                  .replaceAll("\\b(?i)(?!(?:Math|sqrt|pow|log|abs|E))([a-zA-Z']+)\\b", "") // Remove invalid words
				                                  .replaceAll("\\s+", " ") // Collapse multiple spaces into one
				                                  .trim();
		
		// Ensure correct casing for "Math" functions after sanitization
		finalSanitizedExpression = finalSanitizedExpression.replace("math", "Math");
		
		// Basic check for empty or invalid-looking expressions after processing
		if (finalSanitizedExpression.isEmpty() || finalSanitizedExpression.matches("^[+\\-*/()%\\s]+$")) {
			throw new IllegalArgumentException("EMPTY_EXPRESSION_AFTER_PROCESSING: " + expression);
		}
		if (finalSanitizedExpression.equalsIgnoreCase("Math")) {
			throw new IllegalArgumentException("INVALID_MATH_FUNCTION_CALL: " + expression);
		}
		
		Object result = engine.eval(finalSanitizedExpression);
		
		if (result instanceof Number) {
			return ((Number) result).doubleValue();
		} else {
			System.err.println("Expression did not evaluate to a numeric value: " + finalSanitizedExpression + " (Evaluated to: " + result + ")");
			throw new IllegalArgumentException("NON_NUMERIC_RESULT: " + expression);
		}
	} catch (ScriptException e) {
		System.err.println("Error evaluating arithmetic expression '" + expression + "': " + e.getMessage());
		throw new IllegalArgumentException("SYNTAX_ERROR: " + expression, e);
	}
}

private String extractNameFromMessage(String userMessage) {
	String extractedName = null;
	
	Pattern[] regexPatterns = {
			Pattern.compile("my full name is\\s+([a-zA-Z'-]+(?:\\s+[a-zA-Z'-]+){0,2})", Pattern.CASE_INSENSITIVE),
			Pattern.compile("my name is\\s+([a-zA-Z'-]+(?:\\s+[a-zA-Z'-]+){0,2})", Pattern.CASE_INSENSITIVE),
			Pattern.compile("please remember my name is\\s+([a-zA-Z'-]+(?:\\s+[a-zA-Z'-]+){0,2})", Pattern.CASE_INSENSITIVE),
			Pattern.compile("you can call me\\s+([a-zA-Z'-]+(?:\\s+[a-zA-Z'-]+){0,2})", Pattern.CASE_INSENSITIVE),
			Pattern.compile("just call me\\s+([a-zA-Z'-]+(?:\\s+[a-zA-Z'-]+){0,2})", Pattern.CASE_INSENSITIVE),
			Pattern.compile("call me\\s+([a-zA-Z'-]+(?:\\s+[a-zA-Z'-]+){0,2})", Pattern.CASE_INSENSITIVE),
			Pattern.compile("i go by\\s+([a-zA-Z'-]+(?:\\s+[a-zA-Z'-]+){0,2})", Pattern.CASE_INSENSITIVE),
			Pattern.compile("i'm known as\\s+([a-zA-Z'-]+(?:\\s+[a-zA-Z'-]+){0,2})", Pattern.CASE_INSENSITIVE),
			Pattern.compile("people call me\\s+([a-zA-Z'-]+(?:\\s+[a-zA-Z'-]+){0,2})", Pattern.CASE_INSENSITIVE),
			Pattern.compile("i am\\s+([a-zA-Z'-]+(?:\\s+[a-zA-Z'-]+){0,1})\\b", Pattern.CASE_INSENSITIVE),
			Pattern.compile("i'm\\s+([a-zA-Z'-]+(?:\\s+[a-zA-Z'-]+){0,1})\\b", Pattern.CASE_INSENSITIVE)
	};
	for (Pattern pattern : regexPatterns) {
		Matcher matcher = pattern.matcher(userMessage);
		if (matcher.find()) {
			String potentialName = matcher.group(1).trim();
			String[] parts = potentialName.split("\\s+(?:and|or|but|so|is|was|the|a|for|to|in|on|at|by|from|with|who|which|that|because|also|then)\\s+", 2);
			extractedName = parts[0].trim();
			if (extractedName.endsWith(".") || extractedName.endsWith("!") || extractedName.endsWith("?")) {
				extractedName = extractedName.substring(0, extractedName.length() - 1).trim();
			}
			if (extractedName != null && !extractedName.isEmpty()) {
				break;
			} else {
				extractedName = null;
			}
		}
		
	}
	if (extractedName == null || extractedName.isEmpty()) {
		String trimmedMessage = userMessage.trim();
		String lowerTrimmedMessage = trimmedMessage.toLowerCase();
		
		String[] leadingFluff = {"hi ", "hello ", "hey ", "it's "};
		for (String fluff : leadingFluff) { // Corrected: Use 'leadingFluff'
			if (lowerTrimmedMessage.startsWith(fluff)) {
				trimmedMessage = trimmedMessage.substring(fluff.length()).trim();
				lowerTrimmedMessage = trimmedMessage.toLowerCase();
				break;
			}
		}
		String[] words = trimmedMessage.split("\\s+");
		if (words.length > 0 && words.length <= 3) {
			boolean isIntroPhraseItself = false;
			for (Pattern p : regexPatterns) {
				String patternPrefix = p.pattern().substring(0, p.pattern().indexOf("([")).replaceAll("\\\\s\\+", " ").trim();
				if (lowerTrimmedMessage.equals(patternPrefix)) {
					isIntroPhraseItself = true;
					break;
				}
			}
			
			if (!isIntroPhraseItself && !trimmedMessage.isEmpty()) {
				extractedName = trimmedMessage;
				if (extractedName.endsWith(".") || extractedName.endsWith("!") || extractedName.endsWith("?")) {
					extractedName = extractedName.substring(0, extractedName.length() - 1).trim();
				}
			}
		}
	}
	if (extractedName != null && !extractedName.isEmpty()) {
		String[] nameTokens = extractedName.split("\\s+");
		StringBuilder capitalizedNameBuilder = new StringBuilder();
		for (String token : nameTokens) {
			if (!token.isEmpty()) {
				if (token.matches("^(X|IX|IV|V?I{0,3})$") || token.matches("^(L|XL|XC|C)?(X|IX|IV|V?I{0,3})$") || token.matches("^[A-Z]$")) {
					capitalizedNameBuilder.append(token.toUpperCase());
				} else {
					capitalizedNameBuilder.append(Character.toUpperCase(token.charAt(0)))
							.append(token.length() > 1 ? token.substring(1).toLowerCase() : "");
				}
				capitalizedNameBuilder.append(" ");
			}
		}
		extractedName = capitalizedNameBuilder.toString().trim();
		String lowerExtractedName = extractedName.toLowerCase();
		List<String> stopWords = Arrays.asList(
				"what", "how", "who", "when", "why", "is", "are", "am", "the", "a", "an",
				"yes", "no", "ok", "okay", "hi", "hello", "hey", "thanks", "thank", "you",
				"please", "sorry", "bye", "goodbye", "me", "my", "i", "it", "name",
				"calculate", "weather", "joke", "time", "feeling"
		);
		
		int maxNameWords = 3;
		if (extractedName.split("\\s+").length > maxNameWords) return null;
		if (extractedName.length() > 35) return null;
		if (extractedName.length() == 1 && !Character.isUpperCase(extractedName.charAt(0))) return null;
		if (lowerExtractedName.matches(".*[0-9!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*") && !lowerExtractedName.contains("-") && !lowerExtractedName.contains("'")) {
			return null;
		}
		if (stopWords.contains(lowerExtractedName) && extractedName.split("\\s+").length == 1) return null;
		
	} else {
		return null;
	}
	
	return extractedName;
}

/**
 * Extracts a mathematical expression from the user's message.
 * This method is now more aggressive in stripping non-mathematical phrasing,
 * leaving only the core expression to be evaluated.
 *
 * @param userMessage The original message from the user.
 * @return The potential arithmetic expression string, or null if none is clearly found.
 */
private String extractArithmeticExpression(String userMessage) {
	String lowerUserMessage = userMessage.toLowerCase();
	
	// Define a comprehensive list of prefixes and surrounding words to strip
	// Added "of " to handle phrases like "square root of X"
	String[] prefixesAndSuffixesToRemove = {
			"calculate ", "compute ", "solve ", "evaluate ", "what is the ", "what is ", "how much is ", "how many is ", "for ",
			"can you calculate ", "can you compute ", "can you solve ", "can you evaluate ",
			"please calculate ", "please compute ", "please solve ", "please evaluate ",
			"the value of ", "the result of ", "find the ", "tell me the ", "of " // Added "of "
	};
	
	String processedMessage = userMessage;
	
	// Strip prefixes/suffixes iteratively
	for (String phrase : prefixesAndSuffixesToRemove) {
		// Use word boundary \b to avoid partial matches within words
		// Also, consider the beginning of the string for the prefix
		Pattern pattern = Pattern.compile("^" + Pattern.quote(phrase) + "|\\b" + Pattern.quote(phrase) + "\\b", Pattern.CASE_INSENSITIVE);
		Matcher matcher = pattern.matcher(processedMessage);
		if (matcher.find()) {
			processedMessage = matcher.replaceAll("").trim();
		}
	}
	
	// Additional cleaning for phrases that might remain and interfere
	// These are more general words that are often part of a question but not the math itself.
	processedMessage = processedMessage.replaceAll("\\bthe\\b", "").trim(); // Remove standalone "the"
	
	// Handle specific mathematical phrases after stripping prefixes
	// These need to be handled carefully as they are now the "core" of the expression
	// They will be translated into Math. functions in evaluateArithmeticExpression
	Pattern squareRootPattern = Pattern.compile(".*(square root|sqrt).*\\d+(\\.\\d+)?.*", Pattern.CASE_INSENSITIVE);
	Pattern powerPattern = Pattern.compile(".*\\d+(\\.\\d+)?.*(to the power of|cubed|squared).*\\d*(\\.\\d+)?.*", Pattern.CASE_INSENSITIVE); // X squared/cubed, X to the power of Y
	
	// Check for common arithmetic operations (e.g., +, -, *, /)
	Pattern arithmeticPattern = Pattern.compile(".*\\d+.*[+\\-*/%].*\\d+.*");
	
	// If it's a simple number (e.g., "16" as a follow-up if context implies math)
	Pattern simpleNumber = Pattern.compile("^-?\\d+(\\.\\d+)?$");
	
	if (arithmeticPattern.matcher(processedMessage).matches() ||
			    squareRootPattern.matcher(processedMessage).matches() ||
			    powerPattern.matcher(processedMessage).matches() ||
			    simpleNumber.matcher(processedMessage).matches()) {
		return processedMessage;
	}
	
	return null; // No clear arithmetic expression found
}


/**
 * Extracts the core query for factual lookups.
 * It removes common question prefixes and generic phrases.
 *
 * @param userMessage The original message from the user.
 * @return The cleaned factual query, or null if it's too generic after cleaning.
 */
private String extractFactualQuery(String userMessage) {
	String lowerMessage = userMessage.toLowerCase();
	
	// Create a mutable list of prefixes to sort
	List<String> prefixesToRemove = new ArrayList<>(Arrays.asList(
			"what is ", "who is ", "where is ", "when is ", "how is ", "why is ",
			"tell me about ", "can you tell me about ", "do you know about ",
			"what's ", "who's ", "where's ", "when's ", "how's ", "why's ",
			"define ", "meaning of ", "what is the meaning of ", "fact about ", "tell me a fact about ",
			"tell me about the number ", "what is the ", // generic for "what is the capital of..."
			"can you explain ", "explain ", "give me information on ", "information about ",
			"query about ", "search for ", "look up ", "find out about ", "what do you know about ",
			"the word ", // Specific for dictionary lookups
			"tell me about the ", "who is the ", "what is the ", "what is a ", "who is a ",
			"how to ", "tell me how to ", "what is the best way to ", "show me how to ", "explain how to ",
			// --- NEW: Prefixes for processes and requirements ---
			"what are the steps to ", "steps to ", "how can i ", "what do i need to ", "requirements for ",
			"prerequisites for ", "how do i ", "what's needed to ", "what's required to ", "what equipment do i need to ",
			"what materials do i need to ", "what supplies do i need to ", "what are the ingredients for ", "ingredients for ",
			"materials for ", "supplies for ", "equipment for "
			// --- End new prefixes ---
	));
	
	// Sort prefixes by length, descending, to match the most specific one first.
	prefixesToRemove.sort(Comparator.comparingInt(String::length).reversed());
	
	String cleanedQuery = userMessage;
	for (String prefix : prefixesToRemove) {
		if (lowerMessage.startsWith(prefix)) {
			// Once the longest matching prefix is found, remove it and stop.
			cleanedQuery = userMessage.substring(prefix.length()).trim();
			break;
		}
	}
	
	// Remove trailing question marks and other punctuation
	cleanedQuery = cleanedQuery.replaceAll("[.!?]$", "").trim();
	
	// Remove very generic trailing words that might remain
	String[] genericSuffixes = {"please", "can you", "for me", "right now"};
	for (String suffix : genericSuffixes) {
		if (cleanedQuery.toLowerCase().endsWith(" " + suffix)) {
			cleanedQuery = cleanedQuery.substring(0, cleanedQuery.length() - suffix.length() - 1).trim();
		}
	}
	
	// *** REVISED LOGIC: Allow single-word queries ***
	// If after all cleaning, the query is empty, return null.
	// A single word (like a name or concept) is a valid query.
	if (cleanedQuery.isEmpty()) {
		return null;
	}
	
	return cleanedQuery;
}

// Helper method to capitalize the first letter of each word in a string
private String capitalizeFirstLetterOfEachWord(String input) {
	if (input == null || input.isEmpty()) {
		return input;
	}
	StringBuilder result = new StringBuilder();
	boolean capitalizeNext = true;
	for (char c : input.toCharArray()) {
		if (Character.isWhitespace(c) || c == '/') { // Also capitalize after a '/' for ZoneId names
			capitalizeNext = true;
			result.append(c);
		} else if (capitalizeNext) {
			result.append(Character.toUpperCase(c));
			capitalizeNext = false;
		} else {
			result.append(c);
		}
	}
	return result.toString();
}

}
