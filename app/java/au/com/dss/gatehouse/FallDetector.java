package au.com.dss.gatehouse;

import java.util.Locale;

/**
 * A fall, as the accelerometer sees it: a stretch of near weightlessness,
 * a hard impact soon after, then stillness. All three in order, or nothing.
 *
 * Pure arithmetic, no Android, so the thresholds can be tuned from recorded
 * traces and the same code runs on every hut phone. Times are whatever
 * monotonic millisecond clock the caller feeds in.
 *
 * What it will not catch: a slump or collapse where the phone never reads
 * below the free-fall threshold. What it will raise on: a phone dropped
 * onto concrete and left there. The check-in that follows is what turns a
 * detection into an alert, so start strict and loosen from the record.
 */
final class FallDetector {

    interface Listener {
        void onFall(Event e);
    }

    static final class Event {
        long impactElapsedMs;
        float peakG;
        int freeFallMs;
        float stillDevG;

        String describe() {
            return String.format(Locale.US, "free-fall %d ms, impact %.1f g, still within %.2f g",
                    freeFallMs, peakG, stillDevG);
        }
    }

    // Tunables. Start strict; loosen from the record.
    /** Below this many g counts as weightless. */
    static final float FREE_FALL_G = 0.60f;
    /** Weightless for at least this long to count as a fall starting. */
    static final int FREE_FALL_MIN_MS = 100;
    /** An impact is a reading above this many g. */
    static final float IMPACT_G = 2.5f;
    /** The impact must land within this long of the free-fall ending. */
    static final int IMPACT_WINDOW_MS = 1500;
    /** Ignore this long after the impact: bouncing, tumbling, the phone settling. */
    static final int SETTLE_MS = 1000;
    /** Then the phone has to stay still for this long. */
    static final int STILL_MS = 2500;
    /** Still means |g| stays within this much of one g throughout. */
    static final float STILL_MAX_DEV_G = 0.15f;
    /** After a detection, ignore everything for this long. */
    static final int COOLDOWN_MS = 60_000;

    private static final float G = 9.80665f;

    private enum State { IDLE, FREE_FALL, AWAIT_IMPACT, SETTLING, STILL }

    private final Listener listener;
    private State state = State.IDLE;
    private long freeFallStart, impactAt, stillStart, impactDeadline, cooldownUntil;
    private boolean freeFallQualified;
    private int freeFallMs;
    private float peakG, stillDev;

    FallDetector(Listener listener) {
        this.listener = listener;
    }

    String state() {
        return state.name();
    }

    void feed(long tMs, float ax, float ay, float az) {
        float g = (float) Math.sqrt(ax * ax + ay * ay + az * az) / G;
        if (tMs < cooldownUntil) return;

        switch (state) {
            case IDLE:
                if (g < FREE_FALL_G) {
                    state = State.FREE_FALL;
                    freeFallStart = tMs;
                    freeFallQualified = false;
                }
                break;

            case FREE_FALL:
                if (g < FREE_FALL_G) {
                    if (tMs - freeFallStart >= FREE_FALL_MIN_MS) freeFallQualified = true;
                } else if (freeFallQualified) {
                    freeFallMs = (int) (tMs - freeFallStart);
                    state = State.AWAIT_IMPACT;
                    impactDeadline = tMs + IMPACT_WINDOW_MS;
                    peakG = g;
                    if (g >= IMPACT_G) impact(tMs, g);
                } else {
                    state = State.IDLE;
                }
                break;

            case AWAIT_IMPACT:
                if (g > peakG) peakG = g;
                if (g >= IMPACT_G) impact(tMs, g);
                else if (tMs > impactDeadline) state = State.IDLE;
                break;

            case SETTLING:
                if (g > peakG) peakG = g;
                if (tMs - impactAt >= SETTLE_MS) {
                    state = State.STILL;
                    stillStart = tMs;
                    stillDev = 0f;
                }
                break;

            case STILL:
                float dev = Math.abs(g - 1f);
                if (dev > STILL_MAX_DEV_G) {
                    // Moved: picked up, got up, or never a person. Not a fall.
                    state = State.IDLE;
                    break;
                }
                if (dev > stillDev) stillDev = dev;
                if (tMs - stillStart >= STILL_MS) {
                    Event e = new Event();
                    e.impactElapsedMs = impactAt;
                    e.peakG = peakG;
                    e.freeFallMs = freeFallMs;
                    e.stillDevG = stillDev;
                    state = State.IDLE;
                    cooldownUntil = tMs + COOLDOWN_MS;
                    listener.onFall(e);
                }
                break;
        }
    }

    private void impact(long tMs, float g) {
        impactAt = tMs;
        if (g > peakG) peakG = g;
        state = State.SETTLING;
    }
}
