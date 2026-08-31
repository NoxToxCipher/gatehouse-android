package au.com.dss.gatehouse;

import android.content.Context;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;

/**
 * Autonomous Shift Automation & SPARK Ada Core Attendance Engine.
 * 
 * Bridges BLE presence events with the append-only cryptographic ledger:
 * - Automatically opens shift / checks-in when rostered guard arrives on site.
 * - Logs unscheduled / emergency supervisor visits under TOPIC_SITE_ACCESS with exact dwell times.
 * - Detects dual-presence handovers and auto-seals & continues unbroken cryptographic chains on departure.
 */
public class ShiftAutomationEngine implements BlePresenceManager.PresenceEventListener {

    public interface AutomationCallback {
        void onLedgerEntryAppended(String summary);
        void onHandoverReady(BlePresenceManager.PeerPresence incomingGuard);
        void onShiftAutoClosed(String summary, String reportText);
        void onGateAlertTriggered(String title, String message);
    }

    private final Context context;
    private final DssKeyManager keyManager;
    private final BlePresenceManager presenceManager;
    private AutomationCallback callback;

    private final Set<String> loggedArrivalsThisShift = new HashSet<String>();
    private boolean isShiftActive = false;

    public ShiftAutomationEngine(Context ctx, DssKeyManager km, BlePresenceManager pm) {
        this.context = ctx;
        this.keyManager = km;
        this.presenceManager = pm;
        this.presenceManager.setListener(this);
    }

    public void setCallback(AutomationCallback cb) {
        this.callback = cb;
    }

    public void setShiftActive(boolean active) {
        this.isShiftActive = active;
        if (!active) {
            loggedArrivalsThisShift.clear();
        }
    }

    private static int nowMinutes() {
        long ms = System.currentTimeMillis();
        return (int) ((ms + TimeZone.getDefault().getOffset(ms)) / 60000L);
    }

    private String formatTime(long ms) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.US);
        sdf.setTimeZone(TimeZone.getDefault());
        return sdf.format(new Date(ms));
    }

    @Override
    public void onPeerArrived(BlePresenceManager.PeerPresence peer) {
        int now = nowMinutes();
        int t = Math.max(now, Core.lastRecorded());
        String timeStr = formatTime(peer.firstSeenMs);

        DssKeyManager.GuardProfile activeGuard = keyManager.getActiveGuard();

        if (peer.guardId.equalsIgnoreCase(activeGuard.guardId)) {
            return;
        }

        // Unscheduled Visit / Colleague on Site
        String entryText = "Site Access: " + peer.name + " (" + peer.licence + ") arrived on site [" + timeStr + "]";
        if (!loggedArrivalsThisShift.contains(peer.guardId + "_in")) {
            loggedArrivalsThisShift.add(peer.guardId + "_in");
            int res = Core.addNote(Core.KIND_OBSERVATION, Core.TOPIC_SITE_ACCESS, now, t, entryText, 0);
            if (callback != null) {
                callback.onLedgerEntryAppended("🔔 " + entryText);
            }
        }

        // Handover Check
        int curHourMin = now % 1440;
        boolean isHandoverWindow = (curHourMin >= 340 && curHourMin <= 380) || (curHourMin >= 1060 && curHourMin <= 1100);
        if (isHandoverWindow && callback != null) {
            callback.onHandoverReady(peer);
        }
    }

    @Override
    public void onPeerDwellUpdated(BlePresenceManager.PeerPresence peer) {
    }

    @Override
    public void onPeerDeparted(BlePresenceManager.PeerPresence peer, long dwellMinutes) {
        int now = nowMinutes();
        int t = Math.max(now, Core.lastRecorded());
        String timeStr = formatTime(peer.lastSeenMs);

        DssKeyManager.GuardProfile activeGuard = keyManager.getActiveGuard();

        if (peer.guardId.equalsIgnoreCase(activeGuard.guardId)) {
            executeAutoShiftClosure("Shift Complete: Primary Officer " + activeGuard.name + " departed site");
            return;
        }

        String exitText = "Site Access: " + peer.name + " departed site [" + timeStr + "] (Dwell: " + dwellMinutes + "m)";
        String key = peer.guardId + "_out";
        if (!loggedArrivalsThisShift.contains(key)) {
            loggedArrivalsThisShift.add(key);
            Core.addNote(Core.KIND_OBSERVATION, Core.TOPIC_SITE_ACCESS, now, t, exitText, 0);
            if (callback != null) {
                callback.onLedgerEntryAppended("🚗 " + exitText);
            }
        }
    }

    @Override
    public void onGateArrivalAlertReceived(String fromNode, String message, long timestampMs) {
        if (callback != null) {
            callback.onGateAlertTriggered("🚨 GATE ALERT (" + fromNode + ")", message);
        }
    }

    public void executeAutoShiftClosure(String sealReason) {
        int now = nowMinutes();
        int t = Math.max(now, Core.lastRecorded());

        if (Core.isSealed() == 0) {
            Core.seal(t, t, sealReason);
            Core.kept();
            String report = Core.report(now - 720, now);
            if (callback != null) {
                callback.onShiftAutoClosed("🔒 Shift Sealed & Continuous Chain Kept", report);
            }
        }
    }

    public void executeHandoverToNewGuard(BlePresenceManager.PeerPresence incomingGuard) {
        int now = nowMinutes();
        int t = Math.max(now, Core.lastRecorded());

        if (Core.isSealed() == 0) {
            Core.seal(t, t, "Handover to Officer " + incomingGuard.name + " (" + incomingGuard.licence + ")");
            Core.kept();
        }

        keyManager.switchActiveGuardByPinOrId(incomingGuard.guardId);

        String headHash = Core.head();
        int contRes = Core.continueShift(now, t, "Shift commenced via BLE handover on site");
        if (contRes != Core.OK) {
            Core.openShift(headHash, Core.siteHash(), now, t, "Relief shift started on site");
        }

        if (callback != null) {
            callback.onLedgerEntryAppended("🤝 Handover Complete: Active Guard is now " + incomingGuard.name);
        }
    }
}
