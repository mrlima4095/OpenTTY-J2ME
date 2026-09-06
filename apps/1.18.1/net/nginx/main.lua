#!/bin/lua

-- OpenTTY Nginx - HTTP Server
-- Works with config files in /etc/nginx/

local version = "1.0.0"

if arg[1] == "--deamon" then
    os.setproc("name", "nginxd")
    os.setproc(false)

    local running = true
    local config = {}
    local routes = {}
    local server_socket = nil

    local function load_config()
        local conf = io.read("/etc/nginx/nginx.conf")
        if conf == "" or conf == nil then
            config.port = 80
            config.root = "/home/"
            config.server_name = "OpenTTY"
            config.worker_processes = 1
            config.error_log = "/tmp/nginx-error.log"
            config.access_log = "/tmp/nginx-access.log"
            config.max_clients = 10
            return
        end

        config.port = 80
        config.root = "/home/"
        config.server_name = "OpenTTY"
        config.worker_processes = 1
        config.error_log = "/tmp/nginx-error.log"
        config.access_log = "/tmp/nginx-access.log"
        config.max_clients = 10

        local lines = string.split(conf, "\n")
        for _, line in pairs(lines) do
            line = string.trim(line)
            if line ~= "" and not string.startswith(line, "#") then
                local sp = string.find(line, " ")
                if sp then
                    local key = string.sub(line, 1, sp - 1)
                    local val = string.sub(line, sp + 1)
                    val = string.trim(val)
                    if string.endswith(val, ";") then
                        val = string.sub(val, 1, -2)
                    end

                    if key == "listen" then
                        config.port = tonumber(val) or 80
                    elseif key == "root" then
                        config.root = val
                    elseif key == "server_name" then
                        config.server_name = val
                    elseif key == "worker_processes" then
                        config.worker_processes = tonumber(val) or 1
                    elseif key == "error_log" then
                        config.error_log = val
                    elseif key == "access_log" then
                        config.access_log = val
                    elseif key == "max_clients" then
                        config.max_clients = tonumber(val) or 10
                    end
                end
            end
        end

        local sites = io.dirs("/etc/nginx/sites-enabled/")
        if sites then
            for _, site in pairs(sites) do
                local site_conf = io.read("/etc/nginx/sites-enabled/" .. site)
                if site_conf and site_conf ~= "" then
                    local slines = string.split(site_conf, "\n")
                    local location = "/"
                    local return_type = "html"
                    for _, sline in pairs(slines) do
                        sline = string.trim(sline)
                        if string.startswith(sline, "location ") then
                            local sp2 = string.find(sline, " ")
                            if sp2 then
                                location = string.sub(sline, sp2 + 1)
                                if string.endswith(location, " {") then
                                    location = string.sub(location, 1, -3)
                                end
                                location = string.trim(location)
                            end
                        elseif string.startswith(sline, "return ") then
                            local sp2 = string.find(sline, " ")
                            if sp2 then
                                local rest = string.sub(sline, sp2 + 1)
                                if string.startswith(rest, "type ") then
                                    return_type = string.sub(rest, 6)
                                end
                            end
                        elseif string.startswith(sline, "proxy_pass ") then
                            local sp2 = string.find(sline, " ")
                            if sp2 then
                                local upstream = string.sub(sline, sp2 + 1)
                                routes[location] = { type = "proxy", upstream = upstream }
                            end
                        elseif string.startswith(sline, "alias ") then
                            local sp2 = string.find(sline, " ")
                            if sp2 then
                                local alias_path = string.sub(sline, sp2 + 1)
                                routes[location] = { type = "alias", path = alias_path }
                            end
                        end
                    end
                end
            end
        end
    end

    local function get_mime_type(path)
        local mime_conf = io.read("/etc/nginx/mime.types")
        if mime_conf and mime_conf ~= "" then
            local lines = string.split(mime_conf, "\n")
            for _, line in pairs(lines) do
                line = string.trim(line)
                if not string.startswith(line, "#") and not string.startswith(line, "types") and line ~= "}" then
                    local sp = string.find(line, " ")
                    if sp then
                        local ext = string.sub(line, 1, sp - 1)
                        local mime = string.sub(line, sp + 1)
                        if string.endswith(mime, ";") then
                            mime = string.sub(mime, 1, -2)
                        end
                        if string.endswith(path, ext) then
                            return mime
                        end
                    end
                end
            end
        end

        if string.endswith(path, ".html") or string.endswith(path, ".htm") then return "text/html"
        elseif string.endswith(path, ".css") then return "text/css"
        elseif string.endswith(path, ".js") then return "application/javascript"
        elseif string.endswith(path, ".json") then return "application/json"
        elseif string.endswith(path, ".txt") then return "text/plain"
        elseif string.endswith(path, ".png") then return "image/png"
        elseif string.endswith(path, ".jpg") or string.endswith(path, ".jpeg") then return "image/jpeg"
        elseif string.endswith(path, ".gif") then return "image/gif"
        elseif string.endswith(path, ".svg") then return "image/svg+xml"
        elseif string.endswith(path, ".xml") then return "application/xml"
        elseif string.endswith(path, ".pdf") then return "application/pdf"
        elseif string.endswith(path, ".mp3") then return "audio/mpeg"
        elseif string.endswith(path, ".wav") then return "audio/wav"
        elseif string.endswith(path, ".lua") then return "text/x-lua"
        else return "application/octet-stream"
        end
    end

    local function status_line(code)
        local msgs = {
            [200] = "OK",
            [201] = "Created",
            [301] = "Moved Permanently",
            [304] = "Not Modified",
            [400] = "Bad Request",
            [403] = "Forbidden",
            [404] = "Not Found",
            [500] = "Internal Server Error",
            [502] = "Bad Gateway",
            [503] = "Service Unavailable",
        }
        return tostring(code) .. " " .. (msgs[code] or "Unknown")
    end

    local function build_response(code, content, content_type, extra_headers)
        local resp = "HTTP/1.1 " .. status_line(code) .. "\r\n"
        resp = resp .. "Server: OpenTTY-nginx/" .. version .. "\r\n"
        resp = resp .. "Content-Type: " .. (content_type or "text/html") .. "\r\n"
        resp = resp .. "Content-Length: " .. string.len(content) .. "\r\n"
        resp = resp .. "Connection: close\r\n"
        if extra_headers then
            for k, v in pairs(extra_headers) do
                resp = resp .. k .. ": " .. v .. "\r\n"
            end
        end
        resp = resp .. "\r\n"
        resp = resp .. content
        return resp
    end

    local function build_error_page(code)
        local body = "<html><head><title>" .. status_line(code) .. "</title></head>"
        body = body .. "<body><center><h1>" .. status_line(code) .. "</h1></center>"
        body = body .. "<hr><center>OpenTTY-nginx/" .. version .. "</center></body></html>"
        return body
    end

    local function log_request(method, path, status, ip)
        local line = ip .. " - - [" .. os.date("%d/%b/%Y:%H:%M:%S") .. '] "' .. method .. " " .. path .. ' HTTP/1.1" ' .. tostring(status)
        pcall(io.write, line .. "\n", config.access_log, "a")
    end

    local function log_error(msg)
        pcall(io.write, "[" .. os.date("%H:%M:%S") .. "] [error] " .. msg .. "\n", config.error_log, "a")
    end

    local function handle_client(client_conn, client_input, client_output)
        local ip = "127.0.0.1"
        pcall(function() ip = socket.peer(client_conn) end)

        local ok, raw_request = pcall(io.read, client_input, 4096)
        if not ok or not raw_request or raw_request == "" then
            pcall(io.close, client_conn)
            return
        end

        local method = "GET"
        local path = "/"
        local headers = {}

        local first_space = string.find(raw_request, " ")
        if first_space then
            method = string.sub(raw_request, 1, first_space - 1)
            local rest = string.sub(raw_request, first_space + 1)
            local second_space = string.find(rest, " ")
            if second_space then
                path = string.sub(rest, 1, second_space - 1)
            end
        end

        local header_block_end = string.find(raw_request, "\r\n\r\n")
        if header_block_end then
            local hdrs = string.sub(raw_request, 1, header_block_end - 1)
            local hlines = string.split(hdrs, "\r\n")
            for i = 2, #hlines do
                local colon = string.find(hlines[i], ":")
                if colon then
                    local hname = string.trim(string.sub(hlines[i], 1, colon - 1))
                    local hval = string.trim(string.sub(hlines[i], colon + 1))
                    headers[hname] = hval
                end
            end
        end

        local query = ""
        local qpos = string.find(path, "?")
        if qpos then
            query = string.sub(path, qpos + 1)
            path = string.sub(path, 1, qpos - 1)
        end

        local response = ""
        local resp_code = 200
        local content_type = "text/html"

        if path == "/nginx-status" then
            local status_page = "Active connections: 1\n"
            status_page = status_page .. "server accepts handled requests\n"
            status_page = status_page .. " 1 1 1\n"
            status_page = status_page .. "Reading: 0 Writing: 1 Waiting: 0\n"
            response = build_response(200, status_page, "text/plain")
        else
            local handled = false
            for route_path, route_data in pairs(routes) do
                if string.startswith(path, route_path) then
                    if route_data.type == "proxy" then
                        local ok2, body, status2 = pcall(socket.http.get, route_data.upstream .. path)
                        if ok2 and body then
                            local ct = "text/html"
                            local hsp = string.find(body, "\r\n\r\n")
                            if hsp then
                                local hb = string.sub(body, 1, hsp)
                                local ctsp = string.find(hb, "Content-Type:")
                                if ctsp then
                                    local ctline = string.sub(hb, ctsp + 13)
                                    local cteol = string.find(ctline, "\r\n")
                                    if cteol then
                                        ct = string.trim(string.sub(ctline, 1, cteol))
                                    end
                                end
                                ct = "text/html"
                            end
                            response = build_response(status2 or 200, body, ct)
                        else
                            response = build_response(502, build_error_page(502), "text/html")
                            resp_code = 502
                        end
                        handled = true
                        break
                    elseif route_data.type == "alias" then
                        local file_path = route_data.path .. string.sub(path, #route_path + 1)
                        local file = io.open(file_path)
                        if file then
                            local content = io.read(file)
                            io.close(file)
                            content_type = get_mime_type(file_path)
                            response = build_response(200, content, content_type)
                        else
                            response = build_response(404, build_error_page(404), "text/html")
                            resp_code = 404
                        end
                        handled = true
                        break
                    end
                end
            end

            if not handled then
                if path == "/" then path = "/index.html" end

                local file_path = config.root
                if string.sub(config.root, -1) == "/" then
                    file_path = config.root .. string.sub(path, 2)
                else
                    file_path = config.root .. path
                end

                local file = io.open(file_path)
                if file then
                    local content = io.read(file)
                    io.close(file)
                    content_type = get_mime_type(file_path)
                    if content == nil or content == "" then
                        response = build_response(200, "", content_type)
                    else
                        response = build_response(200, content, content_type)
                    end
                else
                    resp_code = 404
                    response = build_response(404, build_error_page(404), "text/html")
                end
            end
        end

        local write_ok = pcall(io.write, response, client_output)
        if not write_ok then
            log_error("Failed to write response to " .. ip)
        end

        log_request(method, path, resp_code, ip)
        pcall(io.close, client_conn)
    end

    load_config()

    local ok, err = pcall(function()
        server_socket = socket.server(config.port)
    end)

    if not ok then
        log_error("Cannot bind to port " .. config.port .. ": " .. tostring(err))
        return function(payload, args, scope, pid, uid)
            return ":: nginxd failed to start: port " .. config.port .. " in use"
        end
    end

    java.run(function()
        while running do
            local aok, client_conn, client_input, client_output = pcall(socket.accept, server_socket)
            if aok and client_conn then
                java.run(function()
                    handle_client(client_conn, client_input, client_output)
                end, "nginx-worker")
            end
        end
    end, "nginx-accept")

    return function(payload, args, scope, pid, uid)
        if payload == "reload" then
            load_config()
            return ":: configuration reloaded"
        elseif payload == "stop" then
            running = false
            pcall(io.close, server_socket)
            return ":: nginx stopped"
        elseif payload == "status" then
            return ":: nginx running on port " .. config.port
        elseif payload == "config" then
            return config
        else
            return ":: unknown command"
        end
    end
end

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
    print("  /etc/nginx/nginx.conf       Main config")
    print("  /etc/nginx/mime.types       MIME types")
    print("  /etc/nginx/sites-available/ Site configs")
    print("  /etc/nginx/sites-enabled/   Active sites")
    print("")
    print("Example:")
    print("  nginx start")
    print("  curl http://localhost/")
end

os.setproc("name", "nginx")

if arg[1] == "start" then
    local pid = os.getpid("nginxd")
    if pid then
        print("nginx: already running (pid " .. pid .. ")")
        os.exit(68)
    end

    print("nginx: starting server...")
    os.request("1", "serve", os.join(arg[0]))
    java.sleep(300)
    pid = os.getpid("nginxd")
    if pid then
        print("nginx: started (pid " .. pid .. ")")
    else
        print("nginx: failed to start")
        os.exit(1)
    end

elseif arg[1] == "stop" then
    local pid = os.getpid("nginxd")
    if not pid then
        print("nginx: not running")
        os.exit(1)
    end
    local result = os.request(pid, "stop")
    if result then print("nginx: " .. result) end

elseif arg[1] == "reload" then
    local pid = os.getpid("nginxd")
    if not pid then
        print("nginx: not running")
        os.exit(1)
    end
    local result = os.request(pid, "reload")
    if result then print("nginx: " .. result) end

elseif arg[1] == "status" then
    local pid = os.getpid("nginxd")
    if pid then
        local result = os.request(pid, "status")
        if result then print("nginx: " .. result) end
    else
        print("nginx: not running")
    end

elseif arg[1] == "test" then
    print("nginx: testing configuration...")
    local conf = io.read("/etc/nginx/nginx.conf")
    if conf == "" or conf == nil then
        print("nginx: /etc/nginx/nginx.conf not found")
        os.exit(1)
    end
    print("nginx: configuration test successful")

elseif arg[1] == "help" or arg[1] == "--help" or arg[1] == nil then
    show_help()
else
    print("nginx: '" .. arg[1] .. "' is not a valid command")
    print("Try 'nginx help'")
    os.exit(1)
end
