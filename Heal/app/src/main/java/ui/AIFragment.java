package ui;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
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

import java.util.ArrayList;
import java.util.List;

import Ai.ChatAdapter;
import Ai.ChatMessage;
import viewmodels.GeneralViewModel;

// Gemini API Imports



public class AIFragment extends Fragment {

    private RecyclerView chatRecyclerView;
    private ProgressBar loadingProgressBar;
    private LinearLayout inputContainer; // Used for enabling/disabling input area
    private TextInputEditText messageInputEditText;
    private MaterialButton sendButton;

    private ChatAdapter chatAdapter;
    private List<ChatMessage> chatMessages;

    private MainActivity mainActivity;
    private Context context;

    // Declare GenerativeModel instance
  //  private GenerativeModel generativeModel;

    public AIFragment(){
        // Required empty public constructor
    }

    // Factory method for creating instances
    public static AIFragment newInstance() {
        return new AIFragment();
    }


    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
        if (context instanceof MainActivity) {
            mainActivity = (MainActivity) context;
        } else {
            Toast.makeText(context, "Error: Fragment attached to wrong activity", Toast.LENGTH_SHORT).show();
            // It's good practice to handle this error more robustly, e.g., throw an exception
        }
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.fragment_ai, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize RecyclerView first as it's used before UI elements
//        setupRecyclerView();

        // UI Element Initialization
        chatRecyclerView = view.findViewById(R.id.chat_recycler_view);
        loadingProgressBar = view.findViewById(R.id.loading_progress_bar);
        inputContainer = view.findViewById(R.id.input_container);
        messageInputEditText =  view.findViewById(R.id.message_input_edit_text);
        sendButton = view.findViewById(R.id.send_button);

        // Initialize GenerativeModel with your API key
        // Make sure BuildConfig.GEMINI_API_KEY is correctly set up in your build.gradle
       // generativeModel = new GenerativeModel("gemini-pro", BuildConfig.GEMINI_API_KEY);


        sendButton.setOnClickListener(v ->{
            String messageText = "";
            if (messageInputEditText.getText() != null){
                messageText = messageInputEditText.getText().toString().trim();
            }
            if (!messageText.isEmpty()){
                ChatMessage userMessage = new ChatMessage(messageText, true);
                addMessageToChat(userMessage);

                messageInputEditText.setText(""); // Clear input field
                sendMessageToAi(messageText); // Call our (now Gemini-powered) AI logic
                hideKeyboard(v); // Call local hideKeyboard method
            }else {
                Toast.makeText(getContext(), "Please enter a message", Toast.LENGTH_SHORT).show(); // Use getContext() or mainActivity.getApplicationContext()
            }
        });

        // Set Toolbar title and Navigation View item (assuming mainActivity is not null)
        if (mainActivity != null) {
            mainActivity.navigationView.setCheckedItem(R.id.nav_ai);
        } else {
            Log.e("AIFragment", "MainActivity is null, cannot set toolbar or navigation view.");
        }


        // --- Keeping ViewModel-related code as per "don't remove" instruction ---
        // Note: This ViewModel logic might conflict or be redundant with direct AI calls.
        View loadingProgressBarForViewModel = view.findViewById(R.id.loading_progress_bar); // Redundant find, but keeping
        View aiCoordinatorLayout = view.findViewById(R.id.ai_chat_coordinator_layout);

        GeneralViewModel viewModel = new ViewModelProvider(this).get(GeneralViewModel.class);

        viewModel.isLoading.observe(getViewLifecycleOwner(), isLoading -> {
            if (isLoading) {
                loadingProgressBarForViewModel.setVisibility(View.VISIBLE);
                if (aiCoordinatorLayout != null) { // Check if layout exists
                    aiCoordinatorLayout.setVisibility(View.GONE);
                }
            } else {
                loadingProgressBarForViewModel.setVisibility(View.GONE);
                if (aiCoordinatorLayout != null) { // Check if layout exists
                    aiCoordinatorLayout.setVisibility(View.VISIBLE);
                }
            }
        });

        // Optional: Add an initial greeting from the AI when the fragment loads
//        if (chatMessages.isEmpty() && chatMessages != null) {
//            addMessageToChat(new ChatMessage("Hello! How can I help you today?", false));
//        }
    }

    private void setupRecyclerView(){
        chatMessages = new ArrayList<>();
        chatAdapter = new ChatAdapter(chatMessages);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        layoutManager.setStackFromEnd(true); // to display new items bottom - top

        chatRecyclerView.setLayoutManager(layoutManager);
        chatRecyclerView.setAdapter(chatAdapter);
    }

    // --- UPDATED: Main AI Logic using Google Gemini ---
    private void sendMessageToAi(String userQuery){
        Log.d("AIFragment","User query: " + userQuery);
        showLoading(true); // Show loading indicator
//
//        // Prepare content for Gemini API
//        Content content = Content.newBuilder()
//                .addPart(Part.from(userQuery))
//                .build();
//
//        // Make the asynchronous API call to Gemini
//        ListenableFuture<GenerateContentResponse> response = GenerativeModelFutures.generateContent(generativeModel, content);
//
//        // Add a callback to handle the API response (success or failure)
//        Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
//            @Override
//            public void onSuccess(GenerateContentResponse result) {
//                showLoading(false); // Hide loading
//                String aiResponseText = result.getText(); // Get AI's text response
//                if (aiResponseText != null && !aiResponseText.isEmpty()) {
//                    addMessageToChat(new ChatMessage(aiResponseText, false)); // Add AI response to chat
//                } else {
//                    addMessageToChat(new ChatMessage("Sorry, I didn't get a clear response.", false));
//                    Log.w("AIFragment", "Gemini returned empty or null response.");
//                }
//            }
//
//            @Override
//            public void onFailure(Throwable t) {
//                showLoading(false); // Hide loading
//                Log.e("AIFragment", "Error from Gemini API: " + t.getMessage(), t);
//                Toast.makeText(getContext(), "Error getting AI response: " + t.getLocalizedMessage(), Toast.LENGTH_LONG).show();
//                addMessageToChat(new ChatMessage("Sorry, I'm having trouble connecting right now. Please try again.", false));
//            }
//        }, ContextCompat.getMainExecutor(context)); // Ensure callback runs on the main (UI) thread


        // --- Original Simulated AI Logic (commented out as per "don't remove" and new Gemini logic) ---
        /*
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable(){
            @Override
            public void run(){
                showLoading(false);
                String aiResponseText = "This is a simulated Ai response to: :\"" + userQuery + "\"";
                ChatMessage aiResponseMessage = new ChatMessage(aiResponseText, false);
                addMessageToChat(aiResponseMessage);
            }
        },2000);
        */
    }

    private void addMessageToChat(ChatMessage message){
        if (chatMessages == null || chatAdapter == null){
            Log.e("AIFragment","Chat messages or adapter is null");
            return;
        }
        chatMessages.add(message);
        chatAdapter.notifyItemInserted(chatMessages.size() - 1);
        chatRecyclerView.smoothScrollToPosition(chatMessages.size() - 1);
    }

    // --- CORRECTED: showLoading logic ---
    private void showLoading(boolean isLoading){
        if (loadingProgressBar != null){
            loadingProgressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
        // CORRECTED: Enable input and send button when NOT loading
        if (messageInputEditText != null){
            messageInputEditText.setEnabled(!isLoading);
        }
        if (sendButton != null){
            sendButton.setEnabled(!isLoading);
        }
        // You might also want to control the inputContainer or aiCoordinatorLayout here if they exist and are relevant
        if (inputContainer != null) {
            inputContainer.setEnabled(!isLoading); // Disable the whole input area if needed
        }
        // if (mainActivity != null && mainActivity.findViewById(R.id.ai_chat_coordinator_layout) != null) {
        //     mainActivity.findViewById(R.id.ai_chat_coordinator_layout).setVisibility(isLoading ? View.GONE : View.VISIBLE);
        // }
    }

    // --- NEW: Helper method to hide the keyboard ---
    private void hideKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
    @Override
    public void onDestroy(){
        super.onDestroy();
        if (mainActivity != null){
            mainActivity.MenuTrigger.setVisibility(View.VISIBLE);
            mainActivity. Fab.setVisibility(View.VISIBLE);
            mainActivity.shakeView(mainActivity.Fab);
        }
    }
}