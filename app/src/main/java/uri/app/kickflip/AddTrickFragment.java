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
    
    private String selectedTrick = "";
    private String selectedTerrain = "";
    private String videoPath = "";
    private int selectedTrickIndex = -1;
    private int selectedTerrainIndex = -1;
    
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
        "Stairs", "Gap", "Quarter Pipe", "Half Pipe",
        "Bank", "Manual Pad", "Curb", "Ramp"
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

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, 
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add_trick, container, false);
        
        initializeViews(view);
        setupTrickButton();
        setupTerrainButton();
        setupButtons();
        
        return view;
    }
    
    private void initializeViews(View view) {
        etTrickName = view.findViewById(R.id.et_trick_name);
        btnPickTrick = view.findViewById(R.id.btn_pick_trick);
        btnPickTerrain = view.findViewById(R.id.btn_pick_terrain);
        etMeasurement = view.findViewById(R.id.et_measurement);
        tvMeasurementLabel = view.findViewById(R.id.tv_measurement_label);
        btnSelectVideo = view.findViewById(R.id.btn_select_video);
        btnSaveTrick = view.findViewById(R.id.btn_save_trick);
        tvSelectedTrick = view.findViewById(R.id.tv_selected_trick);
        tvSelectedTerrain = view.findViewById(R.id.tv_selected_terrain);
        tvVideoPath = view.findViewById(R.id.tv_video_path);
    }
    
    private void setupTrickButton() {
        btnPickTrick.setOnClickListener(v -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Select Trick")
                    .setItems(tricks, (dialog, which) -> selectTrick(which))
                    .show();
        });
    }
    
    private void setupTerrainButton() {
        btnPickTerrain.setOnClickListener(v -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Select Terrain")
                    .setItems(terrains, (dialog, which) -> selectTerrain(which))
                    .show();
        });
    }
    
    private void selectTrick(int index) {
        selectedTrickIndex = index;
        selectedTrick = tricks[index];
        tvSelectedTrick.setText("Trick: " + selectedTrick);
        btnPickTrick.setText(selectedTrick);
        updatePreview();
    }
    
    private void selectTerrain(int index) {
        selectedTerrainIndex = index;
        selectedTerrain = terrains[index];
        tvSelectedTerrain.setText("Terrain: " + selectedTerrain);
        btnPickTerrain.setText(selectedTerrain);
        updateMeasurementLabel(selectedTerrain);
        updatePreview();
    }
    
    private void updateMeasurementLabel(String terrain) {
        String label;
        
        switch (terrain) {
            case "Flatground":
                etMeasurement.setVisibility(View.GONE);
                tvMeasurementLabel.setVisibility(View.GONE);
                return;
            case "Stairs":
                label = "Number of stairs:";
                break;
            case "Gap":
                label = "Length (cm):";
                break;
            case "Ledge":
            case "Rail":
            case "Hubba":
            case "Quarter Pipe":
            case "Half Pipe":
            case "Manual Pad":
            case "Curb":
            case "Ramp":
                label = "Height (cm):";
                break;
            case "Bank":
                label = "Height (cm):";
                break;
            default:
                label = "Measurement:";
        }
        
        tvMeasurementLabel.setText(label);
        tvMeasurementLabel.setVisibility(View.VISIBLE);
        etMeasurement.setVisibility(View.VISIBLE);
        etMeasurement.setText("");
    }
    
    private void updatePreview() {
        // This will show a preview of what's been selected
        if (!selectedTrick.isEmpty() && !selectedTerrain.isEmpty()) {
            btnSaveTrick.setEnabled(true);
        }
    }
    
    private void setupButtons() {
        btnSelectVideo.setOnClickListener(v -> openVideoSelector());
        btnSaveTrick.setOnClickListener(v -> saveTrick());
        btnSaveTrick.setEnabled(false);
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
        
        // Calculate difficulty (base trick difficulty + terrain modifier)
        int difficulty = calculateDifficulty(selectedTrickIndex, selectedTerrainIndex, measurement);
        
        // Get current date
        String date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
        
        // Create TrickEntry object and save to database
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
        
        // Add terrain difficulty modifier
        int terrainModifier = 0;
        String terrain = terrains[terrainIndex];
        
        switch (terrain) {
            case "Flatground":
                terrainModifier = 0;
                break;
            case "Ledge":
            case "Curb":
                terrainModifier = 1;
                break;
            case "Rail":
            case "Hubba":
                terrainModifier = 2;
                break;
            case "Stairs":
            case "Gap":
                terrainModifier = measurement > 50 ? 2 : 1;
                break;
            case "Quarter Pipe":
            case "Bank":
                terrainModifier = measurement > 100 ? 2 : 1;
                break;
            case "Half Pipe":
                terrainModifier = 3;
                break;
        }
        
        int totalDifficulty = baseDifficulty + terrainModifier;
        return Math.min(5, Math.max(1, totalDifficulty)); // Clamp between 1-5
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
                    getParentFragmentManager().popBackStack();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "Failed to save: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    btnSaveTrick.setEnabled(true);
                });
    }
    
    private void resetForm() {
        etTrickName.setText("");
        etMeasurement.setText("");
        etMeasurement.setVisibility(View.GONE);
        tvMeasurementLabel.setVisibility(View.GONE);
        
        selectedTrick = "";
        selectedTerrain = "";
        selectedTrickIndex = -1;
        selectedTerrainIndex = -1;
        videoPath = "";

        tvSelectedTrick.setText("Trick: None selected");
        btnPickTrick.setText("Choose Trick");
        tvSelectedTerrain.setText("Terrain: None selected");
        btnPickTerrain.setText("Choose Terrain");
        tvVideoPath.setText("No video selected");
        btnSaveTrick.setEnabled(false);
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
