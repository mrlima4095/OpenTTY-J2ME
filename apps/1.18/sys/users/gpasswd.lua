#!/bin/lua

if arg[1] == "-a" or arg[1] == "-d" then
    if not arg[2] or not arg[3] then print("gpasswd: usage: gpasswd [-a|-d] user group") os.exit(2) end
    local action = arg[1] == "-a" and "add" or "remove"
    local status = os.request(1, "usermod", { ["user"] = arg[2], ["group"] = arg[3], ["action"] = action })
    if status ~= 0 then print("gpasswd: operation failed") os.exit(tonumber(status)) end
elseif arg[1] and arg[2] then
    local status = os.request(1, "gpasswd", { ["group"] = arg[1], ["password"] = arg[2] })
    if status ~= 0 then print("gpasswd: operation failed") os.exit(tonumber(status)) end
else
    print("gpasswd: usage: gpasswd [-a|-d] user group")
end
