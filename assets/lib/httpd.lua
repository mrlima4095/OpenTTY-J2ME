local httpd = {}

httpd._routes = {}
httpd._row = 1

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
end
httpd.run = function (port, debug, buffer, mime)
    while true do
        local ok, server = pcall(socket.server, port)
        if not ok then
            pcall(io.close, server)
            print(server)

            if httpd._row == 1 then
                error("httpd - server binding error")
            else
                error("httpd - " .. server)
            end
        end

        if httpd._row == 1 then
            print("httpd - listening at port " .. port)
        end

        local client, i, o = socket.accept(server)
        if client then
            local raw = io.read(i, buffer or 4096)

            if raw then
                if debug then
                    print(trim(raw))
                    end

                local method, route, headers, body = getMethod(raw), getRoute(raw), getHeaders(raw), getBody(raw)
                local handler = httpd._routes[route]

                local response, status = "", "200 OK"
                if not handler then
                    status = "404 Not Found"
                    response = "<h1>404 - Not Found</h1>"
                elseif handler["method"] ~= method then
                    status = "405 Method Not Allowed"
                    response = "<h1>Method Not Allowed</h1>"
                else
                    ok, response = pcall(handler, method, headers, body)
                    if not ok then
                        status = "500 Internal Server Error"
                        response = "<h1>500 Internal Server Error</h1>"
                    else
                        if type(response) == "table" then
                            if response.status then status = response.status end
                            if response.body then response = response.body end
                        end
                    end
                end

                local sendb = "HTTP/1.1 " .. status .. "\r\n" ..
                            "Content-Type: " .. (mime or "text/html") .. "\r\n" ..
                            "Content-Length: " .. #response .. "\r\n\r\n" ..
                            response

                io.write(sendb, o)
                pcall(io.close, i, o)
            end
        end

        pcall(io.close, server, client)
    end
end

return httpd
