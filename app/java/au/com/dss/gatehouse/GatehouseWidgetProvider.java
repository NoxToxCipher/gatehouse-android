package au.com.dss.gatehouse;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;
import java.util.Calendar;

public class GatehouseWidgetProvider extends AppWidgetProvider {

    public static final String ACTION_TORCH = "au.com.dss.gatehouse.WIDGET_TORCH";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    public static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        String pkg = context.getPackageName();
        int layoutId = context.getResources().getIdentifier("widget_gatehouse", "layout", pkg);
        if (layoutId == 0) return;

        RemoteViews views = new RemoteViews(pkg, layoutId);

        int rootId = context.getResources().getIdentifier("widget_root", "id", pkg);
        int badgeId = context.getResources().getIdentifier("widget_badge", "id", pkg);
        int barId = context.getResources().getIdentifier("widget_progress_bar", "id", pkg);
        int labelId = context.getResources().getIdentifier("widget_progress_label", "id", pkg);
        int btnTorchId = context.getResources().getIdentifier("widget_btn_torch", "id", pkg);
        int btnIncidentId = context.getResources().getIdentifier("widget_btn_incident", "id", pkg);
        int btnNoteId = context.getResources().getIdentifier("widget_btn_note", "id", pkg);

        // Calculate Shift Progress Percentage (18:00 - 06:00 standard 12-hour shift)
        Calendar now = Calendar.getInstance();
        int hour = now.get(Calendar.HOUR_OF_DAY);
        int min = now.get(Calendar.MINUTE);
        int curMins = hour * 60 + min;

        boolean onShift = (curMins >= 18 * 60 || curMins < 6 * 60);

        if (onShift) {
            int elapsedMins = 0;
            if (curMins >= 18 * 60) {
                elapsedMins = curMins - 18 * 60;
            } else {
                elapsedMins = (24 * 60 - 18 * 60) + curMins;
            }
            int totalShiftMins = 12 * 60; // 720 minutes
            int pct = Math.min(100, Math.max(0, (elapsedMins * 100) / totalShiftMins));
            int remainMins = Math.max(0, totalShiftMins - elapsedMins);

            if (barId != 0) views.setProgressBar(barId, 100, pct, false);
            if (badgeId != 0) views.setTextViewText(badgeId, "ON SHIFT (" + pct + "%)");
            if (labelId != 0) {
                String elStr = (elapsedMins / 60) + "h " + (elapsedMins % 60) + "m elapsed";
                String remStr = (remainMins / 60) + "h " + (remainMins % 60) + "m left (06:00)";
                views.setTextViewText(labelId, "⏱️ " + elStr + " · " + remStr);
            }
        } else {
            // Off Shift: Next shift at 18:00
            int untilMins = (18 * 60) - curMins;
            int pct = 0;
            if (barId != 0) views.setProgressBar(barId, 100, pct, false);
            if (badgeId != 0) views.setTextViewText(badgeId, "NEXT: 18:00");
            if (labelId != 0) {
                String untilStr = (untilMins / 60) + "h " + (untilMins % 60) + "m";
                views.setTextViewText(labelId, "📅 Next Shift: Tonight 18:00 (Starts in " + untilStr + ")");
            }
        }

        // Open App Intent
        Intent openIntent = new Intent(context, MainActivity.class);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pOpen = PendingIntent.getActivity(context, 0, openIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        if (rootId != 0) views.setOnClickPendingIntent(rootId, pOpen);

        // Torch Action
        Intent torchIntent = new Intent(context, GatehouseWidgetProvider.class);
        torchIntent.setAction(ACTION_TORCH);
        PendingIntent pTorch = PendingIntent.getBroadcast(context, 1, torchIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        if (btnTorchId != 0) views.setOnClickPendingIntent(btnTorchId, pTorch);

        // Incident Action
        Intent incidentIntent = new Intent(context, MainActivity.class);
        incidentIntent.putExtra("ACTION", "INCIDENT");
        incidentIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pIncident = PendingIntent.getActivity(context, 2, incidentIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        if (btnIncidentId != 0) views.setOnClickPendingIntent(btnIncidentId, pIncident);

        // Note Action
        Intent noteIntent = new Intent(context, MainActivity.class);
        noteIntent.putExtra("ACTION", "NOTE");
        noteIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pNote = PendingIntent.getActivity(context, 3, noteIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        if (btnNoteId != 0) views.setOnClickPendingIntent(btnNoteId, pNote);

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        String action = intent.getAction();
        if (ACTION_TORCH.equals(action)) {
            Intent i = new Intent("au.com.dss.gatehouse.ACTION_TOGGLE_TORCH");
            context.sendBroadcast(i);
        }
    }
}
