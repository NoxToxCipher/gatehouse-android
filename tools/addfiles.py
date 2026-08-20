"""Adds the dex and the native library into the APK aapt2 produced.

Git Bash has no zip, and an APK is a zip. Python's zipfile is here anyway and
is more precise about entry names than a command line tool would be: an entry
called lib\\arm64-v8a\\libgatehouse.so with backslashes is not the entry
Android looks for, and nothing would say so beyond the app failing to find its
own library at run time.

    python tools/addfiles.py <apk> <local-path>=<entry-name> ...
"""

import sys
import zipfile

apk = sys.argv[1]
with zipfile.ZipFile(apk, "a", zipfile.ZIP_DEFLATED) as z:
    for pair in sys.argv[2:]:
        local, entry = pair.split("=", 1)
        z.write(local, entry)
        print("  added %s" % entry)
