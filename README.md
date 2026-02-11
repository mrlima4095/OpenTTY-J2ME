# 🖥️ OpenTTY

![License](https://img.shields.io/badge/License-MIT-blue.svg) ![GitHub top language](https://img.shields.io/github/languages/top/mrlima4095/OpenTTY-J2ME) ![GitHub release (latest by date)](https://img.shields.io/github/v/release/mrlima4095/OpenTTY-J2ME)


**OpenTTY** — a lightweight, MIDlet-based terminal / shell environment inspired by classic Unix tools, written in Java for J2ME devices.  
It exposes a SandBox with Lua interpreter and an ARM 32 ELF emulator (in development), enabling scripting and native binary execution in constrained mobile environments.

---

## 🌟 Key Features

### 🐚 **Integrated Shell**
- Lua 5.x interpreter with support for functions, tables, loops, and error handling
- Virtual Unix-like filesystem structure (`/bin`, `/etc`, `/home`, `/lib`, `/mnt`, `/tmp`)
- Support for pipes, redirection, and command execution
- Multi-process environment with PID control

### 🏗️ **ARM ELF Emulator**
- 32-bit ARM executable (ELF) emulator
- Linux ARM syscall implementation (EABI)
- 1MB virtual memory with segment management
- Support for basic ARM instructions and syscalls

### 📂 **File System**
- Hierarchical Unix-style system
- Persistent storage support via RecordStore
- Real device filesystem mounting (`/mnt/`)
- Caching system for better performance

### 🎨 **Graphical Interface**
- Integrated LCDUI display
- Forms, alerts, lists, and input fields
- Custom font and layout support
- Event and command system

### 🔌 **Network and Connectivity**
- TCP/IP socket support
- HTTP/HTTPS client
- Network connections management
- Inter-process communication

---

## 🗂️ Directory Structure

```
/
├── 📁 bin/            # Executables and scripts
│   ├── 📄 cp          # Copy files
│   ├── 📄 curl        # HTTP client
│   ├── 📄 init        # Initialization script
│   ├── 📄 kill        # Kill processes
│   ├── 📄 lua         # Lua interpreter
│   ├── 📄 nano        # Text editor
│   ├── 📄 rm          # Remove files
│   ├── 📄 sh          # Basic shell
│   ├── 📄 touch       # Create files
│   └── 📄 yang        # Package manager
├── 📁 dev/            # Devices
│   ├── 📄 null        # Null device
│   ├── 📄 random      # Random number generator
│   ├── 📄 stdin       # Standard input
│   ├── 📄 stdout      # Standard output
│   └── 📄 zero        # Zero device
├── 📁 etc/            # Configuration
│   ├── 📄 fstab       # Filesystem table
│   ├── 📄 hostname    # Host name
│   ├── 📄 motd        # Initial message
│   └── 📄 os-release  # Release information
├── 📁 home/           # User files
├── 📁 lib/            # Libraries
│   └── 📄 libcore.so  # System core library
├── 📁 mnt/            # Mount points
└── 📁 tmp/            # Temporary files
└── 📁 proc/           # Process files
```

---

## 🚀 Quick Start

### 1. **First Execution**
- On first run, credential creation will be requested
- Set up username and password
- Restart MIDlet after configuration

### 2. **Commands**
# 📋 OpenTTY Shell Commands Table

| Category | Command | Description | Usage Example | Status Codes |
|----------|---------|-------------|---------------|--------------|
| **Process Management** | `ps` | List running processes | `ps` | Always 0 |
| | `bg` | Run command in background | `bg sleep 5` | Command exit code |
| | `exec` | Execute multiple commands | `exec ls pwd whoami "echo gg"` | Last command exit code |
| **Permissions & Users** | `su` | Switch user (to root or other) | `su` or `su root password` | 0=Success, 13=Permission denied |
| | `whoami` | Show current username | `whoami` | Always 0 |
| | `logname` | Show login name | `logname` | Always 0 |
| | `id` | Show user ID | `id` | Always 0 |
| **Session Control** | `exit` | Exit/close the MIDlet | `exit` | Terminates session |
| **Shell Management** | `alias` | Create/view shell aliases | `alias ll='ls -l'` or `alias` | 127=Alias not found |
| | `unalias` | Remove shell aliases | `unalias ll` | 127=Alias not found |
| | `env` | Set/view environment variables | `env PATH=/bin` or `env` | 127=Var not found |
| | `set` | Same as `env` | `set` | 127=Var not found |
| | `export` | Same as `env` | `export` | 127=Var not found |
| | `unset` | Remove environment variables | `unset PATH` | Always 0 |
| **Shell Utilities** | `eval` | Evaluate shell command string | `eval "echo hello"` | Command exit code |
| | `echo` | Print text to stdout | `echo "Hello World"` | Always 0 |
| | `date` | Show current date/time | `date` | Always 0 |
| | `clear` | Clear the screen | `clear` | Always 0 |
| | `builtin` | Execute builtin ignoring aliases | `builtin ls` | Command exit code |
| | `command` | Same as `builtin` | `command ls` | Command exit code |
| | `source` | Execute commands from file | `source script.sh` | Last command exit code |
| **File System** | `pwd` | Print working directory | `pwd` | Always 0 |
| | `cd` | Change directory | `cd /home/` | 0=Success, 127=Not found, 20=Not a directory |
| | `cat` | Display file contents | `cat /etc/motd` | 127=File not found |
| | `ls` | List directory contents | `ls` or `ls /home/` | Always 0 |
| | `open` | Open file/connection | `open http://example.com` | 127=Not found, 1=Error |
| **System Info** | `uptime` | Show system uptime | `uptime` | Always 0 |
| **Window/Graphics** | `xterm` | Switch to default terminal screen | `xterm` | Always 0 |
| | `warn` | Display alert dialog | `warn "Title" "Message"` | Always 0 |
| | `title` | Change window title | `title "My Terminal"` | Always 0 |
| | `buff` | Set stdin buffer text | `buff "command to run"` | Always 0 |
| **System Control** | `gc` | Run garbage collector | `gc` | Always 0 |
| **Placeholders** | `true` | Always succeeds (no-op) | `true` | Always 0 |
| | `false` | Always fails | `false` | Always 255 |
| **Comment** | `#` | Comment (ignored) | `# This is a comment` | Always 0 |
| **Special** | `.` | Execute program (current dir) | `. program` | Program exit code |
| **Redirection** | `>` | Redirect output to file | `echo hello > file.txt` | Command exit code |

### 3. **Lua Script Examples**
```lua
-- Hello World
print("Hello OpenTTY!")

-- File manipulation
local file = io.write("content", "/tmp/test.txt")

-- HTTP request
local response, code = socket.http.get("http://example.com")
print("Code:", code)
print("Response:", response)
```

---

## 🛠️ Lua API

### 📦 **Available Modules**

| Module | Description | Main Functions |
|--------|-----------|-------------------|
| `os` | System operations | `execute`, `getenv`, `setenv`, `exit`, `date` |
| `io` | Input/Output | `read`, `write`, `open`, `close`, `dirs` |
| `string` | String manipulation | `upper`, `lower`, `sub`, `find`, `match` |
| `table` | Table manipulation | `insert`, `remove`, `concat`, `sort` |
| `socket` | Network and sockets | `connect`, `http.get`, `http.post` |
| `graphics` | Graphical interface | `display`, `new`, `append`, `handler` |
| `java` | Java integration | `class`, `getName`, `run`, `thread` |
| `base64` | Base64 API | `encode`, `decode` |
| `push` | PushRegistry | `register`, `unregister`, `list`, `pending`, `setAlarm`, `getAlarm` |

---

## ⚙️ ARM ELF Emulator

### 🎯 **Features**
- ✅ 32-bit ARM ELF executable loading
- ✅ Basic ARM instruction emulation
- ✅ Linux ARM syscalls (EABI)
- ✅ Memory management (1MB)
- ✅ File descriptors and I/O
- ✅ Registers and CPSR flags

### 🔌 **Supported Syscalls**
- `exit`, `fork`, `read`, `write`
- `open`, `close`, `creat`
- `time`, `gettimeofday`, `kill`
- `getpid`, `getppid`, `getuid`
- `brk`, `getcwd`, `chdir`

---

## 📡 Network and Communication

### 🌐 **Supported Protocols**
- **HTTP/HTTPS**: GET, POST, custom headers
- **TCP Sockets**: Client and server
- **Socket Streams**: Asynchronous read/write

### 🔗 **Connection Example**
```lua
-- HTTP client
local response, code = socket.http.get("http://api.example.com/data")

-- TCP Socket
local conn, input, output = socket.connect("example.com:80")
io.write("GET / HTTP/1.0\r\n\r\n", output)
local response = io.read(input, 4096)
io.close(conn, output, input)
```

---

## 🎨 Graphical Interface

### 🖼️ **Available Components**
- `Form`: Forms with multiple items
- `Alert`: Dialog boxes
- `List`: Selectable lists
- `TextBox`: Text input fields
- `StringItem`: Formatted text items
- `Image`: Image display

### 🎮 **UI Example**
```lua
-- Create form
local form = graphics.new("screen", "My App")

-- Add components
graphics.append(form, {
    type = "text",
    label = "Name:",
    value = "Enter your name"
})

graphics.append(form, {
    type = "field",
    label = "Password:",
    mode = "password"
})

-- Display
graphics.display(form)
```

---

## 🔒 Security and Permissions

### 👤 **User System**
- Root user (UID 0) with full privileges
- Normal users (UID 1000+) with restrictions
- File and process access control
- Password authentication system

### 🛡️ **Protections**
- Process sandboxing
- Syscall validation
- Filesystem access control
- Resource limits per process

---

## ⚡ Performance

### 🚀 **Optimizations**
- Lua token caching for frequent scripts
- Shared memory between processes
- Efficient J2ME resource management
- Configurable garbage collection

### 📊 **Monitoring**
```lua
-- Memory status
local free = collectgarbage("free")     -- Free memory (KB)
local total = collectgarbage("total")   -- Total memory (KB)
local used = collectgarbage("count")    -- Used memory (KB)

-- System information
print("Uptime:", java.midlet.uptime())
print("Build:", java.midlet.build)
```

---

## 🔄 Updates and Maintenance

### 📦 **Package System**
```bash
# Update mirrors
yang update

# Change to root
su [password]

# Install package
yang install package

# Remove package
yang remove package

# List installed packages
yang list
```

## 📚 Additional Resources

### 🎓 **Learn More**
- [Lua 5.1 Documentation](https://www.lua.org/manual/5.1/)
- [ELF Specification](https://refspecs.linuxfoundation.org/elf/elf.pdf)
- [ARM Architecture Reference](https://developer.arm.com/documentation/ddi0406/latest/)

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
