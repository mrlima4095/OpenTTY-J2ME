#!/bin/sh
# Build and run OpenTTY on the desktop (stubs + real J2ME source).
set -e
cd "$(dirname "$0")"

# A headless-only JDK cannot open any window; detect it early and point
# the user at the fix instead of showing a silent blank screen.
PROBE=/tmp/opentty-headless-probe
mkdir -p "$PROBE"
printf 'public class H { public static void main(String[] a) { System.out.println(java.awt.GraphicsEnvironment.isHeadless() ? "HEADLESS" : "OK"); } }\n' > "$PROBE/H.java"
javac -d "$PROBE" "$PROBE/H.java" 2>/dev/null
if [ "$(java -cp "$PROBE" H 2>/dev/null)" = "HEADLESS" ]; then
    echo "ERROR: your Java installation is headless-only (no GUI support)." >&2
    echo "       Install a headful JDK/JRE, e.g.:  sudo apt install openjdk-21-jre" >&2
    exit 1
fi

sh stubs/build.sh "$@"