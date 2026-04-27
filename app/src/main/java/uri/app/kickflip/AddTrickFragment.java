package uri.app.kickflip;

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
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class AddTrickFragment extends Fragment {

    private static final int VIDEO_PICK_REQUEST = 1001;

    private static final String ARG_TERRAIN    = "arg_terrain";
    private static final String ARG_TRICK      = "arg_trick";
    private static final String ARG_DIFFICULTY = "arg_difficulty";

    // ---- Factory ----

    public static AddTrickFragment newInstance(String terrain, String trick, int difficulty) {
        AddTrickFragment f = new AddTrickFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TERRAIN, terrain);
        args.putString(ARG_TRICK, trick);
        args.putInt(ARG_DIFFICULTY, difficulty);
        f.setArguments(args);
        return f;
    }

    // ---- Views ----
    private EditText etTrickName;
    private Button btnPickTrick;
    private Button btnPickTerrain;
    private EditText etMeasurement;
    private TextView tvMeasurementLabel;
    private Button btnSelectVideo;
    private Button btnSaveTrick;
    private TextView tvSelectedTrick;
    private TextView tvSelectedTerrain;
    private TextView tvVideoPath;

    // ---- State ----
    private String selectedTrick = "";
    private String selectedTerrain = "";
    private String videoPath = "";
    private int selectedTrickIndex = -1;
    private int selectedTerrainIndex = -1;
    private int preFillDifficulty = -1;
    private boolean isPreFilled = false;

    // Trick options
    private final String[] tricks = {
        "Kickflip", "Heelflip", "Varial Flip", "Hardflip",
        "Treflip", "Pop Shuvit", "Frontside Flip", "Backside Flip",
        "Ollie", "Nollie", "Fakie Flip", "Switch Flip",
        "50-50", "5-0", "Nosegrind", "Boardslide",
        "Lipslide", "Crooked Grind", "Smith Grind", "Feeble Grind",
        "Bluntslide", "Noseblunt", "Tailslide", "Noseslide"
    };

    // Terrain options with their measurement types
    private final String[] terrains = {
        "Flatground", "Ledge", "Rail", "Hubba",
        "Down Stairs", "Up Stairs", "Gap", "Euro Gap",
        "Quarter Pipe", "Half Pipe", "Bank", "Ramp",
        "Curb", "Grass", "Curved Rail", "On a manual pad",
        "Up a Manual Pad", "Bench", "Table", "Pyramid",
        "Frame", "Frame Gap", "Over an obsticle"
    };

    // Difficulty ratings for tricks (1-5)
    private final int[] trickDifficulty = {
        3, 3, 4, 4,  // Kickflip, Heelflip, Varial, Hardflip
        5, 2, 4, 4,  // Treflip, Pop Shuvit, FS Flip, BS Flip
        1, 2, 3, 4,  // Ollie, Nollie, Fakie Flip, Switch Flip
        2, 3, 3, 3,  // 50-50, 5-0, Nosegrind, Boardslide
        4, 4, 4, 4,  // Lipslide, Crooked, Smith, Feeble
        5, 5, 3, 3   // Bluntslide, Noseblunt, Tailslide, Noseslide
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args != null) {
            String argTerrain = args.getString(ARG_TERRAIN, "");
            String argTrick   = args.getString(ARG_TRICK, "");
            int argDiff       = args.getInt(ARG_DIFFICULTY, -1);
            if (!argTerrain.isEmpty() && !argTrick.isEmpty()) {
                selectedTerrain = argTerrain;
                selectedTrick   = argTrick;
                preFillDifficulty = argDiff;
                isPreFilled = true;
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add_trick, container, false);

        initializeViews(view);

        if (isPreFilled) {
            applyPreFill();
        } else {
            setupTrickButton();
            setupTerrainButton();
        }

        setupButtons();

        return view;
    }

    private void initializeViews(View view) {
        etTrickName      = view.findViewById(R.id.et_trick_name);
        btnPickTrick     = view.findViewById(R.id.btn_pick_trick);
        btnPickTerrain   = view.findViewById(R.id.btn_pick_terrain);
        etMeasurement    = view.findViewById(R.id.et_measurement);
        tvMeasurementLabel = view.findViewById(R.id.tv_measurement_label);
        btnSelectVideo   = view.findViewById(R.id.btn_select_video);
        btnSaveTrick     = view.findViewById(R.id.btn_save_trick);
        tvSelectedTrick  = view.findViewById(R.id.tv_selected_trick);
        tvSelectedTerrain = view.findViewById(R.id.tv_selected_terrain);
        tvVideoPath      = view.findViewById(R.id.tv_video_path);
    }

    private void applyPreFill() {
        // Pre-fill trick name from the trick string
        etTrickName.setText(selectedTrick);
        etTrickName.setSelection(selectedTrick.length());

        // Update display labels
        tvSelectedTrick.setText("Trick: " + selectedTrick);
        btnPickTrick.setText(selectedTrick);
        tvSelectedTerrain.setText("Terrain: " + selectedTerrain);
        btnPickTerrain.setText(selectedTerrain);

        // Apply measurement label for the 8 broad terrain names
        updateMeasurementLabel(selectedTerrain);

        // Hide picker buttons since terrain+trick are pre-selected
        btnPickTrick.setVisibility(View.GONE);
        btnPickTerrain.setVisibility(View.GONE);
    }

    private void setupTrickButton() {
        btnPickTrick.setOnClickListener(v -> new AlertDialog.Builder(requireContext())
                .setTitle("Select Trick")
                .setItems(tricks, (dialog, which) -> selectTrick(which))
                .show());
    }

    private void setupTerrainButton() {
        btnPickTerrain.setOnClickListener(v -> new AlertDialog.Builder(requireContext())
                .setTitle("Select Terrain")
                .setItems(terrains, (dialog, which) -> selectTerrain(which))
                .show());
    }

    private void selectTrick(int index) {
        selectedTrickIndex = index;
        selectedTrick = tricks[index];
        tvSelectedTrick.setText("Trick: " + selectedTrick);
        btnPickTrick.setText(selectedTrick);
    }

    private void selectTerrain(int index) {
        selectedTerrainIndex = index;
        selectedTerrain = terrains[index];
        tvSelectedTerrain.setText("Terrain: " + selectedTerrain);
        btnPickTerrain.setText(selectedTerrain);
        updateMeasurementLabel(selectedTerrain);
    }

    private void updateMeasurementLabel(String terrain) {
        String label;

        switch (terrain) {
            // Original narrow terrain names
            case "Flatground":
            // New broad terrain names that act like flatground
            case "Park":
            case "Vert":
            case "Bowl":
                etMeasurement.setVisibility(View.GONE);
                tvMeasurementLabel.setVisibility(View.GONE);
                return;

            case "Down Stairs":
            case "Stairs":
                label = "Number of stairs:";
                break;
            case "Up Stairs":
                label = "Number of stairs:";
                break;
            case "Gap":
                label = "Length (cm):";
                break;
            case "Ledge":
            case "Grass":
            case "Rail":
            case "Rails":
                label = "Height (cm)";
                break;
            case "Manual Pad":
            case "Hubba":
            case "Quarter Pipe":
            case "Euro Gap":
            case "Curved Rail":
            case "On a manual pad":
            case "Bench":
            case "Table":
            case "Over an obsticle":
            case "Pyramid":
            case "Frame":
            case "Frame Gap":
                label = "Distance (cm)";
                break;
            case "Half Pipe":
            case "Up a Manual Pad":
            case "Curb":
            case "Ramp":
            case "Bank":
                label = "Height (cm):";
                break;
            case "Street":
                label = "Height (cm)";
                break;
            default:
                label = "Measurement:";
        }

        tvMeasurementLabel.setText(label);
        tvMeasurementLabel.setVisibility(View.VISIBLE);
        etMeasurement.setVisibility(View.VISIBLE);
        etMeasurement.setText("");
    }

    private void setupButtons() {
        btnSelectVideo.setOnClickListener(v -> openVideoSelector());
        btnSaveTrick.setOnClickListener(v -> saveTrick());
    }

    private void openVideoSelector() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, VIDEO_PICK_REQUEST);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == VIDEO_PICK_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            Uri videoUri = data.getData();
            if (videoUri != null) {
                videoPath = videoUri.toString();
                tvVideoPath.setText("Video selected: " + videoUri.getLastPathSegment());
            }
        }
    }

    private void saveTrick() {
        String trickName = etTrickName.getText().toString().trim();

        if (trickName.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter a trick name", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedTrick.isEmpty()) {
            Toast.makeText(requireContext(), "Please select a trick", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedTerrain.isEmpty()) {
            Toast.makeText(requireContext(), "Please select terrain", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get measurement if applicable
        int measurement = 0;
        if (etMeasurement.getVisibility() == View.VISIBLE) {
            String measurementStr = etMeasurement.getText().toString().trim();
            if (measurementStr.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter measurement", Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                measurement = Integer.parseInt(measurementStr);
            } catch (NumberFormatException e) {
                Toast.makeText(requireContext(), "Please enter a valid number", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // Calculate difficulty
        int difficulty;
        if (isPreFilled && preFillDifficulty > 0) {
            difficulty = preFillDifficulty;
        } else if (selectedTrickIndex >= 0 && selectedTerrainIndex >= 0) {
            difficulty = calculateDifficulty(selectedTrickIndex, selectedTerrainIndex, measurement);
        } else {
            difficulty = 1;
        }

        String date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

        TrickEntry entry = new TrickEntry(
                trickName,
                selectedTrick,
                selectedTerrain,
                measurement,
                difficulty,
                videoPath,
                date
        );

        saveTrickToDatabase(entry);
    }

    private int calculateDifficulty(int trickIndex, int terrainIndex, int measurement) {
        int baseDifficulty = trickDifficulty[trickIndex];
        int terrainModifier = 0;
        String terrain = terrains[terrainIndex];

        switch (terrain) {
            case "Flatground":
                terrainModifier = 0;
                break;
            case "Ledge":
            case "Curb":
            case "Grass":
            case "Bench":
            case "Table":
            case "On a manual pad":
            case "Up a Manual Pad":
            case "Pyramid":
            case "Over an obsticle":
                terrainModifier = 1;
                break;
            case "Rail":
            case "Hubba":
            case "Curved Rail":
            case "Frame":
            case "Frame Gap":
                terrainModifier = 2;
                break;
            case "Down Stairs":
            case "Gap":
            case "Euro Gap":
                terrainModifier = measurement > 50 ? 2 : 1;
                break;
            case "Up Stairs":
                terrainModifier = measurement > 50 ? 3 : 2;
                break;
            case "Quarter Pipe":
            case "Bank":
            case "Ramp":
                terrainModifier = measurement > 100 ? 2 : 1;
                break;
            case "Half Pipe":
                terrainModifier = 3;
                break;
        }

        return Math.min(5, Math.max(1, baseDifficulty + terrainModifier));
    }

    private void saveTrickToDatabase(TrickEntry entry) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(requireContext(), "Not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("name", entry.name);
        data.put("trick", entry.trick);
        data.put("terrain", entry.terrain);
        data.put("measurement", entry.measurement);
        data.put("difficulty", entry.difficulty);
        data.put("videoPath", entry.videoPath);
        data.put("date", entry.date);

        btnSaveTrick.setEnabled(false);

        FirebaseFirestore.getInstance()
                .collection("users").document(user.getUid())
                .collection("tricks")
                .add(data)
                .addOnSuccessListener(ref -> {
                    getParentFragmentManager().setFragmentResult("tricks_updated", new Bundle());
                    // Confetti burst, then pop back
                    View root = requireView();
                    ViewGroup rootVg = (ViewGroup) root.getRootView()
                            .findViewById(android.R.id.content);
                    if (rootVg == null) rootVg = (ViewGroup) root.getParent();
                    final ViewGroup parent = rootVg;
                    ConfettiView.burst(parent,
                            () -> getParentFragmentManager().popBackStack());
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "Failed to save: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    btnSaveTrick.setEnabled(true);
                });
    }

    // Inner class for trick data
    public static class TrickEntry {
        public String name;
        public String trick;
        public String terrain;
        public int measurement;
        public int difficulty;
        public String videoPath;
        public String date;

        public TrickEntry(String name, String trick, String terrain, int measurement,
                         int difficulty, String videoPath, String date) {
            this.name = name;
            this.trick = trick;
            this.terrain = terrain;
            this.measurement = measurement;
            this.difficulty = difficulty;
            this.videoPath = videoPath;
            this.date = date;
        }
    }
}
