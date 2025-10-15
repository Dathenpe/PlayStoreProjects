package com.f9ld3.Zion.ui.dialogs;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import com.f9ld3.Zion.databinding.DialogCustomInputBinding; // <-- Note the new binding class

public class CustomInputDialogFragment extends DialogFragment {

    private DialogCustomInputBinding binding;
    private String title, message, hint, positiveButtonText, negativeButtonText;
    private boolean isPasswordInput;
    private InputListener listener;

    public interface InputListener {
        void onInputConfirmed(String inputText);
    }

    public static CustomInputDialogFragment newInstance(String title, String message, String hint, String positiveButtonText, String negativeButtonText, boolean isPassword) {
        CustomInputDialogFragment fragment = new CustomInputDialogFragment();
        Bundle args = new Bundle();
        args.putString("title", title);
        args.putString("message", message);
        args.putString("hint", hint);
        args.putString("positiveBtnText", positiveButtonText);
        args.putString("negativeBtnText", negativeButtonText);
        args.putBoolean("isPassword", isPassword);
        fragment.setArguments(args);
        return fragment;
    }

    public void setInputListener(InputListener listener) {
        this.listener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            title = getArguments().getString("title");
            message = getArguments().getString("message");
            hint = getArguments().getString("hint");
            positiveButtonText = getArguments().getString("positiveBtnText");
            negativeButtonText = getArguments().getString("negativeBtnText");
            isPasswordInput = getArguments().getBoolean("isPassword");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DialogCustomInputBinding.inflate(inflater, container, false);
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        }
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.inputDialogTitle.setText(title);
        binding.inputDialogMessage.setText(message);
        binding.inputDialogLayout.setHint(hint);
        binding.buttonInputPositive.setText(positiveButtonText);
        binding.buttonInputNegative.setText(negativeButtonText);

        if (isPasswordInput) {
            binding.inputDialogInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        }

        binding.buttonInputPositive.setOnClickListener(v -> {
            if (listener != null) {
                String inputText = binding.inputDialogInput.getText().toString();
                listener.onInputConfirmed(inputText);
            }
            dismiss();
        });

        binding.buttonInputNegative.setOnClickListener(v -> dismiss());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}