#!/usr/bin/env bash
# Run the real OpenTTY MIDlet (src/) on the desktop using the pc/j2me
# J2ME bindings. Nothing in src/ is modified: a work copy under pc/work
# receives two mechanical, behavior-preserving patches that modern javac
# needs (CLDC has no java.util.List, so src treats "List" as the LCDUI
# List; and LCDUI types are imported with java.util.*):
#   1. add "import javax.microedition.lcdui.List;" (single-type import wins
#      over the wildcard imports) to OpenTTY.java and Lua.java;
#   2. rename a local "arg" shadowing the method-level "arg" in Lua.java.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
BUILD="$ROOT/pc/build"
WORK="$ROOT/pc/work"
DATA="$ROOT/data"

cd "$ROOT"

mkdir -p "$BUILD" "$DATA/rms" "$DATA/mnt"
rm -rf "$WORK/src"
mkdir -p "$WORK"
cp -r src "$WORK/src"

echo "[run.sh] patching work copy (src/ stays untouched)..."
python3 "$ROOT/pc/patch_src.py" "$WORK/src"

echo "[run.sh] compiling src/ + pc/j2me bindings..."
JAVA_FILES=$(find "$ROOT/pc/j2me" -name '*.java' | sort)
javac -nowarn -encoding UTF-8 -d "$BUILD" \
    "$WORK"/src/*.java \
    "$ROOT/pc/app"/*.java \
    $JAVA_FILES

echo "[run.sh] launching OpenTTY..."

JAVA=java
if [ -x "$ROOT/pc/java" ]; then
    JAVA="$ROOT/pc/java"
fi

exec "$JAVA" \
    ${OPENTTY_JVM_OPTS:-} \
    -Dopentty.rms="$DATA/rms" \
    -Dopentty.mnt="$DATA/mnt" \
    -Dopentty.user="${OPENTTY_USER:-opentty}" \
    -cp "$BUILD:$ROOT/src" \
    OpenTTYMain "$@"

exit 0