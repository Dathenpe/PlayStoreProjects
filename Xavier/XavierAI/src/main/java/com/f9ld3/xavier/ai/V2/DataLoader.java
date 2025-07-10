package com.f9ld3.xavier.ai.V2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * A class responsible for loading and preparing training data from a file.
 */
public class DataLoader {

private final List<List<String>> documents;
private final List<String> labels;

public DataLoader() {
	this.documents = new ArrayList<>();
	this.labels = new ArrayList<>();
}

/**
 * Loads training data from a file located in the resources folder.
 * The file is expected to have one sample per line, in the format:
 * intent,sentence
 *
 * @param resourceFileName The name of the file in the resources folder (e.g., "training_data.txt").
 * @throws IOException If there is an error reading the file.
 */
public void loadDataFromResource(String resourceFileName) throws IOException {
	// Clear any existing data
	documents.clear();
	labels.clear();
	
	InputStream is = getClass().getClassLoader().getResourceAsStream(resourceFileName);
	if (is == null) {
		throw new IOException("Resource file not found: " + resourceFileName);
	}
	
	try (InputStreamReader streamReader = new InputStreamReader(is, StandardCharsets.UTF_8);
	     BufferedReader reader = new BufferedReader(streamReader)) {
		
		String line;
		while ((line = reader.readLine()) != null) {
			if (line.trim().isEmpty() || !line.contains(",")) {
				continue; // Skip empty or malformed lines
			}
			
			String[] parts = line.split(",", 2);
			if (parts.length == 2) {
				String label = parts[0].trim();
				String sentence = parts[1].trim();
				
				labels.add(label);
				documents.add(TextProcessor.tokenize(sentence));
			}
		}
	}
}

public List<List<String>> getDocuments() {
	return documents;
}

public List<String> getLabels() {
	return labels;
}

// A simple main method to test the data loader
public static void main(String[] args) {
	DataLoader dataLoader = new DataLoader();
	try {
		// NOTE: For this main method to work, you must create a file named
		// 'training_data.txt' in your 'src/main/resources' folder.
		dataLoader.loadDataFromResource("training_data.txt");
		
		System.out.println("Data loaded successfully!");
		System.out.println("Number of documents: " + dataLoader.getDocuments().size());
		System.out.println("Number of labels: " + dataLoader.getLabels().size());
		
		System.out.println("\n--- Sample Data ---");
		// Print the first 3 samples to verify
		for (int i = 0; i < Math.min(3, dataLoader.getDocuments().size()); i++) {
			System.out.println("Label: " + dataLoader.getLabels().get(i) + ", Tokens: " + dataLoader.getDocuments().get(i));
		}
		
	} catch (IOException e) {
		System.err.println("Error loading data: " + e.getMessage());
		e.printStackTrace();
	}
}
}