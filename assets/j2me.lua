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
    exit = function (status) os.exit(status, true) end,
    --[=[Get current date-time]=]
    date = function () end,
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
    getuid = function () return 1000 end,
    --[=[Make a request to Device API to perform opening of URI]=]
    open = function (uri) return true end,
    --[=[Change current scope]=]
    scope = function (...) return {} end,
    sudo = function (password) return 0 end,
    --[=[Change current user]=]
    su = function (username, password) return 0 end,
    --[=[Delete files]=]
    remove = function (file) return 0 end,
    --[=[Build PATH questions]=]
    join = function (pwd) return "/bin/sh" end
}

package = { loadlib = function (libname, funcname) end, loaded = {} }

io = {
    --[=[Reads a file or stream]=]
    read = function (file, lenght) end,
    --[=[Writes on a file or stream]=]
    write = function (text, file, mode) return 0 end,
    --[=[Closes files, streams, and connections]=]
    close = function (...) end,
    --[=[Opens applications and manipulates it]=]
    popen = function (program, argument, sudo, out, scope) return 0, "" end,
    --[=[Returns directory content]=]
    dirs = function (pwd) return { ".." } end,
    --[=[Set program stdout]=]
    setstdout = function (file) end,
    --[=[Mount a file system or device]=]
    mount = function (struct) end,
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
    peer = function (conn) return "opentty.xyz", 31522 end,
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
    upper = function (s) end,
    lower = function (s) end,
    len = function (s) end,
    find = function (s, pattern, init, plain) end,
    match = function (s, pattern) end,
    reverse = function (s) end,
    sub = function (s, x, y) end,
    hash = function (s) end,
    byte = function (s, x, y) end,
    char = function (...) end,
    trim = function (s) return "" end,
    uuid = function () return "" end,
    split = function (s, char) return "" end,
    getCommand = function(s) return "" end,
    getArgument = function(s) return "" end,
    env = function(s) return "" end
}

--[=[
Lua J2ME - Graphics API
]=]
graphics = {
    --[=[Set current screen]=]
    display = function (...) end,
    --[=[Generate new Screen Object (`alert`, `screen`, `list`, `edit`)]=]
    new = function (type, title, ...) end,
    --[=[Set title of screen]=]
    SetTitle = function (screen, title) end,
    --[=[Set ticker of screen]=]
    SetTicker = function (scrren, text) end,
    --[=[Set label of Item]=]
    SetLabel = function (field, text) end,
    --[=[Set text of Item or Editor Screen]=]
    SetText = function (field, text) end,
    --[=[Get label of Item]=]
    GetLabel = function (field, text) end,
    --[=[Get text of Item or Editor Screen]=]
    GetText = function (field, text) end,
    --[=[Get current displayed scrren]=]
    getCurrent = function () end,
    --[=[Render Image]=]
    render = function (img) end,
    --[=[Append itens on screen]=]
    append = function (screen, field) end,
    --[=[Add buttons `Command` in screen]=]
    addCommand = function (screen, command) end,
    --[=[Set `Command Listener` for screen]=]
    handler = function (screen, actions) end,
    --[=[Table to save screens across programs]=]
    db = {}
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
    --[=[Run ARM 32 ELF binaries]=]
    elf = function (elf) end,

    --[=[MIDlet Lua representation]=]
    midlet = {
        --[=[MIDlet Default User Name]=]
        username = "myuser",
        --[=[Cached Content from Archive Strucutres]=]
        cache = { ["/bin/shprxy"] = "Cached file" },
        --[=[OpenTTY Build Code]=]
        build = "2025-1.17-02x96"
    }
}

--[=[Get a MIDlet Property Value]=]
getAppProperty = function (field) end
--[=[Run code and returns value of code, inject scopes with a table on scope]=]
load = function (code, scope) end