package uri.app.kickflip;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class SpinPickerFragment extends Fragment {

    private static final String ARG_TERRAIN            = "terrain";
    private static final String ARG_TRICK              = "trick";
    private static final String ARG_CATEGORY           = "category";
    private static final String ARG_PRESET_MEASUREMENT = "preset_meas";

    public static SpinPickerFragment newInstance(String terrain, String trick,
                                                  String category, int presetMeasurement) {
        SpinPickerFragment f = new SpinPickerFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TERRAIN,            terrain);
        args.putString(ARG_TRICK,              trick);
        args.putString(ARG_CATEGORY,           category);
        args.putInt(ARG_PRESET_MEASUREMENT,    presetMeasurement);
        f.setArguments(args);
        return f;
    }

    // ── State ─────────────────────────────────────────────────────────────────
    private String terrain, trick, category;
    private int presetMeasurement;
    private int    selectedSpin      = 0;
    private String selectedDirection = "";
    private String selectedStance    = "Regular";

    // ── Views ─────────────────────────────────────────────────────────────────
    private LinearLayout sectionSpin, sectionDirection;
    private GridLayout   gridSpin;
    private Button       btnBackside, btnFrontside;
    private Button       btnRegular, btnFakie, btnSwitch;
    private Button       btnContinue;
    private Button[]     spinButtons;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            terrain           = getArguments().getString(ARG_TERRAIN,  "");
            trick             = getArguments().getString(ARG_TRICK,    "");
            category          = getArguments().getString(ARG_CATEGORY, "");
            presetMeasurement = getArguments().getInt(ARG_PRESET_MEASUREMENT, 0);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_spin_picker, container, false);

        ((TextView) view.findViewById(R.id.tv_spin_subtitle))
                .setText(trick + " · " + terrainDisplayName());

        sectionSpin      = view.findViewById(R.id.section_spin);
        sectionDirection = view.findViewById(R.id.section_direction);
        gridSpin         = view.findViewById(R.id.grid_spin);
        btnBackside      = view.findViewById(R.id.btn_backside);
        btnFrontside     = view.findViewById(R.id.btn_frontside);
        btnRegular       = view.findViewById(R.id.btn_regular);
        btnFakie         = view.findViewById(R.id.btn_fakie);
        btnSwitch        = view.findViewById(R.id.btn_switch);
        btnContinue      = view.findViewById(R.id.btn_continue);

        view.findViewById(R.id.btn_back).setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());

        boolean spinEligible = DifficultyEngine.isSpinEligible(category);
        boolean dirAlways    = DifficultyEngine.isDirectionAlwaysEligible(category);
        boolean shuvEligible = DifficultyEngine.isShuvEligible(trick);

        // Spin grid: only for air/flip/ollie tricks
        if (spinEligible) {
            sectionSpin.setVisibility(View.VISIBLE);
            buildSpinGrid();
        }

        // Direction: always shown for grinds/slides/stalls + shuvit tricks;
        // for spin-eligible tricks it appears dynamically when spin > 0
        sectionDirection.setVisibility((dirAlways || shuvEligible) ? View.VISIBLE : View.GONE);

        setupDirectionButtons();
        setupStanceButtons();
        btnContinue.setOnClickListener(v -> navigateToAddTrick());

        return view;
    }

    // ── Spin grid ─────────────────────────────────────────────────────────────

    private void buildSpinGrid() {
        final int[]    spinValues = DifficultyEngine.SPIN_VALUES;
        final String[] spinLabels = DifficultyEngine.SPIN_LABELS;
        final int colCount = 4;

        spinButtons = new Button[spinValues.length];

        for (int i = 0; i < spinValues.length; i++) {
            final int spinVal = spinValues[i];
            final int idx     = i;

            Button btn = new Button(requireContext());
            btn.setStateListAnimator(null);
            btn.setTextSize(13f);
            btn.setText(spinLabels[i]);

            GridLayout.LayoutParams lp = new GridLayout.LayoutParams(
                    GridLayout.spec(i / colCount, GridLayout.FILL, 1f),
                    GridLayout.spec(i % colCount, GridLayout.FILL, 1f));
            lp.width  = 0;
            lp.height = GridLayout.LayoutParams.WRAP_CONTENT;
            lp.setMargins(5, 5, 5, 5);
            btn.setLayoutParams(lp);

            applySpinStyle(btn, spinVal == selectedSpin);
            btn.setOnClickListener(v -> onSpinSelected(spinVal));

            spinButtons[i] = btn;
            gridSpin.addView(btn);
        }
    }

    private void onSpinSelected(int spinVal) {
        selectedSpin = spinVal;
        for (int j = 0; j < spinButtons.length; j++) {
            applySpinStyle(spinButtons[j], DifficultyEngine.SPIN_VALUES[j] == selectedSpin);
        }
        // For spin-eligible tricks, reveal direction picker only when spin > 0
        if (DifficultyEngine.isSpinEligible(category)
                && !DifficultyEngine.isDirectionAlwaysEligible(category)
                && !DifficultyEngine.isShuvEligible(trick)) {
            sectionDirection.setVisibility(selectedSpin > 0 ? View.VISIBLE : View.GONE);
            if (selectedSpin == 0) {
                selectedDirection = "";
                applyDirectionStyle();
            }
        }
    }

    private void applySpinStyle(Button btn, boolean selected) {
        btn.setBackgroundTintList(ColorStateList.valueOf(selected ? 0xFF111111 : 0xFFF0F0F0));
        btn.setTextColor(selected ? 0xFFFFFFFF : 0xFF0A0A0A);
    }

    // ── Direction ─────────────────────────────────────────────────────────────

    private void setupDirectionButtons() {
        btnBackside.setOnClickListener(v  -> selectDirection("Backside"));
        btnFrontside.setOnClickListener(v -> selectDirection("Frontside"));
    }

    private void selectDirection(String dir) {
        selectedDirection = dir.equals(selectedDirection) ? "" : dir;
        applyDirectionStyle();
    }

    private void applyDirectionStyle() {
        boolean bs = "Backside".equals(selectedDirection);
        boolean fs = "Frontside".equals(selectedDirection);
        btnBackside.setBackgroundTintList(ColorStateList.valueOf(bs ? 0xFF111111 : 0xFFF0F0F0));
        btnBackside.setTextColor(bs ? 0xFFFFFFFF : 0xFF0A0A0A);
        btnFrontside.setBackgroundTintList(ColorStateList.valueOf(fs ? 0xFF111111 : 0xFFF0F0F0));
        btnFrontside.setTextColor(fs ? 0xFFFFFFFF : 0xFF0A0A0A);
    }

    // ── Stance ────────────────────────────────────────────────────────────────

    private void setupStanceButtons() {
        btnRegular.setOnClickListener(v -> selectStance("Regular"));
        btnFakie.setOnClickListener(v   -> selectStance("Fakie"));
        btnSwitch.setOnClickListener(v  -> selectStance("Switch"));
        applyStanceStyle(); // "Regular" is selected by default
    }

    private void selectStance(String stance) {
        selectedStance = stance;
        applyStanceStyle();
    }

    private void applyStanceStyle() {
        applyToggleStyle(btnRegular,  "Regular".equals(selectedStance));
        applyToggleStyle(btnFakie,    "Fakie".equals(selectedStance));
        applyToggleStyle(btnSwitch,   "Switch".equals(selectedStance));
    }

    private void applyToggleStyle(Button btn, boolean selected) {
        btn.setBackgroundTintList(ColorStateList.valueOf(selected ? 0xFF111111 : 0xFFF0F0F0));
        btn.setTextColor(selected ? 0xFFFFFFFF : 0xFF0A0A0A);
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    private void navigateToAddTrick() {
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(
                        R.anim.slide_in_right, R.anim.slide_out_left,
                        R.anim.slide_in_left,  R.anim.slide_out_right)
                .replace(R.id.homeFragmentContainer,
                        AddTrickFragment.newInstance(terrain, trick,
                                selectedSpin, selectedDirection,
                                selectedStance, presetMeasurement))
                .addToBackStack(null)
                .commit();
    }

    private String terrainDisplayName() {
        if ("Quarter Pipe".equals(terrain) && presetMeasurement > 0) {
            String[] labels = {"Small QP (2ft)", "Medium QP (4ft)", "Big QP (6ft)", "Vert (8ft+)"};
            int idx = presetMeasurement - 1;
            if (idx >= 0 && idx < labels.length) return labels[idx];
        }
        return terrain;
    }
}
