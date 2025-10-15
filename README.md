# 🖥️ OpenTTY

![License](https://img.shields.io/badge/License-MIT-blue.svg) ![GitHub top language](https://img.shields.io/github/languages/top/mrlima4095/OpenTTY-J2ME) ![GitHub release (latest by date)](https://img.shields.io/github/v/release/mrlima4095/OpenTTY-J2ME)


**OpenTTY** — a lightweight, MIDlet-based terminal / shell environment inspired by classic Unix tools, written in Java for J2ME devices.  
It exposes a shell-like interface, a small process manager, a simple file API (RMS + mountable roots), networking utilities (bind, port scanner, gobuster, net tools), a logging manager, and Lua programming language support.

---

## 🌐 Overview
OpenTTY provides an interactive shell and a small runtime for running scripts and lightweight services on constrained Java ME devices.  
It mixes traditional shell primitives (aliases, environment variables, `sh`/`login`), a MIDlet UI (xterm/x11 modes), process tracking, simple file abstractions (RMS and mount points), and a handful of networking and inspection utilities.

---

## ⚙️ Basic Commands
Below are commonly used commands. See the in-app `man` / `help` for details.

- `sh`, `login` — start shell or run a script.  
- `exit`, `logout`, `quit` — leave session / close app.  
- `env`, `set`, `unset`, `export` — manage environment variables.  
- `alias`, `unalias` — create / remove aliases.  
- `ls`, `pwd`, `cd`, `dir` — filesystem navigation & listing.  
- `nano` — text editor.  
- `ps`, `start`, `stop`, `kill`, `top` — process management and inspection.  
- `log`, `logcat` — logging manager and dump.  
- `wget`, `curl` — HTTP fetch tools.  
- `lua` — run Lua scripts (if Lua support is built in).

---

## 🌱 Environment Variables
OpenTTY stores environment-like keys in an internal attributes table. Use:

- `set KEY=value` — set a variable.  
- `unset KEY` — remove it.  
- `env` — list variables.  
- `export` — shorthand to set or print environment items.  

Common internal variables include `$VERSION`, `$HOSTNAME`, `$TYPE`, `$LOCALE`, and build-related keys.

---

## 🧩 Exit Codes
Standard exit codes used across OpenTTY subsystems:

| Code | Meaning |
|------|----------|
| `0` | Success |
| `1` | General I/O or runtime error |
| `2` | Missing argument / bad usage |
| `3` | Unsupported API or feature not available |
| `5` | Tried to write on a read-only storage |
| `13` | Permission denied / security exception |
| `68` | Service is already running |
| `69` | Service is unavailable |
| `101` | Network related error |
| `127` | Not found |
| `128–254` | Reserved for subsystem-specific errors |
| `255` | Caused by command `false` |

---

## 🧠 Process System
OpenTTY implements a lightweight process table (`trace`), with a PID generator and utilities to start/stop/kill processes.

- `start <app>` — allocates a PID and spawns a process.  
- `stop <name>` — stops a process by name.  
- `kill <PID>` — terminates a process by PID.  
- `ps` — prints running processes.  
- `top` / `trace` — opens process and memory monitor.

Each process stores metadata such as owner, collector, I/O streams, and screen handles.

---

## 📂 File System
OpenTTY exposes several storage layers:

- **RMS (`/home/`)** — local RecordStore storage (e.g., `/home/nano`, `/home/man.html`).  
- **`/mnt/`** — mount points for the device file system.  
- **`/tmp/`** — temporary in-memory store.  

### Commands:
`mount`, `umount`, `mkdir`, `cp`, `rm`, `touch`, `ls`, `fdisk`, `lsblk`, `dir`

Some operations may be restricted by the MIDlet sandbox.

---

## 🧰 Utilities

### 🔗 Bind (server)
`bind <port> [db] [proc_name]` — opens a listening socket on the specified port and manages connections as processes.

### 🚪 Port Scanner
`prscan <host> [start]` — scans TCP ports and lists open ones.

### 🕵️ GoBuster
`gobuster <host> [wordlist]` — performs HTTP wordlist enumeration and lists valid paths.

### 📜 History
`history` — opens the command history UI, allowing you to rerun or edit past commands.

### 🗂️ File Explorer
`dir` — graphical file browser for RMS, `/mnt/`, and `/tmp/`.

---

## 📋 Logs Management
OpenTTY includes a built-in logging manager.

- `log add <level> <message>` — append a log entry (`info`, `warn`, `debug`, `error`).  
- `log view` — view current logs.  
- `log swap <name>` — archive current log buffer.  
- `log clear` — clear logs.  
- `logcat` — dump logs to stdout.

Logs can also be managed via `MIDletLogs(...)` when extending subsystems.

---

## 🐍 Lua Scripting
OpenTTY can execute Lua scripts when the Lua bridge is enabled.

Use:
```sh
lua [file]
lua -e "print('Hello, Lua!')"
````

> **Note:**
> If Lua is not built into the current MIDlet version, it will return an unsupported feature error.
> For learning Lua, read the official documentation: [https://www.lua.org/manual/](https://www.lua.org/manual/)

---

## 🛠️ Build & Installation

For detailed build instructions and installation guide, check out our complete documentation:

📖 **[Build Documentation Wiki](https://github.com/mrlima4095/OpenTTY-J2ME/wiki/%F0%9F%9A%80-Build-Documentation)**

*Quick overview:*
- Built using [J2ME SDK Mobile](http://opentty.xyz/dl/SDK.jar)
- Compiles to `OpenTTY.jar` and `OpenTTY.jad` files
- Direct installation on Java ME compatible devices
- Supports various mobile platforms with J2ME runtime

---

## 🤝 Contributing & Collaborators
OpenTTY is actively developed by the community.
If you want to contribute, open issues or pull requests on GitHub.

**Author:** Mr. Lima

---
