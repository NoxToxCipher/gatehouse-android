# Gatehouse, on a phone

The guard's screen. The record underneath it is the Ada core in
`C:\Users\lochr\shiftlog`, which this links against rather than reimplements.

## What is real and what is placeholder

Real: every entry goes through the same library that is proved and tested on
the desktop. The rules that refuse an entry are its rules, the sentence
explaining a refusal is its sentence, and the handover page on screen is
rendered by it rather than assembled in Java. The times are real minutes since
the epoch in local time, so the report carries today's date.

Placeholder: the site and the guard are hardcoded, a checkpoint tap is a
button rather than an NFC tag, and there is no storage, so closing the app
loses the night. None of that is hard to replace; all of it is deliberately
not done yet.

## Building

    bash tools/build.sh            build build/gatehouse.apk
    bash tools/build.sh install    build, then install and launch

No Gradle. Everything needed is already on the machine — aapt2, d8, apksigner,
a JDK and the NDK — and driving them directly means the build does not fail
because something upstream moved or a download did not happen.

Three things about this toolchain that cost an hour each and are not written
down anywhere obvious:

- **aapt2 on Windows splits a resource path on backslashes** to work out which
  resource directory a file belongs to. A forward-slash path comes back as
  `bad resource path` with nothing to say which part it disliked. Everything
  handed to aapt2 goes through `native()` first.
- **`MSYS_NO_PATHCONV=1` is for paths that must not be translated**, like
  `/data/local/tmp` on the phone. Putting it in front of a tool taking a
  Windows path gives `\c\Users\...`, which fails in a way that reads like a
  missing file.
- **Git Bash has no `zip`.** `tools/addfiles.py` puts the dex and the
  libraries into the APK, and is more careful about entry names than a command
  line tool would have been.

## The two libraries

`libgatehouse_core.so` is the record core exactly as the crate builds it.
`libgatehouse.so` is the JNI bridge, linked against it. Java loads them in
that order.

The bridge is deliberately thin: it converts a Java string into an address and
a length, calls one exported function, and hands back what came out. It
decides nothing. An app that explained the record's rules in its own words
would eventually explain them wrongly.

The build checks that both libraries actually export what the app calls. That
check exists because an earlier version scavenged the crate's intermediate
object files, which the crate's own build deletes, and produced a 28 KB core
instead of a 130 KB one. It linked, it signed, it installed, and it would have
failed on the first tap.

## Installing on a Xiaomi

`adb install` returns `INSTALL_FAILED_USER_RESTRICTED` until **Developer
options → Install via USB** is turned on. Until then, `tools/build.sh` still
produces the APK and it can be pushed and tapped:

    adb push build/gatehouse.apk /sdcard/Download/

Then open it from Files on the phone.
