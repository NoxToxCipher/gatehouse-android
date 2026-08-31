package au.com.dss.gatehouse;

/** The record core, as Java sees it.
 *
 * Every method here is one call into the Ada library. Nothing on this side
 * decides whether an entry is allowed: the core refuses it and leaves a
 * sentence behind, and {@link #lastReason} is that sentence. A screen that
 * explained the rules in its own words would eventually explain them wrongly.
 */
public final class Core {
    static { System.loadLibrary("gatehouse_core");
        System.loadLibrary("gatehouse"); }

    private Core() { }

    public static final int OK = 0;

    public static final int KIND_OBSERVATION = 4;

    public static final int TOPIC_ROUTINE = 0;
    public static final int TOPIC_FOR_DAY_CREW = 1;
    public static final int TOPIC_INCIDENT = 2;
    public static final int TOPIC_SITE_ACCESS = 3;

    public static final int DEVICE_PERSONAL = 0;
    public static final int DEVICE_SHARED = 1;

    public static final int METHOD_SESSION = 0;
    public static final int METHOD_PIN = 1;
    public static final int METHOD_CARD = 2;
    public static final int METHOD_ROSTER_ONLY = 3;

    public static final int AUTH_CRYPTOGRAPHIC = 2;

    public static native int encodingVersion();
    public static native int archiveVersion();

    public static native int siteBegin(String name);
    public static native int siteAddPoint(String label, String uid);
    public static native int siteAddOption(String label, String text);
    public static native int sitePolicy(int everyPoint, int maxGap, int rounds);
    public static native String siteHash();
    public static native String genesis();

    public static native int setAttribution(int device, int method);
    public static native int setGuard(String id, String name, String licence,
                                      String source, String ext);

    public static native int openShift(String prior, String policy,
                                       int occurred, int recorded, String text);
    public static native int addCheckpoint(int occurred, int recorded,
                                           String label, String uid,
                                           int taps, int auth);
    public static native int addNote(int kind, int topic, int occurred,
                                     int recorded, String text, int refers);
    public static native int seal(int occurred, int recorded, String text);

    /** One entry as a line for the screen, 1 based; "" once past the end.
     *  The wording is the record's, so what a guard watches go in matches
     *  what the handover page will say about it. */
    public static native String entryLine(int i);

    /** What the record already holds for a checkpoint: how many times it has
     *  been recorded tonight, and the minute of the last one (0 for none). */
    public static native int pointVisits(String label);
    public static native int pointLast(String label);

    /** The recorded time of the last entry, or 0 on an empty record. Take
     *  the later of this and the clock so a recorded time never goes
     *  backwards, which is what happens on the daylight saving changeover. */
    public static native int lastRecorded();

    public static native int entryCount();
    public static native int isSealed();
    public static native int verified();
    public static native String head();
    public static native String lastReason();

    public static native int kept();
    public static native int isSaved();
    public static native int continueShift(int occurred, int recorded,
                                           String text);

    public static native String report(int opens, int closes);
    public static native String archive();
}
