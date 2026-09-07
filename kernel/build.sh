#!/bin/bash
# build.sh — Build the OpenTTY real kernel (kernel/vmlinuz) for src/ELF.java.
#
# Produces kernel/vmlinuz, an ARM32 ET_EXEC ELF that the J2ME emulator boots
# (the same format src/bin programs use). To actually run it on-device, copy
# kernel/vmlinuz to /boot/vmlinuz (default per /boot/config KERNEL=).
#
# On the host this is CROSS-compiled with the bare-metal ARM binutils; the
# same toolchain is baked into build-elf.sh, so this just wraps it.

set -euo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(dirname "$HERE")"

cd "$ROOT"
exec ./build-elf.sh kernel/vmlinuz.s -o kernel/vmlinuz
