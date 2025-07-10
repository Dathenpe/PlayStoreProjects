package com.f9ld3.xavier.ai.V2;

import java.util.*;
import java.util.stream.Collectors;

public class NaiveBayesClassifier {

// More descriptive names for clarity
private final Map<String, Integer> classDocumentCounts;
private final Map<String, Map<String, Integer>> wordCountsByClass;
private final Set<String> vocabulary;

// Caches the total number of words for each class for efficiency
private final Map<String, Integer> classTotalWordCounts;
private List<String> classes;

public NaiveBayesClassifier() {
	this.classDocumentCounts = new HashMap<>();
	this.wordCountsByClass = new HashMap<>();
	this.vocabulary = new HashSet<>();
	this.classTotalWordCounts = new HashMap<>();
}

/**
 * Trains the classifier on a set of documents and their corresponding labels.
 *
 * @param documents A list of documents, where each document is a list of words.
 * @param labels    The list of corresponding labels for each document.
 */
public void fit(List<List<String>> documents, List<String> labels) {
	if (documents.size() != labels.size()) {
		throw new IllegalArgumentException("Documents and labels must have the same size.");
	}
	
	// Use a stream to get unique classes, which is a bit cleaner
	classes = new ArrayList<>(new HashSet<>(labels));
	for (String c : classes) {
		classDocumentCounts.put(c, 0);
		wordCountsByClass.put(c, new HashMap<>());
		classTotalWordCounts.put(c, 0);
	}
	
	// Populate counts
	for (int i = 0; i < documents.size(); i++) {
		String label = labels.get(i);
		List<String> doc = documents.get(i);
		
		classDocumentCounts.put(label, classDocumentCounts.get(label) + 1);
		for (String word : doc) {
			vocabulary.add(word);
			Map<String, Integer> currentWordCounts = wordCountsByClass.get(label);
			currentWordCounts.put(word, currentWordCounts.getOrDefault(word, 0) + 1);
		}
	}
	
	// Pre-calculate and cache the total word counts for each class
	wordCountsByClass.forEach((className, wordCounts) -> {
		int totalWords = wordCounts.values().stream().mapToInt(Integer::intValue).sum();
		classTotalWordCounts.put(className, totalWords);
	});
}

/**
 * Predicts the most likely class for a given document and its confidence.
 *
 * @param doc The document to classify, as a list of words.
 * @return A PredictionResult object containing the best label and its confidence score (0.0 to 1.0).
 */
public PredictionResult predict(List<String> doc) {
	Map<String, Double> logProbabilities = new HashMap<>();
	int totalDocuments = classes.size();
	int vocabularySize = vocabulary.size();
	
	for (String c : classes) {
		// Log prior probability
		double logPrior = Math.log((double) classDocumentCounts.get(c) / totalDocuments);
		logProbabilities.put(c, logPrior);
		
		int totalWordsInClass = classTotalWordCounts.get(c);
		
		// Log likelihood probability
		for (String word : doc) {
			int wordCount = wordCountsByClass.get(c).getOrDefault(word, 0);
			// Using Laplace smoothing
			double wordProb = (double) (wordCount + 1) / (totalWordsInClass + vocabularySize);
			logProbabilities.put(c, logProbabilities.get(c) + Math.log(wordProb));
		}
	}
	
	return calculateConfidence(logProbabilities);
}

/**
 * Converts log probabilities into a normalized confidence score for the best class.
 * This uses the "log-sum-exp" trick for numerical stability.
 *
 * @param logProbs A map of class names to their calculated log probabilities.
 * @return A PredictionResult with the best class and its normalized confidence.
 */
private PredictionResult calculateConfidence(Map<String, Double> logProbs) {
	if (logProbs.isEmpty()) {
		return new PredictionResult(null, 0.0);
	}
	
	// Find the maximum log probability for numerical stability
	double maxLogProb = Collections.max(logProbs.values());
	
	Map<String, Double> probs = new HashMap<>();
	double sumOfProbs = 0.0;
	
	for (Map.Entry<String, Double> entry : logProbs.entrySet()) {
		// This is the core of the softmax function, converting logs to probabilities
		double probability = Math.exp(entry.getValue() - maxLogProb);
		probs.put(entry.getKey(), probability);
		sumOfProbs += probability;
	}
	
	// Find the best class and normalize its probability to get a final confidence score
	String bestClass = null;
	double bestProb = -1.0;
	
	for (Map.Entry<String, Double> entry : probs.entrySet()) {
		if (entry.getValue() > bestProb) {
			bestClass = entry.getKey();
			bestProb = entry.getValue();
		}
	}
	
	double confidence = (sumOfProbs > 0) ? bestProb / sumOfProbs : 0.0;
	return new PredictionResult(bestClass, confidence);
}

/**
 * Updated main method to test the classifier with confidence scoring.
 */
public static void main(String[] args) {
	List<List<String>> X_train = new ArrayList<>();
	X_train.add(Arrays.asList("this", "is", "a", "good", "document"));
	X_train.add(Arrays.asList("this", "is", "a", "bad", "document"));
	X_train.add(Arrays.asList("good", "one"));
	X_train.add(Arrays.asList("bad", "one"));
	
	List<String> y_train = Arrays.asList("positive", "negative", "positive", "negative");
	
	NaiveBayesClassifier classifier = new NaiveBayesClassifier();
	classifier.fit(X_train, y_train);
	
	List<String> X_test = Arrays.asList("this", "is", "a", "good", "one");
	PredictionResult result = classifier.predict(X_test);
	
	System.out.println("--- Test Prediction ---");
	System.out.println("Input: " + X_test);
	System.out.printf("Predicted class: '%s'%n", result.getPredictedLabel());
	System.out.printf("Confidence: %.2f%%%n", result.getConfidence() * 100);
}
}