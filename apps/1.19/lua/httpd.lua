-- rock: name=httpd
-- rock: version=1.1.0
-- rock: description=Small HTTP/1.1 server with routing, query parsing and static files
-- rock: here=/lib/httpd.lua

-- Pure-Lua HTTP server built on `socket.server`/`socket.accept`. It handles
-- one connection at a time, which is what the J2ME runtime supports without
-- threads; use `java.run` if you need a second listener.
local httpd = {}
httpd.__routes = {}

local STATUS = {
    [200] = "OK",
    [201] = "Created",
    [204] = "No Content",
    [301] = "Moved Permanently",
    [302] = "Found",
    [400] = "Bad Request",
    [401] = "Unauthorized",
    [403] = "Forbidden",
    [404] = "Not Found",
    [405] = "Method Not Allowed",
    [500] = "Internal Server Error"
}

local MIME = {
    [".html"] = "text/html",
    [".htm"] = "text/html",
    [".css"] = "text/css",
    [".js"] = "application/javascript",
    [".json"] = "application/json",
    [".txt"] = "text/plain",
    [".md"] = "text/markdown",
    [".png"] = "image/png",
    [".jpg"] = "image/jpeg",
    [".jpeg"] = "image/jpeg",
    [".gif"] = "image/gif",
    [".svg"] = "image/svg+xml",
    [".xml"] = "application/xml",
    [".lua"] = "text/x-lua"
}

local function split_first(text, sep)
    local i = string.find(text, sep)
    if not i then return text, "" end
    return string.sub(text, 1, i - 1), string.sub(text, i + #sep)
end

local function trim(text)
    return string.trim(text or "")
end

local function hex_digit(c)
    local b = string.byte(c)
    if b == nil then return nil end
    if b >= 48 and b <= 57 then return b - 48 end
    if b >= 65 and b <= 70 then return b - 55 end
    if b >= 97 and b <= 102 then return b - 87 end
    return nil
end

local function url_decode(text)
    if not text or text == "" then return "" end
    local out = {}
    local i = 1
    while i <= #text do
        local c = string.sub(text, i, i)
        if c == "%" and i + 2 <= #text then
            local hi = hex_digit(string.sub(text, i + 1, i + 1))
            local lo = hex_digit(string.sub(text, i + 2, i + 2))
            if hi ~= nil and lo ~= nil then
                out[#out + 1] = string.char(hi * 16 + lo)
                i = i + 3
            else
                out[#out + 1] = c
                i = i + 1
            end
        elseif c == "+" then
            out[#out + 1] = " "
            i = i + 1
        else
            out[#out + 1] = c
            i = i + 1
        end
    end
    return table.concat(out)
end

local function parse_query(query_string)
    local query = {}
    if query_string == nil or query_string == "" then return query end
    for _, pair in ipairs(string.split(query_string, "&")) do
        local key, value = split_first(pair, "=")
        query[url_decode(key)] = url_decode(value)
    end
    return query
end

local function parse_request(payload)
    local request = {
        method = "GET",
        path = "/",
        query = {},
        headers = {},
        body = "",
        payload = payload
    }
    if not payload or payload == "" then return request end

    local line_end = string.find(payload, "\r\n")
    local request_line = line_end and string.sub(payload, 1, line_end - 1) or payload
    local method, target = split_first(request_line, " ")
    request.method = string.upper(trim(method))
    target = trim(target)

    local query_string = ""
    local qpos = string.find(target, "?")
    if qpos then
        query_string = string.sub(target, qpos + 1)
        target = string.sub(target, 1, qpos - 1)
    end
    request.path = target ~= "" and target or "/"
    request.query = parse_query(query_string)

    if line_end then
        local header_end = string.find(payload, "\r\n\r\n", line_end)
        local block
        if header_end then
            block = string.sub(payload, line_end + 2, header_end - 1)
            request.body = string.sub(payload, header_end + 4)
        else
            block = string.sub(payload, line_end + 2)
        end
        for _, line in ipairs(string.split(block, "\r\n")) do
            local colon = string.find(line, ":")
            if colon then
                local name = string.lower(trim(string.sub(line, 1, colon - 1)))
                local value = trim(string.sub(line, colon + 1))
                request.headers[name] = value
            end
        end
    end

    return request
end

local function find_route(path, method)
    local route = httpd.__routes[path]
    if route == nil then return nil end
    if route.method == method or route.method == "ANY" then return route.handler end
    return nil
end

local function build_response(status, content, content_type)
    content = content or ""
    content_type = content_type or "text/plain"
    local message = STATUS[status] or "Unknown"
    return "HTTP/1.1 " .. status .. " " .. message .. "\r\n" ..
        "Content-Type: " .. content_type .. "\r\n" ..
        "Content-Length: " .. #content .. "\r\n" ..
        "Connection: close\r\n\r\n" .. content
end

local function normalize_response(result)
    if type(result) == "string" then
        return { status = 200, content = result, content_type = "text/html" }
    end
    if type(result) == "table" then
        return {
            status = result.status or 200,
            content = result.content or "",
            content_type = result.content_type or "text/html"
        }
    end
    return { status = 204, content = "", content_type = "text/plain" }
end

function httpd.route(endpoint, method, handler)
    if type(method) == "function" then
        handler = method
        method = "GET"
    end
    method = string.upper(method or "GET")
    httpd.__routes[endpoint] = { method = method, handler = handler }
end

function httpd.get(endpoint, handler) return httpd.route(endpoint, "GET", handler) end
function httpd.post(endpoint, handler) return httpd.route(endpoint, "POST", handler) end
function httpd.put(endpoint, handler) return httpd.route(endpoint, "PUT", handler) end
function httpd.delete(endpoint, handler) return httpd.route(endpoint, "DELETE", handler) end

local function extension_of(path)
    local i = #path
    while i > 0 do
        local c = string.sub(path, i, i)
        if c == "." then return string.lower(string.sub(path, i)) end
        if c == "/" then return "" end
        i = i - 1
    end
    return ""
end

function httpd.static(root_dir)
    if string.sub(root_dir, -1) ~= "/" then root_dir = root_dir .. "/" end
    return function(request)
        local path = request.path
        if path == "/" then path = "/index.html" end
        local file_path = root_dir .. string.sub(path, 2)
        local content = io.read(file_path)
        if content == nil then
            return { status = 404, content = "404 Not Found", content_type = "text/plain" }
        end
        local content_type = MIME[extension_of(path)] or "application/octet-stream"
        return { status = 200, content = content, content_type = content_type }
    end
end

function httpd.run(port)
    port = port or 8080
    print("HTTP server listening on port " .. port)
    local server = socket.server(port)
    if server == nil then
        print("HTTP server: could not bind port " .. port)
        return
    end

    while true do
        local ok, conn, input, output = pcall(socket.accept, server)
        if not ok or conn == nil then
            java.sleep(100)
        else
            local payload = nil
            pcall(function() payload = io.read(input, 8192) end)
            if payload ~= nil and payload ~= "" then
                local request = parse_request(payload)
                local handler = find_route(request.path, request.method)
                local response
                if handler then
                    local handler_ok, result = pcall(handler, request)
                    if handler_ok then
                        response = normalize_response(result)
                    else
                        response = { status = 500, content = "Handler error: " .. tostring(result), content_type = "text/plain" }
                    end
                else
                    response = { status = 404, content = "404 Not Found: " .. request.path, content_type = "text/plain" }
                end
                pcall(io.write, output, build_response(response.status, response.content, response.content_type))
            end
            pcall(io.close, conn)
            pcall(io.close, input)
            pcall(io.close, output)
        end
        java.sleep(1)
    end
end

return httpd
