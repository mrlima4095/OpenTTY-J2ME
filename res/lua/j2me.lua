---@meta
-- OpenTTY Lua J2ME API stub
--
-- This file describes the API the OpenTTY runtime exposes to Lua scripts on
-- the device (CLDC 1.0 / MIDP 2.0). It is a hinting stub: it is never
-- executed, it only teaches editors (Lua Language Server and friends) the
-- names, signatures and return values of the runtime.
--
-- Scripts start with a `#!/bin/lua` shebang, get their arguments in `arg`
-- (`arg[0]` is the script path) and can load rocks from `/lib` with
-- `require`. The interpreter is Lua 5.1-like: see docs/lua/README.md and
-- docs/LUACANVAS.md for the differences.
--
-- LCDUI screens are handles: graphics.new("screen"/"list"/"edit"/"alert")
-- returns a table that answers method calls (`screen:append(item)`,
-- `screen:addCommand(cmd)`, `screen:handler({...})`, `screen:display()`)
-- because its metatable uses the `graphics` table as __index. The graphics.*
-- functions keep accepting the handle as their first argument.

---@class CanvasHandle
---@field title string
---@field font string
---@field background integer|string
---@field foreground integer|string
---@field color integer|string
---@field paint? fun(canvas: CanvasHandle, width: integer, height: integer)
---@field keyPressed? fun(canvas: CanvasHandle, keyCode: integer, gameAction: integer)
---@field keyReleased? fun(canvas: CanvasHandle, keyCode: integer)
---@field keyRepeated? fun(canvas: CanvasHandle, keyCode: integer, gameAction: integer)
---@field pointerPressed? fun(canvas: CanvasHandle, x: integer, y: integer)
---@field pointerReleased? fun(canvas: CanvasHandle, x: integer, y: integer)
---@field pointerDragged? fun(canvas: CanvasHandle, x: integer, y: integer)
---@field show? fun(canvas: CanvasHandle, width: integer, height: integer)
---@field hide? fun(canvas: CanvasHandle, width: integer, height: integer)
---@field size? fun(canvas: CanvasHandle, width: integer, height: integer)
---@field tick? fun(canvas: CanvasHandle, dtMs: integer)
---@field command? fun(canvas: CanvasHandle, label: string)

--- A screen created by `graphics.new` (`"screen"`/`"form"`, `"list"`,
--- `"edit"`/`"textbox"`, `"alert"`). The runtime returns a plain table whose
--- metatable keeps the Java Displayable under `__screen` and uses the
--- `graphics` table as `__index`, so every graphics function is also a method:
--- `graphics.append(screen, item)` and `screen:append(item)` are the same
--- call, `graphics.getCurrent() == screen` holds, and `type(screen)` is
--- `"screen"`.
---@class ScreenHandle
---@field append fun(self: ScreenHandle, item: ScreenItem|AppendItem|string, image?: ImageHandle|string): nil
---@field addCommand fun(self: ScreenHandle, command: ScreenCommand): nil
---@field handler fun(self: ScreenHandle, actions: table<ScreenCommand, fun(...): any>|fun(screen: ScreenHandle, label: string, ...): any): nil
---@field display fun(self: ScreenHandle, next?: ScreenHandle): nil
---@field clear fun(self: ScreenHandle): nil
---@field title fun(self: ScreenHandle, title?: string): string
---@field ticker fun(self: ScreenHandle, text?: string): string
---@field SetTitle fun(self: ScreenHandle, title?: string): string
---@field SetTicker fun(self: ScreenHandle, text?: string): string
---@field SetText fun(self: ScreenHandle, text: string): nil
---@field GetText fun(self: ScreenHandle): string
---@field SetIndicator fun(self: ScreenHandle, gauge: ScreenItem): nil
---@field SetGauge fun(self: ScreenHandle, value: integer): nil
---@field GetGauge fun(self: ScreenHandle): integer
---@field vibrate fun(self: ScreenHandle, ms?: integer): nil

---@alias Stream table            -- InputStream, OutputStream, StringBuffer or ByteBuffer
---@alias Screen ScreenHandle     -- Displayable (Form, List, TextBox, Alert)
---@alias ScreenItem table        -- Item (StringItem, TextField, ChoiceGroup, Gauge, Spacer)
---@alias ScreenCommand table     -- Command created by graphics.new("command", ...)
---@alias ImageHandle table       -- Image returned by graphics.render
---@alias AudioPlayer table       -- audio player/player state
---@alias SocketConnection table  -- open socket connection
---@alias SocketServer table      -- listening server socket
---@alias Canvas CanvasHandle     -- graphics.canvas handle

-- ---------------------------------------------------------------------------
-- Global functions
-- ---------------------------------------------------------------------------

--- Current process arguments. `arg[0]` is the program path, `arg[1]`.. are
--- the arguments. Service handlers get their own arguments (see os.request),
--- not this table.
---@type table<integer, string>
arg = {}

--- Global environment table. Assigning to an undeclared name writes here;
--- `_G.foo` and `foo` are the same slot.
---@type table
_G = {}

--- Interpreter version string, always "Lua J2ME".
_VERSION = "Lua J2ME"

--- Numeric uid of the current scope, kept as a string.
_UID = "1000"

--- Write values to the current process stdout (default: the shell console).
--- Values are separated by tabs and end with a newline; a table is printed
--- field by field (nested vectors tab-separated).
---@param ... any
function print(...) end

--- Raise an error. A table/vector argument is kept as a structured error
--- object and can be caught by pcall.
---@param message any
function error(message) end

--- Call `f` in protected mode and return its results.
---@param f function
---@param ... any
---@return boolean ok
---@return any result
function pcall(f, ...) end

--- Raise an error when `v` is falsy, otherwise return all arguments unchanged.
---@param v any
---@param message? any
---@return any
function assert(v, message) end

--- Load a rock (or a Lua file). Looks for the exact path first, then
--- `/lib/<name>.lua` and `/lib/<name>.so`, and caches the module in
--- `package.loaded` so later calls return the same value.
---@param name string
---@return any module
function require(name) end

--- Compile and immediately run a Lua chunk with an optional scope table,
--- returning the chunk results. Unlike stock Lua this is not a chunk loader;
--- use `pcall(load, code)` when you only want to test compilation.
---@param code string
---@param scope? table
---@param name? string
---@return any ...
function load(code, scope, name) end

--- Alias of `load` (Lua 5.1 compatibility).
---@param code string
---@param scope? table
---@param name? string
---@return any ...
function loadstring(code, scope, name) end

--- Iterate over all key/value pairs (skips the internal metatable slot).
---@param t table
---@return any
function pairs(t) end

--- Iterate over the array part of a table.
---@param t table
---@return any
function ipairs(t) end

--- Return the next key/value pair of a table, or nil at the end.
---@param t table
---@param key? any
---@return any key
---@return any value
function next(t, key) end

--- Run the Java garbage collector or query memory (values in KB).
--- Options: "collect"/"restart", "stop", "free", "total", "count", "step",
--- "isrunning", "generational", "incremental". With no option it collects.
---@param option? string
---@return integer|boolean|string|nil
function collectgarbage(option) end

--- Convert a value to a string (honors `__tostring`).
---@param value any
---@return string
function tostring(value) end

--- Convert a value to a number, or parse it in the given base (2..36).
---@param value any
---@param base? integer
---@return number|nil
function tonumber(value, base) end

--- Select the arguments from `index` on, or return the argument count for "#".
--- Negative indexes count from the end.
---@param index integer|"#"
---@param ... any
---@return any ...
function select(index, ...) end

--- Return the Lua type name: "nil", "boolean", "number", "string", "table",
--- "function", "stream", "connection", "server", "screen", "image", "button",
--- "audio" or "userdata".
---@param value any
---@return string
function type(value) end

--- Read a MIDlet property (JAD/MANIFEST). A key starting with "/" reads the
--- Java system property of that name instead.
---@param key string
---@return string|nil
function getAppProperty(key) end

--- Attach a metatable (`nil` clears it). The metatable is stored inside the
--- table under the `__metatable` key.
---@param t table
---@param metatable? table
---@return table
function setmetatable(t, metatable) end

--- Return the table's metatable, or nil.
---@param t table
---@return table|nil
function getmetatable(t) end

--- Read a key without consulting `__index`.
---@param t table
---@param key any
---@return any
function rawget(t, key) end

--- Write a key without consulting `__newindex`.
---@param t table
---@param key any
---@param value any
---@return table
function rawset(t, key, value) end

--- Compare two values without metamethods.
---@param a any
---@param b any
---@return boolean
function rawequal(a, b) end

--- Length of a table without metamethods (the metatable slot is excluded).
---@param t table
---@return integer
function rawlen(t) end

--- Return the array elements from `i` to `j` as multiple values.
---@param t table
---@param i? integer
---@param j? integer
---@return any ...
function unpack(t, i, j) end

--- Module cache and loader table. `package.loaded` maps a module name to its
--- cached value; `package.loadlib(name)` is an alias of `require(name)`.
package = {
    loaded = {},
    loadlib = function(name) return nil end,
}

-- ---------------------------------------------------------------------------
-- os library
-- ---------------------------------------------------------------------------

os = {
    --- Execute a shell command in a child process through the installed shell.
    --- Supports redirection (`>` / `>>`), `&&` chains that stop on the first
    --- failure, and background jobs (`&`, which returns 0 immediately).
    ---@param command string
    ---@param builtin? boolean skip alias expansion and call /bin directly
    ---@return integer status
    execute = function(command, builtin) return 0 end,

    --- Read an environment value, or the whole environment table with no key.
    ---@param key? string
    ---@return string|table|nil
    getenv = function(key) return nil end,

    --- Set an environment value. A nil value removes the key.
    ---@param key string
    ---@param value? string
    setenv = function(key, value) end,

    --- Milliseconds since this process started.
    ---@return integer
    clock = function() return 0 end,

    --- Set the process locale (stored as the LOCALE attribute).
    ---@param locale string
    setlocale = function(locale) end,

    --- Terminate the current process (PID 1 destroys the MIDlet). Does not
    --- return. Without a status the exit status is 1.
    ---@param status? integer
    exit = function(status) end,

    --- Format the current date/time. Without a format it returns the CLDC
    --- `Date.toString()` form. Supported `%` fields: Y y m d e H I M S j A a B
    --- b p Z % (unknown fields are copied through).
    ---@param format? string
    ---@param seconds? number Unix time to format instead of now
    ---@return string
    date = function(format, seconds) return "" end,

    --- Current Unix time in seconds, or the time described by a table with
    --- `year`, `month` (1-12), `day`, `hour`, `min` and `sec` fields.
    ---@param fields? table
    ---@return integer
    time = function(fields) return 0 end,

    --- Current process id, or the pid of the process with that name.
    ---@param name? string
    ---@return string|nil
    getpid = function(name) return "1" end,

    --- Change a process attribute: "name", "handler", "cmd", "sighandler",
    --- "screen" (a ScreenHandle from graphics.new), "stdout", "scope", or any
    --- custom key stored in the process database. A boolean first argument
    --- sets the process kill flag.
    ---@param field string|boolean
    ---@param value? any
    setproc = function(field, value) end,

    --- With no arguments, return a table mapping every pid to its process
    --- name. With a pid, return the process database field (or raise when the
    --- field is missing / the caller lacks permission).
    ---@param pid? string
    ---@param field? string
    ---@return table|any
    getproc = function(pid, field) return {} end,

    --- Current working directory of this process.
    ---@return string
    getcwd = function() return "/home/" end,

    --- Call the handler of a service process. The handler receives
    --- `(payload, args, scope, pid, uid)` and its return value is forwarded.
    ---@param pid string
    ---@param payload any
    ---@param args? any
    ---@return any
    request = function(pid, payload, args) return nil end,

    --- Numeric uid of the current user, or of the named user.
    ---@param user? string
    ---@return integer
    getuid = function(user) return 1000 end,

    --- Change the working directory. With no argument it returns the current
    --- path; otherwise it returns a status code (0 = ok).
    ---@param path? string
    ---@return integer|string
    chdir = function(path) return 0 end,

    --- Ask the device to open a URI (browser, dialer, ...). Returns whether the
    --- platform accepted the request.
    ---@param uri string
    ---@return boolean
    open = function(uri) return true end,

    --- Switch the current user. Returns 0 on success, 13 on bad credentials.
    ---@param username string
    ---@param password? string
    ---@return integer
    su = function(username, password) return 0 end,

    --- Delete a file or an empty directory. Relative paths follow the PWD.
    ---@param path string
    ---@return integer status
    remove = function(path) return 0 end,

    --- With no argument, return the current scope table. With a table it
    --- replaces the scope and returns nothing.
    ---@param scope? table
    ---@return table|nil
    scope = function(scope) return {} end,

    --- Resolve a path against the current PWD and return the absolute path.
    ---@param path string
    ---@return string
    join = function(path) return "/home/" end,

    --- Create a directory (and its parent path when the backend supports it).
    --- Relative paths follow the PWD.
    ---@param path string
    ---@return integer status
    mkdir = function(path) return 0 end,
}

-- ---------------------------------------------------------------------------
-- io library
-- ---------------------------------------------------------------------------

io = {
    --- Active stdout sink (StringItem, StringBuffer or a file path).
    ---@type any
    stdout = nil,

    --- Read from a file, an input stream, stdout, or the current stdout when
    --- called with no argument. `length` limits how many bytes are read from
    --- a stream. A missing file returns an empty string.
    ---@param source? string|Stream
    ---@param length? integer
    ---@return string
    read = function(source, length) return "" end,

    --- Write data to a file or stream and return a status code (0 = ok).
    --- `mode` = "a" appends; otherwise the target is truncated. Relative
    --- paths follow the PWD. Called with no target it writes to stdout.
    ---@param data string|Stream
    ---@param target? string|Stream
    ---@param mode? "a"|"w"
    ---@return integer status
    write = function(data, target, mode) return 0 end,

    --- Close streams, sockets, servers, players or buffers. Accepts several
    --- values and closes every one.
    ---@param ... any
    close = function(...) end,

    --- Open a file for reading (InputStream), or nil when it does not exist.
    --- With no argument it returns a fresh in-memory output buffer
    --- (ByteArrayOutputStream) ready for io.write.
    ---@param path? string
    ---@return Stream|nil
    open = function(path) return nil end,

    --- Run a program as a child process. `sudo` = true keeps the caller uid,
    --- false runs as the default user. `out` is the stdout sink (a StringBuffer
    --- by default) and `scope` the child scope. Returns the exit status and,
    --- when `out` is a StringBuffer, its contents.
    ---@param program string
    ---@param arguments? string|table
    ---@param sudo? boolean
    ---@param out? Stream|string
    ---@param scope? table
    ---@param stream? Stream
    ---@return integer status
    ---@return string output
    popen = function(program, arguments, sudo, out, scope, stream) return 0, "" end,

    --- List a directory as an array-like table (1-based). Supported roots:
    --- /tmp, /mnt, /proc, /home, /bin, /etc, /lib, /boot and nested VFS
    --- directories. Relative paths follow the PWD.
    ---@param path? string
    ---@return table<integer, string>
    dirs = function(path) return { ".." } end,

    --- Redirect print()/stdout for this process.
    ---@param stream? Stream|string
    setstdout = function(stream) end,

    --- Populate the VFS from a mount description. Each line is
    --- `path=file1,file2,dir/`; lines starting with `#` and empty lines are
    --- ignored. An empty string clears the mounted entries.
    ---@param spec string
    mount = function(spec) end,

    --- Create an in-memory string buffer (StringBuffer) usable as a stream.
    ---@return Stream
    new = function() return {} end,

    --- Copy a file, stream or buffer to a file or stream. Returns 0 on
    --- success.
    ---@param source string|Stream
    ---@param target string|Stream
    ---@return integer status
    copy = function(source, target) return 0 end,
}

-- ---------------------------------------------------------------------------
-- table library
-- ---------------------------------------------------------------------------

table = {
    --- Insert `value` at `pos` (default: append) in an array-like table.
    ---@param t table
    ---@param posOrValue integer|any
    ---@param value? any
    insert = function(t, posOrValue, value) end,

    --- Join the array elements between `i` and `j` with `sep`.
    ---@param t table
    ---@param sep? string
    ---@param i? integer
    ---@param j? integer
    ---@return string
    concat = function(t, sep, i, j) return "" end,

    --- Remove and return the element at `pos` (default: the last one), shifting
    --- the remaining elements left.
    ---@param t table
    ---@param pos? integer
    ---@return any
    remove = function(t, pos) return nil end,

    --- Sort an array-like table in place. `comparator(a, b)` must return true
    --- when `a` belongs before `b`.
    ---@param t table
    ---@param comparator? fun(a: any, b: any): boolean
    sort = function(t, comparator) end,

    --- Move `len` elements starting at `from` to position `to`, then return the
    --- same table. `a` is the destination range start used for bounds checking
    --- (default 1).
    ---@param t table
    ---@param from integer
    ---@param to integer
    ---@param len integer
    ---@param a? integer
    ---@return table
    move = function(t, from, to, len, a) return t end,

    --- Pack the arguments into an array-like table with a `n` field.
    ---@param ... any
    ---@return table
    pack = function(...) return { n = 0 } end,

    --- Return the elements from `i` to `j` as multiple values.
    ---@param t table
    ---@param i? integer
    ---@param j? integer
    ---@return any ...
    unpack = function(t, i, j) end,

    --- Parse `key=value` lines (INI/config style) into a table. Comments start
    --- with `#`; quoted values are unquoted. An empty string returns {}.
    ---@param text string
    ---@return table<string, string>
    decode = function(text) return {} end,
}

-- ---------------------------------------------------------------------------
-- string library
-- ---------------------------------------------------------------------------

string = {
    --- Uppercase every character.
    ---@param s string
    ---@return string
    upper = function(s) return "" end,

    --- Lowercase every character.
    ---@param s string
    ---@return string
    lower = function(s) return "" end,

    --- Byte length of the string.
    ---@param s string
    ---@return integer
    len = function(s) return 0 end,

    --- Plain substring search (not a Lua pattern): 1-based index or nil.
    ---@param s string
    ---@param needle string
    ---@param init? integer
    ---@return integer|nil
    find = function(s, needle, init) return nil end,

    --- Plain substring search returning the matched text or nil.
    ---@param s string
    ---@param needle string
    ---@param init? integer
    ---@return string|nil
    match = function(s, needle, init) return nil end,

    --- Reverse the string.
    ---@param s string
    ---@return string
    reverse = function(s) return "" end,

    --- Substring from `i` to `j` (1-based; negative indexes count from the end).
    ---@param s string
    ---@param i? integer
    ---@param j? integer
    ---@return string
    sub = function(s, i, j) return "" end,

    --- Java hashCode of the string (stable within a session).
    ---@param s string
    ---@return integer
    hash = function(s) return 0 end,

    --- Byte value(s) at `i`..`j` as multiple returns.
    ---@param s string
    ---@param i? integer
    ---@param j? integer
    ---@return integer ...
    byte = function(s, i, j) return 0 end,

    --- Build a string from byte values (numbers or a table of numbers).
    ---@param ... integer|table
    ---@return string
    char = function(...) return "" end,

    --- Remove leading and trailing whitespace.
    ---@param s string
    ---@return string
    trim = function(s) return "" end,

    --- Generate a random UUID v4 string.
    ---@return string
    uuid = function() return "" end,

    --- Split a string. With a separator, returns the parts; with `nil` as the
    --- separator, splits shell-style (honoring quotes); with "" it splits into
    --- characters.
    ---@param s string
    ---@param sep? string
    ---@return table<integer, string>
    split = function(s, sep) return {} end,

    --- First whitespace-delimited token of a command line.
    ---@param s string
    ---@return string
    getCommand = function(s) return "" end,

    --- Rest of a command line after the first token.
    ---@param s string
    ---@return string
    getArgument = function(s) return "" end,

    --- Expand `$VAR`/`$.` environment references and unescape the result.
    ---@param s string
    ---@return string
    env = function(s) return "" end,

    --- Trim and remove one pair of surrounding double quotes.
    ---@param s string
    ---@return string
    getpattern = function(s) return "" end,

    --- Test whether the string starts with `prefix`.
    ---@param s string
    ---@param prefix string
    ---@return boolean
    startswith = function(s, prefix) return false end,

    --- Test whether the string ends with `suffix`.
    ---@param s string
    ---@param suffix string
    ---@return boolean
    endswith = function(s, suffix) return false end,

    -- The functions below are added by the shipped `string` rock
    -- (`require("string")`). It also replaces find/match with a Lua pattern
    -- engine.

    --- Lua string.format (subset: d i u o x X c s q p f e g and %).
    ---@param fmt string
    ---@param ... any
    ---@return string
    format = function(fmt, ...) return "" end,

    --- Repeat `s` `n` times, optionally separated by `sep`.
    ---@param s string
    ---@param n integer
    ---@param sep? string
    ---@return string
    rep = function(s, n, sep) return "" end,

    --- Replace every match of the Lua pattern `p` (function/table/string form).
    ---@param s string
    ---@param p string
    ---@param repl string|table|function
    ---@param limit? integer
    ---@return string
    gsub = function(s, p, repl, limit) return "" end,

    --- Iterate over every match of the Lua pattern `p`.
    ---@param s string
    ---@param p string
    ---@return fun(): string
    gmatch = function(s, p) return function() end end,

    --- Pad `s` on the left to `n` characters with `ch`.
    ---@param s string
    ---@param n integer
    ---@param ch? string
    ---@return string
    lpad = function(s, n, ch) return "" end,

    --- Pad `s` on the right to `n` characters with `ch`.
    ---@param s string
    ---@param n integer
    ---@param ch? string
    ---@return string
    rpad = function(s, n, ch) return "" end,

    --- Join an array-like table with `sep`.
    ---@param t table
    ---@param sep? string
    ---@return string
    join = function(t, sep) return "" end,

    --- Iterate over the lines of a string.
    ---@param s string
    ---@return fun(): string
    lines = function(s) return function() end end,

    --- Replace plain substrings.
    ---@param s string
    ---@param find string
    ---@param repl string
    ---@return string
    replace = function(s, find, repl) return "" end,

    --- Convert a note name/number to the other representation.
    ---@param s string|integer
    ---@return string|integer
    text2note = function(s) return "" end,
}

-- ---------------------------------------------------------------------------
-- math library
-- ---------------------------------------------------------------------------

math = {
    --- Native random source. Without arguments returns a float in [0, 1).
    --- `random(n)` returns 1..n and `random(m, n)` returns m..n.
    ---@param m? integer
    ---@param n? integer
    ---@return number
    random = function(m, n) return 0 end,

    -- The functions below are added by the shipped `math` rock
    -- (`require("math")`), which uses only arithmetic (the runtime has no
    -- float library and no inf/nan literals).

    ---@type number
    pi = 3.141592653589793,
    ---@type number
    huge = 0, -- computed by the rock: Lua J2ME has no infinity literal
    ---@type number
    e = 2.718281828459045,

    ---@param x number
    ---@return number
    abs = function(x) return 0 end,
    ---@param x number
    ---@return number
    sqrt = function(x) return 0 end,
    ---@param base number
    ---@param exponent number
    ---@return number
    pow = function(base, exponent) return 0 end,
    ---@param x number
    ---@return number
    factorial = function(x) return 0 end,
    ---@param x number
    ---@param y number
    ---@return number
    fmod = function(x, y) return 0 end,
    ---@param x number
    ---@return number
    floor = function(x) return 0 end,
    ---@param x number
    ---@return number
    ceil = function(x) return 0 end,
    ---@param x number
    ---@return number
    trunc = function(x) return 0 end,
    ---@param x number
    ---@return number
    round = function(x) return 0 end,
    ---@param x number
    ---@return number ipart
    ---@return number fpart
    modf = function(x) return 0, 0 end,
    ---@param x number
    ---@return integer
    sign = function(x) return 0 end,
    ---@param x number
    ---@return number
    exp = function(x) return 0 end,
    ---@param x number
    ---@param base? number
    ---@return number
    log = function(x, base) return 0 end,
    ---@param x number
    ---@return number
    log10 = function(x) return 0 end,
    ---@param x number
    ---@return number
    log2 = function(x) return 0 end,
    ---@param x number
    ---@return number
    sin = function(x) return 0 end,
    ---@param x number
    ---@return number
    cos = function(x) return 0 end,
    ---@param x number
    ---@return number
    tan = function(x) return 0 end,
    ---@param x number
    ---@return number
    atan = function(x) return 0 end,
    ---@param y number
    ---@param x number
    ---@return number
    atan2 = function(y, x) return 0 end,
    ---@param x number
    ---@return number
    asin = function(x) return 0 end,
    ---@param x number
    ---@return number
    acos = function(x) return 0 end,
    ---@param x number
    ---@return number
    deg = function(x) return 0 end,
    ---@param x number
    ---@return number
    rad = function(x) return 0 end,
    ---@param ... number
    ---@return number
    min = function(...) return 0 end,
    ---@param ... number
    ---@return number
    max = function(...) return 0 end,
    ---@param x number
    ---@param low number
    ---@param high number
    ---@return number
    clamp = function(x, low, high) return 0 end,
    ---@param a integer
    ---@param b integer
    ---@return integer
    gcd = function(a, b) return 0 end,
    ---@param a integer
    ---@param b integer
    ---@return integer
    lcm = function(a, b) return 0 end,
    ---@param x number
    ---@param y number
    ---@return number
    hypot = function(x, y) return 0 end,
    ---@param a number
    ---@param b number
    ---@param rel? number
    ---@param absTol? number
    ---@return boolean
    isclose = function(a, b, rel, absTol) return false end,
    ---@param x number
    ---@return integer|nil
    tointeger = function(x) return nil end,
}

-- ---------------------------------------------------------------------------
-- debug library
-- ---------------------------------------------------------------------------

debug = {
    --- Build a stack traceback string, optionally prefixed by `message`.
    --- `level` skips that many frames (default 1).
    ---@param message? string
    ---@param level? integer
    ---@return string
    traceback = function(message, level) return "" end,

    --- Describe a function or a stack level. Returns a table with `what`,
    --- `source`, `short_src`, `currentline`, `linedefined`, `lastlinedefined`,
    --- `nparams` and `name` where available.
    ---@param target function|integer
    ---@return table
    getinfo = function(target) return {} end,

    --- Return the global registry (the interpreter's global table).
    ---@return table
    getregistry = function() return {} end,
}

-- ---------------------------------------------------------------------------
-- socket library
-- ---------------------------------------------------------------------------

socket = {
    --- Open a TCP connection. Returns the connection, its input stream and its
    --- output stream.
    ---@param url string `socket://host:port`
    ---@return SocketConnection conn
    ---@return Stream input
    ---@return Stream output
    connect = function(url) return {}, {}, {} end,

    --- Address and port of the remote peer.
    ---@param conn SocketConnection
    ---@return string address
    ---@return integer port
    peer = function(conn) return "", 0 end,

    --- Address and port of the local device side.
    ---@param conn SocketConnection
    ---@return string address
    ---@return integer port
    device = function(conn) return "", 0 end,

    --- Listen on a port.
    ---@param port integer
    ---@return SocketServer server
    server = function(port) return {} end,

    --- Accept the next client. Returns the connection, its input stream and its
    --- output stream.
    ---@param server SocketServer
    ---@return SocketConnection conn
    ---@return Stream input
    ---@return Stream output
    accept = function(server) return {}, {}, {} end,

    --- HTTP client helpers. Headers are an optional table of request headers.
    --- The `r*` variants return the raw InputStream instead of the body.
    http = {
        ---@param url string
        ---@param headers? table<string, string>
        ---@return string body
        ---@return integer code
        get = function(url, headers) return "", 200 end,

        ---@param url string
        ---@param data string
        ---@param headers? table<string, string>
        ---@return string body
        ---@return integer code
        post = function(url, data, headers) return "", 200 end,

        ---@param url string
        ---@param headers? table<string, string>
        ---@return Stream body
        ---@return integer code
        rget = function(url, headers) return {}, 200 end,

        ---@param url string
        ---@param data string
        ---@param headers? table<string, string>
        ---@return Stream body
        ---@return integer code
        rpost = function(url, data, headers) return {}, 200 end,
    },
}

-- ---------------------------------------------------------------------------
-- audio library
-- ---------------------------------------------------------------------------

audio = {
    --- Create a prefetched player for an audio file. The default MIME type is
    --- "audio/mpeg". Returns nil when the file cannot be read.
    ---@param file string
    ---@param mimeType? string
    ---@return AudioPlayer|nil
    load = function(file, mimeType) return nil end,

    --- Start/resume playback.
    ---@param player AudioPlayer
    ---@return integer status
    play = function(player) return 0 end,

    --- Pause playback.
    ---@param player AudioPlayer
    ---@return integer status
    pause = function(player) return 0 end,

    --- Get (or set, with a level) the player volume.
    ---@param player AudioPlayer
    ---@param level? integer
    ---@return integer level
    volume = function(player, level) return 0 end,

    --- Total duration in seconds, or -1 when unknown.
    ---@param player AudioPlayer
    ---@return number
    duration = function(player) return -1 end,

    --- Get (or seek to, with seconds) the media time. Returns -1 when unknown.
    ---@param player AudioPlayer
    ---@param seconds? number
    ---@return number|integer
    time = function(player, seconds) return -1 end,
}

-- ---------------------------------------------------------------------------
-- base64 library
-- ---------------------------------------------------------------------------

base64 = {
    --- Encode a string or an array-like table of byte values (0-255).
    ---@param data string|table<integer, integer>
    ---@return string encoded
    encode = function(data) return "" end,

    --- Decode a Base64 string into an array-like table of byte values. With a
    --- second argument it returns a ByteArrayInputStream instead. Returns nil
    --- for invalid input.
    ---@param data string
    ---@param asStream? boolean
    ---@return table<integer, integer>|Stream|nil
    decode = function(data, asStream) return {} end,
}

-- ---------------------------------------------------------------------------
-- push library (PushRegistry)
-- ---------------------------------------------------------------------------

push = {
    --- Register a push connection. `sender` is accepted for compatibility but
    --- ignored by the runtime.
    ---@param connection string
    ---@param filter string
    ---@param midletClass string
    ---@param sender? string
    ---@return boolean ok
    register = function(connection, filter, midletClass, sender) return true end,

    --- Remove a push connection.
    ---@param connection string
    ---@return boolean ok
    unregister = function(connection) return true end,

    --- List registered connections whose name starts with `filter` ("*" lists
    --- every connection).
    ---@param filter string
    ---@return table<integer, string>
    list = function(filter) return {} end,

    --- Whether the MIDlet was launched by a pending push connection.
    ---@return boolean
    pending = function() return false end,

    --- Schedule a MIDlet alarm at an absolute time in milliseconds. Returns the
    --- time of the next alarm.
    ---@param midletClass string
    ---@param time integer
    ---@return integer nextAlarm
    setAlarm = function(midletClass, time) return 0 end,
}

-- ---------------------------------------------------------------------------
-- java library
-- ---------------------------------------------------------------------------

java = {
    --- Whether the given Java class is available in this VM.
    ---@param name string
    ---@return boolean
    class = function(name) return false end,

    --- Name and version of the Java VM.
    ---@return string
    getName = function() return "JVM" end,

    --- Really remove a key from a table. Scripts can set a Lua value to nil,
    --- but a table coming from Java keeps a nil marker; this removes it.
    ---@param t table
    ---@param key any
    delete = function(t, key) end,

    --- Run a function on a background thread (does not block the shell).
    ---@param fn function
    ---@param threadName? string
    run = function(fn, threadName) end,

    --- Name of the current thread.
    ---@return string
    thread = function() return "MIDlet" end,

    --- Sleep the current thread for `ms` milliseconds.
    ---@param ms integer
    sleep = function(ms) end,

    --- Release the interpreter for `ms` milliseconds so queued UI callbacks can
    --- run (default 20). Used by background jobs that update a screen.
    ---@param ms? integer
    yield = function(ms) end,

    --- Read-only information about the running MIDlet.
    midlet = {
        --- Default/login user name.
        username = "user",
        --- OpenTTY build string (e.g. "2026-1.19-04x55").
        build = "2026-1.19-04x55",
        --- Whether this build is the lite ELF interpreter.
        lite = false,
        --- Milliseconds since the MIDlet started.
        ---@return number
        uptime = function() return 0 end,
    },
}

-- ---------------------------------------------------------------------------
-- graphics library
-- ---------------------------------------------------------------------------

--- A text row appended to a Form.
---@class AppendTextItem
---@field type "text"
---@field label? string
---@field value? string
---@field layout? "default"|"link"|"button"
---@field style? string font spec ("monospace bold large")

--- An image appended to a Form.
---@class AppendImageItem
---@field type "image"
---@field img ImageHandle|string

--- An inline button with its own click handler.
---@class AppendButtonItem
---@field type "item"
---@field root function handler receiving no arguments
---@field label string
---@field style? string

--- A choice group appended to a Form.
---@class AppendChoiceItem
---@field type "choice"
---@field label? string
---@field mode? "exclusive"|"multiple"|"popup"
---@field options table<integer, string>
---@field root? function selection state handler

--- A text field appended to a Form.
---@class AppendFieldItem
---@field type "field"
---@field label? string
---@field value? string
---@field length? integer
---@field mode? string "password", "number", "email", "phone", "decimal"

--- Blank space appended to a Form.
---@class AppendSpacerItem
---@field type "spacer"
---@field width? integer
---@field height? integer

--- A gauge appended to a Form (or used as an Alert indicator).
---@class AppendGaugeItem
---@field type "gauge"
---@field label? string
---@field interactive? boolean
---@field maxValue? integer
---@field value? integer

--- Any item table accepted by graphics.append(form, item).
---@alias AppendItem AppendTextItem|AppendImageItem|AppendButtonItem|AppendChoiceItem|AppendFieldItem|AppendSpacerItem|AppendGaugeItem

graphics = {
    --- Current-screen handle shared by apps (survives between programs).
    ---@type table
    db = {},

    --- Value passed to a List select handler; use as the key of a handler
    --- action table to handle the List "Select" command.
    ---@type integer
    fire = 8,

    --- Gauge values for continuous indicators (maxValue = 0).
    ---@type integer
    CONTINUOUS_IDLE = 0,
    ---@type integer
    INCREMENTAL_IDLE = 1,
    ---@type integer
    CONTINUOUS_RUNNING = 2,
    ---@type integer
    INCREMENTAL_UPDATING = 3,

    --- Show a screen handle (or a raw screen object). An Alert may be followed
    --- by the screen to show when it closes.
    ---@param screen ScreenHandle
    ---@param next? ScreenHandle
    display = function(screen, next) end,

    --- Create a screen, command or item.
    ---
    --- Types: "screen" (alias "form") for a Form, "list" for a List, "edit"
    --- (alias "textbox") for a TextBox, "alert" for an Alert, "command" for a
    --- Command, "buffer" for a StringItem, "field" for a TextField and
    --- "gauge" for a Gauge. Screens come back as handles (see ScreenHandle).
    ---@param kind string
    ---@param titleOrOptions string|table
    ---@param content? string|table
    ---@param options? table
    ---@return ScreenHandle|ScreenCommand|ScreenItem
    new = function(kind, titleOrOptions, content, options) return {} end,

    --- Load an image from a file path.
    ---@param path string
    ---@return ImageHandle|nil
    render = function(path) return nil end,

    --- Append an item to a Form or a row to a List. Strings are appended as
    --- text. For a List the optional third argument is the row image. Also
    --- available as `screen:append(item [, image])`.
    ---@param screen ScreenHandle
    ---@param item ScreenItem|AppendItem|string
    ---@param image? ImageHandle|string
    append = function(screen, item, image) end,

    --- Add a command to a screen. Also available as `screen:addCommand(cmd)`.
    ---@param screen ScreenHandle
    ---@param command ScreenCommand
    addCommand = function(screen, command) end,

    --- Install a command listener. `actions` either maps Command objects
    --- (created by graphics.new("command", ...)) to handler functions, or is a
    --- single function called for every command as
    --- `fn(screen, label, ...values)`.
    ---@param screen ScreenHandle
    ---@param actions table<ScreenCommand, fun(...): any>|fun(screen: ScreenHandle, label: string, ...): any
    handler = function(screen, actions) end,

    --- Currently displayed screen handle.
    ---@return ScreenHandle|nil
    getCurrent = function() return nil end,

    --- Read (no title) or change the screen title.
    ---@param screen ScreenHandle
    ---@param title? string
    ---@return string|nil
    SetTitle = function(screen, title) return title end,

    --- Read (no text) or set (nil clears) the screen ticker.
    ---@param screen ScreenHandle
    ---@param text? string
    ---@return string|nil
    SetTicker = function(screen, text) return text end,

    --- Lowercase alias of SetTitle.
    ---@param screen ScreenHandle
    ---@param title? string
    ---@return string|nil
    title = function(screen, title) return title end,

    --- Lowercase alias of SetTicker.
    ---@param screen ScreenHandle
    ---@param text? string
    ---@return string|nil
    ticker = function(screen, text) return text end,

    --- Vibrate the device for `ms` milliseconds (default 500).
    ---@param ms? integer
    vibrate = function(ms) end,

    --- Change the label of an item.
    ---@param item ScreenItem
    ---@param label? string
    SetLabel = function(item, label) end,

    --- Change the text/value of a StringItem, TextField or TextBox (an "edit"
    --- screen handle wraps a TextBox).
    ---@param item ScreenItem|ScreenHandle
    ---@param text string
    SetText = function(item, text) end,

    --- Read the label of an item.
    ---@param item ScreenItem
    ---@return string|nil
    GetLabel = function(item) return nil end,

    --- Read the text/value of a StringItem, TextField or TextBox (an "edit"
    --- screen handle wraps a TextBox).
    ---@param item ScreenItem|ScreenHandle
    ---@return string
    GetText = function(item) return "" end,

    --- Remove every item from a Form or List. Also `screen:clear()`.
    ---@param screen ScreenHandle
    clear = function(screen) end,

    --- Open the system task manager.
    taskmngr = function() end,

    --- Attach a non-interactive Gauge as an Alert indicator. Must be called
    --- before the alert is displayed. Also `alert:SetIndicator(gauge)`.
    ---@param alert ScreenHandle
    ---@param gauge ScreenItem
    SetIndicator = function(alert, gauge) end,

    --- Change a gauge value. The update is posted to the UI thread; with a
    --- maxValue of 0 the value is one of the CONTINUOUS_* constants.
    ---@param gauge ScreenItem
    ---@param value integer
    SetGauge = function(gauge, value) end,

    --- Read a gauge value.
    ---@param gauge ScreenItem
    ---@return integer
    GetGauge = function(gauge) return 0 end,
}

-- ---------------------------------------------------------------------------
-- graphics.canvas (free-form drawing surface, see docs/LUACANVAS.md)
-- ---------------------------------------------------------------------------

--- Options accepted by graphics.canvas.new. Every callback can also be given
--- here instead of assigned to the handle later.
---@class CanvasOptions
---@field title? string
---@field background? integer|string
---@field foreground? integer|string
---@field font? string
---@field fullscreen? boolean
---@field paint? fun(canvas: CanvasHandle, width: integer, height: integer)
---@field keyPressed? fun(canvas: CanvasHandle, keyCode: integer, gameAction: integer)
---@field keyReleased? fun(canvas: CanvasHandle, keyCode: integer)
---@field keyRepeated? fun(canvas: CanvasHandle, keyCode: integer, gameAction: integer)
---@field pointerPressed? fun(canvas: CanvasHandle, x: integer, y: integer)
---@field pointerReleased? fun(canvas: CanvasHandle, x: integer, y: integer)
---@field pointerDragged? fun(canvas: CanvasHandle, x: integer, y: integer)
---@field show? fun(canvas: CanvasHandle, width: integer, height: integer)
---@field hide? fun(canvas: CanvasHandle, width: integer, height: integer)
---@field size? fun(canvas: CanvasHandle, width: integer, height: integer)
---@field tick? fun(canvas: CanvasHandle, dtMs: integer)
---@field command? fun(canvas: CanvasHandle, label: string)

graphics.canvas = {
    --- Create a canvas handle from an options table (or with no arguments).
    ---@param options? CanvasOptions
    ---@return Canvas
    new = function(options) return {} end,

    --- Pack an RGB triple into a 0xRRGGBB number.
    ---@param r integer
    ---@param g integer
    ---@param b integer
    ---@return integer
    rgb = function(r, g, b) return 0 end,

    --- Install a callback (same names as CanvasOptions).
    ---@param canvas Canvas
    ---@param event string
    ---@param fn function
    ---@return Canvas
    on = function(canvas, event, fn) return canvas end,

    --- Remove a callback.
    ---@param canvas Canvas
    ---@param event string
    ---@return Canvas
    off = function(canvas, event) return canvas end,

    --- Show the canvas.
    ---@param canvas Canvas
    ---@return Canvas
    display = function(canvas) return canvas end,

    --- Restore the previous screen.
    ---@param canvas Canvas
    ---@return Canvas
    close = function(canvas) return canvas end,

    --- Read (no text) or change the screen title.
    ---@param canvas Canvas
    ---@param text? string
    ---@return string|Canvas
    title = function(canvas, text) return canvas end,

    --- Read (no argument) or change fullscreen mode.
    ---@param canvas Canvas
    ---@param on? boolean
    ---@return boolean
    fullscreen = function(canvas, on) return false end,

    --- Canvas size in pixels.
    ---@param canvas Canvas
    ---@return integer width
    ---@return integer height
    size = function(canvas) return 0, 0 end,

    --- Request a repaint of the whole canvas or of a rectangle.
    ---@param canvas Canvas
    ---@param x? integer
    ---@param y? integer
    ---@param width? integer
    ---@param height? integer
    ---@return Canvas
    repaint = function(canvas, x, y, width, height) return canvas end,

    --- Repaint immediately.
    ---@param canvas Canvas
    ---@return Canvas
    flush = function(canvas) return canvas end,

    --- Fill the whole canvas or a rectangle with the background color.
    ---@param canvas Canvas
    ---@param x? integer
    ---@param y? integer
    ---@param width? integer
    ---@param height? integer
    ---@return Canvas
    clear = function(canvas, x, y, width, height) return canvas end,

    --- Read (no color) or set the pen color. A color is 0xRRGGBB, "#RRGGBB",
    --- a color name, or three r, g, b arguments.
    ---@param canvas Canvas
    ---@param color? integer|string
    ---@param g? integer
    ---@param b? integer
    ---@return integer|Canvas
    color = function(canvas, color, g, b) return canvas end,

    --- Read (no color) or set the background color.
    ---@param canvas Canvas
    ---@param color? integer|string
    ---@return integer|Canvas
    background = function(canvas, color) return canvas end,

    --- Read (no color) or set the foreground color (also sets the pen color).
    ---@param canvas Canvas
    ---@param color? integer|string
    ---@return integer|Canvas
    foreground = function(canvas, color) return canvas end,

    --- Read (no spec) or set the font. A spec is words separated by spaces
    --- ("monospace bold large"); a face/style/size triple also works.
    ---@param canvas Canvas
    ---@param spec? string|integer
    ---@param style? integer
    ---@param size? integer
    ---@return string|Canvas
    font = function(canvas, spec, style, size) return canvas end,

    --- Font height and baseline offset.
    ---@param canvas Canvas
    ---@return integer height
    ---@return integer baseline
    metrics = function(canvas) return 0, 0 end,

    --- Width of a string in the current font.
    ---@param canvas Canvas
    ---@param text string
    ---@return integer
    textWidth = function(canvas, text) return 0 end,

    --- Width of one character in the current font.
    ---@param canvas Canvas
    ---@param char string|integer
    ---@return integer
    charWidth = function(canvas, char) return 0 end,

    --- Draw text at x, y with an optional anchor (number or words such as
    --- "center", "top left", "bottom right").
    ---@param canvas Canvas
    ---@param text string
    ---@param x integer
    ---@param y integer
    ---@param anchor? integer|string
    ---@return Canvas
    text = function(canvas, text, x, y, anchor) return canvas end,

    --- Draw text at the cursor and advance it.
    ---@param canvas Canvas
    ---@param text string
    ---@return Canvas
    print = function(canvas, text) return canvas end,

    --- Move the text cursor to the next line.
    ---@param canvas Canvas
    ---@return Canvas
    nl = function(canvas) return canvas end,

    --- Erase characters behind the cursor.
    ---@param canvas Canvas
    ---@param count? integer
    ---@return Canvas
    backspace = function(canvas, count) return canvas end,

    --- Read (no coordinates) or set (returns the handle) the text cursor.
    ---@param canvas Canvas
    ---@param x? integer
    ---@param y? integer
    ---@return integer x
    ---@return integer y
    cursor = function(canvas, x, y) return 0, 0 end,

    --- Draw a caret at the text cursor.
    ---@param canvas Canvas
    ---@return Canvas
    caret = function(canvas) return canvas end,

    ---@param canvas Canvas
    ---@param x1 integer
    ---@param y1 integer
    ---@param x2 integer
    ---@param y2 integer
    ---@return Canvas
    line = function(canvas, x1, y1, x2, y2) return canvas end,

    --- Draw (or fill, with `fill`) a rectangle.
    ---@param canvas Canvas
    ---@param x integer
    ---@param y integer
    ---@param width integer
    ---@param height integer
    ---@param fill? boolean
    ---@return Canvas
    rect = function(canvas, x, y, width, height, fill) return canvas end,

    --- Draw (or fill) a rounded rectangle.
    ---@param canvas Canvas
    ---@param x integer
    ---@param y integer
    ---@param width integer
    ---@param height integer
    ---@param arcWidth integer
    ---@param arcHeight integer
    ---@param fill? boolean
    ---@return Canvas
    roundRect = function(canvas, x, y, width, height, arcWidth, arcHeight, fill) return canvas end,

    --- Draw (or fill) an arc. Angles are degrees, clockwise, 0 = 3 o'clock.
    ---@param canvas Canvas
    ---@param x integer
    ---@param y integer
    ---@param width integer
    ---@param height integer
    ---@param startAngle integer
    ---@param arcAngle integer
    ---@param fill? boolean
    ---@return Canvas
    arc = function(canvas, x, y, width, height, startAngle, arcAngle, fill) return canvas end,

    --- Draw (or fill) a circle around cx, cy.
    ---@param canvas Canvas
    ---@param cx integer
    ---@param cy integer
    ---@param radius integer
    ---@param fill? boolean
    ---@return Canvas
    circle = function(canvas, cx, cy, radius, fill) return canvas end,

    --- Draw (or fill) the ellipse inscribed in the rectangle.
    ---@param canvas Canvas
    ---@param x integer
    ---@param y integer
    ---@param width integer
    ---@param height integer
    ---@param fill? boolean
    ---@return Canvas
    ellipse = function(canvas, x, y, width, height, fill) return canvas end,

    --- Draw (or fill) a triangle from three points.
    ---@param canvas Canvas
    ---@param x1 integer
    ---@param y1 integer
    ---@param x2 integer
    ---@param y2 integer
    ---@param x3 integer
    ---@param y3 integer
    ---@param fill? boolean
    ---@return Canvas
    triangle = function(canvas, x1, y1, x2, y2, x3, y3, fill) return canvas end,

    --- Draw (or fill) a polygon. Points are a flat {x1, y1, x2, y2, ...} array
    --- or an array of {x=, y=} / {x, y} tables.
    ---@param canvas Canvas
    ---@param points table
    ---@param fill? boolean
    ---@return Canvas
    polygon = function(canvas, points, fill) return canvas end,

    --- Draw one pixel.
    ---@param canvas Canvas
    ---@param x integer
    ---@param y integer
    ---@return Canvas
    pixel = function(canvas, x, y) return canvas end,

    --- Draw an image (handle or file path) at x, y with an optional anchor.
    ---@param canvas Canvas
    ---@param image ImageHandle|string
    ---@param x integer
    ---@param y integer
    ---@param anchor? integer|string
    ---@return Canvas
    image = function(canvas, image, x, y, anchor) return canvas end,

    --- Restrict drawing to a rectangle.
    ---@param canvas Canvas
    ---@param x integer
    ---@param y integer
    ---@param width integer
    ---@param height integer
    ---@return Canvas
    clip = function(canvas, x, y, width, height) return canvas end,

    --- Restore the full-canvas clip.
    ---@param canvas Canvas
    ---@return Canvas
    resetClip = function(canvas) return canvas end,

    --- Current clip rectangle.
    ---@param canvas Canvas
    ---@return integer x
    ---@return integer y
    ---@return integer width
    ---@return integer height
    clipRect = function(canvas) return 0, 0, 0, 0 end,

    --- Move the drawing origin.
    ---@param canvas Canvas
    ---@param dx integer
    ---@param dy integer
    ---@return Canvas
    translate = function(canvas, dx, dy) return canvas end,

    --- Push the drawing state (color, font, clip, translation).
    ---@param canvas Canvas
    ---@return Canvas
    save = function(canvas) return canvas end,

    --- Pop the last saved drawing state.
    ---@param canvas Canvas
    ---@return Canvas
    restore = function(canvas) return canvas end,

    --- Reset to the initial drawing state.
    ---@param canvas Canvas
    ---@return Canvas
    reset = function(canvas) return canvas end,

    --- Export the buffer as a PNG. Returns 0 on success and -1 when the runtime
    --- cannot read Image pixels (always -1 on a real MIDP device).
    ---@param canvas Canvas
    ---@param path string
    ---@return integer
    saveImage = function(canvas, path) return -1 end,

    --- Whether a key is currently held.
    ---@param canvas Canvas
    ---@param keyCode integer
    ---@return boolean
    keyDown = function(canvas, keyCode) return false end,

    --- Human-readable name of a key code.
    ---@param canvas Canvas
    ---@param keyCode integer
    ---@return string
    keyName = function(canvas, keyCode) return "" end,

    --- Game action mapped to a key code (0 when none).
    ---@param canvas Canvas
    ---@param keyCode integer
    ---@return integer
    gameAction = function(canvas, keyCode) return 0 end,

    --- Array of the key codes currently held.
    ---@param canvas Canvas
    ---@return table<integer, integer>
    keys = function(canvas) return {} end,

    --- Clear the held-key set.
    ---@param canvas Canvas
    ---@return Canvas
    resetKeys = function(canvas) return canvas end,

    --- Last pointer position.
    ---@param canvas Canvas
    ---@return integer x
    ---@return integer y
    pointer = function(canvas) return 0, 0 end,

    --- Block until a key is pressed (outside a loop). Returns the key code and
    --- game action, or nil on timeout. A timeout of 0 (the default) waits
    --- forever.
    ---@param canvas Canvas
    ---@param timeoutMs? integer
    ---@return integer keyCode
    ---@return integer gameAction
    waitKey = function(canvas, timeoutMs) return 0, 0 end,

    --- Run a blocking frame loop on the script thread. `fn(canvas, dtMs)` is
    --- called once per frame; `delayMs` is the target frame time (default 20).
    ---@param canvas Canvas
    ---@param fn? fun(canvas: Canvas, dtMs: integer)
    ---@param delayMs? integer
    ---@return Canvas
    loop = function(canvas, fn, delayMs) return canvas end,

    --- Run the same frame loop on a background thread.
    ---@param canvas Canvas
    ---@param fn? fun(canvas: Canvas, dtMs: integer)
    ---@param delayMs? integer
    ---@return Canvas
    animate = function(canvas, fn, delayMs) return canvas end,

    --- Stop a running loop.
    ---@param canvas Canvas
    ---@return Canvas
    stop = function(canvas) return canvas end,

    --- Whether a loop is running.
    ---@param canvas Canvas
    ---@return boolean
    running = function(canvas) return false end,

    --- Milliseconds since the current loop started.
    ---@param canvas Canvas
    ---@return integer
    age = function(canvas) return 0 end,

    --- Add a command to the canvas. `type` is one of "screen" (default),
    --- "back", "cancel", "ok", "help", "stop", "exit" or "item"; `fn` is
    --- called with the canvas and the command label. Without `fn` the canvas
    --- `command` callback runs instead.
    ---@param canvas Canvas
    ---@param label string
    ---@param type? string
    ---@param priority? integer
    ---@param fn? fun(canvas: Canvas, label: string)
    ---@return Canvas
    command = function(canvas, label, type, priority, fn) return canvas end,

    --- Remove every command from the canvas.
    ---@param canvas Canvas
    ---@return Canvas
    clearCommands = function(canvas) return canvas end,

    --- Anchor constants (MIDP Graphics bits). Words such as "center",
    --- "top left" and "bottom right" are also accepted by drawing methods.
    anchor = {
        HCENTER = 1,
        VCENTER = 2,
        CENTER = 3,   -- HCENTER | VCENTER
        LEFT = 4,
        RIGHT = 8,
        TOP = 16,
        BOTTOM = 32,
        BASELINE = 64,
    },

    --- MIDP key codes.
    key = {
        UP = 1,
        DOWN = 6,
        LEFT = 2,
        RIGHT = 5,
        FIRE = 8,
        GAME_A = 9,
        GAME_B = 10,
        GAME_C = 11,
        GAME_D = 12,
        NUM0 = 48,
        NUM1 = 49,
        NUM2 = 50,
        NUM3 = 51,
        NUM4 = 52,
        NUM5 = 53,
        NUM6 = 54,
        NUM7 = 55,
        NUM8 = 56,
        NUM9 = 57,
        STAR = 42,
        POUND = 35,
    },

    --- MIDP game actions.
    action = {
        UP = 1,
        DOWN = 6,
        LEFT = 2,
        RIGHT = 5,
        FIRE = 8,
        GAME_A = 9,
        GAME_B = 10,
        GAME_C = 11,
        GAME_D = 12,
    },

    --- MIDP font face/style/size constants (numeric Font.getFont arguments).
    fonts = {
        SYSTEM = 0,
        MONOSPACE = 32,
        PROPORTIONAL = 64,
        PLAIN = 0,
        BOLD = 1,
        ITALIC = 2,
        UNDERLINED = 4,
        SMALL = 8,
        MEDIUM = 0,
        LARGE = 16,
    },

    --- Named colors, also accepted as strings by every color argument.
    colors = {
        black = 0x000000,
        white = 0xFFFFFF,
        red = 0xFF0000,
        green = 0x00FF00,
        blue = 0x0000FF,
        yellow = 0xFFFF00,
        cyan = 0x00FFFF,
        magenta = 0xFF00FF,
        gray = 0x808080,
        grey = 0x808080,
        orange = 0xFF8000,
        purple = 0x800080,
        brown = 0x804000,
        pink = 0xFF80C0,
        lime = 0x80FF00,
        navy = 0x000080,
        teal = 0x008080,
        silver = 0xC0C0C0,
        maroon = 0x800000,
        olive = 0x808000,
    },
}

-- Canvas handles are tables whose metatable holds the Java object and uses
-- graphics.canvas as __index. Both call styles work:
--
--     local c = graphics.canvas.new({ title = "Game" })
--     c:text("hi", 4, 4)                  -- method style
--     graphics.canvas.text(c, "hi", 4, 4) -- module style
--
-- Writing a function to a callback name installs it; writing a value to
-- title/font/background/foreground/color applies the property:
--
--     c.paint       = function(canvas, w, h) end
--     c.keyPressed  = function(canvas, key, action) end
--     c.title       = "New title"
--     c.font        = "proportional bold small"
--     c.background  = "#202030"
--     c.foreground  = "white"
--     c.color       = 0x00FF00
--
-- Callbacks: paint, keyPressed, keyReleased, keyRepeated, pointerPressed,
-- pointerReleased, pointerDragged, show, hide, size, tick and command.
