package com.f9ld3.xavier.ai.V2.handlers;

import com.f9ld3.xavier.ai.V2.ConversationContext;
import com.f9ld3.xavier.ai.V2.services.JokeService;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handles requests for jokes using the specialized JokeService.
 * Can handle requests for single or multiple jokes.
 */
public class JokeHandler implements IntentHandler {

private final JokeService jokeService;
// Pattern to find a number in a joke request, e.g., "tell me 5 jokes"
private static final Pattern JOKE_COUNT_PATTERN = Pattern.compile("\\b(\\d+)\\b");

public JokeHandler(JokeService jokeService) {
	this.jokeService = jokeService;
}

@Override
public String handle(String userInput, ConversationContext context) {
	Matcher matcher = JOKE_COUNT_PATTERN.matcher(userInput);
	
	if (matcher.find()) {
		try {
			int count = Integer.parseInt(matcher.group(1));
			if (count > 50) {
				return "That's a lot of jokes! How about we start with a smaller number, like 10?";
			}
			if (count <= 0) {
				return "I can only tell a positive number of jokes!";
			}
			
			List<String> jokes = jokeService.getJokes(count);
			if (jokes.isEmpty()) {
				return "I had trouble finding jokes right now. Please try again in a moment.";
			}
			// Join the jokes with a double newline for readability
			return "Here you go:\n\n" + String.join("\n\n", jokes);
			
		} catch (NumberFormatException e) {
			// Should be rare due to regex, but good to handle
			return getSingleJoke();
		}
	} else {
		// If no number is found, just tell one joke.
		return getSingleJoke();
	}
}

private String getSingleJoke() {
	Optional<String> joke = jokeService.getJoke();
	return joke.orElse("I tried to think of a joke, but my punchline was off. Please try again.");
}
}