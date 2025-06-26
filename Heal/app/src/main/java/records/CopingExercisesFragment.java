package records;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.example.heal.MainActivity;
import com.example.heal.R;

public class CopingExercisesFragment extends Fragment {

    // Argument key
    private static final String ARG_SHOW_GROUNDING_DIALOG = "show_grounding_dialog";

    // Existing CardViews
    private CardView cardGroundingExercise;
    private CardView cardBreathingExercises;
    private CardView cardMindfulnessMeditation;
    private CardView cardPositiveAffirmations;

    // NEW CardViews (8 more)
    private CardView cardJournaling;
    private CardView cardProgressiveMuscleRelaxation;
    private CardView cardGuidedImagery;
    private CardView cardGratitudePractice;
    private CardView cardMindfulMovement;
    private CardView cardBodyScanMeditation;
    private CardView cardSelfCompassionBreak;
    private CardView cardDigitalDetox;

    private MainActivity mainActivity;

    // --- Static factory method to create an instance with arguments ---
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
            Toast.makeText(context, "Error: CopingExercisesFragment attached to wrong activity", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        View view = inflater.inflate(R.layout.fragment_coping_exercises, container, false);

        // Initialize CardViews
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

        return view;
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Set up click listeners for all exercise cards
        cardGroundingExercise.setOnClickListener(v -> showGroundingExerciseDialog());
        cardBreathingExercises.setOnClickListener(v -> showExerciseDialog(
                "Breathing Exercises",
                "Try 4-7-8 Breathing:\n\n" +
                        "1.  Inhale quietly through your nose for a count of 4.\n" +
                        "2.  Hold your breath for a count of 7.\n" +
                        "3.  Exhale completely through your mouth, making a whoosh sound, for a count of 8.\n" +
                        "4.  Repeat for 3-4 cycles. This technique can help calm the nervous system."
        ));
        cardMindfulnessMeditation.setOnClickListener(v -> showExerciseDialog(
                "Mindfulness Meditation",
                "Find a quiet place and sit comfortably. Close your eyes or soften your gaze.\n\n" +
                        "1.  **Focus on your breath:** Notice the sensation of air entering and leaving your body.\n" +
                        "2.  **Observe thoughts:** When your mind wanders, gently bring your attention back to your breath without judgment.\n" +
                        "3.  **Notice sensations:** Expand your awareness to include sounds, bodily sensations, and emotions, observing them as they arise and pass.\n\n" +
                        "Start with 5-10 minutes daily and gradually increase the duration."
        ));
        cardPositiveAffirmations.setOnClickListener(v -> showExerciseDialog(
                "Positive Affirmations",
                "Choose a few positive statements that resonate with you and repeat them daily. You can say them aloud, write them down, or think them silently.\n\n" +
                        "Examples:\n" +
                        "-   'I am strong and capable.'\n" +
                        "-   'I choose peace and calm.'\n" +
                        "-   'I am worthy of love and happiness.'\n" +
                        "-   'I can handle whatever comes my way.'\n\n" +
                        "Consistency is key to shifting your mindset."
        ));
        cardJournaling.setOnClickListener(v -> showExerciseDialog(
                "Journaling for Emotional Release",
                "Find a quiet time and space to write freely without judgment. You can write about:\n\n" +
                        "1.  **Your feelings:** What emotions are you experiencing right now? Why do you think you feel this way?\n" +
                        "2.  **Recent events:** Describe a challenging or positive event and how it impacted you.\n" +
                        "3.  **Future aspirations:** What are your hopes, dreams, or goals? What steps can you take?\n\n" +
                        "The goal is to express yourself authentically and gain insight into your inner world."
        ));
        cardProgressiveMuscleRelaxation.setOnClickListener(v -> showExerciseDialog(
                "Progressive Muscle Relaxation (PMR)",
                "Lie down or sit comfortably. You will systematically tense and relax different muscle groups.\n\n" +
                        "1.  **Start with your feet:** Tense the muscles in your feet for 5 seconds, then completely relax them for 15-20 seconds.\n" +
                        "2.  **Move up your body:** Continue with your calves, thighs, glutes, abdomen, chest, arms, hands, shoulders, neck, and face.\n" +
                        "3.  **Notice the difference:** Pay attention to the contrast between tension and relaxation.\n\n" +
                        "PMR helps release physical tension and promotes overall relaxation."
        ));
        cardGuidedImagery.setOnClickListener(v -> showExerciseDialog(
                "Guided Imagery & Visualization",
                "Find a comfortable position and close your eyes. Imagine a peaceful and safe place.\n\n" +
                        "1.  **Engage your senses:** What do you see, hear, smell, feel, and perhaps even taste in this place?\n" +
                        "2.  **Explore the scene:** Walk through your imagined space, noticing details.\n" +
                        "3.  **Feel the calm:** Allow the feelings of peace and relaxation to wash over you.\n\n" +
                        "You can find many guided imagery recordings online or create your own mental escape."
        ));
        cardGratitudePractice.setOnClickListener(v -> showExerciseDialog(
                "Gratitude Practice",
                "Take a few minutes each day to reflect on things you are grateful for. You can:\n\n" +
                        "1.  **Keep a gratitude journal:** Write down 3-5 things you're thankful for each day.\n" +
                        "2.  **Express thanks:** Tell someone you appreciate them or write a thank-you note.\n" +
                        "3.  **Mindful appreciation:** Take a moment to truly appreciate a simple pleasure, like a warm drink or a beautiful sky.\n\n" +
                        "Cultivating gratitude can significantly boost your mood and well-being."
        ));
        cardMindfulMovement.setOnClickListener(v -> showExerciseDialog(
                "Mindful Movement (e.g., Stretching)",
                "Engage in gentle physical activity, bringing full awareness to your body's sensations.\n\n" +
                        "1.  **Slow stretching:** Gently stretch different parts of your body, noticing the feeling of elongation and release.\n" +
                        "2.  **Walking meditation:** Walk slowly, paying attention to the sensation of your feet on the ground, your breath, and the environment around you.\n" +
                        "3.  **Yoga or Tai Chi:** Explore these practices for a structured approach to mindful movement.\n\n" +
                        "The goal is to connect with your body and its movements without judgment."
        ));
        cardBodyScanMeditation.setOnClickListener(v -> showExerciseDialog(
                "Body Scan Meditation",
                "Lie down comfortably or sit upright. Close your eyes.\n\n" +
                        "1.  **Bring awareness to your feet:** Notice any sensations without trying to change them.\n" +
                        "2.  **Slowly move your attention:** Gradually move your awareness up through your legs, torso, arms, hands, neck, and head.\n" +
                        "3.  **Observe sensations:** Pay attention to tingling, warmth, coolness, pressure, or absence of sensation.\n\n" +
                        "This practice helps you become more attuned to your body and release tension."
        ));
        cardSelfCompassionBreak.setOnClickListener(v -> showExerciseDialog(
                "Self-Compassion Break",
                "When you're struggling or feeling pain (emotional or physical), try this:\n\n" +
                        "1.  **Mindfulness:** 'This is a moment of suffering.' (Acknowledge the pain).\n" +
                        "2.  **Common Humanity:** 'Suffering is a part of life.' (Remember you're not alone).\n" +
                        "3.  **Self-Kindness:** 'May I be kind to myself.' (Offer yourself comfort, e.g., a hand on your heart).\n\n" +
                        "Treat yourself with the same kindness you would offer a dear friend."
        ));
        cardDigitalDetox.setOnClickListener(v -> showExerciseDialog(
                "Digital Detox",
                "Set aside dedicated time each day or week to disconnect from screens and digital devices.\n\n" +
                        "1.  **Schedule screen-free time:** Designate specific hours or days for no phone, computer, or TV.\n" +
                        "2.  **Engage in offline activities:** Read a book, go for a walk, spend time in nature, pursue a hobby, or connect with loved ones in person.\n" +
                        "3.  **Notice the difference:** Pay attention to how you feel mentally and emotionally when disconnected.\n\n" +
                        "A digital detox can reduce mental fatigue and improve focus."
        ));

        // Optional: Set toolbar title
        if (mainActivity != null) {
            mainActivity.toolbar.setTitle("Coping Exercises");
        }

        // --- Check if the grounding dialog should be shown ---
        if (getArguments() != null && getArguments().getBoolean(ARG_SHOW_GROUNDING_DIALOG, false)) {
            showGroundingExerciseDialog();
            // Clear the argument so it doesn't show again on rotation/re-creation
            getArguments().remove(ARG_SHOW_GROUNDING_DIALOG);
        }
    }

    private void showExerciseDialog(String title, String instructions) {
        if (getContext() == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(title);

        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 30, 50, 30);

        TextView instructionsTv = new TextView(getContext());
        instructionsTv.setText(instructions);
        instructionsTv.setTextSize(16f);
        instructionsTv.setTextColor(getResources().getColor(R.color.text_color_primary));
        instructionsTv.setMovementMethod(new ScrollingMovementMethod());

        ScrollView scrollView = new ScrollView(getContext());
        scrollView.addView(instructionsTv);
        layout.addView(scrollView);

        builder.setView(layout);

        builder.setPositiveButton("Got It!", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void showGroundingExerciseDialog() {
        // This now simply calls the generic showExerciseDialog with specific content
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

    private void showToast(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onResume(){
        mainActivity.toolbar.setTitle("Coping Exercises");
        super.onResume();
    }
}