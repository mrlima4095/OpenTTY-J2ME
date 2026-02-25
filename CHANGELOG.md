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

lua

- Added functions `string.startswith(s, pattern)` and `string.endswith(s, pattern)`

elf

