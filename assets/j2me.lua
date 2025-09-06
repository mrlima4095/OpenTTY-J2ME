os = {
    execute = function (command) end,
    getenv = function (key) end,
    clock = function () end,
    setlocale = function (locale) end,
    exit = function (status) end
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
    server = function (port) end,
    accept = function (server) end,

    peer = function (conn) end,
    device = function (conn) end,

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

print = function (text) end
error = function (message) end
pcall = function (func, ...) end
require = function (file) end
load = function (text) end
pairs = function (tb) end
collectgarbage = function () end
tostring = function (item) end
tonumber = function (item) end
select = function (index, ...) end
type = function (item) end