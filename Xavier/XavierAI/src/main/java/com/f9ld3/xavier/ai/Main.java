package com.f9ld3.xavier.ai; // IMPORTANT: Ensure this matches your package name

// Weka imports for training and evaluation (used only in trainAndSaveModel)
import weka.classifiers.Classifier;
import weka.classifiers.evaluation.Evaluation;
import weka.classifiers.trees.J48;
import weka.core.Attribute;
import weka.core.Instances;
import weka.core.SerializationHelper;
import weka.core.converters.ArffLoader;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.StringToWordVector;

// Standard Java imports
import java.io.BufferedReader;
import java.io.File; // NEW: For checking file existence
import java.io.FileReader;
import java.util.ArrayList; // For intentPossibleValues in training
import java.util.Arrays; // For Array utilities
import java.util.Random; // For Evaluation random seed
import java.util.Scanner; // For user input

/**
 * Main class for the Xavier AI console application.
 * This class serves as the entry point and demonstrates how to
 * initialize and interact with the XavierCoreAI module.
 *
 * It is responsible for:
 * 1. Defining file paths for ARFF data, trained model, and filter.
 * 2. Providing a utility to train and save the AI model and filter (if not already existing).
 * 3. Creating an instance of XavierCoreAI (which loads the saved components).
 * 4. Handling user input via a console loop.
 * 5. Sending user messages to XavierCoreAI for processing.
 * 6. Printing Xavier's responses.
 */
public class Main {

// --- Configuration Constants ---
private static final String ARFF_FILE_PATH = "xavier_data.arff";
private static final String MODEL_FILE_PATH = "xavier_intent_classifier.model";
private static final String FILTER_FILE_PATH = "xavier_wordvector_filter.filter";

// IMPORTANT: This list MUST match the intents defined in your xavier_data.arff file.
// It's duplicated here for the *training* method to correctly define the attribute.
// In XavierCoreAI, it's used internally.
private static final ArrayList<String> ALL_POSSIBLE_INTENTS = new ArrayList<>(Arrays.asList(
		"greeting", "goodbye", "unknown", "weather_query", "joke_request", "followup_location",
		"gratitude", "affirmation", "negation", "personal_question", "time_query", "feeling_status"
));


public static void main(String[] args) {
	System.out.println("------Starting Xavier AI Console Application------");
	System.out.println("Type 'exit' to end the conversation.");
	
	// --- Step 1: Ensure AI model and filter are trained and saved ---
	// This method will only train/save if the files don't exist, or if explicitly needed.
	try {
		trainAndSaveModel();
	} catch (Exception e) {
		System.err.println("Failed to train and save AI model: " + e.getMessage());
		e.printStackTrace();
		System.exit(1); // Exit if training fails
	}
	
	XavierCoreAI xavierAI = null;
	
	try {
		// --- Step 2: Initialize XavierCoreAI with paths to the saved model and filter ---
		xavierAI = new XavierCoreAI(MODEL_FILE_PATH, FILTER_FILE_PATH);
		
		// --- Step 3: Start the Interactive Console Loop ---
		Scanner scanner = new Scanner(System.in);
		System.out.print("\nAI Friend (Xavier): Hello! How can I help you today? ");
		
		while (true) {
			System.out.print("\nYou: ");
			String userMessage = scanner.nextLine();
			
			if (userMessage.equalsIgnoreCase("exit")) {
				System.out.println("AI Friend (Xavier): Goodbye! It was nice chatting with you.");
				break; // Exit the loop
			}
			
			// --- Step 4: Process the user's message using XavierCoreAI ---
			String aiResponse = xavierAI.processMessage(userMessage);
			
			System.out.println("AI Friend (Xavier): " + aiResponse);
		}
		scanner.close(); // Close the scanner when done.
		
	} catch (Exception e) {
		System.err.println("An unrecoverable error occurred in Xavier AI runtime: " + e.getMessage());
		e.printStackTrace();
		System.exit(1); // Exit with an error code
	}
}

/**
 * Utility method to train the Weka model and filter and save them to files.
 * This method is called from main to ensure the AI's "brain" is ready.
 * It will only perform training if the model and filter files do not exist,
 * or if you explicitly delete them to force a re-train.
 * @throws Exception If any Weka operation fails during training.
 */
private static void trainAndSaveModel() throws Exception {
	File modelFile = new File(MODEL_FILE_PATH);
	File filterFile = new File(FILTER_FILE_PATH);
	
	// Only train and save if files don't exist (or force re-train by deleting them)
	if (modelFile.exists() && filterFile.exists()) {
		System.out.println("\n--- AI Model and Filter already exist. Skipping training. ---");
		return;
	}
	
	System.out.println("\n--- Training AI Model and Filter (first-time setup or retraining) ---");
	System.out.println("Loading data from " + ARFF_FILE_PATH + "...");
	
	// --- Data Loading ---
	ArffLoader loader = new ArffLoader();
	loader.setSource(new java.io.File(ARFF_FILE_PATH));
	Instances data = loader.getDataSet();
	// Set the class attribute to 'intent'
	if (data.classIndex() == -1) {
		Attribute intentAttribute = data.attribute("intent");
		if (intentAttribute == null) {
			throw new RuntimeException("Class attribute 'intent' not found in ARFF data.");
		}
		data.setClassIndex(intentAttribute.index());
	}
	
	System.out.println("Data Loaded. Instances: " + data.numInstances() + ", Attributes: " + data.numAttributes());
	
	// --- Data Preprocessing ---
	System.out.println("Applying StringToWordVector Filter...");
	StringToWordVector stringToWordVectorFilter = new StringToWordVector();
	stringToWordVectorFilter.setAttributeIndices(String.valueOf(data.attribute("text").index() + 1));
	stringToWordVectorFilter.setWordsToKeep(1000);
	stringToWordVectorFilter.setTFTransform(true);
	stringToWordVectorFilter.setIDFTransform(true);
	stringToWordVectorFilter.setLowerCaseTokens(true);
	
	stringToWordVectorFilter.setInputFormat(data);
	Instances vectorizedData = Filter.useFilter(data, stringToWordVectorFilter);
	
	vectorizedData.setClassIndex(vectorizedData.attribute("intent").index());
	
	System.out.println("Text vectorized. New attributes: " + vectorizedData.numAttributes());
	if (!vectorizedData.classAttribute().isNominal()) {
		throw new RuntimeException("Class attribute '" + vectorizedData.classAttribute().name() + "' is not nominal after filtering!");
	}
	
	// --- Model Training ---
	System.out.println("Training J48 Classifier...");
	Classifier classifier = new J48();
	classifier.buildClassifier(vectorizedData);
	System.out.println("Classifier trained.");
	
	// --- Model Evaluation (Optional, but good for feedback during development) ---
	System.out.println("Evaluating Classifier with 10-Fold Cross-Validation...");
	Evaluation eval = new Evaluation(vectorizedData);
	Random rand = new Random(1);
	int numFolds = 10;
	if (vectorizedData.numInstances() < numFolds) {
		numFolds = vectorizedData.numInstances();
		System.out.println("Warning: Dataset size is less than numFolds. Setting numFolds to " + numFolds);
	}
	if (numFolds > 1) {
		eval.crossValidateModel(classifier, vectorizedData, numFolds, rand);
		System.out.println("\n--- Model Evaluation Summary ---");
		System.out.println(eval.toSummaryString("\nResults\n", false));
		System.out.println(eval.toMatrixString("Confusion Matrix"));
		// System.out.println(eval.toClassDetailsString("Class Details (Precision, Recall, F-Measure)")); // Commented out for brevity in console
	} else {
		System.out.println("Model Evaluation Skipped (Dataset too small for " + numFolds + "-fold CV)");
	}
	
	// --- Save Trained Model and Filter ---
	System.out.println("Saving trained model to " + MODEL_FILE_PATH + " and filter to " + FILTER_FILE_PATH + "...");
	SerializationHelper.write(MODEL_FILE_PATH, classifier);
	SerializationHelper.write(FILTER_FILE_PATH, stringToWordVectorFilter);
	System.out.println("Model and Filter saved successfully.");
}
}
