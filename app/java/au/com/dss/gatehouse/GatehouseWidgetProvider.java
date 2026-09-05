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
        int imgId = context.getResources().getIdentifier("widget_chronograph_img", "id", pkg);

        // 1. Fetch live or cached Deputy roster
        RosterProvider api = Rostering.create(context);
        RosterProvider.Result roster = api.loadCachedResult();
        if (roster == null) {
            roster = api.createSampleFallback();
        }

        long nowSec = System.currentTimeMillis() / 1000L;
        RosterProvider.Shift activeShift = null;
        RosterProvider.Shift nextShift = null;
        if (roster != null && roster.weekShifts != null) {
            for (RosterProvider.Shift s : roster.weekShifts) {
                if (nowSec >= s.startTs && nowSec <= s.endTs) {
                    activeShift = s;
                    break;
                } else if (nowSec < s.startTs && (nextShift == null || s.startTs < nextShift.startTs)) {
                    nextShift = s;
                }
            }
        }

        float shiftProgress = 0f;
        String startLabel = "16:00";
        String endLabel = "00:00";
        int totalHours = 8;

        SimpleDateFormat sdfHour = new SimpleDateFormat("HH:mm", Locale.US);
        SimpleDateFormat sdfTime = new SimpleDateFormat("HH:mm:ss", Locale.US);
        sdfTime.setTimeZone(TimeZone.getDefault());
        String curTimeStr = sdfTime.format(new Date(nowSec * 1000L));

        if (activeShift != null) {
            startLabel = sdfHour.format(new Date(activeShift.startTs * 1000L));
            endLabel = sdfHour.format(new Date(activeShift.endTs * 1000L));
            totalHours = Math.max(1, (int) Math.round(activeShift.totalHours));

            long elapsedSec = Math.max(0, nowSec - activeShift.startTs);
            long totalSec = Math.max(1, activeShift.endTs - activeShift.startTs);
            shiftProgress = Math.min(1f, Math.max(0f, (float) elapsedSec / (float) totalSec));
        } else if (nextShift != null) {
            startLabel = sdfHour.format(new Date(nextShift.startTs * 1000L));
            endLabel = sdfHour.format(new Date(nextShift.endTs * 1000L));
            totalHours = Math.max(1, (int) Math.round(nextShift.totalHours));
            shiftProgress = 0f;
        }

        // 2. Render Pure In-App Vector Chronograph Dial Bitmap
        if (imgId != 0) {
            Bitmap dialBitmap = renderChronographBitmap(
                    context, 600, 600, shiftProgress, curTimeStr, startLabel, endLabel, totalHours, activeShift != null);
            if (dialBitmap != null) {
                views.setImageViewBitmap(imgId, dialBitmap);
            }
        }

        // 3. 1-Tap Launch MainActivity
        Intent openIntent = new Intent(context, MainActivity.class);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pOpen = PendingIntent.getActivity(context, 0, openIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        if (rootId != 0) views.setOnClickPendingIntent(rootId, pOpen);
        if (imgId != 0) views.setOnClickPendingIntent(imgId, pOpen);

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    /**
     * Vector 2D Canvas rendering of the Shift Chronograph Dial identical to in-app ChronographView.
     */
    private static Bitmap renderChronographBitmap(
            Context context, int w, int h, float shiftProgress, String timeStr,
            String startLabel, String endLabel, int totalHours, boolean onShift) {
        try {
            Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bmp);

            float cx = w / 2f;
            float cy = h / 2f - 10f;
            float rOuter = 215f;
            float rInner = 180f;

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
            trackPaint.setStrokeWidth(13f);
            canvas.drawArc(outerRect, 135f, 270f, false, trackPaint);

            // Draw hour notches matching shift duration
            int tickCount = Math.max(4, totalHours);
            Paint tickPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            tickPaint.setStrokeCap(Paint.Cap.ROUND);
            for (int i = 0; i <= tickCount; i++) {
                float angleDeg = 135f + (i * 270f / (float) tickCount);
                double rad = Math.toRadians(angleDeg);
                boolean isMajor = (i == 0 || i == tickCount || i == tickCount / 2);
                float tLen = isMajor ? 20f : 11f;
                float x1 = cx + (float) Math.cos(rad) * (rOuter + 8f);
                float y1 = cy + (float) Math.sin(rad) * (rOuter + 8f);
                float x2 = cx + (float) Math.cos(rad) * (rOuter + 8f + tLen);
                float y2 = cy + (float) Math.sin(rad) * (rOuter + 8f + tLen);

                tickPaint.setColor(isMajor ? COL_ACCENT : COL_QUIET);
                tickPaint.setStrokeWidth(isMajor ? 5f : 3f);
                canvas.drawLine(x1, y1, x2, y2, tickPaint);
            }

            // 2. Active Progress Sweep & Luminous Glow
            float outerSweep = Math.max(0.01f, shiftProgress * 270f);
            int arcColor = onShift ? COL_ACCENT : COL_QUIET;

            glowPaint.setColor(arcColor);
            glowPaint.setAlpha(onShift ? 70 : 25);
            glowPaint.setStrokeWidth(26f);
            canvas.drawArc(outerRect, 135f, outerSweep, false, glowPaint);

            arcPaint.setColor(arcColor);
            arcPaint.setStrokeWidth(13f);
            canvas.drawArc(outerRect, 135f, outerSweep, false, arcPaint);

            // Progress Head Pip
            if (onShift && shiftProgress > 0.01f) {
                double headRad = Math.toRadians(135f + outerSweep);
                float hx = cx + (float) Math.cos(headRad) * rOuter;
                float hy = cy + (float) Math.sin(headRad) * rOuter;
                Paint pipPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                pipPaint.setStyle(Paint.Style.FILL);
                pipPaint.setColor(0xFFFFFFFF);
                canvas.drawCircle(hx, hy, 8.5f, pipPaint);
            }

            // 3. Inner Secondary Track (Cyan / Emerald Aura)
            trackPaint.setStrokeWidth(8f);
            trackPaint.setColor(0x22475569);
            canvas.drawArc(innerRect, 135f, 270f, false, trackPaint);

            float innerSweep = Math.max(0.01f, (1f - shiftProgress) * 270f);
            arcPaint.setColor(COL_EMERALD);
            arcPaint.setStrokeWidth(8f);
            canvas.drawArc(innerRect, 135f, innerSweep, false, arcPaint);

            // 4. Center Monospace Digital Core Display (Identical to in-app Chronograph)
            int pct = (int) (shiftProgress * 100);

            // Top: Shift % Label (Above Time)
            textPaint.setColor(onShift ? arcColor : COL_MUTED);
            textPaint.setTextSize(26f);
            textPaint.setLetterSpacing(0.08f);
            canvas.drawText(onShift ? ("SHIFT " + pct + "%") : "OFF SHIFT", cx, cy - 48f, textPaint);

            // Center: Hero Digital Time
            textPaint.setColor(0xFFFFFFFF);
            textPaint.setTextSize(60f);
            textPaint.setLetterSpacing(0.02f);
            canvas.drawText(timeStr, cx, cy + 14f, textPaint);

            // Bottom: Timezone / City Sub-label
            textPaint.setColor(COL_QUIET);
            textPaint.setTextSize(19f);
            textPaint.setLetterSpacing(0.12f);
            canvas.drawText("AEST · BRISBANE", cx, cy + 62f, textPaint);

            // 5. Dial Baseline Start/End Time Markers
            textPaint.setTextSize(21f);
            textPaint.setColor(COL_MUTED);
            textPaint.setLetterSpacing(0f);

            double leftRad = Math.toRadians(135.0);
            float lx = cx + (float) Math.cos(leftRad) * (rOuter + 38f);
            float ly = cy + (float) Math.sin(leftRad) * (rOuter + 38f);
            canvas.drawText(startLabel, lx - 14f, ly + 18f, textPaint);

            double rightRad = Math.toRadians(45.0);
            float rx = cx + (float) Math.cos(rightRad) * (rOuter + 38f);
            float ry = cy + (float) Math.sin(rightRad) * (rOuter + 38f);
            canvas.drawText(endLabel, rx + 14f, ry + 18f, textPaint);

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
            RosterProvider api = Rostering.create(context);
            api.syncRoster(new RosterProvider.Callback<RosterProvider.Result>() {
                @Override
                public void onSuccess(RosterProvider.Result result) {
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

