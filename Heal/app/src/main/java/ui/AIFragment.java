package ui;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;


import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.heal.MainActivity;
import com.example.heal.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import Ai.ChatAdapter;
import Ai.ChatMessage;
import viewmodels.GeneralViewModel;

import java.util.ArrayList;
import java.util.List;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

public class AIFragment extends Fragment {

    private RecyclerView chatRecyclerView;
    private ProgressBar loadingProgressBar;
    private LinearLayout inputContainer;
    private TextInputEditText messageInputEditText;
    private MaterialButton sendButton;

    //For  The RecyclerView

    private ChatAdapter chatAdapter;
    private List<ChatMessage> chatMessages;
    private AIFragment(){

    }

    private MainActivity mainActivity;
    private Context context;


    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
        if (context instanceof MainActivity) {
            mainActivity = (MainActivity) context;
        } else {
            Toast.makeText(context, "Error: Fragment attached to wrong activity", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {

        return inflater.inflate(R.layout.fragment_ai, container, false);
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        chatRecyclerView = view.findViewById(R.id.chat_recycler_view);
        loadingProgressBar = view.findViewById(R.id.loading_progress_bar);
        inputContainer = view.findViewById(R.id.input_container);
        messageInputEditText =  view.findViewById(R.id.message_input_edit_text);
        sendButton = view.findViewById(R.id.send_button);

        sendButton.setOnClickListener(v ->{
            String messageText = "";
            if (messageInputEditText.getText() != null){
                messageText = messageInputEditText.getText().toString().trim();
            }
            if (!messageText.isEmpty()){
                ChatMessage userMessage = new ChatMessage(messageText, true);
                addMessageToChat(userMessage);

                messageInputEditText.setText(" ");
                sendMessageToAi(messageText);
            }
        });


        mainActivity.toolbar.setTitle("Game Room");
        mainActivity.navigationView.setCheckedItem(R.id.nav_ai);

        View loadingProgressBar = view.findViewById(R.id.loading_progress_bar);
        View aiCoordinatorLayout = view.findViewById(R.id.ai_chat_coordinator_layout);

        GeneralViewModel viewModel = new ViewModelProvider(this).get(GeneralViewModel.class);

        viewModel.isLoading.observe(getViewLifecycleOwner(), isLoading -> {
            if (isLoading) {
                loadingProgressBar.setVisibility(View.VISIBLE);
                aiCoordinatorLayout.setVisibility(View.GONE);
            } else {
                loadingProgressBar.setVisibility(View.GONE);
                aiCoordinatorLayout.setVisibility(View.VISIBLE);
            }
        });
    }
    private void setupRecyclerView(){
        chatMessages = new ArrayList<>();
        chatAdapter = new ChatAdapter(chatMessages);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
       // to display new items bottom - top
        layoutManager.setStackFromEnd(true);
        //to display new items top - bottom
        //layoutManager.setReverseLayout(true);

        chatRecyclerView.setLayoutManager(layoutManager);
        chatRecyclerView.setAdapter(chatAdapter);

        //addMessageToChat(new ChatMessage("Hello! How can I help you today?",false));
    }
    private void sendMessageToAi(String userQuery){
        Log.d("AiChatFragment","User query: " + userQuery);
        showLoading(true);

        //--TODO: Main Ai Logic

        new Handler(Looper.getMainLooper()).postDelayed(new Runnable(){
            @Override
            public void run(){
                showLoading(false);
                String aiResponseText = "This is a simulated Ai response to: :\"" + userQuery + "\"";
                ChatMessage aiResponseMessage = new ChatMessage(aiResponseText, false);
                addMessageToChat(aiResponseMessage);
            }
        },2000);
    }
    private void addMessageToChat(ChatMessage message){
        if (chatMessages == null || chatAdapter == null){
            Log.e("AiChatFragment","Chat messages or adapter is null");
            return;
        }
        chatMessages.add(message);
        chatAdapter.notifyItemInserted(chatMessages.size() - 1);
        chatRecyclerView.smoothScrollToPosition(chatMessages.size() - 1);

    }
    private void showLoading(boolean isLoading){
        if (loadingProgressBar != null){
            loadingProgressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
        if (messageInputEditText != null){
            messageInputEditText.setEnabled(isLoading);
        }
        if (sendButton != null){
            sendButton.setEnabled(!isLoading);
        }
    }

}