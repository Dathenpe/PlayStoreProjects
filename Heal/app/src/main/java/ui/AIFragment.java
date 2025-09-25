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

// Imports for Jira Integration
import com.f9ld3.heal.Fields;
import com.f9ld3.heal.IssueType;
import com.f9ld3.heal.JiraApiService;
import com.f9ld3.heal.JiraIssueRequest;
import com.f9ld3.heal.JiraIssueResponse;
import com.f9ld3.heal.MainActivity;
import com.f9ld3.heal.Project;
import com.f9ld3.heal.R;
import com.f9ld3.heal.RetrofitClient;
import com.f9ld3.xavier.ai.V2.ConversationContext;
import com.f9ld3.xavier.ai.V2.XavierCoreV2;
import com.f9ld3.xavier.ai.V2.utils.ContentSafetyFilter;
import com.f9ld3.xavier.ai.V2.utils.ContentSafetyFilter.ContentSafetyResult; // Import ContentSafetyResult explicitly
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
import retrofit2.Call;
import retrofit2.Response;

public class AIFragment extends Fragment implements CustomMessageDialogFragment.OnMessageDialogListener {

    private static final String TAG = "AIFragment";
    private static final String PREFS_NAME = "XavierAiPrefs";
    private static final String KEY_CHAT_HISTORY = "chatHistory";
    private static final String KEY_CONVERSATION_CONTEXT = "conversationContext";

    private RecyclerView chatRecyclerView;
    private LinearLayout inputContainer;
    private TextInputEditText messageInputEditText;
    private MaterialButton sendButton;
    private FloatingActionButton fabClearChat;
    private FloatingActionButton fabScrollToBottom;
    private FloatingActionButton fabUploadLogs; // New FAB for uploading logs
    private ImageView networkStatusIcon;

    private ChatAdapter chatAdapter;
    private List<ChatMessage> chatMessages;
    private MainActivity mainActivity;
    private Context context; // Store attached context

    private XavierCoreV2 xavier;
    private ConversationContext conversationContext;
    private boolean isXavierReadyAndTrained = false; // Combined flag

    private ContentSafetyFilter contentSafetyFilter;

    private final ExecutorService xavierExecutor = Executors.newSingleThreadExecutor();
    private final Gson gson = new Gson();
    private ChatMessage loadingIndicatorMessage;

    private final BroadcastReceiver networkChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "NetworkChangeReceiver: onReceive - Network state changed.");
            updateNetworkStatusVisuals();
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
        Log.d(TAG, "onAttach called.");
        this.context = context;
        if (context instanceof MainActivity) {
            mainActivity = (MainActivity) context;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView called.");
        return inflater.inflate(R.layout.fragment_ai, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.i(TAG, "onViewCreated: START");

        chatRecyclerView = view.findViewById(R.id.chat_recycler_view);
        inputContainer = view.findViewById(R.id.input_container);
        messageInputEditText = view.findViewById(R.id.message_input_edit_text);
        sendButton = view.findViewById(R.id.send_button);
        fabClearChat = view.findViewById(R.id.fab_clear_chat);
        fabScrollToBottom = view.findViewById(R.id.fab_scroll_to_bottom);
        networkStatusIcon = view.findViewById(R.id.network_status_icon);
        fabUploadLogs = view.findViewById(R.id.fab_upload_logs); // Initialize new FAB

        Log.d(TAG, "onViewCreated: Setting up RecyclerView...");
        setupRecyclerView();

        Log.d(TAG, "onViewCreated: Loading chat history...");
        loadChatHistory();

        Log.d(TAG, "onViewCreated: Initializing Xavier...");
        initializeXavier();

        sendButton.setOnClickListener(v -> handleSendButtonClick());
        fabClearChat.setOnClickListener(v -> showClearHistoryConfirmationDialog());
        fabScrollToBottom.setOnClickListener(v -> chatRecyclerView.smoothScrollToPosition(chatMessages.size() - 1));
        fabUploadLogs.setOnClickListener(v -> showUploadConfirmationDialog()); // Set listener for new FAB

        updateNetworkStatusVisuals();

        if (mainActivity != null && mainActivity.navigationView != null) {
            mainActivity.navigationView.setCheckedItem(R.id.nav_ai);
        }

        Log.i(TAG, "onViewCreated: END");
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d(TAG, "onStart: Registering network receiver.");
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        Context safeContext = getContextSafe();
        if (safeContext != null) {
            safeContext.registerReceiver(networkChangeReceiver, filter);
        } else {
            Log.w(TAG, "onStart: Could not register network receiver, context is null.");
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, "onStop: Unregistering network receiver.");
        Context safeContext = getContextSafe();
        if (safeContext != null) {
            try {
                safeContext.unregisterReceiver(networkChangeReceiver);
            } catch (IllegalArgumentException e) {
                Log.w(TAG, "onStop: NetworkChangeReceiver was not registered or already unregistered.", e);
            }
        } else {
            Log.w(TAG, "onStop: Could not unregister network receiver, context is null.");
        }
    }

    private void updateNetworkStatusVisuals() {
        Context safeContext = getContextSafe();
        if (safeContext == null) {
            Log.w(TAG, "updateNetworkStatusVisuals: safeContext is null, cannot update icon.");
            return;
        }
        if (networkStatusIcon == null) {
            Log.w(TAG, "updateNetworkStatusVisuals: networkStatusIcon is null, cannot update icon.");
            return;
        }

        ConnectivityManager cm = (ConnectivityManager) safeContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = null;
        if (cm != null) {
            activeNetwork = cm.getActiveNetworkInfo();
        }

        boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        Log.d(TAG, "updateNetworkStatusVisuals: Network isConnected = " + isConnected);

        if (isConnected) {
            networkStatusIcon.setImageResource(R.drawable.ic_network_connected);
            networkStatusIcon.setAlpha(1.0f);
            networkStatusIcon.setOnClickListener(v -> Toast.makeText(safeContext, "Online: Your device has a network connection.", Toast.LENGTH_SHORT).show());
        } else {
            networkStatusIcon.setImageResource(R.drawable.ic_network_disconnected);
            networkStatusIcon.setAlpha(0.6f);
            networkStatusIcon.setOnClickListener(v -> Toast.makeText(safeContext, "Offline: No network connection detected.", Toast.LENGTH_SHORT).show());
        }
    }

    private void handleSendButtonClick() {
        Log.i(TAG, "handleSendButtonClick: START");

        if (!isXavierReadyAndTrained) {
            Log.w(TAG, "handleSendButtonClick: Xavier not ready or training failed. Toast shown.");
            Toast.makeText(getContextSafe(), "AI is still warming up or encountered an issue...", Toast.LENGTH_SHORT).show();
            return;
        }

        String messageText = "";
        if (messageInputEditText.getText() != null) {
            messageText = messageInputEditText.getText().toString().trim();
        }

        if (!messageText.isEmpty()) {
            ContentSafetyResult userSafetyResult = contentSafetyFilter.analyzeContent(messageText);
            if (!userSafetyResult.isSafe()) {
                Log.w(TAG, "handleSendButtonClick: Unsafe content detected from user: '" + messageText + "'. Details: " + userSafetyResult);
                String feedbackMessage;
                if (userSafetyResult.getSeverityScore() >= 0.95) {
                    feedbackMessage = "Your message contains critically unsafe content. I cannot process this. Further attempts may result in restrictions.";
                } else if (userSafetyResult.getSeverityScore() >= 0.8) {
                    feedbackMessage = "I cannot respond to that. Please keep our conversation respectful and appropriate. Your message contained highly inappropriate content.";
                } else {
                    feedbackMessage = "I cannot respond to that. Please keep our conversation respectful and appropriate.";
                }
                addMessageToChat(new ChatMessage(feedbackMessage, false), true);
                showInputDisabled(false);
                Log.i(TAG, "handleSendButtonClick: END (unsafe user content)");
                return;
            }

            Log.i(TAG, "handleSendButtonClick: User message to send: '" + messageText + "'");
            ChatMessage userMessage = new ChatMessage(messageText, true);
            addMessageToChat(userMessage, true);
            messageInputEditText.setText("");

            sendMessageToAi(messageText);
            hideKeyboard(messageInputEditText);
        } else {
            Log.d(TAG, "handleSendButtonClick: Empty message entered. Toast shown.");
            Toast.makeText(getContextSafe(), "Please enter a message", Toast.LENGTH_SHORT).show();
        }

        Log.i(TAG, "handleSendButtonClick: END");
    }

    private void initializeXavier() {
        Log.i(TAG, "initializeXavier: START");
        showInputDisabled(true);
        messageInputEditText.setHint("Xavier is warming up...");
        if (networkStatusIcon != null) {
            networkStatusIcon.setImageResource(R.drawable.ic_network_checking);
            networkStatusIcon.setAlpha(0.8f);
            Context safeContext = getContextSafe();
            if (safeContext != null) {
                networkStatusIcon.setOnClickListener(v -> Toast.makeText(safeContext, "Initializing: AI is preparing for use.", Toast.LENGTH_SHORT).show());
            }
        }

        CompletableFuture.runAsync(() -> {
            Log.i(TAG, "initializeXavier_background: START");
            try {
                Context safeContext = getContextSafe();
                if (safeContext == null) {
                    Log.e(TAG, "initializeXavier_background: Context is NULL!");
                    throw new IllegalStateException("Context is null during Xavier initialization");
                }

                Log.d(TAG, "initializeXavier_background: Initializing ResponseGenerator...");
                ResponseGenerator.init(safeContext.getApplicationContext());

                contentSafetyFilter = new ContentSafetyFilter();
                Log.d(TAG, "initializeXavier_background: ContentSafetyFilter initialized.");

                Log.d(TAG, "initializeXavier_background: Creating XavierCoreV2 instance...");
                xavier = new XavierCoreV2(safeContext.getApplicationContext(), contentSafetyFilter);
                Log.d(TAG, "initializeXavier_background: XavierCoreV2 instance created.");

                Log.d(TAG, "initializeXavier_background: Starting Xavier training...");
                xavier.train("training_data.txt");
                Log.i(TAG, "initializeXavier_background: Xavier training finished. isTrained: " + (xavier != null && xavier.isTrained()));

                if (xavier == null || !xavier.isTrained()) {
                    Log.e(TAG, "initializeXavier_background: Xavier training FAILED or xavier instance is null.");
                    throw new Exception("Xavier training failed or resulted in an untrained state.");
                }

                Log.d(TAG, "initializeXavier_background: Loading conversation context...");
                loadConversationContext();

                if (conversationContext == null) {
                    Log.w(TAG, "initializeXavier_background: ConversationContext was null after load, creating new.");
                    conversationContext = new ConversationContext();
                }

                Log.d(TAG, "initializeXavier_background: Conversation context loaded/created.");
                isXavierReadyAndTrained = true;
                Log.i(TAG, "initializeXavier_background: SUCCESS - Xavier is ready and trained!");

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Log.d(TAG, "initializeXavier_background_ui: Updating UI (ready state).");
                        updateNetworkStatusVisuals();
                        showInputDisabled(false);
                        messageInputEditText.setHint("Type a message...");
                        if (chatMessages.isEmpty()) {
                            Log.d(TAG, "initializeXavier_background_ui: Chat is empty, adding welcome message.");
                            addMessageToChat(new ChatMessage("Hello! I'm Xavier. How can I help you today?", false), true);
                        }
                    });
                } else {
                    Log.w(TAG, "initializeXavier_background: getActivity() is null, cannot update UI for ready state.");
                }

            } catch (Exception e) {
                Log.e(TAG, "initializeXavier_background: FAILED to initialize Xavier AI", e);
                isXavierReadyAndTrained = false;
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Log.e(TAG, "initializeXavier_background_ui: Updating UI (initialization failed state).");
                        Toast.makeText(getContextSafe(), "Fatal Error: Could not initialize AI.", Toast.LENGTH_LONG).show();
                        addMessageToChat(new ChatMessage("I'm sorry, I couldn't start up correctly. Please restart the app.", false), false);
                        messageInputEditText.setHint("AI Initialization Failed");
                        updateNetworkStatusVisuals();
                    });
                } else {
                    Log.w(TAG, "initializeXavier_background: getActivity() is null, cannot update UI for failed state.");
                }
            }
            Log.i(TAG, "initializeXavier_background: END");
        }, xavierExecutor);
        Log.i(TAG, "initializeXavier: END (background task submitted)");
    }

    private void setupRecyclerView() {
        Log.d(TAG, "setupRecyclerView: START");
        if (chatMessages == null) {
            chatMessages = new ArrayList<>();
        }
        chatAdapter = new ChatAdapter(chatMessages);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContextSafe());
        layoutManager.setStackFromEnd(true);
        if (chatRecyclerView != null) {
            chatRecyclerView.setLayoutManager(layoutManager);
            chatRecyclerView.setAdapter(chatAdapter);
            chatRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                    super.onScrolled(recyclerView, dx, dy);
                    LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                    if (layoutManager == null || fabScrollToBottom == null || chatAdapter == null) {
                        return;
                    }

                    int lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition();
                    int totalItemCount = chatAdapter.getItemCount();

                    if (lastVisibleItemPosition < totalItemCount - 1) {
                        fabScrollToBottom.show();
                    } else {
                        fabScrollToBottom.hide();
                    }
                }
            });
        } else {
            Log.e(TAG, "setupRecyclerView: chatRecyclerView is NULL!");
        }
        Log.d(TAG, "setupRecyclerView: END");
    }

    private void sendMessageToAi(String userQuery) {
        Log.i(TAG, "sendMessageToAi: START for query: '" + userQuery + "'");

        showLoadingIndicator(true);
        showInputDisabled(true);

        CompletableFuture.supplyAsync(() -> {
            Log.i(TAG, "sendMessageToAi_background: START for query: '" + userQuery + "'");
            if (xavier == null || !xavier.isTrained()) {
                Log.e(TAG, "sendMessageToAi_background: Xavier instance is NULL or not trained.");
                return "I'm still learning. Please try again in a moment.";
            }
            if (conversationContext == null) {
                Log.w(TAG, "sendMessageToAi_background: ConversationContext is NULL. Creating new one.");
                conversationContext = new ConversationContext();
            }
            Log.d(TAG, "sendMessageToAi_background: Calling xavier.getResponse for: '" + userQuery + "'");
            String response = xavier.getResponse(userQuery, conversationContext);
            Log.i(TAG, "sendMessageToAi_background: Received response from XavierCoreV2: '" + response + "'");
            return response;
        }, xavierExecutor).whenComplete((responseText, throwable) -> {
            Log.i(TAG, "sendMessageToAi_whenComplete: START. ResponseText: '" + responseText + "'");
            if (throwable != null) {
                Log.e(TAG, "sendMessageToAi_whenComplete: Throwable received for query '" + userQuery + "':", throwable);
            }

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    Log.d(TAG, "sendMessageToAi_whenComplete_ui: Updating UI.");
                    showLoadingIndicator(false);
                    showInputDisabled(false);

                    String finalResponse;
                    if (throwable != null) {
                        Log.d(TAG, "sendMessageToAi_whenComplete_ui: Handling throwable.");
                        finalResponse = "Sorry, I encountered an internal error. Please try again.";
                    } else {
                        Log.d(TAG, "sendMessageToAi_whenComplete_ui: Adding AI response: '" + responseText + "'");
                        finalResponse = (responseText == null || responseText.isBlank()) ? "Sorry, I didn't get a response." : responseText;
                    }

                    addMessageToChat(new ChatMessage(finalResponse, false), true);
                    saveConversationContext();
                    Log.d(TAG, "sendMessageToAi_whenComplete_ui: UI update complete.");
                });
            } else {
                Log.w(TAG, "sendMessageToAi_whenComplete: getActivity() is NULL. Cannot update UI.");
            }
            Log.i(TAG, "sendMessageToAi_whenComplete: END.");
        });
        Log.i(TAG, "sendMessageToAi: END (CompletableFuture submitted) for query: '" + userQuery + "'");
    }

    private void showLoadingIndicator(boolean show) {
        if (chatAdapter == null || chatMessages == null || chatRecyclerView == null) {
            Log.w(TAG, "showLoadingIndicator: Adapter, messages, or RecyclerView is null. Cannot show/hide indicator.");
            return;
        }

        if (show) {
            if (loadingIndicatorMessage == null) {
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
            Log.e(TAG, "addMessageToChat: chatMessages or chatAdapter is null!");
            return;
        }

        if (!message.isUserMessage() && loadingIndicatorMessage != null) {
            showLoadingIndicator(false);
        }

        chatMessages.add(message);
        chatAdapter.notifyItemInserted(chatMessages.size() - 1);
        if (chatRecyclerView != null) {
            chatRecyclerView.smoothScrollToPosition(chatMessages.size() - 1);
        }
        if (saveHistory) {
            saveChatHistory();
        }
    }

    private void showInputDisabled(boolean isDisabled) {
        if (messageInputEditText != null) messageInputEditText.setEnabled(!isDisabled);
        if (sendButton != null) sendButton.setEnabled(!isDisabled);
        if (inputContainer != null) inputContainer.setAlpha(isDisabled ? 0.7f : 1.0f);
    }

    private void saveChatHistory() {
        Log.d(TAG, "saveChatHistory: Attempting to save " + (chatMessages != null ? chatMessages.size() : "null list") + " messages.");
        Context safeContext = getContextSafe();
        if (safeContext == null || chatMessages == null) {
            Log.w(TAG, "saveChatHistory: Context or chatMessages is null. Cannot save.");
            return;
        }
        SharedPreferences prefs = safeContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        try {
            String jsonChatHistory = gson.toJson(chatMessages);
            editor.putString(KEY_CHAT_HISTORY, jsonChatHistory);
            editor.apply();
            Log.i(TAG, "saveChatHistory: Success. Items: " + chatMessages.size());
        } catch (Exception e) {
            Log.e(TAG, "saveChatHistory: Error saving chat history", e);
        }
    }

    private void loadChatHistory() {
        Log.d(TAG, "loadChatHistory: START");
        Context safeContext = getContextSafe();
        if (safeContext == null) {
            Log.w(TAG, "loadChatHistory: Context is null. Cannot load.");
            if (chatMessages == null) chatMessages = new ArrayList<>();
            return;
        }
        if (chatMessages == null) chatMessages = new ArrayList<>();

        if (chatAdapter == null && chatRecyclerView != null) {
            Log.w(TAG, "loadChatHistory: chatAdapter was null, creating new one.");
            chatAdapter = new ChatAdapter(chatMessages);
            chatRecyclerView.setAdapter(chatAdapter);
        }
        SharedPreferences prefs = safeContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String jsonChatHistory = prefs.getString(KEY_CHAT_HISTORY, null);
        if (jsonChatHistory != null) {
            try {
                Type type = new TypeToken<List<ChatMessage>>() {}.getType();
                List<ChatMessage> loadedMessages = gson.fromJson(jsonChatHistory, type);
                if (loadedMessages != null && !loadedMessages.isEmpty()) {
                    chatMessages.clear();
                    chatMessages.addAll(loadedMessages);
                    Log.i(TAG, "loadChatHistory: Successfully loaded " + chatMessages.size() + " messages.");
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            if (chatAdapter != null) chatAdapter.notifyDataSetChanged();
                            if (chatRecyclerView != null && !chatMessages.isEmpty()) {
                                chatRecyclerView.scrollToPosition(chatMessages.size() - 1);
                            }
                        });
                    } else if (chatAdapter != null) {
                        chatAdapter.notifyDataSetChanged();
                        if (chatRecyclerView != null && !chatMessages.isEmpty()) {
                            chatRecyclerView.scrollToPosition(chatMessages.size() - 1);
                        }
                    }
                } else {
                    Log.d(TAG, "loadChatHistory: Loaded chat history was null or empty from JSON.");
                }
            } catch (Exception e) {
                Log.e(TAG, "loadChatHistory: Error loading chat history from JSON", e);
            }
        } else {
            Log.d(TAG, "loadChatHistory: No chat history found in SharedPreferences.");
        }
        Log.d(TAG, "loadChatHistory: END");
    }

    private void saveConversationContext() {
        Log.d(TAG, "saveConversationContext: START");
        Context safeContext = getContextSafe();
        if (safeContext == null || conversationContext == null) {
            Log.w(TAG, "saveConversationContext: Context or conversationContext is null. Cannot save.");
            return;
        }
        SharedPreferences prefs = safeContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        try {
            String jsonContext = gson.toJson(conversationContext);
            editor.putString(KEY_CONVERSATION_CONTEXT, jsonContext);
            editor.apply();
            Log.i(TAG, "saveConversationContext: Success.");
        } catch (Exception e) {
            Log.e(TAG, "saveConversationContext: Error saving ConversationContext", e);
        }
    }

    private void loadConversationContext() {
        Log.d(TAG, "loadConversationContext: START");
        Context safeContext = getContextSafe();
        if (safeContext == null) {
            Log.w(TAG, "loadConversationContext: Context is null. Creating new ConversationContext.");
            conversationContext = new ConversationContext();
            return;
        }
        SharedPreferences prefs = safeContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String jsonContext = prefs.getString(KEY_CONVERSATION_CONTEXT, null);
        if (jsonContext != null) {
            try {
                conversationContext = gson.fromJson(jsonContext, ConversationContext.class);
                if (conversationContext != null) {
                    Log.i(TAG, "loadConversationContext: Successfully loaded.");
                } else {
                    Log.w(TAG, "loadConversationContext: Failed to parse saved ConversationContext, creating new.");
                    conversationContext = new ConversationContext();
                }
            } catch (Exception e) {
                Log.e(TAG, "loadConversationContext: Error loading ConversationContext from JSON. Creating new.", e);
                conversationContext = new ConversationContext();
            }
        } else {
            Log.d(TAG, "loadConversationContext: No ConversationContext found, creating new.");
            conversationContext = new ConversationContext();
        }
        Log.d(TAG, "loadConversationContext: END");
    }

    private void clearChatHistory() {
        Log.i(TAG, "clearChatHistory: START");
        Context safeContext = getContextSafe();
        if (safeContext == null) return;
        if (chatMessages != null) chatMessages.clear();
        if (chatAdapter != null) chatAdapter.notifyDataSetChanged();
        SharedPreferences prefs = safeContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().remove(KEY_CHAT_HISTORY).apply();
        conversationContext = new ConversationContext();
        saveConversationContext();
        Toast.makeText(safeContext, "Chat history cleared", Toast.LENGTH_SHORT).show();
        Log.i(TAG, "clearChatHistory: END. Chat history cleared.");
        addMessageToChat(new ChatMessage("Hello! I'm Xavier. How can I help you today?", false), true);
    }

    private void showClearHistoryConfirmationDialog() {
        Log.d(TAG, "showClearHistoryConfirmationDialog called.");
        CustomMessageDialogFragment dialogFragment = CustomMessageDialogFragment.newInstance(
                "Clear Chat History",
                "Are you sure you want to delete all messages? This action cannot be undone.",
                "Clear All",
                "Cancel"
        );
        dialogFragment.setListener(this);
        if (getParentFragmentManager() != null) {
            dialogFragment.show(getParentFragmentManager(), "ClearHistoryDialog");
        } else {
            Log.e(TAG, "FragmentManager is null, cannot show ClearHistoryDialog.");
        }
    }

    private void showUploadConfirmationDialog() {
        Log.d(TAG, "showUploadConfirmationDialog called.");
        if (chatMessages == null || chatMessages.size() <= 1) {
            Toast.makeText(getContextSafe(), "There is not enough conversation to upload.", Toast.LENGTH_SHORT).show();
            return;
        }
        CustomMessageDialogFragment dialogFragment = CustomMessageDialogFragment.newInstance(
                "Help Improve Xavier?",
                "Would you like to upload your current chat session to help us improve Xavier's responses? Your conversation will be reviewed by our developers. This action is anonymous and cannot be undone.",
                "Upload",
                "Cancel"
        );
        dialogFragment.setListener(this);
        if (getParentFragmentManager() != null) {
            dialogFragment.show(getParentFragmentManager(), "UploadLogsDialog");
        } else {
            Log.e(TAG, "FragmentManager is null, cannot show UploadLogsDialog.");
        }
    }

    @Override
    public void onDialogPositiveClick(DialogFragment dialog) {
        Log.d(TAG, "onDialogPositiveClick: " + dialog.getTag());
        if ("ClearHistoryDialog".equals(dialog.getTag())) {
            clearChatHistory();
        } else if ("UploadLogsDialog".equals(dialog.getTag())) {
            uploadChatLogsToJira();
        }
    }

    @Override
    public void onDialogNegativeClick(DialogFragment dialog) {
        Log.d(TAG, "onDialogNegativeClick: " + dialog.getTag());
    }

    private void uploadChatLogsToJira() {
        if (chatMessages == null || chatMessages.isEmpty()) {
            Toast.makeText(getContextSafe(), "Chat log is empty.", Toast.LENGTH_SHORT).show();
            return;
        }

        StringBuilder logBuilder = new StringBuilder();
        logBuilder.append("--- Xavier Chat Log ---\n\n");
        for (ChatMessage message : chatMessages) {
            if (message.isTypingIndicator()) continue;
            String prefix = message.isUserMessage() ? "User: " : "Xavier: ";
            logBuilder.append(prefix).append(message.getText()).append("\n\n");
        }
        logBuilder.append("--- End of Log ---");

        String formattedLog = logBuilder.toString();
        String summary = "Xavier Chat Log Submission";

        Toast.makeText(getContextSafe(), "Uploading chat log...", Toast.LENGTH_LONG).show();
        submitLogsToJiraOnBackgroundThread(summary, formattedLog);
    }

    private void submitLogsToJiraOnBackgroundThread(String summary, String description) {
        xavierExecutor.execute(() -> {
            try {
                JiraApiService apiService = RetrofitClient.getJiraApiService();
                String authHeader = RetrofitClient.getAuthHeader();

                // UPDATED: Project key is now "XVL" as requested
                String jiraProjectKey = "XVL";
                String issueTypeName = "Story";

                Project project = new Project(jiraProjectKey);
                IssueType issueType = new IssueType(issueTypeName);
                Fields fields = new Fields(project, summary, description, issueType);
                JiraIssueRequest request = new JiraIssueRequest(fields);

                Call<JiraIssueResponse> call = apiService.createIssue(authHeader, request);
                Response<JiraIssueResponse> response = call.execute();

                if (getActivity() != null) {
                    if (response.isSuccessful() && response.body() != null) {
                        getActivity().runOnUiThread(() ->
                                Toast.makeText(getContextSafe(), "Thank you! Your feedback has been submitted.", Toast.LENGTH_LONG).show()
                        );
                    } else {
                        String errorBody = response.errorBody() != null ? response.errorBody().string() : "Unknown error";
                        Log.e(TAG, "Failed to create Jira issue for log submission: " + errorBody);
                        getActivity().runOnUiThread(() ->
                                Toast.makeText(getContextSafe(), "Failed to submit logs. Error: " + response.code(), Toast.LENGTH_LONG).show()
                        );
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Exception while submitting chat logs to Jira", e);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() ->
                            Toast.makeText(getContextSafe(), "An error occurred. Please check your internet connection.", Toast.LENGTH_LONG).show()
                    );
                }
            }
        });
    }

    private void hideKeyboard(View view) {
        Context safeContext = getContextSafe();
        if (safeContext != null && view != null) {
            InputMethodManager imm = (InputMethodManager) safeContext.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }

    private Context getContextSafe() {
        if (this.context != null) return this.context;
        if (getActivity() != null) return getActivity();
        if (getView() != null && getView().getContext() != null) return getView().getContext();
        return null;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG, "onDestroyView: Releasing views.");
        chatRecyclerView = null;
        inputContainer = null;
        messageInputEditText = null;
        sendButton = null;
        fabClearChat = null;
        fabScrollToBottom = null;
        fabUploadLogs = null; // Also release this new view
        networkStatusIcon = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy: START. Shutting down executor.");
        if (xavierExecutor != null && !xavierExecutor.isShutdown()) {
            List<Runnable> droppedTasks = xavierExecutor.shutdownNow();
            if (!droppedTasks.isEmpty()) {
                Log.w(TAG, "onDestroy: " + droppedTasks.size() + " tasks were dropped from xavierExecutor.");
            }
            Log.d(TAG, "onDestroy: XavierExecutor shutdown initiated.");
        }
        this.context = null;
        mainActivity = null;
        Log.i(TAG, "onDestroy: END.");
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume called.");
        updateNetworkStatusVisuals();
        if (mainActivity != null) {
            if (mainActivity.toolbar != null) mainActivity.toolbar.setTitle("Xavier");
            if (mainActivity.MenuTrigger != null) {
                mainActivity.MenuTrigger.setVisibility(View.GONE);
            }
            if (mainActivity.Fab != null) {
                mainActivity.invertShakeView(mainActivity.Fab);
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause: Saving chat history and conversation context.");
        saveChatHistory();
        saveConversationContext();
    }
}