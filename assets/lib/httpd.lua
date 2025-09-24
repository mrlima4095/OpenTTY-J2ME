local httpd = {}

httpd._routes = {}

local function trim(s)
    if not s or #s == 0 then return "" end
    local start = 1
    while start <= #s and string.sub(s, start, start) == " " do
        start = start + 1
    end
    local finish = #s
    while finish >= start and string.sub(s, finish, finish) == " " do
        finish = finish - 1
    end
    return string.sub(s, start, finish)
end

local function getMethod(raw)
    local buffer = ""
    for i = 1, #raw do
        local cur = string.sub(raw, i, i)
        if cur == " " then break
        else buffer = buffer .. cur
        end
    end
    return buffer
end

local function getHeaders(raw)
    local headers = {}
    local pos = 1
    -- Find end of request line (\r\n)
    local end_req = string.match(raw, "\r\n", pos)
    if not end_req then return headers end
    pos = end_req + 2

    -- Find end of headers (\r\n\r\n)
    local header_end = string.match(raw, "\r\n\r\n", pos)
    if not header_end then header_end = #raw + 1 end

    -- Extract header part (up to but not including \r\n\r\n)
    local header_part = string.sub(raw, pos, header_end - 1)

    -- Parse each header line manually (no regex)
    local line_start = 1
    while line_start <= #header_part do
        local line_end = string.match(header_part, "\r\n", line_start)
        if not line_end then line_end = #header_part + 1 end
        local line = string.sub(header_part, line_start, line_end - 1)

        -- Find colon position
        local colon_pos = string.match(line, ":", 1)
        if colon_pos then
            local key = string.sub(line, 1, colon_pos - 1)
            local value = string.sub(line, colon_pos + 1)
            key = trim(key)
            value = trim(value)
            if key ~= "" then
                headers[key] = value
            end
        end

        line_start = line_end + 2
    end

    return headers
end

local function getRoute(raw)
    -- Find first space (after method)
    local space1 = string.match(raw, " ", 1)
    if not space1 then return "" end
    local start = space1 + 1

    -- Find next space (before HTTP version)
    local space2 = string.match(raw, " ", start)
    if not space2 then space2 = #raw + 1 end

    local path = string.sub(raw, start, space2 - 1)
    return path
end

local function getBody(raw)
    local pos = 1
    -- Find end of request line (\r\n)
    local end_req = string.match(raw, "\r\n", pos)
    if not end_req then return "" end
    pos = end_req + 2

    -- Find end of headers (\r\n\r\n)
    local header_end = string.match(raw, "\r\n\r\n", pos)
    if not header_end then return "" end

    -- Body starts after \r\n\r\n
    local body_start = header_end + 4
    return string.sub(raw, body_start)
end

function httpd.route(path, method, handler)
    if path == nil then error("bad argument #1 for 'route' (string expected, got nil)") end
    if handler == nil then
        handler = method
        method = "GET"
    end

    httpd._routes[path] = httpd._routes[path] or {}
    httpd._routes[path][method] = handler
end

function httpd.run(port)
    local ok, server = pcall(socket.server, port)
    if not ok then
        print("httpd - port in use\nhttpd - server stopped")
        return
    end

    print("httpd - listening at port " .. port)

    while true do
        local client, i, o = socket.accept(server)
        if client then
            local raw = io.read(i)
            if raw then
                local method = getMethod(raw)
                local route = getRoute(raw)
                local headers = getHeaders(raw)
                local body = getBody(raw)

                local path_handlers = httpd._routes[route]
                local handler = path_handlers and path_handlers[method]

                local response = ""
                local status = "200 OK"

                if not handler then
                    status = "404 Not Found"
                    response = "<h1>404 Not Found</h1>"
                else
                    local ok_handler, resp = pcall(handler, method, headers, body)
                    if not ok_handler then
                        status = "500 Internal Server Error"
                        response = "<h1>Internal Server Error</h1>"
                    else
                        response = resp or ""
                        if type(response) == "table" then
                            if response.status then
                                status = response.status
                            end
                            if response.body then
                                response = response.body
                            end
                        end
                    end
                end

                local full = "HTTP/1.1 " .. status .. "\r\n" ..
                             "Content-Type: text/html\r\n" ..
                             "Content-Length: " .. #response .. "\r\n\r\n" ..
                             response

                io.write(full, o)
            end
            pcall(io.close, client, i, o)
        end
    end

    pcall(io.close, server)
end

return httpd