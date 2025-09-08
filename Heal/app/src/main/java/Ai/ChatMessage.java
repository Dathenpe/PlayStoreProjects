package Ai;

// No changes needed here if using Gson, but ensure it's simple enough for serialization.
public class ChatMessage {
    private String text;
    private boolean isUserMessage;
    private boolean isLoadingMessage; // New field for typing indicator

    // Constructor for regular messages
    public ChatMessage(String text, boolean isUserMessage) {
        this(text, isUserMessage, false);
    }

    // Constructor that includes loading state
    public ChatMessage(String text, boolean isUserMessage, boolean isLoadingMessage) {
        this.text = text;
        this.isUserMessage = isUserMessage;
        this.isLoadingMessage = isLoadingMessage;
    }

    public String getText() {
        return text;
    }

    public boolean isUserMessage() {
        return isUserMessage;
    }

    public boolean isLoadingMessage() { // Getter for the new field
        return isLoadingMessage;
    }

    // Optional: Setter if you need to change loading state after creation
    public void setLoadingMessage(boolean loadingMessage) {
        isLoadingMessage = loadingMessage;
    }
}
