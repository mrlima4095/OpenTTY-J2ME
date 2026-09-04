#!/bin/lua

local forge = {
    version = "1.3"
}


if arg[1] == "--deamon" then
    os.setproc("name", "forge")
    return function (payload, args, pid, id)
        print("Payload '" .. payload .. "' recieved by forge")
    end
end
if arg[0] == "/lib/forge" then
    print("Forge v" .. forge.version .. " API")
    print("Usage: require('forge')")
end

return forge