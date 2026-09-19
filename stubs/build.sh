#!/bin/sh
# Build the desktop stubs + the real J2ME MIDlet source, then run it.
set -e
cd "$(dirname "$0")/.."

OUT=$(dirname "$0")/.desktop-out
rm -rf "$OUT"
mkdir -p "$OUT"

find stubs -name '*.java' > /tmp/opentty-srcs.txt

javac -encoding UTF-8 -nowarn \
    -d "$OUT" @/tmp/opentty-srcs.txt \
    src/OpenTTY.java src/Lua.java src/ELF.java

echo "=== Running OpenTTY desktop ==="
java -cp "$OUT:src" Run "$@"