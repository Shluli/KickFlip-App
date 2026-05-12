package uri.app.kickflip;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private Button registerBtn;
    private TextView signInBtn; // class-level reference

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        registerBtn = findViewById(R.id.RegisterFragmentbtn);
        signInBtn = findViewById(R.id.signInClickable);

        signInBtn.setOnClickListener(v -> {
            // Remove existing dialog if it exists
            LoginDialog oldDialog = (LoginDialog) getSupportFragmentManager().findFragmentByTag("LOGIN_DIALOG");
            if (oldDialog != null) {
                getSupportFragmentManager().beginTransaction().remove(oldDialog).commit();
            }

            // Show new dialog
            LoginDialog loginDialog = new LoginDialog();
            loginDialog.show(getSupportFragmentManager(), "LOGIN_DIALOG");
        });

        registerBtn.setOnClickListener(v -> {
            RegisterDialog oldDialog = (RegisterDialog) getSupportFragmentManager().findFragmentByTag("REGISTER_DIALOG");
            if (oldDialog != null) {
                getSupportFragmentManager().beginTransaction().remove(oldDialog).commit();
            }

            RegisterDialog registerDialog = new RegisterDialog();
            registerDialog.show(getSupportFragmentManager(), "REGISTER_DIALOG");
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        signInBtn.setEnabled(true);
    }
}
