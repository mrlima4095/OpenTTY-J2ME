#!/bin/lua

if arg[1] and arg[2] then
    local content = io.read(os.join(arg[2]))
    local pattern = arg[1]

    print(string.find(content, pattern) and "true" or "false")
else
    print("grep: usage: grep [pattern] [file]")
    os.exit(2)
end
