package au.com.dss.gatehouse;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.telephony.SmsManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * AlertDispatcher — one place that turns a safety event into an SMS to the
 * control pair (and, later, to guards on shift).
 *
 * The actual sending sits behind {@link SmsTransport}. Today the only transport
 * is {@link #ON_DEVICE}, which uses the phone's own SIM: it works on the cell
 * network with no data, no account and no per-message cost, and it goes out
 * from the guard's own handset (which is what a welfare deadman needs, since
 * the phone raising the alarm may be the only one still on signal). When a
 * central-number cloud path is wanted for a sold version, implement the same
 * interface and call {@link #setTransport} once at startup — no call site changes.
 *
 * On-device SMS is best-effort: it needs cell signal, and it is not a
 * substitute for a monitored man-down service where a life depends on it.
 */
public final class AlertDispatcher {

    private static final String TAG = "AlertDispatcher";
    private static final String PREFS = "gatehouse_alerts";
    private static final String SMS_SENT_ACTION = "au.com.dss.gatehouse.SMS_SENT";

    /**
     * Numbers and names come from the site book on this phone ({@link SiteBook}),
     * never from the source and never from a handset's own contacts. Each number
     * can still be overridden in prefs ("num_" + key) without a rebuild.
     */
    /** The control pair: every alert goes to them. */
    private static String[] controlKeys(Context ctx) {
        SiteBook b = SiteBook.get(ctx);
        return b.control.isEmpty() ? new String[]{"petrea", "lochran"} : b.control.toArray(new String[0]);
    }
    /** Told when a finder says the fallen guard is not all right. */
    private static String[] emergencyKeys(Context ctx) {
        SiteBook b = SiteBook.get(ctx);
        return b.emergency.isEmpty() ? new String[]{"petrea", "claren", "lochran"} : b.emergency.toArray(new String[0]);
    }

    private static boolean receiverRegistered = false;

    /** A way to put a message on the wire. Swap for a cloud gateway later. */
    public interface SmsTransport {
        /** @return true if handed to the platform without throwing. */
        boolean send(Context ctx, String number, String message);
    }

    /** On-device SIM transport: no data, no cost, sends from this handset. */
    public static final SmsTransport ON_DEVICE = new SmsTransport() {
        @Override
        public boolean send(Context ctx, String number, String message) {
            try {
                SmsManager sms = getSmsManager(ctx);
                ArrayList<String> parts = sms.divideMessage(message);
                ArrayList<PendingIntent> sent = new ArrayList<>();
                for (int i = 0; i < parts.size(); i++) {
                    Intent it = new Intent(SMS_SENT_ACTION).putExtra("to", number);
                    int flags = PendingIntent.FLAG_UPDATE_CURRENT
                            | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0);
                    sent.add(PendingIntent.getBroadcast(ctx, (number + "#" + i).hashCode(), it, flags));
                }
                sms.sendMultipartTextMessage(number, null, parts, sent, null);
                return true;
            } catch (Throwable t) {
                Log.e(TAG, "On-device SMS send failed to " + number, t);
                return false;
            }
        }
    };

    private static SmsTransport transport = ON_DEVICE;

    private AlertDispatcher() {}

    /** Call once from MainActivity.onCreate so send/failed results get logged. */
    public static void init(Context ctx) {
        registerSentReceiver(ctx.getApplicationContext());
    }

    /** Point every alert at a different transport (e.g. a cloud gateway). */
    public static void setTransport(SmsTransport t) {
        if (t != null) transport = t;
    }

    public static boolean hasSmsPermission(Context ctx) {
        return ctx.checkSelfPermission(android.Manifest.permission.SEND_SMS)
                == android.content.pm.PackageManager.PERMISSION_GRANTED;
    }

    /** The number on file for a phone-book key, with the prefs override honoured. */
    public static String numberFor(Context ctx, String key) {
        if (key == null) return "";
        SharedPreferences p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String legacy = p.getString("control_num_" + key, null);
        String override = p.getString("num_" + key, legacy);
        if (override != null && !override.trim().isEmpty()) return override.trim();
        return SiteBook.get(ctx).numberFor(key);
    }

    public static String displayName(Context ctx, String key) {
        return SiteBook.get(ctx).displayName(key);
    }

    /** The book key for a roster name, or null; the matching lives in {@link SiteBook#guardForName}. */
    public static String keyForGuard(Context ctx, String guardName) {
        SiteBook.Guard g = SiteBook.get(ctx).guardForName(guardName);
        return g != null ? g.key : null;
    }

    /** {name, number} for each control recipient. */
    public static List<String[]> getControlRecipients(Context ctx) {
        List<String[]> out = new ArrayList<>();
        for (String k : controlKeys(ctx)) addUnique(out, displayName(ctx, k), numberFor(ctx, k));
        return out;
    }

    /**
     * Everyone a site alert goes to: the control pair, and when the roster has a
     * second guard on site, that guard's own phone and the other hut phone. The
     * sending phone never texts itself.
     */
    public static List<String[]> siteRecipients(Context ctx) {
        List<String[]> out = getControlRecipients(ctx);
        try {
            String other = FallDetection.otherGuardOnSite(ctx);
            if (other != null && !other.isEmpty()) {
                String k = keyForGuard(ctx, other);
                if (k != null) addUnique(out, displayName(ctx, k), numberFor(ctx, k));
                String tag = MainActivity.getHutPhoneHardwareTag();
                if (tag.endsWith("#1")) addUnique(out, displayName(ctx, "hut2"), numberFor(ctx, "hut2"));
                else if (tag.endsWith("#2")) addUnique(out, displayName(ctx, "hut1"), numberFor(ctx, "hut1"));
            }
        } catch (Throwable ignored) {}
        return out;
    }

    /** Petrea, Claren and Lochran: for a finder who says the fallen guard is not all right. */
    public static List<String[]> emergencyRecipients(Context ctx) {
        List<String[]> out = new ArrayList<>();
        for (String k : emergencyKeys(ctx)) addUnique(out, displayName(ctx, k), numberFor(ctx, k));
        return out;
    }

    private static void addUnique(List<String[]> list, String name, String number) {
        if (number == null || number.trim().isEmpty()) return;
        for (String[] r : list) if (r[1].equals(number)) return;
        list.add(new String[]{name, number});
    }

    /** Lightning stand-down alert to the control pair. Returns a status line. */
    public static String sendStandDown(Context ctx, String reason, String extraLine) {
        String body = "DSS GATEHOUSE ALERT\n"
                + "Hume Doors & Timber, Kingston\n"
                + "LIGHTNING STAND-DOWN\n"
                + reason
                + (extraLine != null && !extraLine.isEmpty() ? "\n" + extraLine : "")
                + "\nGuards: shelter in Guard Hut, cease yard rounds.";
        return dispatch(ctx, body, "stand-down");
    }

    /** Lone-worker welfare escalation alert to the control pair. */
    public static String sendWelfareBreach(Context ctx, String officer, String locationLink) {
        String body = "DSS GATEHOUSE WELFARE ALERT\n"
                + "Hume Doors & Timber, Kingston\n"
                + "Officer " + officer + " did NOT confirm the lone-worker welfare check.\n"
                + "No site activity 90 min, then unconfirmed for 5 min."
                + (locationLink != null && !locationLink.isEmpty() ? "\nLast known: " + locationLink : "")
                + "\nAttempt contact / dispatch a check.";
        return dispatch(ctx, body, "welfare");
    }

    /**
     * Fall alert to the control pair. The phone felt a hard impact, then no
     * movement, and nobody answered the check-in. Written to be read at 2 a.m.
     * on a lock screen: what, who, when, where, then the one thing to do.
     * Plain GSM characters only, so it arrives as one message on any handset.
     */
    public static String sendFallAlert(Context ctx, String officer, String phoneTag, String impactClock,
                                       int checkInSecs, String locationLink, String locationNote,
                                       String alsoOnSite, boolean isTest) {
        StringBuilder b = new StringBuilder();
        b.append(isTest ? "TEST - DSS GATEHOUSE FALL ALERT\n" : "DSS GATEHOUSE FALL ALERT\n");
        b.append("Hume Doors & Timber, Kingston\n");
        b.append("Officer ").append(officer).append(" may have fallen. ")
         .append(phoneTag).append(" felt a hard impact at ").append(impactClock)
         .append(", then no movement, and the ").append(checkInSecs).append("s check-in went unanswered.\n");
        if (locationLink != null && !locationLink.isEmpty()) {
            b.append("Map: ").append(locationLink);
            if (locationNote != null && !locationNote.isEmpty()) b.append(" (").append(locationNote).append(")");
            b.append("\n");
        } else {
            b.append("No location fix on the phone.\n");
        }
        // alsoOnSite: a name, "" when the roster says the officer is alone, null when the roster is unknown.
        if (alsoOnSite != null && !alsoOnSite.isEmpty()) {
            b.append("Ring this number first, then ").append(alsoOnSite)
             .append(" on the other hut phone. No answer: treat as a medical emergency.");
        } else if (alsoOnSite != null) {
            b.append("Ring this number first. No answer: treat as a medical emergency. Nobody else is rostered on site.");
        } else {
            b.append("Ring this number first. No answer: treat as a medical emergency.");
        }
        if (isTest) b.append("\nThis is a test. Nobody has fallen.");
        return dispatch(ctx, b.toString(), isTest ? "fall-test" : "fall");
    }

    /** Sent when the officer answers after the fall alert has already gone out. */
    public static String sendFallAllClear(Context ctx, String officer, String impactClock, boolean isTest) {
        String body = (isTest ? "TEST - " : "") + "DSS GATEHOUSE: Officer " + officer
                + " has answered the phone after the fall alert at " + impactClock + ". Stand down."
                + (isTest ? "\nThis is a test." : "");
        return dispatch(ctx, body, isTest ? "fall-clear-test" : "fall-clear");
    }

    private static String dispatch(Context ctx, String body, String kind) {
        if (!hasSmsPermission(ctx)) {
            Log.w(TAG, "SEND_SMS not granted; skipping " + kind + " SMS");
            record(ctx, "SMS permission not granted (" + kind + ")");
            return "SMS permission not granted";
        }
        List<String[]> rcpts = siteRecipients(ctx);
        int ok = 0;
        StringBuilder names = new StringBuilder();
        for (String[] r : rcpts) {
            if (transport.send(ctx, r[1], body)) {
                ok++;
                if (names.length() > 0) names.append(", ");
                names.append(r[0]);
            }
        }
        String status = "SMS " + kind + ": sent to " + ok + "/" + rcpts.size()
                + (names.length() > 0 ? " (" + names + ")" : "");
        record(ctx, status);
        Log.i(TAG, status);
        return status;
    }

    private static void record(Context ctx, String status) {
        try {
            ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                    .putLong("last_alert_ts", System.currentTimeMillis())
                    .putString("last_alert", status)
                    .apply();
        } catch (Throwable ignored) {}
    }

    private static SmsManager getSmsManager(Context ctx) {
        if (Build.VERSION.SDK_INT >= 31) {
            SmsManager sm = ctx.getSystemService(SmsManager.class);
            if (sm != null) return sm;
        }
        return SmsManager.getDefault();
    }

    private static void registerSentReceiver(final Context app) {
        if (receiverRegistered) return;
        BroadcastReceiver r = new BroadcastReceiver() {
            @Override
            public void onReceive(Context c, Intent i) {
                String to = i.getStringExtra("to");
                boolean ok = getResultCode() == Activity.RESULT_OK;
                try {
                    app.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                            .putLong("last_sms_result_ts", System.currentTimeMillis())
                            .putString("last_sms_result", (ok ? "SENT " : "FAILED ") + to)
                            .apply();
                } catch (Throwable ignored) {}
                Log.i(TAG, "SMS to " + to + " -> " + (ok ? "SENT" : "FAILED code=" + getResultCode()));
            }
        };
        IntentFilter f = new IntentFilter(SMS_SENT_ACTION);
        try {
            if (Build.VERSION.SDK_INT >= 33) {
                app.registerReceiver(r, f, Context.RECEIVER_NOT_EXPORTED);
            } else {
                app.registerReceiver(r, f);
            }
            receiverRegistered = true;
        } catch (Throwable t) {
            Log.e(TAG, "Could not register SMS sent-status receiver", t);
        }
    }
}
