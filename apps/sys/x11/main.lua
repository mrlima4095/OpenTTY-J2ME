#!/bin/lua

os.setproc("name", "x11")

if arg[1] == "screen" then
elseif arg[1] == "list" then   
elseif arg[1] == "quest" then   
elseif arg[1] == "edit" then   
elseif arg[1] == "version" then
else
    local screen = graphics.new("screen", "X.Org")
end