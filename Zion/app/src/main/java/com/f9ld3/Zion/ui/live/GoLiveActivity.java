package com.f9ld3.Zion.ui.live;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.f9ld3.Zion.databinding.ActivityGoLiveBinding;

public class GoLiveActivity extends AppCompatActivity {
    private ActivityGoLiveBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityGoLiveBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        binding.buttonGoLive.setOnClickListener(v -> {
            // TODO: Integrate with a live streaming SDK (e.g., Agora, Mux)
            String title = binding.editTextStreamTitle.getText().toString();
            if (title.isEmpty()) {
                Toast.makeText(this, "Please enter a title for your stream.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Starting live video stream...", Toast.LENGTH_SHORT).show();
                // SDK integration logic would go here.
            }
        });
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