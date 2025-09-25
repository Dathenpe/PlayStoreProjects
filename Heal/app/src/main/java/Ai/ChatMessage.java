package Ai;

public class ChatMessage {
    private String text;
    private boolean isUserMessage;
    private boolean isLoadingMessage; // This field tracks the typing indicator

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

    /**
     * @return True if this message is the "is typing..." indicator.
     * @deprecated Renamed for clarity. Use isTypingIndicator() instead.
     */
    public boolean isLoadingMessage() {
        return isLoadingMessage;
    }

    /**
     * NEW METHOD
     * Checks if this chat message is a temporary "typing..." indicator.
     * This directly resolves the "Cannot resolve method" error in AIFragment.
     * @return true if the message is a typing indicator, false otherwise.
     */
    public boolean isTypingIndicator() {
        return this.isLoadingMessage;
    }

    // Optional: Setter if you need to change loading state after creation
    public void setLoadingMessage(boolean loadingMessage) {
        isLoadingMessage = loadingMessage;
    }
}