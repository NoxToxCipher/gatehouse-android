package au.com.dss.gatehouse;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Locale;

/**
 * GatehouseDynamicIslandOverlay — High-fidelity top cutout Dynamic Island & Live Capsule.
 * Solid pure jet black (0xFF000000) backdrop to prevent status bar bleed-through,
 * with fluid ValueAnimator dimension morphing and spring physics.
 */
public class GatehouseDynamicIslandOverlay {
    private static final String TAG = "DynamicIslandOverlay";

    public enum IslandState {
        HIDDEN,
        COMPACT_PILL,
        EXPANDED_CARD
    }

    private static GatehouseDynamicIslandOverlay instance;
    private final Context context;
    private FrameLayout parentContainer;
    private FrameLayout islandRoot;
    private LinearLayout compactView;
    private LinearLayout expandedView;

    private IslandState currentState = IslandState.HIDDEN;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable autoCollapseRunnable;

    // Active Data
    private String currentEmoji = "⛽";
    private String currentTitle = "OOM 168.9¢";
    private String currentSubtitle = "0.8km · Lowest";
    private int currentAccentColor = 0xFFF59E0B;
    private String currentNavAddress = "OOM Energy Kingston, 122 Kingston Rd, Kingston QLD";

    // Standard Dimensions
    private int compactWidth;
    private int compactHeight;
    private int expandedWidth;
    private int expandedHeight;

    public static synchronized GatehouseDynamicIslandOverlay getInstance(Context ctx) {
        if (instance == null) {
            instance = new GatehouseDynamicIslandOverlay(ctx.getApplicationContext());
        }
        return instance;
    }

    private GatehouseDynamicIslandOverlay(Context ctx) {
        this.context = ctx;
        initDimensions();
    }

    private void initDimensions() {
        compactWidth = dp(168);
        compactHeight = dp(32);
        expandedWidth = dp(320);
        expandedHeight = dp(155);
    }

    public void attachToContainer(FrameLayout container) {
        this.parentContainer = container;
        initViews();
    }

    private int dp(float value) {
        return (int) (value * context.getResources().getDisplayMetrics().density + 0.5f);
    }

    private void initViews() {
        if (parentContainer == null) return;
        if (islandRoot != null && islandRoot.getParent() != null) {
            ((ViewGroup) islandRoot.getParent()).removeView(islandRoot);
        }

        islandRoot = new FrameLayout(parentContainer.getContext());
        FrameLayout.LayoutParams rlp = new FrameLayout.LayoutParams(compactWidth, compactHeight);
        rlp.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        // Positioned cleanly below the status bar so zero status icons are obscured
        rlp.topMargin = dp(44);
        islandRoot.setLayoutParams(rlp);
        islandRoot.setVisibility(View.GONE);
        islandRoot.setElevation(dp(20));

        // Solid Pure Jet Black (#000000) so zero system text bleeds through
        GradientDrawable pillBg = new GradientDrawable();
        pillBg.setColor(0xFF000000);
        pillBg.setCornerRadius(compactHeight / 2f);
        pillBg.setStroke(dp(1.2f), 0xFFF59E0B);
        islandRoot.setBackground(pillBg);
        islandRoot.setPadding(dp(8), dp(2), dp(8), dp(2));

        // 1. Compact Pill View
        compactView = new LinearLayout(parentContainer.getContext());
        compactView.setOrientation(LinearLayout.HORIZONTAL);
        compactView.setGravity(Gravity.CENTER);
        compactView.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        TextView tvEmoji = new TextView(parentContainer.getContext());
        tvEmoji.setText("⛽");
        tvEmoji.setTextSize(13f);
        tvEmoji.setPadding(0, 0, dp(4), 0);
        compactView.addView(tvEmoji);

        TextView tvText = new TextView(parentContainer.getContext());
        tvText.setText("168.9¢ · 0.8km");
        tvText.setTextColor(0xFFFEF08A);
        tvText.setTextSize(11f);
        tvText.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
        compactView.addView(tvText);

        TextView tvDot = new TextView(parentContainer.getContext());
        tvDot.setText("●");
        tvDot.setTextColor(0xFF10B981);
        tvDot.setTextSize(8f);
        tvDot.setPadding(dp(5), 0, 0, 0);
        compactView.addView(tvDot);

        islandRoot.addView(compactView);

        // 2. Expanded HUD View
        expandedView = new LinearLayout(parentContainer.getContext());
        expandedView.setOrientation(LinearLayout.VERTICAL);
        expandedView.setVisibility(View.GONE);
        expandedView.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        islandRoot.addView(expandedView);

        // Gesture detector for tap-to-morph and swipe-up to dismiss
        final android.view.GestureDetector gestureDetector = new android.view.GestureDetector(parentContainer.getContext(), new android.view.GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onFling(android.view.MotionEvent e1, android.view.MotionEvent e2, float velocityX, float velocityY) {
                if (velocityY < -150) {
                    hideIsland();
                    return true;
                }
                return false;
            }
            @Override
            public boolean onSingleTapConfirmed(android.view.MotionEvent e) {
                if (currentState == IslandState.COMPACT_PILL) {
                    expandIsland();
                } else if (currentState == IslandState.EXPANDED_CARD) {
                    collapseIsland();
                }
                return true;
            }
        });

        islandRoot.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, android.view.MotionEvent event) {
                return gestureDetector.onTouchEvent(event);
            }
        });

        parentContainer.addView(islandRoot);
    }

    public void showFuelIsland(final double oomPrice, final double savingCents, final int minsRemaining) {
        if (parentContainer == null) return;
        currentEmoji = "⛽";
        currentTitle = String.format(Locale.US, "OOM %.1f¢", oomPrice);
        currentSubtitle = String.format(Locale.US, "0.8km · Save %.1f¢/L", savingCents);
        currentAccentColor = 0xFFF59E0B;
        currentNavAddress = "OOM Energy Kingston, 122 Kingston Rd, Kingston QLD";

        parentContainer.post(new Runnable() {
            @Override
            public void run() {
                if (islandRoot == null) initViews();
                updateViewsData();
                animateShowIsland();
            }
        });
    }

    private void updateViewsData() {
        if (islandRoot == null) return;

        // Update Compact View
        if (compactView != null && compactView.getChildCount() >= 3) {
            ((TextView) compactView.getChildAt(0)).setText(currentEmoji);
            ((TextView) compactView.getChildAt(1)).setText(currentTitle.replace("OOM ", "") + " · 0.8km");
            ((TextView) compactView.getChildAt(1)).setTextColor(currentAccentColor);
        }

        // Build Expanded View
        if (expandedView != null) {
            expandedView.removeAllViews();
            expandedView.setPadding(dp(12), dp(10), dp(12), dp(10));

            // Header
            LinearLayout hRow = new LinearLayout(parentContainer.getContext());
            hRow.setOrientation(LinearLayout.HORIZONTAL);
            hRow.setGravity(Gravity.CENTER_VERTICAL);

            TextView tvHead = new TextView(parentContainer.getContext());
            tvHead.setText(currentEmoji + " LIVE FUEL RADAR CAPSULE");
            tvHead.setTextColor(currentAccentColor);
            tvHead.setTextSize(10.5f);
            tvHead.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
            LinearLayout.LayoutParams hlp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            tvHead.setLayoutParams(hlp);
            hRow.addView(tvHead);

            TextView btnClose = new TextView(parentContainer.getContext());
            btnClose.setText("✕");
            btnClose.setTextColor(0xFF94A3B8);
            btnClose.setTextSize(14f);
            btnClose.setPadding(dp(8), 0, dp(4), 0);
            btnClose.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    hideIsland();
                }
            });
            hRow.addView(btnClose);
            expandedView.addView(hRow);

            // Station Leaderboard
            TextView tvLeader = new TextView(parentContainer.getContext());
            tvLeader.setText("🟢 OOM Kingston: " + currentTitle.replace("OOM ", "") + "/L (0.8km · Lowest)\n" +
                    "⚪ 7-Eleven: 174.9¢ · ⚪ Ampol: 176.9¢\n" +
                    "💰 Save $3.60 on a 60L fill vs 7-Eleven");
            tvLeader.setTextColor(0xFFE2E8F0);
            tvLeader.setTextSize(10.5f);
            tvLeader.setTypeface(Typeface.MONOSPACE);
            tvLeader.setPadding(0, dp(6), 0, dp(8));
            expandedView.addView(tvLeader);

            // Action Buttons
            LinearLayout btnRow = new LinearLayout(parentContainer.getContext());
            btnRow.setOrientation(LinearLayout.HORIZONTAL);

            TextView btnNav = new TextView(parentContainer.getContext());
            btnNav.setText("🗺️ Drive to OOM");
            btnNav.setTextColor(0xFF0F172A);
            btnNav.setTextSize(10f);
            btnNav.setTypeface(Typeface.DEFAULT_BOLD);
            btnNav.setGravity(Gravity.CENTER);
            btnNav.setPadding(dp(8), dp(6), dp(8), dp(6));
            GradientDrawable navBg = new GradientDrawable();
            navBg.setColor(currentAccentColor);
            navBg.setCornerRadius(dp(6));
            btnNav.setBackground(navBg);
            LinearLayout.LayoutParams nlp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            nlp.rightMargin = dp(6);
            btnNav.setLayoutParams(nlp);
            btnNav.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    hideIsland();
                    try {
                        Uri gmmIntentUri = Uri.parse("geo:0,0?q=" + Uri.encode(currentNavAddress));
                        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                        mapIntent.setPackage("com.google.android.apps.maps");
                        parentContainer.getContext().startActivity(mapIntent);
                    } catch (Exception e) {
                        Intent webMap = new Intent(Intent.ACTION_VIEW, Uri.parse("https://maps.google.com/?q=" + Uri.encode(currentNavAddress)));
                        parentContainer.getContext().startActivity(webMap);
                    }
                }
            });
            btnRow.addView(btnNav);

            TextView btnCollapse = new TextView(parentContainer.getContext());
            btnCollapse.setText("Collapse");
            btnCollapse.setTextColor(0xFF94A3B8);
            btnCollapse.setTextSize(10f);
            btnCollapse.setTypeface(Typeface.MONOSPACE);
            btnCollapse.setGravity(Gravity.CENTER);
            btnCollapse.setPadding(dp(8), dp(6), dp(8), dp(6));
            GradientDrawable colBg = new GradientDrawable();
            colBg.setColor(0xFF1E293B);
            colBg.setCornerRadius(dp(6));
            btnCollapse.setBackground(colBg);
            btnCollapse.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    collapseIsland();
                }
            });
            btnRow.addView(btnCollapse);

            expandedView.addView(btnRow);
        }
    }

    private void animateShowIsland() {
        if (islandRoot == null) return;
        currentState = IslandState.COMPACT_PILL;

        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) islandRoot.getLayoutParams();
        lp.width = compactWidth;
        lp.height = compactHeight;
        islandRoot.setLayoutParams(lp);

        GradientDrawable pillBg = new GradientDrawable();
        pillBg.setColor(0xFF000000);
        pillBg.setCornerRadius(compactHeight / 2f);
        pillBg.setStroke(dp(1.2f), currentAccentColor);
        islandRoot.setBackground(pillBg);

        compactView.setVisibility(View.VISIBLE);
        compactView.setAlpha(1f);
        expandedView.setVisibility(View.GONE);

        islandRoot.setVisibility(View.VISIBLE);
        islandRoot.setScaleX(0.1f);
        islandRoot.setScaleY(0.1f);
        islandRoot.setAlpha(0f);

        islandRoot.animate()
                .scaleX(1.0f)
                .scaleY(1.0f)
                .alpha(1.0f)
                .setDuration(320)
                .setInterpolator(new OvershootInterpolator(1.3f))
                .start();

        scheduleAutoCollapse(8000);
    }

    public void expandIsland() {
        if (islandRoot == null || currentState == IslandState.EXPANDED_CARD) return;
        currentState = IslandState.EXPANDED_CARD;
        cancelAutoCollapse();

        try {
            islandRoot.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP);
        } catch (Exception ignored) {}

        updateViewsData();

        // Fluid morph from compactWidth/Height to expandedWidth/Height
        final int startW = islandRoot.getWidth() > 0 ? islandRoot.getWidth() : compactWidth;
        final int startH = islandRoot.getHeight() > 0 ? islandRoot.getHeight() : compactHeight;
        final float startRadius = compactHeight / 2f;
        final float endRadius = dp(16);

        // Fade out compact contents quickly
        compactView.animate().alpha(0f).setDuration(80).withEndAction(new Runnable() {
            @Override
            public void run() {
                compactView.setVisibility(View.GONE);
                expandedView.setAlpha(0f);
                expandedView.setVisibility(View.VISIBLE);
                expandedView.animate().alpha(1f).setDuration(160).start();
            }
        }).start();

        ValueAnimator anim = ValueAnimator.ofFloat(0f, 1f);
        anim.setDuration(280);
        anim.setInterpolator(new OvershootInterpolator(1.1f));
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator va) {
                float f = va.getAnimatedFraction();
                int curW = (int) (startW + (expandedWidth - startW) * f);
                int curH = (int) (startH + (expandedHeight - startH) * f);
                float curR = startRadius + (endRadius - startRadius) * f;

                FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) islandRoot.getLayoutParams();
                lp.width = curW;
                lp.height = curH;
                islandRoot.setLayoutParams(lp);

                GradientDrawable bg = new GradientDrawable();
                bg.setColor(0xFF000000);
                bg.setCornerRadius(curR);
                bg.setStroke(dp(1.2f), currentAccentColor);
                islandRoot.setBackground(bg);
            }
        });
        anim.start();

        scheduleAutoCollapse(12000);
    }

    public void collapseIsland() {
        if (islandRoot == null || currentState != IslandState.EXPANDED_CARD) return;
        currentState = IslandState.COMPACT_PILL;
        cancelAutoCollapse();

        try {
            islandRoot.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP);
        } catch (Exception ignored) {}

        final int startW = islandRoot.getWidth();
        final int startH = islandRoot.getHeight();
        final float startRadius = dp(16);
        final float endRadius = compactHeight / 2f;

        // Fade out expanded contents quickly
        expandedView.animate().alpha(0f).setDuration(80).withEndAction(new Runnable() {
            @Override
            public void run() {
                expandedView.setVisibility(View.GONE);
                compactView.setAlpha(0f);
                compactView.setVisibility(View.VISIBLE);
                compactView.animate().alpha(1f).setDuration(160).start();
            }
        }).start();

        ValueAnimator anim = ValueAnimator.ofFloat(0f, 1f);
        anim.setDuration(240);
        anim.setInterpolator(new AccelerateDecelerateInterpolator());
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator va) {
                float f = va.getAnimatedFraction();
                int curW = (int) (startW + (compactWidth - startW) * f);
                int curH = (int) (startH + (compactHeight - startH) * f);
                float curR = startRadius + (endRadius - startRadius) * f;

                FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) islandRoot.getLayoutParams();
                lp.width = curW;
                lp.height = curH;
                islandRoot.setLayoutParams(lp);

                GradientDrawable bg = new GradientDrawable();
                bg.setColor(0xFF000000);
                bg.setCornerRadius(curR);
                bg.setStroke(dp(1.2f), currentAccentColor);
                islandRoot.setBackground(bg);
            }
        });
        anim.start();

        scheduleAutoCollapse(6000);
    }

    public void hideIsland() {
        if (islandRoot == null || currentState == IslandState.HIDDEN) return;
        currentState = IslandState.HIDDEN;
        cancelAutoCollapse();

        islandRoot.animate()
                .translationY(-dp(30))
                .scaleX(0.1f)
                .scaleY(0.1f)
                .alpha(0f)
                .setDuration(220)
                .withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        islandRoot.setVisibility(View.GONE);
                        islandRoot.setTranslationY(0);
                    }
                })
                .start();
    }

    private void scheduleAutoCollapse(long delayMs) {
        cancelAutoCollapse();
        autoCollapseRunnable = new Runnable() {
            @Override
            public void run() {
                if (currentState == IslandState.EXPANDED_CARD) {
                    collapseIsland();
                } else if (currentState == IslandState.COMPACT_PILL) {
                    hideIsland();
                }
            }
        };
        handler.postDelayed(autoCollapseRunnable, delayMs);
    }

    private void cancelAutoCollapse() {
        if (autoCollapseRunnable != null) {
            handler.removeCallbacks(autoCollapseRunnable);
            autoCollapseRunnable = null;
        }
    }
}
