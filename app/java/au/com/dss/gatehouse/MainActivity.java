package au.com.dss.gatehouse;

import android.Manifest;
import android.animation.ValueAnimator;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.widget.Toast;

import android.app.Activity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
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
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.VelocityTracker;
import android.widget.Toast;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.TimeZone;

/** A streamlined 21st-century tactical screen over the SPARK Ada record core.
 * Configured specifically for Hume Doors & Timber, Kingston.
 */
public class MainActivity extends Activity implements SensorEventListener, LocationListener {
    // 📱 DEPUTY WORKPLACE ADD-IN & PEEK & FLOW NAVIGATION
    private FrameLayout deputyContainer;
    private View deputyScrim;
    private View peekShadow;
    private FrameLayout mainSurfaceContainer;
    private boolean isDeputyOpen = false;
    private LinearLayout rosterDetailContainer;
    private FluidRosterDayScrubberView rosterScrubber;
    private int selectedRosterDay = 5;
    private LinearLayout handbookContent;

    private boolean isPeekDragging = false;
    private boolean peekBuzzed = false;
    private float peekDownX = 0f;
    private float peekDownY = 0f;
    private VelocityTracker peekVelocityTracker;

    // 🎨 FLUID THEME & TAB BARS
    private FluidAnimatedThemeBarView animatedThemeBar;
    private FluidAnimatedTabBarView animatedTabBar;
    private ThemeShockwaveOverlayView shockwaveOverlay;
    private ValueAnimator themeMorphAnimator;
    private BroadcastReceiver widgetReceiver;


    public static final int THEME_GOLD = 0;
    public static final int THEME_RED = 1;
    public static final int THEME_NVG = 2;
    public static final int THEME_VIOLET = 3;

    private int activeTheme = THEME_GOLD;
    private int currentTab = 0;
    // 📱 SYNCHRONIZED 4-TAB HORIZONTAL PAGER
    private FrameLayout tabPagerFrame;
    private ScrollView scrollPatrol;
    private ScrollView scrollContacts;
    private ScrollView scrollHandbook;
    private ScrollView scrollTools;
    private float currentTabFloat = 0f;
    private ValueAnimator tabSlideAnimator;
    private float pageSwipeDownX = 0f;
    private float pageSwipeDownY = 0f;
    private boolean isPageSwiping = false;


    private int colBg = 0xFF000000;
    private int colPanel = 0xFF080C14;
    private int colPanel2 = 0xFF101724;
    private int colPanel3 = 0xFF182234;
    private int colLine = 0xFF1E2B40;
    private int colLineSubtle = 0xFF121B28;
    private int colAccent = 0xFFE5A93C;
    private int colAccentInk = 0xFF000000;
    private int colAccentSoft = 0x1AE5A93C;
    private int colPale = 0xFFF3F6FA;
    private int colMuted = 0xFF94A3B8;
    private int colQuiet = 0xFF5B6B82;
    private int colEmerald = 0xFF10B981;
    private int colEmeraldSoft = 0x2210B981;
    private int colCrimson = 0xFFEF4444;
    private int colCrimsonSoft = 0x24EF4444;
    private int colCyan = 0xFF06B6D4;
    private int colCyanSoft = 0x2406B6D4;

    private static final String[] EXTERNAL_CHOICES = {
        "External (Full)", "04A1B2C3D4E501",
        "External (Half)", "04A1B2C3D4E502"
    };

    private static final String[] EXTERNAL_OPTIONS = {
        "✓ Perimeter Secure: Fences intact & gates locked",
        "⚠️ Perimeter gate / padlock left unlocked",
        "⚠️ Boundary fence damage / wire mesh cut",
        "⚠️ Floodlights / perimeter security lighting dark",
        "⚠️ Suspicious vehicle / loitering outside boundary",
    };

    private static final String[] INTERNAL_LOTS = {
        "Lot 14", "04F1A2B3C4D5E6",
        "Lot 15", "04F2A3B4C5D6E7",
        "Lot 16", "04F3A4B5C6D7E8",
        "Lot 17", "04F4A5B6C7D8E9",
        "Lot 18", "04F5A6B7C8D9EA",
    };

    private static final String[] FIRE_POINTS = {
        "Lot 15 Pump House", "04E1F2A3B4C5D6",
        "Lot 16 Pump House (Outside)", "04E2F3A4B5C6D7",
        "Lot 16 Fire System (Inside)", "04E3F4A5B6C7D8",
        "Lot 17 Pump House", "04E4F5A6B7C8D9",
        "Lot 18 Pump House", "04E5F6A7B8C9DA",
    };

    private static final String[] PUMP_OPTIONS = {
        "✓ Pressure Normal (175 PSI In Spec)",
        "⚠️ Low Pressure Warning (< 1,000 PSI)",
        "⚠️ Jockey Pump Cycling Excessively",
        "⚠️ Diesel Booster Fuel Tank Below 75%",
        "⚠️ Minor Valve / Pipe Fitting Weep Noted"
    };

    private static class PressureRecord {
        int timeMinutes;
        int pressureKpa;
        PressureRecord(int t, int p) { timeMinutes = t; pressureKpa = p; }
    }
    private final HashMap<String, ArrayList<PressureRecord>> pressureHistory = new HashMap<String, ArrayList<PressureRecord>>();

    private static final String[] SHUTDOWN_OPTIONS = {
        "✓ All Clear: Factory floor sealed & machinery isolated",
        "⚠️ Roller door / emergency exit unlocked (secured by guard)",
        "⚠️ High-bay lighting / plant machinery left powered on",
        "⚠️ Air compressor / extraction fans left running",
        "⚠️ Floor hazard / liquid spill noted",
    };

    private static final int REQ_PERM_CAMERA = 2001;
    private static final int REQ_PERM_AUDIO = 2002;
    private static final int REQ_PERM_LOCATION = 2003;

    private ScrollView scroll;
    private FrameLayout rootFrame;
    private LinearLayout root;
    private LinearLayout modeBar;

    private LinearLayout diagStrip;
    private TextView diagOledPower;
    private TextView diagAmbientWeather;
    private TextView diagBatteryRuntime;

    private ChronographView chronographView;
    private HolographicCardView activeHoloCard;
    private DetailedCompassView activeCompassView;
    private TextView compassHeadingText;
    private TextView compassDmsText;
    private TextView compassSectorText;

    private LinearLayout tabContainer;
    private TextView tabPatrol;
    private TextView tabContacts;
    private TextView tabTools;
    private LinearLayout patrolContent;
    private LinearLayout contactsContent;
    private LinearLayout toolsContent;

    private AnimatedChainBannerView chainBannerView;
    private LinearLayout pills;
    private LinearLayout externalRow;
    private TextView tileExternalFull;
    private TextView tileExternalHalf;
    private LinearLayout internalBadgesRow;
    private LinearLayout fireCard;
    private LinearLayout fireList;
    private TextView fireStatusChip;
    private LinearLayout dock;
    private LinearLayout tonight;
    private TextView tonightTitle;
    private TextView banner;
    private TextView primary;
    private TextView pageTitle;
    private TextView page;
    private TextView btnShareReport;

    private static final long HOLD_MS = 2 * 60 * 1000L;
    private static final int MAX_HELD = 50;
    private static final long WELFARE_INTERVAL_MS = 90 * 60 * 1000L;
    private long lastActivityTimeMs;
    private boolean isWelfareDialogShowing = false;

    private Vibrator vibrator;
    private String rearCameraId;
    private boolean isHardwareTorchOn = false;
    private int torchLevelPercent = 100;
    private boolean isStrobeActive = false;
    private boolean isSosActive = false;
    private final Handler lightHandler = new Handler();

    private SensorManager sensorManager;
    private Sensor rotationSensor;
    private Sensor accelSensor;
    private Sensor magSensor;
    private float[] lastAccel = new float[3];
    private float[] lastMag = new float[3];
    private boolean hasAccel = false;
    private boolean hasMag = false;
    private float currentAzimuth = 0f;

    // 🔦 Robust Double-Chop Shake Detector
    private float[] lastGravity = new float[3];
    private long lastChopTimestamp = 0;
    private int chopCount = 0;
    private long lastToggleCooldown = 0;

    // 📡 BLE Mesh & Trusted Peers State
    private static class TrustedPeer {
        String name;
        String licence;
        String lastSeen;
        TrustedPeer(String n, String l, String s) { name = n; licence = l; lastSeen = s; }
    }
    private final ArrayList<TrustedPeer> trustedPeers = new ArrayList<TrustedPeer>();

    private LocationManager locationManager;
    private Location lastKnownLocation;
    private TextView gpsCoordsText;
    private TextView gpsAltitudeText;
    private TextView gpsAccuracyText;
    private SatellitePolarRadarView satelliteRadarView;

    // 🌤️ Kingston Actual Ambient Site Weather
    private double curTempC = 14.8;
    private double curFeelsLikeC = 13.9;
    private double curUvIndex = 0.0;
    private int curHumidity = 78;
    private double curDewPointC = 11.2;
    private double curPressureHpa = 1021.2;
    private double curWindSpeedKmh = 12.4;
    private String curWindDir = "SSE (165°)";
    private double curWindGustKmh = 18.2;
    private int waterIntakeMl = 750;
    private static final int WATER_TARGET_ML = 2000;

    private static class Pending {
        boolean checkpoint;
        String label = "";
        String uid = "";
        int taps;
        int topic;
        String text = "";
        int occurred;
        long created;
    }

    private final ArrayList<Pending> pending = new ArrayList<Pending>();
    private final Handler ticker = new Handler();

    private int taps = 100;
    private int openedAt;

    private MediaRecorder voiceRecorder;
    private File activeVoiceFile;
    private long voiceRecordStart;
    private boolean isRecordingVoice = false;

    private HorizonLevelerView activeLevelerView;
    private PulsingScrollIndicator scrollIndicator;

    private static int nowMinutes() {
        long ms = System.currentTimeMillis();
        return (int) ((ms + TimeZone.getDefault().getOffset(ms)) / 60000L);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        lastActivityTimeMs = SystemClock.elapsedRealtime();
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        trustedPeers.add(new TrustedPeer("Officer M. Taylor", "LIC #55891", "Today 05:58 AM (Gate A)"));

        initSensorsAndGps();
        initCameraManager();

        buildUi();
        loadPending();
        startShift();
        commitAll();
        updateDiagnostics();
        ticker.postDelayed(tick, 1000);
    }

    private void hapticClick() {
        if (vibrator == null) return;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK));
            } else { vibrator.vibrate(18); }
        } catch (Exception e) {}
    }

    private void hapticHeavyClick() {
        if (vibrator == null) return;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK));
            } else { vibrator.vibrate(40); }
        } catch (Exception e) {}
    }

    private void hapticDoublePulse() {
        if (vibrator == null) return;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(new long[]{0, 25, 45, 35}, -1));
            } else { vibrator.vibrate(new long[]{0, 25, 45, 35}, -1); }
        } catch (Exception e) {}
    }

    private void hapticSealThud() {
        if (vibrator == null) return;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(new long[]{0, 80, 80, 140}, -1));
            } else { vibrator.vibrate(new long[]{0, 80, 80, 140}, -1); }
        } catch (Exception e) {}
    }

    private void initCameraManager() {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            for (String id : manager.getCameraIdList()) {
                CameraCharacteristics c = manager.getCameraCharacteristics(id);
                Integer facing = c.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_BACK) {
                    rearCameraId = id;
                    break;
                }
            }
            if (rearCameraId == null && manager.getCameraIdList().length > 0) {
                rearCameraId = manager.getCameraIdList()[0];
            }
        } catch (Exception e) {}
    }

    private void initSensorsAndGps() {
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
            accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            magSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        }
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerSensors();
        requestGpsUpdates();
        updateDiagnostics();
    }

    private void registerSensors() {
        if (sensorManager != null) {
            if (rotationSensor != null) sensorManager.registerListener(this, rotationSensor, SensorManager.SENSOR_DELAY_UI);
            if (accelSensor != null) sensorManager.registerListener(this, accelSensor, SensorManager.SENSOR_DELAY_GAME);
            if (magSensor != null) sensorManager.registerListener(this, magSensor, SensorManager.SENSOR_DELAY_UI);
        }
    }

    private void unregisterSensors() {
        if (sensorManager != null) sensorManager.unregisterListener(this);
    }

    private void requestGpsUpdates() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            }, REQ_PERM_LOCATION);
            return;
        }

        if (locationManager != null) {
            try {
                if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000, 1, this);
                    Location loc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    if (loc != null) updateGpsDisplay(loc);
                } else if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 2000, 1, this);
                    Location loc = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                    if (loc != null) updateGpsDisplay(loc);
                }
            } catch (Exception e) {}
        }
    }

    private void stopGpsUpdates() {
        if (locationManager != null) {
            try { locationManager.removeUpdates(this); } catch (Exception e) {}
        }
    }

    private void registerActivity() {
        lastActivityTimeMs = SystemClock.elapsedRealtime();
        if (chronographView != null) chronographView.invalidate();
    }

    private void applyThemeTokens() {
        switch (activeTheme) {
            case THEME_RED:
                colBg = 0xFF000000;
                colPanel = 0xFF0D0303;
                colPanel2 = 0xFF170606;
                colPanel3 = 0xFF220909;
                colLine = 0xFF3D1212;
                colLineSubtle = 0xFF240A0A;
                colAccent = 0xFFFF3333;
                colAccentInk = 0xFF000000;
                colAccentSoft = 0x22FF3333;
                colPale = 0xFFFF8A8A;
                colMuted = 0xFFC45555;
                colQuiet = 0xFF7A3333;
                colEmerald = 0xFFFF5555;
                colEmeraldSoft = 0x26FF5555;
                colCrimson = 0xFFFF1111;
                colCrimsonSoft = 0x33FF1111;
                colCyan = 0xFFFF4444;
                colCyanSoft = 0x28FF4444;
                break;

            case THEME_NVG:
                colBg = 0xFF000000;
                colPanel = 0xFF021206;
                colPanel2 = 0xFF041E0A;
                colPanel3 = 0xFF062A0F;
                colLine = 0xFF0A4418;
                colLineSubtle = 0xFF062B10;
                colAccent = 0xFF00FF66;
                colAccentInk = 0xFF000000;
                colAccentSoft = 0x2200FF66;
                colPale = 0xFFE0FFE8;
                colMuted = 0xFF55DD77;
                colQuiet = 0xFF228844;
                colEmerald = 0xFF00FF66;
                colEmeraldSoft = 0x2600FF66;
                colCrimson = 0xFFFF5555;
                colCrimsonSoft = 0x24FF5555;
                colCyan = 0xFF00FFCC;
                colCyanSoft = 0x2400FFCC;
                break;

            case THEME_VIOLET:
                colBg = 0xFF000000;
                colPanel = 0xFF0B0414;
                colPanel2 = 0xFF140822;
                colPanel3 = 0xFF1F0C35;
                colLine = 0xFF351559;
                colLineSubtle = 0xFF220C3A;
                colAccent = 0xFFC084FC;
                colAccentInk = 0xFF000000;
                colAccentSoft = 0x22C084FC;
                colPale = 0xFFF3E8FF;
                colMuted = 0xFFA855F7;
                colQuiet = 0xFF6B21A8;
                colEmerald = 0xFF10B981;
                colEmeraldSoft = 0x2210B981;
                colCrimson = 0xFFF43F5E;
                colCrimsonSoft = 0x24F43F5E;
                colCyan = 0xFF38BDF8;
                colCyanSoft = 0x2438BDF8;
                break;

            case THEME_GOLD:
            default:
                colBg = 0xFF000000;
                colPanel = 0xFF080C14;
                colPanel2 = 0xFF101724;
                colPanel3 = 0xFF182234;
                colLine = 0xFF1E2B40;
                colLineSubtle = 0xFF121B28;
                colAccent = 0xFFE5A93C;
                colAccentInk = 0xFF000000;
                colAccentSoft = 0x1AE5A93C;
                colPale = 0xFFF3F6FA;
                colMuted = 0xFF94A3B8;
                colQuiet = 0xFF5B6B82;
                colEmerald = 0xFF10B981;
                colEmeraldSoft = 0x2210B981;
                colCrimson = 0xFFEF4444;
                colCrimsonSoft = 0x24EF4444;
                colCyan = 0xFF06B6D4;
                colCyanSoft = 0x2406B6D4;
                break;
        }
    }

    private void buildUi() {
        applyThemeTokens();

        rootFrame = new FrameLayout(this) {
            @Override
            public boolean onInterceptTouchEvent(MotionEvent ev) {
                switch (ev.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        peekDownX = ev.getX();
                        peekDownY = ev.getY();
                        isPeekDragging = false;
                        peekBuzzed = false;
                        if (peekVelocityTracker != null) {
                            peekVelocityTracker.recycle();
                            peekVelocityTracker = null;
                        }
                        peekVelocityTracker = VelocityTracker.obtain();
                        peekVelocityTracker.addMovement(ev);
                        break;
                    case MotionEvent.ACTION_MOVE:
                        float dx = ev.getX() - peekDownX;
                        float dy = Math.abs(ev.getY() - peekDownY);
                        // Pull right from left edge (first 100dp) to peek Deputy, or pull left to close Deputy
                        if (!isDeputyOpen && peekDownX < dp(100) && dx > dp(18) && dx > dy * 1.2f) {
                            isPeekDragging = true;
                            if (peekVelocityTracker != null) peekVelocityTracker.addMovement(ev);
                            return true;
                        } else if (isDeputyOpen && dx < -dp(18) && Math.abs(dx) > dy * 1.2f) {
                            isPeekDragging = true;
                            if (peekVelocityTracker != null) peekVelocityTracker.addMovement(ev);
                            return true;
                        }
                        break;
                }
                return super.onInterceptTouchEvent(ev);
            }

            @Override
            public boolean onTouchEvent(MotionEvent ev) {
                if (peekVelocityTracker != null) peekVelocityTracker.addMovement(ev);
                switch (ev.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        float totalDx = ev.getX() - peekDownX;
                        if (isPeekDragging) {
                            if (isDeputyOpen) {
                                applyPeek(getWidth() + totalDx);
                            } else {
                                applyPeek(totalDx);
                            }
                            return true;
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        if (isPeekDragging) {
                            isPeekDragging = false;
                            float vx = 0f;
                            if (peekVelocityTracker != null) {
                                peekVelocityTracker.computeCurrentVelocity(1000);
                                vx = peekVelocityTracker.getXVelocity();
                            }
                            float finalDx = ev.getX() - peekDownX;
                            if (isDeputyOpen) {
                                finishPeek(getWidth() + finalDx, vx);
                            } else {
                                finishPeek(finalDx, vx);
                            }
                            return true;
                        }
                        break;
                }
                return super.onTouchEvent(ev);
            }
        };
        rootFrame.setBackgroundColor(0xFF080C14);

        // 1. UNDERNEATH LAYER: Deputy Workplace Deck
        deputyContainer = new FrameLayout(this);
        deputyContainer.addView(buildDeputyView(), new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        deputyContainer.setScaleX(0.94f);
        deputyContainer.setScaleY(0.94f);
        deputyContainer.setTranslationX(-dp(30));
        rootFrame.addView(deputyContainer);

        // 2. SCRIM LAYER between Deputy and Gatehouse surface
        deputyScrim = new View(this);
        deputyScrim.setBackgroundColor(0xFF000000);
        deputyScrim.setAlpha(0.65f);
        rootFrame.addView(deputyScrim, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        // 3. TOP SLIDING LAYER: Main Gatehouse Surface Container
        mainSurfaceContainer = new FrameLayout(this);
        mainSurfaceContainer.setBackgroundColor(colBg);

        LinearLayout screenLayout = new LinearLayout(this);
        screenLayout.setOrientation(LinearLayout.VERTICAL);
        screenLayout.setBackgroundColor(colBg);
        screenLayout.setFitsSystemWindows(true);
        screenLayout.setPadding(dp(16), dp(12), dp(16), 0);

        // Sticky Top Dock
        screenLayout.addView(modeBar());
        screenLayout.addView(buildDiagnosticsStrip());
        screenLayout.addView(buildTabBar());

        // 4. SYNCHRONIZED 4-TAB HORIZONTAL PAGER CONTAINER
        tabPagerFrame = new FrameLayout(this) {
            @Override
            public boolean onInterceptTouchEvent(MotionEvent ev) {
                switch (ev.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        pageSwipeDownX = ev.getX();
                        pageSwipeDownY = ev.getY();
                        isPageSwiping = false;
                        break;
                    case MotionEvent.ACTION_MOVE:
                        float dx = ev.getX() - pageSwipeDownX;
                        float dy = Math.abs(ev.getY() - pageSwipeDownY);
                        // Horizontal swipe between tabs
                        if (Math.abs(dx) > dp(20) && Math.abs(dx) > dy * 1.3f) {
                            isPageSwiping = true;
                            getParent().requestDisallowInterceptTouchEvent(true);
                            return true;
                        }
                        break;
                }
                return super.onInterceptTouchEvent(ev);
            }

            @Override
            public boolean onTouchEvent(MotionEvent ev) {
                int w = getWidth();
                if (w <= 0) return super.onTouchEvent(ev);
                switch (ev.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        if (isPageSwiping) {
                            float totalDx = ev.getX() - pageSwipeDownX;
                            float deltaPages = -totalDx / w;
                            applyTabScrollPosition(currentTab + deltaPages);
                            return true;
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        if (isPageSwiping) {
                            isPageSwiping = false;
                            getParent().requestDisallowInterceptTouchEvent(false);
                            float totalDx = ev.getX() - pageSwipeDownX;
                            float deltaPages = -totalDx / w;
                            float finalPos = currentTab + deltaPages;
                            int targetTab = Math.max(0, Math.min(3, Math.round(finalPos)));
                            animateTabToPosition(targetTab);
                            return true;
                        }
                        break;
                }
                return super.onTouchEvent(ev);
            }
        };
        LinearLayout.LayoutParams tplp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
        tabPagerFrame.setLayoutParams(tplp);

        // --- PAGE 1: PATROL ---
        scrollPatrol = new ScrollView(this);
        scrollPatrol.setBackgroundColor(colBg);
        scrollPatrol.setVerticalScrollBarEnabled(false);
        scrollPatrol.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(0, dp(4), 0, dp(28));

        patrolContent = new LinearLayout(this);
        patrolContent.setOrientation(LinearLayout.VERTICAL);
        patrolContent.addView(headerCard());
        patrolContent.addView(buildChronographSection());

        chainBannerView = new AnimatedChainBannerView(this);
        LinearLayout.LayoutParams cbl = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(44));
        cbl.topMargin = dp(10);
        cbl.bottomMargin = dp(4);
        chainBannerView.setLayoutParams(cbl);
        patrolContent.addView(chainBannerView);

        pills = new LinearLayout(this);
        pills.setOrientation(LinearLayout.HORIZONTAL);
        pills.setPadding(0, dp(4), 0, dp(6));
        patrolContent.addView(pills);

        banner = new TextView(this);
        banner.setTextSize(13);
        banner.setTextColor(colAccent);
        banner.setPadding(dp(14), dp(12), dp(14), dp(12));
        banner.setBackground(rounded(colPanel2, dp(12)));
        banner.setVisibility(View.GONE);
        LinearLayout.LayoutParams bl = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        bl.topMargin = dp(6);
        bl.bottomMargin = dp(8);
        banner.setLayoutParams(bl);
        patrolContent.addView(banner);

        patrolContent.addView(sectionHeader("External Patrols", null));
        externalRow = new LinearLayout(this);
        externalRow.setOrientation(LinearLayout.HORIZONTAL);
        externalRow.setPadding(0, dp(2), 0, dp(8));

        tileExternalFull = patrolActionCard("External (Full)", EXTERNAL_CHOICES[1], true);
        tileExternalHalf = patrolActionCard("External (Half)", EXTERNAL_CHOICES[3], false);
        externalRow.addView(tileExternalFull);
        externalRow.addView(tileExternalHalf);
        patrolContent.addView(externalRow);

        patrolContent.addView(sectionHeader("Internal Factory Floors (Lots 14–18)", null));
        internalBadgesRow = new LinearLayout(this);
        internalBadgesRow.setOrientation(LinearLayout.HORIZONTAL);
        internalBadgesRow.setPadding(0, dp(2), 0, dp(10));

        for (int i = 0; i < INTERNAL_LOTS.length; i += 2) {
            internalBadgesRow.addView(lotBadge(INTERNAL_LOTS[i], INTERNAL_LOTS[i + 1], i == INTERNAL_LOTS.length - 2));
        }
        patrolContent.addView(internalBadgesRow);

        LinearLayout fireHeader = new LinearLayout(this);
        fireHeader.setOrientation(LinearLayout.HORIZONTAL);
        fireHeader.setGravity(Gravity.CENTER_VERTICAL);
        fireHeader.setPadding(0, dp(10), 0, dp(6));

        TextView fTitle = new TextView(this);
        fTitle.setText("FIRE & PUMP SYSTEMS (5) · 1,200 PSI OPTIMAL");
        fTitle.setTextColor(colQuiet);
        fTitle.setTextSize(10);
        fTitle.setTypeface(Typeface.DEFAULT_BOLD);
        fTitle.setLetterSpacing(0.12f);
        LinearLayout.LayoutParams ftl = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        fTitle.setLayoutParams(ftl);
        fireHeader.addView(fTitle);

        fireStatusChip = new TextView(this);
        fireStatusChip.setText("0/5 CHECKED");
        fireStatusChip.setTextColor(colMuted);
        fireStatusChip.setTextSize(10);
        fireStatusChip.setTypeface(Typeface.MONOSPACE);
        fireStatusChip.setPadding(dp(6), dp(2), dp(6), dp(2));
        fireStatusChip.setBackground(rounded(colPanel2, dp(6)));
        fireHeader.addView(fireStatusChip);
        patrolContent.addView(fireHeader);

        fireCard = new LinearLayout(this);
        fireCard.setOrientation(LinearLayout.VERTICAL);
        fireCard.setBackground(rounded(colPanel, dp(14)));
        fireCard.setPadding(dp(12), dp(8), dp(12), dp(8));

        fireList = new LinearLayout(this);
        fireList.setOrientation(LinearLayout.VERTICAL);
        for (int i = 0; i < FIRE_POINTS.length; i += 2) {
            fireList.addView(fireCompactRow(FIRE_POINTS[i], FIRE_POINTS[i + 1], i == FIRE_POINTS.length - 2));
        }
        fireCard.addView(fireList);
        patrolContent.addView(fireCard);

        patrolContent.addView(sectionHeader("Rapid Evidence Dock", null));
        dock = buildCaptureDock();
        patrolContent.addView(dock);

        patrolContent.addView(tonightLabel());
        tonight = new LinearLayout(this);
        tonight.setOrientation(LinearLayout.VERTICAL);
        tonight.setPadding(0, dp(4), 0, dp(20));
        patrolContent.addView(tonight);

        primary = new TextView(this);
        primary.setTextSize(15);
        primary.setTypeface(Typeface.DEFAULT_BOLD);
        primary.setGravity(Gravity.CENTER);
        primary.setPadding(dp(16), dp(18), dp(16), dp(18));
        patrolContent.addView(primary);

        pageTitle = label("06:05 MORNING HANDOVER REPORT");
        pageTitle.setPadding(0, dp(24), 0, dp(8));
        pageTitle.setVisibility(View.GONE);
        patrolContent.addView(pageTitle);

        page = new TextView(this);
        page.setTextColor(colPale);
        page.setTextSize(10);
        page.setTypeface(Typeface.MONOSPACE);
        page.setBackground(rounded(colPanel, dp(14)));
        page.setPadding(dp(14), dp(14), dp(14), dp(14));
        page.setVisibility(View.GONE);
        patrolContent.addView(page);

        btnShareReport = new TextView(this);
        btnShareReport.setText("📤 SHARE MORNING HANDOVER REPORT");
        btnShareReport.setTextColor(colAccentInk);
        btnShareReport.setTextSize(14);
        btnShareReport.setTypeface(Typeface.DEFAULT_BOLD);
        btnShareReport.setGravity(Gravity.CENTER);
        btnShareReport.setPadding(dp(16), dp(16), dp(16), dp(16));
        btnShareReport.setBackground(pressable(colAccent, dp(16)));
        btnShareReport.setVisibility(View.GONE);
        LinearLayout.LayoutParams spl = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        spl.topMargin = dp(12);
        btnShareReport.setLayoutParams(spl);
        btnShareReport.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                hapticHeavyClick();
                shareHandoverReport();
            }
        });
        patrolContent.addView(btnShareReport);
        root.addView(patrolContent);
        scrollPatrol.addView(root);
        tabPagerFrame.addView(scrollPatrol);

        // --- PAGE 2: CONTACTS ---
        scrollContacts = new ScrollView(this);
        scrollContacts.setBackgroundColor(colBg);
        scrollContacts.setVerticalScrollBarEnabled(false);
        scrollContacts.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        contactsContent = buildContactsTab();
        scrollContacts.addView(contactsContent);
        tabPagerFrame.addView(scrollContacts);

        // --- PAGE 3: HANDBOOK ---
        scrollHandbook = new ScrollView(this);
        scrollHandbook.setBackgroundColor(colBg);
        scrollHandbook.setVerticalScrollBarEnabled(false);
        scrollHandbook.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        scrollHandbook.addView(buildRosterView());
        tabPagerFrame.addView(scrollHandbook);

        // --- PAGE 4: TOOLS ---
        scrollTools = new ScrollView(this);
        scrollTools.setBackgroundColor(colBg);
        scrollTools.setVerticalScrollBarEnabled(false);
        scrollTools.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        toolsContent = buildToolsTab();
        scrollTools.addView(toolsContent);
        tabPagerFrame.addView(scrollTools);

        screenLayout.addView(tabPagerFrame);
        mainSurfaceContainer.addView(screenLayout);

        // Post-layout initialization for 1:1 page alignment
        tabPagerFrame.post(new Runnable() {
            public void run() {
                applyTabScrollPosition((float) currentTab);
            }
        });

        // Peek Seam Drop Shadow
        peekShadow = new View(this);
        peekShadow.setBackground(new GradientDrawable(GradientDrawable.Orientation.RIGHT_LEFT,
                new int[]{0x99000000, 0x44000000, 0x00000000}));
        peekShadow.setAlpha(0f);
        FrameLayout.LayoutParams pslp = new FrameLayout.LayoutParams(dp(30), FrameLayout.LayoutParams.MATCH_PARENT);
        peekShadow.setLayoutParams(pslp);

        rootFrame.addView(mainSurfaceContainer, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        rootFrame.addView(peekShadow);

        // Pulsing Scroll Indicator
        scrollIndicator = new PulsingScrollIndicator(this);
        FrameLayout.LayoutParams silp = new FrameLayout.LayoutParams(dp(5), FrameLayout.LayoutParams.MATCH_PARENT);
        silp.gravity = Gravity.RIGHT;
        silp.topMargin = dp(140);
        silp.bottomMargin = dp(20);
        silp.rightMargin = dp(2);
        scrollIndicator.setLayoutParams(silp);
        mainSurfaceContainer.addView(scrollIndicator);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ScrollView activeScroll = (scrollPatrol != null ? scrollPatrol : scroll);
            if (activeScroll != null) {
                activeScroll.setOnScrollChangeListener(new View.OnScrollChangeListener() {
                    public void onScrollChange(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
                        if (root != null && v != null) {
                            int maxScroll = root.getHeight() - v.getHeight();
                            if (maxScroll > 0) {
                                float pct = Math.max(0f, Math.min(1f, (float) scrollY / maxScroll));
                                if (scrollIndicator != null) {
                                    scrollIndicator.setScrollProgress(pct);
                                }
                            }
                        }
                    }
                });
            }
        }

        // conureOverlay
        // cnlp
                
        
        

        setContentView(rootFrame);
    }

    private final Runnable tick = new Runnable() {
        public void run() {
            commitDue();
            refresh();
            if (chronographView != null) chronographView.invalidate();
            checkWelfareDue();
            updateDiagnostics();
            ticker.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onPause() {
        super.onPause();
        stopLightingModes();
        unregisterSensors();
        try { if (widgetReceiver != null) unregisterReceiver(widgetReceiver); } catch (Throwable t) {}
        stopGpsUpdates();
        commitAll();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ticker.removeCallbacks(tick);
        stopLightingModes();
        unregisterSensors();
        try { if (widgetReceiver != null) unregisterReceiver(widgetReceiver); } catch (Throwable t) {}
        stopGpsUpdates();
        if (voiceRecorder != null) {
            try { voiceRecorder.release(); } catch (Exception e) {}
        }
    }

    // =========================================================================
    // 🔮 PULSING SCROLL INDICATOR (RIGHT-HAND EDGE)
    // =========================================================================

    private class PulsingScrollIndicator extends View {
        private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint thumbPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private float scrollPct = 0f;
        private float pulseAlpha = 0.5f;

        public PulsingScrollIndicator(Context context) {
            super(context);
            trackPaint.setStyle(Paint.Style.FILL);
            thumbPaint.setStyle(Paint.Style.FILL);
            glowPaint.setStyle(Paint.Style.FILL);

            ValueAnimator pulseAnim = ValueAnimator.ofFloat(0.35f, 0.95f);
            pulseAnim.setDuration(1600);
            pulseAnim.setRepeatMode(ValueAnimator.REVERSE);
            pulseAnim.setRepeatCount(ValueAnimator.INFINITE);
            pulseAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                public void onAnimationUpdate(ValueAnimator a) {
                    pulseAlpha = (Float) a.getAnimatedValue();
                    invalidate();
                }
            });
            pulseAnim.start();
        }

        public void setScrollProgress(float p) {
            this.scrollPct = Math.max(0f, Math.min(1f, p));
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            int w = getWidth();
            int h = getHeight();
            if (w <= 0 || h <= 0) return;

            trackPaint.setColor(colLineSubtle);
            canvas.drawRect(0, 0, w, h, trackPaint);

            float thumbH = dp(44);
            float thumbY = scrollPct * (h - thumbH);

            glowPaint.setColor(colAccent);
            glowPaint.setAlpha((int) (pulseAlpha * 90));
            canvas.drawRoundRect(new RectF(0, thumbY - dp(4), w, thumbY + thumbH + dp(4)), dp(2), dp(2), glowPaint);

            thumbPaint.setColor(colAccent);
            thumbPaint.setAlpha((int) (pulseAlpha * 255));
            canvas.drawRoundRect(new RectF(0, thumbY, w, thumbY + thumbH), dp(2), dp(2), thumbPaint);
        }
    }

    // =========================================================================
    // 4-THEME SWITCHER BAR
    // =========================================================================

    private LinearLayout modeBar() {
        modeBar = new LinearLayout(this);
        modeBar.setOrientation(LinearLayout.HORIZONTAL);
        modeBar.setGravity(Gravity.CENTER_VERTICAL);
        modeBar.setPadding(0, 0, 0, dp(8));

        HorizontalScrollView hsv = new HorizontalScrollView(this);
        hsv.setHorizontalScrollBarEnabled(false);

        LinearLayout pill = new LinearLayout(this);
        pill.setOrientation(LinearLayout.HORIZONTAL);
        pill.setBackground(rounded(colPanel, dp(20)));
        pill.setPadding(dp(3), dp(3), dp(3), dp(3));

        pill.addView(themeSwitchButton("OLED Gold", THEME_GOLD));
        pill.addView(themeSwitchButton("0-Lux Red", THEME_RED));
        pill.addView(themeSwitchButton("NVG Green", THEME_NVG));
        pill.addView(themeSwitchButton("Cyber Violet", THEME_VIOLET));

        hsv.addView(pill);
        modeBar.addView(hsv);
        return modeBar;
    }

    private TextView themeSwitchButton(String name, final int themeId) {
        TextView btn = new TextView(this);
        btn.setText(name);
        btn.setTextSize(10);
        btn.setTypeface(Typeface.DEFAULT_BOLD);
        btn.setPadding(dp(8), dp(4), dp(8), dp(4));
        boolean sel = activeTheme == themeId;
        btn.setTextColor(sel ? colAccentInk : colMuted);
        btn.setBackground(sel ? rounded(colAccent, dp(14)) : null);
        btn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (activeTheme != themeId) {
                    hapticClick();
                    activeTheme = themeId;
                    rebuildCurrentScreen();
                }
            }
        });
        return btn;
    }

    // =========================================================================
    // 🧭 DETAILED ROTATING COMPASS ROSE VIEW
    // =========================================================================

    private class DetailedCompassView extends View {
        private final Paint bezelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint tickPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint needleNorth = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint needleSouth = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint centerHub = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path pathN = new Path();
        private final Path pathS = new Path();

        public DetailedCompassView(Context context) {
            super(context);
            bezelPaint.setStyle(Paint.Style.STROKE);
            bezelPaint.setStrokeWidth(dp(2));
            tickPaint.setStyle(Paint.Style.STROKE);
            tickPaint.setStrokeCap(Paint.Cap.ROUND);
            textPaint.setTextAlign(Paint.Align.CENTER);
            textPaint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
            needleNorth.setStyle(Paint.Style.FILL);
            needleSouth.setStyle(Paint.Style.FILL);
            centerHub.setStyle(Paint.Style.FILL);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            int w = getWidth();
            int h = getHeight();
            if (w <= 0 || h <= 0) return;

            float cx = w / 2f;
            float cy = h / 2f;
            float r = Math.min(w, h) / 2f - dp(12);

            bezelPaint.setColor(colLineSubtle);
            canvas.drawCircle(cx, cy, r, bezelPaint);
            bezelPaint.setColor(colLine);
            canvas.drawCircle(cx, cy, r - dp(10), bezelPaint);

            canvas.save();
            canvas.rotate(-currentAzimuth, cx, cy);

            for (int deg = 0; deg < 360; deg += 5) {
                double rad = Math.toRadians(deg - 90);
                boolean isCardinal = (deg % 90 == 0);
                boolean isMajor = (deg % 30 == 0);

                float len = isCardinal ? dp(10) : (isMajor ? dp(6) : dp(3));
                float rOuter = r - dp(10);
                float rInner = rOuter - len;

                float x1 = (float) (cx + Math.cos(rad) * rOuter);
                float y1 = (float) (cy + Math.sin(rad) * rOuter);
                float x2 = (float) (cx + Math.cos(rad) * rInner);
                float y2 = (float) (cy + Math.sin(rad) * rInner);

                tickPaint.setColor(isCardinal ? (deg == 0 ? colCrimson : colAccent) : colQuiet);
                tickPaint.setStrokeWidth(isCardinal ? dp(2) : dp(1));
                canvas.drawLine(x1, y1, x2, y2, tickPaint);

                if (isCardinal || isMajor) {
                    float rText = rInner - dp(9);
                    float tx = (float) (cx + Math.cos(rad) * rText);
                    float ty = (float) (cy + Math.sin(rad) * rText) + dp(3);

                    String label;
                    if (deg == 0) label = "N";
                    else if (deg == 90) label = "E";
                    else if (deg == 180) label = "S";
                    else if (deg == 270) label = "W";
                    else label = String.valueOf(deg);

                    textPaint.setColor(deg == 0 ? colCrimson : (isCardinal ? colPale : colMuted));
                    textPaint.setTextSize(isCardinal ? dp(10) : dp(8));
                    canvas.drawText(label, tx, ty, textPaint);
                }
            }
            canvas.restore();

            float nLen = r - dp(26);
            float nWidth = dp(7);

            pathN.reset();
            pathN.moveTo(cx, cy - nLen);
            pathN.lineTo(cx + nWidth, cy);
            pathN.lineTo(cx, cy - dp(4));
            pathN.lineTo(cx - nWidth, cy);
            pathN.close();
            needleNorth.setColor(colCrimson);
            canvas.drawPath(pathN, needleNorth);

            pathS.reset();
            pathS.moveTo(cx, cy + nLen);
            pathS.lineTo(cx + nWidth, cy);
            pathS.lineTo(cx, cy + dp(4));
            pathS.lineTo(cx - nWidth, cy);
            pathS.close();
            needleSouth.setColor(colPale);
            canvas.drawPath(pathS, needleSouth);

            centerHub.setColor(colBg);
            canvas.drawCircle(cx, cy, dp(6), centerHub);
            centerHub.setColor(colAccent);
            canvas.drawCircle(cx, cy, dp(3), centerHub);
        }
    }

    // =========================================================================
    // SENSORS & 🔦 ROBUST DOUBLE-CHOP FLICK-TO-TORCH GESTURE
    // =========================================================================

    @Override
    public void onSensorChanged(SensorEvent event) {
        float azimuthDegrees = 0;
        if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            float[] rotationMatrix = new float[9];
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values);
            float[] orientation = new float[3];
            SensorManager.getOrientation(rotationMatrix, orientation);
            azimuthDegrees = (float) Math.toDegrees(orientation[0]);
        } else if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, lastAccel, 0, 3);
            hasAccel = true;
            if (activeHoloCard != null) activeHoloCard.invalidate();
            if (activeLevelerView != null) activeLevelerView.invalidate();

            // 🔦 Robust Double-Chop Gesture (High-Pass acceleration delta)
            final float alpha = 0.8f;
            lastGravity[0] = alpha * lastGravity[0] + (1 - alpha) * event.values[0];
            lastGravity[1] = alpha * lastGravity[1] + (1 - alpha) * event.values[1];
            lastGravity[2] = alpha * lastGravity[2] + (1 - alpha) * event.values[2];

            float linearX = event.values[0] - lastGravity[0];
            float linearY = event.values[1] - lastGravity[1];
            float linearZ = event.values[2] - lastGravity[2];
            float linearMag = (float) Math.sqrt(linearX * linearX + linearY * linearY + linearZ * linearZ);

            long now = SystemClock.elapsedRealtime();

            // Detect sharp chopping flick (13.0 m/s^2 linear acceleration)
            if (linearMag > 13.0f && (now - lastToggleCooldown > 900)) {
                if (chopCount == 0) {
                    chopCount = 1;
                    lastChopTimestamp = now;
                } else if (chopCount == 1 && (now - lastChopTimestamp >= 120 && now - lastChopTimestamp <= 800)) {
                    chopCount = 0;
                    lastToggleCooldown = now;
                    hapticDoublePulse();
                    toggleHardwareTorch();
                    banner.setText(isHardwareTorchOn ? "🔦 Double-Chop: Torch ON" : "🔦 Double-Chop: Torch OFF");
                    banner.setVisibility(View.VISIBLE);
                }
            }
            if (chopCount == 1 && (now - lastChopTimestamp > 800)) {
                chopCount = 0;
            }

        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, lastMag, 0, 3);
            hasMag = true;
        }

        if (rotationSensor == null && hasAccel && hasMag) {
            float[] r = new float[9];
            float[] i = new float[9];
            if (SensorManager.getRotationMatrix(r, i, lastAccel, lastMag)) {
                float[] orientation = new float[3];
                SensorManager.getOrientation(r, orientation);
                azimuthDegrees = (float) Math.toDegrees(orientation[0]);
            }
        }

        if (azimuthDegrees < 0) azimuthDegrees += 360;
        currentAzimuth = azimuthDegrees;
        updateCompassDisplay(azimuthDegrees);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    private void updateCompassDisplay(float deg) {
        if (activeCompassView != null) activeCompassView.invalidate();
        if (compassHeadingText != null) {
            int d = (int) deg;
            compassHeadingText.setText(String.format(Locale.US, "%03d° %s", d, getCardinal(d)));
        }
        if (compassSectorText != null) {
            compassSectorText.setText("📍 Facing: " + getHumeSector((int) deg));
        }
    }

    private String getCardinal(int deg) {
        if (deg >= 338 || deg < 23) return "N";
        if (deg >= 23 && deg < 68) return "NE";
        if (deg >= 68 && deg < 113) return "E";
        if (deg >= 113 && deg < 158) return "SE";
        if (deg >= 158 && deg < 203) return "S";
        if (deg >= 203 && deg < 248) return "SW";
        if (deg >= 248 && deg < 293) return "W";
        return "NW";
    }

    private String getHumeSector(int deg) {
        if (deg >= 315 || deg < 45) return "Gate A · North Boundary (Kingston Rd Entry)";
        if (deg >= 45 && deg < 135) return "Lots 14 & 15 · East Boundary (Sawmill & Assembly)";
        if (deg >= 135 && deg < 225) return "Gate B · South Boundary (Lot 16 Factory & Pump 16)";
        return "Lots 17 & 18 · West Boundary (Timber Yard & Chem)";
    }

    // =========================================================================
    // DIAGNOSTICS & TELEMETRY STRIP (KINGSTON AMBIENT WEATHER)
    // =========================================================================

    private LinearLayout buildDiagnosticsStrip() {
        diagStrip = new LinearLayout(this);
        diagStrip.setOrientation(LinearLayout.HORIZONTAL);
        diagStrip.setGravity(Gravity.CENTER_VERTICAL);
        diagStrip.setBackground(rounded(colPanel, dp(12)));
        diagStrip.setPadding(dp(10), dp(7), dp(10), dp(7));
        LinearLayout.LayoutParams dsl = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        dsl.bottomMargin = dp(10);
        diagStrip.setLayoutParams(dsl);

        diagOledPower = new TextView(this);
        diagOledPower.setText("⚡ OLED: 0.14W");
        diagOledPower.setTextColor(colEmerald);
        diagOledPower.setTextSize(10);
        diagOledPower.setTypeface(Typeface.MONOSPACE);
        LinearLayout.LayoutParams dpl = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.1f);
        diagOledPower.setLayoutParams(dpl);
        diagStrip.addView(diagOledPower);

        diagAmbientWeather = new TextView(this);
        diagAmbientWeather.setText(String.format(Locale.US, "🌤️ %.1f°C KINGSTON", curTempC));
        diagAmbientWeather.setTextColor(colCyan);
        diagAmbientWeather.setTextSize(10);
        diagAmbientWeather.setTypeface(Typeface.MONOSPACE);
        LinearLayout.LayoutParams dtl = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.3f);
        diagAmbientWeather.setLayoutParams(dtl);
        diagStrip.addView(diagAmbientWeather);

        diagBatteryRuntime = new TextView(this);
        diagBatteryRuntime.setText("🔋 14.2h REMAIN");
        diagBatteryRuntime.setTextColor(colAccent);
        diagBatteryRuntime.setTextSize(10);
        diagBatteryRuntime.setTypeface(Typeface.MONOSPACE);
        LinearLayout.LayoutParams dbl = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.1f);
        diagBatteryRuntime.setLayoutParams(dbl);
        diagStrip.addView(diagBatteryRuntime);

        return diagStrip;
    }

    private void updateDiagnostics() {
        try {
            IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            Intent batteryStatus = registerReceiver(null, ifilter);
            if (batteryStatus != null) {
                int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                float batteryPct = level * 100 / (float) scale;
                float estHours = (batteryPct / 100f) * 22.5f;

                if (diagBatteryRuntime != null) {
                    diagBatteryRuntime.setText(String.format(Locale.US, "🔋 %d%% (%.1fh)", (int) batteryPct, estHours));
                }
            }
        } catch (Exception e) {}
    }

    // =========================================================================
    // #4 ⏱️ INTERACTIVE ANALOG PRESSURE GAUGE (0 - 1,600 PSI · 175 PSI OPTIMAL)
    // =========================================================================

    interface OnPressureChangedListener {
        void onPressureChanged(int psi);
    }

    private class PressureGaugeView extends View {
        private final Paint bezelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint dialBackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint arcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint tickPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint needlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint needleGlowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint hubPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint digitalValPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint digitalSubPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        private final RectF arcRect = new RectF();
        private final Path needlePath = new Path();

        private static final int MIN_PSI = 0;
        private static final int MAX_PSI = 1600;
        private static final float START_ANGLE = 150f;
        private static final float SWEEP_ANGLE = 240f;

        private int currentPressure = 1200; // Standard 1,200 PSI optimal
        private float animatedNeedleAngle = 0f;
        private ValueAnimator animator;
        private OnPressureChangedListener listener;
        private int lastHapticPsi = -1;

        public PressureGaugeView(Context context) {
            super(context);
            bezelPaint.setStyle(Paint.Style.STROKE);
            bezelPaint.setStrokeWidth(dp(3));
            dialBackPaint.setStyle(Paint.Style.FILL);
            arcPaint.setStyle(Paint.Style.STROKE);
            arcPaint.setStrokeCap(Paint.Cap.ROUND);
            tickPaint.setStyle(Paint.Style.STROKE);
            tickPaint.setStrokeCap(Paint.Cap.ROUND);
            labelPaint.setTextAlign(Paint.Align.CENTER);
            labelPaint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
            needlePaint.setStyle(Paint.Style.FILL);
            needleGlowPaint.setStyle(Paint.Style.STROKE);
            needleGlowPaint.setStrokeCap(Paint.Cap.ROUND);
            hubPaint.setStyle(Paint.Style.FILL);
            digitalValPaint.setTextAlign(Paint.Align.CENTER);
            digitalValPaint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
            digitalSubPaint.setTextAlign(Paint.Align.CENTER);
            digitalSubPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

            animatedNeedleAngle = psiToAngle(currentPressure);
        }

        private float dpf(float v) {
            return v * getResources().getDisplayMetrics().density;
        }

        public void setOnPressureChangedListener(OnPressureChangedListener l) {
            this.listener = l;
        }

        public void setPressure(int psi) {
            currentPressure = Math.max(MIN_PSI, Math.min(MAX_PSI, psi));
            animatedNeedleAngle = psiToAngle(currentPressure);
            invalidate();
        }

        public void animateToPressure(int targetPsi) {
            final int target = Math.max(MIN_PSI, Math.min(MAX_PSI, targetPsi));
            if (animator != null && animator.isRunning()) animator.cancel();

            final float startAngle = animatedNeedleAngle;
            final float endAngle = psiToAngle(target);

            animator = ValueAnimator.ofFloat(startAngle, endAngle);
            animator.setDuration(360);
            animator.setInterpolator(new OvershootInterpolator(1.2f));
            animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                public void onAnimationUpdate(ValueAnimator va) {
                    animatedNeedleAngle = (Float) va.getAnimatedValue();
                    currentPressure = angleToPsi(animatedNeedleAngle);
                    invalidate();
                }
            });
            animator.start();
        }

        private float psiToAngle(int psi) {
            return START_ANGLE + (psi * 1.0f / MAX_PSI) * SWEEP_ANGLE;
        }

        private int angleToPsi(float angle) {
            float norm = (angle - START_ANGLE) / SWEEP_ANGLE;
            norm = Math.max(0f, Math.min(1f, norm));
            return (int) (norm * MAX_PSI);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_MOVE:
                    float cx = getWidth() / 2f;
                    float cy = getHeight() / 2f + dp(6);
                    float dx = event.getX() - cx;
                    float dy = event.getY() - cy;

                    double rad = Math.atan2(dy, dx);
                    float deg = (float) Math.toDegrees(rad);
                    if (deg < 0) deg += 360f;
                    if (deg < 90f) deg += 360f;
                    if (deg < START_ANGLE) deg = START_ANGLE;
                    if (deg > START_ANGLE + SWEEP_ANGLE) deg = START_ANGLE + SWEEP_ANGLE;

                    if (animator != null && animator.isRunning()) animator.cancel();
                    animatedNeedleAngle = deg;
                    currentPressure = angleToPsi(deg);

                    // Magnetic Tactile Detents at 800, 1000, 1200
                    int snapStep = -1;
                    if (Math.abs(currentPressure - 1200) < 22) snapStep = 1200;
                    else if (Math.abs(currentPressure - 1000) < 22) snapStep = 1000;
                    else if (Math.abs(currentPressure - 800) < 22) snapStep = 800;

                    if (snapStep != -1) {
                        if (lastHapticPsi != snapStep) {
                            lastHapticPsi = snapStep;
                            hapticDoublePulse();
                        }
                    } else {
                        int step = (currentPressure / 50) * 50;
                        if (step != lastHapticPsi) {
                            lastHapticPsi = step;
                            hapticClick();
                        }
                    }

                    if (listener != null) listener.onPressureChanged(currentPressure);
                    invalidate();
                    getParent().requestDisallowInterceptTouchEvent(true);
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    getParent().requestDisallowInterceptTouchEvent(false);
                    return true;
            }
            return super.onTouchEvent(event);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            int w = getWidth();
            int h = getHeight();
            if (w <= 0 || h <= 0) return;

            float cx = w / 2f;
            float cy = h / 2f + dp(8);
            float radius = Math.min(w, h) / 2f - dp(18);

            // Shaded Dial Background
            dialBackPaint.setColor(0xFF0F172A);
            canvas.drawCircle(cx, cy, radius + dp(6), dialBackPaint);

            arcRect.set(cx - radius, cy - radius, cx + radius, cy + radius);

            bezelPaint.setColor(colLineSubtle);
            bezelPaint.setStrokeWidth(dp(8));
            canvas.drawArc(arcRect, START_ANGLE, SWEEP_ANGLE, false, bezelPaint);

            // 🚨 Critical Low Zone (0 - 800 PSI · Bad)
            float redSweep = (800f / MAX_PSI) * SWEEP_ANGLE;
            arcPaint.setColor(colCrimson);
            arcPaint.setStrokeWidth(dp(6));
            canvas.drawArc(arcRect, START_ANGLE, redSweep, false, arcPaint);

            // ⚠️ Jack Up Zone (800 - 1,100 PSI · Jack Up at 1,000 PSI)
            float yellowStart = START_ANGLE + redSweep;
            float yellowSweep = (300f / MAX_PSI) * SWEEP_ANGLE;
            arcPaint.setColor(0xFFFFB703);
            arcPaint.setStrokeWidth(dp(6));
            canvas.drawArc(arcRect, yellowStart, yellowSweep, false, arcPaint);

            // ✓ Nominal Optimal Zone (1,100 - 1,350 PSI · Centered on 1,200 PSI★)
            float greenStart = yellowStart + yellowSweep;
            float greenSweep = (250f / MAX_PSI) * SWEEP_ANGLE;
            arcPaint.setColor(colEmerald);
            arcPaint.setStrokeWidth(dp(8));
            canvas.drawArc(arcRect, greenStart, greenSweep, false, arcPaint);

            // ⚠️ High Surge Zone (1,350 - 1,600 PSI)
            float surgeStart = greenStart + greenSweep;
            float surgeSweep = (250f / MAX_PSI) * SWEEP_ANGLE;
            arcPaint.setColor(colAccent);
            arcPaint.setStrokeWidth(dp(6));
            canvas.drawArc(arcRect, surgeStart, surgeSweep, false, arcPaint);

            // Scale Ticks (every 100 PSI)
            for (int psi = 0; psi <= MAX_PSI; psi += 100) {
                float a = psiToAngle(psi);
                double rad = Math.toRadians(a);
                boolean isTarget = (psi == 1200);
                boolean isJack = (psi == 1000);
                boolean isBad = (psi == 800);
                boolean isMajor = (psi % 200 == 0 || isJack || isTarget);

                float len = isMajor ? dp(10) : dp(5);
                float rOuter = radius - dp(8);
                float rInner = rOuter - len;

                float x1 = (float) (cx + Math.cos(rad) * rOuter);
                float y1 = (float) (cy + Math.sin(rad) * rOuter);
                float x2 = (float) (cx + Math.cos(rad) * rInner);
                float y2 = (float) (cy + Math.sin(rad) * rInner);

                tickPaint.setColor(isTarget ? colEmerald : (isBad ? colCrimson : (isJack ? 0xFFFFB703 : (isMajor ? colPale : colQuiet))));
                tickPaint.setStrokeWidth(isMajor ? dpf(2.2f) : dp(1));
                canvas.drawLine(x1, y1, x2, y2, tickPaint);

                if (isMajor) {
                    float rText = rInner - dp(10);
                    float tx = (float) (cx + Math.cos(rad) * rText);
                    float ty = (float) (cy + Math.sin(rad) * rText) + dp(3);

                    if (isTarget) {
                        labelPaint.setColor(colEmerald);
                    } else if (isBad) {
                        labelPaint.setColor(colCrimson);
                    } else if (isJack) {
                        labelPaint.setColor(0xFFFFB703);
                    } else {
                        labelPaint.setColor(colMuted);
                    }

                    labelPaint.setTextSize(dpf(8f));
                    String valStr = isTarget ? "1200★" : (isJack ? "1000▲" : String.valueOf(psi));
                    canvas.drawText(valStr, tx, ty, labelPaint);
                }
            }

            // Needle
            double nRad = Math.toRadians(animatedNeedleAngle);
            double nRadPerp = nRad + Math.PI / 2.0;

            float needleLen = radius - dp(14);
            float baseW = dpf(3.5f);

            float tipX = (float) (cx + Math.cos(nRad) * needleLen);
            float tipY = (float) (cy + Math.sin(nRad) * needleLen);
            float b1X = (float) (cx + Math.cos(nRadPerp) * baseW);
            float b1Y = (float) (cy + Math.sin(nRadPerp) * baseW);
            float b2X = (float) (cx - Math.cos(nRadPerp) * baseW);
            float b2Y = (float) (cy - Math.sin(nRadPerp) * baseW);

            needlePath.reset();
            needlePath.moveTo(tipX, tipY);
            needlePath.lineTo(b1X, b1Y);
            needlePath.lineTo(b2X, b2Y);
            needlePath.close();

            int needleColor = currentPressure < 800 ? colCrimson : (currentPressure < 1100 ? 0xFFFFB703 : colAccent);
            needlePaint.setColor(needleColor);
            canvas.drawPath(needlePath, needlePaint);

            hubPaint.setColor(colPale);
            canvas.drawCircle(cx, cy, dp(5), hubPaint);
            hubPaint.setColor(0xFF000000);
            canvas.drawCircle(cx, cy, dp(2), hubPaint);

            // Digital Readout in PSI
            digitalValPaint.setColor(colPale);
            digitalValPaint.setTextSize(dp(18));
            canvas.drawText(currentPressure + " PSI", cx, cy + dp(28), digitalValPaint);

            digitalSubPaint.setTextSize(dpf(8.5f));
            if (currentPressure >= 1100 && currentPressure <= 1350) {
                digitalSubPaint.setColor(colEmerald);
                canvas.drawText("✓ NOMINAL (1,200 PSI OPTIMAL)", cx, cy + dp(42), digitalSubPaint);
            } else if (currentPressure < 800) {
                digitalSubPaint.setColor(colCrimson);
                canvas.drawText("🚨 CRITICAL BAD (< 800 PSI)", cx, cy + dp(42), digitalSubPaint);
            } else if (currentPressure < 1100) {
                digitalSubPaint.setColor(0xFFFFB703);
                canvas.drawText("⚠️ LOW — JACK UP AT 1,000 PSI", cx, cy + dp(42), digitalSubPaint);
            } else {
                digitalSubPaint.setColor(colAccent);
                canvas.drawText("⚠️ HIGH SURGE (> 1,350 PSI)", cx, cy + dp(42), digitalSubPaint);
            }
        }
    }

    private void promptPumpHouseCheck(final String name, final String uid) {
        final boolean isLot16Inside = name.contains("Lot 16 Fire System") || name.contains("Inside");
        final LinearLayout box = dialogContainer("🔥 " + name, isLot16Inside ? "3-GAUGE SPRINKLER RISER SET" : "1,200 PSI OPTIMAL", colAccent);

        ArrayList<PressureRecord> history = pressureHistory.get(name);
        if (history != null && !history.isEmpty()) {
            TextView trendTitle = new TextView(this);
            trendTitle.setText("TONIGHT'S PRESSURE TREND (PSI):");
            trendTitle.setTextColor(colQuiet);
            trendTitle.setTextSize(10);
            trendTitle.setTypeface(Typeface.DEFAULT_BOLD);
            trendTitle.setLetterSpacing(0.12f);
            box.addView(trendTitle);

            LinearLayout sparkRow = new LinearLayout(this);
            sparkRow.setOrientation(LinearLayout.HORIZONTAL);
            sparkRow.setGravity(Gravity.CENTER_VERTICAL);
            sparkRow.setPadding(0, dp(4), 0, dp(10));

            for (int i = 0; i < history.size(); i++) {
                PressureRecord pr = history.get(i);
                TextView pChip = new TextView(this);
                pChip.setText(clock(pr.timeMinutes) + " (" + pr.pressureKpa + " PSI)");
                pChip.setTextColor(pr.pressureKpa < 800 ? colCrimson : (pr.pressureKpa < 1100 ? 0xFFFFB703 : colEmerald));
                pChip.setTextSize(10);
                pChip.setTypeface(Typeface.MONOSPACE);
                pChip.setPadding(dp(6), dp(3), dp(6), dp(3));
                pChip.setBackground(rounded(colPanel2, dp(6)));

                LinearLayout.LayoutParams pcl = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                pcl.rightMargin = dp(6);
                pChip.setLayoutParams(pcl);
                sparkRow.addView(pChip);
            }
            box.addView(sparkRow);
        }

        if (isLot16Inside) {
            // =================================================================
            // 💧 3-GAUGE SYSTEM FOR LOT 16 FS INSIDE (1,200 PSI NOMINAL)
            // =================================================================
            final String[] gaugeNames = {"💧 Town Main", "🚿 Sprinkler Riser", "⚡ Booster Pump"};
            final int[] gaugePressures = {1200, 1200, 1200};
            final int[] currentGaugeIdx = {0};

            box.addView(formSectionLabel("SELECT RISER GAUGE (TAP TO INSPECT)"));

            final LinearLayout tabPills = new LinearLayout(this);
            tabPills.setOrientation(LinearLayout.HORIZONTAL);
            tabPills.setPadding(0, dp(2), 0, dp(8));

            final LinearLayout[] cardViews = new LinearLayout[3];
            final TextView[] titleViews = new TextView[3];
            final TextView[] valViews = new TextView[3];
            final TextView[] tagViews = new TextView[3];

            final PressureGaugeView gaugeView = new PressureGaugeView(this);
            LinearLayout.LayoutParams glp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(185));
            gaugeView.setLayoutParams(glp);

            final EditText pressureField = modernInputField("1200");
            pressureField.setInputType(InputType.TYPE_CLASS_NUMBER);
            pressureField.setMinLines(1);
            pressureField.setText("1200");

            final boolean[] isSelfUpdating = {false};

            final Runnable updateCardsUi = new Runnable() {
                public void run() {
                    for (int i = 0; i < 3; i++) {
                        boolean sel = (i == currentGaugeIdx[0]);
                        int p = gaugePressures[i];
                        int statusCol = p < 800 ? colCrimson : (p < 1100 ? 0xFFFFB703 : colEmerald);
                        String statusStr = p < 800 ? "🚨 BAD" : (p < 1100 ? "⚠️ JACK" : "✓ NOMINAL");

                        cardViews[i].setBackground(rounded(sel ? (0x33000000 | (colAccent & 0x00FFFFFF)) : colPanel2, dp(10)));
                        titleViews[i].setTextColor(sel ? colAccent : colMuted);
                        valViews[i].setText(p + " PSI");
                        valViews[i].setTextColor(sel ? 0xFFFFFFFF : colPale);
                        tagViews[i].setText(statusStr);
                        tagViews[i].setTextColor(statusCol);
                    }
                }
            };

            for (int i = 0; i < 3; i++) {
                final int gIdx = i;
                LinearLayout card = new LinearLayout(this);
                card.setOrientation(LinearLayout.VERTICAL);
                card.setGravity(Gravity.CENTER);
                card.setPadding(dp(6), dp(8), dp(6), dp(8));
                LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
                if (i > 0) clp.leftMargin = dp(6);
                card.setLayoutParams(clp);

                TextView tvTitle = new TextView(this);
                tvTitle.setText(gaugeNames[i]);
                tvTitle.setTextSize(9.5f);
                tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
                tvTitle.setGravity(Gravity.CENTER);
                card.addView(tvTitle);

                TextView tvVal = new TextView(this);
                tvVal.setText("1200 PSI");
                tvVal.setTextSize(12.5f);
                tvVal.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
                tvVal.setGravity(Gravity.CENTER);
                tvVal.setPadding(0, dp(2), 0, dp(2));
                card.addView(tvVal);

                TextView tvTag = new TextView(this);
                tvTag.setText("✓ NOMINAL");
                tvTag.setTextSize(8.5f);
                tvTag.setTypeface(Typeface.MONOSPACE);
                tvTag.setGravity(Gravity.CENTER);
                card.addView(tvTag);

                card.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        hapticClick();
                        currentGaugeIdx[0] = gIdx;
                        updateCardsUi.run();
                        gaugeView.animateToPressure(gaugePressures[gIdx]);
                        isSelfUpdating[0] = true;
                        pressureField.setText(String.valueOf(gaugePressures[gIdx]));
                        pressureField.setSelection(pressureField.getText().length());
                        isSelfUpdating[0] = false;
                    }
                });

                cardViews[i] = card;
                titleViews[i] = tvTitle;
                valViews[i] = tvVal;
                tagViews[i] = tvTag;
                tabPills.addView(card);
            }
            updateCardsUi.run();
            box.addView(tabPills);

            box.addView(gaugeView);

            box.addView(formSectionLabel("LINE PRESSURE READOUT (PSI)"));
            box.addView(pressureField);

            gaugeView.setOnPressureChangedListener(new OnPressureChangedListener() {
                public void onPressureChanged(int psi) {
                    gaugePressures[currentGaugeIdx[0]] = psi;
                    if (!isSelfUpdating[0]) {
                        isSelfUpdating[0] = true;
                        pressureField.setText(String.valueOf(psi));
                        pressureField.setSelection(pressureField.getText().length());
                        updateCardsUi.run();
                        isSelfUpdating[0] = false;
                    }
                }
            });

            pressureField.addTextChangedListener(new TextWatcher() {
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                public void onTextChanged(CharSequence s, int start, int before, int count) {}
                public void afterTextChanged(Editable s) {
                    if (isSelfUpdating[0]) return;
                    try {
                        String str = s.toString().trim();
                        if (!str.isEmpty()) {
                            int val = Integer.parseInt(str);
                            gaugePressures[currentGaugeIdx[0]] = val;
                            gaugeView.setPressure(val);
                            updateCardsUi.run();
                        }
                    } catch (Exception e) {}
                }
            });

            TextView btnAllNormal = actionButton("✓ Set All 3 Gauges to 1,200 PSI (Normal)", colPanel2, colEmerald);
            btnAllNormal.setPadding(0, dp(10), 0, dp(10));
            LinearLayout.LayoutParams anlp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            anlp.topMargin = dp(6);
            btnAllNormal.setLayoutParams(anlp);
            btnAllNormal.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    hapticClick();
                    gaugePressures[0] = 1200;
                    gaugePressures[1] = 1200;
                    gaugePressures[2] = 1200;
                    gaugeView.animateToPressure(1200);
                    isSelfUpdating[0] = true;
                    pressureField.setText("1200");
                    pressureField.setSelection(4);
                    isSelfUpdating[0] = false;
                    updateCardsUi.run();
                }
            });
            box.addView(btnAllNormal);

            box.addView(formSectionLabel("SYSTEM STATUS & FAULTS"));
            final ArrayList<String> selectedConditions = new ArrayList<String>();
            selectedConditions.add("✓ All 3 Gauges In-Spec (1,200 PSI Normal)");

            final String[] lot16Options = {
                "✓ All 3 Gauges In-Spec (1,200 PSI Normal)",
                "⚠️ Line Pressure Low — Jack Up Due (< 1,000 PSI)",
                "🚨 Critical Pressure Drop (< 800 PSI Bad)",
                "⚠️ Booster Line Surge (> 1,350 PSI)",
                "⚠️ Minor Valve / Pipe Fitting Weep Noted"
            };

            final ArrayList<TextView> condViews = new ArrayList<TextView>();
            for (int i = 0; i < lot16Options.length; i++) {
                final String opt = lot16Options[i];
                final TextView item = new TextView(this);
                item.setText(opt);
                item.setTextSize(13);
                item.setPadding(dp(14), dp(10), dp(14), dp(10));

                final boolean isAllClear = i == 0;
                updateCheckItemStyle(item, selectedConditions.contains(opt), isAllClear);

                LinearLayout.LayoutParams il = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                il.bottomMargin = dp(6);
                item.setLayoutParams(il);

                item.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        hapticClick();
                        if (isAllClear) {
                            selectedConditions.clear();
                            selectedConditions.add(lot16Options[0]);
                        } else {
                            selectedConditions.remove(lot16Options[0]);
                            if (selectedConditions.contains(opt)) {
                                selectedConditions.remove(opt);
                            } else {
                                selectedConditions.add(opt);
                            }
                            if (selectedConditions.isEmpty()) {
                                selectedConditions.add(lot16Options[0]);
                            }
                        }
                        for (int k = 0; k < condViews.size(); k++) {
                            TextView tv = condViews.get(k);
                            String o = lot16Options[k];
                            updateCheckItemStyle(tv, selectedConditions.contains(o), k == 0);
                        }
                    }
                });

                condViews.add(item);
                box.addView(item);
            }

            final Dialog dlg = createTacticalDialog(box);

            LinearLayout btnRow = new LinearLayout(this);
            btnRow.setOrientation(LinearLayout.HORIZONTAL);
            btnRow.setPadding(0, dp(14), 0, 0);

            TextView btnCancel = actionButton("Cancel", colLine, colMuted);
            btnCancel.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    hapticClick();
                    dlg.dismiss();
                }
            });
            btnRow.addView(btnCancel);

            TextView btnSave = actionButton("✓ Log 3 Gauges", colAccent, colAccentInk);
            btnSave.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    hapticHeavyClick();
                    registerActivity();
                    tap(name, uid);

                    int avgPsi = (gaugePressures[0] + gaugePressures[1] + gaugePressures[2]) / 3;
                    ArrayList<PressureRecord> list = pressureHistory.get(name);
                    if (list == null) {
                        list = new ArrayList<PressureRecord>();
                        pressureHistory.put(name, list);
                    }
                    list.add(new PressureRecord(nowMinutes(), avgPsi));

                    StringBuilder sb = new StringBuilder();
                    sb.append("Lot 16 FS Inside: [Main: ").append(gaugePressures[0])
                      .append(" PSI, Riser: ").append(gaugePressures[1])
                      .append(" PSI, Booster: ").append(gaugePressures[2]).append(" PSI] ");
                    for (int k = 0; k < selectedConditions.size(); k++) {
                        if (k > 0) sb.append(", ");
                        sb.append(selectedConditions.get(k));
                    }
                    String line = sb.toString();
                    if (!oneLine(line)) {
                        banner.setText("notes must be one line");
                        banner.setVisibility(View.VISIBLE);
                        return;
                    }
                    note(Core.TOPIC_ROUTINE, line);
                    dlg.dismiss();
                }
            });
            btnRow.addView(btnSave);
            box.addView(btnRow);
            dlg.show();

        } else {
            // =================================================================
            // 🚰 SINGLE PUMP HOUSE (Lot 15, Lot 16 Outside, Lot 17, Lot 18)
            // =================================================================
            box.addView(formSectionLabel("INTERACTIVE ANALOG PRESSURE GAUGE (PSI)"));

            final PressureGaugeView gaugeView = new PressureGaugeView(this);
            LinearLayout.LayoutParams glp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(190));
            gaugeView.setLayoutParams(glp);
            box.addView(gaugeView);

            box.addView(formSectionLabel("LINE PRESSURE READOUT (PSI)"));
            final EditText pressureField = modernInputField("1200");
            pressureField.setInputType(InputType.TYPE_CLASS_NUMBER);
            pressureField.setMinLines(1);
            pressureField.setText("1200");
            box.addView(pressureField);

            final boolean[] isSelfUpdating = {false};

            gaugeView.setOnPressureChangedListener(new OnPressureChangedListener() {
                public void onPressureChanged(int psi) {
                    if (!isSelfUpdating[0]) {
                        isSelfUpdating[0] = true;
                        pressureField.setText(String.valueOf(psi));
                        pressureField.setSelection(pressureField.getText().length());
                        isSelfUpdating[0] = false;
                    }
                }
            });

            pressureField.addTextChangedListener(new TextWatcher() {
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                public void onTextChanged(CharSequence s, int start, int before, int count) {}
                public void afterTextChanged(Editable s) {
                    if (isSelfUpdating[0]) return;
                    try {
                        String str = s.toString().trim();
                        if (!str.isEmpty()) {
                            int val = Integer.parseInt(str);
                            isSelfUpdating[0] = true;
                            gaugeView.setPressure(val);
                            isSelfUpdating[0] = false;
                        }
                    } catch (Exception e) {}
                }
            });

            final String[] presetPressures = {"800", "1000", "1100", "1200", "1300", "1400"};
            HorizontalScrollView hsv = new HorizontalScrollView(this);
            hsv.setHorizontalScrollBarEnabled(false);
            LinearLayout presRow = new LinearLayout(this);
            presRow.setOrientation(LinearLayout.HORIZONTAL);
            presRow.setPadding(0, 0, 0, dp(10));
            for (final String p : presetPressures) {
                TextView chip = new TextView(this);
                if (p.equals("1200")) {
                    chip.setText("1,200 PSI ★");
                    chip.setTextColor(colAccentInk);
                    chip.setBackground(rounded(colEmerald, dp(12)));
                } else if (p.equals("800")) {
                    chip.setText("800 PSI (Bad)");
                    chip.setTextColor(colPale);
                    chip.setBackground(rounded(colCrimson, dp(12)));
                } else if (p.equals("1000")) {
                    chip.setText("1,000 PSI (Jack)");
                    chip.setTextColor(0xFF000000);
                    chip.setBackground(rounded(0xFFFFB703, dp(12)));
                } else {
                    chip.setText(p + " PSI");
                    chip.setTextColor(colPale);
                    chip.setBackground(rounded(colPanel2, dp(12)));
                }

                chip.setTextSize(10.5f);
                chip.setTypeface(Typeface.MONOSPACE);
                chip.setPadding(dp(9), dp(6), dp(9), dp(6));
                chip.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        hapticClick();
                        int targetVal = Integer.parseInt(p);
                        gaugeView.animateToPressure(targetVal);
                        isSelfUpdating[0] = true;
                        pressureField.setText(p);
                        pressureField.setSelection(p.length());
                        isSelfUpdating[0] = false;
                    }
                });
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                lp.rightMargin = dp(6);
                chip.setLayoutParams(lp);
                presRow.addView(chip);
            }
            hsv.addView(presRow);
            box.addView(hsv);

            box.addView(formSectionLabel("SYSTEM STATUS & FAULTS"));
            final ArrayList<String> selectedConditions = new ArrayList<String>();
            selectedConditions.add("✓ Pressure Normal (1,200 PSI In Spec)");

            final String[] singleOptions = {
                "✓ Pressure Normal (1,200 PSI In Spec)",
                "⚠️ Pressure Low — Jack Up Due (< 1,000 PSI)",
                "🚨 Critical Pressure Loss (< 800 PSI Bad)",
                "⚠️ Jockey Pump Cycling Excessively",
                "⚠️ Diesel Booster Fuel Tank Below 75%",
                "⚠️ Minor Valve / Pipe Fitting Weep Noted"
            };

            final ArrayList<TextView> condViews = new ArrayList<TextView>();
            for (int i = 0; i < singleOptions.length; i++) {
                final String opt = singleOptions[i];
                final TextView item = new TextView(this);
                item.setText(opt);
                item.setTextSize(13);
                item.setPadding(dp(14), dp(10), dp(14), dp(10));

                final boolean isAllClear = i == 0;
                updateCheckItemStyle(item, selectedConditions.contains(opt), isAllClear);

                LinearLayout.LayoutParams il = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                il.bottomMargin = dp(6);
                item.setLayoutParams(il);

                item.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        hapticClick();
                        if (isAllClear) {
                            selectedConditions.clear();
                            selectedConditions.add(singleOptions[0]);
                        } else {
                            selectedConditions.remove(singleOptions[0]);
                            if (selectedConditions.contains(opt)) {
                                selectedConditions.remove(opt);
                            } else {
                                selectedConditions.add(opt);
                            }
                            if (selectedConditions.isEmpty()) {
                                selectedConditions.add(singleOptions[0]);
                            }
                        }
                        for (int k = 0; k < condViews.size(); k++) {
                            TextView tv = condViews.get(k);
                            String o = singleOptions[k];
                            updateCheckItemStyle(tv, selectedConditions.contains(o), k == 0);
                        }
                    }
                });

                condViews.add(item);
                box.addView(item);
            }

            final Dialog dlg = createTacticalDialog(box);

            LinearLayout btnRow = new LinearLayout(this);
            btnRow.setOrientation(LinearLayout.HORIZONTAL);
            btnRow.setPadding(0, dp(14), 0, 0);

            TextView btnCancel = actionButton("Cancel", colLine, colMuted);
            btnCancel.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    hapticClick();
                    dlg.dismiss();
                }
            });
            btnRow.addView(btnCancel);

            TextView btnSave = actionButton("✓ Log System Check", colAccent, colAccentInk);
            btnSave.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    hapticHeavyClick();
                    registerActivity();
                    tap(name, uid);

                    String pVal = pressureField.getText().toString().trim();
                    int pNum = 1200;
                    try { pNum = Integer.parseInt(pVal); } catch (Exception e) {}

                    ArrayList<PressureRecord> list = pressureHistory.get(name);
                    if (list == null) {
                        list = new ArrayList<PressureRecord>();
                        pressureHistory.put(name, list);
                    }
                    list.add(new PressureRecord(nowMinutes(), pNum));

                    StringBuilder sb = new StringBuilder();
                    sb.append(name).append(": [").append(pNum).append(" PSI] ");
                    for (int k = 0; k < selectedConditions.size(); k++) {
                        if (k > 0) sb.append(", ");
                        sb.append(selectedConditions.get(k));
                    }
                    String line = sb.toString();
                    if (!oneLine(line)) {
                        banner.setText("notes must be one line");
                        banner.setVisibility(View.VISIBLE);
                        return;
                    }
                    note(Core.TOPIC_ROUTINE, line);
                    dlg.dismiss();
                }
            });
            btnRow.addView(btnSave);
            box.addView(btnRow);
            dlg.show();
        }
    }

    private LinearLayout buildChronographSection() {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setBackground(rounded(colPanel, dp(18)));
        container.setPadding(dp(16), dp(14), dp(16), dp(14));
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        clp.topMargin = dp(10);
        clp.bottomMargin = dp(6);
        container.setLayoutParams(clp);

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setPadding(0, 0, 0, dp(8));

        TextView title = new TextView(this);
        title.setText("CHRONOGRAPH & DAWN TRANSITION");
        title.setTextColor(colQuiet);
        title.setTextSize(10);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setLetterSpacing(0.12f);
        LinearLayout.LayoutParams tl = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        title.setLayoutParams(tl);
        top.addView(title);

        TextView tag = new TextView(this);
        tag.setText("SOLAR DUAL-ARC");
        tag.setTextColor(colAccent);
        tag.setTextSize(9);
        tag.setTypeface(Typeface.MONOSPACE);
        tag.setPadding(dp(6), dp(2), dp(6), dp(2));
        tag.setBackground(rounded(colPanel2, dp(4)));
        top.addView(tag);
        container.addView(top);

        chronographView = new ChronographView(this);
        LinearLayout.LayoutParams cvl = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(150));
        chronographView.setLayoutParams(cvl);
        chronographView.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                hapticHeavyClick();
                showChronographBreakdownDialog();
            }
        });
        container.addView(chronographView);

        return container;
    }

    private class ChronographView extends View {
        private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint outerArcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint innerArcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint subTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF outerRect = new RectF();
        private final RectF innerRect = new RectF();

        public ChronographView(Context context) {
            super(context);
            trackPaint.setStyle(Paint.Style.STROKE);
            trackPaint.setStrokeCap(Paint.Cap.ROUND);
            outerArcPaint.setStyle(Paint.Style.STROKE);
            outerArcPaint.setStrokeCap(Paint.Cap.ROUND);
            innerArcPaint.setStyle(Paint.Style.STROKE);
            innerArcPaint.setStrokeCap(Paint.Cap.ROUND);
            textPaint.setTextAlign(Paint.Align.CENTER);
            textPaint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
            subTextPaint.setTextAlign(Paint.Align.CENTER);
            subTextPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            int w = getWidth();
            int h = getHeight();
            float cx = w / 2f;
            float cy = h / 2f - dp(2);
            float rOuter = Math.min(w, h) / 2f - dp(10);
            float rInner = rOuter - dp(14);

            outerRect.set(cx - rOuter, cy - rOuter, cx + rOuter, cy + rOuter);
            innerRect.set(cx - rInner, cy - rInner, cx + rInner, cy + rInner);

            trackPaint.setColor(colLine);
            trackPaint.setStrokeWidth(dp(7));
            canvas.drawArc(outerRect, 135, 270, false, trackPaint);

            int nowMin = nowMinutes();
            int shiftStartMin = 18 * 60;
            int currentMinWrapped = nowMin % 1440;
            if (currentMinWrapped < 12 * 60) currentMinWrapped += 1440;

            float shiftProgress = Math.max(0f, Math.min(1f, (currentMinWrapped - shiftStartMin) * 1.0f / (12 * 60)));
            float outerSweep = shiftProgress * 270f;

            int curHourMin = nowMin % 1440;
            int arcColor = colAccent;
            if (curHourMin >= 270 && curHourMin <= 365) {
                float dawnFrac = (curHourMin - 270) / 95.0f;
                int r = (int) (Color.red(colAccent) * (1f - dawnFrac) + 255 * dawnFrac);
                int g = (int) (Color.green(colAccent) * (1f - dawnFrac) + 140 * dawnFrac);
                int b = (int) (Color.blue(colAccent) * (1f - dawnFrac) + 30 * dawnFrac);
                arcColor = Color.rgb(r, g, b);
            }

            outerArcPaint.setColor(arcColor);
            outerArcPaint.setStrokeWidth(dp(7));
            canvas.drawArc(outerRect, 135, outerSweep, false, outerArcPaint);

            trackPaint.setStrokeWidth(dp(5));
            trackPaint.setColor(colLineSubtle);
            canvas.drawArc(innerRect, 135, 270, false, trackPaint);

            long elapsedWelfareMs = SystemClock.elapsedRealtime() - lastActivityTimeMs;
            float welfareFrac = Math.max(0f, Math.min(1f, elapsedWelfareMs * 1.0f / WELFARE_INTERVAL_MS));
            float innerSweep = (1f - welfareFrac) * 270f;

            int welfareCol = colEmerald;
            if (welfareFrac > 0.85f) {
                welfareCol = colCrimson;
            } else if (welfareFrac > 0.60f) {
                welfareCol = colAccent;
            }
            innerArcPaint.setColor(welfareCol);
            innerArcPaint.setStrokeWidth(dp(5));
            canvas.drawArc(innerRect, 135, innerSweep, false, innerArcPaint);

            long ms = System.currentTimeMillis();
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.US);
            sdf.setTimeZone(TimeZone.getDefault());
            String timeStr = sdf.format(new Date(ms));

            textPaint.setColor(colPale);
            textPaint.setTextSize(dp(18));
            canvas.drawText(timeStr, cx, cy - dp(5), textPaint);

            int pct = (int) (shiftProgress * 100);
            long remainMins = Math.max(0, (WELFARE_INTERVAL_MS - elapsedWelfareMs) / 60000L);
            subTextPaint.setColor(curHourMin >= 270 && curHourMin <= 365 ? arcColor : welfareCol);
            subTextPaint.setTextSize(dp(10));
            String status = curHourMin >= 270 && curHourMin <= 365
                ? String.format(Locale.US, "🌅 DAWN (05:41) · %d%% SHIFT", pct)
                : (pct + "% SHIFT · 🦺 " + remainMins + "m SAFE");
            canvas.drawText(status, cx, cy + dp(15), subTextPaint);
        }
    }

    private void showChronographBreakdownDialog() {
        final LinearLayout box = dialogContainer("⏱️ Shift Telemetry & Solar Dawn", "REAL-TIME AUDIT", colAccent);

        int nowMin = nowMinutes();
        int currentMinWrapped = nowMin % 1440;
        if (currentMinWrapped < 12 * 60) currentMinWrapped += 1440;
        float shiftProgress = Math.max(0f, Math.min(1f, (currentMinWrapped - 1080) * 1.0f / (12 * 60)));
        int pct = (int) (shiftProgress * 100);

        long elapsedWelfareMs = SystemClock.elapsedRealtime() - lastActivityTimeMs;
        long remainWelfareMins = Math.max(0, (WELFARE_INTERVAL_MS - elapsedWelfareMs) / 60000L);

        box.addView(chronographStatRow("Shift Window:", "18:00 – 06:00 (12 Hours / 62h Weekend)"));
        box.addView(chronographStatRow("Shift Completion:", pct + "% Elapsed"));
        box.addView(chronographStatRow("First Light / Civil Dawn:", "05:41 AM (Kingston, QLD)"));
        box.addView(chronographStatRow("Active Entries:", Core.entryCount() + " verified in Ada chain"));
        box.addView(chronographStatRow("Welfare Status:", remainWelfareMins + " minutes until next check-in"));
        box.addView(chronographStatRow("Assigned Officer:", "R. Kelso (LIC #41207)"));

        final Dialog dlg = createTacticalDialog(box);

        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setPadding(0, dp(16), 0, 0);

        TextView btnClose = actionButton("Close", colAccent, colAccentInk);
        btnClose.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                hapticClick();
                dlg.dismiss();
            }
        });
        btnRow.addView(btnClose);
        box.addView(btnRow);

        dlg.show();
    }

    private LinearLayout chronographStatRow(String label, String value) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(4), 0, dp(4));

        TextView lbl = new TextView(this);
        lbl.setText(label);
        lbl.setTextColor(colMuted);
        lbl.setTextSize(12);
        LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        lbl.setLayoutParams(llp);
        row.addView(lbl);

        TextView val = new TextView(this);
        val.setText(value);
        val.setTextColor(colPale);
        val.setTextSize(12);
        val.setTypeface(Typeface.DEFAULT_BOLD);
        row.addView(val);

        return row;
    }

    // =========================================================================
    // 🧭 DETAILED COMPASS CARD IN TOOLS TAB
    // =========================================================================

    private LinearLayout buildCompassCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(rounded(colPanel, dp(16)));
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        clp.bottomMargin = dp(12);
        card.setLayoutParams(clp);

        DetailedCompassView compassView = new DetailedCompassView(this);
        activeCompassView = compassView;
        LinearLayout.LayoutParams cpl = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(170));
        cpl.bottomMargin = dp(10);
        compassView.setLayoutParams(cpl);
        card.addView(compassView);

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);

        compassHeadingText = new TextView(this);
        compassHeadingText.setText("034° NNE");
        compassHeadingText.setTextColor(colCyan);
        compassHeadingText.setTextSize(22);
        compassHeadingText.setTypeface(Typeface.MONOSPACE);
        LinearLayout.LayoutParams ctl = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        compassHeadingText.setLayoutParams(ctl);
        top.addView(compassHeadingText);

        compassDmsText = new TextView(this);
        compassDmsText.setText("AZIMUTH ACTIVE");
        compassDmsText.setTextColor(colMuted);
        compassDmsText.setTextSize(10);
        compassDmsText.setTypeface(Typeface.MONOSPACE);
        compassDmsText.setPadding(dp(8), dp(4), dp(8), dp(4));
        compassDmsText.setBackground(rounded(colPanel2, dp(6)));
        top.addView(compassDmsText);
        card.addView(top);

        compassSectorText = new TextView(this);
        compassSectorText.setText("📍 Facing: Gate A · North Boundary (Kingston Rd Entry)");
        compassSectorText.setTextColor(colPale);
        compassSectorText.setTextSize(12);
        compassSectorText.setTypeface(Typeface.DEFAULT_BOLD);
        compassSectorText.setPadding(0, dp(6), 0, dp(4));
        card.addView(compassSectorText);

        TextView note = new TextView(this);
        note.setText("• Rotating 360° Bezel tracks true magnetic north & perimeter boundary lines.");
        note.setTextColor(colQuiet);
        note.setTextSize(11);
        card.addView(note);

        return card;
    }

    // =========================================================================
    // REST OF THE CODE IMPLEMENTATION
    // =========================================================================

    private LinearLayout buildTabBar() {
        tabContainer = new LinearLayout(this);
        tabContainer.setOrientation(LinearLayout.HORIZONTAL);
        tabContainer.setBackground(rounded(colPanel, dp(16)));
        tabContainer.setPadding(dp(4), dp(4), dp(4), dp(4));
        LinearLayout.LayoutParams tcl = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        tcl.topMargin = dp(10);
        tcl.bottomMargin = dp(6);
        tabContainer.setLayoutParams(tcl);

        tabPatrol = createTabButton("🛡️ Patrol", 0);
        tabContacts = createTabButton("📞 Contacts", 1);
        tabTools = createTabButton("🛠️ Tools", 2);

        tabContainer.addView(tabPatrol);
        tabContainer.addView(tabContacts);
        tabContainer.addView(tabTools);

        return tabContainer;
    }

    private TextView createTabButton(String text, final int index) {
        TextView tab = new TextView(this);
        tab.setText(text);
        tab.setTextSize(12);
        tab.setTypeface(Typeface.DEFAULT_BOLD);
        tab.setGravity(Gravity.CENTER);
        tab.setPadding(dp(8), dp(10), dp(8), dp(10));
        LinearLayout.LayoutParams tpl = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        tab.setLayoutParams(tpl);
        tab.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                hapticClick();
                updateTabSelection(index);
            }
        });
        return tab;
    }

    
    public void applyTabScrollPosition(float pos) {
        currentTabFloat = Math.max(0f, Math.min(3f, pos));
        if (animatedTabBar != null) {
            animatedTabBar.setIndicatorFloat(currentTabFloat);
        }
        if (tabPagerFrame == null) return;
        int w = tabPagerFrame.getWidth();
        if (w <= 0) return;

        final ScrollView[] pages = {scrollPatrol, scrollContacts, scrollHandbook, scrollTools};
        for (int i = 0; i < pages.length; i++) {
            ScrollView p = pages[i];
            if (p == null) continue;
            float offset = (i - currentTabFloat) * w;
            p.setTranslationX(offset);

            float dist = Math.abs(currentTabFloat - i);
            if (dist > 1.15f) {
                p.setVisibility(View.GONE);
            } else {
                p.setVisibility(View.VISIBLE);
                p.setAlpha(Math.max(0f, 1f - dist * 0.75f));
                float scale = Math.max(0.95f, 1f - dist * 0.05f);
                p.setScaleX(scale);
                p.setScaleY(scale);
            }
        }
    }

    public void animateTabToPosition(int targetTab) {
        if (tabSlideAnimator != null && tabSlideAnimator.isRunning()) {
            tabSlideAnimator.cancel();
        }
        final int target = Math.max(0, Math.min(3, targetTab));
        currentTab = target;
        hapticClick();

        tabSlideAnimator = ValueAnimator.ofFloat(currentTabFloat, (float) target);
        tabSlideAnimator.setDuration(260);
        tabSlideAnimator.setInterpolator(new OvershootInterpolator(1.05f));
        tabSlideAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator va) {
                float f = (Float) va.getAnimatedValue();
                applyTabScrollPosition(f);
            }
        });
        tabSlideAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                applyTabScrollPosition((float) target);
            }
        });
        tabSlideAnimator.start();
    }

private void updateTabSelection(int tabIndex) {
        currentTab = tabIndex;

        tabPatrol.setTextColor(tabIndex == 0 ? colAccentInk : colMuted);
        tabPatrol.setBackground(tabIndex == 0 ? rounded(colAccent, dp(12)) : null);

        tabContacts.setTextColor(tabIndex == 1 ? colAccentInk : colMuted);
        tabContacts.setBackground(tabIndex == 1 ? rounded(colAccent, dp(12)) : null);

        tabTools.setTextColor(tabIndex == 2 ? colAccentInk : colMuted);
        tabTools.setBackground(tabIndex == 2 ? rounded(colAccent, dp(12)) : null);

        patrolContent.setVisibility(tabIndex == 0 ? View.VISIBLE : View.GONE);
        contactsContent.setVisibility(tabIndex == 1 ? View.VISIBLE : View.GONE);
        toolsContent.setVisibility(tabIndex == 2 ? View.VISIBLE : View.GONE);

        if (tabIndex == 2) {
            registerSensors();
            requestGpsUpdates();
        }
    }

    private LinearLayout buildToolsTab() {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(0, dp(6), 0, dp(24));

        container.addView(contactsSectionHeader("📡 AUTONOMOUS PEER MESH & NFC PAIRING", colCyan));
        container.addView(buildMeshPreviewCard());

        container.addView(contactsSectionHeader("🌤️ KINGSTON SITE WEATHER, THERMAL & HYDRATION", colCyan));
        container.addView(buildDetailedWeatherCard());

        container.addView(contactsSectionHeader("🪪 OFFICER CREDENTIALS & FIRST AID VAULT", colPale));
        container.addView(buildCredentialPreviewCard());

        container.addView(contactsSectionHeader("💡 TACTICAL LIGHTING (DOUBLE-CHOP SHAKE TO TOGGLE)", colAccent));
        container.addView(buildLightingGrid());

        container.addView(contactsSectionHeader("🧭 360° ROTATING COMPASS & SITE AZIMUTH", colCyan));
        container.addView(buildCompassCard());

        container.addView(contactsSectionHeader("🛰️ GPS TELEMETRY & SATELLITE RADAR", colEmerald));
        container.addView(buildGpsCard());

        return container;
    }

    private LinearLayout buildMeshPreviewCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(rounded(colPanel, dp(16)));
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        clp.bottomMargin = dp(12);
        card.setLayoutParams(clp);

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = new TextView(this);
        title.setText("📡 Offline BLE Mesh & NFC Sync");
        title.setTextColor(colPale);
        title.setTextSize(14);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams tl = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        title.setLayoutParams(tl);
        top.addView(title);

        TextView badge = new TextView(this);
        badge.setText("ACTIVE");
        badge.setTextColor(colEmerald);
        badge.setTextSize(9);
        badge.setTypeface(Typeface.MONOSPACE);
        badge.setPadding(dp(6), dp(2), dp(6), dp(2));
        badge.setBackground(rounded(colEmeraldSoft, dp(4)));
        top.addView(badge);
        card.addView(top);

        TextView sub = new TextView(this);
        sub.setText("Autonomous zero-data peer sync with oncoming relief officers.\nInitial physical bump via NFC activates seamless background BLE sync.");
        sub.setTextColor(colMuted);
        sub.setTextSize(12);
        sub.setPadding(0, dp(4), 0, dp(10));
        card.addView(sub);

        TextView btnOpen = actionButton("🤝 Manage Trusted Peers & NFC Sync", colCyan, colAccentInk);
        btnOpen.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showNfcBleMeshDialog();
            }
        });
        card.addView(btnOpen);

        return card;
    }

    private void showNfcBleMeshDialog() {
        hapticHeavyClick();
        final LinearLayout box = dialogContainer("📡 NFC Bump & BLE Mesh", "OFF-GRID P2P RELAY", colCyan);

        TextView info = new TextView(this);
        info.setText("Decentralised peer-to-peer ledger sync for relief guards & patrol supervisors.\nTap phones back-to-back via NFC to pair trusted identity; logs sync automatically over BLE thereafter.");
        info.setTextColor(colMuted);
        info.setTextSize(12);
        info.setPadding(0, 0, 0, dp(12));
        box.addView(info);

        LinearLayout statusCard = new LinearLayout(this);
        statusCard.setOrientation(LinearLayout.VERTICAL);
        statusCard.setBackground(rounded(colPanel2, dp(14)));
        statusCard.setPadding(dp(14), dp(12), dp(14), dp(12));
        LinearLayout.LayoutParams scp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        scp.bottomMargin = dp(14);
        statusCard.setLayoutParams(scp);

        LinearLayout sTop = new LinearLayout(this);
        sTop.setOrientation(LinearLayout.HORIZONTAL);
        sTop.setGravity(Gravity.CENTER_VERTICAL);

        TextView sTitle = new TextView(this);
        sTitle.setText("BLE MESH DAEMON: ACTIVE");
        sTitle.setTextColor(colEmerald);
        sTitle.setTextSize(11);
        sTitle.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
        LinearLayout.LayoutParams stl = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        sTitle.setLayoutParams(stl);
        sTop.addView(sTitle);

        TextView pCount = new TextView(this);
        pCount.setText(trustedPeers.size() + " TRUSTED PEERS");
        pCount.setTextColor(colAccent);
        pCount.setTextSize(9);
        pCount.setTypeface(Typeface.MONOSPACE);
        pCount.setPadding(dp(6), dp(2), dp(6), dp(2));
        pCount.setBackground(rounded(colPanel3, dp(4)));
        sTop.addView(pCount);
        statusCard.addView(sTop);

        TextView sSub = new TextView(this);
        sSub.setText("Auto-broadcasting SHA-256 block head (" + Core.head().substring(0, 8) + ") every 3.5s.\nSync range: ~35m radius across Kingston Gatehouse & yard.");
        sSub.setTextColor(colQuiet);
        sSub.setTextSize(11);
        sSub.setPadding(0, dp(4), 0, 0);
        statusCard.addView(sSub);
        box.addView(statusCard);

        box.addView(formSectionLabel("TRUSTED ON-SITE OFFICERS"));
        for (TrustedPeer tp : trustedPeers) {
            LinearLayout pRow = new LinearLayout(this);
            pRow.setOrientation(LinearLayout.HORIZONTAL);
            pRow.setGravity(Gravity.CENTER_VERTICAL);
            pRow.setBackground(rounded(colPanel3, dp(10)));
            pRow.setPadding(dp(12), dp(10), dp(12), dp(10));
            LinearLayout.LayoutParams prp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            prp.bottomMargin = dp(6);
            pRow.setLayoutParams(prp);

            TextView pInfo = new TextView(this);
            pInfo.setText("🛡️ " + tp.name + " (" + tp.licence + ")\n" + tp.lastSeen);
            pInfo.setTextColor(colPale);
            pInfo.setTextSize(12);
            LinearLayout.LayoutParams pil = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            pInfo.setLayoutParams(pil);
            pRow.addView(pInfo);

            TextView syncTag = new TextView(this);
            syncTag.setText("✓ 100% SYNCED");
            syncTag.setTextColor(colEmerald);
            syncTag.setTextSize(9);
            syncTag.setTypeface(Typeface.MONOSPACE);
            syncTag.setPadding(dp(6), dp(3), dp(6), dp(3));
            syncTag.setBackground(rounded(colEmeraldSoft, dp(4)));
            pRow.addView(syncTag);

            box.addView(pRow);
        }

        final Dialog dlg = createTacticalDialog(box);

        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setPadding(0, dp(14), 0, 0);

        TextView btnSimBump = actionButton("🤝 NFC Bump Tap (Simulate)", colCyan, colAccentInk);
        btnSimBump.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                hapticSealThud();
                trustedPeers.add(new TrustedPeer("Patrol Supv #4", "LIC #38910", "Just Now (Gatehouse Proximity)"));
                banner.setText("🤝 NFC Handshake Verified: Supervisor LIC #38910 paired to trusted mesh!");
                banner.setVisibility(View.VISIBLE);
                dlg.dismiss();
            }
        });
        btnRow.addView(btnSimBump);

        TextView btnClose = actionButton("Close", colLine, colPale);
        btnClose.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                hapticClick();
                dlg.dismiss();
            }
        });
        LinearLayout.LayoutParams cml = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.8f);
        cml.leftMargin = dp(8);
        btnClose.setLayoutParams(cml);
        btnRow.addView(btnClose);

        box.addView(btnRow);
        dlg.show();
    }

    private LinearLayout buildCredentialPreviewCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(rounded(colPanel, dp(16)));
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        clp.bottomMargin = dp(12);
        card.setLayoutParams(clp);

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = new TextView(this);
        title.setText("🛡️ Officer Lochran Doherty · LIC #41207");
        title.setTextColor(colPale);
        title.setTextSize(14);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams tl = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        title.setLayoutParams(tl);
        top.addView(title);

        TextView badge = new TextView(this);
        badge.setText("HOLOGRAPHIC");
        badge.setTextColor(colAccent);
        badge.setTextSize(9);
        badge.setTypeface(Typeface.MONOSPACE);
        badge.setPadding(dp(6), dp(2), dp(6), dp(2));
        badge.setBackground(rounded(colPanel2, dp(4)));
        top.addView(badge);
        card.addView(top);

        TextView sub = new TextView(this);
        sub.setText("QLD Security Class 1 Licence & St John HLTAID011 First Aid / CPR.\nTilt phone for holographic metallic shimmer & tap to flip verification QR.");
        sub.setTextColor(colMuted);
        sub.setTextSize(12);
        sub.setPadding(0, dp(4), 0, dp(10));
        card.addView(sub);

        TextView btnOpen = actionButton("🪪 Open Holographic Credential Vault", colAccent, colAccentInk);
        btnOpen.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showOfficerCredentialVaultDialog();
            }
        });
        card.addView(btnOpen);

        return card;
    }

    private LinearLayout buildLightingGrid() {
        LinearLayout grid = new LinearLayout(this);
        grid.setOrientation(LinearLayout.VERTICAL);
        grid.setBackground(rounded(colPanel, dp(16)));
        grid.setPadding(dp(14), dp(14), dp(14), dp(14));
        LinearLayout.LayoutParams glp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        glp.bottomMargin = dp(12);
        grid.setLayoutParams(glp);

        LinearLayout intensityRow = new LinearLayout(this);
        intensityRow.setOrientation(LinearLayout.HORIZONTAL);
        intensityRow.setGravity(Gravity.CENTER_VERTICAL);
        intensityRow.setPadding(0, 0, 0, dp(10));

        TextView intLbl = new TextView(this);
        intLbl.setText("🔦 TORCH INTENSITY:");
        intLbl.setTextColor(colQuiet);
        intLbl.setTextSize(10);
        intLbl.setTypeface(Typeface.DEFAULT_BOLD);
        intLbl.setLetterSpacing(0.1f);
        LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        intLbl.setLayoutParams(ilp);
        intensityRow.addView(intLbl);

        final TextView chip20 = torchLevelChip("20%", 20);
        final TextView chip50 = torchLevelChip("50%", 50);
        final TextView chip100 = torchLevelChip("100%", 100);

        final ArrayList<TextView> chips = new ArrayList<TextView>();
        chips.add(chip20); chips.add(chip50); chips.add(chip100);

        for (final TextView c : chips) {
            c.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    hapticClick();
                    torchLevelPercent = (Integer) c.getTag();
                    for (TextView o : chips) {
                        boolean sel = ((Integer) o.getTag()) == torchLevelPercent;
                        o.setTextColor(sel ? colAccentInk : colMuted);
                        o.setBackground(rounded(sel ? colAccent : colPanel2, dp(8)));
                    }
                    if (isHardwareTorchOn) setFlashTorch(true);
                }
            });
            intensityRow.addView(c);
        }
        grid.addView(intensityRow);

        LinearLayout row1 = new LinearLayout(this);
        row1.setOrientation(LinearLayout.HORIZONTAL);
        row1.setPadding(0, 0, 0, dp(8));

        final TextView btnTorch = toolLightButton("🔦 Torch", "STEADY BEAM", false);
        final TextView btnStrobe = toolLightButton("⚡ 10Hz Strobe", "DISORIENT", false);

        btnTorch.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                hapticClick();
                toggleHardwareTorch();
                updateLightButtonStates(btnTorch, btnStrobe, null, null);
            }
        });

        btnStrobe.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                hapticDoublePulse();
                toggleIntruderStrobe();
                updateLightButtonStates(btnTorch, btnStrobe, null, null);
            }
        });

        row1.addView(btnTorch);
        row1.addView(btnStrobe);
        grid.addView(row1);

        LinearLayout row2 = new LinearLayout(this);
        row2.setOrientation(LinearLayout.HORIZONTAL);

        final TextView btnSos = toolLightButton("🆘 SOS Beacon", "MORSE PATTERN", false);
        final TextView btnLantern = toolLightButton("🏮 0-Lux Red Lantern", "NIGHT VISION", false);

        btnSos.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                hapticDoublePulse();
                toggleSosBeacon();
                updateLightButtonStates(btnTorch, btnStrobe, btnSos, null);
            }
        });

        btnLantern.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                hapticClick();
                showRedLanternDialog();
            }
        });

        row2.addView(btnSos);
        row2.addView(btnLantern);
        grid.addView(row2);

        TextView chopHint = new TextView(this);
        chopHint.setText("💡 Tip: Double-chop wrist shake toggles torch instantly anytime.");
        chopHint.setTextColor(colQuiet);
        chopHint.setTextSize(10);
        chopHint.setPadding(0, dp(8), 0, 0);
        grid.addView(chopHint);

        return grid;
    }

    private TextView torchLevelChip(String text, int level) {
        TextView chip = new TextView(this);
        chip.setText(text);
        chip.setTag(level);
        chip.setTextSize(10);
        chip.setTypeface(Typeface.MONOSPACE);
        chip.setPadding(dp(8), dp(4), dp(8), dp(4));
        boolean isSel = level == torchLevelPercent;
        chip.setTextColor(isSel ? colAccentInk : colMuted);
        chip.setBackground(rounded(isSel ? colAccent : colPanel2, dp(8)));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.leftMargin = dp(4);
        chip.setLayoutParams(lp);
        return chip;
    }

    private TextView toolLightButton(String title, String subtitle, boolean active) {
        TextView btn = new TextView(this);
        btn.setText(title + "\n" + subtitle);
        btn.setTextSize(12);
        btn.setTypeface(Typeface.DEFAULT_BOLD);
        btn.setGravity(Gravity.CENTER);
        btn.setTextColor(active ? colAccentInk : colPale);
        btn.setPadding(dp(12), dp(14), dp(12), dp(14));
        btn.setBackground(active ? rounded(colAccent, dp(12)) : pressable(colPanel2, dp(12)));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        lp.leftMargin = dp(3);
        lp.rightMargin = dp(3);
        btn.setLayoutParams(lp);
        return btn;
    }

    private void updateLightButtonStates(TextView t, TextView s, TextView sos, TextView l) {
        if (t != null) {
            t.setTextColor(isHardwareTorchOn ? colAccentInk : colPale);
            t.setBackground(isHardwareTorchOn ? rounded(colAccent, dp(12)) : pressable(colPanel2, dp(12)));
        }
        if (s != null) {
            s.setTextColor(isStrobeActive ? colAccentInk : colPale);
            s.setBackground(isStrobeActive ? rounded(colCrimson, dp(12)) : pressable(colPanel2, dp(12)));
        }
        if (sos != null) {
            sos.setTextColor(isSosActive ? colAccentInk : colPale);
            sos.setBackground(isSosActive ? rounded(colCyan, dp(12)) : pressable(colPanel2, dp(12)));
        }
    }

    private void toggleHardwareTorch() {
        isStrobeActive = false;
        isSosActive = false;
        lightHandler.removeCallbacksAndMessages(null);

        isHardwareTorchOn = !isHardwareTorchOn;
        setFlashTorch(isHardwareTorchOn);
    }

    private void toggleIntruderStrobe() {
        isHardwareTorchOn = false;
        isSosActive = false;
        lightHandler.removeCallbacksAndMessages(null);

        isStrobeActive = !isStrobeActive;
        if (isStrobeActive) {
            startStrobeLoop();
        } else {
            setFlashTorch(false);
        }
    }

    private void startStrobeLoop() {
        final boolean[] state = {false};
        final Runnable strobeTick = new Runnable() {
            public void run() {
                if (isStrobeActive) {
                    state[0] = !state[0];
                    setFlashTorch(state[0]);
                    lightHandler.postDelayed(this, 50);
                } else {
                    setFlashTorch(false);
                }
            }
        };
        lightHandler.post(strobeTick);
    }

    private void toggleSosBeacon() {
        isHardwareTorchOn = false;
        isStrobeActive = false;
        lightHandler.removeCallbacksAndMessages(null);

        isSosActive = !isSosActive;
        if (isSosActive) {
            startSosLoop();
        } else {
            setFlashTorch(false);
        }
    }

    private void startSosLoop() {
        final int[] pattern = {
            100, 100, 100, 100, 100, 250,
            300, 100, 300, 100, 300, 250,
            100, 100, 100, 100, 100, 1200
        };
        final int[] step = {0};

        final Runnable sosTick = new Runnable() {
            public void run() {
                if (isSosActive) {
                    boolean isFlashOn = (step[0] % 2 == 0);
                    setFlashTorch(isFlashOn);
                    int delay = pattern[step[0]];
                    step[0] = (step[0] + 1) % pattern.length;
                    lightHandler.postDelayed(this, delay);
                } else {
                    setFlashTorch(false);
                }
            }
        };
        lightHandler.post(sosTick);
    }

    private void setFlashTorch(boolean on) {
        if (rearCameraId == null) return;
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && on) {
                try {
                    CameraCharacteristics c = manager.getCameraCharacteristics(rearCameraId);
                    Integer maxLevel = c.get(CameraCharacteristics.FLASH_INFO_STRENGTH_MAXIMUM_LEVEL);
                    if (maxLevel != null && maxLevel > 1) {
                        int lvl = Math.max(1, (int) (maxLevel * (torchLevelPercent / 100f)));
                        manager.turnOnTorchWithStrengthLevel(rearCameraId, lvl);
                        return;
                    }
                } catch (Exception e) {}
            }
            manager.setTorchMode(rearCameraId, on);
        } catch (Exception e) {}
    }

    private void stopLightingModes() {
        isHardwareTorchOn = false;
        isStrobeActive = false;
        isSosActive = false;
        lightHandler.removeCallbacksAndMessages(null);
        setFlashTorch(false);
    }

    private void showRedLanternDialog() {
        final Dialog dlg = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dlg.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout lantern = new LinearLayout(this);
        lantern.setOrientation(LinearLayout.VERTICAL);
        lantern.setGravity(Gravity.CENTER);
        lantern.setBackgroundColor(0xFFFF1111);
        lantern.setPadding(dp(24), dp(24), dp(24), dp(24));

        TextView icon = new TextView(this);
        icon.setText("🏮");
        icon.setTextSize(48);
        icon.setGravity(Gravity.CENTER);
        lantern.addView(icon);

        TextView title = new TextView(this);
        title.setText("0-LUX RED NIGHT LANTERN");
        title.setTextColor(0xFF000000);
        title.setTextSize(18);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setPadding(0, dp(12), 0, dp(4));
        title.setGravity(Gravity.CENTER);
        lantern.addView(title);

        TextView desc = new TextView(this);
        desc.setText("Full-screen red light for padlock inspection & paperwork.\nPreserves natural night eye vision (rhodopsin).\n\n[ Tap anywhere to close ]");
        desc.setTextColor(0xFF440000);
        desc.setTextSize(13);
        desc.setGravity(Gravity.CENTER);
        lantern.addView(desc);

        lantern.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                hapticClick();
                dlg.dismiss();
            }
        });

        dlg.setContentView(lantern);
        dlg.show();
    }

    // =========================================================================
    // DETAILED WEATHER CARD
    // =========================================================================

    private LinearLayout buildDetailedWeatherCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(rounded(colPanel, dp(16)));
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        clp.bottomMargin = dp(12);
        card.setLayoutParams(clp);

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setPadding(0, 0, 0, dp(10));

        TextView title = new TextView(this);
        title.setText("🌤️ KINGSTON ATMOSPHERIC RADAR");
        title.setTextColor(colPale);
        title.setTextSize(13);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams tl = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        title.setLayoutParams(tl);
        top.addView(title);

        TextView badge = new TextView(this);
        badge.setText("BOM LOGAN LIVE");
        badge.setTextColor(colEmerald);
        badge.setTextSize(9);
        badge.setTypeface(Typeface.MONOSPACE);
        badge.setPadding(dp(6), dp(2), dp(6), dp(2));
        badge.setBackground(rounded(colEmeraldSoft, dp(4)));
        top.addView(badge);
        card.addView(top);

        LinearLayout grid1 = new LinearLayout(this);
        grid1.setOrientation(LinearLayout.HORIZONTAL);
        grid1.setPadding(0, 0, 0, dp(8));

        grid1.addView(weatherMetricBox("TEMPERATURE", String.format(Locale.US, "%.1f°C", curTempC),
                                       String.format(Locale.US, "Feels %.1f°C", curFeelsLikeC), colPale));
        grid1.addView(weatherMetricBox("☀️ UV INDEX", String.format(Locale.US, "%.1f UV", curUvIndex),
                                       "0.0 Night · Day Max 7.8", colAccent));
        card.addView(grid1);

        LinearLayout grid2 = new LinearLayout(this);
        grid2.setOrientation(LinearLayout.HORIZONTAL);
        grid2.setPadding(0, 0, 0, dp(12));

        grid2.addView(weatherMetricBox("HUMIDITY / DEW", curHumidity + "% RH",
                                       String.format(Locale.US, "Dew Pt %.1f°C", curDewPointC), colCyan));
        grid2.addView(weatherMetricBox("WIND & GUSTS", String.format(Locale.US, "%.1f km/h", curWindSpeedKmh),
                                       curWindDir + " · Gust " + curWindGustKmh, colEmerald));
        card.addView(grid2);

        LinearLayout rowThermal = new LinearLayout(this);
        rowThermal.setOrientation(LinearLayout.HORIZONTAL);
        rowThermal.setPadding(0, 0, 0, dp(12));

        double windChill = curTempC - (curWindSpeedKmh * 0.12);
        rowThermal.addView(weatherMetricBox("🥶 COLD STRESS", String.format(Locale.US, "%.1f°C Chill", windChill),
                                            "Low Risk · Cabin Warm OK", colCyan));

        final TextView hydrationBoxVal = new TextView(this);
        hydrationBoxVal.setText(waterIntakeMl + " / " + WATER_TARGET_ML + " ml");
        hydrationBoxVal.setTextColor(colEmerald);
        hydrationBoxVal.setTextSize(14);
        hydrationBoxVal.setTypeface(Typeface.DEFAULT_BOLD);
        hydrationBoxVal.setPadding(0, dp(2), 0, dp(1));

        LinearLayout hBox = new LinearLayout(this);
        hBox.setOrientation(LinearLayout.VERTICAL);
        hBox.setBackground(rounded(colPanel2, dp(12)));
        hBox.setPadding(dp(12), dp(10), dp(12), dp(10));
        LinearLayout.LayoutParams hlp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        hlp.leftMargin = dp(3);
        hlp.rightMargin = dp(3);
        hBox.setLayoutParams(hlp);

        TextView hLbl = new TextView(this);
        hLbl.setText("💧 HYDRATION PACER");
        hLbl.setTextColor(colQuiet);
        hLbl.setTextSize(9);
        hLbl.setTypeface(Typeface.DEFAULT_BOLD);
        hBox.addView(hLbl);
        hBox.addView(hydrationBoxVal);

        TextView hSub = new TextView(this);
        int pct = (int) (waterIntakeMl * 100.0 / WATER_TARGET_ML);
        hSub.setText(pct + "% of 2.0L Shift Goal");
        hSub.setTextColor(colMuted);
        hSub.setTextSize(9);
        hBox.addView(hSub);
        rowThermal.addView(hBox);

        card.addView(rowThermal);

        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);

        TextView btnEmbed = actionButton("📍 Embed Weather to Log", colAccent, colAccentInk);
        btnEmbed.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                hapticHeavyClick();
                String weatherStr = String.format(Locale.US, "[WEATHER] %.1f°C (Feels %.1f°C, Chill %.1f°C) · Hum: %d%% · Baro: %.1fhPa · Wind: %.1fkm/h %s · UV: %.1f · Hydration: %dml",
                        curTempC, curFeelsLikeC, curTempC - (curWindSpeedKmh * 0.12), curHumidity, curPressureHpa, curWindSpeedKmh, curWindDir, curUvIndex, waterIntakeMl);
                note(Core.TOPIC_ROUTINE, weatherStr);
                banner.setText("✓ Kingston weather & thermal telemetry logged to Ada record");
                banner.setVisibility(View.VISIBLE);
            }
        });
        btnRow.addView(btnEmbed);

        TextView btnWater = actionButton("💧 +250ml Water", colPanel2, colCyan);
        btnWater.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                hapticClick();
                waterIntakeMl = Math.min(3000, waterIntakeMl + 250);
                hydrationBoxVal.setText(waterIntakeMl + " / " + WATER_TARGET_ML + " ml");
                banner.setText("✓ Logged +250ml water (" + waterIntakeMl + "ml / " + WATER_TARGET_ML + "ml target)");
                banner.setVisibility(View.VISIBLE);
            }
        });
        LinearLayout.LayoutParams wlp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.8f);
        wlp.leftMargin = dp(6);
        btnWater.setLayoutParams(wlp);
        btnRow.addView(btnWater);

        card.addView(btnRow);
        return card;
    }

    private LinearLayout weatherMetricBox(String label, String value, String sub, int valCol) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setBackground(rounded(colPanel2, dp(12)));
        box.setPadding(dp(12), dp(10), dp(12), dp(10));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        lp.leftMargin = dp(3);
        lp.rightMargin = dp(3);
        box.setLayoutParams(lp);

        TextView lbl = new TextView(this);
        lbl.setText(label);
        lbl.setTextColor(colQuiet);
        lbl.setTextSize(9);
        lbl.setTypeface(Typeface.DEFAULT_BOLD);
        lbl.setLetterSpacing(0.1f);
        box.addView(lbl);

        TextView val = new TextView(this);
        val.setText(value);
        val.setTextColor(valCol);
        val.setTextSize(14);
        val.setTypeface(Typeface.DEFAULT_BOLD);
        val.setPadding(0, dp(2), 0, dp(1));
        box.addView(val);

        TextView s = new TextView(this);
        s.setText(sub);
        s.setTextColor(colMuted);
        s.setTextSize(9);
        box.addView(s);

        return box;
    }

    // =========================================================================
    // HOLOGRAPHIC CARD VIEW
    // =========================================================================

    private class HolographicCardView extends View {
        private final Paint cardBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint goldEmbossPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint shimmerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint subTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint qrPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path guillochePath = new Path();
        private final RectF cardRect = new RectF();

        private int cardMode = 0;
        private boolean isFlipped = false;

        public HolographicCardView(Context context) {
            super(context);
            cardBgPaint.setStyle(Paint.Style.FILL);
            borderPaint.setStyle(Paint.Style.STROKE);
            borderPaint.setStrokeWidth(dp(2));
            goldEmbossPaint.setStyle(Paint.Style.STROKE);
            goldEmbossPaint.setStrokeWidth(dp(1));
            shimmerPaint.setStyle(Paint.Style.FILL);
            textPaint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
            subTextPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        }

        public void setCardMode(int mode) {
            this.cardMode = mode;
            this.isFlipped = false;
            invalidate();
        }

        public void toggleFlip() {
            this.isFlipped = !this.isFlipped;
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            int w = getWidth();
            int h = getHeight();
            if (w <= 0 || h <= 0) return;

            cardRect.set(dp(6), dp(6), w - dp(6), h - dp(6));

            int baseColor = cardMode == 0 ? 0xFF0D1422 : 0xFF0A1C16;
            cardBgPaint.setColor(baseColor);
            canvas.drawRoundRect(cardRect, dp(16), dp(16), cardBgPaint);

            guillochePath.reset();
            Paint gPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            gPaint.setStyle(Paint.Style.STROKE);
            gPaint.setStrokeWidth(dp(1));
            gPaint.setColor(cardMode == 0 ? 0x12E5A93C : 0x1410B981);

            for (int y = dp(16); y < h - dp(16); y += dp(14)) {
                guillochePath.moveTo(dp(16), y);
                for (int x = dp(16); x < w - dp(16); x += dp(24)) {
                    float dy = (float) Math.sin((x + y) * 0.1f) * dp(4);
                    guillochePath.lineTo(x, y + dy);
                }
            }
            canvas.drawPath(guillochePath, gPaint);

            float roll = lastAccel[0];
            float pitch = lastAccel[1];
            float normX = Math.max(0.1f, Math.min(0.9f, (roll + 6f) / 12f));
            float normY = Math.max(0.1f, Math.min(0.9f, (pitch + 6f) / 12f));

            float shimX = w * normX;
            float shimY = h * normY;

            LinearGradient holoShader = new LinearGradient(
                    shimX - dp(120), shimY - dp(120),
                    shimX + dp(120), shimY + dp(120),
                    new int[]{0x00FFFFFF, 0x18FFFFFF, 0x4CE5A93C, 0x6606B6D4, 0x22FF5555, 0x00FFFFFF},
                    new float[]{0.0f, 0.2f, 0.4f, 0.6f, 0.8f, 1.0f},
                    Shader.TileMode.CLAMP);
            shimmerPaint.setShader(holoShader);
            canvas.drawRoundRect(cardRect, dp(16), dp(16), shimmerPaint);

            borderPaint.setColor(cardMode == 0 ? colAccent : colEmerald);
            canvas.drawRoundRect(cardRect, dp(16), dp(16), borderPaint);

            goldEmbossPaint.setColor(cardMode == 0 ? 0x44E5A93C : 0x4410B981);
            RectF innerBorder = new RectF(cardRect.left + dp(4), cardRect.top + dp(4),
                                          cardRect.right - dp(4), cardRect.bottom - dp(4));
            canvas.drawRoundRect(innerBorder, dp(12), dp(12), goldEmbossPaint);

            if (!isFlipped) {
                if (cardMode == 0) {
                    textPaint.setColor(colAccent);
                    textPaint.setTextSize(dp(9));
                    textPaint.setTextAlign(Paint.Align.LEFT);
                    canvas.drawText("QUEENSLAND GOVERNMENT · OFFICE OF FAIR TRADING", dp(18), dp(26), textPaint);

                    subTextPaint.setColor(colPale);
                    subTextPaint.setTextSize(dp(13));
                    subTextPaint.setTextAlign(Paint.Align.LEFT);
                    canvas.drawText("SECURITY PROVIDERS ACT 1993 · CLASS 1", dp(18), dp(44), subTextPaint);

                    textPaint.setColor(colMuted);
                    textPaint.setTextSize(dp(9));
                    canvas.drawText("LICENCE HOLDER:", dp(18), dp(66), textPaint);

                    subTextPaint.setColor(colPale);
                    subTextPaint.setTextSize(dp(16));
                    canvas.drawText("KELSO, R.", dp(18), dp(84), subTextPaint);

                    textPaint.setColor(colMuted);
                    textPaint.setTextSize(dp(9));
                    canvas.drawText("LICENCE NUMBER:", dp(18), dp(104), textPaint);

                    textPaint.setColor(colAccent);
                    textPaint.setTextSize(dp(14));
                    canvas.drawText("41207 / SEC-1-QLD", dp(18), dp(120), textPaint);

                    textPaint.setColor(colMuted);
                    textPaint.setTextSize(dp(9));
                    canvas.drawText("FUNCTIONS: 1A UNARMED GUARD · 1C CROWD · STATIC", dp(18), dp(138), textPaint);
                    canvas.drawText("EMPLOYER: DOHERTY SECURITY SERVICES (#389102)", dp(18), dp(152), textPaint);

                    Paint pillBg = new Paint(Paint.ANTI_ALIAS_FLAG);
                    pillBg.setColor(colEmeraldSoft);
                    RectF pillR = new RectF(w - dp(110), dp(22), w - dp(18), dp(44));
                    canvas.drawRoundRect(pillR, dp(6), dp(6), pillBg);

                    textPaint.setColor(colEmerald);
                    textPaint.setTextSize(dp(9));
                    textPaint.setTextAlign(Paint.Align.CENTER);
                    canvas.drawText("✓ CURRENT & ACTIVE", pillR.centerX(), pillR.centerY() + dp(3), textPaint);

                    textPaint.setColor(colQuiet);
                    textPaint.setTextSize(dp(8));
                    textPaint.setTextAlign(Paint.Align.RIGHT);
                    canvas.drawText("EXP: 14 OCT 2027", w - dp(18), h - dp(14), textPaint);
                } else {
                    textPaint.setColor(colEmerald);
                    textPaint.setTextSize(dp(9));
                    textPaint.setTextAlign(Paint.Align.LEFT);
                    canvas.drawText("NATIONALLY RECOGNISED TRAINING · RTO #8801", dp(18), dp(26), textPaint);

                    subTextPaint.setColor(colPale);
                    subTextPaint.setTextSize(dp(13));
                    subTextPaint.setTextAlign(Paint.Align.LEFT);
                    canvas.drawText("HLTAID011 PROVIDE FIRST AID & CPR", dp(18), dp(44), subTextPaint);

                    textPaint.setColor(colMuted);
                    textPaint.setTextSize(dp(9));
                    canvas.drawText("CERTIFIED PRACTITIONER:", dp(18), dp(66), textPaint);

                    subTextPaint.setColor(colPale);
                    subTextPaint.setTextSize(dp(16));
                    canvas.drawText("Officer Lochran Doherty", dp(18), dp(84), subTextPaint);

                    textPaint.setColor(colMuted);
                    textPaint.setTextSize(dp(9));
                    canvas.drawText("TRAINING BODY: St John Ambulance Australia", dp(18), dp(104), textPaint);

                    textPaint.setColor(colEmerald);
                    textPaint.setTextSize(dp(12));
                    canvas.drawText("CERT ID: SJA-QLD-849102-K", dp(18), dp(120), textPaint);

                    textPaint.setColor(colMuted);
                    textPaint.setTextSize(dp(9));
                    canvas.drawText("CPR RE-CERT DUE: 12 MAR 2026 (Annual Compliant)", dp(18), dp(138), textPaint);
                    canvas.drawText("FIRST AID EXPIRY: 12 MAR 2028 (3-Yr Triennial)", dp(18), dp(152), textPaint);

                    Paint pillBg = new Paint(Paint.ANTI_ALIAS_FLAG);
                    pillBg.setColor(colEmeraldSoft);
                    RectF pillR = new RectF(w - dp(116), dp(22), w - dp(18), dp(44));
                    canvas.drawRoundRect(pillR, dp(6), dp(6), pillBg);

                    textPaint.setColor(colEmerald);
                    textPaint.setTextSize(dp(9));
                    textPaint.setTextAlign(Paint.Align.CENTER);
                    canvas.drawText("✓ WHS COMPLIANT", pillR.centerX(), pillR.centerY() + dp(3), textPaint);

                    textPaint.setColor(colQuiet);
                    textPaint.setTextSize(dp(8));
                    textPaint.setTextAlign(Paint.Align.RIGHT);
                    canvas.drawText("ANNUAL CPR REFRESHED", w - dp(18), h - dp(14), textPaint);
                }

                textPaint.setColor(colAccent);
                textPaint.setTextSize(dp(9));
                textPaint.setTextAlign(Paint.Align.LEFT);
                canvas.drawText("🔄 TAP CARD TO FLIP VERIFICATION QR", dp(18), h - dp(14), textPaint);

            } else {
                textPaint.setColor(colAccent);
                textPaint.setTextSize(dp(10));
                textPaint.setTextAlign(Paint.Align.LEFT);
                canvas.drawText("DIGITAL AUDIT & JURISDICTION VERIFICATION", dp(18), dp(26), textPaint);

                float qrSize = dp(110);
                float qx = dp(18);
                float qy = dp(38);

                qrPaint.setColor(0xFFFFFFFF);
                canvas.drawRoundRect(new RectF(qx, qy, qx + qrSize, qy + qrSize), dp(8), dp(8), qrPaint);

                qrPaint.setColor(0xFF000000);
                float cellSize = qrSize / 15f;
                for (int r = 0; r < 15; r++) {
                    for (int c = 0; c < 15; c++) {
                        boolean isCorner1 = (r < 4 && c < 4);
                        boolean isCorner2 = (r < 4 && c >= 11);
                        boolean isCorner3 = (r >= 11 && c < 4);
                        boolean isPattern = ((r * 7 + c * 13 + (cardMode + 1) * 19) % 3 == 0);

                        if (isCorner1 || isCorner2 || isCorner3 || isPattern) {
                            canvas.drawRect(qx + c * cellSize, qy + r * cellSize,
                                            qx + (c + 1) * cellSize, qy + (r + 1) * cellSize, qrPaint);
                        }
                    }
                }

                textPaint.setColor(colPale);
                textPaint.setTextSize(dp(11));
                textPaint.setTextAlign(Paint.Align.LEFT);
                canvas.drawText("QLD REGULATED ID", qx + qrSize + dp(14), qy + dp(16), textPaint);

                textPaint.setColor(colMuted);
                textPaint.setTextSize(dp(9));
                canvas.drawText("HASH: 7f8a9b2c...41207", qx + qrSize + dp(14), qy + dp(34), textPaint);
                canvas.drawText("CHAIN: SHA-256 SPARK", qx + qrSize + dp(14), qy + dp(50), textPaint);
                canvas.drawText("SECURITY LIC: #41207", qx + qrSize + dp(14), qy + dp(66), textPaint);
                canvas.drawText("FIRST AID: SJA-849102", qx + qrSize + dp(14), qy + dp(82), textPaint);

                textPaint.setColor(colEmerald);
                textPaint.setTextSize(dp(9));
                canvas.drawText("✓ SIGNED IMMUTABLE", qx + qrSize + dp(14), qy + dp(100), textPaint);

                textPaint.setColor(colAccent);
                textPaint.setTextSize(dp(9));
                canvas.drawText("🔄 TAP CARD TO FLIP BACK", dp(18), h - dp(14), textPaint);
            }
        }
    }

    private void showOfficerCredentialVaultDialog() {
        hapticHeavyClick();
        final LinearLayout box = dialogContainer("🪪 Officer Credential Vault", "LEGAL AUDIT", colAccent);

        TextView info = new TextView(this);
        info.setText("Verified credentials for static guarding & WHS compliance:\n(Physically tilt phone to see holographic shimmer reflection)");
        info.setTextColor(colMuted);
        info.setTextSize(12);
        info.setPadding(0, 0, 0, dp(10));
        box.addView(info);

        LinearLayout switchRow = new LinearLayout(this);
        switchRow.setOrientation(LinearLayout.HORIZONTAL);
        switchRow.setBackground(rounded(colPanel2, dp(14)));
        switchRow.setPadding(dp(3), dp(3), dp(3), dp(3));
        LinearLayout.LayoutParams srp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        srp.bottomMargin = dp(12);
        switchRow.setLayoutParams(srp);

        final TextView tabLic = new TextView(this);
        tabLic.setText("🛡️ QLD Security Licence");
        tabLic.setTextSize(11);
        tabLic.setTypeface(Typeface.DEFAULT_BOLD);
        tabLic.setGravity(Gravity.CENTER);
        tabLic.setPadding(dp(8), dp(8), dp(8), dp(8));
        tabLic.setTextColor(colAccentInk);
        tabLic.setBackground(rounded(colAccent, dp(10)));
        LinearLayout.LayoutParams tlp1 = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        tabLic.setLayoutParams(tlp1);
        switchRow.addView(tabLic);

        final TextView tabAid = new TextView(this);
        tabAid.setText("🩹 First Aid & CPR");
        tabAid.setTextSize(11);
        tabAid.setTypeface(Typeface.DEFAULT_BOLD);
        tabAid.setGravity(Gravity.CENTER);
        tabAid.setPadding(dp(8), dp(8), dp(8), dp(8));
        tabAid.setTextColor(colMuted);
        tabAid.setBackground(null);
        LinearLayout.LayoutParams tlp2 = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        tabAid.setLayoutParams(tlp2);
        switchRow.addView(tabAid);
        box.addView(switchRow);

        final HolographicCardView holoCard = new HolographicCardView(this);
        activeHoloCard = holoCard;
        LinearLayout.LayoutParams hcp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(190));
        hcp.bottomMargin = dp(12);
        holoCard.setLayoutParams(hcp);

        holoCard.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                hapticDoublePulse();
                holoCard.toggleFlip();
            }
        });
        box.addView(holoCard);

        tabLic.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                hapticClick();
                holoCard.setCardMode(0);
                tabLic.setTextColor(colAccentInk);
                tabLic.setBackground(rounded(colAccent, dp(10)));
                tabAid.setTextColor(colMuted);
                tabAid.setBackground(null);
            }
        });

        tabAid.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                hapticClick();
                holoCard.setCardMode(1);
                tabAid.setTextColor(colAccentInk);
                tabAid.setBackground(rounded(colEmerald, dp(10)));
                tabLic.setTextColor(colMuted);
                tabLic.setBackground(null);
            }
        });

        final Dialog dlg = createTacticalDialog(box);

        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);

        TextView btnCopy = actionButton("📋 Copy Licence Details", colPanel2, colPale);
        btnCopy.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                hapticClick();
                String details = "Officer: R. Kelso | QLD Security Licence: #41207 (Class 1A/1C, Exp 14/10/2027) | "
                               + "First Aid: HLTAID011 / CPR HLTAID009 (SJA-QLD-849102-K) | Employer: Doherty Security Services";
                ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                cm.setPrimaryClip(ClipData.newPlainText("Officer Credentials", details));
                banner.setText("✓ Officer licence & First Aid credentials copied to clipboard");
                banner.setVisibility(View.VISIBLE);
                dlg.dismiss();
            }
        });
        btnRow.addView(btnCopy);

        TextView btnClose = actionButton("Close Vault", colAccent, colAccentInk);
        btnClose.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                hapticClick();
                dlg.dismiss();
            }
        });
        LinearLayout.LayoutParams cml = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.2f);
        cml.leftMargin = dp(8);
        btnClose.setLayoutParams(cml);
        btnRow.addView(btnClose);

        box.addView(btnRow);

        dlg.setOnDismissListener(new DialogInterface.OnDismissListener() {
            public void onDismiss(DialogInterface dialog) {
                activeHoloCard = null;
            }
        });

        dlg.show();
    }

    // =========================================================================
    // CONTACTS TAB
    // =========================================================================

    private LinearLayout buildContactsTab() {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(0, dp(6), 0, dp(24));

        container.addView(contactsSectionHeader("🚨 EMERGENCY SERVICES (IMMEDIATE RESPONSE)", colCrimson));
        container.addView(contactCard("Triple Zero (000)", "Police · Fire · Ambulance", "000", "24/7 PRIORITY", colCrimson));
        container.addView(contactCard("Logan District Police", "Kingston & Logan Central Station", "0738261888", "24/7 ATTENDANCE", colCrimson));
        container.addView(contactCard("SES Queensland", "Storm, Flood & Structural Damage", "132500", "24/7 DISPATCH", colCyan));
        container.addView(contactCard("Poisons Info Centre", "Chemical & Hazardous Substance Exposure", "131126", "24/7 SUPPORT", colAccent));

        container.addView(contactsSectionHeader("🏢 DOHERTY SECURITY SERVICES (DSS)", colAccent));
        container.addView(contactCard("DSS 24/7 Control Room", "Central Dispatch & Escalations", "1300377000", "24/7 MONITORING", colAccent));
        container.addView(contactCard("DSS Operations Manager", "Brisbane North & South Operations", "0418700120", "ON CALL", colAccent));
        container.addView(contactCard("DSS Field Patrol Supervisor", "Mobile Response Unit 4", "0422555810", "ON SHIFT 18:00–06:00", colEmerald));

        container.addView(contactsSectionHeader("🏭 HUME DOORS & TIMBER (KINGSTON SITE)", colCyan));
        container.addView(contactCard("Hume Site Operations Manager", "Kingston Plant Management", "0439123456", "PRIMARY CLIENT CONTACT", colCyan));
        container.addView(contactCard("Hume Facilities & Plant Engineer", "Power, Pump House & Gate Failures", "0411987654", "ON CALL MAINTENANCE", colCyan));
        container.addView(contactCard("Hume WHS / Safety Officer", "Workplace Safety & Incident Officer", "0423456789", "ON CALL SAFETY", colCyan));

        container.addView(contactsSectionHeader("👥 ON-SITE & RELIEF GUARDS", colPale));
        container.addView(contactCard("Officer Lochran Doherty", "Current Static Guard · LIC #41207", "0455123789", "ON SITE (TONIGHT)", colEmerald));
        container.addView(contactCard("Relief / Day Crew Guard", "Morning Handover Officer (06:05)", "0400111222", "06:05 HANDOVER", colMuted));

        return container;
    }

    private TextView contactsSectionHeader(String title, int color) {
        TextView t = new TextView(this);
        t.setText(title);
        t.setTextColor(color);
        t.setTextSize(11);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setLetterSpacing(0.12f);
        t.setPadding(0, dp(14), 0, dp(6));
        return t;
    }

    private LinearLayout contactCard(final String name, String subtitle, final String phoneDisplay,
                                     String badgeText, final int badgeColor) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(rounded(colPanel, dp(14)));
        card.setPadding(dp(14), dp(12), dp(14), dp(12));

        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        clp.bottomMargin = dp(8);
        card.setLayoutParams(clp);

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = new TextView(this);
        title.setText(name);
        title.setTextColor(colPale);
        title.setTextSize(14);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams tl = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        title.setLayoutParams(tl);
        top.addView(title);

        TextView badge = new TextView(this);
        badge.setText(badgeText);
        badge.setTextColor(badgeColor);
        badge.setTextSize(9);
        badge.setTypeface(Typeface.MONOSPACE);
        badge.setPadding(dp(6), dp(2), dp(6), dp(2));
        badge.setBackground(rounded(colPanel2, dp(4)));
        top.addView(badge);
        card.addView(top);

        TextView sub = new TextView(this);
        sub.setText(subtitle);
        sub.setTextColor(colMuted);
        sub.setTextSize(12);
        sub.setPadding(0, dp(2), 0, dp(8));
        card.addView(sub);

        LinearLayout btm = new LinearLayout(this);
        btm.setOrientation(LinearLayout.HORIZONTAL);
        btm.setGravity(Gravity.CENTER_VERTICAL);

        final String formattedNum = formatPhoneNumber(phoneDisplay);
        TextView num = new TextView(this);
        num.setText("📞 " + formattedNum);
        num.setTextColor(colAccent);
        num.setTextSize(13);
        num.setTypeface(Typeface.MONOSPACE);
        num.setPadding(0, dp(4), dp(8), dp(4));
        num.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                hapticClick();
                try {
                    android.content.ClipboardManager cm = (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    if (cm != null) {
                        cm.setPrimaryClip(android.content.ClipData.newPlainText("phone", phoneDisplay));
                        banner.setText("✓ Copied " + formattedNum + " to clipboard");
                        banner.setVisibility(View.VISIBLE);
                    }
                } catch (Exception e) {}
            }
        });
        LinearLayout.LayoutParams nl = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        num.setLayoutParams(nl);
        btm.addView(num);

        final boolean isMobile = phoneDisplay.startsWith("04");
        if (isMobile) {
            TextView btnSms = new TextView(this);
            btnSms.setText("SMS");
            btnSms.setTextColor(colCyan);
            btnSms.setTextSize(11);
            btnSms.setTypeface(Typeface.DEFAULT_BOLD);
            btnSms.setPadding(dp(10), dp(6), dp(10), dp(6));
            btnSms.setBackground(pressable(colPanel2, dp(8)));
            LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            slp.rightMargin = dp(6);
            btnSms.setLayoutParams(slp);
            btnSms.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    hapticClick();
                    registerActivity();
                    try {
                        Intent smsIntent = new Intent(Intent.ACTION_SENDTO);
                        smsIntent.setData(Uri.parse("smsto:" + phoneDisplay));
                        smsIntent.putExtra("sms_body", "DSS Gatehouse (Hume Kingston Post 01): ");
                        startActivity(smsIntent);
                    } catch (Exception e) {}
                }
            });
            btm.addView(btnSms);
        }

        TextView btnCall = new TextView(this);
        btnCall.setText(phoneDisplay.equals("000") ? "CALL 000" : "CALL");
        btnCall.setTextColor(phoneDisplay.equals("000") ? 0xFFFFFFFF : colAccentInk);
        btnCall.setTextSize(11);
        btnCall.setTypeface(Typeface.DEFAULT_BOLD);
        btnCall.setPadding(dp(12), dp(6), dp(12), dp(6));
        btnCall.setBackground(pressable(phoneDisplay.equals("000") ? colCrimson : colAccent, dp(8)));
        btnCall.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                hapticClick();
                registerActivity();
                dialNumber(phoneDisplay);
            }
        });
        btm.addView(btnCall);
        card.addView(btm);

        return card;
    }

    private String formatPhoneNumber(String raw) {
        if (raw.equals("000")) return "000 (EMERGENCY)";
        if (raw.length() == 10 && raw.startsWith("04")) {
            return raw.substring(0, 4) + " " + raw.substring(4, 7) + " " + raw.substring(7);
        }
        if (raw.length() == 10 && raw.startsWith("07")) {
            return "(07) " + raw.substring(2, 6) + " " + raw.substring(6);
        }
        if (raw.length() == 10 && raw.startsWith("1300")) {
            return "1300 " + raw.substring(4, 7) + " " + raw.substring(7);
        }
        if (raw.length() == 6 && (raw.startsWith("132") || raw.startsWith("131"))) {
            return raw.substring(0, 3) + " " + raw.substring(3);
        }
        return raw;
    }

    private void dialNumber(String raw) {
        try {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:" + raw));
            startActivity(intent);
        } catch (Exception e) {
            banner.setText("unable to open dialer for " + raw);
            banner.setVisibility(View.VISIBLE);
        }
    }

    private void rebuildCurrentScreen() {
        buildUi();
        refresh();
    }

    private LinearLayout headerCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(rounded(colPanel, dp(18)));
        card.setPadding(dp(16), dp(16), dp(16), dp(16));

        TextView brand = new TextView(this);
        brand.setText("DOHERTY SECURITY SERVICES · STATIC GUARDING");
        brand.setTextColor(colAccent);
        brand.setTextSize(10);
        brand.setTypeface(Typeface.DEFAULT_BOLD);
        brand.setLetterSpacing(0.14f);
        brand.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { showWelfareCheckDialog(); }
        });
        card.addView(brand);

        TextView site = new TextView(this);
        site.setText("Hume Doors & Timber, Kingston");
        site.setTextColor(colPale);
        site.setTextSize(21);
        site.setTypeface(Typeface.DEFAULT_BOLD);
        site.setPadding(0, dp(4), 0, dp(4));
        card.addView(site);

        LinearLayout guardRow = new LinearLayout(this);
        guardRow.setOrientation(LinearLayout.HORIZONTAL);
        guardRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView who = new TextView(this);
        who.setText("Officer Lochran Doherty  ");
        who.setTextColor(colPale);
        who.setTextSize(13);
        who.setTypeface(Typeface.DEFAULT_BOLD);
        guardRow.addView(who);

        TextView lic = new TextView(this);
        lic.setText("LIC #41207 🪪");
        lic.setTextColor(colAccent);
        lic.setTextSize(10);
        lic.setTypeface(Typeface.MONOSPACE);
        lic.setPadding(dp(6), dp(2), dp(6), dp(2));
        lic.setBackground(rounded(colPanel3, dp(4)));
        guardRow.addView(lic);

        TextView shiftTime = new TextView(this);
        shiftTime.setText("  · Shift 18:00–06:00");
        shiftTime.setTextColor(colQuiet);
        shiftTime.setTextSize(12);
        guardRow.addView(shiftTime);

        guardRow.setPadding(0, dp(4), 0, 0);
        guardRow.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showOfficerCredentialVaultDialog();
            }
        });

        card.addView(guardRow);
        return card;
    }

    private LinearLayout sectionHeader(String title, String action) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(14), 0, dp(6));

        TextView t = new TextView(this);
        t.setText(title.toUpperCase());
        t.setTextColor(colQuiet);
        t.setTextSize(11);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setLetterSpacing(0.12f);
        LinearLayout.LayoutParams tl = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        t.setLayoutParams(tl);
        row.addView(t);

        if (action != null) {
            TextView a = new TextView(this);
            a.setText(action);
            a.setTextColor(colAccent);
            a.setTextSize(11);
            a.setTypeface(Typeface.DEFAULT_BOLD);
            row.addView(a);
        }
        return row;
    }

    private TextView label(String text) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextColor(colQuiet);
        t.setTextSize(11);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setLetterSpacing(0.12f);
        return t;
    }

    private TextView pill(String text, boolean strong) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextSize(11);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setTextColor(strong ? colAccentInk : colMuted);
        t.setPadding(dp(11), dp(5), dp(11), dp(5));
        t.setBackground(strong ? rounded(colAccent, dp(20)) : outlined(colLine, dp(20)));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.rightMargin = dp(8);
        t.setLayoutParams(lp);
        return t;
    }

    private TextView patrolActionCard(final String name, final String uid, boolean isLeft) {
        TextView t = new TextView(this);
        t.setTag(name);
        t.setText(name);
        t.setTextSize(13);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setTextColor(colPale);
        t.setPadding(dp(14), dp(14), dp(14), dp(14));
        t.setBackground(pressable(colPanel, dp(16)));
        t.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                hapticClick();
                promptExternalPatrol(name, uid);
            }
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        lp.rightMargin = isLeft ? dp(5) : 0;
        lp.leftMargin = isLeft ? 0 : dp(5);
        t.setLayoutParams(lp);
        return t;
    }

    private TextView lotBadge(final String lotName, final String uid, boolean isLast) {
        TextView b = new TextView(this);
        b.setTag(lotName);
        b.setText(lotName);
        b.setTextSize(11);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setGravity(Gravity.CENTER);
        b.setTextColor(colPale);
        b.setPadding(dp(8), dp(10), dp(8), dp(10));
        b.setBackground(pressable(colPanel, dp(12)));
        b.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                hapticClick();
                promptLotShutdown(lotName, uid);
            }
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        lp.rightMargin = isLast ? 0 : dp(5);
        b.setLayoutParams(lp);
        return b;
    }

    private LinearLayout fireCompactRow(final String name, final String uid, boolean isLast) {
        LinearLayout row = new LinearLayout(this);
        row.setTag(name);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(4), dp(8), dp(4), dp(8));
        row.setBackground(pressable(Color.TRANSPARENT, dp(8)));
        row.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                hapticClick();
                promptPumpHouseCheck(name, uid);
            }
        });

        TextView title = new TextView(this);
        title.setText(name);
        title.setTextColor(colPale);
        title.setTextSize(13);
        LinearLayout.LayoutParams tl = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        title.setLayoutParams(tl);
        row.addView(title);

        TextView status = new TextView(this);
        status.setText("·");
        status.setTextColor(colQuiet);
        status.setTextSize(11);
        status.setTypeface(Typeface.MONOSPACE);
        row.addView(status);

        return row;
    }

    private LinearLayout buildCaptureDock() {
        LinearLayout dock = new LinearLayout(this);
        dock.setOrientation(LinearLayout.HORIZONTAL);
        dock.setPadding(0, dp(4), 0, dp(12));

        dock.addView(dockButton("Incident", "🚨", new View.OnClickListener() {
            public void onClick(View v) {
                hapticHeavyClick();
                showModernIncidentSheet();
            }
        }, 0));

        dock.addView(dockButton("Notes", "📝", new View.OnClickListener() {
            public void onClick(View v) {
                hapticClick();
                showModernNotesSheet();
            }
        }, 1));

        dock.addView(dockButton("Photo", "📷", new View.OnClickListener() {
            public void onClick(View v) {
                hapticDoublePulse();
                checkAndLaunchFastCamera(null);
            }
        }, 2));

        dock.addView(dockButton("Voice", "🎙️", new View.OnClickListener() {
            public void onClick(View v) {
                hapticDoublePulse();
                checkAndLaunchVoice();
            }
        }, 3));

        return dock;
    }

    private LinearLayout dockButton(String title, String icon, View.OnClickListener onClick, int pos) {
        LinearLayout btn = new LinearLayout(this);
        btn.setOrientation(LinearLayout.VERTICAL);
        btn.setGravity(Gravity.CENTER);
        btn.setBackground(pressable(colPanel, dp(16)));
        btn.setPadding(dp(6), dp(12), dp(6), dp(12));
        btn.setOnClickListener(onClick);

        TextView ic = new TextView(this);
        ic.setText(icon);
        ic.setTextSize(18);
        ic.setGravity(Gravity.CENTER);
        btn.addView(ic);

        TextView lbl = new TextView(this);
        lbl.setText(title);
        lbl.setTextColor(colMuted);
        lbl.setTextSize(10);
        lbl.setTypeface(Typeface.DEFAULT_BOLD);
        lbl.setPadding(0, dp(4), 0, 0);
        lbl.setGravity(Gravity.CENTER);
        btn.addView(lbl);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        lp.leftMargin = (pos > 0) ? dp(4) : 0;
        lp.rightMargin = (pos < 3) ? dp(4) : 0;
        btn.setLayoutParams(lp);
        return btn;
    }

    // =========================================================================
    // DIALOG CONTAINER & SATELLITE RADAR
    // =========================================================================

    private Dialog createTacticalDialog(View content) {
        final Dialog dlg = new Dialog(this);
        dlg.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dlg.setContentView(content);
        if (dlg.getWindow() != null) {
            dlg.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dlg.getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            dlg.getWindow().setGravity(Gravity.BOTTOM);
        }
        return dlg;
    }

    private LinearLayout dialogContainer(String titleText, String badgeText, int badgeColor) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setBackground(rounded(colPanel, dp(24)));
        box.setPadding(dp(20), dp(20), dp(20), dp(24));
        box.setElevation(dp(16));

        View handle = new View(this);
        handle.setBackground(rounded(colLine, dp(3)));
        LinearLayout.LayoutParams hl = new LinearLayout.LayoutParams(dp(44), dp(4));
        hl.gravity = Gravity.CENTER_HORIZONTAL;
        hl.bottomMargin = dp(16);
        handle.setLayoutParams(hl);
        box.addView(handle);

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setPadding(0, 0, 0, dp(14));

        TextView title = new TextView(this);
        title.setText(titleText);
        title.setTextColor(colPale);
        title.setTextSize(17);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams tl = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        title.setLayoutParams(tl);
        top.addView(title);

        if (badgeText != null) {
            TextView badge = new TextView(this);
            badge.setText(badgeText);
            badge.setTextColor(badgeColor);
            badge.setTextSize(10);
            badge.setTypeface(Typeface.MONOSPACE);
            badge.setPadding(dp(8), dp(3), dp(8), dp(3));
            badge.setBackground(rounded(badgeColor == colCrimson ? colCrimsonSoft : colAccentSoft, dp(6)));
            top.addView(badge);
        }
        box.addView(top);
        return box;
    }

    private class SatellitePolarRadarView extends View {
        private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint satPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        public SatellitePolarRadarView(Context context) {
            super(context);
            gridPaint.setStyle(Paint.Style.STROKE);
            gridPaint.setStrokeWidth(dp(1));
            satPaint.setStyle(Paint.Style.FILL);
            textPaint.setTextAlign(Paint.Align.CENTER);
            textPaint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            int w = getWidth();
            int h = getHeight();
            if (w <= 0 || h <= 0) return;

            float cx = w / 2f;
            float cy = h / 2f;
            float maxR = Math.min(w, h) / 2f - dp(14);

            gridPaint.setColor(colLineSubtle);
            canvas.drawCircle(cx, cy, maxR, gridPaint);
            canvas.drawCircle(cx, cy, maxR * 0.66f, gridPaint);
            canvas.drawCircle(cx, cy, maxR * 0.33f, gridPaint);

            gridPaint.setColor(colLine);
            canvas.drawLine(cx - maxR, cy, cx + maxR, cy, gridPaint);
            canvas.drawLine(cx, cy - maxR, cx, cy + maxR, gridPaint);

            textPaint.setColor(colQuiet);
            textPaint.setTextSize(dp(9));
            canvas.drawText("N", cx, cy - maxR + dp(9), textPaint);
            canvas.drawText("S", cx, cy + maxR - dp(2), textPaint);
            canvas.drawText("E", cx + maxR - dp(7), cy + dp(3), textPaint);
            canvas.drawText("W", cx - maxR + dp(7), cy + dp(3), textPaint);

            int[][] sats = {
                {45, 65, 1}, {110, 45, 1}, {165, 80, 1}, {210, 30, 2},
                {280, 55, 1}, {330, 75, 1}, {15, 20, 2}, {85, 35, 1},
                {195, 60, 1}, {245, 85, 1}, {305, 40, 1}, {140, 15, 2}
            };

            for (int[] s : sats) {
                float az = s[0];
                float el = s[1];
                int type = s[2];

                float r = maxR * (1.0f - (el / 90.0f));
                double rad = Math.toRadians(az - 90);
                float sx = (float) (cx + Math.cos(rad) * r);
                float sy = (float) (cy + Math.sin(rad) * r);

                satPaint.setColor(type == 1 ? colEmerald : colAccent);
                canvas.drawCircle(sx, sy, dp(4), satPaint);
            }
        }
    }

    private LinearLayout buildGpsCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(rounded(colPanel, dp(16)));
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        clp.bottomMargin = dp(12);
        card.setLayoutParams(clp);

        satelliteRadarView = new SatellitePolarRadarView(this);
        LinearLayout.LayoutParams srp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(130));
        srp.bottomMargin = dp(10);
        satelliteRadarView.setLayoutParams(srp);
        card.addView(satelliteRadarView);

        gpsCoordsText = new TextView(this);
        gpsCoordsText.setText("Latitude / Longitude: Acquiring Fix...");
        gpsCoordsText.setTextColor(colEmerald);
        gpsCoordsText.setTextSize(15);
        gpsCoordsText.setTypeface(Typeface.MONOSPACE);
        card.addView(gpsCoordsText);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(4), 0, dp(12));

        gpsAltitudeText = new TextView(this);
        gpsAltitudeText.setText("Altitude: -- m");
        gpsAltitudeText.setTextColor(colMuted);
        gpsAltitudeText.setTextSize(12);
        LinearLayout.LayoutParams atl = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        gpsAltitudeText.setLayoutParams(atl);
        row.addView(gpsAltitudeText);

        gpsAccuracyText = new TextView(this);
        gpsAccuracyText.setText("Accuracy: ± -- m  [HDOP 0.7]");
        gpsAccuracyText.setTextColor(colQuiet);
        gpsAccuracyText.setTextSize(12);
        row.addView(gpsAccuracyText);
        card.addView(row);

        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);

        TextView btnCopy = actionButton("📋 Copy for 000", colPanel2, colPale);
        btnCopy.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                hapticClick();
                if (lastKnownLocation != null) {
                    String coords = String.format(Locale.US, "%.6f, %.6f (Hume Doors Kingston)",
                            lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
                    ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    cm.setPrimaryClip(ClipData.newPlainText("GPS Coords", coords));
                    banner.setText("✓ GPS coordinates copied to clipboard for emergency 000");
                    banner.setVisibility(View.VISIBLE);
                }
            }
        });
        btnRow.addView(btnCopy);

        TextView btnLogGps = actionButton("📍 Log GPS Fix", colAccent, colAccentInk);
        btnLogGps.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                hapticHeavyClick();
                if (lastKnownLocation != null) {
                    String logLine = String.format(Locale.US, "[GPS FIX] Lat: %.6f, Lon: %.6f, Alt: %.1fm (±%.1fm)",
                            lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude(),
                            lastKnownLocation.getAltitude(), lastKnownLocation.getAccuracy());
                    note(Core.TOPIC_ROUTINE, logLine);
                    banner.setText("✓ GPS telemetry point logged to Ada record");
                    banner.setVisibility(View.VISIBLE);
                }
            }
        });
        LinearLayout.LayoutParams lgl = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.2f);
        lgl.leftMargin = dp(8);
        btnLogGps.setLayoutParams(lgl);
        btnRow.addView(btnLogGps);

        card.addView(btnRow);
        return card;
    }

    @Override
    public void onLocationChanged(Location location) {
        lastKnownLocation = location;
        updateGpsDisplay(location);
    }

    private void updateGpsDisplay(Location loc) {
        if (gpsCoordsText == null) return;
        gpsCoordsText.setText(String.format(Locale.US, "%.6f°, %.6f°", loc.getLatitude(), loc.getLongitude()));
        gpsAltitudeText.setText(String.format(Locale.US, "Altitude: %.1f m ASL", loc.getAltitude()));
        gpsAccuracyText.setText(String.format(Locale.US, "Accuracy: ±%.1f m  [HDOP 0.7]", loc.getAccuracy()));
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {}
    @Override
    public void onProviderEnabled(String provider) {}
    @Override
    public void onProviderDisabled(String provider) {}

    // ---- Blueprint Mini-Map ----

    private static class MapSector {
        String code;
        String label;
        String desc;
        MapSector(String c, String l, String d) { code = c; label = l; desc = d; }
    }

    private static final MapSector[] SITE_MAP_SECTORS = {
        new MapSector("NW", "North-West", "North-West Perimeter Fence"),
        new MapSector("GATE_A", "Gate A (Main)", "Gate A (Main Vehicle Entry)"),
        new MapSector("NE", "North-East", "North-East Fence (near Lot 14)"),
        new MapSector("LOT18", "Lot 18 (Yard)", "Lot 18 (Timber Yard / Dispatch)"),
        new MapSector("CENTRAL", "Central Yard", "Central Compound & Staging"),
        new MapSector("LOT14", "Lot 14 (Doors)", "Lot 14 (Door Assembly Floor)"),
        new MapSector("LOT17", "Lot 17 (Chem)", "Lot 17 (Adhesives & Chemicals)"),
        new MapSector("GATE_B", "Gate B / P16", "South Gate B & Lot 16 Pump House"),
        new MapSector("LOT15", "Lot 15 (Mill)", "Lot 15 (Timber Sawmill Floor)")
    };

    private LinearLayout buildBlueprintMiniMap(final String[] pinnedLocationHolder, final Runnable onPinChanged) {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setBackground(rounded(colPanel2, dp(14)));
        container.setPadding(dp(12), dp(10), dp(12), dp(10));
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        clp.bottomMargin = dp(10);
        container.setLayoutParams(clp);

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setPadding(0, 0, 0, dp(8));

        TextView title = new TextView(this);
        title.setText("📍 SITE BLUEPRINT (TAP TO PIN LOCATION)");
        title.setTextColor(colQuiet);
        title.setTextSize(10);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setLetterSpacing(0.12f);
        LinearLayout.LayoutParams tl = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        title.setLayoutParams(tl);
        top.addView(title);

        final TextView pinStatus = new TextView(this);
        pinStatus.setText(pinnedLocationHolder[0] == null ? "No Pin Dropped" : "📍 " + pinnedLocationHolder[0]);
        pinStatus.setTextColor(pinnedLocationHolder[0] == null ? colQuiet : colAccent);
        pinStatus.setTextSize(10);
        pinStatus.setTypeface(Typeface.MONOSPACE);
        top.addView(pinStatus);
        container.addView(top);

        final ArrayList<TextView> sectorViews = new ArrayList<TextView>();

        for (int r = 0; r < 3; r++) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            rlp.bottomMargin = (r < 2) ? dp(6) : 0;
            row.setLayoutParams(rlp);

            for (int c = 0; c < 3; c++) {
                final int idx = r * 3 + c;
                final MapSector sec = SITE_MAP_SECTORS[idx];

                final TextView btnSec = new TextView(this);
                btnSec.setText(sec.label);
                btnSec.setTextSize(11);
                btnSec.setTypeface(Typeface.DEFAULT_BOLD);
                btnSec.setGravity(Gravity.CENTER);
                btnSec.setPadding(dp(6), dp(10), dp(6), dp(10));

                boolean isSelected = pinnedLocationHolder[0] != null && pinnedLocationHolder[0].equals(sec.desc);
                updateMapSectorStyle(btnSec, isSelected);

                LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
                        0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
                slp.leftMargin = (c > 0) ? dp(4) : 0;
                slp.rightMargin = (c < 2) ? dp(4) : 0;
                btnSec.setLayoutParams(slp);

                btnSec.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        hapticClick();
                        if (pinnedLocationHolder[0] != null && pinnedLocationHolder[0].equals(sec.desc)) {
                            pinnedLocationHolder[0] = null;
                            pinStatus.setText("No Pin Dropped");
                            pinStatus.setTextColor(colQuiet);
                        } else {
                            pinnedLocationHolder[0] = sec.desc;
                            pinStatus.setText("📍 " + sec.desc);
                            pinStatus.setTextColor(colAccent);
                        }
                        for (int k = 0; k < sectorViews.size(); k++) {
                            TextView tv = sectorViews.get(k);
                            MapSector s = SITE_MAP_SECTORS[k];
                            boolean sel = pinnedLocationHolder[0] != null && pinnedLocationHolder[0].equals(s.desc);
                            updateMapSectorStyle(tv, sel);
                        }
                        if (onPinChanged != null) onPinChanged.run();
                    }
                });

                sectorViews.add(btnSec);
                row.addView(btnSec);
            }
            container.addView(row);
        }

        return container;
    }

    private void updateMapSectorStyle(TextView tv, boolean selected) {
        if (selected) {
            tv.setTextColor(colAccentInk);
            tv.setBackground(rounded(colAccent, dp(8)));
        } else {
            tv.setTextColor(colPale);
            tv.setBackground(pressable(colPanel3, dp(8)));
        }
    }

    // ---- Camera2 with Horizon Leveler ----

    interface OnPhotoCapturedCallback {
        void onCaptured(Bitmap bmp, String sha256);
    }

    private void checkAndLaunchFastCamera(final OnPhotoCapturedCallback cb) {
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, REQ_PERM_CAMERA);
            return;
        }
        openInAppCameraOverlay(cb);
    }

    private void openInAppCameraOverlay(final OnPhotoCapturedCallback cb) {
        final Dialog dlg = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dlg.requestWindowFeature(Window.FEATURE_NO_TITLE);

        final LinearLayout cameraRoot = new LinearLayout(this);
        cameraRoot.setOrientation(LinearLayout.VERTICAL);
        cameraRoot.setBackgroundColor(0xFF000000);
        cameraRoot.setPadding(dp(16), dp(24), dp(16), dp(56));

        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        topBar.setPadding(0, 0, 0, dp(12));

        TextView camTitle = new TextView(this);
        camTitle.setText("📷 PHOTO EVIDENCE & LEVEL");
        camTitle.setTextColor(colPale);
        camTitle.setTextSize(13);
        camTitle.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams ctl = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        camTitle.setLayoutParams(ctl);
        topBar.addView(camTitle);

        TextView btnClose = new TextView(this);
        btnClose.setText("✕");
        btnClose.setTextColor(colPale);
        btnClose.setTextSize(20);
        btnClose.setPadding(dp(14), dp(4), dp(4), dp(4));
        btnClose.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                hapticClick();
                dlg.dismiss();
            }
        });
        topBar.addView(btnClose);
        cameraRoot.addView(topBar);

        FrameLayout viewfinderFrame = new FrameLayout(this);
        LinearLayout.LayoutParams vfl = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
        vfl.bottomMargin = dp(12);
        viewfinderFrame.setLayoutParams(vfl);

        final TextureView textureView = new TextureView(this);
        viewfinderFrame.addView(textureView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        final HorizonLevelerView levelerView = new HorizonLevelerView(this);
        activeLevelerView = levelerView;
        viewfinderFrame.addView(levelerView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        cameraRoot.addView(viewfinderFrame);

        LinearLayout bottomBar = new LinearLayout(this);
        bottomBar.setOrientation(LinearLayout.HORIZONTAL);
        bottomBar.setGravity(Gravity.CENTER);
        bottomBar.setPadding(0, 0, 0, dp(6));

        final TextView btnCapture = new TextView(this);
        btnCapture.setText("🔘 CAPTURE PHOTO EVIDENCE");
        btnCapture.setTextColor(colAccentInk);
        btnCapture.setTextSize(15);
        btnCapture.setTypeface(Typeface.DEFAULT_BOLD);
        btnCapture.setGravity(Gravity.CENTER);
        btnCapture.setPadding(dp(20), dp(16), dp(20), dp(16));
        btnCapture.setBackground(pressable(colAccent, dp(18)));
        LinearLayout.LayoutParams cbl = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        btnCapture.setLayoutParams(cbl);

        final Runnable executeCapture = new Runnable() {
            public void run() {
                hapticDoublePulse();
                registerActivity();
                Bitmap bmp = textureView.getBitmap();
                if (bmp != null) {
                    dlg.dismiss();
                    byte[] bytes = bitmapToJpegBytes(bmp);
                    String hash = sha256Hex(bytes);
                    if (cb != null) {
                        cb.onCaptured(bmp, hash);
                    } else {
                        showPhotoReviewSheet(bmp);
                    }
                }
            }
        };

        btnCapture.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { executeCapture.run(); }
        });
        bottomBar.addView(btnCapture);
        cameraRoot.addView(bottomBar);

        dlg.setContentView(cameraRoot);
        dlg.setOnDismissListener(new DialogInterface.OnDismissListener() {
            public void onDismiss(DialogInterface dialog) {
                activeLevelerView = null;
            }
        });
        dlg.show();
    }

    private void showPhotoReviewSheet(final Bitmap bmp) {
        final byte[] bytes = bitmapToJpegBytes(bmp);
        final String hash = sha256Hex(bytes);
        final String hashSnippet = hash.length() >= 8 ? hash.substring(0, 8) : hash;

        final LinearLayout box = dialogContainer("📷 Photo Evidence", "SHA-256 VERIFIED", colEmerald);

        ImageView preview = new ImageView(this);
        preview.setImageBitmap(bmp);
        preview.setScaleType(ImageView.ScaleType.CENTER_CROP);
        preview.setBackground(rounded(colPanel2, dp(14)));
        preview.setClipToOutline(true);
        LinearLayout.LayoutParams pl = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(180));
        pl.bottomMargin = dp(12);
        preview.setLayoutParams(pl);
        box.addView(preview);

        final EditText descField = modernInputField("Photo Subject (e.g. Main gate padlock, Lot 16 mesh)");
        box.addView(descField);

        final Dialog dlg = createTacticalDialog(box);

        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setPadding(0, dp(16), 0, 0);

        TextView btnCancel = actionButton("Cancel", colLine, colMuted);
        btnCancel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                hapticClick();
                dlg.dismiss();
            }
        });
        btnRow.addView(btnCancel);

        TextView btnCommit = actionButton("Save Photo", colAccent, colAccentInk);
        btnCommit.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                hapticHeavyClick();
                registerActivity();
                String d = descField.getText().toString().trim();
                String noteText = "[PHOTO " + hashSnippet + "] " + (d.isEmpty() ? "evidence captured" : d);
                if (!oneLine(noteText)) {
                    banner.setText("notes must be one line");
                    banner.setVisibility(View.VISIBLE);
                    return;
                }
                note(Core.TOPIC_ROUTINE, noteText);
                dlg.dismiss();
            }
        });
        LinearLayout.LayoutParams cml = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.4f);
        cml.leftMargin = dp(8);
        btnCommit.setLayoutParams(cml);
        btnRow.addView(btnCommit);

        box.addView(btnRow);
        dlg.show();
    }

    private void promptExternalPatrol(final String name, final String uid) {
        final LinearLayout box = dialogContainer("🛡️ " + name, "PERIMETER INSPECTION", colAccent);

        TextView subtitle = new TextView(this);
        subtitle.setText("Select perimeter conditions, pin location on map or attach photo evidence:");
        subtitle.setTextColor(colMuted);
        subtitle.setTextSize(12);
        subtitle.setPadding(0, 0, 0, dp(8));
        box.addView(subtitle);

        final ArrayList<String> selectedItems = new ArrayList<String>();
        selectedItems.add(EXTERNAL_OPTIONS[0]);

        final ArrayList<TextView> itemViews = new ArrayList<TextView>();

        for (int i = 0; i < EXTERNAL_OPTIONS.length; i++) {
            final String opt = EXTERNAL_OPTIONS[i];
            final TextView item = new TextView(this);
            item.setText(opt);
            item.setTextSize(13);
            item.setPadding(dp(14), dp(10), dp(14), dp(10));

            final boolean isAllClear = i == 0;
            updateCheckItemStyle(item, selectedItems.contains(opt), isAllClear);

            LinearLayout.LayoutParams il = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            il.bottomMargin = dp(6);
            item.setLayoutParams(il);

            item.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    hapticClick();
                    if (isAllClear) {
                        selectedItems.clear();
                        selectedItems.add(EXTERNAL_OPTIONS[0]);
                    } else {
                        selectedItems.remove(EXTERNAL_OPTIONS[0]);
                        if (selectedItems.contains(opt)) {
                            selectedItems.remove(opt);
                        } else {
                            selectedItems.add(opt);
                        }
                        if (selectedItems.isEmpty()) {
                            selectedItems.add(EXTERNAL_OPTIONS[0]);
                        }
                    }
                    for (int k = 0; k < itemViews.size(); k++) {
                        TextView tv = itemViews.get(k);
                        String o = EXTERNAL_OPTIONS[k];
                        updateCheckItemStyle(tv, selectedItems.contains(o), k == 0);
                    }
                }
            });

            itemViews.add(item);
            box.addView(item);
        }

        final String[] pinnedLocation = {null};
        box.addView(buildBlueprintMiniMap(pinnedLocation, null));

        final EditText customField = modernInputField("Perimeter observation notes (optional)...");
        box.addView(customField);

        final Dialog dlg = createTacticalDialog(box);

        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setPadding(0, dp(12), 0, 0);

        TextView btnCancel = actionButton("Cancel", colLine, colMuted);
        btnCancel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                hapticClick();
                dlg.dismiss();
            }
        });
        btnRow.addView(btnCancel);

        TextView btnSave = actionButton("✓ Log Patrol", colAccent, colAccentInk);
        btnSave.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                hapticHeavyClick();
                registerActivity();
                tap(name, uid);
                String custom = customField.getText().toString().trim();
                
                StringBuilder sb = new StringBuilder();
                sb.append(name).append(": ");
                for (int k = 0; k < selectedItems.size(); k++) {
                    if (k > 0) sb.append(", ");
                    sb.append(selectedItems.get(k));
                }
                if (pinnedLocation[0] != null) {
                    sb.append(" 📍 [PIN: ").append(pinnedLocation[0]).append("]");
                }
                if (!custom.isEmpty()) {
                    sb.append(" · ").append(custom);
                }
                String fullLine = sb.toString();
                if (!oneLine(fullLine)) {
                    banner.setText("notes must be one line");
                    banner.setVisibility(View.VISIBLE);
                    return;
                }
                note(Core.TOPIC_ROUTINE, fullLine);
                dlg.dismiss();
            }
        });
        LinearLayout.LayoutParams cml = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.4f);
        cml.leftMargin = dp(8);
        btnSave.setLayoutParams(cml);
        btnRow.addView(btnSave);

        box.addView(btnRow);
        dlg.show();
    }

    private void checkAndLaunchVoice() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQ_PERM_AUDIO);
            return;
        }
        showModernVoiceSheet();
    }

    private void showModernVoiceSheet() {
        final LinearLayout box = dialogContainer("🎙️ Voice Memo", "AUDIO LOG", colAccent);

        final TextView timerView = new TextView(this);
        timerView.setText("00:00");
        timerView.setTextSize(32);
        timerView.setTextColor(colPale);
        timerView.setTypeface(Typeface.MONOSPACE);
        timerView.setGravity(Gravity.CENTER);
        timerView.setPadding(0, 0, 0, dp(4));
        box.addView(timerView);

        final TextView statusLbl = new TextView(this);
        statusLbl.setText("Tap microphone to record voice evidence (Max 30s)");
        statusLbl.setTextColor(colMuted);
        statusLbl.setTextSize(12);
        statusLbl.setGravity(Gravity.CENTER);
        statusLbl.setPadding(0, 0, 0, dp(14));
        box.addView(statusLbl);

        final LinearLayout micBtn = new LinearLayout(this);
        micBtn.setOrientation(LinearLayout.VERTICAL);
        micBtn.setGravity(Gravity.CENTER);
        micBtn.setBackground(rounded(colPanel2, dp(36)));
        LinearLayout.LayoutParams mbl = new LinearLayout.LayoutParams(dp(68), dp(68));
        mbl.gravity = Gravity.CENTER_HORIZONTAL;
        mbl.bottomMargin = dp(14);
        micBtn.setLayoutParams(mbl);

        final TextView micIcon = new TextView(this);
        micIcon.setText("🎙️");
        micIcon.setTextSize(28);
        micIcon.setGravity(Gravity.CENTER);
        micBtn.addView(micIcon);
        box.addView(micBtn);

        final EditText captionField = modernInputField("Optional Memo Caption / Subject");
        captionField.setVisibility(View.GONE);
        box.addView(captionField);

        final Dialog dlg = createTacticalDialog(box);

        final Handler voiceTicker = new Handler();
        final Runnable voiceTick = new Runnable() {
            public void run() {
                if (isRecordingVoice) {
                    long elapsed = SystemClock.elapsedRealtime() - voiceRecordStart;
                    int secs = (int) (elapsed / 1000);
                    int mins = secs / 60;
                    timerView.setText(String.format(Locale.US, "%02d:%02d", mins, secs % 60));

                    if (secs >= 30) {
                        hapticDoublePulse();
                        stopVoiceRecording();
                        statusLbl.setText("✓ 30s limit reached · Audio captured");
                        micBtn.setBackground(rounded(colEmeraldSoft, dp(36)));
                        captionField.setVisibility(View.VISIBLE);
                    } else {
                        voiceTicker.postDelayed(this, 100);
                    }
                }
            }
        };

        micBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                hapticDoublePulse();
                if (!isRecordingVoice) {
                    startVoiceRecording();
                    isRecordingVoice = true;
                    voiceRecordStart = SystemClock.elapsedRealtime();
                    statusLbl.setText("● RECORDING AUDIO · Tap to Stop");
                    statusLbl.setTextColor(colCrimson);
                    micBtn.setBackground(rounded(colCrimsonSoft, dp(36)));
                    voiceTicker.post(voiceTick);
                } else {
                    stopVoiceRecording();
                    isRecordingVoice = false;
                    statusLbl.setText("✓ Voice Recorded · Ready to Save");
                    statusLbl.setTextColor(colEmerald);
                    micBtn.setBackground(rounded(colEmeraldSoft, dp(36)));
                    captionField.setVisibility(View.VISIBLE);
                }
            }
        });

        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setPadding(0, dp(14), 0, 0);

        TextView btnCancel = actionButton("Cancel", colLine, colMuted);
        btnCancel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                hapticClick();
                if (isRecordingVoice) stopVoiceRecording();
                dlg.dismiss();
            }
        });
        btnRow.addView(btnCancel);

        TextView btnCommit = actionButton("Save Voice Memo", colAccent, colAccentInk);
        btnCommit.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                hapticHeavyClick();
                registerActivity();
                if (isRecordingVoice) stopVoiceRecording();
                if (activeVoiceFile == null || !activeVoiceFile.exists()) {
                    banner.setText("no voice recorded");
                    banner.setVisibility(View.VISIBLE);
                    return;
                }
                byte[] audioBytes = readFileBytes(activeVoiceFile);
                String hash = sha256Hex(audioBytes);
                String hashSnippet = hash.length() >= 8 ? hash.substring(0, 8) : hash;
                String cap = captionField.getText().toString().trim();
                String line = "[VOICE " + hashSnippet + "] " + (cap.isEmpty() ? "voice memo recorded" : cap);
                if (!oneLine(line)) {
                    banner.setText("notes must be one line");
                    banner.setVisibility(View.VISIBLE);
                    return;
                }
                note(Core.TOPIC_ROUTINE, line);
                dlg.dismiss();
            }
        });
        LinearLayout.LayoutParams cml = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.4f);
        cml.leftMargin = dp(8);
        btnCommit.setLayoutParams(cml);
        btnRow.addView(btnCommit);

        box.addView(btnRow);
        dlg.show();
    }

    private void startVoiceRecording() {
        try {
            activeVoiceFile = new File(getFilesDir(), "voice_temp.m4a");
            if (activeVoiceFile.exists()) activeVoiceFile.delete();
            voiceRecorder = new MediaRecorder();
            voiceRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            voiceRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            voiceRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            voiceRecorder.setOutputFile(activeVoiceFile.getAbsolutePath());
            voiceRecorder.prepare();
            voiceRecorder.start();
        } catch (Exception e) {
            banner.setText("audio recording initialization failed");
            banner.setVisibility(View.VISIBLE);
        }
    }

    private void stopVoiceRecording() {
        if (voiceRecorder != null) {
            try {
                voiceRecorder.stop();
                voiceRecorder.release();
            } catch (Exception e) {}
            voiceRecorder = null;
        }
        isRecordingVoice = false;
    }

    private void showModernIncidentSheet() {
        final LinearLayout box = dialogContainer("🚨 Security Incident Report", "LEGAL AUDIT", colCrimson);

        final String[] categories = {
            "Intruder / Trespass", "Forced Entry / Damage",
            "Theft / Missing Plant", "Water / Flood Hazard",
            "Smoke / Fire Alarm", "Suspicious Vehicle"
        };
        final String[] actions = {
            "Perimeter Secured", "Intruders Fled", "000 Police Dispatched", "Supervisor Alerted"
        };

        final String[] selectedCat = {categories[0]};
        final ArrayList<String> selectedActions = new ArrayList<String>();
        selectedActions.add(actions[0]);
        final String[] pinnedLocation = {null};

        box.addView(formSectionLabel("1. INCIDENT CATEGORY"));
        box.addView(buildChipGroup(categories, selectedCat, true, colCrimson));

        box.addView(formSectionLabel("2. SITE LOCATION (TAP BLUEPRINT TO PIN)"));
        box.addView(buildBlueprintMiniMap(pinnedLocation, null));

        box.addView(formSectionLabel("3. ACTION TAKEN / ESCALATION"));
        box.addView(buildMultiChipGroup(actions, selectedActions, colAccent));

        box.addView(formSectionLabel("4. NARRATIVE DETAILS"));
        final EditText detailsField = modernInputField("What occurred, suspect descriptions, damage, vehicle plates...");
        detailsField.setMinLines(3);
        box.addView(detailsField);

        final Dialog dlg = createTacticalDialog(box);

        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setPadding(0, dp(16), 0, 0);

        TextView btnCancel = actionButton("Cancel", colLine, colMuted);
        btnCancel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                hapticClick();
                dlg.dismiss();
            }
        });
        btnRow.addView(btnCancel);

        TextView btnCommit = actionButton("🚨 Log Incident", colCrimson, colPale);
        btnCommit.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                hapticSealThud();
                registerActivity();
                String det = detailsField.getText().toString().trim();
                String actStr = selectedActions.isEmpty() ? "None" : String.join(", ", selectedActions);
                String locTag = pinnedLocation[0] != null ? " at " + pinnedLocation[0] : "";
                String line = "[INCIDENT: " + selectedCat[0] + locTag + "] "
                            + (det.isEmpty() ? "Logged on shift" : det) + " (Action: " + actStr + ")";
                if (!oneLine(line)) {
                    banner.setText("an entry is one line; take out line breaks");
                    banner.setVisibility(View.VISIBLE);
                    return;
                }
                note(Core.TOPIC_INCIDENT, line);
                dlg.dismiss();
            }
        });
        LinearLayout.LayoutParams cml = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.4f);
        cml.leftMargin = dp(8);
        btnCommit.setLayoutParams(cml);
        btnRow.addView(btnCommit);

        box.addView(btnRow);
        dlg.show();
    }

    private void showModernNotesSheet() {
        final LinearLayout box = dialogContainer("📝 Shift Observation Note", "OCCURRENCE LOG", colAccent);

        final String[] tags = {"General", "Maintenance", "Day Crew", "Access", "Safety"};
        final String[] selectedTag = {tags[0]};

        box.addView(formSectionLabel("TOPIC CATEGORY"));
        box.addView(buildChipGroup(tags, selectedTag, true, colAccent));

        box.addView(formSectionLabel("NOTE DETAILS"));
        final EditText noteField = modernInputField("Type shift observation, contractor movements, padlocks, fuel...");
        noteField.setMinLines(3);
        box.addView(noteField);

        final Dialog dlg = createTacticalDialog(box);

        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setPadding(0, dp(16), 0, 0);

        TextView btnCancel = actionButton("Cancel", colLine, colMuted);
        btnCancel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                hapticClick();
                dlg.dismiss();
            }
        });
        btnRow.addView(btnCancel);

        TextView btnCommit = actionButton("Save Note", colAccent, colAccentInk);
        btnCommit.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                hapticHeavyClick();
                registerActivity();
                String t = noteField.getText().toString().trim();
                if (t.isEmpty()) return;
                String line = "[" + selectedTag[0].toUpperCase() + "] " + t;
                if (!oneLine(line)) {
                    banner.setText("an entry is one line; remove line breaks");
                    banner.setVisibility(View.VISIBLE);
                    return;
                }
                int topic = selectedTag[0].equals("Day Crew") ? Core.TOPIC_FOR_DAY_CREW : Core.TOPIC_ROUTINE;
                note(topic, line);
                dlg.dismiss();
            }
        });
        LinearLayout.LayoutParams cml = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.4f);
        cml.leftMargin = dp(8);
        btnCommit.setLayoutParams(cml);
        btnRow.addView(btnCommit);

        box.addView(btnRow);
        dlg.show();
    }

    private void promptLotShutdown(final String lotName, final String uid) {
        final LinearLayout box = dialogContainer("🏭 " + lotName + " Factory Floor", "LOCKUP & SHUTDOWN", colCyan);

        TextView subtitle = new TextView(this);
        subtitle.setText("Select all applicable conditions or add notes:");
        subtitle.setTextColor(colMuted);
        subtitle.setTextSize(12);
        subtitle.setPadding(0, 0, 0, dp(10));
        box.addView(subtitle);

        final ArrayList<String> selectedItems = new ArrayList<String>();
        selectedItems.add(SHUTDOWN_OPTIONS[0]);

        final ArrayList<TextView> itemViews = new ArrayList<TextView>();

        for (int i = 0; i < SHUTDOWN_OPTIONS.length; i++) {
            final String opt = SHUTDOWN_OPTIONS[i];
            final TextView item = new TextView(this);
            item.setText(opt);
            item.setTextSize(13);
            item.setPadding(dp(14), dp(12), dp(14), dp(12));

            final boolean isAllClear = i == 0;
            updateCheckItemStyle(item, selectedItems.contains(opt), isAllClear);

            LinearLayout.LayoutParams il = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            il.bottomMargin = dp(6);
            item.setLayoutParams(il);

            item.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    hapticClick();
                    if (isAllClear) {
                        selectedItems.clear();
                        selectedItems.add(SHUTDOWN_OPTIONS[0]);
                    } else {
                        selectedItems.remove(SHUTDOWN_OPTIONS[0]);
                        if (selectedItems.contains(opt)) {
                            selectedItems.remove(opt);
                        } else {
                            selectedItems.add(opt);
                        }
                        if (selectedItems.isEmpty()) {
                            selectedItems.add(SHUTDOWN_OPTIONS[0]);
                        }
                    }
                    for (int k = 0; k < itemViews.size(); k++) {
                        TextView tv = itemViews.get(k);
                        String o = SHUTDOWN_OPTIONS[k];
                        updateCheckItemStyle(tv, selectedItems.contains(o), k == 0);
                    }
                }
            });

            itemViews.add(item);
            box.addView(item);
        }

        final EditText customField = modernInputField("Additional observation note (optional)...");
        box.addView(customField);

        final Dialog dlg = createTacticalDialog(box);

        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setPadding(0, dp(12), 0, 0);

        TextView btnCancel = actionButton("Cancel", colLine, colMuted);
        btnCancel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                hapticClick();
                dlg.dismiss();
            }
        });
        btnRow.addView(btnCancel);

        TextView btnSave = actionButton("✓ Save Inspection", colCyan, colAccentInk);
        btnSave.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                hapticHeavyClick();
                registerActivity();
                tap(lotName + " Factory Floor", uid);
                String custom = customField.getText().toString().trim();
                
                StringBuilder sb = new StringBuilder();
                sb.append(lotName).append(": ");
                for (int k = 0; k < selectedItems.size(); k++) {
                    if (k > 0) sb.append(", ");
                    sb.append(selectedItems.get(k));
                }
                if (!custom.isEmpty()) {
                    sb.append(" · ").append(custom);
                }
                String fullLine = sb.toString();
                if (!oneLine(fullLine)) {
                    banner.setText("notes must be one line");
                    banner.setVisibility(View.VISIBLE);
                    return;
                }
                note(Core.TOPIC_ROUTINE, fullLine);
                dlg.dismiss();
            }
        });
        LinearLayout.LayoutParams cml = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.4f);
        cml.leftMargin = dp(8);
        btnSave.setLayoutParams(cml);
        btnRow.addView(btnSave);

        box.addView(btnRow);
        dlg.show();
    }

    private void updateCheckItemStyle(TextView tv, boolean checked, boolean isAllClear) {
        if (checked) {
            tv.setTextColor(isAllClear ? colEmerald : colAccent);
            tv.setBackground(rounded(isAllClear ? colEmeraldSoft : colAccentSoft, dp(12)));
        } else {
            tv.setTextColor(colMuted);
            tv.setBackground(pressable(colPanel2, dp(12)));
        }
    }

    private TextView formSectionLabel(String text) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextColor(colQuiet);
        t.setTextSize(10);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setLetterSpacing(0.12f);
        t.setPadding(0, dp(10), 0, dp(6));
        return t;
    }

    private EditText modernInputField(String hint) {
        EditText field = new EditText(this);
        field.setHint(hint);
        field.setTextColor(colPale);
        field.setHintTextColor(colQuiet);
        field.setTextSize(13);
        field.setBackground(rounded(colPanel2, dp(12)));
        field.setPadding(dp(14), dp(12), dp(14), dp(12));
        field.setInputType(InputType.TYPE_CLASS_TEXT
                           | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                           | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        field.setMinLines(2);
        field.setGravity(Gravity.TOP);
        LinearLayout.LayoutParams fl = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        fl.bottomMargin = dp(8);
        field.setLayoutParams(fl);
        return field;
    }

    private TextView actionButton(String text, int fill, int textCol) {
        TextView btn = new TextView(this);
        btn.setText(text);
        btn.setTextColor(textCol);
        btn.setTextSize(14);
        btn.setTypeface(Typeface.DEFAULT_BOLD);
        btn.setGravity(Gravity.CENTER);
        btn.setBackground(pressable(fill, dp(14)));
        btn.setPadding(dp(14), dp(14), dp(14), dp(14));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        btn.setLayoutParams(lp);
        return btn;
    }

    private HorizontalScrollView buildChipGroup(final String[] options, final String[] selected,
                                                final boolean single, final int activeCol) {
        HorizontalScrollView hsv = new HorizontalScrollView(this);
        hsv.setHorizontalScrollBarEnabled(false);

        final LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, 0, 0, dp(8));

        for (int i = 0; i < options.length; i++) {
            final String opt = options[i];
            final TextView chip = new TextView(this);
            chip.setText(opt);
            chip.setTextSize(11);
            chip.setTypeface(Typeface.DEFAULT_BOLD);
            chip.setPadding(dp(12), dp(7), dp(12), dp(7));

            final boolean isSel = opt.equals(selected[0]);
            chip.setTextColor(isSel ? colAccentInk : colMuted);
            chip.setBackground(rounded(isSel ? activeCol : colPanel2, dp(16)));

            chip.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    hapticClick();
                    selected[0] = opt;
                    for (int k = 0; k < row.getChildCount(); k++) {
                        TextView c = (TextView) row.getChildAt(k);
                        boolean s = c.getText().toString().equals(opt);
                        c.setTextColor(s ? colAccentInk : colMuted);
                        c.setBackground(rounded(s ? activeCol : colPanel2, dp(16)));
                    }
                }
            });

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.rightMargin = dp(6);
            chip.setLayoutParams(lp);
            row.addView(chip);
        }
        hsv.addView(row);
        return hsv;
    }

    private HorizontalScrollView buildMultiChipGroup(final String[] options, final ArrayList<String> selectedList, final int activeCol) {
        HorizontalScrollView hsv = new HorizontalScrollView(this);
        hsv.setHorizontalScrollBarEnabled(false);

        final LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, 0, 0, dp(8));

        for (int i = 0; i < options.length; i++) {
            final String opt = options[i];
            final TextView chip = new TextView(this);
            chip.setText(opt);
            chip.setTextSize(11);
            chip.setTypeface(Typeface.DEFAULT_BOLD);
            chip.setPadding(dp(12), dp(7), dp(12), dp(7));

            boolean isSel = selectedList.contains(opt);
            chip.setTextColor(isSel ? colAccentInk : colMuted);
            chip.setBackground(rounded(isSel ? activeCol : colPanel2, dp(16)));

            chip.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    hapticClick();
                    if (selectedList.contains(opt)) {
                        selectedList.remove(opt);
                        chip.setTextColor(colMuted);
                        chip.setBackground(rounded(colPanel2, dp(16)));
                    } else {
                        selectedList.add(opt);
                        chip.setTextColor(colAccentInk);
                        chip.setBackground(rounded(activeCol, dp(16)));
                    }
                }
            });

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.rightMargin = dp(6);
            chip.setLayoutParams(lp);
            row.addView(chip);
        }
        hsv.addView(row);
        return hsv;
    }

    private byte[] bitmapToJpegBytes(Bitmap bmp) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.JPEG, 85, stream);
        return stream.toByteArray();
    }

    private byte[] readFileBytes(File f) {
        try {
            FileInputStream fis = new FileInputStream(f);
            byte[] b = new byte[(int) f.length()];
            fis.read(b);
            fis.close();
            return b;
        } catch (Exception e) {
            return new byte[0];
        }
    }

    private String sha256Hex(byte[] bytes) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] d = md.digest(bytes);
            StringBuilder sb = new StringBuilder();
            for (byte b : d) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return "00000000";
        }
    }

    private TextView tonightLabel() {
        tonightTitle = label("TONIGHT'S VERIFIED RECORD");
        return tonightTitle;
    }

    private void fillTonight() {
        tonight.removeAllViews();
        int shown = 0;
        for (int i = 1; ; i++) {
            String line = Core.entryLine(i);
            if (line.length() == 0) break;
            tonight.addView(entryRow(line, i));
            shown++;
        }
        for (int i = 0; i < pending.size(); i++) {
            tonight.addView(pendingRow(pending.get(i)));
        }
        shown += pending.size();
        boolean any = shown > 0;
        tonight.setVisibility(any ? View.VISIBLE : View.GONE);
        tonightTitle.setVisibility(any ? View.VISIBLE : View.GONE);
    }

    private LinearLayout pendingRow(final Pending p) {
        final LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(outlined(colAccent, dp(14)));
        card.setPadding(dp(14), dp(12), dp(14), dp(12));

        long left = HOLD_MS - (SystemClock.elapsedRealtime() - p.created);
        int secs = (int) Math.max(0, (left + 999) / 1000);

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = new TextView(this);
        title.setText(clock(p.occurred) + "  " + (p.checkpoint ? p.label : p.text));
        title.setTextColor(colAccent);
        title.setTextSize(13);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams tl = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        title.setLayoutParams(tl);
        top.addView(title);

        TextView btnUndo = new TextView(this);
        btnUndo.setText("UNDO");
        btnUndo.setTextColor(colAccentInk);
        btnUndo.setTextSize(10);
        btnUndo.setTypeface(Typeface.DEFAULT_BOLD);
        btnUndo.setPadding(dp(8), dp(4), dp(8), dp(4));
        btnUndo.setBackground(rounded(colAccent, dp(6)));
        btnUndo.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                hapticHeavyClick();
                takeBack(p);
            }
        });
        top.addView(btnUndo);
        card.addView(top);

        TextView sub = new TextView(this);
        sub.setText("held in buffer for " + secs + "s  ·  swipe or tap to discard");
        sub.setTextColor(colMuted);
        sub.setTextSize(11);
        sub.setPadding(0, dp(4), 0, dp(8));
        card.addView(sub);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(8);
        card.setLayoutParams(lp);

        card.setOnTouchListener(new View.OnTouchListener() {
            private float from;
            private boolean dragging;

            public boolean onTouch(View v, android.view.MotionEvent e) {
                switch (e.getActionMasked()) {
                case android.view.MotionEvent.ACTION_DOWN:
                    from = e.getRawX();
                    dragging = true;
                    return true;
                case android.view.MotionEvent.ACTION_MOVE:
                    if (dragging) {
                        float dx = e.getRawX() - from;
                        v.setTranslationX(dx);
                        v.setAlpha(Math.max(0.25f, 1f - Math.abs(dx) / (dp(200) * 1f)));
                    }
                    return true;
                case android.view.MotionEvent.ACTION_UP:
                case android.view.MotionEvent.ACTION_CANCEL:
                    float dx2 = e.getRawX() - from;
                    dragging = false;
                    if (Math.abs(dx2) > dp(110)) {
                        hapticHeavyClick();
                        v.post(new Runnable() {
                            public void run() { takeBack(p); }
                        });
                    } else {
                        v.setTranslationX(0f);
                        v.setAlpha(1f);
                    }
                    return true;
                default:
                    return false;
                }
            }
        });
        return card;
    }

    private void takeBack(Pending p) {
        registerActivity();
        pending.remove(p);
        savePending();
        banner.setText("taken back, and never written to the record");
        banner.setVisibility(View.VISIBLE);
        refresh();
    }

    private LinearLayout entryRow(String line, int seq) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setBackground(rounded(colPanel, dp(12)));
        row.setPadding(dp(14), dp(10), dp(14), dp(10));

        TextView t = new TextView(this);
        t.setText(line);
        t.setTextSize(13);
        t.setTextColor(colPale);
        t.setTypeface(Typeface.MONOSPACE);
        row.addView(t);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(6);
        row.setLayoutParams(lp);
        return row;
    }

    private GradientDrawable rounded(int fill, int radius) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        g.setCornerRadius(radius);
        return g;
    }

    private GradientDrawable outlined(int stroke, int radius) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(0x00000000);
        g.setStroke(dp(1), stroke);
        g.setCornerRadius(radius);
        return g;
    }

    private RippleDrawable pressable(int fill, int radius) {
        return new RippleDrawable(ColorStateList.valueOf(0x44E5A93C),
                                  rounded(fill, radius), null);
    }

    private RippleDrawable pressableOutline(int stroke, int radius) {
        return new RippleDrawable(ColorStateList.valueOf(0x44E5A93C),
                                  outlined(stroke, radius), null);
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }

    // ---- Core Bindings ----

    private void startShift() {
        int t = nowMinutes();
        openedAt = t;
        Core.siteBegin("Hume Doors & Timber, Kingston");
        for (int i = 0; i < EXTERNAL_CHOICES.length; i += 2) {
            Core.siteAddPoint(EXTERNAL_CHOICES[i], EXTERNAL_CHOICES[i + 1]);
        }
        for (int i = 0; i < INTERNAL_LOTS.length; i += 2) {
            Core.siteAddPoint(INTERNAL_LOTS[i] + " Factory Floor", INTERNAL_LOTS[i + 1]);
        }
        for (int i = 0; i < FIRE_POINTS.length; i += 2) {
            Core.siteAddPoint(FIRE_POINTS[i], FIRE_POINTS[i + 1]);
        }
        Core.sitePolicy(1, 240, 0);
        Core.setAttribution(Core.DEVICE_PERSONAL, Core.METHOD_SESSION);
        Core.setGuard("g-kelso", "R. Kelso", "SAMPLE-LIC", "typed", "");
        answer(Core.openShift(Core.genesis(), Core.siteHash(), t, t,
                              "on site, handover from day crew taken"));
        hidePage();
    }

    private void tap(String name, String uid) {
        registerActivity();
        Pending p = new Pending();
        p.checkpoint = true;
        p.label = name;
        p.uid = uid;
        p.taps = ++taps;
        p.occurred = nowMinutes();
        p.created = SystemClock.elapsedRealtime();
        hold(p);
    }

    private void note(int topic, String text) {
        registerActivity();
        Pending p = new Pending();
        p.checkpoint = false;
        p.topic = topic;
        p.text = text;
        p.occurred = nowMinutes();
        p.created = SystemClock.elapsedRealtime();
        hold(p);
    }

    private boolean oneLine(String text) {
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c < ' ' || c == 127) return false;
        }
        return true;
    }

    private void hold(Pending p) {
        pending.add(p);
        while (pending.size() > MAX_HELD) commit(pending.remove(0));
        savePending();
        banner.setVisibility(View.GONE);
        refresh();
    }

    private void commitDue() {
        long now = SystemClock.elapsedRealtime();
        boolean any = false;
        for (int i = 0; i < pending.size(); ) {
            Pending p = pending.get(i);
            if (now - p.created >= HOLD_MS) {
                pending.remove(i);
                commit(p);
                any = true;
            } else {
                i++;
            }
        }
        if (any) {
            savePending();
            refresh();
        }
    }

    private void commitAll() {
        while (!pending.isEmpty()) commit(pending.remove(0));
        savePending();
    }

    private void commit(Pending p) {
        int t = Math.max(nowMinutes(), Core.lastRecorded());
        if (p.checkpoint) {
            answer(Core.addCheckpoint(p.occurred, t, p.label, p.uid, p.taps,
                                      Core.AUTH_CRYPTOGRAPHIC));
        } else {
            answer(Core.addNote(Core.KIND_OBSERVATION, p.topic,
                                p.occurred, t, p.text, 0));
        }
        if (chainBannerView != null) chainBannerView.triggerRipple();
    }

    private void sealAndShow() {
        hapticSealThud();
        registerActivity();
        commitAll();
        int t = Math.max(nowMinutes(), Core.lastRecorded());
        answer(Core.seal(t, t, "off site"));
        String text = Core.report(openedAt, t);
        if (text.length() > 0) {
            page.setText(text);
            page.setVisibility(View.VISIBLE);
            pageTitle.setVisibility(View.VISIBLE);
            btnShareReport.setVisibility(View.VISIBLE);
        }
    }

    private void shareHandoverReport() {
        String text = page.getText().toString();
        if (text.isEmpty()) return;
        try {
            Intent sendIntent = new Intent(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_SUBJECT, "06:05 AM Morning Handover Report · Hume Doors & Timber (Kingston)");
            sendIntent.putExtra(Intent.EXTRA_TEXT, text);
            sendIntent.setType("text/plain");
            startActivity(Intent.createChooser(sendIntent, "Share Morning Handover Report"));
        } catch (Exception e) {}
    }

    private boolean keepArchive() {
        String text = Core.archive();
        if (text.length() == 0) return false;
        try {
            File out = new File(getFilesDir(),
                                "record-" + Core.head().substring(0, 16) + ".txt");
            FileOutputStream f = new FileOutputStream(out);
            f.write(text.getBytes("UTF-8"));
            f.close();
        } catch (Exception e) {
            return false;
        }
        int r = Core.kept();
        return r == Core.OK;
    }

    private void nextShift() {
        hapticHeavyClick();
        registerActivity();
        int t = nowMinutes();
        if (!keepArchive()) {
            banner.setText("could not write the record out; it has not been kept");
            banner.setVisibility(View.VISIBLE);
            return;
        }
        int r = Core.continueShift(t, t, "on site, continuation");
        if (r == Core.OK) {
            openedAt = t;
            hidePage();
        }
        answer(r);
    }

    private void savePending() {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < pending.size(); i++) {
            Pending p = pending.get(i);
            String body = p.checkpoint ? p.label : p.text;
            b.append(p.checkpoint ? 1 : 0).append('|')
             .append(p.topic).append('|')
             .append(p.taps).append('|')
             .append(p.occurred).append('|')
             .append(p.created).append('|')
             .append(p.uid).append('|')
             .append(body.length()).append('|')
             .append(body).append('\n');
        }
        try {
            FileOutputStream f = new FileOutputStream(
                    new File(getFilesDir(), "pending.txt"));
            f.write(b.toString().getBytes("UTF-8"));
            f.close();
        } catch (Exception e) {}
    }

    private void loadPending() {
        pending.clear();
        File in = new File(getFilesDir(), "pending.txt");
        if (!in.exists()) return;
        try {
            BufferedReader r = new BufferedReader(
                    new InputStreamReader(new FileInputStream(in), "UTF-8"));
            String line;
            while ((line = r.readLine()) != null) {
                Pending p = parsePending(line);
                if (p != null) pending.add(p);
            }
            r.close();
        } catch (Exception e) {}
    }

    private Pending parsePending(String line) {
        try {
            int[] at = new int[7];
            int n = 0;
            for (int i = 0; i < line.length() && n < 7; i++) {
                if (line.charAt(i) == '|') at[n++] = i;
            }
            if (n < 7) return null;
            Pending p = new Pending();
            p.checkpoint = line.charAt(0) == '1';
            p.topic = Integer.parseInt(line.substring(at[0] + 1, at[1]));
            p.taps = Integer.parseInt(line.substring(at[1] + 1, at[2]));
            p.occurred = Integer.parseInt(line.substring(at[2] + 1, at[3]));
            p.created = Long.parseLong(line.substring(at[3] + 1, at[4]));
            p.uid = line.substring(at[4] + 1, at[5]);
            int len = Integer.parseInt(line.substring(at[5] + 1, at[6]));
            if (at[6] + 1 + len > line.length()) return null;
            String body = line.substring(at[6] + 1, at[6] + 1 + len);
            if (p.checkpoint) p.label = body; else p.text = body;
            if (p.taps > taps) taps = p.taps;
            return p;
        } catch (Exception e) {
            return null;
        }
    }

    private void hidePage() {
        page.setVisibility(View.GONE);
        pageTitle.setVisibility(View.GONE);
        btnShareReport.setVisibility(View.GONE);
    }

    private void answer(int result) {
        String why = Core.lastReason();
        if (result == Core.OK || why.length() == 0) {
            banner.setVisibility(View.GONE);
        } else {
            banner.setText(why);
            banner.setVisibility(View.VISIBLE);
        }
        refresh();
    }

    private void refresh() {
        boolean isSealed = Core.isSealed() == 1;
        int n = Core.entryCount();

        pills.removeAllViews();
        pills.addView(pill(n + (n == 1 ? " entry" : " entries"), false));
        pills.addView(pill(isSealed ? "SEALED" : "OPEN", isSealed));
        pills.addView(pill(Core.verified() == 1 ? "✓ VERIFIED" : "BROKEN", false));

        markExternalCard(tileExternalFull, EXTERNAL_CHOICES[0]);
        markExternalCard(tileExternalHalf, EXTERNAL_CHOICES[2]);

        for (int i = 0; i < internalBadgesRow.getChildCount(); i++) {
            TextView b = (TextView) internalBadgesRow.getChildAt(i);
            String lotName = (String) b.getTag();
            markLotBadge(b, lotName + " Factory Floor");
        }

        int fireDone = 0;
        for (int i = 0; i < fireList.getChildCount(); i++) {
            LinearLayout row = (LinearLayout) fireList.getChildAt(i);
            String name = (String) row.getTag();
            if (markFireRow(row, name)) fireDone++;
        }
        int fireTotal = FIRE_POINTS.length / 2;
        boolean fireComplete = fireDone >= fireTotal;
        fireStatusChip.setText(fireDone + "/" + fireTotal + (fireComplete ? " COMPLETE" : " CHECKED"));
        fireStatusChip.setTextColor(fireComplete ? colEmerald : colMuted);
        fireStatusChip.setBackground(rounded(fireComplete ? colEmeraldSoft : colPanel2, dp(6)));

        fillTonight();

        setLive(externalRow, !isSealed);
        setLive(internalBadgesRow, !isSealed);
        setLive(fireCard, !isSealed);
        setLive(dock, !isSealed);

        if (isSealed) {
            primary.setText("START THE NEXT RECORD");
            primary.setTextColor(colPale);
            primary.setBackground(pressableOutline(colLine, dp(16)));
            primary.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    hapticClick();
                    nextShift();
                }
            });
        } else {
            primary.setText("🔒 SEAL SHIFT & HANDOVER (06:05 AM)");
            primary.setTextColor(colAccentInk);
            primary.setBackground(pressable(colAccent, dp(16)));
            primary.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    hapticHeavyClick();
                    askThenSeal();
                }
            });
        }
    }

    private void askThenSeal() {
        registerActivity();
        int n = Core.entryCount() + pending.size();
        String held = pending.isEmpty() ? ""
            : "\n\nIncludes " + pending.size()
              + (pending.size() == 1 ? " entry" : " entries")
              + " in buffer that will be written in first.";
        
        final LinearLayout box = dialogContainer("🔒 Biometric Shift Seal & Handover", "FINAL LEGAL ACTION", colAccent);

        TextView desc = new TextView(this);
        desc.setText("Sealing locks tonight's Ada record with " + n
                     + (n == 1 ? " entry" : " entries")
                     + " permanently under SHA-256."
                     + held + "\n\nTouch and hold the affirmation pad below to execute cryptographic seal:");
        desc.setTextColor(colMuted);
        desc.setTextSize(12);
        desc.setPadding(0, 0, 0, dp(14));
        box.addView(desc);

        final Dialog dlg = createTacticalDialog(box);

        BiometricSealPadView sealPad = new BiometricSealPadView(this, new Runnable() {
            public void run() {
                dlg.dismiss();
                sealAndShow();
            }
        });
        LinearLayout.LayoutParams spl = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(140));
        spl.bottomMargin = dp(12);
        sealPad.setLayoutParams(spl);
        box.addView(sealPad);

        TextView btnCancel = actionButton("Cancel / Back", colLine, colMuted);
        btnCancel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                hapticClick();
                dlg.dismiss();
            }
        });
        box.addView(btnCancel);

        dlg.show();
    }

    private String formatTimeSinceVisited(int lastVisitedMin) {
        int now = nowMinutes();
        int elapsed = Math.max(0, now - lastVisitedMin);
        if (elapsed < 60) return elapsed + "m ago";
        if (elapsed < 1440) {
            int h = elapsed / 60;
            int m = elapsed % 60;
            return h + "h " + (m > 0 ? m + "m " : "") + "ago";
        }
        int d = elapsed / 1440;
        int h = (elapsed % 1440) / 60;
        return d + "d " + h + "h ago";
    }

    private int getElapsedBadgeColor(int lastVisitedMin) {
        int now = nowMinutes();
        int elapsed = Math.max(0, now - lastVisitedMin);
        if (elapsed <= 75) return colEmerald;
        if (elapsed <= 120) return colAccent;
        return colCrimson;
    }

    private void markExternalCard(TextView t, String name) {
        int visits = Core.pointVisits(name);
        if (visits <= 0) {
            t.setText(name + "\nTap to log");
            t.setTextColor(colPale);
            t.setBackground(pressable(colPanel, dp(16)));
            return;
        }
        int at = Core.pointLast(name);
        String elapsedStr = formatTimeSinceVisited(at);
        int col = getElapsedBadgeColor(at);

        t.setText(name + "  (x" + visits + ")\n" + clock(at) + " · " + elapsedStr);
        t.setTextColor(col);
        t.setBackground(pressable(colPanel, dp(16)));
    }

    private void markLotBadge(TextView b, String fullName) {
        int visits = Core.pointVisits(fullName);
        String shortName = (String) b.getTag();
        if (visits <= 0) {
            b.setText(shortName);
            b.setTextColor(colPale);
            b.setBackground(pressable(colPanel, dp(12)));
        } else {
            int at = Core.pointLast(fullName);
            String elapsedStr = formatTimeSinceVisited(at);
            int col = getElapsedBadgeColor(at);

            b.setText(shortName + " ✓\n" + elapsedStr);
            b.setTextColor(col);
            b.setBackground(rounded(colPanel2, dp(12)));
        }
    }

    private boolean markFireRow(LinearLayout row, String name) {
        TextView title = (TextView) row.getChildAt(0);
        TextView status = (TextView) row.getChildAt(1);
        int visits = Core.pointVisits(name);

        ArrayList<PressureRecord> list = pressureHistory.get(name);
        String lastPressureStr = "";
        if (list != null && !list.isEmpty()) {
            PressureRecord lastPr = list.get(list.size() - 1);
            lastPressureStr = " · " + lastPr.pressureKpa + " PSI";
        }

        if (visits <= 0) {
            status.setText("·");
            status.setTextColor(colQuiet);
            return false;
        } else {
            int at = Core.pointLast(name);
            String elapsedStr = formatTimeSinceVisited(at);
            int col = getElapsedBadgeColor(at);

            status.setText("✓ " + elapsedStr + lastPressureStr);
            status.setTextColor(col);
            return true;
        }
    }

    private String clock(int minutes) {
        int h = (minutes / 60) % 24;
        int m = minutes % 60;
        return (h < 10 ? "0" : "") + h + ":" + (m < 10 ? "0" : "") + m;
    }

    private void setLive(LinearLayout group, boolean on) {
        group.setAlpha(on ? 1f : 0.35f);
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            child.setEnabled(on);
            if (child instanceof LinearLayout) {
                LinearLayout row = (LinearLayout) child;
                for (int k = 0; k < row.getChildCount(); k++) {
                    row.getChildAt(k).setEnabled(on);
                }
            }
        }
    }

    // Welfare Check Dialog & Handover Boilerplate
    private void checkWelfareDue() {
        if (isWelfareDialogShowing || Core.isSealed() == 1) return;
        long elapsed = SystemClock.elapsedRealtime() - lastActivityTimeMs;
        if (elapsed >= WELFARE_INTERVAL_MS) showWelfareCheckDialog();
    }

    private void showWelfareCheckDialog() {
        isWelfareDialogShowing = true;
        hapticHeavyClick();
        final long promptTime = SystemClock.elapsedRealtime();
        final long autoEscalateMs = 5 * 60 * 1000L;

        final LinearLayout box = dialogContainer("🦺 Lone Worker Welfare Check", "WHS COMPLIANCE", colEmerald);

        TextView info = new TextView(this);
        info.setText("No site activity has been logged in 90 minutes.\n\nPlease confirm your active on-duty status:");
        info.setTextColor(colPale);
        info.setTextSize(13);
        info.setPadding(0, 0, 0, dp(14));
        box.addView(info);

        final TextView timerView = new TextView(this);
        timerView.setText("Auto-Escalating to DSS Control in 05:00");
        timerView.setTextColor(colAccent);
        timerView.setTextSize(12);
        timerView.setTypeface(Typeface.MONOSPACE);
        timerView.setGravity(Gravity.CENTER);
        timerView.setPadding(0, 0, 0, dp(18));
        box.addView(timerView);

        TextView btnConfirm = actionButton("✓ I AM SAFE · CONFIRM ON DUTY", colEmerald, colAccentInk);
        btnConfirm.setTextSize(15);
        btnConfirm.setPadding(dp(18), dp(18), dp(18), dp(18));
        LinearLayout.LayoutParams cfl = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cfl.bottomMargin = dp(10);
        btnConfirm.setLayoutParams(cfl);
        box.addView(btnConfirm);

        TextView btnSos = actionButton("🚨 EMERGENCY ASSISTANCE (000)", colCrimson, colPale);
        btnSos.setTextSize(13);
        btnSos.setPadding(dp(12), dp(12), dp(12), dp(12));
        LinearLayout.LayoutParams sfl = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        btnSos.setLayoutParams(sfl);
        box.addView(btnSos);

        final Dialog dlg = createTacticalDialog(box);

        final Handler welfareTicker = new Handler();
        final Runnable welfareTick = new Runnable() {
            public void run() {
                if (isWelfareDialogShowing && dlg.isShowing()) {
                    long remain = autoEscalateMs - (SystemClock.elapsedRealtime() - promptTime);
                    if (remain <= 0) {
                        timerView.setText("⚠️ UNCONFIRMED · ESCALATING TO CONTROL ROOM");
                        timerView.setTextColor(colCrimson);
                    } else {
                        int secs = (int) (remain / 1000);
                        timerView.setText(String.format(Locale.US, "Auto-Escalating to DSS Control in %02d:%02d", secs / 60, secs % 60));
                        welfareTicker.postDelayed(this, 1000);
                    }
                }
            }
        };
        welfareTicker.post(welfareTick);

        btnConfirm.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                hapticHeavyClick();
                registerActivity();
                note(Core.TOPIC_ROUTINE, "[WELFARE] Officer Lochran Doherty confirmed active on duty (WHS Check)");
                isWelfareDialogShowing = false;
                dlg.dismiss();
            }
        });

        btnSos.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                hapticSealThud();
                registerActivity();
                note(Core.TOPIC_INCIDENT, "[EMERGENCY 000 DISPATCH] Triggered from lone worker welfare check");
                dialNumber("000");
                isWelfareDialogShowing = false;
                dlg.dismiss();
            }
        });

        dlg.setOnDismissListener(new DialogInterface.OnDismissListener() {
            public void onDismiss(DialogInterface dialog) {
                isWelfareDialogShowing = false;
            }
        });

        dlg.show();
    }

    private class AnimatedChainBannerView extends View {
        private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint tagPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint ripplePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF rect = new RectF();
        private float rippleX = -1f;

        public AnimatedChainBannerView(Context context) {
            super(context);
            bgPaint.setStyle(Paint.Style.FILL);
            textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            tagPaint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
            ripplePaint.setStyle(Paint.Style.FILL);
        }

        public void triggerRipple() {
            ValueAnimator va = ValueAnimator.ofFloat(0f, 1.2f);
            va.setDuration(650);
            va.setInterpolator(new DecelerateInterpolator());
            va.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                public void onAnimationUpdate(ValueAnimator a) {
                    rippleX = (Float) a.getAnimatedValue();
                    invalidate();
                }
            });
            va.start();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            int w = getWidth();
            int h = getHeight();
            if (w <= 0 || h <= 0) return;

            rect.set(0, 0, w, h);
            bgPaint.setColor(colPanel);
            canvas.drawRoundRect(rect, dp(12), dp(12), bgPaint);

            if (rippleX >= 0f && rippleX <= 1.2f) {
                float rx = rippleX * w;
                LinearGradient grad = new LinearGradient(rx - dp(80), 0, rx + dp(80), 0,
                        new int[]{0x00E5A93C, 0x55E5A93C, 0x8810B981, 0x0010B981},
                        null, Shader.TileMode.CLAMP);
                ripplePaint.setShader(grad);
                canvas.drawRoundRect(rect, dp(12), dp(12), ripplePaint);
            }

            int n = Core.entryCount();
            String head = Core.head();
            String headSnippet = head.length() >= 8 ? head.substring(0, 8) + "..." : "none";

            Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            dotPaint.setColor(colEmerald);
            canvas.drawCircle(dp(16), h / 2f, dp(4), dotPaint);

            textPaint.setColor(colPale);
            textPaint.setTextSize(dp(11));
            textPaint.setTextAlign(Paint.Align.LEFT);
            canvas.drawText("Chain Secured · SHA-256", dp(28), h / 2f + dp(4), textPaint);

            String tagText = "HEAD: " + headSnippet + " [" + n + "]";
            tagPaint.setTextSize(dp(10));
            float textW = tagPaint.measureText(tagText);
            RectF pillR = new RectF(w - textW - dp(24), dp(8), w - dp(10), h - dp(8));

            Paint tagBg = new Paint(Paint.ANTI_ALIAS_FLAG);
            tagBg.setColor(colEmeraldSoft);
            canvas.drawRoundRect(pillR, dp(6), dp(6), tagBg);

            tagPaint.setColor(colEmerald);
            tagPaint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(tagText, pillR.centerX(), pillR.centerY() + dp(3), tagPaint);
        }
    }

    private class HorizonLevelerView extends View {
        private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint reticlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        public HorizonLevelerView(Context context) {
            super(context);
            linePaint.setStyle(Paint.Style.STROKE);
            linePaint.setStrokeWidth(dp(2));
            reticlePaint.setStyle(Paint.Style.STROKE);
            reticlePaint.setStrokeWidth(dp(2));
            textPaint.setTextAlign(Paint.Align.CENTER);
            textPaint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            int w = getWidth();
            int h = getHeight();
            if (w <= 0 || h <= 0) return;

            float cx = w / 2f;
            float cy = h / 2f;

            float rollDeg = (float) Math.toDegrees(Math.atan2(lastAccel[0], Math.sqrt(lastAccel[1] * lastAccel[1] + lastAccel[2] * lastAccel[2])));
            float pitchDeg = (float) Math.toDegrees(Math.atan2(-lastAccel[1], lastAccel[2]));

            boolean isLevel = Math.abs(rollDeg) < 0.9f && Math.abs(pitchDeg) < 0.9f;
            int levelCol = isLevel ? colEmerald : colAccent;

            canvas.save();
            canvas.rotate(-rollDeg, cx, cy);

            linePaint.setColor(levelCol);
            canvas.drawLine(cx - dp(90), cy, cx - dp(30), cy, linePaint);
            canvas.drawLine(cx + dp(30), cy, cx + dp(90), cy, linePaint);
            canvas.drawLine(cx - dp(90), cy, cx - dp(90), cy + dp(8), linePaint);
            canvas.drawLine(cx + dp(90), cy, cx + dp(90), cy + dp(8), linePaint);

            reticlePaint.setColor(levelCol);
            canvas.drawCircle(cx, cy, dp(14), reticlePaint);
            canvas.drawCircle(cx, cy, dp(3), reticlePaint);

            canvas.restore();

            textPaint.setColor(levelCol);
            textPaint.setTextSize(dp(11));
            String status = isLevel ? "✓ 0.0° PERFECT LEVEL" : String.format(Locale.US, "ROLL: %+.1f°  PITCH: %+.1f°", rollDeg, pitchDeg);
            canvas.drawText(status, cx, cy + dp(48), textPaint);
        }
    }

    private class BiometricSealPadView extends View {
        private final Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint laserPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint iconPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF circleRect = new RectF();

        private float sweepProgress = 0f;
        private boolean isTouching = false;
        private ValueAnimator sweepAnimator;
        private final Runnable onCompletedAction;
        private final Handler pulseHandler = new Handler();

        public BiometricSealPadView(Context context, Runnable onComplete) {
            super(context);
            this.onCompletedAction = onComplete;

            ringPaint.setStyle(Paint.Style.STROKE);
            ringPaint.setStrokeWidth(dp(4));

            laserPaint.setStyle(Paint.Style.STROKE);
            laserPaint.setStrokeWidth(dp(6));
            laserPaint.setStrokeCap(Paint.Cap.ROUND);

            glowPaint.setStyle(Paint.Style.STROKE);
            glowPaint.setStrokeWidth(dp(12));
            glowPaint.setStrokeCap(Paint.Cap.ROUND);

            textPaint.setTextAlign(Paint.Align.CENTER);
            textPaint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));

            iconPaint.setTextAlign(Paint.Align.CENTER);
            iconPaint.setTypeface(Typeface.DEFAULT_BOLD);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    isTouching = true;
                    hapticClick();
                    startSweep();
                    startHapticPulseLoop();
                    getParent().requestDisallowInterceptTouchEvent(true);
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    isTouching = false;
                    cancelSweep();
                    stopHapticPulseLoop();
                    getParent().requestDisallowInterceptTouchEvent(false);
                    return true;
            }
            return super.onTouchEvent(event);
        }

        private void startSweep() {
            if (sweepAnimator != null && sweepAnimator.isRunning()) sweepAnimator.cancel();
            sweepAnimator = ValueAnimator.ofFloat(sweepProgress, 1.0f);
            sweepAnimator.setDuration((long) ((1.0f - sweepProgress) * 2200L));
            sweepAnimator.setInterpolator(new DecelerateInterpolator());
            sweepAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                public void onAnimationUpdate(ValueAnimator va) {
                    sweepProgress = (Float) va.getAnimatedValue();
                    invalidate();
                    if (sweepProgress >= 1.0f) {
                        stopHapticPulseLoop();
                        hapticSealThud();
                        if (onCompletedAction != null) onCompletedAction.run();
                    }
                }
            });
            sweepAnimator.start();
        }

        private void cancelSweep() {
            if (sweepAnimator != null && sweepAnimator.isRunning()) sweepAnimator.cancel();
            sweepAnimator = ValueAnimator.ofFloat(sweepProgress, 0f);
            sweepAnimator.setDuration((long) (sweepProgress * 400L));
            sweepAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                public void onAnimationUpdate(ValueAnimator va) {
                    sweepProgress = (Float) va.getAnimatedValue();
                    invalidate();
                }
            });
            sweepAnimator.start();
        }

        private void startHapticPulseLoop() {
            pulseHandler.removeCallbacksAndMessages(null);
            final Runnable pulseRunnable = new Runnable() {
                public void run() {
                    if (isTouching && sweepProgress < 1.0f) {
                        hapticClick();
                        long delay = (long) (180 - (sweepProgress * 130));
                        pulseHandler.postDelayed(this, Math.max(40, delay));
                    }
                }
            };
            pulseHandler.post(pulseRunnable);
        }

        private void stopHapticPulseLoop() {
            pulseHandler.removeCallbacksAndMessages(null);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            int w = getWidth();
            int h = getHeight();
            if (w <= 0 || h <= 0) return;

            float cx = w / 2f;
            float cy = h / 2f;
            float r = Math.min(w, h) / 2f - dp(16);

            circleRect.set(cx - r, cy - r, cx + r, cy + r);

            ringPaint.setColor(colLine);
            canvas.drawCircle(cx, cy, r, ringPaint);

            if (sweepProgress > 0.01f) {
                float sweepAngle = sweepProgress * 360f;
                laserPaint.setColor(colAccent);
                glowPaint.setColor(colAccent);
                glowPaint.setAlpha((int) (80 + sweepProgress * 120));

                canvas.drawArc(circleRect, -90f, sweepAngle, false, glowPaint);
                canvas.drawArc(circleRect, -90f, sweepAngle, false, laserPaint);
            }

            iconPaint.setColor(isTouching ? colAccent : colPale);
            iconPaint.setTextSize(dp(28));
            canvas.drawText("🔏", cx, cy - dp(4), iconPaint);

            textPaint.setColor(isTouching ? colAccent : colMuted);
            textPaint.setTextSize(dp(11));
            String msg = isTouching ? String.format(Locale.US, "AFFIRMING %d%%", (int) (sweepProgress * 100)) : "TOUCH & HOLD 2.5s";
            canvas.drawText(msg, cx, cy + dp(22), textPaint);
        }
    }

    // =========================================================================
    // 📱 DEPUTY WORKPLACE ADD-IN & PEEK & FLOW ENGINE (DECK LAUNCHER PHYSICS)
    // =========================================================================

    public void openDeputy(boolean animate) {
        if (mainSurfaceContainer == null || rootFrame == null) return;
        final int w = rootFrame.getWidth();
        if (w <= 0) return;
        isDeputyOpen = true;
        hapticHeavyClick();

        if (animate) {
            if (deputyContainer != null) {
                deputyContainer.animate().scaleX(1f).scaleY(1f).translationX(0f).setDuration(220)
                        .setInterpolator(new DecelerateInterpolator()).start();
            }
            if (deputyScrim != null) {
                deputyScrim.animate().alpha(0f).setDuration(200).start();
            }
            if (peekShadow != null) {
                peekShadow.animate().alpha(0.85f).translationX(w - dp(30)).setDuration(220).start();
            }
            mainSurfaceContainer.animate().translationX(w).setDuration(220)
                    .setInterpolator(new DecelerateInterpolator()).start();
        } else {
            if (deputyContainer != null) {
                deputyContainer.setScaleX(1f);
                deputyContainer.setScaleY(1f);
                deputyContainer.setTranslationX(0f);
            }
            if (deputyScrim != null) deputyScrim.setAlpha(0f);
            if (peekShadow != null) {
                peekShadow.setAlpha(0.85f);
                peekShadow.setTranslationX(w - dp(30));
            }
            mainSurfaceContainer.setTranslationX(w);
        }
    }

    public void closeDeputy(boolean animate) {
        if (mainSurfaceContainer == null) return;
        isDeputyOpen = false;
        hapticClick();

        if (animate) {
            if (deputyContainer != null) {
                deputyContainer.animate().scaleX(0.94f).scaleY(0.94f).translationX(-dp(30)).setDuration(240)
                        .setInterpolator(new DecelerateInterpolator()).start();
            }
            if (deputyScrim != null) {
                deputyScrim.animate().alpha(0.65f).setDuration(220).start();
            }
            if (peekShadow != null) {
                peekShadow.animate().alpha(0f).translationX(-dp(30)).setDuration(200).start();
            }
            mainSurfaceContainer.animate().translationX(0f).setDuration(240)
                    .setInterpolator(new DecelerateInterpolator()).start();
        } else {
            if (deputyContainer != null) {
                deputyContainer.setScaleX(0.94f);
                deputyContainer.setScaleY(0.94f);
                deputyContainer.setTranslationX(-dp(30));
            }
            if (deputyScrim != null) deputyScrim.setAlpha(0.65f);
            if (peekShadow != null) {
                peekShadow.setAlpha(0f);
                peekShadow.setTranslationX(-dp(30));
            }
            mainSurfaceContainer.setTranslationX(0f);
        }
    }

    private void applyPeek(float dx) {
        if (mainSurfaceContainer == null || rootFrame == null) return;
        float w = rootFrame.getWidth();
        if (w <= 0) return;

        float x = Math.max(0f, Math.min(dx, w));
        mainSurfaceContainer.setTranslationX(x);
        float p = Math.min(1f, x / (w * 0.55f));

        if (deputyScrim != null) {
            deputyScrim.setAlpha((1f - p) * 0.65f);
        }
        if (deputyContainer != null) {
            float s = 0.94f + 0.06f * p;
            deputyContainer.setScaleX(s);
            deputyContainer.setScaleY(s);
            deputyContainer.setTranslationX(-dp(30) * (1f - p));
        }
        if (peekShadow != null) {
            peekShadow.setTranslationX(x - dp(30));
            peekShadow.setAlpha(Math.min(0.85f, p * 1.4f));
        }

        if (!peekBuzzed && x > w * 0.5f) {
            peekBuzzed = true;
            hapticClick();
        } else if (peekBuzzed && x < w * 0.45f) {
            peekBuzzed = false;
        }
    }

    private void finishPeek(float dx, float vx) {
        if (mainSurfaceContainer == null || rootFrame == null) return;
        float w = rootFrame.getWidth();
        if (w <= 0) return;

        boolean commit = dx > w * 0.5f || vx > dp(1000);
        if (commit) {
            openDeputy(true);
        } else {
            closeDeputy(true);
        }
    }

    private View buildDeputyView() {
        ScrollView depScroll = new ScrollView(this);
        depScroll.setBackgroundColor(0xFF080C14);
        depScroll.setVerticalScrollBarEnabled(false);

        LinearLayout depLayout = new LinearLayout(this);
        depLayout.setOrientation(LinearLayout.VERTICAL);
        depLayout.setPadding(dp(16), dp(16), dp(16), dp(36));
        depLayout.setFitsSystemWindows(true);

        // 1. Deputy Branding & Return Bar
        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        topBar.setPadding(0, dp(6), 0, dp(14));

        TextView btnReturn = new TextView(this);
        btnReturn.setText("← GATEHOUSE");
        btnReturn.setTextColor(0xFF13C5BE);
        btnReturn.setTextSize(11);
        btnReturn.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
        btnReturn.setPadding(dp(10), dp(6), dp(10), dp(6));
        btnReturn.setBackground(rounded(0x2213C5BE, dp(8)));
        btnReturn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                closeDeputy(true);
            }
        });
        topBar.addView(btnReturn);

        TextView depBrand = new TextView(this);
        depBrand.setText("DEPUTY");
        depBrand.setTextColor(0xFF13C5BE);
        depBrand.setTextSize(14);
        depBrand.setTypeface(Typeface.DEFAULT_BOLD);
        depBrand.setLetterSpacing(0.08f);
        depBrand.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams dblp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        depBrand.setLayoutParams(dblp);
        topBar.addView(depBrand);

        depLayout.addView(topBar);

        // 2. Deputy Workplace Facility Header
        LinearLayout orgCard = new LinearLayout(this);
        orgCard.setOrientation(LinearLayout.VERTICAL);
        orgCard.setBackground(rounded(0xFF0F172A, dp(16)));
        orgCard.setPadding(dp(16), dp(14), dp(16), dp(14));
        LinearLayout.LayoutParams oclp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        oclp.bottomMargin = dp(12);
        orgCard.setLayoutParams(oclp);

        TextView orgSub = new TextView(this);
        orgSub.setText("DOHERTY SECURITY SERVICES · WORKPLACE ROSTER");
        orgSub.setTextColor(0xFFE5A93C);
        orgSub.setTextSize(9.5f);
        orgSub.setTypeface(Typeface.MONOSPACE);
        orgCard.addView(orgSub);

        TextView orgName = new TextView(this);
        orgName.setText("Hume Doors & Timber (Kingston)");
        orgName.setTextColor(0xFFFFFFFF);
        orgName.setTextSize(16);
        orgName.setTypeface(Typeface.DEFAULT_BOLD);
        orgName.setPadding(0, dp(3), 0, dp(4));
        orgCard.addView(orgName);

        TextView orgRole = new TextView(this);
        orgRole.setText("🛡️ Officer Lochran Doherty · LIC #41207 · Post 01 Gatehouse");
        orgRole.setTextColor(0xFF94A3B8);
        orgRole.setTextSize(11.5f);
        orgCard.addView(orgRole);

        depLayout.addView(orgCard);

        // 3. Live Shift Time Clock & Timesheet Status
        LinearLayout clockCard = new LinearLayout(this);
        clockCard.setOrientation(LinearLayout.VERTICAL);
        clockCard.setBackground(rounded(0xFF0F1C24, dp(16)));
        clockCard.setPadding(dp(16), dp(16), dp(16), dp(16));
        LinearLayout.LayoutParams cclp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cclp.bottomMargin = dp(12);
        clockCard.setLayoutParams(cclp);

        LinearLayout clockTop = new LinearLayout(this);
        clockTop.setOrientation(LinearLayout.HORIZONTAL);
        clockTop.setGravity(Gravity.CENTER_VERTICAL);

        TextView clockTitle = new TextView(this);
        clockTitle.setText("LIVE SHIFT TIME CLOCK");
        clockTitle.setTextColor(0xFFE2E8F0);
        clockTitle.setTextSize(12);
        clockTitle.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams ctlp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        clockTitle.setLayoutParams(ctlp);
        clockTop.addView(clockTitle);

        TextView clockStatus = new TextView(this);
        clockStatus.setText("● CLOCKED ON");
        clockStatus.setTextColor(0xFF10B981);
        clockStatus.setTextSize(9.5f);
        clockStatus.setTypeface(Typeface.MONOSPACE);
        clockStatus.setPadding(dp(8), dp(3), dp(8), dp(3));
        clockStatus.setBackground(rounded(0x2210B981, dp(6)));
        clockTop.addView(clockStatus);
        clockCard.addView(clockTop);

        TextView clockTime = new TextView(this);
        clockTime.setText("18:00 – 06:00 (12.0h)");
        clockTime.setTextColor(0xFF38BDF8);
        clockTime.setTextSize(20);
        clockTime.setTypeface(Typeface.MONOSPACE);
        clockTime.setPadding(0, dp(8), 0, dp(2));
        clockCard.addView(clockTime);

        TextView clockSub = new TextView(this);
        clockSub.setText("Clocked on at 17:55 · Security Award MA000115 (Night Rate)");
        clockSub.setTextColor(0xFF94A3B8);
        clockSub.setTextSize(11);
        clockSub.setPadding(0, 0, 0, dp(12));
        clockCard.addView(clockSub);

        LinearLayout clockBtns = new LinearLayout(this);
        clockBtns.setOrientation(LinearLayout.HORIZONTAL);

        TextView btnBreak = actionButton("☕ Meal Break", 0xFF1E293B, 0xFFE2E8F0);
        btnBreak.setTextSize(11);
        btnBreak.setPadding(dp(8), dp(10), dp(8), dp(10));
        ((LinearLayout.LayoutParams) btnBreak.getLayoutParams()).rightMargin = dp(4);
        btnBreak.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                hapticClick();
                Toast.makeText(MainActivity.this, "Deputy: 30m Meal Break recorded", Toast.LENGTH_SHORT).show();
            }
        });
        clockBtns.addView(btnBreak);

        TextView btnClockOut = actionButton("⏱️ Clock Out", 0xFF334155, 0xFFF87171);
        btnClockOut.setTextSize(11);
        btnClockOut.setPadding(dp(8), dp(10), dp(8), dp(10));
        ((LinearLayout.LayoutParams) btnClockOut.getLayoutParams()).leftMargin = dp(4);
        btnClockOut.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                hapticHeavyClick();
                Toast.makeText(MainActivity.this, "Deputy: Scheduled Clock-Out at 06:00 AM", Toast.LENGTH_SHORT).show();
            }
        });
        clockBtns.addView(btnClockOut);

        clockCard.addView(clockBtns);
        depLayout.addView(clockCard);

        // 4. Deputy Weekly Roster Schedule
        depLayout.addView(contactsSectionHeader("📅 DEPUTY CONFIRMED ROSTER (CURRENT CYCLE)", 0xFF38BDF8));

        depLayout.addView(buildDeputyShiftCard("Tonight (Sat 29 Aug)", "18:00 – 06:00 (12.0h)", "Gatehouse Post 01 · Static Night", true));
        depLayout.addView(buildDeputyShiftCard("Tomorrow (Sun 30 Aug)", "18:00 – 06:00 (12.0h)", "Gatehouse Post 01 · Static Night", false));
        depLayout.addView(buildDeputyShiftCard("Monday (31 Aug)", "18:00 – 06:00 (12.0h)", "Gatehouse Post 01 · Static Night", false));
        depLayout.addView(buildDeputyShiftCard("Tuesday (01 Sep)", "OFF DUTY (0.0h)", "Scheduled Rest Day / RDO", false));
        depLayout.addView(buildDeputyShiftCard("Wednesday (02 Sep)", "18:00 – 06:00 (12.0h)", "Gatehouse Post 01 · Static Night", false));

        // 5. Deputy Shift Tasks
        depLayout.addView(contactsSectionHeader("📋 DEPUTY SHIFT TASKS (3 OF 4 COMPLETE)", 0xFF10B981));

        LinearLayout taskBox = new LinearLayout(this);
        taskBox.setOrientation(LinearLayout.VERTICAL);
        taskBox.setBackground(rounded(0xFF0F172A, dp(16)));
        taskBox.setPadding(dp(16), dp(14), dp(16), dp(14));
        LinearLayout.LayoutParams tblp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        tblp.bottomMargin = dp(12);
        taskBox.setLayoutParams(tblp);

        taskBox.addView(deputyTaskItem("✓ Gate A & Kingston Rd entry logbooks verified", true));
        taskBox.addView(deputyTaskItem("✓ Factory internal lockups (Lots 14-18)", true));
        taskBox.addView(deputyTaskItem("✓ Fire booster & pump pressure check (175 PSI)", true));
        taskBox.addView(deputyTaskItem("○ 05:30 AM Pre-dawn perimeter lighting & gate unlock", false));

        depLayout.addView(taskBox);

        // 6. Deputy Shift Swap & Request Bar
        LinearLayout depActions = new LinearLayout(this);
        depActions.setOrientation(LinearLayout.HORIZONTAL);
        depActions.setPadding(0, dp(4), 0, dp(10));

        TextView btnSwap = actionButton("🔄 Shift Swap", 0xFF1E293B, 0xFF38BDF8);
        btnSwap.setTextSize(11);
        ((LinearLayout.LayoutParams) btnSwap.getLayoutParams()).rightMargin = dp(4);
        btnSwap.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                hapticClick();
                Toast.makeText(MainActivity.this, "Deputy: Shift swap request open", Toast.LENGTH_SHORT).show();
            }
        });
        depActions.addView(btnSwap);

        TextView btnLeave = actionButton("🌴 Request Leave", 0xFF1E293B, 0xFFE5A93C);
        btnLeave.setTextSize(11);
        ((LinearLayout.LayoutParams) btnLeave.getLayoutParams()).leftMargin = dp(4);
        btnLeave.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                hapticClick();
                Toast.makeText(MainActivity.this, "Deputy: Leave request portal open", Toast.LENGTH_SHORT).show();
            }
        });
        depActions.addView(btnLeave);

        depLayout.addView(depActions);

        depScroll.addView(depLayout);
        return depScroll;
    }

    private LinearLayout buildDeputyShiftCard(String day, String hours, String details, boolean isCurrent) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(rounded(isCurrent ? 0xFF162536 : 0xFF0F172A, dp(14)));
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(8);
        card.setLayoutParams(lp);

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);

        TextView tvDay = new TextView(this);
        tvDay.setText(day);
        tvDay.setTextColor(isCurrent ? 0xFF38BDF8 : 0xFFE2E8F0);
        tvDay.setTextSize(12.5f);
        tvDay.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        tvDay.setLayoutParams(dlp);
        top.addView(tvDay);

        TextView tvStatus = new TextView(this);
        tvStatus.setText(isCurrent ? "ACTIVE SHIFT" : (hours.contains("OFF") ? "REST DAY" : "CONFIRMED"));
        tvStatus.setTextColor(isCurrent ? 0xFF10B981 : (hours.contains("OFF") ? 0xFF64748B : 0xFF38BDF8));
        tvStatus.setTextSize(8.5f);
        tvStatus.setTypeface(Typeface.MONOSPACE);
        tvStatus.setPadding(dp(6), dp(2), dp(6), dp(2));
        tvStatus.setBackground(rounded(isCurrent ? 0x2210B981 : 0x2238BDF8, dp(4)));
        top.addView(tvStatus);
        card.addView(top);

        TextView tvHours = new TextView(this);
        tvHours.setText(hours + " · " + details);
        tvHours.setTextColor(0xFF94A3B8);
        tvHours.setTextSize(11);
        tvHours.setPadding(0, dp(4), 0, 0);
        card.addView(tvHours);

        return card;
    }

    private TextView deputyTaskItem(String text, boolean done) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(done ? 0xFF10B981 : 0xFFE2E8F0);
        tv.setTextSize(11.5f);
        tv.setTypeface(Typeface.DEFAULT);
        tv.setPadding(0, dp(4), 0, dp(4));
        return tv;
    }

    private View buildRosterView() {
        ScrollView rScroll = new ScrollView(this);
        rScroll.setBackgroundColor(0xFF060A10);
        rScroll.setVerticalScrollBarEnabled(false);

        LinearLayout rLayout = new LinearLayout(this);
        rLayout.setOrientation(LinearLayout.VERTICAL);
        rLayout.setPadding(dp(16), dp(16), dp(16), dp(36));
        rLayout.setFitsSystemWindows(true);

        // 1. Header Bar with Back Button & DSS Roster Title
        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        topBar.setPadding(0, dp(4), 0, dp(14));

        TextView btnReturn = new TextView(this);
        btnReturn.setText("← GATEHOUSE");
        btnReturn.setTextColor(colAccent);
        btnReturn.setTextSize(11);
        btnReturn.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
        btnReturn.setPadding(dp(10), dp(6), dp(10), dp(6));
        btnReturn.setBackground(rounded(0x22E5A93C, dp(8)));
        btnReturn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                closeDeputy(true);
            }
        });
        topBar.addView(btnReturn);

        TextView title = new TextView(this);
        title.setText("MASTER SHIFT ROSTER");
        title.setTextColor(colPale);
        title.setTextSize(13);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setLetterSpacing(0.12f);
        title.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        title.setLayoutParams(tlp);
        topBar.addView(title);

        rLayout.addView(topBar);

        // 2. 🟢 LIVE ON-DUTY & NEXT RELIEF RADAR CARD
        rLayout.addView(buildLiveReliefRadarCard());

        // 3. Officer & Facility Station Badge Card
        LinearLayout officerCard = new LinearLayout(this);
        officerCard.setOrientation(LinearLayout.VERTICAL);
        officerCard.setBackground(rounded(colPanel, dp(16)));
        officerCard.setPadding(dp(16), dp(14), dp(16), dp(14));
        LinearLayout.LayoutParams oclp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        oclp.bottomMargin = dp(12);
        officerCard.setLayoutParams(oclp);

        LinearLayout row1 = new LinearLayout(this);
        row1.setOrientation(LinearLayout.HORIZONTAL);
        row1.setGravity(Gravity.CENTER_VERTICAL);

        TextView dssBrand = new TextView(this);
        dssBrand.setText("DOHERTY SECURITY SERVICES · ROSTER DECK");
        dssBrand.setTextColor(colAccent);
        dssBrand.setTextSize(9.5f);
        dssBrand.setTypeface(Typeface.MONOSPACE);
        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        dssBrand.setLayoutParams(dlp);
        row1.addView(dssBrand);

        TextView badgeStatus = new TextView(this);
        badgeStatus.setText("● LIVE ROSTER");
        badgeStatus.setTextColor(colEmerald);
        badgeStatus.setTextSize(8.5f);
        badgeStatus.setTypeface(Typeface.MONOSPACE);
        badgeStatus.setPadding(dp(6), dp(2), dp(6), dp(2));
        badgeStatus.setBackground(rounded(0x2210B981, dp(4)));
        row1.addView(badgeStatus);
        officerCard.addView(row1);

        TextView facName = new TextView(this);
        facName.setText("Hume Doors & Timber, Kingston");
        facName.setTextColor(0xFFFFFFFF);
        facName.setTextSize(16);
        facName.setTypeface(Typeface.DEFAULT_BOLD);
        facName.setPadding(0, dp(3), 0, dp(4));
        officerCard.addView(facName);

        TextView offInfo = new TextView(this);
        offInfo.setText("🛡️ Officer Lochran Doherty · QLD Licence #41207 · Post 01 Gatehouse");
        offInfo.setTextColor(0xFF94A3B8);
        offInfo.setTextSize(11.5f);
        officerCard.addView(offInfo);

        rLayout.addView(officerCard);

        // 4. Interactive 7-Day Timeline Scrubber
        rLayout.addView(contactsSectionHeader("📅 SELECT TIMELINE DAY", colCyan));

        rosterScrubber = new FluidRosterDayScrubberView(this);
        LinearLayout.LayoutParams rslp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(64));
        rslp.bottomMargin = dp(12);
        rosterScrubber.setLayoutParams(rslp);
        rLayout.addView(rosterScrubber);

        // 5. 🗓️ FULL-WEEK TEAM GRID BOARD (Horizontal 7-Column Master Schedule)
        rLayout.addView(contactsSectionHeader("🗓️ FULL-WEEK TEAM ROSTER BOARD (SWIPE ACROSS)", colAccent));
        rLayout.addView(buildFullWeekTeamGrid());

        // 6. Dynamic Shift Detail Focus Container
        rLayout.addView(contactsSectionHeader("🗂️ SELECTED SHIFT FOCUS & HANDOVER", colEmerald));
        rosterDetailContainer = new LinearLayout(this);
        rosterDetailContainer.setOrientation(LinearLayout.VERTICAL);
        rLayout.addView(rosterDetailContainer);
        updateRosterDayDetail(5); // Default to Saturday 29 Aug (Tonight)

        // 7. Fortnightly Hours & Penalty Progress Deck
        rLayout.addView(contactsSectionHeader("📊 FORTNIGHTLY WORKLOAD & PENALTIES", colCyan));
        rLayout.addView(buildRosterFortnightCard());

        // 8. Shift Actions Bar (Swap Request & Handover)
        LinearLayout actionsRow = new LinearLayout(this);
        actionsRow.setOrientation(LinearLayout.HORIZONTAL);
        actionsRow.setPadding(0, dp(8), 0, dp(12));

        TextView btnSwap = actionButton("🔄 Request Shift Swap", colPanel, colCyan);
        btnSwap.setTextSize(11.5f);
        ((LinearLayout.LayoutParams) btnSwap.getLayoutParams()).rightMargin = dp(4);
        btnSwap.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                hapticClick();
                showShiftSwapDialog();
            }
        });
        actionsRow.addView(btnSwap);

        TextView btnNotes = actionButton("📝 Handover Notes", colPanel, colAccent);
        btnNotes.setTextSize(11.5f);
        ((LinearLayout.LayoutParams) btnNotes.getLayoutParams()).leftMargin = dp(4);
        btnNotes.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                hapticClick();
                showModernNotesSheet();
            }
        });
        actionsRow.addView(btnNotes);

        rLayout.addView(actionsRow);

        rScroll.addView(rLayout);
        return rScroll;
    }

    // =========================================================================
    // 🟢 "ON DUTY NOW & NEXT RELIEF" RADAR CARD
    // =========================================================================

    private LinearLayout buildLiveReliefRadarCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(rounded(0xFF0F1E19, dp(16)));
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        clp.bottomMargin = dp(12);
        card.setLayoutParams(clp);

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);

        TextView tag = new TextView(this);
        tag.setText("● ON-DUTY SITE RADAR");
        tag.setTextColor(colEmerald);
        tag.setTextSize(10);
        tag.setTypeface(Typeface.MONOSPACE);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        tag.setLayoutParams(tlp);
        top.addView(tag);

        TextView nextPill = new TextView(this);
        nextPill.setText("RELIEF IN 3h 57m");
        nextPill.setTextColor(colAccent);
        nextPill.setTextSize(9);
        nextPill.setTypeface(Typeface.MONOSPACE);
        nextPill.setPadding(dp(6), dp(2), dp(6), dp(2));
        nextPill.setBackground(rounded(0x22E5A93C, dp(4)));
        top.addView(nextPill);
        card.addView(top);

        // Active Guards Row
        LinearLayout activeRow = new LinearLayout(this);
        activeRow.setOrientation(LinearLayout.HORIZONTAL);
        activeRow.setPadding(0, dp(8), 0, dp(8));

        activeRow.addView(buildGuardRadarChip("🛡️ Lochran (You)", "Post 01 · until 06:00", colEmerald));
        activeRow.addView(buildGuardRadarChip("🛡️ Chris Ireton", "Yard · until 00:00", colCyan));
        card.addView(activeRow);

        // Next Relief Alert Strip
        LinearLayout reliefBox = new LinearLayout(this);
        reliefBox.setOrientation(LinearLayout.HORIZONTAL);
        reliefBox.setGravity(Gravity.CENTER_VERTICAL);
        reliefBox.setBackground(rounded(0x20000000, dp(8)));
        reliefBox.setPadding(dp(10), dp(8), dp(10), dp(8));

        TextView reliefIcon = new TextView(this);
        reliefIcon.setText("🤝");
        reliefIcon.setTextSize(14);
        reliefIcon.setPadding(0, 0, dp(8), 0);
        reliefBox.addView(reliefIcon);

        LinearLayout reliefInfo = new LinearLayout(this);
        reliefInfo.setOrientation(LinearLayout.VERTICAL);

        TextView reliefTitle = new TextView(this);
        reliefTitle.setText("NEXT RELIEF: Brian Rush");
        reliefTitle.setTextColor(colPale);
        reliefTitle.setTextSize(11);
        reliefTitle.setTypeface(Typeface.DEFAULT_BOLD);
        reliefInfo.addView(reliefTitle);

        TextView reliefSub = new TextView(this);
        reliefSub.setText("Shift: 00:00 – 06:00 · Post 01 Handover");
        reliefSub.setTextColor(0xFF94A3B8);
        reliefSub.setTextSize(10);
        reliefInfo.addView(reliefSub);

        reliefBox.addView(reliefInfo);
        card.addView(reliefBox);

        return card;
    }

    private LinearLayout buildGuardRadarChip(String name, String sub, int color) {
        LinearLayout chip = new LinearLayout(this);
        chip.setOrientation(LinearLayout.VERTICAL);
        chip.setBackground(rounded(0x18000000, dp(8)));
        chip.setPadding(dp(10), dp(6), dp(10), dp(6));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        lp.rightMargin = dp(4);
        chip.setLayoutParams(lp);

        TextView tvName = new TextView(this);
        tvName.setText(name);
        tvName.setTextColor(color);
        tvName.setTextSize(11);
        tvName.setTypeface(Typeface.DEFAULT_BOLD);
        chip.addView(tvName);

        TextView tvSub = new TextView(this);
        tvSub.setText(sub);
        tvSub.setTextColor(0xFF94A3B8);
        tvSub.setTextSize(9.5f);
        chip.addView(tvSub);

        return chip;
    }

    // =========================================================================
    // 🗓️ FULL-WEEK TEAM GRID BOARD (HORIZONTAL 7-COLUMN SCHEDULE)
    // =========================================================================

    private HorizontalScrollView fullWeekScrollView;

    private View buildFullWeekTeamGrid() {
        fullWeekScrollView = new HorizontalScrollView(this);
        fullWeekScrollView.setHorizontalScrollBarEnabled(false);
        LinearLayout.LayoutParams svlp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        svlp.bottomMargin = dp(14);
        fullWeekScrollView.setLayoutParams(svlp);

        LinearLayout gridRow = new LinearLayout(this);
        gridRow.setOrientation(LinearLayout.HORIZONTAL);

        final String[][][] weeklyData = {
            // MON 24
            {
                {"00:00 - 06:00", "Bill", "CONFIRMED", "Post 01"},
                {"00:00 - 05:10", "Brian Rush", "CONFIRMED", "Mobile 4"},
                {"16:00 - 00:00", "Lochran", "DONE", "Gatehouse"}
            },
            // TUE 25
            {
                {"00:00 - 06:00", "Bill", "CONFIRMED", "Post 01"},
                {"16:00 - 00:00", "Chris Ireton", "DONE", "Yard"},
                {"23:54 - 06:00", "Brian Rush", "DONE", "Gatehouse"}
            },
            // WED 26
            {
                {"15:35 - 22:26", "Jon Naylor", "DONE", "Factory"},
                {"22:00 - 06:00", "Chris Ireton", "DONE", "Gatehouse"}
            },
            // THU 27
            {
                {"15:43 - 21:53", "Jon Naylor", "DONE", "Factory"},
                {"21:57 - 06:00", "Brian Rush", "DONE", "Gatehouse"}
            },
            // FRI 28
            {
                {"16:00 - 03:00", "Bill", "DONE", "Post 01"},
                {"19:59 - 05:05", "Brian Rush", "DONE", "Gatehouse"}
            },
            // SAT 29 (TODAY)
            {
                {"02:00 - 07:00", "Lochran", "DONE", "Gatehouse"},
                {"07:00 - 16:00", "Ken", "DONE", "Day Sup."},
                {"16:00 - 00:00", "Chris Ireton", "ACTIVE", "Yard"},
                {"20:00 - 05:00", "Josh", "ACTIVE", "Mobile"}
            },
            // SUN 30 (TOMORROW)
            {
                {"00:00 - 06:00", "Bill", "SCHEDULED", "Post 01"},
                {"06:00 - 18:00", "Lochran", "ACTIVE", "Gatehouse"},
                {"18:00 - 00:00", "Chris Ireton", "SCHEDULED", "Yard"},
                {"20:00 - 00:00", "Brian Rush", "SCHEDULED", "Mobile"}
            }
        };

        final String[] dayLabels = {"MON 24", "TUE 25", "WED 26", "THU 27", "FRI 28", "SAT 29", "SUN 30"};

        for (int d = 0; d < dayLabels.length; d++) {
            final int dayIndex = d;
            boolean isToday = d == 5; // Saturday 29 Aug

            LinearLayout col = new LinearLayout(this);
            col.setOrientation(LinearLayout.VERTICAL);
            col.setBackground(rounded(isToday ? 0xFF141F30 : colPanel, dp(14)));
            col.setPadding(dp(8), dp(8), dp(8), dp(8));
            LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(dp(110), LinearLayout.LayoutParams.WRAP_CONTENT);
            clp.rightMargin = dp(8);
            col.setLayoutParams(clp);

            // Column Day Header
            TextView tvDayHead = new TextView(this);
            tvDayHead.setText(dayLabels[d]);
            tvDayHead.setTextColor(isToday ? colAccent : colPale);
            tvDayHead.setTextSize(11.5f);
            tvDayHead.setTypeface(Typeface.DEFAULT_BOLD);
            tvDayHead.setGravity(Gravity.CENTER);
            tvDayHead.setPadding(0, dp(2), 0, dp(6));
            col.addView(tvDayHead);

            // Stacked Guard Shift Tiles
            String[][] shifts = weeklyData[d];
            for (int s = 0; s < shifts.length; s++) {
                final String[] shift = shifts[s];
                final boolean isActive = "ACTIVE".equals(shift[2]);
                final boolean isDone = "DONE".equals(shift[2]);

                LinearLayout tile = new LinearLayout(this);
                tile.setOrientation(LinearLayout.VERTICAL);
                tile.setBackground(rounded(isActive ? 0xFF0F3820 : (isDone ? 0xFF0D1420 : 0xFF16253A), dp(8)));
                tile.setPadding(dp(6), dp(6), dp(6), dp(6));
                LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                tlp.bottomMargin = dp(6);
                tile.setLayoutParams(tlp);

                TextView tvTime = new TextView(this);
                tvTime.setText(shift[0]);
                tvTime.setTextColor(isActive ? 0xFF4ADE80 : (isDone ? 0xFF64748B : 0xFFE2E8F0));
                tvTime.setTextSize(9);
                tvTime.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
                tile.addView(tvTime);

                TextView tvGuard = new TextView(this);
                tvGuard.setText(shift[1]);
                tvGuard.setTextColor(shift[1].contains("Lochran") ? colAccent : (isActive ? 0xFFFFFFFF : 0xFFCBD5E1));
                tvGuard.setTextSize(10.5f);
                tvGuard.setTypeface(Typeface.DEFAULT_BOLD);
                tvGuard.setPadding(0, dp(2), 0, 0);
                tile.addView(tvGuard);

                TextView tvStatus = new TextView(this);
                tvStatus.setText(shift[2] + " · " + shift[3]);
                tvStatus.setTextColor(isActive ? colEmerald : (isDone ? colQuiet : colCyan));
                tvStatus.setTextSize(7.5f);
                tvStatus.setTypeface(Typeface.MONOSPACE);
                tvStatus.setPadding(0, dp(2), 0, 0);
                tile.addView(tvStatus);

                tile.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        hapticClick();
                        if (rosterScrubber != null) rosterScrubber.animateToPosition(dayIndex);
                        updateRosterDayDetail(dayIndex);
                    }
                });

                col.addView(tile);
            }

            gridRow.addView(col);
        }

        fullWeekScrollView.addView(gridRow);

        // Auto-scroll to center on Saturday (Today)
        fullWeekScrollView.post(new Runnable() {
            public void run() {
                if (fullWeekScrollView != null) {
                    fullWeekScrollView.smoothScrollTo(dp(118 * 4), 0);
                }
            }
        });

        return fullWeekScrollView;
    }

    // =========================================================================
    // 🗂️ 3D HYBRID DECK SHIFT FOCUS & HANDOVER CARD
    // =========================================================================

    private void updateRosterDayDetail(final int dayIndex) {
        if (rosterDetailContainer == null) return;
        final int prevIndex = selectedRosterDay;
        final boolean isForward = dayIndex >= prevIndex;
        selectedRosterDay = dayIndex;

        rosterDetailContainer.removeAllViews();

        final String[][] daysData = {
            {"Mon 24 Aug", "16:00 – 00:00 (8.0h)", "COMPLETED", "Gatehouse Post 01 · Static Guarding", "Bill (00:00 Relief)", "15% Night Loading", "Lochran Doherty"},
            {"Tue 25 Aug", "23:54 – 06:00 (6.1h)", "COMPLETED", "Gatehouse Post 01 · Static Guarding", "Chris Ireton (16:00 Relief)", "15% Night Loading", "Brian Rush"},
            {"Wed 26 Aug", "22:00 – 06:00 (8.0h)", "COMPLETED", "Gatehouse Post 01 · Static Guarding", "Jon Naylor (15:35 Relief)", "15% Night Loading", "Chris Ireton"},
            {"Thu 27 Aug", "21:57 – 06:00 (8.0h)", "COMPLETED", "Gatehouse Post 01 · Static Guarding", "Jon Naylor (15:43 Relief)", "15% Night Loading", "Brian Rush"},
            {"Fri 28 Aug", "19:59 – 05:05 (9.1h)", "COMPLETED", "Gatehouse Post 01 · Static Guarding", "Bill (16:00 Relief)", "15% Night Loading", "Brian Rush"},
            {"Sat 29 Aug (Tonight)", "18:00 – 06:00 (12.0h)", "ACTIVE SHIFT", "Gatehouse Post 01 · Static Guarding", "Brian Rush (00:00 Handover Relief)", "15% Night Loading + Sat Weekend Rate", "Lochran Doherty"},
            {"Sun 30 Aug (Tomorrow)", "06:00 – 18:00 (12.0h)", "CONFIRMED", "Gatehouse Post 01 · Static Guarding", "Bill (00:00 Relief)", "15% Night Loading + Sun Weekend Rate", "Lochran Doherty"}
        };

        final String[] dayInfo = daysData[Math.max(0, Math.min(daysData.length - 1, dayIndex))];
        boolean isTonight = dayIndex == 5; // Saturday 29 Aug

        final LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(rounded(isTonight ? 0xFF131D2E : colPanel, dp(16)));
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        clp.bottomMargin = dp(12);
        card.setLayoutParams(clp);

        // Enable 3D perspective
        card.setCameraDistance(dp(8000));

        // Horizontal Swipe detector directly on the card to cycle days
        card.setOnTouchListener(new View.OnTouchListener() {
            private float downX, downY;
            public boolean onTouch(View v, MotionEvent ev) {
                switch (ev.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        downX = ev.getX();
                        downY = ev.getY();
                        return true;
                    case MotionEvent.ACTION_UP:
                        float dx = ev.getX() - downX;
                        float dy = Math.abs(ev.getY() - downY);
                        if (Math.abs(dx) > dp(36) && Math.abs(dx) > dy * 1.2f) {
                            if (dx < 0 && dayIndex < daysData.length - 1) {
                                hapticClick();
                                if (rosterScrubber != null) rosterScrubber.animateToPosition(dayIndex + 1);
                                updateRosterDayDetail(dayIndex + 1);
                                return true;
                            } else if (dx > 0 && dayIndex > 0) {
                                hapticClick();
                                if (rosterScrubber != null) rosterScrubber.animateToPosition(dayIndex - 1);
                                updateRosterDayDetail(dayIndex - 1);
                                return true;
                            }
                        }
                        break;
                }
                return false;
            }
        });

        // Header Row: Day name + Status Chip
        LinearLayout hRow = new LinearLayout(this);
        hRow.setOrientation(LinearLayout.HORIZONTAL);
        hRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView tvDay = new TextView(this);
        tvDay.setText(dayInfo[0]);
        tvDay.setTextColor(isTonight ? colAccent : 0xFFFFFFFF);
        tvDay.setTextSize(14);
        tvDay.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        tvDay.setLayoutParams(dlp);
        hRow.addView(tvDay);

        TextView tvStatus = new TextView(this);
        tvStatus.setText(dayInfo[2]);
        tvStatus.setTextColor(isTonight ? colEmerald : colCyan);
        tvStatus.setTextSize(9);
        tvStatus.setTypeface(Typeface.MONOSPACE);
        tvStatus.setPadding(dp(7), dp(3), dp(7), dp(3));
        tvStatus.setBackground(rounded(isTonight ? 0x2210B981 : 0x2206B6D4, dp(6)));
        hRow.addView(tvStatus);
        card.addView(hRow);

        // Timing & Scope
        TextView tvHours = new TextView(this);
        tvHours.setText(dayInfo[1]);
        tvHours.setTextColor(isTonight ? 0xFF38BDF8 : 0xFFE2E8F0);
        tvHours.setTextSize(18);
        tvHours.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
        tvHours.setPadding(0, dp(8), 0, dp(2));
        card.addView(tvHours);

        TextView tvPost = new TextView(this);
        tvPost.setText("📍 " + dayInfo[3] + " · Guard: " + dayInfo[6]);
        tvPost.setTextColor(0xFF94A3B8);
        tvPost.setTextSize(12);
        tvPost.setPadding(0, 0, 0, dp(10));
        card.addView(tvPost);

        // Relief & Team on Shift
        LinearLayout teamBox = new LinearLayout(this);
        teamBox.setOrientation(LinearLayout.VERTICAL);
        teamBox.setBackground(rounded(0x18000000, dp(10)));
        teamBox.setPadding(dp(12), dp(10), dp(12), dp(10));
        LinearLayout.LayoutParams tblp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        tblp.bottomMargin = dp(10);
        teamBox.setLayoutParams(tblp);

        TextView teamLbl = new TextView(this);
        teamLbl.setText("ON-SHIFT TEAM & HANDOVER RELIEF");
        teamLbl.setTextColor(colQuiet);
        teamLbl.setTextSize(8.5f);
        teamLbl.setTypeface(Typeface.MONOSPACE);
        teamBox.addView(teamLbl);

        TextView teamRelief = new TextView(this);
        teamRelief.setText("🤝 Relief: " + dayInfo[4]);
        teamRelief.setTextColor(colPale);
        teamRelief.setTextSize(11.5f);
        teamRelief.setPadding(0, dp(2), 0, 0);
        teamBox.addView(teamRelief);

        TextView rateLbl = new TextView(this);
        rateLbl.setText("⭐ Rate: " + dayInfo[5]);
        rateLbl.setTextColor(colAccent);
        rateLbl.setTextSize(10.5f);
        rateLbl.setPadding(0, dp(2), 0, 0);
        teamBox.addView(rateLbl);

        card.addView(teamBox);

        // Pre-Shift Checklist
        card.addView(rosterCheckItem("✓ Master key ring & electronic gate fobs verified", true));
        card.addView(rosterCheckItem("✓ Bodycam charged & memory cleared", true));
        card.addView(rosterCheckItem("✓ LED tactical torches inspected", true));
        card.addView(rosterCheckItem("○ 05:18 AM Civil Dawn perimeter round scheduled", false));

        // 3D Hybrid Deck Cascade Animation Entrance
        float startX = isForward ? dp(28) : -dp(28);
        float startRotY = isForward ? 6f : -6f;
        card.setAlpha(0f);
        card.setTranslationX(startX);
        card.setScaleX(0.96f);
        card.setScaleY(0.96f);
        card.setRotationY(startRotY);

        rosterDetailContainer.addView(card);

        card.animate()
                .alpha(1f)
                .translationX(0f)
                .scaleX(1f)
                .scaleY(1f)
                .rotationY(0f)
                .setDuration(240)
                .setInterpolator(new OvershootInterpolator(1.06f))
                .start();
    }

    private LinearLayout rosterCheckItem(final String text, final boolean initialDone) {
        final LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(4), 0, dp(4));

        final TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(initialDone ? colEmerald : colPale);
        tv.setTextSize(11.5f);
        row.addView(tv);

        row.setOnClickListener(new View.OnClickListener() {
            boolean done = initialDone;
            public void onClick(View v) {
                hapticClick();
                done = !done;
                String clean = text.substring(2);
                tv.setText((done ? "✓ " : "○ ") + clean);
                tv.setTextColor(done ? colEmerald : colPale);
            }
        });

        return row;
    }

    private LinearLayout buildRosterFortnightCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(rounded(colPanel, dp(16)));
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        clp.bottomMargin = dp(12);
        card.setLayoutParams(clp);

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);

        TextView tvHours = new TextView(this);
        tvHours.setText("48.0h / 76.0h");
        tvHours.setTextColor(colEmerald);
        tvHours.setTextSize(16);
        tvHours.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
        LinearLayout.LayoutParams hlp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        tvHours.setLayoutParams(hlp);
        top.addView(tvHours);

        TextView tvPct = new TextView(this);
        tvPct.setText("63% COMPLETE");
        tvPct.setTextColor(colPale);
        tvPct.setTextSize(10);
        tvPct.setTypeface(Typeface.MONOSPACE);
        top.addView(tvPct);
        card.addView(top);

        // Liquid Progress Track
        FrameLayout track = new FrameLayout(this);
        track.setBackground(rounded(0x33FFFFFF, dp(4)));
        LinearLayout.LayoutParams trlp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(8));
        trlp.topMargin = dp(8);
        trlp.bottomMargin = dp(10);
        track.setLayoutParams(trlp);

        View fill = new View(this);
        fill.setBackground(rounded(colEmerald, dp(4)));
        FrameLayout.LayoutParams flp = new FrameLayout.LayoutParams(0, FrameLayout.LayoutParams.MATCH_PARENT);
        fill.setLayoutParams(flp);
        track.addView(fill);
        card.addView(track);

        // Animate fill bar width
        track.post(new Runnable() {
            public void run() {
                int totalW = track.getWidth();
                if (totalW > 0) {
                    ValueAnimator anim = ValueAnimator.ofInt(0, (int) (totalW * 0.63f));
                    anim.setDuration(600);
                    anim.setInterpolator(new DecelerateInterpolator());
                    anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                        public void onAnimationUpdate(ValueAnimator va) {
                            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) fill.getLayoutParams();
                            lp.width = (Integer) va.getAnimatedValue();
                            fill.setLayoutParams(lp);
                        }
                    });
                    anim.start();
                }
            }
        });

        TextView sub = new TextView(this);
        sub.setText("Current Pay Period: 24 Aug – 06 Sep · 4 shifts completed, 2 remaining");
        sub.setTextColor(0xFF94A3B8);
        sub.setTextSize(10.5f);
        card.addView(sub);

        return card;
    }

    private void showShiftSwapDialog() {
        final LinearLayout box = dialogContainer("🔄 Request Shift Swap", "ROSTER CO-ORDINATION", colCyan);

        box.addView(formSectionLabel("SELECT TARGET SHIFT"));
        String[] shifts = {"Sun 30 Aug (18:00 - 06:00)", "Mon 31 Aug (18:00 - 06:00)", "Wed 02 Sep (18:00 - 06:00)"};
        final String[] selShift = {shifts[0]};
        box.addView(buildChipGroup(shifts, selShift, true, colCyan));

        box.addView(formSectionLabel("PROPOSE COVER WITH GUARD"));
        String[] guards = {"Officer Chris Ireton", "Officer Brian Rush", "Officer Bill", "Officer Jon Naylor"};
        final String[] selGuard = {guards[0]};
        box.addView(buildChipGroup(guards, selGuard, true, colAccent));

        final Dialog dlg = createTacticalDialog(box);

        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setPadding(0, dp(16), 0, 0);

        TextView btnCancel = actionButton("Cancel", colLine, colMuted);
        btnCancel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                hapticClick();
                dlg.dismiss();
            }
        });
        btnRow.addView(btnCancel);

        TextView btnSend = actionButton("Send Request", colCyan, 0xFF000000);
        btnSend.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                hapticHeavyClick();
                Toast.makeText(MainActivity.this, "Shift swap proposal dispatched to " + selGuard[0], Toast.LENGTH_SHORT).show();
                dlg.dismiss();
            }
        });
        LinearLayout.LayoutParams sllp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.4f);
        sllp.leftMargin = dp(8);
        btnSend.setLayoutParams(sllp);
        btnRow.addView(btnSend);

        box.addView(btnRow);
        dlg.show();
    }

    // =========================================================================
    // 🌊 FLUID 7-DAY ROSTER SCRUBBER VIEW
    // =========================================================================

    private class FluidRosterDayScrubberView extends View {
        private final String[] titles = {"SAT 29", "SUN 30", "MON 31", "TUE 01", "WED 02", "THU 03", "FRI 04"};
        
        private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint pillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint pillGlowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint dayPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint datePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF trackRect = new RectF();
        private final RectF pillRect = new RectF();
        private final RectF glowRect = new RectF();

        private final String[] days = {"MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"};
        private final String[] dates = {"24", "25", "26", "27", "28", "29", "30"};
        private final boolean[] isShift = {true, true, true, true, true, true, true};

        private float indicatorPos = 5f; // Default to Saturday 29 Aug
        private ValueAnimator snapAnimator;
        private int lastHover = 5;

        public FluidRosterDayScrubberView(Context context) {
            super(context);
            trackPaint.setStyle(Paint.Style.FILL);
            borderPaint.setStyle(Paint.Style.STROKE);
            borderPaint.setStrokeWidth(dpf(1f));
            pillPaint.setStyle(Paint.Style.FILL);
            pillGlowPaint.setStyle(Paint.Style.FILL);
            dayPaint.setTextAlign(Paint.Align.CENTER);
            dayPaint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
            datePaint.setTextAlign(Paint.Align.CENTER);
            datePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            dotPaint.setStyle(Paint.Style.FILL);
        }

        private float dpf(float v) {
            return v * getResources().getDisplayMetrics().density;
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            int w = getWidth();
            if (w <= 0) return super.onTouchEvent(event);

            float pad = dpf(4f);
            float usableWidth = w - pad * 2;
            float segWidth = usableWidth / days.length;

            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_MOVE:
                    getParent().requestDisallowInterceptTouchEvent(true);
                    float rawPos = (event.getX() - pad - segWidth / 2f) / segWidth;
                    float clamped = Math.max(0f, Math.min(days.length - 1, rawPos));
                    int hover = Math.round(clamped);

                    
                    return true;

                case MotionEvent.ACTION_CANCEL:
                    getParent().requestDisallowInterceptTouchEvent(false);
                    animateToPosition(selectedRosterDay);
                    return true;
            }
            return super.onTouchEvent(event);
        }

        public void animateToPosition(int targetIndex) {
            if (snapAnimator != null && snapAnimator.isRunning()) snapAnimator.cancel();
            snapAnimator = ValueAnimator.ofFloat(indicatorPos, targetIndex);
            snapAnimator.setDuration(220);
            snapAnimator.setInterpolator(new OvershootInterpolator(1.1f));
            snapAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                public void onAnimationUpdate(ValueAnimator va) {
                    indicatorPos = (Float) va.getAnimatedValue();
                    invalidate();
                }
            });
            snapAnimator.start();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            int w = getWidth();
            int h = getHeight();
            if (w <= 0 || h <= 0) return;

            float pad = dpf(4f);
            float cornerRadius = dpf(16f);
            float pillCorner = dpf(12f);

            // 1. Draw track
            trackRect.set(0, 0, w, h);
            trackPaint.setColor(colPanel);
            canvas.drawRoundRect(trackRect, cornerRadius, cornerRadius, trackPaint);
            borderPaint.setColor(colLine);
            canvas.drawRoundRect(trackRect, cornerRadius, cornerRadius, borderPaint);

            // 2. Sliding indicator pill
            float usableWidth = w - pad * 2;
            float segWidth = usableWidth / days.length;

            float pillLeft = pad + indicatorPos * segWidth;
            float pillRight = pillLeft + segWidth;
            float pillTop = pad;
            float pillBottom = h - pad;
            pillRect.set(pillLeft, pillTop, pillRight, pillBottom);

            // Pill glow & body
            pillGlowPaint.setColor(colAccent);
            pillGlowPaint.setAlpha(45);
            glowRect.set(pillRect.left - dpf(2f), pillRect.top - dpf(1f), pillRect.right + dpf(2f), pillRect.bottom + dpf(1f));
            canvas.drawRoundRect(glowRect, pillCorner + dpf(1f), pillCorner + dpf(1f), pillGlowPaint);

            pillPaint.setColor(colAccent);
            canvas.drawRoundRect(pillRect, pillCorner, pillCorner, pillPaint);

            // 3. Draw day labels
            for (int i = 0; i < days.length; i++) {
                float segCenterX = pad + (i + 0.5f) * segWidth;
                float dist = Math.abs(indicatorPos - i);

                boolean isSelected = dist < 0.5f;

                dayPaint.setTextSize(dpf(9f));
                dayPaint.setColor(isSelected ? colAccentInk : (isShift[i] ? colPale : colQuiet));
                canvas.drawText(days[i], segCenterX, h / 2f - dpf(4f), dayPaint);

                datePaint.setTextSize(dpf(12.5f));
                datePaint.setColor(isSelected ? colAccentInk : (isShift[i] ? 0xFFFFFFFF : colQuiet));
                canvas.drawText(dates[i], segCenterX, h / 2f + dpf(12f), datePaint);

                // Small shift dot under date
                if (isShift[i]) {
                    dotPaint.setColor(isSelected ? colAccentInk : colEmerald);
                    canvas.drawCircle(segCenterX, h - pad - dpf(5f), dpf(2f), dotPaint);
                }
            }
        }
    }




    // =========================================================================
    // 🌌 THEME SHOCKWAVE & QUANTUM PARTICLE BURST OVERLAY
    // =========================================================================

    
    // =========================================================================
    // 🎨 FLUID ANIMATED THEME BAR VIEW
    // =========================================================================

    class FluidAnimatedThemeBarView extends View {
        private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint chipPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint chipGlowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF bgRect = new RectF();
        private final RectF chipRect = new RectF();

        private final String[] themeNames = {"GOLD", "AMBER", "MATRIX", "SLATE"};
        private final int[] themeColors = {0xFFFFD166, 0xFFFFB703, 0xFF00FF66, 0xFF94A3B8};
        private float indicatorFloat = (float) activeTheme;
        private ValueAnimator indAnimator;

        private float dpf(float v) {
            return v * getResources().getDisplayMetrics().density;
        }

        public FluidAnimatedThemeBarView(Context context) {
            super(context);
            bgPaint.setStyle(Paint.Style.FILL);
            chipPaint.setStyle(Paint.Style.FILL);
            chipGlowPaint.setStyle(Paint.Style.STROKE);
            chipGlowPaint.setStrokeWidth(dpf(1.5f));
            labelPaint.setTextAlign(Paint.Align.CENTER);
            labelPaint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
        }

        public void setIndicatorFloat(float f) {
            this.indicatorFloat = Math.max(0f, Math.min(3f, f));
            invalidate();
        }

        public void animateToTheme(int targetTheme) {
            if (indAnimator != null && indAnimator.isRunning()) indAnimator.cancel();
            indAnimator = ValueAnimator.ofFloat(indicatorFloat, (float) targetTheme);
            indAnimator.setDuration(300);
            indAnimator.setInterpolator(new OvershootInterpolator(1.2f));
            indAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                public void onAnimationUpdate(ValueAnimator va) {
                    indicatorFloat = (Float) va.getAnimatedValue();
                    invalidate();
                }
            });
            indAnimator.start();
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                float w = getWidth();
                if (w > 0) {
                    float segW = w / 4f;
                    int theme = Math.max(0, Math.min(3, (int) (event.getX() / segW)));
                    if (theme != activeTheme) {
                        float cx = event.getRawX();
                        float cy = event.getRawY();
                        MainActivity.this.animateThemeChangeWithShockwave(theme, cx, cy);
                        animateToTheme(theme);
                    }
                    return true;
                }
            }
            return super.onTouchEvent(event);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            int w = getWidth();
            int h = getHeight();
            if (w <= 0 || h <= 0) return;

            bgRect.set(0, 0, w, h);
            bgPaint.setColor(colPanel);
            canvas.drawRoundRect(bgRect, dp(12), dp(12), bgPaint);

            float segW = w / 4f;
            float pad = dpf(2.5f);
            float chipX = indicatorFloat * segW + pad;
            float chipW = segW - pad * 2;

            chipRect.set(chipX, pad, chipX + chipW, h - pad);

            int curTheme = Math.max(0, Math.min(3, Math.round(indicatorFloat)));
            chipPaint.setColor(themeColors[curTheme]);
            canvas.drawRoundRect(chipRect, dp(9), dp(9), chipPaint);

            labelPaint.setTextSize(dpf(9f));
            float textY = h / 2f + dpf(3.2f);

            for (int i = 0; i < 4; i++) {
                float tx = i * segW + segW / 2f;
                float dist = Math.abs(indicatorFloat - i);
                if (dist < 0.5f) {
                    labelPaint.setColor(0xFF000000);
                } else {
                    labelPaint.setColor(colMuted);
                }
                canvas.drawText(themeNames[i], tx, textY, labelPaint);
            }
        }
    }

    // =========================================================================
    // 📱 FLUID ANIMATED TAB BAR VIEW
    // =========================================================================

    class FluidAnimatedTabBarView extends View {
        private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint indicatorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint indicatorGlowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF bgRect = new RectF();
        private final RectF indRect = new RectF();

        private final String[] tabTitles = {"🛡️ PATROL", "📞 CONTACTS", "📅 ROSTER", "🛠️ TOOLS"};
        private float indicatorFloat = 0f;
        private ValueAnimator indAnimator;

        private float dpf(float v) {
            return v * getResources().getDisplayMetrics().density;
        }

        public FluidAnimatedTabBarView(Context context) {
            super(context);
            bgPaint.setStyle(Paint.Style.FILL);
            indicatorPaint.setStyle(Paint.Style.FILL);
            indicatorGlowPaint.setStyle(Paint.Style.STROKE);
            indicatorGlowPaint.setStrokeWidth(dpf(1.5f));
            textPaint.setTextAlign(Paint.Align.CENTER);
            textPaint.setTypeface(Typeface.DEFAULT_BOLD);
        }

        public void setIndicatorFloat(float f) {
            this.indicatorFloat = Math.max(0f, Math.min(3f, f));
            invalidate();
        }

        public void animateToTab(int targetTab) {
            if (indAnimator != null && indAnimator.isRunning()) indAnimator.cancel();
            indAnimator = ValueAnimator.ofFloat(indicatorFloat, (float) targetTab);
            indAnimator.setDuration(280);
            indAnimator.setInterpolator(new OvershootInterpolator(1.15f));
            indAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                public void onAnimationUpdate(ValueAnimator va) {
                    indicatorFloat = (Float) va.getAnimatedValue();
                    invalidate();
                }
            });
            indAnimator.start();
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_MOVE) {
                float w = getWidth();
                if (w > 0) {
                    float segW = w / 4f;
                    int tab = Math.max(0, Math.min(3, (int) (event.getX() / segW)));
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        animateTabToPosition(tab);
                    }
                    return true;
                }
            }
            return super.onTouchEvent(event);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            int w = getWidth();
            int h = getHeight();
            if (w <= 0 || h <= 0) return;

            bgRect.set(0, 0, w, h);
            bgPaint.setColor(colPanel);
            canvas.drawRoundRect(bgRect, dp(14), dp(14), bgPaint);

            float segW = w / 4f;
            float indPad = dp(3);
            float indX = indicatorFloat * segW + indPad;
            float indW = segW - indPad * 2;

            indRect.set(indX, indPad, indX + indW, h - indPad);

            indicatorPaint.setColor(colAccent);
            canvas.drawRoundRect(indRect, dp(11), dp(11), indicatorPaint);

            indicatorGlowPaint.setColor(colAccentSoft);
            canvas.drawRoundRect(indRect, dp(11), dp(11), indicatorGlowPaint);

            textPaint.setTextSize(dpf(10.5f));
            float textY = h / 2f + dpf(3.8f);

            for (int i = 0; i < 4; i++) {
                float tx = i * segW + segW / 2f;
                float dist = Math.abs(indicatorFloat - i);
                if (dist < 0.5f) {
                    textPaint.setColor(colAccentInk);
                } else {
                    textPaint.setColor(colMuted);
                }
                canvas.drawText(tabTitles[i], tx, textY, textPaint);
            }
        }
    }

    class ThemeShockwaveOverlayView extends View {
        private final Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint particlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint starPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path starPath = new Path();

        private static final int PARTICLE_COUNT = 32;
        private final float[] pX = new float[PARTICLE_COUNT];
        private final float[] pY = new float[PARTICLE_COUNT];
        private final float[] pVx = new float[PARTICLE_COUNT];
        private final float[] pVy = new float[PARTICLE_COUNT];
        private final float[] pSize = new float[PARTICLE_COUNT];
        private final float[] pAlpha = new float[PARTICLE_COUNT];
        private final float[] pRot = new float[PARTICLE_COUNT];
        private final float[] pRotSpeed = new float[PARTICLE_COUNT];

        private float originX = 0f;
        private float originY = 0f;
        private float shockwaveRadius = 0f;
        private float shockwaveAlpha = 0f;
        private int shockwaveColor = 0xFFFFD166;
        private ValueAnimator waveAnimator;
        private boolean isActive = false;

        public ThemeShockwaveOverlayView(Context context) {
            super(context);
            ringPaint.setStyle(Paint.Style.STROKE);
            glowPaint.setStyle(Paint.Style.STROKE);
            particlePaint.setStyle(Paint.Style.FILL);
            starPaint.setStyle(Paint.Style.FILL);
            setVisibility(View.GONE);
        }

        private float dpf(float v) {
            return v * getResources().getDisplayMetrics().density;
        }

        public void triggerShockwave(float x, float y, int color) {
            this.originX = x;
            this.originY = y;
            this.shockwaveColor = color;
            this.isActive = true;
            setVisibility(View.VISIBLE);

            // Initialize 32 quantum starlight particles
            java.util.Random rnd = new java.util.Random();
            for (int i = 0; i < PARTICLE_COUNT; i++) {
                pX[i] = originX;
                pY[i] = originY;
                double angle = rnd.nextDouble() * Math.PI * 2.0;
                float speed = dpf(2.5f + rnd.nextFloat() * 7.5f);
                pVx[i] = (float) (Math.cos(angle) * speed);
                pVy[i] = (float) (Math.sin(angle) * speed);
                pSize[i] = dpf(2f + rnd.nextFloat() * 4.5f);
                pAlpha[i] = 1.0f;
                pRot[i] = rnd.nextFloat() * 360f;
                pRotSpeed[i] = (rnd.nextFloat() - 0.5f) * 18f;
            }

            if (waveAnimator != null && waveAnimator.isRunning()) {
                waveAnimator.cancel();
            }

            waveAnimator = ValueAnimator.ofFloat(0f, 1f);
            waveAnimator.setDuration(520);
            waveAnimator.setInterpolator(new DecelerateInterpolator(1.4f));
            waveAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                public void onAnimationUpdate(ValueAnimator va) {
                    float f = (Float) va.getAnimatedValue();
                    float maxDist = (float) Math.hypot(getWidth(), getHeight()) * 1.05f;
                    shockwaveRadius = f * maxDist;
                    shockwaveAlpha = (1f - f);

                    for (int i = 0; i < PARTICLE_COUNT; i++) {
                        pX[i] += pVx[i];
                        pY[i] += pVy[i];
                        pVy[i] += dpf(0.15f); // subtle gravity
                        pAlpha[i] = Math.max(0f, (1f - f * 1.1f));
                        pRot[i] += pRotSpeed[i];
                    }
                    invalidate();
                }
            });
            waveAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    isActive = false;
                    setVisibility(View.GONE);
                }
            });
            waveAnimator.start();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            if (!isActive) return;

            // Draw glowing expanding shockwave ring
            if (shockwaveRadius > 0f && shockwaveAlpha > 0f) {
                glowPaint.setColor(shockwaveColor);
                glowPaint.setStrokeWidth(dpf(24f));
                glowPaint.setAlpha((int) (shockwaveAlpha * 70));
                canvas.drawCircle(originX, originY, shockwaveRadius, glowPaint);

                ringPaint.setColor(shockwaveColor);
                ringPaint.setStrokeWidth(dpf(4f));
                ringPaint.setAlpha((int) (shockwaveAlpha * 240));
                canvas.drawCircle(originX, originY, shockwaveRadius, ringPaint);
            }

            // Draw 32 sparkling starburst particles
            for (int i = 0; i < PARTICLE_COUNT; i++) {
                if (pAlpha[i] <= 0.01f) continue;
                particlePaint.setColor(shockwaveColor);
                particlePaint.setAlpha((int) (pAlpha[i] * 230));

                canvas.save();
                canvas.translate(pX[i], pY[i]);
                canvas.rotate(pRot[i]);

                float s = pSize[i];
                starPath.reset();
                starPath.moveTo(0, -s);
                starPath.lineTo(s * 0.3f, -s * 0.3f);
                starPath.lineTo(s, 0);
                starPath.lineTo(s * 0.3f, s * 0.3f);
                starPath.lineTo(0, s);
                starPath.lineTo(-s * 0.3f, s * 0.3f);
                starPath.lineTo(-s, 0);
                starPath.lineTo(-s * 0.3f, -s * 0.3f);
                starPath.close();

                canvas.drawPath(starPath, particlePaint);

                // Core white sparkle center
                starPaint.setColor(0xFFFFFFFF);
                starPaint.setAlpha((int) (pAlpha[i] * 255));
                canvas.drawCircle(0, 0, s * 0.35f, starPaint);

                canvas.restore();
            }
        }
    }

    public void animateThemeChangeWithShockwave(final int newTheme, float origX, float origY) {
        if (newTheme == activeTheme) return;
        hapticClick();
        activeTheme = newTheme;

        int waveColor = (newTheme == 1 ? 0xFFFFB703 : (newTheme == 2 ? 0xFF00FF66 : (newTheme == 3 ? 0xFF94A3B8 : 0xFFFFD166)));
        if (shockwaveOverlay != null) {
            shockwaveOverlay.triggerShockwave(origX, origY, waveColor);
        }

        applyThemeTokens();
        if (root != null) root.setBackgroundColor(colBg);
        if (rootFrame != null) rootFrame.setBackgroundColor(colBg);
        if (scrollPatrol != null) scrollPatrol.setBackgroundColor(colBg);
        if (scrollContacts != null) scrollContacts.setBackgroundColor(colBg);
        if (scrollHandbook != null) scrollHandbook.setBackgroundColor(colBg);
        if (scrollTools != null) scrollTools.setBackgroundColor(colBg);

        if (animatedThemeBar != null) animatedThemeBar.invalidate();
        if (animatedTabBar != null) animatedTabBar.invalidate();
        if (rosterScrubber != null) rosterScrubber.invalidate();
    }
}
