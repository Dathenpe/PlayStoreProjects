package com.f9ld3.Zion.ui.upload;

import static com.f9ld3.Zion.ui.player.MediaOptionsBottomSheet.TAG;

import android.content.Intent;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.f9ld3.Zion.R;
import com.f9ld3.Zion.databinding.ActivityUploadPodcastBinding;
import com.f9ld3.Zion.ui.dialogs.CustomAlertDialogFragment;

import java.io.IOException;
import java.util.concurrent.TimeUnit; // <-- ADDED IMPORT

public class UploadPodcastActivity extends AppCompatActivity {

    private ActivityUploadPodcastBinding binding;
    private UploadViewModel uploadViewModel;
    private Uri selectedAudioUri = null;
    private Uri selectedThumbnailUri = null;
    private long audioDuration = 0;

    // Audio file picker
    private final ActivityResultLauncher<Intent> audioPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedAudioUri = result.getData().getData();
                    if (selectedAudioUri != null) {
                        binding.textAudioFileName.setText(getFileName(selectedAudioUri));
                        binding.audioFileInfoLayout.setVisibility(View.VISIBLE);
                        // Updated call to setAudioDuration
                        try {
                            setAudioDuration(selectedAudioUri);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        Toast.makeText(this, "Audio file selected", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    // Thumbnail picker
    private final ActivityResultLauncher<Intent> thumbnailPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedThumbnailUri = result.getData().getData();
                    if (selectedThumbnailUri != null) {
                        Glide.with(this)
                                .load(selectedThumbnailUri)
                                .placeholder(R.drawable.ic_mic_24dp)
                                .into(binding.imageThumbnailPreview);
                        Toast.makeText(this, "Thumbnail selected", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityUploadPodcastBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        uploadViewModel = new ViewModelProvider(this).get(UploadViewModel.class);

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Upload Podcast");
        }

        setupObservers();
        setupClickListeners();
    }

    private void setupObservers() {
        // Observers remain the same...
        // Upload progress
        uploadViewModel.getUploadProgress().observe(this, progress -> {
            if (progress != null) {
                binding.progressBar.setProgress(progress);
                binding.textUploadProgress.setText(String.format("%d%%", progress));
            }
        });

        // Upload status
        uploadViewModel.getUploadStatus().observe(this, status -> {
            switch (status) {
                case IDLE:
                    binding.progressLayout.setVisibility(View.GONE);
                    binding.buttonUpload.setEnabled(true);
                    break;
                case UPLOADING:
                    binding.progressLayout.setVisibility(View.VISIBLE);
                    binding.buttonUpload.setEnabled(false);
                    break;
                case SUCCESS:
                    showSuccessDialog("Podcast uploaded successfully!");
                    break;
                case FAILED:
                    binding.progressLayout.setVisibility(View.GONE);
                    binding.buttonUpload.setEnabled(true);
                    break;
            }
        });

        // Error messages
        uploadViewModel.getErrorMessage().observe(this, error -> {
            if (error != null) {
                showDialog("Upload Failed", error);
                uploadViewModel.clearError();
            }
        });
    }

    private void setupClickListeners() {
        // Click listeners remain the same...
        binding.buttonSelectAudio.setOnClickListener(v -> openAudioPicker());
        binding.fabSelectThumbnail.setOnClickListener(v -> openThumbnailPicker());
        binding.buttonUpload.setOnClickListener(v -> attemptUpload());
    }

    private void openAudioPicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("audio/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        audioPickerLauncher.launch(Intent.createChooser(intent, "Select Audio File"));
    }

    private void openThumbnailPicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        thumbnailPickerLauncher.launch(intent);
    }

    private void attemptUpload() {
        String title = binding.editTextTitle.getText().toString().trim();
        String description = binding.editTextDescription.getText().toString().trim();

        // Validation
        if (TextUtils.isEmpty(title)) {
            binding.layoutTitle.setError("Title is required");
            return;
        }
        binding.layoutTitle.setError(null);

        if (selectedAudioUri == null) {
            showDialog("Missing Audio", "Please select an audio file to upload.");
            return;
        }

        if (selectedThumbnailUri == null) {
            showDialog("Missing Thumbnail", "Please select a thumbnail image.");
            return;
        }

        // Start upload
        uploadViewModel.uploadPodcast(
                title,
                description,
                selectedAudioUri,
                selectedThumbnailUri,
                audioDuration,
                this
        );
    }

    private String getFileName(Uri uri) {
        String fileName = "audio_file";
        if (uri != null) {
            try (android.database.Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1) {
                        fileName = cursor.getString(nameIndex);
                    }
                }
            } catch (Exception e) {
                fileName = uri.getLastPathSegment();
            }
        }
        return fileName;
    }

    // MODIFIED METHOD
    private void setAudioDuration(Uri audioUri) throws IOException {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(this, audioUri);
            String durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            if (durationStr != null) {
                long durationMs = Long.parseLong(durationStr);
                // Convert to seconds
                audioDuration = TimeUnit.MILLISECONDS.toSeconds(durationMs);
                // Set text on the new TextView
                binding.textAudioDuration.setText(formatDuration(audioDuration));
                binding.textAudioDuration.setVisibility(View.VISIBLE);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to get audio duration", e);
            binding.textAudioDuration.setVisibility(View.GONE);
        } finally {
            retriever.release();
        }
    }

    // NEW HELPER METHOD
    private String formatDuration(long seconds) {
        long hours = TimeUnit.SECONDS.toHours(seconds);
        long minutes = TimeUnit.SECONDS.toMinutes(seconds) - TimeUnit.HOURS.toMinutes(hours);
        long remainingSeconds = seconds - TimeUnit.HOURS.toSeconds(hours) - TimeUnit.MINUTES.toSeconds(minutes);

        if (hours > 0) {
            return String.format("%02d:%02d:%02d", hours, minutes, remainingSeconds);
        } else {
            return String.format("%02d:%02d", minutes, remainingSeconds);
        }
    }

    private void showDialog(String title, String message) {
        CustomAlertDialogFragment.newInstance(title, message, "OK", null)
                .show(getSupportFragmentManager(), "UploadDialog");
    }

    private void showSuccessDialog(String message) {
        CustomAlertDialogFragment dialog = CustomAlertDialogFragment.newInstance("Success!", message, "OK", null);
        dialog.setDialogListener(new CustomAlertDialogFragment.DialogListener() {
            @Override
            public void onPositiveClick() {
                finish();
            }

            @Override
            public void onNegativeClick() {
                finish();
            }
        });
        dialog.show(getSupportFragmentManager(), "SuccessDialog");
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}