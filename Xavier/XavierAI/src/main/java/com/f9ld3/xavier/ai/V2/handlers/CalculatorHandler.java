package com.f9ld3.xavier.ai.V2.handlers;

import com.f9ld3.xavier.ai.V2.ConversationContext;
import com.f9ld3.xavier.ai.V2.utils.NumberWordConverter;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handles mathematical calculations by extracting the expression from the user's
 * input, converting number words to digits, and using a robust math evaluation library.
 */
public class CalculatorHandler implements IntentHandler {

// A pattern to find the core mathematical expression in a sentence.
private static final Pattern MATH_PATTERN = Pattern.compile(
		"(?:what is|what's|calculate|compute)?\\s*(.*)",
		Pattern.CASE_INSENSITIVE
);

@Override
public String handle(String userInput, ConversationContext context) {
	// 1. Convert number words to digits first (e.g., "one hundred" -> "100")
	String numericInput = NumberWordConverter.convertWordsToNumbers(userInput);
	
	// 2. Extract the core expression
	Matcher matcher = MATH_PATTERN.matcher(numericInput);
	if (!matcher.matches()) {
		return "I couldn't understand the math problem. Please try phrasing it differently.";
	}
	
	String expressionStr = matcher.group(1)
			                       .replaceAll("(?i)plus", "+")
			                       .replaceAll("(?i)minus", "-")
			                       .replaceAll("(?i)times", "*")
			                       .replaceAll("(?i)divided by", "/")
			                       .replaceAll("(?i)to the power of", "^")
			                       .replaceAll("[^0-9+\\-*/.^()\\s]", ""); // Sanitize
	
	if (expressionStr.trim().isEmpty()) {
		return "It seems you asked me to calculate something, but I couldn't find the numbers.";
	}
	
	try {
		// 3. Evaluate the sanitized expression
		Expression expression = new ExpressionBuilder(expressionStr).build();
		double result = expression.evaluate();
		
		// Return integer format if the result is a whole number
		if (result == (long) result) {
			return String.format("The answer is %d.", (long) result);
		} else {
			return String.format("The answer is %s.", result);
		}
	} catch (Exception e) {
		return "I encountered an error trying to solve that: " + e.getMessage() + ". Please check the expression.";
	}
}
}