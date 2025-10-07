package com.f9ld3.Zion.ui.settings;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.Preference;
import androidx.lifecycle.ViewModelProvider;

import com.f9ld3.Zion.R;
import com.f9ld3.Zion.auth.AuthViewModel; // Import AuthViewModel
import com.f9ld3.Zion.ui.profile.EditProfileActivity; // Import EditProfileActivity
import com.f9ld3.Zion.auth.LoginActivity; // Import LoginActivity for sign out navigation

/**
 * Fragment to display application settings using PreferenceFragmentCompat.
 */
public class SettingsFragment extends PreferenceFragmentCompat {

    private SettingsViewModel settingsViewModel;
    private AuthViewModel authViewModel; // New: AuthViewModel for sign out

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey);

        settingsViewModel = new ViewModelProvider(this).get(SettingsViewModel.class);
        authViewModel = new ViewModelProvider(requireActivity()).get(AuthViewModel.class); // Scoped to activity for consistent auth state

        // Example: Observe a setting and react to changes (optional, as PreferenceFragment handles UI updates)
        settingsViewModel.getNotificationsEnabled().observe(this, enabled -> {
            // You could log this or trigger other logic if needed
            // Log.d("SettingsFragment", "Notifications enabled: " + enabled);
        });

        // Add click listener for "Edit Profile"
        Preference editProfilePreference = findPreference("edit_profile_shortcut");
        if (editProfilePreference != null) {
            editProfilePreference.setOnPreferenceClickListener(preference -> {
                startActivity(new Intent(requireContext(), EditProfileActivity.class));
                return true;
            });
        }

        // Add click listener for "Change Password"
        Preference changePasswordPreference = findPreference("change_password");
        if (changePasswordPreference != null) {
            changePasswordPreference.setOnPreferenceClickListener(preference -> {
                // TODO: Implement a dialog or new activity for changing password
                // For now, a simple toast
                Toast.makeText(getContext(), "Change Password functionality coming soon!", Toast.LENGTH_SHORT).show();
                return true;
            });
        }

        // Add click listener for "Sign Out"
        Preference signOutPreference = findPreference("sign_out_shortcut");
        if (signOutPreference != null) {
            signOutPreference.setOnPreferenceClickListener(preference -> {
                authViewModel.signOut();
                // After signing out, navigate to LoginActivity or MainActivity (which will redirect to login)
                Intent intent = new Intent(requireActivity(), LoginActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK); // Clear back stack
                startActivity(intent);
                requireActivity().finish(); // Finish current activity
                return true;
            });
        }

        // Example: Add a click listener to a specific preference
        Preference feedbackPreference = findPreference("feedback_key");
        if (feedbackPreference != null) {
            feedbackPreference.setOnPreferenceClickListener(preference -> {
                Toast.makeText(getContext(), "Opening feedback form...", Toast.LENGTH_SHORT).show();
                // TODO: Implement actual feedback mechanism (e.g., open email client, web form)
                return true;
            });
        }
    }
}