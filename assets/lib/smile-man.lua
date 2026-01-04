#!/bin/lua

if os.getuid() ~= 0 then
    print(":: permission denied")
end

os.setproc("name", "smile")


if arg[1] == "start" then
elseif arg[1] == "stop" then
elseif arg[1] == "reload" then
elseif arg[1] == "restart" then
elseif arg[1] == "isolate" then
elseif arg[1] == "kill" then
elseif arg[1] == "logs" then
elseif arg[1] == "info" then
elseif arg[1] == "--deamon" then
    return function (payload, args, scope, pid, uid)

    end
end