os = {
    execute = function (command) return 0 end,
    getenv = function (key) return nil or key end,
    clock = function () end,
    setlocale = function (locale) end,
    exit = function (status) os.exit(status, true) end,
    date = function () end,
    getpid = function (proc) end,
    getproc = function (pid, field) return {} end,
    setproc = function (field, value) end,
    getcwd = function () return "/home/" end
}

package = { loadlib = function (libname, funcname) end, loaded = {} }

io = {
    read = function (text, file) end,
    write = function (text, file, mode) end,
    close = function (...) end
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
    connect = function (address) end,
    server = function (port) end,
    accept = function (server) end,

    peer = function (conn) end,
    device = function (conn) end,

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
    trim = function (s) return "" end
}

graphics = {
    display = function (screen) end,
    Alert = function (config) end,
    BuildScreen = function (config) end,
    BuildList = function (config) end,
    BuildQuest = function (config) end,
    BuildEdit = function (config) end,
    SetTitle = function (title) end,
    WindowTitle = function (title, screen) end,
    SetTicker = function (text) end,
    getCurrent = function () end,
    render = function (img) end,
    append = function (screen, field) end,
    xterm = ""
}

java = {
    class = function (name) return true end,
    getName = function () return "JVM" end
}

getAppProperty = function (field) end
random = function (max) end