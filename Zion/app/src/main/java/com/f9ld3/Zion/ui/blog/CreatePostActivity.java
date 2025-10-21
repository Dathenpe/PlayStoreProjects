// main/java/com/f9ld3/Zion/ui/blog/CreatePostActivity.java
package com.f9ld3.Zion.ui.blog;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
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
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import com.f9ld3.Zion.R;
import com.f9ld3.Zion.databinding.ActivityCreatePostBinding;
import com.f9ld3.Zion.databinding.ItemPollOptionInputBinding; // Use a dedicated binding for poll options
import com.f9ld3.Zion.ui.dialogs.CustomAlertDialogFragment;
import com.f9ld3.Zion.ui.feed.PollOption;
import com.f9ld3.Zion.ui.feed.Post;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import java.util.ArrayList;
import java.util.List;

public class CreatePostActivity extends AppCompatActivity {

    private ActivityCreatePostBinding binding;
    private PostViewModel postViewModel;
    private MediaPreviewAdapter adapter;
    private final List<Uri> mediaUris = new ArrayList<>();
    private String currentPostType = Post.TYPE_TEXT_MEDIA;

    // Keep track of dynamically added poll option views/edittexts
    private final List<ItemPollOptionInputBinding> pollOptionBindings = new ArrayList<>();
    private final int MAX_POLL_OPTIONS = 5;
    private final int MIN_POLL_OPTIONS = 2;


    private final ActivityResultLauncher<Intent> pickMediaLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    int maxSelection = 15; // Define max selectable items
                    int currentCount = mediaUris.size();
                    int addedCount = 0;

                    if (result.getData().getClipData() != null) {
                        int count = result.getData().getClipData().getItemCount();
                        addedCount = Math.min(count, maxSelection - currentCount); // How many can we add?
                        if (count > addedCount) {
                            Toast.makeText(this, "Maximum of " + maxSelection + " files allowed.", Toast.LENGTH_SHORT).show();
                        }
                        for (int i = 0; i < addedCount; i++) {
                            mediaUris.add(result.getData().getClipData().getItemAt(i).getUri());
                        }
                    } else if (result.getData().getData() != null) {
                        if (currentCount < maxSelection) {
                            mediaUris.add(result.getData().getData());
                            addedCount = 1;
                        } else {
                            Toast.makeText(this, "Maximum of " + maxSelection + " files allowed.", Toast.LENGTH_SHORT).show();
                        }
                    }

                    if (addedCount > 0) {
                        adapter.notifyDataSetChanged();
                        binding.mediaPreviewRecycler.setVisibility(View.VISIBLE);
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCreatePostBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        postViewModel = new ViewModelProvider(this).get(PostViewModel.class);

        setupToolbar();
        setupRecyclerView();
        setupInitialPollOptions(); // Setup initial bindings
        setupClickListeners();
        observeViewModel();
        updateUiForPostType(); // Set initial UI state
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if(getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Create Post"); // Set a title
        }
    }

    private void setupRecyclerView() {
        adapter = new MediaPreviewAdapter(mediaUris);
        binding.mediaPreviewRecycler.setLayoutManager(new GridLayoutManager(this, 3));
        binding.mediaPreviewRecycler.setAdapter(adapter);
    }

    private void setupInitialPollOptions() {
        // --- START FIX ---
        // Manually create bindings for the initial two options from activity_create_post.xml
        ItemPollOptionInputBinding binding1 = ItemPollOptionInputBinding.bind(binding.layoutPollOption1);
        ItemPollOptionInputBinding binding2 = ItemPollOptionInputBinding.bind(binding.layoutPollOption2);

        // Clear existing list and add the correctly associated bindings
        pollOptionBindings.clear();
        pollOptionBindings.add(binding1);
        pollOptionBindings.add(binding2);

        // Add text watchers specifically using the correct EditText IDs from activity_create_post
        if (binding.inputPollOption1 != null) {
            binding.inputPollOption1.addTextChangedListener(quizOptionTextWatcher);
        }
        if (binding.inputPollOption2 != null) {
            binding.inputPollOption2.addTextChangedListener(quizOptionTextWatcher);
        }
        // --- END FIX ---

        // Ensure remove icons (end icons) for initial options are hidden
        // (This relies on the updated updatePollOptionHints logic)
        updatePollOptionHints();

        // Set initial hints correctly (this call remains)
        updatePollOptionHints();
    }


    private void setupClickListeners() {
        binding.buttonPublish.setOnClickListener(v -> publishPost());
        binding.buttonAttachMedia.setOnClickListener(v -> openMediaPicker());
        binding.buttonAddOption.setOnClickListener(v -> addPollOptionInput());

        binding.togglePostType.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;

            String previousType = currentPostType;
            if (checkedId == R.id.button_type_media) {
                currentPostType = Post.TYPE_TEXT_MEDIA;
            } else if (checkedId == R.id.button_type_poll) {
                currentPostType = Post.TYPE_POLL;
            } else if (checkedId == R.id.button_type_quiz) {
                currentPostType = Post.TYPE_QUIZ;
            }

            // Ask for confirmation if switching away from Poll/Quiz with entered options
            if (!currentPostType.equals(previousType) && (previousType.equals(Post.TYPE_POLL) || previousType.equals(Post.TYPE_QUIZ))) {
                boolean hasPollData = false;
                // Check initial fields directly
                if (binding.inputPollOption1.getText().length() > 0 || binding.inputPollOption2.getText().length() > 0) {
                    hasPollData = true;
                } else {
                    // Check dynamically added fields
                    for (int i = MIN_POLL_OPTIONS; i < pollOptionBindings.size(); i++) { // Start checking from index 2
                        ItemPollOptionInputBinding itemBinding = pollOptionBindings.get(i);
                        if (itemBinding.inputPollOption != null && itemBinding.inputPollOption.getText().length() > 0) {
                            hasPollData = true;
                            break;
                        }
                    }
                }

                if (hasPollData) {
                    showSwitchConfirmationDialog(previousType);
                } else {
                    updateUiForPostType(); // Switch directly if no data
                }
            } else {
                updateUiForPostType(); // Update UI for the selected type
            }
        });
    }

    private void showSwitchConfirmationDialog(String previousType) {
        CustomAlertDialogFragment dialog = CustomAlertDialogFragment.newInstance(
                "Discard " + previousType + "?",
                "Switching post type will discard the options you've entered.",
                "Switch Anyway",
                "Cancel"
        );
        dialog.setDialogListener(new CustomAlertDialogFragment.DialogListener() {
            @Override
            public void onPositiveClick() {
                clearPollOptions(); // Clear data before switching UI
                updateUiForPostType();
            }

            @Override
            public void onNegativeClick() {
                // Revert toggle button selection
                int buttonIdToReselect = R.id.button_type_media; // Default
                if (previousType.equals(Post.TYPE_POLL)) buttonIdToReselect = R.id.button_type_poll;
                else if (previousType.equals(Post.TYPE_QUIZ)) buttonIdToReselect = R.id.button_type_quiz;
                binding.togglePostType.check(buttonIdToReselect);
                // Set currentPostType back
                currentPostType = previousType;
            }
        });
        dialog.show(getSupportFragmentManager(), "SwitchTypeConfirmation");
    }

    private void clearPollOptions() {
        // Clear text from initial options
        if (binding.inputPollOption1 != null) binding.inputPollOption1.setText("");
        if (binding.inputPollOption2 != null) binding.inputPollOption2.setText("");

        // Remove dynamically added views
        binding.additionalOptionsContainer.removeAllViews();
        // Clear the list, keeping only the initial two bindings (correct logic)
        if (pollOptionBindings.size() > MIN_POLL_OPTIONS) {
            pollOptionBindings.subList(MIN_POLL_OPTIONS, pollOptionBindings.size()).clear();
        }
        binding.quizCorrectAnswerGroup.clearCheck(); // Clear quiz selection
        binding.buttonAddOption.setEnabled(true); // Re-enable add button
        updatePollOptionHints(); // Reset hints
        updateQuizRadioButtons(); // Clear radio buttons
    }


    private void observeViewModel() {
        postViewModel.getUploadStatus().observe(this, status -> {
            if (status == null) return;
            boolean isLoading = status == PostViewModel.UploadStatus.UPLOADING;
            binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            binding.progressBar.setIndeterminate(isLoading); // Show indeterminate progress
            binding.buttonPublish.setEnabled(!isLoading);
            binding.editTextContent.setEnabled(!isLoading); // Disable input while uploading
            // Disable other inputs as well (media button, poll options)
            binding.buttonAttachMedia.setEnabled(!isLoading);
            binding.buttonAddOption.setEnabled(!isLoading);
            for (ItemPollOptionInputBinding itemBinding : pollOptionBindings) {
                if (itemBinding.inputPollOption != null) {
                    itemBinding.inputPollOption.setEnabled(!isLoading);
                }
                // --- FIX: Remove the line causing the error ---
                // The root's clickability (specifically the end icon) is handled by its listener
                // itemBinding.getRoot().setEndIconClickable(!isLoading); // REMOVED THIS LINE
                // --- END FIX ---
            }
            binding.togglePostType.setEnabled(!isLoading);


            if (status == PostViewModel.UploadStatus.SUCCESS) {
                Toast.makeText(this, "Post published!", Toast.LENGTH_SHORT).show();
                finish();
            } else if (status == PostViewModel.UploadStatus.FAILED) {
                // Error message is shown via the _errorMessage LiveData
            }
        });

        postViewModel.getErrorMessage().observe(this, error -> {
            if (error != null) {
                // Show a more prominent error dialog
                CustomAlertDialogFragment.newInstance("Upload Failed", error, "OK", null)
                        .show(getSupportFragmentManager(), "UploadErrorDialog");
                postViewModel.clearMessages(); // Clear error after showing
            }
        });
    }

    private void updateUiForPostType() {
        binding.pollCreationContainer.setVisibility(
                currentPostType.equals(Post.TYPE_POLL) || currentPostType.equals(Post.TYPE_QUIZ) ? View.VISIBLE : View.GONE
        );
        binding.mediaPreviewRecycler.setVisibility(
                currentPostType.equals(Post.TYPE_TEXT_MEDIA) && !mediaUris.isEmpty() ? View.VISIBLE : View.GONE
        );
        binding.buttonAttachMedia.setVisibility(
                currentPostType.equals(Post.TYPE_TEXT_MEDIA) ? View.VISIBLE : View.GONE
        );

        // Poll/Quiz Specific UI
        if (currentPostType.equals(Post.TYPE_POLL) || currentPostType.equals(Post.TYPE_QUIZ)) {
            binding.pollQuizLabel.setText(currentPostType.equals(Post.TYPE_POLL) ? "Create Poll" : "Create Quiz");
            binding.quizAnswerLabel.setVisibility(currentPostType.equals(Post.TYPE_QUIZ) ? View.VISIBLE : View.GONE);
            binding.quizCorrectAnswerGroup.setVisibility(currentPostType.equals(Post.TYPE_QUIZ) ? View.VISIBLE : View.GONE);
            if (currentPostType.equals(Post.TYPE_QUIZ)) {
                updateQuizRadioButtons(); // Ensure radio buttons are updated
            }
        }
    }


    private void openMediaPicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*"); // Allow selection of both images and videos
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"image/*", "video/*"});
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);

        pickMediaLauncher.launch(Intent.createChooser(intent, "Select Media (Max 15)"));
    }


    private void addPollOptionInput() {
        if (pollOptionBindings.size() >= MAX_POLL_OPTIONS) {
            Toast.makeText(this, "Maximum of " + MAX_POLL_OPTIONS + " options.", Toast.LENGTH_SHORT).show();
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(this);
        // Use the specific binding for the poll option item layout
        ItemPollOptionInputBinding newOptionBinding = ItemPollOptionInputBinding.inflate(inflater, binding.additionalOptionsContainer, true); // Inflate and attach

        // Set the click listener on the TextInputLayout's end icon
        newOptionBinding.getRoot().setEndIconOnClickListener(v -> removePollOptionInput(newOptionBinding));
        // Visibility is handled by updatePollOptionHints

        if (newOptionBinding.inputPollOption != null) {
            newOptionBinding.inputPollOption.addTextChangedListener(quizOptionTextWatcher);
        }

        pollOptionBindings.add(newOptionBinding); // Add the binding to the list
        updatePollOptionHints(); // This will now control the end icon visibility
        updateQuizRadioButtons(); // Update quiz options if applicable

        // Disable add button if max reached
        binding.buttonAddOption.setEnabled(pollOptionBindings.size() < MAX_POLL_OPTIONS);
    }

    private void removePollOptionInput(ItemPollOptionInputBinding bindingToRemove) {
        if (pollOptionBindings.size() <= MIN_POLL_OPTIONS) {
            // Should not happen if remove icon is hidden, but added as safety
            return;
        }
        // Remove the view from the layout
        binding.additionalOptionsContainer.removeView(bindingToRemove.getRoot());
        // Remove the binding from the list
        pollOptionBindings.remove(bindingToRemove);

        // Re-enable add button if below max
        binding.buttonAddOption.setEnabled(true);

        // Update hints and quiz options
        updatePollOptionHints();
        updateQuizRadioButtons();
    }

    private void updatePollOptionHints() {
        for (int i = 0; i < pollOptionBindings.size(); i++) {
            TextInputLayout layout = pollOptionBindings.get(i).getRoot();
            boolean isRequired = i < MIN_POLL_OPTIONS;
            layout.setHint("Option " + (i + 1) + (isRequired ? " (Required)" : ""));

            // Control the visibility of the end icon (remove icon)
            boolean showRemoveIcon = i >= MIN_POLL_OPTIONS;
            layout.setEndIconVisible(showRemoveIcon);
        }
    }

    private final TextWatcher quizOptionTextWatcher = new TextWatcher() {
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
        @Override public void afterTextChanged(Editable s) {
            if (currentPostType.equals(Post.TYPE_QUIZ)) {
                updateQuizRadioButtons();
            }
        }
    };


    private void updateQuizRadioButtons() {
        binding.quizCorrectAnswerGroup.removeAllViews(); // Clear existing buttons
        if (!currentPostType.equals(Post.TYPE_QUIZ)) {
            return; // Only proceed if it's a quiz
        }

        int currentSelection = binding.quizCorrectAnswerGroup.getCheckedRadioButtonId();

        for (int i = 0; i < pollOptionBindings.size(); i++) {
            TextInputEditText editText;
            // Access initial EditTexts via main binding
            if (i == 0) {
                editText = binding.inputPollOption1;
            } else if (i == 1) {
                editText = binding.inputPollOption2;
            } else {
                // Check if the binding and its EditText are valid before accessing
                if (i < pollOptionBindings.size() && pollOptionBindings.get(i) != null) {
                    editText = pollOptionBindings.get(i).inputPollOption;
                } else {
                    continue; // Skip if binding or EditText is somehow null
                }
            }

            if (editText == null) continue; // Safety check

            String optionText = editText.getText().toString().trim();
            if (!optionText.isEmpty()) {
                RadioButton radioButton = new RadioButton(this);
                radioButton.setId(i); // Use index as ID
                radioButton.setText(optionText); // Show the actual option text
                radioButton.setLayoutParams(new RadioGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                binding.quizCorrectAnswerGroup.addView(radioButton);

                // Restore selection if ID matches
                if (i == currentSelection) {
                    radioButton.setChecked(true);
                }
            }
        }
    }

    private void publishPost() {
        String content = binding.editTextContent.getText().toString().trim();

        Post post = new Post();
        post.setPostType(currentPostType);
        // Only set text content if it's not empty
        if (!content.isEmpty()) {
            post.setTextContent(content);
        }

        List<PollOption> pollOptions = new ArrayList<>();
        int quizCorrectIndex = -1;

        if (currentPostType.equals(Post.TYPE_POLL) || currentPostType.equals(Post.TYPE_QUIZ)) {
            for (int i = 0; i < pollOptionBindings.size(); i++) { // Iterate using index
                TextInputEditText editText;
                // Access initial EditTexts via main binding
                if (i == 0) {
                    editText = binding.inputPollOption1;
                } else if (i == 1) {
                    editText = binding.inputPollOption2;
                } else {
                    // Check if the binding and its EditText are valid before accessing
                    if (i < pollOptionBindings.size() && pollOptionBindings.get(i) != null) {
                        editText = pollOptionBindings.get(i).inputPollOption;
                    } else {
                        continue; // Skip if binding or EditText is somehow null
                    }
                }

                if (editText == null) continue; // Safety check

                String optionText = editText.getText().toString().trim();
                if (!optionText.isEmpty()) {
                    pollOptions.add(new PollOption(optionText));
                } else if (i < MIN_POLL_OPTIONS) { // Check required options
                    Toast.makeText(this, "Option " + (i + 1) + " cannot be empty.", Toast.LENGTH_SHORT).show();
                    return; // Stop publishing
                }
            }

            if (pollOptions.size() < MIN_POLL_OPTIONS) {
                Toast.makeText(this, "A poll or quiz requires at least " + MIN_POLL_OPTIONS + " non-empty options.", Toast.LENGTH_SHORT).show();
                return;
            }

            post.setPollOptions(pollOptions);

            if (currentPostType.equals(Post.TYPE_QUIZ)) {
                quizCorrectIndex = binding.quizCorrectAnswerGroup.getCheckedRadioButtonId(); // This ID is the index
                // Validate quizCorrectIndex against the *actual* number of non-empty options added
                if (quizCorrectIndex == -1 || quizCorrectIndex >= pollOptions.size()) {
                    Toast.makeText(this, "Please select a valid correct answer for the quiz.", Toast.LENGTH_SHORT).show();
                    return;
                }
                post.setQuizCorrectOptionIndex(quizCorrectIndex);
            }
        }

        // Final validation: Cannot publish completely empty post
        if (content.isEmpty() && mediaUris.isEmpty() && pollOptions.isEmpty()) {
            Toast.makeText(this, "Cannot publish an empty post.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validation passed, start upload/save process
        postViewModel.createPost(post, mediaUris, this);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            // Add confirmation if content has been added
            // Simple finish for now
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