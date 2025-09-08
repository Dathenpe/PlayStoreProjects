package com.f9ld3.xavier.ai.V2.handlers;

import com.f9ld3.xavier.ai.V2.ConversationContext;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class DateQueryHandler implements IntentHandler {
@Override
public String handle(String userInput, ConversationContext context) {
	return "Today's date is " + LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM d, yyyy"));
}
}