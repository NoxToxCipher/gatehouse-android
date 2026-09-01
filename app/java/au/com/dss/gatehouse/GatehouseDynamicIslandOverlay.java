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
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Locale;

/**
 * GatehouseDynamicIslandOverlay — High-fidelity top cutout Dynamic Island & Live Capsule.
 * Morphs seamlessly around the camera punch-hole cutout with spring physics,
 * expanding on tap into an interactive cyber-glass telemetry HUD.
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
    private String currentSubtitle = "0.8km · Save 6.0¢";
    private int currentAccentColor = 0xFFF59E0B;
    private String currentNavAddress = "OOM Energy Kingston, 122 Kingston Rd, Kingston QLD";

    public static synchronized GatehouseDynamicIslandOverlay getInstance(Context ctx) {
        if (instance == null) {
            instance = new GatehouseDynamicIslandOverlay(ctx.getApplicationContext());
        }
        return instance;
    }

    private GatehouseDynamicIslandOverlay(Context ctx) {
        this.context = ctx;
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
        FrameLayout.LayoutParams rlp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        rlp.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        rlp.topMargin = dp(6);
        islandRoot.setLayoutParams(rlp);
        islandRoot.setVisibility(View.GONE);
        islandRoot.setElevation(dp(16));

        // Background Glass Pill
        GradientDrawable pillBg = new GradientDrawable();
        pillBg.setColor(0xF00A0E17);
        pillBg.setCornerRadius(dp(22));
        pillBg.setStroke(dp(1.5f), 0xFFF59E0B);
        islandRoot.setBackground(pillBg);
        islandRoot.setPadding(dp(12), dp(6), dp(12), dp(6));

        // 1. Compact Pill View
        compactView = new LinearLayout(parentContainer.getContext());
        compactView.setOrientation(LinearLayout.HORIZONTAL);
        compactView.setGravity(Gravity.CENTER_VERTICAL);
        compactView.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT));

        TextView tvEmoji = new TextView(parentContainer.getContext());
        tvEmoji.setId(View.generateViewId());
        tvEmoji.setText("⛽");
        tvEmoji.setTextSize(14f);
        tvEmoji.setPadding(0, 0, dp(6), 0);
        compactView.addView(tvEmoji);

        TextView tvText = new TextView(parentContainer.getContext());
        tvText.setId(View.generateViewId());
        tvText.setText("OOM 168.9¢ · 0.8km");
        tvText.setTextColor(0xFFFEF08A);
        tvText.setTextSize(11.5f);
        tvText.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
        compactView.addView(tvText);

        TextView tvBadge = new TextView(parentContainer.getContext());
        tvBadge.setId(View.generateViewId());
        tvBadge.setText("★ BEST");
        tvBadge.setTextColor(0xFF0F172A);
        tvBadge.setTextSize(8.5f);
        tvBadge.setTypeface(Typeface.DEFAULT_BOLD);
        tvBadge.setPadding(dp(5), dp(2), dp(5), dp(2));
        GradientDrawable bgBadge = new GradientDrawable();
        bgBadge.setColor(0xFF10B981);
        bgBadge.setCornerRadius(dp(4));
        tvBadge.setBackground(bgBadge);
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        blp.leftMargin = dp(6);
        tvBadge.setLayoutParams(blp);
        compactView.addView(tvBadge);

        islandRoot.addView(compactView);

        // 2. Expanded HUD View
        expandedView = new LinearLayout(parentContainer.getContext());
        expandedView.setOrientation(LinearLayout.VERTICAL);
        expandedView.setVisibility(View.GONE);
        expandedView.setLayoutParams(new FrameLayout.LayoutParams(
                dp(300), FrameLayout.LayoutParams.WRAP_CONTENT));

        islandRoot.addView(expandedView);

        // Click to toggle expand / collapse
        islandRoot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentState == IslandState.COMPACT_PILL) {
                    expandIsland();
                } else if (currentState == IslandState.EXPANDED_CARD) {
                    collapseIsland();
                }
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

        GradientDrawable pillBg = new GradientDrawable();
        pillBg.setColor(0xF40A0E17);
        pillBg.setCornerRadius(dp(currentState == IslandState.EXPANDED_CARD ? 16 : 22));
        pillBg.setStroke(dp(1.5f), currentAccentColor);
        islandRoot.setBackground(pillBg);

        // Update Compact View
        if (compactView != null && compactView.getChildCount() >= 3) {
            ((TextView) compactView.getChildAt(0)).setText(currentEmoji);
            ((TextView) compactView.getChildAt(1)).setText(currentTitle + " · 0.8km");
            ((TextView) compactView.getChildAt(1)).setTextColor(currentAccentColor);
        }

        // Build Expanded View
        if (expandedView != null) {
            expandedView.removeAllViews();
            expandedView.setPadding(dp(6), dp(4), dp(6), dp(4));

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
            btnClose.setTextSize(13f);
            btnClose.setPadding(dp(6), 0, dp(2), 0);
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
                    "💰 Save $3.60 on 60L fill vs 7-Eleven");
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
            btnNav.setPadding(dp(8), dp(5), dp(8), dp(5));
            GradientDrawable navBg = new GradientDrawable();
            navBg.setColor(currentAccentColor);
            navBg.setCornerRadius(dp(6));
            btnNav.setBackground(navBg);
            LinearLayout.LayoutParams nlp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            nlp.rightMargin = dp(4);
            btnNav.setLayoutParams(nlp);
            btnNav.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
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
            btnCollapse.setPadding(dp(8), dp(5), dp(8), dp(5));
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
        compactView.setVisibility(View.VISIBLE);
        expandedView.setVisibility(View.GONE);

        islandRoot.setVisibility(View.VISIBLE);
        islandRoot.setScaleX(0.2f);
        islandRoot.setScaleY(0.2f);
        islandRoot.setAlpha(0f);

        islandRoot.animate()
                .scaleX(1.0f)
                .scaleY(1.0f)
                .alpha(1.0f)
                .setDuration(350)
                .setInterpolator(new OvershootInterpolator(1.4f))
                .start();

        scheduleAutoCollapse(8000);
    }

    public void expandIsland() {
        if (islandRoot == null || currentState == IslandState.EXPANDED_CARD) return;
        currentState = IslandState.EXPANDED_CARD;
        cancelAutoCollapse();

        updateViewsData();
        compactView.setVisibility(View.GONE);
        expandedView.setVisibility(View.VISIBLE);

        islandRoot.animate()
                .scaleX(1.03f)
                .scaleY(1.03f)
                .setDuration(120)
                .withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        islandRoot.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start();
                    }
                })
                .start();

        scheduleAutoCollapse(12000);
    }

    public void collapseIsland() {
        if (islandRoot == null || currentState != IslandState.EXPANDED_CARD) return;
        currentState = IslandState.COMPACT_PILL;
        cancelAutoCollapse();

        expandedView.setVisibility(View.GONE);
        compactView.setVisibility(View.VISIBLE);
        updateViewsData();

        scheduleAutoCollapse(6000);
    }

    public void hideIsland() {
        if (islandRoot == null || currentState == IslandState.HIDDEN) return;
        currentState = IslandState.HIDDEN;
        cancelAutoCollapse();

        islandRoot.animate()
                .scaleX(0.2f)
                .scaleY(0.2f)
                .alpha(0f)
                .setDuration(250)
                .withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        islandRoot.setVisibility(View.GONE);
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
