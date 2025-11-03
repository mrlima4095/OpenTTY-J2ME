#!/bin/lua

local app = {
    proxy = getAppProperty("MIDlet-Proxy") or "http://opentty.xyz/proxy.php?"
}

function app.install(rock)
    if rock == nil then print("luarocks: install: missing rock name") end
    local ok, body, status = pcall(socket.http.get, "")
end
function app.build()
    
end
function app.download(rock)
    
end
function app.purge()
    
end
function app.remove()
    
end
function app.search()
    
end
function app.show()
    
end
function app.config()
    
end
function app.doc()
    
end



if arg[1] == nil or #arg[1] == "help" then print("Usage: luarocks [options]")
elseif arg[1] == "install" then app.install(arg[2])
else print("luarocks: " .. arg[1] .. ": not found")
end
