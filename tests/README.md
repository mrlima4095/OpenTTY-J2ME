# OpenTTY 1.18.2 test suite

Pre-release tests for OpenTTY. They run on a plain Linux box with only
`python3`, `lua`, `javac`/`java` and (fallback) the LLVM toolchain — no J2ME
SDK needed. The real device build (`sdkcli.jar`) is covered by an optional
stage.

## Quick start

    ./tests/run_all.sh              # default 4 suites
    ./tests/run_all.sh --build      # + the real J2ME device build (~3 min)

## Suites

| Suite | Command | Guards |
|---|---|---|
| Lua syntax | `tests/check_syntax.sh` | every `.lua` and every `#!/bin/lua` script in `src/`, `apps/`, `res/` parses (the maintainers' `assert(loadfile(...))` convention). |
| Appstore DB | `tests/check_sources.py` | each `apps/<major>/sources.lua`: `remote` files exist, `depends` resolve, `riscv=true` apps ship a real ELF, and `src/etc/sources` matches the catalog of the release in `src/OpenTTY.java`. |
| ELF headers | `tests/check_elves.py` | every shipped RISC-V binary is ELF32 LE, `EM_RISCV` (243), `ET_EXEC`/`ET_DYN`, and `ET_EXEC` entry < 1 MB (the emulator's guest RAM). |
| ELF emulator | `tests/java/run_java.sh` | runs the **real** `src/ELF.java` on the JVM against small J2ME stubs and executes the shipped `res/apps/dist` binaries (`rev`, `tac`, `seq`, `factor`, `cal`, `rot13`, `memtest`) checking exact stdout. |
| Device build | `tests/build/run_j2me_build.sh` | real `sdkcli.jar` build; verifies the jar has `MIDlet-Version: 1.18.2`, the full `ELF.class` emulator, and the `/boot`, `/bin`, `/lib`, `/etc/sources` seeds. |

## How the emulator test works

`src/ELF.java` is compiled verbatim alongside the tiny J2ME stubs in
`tests/java/stubs/` (`Lua`, `OpenTTY`, `Process` + the `javax.microedition.*`
interfaces it imports) and the harness in `tests/java/src/`. No emulator code
is modified or stubbed, so the tests exercise the exact code that ships on a
device.

The `memtest` fixture is rebuilt from `tests/java/memtest.c` +
`res/lib/libc.s` by `tests/java/build_rv.sh` using `clang`/`llvm-mc`/`ld.lld`
(the same pipeline `build-elf.sh -stdlib` uses) whenever an LLVM toolchain is
available; the test skips gracefully otherwise.

## Requirements

- `lua` 5.x (`check_syntax.sh`)
- `python3` (`check_sources.py`, `check_elves.py`)
- `javac`+`java` (`run_java.sh`, `run_j2me_build.sh`)
- `clang`/`llvm-mc`/`ld.lld` (optional; only to rebuild the `memtest` fixture)