#!/bin/lua

local version = "2.1.0"

os.setproc("name", "nc")

local function usage()
    print("nc (netcat) v" .. version .. " - OpenTTY Network Terminal")
    print("")
    print("Usage:")
    print("  nc [host] [port]          Connect to remote host:port")
    print("  nc -l [port]              Listen on port (server mode)")
    print("  nc -h                     Show this help")
    print("")
    print("The terminal sends each entered line to the remote endpoint.")
    print("Use Switch to... to open the task manager without disconnecting.")
end

local function run_terminal(host, port, is_server)
    local previous = graphics.getCurrent()
    local screen = graphics.new("screen", "nc " .. host .. ":" .. port)
    local back = graphics.new("command", { label = "Disconnect", type = "screen", priority = 1 })
    local clear = graphics.new("command", { label = "Clear", type = "screen", priority = 1 })
    local run = graphics.new("command", { label = "Send", type = "ok", priority = 1 })
    local switch = graphics.new("command", { label = "Switch to...", type = "screen", priority = 2 })

    local buffer = graphics.new("buffer", { label = "", value = "", style = "monospace" })
    local field = graphics.new("field", { label = host .. ":" .. port .. " >", value = "", length = 256, mode = "" })

    local running = true
    local connected = false
    local conn = nil
    local inp = nil
    local out = nil
    local server = nil

    local function append_output(text)
        local current = graphics.GetText(buffer) or ""
        graphics.SetText(buffer, current .. text)
    end

    graphics.append(screen, buffer)
    graphics.append(screen, field)
    graphics.addCommand(screen, run)
    graphics.addCommand(screen, back)
    graphics.addCommand(screen, clear)
    graphics.addCommand(screen, switch)

    graphics.handler(screen, {
        [back] = function()
            running = false
            if conn then pcall(io.close, conn) end
            if server then pcall(io.close, server) end
            graphics.display(previous)
            os.exit(0)
        end,
        [clear] = function()
            graphics.SetText(buffer, "")
        end,
        [run] = function(command)
            if command and command ~= "" then
                if not connected or not out then
                    append_output("[nc] Not connected yet.\n")
                    return
                end
                local ok, err = pcall(io.write, command .. "\n", out)
                if ok then
                    append_output("> " .. command .. "\n")
                    graphics.SetText(field, "")
                else
                    append_output("[nc] Send failed: " .. tostring(err) .. "\n")
                end
            end
        end,
        [switch] = graphics.taskmngr
    })

    os.setproc("screen", screen)
    os.setproc("stdout", buffer)
    graphics.display(screen)

    java.run(function()
        if is_server then
            append_output("[nc] Listening on port " .. tostring(port) .. "...\n")
            local ok, value = pcall(socket.server, port)
            if not ok then
                append_output("[nc] Listen failed: " .. tostring(value) .. "\n")
                return
            end
            server = value
            append_output("[nc] Waiting for a connection...\n")
            local accepted, c, i, o = pcall(socket.accept, server)
            if not accepted then
                if running then append_output("[nc] Accept failed: " .. tostring(c) .. "\n") end
                return
            end
            conn, inp, out = c, i, o
            pcall(io.close, server)
            server = nil
        else
            append_output("[nc] Connecting to " .. host .. ":" .. tostring(port) .. "...\n")
            local ok, c, i, o = pcall(socket.connect, "socket://" .. host .. ":" .. tostring(port))
            if not ok then
                append_output("[nc] Connection failed: " .. tostring(c) .. "\n")
                return
            end
            conn, inp, out = c, i, o
        end

        connected = true
        append_output("[nc] Connected.\n")
        while running do
            local ok, data = pcall(io.read, inp, 1024)
            if not ok or not data or data == "" then
                connected = false
                if running then append_output("\n[nc] Remote closed the connection.\n") end
                break
            end
            append_output(data)
        end
    end)
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
        run_terminal("0.0.0.0", port, true)
    else
        local port = tonumber(arg[2])
        if not port then
            print("nc: invalid port: " .. tostring(arg[2]))
            os.exit(2)
        end
        run_terminal(a1, port, false)
    end
else
    usage()
    os.exit(2)
end
