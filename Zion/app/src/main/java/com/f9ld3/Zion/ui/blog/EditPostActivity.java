// main/java/com/f9ld3/Zion/ui/blog/EditPostActivity.java
package com.f9ld3.Zion.ui.blog;

import static com.f9ld3.Zion.ui.feed.CommentsBottomSheet.TAG;

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
import android.widget.ImageButton; // Import ImageButton
import android.widget.ImageView; // Import ImageView
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;

import com.bumptech.glide.Glide; // Import Glide
import com.f9ld3.Zion.R;
import com.f9ld3.Zion.databinding.ActivityEditPostBinding;
import com.f9ld3.Zion.ui.dialogs.CustomAlertDialogFragment;
import com.f9ld3.Zion.ui.feed.MediaItem;
import com.f9ld3.Zion.ui.feed.PollOption;
import com.f9ld3.Zion.ui.feed.Post;
import com.google.android.material.imageview.ShapeableImageView; // Import ShapeableImageView
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap; // Import HashMap
import java.util.List;
import java.util.Map; // Import Map
import java.util.Objects;
import java.util.stream.Collectors;

public class EditPostActivity extends AppCompatActivity implements MediaPreviewAdapter.OnMediaItemClickListener {

    public static final String EXTRA_POST_TO_EDIT = "extra_post_to_edit";
    // private static final String TAG = "EditPostActivity"; // Reusing TAG from CommentsBottomSheet

    private ActivityEditPostBinding binding;
    private EditPostViewModel editPostViewModel;
    private MediaPreviewAdapter adapter;
    private final List<MediaItem> mediaItems = new ArrayList<>();
    private String currentPostType = Post.TYPE_TEXT_MEDIA;
    private Post postToEdit;

    // --- Poll Option Management Logic ---
    private final List<TextInputLayout> pollOptionLayouts = new ArrayList<>();
    private final List<TextInputEditText> pollOptionEditTexts = new ArrayList<>();
    // *** NEW: Store Views for Image Handling ***
    private final List<ImageView> pollOptionImageViews = new ArrayList<>();
    private final List<ImageButton> pollOptionImageButtons = new ArrayList<>();
    // Map stores local URIs (if changed) or existing URLs (if unchanged)
    private final Map<Integer, Object> pollOptionImageData = new HashMap<>(); // Key: Index, Value: Uri (new) or String (URL old)
    private int currentImagePickerIndex = -1; // Track which option's image is being picked
    // *** END NEW ***
    private final int MAX_POLL_OPTIONS = 5;
    private final int MIN_POLL_OPTIONS = 2;


    // Media Picker Launcher (for post media)
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


    // *** NEW: Image Picker Launcher for Poll Options ***
    private final ActivityResultLauncher<Intent> pickPollOptionImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null && currentImagePickerIndex != -1) {
                    Uri selectedUri = result.getData().getData();
                    if (selectedUri != null && currentImagePickerIndex < pollOptionImageViews.size()) {
                        pollOptionImageData.put(currentImagePickerIndex, selectedUri); // Store URI (marks as changed)
                        ImageView preview = pollOptionImageViews.get(currentImagePickerIndex);
                        if (preview != null) {
                            Glide.with(this).load(selectedUri).into(preview);
                            preview.setVisibility(View.VISIBLE);
                            Log.d(TAG, "Image selected for option " + currentImagePickerIndex + ": " + selectedUri);
                        } else {
                            Log.e(TAG, "ImageView is null for index: " + currentImagePickerIndex);
                        }
                    } else {
                        Log.e(TAG, "Selected URI is null or index out of bounds: " + currentImagePickerIndex);
                    }
                } else {
                    Log.d(TAG, "Image selection cancelled or failed for index: " + currentImagePickerIndex);
                }
                currentImagePickerIndex = -1; // Reset index
            });
    // *** END NEW ***


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

        // --- NEW: Handle Back Press ---
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                checkForUnsavedChanges();
            }
        });
        // --- END NEW ---
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
                        PollOption option = (i < options.size()) ? options.get(i) : null;
                        pollOptionEditTexts.get(i).setText(option != null ? option.getOptionText() : "");
                        // *** NEW: Populate Image ***
                        ImageView imageView = pollOptionImageViews.get(i);
                        if (imageView != null && option != null && option.getImageUrl() != null && !option.getImageUrl().isEmpty()) {
                            Glide.with(this).load(option.getImageUrl()).into(imageView);
                            imageView.setVisibility(View.VISIBLE);
                            pollOptionImageData.put(i, option.getImageUrl()); // Store existing URL
                        } else if (imageView != null) {
                            imageView.setVisibility(View.GONE);
                            pollOptionImageData.remove(i);
                        }
                        // *** END NEW ***
                    } else {
                        Log.e(TAG, "Missing EditText/ImageView for initial poll option index: " + i);
                    }
                }
                // Add and populate the remaining options dynamically
                for (int i = MIN_POLL_OPTIONS; i < options.size(); i++) {
                    addPollOptionInputView(options.get(i)); // Pass the whole PollOption
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

    // --- Poll Option Logic ---
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
            addPollOptionInputView(null); // Add empty views initially
        }
        updatePollOptionHintsAndRemoveButtons();
    }

    // *** MODIFIED: Accept PollOption to pre-fill image ***
    private void addPollOptionInputView(@Nullable PollOption initialOption) {
        if (pollOptionLayouts.size() >= MAX_POLL_OPTIONS) return;

        LayoutInflater inflater = LayoutInflater.from(this);
        // 1. Inflate the root view (ConstraintLayout)
        View inflatedView = inflater.inflate(R.layout.item_poll_option_input, binding.pollOptionsContainer, false);

        // 2. Find the TextInputLayout *within* the inflated view by ID
        TextInputLayout newLayout = inflatedView.findViewById(R.id.poll_option_input_layout); // <-- Find by ID

        // 3. Find other views *within* the inflated view
        TextInputEditText newEditText = inflatedView.findViewById(R.id.input_poll_option);
        ImageView newImageView = inflatedView.findViewById(R.id.image_poll_option_preview);
        ImageButton newImageButton = inflatedView.findViewById(R.id.button_add_poll_option_image);

        // Check if all necessary views were found
        if (newLayout != null && newEditText != null && newImageView != null && newImageButton != null) {
            final int currentIndex = pollOptionLayouts.size(); // Capture index

            // Populate text and image if initialOption is provided
            if (initialOption != null) {
                newEditText.setText(initialOption.getOptionText());
                if (initialOption.getImageUrl() != null && !initialOption.getImageUrl().isEmpty()) {
                    Glide.with(this).load(initialOption.getImageUrl()).into(newImageView);
                    newImageView.setVisibility(View.VISIBLE);
                    pollOptionImageData.put(currentIndex, initialOption.getImageUrl()); // Store existing URL
                } else {
                    newImageView.setVisibility(View.GONE);
                }
            }

            newEditText.addTextChangedListener(quizOptionTextWatcher);
            // Use the found TextInputLayout (newLayout) for the click listener
            newLayout.setEndIconOnClickListener(v -> removePollOptionInputView(newLayout));
            newImageButton.setOnClickListener(v -> {
                currentImagePickerIndex = currentIndex;
                openPollOptionImagePicker();
            });

            // Add the entire inflated view (the ConstraintLayout) to the container
            binding.pollOptionsContainer.addView(inflatedView);

            // Keep track of the actual TextInputLayout and other views
            pollOptionLayouts.add(newLayout);
            pollOptionEditTexts.add(newEditText);
            pollOptionImageViews.add(newImageView);
            pollOptionImageButtons.add(newImageButton);

            updatePollOptionHintsAndRemoveButtons();
            updateQuizRadioButtons();
            binding.buttonAddOption.setEnabled(pollOptionLayouts.size() < MAX_POLL_OPTIONS);

        } else {
            Log.e(TAG, "Could not find required views in inflated poll option layout.");
        }
    }
    // *** END MODIFIED ***
    private void removePollOptionInputView(TextInputLayout layoutToRemove) {
        if (pollOptionLayouts.size() <= MIN_POLL_OPTIONS) return;

        int indexToRemove = pollOptionLayouts.indexOf(layoutToRemove);
        if (indexToRemove != -1) {
            binding.pollOptionsContainer.removeView(layoutToRemove);
            pollOptionLayouts.remove(indexToRemove);
            if (indexToRemove < pollOptionEditTexts.size()) pollOptionEditTexts.remove(indexToRemove);
            if (indexToRemove < pollOptionImageViews.size()) pollOptionImageViews.remove(indexToRemove);
            if (indexToRemove < pollOptionImageButtons.size()) pollOptionImageButtons.remove(indexToRemove);
            pollOptionImageData.remove(indexToRemove); // Remove data

            // Adjust indices in the map
            Map<Integer, Object> adjustedData = new HashMap<>();
            for (Map.Entry<Integer, Object> entry : pollOptionImageData.entrySet()) {
                if (entry.getKey() > indexToRemove) {
                    adjustedData.put(entry.getKey() - 1, entry.getValue());
                } else {
                    adjustedData.put(entry.getKey(), entry.getValue());
                }
            }
            pollOptionImageData.clear();
            pollOptionImageData.putAll(adjustedData);

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
            Object imageData = pollOptionImageData.get(i); // Check if there's image data
            // Add radio button if there's text OR image data
            if (!optionText.isEmpty() || imageData != null) {
                RadioButton radioButton = new RadioButton(this);
                radioButton.setId(i);
                radioButton.setText(optionText.isEmpty() ? "Image Option " + (i+1) : optionText);
                radioButton.setLayoutParams(new RadioGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                binding.quizCorrectAnswerGroup.addView(radioButton);
                if (i == currentSelection) radioButton.setChecked(true);
            }
        }
    }


    private void clearPollOptions() {
        while (pollOptionLayouts.size() > MIN_POLL_OPTIONS) {
            removePollOptionInputView(pollOptionLayouts.get(pollOptionLayouts.size() - 1));
        }
        for (TextInputEditText editText : pollOptionEditTexts) {
            if (editText != null) editText.setText("");
        }
        for (ImageView imageView : pollOptionImageViews) {
            if (imageView != null) {
                imageView.setImageDrawable(null);
                imageView.setVisibility(View.GONE);
            }
        }
        pollOptionImageData.clear(); // Clear image data map
        binding.quizCorrectAnswerGroup.clearCheck();
        binding.pollDurationGroup.check(R.id.duration_none); // *** Reset duration ***
        updatePollOptionHintsAndRemoveButtons();
        updateQuizRadioButtons();
        binding.buttonAddOption.setEnabled(true);
    }


    // *** NEW: Method to open Image Picker for Poll Option ***
    private void openPollOptionImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        pickPollOptionImageLauncher.launch(intent);
    }
    // *** END NEW ***

    private void setupClickListeners() {
        binding.buttonSavePost.setOnClickListener(v -> savePostChanges());
        binding.buttonAttachMedia.setOnClickListener(v -> openMediaPicker());
        // *** MODIFIED: Pass null for initial option data ***
        binding.buttonAddOption.setOnClickListener(v -> addPollOptionInputView(null));
        // *** END MODIFIED ***

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
                // *** NEW: Check image data too ***
                if (!hasPollData && !pollOptionImageData.isEmpty()) {
                    hasPollData = true;
                }
                // *** END NEW ***

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
        });
    }

    // Updated confirmation dialog to handle the new type
    private void showSwitchConfirmationDialog(String previousType, String newType) {
        CustomAlertDialogFragment dialog = CustomAlertDialogFragment.newInstance(
                "Discard " + previousType + " data?",
                "Switching post type will discard the options and images you've entered.", // Updated message
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
            }
        });
        dialog.show(getSupportFragmentManager(), "SwitchTypeConfirmation");
    }

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

            // Disable poll inputs, remove buttons, and image buttons while loading
            for (int i = 0; i < pollOptionLayouts.size(); i++) {
                if(i < pollOptionEditTexts.size() && pollOptionEditTexts.get(i) != null) {
                    pollOptionEditTexts.get(i).setEnabled(!isLoading);
                }
                // *** NEW: Disable image button ***
                if(i < pollOptionImageButtons.size() && pollOptionImageButtons.get(i) != null) {
                    pollOptionImageButtons.get(i).setEnabled(!isLoading);
                }
                // *** END NEW ***
                if (pollOptionLayouts.get(i) != null) {
                    if (i >= MIN_POLL_OPTIONS) { // Only disable remove for dynamic ones
                        pollOptionLayouts.get(i).setEnabled(!isLoading);
                    }
                }
            }
            binding.durationNone.setEnabled(!isLoading);
            binding.duration24h.setEnabled(!isLoading);
            binding.duration3d.setEnabled(!isLoading);


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

    private void updateUiForPostType() {
        boolean isPollOrQuiz = currentPostType.equals(Post.TYPE_POLL) || currentPostType.equals(Post.TYPE_QUIZ);

        binding.pollCreationContainer.setVisibility(isPollOrQuiz ? View.VISIBLE : View.GONE);
        binding.mediaPreviewRecycler.setVisibility(
                currentPostType.equals(Post.TYPE_TEXT_MEDIA) && !mediaItems.isEmpty() ? View.VISIBLE : View.GONE
        );
        binding.buttonAttachMedia.setVisibility(
                currentPostType.equals(Post.TYPE_TEXT_MEDIA) ? View.VISIBLE : View.GONE
        );

        binding.pollDurationLabel.setVisibility(isPollOrQuiz ? View.VISIBLE : View.GONE);
        binding.pollDurationGroup.setVisibility(isPollOrQuiz ? View.VISIBLE : View.GONE);
        binding.quizAnswerLabel.setVisibility(currentPostType.equals(Post.TYPE_QUIZ) ? View.VISIBLE : View.GONE);
        binding.quizCorrectAnswerGroup.setVisibility(currentPostType.equals(Post.TYPE_QUIZ) ? View.VISIBLE : View.GONE);

        if (isPollOrQuiz) {
            binding.pollQuizLabel.setText(currentPostType.equals(Post.TYPE_POLL) ? "Edit Poll" : "Edit Quiz");
            if (currentPostType.equals(Post.TYPE_QUIZ)) {
                updateQuizRadioButtons(); // Ensure radio buttons are updated when type changes to Quiz
            }
        }
    }


    private void openMediaPicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"image/*", "video/*"});
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        pickMediaLauncher.launch(Intent.createChooser(intent, "Select Media (Max 15)"));
    }

    private void savePostChanges() {
        String content = binding.editTextContent.getText().toString().trim();

        Post updatedPost = postToEdit;
        if (updatedPost == null) {
            Toast.makeText(this, "Error: Cannot save, original post data missing.", Toast.LENGTH_SHORT).show();
            return;
        }

        updatedPost.setPostType(currentPostType);
        updatedPost.setTextContent(content.isEmpty() ? null : content);

        List<PollOption> newPollOptions = new ArrayList<>();
        int quizCorrectIndex = -1;
        Integer pollDuration = null;
        // *** NEW: Map to pass image data (URIs or URLs) to ViewModel ***
        Map<Integer, Object> finalOptionImageData = new HashMap<>();
        // *** END NEW ***


        if (currentPostType.equals(Post.TYPE_POLL) || currentPostType.equals(Post.TYPE_QUIZ)) {
            for (int i = 0; i < pollOptionEditTexts.size(); i++) {
                TextInputEditText editText = pollOptionEditTexts.get(i);
                TextInputLayout layout = pollOptionLayouts.get(i);
                if (editText == null || layout == null) continue;

                String optionText = editText.getText().toString().trim();
                Object imageData = pollOptionImageData.get(i); // Get Uri or String URL

                // An option is valid if it has text OR an image
                if (!optionText.isEmpty() || imageData != null) {
                    // Try to find the original option by text or OLD image URL to preserve its vote count
                    PollOption existingOption = findExistingPollOption(postToEdit.getPollOptions(), optionText, (imageData instanceof String) ? (String)imageData : null);
                    int voteCount = existingOption != null ? existingOption.getVoteCount() : 0;

                    PollOption newOption = new PollOption(optionText, voteCount); // Create option (URL added later by VM)
                    // If image data exists and is a URL, set it directly (won't be re-uploaded)
                    if (imageData instanceof String) {
                        newOption.setImageUrl((String) imageData);
                    }
                    newPollOptions.add(newOption);

                    if (imageData != null) {
                        finalOptionImageData.put(newPollOptions.size() - 1, imageData); // Map data to final index
                    }
                    layout.setError(null);
                } else if (i < MIN_POLL_OPTIONS) {
                    layout.setError("Option " + (i + 1) + " requires text or an image.");
                    return;
                } else {
                    layout.setError(null);
                }
            }
            if (pollOptionLayouts.size() > 0 && pollOptionLayouts.get(0) != null) pollOptionLayouts.get(0).setError(null);
            if (pollOptionLayouts.size() > 1 && pollOptionLayouts.get(1) != null) pollOptionLayouts.get(1).setError(null);


            if (newPollOptions.size() < MIN_POLL_OPTIONS) {
                Toast.makeText(this, "A poll/quiz requires at least " + MIN_POLL_OPTIONS + " non-empty options.", Toast.LENGTH_SHORT).show();
                return;
            }
            updatedPost.setPollOptions(newPollOptions); // Set the updated list (might have URLs already)

            // Get selected poll duration
            int selectedDurationId = binding.pollDurationGroup.getCheckedRadioButtonId();
            if (selectedDurationId == R.id.duration_24h) pollDuration = 24;
            else if (selectedDurationId == R.id.duration_3d) pollDuration = 72;
            updatedPost.setPollDurationHours(pollDuration);

            if (currentPostType.equals(Post.TYPE_QUIZ)) {
                quizCorrectIndex = binding.quizCorrectAnswerGroup.getCheckedRadioButtonId();
                if (quizCorrectIndex == -1 || quizCorrectIndex >= newPollOptions.size()) {
                    Toast.makeText(this, "Please select a valid correct answer.", Toast.LENGTH_SHORT).show();
                    return;
                }
                updatedPost.setQuizCorrectOptionIndex(quizCorrectIndex);
            } else {
                updatedPost.setQuizCorrectOptionIndex(-1);
            }
            // Recalculate total votes based on potentially preserved counts
            updatedPost.setTotalVotes(newPollOptions.stream().mapToInt(PollOption::getVoteCount).sum());

        } else {
            // Clear poll/quiz data if switching back to a media/text post
            updatedPost.setPollOptions(new ArrayList<>());
            updatedPost.setQuizCorrectOptionIndex(-1);
            updatedPost.setTotalVotes(0);
            updatedPost.setPollDurationHours(null);
        }

        // Final content validation
        if (content.isEmpty() && mediaItems.isEmpty() && newPollOptions.isEmpty()) {
            Toast.makeText(this, "Cannot save an empty post.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Pass the current state of mediaItems and poll image data to the ViewModel
        // *** UPDATED: Pass finalOptionImageData to ViewModel ***
        editPostViewModel.updatePost(updatedPost, mediaItems, finalOptionImageData, this);
    }

    // *** MODIFIED: Check image URL as well ***
    private PollOption findExistingPollOption(List<PollOption> existingOptions, String newText, @Nullable String existingImageUrl) {
        if (existingOptions == null) return null;
        for (PollOption option : existingOptions) {
            boolean textMatches = (newText != null && !newText.isEmpty() && Objects.equals(option.getOptionText(), newText));
            boolean imageMatches = (existingImageUrl != null && Objects.equals(option.getImageUrl(), existingImageUrl));

            // Prefer matching by text if available
            if (textMatches) return option;
            // If text doesn't match (or new text is empty), try matching by existing image URL
            if (imageMatches && (newText == null || newText.isEmpty())) return option;
        }
        return null; // Not found
    }
    // *** END MODIFIED ***


    // --- Unsaved Changes Logic ---
    private void checkForUnsavedChanges() {
        if (hasUnsavedChanges()) {
            showUnsavedChangesDialog();
        } else {
            finish(); // No changes, just close
        }
    }

    private boolean hasUnsavedChanges() {
        if (postToEdit == null || editPostViewModel == null) return false;

        // 1. Check Text Content
        String newText = binding.editTextContent.getText().toString().trim();
        String oldText = postToEdit.getTextContent() != null ? postToEdit.getTextContent().trim() : "";
        if (!newText.equals(oldText)) return true;

        // 2. Check Post Type
        if (!currentPostType.equals(postToEdit.getPostType())) return true;

        // 3. Check Media (if it's a media post and changed)
        if (currentPostType.equals(Post.TYPE_TEXT_MEDIA) && editPostViewModel.getMediaChanged()) return true;

        // 4. Check Poll/Quiz (if it's that type)
        if (currentPostType.equals(Post.TYPE_POLL) || currentPostType.equals(Post.TYPE_QUIZ)) {
            // Check options text and images
            List<PollOption> oldOptions = postToEdit.getPollOptions() != null ? postToEdit.getPollOptions() : new ArrayList<>();
            int currentOptionCount = 0;
            for(int i=0; i<pollOptionEditTexts.size(); i++) {
                String currentText = pollOptionEditTexts.get(i).getText().toString().trim();
                Object currentImageData = pollOptionImageData.get(i);
                if (!currentText.isEmpty() || currentImageData != null) {
                    currentOptionCount++;
                    if (i >= oldOptions.size()) return true; // Added option
                    PollOption oldOption = oldOptions.get(i);
                    // Check text difference
                    if (!Objects.equals(currentText, oldOption.getOptionText())) return true;
                    // Check image difference (new URI vs old URL, or removed)
                    String oldImageUrl = oldOption.getImageUrl();
                    if (currentImageData instanceof Uri) return true; // New image selected
                    if (currentImageData == null && oldImageUrl != null) return true; // Image removed
                    if (currentImageData instanceof String && !Objects.equals(currentImageData, oldImageUrl)) return true; // URL somehow changed? (Shouldn't happen)
                }
            }
            if (currentOptionCount != oldOptions.size()) return true; // Option removed

            // Check duration
            Integer newDuration = null;
            int selectedDurationId = binding.pollDurationGroup.getCheckedRadioButtonId();
            if (selectedDurationId == R.id.duration_24h) newDuration = 24;
            else if (selectedDurationId == R.id.duration_3d) newDuration = 72;
            if (!Objects.equals(newDuration, postToEdit.getPollDurationHours())) return true;

            // Check correct index
            if (currentPostType.equals(Post.TYPE_QUIZ)) {
                int newIndex = binding.quizCorrectAnswerGroup.getCheckedRadioButtonId();
                if (newIndex != postToEdit.getQuizCorrectOptionIndex()) return true;
            }
        }

        return false; // No changes found
    }

    private void showUnsavedChangesDialog() {
        CustomAlertDialogFragment dialog = CustomAlertDialogFragment.newInstance(
                "Discard Changes?",
                "If you go back now, your edits will be discarded.",
                "Discard",
                "Cancel"
        );
        dialog.setDialogListener(new CustomAlertDialogFragment.DialogListener() {
            @Override
            public void onPositiveClick() {
                finish(); // User confirmed discard, close activity
            }
            @Override
            public void onNegativeClick() {
                // User cancelled, do nothing
            }
        });
        if (!isFinishing()) {
            dialog.show(getSupportFragmentManager(), "UnsavedChangesDialog");
        }
    }
    // --- END NEW ---

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            checkForUnsavedChanges(); // <-- UPDATED: Check for changes
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