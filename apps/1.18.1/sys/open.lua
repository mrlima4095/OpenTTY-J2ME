#!/bin/lua

if arg[1] then
    local uri = ""
    for i = 1, #arg do
        uri = uri .. arg[i] .. " "
    end
    uri = string.trim(uri)
    os.setproc("name", "open")
    pcall(os.open, uri)
else
    print("open: usage: open [uri]")
end