package uri.app.kickflip; // Change this to match your package name

public class SkateSpot {

    public static final int STATUS_DRY = 0;
    public static final int STATUS_DRYING = 1;
    public static final int STATUS_WET = 2;

    private String locationName;
    private double temperature;
    private String weatherCondition;
    private String groundStatus;
    private int groundStatusLevel;
    private String lastRainInfo;
    private long lastRainTimestamp;
    private boolean isRaining;
    private double humidity;

    public SkateSpot(String locationName) {
        this.locationName = locationName;
    }

    // Calculate ground status based on weather data
    public void calculateGroundStatus(boolean isRaining, long lastRainTime, double temp, double humidity) {
        this.isRaining = isRaining;
        this.lastRainTimestamp = lastRainTime;
        this.temperature = temp;
        this.humidity = humidity;

        if (isRaining) {
            this.groundStatus = "🌧️ WET - Don't skate!";
            this.groundStatusLevel = STATUS_WET;
            this.lastRainInfo = "Raining now";
            return;
        }

        long currentTime = System.currentTimeMillis();
        long hoursSinceRain = (currentTime - lastRainTime) / (1000 * 60 * 60);

        if (lastRainTime == 0) {
            // No recent rain data
            if (humidity > 80) {
                this.groundStatus = "💧 Possibly wet - check first";
                this.groundStatusLevel = STATUS_DRYING;
                this.lastRainInfo = "High humidity";
            } else {
                this.groundStatus = "✅ Probably dry - good to go!";
                this.groundStatusLevel = STATUS_DRY;
                this.lastRainInfo = "No recent rain";
            }
            return;
        }

        // Drying time logic
        // Faster drying in warm weather, slower when humid
        int dryingHours = 3; // Base drying time

        if (temperature < 10) {
            dryingHours = 6; // Slower drying in cold
        } else if (temperature > 25) {
            dryingHours = 2; // Faster drying in heat
        }

        if (humidity > 70) {
            dryingHours += 2; // Humidity slows drying
        }

        if (hoursSinceRain < 1) {
            this.groundStatus = "🌧️ WET - Just rained!";
            this.groundStatusLevel = STATUS_WET;
            this.lastRainInfo = "Rained " + hoursSinceRain * 60 + " mins ago";
        } else if (hoursSinceRain < dryingHours) {
            this.groundStatus = "⏳ DRYING - Wait a bit";
            this.groundStatusLevel = STATUS_DRYING;
            this.lastRainInfo = "Rained " + hoursSinceRain + "h ago";
        } else {
            this.groundStatus = "✅ DRY - Go skate!";
            this.groundStatusLevel = STATUS_DRY;
            this.lastRainInfo = "Rained " + hoursSinceRain + "h ago";
        }
    }

    // Getters and Setters
    public String getLocationName() {
        return locationName;
    }

    public void setLocationName(String locationName) {
        this.locationName = locationName;
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public String getWeatherCondition() {
        return weatherCondition;
    }

    public void setWeatherCondition(String weatherCondition) {
        this.weatherCondition = weatherCondition;
    }

    public String getGroundStatus() {
        return groundStatus;
    }

    public int getGroundStatusLevel() {
        return groundStatusLevel;
    }

    public String getLastRainInfo() {
        return lastRainInfo;
    }

    public boolean isRaining() {
        return isRaining;
    }

    public double getHumidity() {
        return humidity;
    }
}