os = {
    execute = function (command) end,
    getenv = function (key) end,
    clock = function () end,
    setlocale = function (locale) end,
    exit = function (status) end,
    date = function () end,
    getpid = function (proc) end,
    getproc = function (pid, field) end,
    setproc = function (field, value) end
}

package = { loadlib = function (libname, funcname) end, loaded = {} }

io = { 
    read = function (text, file) end,
    write = function (text, file, mode) end,
    close = function (...) end
}

table = {
    pack = function (...) end,
    decode = function (text) end
}

socket = {
    connect = function (address) end,

    http = {
        get = function (url, headers) end,
        post = function (url, data, headers) end
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
    char = function (...) end
}

graphics = {
    display = function (screen) end,
    Alert = function (config) end,
    BuildScreen = function (config) end,
    BuildList = function (config) end,
    BuildQuest = function (config) end,
    BuildEdit = function (config) end,
    SetTitle = function (title) end,
    WindowTitle = function (title) end,
    SetTicker = function (text) end
}
