package uri.app.kickflip;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class ProgressionFragment extends Fragment {

    // ---- Terrain → Category mapping ----------------------------------------
    private static final Map<String, String> TERRAIN_CAT = new HashMap<>();
    static {
        TERRAIN_CAT.put("Flatground",       "Flatground");
        TERRAIN_CAT.put("Bowl",             "Pool");
        TERRAIN_CAT.put("Quarter Pipe",     "Pool");
        TERRAIN_CAT.put("Half Pipe",        "Pool");
        TERRAIN_CAT.put("Ramp",             "Park");
        TERRAIN_CAT.put("Bank",             "Park");
        TERRAIN_CAT.put("Pyramid",          "Park");
        TERRAIN_CAT.put("Euro Gap",         "Park");
        TERRAIN_CAT.put("Frame",            "Park");
        TERRAIN_CAT.put("Frame Gap",        "Park");
        TERRAIN_CAT.put("Ledge",            "Street");
        TERRAIN_CAT.put("Rail",             "Street");
        TERRAIN_CAT.put("Curved Rail",      "Street");
        TERRAIN_CAT.put("Hubba",            "Street");
        TERRAIN_CAT.put("Down Stairs",      "Street");
        TERRAIN_CAT.put("Up Stairs",        "Street");
        TERRAIN_CAT.put("Gap",              "Street");
        TERRAIN_CAT.put("Curb",             "Street");
        TERRAIN_CAT.put("Grass",            "Street");
        TERRAIN_CAT.put("On a manual pad",  "Street");
        TERRAIN_CAT.put("Up a Manual Pad",  "Street");
        TERRAIN_CAT.put("Bench",            "Street");
        TERRAIN_CAT.put("Table",            "Street");
        TERRAIN_CAT.put("Over an obsticle", "Street");
    }

    // ---- Views ---------------------------------------------------------------
    private ProgressBar pbLoading;
    private TextView tvTotal, tvThisWeek, tvThisMonth;
    private View     viewFavAccent;
    private TextView tvFavCat, tvFavDesc;
    private ProgressBar pbFlat, pbPool, pbPark, pbStreet;
    private TextView tvFlatCount, tvPoolCount, tvParkCount, tvStreetCount;
    private TextView tvMostThrown, tvHardest, tvAvgDiff;
    private TextView tvStreak, tvBestSession, tvVideos;
    private TextView[] tvRecents;

    // ---- Lifecycle -----------------------------------------------------------
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_progression, container, false);
        bindViews(v);
        loadData();
        return v;
    }

    private void bindViews(View v) {
        pbLoading     = v.findViewById(R.id.pb_prog_loading);
        tvTotal       = v.findViewById(R.id.tv_total_tricks);
        tvThisWeek    = v.findViewById(R.id.tv_this_week);
        tvThisMonth   = v.findViewById(R.id.tv_this_month);
        viewFavAccent = v.findViewById(R.id.view_fav_accent);
        tvFavCat      = v.findViewById(R.id.tv_fav_category);
        tvFavDesc     = v.findViewById(R.id.tv_fav_cat_desc);
        pbFlat        = v.findViewById(R.id.pb_flatground);
        pbPool        = v.findViewById(R.id.pb_pool);
        pbPark        = v.findViewById(R.id.pb_park);
        pbStreet      = v.findViewById(R.id.pb_street);
        tvFlatCount   = v.findViewById(R.id.tv_flat_count);
        tvPoolCount   = v.findViewById(R.id.tv_pool_count);
        tvParkCount   = v.findViewById(R.id.tv_park_count);
        tvStreetCount = v.findViewById(R.id.tv_street_count);
        tvMostThrown  = v.findViewById(R.id.tv_most_thrown);
        tvHardest     = v.findViewById(R.id.tv_hardest);
        tvAvgDiff     = v.findViewById(R.id.tv_avg_diff);
        tvStreak      = v.findViewById(R.id.tv_streak);
        tvBestSession = v.findViewById(R.id.tv_best_session);
        tvVideos      = v.findViewById(R.id.tv_video_count);
        tvRecents     = new TextView[]{
            v.findViewById(R.id.tv_recent_1),
            v.findViewById(R.id.tv_recent_2),
            v.findViewById(R.id.tv_recent_3),
            v.findViewById(R.id.tv_recent_4),
            v.findViewById(R.id.tv_recent_5)
        };
    }

    // ---- Data ---------------------------------------------------------------
    private void loadData() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        FirebaseFirestore.getInstance()
                .collection("users").document(user.getUid())
                .collection("tricks")
                .get()
                .addOnSuccessListener(snap -> {
                    List<VaultFragment.TrickEntry> list = new ArrayList<>();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        Long m = doc.getLong("measurement");
                        Long d = doc.getLong("difficulty");
                        list.add(new VaultFragment.TrickEntry(
                                doc.getId(),
                                doc.getString("name"),
                                doc.getString("trick"),
                                doc.getString("terrain"),
                                m != null ? m.intValue() : 0,
                                d != null ? d.intValue() : 1,
                                doc.getString("videoPath"),
                                doc.getString("date")
                        ));
                    }
                    if (isAdded()) pbLoading.setVisibility(View.GONE);
                    populate(list);
                });
    }

    // ---- Stats ---------------------------------------------------------------
    private void populate(List<VaultFragment.TrickEntry> tricks) {
        int total = tricks.size();
        tvTotal.setText(String.valueOf(total));

        // ---- This week / this month ----
        Calendar c = Calendar.getInstance();
        c.set(Calendar.DAY_OF_WEEK, c.getFirstDayOfWeek());
        c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0); c.set(Calendar.SECOND, 0);
        Date weekStart = c.getTime();
        c = Calendar.getInstance();
        c.set(Calendar.DAY_OF_MONTH, 1);
        c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0); c.set(Calendar.SECOND, 0);
        Date monthStart = c.getTime();

        int week = 0, month = 0;
        for (VaultFragment.TrickEntry t : tricks) {
            Date d = parseDate(t.date);
            if (d == null) continue;
            if (!d.before(weekStart))  week++;
            if (!d.before(monthStart)) month++;
        }
        tvThisWeek.setText("+" + week + " this week");
        tvThisMonth.setText("+" + month + " this month");

        // ---- Category breakdown ----
        Map<String, Integer> cats = new LinkedHashMap<>();
        cats.put("Flatground", 0); cats.put("Pool", 0);
        cats.put("Park", 0);      cats.put("Street", 0);
        for (VaultFragment.TrickEntry t : tricks) {
            String cat = t.terrain != null ? TERRAIN_CAT.getOrDefault(t.terrain, "Street") : "Street";
            cats.put(cat, cats.get(cat) + 1);
        }

        String favCat = "—"; int favCount = 0;
        for (Map.Entry<String, Integer> e : cats.entrySet()) {
            if (e.getValue() > favCount) { favCount = e.getValue(); favCat = e.getKey(); }
        }
        tvFavCat.setText(favCat.toUpperCase(Locale.getDefault()));
        viewFavAccent.setBackgroundColor(catColor(favCat));
        int pct = total > 0 ? (favCount * 100 / total) : 0;
        tvFavDesc.setText(favCount + " tricks · " + pct + "% of your sessions");

        int maxBar = Math.max(1, Collections.max(cats.values()));
        pbFlat.setMax(maxBar);   pbFlat.setProgress(cats.get("Flatground"));
        pbPool.setMax(maxBar);   pbPool.setProgress(cats.get("Pool"));
        pbPark.setMax(maxBar);   pbPark.setProgress(cats.get("Park"));
        pbStreet.setMax(maxBar); pbStreet.setProgress(cats.get("Street"));
        tvFlatCount.setText(cats.get("Flatground") + "×");
        tvPoolCount.setText(cats.get("Pool") + "×");
        tvParkCount.setText(cats.get("Park") + "×");
        tvStreetCount.setText(cats.get("Street") + "×");

        // ---- Most thrown trick ----
        Map<String, Integer> trickFreq = new HashMap<>();
        for (VaultFragment.TrickEntry t : tricks) {
            if (t.trick == null) continue;
            trickFreq.put(t.trick, trickFreq.getOrDefault(t.trick, 0) + 1);
        }
        String mostThrown = "—"; int mostCount = 0;
        for (Map.Entry<String, Integer> e : trickFreq.entrySet()) {
            if (e.getValue() > mostCount) { mostCount = e.getValue(); mostThrown = e.getKey(); }
        }
        tvMostThrown.setText(mostCount > 0 ? mostThrown + " (" + mostCount + "×)" : "—");

        // ---- Hardest trick ----
        int maxDiff = 0; String hardest = "—";
        for (VaultFragment.TrickEntry t : tricks) {
            if (t.difficulty > maxDiff) {
                maxDiff = t.difficulty;
                hardest = t.trick != null ? t.trick : t.name;
            }
        }
        tvHardest.setText(maxDiff > 0 ? hardest + "  " + stars(maxDiff) : "—");

        // ---- Average difficulty ----
        if (total > 0) {
            int sum = 0;
            for (VaultFragment.TrickEntry t : tricks) sum += t.difficulty;
            tvAvgDiff.setText(String.format(Locale.getDefault(), "%.1f / 5", (double) sum / total));
        } else {
            tvAvgDiff.setText("—");
        }

        // ---- Current streak ----
        TreeSet<String> days = new TreeSet<>(Collections.reverseOrder());
        for (VaultFragment.TrickEntry t : tricks) {
            if (t.date != null && t.date.length() >= 10) days.add(t.date.substring(0, 10));
        }
        int streak = computeStreak(new ArrayList<>(days));
        tvStreak.setText(streak + (streak == 1 ? " day" : " days"));

        // ---- Best session ----
        Map<String, Integer> dayCounts = new HashMap<>();
        for (VaultFragment.TrickEntry t : tricks) {
            if (t.date == null || t.date.length() < 10) continue;
            String day = t.date.substring(0, 10);
            dayCounts.put(day, dayCounts.getOrDefault(day, 0) + 1);
        }
        int bestCount = 0; String bestDay = "";
        for (Map.Entry<String, Integer> e : dayCounts.entrySet()) {
            if (e.getValue() > bestCount) { bestCount = e.getValue(); bestDay = e.getKey(); }
        }
        tvBestSession.setText(bestCount > 0 ? bestCount + " tricks on " + bestDay : "—");

        // ---- Videos ----
        int videos = 0;
        for (VaultFragment.TrickEntry t : tricks)
            if (t.videoPath != null && !t.videoPath.isEmpty()) videos++;
        tvVideos.setText(videos + " clips");

        // ---- Recent 5 ----
        List<VaultFragment.TrickEntry> sorted = new ArrayList<>(tricks);
        sorted.sort((a, b) ->
                (b.date != null ? b.date : "").compareTo(a.date != null ? a.date : ""));
        for (int i = 0; i < tvRecents.length; i++) {
            if (i < sorted.size()) {
                VaultFragment.TrickEntry t = sorted.get(i);
                String date = (t.date != null && t.date.length() >= 10)
                        ? t.date.substring(5, 10).replace("-", "/") : "?";
                String trick   = t.trick   != null ? t.trick   : t.name;
                String terrain = t.terrain != null ? t.terrain : "";
                tvRecents[i].setText(date + "  ·  " + trick + "  ·  " + terrain + "  " + stars(t.difficulty));
                tvRecents[i].setVisibility(View.VISIBLE);
            } else {
                tvRecents[i].setVisibility(View.GONE);
            }
        }
    }

    // ---- Helpers ------------------------------------------------------------
    private int computeStreak(List<String> daysDesc) {
        if (daysDesc.isEmpty()) return 0;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String today = sdf.format(new Date());
        Calendar yest = Calendar.getInstance();
        yest.add(Calendar.DAY_OF_YEAR, -1);
        String yesterday = sdf.format(yest.getTime());
        String first = daysDesc.get(0);
        if (!first.equals(today) && !first.equals(yesterday)) return 0;
        int streak = 1;
        for (int i = 1; i < daysDesc.size(); i++) {
            try {
                long diff = sdf.parse(daysDesc.get(i - 1)).getTime()
                          - sdf.parse(daysDesc.get(i)).getTime();
                if (diff == 86_400_000L) streak++;
                else break;
            } catch (ParseException ignored) { break; }
        }
        return streak;
    }

    private int catColor(String cat) {
        switch (cat) {
            case "Pool":   return 0xFF8B5CF6;
            case "Park":   return 0xFF10B981;
            case "Street": return 0xFFEF4444;
            default:       return 0xFF3B82F6;
        }
    }

    private String stars(int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 5; i++) sb.append(i < n ? "★" : "☆");
        return sb.toString();
    }

    private Date parseDate(String s) {
        if (s == null) return null;
        try {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(s);
        } catch (ParseException e) { return null; }
    }
}
