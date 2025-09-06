os = {
    execute = function (command) end,
    getenv = function (key) end,
    clock = function () end,
    setlocale = function (locale) end,
    exit = function (status) end,
    getproc = function (pid) end,
    getpid = function (name) end,
    putproc = function (pid, key, item) end,
    running = function (pid) end
}

package = {
    loadlib = function (file) end,
    loaded = {}
}

io = {
    read = function (file) end,
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
