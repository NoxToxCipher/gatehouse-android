package au.com.dss.gatehouse;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelUuid;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Autonomous BLE Presence & Hut Anchor Node Engine.
 * 
 * Manages background BLE advertising and scanning for trusted guard peers.
 * - Senses arriving guards/supervisors within ~35-50m radius.
 * - Computes on-site dwell times & departure transitions.
 * - Hut Anchor Node: Broadcasts high-priority Gate Arrival alerts to patrol rovers.
 */
public class BlePresenceManager {
    // 128-bit Service UUID dedicated to Doherty Security Services Gatehouse Mesh
    public static final UUID DSS_MESH_SERVICE_UUID = UUID.fromString("0000d550-0000-1000-8000-00805f9b34fb");
    public static final ParcelUuid DSS_PARCEL_UUID = new ParcelUuid(DSS_MESH_SERVICE_UUID);
    public static final int DSS_MANUFACTURER_ID = 0x055D; // DSS custom ID

    private static final long DEPARTURE_TIMEOUT_MS = 90 * 1000L; // 90s without ping = departed
    private static final int RSSI_PROXIMITY_THRESHOLD = -88; // dBm threshold for on-site presence

    public enum PresenceState {
        ARRIVED,
        ON_SITE,
        DEPARTED
    }

    public static class PeerPresence {
        public String guardId;
        public String name;
        public String licence;
        public String blePubKey;
        public long firstSeenMs;
        public long lastSeenMs;
        public int lastRssi;
        public PresenceState state;
        public boolean isRostered;
        public boolean departureLogged;

        public PeerPresence(String id, String n, String l, String ble, int rssi) {
            this.guardId = id;
            this.name = n;
            this.licence = l;
            this.blePubKey = ble;
            this.firstSeenMs = System.currentTimeMillis();
            this.lastSeenMs = this.firstSeenMs;
            this.lastRssi = rssi;
            this.state = PresenceState.ARRIVED;
            this.isRostered = false;
            this.departureLogged = false;
        }

        public long getDwellMinutes() {
            return Math.max(1, (lastSeenMs - firstSeenMs) / 60000L);
        }
    }

    public interface PresenceEventListener {
        void onPeerArrived(PeerPresence peer);
        void onPeerDwellUpdated(PeerPresence peer);
        void onPeerDeparted(PeerPresence peer, long dwellMinutes);
        void onGateArrivalAlertReceived(String fromNode, String message, long timestampMs);
    }

    private final Context context;
    private final DssKeyManager keyManager;
    private final NfcPeerExchange peerExchange;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private PresenceEventListener listener;

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeAdvertiser advertiser;
    private BluetoothLeScanner scanner;
    private boolean isScanning = false;
    private boolean isAdvertising = false;
    private boolean isHutAnchorMode = false;

    private final Map<String, PeerPresence> activePeers = new HashMap<String, PeerPresence>();
    private final Runnable cleanupTicker = new Runnable() {
        @Override
        public void run() {
            checkDepartures();
            mainHandler.postDelayed(this, 10000);
        }
    };

    public BlePresenceManager(Context ctx, DssKeyManager km, NfcPeerExchange nfc) {
        this.context = ctx;
        this.keyManager = km;
        this.peerExchange = nfc;
        initBluetooth();
        mainHandler.postDelayed(cleanupTicker, 10000);
    }

    public void setListener(PresenceEventListener l) {
        this.listener = l;
    }

    public void setHutAnchorMode(boolean isHut) {
        this.isHutAnchorMode = isHut;
        keyManager.setHutBaseRole(isHut);
    }

    public boolean isHutAnchorMode() {
        return isHutAnchorMode;
    }

    private void initBluetooth() {
        BluetoothManager bm = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        if (bm != null) {
            bluetoothAdapter = bm.getAdapter();
        }
    }

    public boolean isBluetoothAvailable() {
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled();
    }

    public void start() {
        if (!isBluetoothAvailable()) return;
        startAdvertising();
        startScanning();
    }

    public void stop() {
        stopAdvertising();
        stopScanning();
    }

    public void startAdvertising() {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled() || isAdvertising) return;
        advertiser = bluetoothAdapter.getBluetoothLeAdvertiser();
        if (advertiser == null) return;

        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
                .setConnectable(false)
                .setTimeout(0)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
                .build();

        DssKeyManager.GuardProfile activeGuard = keyManager.getActiveGuard();
        String payload = (isHutAnchorMode ? "HUT:" : "G:") + activeGuard.guardId + ":" + activeGuard.blePubKey;
        byte[] payloadBytes = payload.getBytes(StandardCharsets.UTF_8);

        AdvertiseData data = new AdvertiseData.Builder()
                .addServiceUuid(DSS_PARCEL_UUID)
                .addManufacturerData(DSS_MANUFACTURER_ID, payloadBytes)
                .setIncludeTxPowerLevel(false)
                .build();

        try {
            advertiser.startAdvertising(settings, data, advertiseCallback);
            isAdvertising = true;
        } catch (Exception e) {}
    }

    public void stopAdvertising() {
        if (advertiser != null && isAdvertising) {
            try {
                advertiser.stopAdvertising(advertiseCallback);
            } catch (Exception e) {}
            isAdvertising = false;
        }
    }

    private final AdvertiseCallback advertiseCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            isAdvertising = true;
        }
        @Override
        public void onStartFailure(int errorCode) {
            isAdvertising = false;
        }
    };

    public void startScanning() {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled() || isScanning) return;
        scanner = bluetoothAdapter.getBluetoothLeScanner();
        if (scanner == null) return;

        ScanFilter filter = new ScanFilter.Builder()
                .setServiceUuid(DSS_PARCEL_UUID)
                .build();

        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
                .build();

        try {
            scanner.startScan(Collections.singletonList(filter), settings, scanCallback);
            isScanning = true;
        } catch (Exception e) {}
    }

    public void stopScanning() {
        if (scanner != null && isScanning) {
            try {
                scanner.stopScan(scanCallback);
            } catch (Exception e) {}
            isScanning = false;
        }
    }

    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            processScanResult(result);
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            for (ScanResult r : results) {
                processScanResult(r);
            }
        }
    };

    private void processScanResult(ScanResult result) {
        if (result == null) return;
        ScanRecord record = result.getScanRecord();
        if (record == null) return;
        byte[] rawBytes = record.getManufacturerSpecificData(DSS_MANUFACTURER_ID);
        if (rawBytes == null || rawBytes.length == 0) return;

        String payload = new String(rawBytes, StandardCharsets.UTF_8);
        int rssi = result.getRssi();
        handleMeshPacket(payload, rssi);
    }

    public void handleMeshPacket(String payload, int rssi) {
        if (payload == null || payload.isEmpty()) return;

        // Check for Gate Arrival Alert
        if (payload.startsWith("ALERT_GATE_ARRIVAL:")) {
            String[] parts = payload.split(":");
            String fromNode = parts.length > 1 ? parts[1] : "Hut Base";
            String msg = parts.length > 2 ? parts[2] : "Arrival at Gatehouse Main Gate";
            if (listener != null) {
                listener.onGateArrivalAlertReceived(fromNode, msg, System.currentTimeMillis());
            }
            return;
        }

        // Process Guard Presence
        boolean isHut = payload.startsWith("HUT:");
        String clean = isHut ? payload.substring(4) : (payload.startsWith("G:") ? payload.substring(2) : payload);
        String[] parts = clean.split(":");
        if (parts.length < 2) return;

        String guardId = parts[0];
        String bleKey = parts[1];

        // Ignore self
        DssKeyManager.GuardProfile myGuard = keyManager.getActiveGuard();
        if (guardId.equalsIgnoreCase(myGuard.guardId)) return;

        // Lookup Guard Info from KeyManager or TrustedPeers
        String name = guardId;
        String licence = "LIC #VERIFIED";
        DssKeyManager.GuardProfile found = keyManager.findGuardByBleKey(bleKey);
        if (found != null) {
            name = found.name;
            licence = found.licence;
        } else {
            for (NfcPeerExchange.TrustedPeerRecord p : peerExchange.getTrustedPeers()) {
                if (p.guardId.equalsIgnoreCase(guardId) || p.blePubKey.equalsIgnoreCase(bleKey)) {
                    name = p.name;
                    licence = p.licence;
                    break;
                }
            }
        }

        synchronized (activePeers) {
            PeerPresence peer = activePeers.get(guardId);
            long now = System.currentTimeMillis();
            if (peer == null) {
                peer = new PeerPresence(guardId, name, licence, bleKey, rssi);
                activePeers.put(guardId, peer);
                if (listener != null) {
                    listener.onPeerArrived(peer);
                }
            } else {
                peer.lastSeenMs = now;
                peer.lastRssi = rssi;
                if (peer.state == PresenceState.DEPARTED) {
                    peer.state = PresenceState.ARRIVED;
                    peer.firstSeenMs = now;
                    peer.departureLogged = false;
                    if (listener != null) {
                        listener.onPeerArrived(peer);
                    }
                } else {
                    peer.state = PresenceState.ON_SITE;
                    if (listener != null) {
                        listener.onPeerDwellUpdated(peer);
                    }
                }
            }
        }
    }

    public void broadcastGateArrivalAlert(String customMsg) {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) return;
        BluetoothLeAdvertiser adv = bluetoothAdapter.getBluetoothLeAdvertiser();
        if (adv == null) return;

        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                .setTimeout(4000)
                .build();

        String node = isHutAnchorMode ? "Hut Base Station" : keyManager.getActiveGuard().name;
        String msg = customMsg != null ? customMsg : "Vehicle / Visitor Arrival at Main Gate";
        String payload = "ALERT_GATE_ARRIVAL:" + node + ":" + msg;
        byte[] payloadBytes = payload.getBytes(StandardCharsets.UTF_8);

        AdvertiseData data = new AdvertiseData.Builder()
                .addServiceUuid(DSS_PARCEL_UUID)
                .addManufacturerData(DSS_MANUFACTURER_ID, payloadBytes)
                .build();

        try {
            adv.startAdvertising(settings, data, new AdvertiseCallback() {
                @Override
                public void onStartSuccess(AdvertiseSettings settingsInEffect) {}
            });
        } catch (Exception e) {}
    }

    private void checkDepartures() {
        long now = System.currentTimeMillis();
        synchronized (activePeers) {
            for (PeerPresence p : activePeers.values()) {
                if (p.state != PresenceState.DEPARTED && (now - p.lastSeenMs) > DEPARTURE_TIMEOUT_MS) {
                    p.state = PresenceState.DEPARTED;
                    long dwellMins = p.getDwellMinutes();
                    if (listener != null && !p.departureLogged) {
                        p.departureLogged = true;
                        listener.onPeerDeparted(p, dwellMins);
                    }
                }
            }
        }
    }

    public List<PeerPresence> getOnSitePeers() {
        List<PeerPresence> list = new ArrayList<PeerPresence>();
        synchronized (activePeers) {
            for (PeerPresence p : activePeers.values()) {
                if (p.state != PresenceState.DEPARTED) {
                    list.add(p);
                }
            }
        }
        return list;
    }

    public List<PeerPresence> getAllPeersHistory() {
        synchronized (activePeers) {
            return new ArrayList<PeerPresence>(activePeers.values());
        }
    }

    public void simulatePeerArrival(String guardId, String name, String licence, String bleKey) {
        String payload = "G:" + guardId + ":" + bleKey;
        handleMeshPacket(payload, -65);
    }

    public void simulatePeerDeparture(String guardId) {
        synchronized (activePeers) {
            PeerPresence p = activePeers.get(guardId);
            if (p != null && p.state != PresenceState.DEPARTED) {
                p.state = PresenceState.DEPARTED;
                p.lastSeenMs = System.currentTimeMillis() + 1000L;
                long dwellMins = p.getDwellMinutes();
                if (listener != null && !p.departureLogged) {
                    p.departureLogged = true;
                    listener.onPeerDeparted(p, dwellMins);
                }
            }
        }
    }
}
