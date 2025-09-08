package com.f9ld3.xavier.ai.V2;

public class WolframAlphaResult {
   public String answer;
   public String interpretation;
   
   public WolframAlphaResult(String answer, String interpretation) {
	   this.answer = answer;
	   this.interpretation = interpretation;
   }
   
   public String getAnswer() {
	   return answer;
   }
   
   public String getInterpretation() {
	   return interpretation;
   }
}
