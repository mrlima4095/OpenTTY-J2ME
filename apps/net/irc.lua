#!/bin/lua

local version = "1.0.0"
local conn, input, output
local connected = false
local current_nick = ""
local current_channel = ""
local channels = {}

local function parse_irc(raw)
    if not raw or raw == "" then return nil end
    local msg = {}
    msg.raw = raw

    if string.sub(raw, 1, 1) == ":" then
        local space1 = string.find(raw, " ")
        if not space1 then return nil end
        msg.prefix = string.sub(raw, 2, space1 - 1)
        local atpos = string.find(msg.prefix, "!")
        if atpos then
            msg.nick = string.sub(msg.prefix, 1, atpos - 1)
            msg.host = string.sub(msg.prefix, atpos + 1)
        else
            msg.nick = msg.prefix
        end
        raw = string.sub(raw, space1 + 1)
    end

    local space1 = string.find(raw, " ")
    if not space1 then
        msg.command = raw
        return msg
    end
    msg.command = string.sub(raw, 1, space1 - 1)
    raw = string.sub(raw, space1 + 1)

    if string.sub(raw, 1, 1) == ":" then
        msg.trailing = string.sub(raw, 2)
        return msg
    end

    local params = {}
    while raw ~= "" do
        local space1 = string.find(raw, " ")
        if space1 then
            table.insert(params, string.sub(raw, 1, space1 - 1))
            raw = string.sub(raw, space1 + 1)
        else
            table.insert(params, raw)
            break
        end
    end
    msg.params = params
    if #params > 0 then
        msg.target = params[1]
    end
    return msg
end

local function send_irc(...)
    if not connected or not output then return false end
    local line = ""
    for i = 1, select("#", ...) do
        local v = select(i, ...)
        if i > 1 then line = line .. " " end
        line = line .. tostring(v)
    end
    local ok = pcall(io.write, line .. "\r\n", output)
    return ok
end

local function format_time()
    local t = os.date("*t")
    local h = tostring(t.hour)
    local m = tostring(t.min)
    if #h < 2 then h = "0" .. h end
    if #m < 2 then m = "0" .. m end
    return h .. ":" .. m
end

local function process_message(msg)
    if not msg or not msg.command then return end
    local cmd = msg.command

    if cmd == "PING" then
        send_irc("PONG", msg.trailing or "")

    elseif cmd == "001" then
        connected = true
        return "[SERVER] Connected as " .. current_nick

    elseif cmd == "376" or cmd == "422" then
        if current_channel ~= "" then
            send_irc("JOIN", current_channel)
        end
        return "[SERVER] Motd received"

    elseif cmd == "JOIN" then
        local ch = msg.trailing or (msg.params and msg.params[1]) or ""
        return "[" .. format_time() .. "] " .. (msg.nick or "???") .. " joined " .. ch

    elseif cmd == "PART" then
        local ch = msg.trailing or (msg.params and msg.params[1]) or ""
        return "[" .. format_time() .. "] " .. (msg.nick or "???") .. " left " .. ch

    elseif cmd == "PRIVMSG" then
        local target = msg.target or ""
        local text = msg.trailing or ""
        local nick = msg.nick or "???"
        if text == "" and msg.params and #msg.params > 1 then
            text = msg.params[2] or ""
        end
        return "[" .. format_time() .. "] <" .. nick .. "> " .. text

    elseif cmd == "NOTICE" then
        local text = msg.trailing or ""
        return "[NOTICE] " .. text

    elseif cmd == "NICK" then
        local newnick = msg.trailing or (msg.params and msg.params[1]) or ""
        return "[" .. format_time() .. "] " .. (msg.nick or "???") .. " is now known as " .. newnick

    elseif cmd == "KICK" then
        local ch = msg.params and msg.params[1] or ""
        local who = msg.params and msg.params[2] or ""
        local reason = msg.trailing or ""
        return "[" .. format_time() .. "] " .. who .. " was kicked from " .. ch .. " (" .. reason .. ")"

    elseif cmd == "QUIT" then
        return "[" .. format_time() .. "] " .. (msg.nick or "???") .. " quit (" .. (msg.trailing or "") .. ")"

    elseif cmd == "353" then
        local names = msg.trailing or ""
        return "[NAMES] " .. names

    elseif cmd == "332" then
        local ch = msg.params and msg.params[2] or ""
        local topic = msg.trailing or ""
        return "[TOPIC] " .. ch .. ": " .. topic

    elseif cmd == "433" then
        return "[ERROR] Nickname already in use"

    elseif cmd == "404" then
        return "[ERROR] Cannot send to channel"

    elseif cmd == "401" then
        return "[ERROR] No such nick/channel"
    end

    return nil
end

local function help()
    print("OpenTTY IRC Client v" .. version)
    print("")
    print("Usage: irc [options]")
    print("")
    print("CLI mode:")
    print("  irc connect <server> <port> <nick> [channel]")
    print("  irc send <server> <port> <nick> <channel> <message>")
    print("")
    print("GUI mode:")
    print("  irc               Open interactive IRC client")
    print("")
    print("Options:")
    print("  /join <channel>   Join a channel")
    print("  /part             Leave current channel")
    print("  /nick <newnick>   Change nickname")
    print("  /msg <nick> <msg> Send private message")
    print("  /quit             Disconnect and exit")
    print("  /help             Show this help")
    print("")
    print("Example:")
    print("  irc connect irc.libera.chat 6667 MyBot #lua")
end

if arg[1] == "connect" then
    local server = arg[2]
    local port = arg[3] or "6667"
    local nick = arg[4] or "OpenTTY_User"
    local channel = arg[5] or ""

    if not server then help() os.exit(2) end

    os.setproc("name", "irc")

    local ok, err = pcall(function()
        conn, input, output = socket.connect("socket://" .. server .. ":" .. port)
    end)
    if not ok then
        print("irc: cannot connect to " .. server .. ":" .. port)
        os.exit(101)
    end

    connected = true
    current_nick = nick
    current_channel = channel

    send_irc("NICK", nick)
    send_irc("USER", nick, "0", "*", "OpenTTY IRC Client")

    if channel ~= "" and string.sub(channel, 1, 1) ~= "#" then
        channel = "#" .. channel
        current_channel = channel
    end

    if channel ~= "" then
        send_irc("JOIN", channel)
        print(":: Joining " .. channel)
    end

    java.run(function()
        while connected do
            local ok2, data = pcall(io.read, input, 2048)
            if ok2 and data and data ~= "" then
                local lines = string.split(data, "\r\n")
                for _, line in pairs(lines) do
                    if line ~= "" then
                        local msg = parse_irc(line)
                        local text = process_message(msg)
                        if text then
                            print(text)
                        end
                    end
                end
            else
                connected = false
                print("[DISCONNECTED]")
            end
        end
    end)

    print(":: Connected to " .. server .. " as " .. nick)
    os.execute("sh")

elseif arg[1] == "send" then
    local server = arg[2]
    local port = arg[3] or "6667"
    local nick = arg[4] or "OpenTTY_User"
    local channel = arg[5] or "#general"
    local message = arg[6] or ""

    if not server or message == "" then
        help()
        os.exit(2)
    end

    os.setproc("name", "irc-send")

    local ok = pcall(function()
        conn, input, output = socket.connect("socket://" .. server .. ":" .. port)
    end)
    if not ok then
        print("irc: cannot connect to " .. server .. ":" .. port)
        os.exit(101)
    end

    connected = true
    current_nick = nick

    send_irc("NICK", nick)
    send_irc("USER", nick, "0", "*", "OpenTTY IRC Client")

    java.sleep(500)
    send_irc("JOIN", channel)
    java.sleep(200)

    send_irc("PRIVMSG", channel, ":" .. message)
    java.sleep(200)

    send_irc("QUIT", ":OpenTTY IRC")
    java.sleep(100)

    pcall(io.close, conn)
    print(":: Message sent to " .. channel)

elseif arg[1] == "gui" or arg[1] == nil then
    os.setproc("name", "irc-gui")

    local previous = graphics.getCurrent()
    local main_screen = graphics.new("list", "IRC Client v" .. version)
    local connect_btn = graphics.new("command", { label = "Connect", type = "ok", priority = 1 })
    local back_btn = graphics.new("command", { label = "Back", type = "back", priority = 1 })

    graphics.append(main_screen, "Connect to Server")
    graphics.append(main_screen, "Quick Connect (libera)")
    graphics.append(main_screen, "Channels Joined")

    graphics.addCommand(main_screen, connect_btn)
    graphics.addCommand(main_screen, back_btn)

    local function show_connect_form()
        local screen = graphics.new("screen", "Connect to IRC")
        local back = graphics.new("command", { label = "Back", type = "back", priority = 1 })
        local do_connect = graphics.new("command", { label = "Connect", type = "ok", priority = 1 })

        graphics.append(screen, { type = "field", label = "Server", value = "irc.libera.chat" })
        graphics.append(screen, { type = "field", label = "Port", value = "6667" })
        graphics.append(screen, { type = "field", label = "Nick", value = "OpenTTY_User" })
        graphics.append(screen, { type = "field", label = "Channel", value = "#lua" })

        graphics.addCommand(screen, back)
        graphics.addCommand(screen, do_connect)

        graphics.handler(screen, {
            [back] = function() graphics.display(main_screen) end,
            [do_connect] = function(server, port, nick, channel)
                if not server or server == "" then
                    graphics.display(graphics.new("alert", "Error", "Server is required"))
                    return
                end
                port = port or "6667"
                nick = nick or "OpenTTY_User"
                channel = channel or "#lua"

                start_irc_session(server, port, nick, channel)
            end
        })
        graphics.display(screen)
    end

    function start_irc_session(server, port, nick, channel)
        local ok = pcall(function()
            conn, input, output = socket.connect("socket://" .. server .. ":" .. port)
        end)
        if not ok then
            graphics.display(graphics.new("alert", "Error", "Cannot connect to " .. server))
            return
        end

        connected = true
        current_nick = nick
        current_channel = channel

        send_irc("NICK", nick)
        send_irc("USER", nick, "0", "*", "OpenTTY IRC Client")

        local chat_screen = graphics.new("screen", "IRC: " .. channel)
        local back = graphics.new("command", { label = "Back", type = "back", priority = 1 })
        local send_btn = graphics.new("command", { label = "Send", type = "ok", priority = 1 })
        local chat_buf = graphics.new("buffer", {})
        local input_field = graphics.new("field", { label = "Message", value = "", length = 256 })

        graphics.append(chat_screen, chat_buf)
        graphics.append(chat_screen, input_field)
        graphics.addCommand(chat_screen, send_btn)
        graphics.addCommand(chat_screen, back)

        java.run(function()
            while connected do
                local ok2, data = pcall(io.read, input, 2048)
                if ok2 and data and data ~= "" then
                    local lines = string.split(data, "\r\n")
                    for _, line in pairs(lines) do
                        if line ~= "" then
                            local msg = parse_irc(line)
                            local text = process_message(msg)
                            if text then
                                io.write(text .. "\n", chat_buf, "a")
                            end
                        end
                    end
                else
                    connected = false
                    io.write("[DISCONNECTED]\n", chat_buf, "a")
                end
            end
        end)

        graphics.handler(chat_screen, {
            [back] = function()
                connected = false
                send_irc("QUIT", ":OpenTTY IRC")
                java.sleep(200)
                pcall(io.close, conn)
                graphics.display(main_screen)
            end,
            [send_btn] = function(message)
                if message and message ~= "" then
                    if string.startswith(message, "/") then
                        local parts = string.split(message, " ")
                        local cmd = parts[1] or ""

                        if cmd == "/quit" then
                            connected = false
                            send_irc("QUIT", ":Goodbye")
                            java.sleep(200)
                            pcall(io.close, conn)
                            graphics.display(main_screen)
                        elseif cmd == "/join" and parts[2] then
                            current_channel = parts[2]
                            if string.sub(current_channel, 1, 1) ~= "#" then
                                current_channel = "#" .. current_channel
                            end
                            send_irc("JOIN", current_channel)
                        elseif cmd == "/part" then
                            send_irc("PART", current_channel)
                        elseif cmd == "/nick" and parts[2] then
                            send_irc("NICK", parts[2])
                            current_nick = parts[2]
                        elseif cmd == "/msg" and parts[2] and parts[3] then
                            local msgtext = ""
                            for i = 3, #parts do
                                if i > 3 then msgtext = msgtext .. " " end
                                msgtext = msgtext .. parts[i]
                            end
                            send_irc("PRIVMSG", parts[2], ":" .. msgtext)
                        else
                            io.write("[USAGE] /join #ch | /part | /nick name | /msg nick text | /quit\n", chat_buf, "a")
                        end
                    else
                        send_irc("PRIVMSG", current_channel, ":" .. message)
                        io.write("[" .. format_time() .. "] <" .. current_nick .. "> " .. message .. "\n", chat_buf, "a")
                    end
                    graphics.SetText(input_field, "")
                end
            end
        })
        graphics.display(chat_screen)
    end

    graphics.handler(main_screen, {
        [back] = function()
            graphics.display(previous)
            os.exit(0)
        end,
        [connect_btn] = function(opt)
            if opt == "Connect to Server" then
                show_connect_form()
            elseif opt == "Quick Connect (libera)" then
                start_irc_session("irc.libera.chat", "6667", "OpenTTY_User", "#lua")
            end
        end,
        [graphics.fire] = function(opt)
            if opt == "Connect to Server" then
                show_connect_form()
            elseif opt == "Quick Connect (libera)" then
                start_irc_session("irc.libera.chat", "6667", "OpenTTY_User", "#lua")
            end
        end
    })
    graphics.display(main_screen)

elseif arg[1] == "help" or arg[1] == "--help" then
    help()
else
    help()
end
