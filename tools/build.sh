#!/usr/bin/env bash
#
# Builds the APK without Gradle.
#
# Gradle would want to fetch the Android Gradle Plugin, and everything needed
# is already on this machine: aapt2, d8, apksigner, a JDK and the NDK. So this
# drives them directly. Longer to write once, and it does not break because
# something upstream moved.
#
#   bash tools/build.sh          build
#   bash tools/build.sh install  build, then install and launch on the phone

set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

SDK="${ANDROID_SDK:-/c/Users/lochr/android-sdk}"
BT="$SDK/build-tools/35.0.0"
JAR="$SDK/platforms/android-35/android.jar"
NDK="$SDK/ndk/27.2.12479018"
JDK="$SDK/jdk-17.0.20+8"
ADB="$SDK/platform-tools/adb.exe"
CORE="${GATEHOUSE_CORE:-/c/Users/lochr/shiftlog}"
API=26

CLANG="$NDK/toolchains/llvm/prebuilt/windows-x86_64/bin/aarch64-linux-android${API}-clang"

OUT="$ROOT/build"
rm -rf "$OUT"
mkdir -p "$OUT/res" "$OUT/classes" "$OUT/lib/arm64-v8a"

# aapt2 on Windows works out which resource directory a file is in by
# splitting its path on backslashes. Hand it a forward-slash path and every
# file comes back "bad resource path", with nothing to say which part it
# disliked. So anything going to aapt2 is converted to a native path first.
BS='\'
native() { echo "$1" | sed "s|/|${BS}${BS}|g"; }
W=$(native "$(pwd -W)")
JARW=$(native "$(echo "$JAR" | sed 's|^/c|C:|')")

say() { printf '  %-34s' "$1"; }
ok()  { echo "ok"; }

# ---- the record core, and the bridge to it --------------------------------
#
# Two libraries: the record core exactly as the crate builds it, and a thin
# bridge linked against it. Java loads them in that order.
#
# Built here rather than reused, and linked against the library the crate is
# meant to produce rather than the object files it happens to leave behind.
# Scavenging those objects silently produced a 28 KB library instead of a
# 130 KB one, because the crate's own build removes them, and an APK carrying
# a core with most of it missing installs and launches exactly like a good
# one.
say "record core"
( cd "$CORE" && bash tools/build_android.sh > "$OUT/core.log" 2>&1 ) \
  || { echo "FAILED"; tail -20 "$OUT/core.log"; exit 1; }
CORE_SO="$CORE/obj/android/libgatehouse_core.so"
[ -f "$CORE_SO" ] || { echo "FAILED: no $CORE_SO"; exit 1; }
cp "$CORE_SO" "$OUT/lib/arm64-v8a/"
ok

say "bridge"
"$CLANG" -c -O1 -fPIC -I "$CORE/include" \
  -o "$OUT/gatehouse_jni.o" app/jni/gatehouse_jni.c
ok

say "libgatehouse.so"
"$CLANG" -shared -o "$OUT/lib/arm64-v8a/libgatehouse.so" \
  "$OUT/gatehouse_jni.o" \
  -L "$OUT/lib/arm64-v8a" -lgatehouse_core
ok

# What the app calls has to be in what the app ships. This is the check that
# would have caught the scavenged build: it linked, it installed, and it was
# missing nearly everything.
say "the library holds the core"
NM="$NDK/toolchains/llvm/prebuilt/windows-x86_64/bin/llvm-nm.exe"
for sym in gatehouse_report gatehouse_seal gatehouse_add_checkpoint \
           gatehouse_last_reason gatehouse_site_hash; do
  "$NM" --defined-only "$OUT/lib/arm64-v8a/libgatehouse_core.so" \
    | grep -q " $sym\$" || { echo "FAILED: $sym is missing"; exit 1; }
done
for sym in Java_au_com_dss_gatehouse_Core_report \
           Java_au_com_dss_gatehouse_Core_addCheckpoint; do
  "$NM" --defined-only "$OUT/lib/arm64-v8a/libgatehouse.so" \
    | grep -q " $sym\$" || { echo "FAILED: $sym is missing"; exit 1; }
done
ok

# ---- resources ------------------------------------------------------------
say "resources"
for f in app/res/*/*.xml; do
  MSYS_NO_PATHCONV=1 "$BT/aapt2.exe" compile \
    -o "${W}${BS}build${BS}res" "${W}${BS}$(native "$f")"
done
ok

say "manifest and resource table"
MSYS_NO_PATHCONV=1 "$BT/aapt2.exe" link \
  -o "${W}${BS}build${BS}base.apk" \
  -I "$JARW" \
  --manifest "${W}${BS}app${BS}AndroidManifest.xml" \
  --min-sdk-version 26 --target-sdk-version 35 \
  $(for fl in "$OUT"/res/*.flat; do printf "%s " "${W}${BS}build${BS}res${BS}$(basename "$fl")"; done)
ok

# ---- code -----------------------------------------------------------------
say "java"
if ! "$JDK/bin/javac.exe" -nowarn --release 11 -encoding UTF-8 -classpath "$JAR" \
     -d "$OUT/classes" app/java/au/com/dss/gatehouse/*.java 2> "$OUT/javac.log"
then
  echo "FAILED"; cat "$OUT/javac.log"; exit 1
fi
ok

say "dex"
"$JDK/bin/jar.exe" cf "$OUT/classes.jar" -C "$OUT/classes" .
if ! "$BT/d8.bat" --min-api 26 --output "$OUT" \
     "$OUT/classes.jar" > "$OUT/d8.log" 2>&1
then
  echo "FAILED"; cat "$OUT/d8.log"; exit 1
fi
[ -f "$OUT/classes.dex" ] || { echo "FAILED: no classes.dex"; exit 1; }
ok

# ---- assemble -------------------------------------------------------------
say "assemble"
cp "$OUT/base.apk" "$OUT/unsigned.apk"
python tools/addfiles.py "$OUT/unsigned.apk" \
  "$OUT/classes.dex=classes.dex" \
  "$OUT/lib/arm64-v8a/libgatehouse_core.so=lib/arm64-v8a/libgatehouse_core.so" \
  "$OUT/lib/arm64-v8a/libgatehouse.so=lib/arm64-v8a/libgatehouse.so" \
  > "$OUT/zip.log" 2>&1 || { echo "FAILED"; cat "$OUT/zip.log"; exit 1; }
ok

say "align"
"$BT/zipalign.exe" -p -f 4 "$OUT/unsigned.apk" "$OUT/aligned.apk"
ok

# ---- sign -----------------------------------------------------------------
#
# A debug key, made once and kept beside the build. This is a development
# build and says so; a release would be signed with a key that does not live
# in the working tree.
KEY="$ROOT/debug.keystore"
if [ ! -f "$KEY" ]; then
  say "debug key"
  "$JDK/bin/keytool.exe" -genkeypair -v \
    -keystore "$KEY" -storepass android -keypass android \
    -alias androiddebugkey -keyalg RSA -keysize 2048 -validity 10000 \
    -dname "CN=Gatehouse Debug, O=Doherty Security Services, C=AU" \
    >/dev/null 2>&1
  ok
fi

say "sign"
if ! "$BT/apksigner.bat" sign \
     --ks "$KEY" --ks-pass pass:android --key-pass pass:android \
     --out "$OUT/gatehouse.apk" "$OUT/aligned.apk" > "$OUT/sign.log" 2>&1
then
  echo "FAILED"; cat "$OUT/sign.log"; exit 1
fi
ok

echo
echo "built build/gatehouse.apk"
ls -la "$OUT/gatehouse.apk" | awk '{print "  " $5 " bytes"}'

if [ "${1:-}" = "install" ]; then
  echo
  DEVICE=$(MSYS_NO_PATHCONV=1 "$ADB" devices | awk '/device$/ {print $1}' \
             | grep -v emulator | head -1)
  [ -n "$DEVICE" ] || { echo "no phone attached (an emulator cannot run arm64)"; exit 1; }
  echo "installing on $DEVICE"
  "$ADB" -s "$DEVICE" install -r "$OUT/gatehouse.apk"
  MSYS_NO_PATHCONV=1 "$ADB" -s "$DEVICE" shell monkey -p au.com.dss.gatehouse \
    -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1
  echo "launched"
fi
