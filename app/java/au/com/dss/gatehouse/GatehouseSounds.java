package au.com.dss.gatehouse;

import android.app.NotificationChannel;
import android.content.Context;
import android.media.AudioAttributes;
import android.net.Uri;

/**
 * GatehouseSounds — the app's own notification tones, so every guard's phone
 * says the same thing and none of them shout.
 *
 *   chime  a soft two-note bell (E5 then B5), mastered at -14 dBFS, ~1 s.
 *          Roster changes, reminders, updates, radar and sky alerts.
 *   alert  a rising three-note bell (E5, G#5, B5), mastered at -9 dBFS, ~1.25 s.
 *          Fire, lightning and hail: firmer, still round, never a klaxon.
 *
 * The tones are synthesised sine bells with a warm second partial (res/raw),
 * chosen because a guard walking the yard alone at 3 am should not be audible
 * from fifty metres. A channel's sound is fixed the moment it is created, so
 * any channel that adopts these needs a fresh id.
 */
public final class GatehouseSounds {

    private GatehouseSounds() {}

    public static Uri chime(Context ctx) {
        return Uri.parse("android.resource://" + ctx.getPackageName() + "/raw/gatehouse_chime");
    }

    public static Uri alert(Context ctx) {
        return Uri.parse("android.resource://" + ctx.getPackageName() + "/raw/gatehouse_alert");
    }

    public static AudioAttributes attrs() {
        return new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
    }

    /** Ordinary notifications: the soft chime. */
    public static void applyChime(NotificationChannel chan, Context ctx) {
        chan.setSound(chime(ctx), attrs());
    }

    /** Safety notifications: the firmer three-note bell. */
    public static void applyAlert(NotificationChannel chan, Context ctx) {
        chan.setSound(alert(ctx), attrs());
    }

    // The package is fixed by the manifest, so callers without a Context in
    // scope can still adopt the tones.
    private static final String PKG = "au.com.dss.gatehouse";

    public static void applyChime(NotificationChannel chan) {
        chan.setSound(Uri.parse("android.resource://" + PKG + "/raw/gatehouse_chime"), attrs());
    }

    public static void applyAlert(NotificationChannel chan) {
        chan.setSound(Uri.parse("android.resource://" + PKG + "/raw/gatehouse_alert"), attrs());
    }
}
