#!/bin/bash
# Rebuilds the ELF emulator test fixtures straight from the repo sources.
#
# Produces:
#   tests/java/rv/memtest   (memtest.c linked with res/lib/libc.s, -stdlib)
#   tests/java/rv/abitest   (abitest.c: strings, qsort/bsearch, heap, LCDUI)
#
# Two toolchains are accepted:
#   - RISC-V GNU binutils/gcc -> ./build-elf.sh -stdlib (preferred here)
#   - clang + llvm-mc + ld.lld (fallback, no GNU toolchain installed)
# Both paths also sanity-check res/lib/libc.s: every .s shipped for the
# emulator must assemble (llvm-mc -triple=riscv32 -mattr=+m or the GNU as).

set -euo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "$HERE/../.." && pwd)"
WORK="$HERE/work"
mkdir -p "$WORK" "$HERE/rv"

FIXTURES="memtest abitest"

have_gnu() {
    local p
    for p in "${CROSS:-}" riscv64-unknown-elf- riscv64-unknown-linux-gnu- riscv64-linux-gnu- riscv32-unknown-elf-; do
        [ -z "$p" ] && continue
        command -v "${p}as" >/dev/null 2>&1 && command -v "${p}gcc" >/dev/null 2>&1 && return 0
    done
    return 1
}

if have_gnu; then
    for fixture in $FIXTURES; do
        "$ROOT/build-elf.sh" "$HERE/$fixture.c" -stdlib -o "$HERE/rv/$fixture" >/dev/null
        echo "built: $HERE/rv/$fixture"
    done
    exit 0
fi

for tool in clang llvm-mc ld.lld; do
    command -v "$tool" >/dev/null 2>&1 || { echo "linux-rv: no RISC-V GNU toolchain and '$tool' not found; skipping fixture build" >&2; exit 2; }
done

# 1) guest C runtime (res/lib/libc.s) -> object, validating the assembly.
llvm-mc -triple=riscv32 -mattr=+m -filetype=obj "$ROOT/res/lib/libc.s" -o "$WORK/libc.o"

# 2) test programs.
for fixture in $FIXTURES; do
    clang -target riscv32 -march=rv32im -mabi=ilp32 -ffreestanding -fno-builtin \
        -I"$ROOT/res/lib" \
        -c "$HERE/$fixture.c" -o "$WORK/$fixture.o"

    # 3) static ET_EXEC, entry at 0x10000 (1 MB guest RAM), no interpreter.
    ld.lld -m elf32lriscv --hash-style=sysv -Ttext=0x10000 --no-dynamic-linker \
        --entry=_start -o "$HERE/rv/$fixture" "$WORK/libc.o" "$WORK/$fixture.o"

    echo "built: $HERE/rv/$fixture"
done
