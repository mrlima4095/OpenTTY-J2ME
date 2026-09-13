# OpenTTY Boot Menu

OpenTTY reads a GRUB-style boot configuration at boot time from `/boot/grub.cfg`
VFS/RMS stable state. This document explains the entries it accepts, the boot
flow, and how path-based chroots work.

## Configuration File

`/boot/grub.cfg` is seeded from `src/boot/grub/grub.cfg`. A copy made on the
device in `/boot/grub.cfg` (via `write("/boot/grub.cfg", ...)`) **overrides the
seed**, because the boot loader reads the RMS-backed file first and falls back
to the bundled resource only when it does not exist.

The format is a small, GRUB-inspired language:

```
set default=0

menuentry "OpenTTY" {
    root=/
    init=/bin/init
}
```

### Global settings

| Setting   | Meaning                                                                 |
|-----------|-------------------------------------------------------------------------|
| `timeout` | Accepted for GRUB compatibility but **ignored** — OpenTTY never auto-boots or counts down. |
| `default` | Preselected entry index (`0`, `1`, ...) or a quoted title (`"My OS"`). Default is the first entry. |

### Menu entries

A `menuentry "Title" { ... }` block declares one boot candidate:

| Setting | Default     | Meaning                                                            |
|---------|-------------|--------------------------------------------------------------------|
| `root`  | `/`          | Root of the booted system. `/` boots the normal OpenTTY (RMS). Any other absolute path is used as a chroot root (see below). |
| `init`  | `/bin/init`  | Script executed as PID 1 relative to `root`.                       |

Comments start with `#` and blank lines are ignored.

## Boot Flow

1. `loadBootMenu()` reads `/boot/grub.cfg` and parses it with `parseBootMenu()`.
2. A single entry (or none) boots immediately; multiple entries show a `List`
   menu titled "OpenTTY - Boot". The menu waits for the user to pick an entry and
   press **Boot** (or tap a line — the list is implicit); there is no countdown
   or auto-boot.
3. `bootSelect(index)` launches the chosen boot on a dedicated thread so the
   LCDUI event thread is never blocked.
4. `bootEntry()` applies the entry:

   - Standard entry (`root=/` with `init=/bin/init`) → `defaultBoot()` — shows
     the first-run account gate and then `bootKernel()`.
   - Anything else → `bootKernel()` directly, which spawns the `init` script
     as PID 1 (no OpenTTY account gate).

## Chroot (`root=/mnt/...`)

When `root` is not `/`, it must be an absolute path to a folder on the real
file system (a `FileConnection` root, typically an SD card). That folder
becomes the system root: every path used by the shell, the `init` script, the
apps, and the ELF emulator is redirected inside it.

`OpenTTY.redirect(path)` performs the mapping and is applied everywhere a path
enters the system (`getInputStream`, `write`, `deleteFile`, and from Lua
`dirs`, `chdir`, `MKDIR`):

| Path prefix                        | Mapping                      |
|------------------------------------|------------------------------|
| `/boot/`, `/proc/`, `/tmp/`, `/mnt/`, `/dev/` | Kept as-is — always the global system mounts, never redirected. |
| `/` (and everything else)           | Rewritten under the chroot root, e.g. `/etc/fstab` → `/mnt/Card/OpenTTY/etc/fstab`. |

Global mounts are shared across all roots so that `/boot` (firmware/RMS),
`/proc` (virtual), `/tmp` (session RAM) and `/mnt` (real file system
browsing) behave identically whether OpenTTY boots from RMS or from a card.

Example — a second boot candidate running from an SD card:

```
set default="OpenTTY"

menuentry "OpenTTY" {
    root=/
    init=/bin/init
}

menuentry "OpenTTY SD (chroot)" {
    root=/mnt/Card/OpenTTY
    init=/bin/init
}
```

## Where the code lives

The boot logic is implemented in `src/OpenTTY.java`:
`loadBootMenu`, `parseBootMenu`, `bootMenuTitleIndex`, `showBootMenu`,
`bootSelect`, `bootEntry`, `defaultBoot`, `bootKernel`, `redirect`, and the
`BootEntry` class. Lua-side redirection hooks for `dirs`/`chdir`/`MKDIR` are
in `src/Lua.java`. The desktop kernel under `lua/sys` does **not** implement
the boot menu.