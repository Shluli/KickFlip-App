package uri.app.kickflip;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    Button RegisterFragBtn;
    TextView SignInBtn;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        RegisterFragBtn = findViewById(R.id.RegisterFragmentbtn);
        TextView signInBtn = findViewById(R.id.signInClickable);

        signInBtn.setOnClickListener(v -> {
            signInBtn.setEnabled(false);
            LoginDialog loginDialog = new LoginDialog();
            loginDialog.show(getSupportFragmentManager(), "loginDialog");
        });


        RegisterFragBtn.setOnClickListener(view -> {
            RegisterDialog dialog = new RegisterDialog();
            dialog.show(getSupportFragmentManager(), "register_popup");
        });
    }


}