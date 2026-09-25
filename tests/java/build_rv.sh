#!/bin/bash
# Rebuilds the ELF emulator test fixtures straight from the repo sources when no
# RISCV binutils are installed. Uses clang + llvm-mc + ld.lld (LLVM suite).
#
# Produces:
#   tests/java/rv/memtest   (memtest.c linked with res/lib/libc.s, -stdlib)
#
# This doubles as a toolchain sanity check for res/lib/libc.s: every .s that
# ships for the emulator must assemble with llvm-mc -triple=riscv32 -mattr=+m.

set -euo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "$HERE/../.." && pwd)"
WORK="$HERE/work"
mkdir -p "$WORK" "$HERE/rv"

for tool in clang llvm-mc ld.lld; do
    command -v "$tool" >/dev/null 2>&1 || { echo "linux-rv: '$tool' not found; skipping fixture build" >&2; exit 2; }
done

# 1) guest C runtime (res/lib/libc.s) -> object, validating the assembly.
llvm-mc -triple=riscv32 -mattr=+m -filetype=obj "$ROOT/res/lib/libc.s" -o "$WORK/libc.o"

# 2) test program.
clang -target riscv32 -march=rv32im -mabi=ilp32 -ffreestanding -fno-builtin \
    -c "$HERE/memtest.c" -o "$WORK/memtest.o"

# 3) static ET_EXEC, entry at 0x10000 (1 MB guest RAM), no interpreter.
ld.lld -m elf32lriscv --hash-style=sysv -Ttext=0x10000 --no-dynamic-linker \
    --entry=_start -o "$HERE/rv/memtest" "$WORK/libc.o" "$WORK/memtest.o"

echo "built: $HERE/rv/memtest"