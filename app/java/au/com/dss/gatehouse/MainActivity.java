package au.com.dss.gatehouse;

import android.R;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.view.TextureView;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.TimeZone;

/* loaded from: classes.dex */
public class MainActivity extends Activity implements SensorEventListener, LocationListener {
    private static final long HOLD_MS = 120000;
    private static final int MAX_HELD = 50;
    private static final int REQ_PERM_AUDIO = 2002;
    private static final int REQ_PERM_CAMERA = 2001;
    private static final int REQ_PERM_LOCATION = 2003;
    public static final int THEME_GOLD = 0;
    public static final int THEME_NVG = 2;
    public static final int THEME_RED = 1;
    public static final int THEME_VIOLET = 3;
    private static final int WATER_TARGET_ML = 2000;
    private static final long WELFARE_INTERVAL_MS = 5400000;
    private Sensor accelSensor;
    private DetailedCompassView activeCompassView;
    private HolographicCardView activeHoloCard;
    private HorizonLevelerView activeLevelerView;
    private File activeVoiceFile;
    private FluidAnimatedTabBarView animatedTabBar;
    private FluidAnimatedThemeBarView animatedThemeBar;
    private TextView banner;
    private TextView btnShareReport;
    private AnimatedChainBannerView chainBannerView;
    private ChronographView chronographView;
    private TextView compassDmsText;
    private TextView compassHeadingText;
    private TextView compassSectorText;
    private LinearLayout contactsContent;
    private SunConureFlightOverlayView conureOverlay;
    private TextView diagAmbientWeather;
    private TextView diagBatteryRuntime;
    private TextView diagOledPower;
    private LinearLayout diagStrip;
    private LinearLayout dock;
    private LinearLayout externalRow;
    private LinearLayout fireCard;
    private LinearLayout fireList;
    private TextView fireStatusChip;
    private TextView gpsAccuracyText;
    private TextView gpsAltitudeText;
    private TextView gpsCoordsText;
    private LinearLayout handbookContent;
    private LinearLayout internalBadgesRow;
    private long lastActivityTimeMs;
    private Location lastKnownLocation;
    private LocationManager locationManager;
    private Sensor magSensor;
    private LinearLayout modeBar;
    private int openedAt;
    private TextView page;
    private TextView pageTitle;
    private LinearLayout patrolContent;
    private LinearLayout pills;
    private TextView primary;
    private String rearCameraId;
    private LinearLayout root;
    private FrameLayout rootFrame;
    private Sensor rotationSensor;
    private SatellitePolarRadarView satelliteRadarView;
    private ScrollView scroll;
    private PulsingScrollIndicator scrollIndicator;
    private SensorManager sensorManager;
    private TextView tabContacts;
    private LinearLayout tabContainer;
    private TextView tabHandbook;
    private TextView tabPatrol;
    private TextView tabTools;
    private LinearLayout tileExternalFull;
    private LinearLayout tileExternalHalf;
    private LinearLayout tonight;
    private TextView tonightTitle;
    private LinearLayout toolsContent;
    private Vibrator vibrator;
    private long voiceRecordStart;
    private MediaRecorder voiceRecorder;
    private static final String[] EXTERNAL_CHOICES = {"External (Full)", "04A1B2C3D4E501", "External (Half)", "04A1B2C3D4E502"};
    private static final String[] EXTERNAL_OPTIONS = {"✓ Perimeter Secure: Fences intact & gates locked", "⚠️ Perimeter gate / padlock left unlocked", "⚠️ Boundary fence damage / wire mesh cut", "⚠️ Floodlights / perimeter security lighting dark", "⚠️ Suspicious vehicle / loitering outside boundary"};
    private static final String[] INTERNAL_LOTS = {"Lot 14", "04F1A2B3C4D5E6", "Lot 15", "04F2A3B4C5D6E7", "Lot 16", "04F3A4B5C6D7E8", "Lot 17", "04F4A5B6C7D8E9", "Lot 18", "04F5A6B7C8D9EA"};
    private static final String[] FIRE_POINTS = {"Lot 15 Pump House", "04E1F2A3B4C5D6", "Lot 16 Pump House (Outside)", "04E2F3A4B5C6D7", "Lot 16 Fire System (Inside)", "04E3F4A5B6C7D8", "Lot 17 Pump House", "04E4F5A6B7C8D9", "Lot 18 Pump House", "04E5F6A7B8C9DA"};
    private static final String[] PUMP_OPTIONS = {"✓ Pressure Normal (1,200 kPa In Spec)", "⚠️ Low Pressure Warning (< 1,000 kPa)", "⚠️ Jockey Pump Cycling Excessively", "⚠️ Diesel Booster Fuel Tank Below 75%", "⚠️ Minor Valve / Pipe Fitting Weep Noted"};
    private static final String[] SHUTDOWN_OPTIONS = {"✓ All Clear: Factory floor sealed & machinery isolated", "⚠️ Roller door / emergency exit unlocked (secured by guard)", "⚠️ High-bay lighting / plant machinery left powered on", "⚠️ Air compressor / extraction fans left running", "⚠️ Floor hazard / liquid spill noted"};
    private static final MapSector[] SITE_MAP_SECTORS = {new MapSector("NW", "North-West", "North-West Perimeter Fence"), new MapSector("GATE_A", "Gate A (Main)", "Gate A (Main Vehicle Entry)"), new MapSector("NE", "North-East", "North-East Fence (near Lot 14)"), new MapSector("LOT18", "Lot 18 (Yard)", "Lot 18 (Timber Yard / Dispatch)"), new MapSector("CENTRAL", "Central Yard", "Central Compound & Staging"), new MapSector("LOT14", "Lot 14 (Doors)", "Lot 14 (Door Assembly Floor)"), new MapSector("LOT17", "Lot 17 (Chem)", "Lot 17 (Adhesives & Chemicals)"), new MapSector("GATE_B", "Gate B / P16", "South Gate B & Lot 16 Pump House"), new MapSector("LOT15", "Lot 15 (Mill)", "Lot 15 (Timber Sawmill Floor)")};
    private int activeTheme = 0;
    private int currentTab = 0;
    private int colBg = -16777216;
    private int colPanel = -16249836;
    private int colPanel2 = -15722716;
    private int colPanel3 = -15195596;
    private int colLine = -14800064;
    private int colLineSubtle = -15590616;
    private int colAccent = -1726148;
    private int colAccentInk = -16777216;
    private int colAccentSoft = 451258684;
    private int colPale = -788742;
    private int colMuted = -7035976;
    private int colQuiet = -10785918;
    private int colEmerald = -15681151;
    private int colEmeraldSoft = 571521409;
    private int colCrimson = -1096636;
    private int colCrimsonSoft = 619660356;
    private int colCyan = -16337196;
    private int colCyanSoft = 604419796;
    private final HashMap<String, ArrayList<PressureRecord>> pressureHistory = new HashMap<>();
    private int headerTapCount = 0;
    private long lastHeaderTapMs = 0;
    private boolean isWelfareDialogShowing = false;
    private boolean isHardwareTorchOn = false;
    private int torchLevelPercent = 100;
    private boolean isStrobeActive = false;
    private boolean isSosActive = false;
    private final Handler lightHandler = new Handler();
    private float[] lastAccel = new float[3];
    private float[] lastMag = new float[3];
    private boolean hasAccel = false;
    private boolean hasMag = false;
    private float currentAzimuth = 0.0f;
    private float[] lastGravity = new float[3];
    private long lastChopTimestamp = 0;
    private int chopCount = 0;
    private long lastToggleCooldown = 0;
    private final ArrayList<TrustedPeer> trustedPeers = new ArrayList<>();
    private double curTempC = 14.8d;
    private double curFeelsLikeC = 13.9d;
    private double curUvIndex = 0.0d;
    private int curHumidity = 78;
    private double curDewPointC = 11.2d;
    private double curPressureHpa = 1021.2d;
    private double curWindSpeedKmh = 12.4d;
    private String curWindDir = "SSE (165°)";
    private double curWindGustKmh = 18.2d;
    private int waterIntakeMl = 750;
    private final ArrayList<Pending> pending = new ArrayList<>();
    private final Handler ticker = new Handler();
    private int taps = 100;
    private boolean isRecordingVoice = false;
    private final Runnable tick = new Runnable() { // from class: au.com.dss.gatehouse.MainActivity.4
        @Override // java.lang.Runnable
        public void run() {
            MainActivity.this.commitDue();
            MainActivity.this.refresh();
            if (MainActivity.this.chronographView != null) {
                MainActivity.this.chronographView.invalidate();
            }
            MainActivity.this.checkWelfareDue();
            MainActivity.this.updateDiagnostics();
            MainActivity.this.ticker.postDelayed(this, 1000L);
        }
    };

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public interface OnPhotoCapturedCallback {
        void onCaptured(Bitmap bitmap, String str);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public interface OnPressureChangedListener {
        void onPressureChanged(int i);
    }

    /* renamed from: -$$Nest$smnowMinutes, reason: not valid java name */
    static /* bridge */ /* synthetic */ int m110$$Nest$smnowMinutes() {
        return nowMinutes();
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class PressureRecord {
        int pressureKpa;
        int timeMinutes;

        PressureRecord(int i, int i2) {
            this.timeMinutes = i;
            this.pressureKpa = i2;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class TrustedPeer {
        String lastSeen;
        String licence;
        String name;

        TrustedPeer(String str, String str2, String str3) {
            this.name = str;
            this.licence = str2;
            this.lastSeen = str3;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class Pending {
        boolean checkpoint;
        long created;
        String label;
        int occurred;
        int taps;
        String text;
        int topic;
        String uid;

        private Pending() {
            this.label = "";
            this.uid = "";
            this.text = "";
        }
    }

    private static int nowMinutes() {
        long now = System.currentTimeMillis();
        return (int) ((now + TimeZone.getDefault().getOffset(now)) / 60000);
    }

    @Override // android.app.Activity
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.lastActivityTimeMs = SystemClock.elapsedRealtime();
        this.vibrator = (Vibrator) getSystemService("vibrator");
        this.trustedPeers.add(new TrustedPeer("Officer M. Taylor", "LIC #55891", "Today 05:58 AM (Gate A)"));
        initSensorsAndGps();
        initCameraManager();
        buildUi();
        loadPending();
        startShift();
        commitAll();
        updateDiagnostics();
        this.ticker.postDelayed(this.tick, 1000L);
        SharedPreferences sharedPreferences = getSharedPreferences("gatehouse_prefs", 0);
        int i = sharedPreferences.getInt("launch_count", 0) + 1;
        sharedPreferences.edit().putInt("launch_count", i).apply();
        if (i % 17 == 0) {
            this.rootFrame.postDelayed(new Runnable() { // from class: au.com.dss.gatehouse.MainActivity.1
                @Override // java.lang.Runnable
                public void run() {
                    MainActivity.this.triggerSunConureFlight();
                }
            }, 1200L);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void hapticClick() {
        if (this.vibrator == null) {
            return;
        }
        try {
            if (Build.VERSION.SDK_INT >= 29) {
                this.vibrator.vibrate(VibrationEffect.createPredefined(0));
            } else {
                this.vibrator.vibrate(18L);
            }
        } catch (Exception e) {
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void hapticHeavyClick() {
        if (this.vibrator == null) {
            return;
        }
        try {
            if (Build.VERSION.SDK_INT >= 29) {
                this.vibrator.vibrate(VibrationEffect.createPredefined(5));
            } else {
                this.vibrator.vibrate(40L);
            }
        } catch (Exception e) {
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void hapticDoublePulse() {
        if (this.vibrator == null) {
            return;
        }
        try {
            this.vibrator.vibrate(VibrationEffect.createWaveform(new long[]{0, 25, 45, 35}, -1));
        } catch (Exception e) {
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void hapticSealThud() {
        if (this.vibrator == null) {
            return;
        }
        try {
            this.vibrator.vibrate(VibrationEffect.createWaveform(new long[]{0, 80, 80, 140}, -1));
        } catch (Exception e) {
        }
    }

    private void initCameraManager() {
        CameraManager cameraManager = (CameraManager) getSystemService("camera");
        try {
            String[] cameraIdList = cameraManager.getCameraIdList();
            int length = cameraIdList.length;
            int i = 0;
            while (true) {
                if (i >= length) {
                    break;
                }
                String str = cameraIdList[i];
                Integer num = (Integer) cameraManager.getCameraCharacteristics(str).get(CameraCharacteristics.LENS_FACING);
                if (num == null || num.intValue() != 1) {
                    i++;
                } else {
                    this.rearCameraId = str;
                    break;
                }
            }
            if (this.rearCameraId == null && cameraManager.getCameraIdList().length > 0) {
                this.rearCameraId = cameraManager.getCameraIdList()[0];
            }
        } catch (Exception e) {
        }
    }

    private void initSensorsAndGps() {
        this.sensorManager = (SensorManager) getSystemService("sensor");
        if (this.sensorManager != null) {
            this.rotationSensor = this.sensorManager.getDefaultSensor(11);
            this.accelSensor = this.sensorManager.getDefaultSensor(1);
            this.magSensor = this.sensorManager.getDefaultSensor(2);
        }
        this.locationManager = (LocationManager) getSystemService("location");
    }

    @Override // android.app.Activity
    protected void onResume() {
        super.onResume();
        registerSensors();
        requestGpsUpdates();
        updateDiagnostics();
    }

    private void registerSensors() {
        if (this.sensorManager != null) {
            if (this.rotationSensor != null) {
                this.sensorManager.registerListener(this, this.rotationSensor, 2);
            }
            if (this.accelSensor != null) {
                this.sensorManager.registerListener(this, this.accelSensor, 1);
            }
            if (this.magSensor != null) {
                this.sensorManager.registerListener(this, this.magSensor, 2);
            }
        }
    }

    private void unregisterSensors() {
        if (this.sensorManager != null) {
            this.sensorManager.unregisterListener(this);
        }
    }

    private void requestGpsUpdates() {
        if (checkSelfPermission("android.permission.ACCESS_FINE_LOCATION") != 0) {
            requestPermissions(new String[]{"android.permission.ACCESS_FINE_LOCATION", "android.permission.ACCESS_COARSE_LOCATION"}, REQ_PERM_LOCATION);
            return;
        }
        if (this.locationManager != null) {
            try {
                if (!this.locationManager.isProviderEnabled("gps")) {
                    if (this.locationManager.isProviderEnabled("network")) {
                        this.locationManager.requestLocationUpdates("network", 2000L, 1.0f, this);
                        Location lastKnownLocation = this.locationManager.getLastKnownLocation("network");
                        if (lastKnownLocation != null) {
                            updateGpsDisplay(lastKnownLocation);
                        }
                    }
                } else {
                    this.locationManager.requestLocationUpdates("gps", 2000L, 1.0f, this);
                    Location lastKnownLocation2 = this.locationManager.getLastKnownLocation("gps");
                    if (lastKnownLocation2 != null) {
                        updateGpsDisplay(lastKnownLocation2);
                    }
                }
            } catch (Exception e) {
            }
        }
    }

    private void stopGpsUpdates() {
        if (this.locationManager != null) {
            try {
                this.locationManager.removeUpdates(this);
            } catch (Exception e) {
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void registerActivity() {
        this.lastActivityTimeMs = SystemClock.elapsedRealtime();
        if (this.chronographView != null) {
            this.chronographView.invalidate();
        }
    }

    private void applyThemeTokens() {
        if (this.animatedTabBar != null) {
            this.animatedTabBar.invalidate();
        }
        switch (this.activeTheme) {
            case 1:
                this.colBg = -16777216;
                this.colPanel = -15924477;
                this.colPanel2 = -15268346;
                this.colPanel3 = -14546679;
                this.colLine = -12774894;
                this.colLineSubtle = -14415350;
                this.colAccent = -52429;
                this.colAccentInk = -16777216;
                this.colAccentSoft = 587150131;
                this.colPale = -30070;
                this.colMuted = -3910315;
                this.colQuiet = -8768717;
                this.colEmerald = -43691;
                this.colEmeraldSoft = 654267733;
                this.colCrimson = -61167;
                this.colCrimsonSoft = 872354065;
                this.colCyan = -48060;
                this.colCyanSoft = 687817796;
                return;
            case 2:
                this.colBg = -16777216;
                this.colPanel = -16641530;
                this.colPanel2 = -16507382;
                this.colPanel3 = -16373233;
                this.colLine = -16104424;
                this.colLineSubtle = -16372976;
                this.colAccent = -16711834;
                this.colAccentInk = -16777216;
                this.colAccentSoft = 570490726;
                this.colPale = -2031640;
                this.colMuted = -11149961;
                this.colQuiet = -14514108;
                this.colEmerald = -16711834;
                this.colEmeraldSoft = 637599590;
                this.colCrimson = -43691;
                this.colCrimsonSoft = 620713301;
                this.colCyan = -16711732;
                this.colCyanSoft = 604045260;
                return;
            case 3:
                this.colBg = -16777216;
                this.colPanel = -16055276;
                this.colPanel2 = -15464414;
                this.colPanel3 = -14742475;
                this.colLine = -13298343;
                this.colLineSubtle = -14545862;
                this.colAccent = -4160260;
                this.colAccentInk = -16777216;
                this.colAccentSoft = 583042300;
                this.colPale = -792321;
                this.colMuted = -5745161;
                this.colQuiet = -9756248;
                this.colEmerald = -15681151;
                this.colEmeraldSoft = 571521409;
                this.colCrimson = -770210;
                this.colCrimsonSoft = 619986782;
                this.colCyan = -13058568;
                this.colCyanSoft = 607698424;
                return;
            default:
                this.colBg = -16777216;
                this.colPanel = -16249836;
                this.colPanel2 = -15722716;
                this.colPanel3 = -15195596;
                this.colLine = -14800064;
                this.colLineSubtle = -15590616;
                this.colAccent = -1726148;
                this.colAccentInk = -16777216;
                this.colAccentSoft = 451258684;
                this.colPale = -788742;
                this.colMuted = -7035976;
                this.colQuiet = -10785918;
                this.colEmerald = -15681151;
                this.colEmeraldSoft = 571521409;
                this.colCrimson = -1096636;
                this.colCrimsonSoft = 619660356;
                this.colCyan = -16337196;
                this.colCyanSoft = 604419796;
                return;
        }
    }

    private void buildUi() {
        applyThemeTokens();
        this.rootFrame = new FrameLayout(this);
        this.rootFrame.setBackgroundColor(this.colBg);
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(1);
        linearLayout.setBackgroundColor(this.colBg);
        linearLayout.setFitsSystemWindows(true);
        linearLayout.setPadding(dp(16), dp(12), dp(16), 0);
        linearLayout.addView(modeBar());
        linearLayout.addView(buildDiagnosticsStrip());
        linearLayout.addView(buildTabBar());
        this.scroll = new ScrollView(this);
        this.scroll.setBackgroundColor(this.colBg);
        this.scroll.setVerticalScrollBarEnabled(false);
        this.scroll.setLayoutParams(new LinearLayout.LayoutParams(-1, 0, 1.0f));
        this.root = new LinearLayout(this);
        this.root.setOrientation(1);
        this.root.setPadding(0, dp(4), 0, dp(28));
        this.patrolContent = new LinearLayout(this);
        this.patrolContent.setOrientation(1);
        this.patrolContent.addView(headerCard());
        this.patrolContent.addView(buildChronographSection());
        this.chainBannerView = new AnimatedChainBannerView(this);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, dp(44));
        layoutParams.topMargin = dp(10);
        layoutParams.bottomMargin = dp(4);
        this.chainBannerView.setLayoutParams(layoutParams);
        this.patrolContent.addView(this.chainBannerView);
        this.pills = new LinearLayout(this);
        this.pills.setOrientation(0);
        this.pills.setPadding(0, dp(4), 0, dp(6));
        this.patrolContent.addView(this.pills);
        this.banner = new TextView(this);
        this.banner.setTextSize(13.0f);
        this.banner.setTextColor(this.colAccent);
        this.banner.setPadding(dp(14), dp(12), dp(14), dp(12));
        this.banner.setBackground(rounded(this.colPanel2, dp(12)));
        this.banner.setVisibility(8);
        LinearLayout.LayoutParams layoutParams2 = new LinearLayout.LayoutParams(-1, -2);
        layoutParams2.topMargin = dp(6);
        layoutParams2.bottomMargin = dp(8);
        this.banner.setLayoutParams(layoutParams2);
        this.patrolContent.addView(this.banner);
        this.patrolContent.addView(sectionHeader("External Patrols", null));
        this.externalRow = new LinearLayout(this);
        this.externalRow.setOrientation(0);
        this.externalRow.setPadding(0, dp(2), 0, dp(8));
        this.tileExternalFull = patrolActionCard("External (Full)", EXTERNAL_CHOICES[1], true);
        this.tileExternalHalf = patrolActionCard("External (Half)", EXTERNAL_CHOICES[3], false);
        this.externalRow.addView(this.tileExternalFull);
        this.externalRow.addView(this.tileExternalHalf);
        this.patrolContent.addView(this.externalRow);
        this.patrolContent.addView(sectionHeader("Internal Factory Floors (Lots 14–18)", null));
        this.internalBadgesRow = new LinearLayout(this);
        this.internalBadgesRow.setOrientation(0);
        this.internalBadgesRow.setPadding(0, dp(2), 0, dp(10));
        int i = 0;
        while (i < INTERNAL_LOTS.length) {
            this.internalBadgesRow.addView(lotBadge(INTERNAL_LOTS[i], INTERNAL_LOTS[i + 1], i == INTERNAL_LOTS.length - 2));
            i += 2;
        }
        this.patrolContent.addView(this.internalBadgesRow);
        LinearLayout linearLayout2 = new LinearLayout(this);
        linearLayout2.setOrientation(0);
        linearLayout2.setGravity(16);
        linearLayout2.setPadding(0, dp(10), 0, dp(6));
        TextView textView = new TextView(this);
        textView.setText("FIRE & PUMP SYSTEMS (5) · 1,200 kPa OPTIMAL");
        textView.setTextColor(this.colQuiet);
        textView.setTextSize(10.0f);
        textView.setTypeface(Typeface.DEFAULT_BOLD);
        textView.setLetterSpacing(0.12f);
        textView.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));
        linearLayout2.addView(textView);
        this.fireStatusChip = new TextView(this);
        this.fireStatusChip.setText("0/5 CHECKED");
        this.fireStatusChip.setTextColor(this.colMuted);
        this.fireStatusChip.setTextSize(10.0f);
        this.fireStatusChip.setTypeface(Typeface.MONOSPACE);
        this.fireStatusChip.setPadding(dp(6), dp(2), dp(6), dp(2));
        this.fireStatusChip.setBackground(rounded(this.colPanel2, dp(6)));
        linearLayout2.addView(this.fireStatusChip);
        this.patrolContent.addView(linearLayout2);
        this.fireCard = new LinearLayout(this);
        this.fireCard.setOrientation(1);
        this.fireCard.setBackground(rounded(this.colPanel, dp(14)));
        this.fireCard.setPadding(dp(12), dp(8), dp(12), dp(8));
        this.fireList = new LinearLayout(this);
        this.fireList.setOrientation(1);
        int i2 = 0;
        while (i2 < FIRE_POINTS.length) {
            this.fireList.addView(fireCompactRow(FIRE_POINTS[i2], FIRE_POINTS[i2 + 1], i2 == FIRE_POINTS.length - 2));
            i2 += 2;
        }
        this.fireCard.addView(this.fireList);
        this.patrolContent.addView(this.fireCard);
        this.patrolContent.addView(sectionHeader("Rapid Evidence Dock", null));
        this.dock = buildCaptureDock();
        this.patrolContent.addView(this.dock);
        this.patrolContent.addView(tonightLabel());
        this.tonight = new LinearLayout(this);
        this.tonight.setOrientation(1);
        this.tonight.setPadding(0, dp(4), 0, dp(20));
        this.patrolContent.addView(this.tonight);
        this.primary = new TextView(this);
        this.primary.setTextSize(15.0f);
        this.primary.setTypeface(Typeface.DEFAULT_BOLD);
        this.primary.setGravity(17);
        this.primary.setPadding(dp(16), dp(18), dp(16), dp(18));
        this.patrolContent.addView(this.primary);
        this.pageTitle = label("06:05 MORNING HANDOVER REPORT");
        this.pageTitle.setPadding(0, dp(24), 0, dp(8));
        this.pageTitle.setVisibility(8);
        this.patrolContent.addView(this.pageTitle);
        this.page = new TextView(this);
        this.page.setTextColor(this.colPale);
        this.page.setTextSize(10.0f);
        this.page.setTypeface(Typeface.MONOSPACE);
        this.page.setBackground(rounded(this.colPanel, dp(14)));
        this.page.setPadding(dp(14), dp(14), dp(14), dp(14));
        this.page.setVisibility(8);
        this.patrolContent.addView(this.page);
        this.btnShareReport = new TextView(this);
        this.btnShareReport.setText("📤 SHARE MORNING HANDOVER REPORT");
        this.btnShareReport.setTextColor(this.colAccentInk);
        this.btnShareReport.setTextSize(14.0f);
        this.btnShareReport.setTypeface(Typeface.DEFAULT_BOLD);
        this.btnShareReport.setGravity(17);
        this.btnShareReport.setPadding(dp(16), dp(16), dp(16), dp(16));
        this.btnShareReport.setBackground(pressable(this.colAccent, dp(16)));
        this.btnShareReport.setVisibility(8);
        LinearLayout.LayoutParams layoutParams3 = new LinearLayout.LayoutParams(-1, -2);
        layoutParams3.topMargin = dp(12);
        this.btnShareReport.setLayoutParams(layoutParams3);
        this.btnShareReport.setOnClickListener(new View.OnClickListener() { // from class: au.com.dss.gatehouse.MainActivity.2
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                MainActivity.this.hapticHeavyClick();
                MainActivity.this.shareHandoverReport();
            }
        });
        this.patrolContent.addView(this.btnShareReport);
        this.root.addView(this.patrolContent);
        this.contactsContent = buildContactsTab();
        this.root.addView(this.contactsContent);
        this.handbookContent = buildHandbookTab();
        this.root.addView(this.handbookContent);
        this.toolsContent = buildToolsTab();
        this.root.addView(this.toolsContent);
        updateTabSelection(this.currentTab);
        this.scroll.addView(this.root);
        linearLayout.addView(this.scroll);
        this.rootFrame.addView(linearLayout);
        this.scrollIndicator = new PulsingScrollIndicator(this);
        FrameLayout.LayoutParams layoutParams4 = new FrameLayout.LayoutParams(dp(4), -1);
        layoutParams4.gravity = 8388613;
        this.scrollIndicator.setLayoutParams(layoutParams4);
        this.rootFrame.addView(this.scrollIndicator);
        this.scroll.setOnScrollChangeListener(new View.OnScrollChangeListener() { // from class: au.com.dss.gatehouse.MainActivity.3
            @Override // android.view.View.OnScrollChangeListener
            public void onScrollChange(View view, int i3, int i4, int i5, int i6) {
                int height = MainActivity.this.scroll.getChildAt(0).getHeight() - MainActivity.this.scroll.getHeight();
                MainActivity.this.scrollIndicator.setScrollProgress(height > 0 ? (i4 * 1.0f) / height : 0.0f);
            }
        });
        this.conureOverlay = new SunConureFlightOverlayView(this);
        this.conureOverlay.setLayoutParams(new FrameLayout.LayoutParams(-1, -1));
        this.rootFrame.addView(this.conureOverlay);
        setContentView(this.rootFrame);
    }

    @Override // android.app.Activity
    protected void onPause() {
        super.onPause();
        stopLightingModes();
        unregisterSensors();
        stopGpsUpdates();
        commitAll();
    }

    @Override // android.app.Activity
    protected void onDestroy() {
        super.onDestroy();
        this.ticker.removeCallbacks(this.tick);
        stopLightingModes();
        unregisterSensors();
        stopGpsUpdates();
        if (this.voiceRecorder != null) {
            try {
                this.voiceRecorder.release();
            } catch (Exception e) {
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class PulsingScrollIndicator extends View {
        private final Paint glowPaint;
        private float pulseAlpha;
        private float scrollPct;
        private final Paint thumbPaint;
        private final Paint trackPaint;

        public PulsingScrollIndicator(Context context) {
            super(context);
            this.trackPaint = new Paint(1);
            this.thumbPaint = new Paint(1);
            this.glowPaint = new Paint(1);
            this.scrollPct = 0.0f;
            this.pulseAlpha = 0.5f;
            this.trackPaint.setStyle(Paint.Style.FILL);
            this.thumbPaint.setStyle(Paint.Style.FILL);
            this.glowPaint.setStyle(Paint.Style.FILL);
            ValueAnimator ofFloat = ValueAnimator.ofFloat(0.35f, 0.95f);
            ofFloat.setDuration(1600L);
            ofFloat.setRepeatMode(2);
            ofFloat.setRepeatCount(-1);
            ofFloat.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: au.com.dss.gatehouse.MainActivity.PulsingScrollIndicator.1
                @Override // android.animation.ValueAnimator.AnimatorUpdateListener
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    PulsingScrollIndicator.this.pulseAlpha = ((Float) valueAnimator.getAnimatedValue()).floatValue();
                    PulsingScrollIndicator.this.invalidate();
                }
            });
            ofFloat.start();
        }

        public void setScrollProgress(float f) {
            this.scrollPct = Math.max(0.0f, Math.min(1.0f, f));
            invalidate();
        }

        @Override // android.view.View
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            int width = getWidth();
            int height = getHeight();
            if (width <= 0 || height <= 0) {
                return;
            }
            this.trackPaint.setColor(MainActivity.this.colLineSubtle);
            float f = width;
            float f2 = height;
            canvas.drawRect(0.0f, 0.0f, f, f2, this.trackPaint);
            float dp = MainActivity.this.dp(44);
            float f3 = this.scrollPct * (f2 - dp);
            this.glowPaint.setColor(MainActivity.this.colAccent);
            this.glowPaint.setAlpha((int) (this.pulseAlpha * 90.0f));
            float f4 = dp + f3;
            canvas.drawRoundRect(new RectF(0.0f, f3 - MainActivity.this.dp(4), f, MainActivity.this.dp(4) + f4), MainActivity.this.dp(2), MainActivity.this.dp(2), this.glowPaint);
            this.thumbPaint.setColor(MainActivity.this.colAccent);
            this.thumbPaint.setAlpha((int) (this.pulseAlpha * 255.0f));
            canvas.drawRoundRect(new RectF(0.0f, f3, f, f4), MainActivity.this.dp(2), MainActivity.this.dp(2), this.thumbPaint);
        }
    }

    private View modeBar() {
        this.animatedThemeBar = new FluidAnimatedThemeBarView(this);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, dp(38));
        layoutParams.bottomMargin = dp(6);
        this.animatedThemeBar.setLayoutParams(layoutParams);
        return this.animatedThemeBar;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class DetailedCompassView extends View {
        private final Paint bezelPaint;
        private final Paint centerHub;
        private final Paint needleNorth;
        private final Paint needleSouth;
        private final Path pathN;
        private final Path pathS;
        private final Paint textPaint;
        private final Paint tickPaint;

        public DetailedCompassView(Context context) {
            super(context);
            this.bezelPaint = new Paint(1);
            this.tickPaint = new Paint(1);
            this.textPaint = new Paint(1);
            this.needleNorth = new Paint(1);
            this.needleSouth = new Paint(1);
            this.centerHub = new Paint(1);
            this.pathN = new Path();
            this.pathS = new Path();
            this.bezelPaint.setStyle(Paint.Style.STROKE);
            this.bezelPaint.setStrokeWidth(MainActivity.this.dp(2));
            this.tickPaint.setStyle(Paint.Style.STROKE);
            this.tickPaint.setStrokeCap(Paint.Cap.ROUND);
            this.textPaint.setTextAlign(Paint.Align.CENTER);
            this.textPaint.setTypeface(Typeface.create(Typeface.MONOSPACE, 1));
            this.needleNorth.setStyle(Paint.Style.FILL);
            this.needleSouth.setStyle(Paint.Style.FILL);
            this.centerHub.setStyle(Paint.Style.FILL);
        }

        @Override // android.view.View
        protected void onDraw(Canvas canvas) {
            int dp;
            int i;
            int i2;
            String valueOf;
            int i3;
            int i4;
            int dp2;
            super.onDraw(canvas);
            int width = getWidth();
            int height = getHeight();
            if (width <= 0 || height <= 0) {
                return;
            }
            float f = width / 2.0f;
            float f2 = height / 2.0f;
            float min = (Math.min(width, height) / 2.0f) - MainActivity.this.dp(12);
            this.bezelPaint.setColor(MainActivity.this.colLineSubtle);
            canvas.drawCircle(f, f2, min, this.bezelPaint);
            this.bezelPaint.setColor(MainActivity.this.colLine);
            int i5 = 10;
            canvas.drawCircle(f, f2, min - MainActivity.this.dp(10), this.bezelPaint);
            canvas.save();
            canvas.rotate(-MainActivity.this.currentAzimuth, f, f2);
            int i6 = 0;
            while (i6 < 360) {
                double radians = Math.toRadians(i6 - 90);
                boolean z = i6 % 90 == 0;
                boolean z2 = i6 % 30 == 0;
                if (z) {
                    dp = MainActivity.this.dp(i5);
                } else {
                    MainActivity mainActivity = MainActivity.this;
                    dp = z2 ? mainActivity.dp(6) : mainActivity.dp(3);
                }
                float dp3 = min - MainActivity.this.dp(i5);
                float f3 = dp3 - dp;
                double d = f;
                double d2 = dp3;
                float cos = (float) (d + (Math.cos(radians) * d2));
                double d3 = f2;
                float sin = (float) (d3 + (Math.sin(radians) * d2));
                int i7 = i6;
                double d4 = f3;
                float cos2 = (float) (d + (Math.cos(radians) * d4));
                float sin2 = (float) (d3 + (Math.sin(radians) * d4));
                Paint paint = this.tickPaint;
                if (z) {
                    MainActivity mainActivity2 = MainActivity.this;
                    i = i7 == 0 ? mainActivity2.colCrimson : mainActivity2.colAccent;
                } else {
                    i = MainActivity.this.colQuiet;
                }
                paint.setColor(i);
                this.tickPaint.setStrokeWidth(MainActivity.this.dp(z ? 2 : 1));
                canvas.drawLine(cos, sin, cos2, sin2, this.tickPaint);
                if (z || z2) {
                    double dp4 = f3 - MainActivity.this.dp(9);
                    float cos3 = (float) (d + (Math.cos(radians) * dp4));
                    float sin3 = ((float) (d3 + (Math.sin(radians) * dp4))) + MainActivity.this.dp(3);
                    if (i7 == 0) {
                        valueOf = "N";
                        i2 = i7;
                    } else {
                        i2 = i7;
                        if (i2 == 90) {
                            valueOf = "E";
                        } else if (i2 == 180) {
                            valueOf = "S";
                        } else {
                            valueOf = i2 == 270 ? "W" : String.valueOf(i2);
                        }
                    }
                    Paint paint2 = this.textPaint;
                    if (i2 == 0) {
                        i3 = MainActivity.this.colCrimson;
                    } else {
                        MainActivity mainActivity3 = MainActivity.this;
                        i3 = z ? mainActivity3.colPale : mainActivity3.colMuted;
                    }
                    paint2.setColor(i3);
                    Paint paint3 = this.textPaint;
                    if (z) {
                        i4 = 10;
                        dp2 = MainActivity.this.dp(10);
                    } else {
                        i4 = 10;
                        dp2 = MainActivity.this.dp(8);
                    }
                    paint3.setTextSize(dp2);
                    canvas.drawText(valueOf, cos3, sin3, this.textPaint);
                } else {
                    i2 = i7;
                    i4 = 10;
                }
                i6 = i2 + 5;
                i5 = i4;
            }
            canvas.restore();
            float dp5 = min - MainActivity.this.dp(26);
            float dp6 = MainActivity.this.dp(7);
            this.pathN.reset();
            this.pathN.moveTo(f, f2 - dp5);
            float f4 = f + dp6;
            this.pathN.lineTo(f4, f2);
            this.pathN.lineTo(f, f2 - MainActivity.this.dp(4));
            float f5 = f - dp6;
            this.pathN.lineTo(f5, f2);
            this.pathN.close();
            this.needleNorth.setColor(MainActivity.this.colCrimson);
            canvas.drawPath(this.pathN, this.needleNorth);
            this.pathS.reset();
            this.pathS.moveTo(f, dp5 + f2);
            this.pathS.lineTo(f4, f2);
            this.pathS.lineTo(f, MainActivity.this.dp(4) + f2);
            this.pathS.lineTo(f5, f2);
            this.pathS.close();
            this.needleSouth.setColor(MainActivity.this.colPale);
            canvas.drawPath(this.pathS, this.needleSouth);
            this.centerHub.setColor(MainActivity.this.colBg);
            canvas.drawCircle(f, f2, MainActivity.this.dp(6), this.centerHub);
            this.centerHub.setColor(MainActivity.this.colAccent);
            canvas.drawCircle(f, f2, MainActivity.this.dp(3), this.centerHub);
        }
    }

    @Override // android.hardware.SensorEventListener
    public void onSensorChanged(SensorEvent sensorEvent) {
        float f;
        if (sensorEvent.sensor.getType() == 11) {
            float[] fArr = new float[9];
            SensorManager.getRotationMatrixFromVector(fArr, sensorEvent.values);
            float[] ori = new float[3];
            SensorManager.getOrientation(fArr, ori);
            f = (float) Math.toDegrees(ori[0]);
        } else {
            if (sensorEvent.sensor.getType() == 1) {
                System.arraycopy(sensorEvent.values, 0, this.lastAccel, 0, 3);
                this.hasAccel = true;
                if (this.activeHoloCard != null) {
                    this.activeHoloCard.invalidate();
                }
                if (this.activeLevelerView != null) {
                    this.activeLevelerView.invalidate();
                }
                this.lastGravity[0] = (this.lastGravity[0] * 0.8f) + (sensorEvent.values[0] * 0.19999999f);
                this.lastGravity[1] = (this.lastGravity[1] * 0.8f) + (sensorEvent.values[1] * 0.19999999f);
                this.lastGravity[2] = (this.lastGravity[2] * 0.8f) + (sensorEvent.values[2] * 0.19999999f);
                float f2 = sensorEvent.values[0] - this.lastGravity[0];
                float f3 = sensorEvent.values[1] - this.lastGravity[1];
                float f4 = sensorEvent.values[2] - this.lastGravity[2];
                float sqrt = (float) Math.sqrt((f2 * f2) + (f3 * f3) + (f4 * f4));
                long elapsedRealtime = SystemClock.elapsedRealtime();
                if (sqrt > 13.0f && elapsedRealtime - this.lastToggleCooldown > 900) {
                    if (this.chopCount == 0) {
                        this.chopCount = 1;
                        this.lastChopTimestamp = elapsedRealtime;
                    } else if (this.chopCount == 1 && elapsedRealtime - this.lastChopTimestamp >= 120 && elapsedRealtime - this.lastChopTimestamp <= 800) {
                        this.chopCount = 0;
                        this.lastToggleCooldown = elapsedRealtime;
                        hapticDoublePulse();
                        toggleHardwareTorch();
                        this.banner.setText(this.isHardwareTorchOn ? "🔦 Double-Chop: Torch ON" : "🔦 Double-Chop: Torch OFF");
                        this.banner.setVisibility(0);
                    }
                }
                if (this.chopCount == 1 && elapsedRealtime - this.lastChopTimestamp > 800) {
                    this.chopCount = 0;
                }
            } else if (sensorEvent.sensor.getType() == 2) {
                System.arraycopy(sensorEvent.values, 0, this.lastMag, 0, 3);
                this.hasMag = true;
            }
            f = 0.0f;
        }
        if (this.rotationSensor == null && this.hasAccel && this.hasMag) {
            float[] fArr2 = new float[9];
            if (SensorManager.getRotationMatrix(fArr2, new float[9], this.lastAccel, this.lastMag)) {
                float[] ori2 = new float[3];
                SensorManager.getOrientation(fArr2, ori2);
                f = (float) Math.toDegrees(ori2[0]);
            }
        }
        if (f < 0.0f) {
            f += 360.0f;
        }
        this.currentAzimuth = f;
        updateCompassDisplay(f);
    }

    @Override // android.hardware.SensorEventListener
    public void onAccuracyChanged(Sensor sensor, int i) {
    }

    private void updateCompassDisplay(float f) {
        if (this.activeCompassView != null) {
            this.activeCompassView.invalidate();
        }
        if (this.compassHeadingText != null) {
            int i = (int) f;
            this.compassHeadingText.setText(String.format(Locale.US, "%03d° %s", Integer.valueOf(i), getCardinal(i)));
        }
        if (this.compassSectorText != null) {
            this.compassSectorText.setText("📍 Facing: " + getHumeSector((int) f));
        }
    }

    private String getCardinal(int i) {
        if (i >= 338 || i < 23) {
            return "N";
        }
        return (i < 23 || i >= 68) ? (i < 68 || i >= 113) ? (i < 113 || i >= 158) ? (i < 158 || i >= 203) ? (i < 203 || i >= 248) ? (i < 248 || i >= 293) ? "NW" : "W" : "SW" : "S" : "SE" : "E" : "NE";
    }

    private String getHumeSector(int i) {
        if (i >= 315 || i < 45) {
            return "Gate A · North Boundary (Kingston Rd Entry)";
        }
        return (i < 45 || i >= 135) ? (i < 135 || i >= 225) ? "Lots 17 & 18 · West Boundary (Timber Yard & Chem)" : "Gate B · South Boundary (Lot 16 Factory & Pump 16)" : "Lots 14 & 15 · East Boundary (Sawmill & Assembly)";
    }

    private LinearLayout buildDiagnosticsStrip() {
        this.diagStrip = new LinearLayout(this);
        this.diagStrip.setOrientation(0);
        this.diagStrip.setGravity(16);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, -2);
        layoutParams.bottomMargin = dp(6);
        this.diagStrip.setLayoutParams(layoutParams);
        this.diagOledPower = new TextView(this);
        this.diagOledPower.setText("⚡ 0.14W OLED");
        this.diagOledPower.setTextColor(this.colEmerald);
        this.diagOledPower.setTextSize(9.5f);
        this.diagOledPower.setSingleLine(true);
        this.diagOledPower.setGravity(17);
        this.diagOledPower.setTypeface(Typeface.create(Typeface.MONOSPACE, 1));
        this.diagStrip.addView(buildTelemetryChip(this.diagOledPower, new View.OnClickListener() { // from class: au.com.dss.gatehouse.MainActivity.5
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                MainActivity.this.hapticClick();
                MainActivity.this.showPowerDiagnosticsDialog();
            }
        }));
        this.diagAmbientWeather = new TextView(this);
        this.diagAmbientWeather.setText(String.format(Locale.US, "🌤️ %.1f°C KGTN", Double.valueOf(this.curTempC)));
        this.diagAmbientWeather.setTextColor(this.colCyan);
        this.diagAmbientWeather.setTextSize(9.5f);
        this.diagAmbientWeather.setSingleLine(true);
        this.diagAmbientWeather.setGravity(17);
        this.diagAmbientWeather.setTypeface(Typeface.create(Typeface.MONOSPACE, 1));
        this.diagStrip.addView(buildTelemetryChip(this.diagAmbientWeather, new View.OnClickListener() { // from class: au.com.dss.gatehouse.MainActivity.6
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                MainActivity.this.hapticClick();
                MainActivity.this.showWeatherTelemetryDialog();
            }
        }));
        this.diagBatteryRuntime = new TextView(this);
        this.diagBatteryRuntime.setText("🔋 100% (22h)");
        this.diagBatteryRuntime.setTextColor(this.colAccent);
        this.diagBatteryRuntime.setTextSize(9.5f);
        this.diagBatteryRuntime.setSingleLine(true);
        this.diagBatteryRuntime.setGravity(17);
        this.diagBatteryRuntime.setTypeface(Typeface.create(Typeface.MONOSPACE, 1));
        this.diagStrip.addView(buildTelemetryChip(this.diagBatteryRuntime, new View.OnClickListener() { // from class: au.com.dss.gatehouse.MainActivity.7
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                MainActivity.this.hapticClick();
                MainActivity.this.showBatteryTelemetryDialog();
            }
        }));
        return this.diagStrip;
    }

    private LinearLayout buildTelemetryChip(TextView textView, View.OnClickListener onClickListener) {
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(0);
        linearLayout.setGravity(17);
        linearLayout.setBackground(rounded(this.colPanel, dp(10)));
        linearLayout.setPadding(dp(6), dp(6), dp(6), dp(6));
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(0, -2, 1.0f);
        layoutParams.leftMargin = dp(2);
        layoutParams.rightMargin = dp(2);
        linearLayout.setLayoutParams(layoutParams);
        linearLayout.addView(textView);
        linearLayout.setOnClickListener(onClickListener);
        return linearLayout;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateDiagnostics() {
        try {
            Intent bIntent = registerReceiver(null, new IntentFilter("android.intent.action.BATTERY_CHANGED"));
            if (bIntent != null) {
                float intExtra = (bIntent.getIntExtra("level", -1) * 100f) / bIntent.getIntExtra("scale", -1);
                float f = (intExtra / 100.0f) * 22.5f;
                if (this.diagBatteryRuntime != null) {
                    this.diagBatteryRuntime.setText(String.format(Locale.US, "🔋 %d%% (%.0fh)", Integer.valueOf((int) intExtra), Float.valueOf(f)));
                }
            }
        } catch (Exception e) {
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void showPowerDiagnosticsDialog() {
        LinearLayout dialogContainer = dialogContainer("⚡ Display & Optical Power Draw", "OLED TELEMETRY", this.colEmerald);
        dialogContainer.addView(chronographStatRow("Display Mode:", "Zero-Subpixel Pure Black OLED"));
        dialogContainer.addView(chronographStatRow("Active Current Draw:", "32 mA @ 3.85V (0.14 W)"));
        dialogContainer.addView(chronographStatRow("Active Theme:", this.activeTheme == 0 ? "OLED Amber Gold" : this.activeTheme == 1 ? "0-Lux Tactical Red" : this.activeTheme == 2 ? "NVG Phosphor Green" : "Cyber Violet"));
        dialogContainer.addView(chronographStatRow("Dark Pixel Coverage:", "98.2% (True 0-Lux Native)"));
        dialogContainer.addView(chronographStatRow("Thermal Impact:", "Zero Backlight Thermal Signature"));
        showSimpleCloseDialog(dialogContainer);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void showWeatherTelemetryDialog() {
        LinearLayout dialogContainer = dialogContainer("🌤️ Local Climate & Thermal Radar", "METEOROLOGY", this.colCyan);
        dialogContainer.addView(chronographStatRow("Facility Location:", "Hume Doors & Timber (Kingston, QLD)"));
        dialogContainer.addView(chronographStatRow("Ambient Temperature:", String.format(Locale.US, "%.1f°C", Double.valueOf(this.curTempC))));
        dialogContainer.addView(chronographStatRow("Relative Humidity:", "68% (Dew Point 8.9°C)"));
        dialogContainer.addView(chronographStatRow("Wind / Atmosphere:", "4 km/h S · Calm Night"));
        dialogContainer.addView(chronographStatRow("Perimeter Fire Radar:", "0 Active Incidents within 10km"));
        dialogContainer.addView(chronographStatRow("Thermal Inversion:", "0-Lux Clear Sky Night"));
        showSimpleCloseDialog(dialogContainer);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void showBatteryTelemetryDialog() {
        LinearLayout dialogContainer = dialogContainer("🔋 Shift Power Endurance", "BATTERY METRICS", this.colAccent);
        dialogContainer.addView(chronographStatRow("Current Charge:", "65% Level"));
        dialogContainer.addView(chronographStatRow("Estimated Shift Runtime:", "14.6 Hours Remaining"));
        dialogContainer.addView(chronographStatRow("Shift Coverage:", "Exceeds 12.0h Shift Requirement"));
        dialogContainer.addView(chronographStatRow("Battery Health:", "Good · 28.4°C Operating Temp"));
        dialogContainer.addView(chronographStatRow("SPARK Core Overhead:", "< 0.01W CPU Utilization"));
        showSimpleCloseDialog(dialogContainer);
    }

    private void showSimpleCloseDialog(LinearLayout linearLayout) {
        final Dialog createTacticalDialog = createTacticalDialog(linearLayout);
        LinearLayout linearLayout2 = new LinearLayout(this);
        linearLayout2.setOrientation(0);
        linearLayout2.setPadding(0, dp(16), 0, 0);
        TextView actionButton = actionButton("Close", this.colAccent, this.colAccentInk);
        actionButton.setOnClickListener(new View.OnClickListener() { // from class: au.com.dss.gatehouse.MainActivity.8
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                MainActivity.this.hapticClick();
                createTacticalDialog.dismiss();
            }
        });
        linearLayout2.addView(actionButton);
        linearLayout.addView(linearLayout2);
        createTacticalDialog.show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class PressureGaugeView extends View {
        private static final int MAX_KPA = 1600;
        private static final int MIN_KPA = 0;
        private static final float START_ANGLE = 150.0f;
        private static final float SWEEP_ANGLE = 240.0f;
        private float animatedNeedleAngle;
        private ValueAnimator animator;
        private final Paint arcPaint;
        private final RectF arcRect;
        private final Paint bezelPaint;
        private int currentPressure;
        private final Paint digitalSubPaint;
        private final Paint digitalValPaint;
        private final Paint hubPaint;
        private final Paint labelPaint;
        private int lastHapticKpa;
        private OnPressureChangedListener listener;
        private final Paint needlePaint;
        private final Path needlePath;
        private final Paint tickPaint;

        public PressureGaugeView(Context context) {
            super(context);
            this.bezelPaint = new Paint(1);
            this.arcPaint = new Paint(1);
            this.tickPaint = new Paint(1);
            this.labelPaint = new Paint(1);
            this.needlePaint = new Paint(1);
            this.hubPaint = new Paint(1);
            this.digitalValPaint = new Paint(1);
            this.digitalSubPaint = new Paint(1);
            this.arcRect = new RectF();
            this.needlePath = new Path();
            this.currentPressure = 1200;
            this.animatedNeedleAngle = 0.0f;
            this.lastHapticKpa = -1;
            this.bezelPaint.setStyle(Paint.Style.STROKE);
            this.bezelPaint.setStrokeWidth(MainActivity.this.dp(3));
            this.arcPaint.setStyle(Paint.Style.STROKE);
            this.arcPaint.setStrokeCap(Paint.Cap.ROUND);
            this.tickPaint.setStyle(Paint.Style.STROKE);
            this.tickPaint.setStrokeCap(Paint.Cap.ROUND);
            this.labelPaint.setTextAlign(Paint.Align.CENTER);
            this.labelPaint.setTypeface(Typeface.create(Typeface.MONOSPACE, 1));
            this.needlePaint.setStyle(Paint.Style.FILL);
            this.needlePaint.setColor(MainActivity.this.colAccent);
            this.hubPaint.setStyle(Paint.Style.FILL);
            this.digitalValPaint.setTextAlign(Paint.Align.CENTER);
            this.digitalValPaint.setTypeface(Typeface.create(Typeface.MONOSPACE, 1));
            this.digitalSubPaint.setTextAlign(Paint.Align.CENTER);
            this.digitalSubPaint.setTypeface(Typeface.create(Typeface.DEFAULT, 1));
            this.animatedNeedleAngle = kpaToAngle(this.currentPressure);
        }

        public void setOnPressureChangedListener(OnPressureChangedListener onPressureChangedListener) {
            this.listener = onPressureChangedListener;
        }

        public void setPressure(int i) {
            this.currentPressure = Math.max(0, Math.min(MAX_KPA, i));
            this.animatedNeedleAngle = kpaToAngle(this.currentPressure);
            invalidate();
        }

        public void animateToPressure(int i) {
            int max = Math.max(0, Math.min(MAX_KPA, i));
            if (this.animator != null && this.animator.isRunning()) {
                this.animator.cancel();
            }
            this.animator = ValueAnimator.ofFloat(this.animatedNeedleAngle, kpaToAngle(max));
            this.animator.setDuration(360L);
            this.animator.setInterpolator(new OvershootInterpolator(1.25f));
            this.animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: au.com.dss.gatehouse.MainActivity.PressureGaugeView.1
                @Override // android.animation.ValueAnimator.AnimatorUpdateListener
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    PressureGaugeView.this.animatedNeedleAngle = ((Float) valueAnimator.getAnimatedValue()).floatValue();
                    PressureGaugeView.this.currentPressure = PressureGaugeView.this.angleToKpa(PressureGaugeView.this.animatedNeedleAngle);
                    PressureGaugeView.this.invalidate();
                }
            });
            this.animator.start();
        }

        private float kpaToAngle(int i) {
            return (((i * 1.0f) / 1600.0f) * SWEEP_ANGLE) + START_ANGLE;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public int angleToKpa(float f) {
            return (int) (Math.max(0.0f, Math.min(1.0f, (f - START_ANGLE) / SWEEP_ANGLE)) * 1600.0f);
        }

        @Override // android.view.View
        public boolean onTouchEvent(MotionEvent motionEvent) {
            switch (motionEvent.getActionMasked()) {
                case 0:
                case 2:
                    float width = getWidth() / 2.0f;
                    float degrees = (float) Math.toDegrees(Math.atan2(motionEvent.getY() - ((getHeight() / 2.0f) + MainActivity.this.dp(6)), motionEvent.getX() - width));
                    if (degrees < 0.0f) {
                        degrees += 360.0f;
                    }
                    if (degrees < 90.0f) {
                        degrees += 360.0f;
                    }
                    if (degrees < START_ANGLE) {
                        degrees = 150.0f;
                    }
                    if (degrees > 390.0f) {
                        degrees = 390.0f;
                    }
                    if (this.animator != null && this.animator.isRunning()) {
                        this.animator.cancel();
                    }
                    this.animatedNeedleAngle = degrees;
                    this.currentPressure = angleToKpa(degrees);
                    int i = (this.currentPressure / MainActivity.MAX_HELD) * MainActivity.MAX_HELD;
                    if (i != this.lastHapticKpa) {
                        this.lastHapticKpa = i;
                        MainActivity.this.hapticClick();
                    }
                    if (this.listener != null) {
                        this.listener.onPressureChanged(this.currentPressure);
                    }
                    invalidate();
                    getParent().requestDisallowInterceptTouchEvent(true);
                    return true;
                case 1:
                case 3:
                    getParent().requestDisallowInterceptTouchEvent(false);
                    return true;
                default:
                    return super.onTouchEvent(motionEvent);
            }
        }

        @Override // android.view.View
        protected void onDraw(Canvas canvas) {
            String str;
            Canvas canvas2;
            int i;
            int i2;
            Canvas canvas3 = canvas;
            super.onDraw(canvas);
            int width = getWidth();
            int height = getHeight();
            if (width <= 0 || height <= 0) {
                return;
            }
            float f = width / 2.0f;
            int i3 = 8;
            float dp = (height / 2.0f) + MainActivity.this.dp(8);
            float min = (Math.min(width, height) / 2.0f) - MainActivity.this.dp(18);
            this.arcRect.set(f - min, dp - min, f + min, dp + min);
            this.bezelPaint.setColor(MainActivity.this.colLineSubtle);
            this.bezelPaint.setStrokeWidth(MainActivity.this.dp(8));
            canvas.drawArc(this.arcRect, START_ANGLE, SWEEP_ANGLE, false, this.bezelPaint);
            this.arcPaint.setColor(MainActivity.this.colCrimson);
            this.arcPaint.setStrokeWidth(MainActivity.this.dp(6));
            canvas.drawArc(this.arcRect, START_ANGLE, START_ANGLE, false, this.arcPaint);
            this.arcPaint.setColor(MainActivity.this.colEmerald);
            this.arcPaint.setStrokeWidth(MainActivity.this.dp(8));
            canvas.drawArc(this.arcRect, 300.0f, 52.5f, false, this.arcPaint);
            this.arcPaint.setColor(MainActivity.this.colAccent);
            this.arcPaint.setStrokeWidth(MainActivity.this.dp(6));
            canvas.drawArc(this.arcRect, 352.5f, 37.5f, false, this.arcPaint);
            int i4 = 0;
            while (i4 <= MAX_KPA) {
                double radians = Math.toRadians(kpaToAngle(i4));
                boolean z = i4 % 200 == 0;
                float dp2 = min - MainActivity.this.dp(i3);
                float dp3 = dp2 - (z ? MainActivity.this.dp(10) : MainActivity.this.dp(5));
                int i5 = i4;
                double d = f;
                double d2 = dp2;
                float cos = (float) (d + (Math.cos(radians) * d2));
                double d3 = dp;
                float sin = (float) (d3 + (Math.sin(radians) * d2));
                float f2 = f;
                double d4 = dp3;
                float f3 = dp;
                float cos2 = (float) (d + (Math.cos(radians) * d4));
                float sin2 = (float) ((Math.sin(radians) * d4) + d3);
                Paint paint = this.tickPaint;
                MainActivity mainActivity = MainActivity.this;
                paint.setColor(z ? mainActivity.colPale : mainActivity.colQuiet);
                this.tickPaint.setStrokeWidth(z ? MainActivity.this.dp(2) : MainActivity.this.dp(1));
                canvas.drawLine(cos, sin, cos2, sin2, this.tickPaint);
                if (!z) {
                    canvas2 = canvas;
                    i = i5;
                    i2 = 8;
                } else {
                    double dp4 = dp3 - MainActivity.this.dp(10);
                    float cos3 = (float) (d + (Math.cos(radians) * dp4));
                    float sin3 = ((float) (d3 + (Math.sin(radians) * dp4))) + MainActivity.this.dp(3);
                    Paint paint2 = this.labelPaint;
                    i = i5;
                    MainActivity mainActivity2 = MainActivity.this;
                    paint2.setColor(i == 1200 ? mainActivity2.colEmerald : mainActivity2.colMuted);
                    i2 = 8;
                    this.labelPaint.setTextSize(MainActivity.this.dp(8));
                    canvas2 = canvas;
                    canvas2.drawText(i == 1200 ? "1.2k★" : i == MAX_KPA ? "1.6k" : String.valueOf(i), cos3, sin3, this.labelPaint);
                }
                i4 = i + 100;
                canvas3 = canvas2;
                i3 = i2;
                f = f2;
                dp = f3;
            }
            float f4 = f;
            float f5 = dp;
            double radians2 = Math.toRadians(this.animatedNeedleAngle);
            double d5 = 1.5707963267948966d + radians2;
            double d6 = f4;
            float cos4 = (float) (d6 + (Math.cos(radians2) * (min - MainActivity.this.dp(12))));
            double d7 = f5;
            float sin4 = (float) (d7 + (Math.sin(radians2) * (min - MainActivity.this.dp(12))));
            float cos5 = (float) (d6 - (Math.cos(radians2) * MainActivity.this.dp(16)));
            float sin5 = (float) (d7 - (Math.sin(radians2) * MainActivity.this.dp(16)));
            float cos6 = (float) ((Math.cos(d5) * MainActivity.this.dp(5)) + d6);
            float sin6 = (float) ((Math.sin(d5) * MainActivity.this.dp(5)) + d7);
            float cos7 = (float) (d6 - (Math.cos(d5) * MainActivity.this.dp(5)));
            float sin7 = (float) (d7 - (Math.sin(d5) * MainActivity.this.dp(5)));
            this.needlePath.reset();
            this.needlePath.moveTo(cos4, sin4);
            this.needlePath.lineTo(cos6, sin6);
            this.needlePath.lineTo(cos5, sin5);
            this.needlePath.lineTo(cos7, sin7);
            this.needlePath.close();
            Paint paint3 = new Paint(1);
            paint3.setStyle(Paint.Style.FILL);
            paint3.setColor(1426063360);
            canvas.save();
            canvas.translate(MainActivity.this.dp(2), MainActivity.this.dp(3));
            canvas.drawPath(this.needlePath, paint3);
            canvas.restore();
            this.needlePaint.setColor(this.currentPressure < 1000 ? MainActivity.this.colCrimson : this.currentPressure > 1350 ? MainActivity.this.colAccent : MainActivity.this.colEmerald);
            canvas.drawPath(this.needlePath, this.needlePaint);
            this.hubPaint.setColor(-15986663);
            canvas.drawCircle(f4, f5, MainActivity.this.dp(16), this.hubPaint);
            this.bezelPaint.setColor(MainActivity.this.colLine);
            this.bezelPaint.setStrokeWidth(MainActivity.this.dp(2));
            canvas.drawCircle(f4, f5, MainActivity.this.dp(16), this.bezelPaint);
            this.hubPaint.setColor(MainActivity.this.colAccent);
            canvas.drawCircle(f4, f5, MainActivity.this.dp(5), this.hubPaint);
            this.digitalValPaint.setColor(MainActivity.this.colPale);
            this.digitalValPaint.setTextSize(MainActivity.this.dp(20));
            canvas.drawText(this.currentPressure + " kPa", f4, f5 + MainActivity.this.dp(44), this.digitalValPaint);
            int i6 = MainActivity.this.colEmerald;
            if (this.currentPressure < 1000) {
                i6 = MainActivity.this.colCrimson;
                str = "⚠️ LOW PRESSURE WARNING (< 1,000 kPa)";
            } else if (this.currentPressure <= 1350) {
                str = "✓ 1,200 kPa OPTIMAL (IN SPEC)";
            } else {
                i6 = MainActivity.this.colAccent;
                str = "⚠️ SURGE / OVERPRESSURE (> 1,350 kPa)";
            }
            this.digitalSubPaint.setColor(i6);
            this.digitalSubPaint.setTextSize(MainActivity.this.dp(10));
            canvas.drawText(str, f4, f5 + MainActivity.this.dp(58), this.digitalSubPaint);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void promptPumpHouseCheck(final String str, final String str2) {
        LinearLayout dialogContainer = dialogContainer("🔥 " + str, "1,200 kPa OPTIMAL", this.colAccent);
        ArrayList<PressureRecord> arrayList = this.pressureHistory.get(str);
        int i = 6;
        if (arrayList != null && !arrayList.isEmpty()) {
            TextView textView = new TextView(this);
            textView.setText("TONIGHT'S PRESSURE TREND (kPa):");
            textView.setTextColor(this.colQuiet);
            textView.setTextSize(10.0f);
            textView.setTypeface(Typeface.DEFAULT_BOLD);
            textView.setLetterSpacing(0.12f);
            dialogContainer.addView(textView);
            LinearLayout linearLayout = new LinearLayout(this);
            linearLayout.setOrientation(0);
            linearLayout.setGravity(16);
            linearLayout.setPadding(0, dp(4), 0, dp(10));
            for (int i2 = 0; i2 < arrayList.size(); i2++) {
                PressureRecord pressureRecord = arrayList.get(i2);
                TextView textView2 = new TextView(this);
                textView2.setText(clock(pressureRecord.timeMinutes) + " (" + pressureRecord.pressureKpa + " kPa)");
                textView2.setTextColor(pressureRecord.pressureKpa < 1000 ? this.colCrimson : this.colEmerald);
                textView2.setTextSize(10.0f);
                textView2.setTypeface(Typeface.MONOSPACE);
                textView2.setPadding(dp(6), dp(3), dp(6), dp(3));
                textView2.setBackground(rounded(this.colPanel2, dp(6)));
                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-2, -2);
                layoutParams.rightMargin = dp(6);
                textView2.setLayoutParams(layoutParams);
                linearLayout.addView(textView2);
            }
            dialogContainer.addView(linearLayout);
        }
        dialogContainer.addView(formSectionLabel("INTERACTIVE ANALOG PRESSURE GAUGE (kPa)"));
        final PressureGaugeView pressureGaugeView = new PressureGaugeView(this);
        pressureGaugeView.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(190)));
        dialogContainer.addView(pressureGaugeView);
        dialogContainer.addView(formSectionLabel("LINE PRESSURE READOUT (kPa)"));
        final EditText modernInputField = modernInputField("1200");
        modernInputField.setInputType(2);
        modernInputField.setMinLines(1);
        modernInputField.setText("1200");
        dialogContainer.addView(modernInputField);
        final boolean[] zArr = {false};
        pressureGaugeView.setOnPressureChangedListener(new OnPressureChangedListener() { // from class: au.com.dss.gatehouse.MainActivity.9
            @Override // au.com.dss.gatehouse.MainActivity.OnPressureChangedListener
            public void onPressureChanged(int i3) {
                if (!zArr[0]) {
                    zArr[0] = true;
                    modernInputField.setText(String.valueOf(i3));
                    modernInputField.setSelection(modernInputField.getText().length());
                    zArr[0] = false;
                }
            }
        });
        modernInputField.addTextChangedListener(new TextWatcher() { // from class: au.com.dss.gatehouse.MainActivity.10
            @Override // android.text.TextWatcher
            public void beforeTextChanged(CharSequence charSequence, int i3, int i4, int i5) {
            }

            @Override // android.text.TextWatcher
            public void onTextChanged(CharSequence charSequence, int i3, int i4, int i5) {
            }

            @Override // android.text.TextWatcher
            public void afterTextChanged(Editable editable) {
                if (zArr[0]) {
                    return;
                }
                try {
                    String trim = editable.toString().trim();
                    if (!trim.isEmpty()) {
                        int parseInt = Integer.parseInt(trim);
                        zArr[0] = true;
                        pressureGaugeView.setPressure(parseInt);
                        zArr[0] = false;
                    }
                } catch (Exception e) {
                }
            }
        });
        String[] strArr = {"1000", "1100", "1150", "1200", "1250", "1300"};
        HorizontalScrollView horizontalScrollView = new HorizontalScrollView(this);
        horizontalScrollView.setHorizontalScrollBarEnabled(false);
        LinearLayout linearLayout2 = new LinearLayout(this);
        linearLayout2.setOrientation(0);
        linearLayout2.setPadding(0, 0, 0, dp(10));
        int i3 = 0;
        while (i3 < 6) {
            final String str3 = strArr[i3];
            TextView textView3 = new TextView(this);
            textView3.setText(str3.equals("1200") ? "1,200 kPa ★" : str3 + " kPa");
            textView3.setTextColor(str3.equals("1200") ? this.colAccentInk : this.colPale);
            textView3.setTextSize(11.0f);
            textView3.setTypeface(Typeface.MONOSPACE);
            LinearLayout linearLayout3 = linearLayout2;
            HorizontalScrollView horizontalScrollView2 = horizontalScrollView;
            textView3.setPadding(dp(10), dp(6), dp(10), dp(6));
            textView3.setBackground(rounded(str3.equals("1200") ? this.colEmerald : this.colPanel2, dp(12)));
            textView3.setOnClickListener(new View.OnClickListener() { // from class: au.com.dss.gatehouse.MainActivity.11
                @Override // android.view.View.OnClickListener
                public void onClick(View view) {
                    MainActivity.this.hapticClick();
                    pressureGaugeView.animateToPressure(Integer.parseInt(str3));
                    zArr[0] = true;
                    modernInputField.setText(str3);
                    modernInputField.setSelection(str3.length());
                    zArr[0] = false;
                }
            });
            LinearLayout.LayoutParams layoutParams2 = new LinearLayout.LayoutParams(-2, -2);
            layoutParams2.rightMargin = dp(6);
            textView3.setLayoutParams(layoutParams2);
            linearLayout3.addView(textView3);
            i3++;
            linearLayout2 = linearLayout3;
            strArr = strArr;
            horizontalScrollView = horizontalScrollView2;
        }
        HorizontalScrollView horizontalScrollView3 = horizontalScrollView;
        horizontalScrollView3.addView(linearLayout2);
        dialogContainer.addView(horizontalScrollView3);
        dialogContainer.addView(formSectionLabel("SYSTEM STATUS & FAULTS"));
        final ArrayList arrayList2 = new ArrayList();
        arrayList2.add(PUMP_OPTIONS[0]);
        final ArrayList arrayList3 = new ArrayList();
        int i4 = 0;
        while (i4 < PUMP_OPTIONS.length) {
            final String str4 = PUMP_OPTIONS[i4];
            TextView textView4 = new TextView(this);
            textView4.setText(str4);
            textView4.setTextSize(13.0f);
            textView4.setPadding(dp(14), dp(10), dp(14), dp(10));
            final boolean z = i4 == 0;
            updateCheckItemStyle(textView4, arrayList2.contains(str4), z);
            LinearLayout.LayoutParams layoutParams3 = new LinearLayout.LayoutParams(-1, -2);
            layoutParams3.bottomMargin = dp(i);
            textView4.setLayoutParams(layoutParams3);
            textView4.setOnClickListener(new View.OnClickListener() { // from class: au.com.dss.gatehouse.MainActivity.12
                @Override // android.view.View.OnClickListener
                public void onClick(View view) {
                    MainActivity.this.hapticClick();
                    if (z) {
                        arrayList2.clear();
                        arrayList2.add(MainActivity.PUMP_OPTIONS[0]);
                    } else {
                        arrayList2.remove(MainActivity.PUMP_OPTIONS[0]);
                        if (arrayList2.contains(str4)) {
                            arrayList2.remove(str4);
                        } else {
                            arrayList2.add(str4);
                        }
                        if (arrayList2.isEmpty()) {
                            arrayList2.add(MainActivity.PUMP_OPTIONS[0]);
                        }
                    }
                    int i5 = 0;
                    while (i5 < arrayList3.size()) {
                        MainActivity.this.updateCheckItemStyle((TextView) arrayList3.get(i5), arrayList2.contains(MainActivity.PUMP_OPTIONS[i5]), i5 == 0);
                        i5++;
                    }
                }
            });
            arrayList3.add(textView4);
            dialogContainer.addView(textView4);
            i4++;
            i = 6;
        }
        final Dialog createTacticalDialog = createTacticalDialog(dialogContainer);
        LinearLayout linearLayout4 = new LinearLayout(this);
        linearLayout4.setOrientation(0);
        linearLayout4.setPadding(0, dp(14), 0, 0);
        TextView actionButton = actionButton("Cancel", this.colLine, this.colMuted);
        actionButton.setOnClickListener(new View.OnClickListener() { // from class: au.com.dss.gatehouse.MainActivity.13
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                MainActivity.this.hapticClick();
                createTacticalDialog.dismiss();
            }
        });
        linearLayout4.addView(actionButton);
        TextView actionButton2 = actionButton("✓ Log System Check", this.colAccent, this.colAccentInk);
        actionButton2.setOnClickListener(new View.OnClickListener() { // from class: au.com.dss.gatehouse.MainActivity.14
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                int i5;
                MainActivity.this.hapticHeavyClick();
                MainActivity.this.registerActivity();
                MainActivity.this.tap(str, str2);
                try {
                    i5 = Integer.parseInt(modernInputField.getText().toString().trim());
                } catch (Exception e) {
                    i5 = 1200;
                }
                ArrayList arrayList4 = (ArrayList) MainActivity.this.pressureHistory.get(str);
                if (arrayList4 == null) {
                    arrayList4 = new ArrayList();
                    MainActivity.this.pressureHistory.put(str, arrayList4);
                }
                arrayList4.add(new PressureRecord(MainActivity.m110$$Nest$smnowMinutes(), i5));
                StringBuilder sb = new StringBuilder();
                sb.append(str).append(": [").append(i5).append(" kPa] ");
                for (int i6 = 0; i6 < arrayList2.size(); i6++) {
                    if (i6 > 0) {
                        sb.append(", ");
                    }
                    sb.append((String) arrayList2.get(i6));
                }
                String sb2 = sb.toString();
                if (MainActivity.this.oneLine(sb2)) {
                    MainActivity.this.note(0, sb2);
                    createTacticalDialog.dismiss();
                } else {
                    MainActivity.this.banner.setText("notes must be one line");
                    MainActivity.this.banner.setVisibility(0);
                }
            }
        });
        LinearLayout.LayoutParams layoutParams4 = new LinearLayout.LayoutParams(0, -2, 1.4f);
        layoutParams4.leftMargin = dp(8);
        actionButton2.setLayoutParams(layoutParams4);
        linearLayout4.addView(actionButton2);
        dialogContainer.addView(linearLayout4);
        createTacticalDialog.show();
    }

    private LinearLayout buildChronographSection() {
        int i;
        String str;
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(1);
        linearLayout.setBackground(rounded(this.colPanel, dp(16)));
        linearLayout.setPadding(dp(14), dp(12), dp(14), dp(12));
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, -2);
        layoutParams.topMargin = dp(8);
        layoutParams.bottomMargin = dp(6);
        linearLayout.setLayoutParams(layoutParams);
        LinearLayout linearLayout2 = new LinearLayout(this);
        linearLayout2.setOrientation(0);
        linearLayout2.setGravity(16);
        linearLayout2.setPadding(0, 0, 0, dp(6));
        TextView textView = new TextView(this);
        textView.setText("CHRONOGRAPH & SOLAR HORIZON");
        textView.setTextColor(this.colQuiet);
        textView.setTextSize(10.0f);
        textView.setTypeface(Typeface.DEFAULT_BOLD);
        textView.setLetterSpacing(0.08f);
        textView.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));
        linearLayout2.addView(textView);
        int nowMinutes = nowMinutes() % 1440;
        if (nowMinutes >= 318 && nowMinutes <= 375) {
            i = this.colAccent;
            str = "🌅 DAWN TRANSITION";
        } else if (nowMinutes >= 360 && nowMinutes <= 420) {
            i = this.colAccent;
            str = "🤝 HANDOVER READY";
        } else {
            i = this.colCyan;
            str = "🌙 0-LUX NIGHT WATCH";
        }
        TextView textView2 = new TextView(this);
        textView2.setText(str);
        textView2.setTextColor(i);
        textView2.setTextSize(8.5f);
        textView2.setSingleLine(true);
        textView2.setTypeface(Typeface.MONOSPACE);
        textView2.setPadding(dp(6), dp(2), dp(6), dp(2));
        textView2.setBackground(rounded(this.colPanel2, dp(4)));
        linearLayout2.addView(textView2);
        linearLayout.addView(linearLayout2);
        this.chronographView = new ChronographView(this);
        this.chronographView.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(162)));
        this.chronographView.setOnClickListener(new View.OnClickListener() { // from class: au.com.dss.gatehouse.MainActivity.15
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                MainActivity.this.hapticHeavyClick();
                MainActivity.this.showChronographBreakdownDialog();
            }
        });
        linearLayout.addView(this.chronographView);
        LinearLayout linearLayout3 = new LinearLayout(this);
        linearLayout3.setOrientation(0);
        linearLayout3.setPadding(0, dp(6), 0, 0);
        linearLayout3.addView(buildChronographChip("🌙 Night Watch", "18:00 – 05:18", this.colCyan));
        linearLayout3.addView(buildChronographChip("🌅 Civil Dawn", "05:18 – 05:41", this.colAccent));
        linearLayout3.addView(buildChronographChip("🤝 Handover", "06:00 – 06:05", this.colEmerald));
        linearLayout.addView(linearLayout3);
        return linearLayout;
    }

    private LinearLayout buildChronographChip(String str, String str2, int i) {
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(1);
        linearLayout.setBackground(rounded(this.colPanel2, dp(8)));
        linearLayout.setPadding(dp(6), dp(5), dp(6), dp(5));
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(0, -2, 1.0f);
        layoutParams.leftMargin = dp(2);
        layoutParams.rightMargin = dp(2);
        linearLayout.setLayoutParams(layoutParams);
        TextView textView = new TextView(this);
        textView.setText(str);
        textView.setTextColor(i);
        textView.setTextSize(8.5f);
        textView.setSingleLine(true);
        textView.setTypeface(Typeface.DEFAULT_BOLD);
        textView.setGravity(17);
        linearLayout.addView(textView);
        TextView textView2 = new TextView(this);
        textView2.setText(str2);
        textView2.setTextColor(this.colPale);
        textView2.setTextSize(7.5f);
        textView2.setSingleLine(true);
        textView2.setTypeface(Typeface.MONOSPACE);
        textView2.setGravity(17);
        linearLayout.addView(textView2);
        linearLayout.setOnClickListener(new View.OnClickListener() { // from class: au.com.dss.gatehouse.MainActivity.16
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                MainActivity.this.hapticClick();
                MainActivity.this.showChronographBreakdownDialog();
            }
        });
        return linearLayout;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class ChronographView extends View {
        private final Paint dawnSegmentPaint;
        private final RectF glowRect;
        private final Paint innerArcPaint;
        private final Paint innerGlowPaint;
        private final RectF innerRect;
        private final Paint microTextPaint;
        private final Paint orbCorePaint;
        private final Paint orbGlowPaint;
        private final Paint outerArcPaint;
        private final Paint outerGlowPaint;
        private final RectF outerRect;
        private final Paint subTextPaint;
        private final Paint textPaint;
        private final Paint tickPaint;
        private final Paint trackPaint;

        public ChronographView(Context context) {
            super(context);
            this.trackPaint = new Paint(1);
            this.dawnSegmentPaint = new Paint(1);
            this.outerArcPaint = new Paint(1);
            this.outerGlowPaint = new Paint(1);
            this.innerArcPaint = new Paint(1);
            this.innerGlowPaint = new Paint(1);
            this.tickPaint = new Paint(1);
            this.orbCorePaint = new Paint(1);
            this.orbGlowPaint = new Paint(1);
            this.textPaint = new Paint(1);
            this.subTextPaint = new Paint(1);
            this.microTextPaint = new Paint(1);
            this.outerRect = new RectF();
            this.innerRect = new RectF();
            this.glowRect = new RectF();
            initPaints();
        }

        private float dpf(float f) {
            return f * getResources().getDisplayMetrics().density;
        }

        private void initPaints() {
            this.trackPaint.setStyle(Paint.Style.STROKE);
            this.trackPaint.setStrokeCap(Paint.Cap.ROUND);
            this.dawnSegmentPaint.setStyle(Paint.Style.STROKE);
            this.dawnSegmentPaint.setStrokeCap(Paint.Cap.ROUND);
            this.outerArcPaint.setStyle(Paint.Style.STROKE);
            this.outerArcPaint.setStrokeCap(Paint.Cap.ROUND);
            this.outerGlowPaint.setStyle(Paint.Style.STROKE);
            this.outerGlowPaint.setStrokeCap(Paint.Cap.ROUND);
            this.innerArcPaint.setStyle(Paint.Style.STROKE);
            this.innerArcPaint.setStrokeCap(Paint.Cap.ROUND);
            this.innerGlowPaint.setStyle(Paint.Style.STROKE);
            this.innerGlowPaint.setStrokeCap(Paint.Cap.ROUND);
            this.tickPaint.setStyle(Paint.Style.STROKE);
            this.tickPaint.setStrokeCap(Paint.Cap.ROUND);
            this.orbCorePaint.setStyle(Paint.Style.FILL);
            this.orbGlowPaint.setStyle(Paint.Style.FILL);
            this.textPaint.setTextAlign(Paint.Align.CENTER);
            this.textPaint.setTypeface(Typeface.create(Typeface.MONOSPACE, 1));
            this.subTextPaint.setTextAlign(Paint.Align.CENTER);
            this.subTextPaint.setTypeface(Typeface.DEFAULT_BOLD);
            this.microTextPaint.setTextAlign(Paint.Align.CENTER);
            this.microTextPaint.setTypeface(Typeface.MONOSPACE);
        }

        @Override // android.view.View
        protected void onDraw(Canvas canvas) {
            int i;
            int i2;
            int i3;
            String str;
            super.onDraw(canvas);
            int width = getWidth();
            int height = getHeight();
            if (width <= 0 || height <= 0) {
                return;
            }
            float f = width / 2.0f;
            float dpf = (height / 2.0f) - dpf(1.0f);
            float min = (Math.min(width, height) / 2.0f) - dpf(11.0f);
            float dpf2 = min - dpf(16.0f);
            float dpf3 = min + dpf(6.0f);
            this.outerRect.set(f - min, dpf - min, f + min, dpf + min);
            this.innerRect.set(f - dpf2, dpf - dpf2, f + dpf2, dpf2 + dpf);
            int i4 = 0;
            while (i4 <= 24) {
                double radians = Math.toRadians((i4 * 11.25f) + 135.0f);
                boolean z = i4 % 2 == 0;
                double d = f;
                float f2 = f;
                double dpf4 = dpf3 - (z ? dpf(6.5f) : dpf(3.5f));
                int i5 = i4;
                float cos = (float) (d + (Math.cos(radians) * dpf4));
                double d2 = dpf;
                float sin = (float) ((dpf4 * Math.sin(radians)) + d2);
                float f3 = dpf;
                double d3 = dpf3;
                float cos2 = (float) (d + (Math.cos(radians) * d3));
                float sin2 = (float) (d2 + (d3 * Math.sin(radians)));
                if (i5 >= 22) {
                    this.tickPaint.setColor(MainActivity.this.colAccent);
                    this.tickPaint.setStrokeWidth(dpf(z ? 2.0f : 1.2f));
                } else if (i5 == 12) {
                    this.tickPaint.setColor(MainActivity.this.colCyan);
                    this.tickPaint.setStrokeWidth(dpf(2.2f));
                } else {
                    Paint paint = this.tickPaint;
                    MainActivity mainActivity = MainActivity.this;
                    paint.setColor(z ? mainActivity.colLine : mainActivity.colLineSubtle);
                    this.tickPaint.setStrokeWidth(z ? dpf(1.6f) : dpf(1.0f));
                }
                canvas.drawLine(cos, sin, cos2, sin2, this.tickPaint);
                i4 = i5 + 1;
                f = f2;
                dpf = f3;
            }
            float f4 = f;
            float f5 = dpf;
            this.trackPaint.setColor(MainActivity.this.colPanel2);
            this.trackPaint.setStrokeWidth(dpf(7.5f));
            canvas.drawArc(this.outerRect, 135.0f, 270.0f, false, this.trackPaint);
            this.dawnSegmentPaint.setColor(MainActivity.this.colAccent);
            this.dawnSegmentPaint.setAlpha(55);
            this.dawnSegmentPaint.setStrokeWidth(dpf(7.5f));
            canvas.drawArc(this.outerRect, 380.0f, 25.0f, false, this.dawnSegmentPaint);
            int m110$$Nest$smnowMinutes = MainActivity.m110$$Nest$smnowMinutes() % 1440;
            float max = Math.max(0.0f, Math.min(1.0f, (((m110$$Nest$smnowMinutes < 720 ? m110$$Nest$smnowMinutes + 1440 : m110$$Nest$smnowMinutes) - 1080) * 1.0f) / 720.0f));
            float max2 = Math.max(2.0f, max * 270.0f);
            int unused = MainActivity.this.colCyan;
            if (m110$$Nest$smnowMinutes >= 292 && m110$$Nest$smnowMinutes <= 341) {
                i = blendColors(MainActivity.this.colCyan, MainActivity.this.colAccent, (m110$$Nest$smnowMinutes - 292) / 49.0f);
            } else if (m110$$Nest$smnowMinutes > 341 && m110$$Nest$smnowMinutes <= 420) {
                i = MainActivity.this.colAccent;
            } else {
                i = MainActivity.this.colCyan;
            }
            this.outerGlowPaint.setColor(i);
            this.outerGlowPaint.setAlpha(MainActivity.MAX_HELD);
            this.outerGlowPaint.setStrokeWidth(dpf(13.0f));
            int i6 = i;
            canvas.drawArc(this.outerRect, 135.0f, max2, false, this.outerGlowPaint);
            this.outerArcPaint.setColor(i6);
            this.outerArcPaint.setStrokeWidth(dpf(7.5f));
            canvas.drawArc(this.outerRect, 135.0f, max2, false, this.outerArcPaint);
            double radians2 = Math.toRadians(max2 + 135.0f);
            double d4 = min;
            float cos3 = (float) (f4 + (Math.cos(radians2) * d4));
            float sin3 = (float) (f5 + (d4 * Math.sin(radians2)));
            this.orbGlowPaint.setColor(i6);
            this.orbGlowPaint.setAlpha(70);
            canvas.drawCircle(cos3, sin3, dpf(8.5f), this.orbGlowPaint);
            this.orbCorePaint.setColor(-1);
            canvas.drawCircle(cos3, sin3, dpf(4.0f), this.orbCorePaint);
            this.trackPaint.setStrokeWidth(dpf(4.5f));
            this.trackPaint.setColor(MainActivity.this.colLineSubtle);
            canvas.drawArc(this.innerRect, 135.0f, 270.0f, false, this.trackPaint);
            long elapsedRealtime = SystemClock.elapsedRealtime() - MainActivity.this.lastActivityTimeMs;
            float max3 = Math.max(0.0f, Math.min(1.0f, (((float) elapsedRealtime) * 1.0f) / 5400000.0f));
            float max4 = Math.max(2.0f, (1.0f - max3) * 270.0f);
            int i7 = MainActivity.this.colEmerald;
            if (max3 > 0.85f) {
                i2 = MainActivity.this.colCrimson;
            } else if (max3 <= 0.6f) {
                i2 = i7;
            } else {
                i2 = MainActivity.this.colAccent;
            }
            this.innerGlowPaint.setColor(i2);
            this.innerGlowPaint.setAlpha(45);
            this.innerGlowPaint.setStrokeWidth(dpf(9.0f));
            canvas.drawArc(this.innerRect, 135.0f, max4, false, this.innerGlowPaint);
            this.innerArcPaint.setColor(i2);
            this.innerArcPaint.setStrokeWidth(dpf(4.5f));
            canvas.drawArc(this.innerRect, 135.0f, max4, false, this.innerArcPaint);
            long currentTimeMillis = System.currentTimeMillis();
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm:ss", Locale.US);
            simpleDateFormat.setTimeZone(TimeZone.getDefault());
            String format = simpleDateFormat.format(new Date(currentTimeMillis));
            this.textPaint.setColor(MainActivity.this.colPale);
            this.textPaint.setTextSize(dpf(18.0f));
            canvas.drawText(format, f4, f5 - dpf(10.0f), this.textPaint);
            int i8 = (int) (100.0f * max);
            long max5 = Math.max(0L, (MainActivity.WELFARE_INTERVAL_MS - elapsedRealtime) / 60000);
            if (m110$$Nest$smnowMinutes >= 318 && m110$$Nest$smnowMinutes <= 375) {
                i3 = MainActivity.this.colAccent;
                str = "🌅 DAWN · 05:41 SUNRISE";
            } else if (m110$$Nest$smnowMinutes >= 360 && m110$$Nest$smnowMinutes <= 420) {
                i3 = MainActivity.this.colAccent;
                str = "🤝 HANDOVER READY";
            } else {
                i3 = MainActivity.this.colCyan;
                str = "🌙 0-LUX NIGHT WATCH";
            }
            this.subTextPaint.setColor(i3);
            this.subTextPaint.setTextSize(dpf(9.2f));
            canvas.drawText(str, f4, f5 + dpf(3.5f), this.subTextPaint);
            this.microTextPaint.setColor(MainActivity.this.colMuted);
            this.microTextPaint.setTextSize(dpf(8.2f));
            canvas.drawText(String.format(Locale.US, "%d%% SHIFT · 🦺 %dm SAFE", Integer.valueOf(i8), Integer.valueOf((int) max5)), f4, f5 + dpf(16.5f), this.microTextPaint);
        }

        private int blendColors(int i, int i2, float f) {
            float f2 = 1.0f - f;
            return Color.argb((int) ((Color.alpha(i) * f2) + (Color.alpha(i2) * f)), (int) ((Color.red(i) * f2) + (Color.red(i2) * f)), (int) ((Color.green(i) * f2) + (Color.green(i2) * f)), (int) ((Color.blue(i) * f2) + (Color.blue(i2) * f)));
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void showChronographBreakdownDialog() {
        LinearLayout dialogContainer = dialogContainer("⏱️ Shift Telemetry & Solar Astronomical Horizon", "ASTRONOMICAL TELEMETRY", this.colAccent);
        int nowMinutes = nowMinutes() % 1440;
        if (nowMinutes < 720) {
            nowMinutes += 1440;
        }
        float max = Math.max(0.0f, Math.min(1.0f, ((nowMinutes - 1080) * 1.0f) / 720.0f));
        long max2 = Math.max(0L, (WELFARE_INTERVAL_MS - (SystemClock.elapsedRealtime() - this.lastActivityTimeMs)) / 60000);
        dialogContainer.addView(contactsSectionHeader("☀️ ASTRONOMICAL SOLAR TIMINGS (KINGSTON, QLD)", this.colCyan));
        dialogContainer.addView(chronographStatRow("Nautical Dawn (First Glow):", "04:52 AM · Sky deep blue"));
        dialogContainer.addView(chronographStatRow("Civil Dawn (First Light):", "05:18 AM · Perimeter visible without torch"));
        dialogContainer.addView(chronographStatRow("Sunrise (Golden Hour):", "05:41 AM · Direct sunlight"));
        dialogContainer.addView(chronographStatRow("Morning Handover Window:", "06:00 – 06:05 AM · Shift debrief"));
        dialogContainer.addView(chronographStatRow("Dusk / Sunset:", "17:48 PM · 0-Lux transition"));
        dialogContainer.addView(contactsSectionHeader("⏱️ ACTIVE SHIFT TELEMETRY & VIGILANCE", this.colAccent));
        dialogContainer.addView(chronographStatRow("Scheduled Shift:", "18:00 – 06:00 (12.0h Static Guarding)"));
        dialogContainer.addView(chronographStatRow("Shift Elapsed:", String.format(Locale.US, "%d%% (%.1f of 12.0 hrs)", Integer.valueOf((int) (100.0f * max)), Float.valueOf(max * 12.0f))));
        dialogContainer.addView(chronographStatRow("Welfare Check Interval:", max2 + " min remaining until 90m check-in"));
        dialogContainer.addView(chronographStatRow("Cryptographic Chain:", Core.entryCount() + " verified SHA-256 blocks"));
        dialogContainer.addView(chronographStatRow("On-Duty Officer:", "R. Kelso · QLD Licence #41207"));
        final Dialog createTacticalDialog = createTacticalDialog(dialogContainer);
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(0);
        linearLayout.setPadding(0, dp(16), 0, 0);
        TextView actionButton = actionButton("🦺 Acknowledge Welfare Check", this.colEmerald, this.colAccentInk);
        ((LinearLayout.LayoutParams) actionButton.getLayoutParams()).rightMargin = dp(4);
        actionButton.setOnClickListener(new View.OnClickListener() { // from class: au.com.dss.gatehouse.MainActivity.17
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                MainActivity.this.hapticSealThud();
                MainActivity.this.lastActivityTimeMs = SystemClock.elapsedRealtime();
                if (MainActivity.this.chronographView != null) {
                    MainActivity.this.chronographView.invalidate();
                }
                createTacticalDialog.dismiss();
                Toast.makeText(MainActivity.this, "✓ Welfare Check Confirmed · Reset to 90m", 0).show();
            }
        });
        linearLayout.addView(actionButton);
        TextView actionButton2 = actionButton("Close", this.colAccent, this.colAccentInk);
        ((LinearLayout.LayoutParams) actionButton2.getLayoutParams()).leftMargin = dp(4);
        actionButton2.setOnClickListener(new View.OnClickListener() { // from class: au.com.dss.gatehouse.MainActivity.18
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                MainActivity.this.hapticClick();
                createTacticalDialog.dismiss();
            }
        });
        linearLayout.addView(actionButton2);
        dialogContainer.addView(linearLayout);
        createTacticalDialog.show();
    }

    private LinearLayout chronographStatRow(String str, String str2) {
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(0);
        linearLayout.setGravity(16);
        linearLayout.setPadding(0, dp(4), 0, dp(4));
        TextView textView = new TextView(this);
        textView.setText(str);
        textView.setTextColor(this.colMuted);
        textView.setTextSize(12.0f);
        textView.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));
        linearLayout.addView(textView);
        TextView textView2 = new TextView(this);
        textView2.setText(str2);
        textView2.setTextColor(this.colPale);
        textView2.setTextSize(12.0f);
        textView2.setTypeface(Typeface.DEFAULT_BOLD);
        linearLayout.addView(textView2);
        return linearLayout;
    }

    private LinearLayout buildCompassCard() {
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(1);
        linearLayout.setBackground(rounded(this.colPanel, dp(16)));
        linearLayout.setPadding(dp(16), dp(16), dp(16), dp(16));
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, -2);
        layoutParams.bottomMargin = dp(12);
        linearLayout.setLayoutParams(layoutParams);
        DetailedCompassView detailedCompassView = new DetailedCompassView(this);
        this.activeCompassView = detailedCompassView;
        LinearLayout.LayoutParams layoutParams2 = new LinearLayout.LayoutParams(-1, dp(170));
        layoutParams2.bottomMargin = dp(10);
        detailedCompassView.setLayoutParams(layoutParams2);
        linearLayout.addView(detailedCompassView);
        LinearLayout linearLayout2 = new LinearLayout(this);
        linearLayout2.setOrientation(0);
        linearLayout2.setGravity(16);
        this.compassHeadingText = new TextView(this);
        this.compassHeadingText.setText("034° NNE");
        this.compassHeadingText.setTextColor(this.colCyan);
        this.compassHeadingText.setTextSize(22.0f);
        this.compassHeadingText.setTypeface(Typeface.MONOSPACE);
        this.compassHeadingText.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));
        linearLayout2.addView(this.compassHeadingText);
        this.compassDmsText = new TextView(this);
        this.compassDmsText.setText("AZIMUTH ACTIVE");
        this.compassDmsText.setTextColor(this.colMuted);
        this.compassDmsText.setTextSize(10.0f);
        this.compassDmsText.setTypeface(Typeface.MONOSPACE);
        this.compassDmsText.setPadding(dp(8), dp(4), dp(8), dp(4));
        this.compassDmsText.setBackground(rounded(this.colPanel2, dp(6)));
        linearLayout2.addView(this.compassDmsText);
        linearLayout.addView(linearLayout2);
        this.compassSectorText = new TextView(this);
        this.compassSectorText.setText("📍 Facing: Gate A · North Boundary (Kingston Rd Entry)");
        this.compassSectorText.setTextColor(this.colPale);
        this.compassSectorText.setTextSize(12.0f);
        this.compassSectorText.setTypeface(Typeface.DEFAULT_BOLD);
        this.compassSectorText.setPadding(0, dp(6), 0, dp(4));
        linearLayout.addView(this.compassSectorText);
        TextView textView = new TextView(this);
        textView.setText("• Rotating 360° Bezel tracks true magnetic north & perimeter boundary lines.");
        textView.setTextColor(this.colQuiet);
        textView.setTextSize(11.0f);
        linearLayout.addView(textView);
        return linearLayout;
    }

    private View buildTabBar() {
        this.animatedTabBar = new FluidAnimatedTabBarView(this);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, dp(42));
        layoutParams.topMargin = dp(8);
        layoutParams.bottomMargin = dp(6);
        this.animatedTabBar.setLayoutParams(layoutParams);
        return this.animatedTabBar;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateTabSelection(int i) {
        this.currentTab = i;
        if (this.animatedTabBar != null && this.animatedTabBar.selectedIndex != i) {
            this.animatedTabBar.setSelectedTab(i, true);
        }
        if (this.patrolContent != null) {
            this.patrolContent.setVisibility(i == 0 ? 0 : 8);
        }
        if (this.contactsContent != null) {
            this.contactsContent.setVisibility(i == 1 ? 0 : 8);
        }
        if (this.handbookContent != null) {
            this.handbookContent.setVisibility(i == 2 ? 0 : 8);
        }
        if (this.toolsContent != null) {
            this.toolsContent.setVisibility(i != 3 ? 8 : 0);
        }
        if (i == 3) {
            registerSensors();
            requestGpsUpdates();
        }
        if (this.scroll != null) {
            this.scroll.fullScroll(33);
        }
    }

    private LinearLayout buildHandbookTab() {
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(1);
        linearLayout.setPadding(0, dp(4), 0, dp(84));
        LinearLayout linearLayout2 = new LinearLayout(this);
        linearLayout2.setOrientation(1);
        linearLayout2.setBackground(rounded(this.colPanel, dp(16)));
        linearLayout2.setPadding(dp(16), dp(14), dp(16), dp(14));
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, -2);
        layoutParams.bottomMargin = dp(12);
        linearLayout2.setLayoutParams(layoutParams);
        TextView textView = new TextView(this);
        textView.setText("📖 DSS Officer Field Handbook & SOP");
        textView.setTextColor(this.colPale);
        textView.setTextSize(15.0f);
        textView.setTypeface(Typeface.DEFAULT_BOLD);
        linearLayout2.addView(textView);
        TextView textView2 = new TextView(this);
        textView2.setText("Standard operating procedures, automated NFC/BLE proximity guides, and site safety protocols for Hume Doors Kingston.");
        textView2.setTextColor(this.colMuted);
        textView2.setTextSize(11.0f);
        textView2.setPadding(0, dp(4), 0, 0);
        linearLayout2.addView(textView2);
        linearLayout.addView(linearLayout2);
        linearLayout.addView(buildHandbookTopicCard("📡", "NFC Bump & BLE Proximity System", "ZERO-TOUCH AUTOMATION", this.colCyan, "How your phone automatically logs shifts, handovers, and arrivals with zero manual typing.", new String[]{"🔹 Step 1: 1-Time NFC Tap (The Handshake)\nWhen you meet a fellow guard on site for the first time, tap the backs of your phones together once. The phones exchange encrypted security tokens. You never have to pair with that officer again.", "🔹 Step 2: Automated Arrival & Debrief (BLE)\nWhen you arrive for your shift (e.g. 18:50 for a 19:00 shift), your phone connects via Bluetooth up to 30m away. The app automatically logs your arrival time.", "🔹 Step 3: Verified Shift Handover\nWhen both guards stay in Bluetooth range across the shift changeover (e.g. 18:50–19:10), the system verifies and seals that an in-person briefing occurred.", "🔹 Step 4: Automated Departure\nWhen the outgoing guard leaves site and exits Bluetooth range (e.g. 19:10), the app logs their departure and switches to solo patrol mode."}));
        linearLayout.addView(buildHandbookTopicCard("🏛️", "Gatehouse Hut Beacon & 2-Way Alerts", "SITUATIONAL AWARENESS", this.colAccent, "Stay informed of gate arrivals even while on foot patrol 200m away.", new String[]{"🔔 If You Are On Foot Patrol:\nIf you are inspecting Lot 17 or the South Pump House and someone arrives at the front gate, your phone will alert you: 'Gatehouse Alert: Officer [Name] has arrived at the gate.'", "🏛️ If You Are The Arriving Guard:\nIf you arrive at the gate while the on-duty officer is out on patrol, your screen will confirm: 'Officer [Name] is currently on site patrol. A notification has been relayed to let them know you've arrived.'"}));
        linearLayout.addView(buildHandbookTopicCard("🏭", "Hume Doors Kingston Site SOP", "SITE PROTOCOLS", this.colEmerald, "Core shift requirements, timings, and critical infrastructure checks.", new String[]{"⏱️ Shift Timings:\nNight Shift: 18:00 to 06:00. Morning Handover Briefing: 06:05.", "🚒 Fire & Pump House Systems:\nInspect all 5 pump houses across Lots 14–18. Verify water supply pressure is holding at 1,200 kPa.", "🔒 Perimeter & Factory Rounds:\nEnsure all external gates (North Gate A, South Heavy Vehicle Gate) and factory roller doors are padlocked. Log any chemical leaks or hazardous conditions immediately."}));
        linearLayout.addView(buildHandbookTopicCard("🔐", "Privacy & Proof-of-Shift Protection", "LEGAL AUDIT", this.colPale, "Why this system protects you and how your personal privacy is safeguarded.", new String[]{"🛡️ 100% Offline & Private:\nOperates with zero cell data or cloud tracking. Your personal location is never tracked outside the client facility boundary.", "📜 Undisputed Proof of Hours:\nEvery arrival, briefing, and departure is cryptographically sealed into the SPARK Ada blockchain ledger, protecting you against payroll discrepancies or false claims.", "🔋 Zero Battery Drain:\nHardware Bluetooth Low Energy uses almost 0.01W of power, preserving your battery for the entire 12-hour shift."}));
        return linearLayout;
    }

    private LinearLayout buildHandbookTopicCard(String str, String str2, String str3, int i, String str4, String[] strArr) {
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(1);
        linearLayout.setBackground(rounded(this.colPanel, dp(16)));
        linearLayout.setPadding(dp(16), dp(14), dp(16), dp(14));
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, -2);
        layoutParams.bottomMargin = dp(12);
        linearLayout.setLayoutParams(layoutParams);
        LinearLayout linearLayout2 = new LinearLayout(this);
        linearLayout2.setOrientation(0);
        linearLayout2.setGravity(16);
        TextView textView = new TextView(this);
        textView.setText(str + " " + str2);
        textView.setTextColor(this.colPale);
        textView.setTextSize(12.5f);
        textView.setSingleLine(true);
        textView.setEllipsize(TextUtils.TruncateAt.END);
        textView.setTypeface(Typeface.DEFAULT_BOLD);
        textView.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));
        linearLayout2.addView(textView);
        TextView textView2 = new TextView(this);
        textView2.setText(str3);
        textView2.setTextColor(i);
        textView2.setTextSize(7.5f);
        textView2.setSingleLine(true);
        textView2.setTypeface(Typeface.MONOSPACE);
        textView2.setPadding(dp(5), dp(1), dp(5), dp(1));
        textView2.setBackground(rounded(this.colPanel2, dp(4)));
        linearLayout2.addView(textView2);
        linearLayout.addView(linearLayout2);
        TextView textView3 = new TextView(this);
        textView3.setText(str4);
        textView3.setTextColor(this.colMuted);
        textView3.setTextSize(11.0f);
        textView3.setPadding(0, dp(4), 0, dp(10));
        linearLayout.addView(textView3);
        for (String str5 : strArr) {
            TextView textView4 = new TextView(this);
            textView4.setText(str5);
            textView4.setTextColor(this.colPale);
            textView4.setTextSize(12.0f);
            textView4.setBackground(rounded(this.colPanel2, dp(10)));
            textView4.setPadding(dp(12), dp(10), dp(12), dp(10));
            LinearLayout.LayoutParams layoutParams2 = new LinearLayout.LayoutParams(-1, -2);
            layoutParams2.bottomMargin = dp(6);
            textView4.setLayoutParams(layoutParams2);
            linearLayout.addView(textView4);
        }
        return linearLayout;
    }

    private LinearLayout buildToolsTab() {
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(1);
        linearLayout.setPadding(0, dp(6), 0, dp(24));
        linearLayout.addView(contactsSectionHeader("📡 AUTONOMOUS PEER MESH & NFC PAIRING", this.colCyan));
        linearLayout.addView(buildMeshPreviewCard());
        linearLayout.addView(contactsSectionHeader("🌤️ KINGSTON SITE WEATHER, THERMAL & HYDRATION", this.colCyan));
        linearLayout.addView(buildDetailedWeatherCard());
        linearLayout.addView(contactsSectionHeader("\u1faaa OFFICER CREDENTIALS & FIRST AID VAULT", this.colPale));
        linearLayout.addView(buildCredentialPreviewCard());
        linearLayout.addView(contactsSectionHeader("💡 TACTICAL LIGHTING (DOUBLE-CHOP SHAKE TO TOGGLE)", this.colAccent));
        linearLayout.addView(buildLightingGrid());
        linearLayout.addView(contactsSectionHeader("🧭 360° ROTATING COMPASS & SITE AZIMUTH", this.colCyan));
        linearLayout.addView(buildCompassCard());
        linearLayout.addView(contactsSectionHeader("🛰️ GPS TELEMETRY & SATELLITE RADAR", this.colEmerald));
        linearLayout.addView(buildGpsCard());
        return linearLayout;
    }

    private LinearLayout buildMeshPreviewCard() {
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(1);
        linearLayout.setBackground(rounded(this.colPanel, dp(16)));
        linearLayout.setPadding(dp(16), dp(14), dp(16), dp(14));
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, -2);
        layoutParams.bottomMargin = dp(12);
        linearLayout.setLayoutParams(layoutParams);
        LinearLayout linearLayout2 = new LinearLayout(this);
        linearLayout2.setOrientation(0);
        linearLayout2.setGravity(16);
        TextView textView = new TextView(this);
        textView.setText("📡 Offline BLE Mesh & NFC Sync");
        textView.setTextColor(this.colPale);
        textView.setTextSize(14.0f);
        textView.setTypeface(Typeface.DEFAULT_BOLD);
        textView.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));
        linearLayout2.addView(textView);
        TextView textView2 = new TextView(this);
        textView2.setText("ACTIVE");
        textView2.setTextColor(this.colEmerald);
        textView2.setTextSize(9.0f);
        textView2.setTypeface(Typeface.MONOSPACE);
        textView2.setPadding(dp(6), dp(2), dp(6), dp(2));
        textView2.setBackground(rounded(this.colEmeraldSoft, dp(4)));
        linearLayout2.addView(textView2);
        linearLayout.addView(linearLayout2);
        TextView textView3 = new TextView(this);
        textView3.setText("Autonomous zero-data peer sync with oncoming relief officers.\nInitial physical bump via NFC activates seamless background BLE sync.");
        textView3.setTextColor(this.colMuted);
        textView3.setTextSize(12.0f);
        textView3.setPadding(0, dp(4), 0, dp(10));
        linearLayout.addView(textView3);
        TextView fullActionButton = fullActionButton("🤝 Manage Trusted Peers & NFC Sync", this.colCyan, this.colAccentInk);
        fullActionButton.setOnClickListener(new View.OnClickListener() { // from class: au.com.dss.gatehouse.MainActivity.19
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                MainActivity.this.showNfcBleMeshDialog();
            }
        });
        linearLayout.addView(fullActionButton);
        return linearLayout;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void showNfcBleMeshDialog() {
        hapticHeavyClick();
        LinearLayout dialogContainer = dialogContainer("📡 NFC Bump & BLE Mesh", "OFF-GRID P2P RELAY", this.colCyan);
        TextView textView = new TextView(this);
        textView.setText("Decentralised peer-to-peer ledger sync for relief guards & patrol supervisors.\nTap phones back-to-back via NFC to pair trusted identity; logs sync automatically over BLE thereafter.");
        textView.setTextColor(this.colMuted);
        textView.setTextSize(12.0f);
        int i = 12;
        textView.setPadding(0, 0, 0, dp(12));
        dialogContainer.addView(textView);
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(1);
        linearLayout.setBackground(rounded(this.colPanel2, dp(14)));
        linearLayout.setPadding(dp(14), dp(12), dp(14), dp(12));
        int i2 = -1;
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, -2);
        layoutParams.bottomMargin = dp(14);
        linearLayout.setLayoutParams(layoutParams);
        LinearLayout linearLayout2 = new LinearLayout(this);
        linearLayout2.setOrientation(0);
        int i3 = 16;
        linearLayout2.setGravity(16);
        TextView textView2 = new TextView(this);
        textView2.setText("BLE MESH DAEMON: ACTIVE");
        textView2.setTextColor(this.colEmerald);
        textView2.setTextSize(11.0f);
        textView2.setTypeface(Typeface.create(Typeface.MONOSPACE, 1));
        textView2.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));
        linearLayout2.addView(textView2);
        TextView textView3 = new TextView(this);
        textView3.setText(this.trustedPeers.size() + " TRUSTED PEERS");
        textView3.setTextColor(this.colAccent);
        textView3.setTextSize(9.0f);
        textView3.setTypeface(Typeface.MONOSPACE);
        textView3.setPadding(dp(6), dp(2), dp(6), dp(2));
        textView3.setBackground(rounded(this.colPanel3, dp(4)));
        linearLayout2.addView(textView3);
        linearLayout.addView(linearLayout2);
        TextView textView4 = new TextView(this);
        textView4.setText("Auto-broadcasting SHA-256 block head (" + Core.head().substring(0, 8) + ") every 3.5s.\nSync range: ~35m radius across Kingston Gatehouse & yard.");
        textView4.setTextColor(this.colQuiet);
        textView4.setTextSize(11.0f);
        textView4.setPadding(0, dp(4), 0, 0);
        linearLayout.addView(textView4);
        dialogContainer.addView(linearLayout);
        dialogContainer.addView(formSectionLabel("TRUSTED ON-SITE OFFICERS"));
        Iterator<TrustedPeer> it = this.trustedPeers.iterator();
        while (it.hasNext()) {
            TrustedPeer next = it.next();
            LinearLayout linearLayout3 = new LinearLayout(this);
            linearLayout3.setOrientation(0);
            linearLayout3.setGravity(i3);
            linearLayout3.setBackground(rounded(this.colPanel3, dp(10)));
            linearLayout3.setPadding(dp(i), dp(10), dp(i), dp(10));
            LinearLayout.LayoutParams layoutParams2 = new LinearLayout.LayoutParams(i2, -2);
            layoutParams2.bottomMargin = dp(6);
            linearLayout3.setLayoutParams(layoutParams2);
            TextView textView5 = new TextView(this);
            textView5.setText("🛡️ " + next.name + " (" + next.licence + ")\n" + next.lastSeen);
            textView5.setTextColor(this.colPale);
            textView5.setTextSize(12.0f);
            textView5.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));
            linearLayout3.addView(textView5);
            TextView textView6 = new TextView(this);
            textView6.setText("✓ 100% SYNCED");
            textView6.setTextColor(this.colEmerald);
            textView6.setTextSize(9.0f);
            textView6.setTypeface(Typeface.MONOSPACE);
            textView6.setPadding(dp(6), dp(3), dp(6), dp(3));
            textView6.setBackground(rounded(this.colEmeraldSoft, dp(4)));
            linearLayout3.addView(textView6);
            dialogContainer.addView(linearLayout3);
            i = 12;
            i2 = -1;
            i3 = 16;
        }
        final Dialog createTacticalDialog = createTacticalDialog(dialogContainer);
        LinearLayout linearLayout4 = new LinearLayout(this);
        linearLayout4.setOrientation(0);
        linearLayout4.setPadding(0, dp(14), 0, 0);
        TextView actionButton = actionButton("🤝 NFC Bump Tap (Simulate)", this.colCyan, this.colAccentInk);
        actionButton.setOnClickListener(new View.OnClickListener() { // from class: au.com.dss.gatehouse.MainActivity.20
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                MainActivity.this.hapticSealThud();
                MainActivity.this.trustedPeers.add(new TrustedPeer("Patrol Supv #4", "LIC #38910", "Just Now (Gatehouse Proximity)"));
                MainActivity.this.banner.setText("🤝 NFC Handshake Verified: Supervisor LIC #38910 paired to trusted mesh!");
                MainActivity.this.banner.setVisibility(0);
                createTacticalDialog.dismiss();
            }
        });
        linearLayout4.addView(actionButton);
        TextView actionButton2 = actionButton("Close", this.colLine, this.colPale);
        actionButton2.setOnClickListener(new View.OnClickListener() { // from class: au.com.dss.gatehouse.MainActivity.21
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                MainActivity.this.hapticClick();
                createTacticalDialog.dismiss();
            }
        });
        LinearLayout.LayoutParams layoutParams3 = new LinearLayout.LayoutParams(0, -2, 0.8f);
        layoutParams3.leftMargin = dp(8);
        actionButton2.setLayoutParams(layoutParams3);
        linearLayout4.addView(actionButton2);
        dialogContainer.addView(linearLayout4);
        createTacticalDialog.show();
    }

    private LinearLayout buildCredentialPreviewCard() {
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(1);
        linearLayout.setBackground(rounded(this.colPanel, dp(16)));
        linearLayout.setPadding(dp(16), dp(14), dp(16), dp(14));
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, -2);
        layoutParams.bottomMargin = dp(12);
        linearLayout.setLayoutParams(layoutParams);
        LinearLayout linearLayout2 = new LinearLayout(this);
        linearLayout2.setOrientation(0);
        linearLayout2.setGravity(16);
        TextView textView = new TextView(this);
        textView.setText("🛡️ Officer R. Kelso · LIC #41207");
        textView.setTextColor(this.colPale);
        textView.setTextSize(14.0f);
        textView.setTypeface(Typeface.DEFAULT_BOLD);
        textView.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));
        linearLayout2.addView(textView);
        TextView textView2 = new TextView(this);
        textView2.setText("HOLOGRAPHIC");
        textView2.setTextColor(this.colAccent);
        textView2.setTextSize(9.0f);
        textView2.setTypeface(Typeface.MONOSPACE);
        textView2.setPadding(dp(6), dp(2), dp(6), dp(2));
        textView2.setBackground(rounded(this.colPanel2, dp(4)));
        linearLayout2.addView(textView2);
        linearLayout.addView(linearLayout2);
        TextView textView3 = new TextView(this);
        textView3.setText("QLD Security Class 1 Licence & St John HLTAID011 First Aid / CPR.\nTilt phone for holographic metallic shimmer & tap to flip verification QR.");
        textView3.setTextColor(this.colMuted);
        textView3.setTextSize(12.0f);
        textView3.setPadding(0, dp(4), 0, dp(10));
        linearLayout.addView(textView3);
        TextView fullActionButton = fullActionButton("\u1faaa Open Holographic Credential Vault", this.colAccent, this.colAccentInk);
        fullActionButton.setOnClickListener(new View.OnClickListener() { // from class: au.com.dss.gatehouse.MainActivity.22
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                MainActivity.this.showOfficerCredentialVaultDialog();
            }
        });
        linearLayout.addView(fullActionButton);
        return linearLayout;
    }

    private LinearLayout buildLightingGrid() {
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(1);
        linearLayout.setBackground(rounded(this.colPanel, dp(16)));
        linearLayout.setPadding(dp(14), dp(14), dp(14), dp(14));
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, -2);
        layoutParams.bottomMargin = dp(12);
        linearLayout.setLayoutParams(layoutParams);
        LinearLayout linearLayout2 = new LinearLayout(this);
        linearLayout2.setOrientation(0);
        linearLayout2.setGravity(16);
        linearLayout2.setPadding(0, 0, 0, dp(10));
        TextView textView = new TextView(this);
        textView.setText("🔦 TORCH INTENSITY:");
        textView.setTextColor(this.colQuiet);
        textView.setTextSize(10.0f);
        textView.setTypeface(Typeface.DEFAULT_BOLD);
        textView.setLetterSpacing(0.1f);
        textView.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));
        linearLayout2.addView(textView);
        TextView textView2 = torchLevelChip("20%", 20);
        TextView textView3 = torchLevelChip("50%", MAX_HELD);
        TextView textView4 = torchLevelChip("100%", 100);
        final ArrayList arrayList = new ArrayList();
        arrayList.add(textView2);
        arrayList.add(textView3);
        arrayList.add(textView4);
        Iterator it = arrayList.iterator();
        while (it.hasNext()) {
            final TextView textView5 = (TextView) it.next();
            textView5.setOnClickListener(new View.OnClickListener() { // from class: au.com.dss.gatehouse.MainActivity.23
                @Override // android.view.View.OnClickListener
                public void onClick(View view) {
                    MainActivity.this.hapticClick();
                    MainActivity.this.torchLevelPercent = ((Integer) textView5.getTag()).intValue();
                    Iterator it2 = arrayList.iterator();
                    while (true) {
                        if (!it2.hasNext()) {
                            break;
                        }
                        TextView textView6 = (TextView) it2.next();
                        boolean z = ((Integer) textView6.getTag()).intValue() == MainActivity.this.torchLevelPercent;
                        MainActivity mainActivity = MainActivity.this;
                        textView6.setTextColor(z ? mainActivity.colAccentInk : mainActivity.colMuted);
                        textView6.setBackground(MainActivity.this.rounded(z ? MainActivity.this.colAccent : MainActivity.this.colPanel2, MainActivity.this.dp(8)));
                    }
                    if (MainActivity.this.isHardwareTorchOn) {
                        MainActivity.this.setFlashTorch(true);
                    }
                }
            });
            linearLayout2.addView(textView5);
        }
        linearLayout.addView(linearLayout2);
        LinearLayout linearLayout3 = new LinearLayout(this);
        linearLayout3.setOrientation(0);
        linearLayout3.setPadding(0, 0, 0, dp(8));
        final TextView textView6 = toolLightButton("🔦 Torch", "STEADY BEAM", false);
        final TextView textView7 = toolLightButton("⚡ 10Hz Strobe", "DISORIENT", false);
        textView6.setOnClickListener(new View.OnClickListener() { // from class: au.com.dss.gatehouse.MainActivity.24
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                MainActivity.this.hapticClick();
                MainActivity.this.toggleHardwareTorch();
                MainActivity.this.updateLightButtonStates(textView6, textView7, null, null);
            }
        });
        textView7.setOnClickListener(new View.OnClickListener() { // from class: au.com.dss.gatehouse.MainActivity.25
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                MainActivity.this.hapticDoublePulse();
                MainActivity.this.toggleIntruderStrobe();
                MainActivity.this.updateLightButtonStates(textView6, textView7, null, null);
            }
        });
        linearLayout3.addView(textView6);
        linearLayout3.addView(textView7);
        linearLayout.addView(linearLayout3);
        LinearLayout linearLayout4 = new LinearLayout(this);
        linearLayout4.setOrientation(0);
        final TextView textView8 = toolLightButton("🆘 SOS Beacon", "MORSE PATTERN", false);
        TextView textView9 = toolLightButton("🏮 0-Lux Red Lantern", "NIGHT VISION", false);
        textView8.setOnClickListener(new View.OnClickListener() { // from class: au.com.dss.gatehouse.MainActivity.26
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                MainActivity.this.hapticDoublePulse();
                MainActivity.this.toggleSosBeacon();
                MainActivity.this.updateLightButtonStates(textView6, textView7, textView8, null);
            }
        });
        textView9.setOnClickListener(new View.OnClickListener() { // from class: au.com.dss.gatehouse.MainActivity.27
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                MainActivity.this.hapticClick();
                MainActivity.this.showRedLanternDialog();
            }
        });
        linearLayout4.addView(textView8);
        linearLayout4.addView(textView9);
        linearLayout.addView(linearLayout4);
        TextView textView10 = new TextView(this);
        textView10.setText("💡 Tip: Double-chop wrist shake toggles torch instantly anytime.");
        textView10.setTextColor(this.colQuiet);
        textView10.setTextSize(10.0f);
        textView10.setPadding(0, dp(8), 0, 0);
        linearLayout.addView(textView10);
        return linearLayout;
    }

    private TextView torchLevelChip(String str, int i) {
        TextView textView = new TextView(this);
        textView.setText(str);
        textView.setTag(Integer.valueOf(i));
        textView.setTextSize(10.0f);
        textView.setTypeface(Typeface.MONOSPACE);
        textView.setPadding(dp(8), dp(4), dp(8), dp(4));
        boolean z = i == this.torchLevelPercent;
        textView.setTextColor(z ? this.colAccentInk : this.colMuted);
        textView.setBackground(rounded(z ? this.colAccent : this.colPanel2, dp(8)));
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-2, -2);
        layoutParams.leftMargin = dp(4);
        textView.setLayoutParams(layoutParams);
        return textView;
    }

    private TextView toolLightButton(String str, String str2, boolean z) {
        TextView textView = new TextView(this);
        textView.setText(str + "\n" + str2);
        textView.setTextSize(12.0f);
        textView.setTypeface(Typeface.DEFAULT_BOLD);
        textView.setGravity(17);
        textView.setTextColor(z ? this.colAccentInk : this.colPale);
        textView.setPadding(dp(12), dp(14), dp(12), dp(14));
        textView.setBackground(z ? rounded(this.colAccent, dp(12)) : pressable(this.colPanel2, dp(12)));
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(0, -2, 1.0f);
        layoutParams.leftMargin = dp(3);
        layoutParams.rightMargin = dp(3);
        textView.setLayoutParams(layoutParams);
        return textView;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateLightButtonStates(TextView textView, TextView textView2, TextView textView3, TextView textView4) {
        if (textView != null) {
            textView.setTextColor(this.isHardwareTorchOn ? this.colAccentInk : this.colPale);
            textView.setBackground(this.isHardwareTorchOn ? rounded(this.colAccent, dp(12)) : pressable(this.colPanel2, dp(12)));
        }
        if (textView2 != null) {
            textView2.setTextColor(this.isStrobeActive ? this.colAccentInk : this.colPale);
            textView2.setBackground(this.isStrobeActive ? rounded(this.colCrimson, dp(12)) : pressable(this.colPanel2, dp(12)));
        }
        if (textView3 != null) {
            textView3.setTextColor(this.isSosActive ? this.colAccentInk : this.colPale);
            textView3.setBackground(this.isSosActive ? rounded(this.colCyan, dp(12)) : pressable(this.colPanel2, dp(12)));
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void toggleHardwareTorch() {
        this.isStrobeActive = false;
        this.isSosActive = false;
        this.lightHandler.removeCallbacksAndMessages(null);
        this.isHardwareTorchOn = !this.isHardwareTorchOn;
        setFlashTorch(this.isHardwareTorchOn);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void toggleIntruderStrobe() {
        this.isHardwareTorchOn = false;
        this.isSosActive = false;
        this.lightHandler.removeCallbacksAndMessages(null);
        this.isStrobeActive = !this.isStrobeActive;
        if (this.isStrobeActive) {
            startStrobeLoop();
        } else {
            setFlashTorch(false);
        }
    }

    private void startStrobeLoop() {
        final boolean[] zArr = {false};
        this.lightHandler.post(new Runnable() { // from class: au.com.dss.gatehouse.MainActivity.28
            @Override // java.lang.Runnable
            public void run() {
                if (MainActivity.this.isStrobeActive) {
                    zArr[0] = !zArr[0];
                    MainActivity.this.setFlashTorch(zArr[0]);
                    MainActivity.this.lightHandler.postDelayed(this, 50L);
                    return;
                }
                MainActivity.this.setFlashTorch(false);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void toggleSosBeacon() {
        this.isHardwareTorchOn = false;
        this.isStrobeActive = false;
        this.lightHandler.removeCallbacksAndMessages(null);
        this.isSosActive = !this.isSosActive;
        if (this.isSosActive) {
            startSosLoop();
        } else {
            setFlashTorch(false);
        }
    }

    private void startSosLoop() {
        final int[] iArr = {100, 100, 100, 100, 100, 250, 300, 100, 300, 100, 300, 250, 100, 100, 100, 100, 100, 1200};
        final int[] iArr2 = {0};
        this.lightHandler.post(new Runnable() { // from class: au.com.dss.gatehouse.MainActivity.29
            @Override // java.lang.Runnable
            public void run() {
                if (MainActivity.this.isSosActive) {
                    MainActivity.this.setFlashTorch(iArr2[0] % 2 == 0);
                    int i = iArr[iArr2[0]];
                    iArr2[0] = (iArr2[0] + 1) % iArr.length;
                    MainActivity.this.lightHandler.postDelayed(this, i);
                    return;
                }
                MainActivity.this.setFlashTorch(false);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setFlashTorch(boolean z) {
        if (this.rearCameraId == null) {
            return;
        }
        CameraManager cameraManager = (CameraManager) getSystemService("camera");
        try {
            if (Build.VERSION.SDK_INT >= 33 && z) {
                try {
                    Integer num = (Integer) cameraManager.getCameraCharacteristics(this.rearCameraId).get(CameraCharacteristics.FLASH_INFO_STRENGTH_MAXIMUM_LEVEL);
                    if (num != null && num.intValue() > 1) {
                        cameraManager.turnOnTorchWithStrengthLevel(this.rearCameraId, Math.max(1, (int) (num.intValue() * (this.torchLevelPercent / 100.0f))));
                        return;
                    }
                } catch (Exception e) {
                }
            }
            cameraManager.setTorchMode(this.rearCameraId, z);
        } catch (Exception e2) {
        }
    }

    private void stopLightingModes() {
        this.isHardwareTorchOn = false;
        this.isStrobeActive = false;
        this.isSosActive = false;
        this.lightHandler.removeCallbacksAndMessages(null);
        setFlashTorch(false);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void showRedLanternDialog() {
        final Dialog dialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.requestWindowFeature(1);
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(1);
        linearLayout.setGravity(17);
        linearLayout.setBackgroundColor(-61167);
        linearLayout.setPadding(dp(24), dp(24), dp(24), dp(24));
        TextView textView = new TextView(this);
        textView.setText("🏮");
        textView.setTextSize(48.0f);
        textView.setGravity(17);
        linearLayout.addView(textView);
        TextView textView2 = new TextView(this);
        textView2.setText("0-LUX RED NIGHT LANTERN");
        textView2.setTextColor(-16777216);
        textView2.setTextSize(18.0f);
        textView2.setTypeface(Typeface.DEFAULT_BOLD);
        textView2.setPadding(0, dp(12), 0, dp(4));
        textView2.setGravity(17);
        linearLayout.addView(textView2);
        TextView textView3 = new TextView(this);
        textView3.setText("Full-screen red light for padlock inspection & paperwork.\nPreserves natural night eye vision (rhodopsin).\n\n[ Tap anywhere to close ]");
        textView3.setTextColor(-12320768);
        textView3.setTextSize(13.0f);
        textView3.setGravity(17);
        linearLayout.addView(textView3);
        linearLayout.setOnClickListener(new View.OnClickListener() { // from class: au.com.dss.gatehouse.MainActivity.30
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                MainActivity.this.hapticClick();
                dialog.dismiss();
            }
        });
        dialog.setContentView(linearLayout);
        dialog.show();
    }

    private LinearLayout buildDetailedWeatherCard() {
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(1);
        linearLayout.setBackground(rounded(this.colPanel, dp(16)));
        linearLayout.setPadding(dp(16), dp(16), dp(16), dp(16));
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, -2);
        layoutParams.bottomMargin = dp(12);
        linearLayout.setLayoutParams(layoutParams);
        LinearLayout linearLayout2 = new LinearLayout(this);
        linearLayout2.setOrientation(0);
        linearLayout2.setGravity(16);
        linearLayout2.setPadding(0, 0, 0, dp(10));
        TextView textView = new TextView(this);
        textView.setText("🌤️ KINGSTON ATMOSPHERIC RADAR");
        textView.setTextColor(this.colPale);
        textView.setTextSize(13.0f);
        textView.setTypeface(Typeface.DEFAULT_BOLD);
        textView.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));
        linearLayout2.addView(textView);
        TextView textView2 = new TextView(this);
        textView2.setText("BOM LOGAN LIVE");
        textView2.setTextColor(this.colEmerald);
        textView2.setTextSize(9.0f);
        textView2.setTypeface(Typeface.MONOSPACE);
        textView2.setPadding(dp(6), dp(2), dp(6), dp(2));
        textView2.setBackground(rounded(this.colEmeraldSoft, dp(4)));
        linearLayout2.addView(textView2);
        linearLayout.addView(linearLayout2);
        LinearLayout linearLayout3 = new LinearLayout(this);
        linearLayout3.setOrientation(0);
        linearLayout3.setPadding(0, 0, 0, dp(8));
        linearLayout3.addView(weatherMetricBox("TEMPERATURE", String.format(Locale.US, "%.1f°C", Double.valueOf(this.curTempC)), String.format(Locale.US, "Feels %.1f°C", Double.valueOf(this.curFeelsLikeC)), this.colPale));
        linearLayout3.addView(weatherMetricBox("☀️ UV INDEX", String.format(Locale.US, "%.1f UV", Double.valueOf(this.curUvIndex)), "0.0 Night · Day Max 7.8", this.colAccent));
        linearLayout.addView(linearLayout3);
        LinearLayout linearLayout4 = new LinearLayout(this);
        linearLayout4.setOrientation(0);
        linearLayout4.setPadding(0, 0, 0, dp(12));
        linearLayout4.addView(weatherMetricBox("HUMIDITY / DEW", this.curHumidity + "% RH", String.format(Locale.US, "Dew Pt %.1f°C", Double.valueOf(this.curDewPointC)), this.colCyan));
        linearLayout4.addView(weatherMetricBox("WIND & GUSTS", String.format(Locale.US, "%.1f km/h", Double.valueOf(this.curWindSpeedKmh)), this.curWindDir + " · Gust " + this.curWindGustKmh, this.colEmerald));
        linearLayout.addView(linearLayout4);
        LinearLayout linearLayout5 = new LinearLayout(this);
        linearLayout5.setOrientation(0);
        linearLayout5.setPadding(0, 0, 0, dp(12));
        linearLayout5.addView(weatherMetricBox("🥶 COLD STRESS", String.format(Locale.US, "%.1f°C Chill", Double.valueOf(this.curTempC - (this.curWindSpeedKmh * 0.12d))), "Low Risk · Cabin Warm OK", this.colCyan));
        final TextView textView3 = new TextView(this);
        textView3.setText(this.waterIntakeMl + " / 2000 ml");
        textView3.setTextColor(this.colEmerald);
        textView3.setTextSize(14.0f);
        textView3.setTypeface(Typeface.DEFAULT_BOLD);
        textView3.setPadding(0, dp(2), 0, dp(1));
        LinearLayout linearLayout6 = new LinearLayout(this);
        linearLayout6.setOrientation(1);
        linearLayout6.setBackground(rounded(this.colPanel2, dp(12)));
        linearLayout6.setPadding(dp(12), dp(10), dp(12), dp(10));
        LinearLayout.LayoutParams layoutParams2 = new LinearLayout.LayoutParams(0, -2, 1.0f);
        layoutParams2.leftMargin = dp(3);
        layoutParams2.rightMargin = dp(3);
        linearLayout6.setLayoutParams(layoutParams2);
        TextView textView4 = new TextView(this);
        textView4.setText("💧 HYDRATION PACER");
        textView4.setTextColor(this.colQuiet);
        textView4.setTextSize(9.0f);
        textView4.setTypeface(Typeface.DEFAULT_BOLD);
        linearLayout6.addView(textView4);
        linearLayout6.addView(textView3);
        TextView textView5 = new TextView(this);
        textView5.setText(((int) ((this.waterIntakeMl * 100.0d) / 2000.0d)) + "% of 2.0L Shift Goal");
        textView5.setTextColor(this.colMuted);
        textView5.setTextSize(9.0f);
        linearLayout6.addView(textView5);
        linearLayout5.addView(linearLayout6);
        linearLayout.addView(linearLayout5);
        LinearLayout linearLayout7 = new LinearLayout(this);
        linearLayout7.setOrientation(0);
        TextView actionButton = actionButton("📍 Embed Weather to Log", this.colAccent, this.colAccentInk);
        actionButton.setOnClickListener(new View.OnClickListener() { // from class: au.com.dss.gatehouse.MainActivity.31
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                MainActivity.this.hapticHeavyClick();
                MainActivity.this.note(0, String.format(Locale.US, "[WEATHER] %.1f°C (Feels %.1f°C, Chill %.1f°C) · Hum: %d%% · Baro: %.1fhPa · Wind: %.1fkm/h %s · UV: %.1f · Hydration: %dml", Double.valueOf(MainActivity.this.curTempC), Double.valueOf(MainActivity.this.curFeelsLikeC), Double.valueOf(MainActivity.this.curTempC - (MainActivity.this.curWindSpeedKmh * 0.12d)), Integer.valueOf(MainActivity.this.curHumidity), Double.valueOf(MainActivity.this.curPressureHpa), Double.valueOf(MainActivity.this.curWindSpeedKmh), MainActivity.this.curWindDir, Double.valueOf(MainActivity.this.curUvIndex), Integer.valueOf(MainActivity.this.waterIntakeMl)));
                MainActivity.this.banner.setText("✓ Kingston weather & thermal telemetry logged to Ada record");
                MainActivity.this.banner.setVisibility(0);
            }
        });
        linearLayout7.addView(actionButton);
        TextView actionButton2 = actionButton("💧 +250ml Water", this.colPanel2, this.colCyan);
        actionButton2.setOnClickListener(new View.OnClickListener() { // from class: au.com.dss.gatehouse.MainActivity.32
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                MainActivity.this.hapticClick();
                MainActivity.this.waterIntakeMl = Math.min(3000, MainActivity.this.waterIntakeMl + 250);
                textView3.setText(MainActivity.this.waterIntakeMl + " / 2000 ml");
                MainActivity.this.banner.setText("✓ Logged +250ml water (" + MainActivity.this.waterIntakeMl + "ml / 2000ml target)");
                MainActivity.this.banner.setVisibility(0);
            }
        });
        LinearLayout.LayoutParams layoutParams3 = new LinearLayout.LayoutParams(0, -2, 0.8f);
        layoutParams3.leftMargin = dp(6);
        actionButton2.setLayoutParams(layoutParams3);
        linearLayout7.addView(actionButton2);
        linearLayout.addView(linearLayout7);
        return linearLayout;
    }

    private LinearLayout weatherMetricBox(String str, String str2, String str3, int i) {
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(1);
        linearLayout.setBackground(rounded(this.colPanel2, dp(12)));
        linearLayout.setPadding(dp(12), dp(10), dp(12), dp(10));
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(0, -2, 1.0f);
        layoutParams.leftMargin = dp(3);
        layoutParams.rightMargin = dp(3);
        linearLayout.setLayoutParams(layoutParams);
        TextView textView = new TextView(this);
        textView.setText(str);
        textView.setTextColor(this.colQuiet);
        textView.setTextSize(9.0f);
        textView.setTypeface(Typeface.DEFAULT_BOLD);
        textView.setLetterSpacing(0.1f);
        linearLayout.addView(textView);
        TextView textView2 = new TextView(this);
        textView2.setText(str2);
        textView2.setTextColor(i);
        textView2.setTextSize(14.0f);
        textView2.setTypeface(Typeface.DEFAULT_BOLD);
        textView2.setPadding(0, dp(2), 0, dp(1));
        linearLayout.addView(textView2);
        TextView textView3 = new TextView(this);
        textView3.setText(str3);
        textView3.setTextColor(this.colMuted);
        textView3.setTextSize(9.0f);
        linearLayout.addView(textView3);
        return linearLayout;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class HolographicCardView extends View {
        private final Paint borderPaint;
        private final Paint cardBgPaint;
        private int cardMode;
        private final RectF cardRect;
        private final Paint goldEmbossPaint;
        private final Path guillochePath;
        private boolean isFlipped;
        private final Paint qrPaint;
        private final Paint shimmerPaint;
        private final Paint subTextPaint;
        private final Paint textPaint;

        public HolographicCardView(Context context) {
            super(context);
            this.cardBgPaint = new Paint(1);
            this.borderPaint = new Paint(1);
            this.goldEmbossPaint = new Paint(1);
            this.shimmerPaint = new Paint(1);
            this.textPaint = new Paint(1);
            this.subTextPaint = new Paint(1);
            this.qrPaint = new Paint(1);
            this.guillochePath = new Path();
            this.cardRect = new RectF();
            this.cardMode = 0;
            this.isFlipped = false;
            this.cardBgPaint.setStyle(Paint.Style.FILL);
            this.borderPaint.setStyle(Paint.Style.STROKE);
            this.borderPaint.setStrokeWidth(MainActivity.this.dp(2));
            this.goldEmbossPaint.setStyle(Paint.Style.STROKE);
            this.goldEmbossPaint.setStrokeWidth(MainActivity.this.dp(1));
            this.shimmerPaint.setStyle(Paint.Style.FILL);
            this.textPaint.setTypeface(Typeface.create(Typeface.MONOSPACE, 1));
            this.subTextPaint.setTypeface(Typeface.create(Typeface.DEFAULT, 1));
        }

        public void setCardMode(int i) {
            this.cardMode = i;
            this.isFlipped = false;
            invalidate();
        }

        public void toggleFlip() {
            this.isFlipped = !this.isFlipped;
            invalidate();
        }

        @Override // android.view.View
        protected void onDraw(Canvas canvas) {
            int i;
            int i2;
            int i3;
            int i4;
            float f;
            float f2;
            super.onDraw(canvas);
            int width = getWidth();
            int height = getHeight();
            if (width > 0 && height > 0) {
                this.cardRect.set(MainActivity.this.dp(6), MainActivity.this.dp(6), width - MainActivity.this.dp(6), height - MainActivity.this.dp(6));
                this.cardBgPaint.setColor(this.cardMode == 0 ? -15920094 : -16114666);
                int i5 = 16;
                canvas.drawRoundRect(this.cardRect, MainActivity.this.dp(16), MainActivity.this.dp(16), this.cardBgPaint);
                this.guillochePath.reset();
                Paint paint = new Paint(1);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(MainActivity.this.dp(1));
                paint.setColor(this.cardMode == 0 ? 317040956 : 336640385);
                int dp = MainActivity.this.dp(16);
                while (true) {
                    i = 4;
                    if (dp >= height - MainActivity.this.dp(i5)) {
                        break;
                    }
                    float f3 = dp;
                    this.guillochePath.moveTo(MainActivity.this.dp(i5), f3);
                    int dp2 = MainActivity.this.dp(i5);
                    while (dp2 < width - MainActivity.this.dp(i5)) {
                        this.guillochePath.lineTo(dp2, (((float) Math.sin((dp2 + dp) * 0.1f)) * MainActivity.this.dp(4)) + f3);
                        dp2 += MainActivity.this.dp(24);
                        i5 = 16;
                    }
                    dp += MainActivity.this.dp(14);
                    i5 = 16;
                }
                canvas.drawPath(this.guillochePath, paint);
                float f4 = MainActivity.this.lastAccel[0];
                float f5 = MainActivity.this.lastAccel[1];
                float max = width * Math.max(0.1f, Math.min(0.9f, (f4 + 6.0f) / 12.0f));
                float max2 = height * Math.max(0.1f, Math.min(0.9f, (f5 + 6.0f) / 12.0f));
                this.shimmerPaint.setShader(new LinearGradient(max - MainActivity.this.dp(120), max2 - MainActivity.this.dp(120), max + MainActivity.this.dp(120), max2 + MainActivity.this.dp(120), new int[]{16777215, 419430399, 1290119484, 1711716052, 587158869, 16777215}, new float[]{0.0f, 0.2f, 0.4f, 0.6f, 0.8f, 1.0f}, Shader.TileMode.CLAMP));
                canvas.drawRoundRect(this.cardRect, MainActivity.this.dp(16), MainActivity.this.dp(16), this.shimmerPaint);
                this.borderPaint.setColor(this.cardMode == 0 ? MainActivity.this.colAccent : MainActivity.this.colEmerald);
                canvas.drawRoundRect(this.cardRect, MainActivity.this.dp(16), MainActivity.this.dp(16), this.borderPaint);
                this.goldEmbossPaint.setColor(this.cardMode == 0 ? 1155901756 : 1141946753);
                canvas.drawRoundRect(new RectF(this.cardRect.left + MainActivity.this.dp(4), this.cardRect.top + MainActivity.this.dp(4), this.cardRect.right - MainActivity.this.dp(4), this.cardRect.bottom - MainActivity.this.dp(4)), MainActivity.this.dp(12), MainActivity.this.dp(12), this.goldEmbossPaint);
                if (this.isFlipped) {
                    this.textPaint.setColor(MainActivity.this.colAccent);
                    this.textPaint.setTextSize(MainActivity.this.dp(10));
                    this.textPaint.setTextAlign(Paint.Align.LEFT);
                    canvas.drawText("DIGITAL AUDIT & JURISDICTION VERIFICATION", MainActivity.this.dp(18), MainActivity.this.dp(26), this.textPaint);
                    float dp3 = MainActivity.this.dp(110);
                    float dp4 = MainActivity.this.dp(18);
                    float dp5 = MainActivity.this.dp(38);
                    this.qrPaint.setColor(-1);
                    float f6 = dp4 + dp3;
                    canvas.drawRoundRect(new RectF(dp4, dp5, f6, dp5 + dp3), MainActivity.this.dp(8), MainActivity.this.dp(8), this.qrPaint);
                    this.qrPaint.setColor(-16777216);
                    float f7 = dp3 / 15.0f;
                    int i6 = 0;
                    while (true) {
                        int i7 = 11;
                        if (i6 >= 15) {
                            float f8 = f6;
                            float f9 = dp5;
                            this.textPaint.setColor(MainActivity.this.colPale);
                            this.textPaint.setTextSize(MainActivity.this.dp(11));
                            this.textPaint.setTextAlign(Paint.Align.LEFT);
                            canvas.drawText("QLD REGULATED ID", f8 + MainActivity.this.dp(14), f9 + MainActivity.this.dp(16), this.textPaint);
                            this.textPaint.setColor(MainActivity.this.colMuted);
                            this.textPaint.setTextSize(MainActivity.this.dp(9));
                            canvas.drawText("HASH: 7f8a9b2c...41207", f8 + MainActivity.this.dp(14), f9 + MainActivity.this.dp(34), this.textPaint);
                            canvas.drawText("CHAIN: SHA-256 SPARK", f8 + MainActivity.this.dp(14), f9 + MainActivity.this.dp(MainActivity.MAX_HELD), this.textPaint);
                            canvas.drawText("SECURITY LIC: #41207", f8 + MainActivity.this.dp(14), f9 + MainActivity.this.dp(66), this.textPaint);
                            canvas.drawText("FIRST AID: SJA-849102", f8 + MainActivity.this.dp(14), f9 + MainActivity.this.dp(82), this.textPaint);
                            this.textPaint.setColor(MainActivity.this.colEmerald);
                            this.textPaint.setTextSize(MainActivity.this.dp(9));
                            canvas.drawText("✓ SIGNED IMMUTABLE", f8 + MainActivity.this.dp(14), f9 + MainActivity.this.dp(100), this.textPaint);
                            this.textPaint.setColor(MainActivity.this.colAccent);
                            this.textPaint.setTextSize(MainActivity.this.dp(9));
                            canvas.drawText("🔄 TAP CARD TO FLIP BACK", MainActivity.this.dp(18), height - MainActivity.this.dp(14), this.textPaint);
                            return;
                        }
                        int i8 = 0;
                        while (i8 < 15) {
                            boolean z = i6 < i && i8 < i;
                            boolean z2 = i6 < i && i8 >= i7;
                            boolean z3 = i6 >= i7 && i8 < i;
                            boolean z4 = (((i6 * 7) + (i8 * 13)) + ((this.cardMode + 1) * 19)) % 3 == 0;
                            if (z || z2 || z3 || z4) {
                                i2 = i8;
                                i3 = 11;
                                i4 = i6;
                                f = f6;
                                f2 = dp5;
                                canvas.drawRect(dp4 + (i8 * f7), dp5 + (i6 * f7), dp4 + ((i8 + 1) * f7), dp5 + ((i6 + 1) * f7), this.qrPaint);
                            } else {
                                i2 = i8;
                                i4 = i6;
                                f = f6;
                                f2 = dp5;
                                i3 = 11;
                            }
                            i8 = i2 + 1;
                            i7 = i3;
                            i6 = i4;
                            f6 = f;
                            dp5 = f2;
                            i = 4;
                        }
                        i6++;
                        i = 4;
                    }
                } else {
                    if (this.cardMode == 0) {
                        this.textPaint.setColor(MainActivity.this.colAccent);
                        this.textPaint.setTextSize(MainActivity.this.dp(9));
                        this.textPaint.setTextAlign(Paint.Align.LEFT);
                        canvas.drawText("QUEENSLAND GOVERNMENT · OFFICE OF FAIR TRADING", MainActivity.this.dp(18), MainActivity.this.dp(26), this.textPaint);
                        this.subTextPaint.setColor(MainActivity.this.colPale);
                        this.subTextPaint.setTextSize(MainActivity.this.dp(13));
                        this.subTextPaint.setTextAlign(Paint.Align.LEFT);
                        canvas.drawText("SECURITY PROVIDERS ACT 1993 · CLASS 1", MainActivity.this.dp(18), MainActivity.this.dp(44), this.subTextPaint);
                        this.textPaint.setColor(MainActivity.this.colMuted);
                        this.textPaint.setTextSize(MainActivity.this.dp(9));
                        canvas.drawText("LICENCE HOLDER:", MainActivity.this.dp(18), MainActivity.this.dp(66), this.textPaint);
                        this.subTextPaint.setColor(MainActivity.this.colPale);
                        this.subTextPaint.setTextSize(MainActivity.this.dp(16));
                        canvas.drawText("KELSO, R.", MainActivity.this.dp(18), MainActivity.this.dp(84), this.subTextPaint);
                        this.textPaint.setColor(MainActivity.this.colMuted);
                        this.textPaint.setTextSize(MainActivity.this.dp(9));
                        canvas.drawText("LICENCE NUMBER:", MainActivity.this.dp(18), MainActivity.this.dp(104), this.textPaint);
                        this.textPaint.setColor(MainActivity.this.colAccent);
                        this.textPaint.setTextSize(MainActivity.this.dp(14));
                        canvas.drawText("41207 / SEC-1-QLD", MainActivity.this.dp(18), MainActivity.this.dp(120), this.textPaint);
                        this.textPaint.setColor(MainActivity.this.colMuted);
                        this.textPaint.setTextSize(MainActivity.this.dp(9));
                        canvas.drawText("FUNCTIONS: 1A UNARMED GUARD · 1C CROWD · STATIC", MainActivity.this.dp(18), MainActivity.this.dp(138), this.textPaint);
                        canvas.drawText("EMPLOYER: DOHERTY SECURITY SERVICES (#389102)", MainActivity.this.dp(18), MainActivity.this.dp(152), this.textPaint);
                        Paint paint2 = new Paint(1);
                        paint2.setColor(MainActivity.this.colEmeraldSoft);
                        RectF rectF = new RectF(width - MainActivity.this.dp(110), MainActivity.this.dp(22), width - MainActivity.this.dp(18), MainActivity.this.dp(44));
                        canvas.drawRoundRect(rectF, MainActivity.this.dp(6), MainActivity.this.dp(6), paint2);
                        this.textPaint.setColor(MainActivity.this.colEmerald);
                        this.textPaint.setTextSize(MainActivity.this.dp(9));
                        this.textPaint.setTextAlign(Paint.Align.CENTER);
                        canvas.drawText("✓ CURRENT & ACTIVE", rectF.centerX(), rectF.centerY() + MainActivity.this.dp(3), this.textPaint);
                        this.textPaint.setColor(MainActivity.this.colQuiet);
                        this.textPaint.setTextSize(MainActivity.this.dp(8));
                        this.textPaint.setTextAlign(Paint.Align.RIGHT);
                        canvas.drawText("EXP: 14 OCT 2027", width - MainActivity.this.dp(18), height - MainActivity.this.dp(14), this.textPaint);
                    } else {
                        this.textPaint.setColor(MainActivity.this.colEmerald);
                        this.textPaint.setTextSize(MainActivity.this.dp(9));
                        this.textPaint.setTextAlign(Paint.Align.LEFT);
                        canvas.drawText("NATIONALLY RECOGNISED TRAINING · RTO #8801", MainActivity.this.dp(18), MainActivity.this.dp(26), this.textPaint);
                        this.subTextPaint.setColor(MainActivity.this.colPale);
                        this.subTextPaint.setTextSize(MainActivity.this.dp(13));
                        this.subTextPaint.setTextAlign(Paint.Align.LEFT);
                        canvas.drawText("HLTAID011 PROVIDE FIRST AID & CPR", MainActivity.this.dp(18), MainActivity.this.dp(44), this.subTextPaint);
                        this.textPaint.setColor(MainActivity.this.colMuted);
                        this.textPaint.setTextSize(MainActivity.this.dp(9));
                        canvas.drawText("CERTIFIED PRACTITIONER:", MainActivity.this.dp(18), MainActivity.this.dp(66), this.textPaint);
                        this.subTextPaint.setColor(MainActivity.this.colPale);
                        this.subTextPaint.setTextSize(MainActivity.this.dp(16));
                        canvas.drawText("Officer R. Kelso", MainActivity.this.dp(18), MainActivity.this.dp(84), this.subTextPaint);
                        this.textPaint.setColor(MainActivity.this.colMuted);
                        this.textPaint.setTextSize(MainActivity.this.dp(9));
                        canvas.drawText("TRAINING BODY: St John Ambulance Australia", MainActivity.this.dp(18), MainActivity.this.dp(104), this.textPaint);
                        this.textPaint.setColor(MainActivity.this.colEmerald);
                        this.textPaint.setTextSize(MainActivity.this.dp(12));
                        canvas.drawText("CERT ID: SJA-QLD-849102-K", MainActivity.this.dp(18), MainActivity.this.dp(120), this.textPaint);
                        this.textPaint.setColor(MainActivity.this.colMuted);
                        this.textPaint.setTextSize(MainActivity.this.dp(9));
                        canvas.drawText("CPR RE-CERT DUE: 12 MAR 2026 (Annual Compliant)", MainActivity.this.dp(18), MainActivity.this.dp(138), this.textPaint);
                        canvas.drawText("FIRST AID EXPIRY: 12 MAR 2028 (3-Yr Triennial)", MainActivity.this.dp(18), MainActivity.this.dp(152), this.textPaint);
                        Paint paint3 = new Paint(1);
                        paint3.setColor(MainActivity.this.colEmeraldSoft);
                        RectF rectF2 = new RectF(width - MainActivity.this.dp(116), MainActivity.this.dp(22), width - MainActivity.this.dp(18), MainActivity.this.dp(44));
                        canvas.drawRoundRect(rectF2, MainActivity.this.dp(6), MainActivity.this.dp(6), paint3);
                        this.textPaint.setColor(MainActivity.this.colEmerald);
                        this.textPaint.setTextSize(MainActivity.this.dp(9));
                        this.textPaint.setTextAlign(Paint.Align.CENTER);
                        canvas.drawText("✓ WHS COMPLIANT", rectF2.centerX(), rectF2.centerY() + MainActivity.this.dp(3), this.textPaint);
                        this.textPaint.setColor(MainActivity.this.colQuiet);
                        this.textPaint.setTextSize(MainActivity.this.dp(8));
                        this.textPaint.setTextAlign(Paint.Align.RIGHT);
                        canvas.drawText("ANNUAL CPR REFRESHED", width - MainActivity.this.dp(18), height - MainActivity.this.dp(14), this.textPaint);
                    }
                    this.textPaint.setColor(MainActivity.this.colAccent);
                    this.textPaint.setTextSize(MainActivity.this.dp(9));
                    this.textPaint.setTextAlign(Paint.Align.LEFT);
                    canvas.drawText("🔄 TAP CARD TO FLIP VERIFICATION QR", MainActivity.this.dp(18), height - MainActivity.this.dp(14), this.textPaint);
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void showOfficerCredentialVaultDialog() {
        hapticHeavyClick();
        LinearLayout dialogContainer = dialogContainer("\u1faaa Officer Credential Vault", "LEGAL AUDIT", this.colAccent);
        TextView textView = new TextView(this);
        textView.setText("Verified credentials for static guarding & WHS compliance:\n(Physically tilt phone to see holographic shimmer reflection)");
        textView.setTextColor(this.colMuted);
        textView.setTextSize(12.0f);
        textView.setPadding(0, 0, 0, dp(10));
        dialogContainer.addView(textView);
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(0);
        linearLayout.setBackground(rounded(this.colPanel2, dp(14)));
        linearLayout.setPadding(dp(3), dp(3), dp(3), dp(3));
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, -2);
        layoutParams.bottomMargin = dp(12);
        linearLayout.setLayoutParams(layoutParams);
        final TextView textView2 = new TextView(this);
        textView2.setText("🛡️ QLD Security Licence");
        textView2.setTextSize(11.0f);
        textView2.setTypeface(Typeface.DEFAULT_BOLD);
        textView2.setGravity(17);
        textView2.setPadding(dp(8), dp(8), dp(8), dp(8));
        textView2.setTextColor(this.colAccentInk);
        textView2.setBackground(rounded(this.colAccent, dp(10)));
        textView2.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));
        linearLayout.addView(textView2);
        final TextView textView3 = new TextView(this);
        textView3.setText("🩹 First Aid & CPR");
        textView3.setTextSize(11.0f);
        textView3.setTypeface(Typeface.DEFAULT_BOLD);
        textView3.setGravity(17);
        textView3.setPadding(dp(8), dp(8), dp(8), dp(8));
        textView3.setTextColor(this.colMuted);
        textView3.setBackground(null);
        textView3.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));
        linearLayout.addView(textView3);
        dialogContainer.addView(linearLayout);
        final HolographicCardView holographicCardView = new HolographicCardView(this);
        this.activeHoloCard = holographicCardView;
        LinearLayout.LayoutParams layoutParams2 = new LinearLayout.LayoutParams(-1, dp(190));
        layoutParams2.bottomMargin = dp(12);
        holographicCardView.setLayoutParams(layoutParams2);
        holographicCardView.setOnClickListener(new View.OnClickListener() { // from class: au.com.dss.gatehouse.MainActivity.33
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                MainActivity.this.hapticDoublePulse();
                holographicCardView.toggleFlip();
            }
        });
        dialogContainer.addView(holographicCardView);
        textView2.setOnClickListener(new View.OnClickListener() { // from class: au.com.dss.gatehouse.MainActivity.34
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                MainActivity.this.hapticClick();
                holographicCardView.setCardMode(0);
                textView2.setTextColor(MainActivity.this.colAccentInk);
                textView2.setBackground(MainActivity.this.rounded(MainActivity.this.colAccent, MainActivity.this.dp(10)));
                textView3.setTextColor(MainActivity.this.colMuted);
                textView3.setBackground(null);
            }
        });
        textView3.setOnClickListener(new View.OnClickListener() { // from class: au.com.dss.gatehouse.MainActivity.35
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                MainActivity.this.hapticClick();
                holographicCardView.setCardMode(1);
                textView3.setTextColor(MainActivity.this.colAccentInk);
                textView3.setBackground(MainActivity.this.rounded(MainActivity.this.colEmerald, MainActivity.this.dp(10)));
                textView2.setTextColor(MainActivity.this.colMuted);
                textView2.setBackground(null);
            }
        });
        final Dialog createTacticalDialog = createTacticalDialog(dialogContainer);
        LinearLayout linearLayout2 = new LinearLayout(this);
        linearLayout2.setOrientation(0);
        TextView actionButton = actionButton("📋 Copy Licence Details", this.colPanel2, this.colPale);
        actionButton.setOnClickListener(new View.OnClickListener() { // from class: au.com.dss.gatehouse.MainActivity.36
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                MainActivity.this.hapticClick();
                ((ClipboardManager) MainActivity.this.getSystemService("clipboard")).setPrimaryClip(ClipData.newPlainText("Officer Credentials", "Officer: R. Kelso | QLD Security Licence: #41207 (Class 1A/1C, Exp 14/10/2027) | First Aid: HLTAID011 / CPR HLTAID009 (SJA-QLD-849102-K) | Employer: Doherty Security Services"));
                MainActivity.this.banner.setText("✓ Officer licence & First Aid credentials copied to clipboard");
                MainActivity.this.banner.setVisibility(0);
                createTacticalDialog.dismiss();
            }
        });
        linearLayout2.addView(actionButton);
        TextView actionButton2 = actionButton("Close Vault", this.colAccent, this.colAccentInk);
        actionButton2.setOnClickListener(new View.OnClickListener() { // from class: au.com.dss.gatehouse.MainActivity.37
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                MainActivity.this.hapticClick();
                createTacticalDialog.dismiss();
            }
        });
        LinearLayout.LayoutParams layoutParams3 = new LinearLayout.LayoutParams(0, -2, 1.2f);
        layoutParams3.leftMargin = dp(8);
        actionButton2.setLayoutParams(layoutParams3);
        linearLayout2.addView(actionButton2);
        dialogContainer.addView(linearLayout2);
        createTacticalDialog.setOnDismissListener(new DialogInterface.OnDismissListener() { // from class: au.com.dss.gatehouse.MainActivity.38
            @Override // android.content.DialogInterface.OnDismissListener
            public void onDismiss(DialogInterface dialogInterface) {
                MainActivity.this.activeHoloCard = null;
            }
        });
        createTacticalDialog.show();
    }

    private LinearLayout buildContactsTab() {
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(1);
        linearLayout.setPadding(0, dp(6), 0, dp(24));
        linearLayout.addView(contactsSectionHeader("🚨 EMERGENCY SERVICES · 24/7", this.colCrimson));
        linearLayout.addView(contactCard("Triple Zero (000)", "Police · Fire · Ambulance", "000", "24/7 PRIORITY", this.colCrimson));
        linearLayout.addView(contactCard("Logan District Police", "Kingston & Logan Central Station", "0738261888", "24/7 ATTENDANCE", this.colCrimson));
        linearLayout.addView(contactCard("SES Queensland", "Storm, Flood & Structural Damage", "132500", "24/7 DISPATCH", this.colCyan));
        linearLayout.addView(contactCard("Poisons Info Centre", "Chemical & Hazardous Substance Exposure", "131126", "24/7 SUPPORT", this.colAccent));
        linearLayout.addView(contactsSectionHeader("🏢 DOHERTY SECURITY SERVICES (DSS)", this.colAccent));
        linearLayout.addView(contactCard("DSS 24/7 Control Room", "Central Dispatch & Escalations", "1300377000", "24/7 MONITORING", this.colAccent));
        linearLayout.addView(contactCard("DSS Operations Manager", "Brisbane North & South Operations", "0418700120", "ON CALL", this.colAccent));
        linearLayout.addView(contactCard("DSS Field Patrol Supervisor", "Mobile Response Unit 4", "0422555810", "ON SHIFT 18:00–06:00", this.colEmerald));
        linearLayout.addView(contactsSectionHeader("🏭 HUME DOORS & TIMBER (KINGSTON SITE)", this.colCyan));
        linearLayout.addView(contactCard("Hume Site Operations Manager", "Kingston Plant Management", "0439123456", "PRIMARY CLIENT CONTACT", this.colCyan));
        linearLayout.addView(contactCard("Hume Facilities & Plant Engineer", "Power, Pump House & Gate Failures", "0411987654", "ON CALL MAINTENANCE", this.colCyan));
        linearLayout.addView(contactCard("Hume WHS / Safety Officer", "Workplace Safety & Incident Officer", "0423456789", "ON CALL SAFETY", this.colCyan));
        linearLayout.addView(contactsSectionHeader("👥 ON-SITE & RELIEF GUARDS", this.colPale));
        linearLayout.addView(contactCard("Officer R. Kelso", "Current Static Guard · LIC #41207", "0455123789", "ON SITE (TONIGHT)", this.colEmerald));
        linearLayout.addView(contactCard("Relief / Day Crew Guard", "Morning Handover Officer (06:05)", "0400111222", "06:05 HANDOVER", this.colMuted));
        return linearLayout;
    }

    private TextView contactsSectionHeader(String str, int i) {
        TextView textView = new TextView(this);
        textView.setText(str);
        textView.setTextColor(i);
        textView.setTextSize(11.0f);
        textView.setTypeface(Typeface.DEFAULT_BOLD);
        textView.setLetterSpacing(0.12f);
        textView.setPadding(0, dp(14), 0, dp(6));
        return textView;
    }

    private LinearLayout contactCard(String str, String str2, final String str3, String str4, int i) {
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(1);
        linearLayout.setBackground(rounded(this.colPanel, dp(14)));
        linearLayout.setPadding(dp(14), dp(12), dp(14), dp(12));
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, -2);
        layoutParams.bottomMargin = dp(8);
        linearLayout.setLayoutParams(layoutParams);
        LinearLayout linearLayout2 = new LinearLayout(this);
        linearLayout2.setOrientation(0);
        linearLayout2.setGravity(16);
        TextView textView = new TextView(this);
        textView.setText(str);
        textView.setTextColor(this.colPale);
        textView.setTextSize(13.0f);
        textView.setSingleLine(true);
        textView.setEllipsize(TextUtils.TruncateAt.END);
        textView.setTypeface(Typeface.DEFAULT_BOLD);
        textView.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));
        linearLayout2.addView(textView);
        TextView textView2 = new TextView(this);
        textView2.setText(str4);
        textView2.setTextColor(i);
        textView2.setTextSize(8.0f);
        textView2.setSingleLine(true);
        textView2.setTypeface(Typeface.MONOSPACE);
        textView2.setPadding(dp(5), dp(1), dp(5), dp(1));
        textView2.setBackground(rounded(this.colPanel2, dp(4)));
        linearLayout2.addView(textView2);
        linearLayout.addView(linearLayout2);
        TextView textView3 = new TextView(this);
        textView3.setText(str2);
        textView3.setTextColor(this.colMuted);
        textView3.setTextSize(12.0f);
        textView3.setPadding(0, dp(2), 0, dp(8));
        linearLayout.addView(textView3);
        LinearLayout linearLayout3 = new LinearLayout(this);
        linearLayout3.setOrientation(0);
        linearLayout3.setGravity(16);
        TextView textView4 = new TextView(this);
        textView4.setText("📞 " + formatPhoneNumber(str3));
        textView4.setTextColor(this.colAccent);
        textView4.setTextSize(13.0f);
        textView4.setTypeface(Typeface.MONOSPACE);
        textView4.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));
        linearLayout3.addView(textView4);
        TextView textView5 = new TextView(this);
        textView5.setText("CALL NOW");
        textView5.setTextColor(this.colAccentInk);
        textView5.setTextSize(11.0f);
        textView5.setTypeface(Typeface.DEFAULT_BOLD);
        textView5.setPadding(dp(12), dp(6), dp(12), dp(6));
        textView5.setBackground(pressable(this.colAccent, dp(8)));
        textView5.setOnClickListener(new View.OnClickListener() { // from class: au.com.dss.gatehouse.MainActivity.39
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                MainActivity.this.hapticClick();
                MainActivity.this.registerActivity();
                MainActivity.this.dialNumber(str3);
            }
        });
        linearLayout3.addView(textView5);
        linearLayout.addView(linearLayout3);
        return linearLayout;
    }

    private String formatPhoneNumber(String str) {
        if (str.equals("000")) {
            return "000 (EMERGENCY)";
        }
        if (str.length() == 10 && str.startsWith("04")) {
            return str.substring(0, 4) + " " + str.substring(4, 7) + " " + str.substring(7);
        }
        if (str.length() == 10 && str.startsWith("07")) {
            return "(07) " + str.substring(2, 6) + " " + str.substring(6);
        }
        if (str.length() == 10 && str.startsWith("1300")) {
            return "1300 " + str.substring(4, 7) + " " + str.substring(7);
        }
        if (str.length() == 6 && (str.startsWith("132") || str.startsWith("131"))) {
            return str.substring(0, 3) + " " + str.substring(3);
        }
        return str;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void dialNumber(String str) {
        try {
            Intent intent = new Intent("android.intent.action.DIAL");
            intent.setData(Uri.parse("tel:" + str));
            startActivity(intent);
        } catch (Exception e) {
            this.banner.setText("unable to open dialer for " + str);
            this.banner.setVisibility(0);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void rebuildCurrentScreen() {
        buildUi();
        refresh();
    }

    private LinearLayout headerCard() {
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(1);
        linearLayout.setBackground(rounded(this.colPanel, dp(16)));
        linearLayout.setPadding(dp(14), dp(13), dp(14), dp(13));
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, -2);
        layoutParams.bottomMargin = dp(8);
        linearLayout.setLayoutParams(layoutParams);
        LinearLayout linearLayout2 = new LinearLayout(this);
        linearLayout2.setOrientation(0);
        linearLayout2.setGravity(16);
        TextView textView = new TextView(this);
        textView.setText("DOHERTY SECURITY SERVICES");
        textView.setTextColor(this.colAccent);
        textView.setTextSize(9.5f);
        textView.setTypeface(Typeface.create(Typeface.MONOSPACE, 1));
        textView.setLetterSpacing(0.08f);
        textView.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));
        linearLayout2.addView(textView);
        TextView textView2 = new TextView(this);
        textView2.setText("📍 POST 01 · GATEHOUSE");
        textView2.setTextColor(this.colMuted);
        textView2.setTextSize(8.5f);
        textView2.setTypeface(Typeface.MONOSPACE);
        textView2.setPadding(dp(6), dp(2), dp(6), dp(2));
        textView2.setBackground(rounded(this.colPanel2, dp(4)));
        linearLayout2.addView(textView2);
        linearLayout.addView(linearLayout2);
        TextView textView3 = new TextView(this);
        textView3.setText("Hume Doors & Timber, Kingston");
        textView3.setTextColor(this.colPale);
        textView3.setTextSize(17.5f);
        textView3.setTypeface(Typeface.DEFAULT_BOLD);
        textView3.setPadding(0, dp(4), 0, dp(4));
        linearLayout.addView(textView3);
        LinearLayout linearLayout3 = new LinearLayout(this);
        linearLayout3.setOrientation(0);
        linearLayout3.setGravity(16);
        linearLayout3.setPadding(0, dp(2), 0, dp(4));
        TextView textView4 = new TextView(this);
        textView4.setText("🛡️ Officer R. Kelso");
        textView4.setTextColor(this.colPale);
        textView4.setTextSize(12.5f);
        textView4.setTypeface(Typeface.DEFAULT_BOLD);
        linearLayout3.addView(textView4);
        TextView textView5 = new TextView(this);
        textView5.setText("LIC #41207 \u1faaa");
        textView5.setTextColor(this.colAccent);
        textView5.setTextSize(8.5f);
        textView5.setTypeface(Typeface.MONOSPACE);
        textView5.setPadding(dp(5), dp(2), dp(5), dp(2));
        textView5.setBackground(rounded(this.colPanel2, dp(4)));
        LinearLayout.LayoutParams layoutParams2 = new LinearLayout.LayoutParams(-2, -2);
        layoutParams2.leftMargin = dp(6);
        textView5.setLayoutParams(layoutParams2);
        linearLayout3.addView(textView5);
        View view = new View(this);
        view.setLayoutParams(new LinearLayout.LayoutParams(0, 0, 1.0f));
        linearLayout3.addView(view);
        TextView textView6 = new TextView(this);
        textView6.setText("● ON DUTY");
        textView6.setTextColor(this.colEmerald);
        textView6.setTextSize(8.5f);
        textView6.setTypeface(Typeface.create(Typeface.MONOSPACE, 1));
        textView6.setPadding(dp(6), dp(2), dp(6), dp(2));
        textView6.setBackground(rounded(this.colEmeraldSoft, dp(4)));
        linearLayout3.addView(textView6);
        linearLayout.addView(linearLayout3);
        TextView textView7 = new TextView(this);
        textView7.setText("🌙 Shift 18:00 – 06:00 (12.0h) · Night Static Guarding");
        textView7.setTextColor(this.colQuiet);
        textView7.setTextSize(10.0f);
        textView7.setPadding(0, dp(1), 0, 0);
        linearLayout.addView(textView7);
        linearLayout.setOnClickListener(new View.OnClickListener() { // from class: au.com.dss.gatehouse.MainActivity.40
            @Override // android.view.View.OnClickListener
            public void onClick(View view2) {
                long elapsedRealtime = SystemClock.elapsedRealtime();
                if (elapsedRealtime - MainActivity.this.lastHeaderTapMs < 400) {
                    MainActivity.this.headerTapCount++;
                    if (MainActivity.this.headerTapCount >= 3) {
                        MainActivity.this.headerTapCount = 0;
                        MainActivity.this.hapticDoublePulse();
                        MainActivity.this.triggerSunConureFlight();
                    }
                } else {
                    MainActivity.this.headerTapCount = 1;
                }
                MainActivity.this.lastHeaderTapMs = elapsedRealtime;
            }
        });
        return linearLayout;
    }

    private LinearLayout sectionHeader(String str, String str2) {
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(0);
        linearLayout.setGravity(16);
        linearLayout.setPadding(0, dp(14), 0, dp(6));
        TextView textView = new TextView(this);
        textView.setText(str.toUpperCase());
        textView.setTextColor(this.colQuiet);
        textView.setTextSize(11.0f);
        textView.setTypeface(Typeface.DEFAULT_BOLD);
        textView.setLetterSpacing(0.12f);
        textView.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));
        linearLayout.addView(textView);
        if (str2 != null) {
            TextView textView2 = new TextView(this);
            textView2.setText(str2);
            textView2.setTextColor(this.colAccent);
            textView2.setTextSize(11.0f);
            textView2.setTypeface(Typeface.DEFAULT_BOLD);
            linearLayout.addView(textView2);
        }
        return linearLayout;
    }

    private TextView label(String str) {
        TextView textView = new TextView(this);
        textView.setText(str);
        textView.setTextColor(this.colQuiet);
        textView.setTextSize(11.0f);
        textView.setTypeface(Typeface.DEFAULT_BOLD);
        textView.setLetterSpacing(0.12f);
        return textView;
    }

    private TextView pill(String str, boolean z) {
        TextView textView = new TextView(this);
        textView.setText(str);
        textView.setTextSize(11.0f);
        textView.setTypeface(Typeface.DEFAULT_BOLD);
        textView.setTextColor(z ? this.colAccentInk : this.colMuted);
        textView.setPadding(dp(11), dp(5), dp(11), dp(5));
        textView.setBackground(z ? rounded(this.colAccent, dp(20)) : outlined(this.colLine, dp(20)));
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-2, -2);
        layoutParams.rightMargin = dp(8);
        textView.setLayoutParams(layoutParams);
        return textView;
    }

    private LinearLayout patrolActionCard(final String str, final String str2, boolean z) {
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setTag(str);
        linearLayout.setOrientation(1);
        linearLayout.setBackground(pressable(this.colPanel, dp(14)));
        linearLayout.setPadding(dp(12), dp(12), dp(12), dp(12));
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(0, -2, 1.0f);
        layoutParams.rightMargin = z ? dp(4) : 0;
        layoutParams.leftMargin = z ? 0 : dp(4);
        linearLayout.setLayoutParams(layoutParams);
        LinearLayout linearLayout2 = new LinearLayout(this);
        linearLayout2.setOrientation(0);
        linearLayout2.setGravity(16);
        TextView textView = new TextView(this);
        textView.setText(str);
        textView.setTextColor(this.colPale);
        textView.setTextSize(12.5f);
        textView.setTypeface(Typeface.DEFAULT_BOLD);
        textView.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));
        linearLayout2.addView(textView);
        TextView textView2 = new TextView(this);
        textView2.setText(str.contains("Full") ? "45m" : "25m");
        textView2.setTextColor(this.colMuted);
        textView2.setTextSize(8.5f);
        textView2.setTypeface(Typeface.MONOSPACE);
        textView2.setPadding(dp(5), dp(2), dp(5), dp(2));
        textView2.setBackground(rounded(this.colPanel2, dp(4)));
        linearLayout2.addView(textView2);
        linearLayout.addView(linearLayout2);
        TextView textView3 = new TextView(this);
        textView3.setText(str.contains("Full") ? "7.2 Ha Boundary & Fence" : "Yard & North Gate A");
        textView3.setTextColor(this.colQuiet);
        textView3.setTextSize(10.0f);
        textView3.setPadding(0, dp(4), 0, dp(8));
        linearLayout.addView(textView3);
        TextView textView4 = new TextView(this);
        textView4.setText("TAP TO LOG");
        textView4.setTextColor(this.colAccent);
        textView4.setTextSize(9.0f);
        textView4.setTypeface(Typeface.create(Typeface.MONOSPACE, 1));
        textView4.setGravity(17);
        textView4.setPadding(0, dp(5), 0, dp(5));
        textView4.setBackground(rounded(this.colPanel2, dp(6)));
        linearLayout.addView(textView4);
        linearLayout.setOnClickListener(new View.OnClickListener() { // from class: au.com.dss.gatehouse.MainActivity.41
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                MainActivity.this.hapticClick();
                MainActivity.this.promptExternalPatrol(str, str2);
            }
        });
        return linearLayout;
    }

    private TextView lotBadge(final String str, final String str2, boolean z) {
        TextView textView = new TextView(this);
        textView.setTag(str);
        textView.setText(str);
        textView.setTextSize(10.5f);
        textView.setTypeface(Typeface.DEFAULT_BOLD);
        textView.setGravity(17);
        textView.setTextColor(this.colPale);
        textView.setPadding(dp(4), dp(10), dp(4), dp(10));
        textView.setBackground(pressable(this.colPanel, dp(10)));
        textView.setOnClickListener(new View.OnClickListener() { // from class: au.com.dss.gatehouse.MainActivity.42
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                MainActivity.this.hapticClick();
                MainActivity.this.promptLotShutdown(str, str2);
            }
        });
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(0, -2, 1.0f);
        layoutParams.rightMargin = z ? 0 : dp(4);
        textView.setLayoutParams(layoutParams);
        return textView;
    }

    private LinearLayout fireCompactRow(final String str, final String str2, boolean z) {
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setTag(str);
        linearLayout.setOrientation(0);
        linearLayout.setGravity(16);
        linearLayout.setPadding(dp(4), dp(9), dp(4), dp(9));
        linearLayout.setBackground(pressable(0, dp(8)));
        linearLayout.setOnClickListener(new View.OnClickListener() { // from class: au.com.dss.gatehouse.MainActivity.43
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                MainActivity.this.hapticClick();
                MainActivity.this.promptPumpHouseCheck(str, str2);
            }
        });
        TextView textView = new TextView(this);
        textView.setText("🚰");
        textView.setTextSize(13.0f);
        textView.setPadding(0, 0, dp(8), 0);
        linearLayout.addView(textView);
        TextView textView2 = new TextView(this);
        textView2.setText(str);
        textView2.setTextColor(this.colPale);
        textView2.setTextSize(12.5f);
        textView2.setTypeface(Typeface.DEFAULT_BOLD);
        textView2.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));
        linearLayout.addView(textView2);
        TextView textView3 = new TextView(this);
        textView3.setText("1,200 kPa ✓");
        textView3.setTextColor(this.colEmerald);
        textView3.setTextSize(9.0f);
        textView3.setTypeface(Typeface.MONOSPACE);
        textView3.setPadding(dp(6), dp(3), dp(6), dp(3));
        textView3.setBackground(rounded(this.colPanel2, dp(6)));
        linearLayout.addView(textView3);
        return linearLayout;
    }

    private LinearLayout buildCaptureDock() {
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(0);
        linearLayout.setPadding(0, dp(4), 0, dp(12));
        linearLayout.addView(dockButton("Incident", "🚨", new View.OnClickListener() { // from class: au.com.dss.gatehouse.MainActivity.44
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                MainActivity.this.hapticHeavyClick();
                MainActivity.this.showModernIncidentSheet();
            }
        }, 0));
        linearLayout.addView(dockButton("Notes", "📝", new View.OnClickListener() { // from class: au.com.dss.gatehouse.MainActivity.45
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                MainActivity.this.hapticClick();
                MainActivity.this.showModernNotesSheet();
            }
        }, 1));
        linearLayout.addView(dockButton("Photo", "📷", new View.OnClickListener() { // from class: au.com.dss.gatehouse.MainActivity.46
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                MainActivity.this.hapticDoublePulse();
                MainActivity.this.checkAndLaunchFastCamera(null);
            }
        }, 2));
        linearLayout.addView(dockButton("Voice", "🎙️", new View.OnClickListener() { // from class: au.com.dss.gatehouse.MainActivity.47
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                MainActivity.this.hapticDoublePulse();
                MainActivity.this.checkAndLaunchVoice();
            }
        }, 3));
        return linearLayout;
    }

    private LinearLayout dockButton(String str, String str2, View.OnClickListener onClickListener, int i) {
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(1);
        linearLayout.setGravity(17);
        linearLayout.setBackground(pressable(this.colPanel, dp(16)));
        linearLayout.setPadding(dp(6), dp(12), dp(6), dp(12));
        linearLayout.setOnClickListener(onClickListener);
        TextView textView = new TextView(this);
        textView.setText(str2);
        textView.setTextSize(18.0f);
        textView.setGravity(17);
        linearLayout.addView(textView);
        TextView textView2 = new TextView(this);
        textView2.setText(str);
        textView2.setTextColor(this.colMuted);
        textView2.setTextSize(10.0f);
        textView2.setTypeface(Typeface.DEFAULT_BOLD);
        textView2.setPadding(0, dp(4), 0, 0);
        textView2.setGravity(17);
        linearLayout.addView(textView2);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(0, -2, 1.0f);
        layoutParams.leftMargin = i > 0 ? dp(4) : 0;
        layoutParams.rightMargin = i < 3 ? dp(4) : 0;
        linearLayout.setLayoutParams(layoutParams);
        return linearLayout;
    }

    private Dialog createTacticalDialog(View view) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(1);
        dialog.setContentView(view);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
            dialog.getWindow().setLayout(-1, -2);
            dialog.getWindow().setGravity(80);
        }
        return dialog;
    }

    private LinearLayout dialogContainer(String str, String str2, int i) {
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(1);
        linearLayout.setBackground(rounded(this.colPanel, dp(24)));
        linearLayout.setPadding(dp(20), dp(20), dp(20), dp(24));
        linearLayout.setElevation(dp(16));
        View view = new View(this);
        view.setBackground(rounded(this.colLine, dp(3)));
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(dp(44), dp(4));
        layoutParams.gravity = 1;
        layoutParams.bottomMargin = dp(16);
        view.setLayoutParams(layoutParams);
        linearLayout.addView(view);
        LinearLayout linearLayout2 = new LinearLayout(this);
        linearLayout2.setOrientation(0);
        linearLayout2.setGravity(16);
        linearLayout2.setPadding(0, 0, 0, dp(14));
        TextView textView = new TextView(this);
        textView.setText(str);
        textView.setTextColor(this.colPale);
        textView.setTextSize(17.0f);
        textView.setTypeface(Typeface.DEFAULT_BOLD);
        textView.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));
        linearLayout2.addView(textView);
        if (str2 != null) {
            TextView textView2 = new TextView(this);
            textView2.setText(str2);
            textView2.setTextColor(i);
            textView2.setTextSize(10.0f);
            textView2.setTypeface(Typeface.MONOSPACE);
            textView2.setPadding(dp(8), dp(3), dp(8), dp(3));
            textView2.setBackground(rounded(i == this.colCrimson ? this.colCrimsonSoft : this.colAccentSoft, dp(6)));
            linearLayout2.addView(textView2);
        }
        linearLayout.addView(linearLayout2);
        return linearLayout;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class SatellitePolarRadarView extends View {
        private final Paint gridPaint;
        private final Paint satPaint;
        private final Paint textPaint;

        public SatellitePolarRadarView(Context context) {
            super(context);
            this.gridPaint = new Paint(1);
            this.satPaint = new Paint(1);
            this.textPaint = new Paint(1);
            this.gridPaint.setStyle(Paint.Style.STROKE);
            this.gridPaint.setStrokeWidth(MainActivity.this.dp(1));
            this.satPaint.setStyle(Paint.Style.FILL);
            this.textPaint.setTextAlign(Paint.Align.CENTER);
            this.textPaint.setTypeface(Typeface.create(Typeface.MONOSPACE, 1));
        }

        @Override // android.view.View
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            int width = getWidth();
            int height = getHeight();
            if (width <= 0 || height <= 0) {
                return;
            }
            float f = width / 2.0f;
            float f2 = height / 2.0f;
            float min = (Math.min(width, height) / 2.0f) - MainActivity.this.dp(14);
            this.gridPaint.setColor(MainActivity.this.colLineSubtle);
            canvas.drawCircle(f, f2, min, this.gridPaint);
            canvas.drawCircle(f, f2, 0.66f * min, this.gridPaint);
            canvas.drawCircle(f, f2, 0.33f * min, this.gridPaint);
            this.gridPaint.setColor(MainActivity.this.colLine);
            float f3 = f - min;
            float f4 = f + min;
            canvas.drawLine(f3, f2, f4, f2, this.gridPaint);
            float f5 = f2 - min;
            float f6 = f2 + min;
            canvas.drawLine(f, f5, f, f6, this.gridPaint);
            this.textPaint.setColor(MainActivity.this.colQuiet);
            this.textPaint.setTextSize(MainActivity.this.dp(9));
            canvas.drawText("N", f, f5 + MainActivity.this.dp(9), this.textPaint);
            char c = 2;
            canvas.drawText("S", f, f6 - MainActivity.this.dp(2), this.textPaint);
            canvas.drawText("E", f4 - MainActivity.this.dp(7), MainActivity.this.dp(3) + f2, this.textPaint);
            canvas.drawText("W", f3 + MainActivity.this.dp(7), MainActivity.this.dp(3) + f2, this.textPaint);
            int[][] iArr = {new int[]{45, 65, 1}, new int[]{110, 45, 1}, new int[]{165, 80, 1}, new int[]{210, 30, 2}, new int[]{280, 55, 1}, new int[]{330, 75, 1}, new int[]{15, 20, 2}, new int[]{85, 35, 1}, new int[]{195, 60, 1}, new int[]{245, 85, 1}, new int[]{305, 40, 1}, new int[]{140, 15, 2}};
            char c2 = 0;
            int i = 0;
            while (i < 12) {
                int[] iArr2 = iArr[i];
                float f7 = iArr2[c2];
                float f8 = iArr2[1];
                int i2 = iArr2[c];
                double radians = Math.toRadians(f7 - 90.0f);
                double d = (1.0f - (f8 / 90.0f)) * min;
                float cos = (float) (f + (Math.cos(radians) * d));
                float sin = (float) (f2 + (Math.sin(radians) * d));
                this.satPaint.setColor(i2 == 1 ? MainActivity.this.colEmerald : MainActivity.this.colAccent);
                canvas.drawCircle(cos, sin, MainActivity.this.dp(4), this.satPaint);
                i++;
                c = 2;
                c2 = 0;
            }
        }
    }

    private LinearLayout buildGpsCard() {
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(1);
        linearLayout.setBackground(rounded(this.colPanel, dp(16)));
        linearLayout.setPadding(dp(16), dp(16), dp(16), dp(16));
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, -2);
        layoutParams.bottomMargin = dp(12);
        linearLayout.setLayoutParams(layoutParams);
        this.satelliteRadarView = new SatellitePolarRadarView(this);
        LinearLayout.LayoutParams layoutParams2 = new LinearLayout.LayoutParams(-1, dp(130));
        layoutParams2.bottomMargin = dp(10);
        this.satelliteRadarView.setLayoutParams(layoutParams2);
        linearLayout.addView(this.satelliteRadarView);
        this.gpsCoordsText = new TextView(this);
        this.gpsCoordsText.setText("Latitude / Longitude: Acquiring Fix...");
        this.gpsCoordsText.setTextColor(this.colEmerald);
        this.gpsCoordsText.setTextSize(15.0f);
        this.gpsCoordsText.setTypeface(Typeface.MONOSPACE);
        linearLayout.addView(this.gpsCoordsText);
        LinearLayout linearLayout2 = new LinearLayout(this);
        linearLayout2.setOrientation(0);
        linearLayout2.setPadding(0, dp(4), 0, dp(12));
        this.gpsAltitudeText = new TextView(this);
        this.gpsAltitudeText.setText("Altitude: -- m");
        this.gpsAltitudeText.setTextColor(this.colMuted);
        this.gpsAltitudeText.setTextSize(12.0f);
        this.gpsAltitudeText.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));
        linearLayout2.addView(this.gpsAltitudeText);
        this.gpsAccuracyText = new TextView(this);
        this.gpsAccuracyText.setText("Accuracy: ± -- m  [HDOP 0.7]");
        this.gpsAccuracyText.setTextColor(this.colQuiet);
        this.gpsAccuracyText.setTextSize(12.0f);
        linearLayout2.addView(this.gpsAccuracyText);
        linearLayout.addView(linearLayout2);
        LinearLayout linearLayout3 = new LinearLayout(this);
        linearLayout3.setOrientation(0);
        TextView actionButton = actionButton("📋 Copy for 000", this.colPanel2, this.colPale);
        actionButton.setOnClickListener(new View.OnClickListener() { // from class: au.com.dss.gatehouse.MainActivity.48
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                MainActivity.this.hapticClick();
                if (MainActivity.this.lastKnownLocation != null) {
                    ((ClipboardManager) MainActivity.this.getSystemService("clipboard")).setPrimaryClip(ClipData.newPlainText("GPS Coords", String.format(Locale.US, "%.6f, %.6f (Hume Doors Kingston)", Double.valueOf(MainActivity.this.lastKnownLocation.getLatitude()), Double.valueOf(MainActivity.this.lastKnownLocation.getLongitude()))));
                    MainActivity.this.banner.setText("✓ GPS coordinates copied to clipboard for emergency 000");
                    MainActivity.this.banner.setVisibility(0);
                }
            }
        });
        linearLayout3.addView(actionButton);
        TextView actionButton2 = actionButton("📍 Log GPS Fix", this.colAccent, this.colAccentInk);
        actionButton2.setOnClickListener(new View.OnClickListener() { // from class: au.com.dss.gatehouse.MainActivity.49
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                MainActivity.this.hapticHeavyClick();
                if (MainActivity.this.lastKnownLocation != null) {
                    MainActivity.this.note(0, String.format(Locale.US, "[GPS FIX] Lat: %.6f, Lon: %.6f, Alt: %.1fm (±%.1fm)", Double.valueOf(MainActivity.this.lastKnownLocation.getLatitude()), Double.valueOf(MainActivity.this.lastKnownLocation.getLongitude()), Double.valueOf(MainActivity.this.lastKnownLocation.getAltitude()), Float.valueOf(MainActivity.this.lastKnownLocation.getAccuracy())));
                    MainActivity.this.banner.setText("✓ GPS telemetry point logged to Ada record");
                    MainActivity.this.banner.setVisibility(0);
                }
            }
        });
        LinearLayout.LayoutParams layoutParams3 = new LinearLayout.LayoutParams(0, -2, 1.2f);
        layoutParams3.leftMargin = dp(8);
        actionButton2.setLayoutParams(layoutParams3);
        linearLayout3.addView(actionButton2);
        linearLayout.addView(linearLayout3);
        return linearLayout;
    }

    @Override // android.location.LocationListener
    public void onLocationChanged(Location location) {
        this.lastKnownLocation = location;
        updateGpsDisplay(location);
    }

    private void updateGpsDisplay(Location location) {
        if (this.gpsCoordsText == null) {
            return;
        }
        this.gpsCoordsText.setText(String.format(Locale.US, "%.6f°, %.6f°", Double.valueOf(location.getLatitude()), Double.valueOf(location.getLongitude())));
        this.gpsAltitudeText.setText(String.format(Locale.US, "Altitude: %.1f m ASL", Double.valueOf(location.getAltitude())));
        this.gpsAccuracyText.setText(String.format(Locale.US, "Accuracy: ±%.1f m  [HDOP 0.7]", Float.valueOf(location.getAccuracy())));
    }

    @Override // android.location.LocationListener
    public void onStatusChanged(String str, int i, Bundle bundle) {
    }

    @Override // android.location.LocationListener
    public void onProviderEnabled(String str) {
    }

    @Override // android.location.LocationListener
    public void onProviderDisabled(String str) {
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class MapSector {
        String code;
        String desc;
        String label;

        MapSector(String str, String str2, String str3) {
            this.code = str;
            this.label = str2;
            this.desc = str3;
        }
    }

    /* JADX WARN: Multi-variable type inference failed */
    private LinearLayout buildBlueprintMiniMap(final String[] strArr, final Runnable runnable) {
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(1);
        linearLayout.setBackground(rounded(this.colPanel2, dp(14)));
        int i = 10;
        linearLayout.setPadding(dp(12), dp(10), dp(12), dp(10));
        int i2 = -1;
        int i3 = -2;
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, -2);
        layoutParams.bottomMargin = dp(10);
        linearLayout.setLayoutParams(layoutParams);
        LinearLayout linearLayout2 = new LinearLayout(this);
        int i4 = 0;
        linearLayout2.setOrientation(0);
        linearLayout2.setGravity(16);
        linearLayout2.setPadding(0, 0, 0, dp(8));
        TextView textView = new TextView(this);
        textView.setText("📍 SITE BLUEPRINT (TAP TO PIN LOCATION)");
        textView.setTextColor(this.colQuiet);
        textView.setTextSize(10.0f);
        textView.setTypeface(Typeface.DEFAULT_BOLD);
        textView.setLetterSpacing(0.12f);
        textView.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));
        linearLayout2.addView(textView);
        final TextView textView2 = new TextView(this);
        textView2.setText(strArr[0] == null ? "No Pin Dropped" : "📍 " + strArr[0]);
        textView2.setTextColor(strArr[0] == null ? this.colQuiet : this.colAccent);
        textView2.setTextSize(10.0f);
        textView2.setTypeface(Typeface.MONOSPACE);
        linearLayout2.addView(textView2);
        linearLayout.addView(linearLayout2);
        ArrayList arrayList = new ArrayList();
        int i5 = 0;
        while (true) {
            int i6 = 3;
            if (i5 < 3) {
                LinearLayout linearLayout3 = new LinearLayout(this);
                linearLayout3.setOrientation(i4);
                LinearLayout.LayoutParams layoutParams2 = new LinearLayout.LayoutParams(i2, i3);
                int i7 = 6;
                layoutParams2.bottomMargin = i5 < 2 ? dp(6) : i4;
                linearLayout3.setLayoutParams(layoutParams2);
                int i8 = i4;
                while (i8 < i6) {
                    final MapSector mapSector = SITE_MAP_SECTORS[(i5 * 3) + i8];
                    TextView textView3 = new TextView(this);
                    textView3.setText(mapSector.label);
                    textView3.setTextSize(11.0f);
                    textView3.setTypeface(Typeface.DEFAULT_BOLD);
                    textView3.setGravity(17);
                    textView3.setPadding(dp(i7), dp(i), dp(i7), dp(i));
                    updateMapSectorStyle(textView3, (strArr[i4] != null && strArr[i4].equals(mapSector.desc)));
                    LinearLayout.LayoutParams layoutParams3 = new LinearLayout.LayoutParams(i4, -2, 1.0f);
                    layoutParams3.leftMargin = i8 > 0 ? dp(4) : i4;
                    layoutParams3.rightMargin = i8 < 2 ? dp(4) : i4;
                    textView3.setLayoutParams(layoutParams3);
                    LinearLayout linearLayout4 = linearLayout3;
                    int i9 = i5;
                    final ArrayList arrayList2 = arrayList;
                    ArrayList arrayList3 = arrayList;
                    textView3.setOnClickListener(new View.OnClickListener() { // from class: au.com.dss.gatehouse.MainActivity.50
                        @Override // android.view.View.OnClickListener
                        public void onClick(View view) {
                            MainActivity.this.hapticClick();
                            if (strArr[0] != null && strArr[0].equals(mapSector.desc)) {
                                strArr[0] = null;
                                textView2.setText("No Pin Dropped");
                                textView2.setTextColor(MainActivity.this.colQuiet);
                            } else {
                                strArr[0] = mapSector.desc;
                                textView2.setText("📍 " + mapSector.desc);
                                textView2.setTextColor(MainActivity.this.colAccent);
                            }
                            for (int i10 = 0; i10 < arrayList2.size(); i10++) {
                                MainActivity.this.updateMapSectorStyle((TextView) arrayList2.get(i10), strArr[0] != null && strArr[0].equals(MainActivity.SITE_MAP_SECTORS[i10].desc));
                            }
                            if (runnable != null) {
                                runnable.run();
                            }
                        }
                    });
                    arrayList3.add(textView3);
                    linearLayout4.addView(textView3);
                    i8++;
                    linearLayout3 = linearLayout4;
                    arrayList = arrayList3;
                    i6 = 3;
                    i7 = 6;
                    i5 = i9;
                    i = 10;
                    i4 = 0;
                }
                linearLayout.addView(linearLayout3);
                i5++;
                i = 10;
                i2 = -1;
                i3 = -2;
                i4 = 0;
            } else {
                return linearLayout;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateMapSectorStyle(TextView textView, boolean z) {
        if (z) {
            textView.setTextColor(this.colAccentInk);
            textView.setBackground(rounded(this.colAccent, dp(8)));
        } else {
            textView.setTextColor(this.colPale);
            textView.setBackground(pressable(this.colPanel3, dp(8)));
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void checkAndLaunchFastCamera(OnPhotoCapturedCallback onPhotoCapturedCallback) {
        if (checkSelfPermission("android.permission.CAMERA") != 0) {
            requestPermissions(new String[]{"android.permission.CAMERA"}, REQ_PERM_CAMERA);
        } else {
            openInAppCameraOverlay(onPhotoCapturedCallback);
        }
    }

    private void openInAppCameraOverlay(final OnPhotoCapturedCallback onPhotoCapturedCallback) {
        final Dialog dialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.requestWindowFeature(1);
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(1);
        linearLayout.setBackgroundColor(-16777216);
        linearLayout.setPadding(dp(16), dp(24), dp(16), dp(56));
        LinearLayout linearLayout2 = new LinearLayout(this);
        linearLayout2.setOrientation(0);
        linearLayout2.setGravity(16);
        linearLayout2.setPadding(0, 0, 0, dp(12));
        TextView textView = new TextView(this);
        textView.setText("📷 PHOTO EVIDENCE & LEVEL");
        textView.setTextColor(this.colPale);
        textView.setTextSize(13.0f);
        textView.setTypeface(Typeface.DEFAULT_BOLD);
        textView.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));
        linearLayout2.addView(textView);
        TextView textView2 = new TextView(this);
        textView2.setText("✕");
        textView2.setTextColor(this.colPale);
        textView2.setTextSize(20.0f);
        textView2.setPadding(dp(14), dp(4), dp(4), dp(4));
        textView2.setOnClickListener(new View.OnClickListener() { // from class: au.com.dss.gatehouse.MainActivity.51
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                MainActivity.this.hapticClick();
                dialog.dismiss();
            }
        });
        linearLayout2.addView(textView2);
        linearLayout.addView(linearLayout2);
        FrameLayout frameLayout = new FrameLayout(this);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, 0, 1.0f);
        layoutParams.bottomMargin = dp(12);
        frameLayout.setLayoutParams(layoutParams);
        final TextureView textureView = new TextureView(this);
        frameLayout.addView(textureView, new FrameLayout.LayoutParams(-1, -1));
        HorizonLevelerView horizonLevelerView = new HorizonLevelerView(this);
        this.activeLevelerView = horizonLevelerView;
        frameLayout.addView(horizonLevelerView, new FrameLayout.LayoutParams(-1, -1));
        linearLayout.addView(frameLayout);
        LinearLayout linearLayout3 = new LinearLayout(this);
        linearLayout3.setOrientation(0);
        linearLayout3.setGravity(17);
        linearLayout3.setPadding(0, 0, 0, dp(6));
        TextView textView3 = new TextView(this);
        textView3.setText("🔘 CAPTURE PHOTO EVIDENCE");
        textView3.setTextColor(this.colAccentInk);
        textView3.setTextSize(15.0f);
        textView3.setTypeface(Typeface.DEFAULT_BOLD);
        textView3.setGravity(17);
        textView3.setPadding(dp(20), dp(16), dp(20), dp(16));
        textView3.setBackground(pressable(this.colAccent, dp(18)));
        textView3.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
        final Runnable runnable = new Runnable() { // from class: au.com.dss.gatehouse.MainActivity.52
            @Override // java.lang.Runnable
            public void run() {
                MainActivity.this.hapticDoublePulse();
                MainActivity.this.registerActivity();
                Bitmap bitmap = textureView.getBitmap();
                if (bitmap != null) {
                    dialog.dismiss();
                    String sha256Hex = MainActivity.this.sha256Hex(MainActivity.this.bitmapToJpegBytes(bitmap));
                    if (onPhotoCapturedCallback != null) {
                        onPhotoCapturedCallback.onCaptured(bitmap, sha256Hex);
                    } else {
                        MainActivity.this.showPhotoReviewSheet(bitmap);
                    }
                }
            }
        };
        textView3.setOnClickListener(new View.OnClickListener() { // from class: au.com.dss.gatehouse.MainActivity.53
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                runnable.run();
            }
        });
        linearLayout3.addView(textView3);
        linearLayout.addView(linearLayout3);
        dialog.setContentView(linearLayout);
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() { // from class: au.com.dss.gatehouse.MainActivity.54
            @Override // android.content.DialogInterface.OnDismissListener
            public void onDismiss(DialogInterface dialogInterface) {
                MainActivity.this.activeLevelerView = null;
            }
        });
        dialog.show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void showPhotoReviewSheet(Bitmap bitmap) {
        final String sha256Hex = sha256Hex(bitmapToJpegBytes(bitmap));
        if (sha256Hex.length() >= 8) {
            String dispHash = sha256Hex.substring(0, 8);
        }
        LinearLayout dialogContainer = dialogContainer("📷 Photo Evidence", "SHA-256 VERIFIED", this.colEmerald);
        ImageView imageView = new ImageView(this);
        imageView.setImageBitmap(bitmap);
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imageView.setBackground(rounded(this.colPanel2, dp(14)));
        imageView.setClipToOutline(true);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, dp(180));
        layoutParams.bottomMargin = dp(12);
        imageView.setLayoutParams(layoutParams);
        dialogContainer.addView(imageView);
        final EditText modernInputField = modernInputField("Photo Subject (e.g. Main gate padlock, Lot 16 mesh)");
        dialogContainer.addView(modernInputField);
        final Dialog createTacticalDialog = createTacticalDialog(dialogContainer);
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(0);
        linearLayout.setPadding(0, dp(16), 0, 0);
        TextView actionButton = actionButton("Cancel", this.colLine, this.colMuted);
        actionButton.setOnClickListener(new View.OnClickListener() { // from class: au.com.dss.gatehouse.MainActivity.55
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                MainActivity.this.hapticClick();
                createTacticalDialog.dismiss();
            }
        });
        linearLayout.addView(actionButton);
        TextView actionButton2 = actionButton("Save Photo", this.colAccent, this.colAccentInk);
        actionButton2.setOnClickListener(new View.OnClickListener() { // from class: au.com.dss.gatehouse.MainActivity.56
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                MainActivity.this.hapticHeavyClick();
                MainActivity.this.registerActivity();
                String trim = modernInputField.getText().toString().trim();
                String str = sha256Hex;
                if (trim.isEmpty()) {
                    trim = "evidence captured";
                }
                String str2 = "[PHOTO " + str + "] " + trim;
                if (!MainActivity.this.oneLine(str2)) {
                    MainActivity.this.banner.setText("notes must be one line");
                    MainActivity.this.banner.setVisibility(0);
                } else {
                    MainActivity.this.note(0, str2);
                    createTacticalDialog.dismiss();
                }
            }
        });
        LinearLayout.LayoutParams layoutParams2 = new LinearLayout.LayoutParams(0, -2, 1.4f);
        layoutParams2.leftMargin = dp(8);
        actionButton2.setLayoutParams(layoutParams2);
        linearLayout.addView(actionButton2);
        dialogContainer.addView(linearLayout);
        createTacticalDialog.show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void promptExternalPatrol(final String str, final String str2) {
        LinearLayout dialogContainer = dialogContainer("🛡️ " + str, "PERIMETER INSPECTION", this.colAccent);
        TextView textView = new TextView(this);
        textView.setText("Select perimeter conditions, pin location on map or attach photo evidence:");
        textView.setTextColor(this.colMuted);
        textView.setTextSize(12.0f);
        textView.setPadding(0, 0, 0, dp(8));
        dialogContainer.addView(textView);
        final ArrayList arrayList = new ArrayList();
        arrayList.add(EXTERNAL_OPTIONS[0]);
        final ArrayList arrayList2 = new ArrayList();
        int i = 0;
        while (i < EXTERNAL_OPTIONS.length) {
            final String str3 = EXTERNAL_OPTIONS[i];
            TextView textView2 = new TextView(this);
            textView2.setText(str3);
            textView2.setTextSize(13.0f);
            textView2.setPadding(dp(14), dp(10), dp(14), dp(10));
            final boolean z = i == 0;
            updateCheckItemStyle(textView2, arrayList.contains(str3), z);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, -2);
            layoutParams.bottomMargin = dp(6);
            textView2.setLayoutParams(layoutParams);
            textView2.setOnClickListener(new View.OnClickListener() { // from class: au.com.dss.gatehouse.MainActivity.57
                @Override // android.view.View.OnClickListener
                public void onClick(View view) {
                    MainActivity.this.hapticClick();
                    if (z) {
                        arrayList.clear();
                        arrayList.add(MainActivity.EXTERNAL_OPTIONS[0]);
                    } else {
                        arrayList.remove(MainActivity.EXTERNAL_OPTIONS[0]);
                        if (arrayList.contains(str3)) {
                            arrayList.remove(str3);
                        } else {
                            arrayList.add(str3);
                        }
                        if (arrayList.isEmpty()) {
                            arrayList.add(MainActivity.EXTERNAL_OPTIONS[0]);
                        }
                    }
                    int i2 = 0;
                    while (i2 < arrayList2.size()) {
                        MainActivity.this.updateCheckItemStyle((TextView) arrayList2.get(i2), arrayList.contains(MainActivity.EXTERNAL_OPTIONS[i2]), i2 == 0);
                        i2++;
                    }
                }
            });
            arrayList2.add(textView2);
            dialogContainer.addView(textView2);
            i++;
        }
        final String[] strArr = {null};
        dialogContainer.addView(buildBlueprintMiniMap(strArr, null));
        final EditText modernInputField = modernInputField("Perimeter observation notes (optional)...");
        dialogContainer.addView(modernInputField);
        final Dialog createTacticalDialog = createTacticalDialog(dialogContainer);
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(0);
        linearLayout.setPadding(0, dp(12), 0, 0);
        TextView actionButton = actionButton("Cancel", this.colLine, this.colMuted);
        actionButton.setOnClickListener(new View.OnClickListener() { // from class: au.com.dss.gatehouse.MainActivity.58
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                MainActivity.this.hapticClick();
                createTacticalDialog.dismiss();
            }
        });
        linearLayout.addView(actionButton);
        TextView actionButton2 = actionButton("✓ Log Patrol", this.colAccent, this.colAccentInk);
        actionButton2.setOnClickListener(new View.OnClickListener() { // from class: au.com.dss.gatehouse.MainActivity.59
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                MainActivity.this.hapticHeavyClick();
                MainActivity.this.registerActivity();
                MainActivity.this.tap(str, str2);
                String trim = modernInputField.getText().toString().trim();
                StringBuilder sb = new StringBuilder();
                sb.append(str).append(": ");
                for (int i2 = 0; i2 < arrayList.size(); i2++) {
                    if (i2 > 0) {
                        sb.append(", ");
                    }
                    sb.append((String) arrayList.get(i2));
                }
                if (strArr[0] != null) {
                    sb.append(" 📍 [PIN: ").append(strArr[0]).append("]");
                }
                if (!trim.isEmpty()) {
                    sb.append(" · ").append(trim);
                }
                String sb2 = sb.toString();
                if (MainActivity.this.oneLine(sb2)) {
                    MainActivity.this.note(0, sb2);
                    createTacticalDialog.dismiss();
                } else {
                    MainActivity.this.banner.setText("notes must be one line");
                    MainActivity.this.banner.setVisibility(0);
                }
            }
        });
        LinearLayout.LayoutParams layoutParams2 = new LinearLayout.LayoutParams(0, -2, 1.4f);
        layoutParams2.leftMargin = dp(8);
        actionButton2.setLayoutParams(layoutParams2);
        linearLayout.addView(actionButton2);
        dialogContainer.addView(linearLayout);
        createTacticalDialog.show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void checkAndLaunchVoice() {
        if (checkSelfPermission("android.permission.RECORD_AUDIO") != 0) {
            requestPermissions(new String[]{"android.permission.RECORD_AUDIO"}, REQ_PERM_AUDIO);
        } else {
            showModernVoiceSheet();
        }
    }

    private void showModernVoiceSheet() {
        LinearLayout dialogContainer = dialogContainer("🎙️ Voice Memo", "AUDIO LOG", this.colAccent);
        final TextView textView = new TextView(this);
        textView.setText("00:00");
        textView.setTextSize(32.0f);
        textView.setTextColor(this.colPale);
        textView.setTypeface(Typeface.MONOSPACE);
        textView.setGravity(17);
        textView.setPadding(0, 0, 0, dp(4));
        dialogContainer.addView(textView);
        final TextView textView2 = new TextView(this);
        textView2.setText("Tap microphone to record voice evidence (Max 30s)");
        textView2.setTextColor(this.colMuted);
        textView2.setTextSize(12.0f);
        textView2.setGravity(17);
        textView2.setPadding(0, 0, 0, dp(14));
        dialogContainer.addView(textView2);
        final LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(1);
        linearLayout.setGravity(17);
        linearLayout.setBackground(rounded(this.colPanel2, dp(36)));
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(dp(68), dp(68));
        layoutParams.gravity = 1;
        layoutParams.bottomMargin = dp(14);
        linearLayout.setLayoutParams(layoutParams);
        TextView textView3 = new TextView(this);
        textView3.setText("🎙️");
        textView3.setTextSize(28.0f);
        textView3.setGravity(17);
        linearLayout.addView(textView3);
        dialogContainer.addView(linearLayout);
        final EditText modernInputField = modernInputField("Optional Memo Caption / Subject");
        modernInputField.setVisibility(8);
        dialogContainer.addView(modernInputField);
        final Dialog createTacticalDialog = createTacticalDialog(dialogContainer);
        final Handler handler = new Handler();
        final Runnable runnable = new Runnable() { // from class: au.com.dss.gatehouse.MainActivity.60
            @Override // java.lang.Runnable
            public void run() {
                if (MainActivity.this.isRecordingVoice) {
                    int elapsedRealtime = (int) ((SystemClock.elapsedRealtime() - MainActivity.this.voiceRecordStart) / 1000);
                    textView.setText(String.format(Locale.US, "%02d:%02d", Integer.valueOf(elapsedRealtime / 60), Integer.valueOf(elapsedRealtime % 60)));
                    if (elapsedRealtime >= 30) {
                        MainActivity.this.hapticDoublePulse();
                        MainActivity.this.stopVoiceRecording();
                        textView2.setText("✓ 30s limit reached · Audio captured");
                        linearLayout.setBackground(MainActivity.this.rounded(MainActivity.this.colEmeraldSoft, MainActivity.this.dp(36)));
                        modernInputField.setVisibility(0);
                        return;
                    }
                    handler.postDelayed(this, 100L);
                }
            }
        };
        linearLayout.setOnClickListener(new View.OnClickListener() { // from class: au.com.dss.gatehouse.MainActivity.61
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                MainActivity.this.hapticDoublePulse();
                if (!MainActivity.this.isRecordingVoice) {
                    MainActivity.this.startVoiceRecording();
                    MainActivity.this.isRecordingVoice = true;
                    MainActivity.this.voiceRecordStart = SystemClock.elapsedRealtime();
                    textView2.setText("● RECORDING AUDIO · Tap to Stop");
                    textView2.setTextColor(MainActivity.this.colCrimson);
                    linearLayout.setBackground(MainActivity.this.rounded(MainActivity.this.colCrimsonSoft, MainActivity.this.dp(36)));
                    handler.post(runnable);
                    return;
                }
                MainActivity.this.stopVoiceRecording();
                MainActivity.this.isRecordingVoice = false;
                textView2.setText("✓ Voice Recorded · Ready to Save");
                textView2.setTextColor(MainActivity.this.colEmerald);
                linearLayout.setBackground(MainActivity.this.rounded(MainActivity.this.colEmeraldSoft, MainActivity.this.dp(36)));
                modernInputField.setVisibility(0);
            }
        });
        LinearLayout linearLayout2 = new LinearLayout(this);
        linearLayout2.setOrientation(0);
        linearLayout2.setPadding(0, dp(14), 0, 0);
        TextView actionButton = actionButton("Cancel", this.colLine, this.colMuted);
        actionButton.setOnClickListener(new View.OnClickListener() { // from class: au.com.dss.gatehouse.MainActivity.62
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                MainActivity.this.hapticClick();
                if (MainActivity.this.isRecordingVoice) {
                    MainActivity.this.stopVoiceRecording();
                }
                createTacticalDialog.dismiss();
            }
        });
        linearLayout2.addView(actionButton);
        TextView actionButton2 = actionButton("Save Voice Memo", this.colAccent, this.colAccentInk);
        actionButton2.setOnClickListener(new View.OnClickListener() { // from class: au.com.dss.gatehouse.MainActivity.63
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                MainActivity.this.hapticHeavyClick();
                MainActivity.this.registerActivity();
                if (MainActivity.this.isRecordingVoice) {
                    MainActivity.this.stopVoiceRecording();
                }
                if (MainActivity.this.activeVoiceFile == null || !MainActivity.this.activeVoiceFile.exists()) {
                    MainActivity.this.banner.setText("no voice recorded");
                    MainActivity.this.banner.setVisibility(0);
                    return;
                }
                String sha256Hex = MainActivity.this.sha256Hex(MainActivity.this.readFileBytes(MainActivity.this.activeVoiceFile));
                if (sha256Hex.length() >= 8) {
                    String dispHash = sha256Hex.substring(0, 8);
                }
                String trim = modernInputField.getText().toString().trim();
                if (trim.isEmpty()) {
                    trim = "voice memo recorded";
                }
                String str = "[VOICE " + sha256Hex + "] " + trim;
                if (!MainActivity.this.oneLine(str)) {
                    MainActivity.this.banner.setText("notes must be one line");
                    MainActivity.this.banner.setVisibility(0);
                } else {
                    MainActivity.this.note(0, str);
                    createTacticalDialog.dismiss();
                }
            }
        });
        LinearLayout.LayoutParams layoutParams2 = new LinearLayout.LayoutParams(0, -2, 1.4f);
        layoutParams2.leftMargin = dp(8);
        actionButton2.setLayoutParams(layoutParams2);
        linearLayout2.addView(actionButton2);
        dialogContainer.addView(linearLayout2);
        createTacticalDialog.show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void startVoiceRecording() {
        try {
            this.activeVoiceFile = new File(getFilesDir(), "voice_temp.m4a");
            if (this.activeVoiceFile.exists()) {
                this.activeVoiceFile.delete();
            }
            this.voiceRecorder = new MediaRecorder();
            this.voiceRecorder.setAudioSource(1);
            this.voiceRecorder.setOutputFormat(2);
            this.voiceRecorder.setAudioEncoder(3);
            this.voiceRecorder.setOutputFile(this.activeVoiceFile.getAbsolutePath());
            this.voiceRecorder.prepare();
            this.voiceRecorder.start();
        } catch (Exception e) {
            this.banner.setText("audio recording initialization failed");
            this.banner.setVisibility(0);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void stopVoiceRecording() {
        if (this.voiceRecorder != null) {
            try {
                this.voiceRecorder.stop();
                this.voiceRecorder.release();
            } catch (Exception e) {
            }
            this.voiceRecorder = null;
        }
        this.isRecordingVoice = false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void showModernIncidentSheet() {
        LinearLayout dialogContainer = dialogContainer("🚨 Security Incident Report", "LEGAL AUDIT", this.colCrimson);
        String[] strArr = {"Intruder / Trespass", "Forced Entry / Damage", "Theft / Missing Plant", "Water / Flood Hazard", "Smoke / Fire Alarm", "Suspicious Vehicle"};
        String[] strArr2 = {"Perimeter Secured", "Intruders Fled", "000 Police Dispatched", "Supervisor Alerted"};
        final String[] strArr3 = {strArr[0]};
        final ArrayList<String> arrayList = new ArrayList<>();
        arrayList.add(strArr2[0]);
        final String[] strArr4 = {null};
        dialogContainer.addView(formSectionLabel("1. INCIDENT CATEGORY"));
        dialogContainer.addView(buildChipGroup(strArr, strArr3, true, this.colCrimson));
        dialogContainer.addView(formSectionLabel("2. SITE LOCATION (TAP BLUEPRINT TO PIN)"));
        dialogContainer.addView(buildBlueprintMiniMap(strArr4, null));
        dialogContainer.addView(formSectionLabel("3. ACTION TAKEN / ESCALATION"));
        dialogContainer.addView(buildMultiChipGroup(strArr2, arrayList, this.colAccent));
        dialogContainer.addView(formSectionLabel("4. NARRATIVE DETAILS"));
        final EditText modernInputField = modernInputField("What occurred, suspect descriptions, damage, vehicle plates...");
        modernInputField.setMinLines(3);
        dialogContainer.addView(modernInputField);
        final Dialog createTacticalDialog = createTacticalDialog(dialogContainer);
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(0);
        linearLayout.setPadding(0, dp(16), 0, 0);
        TextView actionButton = actionButton("Cancel", this.colLine, this.colMuted);
        actionButton.setOnClickListener(new View.OnClickListener() { // from class: au.com.dss.gatehouse.MainActivity.64
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                MainActivity.this.hapticClick();
                createTacticalDialog.dismiss();
            }
        });
        linearLayout.addView(actionButton);
        TextView actionButton2 = actionButton("🚨 Log Incident", this.colCrimson, this.colPale);
        actionButton2.setOnClickListener(new View.OnClickListener() { // from class: au.com.dss.gatehouse.MainActivity.65
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                MainActivity.this.hapticSealThud();
                MainActivity.this.registerActivity();
                String trim = modernInputField.getText().toString().trim();
                String join = arrayList.isEmpty() ? "None" : String.join(", ", arrayList);
                String str = strArr4[0] != null ? " at " + strArr4[0] : "";
                String str2 = strArr3[0];
                if (trim.isEmpty()) {
                    trim = "Logged on shift";
                }
                String str3 = "[INCIDENT: " + str2 + str + "] " + trim + " (Action: " + join + ")";
                if (!MainActivity.this.oneLine(str3)) {
                    MainActivity.this.banner.setText("an entry is one line; take out line breaks");
                    MainActivity.this.banner.setVisibility(0);
                } else {
                    MainActivity.this.note(2, str3);
                    createTacticalDialog.dismiss();
                }
            }
        });
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(0, -2, 1.4f);
        layoutParams.leftMargin = dp(8);
        actionButton2.setLayoutParams(layoutParams);
        linearLayout.addView(actionButton2);
        dialogContainer.addView(linearLayout);
        createTacticalDialog.show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void showModernNotesSheet() {
        LinearLayout dialogContainer = dialogContainer("📝 Shift Observation Note", "OCCURRENCE LOG", this.colAccent);
        String[] strArr = {"General", "Maintenance", "Day Crew", "Access", "Safety"};
        final String[] strArr2 = {strArr[0]};
        dialogContainer.addView(formSectionLabel("TOPIC CATEGORY"));
        dialogContainer.addView(buildChipGroup(strArr, strArr2, true, this.colAccent));
        dialogContainer.addView(formSectionLabel("NOTE DETAILS"));
        final EditText modernInputField = modernInputField("Type shift observation, contractor movements, padlocks, fuel...");
        modernInputField.setMinLines(3);
        dialogContainer.addView(modernInputField);
        final Dialog createTacticalDialog = createTacticalDialog(dialogContainer);
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(0);
        linearLayout.setPadding(0, dp(16), 0, 0);
        TextView actionButton = actionButton("Cancel", this.colLine, this.colMuted);
        actionButton.setOnClickListener(new View.OnClickListener() { // from class: au.com.dss.gatehouse.MainActivity.66
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                MainActivity.this.hapticClick();
                createTacticalDialog.dismiss();
            }
        });
        linearLayout.addView(actionButton);
        TextView actionButton2 = actionButton("Save Note", this.colAccent, this.colAccentInk);
        actionButton2.setOnClickListener(new View.OnClickListener() { // from class: au.com.dss.gatehouse.MainActivity.67
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                MainActivity.this.hapticHeavyClick();
                MainActivity.this.registerActivity();
                String trim = modernInputField.getText().toString().trim();
                if (trim.isEmpty()) {
                    return;
                }
                String str = "[" + strArr2[0].toUpperCase() + "] " + trim;
                if (!MainActivity.this.oneLine(str)) {
                    MainActivity.this.banner.setText("an entry is one line; remove line breaks");
                    MainActivity.this.banner.setVisibility(0);
                } else {
                    MainActivity.this.note(strArr2[0].equals("Day Crew") ? 1 : 0, str);
                    createTacticalDialog.dismiss();
                }
            }
        });
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(0, -2, 1.4f);
        layoutParams.leftMargin = dp(8);
        actionButton2.setLayoutParams(layoutParams);
        linearLayout.addView(actionButton2);
        dialogContainer.addView(linearLayout);
        createTacticalDialog.show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void promptLotShutdown(final String str, final String str2) {
        LinearLayout dialogContainer = dialogContainer("🏭 " + str + " Factory Floor", "LOCKUP & SHUTDOWN", this.colCyan);
        TextView textView = new TextView(this);
        textView.setText("Select all applicable conditions or add notes:");
        textView.setTextColor(this.colMuted);
        textView.setTextSize(12.0f);
        textView.setPadding(0, 0, 0, dp(10));
        dialogContainer.addView(textView);
        final ArrayList arrayList = new ArrayList();
        arrayList.add(SHUTDOWN_OPTIONS[0]);
        final ArrayList arrayList2 = new ArrayList();
        int i = 0;
        while (i < SHUTDOWN_OPTIONS.length) {
            final String str3 = SHUTDOWN_OPTIONS[i];
            TextView textView2 = new TextView(this);
            textView2.setText(str3);
            textView2.setTextSize(13.0f);
            textView2.setPadding(dp(14), dp(12), dp(14), dp(12));
            final boolean z = i == 0;
            updateCheckItemStyle(textView2, arrayList.contains(str3), z);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, -2);
            layoutParams.bottomMargin = dp(6);
            textView2.setLayoutParams(layoutParams);
            textView2.setOnClickListener(new View.OnClickListener() { // from class: au.com.dss.gatehouse.MainActivity.68
                @Override // android.view.View.OnClickListener
                public void onClick(View view) {
                    MainActivity.this.hapticClick();
                    if (z) {
                        arrayList.clear();
                        arrayList.add(MainActivity.SHUTDOWN_OPTIONS[0]);
                    } else {
                        arrayList.remove(MainActivity.SHUTDOWN_OPTIONS[0]);
                        if (arrayList.contains(str3)) {
                            arrayList.remove(str3);
                        } else {
                            arrayList.add(str3);
                        }
                        if (arrayList.isEmpty()) {
                            arrayList.add(MainActivity.SHUTDOWN_OPTIONS[0]);
                        }
                    }
                    int i2 = 0;
                    while (i2 < arrayList2.size()) {
                        MainActivity.this.updateCheckItemStyle((TextView) arrayList2.get(i2), arrayList.contains(MainActivity.SHUTDOWN_OPTIONS[i2]), i2 == 0);
                        i2++;
                    }
                }
            });
            arrayList2.add(textView2);
            dialogContainer.addView(textView2);
            i++;
        }
        final EditText modernInputField = modernInputField("Additional observation note (optional)...");
        dialogContainer.addView(modernInputField);
        final Dialog createTacticalDialog = createTacticalDialog(dialogContainer);
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(0);
        linearLayout.setPadding(0, dp(12), 0, 0);
        TextView actionButton = actionButton("Cancel", this.colLine, this.colMuted);
        actionButton.setOnClickListener(new View.OnClickListener() { // from class: au.com.dss.gatehouse.MainActivity.69
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                MainActivity.this.hapticClick();
                createTacticalDialog.dismiss();
            }
        });
        linearLayout.addView(actionButton);
        TextView actionButton2 = actionButton("✓ Save Inspection", this.colCyan, this.colAccentInk);
        actionButton2.setOnClickListener(new View.OnClickListener() { // from class: au.com.dss.gatehouse.MainActivity.70
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                MainActivity.this.hapticHeavyClick();
                MainActivity.this.registerActivity();
                MainActivity.this.tap(str + " Factory Floor", str2);
                String trim = modernInputField.getText().toString().trim();
                StringBuilder sb = new StringBuilder();
                sb.append(str).append(": ");
                for (int i2 = 0; i2 < arrayList.size(); i2++) {
                    if (i2 > 0) {
                        sb.append(", ");
                    }
                    sb.append((String) arrayList.get(i2));
                }
                if (!trim.isEmpty()) {
                    sb.append(" · ").append(trim);
                }
                String sb2 = sb.toString();
                if (MainActivity.this.oneLine(sb2)) {
                    MainActivity.this.note(0, sb2);
                    createTacticalDialog.dismiss();
                } else {
                    MainActivity.this.banner.setText("notes must be one line");
                    MainActivity.this.banner.setVisibility(0);
                }
            }
        });
        LinearLayout.LayoutParams layoutParams2 = new LinearLayout.LayoutParams(0, -2, 1.4f);
        layoutParams2.leftMargin = dp(8);
        actionButton2.setLayoutParams(layoutParams2);
        linearLayout.addView(actionButton2);
        dialogContainer.addView(linearLayout);
        createTacticalDialog.show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateCheckItemStyle(TextView textView, boolean z, boolean z2) {
        if (z) {
            textView.setTextColor(z2 ? this.colEmerald : this.colAccent);
            textView.setBackground(rounded(z2 ? this.colEmeraldSoft : this.colAccentSoft, dp(12)));
        } else {
            textView.setTextColor(this.colMuted);
            textView.setBackground(pressable(this.colPanel2, dp(12)));
        }
    }

    private TextView formSectionLabel(String str) {
        TextView textView = new TextView(this);
        textView.setText(str);
        textView.setTextColor(this.colQuiet);
        textView.setTextSize(10.0f);
        textView.setTypeface(Typeface.DEFAULT_BOLD);
        textView.setLetterSpacing(0.12f);
        textView.setPadding(0, dp(10), 0, dp(6));
        return textView;
    }

    private EditText modernInputField(String str) {
        EditText editText = new EditText(this);
        editText.setHint(str);
        editText.setTextColor(this.colPale);
        editText.setHintTextColor(this.colQuiet);
        editText.setTextSize(13.0f);
        editText.setBackground(rounded(this.colPanel2, dp(12)));
        editText.setPadding(dp(14), dp(12), dp(14), dp(12));
        editText.setInputType(147457);
        editText.setMinLines(2);
        editText.setGravity(48);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, -2);
        layoutParams.bottomMargin = dp(8);
        editText.setLayoutParams(layoutParams);
        return editText;
    }

    private TextView actionButton(String str, int i, int i2) {
        TextView textView = new TextView(this);
        textView.setText(str);
        textView.setTextColor(i2);
        textView.setTextSize(12.5f);
        textView.setTypeface(Typeface.DEFAULT_BOLD);
        textView.setGravity(17);
        textView.setBackground(pressable(i, dp(12)));
        textView.setPadding(dp(10), dp(12), dp(10), dp(12));
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(0, -2, 1.0f);
        layoutParams.leftMargin = dp(3);
        layoutParams.rightMargin = dp(3);
        textView.setLayoutParams(layoutParams);
        return textView;
    }

    private TextView fullActionButton(String str, int i, int i2) {
        TextView textView = new TextView(this);
        textView.setText(str);
        textView.setTextColor(i2);
        textView.setTextSize(12.5f);
        textView.setTypeface(Typeface.DEFAULT_BOLD);
        textView.setGravity(17);
        textView.setBackground(pressable(i, dp(12)));
        textView.setPadding(dp(12), dp(12), dp(12), dp(12));
        textView.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
        return textView;
    }

    private HorizontalScrollView buildChipGroup(String[] strArr, final String[] strArr2, boolean z, final int i) {
        HorizontalScrollView horizontalScrollView = new HorizontalScrollView(this);
        horizontalScrollView.setHorizontalScrollBarEnabled(false);
        final LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(0);
        linearLayout.setPadding(0, 0, 0, dp(8));
        for (final String str : strArr) {
            TextView textView = new TextView(this);
            textView.setText(str);
            textView.setTextSize(11.0f);
            textView.setTypeface(Typeface.DEFAULT_BOLD);
            textView.setPadding(dp(12), dp(7), dp(12), dp(7));
            boolean equals = str.equals(strArr2[0]);
            textView.setTextColor(equals ? this.colAccentInk : this.colMuted);
            textView.setBackground(rounded(equals ? i : this.colPanel2, dp(16)));
            textView.setOnClickListener(new View.OnClickListener() { // from class: au.com.dss.gatehouse.MainActivity.71
                @Override // android.view.View.OnClickListener
                public void onClick(View view) {
                    MainActivity.this.hapticClick();
                    strArr2[0] = str;
                    for (int i2 = 0; i2 < linearLayout.getChildCount(); i2++) {
                        TextView textView2 = (TextView) linearLayout.getChildAt(i2);
                        boolean equals2 = textView2.getText().toString().equals(str);
                        MainActivity mainActivity = MainActivity.this;
                        textView2.setTextColor(equals2 ? mainActivity.colAccentInk : mainActivity.colMuted);
                        textView2.setBackground(MainActivity.this.rounded(equals2 ? i : MainActivity.this.colPanel2, MainActivity.this.dp(16)));
                    }
                }
            });
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-2, -2);
            layoutParams.rightMargin = dp(6);
            textView.setLayoutParams(layoutParams);
            linearLayout.addView(textView);
        }
        horizontalScrollView.addView(linearLayout);
        return horizontalScrollView;
    }

    private HorizontalScrollView buildMultiChipGroup(String[] strArr, final ArrayList<String> arrayList, final int i) {
        HorizontalScrollView horizontalScrollView = new HorizontalScrollView(this);
        horizontalScrollView.setHorizontalScrollBarEnabled(false);
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(0);
        linearLayout.setPadding(0, 0, 0, dp(8));
        for (final String str : strArr) {
            final TextView textView = new TextView(this);
            textView.setText(str);
            textView.setTextSize(11.0f);
            textView.setTypeface(Typeface.DEFAULT_BOLD);
            textView.setPadding(dp(12), dp(7), dp(12), dp(7));
            boolean contains = arrayList.contains(str);
            textView.setTextColor(contains ? this.colAccentInk : this.colMuted);
            textView.setBackground(rounded(contains ? i : this.colPanel2, dp(16)));
            textView.setOnClickListener(new View.OnClickListener() { // from class: au.com.dss.gatehouse.MainActivity.72
                @Override // android.view.View.OnClickListener
                public void onClick(View view) {
                    MainActivity.this.hapticClick();
                    if (arrayList.contains(str)) {
                        arrayList.remove(str);
                        textView.setTextColor(MainActivity.this.colMuted);
                        textView.setBackground(MainActivity.this.rounded(MainActivity.this.colPanel2, MainActivity.this.dp(16)));
                    } else {
                        arrayList.add(str);
                        textView.setTextColor(MainActivity.this.colAccentInk);
                        textView.setBackground(MainActivity.this.rounded(i, MainActivity.this.dp(16)));
                    }
                }
            });
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-2, -2);
            layoutParams.rightMargin = dp(6);
            textView.setLayoutParams(layoutParams);
            linearLayout.addView(textView);
        }
        horizontalScrollView.addView(linearLayout);
        return horizontalScrollView;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public byte[] bitmapToJpegBytes(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, byteArrayOutputStream);
        return byteArrayOutputStream.toByteArray();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public byte[] readFileBytes(File file) {
        try {
            FileInputStream fileInputStream = new FileInputStream(file);
            byte[] bArr = new byte[(int) file.length()];
            fileInputStream.read(bArr);
            fileInputStream.close();
            return bArr;
        } catch (Exception e) {
            return new byte[0];
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public String sha256Hex(byte[] bArr) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(bArr);
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", Byte.valueOf(b)));
            }
            return sb.toString();
        } catch (Exception e) {
            return "00000000";
        }
    }

    private TextView tonightLabel() {
        this.tonightTitle = label("TONIGHT'S VERIFIED RECORD");
        return this.tonightTitle;
    }

    private void fillTonight() {
        this.tonight.removeAllViews();
        int i = 0;
        int i2 = 1;
        while (true) {
            String entryLine = Core.entryLine(i2);
            if (entryLine.length() == 0) {
                break;
            }
            this.tonight.addView(entryRow(entryLine, i2));
            i++;
            i2++;
        }
        for (int i3 = 0; i3 < this.pending.size(); i3++) {
            this.tonight.addView(pendingRow(this.pending.get(i3)));
        }
        boolean z = i + this.pending.size() > 0;
        this.tonight.setVisibility(z ? 0 : 8);
        this.tonightTitle.setVisibility(z ? 0 : 8);
    }

    private LinearLayout pendingRow(final Pending pending) {
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(1);
        linearLayout.setBackground(outlined(this.colAccent, dp(14)));
        linearLayout.setPadding(dp(14), dp(12), dp(14), dp(12));
        int max = (int) Math.max(0L, ((HOLD_MS - (SystemClock.elapsedRealtime() - pending.created)) + 999) / 1000);
        LinearLayout linearLayout2 = new LinearLayout(this);
        linearLayout2.setOrientation(0);
        linearLayout2.setGravity(16);
        TextView textView = new TextView(this);
        textView.setText(clock(pending.occurred) + "  " + (pending.checkpoint ? pending.label : pending.text));
        textView.setTextColor(this.colAccent);
        textView.setTextSize(13.0f);
        textView.setTypeface(Typeface.DEFAULT_BOLD);
        textView.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));
        linearLayout2.addView(textView);
        TextView textView2 = new TextView(this);
        textView2.setText("UNDO");
        textView2.setTextColor(this.colAccentInk);
        textView2.setTextSize(10.0f);
        textView2.setTypeface(Typeface.DEFAULT_BOLD);
        textView2.setPadding(dp(8), dp(4), dp(8), dp(4));
        textView2.setBackground(rounded(this.colAccent, dp(6)));
        textView2.setOnClickListener(new View.OnClickListener() { // from class: au.com.dss.gatehouse.MainActivity.73
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                MainActivity.this.hapticHeavyClick();
                MainActivity.this.takeBack(pending);
            }
        });
        linearLayout2.addView(textView2);
        linearLayout.addView(linearLayout2);
        TextView textView3 = new TextView(this);
        textView3.setText("held in buffer for " + max + "s  ·  swipe or tap to discard");
        textView3.setTextColor(this.colMuted);
        textView3.setTextSize(11.0f);
        textView3.setPadding(0, dp(4), 0, dp(8));
        linearLayout.addView(textView3);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, -2);
        layoutParams.bottomMargin = dp(8);
        linearLayout.setLayoutParams(layoutParams);
        linearLayout.setOnTouchListener(new View.OnTouchListener() { // from class: au.com.dss.gatehouse.MainActivity.74
            private boolean dragging;
            private float from;

            @Override // android.view.View.OnTouchListener
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getActionMasked()) {
                    case 0:
                        this.from = motionEvent.getRawX();
                        this.dragging = true;
                        return true;
                    case 1:
                    case 3:
                        float rawX = motionEvent.getRawX() - this.from;
                        this.dragging = false;
                        if (Math.abs(rawX) > MainActivity.this.dp(110)) {
                            MainActivity.this.hapticHeavyClick();
                            view.post(new Runnable() { // from class: au.com.dss.gatehouse.MainActivity.74.1
                                @Override // java.lang.Runnable
                                public void run() {
                                    MainActivity.this.takeBack(pending);
                                }
                            });
                        } else {
                            view.setTranslationX(0.0f);
                            view.setAlpha(1.0f);
                        }
                        return true;
                    case 2:
                        if (this.dragging) {
                            float rawX2 = motionEvent.getRawX() - this.from;
                            view.setTranslationX(rawX2);
                            view.setAlpha(Math.max(0.25f, 1.0f - (Math.abs(rawX2) / (MainActivity.this.dp(200) * 1.0f))));
                        }
                        return true;
                    default:
                        return false;
                }
            }
        });
        return linearLayout;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void takeBack(Pending pending) {
        registerActivity();
        this.pending.remove(pending);
        savePending();
        this.banner.setText("taken back, and never written to the record");
        this.banner.setVisibility(0);
        refresh();
    }

    private LinearLayout entryRow(String str, int i) {
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(1);
        linearLayout.setBackground(rounded(this.colPanel, dp(12)));
        linearLayout.setPadding(dp(14), dp(10), dp(14), dp(10));
        TextView textView = new TextView(this);
        textView.setText(str);
        textView.setTextSize(13.0f);
        textView.setTextColor(this.colPale);
        textView.setTypeface(Typeface.MONOSPACE);
        linearLayout.addView(textView);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, -2);
        layoutParams.bottomMargin = dp(6);
        linearLayout.setLayoutParams(layoutParams);
        return linearLayout;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public GradientDrawable rounded(int i, int i2) {
        GradientDrawable gradientDrawable = new GradientDrawable();
        gradientDrawable.setColor(i);
        gradientDrawable.setCornerRadius(i2);
        return gradientDrawable;
    }

    private GradientDrawable outlined(int i, int i2) {
        GradientDrawable gradientDrawable = new GradientDrawable();
        gradientDrawable.setColor(0);
        gradientDrawable.setStroke(dp(1), i);
        gradientDrawable.setCornerRadius(i2);
        return gradientDrawable;
    }

    private RippleDrawable pressable(int i, int i2) {
        return new RippleDrawable(ColorStateList.valueOf(1155901756), rounded(i, i2), null);
    }

    private RippleDrawable pressableOutline(int i, int i2) {
        return new RippleDrawable(ColorStateList.valueOf(1155901756), outlined(i, i2), null);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int dp(int i) {
        return (int) (i * getResources().getDisplayMetrics().density);
    }

    private void startShift() {
        int nowMinutes = nowMinutes();
        this.openedAt = nowMinutes;
        Core.siteBegin("Hume Doors & Timber, Kingston");
        for (int i = 0; i < EXTERNAL_CHOICES.length; i += 2) {
            Core.siteAddPoint(EXTERNAL_CHOICES[i], EXTERNAL_CHOICES[i + 1]);
        }
        for (int i2 = 0; i2 < INTERNAL_LOTS.length; i2 += 2) {
            Core.siteAddPoint(INTERNAL_LOTS[i2] + " Factory Floor", INTERNAL_LOTS[i2 + 1]);
        }
        for (int i3 = 0; i3 < FIRE_POINTS.length; i3 += 2) {
            Core.siteAddPoint(FIRE_POINTS[i3], FIRE_POINTS[i3 + 1]);
        }
        Core.sitePolicy(1, 240, 0);
        Core.setAttribution(0, 0);
        Core.setGuard("g-kelso", "R. Kelso", "SAMPLE-LIC", "typed", "");
        answer(Core.openShift(Core.genesis(), Core.siteHash(), nowMinutes, nowMinutes, "on site, handover from day crew taken"));
        hidePage();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void tap(String str, String str2) {
        registerActivity();
        Pending pending = new Pending();
        pending.checkpoint = true;
        pending.label = str;
        pending.uid = str2;
        int i = this.taps + 1;
        this.taps = i;
        pending.taps = i;
        pending.occurred = nowMinutes();
        pending.created = SystemClock.elapsedRealtime();
        hold(pending);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void note(int i, String str) {
        registerActivity();
        Pending pending = new Pending();
        pending.checkpoint = false;
        pending.topic = i;
        pending.text = str;
        pending.occurred = nowMinutes();
        pending.created = SystemClock.elapsedRealtime();
        hold(pending);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean oneLine(String str) {
        for (int i = 0; i < str.length(); i++) {
            char charAt = str.charAt(i);
            if (charAt < ' ' || charAt == 127) {
                return false;
            }
        }
        return true;
    }

    private void hold(Pending pending) {
        this.pending.add(pending);
        while (this.pending.size() > MAX_HELD) {
            commit(this.pending.remove(0));
        }
        savePending();
        this.banner.setVisibility(8);
        refresh();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void commitDue() {
        long elapsedRealtime = SystemClock.elapsedRealtime();
        int i = 0;
        boolean z = false;
        while (i < this.pending.size()) {
            Pending pending = this.pending.get(i);
            if (elapsedRealtime - pending.created >= HOLD_MS) {
                this.pending.remove(i);
                commit(pending);
                z = true;
            } else {
                i++;
            }
        }
        if (z) {
            savePending();
            refresh();
        }
    }

    private void commitAll() {
        while (!this.pending.isEmpty()) {
            commit(this.pending.remove(0));
        }
        savePending();
    }

    private void commit(Pending pending) {
        int max = Math.max(nowMinutes(), Core.lastRecorded());
        if (pending.checkpoint) {
            answer(Core.addCheckpoint(pending.occurred, max, pending.label, pending.uid, pending.taps, 2));
        } else {
            answer(Core.addNote(4, pending.topic, pending.occurred, max, pending.text, 0));
        }
        if (this.chainBannerView != null) {
            this.chainBannerView.triggerRipple();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void sealAndShow() {
        hapticSealThud();
        registerActivity();
        commitAll();
        int max = Math.max(nowMinutes(), Core.lastRecorded());
        answer(Core.seal(max, max, "off site"));
        String report = Core.report(this.openedAt, max);
        if (report.length() > 0) {
            this.page.setText(report);
            this.page.setVisibility(0);
            this.pageTitle.setVisibility(0);
            this.btnShareReport.setVisibility(0);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void shareHandoverReport() {
        String obj = this.page.getText().toString();
        if (obj.isEmpty()) {
            return;
        }
        try {
            Intent intent = new Intent("android.intent.action.SEND");
            intent.putExtra("android.intent.extra.SUBJECT", "06:05 AM Morning Handover Report · Hume Doors & Timber (Kingston)");
            intent.putExtra("android.intent.extra.TEXT", obj);
            intent.setType("text/plain");
            startActivity(Intent.createChooser(intent, "Share Morning Handover Report"));
        } catch (Exception e) {
        }
    }

    private boolean keepArchive() {
        String archive = Core.archive();
        if (archive.length() == 0) {
            return false;
        }
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(new File(getFilesDir(), "record-" + Core.head().substring(0, 16) + ".txt"));
            fileOutputStream.write(archive.getBytes("UTF-8"));
            fileOutputStream.close();
            return Core.kept() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void nextShift() {
        hapticHeavyClick();
        registerActivity();
        int nowMinutes = nowMinutes();
        if (!keepArchive()) {
            this.banner.setText("could not write the record out; it has not been kept");
            this.banner.setVisibility(0);
            return;
        }
        int continueShift = Core.continueShift(nowMinutes, nowMinutes, "on site, continuation");
        if (continueShift == 0) {
            this.openedAt = nowMinutes;
            hidePage();
        }
        answer(continueShift);
    }

    private void savePending() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < this.pending.size(); i++) {
            Pending pending = this.pending.get(i);
            String str = pending.checkpoint ? pending.label : pending.text;
            sb.append(pending.checkpoint ? 1 : 0).append('|').append(pending.topic).append('|').append(pending.taps).append('|').append(pending.occurred).append('|').append(pending.created).append('|').append(pending.uid).append('|').append(str.length()).append('|').append(str).append('\n');
        }
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(new File(getFilesDir(), "pending.txt"));
            fileOutputStream.write(sb.toString().getBytes("UTF-8"));
            fileOutputStream.close();
        } catch (Exception e) {
        }
    }

    private void loadPending() {
        this.pending.clear();
        File file = new File(getFilesDir(), "pending.txt");
        if (!file.exists()) {
            return;
        }
        try {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
            while (true) {
                String readLine = bufferedReader.readLine();
                if (readLine != null) {
                    Pending parsePending = parsePending(readLine);
                    if (parsePending != null) {
                        this.pending.add(parsePending);
                    }
                } else {
                    bufferedReader.close();
                    return;
                }
            }
        } catch (Exception e) {
        }
    }

    private Pending parsePending(String str) {
        try {
            int[] iArr = new int[7];
            int i = 0;
            for (int i2 = 0; i2 < str.length() && i < 7; i2++) {
                if (str.charAt(i2) == '|') {
                    iArr[i] = i2;
                    i++;
                }
            }
            if (i < 7) {
                return null;
            }
            Pending pending = new Pending();
            pending.checkpoint = str.charAt(0) == '1';
            pending.topic = Integer.parseInt(str.substring(iArr[0] + 1, iArr[1]));
            pending.taps = Integer.parseInt(str.substring(iArr[1] + 1, iArr[2]));
            pending.occurred = Integer.parseInt(str.substring(iArr[2] + 1, iArr[3]));
            pending.created = Long.parseLong(str.substring(iArr[3] + 1, iArr[4]));
            pending.uid = str.substring(iArr[4] + 1, iArr[5]);
            int parseInt = Integer.parseInt(str.substring(iArr[5] + 1, iArr[6]));
            if (iArr[6] + 1 + parseInt > str.length()) {
                return null;
            }
            String substring = str.substring(iArr[6] + 1, iArr[6] + 1 + parseInt);
            if (pending.checkpoint) {
                pending.label = substring;
            } else {
                pending.text = substring;
            }
            if (pending.taps > this.taps) {
                this.taps = pending.taps;
            }
            return pending;
        } catch (Exception e) {
            return null;
        }
    }

    private void hidePage() {
        this.page.setVisibility(8);
        this.pageTitle.setVisibility(8);
        this.btnShareReport.setVisibility(8);
    }

    private void answer(int i) {
        String lastReason = Core.lastReason();
        if (i == 0 || lastReason.length() == 0) {
            this.banner.setVisibility(8);
        } else {
            this.banner.setText(lastReason);
            this.banner.setVisibility(0);
        }
        refresh();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void refresh() {
        boolean z = Core.isSealed() == 1;
        int entryCount = Core.entryCount();
        this.pills.removeAllViews();
        this.pills.addView(pill(entryCount + (entryCount == 1 ? " entry" : " entries"), false));
        this.pills.addView(pill(z ? "SEALED" : "OPEN", z));
        this.pills.addView(pill(Core.verified() == 1 ? "✓ VERIFIED" : "BROKEN", false));
        markExternalCard(this.tileExternalFull, EXTERNAL_CHOICES[0]);
        markExternalCard(this.tileExternalHalf, EXTERNAL_CHOICES[2]);
        for (int i = 0; i < this.internalBadgesRow.getChildCount(); i++) {
            TextView textView = (TextView) this.internalBadgesRow.getChildAt(i);
            markLotBadge(textView, ((String) textView.getTag()) + " Factory Floor");
        }
        int i2 = 0;
        for (int i3 = 0; i3 < this.fireList.getChildCount(); i3++) {
            LinearLayout linearLayout = (LinearLayout) this.fireList.getChildAt(i3);
            if (markFireRow(linearLayout, (String) linearLayout.getTag())) {
                i2++;
            }
        }
        int length = FIRE_POINTS.length / 2;
        boolean z2 = i2 >= length;
        this.fireStatusChip.setText(i2 + "/" + length + (z2 ? " COMPLETE" : " CHECKED"));
        this.fireStatusChip.setTextColor(z2 ? this.colEmerald : this.colMuted);
        this.fireStatusChip.setBackground(rounded(z2 ? this.colEmeraldSoft : this.colPanel2, dp(6)));
        fillTonight();
        setLive(this.externalRow, !z);
        setLive(this.internalBadgesRow, !z);
        setLive(this.fireCard, !z);
        setLive(this.dock, !z);
        if (z) {
            this.primary.setText("START THE NEXT RECORD");
            this.primary.setTextColor(this.colPale);
            this.primary.setBackground(pressableOutline(this.colLine, dp(16)));
            this.primary.setOnClickListener(new View.OnClickListener() { // from class: au.com.dss.gatehouse.MainActivity.75
                @Override // android.view.View.OnClickListener
                public void onClick(View view) {
                    MainActivity.this.hapticClick();
                    MainActivity.this.nextShift();
                }
            });
            return;
        }
        this.primary.setText("🔒 SEAL SHIFT & HANDOVER (06:05 AM)");
        this.primary.setTextColor(this.colAccentInk);
        this.primary.setBackground(pressable(this.colAccent, dp(16)));
        this.primary.setOnClickListener(new View.OnClickListener() { // from class: au.com.dss.gatehouse.MainActivity.76
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                MainActivity.this.hapticHeavyClick();
                MainActivity.this.askThenSeal();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void askThenSeal() {
        String str;
        registerActivity();
        int entryCount = Core.entryCount() + this.pending.size();
        if (this.pending.isEmpty()) {
            str = "";
        } else {
            str = "\n\nIncludes " + this.pending.size() + (this.pending.size() == 1 ? " entry" : " entries") + " in buffer that will be written in first.";
        }
        LinearLayout dialogContainer = dialogContainer("🔒 Biometric Shift Seal & Handover", "FINAL LEGAL ACTION", this.colAccent);
        TextView textView = new TextView(this);
        textView.setText("Sealing locks tonight's Ada record with " + entryCount + (entryCount != 1 ? " entries" : " entry") + " permanently under SHA-256." + str + "\n\nTouch and hold the affirmation pad below to execute cryptographic seal:");
        textView.setTextColor(this.colMuted);
        textView.setTextSize(12.0f);
        textView.setPadding(0, 0, 0, dp(14));
        dialogContainer.addView(textView);
        final Dialog createTacticalDialog = createTacticalDialog(dialogContainer);
        BiometricSealPadView biometricSealPadView = new BiometricSealPadView(this, new Runnable() { // from class: au.com.dss.gatehouse.MainActivity.77
            @Override // java.lang.Runnable
            public void run() {
                createTacticalDialog.dismiss();
                MainActivity.this.sealAndShow();
            }
        });
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, dp(140));
        layoutParams.bottomMargin = dp(12);
        biometricSealPadView.setLayoutParams(layoutParams);
        dialogContainer.addView(biometricSealPadView);
        TextView actionButton = actionButton("Cancel / Back", this.colLine, this.colMuted);
        actionButton.setOnClickListener(new View.OnClickListener() { // from class: au.com.dss.gatehouse.MainActivity.78
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                MainActivity.this.hapticClick();
                createTacticalDialog.dismiss();
            }
        });
        dialogContainer.addView(actionButton);
        createTacticalDialog.show();
    }

    private String formatTimeSinceVisited(int i) {
        int max = Math.max(0, nowMinutes() - i);
        if (max < 60) {
            return max + "m ago";
        }
        if (max >= 1440) {
            return (max / 1440) + "d " + ((max % 1440) / 60) + "h ago";
        }
        int i2 = max / 60;
        int i3 = max % 60;
        return i2 + "h " + (i3 > 0 ? i3 + "m " : "") + "ago";
    }

    private int getElapsedBadgeColor(int i) {
        int max = Math.max(0, nowMinutes() - i);
        return max <= 75 ? this.colEmerald : max <= 120 ? this.colAccent : this.colCrimson;
    }

    private void markExternalCard(LinearLayout linearLayout, String str) {
        if (linearLayout == null || linearLayout.getChildCount() < 4) {
            return;
        }
        int pointVisits = Core.pointVisits(str);
        try {
            TextView textView = (TextView) ((LinearLayout) linearLayout.getChildAt(0)).getChildAt(0);
            TextView textView2 = (TextView) linearLayout.getChildAt(3);
            if (pointVisits <= 0) {
                textView.setText(str);
                textView.setTextColor(this.colPale);
                textView2.setText("TAP TO LOG");
                textView2.setTextColor(this.colAccent);
                textView2.setBackground(rounded(this.colPanel2, dp(6)));
                return;
            }
            int pointLast = Core.pointLast(str);
            String formatTimeSinceVisited = formatTimeSinceVisited(pointLast);
            int elapsedBadgeColor = getElapsedBadgeColor(pointLast);
            textView.setText(str + " (x" + pointVisits + ")");
            textView.setTextColor(elapsedBadgeColor);
            textView2.setText("✓ " + clock(pointLast) + " (" + formatTimeSinceVisited + ")");
            textView2.setTextColor(this.colEmerald);
            textView2.setBackground(rounded(this.colEmeraldSoft, dp(6)));
        } catch (Exception e) {
        }
    }

    private void markLotBadge(TextView textView, String str) {
        if (textView == null) {
            return;
        }
        int pointVisits = Core.pointVisits(str);
        String str2 = (String) textView.getTag();
        if (str2 == null) {
            str2 = textView.getText().toString();
        }
        if (pointVisits <= 0) {
            textView.setText(str2);
            textView.setTextColor(this.colPale);
            textView.setBackground(pressable(this.colPanel, dp(10)));
        } else {
            getElapsedBadgeColor(Core.pointLast(str));
            textView.setText("✓ " + str2);
            textView.setTextColor(this.colEmerald);
            textView.setBackground(pressable(this.colEmeraldSoft, dp(10)));
        }
    }

    private boolean markFireRow(LinearLayout linearLayout, String str) {
        if (linearLayout == null || linearLayout.getChildCount() < 3) {
            return false;
        }
        try {
            TextView textView = (TextView) linearLayout.getChildAt(1);
            TextView textView2 = (TextView) linearLayout.getChildAt(2);
            int pointVisits = Core.pointVisits(str);
            ArrayList<PressureRecord> arrayList = this.pressureHistory.get(str);
            String str2 = "";
            if (arrayList != null && !arrayList.isEmpty()) {
                str2 = " · " + arrayList.get(arrayList.size() - 1).pressureKpa + " kPa";
            }
            if (pointVisits <= 0) {
                textView.setTextColor(this.colPale);
                textView2.setText("1,200 kPa");
                textView2.setTextColor(this.colMuted);
                textView2.setBackground(rounded(this.colPanel2, dp(6)));
                return false;
            }
            int pointLast = Core.pointLast(str);
            String formatTimeSinceVisited = formatTimeSinceVisited(pointLast);
            textView.setTextColor(getElapsedBadgeColor(pointLast));
            textView2.setText("✓ " + formatTimeSinceVisited + str2);
            textView2.setTextColor(this.colEmerald);
            textView2.setBackground(rounded(this.colEmeraldSoft, dp(6)));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private String clock(int i) {
        int i2 = (i / 60) % 24;
        int i3 = i % 60;
        return (i2 < 10 ? "0" : "") + i2 + ":" + (i3 >= 10 ? "" : "0") + i3;
    }

    private void setLive(LinearLayout linearLayout, boolean z) {
        linearLayout.setAlpha(z ? 1.0f : 0.35f);
        for (int i = 0; i < linearLayout.getChildCount(); i++) {
            View childAt = linearLayout.getChildAt(i);
            childAt.setEnabled(z);
            if (childAt instanceof LinearLayout) {
                LinearLayout linearLayout2 = (LinearLayout) childAt;
                for (int i2 = 0; i2 < linearLayout2.getChildCount(); i2++) {
                    linearLayout2.getChildAt(i2).setEnabled(z);
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void checkWelfareDue() {
        if (this.isWelfareDialogShowing || Core.isSealed() == 1 || SystemClock.elapsedRealtime() - this.lastActivityTimeMs < WELFARE_INTERVAL_MS) {
            return;
        }
        showWelfareCheckDialog();
    }

    private void showWelfareCheckDialog() {
        this.isWelfareDialogShowing = true;
        hapticHeavyClick();
        final long elapsedRealtime = SystemClock.elapsedRealtime();
        LinearLayout dialogContainer = dialogContainer("🦺 Lone Worker Welfare Check", "WHS COMPLIANCE", this.colEmerald);
        TextView textView = new TextView(this);
        textView.setText("No site activity has been logged in 90 minutes.\n\nPlease confirm your active on-duty status:");
        textView.setTextColor(this.colPale);
        textView.setTextSize(13.0f);
        textView.setPadding(0, 0, 0, dp(14));
        dialogContainer.addView(textView);
        final TextView textView2 = new TextView(this);
        textView2.setText("Auto-Escalating to DSS Control in 05:00");
        textView2.setTextColor(this.colAccent);
        textView2.setTextSize(12.0f);
        textView2.setTypeface(Typeface.MONOSPACE);
        textView2.setGravity(17);
        textView2.setPadding(0, 0, 0, dp(18));
        dialogContainer.addView(textView2);
        TextView actionButton = actionButton("✓ I AM SAFE · CONFIRM ON DUTY", this.colEmerald, this.colAccentInk);
        actionButton.setTextSize(15.0f);
        actionButton.setPadding(dp(18), dp(18), dp(18), dp(18));
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, -2);
        layoutParams.bottomMargin = dp(10);
        actionButton.setLayoutParams(layoutParams);
        dialogContainer.addView(actionButton);
        TextView actionButton2 = actionButton("🚨 EMERGENCY ASSISTANCE (000)", this.colCrimson, this.colPale);
        actionButton2.setTextSize(13.0f);
        actionButton2.setPadding(dp(12), dp(12), dp(12), dp(12));
        actionButton2.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
        dialogContainer.addView(actionButton2);
        final Dialog createTacticalDialog = createTacticalDialog(dialogContainer);
        final Handler handler = new Handler();
        handler.post(new Runnable() { // from class: au.com.dss.gatehouse.MainActivity.79
            @Override // java.lang.Runnable
            public void run() {
                if (MainActivity.this.isWelfareDialogShowing && createTacticalDialog.isShowing()) {
                    long elapsedRealtime2 = 300000 - (SystemClock.elapsedRealtime() - elapsedRealtime);
                    if (elapsedRealtime2 <= 0) {
                        textView2.setText("⚠️ UNCONFIRMED · ESCALATING TO CONTROL ROOM");
                        textView2.setTextColor(MainActivity.this.colCrimson);
                    } else {
                        int i = (int) (elapsedRealtime2 / 1000);
                        textView2.setText(String.format(Locale.US, "Auto-Escalating to DSS Control in %02d:%02d", Integer.valueOf(i / 60), Integer.valueOf(i % 60)));
                        handler.postDelayed(this, 1000L);
                    }
                }
            }
        });
        actionButton.setOnClickListener(new View.OnClickListener() { // from class: au.com.dss.gatehouse.MainActivity.80
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                MainActivity.this.hapticHeavyClick();
                MainActivity.this.registerActivity();
                MainActivity.this.note(0, "[WELFARE] Officer R. Kelso confirmed active on duty (WHS Check)");
                MainActivity.this.isWelfareDialogShowing = false;
                createTacticalDialog.dismiss();
            }
        });
        actionButton2.setOnClickListener(new View.OnClickListener() { // from class: au.com.dss.gatehouse.MainActivity.81
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                MainActivity.this.hapticSealThud();
                MainActivity.this.registerActivity();
                MainActivity.this.note(2, "[EMERGENCY 000 DISPATCH] Triggered from lone worker welfare check");
                MainActivity.this.dialNumber("000");
                MainActivity.this.isWelfareDialogShowing = false;
                createTacticalDialog.dismiss();
            }
        });
        createTacticalDialog.setOnDismissListener(new DialogInterface.OnDismissListener() { // from class: au.com.dss.gatehouse.MainActivity.82
            @Override // android.content.DialogInterface.OnDismissListener
            public void onDismiss(DialogInterface dialogInterface) {
                MainActivity.this.isWelfareDialogShowing = false;
            }
        });
        createTacticalDialog.show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class AnimatedChainBannerView extends View {
        private final Paint bgPaint;
        private final RectF rect;
        private final Paint ripplePaint;
        private float rippleX;
        private final Paint tagPaint;
        private final Paint textPaint;

        public AnimatedChainBannerView(Context context) {
            super(context);
            this.bgPaint = new Paint(1);
            this.textPaint = new Paint(1);
            this.tagPaint = new Paint(1);
            this.ripplePaint = new Paint(1);
            this.rect = new RectF();
            this.rippleX = -1.0f;
            this.bgPaint.setStyle(Paint.Style.FILL);
            this.textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, 1));
            this.tagPaint.setTypeface(Typeface.create(Typeface.MONOSPACE, 1));
            this.ripplePaint.setStyle(Paint.Style.FILL);
        }

        public void triggerRipple() {
            ValueAnimator ofFloat = ValueAnimator.ofFloat(0.0f, 1.2f);
            ofFloat.setDuration(650L);
            ofFloat.setInterpolator(new DecelerateInterpolator());
            ofFloat.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: au.com.dss.gatehouse.MainActivity.AnimatedChainBannerView.1
                @Override // android.animation.ValueAnimator.AnimatorUpdateListener
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    AnimatedChainBannerView.this.rippleX = ((Float) valueAnimator.getAnimatedValue()).floatValue();
                    AnimatedChainBannerView.this.invalidate();
                }
            });
            ofFloat.start();
        }

        @Override // android.view.View
        protected void onDraw(Canvas canvas) {
            String str;
            super.onDraw(canvas);
            int width = getWidth();
            int height = getHeight();
            if (width <= 0 || height <= 0) {
                return;
            }
            float f = width;
            float f2 = height;
            this.rect.set(0.0f, 0.0f, f, f2);
            this.bgPaint.setColor(MainActivity.this.colPanel);
            canvas.drawRoundRect(this.rect, MainActivity.this.dp(12), MainActivity.this.dp(12), this.bgPaint);
            if (this.rippleX >= 0.0f && this.rippleX <= 1.2f) {
                float f3 = this.rippleX * f;
                this.ripplePaint.setShader(new LinearGradient(f3 - MainActivity.this.dp(80), 0.0f, f3 + MainActivity.this.dp(80), 0.0f, new int[]{15051068, 1441114428, -2012169855, 1096065}, (float[]) null, Shader.TileMode.CLAMP));
                canvas.drawRoundRect(this.rect, MainActivity.this.dp(12), MainActivity.this.dp(12), this.ripplePaint);
            }
            int entryCount = Core.entryCount();
            String head = Core.head();
            if (head.length() >= 8) {
                str = head.substring(0, 8) + "...";
            } else {
                str = "none";
            }
            Paint paint = new Paint(1);
            paint.setColor(MainActivity.this.colEmerald);
            float f4 = f2 / 2.0f;
            canvas.drawCircle(MainActivity.this.dp(16), f4, MainActivity.this.dp(4), paint);
            this.textPaint.setColor(MainActivity.this.colPale);
            this.textPaint.setTextSize(MainActivity.this.dp(11));
            this.textPaint.setTextAlign(Paint.Align.LEFT);
            canvas.drawText("Chain Secured · SHA-256", MainActivity.this.dp(28), f4 + MainActivity.this.dp(4), this.textPaint);
            String str2 = "HEAD: " + str + " [" + entryCount + "]";
            this.tagPaint.setTextSize(MainActivity.this.dp(10));
            RectF rectF = new RectF((f - this.tagPaint.measureText(str2)) - MainActivity.this.dp(24), MainActivity.this.dp(8), width - MainActivity.this.dp(10), height - MainActivity.this.dp(8));
            Paint paint2 = new Paint(1);
            paint2.setColor(MainActivity.this.colEmeraldSoft);
            canvas.drawRoundRect(rectF, MainActivity.this.dp(6), MainActivity.this.dp(6), paint2);
            this.tagPaint.setColor(MainActivity.this.colEmerald);
            this.tagPaint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(str2, rectF.centerX(), rectF.centerY() + MainActivity.this.dp(3), this.tagPaint);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class HorizonLevelerView extends View {
        private final Paint linePaint;
        private final Paint reticlePaint;
        private final Paint textPaint;

        public HorizonLevelerView(Context context) {
            super(context);
            this.linePaint = new Paint(1);
            this.reticlePaint = new Paint(1);
            this.textPaint = new Paint(1);
            this.linePaint.setStyle(Paint.Style.STROKE);
            this.linePaint.setStrokeWidth(MainActivity.this.dp(2));
            this.reticlePaint.setStyle(Paint.Style.STROKE);
            this.reticlePaint.setStrokeWidth(MainActivity.this.dp(2));
            this.textPaint.setTextAlign(Paint.Align.CENTER);
            this.textPaint.setTypeface(Typeface.create(Typeface.MONOSPACE, 1));
        }

        @Override // android.view.View
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            int width = getWidth();
            int height = getHeight();
            if (width <= 0 || height <= 0) {
                return;
            }
            float f = width / 2.0f;
            float f2 = height / 2.0f;
            float degrees = (float) Math.toDegrees(Math.atan2(MainActivity.this.lastAccel[0], Math.sqrt((MainActivity.this.lastAccel[1] * MainActivity.this.lastAccel[1]) + (MainActivity.this.lastAccel[2] * MainActivity.this.lastAccel[2]))));
            float degrees2 = (float) Math.toDegrees(Math.atan2(-MainActivity.this.lastAccel[1], MainActivity.this.lastAccel[2]));
            boolean z = Math.abs(degrees) < 0.9f && Math.abs(degrees2) < 0.9f;
            MainActivity mainActivity = MainActivity.this;
            int i = z ? mainActivity.colEmerald : mainActivity.colAccent;
            canvas.save();
            canvas.rotate(-degrees, f, f2);
            this.linePaint.setColor(i);
            canvas.drawLine(f - MainActivity.this.dp(90), f2, f - MainActivity.this.dp(30), f2, this.linePaint);
            canvas.drawLine(f + MainActivity.this.dp(30), f2, f + MainActivity.this.dp(90), f2, this.linePaint);
            canvas.drawLine(f - MainActivity.this.dp(90), f2, f - MainActivity.this.dp(90), f2 + MainActivity.this.dp(8), this.linePaint);
            canvas.drawLine(f + MainActivity.this.dp(90), f2, f + MainActivity.this.dp(90), f2 + MainActivity.this.dp(8), this.linePaint);
            this.reticlePaint.setColor(i);
            canvas.drawCircle(f, f2, MainActivity.this.dp(14), this.reticlePaint);
            canvas.drawCircle(f, f2, MainActivity.this.dp(3), this.reticlePaint);
            canvas.restore();
            this.textPaint.setColor(i);
            this.textPaint.setTextSize(MainActivity.this.dp(11));
            canvas.drawText(z ? "✓ 0.0° PERFECT LEVEL" : String.format(Locale.US, "ROLL: %+.1f°  PITCH: %+.1f°", Float.valueOf(degrees), Float.valueOf(degrees2)), f, f2 + MainActivity.this.dp(48), this.textPaint);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class BiometricSealPadView extends View {
        private final RectF circleRect;
        private final Paint glowPaint;
        private final Paint iconPaint;
        private boolean isTouching;
        private final Paint laserPaint;
        private final Runnable onCompletedAction;
        private final Handler pulseHandler;
        private final Paint ringPaint;
        private ValueAnimator sweepAnimator;
        private float sweepProgress;
        private final Paint textPaint;

        public BiometricSealPadView(Context context, Runnable runnable) {
            super(context);
            this.ringPaint = new Paint(1);
            this.laserPaint = new Paint(1);
            this.glowPaint = new Paint(1);
            this.textPaint = new Paint(1);
            this.iconPaint = new Paint(1);
            this.circleRect = new RectF();
            this.sweepProgress = 0.0f;
            this.isTouching = false;
            this.pulseHandler = new Handler();
            this.onCompletedAction = runnable;
            this.ringPaint.setStyle(Paint.Style.STROKE);
            this.ringPaint.setStrokeWidth(MainActivity.this.dp(4));
            this.laserPaint.setStyle(Paint.Style.STROKE);
            this.laserPaint.setStrokeWidth(MainActivity.this.dp(6));
            this.laserPaint.setStrokeCap(Paint.Cap.ROUND);
            this.glowPaint.setStyle(Paint.Style.STROKE);
            this.glowPaint.setStrokeWidth(MainActivity.this.dp(12));
            this.glowPaint.setStrokeCap(Paint.Cap.ROUND);
            this.textPaint.setTextAlign(Paint.Align.CENTER);
            this.textPaint.setTypeface(Typeface.create(Typeface.MONOSPACE, 1));
            this.iconPaint.setTextAlign(Paint.Align.CENTER);
            this.iconPaint.setTypeface(Typeface.DEFAULT_BOLD);
        }

        @Override // android.view.View
        public boolean onTouchEvent(MotionEvent motionEvent) {
            switch (motionEvent.getActionMasked()) {
                case 0:
                    this.isTouching = true;
                    MainActivity.this.hapticClick();
                    startSweep();
                    startHapticPulseLoop();
                    getParent().requestDisallowInterceptTouchEvent(true);
                    return true;
                case 1:
                case 3:
                    this.isTouching = false;
                    cancelSweep();
                    stopHapticPulseLoop();
                    getParent().requestDisallowInterceptTouchEvent(false);
                    return true;
                case 2:
                default:
                    return super.onTouchEvent(motionEvent);
            }
        }

        private void startSweep() {
            if (this.sweepAnimator != null && this.sweepAnimator.isRunning()) {
                this.sweepAnimator.cancel();
            }
            this.sweepAnimator = ValueAnimator.ofFloat(this.sweepProgress, 1.0f);
            this.sweepAnimator.setDuration((long) ((1.0f - this.sweepProgress) * 2200.0f));
            this.sweepAnimator.setInterpolator(new DecelerateInterpolator());
            this.sweepAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: au.com.dss.gatehouse.MainActivity.BiometricSealPadView.1
                @Override // android.animation.ValueAnimator.AnimatorUpdateListener
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    BiometricSealPadView.this.sweepProgress = ((Float) valueAnimator.getAnimatedValue()).floatValue();
                    BiometricSealPadView.this.invalidate();
                    if (BiometricSealPadView.this.sweepProgress >= 1.0f) {
                        BiometricSealPadView.this.stopHapticPulseLoop();
                        MainActivity.this.hapticSealThud();
                        if (BiometricSealPadView.this.onCompletedAction != null) {
                            BiometricSealPadView.this.onCompletedAction.run();
                        }
                    }
                }
            });
            this.sweepAnimator.start();
        }

        private void cancelSweep() {
            if (this.sweepAnimator != null && this.sweepAnimator.isRunning()) {
                this.sweepAnimator.cancel();
            }
            this.sweepAnimator = ValueAnimator.ofFloat(this.sweepProgress, 0.0f);
            this.sweepAnimator.setDuration((long) (this.sweepProgress * 400.0f));
            this.sweepAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: au.com.dss.gatehouse.MainActivity.BiometricSealPadView.2
                @Override // android.animation.ValueAnimator.AnimatorUpdateListener
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    BiometricSealPadView.this.sweepProgress = ((Float) valueAnimator.getAnimatedValue()).floatValue();
                    BiometricSealPadView.this.invalidate();
                }
            });
            this.sweepAnimator.start();
        }

        private void startHapticPulseLoop() {
            this.pulseHandler.removeCallbacksAndMessages(null);
            this.pulseHandler.post(new Runnable() { // from class: au.com.dss.gatehouse.MainActivity.BiometricSealPadView.3
                @Override // java.lang.Runnable
                public void run() {
                    if (BiometricSealPadView.this.isTouching && BiometricSealPadView.this.sweepProgress < 1.0f) {
                        MainActivity.this.hapticClick();
                        BiometricSealPadView.this.pulseHandler.postDelayed(this, Math.max(40L, (long) (180.0f - (BiometricSealPadView.this.sweepProgress * 130.0f))));
                    }
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void stopHapticPulseLoop() {
            this.pulseHandler.removeCallbacksAndMessages(null);
        }

        @Override // android.view.View
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            int width = getWidth();
            int height = getHeight();
            if (width <= 0 || height <= 0) {
                return;
            }
            float f = width / 2.0f;
            float f2 = height / 2.0f;
            float min = (Math.min(width, height) / 2.0f) - MainActivity.this.dp(16);
            this.circleRect.set(f - min, f2 - min, f + min, f2 + min);
            this.ringPaint.setColor(MainActivity.this.colLine);
            canvas.drawCircle(f, f2, min, this.ringPaint);
            if (this.sweepProgress > 0.01f) {
                float f3 = this.sweepProgress * 360.0f;
                this.laserPaint.setColor(MainActivity.this.colAccent);
                this.glowPaint.setColor(MainActivity.this.colAccent);
                this.glowPaint.setAlpha((int) ((this.sweepProgress * 120.0f) + 80.0f));
                canvas.drawArc(this.circleRect, -90.0f, f3, false, this.glowPaint);
                canvas.drawArc(this.circleRect, -90.0f, f3, false, this.laserPaint);
            }
            this.iconPaint.setColor(this.isTouching ? MainActivity.this.colAccent : MainActivity.this.colPale);
            this.iconPaint.setTextSize(MainActivity.this.dp(28));
            canvas.drawText("🔏", f, f2 - MainActivity.this.dp(4), this.iconPaint);
            this.textPaint.setColor(this.isTouching ? MainActivity.this.colAccent : MainActivity.this.colMuted);
            this.textPaint.setTextSize(MainActivity.this.dp(11));
            canvas.drawText(this.isTouching ? String.format(Locale.US, "AFFIRMING %d%%", Integer.valueOf((int) (this.sweepProgress * 100.0f))) : "TOUCH & HOLD 2.5s", f, f2 + MainActivity.this.dp(22), this.textPaint);
        }
    }

    public void triggerSunConureFlight() {
        if (this.conureOverlay != null) {
            hapticDoublePulse();
            this.conureOverlay.triggerFlight();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class SunConureFlightOverlayView extends View {
        private final Paint beakPaint;
        private final Path beakPath;
        private final Paint bodyPaint;
        private final Path bodyPath;
        private ValueAnimator flightAnimator;
        private float flightProgress;
        private boolean isFlying;
        private final Path leftWing;
        private final Paint maskPaint;
        private final Path maskPath;
        private final Path rightWing;
        private final Paint sparklePaint;
        private final Paint wingBluePaint;
        private final Paint wingSheenPaint;
        private final Paint wingYellowPaint;

        public SunConureFlightOverlayView(Context context) {
            super(context);
            this.bodyPaint = new Paint(1);
            this.wingYellowPaint = new Paint(1);
            this.wingSheenPaint = new Paint(1);
            this.maskPaint = new Paint(1);
            this.wingBluePaint = new Paint(1);
            this.beakPaint = new Paint(1);
            this.sparklePaint = new Paint(1);
            this.bodyPath = new Path();
            this.maskPath = new Path();
            this.leftWing = new Path();
            this.rightWing = new Path();
            this.beakPath = new Path();
            this.flightProgress = 0.0f;
            this.isFlying = false;
            initPaints();
            setVisibility(8);
        }

        private float dpf(float f) {
            return f * getResources().getDisplayMetrics().density;
        }

        private void initPaints() {
            this.bodyPaint.setColor(-5632);
            this.bodyPaint.setStyle(Paint.Style.FILL);
            this.wingYellowPaint.setColor(-10752);
            this.wingYellowPaint.setStyle(Paint.Style.FILL);
            this.wingSheenPaint.setColor(-2659);
            this.wingSheenPaint.setStyle(Paint.Style.STROKE);
            this.wingSheenPaint.setStrokeWidth(dpf(1.5f));
            this.maskPaint.setColor(-28416);
            this.maskPaint.setStyle(Paint.Style.FILL);
            this.wingBluePaint.setColor(-14059009);
            this.wingBluePaint.setStyle(Paint.Style.STROKE);
            this.wingBluePaint.setStrokeWidth(dpf(1.2f));
            this.beakPaint.setColor(-14606047);
            this.beakPaint.setStyle(Paint.Style.FILL);
            this.sparklePaint.setStyle(Paint.Style.FILL);
        }

        public void triggerFlight() {
            if (this.flightAnimator != null && this.flightAnimator.isRunning()) {
                this.flightAnimator.cancel();
            }
            setVisibility(0);
            this.isFlying = true;
            this.flightProgress = 0.0f;
            this.flightAnimator = ValueAnimator.ofFloat(0.0f, 1.0f);
            this.flightAnimator.setDuration(2200L);
            this.flightAnimator.setInterpolator(new LinearInterpolator());
            this.flightAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: au.com.dss.gatehouse.MainActivity.SunConureFlightOverlayView.1
                @Override // android.animation.ValueAnimator.AnimatorUpdateListener
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    SunConureFlightOverlayView.this.flightProgress = ((Float) valueAnimator.getAnimatedValue()).floatValue();
                    SunConureFlightOverlayView.this.invalidate();
                }
            });
            this.flightAnimator.addListener(new AnimatorListenerAdapter() { // from class: au.com.dss.gatehouse.MainActivity.SunConureFlightOverlayView.2
                @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
                public void onAnimationEnd(Animator animator) {
                    SunConureFlightOverlayView.this.isFlying = false;
                    SunConureFlightOverlayView.this.setVisibility(8);
                }
            });
            this.flightAnimator.start();
        }

        @Override // android.view.View
        public boolean onTouchEvent(MotionEvent motionEvent) {
            return false;
        }

        @Override // android.view.View
        protected void onDraw(Canvas canvas) {
            float f;
            float f2;
            float f3;
            super.onDraw(canvas);
            if (!this.isFlying || this.flightProgress <= 0.001f || this.flightProgress >= 0.999f) {
                return;
            }
            float width = getWidth();
            float height = getHeight();
            float f4 = 0.0f;
            if (width <= 0.0f || height <= 0.0f) {
                return;
            }
            float f5 = getResources().getDisplayMetrics().density;
            float f6 = this.flightProgress;
            float f7 = (120.0f * f5) + width;
            float f8 = (-140.0f) * f5;
            float f9 = 1.0f - f6;
            float f10 = f9 * f9;
            float f11 = f9 * 2.0f * f6;
            float f12 = width * 0.48f;
            float f13 = f6 * f6;
            float f14 = (f10 * f7) + (f11 * f12) + (f13 * f8);
            float f15 = 0.3f * height;
            float f16 = 0.65f * height;
            float f17 = height * 0.22f;
            float f18 = (f10 * f15) + (f11 * f16) + (f13 * f17);
            float f19 = f6 * 2.0f;
            float u = 1.0f - f19;
            float vx = 2.0f * u * (f12 - f7) + 2.0f * f19 * (f8 - f12);
            float vy = 2.0f * u * (f16 - f15) + 2.0f * f19 * (f17 - f16);
            float degrees = (float) Math.toDegrees(Math.atan2(vy, vx));
            float sin = (((float) Math.sin(f6 * 44.0d)) * 0.45f) + 0.55f;
            int i = 1;
            while (i <= 5) {
                float f20 = i;
                float max = Math.max(f4, f6 - (0.022f * f20));
                if (max <= f4 || max >= 1.0f) {
                    f = f12;
                    f2 = f17;
                    f3 = f6;
                } else {
                    float f21 = 1.0f - max;
                    float f22 = f21 * f21;
                    float f23 = f21 * 2.0f * max;
                    float f24 = max * max;
                    float f25 = (f22 * f7) + (f23 * f12) + (f24 * f8);
                    float f26 = (f22 * f15) + (f23 * f16) + (f24 * f17);
                    f = f12;
                    f2 = f17;
                    float sin2 = f26 + (((float) Math.sin(i * 3.7d)) * dpf(8.0f));
                    f3 = f6;
                    this.sparklePaint.setColor(i % 2 == 0 ? -10496 : -28416);
                    this.sparklePaint.setAlpha((int) ((1.0f - (0.18f * f20)) * 180.0f));
                    canvas.drawCircle(f25, sin2, dpf(2.5f - (f20 * 0.35f)), this.sparklePaint);
                }
                i++;
                f12 = f;
                f17 = f2;
                f6 = f3;
                f4 = 0.0f;
            }
            canvas.save();
            canvas.translate(f14, f18);
            canvas.rotate(degrees);
            float f27 = f5 * 1.4f;
            canvas.scale(f27, f27);
            this.bodyPath.reset();
            this.bodyPath.moveTo(18.0f, 0.0f);
            this.bodyPath.lineTo(6.0f, 4.0f);
            this.bodyPath.lineTo(-12.0f, 3.0f);
            this.bodyPath.lineTo(-24.0f, 1.5f);
            this.bodyPath.lineTo(-24.0f, -1.5f);
            this.bodyPath.lineTo(-12.0f, -3.0f);
            this.bodyPath.lineTo(6.0f, -4.0f);
            this.bodyPath.close();
            this.maskPath.reset();
            this.maskPath.moveTo(16.0f, 0.0f);
            this.maskPath.lineTo(7.0f, 3.5f);
            this.maskPath.lineTo(1.0f, 2.5f);
            this.maskPath.lineTo(1.0f, -2.5f);
            this.maskPath.lineTo(7.0f, -3.5f);
            this.maskPath.close();
            float f28 = sin * 22.0f;
            this.leftWing.reset();
            this.leftWing.moveTo(4.0f, -2.0f);
            float f29 = -f28;
            this.leftWing.lineTo(-6.0f, f29);
            this.leftWing.lineTo(-14.0f, f29 * 0.85f);
            this.leftWing.lineTo(-10.0f, -2.0f);
            this.leftWing.close();
            this.rightWing.reset();
            this.rightWing.moveTo(4.0f, 2.0f);
            this.rightWing.lineTo(-6.0f, f28);
            this.rightWing.lineTo(-14.0f, f28 * 0.85f);
            this.rightWing.lineTo(-10.0f, 2.0f);
            this.rightWing.close();
            this.beakPath.reset();
            this.beakPath.moveTo(18.0f, 0.0f);
            this.beakPath.lineTo(8.0f, 3.5f);
            this.beakPath.lineTo(8.0f, -3.5f);
            this.beakPath.close();
            canvas.drawPath(this.leftWing, this.wingYellowPaint);
            canvas.drawPath(this.rightWing, this.wingYellowPaint);
            canvas.drawPath(this.leftWing, this.wingSheenPaint);
            canvas.drawPath(this.rightWing, this.wingSheenPaint);
            canvas.drawPath(this.leftWing, this.wingBluePaint);
            canvas.drawPath(this.rightWing, this.wingBluePaint);
            canvas.drawPath(this.bodyPath, this.bodyPaint);
            canvas.drawPath(this.maskPath, this.maskPaint);
            canvas.drawPath(this.beakPath, this.beakPaint);
            canvas.restore();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class FluidAnimatedThemeBarView extends View {
        private final Paint borderPaint;
        private final RectF glowRect;
        private float indicatorPos;
        private boolean isDragging;
        private boolean isTouching;
        private int lastHoverIndex;
        private final Paint pillGlowPaint;
        private final Paint pillPaint;
        private final RectF pillRect;
        public int selectedIndex;
        private ValueAnimator snapAnimator;
        private final Paint textPaint;
        private final int[] themeAccents;
        private final String[] titles;
        private float touchDownX;
        private float touchDownY;
        private float touchScale;
        private ValueAnimator touchScaleAnimator;
        private final Paint trackPaint;
        private final RectF trackRect;

        public FluidAnimatedThemeBarView(Context context) {
            super(context);
            this.titles = new String[]{"🟡 Gold", "🔴 0-Lux", "🟢 NVG", "🟣 Cyber"};
            this.themeAccents = new int[]{-11930, -50384, -16711834, -4487428};
            this.trackPaint = new Paint(1);
            this.borderPaint = new Paint(1);
            this.pillPaint = new Paint(1);
            this.pillGlowPaint = new Paint(1);
            this.textPaint = new Paint(1);
            this.trackRect = new RectF();
            this.pillRect = new RectF();
            this.glowRect = new RectF();
            this.indicatorPos = 0.0f;
            this.selectedIndex = 0;
            this.lastHoverIndex = 0;
            this.isTouching = false;
            this.touchDownX = 0.0f;
            this.touchDownY = 0.0f;
            this.isDragging = false;
            this.touchScale = 1.0f;
            this.indicatorPos = MainActivity.this.activeTheme;
            this.selectedIndex = MainActivity.this.activeTheme;
            this.lastHoverIndex = MainActivity.this.activeTheme;
            initPaints();
        }

        private float dpf(float f) {
            return f * getResources().getDisplayMetrics().density;
        }

        private void initPaints() {
            this.trackPaint.setStyle(Paint.Style.FILL);
            this.borderPaint.setStyle(Paint.Style.STROKE);
            this.borderPaint.setStrokeWidth(dpf(1.0f));
            this.pillPaint.setStyle(Paint.Style.FILL);
            this.pillGlowPaint.setStyle(Paint.Style.FILL);
            this.textPaint.setTextAlign(Paint.Align.CENTER);
            this.textPaint.setTypeface(Typeface.DEFAULT_BOLD);
        }

        private int getThemeColorForPos(float f) {
            int floor = (int) Math.floor(f);
            if (floor < 0) {
                return this.themeAccents[0];
            }
            if (floor >= this.themeAccents.length - 1) {
                return this.themeAccents[this.themeAccents.length - 1];
            }
            return blendColors(this.themeAccents[floor], this.themeAccents[floor + 1], f - floor);
        }

        public void setSelectedTheme(int i, boolean z) {
            if (i < 0 || i >= this.titles.length) {
                return;
            }
            this.selectedIndex = i;
            this.lastHoverIndex = i;
            if (z) {
                animateToPosition(i);
                return;
            }
            if (this.snapAnimator != null && this.snapAnimator.isRunning()) {
                this.snapAnimator.cancel();
            }
            this.indicatorPos = i;
            invalidate();
        }

        private void animateToPosition(float f) {
            if (this.snapAnimator != null && this.snapAnimator.isRunning()) {
                this.snapAnimator.cancel();
            }
            this.snapAnimator = ValueAnimator.ofFloat(this.indicatorPos, f);
            this.snapAnimator.setDuration(200L);
            this.snapAnimator.setInterpolator(new DecelerateInterpolator(1.8f));
            this.snapAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: au.com.dss.gatehouse.MainActivity.FluidAnimatedThemeBarView.1
                @Override // android.animation.ValueAnimator.AnimatorUpdateListener
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    FluidAnimatedThemeBarView.this.indicatorPos = ((Float) valueAnimator.getAnimatedValue()).floatValue();
                    FluidAnimatedThemeBarView.this.invalidate();
                }
            });
            this.snapAnimator.start();
        }

        private void animateTouchScale(float f) {
            if (this.touchScaleAnimator != null && this.touchScaleAnimator.isRunning()) {
                this.touchScaleAnimator.cancel();
            }
            this.touchScaleAnimator = ValueAnimator.ofFloat(this.touchScale, f);
            this.touchScaleAnimator.setDuration(120L);
            this.touchScaleAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: au.com.dss.gatehouse.MainActivity.FluidAnimatedThemeBarView.2
                @Override // android.animation.ValueAnimator.AnimatorUpdateListener
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    FluidAnimatedThemeBarView.this.touchScale = ((Float) valueAnimator.getAnimatedValue()).floatValue();
                    FluidAnimatedThemeBarView.this.invalidate();
                }
            });
            this.touchScaleAnimator.start();
        }

        @Override // android.view.View
        public boolean onTouchEvent(MotionEvent motionEvent) {
            int width = getWidth();
            if (width <= 0) {
                return super.onTouchEvent(motionEvent);
            }
            float dpf = dpf(3.0f);
            float length = (width - (dpf * 2.0f)) / this.titles.length;
            switch (motionEvent.getActionMasked()) {
                case 0:
                    this.isTouching = true;
                    this.isDragging = false;
                    this.touchDownX = motionEvent.getX();
                    this.touchDownY = motionEvent.getY();
                    getParent().requestDisallowInterceptTouchEvent(true);
                    float max = Math.max(0.0f, Math.min(this.titles.length - 1, ((motionEvent.getX() - dpf) - (length / 2.0f)) / length));
                    this.lastHoverIndex = Math.round(max);
                    animateToPosition(max);
                    animateTouchScale(1.04f);
                    MainActivity.this.hapticClick();
                    return true;
                case 1:
                    this.isTouching = false;
                    this.isDragging = false;
                    getParent().requestDisallowInterceptTouchEvent(false);
                    int max2 = Math.max(0, Math.min(this.titles.length - 1, Math.round(Math.max(0.0f, Math.min(this.titles.length - 1, ((motionEvent.getX() - dpf) - (length / 2.0f)) / length)))));
                    this.selectedIndex = max2;
                    animateToPosition(max2);
                    animateTouchScale(1.0f);
                    MainActivity.this.hapticClick();
                    if (MainActivity.this.activeTheme != max2) {
                        MainActivity.this.activeTheme = max2;
                        MainActivity.this.rebuildCurrentScreen();
                    }
                    return true;
                case 2:
                    float abs = Math.abs(motionEvent.getX() - this.touchDownX);
                    float abs2 = Math.abs(motionEvent.getY() - this.touchDownY);
                    if (abs > dpf(6.0f) || abs2 > dpf(6.0f)) {
                        this.isDragging = true;
                    }
                    float max3 = Math.max(0.0f, Math.min(this.titles.length - 1, ((motionEvent.getX() - dpf) - (length / 2.0f)) / length));
                    if (this.snapAnimator != null && this.snapAnimator.isRunning()) {
                        this.snapAnimator.cancel();
                    }
                    this.indicatorPos = max3;
                    int max4 = Math.max(0, Math.min(this.titles.length - 1, Math.round(max3)));
                    if (max4 != this.lastHoverIndex) {
                        this.lastHoverIndex = max4;
                        MainActivity.this.hapticClick();
                    }
                    invalidate();
                    return true;
                case 3:
                    this.isTouching = false;
                    this.isDragging = false;
                    getParent().requestDisallowInterceptTouchEvent(false);
                    animateToPosition(MainActivity.this.activeTheme);
                    animateTouchScale(1.0f);
                    return true;
                default:
                    return super.onTouchEvent(motionEvent);
            }
        }

        @Override // android.view.View
        protected void onMeasure(int i, int i2) {
            setMeasuredDimension(View.MeasureSpec.getSize(i), (int) dpf(38.0f));
        }

        @Override // android.view.View
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            int width = getWidth();
            int height = getHeight();
            if (width <= 0 || height <= 0) {
                return;
            }
            float dpf = dpf(3.0f);
            float dpf2 = dpf(14.0f);
            float dpf3 = dpf(11.0f);
            float f = width;
            float f2 = height;
            this.trackRect.set(0.0f, 0.0f, f, f2);
            this.trackPaint.setColor(MainActivity.this.colPanel);
            canvas.drawRoundRect(this.trackRect, dpf2, dpf2, this.trackPaint);
            this.borderPaint.setColor(MainActivity.this.colLine);
            canvas.drawRoundRect(this.trackRect, dpf2, dpf2, this.borderPaint);
            float length = (f - (dpf * 2.0f)) / this.titles.length;
            float f3 = (this.indicatorPos * length) + dpf;
            float f4 = f3 + length;
            float f5 = f2 - dpf;
            if (this.touchScale > 1.001f) {
                float f6 = (f3 + f4) / 2.0f;
                float f7 = (dpf + f5) / 2.0f;
                float f8 = (length / 2.0f) * this.touchScale;
                float f9 = ((f5 - dpf) / 2.0f) * this.touchScale;
                this.pillRect.set(f6 - f8, f7 - f9, f6 + f8, f7 + f9);
            } else {
                this.pillRect.set(f3, dpf, f4, f5);
            }
            int themeColorForPos = getThemeColorForPos(this.indicatorPos);
            this.pillGlowPaint.setColor(themeColorForPos);
            this.pillGlowPaint.setAlpha(this.isTouching ? 70 : 40);
            this.glowRect.set(this.pillRect.left - dpf(2.0f), this.pillRect.top - dpf(1.0f), this.pillRect.right + dpf(2.0f), this.pillRect.bottom + dpf(1.0f));
            canvas.drawRoundRect(this.glowRect, dpf(1.0f) + dpf3, dpf(1.0f) + dpf3, this.pillGlowPaint);
            this.pillPaint.setColor(themeColorForPos);
            canvas.drawRoundRect(this.pillRect, dpf3, dpf3, this.pillPaint);
            float dpf4 = (f2 / 2.0f) + dpf(3.5f);
            for (int i = 0; i < this.titles.length; i++) {
                float f10 = i;
                float f11 = ((f10 + 0.5f) * length) + dpf;
                float abs = Math.abs(this.indicatorPos - f10);
                if (abs < 0.5f) {
                    float f12 = abs / 0.5f;
                    this.textPaint.setColor(blendColors(MainActivity.this.colAccentInk, MainActivity.this.colPale, f12));
                    this.textPaint.setTextSize(dpf(10.2f - (f12 * 0.7f)));
                } else if (abs < 1.0f) {
                    this.textPaint.setColor(blendColors(MainActivity.this.colPale, MainActivity.this.colMuted, (abs - 0.5f) / 0.5f));
                    this.textPaint.setTextSize(dpf(9.5f));
                } else {
                    this.textPaint.setColor(MainActivity.this.colMuted);
                    this.textPaint.setTextSize(dpf(9.5f));
                }
                canvas.drawText(this.titles[i], f11, dpf4, this.textPaint);
            }
        }

        private int blendColors(int i, int i2, float f) {
            float f2 = 1.0f - f;
            return Color.argb((int) ((Color.alpha(i) * f2) + (Color.alpha(i2) * f)), (int) ((Color.red(i) * f2) + (Color.red(i2) * f)), (int) ((Color.green(i) * f2) + (Color.green(i2) * f)), (int) ((Color.blue(i) * f2) + (Color.blue(i2) * f)));
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class FluidAnimatedTabBarView extends View {
        private final Paint borderPaint;
        private final RectF glowRect;
        private float indicatorPos;
        private boolean isDragging;
        private boolean isTouching;
        private int lastHoverIndex;
        private final Paint pillGlowPaint;
        private final Paint pillPaint;
        private final RectF pillRect;
        public int selectedIndex;
        private ValueAnimator snapAnimator;
        private final Paint textPaint;
        private final String[] titles;
        private float touchDownX;
        private float touchDownY;
        private float touchScale;
        private ValueAnimator touchScaleAnimator;
        private final Paint trackPaint;
        private final RectF trackRect;

        public FluidAnimatedTabBarView(Context context) {
            super(context);
            this.titles = new String[]{"🛡️ Patrol", "📞 Contacts", "📖 Guide", "🛠️ Tools"};
            this.trackPaint = new Paint(1);
            this.borderPaint = new Paint(1);
            this.pillPaint = new Paint(1);
            this.pillGlowPaint = new Paint(1);
            this.textPaint = new Paint(1);
            this.trackRect = new RectF();
            this.pillRect = new RectF();
            this.glowRect = new RectF();
            this.indicatorPos = 0.0f;
            this.selectedIndex = 0;
            this.lastHoverIndex = 0;
            this.isTouching = false;
            this.touchDownX = 0.0f;
            this.touchDownY = 0.0f;
            this.isDragging = false;
            this.touchScale = 1.0f;
            initPaints();
        }

        private float dpf(float f) {
            return f * getResources().getDisplayMetrics().density;
        }

        private void initPaints() {
            this.trackPaint.setStyle(Paint.Style.FILL);
            this.borderPaint.setStyle(Paint.Style.STROKE);
            this.borderPaint.setStrokeWidth(dpf(1.0f));
            this.pillPaint.setStyle(Paint.Style.FILL);
            this.pillGlowPaint.setStyle(Paint.Style.FILL);
            this.textPaint.setTextAlign(Paint.Align.CENTER);
            this.textPaint.setTypeface(Typeface.DEFAULT_BOLD);
        }

        public void setSelectedTab(int i, boolean z) {
            if (i < 0 || i >= this.titles.length) {
                return;
            }
            this.selectedIndex = i;
            this.lastHoverIndex = i;
            if (z) {
                animateToPosition(i);
                return;
            }
            if (this.snapAnimator != null && this.snapAnimator.isRunning()) {
                this.snapAnimator.cancel();
            }
            this.indicatorPos = i;
            invalidate();
        }

        private void animateToPosition(float f) {
            if (this.snapAnimator != null && this.snapAnimator.isRunning()) {
                this.snapAnimator.cancel();
            }
            this.snapAnimator = ValueAnimator.ofFloat(this.indicatorPos, f);
            this.snapAnimator.setDuration(200L);
            this.snapAnimator.setInterpolator(new DecelerateInterpolator(1.8f));
            this.snapAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: au.com.dss.gatehouse.MainActivity.FluidAnimatedTabBarView.1
                @Override // android.animation.ValueAnimator.AnimatorUpdateListener
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    FluidAnimatedTabBarView.this.indicatorPos = ((Float) valueAnimator.getAnimatedValue()).floatValue();
                    FluidAnimatedTabBarView.this.invalidate();
                }
            });
            this.snapAnimator.start();
        }

        private void animateTouchScale(float f) {
            if (this.touchScaleAnimator != null && this.touchScaleAnimator.isRunning()) {
                this.touchScaleAnimator.cancel();
            }
            this.touchScaleAnimator = ValueAnimator.ofFloat(this.touchScale, f);
            this.touchScaleAnimator.setDuration(120L);
            this.touchScaleAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: au.com.dss.gatehouse.MainActivity.FluidAnimatedTabBarView.2
                @Override // android.animation.ValueAnimator.AnimatorUpdateListener
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    FluidAnimatedTabBarView.this.touchScale = ((Float) valueAnimator.getAnimatedValue()).floatValue();
                    FluidAnimatedTabBarView.this.invalidate();
                }
            });
            this.touchScaleAnimator.start();
        }

        @Override // android.view.View
        public boolean onTouchEvent(MotionEvent motionEvent) {
            int width = getWidth();
            if (width <= 0) {
                return super.onTouchEvent(motionEvent);
            }
            float dpf = dpf(3.0f);
            float length = (width - (dpf * 2.0f)) / this.titles.length;
            switch (motionEvent.getActionMasked()) {
                case 0:
                    this.isTouching = true;
                    this.isDragging = false;
                    this.touchDownX = motionEvent.getX();
                    this.touchDownY = motionEvent.getY();
                    getParent().requestDisallowInterceptTouchEvent(true);
                    float max = Math.max(0.0f, Math.min(this.titles.length - 1, ((motionEvent.getX() - dpf) - (length / 2.0f)) / length));
                    this.lastHoverIndex = Math.round(max);
                    animateToPosition(max);
                    animateTouchScale(1.04f);
                    MainActivity.this.hapticClick();
                    return true;
                case 1:
                    this.isTouching = false;
                    this.isDragging = false;
                    getParent().requestDisallowInterceptTouchEvent(false);
                    int max2 = Math.max(0, Math.min(this.titles.length - 1, Math.round(Math.max(0.0f, Math.min(this.titles.length - 1, ((motionEvent.getX() - dpf) - (length / 2.0f)) / length)))));
                    this.selectedIndex = max2;
                    animateToPosition(max2);
                    animateTouchScale(1.0f);
                    MainActivity.this.hapticClick();
                    MainActivity.this.updateTabSelection(max2);
                    return true;
                case 2:
                    float abs = Math.abs(motionEvent.getX() - this.touchDownX);
                    float abs2 = Math.abs(motionEvent.getY() - this.touchDownY);
                    if (abs > dpf(6.0f) || abs2 > dpf(6.0f)) {
                        this.isDragging = true;
                    }
                    float max3 = Math.max(0.0f, Math.min(this.titles.length - 1, ((motionEvent.getX() - dpf) - (length / 2.0f)) / length));
                    if (this.snapAnimator != null && this.snapAnimator.isRunning()) {
                        this.snapAnimator.cancel();
                    }
                    this.indicatorPos = max3;
                    int max4 = Math.max(0, Math.min(this.titles.length - 1, Math.round(max3)));
                    if (max4 != this.lastHoverIndex) {
                        this.lastHoverIndex = max4;
                        MainActivity.this.hapticClick();
                    }
                    invalidate();
                    return true;
                case 3:
                    this.isTouching = false;
                    this.isDragging = false;
                    getParent().requestDisallowInterceptTouchEvent(false);
                    animateToPosition(this.selectedIndex);
                    animateTouchScale(1.0f);
                    return true;
                default:
                    return super.onTouchEvent(motionEvent);
            }
        }

        @Override // android.view.View
        protected void onMeasure(int i, int i2) {
            setMeasuredDimension(View.MeasureSpec.getSize(i), (int) dpf(42.0f));
        }

        @Override // android.view.View
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            int width = getWidth();
            int height = getHeight();
            if (width <= 0 || height <= 0) {
                return;
            }
            float dpf = dpf(3.0f);
            float dpf2 = dpf(14.0f);
            float dpf3 = dpf(11.0f);
            float f = width;
            float f2 = height;
            this.trackRect.set(0.0f, 0.0f, f, f2);
            this.trackPaint.setColor(MainActivity.this.colPanel);
            canvas.drawRoundRect(this.trackRect, dpf2, dpf2, this.trackPaint);
            this.borderPaint.setColor(MainActivity.this.colLine);
            canvas.drawRoundRect(this.trackRect, dpf2, dpf2, this.borderPaint);
            float length = (f - (dpf * 2.0f)) / this.titles.length;
            float f3 = (this.indicatorPos * length) + dpf;
            float f4 = f3 + length;
            float f5 = f2 - dpf;
            if (this.touchScale > 1.001f) {
                float f6 = (f3 + f4) / 2.0f;
                float f7 = (dpf + f5) / 2.0f;
                float f8 = (length / 2.0f) * this.touchScale;
                float f9 = ((f5 - dpf) / 2.0f) * this.touchScale;
                this.pillRect.set(f6 - f8, f7 - f9, f6 + f8, f7 + f9);
            } else {
                this.pillRect.set(f3, dpf, f4, f5);
            }
            this.pillGlowPaint.setColor(MainActivity.this.colAccent);
            this.pillGlowPaint.setAlpha(this.isTouching ? 65 : 35);
            this.glowRect.set(this.pillRect.left - dpf(2.0f), this.pillRect.top - dpf(1.0f), this.pillRect.right + dpf(2.0f), this.pillRect.bottom + dpf(1.0f));
            canvas.drawRoundRect(this.glowRect, dpf(1.0f) + dpf3, dpf(1.0f) + dpf3, this.pillGlowPaint);
            this.pillPaint.setColor(MainActivity.this.colAccent);
            canvas.drawRoundRect(this.pillRect, dpf3, dpf3, this.pillPaint);
            float dpf4 = (f2 / 2.0f) + dpf(4.0f);
            for (int i = 0; i < this.titles.length; i++) {
                float f10 = i;
                float f11 = ((f10 + 0.5f) * length) + dpf;
                float abs = Math.abs(this.indicatorPos - f10);
                if (abs < 0.5f) {
                    float f12 = abs / 0.5f;
                    this.textPaint.setColor(blendColors(MainActivity.this.colAccentInk, MainActivity.this.colPale, f12));
                    this.textPaint.setTextSize(dpf(11.2f - (f12 * 0.7f)));
                } else if (abs < 1.0f) {
                    this.textPaint.setColor(blendColors(MainActivity.this.colPale, MainActivity.this.colMuted, (abs - 0.5f) / 0.5f));
                    this.textPaint.setTextSize(dpf(10.5f));
                } else {
                    this.textPaint.setColor(MainActivity.this.colMuted);
                    this.textPaint.setTextSize(dpf(10.5f));
                }
                canvas.drawText(this.titles[i], f11, dpf4, this.textPaint);
            }
        }

        private int blendColors(int i, int i2, float f) {
            float f2 = 1.0f - f;
            return Color.argb((int) ((Color.alpha(i) * f2) + (Color.alpha(i2) * f)), (int) ((Color.red(i) * f2) + (Color.red(i2) * f)), (int) ((Color.green(i) * f2) + (Color.green(i2) * f)), (int) ((Color.blue(i) * f2) + (Color.blue(i2) * f)));
        }
    }
}
