package uri.app.kickflip;

import java.util.HashMap;
import java.util.Map;

 // So, this is my dif system, basiclly takes the trick u did, and what u did it on, and
 // calculates if its good or no, score out of 1-100 and ranks it in the ranks,
 //
 // Hot out the oven examples for ranks:
 //   1. Ollie  × flat(1.00)           = 15  → Bronze
 //   2. Kickflip × flat(1.00)         = 28  → Silver
 //   3. Treflip × flat(1.00)          = 46  → Gold
 //   4. Ollie × 5-stairs(1.68)        = 25  → Silver
 //   5. Kickflip × 5-stairs(1.68)     = 47  → Gold
 //   6. Kickflip × low-ledge(1.25)    = 35  → Silver (grind technique adds challenge)
 //   7. Bluntslide × high-ledge(1.55) = 65  → Diamond
 //   8. Treflip × 5-stairs(1.68)      = 77  → Epic
 //   9. Noseblunt × QP-huge(2.10)     = 92  → Legendary
 //  10. Treflip × 10-stairs(2.05)     = 94  → Legendary
 //
 //  tons of stuff to make sure nothing is overpowered, like you cant just extend your grind to 20m and become a
 //  legend, and you cant just ollie like 7 stairs and its harder than a treaflip on bank.
 //
 //  oh and for some obstacles u gotta input a measurement, like how many stairs.
 //

public class DifficultyEngine {

    // ranks constants
    public static final int RANK_BRONZE    = 0;
    public static final int RANK_SILVER    = 1;
    public static final int RANK_GOLD      = 2;
    public static final int RANK_DIAMOND   = 3;
    public static final int RANK_EPIC      = 4;
    public static final int RANK_LEGENDARY = 5;

    // Basic score assigning and coloring
    private static final int[] THRESHOLDS = {0, 21, 36, 55, 71, 86};

    public static final String[] RANK_NAMES  = {"Bronze","Silver","Gold","Diamond","Epic","Legendary"};
    public static final String[] RANK_ABBR   = {"Br","Si","Go","Di","Ep","Lg"};
    public static final int[]    RANK_COLORS = {
            0xFFCD7F32,  // Bronze
            0xFF9E9E9E,  // Silver
            0xFFFFD700,  // Gold
            0xFF29B6D4,  // Diamond  (cyan-blue just cuz dah)
            0xFF9C27B0,  // Epic     (purple)
            0xFFE53935   // Legendary(red only color that remained basiclly)
    };

    // ── Measurement type for each terrain ─────────────────────────────────────
    public enum MeasurementType { NONE, STAIRS, UP_STAIRS, GAP, HEIGHT, SIZE }

    // Only 2 options for Height
    public static final String[] HEIGHT_OPTIONS = {"Low", "High"};
    // I was generous for the QPs
    public static final String[] SIZE_OPTIONS   = {"Small (2ft)", "Medium (4ft)", "Big (6ft)", "Vert (8ft+)"};

    // Spinning amount, If you span more than 1440 contact me personally and screw off
    public static final int[] SPIN_VALUES  = {0, 180, 360, 540, 720, 900, 1080, 1440};
    public static final String[] SPIN_LABELS = {"None", "180°", "360°", "540°", "720°", "900°", "1080°", "1440°"};

    // Trick scores, from ollie 15 to 46 I think with the tre flip, organized to kinds of tricks
    private static final Map<String, Integer> TRICK_BASES = new HashMap<>();
    static {
        // Ollies / basics
        TRICK_BASES.put("Ollie",                      15);
        TRICK_BASES.put("Fakie Ollie",                13);
        TRICK_BASES.put("Nollie",                     20);
        TRICK_BASES.put("Pop Shuvit",                 18);

        // Grinds
        TRICK_BASES.put("50-50",                      18);
        TRICK_BASES.put("5-0",                        22);
        TRICK_BASES.put("5-0 Grind",                  22);
        TRICK_BASES.put("Nosegrind",                  22);
        TRICK_BASES.put("Axle Stall",                 18);
        TRICK_BASES.put("Crooked Grind",              32);
        TRICK_BASES.put("Smith Grind",                32);
        TRICK_BASES.put("Feeble Grind",               32);

        // Slides
        TRICK_BASES.put("Boardslide",                 22);
        TRICK_BASES.put("Tailslide",                  24);
        TRICK_BASES.put("Noseslide",                  24);
        TRICK_BASES.put("Lipslide",                   34);
        TRICK_BASES.put("Bluntslide",                 42);
        TRICK_BASES.put("Noseblunt",                  44);

        // Flip tricks
        TRICK_BASES.put("Kickflip",                   28);
        TRICK_BASES.put("Heelflip",                   28);
        TRICK_BASES.put("Fakie Kickflip",             28);
        TRICK_BASES.put("Varial Flip",                32);
        TRICK_BASES.put("Hardflip",                   38);
        TRICK_BASES.put("Switch Kickflip",            38);
        TRICK_BASES.put("Treflip",                    46);
        TRICK_BASES.put("Impossible",                 30);

        // Vert / park
        TRICK_BASES.put("Rock to Fakie",              18);
        TRICK_BASES.put("Disaster",                   22);
        TRICK_BASES.put("Indy Grab",                  28);
        TRICK_BASES.put("Stalefish",                  28);
        TRICK_BASES.put("Method Air",                 30);
        TRICK_BASES.put("Kickflip Indy",              38);

        // Manuals
        TRICK_BASES.put("Manual",                     12);
        TRICK_BASES.put("Nose Manual",                18);
        TRICK_BASES.put("Casper",                     26);
        TRICK_BASES.put("Manual Shove-it Out",        26);
        TRICK_BASES.put("Kickflip to Manual",         34);
        TRICK_BASES.put("Heelflip to Manual",         34);
        TRICK_BASES.put("Nose Manual Kickflip Out",   38);

        // Stalls (coping / transitions)
        TRICK_BASES.put("Nosestall",                  20);
        TRICK_BASES.put("Tailstall",                  20);
        TRICK_BASES.put("Smith Stall",                28);
        TRICK_BASES.put("Blunt Stall",                36);
        TRICK_BASES.put("Noseblunt Stall",            40);
        TRICK_BASES.put("Lien to Tail",               30);

        // Air grabs (spin-eligible)
        TRICK_BASES.put("Air",                        20);
        TRICK_BASES.put("Melon Grab",                 26);
        TRICK_BASES.put("Lien Air",                   24);
        TRICK_BASES.put("Nose Grab",                  22);
        TRICK_BASES.put("Tail Grab",                  22);
        TRICK_BASES.put("Body Jar",                   30);
        TRICK_BASES.put("Christ Air",                 40);

        // Handplants
        TRICK_BASES.put("Eggplant",                   36);
        TRICK_BASES.put("Invert",                     44);
        TRICK_BASES.put("Layback Grind",              28);
    }

    // Cant enter an amount of stairs to a rail can u
    public static MeasurementType getMeasurementType(String terrain) {
        switch (terrain) {
            case "Down Stairs":       return MeasurementType.STAIRS;
            case "Up Stairs":         return MeasurementType.UP_STAIRS;
            case "Gap":
            case "Euro Gap":          return MeasurementType.GAP;
            case "Ledge":
            case "Rail":
            case "Hubba":
            case "Curved Rail":
            case "Frame":
            case "Frame Gap":         return MeasurementType.HEIGHT;
            case "Quarter Pipe":      return MeasurementType.SIZE;
            default:                  return MeasurementType.NONE;
        }
    }

    // Gotta score for each terrain, some have measurements some dont.

    public static double getTerrainMultiplier(String terrain, int measurement) {
        switch (terrain) {
            // Simple , no options.
            case "Flatground":          return 1.00;
            case "Curb":                return 1.12;
            case "Grass":               return 1.15;
            case "Bank":                return 1.20;
            case "Ramp":                return 1.25;
            case "On a manual pad":
            case "Manual Pad":          return 1.20;
            case "Up a Manual Pad":     return 1.30;
            case "Bench":               return 1.20;
            case "Table":               return 1.20;
            case "Pyramid":             return 1.25;
            case "Over an obsticle":    return 1.25;

            // Height, either tall or short with very small if functions.
            case "Ledge":       return (measurement == 2) ? 1.55 : 1.25;
            case "Rail":        return (measurement == 2) ? 1.70 : 1.35;
            case "Hubba":       return (measurement == 2) ? 1.75 : 1.40;
            case "Curved Rail": return (measurement == 2) ? 1.65 : 1.30;
            case "Frame":       return (measurement == 2) ? 1.50 : 1.25;
            case "Frame Gap":   return (measurement == 2) ? 1.55 : 1.30;

            // The QPs need some more options
            case "Quarter Pipe": {
                double[] m = {1.28, 1.55, 1.82, 2.10};
                return m[Math.min(3, Math.max(0, measurement - 1))];
            }

            // alr this one very thought out, we want this to be logarithmic so you couldnt just spam stairs
            // Down stairs: 1.0 + 1.5×(1 − e^(−0.12n))
            // also the difference between 4 and 5 stairs is way more important than 9 to 10
            case "Down Stairs": {
                int n = Math.max(1, measurement);
                return 1.0 + 1.5 * (1.0 - Math.exp(-0.12 * n));
            }

            // Up stairs is harder so 1.7* not 1.5
            // n=5→1.75, n=10→2.14, n=15→2.36
            case "Up Stairs": {
                int n = Math.max(1, measurement);
                return 1.0 + 1.7 * (1.0 - Math.exp(-0.12 * n));
            }

            // gaps, basiclly the same thing
            case "Gap":
            case "Euro Gap": {
                int n = Math.max(1, measurement);
                return 1.0 + 1.4 * (1.0 - Math.exp(-0.10 * n));
            }

            default: return 1.15; // unknown terrain: slight bonus
        }
    }

    // Spin

    // Only if its a spinable trick like an ollie catagory or a flip or air catagory.
    public static boolean isSpinEligible(String category) {
        return "OLLIE".equals(category) || "FLIP".equals(category) || "AIR".equals(category);
    }

    // only if you need spin for these
    public static boolean isDirectionAlwaysEligible(String category) {
        return "GRIND".equals(category) || "SLIDE".equals(category)
                || "STALL".equals(category) || "HANDPLANT".equals(category);
    }

    // Suvits also need a spin, dont have energy to make shuvs a catag
    public static boolean isShuvEligible(String trickName) {
        if (trickName == null) return false;
        String lower = trickName.toLowerCase();
        return lower.contains("shuvit") || lower.contains("shuv-it")
                || lower.contains("shove-it") || lower.contains("shoveit");
    }

    /** Score multiplier for a given spin. No spin = 1.0. */
    public static double getSpinMultiplier(int degrees) {
        switch (degrees) {
            case 180:  return 1.10;
            case 360:  return 1.22;
            case 540:  return 1.38;
            case 720:  return 1.55;
            case 900:  return 1.72;
            case 1080: return 1.90;
            case 1440: return 2.10;
            default:   return 1.00;
        }
    }

    // Judges table

    // Before spin bonus
    public static int calculate(String trick, String terrain, int measurement) {
        return calculate(trick, terrain, measurement, 0);
    }

    // Spin Score
    public static int calculate(String trick, String terrain, int measurement, int spinDegrees) {
        int base = TRICK_BASES.containsKey(trick) ? TRICK_BASES.get(trick) : 20; // Casually dodging Null inputs
        double multiplier = getTerrainMultiplier(terrain, measurement);
        double spin = getSpinMultiplier(spinDegrees);
        return Math.min(100, (int) Math.round(base * multiplier * spin));
    }

    // The RANKKK
    public static int getRank(int score) { // Loop through the entire list of ranks to see which rank it fits
        for (int i = THRESHOLDS.length - 1; i >= 0; i--) {
            if (score >= THRESHOLDS[i]) return i;
        }
        return RANK_BRONZE;
    }

    // gettersss
    public static String getRankName(int score)  { return RANK_NAMES[getRank(score)]; }
    public static String getRankAbbr(int score)  { return RANK_ABBR[getRank(score)]; }
    public static int    getRankColor(int score) { return RANK_COLORS[getRank(score)]; }

    // I will ask the questions here, ur job as a user is to input and shut up
    public static String getMeasurementLabel(String terrain, int measurement) {
        if (measurement <= 0) return "";
        switch (getMeasurementType(terrain)) {
            case STAIRS:
            case UP_STAIRS:
                return measurement + (measurement == 1 ? " stair" : " stairs");
            case GAP:
                return measurement + (measurement == 1 ? " gap" : " gaps");
            case HEIGHT:
                return measurement == 2 ? "High" : "Low";
            case SIZE:
                if (measurement >= 1 && measurement <= 4) return SIZE_OPTIONS[measurement - 1];
                return "";
            default:
                return "";
        }
    }
}
