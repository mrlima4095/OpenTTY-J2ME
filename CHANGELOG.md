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


