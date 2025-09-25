package Ai;

import android.text.method.LinkMovementMethod; // Import this
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
    private OnItemLongClickListener onItemLongClickListener;

    private static final int VIEW_TYPE_USER_MESSAGE = 1;
    private static final int VIEW_TYPE_AI_MESSAGE = 2;
    private static final int VIEW_TYPE_LOADING_MESSAGE = 3;

    public interface OnItemLongClickListener {
        void onAiMessageLongClick(ChatMessage message);
    }

    public ChatAdapter(List<ChatMessage> messages) {
        this.messages = messages;
    }

    public void setOnItemLongClickListener(OnItemLongClickListener listener) {
        this.onItemLongClickListener = listener;
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
        } else {
            view = inflater.inflate(R.layout.item_loading_message, parent, false);
        }
        return new ChatMessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatMessageViewHolder holder, int position) {
        ChatMessage message = messages.get(position);
        if (!message.isLoadingMessage()) {
            holder.messageTextView.setText(message.getText());

            // Check if it's an AI message
            if (!message.isUserMessage()) {
                // ADDED: Enable link clicking for AI messages
                holder.messageTextView.setMovementMethod(LinkMovementMethod.getInstance());

                // Set long click listener for AI messages to enable copying
                if (onItemLongClickListener != null) {
                    holder.itemView.setOnLongClickListener(v -> {
                        onItemLongClickListener.onAiMessageLongClick(message);
                        return true;
                    });
                } else {
                    holder.itemView.setOnLongClickListener(null);
                }
            } else {
                // For user messages:

                // Clear long click listener on the item view (so default TextView long-click works)
                holder.itemView.setOnLongClickListener(null);

                // REMOVED: holder.messageTextView.setMovementMethod(null);
                // By not calling setMovementMethod(null), we allow the
                // android:textIsSelectable="true" in item_user_message.xml
                // to enable the default copy/selection behavior on the TextView itself.
            }
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class ChatMessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageTextView;

        public ChatMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageTextView = itemView.findViewById(R.id.message_text_view);
        }
    }
}