package uri.app.kickflip;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.NumberPicker;
import android.widget.TextView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import java.util.HashMap;
import java.util.Map;

public class ProfileFragment extends Fragment {

    private static final String PREFS = "kickflip_profile";

    private ImageView  ivAvatar;
    private TextView   tvDisplayName, tvEmail, tvAgeValue, tvSaveStatus;
    private TextView   btnRegular, btnGoofy;

    private SharedPreferences prefs;
    private FirebaseFirestore db;
    private String uid;

    private final Handler saveHandler = new Handler(Looper.getMainLooper());

    private final ActivityResultLauncher<String> pickImage =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri == null) return;
                try {
                    requireActivity().getContentResolver()
                            .takePersistableUriPermission(uri,
                                    Intent.FLAG_GRANT_READ_URI_PERMISSION);
                } catch (Exception ignored) {}
                ivAvatar.setImageURI(uri);
                String uriStr = uri.toString();
                prefs.edit().putString("avatar_uri", uriStr).apply();
                saveField("avatarUri", uriStr);
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_profile, container, false);

        prefs = requireActivity().getSharedPreferences(PREFS,
                android.content.Context.MODE_PRIVATE);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            uid = user.getUid();
            db  = FirebaseFirestore.getInstance();
        }

        ivAvatar      = v.findViewById(R.id.iv_avatar);
        tvDisplayName = v.findViewById(R.id.tv_display_name);
        tvEmail       = v.findViewById(R.id.tv_email);
        tvAgeValue    = v.findViewById(R.id.tv_age_value);
        tvSaveStatus  = v.findViewById(R.id.tv_save_status);
        btnRegular    = v.findViewById(R.id.btn_regular);
        btnGoofy      = v.findViewById(R.id.btn_goofy);

        // Load from Firestore first, fall back to local prefs
        loadFromFirestore();
        populateFromPrefs();

        // Avatar pickers
        View.OnClickListener pickAvatar = x -> pickImage.launch("image/*");
        ivAvatar.setOnClickListener(pickAvatar);
        v.findViewById(R.id.tv_camera_badge).setOnClickListener(pickAvatar);

        // Editable name
        v.findViewById(R.id.row_name).setOnClickListener(x -> showEditNameDialog());

        // Stance
        setupStance();

        // Age
        tvAgeValue.setOnClickListener(x -> showAgePicker());

        // Logout
        v.findViewById(R.id.btn_logout).setOnClickListener(x -> confirmLogout());

        return v;
    }

    // ── Local prefs fallback (instant display on open) ───────────────────────

    private void populateFromPrefs() {
        String name = prefs.getString("display_name", null);
        if (name != null && !name.isEmpty()) tvDisplayName.setText(name);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && user.getEmail() != null) tvEmail.setText(user.getEmail());

        String avatarUri = prefs.getString("avatar_uri", null);
        if (avatarUri != null) {
            try { ivAvatar.setImageURI(Uri.parse(avatarUri)); }
            catch (Exception ignored) {}
        }

        int age = prefs.getInt("age", 0);
        tvAgeValue.setText(age > 0 ? age + " yrs  ›" : "Set age  ›");

        boolean goofy = prefs.getBoolean("stance_goofy", false);
        applyStance(goofy);
    }

    // ── Firestore: load ──────────────────────────────────────────────────────

    private void loadFromFirestore() {
        if (db == null || uid == null) return;
        db.collection("users").document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!isAdded()) return;
                    if (doc.exists()) {
                        String name = doc.getString("name");
                        if (name != null && !name.isEmpty()) {
                            tvDisplayName.setText(name);
                            prefs.edit().putString("display_name", name).apply();
                        }
                        Long ageLong = doc.getLong("age");
                        if (ageLong != null) {
                            int age = ageLong.intValue();
                            tvAgeValue.setText(age + " yrs  ›");
                            prefs.edit().putInt("age", age).apply();
                        }
                        String stance = doc.getString("stance");
                        if (stance != null) {
                            boolean goofy = stance.equals("Goofy");
                            prefs.edit().putBoolean("stance_goofy", goofy).apply();
                            applyStance(goofy);
                        }
                        String avatarUri = doc.getString("avatarUri");
                        if (avatarUri != null) {
                            prefs.edit().putString("avatar_uri", avatarUri).apply();
                            try { ivAvatar.setImageURI(Uri.parse(avatarUri)); }
                            catch (Exception ignored) {}
                        }
                    }
                });
    }

    // ── Firestore: save ──────────────────────────────────────────────────────

    private void saveField(String key, Object value) {
        if (db == null || uid == null) return;
        Map<String, Object> data = new HashMap<>();
        data.put(key, value);
        db.collection("users").document(uid)
                .set(data, SetOptions.merge())
                .addOnSuccessListener(unused -> showSaved())
                .addOnFailureListener(e -> showSaveError());
    }

    private void showSaved() {
        if (!isAdded()) return;
        tvSaveStatus.setText("Saved ✓");
        tvSaveStatus.setTextColor(Color.parseColor("#10B981"));
        tvSaveStatus.setVisibility(View.VISIBLE);
        saveHandler.removeCallbacksAndMessages(null);
        saveHandler.postDelayed(() -> {
            if (isAdded()) tvSaveStatus.setVisibility(View.INVISIBLE);
        }, 2000);
    }

    private void showSaveError() {
        if (!isAdded()) return;
        tvSaveStatus.setText("Couldn't save");
        tvSaveStatus.setTextColor(Color.parseColor("#EF4444"));
        tvSaveStatus.setVisibility(View.VISIBLE);
        saveHandler.removeCallbacksAndMessages(null);
        saveHandler.postDelayed(() -> {
            if (isAdded()) tvSaveStatus.setVisibility(View.INVISIBLE);
        }, 3000);
    }

    // ── Name editing ─────────────────────────────────────────────────────────

    private void showEditNameDialog() {
        EditText et = new EditText(requireContext());
        et.setText(tvDisplayName.getText());
        et.setSelectAllOnFocus(true);
        et.setSingleLine(true);

        int pad = dp(20);
        FrameLayout wrap = new FrameLayout(requireContext());
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(pad, dp(8), pad, 0);
        et.setLayoutParams(lp);
        wrap.addView(et);

        new AlertDialog.Builder(requireContext())
                .setTitle("Your name")
                .setView(wrap)
                .setPositiveButton("Save", (d, w) -> {
                    String name = et.getText().toString().trim();
                    if (name.isEmpty()) return;
                    tvDisplayName.setText(name);
                    prefs.edit().putString("display_name", name).apply();
                    saveField("name", name);
                    // Also update Firebase Auth display name
                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                    if (user != null) {
                        user.updateProfile(new UserProfileChangeRequest.Builder()
                                .setDisplayName(name).build());
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();

        et.post(() -> {
            et.requestFocus();
            android.view.inputmethod.InputMethodManager imm =
                    (android.view.inputmethod.InputMethodManager)
                            requireActivity().getSystemService(
                                    android.content.Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.showSoftInput(et,
                    android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
        });
    }

    // ── Stance ───────────────────────────────────────────────────────────────

    private void setupStance() {
        btnRegular.setOnClickListener(v -> {
            prefs.edit().putBoolean("stance_goofy", false).apply();
            applyStance(false);
            saveField("stance", "Regular");
        });
        btnGoofy.setOnClickListener(v -> {
            prefs.edit().putBoolean("stance_goofy", true).apply();
            applyStance(true);
            saveField("stance", "Goofy");
        });
    }

    private void applyStance(boolean goofy) {
        styleStanceBtn(btnRegular, !goofy);
        styleStanceBtn(btnGoofy,   goofy);
    }

    private void styleStanceBtn(TextView btn, boolean active) {
        float r = 100f * getResources().getDisplayMetrics().density;
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(r);
        bg.setColor(active ? 0xFF111111 : android.graphics.Color.TRANSPARENT);
        btn.setBackground(bg);
        btn.setTextColor(active ? Color.WHITE : Color.parseColor("#888888"));
    }

    // ── Age ──────────────────────────────────────────────────────────────────

    private void showAgePicker() {
        NumberPicker picker = new NumberPicker(requireContext());
        picker.setMinValue(8);
        picker.setMaxValue(65);
        int saved = prefs.getInt("age", 16);
        picker.setValue(Math.max(8, saved));
        picker.setWrapSelectorWheel(false);
        picker.setPadding(dp(24), dp(12), dp(24), dp(12));

        new AlertDialog.Builder(requireContext())
                .setTitle("Your age")
                .setView(picker)
                .setPositiveButton("Done", (d, w) -> {
                    int age = picker.getValue();
                    prefs.edit().putInt("age", age).apply();
                    tvAgeValue.setText(age + " yrs  ›");
                    saveField("age", (long) age);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ── Logout ───────────────────────────────────────────────────────────────

    private void confirmLogout() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Log out")
                .setMessage("Are you sure?")
                .setPositiveButton("Log out", (d, w) -> {
                    FirebaseAuth.getInstance().signOut();
                    Intent intent = new Intent(requireActivity(), MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                            | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ── Util ─────────────────────────────────────────────────────────────────

    private int dp(int d) {
        return Math.round(d * getResources().getDisplayMetrics().density);
    }
}
