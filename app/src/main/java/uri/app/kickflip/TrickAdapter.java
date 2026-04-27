package uri.app.kickflip;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class TrickAdapter extends RecyclerView.Adapter<TrickAdapter.TrickViewHolder> {

    private List<VaultFragment.TrickEntry> trickList;
    private OnTrickClickListener listener;

    public interface OnTrickClickListener {
        void onTrickClick(VaultFragment.TrickEntry trick);
        void onTrickLongClick(VaultFragment.TrickEntry trick);
    }

    public TrickAdapter(List<VaultFragment.TrickEntry> trickList, OnTrickClickListener listener) {
        this.trickList = trickList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public TrickViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_trick, parent, false);
        return new TrickViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TrickViewHolder holder, int position) {
        VaultFragment.TrickEntry trick = trickList.get(position);
        holder.bind(trick, listener);
    }

    @Override
    public int getItemCount() {
        return trickList.size();
    }

    static class TrickViewHolder extends RecyclerView.ViewHolder {
        private TextView tvTrickName;
        private TextView tvTrickType;
        private TextView tvTerrainInfo;
        private TextView tvDate;
        private TextView tvDifficulty;
        private View ivVideoIndicator;
        private View difficultyBar;

        public TrickViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTrickName = itemView.findViewById(R.id.tv_trick_name);
            tvTrickType = itemView.findViewById(R.id.tv_trick_type);
            tvTerrainInfo = itemView.findViewById(R.id.tv_terrain_info);
            tvDate = itemView.findViewById(R.id.tv_date);
            tvDifficulty = itemView.findViewById(R.id.tv_difficulty);
            ivVideoIndicator = itemView.findViewById(R.id.iv_video_indicator);
            difficultyBar = itemView.findViewById(R.id.difficulty_bar);
        }

        public void bind(VaultFragment.TrickEntry trick, OnTrickClickListener listener) {
            tvTrickName.setText(trick.name);
            tvTrickType.setText(trick.trick);
            
            // Build terrain info string
            String terrainInfo = trick.terrain;
            if (trick.measurement > 0) {
                if (trick.terrain.equals("Stairs")) {
                    terrainInfo += " (" + trick.measurement + " stairs)";
                } else if (trick.terrain.equals("Gap")) {
                    terrainInfo += " (" + trick.measurement + "cm length)";
                } else {
                    terrainInfo += " (" + trick.measurement + "cm)";
                }
            }
            tvTerrainInfo.setText(terrainInfo);
            
            // Format date (show just date, not time)
            String dateStr = trick.date != null ? trick.date : "";
            if (dateStr.contains(" ")) {
                dateStr = dateStr.split(" ")[0];
            }
            tvDate.setText(dateStr);
            
            // Display difficulty
            tvDifficulty.setText(String.valueOf(trick.difficulty));
            setDifficultyColor(trick.difficulty);
            
            // Show video indicator if video exists
            if (trick.videoPath != null && !trick.videoPath.isEmpty()) {
                ivVideoIndicator.setVisibility(View.VISIBLE);
            } else {
                ivVideoIndicator.setVisibility(View.GONE);
            }
            
            // Click listeners
            itemView.setOnClickListener(v -> listener.onTrickClick(trick));
            itemView.setOnLongClickListener(v -> {
                listener.onTrickLongClick(trick);
                return true;
            });
        }

        private void setDifficultyColor(int difficulty) {
            int color;
            switch (difficulty) {
                case 1:
                    color = 0xFF4CAF50; // Green - Easy
                    break;
                case 2:
                    color = 0xFF8BC34A; // Light Green
                    break;
                case 3:
                    color = 0xFFFFC107; // Yellow - Medium
                    break;
                case 4:
                    color = 0xFFFF9800; // Orange
                    break;
                case 5:
                    color = 0xFFF44336; // Red - Hard
                    break;
                default:
                    color = 0xFF9E9E9E; // Gray
            }
            
            tvDifficulty.setTextColor(color);
            difficultyBar.setBackgroundColor(color);
        }
    }

    public void updateTricks(List<VaultFragment.TrickEntry> newTricks) {
        this.trickList = newTricks;
        notifyDataSetChanged();
    }
}
