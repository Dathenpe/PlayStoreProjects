// MultipleFiles/CreateBlogActivity.java
package com.f9ld3.Zion.ui.blog;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.f9ld3.Zion.R;
import com.f9ld3.Zion.databinding.ActivityCreateBlogBinding; // NEW: Create this layout

public class CreateBlogActivity extends AppCompatActivity {

    private ActivityCreateBlogBinding binding;
    private BlogViewModel blogViewModel;
    private Uri selectedImageUri;

    // ActivityResultLauncher for picking an image
    private final ActivityResultLauncher<String> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    Glide.with(this).load(selectedImageUri).into(binding.imageViewThumbnail);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCreateBlogBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        blogViewModel = new ViewModelProvider(this).get(BlogViewModel.class);

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Create New Blog Post");
        }

        // Observe ViewModel for upload status
        blogViewModel.getUploading().observe(this, isUploading -> {
            if (isUploading) {
                binding.progressBar.setVisibility(View.VISIBLE);
                binding.buttonPublish.setEnabled(false);
            } else {
                binding.progressBar.setVisibility(View.GONE);
                binding.buttonPublish.setEnabled(true);
            }
        });

        blogViewModel.getUploadError().observe(this, error -> {
            if (error != null) {
                Toast.makeText(this, "Upload Error: " + error, Toast.LENGTH_LONG).show();
                blogViewModel.clearUploadStatus();
            }
        });

        blogViewModel.getUploadSuccess().observe(this, success -> {
            if (success != null && success) {
                Toast.makeText(this, "Blog post published successfully!", Toast.LENGTH_SHORT).show();
                blogViewModel.clearUploadStatus();
                finish(); // Close activity on success
            }
        });

        binding.buttonSelectImage.setOnClickListener(v -> pickImageLauncher.launch("image/*"));

        binding.buttonPublish.setOnClickListener(v -> {
            String title = binding.editTextTitle.getText().toString().trim();
            String description = binding.editTextDescription.getText().toString().trim();

            if (TextUtils.isEmpty(title)) {
                binding.editTextTitle.setError("Title is required");
                return;
            }
            if (TextUtils.isEmpty(description)) {
                binding.editTextDescription.setError("Description is required");
                return;
            }

            blogViewModel.createBlogPost(title, description, selectedImageUri);
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}