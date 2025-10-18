package com.f9ld3.Zion.ui.notifications;

import android.os.Bundle;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.f9ld3.Zion.R;
import com.f9ld3.Zion.databinding.ActivityFragmentHostBinding;
import com.f9ld3.Zion.databinding.ActivityFragmentHostWithToolbarBinding;

public class NotificationsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityFragmentHostWithToolbarBinding binding = ActivityFragmentHostWithToolbarBinding.inflate(getLayoutInflater()); // Inflate the new layout
        setContentView(binding.getRoot());

        Toolbar toolbar = binding.toolbar; // Use the binding to find the toolbar
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.title_notifications);
        }

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new NotificationsFragment())
                    .commit();
        }
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