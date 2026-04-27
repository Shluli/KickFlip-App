package uri.app.kickflip;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.os.Bundle;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
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

    private static final String ARG_TERRAIN = "terrain";

    // ---- Data model ----------------------------------------------------------

    public static class TrickItem {
        public final String name;
        public final int difficulty;
        public final String category; // FLIP / GRIND / SLIDE / OLLIE / AIR / MANUAL

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
            new TrickItem("Frontside Flip",  4, "FLIP"),
            new TrickItem("Backside Flip",   4, "FLIP"),
            new TrickItem("Varial Flip",     4, "FLIP"),
            new TrickItem("Hardflip",        4, "FLIP"),
            new TrickItem("Treflip",         5, "FLIP"),
            new TrickItem("Switch Kickflip", 5, "FLIP"),
            new TrickItem("Impossible",      4, "FLIP")
        );
    }

    private static List<TrickItem> jumpTricks() {
        return Arrays.asList(
            new TrickItem("Ollie",          2, "OLLIE"),
            new TrickItem("Pop Shuvit",     2, "OLLIE"),
            new TrickItem("Kickflip",       3, "FLIP"),
            new TrickItem("Heelflip",       3, "FLIP"),
            new TrickItem("Frontside Flip", 4, "FLIP"),
            new TrickItem("Backside Flip",  4, "FLIP"),
            new TrickItem("Varial Flip",    4, "FLIP"),
            new TrickItem("Hardflip",       4, "FLIP"),
            new TrickItem("Treflip",        5, "FLIP"),
            new TrickItem("Impossible",     4, "FLIP")
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

    private static List<TrickItem> vertTricks() {
        return Arrays.asList(
            new TrickItem("Rock to Fakie",  2, "AIR"),
            new TrickItem("Axle Stall",     2, "GRIND"),
            new TrickItem("Frontside Air",  2, "AIR"),
            new TrickItem("Backside Air",   2, "AIR"),
            new TrickItem("Indy Grab",      3, "AIR"),
            new TrickItem("Stalefish",      3, "AIR"),
            new TrickItem("Method Air",     3, "AIR"),
            new TrickItem("Smith Grind",    4, "GRIND"),
            new TrickItem("Kickflip Indy",  4, "AIR"),
            new TrickItem("540",            5, "AIR"),
            new TrickItem("McTwist",        5, "AIR")
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

        // Pool
        TRICKS_BY_TERRAIN.put("Bowl",         vertTricks());
        TRICKS_BY_TERRAIN.put("Quarter Pipe", Arrays.asList(
            new TrickItem("Rock to Fakie", 2, "AIR"),
            new TrickItem("Axle Stall",    2, "GRIND"),
            new TrickItem("Disaster",      3, "AIR"),
            new TrickItem("50-50",         3, "GRIND"),
            new TrickItem("Nosegrind",     3, "GRIND"),
            new TrickItem("Frontside Air", 3, "AIR"),
            new TrickItem("Backside Air",  3, "AIR"),
            new TrickItem("Smith Grind",   4, "GRIND"),
            new TrickItem("Indy Grab",     4, "AIR"),
            new TrickItem("Kickflip Indy", 5, "AIR")
        ));
        TRICKS_BY_TERRAIN.put("Half Pipe", Arrays.asList(
            new TrickItem("Rock to Fakie", 2, "AIR"),
            new TrickItem("Frontside Air", 2, "AIR"),
            new TrickItem("Backside Air",  2, "AIR"),
            new TrickItem("Indy Grab",     3, "AIR"),
            new TrickItem("Stalefish",     3, "AIR"),
            new TrickItem("Method Air",    3, "AIR"),
            new TrickItem("Kickflip Indy", 4, "AIR"),
            new TrickItem("540",           5, "AIR"),
            new TrickItem("McTwist",       5, "AIR")
        ));

        // Park
        TRICKS_BY_TERRAIN.put("Ramp", Arrays.asList(
            new TrickItem("Rock to Fakie", 2, "AIR"),
            new TrickItem("Axle Stall",    2, "GRIND"),
            new TrickItem("Disaster",      3, "AIR"),
            new TrickItem("Kickflip",      3, "FLIP"),
            new TrickItem("Heelflip",      3, "FLIP"),
            new TrickItem("Frontside Air", 3, "AIR"),
            new TrickItem("Indy Grab",     4, "AIR"),
            new TrickItem("Smith Grind",   4, "GRIND")
        ));
        TRICKS_BY_TERRAIN.put("Bank", Arrays.asList(
            new TrickItem("Ollie",          1, "OLLIE"),
            new TrickItem("Pop Shuvit",     2, "OLLIE"),
            new TrickItem("Kickflip",       3, "FLIP"),
            new TrickItem("Heelflip",       3, "FLIP"),
            new TrickItem("Rock to Fakie",  3, "AIR"),
            new TrickItem("Frontside Flip", 4, "FLIP"),
            new TrickItem("Smith Grind",    4, "GRIND")
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
        TRICKS_BY_TERRAIN.put("Euro Gap",   jumpTricks());
        TRICKS_BY_TERRAIN.put("Frame",      jumpTricks());
        TRICKS_BY_TERRAIN.put("Frame Gap",  jumpTricks());

        // Street — grinds / slides
        TRICKS_BY_TERRAIN.put("Ledge",        grindSlideTricks());
        TRICKS_BY_TERRAIN.put("Rail",         grindSlideTricks());
        TRICKS_BY_TERRAIN.put("Curved Rail",  basicGrindTricks());
        TRICKS_BY_TERRAIN.put("Hubba",        basicGrindTricks());
        TRICKS_BY_TERRAIN.put("Bench",        grindSlideTricks());
        TRICKS_BY_TERRAIN.put("Table",        basicGrindTricks());
        TRICKS_BY_TERRAIN.put("Curb",         basicGrindTricks());

        // Street — jumps
        TRICKS_BY_TERRAIN.put("Down Stairs",      jumpTricks());
        TRICKS_BY_TERRAIN.put("Up Stairs",         jumpTricks());
        TRICKS_BY_TERRAIN.put("Gap",               jumpTricks());
        TRICKS_BY_TERRAIN.put("Over an obsticle",  jumpTricks());

        // Street — grass (flatground-like but harder)
        TRICKS_BY_TERRAIN.put("Grass", Arrays.asList(
            new TrickItem("Ollie",          2, "OLLIE"),
            new TrickItem("Pop Shuvit",     3, "OLLIE"),
            new TrickItem("Kickflip",       4, "FLIP"),
            new TrickItem("Heelflip",       4, "FLIP"),
            new TrickItem("Frontside Flip", 5, "FLIP")
        ));

        // Street — manuals
        TRICKS_BY_TERRAIN.put("On a manual pad",  manualTricks());
        TRICKS_BY_TERRAIN.put("Up a Manual Pad",  manualTricks());
    }

    // ---- Factory -------------------------------------------------------------

    public static TrickPickerFragment newInstance(String terrain) {
        TrickPickerFragment f = new TrickPickerFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TERRAIN, terrain);
        f.setArguments(args);
        return f;
    }

    private String terrain;

    // ---- Lifecycle -----------------------------------------------------------

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        terrain = getArguments() != null ? getArguments().getString(ARG_TERRAIN, "") : "";
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_trick_picker, container, false);

        ((TextView) view.findViewById(R.id.tv_trick_terrain_label)).setText(terrain);

        List<TrickItem> tricks = TRICKS_BY_TERRAIN.get(terrain);
        if (tricks == null) tricks = new ArrayList<>();

        RecyclerView rv = view.findViewById(R.id.rv_tricks);
        rv.setLayoutManager(new GridLayoutManager(requireContext(), 3));
        rv.setAdapter(new TrickAdapter(tricks));

        return view;
    }

    // ---- Adapter -------------------------------------------------------------

    private int iconForCategory(String cat) {
        switch (cat) {
            case "FLIP":   return R.drawable.ic_trick_flip;
            case "GRIND":  return R.drawable.ic_trick_grind;
            case "SLIDE":  return R.drawable.ic_trick_slide;
            case "AIR":    return R.drawable.ic_trick_air;
            case "MANUAL": return R.drawable.ic_trick_manual;
            default:       return R.drawable.ic_trick_ollie;
        }
    }

    private String buildStars(int difficulty) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 5; i++) sb.append(i < difficulty ? "★" : "☆");
        return sb.toString();
    }

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
            holder.tvStars.setText(buildStars(item.difficulty));
            holder.ivIcon.setImageResource(iconForCategory(item.category));

            holder.itemView.setOnClickListener(v -> {
                animateBounce(holder.itemView);
                v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                v.postDelayed(() -> navigateToAddTrick(item), 250);
            });
        }

        @Override
        public int getItemCount() { return items.size(); }

        class VH extends RecyclerView.ViewHolder {
            ImageView ivIcon;
            TextView tvName, tvStars;

            VH(@NonNull View itemView) {
                super(itemView);
                ivIcon  = itemView.findViewById(R.id.iv_trick_icon);
                tvName  = itemView.findViewById(R.id.tv_trick_name);
                tvStars = itemView.findViewById(R.id.tv_trick_stars);
            }
        }
    }

    private void navigateToAddTrick(TrickItem item) {
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(
                        R.anim.slide_in_right, R.anim.slide_out_left,
                        R.anim.slide_in_left,  R.anim.slide_out_right)
                .replace(R.id.homeFragmentContainer,
                        AddTrickFragment.newInstance(terrain, item.name, item.difficulty))
                .addToBackStack(null)
                .commit();
    }
}
