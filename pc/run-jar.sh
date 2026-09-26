#!/usr/bin/env bash
# Launch the built desktop JAR. Non-reparenting WMs (bspwm/i3/dwm...) leave
# AWT windows blank grey without _JAVA_AWT_WM_NONREPARENTING=1.
set -euo pipefail
cd "$(dirname "$0")/.."
jar="dist/OpenTTY-desktop-1.18.2.jar"
if [ $# -gt 0 ] && [ -f "$1" ]; then jar="$1"; shift; fi
[ -f "$jar" ] || { echo "jar not found: $jar (run pc/build-jar.sh first)" >&2; exit 1; }
export _JAVA_AWT_WM_NONREPARENTING=1
exec java -jar "$jar" "$@"