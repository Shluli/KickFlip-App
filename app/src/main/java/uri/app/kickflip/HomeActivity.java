package uri.app.kickflip;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class HomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // Find your BottomNavigationView
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);

        // Load weather fragment as default on first launch
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.homeFragmentContainer, new WeatherFragment())
                    .commit();
        }

        // Set up navigation listener
        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;

            int itemId = item.getItemId();

            if (itemId == R.id.nav_weather) {
                selectedFragment = new WeatherFragment();
            } else if (itemId == R.id.nav_vault) {
                selectedFragment = new VaultFragment(); // Create this later
            } else if (itemId == R.id.nav_progression) {
                // selectedFragment = new ProgressionFragment(); // Create this later
            } else if (itemId == R.id.nav_profile) {
                // selectedFragment = new ProfileFragment(); // Create this later
            }

            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.homeFragmentContainer, selectedFragment)
                        .commit();
            }

            return true;
        });
    }
}