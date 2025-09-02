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
	String expressionString = (String) context.getEntity("calculator_query");
	
	if (expressionString == null || expressionString.isBlank()) {
		expressionString = userInput;
	}
	
	String numericExpression = NumberWordConverter.convertWordsToNumbers(expressionString);
	
	// --- UPDATED: More intelligent sanitization to handle natural language math ---
	String sanitizedExpression = numericExpression
			                             .toLowerCase()
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
		Expression expression = new ExpressionBuilder(sanitizedExpression).build();
		double result = expression.evaluate();
		
		return "The result is " + FORMATTER.format(result) + ".";
		
	} catch (Exception e) {
		// This error is more helpful to the user.
		return "I tried to calculate that, but the expression seems to be invalid. Please try again.";
	}
}
}