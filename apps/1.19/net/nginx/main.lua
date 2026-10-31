#!/bin/lua

-- OpenTTY Nginx - HTTP/1.1 static + proxy server
--
-- Layout (nginx compatible, all under /etc/nginx/):
--   /etc/nginx/nginx.conf             flat directives (listen, root, index, ...)
--   /etc/nginx/mime.types             "<mime-type> <extension> ..." lines
--   /etc/nginx/sites-enabled/<file>   location blocks (alias / proxy_pass)
--
-- `nginx start` creates those files when they are missing (needs root).
--
-- Concurrency note: OpenTTY gives each process its own Lua interpreter, and a
-- script function owns that interpreter until it returns. A background thread
-- blocked in socket.accept keeps the ownership, so:
--   * per-connection worker threads never run (the client waits forever), and
--   * a `serve` handler for that process can never answer, so `os.request`
--     to the daemon deadlocks while it is idle in accept.
-- Requests are therefore handled inline in the accept thread (like
-- /lib/httpd.lua), and stop/reload go through a control file plus a wake-up
-- connection instead of an os.request round trip.

local version = "1.1.0"

local CONF_DIR = "/etc/nginx/"
local CONF_MAIN = "/etc/nginx/nginx.conf"
local CONF_MIME = "/etc/nginx/mime.types"
local SITES_AVAILABLE = "/etc/nginx/sites-available/"
local SITES_ENABLED = "/etc/nginx/sites-enabled/"

local PID_FILE = "/tmp/nginx.pid"
local FAIL_FILE = "/tmp/nginx.fail"
local STOP_FLAG = "/tmp/nginx.stop"
local RELOAD_FLAG = "/tmp/nginx.reload"

local DEFAULT_CONF = "# OpenTTY Nginx configuration\n" ..
    "# /etc/nginx/nginx.conf - created by 'nginx start' when missing.\n" ..
    "# One directive per line; '#' starts a comment.\n" ..
    "\n" ..
    "listen 80;\n" ..
    "root /home/;\n" ..
    "index index.html;\n" ..
    "server_name OpenTTY;\n" ..
    "worker_processes 1;\n" ..
    "max_clients 10;\n" ..
    "error_log /tmp/nginx-error.log;\n" ..
    "access_log /tmp/nginx-access.log;\n"

local DEFAULT_MIME = "# OpenTTY Nginx MIME types\n" ..
    "# /etc/nginx/mime.types - <mime-type> <extension> ...\n" ..
    "\n" ..
    "text/html html htm\n" ..
    "text/css css\n" ..
    "application/javascript js\n" ..
    "application/json json\n" ..
    "text/plain txt\n" ..
    "image/png png\n" ..
    "image/jpeg jpg jpeg\n" ..
    "image/gif gif\n" ..
    "image/svg+xml svg\n" ..
    "application/xml xml\n" ..
    "application/pdf pdf\n" ..
    "audio/mpeg mp3\n" ..
    "audio/wav wav\n" ..
    "text/x-lua lua\n" ..
    "application/octet-stream bin\n"

local STATUS = {
    [200] = "OK",
    [201] = "Created",
    [204] = "No Content",
    [301] = "Moved Permanently",
    [302] = "Found",
    [304] = "Not Modified",
    [400] = "Bad Request",
    [403] = "Forbidden",
    [404] = "Not Found",
    [405] = "Method Not Allowed",
    [408] = "Request Timeout",
    [500] = "Internal Server Error",
    [502] = "Bad Gateway",
    [503] = "Service Unavailable",
}

-- Used only when /etc/nginx/mime.types is missing.
local MIME_FALLBACK = {
    html = "text/html", htm = "text/html",
    css = "text/css",
    js = "application/javascript",
    json = "application/json",
    txt = "text/plain",
    png = "image/png",
    jpg = "image/jpeg", jpeg = "image/jpeg",
    gif = "image/gif",
    svg = "image/svg+xml",
    xml = "application/xml",
    pdf = "application/pdf",
    mp3 = "audio/mpeg",
    wav = "audio/wav",
    lua = "text/x-lua",
}

-- Helpers ---------------------------------------------------------------

-- Strip a '#' comment (Lua's string.find is a literal search in this VM).
local function strip_comment(line)
    local pos = string.find(line, "#")
    if pos then return string.sub(line, 1, pos - 1) end
    return line
end

-- Trim, drop comments and the trailing ';'.
local function clean_value(value)
    value = string.trim(strip_comment(value or ""))
    if string.endswith(value, ";") then value = string.sub(value, 1, -2) end
    return string.trim(value)
end

-- "key rest" -> key, rest
local function split_directive(line)
    local pos = string.find(line, " ")
    if not pos then return line, "" end
    return string.sub(line, 1, pos - 1), string.trim(string.sub(line, pos + 1))
end

local function default_config()
    return {
        port = 80,
        root = "/home/",
        index = "index.html",
        server_name = "OpenTTY",
        worker_processes = 1,
        max_clients = 10,
        error_log = "/tmp/nginx-error.log",
        access_log = "/tmp/nginx-access.log",
    }
end

-- "<mime> <ext> <ext> ..." (nginx) or the legacy "<ext> <mime>" layout.
local function parse_mime(text)
    local types = {}
    for _, raw in ipairs(string.split(text or "", "\n")) do
        local line = clean_value(raw)
        if line ~= "" then
            local parts = string.split(line, " ")
            local first = parts[1]
            if first and first ~= "" then
                if string.find(first, "/") then
                    for i = 2, #parts do
                        local ext = parts[i]
                        if ext ~= "" then types[ext] = first end
                    end
                else
                    local mime = parts[2]
                    if mime and mime ~= "" then types[first] = mime end
                end
            end
        end
    end
    return types
end

local function parse_config(text)
    local conf = default_config()
    for _, raw in ipairs(string.split(text or "", "\n")) do
        local line = clean_value(raw)
        if line ~= "" then
            local key, value = split_directive(line)
            if key == "listen" then
                local port = tonumber(split_directive(value))
                if port then conf.port = port end
            elseif key == "root" then
                if value ~= "" then conf.root = value end
            elseif key == "index" then
                if value ~= "" then conf.index = value end
            elseif key == "server_name" then
                if value ~= "" then conf.server_name = value end
            elseif key == "worker_processes" then
                conf.worker_processes = tonumber(value) or conf.worker_processes
            elseif key == "max_clients" then
                conf.max_clients = tonumber(value) or conf.max_clients
            elseif key == "error_log" then
                if value ~= "" then conf.error_log = value end
            elseif key == "access_log" then
                if value ~= "" then conf.access_log = value end
            end
        end
    end
    return conf
end

-- One `location <prefix> { alias <dir>; }` or `{ proxy_pass <url>; }` per
-- block. The longest matching prefix wins when a request is routed.
local function parse_site(text, routes)
    local location = nil
    for _, raw in ipairs(string.split(text or "", "\n")) do
        local line = clean_value(raw)
        if line ~= "" then
            if string.startswith(line, "location ") then
                location = string.trim(string.sub(line, 10))
                if string.endswith(location, "{") then
                    location = string.trim(string.sub(location, 1, -2))
                end
            elseif line == "}" then
                location = nil
            elseif location and string.startswith(line, "alias ") then
                local target = clean_value(string.sub(line, 7))
                if target ~= "" then
                    table.insert(routes, { prefix = location, kind = "alias", path = target })
                end
            elseif location and string.startswith(line, "proxy_pass ") then
                local target = clean_value(string.sub(line, 12))
                if target ~= "" then
                    table.insert(routes, { prefix = location, kind = "proxy", upstream = target })
                end
            end
        end
    end
end

local function status_line(code)
    local message = STATUS[code]
    if message == nil then message = "Unknown" end
    return tostring(code) .. " " .. message
end

-- `#text` is the character count; HTTP wants the UTF-8 byte count, and
-- io.write() encodes the response as UTF-8.
local function utf8_length(text)
    local total = 0
    for i = 1, string.len(text) do
        local byte = string.byte(text, i)
        if byte < 0x80 then total = total + 1
        elseif byte < 0x800 then total = total + 2
        elseif byte < 0x10000 then total = total + 3
        else total = total + 4 end
    end
    return total
end

local function build_response(code, body, content_type, extra, head_only)
    body = body or ""
    local response = "HTTP/1.1 " .. status_line(code) .. "\r\n"
    response = response .. "Server: OpenTTY-nginx/" .. version .. "\r\n"
    response = response .. "Content-Type: " .. (content_type or "text/html") .. "\r\n"
    response = response .. "Content-Length: " .. tostring(utf8_length(body)) .. "\r\n"
    response = response .. "Connection: close\r\n"
    if extra then
        for name, value in pairs(extra) do
            response = response .. name .. ": " .. tostring(value) .. "\r\n"
        end
    end
    response = response .. "\r\n"
    if not head_only then response = response .. body end
    return response
end

local function build_error_page(code)
    local title = status_line(code)
    return "<html><head><title>" .. title .. "</title></head>" ..
        "<body><center><h1>" .. title .. "</h1></center><hr>" ..
        "<center>OpenTTY-nginx/" .. version .. "</center></body></html>"
end

local function flag_set(path)
    local content = io.read(path)
    return content ~= nil and content ~= ""
end

local function read_file(path)
    if path == nil or path == "" then return nil end
    local handle = io.open(path)
    if not handle then return nil end
    local content = io.read(handle, -1)
    io.close(handle)
    return content or ""
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

-- Wake the accept loop up so it can read a control flag.
local function poke(port)
    if port == nil then return false end
    local ok, conn, input, output = pcall(socket.connect, "socket://127.0.0.1:" .. tostring(port))
    if not ok or conn == nil then return false end
    pcall(io.write, "GET /__nginx/control HTTP/1.0\r\n\r\n", output)
    pcall(io.close, output)
    pcall(io.close, input)
    pcall(io.close, conn)
    return true
end

-- Create /etc/nginx/{nginx.conf,mime.types,sites-*} when missing (root only).
local function seed_config()
    if os.getuid() ~= 0 then return false end
    pcall(os.mkdir, CONF_DIR)
    pcall(os.mkdir, SITES_AVAILABLE)
    pcall(os.mkdir, SITES_ENABLED)
    if io.read(CONF_MAIN) == "" then pcall(io.write, DEFAULT_CONF, CONF_MAIN) end
    if io.read(CONF_MIME) == "" then pcall(io.write, DEFAULT_MIME, CONF_MIME) end
    return true
end

-- Daemon ----------------------------------------------------------------

if arg[1] == "--deamon" then
    os.setproc("name", "nginxd")
    os.setproc(false)

    local running = true
    local config = default_config()
    local mimes = {}
    local routes = {}
    local server_socket = nil
    local requests = 0
    local started_at = os.time()

    local function log_error(message)
        pcall(io.write, "[" .. os.date("%H:%M:%S") .. "] [error] " .. message .. "\n", config.error_log, "a")
    end

    local function log_request(method, target, code, ip)
        local line = ip .. " - - [" .. os.date("%d/%b/%Y:%H:%M:%S") .. '] "' ..
            method .. " " .. target .. ' HTTP/1.1" ' .. tostring(code)
        pcall(io.write, line .. "\n", config.access_log, "a")
    end

    local function load_config()
        local conf = parse_config(io.read(CONF_MAIN))
        local parsed_routes = {}

        local sites = io.dirs(SITES_ENABLED)
        if sites then
            for _, entry in ipairs(sites) do
                local name = entry
                if name ~= ".." and name ~= "." and not string.startswith(name, ".") and not string.endswith(name, "/") then
                    parse_site(io.read(SITES_ENABLED .. name), parsed_routes)
                end
            end
        end

        config = conf
        mimes = parse_mime(io.read(CONF_MIME))
        routes = parsed_routes
    end

    local function mime_for(path)
        local dot = 0
        for i = 1, string.len(path) do
            if string.sub(path, i, i) == "." then dot = i end
        end
        if dot == 0 then return "application/octet-stream" end

        local ext = string.lower(string.sub(path, dot + 1))
        local known = mimes[ext]
        if known == nil then known = MIME_FALLBACK[ext] end
        if known == nil then return "application/octet-stream" end
        return known
    end

    -- Try <base><rel>, then <that>/index.html.
    local function resolve_static(base, rel)
        if not string.endswith(base, "/") then base = base .. "/" end
        local candidate = base
        if rel ~= "" then candidate = base .. rel end

        local content = read_file(candidate)
        if content ~= nil then return candidate, content end

        local index_path = candidate
        if not string.endswith(index_path, "/") then index_path = index_path .. "/" end
        index_path = index_path .. config.index
        content = read_file(index_path)
        if content ~= nil then return index_path, content end

        return nil, nil
    end

    -- Percent-decoding for the request target.
    local function decode_path(path)
        local out = ""
        local i = 1
        while i <= string.len(path) do
            local char = string.sub(path, i, i)
            if char == "%" then
                local code = tonumber(string.sub(path, i + 1, i + 2), 16)
                if code then
                    out = out .. string.char(code)
                    i = i + 3
                else
                    out = out .. char
                    i = i + 1
                end
            else
                out = out .. char
                i = i + 1
            end
        end
        return out
    end

    -- Reject traversal: returns the path below the root ("a/b.txt", "" for /).
    local function relative_path(path)
        local clean = {}
        for _, part in ipairs(string.split(path, "/")) do
            if part == ".." then return nil end
            if part ~= "" and part ~= "." then table.insert(clean, part) end
        end
        return table.concat(clean, "/")
    end

    local function match_route(path)
        local best, best_length = nil, -1
        for _, route in ipairs(routes) do
            local prefix = route.prefix
            if string.startswith(path, prefix) and string.len(prefix) > best_length then
                best = route
                best_length = string.len(prefix)
            end
        end
        return best
    end

    local function status_page()
        local body = "OpenTTY-nginx/" .. version .. "\n"
        body = body .. "Active connections: 1\n"
        body = body .. "server accepts handled requests\n"
        body = body .. " " .. tostring(requests) .. " " .. tostring(requests) .. " " .. tostring(requests) .. "\n"
        body = body .. "pid: " .. os.getpid() .. "\n"
        body = body .. "port: " .. tostring(config.port) .. "\n"
        body = body .. "root: " .. config.root .. "\n"
        body = body .. "routes: " .. tostring(#routes) .. "\n"
        body = body .. "uptime: " .. tostring(os.time() - started_at) .. "s\n"
        return body
    end

    -- Read the request head (up to 8 KB); the full request line is needed
    -- before routing, so a single short read is not enough.
    local function read_request(input)
        local data = ""
        while true do
            local chunk = io.read(input, 1024)
            if chunk == nil or chunk == "" then break end
            data = data .. chunk
            if string.find(data, "\r\n\r\n") or string.len(data) >= 8192 then break end
        end
        return data
    end

    local function handle_client(client, input, output)
        local ip = "127.0.0.1"
        pcall(function() ip = socket.peer(client) end)

        local raw = read_request(input)
        if raw == "" then
            pcall(io.close, client)
            return
        end

        local method = "GET"
        local target = "/"
        local first_space = string.find(raw, " ")
        if first_space then
            method = string.upper(string.sub(raw, 1, first_space - 1))
            local rest = string.sub(raw, first_space + 1)
            local second_space = string.find(rest, " ")
            if second_space then target = string.sub(rest, 1, second_space - 1) else target = rest end
        end

        local query = ""
        local path = target
        local qpos = string.find(target, "?")
        if qpos then
            query = string.sub(target, qpos + 1)
            path = string.sub(target, 1, qpos - 1)
        end
        if path == "" then path = "/" end

        local code, body, content_type = 200, "", "text/html"
        local extra = nil

        if method ~= "GET" and method ~= "HEAD" then
            code, body, content_type = 405, build_error_page(405), "text/html"
            extra = { Allow = "GET, HEAD" }
        elseif path == "/nginx-status" then
            code, body, content_type = 200, status_page(), "text/plain"
        else
            local route = match_route(path)
            if route and route.kind == "proxy" then
                local upstream = route.upstream
                if not string.endswith(upstream, "/") then upstream = upstream .. "/" end
                local tail = string.sub(path, string.len(route.prefix) + 1)
                local pok, upstream_body, upstream_code = pcall(socket.http.get, upstream .. tail)
                if pok and upstream_body then
                    code, body, content_type = upstream_code or 200, upstream_body, "text/html"
                else
                    code, body, content_type = 502, build_error_page(502), "text/html"
                end
            else
                local base, rel = config.root, nil
                if route and route.kind == "alias" then
                    base = route.path
                    rel = string.sub(path, string.len(route.prefix) + 1)
                else
                    rel = relative_path(decode_path(path))
                end

                local file = nil
                if rel ~= nil then file, body = resolve_static(base, rel) end

                if file ~= nil then
                    code, content_type = 200, mime_for(file)
                else
                    code, body, content_type = 404, build_error_page(404), "text/html"
                end
            end
        end

        requests = requests + 1
        local response = build_response(code, body, content_type, extra, method == "HEAD")
        if not pcall(io.write, response, output) then
            log_error("failed to write the response to " .. ip)
        end

        local logged = path
        if query ~= "" then logged = path .. "?" .. query end
        log_request(method, logged, code, ip)

        pcall(io.close, output)
        pcall(io.close, input)
        pcall(io.close, client)
    end

    load_config()

    local ok, server = pcall(socket.server, config.port)
    if not ok then
        local message = "cannot bind to port " .. tostring(config.port) .. ": " .. tostring(server)
        log_error(message)
        pcall(io.write, message, FAIL_FILE)
        pcall(os.request, "1", "sendsig", { pid = os.getpid(), signal = "9" })
        return nil
    end

    server_socket = server
    pcall(io.write, os.getpid() .. " " .. tostring(config.port), PID_FILE)
    pcall(io.write, "", STOP_FLAG)
    pcall(io.write, "", RELOAD_FLAG)

    -- | Some MIDP stacks invalidate the listening socket as soon as the
    -- | accepted connection is closed (`accept` then throws IOException even
    -- | though the listener was never touched). Re-binding the port after each
    -- | request keeps the server reachable on such devices.
    local function bind_listener()
        pcall(io.close, server_socket)
        local bound, fresh = pcall(socket.server, config.port)
        if bound then
            server_socket = fresh
            pcall(io.write, os.getpid() .. " " .. tostring(config.port), PID_FILE)
        end
        return bound
    end

    java.run(function()
        local failures = 0

        while running do
            local aok, client, input, output = pcall(socket.accept, server_socket)

            if aok and client then
                failures = 0

                if flag_set(STOP_FLAG) then
                    pcall(io.write, "", STOP_FLAG)
                    running = false
                    pcall(io.close, client)
                elseif flag_set(RELOAD_FLAG) then
                    pcall(io.write, "", RELOAD_FLAG)
                    pcall(io.close, client)
                    load_config()
                else
                    -- One bad request must never take the server down (a J2ME
                    -- read on a half-closed socket throws, and an exception here
                    -- would end the accept thread and the whole daemon).
                    local ok, err = pcall(handle_client, client, input, output)
                    if not ok then
                        log_error("request failed: " .. tostring(err))
                        pcall(io.close, client)
                        pcall(io.close, input)
                        pcall(io.close, output)
                    end
                end

                if running and not bind_listener() then
                    failures = failures + 1
                    log_error("cannot rebind port " .. tostring(config.port))
                    if failures >= 5 then running = false end
                end
            elseif aok then
                java.sleep(10)
            else
                -- Transient accept errors are retried; a socket that keeps
                -- failing (closed underneath us) ends the loop after five tries.
                failures = failures + 1
                log_error("accept failed (" .. tostring(failures) .. "): " .. tostring(client))
                if failures >= 5 then
                    running = false
                elseif not bind_listener() then
                    java.sleep(200)
                end
            end
        end

        pcall(io.close, server_socket)
        pcall(io.write, "", PID_FILE)
        -- Drop the process from the process table so `ps` stops listing nginxd.
        pcall(os.request, "1", "sendsig", { pid = os.getpid(), signal = "9" })
    end, "nginx-accept")

    -- Stay registered as a service: returning a handler keeps the process (and
    -- its listening socket) alive after the chunk finishes.
    return function(payload, args, scope, pid, uid)
        local event = tostring(payload)

        if event == "stop" then
            running = false
            poke(config.port)
            return "stopping"
        elseif event == "reload" then
            load_config()
            return "reloaded"
        elseif event == "status" then
            local _, port = read_pidfile()
            return "running (pid " .. tostring(os.getpid()) .. port_suffix(port) .. ")"
        end

        return "ok"
    end
end

-- Command line ----------------------------------------------------------

local function show_help()
    print("OpenTTY Nginx v" .. version)
    print("")
    print("Usage: nginx [command]")
    print("")
    print("Commands:")
    print("  start           Start nginx server (background)")
    print("  stop            Stop nginx server")
    print("  reload          Reload configuration")
    print("  status          Show server status")
    print("  test            Test configuration")
    print("  help            Show this help")
    print("")
    print("Configuration:")
    print("  " .. CONF_MAIN .. "     Main config")
    print("  " .. CONF_MIME .. "     MIME types")
    print("  " .. SITES_AVAILABLE .. " Site configs")
    print("  " .. SITES_ENABLED .. "   Active sites")
    print("")
    print("Example:")
    print("  nginx start")
    print("  curl http://localhost/")
end

os.setproc("name", "nginx")

local command = arg[1]

if command == "start" then
    local pid = os.getpid("nginxd")
    if pid then
        local _, port = read_pidfile()
        print("nginx: already running (pid " .. pid .. port_suffix(port) .. ")")
        os.exit(68)
    end

    if not seed_config() then
        print("nginx: warning: not root, configuration is not seeded into " .. CONF_DIR)
    end

    pcall(io.write, "", FAIL_FILE)

    print("nginx: starting server...")
    os.request("1", "serve", os.join(arg[0]))
    java.sleep(400)

    pid = os.getpid("nginxd")
    if pid then
        local _, port = read_pidfile()
        print("nginx: started (pid " .. pid .. port_suffix(port) .. ")")
    else
        local message = io.read(FAIL_FILE)
        if message ~= nil and message ~= "" then
            print("nginx: " .. message)
        else
            print("nginx: failed to start")
        end
        os.exit(1)
    end

elseif command == "stop" then
    local pid, port = read_pidfile()
    if pid == nil or os.getpid("nginxd") == nil then
        print("nginx: not running")
        os.exit(1)
    end

    pcall(io.write, "1", STOP_FLAG)
    if not poke(port) then
        print("nginx: running, but the control connection to port " .. tostring(port) .. " failed")
        os.exit(1)
    end

    local waited = 0
    while os.getpid("nginxd") and waited < 3000 do
        java.sleep(100)
        waited = waited + 100
    end

    if os.getpid("nginxd") then
        print("nginx: stop requested (pid " .. pid .. " is still busy)")
    else
        print("nginx: stopped")
    end

elseif command == "reload" then
    local pid, port = read_pidfile()
    if pid == nil or os.getpid("nginxd") == nil then
        print("nginx: not running")
        os.exit(1)
    end

    pcall(io.write, "1", RELOAD_FLAG)
    if not poke(port) then
        print("nginx: running, but the control connection to port " .. tostring(port) .. " failed")
        os.exit(1)
    end

    local waited = 0
    while flag_set(RELOAD_FLAG) and waited < 3000 do
        java.sleep(100)
        waited = waited + 100
    end

    if flag_set(RELOAD_FLAG) then
        print("nginx: reload requested (pid " .. pid .. " is still busy)")
    else
        print("nginx: configuration reloaded")
    end

elseif command == "status" then
    local pid, port = read_pidfile()
    if pid ~= nil and os.getpid("nginxd") ~= nil then
        print("nginx: running (pid " .. pid .. port_suffix(port) .. ")")
    else
        print("nginx: not running")
    end

elseif command == "test" then
    print("nginx: testing configuration...")
    local text = io.read(CONF_MAIN)
    if text == "" then
        print("nginx: " .. CONF_MAIN .. " not found (run 'nginx start' to create it)")
        os.exit(1)
    end

    local conf = parse_config(text)
    local routes = {}
    local sites = io.dirs(SITES_ENABLED)
    local enabled = 0
    if sites then
        for _, entry in ipairs(sites) do
            local name = entry
            if name ~= ".." and name ~= "." and not string.startswith(name, ".") and not string.endswith(name, "/") then
                enabled = enabled + 1
                parse_site(io.read(SITES_ENABLED .. name), routes)
            end
        end
    end

    if conf.port == nil or conf.port < 1 or conf.port > 65535 then
        print("nginx: invalid 'listen' port in " .. CONF_MAIN)
        os.exit(1)
    end

    print("nginx: configuration file " .. CONF_MAIN .. " test is successful")
    print("  listen " .. tostring(conf.port) .. ";  root " .. conf.root .. ";  index " .. conf.index)
    print("  sites enabled " .. tostring(enabled) .. ";  routes " .. tostring(#routes))

elseif command == "help" or command == "--help" or command == nil then
    show_help()

else
    print("nginx: '" .. tostring(command) .. "' is not a valid command")
    print("Try 'nginx help'")
    os.exit(1)
end
