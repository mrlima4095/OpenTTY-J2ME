#!/bin/lua

-- scan - TCP port scanner for OpenTTY.
--   scan <host>                common ports
--   scan <host> -c             common ports
--   scan <host> <port>         single port
--   scan <host> <start> <end>  port range

local version = "1.0.0"

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

if arg[2] ~= nil and arg[2] ~= "-c" then
    local ok1, p1 = pcall(tonumber, arg[2])
    if ok1 and p1 then
        start_p = p1
        local ok2, p2 = pcall(tonumber, arg[3])
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
        local ps = tostring(port)
        local pad = string.sub("00000", 1, 5 - #ps)
        print(pad .. ps .. "/tcp  open")
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