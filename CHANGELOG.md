OpenTTY Java Edition 1.17.1
Copyright (C) 2026 - Mr. Lima

---

## 🧠 **ELF**

- 🎮 Added support for FPU controller  
- ➕ Added support for more operations  
- 🔧 Added support for accessing program arguments  
- 📞 Added support for syscalls:  
  `execve`, `mkdir`, `rmdir`, `stat`, `fstat`, `ioctl`, `clone`, `getpriority`, `setpriority`, `lseek`, `getdents`, `dup`, `dup2`, `unlink`, `socket`, `connect`, `send`, `recv`, `bind`, `listen`, `accept`, `shutdown`, `setsockopt`, `getsockopt`, `sendto`, `recvfrom`, `getsockname`, `getpeername`, `signal`, `sigaction`, `setjmp`, `longjmp`, `gettid`, `nanosleep`, `pipe`, `select`, `pool`, `fsync`  
- 🐛 New debug mode — enable with `curl -s 1 debug true`  
- 📦 ELF symbols manager  

## 🐛 **Bug Fixes**
- 🔧 Fixed a bug with invalid caching  

## 📁 **FileSystem**
- 📂 Added `/proc/` support  

## 🦎 **Lua**
- 📚 Added `push` library  
- 🏷️ Added support for labels  
- 📁 Added function `os.mkdir()`  

## 🖥️ **General**
- 🐛 Added debug mode  
- ⌨️ Input char changing: `$` for normal user, `#` for root  
- 📁 Scopes can change root directory  
- 🚑 Added recovery menu  
- 💥 Improved Kernel Panic screen  
- 👥 Added support for multi-user sessions  

## 💻 **Commands**
- ⏱️ Added command `time`  

## 📦 **Packages**
- ✅ **Default** — All features  
- 🪶 **Lite** — No ELF emulator and no LuaCanvas  
