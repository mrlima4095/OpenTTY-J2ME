#!/bin/lua

-- OpenTTY sshd - plaintext login shell over a raw TCP socket.
--
-- This is NOT OpenSSH: there is no SSH protocol, no key exchange and no
-- encryption. A client connects, gets a banner, types the account password
-- and then talks to a small line-oriented shell that runs /bin commands.
-- Everything (the password included) crosses the wire in clear text, so only
-- use it on a trusted LAN, a USB tether or a lab network.
--
-- The daemon follows the runtime model documented in
-- apps/1.19/net/nginx/main.lua: a Lua function owns the interpreter until it
-- returns, and a function blocked in socket.accept() keeps that ownership.
-- The CLI therefore starts the daemon as a background process whose chunk
-- runs the accept loop directly (no java.run thread), and the stop/reload
-- commands use a flag file in /tmp plus a wake-up connection instead of an
-- os.request round trip (which would deadlock while the daemon is idle in
-- accept). For the same reason one client is served at a time: a second
-- connection waits in the TCP backlog until the first one logs out.
--
-- Installed at /bin/sshd. The first run creates /etc/sshd, a Lua file that
-- returns the configuration table.
--
-- Real-device notes: port 2222 is used by default because phones usually
-- refuse privileged ports. The JAD must request
-- javax.microedition.io.Connector.socket (and serversocket) or the device
-- denies the connection. Keep the MIDlet in the background while serving;
-- some stacks close the listening socket when the app is paused, and the
-- accept loop rebinds when it notices.

local version = "1.1.0"

local PID_FILE = "/tmp/sshd.pid"
local STOP_FLAG = "/tmp/sshd.stop"
local RELOAD_FLAG = "/tmp/sshd.reload"
local FAIL_FILE = "/tmp/sshd.fail"

-- A control connection is just a TCP client whose first line is this token.
local CONTROL = "sshd-control-7f31"

local DEFAULT_CONFIG = [[-- OpenTTY sshd configuration (plaintext, no encryption)
-- This file is a Lua script: edit it and run "sshd reload".
return {
    -- TCP port to listen on. Ports below 1024 usually need a privileged
    -- MIDlet (or root on the desktop shim), hence 2222 by default.
    port = 2222,

    -- Account name shown in the prompt/banner. The password is checked
    -- against `password` below, or against the OpenTTY account password
    -- when `password` is empty.
    user = "root",
    password = "",

    banner = "OpenTTY-sshd 1.1.0 (plaintext)",
    motd = "/etc/motd",
    log = "/tmp/sshd.log",
    maxlogintries = 3,
}
]]

local function help()
    print("OpenTTY sshd v" .. version .. " - plaintext login shell for OpenTTY")
    print("")
    print("Usage: sshd [command]")
    print("")
    print("Commands:")
    print("  start          Start the server in the background (default)")
    print("  stop           Stop the server")
    print("  status         Show server status")
    print("  reload         Reload the /etc/sshd configuration")
    print("  config         Print the current /etc/sshd configuration")
    print("  password       Generate a password line for /etc/sshd")
    print("  help           Show this help")
    print("")
    print("Configuration:")
    print("  /etc/sshd      Lua file returning { port, user, password, ... }")
    print("")
    print("Connecting (no encryption - the password is sent in clear text):")
    print("  nc <phone-ip> 2222")
    print("  telnet <phone-ip> 2222")
    print("")
    print("Example:")
    print("  sshd start")
    print("  sshd status")
end

-- Configuration ---------------------------------------------------------

local function load_config()
    local content = io.read("/etc/sshd")
    if content == nil or content == "" then
        return nil
    end
    -- load() compiles *and runs* the chunk here, so its result is the table
    -- returned by the configuration file (not a function to call).
    local ok, cfg = pcall(load, content)
    if ok and type(cfg) == "table" then return cfg end
    return nil
end

local function ensure_config()
    local content = io.read("/etc/sshd")
    if content == nil or content == "" then
        -- 0 means the write succeeded; a non-root start gets 13 here and keeps
        -- the built-in defaults instead of the file.
        local ok, status = pcall(io.write, DEFAULT_CONFIG, "/etc/sshd")
        if not ok or status ~= 0 then return {}, false end
    end
    return load_config() or {}, true
end

local function port_of(cfg)
    return tonumber(cfg.port) or 2222
end

-- Small helpers ---------------------------------------------------------

local function flag_set(path)
    local content = io.read(path)
    return content ~= nil and content ~= ""
end

local function read_pidfile()
    local content = io.read(PID_FILE)
    if content == nil or content == "" then return nil, nil end
    local parts = string.split(string.trim(content), " ")
    local pid = parts[1]
    if pid == nil or pid == "" then return nil, nil end
    return pid, tonumber(parts[2])
end

local function port_suffix(port)
    if port == nil then return "" end
    return ", port " .. tostring(port)
end

-- Wake the accept loop up so it can read a control flag. Loopback is the
-- fastest route, but a few phone stacks refuse socket://127.0.0.1, so the
-- device's own name/IP (HOSTNAME, e.g. from /etc/hostname) is the fallback.
local function poke(port)
    if port == nil then return false end

    local hosts = { "127.0.0.1" }
    local name = os.getenv("HOSTNAME")
    if name ~= nil and name ~= "" and name ~= "localhost" and name ~= "127.0.0.1" then
        hosts[#hosts + 1] = name
    end

    for i = 1, #hosts do
        local ok, conn, input, output = pcall(socket.connect, "socket://" .. hosts[i] .. ":" .. tostring(port))
        if ok and conn ~= nil then
            pcall(io.write, CONTROL .. "\n", output)
            pcall(io.close, output)
            pcall(io.close, input)
            pcall(io.close, conn)
            return true
        end
    end

    return false
end

-- Read one CRLF/LF terminated line from a client. Each session keeps its own
-- buffer, so two connections never steal each other's leftover bytes.
local function read_line(state, limit)
    limit = limit or 1024
    local buf = state.buf or ""

    while true do
        local nl = string.find(buf, "\n")
        if nl then
            local line = string.sub(buf, 1, nl - 1)
            state.buf = string.sub(buf, nl + 1)
            if string.endswith(line, "\r") then
                line = string.sub(line, 1, #line - 1)
            end
            return line
        end
        if #buf > limit then
            state.buf = ""
            return nil
        end
        local ok, chunk = pcall(io.read, state.input, 64)
        if not ok or chunk == nil or chunk == "" then
            state.buf = ""
            return nil
        end
        buf = buf .. chunk
    end
end

-- Password check: `password` in /etc/sshd wins, otherwise the OpenTTY
-- account password (the same one used by the login form / sudo).
local function password_ok(cfg, query)
    local expected = cfg.password
    if expected ~= nil and tostring(expected) ~= "" then
        return query == tostring(expected)
    end
    local ok, valid = pcall(os.request, "1", "passwd", query)
    return ok and valid == true
end

-- Daemon ----------------------------------------------------------------

if arg[1] == "--deamon" then
    os.setproc("name", "sshd")

    local running = true
    local config = {}
    local server_socket = nil
    local port = 2222

    local function log(msg)
        local line = "[" .. os.date("%H:%M:%S") .. "] " .. msg .. "\n"
        pcall(io.write, line, config.log or "/tmp/sshd.log", "a")
    end

    local function write_pidfile()
        pcall(io.write, os.getpid() .. " " .. tostring(port), PID_FILE)
    end

    -- Open the new listener first and only then close the previous one, so a
    -- failed reload keeps the daemon reachable on its current port.
    local function bind_listener(target)
        local ok, server = pcall(socket.server, target)
        if not ok or server == nil then return false end
        if server_socket ~= nil then pcall(io.close, server_socket) end
        server_socket = server
        port = target
        return true
    end

    local function handle_control()
        if flag_set(STOP_FLAG) then
            pcall(io.write, "", STOP_FLAG)
            running = false
            log("stop requested")
        elseif flag_set(RELOAD_FLAG) then
            pcall(io.write, "", RELOAD_FLAG)
            local previous = port
            config = ensure_config()
            local target = port_of(config)
            if target == previous then
                log("configuration reloaded")
            elseif bind_listener(target) then
                write_pidfile()
                log("configuration reloaded (now listening on port " .. tostring(target) .. ")")
            else
                log("reload: cannot bind port " .. tostring(target) .. ", keeping " .. tostring(previous))
            end
        end
    end

    local function close_session(state)
        pcall(io.close, state.output)
        pcall(io.close, state.input)
        pcall(io.close, state.conn)
    end

    local function run_shell(state, user)
        local saved_stdout = io.stdout
        io.setstdout(state.output)

        local function write(text)
            pcall(io.write, text, state.output)
        end

        -- Always restore the daemon's stdout, even if a command blows up:
        -- otherwise later tracebacks would be written to a closed socket.
        local shell_ok, shell_err = pcall(function()
            local motd = io.read(config.motd or "/etc/motd")
            if motd ~= nil and motd ~= "" then
                write("\r\n" .. string.env(motd) .. "\r\n")
            else
                write("\r\n")
            end

            local prompt = tostring(user) .. "@opentty" .. (os.getuid() == 0 and "#" or "$")

            while running do
                -- Stop/reload flags are written before the CLI's wake-up
                -- connection, so an active session notices them too.
                if flag_set(STOP_FLAG) or flag_set(RELOAD_FLAG) then
                    handle_control()
                end

                write(prompt .. " ")
                local line = read_line(state, 2048)
                if line == nil then break end
                line = string.trim(line)

                if line == CONTROL then
                    -- Stop/reload also work while a session is open: the CLI's
                    -- wake-up connection is a normal client up to this line.
                    handle_control()
                    if not running then break end
                elseif line == "" then
                    -- keep prompting
                elseif line == "exit" or line == "logout" or line == "quit" or line == "bye" or line == "exit 0" then
                    break
                elseif line == "help" or line == "?" then
                    write("OpenTTY sshd: every other line is run as a shell command.\r\n")
                    write("  help              this help\r\n")
                    write("  exit | logout     close the session\r\n")
                    write("  cd, pwd, ls, cat, echo, ps, ...   /bin commands\r\n")
                    write("Interactive/UI programs (nano, passwd, xterm) cannot read\r\n")
                    write("from the socket; use commands that take arguments instead.\r\n")
                else
                    local ok, status = pcall(os.execute, line)
                    if not ok then
                        write("sshd: " .. tostring(status) .. "\r\n")
                    end
                end

                -- Give queued UI callbacks and background jobs a turn between
                -- commands (this is a long-running Lua function).
                pcall(java.yield, 5)
            end

            write("logout\r\n")
        end)

        io.setstdout(saved_stdout)
        if not shell_ok then error(shell_err) end
    end

    local function handle_client(conn, input, output)
        local state = { conn = conn, input = input, output = output, buf = "" }

        local function write(text)
            pcall(io.write, text, output)
        end

        local peer = "unknown"
        local okp, address, peer_port = pcall(socket.peer, conn)
        if okp and address then
            peer = tostring(address)
            if peer_port then peer = peer .. ":" .. tostring(peer_port) end
        end

        write(string.env(config.banner or ("OpenTTY-sshd " .. version)) .. "\r\n")

        local maxattempts = tonumber(config.maxlogintries) or 3
        if maxattempts < 1 then maxattempts = 1 end

        local authenticated = false
        local attempts = 0

        while attempts < maxattempts do
            attempts = attempts + 1
            write("Password: ")
            local line = read_line(state, 256)
            if line == nil then
                close_session(state)
                return
            end
            line = string.trim(line)

            if line == CONTROL then
                handle_control()
                close_session(state)
                return
            end

            if password_ok(config, line) then
                authenticated = true
                break
            end

            write("\r\nPermission denied, please try again.\r\n")
            log(peer .. " authentication failed (attempt " .. attempts .. "/" .. maxattempts .. ")")
        end

        if not authenticated then
            write("Permission denied (too many failed attempts).\r\n")
            log(peer .. " disconnected after failed login")
            close_session(state)
            return
        end

        local user = tostring(config.user ~= nil and config.user ~= "" and config.user or "root")
        log(peer .. " login ok (" .. user .. ")")
        write("Logged in as " .. user .. " - plaintext session, no encryption.\r\n")

        local ok, err = pcall(run_shell, state, user)
        if not ok then
            log(peer .. " session error: " .. tostring(err))
        end
        log(peer .. " disconnected")
        close_session(state)
    end

    config = ensure_config()
    port = port_of(config)
    pcall(io.write, "", FAIL_FILE)

    if not bind_listener(port) then
        local message = "cannot bind to port " .. tostring(port) ..
            " (ports below 1024 usually need a privileged MIDlet; set port = 2222 in /etc/sshd)"
        log(message)
        pcall(io.write, message, FAIL_FILE)
        pcall(os.request, "1", "sendsig", { pid = os.getpid(), signal = "9" })
        return nil
    end

    write_pidfile()
    pcall(io.write, "", STOP_FLAG)
    pcall(io.write, "", RELOAD_FLAG)
    log("sshd started on port " .. tostring(port))

    -- The accept loop runs directly in this chunk (the CLI starts it as a
    -- background process). Do NOT move it to a java.run thread: a chunk and a
    -- thread share the interpreter's parser state, and the session shell is a
    -- long-running function that keeps running commands, so one thread keeps
    -- everything consistent. Stop/reload arrive as a control connection and a
    -- flag file because os.request would block while accept owns the process.
    local loop_ok, loop_err = pcall(function()
        while running do
            local aok, conn, input, output = pcall(socket.accept, server_socket)

            if aok and conn then
                if flag_set(STOP_FLAG) or flag_set(RELOAD_FLAG) then
                    handle_control()
                    pcall(io.close, conn)
                    pcall(io.close, input)
                    pcall(io.close, output)
                else
                    -- One bad session must never take the daemon down: a J2ME
                    -- read on a half-closed socket throws.
                    local ok, err = pcall(handle_client, conn, input, output)
                    if not ok then
                        log("session failed: " .. tostring(err))
                        pcall(io.close, conn)
                        pcall(io.close, input)
                        pcall(io.close, output)
                    end
                end
            elseif aok then
                java.sleep(10)
            else
                -- Some MIDP stacks invalidate the listening socket as soon as
                -- an accepted connection is closed. Rebind and keep serving.
                log("accept failed: " .. tostring(conn))
                pcall(io.close, server_socket)
                server_socket = nil
                if not bind_listener(port) then
                    java.sleep(200)
                end
            end
        end
    end)

    if not loop_ok then
        log("accept loop failed: " .. tostring(loop_err))
    end

    pcall(io.close, server_socket)
    pcall(io.write, "", PID_FILE)
    log("sshd stopped")
    -- Drop the process from the process table so `ps` stops listing sshd.
    pcall(os.request, "1", "sendsig", { pid = os.getpid(), signal = "9" })
    return nil
end

-- Command line ----------------------------------------------------------

-- The CLI itself must not be named "sshd": `sshd start`/`status`/`stop` look
-- the daemon up with os.getpid("sshd") and would otherwise find themselves.
os.setproc("name", "sshd-cli")

local command = arg[1] or "start"

if command == "start" then
    local pid = os.getpid("sshd")
    if pid then
        local _, port = read_pidfile()
        print("sshd: already running (pid " .. pid .. port_suffix(port) .. ")")
        os.exit(68)
    end

    pcall(io.write, "", FAIL_FILE)
    pcall(io.write, "", STOP_FLAG)
    pcall(io.write, "", RELOAD_FLAG)

    -- Launch the daemon as a plain background process (the accept loop runs
    -- inside that chunk). `serve` is not used on purpose: its chunk would
    -- return while a java.run accept thread is still sharing the interpreter.
    local cfg, seeded = ensure_config()
    if not seeded then
        print("sshd: warning: cannot write /etc/sshd (run as root); using defaults")
    end
    local configured = port_of(cfg or {})
    print("sshd: starting server...")
    -- "." runs this same file through popen instead of looking it up in /bin,
    -- so `sshd start` also works from a copy outside /bin. The command is not
    -- a bare path: exec() resolves a leading /bin through getCommand().
    os.execute(". " .. os.join(arg[0]) .. " --deamon &")
    java.sleep(500)

    pid = os.getpid("sshd")
    if pid then
        print("sshd: started (pid " .. pid .. port_suffix(configured) .. ")")
        print("sshd: plaintext only - connect with 'nc <ip> " .. tostring(configured) .. "'")
    else
        local message = io.read(FAIL_FILE)
        if message ~= nil and message ~= "" then
            print("sshd: " .. message)
        else
            print("sshd: failed to start")
        end
        os.exit(1)
    end
    os.exit(0)

elseif command == "stop" then
    local pid, port = read_pidfile()
    if pid == nil or os.getpid("sshd") == nil then
        print("sshd: not running")
        os.exit(1)
    end

    pcall(io.write, "1", STOP_FLAG)
    if not poke(port) then
        print("sshd: running, but the control connection to port " .. tostring(port) .. " failed")
        os.exit(1)
    end

    local waited = 0
    while os.getpid("sshd") and waited < 3000 do
        java.sleep(100)
        waited = waited + 100
    end

    if os.getpid("sshd") then
        print("sshd: stop requested (pid " .. pid .. " is still busy with a session)")
        os.exit(1)
    else
        print("sshd: stopped")
    end
    os.exit(0)

elseif command == "reload" then
    local pid, port = read_pidfile()
    if pid == nil or os.getpid("sshd") == nil then
        print("sshd: not running")
        os.exit(1)
    end

    pcall(io.write, "1", RELOAD_FLAG)
    if not poke(port) then
        print("sshd: running, but the control connection to port " .. tostring(port) .. " failed")
        os.exit(1)
    end

    local waited = 0
    while flag_set(RELOAD_FLAG) and waited < 3000 do
        java.sleep(100)
        waited = waited + 100
    end

    if flag_set(RELOAD_FLAG) then
        print("sshd: reload requested (pid " .. pid .. " is still busy with a session)")
        os.exit(1)
    else
        print("sshd: configuration reloaded")
    end
    os.exit(0)

elseif command == "status" then
    local pid, port = read_pidfile()
    if pid ~= nil and os.getpid("sshd") ~= nil then
        print("sshd: running (pid " .. pid .. port_suffix(port) .. ")")
    else
        print("sshd: not running")
        os.exit(1)
    end
    os.exit(0)

elseif command == "config" then
    local content = io.read("/etc/sshd")
    if content == nil or content == "" then
        print("sshd: /etc/sshd not found (run 'sshd start' to create it)")
        os.exit(1)
    end
    print(content)
    os.exit(0)

elseif command == "password" then
    local generated = arg[2]
    if generated == nil or generated == "" then
        generated = string.uuid()
    end
    print("sshd: add this line to /etc/sshd, then run 'sshd reload':")
    print("    password = \"" .. tostring(generated) .. "\",")
    os.exit(0)

elseif command == "help" or command == "--help" then
    help()
    os.exit(0)

else
    print("sshd: '" .. tostring(command) .. "' is not a valid command")
    print("Try 'sshd help'")
    os.exit(1)
end
