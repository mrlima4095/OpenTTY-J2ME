OpenTTY Java Edition 1.17.1
Copyright (C) 2026 - Mr. Lima

---

ELF

- add support for fpu controller
- add support for more operations
- add support for syscalls `execve` `mkdir` `rmdir` `stat` `fstat` `ioctl` `clone` `getpriority` `setpriority` `lseek` `getdents` `dup` `dup2` `unlink`, `socket`, `connect`, `send`, `recv`, `bind`, `listen`, `accept`, `shutdown`, setsockopt`, `getsockopt`, `sendto`, `recvfrom`, `getsockname`, `getpeername`, `signal`, `sigaction`, `setjmp`, `longjmp`, `gettid`, `nanosleep`, `pipe`, `select`, `pool`, `fsync`
- new debug mode - enable with `curl -s 1 debug true`

Bug fixes

- fixed a bug with invalid caching