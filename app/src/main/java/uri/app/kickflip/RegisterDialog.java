package uri.app.kickflip;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.firebase.auth.FirebaseAuth;

public class RegisterDialog extends DialogFragment {

    EditText emailEt, passwordEt, repeatPasswordEt;
    Button registerBtn;
    TextView errorTv;
    ImageButton closeBtn;


    FirebaseAuth auth;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = new Dialog(requireContext());
        dialog.setContentView(R.layout.fragment_register);

        auth = FirebaseAuth.getInstance();

        emailEt = dialog.findViewById(R.id.EditTextEmailRegister);
        passwordEt = dialog.findViewById(R.id.editTextPassword);
        repeatPasswordEt = dialog.findViewById(R.id.EditTextRepeatPassword);
        registerBtn = dialog.findViewById(R.id.ButtonRegister);
        errorTv = dialog.findViewById(R.id.ViewRegisterError);
        closeBtn = dialog.findViewById(R.id.CloseRegisterBtn);




        registerBtn.setOnClickListener(v -> {
            String email = emailEt.getText().toString().trim();
            String password = passwordEt.getText().toString().trim();
            String repeat = repeatPasswordEt.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty() || repeat.isEmpty()) {
                errorTv.setText("Please fill all fields");
                return;
            }

            if (!password.equals(repeat)) {
                errorTv.setText("Passwords do not match");
                return;
            }

            if (password.length() < 8) {
                errorTv.setText("Password must be at least 8 characters");
                return;
            }

            registerBtn.setEnabled(false);
            errorTv.setText("Creating account...");

            auth.createUserWithEmailAndPassword(email, password)
                    .addOnSuccessListener(result -> {
                        errorTv.setText("Registration successful");
                        dismiss();
                    })
                    .addOnFailureListener(e -> {
                        errorTv.setText(e.getMessage());
                        registerBtn.setEnabled(true);
                    });
        });

        closeBtn.setOnClickListener(v -> dismiss());

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }

        return dialog;
    }
}
