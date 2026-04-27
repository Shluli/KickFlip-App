package uri.app.kickflip;

import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

public class HomeActivity extends AppCompatActivity {

    private static final int TAB_WEATHER     = 0;
    private static final int TAB_VAULT       = 1;
    private static final int TAB_PROGRESSION = 2;

    private ImageView icWeather, icProgression;
    private TextView  btnProfileAvatar;
    private int currentTab = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        icWeather      = findViewById(R.id.ic_weather);
        icProgression  = findViewById(R.id.ic_progression);
        btnProfileAvatar = findViewById(R.id.btn_profile_avatar);

        findViewById(R.id.btn_nav_weather).setOnClickListener(v     -> selectTab(TAB_WEATHER));
        findViewById(R.id.btn_nav_vault).setOnClickListener(v       -> selectTab(TAB_VAULT));
        findViewById(R.id.btn_nav_progression).setOnClickListener(v -> selectTab(TAB_PROGRESSION));

        btnProfileAvatar.setOnClickListener(v -> {
            currentTab = -1; // allow returning to vault and re-selecting
            FragmentTransaction tx = getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(
                            R.anim.slide_in_right, R.anim.slide_out_left,
                            R.anim.slide_in_left,  R.anim.slide_out_right)
                    .replace(R.id.homeFragmentContainer, new ProfileFragment())
                    .addToBackStack(null);
            tx.commit();
        });

        if (savedInstanceState == null) {
            selectTab(TAB_VAULT);
        }

        refreshAvatar();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshAvatar();
    }

    private void selectTab(int tab) {
        if (tab == currentTab) return;
        currentTab = tab;

        Fragment fragment;
        switch (tab) {
            case TAB_WEATHER:     fragment = new WeatherFragment();     break;
            case TAB_PROGRESSION: fragment = new ProgressionFragment(); break;
            default:              fragment = new VaultFragment();        break;
        }

        getSupportFragmentManager().popBackStack(null,
                FragmentManager.POP_BACK_STACK_INCLUSIVE);

        getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.homeFragmentContainer, fragment)
                .commit();

        updateNavState(tab);
    }

    private void updateNavState(int activeTab) {
        setIconTint(icWeather,     activeTab == TAB_WEATHER);
        setIconTint(icProgression, activeTab == TAB_PROGRESSION);
    }

    private void setIconTint(ImageView iv, boolean active) {
        iv.setImageTintList(ColorStateList.valueOf(
                active ? 0xFF111111 : 0xFFAAAAAA));
    }

    private void refreshAvatar() {
        SharedPreferences prefs = getSharedPreferences("kickflip_profile", MODE_PRIVATE);
        String name = prefs.getString("display_name", null);
        if (name != null && !name.isEmpty()) {
            btnProfileAvatar.setText(String.valueOf(name.charAt(0)).toUpperCase());
        } else {
            btnProfileAvatar.setText("👤");
        }
    }
}
