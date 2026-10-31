os = {
    --[=[Execute command in installed shell]=]
    execute = function (command, ...) return 0 end,
    --[=[Get an environment value]=]
    getenv = function (...) return nil or ... end,
    --[=[Set an environment value]=]
    setenv = function (key, value) return value end,
    --[=[How much time this process is active]=]
    clock = function () end,
    --[=[Set locale of System]=]
    setlocale = function (locale) end,
    --[=[Exit from program or MIDlet]=]
    exit = function (status) end,
    --[=[Get current date-time]=]
    date = function () end,
    --[=[Get or set Unix time]=]
    time = function (fields) return 0 end,
    --[=[Get Process ID current process or another]=]
    getpid = function (proc) end,
    --[=[Get data of a process]=]
    getproc = function (pid, field) return {} end,
    --[=[Set a information of current process]=]
    setproc = function (field, value) end,
    --[=[Returns current path]=]
    getcwd = function () return "/home/" end,
    --[=[Change current working directory]=]
    chdir = function (pwd) return 0 end,
    --[=[Make a request to a Service API]=]
    request = function(pid, payload, args) return {} end,
    --[=[Get UID of current user]=]
    getuid = function (user) return 1000 end,
    --[=[Make a request to Device API to perform opening of URI]=]
    open = function (uri) return true end,
    --[=[Change current scope]=]
    scope = function (...) return {} end,
    --[=[Change current user]=]
    su = function (username, password) return 0 end,
    --[=[Delete files]=]
    remove = function (file) return 0 end,
    --[=[Build PATH questions]=]
    join = function (pwd) return "/bin/sh" end,
    --[=[Create directories]=]
    mkdir = function (path) return 0 end
}

package = { loadlib = function (libname, funcname) end, loaded = {} }

io = {
    --[=[Current stdout sink]=]
    stdout = nil,
    --[=[Reads a file or stream]=]
    read = function (file, length) end,
    --[=[Writes on a file or stream]=]
    write = function (text, file, mode) return 0 end,
    --[=[Closes files, streams, and connections]=]
    close = function (...) end,
    --[=[Opens a file or returns a new output buffer]=]
    open = function (path) end,
    --[=[Opens applications and manipulates it]=]
    popen = function (program, argument, sudo, out, scope, stream) return 0, "" end,
    --[=[Returns directory content]=]
    dirs = function (pwd) return { ".." } end,
    --[=[Set program stdout]=]
    setstdout = function (file) end,
    --[=[Mount a file system or device]=]
    mount = function (struct) end,
    --[=[Create an in-memory string buffer]=]
    new = function () return {} end,
    --[=[Copy a stream into a file]=]
    copy = function (stream, file) end
}

table = {
    insert = function (table, field) end,
    concat = function (table, char) end,
    remove = function (table, field) end,
    sort = function (table) end,
    move = function (table, from, to) end,
    pack = function () end,
    unpack = function () end,
    decode = function (text) return {} end
}

socket = {
    --[=[Connect with Socket]=]
    connect = function (address) return "conn", "inputstream", "outputstream" end,
    --[=[Open a Server]=]
    server = function (port) return "server" end,
    --[=[Accept clients on Server]=]
    accept = function (server) end,

    --[=[Get informations about other side of connection]=]
    peer = function (conn) return "opentty.fun", 31522 end,
    --[=[Get informations about current device on connection]=]
    device = function (conn) return "127.0.0.1", 31522 end,

    --[=[HTTP library]=]
    http = {
        --[=[Make a HTTP GET request]=]
        get = function (url, headers) return "", 200 end,
        --[=[Make a HTTP POST request]=]
        post = function (url, data, headers) return "", 200 end,
        --[=[Make a HTTP GET request, returns the stream object]=]
        rget = function (url, headers) return "", 200 end,
        --[=[Make a HTTP POST request, returns the stream object]=]
        rpost = function (url, data, headers) return "", 200 end
    }
}

string = {
    --[=[Change all characters to upper case]=]
    upper = function (s) end,
    --[=[Change all characters to lower case]=]
    lower = function (s) end,
    --[=[Returns length of string]=]
    len = function (s) end,
    --[=[]=]
    find = function (s, pattern, init, plain) end,
    --[=[]=]
    match = function (s, pattern) end,
    --[=[Reverse string]=]
    reverse = function (s) end,
    --[=[Get parts of string]=]
    sub = function (s, x, y) end,
    --[=[Get string hash]=]
    hash = function (s) end,
    --[=[Get bytes of string]=]
    byte = function (s, x, y) end,
    --[=[Get a string from a byte array]=]
    char = function (...) end,
    --[=[Removes spaces from start and end of stringf]=]
    trim = function (s) return "" end,
    --[=[Generates a random UUID]=]
    uuid = function () return "" end,
    --[=[Split string in parts]=]
    split = function (s, char) return {} end,
    --[=[Get main part from string]=]
    getCommand = function(s) return "" end,
    --[=[Get secundary part of string]=]
    getArgument = function(s) return "" end,
    --[=[Process environment keys on string]=]
    env = function(s) return "" end,
    --[=[Clear patterns of screen]=]
    getpattern = function (s) return "" end,
    --[=[Check if string starts with a prefix]=]
    startswith = function (s, prefix) return false end,
    --[=[Check if string ends with a suffix]=]
    endswith = function (s, suffix) return false end
}

math = {
    --[=[Generate a random number (`random()`, `random(n)` or `random(m, n)`)]=]
    random = function (m, n) return 0 end
}

debug = {
    --[=[Build a stack traceback string]=]
    traceback = function (message, level) return "" end,
    --[=[Describe a function or stack level]=]
    getinfo = function (target) return {} end,
    --[=[Return the global registry]=]
    getregistry = function () return {} end
}

--[=[
Lua J2ME - Graphics API
]=]
graphics = {
    --[=[Set current screen]=]
    display = function (...) end,
    --[=[Generate new Screen Object (`alert`, `screen`, `list`, `edit`, `command`, `buffer`, `field`, `gauge`)]=]
    new = function (type, title, ...) end,
    --[=[Set title of screen]=]
    SetTitle = function (screen, title) end,
    --[=[Set ticker of screen]=]
    SetTicker = function (scrren, text) end,
    --[=[Read or set title of screen]=]
    title = function (screen, title) end,
    --[=[Read or set ticker of screen]=]
    ticker = function (screen, text) end,
    --[=[Set label of Item]=]
    SetLabel = function (field, text) end,
    --[=[Set text of Item or Editor Screen]=]
    SetText = function (field, text) end,
    --[=[Get label of Item]=]
    GetLabel = function (field) end,
    --[=[Get text of Item or Editor Screen]=]
    GetText = function (field) end,
    --[=[Get current displayed scrren]=]
    getCurrent = function () end,
    --[=[Render Image]=]
    render = function (img) end,
    --[=[Append itens on screen]=]
    append = function (screen, field, ...) end,
    --[=[Clear Screen Contenst]=]
    clear = function (screen) end,
    --[=[Add buttons `Command` in screen]=]
    addCommand = function (screen, command) end,
    --[=[Set `Command Listener` for screen]=]
    handler = function (screen, actions) end,
    --[=[Vibrate the device]=]
    vibrate = function (ms) end,
    --[=[Open the system task manager]=]
    taskmngr = function () end,
    --[=[Set an Alert indicator gauge]=]
    SetIndicator = function (alert, gauge) end,
    --[=[Set value of a Gauge]=]
    SetGauge = function (gauge, value) end,
    --[=[Get value of a Gauge]=]
    GetGauge = function (gauge) return 0 end,
    --[=[Fire Button on List]=]
    fire = 8,
    --[=[Continuous Gauge values]=]
    CONTINUOUS_IDLE = 0,
    INCREMENTAL_IDLE = 1,
    CONTINUOUS_RUNNING = 2,
    INCREMENTAL_UPDATING = 3,
    --[=[Table to save screens across programs]=]
    db = {},

    --[=[
    Free-form drawing surface. Handles are tables whose __index is this
    table, so `c:text(...)` and `graphics.canvas.text(c, ...)` are the same.
    Callbacks: paint, keyPressed, keyReleased, keyRepeated, pointerPressed,
    pointerReleased, pointerDragged, show, hide, size, tick and command.
    Writing title/font/background/foreground/color applies the property.
    ]=]
    canvas = {
        --[=[Create a canvas from an options table]=]
        new = function (options) return {} end,
        --[=[Pack an RGB triple into 0xRRGGBB]=]
        rgb = function (r, g, b) return 0 end,
        --[=[Install a callback]=]
        on = function (canvas, event, fn) end,
        --[=[Remove a callback]=]
        off = function (canvas, event) end,
        --[=[Show the canvas]=]
        display = function (canvas) end,
        --[=[Restore the previous screen]=]
        close = function (canvas) end,
        --[=[Read or set the screen title]=]
        title = function (canvas, text) end,
        --[=[Read or set fullscreen mode]=]
        fullscreen = function (canvas, on) end,
        --[=[Get canvas width and height]=]
        size = function (canvas) end,
        --[=[Request a repaint]=]
        repaint = function (canvas, x, y, width, height) end,
        --[=[Repaint immediately]=]
        flush = function (canvas) end,
        --[=[Fill the canvas or a rectangle]=]
        clear = function (canvas, x, y, width, height) end,
        --[=[Read or set the pen color]=]
        color = function (canvas, color, g, b) end,
        --[=[Read or set the background color]=]
        background = function (canvas, color) end,
        --[=[Read or set the foreground color]=]
        foreground = function (canvas, color) end,
        --[=[Read or set the font]=]
        font = function (canvas, spec, style, size) end,
        --[=[Get font height and baseline]=]
        metrics = function (canvas) end,
        --[=[Get the width of a string]=]
        textWidth = function (canvas, text) return 0 end,
        --[=[Get the width of one character]=]
        charWidth = function (canvas, char) return 0 end,
        --[=[Draw text at x, y]=]
        text = function (canvas, text, x, y, anchor) end,
        --[=[Draw text at the cursor and advance it]=]
        print = function (canvas, text) end,
        --[=[Move the cursor to the next line]=]
        nl = function (canvas) end,
        --[=[Erase characters behind the cursor]=]
        backspace = function (canvas, count) end,
        --[=[Read or set the text cursor]=]
        cursor = function (canvas, x, y) end,
        --[=[Draw a caret at the cursor]=]
        caret = function (canvas) end,
        --[=[Draw a line]=]
        line = function (canvas, x1, y1, x2, y2) end,
        --[=[Draw or fill a rectangle]=]
        rect = function (canvas, x, y, width, height, fill) end,
        --[=[Draw or fill a rounded rectangle]=]
        roundRect = function (canvas, x, y, width, height, arcWidth, arcHeight, fill) end,
        --[=[Draw or fill an arc]=]
        arc = function (canvas, x, y, width, height, startAngle, arcAngle, fill) end,
        --[=[Draw or fill a circle]=]
        circle = function (canvas, cx, cy, radius, fill) end,
        --[=[Draw or fill an ellipse]=]
        ellipse = function (canvas, x, y, width, height, fill) end,
        --[=[Draw or fill a triangle]=]
        triangle = function (canvas, x1, y1, x2, y2, x3, y3, fill) end,
        --[=[Draw or fill a polygon]=]
        polygon = function (canvas, points, fill) end,
        --[=[Draw one pixel]=]
        pixel = function (canvas, x, y) end,
        --[=[Draw an image]=]
        image = function (canvas, image, x, y, anchor) end,
        --[=[Restrict drawing to a rectangle]=]
        clip = function (canvas, x, y, width, height) end,
        --[=[Restore the full-canvas clip]=]
        resetClip = function (canvas) end,
        --[=[Get the current clip rectangle]=]
        clipRect = function (canvas) end,
        --[=[Move the drawing origin]=]
        translate = function (canvas, dx, dy) end,
        --[=[Push the drawing state]=]
        save = function (canvas) end,
        --[=[Pop the last saved drawing state]=]
        restore = function (canvas) end,
        --[=[Reset to the initial drawing state]=]
        reset = function (canvas) end,
        --[=[Export the buffer as a PNG]=]
        saveImage = function (canvas, path) return -1 end,
        --[=[Check whether a key is held]=]
        keyDown = function (canvas, keyCode) return false end,
        --[=[Get the name of a key code]=]
        keyName = function (canvas, keyCode) return "" end,
        --[=[Map a key code to a game action]=]
        gameAction = function (canvas, keyCode) return 0 end,
        --[=[Get the held key codes]=]
        keys = function (canvas) return {} end,
        --[=[Clear the held-key set]=]
        resetKeys = function (canvas) end,
        --[=[Wait for a key press]=]
        waitKey = function (canvas, timeoutMs) end,
        --[=[Get the last pointer position]=]
        pointer = function (canvas) end,
        --[=[Run a blocking frame loop]=]
        loop = function (canvas, fn, delayMs) end,
        --[=[Run a frame loop on a background thread]=]
        animate = function (canvas, fn, delayMs) end,
        --[=[Stop a running loop]=]
        stop = function (canvas) end,
        --[=[Check whether a loop is running]=]
        running = function (canvas) return false end,
        --[=[Get milliseconds since the loop started]=]
        age = function (canvas) return 0 end,
        --[=[Add a command to the canvas]=]
        command = function (canvas, label, type, priority, fn) end,
        --[=[Remove every canvas command]=]
        clearCommands = function (canvas) end,

        --[=[Anchor constants]=]
        anchor = {
            HCENTER = 1,
            VCENTER = 2,
            CENTER = 3,
            LEFT = 4,
            RIGHT = 8,
            TOP = 16,
            BOTTOM = 32,
            BASELINE = 64
        },
        --[=[MIDP key codes]=]
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
            POUND = 35
        },
        --[=[MIDP game actions]=]
        action = {
            UP = 1,
            DOWN = 6,
            LEFT = 2,
            RIGHT = 5,
            FIRE = 8,
            GAME_A = 9,
            GAME_B = 10,
            GAME_C = 11,
            GAME_D = 12
        },
        --[=[MIDP font face/style/size constants]=]
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
            LARGE = 16
        },
        --[=[Named colors]=]
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
            olive = 0x808000
        }
    }
}

--[=[
Audio API
]=]
audio = {
    --[=[Get player from audio file]=]
    load = function (file, mimeType) end,
    --[=[Start player]=]
    play = function (player) end,
    --[=[Pause player]=]
    pause = function (player) end,
    --[=[Get or set player volume]=]
    volume = function (player, level) end,
    --[=[Get total durantion of audio on player]=]
    duration = function (player) end,
    --[=[Get or set player current time]=] 
    time = function (player, seek) end,
}

--[=[
Base64 J2ME API
]=]
base64 = {
    --[=[Encode data to Base64]=]
    encode = function (data) end,
    --[=[Decode Base64 to table with bytes]=]
    decode = function (table) return {} end
}

--[=[ 
Lua J2ME - Control MIDlet Suite with Lua
]=]
java = {
    --[=[Verify if current Runtime supports a Java Class]=]
    class = function (name) return true end,
    --[=[Returns JVM Name]=]
    getName = function () return "JVM" end,
    --[=[When you set a Lua NIL on a table it current exists on Java as nil, usage java.delete to really delete from table]=]
    delete = function (struct, field) local value = struct[field] struct[field] = nil return value end,
    --[=[Run function in another Thread (Background)]=]
    run = function (func, thread_name) end,
    --[=[Get current thread name]=]
    thread = function () return "MIDlet" end,
    --[=[Sleep the current thread]=]
    sleep = function (ms) end,
    --[=[Release the interpreter so queued UI callbacks can run]=]
    yield = function (ms) end,

    --[=[MIDlet Lua representation]=]
    midlet = {
        --[=[MIDlet Default User Name]=]
        username = "myuser",
        --[=[OpenTTY Build Code]=]
        build = "2026-1.17-03x08",
        --[=[Whether this build is the lite ELF interpreter]=]
        lite = false,
        --[=[Returns MIDlet uptime's]=]
        uptime = function () return 0 end
    }
}

--[=[
PushRegister Library
]=]
push = {
    register = function (connection, filter, midlet, sender) end,
    unregister = function (connection) end,
    list = function (filter) end,
    pending = function () end,
    setAlarm = function (midletClass, time) end
}

--[=[Current process arguments (`arg[0]` is the script path)]=]
arg = {}
--[=[Global environment table]=]
_G = _G
--[=[Interpreter version string]=]
_VERSION = "Lua J2ME"
--[=[Numeric uid of the current scope, kept as a string]=]
_UID = "1000"

--[=[Write values to the current process stdout]=]
print = function (...) end
--[=[Raise an error]=]
error = function (message) end
--[=[Call a function in protected mode]=]
pcall = function (f, ...) end
--[=[Asserts that a value is truthy, raises an error otherwise]=]
assert = function (v, msg) if not v then error(msg or "assertion failed!") end return v, msg end
--[=[Load a rock or Lua file from `/lib`]=]
require = function (name) end
--[=[Run code and returns value of code, inject scopes with a table on scope]=]
load = function (code, scope) end
--[=[Alias of load (Lua 5.1 compatibility)]=]
loadstring = function (code, scope, name) end
--[=[Iterate over all key/value pairs]=]
pairs = function (t) end
--[=[Iterate over the array part of a table]=]
ipairs = function (t) end
--[=[Return the next key/value pair of a table]=]
next = function (t, key) end
--[=[Run or query the Java garbage collector]=]
collectgarbage = function (option) end
--[=[Convert a value to a string]=]
tostring = function (value) end
--[=[Convert a value to a number]=]
tonumber = function (value, base) end
--[=[Select arguments from an index]=]
select = function (index, ...) end
--[=[Return the Lua type name of a value]=]
type = function (value) end
--[=[Get a MIDlet Property Value]=]
getAppProperty = function (field) end
--[=[Attach a metatable to a table]=]
setmetatable = function (t, metatable) end
--[=[Return the metatable of a table]=]
getmetatable = function (t) end
--[=[Read a key without consulting __index]=]
rawget = function (t, key) end
--[=[Write a key without consulting __newindex]=]
rawset = function (t, key, value) end
--[=[Compare two values without metamethods]=]
rawequal = function (a, b) return false end
--[=[Length of a table without metamethods]=]
rawlen = function (t) return 0 end
--[=[Return the array elements from `i` to `j`]=]
unpack = function (t, i, j) end
