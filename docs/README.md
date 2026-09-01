# OpenTTY — User & Developer Guide

**OpenTTY** is a complete virtual terminal environment for Java ME (J2ME/MIDP)
devices. It implements a miniature operating system — virtual filesystem, Lua
interpreter, process management, graphical interface, and network APIs — inside a
single MIDlet.

This guide covers usage, the Lua API, the graphical interface, networking,
installing programs, and troubleshooting.

---

## Table of Contents

- [Overview](#overview)
- [Getting Started](#getting-started)
- [File System](#file-system)
- [Lua Language](#lua-language)
- [System API](#system-api)
- [Graphical Interface](#graphical-interface)
- [Network Communication](#network-communication)
- [Security and Permissions](#security-and-permissions)
- [Installing Programs](#installing-programs)
- [Debugging and Troubleshooting](#debugging-and-troubleshooting)
- [System Integration](#system-integration)
- [Compatibility and Limitations](#compatibility-and-limitations)

---

## Overview

OpenTTY turns a J2ME device into a portable development and automation
environment, featuring:

- a **virtual filesystem** with `/bin`, `/etc`, `/home`, `/tmp`, `/mnt`, `/dev`, `/proc`
- a **Lua 5.x interpreter** with adapted standard libraries (`os`, `io`, `string`, `table`, ...)
- an **interactive terminal** with command and script execution
- a **graphics API** for building user interfaces (Alert, Form, List, TextBox)
- **network APIs** (HTTP, TCP sockets)
- **process management** with PIDs and permissions
- an **ARM 32-bit ELF emulator** (in development)

---

## Getting Started

### First Startup

On first launch you'll be asked to create a **username** (cannot be `root`) and a
**password** (stored as a hash). Credentials are saved in the `OpenRMS`
RecordStore. After restart, OpenTTY auto-logs in that user.

### Terminal Interface

After login you have access to:

- a command line for executing programs,
- built-in utilities for creating/editing files (`nano`),
- Unix-style file navigation (`ls`, `cd`, `cat`, `mkdir`, `rm`, ...).

### Running Programs

Programs can be:

- **Lua scripts** — interpreted by the Lua runtime,
- **ELF binaries** — executed by the ARM emulator,
- **shell scripts** — implemented in Java.

---

## File System

See the dedicated [File System guide](FILESYS.md) for a complete reference.

```
/
├── bin/   # System applications and commands
├── dev/   # Virtual devices (stdin, stdout, null, random, zero, tty)
├── etc/   # Configuration files (fstab, hostname, motd, os-release)
├── home/  # User data (RMS RecordStores)
├── lib/   # Lua libraries and modules
├── mnt/   # Real device file system (JSR-75)
├── proc/  # Virtual process/sysinfo files
├── root/  # Root user's protected home
└── tmp/   # Temporary in-memory storage
```

---

## Lua Language

### Available Libraries

| Library | Main Functions |
|---------|----------------|
| `os` | `execute`, `getenv`, `setenv`, `exit`, `date`, `getuid`, `su`, `setproc`, `getproc`, `request`, `mkdir`, `remove` |
| `io` | `read`, `write`, `open`, `close`, `popen`, `dirs`, `copy`, `mount` |
| `string` | `sub`, `find`, `match`, `upper`, `lower`, `byte`, `char`, `split`, `hash`, `startswith`, `endswith` |
| `table` | `insert`, `remove`, `sort`, `concat`, `pack`, `unpack`, `decode` |
| `socket` | `connect`, `server`, `accept`, `http.get`, `http.post`, `peer`, `device` |
| `graphics` | `display`, `new`, `append`, `addCommand`, `handler`, `render`, `vibrate` |
| `base64` | `encode`, `decode` |
| `push` | `register`, `unregister`, `list`, `pending`, `setAlarm`, `getAlarm` |
| `audio` | `load`, `play`, `pause`, `volume`, `duration` |

> **Important:** this Lua implementation has a **reduced `string` library**. It
> does **not** provide `string.format`, `string.rep`, `string.gsub`, or
> `string.gmatch`. Use the native `string.startswith`/`string.endswith` instead
> of shadowing them.

See the [Lua reference](lua/README.md) for the full function list and examples.

### Example

```lua
print("Welcome to OpenTTY!")

local files = io.dirs(".")
for i = 1, #files do
    print(i .. ": " .. files[i])
end
```

---

## System API

### Kernel / Process

```lua
print("Uptime ms:", java.midlet.uptime())

os.execute("ps")  -- list processes
os.exit(0)        -- terminate current process
```

### Filesystem Operations

```lua
local hostname = io.read("/etc/hostname")
io.write("Hello", "/tmp/test.txt")

local listing = io.dirs("/bin")
```

---

## Graphical Interface

### Available Components

- `alert` — dialog box
- `form` — form with fields
- `list` — selectable list (implicit/exclusive modes)
- `textbox` — text editor
- `screen` / `buffer` — terminal-style screens
- `command` — action buttons
- `image`, `gauge`, `choice` — additional field types

### Example

```lua
local form = graphics.new("form", "Registration")

graphics.append(form, { type = "field", label = "Name:", length = 50 })
graphics.append(form, {
    type = "choice",
    label = "Options:",
    options = { "Option 1", "Option 2", "Option 3" }
})

local save = graphics.new("command", { label = "Save", type = "ok" })
graphics.addCommand(form, save)

graphics.handler(form, { [save] = function()
    print("Data saved!")
end })

graphics.display(form)
```

---

## Network Communication

### HTTP Client

```lua
local response, code = socket.http.get("http://api.example.com/data")
print("Code:", code)
print("Response:", response)

local result = socket.http.post(
    "http://api.example.com/post",
    "data=value",
    { ["Content-Type"] = "application/x-www-form-urlencoded" }
)
```

### TCP Sockets

```lua
-- Client
local conn, input, output = socket.connect("example.com:80")
io.write("GET / HTTP/1.0\r\n\r\n", output)
local response = io.read(input)
io.close(conn, input, output)

-- Server
local server = socket.server(8080)
print("Server listening on port 8080...")
```

---

## Security and Permissions

- **Root (UID 0)** — full system access
- **User (UID 1000+)** — restricted to their own files and processes
- **Guest** — read-only access to public areas

```bash
su [password]    # become root (asks for password)
sudo <cmd>       # run a command as root
pkg install <p>  # installing packages requires root
```

See the [User System guide](USERS.md) for details.

---

## Installing Programs

Programs can be installed in several ways:

1. **Via package manager** — `pkg install <name>` / `pkg install *`
2. **Via file** — copy `.lua` files to `/bin/`
3. **Via network** — download from an HTTP server with `wget`/`curl`

---

## Debugging and Troubleshooting

### Debug Mode

```lua
os.request(1, "debug", true)
```

### Common Error Codes

| Code | Meaning |
|------|---------|
| 0 | Success |
| 1 | General error |
| 2 | Invalid argument |
| 5 | Read-only storage / operation not permitted |
| 13 | Permission denied |
| 101 | Network / connection error |
| 127 | File or command not found |
| 255 | Command failure (`false`) |

> When the runtime raises a Java exception it carries no message; OpenTTY
> appends a Lua-side traceback pointing at the offending source line.

---

## System Integration

```lua
graphics.vibrate(500)          -- vibrate the device

local vm = java.getName()      -- JVM name
print("Java VM:", vm)

os.open("http://opentty.fun")  -- open external URL
```

---

## Compatibility and Limitations

### Supported Devices

- Java phones (J2ME MIDP-2.0 / CLDC-1.0)
- Emulators (Wireless Toolkit, MicroEmulator, J2ME Loader)
- Tablets and PDAs (depending on the Java implementation)
- Not native Android/iOS

### Known Limitations

- Limited heap (typically 1–4 MB)
- No direct access to specific hardware (camera, GPS)
- Network subject to carrier restrictions
- No encryption for network traffic
- Performance varies by device
