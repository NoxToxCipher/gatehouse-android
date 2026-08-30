package au.com.dss.gatehouse;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.Parcelable;
import android.util.Base64;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * NFC Guard Bump Peer Handshake Engine.
 * 
 * Enables zero-friction physical tap-to-trust ceremony between guard devices.
 * Tapping two guard phones exchanges guard identity & BLE public identification tokens,
 * allowing subsequent passive presence detection without manual pairing.
 */
public class NfcPeerExchange {
    public static final String MIME_DSS_PEER = "application/vnd.au.com.dss.gatehouse.peer";
    private static final String PREF_NAME = "dss_nfc_peers";
    private static final String KEY_PEER_LIST = "trusted_peer_list";

    public static class TrustedPeerRecord {
        public String guardId;
        public String name;
        public String licence;
        public String blePubKey;
        public long pairedTimestampMs;

        public TrustedPeerRecord(String id, String n, String l, String ble, long time) {
            this.guardId = id;
            this.name = n;
            this.licence = l;
            this.blePubKey = ble;
            this.pairedTimestampMs = time;
        }

        public String serialize() {
            return guardId + "|" + name + "|" + licence + "|" + blePubKey + "|" + pairedTimestampMs;
        }

        public static TrustedPeerRecord deserialize(String s) {
            if (s == null) return null;
            String[] p = s.split("\\|");
            if (p.length < 5) return null;
            try {
                return new TrustedPeerRecord(p[0], p[1], p[2], p[3], Long.parseLong(p[4]));
            } catch (Exception e) {
                return null;
            }
        }
    }

    public interface PeerHandshakeListener {
        void onPeerHandshakeComplete(TrustedPeerRecord peer, boolean isNew);
    }

    private final Context context;
    private final SharedPreferences prefs;
    private final NfcAdapter nfcAdapter;
    private PeerHandshakeListener listener;
    private final List<TrustedPeerRecord> cachedPeers = new ArrayList<TrustedPeerRecord>();

    public NfcPeerExchange(Context ctx) {
        this.context = ctx;
        this.prefs = ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.nfcAdapter = NfcAdapter.getDefaultAdapter(ctx);
        loadPeers();
    }

    public void setListener(PeerHandshakeListener l) {
        this.listener = l;
    }

    public boolean isNfcSupported() {
        return nfcAdapter != null;
    }

    public boolean isNfcEnabled() {
        return nfcAdapter != null && nfcAdapter.isEnabled();
    }

    public void enableForegroundDispatch(Activity activity) {
        if (nfcAdapter == null || !nfcAdapter.isEnabled()) return;
        try {
            Intent intent = new Intent(activity, activity.getClass());
            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            PendingIntent pendingIntent = PendingIntent.getActivity(activity, 0, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | (android.os.Build.VERSION.SDK_INT >= 31 ? PendingIntent.FLAG_MUTABLE : 0));
            IntentFilter filter = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
            filter.addDataType(MIME_DSS_PEER);
            IntentFilter tagFilter = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
            nfcAdapter.enableForegroundDispatch(activity, pendingIntent, new IntentFilter[]{filter, tagFilter}, null);
        } catch (Exception e) {}
    }

    public void disableForegroundDispatch(Activity activity) {
        if (nfcAdapter == null) return;
        try {
            nfcAdapter.disableForegroundDispatch(activity);
        } catch (Exception e) {}
    }

    public NdefMessage createHandshakeMessage(DssKeyManager.GuardProfile myGuard) {
        if (myGuard == null) return null;
        String payload = myGuard.guardId + ":" + myGuard.name + ":" + myGuard.licence + ":" + myGuard.blePubKey;
        byte[] payloadBytes = payload.getBytes(StandardCharsets.UTF_8);
        NdefRecord record = NdefRecord.createMime(MIME_DSS_PEER, payloadBytes);
        return new NdefMessage(new NdefRecord[]{record});
    }

    public boolean processNfcIntent(Intent intent) {
        if (intent == null) return false;
        String action = intent.getAction();
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action) || NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)) {
            Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            if (rawMsgs != null && rawMsgs.length > 0) {
                for (Parcelable raw : rawMsgs) {
                    NdefMessage msg = (NdefMessage) raw;
                    for (NdefRecord rec : msg.getRecords()) {
                        byte[] payload = rec.getPayload();
                        if (payload != null && payload.length > 0) {
                            String text = new String(payload, StandardCharsets.UTF_8);
                            if (processPayloadText(text)) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    public boolean processPayloadText(String text) {
        if (text == null) return false;
        String[] parts = text.split(":");
        if (parts.length >= 4) {
            String id = parts[0];
            String name = parts[1];
            String licence = parts[2];
            String ble = parts[3];
            TrustedPeerRecord peer = new TrustedPeerRecord(id, name, licence, ble, System.currentTimeMillis());
            boolean isNew = savePeer(peer);
            if (listener != null) {
                listener.onPeerHandshakeComplete(peer, isNew);
            }
            return true;
        }
        return false;
    }

    private void loadPeers() {
        cachedPeers.clear();
        String raw = prefs.getString(KEY_PEER_LIST, "");
        if (!raw.isEmpty()) {
            String[] lines = raw.split("\n");
            for (String l : lines) {
                TrustedPeerRecord r = TrustedPeerRecord.deserialize(l);
                if (r != null) cachedPeers.add(r);
            }
        }
    }

    public synchronized boolean savePeer(TrustedPeerRecord newPeer) {
        boolean exists = false;
        for (int i = 0; i < cachedPeers.size(); i++) {
            if (cachedPeers.get(i).guardId.equals(newPeer.guardId)) {
                cachedPeers.set(i, newPeer);
                exists = true;
                break;
            }
        }
        if (!exists) {
            cachedPeers.add(0, newPeer);
        }
        saveAllPeers();
        return !exists;
    }

    private void saveAllPeers() {
        StringBuilder sb = new StringBuilder();
        for (TrustedPeerRecord p : cachedPeers) {
            sb.append(p.serialize()).append("\n");
        }
        prefs.edit().putString(KEY_PEER_LIST, sb.toString()).apply();
    }

    public List<TrustedPeerRecord> getTrustedPeers() {
        return new ArrayList<TrustedPeerRecord>(cachedPeers);
    }
}
