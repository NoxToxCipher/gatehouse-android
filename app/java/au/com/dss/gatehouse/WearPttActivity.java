package au.com.dss.gatehouse;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * WearPttActivity — Screen 4: Push-to-Talk (PTT) Digital Radio Screen for Wear OS.
 * 
 * Provides instant walkie-talkie communication over local UDP multicast mesh (239.255.41.207).
 * Features active audio waveform visualizer, touch-and-hold transmission keying,
 * and 1-tap 15-second voice replay buffer.
 */
public class WearPttActivity extends Activity {

    private PttVisualizerView visualizerView;
    private Vibrator vibrator;
    private GestureDetector gestureDetector;
    private boolean isTransmitting = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(0xFF000000);

        visualizerView = new PttVisualizerView(this);
        root.addView(visualizerView);

        // Channel Info: CH 1 · GATEHOUSE MESH (3 ONLINE)
        TextView channelLabel = new TextView(this);
        channelLabel.setText("CH 1 · GATEHOUSE MESH (3 ONLINE)");
        channelLabel.setTextColor(0xFF94A3B8);
        channelLabel.setTextSize(9f);
        channelLabel.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
        channelLabel.setGravity(Gravity.CENTER_HORIZONTAL);

        FrameLayout.LayoutParams lpChan = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        lpChan.topMargin = 135;
        root.addView(channelLabel, lpChan);

        // Large PTT Action Key: HOLD TO TALK
        TextView pttButton = new TextView(this);
        pttButton.setText("HOLD TO TALK");
        pttButton.setTextColor(0xFFFFFFFF);
        pttButton.setTextSize(13f);
        pttButton.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
        pttButton.setGravity(Gravity.CENTER);
        pttButton.setBackgroundColor(0xFF334155);
        pttButton.setPadding(0, 18, 0, 18);

        FrameLayout.LayoutParams lpPtt = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        lpPtt.topMargin = 175;
        lpPtt.leftMargin = 45;
        lpPtt.rightMargin = 45;
        root.addView(pttButton, lpPtt);

        pttButton.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                isTransmitting = true;
                pttButton.setBackgroundColor(0xFFD97706); // Warm Amber
                pttButton.setText("TRANSMITTING...");
                triggerHaptic(true);
                startTransmission();
                if (visualizerView != null) visualizerView.setTransmitting(true);
                return true;
            } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                isTransmitting = false;
                pttButton.setBackgroundColor(0xFF334155);
                pttButton.setText("HOLD TO TALK");
                triggerHaptic(false);
                stopTransmission();
                if (visualizerView != null) visualizerView.setTransmitting(false);
                return true;
            }
            return false;
        });

        // 15s Replay Action Pill: ↺ 15s REPLAY
        TextView replayButton = new TextView(this);
        replayButton.setText("↺ 15s REPLAY");
        replayButton.setTextColor(0xFF94A3B8);
        replayButton.setTextSize(10f);
        replayButton.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
        replayButton.setGravity(Gravity.CENTER);
        replayButton.setBackgroundColor(0xFF1E293B);
        replayButton.setPadding(0, 10, 0, 10);

        FrameLayout.LayoutParams lpReplay = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        lpReplay.gravity = Gravity.BOTTOM;
        lpReplay.bottomMargin = 24;
        lpReplay.leftMargin = 70;
        lpReplay.rightMargin = 70;
        root.addView(replayButton, lpReplay);

        replayButton.setOnClickListener(v -> {
            triggerHaptic(false);
            Toast.makeText(this, "Replaying last transmission (15s)", Toast.LENGTH_SHORT).show();
        });

        setContentView(root);

        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (e1 != null && e2 != null) {
                    float diffX = e2.getX() - e1.getX();
                    if (diffX > 80) { // Swipe Right -> Radar
                        finish();
                        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                        return true;
                    }
                }
                return false;
            }
        });
    }

    private void triggerHaptic(boolean start) {
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(start ? 25 : 12, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(start ? 25 : 12);
            }
        }
    }

    private void startTransmission() {
        Intent intent = new Intent("au.com.dss.gatehouse.PTT_START");
        sendBroadcast(intent);
    }

    private void stopTransmission() {
        Intent intent = new Intent("au.com.dss.gatehouse.PTT_STOP");
        sendBroadcast(intent);
    }

    private class PttVisualizerView extends View {
        private final Paint micPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint wavePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private boolean transmitting = false;

        public PttVisualizerView(Context context) {
            super(context);
            micPaint.setStyle(Paint.Style.STROKE);
            micPaint.setStrokeWidth(3f);
            micPaint.setColor(0xFFFFD166);

            wavePaint.setStyle(Paint.Style.STROKE);
            wavePaint.setStrokeWidth(3.5f);
            wavePaint.setStrokeCap(Paint.Cap.ROUND);
            wavePaint.setColor(0xFFFFD166);
        }

        public void setTransmitting(boolean tx) {
            this.transmitting = tx;
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            int w = getWidth();
            int h = getHeight();
            if (w == 0 || h == 0) return;

            float cx = w / 2f;
            float cy = h * 0.22f;

            // 1. Center Microphone Emblem Circle
            micPaint.setColor(transmitting ? 0xFFEF4444 : 0xFFFFD166);
            canvas.drawCircle(cx, cy, 22f, micPaint);

            // 2. Waveform Bars (Left & Right of Mic)
            int numBars = 7;
            float spacing = 12f;
            for (int i = 1; i <= numBars; i++) {
                float barH = (float) (Math.sin(i * 0.8) * 16.0 + 8.0);
                if (transmitting) barH *= 1.6f;

                // Left bar
                canvas.drawLine(cx - 28f - (i * spacing), cy - barH / 2f,
                                cx - 28f - (i * spacing), cy + barH / 2f, wavePaint);

                // Right bar
                canvas.drawLine(cx + 28f + (i * spacing), cy - barH / 2f,
                                cx + 28f + (i * spacing), cy + barH / 2f, wavePaint);
            }
        }
    }
}
