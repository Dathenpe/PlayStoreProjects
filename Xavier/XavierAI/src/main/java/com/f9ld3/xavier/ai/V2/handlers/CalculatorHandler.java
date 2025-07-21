package com.f9ld3.xavier.ai.V2.handlers;

import com.f9ld3.xavier.ai.V2.ConversationContext;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.regex.Pattern;

public class CalculatorHandler implements IntentHandler {

private final ScriptEngine engine;

public CalculatorHandler() {
	ScriptEngineManager manager = new ScriptEngineManager();
	this.engine = manager.getEngineByName("JavaScript");
}

@Override
public String handle(String userInput, ConversationContext context) {
	if (engine == null) {
		return "I'm sorry, my calculation module is currently unavailable.";
	}
	
	try {
		String expression = preprocessExpression(userInput);
		Object result = engine.eval(expression);
		return String.format("The answer is %.2f.", Double.parseDouble(result.toString()));
	} catch (ScriptException | IllegalArgumentException e) {
		System.err.println("Calculation Error: " + e.getMessage());
		return "I can perform calculations, but I didn't understand that expression. Please ask like 'what is 5 plus 3?' or 'square root of 16'.";
	}
}

/**
 * Converts natural language math phrases into a JavaScript-evaluable expression.
 */
private String preprocessExpression(String input) {
	String processed = input.toLowerCase()
			                   // Replace words with operators
			                   .replaceAll("\\s+plus\\s+", "+")
			                   .replaceAll("\\s+minus\\s+", "-")
			                   .replaceAll("\\s+times\\s+", "*")
			                   .replaceAll("\\s+x\\s+", "*")
			                   .replaceAll("\\s+divided by\\s+", "/")
			                   // Handle "square root of X"
			                   .replaceAll("square root of\\s*(\\d+\\.?\\d*)", "Math.sqrt($1)")
			                   // Handle "X squared" and "X cubed"
			                   .replaceAll("(\\d+\\.?\\d*)\\s+squared", "Math.pow($1, 2)")
			                   .replaceAll("(\\d+\\.?\\d*)\\s+cubed", "Math.pow($1, 3)")
			                   // Handle "X to the power of Y"
			                   .replaceAll("(\\d+\\.?\\d*)\\s+to the power of\\s+(\\d+\\.?\\d*)", "Math.pow($1, $2)");
	
	// --- SAFER CLEANUP LOGIC ---
	// After replacing known phrases, remove ALL other letters.
	// This prevents words like "timew" from causing a syntax error.
	processed = processed.replaceAll("[a-zA-Z]", "").trim();
	
	if (processed.trim().isEmpty()) {
		throw new IllegalArgumentException("Expression is empty after preprocessing.");
	}
	
	return processed;
}
}