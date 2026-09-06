package au.com.dss.gatehouse;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.util.Log;
import android.widget.RemoteViews;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.Executors;

/**
 * DeputyNotifier — Bespoke luxury notification engine for Doherty Security Services.
 * Features custom obsidian & gold RemoteViews cards, 12h pre-shift weather briefings,
 * environmental telemetry, and real-time Deputy roster change alerts.
 */
public class DeputyNotifier {
    private static final String TAG = "DeputyNotifier";

    public static final String CHANNEL_ROSTER_CHANGES = "deputy_roster_updates_v2"; // v2: Gatehouse chime
    public static final String CHANNEL_SHIFT_WEATHER = "deputy_shift_weather_v2";   // v2: Gatehouse chime

    private static final String PREFS_NAME = "deputy_notifications";
    private static final String KEY_LAST_KNOWN_SHIFTS = "last_known_shifts_digest";
    private static final String PREF_NOTIFIED_PREFIX = "notified_12h_";

    public static final String ACTION_OPEN_DEPUTY = "au.com.dss.gatehouse.OPEN_DEPUTY";

    // Kingston, QLD Coordinates (Hume Doors & Timber Gatehouse Post 01)
    private static final double KINGSTON_LAT = -27.635;
    private static final double KINGSTON_LON = 153.116;

    public static void initChannels(Context context) {
        GatehouseNotify.createChannel(context, CHANNEL_ROSTER_CHANGES,
                "Roster changes", "Shifts added, changed or cancelled",
                GatehouseNotify.Tier.CHIME, GatehouseNotify.GROUP_ROSTER);
        GatehouseNotify.createChannel(context, CHANNEL_SHIFT_WEATHER,
                "Shift reminders", "Twelve hours before a shift, with the weather",
                GatehouseNotify.Tier.CHIME, GatehouseNotify.GROUP_ROSTER);
    }

    public static void cancelShiftNotifications(Context context) {
        if (context != null) {
            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) {
                nm.cancel(1001);
                for (int id = 2000; id <= 2999; id++) {
                    nm.cancel(id);
                }
            }
        }
    }

    public static void clearNotificationHistory(Context context) {
        if (context != null) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().clear().apply();
            cancelShiftNotifications(context);
            Log.i(TAG, "Notification history cleared");
        }
    }

    private static int id(Context context, String name, String type) {
        if (context == null || name == null) return 0;
        return context.getResources().getIdentifier(name, type, context.getPackageName());
    }

    private static void safeSetText(RemoteViews views, int viewId, String text) {
        if (views != null && viewId != 0 && text != null) {
            views.setTextViewText(viewId, text);
        }
    }

    /**
     * Compare newly fetched shifts with previous snapshot, notify on changes,
     * and check for any shifts starting in ~12 hours to deliver weather briefings.
     */
    public static void processSyncResult(final Context context, final RosterProvider.Result result) {
        if (context == null || result == null || result.weekShifts == null) return;
        // Do not spam shift notifications for offline fallback or mock cached schedules
        if (!result.isLive) return;
        initChannels(context);

        Executors.newSingleThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    checkAndNotifyRosterChanges(context, result.weekShifts);
                    check12HourShiftReminders(context, result.weekShifts);
                    checkShiftEndFuelReminders(context, result.weekShifts);
                } catch (Exception e) {
                    Log.e(TAG, "Error processing notifications: " + e.getMessage(), e);
                }
            }
        });
    }

    private static void checkShiftEndFuelReminders(Context context, List<RosterProvider.Shift> shifts) {
        if (shifts == null) return;
        FuelPriceManager fpm = FuelPriceManager.getInstance(context);
        for (RosterProvider.Shift s : shifts) {
            if (s.endTs > 0) {
                fpm.evaluateShiftEndFuelAlert(s.endTs, String.valueOf(s.id));
            }
        }
    }

    // =========================================================================
    // 1. ROSTER CHANGE DETECTION & LUXURY NOTIFICATIONS
    // =========================================================================

    private static void checkAndNotifyRosterChanges(Context context, List<RosterProvider.Shift> newShifts) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String lastDigest = prefs.getString(KEY_LAST_KNOWN_SHIFTS, null);

        Map<Integer, String> currentMap = new HashMap<>();
        StringBuilder currentDigestBuilder = new StringBuilder();

        for (RosterProvider.Shift s : newShifts) {
            String sig = s.id + ":" + s.guardName + ":" + s.startTs + ":" + s.endTs + ":" + s.operationalUnit;
            currentMap.put(s.id, sig);
            currentDigestBuilder.append(sig).append(";");
        }
        String currentDigest = currentDigestBuilder.toString();

        if (lastDigest == null) {
            // First run: save baseline digest without blasting notifications
            prefs.edit().putString(KEY_LAST_KNOWN_SHIFTS, currentDigest).apply();
            return;
        }

        if (lastDigest.equals(currentDigest)) {
            // No changes
            return;
        }

        // Parse previous map
        Map<Integer, String> oldMap = new HashMap<>();
        String[] oldEntries = lastDigest.split(";");
        for (String entry : oldEntries) {
            if (entry.trim().isEmpty()) continue;
            String[] parts = entry.split(":");
            if (parts.length >= 1) {
                try {
                    int id = Integer.parseInt(parts[0]);
                    oldMap.put(id, entry);
                } catch (Exception ignored) {}
            }
        }

        List<String> added = new ArrayList<>();
        List<String> modified = new ArrayList<>();
        List<String> removed = new ArrayList<>();

        for (RosterProvider.Shift s : newShifts) {
            if (!oldMap.containsKey(s.id)) {
                added.add("Added · " + s.getDayDisplayLabel() + " " + s.getFormattedHoursRange() + " · " + s.guardName);
            } else {
                String oldSig = oldMap.get(s.id);
                String newSig = currentMap.get(s.id);
                if (!oldSig.equals(newSig)) {
                    modified.add("Changed · " + s.getDayDisplayLabel() + " " + s.getFormattedHoursRange() + " · " + s.guardName);
                }
            }
        }

        for (Map.Entry<Integer, String> entry : oldMap.entrySet()) {
            if (!currentMap.containsKey(entry.getKey())) {
                String[] parts = entry.getValue().split(":");
                String gName = parts.length > 1 ? parts[1] : "Guard";
                removed.add("Cancelled · " + gName + " · shift " + entry.getKey());
            }
        }

        int totalChanges = added.size() + modified.size() + removed.size();
        if (totalChanges > 0) {
            StringBuilder body = new StringBuilder();
            List<String> allLines = new ArrayList<>();
            for (String a : added) { body.append(a).append("\n"); allLines.add(a); }
            for (String m : modified) { body.append(m).append("\n"); allLines.add(m); }
            for (String r : removed) { body.append(r).append("\n"); allLines.add(r); }

            postLuxuryRosterChangeNotification(context, totalChanges, body.toString().trim(), allLines);
            prefs.edit().putString(KEY_LAST_KNOWN_SHIFTS, currentDigest).apply();
        }
    }

    private static void postLuxuryRosterChangeNotification(Context context, int totalChanges, String bodyText, List<String> changeLines) {
        try {
            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm == null) return;

            Intent intent = new Intent(context, MainActivity.class);
            intent.setAction(ACTION_OPEN_DEPUTY);
            intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent pi = GatehouseNotify.open(context, 1001, intent);

            String title = totalChanges == 1 ? "Roster changed" : "Roster changed · " + totalChanges + " shifts";
            Notification.InboxStyle style = new Notification.InboxStyle();
            int shown = 0;
            for (String line : changeLines) {
                if (shown == 6) { style.setSummaryText((changeLines.size() - shown) + " more"); break; }
                style.addLine(line);
                shown++;
            }

            Notification.Builder builder = GatehouseNotify.builder(context, CHANNEL_ROSTER_CHANGES, GatehouseNotify.Tier.CHIME)
                   .setContentTitle(title)
                   .setContentText(changeLines.isEmpty() ? "Open the roster to see what moved." : changeLines.get(0))
                   .setStyle(style)
                   .setContentIntent(pi)
                   .addAction(GatehouseNotify.action(context, "Open roster", pi));

            nm.notify(1001, builder.build());
        } catch (Exception e) {
            Log.e(TAG, "Failed to post roster change notification: " + e.getMessage(), e);
        }
    }

    // =========================================================================
    // 2. 12-HOUR PRE-SHIFT WEATHER & READINESS REMINDERS
    // =========================================================================

    private static void check12HourShiftReminders(Context context, List<RosterProvider.Shift> shifts) {
        long nowSec = System.currentTimeMillis() / 1000L;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        for (RosterProvider.Shift s : shifts) {
            if (s.startTs <= 0) continue;

            long secUntilStart = s.startTs - nowSec;
            double hoursUntilStart = secUntilStart / 3600.0;

            // Trigger window: between 7.0 and 15.0 hours prior to shift (nominally ~12 hours)
            if (hoursUntilStart >= 7.0 && hoursUntilStart <= 15.0) {
                String notifiedKey = PREF_NOTIFIED_PREFIX + s.id + "_" + s.startTs;
                if (!prefs.getBoolean(notifiedKey, false)) {
                    WeatherForecast forecast = fetchKingstonForecast(s.startTs);
                    postLuxury12HourNotification(context, s, hoursUntilStart, forecast);
                    prefs.edit().putBoolean(notifiedKey, true).apply();
                }
            }
        }
    }

    public static class WeatherForecast {
        public double tempC = 14.5;
        public double minTempC = 11.8;
        public int rainProbPercent = 0;
        public double windSpeedKmh = 10.0;
        public String condition = "Clear Night";
        public String gearAdvice = "Winter duty fleece, thermal underlayer, & warm beanie advised.";
    }

    private static WeatherForecast fetchKingstonForecast(long shiftStartTs) {
        WeatherForecast wf = new WeatherForecast();
        try {
            String urlStr = String.format(Locale.US,
                    "https://api.open-meteo.com/v1/forecast?latitude=%.4f&longitude=%.4f&hourly=temperature_2m,precipitation_probability,weathercode,windspeed_10m&timezone=Australia%%2FBrisbane&forecast_days=3",
                    KINGSTON_LAT, KINGSTON_LON);

            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "Gatehouse-Duty-App/1.0");
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);

            if (conn.getResponseCode() == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                reader.close();

                JSONObject root = new JSONObject(sb.toString());
                JSONObject hourly = root.optJSONObject("hourly");
                if (hourly != null) {
                    JSONArray times = hourly.optJSONArray("time");
                    JSONArray temps = hourly.optJSONArray("temperature_2m");
                    JSONArray rains = hourly.optJSONArray("precipitation_probability");
                    JSONArray codes = hourly.optJSONArray("weathercode");
                    JSONArray winds = hourly.optJSONArray("windspeed_10m");

                    if (times != null && temps != null && times.length() > 0) {
                        SimpleDateFormat isoFmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:00", Locale.US);
                        isoFmt.setTimeZone(TimeZone.getTimeZone("Australia/Brisbane"));
                        String targetTimeStr = isoFmt.format(new Date(shiftStartTs * 1000L));

                        int bestIdx = 0;
                        for (int i = 0; i < times.length(); i++) {
                            if (times.optString(i).equals(targetTimeStr)) {
                                bestIdx = i;
                                break;
                            }
                        }

                        wf.tempC = temps.optDouble(bestIdx, 14.5);
                        wf.rainProbPercent = rains != null ? rains.optInt(bestIdx, 0) : 0;
                        wf.windSpeedKmh = winds != null ? winds.optDouble(bestIdx, 10.0) : 10.0;
                        int wCode = codes != null ? codes.optInt(bestIdx, 0) : 0;

                        // Calculate overnight minimum (next 8 hours)
                        double minT = wf.tempC;
                        for (int j = bestIdx; j < Math.min(times.length(), bestIdx + 9); j++) {
                            double t = temps.optDouble(j, minT);
                            if (t < minT) minT = t;
                        }
                        wf.minTempC = minT;

                        wf.condition = parseWeatherCode(wCode);
                        wf.gearAdvice = computeGearRecommendation(wf.tempC, wf.minTempC, wf.rainProbPercent, wf.windSpeedKmh);
                    }
                }
            }
            conn.disconnect();
        } catch (Exception e) {
            Log.w(TAG, "Failed to fetch live Open-Meteo forecast, using seasonal fallback: " + e.getMessage());
            wf.condition = "Clear";
            wf.gearAdvice = "Fleece and a thermal layer.";
        }
        return wf;
    }

    private static String parseWeatherCode(int code) {
        if (code == 0) return "Clear";
        if (code >= 1 && code <= 3) return "Partly cloudy";
        if (code == 45 || code == 48) return "Fog";
        if (code >= 51 && code <= 55) return "Light drizzle";
        if (code >= 61 && code <= 65) return "Rain";
        if (code >= 80 && code <= 82) return "Showers";
        if (code >= 95) return "Thunderstorms";
        return "Clear";
    }

    private static String computeGearRecommendation(double temp, double minTemp, int rainProb, double wind) {
        StringBuilder sb = new StringBuilder();
        if (rainProb >= 25) {
            sb.append("Rain likely (").append(rainProb).append("%): waterproof jacket and slip-resistant boots. ");
        }
        if (minTemp <= 14.0) {
            sb.append(String.format(Locale.US, "Cold night, low %.0f°C: fleece, thermal layer and beanie. ", minTemp));
        } else if (temp >= 26.0) {
            sb.append(String.format(Locale.US, "Warm, %.0f°C: summer shirt and at least two litres of water. ", temp));
        } else {
            sb.append("Mild: standard duty uniform and torch. ");
        }
        if (wind >= 25.0) {
            sb.append(String.format(Locale.US, "Gusty, %.0f km/h: check the perimeter gates.", wind));
        }
        return sb.toString().trim();
    }

    private static void postLuxury12HourNotification(Context context, RosterProvider.Shift shift, double hoursAway, WeatherForecast wf) {
        try {
            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm == null) return;

            int notifId = 2000 + (shift.id % 1000);
            String hoursStr = String.format(Locale.US, "%.0f", Math.max(1, hoursAway));

            Intent intent = new Intent(context, MainActivity.class);
            intent.setAction(ACTION_OPEN_DEPUTY);
            intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent pi = GatehouseNotify.open(context, notifId, intent);

            // The range reads "20:00 – 00:00 (4.0h)"; the title keeps the times, the duration is implied.
            String range = shift.getFormattedHoursRange();
            int paren = range.indexOf(" (");
            if (paren > 0) range = range.substring(0, paren);
            String title = "Shift in " + hoursStr + " h · " + range;
            String text = shift.guardName + " · " + shift.operationalUnit + " · " + wf.condition + ", " + String.format(Locale.US, "%.0f°C", wf.tempC);

            Notification.Builder builder = GatehouseNotify.builder(context, CHANNEL_SHIFT_WEATHER, GatehouseNotify.Tier.CHIME)
                    .setContentTitle(title)
                    .setContentText(text)
                    .setStyle(new Notification.BigTextStyle().bigText(GatehouseNotify.lines(
                            shift.getDayDisplayLabel() + " " + shift.getFormattedHoursRange() + " · " + shift.operationalUnit,
                            wf.condition + String.format(Locale.US, ", %.0f°C, low %.0f°C", wf.tempC, wf.minTempC)
                                    + " · rain " + wf.rainProbPercent + "%"
                                    + String.format(Locale.US, " · wind %.0f km/h", wf.windSpeedKmh),
                            wf.gearAdvice)))
                    .setContentIntent(pi)
                    .addAction(GatehouseNotify.action(context, "Open roster", pi));

            long untilStart = shift.startTs * 1000L - System.currentTimeMillis();
            if (untilStart > 0) builder.setTimeoutAfter(untilStart);

            nm.notify(notifId, builder.build());
            Log.i(TAG, "Posted pre-shift reminder for shift #" + shift.id);
        } catch (Exception e) {
            Log.e(TAG, "Failed to post pre-shift reminder: " + e.getMessage(), e);
        }
    }

    public static void schedulePeriodicAlarm(Context context) {
        try {
            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (am == null) return;

            Intent intent = new Intent(context, DeputyAlarmReceiver.class);
            intent.setAction("au.com.dss.gatehouse.DEPUTY_SYNC_ALARM");

            PendingIntent pi = PendingIntent.getBroadcast(
                    context,
                    8888,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
            );

            long intervalMs = 45 * 60 * 1000L; // Every 45 minutes
            long triggerAtMs = System.currentTimeMillis() + intervalMs;

            am.setInexactRepeating(AlarmManager.RTC_WAKEUP, triggerAtMs, intervalMs, pi);
            Log.i(TAG, "Scheduled periodic Deputy background sync alarm (45m interval)");
        } catch (Exception e) {
            Log.e(TAG, "Failed to schedule background alarm: " + e.getMessage(), e);
        }
    }
}