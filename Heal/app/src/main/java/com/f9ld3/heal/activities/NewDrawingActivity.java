package com.f9ld3.heal.activities;

import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import com.f9ld3.heal.R;

import java.io.Serializable;
import java.util.HashSet;

import drawing.DrawingCanvasFragment;
import ui.CustomMessageDialogFragment;

public class NewDrawingActivity extends AppCompatActivity implements DrawingCanvasFragment.OnDrawingSavedListener {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_drawing); // You'll need to create this layout

        if (savedInstanceState == null) {
            // Load the DrawingCanvasFragment directly
            DrawingCanvasFragment drawingCanvasFragment = new DrawingCanvasFragment();
            Bundle args = new Bundle();
            // Pass an empty set for existing artwork names as we are starting a new drawing
            args.putSerializable("existingArtworkNames", (Serializable) new HashSet<String>());
            drawingCanvasFragment.setArguments(args);
            drawingCanvasFragment.setTargetFragment(null, 0); // No target fragment needed for direct activity launch

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.drawing_fragment_container, drawingCanvasFragment, "DrawingCanvasFragmentTag")
                    .commit();
        }
    }

    @Override
    public void onDrawingSaved(String imageUri, String artworkName) {
        // Handle the saved drawing, e.g., show a toast and finish the activity
        Toast.makeText(this, "Artwork '" + artworkName + "' saved!", Toast.LENGTH_SHORT).show();
        finish(); // Close the activity after saving
    }

    @Override
    public void onBackPressed() {
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.drawing_fragment_container);
        if (currentFragment instanceof DrawingCanvasFragment) {
            // Replaced AlertDialog with CustomMessageDialogFragment
            CustomMessageDialogFragment dialog = CustomMessageDialogFragment.newInstance(
                    "Exit Canvas",
                    "Are you sure you want to exit without saving? Your changes will be lost.",
                    "Yes",
                    "No"
            );
            dialog.setListener(new CustomMessageDialogFragment.OnMessageDialogListener() {
                @Override
                public void onDialogPositiveClick(DialogFragment dialogFragment) {
                    dialogFragment.dismiss();
                    NewDrawingActivity.super.onBackPressed(); // Allow back press
                }

                @Override
                public void onDialogNegativeClick(DialogFragment dialogFragment) {
                    dialogFragment.dismiss();
                }
            });
            dialog.show(getSupportFragmentManager(), "ExitCanvasDialog");
        } else {
            super.onBackPressed();
        }
    }
}
