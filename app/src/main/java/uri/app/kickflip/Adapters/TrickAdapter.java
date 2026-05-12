package uri.app.kickflip;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class TrickAdapter extends RecyclerView.Adapter<TrickAdapter.TrickViewHolder> {

    private List<VaultFragment.TrickEntry> trickList;
    private final OnTrickClickListener listener;

    public interface OnTrickClickListener {
        void onTrickClick(VaultFragment.TrickEntry trick);
        void onTrickLongClick(VaultFragment.TrickEntry trick);
    }

    public TrickAdapter(List<VaultFragment.TrickEntry> trickList, OnTrickClickListener listener) {
        this.trickList = trickList;
        this.listener  = listener;
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
        holder.bind(trickList.get(position), listener);
    }

    @Override
    public int getItemCount() { return trickList.size(); }

    static class TrickViewHolder extends RecyclerView.ViewHolder {
        private final TextView  tvModifier;
        private final TextView  tvTrickName;
        private final TextView  tvTerrainMeas;
        private final TextView  tvDate;
        private final ImageView ivDifficultyIcon;
        private final View      ivVideoDot;
        private final View      vBorderOverlay;

        TrickViewHolder(@NonNull View itemView) {
            super(itemView);
            tvModifier       = itemView.findViewById(R.id.tv_modifier);
            tvTrickName      = itemView.findViewById(R.id.tv_trick_name);
            tvTerrainMeas    = itemView.findViewById(R.id.tv_terrain_meas);
            tvDate           = itemView.findViewById(R.id.tv_date);
            ivDifficultyIcon = itemView.findViewById(R.id.iv_difficulty_icon);
            ivVideoDot       = itemView.findViewById(R.id.iv_video_dot);
            vBorderOverlay   = itemView.findViewById(R.id.v_border_overlay);
        }

        void bind(VaultFragment.TrickEntry trick, OnTrickClickListener listener) {
            tvTrickName.setText(trick.name);
            tvTerrainMeas.setText(buildTerrainLabel(trick));
            tvModifier.setText(buildModifier(trick));

            String dateStr = trick.date != null ? trick.date : "";
            if (dateStr.contains(" ")) dateStr = dateStr.split(" ")[0];
            tvDate.setText(dateStr);

            ivVideoDot.setVisibility(
                    trick.videoPath != null && !trick.videoPath.isEmpty()
                    ? View.VISIBLE : View.GONE);

            applyDifficulty(trick.difficulty);

            itemView.setOnClickListener(v -> listener.onTrickClick(trick));
            itemView.setOnLongClickListener(v -> { listener.onTrickLongClick(trick); return true; });
        }

        // "180° Fakie BS"  /  "Switch FS"  /  "BS"  — empty string if nothing applies
        private String buildModifier(VaultFragment.TrickEntry trick) {
            StringBuilder sb = new StringBuilder();
            if (trick.spinDegrees > 0) sb.append(trick.spinDegrees).append("° ");
            String st = trick.stance;
            if (st != null && !st.isEmpty() && !"Regular".equals(st)) sb.append(st).append(" ");
            String dir = trick.direction;
            if (dir != null && !dir.isEmpty()) {
                sb.append("Backside".equals(dir) ? "BS" : "FS");
            }
            return sb.toString().trim();
        }

        private String buildTerrainLabel(VaultFragment.TrickEntry trick) {
            String meas = DifficultyEngine.getMeasurementLabel(trick.terrain, trick.measurement);
            return meas.isEmpty() ? trick.terrain : trick.terrain + " · " + meas;
        }

        private void applyDifficulty(int score) {
            int rank  = DifficultyEngine.getRank(score);
            int color = DifficultyEngine.getRankColor(score);

            // Icon shape: circle (Bronze–Gold), diamond (Diamond), triangle (Epic–Legendary)
            int iconRes;
            if (rank <= DifficultyEngine.RANK_GOLD)      iconRes = R.drawable.ic_shape_circle;
            else if (rank == DifficultyEngine.RANK_DIAMOND) iconRes = R.drawable.ic_shape_diamond;
            else                                          iconRes = R.drawable.ic_shape_triangle;

            ivDifficultyIcon.setImageResource(iconRes);
            ivDifficultyIcon.setImageTintList(ColorStateList.valueOf(color));

            // Colored stroke border overlay
            float radius = itemView.getResources().getDisplayMetrics().density * 14;
            int   stroke = Math.round(itemView.getResources().getDisplayMetrics().density * 4);
            GradientDrawable border = new GradientDrawable();
            border.setShape(GradientDrawable.RECTANGLE);
            border.setCornerRadius(radius);
            border.setStroke(stroke, color);
            border.setColor(Color.TRANSPARENT);
            vBorderOverlay.setBackground(border);
        }
    }

    public void updateTricks(List<VaultFragment.TrickEntry> newTricks) {
        this.trickList = newTricks;
        notifyDataSetChanged();
    }
}
