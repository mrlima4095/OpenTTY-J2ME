#!/bin/lua

if arg[1] == "add" then
    os.request(1, "groupadd", arg[2])
elseif arg[1] == "del" or arg[1] == "delete" then
    os.request(1, "groupdel", arg[2])
else
    print("group: usage: group add <name> | group del <name>")
end
