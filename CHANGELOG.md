OpenTTY Java Edition 1.17  
Copyright (C) 2026 - Mr. Lima

---

**OpenTTY is now fully controlled by Lua!** The shell and commands are provided by Lua scripts and binaries in the `/bin/` directory.

---

## 🚀 Key Changes

### 🏗️ **Revolutionized Architecture**
- **Source code reorganized** into specialized modules:
  - `OpenTTY.java` (Main Class)
  - `Lua.java` (Lua Runtime)
  - `ELF.java` (ARM 32-bit Emulator)
- **Removed support for native applications** - Lua is now the primary language
- **X Server is no longer native** - Install via `yang` when needed

### 📁 **Restructured Filesystem**
- **New, optimized directory tree**
- User startup script migrated to Lua (`/home/.initrc`)
- **Updated `/etc/fstab` format**
- Files in `/tmp/` are **automatically removed** when closing the MIDlet

### 🔒 **Enhanced Security**
- **Fixed vulnerability** in accessing the process `owner` field via Lua
- **Refined access control**:
  - Super-user can write to `/bin/`, `/etc/`, and `/lib/`
  - MIDlet login optimized for user prompt

---

## 🛠️ New Lua API

### ✨ **New Functions**
```lua
os.getcwd()      -- Gets current directory
os.getuid()      -- Returns current user ID
string.uuid()    -- Generates unique UUID
graphics.render(file) -- Loads images (new Image type)
```

### 🔄 **Enhanced Functions**
- `require` now searches for modules in `/lib/` in addition to the current directory
- `io.read` and `io.write` require explicit use of `/dev/stdin` and `/dev/stdout`
- `random(max)` moved to `math.random()`
- `os.execute` sends commands directly to the shell
- `socket.peer` and `socket.device` return **IP and Port**

### 🎯 **New Language Features**
- ✅ Support for **methods** using the `:` operator
- ✅ **Metatables** (minimal support)
- ✅ **`break` token** working in `while` and `repeat` loops
- ✅ New standard `java` library for direct JVM access

---

## 💻 Native Shell Commands

### 🔧 **System Management**
| Command | Description |
|---------|-----------|
| `exec [commands...]` | Executes multiple commands |
| `builtin [command...]` | Executes commands ignoring aliases |
| `bg [command...]` | Executes command in another thread |
| `gc` | Runs garbage collector |
| `ps` | Lists active processes |
| `su [password]` | Switches between root/user |
| `exit` | Exits OpenTTY |

### 📊 **System Information**
| Command | Description |
|---------|-----------|
| `whoami` | Current username |
| `id` | ID of logged-in user |
| `uname` | System information |
| `hostname` | Manages machine name |
| `date` | Current date and time |

### 🗂️ **Navigation and Files**
| Command | Description |
|---------|-----------|
| `pwd` | Current directory |
| `cd [path]` | Changes directory |
| `ls` | Lists files |
| `cat [file]` | Displays content |
| `open [uri]` | Opens URI via device API |

### ⚙️ **Configuration and Environment**
| Command | Description |
|---------|-----------|
| `env [key]` | Variable value |
| `set [key]=[value]` | Defines variable |
| `alias [name]=<expression>` | Creates shortcuts |
| `title` | Changes screen title |

### 🎮 **Interface and Utilities**
| Command | Description |
|---------|-----------|
| `xterm` | Main screen |
| `warn [title] [message]` | Custom alert |
| `clear` | Clears terminal |
| `echo [message]` | Displays text |

---

## 📦 Included Programs (`/bin/`)

### 🎯 **Essentials**
- `lua` - Interactive Lua terminal
- `sh` - Main shell
- `yang` - Package manager
- `curl` - HTTP/WebSocket client

### 🛠️ **Utilities**
- `rm` - Removes files/directories
- `touch` - Creates/clears files
- `kill` - Terminates processes
- `nano` - Text editor

---

## 📥 Installable Packages (via `yang`)

### 🔧 **Development**
- `sdk` - Complete Lua SDK
- `jdb` - OpenTTY debugger
- `expr` - Lua expression evaluator
- `nano` - File editor

### 🌐 **Network and Internet**
- `ping` - Connectivity test
- `wget` - File download
- `pastebin` - Pastebin client
- `shprxy` - WebProxy server
- `nc` - Interactive Connection

### 📊 **System and Monitoring**
- `htop` - Memory/process panel
- `sync` - Update checker
- `autogc` - Automatic garbage collector
- `hash` - Hash generator

### 🎨 **Entertainment and Effects**
- `cmatrix` - Matrix effect
- `sed` - String editor
- `find`, `grep` - Advanced search
- `head` - Partial viewer

---

## 💡 Important Notes

### 🚨 **Breaking Changes**
1. **Graphics API removed** (Screen, List, Quest, Canvas) - Use **Lua Graphics API**
2. **Java commands eliminated** - Migrate to Lua solutions
3. **MIDlet startup message** now comes from `/etc/motd`
4. **MIDlet initialized** by `/etc/init` - Errors here cause a crash

### 🔄 **Migration**
- **Lua is now the official language** for OpenTTY development
- Lua services with `--deamon` argument accessible via `arg[1]`
- On devices with limited memory: disable cache and Lua resources
  - `curl -s 1 cache off`

### 🏆 **Highlights**
- ✅ **Font Generator** improved
- ✅ **Standard library** expanded and optimized

---

## 📈 Next Steps

1. **Migrate your scripts** to the new Lua API
2. **Install necessary packages** via `yang install`
3. **Explore the new `graphics` library** for visual interfaces
4. **Enjoy the enhanced performance** of the Lua runtime
