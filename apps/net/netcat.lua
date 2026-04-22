#!/bin/lua

local version = "1.0.0"

os.setproc("name", "nc")

if arg[1] and arg[2] then
    local remote, port = arg[1], arg[2]
    local running = false

    print("conectando")
    local ok, conn, i, o = pcall(socket.connect, "socket://" .. remote .. ":" .. port)
    if not ok then
        print("nc: " .. tostring(conn))
        os.exit(101)
    else
        print("ok, running")
        running = true
    end
    print("salvou tela")
    local previous = graphics.getCurrent()
    print("criou tela")
    local screen = graphics.new("screen", "OpenTTY " .. os.getenv("VERSION"))
    print("botao voltar")
    local back = graphics.new("command", { label = "Back", type = "screen", priority = 1 })
    print("botao limpar")
    local clear = graphics.new("command", { label = "Clear", type = "screen", priority = 1 })
    print("botao enviar")
    local run = graphics.new("command", { label = "Send", type = "ok", priority = 1 })
    print("buffer de saida")
    local buffer = graphics.new("buffer", { })

    print("subiu bg")
    java.run(function()
        while running do local x, response = pcall(io.read, i) if x then io.write(response, buffer, "a") end end
    end)

    print("mostrando itens")
    local label = "Remote (" .. remote .. ")"
    graphics.append(screen, buffer)
    graphics.append(screen, { type = "field", label = label })
    print("mostrando botoes")
    graphics.addCommand(screen, run)
    graphics.addCommand(screen, back)
    graphics.addCommand(screen, clear)
    graphics.handler(screen, {
        [back] = function ()
            print("voltou")
            graphics.display(previous)
            running = false
            os.exit(0)
        end,
        [clear] = function () graphics.SetText(buffer, "") end,
        [run] = function (payload) pcall(io.write, payload, o) end
    })
    print("mostrou")
    graphics.display(screen)
else
    print("nc: usage: nc [host] [port]")
    os.exit(2)
end