#!/bin/lua

local libcore = require("libcore")

if os.getuid() > 0 then
    print("userdel: permission denied")
    os.exit(13)
end

if arg[1] then
    for i = 1, #arg - 1 do
        local status = os.request(1, "userdel", arg[i])
        if status > 0 then
            print("useradd: " .. arg[i] .. ": " .. libcore.errormsg(status))
            os.exit(tonumber(status))
        end
    end
else
    print("userdel: usage: userdel [user...]")
end