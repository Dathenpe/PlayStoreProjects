package Ai;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.f9ld3.heal.R;

import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatMessageViewHolder> {
    private List<ChatMessage> messages;

    private static final int VIEW_TYPE_USER_MESSAGE = 1;
    private static final int VIEW_TYPE_AI_MESSAGE = 2;
    private static final int VIEW_TYPE_LOADING_MESSAGE = 3;


    public ChatAdapter(List<ChatMessage> messages) {
        this.messages = messages;
    }

    @Override
    public int getItemViewType(int position) {
        ChatMessage message = messages.get(position);
        if (message.isLoadingMessage()) {
            return VIEW_TYPE_LOADING_MESSAGE;
        } else if (message.isUserMessage()) {
            return VIEW_TYPE_USER_MESSAGE;
        } else {
            return VIEW_TYPE_AI_MESSAGE;
        }
    }

    @NonNull
    @Override
    public ChatMessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == VIEW_TYPE_USER_MESSAGE) {
            view = inflater.inflate(R.layout.item_user_message, parent, false);
        } else if (viewType == VIEW_TYPE_AI_MESSAGE) {
            view = inflater.inflate(R.layout.item_ai_message, parent, false);
        } else { // VIEW_TYPE_LOADING_MESSAGE
            view = inflater.inflate(R.layout.item_loading_message, parent, false);
        }
        return new ChatMessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatMessageViewHolder holder, int position) {
        ChatMessage message = messages.get(position);
        // The loading message layout might have its own text or just a progress bar
        if (!message.isLoadingMessage()) {
            holder.messageTextView.setText(message.getText());
        }
        // If it's a loading message, the text is often static or handled by the XML
        // and the ProgressBar visibility might be handled in XML too.
        // If you need to dynamically change text or progress bar visibility for loading:
        // if (holder.typingIndicatorProgress != null) {
        //     holder.typingIndicatorProgress.setVisibility(message.isLoadingMessage() ? View.VISIBLE : View.GONE);
        // }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class ChatMessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageTextView;
        // ProgressBar typingIndicatorProgress; // Optional, if you want to control it from adapter

        public ChatMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageTextView = itemView.findViewById(R.id.message_text_view);
            // typingIndicatorProgress = itemView.findViewById(R.id.typing_indicator_progress); // Optional
        }
    }
}
