package uri.app.kickflip;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class weatherApiService {

    private static final String API_KEY      = "8fabb613f01c4f04aae5a348d41dc017";
    private static final String BASE_URL     = "https://api.openweathermap.org/data/2.5/weather";
    private static final String FORECAST_URL = "https://api.openweathermap.org/data/2.5/forecast";

    private final ExecutorService executor;
    private final Handler mainHandler;

    public weatherApiService(Context context) {
        this.executor    = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public interface WeatherCallback {
        void onSuccess(SkateSpot spot);
        void onError(String error);
    }

    public void getWeather(String location, WeatherCallback callback) {
        executor.execute(() -> {
            try {
                // ── Current weather ───────────────────────────────────────────
                String currentUrl = BASE_URL + "?q=" + location
                        + "&appid=" + API_KEY + "&units=metric";
                JSONObject json = new JSONObject(makeHttpRequest(currentUrl));

                String locationName = json.getString("name");

                JSONObject main = json.getJSONObject("main");
                double temp     = main.getDouble("temp");
                double humidity = main.getDouble("humidity");

                JSONArray weatherArr = json.getJSONArray("weather");
                String condition    = weatherArr.getJSONObject(0).getString("main");
                String description  = weatherArr.getJSONObject(0).getString("description");

                boolean isRaining = condition.equalsIgnoreCase("Rain")
                        || condition.equalsIgnoreCase("Drizzle")
                        || condition.equalsIgnoreCase("Thunderstorm")
                        || condition.equalsIgnoreCase("Snow");

                // ── Wind (m/s) ────────────────────────────────────────────────
                double windSpeedMs = 0;
                JSONObject windObj = json.optJSONObject("wind");
                if (windObj != null) windSpeedMs = windObj.optDouble("speed", 0);

                // ── Cloud cover (%) ───────────────────────────────────────────
                int cloudCoverPct = 0;
                JSONObject cloudsObj = json.optJSONObject("clouds");
                if (cloudsObj != null) cloudCoverPct = cloudsObj.optInt("all", 0);

                // ── Recent rainfall (mm) ──────────────────────────────────────
                // rain.1h = mm in last 60 min; rain.3h = mm in last 3 h
                double rain1h = 0, rain3h = 0;
                JSONObject rainObj = json.optJSONObject("rain");
                if (rainObj != null) {
                    rain1h = rainObj.optDouble("1h", 0);
                    rain3h = rainObj.optDouble("3h", 0);
                }
                // Snow is also wet ground
                JSONObject snowObj = json.optJSONObject("snow");
                if (snowObj != null) {
                    rain1h += snowObj.optDouble("1h", 0);
                    rain3h += snowObj.optDouble("3h", 0);
                }

                // ── Daytime flag ──────────────────────────────────────────────
                boolean isDaytime = true;
                JSONObject sysObj = json.optJSONObject("sys");
                if (sysObj != null) {
                    long sunrise = sysObj.optLong("sunrise", 0) * 1000L;
                    long sunset  = sysObj.optLong("sunset",  0) * 1000L;
                    if (sunrise > 0 && sunset > 0) {
                        long nowMs = System.currentTimeMillis();
                        isDaytime = nowMs >= sunrise && nowMs <= sunset;
                    }
                }

                // ── Forecast (for historical rain fallback) ───────────────────
                String forecastUrl = FORECAST_URL + "?q=" + location
                        + "&appid=" + API_KEY + "&units=metric";
                String forecastResponse = makeHttpRequest(forecastUrl);

                // ── Derive lastRainTimestamp + totalRainMm ────────────────────
                long nowMs = System.currentTimeMillis();
                long lastRainTime;
                double totalRainMm;

                if (isRaining) {
                    // Currently raining: rain started at most 1h ago; use 1h/3h data
                    lastRainTime = nowMs;
                    totalRainMm = Math.max(rain1h, rain3h);
                    if (totalRainMm == 0) totalRainMm = 1.0; // API sometimes omits small values

                } else if (rain1h > 0) {
                    // Rained in the last hour but stopped; midpoint = ~30 min ago
                    lastRainTime = nowMs - 30L * 60 * 1000;
                    totalRainMm  = rain3h > 0 ? rain3h : rain1h;

                } else if (rain3h > 0) {
                    // Rained 1–3 h ago; midpoint = ~1.5 h ago
                    lastRainTime = nowMs - 90L * 60 * 1000;
                    totalRainMm  = rain3h;

                } else {
                    // No recent rain in current-weather data; scan forecast history
                    RainData rd  = findRainDataFromForecast(forecastResponse, nowMs);
                    lastRainTime = rd.lastRainTime;
                    totalRainMm  = rd.totalRainMm;
                }

                // ── Build SkateSpot ───────────────────────────────────────────
                SkateSpot spot = new SkateSpot(locationName);
                spot.setTemperature(temp);
                spot.setWeatherCondition(description);
                spot.calculateGroundStatus(
                        isRaining,
                        lastRainTime,
                        totalRainMm,
                        temp,
                        humidity,
                        windSpeedMs,
                        cloudCoverPct,
                        isDaytime);

                mainHandler.post(() -> callback.onSuccess(spot));

            } catch (Exception e) {
                e.printStackTrace();
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    // ── Rain history from forecast ────────────────────────────────────────────

    /** Lightweight holder returned by findRainDataFromForecast. */
    private static class RainData {
        final long   lastRainTime;
        final double totalRainMm;
        RainData(long t, double mm) { lastRainTime = t; totalRainMm = mm; }
    }

    /**
     * Scans the 5-day/3-h forecast for precipitation near or before the current
     * time.  The OWM forecast list starts at the current 3-hour block, so items
     * with dt ≤ now are genuine past/current observations; items slightly in the
     * future (within one interval) are also counted because their rain.3h window
     * overlaps the present.
     */
    private RainData findRainDataFromForecast(String forecastJson, long nowMs) {
        long   lastRainTime = 0;
        double totalRainMm  = 0;

        try {
            JSONArray list = new JSONObject(forecastJson).getJSONArray("list");

            // One 3-hour window is 10 800 000 ms; scan up to 48 h back
            long windowMs = 3L * 3600 * 1000;

            for (int i = 0; i < Math.min(list.length(), 16); i++) {
                JSONObject item = list.getJSONObject(i);
                long dt = item.getLong("dt") * 1000L;

                // Stop scanning once we're more than one window into the future
                if (dt > nowMs + windowMs) break;

                JSONArray weatherArr = item.optJSONArray("weather");
                if (weatherArr == null || weatherArr.length() == 0) continue;

                String itemMain = weatherArr.getJSONObject(0).getString("main");
                boolean hadRain = itemMain.equalsIgnoreCase("Rain")
                        || itemMain.equalsIgnoreCase("Drizzle")
                        || itemMain.equalsIgnoreCase("Thunderstorm")
                        || itemMain.equalsIgnoreCase("Snow");

                if (!hadRain) continue;

                if (dt > lastRainTime) lastRainTime = dt;

                JSONObject rain = item.optJSONObject("rain");
                if (rain != null) totalRainMm += rain.optDouble("3h", 0);

                JSONObject snow = item.optJSONObject("snow");
                if (snow != null) totalRainMm += snow.optDouble("3h", 0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return new RainData(lastRainTime, totalRainMm);
    }

    // ── HTTP helper ───────────────────────────────────────────────────────────

    private String makeHttpRequest(String urlString) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlString).openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(10_000);
        conn.setReadTimeout(10_000);

        if (conn.getResponseCode() != 200) {
            throw new Exception("API Error: " + conn.getResponseCode());
        }

        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader =
                     new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
        } finally {
            conn.disconnect();
        }
        return sb.toString();
    }

    public void shutdown() {
        if (executor != null) executor.shutdown();
    }
}
