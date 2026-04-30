package uri.app.kickflip;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AddTrickFragment extends Fragment {

    private static final int VIDEO_PICK_REQUEST = 1001;

    private static final String ARG_TERRAIN            = "arg_terrain";
    private static final String ARG_TRICK              = "arg_trick";
    private static final String ARG_SPIN               = "arg_spin";
    private static final String ARG_DIRECTION          = "arg_direction";
    private static final String ARG_STANCE             = "arg_stance";
    private static final String ARG_PRESET_MEASUREMENT = "arg_preset_meas";

    // ── Factory ───────────────────────────────────────────────────────────────

    public static AddTrickFragment newInstance(String terrain, String trick,
                                               int spinDegrees, String direction,
                                               String stance, int presetMeasurement) {
        AddTrickFragment f = new AddTrickFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TERRAIN,            terrain);
        args.putString(ARG_TRICK,              trick);
        args.putInt(ARG_SPIN,                  spinDegrees);
        args.putString(ARG_DIRECTION,          direction);
        args.putString(ARG_STANCE,             stance != null ? stance : "Regular");
        args.putInt(ARG_PRESET_MEASUREMENT,    presetMeasurement);
        f.setArguments(args);
        return f;
    }

    /** Called from TrickPickerFragment for tricks that skip the spin screen (e.g. Manuals). */
    public static AddTrickFragment newInstance(String terrain, String trick,
                                               int spinDegrees, String direction,
                                               int presetMeasurement) {
        return newInstance(terrain, trick, spinDegrees, direction, "Regular", presetMeasurement);
    }

    /** Legacy entry point – keeps old callers compiling. */
    public static AddTrickFragment newInstance(String terrain, String trick,
                                               int ignoredLegacyDiff) {
        return newInstance(terrain, trick, 0, "", "Regular", 0);
    }

    // ── State ─────────────────────────────────────────────────────────────────
    private String terrain, trick, direction, stance;
    private int    spinDegrees, presetMeasurement;
    private int    selectedMeasurement = 0;
    private String videoPath           = "";
    private DifficultyEngine.MeasurementType measType;

    // Stepper
    private List<FrameLayout> steps;
    private int currentStep = 0;

    // ── Views ─────────────────────────────────────────────────────────────────
    private LinearLayout  llStepDots;
    private FrameLayout   stepMeasurement, stepName, stepVideo;
    private TextView      tvMeasSubtitle, tvMeasLabel, tvVideoPath;
    private EditText      etMeasurement, etTrickName;
    private Button        btnPickMeasurement, btnSelectVideo, btnBack, btnNext;

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args != null) {
            terrain           = args.getString(ARG_TERRAIN,   "");
            trick             = args.getString(ARG_TRICK,     "");
            spinDegrees       = args.getInt(ARG_SPIN,         0);
            direction         = args.getString(ARG_DIRECTION, "");
            stance            = args.getString(ARG_STANCE,    "Regular");
            presetMeasurement = args.getInt(ARG_PRESET_MEASUREMENT, 0);
        }
        measType = DifficultyEngine.getMeasurementType(terrain);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add_trick, container, false);

        llStepDots       = view.findViewById(R.id.ll_step_dots);
        stepMeasurement  = view.findViewById(R.id.step_measurement);
        stepName         = view.findViewById(R.id.step_name);
        stepVideo        = view.findViewById(R.id.step_video);
        tvMeasSubtitle   = view.findViewById(R.id.tv_meas_subtitle);
        tvMeasLabel      = view.findViewById(R.id.tv_measurement_label);
        etMeasurement    = view.findViewById(R.id.et_measurement);
        btnPickMeasurement = view.findViewById(R.id.btn_pick_measurement);
        etTrickName      = view.findViewById(R.id.et_trick_name);
        btnSelectVideo   = view.findViewById(R.id.btn_select_video);
        tvVideoPath      = view.findViewById(R.id.tv_video_path);
        btnBack          = view.findViewById(R.id.btn_back);
        btnNext          = view.findViewById(R.id.btn_next);

        buildStepList();
        buildStepDots();
        configureMeasurementStep();
        prefillName();

        btnSelectVideo.setOnClickListener(v ->
                startActivityForResult(
                        new Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI),
                        VIDEO_PICK_REQUEST));

        btnBack.setOnClickListener(v -> goBack());
        btnNext.setOnClickListener(v -> goNext());

        showStep(0);
        return view;
    }

    // ── Stepper ───────────────────────────────────────────────────────────────

    private void buildStepList() {
        steps = new ArrayList<>();
        boolean needsMeasurement = measType != DifficultyEngine.MeasurementType.NONE
                && presetMeasurement == 0;
        if (needsMeasurement) steps.add(stepMeasurement);
        steps.add(stepName);
        steps.add(stepVideo);
    }

    private void buildStepDots() {
        llStepDots.removeAllViews();
        int dotSize  = dpToPx(8);
        int dotSpace = dpToPx(6);
        for (int i = 0; i < steps.size(); i++) {
            View dot = new View(requireContext());
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dotSize, dotSize);
            lp.setMargins(dotSpace, 0, dotSpace, 0);
            dot.setLayoutParams(lp);
            // Will be colored in updateDots()
            llStepDots.addView(dot);
        }
    }

    private void updateDots() {
        for (int i = 0; i < llStepDots.getChildCount(); i++) {
            llStepDots.getChildAt(i)
                    .setBackgroundResource(i == currentStep
                            ? R.drawable.dot_active
                            : R.drawable.dot_inactive);
        }
    }

    private void showStep(int index) {
        currentStep = index;
        for (FrameLayout s : steps) s.setVisibility(View.GONE);
        steps.get(index).setVisibility(View.VISIBLE);
        updateDots();
        updateNavButtons();
    }

    private void animateToStep(int nextIndex, boolean forward) {
        FrameLayout current = steps.get(currentStep);
        FrameLayout next    = steps.get(nextIndex);

        int width = current.getWidth();
        if (width == 0) { showStep(nextIndex); return; }

        float fromX = forward ?  width : -width;
        float toX   = forward ? -width :  width;

        next.setTranslationX(fromX);
        next.setVisibility(View.VISIBLE);

        ObjectAnimator outAnim = ObjectAnimator.ofFloat(current, "translationX", 0, toX);
        ObjectAnimator inAnim  = ObjectAnimator.ofFloat(next,    "translationX", fromX, 0);
        outAnim.setDuration(220);
        inAnim.setDuration(220);
        outAnim.start();
        inAnim.start();

        outAnim.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(android.animation.Animator a) {
                current.setVisibility(View.GONE);
                current.setTranslationX(0);
                currentStep = nextIndex;
                updateDots();
                updateNavButtons();
            }
        });
    }

    private void updateNavButtons() {
        boolean isLast = (currentStep == steps.size() - 1);
        btnNext.setText(isLast ? "Save" : "Continue");
    }

    private void goNext() {
        if (!validateCurrentStep()) return;
        if (currentStep == steps.size() - 1) {
            saveTrick();
        } else {
            animateToStep(currentStep + 1, true);
        }
    }

    private void goBack() {
        if (currentStep > 0) {
            animateToStep(currentStep - 1, false);
        } else {
            requireActivity().getSupportFragmentManager().popBackStack();
        }
    }

    // ── Measurement step setup ────────────────────────────────────────────────

    private void configureMeasurementStep() {
        if (!steps.contains(stepMeasurement)) return;

        tvMeasSubtitle.setText(terrain);

        switch (measType) {
            case STAIRS:
                tvMeasLabel.setText("Number of stairs:");
                etMeasurement.setHint("e.g. 5");
                etMeasurement.setVisibility(View.VISIBLE);
                break;

            case UP_STAIRS:
                tvMeasLabel.setText("Number of stairs (up):");
                etMeasurement.setHint("e.g. 3");
                etMeasurement.setVisibility(View.VISIBLE);
                break;

            case GAP:
                tvMeasLabel.setText("Gap size (stair equivalents):");
                etMeasurement.setHint("e.g. 4");
                etMeasurement.setVisibility(View.VISIBLE);
                break;

            case HEIGHT:
                tvMeasLabel.setText("Height:");
                btnPickMeasurement.setText("Select height…");
                btnPickMeasurement.setVisibility(View.VISIBLE);
                btnPickMeasurement.setOnClickListener(v ->
                        new AlertDialog.Builder(requireContext())
                                .setTitle("Height")
                                .setItems(DifficultyEngine.HEIGHT_OPTIONS, (d, i) -> {
                                    selectedMeasurement = i + 1;
                                    btnPickMeasurement.setText(DifficultyEngine.HEIGHT_OPTIONS[i]);
                                })
                                .show());
                break;

            case SIZE:
                tvMeasLabel.setText("Ramp size:");
                btnPickMeasurement.setText("Select size…");
                btnPickMeasurement.setVisibility(View.VISIBLE);
                btnPickMeasurement.setOnClickListener(v ->
                        new AlertDialog.Builder(requireContext())
                                .setTitle("Size")
                                .setItems(DifficultyEngine.SIZE_OPTIONS, (d, i) -> {
                                    selectedMeasurement = i + 1;
                                    btnPickMeasurement.setText(DifficultyEngine.SIZE_OPTIONS[i]);
                                })
                                .show());
                break;

            default:
                break;
        }
    }

    // ── Pre-fill name ─────────────────────────────────────────────────────────

    private void prefillName() {
        etTrickName.setText(buildDefaultName());
        etTrickName.setSelection(etTrickName.getText().length());
    }

    private String buildDefaultName() {
        StringBuilder sb = new StringBuilder();
        if (stance != null && !"Regular".equals(stance)) sb.append(stance).append(" ");
        if (!direction.isEmpty())                        sb.append(direction).append(" ");
        if (spinDegrees > 0)                             sb.append(spinDegrees).append(" ");
        sb.append(trick);
        return sb.toString();
    }

    // ── Validation ────────────────────────────────────────────────────────────

    private boolean validateCurrentStep() {
        FrameLayout current = steps.get(currentStep);
        if (current == stepMeasurement) {
            return validateMeasurement();
        }
        if (current == stepName) {
            if (etTrickName.getText().toString().trim().isEmpty()) {
                Toast.makeText(requireContext(), "Enter a name for this trick", Toast.LENGTH_SHORT).show();
                return false;
            }
        }
        return true;
    }

    private boolean validateMeasurement() {
        switch (measType) {
            case STAIRS:
            case UP_STAIRS:
            case GAP: {
                String raw = etMeasurement.getText().toString().trim();
                if (raw.isEmpty()) {
                    Toast.makeText(requireContext(), "Enter a count", Toast.LENGTH_SHORT).show();
                    return false;
                }
                try {
                    int val = Integer.parseInt(raw);
                    if (val < 1) throw new NumberFormatException();
                    selectedMeasurement = val;
                } catch (NumberFormatException e) {
                    Toast.makeText(requireContext(), "Enter a valid number", Toast.LENGTH_SHORT).show();
                    return false;
                }
                break;
            }
            case HEIGHT:
            case SIZE:
                if (selectedMeasurement == 0) {
                    String what = (measType == DifficultyEngine.MeasurementType.HEIGHT)
                            ? "Select a height" : "Select a size";
                    Toast.makeText(requireContext(), what, Toast.LENGTH_SHORT).show();
                    return false;
                }
                break;
            default:
                break;
        }
        return true;
    }

    // ── Video picker ──────────────────────────────────────────────────────────

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == VIDEO_PICK_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                videoPath = uri.toString();
                tvVideoPath.setText("Video: " + uri.getLastPathSegment());
            }
        }
    }

    // ── Save ──────────────────────────────────────────────────────────────────

    private void saveTrick() {
        String trickName = etTrickName.getText().toString().trim();
        if (trickName.isEmpty()) {
            Toast.makeText(requireContext(), "Enter a name for this trick", Toast.LENGTH_SHORT).show();
            return;
        }

        int measurement = (presetMeasurement > 0) ? presetMeasurement : selectedMeasurement;
        int difficulty  = DifficultyEngine.calculate(trick, terrain, measurement, spinDegrees);
        String date     = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                            .format(new Date());

        saveTrickToDatabase(trickName, trick, terrain, measurement, difficulty,
                spinDegrees, direction, stance, videoPath, date);
    }

    private void saveTrickToDatabase(String name, String trickType, String terrainName,
                                     int measurement, int difficulty,
                                     int spin, String dir, String stanceVal,
                                     String video, String date) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(requireContext(), "Not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("name",        name);
        data.put("trick",       trickType);
        data.put("terrain",     terrainName);
        data.put("measurement", measurement);
        data.put("difficulty",  difficulty);
        data.put("spinDegrees", spin);
        data.put("direction",   dir);
        data.put("stance",      stanceVal != null ? stanceVal : "Regular");
        data.put("videoPath",   video);
        data.put("date",        date);

        btnNext.setEnabled(false);

        FirebaseFirestore.getInstance()
                .collection("users").document(user.getUid())
                .collection("tricks")
                .add(data)
                .addOnSuccessListener(ref ->
                        ((HomeActivity) requireActivity()).switchToVault()
                )
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "Failed: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    btnNext.setEnabled(true);
                });
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private int dpToPx(int dp) {
        return Math.round(dp * requireContext().getResources().getDisplayMetrics().density);
    }
}
