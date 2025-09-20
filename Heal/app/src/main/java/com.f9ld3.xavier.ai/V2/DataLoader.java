package com.f9ld3.xavier.ai.V2;

import android.content.Context; // Import Android Context

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class DataLoader {

	private final List<List<String>> documents = new ArrayList<>();
	private final List<String> labels = new ArrayList<>();
	private final List<String> rawPhrases = new ArrayList<>();

	// MODIFIED: Method now takes Context and loads from assets
	public void loadDataFromAsset(Context context, String assetFileName) throws IOException {
		try (InputStream is = context.getAssets().open(assetFileName)) {
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
				String line;
				while ((line = reader.readLine()) != null) {
					line = line.trim();
					if (line.isEmpty() || line.startsWith("#")) {
						continue; // Skip empty lines and comments
					}
					String[] parts = line.split(",", 2);
					if (parts.length == 2) {
						String label = parts[0].trim();
						String phrase = parts[1].trim();

						labels.add(label);
						rawPhrases.add(phrase);
						documents.add(TextProcessor.tokenize(phrase));
					}
				}
			}
		}
	}

	public List<List<String>> getDocuments() { return documents; }
	public List<String> getLabels() { return labels; }
	public List<String> getRawPhrases() { return rawPhrases; }
}