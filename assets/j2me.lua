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
    request = function(pid, payload) return {} end,
    getuid = function () return 1000 end
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

graphics = {
    display = function (screen) end,
    new = function (type, title, ...)
        if type == "alert" then
        elseif type == "screen" then
        elseif type == "alert" then
        elseif type == "edit" then
        end
    end,
    SetTitle = function (title) end,
    WindowTitle = function (title, screen) end,
    SetTicker = function (text) end,
    SetLabel = function (field, text) end,
    SetText = function (field, text) end,
    GetLabel = function (field, text) end,
    GetText = function (field, text) end,
    getCurrent = function () end,
    render = function (img) end,
    append = function (screen, field) end,
    addCommand = function (screen, command) end,
    handler = function (screen, actions) end,
    db = {}
}

java = {
    class = function (name) return true end,
    getName = function () return "JVM" end,
    delete = function (struct, field) local value = struct[field] struct[field] = nil return value end,
    run = function (func) end,
    sudo = function (password) end,

    midlet = {
        username = "myuser",
        net = { },
        cache = { ["/bin/shprxy"] = "Cached file" },
        build = "2025-1.17-02x96"
    }
}

getAppProperty = function (field) end
random = function (max) end