package au.com.dss.gatehouse;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * TandaApi — {@link RosterProvider} for Tanda (my.tanda.co), the AU workforce
 * platform built around Fair Work and modern-award pay.
 *
 * Wired against Tanda's documented v2 API:
 *   base   https://my.tanda.co/api/v2
 *   auth   Authorization: Bearer <token>   (scopes: me, roster)
 *   GET /users/me                       -> { name, user_ids: [..] }
 *   GET /schedules?user_ids=&from=YYYY-MM-DD&to=YYYY-MM-DD&include_names=true
 *        -> [ { id, user_id, start, finish, department_name, ... } ]
 *
 * start/finish are treated as unix epoch seconds and department_name as the
 * post/operational unit. These field shapes come from Tanda's public docs and
 * data model; confirm against a live token before relying on it, since Tanda
 * has noted schedules can arrive without start/finish set. Open-shift claiming
 * and a native post-orders feed are not part of this first cut (see below).
 */
public class TandaApi implements RosterProvider {

    private static final String TAG = "TandaApi";
    private static final String PREFS_NAME = "tanda_config";
    private static final String KEY_TOKEN = "api_token";
    private static final String KEY_BASE_URL = "base_url";
    private static final String KEY_LAST_SYNC = "last_sync_timestamp";
    private static final String KEY_CACHE = "cached_shifts_json";
    private static final String DEFAULT_BASE_URL = "https://my.tanda.co/api/v2";

    private final Context context;
    private final SharedPreferences prefs;
    private final ExecutorService executor;
    private final Handler mainHandler;

    public TandaApi(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = this.context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.executor = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    @Override public String providerName() { return "Tanda"; }
    @Override public boolean isConfigured() { return hasToken(); }

    @Override public boolean hasToken() {
        String t = getToken();
        return t != null && !t.isEmpty();
    }

    @Override public String getToken() { return prefs.getString(KEY_TOKEN, ""); }
    @Override public void setToken(String token) { prefs.edit().putString(KEY_TOKEN, token == null ? "" : token.trim()).apply(); }
    @Override public String getBaseUrl() { return prefs.getString(KEY_BASE_URL, DEFAULT_BASE_URL); }
    @Override public void setBaseUrl(String url) { prefs.edit().putString(KEY_BASE_URL, (url == null || url.isEmpty()) ? DEFAULT_BASE_URL : url).apply(); }
    @Override public long getLastSyncTimestamp() { return prefs.getLong(KEY_LAST_SYNC, 0L); }

    @Override
    public void testConnection(final String testToken, final Callback<String> callback) {
        executor.execute(new Runnable() {
            @Override public void run() {
                try {
                    String token = (testToken != null && !testToken.isEmpty()) ? testToken : getToken();
                    if (token.isEmpty()) { postError(callback, "No Tanda API token provided."); return; }
                    JSONObject me = requestObject("/users/me", token);
                    String name = me.optString("name", "Tanda User");
                    postSuccess(callback, "Connected to Tanda as " + name);
                } catch (Exception e) {
                    postError(callback, "Connection failed: " + e.getMessage());
                }
            }
        });
    }

    @Override
    public void syncRoster(final Callback<Result> callback) {
        executor.execute(new Runnable() {
            @Override public void run() {
                final String token = getToken();
                if (token.isEmpty()) {
                    Result cached = loadCachedResult();
                    if (cached == null) cached = createSampleFallback();
                    cached.isLive = false;
                    cached.statusMessage = "Tanda token not configured. Using cached roster.";
                    postSuccess(callback, cached);
                    return;
                }
                try {
                    JSONObject me = requestObject("/users/me", token);
                    String meName = me.optString("name", "Officer");
                    JSONArray idsArr = me.optJSONArray("user_ids");
                    StringBuilder ids = new StringBuilder();
                    if (idsArr != null) {
                        for (int i = 0; i < idsArr.length(); i++) {
                            if (ids.length() > 0) ids.append(",");
                            ids.append(idsArr.optLong(i));
                        }
                    }
                    if (ids.length() == 0) ids.append(me.optLong("id"));

                    SimpleDateFormat day = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                    Calendar cal = Calendar.getInstance();
                    cal.add(Calendar.DAY_OF_YEAR, -2);
                    String from = day.format(cal.getTime());
                    cal.add(Calendar.DAY_OF_YEAR, 10);
                    String to = day.format(cal.getTime());

                    String path = "/schedules?user_ids=" + enc(ids.toString())
                            + "&from=" + from + "&to=" + to + "&include_names=true";
                    JSONArray schedules = requestArray(path, token);

                    Result res = buildResult(schedules, meName, true);
                    res.statusMessage = "Live from Tanda (" + res.weekShifts.size() + " shifts)";
                    prefs.edit()
                            .putString(KEY_CACHE, schedules.toString())
                            .putLong(KEY_LAST_SYNC, System.currentTimeMillis())
                            .apply();
                    postSuccess(callback, res);
                } catch (Exception e) {
                    Log.w(TAG, "Tanda sync failed: " + e.getMessage());
                    Result cached = loadCachedResult();
                    if (cached == null) cached = createSampleFallback();
                    cached.isLive = false;
                    cached.statusMessage = "Tanda offline (" + e.getMessage() + "). Using cached roster.";
                    postSuccess(callback, cached);
                }
            }
        });
    }

    @Override
    public Result loadCachedResult() {
        String json = prefs.getString(KEY_CACHE, "");
        if (json.isEmpty()) return null;
        try {
            Result res = buildResult(new JSONArray(json), "Officer", false);
            res.syncTimestamp = getLastSyncTimestamp();
            return res;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public Result createSampleFallback() {
        Result res = new Result();
        res.isLive = false;
        res.statusMessage = "Sample roster (connect Tanda to go live).";
        long nowSec = System.currentTimeMillis() / 1000L;
        Shift s = new Shift();
        s.id = 1;
        s.guardName = "Lochran Doherty";
        s.operationalUnit = "Hume Guard Hut";
        s.startTs = nowSec - 3600;
        s.endTs = nowSec + 11 * 3600;
        s.totalHours = 12;
        s.status = "ACTIVE";
        s.isCurrentGuard = true;
        s.isLiveNow = true;
        res.weekShifts.add(s);
        res.activeShift = s;
        res.onDutyGuards.add(s);
        return res;
    }

    @Override
    public void claimOpenShift(final int shiftId, final Callback<String> callback) {
        // Tanda open-shift acceptance uses a different flow (shift offers / leave
        // requests) that is not wired in this first cut. Fail honestly rather
        // than report a claim that did not happen.
        postError(callback, "Open-shift claiming isn't available on Tanda yet.");
    }

    @Override
    public void fetchDocuments(final Callback<List<Document>> callback) {
        // Tanda has no Deputy-style post-orders feed; serve the offline library
        // so the compliance reader still works.
        executor.execute(new Runnable() {
            @Override public void run() {
                postSuccess(callback, DeputyApi.getPreloadedDocuments());
            }
        });
    }

    // ---- Mapping Tanda schedules -> neutral model ----

    private Result buildResult(JSONArray schedules, String meName, boolean live) {
        Result res = new Result();
        res.isLive = live;
        res.userName = meName;
        long nowSec = System.currentTimeMillis() / 1000L;
        if (schedules != null) {
            for (int i = 0; i < schedules.length(); i++) {
                JSONObject sc = schedules.optJSONObject(i);
                if (sc == null) continue;
                long start = sc.optLong("start", 0L);
                long finish = sc.optLong("finish", 0L);
                if (start <= 0 || finish <= 0) continue; // Tanda may omit times
                Shift s = new Shift();
                s.id = sc.optInt("id", i + 1);
                s.employeeId = sc.optInt("user_id", 0);
                s.startTs = start;
                s.endTs = finish;
                s.totalHours = (finish - start) / 3600.0;
                s.operationalUnit = sc.optString("department_name", "Guard Hut");
                s.guardName = sc.optString("user_name", meName);
                s.dateString = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date(start * 1000L));
                if (nowSec >= start && nowSec <= finish) {
                    s.status = "ACTIVE";
                    s.isLiveNow = true;
                    if (res.activeShift == null) { res.activeShift = s; s.isCurrentGuard = true; }
                    res.onDutyGuards.add(s);
                } else if (start > nowSec) {
                    s.status = "SCHEDULED";
                    if (res.nextRelief == null) res.nextRelief = s;
                } else {
                    s.status = "DONE";
                }
                res.weekShifts.add(s);
            }
        }
        return res;
    }

    // ---- HTTP ----

    private JSONObject requestObject(String path, String token) throws Exception {
        return new JSONObject(requestString(path, token));
    }

    private JSONArray requestArray(String path, String token) throws Exception {
        return new JSONArray(requestString(path, token));
    }

    private String requestString(String path, String token) throws Exception {
        URL url = new URL(getBaseUrl() + path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(20000);
        conn.setRequestProperty("Authorization", "Bearer " + token);
        conn.setRequestProperty("Accept", "application/json");
        int code = conn.getResponseCode();
        InputStream is = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
        StringBuilder sb = new StringBuilder();
        if (is != null) {
            BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            br.close();
        }
        conn.disconnect();
        if (code < 200 || code >= 300) {
            throw new Exception("HTTP " + code);
        }
        return sb.toString();
    }

    private static String enc(String s) {
        try { return URLEncoder.encode(s, "UTF-8"); } catch (Exception e) { return s; }
    }

    private void postSuccess(final Callback callback, final Object result) {
        if (callback == null) return;
        mainHandler.post(new Runnable() {
            @SuppressWarnings("unchecked")
            @Override public void run() { callback.onSuccess(result); }
        });
    }

    private void postError(final Callback callback, final String msg) {
        if (callback == null) return;
        mainHandler.post(new Runnable() {
            @Override public void run() { callback.onError(msg); }
        });
    }
}
