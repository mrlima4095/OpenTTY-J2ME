local httpd = {}

httpd.__routes = {}
httpd.__default_headers = {
    ["Content-Type"] = "text/html; charset=UTF-8",
    ["Server"] = "LuaJ2ME/1.0"
}

-- Função para dividir string sem regex
local function split_string(str, sep)
    if str == nil then return {} end
    local result = {}
    local i = 1
    
    while i <= string.len(str) do
        local found = false
        for j = i, string.len(str) do
            if string.sub(str, j, j + string.len(sep) - 1) == sep then
                table.insert(result, string.sub(str, i, j - 1))
                i = j + string.len(sep)
                found = true
                break
            end
        end
        if not found then
            table.insert(result, string.sub(str, i))
            break
        end
    end
    
    return result
end

-- Trim sem regex
local function trim_string(str)
    if str == nil then return "" end
    
    -- Remove espaços do início
    local start = 1
    while start <= string.len(str) and string.sub(str, start, start) == " " do
        start = start + 1
    end
    
    -- Remove espaços do final
    local finish = string.len(str)
    while finish >= 1 and string.sub(str, finish, finish) == " " do
        finish = finish - 1
    end
    
    if start > finish then return "" end
    
    return string.sub(str, start, finish)
end

local function getMethod(payload)
    if not payload or type(payload) ~= "string" then
        return "GET"
    end
    
    local first_line_end = string.find(payload, "\n") or (string.len(payload) + 1)
    local first_line = string.sub(payload, 1, first_line_end - 1)
    local first_space = string.find(first_line, " ") or (string.len(first_line) + 1)
    
    local method = string.sub(first_line, 1, first_space - 1)
    return string.upper(method) or "GET"
end

local function getPath(payload)
    if not payload or type(payload) ~= "string" then
        return "/"
    end
    
    local first_line_end = string.find(payload, "\n") or (string.len(payload) + 1)
    local first_line = string.sub(payload, 1, first_line_end - 1)
    
    local first_space = string.find(first_line, " ") or 1
    local second_space = string.find(first_line, " ", first_space + 1) or (string.len(first_line) + 1)
    
    local path = string.sub(first_line, first_space + 1, second_space - 1)
    
    -- Remove query string
    local query_start = string.find(path, "?")
    if query_start then
        path = string.sub(path, 1, query_start - 1)
    end
    
    return path or "/"
end

local function getHeaders(payload)
    local headers = {}
    
    if not payload or type(payload) ~= "string" then
        return headers
    end
    
    local header_end = string.find(payload, "\n\n") or string.find(payload, "\r\n\r\n")
    if not header_end then return headers end
    
    local header_block = string.sub(payload, 1, header_end - 1)
    local lines = split_string(header_block, "\n")
    
    -- Skip first line (request line)
    for i = 2, #lines do
        local line = trim_string(lines[i])
        if line == "" then break end
        
        local colon_pos = string.find(line, ":")
        if colon_pos then
            local key = trim_string(string.sub(line, 1, colon_pos - 1))
            local value = trim_string(string.sub(line, colon_pos + 1))
            headers[string.lower(key)] = value
        end
    end
    
    return headers
end

local function getBody(payload)
    if not payload or type(payload) ~= "string" then
        return ""
    end
    
    local header_end = string.find(payload, "\n\n")
    if header_end then
        return string.sub(payload, header_end + 2)
    end
    
    header_end = string.find(payload, "\r\n\r\n")
    if header_end then
        return string.sub(payload, header_end + 4)
    end
    
    return ""
end

local function getQuery(payload)
    local query = {}
    
    if not payload or type(payload) ~= "string" then
        return query
    end
    
    local first_line_end = string.find(payload, "\n") or (string.len(payload) + 1)
    local first_line = string.sub(payload, 1, first_line_end - 1)
    
    local first_space = string.find(first_line, " ") or 1
    local second_space = string.find(first_line, " ", first_space + 1) or (string.len(first_line) + 1)
    
    local path = string.sub(first_line, first_space + 1, second_space - 1)
    local query_start = string.find(path, "?")
    
    if query_start then
        local query_string = string.sub(path, query_start + 1)
        local pairs = split_string(query_string, "&")
        
        for _, pair in ipairs(pairs) do
            local equals_pos = string.find(pair, "=")
            if equals_pos then
                local key = trim_string(string.sub(pair, 1, equals_pos - 1))
                local value = trim_string(string.sub(pair, equals_pos + 1))
                query[key] = value
            else
                local key = trim_string(pair)
                query[key] = ""
            end
        end
    end
    
    return query
end

local function parseCookies(headers)
    local cookies = {}
    local cookie_header = headers["cookie"]
    
    if cookie_header then
        local cookie_pairs = split_string(cookie_header, ";")
        for _, cookie in ipairs(cookie_pairs) do
            local equals_pos = string.find(cookie, "=")
            if equals_pos then
                local key = trim_string(string.sub(cookie, 1, equals_pos - 1))
                local value = trim_string(string.sub(cookie, equals_pos + 1))
                cookies[key] = value
            else
                local key = trim_string(cookie)
                cookies[key] = ""
            end
        end
    end
    
    return cookies
end

function httpd.generate(body, headers, status_code)
    local status_text = "200 OK"
    if status_code == 404 then status_text = "404 Not Found"
    elseif status_code == 500 then status_text = "500 Internal Server Error"
    elseif status_code == 400 then status_text = "400 Bad Request"
    elseif status_code == 405 then status_text = "405 Method Not Allowed"
    end
    
    local response_headers = {}
    
    -- Headers padrão
    for k, v in pairs(httpd.__default_headers) do
        response_headers[k] = v
    end
    
    -- Headers customizados
    if headers and type(headers) == "table" then
        for k, v in pairs(headers) do
            response_headers[k] = v
        end
    end
    
    -- Content-Length
    local content = body or ""
    response_headers["Content-Length"] = tostring(string.len(content))
    
    -- Construir response
    local response = "HTTP/1.1 " .. status_text .. "\r\n"
    
    for k, v in pairs(response_headers) do
        response = response .. k .. ": " .. v .. "\r\n"
    end
    
    response = response .. "\r\n" .. content
    
    return response
end

function httpd.handler(payload)
    local method = getMethod(payload)
    local path = getPath(payload)
    local headers = getHeaders(payload)
    local body = getBody(payload)
    local query = getQuery(payload)
    local cookies = parseCookies(headers)
    
    local request = {
        method = method,
        path = path,
        headers = headers,
        body = body,
        query = query,
        cookies = cookies
    }
    
    -- Procurar rota exata
    local route = httpd.__routes[path]
    
    if route then
        if route.method ~= method then
            return httpd.generate(
                "<h1>405 Method Not Allowed</h1>",
                { ["Content-Type"] = "text/html" },
                405
            )
        end
        
        local ok, result = pcall(route.handler, request)
        if ok then
            if type(result) == "string" then
                return httpd.generate(result)
            elseif type(result) == "table" then
                return httpd.generate(result.body or "", result.headers or {}, result.status or 200)
            else
                return httpd.generate(tostring(result))
            end
        else
            return httpd.generate(
                "<h1>500 Internal Server Error</h1><pre>" .. tostring(result) .. "</pre>",
                { ["Content-Type"] = "text/html" },
                500
            )
        end
    else
        for route_path, route_data in pairs(httpd.__routes) do
            if string.sub(route_path, -1) == "*" then
                local base_path = string.sub(route_path, 1, -2)
                if string.sub(path, 1, string.len(base_path)) == base_path then
                    if route_data.method ~= method then
                        return httpd.generate(
                            "<h1>405 Method Not Allowed</h1>",
                            { ["Content-Type"] = "text/html" },
                            405
                        )
                    end
                    
                    local ok, result = pcall(route_data.handler, request)
                    if ok then
                        if type(result) == "string" then
                            return httpd.generate(result)
                        elseif type(result) == "table" then
                            return httpd.generate(result.body or "", result.headers or {}, result.status or 200)
                        else
                            return httpd.generate(tostring(result))
                        end
                    else
                        return httpd.generate(
                            "<h1>500 Internal Server Error</h1><pre>" .. tostring(result) .. "</pre>",
                            { ["Content-Type"] = "text/html" },
                            500
                        )
                    end
                end
            end
        end

        return httpd.generate("<h1>404 Not Found</h1><p>The requested URL " .. path .. " was not found on this server.</p>", { ["Content-Type"] = "text/html" }, 404)
    end
end

function httpd.route(path, method, handler)
    if not path then error("bad argument #1 to 'route' (string expected, got no value)") end
    if type(path) ~= "string" then error("bad argument #1 to 'route' (string expected, got " .. type(path) .. ")") end

    if not handler then handler = method method = "GET" end
    if type(handler) ~= "function" then error("bad argument #" .. (method == "GET" and 2 or 3) .. " to 'route' (function expected, got " .. type(handler) .. ")") end

    httpd.__routes[path] = { method = string.upper(method), handler = handler }
end

function httpd.get(path, handler) return httpd.route(path, "GET", handler) end
function httpd.post(path, handler) return httpd.route(path, "POST", handler) end
function httpd.put(path, handler) return httpd.route(path, "PUT", handler) end
function httpd.delete(path, handler) return httpd.route(path, "DELETE", handler) end

function httpd.run(port)
    if not port then 
        error("bad argument #1 to 'run' (number expected, got no value)") 
    end
    
    if type(port) ~= "number" then
        error("bad argument #1 to 'run' (number expected, got " .. type(port) .. ")")
    end
    
    while true do
        local server, conn, instream, outstream, payload

        local ok, err = pcall(function() server = socket.server(port) end)
        
        if not ok then 
            if server then pcall(socket.close, server) end
            error("Failed to start server: " .. tostring(err)) break
        end

        ok, err = pcall(function() conn, instream, outstream = socket.accept(server) end)
        
        if not ok then pcall(io.close, server, conn, instream, outstream) break end

        ok, err = pcall(function() payload = io.read(instream, 8192) end)
        
        if not ok then pcall(io.close, server, conn, instream, outstream) break end

        if payload and string.len(payload) > 0 then
            local response_ok, response = pcall(httpd.handler, payload)
            
            local response_data = response
            if not response_ok then
                response_data = httpd.generate(
                    "<h1>500 Internal Server Error</h1><pre>" .. tostring(response) .. "</pre>",
                    { ["Content-Type"] = "text/html" },
                    500
                )
            end
            
            pcall(io.write, outstream, response_data)
            pcall(io.close, conn, instream, outstream)
        else
            pcall(io.close, conn, instream, outstream)
        end
        
    end
end


return httpd