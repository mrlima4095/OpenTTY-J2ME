OpenTTY Java Edition
Copyright (C) 2026 - Mr. Lima

---

[ User & Permissions ]

- `su` login interface

[ Package Manager ]

- Multi-sources for different versions
- Feature to block installing of **RISC-V 32 binaries** on OpenTTY builds without emulator

[ VFS ]

- `/boot/` partition
- Improved filesystem mounting 
- File caching disabled

[ Lua ]

- Fixed inverted logic in **Garbage Collector** when reading `free` and `total`

[ ELF ]

- Interface changed to RISC-V 32
- Default memory size decreased to 512kb, setup with `os.request(1, "memory", [size])`

[ MIDlet ]

- Boot Menu
