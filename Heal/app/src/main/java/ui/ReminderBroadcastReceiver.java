package ui;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager; // Import LocalBroadcastManager
import androidx.preference.PreferenceManager;

import com.f9ld3.heal.MainActivity;
import com.f9ld3.heal.R;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Random;

public class ReminderBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = "ReminderReceiver";
    public static final String ACTION_NOTIFICATION_RECEIVED = "com.example.heal.NOTIFICATION_RECEIVED"; // Custom action for local broadcast

    private static final String[] REMINDER_MESSAGES = {
            "It's time for your daily mood check-in!",
            "How are you feeling right now? Take a moment to check-in.",
            "A quick check-in can make a big difference. How's your mood?",
            "Don't forget to track your mood today!",
            "Your emotional well-being matters. Time for a mood check-in.",
            "Take a pause and assess your mood. It only takes a minute!",
            "Curious about your mood trends? Record your mood now.",
            "Record your mood to understand yourself better.",
            "A daily mood check-in is a step towards self-awareness.",
            "Check in with your feelings today.",
            "What's on your mind? Log your mood.",
            "How's your mental sunshine today?",
            "Your mood, your data. Track it now.",
            "Mindful moment: how are you truly feeling?",
            "Capture your current emotional state.",
            "A small step, a big insight: check your mood.",
            "Understanding your emotions starts with checking in.",
            "Your daily emotional snapshot awaits.",
            "Don't let your feelings go unnoticed. Check-in.",
            "Give a voice to your emotions by logging your mood.",
            "It's okay to not be okay. But let's track it.",
            "Every mood tells a story. What's yours today?",
            "The journey to self-discovery includes mood tracking.",
            "Your emotional compass: time to check your direction.",
            "Gauge your inner weather. Record your mood.",
            "Feeling good? Feeling meh? Feeling sad? Log it!",
            "Know thyself, start with your mood.",
            "Unlock patterns in your mood by checking in daily.",
            "Your daily reminder to connect with your feelings.",
            "Empower yourself by understanding your moods.",
            "A moment of reflection: how's your mood holding up?",
            "Your feelings are valid. Let's record them.",
            "Document your emotional landscape.",
            "The best way to predict your future mood is to create it, and track it.",
            "Observe your emotions without judgment. Check-in.",
            "Your daily dose of self-awareness: mood check-in!",
            "Heal your mind by understanding its rhythms. Check-in.",
            "What emotion is most present for you right now?",
            "Give yourself the gift of emotional insight. Check-in.",
            "Take control of your emotional journey. Log your mood.",
            "Your personal emotional diary is waiting.",
            "The first step to managing emotions is recognizing them. Check-in.",
            "A mindful pause for your mood.",
            "What color is your mood today? Find out with a check-in.",
            "Track your progress, starting with your mood.",
            "Heal helps you listen to your inner self. Check-in now.",
            "Your emotional health is a priority. Check-in.",
            "Invest in yourself. Start with a mood check-in.",
            "Understand your emotional fluctuations. Log your mood.",
            "Be present with your feelings. Check-in now."
    };


    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "ReminderBroadcastReceiver onReceive called. Action: " + intent.getAction());

        // Re-schedule alarms if device booted or package replaced
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()) || Intent.ACTION_MY_PACKAGE_REPLACED.equals(intent.getAction())) {
            Log.d(TAG, "Device booted or package replaced. Re-scheduling reminders.");
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
            boolean reminderEnabled = sharedPreferences.getBoolean("reminder_enabled", false);
            if (reminderEnabled) {
                // To re-schedule alarms after boot/package replaced, we need to ensure MainActivity's
                // scheduleReminder method is called. Since we cannot directly call Activity methods
                // from a BroadcastReceiver if the Activity is not running, we rely on MainActivity's
                // onCreate/onResume to re-schedule.
                // For a truly robust solution that doesn't rely on the Activity being launched,
                // you would typically use WorkManager or a dedicated Service to re-schedule.
                // For now, we'll assume MainActivity's lifecycle handles it.
                Log.d(TAG, "Boot/Package Replaced: Reminder setting is " + reminderEnabled + ". MainActivity will re-schedule on launch.");
                // Optionally, you could start MainActivity here if it's critical for scheduling
                // Intent launchMainActivity = new Intent(context, MainActivity.class);
                // launchMainActivity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                // context.startActivity(launchMainActivity);
            }
            return; // Exit as we've handled boot/package replaced
        }

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        boolean reminderEnabled = sharedPreferences.getBoolean("reminder_enabled", false);
        Log.d(TAG, "Reminder enabled setting: " + reminderEnabled);

        SharedPreferences appPrefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
        long lastMoodCheckinDateMillis = appPrefs.getLong(MainActivity.KEY_LAST_MOOD_CHECKIN_DATE, 0);

        Calendar lastCheckinCalendar = Calendar.getInstance();
        lastCheckinCalendar.setTimeInMillis(lastMoodCheckinDateMillis);

        Calendar todayCalendar = Calendar.getInstance();
        todayCalendar.set(Calendar.HOUR_OF_DAY, 0);
        todayCalendar.set(Calendar.MINUTE, 0);
        todayCalendar.set(Calendar.SECOND, 0);
        todayCalendar.set(Calendar.MILLISECOND, 0);

        Calendar checkinDayCalendar = (Calendar) lastCheckinCalendar.clone();
        checkinDayCalendar.set(Calendar.HOUR_OF_DAY, 0);
        checkinDayCalendar.set(Calendar.MINUTE, 0);
        checkinDayCalendar.set(Calendar.SECOND, 0);
        checkinDayCalendar.set(Calendar.MILLISECOND, 0);

        boolean hasCheckedInToday = checkinDayCalendar.equals(todayCalendar);
        Log.d(TAG, "Last mood check-in: " + (lastMoodCheckinDateMillis > 0 ? new java.util.Date(lastMoodCheckinDateMillis).toString() : "Never"));
        Log.d(TAG, "Has checked in today: " + hasCheckedInToday);

        if (reminderEnabled && !hasCheckedInToday) {
            Intent launchIntent = new Intent(context, MainActivity.class);
            launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

            PendingIntent pendingIntent = PendingIntent.getActivity(
                    context,
                    0,
                    launchIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            Random random = new Random();
            String notificationMessage = REMINDER_MESSAGES[random.nextInt(REMINDER_MESSAGES.length)];

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, MainActivity.REMINDER_CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_logo_mono) // Ensure this drawable exists and is monochrome
                    .setContentTitle("Heal: Daily Check-in")
                    .setContentText(notificationMessage)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true);

            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

            if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU ||
                    ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                int notificationId = (int) System.currentTimeMillis();
                notificationManager.notify(notificationId, builder.build());
                Log.d(TAG, "Notification sent with ID: " + notificationId);

                // ALWAYS save notification to SharedPreferences directly from here
                SharedPreferences prefs = context.getSharedPreferences(MainActivity.PREFS_NOTIFICATIONS, Context.MODE_PRIVATE);
                Gson gson = new Gson();
                String json = prefs.getString(MainActivity.KEY_RECENT_NOTIFICATIONS, "[]");
                Type type = new TypeToken<List<String>>() {}.getType();
                List<String> notifications = gson.fromJson(json, type);

                if (notifications == null) {
                    notifications = new ArrayList<>();
                }
                String timestamp = android.text.format.DateFormat.format("MMM dd, hh:mm a", System.currentTimeMillis()).toString();
                notifications.add(0, timestamp + ": " + notificationMessage);
                while (notifications.size() > 5) {
                    notifications.remove(notifications.size() - 1);
                }
                prefs.edit().putString(MainActivity.KEY_RECENT_NOTIFICATIONS, gson.toJson(notifications)).apply();
                Log.d(TAG, "Notification saved to shared preferences.");

                // Send a local broadcast to notify MainActivity to update its UI
                Intent localBroadcastIntent = new Intent(ACTION_NOTIFICATION_RECEIVED);
                LocalBroadcastManager.getInstance(context).sendBroadcast(localBroadcastIntent);
                Log.d(TAG, "Local broadcast sent: " + ACTION_NOTIFICATION_RECEIVED);

            } else {
                Log.w(TAG, "Notification permission not granted, cannot show notification.");
            }
        } else {
            Log.d(TAG, "Notification skipped: Reminder enabled = " + reminderEnabled + ", Has checked in today = " + hasCheckedInToday);
        }
    }
}
