package com.f9ld3.xavier.ai.V2;

/**
 * A simple data class to hold the result of a classification.
 * It contains the predicted label (intent) and the confidence score of that prediction.
 */
public class PredictionResult {

private final String predictedLabel;
private final double confidence;

public PredictionResult(String predictedLabel, double confidence) {
	this.predictedLabel = predictedLabel;
	this.confidence = confidence;
}

public String getPredictedLabel() {
	return predictedLabel;
}

public double getConfidence() {
	return confidence;
}
}