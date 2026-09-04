# Lua for J2ME — Reference

This is a full implementation of the **Lua language for J2ME (CLDC)**, designed
to run on memory-limited mobile devices. It includes a parser, an interpreter,
and standard libraries adapted for the J2ME environment.

## Highlights

- **Complete language**: functions, tables, loops, and protected error handling
  (`pcall`, `xpcall`-style tracebacks)
- **J2ME optimized**: tuned for low-heap devices
- **Native libraries**: adapted versions of `os`, `io`, `string`, `table`, ...
- **Graphics**: full integration with J2ME UI (Form, List, Alert, TextBox)
- **Networking**: HTTP and TCP/IP sockets
- **File system**: access via RMS / virtual filesystem

## Comparison: Original Lua vs Lua J2ME

| Feature | Original Lua | Lua J2ME |
|---------|--------------|----------|
| Platform | Cross-platform | J2ME only |
| Memory | No restrictions | Limited (1–4 MB heap typical) |
| Libraries | Complete | Adapted / reduced |
| UI | Terminal / console | J2ME Display (Form, List, Alert) |
| Networking | Full socket support | Basic HTTP + socket |
| File system | Complete | Virtual FS / RMS |
| Threading | Full support | Limited by J2ME |
| Garbage collection | Advanced | Basic |

---

## Global Functions

| Function | Description |
|----------|-------------|
| `print(...)` | Output text to stdout |
| `error(msg)` | Throw an error with a message |
| `assert(v [, msg])` | Raise an error if `v` is falsy, return all args otherwise |
| `pcall(f, ...)` | Call a function in protected mode |
| `require(mod)` | Load a Lua module |
| `load(string)` | Load and compile Lua code |
| `pairs(t)` | Iterate over table key/value pairs |
| `ipairs(t)` | Iterate over an array part |
| `collectgarbage(opt)` | Control garbage collection |
| `tostring(v)` | Convert a value to string |
| `tonumber(v[, base])` | Convert a value to number |
| `select(index, ...)` | Select arguments |
| `type(v)` | Return the value's type |
| `getAppProperty(key)` | Get an application property |

## `os` Library

| Function | Description |
|----------|-------------|
| `os.execute(cmd)` | Execute a system command |
| `os.request(pid, payload, args)` | Send a request to a service (kernel) |
| `os.getenv(var)` / `os.setenv(var, val)` | Get / set environment variables |
| `os.exit(code)` | Terminate the current process |
| `os.date()` / `os.clock()` | Current date / elapsed time |
| `os.getpid()` | Current process ID |
| `os.setproc(attr, val)` | Set process properties (e.g. `name`) |
| `os.getproc(pid, field)` | Get process information |
| `os.getuid([procname])` | Get a user ID or a PID by process name |
| `os.getcwd()` / `os.chdir(path)` | Get / change working directory |
| `os.open(uri)` | Ask the device to open a URI |
| `os.scope(...)` | Get or change scope |
| `os.su(user, password)` | Switch the current user |
| `os.remove(file)` | Remove a file or directory |
| `os.join(file)` | Get the absolute path of a file |
| `os.mkdir(dir)` | Create a directory |

## `io` Library

| Function | Description |
|----------|-------------|
| `io.read([source])` | Read from a file, stream, or `-1` to EOF |
| `io.write(data, [target])` | Write to a file / stream |
| `io.close(stream)` | Close a stream |
| `io.open(file)` | Open a file |
| `io.popen(program, args, sudo, stdout, scope)` | Run a program |
| `io.dirs(path)` | List the contents of a directory |
| `io.setstdout(file)` | Redirect the program's stdout |
| `io.mount(struct)` | Mount a filesystem structure |
| `io.copy(stream, file)` | Copy a stream to a file |

> `io.dirs(path)` returns entries only for `/tmp/`, `/mnt/<sub>`, and exactly
> `/bin/`, `/etc/`, `/lib/`, `/home/`. Any other path yields an empty table.

## `string` Library

> **Reduced library.** The following functions are **not** available:
> `format`, `rep`, `gsub`, `gmatch`. Use the native `startswith` / `endswith`
> instead of reimplementing them.

| Function | Description |
|----------|-------------|
| `string.upper(s)` / `string.lower(s)` | Case conversion |
| `string.len(s)` | String length |
| `string.find(s, pattern)` | Find a pattern |
| `string.match(s, pattern)` | Match a pattern |
| `string.reverse(s)` | Reverse a string |
| `string.sub(s, i, j)` | Substring |
| `string.hash(s)` | Hash code |
| `string.byte(s[, i[, j]])` | Convert to byte values |
| `string.char(...)` | Convert byte values to a string |
| `string.trim(s)` | Trim surrounding whitespace |
| `string.uuid()` | Generate a UUID |
| `string.split(s, char)` | Split a string |
| `string.getCommand(s)` | Get the first token of a string |
| `string.getArgument(s)` | Get the argument part of a string |
| `string.env(s)` | Expand environment keys in a string |
| `string.getpattern(s)` | Get a pattern |
| `string.startswith(s, pattern)` | Prefix check |
| `string.endswith(s, pattern)` | Suffix check |

## `table` Library

| Function | Description |
|----------|-------------|
| `table.insert(t, [pos], value)` | Insert an element |
| `table.concat(t, [sep], [i], [j])` | Concatenate elements |
| `table.remove(t, [pos])` | Remove an element |
| `table.sort(t)` | Sort a table |
| `table.move(t, f, t, len)` | Move elements |
| `table.unpack(t, [i], [j])` | Unpack a table |
| `table.pack(...)` | Pack arguments |
| `table.decode(str)` | Decode a string to a table |

## `graphics` Library

| Function | Description |
|----------|-------------|
| `graphics.display(screen)` | Set the current screen |
| `graphics.new(type, title, ...)` | Create a screen object |
| `graphics.SetTitle(screen, title)` | Change the screen title |
| `graphics.SetTicker(screen, ticker)` | Set a screen ticker |
| `graphics.SetLabel(field, text)` / `graphics.GetLabel(field)` | Field label |
| `graphics.GetText(field)` / `graphics.SetText(field, text)` | Field text |
| `graphics.getCurrent()` | Get the current screen |
| `graphics.render(file)` | Render an image from a file |
| `graphics.append(screen, field)` | Append a field to a screen |
| `graphics.clear(screen)` | Clear a screen's fields |
| `graphics.addCommand(screen, command)` | Add a command/button |
| `graphics.handler(screen, {})` | Set screen button handlers |
| `graphics.vibrate(ms)` | Vibrate the device |
| `graphics.db` | Screens database |

## `socket` Library

| Function | Description |
|----------|-------------|
| `socket.connect(url)` | Open a TCP connection |
| `socket.peer(conn)` | Peer address |
| `socket.device(conn)` | Local address |
| `socket.server(port)` | Create a socket server |
| `socket.accept(server)` | Accept a connection |
| `socket.http.get(url, headers)` | HTTP GET |
| `socket.http.post(url, data, headers)` | HTTP POST |
| `socket.http.rget(url, headers)` | HTTP GET returning a stream |

## `java` Library

| Function | Description |
|----------|-------------|
| `java.class(name)` | Check whether a class exists |
| `java.getName()` | JVM name |
| `java.run(function, name)` | Run a function on another thread |
| `java.delete(table, field)` | Delete a table field in Java |
| `java.midlet.username` | Default username |
| `java.midlet.cache` | Files cached in the MIDlet |
| `java.midlet.build` | MIDlet build code |
| `java.midlet.uptime` | MIDlet uptime |

## `base64` Library

| Function | Description |
|----------|-------------|
| `base64.encode(s)` | Encode a string |
| `base64.decode(s)` | Decode a string |

## `push` Library (PushRegistry)

| Function | Description |
|----------|-------------|
| `push.register(conn, filter, midlet, sender)` | Register a connection |
| `push.unregister(conn)` | Unregister a connection |
| `push.list(filter)` | List connections (`*` for all) |
| `push.pending()` | Check for active connections |
| `push.setAlarm(midletClass, time)` | Set a MIDlet alarm |
| `push.getAlarm(midletClass)` | View an alarm |

---

## Examples

Runnable examples are in the [`examples/`](examples/) directory:

- `http_get.lua` — performing an HTTP GET
- `read_file.lua` — reading a file
- `write_file.lua` — writing a file
- `graphics_screen.lua` — building a screen
- `graphics_list.lua` — building a list

### Hello World

```lua
print("Hello Lua J2ME World!")

local t = { 1, 2, 3, name = "Lua" }
table.insert(t, 4)
print(table.concat(t, ", "))
```

### Basic Graphics

```lua
graphics.display(graphics.new("alert", "Welcome", "Hello Lua J2ME!"))

local form = graphics.new("form", "My Form")
graphics.append(form, { type = "text", value = "Welcome!", layout = "default" })
graphics.append(form, { type = "image", img = "/icon.png" })
graphics.append(form, { type = "field", label = "Username:", value = "" })

local back = graphics.new("command", { label = "Back", type = "back", priority = 1 })
local ok = graphics.new("command", { label = "OK", type = "ok", priority = 1 })
graphics.addCommand(form, back)
graphics.addCommand(form, ok)

graphics.display(form)
```

### List

```lua
local list = graphics.new("form", "Main Menu", "implicit")
for _, item in ipairs({ "Settings", "Games", "Tools", "Exit" }) do
    graphics.append(list, item)
end

local select = graphics.new("command", { label = "Select", type = "ok", priority = 1 })
graphics.addCommand(list, select)
graphics.handler(list, {
    [select] = function(option)
        print("Selected:", option)
    end
})

graphics.display(list)
```

---

## Limitations

- **Performance**: slower than native Lua because of the JVM.
- **Memory**: severe constraints on small heaps.
- **Libraries**: reduced functionality compared to full Lua (notably the
  `string` library).
- **Platform**: restricted to the J2ME ecosystem.
