#!/bin/lua

local version = "1.0.0"

os.setproc("name", "nc")

if arg[1] and arg[2] then
    local remote, port = arg[1], arg[2]
    local running = false

    print("")
    local ok, conn, i, o = pcall(socket.connect, "socket://" .. remote .. ":" .. port)
    if not ok then
        print("nc: " .. tostring(conn))
        os.exit(101)
    else
        print("")
        running = true
    end
    print("")
    local previous = graphics.getCurrent()
    print("")
    local screen = graphics.new("screen", "OpenTTY " .. os.getenv("VERSION"))
    print("")
    local back = graphics.new("command", { label = "Back", type = "screen", priority = 1 })
    print("")
    local clear = graphics.new("command", { label = "Clear", type = "screen", priority = 1 })
    print("")
    local run = graphics.new("command", { label = "Send", type = "ok", priority = 1 })
    print("")
    local buffer = graphics.new("buffer", { })

    print("")
    java.run(function ()
        while running do local x, response = pcall(io.read, i) if x then io.write(response, buffer, "a") end end
    end)

    print("")
    graphics.append(screen, buffer)
    graphics.append(screen, { type = "field", label = "Remote (" .. remote .. ")" })
    print("")
    graphics.addCommand(screen, run)
    graphics.addCommand(screen, back)
    graphics.addCommand(screen, clear)
    graphics.handler(screen, {
        [back] = function ()
            print("")
            graphics.display(previous)
            running = false
            os.exit(0)
        end,
        [clear] = function () graphics.SetText(buffer, "") end,
        [run] = function (payload) pcall(io.write, payload, o) end
    })
    print("")
    graphics.display(screen)
else
    print("nc: usage: nc [host] [port]")
    os.exit(2)
end