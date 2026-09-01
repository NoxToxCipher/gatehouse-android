package au.com.dss.gatehouse;

import android.app.NotificationManager;
import android.content.Context;
import android.media.session.MediaSession;
import android.os.Build;
import android.util.Log;

/**
 * GatehouseIslandManager — Cleaned up to avoid unwanted media player cards in notification shade.
 */
public class GatehouseIslandManager {
    private static final String TAG = "GatehouseIslandManager";
    public static final String ACTION_DISMISS_ISLAND = "au.com.dss.gatehouse.DISMISS_ISLAND";

    private static GatehouseIslandManager instance;
    private final Context context;

    public static synchronized GatehouseIslandManager getInstance(Context ctx) {
        if (instance == null) {
            instance = new GatehouseIslandManager(ctx.getApplicationContext());
        }
        return instance;
    }

    private GatehouseIslandManager(Context ctx) {
        this.context = ctx;
        dismissCapsule();
    }

    public void showFuelIsland(double oomPrice, double savingCents, int minsRemaining) {
        // No-op for MediaSession to prevent music player clutter in notification shade
    }

    public void dismissCapsule() {
        try {
            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) {
                nm.cancel(8801);
            }
        } catch (Exception ignored) {}
    }
}
