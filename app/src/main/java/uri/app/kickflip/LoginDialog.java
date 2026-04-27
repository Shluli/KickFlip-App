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
import com.google.firebase.auth.FirebaseAuthException;

public class LoginDialog extends DialogFragment {

    private EditText emailEt, passwordEt;
    private Button loginBtn, forgotBtn;
    private TextView errorTv;
    private ImageButton closeBtn;

    private FirebaseAuth auth;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = new Dialog(requireContext());
        dialog.setContentView(R.layout.fragment_login);

        dialog.setCanceledOnTouchOutside(true);
        auth = FirebaseAuth.getInstance();

        emailEt = dialog.findViewById(R.id.EditTextEmailLogin);
        passwordEt = dialog.findViewById(R.id.EditTextPasswordLogin);
        loginBtn = dialog.findViewById(R.id.ButtonLogin);
        forgotBtn = dialog.findViewById(R.id.ButtonForgotPassword);
        errorTv = dialog.findViewById(R.id.ViewLoginError);
        closeBtn = dialog.findViewById(R.id.CloseLoginBtn);

        loginBtn.setOnClickListener(v -> attemptLogin());
        forgotBtn.setOnClickListener(v -> resetPassword());
        closeBtn.setOnClickListener(v -> dismiss());

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }

        setCancelable(true);
        return dialog;
    }

    private void attemptLogin() {
        String email = emailEt.getText().toString().trim();
        String password = passwordEt.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            errorTv.setText("Missing email or password");
            return;
        }

        // FIXED: Disable button only after validation passes
        loginBtn.setEnabled(false);
        errorTv.setText("Logging in...");

        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    startActivity(new Intent(requireActivity(), HomeActivity.class));
                    dismiss();
                })
                .addOnFailureListener(e -> {
                    String message;
                    if (e instanceof FirebaseAuthException) {
                        switch (((FirebaseAuthException) e).getErrorCode()) {
                            case "ERROR_NETWORK_REQUEST_FAILED":
                                message = "No internet connection — check your network and try again.";
                                break;
                            case "ERROR_INVALID_CREDENTIAL":
                            case "ERROR_USER_NOT_FOUND":
                            case "ERROR_WRONG_PASSWORD":
                                message = "Incorrect email or password.";
                                break;
                            case "ERROR_USER_DISABLED":
                                message = "This account has been disabled.";
                                break;
                            case "ERROR_TOO_MANY_REQUESTS":
                                message = "Too many attempts — try again later.";
                                break;
                            default:
                                message = "Login failed: " + e.getLocalizedMessage();
                        }
                    } else {
                        message = "Login failed: " + e.getLocalizedMessage();
                    }
                    errorTv.setText(message);
                    loginBtn.setEnabled(true);
                });
    }

    private void resetPassword() {
        String email = emailEt.getText().toString().trim();
        if (email.isEmpty()) {
            errorTv.setText("Enter email first");
            return;
        }

        auth.sendPasswordResetEmail(email)
                .addOnSuccessListener(aVoid -> errorTv.setText("Reset email sent"))
                .addOnFailureListener(e -> errorTv.setText(e.getLocalizedMessage()));
    }
}