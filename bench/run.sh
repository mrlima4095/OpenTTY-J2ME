#!/usr/bin/env bash
# Build and run the two benchmark twins (bench/bench.lua and bench/bench.c)
# on the desktop shim, headless, and print a Lua-vs-C comparison table.
#
#   bench/run.sh [scale] [passes]
#
# scale  multiplies the loop counts (default 4)
# passes repeats the suite in each run; the fastest pass per test is used
#        (microbenchmarks on a JVM are noisy, so best-of-N is the honest
#        number to quote)
#
# Both reports are also written to data/mnt/opentty/bench-lua.txt and
# data/mnt/opentty/bench-c.txt.

set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

SCALE="${1:-4}"
PASSES="${2:-3}"

LUA_REPORT="$ROOT/data/mnt/opentty/bench-lua.txt"
C_REPORT="$ROOT/data/mnt/opentty/bench-c.txt"
LOGDIR="$ROOT/data/mnt/opentty"

echo "[bench] checking the Lua source"
lua -e "assert(loadfile('$ROOT/bench/bench.lua'))" 2>/dev/null || true

echo "[bench] building bench/bench (RISC-V)"
./build-elf.sh bench/bench.c -stdlib -o bench/bench

rm -f "$LUA_REPORT" "$C_REPORT"

echo "[bench] running the Lua suite (scale=$SCALE passes=$PASSES)"
OPENTTY_JVM_OPTS="-Dopentty.headless=1" timeout 600 bash pc/run.sh \
    --user opentty --pass opentty -- "$ROOT/bench/bench.lua" "$SCALE" "$PASSES" \
    > "$LOGDIR/bench-lua.log" 2>&1 || true

echo "[bench] running the C suite (scale=$SCALE passes=$PASSES)"
OPENTTY_JVM_OPTS="-Dopentty.headless=1" timeout 600 bash pc/run.sh \
    --user opentty --pass opentty -- "$ROOT/bench/bench" "$SCALE" "$PASSES" \
    > "$LOGDIR/bench-c.log" 2>&1 || true

[ -f "$LUA_REPORT" ] || { echo "[bench] missing $LUA_REPORT (see $LOGDIR/bench-lua.log)" >&2; exit 1; }
[ -f "$C_REPORT" ] || { echo "[bench] missing $C_REPORT (see $LOGDIR/bench-c.log)" >&2; exit 1; }

report() {
    echo
    echo "== $1 =="
    cat "$2"
}

report "lua" "$LUA_REPORT"
report "c/rv32" "$C_REPORT"

# best (fastest) time per test and the C speedup, plus the checksums side by side
echo
printf '%-10s %8s %8s %8s   %s\n' test "c ms" "lua ms" "speedup" checksum
awk '
    FNR == 1 { file++; }
    /^[a-z_]+ n=/ {
        name = $1
        for (i = 1; i <= NF; i++) {
            split($i, kv, "=")
            if (kv[1] == "ms") ms = kv[2] + 0
            if (kv[1] == "checksum") ck = kv[2]
        }
        key = file ":" name
        if (!(key in best) || ms < best[key]) best[key] = ms
        checksum[name] = ck
    }
    END {
        split("sum_loop calls fib sieve sort string array alloc", order, " ")
        tc = 0; tl = 0; bad = 0
        for (i = 1; i <= 8; i++) {
            name = order[i]
            c = best["1:" name]; l = best["2:" name]
            if (c == "" || l == "") continue
            tc += c; tl += l
            if (l > 0) printf "%-10s %8d %8d %7.1fx   %s\n", name, c, l, l / c, checksum[name]
            else printf "%-10s %8d %8d %8s   %s\n", name, c, l, "-", checksum[name]
        }
        printf "%-10s %8d %8d %7.1fx\n", "total", tc, tl, (tc > 0 ? tl / tc : 0)
    }
' "$C_REPORT" "$LUA_REPORT" 2>/dev/null || true

echo
echo "[bench] best of $PASSES pass(es), scale=$SCALE, desktop shim (JVM)"
echo "[bench] raw reports: $C_REPORT, $LUA_REPORT"
