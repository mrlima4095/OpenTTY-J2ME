#!/bin/lua

local shell = require("/bin/sh")
local aliases, scope = {}, {}
local password = arg[1]

local function app()
    local ok, conn, i, o = pcall(socket.connect, "socket://opentty.xyz:4096")
    if not ok then
        print(arg[0] .. ": " .. tostring(conn))
        os.exit(101)
    end

    local address, port = socket.device(conn)
    local _ = io.read(i)

    io.write(password .. "\n", o)
    local id = string.trim(string.sub(io.read(i), 22))

    print("WebProxy ID: " .. id)

    os.setproc("id", id)
    os.setproc("passwd", password)
    os.setproc("name", "web-proxy")

    local bkp_stdout = io.stdout
    io.setstdout(o)

    pcall(function ()
        
        while true do
            local cmd = string.trim(io.read(i))
            
            if cmd then shell(cmd, true, aliases, o, scope)
            else break end
        end
    end)

    io.setstdout(bkp_stdout)

    pcall(io.close, conn, i, o)
    print("WebProxy -> Server disconnected")
end
local function Quest()
    local previous = graphics.getCurrent()
    local screen = graphics.new("screen", "WebProxy")
    local back = graphics.new("command", { label = "Back", type = "back", priority = 1 })
    local connect = graphics.new("command", { label = "Connect", type = "ok", priority = 1 })
    local handler = {
        [back] = function()
            graphics.display(previous)
            os.exit()
        end,
        [connect] = function(passwd)
            if passwd ~= "" and passwd then
                graphics.display(previous)
                password = passwd
                java.run(app)
            else
                graphics.display(graphics.new("alert", "WebProxy", "Missing password"))
            end
        end
    }

    graphics.append(screen, graphics.new("field", { label = "Password for Connection", mode = "password" }))
    graphics.addCommand(screen, back)
    graphics.addCommand(screen, connect)
    graphics.handler(screen, handler)
    graphics.display(screen)
end

if password then java.run(app)
else 
    
end
