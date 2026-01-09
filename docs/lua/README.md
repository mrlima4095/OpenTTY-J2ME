# 🌙 Lua J2ME ‐ Documentation

This is a complete implementation of the Lua language for the J2ME (Java Micro Edition) platform, designed to work on mobile devices with limited resources. The implementation includes a parser, interpreter, and various standard libraries adapted for the J2ME environment.

## ✨ Key Features

- **Complete Language**: Supports most Lua language concepts
- **J2ME Optimized**: Adapted for memory-limited mobile devices
- **Native Libraries**: Includes adapted versions of standard Lua libraries
- **Graphical Interface**: Integration with J2ME UI (Display, Form, List, etc.)
- **Networking**: HTTP, TCP/IP socket support
- **File System**: File system access via RMS

## 📊 Comparison Table: Original Lua vs Lua J2ME

| Feature | Original Lua 🐧 | Lua J2ME 📱 |
|---------|------------------|--------------|
| **Platform** | Cross-platform | J2ME only |
| **Memory** | No restrictions | Limited (mobile devices) |
| **Libraries** | Complete | Adapted/Reduced |
| **UI** | Terminal/Console | J2ME Display (Form, List, Alert) |
| **Networking** | Full socket support | Basic HTTP, Socket |
| **File System** | Complete | RMS (Record Management System) |
| **Threading** | Full support | Limited by J2ME |
| **Garbage Collection** | Advanced | Basic (System.gc()) |

## 🛠️ Native Functions Implemented

### 🌍 Global Functions

| Function | Description |
|----------|-------------|
| `print(...)` | Output text to console |
| `error(msg)` | Throws error with message |
| `pcall(f, ...)` | Protected function call |
| `require(mod)` | Loads Lua modules |
| `load(string)` | Loads Lua code |
| `pairs(t)` | Iterates over tables |
| `ipairs(t)` | Advanced iterates over tables |
| `collectgarbage(opt)` | Controls garbage collection |
| `tostring(v)` | Converts to string |
| `tonumber(v)` | Converts to number |
| `select(index, ...)` | Selects arguments |
| `type(v)` | Returns value type |
| `getAppProperty(key)` | Gets application properties |

### 💻 OS Library

| Function | Description |
|----------|-------------|
| `os.execute(cmd)` | Executes system commands |
| `os.getenv(var)` | Gets environment variables |
| `os.clock()` | Program execution time |
| `os.setlocale(loc)` | Sets locale |
| `os.exit(code)` | Terminates execution |
| `os.date()` | Current date/time |
| `os.getpid()` | Process ID |
| `os.setproc(attr, val)` | Sets process properties |
| `os.getproc(pid, field)` | Gets process information |
| `os.getcwd()` | Current directory |
| `os.chdir(path)` | Change current directory |
| `os.request(pid, payload, args)` | Make a request to a Service |
| `os.getuid(procname)` | Get PID of a process by name|
| `os.open(uri)` | Request Device API to perform a request to URI |
| `os.scope(...)` | Change or get scope |
| `os.su(user, password)` | Change current user |
| `os.remove(file)` | Remove files and directories |
| `os.join(file)` | Get absolute path of a file |

### 📁 IO Library

| Function | Description |
|----------|-------------|
| `io.read([source])` | Reads from file/stream |
| `io.write(data, [target])` | Writes to file/stream |
| `io.close(stream)` | Closes stream |
| `io.open(file)` | Opens file |
| `io.popen(program, args, sudo, stdout, scope)` | Run program |
| `io.dirs(path)` | Get contents of directory |
| `io.setstdout(file)` | Change program stdout |
| `io.mount(strcut)` | Mount a file system struct |
| `io.copy(stream, file)` | Copy a stream to a file |


### 🔤 String Library

| Function | Description |
|----------|-------------|
| `string.upper(s)` | Converts to uppercase |
| `string.lower(s)` | Converts to lowercase |
| `string.len(s)` | String length |
| `string.find(s, pattern)` | Finds pattern in string |
| `string.match(s, pattern)` | Matches pattern in string |
| `string.reverse(s)` | Reverses string |
| `string.sub(s, i, j)` | Substring |
| `string.hash(s)` | Hash code |
| `string.byte(s, i, j)` | Converts to bytes |
| `string.char(...)` | Converts bytes to string |
| `string.trim(s)` | Trims extra spaces |
| `string.uuid()` | Generates UUID |
| `string.split(s, char)` | Splits string |
| `string.getCommand(s)` | Get first part of a string |
| `string.getArgument(s)` | Get argument part of string |
| `string.env(s)` | Decodes string with environment keys |


### 🗂️ Table Library

| Function | Description |
|----------|-------------|
| `table.insert(t, [pos], value)` | Inserts element into table |
| `table.concat(t, [sep], [i], [j])` | Concatenates elements |
| `table.remove(t, [pos])` | Removes element |
| `table.sort(t)` | Sorts table |
| `table.move(t, f, t, len)` | Moves elements |
| `table.unpack(t, [i], [j])` | Unpacks table |
| `table.pack(...)` | Packs arguments |
| `table.decode(str)` | Decodes string to table |

### 🎨 Graphics Library

| Function | Description |
|----------|-------------|
| `graphics.display(screen)` | Set current screen |
| `graphics.new(type, title, ...)` | Create new screen object |
| `graphics.SetTitle(screen, title)` | Change screen title |
| `graphics.SetTicker(screen, ticker)` | Change screen ticker |
| `graphics.SetLabel(field, text)` | Change field label |
| `graphics.GetLabel(field)` | Get field label |
| `graphics.GetText(field)` | Get field text |
| `graphics.getCurrent()` | Get current screen |
| `graphics.render(file)` | Render Image from file |
| `graphics.append(screen, field)` | Appends file in screen |
| `graphics.clear(screen)` | Clear screens fields |
| `graphics.addCommand(screen, command)` | Add commands in screen |
| `graphics.handler(screen, {})` | Set screen buttons handler |
| `graphics.db` | Screens database |


### 🌐 Socket Library

| Function | Description |
|----------|-------------|
| `socket.connect(url)` | TCP connection |
| `socket.peer(conn)` | Peer address |
| `socket.device(conn)` | Local address |
| `socket.server(port)` | Socket server |
| `socket.accept(server)` | Accepts connection |
| `socket.http.get(url, headers)` | HTTP GET request |
| `socket.http.post(url, data, headers)` | HTTP POST request |

### ☕ Java Library

| Function | Description |
|----------|-------------|
| `java.class(name)` | Checks if class exists |
| `java.getName()` | JVM name |
| `java.run(function, name)` | Run function in another thread (Background) |
| `java.getName()` | JVM name |
| `java.delete(table, field)` | Delete a field from table in Java |
| `java.midlet.username` | Default user name |
| `java.midlet.cache` | Cached files in MIDlet |
| `java.midlet.build` | MIDlet Build Code |
| `java.midlet.uptime` | MIDlet uptime |

### PushRegistry

| Function | Description |
|----------|-------------|
| `push.register(connection, filter, midlet, sender)` | Register Connection |
| `push.unregister(connection)` | Unregister Connection |
| `push.list(filter)` | List connections by filter or `*` for all |
| `push.pending()` | Check if has conncetions active |
| `push.setAlarm(midletClass, time)` | Set an alarm to MIDlet |
| `push.getAlarm(midletClass)` | View an alarm |


## 📝 Usage Example

```lua
-- Hello World
print("Hello Lua J2ME World!")

-- Table manipulation
local t = { 1, 2, 3, name = "Lua" }
table.insert(t, 4)
print(table.concat(t, ", "))

-- Graphical interface
local form = graphics.new("form", "My App")
graphics.append(form, "Welcome to Lua J2ME")

local cmd = graphics.new("command", { label = "Save", type = "ok" })
graphics.addCommand(form, cmd)

graphics.handler(form, {
    [cmd] = function()
        print("button pressed")
    end
})

graphics.display(form)
```

### 🎨 Basic Graphics Example
```lua
-- Simple alert
graphics.display(graphics.new("alert", "Welcome", "Hello Lua J2ME!"))

-- Form with multiple components
local form = graphics.new("form", "My Form")
graphics.append(form, { type = "text", value = "Welcome to my app!", layout = "default" })
graphics.append(form, { type = "image", img = "/icon.png" })
graphics.append(form, { type = "field", label = "Username:", value = "" })
graphics.append(form, { type = "choice", label = "Options:", mode = "exclusive", options = { "Option 1", "Option 2", "Option 3"} })
graphics.append(form, { type = "gauge", label = "Progress:", interactive = false, max = 100, value = 50 })

local back = graphics.new("command", { label = "Back", type = "back", priority = 1 })
local submit = graphics.new("command", { label = "Submit", type = "ok", priority = 1 })
graphics.addCommand(form, back)
graphics.addCommand(form, submit)

graphics.display(form)
```

### 📋 List Example
```lua
local list = graphics.new("form", "Main Menu", "implicit")
graphics.append(list, "Settings")
graphics.append(list, "Games")
graphics.append(list, "Tools")
graphics.append(list, "Exit")

local select = graphics.new("command", { label = "Select", type = "ok", priority = 1 })
graphics.addCommand(list, select)
graphics.handler(list, {
    [select] = function (option) 
        print("Selected: " .. option)
    end
})

graphics.display(list)
```

## ⚠️ Limitations

- **Performance**: Slower than native Lua due to JVM
- **Memory**: Severe memory constraints
- **Libraries**: Reduced functionality compared to full Lua
- **Platform**: Limited to J2ME ecosystem

## 🎯 Conclusion

Lua J2ME is an impressive implementation that brings the power of Lua language to older mobile devices with J2ME. Although it has limitations compared to the original implementation, it offers a robust solution for scripting in resource-limited environments.