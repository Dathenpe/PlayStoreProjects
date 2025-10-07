// MultipleFiles/MainActivity.java
package com.f9ld3.Zion;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.f9ld3.Zion.auth.AuthViewModel;
import com.f9ld3.Zion.auth.LoginActivity;
import com.f9ld3.Zion.databinding.ActivityMainBinding;

import com.f9ld3.Zion.ui.feed.FeedFragment;
import com.f9ld3.Zion.ui.player.PlayerFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser ;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private ActivityMainBinding binding;
    private AuthViewModel authViewModel;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();
        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        // ENHANCED: Strict auth check on start - No anonymous or null users allowed
        FirebaseUser  currentUser  = mAuth.getCurrentUser ();
        if (currentUser  == null || currentUser .isAnonymous()) {
            Log.w(TAG, "No registered user or anonymous detected on app start. Forcing sign-out and redirect to login.");
            mAuth.signOut();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return; // Exit onCreate immediately
        }

        // Observe authentication changes (e.g., if signed out mid-session)
        authViewModel.isAuthenticated().observe(this, isAuthenticated -> {
            if (isAuthenticated == null || !isAuthenticated) {
                Log.d(TAG, "User  no longer authenticated. Redirecting to login.");
                startActivity(new Intent(this, LoginActivity.class));
                finish();
            }
        });

        // Observe errors (e.g., verification issues)
        authViewModel.getAuthError().observe(this, error -> {
            if (error != null) {
                Log.e(TAG, "Auth error in MainActivity: " + error);
                // Optionally show dialog or toast, but redirect if critical
                if (error.contains("verification")) {
                    // Could navigate to profile for resend, but for now log
                }
                authViewModel.clearAuthError();
            }
        });

        // Bottom navigation setup
        BottomNavigationView navView = findViewById(R.id.nav_view);
        navView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                Fragment selectedFragment = null;
                int itemId = item.getItemId();

                if (itemId == R.id.navigation_feed) {
                    selectedFragment = new FeedFragment();
                } else if (itemId == R.id.navigation_player) {
                    // Check auth before allowing player access (e.g., uploads)
                    if (!authViewModel.canPerformAuthenticatedAction()) {
                        Log.w(TAG, "Unauthenticated attempt to access player. Redirecting to login.");
                        startActivity(new Intent(MainActivity.this, LoginActivity.class));
                        finish();
                        return true;
                    }
                    selectedFragment = new PlayerFragment();
                } else if (itemId == R.id.navigation_profile) {
                    selectedFragment = new com.f9ld3.Zion.ui.profile.ProfileFragment();
                }

                if (selectedFragment != null) {
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.nav_host_fragment_activity_main, selectedFragment)
                            .commit();
                }

                return true;
            }
        });

        // Default to Feed
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.nav_host_fragment_activity_main, new FeedFragment())
                    .commit();
            navView.setSelectedItemId(R.id.navigation_feed);
        }

        // REMOVED: Any anonymous user handling in history or navigation
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (binding != null) {
            binding = null;
        }
    }
}