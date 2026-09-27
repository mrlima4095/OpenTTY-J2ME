#!/bin/lua

-- OpenTTY sshd - OpenSSH server for J2ME (server only)
-- Installed at /bin/sshd. Runs in background and creates its own
-- configuration file /etc/sshd (a Lua script that returns a table).

local version = "1.0.0"

local DEFAULT_CONFIG = [[-- OpenTTY sshd configuration
-- This file is a Lua script: edit it and run "sshd reload".
return {
    port = 22,
    user = "root",
    password = "",
    banner = "OpenTTY-sshd/1.0",
    motd = "/etc/motd",
    log = "/tmp/sshd.log",
    maxlogintries = 3,
}
]]

local function help()
    print("OpenTTY sshd v" .. version .. " - OpenSSH server for OpenTTY")
    print("")
    print("Usage: sshd [command]")
    print("")
    print("Commands:")
    print("  (no args)      Start the SSH server in background")
    print("  status         Show server status")
    print("  stop           Stop the SSH server")
    print("  reload         Reload the /etc/sshd configuration")
    print("  config         Print the current /etc/sshd configuration")
    print("  help           Show this help")
    print("")
    print("Configuration:")
    print("  /etc/sshd      Lua file returning { port, user, password, ... }")
    print("")
    print("Example:")
    print("  sshd")
    print("  sshd status")
end

local function load_config()
    local content = io.read("/etc/sshd")
    if content == nil or content == "" then return nil end
    local ok, chunk = pcall(load, content)
    if not ok or type(chunk) ~= "function" then return nil end
    local ok2, cfg = pcall(chunk)
    if ok2 and type(cfg) == "table" then return cfg end
    return nil
end

if arg[1] == "--deamon" then
    os.setproc("name", "sshd")
    os.setproc(false)

    local running = true
    local config = {}

    local function ensure_config()
        local content = io.read("/etc/sshd")
        if content == nil or content == "" then
            pcall(io.write, DEFAULT_CONFIG, "/etc/sshd")
        end
        local cfg = load_config()
        if cfg then config = cfg end
    end

    local function log(msg)
        pcall(io.write, "[" .. os.date("%H:%M:%S") .. "] " .. msg .. "\n", config.log or "/tmp/sshd.log", "a")
    end

    local leftover = ""
    local function read_line(input)
        local buf = leftover
        leftover = ""
        while true do
            local nl = string.find(buf, "\n")
            if nl then
                local line = string.sub(buf, 1, nl - 1)
                leftover = string.sub(buf, nl + 1)
                if string.sub(line, -1) == "\r" then line = string.sub(line, 1, -2) end
                return line
            end
            local ok, chunk = pcall(io.read, input, 8)
            if not ok or not chunk or chunk == "" then
                if buf == "" then return nil end
                return buf
            end
            buf = buf .. chunk
            if #buf > 4096 then return nil end
        end
    end

    local function handle_client(conn, input, output)
        local saved_stdout = io.stdout
        io.setstdout(output)

        local ip = "unknown"
        local ok, peer = pcall(socket.peer, conn)
        if ok and peer then ip = tostring(peer) end

        pcall(io.write, "SSH-2.0-" .. (config.banner or "OpenTTY-sshd") .. "\r\n", output)

        local okv, ver = pcall(read_line, input)
        if not okv or not ver or not string.startswith(string.trim(ver), "SSH-") then
            pcall(io.close, conn)
            io.setstdout(saved_stdout)
            return
        end

        local maxatt = tonumber(config.maxlogintries) or 3
        local login = ""
        local attempts = 0

        while attempts < maxatt do
            attempts = attempts + 1
            pcall(io.write, "login: ", output)
            local ok1, u = pcall(read_line, input)
            if not ok1 or not u then break end
            pcall(io.write, "password: ", output)
            local ok2, p = pcall(read_line, input)
            if not ok2 or not p then break end
            login = u
            if login == tostring(config.user or "root") and p == (config.password or "") then
                break
            end
        end

        if login ~= tostring(config.user or "root") then
            pcall(io.write, "sshd: authorization failed\r\n", output)
            log(ip .. " auth failed (" .. tostring(login) .. ")")
            pcall(io.close, conn)
            io.setstdout(saved_stdout)
            return
        end

        log(ip .. " login ok (" .. tostring(login) .. ")")

        local motd = io.read(config.motd or "/etc/motd")
        if motd and motd ~= "" and motd ~= nil then
            pcall(io.write, motd, output)
            pcall(io.write, "\r\n", output)
        end

        local prompt = (login == "root") and "# " or (tostring(login) .. "$ ")

        while running do
            pcall(io.write, prompt, output)
            local okc, cmd = pcall(read_line, input)
            if not okc or not cmd then break end
            cmd = string.trim(cmd)
            if cmd == "" then
            elseif cmd == "exit" or cmd == "logout" or cmd == "quit" or cmd == "bye" then
                break
            elseif cmd == "help" then
                pcall(io.write, "OpenTTY sshd: type any /bin command to run it.\r\n", output)
            else
                pcall(os.execute, cmd)
            end
        end

        pcall(io.write, "logout\r\n", output)
        log(ip .. " disconnected")
        pcall(io.close, conn)
        io.setstdout(saved_stdout)
    end

    ensure_config()

    local port = tonumber(config.port or 22)
    local server_socket = nil
    local okbind, errbind = pcall(function() server_socket = socket.server(port) end)
    if not okbind or not server_socket then
        log("cannot bind to port " .. tostring(port) .. ": " .. tostring(errbind))
        return function(payload, args, scope, pid, uid)
            return ":: sshd failed to start: port " .. tostring(port) .. " in use"
        end
    end

    log("sshd started on port " .. tostring(port))

    java.run(function()
        while running do
            local aok, conn, input, output = pcall(socket.accept, server_socket)
            if aok and conn then
                handle_client(conn, input, output)
            end
        end
    end, "sshd-accept")

    return function(payload, args, scope, pid, uid)
        if payload == "stop" then
            running = false
            pcall(io.close, server_socket)
            log("sshd stopped")
            return ":: sshd stopped"
        elseif payload == "reload" then
            local old = tonumber(config.port or 22)
            ensure_config()
            local new = tonumber(config.port or 22)
            if new ~= old then
                pcall(io.close, server_socket)
                local okr, errr = pcall(function() server_socket = socket.server(new) end)
                if not okr or not server_socket then
                    return ":: reload failed: port " .. tostring(new) .. " in use"
                end
            end
            log("configuration reloaded")
            return ":: sshd reloaded"
        elseif payload == "status" then
            return ":: sshd running on port " .. tostring(config.port or 22) .. " as user " .. tostring(config.user or "root")
        elseif payload == "config" then
            return config
        else
            return ":: unknown command"
        end
    end
end

local function server_pid()
    return os.getpid("sshd")
end

if arg[1] == nil then
    local pid = server_pid()
    if pid then
        print("sshd: already running (pid " .. pid .. ")")
        os.exit(1)
    end

    print("sshd: starting server...")
    os.request("1", "serve", os.join(arg[0]))
    java.sleep(300)
    pid = server_pid()
    if pid then
        print("sshd: started (pid " .. pid .. ")")
    else
        print("sshd: failed to start")
        os.exit(1)
    end

elseif arg[1] == "status" then
    local pid = server_pid()
    if pid then
        local result = os.request(pid, "status")
        if result then print(result) end
    else
        print("sshd: not running")
        os.exit(1)
    end

elseif arg[1] == "stop" then
    local pid = server_pid()
    if not pid then
        print("sshd: not running")
        os.exit(1)
    end
    local result = os.request(pid, "stop")
    if result then print(result) end

elseif arg[1] == "reload" then
    local pid = server_pid()
    if not pid then
        print("sshd: not running")
        os.exit(1)
    end
    local result = os.request(pid, "reload")
    if result then print(result) end

elseif arg[1] == "config" then
    local content = io.read("/etc/sshd")
    if content == nil or content == "" then
        print("sshd: /etc/sshd not found")
        os.exit(1)
    end
    print(content)

elseif arg[1] == "help" or arg[1] == "--help" then
    help()

else
    print("sshd: '" .. tostring(arg[1]) .. "' is not a valid command")
    print("Try 'sshd help'")
    os.exit(1)
end