#!/bin/lua

if os.getuid() ~= 0 then
    print(":: permission denied")
end

os.setproc("name", "smile")


if arg[1] == "start" then
    if arg[2] then
    else
        print("smile: start: missing service")
        os.exit(2)
    end
elseif arg[1] == "stop" then
    if arg[2] then
    else
        print("smile: stop: missing service")
        os.exit(2)
    end
elseif arg[1] == "reload" then
elseif arg[1] == "restart" then
    if arg[2] then
    else
        print("smile: restart: missing service")
        os.exit(2)
    end
elseif arg[1] == "isolate" then
elseif arg[1] == "kill" then
    if arg[2] then
    else
        print("smile: kill: missing service")
        os.exit(2)
    end
elseif arg[1] == "logs" then
elseif arg[1] == "info" then
    if arg[2] then
    else
        print("smile: info: missing service")
        os.exit(2)
    end
elseif arg[1] == "--deamon" then
    return function (payload, args, scope, pid, uid)

    end
end