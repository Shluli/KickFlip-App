package uri.app.kickflip;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.Arrays;
import java.util.List;

public class TerrainPickerFragment extends Fragment {

    public static class CategoryItem {
        public final String name;
        public final int colorHex;
        public final String description;
        public final int iconRes;
        public final boolean directToTricks; // true = skip sub-terrain picker

        public CategoryItem(String name, int colorHex, String description, int iconRes, boolean directToTricks) {
            this.name = name;
            this.colorHex = colorHex;
            this.description = description;
            this.iconRes = iconRes;
            this.directToTricks = directToTricks;
        }
    }

    private static final List<CategoryItem> CATEGORIES = Arrays.asList(
        new CategoryItem("Flatground", Color.parseColor("#3B82F6"), "No obstacles, pure tech", R.drawable.ic_terrain_flatground, true),
        new CategoryItem("Pool",       Color.parseColor("#8B5CF6"), "Transitions & coping",   R.drawable.ic_terrain_bowl,       false),
        new CategoryItem("Park",       Color.parseColor("#10B981"), "Ramps & park obstacles", R.drawable.ic_terrain_park,       false),
        new CategoryItem("Street",     Color.parseColor("#EF4444"), "Ledges, rails & gaps",   R.drawable.ic_terrain_street,     false)
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_terrain_picker, container, false);
        view.findViewById(R.id.btn_back).setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());

        RecyclerView rv = view.findViewById(R.id.rv_terrain);
        rv.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        rv.setAdapter(new CategoryAdapter());
        return view;
    }

    private class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.VH> {

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_terrain_card, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            CategoryItem item = CATEGORIES.get(position);
            holder.cardRoot.setBackgroundColor(item.colorHex);
            holder.ivIcon.setImageResource(item.iconRes);
            holder.tvName.setText(item.name);
            holder.tvDesc.setText(item.description);
            holder.tvDifficulty.setVisibility(View.GONE);

            holder.card.setOnClickListener(v ->
                animateCardTap(holder.card, () -> onCategorySelected(item)));
        }

        @Override
        public int getItemCount() { return CATEGORIES.size(); }

        class VH extends RecyclerView.ViewHolder {
            CardView card;
            ViewGroup cardRoot;
            ImageView ivIcon;
            TextView tvName, tvDesc, tvDifficulty;

            VH(@NonNull View itemView) {
                super(itemView);
                card     = (CardView) itemView;
                cardRoot = itemView.findViewById(R.id.terrain_card_root);
                ivIcon   = itemView.findViewById(R.id.iv_terrain_icon);
                tvName   = itemView.findViewById(R.id.tv_terrain_name);
                tvDesc   = itemView.findViewById(R.id.tv_terrain_desc);
                tvDifficulty = itemView.findViewById(R.id.tv_terrain_difficulty);
            }
        }
    }

    private void animateCardTap(View view, Runnable onEnd) {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, "scaleX", 1f, 0.92f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 0.92f, 1f);
        scaleX.setDuration(200);
        scaleY.setDuration(200);
        AnimatorSet set = new AnimatorSet();
        set.playTogether(scaleX, scaleY);
        set.start();
        view.postDelayed(onEnd, 180);
    }

    private void onCategorySelected(CategoryItem item) {
        Fragment next = item.directToTricks
                ? TrickPickerFragment.newInstance(item.name)
                : SubTerrainPickerFragment.newInstance(item.name, item.colorHex);

        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(
                        R.anim.slide_in_right, R.anim.slide_out_left,
                        R.anim.slide_in_left,  R.anim.slide_out_right)
                .replace(R.id.homeFragmentContainer, next)
                .addToBackStack(null)
                .commit();
    }
}
