package uri.app.kickflip;

import android.os.Bundle;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WeatherFragment extends Fragment {

    private EditText        etLocationInput;
    private Button          btnAddLocation;
    private RecyclerView    rvSpots;
    private TextView        tvEmptyState;
    private SpotAdapter     adapter;
    private List<SkateSpot> skateSpots;
    private weatherApiService weatherService;

    // Firebase
    private FirebaseFirestore db;
    private String            userId;

    private static final String COL_LOCATIONS = "weather_locations";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_weather, container, false);

        etLocationInput = view.findViewById(R.id.etLocationInput);
        btnAddLocation  = view.findViewById(R.id.btnAddLocation);
        rvSpots         = view.findViewById(R.id.rvSpots);
        tvEmptyState    = view.findViewById(R.id.tvEmptyState);

        skateSpots    = new ArrayList<>();
        weatherService = new weatherApiService(getContext());

        adapter = new SpotAdapter(skateSpots);
        rvSpots.setLayoutManager(new LinearLayoutManager(getContext()));
        rvSpots.setAdapter(adapter);

        btnAddLocation.setOnClickListener(v -> addLocation());

        // Firebase setup
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            userId = user.getUid();
            db     = FirebaseFirestore.getInstance();
            loadSavedLocations();
        } else {
            updateEmptyState();
        }

        return view;
    }

    // ── Load saved cities from Firestore ─────────────────────────────────────

    private void loadSavedLocations() {
        db.collection("users").document(userId)
          .collection(COL_LOCATIONS)
          .get()
          .addOnSuccessListener(snapshot -> {
              if (snapshot.isEmpty()) {
                  updateEmptyState();
                  return;
              }
              for (com.google.firebase.firestore.DocumentSnapshot doc : snapshot.getDocuments()) {
                  String name = doc.getString("name");
                  if (name != null && !name.isEmpty()) {
                      fetchAndAddSpot(name, doc.getId());
                  }
              }
          })
          .addOnFailureListener(e ->
              Toast.makeText(getContext(), "Couldn't load saved spots", Toast.LENGTH_SHORT).show()
          );
    }

    // ── Add a new location ───────────────────────────────────────────────────

    private void addLocation() {
        String location = etLocationInput.getText().toString().trim();
        if (location.isEmpty()) {
            Toast.makeText(getContext(), "Enter a city name", Toast.LENGTH_SHORT).show();
            return;
        }

        // Prevent duplicates
        for (SkateSpot s : skateSpots) {
            if (s.getLocationName().equalsIgnoreCase(location)) {
                Toast.makeText(getContext(), "Already added", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        btnAddLocation.setEnabled(false);
        Toast.makeText(getContext(), "Checking weather…", Toast.LENGTH_SHORT).show();

        weatherService.getWeather(location, new weatherApiService.WeatherCallback() {
            @Override
            public void onSuccess(SkateSpot spot) {
                skateSpots.add(spot);
                adapter.notifyItemInserted(skateSpots.size() - 1);
                etLocationInput.setText("");
                btnAddLocation.setEnabled(true);
                updateEmptyState();

                // Persist to Firestore
                if (db != null) {
                    Map<String, Object> data = new HashMap<>();
                    data.put("name", spot.getLocationName()); // use the API-resolved name
                    data.put("addedAt", FieldValue.serverTimestamp());

                    db.collection("users").document(userId)
                      .collection(COL_LOCATIONS)
                      .add(data)
                      .addOnSuccessListener(docRef -> spot.setFirestoreDocId(docRef.getId()));
                }
            }

            @Override
            public void onError(String error) {
                Toast.makeText(getContext(), "City not found", Toast.LENGTH_SHORT).show();
                btnAddLocation.setEnabled(true);
            }
        });
    }

    // ── Fetch weather for a pre-saved city ───────────────────────────────────

    private void fetchAndAddSpot(String cityName, String docId) {
        weatherService.getWeather(cityName, new weatherApiService.WeatherCallback() {
            @Override
            public void onSuccess(SkateSpot spot) {
                spot.setFirestoreDocId(docId);
                skateSpots.add(spot);
                adapter.notifyItemInserted(skateSpots.size() - 1);
                updateEmptyState();
            }

            @Override
            public void onError(String error) {
                // City may no longer be valid; leave it in Firestore, just skip display
            }
        });
    }

    // ── Refresh a spot ────────────────────────────────────────────────────────

    private void refreshSpot(SkateSpot spot, int position) {
        weatherService.getWeather(spot.getLocationName(), new weatherApiService.WeatherCallback() {
            @Override
            public void onSuccess(SkateSpot updated) {
                updated.setFirestoreDocId(spot.getFirestoreDocId()); // preserve doc ID
                skateSpots.set(position, updated);
                adapter.notifyItemChanged(position);
                Toast.makeText(getContext(), "Updated!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String error) {
                Toast.makeText(getContext(), "Refresh failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ── Delete a spot ─────────────────────────────────────────────────────────

    private void deleteSpot(int position) {
        SkateSpot spot = skateSpots.get(position);
        skateSpots.remove(position);
        adapter.notifyItemRemoved(position);
        updateEmptyState();

        if (db != null && spot.getFirestoreDocId() != null) {
            db.collection("users").document(userId)
              .collection(COL_LOCATIONS)
              .document(spot.getFirestoreDocId())
              .delete();
        }
    }

    private void updateEmptyState() {
        tvEmptyState.setVisibility(skateSpots.isEmpty() ? View.VISIBLE : View.GONE);
    }

    // ── RecyclerView Adapter ──────────────────────────────────────────────────

    private class SpotAdapter extends RecyclerView.Adapter<SpotAdapter.SpotViewHolder> {
        private final List<SkateSpot> spots;

        SpotAdapter(List<SkateSpot> spots) { this.spots = spots; }

        @NonNull
        @Override
        public SpotViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_skate_spot, parent, false);
            return new SpotViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull SpotViewHolder holder, int position) {
            holder.bind(spots.get(position));
        }

        @Override
        public int getItemCount() { return spots.size(); }

        class SpotViewHolder extends RecyclerView.ViewHolder {
            TextView tvLocationName, tvTemperature, tvCondition,
                     tvGroundStatus, tvGroundSub, tvLastRain;
            View statusIndicator;

            SpotViewHolder(@NonNull View itemView) {
                super(itemView);
                tvLocationName  = itemView.findViewById(R.id.tvLocationName);
                tvTemperature   = itemView.findViewById(R.id.tvTemperature);
                tvCondition     = itemView.findViewById(R.id.tvCondition);
                tvGroundStatus  = itemView.findViewById(R.id.tvGroundStatus);
                tvGroundSub     = itemView.findViewById(R.id.tvGroundSub);
                tvLastRain      = itemView.findViewById(R.id.tvLastRain);
                statusIndicator = itemView.findViewById(R.id.statusIndicator);
            }

            void bind(SkateSpot spot) {
                tvLocationName.setText(spot.getLocationName());
                tvTemperature.setText(spot.getTemperature() + "°C");
                tvCondition.setText(spot.getWeatherCondition());
                tvGroundStatus.setText(spot.getGroundStatus());
                tvGroundSub.setText(spot.getGroundSubtitle());
                tvLastRain.setText(spot.getLastRainInfo());

                int color;
                switch (spot.getGroundStatusLevel()) {
                    case SkateSpot.STATUS_DRY:     color = 0xFF4CAF50; break;
                    case SkateSpot.STATUS_DRYING:  color = 0xFFFF9800; break;
                    default:                       color = 0xFFF44336; break;
                }
                statusIndicator.setBackgroundColor(color);

                itemView.findViewById(R.id.btnRefresh).setOnClickListener(v -> {
                    int pos = getAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION) refreshSpot(spots.get(pos), pos);
                });

                itemView.findViewById(R.id.btnDelete).setOnClickListener(v -> {
                    int pos = getAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION) deleteSpot(pos);
                });
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (weatherService != null) weatherService.shutdown();
    }
}
