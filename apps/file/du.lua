#!/bin/lua

if arg[1] then
    for i = 0, #arg - 1 do
        local file = io.open(os.join(arg[i]))
        if file then
            print(#file .. "\t" .. arg[i])
        else
            print("du: " .. arg[i] .. ": not found")
            os.exit(127)
        end
    end
else
    print("du: usage: du [file]")
end