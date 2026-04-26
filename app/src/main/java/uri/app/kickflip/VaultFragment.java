package uri.app.kickflip;

import android.app.AlertDialog;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class VaultFragment extends Fragment {

    private RecyclerView recyclerView;
    private TrickAdapter adapter;
    private TextView tvEmptyState;
    private FloatingActionButton fabAddTrick;
    private Button btnSort;
    private List<TrickEntry> trickList;

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
        loadTricks();

        // Reload from Firestore whenever AddTrickFragment saves a trick
        getParentFragmentManager().setFragmentResultListener("tricks_updated", getViewLifecycleOwner(),
                (requestKey, bundle) -> loadTricks());

        return view;
    }

    private void initializeViews(View view) {
        recyclerView = view.findViewById(R.id.recycler_view_tricks);
        tvEmptyState = view.findViewById(R.id.tv_empty_state);
        fabAddTrick = view.findViewById(R.id.fab_add_trick);
        btnSort = view.findViewById(R.id.btn_sort);
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

        // FIXED: Changed from 5 columns to 2 for readability
        recyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        recyclerView.setAdapter(adapter);
    }

    private void setupFab() {
        fabAddTrick.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.homeFragmentContainer, new AddTrickFragment())
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
        btnSort.setOnClickListener(v -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Sort By")
                    .setItems(options, (dialog, which) -> {
                        switch (which) {
                            case 0:
                                Collections.sort(trickList, (a, b) -> a.difficulty - b.difficulty);
                                break;
                            case 1:
                                Collections.sort(trickList, (a, b) -> b.difficulty - a.difficulty);
                                break;
                            case 2:
                                Collections.sort(trickList, (a, b) -> a.date.compareTo(b.date));
                                break;
                            case 3:
                                Collections.sort(trickList, (a, b) -> b.date.compareTo(a.date));
                                break;
                        }
                        adapter.notifyDataSetChanged();
                    })
                    .show();
        });
    }

    private void loadTricks() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        FirebaseFirestore.getInstance()
                .collection("users").document(user.getUid())
                .collection("tricks")
                .get()
                .addOnSuccessListener(snapshot -> {
                    trickList.clear();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        Long measurement = doc.getLong("measurement");
                        Long difficulty = doc.getLong("difficulty");
                        trickList.add(new TrickEntry(
                                doc.getString("name"),
                                doc.getString("trick"),
                                doc.getString("terrain"),
                                measurement != null ? measurement.intValue() : 0,
                                difficulty != null ? difficulty.intValue() : 1,
                                doc.getString("videoPath"),
                                doc.getString("date")
                        ));
                    }
                    adapter.notifyDataSetChanged();
                    updateEmptyState();
                });
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
        Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_trick_detail);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }

        ((TextView) dialog.findViewById(R.id.dialog_tv_name)).setText(trick.name);
        ((TextView) dialog.findViewById(R.id.dialog_tv_type)).setText(trick.trick);
        ((TextView) dialog.findViewById(R.id.dialog_tv_terrain)).setText(
                trick.measurement > 0 ? trick.terrain + " (" + trick.measurement + "cm)" : trick.terrain
        );
        ((TextView) dialog.findViewById(R.id.dialog_tv_difficulty)).setText(String.valueOf(trick.difficulty));
        ((TextView) dialog.findViewById(R.id.dialog_tv_date)).setText(trick.date);

        String videoStatus = (trick.videoPath != null && !trick.videoPath.isEmpty()) ? "Yes" : "No";
        ((TextView) dialog.findViewById(R.id.dialog_tv_video)).setText(videoStatus);

        dialog.findViewById(R.id.dialog_btn_close).setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void showTrickOptions(TrickEntry trick) {
        // TODO: Bottom sheet with edit/delete
    }

    public static class TrickEntry {
        public String name, trick, terrain, videoPath, date;
        public int measurement, difficulty;

        public TrickEntry() {} // Required empty constructor for Firebase

        public TrickEntry(String name, String trick, String terrain, int measurement,
                          int difficulty, String videoPath, String date) {
            this.name = name; this.trick = trick; this.terrain = terrain;
            this.measurement = measurement; this.difficulty = difficulty;
            this.videoPath = videoPath; this.date = date;
        }
    }
}