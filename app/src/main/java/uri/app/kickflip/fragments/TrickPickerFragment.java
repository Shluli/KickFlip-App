package uri.app.kickflip;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.os.Bundle;
import android.view.HapticFeedbackConstants;
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

public class TrickPickerFragment extends Fragment {

    private static final String ARG_TERRAIN            = "terrain";
    private static final String ARG_PRESET_MEASUREMENT = "preset_measurement";

    // ---- Data model ----------------------------------------------------------

    public static class TrickItem {
        public final String name;
        public final int difficulty;
        public final String category; // FLIP / GRIND / SLIDE / OLLIE / AIR / MANUAL / STALL / HANDPLANT

        public TrickItem(String name, int difficulty, String category) {
            this.name = name;
            this.difficulty = difficulty;
            this.category = category;
        }
    }

    // ---- Shared trick lists --------------------------------------------------

    private static List<TrickItem> flatTricks() {
        return Arrays.asList(
            new TrickItem("Ollie",           1, "OLLIE"),
            new TrickItem("Nollie",          2, "OLLIE"),
            new TrickItem("Fakie Ollie",     2, "OLLIE"),
            new TrickItem("Pop Shuvit",      2, "OLLIE"),
            new TrickItem("Kickflip",        3, "FLIP"),
            new TrickItem("Heelflip",        3, "FLIP"),
            new TrickItem("Fakie Kickflip",  3, "FLIP"),
            new TrickItem("Varial Flip",     4, "FLIP"),
            new TrickItem("Hardflip",        4, "FLIP"),
            new TrickItem("Treflip",         5, "FLIP"),
            new TrickItem("Switch Kickflip", 5, "FLIP"),
            new TrickItem("Impossible",      4, "FLIP")
        );
    }

    private static List<TrickItem> jumpTricks() {
        return Arrays.asList(
            new TrickItem("Ollie",       2, "OLLIE"),
            new TrickItem("Pop Shuvit",  2, "OLLIE"),
            new TrickItem("Kickflip",    3, "FLIP"),
            new TrickItem("Heelflip",    3, "FLIP"),
            new TrickItem("Varial Flip", 4, "FLIP"),
            new TrickItem("Hardflip",    4, "FLIP"),
            new TrickItem("Treflip",     5, "FLIP"),
            new TrickItem("Impossible",  4, "FLIP")
        );
    }

    private static List<TrickItem> grindSlideTricks() {
        return Arrays.asList(
            new TrickItem("50-50",         2, "GRIND"),
            new TrickItem("5-0 Grind",     3, "GRIND"),
            new TrickItem("Nosegrind",     3, "GRIND"),
            new TrickItem("Boardslide",    3, "SLIDE"),
            new TrickItem("Noseslide",     3, "SLIDE"),
            new TrickItem("Tailslide",     3, "SLIDE"),
            new TrickItem("Crooked Grind", 4, "GRIND"),
            new TrickItem("Smith Grind",   4, "GRIND"),
            new TrickItem("Feeble Grind",  4, "GRIND"),
            new TrickItem("Lipslide",      4, "SLIDE"),
            new TrickItem("Bluntslide",    5, "SLIDE"),
            new TrickItem("Noseblunt",     5, "SLIDE")
        );
    }

    private static List<TrickItem> basicGrindTricks() {
        return Arrays.asList(
            new TrickItem("50-50",         2, "GRIND"),
            new TrickItem("5-0 Grind",     3, "GRIND"),
            new TrickItem("Nosegrind",     3, "GRIND"),
            new TrickItem("Boardslide",    3, "SLIDE"),
            new TrickItem("Noseslide",     3, "SLIDE"),
            new TrickItem("Tailslide",     3, "SLIDE"),
            new TrickItem("Crooked Grind", 4, "GRIND"),
            new TrickItem("Smith Grind",   4, "GRIND")
        );
    }

    private static List<TrickItem> quarterPipeTricks() {
        return Arrays.asList(
            // Stalls — no spin, BS/FS direction
            new TrickItem("Rock to Fakie",    2, "STALL"),
            new TrickItem("Axle Stall",       2, "STALL"),
            new TrickItem("Nosestall",        3, "STALL"),
            new TrickItem("Tailstall",        3, "STALL"),
            new TrickItem("Smith Stall",      3, "STALL"),
            new TrickItem("Disaster",         3, "STALL"),
            new TrickItem("Blunt Stall",      4, "STALL"),
            new TrickItem("Lien to Tail",     4, "STALL"),
            new TrickItem("Noseblunt Stall",  5, "STALL"),
            // Grinds — no spin, BS/FS direction
            new TrickItem("50-50",            3, "GRIND"),
            new TrickItem("5-0",              3, "GRIND"),
            new TrickItem("Nosegrind",        3, "GRIND"),
            new TrickItem("Smith Grind",      4, "GRIND"),
            new TrickItem("Layback Grind",    4, "GRIND"),
            // Air grabs — spin + BS/FS
            new TrickItem("Air",              2, "AIR"),
            new TrickItem("Indy Grab",        3, "AIR"),
            new TrickItem("Stalefish",        3, "AIR"),
            new TrickItem("Method Air",       3, "AIR"),
            new TrickItem("Melon Grab",       3, "AIR"),
            new TrickItem("Lien Air",         3, "AIR"),
            new TrickItem("Nose Grab",        3, "AIR"),
            new TrickItem("Tail Grab",        3, "AIR"),
            new TrickItem("Body Jar",         4, "AIR"),
            new TrickItem("Christ Air",       5, "AIR"),
            // Handplants
            new TrickItem("Eggplant",         4, "HANDPLANT"),
            new TrickItem("Invert",           5, "HANDPLANT"),
            // Flip tricks — spin eligible
            new TrickItem("Kickflip",         4, "FLIP"),
            new TrickItem("Heelflip",         4, "FLIP"),
            new TrickItem("Treflip",          5, "FLIP"),
            new TrickItem("Impossible",       4, "FLIP")
        );
    }

    private static List<TrickItem> manualTricks() {
        return Arrays.asList(
            new TrickItem("Manual",                   1, "MANUAL"),
            new TrickItem("Nose Manual",              2, "MANUAL"),
            new TrickItem("Casper",                   3, "MANUAL"),
            new TrickItem("Manual Shove-it Out",      3, "MANUAL"),
            new TrickItem("Kickflip to Manual",       4, "MANUAL"),
            new TrickItem("Heelflip to Manual",       4, "MANUAL"),
            new TrickItem("Nose Manual Kickflip Out", 4, "MANUAL")
        );
    }

    // ---- Terrain → tricks map ------------------------------------------------

    private static final Map<String, List<TrickItem>> TRICKS_BY_TERRAIN = new HashMap<>();

    static {
        // Flatground
        TRICKS_BY_TERRAIN.put("Flatground", flatTricks());

        TRICKS_BY_TERRAIN.put("Quarter Pipe", quarterPipeTricks());

        // Park
        TRICKS_BY_TERRAIN.put("Ramp", Arrays.asList(
            new TrickItem("Rock to Fakie", 2, "STALL"),
            new TrickItem("Axle Stall",    2, "STALL"),
            new TrickItem("Disaster",      3, "STALL"),
            new TrickItem("Kickflip",      3, "FLIP"),
            new TrickItem("Heelflip",      3, "FLIP"),
            new TrickItem("Indy Grab",     4, "AIR"),
            new TrickItem("Smith Grind",   4, "GRIND")
        ));
        TRICKS_BY_TERRAIN.put("Bank", Arrays.asList(
            new TrickItem("Ollie",       1, "OLLIE"),
            new TrickItem("Pop Shuvit",  2, "OLLIE"),
            new TrickItem("Kickflip",    3, "FLIP"),
            new TrickItem("Heelflip",    3, "FLIP"),
            new TrickItem("Smith Grind", 4, "GRIND")
        ));
        TRICKS_BY_TERRAIN.put("Pyramid", Arrays.asList(
            new TrickItem("Ollie",       2, "OLLIE"),
            new TrickItem("Kickflip",    3, "FLIP"),
            new TrickItem("Heelflip",    3, "FLIP"),
            new TrickItem("50-50",       3, "GRIND"),
            new TrickItem("Boardslide",  3, "SLIDE"),
            new TrickItem("Noseslide",   3, "SLIDE"),
            new TrickItem("Tailslide",   3, "SLIDE"),
            new TrickItem("Smith Grind", 4, "GRIND")
        ));
        TRICKS_BY_TERRAIN.put("Euro Gap",  jumpTricks());
        TRICKS_BY_TERRAIN.put("Frame",     jumpTricks());
        TRICKS_BY_TERRAIN.put("Frame Gap", jumpTricks());

        // Street — grinds / slides
        TRICKS_BY_TERRAIN.put("Ledge",       grindSlideTricks());
        TRICKS_BY_TERRAIN.put("Rail",        grindSlideTricks());
        TRICKS_BY_TERRAIN.put("Curved Rail", basicGrindTricks());
        TRICKS_BY_TERRAIN.put("Hubba",       basicGrindTricks());
        TRICKS_BY_TERRAIN.put("Bench",       grindSlideTricks());
        TRICKS_BY_TERRAIN.put("Table",       basicGrindTricks());
        TRICKS_BY_TERRAIN.put("Curb",        basicGrindTricks());

        // Street — jumps
        TRICKS_BY_TERRAIN.put("Down Stairs",     jumpTricks());
        TRICKS_BY_TERRAIN.put("Up Stairs",       jumpTricks());
        TRICKS_BY_TERRAIN.put("Gap",             jumpTricks());
        TRICKS_BY_TERRAIN.put("Over an obsticle",jumpTricks());

        // Street — grass
        TRICKS_BY_TERRAIN.put("Grass", Arrays.asList(
            new TrickItem("Ollie",       2, "OLLIE"),
            new TrickItem("Pop Shuvit",  3, "OLLIE"),
            new TrickItem("Kickflip",    4, "FLIP"),
            new TrickItem("Heelflip",    4, "FLIP")
        ));

        // Street — manuals
        TRICKS_BY_TERRAIN.put("On a manual pad", manualTricks());
        TRICKS_BY_TERRAIN.put("Up a Manual Pad", manualTricks());
    }

    // ---- Factory -------------------------------------------------------------

    public static TrickPickerFragment newInstance(String terrain) {
        return newInstance(terrain, 0);
    }

    public static TrickPickerFragment newInstance(String terrain, int presetMeasurement) {
        TrickPickerFragment f = new TrickPickerFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TERRAIN, terrain);
        args.putInt(ARG_PRESET_MEASUREMENT, presetMeasurement);
        f.setArguments(args);
        return f;
    }

    private String terrain;
    private int presetMeasurement;

    // ---- Lifecycle -----------------------------------------------------------

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            terrain            = getArguments().getString(ARG_TERRAIN, "");
            presetMeasurement  = getArguments().getInt(ARG_PRESET_MEASUREMENT, 0);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_trick_picker, container, false);

        view.findViewById(R.id.btn_back).setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());

        ((TextView) view.findViewById(R.id.tv_trick_terrain_label)).setText(terrainDisplayName());

        List<TrickItem> tricks = TRICKS_BY_TERRAIN.get(terrain);
        if (tricks == null) tricks = new ArrayList<>();

        RecyclerView rv = view.findViewById(R.id.rv_tricks);
        rv.setLayoutManager(new GridLayoutManager(requireContext(), 3));
        rv.setAdapter(new TrickAdapter(tricks));

        return view;
    }

    /** Human-readable label shown at the top of the screen. */
    private String terrainDisplayName() {
        if ("Quarter Pipe".equals(terrain) && presetMeasurement > 0) {
            String[] labels = {"Small Quarter Pipe (2ft)", "Medium Quarter Pipe (4ft)",
                               "Big Quarter Pipe (6ft)",   "Vert (8ft+)"};
            int idx = presetMeasurement - 1;
            if (idx >= 0 && idx < labels.length) return labels[idx];
        }
        return terrain;
    }

    // ---- Adapter -------------------------------------------------------------

    private void animateBounce(View view) {
        PropertyValuesHolder scaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 0.9f, 1.1f, 1.0f);
        PropertyValuesHolder scaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 0.9f, 1.1f, 1.0f);
        ObjectAnimator anim = ObjectAnimator.ofPropertyValuesHolder(view, scaleX, scaleY);
        anim.setDuration(300);
        anim.start();
    }

    private class TrickAdapter extends RecyclerView.Adapter<TrickAdapter.VH> {
        private final List<TrickItem> items;

        TrickAdapter(List<TrickItem> items) { this.items = items; }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_trick_card, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            TrickItem item = items.get(position);
            holder.tvName.setText(item.name);
            holder.itemView.setOnClickListener(v -> {
                animateBounce(holder.itemView);
                v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                v.postDelayed(() -> navigateToNextStep(item), 250);
            });
        }

        @Override
        public int getItemCount() { return items.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView tvName;

            VH(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tv_trick_name);
            }
        }
    }

    private void navigateToNextStep(TrickItem item) {
        boolean spinEligible = DifficultyEngine.isSpinEligible(item.category);
        boolean dirEligible  = DifficultyEngine.isDirectionAlwaysEligible(item.category);

        Fragment next;
        if (spinEligible || dirEligible) {
            next = SpinPickerFragment.newInstance(terrain, item.name, item.category, presetMeasurement);
        } else {
            // MANUAL and other non-spin/non-direction tricks skip the spin screen
            next = AddTrickFragment.newInstance(terrain, item.name, 0, "", presetMeasurement);
        }

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
