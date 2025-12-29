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

public class LoginDialog extends DialogFragment {

    EditText emailEt, passwordEt;
    Button loginBtn, forgotBtn;
    TextView errorTv;
    ImageButton closeBtn;

    FirebaseAuth auth;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = new Dialog(requireContext());
        dialog.setContentView(R.layout.fragment_login); // your layout

        auth = FirebaseAuth.getInstance();

        emailEt = dialog.findViewById(R.id.EditTextEmailLogin);
        passwordEt = dialog.findViewById(R.id.EditTextPasswordLogin);
        loginBtn = dialog.findViewById(R.id.ButtonLogin);
        forgotBtn = dialog.findViewById(R.id.ButtonForgotPassword);
        errorTv = dialog.findViewById(R.id.ViewLoginError);
        closeBtn = dialog.findViewById(R.id.CloseLoginBtn);

        loginBtn.setOnClickListener(v -> {
            String email = emailEt.getText().toString().trim();
            String password = passwordEt.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                errorTv.setText("Missing email or password");
                return;
            }

            loginBtn.setEnabled(false);
            errorTv.setText("Logging in...");

            auth.signInWithEmailAndPassword(email, password)
                    .addOnSuccessListener(result -> {
                        Intent i = new Intent(getContext(), HomeActivity.class); // change this
                        startActivity(i);
                        dismiss();
                    })
                    .addOnFailureListener(e -> {
                        errorTv.setText(e.getMessage());
                        loginBtn.setEnabled(true);
                    });
        });

        forgotBtn.setOnClickListener(v -> {
            String email = emailEt.getText().toString().trim();
            if (email.isEmpty()) {
                errorTv.setText("Enter email first");
                return;
            }

            auth.sendPasswordResetEmail(email)
                    .addOnSuccessListener(aVoid -> errorTv.setText("Reset email sent"))
                    .addOnFailureListener(e -> errorTv.setText(e.getMessage()));
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
