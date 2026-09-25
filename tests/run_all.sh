#!/bin/sh
# OpenTTY 1.18.2 test suite - run everything and summarize.
#
# Suite                     What it guards
# ------------------------- ----------------------------------------------
# check_syntax.sh           every Lua script in the repo parses
# check_sources.py          apps/<major> package DB: remotes/depends/riscv exist
# check_elves.py            shipped RISC-V ELFs match the emulator constraints
# java/run_java.sh          src/ELF.java emulates the real RV32IM apps (JVM + stubs)
#
# Slow/optional stages (not part of the default run):
#   build/run_j2me_build.sh real device build via sdkcli.jar (~3 min)
#   :- ./run_all.sh --build
#
# Exit code 0 when every selected stage passes.

set -u

HERE="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
cd "$HERE"

DO_BUILD=0
for arg in "$@"; do
    case "$arg" in
        --build) DO_BUILD=1 ;;
        *) echo "run_all: unknown flag '$arg' (use --build)" >&2; exit 2 ;;
    esac
done

fail=0
stage() {
    name="$1"; shift
    echo
    echo "============================================================"
    echo "  $name"
    echo "============================================================"
    if "$@"; then
        echo ">> $name: OK"
    else
        echo ">> $name: FAILED"
        fail=$((fail + 1))
    fi
}

stage "1. Lua syntax"          ./check_syntax.sh
stage "2. Appstore DB"         python3 ./check_sources.py
stage "3. ELF headers"         python3 ./check_elves.py
stage "4. ELF emulator (JVM)"  ./java/run_java.sh
if [ "$DO_BUILD" -eq 1 ]; then
    stage "5. J2ME device build" ./build/run_j2me_build.sh
fi

echo
if [ "$fail" -eq 0 ]; then
    echo "ALL SUITES PASSED"
else
    echo "$fail SUITE(S) FAILED"
    exit 1
fi