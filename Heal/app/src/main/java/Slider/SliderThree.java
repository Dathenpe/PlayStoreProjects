package Slider;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import com.f9ld3.heal.R;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SliderThree extends Fragment {

    private String[] textContent;
    private TypedArray shapeDrawables;
    private Random random = new Random();
    private List<AnimatorSet> animatorSets = new ArrayList<>();

    public SliderThree() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_slider_three, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getContext() != null) {
            textContent = getContext().getResources().getStringArray(R.array.positive_affirmations);
            shapeDrawables = getContext().getResources().obtainTypedArray(R.array.material_shapes);
        }

        TextView titleTextView = view.findViewById(R.id.slideThreeTitle);
        TextView descriptionTextView = view.findViewById(R.id.slideThreeDescription);
        ConstraintLayout parentLayout = view.findViewById(R.id.parentLayout);

        if (titleTextView != null && descriptionTextView != null) {
            updateRandomContent(titleTextView, descriptionTextView);
        }

        if (parentLayout != null && shapeDrawables != null) {
            parentLayout.post(() -> addAndAnimateShapes(parentLayout, 2));
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (shapeDrawables != null) {
            shapeDrawables.recycle();
        }
        for (AnimatorSet animatorSet : animatorSets) {
            animatorSet.cancel();
        }
    }

    private void updateRandomContent(TextView title, TextView description) {
        if (textContent != null && textContent.length > 0) {
            int randomIndex = random.nextInt(textContent.length);
            String affirmation = textContent[randomIndex];
            String[] parts = affirmation.split("–", 2);
            if (parts.length == 2) {
                title.setText(parts[0].trim());
                description.setText("–" + parts[1].trim());
            } else {
                title.setText("Positive Affirmation");
                description.setText(affirmation);
            }
        }
    }

    private void addAndAnimateShapes(ConstraintLayout parentLayout, int numberOfShapes) {
        // Find the TextView to position shapes relative to
        TextView descriptionTextView = parentLayout.findViewById(R.id.slideThreeDescription);
        int descriptionBottom = descriptionTextView != null ? descriptionTextView.getBottom() : 0;
        int parentWidth = parentLayout.getWidth();
        int parentHeight = parentLayout.getHeight();
        int size = (int) (50 * getResources().getDisplayMetrics().density);

        for (int i = 0; i < numberOfShapes; i++) {
            ImageView shape = new ImageView(getContext());
            ConstraintLayout.LayoutParams layoutParams = new ConstraintLayout.LayoutParams(size, size);
            shape.setLayoutParams(layoutParams);

            // Set random shape and color
            int randomIndex = random.nextInt(shapeDrawables.length());
            int resourceId = shapeDrawables.getResourceId(randomIndex, 0);
            if (resourceId != 0) {
                shape.setImageResource(resourceId);
            }
            int red = 50 + random.nextInt(200);
            int green = 50 + random.nextInt(200);
            int blue = 50 + random.nextInt(200);
            shape.setColorFilter(Color.rgb(red, green, blue));

            // Set random start positions below the text
            float startX = random.nextFloat() * (parentWidth - size);
            float startY = descriptionBottom + random.nextFloat() * (parentHeight - descriptionBottom - size);
            shape.setTranslationX(startX);
            shape.setTranslationY(startY);

            parentLayout.addView(shape);

            // Animate each shape independently
            float endX = random.nextFloat() * (parentWidth - size);
            float endY = descriptionBottom + random.nextFloat() * (parentHeight - descriptionBottom - size);

            ObjectAnimator animatorX = ObjectAnimator.ofFloat(shape, "translationX", startX, endX);
            ObjectAnimator animatorY = ObjectAnimator.ofFloat(shape, "translationY", startY, endY);
            ObjectAnimator animatorRotation = ObjectAnimator.ofFloat(shape, "rotation", 0f, 360f);

            long duration = 5000 + random.nextInt(2000); // Faster movement
            animatorX.setDuration(duration);
            animatorY.setDuration(duration);
            animatorRotation.setDuration(10000 + random.nextInt(5000)); // Faster rotation

            animatorX.setRepeatCount(ObjectAnimator.INFINITE);
            animatorY.setRepeatCount(ObjectAnimator.INFINITE);
            animatorRotation.setRepeatCount(ObjectAnimator.INFINITE);

            animatorX.setRepeatMode(ObjectAnimator.REVERSE);
            animatorY.setRepeatMode(ObjectAnimator.REVERSE);

            AnimatorSet animatorSet = new AnimatorSet();
            animatorSet.playTogether(animatorX, animatorY, animatorRotation);
            animatorSet.start();
            animatorSets.add(animatorSet);
        }
    }
}