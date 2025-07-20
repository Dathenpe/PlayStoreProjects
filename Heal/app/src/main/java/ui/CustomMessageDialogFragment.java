package ui;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView; // Import ImageView
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.bumptech.glide.Glide; // Import Glide
import com.f9ld3.heal.R;

public class CustomMessageDialogFragment extends DialogFragment {

    // Argument keys
    private static final String ARG_TITLE = "title";
    private static final String ARG_MESSAGE = "message";
    private static final String ARG_POSITIVE_BTN_TEXT = "positive_btn_text";
    private static final String ARG_NEGATIVE_BTN_TEXT = "negative_btn_text";
    private static final String ARG_CUSTOM_LAYOUT_RES_ID = "custom_layout_res_id";
    private static final String ARG_IMAGE_URL = "image_url"; // New argument for image URL
    private static final String ARG_IMAGE_NAME = "image_name"; // New argument for image name

    // Listener for dialog actions
    public interface OnMessageDialogListener {
        void onDialogPositiveClick(DialogFragment dialog);
        void onDialogNegativeClick(DialogFragment dialog);
    }

    private OnMessageDialogListener listener;

    // Factory method for standard message dialog
    public static CustomMessageDialogFragment newInstance(String title, @Nullable String message, String positiveBtnText, @Nullable String negativeBtnText) {
        return newInstance(title, message, positiveBtnText, negativeBtnText, 0, null, null);
    }

    // Factory method for dialog with custom layout (e.g., image viewer)
    public static CustomMessageDialogFragment newInstance(String title, @Nullable String message, String positiveBtnText, @Nullable String negativeBtnText, int customLayoutResId, @Nullable String imageUrl, @Nullable String imageName) {
        CustomMessageDialogFragment fragment = new CustomMessageDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TITLE, title);
        args.putString(ARG_MESSAGE, message);
        args.putString(ARG_POSITIVE_BTN_TEXT, positiveBtnText);
        args.putString(ARG_NEGATIVE_BTN_TEXT, negativeBtnText);
        if (customLayoutResId != 0) {
            args.putInt(ARG_CUSTOM_LAYOUT_RES_ID, customLayoutResId);
        }
        if (imageUrl != null) {
            args.putString(ARG_IMAGE_URL, imageUrl);
        }
        if (imageName != null) {
            args.putString(ARG_IMAGE_NAME, imageName);
        }
        fragment.setArguments(args);
        return fragment;
    }

    public void setListener(OnMessageDialogListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view;
        Bundle args = getArguments();
        int customLayoutResId = 0;
        if (args != null) {
            customLayoutResId = args.getInt(ARG_CUSTOM_LAYOUT_RES_ID, 0);
        }

        if (customLayoutResId != 0) {
            // If a custom layout ID is provided, inflate it
            view = inflater.inflate(customLayoutResId, container, false);
        } else {
            // Otherwise, inflate the default message dialog layout
            view = inflater.inflate(R.layout.dialog_custom_message, container, false);
        }

        // Make the dialog background transparent for rounded corners
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        }

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bundle args = getArguments();
        int customLayoutResId = 0;
        if (args != null) {
            customLayoutResId = args.getInt(ARG_CUSTOM_LAYOUT_RES_ID, 0);
        }

        // Handle views based on whether a custom layout is used
        if (customLayoutResId == R.layout.dialog_image_viewer) {
            // Specific logic for the image viewer dialog
            ImageView fullScreenImageView = view.findViewById(R.id.fullScreenImageView);
            TextView imageViewerNameTextView = view.findViewById(R.id.imageViewerNameTextView);
            Button buttonPositive = view.findViewById(R.id.buttonPositive); // Assuming the close button is R.id.buttonPositive

            if (args != null) {
                String imageUrl = args.getString(ARG_IMAGE_URL);
                String imageName = args.getString(ARG_IMAGE_NAME);

                if (fullScreenImageView != null && imageUrl != null && !imageUrl.isEmpty()) {
                    Glide.with(this)
                            .load(imageUrl)
                            .placeholder(R.drawable.ic_default_contact_avatar)
                            .error(R.drawable.ic_default_contact_avatar)
                            .into(fullScreenImageView);
                }

                if (imageViewerNameTextView != null && imageName != null) {
                    imageViewerNameTextView.setText(imageName);
                }

                if (buttonPositive != null) {
                    buttonPositive.setText(args.getString(ARG_POSITIVE_BTN_TEXT, "Close")); // Default to "Close"
                    buttonPositive.setOnClickListener(v -> {
                        if (listener != null) {
                            listener.onDialogPositiveClick(this);
                        }
                        dismiss();
                    });
                }
            }
        } else {
            // Standard message dialog logic
            TextView dialogTitle = view.findViewById(R.id.dialogTitle);
            TextView dialogMessage = view.findViewById(R.id.dialogMessage);
            Button buttonPositive = view.findViewById(R.id.buttonPositive);
            Button buttonNegative = view.findViewById(R.id.buttonNegative);

            if (args != null) {
                if (dialogTitle != null) {
                    dialogTitle.setText(args.getString(ARG_TITLE));
                }

                String message = args.getString(ARG_MESSAGE);
                if (dialogMessage != null) {
                    if (message != null && !message.isEmpty()) {
                        dialogMessage.setText(message);
                        dialogMessage.setVisibility(View.VISIBLE);
                    } else {
                        dialogMessage.setVisibility(View.GONE);
                    }
                }

                if (buttonPositive != null) {
                    buttonPositive.setText(args.getString(ARG_POSITIVE_BTN_TEXT));
                    buttonPositive.setOnClickListener(v -> {
                        if (listener != null) {
                            listener.onDialogPositiveClick(this);
                        }
                        dismiss();
                    });
                }

                String negativeBtnText = args.getString(ARG_NEGATIVE_BTN_TEXT);
                if (buttonNegative != null && negativeBtnText != null && !negativeBtnText.isEmpty()) {
                    buttonNegative.setText(negativeBtnText);
                    buttonNegative.setVisibility(View.VISIBLE);
                    buttonNegative.setOnClickListener(v -> {
                        if (listener != null) {
                            listener.onDialogNegativeClick(this);
                        }
                        dismiss();
                    });
                } else if (buttonNegative != null) {
                    buttonNegative.setVisibility(View.GONE);
                }
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        Bundle args = getArguments();
        int customLayoutResId = 0;
        if (args != null) {
            customLayoutResId = args.getInt(ARG_CUSTOM_LAYOUT_RES_ID, 0);
        }

        if (getDialog() != null && getDialog().getWindow() != null) {
            if (customLayoutResId == R.layout.dialog_image_viewer) {
                // For image viewer, set to WRAP_CONTENT for height and MATCH_PARENT for width
                getDialog().getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            } else {
                // For other dialogs, set width to MATCH_PARENT and height to WRAP_CONTENT
                getDialog().getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            }
        }
    }
}
