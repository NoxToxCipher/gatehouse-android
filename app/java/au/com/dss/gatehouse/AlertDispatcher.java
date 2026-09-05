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

    // The control pair. Defaults are the numbers already carried in the
    // Contacts screen; each can be overridden in prefs later without a rebuild.
    private static final String[][] DEFAULT_CONTROL = {
            {"Petrea", "0401371724"},
            {"Lochran", "0480749075"}
    };

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

    /** {name, number} for each control recipient. */
    public static List<String[]> getControlRecipients(Context ctx) {
        List<String[]> out = new ArrayList<>();
        SharedPreferences p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        for (String[] d : DEFAULT_CONTROL) {
            String num = p.getString("control_num_" + d[0].toLowerCase(Locale.US), d[1]);
            out.add(new String[]{d[0], num});
        }
        return out;
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

    private static String dispatch(Context ctx, String body, String kind) {
        if (!hasSmsPermission(ctx)) {
            Log.w(TAG, "SEND_SMS not granted; skipping " + kind + " SMS");
            record(ctx, "SMS permission not granted (" + kind + ")");
            return "SMS permission not granted";
        }
        List<String[]> rcpts = getControlRecipients(ctx);
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
