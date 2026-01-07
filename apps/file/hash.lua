#!/bin/lua

if arg[1] then
    local file = io.read(os.join(arg[1]))
    if file then
        print(arg[1] .. ": " .. string.hash(file))
    else
        print("hash: " .. arg[1] .. ": not found")
        os.exit(127)
    end
else
    print("hash [file]")
end