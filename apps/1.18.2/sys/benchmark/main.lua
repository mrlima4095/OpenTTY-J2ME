#!/bin/lua

local version = "1.0.0"
local PORT = 5555
local DEFAULT_HOST = "localhost"

local args = {}
for i = 1, #arg do args[i] = arg[i] end

local device = nil

local function join(array, from, sep)
    local s = ""
    for i = (from or 1), #array do
        if i > (from or 1) then s = s .. (sep or " ") end
        s = s .. tostring(array[i])
    end
    return s
end

local function split_args(cmd)
    local parts = {}
    local cur = ""
    local inq = false
    for i = 1, #cmd do
        local c = string.sub(cmd, i, i)
        if c == "\"" then
            inq = not inq
        elseif c == " " and not inq then
            if #cur > 0 then parts[#parts + 1] = cur cur = "" end
        else
            cur = cur .. c
        end
    end
    if #cur > 0 then parts[#parts + 1] = cur end
    return parts
end

local function count_keys(t)
    local n = 0
    if t then for k, v in pairs(t) do n = n + 1 end end
    return n
end

local function pid_of(name)
    if not name or #name == 0 then return nil end
    local procs = os.getproc()
    if not procs then return nil end
    for k, v in pairs(procs) do
        if k == name or v == name then return k end
    end
    return nil
end

local function capture(fn)
    local buf = io.new()
    local prev = io.stdout
    io.setstdout(buf)
    local ok, res = pcall(fn)
    local out = io.read()
    io.setstdout(prev)
    if not ok then return tostring(out) .. "\n" .. tostring(res), 1 end
    return tostring(out), tonumber(res) or 0
end

local function read_file_all(inp)
    local s = ""
    while true do
        local chunk = io.read(inp, 1024)
        if chunk == nil or chunk == "" then break end
        s = s .. chunk
    end
    return s
end

local function cmd_ps()
    local procs = os.getproc()
    if not procs or count_keys(procs) == 0 then
        print("jdb: no running processes")
        return 0
    end
    print("PID\tPROCESS")
    for k, v in pairs(procs) do
        print(k .. "\t" .. tostring(v))
    end
    return 0
end

local function cmd_getproc(pid, field)
    if not pid or not field then
        print("jdb: getproc <pid> <field>")
        return 2
    end
    local p = pid
    local procs = os.getproc()
    if procs and not procs[p] then p = pid_of(pid) end
    if not p then
        print("jdb: no such process: " .. pid)
        return 127
    end
    local ok, res = pcall(os.getproc, p, field)
    if not ok then
        print("jdb: getproc: " .. tostring(res))
        return 1
    end
    if res == nil or tostring(res) == "nil" then
        print("jdb: field '" .. field .. "' is empty")
        return 0
    end
    print(tostring(res))
    return 0
end

local function cmd_dumpsys(serv)
    local procs = os.getproc()
    if serv then
        local p = serv
        if procs and not procs[p] then p = pid_of(serv) end
        if not p then
            print("jdb: no such process: " .. serv)
            return 127
        end
        print("[" .. serv .. "]")
        print("  pid: " .. p)
        if procs and procs[p] then print("  process: " .. tostring(procs[p])) end
        return 0
    end
    if not procs or count_keys(procs) == 0 then
        print("jdb: no running processes")
        return 0
    end
    for k, v in pairs(procs) do
        local extra = tostring(v)
        if extra ~= "" and extra ~= "nil" then extra = " (" .. extra .. ")" else extra = "" end
        print(k .. extra)
    end
    return 0
end

local function cmd_logcat(num)
    if os.getuid() ~= 0 then
        print("jdb: logcat: permission denied")
        return 13
    end
    local file = io.open("/tmp/logs")
    if file then
        local content = read_file_all(file)
        if content and #content > 0 then
            if tonumber(num) then
                local lines = string.split(content, "\n")
                local last = #lines
                if lines[last] == "" then last = last - 1 end
                local from = last - tonumber(num) + 1
                if from < 1 then from = 1 end
                for i = from, last do print(lines[i]) end
            else
                print(content)
            end
        else
            print("jdb: log buffer is empty")
        end
    else
        print("jdb: no logs on session (manage with 'log add/view/read')")
    end
    return 0
end

local function cmd_users()
    if os.getuid() ~= 0 then
        print("jdb: users: permission denied")
        return 13
    end
    print("jdb users")
    print("  root (0)")
    local u = java and java.midlet and java.midlet.username
    if u and #u > 0 then print("  " .. u .. " (1000)") end
    return 0
end

local function cmd_install(pkg, here)
    if not pkg then
        print("jdb: install <package> [here]")
        return 2
    end
    local cmd = "pkg install " .. pkg
    if here and #here > 0 then cmd = cmd .. " " .. here end
    local ok, status = pcall(os.execute, cmd)
    if not ok then
        print("jdb: install failed: " .. tostring(status))
        return 1
    end
    return tonumber(status) or 0
end

local function cmd_uninstall(pkg)
    if not pkg then
        print("jdb: uninstall <package>")
        return 2
    end
    local ok, status = pcall(os.execute, "pkg remove " .. pkg)
    if not ok then
        print("jdb: uninstall failed: " .. tostring(status))
        return 1
    end
    return tonumber(status) or 0
end

local function cmd_kill(target)
    if not target then
        print("jdb: kill <pid|name>")
        return 2
    end
    local pid = target
    local procs = os.getproc()
    if procs and not procs[pid] then pid = pid_of(target) end
    if not pid then
        print("jdb: no such process: " .. target)
        return 127
    end
    local ok, status = pcall(os.request, 1, "sendsig", { pid = pid, signal = "9" })
    if not ok or status ~= 0 then
        print("jdb: could not kill " .. pid)
        return ok and tonumber(status) or 1
    end
    print("jdb: killed " .. pid)
    return 0
end

local function cmd_shell(words)
    if not words[2] or #words[2] == 0 then
        print("jdb: shell: missing command")
        return 2
    end
    local ok, status = pcall(os.execute, join(words, 2))
    if not ok then
        print("jdb: shell: " .. tostring(status))
        return 1
    end
    return tonumber(status) or 0
end

local function dispatch(words)
    local c = words and words[1]
    if c == "shell" then return cmd_shell(words) end
    if c == "ps" then return cmd_ps() end
    if c == "dumpsys" then return cmd_dumpsys(words[2]) end
    if c == "getproc" then return cmd_getproc(words[2], words[3]) end
    if c == "logcat" then return cmd_logcat(words[2]) end
    if c == "install" then return cmd_install(words[2], words[3]) end
    if c == "uninstall" then return cmd_uninstall(words[2]) end
    if c == "users" then return cmd_users() end
    if c == "kill" or c == "stop" then return cmd_kill(words[2]) end
    if c == "ping" then print("pong") return 0 end
    if c == "version" then print("jdb v" .. version) return 0 end
    print("jdb: unknown command: " .. tostring(c) .. " (try 'jdb help')")
    return 2
end

local function read_line(inp, buf)
    while true do
        local nl = string.find(buf, "\n")
        if nl then
            local line = string.sub(buf, 1, nl - 1)
            buf = string.sub(buf, nl + 1)
            return line, buf
        end
        local data
        local ok, err = pcall(function() data = io.read(inp, 1024) end)
        if not ok then return nil, buf end
        if data == nil or data == "" then return nil, buf end
        buf = buf .. data
    end
end

local function recv_lines(inp, print_lines, buf)
    local status = 0
    while true do
        local line
        line, buf = read_line(inp, buf)
        if not line then break end
        line = string.trim(line)
        if #line > 0 and string.sub(line, 1, 1) == "$" then
            status = tonumber(string.sub(line, 2)) or status
            break
        end
        if #line > 0 and print_lines then print(line) end
    end
    return status
end

local function client_forward(words)
    if not device then return dispatch(words) end
    local ok, conn, inp, outp = pcall(socket.connect, "socket://" .. device.host .. ":" .. tostring(device.port))
    if not ok then
        print("jdb: connect: " .. tostring(conn))
        return 101
    end
    local sent = pcall(io.write, outp, join(words, 1) .. "\n")
    if not sent then
        pcall(io.close, conn, inp, outp)
        print("jdb: failed to send to " .. device.host)
        return 101
    end
    local status = recv_lines(inp, true, "")
    pcall(io.close, conn, inp, outp)
    return status
end

local function do_connect(host, port)
    host = host and #host > 0 and host or DEFAULT_HOST
    port = port and tonumber(port) or PORT
    local ok, conn, inp, outp = pcall(socket.connect, "socket://" .. host .. ":" .. tostring(port))
    if not ok then
        print("jdb: connect failed: " .. tostring(conn))
        return 101
    end
    pcall(io.write, outp, "ping\n")
    local saw_pong = false
    local buf = ""
    while true do
        local line
        line, buf = read_line(inp, buf)
        if not line then break end
        line = string.trim(line)
        if line == "pong" then saw_pong = true end
        if #line > 0 and string.sub(line, 1, 1) == "$" then break end
    end
    pcall(io.close, conn, inp, outp)
    if not saw_pong then
        print("jdb: no jdb daemon at " .. host .. ":" .. tostring(port))
        return 1
    end
    device = { host = host, port = port }
    print("jdb: connected to " .. host .. ":" .. tostring(port))
    return 0
end

local function daemon(payload, dargs, scope, pid, uid)
    local words = split_args(payload or "")
    if #words == 0 then return "jdb daemon ready" end
    local out, code = capture(function() return dispatch(words) end)
    return out .. "\n" .. "$" .. tostring(code)
end

local function serve_socket()
    os.setproc("name", "jdb")
    local srv
    local ok, err = pcall(function() srv = socket.server(PORT) end)
    if not ok then
        print("jdb: daemon: " .. tostring(err))
        return
    end
    print("jdb: daemon listening on port " .. PORT)
    while true do
        local conn, inp, outp
        local aok, aerr = pcall(function() conn, inp, outp = socket.accept(srv) end)
        if not aok or not conn then break end
        local line
        local rok, rerr = pcall(function() line = read_line(inp, "") end)
        if rok and line and #line > 0 then
            local words = split_args(line)
            local out, code = capture(function() return dispatch(words) end)
            pcall(io.write, outp, out .. "\n" .. "$" .. tostring(code) .. "\n")
        end
        pcall(io.close, conn, inp, outp)
    end
end

local function print_help()
    print("jdb v" .. version .. " - OpenTTY debug bridge (adb-like)")
    print("")
    print(" jdb shell <cmd...>        run a command on the device")
    print(" jdb dumpsys [proc]        dump processes (or one process)")
    print(" jdb ps                    list running processes")
    print(" jdb getproc <pid> <field> read a process DB field")
    print(" jdb logcat [n]            print/tail the session log")
    print(" jdb install <pkg> [here]  install a package")
    print(" jdb uninstall <pkg>       remove a package")
    print(" jdb users                 list users (root)")
    print(" jdb kill <pid|name>       terminate a process")
    print(" jdb connect <host> [port]  target a remote device")
    print(" jdb disconnect            clear the remote device")
    print(" jdb devices               list devices")
    print(" jdb version               print version")
    print(" jdb help                  this help")
    print("")
    print("run the device-side daemon with: jdb --deamon")
end

local function main()
    local first = args[1]
    if not first then print_help() return 0 end
    if first == "--deamon" then return "daemon" end
    if first == "help" or first == "-h" or first == "--help" then print_help() return 0 end
    if first == "version" then print("jdb v" .. version) return 0 end
    if first == "connect" then return do_connect(args[2], args[3]) end
    if first == "disconnect" then device = nil print("jdb: disconnected") return 0 end
    if first == "devices" then
        print("jdb devices")
        print("this device\tdevice")
        if device then print(device.host .. ":" .. tostring(device.port) .. "\tdevice") end
        return 0
    end
    return client_forward(args)
end

local mode = main()
if type(mode) == "number" then os.exit(mode) end
if mode == "daemon" then
    os.setproc("name", "jdb")
    local op = args[2] and tonumber(args[2])
    if op and op > 0 then PORT = op end
    pcall(java.run, serve_socket, "jdbd")
    return daemon
end