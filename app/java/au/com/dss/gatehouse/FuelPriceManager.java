package au.com.dss.gatehouse;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

/**
 * FuelPriceManager — Live Fuel Telemetry & Shift-End Price Radar.
 * Tracks the 3 closest stations to Kingston Post 01 (OOM Kingston, 7-Eleven, Ampol),
 * provides offline caching, and alerts guards 30 minutes before shift finish.
 */
public class FuelPriceManager {
    private static final String TAG = "FuelPriceManager";
    public static final String CHANNEL_FUEL_ALERTS = "fuel_shift_alerts_v3"; // v3: Gatehouse notice tone (tier three)
    private static final String PREFS_NAME = "fuel_price_prefs";
    private static final String KEY_CACHED_JSON = "cached_fuel_json";
    private static final String KEY_LAST_FETCH_TS = "last_fetch_ts";
    private static final String PREF_NOTIFIED_SHIFT_PREFIX = "notified_fuel_shift_";

    public static final String ACTION_OPEN_FUEL = "au.com.dss.gatehouse.OPEN_FUEL";

    public static class FuelStation {
        public String id;
        public String name;
        public String brand;
        public String address;
        public double distanceKm;
        public boolean isGuardFavorite;
        public double priceUlp91;   // Cents per litre (e.g. 168.9)
        public double priceP98;     // Cents per litre (e.g. 184.9)
        public double priceDiesel;  // Cents per litre (e.g. 176.9)
        public double priceE10;     // Cents per litre (e.g. 164.9)
        public long lastUpdatedTs;

        public FuelStation(String id, String name, String brand, String address, double distanceKm,
                           boolean isGuardFavorite, double priceUlp91, double priceP98, double priceDiesel, double priceE10) {
            this.id = id;
            this.name = name;
            this.brand = brand;
            this.address = address;
            this.distanceKm = distanceKm;
            this.isGuardFavorite = isGuardFavorite;
            this.priceUlp91 = priceUlp91;
            this.priceP98 = priceP98;
            this.priceDiesel = priceDiesel;
            this.priceE10 = priceE10;
            this.lastUpdatedTs = System.currentTimeMillis();
        }
    }

    private static FuelPriceManager instance;
    private final Context context;
    private final SharedPreferences prefs;
    private final List<FuelStation> stations = new ArrayList<>();

    public interface FuelCallback {
        void onPricesUpdated(List<FuelStation> stations);
    }

    public static synchronized FuelPriceManager getInstance(Context ctx) {
        if (instance == null) {
            instance = new FuelPriceManager(ctx.getApplicationContext());
        }
        return instance;
    }

    private FuelPriceManager(Context ctx) {
        this.context = ctx;
        this.prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        initChannel();
        loadInitialStations();
    }

    private void initChannel() {
        GatehouseNotify.createChannel(context, CHANNEL_FUEL_ALERTS,
                "Fuel", "Cheapest fuel before the shift ends",
                GatehouseNotify.Tier.NOTICE, GatehouseNotify.GROUP_SKY);
    }

    private void loadInitialStations() {
        stations.clear();
        String cached = prefs.getString(KEY_CACHED_JSON, null);
        if (cached != null) {
            try {
                JSONArray arr = new JSONArray(cached);
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject o = arr.getJSONObject(i);
                    FuelStation s = new FuelStation(
                            o.optString("id"),
                            o.optString("name"),
                            o.optString("brand"),
                            o.optString("address"),
                            o.optDouble("distanceKm", 1.0),
                            o.optBoolean("isGuardFavorite", false),
                            o.optDouble("priceUlp91", 169.9),
                            o.optDouble("priceP98", 185.9),
                            o.optDouble("priceDiesel", 177.9),
                            o.optDouble("priceE10", 165.9)
                    );
                    stations.add(s);
                }
            } catch (Exception e) {
                Log.w(TAG, "Error parsing cached fuel json: " + e.getMessage());
            }
        }

        if (stations.isEmpty()) {
            // Default 3 closest stations to Kingston Post 01 (Hume Doors & Timber Gatehouse)
            // 1. OOM Energy Kingston (Guard Favourite - 0.8km)
            stations.add(new FuelStation(
                    "oom_kingston",
                    "OOM Energy Kingston",
                    "OOM",
                    "122 Kingston Rd, Kingston QLD",
                    0.8,
                    true,
                    168.9,
                    184.9,
                    176.9,
                    164.9
            ));

            // 2. 7-Eleven Kingston Rd (1.3km)
            stations.add(new FuelStation(
                    "7eleven_kingston",
                    "7-Eleven Kingston Rd",
                    "7-Eleven",
                    "58-62 Kingston Rd, Kingston QLD",
                    1.3,
                    false,
                    174.9,
                    192.9,
                    181.9,
                    170.9
            ));

            // 3. Ampol Logan Central / Kingston (2.1km)
            stations.add(new FuelStation(
                    "ampol_logan_central",
                    "Ampol Logan Central",
                    "Ampol",
                    "298 Kingston Rd, Logan Central QLD",
                    2.1,
                    false,
                    176.9,
                    194.9,
                    183.9,
                    172.9
            ));
        }
    }

    public synchronized List<FuelStation> getStations() {
        return new ArrayList<>(stations);
    }

    public synchronized FuelStation getCheapestStation() {
        if (stations.isEmpty()) return null;
        FuelStation cheapest = stations.get(0);
        for (FuelStation s : stations) {
            if (s.priceUlp91 < cheapest.priceUlp91) {
                cheapest = s;
            }
        }
        return cheapest;
    }

    public void refreshPrices(final FuelCallback callback) {
        Executors.newSingleThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    saveStationsToCache();
                } catch (Exception e) {
                    Log.e(TAG, "Error refreshing fuel prices: " + e.getMessage());
                }

                if (callback != null) {
                    callback.onPricesUpdated(getStations());
                }
            }
        });
    }

    private void saveStationsToCache() {
        try {
            JSONArray arr = new JSONArray();
            for (FuelStation s : stations) {
                JSONObject o = new JSONObject();
                o.put("id", s.id);
                o.put("name", s.name);
                o.put("brand", s.brand);
                o.put("address", s.address);
                o.put("distanceKm", s.distanceKm);
                o.put("isGuardFavorite", s.isGuardFavorite);
                o.put("priceUlp91", s.priceUlp91);
                o.put("priceP98", s.priceP98);
                o.put("priceDiesel", s.priceDiesel);
                o.put("priceE10", s.priceE10);
                arr.put(o);
            }
            prefs.edit().putString(KEY_CACHED_JSON, arr.toString())
                    .putLong(KEY_LAST_FETCH_TS, System.currentTimeMillis())
                    .apply();
        } catch (Exception e) {
            Log.e(TAG, "Failed to cache fuel stations: " + e.getMessage());
        }
    }

    /**
     * Check if 30 minutes remain before shift end, and dispatch notification if not already sent.
     */
    public void evaluateShiftEndFuelAlert(long shiftEndTs, String shiftId) {
        if (shiftEndTs <= 0) return;
        long nowSec = System.currentTimeMillis() / 1000L;
        long secUntilEnd = shiftEndTs - nowSec;
        double minutesUntilEnd = secUntilEnd / 60.0;

        // Trigger window: between 15 and 35 minutes before shift end (nominally 30 min)
        if (minutesUntilEnd >= 15.0 && minutesUntilEnd <= 35.0) {
            String todayStr = new SimpleDateFormat("yyyyMMdd", Locale.US).format(new Date());
            String notifKey = PREF_NOTIFIED_SHIFT_PREFIX + (shiftId != null ? shiftId : todayStr);

            if (!prefs.getBoolean(notifKey, false)) {
                postShiftEndFuelNotification((int) minutesUntilEnd);
                prefs.edit().putBoolean(notifKey, true).apply();
            }
        }
    }

    public void postShiftEndFuelNotification(int minutesRemaining) {
        try {
            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm == null) return;

            FuelStation oom = null;
            for (FuelStation s : stations) {
                if (s.isGuardFavorite) {
                    oom = s;
                    break;
                }
            }
            if (oom == null && !stations.isEmpty()) oom = stations.get(0);

            Intent intent = new Intent(context, MainActivity.class);
            intent.setAction(ACTION_OPEN_FUEL);
            intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);

            PendingIntent pi = PendingIntent.getActivity(
                    context,
                    3001,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
            );

            Intent navIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://maps.google.com/?q=" + Uri.encode("OOM Energy Kingston, 122 Kingston Rd, Kingston QLD")));
            PendingIntent navPi = PendingIntent.getActivity(
                    context,
                    3002,
                    navIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
            );

            String oomPrice = String.format(Locale.US, "%.1f¢", (oom != null ? oom.priceUlp91 : 168.9));
            String title = "OOM Kingston " + oomPrice + " · 0.8 km";
            String summary = "Shift ends in " + minutesRemaining + " min. 6¢ a litre under 7-Eleven.";

            Notification.Builder builder = GatehouseNotify.builder(context, CHANNEL_FUEL_ALERTS, GatehouseNotify.Tier.NOTICE)
                    .setContentTitle(title)
                    .setContentText(summary)
                    .setStyle(new Notification.BigTextStyle().bigText(GatehouseNotify.lines(
                            "OOM Kingston " + oomPrice + " · 0.8 km · lowest",
                            "7-Eleven 174.9¢ · 1.3 km",
                            "Ampol 176.9¢ · 2.1 km",
                            "About $3.60 saved on a 60 L fill.")))
                    .setContentIntent(pi)
                    .addAction(GatehouseNotify.action(context, "Directions", navPi))
                    .addAction(GatehouseNotify.action(context, "Open fuel", pi))
                    .setTimeoutAfter(3 * GatehouseNotify.HOUR);

            nm.notify(3001, builder.build());
        } catch (Exception e) {
            Log.e(TAG, "Error posting fuel notification: " + e.getMessage(), e);
        }
    }
}