# OpenTTY Roadmap

This document tracks the planned direction of OpenTTY. Every item carries
**status**, **priority** and **estimated effort**, so you can glance at it and
know what to tackle first.

---

## Legend

**Status:** ✅ done · 🚧 in progress · ⬜ planned

**Priority:**

| Level | Meaning |
|---|---|
| P0 | Now — blocks the next release |
| P1 | Next — following cycle |
| P2 | Later |
| P3 | Someday / loose idea |

**Effort:** S (small) · M (medium) · L (large)

---

## Table of contents

1. [Milestones by version](#milestones-by-version)
2. [Next steps (suggested order)](#next-steps-suggested-order)
3. [Kernel](#kernel)
4. [Shell and utilities (`src/bin`)](#shell-and-utilities-srcbin)
5. [Lua J2ME](#lua-j2me)
6. [ELF Emulator](#elf-emulator)
7. [X11](#x11)
8. [Boot Menu](#boot-menu)
9. [Quality and maintenance](#quality-and-maintenance)
10. [Ideas / no deadline](#ideas--no-deadline)

---

## Milestones by version

### 1.19.1
- ⬜ **Zip API** — P0 · L

### 1.20
- ⬜ Pipes (kernel) — P0 · M
- ⬜ Core shell utilities (`mv`, `ln`, `basename`, `dirname`, `test`, `which`) — P0 · S
- ⬜ `bg` / `fg` — P1 · S
- ⬜ Complete `fstat`, `stat` in the ELF emulator — P1 · M

### 1.21
- ⬜ Permission system + Groups — P1 · L
- ⬜ Task scheduler — P1 · L
- ⬜ Event handler — P1 · M
- ⬜ Themes (X11) — P2 · M

---

## Next steps (suggested order)

1. **Zip API** (1.19.1) — unblocks installing `.zip` packages through `yang`/`pkg`.
2. **`mv` / `ln` / `basename` / `dirname` family** — same base, unblocks scripts.
3. **`test` + `which`** — enables real `if` in the shell.
4. **`tee` / `cut` / `tr`** — makes pipelines usable.
5. **Pipes** in the kernel — prerequisite for most of the rest.

---

## Kernel

- ⬜ Pipes — P0 · M
- ⬜ `bg` / `fg` commands — P1 · S
- ⬜ Event handler — P1 · M
- ⬜ Task scheduler — P1 · L
- ⬜ Automatic update check — P2 · M
- ⬜ Backup system — P2 · M
- ⬜ Notify API — P2 · S
- ⬜ Advanced scope — P2 · M
- ⬜ Plugins — P2 · L
- ⬜ Bluetooth API — P3 · L
- ⬜ Power / backlight API — P2 · S
- ⬜ Permission system — P1 · L
- ⬜ Groups — P1 · M
- ⬜ i18n translations — P3 · L

---

## Shell and utilities (`src/bin`)

### POSIX base
- ⬜ `mv` — P0 · S
- ⬜ `ln` (hard/symlink) — P1 · S
- ⬜ `basename` / `dirname` — P0 · S
- ⬜ `which` — P0 · S
- ⬜ `test` (`[ -f ]`, `-d`, `-z`…) — P0 · M
- ⬜ `chmod` / `chown` — P2 · M

### Pipeline and text
- ⬜ `tr` / `cut` / `tee` — P1 · M
- ⬜ `less` / `more` (real pager with scroll) — P1 · M
- ⬜ Persistent `history` + `alias` — P1 · M
- ⬜ `patch` (closes the `diff` loop) — P2 · M

### Files and compression
- ⬜ `tar` / `gzip` — P2 · L

### Editor
- ⬜ `nano`: pre-fill "Save as" and/or use `explorer.lua` as a file picker — P1 · S

---

## Lua J2ME

- ✅ Metatable support
- ✅ `debug` table
- ✅ LuaCanvas
- ⬜ Coroutine support — P1 · L
- ⬜ `collectgarbage` threshold mode — P2 · M
- ⬜ `table.serialize` / `table.unserialize` — P2 · S
- ⬜ Regular expressions — P2 · L
- ⬜ FTP API — P2 · M

---

## ELF Emulator

- ⬜ Complete `fstat`, `stat` — P1 · M
- ⬜ Network syscalls: `bind`, `listen`, `accept`, `recvfrom`, `sendto`, `getsockopt`, `setsockopt` — P1 · L
- ⬜ Add support for `libc` — P1 · L

---

## X11

- ✅ Legacy screens
- ⬜ Themes — P2 · M

---

## Boot Menu

**Title:** Setup Utility

**Screens:**
- About
- Menu
  - Boot Menu
  - Recovery Menu

---

## Quality and maintenance

- ⬜ Modularize `Lua.java` (~305 KB) into `Parser`, `State`, `Stdlib` — P1 · L
- ⬜ Modularize `ELF.java` (~227 KB) into `Cpu`, `Loader`, `Syscalls`, `Memory` — P1 · L
- ⬜ Fill in `CHANGELOG.md` (currently ~52 B, practically empty) — P1 · S
- ⬜ Remove old JARs from `dist/` from version control — P1 · S
- ⬜ Move site/proxies (`index.php`, `reddit.php`, `pinterest.php`, `favicon.ico`) into `web/` — P2 · S

---

## Ideas / no deadline

- ⬜ Zip **writing** support (ZipOutputStream) — depends on the Zip API
- ⬜ Path autocomplete / syntax highlighting in the shell
- ⬜ Package signing in `yang`/`pkg`
