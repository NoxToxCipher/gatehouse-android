package au.com.dss.gatehouse;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * ManualRoster — a {@link RosterProvider} with no back-end at all, so Gatehouse
 * runs for a firm that keeps no rostering system (or a guard offline).
 *
 * Shifts live in local storage. They can be entered by the app or imported from
 * a CSV of the form:
 *
 *   date,start,finish,guard,unit
 *   2026-09-05,18:00,06:00,Lochran Doherty,Hume Guard Hut
 *
 * There is nothing to sync and nothing to leak. The compliance/post-orders
 * reader still works because {@link #fetchDocuments} serves the offline library.
 */
public class ManualRoster implements RosterProvider {

    private static final String PREFS_NAME = "manual_roster";
    private static final String KEY_SHIFTS = "shifts_json";
    private static final String KEY_OFFICER = "officer_name";

    private final SharedPreferences prefs;
    private final Handler mainHandler;

    public ManualRoster(Context context) {
        this.prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    @Override public String providerName() { return "Manual"; }
    @Override public boolean isConfigured() { return true; } // no credentials needed
    @Override public boolean hasToken() { return false; }
    @Override public String getToken() { return ""; }
    @Override public void setToken(String token) { /* no-op */ }
    @Override public String getBaseUrl() { return ""; }
    @Override public void setBaseUrl(String url) { /* no-op */ }
    @Override public long getLastSyncTimestamp() { return prefs.getLong("last_edit", 0L); }

    @Override
    public void testConnection(String testToken, Callback<String> callback) {
        if (callback != null) mainHandler.post(() -> callback.onSuccess("Manual mode active (no server)."));
    }

    @Override
    public void syncRoster(final Callback<Result> callback) {
        Result res = loadCachedResult();
        if (res == null) res = createSampleFallback();
        res.isLive = false;
        res.statusMessage = "Manual roster (" + res.weekShifts.size() + " shifts, no server).";
        final Result out = res;
        if (callback != null) mainHandler.post(() -> callback.onSuccess(out));
    }

    @Override
    public Result loadCachedResult() {
        String json = prefs.getString(KEY_SHIFTS, "");
        if (json.isEmpty()) return null;
        try {
            JSONArray arr = new JSONArray(json);
            if (arr.length() == 0) return null;
            Result res = new Result();
            res.userName = prefs.getString(KEY_OFFICER, "Lochran Doherty");
            long nowSec = System.currentTimeMillis() / 1000L;
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o == null) continue;
                Shift s = new Shift();
                s.id = o.optInt("id", i + 1);
                s.guardName = o.optString("guard", res.userName);
                s.operationalUnit = o.optString("unit", "Guard Hut");
                s.startTs = o.optLong("start", 0L);
                s.endTs = o.optLong("finish", 0L);
                s.dateString = o.optString("date", "");
                s.status = o.optString("status", "CONFIRMED");
                s.totalHours = (s.endTs > s.startTs) ? (s.endTs - s.startTs) / 3600.0 : 0;
                if (s.startTs > 0 && nowSec >= s.startTs && nowSec <= s.endTs) {
                    s.isLiveNow = true;
                    s.status = "ACTIVE";
                    if (res.activeShift == null) { res.activeShift = s; s.isCurrentGuard = true; }
                    res.onDutyGuards.add(s);
                } else if (s.startTs > nowSec && res.nextRelief == null) {
                    res.nextRelief = s;
                }
                res.weekShifts.add(s);
            }
            res.syncTimestamp = getLastSyncTimestamp();
            return res;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public Result createSampleFallback() {
        Result res = new Result();
        res.userName = prefs.getString(KEY_OFFICER, "Lochran Doherty");
        res.statusMessage = "No shifts entered yet. Add one or import a CSV.";
        long nowSec = System.currentTimeMillis() / 1000L;
        Shift s = new Shift();
        s.id = 1;
        s.guardName = res.userName;
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
    public void claimOpenShift(int shiftId, Callback<String> callback) {
        if (callback != null) mainHandler.post(() -> callback.onSuccess("Shift marked (manual roster)."));
    }

    @Override
    public void fetchDocuments(final Callback<List<Document>> callback) {
        if (callback != null) mainHandler.post(() -> callback.onSuccess(DeputyApi.getPreloadedDocuments()));
    }

    // ---- Manual entry / CSV import (not part of the interface) ----

    /**
     * Replace the stored roster from a CSV: date,start,finish,guard,unit.
     * date is YYYY-MM-DD; start/finish are HH:mm (finish before start rolls to
     * the next day, e.g. an 18:00 to 06:00 night shift). Returns shifts imported.
     */
    public int importCsv(String csv) {
        JSONArray arr = new JSONArray();
        if (csv != null) {
            String[] lines = csv.split("\\r?\\n");
            SimpleDateFormat dt = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
            int id = 1;
            for (String line : lines) {
                String t = line.trim();
                if (t.isEmpty()) continue;
                if (t.toLowerCase(Locale.US).startsWith("date,")) continue; // header
                String[] c = t.split(",");
                if (c.length < 3) continue;
                try {
                    String date = c[0].trim();
                    long start = dt.parse(date + " " + c[1].trim()).getTime() / 1000L;
                    long finish = dt.parse(date + " " + c[2].trim()).getTime() / 1000L;
                    if (finish <= start) finish += 24 * 3600L; // overnight
                    JSONObject o = new JSONObject();
                    o.put("id", id++);
                    o.put("date", date);
                    o.put("start", start);
                    o.put("finish", finish);
                    o.put("guard", c.length > 3 ? c[3].trim() : "");
                    o.put("unit", c.length > 4 ? c[4].trim() : "Guard Hut");
                    o.put("status", "CONFIRMED");
                    arr.put(o);
                } catch (Exception ignored) {}
            }
        }
        prefs.edit().putString(KEY_SHIFTS, arr.toString())
                .putLong("last_edit", System.currentTimeMillis()).apply();
        return arr.length();
    }

    public void setOfficerName(String name) {
        prefs.edit().putString(KEY_OFFICER, name == null ? "" : name).apply();
    }

    public void clear() {
        prefs.edit().remove(KEY_SHIFTS).apply();
    }
}
