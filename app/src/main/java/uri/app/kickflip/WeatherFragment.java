package uri.app.kickflip; // Change this to match your package name

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class WeatherFragment extends Fragment {

    private EditText etLocationInput;
    private Button btnAddLocation;
    private RecyclerView rvSpots;
    private SpotAdapter adapter;
    private List<SkateSpot> skateSpots;
    private weatherApiService weatherService;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_weather, container, false);

        // Initialize views
        etLocationInput = view.findViewById(R.id.etLocationInput);
        btnAddLocation = view.findViewById(R.id.btnAddLocation);
        rvSpots = view.findViewById(R.id.rvSpots);

        // Initialize data
        skateSpots = new ArrayList<>();
        weatherService = new weatherApiService(getContext());

        // Setup RecyclerView
        adapter = new SpotAdapter(skateSpots);
        rvSpots.setLayoutManager(new LinearLayoutManager(getContext()));
        rvSpots.setAdapter(adapter);

        // Add location button click
        btnAddLocation.setOnClickListener(v -> addLocation());

        // Add some default spots (optional - you can remove this)
        addDefaultSpots();

        return view;
    }

    private void addLocation() {
        String location = etLocationInput.getText().toString().trim();

        if (location.isEmpty()) {
            Toast.makeText(getContext(), "Enter a location!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show loading
        Toast.makeText(getContext(), "Checking weather...", Toast.LENGTH_SHORT).show();

        // Fetch weather data
        weatherService.getWeather(location, new weatherApiService.WeatherCallback() {
            @Override
            public void onSuccess(SkateSpot spot) {
                skateSpots.add(spot);
                adapter.notifyItemInserted(skateSpots.size() - 1);
                etLocationInput.setText("");
                Toast.makeText(getContext(), "Location added!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String error) {
                Toast.makeText(getContext(), "Error: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addDefaultSpots() {
        // Add your local skate spots here as examples
        // Users can add more or you can remove this
    }

    // RecyclerView Adapter
    private class SpotAdapter extends RecyclerView.Adapter<SpotAdapter.SpotViewHolder> {
        private List<SkateSpot> spots;

        SpotAdapter(List<SkateSpot> spots) {
            this.spots = spots;
        }

        @NonNull
        @Override
        public SpotViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_skate_spot, parent, false);
            return new SpotViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull SpotViewHolder holder, int position) {
            SkateSpot spot = spots.get(position);
            holder.bind(spot);
        }

        @Override
        public int getItemCount() {
            return spots.size();
        }

        class SpotViewHolder extends RecyclerView.ViewHolder {
            TextView tvLocationName, tvTemperature, tvCondition, tvGroundStatus, tvLastRain;
            View statusIndicator;

            SpotViewHolder(@NonNull View itemView) {
                super(itemView);
                tvLocationName = itemView.findViewById(R.id.tvLocationName);
                tvTemperature = itemView.findViewById(R.id.tvTemperature);
                tvCondition = itemView.findViewById(R.id.tvCondition);
                tvGroundStatus = itemView.findViewById(R.id.tvGroundStatus);
                tvLastRain = itemView.findViewById(R.id.tvLastRain);
                statusIndicator = itemView.findViewById(R.id.statusIndicator);
            }

            void bind(SkateSpot spot) {
                tvLocationName.setText(spot.getLocationName());
                tvTemperature.setText(spot.getTemperature() + "°C");
                tvCondition.setText(spot.getWeatherCondition());
                tvGroundStatus.setText(spot.getGroundStatus());
                tvLastRain.setText(spot.getLastRainInfo());

                // Set status color
                int color;
                switch (spot.getGroundStatusLevel()) {
                    case SkateSpot.STATUS_DRY:
                        color = 0xFF4CAF50; // Green
                        break;
                    case SkateSpot.STATUS_DRYING:
                        color = 0xFFFF9800; // Orange
                        break;
                    default:
                        color = 0xFFF44336; // Red
                        break;
                }
                statusIndicator.setBackgroundColor(color);

                // Refresh button
                itemView.findViewById(R.id.btnRefresh).setOnClickListener(v -> {
                    int pos = getAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION) {
                        refreshSpot(spot, pos);
                    }
                });

                // Delete button
                itemView.findViewById(R.id.btnDelete).setOnClickListener(v -> {
                    int pos = getAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION) {
                        spots.remove(pos);
                        notifyItemRemoved(pos);
                    }
                });
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (weatherService != null) {
            weatherService.shutdown();
        }
    }

    private void refreshSpot(SkateSpot spot, int position) {
        weatherService.getWeather(spot.getLocationName(), new weatherApiService.WeatherCallback() {
            @Override
            public void onSuccess(SkateSpot updatedSpot) {
                skateSpots.set(position, updatedSpot);
                adapter.notifyItemChanged(position);
                Toast.makeText(getContext(), "Updated!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String error) {
                Toast.makeText(getContext(), "Refresh failed", Toast.LENGTH_SHORT).show();
            }
        });
    }
}