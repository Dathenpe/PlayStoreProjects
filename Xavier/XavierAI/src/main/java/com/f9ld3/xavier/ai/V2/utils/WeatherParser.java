package com.f9ld3.xavier.ai.V2.utils;

import com.google.gson.JsonObject;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * A utility class to parse the JSON response from the OpenWeatherMap API
 * and format it into a user-friendly string.
 */
public final class WeatherParser {

private WeatherParser() {}

/**
 * Parses the weather JSON and creates a human-readable summary.
 * @param weatherData The full JSON object from the weather API.
 * @return A formatted string describing the weather.
 */
public static String parse(JsonObject weatherData) {
	if (weatherData == null) {
		return "I couldn't retrieve the weather data.";
	}
	
	try {
		String cityName = weatherData.get("name").getAsString();
		
		JsonObject main = weatherData.getAsJsonObject("main");
		double temp = main.get("temp").getAsDouble();
		double feelsLike = main.get("feels_like").getAsDouble();
		int humidity = main.get("humidity").getAsInt();
		
		JsonObject weather = weatherData.getAsJsonArray("weather").get(0).getAsJsonObject();
		String description = weather.get("description").getAsString();
		// Capitalize the first letter of the description
		description = description.substring(0, 1).toUpperCase() + description.substring(1);
		
		JsonObject wind = weatherData.getAsJsonObject("wind");
		double windSpeed = wind.get("speed").getAsDouble();
		// Convert from m/s to km/h
		windSpeed = windSpeed * 3.6;
		
		// Round the doubles to one decimal place for a cleaner look
		BigDecimal tempRounded = BigDecimal.valueOf(temp).setScale(1, RoundingMode.HALF_UP);
		BigDecimal feelsLikeRounded = BigDecimal.valueOf(feelsLike).setScale(1, RoundingMode.HALF_UP);
		BigDecimal windRounded = BigDecimal.valueOf(windSpeed).setScale(1, RoundingMode.HALF_UP);
		
		return String.format(
				"In %s, the weather is currently: %s. The temperature is %s°C, but it feels like %s°C. The humidity is at %d%% with winds of %s km/h.",
				cityName,
				description,
				tempRounded,
				feelsLikeRounded,
				humidity,
				windRounded
		);
		
	} catch (Exception e) {
		System.err.println("Error parsing weather JSON: " + e.getMessage());
		return "I had trouble interpreting the weather data I received.";
	}
}
}