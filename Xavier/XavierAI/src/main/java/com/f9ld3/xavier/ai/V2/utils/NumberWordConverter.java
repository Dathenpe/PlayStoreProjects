package com.f9ld3.xavier.ai.V2.utils;

import java.util.HashMap;
import java.util.Map;

/**
 * A utility to convert number words (e.g., "one hundred and fifty five")
 * into their digit representation ("155"). This allows the calculator
 * to handle more natural language inputs.
 */
public class NumberWordConverter {

private static final Map<String, Long> numberMap = new HashMap<>();

static {
	numberMap.put("zero", 0L);
	numberMap.put("one", 1L);
	numberMap.put("two", 2L);
	numberMap.put("three", 3L);
	numberMap.put("four", 4L);
	numberMap.put("five", 5L);
	numberMap.put("six", 6L);
	numberMap.put("seven", 7L);
	numberMap.put("eight", 8L);
	numberMap.put("nine", 9L);
	numberMap.put("ten", 10L);
	numberMap.put("eleven", 11L);
	numberMap.put("twelve", 12L);
	numberMap.put("thirteen", 13L);
	numberMap.put("fourteen", 14L);
	numberMap.put("fifteen", 15L);
	numberMap.put("sixteen", 16L);
	numberMap.put("seventeen", 17L);
	numberMap.put("eighteen", 18L);
	numberMap.put("nineteen", 19L);
	numberMap.put("twenty", 20L);
	numberMap.put("thirty", 30L);
	numberMap.put("forty", 40L);
	numberMap.put("fifty", 50L);
	numberMap.put("sixty", 60L);
	numberMap.put("seventy", 70L);
	numberMap.put("eighty", 80L);
	numberMap.put("ninety", 90L);
	numberMap.put("hundred", 100L);
	numberMap.put("thousand", 1000L);
	numberMap.put("million", 1000000L);
	numberMap.put("billion", 1000000000L);
}

public static String convertWordsToNumbers(String text) {
	String[] words = text.toLowerCase().split("[\\s-]+");
	StringBuilder resultBuilder = new StringBuilder();
	long currentNumber = 0;
	long finalNumber = 0;
	
	for (String word : words) {
		if (numberMap.containsKey(word)) {
			long value = numberMap.get(word);
			if (value == 100) {
				currentNumber *= value;
			} else if (value >= 1000) {
				currentNumber *= value;
				finalNumber += currentNumber;
				currentNumber = 0;
			} else {
				currentNumber += value;
			}
		} else {
			if (currentNumber != 0 || finalNumber != 0) {
				finalNumber += currentNumber;
				resultBuilder.append(finalNumber).append(" ");
				currentNumber = 0;
				finalNumber = 0;
			}
			resultBuilder.append(word).append(" ");
		}
	}
	
	if (currentNumber != 0 || finalNumber != 0) {
		finalNumber += currentNumber;
		resultBuilder.append(finalNumber);
	}
	
	return resultBuilder.toString().trim();
}
}