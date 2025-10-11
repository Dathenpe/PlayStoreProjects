package com.f9ld3.Zion;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import com.f9ld3.Zion.auth.AuthViewModel;
import com.f9ld3.Zion.auth.LoginActivity;
import com.f9ld3.Zion.databinding.ActivityMainBinding;
import com.f9ld3.Zion.ui.player.PlayerMedia;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity implements com.f9ld3.Zion.ui.player.PlayerFragment.HistoryLogger {

    private static final String TAG = "MainActivity";
    private ActivityMainBinding binding;
    private AuthViewModel authViewModel;
    private NavController navController;
    private boolean isRedirecting = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FirebaseFirestore.setLoggingEnabled(true);
        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        // Check authentication BEFORE inflating layout
        Boolean isAuth = authViewModel.isAuthenticated().getValue();
        Log.d(TAG, "onCreate - isAuthenticated: " + isAuth);

        if (Boolean.FALSE.equals(isAuth) || isAuth == null) {
            Log.w(TAG, "Not authenticated. Redirecting to login.");
            redirectToLogin();
            return; // CRITICAL: Stop execution here
        }

        // Only set up UI if authenticated
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Set up toolbar
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        // Setup toolbar button listeners
        binding.buttonSearchToolbar.setOnClickListener(v -> {
            Log.d(TAG, "Search button clicked");
            Intent searchIntent = new Intent(this, com.f9ld3.Zion.ui.search.SearchActivity.class);
            startActivity(searchIntent);
        });

        binding.buttonNotificationsToolbar.setOnClickListener(v -> {
            Log.d(TAG, "Notifications button clicked");
            Intent notificationsIntent = new Intent(this, com.f9ld3.Zion.ui.notifications.NotificationsActivity.class);
            startActivity(notificationsIntent);
        });

        // SINGLE observer to catch logout events
        authViewModel.isAuthenticated().observe(this, authenticated -> {
            Log.d(TAG, "isAuthenticated changed: " + authenticated + ", isRedirecting: " + isRedirecting);
            if (Boolean.FALSE.equals(authenticated) && !isRedirecting) {
                Log.w(TAG, "Authentication lost. Redirecting to login.");
                isRedirecting = true;
                redirectToLogin();
            }
        });

        authViewModel.getAuthError().observe(this, error -> {
            if (error != null) {
                Log.e(TAG, "Auth error: " + error);
                authViewModel.clearMessages();
            }
        });

        // Setup Navigation Component
        setupNavigation();
    }

    private void setupNavigation() {
        try {
            // Get the NavHostFragment
            NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.nav_host_fragment_activity_main);

            if (navHostFragment == null) {
                Log.e(TAG, "NavHostFragment not found! Check your activity_main.xml");
                return;
            }

            // Get the NavController from the NavHostFragment
            navController = navHostFragment.getNavController();
            Log.d(TAG, "NavController initialized successfully");

            // Connect BottomNavigationView with NavController
            // This automatically handles navigation when items are clicked
            NavigationUI.setupWithNavController(binding.navView, navController);
            Log.d(TAG, "BottomNavigationView connected to NavController");

            // Optional: Listen to destination changes for logging/analytics
            navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
                Log.d(TAG, "Navigated to: " + destination.getLabel() + " (ID: " + destination.getId() + ")");

                // Optional: Show/hide UI elements based on destination
                // Example: Hide bottom nav in certain screens
                /*
                if (destination.getId() == R.id.some_fullscreen_fragment) {
                    binding.navView.setVisibility(View.GONE);
                } else {
                    binding.navView.setVisibility(View.VISIBLE);
                }
                */
            });

            Log.d(TAG, "Navigation setup complete");

        } catch (Exception e) {
            Log.e(TAG, "Error setting up navigation", e);
            e.printStackTrace();
        }
    }

    /**
     * Navigate to player fragment programmatically
     * Call this method from anywhere in MainActivity when you need to show the player
     */
    public void navigateToPlayer() {
        if (navController != null) {
            try {
                Log.d(TAG, "Navigating to player programmatically");
                navController.navigate(R.id.navigation_player);
            } catch (Exception e) {
                Log.e(TAG, "Error navigating to player", e);
            }
        } else {
            Log.e(TAG, "Cannot navigate - NavController is null");
        }
    }

    /**
     * Navigate to feed fragment programmatically
     */
    public void navigateToFeed() {
        if (navController != null) {
            try {
                Log.d(TAG, "Navigating to feed programmatically");
                navController.navigate(R.id.navigation_feed);
            } catch (Exception e) {
                Log.e(TAG, "Error navigating to feed", e);
            }
        }
    }

    /**
     * Navigate to profile fragment programmatically
     */
    public void navigateToProfile() {
        if (navController != null) {
            try {
                Log.d(TAG, "Navigating to profile programmatically");
                navController.navigate(R.id.navigation_profile);
            } catch (Exception e) {
                Log.e(TAG, "Error navigating to profile", e);
            }
        }
    }

    /**
     * Get the NavController for use in other components
     */

    public NavController getNavController() {
        return navController;
    }

    private void redirectToLogin() {
        Log.d(TAG, "redirectToLogin called");
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public boolean onSupportNavigateUp() {
        // Handle up navigation (back button in toolbar)
        return navController != null && navController.navigateUp() || super.onSupportNavigateUp();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
        navController = null;
        isRedirecting = false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        isRedirecting = false;
    }

    // Implement HistoryLogger interface from PlayerFragment
    @Override
    public void logMediaView(com.f9ld3.Zion.ui.player.PlayerMedia mediaItem) {
        Log.d(TAG, "Logging media view: " + mediaItem.getTitle() + " (ID: " + mediaItem.getId() + ")");

        // TODO: Implement actual history logging to Firestore
        // Example implementation:
        /*
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && !user.isAnonymous()) {
            Map<String, Object> historyEntry = new HashMap<>();
            historyEntry.put("userId", user.getUid());
            historyEntry.put("mediaId", mediaItem.getId());
            historyEntry.put("mediaTitle", mediaItem.getTitle());
            historyEntry.put("mediaType", mediaItem.getType());
            historyEntry.put("timestamp", System.currentTimeMillis());

            FirebaseFirestore.getInstance()
                .collection("userHistory")
                .add(historyEntry)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "History logged: " + documentReference.getId());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error logging history", e);
                });
        }
        */
    }
}