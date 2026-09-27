#!/bin/lua

-- scan - TCP port scanner for OpenTTY.
--   scan <host>                common ports
--   scan <host> -c             common ports
--   scan <host> <port>         single port
--   scan <host> <start> <end>  port range
--   scan <host> -s             live screen mode

local version = "1.1.0"

os.setproc("name", "scan")

local common = {
    { 21, "ftp" },
    { 22, "ssh" },
    { 23, "telnet" },
    { 25, "smtp" },
    { 53, "domain" },
    { 80, "http" },
    { 110, "pop3" },
    { 139, "netbios" },
    { 143, "imap" },
    { 443, "https" },
    { 445, "microsoft-ds" },
    { 3389, "rdp" },
    { 4096, "pproxy" },
    { 5432, "postgres" },
    { 6379, "redis" },
    { 8080, "http-alt" },
    { 8443, "https-alt" },
    { 3306, "mysql" },
    { 27017, "mongodb" },
    { 31522, "opentty-mirror" }
}

local function show_help()
    print("OpenTTY Port Scanner v" .. version)
    print("")
    print("Usage: scan <host> [start] [end]")
    print("  scan <host> -c              common ports")
    print("  scan <host> -s              live screen mode")
    print("  scan <host> --screen        live screen mode")
    print("")
    print("Examples:")
    print("  scan opentty.fun           common ports")
    print("  scan opentty.fun 31522     single port")
    print("  scan 10.0.0.1 1 1000       port range")
    print("")
    print("Max range: 2000 ports per run")
end

local host = arg[1]

if host == nil or host == "-h" or host == "--help" then
    show_help()
    if host ~= nil then os.exit(0) end
    os.exit(2)
end

local start_p = nil
local end_p = nil
local screen_mode = false
local numbers = {}

local ai = 2
while arg[ai] ~= nil do
    local value = arg[ai]
    if value == "-s" or value == "--screen" then
        screen_mode = true
    elseif value ~= "-c" then
        table.insert(numbers, value)
    end
    ai = ai + 1
end

if numbers[1] ~= nil then
    local ok1, p1 = pcall(tonumber, numbers[1])
    if ok1 and p1 then
        start_p = p1
        local ok2, p2 = pcall(tonumber, numbers[2])
        if ok2 and p2 then
            end_p = p2
        else
            end_p = p1
        end
    else
        print("scan: usage: scan <host> [start] [end]")
        os.exit(2)
    end
end

local ports = {}

if start_p == nil then
    for i = 1, #common do
        table.insert(ports, common[i][1])
    end
else
    if end_p < start_p then
        local t = start_p
        start_p = end_p
        end_p = t
    end
    if end_p - start_p > 2000 then
        print("scan: range too large (max 2000 ports per run)")
        os.exit(2)
    end
    for p = start_p, end_p do
        table.insert(ports, p)
    end
end

local function port_line(port)
    local ps = tostring(port)
    local pad = string.sub("00000", 1, 5 - #ps)
    return pad .. ps .. "/tcp  open"
end

local function run_screen()
    local previous = graphics.getCurrent()
    local screen = graphics.new("screen", "scan: " .. host)
    local back = graphics.new("command", { label = "Back", type = "back", priority = 1 })
    local stop = graphics.new("command", { label = "Stop", type = "stop", priority = 1 })
    local switch = graphics.new("command", { label = "Switch to...", type = "screen", priority = 2 })
    local status = graphics.new("buffer", { label = "Status", value = "Preparing scan...", style = "monospace" })
    local results = graphics.new("buffer", { label = "Open ports", value = "", style = "monospace" })
    local running = true

    local function add_result(text)
        local current = graphics.GetText(results) or ""
        if current ~= "" then
            graphics.SetText(results, current .. "\n" .. text)
        else
            graphics.SetText(results, text)
        end
    end

    graphics.append(screen, status)
    graphics.append(screen, results)
    graphics.addCommand(screen, back)
    graphics.addCommand(screen, stop)
    graphics.addCommand(screen, switch)
    graphics.SetTicker(screen, "Scanning " .. host .. "...")
    graphics.handler(screen, {
        [back] = function()
            running = false
            graphics.display(previous)
        end,
        [stop] = function()
            running = false
            graphics.SetTicker(screen, "Stopping scan...")
        end,
        [switch] = graphics.taskmngr
    })

    os.setproc("screen", screen)
    graphics.display(screen)

    java.run(function()
        local t0 = java.midlet.uptime()
        local open_count = 0
        local closed = 0

        for i = 1, #ports do
            if running then
                local port = ports[i]
                local progress = tostring(i) .. "/" .. tostring(#ports) .. ": " .. tostring(port) .. "/tcp"
                graphics.SetText(status, progress)
                graphics.SetTicker(screen, "Scanning " .. progress)

                local ok, conn, inp, out = pcall(socket.connect, "socket://" .. host .. ":" .. port)
                if ok then
                    open_count = open_count + 1
                    add_result(port_line(port))
                    pcall(io.close, conn, inp, out)
                else
                    closed = closed + 1
                end
            else
                break
            end
        end

        local ms = java.midlet.uptime() - t0
        if running then
            graphics.SetText(status, "Done: " .. open_count .. " open, " .. closed .. " closed in " .. ms .. "ms")
            graphics.SetTicker(screen, "Scan complete")
        else
            graphics.SetText(status, "Stopped: " .. open_count .. " open, " .. closed .. " closed")
            graphics.SetTicker(screen, "Scan stopped")
        end
    end, "scan:" .. host)
end

if screen_mode then
    run_screen()
    return
end

print("scan: " .. host .. " (" .. #ports .. " ports)")
print("")

local t0 = java.midlet.uptime()
local open_count = 0
local closed = 0

for i = 1, #ports do
    local port = ports[i]
    local ok, conn, inp, out = pcall(socket.connect, "socket://" .. host .. ":" .. port)

    if ok then
        open_count = open_count + 1
        print(port_line(port))
        pcall(io.close, conn, inp, out)
    else
        closed = closed + 1
    end
end

local ms = java.midlet.uptime() - t0
print("")
print("scan: " .. open_count .. " open, " .. closed .. " closed in " .. ms .. "ms")

if open_count > 0 then os.exit(0) end
os.exit(101)
