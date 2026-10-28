#!/bin/sh
# Lua runtime semantics on the real interpreter (src/Lua.java) through the
# desktop shim, headless. Guards the language behavior the apps rely on:
# numeric/generic for loops, break/return inside loops, `and`/`or`
# short-circuit, tonumber, list shrinking and multi-assignment.
#
# Runs one headless shim process (~30 s, it compiles src/ + pc/j2me first).
# Usage: tests/lua/run_lua.sh

set -u

HERE="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
ROOT="$(CDPATH= cd -- "$HERE/../.." && pwd)"
cd "$ROOT" || exit 1

command -v java >/dev/null 2>&1 || { echo "run_lua: no JDK on PATH (skipped)"; exit 0; }
command -v javac >/dev/null 2>&1 || { echo "run_lua: no javac on PATH (skipped)"; exit 0; }

RESULT="$ROOT/data/mnt/opentty/runtime-result.txt"
LOG="$ROOT/data/mnt/opentty/runtime-run.log"
mkdir -p "$ROOT/data/mnt/opentty"
rm -f "$RESULT" "$LOG"

OPENTTY_JVM_OPTS="-Dopentty.headless=1 -Dopentty.smokeWait=7000" \
    timeout 300 bash pc/run.sh --user opentty --pass opentty --smoke -- \
    "$HERE/check_runtime.lua" > "$LOG" 2>&1
status=$?

if [ ! -f "$RESULT" ]; then
    echo "run_lua: no result file (shim exit $status); see $LOG"
    exit 1
fi

cat "$RESULT"

if grep -q "^FAILURES=0$" "$RESULT" && ! grep -q "^FAIL " "$RESULT"; then
    echo "run_lua: OK"
    exit 0
fi

echo "run_lua: FAILED"
exit 1
