package com.f9ld3.xavier.ai.V2;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A utility class for basic text pre-processing.
 */
public class TextProcessor {

/**
 * Tokenizes a raw text sentence into a list of cleaned words.
 * <p>
 * This process involves:
 * 1. Converting the text to lowercase.
 * 2. Removing all punctuation.
 * 3. Splitting the text into individual words (tokens).
 * 4. Filtering out any empty strings that might result from the split.
 *
 * @param text The raw input sentence.
 * @return A List of cleaned, lowercase words.
 */
public static List<String> tokenize(String text) {
	if (text == null || text.trim().isEmpty()) {
		return Collections.emptyList();
	}
	
	// 1. Convert to lowercase
	String lowerCaseText = text.toLowerCase();
	
	// 2. Remove all punctuation using a regular expression
	// This regex replaces anything that is NOT a letter, a number, or whitespace.
	String noPunctuationText = lowerCaseText.replaceAll("[^a-z0-9\\s]", "");
	
	// 3. Split the text into words by whitespace
	String[] words = noPunctuationText.split("\\s+");
	
	// 4. Filter out any empty strings and collect into a List
	return Arrays.stream(words)
			       .filter(word -> !word.isEmpty())
			       .collect(Collectors.toList());
}

// A simple main method to test our tokenizer
public static void main(String[] args) {
	String sentence1 = "Hello! How are you today?";
	List<String> tokens1 = tokenize(sentence1);
	System.out.println("Original: " + sentence1);
	System.out.println("Tokens: " + tokens1); // Expected: [hello, how, are, you, today]
	
	System.out.println("---");
	
	String sentence2 = "Let's test this... with 123 numbers.";
	List<String> tokens2 = tokenize(sentence2);
	System.out.println("Original: " + sentence2);
	System.out.println("Tokens: " + tokens2); // Expected: [lets, test, this, with, 123, numbers]
}
}