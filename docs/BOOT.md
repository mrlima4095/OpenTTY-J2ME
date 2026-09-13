# OpenTTY Boot Menu

OpenTTY reads a GRUB-style boot configuration at boot time from `/boot/grub/grub.cfg`
VFS/RMS stable state. This document explains the entries it accepts, the boot
flow, and how path-based chroots work.

## Configuration File

`/boot/grub/grub.cfg` is seeded from `src/boot/grub/grub.cfg` and read with
`read()`. A copy made on the device in `/boot/grub/grub.cfg` (via
`write("/boot/grub/grub.cfg", ...)`) **overrides the seed**, because the boot
loader reads the RMS-backed file first and falls back to the bundled resource
only when it does not exist.

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

1. `loadBootMenu()` reads `/boot/grub/grub.cfg` and parses it with `parseBootMenu()`.
2. A config with at least one `menuentry` shows a `List` menu titled "OpenTTY -
   Boot" that waits for the user to pick an entry and press **Boot** (or tap a
   line — the list is implicit); there is no countdown or auto-boot. An empty
   or missing config boots the default entry straight.
3. `bootSelect(index)` runs on the current thread (the UI event thread) and
   hands the entry to `bootEntry()`.
4. `bootEntry()` reads `root`/`init` from the entry and applies it:

   - Standard entry (`root=/` with `init=/bin/init`) → `defaultBoot(root, init)`
     — shows the first-run account gate and then `bootKernel(root, init)`.
   - Anything else → `bootKernel(root, init)` directly, which spawns the `init`
     script as PID 1 (no OpenTTY account gate).

   The chosen `root` is stored into `globals["ROOT"]` (so the shell resolves
   every path against it) and `init` is passed as an argument — there is no
   global `bootInit` variable.

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
`bootSelect`, `bootEntry`, `defaultBoot`, `bootKernel`, and `redirect`. Menu
entries are plain `Hashtable`s (keys `title`, `root`, `init`) — there is no
`BootEntry` class. Lua-side redirection hooks for `dirs`/`chdir`/`MKDIR` are
in `src/Lua.java`. The desktop kernel under `lua/sys` does **not** implement
the boot menu.