// main/java/com/f9ld3/Zion/ui/blog/EditPostActivity.java
package com.f9ld3.Zion.ui.blog;

import static com.f9ld3.Zion.ui.feed.CommentsBottomSheet.TAG; // Reusing TAG, consider a specific one

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable; // Added import
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import com.f9ld3.Zion.R;
import com.f9ld3.Zion.databinding.ActivityEditPostBinding; // Use correct binding
import com.f9ld3.Zion.ui.dialogs.CustomAlertDialogFragment;
import com.f9ld3.Zion.ui.feed.MediaItem;
import com.f9ld3.Zion.ui.feed.PollOption;
import com.f9ld3.Zion.ui.feed.Post;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class EditPostActivity extends AppCompatActivity implements MediaPreviewAdapter.OnMediaItemClickListener {

    public static final String EXTRA_POST_TO_EDIT = "extra_post_to_edit";
    // Consider a more specific TAG like "EditPostActivityTAG"
    // private static final String TAG = "EditPostActivity";

    private ActivityEditPostBinding binding;
    private EditPostViewModel editPostViewModel;
    private MediaPreviewAdapter adapter;
    private final List<MediaItem> mediaItems = new ArrayList<>(); // Stores media for the adapter
    private String currentPostType = Post.TYPE_TEXT_MEDIA; // Holds the currently selected type
    private Post postToEdit; // Holds the original post data

    // --- Poll Option Management Logic ---
    private final List<TextInputLayout> pollOptionLayouts = new ArrayList<>();
    private final List<TextInputEditText> pollOptionEditTexts = new ArrayList<>();
    private final int MAX_POLL_OPTIONS = 5;
    private final int MIN_POLL_OPTIONS = 2;
    // --- End Poll Option Management ---


    // Media Picker Launcher (remains the same)
    private final ActivityResultLauncher<Intent> pickMediaLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    int maxSelection = 15;
                    int currentCount = mediaItems.size();
                    int addedCount = 0;

                    if (result.getData().getClipData() != null) {
                        int count = result.getData().getClipData().getItemCount();
                        addedCount = Math.min(count, maxSelection - currentCount);
                        if (count > addedCount) {
                            Toast.makeText(this, "Maximum of " + maxSelection + " files allowed.", Toast.LENGTH_SHORT).show();
                        }
                        for (int i = 0; i < addedCount; i++) {
                            Uri uri = result.getData().getClipData().getItemAt(i).getUri();
                            MediaItem newItem = createMediaItemFromUri(uri);
                            if (newItem != null) {
                                mediaItems.add(newItem);
                            }
                        }
                    } else if (result.getData().getData() != null) {
                        if (currentCount < maxSelection) {
                            Uri uri = result.getData().getData();
                            MediaItem newItem = createMediaItemFromUri(uri);
                            if (newItem != null) {
                                mediaItems.add(newItem);
                                addedCount = 1;
                            }
                        } else {
                            Toast.makeText(this, "Maximum of " + maxSelection + " files allowed.", Toast.LENGTH_SHORT).show();
                        }
                    }

                    if (addedCount > 0) {
                        adapter.notifyDataSetChanged();
                        binding.mediaPreviewRecycler.setVisibility(View.VISIBLE);
                        editPostViewModel.setMediaChanged(true); // Mark media as potentially changed
                    }
                }
            });


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEditPostBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Retrieve the post to edit
        if (getIntent().hasExtra(EXTRA_POST_TO_EDIT)) {
            try {
                postToEdit = (Post) getIntent().getSerializableExtra(EXTRA_POST_TO_EDIT);
            } catch (ClassCastException e) {
                Log.e(TAG, "Failed to cast Post from intent extra", e);
                postToEdit = null;
            }
        }

        if (postToEdit == null) {
            Log.e(TAG, "EditPostActivity started without valid Post data. Finishing.");
            Toast.makeText(this, "Error: Could not load post to edit.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        editPostViewModel = new ViewModelProvider(this).get(EditPostViewModel.class);

        setupToolbar();
        setupRecyclerView();
        setupPollOptionManagement(); // <-- CALL NEW SETUP METHOD HERE
        setupClickListeners();
        observeViewModel();
        populateUiWithPostData(); // Populate AFTER setting up initial poll views
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if(getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Edit Post"); // Set title
        }
    }

    private void setupRecyclerView() {
        adapter = new MediaPreviewAdapter(mediaItems, this);
        binding.mediaPreviewRecycler.setLayoutManager(new GridLayoutManager(this, 3));
        binding.mediaPreviewRecycler.setAdapter(adapter);
    }

    // --- UPDATED populateUiWithPostData ---
    private void populateUiWithPostData() {
        if (postToEdit == null) return;

        binding.editTextContent.setText(postToEdit.getTextContent());
        currentPostType = postToEdit.getPostType(); // Set initial post type

        // --- Auto-select Post Type Toggle ---
        int buttonIdToCheck;
        switch (currentPostType) {
            case Post.TYPE_POLL:
                buttonIdToCheck = R.id.button_type_poll;
                break;
            case Post.TYPE_QUIZ:
                buttonIdToCheck = R.id.button_type_quiz;
                break;
            case Post.TYPE_TEXT_MEDIA:
            default:
                buttonIdToCheck = R.id.button_type_media;
                break;
        }
        binding.togglePostType.check(buttonIdToCheck);
        // --- End Auto-select ---

        // --- Media items population ---
        mediaItems.clear(); // Clear existing adapter items
        if (postToEdit.getMediaItems() != null && currentPostType.equals(Post.TYPE_TEXT_MEDIA)) {
            mediaItems.addAll(postToEdit.getMediaItems()); // Add items from the post
        }
        adapter.notifyDataSetChanged();
        binding.mediaPreviewRecycler.setVisibility(mediaItems.isEmpty() ? View.GONE : View.VISIBLE);
        // --- End Media items population ---


        // --- Revamped Poll/Quiz Population ---
        clearPollOptions(); // Start fresh (clears duration too)

        if (Post.TYPE_POLL.equals(currentPostType) || Post.TYPE_QUIZ.equals(currentPostType)) {
            List<PollOption> options = postToEdit.getPollOptions();
            if (options != null) {
                // Populate the initial MIN_POLL_OPTIONS views first
                for (int i = 0; i < MIN_POLL_OPTIONS; i++) {
                    if (i < pollOptionEditTexts.size() && pollOptionEditTexts.get(i) != null) {
                        if (i < options.size()) {
                            pollOptionEditTexts.get(i).setText(options.get(i).getOptionText());
                        } else {
                            pollOptionEditTexts.get(i).setText("");
                        }
                    } else {
                        Log.e(TAG, "Missing EditText for initial poll option index: " + i);
                    }
                }
                // Add and populate the remaining options dynamically
                for (int i = MIN_POLL_OPTIONS; i < options.size(); i++) {
                    addPollOptionInputView(options.get(i).getOptionText());
                }

                // *** Set Poll Duration Radio Button ***
                Integer duration = postToEdit.getPollDurationHours();
                int durationButtonId = R.id.duration_none; // Default
                if (duration != null) {
                    if (duration == 24) {
                        durationButtonId = R.id.duration_24h;
                    } else if (duration == 72) {
                        durationButtonId = R.id.duration_3d;
                    }
                }
                binding.pollDurationGroup.check(durationButtonId);
                // *** End Set Duration ***


                if (Post.TYPE_QUIZ.equals(currentPostType)) {
                    updateQuizRadioButtons(); // Ensure radio buttons reflect populated text
                    int correctIndex = postToEdit.getQuizCorrectOptionIndex();
                    if (correctIndex >= 0 && correctIndex < pollOptionEditTexts.size()) {
                        binding.quizCorrectAnswerGroup.check(correctIndex);
                    }
                }
            }
        }
        // --- End Revamped Population ---

        updateUiForPostType(); // Update general UI visibility based on the final type
    }
    // --- END UPDATE ---


    private MediaItem createMediaItemFromUri(Uri uri) {
        // --- (Implementation is identical to CreatePostActivity, keep it here) ---
        if (uri == null) return null;
        String mimeType = getContentResolver().getType(uri);
        String mediaType = "unknown";
        String thumbnailUrl = uri.toString();

        if (mimeType != null) {
            if (mimeType.startsWith("image")) mediaType = "image";
            else if (mimeType.startsWith("video")) mediaType = "video";
        } else {
            String path = uri.getPath();
            if (path != null) {
                String lowerPath = path.toLowerCase();
                if (lowerPath.endsWith(".jpg") || lowerPath.endsWith(".jpeg") || lowerPath.endsWith(".png") || lowerPath.endsWith(".gif") || lowerPath.endsWith(".webp")) mediaType = "image";
                else if (lowerPath.endsWith(".mp4") || lowerPath.endsWith(".mov") || lowerPath.endsWith(".avi") || lowerPath.endsWith(".mkv") || lowerPath.endsWith(".webm")) mediaType = "video";
            }
        }
        if ("unknown".equals(mediaType)) {
            Log.w(TAG, "Could not determine media type for URI: " + uri + ". Skipping file.");
            Toast.makeText(this, "Unsupported file type selected", Toast.LENGTH_SHORT).show();
            return null;
        }
        return new MediaItem(mediaType, uri.toString(), thumbnailUrl);
    }

    @Override
    public void onMediaItemClick(int position) {
        if (position >= 0 && position < mediaItems.size()) {
            showRemoveMediaConfirmation(position);
        }
    }

    private void showRemoveMediaConfirmation(int position) {
        CustomAlertDialogFragment dialog = CustomAlertDialogFragment.newInstance(
                "Remove Media?",
                "Are you sure you want to remove this item?",
                "Remove",
                "Cancel"
        );
        dialog.setDialogListener(new CustomAlertDialogFragment.DialogListener() {
            @Override public void onPositiveClick() { removeMediaItem(position); }
            @Override public void onNegativeClick() {}
        });
        if (!isFinishing()) dialog.show(getSupportFragmentManager(), "RemoveMediaDialog");
    }

    private void removeMediaItem(int position) {
        if (position >= 0 && position < mediaItems.size()) {
            mediaItems.remove(position);
            adapter.notifyItemRemoved(position);
            adapter.notifyItemRangeChanged(position, mediaItems.size()); // Important for correct indexing
            if (mediaItems.isEmpty()) binding.mediaPreviewRecycler.setVisibility(View.GONE);
            editPostViewModel.setMediaChanged(true); // Mark that media list was modified
        }
    }

    // --- Poll Option Logic (remains the same) ---
    private final TextWatcher quizOptionTextWatcher = new TextWatcher() {
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
        @Override public void afterTextChanged(Editable s) {
            if (currentPostType.equals(Post.TYPE_QUIZ)) {
                updateQuizRadioButtons();
            }
        }
    };

    private void setupPollOptionManagement() {
        for (int i = 0; i < MIN_POLL_OPTIONS; i++) {
            addPollOptionInputView(null);
        }
        updatePollOptionHintsAndRemoveButtons();
    }

    private void addPollOptionInputView(@Nullable String initialText) {
        if (pollOptionLayouts.size() >= MAX_POLL_OPTIONS) return;

        LayoutInflater inflater = LayoutInflater.from(this);
        TextInputLayout newLayout = (TextInputLayout) inflater.inflate(R.layout.item_poll_option_input, binding.pollOptionsContainer, false);
        TextInputEditText newEditText = newLayout.findViewById(R.id.input_poll_option);

        if (newEditText != null) {
            if (initialText != null) newEditText.setText(initialText);
            newEditText.addTextChangedListener(quizOptionTextWatcher);
            newLayout.setEndIconOnClickListener(v -> removePollOptionInputView(newLayout));

            binding.pollOptionsContainer.addView(newLayout);
            pollOptionLayouts.add(newLayout);
            pollOptionEditTexts.add(newEditText);

            updatePollOptionHintsAndRemoveButtons();
            updateQuizRadioButtons();
            binding.buttonAddOption.setEnabled(pollOptionLayouts.size() < MAX_POLL_OPTIONS);

        } else {
            Log.e(TAG, "Could not find TextInputEditText in inflated poll option layout.");
        }
    }

    private void removePollOptionInputView(TextInputLayout layoutToRemove) {
        if (pollOptionLayouts.size() <= MIN_POLL_OPTIONS) return;

        int indexToRemove = pollOptionLayouts.indexOf(layoutToRemove);
        if (indexToRemove != -1) {
            binding.pollOptionsContainer.removeView(layoutToRemove);
            pollOptionLayouts.remove(indexToRemove);
            if (indexToRemove < pollOptionEditTexts.size()) {
                pollOptionEditTexts.remove(indexToRemove);
            }
            updatePollOptionHintsAndRemoveButtons();
            updateQuizRadioButtons();
            binding.buttonAddOption.setEnabled(true);
        }
    }

    private void updatePollOptionHintsAndRemoveButtons() {
        for (int i = 0; i < pollOptionLayouts.size(); i++) {
            TextInputLayout layout = pollOptionLayouts.get(i);
            if (layout == null) continue;
            boolean isRequired = i < MIN_POLL_OPTIONS;
            layout.setHint("Option " + (i + 1) + (isRequired ? " (Required)" : ""));
            boolean showRemoveIcon = i >= MIN_POLL_OPTIONS;
            layout.setEndIconVisible(showRemoveIcon);
        }
    }

    private void updateQuizRadioButtons() {
        binding.quizCorrectAnswerGroup.removeAllViews();
        if (!currentPostType.equals(Post.TYPE_QUIZ)) return;
        int currentSelection = binding.quizCorrectAnswerGroup.getCheckedRadioButtonId();

        for (int i = 0; i < pollOptionEditTexts.size(); i++) {
            TextInputEditText editText = pollOptionEditTexts.get(i);
            if (editText == null) continue;
            String optionText = editText.getText().toString().trim();
            if (!optionText.isEmpty()) {
                RadioButton radioButton = new RadioButton(this);
                radioButton.setId(i);
                radioButton.setText(optionText);
                radioButton.setLayoutParams(new RadioGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                binding.quizCorrectAnswerGroup.addView(radioButton);
                if (i == currentSelection) radioButton.setChecked(true);
            }
        }
    }

    // --- UPDATED clearPollOptions ---
    private void clearPollOptions() {
        while (pollOptionLayouts.size() > MIN_POLL_OPTIONS) {
            removePollOptionInputView(pollOptionLayouts.get(pollOptionLayouts.size() - 1));
        }
        for (TextInputEditText editText : pollOptionEditTexts) {
            if (editText != null) editText.setText("");
        }
        binding.quizCorrectAnswerGroup.clearCheck();
        binding.pollDurationGroup.check(R.id.duration_none); // *** Reset duration ***
        updatePollOptionHintsAndRemoveButtons();
        updateQuizRadioButtons();
        binding.buttonAddOption.setEnabled(true);
    }
    // --- END UPDATE ---


    private void setupClickListeners() {
        binding.buttonSavePost.setOnClickListener(v -> savePostChanges());
        binding.buttonAttachMedia.setOnClickListener(v -> openMediaPicker());
        binding.buttonAddOption.setOnClickListener(v -> addPollOptionInputView(null)); // Use new method

        binding.togglePostType.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return; // Only act on the checked button

            String previousType = currentPostType;
            String newlySelectedType = Post.TYPE_TEXT_MEDIA; // Default

            if (checkedId == R.id.button_type_poll) newlySelectedType = Post.TYPE_POLL;
            else if (checkedId == R.id.button_type_quiz) newlySelectedType = Post.TYPE_QUIZ;

            // Only show confirmation if switching *away* from Poll/Quiz *to* Media
            if (!newlySelectedType.equals(previousType) &&
                    (previousType.equals(Post.TYPE_POLL) || previousType.equals(Post.TYPE_QUIZ)) &&
                    newlySelectedType.equals(Post.TYPE_TEXT_MEDIA)) {

                boolean hasPollData = false;
                for (TextInputEditText editText : pollOptionEditTexts) {
                    if (editText != null && editText.getText().length() > 0) {
                        hasPollData = true;
                        break;
                    }
                }

                if (hasPollData) {
                    showSwitchConfirmationDialog(previousType, newlySelectedType);
                } else {
                    currentPostType = newlySelectedType; // Update state directly
                    updateUiForPostType(); // Update UI
                }
            } else if (!newlySelectedType.equals(previousType)) {
                // If switching between Poll/Quiz or to Poll/Quiz from Media, just update
                currentPostType = newlySelectedType;
                updateUiForPostType();
            }
            // If the same button is clicked, do nothing
        });
    }

    // Updated confirmation dialog to handle the new type
    private void showSwitchConfirmationDialog(String previousType, String newType) {
        CustomAlertDialogFragment dialog = CustomAlertDialogFragment.newInstance(
                "Discard " + previousType + " data?",
                "Switching post type will discard the options you've entered.",
                "Switch Anyway",
                "Cancel"
        );
        dialog.setDialogListener(new CustomAlertDialogFragment.DialogListener() {
            @Override
            public void onPositiveClick() {
                clearPollOptions();
                currentPostType = newType; // Update state
                updateUiForPostType(); // Update UI
            }
            @Override
            public void onNegativeClick() {
                // Revert toggle button selection back to the previous type
                int buttonIdToReselect;
                switch(previousType) {
                    case Post.TYPE_POLL: buttonIdToReselect = R.id.button_type_poll; break;
                    case Post.TYPE_QUIZ: buttonIdToReselect = R.id.button_type_quiz; break;
                    default: buttonIdToReselect = R.id.button_type_media; break; // Should not happen here
                }
                binding.togglePostType.check(buttonIdToReselect);
                // No need to set currentPostType here, as the check() call might trigger the listener again
                // (Though addOnButtonCheckedListener should ideally handle isChecked correctly)
            }
        });
        dialog.show(getSupportFragmentManager(), "SwitchTypeConfirmation");
    }

    // --- UPDATED observeViewModel ---
    private void observeViewModel() {
        editPostViewModel.getUpdateStatus().observe(this, status -> {
            if (status == null) return;
            boolean isLoading = status == EditPostViewModel.UpdateStatus.UPDATING;
            binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            binding.buttonSavePost.setEnabled(!isLoading);
            binding.editTextContent.setEnabled(!isLoading);
            binding.buttonAttachMedia.setEnabled(!isLoading);
            binding.buttonAddOption.setEnabled(!isLoading && pollOptionLayouts.size() < MAX_POLL_OPTIONS);
            binding.togglePostType.setEnabled(!isLoading);

            // Disable poll inputs and remove buttons while loading
            for (int i = 0; i < pollOptionLayouts.size(); i++) {
                if(pollOptionEditTexts.get(i) != null) {
                    pollOptionEditTexts.get(i).setEnabled(!isLoading);
                }
                if (pollOptionLayouts.get(i) != null) {
                    if (i >= MIN_POLL_OPTIONS) { // Only disable remove for dynamic ones
                        pollOptionLayouts.get(i).setEnabled(!isLoading);
                    }
                }
            }
            // *** Disable duration radio buttons during upload ***
            binding.durationNone.setEnabled(!isLoading);
            binding.duration24h.setEnabled(!isLoading);
            binding.duration3d.setEnabled(!isLoading);
            // *** End Disable ***


            if (status == EditPostViewModel.UpdateStatus.SUCCESS) {
                Toast.makeText(this, "Post updated!", Toast.LENGTH_SHORT).show();
                finish(); // Close activity on success
            } else if (status == EditPostViewModel.UpdateStatus.FAILED) {
                // Error shown via errorMessage LiveData
            }
        });

        editPostViewModel.getErrorMessage().observe(this, error -> {
            if (error != null && !isFinishing()) { // Check isFinishing
                CustomAlertDialogFragment.newInstance("Update Failed", error, "OK", null)
                        .show(getSupportFragmentManager(), "UpdateErrorDialog");
                editPostViewModel.clearMessages();
            }
        });
    }
    // --- END UPDATE ---

    // --- UPDATED updateUiForPostType ---
    private void updateUiForPostType() {
        boolean isPollOrQuiz = currentPostType.equals(Post.TYPE_POLL) || currentPostType.equals(Post.TYPE_QUIZ);

        binding.pollCreationContainer.setVisibility(isPollOrQuiz ? View.VISIBLE : View.GONE);
        binding.mediaPreviewRecycler.setVisibility(
                currentPostType.equals(Post.TYPE_TEXT_MEDIA) && !mediaItems.isEmpty() ? View.VISIBLE : View.GONE
        );
        binding.buttonAttachMedia.setVisibility(
                currentPostType.equals(Post.TYPE_TEXT_MEDIA) ? View.VISIBLE : View.GONE
        );

        // Manage Poll/Quiz specific UI elements
        binding.pollDurationLabel.setVisibility(isPollOrQuiz ? View.VISIBLE : View.GONE); // *** Show/Hide Duration Label ***
        binding.pollDurationGroup.setVisibility(isPollOrQuiz ? View.VISIBLE : View.GONE); // *** Show/Hide Duration Group ***
        binding.quizAnswerLabel.setVisibility(currentPostType.equals(Post.TYPE_QUIZ) ? View.VISIBLE : View.GONE);
        binding.quizCorrectAnswerGroup.setVisibility(currentPostType.equals(Post.TYPE_QUIZ) ? View.VISIBLE : View.GONE);

        if (isPollOrQuiz) {
            binding.pollQuizLabel.setText(currentPostType.equals(Post.TYPE_POLL) ? "Edit Poll" : "Edit Quiz");
            if (currentPostType.equals(Post.TYPE_QUIZ)) {
                updateQuizRadioButtons(); // Ensure radio buttons are updated when type changes to Quiz
            }
        }
    }
    // --- END UPDATE ---


    private void openMediaPicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"image/*", "video/*"});
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        pickMediaLauncher.launch(Intent.createChooser(intent, "Select Media (Max 15)"));
    }

    // --- UPDATED savePostChanges ---
    private void savePostChanges() {
        String content = binding.editTextContent.getText().toString().trim();

        // Start with the original post data and modify it
        Post updatedPost = postToEdit; // Assume postToEdit is correctly loaded and non-null
        if (updatedPost == null) {
            Toast.makeText(this, "Error: Cannot save, original post data missing.", Toast.LENGTH_SHORT).show();
            return;
        }

        updatedPost.setPostType(currentPostType);
        updatedPost.setTextContent(content.isEmpty() ? null : content);

        List<PollOption> newPollOptions = new ArrayList<>();
        int quizCorrectIndex = -1;
        Integer pollDuration = null; // *** NEW: Duration variable ***


        if (currentPostType.equals(Post.TYPE_POLL) || currentPostType.equals(Post.TYPE_QUIZ)) {
            // Iterate through the managed EditTexts
            for (int i = 0; i < pollOptionEditTexts.size(); i++) {
                TextInputEditText editText = pollOptionEditTexts.get(i);
                TextInputLayout layout = pollOptionLayouts.get(i);
                if (editText == null || layout == null) continue;

                String optionText = editText.getText().toString().trim();
                if (!optionText.isEmpty()) {
                    // Try to find the original option by text to preserve its vote count
                    PollOption existingOption = findExistingPollOption(postToEdit.getPollOptions(), optionText);
                    // Create new PollOption, preserving vote count if found, otherwise default to 0
                    newPollOptions.add(new PollOption(optionText, existingOption != null ? existingOption.getVoteCount() : 0));
                    layout.setError(null); // Clear potential previous error
                } else if (i < MIN_POLL_OPTIONS) {
                    layout.setError("Option " + (i + 1) + " cannot be empty."); // Show error on layout
                    return; // Stop processing if required option is empty
                } else {
                    layout.setError(null); // Clear error for optional fields
                }
            }
            // Clear errors for initially required fields if they are now filled
            if (pollOptionLayouts.size() > 0 && pollOptionLayouts.get(0) != null) pollOptionLayouts.get(0).setError(null);
            if (pollOptionLayouts.size() > 1 && pollOptionLayouts.get(1) != null) pollOptionLayouts.get(1).setError(null);


            if (newPollOptions.size() < MIN_POLL_OPTIONS) {
                Toast.makeText(this, "A poll/quiz requires at least " + MIN_POLL_OPTIONS + " non-empty options.", Toast.LENGTH_SHORT).show();
                return;
            }
            updatedPost.setPollOptions(newPollOptions); // Set the updated list

            // *** Get selected poll duration ***
            int selectedDurationId = binding.pollDurationGroup.getCheckedRadioButtonId();
            if (selectedDurationId == R.id.duration_24h) {
                pollDuration = 24;
            } else if (selectedDurationId == R.id.duration_3d) {
                pollDuration = 72; // 3 days * 24 hours
            } // else it remains null (Permanent)
            updatedPost.setPollDurationHours(pollDuration);
            // *** End get duration ***

            if (currentPostType.equals(Post.TYPE_QUIZ)) {
                quizCorrectIndex = binding.quizCorrectAnswerGroup.getCheckedRadioButtonId(); // ID is the index
                // Validate quizCorrectIndex against the *new* options list size
                if (quizCorrectIndex == -1 || quizCorrectIndex >= newPollOptions.size()) {
                    Toast.makeText(this, "Please select a valid correct answer.", Toast.LENGTH_SHORT).show();
                    return;
                }
                updatedPost.setQuizCorrectOptionIndex(quizCorrectIndex);
            } else {
                updatedPost.setQuizCorrectOptionIndex(-1); // Reset index if it's now just a poll
            }
            // Recalculate total votes based on potentially preserved counts
            updatedPost.setTotalVotes(newPollOptions.stream().mapToInt(PollOption::getVoteCount).sum());

        } else {
            // Clear poll/quiz data if switching back to a media/text post
            updatedPost.setPollOptions(new ArrayList<>());
            updatedPost.setQuizCorrectOptionIndex(-1);
            updatedPost.setTotalVotes(0);
            updatedPost.setPollDurationHours(null); // *** Clear duration ***
        }

        // Final content validation
        if (content.isEmpty() && mediaItems.isEmpty() && newPollOptions.isEmpty()) {
            Toast.makeText(this, "Cannot save an empty post.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Pass the current state of mediaItems (mix of URIs and URLs) to the ViewModel
        editPostViewModel.updatePost(updatedPost, mediaItems, this);
    }
    // --- END UPDATE ---

    // Helper to find original poll option by text to preserve votes
    private PollOption findExistingPollOption(List<PollOption> existingOptions, String newText) {
        if (existingOptions == null || newText == null) return null;
        for (PollOption option : existingOptions) {
            // Use equalsIgnoreCase for more flexible matching if desired
            if (option.getOptionText() != null && option.getOptionText().equals(newText)) {
                return option;
            }
        }
        return null; // Not found
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            // TODO: Add check for unsaved changes before finishing
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null; // Clean up binding
    }
}