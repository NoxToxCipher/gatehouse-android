package au.com.dss.gatehouse;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * LicenceVerificationManager — Security Licence Verification & Automated Expiry Alert Engine.
 * 
 * Manages Officer Security Provider Licences (QLD Class 1A/1C Static Guarding & WHS Compliance)
 * and dispatches luxury executive renewal reminders at key regulatory milestones:
 * - 3 Months Before (90 Days)
 * - 1 Month Before (30 Days)
 * - 1 Fortnight Before (14 Days)
 * - Day of Expiry (0 Days / Immediate Action)
 */
public class LicenceVerificationManager {
    private static final String TAG = "LicenceVerification";

    public static final String CHANNEL_LICENCE_ALERTS = "security_licence_alerts";
    private static final String PREFS_NAME = "licence_verification_state";
    private static final String PREF_KEY_LICENCE_NUM = "lic_number";
    private static final String PREF_KEY_EXPIRY_TS = "lic_expiry_ts";
    private static final String PREF_KEY_NOTIFIED_MILESTONE = "lic_notified_milestone_";

    // Default Doherty Security Services assigned officer credentials
    public static final String DEFAULT_OFFICER_NAME = "Lochran Mackenzie Doherty";
    public static final String DEFAULT_LICENCE_NUMBER = "41207";
    public static final String DEFAULT_LICENCE_CLASS = "Class 1A (Unarmed Guard) / Class 1C (Crowd Controller)";
    public static final String DEFAULT_JURISDICTION = "Queensland Office of Fair Trading (Security Providers Act 1993)";
    public static final String DEFAULT_FIRST_AID_CERT = "HLTAID011 Provide First Aid / HLTAID009 CPR (SJA-QLD-849102-K)";
    public static final String DEFAULT_FIRST_AID_EXP = "15/05/2028";

    // Default expiry: 14 October 2027
    public static final long DEFAULT_EXPIRY_TIMESTAMP_MS = 1823472000000L; // 2027-10-14 00:00:00 AEST

    public static class LicenceStatus {
        public String officerName;
        public String licenceNumber;
        public String licenceClass;
        public String jurisdiction;
        public long expiryTimestampMs;
        public String formattedExpiryDate;
        public long daysRemaining;
        public boolean isExpired;
        public boolean isWithin3Months;
        public boolean isWithin1Month;
        public boolean isWithin1Fortnight;
        public boolean isDayOfExpiry;
        public String statusBadgeText;
        public int statusColor;
        public int statusBgColor;
        public String advisoryMessage;
    }

    public static class RenewalMilestone {
        public String label;
        public String targetDateStr;
        public long daysBeforeExpiry;
        public boolean isPassed;
        public boolean isCurrent;
        public String actionAdvice;

        public RenewalMilestone(String label, String targetDateStr, long daysBeforeExpiry, boolean isPassed, boolean isCurrent, String advice) {
            this.label = label;
            this.targetDateStr = targetDateStr;
            this.daysBeforeExpiry = daysBeforeExpiry;
            this.isPassed = isPassed;
            this.isCurrent = isCurrent;
            this.actionAdvice = advice;
        }
    }

    public static void initChannels(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm == null) return;

            NotificationChannel chan = new NotificationChannel(
                    CHANNEL_LICENCE_ALERTS,
                    "Security Licence Compliance & Expiry",
                    NotificationManager.IMPORTANCE_HIGH
            );
            chan.setDescription("Official reminders for Queensland Security Provider Licence renewals (3 months, 1 month, 1 fortnight, day of expiry)");
            chan.enableLights(true);
            chan.setLightColor(0xFFF59E0B);
            chan.enableVibration(true);
            chan.setVibrationPattern(new long[]{0, 200, 100, 200, 100, 400});
            chan.setShowBadge(true);
            nm.createNotificationChannel(chan);
        }
    }

    public static long getExpiryTimestamp(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getLong(PREF_KEY_EXPIRY_TS, DEFAULT_EXPIRY_TIMESTAMP_MS);
    }

    public static String getLicenceNumber(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(PREF_KEY_LICENCE_NUM, DEFAULT_LICENCE_NUMBER);
    }

    public static void saveLicenceDetails(Context context, String licenceNum, long expiryTs) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .putString(PREF_KEY_LICENCE_NUM, licenceNum)
                .putLong(PREF_KEY_EXPIRY_TS, expiryTs)
                .apply();
    }

    public static LicenceStatus getLicenceStatus(Context context) {
        LicenceStatus s = new LicenceStatus();
        s.officerName = DEFAULT_OFFICER_NAME;
        s.licenceNumber = getLicenceNumber(context);
        s.licenceClass = DEFAULT_LICENCE_CLASS;
        s.jurisdiction = DEFAULT_JURISDICTION;
        s.expiryTimestampMs = getExpiryTimestamp(context);

        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("Australia/Brisbane"));
        s.formattedExpiryDate = sdf.format(new Date(s.expiryTimestampMs));

        long nowMs = System.currentTimeMillis();
        long diffMs = s.expiryTimestampMs - nowMs;
        s.daysRemaining = diffMs / (1000L * 60L * 60L * 24L);

        s.isExpired = s.daysRemaining < 0;
        s.isDayOfExpiry = s.daysRemaining == 0;
        s.isWithin1Fortnight = s.daysRemaining > 0 && s.daysRemaining <= 14;
        s.isWithin1Month = s.daysRemaining > 14 && s.daysRemaining <= 30;
        s.isWithin3Months = s.daysRemaining > 30 && s.daysRemaining <= 90;

        if (s.isExpired) {
            s.statusBadgeText = "⛔ LICENCE EXPIRED";
            s.statusColor = 0xFFEF4444; // Crimson
            s.statusBgColor = 0x33EF4444;
            s.advisoryMessage = "Licence expired " + Math.abs(s.daysRemaining) + " days ago. Officer cannot perform security duties until renewed.";
        } else if (s.isDayOfExpiry) {
            s.statusBadgeText = "🚨 EXPIRES TODAY";
            s.statusColor = 0xFFEF4444;
            s.statusBgColor = 0x33EF4444;
            s.advisoryMessage = "Licence expires TODAY (" + s.formattedExpiryDate + "). Urgent renewal submission required immediately.";
        } else if (s.isWithin1Fortnight) {
            s.statusBadgeText = "🚨 " + s.daysRemaining + " DAYS (1 FORTNIGHT)";
            s.statusColor = 0xFFF97316; // Orange
            s.statusBgColor = 0x33F97316;
            s.advisoryMessage = "Critical: Licence expires in " + s.daysRemaining + " days (" + s.formattedExpiryDate + "). Finalise QLD Fair Trading renewal.";
        } else if (s.isWithin1Month) {
            s.statusBadgeText = "⚠️ " + s.daysRemaining + " DAYS (1 MONTH)";
            s.statusColor = 0xFFF59E0B; // Amber
            s.statusBgColor = 0x33F59E0B;
            s.advisoryMessage = "Advisory: Licence expires in " + s.daysRemaining + " days (" + s.formattedExpiryDate + "). Submit fingerprint & renewal docs.";
        } else if (s.isWithin3Months) {
            s.statusBadgeText = "🔔 " + s.daysRemaining + " DAYS (3 MONTHS)";
            s.statusColor = 0xFF06B6D4; // Cyan
            s.statusBgColor = 0x2206B6D4;
            s.advisoryMessage = "Advance Notice: Licence renewal window opens in " + s.daysRemaining + " days (" + s.formattedExpiryDate + ").";
        } else {
            s.statusBadgeText = "✓ VERIFIED ACTIVE";
            s.statusColor = 0xFF10B981; // Emerald
            s.statusBgColor = 0x2210B981;
            s.advisoryMessage = "Licence is fully current and compliant with QLD Fair Trading. Valid for next " + s.daysRemaining + " days.";
        }

        return s;
    }

    public static List<RenewalMilestone> getRenewalMilestones(Context context) {
        List<RenewalMilestone> list = new ArrayList<>();
        long expMs = getExpiryTimestamp(context);
        long nowMs = System.currentTimeMillis();
        long daysLeft = (expMs - nowMs) / (1000L * 60L * 60L * 24L);

        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("Australia/Brisbane"));

        // 1. 3 Months Before (90 Days)
        long ts3M = expMs - (90L * 24L * 3600L * 1000L);
        boolean passed3M = nowMs >= ts3M;
        boolean curr3M = daysLeft > 30 && daysLeft <= 90;
        list.add(new RenewalMilestone("3 Months Notice", sdf.format(new Date(ts3M)), 90, passed3M, curr3M, "Receive renewal pack from Fair Trading QLD"));

        // 2. 1 Month Before (30 Days)
        long ts1M = expMs - (30L * 24L * 3600L * 1000L);
        boolean passed1M = nowMs >= ts1M;
        boolean curr1M = daysLeft > 14 && daysLeft <= 30;
        list.add(new RenewalMilestone("1 Month Notice", sdf.format(new Date(ts1M)), 30, passed1M, curr1M, "Submit verified ID & passport photos online"));

        // 3. 1 Fortnight Before (14 Days)
        long ts14D = expMs - (14L * 24L * 3600L * 1000L);
        boolean passed14D = nowMs >= ts14D;
        boolean curr14D = daysLeft > 0 && daysLeft <= 14;
        list.add(new RenewalMilestone("1 Fortnight Notice", sdf.format(new Date(ts14D)), 14, passed14D, curr14D, "Confirm receipt of application with QLD Licensing"));

        // 4. Day of Expiry (0 Days)
        boolean passedDay = nowMs >= expMs;
        boolean currDay = daysLeft == 0;
        list.add(new RenewalMilestone("Day of Expiry", sdf.format(new Date(expMs)), 0, passedDay, currDay, "Ensure digital interim licence is loaded into Gatehouse"));

        return list;
    }

    /**
     * Checks if any renewal milestone notification should be fired.
     */
    public static void checkAndNotifyLicenceExpiry(Context context) {
        initChannels(context);
        LicenceStatus s = getLicenceStatus(context);
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        String milestoneKey = null;
        String alertTitle = null;
        String alertText = null;

        if (s.isExpired) {
            milestoneKey = "EXPIRED";
            alertTitle = "⛔ Critical: QLD Security Licence EXPIRED";
            alertText = "Officer " + s.officerName + " · Licence #" + s.licenceNumber + " expired " + Math.abs(s.daysRemaining) + " days ago. Renew immediately.";
        } else if (s.isDayOfExpiry) {
            milestoneKey = "DAY_OF";
            alertTitle = "🚨 Final Notice: QLD Security Licence Expires TODAY";
            alertText = "Officer " + s.officerName + " · Licence #" + s.licenceNumber + " expires today (" + s.formattedExpiryDate + ").";
        } else if (s.isWithin1Fortnight) {
            milestoneKey = "14_DAYS";
            alertTitle = "🚨 1 Fortnight Notice: Security Licence Renewal Due";
            alertText = "Licence #" + s.licenceNumber + " expires in " + s.daysRemaining + " days (" + s.formattedExpiryDate + "). Finalise renewal.";
        } else if (s.isWithin1Month) {
            milestoneKey = "30_DAYS";
            alertTitle = "⚠️ 1 Month Notice: Security Licence Renewal Due";
            alertText = "Licence #" + s.licenceNumber + " expires in " + s.daysRemaining + " days (" + s.formattedExpiryDate + "). Submit paperwork to Fair Trading QLD.";
        } else if (s.isWithin3Months) {
            milestoneKey = "90_DAYS";
            alertTitle = "🔔 3 Months Notice: Security Licence Renewal Advisory";
            alertText = "Licence #" + s.licenceNumber + " expires in " + s.daysRemaining + " days (" + s.formattedExpiryDate + "). Renewal window is now open.";
        }

        if (milestoneKey != null) {
            String fullKey = PREF_KEY_NOTIFIED_MILESTONE + milestoneKey + "_" + s.expiryTimestampMs;
            if (!prefs.getBoolean(fullKey, false)) {
                postLicenceNotification(context, alertTitle, alertText, s);
                prefs.edit().putBoolean(fullKey, true).apply();
            }
        }
    }

    public static void postLicenceNotification(Context context, String title, String message, LicenceStatus status) {
        try {
            initChannels(context);
            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm == null) return;

            Intent intent = new Intent(context, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.putExtra("OPEN_CREDENTIAL_VAULT", true);

            PendingIntent pi = PendingIntent.getActivity(
                    context, 41207, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
            );

            Notification.Builder builder;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                builder = new Notification.Builder(context, CHANNEL_LICENCE_ALERTS);
            } else {
                builder = new Notification.Builder(context);
                builder.setPriority(Notification.PRIORITY_HIGH);
            }

            int iconShield = context.getResources().getIdentifier("ic_shield_gold", "drawable", context.getPackageName());
            if (iconShield == 0) iconShield = context.getApplicationInfo().icon;

            builder.setSmallIcon(iconShield)
                    .setContentTitle(title)
                    .setContentText(message)
                    .setStyle(new Notification.BigTextStyle().bigText(
                            message + "\n\n" +
                            "📋 Licence: #" + status.licenceNumber + " (" + status.licenceClass + ")\n" +
                            "🏛️ Authority: " + status.jurisdiction + "\n" +
                            "📅 Expiry: " + status.formattedExpiryDate + " (" + status.daysRemaining + " days remaining)\n\n" +
                            "Tap to open Officer Credential Vault & Compliance Sheet."
                    ))
                    .setContentIntent(pi)
                    .setAutoCancel(true);

            try {
                builder.setLargeIcon(BitmapFactory.decodeResource(context.getResources(), context.getApplicationInfo().icon));
            } catch (Throwable ignored) {}

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                builder.setColor(status.statusColor);
            }

            nm.notify(41207, builder.build());
            Log.i(TAG, "Posted luxury licence reminder: " + title);
        } catch (Exception e) {
            Log.e(TAG, "Failed to post licence reminder: " + e.getMessage(), e);
        }
    }
}
