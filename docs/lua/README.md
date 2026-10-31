# Lua for J2ME — Reference

This is a full implementation of the **Lua language for J2ME (CLDC)**, designed
to run on memory-limited mobile devices. It includes a parser, an interpreter,
and standard libraries adapted for the J2ME environment.

## Highlights

- **Complete language**: functions, tables, loops, and protected error handling
  (`pcall` returns Lua tracebacks)
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
| `require(mod)` | Load a rock from `/lib` (or an exact path) and cache it in `package.loaded` |
| `load(code [, scope [, name]])` | Compile **and run** a chunk, returning its results |
| `loadstring(...)` | Alias of `load` |
| `pairs(t)` | Iterate over table key/value pairs |
| `ipairs(t)` | Iterate over an array part |
| `next(t [, key])` | Next key/value pair, or `nil` at the end |
| `unpack(t [, i [, j]])` | Unpack an array range into multiple values |
| `collectgarbage(opt)` | Control garbage collection (also `free`/`total`/`count` in KB) |
| `tostring(v)` | Convert a value to string |
| `tonumber(v[, base])` | Convert a value to number |
| `select(index, ...)` | Select arguments (`"#"` returns the count) |
| `type(v)` | Return the value's type (`stream`, `screen`, `image`, ...) |
| `getAppProperty(key)` | Get an application property (`/name` reads a system property) |
| `setmetatable(t, mt)` | Attach (or clear, with `nil`) a metatable |
| `getmetatable(t)` | Return the table's metatable |
| `rawget(t, k)` | Read `k` without consulting `__index` |
| `rawset(t, k, v)` | Write `k` without consulting `__newindex` |
| `rawequal(a, b)` | Compare two values without `__eq` |
| `rawlen(t)` | Sequence length without `__len` |

`arg` holds the current process arguments (`arg[0]` is the program path).
`_G` is the global table, `_VERSION` is `"Lua J2ME"`, and `package.loaded` is
the module cache used by `require`.

## Metatables

Metatables are supported with the usual Lua metamethods:

| Metamethod | Used by |
|------------|---------|
| `__index` | `t[k]`, `t.k`, missing fields — a table to chain into, or a function `(t, k)` |
| `__newindex` | `t[k] = v` when the key is absent — a table to write into, or a function `(t, k, v)` |
| `__call` | `t(...)` |
| `__tostring` | `tostring(t)` and `print(t)` |
| `__len` | `#t` |
| `__eq`, `__lt`, `__le` | `==`, `<`, `<=` (and `~=`, `>`, `>=`) |
| `__add`, `__sub`, `__mul`, `__div`, `__mod`, `__pow`, `__unm`, `__concat` | arithmetic and concatenation |

`>` and `>=` are rewritten as `<` and `<=` with swapped operands, and `<=`
falls back to `not (b < a)` when only `__lt` is defined, as in Lua 5.2.

```lua
local Animal = {}
Animal.__index = Animal

Animal.new = function(name)
    local self = setmetatable({}, Animal)
    self.name = name
    return self
end
Animal.speak = function(self) return self.name .. " speaks" end

local Dog = setmetatable({}, { __index = Animal })
Dog.__index = Dog

local rex = Animal.new("rex")
print(rex:speak())          -- rex speaks
```

Notes:

- The metatable is stored inside the table under the hidden `__metatable` key.
  It is skipped by `pairs`, `#` and the `table` library, but `rawget(t,
  "__metatable")` still returns it.
- Method definitions with the `function t:m() ... end` shorthand are **not**
  supported by the parser; assign `t.m = function(self) ... end` instead.
- Coroutines (`coroutine.*`) are not available.

## `debug` Library

| Function | Description |
|----------|-------------|
| `debug.traceback([msg [, level]])` | Return a `stack traceback:` dump starting at `level` |
| `debug.getinfo([level])` | Table describing a stack frame (`what`, `source`, `short_src`, `currentline`, `name`, `linedefined`) |
| `debug.getinfo(func)` | Same table for a function (`what` is `"C"` for native functions) |
| `debug.getregistry()` | The globals table (`_G`) |

`level` follows Lua: `1` is the function calling `debug.getinfo`, `0` is the
native function itself, and a level above the stack returns the main chunk
(`what = "main"`). `debug.getlocal`, `debug.setlocal`, `debug.sethook` and
upvalues are not implemented.

## `os` Library

| Function | Description |
|----------|-------------|
| `os.execute(cmd [, builtin])` | Execute a shell command (supports `>`, `>>`, `&&` and `&`) |
| `os.request(pid, payload, args)` | Send a request to a service (kernel) |
| `os.getenv([var])` / `os.setenv(var, val)` | Get one or all / set environment variables |
| `os.exit([code])` | Terminate the current process |
| `os.date([fmt [, secs]])` / `os.clock()` | Format a date / milliseconds since start |
| `os.time([fields])` | Unix seconds, or the time described by a table |
| `os.getpid([name])` | Current PID, or the PID of a named process |
| `os.setproc(attr, val)` | Set process attributes (`name`, `handler`, `cmd`, `screen`, ...) |
| `os.getproc([pid [, field]])` | Process table, or one field of a process |
| `os.getuid([username])` | Current uid, or the uid of a user |
| `os.getcwd()` / `os.chdir([path])` | Get / change working directory |
| `os.open(uri)` | Ask the device to open a URI |
| `os.scope([table])` | Get or change scope |
| `os.su(user [, password])` | Switch the current user |
| `os.remove(file)` | Remove a file or directory |
| `os.join(file)` | Resolve a path against the PWD |
| `os.mkdir(dir)` | Create a directory |

`os.date` supports the `%Y %y %m %d %e %H %I %M %S %j %A %a %B %b %p %Z %%`
fields. `os.time` accepts `year`, `month` (1-12), `day`, `hour`, `min` and
`sec`. Relative paths in `os.remove`/`os.mkdir` follow the PWD.

A service is a process with a handler installed by `os.setproc("handler", fn)`;
`os.request` calls it with `(payload, args, scope, pid, uid)`. The `/bin/init`
kernel is PID 1.

## `io` Library

| Function | Description |
|----------|-------------|
| `io.read([source [, length]])` | Read a file, stream or the current stdout |
| `io.write(data [, target [, mode]])` | Write to a file / stream (`"a"` appends) |
| `io.close(...)` | Close streams, sockets, servers or players |
| `io.open([file])` | Open a file, or an in-memory output buffer with no argument |
| `io.popen(program, args, sudo, stdout, scope, stream)` | Run a program (returns status + output) |
| `io.dirs([path])` | List the contents of a directory |
| `io.setstdout(file)` | Redirect the program's stdout |
| `io.mount(struct)` | Mount a filesystem structure |
| `io.new()` | Create an in-memory `StringBuffer` stream |
| `io.copy(source, target)` | Copy a file / stream / buffer |

`io.stdout` is the current output sink. Relative paths in `io.read`,
`io.write` and `io.dirs` are resolved against the PWD.

> `io.dirs(path)` returns entries for `/tmp/`, `/mnt/<sub>`, `/proc`, the paged
> VFS directories under `/bin`, `/etc`, `/lib`, `/boot` and `/home` (including
> nested `/home/<user>/...` folders), and the mount points themselves. Other
> paths yield an empty table.

## `string` Library

> **Reduced native library.** `format`, `rep`, `gsub`, `gmatch`, `lpad`,
> `rpad`, `join`, `lines`, `replace` and `text2note` are provided by the
> shipped `string` rock (`require("string")`), which also replaces `find` and
> `match` with a Lua pattern engine. Native `find`/`match` are plain substring
> searches.

| Function | Description |
|----------|-------------|
| `string.upper(s)` / `string.lower(s)` | Case conversion |
| `string.len(s)` | String length |
| `string.find(s, needle [, init])` | Find a plain substring |
| `string.match(s, needle [, init])` | Return the plain substring |
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
| `table.sort(t [, comparator])` | Sort a table |
| `table.move(t, from, to, len [, a])` | Move elements |
| `table.unpack(t, [i], [j])` | Unpack a table |
| `table.pack(...)` | Pack arguments |
| `table.decode(str)` | Decode a string to a table |

## `math` Library

The runtime itself only exposes `math.random([m [, n]])`: no argument returns a
float in `[0, 1)`, `random(n)` returns `1..n` and `random(m, n)` returns `m..n`.
The shipped `math` rock (`require("math")`) adds `pi`, `e`, `huge`, `abs`,
`sqrt`, `pow`, `factorial`, `fmod`, `floor`, `ceil`, `trunc`, `round`, `modf`,
`sign`, `exp`, `log`, `log10`, `log2`, the trigonometric functions, `min`,
`max`, `clamp`, `gcd`, `lcm`, `hypot`, `isclose` and `tointeger`. It is
implemented with pure arithmetic because Lua J2ME has no `inf`/`nan` literals
and no float library.

## `graphics` Library

Screens, lists, text boxes and alerts are returned as **handles**: plain Lua
tables that answer method calls, so `screen:append(item)`,
`screen:addCommand(cmd)`, `screen:handler({...})` and `screen:display()` are
the same as the `graphics.*` functions below. `type(screen)` is still
`"screen"`, and every function also accepts a raw screen object.

```lua
local list = graphics.new("list", "Menu", "implicit")
list:append("Open")
list:append("Exit")
local open = graphics.new("command", { label = "Open", type = "ok", priority = 1 })
list:addCommand(open)
list:handler(function(screen, label, selected)
    -- one function for every command: the screen handle, the command label
    -- and then the field values (a table handler only gets the values)
    if label == "Open" then print("opening " .. selected) end
end)
list:display()
```

| Function | Description |
|----------|-------------|
| `graphics.display(screen [, next])` | Set the current screen (alerts may name the next screen) |
| `graphics.new(type, titleOrOptions, ...)` | Create a screen, command or item |
| `graphics.title(screen [, title])` | Read / change the screen title |
| `graphics.SetTitle(screen [, title])` | Read / change the screen title |
| `graphics.ticker(screen [, text])` | Read / set a screen ticker |
| `graphics.SetTicker(screen [, text])` | Read / set a screen ticker |
| `graphics.SetLabel(field, text)` / `graphics.GetLabel(field)` | Field label |
| `graphics.GetText(field)` / `graphics.SetText(field, text)` | Field text |
| `graphics.getCurrent()` | Get the current screen handle |
| `graphics.render(file)` | Render an image from a file |
| `graphics.append(screen, field [, image])` | Append an item/row to a screen |
| `graphics.clear(screen)` | Clear a Form or List |
| `graphics.addCommand(screen, command)` | Add a command/button |
| `graphics.handler(screen, { [command] = fn })` | Set screen command handlers |
| `graphics.handler(screen, fn)` | One handler for every command: `fn(screen, label, ...values)` |
| `graphics.vibrate([ms])` | Vibrate the device |
| `graphics.taskmngr()` | Open the system task manager |
| `graphics.fire` | `List.SELECT_COMMAND` handler key |
| `graphics.new("gauge", { label, interactive, maxValue, value })` | Standalone gauge |
| `graphics.SetIndicator(alert, gauge)` | Show a gauge inside an alert |
| `graphics.SetGauge(gauge, value)` / `graphics.GetGauge(gauge)` | Update / read a gauge |
| `graphics.CONTINUOUS_RUNNING` (and `CONTINUOUS_IDLE`, `INCREMENTAL_IDLE`, `INCREMENTAL_UPDATING`) | Continuous-indicator values |
| `graphics.db` | Screens database |

`graphics.new` types:

| Type | Arguments | Result |
|------|-----------|--------|
| `"screen"` / `"form"` | `(title)` | `Form` (handle) |
| `"list"` | `(title [, "implicit"/"exclusive"/"multiple"])` | `List` (handle) |
| `"edit"` / `"textbox"` | `(title [, text])` | `TextBox` (handle) |
| `"alert"` | `(title, text [, { type, timeout }])` | `Alert` (handle) |
| `"command"` | `({ label, type, priority })` | `Command` |
| `"buffer"` | `({ label, value, layout, style })` | `StringItem` |
| `"field"` | `({ label, value, length, mode })` | `TextField` |
| `"gauge"` | `({ label, interactive, maxValue, value })` | `Gauge` |

`graphics.append` accepts a plain string, an item from `graphics.new`, or a
table with `type = "image"`, `"text"`, `"item"`, `"choice"`, `"field"`,
`"spacer"` or `"gauge"`. For a `List` the optional third argument is the row
image.

### `graphics.canvas`

A free-form drawing surface for games and custom screens: shapes, text, fonts,
cursor, held keys, pointer input, LCDUI commands and a frame loop.

```lua
local c = graphics.canvas.new({ title = "Game", background = 0x000018 })
c:display()
c:loop(function(canvas, dt)
    canvas:clear()
    canvas:color(0x00FF88)
    canvas:circle(120, 160, 10, true)
end, 33)
```

See the [Canvas guide](../LUACANVAS.md) for the full API, callbacks, threading
rules and the `canvasdemo` example app.

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
| `socket.http.rpost(url, data, headers)` | HTTP POST returning a stream |

## `java` Library

| Function | Description |
|----------|-------------|
| `java.class(name)` | Check whether a class exists |
| `java.getName()` | JVM name and version |
| `java.run(function, name)` | Run a function on another thread |
| `java.thread()` | Current thread name |
| `java.sleep(ms)` | Sleep the current thread |
| `java.yield([ms])` | Release the interpreter so UI callbacks can run |
| `java.delete(table, field)` | Delete a table field in Java |
| `java.midlet.username` | Default username |
| `java.midlet.build` | MIDlet build code |
| `java.midlet.lite` | Whether this build uses the lite ELF interpreter |
| `java.midlet.uptime()` | MIDlet uptime in milliseconds |

## `base64` Library

| Function | Description |
|----------|-------------|
| `base64.encode(stringOrTable)` | Encode a string or a byte array |
| `base64.decode(s [, asStream])` | Decode to a byte table (or a stream) |

## `push` Library (PushRegistry)

| Function | Description |
|----------|-------------|
| `push.register(conn, filter, midlet [, sender])` | Register a connection (`sender` is ignored) |
| `push.unregister(conn)` | Unregister a connection |
| `push.list(filter)` | List connections (`*` for all) |
| `push.pending()` | Check for pending push activation |
| `push.setAlarm(midletClass, time)` | Set a MIDlet alarm |

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

local form = graphics.new("screen", "My Form")
graphics.append(form, { type = "text", value = "Welcome!", layout = "default" })
graphics.append(form, { type = "image", img = "/icon.png" })
graphics.append(form, { type = "field", label = "Username:", value = "" })

local back = graphics.new("command", { label = "Back", type = "back", priority = 1 })
local ok = graphics.new("command", { label = "OK", type = "ok", priority = 1 })
graphics.addCommand(form, back)
graphics.addCommand(form, ok)

graphics.display(form)
```

An alert accepts an optional options table as its fourth argument:

```lua
graphics.new("alert", "Battery", "Low charge", { type = "warning", timeout = 2000 })
```

`type` is `info` (default), `warning`, `error`, `alarm` or `confirm`, and
`timeout` is a duration in milliseconds (`0`, the default, keeps the alert
until the user dismisses it). An alert can also show one non-interactive gauge
as its indicator:

```lua
local alert = graphics.new("alert", "Install", "Unpacking files")
local gauge = graphics.new("gauge", { interactive = false, maxValue = 100, value = 75 })
graphics.SetIndicator(alert, gauge)
graphics.display(alert)
```

With `maxValue = 0` the gauge is the MIDP continuous indicator: its value is
one of `graphics.CONTINUOUS_IDLE`, `graphics.INCREMENTAL_IDLE`,
`graphics.CONTINUOUS_RUNNING` or `graphics.INCREMENTAL_UPDATING`. Use
`graphics.SetGauge(gauge, value)` from a `java.run` loop to animate a normal
gauge (the update is posted to the event thread).

The `alert` command wraps this: `alert "Install" "Unpacking" -g 75 -b Cancel`
keeps the alert with a 75/100 gauge, `alert "Copy" "Please wait" -g` shows the
continuous running indicator and `-c` animates a value gauge.

### List

```lua
local list = graphics.new("list", "Main Menu", "implicit")
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
- **Libraries**: reduced functionality compared to full Lua; the shipped
  `string` and `math` rocks restore most of the standard behavior.
- **Platform**: restricted to the J2ME ecosystem.
