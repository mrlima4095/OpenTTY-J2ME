#!/bin/lua

local app, args = {}, {}
local services = { }

function app.__debug(pname)
    local struct = {}

    struct.pid = os.getpid(pname)
    if struct.pid then struct.running = true
    else struct.running = false end


    return struct
end
function app.dumpsys(service)

end



function app.shell()
    if #args < 2 or args[2] == "" then

    elseif args[2] == "dumpsys" then
        if args[3] == nil then
            for k,_ in services do app.dumpsys(k) end
        else app.dumpsys(3) end
    elseif args[2] == "su" then
        
    end
end

function app.connect() end

function app.main()
    for i = 1, #arg do args[i] = arg[i] end

    if #args == 0 or arg[1] == "" then

    elseif args[1] == "shell" then app.shell()
    elseif args[1] == "connect" then app.connect()

    elseif args[1] == "install" then

    elseif args[1] == "uninstall" then

    elseif args[1] == "start" then

    elseif args[1] == "stop" then

    elseif args[1] == "logcat" then
        local file = io.open("/tmp/logs")
        if file then
            print(io.read(file))
        else
            print("no logs on session")
        end
    else print("jdb: " .. args[1] .. ": not found") end
end

app.main()