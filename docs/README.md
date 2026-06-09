# 🖥️ OpenTTY ‐ Mobile Terminal Emulator for J2ME  

**OpenTTY** is a complete virtual terminal environment developed for Java mobile devices (J2ME/MIDP). It implements a miniature operating system with virtual filesystem, Lua interpreter, process management, graphical interface, and network APIs — all running within a single MIDlet.

---

## 🚀 **Overview**

OpenTTY transforms your J2ME device into a portable development and automation environment, featuring:

- 📂 **Virtual filesystem** with directories `/bin`, `/etc`, `/home`, `/tmp`, `/mnt`
- 🐧 **Lua 5.x interpreter** with standard libraries (os, io, string, table, math)
- 🖥️ **Interactive terminal** with command and script support
- 🎨 **Graphics API** for interface creation (Alert, Form, List, TextBox)
- 🌐 **Network APIs** (HTTP, TCP sockets)
- 🔧 **Process management** with PID and permissions
- 🔊 **Audio playback** via MMAPI

---

## 📁 **Filesystem Structure**

```
/
├── bin/      # System applications and commands
├── etc/      # Configuration files
├── home/     # User data (RecordStore)
├── lib/      # Lua libraries and modules
├── tmp/      # Temporary files
├── mnt/      # Device's real filesystem
├── dev/      # Virtual devices
│   ├── stdin
│   ├── stdout
│   ├── null
│   └── random
└── proc/     # System information
```

---

## 🎮 **Getting Started**

### 1. **First Startup**
On first launch, you'll be prompted to create:
- 👤 **Username** (cannot be `root`)
- 🔒 **Password** (stored as hash)

### 2. **Terminal Interface**
After login, you have access to:
- **Command line** for executing programs
- **Built-in editor** for creating/editing files
- **File navigation** with Unix-style commands

### 3. **Running Programs**
Programs can be:
- **Lua scripts** – Interpreted by Lua runtime
- **ELF binaries** – Compiled executables
- **Shell script** – Implemented in Java

---

## 📚 **Lua Language in OpenTTY**

### **Available Libraries**

| Library | Main Functions |
|---------|----------------|
| `os` | `execute`, `getenv`, `setenv`, `clock`, `exit`, `date` |
| `io` | `read`, `write`, `open`, `close`, `popen`, `dirs` |
| `string` | `sub`, `find`, `match`, `upper`, `lower`, `byte`, `char` |
| `table` | `insert`, `remove`, `sort`, `concat`, `pack`, `unpack` |
| `math` | `random` |
| `socket` | `connect`, `server`, `accept`, `http.get`, `http.post` |
| `graphics` | `display`, `new`, `append`, `handler`, `vibrate` |
| `audio` | `load`, `play`, `pause`, `volume`, `duration` |

### **Lua Script Example**
```lua
-- Hello World in OpenTTY
print("Welcome to OpenTTY!")

-- List files in current directory
local files = io.dirs(".")
for i = 1, #files do
    print(i .. ": " .. files[i])
end

-- Create a graphical interface
local previous = graphics.getCurrent()
local screen = graphics.new("form", "My App")
local back = graphics.new("command", { label = "Back", type = "back priority = 1 })
graphics.append(screen, {type="text", label="Name:", value=""})
graphics.addCommand(screen)
graphics.handler(screen, { [back] = function() graphics.display(previous) end })
graphics.display(screen)
```

---

## 🔧 **System API**

### **Kernel Commands**
```lua
-- Access kernel functions
local kernel = java.midlet.uptime()
print("Uptime: " .. kernel .. " ms")

-- Process management
os.execute("ps")  -- List processes
os.exit(0)        -- Terminate current process
```

### **Filesystem Operations**
```lua
-- Read file
local machine_name = io.read("/etc/hostname")

-- Write file
io.write("Hello", "/tmp/test.txt")

-- List directory
local listing = io.dirs("/bin")
```

---

## 🎨 **Creating Graphical Interfaces**

### **Available Components**
- `alert` – Dialog box
- `form` – Form with fields
- `list` – Selectable list
- `textbox` – Text editor
- `command` – Action buttons

### **Interface Example**
```lua
local form = graphics.new("form", "Registration")
graphics.append(form, {
    type = "field",
    label = "Name:",
    length = 50
})
graphics.append(form, {
    type = "choice",
    label = "Options:",
    options = {"Option 1", "Option 2", "Option 3"}
})

-- Add button
local cmd = graphics.new("command", {label="Save", type="ok"})
graphics.addCommand(form, cmd)

-- Define actions
graphics.handler(form, {
    [cmd] = function(args)
        print("Data saved!")
    end
})

graphics.display(form)
```

---

## 🌐 **Network Communication**

### **HTTP Client**
```lua
-- GET request
local response, code = socket.http.get("http://api.example.com/data")
print("Code: " .. code)
print("Response: " .. response)

-- POST request
local result = socket.http.post(
    "http://api.example.com/post",
    "data=value",
    {["Content-Type"] = "application/x-www-form-urlencoded"}
)
```

### **TCP Sockets**
```lua
-- TCP client
local conn, input, output = socket.connect("example.com:80")
io.write("GET / HTTP/1.0\r\n\r\n", output)
local response = io.read(input)
io.close(conn, input, output)

-- TCP server
local server = socket.server(8080)
print("Server listening on port 8080...")
```

---

## 🔐 **Security and Permissions**

### **Access Levels**
- **Root (UID 0)** – Full system access
- **User (UID 1000)** – Restricted to own files
- **Guest** – Read-only access to public areas

### **Privileged Commands**
```bash
# Become root (asks for password)
su [password]
# Or use sudo to run a specified program as root
sudo []
```

---

## 📦 **Installing Programs**

### **Installation Methods**
1. **Via File** – Copy `.lua` files to `/bin/`
2. **Via RMS** – Use internal storage system
3. **Via Network** – Download from HTTP server

---

## 🐛 **Debugging and Troubleshooting**

### **Debug Mode**
```lua
-- Enable detailed logs
os.request(1, "debug", true)
```

### **Common Error Codes**
| Code | Meaning |
|------|---------|
| 0 | Success |
| 1 | General error |
| 2 | Invalid argument |
| 5 | Operation not permitted |
| 13 | Permission denied |
| 101 | Network error |
| 127 | File not found |

---

## 🔄 **System Integration**

### **Accessing Device Resources**
```lua
-- Vibrate
graphics.vibrate(500)

-- Get system properties
local vm = java.getName()
print("Java VM: " .. vm)

-- Open external URLs
os.open("http://opentty.fun")
```

---

## 📊 **Limitations and Compatibility**

### **Supported Devices**
- ✅ Java phones (J2ME MIDP 2.0)
- ✅ Emulators (Wireless Toolkit, MicroEmulator)
- ⚠️ Tablets and PDAs (depends on Java implementation)
- ❌ Android/iOS (native)

### **Known Limitations**
- Limited memory (1-4MB heap typical)
- No access to specific hardware (camera, GPS)
- Network subject to carrier restrictions
- Performance varies by device
