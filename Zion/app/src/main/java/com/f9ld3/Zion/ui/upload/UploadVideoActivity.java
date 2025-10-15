package com.f9ld3.Zion.ui.upload;

import android.content.Intent;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.f9ld3.Zion.R;
import com.f9ld3.Zion.databinding.ActivityUploadVideoBinding;
import com.f9ld3.Zion.ui.dialogs.CustomAlertDialogFragment;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class UploadVideoActivity extends AppCompatActivity {

    private ActivityUploadVideoBinding binding;
    private UploadViewModel uploadViewModel;
    private Uri selectedVideoUri = null;
    private Uri selectedThumbnailUri = null;
    private long videoDuration = 0;

    private final ActivityResultLauncher<Intent> videoPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedVideoUri = result.getData().getData();
                    if (selectedVideoUri != null) {
                        binding.textVideoFileName.setText(getFileName(selectedVideoUri));
                        binding.videoFileInfoLayout.setVisibility(View.VISIBLE);
                        // This method will now update the new TextView
                        try {
                            setVideoDuration(selectedVideoUri);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }
    );

    private final ActivityResultLauncher<Intent> thumbnailPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedThumbnailUri = result.getData().getData();
                    if (selectedThumbnailUri != null) {
                        Glide.with(this)
                                .load(selectedThumbnailUri)
                                .into(binding.imageThumbnailPreview);
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityUploadVideoBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        uploadViewModel = new ViewModelProvider(this).get(UploadViewModel.class);

        setupToolbar();
        setupObservers();
        setupClickListeners();
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Upload Video");
        }
    }

    private void setupObservers() {
        uploadViewModel.getUploadProgress().observe(this, progress -> {
            if (progress != null) {
                binding.progressBar.setProgress(progress);
                binding.textUploadProgress.setText(String.format("%d%%", progress));
            }
        });

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
                    showSuccessDialog("Video uploaded successfully!");
                    break;
                case FAILED:
                    binding.progressLayout.setVisibility(View.GONE);
                    binding.buttonUpload.setEnabled(true);
                    break;
            }
        });

        uploadViewModel.getErrorMessage().observe(this, error -> {
            if (error != null) {
                showDialog("Upload Failed", error);
                uploadViewModel.clearError();
            }
        });
    }

    private void setupClickListeners() {
        binding.buttonSelectVideo.setOnClickListener(v -> openVideoPicker());
        binding.fabSelectThumbnail.setOnClickListener(v -> openThumbnailPicker());
        binding.buttonUpload.setOnClickListener(v -> attemptUpload());
    }

    private void openVideoPicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("video/*");
        videoPickerLauncher.launch(intent);
    }

    private void openThumbnailPicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        thumbnailPickerLauncher.launch(intent);
    }

    private void attemptUpload() {
        String title = binding.editTextTitle.getText().toString().trim();
        String description = binding.editTextDescription.getText().toString().trim();

        if (TextUtils.isEmpty(title)) {
            binding.layoutTitle.setError("Title is required");
            return;
        }
        binding.layoutTitle.setError(null);

        if (selectedVideoUri == null) {
            showDialog("Missing Video", "Please select a video file to upload.");
            return;
        }

        if (selectedThumbnailUri == null) {
            showDialog("Missing Thumbnail", "Please select a thumbnail image.");
            return;
        }

        uploadViewModel.uploadVideo(title, description, selectedVideoUri, selectedThumbnailUri, videoDuration, this);
    }

    private String getFileName(Uri uri) {
        String fileName = "video_file";
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
        return fileName;
    }

    private void setVideoDuration(Uri videoUri) throws IOException {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(this, videoUri);
            String durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            if (durationStr != null) {
                long durationMs = Long.parseLong(durationStr);
                videoDuration = TimeUnit.MILLISECONDS.toSeconds(durationMs);
                // These lines now work as intended
                binding.textVideoDuration.setText(formatDuration(videoDuration));
                binding.textVideoDuration.setVisibility(View.VISIBLE);
            }
        } catch (Exception e) {
            e.printStackTrace();
            binding.textVideoDuration.setVisibility(View.GONE);
        } finally {
            retriever.release();
        }
    }

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
                // Also finish on negative click in this context
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