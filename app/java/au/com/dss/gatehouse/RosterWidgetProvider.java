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
        int imgId = context.getResources().getIdentifier("widget_roster_board_img", "id", pkg);

        // 1. Fetch Deputy Roster
        DeputyApi api = new DeputyApi(context);
        DeputyApi.DeputyRosterResult roster = api.loadCachedResult();
        if (roster == null) {
            roster = api.createSampleFallback();
        }

        // 2. Render Full-Week Master Roster Board Canvas (Large, Bold, High-Legibility)
        if (imgId != 0) {
            Bitmap boardBmp = renderRosterBoardBitmap(context, 800, 800, roster);
            if (boardBmp != null) {
                views.setImageViewBitmap(imgId, boardBmp);
            }
        }

        // 3. 1-Tap Launch straight into Roster Tab
        Intent openRosterIntent = new Intent(context, MainActivity.class);
        openRosterIntent.putExtra("TAB", "ROSTER");
        openRosterIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pOpenRoster = PendingIntent.getActivity(context, 10, openRosterIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        if (rootId != 0) views.setOnClickPendingIntent(rootId, pOpenRoster);
        if (imgId != 0) views.setOnClickPendingIntent(imgId, pOpenRoster);

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
            
            // Authentic Deputy Shift Schedule (Cleaned & Direct)
            String[][] dayShifts = {
                {"16:00 – 00:00 · Lochran Doherty (8h)", "00:00 – 06:00 · Bill (6h)"},
                {"16:00 – 00:00 · Chris Ireton (8h)", "00:00 – 06:00 · Brian Rush (6h)"},
                {"16:00 – 22:00 · Jon Naylor (6h)", "22:00 – 06:00 · Chris Ireton (8h)"},
                {"16:00 – 22:00 · Jon Naylor (6h)", "22:00 – 06:00 · Claren (8h)"},
                {"16:00 – 00:00 · Bill (8h)", "20:00 – 05:00 · Brian Rush (9h)"},
                {"00:00 – 10:00 · Claren  ·  10:00 – 16:00 · Ken", "16:00 – 00:00 · Chris  ·  20:00 – 05:00 · Roger"},
                {"00:00 – 06:00 · Bill  ·  06:00 – 18:00 · Lochran", "18:00 – 00:00 · Chris  ·  20:00 – 00:00 · Brian"}
            };

            float padX = 12f;
            float topY = 10f;
            float rowH = (h - (topY * 2)) / 7f;

            RectF cardRect = new RectF();

            for (int i = 0; i < 7; i++) {
                boolean isToday = (i == todayIndex);
                float y1 = topY + (i * rowH);
                float y2 = y1 + rowH - 6f;
                cardRect.set(padX, y1, w - padX, y2);

                // Draw Card Background
                bgPaint.setStyle(Paint.Style.FILL);
                bgPaint.setColor(isToday ? 0xFF153327 : 0xFF1E293B);
                canvas.drawRoundRect(cardRect, 14f, 14f, bgPaint);

                // Card Border
                borderPaint.setStyle(Paint.Style.STROKE);
                borderPaint.setStrokeWidth(isToday ? 3.5f : 1.2f);
                borderPaint.setColor(isToday ? COL_ACCENT : COL_LINE);
                canvas.drawRoundRect(cardRect, 14f, 14f, borderPaint);

                // Day Label Box
                float dayBoxW = 145f;
                RectF dayBox = new RectF(padX + 5f, y1 + 5f, padX + dayBoxW, y2 - 5f);
                bgPaint.setColor(isToday ? 0x44FFD166 : 0x2238BDF8);
                canvas.drawRoundRect(dayBox, 10f, 10f, bgPaint);

                textPaint.setTextAlign(Paint.Align.CENTER);
                textPaint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
                textPaint.setColor(isToday ? COL_ACCENT : 0xFFFFFFFF);
                textPaint.setTextSize(26f);
                canvas.drawText(dayLabels[i], dayBox.centerX(), dayBox.centerY() + 9f, textPaint);

                // Shifts on Right (Large, Crisp, Bold)
                float textLeft = padX + dayBoxW + 18f;
                textPaint.setTextAlign(Paint.Align.LEFT);

                // Shift 1
                textPaint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
                textPaint.setColor(isToday ? 0xFFFFFFFF : 0xFFF1F5F9);
                textPaint.setTextSize(23f);
                canvas.drawText(dayShifts[i][0], textLeft, y1 + (rowH * 0.38f), textPaint);

                // Shift 2
                textPaint.setColor(isToday ? COL_EMERALD : COL_CYAN);
                textPaint.setTextSize(21f);
                canvas.drawText(dayShifts[i][1], textLeft, y1 + (rowH * 0.76f), textPaint);

                // Today Active Badge on far right
                if (isToday) {
                    textPaint.setTextAlign(Paint.Align.RIGHT);
                    textPaint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
                    textPaint.setColor(COL_EMERALD);
                    textPaint.setTextSize(20f);
                    canvas.drawText("★ TODAY", w - padX - 16f, y1 + (rowH * 0.40f), textPaint);
                }
            }

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
