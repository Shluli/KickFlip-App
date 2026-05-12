package uri.app.kickflip;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SubTerrainPickerFragment extends Fragment {

    private static final String ARG_CATEGORY = "category";
    private static final String ARG_COLOR    = "color";

    // ---- Data ----------------------------------------------------------------

    public static class SubTerrainItem {
        public final String name;
        public final String description;
        /** The terrain key passed to TrickPickerFragment / DifficultyEngine. Defaults to name. */
        public final String terrainKey;
        /** Pre-selected SIZE measurement (1–4). 0 means no preset. */
        public final int presetMeasurement;

        public SubTerrainItem(String name, String description) {
            this(name, description, name, 0);
        }

        public SubTerrainItem(String name, String description, String terrainKey, int presetMeasurement) {
            this.name = name;
            this.description = description;
            this.terrainKey = terrainKey;
            this.presetMeasurement = presetMeasurement;
        }
    }

    private static final Map<String, List<SubTerrainItem>> SUB_TERRAINS = new HashMap<>();

    static {
        SUB_TERRAINS.put("Pool", Arrays.asList(
            new SubTerrainItem("Small Quarter Pipe (2ft)", "Beginner transition",  "Quarter Pipe", 1),
            new SubTerrainItem("Medium Quarter Pipe (4ft)","Standard transition",  "Quarter Pipe", 2),
            new SubTerrainItem("Big Quarter Pipe (6ft)",   "Large transition",     "Quarter Pipe", 3),
            new SubTerrainItem("Vert (8ft+)",              "Full vert ramp",       "Quarter Pipe", 4)
        ));

        SUB_TERRAINS.put("Park", Arrays.asList(
            new SubTerrainItem("Ramp",       "Launch ramp"),
            new SubTerrainItem("Bank",       "Sloped surface"),
            new SubTerrainItem("Pyramid",    "Four-sided wedge"),
            new SubTerrainItem("Euro Gap",   "Barrier gap"),
            new SubTerrainItem("Frame",      "Rectangular obstacle"),
            new SubTerrainItem("Frame Gap",  "Gap over a frame")
        ));

        SUB_TERRAINS.put("Street", Arrays.asList(
            new SubTerrainItem("Ledge",             "Concrete ledge"),
            new SubTerrainItem("Rail",              "Straight handrail"),
            new SubTerrainItem("Curved Rail",       "Curved handrail"),
            new SubTerrainItem("Hubba",             "Hubba ledge"),
            new SubTerrainItem("Down Stairs",       "Jump down a set"),
            new SubTerrainItem("Up Stairs",         "Jump up a set"),
            new SubTerrainItem("Gap",               "Flat ground gap"),
            new SubTerrainItem("Curb",              "Street curb"),
            new SubTerrainItem("Grass",             "Grass patch"),
            new SubTerrainItem("On a manual pad",   "Flat manual pad"),
            new SubTerrainItem("Up a Manual Pad",   "Up onto a pad"),
            new SubTerrainItem("Bench",             "Park bench"),
            new SubTerrainItem("Table",             "Picnic table"),
            new SubTerrainItem("Over an obsticle",  "Jump over something")
        ));
    }

    // ---- Factory -------------------------------------------------------------

    public static SubTerrainPickerFragment newInstance(String category, int colorHex) {
        SubTerrainPickerFragment f = new SubTerrainPickerFragment();
        Bundle args = new Bundle();
        args.putString(ARG_CATEGORY, category);
        args.putInt(ARG_COLOR, colorHex);
        f.setArguments(args);
        return f;
    }

    // ---- Fields --------------------------------------------------------------

    private String category;
    private int    colorHex;

    // ---- Lifecycle -----------------------------------------------------------

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            category = getArguments().getString(ARG_CATEGORY, "");
            colorHex = getArguments().getInt(ARG_COLOR, 0xFF111111);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_sub_terrain_picker, container, false);

        view.findViewById(R.id.btn_back).setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());

        ((TextView) view.findViewById(R.id.tv_sub_category_label)).setText(category);

        View accent = view.findViewById(R.id.view_category_accent);
        accent.setBackgroundColor(colorHex);

        List<SubTerrainItem> items = SUB_TERRAINS.get(category);
        if (items == null) items = new ArrayList<>();

        RecyclerView rv = view.findViewById(R.id.rv_sub_terrain);
        rv.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        rv.setAdapter(new SubTerrainAdapter(items));

        return view;
    }

    // ---- Adapter -------------------------------------------------------------

    private class SubTerrainAdapter extends RecyclerView.Adapter<SubTerrainAdapter.VH> {
        private final List<SubTerrainItem> items;

        SubTerrainAdapter(List<SubTerrainItem> items) { this.items = items; }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_sub_terrain_card, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            SubTerrainItem item = items.get(position);
            holder.tvName.setText(item.name);
            holder.tvDesc.setText(item.description);
            holder.accent.setBackgroundColor(colorHex);

            holder.itemView.setOnClickListener(v -> {
                animateBounce(holder.itemView);
                v.postDelayed(() -> navigateToTrickPicker(item), 220);
            });
        }

        @Override
        public int getItemCount() { return items.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView tvName, tvDesc;
            View accent;

            VH(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tv_sub_terrain_name);
                tvDesc = itemView.findViewById(R.id.tv_sub_terrain_desc);
                accent = itemView.findViewById(R.id.view_sub_accent);
            }
        }
    }

    // ---- Helpers -------------------------------------------------------------

    private void animateBounce(View view) {
        PropertyValuesHolder scaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 0.93f, 1f);
        PropertyValuesHolder scaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 0.93f, 1f);
        ObjectAnimator anim = ObjectAnimator.ofPropertyValuesHolder(view, scaleX, scaleY);
        anim.setDuration(200);
        anim.start();
    }

    private void navigateToTrickPicker(SubTerrainItem item) {
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(
                        R.anim.slide_in_right, R.anim.slide_out_left,
                        R.anim.slide_in_left,  R.anim.slide_out_right)
                .replace(R.id.homeFragmentContainer,
                        TrickPickerFragment.newInstance(item.terrainKey, item.presetMeasurement))
                .addToBackStack(null)
                .commit();
    }
}
