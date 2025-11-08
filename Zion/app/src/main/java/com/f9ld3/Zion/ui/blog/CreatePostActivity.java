// main/java/com/f9ld3/Zion/ui/blog/CreatePostActivity.java
package com.f9ld3.Zion.ui.blog;

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
import com.f9ld3.Zion.databinding.ActivityCreatePostBinding;
import com.f9ld3.Zion.ui.dialogs.CustomAlertDialogFragment;
import com.f9ld3.Zion.ui.feed.MediaItem;
import com.f9ld3.Zion.ui.feed.PollOption;
import com.f9ld3.Zion.ui.feed.Post;
import com.google.android.material.imageview.ShapeableImageView; // Import ShapeableImageView
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import java.util.ArrayList;
import java.util.HashMap; // Import HashMap
import java.util.List;
import java.util.Map; // Import Map

public class CreatePostActivity extends AppCompatActivity implements MediaPreviewAdapter.OnMediaItemClickListener {

    private static final String TAG = "CreatePostActivity";

    private ActivityCreatePostBinding binding;
    private PostViewModel postViewModel;
    private MediaPreviewAdapter adapter;
    private final List<MediaItem> mediaItems = new ArrayList<>();
    private String currentPostType = Post.TYPE_TEXT_MEDIA;

    // --- Poll Option Management Logic ---
    private final List<TextInputLayout> pollOptionLayouts = new ArrayList<>();
    private final List<TextInputEditText> pollOptionEditTexts = new ArrayList<>();
    // *** NEW: Store Views for Image Handling ***
    private final List<ImageView> pollOptionImageViews = new ArrayList<>();
    private final List<ImageButton> pollOptionImageButtons = new ArrayList<>();
    private final Map<Integer, Uri> pollOptionImageUris = new HashMap<>(); // Store selected URIs by index
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
                        pollOptionImageUris.put(currentImagePickerIndex, selectedUri); // Store URI
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
        binding = ActivityCreatePostBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        postViewModel = new ViewModelProvider(this).get(PostViewModel.class);

        setupToolbar();
        setupRecyclerView();
        setupPollOptionManagement(); // <-- CALL NEW SETUP METHOD HERE
        setupClickListeners();
        observeViewModel();
        updateUiForPostType(); // Set initial UI state

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
            getSupportActionBar().setTitle("Create Post"); // Set a title
        }
    }

    private void setupRecyclerView() {
        adapter = new MediaPreviewAdapter(mediaItems, this);
        binding.mediaPreviewRecycler.setLayoutManager(new GridLayoutManager(this, 3));
        binding.mediaPreviewRecycler.setAdapter(adapter);
    }

    private MediaItem createMediaItemFromUri(Uri uri) {
        if (uri == null) return null;
        String mimeType = getContentResolver().getType(uri);
        String mediaType = "unknown";
        String thumbnailUrl = uri.toString(); // Default thumbnail is the URI itself

        if (mimeType != null) {
            if (mimeType.startsWith("image")) mediaType = "image";
            else if (mimeType.startsWith("video")) mediaType = "video";
        } else {
            // Fallback check based on extension if MIME type is null
            String path = uri.getPath();
            if (path != null) {
                String lowerPath = path.toLowerCase();
                if (lowerPath.endsWith(".jpg") || lowerPath.endsWith(".jpeg") || lowerPath.endsWith(".png") || lowerPath.endsWith(".gif") || lowerPath.endsWith(".webp")) mediaType = "image";
                else if (lowerPath.endsWith(".mp4") || lowerPath.endsWith(".mov") || lowerPath.endsWith(".avi") || lowerPath.endsWith(".mkv") || lowerPath.endsWith(".webm")) mediaType = "video";
            }
        }
        // Only return if type is known (image or video)
        if ("unknown".equals(mediaType)) {
            Log.w(TAG, "Could not determine media type for URI: " + uri + ". Skipping file.");
            Toast.makeText(this, "Unsupported file type selected", Toast.LENGTH_SHORT).show();
            return null;
        }
        // For video, thumbnail might be the same initially, or generated later
        // If it's an image, thumbnail and URL are the same
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
            @Override
            public void onPositiveClick() {
                if (position >= 0 && position < mediaItems.size()) {
                    mediaItems.remove(position);
                    adapter.notifyItemRemoved(position);
                    adapter.notifyItemRangeChanged(position, mediaItems.size());
                    if (mediaItems.isEmpty()) {
                        binding.mediaPreviewRecycler.setVisibility(View.GONE);
                    }
                }
            }
            @Override
            public void onNegativeClick() {}
        });
        if (!isFinishing()) {
            dialog.show(getSupportFragmentManager(), "RemoveMediaDialog");
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
            addPollOptionInputView(null);
        }
        updatePollOptionHintsAndRemoveButtons();
    }

    private void addPollOptionInputView(@Nullable String initialText) {
        if (pollOptionLayouts.size() >= MAX_POLL_OPTIONS) return;

        LayoutInflater inflater = LayoutInflater.from(this);
        // 1. Inflate the root view (ConstraintLayout in this case)
        View inflatedView = inflater.inflate(R.layout.item_poll_option_input, binding.pollOptionsContainer, false);

        // 2. Find the TextInputLayout *within* the inflated view using its ID
        TextInputLayout newLayout = inflatedView.findViewById(R.id.poll_option_input_layout); // <-- Find the TextInputLayout by ID

        // 3. Find other views *within* the inflated view
        TextInputEditText newEditText = inflatedView.findViewById(R.id.input_poll_option);
        ImageView newImageView = inflatedView.findViewById(R.id.image_poll_option_preview);
        ImageButton newImageButton = inflatedView.findViewById(R.id.button_add_poll_option_image);

        // Check if TextInputLayout was found before proceeding
        if (newLayout != null && newEditText != null && newImageView != null && newImageButton != null) {
            if (initialText != null) newEditText.setText(initialText);
            newEditText.addTextChangedListener(quizOptionTextWatcher);
            newLayout.setEndIconOnClickListener(v -> removePollOptionInputView(newLayout)); // Use newLayout here

            final int currentIndex = pollOptionLayouts.size();
            newImageButton.setOnClickListener(v -> {
                currentImagePickerIndex = currentIndex;
                openPollOptionImagePicker();
            });

            // Add the entire inflated view (the ConstraintLayout) to the container
            binding.pollOptionsContainer.addView(inflatedView);

            // Keep track of the actual TextInputLayout
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

    private void removePollOptionInputView(TextInputLayout layoutToRemove) {
        if (pollOptionLayouts.size() <= MIN_POLL_OPTIONS) return;

        int indexToRemove = pollOptionLayouts.indexOf(layoutToRemove);
        if (indexToRemove != -1) {
            binding.pollOptionsContainer.removeView(layoutToRemove);
            pollOptionLayouts.remove(indexToRemove);
            // *** NEW: Remove corresponding Image Views and URI ***
            if (indexToRemove < pollOptionEditTexts.size()) pollOptionEditTexts.remove(indexToRemove);
            if (indexToRemove < pollOptionImageViews.size()) pollOptionImageViews.remove(indexToRemove);
            if (indexToRemove < pollOptionImageButtons.size()) pollOptionImageButtons.remove(indexToRemove);
            pollOptionImageUris.remove(indexToRemove);
            // Adjust indices in the map for items after the removed one
            Map<Integer, Uri> adjustedUris = new HashMap<>();
            for (Map.Entry<Integer, Uri> entry : pollOptionImageUris.entrySet()) {
                if (entry.getKey() > indexToRemove) {
                    adjustedUris.put(entry.getKey() - 1, entry.getValue());
                } else {
                    adjustedUris.put(entry.getKey(), entry.getValue());
                }
            }
            pollOptionImageUris.clear();
            pollOptionImageUris.putAll(adjustedUris);
            // *** END NEW ***

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
            Uri imageUri = pollOptionImageUris.get(i); // Check if there's an image
            // Only add radio button if there's text OR an image
            if (!optionText.isEmpty() || imageUri != null) {
                RadioButton radioButton = new RadioButton(this);
                radioButton.setId(i);
                // Show text, or "Image Option [n]" if only image exists
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
        // *** NEW: Clear Image Views and URIs Map ***
        for (ImageView imageView : pollOptionImageViews) {
            if (imageView != null) {
                imageView.setImageDrawable(null);
                imageView.setVisibility(View.GONE);
            }
        }
        pollOptionImageUris.clear();
        // *** END NEW ***
        binding.quizCorrectAnswerGroup.clearCheck();
        binding.pollDurationGroup.check(R.id.duration_none); // Reset duration selection
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

    // --- Click Listeners ---
    private void setupClickListeners() {
        binding.buttonPublish.setOnClickListener(v -> publishPost());
        binding.buttonAttachMedia.setOnClickListener(v -> openMediaPicker());
        binding.buttonAddOption.setOnClickListener(v -> addPollOptionInputView(null)); // Use new method

        binding.togglePostType.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;

            String previousType = currentPostType;
            if (checkedId == R.id.button_type_media) currentPostType = Post.TYPE_TEXT_MEDIA;
            else if (checkedId == R.id.button_type_poll) currentPostType = Post.TYPE_POLL;
            else if (checkedId == R.id.button_type_quiz) currentPostType = Post.TYPE_QUIZ;

            // Check if switching away from poll/quiz AND if there's data entered
            if (!currentPostType.equals(previousType) && (previousType.equals(Post.TYPE_POLL) || previousType.equals(Post.TYPE_QUIZ))) {
                boolean hasPollData = false;
                for (TextInputEditText editText : pollOptionEditTexts) {
                    if (editText != null && editText.getText().length() > 0) {
                        hasPollData = true;
                        break;
                    }
                }
                // *** NEW: Also check if any images were added ***
                if (!hasPollData && !pollOptionImageUris.isEmpty()) {
                    hasPollData = true;
                }
                // *** END NEW ***
                if (hasPollData) showSwitchConfirmationDialog(previousType);
                else updateUiForPostType(); // Switch UI directly if no data
            } else {
                updateUiForPostType(); // Update UI for the selected type
            }
        });
    }

    private void showSwitchConfirmationDialog(String previousType) {
        CustomAlertDialogFragment dialog = CustomAlertDialogFragment.newInstance(
                "Discard " + previousType + "?",
                "Switching post type will discard the options and images you've entered.", // Updated message
                "Switch Anyway",
                "Cancel"
        );
        dialog.setDialogListener(new CustomAlertDialogFragment.DialogListener() {
            @Override
            public void onPositiveClick() {
                clearPollOptions(); // Clear data
                updateUiForPostType(); // Update UI to new type
            }
            @Override
            public void onNegativeClick() {
                // Revert toggle button selection
                int buttonIdToReselect = R.id.button_type_media;
                if (previousType.equals(Post.TYPE_POLL)) buttonIdToReselect = R.id.button_type_poll;
                else if (previousType.equals(Post.TYPE_QUIZ)) buttonIdToReselect = R.id.button_type_quiz;
                binding.togglePostType.check(buttonIdToReselect);
                currentPostType = previousType; // Revert type state variable
            }
        });
        dialog.show(getSupportFragmentManager(), "SwitchTypeConfirmation");
    }


    private void observeViewModel() {
        postViewModel.getUploadStatus().observe(this, status -> {
            if (status == null) return;
            boolean isLoading = status == PostViewModel.UploadStatus.UPLOADING;
            binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            binding.progressBar.setIndeterminate(isLoading);
            binding.buttonPublish.setEnabled(!isLoading);
            binding.editTextContent.setEnabled(!isLoading);
            binding.buttonAttachMedia.setEnabled(!isLoading);
            binding.buttonAddOption.setEnabled(!isLoading && pollOptionLayouts.size() < MAX_POLL_OPTIONS);
            binding.togglePostType.setEnabled(!isLoading);

            // Disable poll inputs, remove buttons, and image buttons
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
                    // Disable layout to prevent remove icon click (only for optional)
                    if (i >= MIN_POLL_OPTIONS) {
                        pollOptionLayouts.get(i).setEnabled(!isLoading);
                    }
                }
            }
            binding.durationNone.setEnabled(!isLoading);
            binding.duration24h.setEnabled(!isLoading);
            binding.duration3d.setEnabled(!isLoading);

            if (status == PostViewModel.UploadStatus.SUCCESS) {
                Toast.makeText(this, "Post published!", Toast.LENGTH_SHORT).show();
                finish();
            } else if (status == PostViewModel.UploadStatus.FAILED) {
                // Error message shown via observer
            }
        });

        postViewModel.getErrorMessage().observe(this, error -> {
            if (error != null && !isFinishing()) {
                CustomAlertDialogFragment.newInstance("Upload Failed", error, "OK", null)
                        .show(getSupportFragmentManager(), "UploadErrorDialog");
                postViewModel.clearMessages();
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
            binding.pollQuizLabel.setText(currentPostType.equals(Post.TYPE_POLL) ? "Create Poll" : "Create Quiz");
            if (currentPostType.equals(Post.TYPE_QUIZ)) {
                updateQuizRadioButtons();
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

    private void publishPost() {
        String content = binding.editTextContent.getText().toString().trim();

        Post post = new Post();
        post.setPostType(currentPostType);
        if (!content.isEmpty()) {
            post.setTextContent(content);
        }

        List<PollOption> pollOptions = new ArrayList<>();
        int quizCorrectIndex = -1;
        Integer pollDuration = null;
        // *** NEW: Map to pass URIs to ViewModel ***
        Map<Integer, Uri> imageUrisToUpload = new HashMap<>();
        // *** END NEW ***

        if (currentPostType.equals(Post.TYPE_POLL) || currentPostType.equals(Post.TYPE_QUIZ)) {
            // Iterate through the managed EditTexts
            for (int i = 0; i < pollOptionEditTexts.size(); i++) {
                TextInputEditText editText = pollOptionEditTexts.get(i);
                TextInputLayout layout = pollOptionLayouts.get(i);
                if (editText == null || layout == null) continue;

                String optionText = editText.getText().toString().trim();
                Uri imageUri = pollOptionImageUris.get(i); // Get URI for this option

                // An option is valid if it has text OR an image
                if (!optionText.isEmpty() || imageUri != null) {
                    PollOption option = new PollOption(optionText); // Create option (URL will be added later)
                    pollOptions.add(option);
                    if (imageUri != null) {
                        imageUrisToUpload.put(pollOptions.size() - 1, imageUri); // Map URI to final index in pollOptions
                    }
                    layout.setError(null); // Clear error if any
                } else if (i < MIN_POLL_OPTIONS) {
                    // Option is invalid only if it's required (first MIN_POLL_OPTIONS) AND has neither text nor image
                    layout.setError("Option " + (i + 1) + " requires text or an image."); // Show error on layout
                    return; // Stop processing
                } else {
                    layout.setError(null); // Clear error for optional fields that are empty
                }
            }
            // Clear errors for initially required fields if they are now filled
            if (pollOptionLayouts.size() > 0 && pollOptionLayouts.get(0) != null) pollOptionLayouts.get(0).setError(null);
            if (pollOptionLayouts.size() > 1 && pollOptionLayouts.get(1) != null) pollOptionLayouts.get(1).setError(null);


            if (pollOptions.size() < MIN_POLL_OPTIONS) {
                Toast.makeText(this, "A poll or quiz requires at least " + MIN_POLL_OPTIONS + " non-empty options (text or image).", Toast.LENGTH_SHORT).show();
                return;
            }
            post.setPollOptions(pollOptions); // Set options without image URLs initially

            // Get selected poll duration
            int selectedDurationId = binding.pollDurationGroup.getCheckedRadioButtonId();
            if (selectedDurationId == R.id.duration_24h) {
                pollDuration = 24;
            } else if (selectedDurationId == R.id.duration_3d) {
                pollDuration = 72; // 3 days * 24 hours
            }
            post.setPollDurationHours(pollDuration);

            if (currentPostType.equals(Post.TYPE_QUIZ)) {
                quizCorrectIndex = binding.quizCorrectAnswerGroup.getCheckedRadioButtonId(); // ID is the index
                // Check if a valid answer is selected and corresponds to a non-empty option
                if (quizCorrectIndex == -1 || quizCorrectIndex >= pollOptions.size()) {
                    Toast.makeText(this, "Please select a valid correct answer for the quiz.", Toast.LENGTH_SHORT).show();
                    return;
                }
                post.setQuizCorrectOptionIndex(quizCorrectIndex);
            }
        }

        // Final validation: Ensure post is not completely empty
        if (content.isEmpty() && mediaItems.isEmpty() && pollOptions.isEmpty()) {
            Toast.makeText(this, "Cannot publish an empty post.", Toast.LENGTH_SHORT).show();
            return;
        }

        // If it's not a media post, ensure mediaItems list is empty before saving
        if (!currentPostType.equals(Post.TYPE_TEXT_MEDIA)) {
            mediaItems.clear(); // Clear any potentially selected media
            post.setMediaItems(new ArrayList<>());
        }

        // *** UPDATED: Pass pollOptionImageUris to ViewModel ***
        postViewModel.createPost(post, mediaItems, imageUrisToUpload, this);
    }

    // --- NEW: Check for unsaved changes before exiting ---
    private void checkForUnsavedChanges() {
        if (hasUnsavedChanges()) {
            showUnsavedChangesDialog();
        } else {
            finish(); // No changes, just close
        }
    }

    private boolean hasUnsavedChanges() {
        // Check text content
        if (!binding.editTextContent.getText().toString().trim().isEmpty()) {
            return true;
        }
        // Check for added media
        if (!mediaItems.isEmpty()) {
            return true;
        }
        // Check for poll data
        if (currentPostType.equals(Post.TYPE_POLL) || currentPostType.equals(Post.TYPE_QUIZ)) {
            for (TextInputEditText editText : pollOptionEditTexts) {
                if (editText != null && !editText.getText().toString().trim().isEmpty()) {
                    return true;
                }
            }
            // *** NEW: Check if any poll images were added ***
            if (!pollOptionImageUris.isEmpty()) {
                return true;
            }
            // *** END NEW ***
            // Check if duration was changed from default
            if (binding.pollDurationGroup.getCheckedRadioButtonId() != R.id.duration_none) {
                return true;
            }
        }
        return false; // No changes found
    }

    private void showUnsavedChangesDialog() {
        CustomAlertDialogFragment dialog = CustomAlertDialogFragment.newInstance(
                "Discard Post?",
                "If you go back now, your draft will be discarded.",
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