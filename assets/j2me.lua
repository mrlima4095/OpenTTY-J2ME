os = {
    execute = function (command, sudo) return 0 end,
    getenv = function (...) return nil or ... end,
    setenv = function (key, value) return value end,
    clock = function () end,
    setlocale = function (locale) end,
    exit = function (status) os.exit(status, true) end,
    date = function () end,
    getpid = function (proc) end,
    getproc = function (pid, field) return {} end,
    setproc = function (field, value) end,
    getcwd = function () return "/home/" end,
    chdir = function (pwd) return 0 end,
    request = function(pid, payload, args) return {} end,
    getuid = function () return 1000 end,
    open = function (uri) return true end,
    scope = function (...) return {} end,
    sudo = function (password) return 0 end,
    su = function (username, password) return 0 end,
    remove = function (file) return 0 end
}

package = { loadlib = function (libname, funcname) end, loaded = {} }

io = {
    read = function (file, lenght) end,
    write = function (text, file, mode) return 0 end,
    close = function (...) end,
    popen = function (program, argument, sudo, out, scope) return 0, "" end,
    dirs = function (pwd) return {} end,
    setstdout = function (file) end,
    mount = function (struct) end
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
    connect = function (address) return "conn", "inputstream", "outputstream" end,
    server = function (port) return "server" end,
    accept = function (server) end,

    peer = function (conn) return "opentty.xyz", 31522 end,
    device = function (conn) return "127.0.0.1", 31522 end,

    http = {
        get = function (url, headers) return "", 200 end,
        post = function (url, data, headers) return "", 200 end
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
    new = function (type, title, ...)
        if type == "alert" then
        elseif type == "screen" then
        elseif type == "list" then
        elseif type == "edit" then
        end
    end,
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
        --[=[Registred Network Connections]=]
        net = { },
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