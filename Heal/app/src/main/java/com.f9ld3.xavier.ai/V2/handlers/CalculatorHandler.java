// C:/Users/Music_Minister/Desktop/PlayStore/PlayStoreProjects/Xavier/XavierAI/src/main/java/com/f9ld3/xavier/ai/V2/handlers/CalculatorHandler.java
package com.f9ld3.xavier.ai.V2.handlers;

import com.f9ld3.xavier.ai.V2.ConversationContext;
import com.f9ld3.xavier.ai.V2.utils.NumberWordConverter;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;

import java.text.DecimalFormat;

/**
 * Handles mathematical calculations by using the expression provided by the core pipeline's
 * PatternHandler, converting natural language math terms (e.g., "square root"),
 * and evaluating the result.
 */
public class CalculatorHandler implements IntentHandler {

private static final DecimalFormat FORMATTER = new DecimalFormat("0.##############");

@Override
public String handle(String userInput, ConversationContext context) {
	// UPDATED: Use the new context API to safely get the expression.
	// It now correctly uses getEntityFromCurrentContext("expression") and handles the Optional result.
	String expressionString = context.getEntityFromCurrentContext("expression")
			                          .map(String::valueOf)
			                          .orElse(userInput);
	
	String numericExpression = NumberWordConverter.convertWordsToNumbers(expressionString);
	
	// --- UPDATED: More intelligent sanitization to handle natural language math ---
	String sanitizedExpression = numericExpression
			                             .toLowerCase()
			                             // NEW: Remove conversational prefixes to allow the expression to be parsed correctly.
			                             .replaceAll("^what is |^what's |^calculate |^compute ", "")
			                             // NEW: Remove the bot's name from the expression to avoid confusing the parser.
			                             .replaceAll("\\bxavier\\b", "")
			                             // Convert "square root of" and "root of" to the sqrt() function
			                             .replaceAll("square root of|root of", "sqrt")
			                             // Convert "squared" to the power operator
			                             .replaceAll("squared", "^2")
			                             // Convert word operators to symbols
			                             .replaceAll("\\s*plus\\s*|\\s*and\\s*", "+")
			                             .replaceAll("\\s*minus\\s*", "-")
			                             .replaceAll("\\s*x\\s*", "*")
			                             .replaceAll("\\s*times\\s*", "*")
			                             .replaceAll("\\s*divided by\\s*", "/")
			                             .replaceAll("\\s*to the power of\\s*", "^")
			                             // Remove any remaining characters that are not part of a valid expression,
			                             // but keep letters to allow for function names like 'sqrt'.
			                             .replaceAll("[^a-z0-9\\s\\+\\-\\*\\/\\.\\(\\)\\^]", "");
	
	try {
		// The exp4j library is powerful enough to handle spaces correctly.
		Expression expression = new ExpressionBuilder(sanitizedExpression).build();
		double result = expression.evaluate();
		
		return "The result is " + FORMATTER.format(result) + ".";
		
	} catch (Exception e) {
		// This error is more helpful to the user.
		return "I tried to calculate that, but the expression seems to be invalid. Please try again.";
	}
}
}