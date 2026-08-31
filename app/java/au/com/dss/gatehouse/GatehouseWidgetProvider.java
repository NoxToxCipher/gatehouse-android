package au.com.dss.gatehouse;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Build;
import android.widget.RemoteViews;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class GatehouseWidgetProvider extends AppWidgetProvider {

    public static final String ACTION_TORCH = "au.com.dss.gatehouse.WIDGET_TORCH";
    public static final String ACTION_SYNC_DEPUTY = "au.com.dss.gatehouse.WIDGET_SYNC_DEPUTY";

    private static final int COL_ACCENT = 0xFFFFD166;
    private static final int COL_EMERALD = 0xFF10B981;
    private static final int COL_CYAN = 0xFF00E5FF;
    private static final int COL_LINE = 0x33475569;
    private static final int COL_QUIET = 0xFF64748B;
    private static final int COL_MUTED = 0xFF94A3B8;

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
        int titleId = context.getResources().getIdentifier("widget_title", "id", pkg);
        int badgeId = context.getResources().getIdentifier("widget_badge", "id", pkg);
        int facId = context.getResources().getIdentifier("widget_facility", "id", pkg);
        int imgId = context.getResources().getIdentifier("widget_chronograph_img", "id", pkg);
        int radarId = context.getResources().getIdentifier("widget_relief_radar", "id", pkg);
        int btnTorchId = context.getResources().getIdentifier("widget_btn_torch", "id", pkg);
        int btnLogId = context.getResources().getIdentifier("widget_btn_log", "id", pkg);
        int btnSyncId = context.getResources().getIdentifier("widget_btn_sync", "id", pkg);

        // 1. Resolve Deputy Roster Telemetry
        DeputyApi api = new DeputyApi(context);
        DeputyApi.DeputyRosterResult roster = api.loadCachedResult();
        if (roster == null) {
            roster = api.createSampleFallback();
        }

        long nowSec = System.currentTimeMillis() / 1000L;
        DeputyApi.DeputyShift activeShift = null;
        DeputyApi.DeputyShift nextShift = null;

        if (roster != null && roster.weekShifts != null) {
            for (DeputyApi.DeputyShift s : roster.weekShifts) {
                if (nowSec >= s.startTs && nowSec <= s.endTs) {
                    activeShift = s;
                    break;
                } else if (nowSec < s.startTs && (nextShift == null || s.startTs < nextShift.startTs)) {
                    nextShift = s;
                }
            }
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
            views.setTextViewText(titleId, "🛡️ DSS · " + deviceTag);
        }

        float shiftProgress = 0f;
        String startLabel = "16:00";
        String endLabel = "00:00";
        String guardName = "Officer Lochran Doherty";
        String shiftWindow = "16:00–00:00";
        String elRemStr = "0h elapsed · 8h left";
        int totalHours = 8;
        String reliefGuard = (roster != null && roster.nextRelief != null) ? roster.nextRelief.guardName : "William NEWMAN";

        SimpleDateFormat sdfHour = new SimpleDateFormat("HH:mm", Locale.US);
        SimpleDateFormat sdfTime = new SimpleDateFormat("HH:mm:ss", Locale.US);
        sdfTime.setTimeZone(TimeZone.getDefault());
        String curTimeStr = sdfTime.format(new Date(nowSec * 1000L));

        if (activeShift != null) {
            guardName = activeShift.guardName.isEmpty() ? "Officer Lochran Doherty" : activeShift.guardName;
            startLabel = sdfHour.format(new Date(activeShift.startTs * 1000L));
            endLabel = sdfHour.format(new Date(activeShift.endTs * 1000L));
            shiftWindow = startLabel + "–" + endLabel;
            totalHours = Math.max(1, (int) Math.round(activeShift.totalHours));

            long elapsedSec = Math.max(0, nowSec - activeShift.startTs);
            long totalSec = Math.max(1, activeShift.endTs - activeShift.startTs);
            shiftProgress = Math.min(1f, Math.max(0f, (float) elapsedSec / (float) totalSec));

            long remainSec = Math.max(0, activeShift.endTs - nowSec);
            long elHours = elapsedSec / 3600;
            long elMins = (elapsedSec % 3600) / 60;
            long remHours = remainSec / 3600;
            long remMins = (remainSec % 3600) / 60;
            elRemStr = elHours + "h " + elMins + "m elapsed · " + remHours + "h " + remMins + "m left";

            int pct = (int) (shiftProgress * 100);
            if (badgeId != 0) {
                views.setTextViewText(badgeId, "🟢 ON SHIFT (" + pct + "%)");
            }
        } else if (nextShift != null) {
            guardName = nextShift.guardName;
            startLabel = sdfHour.format(new Date(nextShift.startTs * 1000L));
            endLabel = sdfHour.format(new Date(nextShift.endTs * 1000L));
            shiftWindow = startLabel + "–" + endLabel;
            long untilSec = Math.max(0, nextShift.startTs - nowSec);
            long uHours = untilSec / 3600;
            long uMins = (untilSec % 3600) / 60;
            elRemStr = "Starts in " + uHours + "h " + uMins + "m (" + startLabel + ")";
            if (badgeId != 0) {
                views.setTextViewText(badgeId, "NEXT: " + startLabel);
            }
        } else {
            if (badgeId != 0) {
                views.setTextViewText(badgeId, "OFF SHIFT");
            }
        }

        if (facId != 0) {
            views.setTextViewText(facId, "👤 " + guardName + " · " + shiftWindow + " · Post 01");
        }

        if (radarId != 0) {
            views.setTextViewText(radarId, "🤝 Next Relief: " + reliefGuard + " (" + endLabel + ") · 🚰 1,200 PSI ✓");
        }

        // 2. Render Canvas Chronograph Dial Bitmap
        if (imgId != 0) {
            Bitmap dialBitmap = renderChronographBitmap(
                    context, 700, 340, shiftProgress, curTimeStr, startLabel, endLabel, totalHours, elRemStr, activeShift != null);
            if (dialBitmap != null) {
                views.setImageViewBitmap(imgId, dialBitmap);
            }
        }

        // 3. Attach PendingIntents
        Intent openIntent = new Intent(context, MainActivity.class);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pOpen = PendingIntent.getActivity(context, 0, openIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        if (rootId != 0) views.setOnClickPendingIntent(rootId, pOpen);
        if (btnLogId != 0) views.setOnClickPendingIntent(btnLogId, pOpen);

        // Torch Action
        Intent torchIntent = new Intent(context, GatehouseWidgetProvider.class);
        torchIntent.setAction(ACTION_TORCH);
        PendingIntent pTorch = PendingIntent.getBroadcast(context, 1, torchIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        if (btnTorchId != 0) views.setOnClickPendingIntent(btnTorchId, pTorch);

        // Deputy Sync Action
        Intent syncIntent = new Intent(context, GatehouseWidgetProvider.class);
        syncIntent.setAction(ACTION_SYNC_DEPUTY);
        PendingIntent pSync = PendingIntent.getBroadcast(context, 2, syncIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        if (btnSyncId != 0) views.setOnClickPendingIntent(btnSyncId, pSync);

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    /**
     * High-fidelity vector-quality 2D Canvas rendering of the Shift Chronograph Dial for RemoteViews.
     */
    private static Bitmap renderChronographBitmap(
            Context context, int w, int h, float shiftProgress, String timeStr,
            String startLabel, String endLabel, int totalHours, String subtext, boolean onShift) {
        try {
            Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bmp);

            float cx = w / 2f;
            float cy = 160f;
            float rOuter = 135f;
            float rInner = 114f;

            RectF outerRect = new RectF(cx - rOuter, cy - rOuter, cx + rOuter, cy + rOuter);
            RectF innerRect = new RectF(cx - rInner, cy - rInner, cx + rInner, cy + rInner);

            Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            trackPaint.setStyle(Paint.Style.STROKE);
            trackPaint.setStrokeCap(Paint.Cap.ROUND);

            Paint arcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            arcPaint.setStyle(Paint.Style.STROKE);
            arcPaint.setStrokeCap(Paint.Cap.ROUND);

            Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            glowPaint.setStyle(Paint.Style.STROKE);
            glowPaint.setStrokeCap(Paint.Cap.ROUND);

            Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            textPaint.setTextAlign(Paint.Align.CENTER);
            textPaint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));

            // 1. Subtle Outer Track & Hourly Ticks
            trackPaint.setColor(COL_LINE);
            trackPaint.setStrokeWidth(10f);
            canvas.drawArc(outerRect, 135f, 270f, false, trackPaint);

            // Draw hour notches matching shift duration
            int tickCount = Math.max(4, totalHours);
            Paint tickPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            tickPaint.setStrokeCap(Paint.Cap.ROUND);
            for (int i = 0; i <= tickCount; i++) {
                float angleDeg = 135f + (i * 270f / (float) tickCount);
                double rad = Math.toRadians(angleDeg);
                float tLen = (i == 0 || i == tickCount || i == tickCount / 2) ? 14f : 8f;
                float x1 = cx + (float) Math.cos(rad) * (rOuter + 6f);
                float y1 = cy + (float) Math.sin(rad) * (rOuter + 6f);
                float x2 = cx + (float) Math.cos(rad) * (rOuter + 6f + tLen);
                float y2 = cy + (float) Math.sin(rad) * (rOuter + 6f + tLen);

                tickPaint.setColor((i == 0 || i == tickCount) ? COL_ACCENT : COL_QUIET);
                tickPaint.setStrokeWidth((i == 0 || i == tickCount) ? 4f : 2.5f);
                canvas.drawLine(x1, y1, x2, y2, tickPaint);
            }

            // 2. Active Progress Sweep & Luminous Glow
            float outerSweep = Math.max(0.01f, shiftProgress * 270f);
            int arcColor = onShift ? COL_ACCENT : COL_QUIET;

            glowPaint.setColor(arcColor);
            glowPaint.setAlpha(onShift ? 70 : 20);
            glowPaint.setStrokeWidth(20f);
            canvas.drawArc(outerRect, 135f, outerSweep, false, glowPaint);

            arcPaint.setColor(arcColor);
            arcPaint.setStrokeWidth(10f);
            canvas.drawArc(outerRect, 135f, outerSweep, false, arcPaint);

            // Progress Head Pip
            if (onShift && shiftProgress > 0.01f) {
                double headRad = Math.toRadians(135f + outerSweep);
                float hx = cx + (float) Math.cos(headRad) * rOuter;
                float hy = cy + (float) Math.sin(headRad) * rOuter;
                Paint pipPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                pipPaint.setStyle(Paint.Style.FILL);
                pipPaint.setColor(0xFFFFFFFF);
                canvas.drawCircle(hx, hy, 6.5f, pipPaint);
            }

            // 3. Inner Secondary Track (Cyan / Emerald Aura)
            trackPaint.setStrokeWidth(6f);
            trackPaint.setColor(0x22475569);
            canvas.drawArc(innerRect, 135f, 270f, false, trackPaint);

            float innerSweep = Math.max(0.01f, (1f - shiftProgress) * 270f);
            arcPaint.setColor(COL_EMERALD);
            arcPaint.setStrokeWidth(6f);
            canvas.drawArc(innerRect, 135f, innerSweep, false, arcPaint);

            // 4. Center Monospace Digital Core Display
            int pct = (int) (shiftProgress * 100);

            // Shift % Label (Above Time)
            textPaint.setColor(onShift ? arcColor : COL_MUTED);
            textPaint.setTextSize(20f);
            textPaint.setLetterSpacing(0.08f);
            canvas.drawText(onShift ? ("SHIFT " + pct + "%") : "OFF SHIFT", cx, cy - 36f, textPaint);

            // Hero Digital Time (Center)
            textPaint.setColor(0xFFFFFFFF);
            textPaint.setTextSize(46f);
            textPaint.setLetterSpacing(0.02f);
            canvas.drawText(timeStr, cx, cy + 10f, textPaint);

            // Subtext Elapsed / Remaining (Below Time inside dial)
            textPaint.setColor(0xFF38BDF8);
            textPaint.setTextSize(16.5f);
            textPaint.setLetterSpacing(0.04f);
            canvas.drawText(subtext, cx, cy + 42f, textPaint);

            // 5. Dial Baseline Start/End Time Markers
            textPaint.setTextSize(16f);
            textPaint.setColor(COL_ACCENT);
            textPaint.setLetterSpacing(0f);

            double leftRad = Math.toRadians(135.0);
            float lx = cx + (float) Math.cos(leftRad) * (rOuter + 26f);
            float ly = cy + (float) Math.sin(leftRad) * (rOuter + 26f);
            canvas.drawText(startLabel, lx - 10f, ly + 14f, textPaint);

            double rightRad = Math.toRadians(45.0);
            float rx = cx + (float) Math.cos(rightRad) * (rOuter + 26f);
            float ry = cy + (float) Math.sin(rightRad) * (rOuter + 26f);
            canvas.drawText(endLabel, rx + 10f, ry + 14f, textPaint);

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
                    ComponentName cn = new ComponentName(context, GatehouseWidgetProvider.class);
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

