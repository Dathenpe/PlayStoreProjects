// main/java/com/f9ld3/Zion/MainActivity.java
package com.f9ld3.Zion;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast; // Import Toast

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.f9ld3.Zion.auth.AuthViewModel;
import com.f9ld3.Zion.auth.LoginActivity;
import com.f9ld3.Zion.databinding.ActivityMainBinding;
import com.f9ld3.Zion.ui.feed.PostDetailActivity; // Import PostDetailActivity
import com.f9ld3.Zion.ui.notifications.NotificationsActivity;
import com.f9ld3.Zion.ui.notifications.NotificationViewModel;
import com.f9ld3.Zion.ui.notifications.MyFirebaseMessagingService; // <-- ADDED IMPORT
import com.f9ld3.Zion.ui.player.PlayerMedia;
// Removed SearchActivity import as we navigate within MainActivity now
// import com.f9ld3.Zion.ui.search.SearchActivity;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity implements com.f9ld3.Zion.ui.player.PlayerFragment.HistoryLogger {

    private static final String TAG = "MainActivity";
    private ActivityMainBinding binding;
    private AuthViewModel authViewModel;
    private NotificationViewModel notificationViewModel;
    private NavController navController;
    private boolean isRedirecting = false;
    private Intent pendingNavigationIntent = null; // Store intent if NavController isn't ready

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FirebaseFirestore.setLoggingEnabled(true);
        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);
        notificationViewModel = new ViewModelProvider(this).get(NotificationViewModel.class);

        // Store the initial intent in case it's for navigation
        pendingNavigationIntent = getIntent();

        authViewModel.isAuthenticated().observe(this, authenticated -> {
            if (Boolean.TRUE.equals(authenticated)) {
                // If authenticated and binding is null, setup the main UI
                if (binding == null) {
                    binding = ActivityMainBinding.inflate(getLayoutInflater());
                    setContentView(binding.getRoot());

                    setSupportActionBar(binding.toolbar);
                    if (getSupportActionBar() != null) {
                        getSupportActionBar().setDisplayShowTitleEnabled(false);
                    }

                    // Navigate to SearchFragment within this activity
                    binding.buttonSearchToolbar.setOnClickListener(v -> {
                        if (navController != null) {
                            try { // Add try-catch for safety
                                navController.navigate(R.id.navigation_search);
                            } catch (Exception e) {
                                Log.e(TAG, "Failed to navigate to search", e);
                            }
                        }
                    });

                    binding.buttonNotificationsToolbar.setOnClickListener(v -> {
                        Intent notificationsIntent = new Intent(this, NotificationsActivity.class);
                        startActivity(notificationsIntent);
                    });

                    notificationViewModel.getUnreadCount().observe(this, count -> {
                        if (binding != null && binding.notificationBadge != null) { // Check binding
                            if (count != null && count > 0) {
                                binding.notificationBadge.setText(String.valueOf(count));
                                binding.notificationBadge.setVisibility(View.VISIBLE);
                            } else {
                                binding.notificationBadge.setVisibility(View.GONE);
                            }
                        }
                    });

                    setupNavigation(); // Setup navigation which might trigger intent handling

                    // --- ADD THIS LINE ---
                    // Proactively save the FCM token on every authenticated startup.
                    MyFirebaseMessagingService.updateFCMToken();
                    // --- END ADD ---
                }
            } else if (Boolean.FALSE.equals(authenticated)) {
                // If not authenticated (or becomes unauthenticated), redirect to login
                redirectToLogin();
            }
            // If authenticated is null (initial state), do nothing yet, wait for state change
        });
    }

    private void setupNavigation() {
        try {
            NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.nav_host_fragment_activity_main);

            if (navHostFragment == null) {
                Log.e(TAG, "NavHostFragment not found!");
                // Attempt to retry or show error
                // Check if binding is still valid before posting delayed action
                if (binding != null) {
                    binding.getRoot().postDelayed(this::setupNavigation, 500); // Retry after delay
                }
                return;
            }

            navController = navHostFragment.getNavController();
            NavigationUI.setupWithNavController(binding.navView, navController);

            navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
                if (binding == null) return; // Check binding validity

                int destinationId = destination.getId();
                boolean showMainBars = destinationId == R.id.navigation_feed ||
                        destinationId == R.id.navigation_player ||
                        destinationId == R.id.navigation_profile;

                binding.appBarLayout.setVisibility(showMainBars ? View.VISIBLE : View.GONE);
                binding.navView.setVisibility(showMainBars ? View.VISIBLE : View.GONE);
            });

            // *** Crucial: Handle any pending navigation intent *after* NavController is ready ***
            if (pendingNavigationIntent != null) {
                handleNavigationIntent(pendingNavigationIntent);
                pendingNavigationIntent = null; // Consume the intent
            }

        } catch (Exception e) {
            Log.e(TAG, "Error setting up navigation", e);
            // Consider showing an error message or attempting recovery
        }
    }


    // *** ADDED onNewIntent ***
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent); // Update the activity's intent
        // Handle navigation if NavController is ready, otherwise store it
        if (navController != null) {
            handleNavigationIntent(intent);
        } else {
            pendingNavigationIntent = intent;
            Log.w(TAG, "onNewIntent: NavController not ready yet, storing intent.");
        }
    }
    // *** END ADDED onNewIntent ***

    // *** ADDED handleNavigationIntent ***
    private void handleNavigationIntent(Intent intent) {
        // --- Navigation from PostDetailActivity ---
        if (intent != null && PostDetailActivity.ACTION_NAVIGATE_TO_CHANNEL.equals(intent.getAction())) {
            String channelId = intent.getStringExtra(PostDetailActivity.EXTRA_CHANNEL_ID);
            String channelName = intent.getStringExtra(PostDetailActivity.EXTRA_CHANNEL_NAME);

            // It's crucial to clear the action *after* processing to prevent re-navigation
            // if MainActivity restarts for some reason (e.g., config change).
            // Do this within the navigation attempt or after checks.

            if (channelId != null && channelName != null) {
                Log.d(TAG, "Handling intent to navigate to channel: " + channelId);

                // Use post to ensure navigation happens smoothly after setup/resume
                // and avoids potential "FragmentManager is already executing transactions" errors.
                // Check binding validity before posting
                if (binding != null) {
                    binding.getRoot().post(() -> {
                        if (navController != null) {
                            Log.d(TAG, "NavController is ready, attempting navigation via post().");
                            Bundle args = new Bundle();
                            args.putString("channelId", channelId);
                            args.putString("channelName", channelName);
                            try {
                                // You might want to check if already at the destination, but often
                                // just calling navigate() is fine as NavController handles it.
                                navController.navigate(R.id.navigation_channel, args);
                                Log.d(TAG, "Navigation command sent to NavController.");
                                // Clear action after successful navigation attempt
                                getIntent().setAction(null);
                            } catch (IllegalArgumentException e) {
                                Log.e(TAG, "Navigation failed: Destination unknown or invalid arguments.", e);
                                Toast.makeText(this, "Could not navigate to profile (destination error).", Toast.LENGTH_SHORT).show();
                                getIntent().setAction(null); // Clear action even on failure
                            } catch (Exception e) {
                                Log.e(TAG, "Navigation to channel from Intent failed", e);
                                Toast.makeText(this, "Could not navigate to profile.", Toast.LENGTH_SHORT).show();
                                getIntent().setAction(null); // Clear action even on failure
                            }
                        } else {
                            Log.e(TAG, "NavController is null when attempting navigation via post(). Storing intent again.");
                            pendingNavigationIntent = intent; // Re-store if NavController became null somehow
                        }
                    });
                } else {
                    Log.e(TAG, "Binding is null, cannot post navigation action. Storing intent.");
                    pendingNavigationIntent = intent; // Store intent if binding is null
                }
            } else {
                Log.w(TAG, "Intent action matched, but channelId or channelName was null.");
                // Clear action if the data needed is missing
                if (getIntent() == intent) getIntent().setAction(null);
            }
        }
        // --- Add other intent handling logic here if needed ---
        // else if (/* check for other actions */) { ... }

        else {
            Log.v(TAG, "handleNavigationIntent called, but no matching action found or intent is null.");
        }
    }
    // *** END ADDED handleNavigationIntent ***


    private void redirectToLogin() {
        // Prevent redirect loop if already redirecting or activity is finishing
        if (!isRedirecting && !isFinishing()) {
            isRedirecting = true;
            Log.i(TAG, "User not authenticated or email not verified. Redirecting to LoginActivity.");
            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish(); // Finish MainActivity
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null; // Clean up binding
        Log.d(TAG, "MainActivity onDestroy");
    }

    @Override
    public void logMediaView(PlayerMedia mediaItem) {
        // TODO: Implement history logging
        Log.d(TAG, "History logging requested for: " + (mediaItem != null ? mediaItem.getTitle() : "null media"));
    }
}