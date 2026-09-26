#!/usr/bin/env bash
# Build a standalone desktop JAR of OpenTTY: the unmodified src/ MIDlet,
# the pc/j2me bindings, the pc/app runner and the guest /bin /boot /etc /lib
# resources all packed into one runnable archive.
#
#   pc/build-jar.sh                      -> dist/OpenTTY-desktop-1.18.2.jar
#   java -jar dist/OpenTTY-desktop-1.18.2.jar [options] [--] [command tokens...]
#
# Options are identical to pc/run.sh (see docs/RUNNER.md). Per-boot state
# (RecordStores, mounts) lives under data/ by default.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
WORK="$ROOT/pc/work"
OUT="$ROOT/dist/OpenTTY-desktop-1.18.2.jar"

cd "$ROOT"

echo "[build-jar] patching work copy (src/ stays untouched)..."
rm -rf "$WORK/src"
mkdir -p "$WORK"
cp -r src "$WORK/src"
python3 "$ROOT/pc/patch_src.py" "$WORK/src"

STAGE="$WORK/jar-stage"
rm -rf "$STAGE"
mkdir -p "$STAGE/classes"

echo "[build-jar] compiling src/ + pc/j2me + pc/app..."
JAVA_FILES=$(find "$ROOT/pc/j2me" -name '*.java' | sort)
javac -nowarn -encoding UTF-8 -d "$STAGE/classes" \
    "$WORK"/src/*.java \
    "$ROOT/pc/app"/*.java \
    $JAVA_FILES

echo "[build-jar] packing resources (/bin /boot /etc /lib)..."
cp -r "$WORK/src"/bin "$STAGE/classes"/bin
cp -r "$WORK/src"/boot "$STAGE/classes"/boot
cp -r "$WORK/src"/etc "$STAGE/classes"/etc
cp -r "$WORK/src"/lib "$STAGE/classes"/lib

echo "[build-jar] writing manifest and jar..."
mkdir -p "$ROOT/dist"
printf 'Main-Class: OpenTTYMain\n' > "$STAGE/MANIFEST.MF"
jar cfm "$OUT" "$STAGE/MANIFEST.MF" -C "$STAGE/classes" .

rm -rf "$STAGE"
echo "[build-jar] wrote $OUT"
echo "[build-jar] run it with: java -jar $OUT -- /tmp/app.lua"