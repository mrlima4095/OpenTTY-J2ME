#!/bin/lua

if arg[1] and arg[2] then
    local content = table.decode(io.read(os.join(arg[2])))
    local pattern = arg[1]

    print(content[pattern] or "null")
else
    print("find: usage: find [pattern] [file]")
    os.exit(2)
end
