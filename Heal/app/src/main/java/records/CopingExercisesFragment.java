package records;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import com.f9ld3.heal.MainActivity;
import com.f9ld3.heal.R;

import ui.CustomMessageDialogFragment;

public class CopingExercisesFragment extends Fragment {

    // Argument key
    private static final String ARG_SHOW_GROUNDING_DIALOG = "show_grounding_dialog";
    private static final String DIALOG_TAG = "CopingExerciseDialog"; // Define a tag for your dialog

    // UI Elements
    private CardView cardGroundingExercise;
    private CardView cardBreathingExercises;
    private CardView cardMindfulnessMeditation;
    private CardView cardPositiveAffirmations;
    private CardView cardJournaling;
    private CardView cardProgressiveMuscleRelaxation;
    private CardView cardGuidedImagery;
    private CardView cardGratitudePractice;
    private CardView cardMindfulMovement;
    private CardView cardBodyScanMeditation;
    private CardView cardSelfCompassionBreak;
    private CardView cardDigitalDetox;

    private MainActivity mainActivity;
    private CustomMessageDialogFragment activeDialog; // Keep a reference to the active dialog

    public static CopingExercisesFragment newInstance(boolean showGroundingDialog) {
        CopingExercisesFragment fragment = new CopingExercisesFragment();
        Bundle args = new Bundle();
        args.putBoolean(ARG_SHOW_GROUNDING_DIALOG, showGroundingDialog);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof MainActivity) {
            mainActivity = (MainActivity) context;
        } else {
            Toast.makeText(context, "Error: Fragment attached to wrong activity", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_coping_exercises, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initializeViews(view);
        setupClickListeners();

        if (getArguments() != null && getArguments().getBoolean(ARG_SHOW_GROUNDING_DIALOG, false)) {
            new Handler(Looper.getMainLooper()).post(() -> {
                if (isAdded() && getContext() != null) {
                    showGroundingExerciseDialog();
                }
            });
        }
    }

    private void initializeViews(View view) {
        cardGroundingExercise = view.findViewById(R.id.cardGroundingExercise);
        cardBreathingExercises = view.findViewById(R.id.cardBreathingExercises);
        cardMindfulnessMeditation = view.findViewById(R.id.cardMindfulnessMeditation);
        cardPositiveAffirmations = view.findViewById(R.id.cardPositiveAffirmations);
        cardJournaling = view.findViewById(R.id.cardJournaling);
        cardProgressiveMuscleRelaxation = view.findViewById(R.id.cardProgressiveMuscleRelaxation);
        cardGuidedImagery = view.findViewById(R.id.cardGuidedImagery);
        cardGratitudePractice = view.findViewById(R.id.cardGratitudePractice);
        cardMindfulMovement = view.findViewById(R.id.cardMindfulMovement);
        cardBodyScanMeditation = view.findViewById(R.id.cardBodyScanMeditation);
        cardSelfCompassionBreak = view.findViewById(R.id.cardSelfCompassionBreak);
        cardDigitalDetox = view.findViewById(R.id.cardDigitalDetox);
    }

    private void setupClickListeners() {
        cardGroundingExercise.setOnClickListener(v -> showGroundingExerciseDialog());
        cardBreathingExercises.setOnClickListener(v -> showBreathingExercisesDialog());
        // ... set up other click listeners
        cardMindfulnessMeditation.setOnClickListener(v -> showMindfulnessMeditationDialog());
        cardPositiveAffirmations.setOnClickListener(v -> showPositiveAffirmationsDialog());
        cardJournaling.setOnClickListener(v -> showJournalingDialog());
        cardProgressiveMuscleRelaxation.setOnClickListener(v -> showProgressiveMuscleRelaxationDialog());
        cardGuidedImagery.setOnClickListener(v -> showGuidedImageryDialog());
        cardGratitudePractice.setOnClickListener(v -> showGratitudePracticeDialog());
        cardMindfulMovement.setOnClickListener(v -> showMindfulMovementDialog());
        cardBodyScanMeditation.setOnClickListener(v -> showBodyScanMeditationDialog());
        cardSelfCompassionBreak.setOnClickListener(v -> showSelfCompassionBreakDialog());
        cardDigitalDetox.setOnClickListener(v -> showDigitalDetoxDialog());
    }

    private void showExerciseDialog(String title, String message) {
        // Dismiss any existing dialog before showing a new one
        if (activeDialog != null && activeDialog.isVisible()) {
            activeDialog.dismissAllowingStateLoss(); // Use dismissAllowingStateLoss if it might be called after onSaveInstanceState
        }

        activeDialog = CustomMessageDialogFragment.newInstance(
                title,
                message,
                "Close",
                null
        );

        activeDialog.setListener(new CustomMessageDialogFragment.OnMessageDialogListener() {
            @Override
            public void onDialogPositiveClick(DialogFragment dialogFragment) {
                // dialogFragment.dismiss(); // The dialog will dismiss itself if this is not overridden
                if (dialogFragment == activeDialog) {
                    activeDialog = null; // Clear the reference
                }
            }

            @Override
            public void onDialogNegativeClick(DialogFragment dialogFragment) {
                // dialogFragment.dismiss();
                if (dialogFragment == activeDialog) {
                    activeDialog = null; // Clear the reference
                }
            }
        });

        // Ensure FragmentManager is available
        if (isAdded() && getParentFragmentManager() != null) {
            activeDialog.show(getParentFragmentManager(), DIALOG_TAG);
        } else {
            Log.e("CopingExercisesFrag", "Cannot show dialog, fragment not added or FragmentManager is null.");
        }
    }

    // ... your showXYZDialog methods remain the same

    private void showGroundingExerciseDialog() {
        showExerciseDialog(
                "5-4-3-2-1 Grounding Exercise",
                "When you feel overwhelmed, anxious, or disconnected, try the 5-4-3-2-1 technique:\n\n" +
                        "1.  **5 things you can SEE:** Look around and name 5 objects you can see.\n\n" +
                        "2.  **4 things you can FEEL:** Notice 4 things you can feel. (e.g., the texture of your clothes, the chair beneath you, the temperature of the air).\n\n" +
                        "3.  **3 things you can HEAR:** Listen and name 3 sounds you can hear. (e.g., distant traffic, your own breathing, a fan).\n\n" +
                        "4.  **2 things you can SMELL:** Identify 2 things you can smell. (If you can't smell anything, name 2 smells you like).\n\n" +
                        "5.  **1 thing you can TASTE:** Name 1 thing you can taste. (e.g., lingering taste from a drink, or simply the inside of your mouth)."
        );
    }

    private void showBreathingExercisesDialog() {
        showExerciseDialog(
                "Breathing Exercises",
                "Deep breathing can calm your nervous system. Try this simple exercise:\n\n" +
                        "1.  **Inhale:** Breathe in slowly through your nose for 4 seconds.\n\n" +
                        "2.  **Hold:** Hold your breath for 7 seconds.\n\n" +
                        "3.  **Exhale:** Breathe out slowly through your mouth for 8 seconds.\n\n" +
                        "Repeat this cycle 3-5 times."
        );
    }

    private void showMindfulnessMeditationDialog() {
        showExerciseDialog(
                "Mindfulness Meditation",
                "Mindfulness helps you focus on the present moment. Find a quiet place and:\n\n" +
                        "1.  Sit comfortably and close your eyes.\n\n" +
                        "2.  Focus on your breath, noticing the sensation of air entering and leaving your body.\n\n" +
                        "3.  When your mind wanders, gently guide your attention back to your breath without judgment.\n\n" +
                        "Start with 5 minutes and gradually increase the duration."
        );
    }

    private void showPositiveAffirmationsDialog() {
        showExerciseDialog(
                "Positive Affirmations",
                "Challenge negative thoughts with positive statements. Repeat these or create your own:\n\n" +
                        "•   \"I am capable and strong.\"\n\n" +
                        "•   \"I can handle whatever comes my way.\"\n\n" +
                        "•   \"I choose to be happy and love myself today.\"\n\n" +
                        "•   \"My feelings are valid, but they do not control me.\""
        );
    }

    private void showJournalingDialog() {
        showExerciseDialog(
                "Journaling for Clarity",
                "Writing down your thoughts and feelings can provide relief and insight. Try these prompts:\n\n" +
                        "•   What is on my mind right now?\n\n" +
                        "•   What am I grateful for today?\n\n" +
                        "•   Describe a challenge I overcame.\n\n" +
                        "Don't worry about grammar or structure, just write freely."
        );
    }

    private void showProgressiveMuscleRelaxationDialog() {
        showExerciseDialog(
                "Progressive Muscle Relaxation",
                "This technique reduces physical tension. Go through different muscle groups:\n\n" +
                        "1.  **Tense:** Inhale and tense a muscle group (e.g., your hands) for 5-10 seconds.\n\n" +
                        "2.  **Release:** Exhale and completely relax the muscle group, noticing the difference.\n\n" +
                        "Work your way through your body: feet, legs, stomach, arms, shoulders, and face."
        );
    }

    private void showGuidedImageryDialog() {
        showExerciseDialog(
                "Guided Imagery / Visualization",
                "Use your imagination to transport yourself to a calm, safe place.\n\n" +
                        "1.  Close your eyes and imagine a peaceful scene in detail (e.g., a beach, a forest).\n\n" +
                        "2.  Engage all your senses: What do you see, hear, smell, feel, and taste?\n\n" +
                        "3.  Spend a few minutes in your safe place, allowing yourself to relax."
        );
    }

    private void showGratitudePracticeDialog() {
        showExerciseDialog(
                "Gratitude Practice",
                "Shifting focus to gratitude can improve your mood. Each day, identify:\n\n" +
                        "•   **Three things you are grateful for.** They can be big or small.\n\n" +
                        "•   Reflect on why you are grateful for each one.\n\n" +
                        "Consider keeping a gratitude journal to track them."
        );
    }

    private void showMindfulMovementDialog() {
        showExerciseDialog(
                "Mindful Movement",
                "Connect your body and mind with gentle movement.\n\n" +
                        "•   **Stretching:** Gently stretch your muscles, paying attention to the sensations in your body.\n\n" +
                        "•   **Yoga:** Follow a beginner's yoga routine, focusing on your breath with each pose.\n\n" +
                        "•   **Walking:** Go for a walk and pay attention to the movement of your body and the environment around you."
        );
    }

    private void showBodyScanMeditationDialog() {
        showExerciseDialog(
                "Body Scan Meditation",
                "Bring awareness to different parts of your body without judgment.\n\n" +
                        "1.  Lie down comfortably.\n\n" +
                        "2.  Bring your attention to your toes, noticing any sensations (warmth, tingling, pressure).\n\n" +
                        "3.  Slowly move your attention up through your body: feet, legs, torso, arms, and head.\n\n" +
                        "This helps anchor you in your physical self."
        );
    }

    private void showSelfCompassionBreakDialog() {
        showExerciseDialog(
                "Self-Compassion Break",
                "Treat yourself with the same kindness you would offer a friend.\n\n" +
                        "1.  **Acknowledge:** \"This is a moment of suffering.\"\n\n" +
                        "2.  **Common Humanity:** \"Suffering is a part of life. Others feel this way too.\"\n\n" +
                        "3.  **Kindness:** Place a hand over your heart and say, \"May I be kind to myself.\"\n\n"
        );
    }

    private void showDigitalDetoxDialog() {
        showExerciseDialog(
                "Take a Short Digital Detox",
                "Constant notifications and information can be overwhelming. Give your mind a break.\n\n" +
                        "•   Set a timer for 15-30 minutes.\n\n" +
                        "•   Put your phone and other devices away, preferably in another room.\n\n" +
                        "•   Engage in an offline activity: read a book, listen to music, or simply sit in silence.\n\n"
        );
    }


    @Override
    public void onResume() {
        super.onResume();
        if (mainActivity != null) {
            mainActivity.toolbar.setTitle("Coping Exercises");
            mainActivity.navigationView.setCheckedItem(R.id.nav_records);
            mainActivity.MenuTrigger.setVisibility(View.VISIBLE);
            mainActivity.Fab.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onDestroyView() {
        // This is a more appropriate place to dismiss dialogs tied to the view
        if (activeDialog != null && activeDialog.isVisible()) {
            activeDialog.dismissAllowingStateLoss(); // Use dismissAllowingStateLoss for safety
            activeDialog = null; // Clear the reference
        }
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        // While onDestroyView is better for view-related dialogs,
        // you could also put it here as a final fallback, though less common for dialogs.
        // if (activeDialog != null && activeDialog.getDialog() != null && activeDialog.getDialog().isShowing()) {
        // activeDialog.dismissAllowingStateLoss();
        // }
        super.onDestroy();
        mainActivity = null; // Clean up activity reference
    }
}
