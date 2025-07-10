package com.f9ld3.xavier.ai.V2.handlers;

import com.f9ld3.xavier.ai.V2.ConversationContext;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class TimeQueryHandler implements IntentHandler {
@Override
public String handle(String userInput, ConversationContext context) {
	return "The current time is " + LocalTime.now().format(DateTimeFormatter.ofPattern("h:mm a"));
}
}