/* The bridge between the record core and a screen.
 *
 * The core is Ada with a plain C surface and no runtime: it takes buffers and
 * returns status codes, and knows nothing about Android. This file is the only
 * thing between it and Java, and it is deliberately thin. Everything it does
 * is convert a Java string into an address and a length, call one exported
 * function, and hand back what came out.
 *
 * Nothing here decides anything. If a rule refuses an entry, the refusal and
 * the sentence explaining it both come from the core, so the screen cannot
 * accidentally allow something the record forbids by being written wrongly.
 */

#include <jni.h>
#include <string.h>
#include <stdlib.h>
#include "gatehouse.h"

#define NS(name) Java_au_com_dss_gatehouse_Core_##name

static jstring take(JNIEnv *env, const char *buf, int n) {
  if (n <= 0) return (*env)->NewStringUTF(env, "");
  char *tmp = (char *) malloc((size_t) n + 1);
  if (!tmp) return (*env)->NewStringUTF(env, "");
  memcpy(tmp, buf, (size_t) n);
  tmp[n] = 0;
  jstring s = (*env)->NewStringUTF(env, tmp);
  free(tmp);
  return s;
}

JNIEXPORT jint JNICALL NS(encodingVersion)(JNIEnv *e, jclass c) {
  (void) e; (void) c; return gatehouse_encoding_version();
}

JNIEXPORT jint JNICALL NS(archiveVersion)(JNIEnv *e, jclass c) {
  (void) e; (void) c; return gatehouse_archive_version();
}

JNIEXPORT jint JNICALL NS(siteBegin)(JNIEnv *e, jclass c, jstring name) {
  (void) c;
  const char *n = (*e)->GetStringUTFChars(e, name, 0);
  int r = gatehouse_site_begin(n, (int) strlen(n));
  (*e)->ReleaseStringUTFChars(e, name, n);
  return r;
}

JNIEXPORT jint JNICALL NS(siteAddPoint)(JNIEnv *e, jclass c,
                                        jstring label, jstring uid) {
  (void) c;
  const char *l = (*e)->GetStringUTFChars(e, label, 0);
  const char *u = (*e)->GetStringUTFChars(e, uid, 0);
  int r = gatehouse_site_add_point(l, (int) strlen(l), u, (int) strlen(u), "", 0);
  (*e)->ReleaseStringUTFChars(e, label, l);
  (*e)->ReleaseStringUTFChars(e, uid, u);
  return r;
}

JNIEXPORT jint JNICALL NS(siteAddOption)(JNIEnv *e, jclass c,
                                        jstring label, jstring text) {
  (void) c;
  const char *l = (*e)->GetStringUTFChars(e, label, 0);
  const char *t = (*e)->GetStringUTFChars(e, text, 0);
  int r = gatehouse_site_add_option(l, (int) strlen(l), t, (int) strlen(t));
  (*e)->ReleaseStringUTFChars(e, label, l);
  (*e)->ReleaseStringUTFChars(e, text, t);
  return r;
}

JNIEXPORT jint JNICALL NS(sitePolicy)(JNIEnv *e, jclass c,
                                      jint every, jint gap, jint rounds) {
  (void) e; (void) c;
  return gatehouse_site_policy(every, gap, rounds);
}

JNIEXPORT jstring JNICALL NS(siteHash)(JNIEnv *e, jclass c) {
  (void) c;
  char hex[65];
  if (gatehouse_site_hash(hex, 64) != 0) return (*e)->NewStringUTF(e, "");
  return take(e, hex, 64);
}

JNIEXPORT jstring JNICALL NS(genesis)(JNIEnv *e, jclass c) {
  (void) c;
  char z[65];
  memset(z, '0', 64);
  return take(e, z, 64);
}

JNIEXPORT jint JNICALL NS(setAttribution)(JNIEnv *e, jclass c,
                                          jint device, jint method) {
  (void) e; (void) c;
  return gatehouse_set_attribution(device, method);
}

JNIEXPORT jint JNICALL NS(setGuard)(JNIEnv *e, jclass c, jstring id,
                                    jstring name, jstring licence,
                                    jstring source, jstring ext) {
  (void) c;
  const char *a = (*e)->GetStringUTFChars(e, id, 0);
  const char *b = (*e)->GetStringUTFChars(e, name, 0);
  const char *d = (*e)->GetStringUTFChars(e, licence, 0);
  const char *f = (*e)->GetStringUTFChars(e, source, 0);
  const char *g = (*e)->GetStringUTFChars(e, ext, 0);
  int r = gatehouse_set_guard(a, (int) strlen(a), b, (int) strlen(b),
                              d, (int) strlen(d), f, (int) strlen(f),
                              g, (int) strlen(g));
  (*e)->ReleaseStringUTFChars(e, id, a);
  (*e)->ReleaseStringUTFChars(e, name, b);
  (*e)->ReleaseStringUTFChars(e, licence, d);
  (*e)->ReleaseStringUTFChars(e, source, f);
  (*e)->ReleaseStringUTFChars(e, ext, g);
  return r;
}

JNIEXPORT jint JNICALL NS(openShift)(JNIEnv *e, jclass c, jstring prior,
                                     jstring policy, jint occurred,
                                     jint recorded, jstring text) {
  (void) c;
  const char *p = (*e)->GetStringUTFChars(e, prior, 0);
  const char *q = (*e)->GetStringUTFChars(e, policy, 0);
  const char *t = (*e)->GetStringUTFChars(e, text, 0);
  int r = gatehouse_open(p, (int) strlen(p), q, (int) strlen(q),
                         occurred, recorded, t, (int) strlen(t));
  (*e)->ReleaseStringUTFChars(e, prior, p);
  (*e)->ReleaseStringUTFChars(e, policy, q);
  (*e)->ReleaseStringUTFChars(e, text, t);
  return r;
}

JNIEXPORT jint JNICALL NS(addCheckpoint)(JNIEnv *e, jclass c, jint occurred,
                                         jint recorded, jstring label,
                                         jstring uid, jint taps, jint auth) {
  (void) c;
  const char *l = (*e)->GetStringUTFChars(e, label, 0);
  const char *u = (*e)->GetStringUTFChars(e, uid, 0);
  /* Rule 14: a tap claiming a signed tag has to bring back what the tag
     produced. There is no NFC here yet, so this is a stand-in of the right
     shape rather than a real one, and it is marked as such on the screen. */
  static const char stub[64] =
    "1111111111111111111111111111111111111111111111111111111111111111";
  int signed_tap = (auth == GATEHOUSE_AUTH_CRYPTOGRAPHIC);
  int r = gatehouse_add_checkpoint(occurred, recorded, l, (int) strlen(l),
                                   u, (int) strlen(u), taps, auth,
                                   signed_tap ? stub : 0,
                                   signed_tap ? 64 : 0);
  (*e)->ReleaseStringUTFChars(e, label, l);
  (*e)->ReleaseStringUTFChars(e, uid, u);
  return r;
}

JNIEXPORT jint JNICALL NS(addNote)(JNIEnv *e, jclass c, jint kind, jint topic,
                                   jint occurred, jint recorded, jstring text,
                                   jint refers) {
  (void) c;
  const char *t = (*e)->GetStringUTFChars(e, text, 0);
  int r = gatehouse_add_note(kind, topic, occurred, recorded,
                             t, (int) strlen(t), refers, 0, 0);
  (*e)->ReleaseStringUTFChars(e, text, t);
  return r;
}

JNIEXPORT jint JNICALL NS(seal)(JNIEnv *e, jclass c, jint occurred,
                                jint recorded, jstring text) {
  (void) c;
  const char *t = (*e)->GetStringUTFChars(e, text, 0);
  int r = gatehouse_seal(occurred, recorded, t, (int) strlen(t));
  (*e)->ReleaseStringUTFChars(e, text, t);
  return r;
}

JNIEXPORT jstring JNICALL NS(entryLine)(JNIEnv *e, jclass c, jint i) {
  (void) c;
  char line[512];
  int n = gatehouse_entry_line(i, line, (int) sizeof line);
  if (n <= 0) return (*e)->NewStringUTF(e, "");
  return take(e, line, n);
}

JNIEXPORT jint JNICALL NS(pointVisits)(JNIEnv *e, jclass c, jstring label) {
  (void) c;
  const char *l = (*e)->GetStringUTFChars(e, label, 0);
  int r = gatehouse_point_visits(l, (int) strlen(l));
  (*e)->ReleaseStringUTFChars(e, label, l);
  return r;
}

JNIEXPORT jint JNICALL NS(pointLast)(JNIEnv *e, jclass c, jstring label) {
  (void) c;
  const char *l = (*e)->GetStringUTFChars(e, label, 0);
  int r = gatehouse_point_last(l, (int) strlen(l));
  (*e)->ReleaseStringUTFChars(e, label, l);
  return r;
}

JNIEXPORT jint JNICALL NS(lastRecorded)(JNIEnv *e, jclass c) {
  (void) e; (void) c; return gatehouse_last_recorded();
}

JNIEXPORT jint JNICALL NS(entryCount)(JNIEnv *e, jclass c) {
  (void) e; (void) c; return gatehouse_entry_count();
}

JNIEXPORT jint JNICALL NS(isSealed)(JNIEnv *e, jclass c) {
  (void) e; (void) c; return gatehouse_is_sealed();
}

JNIEXPORT jint JNICALL NS(verified)(JNIEnv *e, jclass c) {
  (void) e; (void) c; return gatehouse_verified();
}

JNIEXPORT jstring JNICALL NS(head)(JNIEnv *e, jclass c) {
  (void) c;
  char hex[65];
  if (gatehouse_head(hex, 64) != 0) return (*e)->NewStringUTF(e, "");
  return take(e, hex, 64);
}

/* Why the last call refused, in the core's own words. The screen never writes
   one of these itself: an app that explains the rules in its own language is
   an app that will one day explain them wrongly. */
JNIEXPORT jstring JNICALL NS(lastReason)(JNIEnv *e, jclass c) {
  (void) c;
  char why[256];
  int n = gatehouse_last_reason(why, (int) sizeof why);
  if (n < 0) return (*e)->NewStringUTF(e, "");
  return take(e, why, n);
}

JNIEXPORT jint JNICALL NS(kept)(JNIEnv *e, jclass c) {
  (void) e; (void) c; return gatehouse_kept();
}

JNIEXPORT jint JNICALL NS(isSaved)(JNIEnv *e, jclass c) {
  (void) e; (void) c; return gatehouse_is_saved();
}

/* Carrying on past a sealed record. The core refuses unless what is held has
   been written out, which is why the screen archives first: a night nobody
   kept is a night thrown away, and the library will not do that quietly. */
JNIEXPORT jint JNICALL NS(continueShift)(JNIEnv *e, jclass c, jint occurred,
                                         jint recorded, jstring text) {
  (void) c;
  const char *t = (*e)->GetStringUTFChars(e, text, 0);
  int r = gatehouse_continue(occurred, recorded, t, (int) strlen(t));
  (*e)->ReleaseStringUTFChars(e, text, t);
  return r;
}

JNIEXPORT jstring JNICALL NS(report)(JNIEnv *e, jclass c,
                                     jint opens, jint closes) {
  (void) c;
  /* A full night's report: 1000 lines of up to 100 characters, plus
     room. Sized for the busy night rather than the quiet one, because
     the buffer that is big enough for a demo is the one that truncates
     the record somebody is relying on. */
  static char page[131072];
  int n = gatehouse_report(opens, closes, page, (int) sizeof page);
  if (n < 0) return (*e)->NewStringUTF(e, "");
  return take(e, page, n);
}

JNIEXPORT jstring JNICALL NS(archive)(JNIEnv *e, jclass c) {
  (void) c;
  /* A full record is 512 entries at up to 900 characters a line, so an
     archive of one is around half a megabyte. This was 128 KB, which is
     ample for a quiet night and silently short on a busy one: the
     archive would refuse to render, the record would not be kept, and
     the app would decline to continue. */
  static char buf[524288];
  int n = gatehouse_archive(buf, (int) sizeof buf);
  if (n < 0) return (*e)->NewStringUTF(e, "");
  return take(e, buf, n);
}
