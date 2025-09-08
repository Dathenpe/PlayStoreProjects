package ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.f9ld3.heal.MainActivity;
import com.f9ld3.heal.R;
import com.f9ld3.xavier.ai.V2.ConversationContext;
import com.f9ld3.xavier.ai.V2.XavierCoreV2;
import com.f9ld3.xavier.ai.V2.utils.ResponseGenerator;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import Ai.ChatAdapter;
import Ai.ChatMessage;

public class AIFragment extends Fragment implements CustomMessageDialogFragment.OnMessageDialogListener {

    private static final String TAG = "AIFragment";
    private static final String PREFS_NAME = "XavierAiPrefs";
    private static final String KEY_CHAT_HISTORY = "chatHistory";
    private static final String KEY_CONVERSATION_CONTEXT = "conversationContext"; // If you also want to save context

    private RecyclerView chatRecyclerView;
    // private ProgressBar loadingProgressBar; // Remove if using inline loading
    private LinearLayout inputContainer;
    private TextInputEditText messageInputEditText;
    private MaterialButton sendButton;
    private FloatingActionButton fabClearChat; // FAB for clearing chat
    private ImageView networkStatusIcon; // New: Network status icon

    private ChatAdapter chatAdapter;
    private List<ChatMessage> chatMessages;

    private MainActivity mainActivity;
    private Context context; // Keep this for broader context use

    private XavierCoreV2 xavier;
    private ConversationContext conversationContext;
    private boolean isXavierReady = false;
    private final ExecutorService xavierExecutor = Executors.newSingleThreadExecutor();
    private Gson gson = new Gson(); // Gson instance for JSON conversion

    // For inline loading indicator
    private ChatMessage loadingIndicatorMessage;

    // BroadcastReceiver for network state changes
    private final BroadcastReceiver networkChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateNetworkStatus();
        }
    };


    public AIFragment() {
        // Required empty public constructor
    }

    public static AIFragment newInstance() {
        return new AIFragment();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context; // Assign the attached context
        if (context instanceof MainActivity) {
            mainActivity = (MainActivity) context;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_ai, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        chatRecyclerView = view.findViewById(R.id.chat_recycler_view);
        // loadingProgressBar = view.findViewById(R.id.loading_progress_bar); // Remove
        inputContainer = view.findViewById(R.id.input_container);
        messageInputEditText = view.findViewById(R.id.message_input_edit_text);
        sendButton = view.findViewById(R.id.send_button);
        fabClearChat = view.findViewById(R.id.fab_clear_chat); // Initialize FAB
        networkStatusIcon = view.findViewById(R.id.network_status_icon); // Initialize network status icon

        setupRecyclerView();
        loadChatHistory(); // Load history before initializing Xavier
        initializeXavier();

        sendButton.setOnClickListener(v -> handleSendButtonClick());
        fabClearChat.setOnClickListener(v -> showClearHistoryConfirmationDialog());

        // Initial network status check
        updateNetworkStatus();

        if (mainActivity != null) {
            // Check if navigationView is null before accessing it
            if (mainActivity.navigationView != null) {
                mainActivity.navigationView.setCheckedItem(R.id.nav_ai);
            } else {
                Log.w(TAG, "MainActivity's navigationView is null in onViewCreated.");
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        // Register the network change receiver
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        if (getContextSafe() != null) {
            getContextSafe().registerReceiver(networkChangeReceiver, filter);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        // Unregister the network change receiver to avoid memory leaks
        if (getContextSafe() != null) {
            getContextSafe().unregisterReceiver(networkChangeReceiver);
        }
    }

    private void updateNetworkStatus() {
        if (getContextSafe() == null || networkStatusIcon == null) return;

        ConnectivityManager cm = (ConnectivityManager) getContextSafe().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();

        if (isConnected) {
            networkStatusIcon.setImageResource(R.drawable.ic_network_connected);
            networkStatusIcon.setAlpha(1.0f);
            networkStatusIcon.setOnClickListener(v -> Toast.makeText(getContextSafe(), "Online: Your device has a network connection.", Toast.LENGTH_SHORT).show());
        } else {
            networkStatusIcon.setImageResource(R.drawable.ic_network_disconnected);
            networkStatusIcon.setAlpha(0.6f);
            networkStatusIcon.setOnClickListener(v -> Toast.makeText(getContextSafe(), "Offline: No network connection detected.", Toast.LENGTH_SHORT).show());
        }
    }


    private void handleSendButtonClick() {
        if (!isXavierReady) {
            Toast.makeText(getContextSafe(), "AI is still warming up...", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!isNetworkConnected()) {
            Toast.makeText(getContextSafe(), "You are offline. Please check your connection.", Toast.LENGTH_SHORT).show();
            return;
        }

        String messageText = "";
        if (messageInputEditText.getText() != null) {
            messageText = messageInputEditText.getText().toString().trim();
        }
        if (!messageText.isEmpty()) {
            ChatMessage userMessage = new ChatMessage(messageText, true);
            addMessageToChat(userMessage, true); // Save after adding user message

            messageInputEditText.setText("");
            sendMessageToAi(messageText);
            hideKeyboard(messageInputEditText); // Pass the EditText view
        } else {
            Toast.makeText(getContextSafe(), "Please enter a message", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) getContextSafe().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }


    private void initializeXavier() {
        showInputDisabled(true); // Disable input while training/loading
        messageInputEditText.setHint("Xavier is warming up...");
        networkStatusIcon.setImageResource(R.drawable.ic_network_checking);
        networkStatusIcon.setAlpha(0.8f);
        networkStatusIcon.setOnClickListener(v -> Toast.makeText(getContextSafe(), "Initializing: AI is preparing for use.", Toast.LENGTH_SHORT).show());


        CompletableFuture.runAsync(() -> {
            try {
                if (getContext() != null) { // Ensure context is available
                    ResponseGenerator.init(getContext().getApplicationContext());
                    xavier = new XavierCoreV2(getContext().getApplicationContext());
                    xavier.train(getContext().getApplicationContext(), "training_data.txt");
                } else {
                    throw new IllegalStateException("Context is null during Xavier initialization");
                }

                // Load or create conversation context
                loadConversationContext(); // Load saved context, or create new if not found
                if (conversationContext == null) {
                    conversationContext = new ConversationContext();
                }

                isXavierReady = true;

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        updateNetworkStatus(); // Update status after initialization
                        showInputDisabled(false);
                        messageInputEditText.setHint("Type a message...");
                        if (chatMessages.isEmpty()) { // Only add welcome if no history loaded
                            addMessageToChat(new ChatMessage("Hello! I'm Xavier. How can I help you today?", false), true);
                        }
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to initialize Xavier AI", e);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContextSafe(), "Fatal Error: Could not initialize AI.", Toast.LENGTH_LONG).show();
                        addMessageToChat(new ChatMessage("I'm sorry, I couldn't start up correctly. Please restart the app.", false), false); // Don't save on init error
                    });
                }
            }
        }, xavierExecutor);
    }

    private void setupRecyclerView() {
        chatMessages = new ArrayList<>();
        chatAdapter = new ChatAdapter(chatMessages);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContextSafe());
        layoutManager.setStackFromEnd(true);
        chatRecyclerView.setLayoutManager(layoutManager);
        chatRecyclerView.setAdapter(chatAdapter);
    }

    private void sendMessageToAi(String userQuery) {
        showLoadingIndicator(true); // Show typing indicator
        showInputDisabled(true);    // Optionally disable input while AI is processing

        CompletableFuture.supplyAsync(() -> {
            if (xavier == null || conversationContext == null) {
                Log.e(TAG, "Xavier or ConversationContext not initialized before sending message.");
                return "Error: AI not ready.";
            }
            return xavier.getResponse(userQuery, conversationContext);
        }, xavierExecutor).whenComplete((responseText, throwable) -> {
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    showLoadingIndicator(false); // Hide typing indicator
                    showInputDisabled(false);      // Re-enable input
                    if (throwable != null) {
                        Log.e(TAG, "Error from XavierCore: " + throwable.getMessage(), throwable);
                        addMessageToChat(new ChatMessage("Sorry, I encountered an internal error. Please try again.", false), true);
                    } else {
                        addMessageToChat(new ChatMessage(responseText, false), true);
                    }
                    saveConversationContext(); // Save context after each AI response
                });
            }
        });
    }

    private void showLoadingIndicator(boolean show) {
        if (show) {
            if (loadingIndicatorMessage == null) {
                // The text for loadingIndicatorMessage is often set in its XML layout
                loadingIndicatorMessage = new ChatMessage("Xavier is typing...", false, true);
                chatMessages.add(loadingIndicatorMessage);
                chatAdapter.notifyItemInserted(chatMessages.size() - 1);
                chatRecyclerView.scrollToPosition(chatMessages.size() - 1);
            }
        } else {
            if (loadingIndicatorMessage != null) {
                int index = chatMessages.indexOf(loadingIndicatorMessage);
                if (index != -1) {
                    chatMessages.remove(index);
                    chatAdapter.notifyItemRemoved(index);
                }
                loadingIndicatorMessage = null;
            }
        }
    }


    private void addMessageToChat(ChatMessage message, boolean saveHistory) {
        if (chatMessages == null || chatAdapter == null) {
            Log.e(TAG, "Chat messages or adapter is null");
            return;
        }
        // Ensure we remove loading indicator before adding a new message from AI
        if (!message.isUserMessage() && loadingIndicatorMessage != null) {
            showLoadingIndicator(false);
        }

        chatMessages.add(message);
        chatAdapter.notifyItemInserted(chatMessages.size() - 1);
        chatRecyclerView.smoothScrollToPosition(chatMessages.size() - 1);

        if (saveHistory) {
            saveChatHistory();
        }
    }

    private void showInputDisabled(boolean isDisabled) {
        // if (loadingProgressBar != null) { // If you were still using the main progress bar
        //     loadingProgressBar.setVisibility(isDisabled ? View.VISIBLE : View.GONE);
        // }
        if (messageInputEditText != null) {
            messageInputEditText.setEnabled(!isDisabled);
        }
        if (sendButton != null) {
            sendButton.setEnabled(!isDisabled);
        }
        if (inputContainer != null) {
            // inputContainer.setEnabled(!isDisabled); // Enabling/disabling whole container can have odd visual effects
            inputContainer.setAlpha(isDisabled ? 0.7f : 1.0f); // Visually indicate disabled state
        }
    }


    private void saveChatHistory() {
        if (getContext() == null) return;
        SharedPreferences prefs = getContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        String jsonChatHistory = gson.toJson(chatMessages);
        editor.putString(KEY_CHAT_HISTORY, jsonChatHistory);
        editor.apply();
        Log.d(TAG, "Chat history saved.");
    }

    private void loadChatHistory() {
        if (getContext() == null) return;
        SharedPreferences prefs = getContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String jsonChatHistory = prefs.getString(KEY_CHAT_HISTORY, null);
        if (jsonChatHistory != null) {
            Type type = new TypeToken<ArrayList<ChatMessage>>() {}.getType();
            List<ChatMessage> loadedMessages = gson.fromJson(jsonChatHistory, type);
            if (loadedMessages != null && !loadedMessages.isEmpty()) {
                chatMessages.clear();
                chatMessages.addAll(loadedMessages);
                chatAdapter.notifyDataSetChanged();
                chatRecyclerView.scrollToPosition(chatMessages.size() - 1);
                Log.d(TAG, "Chat history loaded.");
            }
        }
    }

    private void saveConversationContext() {
        if (getContext() == null || conversationContext == null) return;
        SharedPreferences prefs = getContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        // Assuming ConversationContext is serializable with Gson.
        // If not, you'll need to make it so or manually extract data to save.
        try {
            String jsonContext = gson.toJson(conversationContext);
            editor.putString(KEY_CONVERSATION_CONTEXT, jsonContext);
            editor.apply();
            Log.d(TAG, "ConversationContext saved.");
        } catch (Exception e) {
            Log.e(TAG, "Error saving ConversationContext: " + e.getMessage());
        }
    }

    private void loadConversationContext() {
        if (getContext() == null) return;
        SharedPreferences prefs = getContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String jsonContext = prefs.getString(KEY_CONVERSATION_CONTEXT, null);
        if (jsonContext != null) {
            try {
                conversationContext = gson.fromJson(jsonContext, ConversationContext.class);
                if (conversationContext != null) {
                    Log.d(TAG, "ConversationContext loaded.");
                } else {
                    Log.w(TAG, "Failed to parse saved ConversationContext, creating new.");
                    conversationContext = new ConversationContext();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading ConversationContext: " + e.getMessage() + ". Creating new.");
                conversationContext = new ConversationContext();
            }
        } else {
            conversationContext = new ConversationContext(); // Create new if not found
        }
    }


    private void clearChatHistory() {
        if (getContext() == null) return;
        chatMessages.clear();
        chatAdapter.notifyDataSetChanged();
        // Clear from SharedPreferences
        SharedPreferences prefs = getContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().remove(KEY_CHAT_HISTORY).apply();
        // Optionally, reset conversation context as well
        conversationContext = new ConversationContext(); // Reset to a new context
        saveConversationContext(); // Save the cleared/new context state

        Toast.makeText(getContextSafe(), "Chat history cleared", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "Chat history cleared.");
        // Add back the initial greeting
        addMessageToChat(new ChatMessage("Hello! I'm Xavier. How can I help you today?", false), true);
    }

    private void showClearHistoryConfirmationDialog() {
        CustomMessageDialogFragment dialogFragment = CustomMessageDialogFragment.newInstance(
                "Clear Chat History",
                "Are you sure you want to delete all messages? This action cannot be undone.",
                "Clear All",
                "Cancel"
        );
        dialogFragment.setListener(this);
        if (getFragmentManager() != null) {
            dialogFragment.show(getFragmentManager(), "ClearHistoryDialog");
        }
    }

    @Override
    public void onDialogPositiveClick(DialogFragment dialog) {
        if ("ClearHistoryDialog".equals(dialog.getTag())) {
            clearChatHistory();
        }
    }

    @Override
    public void onDialogNegativeClick(DialogFragment dialog) {
        // User cancelled the dialog, do nothing
    }


    private void hideKeyboard(View view) {
        if (getContextSafe() != null) { // Use getContextSafe()
            InputMethodManager imm = (InputMethodManager) getContextSafe().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null && view != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }

    // Helper to safely get context, especially in async callbacks
    private Context getContextSafe() {
        if (context != null) return context;
        if (getActivity() != null) return getActivity();
        return null;
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        xavierExecutor.shutdown();
        Log.d(TAG, "XavierExecutor shutdown.");
        // Save history one last time if needed, though it's saved after each message
        // saveChatHistory();
        // saveConversationContext();

        if (mainActivity != null) {
            // Ensure these views are not null before accessing
            if (mainActivity.MenuTrigger != null) mainActivity.MenuTrigger.setVisibility(View.VISIBLE);
            if (mainActivity.Fab != null) {
                mainActivity.Fab.setVisibility(View.VISIBLE);
                mainActivity.shakeView(mainActivity.Fab);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mainActivity != null) {
            if (mainActivity.toolbar != null) mainActivity.toolbar.setTitle("Xavier");
            if (mainActivity.MenuTrigger != null) {
                mainActivity.MenuTrigger.setVisibility(View.GONE);
                mainActivity.invertShakeView(mainActivity.Fab);
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        saveChatHistory();
        saveConversationContext();
    }
}