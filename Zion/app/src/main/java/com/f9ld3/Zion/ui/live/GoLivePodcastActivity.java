package com.f9ld3.Zion.ui.live;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.f9ld3.Zion.databinding.ActivityGoLivePodcastBinding;

public class GoLivePodcastActivity extends AppCompatActivity {
    private ActivityGoLivePodcastBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityGoLivePodcastBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        binding.buttonGoLive.setOnClickListener(v -> {
            // TODO: Integrate with a live streaming SDK (e.g., Agora, Mux) for audio-only stream
            Toast.makeText(this, "Starting live podcast...", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}