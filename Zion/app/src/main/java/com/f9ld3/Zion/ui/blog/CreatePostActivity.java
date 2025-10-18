package com.f9ld3.Zion.ui.blog;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import com.f9ld3.Zion.databinding.ActivityCreatePostBinding;
import com.f9ld3.Zion.ui.feed.Post;
import java.util.ArrayList;
import java.util.List;

public class CreatePostActivity extends AppCompatActivity {

    private ActivityCreatePostBinding binding;
    private PostViewModel postViewModel;
    private MediaPreviewAdapter adapter;
    private List<Uri> mediaUris = new ArrayList<>();

    private final ActivityResultLauncher<Intent> pickMediaLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    if (result.getData().getClipData() != null) {
                        int count = result.getData().getClipData().getItemCount();
                        if (mediaUris.size() + count > 15) {
                            Toast.makeText(this, "You can select a maximum of 15 files.", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        for (int i = 0; i < count; i++) {
                            mediaUris.add(result.getData().getClipData().getItemAt(i).getUri());
                        }
                    } else if (result.getData().getData() != null) {
                        if (mediaUris.size() >= 15) {
                            Toast.makeText(this, "You can select a maximum of 15 files.", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        mediaUris.add(result.getData().getData());
                    }
                    adapter.notifyDataSetChanged();
                    binding.mediaPreviewRecycler.setVisibility(View.VISIBLE);
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
        setupClickListeners();
        observeViewModel();
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("");
    }

    private void setupRecyclerView() {
        adapter = new MediaPreviewAdapter(mediaUris);
        binding.mediaPreviewRecycler.setLayoutManager(new GridLayoutManager(this, 3));
        binding.mediaPreviewRecycler.setAdapter(adapter);
    }

    private void setupClickListeners() {
        binding.buttonPublish.setOnClickListener(v -> publishPost());
        binding.buttonAttachMedia.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"image/*", "video/*"});
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            pickMediaLauncher.launch(Intent.createChooser(intent, "Select Media"));
        });
    }

    private void observeViewModel() {
        postViewModel.getUploadStatus().observe(this, status -> {
            if (status == null) return;
            switch (status) {
                case UPLOADING:
                    binding.progressBar.setVisibility(View.VISIBLE);
                    binding.buttonPublish.setEnabled(false);
                    break;
                case SUCCESS:
                    Toast.makeText(this, "Post published!", Toast.LENGTH_SHORT).show();
                    finish();
                    break;
                case FAILED:
                    binding.progressBar.setVisibility(View.GONE);
                    binding.buttonPublish.setEnabled(true);
                    break;
            }
        });

        postViewModel.getErrorMessage().observe(this, error -> {
            if (error != null) {
                Toast.makeText(this, error, Toast.LENGTH_LONG).show();
                postViewModel.clearMessages();
            }
        });
    }

    private void publishPost() {
        String content = binding.editTextContent.getText().toString().trim();
        if (content.isEmpty() && mediaUris.isEmpty()) {
            Toast.makeText(this, "Cannot publish an empty post.", Toast.LENGTH_SHORT).show();
            return;
        }
        postViewModel.createPostWithMultipleMedia(content, mediaUris, this);
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