#!/bin/lua

local version = "1.0.0"

os.setproc("name", "nc")

if arg[1] and arg[2] then
    local remote, port = arg[1], arg[2]
    local running = true

    local ok, conn, i, o = pcall(socket.connect, "socket://" .. remote .. ":" .. port)
    if not ok then
        print("nc: " .. tostring(conn))
        os.exit(101)
    end

    local previous = graphics.getCurrent()
    local screen = graphics.new("screen", "OpenTTY " .. os.getenv("VERSION"))
    local back = graphics.new("command", { label = "Back", type = "screen", priority = 1 })
    local clear = graphics.new("command", { label = "Clear", type = "screen", priority = 1 })
    local info = graphics.new("command", { label = "View info", type = "screen", priority = 1 })
    local run = graphics.new("command", { label = "Send", type = "ok", priority = 1 })
    local buffer = graphics.new("buffer", { })

    java.run(function ()
        while running do
            local x, response = pcall(io.read, i)
            if x then
                io.write(response, buffer, "a")
            end
        end
    end, "Background")

    graphics.append(screen, buffer)
    graphics.append(screen, { type = "field", label = "Remote (" .. remote .. ")", })
    graphics.addCommand(screen, run)
    graphics.addCommand(screen, back)
    graphics.addCommand(screen, clear)
    graphics.addCommand(screen, info)
    graphics.handler({
        [back] = function ()
            graphics.display(previous)
            running = false
            os.exit(0)
        end,
        [clear] = function () graphics.SetText(buffer, "") end,
        [info] = function () graphics.display(graphics.new("alert", "Informations", "")) end,
        [run] = function (payload) pcall(io.write, payload, o) end
    })
    graphics.display(screen)

end