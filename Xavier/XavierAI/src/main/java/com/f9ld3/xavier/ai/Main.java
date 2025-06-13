package com.f9ld3.xavier.ai;
import weka.core.Attribute;
import weka.core.Instances;
import weka.core.converters.ArffLoader;
import weka.filters.unsupervised.attribute.StringToWordVector;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class Main {
	public static void main(String[] args) {
		String arffData= """
				@RELATION xavier_conversational_intents
				
				@ATTRIBUTE text STRING
				@ATTRIBUTE intent {greeting, goodbye, unknown}
				
				@DATA
				'hello',greeting
				'hi',greeting
				'how are you',greeting
				'hi there',greeting
				'goodbye',goodbye
				'see ya', goodbye
				'random stuff',unknown
				'what is up',greeting
				'farewell',goodbye
				'i dont know',unknown
				""";
		System.out.println("-----Starting Xavier AI Initialization-----");
		System.out.println("Attempting to load initial conversational data");
		try {
			ArffLoader loader = new ArffLoader();
			byte[] arffDataBytes = arffData.getBytes(StandardCharsets.UTF_8);
			InputStream inputStream = new ByteArrayInputStream(arffDataBytes);
			loader.setSource(inputStream);
			Instances data = loader.getDataSet();
			
			if (data.classIndex() == -1){
				Attribute intentAttribute = data.attribute("intent");
				if (intentAttribute == null){
					throw new RuntimeException("Class attribute 'intent' not found in ARFF data.");
				}else {
					data.setClassIndex(intentAttribute.index());
				}
			}
			
			System.out.println("\n-----Data Loaded Successfully-----");
			System.out.println("-----Starting Xavier AI-----");
			System.out.println("Xavier AI is ready to go!");
			
			System.out.println("Dataset Name: " +  data.relationName());
			System.out.println("Number of Instances: " + data.numInstances());
			System.out.println("Number of Attributes: " + data.numAttributes());
			System.out.println("Class Attribute Set To: " + data.classAttribute().name() + " (Type: " + Attribute.typeToString(data.classAttribute().type()) + ")");
			
			System.out.println("/n ---Applying StringToWordVector Filter ---");
			StringToWordVector stringToWordVectorFilter = new StringToWordVector();
			
			stringToWordVectorFilter.setAttributeIndices(String.valueOf(data.attribute("text").index() + 1));
			stringToWordVectorFilter.setWordsToKeep(10000);
			
			stringToWordVectorFilter.setLowerCaseTokens(true);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}