package com.f9ld3.Zion.ui.settings;

import android.app.Application;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.preference.PreferenceManager;

public class SettingsViewModel extends AndroidViewModel {

    private static final String TAG = "SettingsViewModel";

    // Example keys for settings
    public static final String KEY_NOTIFICATIONS_ENABLED = "notifications_enabled";
    public static final String KEY_STREAMING_QUALITY = "streaming_quality";
    public static final String KEY_BLOG_NOTIFICATIONS = "blog_notifications";
    public static final String KEY_LIVE_STREAM_NOTIFICATIONS = "live_stream_notifications";

    private final SharedPreferences sharedPreferences;

    // LiveData for individual settings (or a single LiveData for a settings object)
    private final MutableLiveData<Boolean> notificationsEnabled = new MutableLiveData<>();
    private final MutableLiveData<String> streamingQuality = new MutableLiveData<>();
    private final MutableLiveData<Boolean> blogNotifications = new MutableLiveData<>();
    private final MutableLiveData<Boolean> liveStreamNotifications = new MutableLiveData<>();

    public SettingsViewModel(Application application) {
        super(application);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(application);
        loadSettings();

        // Listen for changes in SharedPreferences
        sharedPreferences.registerOnSharedPreferenceChangeListener(preferenceChangeListener);
    }

    private void loadSettings() {
        notificationsEnabled.setValue(sharedPreferences.getBoolean(KEY_NOTIFICATIONS_ENABLED, true));
        streamingQuality.setValue(sharedPreferences.getString(KEY_STREAMING_QUALITY, "auto"));
        blogNotifications.setValue(sharedPreferences.getBoolean(KEY_BLOG_NOTIFICATIONS, true));
        liveStreamNotifications.setValue(sharedPreferences.getBoolean(KEY_LIVE_STREAM_NOTIFICATIONS, true));
        Log.d(TAG, "Settings loaded.");
    }

    private final SharedPreferences.OnSharedPreferenceChangeListener preferenceChangeListener =
            (sharedPrefs, key) -> {
                Log.d(TAG, "Preference changed: " + key);
                if (key.equals(KEY_NOTIFICATIONS_ENABLED)) {
                    notificationsEnabled.setValue(sharedPrefs.getBoolean(key, true));
                } else if (key.equals(KEY_STREAMING_QUALITY)) {
                    streamingQuality.setValue(sharedPrefs.getString(key, "auto"));
                } else if (key.equals(KEY_BLOG_NOTIFICATIONS)) {
                    blogNotifications.setValue(sharedPrefs.getBoolean(key, true));
                } else if (key.equals(KEY_LIVE_STREAM_NOTIFICATIONS)) {
                    liveStreamNotifications.setValue(sharedPrefs.getBoolean(key, true));
                }
            };

    public LiveData<Boolean> getNotificationsEnabled() {
        return notificationsEnabled;
    }

    public LiveData<String> getStreamingQuality() {
        return streamingQuality;
    }

    public LiveData<Boolean> getBlogNotifications() {
        return blogNotifications;
    }

    public LiveData<Boolean> getLiveStreamNotifications() {
        return liveStreamNotifications;
    }

    // Example: Method to update a setting programmatically (though usually done via UI)
    public void setNotificationsEnabled(boolean enabled) {
        sharedPreferences.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled).apply();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener);
        Log.d(TAG, "SettingsViewModel cleared, listener unregistered.");
    }
}