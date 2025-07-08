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
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
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
    public static final String ACTION_NOTIFICATION_RECEIVED = "com.example.heal.NOTIFICATION_RECEIVED";

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
            "Be present with your feelings. Check-in now.",
            "Time for a quick emotional pit stop with Heal.",
            "Let's chart your mood for the day. Open Heal to check-in.",
            "A little self-reflection goes a long way. How are you, really?",
            "Unlock insights into your day. Ready for your mood check-in?",
            "Your mental well-being journey continues. Time to log your mood.",
            "Heal is here to listen. What's your current mood?",
            "Just a friendly nudge to check-in with your feelings.",
            "Connect with your inner world. It's mood check-in time!",
            "Small habits, big impact. How's your mood shaping up?",
            "Take a moment for yourself. Log your mood with Heal.",
            "What's the emotional weather like for you today?",
            "Your daily check-in awaits. Let's see how you're doing.",
            "Build a better understanding of you. Time for your mood log.",
            "Ready to tune into your emotions? Heal can help.",
            "A quick pause to acknowledge your feelings. Check-in now.",
            "Track your emotional rhythm with today's mood check-in.",
            "How is your energy today? Log your mood to see the patterns.",
            "Make space for your feelings. It's time to check-in.",
            "The story of your day includes your mood. Let's record it.",
            "Your well-being is a priority. Spare a moment for a mood check-in.",
            "Discover more about yourself. Log your current emotional state.",
            "Keep the momentum going! How are you feeling?",
            "A simple check-in can brighten your perspective. Try it!",
            "Let Heal be your guide. What's your mood telling you?",
            "Pause, breathe, check-in. How are you feeling?",
            "Your feelings matter. Let's give them some attention.",
            "It's that time again! Your friendly mood check-in reminder.",
            "Gain clarity on your emotional landscape. Log your mood.",
            "Step into self-awareness with your daily check-in.",
            "What are your emotions signaling? Time to explore.",
            "A moment for mindfulness and mood logging.",
            "Let's capture today's emotional hue. Check-in with Heal.",
            "Nourish your mind. How are you truly feeling right now?",
            "Your mood diary is calling! What will you write today?",
            "Reflect and record. It's time for your mood update.",
            "Stay connected with yourself. Log your mood.",
            "Unlock the power of knowing your moods. Check-in now.",
            "How's your inner compass pointing today? Log your mood.",
            "One small check-in, one giant leap for self-care.",
            "Embrace your emotions. Time for your daily log.",
            "What's the vibe today? Let Heal know.",
            "Your mental health check-in is here. How are you doing?",
            "Take a minute to center yourself and log your mood.",
            "Every feeling is a message. What's yours saying?",
            "Chart your course to well-being. Start with a mood check-in.",
            "Heal's here for your daily emotional update.",
            "Shine a light on your inner state. Check-in now.",
            "Simple, quick, insightful. Your mood check-in.",
            "Add another data point to your well-being journey.",
            "How's your heart and mind today? Log your mood.",
            "Ready for your daily moment of emotional clarity?",
            "Let's make today count. How are you feeling?",
            "Your emotional well-being adventure continues. Check-in!",
            "A gentle reminder: your feelings are important. Log them.",
            "What's the emotional soundtrack of your day so far?",
            "Tune in, check-in, and thrive. How's your mood?",
            "Time to paint a picture of your current emotional state.",
            "Heal's ready when you are. Let's log that mood.",
            "Another day, another opportunity for self-discovery. Check-in!",
            "How are you navigating your feelings today?",
            "A quick check-in is an act of self-kindness.",
            "Explore the landscape of your emotions. Log your mood.",
            "Your daily dose of introspection is here. How are you?",
            "What's your mood meter saying right now?",
            "Keeping track helps you grow. Time for a mood check-in.",
            "Let's add another entry to your mood journal.",
            "Pause and connect with your inner self. Check-in.",
            "Your emotions are valid and valuable. Record them.",
            "Today's mood check-in: a step towards a healthier you.",
            "How's your spirit today? Let Heal know.",
            "Ready to observe your thoughts and feelings? Check-in.",
            "Take a moment for mental clarity. Log your mood.",
            "Your feelings provide important clues. Time to check-in.",
            "Unlock a deeper connection with yourself. Mood log time!",
            "What's the dominant feeling for you right now?",
            "Heal helps you understand the ebbs and flows. Check-in.",
            "A brief pause for your emotional well-being.",
            "Let's document how you're doing today.",
            "Your mood matters. Give it a moment of your time.",
            "Ready for a quick self-assessment? Log your mood.",
            "The journey inward starts with a simple check-in.",
            "What's the emotional temperature? Time to log it.",
            "Your daily mood insight is just a tap away.",
            "How are you, really and truly? Check-in with Heal.",
            "Make today's mood count. Record it now.",
            "A little check-in can set a positive tone. Try it!",
            "Your emotional data helps you thrive. Log your mood.",
            "Connect, reflect, record. It's check-in time.",
            "What sensations are you aware of right now? Log your mood.",
            "Heal reminder: How's your inner world today?",
            "A moment dedicated to your emotional health.",
            "Let's capture this feeling. Mood check-in.",
            "Track the beautiful complexity of your emotions.",
            "Your daily check-in: an investment in you.",
            "What's your mood whispering (or shouting) today?",
            "Simple check-ins lead to profound understanding.",
            "Time to touch base with your feelings. How are you?",
            "Let Heal be your companion in emotional awareness.",
            "Ready to take your emotional pulse? Check-in now.",
            "Your well-being dashboard awaits an update. Log your mood.",
            "Acknowledge your current state. It's check-in time.",
            "What's the color of your mood today? Let's find out.",
            "Your emotional story is important. Add a new chapter.",
            "A gentle ping to see how you're feeling.",
            "Discover the patterns that shape your days. Check-in.",
            "How are you feeling in this moment? Take time to log.",
            "Your daily mood snapshot is ready to be taken.",
            "One check-in closer to understanding yourself.",
            "What's the emotional climate like for you?",
            "Heal's here for your regular mood update.",
            "A mindful moment to connect with your feelings.",
            "Let's record how your day is unfolding emotionally.",
            "Your mood is a key part of your story. Track it.",
            "Ready for a quick inventory of your emotions?",
            "The path to emotional balance includes regular check-ins.",
            "What's on your heart today? Log your mood.",
            "Heal: Helping you navigate your inner landscape. Check-in.",
            "A small pause for a big impact on your well-being.",
            "Let's take a reading of your current mood.",
            "Your daily emotional check-point is here.",
            "How are you doing on the inside? Time to log.",
            "Unlock insights with every mood entry. Check-in now.",
            "What feeling is most prominent for you at this moment?",
            "Your journey with Heal continues. Let's check-in.",
            "A moment to honor your feelings. Log your mood.",
            "Track your way to greater emotional intelligence.",
            "Your daily mood check: simple, quick, beneficial.",
            "What's your emotional weather forecast today?",
            "Heal is listening. Share your current mood.",
            "A gentle space for your feelings. Time to check-in.",
            "Let's capture the essence of your mood right now.",
            "Your well-being matters. How are you, truly?",
            "Ready to add to your mood history? Check-in.",
            "The more you know your moods, the better you feel.",
            "What's your inner voice saying? Log your mood.",
            "Heal: Your partner in emotional self-discovery.",
            "A quick check-in for a more mindful day.",
            "Let's take stock of your current emotional state.",
            "Your daily reminder to prioritize your feelings.",
            "How's the energy flowing for you today? Check-in.",
            "Unlock the wisdom of your emotions. Log your mood.",
            "What's the mood of the moment? Let Heal know.",
            "Your emotional check-up is due. How are you feeling?",
            "A simple act of self-care: your daily mood log.",
            "Let's get a snapshot of your feelings right now.",
            "Your mood journey is unique. Track it with Heal.",
            "Ready to connect with what you're feeling? Check-in.",
            "The story of your emotions is unfolding. Record it.",
            "What does your inner world look like today? Log your mood.",
            "Heal's daily nudge: Time for your mood check-in!",
            "A brief moment to focus on your emotional well-being.",
            "Let's chart today's mood. How are you doing?",
            "Your feelings are a guide. Time to listen and log."
    };


    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "ReminderBroadcastReceiver onReceive called. Action: " + intent.getAction());

        // Re-schedule alarms if device booted
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Log.d(TAG, "Device booted. Re-scheduling reminders.");
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
            boolean reminderEnabled = sharedPreferences.getBoolean("reminder_enabled", false);
            if (reminderEnabled) {
                // Correctly re-schedule alarms directly from the receiver
                MainActivity.scheduleReminder(context);
            }
            return; // Exit after handling boot
        }

        // --- Standard Alarm Handling ---

        // Re-schedule the alarm for the next day immediately.
        // This ensures that even if the notification is not shown for some reason,
        // the alarm is still set for the future.
        MainActivity.scheduleReminder(context);
        Log.d(TAG, "Alarms have been re-scheduled for the next day.");


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
                    .setSmallIcon(R.drawable.ic_logo_mono)
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