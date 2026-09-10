# OpenTTY — Terminal Environment & RISC-V ELF Emulator for J2ME

![License](https://img.shields.io/badge/License-MIT-blue.svg)
![GitHub top language](https://img.shields.io/github/languages/top/mrlima4095/OpenTTY-J2ME)
![GitHub release (latest by date)](https://img.shields.io/github/v/release/mrlima4095/OpenTTY-J2ME)
![Platform](https://img.shields.io/badge/Platform-J2ME%20MIDP--2.0-lightgrey.svg)

**OpenTTY** is a complete, miniature operating-system environment that runs on
Java ME (J2ME) mobile devices. It ships as a single MIDlet and bundles:

- a **POSIX-like shell** with a virtual filesystem and process management,
- a **Lua 5.x interpreter** tailored for low-memory devices,
- a **RISC-V RV32IM ELF emulator** with a guest C runtime (`lib32.s` / `libc.s`),
- **graphics**, **network**, and **audio** APIs.

All of it runs inside a constrained CLDC-1.0 / MIDP-2.0 environment, turning
legacy handsets into a portable scripting platform.

---

## Table of Contents

- [Features](#features)
- [Quick Start](#quick-start)
- [File System](#file-system)
- [Shell Commands](#shell-commands)
- [Lua API](#lua-api)
- [RISC-V ELF Emulator](#risc-v-elf-emulator)
- [Package Manager (`yang` / `pkg`)](#package-manager)
- [Security](#security)
- [Building & Installation](#building--installation)
- [Documentation](#documentation)
- [Contributing](#contributing)
- [License](#license)

---

## Features

### Shell & Runtime

- Lua 5.x interpreter with functions, tables, loops, and protected error handling
- Virtual Unix-like filesystem (`/bin`, `/boot`, `/etc`, `/home`, `/lib`, `/mnt`, `/tmp`, `/dev`, `/proc`, `/root`)
- Pipes, redirection, environment variables, and command execution
- Multi-process environment with PID control and permissions

### RISC-V ELF Emulator

- RV32IM ELF executable loading (`ET_EXEC`, `EM_RISCV` 243, 32-bit LE)
- Linux EABI syscall emulation on a 1 MB virtual memory with segment management
- RV32I core + M extension (mul/div/rem), guest C runtime in `res/lib/`
- Dynamic loading of `-shared` libraries (`.so`): `DT_NEEDED`, `.rela.plt`/`.rela.dyn` (RELA), `R_RISCV_JUMP_SLOT`/`R_RISCV_COPY`/`R_RISCV_RELATIVE`

### File System

- Hierarchical Unix-style layout with `/proc` virtual files
- Persistent storage via RecordStore (RMS)
- Real device file-system mounting (`/mnt/`, JSR-75)
- VFS subdirectories under `/bin`, `/etc`, `/lib` that persist across restarts

### Graphical Interface

- LCDUI-based forms, alerts, lists, text boxes, and command handlers
- Custom fonts, layouts, and screen management from Lua

### Network

- TCP/IP client and server sockets
- HTTP/HTTPS client (`socket.http`)
- Inter-process communication via `os.request`

---

## Quick Start

### First Run

1. Install `OpenTTY.jar` on a J2ME-capable device (or run it in an emulator).
2. On first launch you will be asked to create a **username** and **password**.
3. Restart the MIDlet. Subsequent boots auto-log in the created user.

### Hello World

```lua
print("Hello OpenTTY!")

-- Write to a file
local status = io.write("content", "/tmp/test.txt")

-- HTTP request
local response, code = socket.http.get("http://example.com")
print("Code:", code)
print("Response:", response)
```

---

## File System

```
/
├── bin/      # System executables and scripts
├── dev/      # Virtual devices (stdin, stdout, null, random, zero, tty)
├── etc/      # Configuration (fstab, hostname, motd, os-release, vfs.conf)
├── home/     # User files (RMS RecordStores)
├── lib/      # Lua libraries and modules (libcore.so)
├── mnt/      # Real device file system (JSR-75)
├── proc/     # Virtual process/sysinfo files (cpuinfo, meminfo, uptime, <pid>/...)
├── root/     # Root user's protected home
└── tmp/      # Temporary in-memory storage
```

See [File System documentation](docs/FILESYS.md) for details.

---

## Shell Commands

Some of the built-in commands available in the shell:

| Category | Command | Description |
|----------|---------|-------------|
| Process | `ps`, `bg`, `exec`, `kill` | Manage running processes |
| Users | `su`, `whoami`, `logname`, `id` | Switch and inspect users |
| Session | `exit` | Close the MIDlet / terminal |
| Shell | `alias`, `env`/`set`/`export`, `unset`, `eval`, `source`, `builtin` | Manage the shell |
| Files | `pwd`, `cd`, `cat`, `ls`, `touch`, `cp`, `rm`, `mkdir`, `nano` | File operations |
| Network | `curl`, `wget`, `nc`, `ping`, `ifconfig` | Network utilities |
| System | `uptime`, `free`, `uname`, `htop`, `gc`, `warn`, `title` | Inspect and control |
| Utilities | `echo`, `date`, `clear`, `true`, `false` | Basic utilities |
| Package | `yang` / `pkg` | Package manager |

Install additional tools from the app store with `pkg install <name>`
(e.g. `pkg install nano`, `pkg install htop`).

---

## Lua API

### Modules

| Module | Purpose | Selected functions |
|--------|---------|--------------------|
| `os` | System operations | `execute`, `getenv`, `setenv`, `exit`, `date`, `getuid`, `su`, `request`, `setproc`, `getproc`, `mkdir`, `remove` |
| `io` | Input / output | `read`, `write`, `open`, `close`, `dirs`, `popen`, `copy`, `mount` |
| `string` | String manipulation | `upper`, `lower`, `sub`, `find`, `match`, `reverse`, `byte`, `char`, `split`, `hash`, `startswith` |
| `table` | Table manipulation | `insert`, `remove`, `concat`, `sort`, `pack`, `unpack`, `decode` |
| `socket` | Networking | `connect`, `server`, `accept`, `http.get`, `http.post`, `peer`, `device` |
| `graphics` | User interface | `display`, `new`, `append`, `addCommand`, `handler`, `render`, `vibrate` |
| `java` | Java integration | `class`, `getName`, `run`, `delete`, `midlet.*` |
| `base64` | Base64 encoding | `encode`, `decode` |
| `push` | PushRegistry | `register`, `unregister`, `list`, `pending`, `setAlarm`, `getAlarm` |
| `audio` | Audio (MMAPI) | `load`, `play`, `pause`, `volume`, `duration` |

> **Note on `string`:** this implementation does **not** provide `string.format`,
> `string.rep`, `string.gsub`, or `string.gmatch`. Use the native
> `string.startswith` / `string.endswith` rather than reimplementing them.

### UI Example

```lua
local form = graphics.new("form", "My App")

graphics.append(form, { type = "field", label = "Name:", value = "" })
graphics.append(form, { type = "choice", label = "Options:",
                        options = { "A", "B", "C" } })

local save = graphics.new("command", { label = "Save", type = "ok" })
graphics.addCommand(form, save)

graphics.handler(form, { [save] = function() print("Saved!") end })
graphics.display(form)
```

---

## RISC-V ELF Emulator

### Features

- RV32IM ELF executable loading (`ET_EXEC`, `EM_RISCV`, entry ≤ 1 MB)
- RV32I instruction emulation plus M extension (`mul`/`mulh`/`div`/`rem`)
- Linux EABI syscalls + library syscalls (`LIB_BASE` 1000: string/memory/printf/printf/sprintf/malloc/AEABI helpers)
- 1 MB virtual memory with segment management
- Shared-library loading (`.so`): `DT_NEEDED`, RELA relocations, PLT/GOT
- File descriptors and I/O
- Registers (x0–x31, a7 syscall number)

### Supported Syscalls (partial)

`exit`, `fork`, `read`, `write`, `open`, `close`, `creat`, `time`,
`gettimeofday`, `kill`, `getpid`, `getppid`, `getuid`, `brk`, `getcwd`,
`chdir`, `nice`, plus `fstat`/`stat` and network syscalls (`bind`, `listen`,
`accept`, `recvfrom`, `sendto`, ...) under active development.

---

## Package Manager

`yang` (also invoked as `pkg`) installs apps from the on-device app store:

```bash
pkg list                  # List available packages
pkg install nano          # Install a package (requires root)
pkg install *             # Install everything
pkg remove nano           # Remove a package
pkg update                # Check for updates
pkg download nano n.txt   # Download a package without installing
pkg info nano             # Show package information
```

Packages include `nano`, `htop`, `curl`, `wget`, `nc`, `ping`, `find`,
`grep`, `sed`, `viewer`, `x11`, `docker`, and many more.

---

## Security

- **Restricted execution environment** — OpenTTY is a trusted terminal for
  old devices, not a hardened remote-access tool.
- **Password storage** — credentials are hashed and stored in an inaccessible
  file.
- **No encryption** — network traffic is not encrypted; use secure networks or a
  VPN.
- **Multi-user model** — root (UID 0) has full access; regular users (UID 1000+)
  are restricted to their own files and processes.

Read the full [Security policy](SECURITY.md) and [User system docs](docs/USERS.md).

---

## Building & Installation

Detailed instructions are in [docs/BUILD.md](docs/BUILD.md).

Quick overview:

- Build on-device using the J2ME SDK (`http://opentty.fun/dl/SDK.jar`)
- Compiles to `OpenTTY.jar` + `OpenTTY.jad`
- Install directly on Java ME (MIDP-2.0 / CLDC-1.0) devices
- Version: **1.18.1**

---

## Documentation

Comprehensive docs live in [`docs/`](docs/):

- [Overview & usage](docs/README.md)
- [File system](docs/FILESYS.md)
- [User system](docs/USERS.md)
- [Lua reference & examples](docs/lua/README.md)
- [Building from source](docs/BUILD.md)

Also see the [changelog](CHANGELOG.md) and [roadmap](ROADMAP.md).

---

## Contributing

OpenTTY is developed by the community. See [CONTRIBUTING.md](CONTRIBUTING.md)
for guidelines on reporting bugs and submitting pull requests.

**Author:** Mr. Lima

---

## License

[MIT](LICENSE)
