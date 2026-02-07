#!/bin/lua

local libcore = require("libcore")

if arg[1] then
    for i = 1, #arg - 1 do
        local status = os.request(1, "useradd", arg[i])
        if status > 0 then
            print("useradd: " .. arg[i] .. ": " .. libcore.errormsg(status))
            os.exit(tonumber(status))
        end
    end
else
    print("useradd: usage: useradd [user...]")
end