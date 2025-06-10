package com.heal.Heal.backend.controller;

import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.generativeai.GenerativeModel;
import com.google.cloud.vertexai.generativeai.ResponseHandler;
import com.google.cloud.vertexai.api.GenerateContentResponse;
import com.google.cloud.vertexai.api.Content;
import com.google.cloud.vertexai.api.HarmCategory;
import com.google.cloud.vertexai.api.SafetySetting;
import com.google.protobuf.ByteString;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.lang.InterruptedException;

@RestController
@RequestMapping("/api/gemini")
public class GeminiController {

@Value("${gcp.project-id}")
private String projectId;

@Value("${gcp.location}")
private String location;

@Value("${gemini.api-key:}") // Use a default empty string to avoid errors if not present
private String apiKey;


@PostMapping("/chat")
public Map<String, String> sendMessage(@RequestBody Map<String, String> requestBody) {
	String message = requestBody.get("message");
	if (message == null || message.trim().isEmpty()) {
		return Collections.singletonMap("error", "Message cannot be empty");
	}
	
	try (VertexAI vertexAi = new VertexAI(projectId, location)) {
		GenerativeModel model;
		
		model = new GenerativeModel("gemini-pro", vertexAi); // Model name (e.g., "gemini-1.0-pro-001")
		
		
		Content content = Content.newBuilder()
				                  .addParts(com.google.cloud.vertexai.api.Part.newBuilder().setText(message).build())
				                  .build();
		
		SafetySetting safetySetting = SafetySetting.newBuilder()
				                              .setCategory(HarmCategory.HARM_CATEGORY_SEXUALLY_EXPLICIT)
				                              .setThreshold(SafetySetting.HarmBlockThreshold.BLOCK_MEDIUM_AND_ABOVE)
				                              .build();
		
		GenerateContentResponse response = model.generateContent(content);
		
		String generatedText = ResponseHandler.getText(response);
		
		return Collections.singletonMap("message", generatedText);
		
	} catch (IOException e) {
		e.printStackTrace(); // Log the full stack trace for debugging
		return Collections.singletonMap("error", "Error during AI generation: " + e.getClass().getSimpleName() + " - " + e.getMessage());
	} catch (Exception e) {
		e.printStackTrace(); // Catch any other unexpected exceptions
		return Collections.singletonMap("error", "An unexpected error occurred: " + e.getClass().getSimpleName() + " - " + e.getMessage());
	}
}
}