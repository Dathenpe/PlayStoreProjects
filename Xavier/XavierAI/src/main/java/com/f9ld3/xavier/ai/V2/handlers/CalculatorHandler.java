package com.f9ld3.xavier.ai.V2.handlers;

import com.f9ld3.xavier.ai.V2.ConversationContext;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CalculatorHandler implements IntentHandler {

// Regex to find two numbers and an operator.
// It looks for: (a number) (optional whitespace) (an operator word) (optional whitespace) (a number)
private static final Pattern CALCULATION_PATTERN =
		Pattern.compile(".*?(\\d+)\\s*(plus|\\+|minus|-|times|x|\\*|divided by|/)\\s*(\\d+).*");

@Override
public String handle(String userInput, ConversationContext context) {
	Matcher matcher = CALCULATION_PATTERN.matcher(userInput.toLowerCase());
	
	if (matcher.matches()) {
		try {
			double num1 = Double.parseDouble(matcher.group(1));
			String operator = matcher.group(2);
			double num2 = Double.parseDouble(matcher.group(3));
			
			switch (operator) {
				case "plus":
				case "+":
					return String.format("The answer is %.2f.", num1 + num2);
				case "minus":
				case "-":
					return String.format("The answer is %.2f.", num1 - num2);
				case "times":
				case "x":
				case "*":
					return String.format("The answer is %.2f.", num1 * num2);
				case "divided by":
				case "/":
					if (num2 == 0) {
						return "I can't divide by zero, that's not possible!";
					}
					return String.format("The answer is %.2f.", num1 / num2);
				default:
					// This case should ideally not be reached due to the regex
					return "I understood the numbers but not the operation.";
			}
		} catch (NumberFormatException e) {
			return "I found what looks like a calculation, but I couldn't understand the numbers.";
		}
	}
	
	return "I can perform calculations, but I didn't understand your request. Please ask like 'what is 5 plus 3?'.";
}
}