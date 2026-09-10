# AGENTS.md

OpenTTY: a J2ME MIDlet (CLDC-1.0/MIDP-2.0) that is a Lua-scripted shell + RISC-V ELF emulator. It also contains a Docker/Coolify deployment of the web services (PHP + Python proxies).

## Source layout — what edits go where

- `src/` — canonical MIDlet source: `OpenTTY.java`, `Lua.java`, `ELF.java`, `LuaCanvas.java`, plus `src/bin` (built-in `/bin` commands, Lua scripts with `#!/bin/lua` shebang), `src/etc`, `src/boot` (kernel/ramdisk seed files), `src/lib/libcore.so` (a Lua module despite the `.so` name).
- `apps/<major>/` — the on-device app-store catalog, versioned by major release. `apps/1.18/` holds the `file/ net/ sys/ games/ dev/` dirs currently shipping with 1.18.x. Apps are registered in the mirror table in `apps/<major>/sources.lua` (e.g. `["docker"] = { remote = "sys/docker/main.lua", here = "/bin/docker", ... }`), which doubles as the `/etc/sources` file on-device (seeded from `src/etc/sources`). Adding an app means updating the app files **and** the sources mirror. New majors: copy the previous `apps/<major>/` dir; the `remote` paths stay relative to the major dir and `version = "<major>"` inside `sources.lua`.
- `res/` — embedded resources (lua modules under `res/lua/modules/`, bundled apps, pages). `res/lib/` holds the guest C runtime for the emulator, **ported to RISC-V RV32I**: `lib32.s` (full asm stdlib, `-lib`) and `libc.s` (thin `li a7,#LIB_*; ecall; ret` wrappers, `-stdlib`). `lib32.s`'s `print_decimal`/`udiv10` are software (no M-ext needed); its `sbrk` tolerates the emulator rounding `brk` up to 4K.
- `res/archive/elf/` — emulator archives for `res/swap_lite.sh`: `Full.java` (full RV32IM emulator, backed up here) and `Lite.java` (lite stub, marker `// ELF Lite`). `swap_lite.sh` swaps `src/ELF.java` between them; the legacy `res/archive/ELF.java`/`ELF.full.java` are stale pre-RISC-V files.
- `build-elf.sh` — builds RISC-V RV32IM ELFs for the emulator (`ET_EXEC`, ELF32 LE, `EM_RISCV`=243, entry ≤ 1 MB). Flags: `-T <addr>`, `-entry <sym>`, `-shared`, `-lib` (links `res/lib/lib32.s`, drop-demo-`main` via awk), `-stdlib` (links `res/lib/libc.s`), `-keep`; `CROSS=` overrides the toolchain search (`riscv64-unknown-elf-`, `riscv64-linux-gnu-`, …). `.c` files compile with `-march=rv32im -mabi=ilp32 -fno-builtin`; `as` runs with `-march=rv32im -mabi=ilp32` (`ASFLAGS` array) and `ld` with `-m elf32lriscv` (`LDFLAGS`) — keep those arrays, not embedded flag strings, so quoted `"$AS"`/`"$LD"` calls work.
- `lua/` — the **host-side** ports of the J2ME runtime, all runnable with plain Python 3: `lua/runtime.py` (port of `src/Lua.java`), `lua/run.py` (CLI harness, seeds `/etc/` and `/boot/` from `src/`), and `lua/sys/` (the desktop kernel — ports of `src/OpenTTY.java`/`src/Lua.java`, formerly `krnl/`; run with `python3 -m lua.sys`, mounts a real dir instead of RMS).
- `kernel/` — legacy ARM32 assembly kernel (`vmlinuz.s`) built into an `ET_EXEC`/`EM_ARM` ELF (`kernel/vmlinuz`). It is not loadable by the current RISC-V-only `src/ELF.java`; `src/boot/vmlinuz` is its seed placeholder on the RAM disk.
- `dist/archive/<ver>` — per-version filesystem snapshots of the app store.
- `nbproject/project.properties` — NetBeans J2ME project config (MIDlet-Version 1.18.1, jar/jad names).

## Build

- There is **no working CI or desktop build** in-repo right now. `src/` needs a J2ME toolchain. The real build happens **on-device** with the J2ME SDK (see `docs/BUILD.md`), producing `dist/OpenTTY.jar` + `dist/OpenTTY.jad`.
- Sanity-check every Lua script you touch with: `lua -e "assert(loadfile('<file>'))"`.

## Lua runtime gotchas (verified in `src/Lua.java`)

- `string.format`, `string.rep`, `string.gsub`, `string.gmatch` do **not** exist. Available string funcs are only: `upper lower len find match reverse sub hash byte char trim uuid split getCommand getArgument env getpattern startswith endswith`. The `docker` app crashed with "Attempt to call a non-function value" because it used `string.format`/`string.rep` — do not reintroduce them.
- `io.dirs(path)` returns entries only for `/tmp/`, `/mnt/<sub>` (real FS), and exactly `/bin/`, `/etc/`, `/lib/`, `/boot/`, `/home/`; any other path yields an empty table.
- `string.startswith`/`endswith` are native — don't shadow them with Lua reimplementations.
- Daemon convention (matches how `os.request(1, "serve", path)` spawns services in `Lua.java`): daemon apps must check `arg[1] == "--deamon"` (**the typo is the convention** — other daemons in `apps/` use it), name themselves with `os.setproc("name", ...)`, and end with a top-level `return function(payload, args, scope, pid, uid) ... end` as the handler.
- `/bin/init` is PID 1 with the kernel handler: `os.request(1, payload, arg)` implements `sendsig`, `serve`, `rms`, `user`/`useradd`/`userdel`, `setsh`, `netsh`, etc.
- Java exceptions (e.g. `java.lang.NullPointerException`) carry no message. The runtime appends a **Lua-side traceback** — `Lua <file>:<line>`, a caret-backed copy of the offending source line (`^---` + `near '<token>'`), and a `stack traceback:` of the Lua function chain — via `Lua.getTraceback(e)`, used in `run()`, `pcall`, `os.request` handler failures, background threads, and UI callbacks. This depends on `Token.offset` (absolute char offset recorded in `tokenize`), `lastCode`, `frameStack`/`thrownFrames`, `thrownTokens`/`thrownTokenIndex` (set by `recordThrow()` in `run()`/`LuaFunction.call()`), `pointerBlock()`, `tokenLexeme()`, and `LuaFunction.name` — keep these in sync in `src/`. Errors contained by `pcall` (and `require`/`load` call sites via `exec`) reset the thrown state so later tracebacks point at the real spot.

## ELF emulator (`src/ELF.java` — RISC-V RV32IM core)

- Machine: `EM_RISCV`=243, 32-bit, 1 MB guest RAM, `registers[32]` (x0 hardwired 0; `REG_SP`=2 x2, `REG_LR`=1 x1/ra, `REG_A0..A3`=x10..13, `REG_A7`=x17/a7). Decoder: RV32I (LUI/AUIPC/JAL/JALR/BL* /LBHWLHU/SB/SH/SW/ALU/SLL*/SR* /FENCE/SYSTEM) + M-ext (MUL/MULH/MULHSU/MULHU/DIV/DIVU/REM/REMU). Syscall via `ecall` → `handleSyscall(registers[REG_A7])`; `ebreak` stops.
- Guest C library via lightweight SYSCALL wrappers (`LIB_BASE=1000`): ids 1000–1053 in `ELF.java` `handleLibraryCall` cover string (`strlen…strdup`), `atoi`/`abs`/`toupper`/`tolower`, memory (`memcpy/memmove/memset/memcmp/memchr`), IO (`putchar/puts/printf/sprintf/snprintf`), heap (`malloc/calloc/realloc/free`), `getpid`, RISC-V runtime helpers, LCDUI (`res/lib/lcdui.h`), `graphics_taskmngr`, and `opentty_setproc(key, value)` for ELF process name, registered screen, command and database values. `exit/_exit/abort` = syscall 1. Raw Linux syscalls (`write/read/open/close/brk`) use the emulator's fixed EABI numbers: load into a7 and `ecall`.
- LCDUI: `ELF` itself implements `CommandListener`; it owns Displayable/Item/Command handles and queues `{type,screen,command,index}` events. `lcdui_wait_event` suspends the guest when empty; `commandAction` enqueues the event and resumes it. On ELF exit/crash, cleanup removes its process and restores another registered process screen, matching Lua. Do not add a listener class for this path.
- Guest `_start` CRT reads args from the stack: `[sp]=argc`, `[sp+4]=argv`, envp after the NULL (initial SP 16-byte aligned).
- `printf`/`sprintf` support `%s %c %d %i %u %x %X %o %p %%` with `l`/`h` (32-bit) and `ll` (64-bit); `%f/%e/%g` consume a 64-bit arg and print `0` (no float). Flags/width/precision parsed and ignored.
- Varargs follow the RISC-V convention: 32-bit args arrive in a0–a7 (x10–17) then `[sp]…`; a 64-bit (`long long`) arg must live in an **even** register pair and on the stack is 8-aligned. `ELF.java` walks params via `libcArgReg`/`libcArgSpOff` (`libcNext32`/`libcNext64`) — keep in sync.
- Heap: first-fit with split, header `{size,next}` at payload−8, new regions from `findFreeMemoryRegion` (64 KB), no coalescing.
- `setjmp`/`longjmp`: jmp_buf saves all 32 registers (x0..x31) + pc at offset 128 (buf ≥ 132 bytes).
- Demo ELF: `/tmp/opencode/mkrv.py` builds a hand-RV32I `rvtest.elf` (write + exit); `TestRV.java` runs it against the stubs. Host-side `javac -d /tmp/jc -cp /tmp/opencode/tty-stubs src/ELF.java /tmp/opencode/tty-stubs/{Lua,OpenTTY,Process}.java` for typechecking. **Dynamic linking (`.so`) works on RISC-V**: `loadSharedObject` reads dynsym/dynstr/relocs from guest `memory` and applies `.rela.dyn`/`.rela.plt` (RELA, 12-byte) with `R_RISCV_32/RELATIVE/COPY/JUMP_SLOT/GLOB_DAT`; the executable side does the same (`processSymbols`/`processRelocations`), with copy relocs (`importdemo`) and eager JUMP_SLOT resolution via `resolveSymbol` (libc stubs under `globalSymbols["libc.so.6"]`, app `.so` under their DT_NEEDED name). `setupPLTGOT` writes only GOT[0]=dynamic — lld's RISC-V `.got.plt` uses GOT[2] as the **first** function slot, do not zero it. All supported PLT relocations resolve eagerly; unresolved symbols retain a zero GOT slot.
- Sample C apps: `res/apps/src/*.c` → `res/apps/dist/<name>`; the ones using `-stdlib` now build against the RISC-V `res/lib` port (LLD/`llvm-mc` can also validate: `llvm-mc -triple=riscv32 res/lib/*.s` and `ld.lld -m elf32lriscv`).

## Deployment (`docker/` + root)

- `Dockerfile`: `php:8.3-fpm-alpine` + nginx + supervisor. Services started by `supervisord`: php-fpm, nginx, `python3 server.py` (TCP `:31522`, the OpenTTY mirror service), and `pproxy/app.py` (TCP `:4096` + Flask web on `:10141`).
- nginx proxies `/cli` + `/api/` to a Flask upstream at `127.0.0.1:10141`; PHP via fastcgi `:9000`; several dirs served with `autoindex`. EXPOSE: `80, 31522, 4096, 10141`.
- `pproxy` is a **git submodule**; the Dockerfile pip-installs from `pproxy/requirements.txt` plus `flask_cors requests`. Don't rely on the builder cloning submodules: the Dockerfile fell-back-clones `pproxy` from GitHub when `pproxy/app.py` is missing.

## Git workflow

- `origin` is SS//git@ssh.github.com:443/mrlima4095/OpenTTY-J2ME.git main`
  (port 22 is blocked from this host; plain `git push` works since `origin` already points at the 443 URL.)
- User preference: commit and push completed work (via the SSH URL above) without waiting to be asked.
