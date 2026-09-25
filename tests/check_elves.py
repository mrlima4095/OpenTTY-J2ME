#!/usr/bin/env python3
"""Validate every RISC-V ELF shipped for the emulator.

Checks the exact constraints src/ELF.java enforces (see docs/ELF/README.md):
  - ELF magic, ELFCLASS32 (1), little-endian (1)
  - e_machine == EM_RISCV (243)
  - e_type == ET_EXEC (2) or ET_DYN (3)
  - ET_EXEC entry point < 0x100000 (the 1 MB guest RAM)  -> loads on device

Scans: res/apps/dist/*, every ELF under apps/<major>/**, and the test fixture.

Exit code 0 = OK, 1 = problems found.
"""

import os
import struct
import sys

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
MAGIC = b"\x7fELF"


def fields(path):
    with open(path, "rb") as f:
        b = f.read(40)
    if b[:4] != MAGIC:
        return None
    try:
        e_type = struct.unpack("<H", b[16:18])[0]
        e_machine = struct.unpack("<H", b[18:20])[0]
        entry = struct.unpack("<I", b[24:28])[0]
    except struct.error:
        return None
    return b[4], b[5], e_type, e_machine, entry


def walk():
    seen = set()
    for base in ("res/apps/dist",):
        p = os.path.join(ROOT, base)
        if not os.path.isdir(p):
            continue
        for name in sorted(os.listdir(p)):
            full = os.path.join(p, name)
            if os.path.isfile(full):
                seen.add(full)
    # apps catalogs: any file starting with ELF magic
    for cat in ("apps",):
        for dirpath, _dirs, files in os.walk(os.path.join(ROOT, cat)):
            for name in files:
                full = os.path.join(dirpath, name)
                try:
                    with open(full, "rb") as f:
                        if f.read(4) == MAGIC:
                            seen.add(full)
                except OSError:
                    pass
    # fixture
    for extra in ("tests/java/rv/memtest",):
        full = os.path.join(ROOT, extra)
        if os.path.isfile(full):
            seen.add(full)
    return sorted(seen)


def main():
    failures = 0
    count = 0
    for path in walk():
        f = fields(path)
        count += 1
        if f is None:
            print("NOT-ELF: %s" % os.path.relpath(path, ROOT))
            failures += 1
            continue
        klass, data, e_type, e_machine, entry = f
        ok = True
        errs = []
        if klass != 1:
            ok, errs2 = False, errs + ["ELFCLASS!=32"]
        if data != 1:
            ok, errs2 = False, errs + ["not little-endian"]
        if e_machine != 243:
            ok, errs2 = False, errs + ["e_machine!=EM_RISCV(243)"]
        if e_type not in (2, 3):
            ok, errs2 = False, errs + ["e_type not EXEC/DYN"]
        if e_type == 2 and entry >= 0x100000:
            ok, errs2 = False, errs + ["e_entry>=1MB (won't load)"]
        if not ok:
            print("BAD-ELF: %s  %s (entry 0x%x)" % (os.path.relpath(path, ROOT), "; ".join(errs), entry))
            failures += 1

    print("check_elves: %d ELF binaries scanned, %d failures" % (count, failures))
    sys.exit(1 if failures else 0)


if __name__ == "__main__":
    main()