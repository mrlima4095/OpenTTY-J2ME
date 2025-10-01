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
    upper = function (text) end,
    lower = function (text) end,
    len = function (text) end,
    match = function (text, pattern) end,
    reverse = function (text) end,
    sub = function (text, x, y) end,
    hash = function (text) end,
    byte = function (text, x, y) end,
    char = function (...) end,
    trim = function (text) return "" end
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
    render = function (img) end
}

getAppProperty = function (field) end
random = function (max) end