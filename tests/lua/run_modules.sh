#!/bin/sh
# Rock payload checks on the real interpreter (src/Lua.java) through the
# desktop shim, headless. Stages apps/1.19/lua/*.lua under /mnt/opentty/rocks
# and loads each payload exactly like `require` does on-device: math, string,
# graphics, kernel and httpd must return their table, keep the native
# functions they extend, and expose the helpers the apps call. The cterm
# Canvas app is staged as cterm-app.lua and loaded in module mode, so the
# terminal engine and one real canvas frame run in the same shim process.
#
# Runs one headless shim process (~30 s, it compiles src/ + pc/j2me first).
# Usage: tests/lua/run_modules.sh

set -u

HERE="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
ROOT="$(CDPATH= cd -- "$HERE/../.." && pwd)"
cd "$ROOT" || exit 1

command -v java >/dev/null 2>&1 || { echo "run_modules: no JDK on PATH (skipped)"; exit 0; }
command -v javac >/dev/null 2>&1 || { echo "run_modules: no javac on PATH (skipped)"; exit 0; }

STAGE="$ROOT/data/mnt/opentty/rocks"
RESULT="$ROOT/data/mnt/opentty/modules-result.txt"
LOG="$ROOT/data/mnt/opentty/modules-run.log"
mkdir -p "$STAGE"
rm -f "$RESULT" "$LOG"

# The shim maps /mnt/opentty to data/mnt/opentty, so staging the payloads here
# gives the script a real /mnt/opentty/rocks/<rock>.lua path.
for f in "$ROOT"/apps/1.19/lua/*.lua; do
    [ -f "$f" ] || continue
    cp "$f" "$STAGE/" || exit 1
done
cp "$ROOT/apps/1.19/sys/cterm" "$STAGE/cterm-app.lua" || exit 1

OPENTTY_JVM_OPTS="-Dopentty.headless=1 -Dopentty.smokeWait=7000" \
    timeout 300 bash pc/run.sh --user opentty --pass opentty --smoke -- \
    "$HERE/check_modules.lua" > "$LOG" 2>&1
status=$?

if [ ! -f "$RESULT" ]; then
    echo "run_modules: no result file (shim exit $status); see $LOG"
    exit 1
fi

cat "$RESULT"

if grep -q "^FAILURES=0$" "$RESULT" && ! grep -q "^FAIL " "$RESULT"; then
    echo "run_modules: OK"
    exit 0
fi

echo "run_modules: FAILED"
exit 1
