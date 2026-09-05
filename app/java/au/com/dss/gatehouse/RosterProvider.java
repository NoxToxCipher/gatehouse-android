package au.com.dss.gatehouse;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * RosterProvider — the vendor-neutral contract Gatehouse consumes for
 * rostering, timesheets and post orders.
 *
 * Deputy is one implementation ({@link DeputyApi}); Tanda, a manual/CSV
 * source, or a future in-house back-end implement the same interface, so the
 * app never names a concrete vendor. Obtain the active provider via
 * {@link Rostering#create(android.content.Context)} rather than constructing
 * a vendor class directly.
 *
 * The model below is deliberately security-rostering shaped: it carries award
 * tags, coworker overlap and open-shift flags that generic tools leave out, so
 * every provider maps into the same shape Gatehouse already renders.
 */
public interface RosterProvider {

    interface Callback<T> {
        void onSuccess(T result);
        void onError(String errorMessage);
    }

    /** One rostered or worked shift, normalised across providers. */
    class Shift {
        public int id;
        public int employeeId;
        public String guardName = "";
        public long startTs; // Unix epoch in seconds
        public long endTs;   // Unix epoch in seconds
        public double totalHours;
        public String operationalUnit = "Guard Hut";
        public String status = "CONFIRMED"; // ACTIVE, CONFIRMED, DONE, SCHEDULED, REST, OPEN
        public boolean isCurrentGuard = false;
        public boolean isLiveNow = false;
        public String notes = "";
        public String dateString = ""; // YYYY-MM-DD

        // Coworker & joint overlap telemetry
        public boolean hasCoworkerOverlap = false;
        public String coworkerName = "";
        public String coworkerOperationalUnit = "";
        public long coworkerStartTs = 0L;
        public long coworkerEndTs = 0L;
        public double overlapHours = 0.0;

        // Security Award MA000115 & rates
        public double baseHourlyRate = 31.85; // Level 3 Security Officer
        public double effectiveHourlyRate = 36.63; // 15% night loading
        public String awardRateTag = "+15% Night Loading (MA000115)";
        public double estimatedGrossPay = 439.56; // 12h @ $36.63

        // Fatigue & health pacer
        public double restHoursPrior = 14.5;
        public boolean isFatigueCompliant = true; // >= 10h break rule

        // WHS shift weather outlook
        public String shiftWeatherSummary = "21.4°C · SSE 14km/h · ⚡ Clear · 🧊 No Hail";

        // Open shift claiming
        public boolean isOpenShift = false;

        public String getFormattedHoursRange() {
            if (startTs <= 0 || endTs <= 0) return "18:00 – 06:00 (12.0h)";
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.US);
            String sStr = sdf.format(new Date(startTs * 1000L));
            String eStr = sdf.format(new Date(endTs * 1000L));
            double h = totalHours > 0 ? totalHours : ((endTs - startTs) / 3600.0);
            return String.format(Locale.US, "%s – %s (%.1fh)", sStr, eStr, h);
        }

        public String getDayDisplayLabel() {
            if (startTs <= 0) return dateString;

            SimpleDateFormat df = new SimpleDateFormat("EEE dd MMM", Locale.US);
            String base = df.format(new Date(startTs * 1000L));

            Calendar cNow = Calendar.getInstance();
            Calendar cShift = Calendar.getInstance();
            cShift.setTimeInMillis(startTs * 1000L);

            if (cNow.get(Calendar.YEAR) == cShift.get(Calendar.YEAR) &&
                cNow.get(Calendar.DAY_OF_YEAR) == cShift.get(Calendar.DAY_OF_YEAR)) {
                return "Tonight (" + base + ")";
            }
            cNow.add(Calendar.DAY_OF_YEAR, 1);
            if (cNow.get(Calendar.YEAR) == cShift.get(Calendar.YEAR) &&
                cNow.get(Calendar.DAY_OF_YEAR) == cShift.get(Calendar.DAY_OF_YEAR)) {
                return "Tomorrow (" + base + ")";
            }
            return base;
        }
    }

    /** A site document / post order, normalised across providers. */
    class Document {
        public String id;
        public String title;
        public String category; // "SOP", "EMERGENCY", "WHS", "LICENCE", "SITE_MAP"
        public String categoryLabel;
        public String icon;
        public String updatedDate;
        public String author;
        public String summary;
        public String contentMarkdown;
        public boolean isMandatory;
        public boolean isAttested;
        public long attestedTs;

        public Document(String id, String title, String category, String categoryLabel, String icon,
                        String updatedDate, String author, String summary, String contentMarkdown, boolean isMandatory) {
            this.id = id;
            this.title = title;
            this.category = category;
            this.categoryLabel = categoryLabel;
            this.icon = icon;
            this.updatedDate = updatedDate;
            this.author = author;
            this.summary = summary;
            this.contentMarkdown = contentMarkdown;
            this.isMandatory = isMandatory;
            this.isAttested = false;
            this.attestedTs = 0L;
        }
    }

    /** A full sync: this week's roster, who is on now, relief, and documents. */
    class Result {
        public boolean isLive = false;
        public long syncTimestamp = 0L;
        public String statusMessage = "";
        public String userName = "Lochran Doherty";
        public String companyName = "Hume Doors & Timber (Kingston)";
        public List<Shift> weekShifts = new ArrayList<>();
        public Shift activeShift = null;
        public List<Shift> onDutyGuards = new ArrayList<>();
        public Shift nextRelief = null;
        public List<Document> documents = new ArrayList<>();
    }

    // ---- Identity / configuration ----

    /** Human label for this back-end, e.g. "Deputy", "Tanda", "Manual". */
    String providerName();

    /** True when the provider has whatever credentials it needs to go live. */
    boolean isConfigured();

    boolean hasToken();
    String getToken();
    void setToken(String token);
    String getBaseUrl();
    void setBaseUrl(String url);
    long getLastSyncTimestamp();
    void testConnection(String testToken, Callback<String> callback);

    // ---- Data operations ----

    void syncRoster(Callback<Result> callback);
    Result loadCachedResult();
    Result createSampleFallback();
    void claimOpenShift(int shiftId, Callback<String> callback);
    void fetchDocuments(Callback<List<Document>> callback);
}
