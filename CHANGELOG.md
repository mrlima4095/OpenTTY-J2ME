OpenTTY Java Edition 1.18.1
Copyright (C) 2026 - Mr. Lima

---

bug fixes

- fixed a bug in id, it can't retrieve id from root and another system virtual users

general

- Added Kernel request `netsh` to get openned objects
- New syscall added `nice` to change process priority
- New log manager `sys/smile/logs.lua`, install with `yang install log`
- Limited Lua cached tokens to 100 files
- Native Shell `os.execute(cmd)` still wrote in Java


lua

- Added functions `string.startswith(s, pattern)` and `string.endswith(s, pattern)`

elf

yang - package manager

- Updated to `1.5.1`
- New command `download [pkg] [file]` to download a package without install it
- New command `run [file]` to run installation scripts
- Fixed **not found** message that disappears from stdout



