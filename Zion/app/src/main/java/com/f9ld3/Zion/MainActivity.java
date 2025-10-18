package com.f9ld3.Zion;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.f9ld3.Zion.auth.AuthViewModel;
import com.f9ld3.Zion.auth.LoginActivity;
import com.f9ld3.Zion.databinding.ActivityMainBinding;
import com.f9ld3.Zion.ui.notifications.NotificationsActivity;
import com.f9ld3.Zion.ui.notifications.NotificationViewModel;
import com.f9ld3.Zion.ui.player.PlayerMedia;
import com.f9ld3.Zion.ui.search.SearchActivity;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity implements com.f9ld3.Zion.ui.player.PlayerFragment.HistoryLogger {

    private static final String TAG = "MainActivity";
    private ActivityMainBinding binding;
    private AuthViewModel authViewModel;
    private NotificationViewModel notificationViewModel;
    private NavController navController;
    private boolean isRedirecting = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FirebaseFirestore.setLoggingEnabled(true);
        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);
        notificationViewModel = new ViewModelProvider(this).get(NotificationViewModel.class);

        authViewModel.isAuthenticated().observe(this, authenticated -> {
            if (Boolean.TRUE.equals(authenticated)) {
                if (binding == null) {
                    binding = ActivityMainBinding.inflate(getLayoutInflater());
                    setContentView(binding.getRoot());

                    setSupportActionBar(binding.toolbar);
                    if (getSupportActionBar() != null) {
                        getSupportActionBar().setDisplayShowTitleEnabled(false);
                    }

                    // *** MODIFIED: Navigate to SearchFragment instead of starting SearchActivity ***
                    binding.buttonSearchToolbar.setOnClickListener(v -> {
                        if (navController != null) {
                            navController.navigate(R.id.navigation_search);
                        }
                    });

                    binding.buttonNotificationsToolbar.setOnClickListener(v -> {
                        Intent notificationsIntent = new Intent(this, NotificationsActivity.class);
                        startActivity(notificationsIntent);
                    });

                    notificationViewModel.getUnreadCount().observe(this, count -> {
                        if (binding.notificationBadge != null) {
                            if (count != null && count > 0) {
                                binding.notificationBadge.setText(String.valueOf(count));
                                binding.notificationBadge.setVisibility(View.VISIBLE);
                            } else {
                                binding.notificationBadge.setVisibility(View.GONE);
                            }
                        }
                    });

                    setupNavigation();
                }
            } else if (Boolean.FALSE.equals(authenticated)) {
                redirectToLogin();
            }
        });
    }

    private void setupNavigation() {
        try {
            NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.nav_host_fragment_activity_main);

            if (navHostFragment == null) {
                Log.e(TAG, "NavHostFragment not found!");
                return;
            }

            navController = navHostFragment.getNavController();
            NavigationUI.setupWithNavController(binding.navView, navController);

            navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
                int destinationId = destination.getId();
                // Show/hide UI elements based on the current screen
                if (destinationId == R.id.navigation_feed ||
                        destinationId == R.id.navigation_player ||
                        destinationId == R.id.navigation_profile) {
                    binding.appBarLayout.setVisibility(View.VISIBLE);
                    binding.navView.setVisibility(View.VISIBLE);
                } else {
                    binding.appBarLayout.setVisibility(View.GONE);
                    binding.navView.setVisibility(View.GONE);
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Error setting up navigation", e);
        }
    }

    private void redirectToLogin() {
        if (!isRedirecting) {
            isRedirecting = true;
            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }

    @Override
    public void logMediaView(PlayerMedia mediaItem) {
        // TODO: Implement history logging
    }
}