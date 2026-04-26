package uri.app.kickflip; // Change this to match your package name

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

import uri.app.kickflip.SkateSpot;

public class weatherApiService {

    // IMPORTANT: Replace this with YOUR API key from OpenWeatherMap
    private static final String API_KEY = "8fabb613f01c4f04aae5a348d41dc017";
    private static final String BASE_URL = "https://api.openweathermap.org/data/2.5/weather";
    private static final String FORECAST_URL = "https://api.openweathermap.org/data/2.5/forecast";

    private ExecutorService executor;
    private Handler mainHandler;
    private Context context;

    public weatherApiService(Context context) {
        this.context = context;
        this.executor = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public interface WeatherCallback {
        void onSuccess(SkateSpot spot);
        void onError(String error);
    }

    public void getWeather(String location, WeatherCallback callback) {
        executor.execute(() -> {
            try {
                // Get current weather
                String urlString = BASE_URL + "?q=" + location + "&appid=" + API_KEY + "&units=metric";
                String response = makeHttpRequest(urlString);

                JSONObject json = new JSONObject(response);

                // Parse current weather
                String locationName = json.getString("name");
                double temp = json.getJSONObject("main").getDouble("temp");
                double humidity = json.getJSONObject("main").getDouble("humidity");

                JSONArray weatherArray = json.getJSONArray("weather");
                String condition = weatherArray.getJSONObject(0).getString("main");
                String description = weatherArray.getJSONObject(0).getString("description");

                boolean isRaining = condition.equalsIgnoreCase("Rain") ||
                        condition.equalsIgnoreCase("Drizzle") ||
                        condition.equalsIgnoreCase("Thunderstorm");

                // Get forecast to check for recent rain
                String forecastUrlString = FORECAST_URL + "?q=" + location + "&appid=" + API_KEY + "&units=metric";
                String forecastResponse = makeHttpRequest(forecastUrlString);

                long lastRainTime = findLastRainTime(forecastResponse);

                // Create SkateSpot object
                SkateSpot spot = new SkateSpot(locationName);
                spot.setTemperature(temp);
                spot.setWeatherCondition(description);
                spot.calculateGroundStatus(isRaining, lastRainTime, temp, humidity);

                // Return on main thread
                mainHandler.post(() -> callback.onSuccess(spot));

            } catch (Exception e) {
                e.printStackTrace();
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    private String makeHttpRequest(String urlString) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(10000);

        int responseCode = connection.getResponseCode();
        if (responseCode != 200) {
            throw new Exception("API Error: " + responseCode);
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;

        while ((line = reader.readLine()) != null) {
            response.append(line);
        }

        reader.close();
        connection.disconnect();

        return response.toString();
    }

    private long findLastRainTime(String forecastJson) {
        try {
            JSONObject forecast = new JSONObject(forecastJson);
            JSONArray list = forecast.getJSONArray("list");

            long currentTime = System.currentTimeMillis();
            long lastRainTime = 0;

            // Check last 24 hours of forecast data
            for (int i = 0; i < Math.min(list.length(), 8); i++) { // 8 x 3-hour intervals = 24 hours
                JSONObject item = list.getJSONObject(i);
                long timestamp = item.getLong("dt") * 1000; // Convert to milliseconds

                if (timestamp > currentTime) {
                    continue; // Skip future forecasts
                }

                JSONArray weather = item.getJSONArray("weather");
                String main = weather.getJSONObject(0).getString("main");

                if (main.equalsIgnoreCase("Rain") ||
                        main.equalsIgnoreCase("Drizzle") ||
                        main.equalsIgnoreCase("Thunderstorm")) {

                    if (timestamp > lastRainTime) {
                        lastRainTime = timestamp;
                    }
                }
            }

            return lastRainTime;

        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    public void shutdown() {
        if (executor != null) {
            executor.shutdown();
        }
    }
}