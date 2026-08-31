package au.com.dss.gatehouse;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DSS Key Account & Device Provisioning Engine.
 * 
 * Handles serverless, zero-overhead key provisioning for Doherty Security Services:
 * - DSS-G-[Payload]: Guard Profile Keys (Personal or Shared Switcher)
 * - DSS-S-[Payload]: Site Phone Keys (Hut Base or Patrol Rover shared terminals)
 * 
 * Directly primes the SPARK Ada record core attribution (Core.setGuard, Core.setAttribution).
 */
public class DssKeyManager {
    private static final String PREF_NAME = "dss_auth_vault";
    private static final String KEY_PROVISIONED = "is_provisioned";
    private static final String KEY_DEVICE_TYPE = "device_type"; // 0 = PERSONAL, 1 = SHARED
    private static final String KEY_UNIT_ROLE = "unit_role";     // "PERSONAL", "HUT_BASE", "PATROL_ROVER"
    private static final String KEY_SITE_ID = "site_id";
    private static final String KEY_SITE_NAME = "site_name";
    
    // Active Guard State
    private static final String KEY_GUARD_ID = "guard_id";
    private static final String KEY_GUARD_NAME = "guard_name";
    private static final String KEY_GUARD_LICENCE = "guard_licence";
    private static final String KEY_GUARD_PIN = "guard_pin";
    private static final String KEY_GUARD_BLE_PUB = "guard_ble_pub";

    public static class GuardProfile {
        public String guardId;
        public String name;
        public String licence;
        public String pin;
        public String blePubKey;

        public GuardProfile(String id, String n, String l, String p, String ble) {
            this.guardId = id;
            this.name = n;
            this.licence = l;
            this.pin = p;
            this.blePubKey = ble;
        }

        public String toKeyString() {
            String raw = guardId + ":" + name + ":" + licence + ":" + pin + ":" + blePubKey;
            String b64 = Base64.encodeToString(raw.getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP | Base64.URL_SAFE);
            String checksum = computeChecksum(b64);
            return "DSS-G-" + b64 + "-" + checksum;
        }

        public static GuardProfile fromKeyString(String key) {
            if (key == null || !key.startsWith("DSS-G-")) return null;
            String[] parts = key.split("-");
            if (parts.length < 4) return null;
            String b64 = parts[2];
            String claimedChecksum = parts[3];
            if (!computeChecksum(b64).equalsIgnoreCase(claimedChecksum)) {
                return null;
            }
            try {
                byte[] decoded = Base64.decode(b64, Base64.URL_SAFE);
                String raw = new String(decoded, StandardCharsets.UTF_8);
                String[] fields = raw.split(":");
                if (fields.length < 5) return null;
                return new GuardProfile(fields[0], fields[1], fields[2], fields[3], fields[4]);
            } catch (Exception e) {
                return null;
            }
        }
    }

    public static class SiteProfile {
        public String siteId;
        public String siteName;
        public String unitRole; // "HUT_BASE" or "PATROL_ROVER"

        public SiteProfile(String id, String n, String role) {
            this.siteId = id;
            this.siteName = n;
            this.unitRole = role;
        }

        public String toKeyString() {
            String raw = siteId + ":" + siteName + ":" + unitRole;
            String b64 = Base64.encodeToString(raw.getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP | Base64.URL_SAFE);
            String checksum = computeChecksum(b64);
            return "DSS-S-" + b64 + "-" + checksum;
        }

        public static SiteProfile fromKeyString(String key) {
            if (key == null || !key.startsWith("DSS-S-")) return null;
            String[] parts = key.split("-");
            if (parts.length < 4) return null;
            String b64 = parts[2];
            String claimedChecksum = parts[3];
            if (!computeChecksum(b64).equalsIgnoreCase(claimedChecksum)) {
                return null;
            }
            try {
                byte[] decoded = Base64.decode(b64, Base64.URL_SAFE);
                String raw = new String(decoded, StandardCharsets.UTF_8);
                String[] fields = raw.split(":");
                if (fields.length < 3) return null;
                return new SiteProfile(fields[0], fields[1], fields[2]);
            } catch (Exception e) {
                return null;
            }
        }
    }

    private final SharedPreferences prefs;
    private final Map<String, GuardProfile> localRosterDirectory = new HashMap<String, GuardProfile>();

    public DssKeyManager(Context context) {
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        populateStandardCompanyRoster();
        ensureInitialDefaults();
    }

    private void populateStandardCompanyRoster() {
        addStandardGuard(new GuardProfile("g-lochran", "Lochran Mackenzie Doherty", "LIC-3943517", "3943", "DSS-BLE-LOCHRAN-3943"));
        addStandardGuard(new GuardProfile("g-claren", "Claren Scott Doherty", "LIC-4611218", "4611", "DSS-BLE-CLAREN-4611"));
        addStandardGuard(new GuardProfile("g-brush", "Brian Mark Rush", "LIC-3186510", "3186", "DSS-BLE-BRIAN-3186"));
        addStandardGuard(new GuardProfile("g-wnewman", "William George Newman", "LIC-3140959", "3140", "DSS-BLE-WILLIAM-3140"));
        addStandardGuard(new GuardProfile("g-jwnewman", "James William Newman", "LIC-4098924", "4098", "DSS-BLE-JAMES-4098"));
        addStandardGuard(new GuardProfile("g-jnaylor", "Jonathan Richard Naylor", "LIC-3200107", "3200", "DSS-BLE-JON-3200"));
        addStandardGuard(new GuardProfile("g-cireton", "Christopher John Ireton", "LIC-4716179", "4716", "DSS-BLE-CHRIS-4716"));
        addStandardGuard(new GuardProfile("g-jedwards", "Joshua Thomas Edwards", "LIC-4123824", "4123", "DSS-BLE-JOSH-4123"));
        addStandardGuard(new GuardProfile("g-rparkinson", "Roger Parkinson", "LIC-3211239", "3211", "DSS-BLE-ROGER-3211"));
        addStandardGuard(new GuardProfile("g-kgordon", "Kenneth David Gordon", "LIC-3510774", "3510", "DSS-BLE-KEN-3510"));
    }

    private void addStandardGuard(GuardProfile p) {
        localRosterDirectory.put(p.guardId, p);
        localRosterDirectory.put(p.name.toLowerCase(), p);
        localRosterDirectory.put(p.pin, p);
    }

    private void ensureInitialDefaults() {
        if (!prefs.contains(KEY_PROVISIONED)) {
            GuardProfile defaultGuard = localRosterDirectory.get("g-lochran");
            if (defaultGuard != null) {
                provisionGuardKey(defaultGuard, true);
            }
        }
    }

    public boolean isProvisioned() {
        return prefs.getBoolean(KEY_PROVISIONED, false);
    }

    public boolean isSharedSitePhone() {
        return prefs.getInt(KEY_DEVICE_TYPE, Core.DEVICE_PERSONAL) == Core.DEVICE_SHARED;
    }

    public String getUnitRole() {
        return prefs.getString(KEY_UNIT_ROLE, "PERSONAL");
    }

    public String getSiteName() {
        return prefs.getString(KEY_SITE_NAME, "Hume Doors & Timber, Kingston");
    }

    public GuardProfile getActiveGuard() {
        String id = prefs.getString(KEY_GUARD_ID, "g-lochran");
        String name = prefs.getString(KEY_GUARD_NAME, "Lochran Doherty");
        String licence = prefs.getString(KEY_GUARD_LICENCE, "LIC-41207");
        String pin = prefs.getString(KEY_GUARD_PIN, "4120");
        String ble = prefs.getString(KEY_GUARD_BLE_PUB, "DSS-BLE-LOCHRAN-4120");
        return new GuardProfile(id, name, licence, pin, ble);
    }

    public boolean provisionFromKeyString(String keyString) {
        if (keyString == null) return false;
        String clean = keyString.trim();
        if (clean.startsWith("DSS-G-")) {
            GuardProfile gp = GuardProfile.fromKeyString(clean);
            if (gp != null) {
                provisionGuardKey(gp, true);
                return true;
            }
        } else if (clean.startsWith("DSS-S-")) {
            SiteProfile sp = SiteProfile.fromKeyString(clean);
            if (sp != null) {
                provisionSiteKey(sp);
                return true;
            }
        }
        return false;
    }

    public void provisionGuardKey(GuardProfile guard, boolean isPersonal) {
        localRosterDirectory.put(guard.guardId, guard);
        prefs.edit()
                .putBoolean(KEY_PROVISIONED, true)
                .putInt(KEY_DEVICE_TYPE, isPersonal ? Core.DEVICE_PERSONAL : Core.DEVICE_SHARED)
                .putString(KEY_UNIT_ROLE, isPersonal ? "PERSONAL" : "PATROL_ROVER")
                .putString(KEY_GUARD_ID, guard.guardId)
                .putString(KEY_GUARD_NAME, guard.name)
                .putString(KEY_GUARD_LICENCE, guard.licence)
                .putString(KEY_GUARD_PIN, guard.pin)
                .putString(KEY_GUARD_BLE_PUB, guard.blePubKey)
                .apply();
        primeCoreAttribution();
    }

    public void provisionSiteKey(SiteProfile site) {
        prefs.edit()
                .putBoolean(KEY_PROVISIONED, true)
                .putInt(KEY_DEVICE_TYPE, Core.DEVICE_SHARED)
                .putString(KEY_UNIT_ROLE, site.unitRole)
                .putString(KEY_SITE_ID, site.siteId)
                .putString(KEY_SITE_NAME, site.siteName)
                .apply();
        primeCoreAttribution();
    }

    public boolean switchActiveGuardByPinOrId(String identifier) {
        if (identifier == null) return false;
        String query = identifier.trim().toLowerCase();
        GuardProfile target = localRosterDirectory.get(query);
        if (target == null) {
            for (GuardProfile p : localRosterDirectory.values()) {
                if (p.pin.equals(query) || p.licence.equalsIgnoreCase(query) || p.name.equalsIgnoreCase(query)) {
                    target = p;
                    break;
                }
            }
        }
        if (target != null) {
            prefs.edit()
                    .putString(KEY_GUARD_ID, target.guardId)
                    .putString(KEY_GUARD_NAME, target.name)
                    .putString(KEY_GUARD_LICENCE, target.licence)
                    .putString(KEY_GUARD_PIN, target.pin)
                    .putString(KEY_GUARD_BLE_PUB, target.blePubKey)
                    .apply();
            primeCoreAttribution();
            return true;
        }
        return false;
    }

    public void setHutBaseRole(boolean isHutBase) {
        prefs.edit()
                .putString(KEY_UNIT_ROLE, isHutBase ? "HUT_BASE" : "PATROL_ROVER")
                .apply();
    }

    public void primeCoreAttribution() {
        GuardProfile guard = getActiveGuard();
        int dev = prefs.getInt(KEY_DEVICE_TYPE, Core.DEVICE_PERSONAL);
        int meth = (dev == Core.DEVICE_PERSONAL) ? Core.METHOD_SESSION : Core.METHOD_SESSION;
        Core.setAttribution(dev, meth);
        String source = isSharedSitePhone() ? "site_terminal" : "personal_key";
        String ext = getUnitRole();
        Core.setGuard(guard.guardId, guard.name, guard.licence, source, ext);
    }

    public List<GuardProfile> getAllKnownGuards() {
        Map<String, GuardProfile> unique = new HashMap<String, GuardProfile>();
        for (GuardProfile p : localRosterDirectory.values()) {
            unique.put(p.guardId, p);
        }
        return new ArrayList<GuardProfile>(unique.values());
    }

    public GuardProfile findGuardByBleKey(String bleKey) {
        if (bleKey == null) return null;
        for (GuardProfile p : localRosterDirectory.values()) {
            if (bleKey.equalsIgnoreCase(p.blePubKey)) {
                return p;
            }
        }
        return null;
    }

    public static String computeChecksum(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 4; i++) {
                sb.append(String.format("%02X", hash[i]));
            }
            return sb.toString();
        } catch (Exception e) {
            return "00000000";
        }
    }
}
