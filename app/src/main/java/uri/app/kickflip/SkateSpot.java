package uri.app.kickflip;

public class SkateSpot {

    public static final int STATUS_DRY    = 0;
    public static final int STATUS_DRYING = 1;
    public static final int STATUS_WET    = 2;

    private String locationName;
    private double temperature;
    private String weatherCondition;
    private String groundStatus;
    private String groundSubtitle;
    private int    groundStatusLevel;
    private String lastRainInfo;
    private long   lastRainTimestamp;
    private boolean isRaining;
    private double humidity;
    private double windSpeedMs;
    private int    cloudCoverPct;
    private int    dryEtaMinutes; // -1 = raining; 0 = already dry
    private String firestoreDocId;

    public SkateSpot(String locationName) {
        this.locationName = locationName;
    }

    public void calculateGroundStatus(
            boolean isRaining,
            long    lastRainTimestamp,
            double  totalRecentRainMm,
            double  temp,
            double  humidity,
            double  windSpeedMs,
            int     cloudCoverPct,
            boolean isDaytime) {

        this.isRaining        = isRaining;
        this.lastRainTimestamp = lastRainTimestamp;
        this.temperature      = temp;
        this.humidity         = humidity;
        this.windSpeedMs      = windSpeedMs;
        this.cloudCoverPct    = cloudCoverPct;


        // Saturation vapour pressure (kPa) via Tetens formula
        double es = 0.6108 * Math.exp(17.27 * temp / (temp + 237.3));

        // Vapour pressure deficit: the atmospheric "thirst" for moisture
        double vpd = es * Math.max(0.0, 1.0 - humidity / 100.0);

        // Wind: logarithmic (removes the saturated boundary layer above the surface)
        double windFactor = 1.0 + Math.log1p(windSpeedMs * 0.5);

        // Solar radiation: pavement in direct sun heats far above air temperature
        double radFactor;
        if (isDaytime) {
            double clearSkyFraction = 1.0 - cloudCoverPct / 100.0;
            radFactor = 1.5 + 2.0 * clearSkyFraction;  // 1.5 (overcast) → 3.5 (clear)
        } else {
            radFactor = 0.8;  // night: no solar, slower but non-zero evaporation
        }

        // Final evaporation rate (mm/hr), calibrated constant K=0.30 for sealed pavement
        double evapRateMmPerHr = Math.max(0.02, 0.30 * vpd * windFactor * radFactor);

        // ── 2. WATER RETAINED ON PAVEMENT ────────────────────────────────────

        // Retention fraction decreases at higher rain volumes as drainage takes over
        double retentionRate  = Math.max(0.25, 0.55 - 0.015 * totalRecentRainMm);
        double waterRetainedMm = (totalRecentRainMm > 0)
                ? Math.min(totalRecentRainMm * retentionRate, 5.0)
                : 0.0;

        // ── 3. STATUS DETERMINATION ───────────────────────────────────────────

        if (isRaining) {
            groundStatus      = "Wet — stay off";
            groundSubtitle    = "Raining now";
            groundStatusLevel = STATUS_WET;
            lastRainInfo      = "Raining now";
            dryEtaMinutes     = -1;
            return;
        }

        if (waterRetainedMm <= 0 || lastRainTimestamp == 0) {
            if (humidity >= 95) {
                // Near-saturation air: surface may be damp from condensation
                groundStatus      = "Possibly damp";
                groundSubtitle    = "Very high humidity";
                groundStatusLevel = STATUS_DRYING;
                lastRainInfo      = "High humidity (" + (int) humidity + "%)";
                dryEtaMinutes     = 0;
            } else {
                groundStatus      = "Go skate.";
                groundSubtitle    = "Ground is dry";
                groundStatusLevel = STATUS_DRY;
                lastRainInfo      = "No recent rain";
                dryEtaMinutes     = 0;
            }
            return;
        }

        // How much water has already evaporated since rain stopped?
        double hoursElapsed    = Math.max(0.0,
                (System.currentTimeMillis() - lastRainTimestamp) / 3_600_000.0);
        double waterEvaporated = evapRateMmPerHr * hoursElapsed;
        double waterRemaining  = Math.max(0.0, waterRetainedMm - waterEvaporated);

        if (waterRemaining <= 0.05) {
            // Effectively dry (< 0.05 mm residual film)
            groundStatus      = "Go skate.";
            groundSubtitle    = "Ground is dry";
            groundStatusLevel = STATUS_DRY;
            dryEtaMinutes     = 0;

            if (hoursElapsed < 1.0) {
                lastRainInfo = "Rained " + (int)(hoursElapsed * 60) + "m ago · dry now";
            } else {
                lastRainInfo = "Rained " + (int) hoursElapsed + "h ago · dry now";
            }

        } else {
            double hoursRemaining = waterRemaining / evapRateMmPerHr;
            dryEtaMinutes = (int) Math.ceil(hoursRemaining * 60);

            // Still very wet (>70% of retained water remains) vs just drying
            if (waterRemaining > waterRetainedMm * 0.70) {
                groundStatus      = "Wet — wait";
                groundStatusLevel = STATUS_WET;
            } else {
                groundStatus      = "Drying...";
                groundStatusLevel = STATUS_DRYING;
            }

            String etaStr = dryEtaMinutes < 60
                    ? "Dries in ~" + dryEtaMinutes + " min"
                    : "Dries in ~" + (dryEtaMinutes / 60) + "h " + (dryEtaMinutes % 60) + "m";

            groundSubtitle = etaStr;
            lastRainInfo   = etaStr + " · " + String.format("%.1f", totalRecentRainMm) + "mm rain";
        }
    }

    public String  getLocationName()     { return locationName; }
    public double  getTemperature()      { return temperature; }
    public String  getWeatherCondition() { return weatherCondition; }
    public String  getGroundStatus()     { return groundStatus; }
    public String  getGroundSubtitle()   { return groundSubtitle; }
    public int     getGroundStatusLevel(){ return groundStatusLevel; }
    public String  getLastRainInfo()     { return lastRainInfo; }
    public boolean isRaining()           { return isRaining; }
    public double  getHumidity()         { return humidity; }
    public double  getWindSpeedMs()      { return windSpeedMs; }
    public int     getCloudCoverPct()    { return cloudCoverPct; }
    public int     getDryEtaMinutes()    { return dryEtaMinutes; }


    public void setLocationName(String locationName)         { this.locationName = locationName; }
    public void setTemperature(double temperature)           { this.temperature = temperature; }
    public void setWeatherCondition(String weatherCondition) { this.weatherCondition = weatherCondition; }
    public String getFirestoreDocId()                        { return firestoreDocId; }
    public void   setFirestoreDocId(String id)               { this.firestoreDocId = id; }
}
