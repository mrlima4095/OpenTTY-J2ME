#!/bin/bash
# Runs the RISC-V ELF emulator tests (src/ELF.java) on the JVM, using small
# J2ME stubs for the parts ELF.java references (Lua/OpenTTY/Process + LCDUI).
#
# Test sources: tests/java/src/TestELF.java, TestMem.java, TestCalNow.java,
# and TestSockets.java. Socket coverage uses the desktop Connector bindings.
# They load the shipped emulator binaries from res/apps/dist/.
#
# Usage: tests/java/run_java.sh [clean]

set -euo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "$HERE/../.." && pwd)"
OUT="$HERE/build"
STUBS="$HERE/stubs"

mkdir -p "$OUT"

# 1) guest libc fixture (memtest) straight from repo sources (skips cleanly when
#    the LLVM toolchain is missing).
"$HERE/build_rv.sh" || echo "note: memtest fixture not rebuilt (see build_rv.sh)"

# 2) J2ME stubs + the real emulator + the test harness -> one class dir.
#    -nowarn silences the CLDC-era `new Integer()` deprecation notices on new
#    JDKs; they are intentional in the J2ME source.
javac -nowarn -encoding UTF-8 -d "$OUT" \
    "$ROOT/src/ELF.java" \
    $(find "$STUBS" -path "$STUBS/javax/microedition/io" -prune -o -name '*.java' -print | sort) \
    $(find "$ROOT/pc/j2me/javax/microedition/io" -name '*.java' | sort) \
    "$HERE/src/TestELF.java" \
    "$HERE/src/TestMem.java" \
    "$HERE/src/TestCalNow.java" \
    "$HERE/src/TestSockets.java"

REPOPROP="-Dopentty.repo=$ROOT"

run() {
    echo "== $1 =="
    java $REPOPROP -cp "$OUT" "$1"
}

fails=0
run TestELF   || fails=$((fails + 1))
if [ -f "$HERE/rv/memtest" ]; then run TestMem || fails=$((fails + 1)); else echo "== TestMem =="; echo "SKIP memtest fixture unavailable"; fi
run TestCalNow || fails=$((fails + 1))
run TestSockets || fails=$((fails + 1))

echo
if [ "$fails" -eq 0 ]; then
    echo "run_java: ALL ELF EMULATOR TESTS PASSED"
else
    echo "run_java: $fails ELF EMULATOR SUITE(S) FAILED"
    exit 1
fi
