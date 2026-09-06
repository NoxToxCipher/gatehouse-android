package au.com.dss.gatehouse;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import java.nio.charset.StandardCharsets;

/**
 * Installs a site book sent over adb, so the owner can put one on a hut phone
 * from a laptop without touching the screen:
 *
 * <pre>
 * adb shell am broadcast -n au.com.dss.gatehouse/.SiteBookReceiver \
 *     -a au.com.dss.gatehouse.SITEBOOK --es b64 "$(base64 -w0 sitebook.json)"
 * </pre>
 *
 * The manifest guards it with WRITE_SECURE_SETTINGS, which only the shell and
 * the system hold, so no other app on the phone can hand the app a book.
 */
public final class SiteBookReceiver extends BroadcastReceiver {

    static final String ACTION = "au.com.dss.gatehouse.SITEBOOK";
    static final String EXTRA_B64 = "b64";
    static final String EXTRA_REMOVE = "remove";
    /** Forget every guard's theme pick, so the next sign-in asks again (test clean-up). */
    static final String EXTRA_FORGET_THEMES = "forget_themes";
    /** Wipe the complaint book (test clean-up). */
    static final String EXTRA_CLEAR_COMPLAINTS = "clear_complaints";

    @Override
    public void onReceive(Context ctx, Intent intent) {
        if (intent == null || !ACTION.equals(intent.getAction())) return;
        try {
            if (intent.getBooleanExtra(EXTRA_CLEAR_COMPLAINTS, false)) {
                ComplaintStore.clear(ctx);
                say(ctx, "Complaints cleared");
                return;
            }
            if (intent.getBooleanExtra(EXTRA_FORGET_THEMES, false)) {
                GuardSession.forgetThemes(ctx);
                say(ctx, "Theme picks forgotten");
                return;
            }
            if (intent.getBooleanExtra(EXTRA_REMOVE, false)) {
                SiteBook.remove(ctx);
                GuardSession.signOut(ctx);
                say(ctx, "Site book removed");
                return;
            }
            String b64 = intent.getStringExtra(EXTRA_B64);
            if (b64 == null || b64.isEmpty()) { say(ctx, "Site book: nothing to install"); return; }
            String json = new String(Base64.decode(b64, Base64.DEFAULT), StandardCharsets.UTF_8);
            String summary = SiteBook.install(ctx, json);
            say(ctx, "Site book installed · " + summary);
        } catch (IllegalArgumentException e) {
            say(ctx, "Site book refused · " + e.getMessage());
        } catch (Throwable t) {
            say(ctx, "Site book failed · " + t);
        }
    }

    private static void say(Context ctx, String line) {
        Log.i("SiteBook", line);
        try { Toast.makeText(ctx, line, Toast.LENGTH_LONG).show(); } catch (Throwable ignored) {}
    }
}
