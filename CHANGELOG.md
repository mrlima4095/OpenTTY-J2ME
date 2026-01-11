OpenTTY Java Edition 1.17.1
Copyright (C) 2026 - Mr. Lima

---

ELF

- add support for fpu controller
- add support for more operations
- add support to accessing program arguments
- add support for syscalls `execve` `mkdir` `rmdir` `stat` `fstat` `ioctl` `clone` `getpriority` `setpriority` `lseek` `getdents` `dup` `dup2` `unlink`, `socket`, `connect`, `send`, `recv`, `bind`, `listen`, `accept`, `shutdown`, `setsockopt`, `getsockopt`, `sendto`, `recvfrom`, `getsockname`, `getpeername`, `signal`, `sigaction`, `setjmp`, `longjmp`, `gettid`, `nanosleep`, `pipe`, `select`, `pool`, `fsync`
- new debug mode - enable with `curl -s 1 debug true`
- ELF symbols manager

Bug fixes

- fixed a bug with invalid caching

FileSystem

- added `/proc/` support

Lua

- global functions caching
- added `push` library
- added support for labels
- added function `os.mkdir()`

General

- added debug mode
- input char changing `$` to normal user and `#` to root

Commands

- added command `time`

Packages

- default - all features
- lite - no ELF emulator and no LuaCanvas
- emu-lite - no Lua Canvas
- canvas-lite - no ELF emulator
- 