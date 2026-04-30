package uri.app.kickflip;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class VaultFragment extends Fragment {

    private RecyclerView recyclerView;
    private TrickAdapter adapter;
    private TextView tvEmptyState;
    private ProgressBar pbLoading;
    private FloatingActionButton fabAddTrick;
    private ImageButton btnSort;
    private List<TrickEntry> trickList;
    private ListenerRegistration snapshotListener;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        trickList = new ArrayList<>();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_vault, container, false);

        initializeViews(view);
        setupRecyclerView();
        setupFab();
        setupSort();
        attachListener();

        return view;
    }

    private void initializeViews(View view) {
        recyclerView  = view.findViewById(R.id.recycler_view_tricks);
        tvEmptyState  = view.findViewById(R.id.tv_empty_state);
        pbLoading     = view.findViewById(R.id.pb_loading);
        fabAddTrick   = view.findViewById(R.id.fab_add_trick);
        btnSort       = view.findViewById(R.id.btn_sort);
    }

    private void setupRecyclerView() {
        adapter = new TrickAdapter(trickList, new TrickAdapter.OnTrickClickListener() {
            @Override
            public void onTrickClick(TrickEntry trick) {
                showTrickDetailPopup(trick);
            }

            @Override
            public void onTrickLongClick(TrickEntry trick) {
                showTrickOptions(trick);
            }
        });

        recyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 3));
        recyclerView.setAdapter(adapter);
    }

    private void setupFab() {
        fabAddTrick.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .setCustomAnimations(
                            R.anim.slide_in_right,
                            R.anim.slide_out_left,
                            R.anim.slide_in_left,
                            R.anim.slide_out_right)
                    .replace(R.id.homeFragmentContainer, new TerrainPickerFragment())
                    .addToBackStack(null)
                    .commit();
        });
    }

    private void setupSort() {
        String[] options = {
                "Easiest to Hardest",
                "Hardest to Easiest",
                "First Added",
                "Last Added"
        };
        btnSort.setOnClickListener(v -> new AlertDialog.Builder(requireContext())
                .setTitle("Sort By")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0: Collections.sort(trickList, (a, b) -> a.difficulty - b.difficulty); break;
                        case 1: Collections.sort(trickList, (a, b) -> b.difficulty - a.difficulty); break;
                        case 2: Collections.sort(trickList, (a, b) -> a.date.compareTo(b.date)); break;
                        case 3: Collections.sort(trickList, (a, b) -> b.date.compareTo(a.date)); break;
                    }
                    adapter.notifyDataSetChanged();
                })
                .show());
    }

    private void attachListener() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        pbLoading.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        tvEmptyState.setVisibility(View.GONE);

        snapshotListener = FirebaseFirestore.getInstance()
                .collection("users").document(user.getUid())
                .collection("tricks")
                .addSnapshotListener((snapshot, error) -> {
                    if (!isAdded()) return;
                    pbLoading.setVisibility(View.GONE);
                    if (error != null || snapshot == null) return;

                    trickList.clear();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        Long measurement  = doc.getLong("measurement");
                        Long difficulty   = doc.getLong("difficulty");
                        Long spinDegrees  = doc.getLong("spinDegrees");
                        trickList.add(new TrickEntry(
                                doc.getId(),
                                doc.getString("name"),
                                doc.getString("trick"),
                                doc.getString("terrain"),
                                measurement != null ? measurement.intValue() : 0,
                                difficulty  != null ? difficulty.intValue()  : 1,
                                doc.getString("videoPath"),
                                doc.getString("date"),
                                spinDegrees != null ? spinDegrees.intValue() : 0,
                                doc.getString("direction"),
                                doc.getString("stance")
                        ));
                    }
                    adapter.notifyDataSetChanged();
                    updateEmptyState();
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (snapshotListener != null) {
            snapshotListener.remove();
            snapshotListener = null;
        }
    }

    private void updateEmptyState() {
        if (trickList.isEmpty()) {
            tvEmptyState.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            tvEmptyState.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void showTrickDetailPopup(TrickEntry trick) {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View sheetView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_trick_detail, null);
        dialog.setContentView(sheetView);

        ((TextView) sheetView.findViewById(R.id.dialog_tv_name)).setText(trick.name);
        ((TextView) sheetView.findViewById(R.id.dialog_tv_type)).setText(trick.trick);
        ((TextView) sheetView.findViewById(R.id.dialog_tv_terrain)).setText(
                trick.measurement > 0
                        ? trick.terrain + " (" + trick.measurement + "cm)"
                        : trick.terrain
        );
        TextView tvDiff = sheetView.findViewById(R.id.dialog_tv_difficulty);
        tvDiff.setText(DifficultyEngine.getRankName(trick.difficulty) + "  (" + trick.difficulty + ")");
        tvDiff.setTextColor(DifficultyEngine.getRankColor(trick.difficulty));
        ((TextView) sheetView.findViewById(R.id.dialog_tv_date)).setText(trick.date);

        boolean hasVideo = trick.videoPath != null && !trick.videoPath.isEmpty();
        ((TextView) sheetView.findViewById(R.id.dialog_tv_video)).setText(hasVideo ? "Yes" : "No");

        Button playVideoBtn = sheetView.findViewById(R.id.dialog_btn_play_video);
        if (hasVideo) {
            playVideoBtn.setVisibility(View.VISIBLE);
            playVideoBtn.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.parse(trick.videoPath), "video/*");
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(intent);
            });
        }

        sheetView.findViewById(R.id.dialog_btn_close).setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void showTrickOptions(TrickEntry trick) {
        new AlertDialog.Builder(requireContext())
                .setTitle(trick.name)
                .setItems(new String[]{"Delete trick"}, (d, which) -> confirmDelete(trick))
                .show();
    }

    private void confirmDelete(TrickEntry trick) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete trick?")
                .setMessage("\"" + trick.name + "\" will be permanently removed.")
                .setPositiveButton("Delete", (d, which) -> deleteTrick(trick))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteTrick(TrickEntry trick) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        FirebaseFirestore.getInstance()
                .collection("users").document(user.getUid())
                .collection("tricks").document(trick.docId)
                .delete()
                .addOnSuccessListener(v -> {
                    trickList.remove(trick);
                    adapter.notifyDataSetChanged();
                    updateEmptyState();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(requireContext(), "Delete failed: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show()
                );
    }

    public static class TrickEntry {
        public String docId, name, trick, terrain, videoPath, date;
        public int measurement, difficulty, spinDegrees;
        public String direction, stance;

        public TrickEntry() {} // Required empty constructor for Firebase

        public TrickEntry(String docId, String name, String trick, String terrain,
                          int measurement, int difficulty, String videoPath, String date,
                          int spinDegrees, String direction, String stance) {
            this.docId = docId;
            this.name = name; this.trick = trick; this.terrain = terrain;
            this.measurement = measurement; this.difficulty = difficulty;
            this.videoPath = videoPath; this.date = date;
            this.spinDegrees = spinDegrees;
            this.direction = direction != null ? direction : "";
            this.stance    = stance     != null ? stance     : "Regular";
        }
    }
}
