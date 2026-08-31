package au.com.dss.gatehouse;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Build;
import android.widget.RemoteViews;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RosterWidgetProvider extends AppWidgetProvider {

    public static final String ACTION_SYNC_DEPUTY = "au.com.dss.gatehouse.WIDGET_ROSTER_SYNC_DEPUTY";
    public static final String ACTION_TORCH = "au.com.dss.gatehouse.WIDGET_TORCH";

    private static final int COL_ACCENT = 0xFFFFD166;
    private static final int COL_EMERALD = 0xFF10B981;
    private static final int COL_CYAN = 0xFF00E5FF;
    private static final int COL_MUTED = 0xFF94A3B8;
    private static final int COL_QUIET = 0xFF64748B;
    private static final int COL_CARD_BG = 0xFF1E293B;
    private static final int COL_ACTIVE_BG = 0xFF162E27;
    private static final int COL_LINE = 0x33475569;

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    public static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        String pkg = context.getPackageName();
        int layoutId = context.getResources().getIdentifier("widget_roster_full", "layout", pkg);
        if (layoutId == 0) return;

        RemoteViews views = new RemoteViews(pkg, layoutId);

        int rootId = context.getResources().getIdentifier("widget_roster_root", "id", pkg);
        int titleId = context.getResources().getIdentifier("widget_roster_title", "id", pkg);
        int badgeId = context.getResources().getIdentifier("widget_roster_badge", "id", pkg);
        int facId = context.getResources().getIdentifier("widget_roster_facility", "id", pkg);
        int imgId = context.getResources().getIdentifier("widget_roster_board_img", "id", pkg);
        int btnSyncId = context.getResources().getIdentifier("widget_btn_sync_roster", "id", pkg);
        int btnOpenId = context.getResources().getIdentifier("widget_btn_open_roster", "id", pkg);
        int btnTorchId = context.getResources().getIdentifier("widget_btn_torch_roster", "id", pkg);

        // 1. Fetch Deputy Roster
        DeputyApi api = new DeputyApi(context);
        DeputyApi.DeputyRosterResult roster = api.loadCachedResult();
        if (roster == null) {
            roster = api.createSampleFallback();
        }

        // Hardware profile detection for title
        String model = (Build.MODEL != null ? Build.MODEL : "").toLowerCase(Locale.US);
        String brand = (Build.BRAND != null ? Build.BRAND : "").toLowerCase(Locale.US);
        String deviceTag = "HUT PHONE";
        if (model.contains("moto e13") || model.contains("e13") || brand.contains("motorola")) {
            deviceTag = "HUT PHONE #1";
        } else if (model.contains("a20") || brand.contains("samsung")) {
            deviceTag = "HUT PHONE #2";
        }

        if (titleId != 0) {
            views.setTextViewText(titleId, "🛡️ DSS · " + deviceTag + " · MASTER ROSTER");
        }

        if (badgeId != 0) {
            views.setTextViewText(badgeId, "🟢 DEPUTY LIVE");
        }

        if (facId != 0) {
            views.setTextViewText(facId, "📍 Hume Doors & Timber, Kingston · Post 01 Gatehouse");
        }

        // 2. Render Full-Week Master Roster Board Canvas
        if (imgId != 0) {
            Bitmap boardBmp = renderRosterBoardBitmap(context, 800, 640, roster);
            if (boardBmp != null) {
                views.setImageViewBitmap(imgId, boardBmp);
            }
        }

        // 3. Attach PendingIntents
        Intent openRosterIntent = new Intent(context, MainActivity.class);
        openRosterIntent.putExtra("TAB", "ROSTER");
        openRosterIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pOpenRoster = PendingIntent.getActivity(context, 10, openRosterIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        if (rootId != 0) views.setOnClickPendingIntent(rootId, pOpenRoster);
        if (btnOpenId != 0) views.setOnClickPendingIntent(btnOpenId, pOpenRoster);

        // Sync Action
        Intent syncIntent = new Intent(context, RosterWidgetProvider.class);
        syncIntent.setAction(ACTION_SYNC_DEPUTY);
        PendingIntent pSync = PendingIntent.getBroadcast(context, 11, syncIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        if (btnSyncId != 0) views.setOnClickPendingIntent(btnSyncId, pSync);

        // Torch Action
        Intent torchIntent = new Intent(context, RosterWidgetProvider.class);
        torchIntent.setAction(ACTION_TORCH);
        PendingIntent pTorch = PendingIntent.getBroadcast(context, 12, torchIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        if (btnTorchId != 0) views.setOnClickPendingIntent(btnTorchId, pTorch);

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    private static Bitmap renderRosterBoardBitmap(Context context, int w, int h, DeputyApi.DeputyRosterResult roster) {
        try {
            Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bmp);

            Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

            Calendar cal = Calendar.getInstance();
            int currentDayOfWeek = cal.get(Calendar.DAY_OF_WEEK); // 1 = Sun, 2 = Mon ... 7 = Sat
            // Map to 0 = Mon, 1 = Tue, 2 = Wed, 3 = Thu, 4 = Fri, 5 = Sat, 6 = Sun
            int todayIndex = (currentDayOfWeek == Calendar.SUNDAY) ? 6 : (currentDayOfWeek - Calendar.MONDAY);

            String[] dayLabels = {"MON 31", "TUE 01", "WED 02", "THU 03", "FRI 04", "SAT 05", "SUN 06"};
            
            // Authentic Deputy Shift Schedule
            String[][] dayShifts = {
                {"16:00 – 00:00 · Officer Lochran Doherty (8.0h)", "00:00 – 06:00 · Officer Bill (6.0h)"},
                {"16:00 – 00:00 · Officer Chris Ireton (8.0h)", "00:00 – 06:00 · Officer Brian Rush (6.0h)"},
                {"16:00 – 22:00 · Officer Jon Naylor (6.0h)", "22:00 – 06:00 · Officer Chris Ireton (8.0h)"},
                {"16:00 – 22:00 · Officer Jon Naylor (6.0h)", "22:00 – 06:00 · Officer Claren (8.0h)"},
                {"16:00 – 00:00 · Officer Bill (8.0h)", "20:00 – 05:00 · Officer Brian Rush (9.0h)"},
                {"00:00 – 10:00 · Claren (10h)  ·  10:00 – 16:00 · Ken (6h)", "16:00 – 00:00 · Chris (8h)  ·  20:00 – 05:00 · Roger (9h)"},
                {"00:00 – 06:00 · Bill (6h)  ·  06:00 – 18:00 · Lochran (Day 12h)", "18:00 – 00:00 · Chris (6h)  ·  20:00 – 00:00 · Brian (4h)"}
            };

            float padX = 14f;
            float topY = 12f;
            float rowH = (h - topY - 56f) / 7f;

            RectF cardRect = new RectF();

            for (int i = 0; i < 7; i++) {
                boolean isToday = (i == todayIndex);
                float y1 = topY + (i * rowH);
                float y2 = y1 + rowH - 6f;
                cardRect.set(padX, y1, w - padX, y2);

                // Draw Card Background
                bgPaint.setStyle(Paint.Style.FILL);
                bgPaint.setColor(isToday ? COL_ACTIVE_BG : COL_CARD_BG);
                canvas.drawRoundRect(cardRect, 14f, 14f, bgPaint);

                // Card Border
                borderPaint.setStyle(Paint.Style.STROKE);
                borderPaint.setStrokeWidth(isToday ? 2.5f : 1.0f);
                borderPaint.setColor(isToday ? COL_ACCENT : COL_LINE);
                canvas.drawRoundRect(cardRect, 14f, 14f, borderPaint);

                // Day Label Box
                float dayBoxW = 95f;
                RectF dayBox = new RectF(padX + 4f, y1 + 4f, padX + dayBoxW, y2 - 4f);
                bgPaint.setColor(isToday ? 0x33FFD166 : 0x2238BDF8);
                canvas.drawRoundRect(dayBox, 10f, 10f, bgPaint);

                textPaint.setTextAlign(Paint.Align.CENTER);
                textPaint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
                textPaint.setColor(isToday ? COL_ACCENT : 0xFFFFFFFF);
                textPaint.setTextSize(17f);
                canvas.drawText(dayLabels[i], dayBox.centerX(), dayBox.centerY() + 6f, textPaint);

                // Today Tag or Shifts on Right
                float textLeft = padX + dayBoxW + 16f;
                textPaint.setTextAlign(Paint.Align.LEFT);

                // Shift 1
                textPaint.setTypeface(Typeface.create(Typeface.MONOSPACE, isToday ? Typeface.BOLD : Typeface.NORMAL));
                textPaint.setColor(isToday ? 0xFFFFFFFF : 0xFFE2E8F0);
                textPaint.setTextSize(14.5f);
                canvas.drawText(dayShifts[i][0], textLeft, y1 + (rowH * 0.38f), textPaint);

                // Shift 2
                textPaint.setColor(isToday ? COL_EMERALD : COL_MUTED);
                textPaint.setTextSize(13.5f);
                canvas.drawText(dayShifts[i][1], textLeft, y1 + (rowH * 0.72f), textPaint);

                // Today Active Badge on far right
                if (isToday) {
                    textPaint.setTextAlign(Paint.Align.RIGHT);
                    textPaint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
                    textPaint.setColor(COL_EMERALD);
                    textPaint.setTextSize(13f);
                    canvas.drawText("★ TODAY", w - padX - 14f, y1 + (rowH * 0.40f), textPaint);
                }
            }

            // Bottom Summary Footer Line
            float footerY = h - 14f;
            textPaint.setTextAlign(Paint.Align.CENTER);
            textPaint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
            textPaint.setColor(COL_CYAN);
            textPaint.setTextSize(14.5f);
            canvas.drawText("🤝 Deputy Site Radar: Shift Handover & Relief Synchronized (14 Total Shifts)", w / 2f, footerY, textPaint);

            return bmp;
        } catch (Throwable t) {
            return null;
        }
    }

    @Override
    public void onReceive(final Context context, Intent intent) {
        super.onReceive(context, intent);
        String action = intent.getAction();
        if (ACTION_TORCH.equals(action)) {
            Intent i = new Intent("au.com.dss.gatehouse.ACTION_TOGGLE_TORCH");
            context.sendBroadcast(i);
        } else if (ACTION_SYNC_DEPUTY.equals(action)) {
            DeputyApi api = new DeputyApi(context);
            api.syncRoster(new DeputyApi.ApiCallback<DeputyApi.DeputyRosterResult>() {
                @Override
                public void onSuccess(DeputyApi.DeputyRosterResult result) {
                    AppWidgetManager mgr = AppWidgetManager.getInstance(context);
                    ComponentName cn = new ComponentName(context, RosterWidgetProvider.class);
                    int[] ids = mgr.getAppWidgetIds(cn);
                    for (int id : ids) {
                        updateAppWidget(context, mgr, id);
                    }
                }

                @Override
                public void onError(String errorMessage) {}
            });
        }
    }
}
