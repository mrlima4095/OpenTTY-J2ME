local httpd = {}

httpd._routes = {}
httpd._row = 1

local function log(msg)
    print("[httpd] " .. msg)
end

local function trim(s)
    if not s or #s == 0 then
        return ""
    end
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

local function indexOf(text, pat, start)
    start = start or 1
    local plen, tlen = #pat, #text
    for i = start, tlen - plen + 1 do
        if string.sub(text, i, i + plen - 1) == pat then
            return i
        end
    end
    return nil
end

local function getMethod(raw)
    local buffer = ""
    for i = 1, #raw do
        local cur = string.sub(raw, i, i)
        if cur == " " then
            break
        end
        buffer = buffer .. cur
    end
    return buffer
end

local function getHeaders(raw)
    local headers = {}
    local pos = indexOf(raw, "\r\n", 1)
    if not pos then
        return headers
    end
    pos = pos + 2

    local header_end = indexOf(raw, "\r\n\r\n", pos)
    if not header_end then header_end = #raw + 1 end

    local header_part = string.sub(raw, pos, header_end - 1)
    local line_start = 1
    while line_start <= #header_part do
        local line_end = indexOf(header_part, "\r\n", line_start)
        if not line_end then line_end = #header_part + 1 end
        local line = string.sub(header_part, line_start, line_end - 1)

        local colon_pos = indexOf(line, ":", 1)
        if colon_pos then
            local key = trim(string.sub(line, 1, colon_pos - 1))
            local value = trim(string.sub(line, colon_pos + 1))
            if key ~= "" then
                headers[key] = value
            end
        end

        line_start = line_end + 2
    end

    return headers
end

local function getRoute(raw)
    local space1 = indexOf(raw, " ", 1)
    if not space1 then
        return ""
    end
    local start = space1 + 1
    local space2 = indexOf(raw, " ", start)
    if not space2 then
        space2 = #raw + 1
    end
    return string.sub(raw, start, space2 - 1)
end

local function getBody(raw)
    local end_req = indexOf(raw, "\r\n", 1)
    if not end_req then
        return ""
    end
    local pos = end_req + 2
    local header_end = indexOf(raw, "\r\n\r\n", pos)
    if not header_end then
        return ""
    end
    return string.sub(raw, header_end + 4)
end

httpd.route = function(path, method, handler)
    if not path then
        error("bad argument #1 for 'route' (string expected, got no value)")
    end
    if not handler then
        handler = method
        method = "GET"
    end
    httpd._routes[path] = { method = method, handler = handler }
    log("Registered route: " .. path .. " [" .. method .. "]")
end

httpd.run = function(port, debug, buffer, mime)
    while true do
        log("Starting server on port " .. port)
        local ok, server = pcall(socket.server, port)
        if not ok then
            log("Server binding error: " .. tostring(server))
            error("httpd - server binding error")
        end

        if httpd._row == 1 then
            log("Listening at port " .. port)
        end

        local client, i, o = socket.accept(server)
        if client then
            log("Client connected")
            local ok, raw = pcall(io.read, i, 4096)
            if not ok then
                log("Error reading client data: " .. tostring(raw))
            elseif raw then
                if debug then
                    log("Raw request:\n" .. trim(raw))
                end

                local method, route, headers, body = getMethod(raw), getRoute(raw), getHeaders(raw), getBody(raw)
                log("Method: " .. tostring(method) .. ", Route: " .. tostring(route))
                local handler = httpd._routes[route]

                local response, status = "", "200 OK"
                if not handler then
                    status = "404 Not Found"
                    response = "<h1>404 - Not Found</h1>"
                    log("No handler found for route: " .. route)
                elseif handler["method"] ~= method then
                    status = "405 Method Not Allowed"
                    response = "<h1>Method Not Allowed</h1>"
                    log("Method not allowed: " .. method)
                else
                    local ok2, res = pcall(handler.handler, method, headers, body)
                    if not ok2 then
                        status = "500 Internal Server Error"
                        response = "<h1>500 Internal Server Error</h1>"
                        log("Handler error: " .. tostring(res))
                    else
                        response = res
                        if type(res) == "table" then
                            if res.status then
                                status = res.status
                            end
                            if res.body then
                                response = res.body
                            end
                        end
                        log("Handler executed successfully for route: " .. route)
                    end
                end

                local sendb = "HTTP/1.1 " .. status .. "\r\n" ..
                              "Content-Type: " .. (mime or "text/html") .. "\r\n" ..
                              "Content-Length: " .. #response .. "\r\n\r\n" ..
                              response

                local ok3, err3 = pcall(io.write, sendb, o)
                if not ok3 then
                    log("Error sending response: " .. tostring(err3))
                end

                pcall(io.close, i, o)
            end
        end
        pcall(io.close, server, client)
    end
end

return httpd
