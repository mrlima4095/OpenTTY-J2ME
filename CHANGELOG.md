OpenTTY Java Edition 1.18.1
Copyright (C) 2026 - Mr. Lima

---

## Build 2026-1.18.1-03x28

filesystem / vfs

- Subdirectories inside `/bin/`, `/etc/`, `/lib/` are now supported as RMS mounts
- Each subdir (e.g. `/bin/tools/`) maps to its own page (index) of the `OpenRMS` store via a stable path hash (indices >= 6), keeping the every-store-in-OpenRMS model (no new RecordStores cluttering `/home/`)
- Read, write and delete now resolve nested paths at any depth (`/bin/tools/sub/file.lua`)
- Directory listing (`io.dirs` and ELF `getdents`) works for any `/bin|etc|lib/...` folder
- Writing into a subdir auto-registers it in the VFS (`cd` + `ls` detect it)
- `rms` handler can clear subdir stores (`rm -r /bin/tools`)
- Declare subdirs in `/etc/fstab` under their parent line using a trailing `/` (e.g. `tools/`)
- Refactored `addFile` to operate on a store index instead of a base string
- Root's home is now `/root/` (OpenRMS index 6, same top-level style as `/bin`→3, `/lib`→4, `/etc`→5); regular users cannot read, write or enter it, and `rms` stays root-only
- VFS hash subdirectories now use indices `>= 9` (`VFS_RESERVED` moved from 6 to 9), leaving indices 7 and 8 free for future top-level mounts without remapping
- `/root/` is declared in `/etc/fstab` root line, `su`, `chdir` and directory listing all enforce the root-home rule
- Fixed `rm` in `/bin|etc|lib/` subdirs: it no longer errors with exit code 5 (read-only) — `deleteFile` now removes a registered VFS subdirectory (clears its OpenRMS store and drops it from the `fs` table), and `rm` accepts `-r`/`-rf`/`-fr`
- Entering `/root/` without permission now returns exit code 13 and the shell prints `cd: <dir>: permission denied`
- Default shell home is now `/home/` for every user (including root after `su`); `/root/` remains as a protected directory but login/boot does not jump to it
- `/proc/` virtual filesystem: `cpuinfo`, `meminfo`, `uptime`, `version` plus per-process dirs `/proc/<pid>/` with `cmdline`, `comm`, `stat`, `status`; regular users only see/read their own processes (root sees all), enforced in file reads, directory listing and `cd`
- VFS subdirs under `/bin/`, `/etc/`, `/lib/` now persist across restarts into `/etc/vfs.conf` (written on mkdir, removed on delete/`rms`, restored on mount) so created folders survive closing the MIDlet

runtime / exit

- `os.exit()` no longer prints `java.lang.Error` on the terminal when it kills a process
- Process death via `os.exit` (any exit code), status abort and `Process killed` is now silent
- Real Lua errors and resource errors are still reported

shell / commands

- New `mkdir` command (`/bin/mkdir`) to create directories, including VFS subdirectories under `/bin/`, `/etc/`, `/lib/`
- `os.mkdir` now creates VFS subdirectory mounts (via `registerVfsDir`) in addition to `/mnt/`
- `mkdir` under `/bin/`, `/etc/`, `/lib/`, `/root/` is now root-only (exit 13 for regular users), consistent with write/delete rules

---

bug fixes

- fixed a bug in id, it can't retrieve id from root and another system virtual users

general

- Added Kernel request `netsh` to get openned objects
- New syscall added `nice` to change process priority
- New log manager `sys/smile/logs.lua`, install with `yang install log`
- Limited Lua cached tokens to 100 files
- Native Shell `os.execute(cmd)` still wrote in Java
- SheBang `#!/bin/sh` on `. [file]` run file with shell
- Added the **Add new line** button in Nano Editor
- Config. file `OpenRMS` doesnt appear in file listings
- New sh label
- Allowed multiple terminals

lua

- Added functions `string.startswith(s, pattern)` and `string.endswith(s, pattern)`
- Read a file or stream with chunck size `-1` will read until end of file/ connection end
- fixed `tonumber` invalid or missing value message

elf

- 

yang - package manager

- Updated to `1.5.1`
- New command `download [pkg] [file]` to download a package without install it
- New command `run [file]` to run installation scripts
- Fixed **not found** message that disappears from stdout
- Command `yang install *` install all available packages
- Indexed new package `du` 
- `yang` linked to `pkg` 


