package com.f9ld3.Zion.ui.notifications;

import android.content.Intent;
import android.net.Uri; // Import Uri
import android.os.Build; // Import Build
import android.os.Bundle;
import android.provider.Settings; // Import Settings
import android.view.Menu; // Import Menu
import android.view.MenuInflater; // Import MenuInflater
import android.view.MenuItem;
import android.widget.Toast; // Import Toast

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider; // Import ViewModelProvider

import com.f9ld3.Zion.R;
import com.f9ld3.Zion.databinding.ActivityFragmentHostWithToolbarBinding;

public class NotificationsActivity extends AppCompatActivity {

    private NotificationViewModel notificationViewModel; // Add ViewModel reference

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityFragmentHostWithToolbarBinding binding = ActivityFragmentHostWithToolbarBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize the ViewModel
        notificationViewModel = new ViewModelProvider(this).get(NotificationViewModel.class);

        Toolbar toolbar = binding.toolbar;
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

    // --- ADDED: Inflate the menu ---
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.notification_options_menu, menu);
        return true;
    }
    // --- END ADDED ---

    // --- UPDATED: Handle menu item clicks ---
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            finish();
            return true;
        } else if (itemId == R.id.action_mark_all_read) {
            // Call the ViewModel method
            notificationViewModel.markAllAsRead();
            Toast.makeText(this, "Marking all as read...", Toast.LENGTH_SHORT).show();
            return true;
        } else if (itemId == R.id.action_notification_settings) {
            // Open system notification settings for this app
            openNotificationSettings();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // --- ADDED: Helper method to open system settings ---
    private void openNotificationSettings() {
        Intent intent = new Intent();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // For API 26+
            intent.setAction(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
            intent.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
        } else {
            // For older versions
            intent.setAction("android.settings.APP_NOTIFICATION_SETTINGS");
            intent.putExtra("app_package", getPackageName());
            intent.putExtra("app_uid", getApplicationInfo().uid);
        }
        startActivity(intent);
    }
    // --- END ADDED ---
}