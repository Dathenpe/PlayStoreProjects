package Ai;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.heal.R;

import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatMessageViewHolder> {
    private List <ChatMessage> messages;

    public ChatAdapter(List<ChatMessage> messages){
        this.messages = messages;
    }

    @NonNull
    @Override
    public  ChatMessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType){
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_message,parent,false);
        return new ChatMessageViewHolder(view);
    }
    @Override
    public void onBindViewHolder(@NonNull ChatMessageViewHolder holder, int position){
        ChatMessage message = messages.get(position);
        holder.messageTextView.setText(message.getText());

//        if (message.isUserMessage()){
//            holder.messageTextView.setTextAlignment(View.TEXT_ALIGNMENT_END);
//        }else{
//            holder.messageTextView.setTextAlignment(View.TEXT_ALIGNMENT_START);
//        }
    }
    @Override
    public int getItemCount(){
        return messages.size();
    }
    static class ChatMessageViewHolder extends RecyclerView.ViewHolder{
        TextView messageTextView;
        public ChatMessageViewHolder(@NonNull View itemView){
            super(itemView);
            messageTextView  = itemView.findViewById(R.id.message_text_view);
        }
    }
}
