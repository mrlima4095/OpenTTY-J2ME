#!/bin/lua

local version = "2.0.0"

os.setproc("name", "nc")

local function usage()
    print("nc (netcat) v" .. version .. " - OpenTTY Chat/Network Client")
    print("")
    print("Usage:")
    print("  nc [host] [port]          Connect to remote host:port")
    print("  nc -l [port]              Listen on port (server mode)")
    print("  nc -h                     Show this help")
    print("")
    print("In chat mode:")
    print("  Type a message and press Send to transmit")
    print("  Use 'Back' to disconnect and return")
    print("  Use 'Clear' to clear the chat log")
end

local function run_chat(host, port, is_server)
    local previous = graphics.getCurrent()
    local screen = graphics.new("screen", "nc " .. host .. ":" .. port)
    local back = graphics.new("command", { label = "Back", type = "screen", priority = 1 })
    local clear = graphics.new("command", { label = "Clear", type = "screen", priority = 1 })
    local run = graphics.new("command", { label = "Send", type = "ok", priority = 1 })
    local switch = graphics.new("command", { label = "Switch to...", type = "screen", priority = 2 })

    local buffer = graphics.new("buffer", { label = "Chat", value = "", style = "monospace" })
    local field = graphics.new("field", { label = "Message", value = "", length = 256, mode = "" })

    local running = true
    local conn = nil
    local inp = nil
    local out = nil
    local peer = host .. ":" .. port

    local function add_line(text)
        local current = graphics.GetText(buffer) or ""
        if current ~= "" then
            graphics.SetText(buffer, current .. "\n" .. text)
        else
            graphics.SetText(buffer, text)
        end
    end

    if is_server then
        add_line("[server] Listening on port " .. tostring(port) .. "...")
        add_line("[server] Waiting for connection...")

        local ok, srv = pcall(socket.server, port)
        if not ok then
            add_line("[error] Failed to listen: " .. tostring(srv))
            graphics.display(previous)
            return
        end
        local ok2, a_conn, a_inp, a_out = pcall(socket.accept, srv)
        if not ok2 then
            add_line("[error] Accept failed: " .. tostring(a_conn))
            graphics.display(previous)
            return
        end
        conn = a_conn
        inp = a_inp
        out = a_out
        peer = "client"
        add_line("[server] Client connected!")
    else
        add_line("[chat] Connecting to " .. host .. ":" .. tostring(port) .. "...")
        local ok, c, i, o = pcall(socket.connect, "socket://" .. host .. ":" .. tostring(port))
        if not ok then
            add_line("[error] Connection failed: " .. tostring(c))
            graphics.display(previous)
            return
        end
        conn = c
        inp = i
        out = o
        add_line("[chat] Connected! Start chatting.")
    end

    java.run(function()
        while running do
            local ok, data = pcall(io.read, inp, 1024)
            if ok and data and data ~= "" then
                add_line(peer .. ": " .. data)
            end
        end
    end)

    graphics.append(screen, buffer)
    graphics.append(screen, field)
    graphics.addCommand(screen, run)
    graphics.addCommand(screen, back)
    graphics.addCommand(screen, clear)
    graphics.addCommand(screen, switch)

    graphics.handler(screen, {
        [back] = function()
            running = false
            pcall(io.close, conn, inp, out)
            graphics.display(previous)
        end,
        [clear] = function()
            graphics.SetText(buffer, "")
        end,
        [run] = function(msg)
            if msg and msg ~= "" then
                local ok, err = pcall(io.write, msg, out)
                if ok then
                    add_line("me: " .. msg)
                    graphics.SetText(field, "")
                else
                    add_line("[error] Failed to send: " .. tostring(err))
                end
            end
        end,
        [switch] = graphics.taskmngr
    })

    os.setproc("screen", screen)
    graphics.display(screen)
end

if arg[1] and arg[2] then
    local a1 = tostring(arg[1])
    if a1 == "-h" or a1 == "--help" then
        usage()
        os.exit(0)
    elseif a1 == "-l" then
        local port = tonumber(arg[2])
        if not port then
            print("nc: invalid port: " .. tostring(arg[2]))
            os.exit(2)
        end
        run_chat("0.0.0.0", port, true)
    else
        local port = tonumber(arg[2])
        if not port then
            print("nc: invalid port: " .. tostring(arg[2]))
            os.exit(2)
        end
        run_chat(a1, port, false)
    end
else
    usage()
    os.exit(2)
end
