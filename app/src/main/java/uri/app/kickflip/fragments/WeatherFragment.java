package uri.app.kickflip;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
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
    private ImageButton     btnMyLocation;
    private RecyclerView    rvSpots;
    private TextView        tvEmptyState;
    private SpotAdapter     adapter;
    private List<SkateSpot> skateSpots;
    private weatherApiService weatherService;

    // Firebase
    private FirebaseFirestore db;
    private String            userId;

    private static final String COL_LOCATIONS        = "weather_locations";
    private static final int    LOCATION_PERM_REQUEST = 1001;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_weather, container, false);

        etLocationInput = view.findViewById(R.id.etLocationInput);
        btnAddLocation  = view.findViewById(R.id.btnAddLocation);
        btnMyLocation   = view.findViewById(R.id.btnMyLocation);
        rvSpots         = view.findViewById(R.id.rvSpots);
        tvEmptyState    = view.findViewById(R.id.tvEmptyState);

        skateSpots    = new ArrayList<>();
        weatherService = new weatherApiService(getContext());

        adapter = new SpotAdapter(skateSpots);
        rvSpots.setLayoutManager(new LinearLayoutManager(getContext()));
        rvSpots.setAdapter(adapter);

        btnAddLocation.setOnClickListener(v -> addLocation());
        btnMyLocation.setOnClickListener(v -> requestMyLocation());

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

    // ── My Location ───────────────────────────────────────────────────────────

    private void requestMyLocation() {
        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                                 Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_PERM_REQUEST);
            return;
        }
        fetchMyLocation();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == LOCATION_PERM_REQUEST
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            fetchMyLocation();
        } else {
            Toast.makeText(getContext(), "Location permission denied", Toast.LENGTH_SHORT).show();
        }
    }

    private void fetchMyLocation() {
        LocationManager lm = (LocationManager)
                requireContext().getSystemService(android.content.Context.LOCATION_SERVICE);

        if (lm == null) {
            Toast.makeText(getContext(), "Location unavailable", Toast.LENGTH_SHORT).show();
            return;
        }

        // Try last known location first (instant, no battery cost)
        Location last = null;
        if (ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            last = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (last == null)
                last = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        }

        if (last != null) {
            addLocationByCoords(last.getLatitude(), last.getLongitude());
            return;
        }

        // No cached location — request a single fresh fix
        Toast.makeText(getContext(), "Getting your location…", Toast.LENGTH_SHORT).show();
        btnMyLocation.setEnabled(false);

        LocationListener listener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location loc) {
                lm.removeUpdates(this);
                btnMyLocation.setEnabled(true);
                addLocationByCoords(loc.getLatitude(), loc.getLongitude());
            }

            @Override public void onProviderDisabled(@NonNull String provider) {}
            @Override public void onProviderEnabled(@NonNull String provider) {}
        };

        boolean hasGps     = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean hasNetwork = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        if (!hasGps && !hasNetwork) {
            Toast.makeText(getContext(), "Enable location services", Toast.LENGTH_SHORT).show();
            btnMyLocation.setEnabled(true);
            return;
        }

        String provider = hasNetwork ? LocationManager.NETWORK_PROVIDER : LocationManager.GPS_PROVIDER;
        try {
            lm.requestSingleUpdate(provider, listener, android.os.Looper.getMainLooper());
        } catch (SecurityException e) {
            btnMyLocation.setEnabled(true);
            Toast.makeText(getContext(), "Location permission error", Toast.LENGTH_SHORT).show();
        }
    }

    private void addLocationByCoords(double lat, double lon) {
        btnMyLocation.setEnabled(false);
        weatherService.getWeatherByCoords(lat, lon, new weatherApiService.WeatherCallback() {
            @Override
            public void onSuccess(SkateSpot spot) {
                // Check for duplicate by name
                for (SkateSpot s : skateSpots) {
                    if (s.getLocationName().equalsIgnoreCase(spot.getLocationName())) {
                        Toast.makeText(getContext(), spot.getLocationName() + " already added",
                                Toast.LENGTH_SHORT).show();
                        btnMyLocation.setEnabled(true);
                        return;
                    }
                }
                skateSpots.add(spot);
                adapter.notifyItemInserted(skateSpots.size() - 1);
                btnMyLocation.setEnabled(true);
                updateEmptyState();

                if (db != null) {
                    Map<String, Object> data = new HashMap<>();
                    data.put("name", spot.getLocationName());
                    data.put("addedAt", FieldValue.serverTimestamp());
                    db.collection("users").document(userId)
                      .collection(COL_LOCATIONS)
                      .add(data)
                      .addOnSuccessListener(docRef -> spot.setFirestoreDocId(docRef.getId()));
                }
            }

            @Override
            public void onError(String error) {
                Toast.makeText(getContext(), "Couldn't get your city", Toast.LENGTH_SHORT).show();
                btnMyLocation.setEnabled(true);
            }
        });
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
