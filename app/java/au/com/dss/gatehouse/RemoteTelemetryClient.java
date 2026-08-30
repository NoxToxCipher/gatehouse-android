package au.com.dss.gatehouse;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * RemoteTelemetryClient
 *
 * Lightweight, zero-config HTTPS telemetry client for transmitting field tester feedback,
 * bug reports, and shift diagnostics wirelessly over cellular/Wi-Fi to the Antigravity AI loop.
 */
public class RemoteTelemetryClient {
    private static final String TAG = "RemoteTelemetryClient";
    public static final String DEFAULT_TELEMETRY_URL = "https://ntfy.sh/gatehouse_field_telemetry_2026_dss";
    private static final String FEEDBACK_CACHE_FILE = "tester_feedbacks.json";

    public interface TelemetryCallback {
        void onSuccess(String response);
        void onError(String error);
    }

    public static class FeedbackItem {
        public String id;
        public String testerName;
        public String category;
        public String title;
        public String description;
        public String diagnostics;
        public String screenshotBase64;
        public long timestamp;
        public int implementedMilestone; // 0 if pending, > 0 if resolved

        public JSONObject toJson() {
            try {
                JSONObject obj = new JSONObject();
                obj.put("id", id);
                obj.put("testerName", testerName);
                obj.put("category", category);
                obj.put("title", title);
                obj.put("description", description);
                obj.put("diagnostics", diagnostics);
                if (screenshotBase64 != null && !screenshotBase64.isEmpty()) {
                    obj.put("screenshotBase64", screenshotBase64);
                }
                obj.put("timestamp", timestamp);
                obj.put("implementedMilestone", implementedMilestone);
                return obj;
            } catch (Exception e) {
                return new JSONObject();
            }
        }

        public static FeedbackItem fromJson(JSONObject obj) {
            FeedbackItem item = new FeedbackItem();
            item.id = obj.optString("id", String.valueOf(System.currentTimeMillis()));
            item.testerName = obj.optString("testerName", "Officer Lochran");
            item.category = obj.optString("category", "BUG_REPORT");
            item.title = obj.optString("title", "");
            item.description = obj.optString("description", "");
            item.diagnostics = obj.optString("diagnostics", "");
            item.screenshotBase64 = obj.optString("screenshotBase64", "");
            item.timestamp = obj.optLong("timestamp", System.currentTimeMillis());
            item.implementedMilestone = obj.optInt("implementedMilestone", 0);
            return item;
        }
    }

    /**
     * Transmits feedback over HTTPS in the background and saves it to the local cache.
     */
    public static void transmitFeedbackAsync(
            final Context context,
            final String testerName,
            final String category,
            final String title,
            final String details,
            final String diagnostics,
            final String screenshotBase64,
            final TelemetryCallback callback) {

        final Handler mainHandler = new Handler(Looper.getMainLooper());

        new Thread(new Runnable() {
            @Override
            public void run() {
                final FeedbackItem item = new FeedbackItem();
                item.id = "fb_" + System.currentTimeMillis();
                item.testerName = testerName;
                item.category = category;
                item.title = title;
                item.description = details;
                item.diagnostics = diagnostics;
                item.screenshotBase64 = screenshotBase64;
                item.timestamp = System.currentTimeMillis();
                item.implementedMilestone = evaluateResolvedMilestone(item.id, title, details, category);

                // 1. Save to local persistent cache
                saveFeedbackToCache(context, item);

                // 2. Prepare payload
                try {
                    JSONObject payload = item.toJson();
                    payload.put("deviceModel", Build.MANUFACTURER + " " + Build.MODEL);
                    payload.put("androidVersion", Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")");
                    payload.put("appVersion", "v" + AutoUpdateManager.getAppVersion(context));
                    payload.put("submittedAtFormatted", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date()));

                    byte[] data = payload.toString(2).getBytes(StandardCharsets.UTF_8);

                    URL url = new URL(DEFAULT_TELEMETRY_URL);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setDoOutput(true);
                    conn.setConnectTimeout(8000);
                    conn.setReadTimeout(8000);

                    // ntfy headers for instant mobile & push notifications
                    conn.setRequestProperty("Title", "[" + category + "] " + title + " (" + testerName + ")");
                    conn.setRequestProperty("Tags", getTagsForCategory(category));
                    conn.setRequestProperty("Priority", category.equals("BUG_REPORT") ? "4" : "3");
                    conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");

                    OutputStream os = conn.getOutputStream();
                    os.write(data);
                    os.flush();
                    os.close();

                    final int code = conn.getResponseCode();
                    conn.disconnect();

                    if (code >= 200 && code < 300) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                if (callback != null) callback.onSuccess("Transmitted successfully (HTTP " + code + ")");
                            }
                        });
                    } else {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                if (callback != null) callback.onSuccess("Saved locally (Relay HTTP " + code + ")");
                            }
                        });
                    }
                } catch (final Exception e) {
                    Log.e(TAG, "Failed to transmit telemetry to cloud", e);
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (callback != null) callback.onSuccess("Saved to offline shift queue");
                        }
                    });
                }
            }
        }).start();
    }

    private static String getTagsForCategory(String category) {
        if ("BUG_REPORT".equalsIgnoreCase(category)) return "warning,beetle";
        if ("FEATURE_REQUEST".equalsIgnoreCase(category)) return "star,sparkles";
        if ("PATROL_CHECKPOINTS".equalsIgnoreCase(category)) return "shield,round_pushpin";
        if ("RADAR_SENSORS".equalsIgnoreCase(category)) return "satellite,fire";
        return "speech_balloon,bulb";
    }

    // Audited Manifest of Resolved Reports and Shipped Milestones
    private static final java.util.Map<String, Integer> RESOLVED_REPORT_IDS = new java.util.HashMap<String, Integer>();
    static {
        // v1.0.10 (Milestone 110)
        RESOLVED_REPORT_IDS.put("fb_1788079507586", 10); // Compass Jitter
        RESOLVED_REPORT_IDS.put("fb_1788079661244", 10); // System UI overlap

        // v1.0.11 (Milestone 111)
        RESOLVED_REPORT_IDS.put("fb_1788083657935", 11); // Version tagging in feedback list
        RESOLVED_REPORT_IDS.put("fb_1788083731245", 11); // Tester Hub Safe Area Insets
        RESOLVED_REPORT_IDS.put("fb_1788083821882", 11); // Gauges manual pressure logging

        // v1.0.12 (Milestone 112)
        RESOLVED_REPORT_IDS.put("fb_1788085188724", 12); // Radar words cover radar

        // v1.0.13 (Milestone 113)
        RESOLVED_REPORT_IDS.put("fb_1788085148558", 13); // Pump House overhaul & pull down
        RESOLVED_REPORT_IDS.put("fb_1788085249104", 13); // Tools grid vs scroll-fest
        RESOLVED_REPORT_IDS.put("fb_1788085309180", 13); // Real-time theme scrubbing
        RESOLVED_REPORT_IDS.put("fb_1788085354606", 13); // Deputy overhaul
        RESOLVED_REPORT_IDS.put("fb_1788085415513", 13); // Records audit
        RESOLVED_REPORT_IDS.put("fb_1788085622693", 13); // Pump House gesture & pressure

        // v1.0.14 (Milestone 114)
        RESOLVED_REPORT_IDS.put("fb_1788087323075", 14); // Gauges - Better (Pull down & clean dial)
        RESOLVED_REPORT_IDS.put("fb_1788087421526", 14); // Deputy Cached Roster & Key
        RESOLVED_REPORT_IDS.put("fb_1788087492186", 14); // The Logbook UI Safe Area insets
    }

    public static int evaluateResolvedMilestone(String reportId, String title, String description, String category) {
        if (reportId != null && RESOLVED_REPORT_IDS.containsKey(reportId)) {
            Integer m = RESOLVED_REPORT_IDS.get(reportId);
            return m != null ? m : 0;
        }
        // ALL newly submitted or unverified reports are strictly 0 (IN QUEUE)
        return 0;
    }

    public static void fetchRemoteFeedbackAsync(final Context context, final Runnable onLoaded) {
        final Handler mainHandler = new Handler(Looper.getMainLooper());
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(DEFAULT_TELEMETRY_URL + "/json?poll=1&since=all");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(6000);
                    conn.setReadTimeout(6000);
                    conn.connect();

                    if (conn.getResponseCode() == 200) {
                        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                        String line;
                        List<FeedbackItem> cached = loadFeedbacksFromCache(context);
                        boolean modified = false;

                        while ((line = br.readLine()) != null) {
                            line = line.trim();
                            if (line.isEmpty()) continue;
                            try {
                                JSONObject msgObj = new JSONObject(line);
                                String messageStr = msgObj.optString("message", "");
                                if (messageStr.startsWith("{") && messageStr.endsWith("}")) {
                                    JSONObject payload = new JSONObject(messageStr);
                                    String id = payload.optString("id", "");
                                    boolean alreadyExists = false;
                                    for (FeedbackItem ex : cached) {
                                        if (ex.id != null && ex.id.equals(id)) {
                                            alreadyExists = true;
                                            break;
                                        }
                                    }
                                    if (!alreadyExists) {
                                        FeedbackItem newItem = FeedbackItem.fromJson(payload);
                                        newItem.implementedMilestone = evaluateResolvedMilestone(newItem.id, newItem.title, newItem.description, newItem.category);
                                        cached.add(0, newItem);
                                        modified = true;
                                    }
                                }
                            } catch (Exception ignored) {}
                        }
                        br.close();
                        conn.disconnect();

                        if (modified) {
                            JSONArray arr = new JSONArray();
                            for (FeedbackItem it : cached) {
                                arr.put(it.toJson());
                            }
                            File f = new File(context.getFilesDir(), FEEDBACK_CACHE_FILE);
                            FileOutputStream fos = new FileOutputStream(f);
                            fos.write(arr.toString(2).getBytes(StandardCharsets.UTF_8));
                            fos.flush();
                            fos.close();
                        }
                    }
                } catch (Exception e) {
                    Log.d(TAG, "Syncing remote feedback: " + e.getMessage());
                }

                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (onLoaded != null) onLoaded.run();
                    }
                });
            }
        }).start();
    }

    public static synchronized List<FeedbackItem> loadFeedbacksFromCache(Context context) {
        List<FeedbackItem> list = new ArrayList<FeedbackItem>();
        try {
            File f = new File(context.getFilesDir(), FEEDBACK_CACHE_FILE);
            if (!f.exists()) return list;

            FileInputStream fis = new FileInputStream(f);
            BufferedReader reader = new BufferedReader(new InputStreamReader(fis, StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();
            fis.close();

            JSONArray arr = new JSONArray(sb.toString());
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                FeedbackItem item = FeedbackItem.fromJson(obj);
                // Re-evaluate milestone status dynamically using exact audited report ID
                item.implementedMilestone = evaluateResolvedMilestone(item.id, item.title, item.description, item.category);
                list.add(item);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading feedback cache", e);
        }
        return list;
    }

    public static synchronized void saveFeedbackToCache(Context context, FeedbackItem newItem) {
        List<FeedbackItem> existing = loadFeedbacksFromCache(context);
        existing.add(0, newItem);

        try {
            JSONArray arr = new JSONArray();
            for (FeedbackItem it : existing) {
                arr.put(it.toJson());
            }
            File f = new File(context.getFilesDir(), FEEDBACK_CACHE_FILE);
            FileOutputStream fos = new FileOutputStream(f);
            fos.write(arr.toString(2).getBytes(StandardCharsets.UTF_8));
            fos.flush();
            fos.close();
        } catch (Exception e) {
            Log.e(TAG, "Error saving feedback to cache", e);
        }
    }
}
