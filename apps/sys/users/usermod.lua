#!/bin/lua

if not arg[1] or not arg[2] then print("usermod: usage: usermod user group") os.exit(2) end
local action = "add"
local standard = false
if arg[1] == "-r" then
    action = "remove"
    table.remove(arg, 1)
elseif arg[1] == "-aG" or arg[1] == "-G" then
    standard = true
    table.remove(arg, 1)
end
if not arg[1] or not arg[2] then print("usermod: usage: usermod user group") os.exit(2) end
local user = standard and arg[2] or arg[1]
local group = standard and arg[1] or arg[2]
local status = os.request(1, "usermod", { ["user"] = user, ["group"] = group, ["action"] = action })
if status ~= 0 then print("usermod: operation failed") os.exit(tonumber(status)) end
