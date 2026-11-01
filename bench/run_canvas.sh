#!/usr/bin/env bash
# Run the Lua Canvas benchmark (bench/canvas.lua) on the desktop shim,
# headless, and print the best pass per test.
#
#   bench/run_canvas.sh [scale] [passes]
#
# scale  multiplies the starting batch sizes (default 1); every test grows its
#        own batch until it has run for TARGET_MS, so scale mostly changes how
#        much warm-up each pass gets
# passes repeats the suite in the same process; the fastest pass per test is
#        the number to quote (a JVM is noisy while it warms up)
#
# The raw report lands in data/mnt/opentty/bench-canvas.txt and the console log
# in data/mnt/opentty/bench-canvas.log.
#
# The shim keeps the JVM alive after a scripted command finishes, so the runner
# polls the report for the "done" marker and stops it as soon as the suite is
# over instead of waiting for a timeout.

set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

SCALE="${1:-1}"
PASSES="${2:-4}"

REPORT="$ROOT/data/mnt/opentty/bench-canvas.txt"
LOG="$ROOT/data/mnt/opentty/bench-canvas.log"
mkdir -p "$ROOT/data/mnt/opentty"
rm -f "$REPORT" "$LOG"

if command -v lua >/dev/null 2>&1; then
    echo "[canvas] checking bench/canvas.lua parses"
    lua -e "assert(loadfile('$ROOT/bench/canvas.lua'))"
fi

echo "[canvas] running the suite (scale=$SCALE passes=$PASSES, headless)"
OPENTTY_JVM_OPTS="-Dopentty.headless=1" bash pc/run.sh \
    --user opentty --pass opentty -- \
    "$ROOT/bench/canvas.lua" "$SCALE" "$PASSES" > "$LOG" 2>&1 &
pid=$!

for _ in $(seq 1 240); do
    if [ -f "$REPORT" ] && grep -q '^done ' "$REPORT" 2>/dev/null; then break; fi
    kill -0 "$pid" 2>/dev/null || break
    sleep 1
done
kill "$pid" 2>/dev/null || true
wait "$pid" 2>/dev/null || true

if [ ! -f "$REPORT" ]; then
    echo "[canvas] no report; see $LOG" >&2
    tail -n 20 "$LOG" >&2 || true
    exit 1
fi

if ! grep -q '^done ' "$REPORT"; then
    echo "[canvas] the suite did not finish (report is partial); see $LOG" >&2
fi

echo
echo "== canvas bench (lua) =="
grep -v '^done ' "$REPORT"

# Best (fastest) pass per test, plus the derived cost per call. The batches
# have different sizes per pass, so the pass is picked on us/call, not on ms.
echo
printf '%-12s %8s %8s %10s %10s %10s\n' test n "ms" "us/call" "fps" checksum
awk '
    /^[a-z_]+ n=/ {
        name = $1
        n = 0; ms = 0; us = 0; ck = ""; fps = "-"
        for (i = 1; i <= NF; i++) {
            split($i, kv, "=")
            if (kv[1] == "n") n = kv[2] + 0
            if (kv[1] == "ms") ms = kv[2] + 0
            if (kv[1] == "us") us = kv[2] + 0
            if (kv[1] == "fps") fps = kv[2]
            if (kv[1] == "checksum") ck = kv[2]
        }
        if (!(name in best) || us < best[name]) {
            best[name] = us; bn[name] = n; bms[name] = ms; bfps[name] = fps; bck[name] = ck
        }
    }
    END {
        split("native_call color clear clear_rect pixel line rect circle polygon text print metrics state clip keys new frame", order, " ")
        for (i = 1; i <= 17; i++) {
            name = order[i]
            if (!(name in best)) continue
            printf "%-12s %8d %8d %10.2f %10s %10s\n", name, bn[name], bms[name], best[name], bfps[name], bck[name]
        }
    }
' "$REPORT"

echo
echo "[canvas] best of $PASSES pass(es), scale=$SCALE, desktop shim (JVM, headless)"
echo "[canvas] raw report: $REPORT"
