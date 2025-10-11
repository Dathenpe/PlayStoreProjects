// path/to/your/ui/dialogs/CustomAlertDialogFragment.java
package com.f9ld3.Zion.ui.dialogs;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import com.f9ld3.Zion.databinding.DialogCustomAlertBinding;

public class CustomAlertDialogFragment extends DialogFragment {

    private DialogCustomAlertBinding binding;
    private String title, message, positiveButtonText, negativeButtonText;
    private DialogListener listener;

    public interface DialogListener {
        void onPositiveClick();
        void onNegativeClick();
    }

    public static CustomAlertDialogFragment newInstance(String title, String message, String positiveButtonText, @Nullable String negativeButtonText) {
        CustomAlertDialogFragment fragment = new CustomAlertDialogFragment();
        Bundle args = new Bundle();
        args.putString("title", title);
        args.putString("message", message);
        args.putString("positiveBtnText", positiveButtonText);
        if (negativeButtonText != null) {
            args.putString("negativeBtnText", negativeButtonText);
        }
        fragment.setArguments(args);
        return fragment;
    }

    public void setDialogListener(DialogListener listener) {
        this.listener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            title = getArguments().getString("title");
            message = getArguments().getString("message");
            positiveButtonText = getArguments().getString("positiveBtnText");
            negativeButtonText = getArguments().getString("negativeBtnText");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DialogCustomAlertBinding.inflate(inflater, container, false);
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        }
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.dialogTitle.setText(title);
        binding.dialogMessage.setText(message);
        binding.buttonPositive.setText(positiveButtonText);

        binding.buttonPositive.setOnClickListener(v -> {
            if (listener != null) listener.onPositiveClick();
            dismiss();
        });

        if (negativeButtonText != null) {
            binding.buttonNegative.setVisibility(View.VISIBLE);
            binding.buttonNegative.setText(negativeButtonText);
            binding.buttonNegative.setOnClickListener(v -> {
                if (listener != null) listener.onNegativeClick();
                dismiss();
            });
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}