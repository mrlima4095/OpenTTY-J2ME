#!/bin/lua

local version = "3.0.0"

os.setproc("name", "nc")

local function usage()
    print("nc (netcat) v" .. version .. " - OpenTTY Network Terminal")
    print("Usage:")
    print("  nc HOST PORT       Connect to a remote TCP endpoint")
    print("  nc -l PORT         Listen for one TCP client")
end

local function new_terminal(title, prompt)
    local previous = graphics.getCurrent()
    local screen = graphics.new("screen", title)
    local output = graphics.new("buffer", { label = "", value = "", style = "monospace" })
    local input = graphics.new("field", { label = prompt, value = "", length = 256, mode = "" })
    local send = graphics.new("command", { label = "Send", type = "ok", priority = 1 })
    local clear = graphics.new("command", { label = "Clear", type = "screen", priority = 1 })
    local disconnect = graphics.new("command", { label = "Disconnect", type = "screen", priority = 1 })
    local switch = graphics.new("command", { label = "Switch to...", type = "screen", priority = 2 })
    local running, connected = true, false
    local conn, stream_in, stream_out, listener = nil, nil, nil, nil

    local function write_output(text)
        graphics.SetText(output, (graphics.GetText(output) or "") .. text)
    end

    local function close_session()
        running = false
        connected = false
        if conn then pcall(io.close, conn) end
        if listener then pcall(io.close, listener) end
        conn, stream_in, stream_out, listener = nil, nil, nil, nil
    end

    local function attach(connection, input_stream, output_stream)
        if not running then
            pcall(io.close, connection)
            return
        end
        conn, stream_in, stream_out = connection, input_stream, output_stream
        connected = true
        write_output("[nc] Connected.\n")

        java.run(function()
            while running and stream_in do
                local ok, data = pcall(io.read, stream_in, 1024)
                if ok and data and data ~= "" then
                    write_output(data)
                else
                    -- The MIDP stream API reports EOF and transient read errors alike.
                    -- Keep the interactive session open until the user disconnects.
                    java.sleep(100)
                end
            end
        end)
    end

    graphics.append(screen, output)
    graphics.append(screen, input)
    graphics.addCommand(screen, send)
    graphics.addCommand(screen, clear)
    graphics.addCommand(screen, disconnect)
    graphics.addCommand(screen, switch)
    graphics.handler(screen, {
        [send] = function(command)
            if command and command ~= "" then
                if not connected or not stream_out then
                    write_output("[nc] Not connected yet.\n")
                    return
                end
                local ok, err = pcall(io.write, command .. "\n", stream_out)
                if ok then
                    write_output("> " .. command .. "\n")
                    graphics.SetText(input, "")
                else
                    write_output("[nc] Send failed: " .. tostring(err) .. "\n")
                end
            end
        end,
        [clear] = function() graphics.SetText(output, "") end,
        [disconnect] = function()
            close_session()
            graphics.display(previous)
            os.exit(0)
        end,
        [switch] = graphics.taskmngr
    })
    os.setproc("screen", screen)
    os.setproc("stdout", output)
    graphics.display(screen)
    return attach, close_session, function() return running end, function(value) listener = value end, write_output
end

local function start_client(host, port)
    local attach, close_session, running, set_listener, write_output = new_terminal("nc " .. host .. ":" .. port, host .. ":" .. port .. " >")
    java.run(function()
        write_output("[nc] Client: connecting to " .. host .. ":" .. tostring(port) .. "...\n")
        local ok, conn, input, output = pcall(socket.connect, "socket://" .. host .. ":" .. tostring(port))
        if not ok then
            write_output("[nc] Connection failed: " .. tostring(conn) .. "\n")
            return
        end
        attach(conn, input, output)
    end)
end

local function start_server(port)
    local attach, close_session, running, set_listener, write_output = new_terminal("nc listen :" .. port, "client:" .. port .. " >")
    java.run(function()
        write_output("[nc] Server: listening on port " .. tostring(port) .. "...\n")
        local ok, server = pcall(socket.server, port)
        if not ok then
            write_output("[nc] Listen failed: " .. tostring(server) .. "\n")
            return
        end
        set_listener(server)
        write_output("[nc] Waiting for one client...\n")
        local accepted, conn, input, output = pcall(socket.accept, server)
        if not accepted then
            if running() then write_output("[nc] Accept failed: " .. tostring(conn) .. "\n") end
            return
        end
        pcall(io.close, server)
        set_listener(nil)
        attach(conn, input, output)
    end)
end

if arg[1] == "-l" then
    local port = tonumber(arg[2])
    if not port then print("nc: invalid port") else start_server(port) end
elseif arg[1] and arg[2] then
    local port = tonumber(arg[2])
    if not port then print("nc: invalid port") else start_client(tostring(arg[1]), port) end
else
    usage()
end
