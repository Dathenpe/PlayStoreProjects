package com.f9ld3.xavier.ai; // IMPORTANT: Ensure this matches your package name

// Weka imports (used only in trainAndSaveModel)
import weka.classifiers.Classifier;
import weka.classifiers.evaluation.Evaluation;
import weka.classifiers.trees.J48;
import weka.core.Attribute;
import weka.core.Instances;
import weka.core.SerializationHelper;
import weka.core.converters.ArffLoader;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.StringToWordVector;

// Swing and AWT imports for the GUI
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

/**
 * Main class for the Xavier AI graphical chat application.
 *
 * This class is responsible for:
 * 1.  Creating and displaying a JFrame-based chat interface.
 * 2.  Triggering the AI model training/loading process on a background thread.
 * 3.  Initializing the XavierCoreAI after the model is ready.
 * 4.  Handling user input from the GUI, processing it with XavierCoreAI,
 * and displaying the conversation in the chat window.
 */
public class Main extends JFrame {

// --- Configuration Constants ---
private static final String ARFF_FILE_PATH = "xavier_data.arff";
private static final String MODEL_FILE_PATH = "xavier_intent_classifier.model";
private static final String FILTER_FILE_PATH = "xavier_wordvector_filter.filter";
private static final ArrayList<String> ALL_POSSIBLE_INTENTS = new ArrayList<>(Arrays.asList(
		"greeting", "goodbye", "unknown", "weather_query", "joke_request", "followup_location",
		"gratitude", "affirmation", "negation", "personal_question", "time_query", "feeling_status",
		"arithmetic_query", "set_user_name", "factual_query"));

// --- GUI Components ---
private final JTextArea chatArea;
private final JTextField userInputField;
private final JButton sendButton;

// --- AI Core ---
private XavierCoreAI xavierAI;
private boolean isAIReady = false;

/**
 * Constructor to set up the GUI.
 */
public Main() {
	// --- Frame Setup ---
	super("Xavier AI Chat");
	setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	setSize(500, 700);
	setLocationRelativeTo(null); // Center the window
	setLayout(new BorderLayout());
	
	// --- Chat Area ---
	chatArea = new JTextArea();
	chatArea.setEditable(false);
	chatArea.setLineWrap(true);
	chatArea.setWrapStyleWord(true);
	chatArea.setFont(new Font("SansSerif", Font.PLAIN, 14));
	chatArea.setMargin(new Insets(10, 10, 10, 10));
	JScrollPane scrollPane = new JScrollPane(chatArea);
	add(scrollPane, BorderLayout.CENTER);
	
	// --- Input Panel ---
	JPanel inputPanel = new JPanel(new BorderLayout(5, 5));
	inputPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
	
	userInputField = new JTextField();
	userInputField.setFont(new Font("SansSerif", Font.PLAIN, 14));
	userInputField.setEnabled(false); // Disabled until AI is ready
	inputPanel.add(userInputField, BorderLayout.CENTER);
	
	sendButton = new JButton("Send");
	sendButton.setFont(new Font("SansSerif", Font.BOLD, 12));
	sendButton.setEnabled(false); // Disabled until AI is ready
	inputPanel.add(sendButton, BorderLayout.EAST);
	
	add(inputPanel, BorderLayout.SOUTH);
	
	// --- Action Listener for Sending Messages ---
	ActionListener sendMessageAction = e -> {
		try {
			processUserInput();
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	};
	sendButton.addActionListener(sendMessageAction);
	userInputField.addActionListener(sendMessageAction); // Allow pressing Enter
}

/**
 * The main entry point of the application.
 */
public static void main(String[] args) {
	SwingUtilities.invokeLater(() -> {
		Main chatInterface = new Main();
		chatInterface.setVisible(true);
		chatInterface.initializeAI(); // Start the AI initialization
	});
}

/**
 * Initializes the AI by checking for existing models or training new ones.
 * This is done on a background thread to keep the GUI responsive.
 */
private void initializeAI() {
	// Show a loading dialog
	JDialog loadingDialog = createLoadingDialog();
	
	// Use SwingWorker to perform training/loading in the background
	SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
		@Override
		protected Boolean doInBackground() throws Exception {
			// This method retains the logic for first-time training and subsequent retaining
			trainAndSaveModel();
			xavierAI = new XavierCoreAI(MODEL_FILE_PATH, FILTER_FILE_PATH);
			return true;
		}
		
		@Override
		protected void done() {
			loadingDialog.dispose(); // Close the loading dialog
			try {
				get(); // Check for exceptions during doInBackground
				isAIReady = true;
				userInputField.setEnabled(true);
				sendButton.setEnabled(true);
				appendToChat("Xavier ", "Hello! How can I help you today?");
				userInputField.requestFocus();
			} catch (Exception e) {
				e.printStackTrace();
				JOptionPane.showMessageDialog(
						Main.this,
						"Failed to initialize AI: " + e.getMessage(),
						"Fatal Error",
						JOptionPane.ERROR_MESSAGE);
				System.exit(1);
			}
		}
	};
	
	worker.execute();
	loadingDialog.setVisible(true); // Show the dialog while the worker runs
}

/**
 * Creates a simple, non-closable dialog to show while the AI is loading.
 */
private JDialog createLoadingDialog() {
	JDialog dialog = new JDialog(this, "Initializing AI", true); // Modal
	JProgressBar progressBar = new JProgressBar();
	progressBar.setIndeterminate(true);
	JLabel label = new JLabel("Please wait while the AI model is being prepared...");
	label.setBorder(new EmptyBorder(10, 10, 0, 10));
	
	JPanel panel = new JPanel(new BorderLayout(10,10));
	panel.setBorder(new EmptyBorder(20, 20, 20, 20));
	panel.add(label, BorderLayout.NORTH);
	panel.add(progressBar, BorderLayout.CENTER);
	
	dialog.add(panel);
	dialog.pack();
	dialog.setLocationRelativeTo(this);
	dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
	return dialog;
}


/**
 * Processes the user's input, gets a response from the AI, and updates the chat.
 */
private void processUserInput() throws Exception {
	if (!isAIReady) return;
	
	String userMessage = userInputField.getText().trim();
	if (userMessage.isEmpty()) {
		return;
	}
	
	appendToChat("You", userMessage);
	userInputField.setText("");
	
	if (userMessage.equalsIgnoreCase("exit")) {
		appendToChat("Xavier ", "Goodbye! It was nice chatting with you.");
		userInputField.setEnabled(false);
		sendButton.setEnabled(false);
		// Optionally close after a delay
		new Timer(2000, e -> System.exit(0)).start();
	} else {
		// Get AI response and update GUI
		String aiResponse = xavierAI.processMessage(userMessage);
		appendToChat("Xavier ", aiResponse);
	}
}

/**
 * Helper method to append messages to the chat area with formatting.
 */
private void appendToChat(String sender, String message) {
	chatArea.append(String.format("%s: %s\n\n", sender, message));
	// Auto-scroll to the bottom
	chatArea.setCaretPosition(chatArea.getDocument().getLength());
}

/**
 * Utility method to train the Weka model and filter.
 * This method is called to ensure the AI's "brain" is ready.
 * It will only perform training if the model/filter files do not exist.
 * @throws Exception If any Weka operation fails.
 */
private static void trainAndSaveModel() throws Exception {
	File modelFile = new File(MODEL_FILE_PATH);
	File filterFile = new File(FILTER_FILE_PATH);
	
	if (modelFile.exists() && filterFile.exists()) {
		System.out.println("AI Model and Filter already exist. Skipping training.");
		return;
	}
	
	System.out.println("--- Training AI Model and Filter (first-time setup) ---");
	System.out.println("Loading data from " + ARFF_FILE_PATH);
	
	// --- Data Loading ---
	ArffLoader loader = new ArffLoader();
	loader.setSource(new File(ARFF_FILE_PATH));
	Instances data = loader.getDataSet();
	if (data.classIndex() == -1) {
		data.setClass(data.attribute("intent"));
	}
	
	System.out.println("Data Loaded. Instances: " + data.numInstances());
	
	// --- Data Preprocessing (Filter) ---
	System.out.println("Applying StringToWordVector Filter...");
	StringToWordVector filter = new StringToWordVector();
	filter.setAttributeIndices(String.valueOf(data.attribute("text").index() + 1));
	filter.setWordsToKeep(1000);
	filter.setTFTransform(true);
	filter.setIDFTransform(true);
	filter.setLowerCaseTokens(true);
	filter.setInputFormat(data);
	Instances vectorizedData = Filter.useFilter(data, filter);
	
	System.out.println("Text vectorized. New attributes: " + vectorizedData.numAttributes());
	
	// --- Model Training ---
	System.out.println("Training J48 Classifier...");
	Classifier classifier = new J48();
	classifier.buildClassifier(vectorizedData);
	System.out.println("Classifier trained.");
	
	// --- Model Evaluation (Optional) ---
	System.out.println("Evaluating Classifier with 10-Fold Cross-Validation...");
	Evaluation eval = new Evaluation(vectorizedData);
	eval.crossValidateModel(classifier, vectorizedData, 10, new Random(1));
	System.out.println(eval.toSummaryString("\n--- Evaluation Summary ---\n", false));
	System.out.println(eval.toMatrixString("--- Confusion Matrix ---"));
	
	// --- Save Model and Filter ---
	System.out.println("Saving model to " + MODEL_FILE_PATH + " and filter to " + FILTER_FILE_PATH);
	SerializationHelper.write(MODEL_FILE_PATH, classifier);
	SerializationHelper.write(FILTER_FILE_PATH, filter);
	System.out.println("Model and Filter saved successfully.");
}
}