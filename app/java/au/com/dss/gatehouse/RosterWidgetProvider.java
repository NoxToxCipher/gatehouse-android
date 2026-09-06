package au.com.dss.gatehouse;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.RemoteViews;

/**
 * The roster on the home screen, drawn by {@link RosterBoard} at the
 * widget's real size, so the board fills the widget and the rows fit the
 * height. The whole board is the button: it opens the roster.
 */
public class RosterWidgetProvider extends AppWidgetProvider {
    public static final String ACTION_SYNC_DEPUTY = "au.com.dss.gatehouse.WIDGET_ROSTER_SYNC_DEPUTY";
    public static final String ACTION_TORCH = "au.com.dss.gatehouse.WIDGET_TORCH";

    /** The frame drawable's padding, in dp, so the board is drawn at exactly the space inside it. */
    private static final int FRAME_PAD_DP = 6;

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle newOptions) {
        updateAppWidget(context, appWidgetManager, appWidgetId);
    }

    public static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        String pkg = context.getPackageName();
        int layoutId = context.getResources().getIdentifier("widget_roster_full", "layout", pkg);
        if (layoutId == 0) return;

        RemoteViews views = new RemoteViews(pkg, layoutId);
        int rootId = context.getResources().getIdentifier("widget_roster_root", "id", pkg);
        int imgId = context.getResources().getIdentifier("widget_roster_board_img", "id", pkg);

        // The cached roster, or nothing. Never a sample dressed as the real thing.
        RosterProvider.Result roster = null;
        try {
            roster = Rostering.create(context).loadCachedResult();
        } catch (Throwable ignored) {}

        if (imgId != 0) {
            int[] size = boardSize(context, appWidgetManager, appWidgetId);
            Bitmap board = RosterBoard.render(context, size[0], size[1], roster);
            if (board != null) views.setImageViewBitmap(imgId, board);
        }

        Intent open = new Intent(context, MainActivity.class);
        open.setAction(DeputyNotifier.ACTION_OPEN_DEPUTY);
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(context, 10, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        if (rootId != 0) views.setOnClickPendingIntent(rootId, pi);
        if (imgId != 0) views.setOnClickPendingIntent(imgId, pi);

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    /** The space inside the frame, in pixels, so nothing is scaled. */
    private static int[] boardSize(Context context, AppWidgetManager mgr, int appWidgetId) {
        float density = context.getResources().getDisplayMetrics().density;
        int wDp = 0, hDp = 0;
        try {
            Bundle o = mgr.getAppWidgetOptions(appWidgetId);
            if (o != null) {
                wDp = o.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0);
                hDp = o.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0);
            }
        } catch (Throwable ignored) {}
        if (wDp <= 0) wDp = 320;
        if (hDp <= 0) hDp = 300;
        int w = Math.min(1200, Math.round((wDp - 2 * FRAME_PAD_DP) * density));
        int h = Math.min(1200, Math.round((hDp - 2 * FRAME_PAD_DP) * density));
        return new int[]{Math.max(120, w), Math.max(120, h)};
    }

    @Override
    public void onReceive(final Context context, Intent intent) {
        super.onReceive(context, intent);
        String action = intent.getAction();
        if (ACTION_TORCH.equals(action)) {
            context.sendBroadcast(new Intent("au.com.dss.gatehouse.ACTION_TOGGLE_TORCH"));
        } else if (ACTION_SYNC_DEPUTY.equals(action)) {
            RosterProvider api = Rostering.create(context);
            api.syncRoster(new RosterProvider.Callback<RosterProvider.Result>() {
                @Override
                public void onSuccess(RosterProvider.Result result) {
                    AppWidgetManager mgr = AppWidgetManager.getInstance(context);
                    int[] ids = mgr.getAppWidgetIds(new ComponentName(context, RosterWidgetProvider.class));
                    for (int id : ids) updateAppWidget(context, mgr, id);
                }

                @Override
                public void onError(String errorMessage) {}
            });
        }
    }
}
