#!/bin/lua

os.setproc("name", "x11")

if arg[1] == "screen" then
elseif arg[1] == "list" then
    if arg[2] then
        local file = io.open(os.join(arg[2]))
        if file then
        
        else
            print()
        end
    else
        print("x11: list: missing file")
    end
elseif arg[1] == "quest" then
elseif arg[1] == "edit" then
elseif arg[1] == "version" then
else
    local screen = graphics.new("screen", "X.Org")
end