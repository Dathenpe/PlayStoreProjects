package com.f9ld3.Zion;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton; // For Search and Notifications
import android.widget.TextView; // For the 'ion' text part of the logo
import android.widget.ImageView; // For the 'Z' image part of the logo
import android.widget.Toast;

import com.f9ld3.Zion.data.HistoryItem;
import com.f9ld3.Zion.ui.player.PlayerMedia;
import com.f9ld3.Zion.ui.player.PlayerFragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.FirebaseApp;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.f9ld3.Zion.databinding.ActivityMainBinding;

// Implement the HistoryLogger interface defined in PlayerFragment
public class MainActivity extends AppCompatActivity implements PlayerFragment.HistoryLogger {

    private static final String TAG = "MainActivity";
    private ActivityMainBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    // 🔥 UPDATED Fields for the custom logo and action buttons in the Toolbar
    private ImageButton searchButton;
    private ImageButton notificationsButton;
    private ImageView logoImage; // 'Z' image part of the logo
    private TextView logoText;  // 'ion' text part of the logo


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize Firebase instances
        FirebaseApp.initializeApp(this);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        signInAnonymouslyIfNeeded();

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 🔥 FIND all custom TOOLBAR COMPONENTS
        searchButton = binding.toolbar.findViewById(R.id.button_search_toolbar);
        notificationsButton = binding.toolbar.findViewById(R.id.button_notifications_toolbar);
        logoImage = binding.toolbar.findViewById(R.id.logo_image); // Find the new ImageView
        logoText = binding.toolbar.findViewById(R.id.logo_text_ion); // Find the new TextView

        // Set up the Toolbar
        setSupportActionBar(binding.toolbar);

        // Ensure the default system title is suppressed since we are using custom views
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        // Set up Navigation Controller
        BottomNavigationView navView = binding.navView;
        NavHostFragment navHostFragment =
                (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment_activity_main);
        NavController navController = navHostFragment.getNavController();

        // Define top-level destinations
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_feed, R.id.navigation_player, R.id.navigation_profile)
                .build();

        // Connect the NavController to the BottomNavigationView
        NavigationUI.setupWithNavController(navView, navController);

        // Custom Title and Action Management on destination change
        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {

            // Ensure both logo components are visible when navigating
            logoImage.setVisibility(View.VISIBLE);
            logoText.setVisibility(View.VISIBLE);

            // Set the internal title for accessibility (e.g., TalkBack)
            if (getSupportActionBar() != null) {
                // Use the full app name for the system accessibility title
                getSupportActionBar().setTitle("Zion");
            }
        });

        // 🔥 Search Button Click Listener
        searchButton.setOnClickListener(v -> {
            Log.d(TAG, "Global Search button clicked. Launching search UI...");
            Toast.makeText(this, "Launching Global Search UI...", Toast.LENGTH_SHORT).show();

            // TODO: Replace with navigation action
        });

        // 🔥 Notifications Button Click Listener
        notificationsButton.setOnClickListener(v -> {
            Log.d(TAG, "Notifications button clicked. Launching notifications UI...");
            Toast.makeText(this, "Launching Notifications UI...", Toast.LENGTH_SHORT).show();

            // TODO: Replace with navigation action
        });
    }

    // --- PlayerFragment.HistoryLogger Implementation (Existing Logic) ---
    @Override
    public void logMediaView(PlayerMedia mediaItem) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null || mediaItem == null) {
            Log.w(TAG, "Cannot log history: User is null or mediaItem is null.");
            return;
        }

        // 1. Create a HistoryItem object
        HistoryItem historyItem = new HistoryItem(
                mediaItem.getId(),
                mediaItem.getTitle(),
                mediaItem.getType(),
                mediaItem.getAuthorName(), // Assuming this is the uploader's display name
                mediaItem.getThumbnailUrl(),
                Timestamp.now()
        );
        // 2. Write to Firestore: users/{userId}/history/{mediaId}
        db.collection("users")
                .document(user.getUid())
                .collection("history")
                .document(mediaItem.getId())
                .set(historyItem)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "History logged for media ID: " + mediaItem.getId()))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to log history for media ID: " + mediaItem.getId(), e));
    }

    private void signInAnonymouslyIfNeeded() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            // Sign in anonymously if no user is authenticated
            mAuth.signInAnonymously()
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "signInAnonymously:success. UID: " + mAuth.getCurrentUser().getUid());
                        } else {
                            Log.w(TAG, "signInAnonymously:failure", task.getException());
                        }
                    });
        } else {
            Log.d(TAG, "User already signed in with UID: " + currentUser.getUid());
        }
    }
}