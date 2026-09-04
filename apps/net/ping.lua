#!/bin/lua

-- ping - HTTP and TCP connectivity probe for Lua J2ME / OpenTTY
-- Measures round-trip time using java.midlet.uptime() (milliseconds).

os.setproc("name", "ping")

local function now()
    return java.midlet.uptime()
end

local function usage()
    print("Usage: ping [options] <target>")
    print("")
    print("  ping <host>                 HTTP GET (auto http://)")
    print("  ping http[s]://host[/path]  HTTP GET ping")
    print("  ping socket://host:port     TCP connect ping")
    print("  ping -t <host> <port>       TCP connect ping")
    print("  ping -c <n> <target>        repeat n times (default 1)")
    print("  ping -c 4 -t host 80        four TCP probes")
    print("  ping -h                     show this help")
    print("")
    print("Exit codes: 0 ok, 2 usage, 101 network error")
end

local function parse_http(url)
    if string.startswith(url, "http://") or string.startswith(url, "https://") then
        return url
    end
    return "http://" .. url
end

-- Returns: ok (bool), ms (number|nil), detail (string)
local function ping_http(url)
    url = parse_http(url)
    local t0 = now()
    local ok, body, status = pcall(socket.http.get, url)
    local ms = now() - t0

    if ok then
        return true, ms, "http_code=" .. tostring(status)
    end
    return false, ms, tostring(body)
end

-- uri must be socket://host:port
-- Returns: ok (bool), ms (number|nil), detail (string)
local function ping_socket(uri)
    local t0 = now()
    local ok, conn, inp, out = pcall(socket.connect, uri)
    local ms = now() - t0

    if ok then
        pcall(io.close, conn, inp, out)
        return true, ms, "open"
    end
    return false, ms, tostring(conn)
end

local function is_http_target(s)
    return string.startswith(s, "http://") or string.startswith(s, "https://")
end

local function is_socket_target(s)
    return string.startswith(s, "socket://")
end

-- host:port without scheme (not IPv6)
local function parse_host_port(s)
    local colon = string.find(s, ":")
    if not colon then return nil, nil end
    if string.find(s, "://") then return nil, nil end
    local h = string.sub(s, 1, colon - 1)
    local p = string.sub(s, colon + 1)
    if h == "" or p == "" then return nil, nil end
    return h, p
end

-- --- argument parsing ---
if not arg[1] or arg[1] == "-h" or arg[1] == "--help" then
    usage()
    os.exit(arg[1] and 0 or 2)
end

local count = 1
local mode = nil      -- "http" | "tcp"
local target = nil    -- http url or socket uri
local host = nil
local port = nil

local i = 1
while arg[i] do
    local a = tostring(arg[i])

    if a == "-h" or a == "--help" then
        usage()
        os.exit(0)
    elseif a == "-c" then
        i = i + 1
        if not arg[i] then
            print("ping: option requires an argument -- c")
            os.exit(2)
        end
        local okc, n = pcall(tonumber, arg[i])
        if not okc or not n or n < 1 then
            print("ping: invalid count: " .. tostring(arg[i]))
            os.exit(2)
        end
        count = n
    elseif a == "-t" or a == "-s" then
        mode = "tcp"
        i = i + 1
        host = arg[i]
        i = i + 1
        port = arg[i]
        if not host or not port then
            print("ping: usage: ping -t <host> <port>")
            os.exit(2)
        end
        target = "socket://" .. tostring(host) .. ":" .. tostring(port)
    elseif a == "-H" or a == "--http" then
        mode = "http"
    else
        if not target then
            target = a
            if not mode then
                if is_http_target(a) then
                    mode = "http"
                elseif is_socket_target(a) then
                    mode = "tcp"
                else
                    local h, p = parse_host_port(a)
                    if h and p then
                        mode = "tcp"
                        host = h
                        port = p
                        target = "socket://" .. h .. ":" .. p
                    else
                        mode = "http"
                    end
                end
            end
        end
    end

    i = i + 1
end

if not target then
    usage()
    os.exit(2)
end

if not mode then
    mode = "http"
end

-- banner
if mode == "http" then
    target = parse_http(target)
    print("PING " .. target .. " (HTTP) count=" .. tostring(count))
else
    print("PING " .. target .. " (TCP) count=" .. tostring(count))
end

local sent = 0
local received = 0
local lost = 0
local total_ms = 0
local min_ms = nil
local max_ms = nil

for n = 1, count do
    sent = sent + 1
    local ok, ms, detail

    if mode == "http" then
        ok, ms, detail = ping_http(target)
    else
        ok, ms, detail = ping_socket(target)
    end

    if ok then
        received = received + 1
        total_ms = total_ms + ms
        if min_ms == nil or ms < min_ms then min_ms = ms end
        if max_ms == nil or ms > max_ms then max_ms = ms end
        print("seq=" .. tostring(n) .. " " .. detail .. " time=" .. tostring(ms) .. "ms")
    else
        lost = lost + 1
        if ms then
            print("seq=" .. tostring(n) .. " failed: " .. tostring(detail) .. " time=" .. tostring(ms) .. "ms")
        else
            print("seq=" .. tostring(n) .. " failed: " .. tostring(detail))
        end
    end

    -- pause between probes (except last)
    if n < count then
        pcall(java.sleep, 500)
    end
end

print("--- " .. target .. " ping statistics ---")
print(tostring(sent) .. " transmitted, " .. tostring(received) .. " received, " .. tostring(lost) .. " lost")

if received > 0 then
    local avg = total_ms / received
    print("rtt min/avg/max = " .. tostring(min_ms) .. "/" .. tostring(avg) .. "/" .. tostring(max_ms) .. " ms")
    os.exit(0)
end

os.exit(101)
