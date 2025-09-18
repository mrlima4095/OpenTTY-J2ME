local httpd = {}
httpd._routes = {}

-- split de string em "/" (usando string.sub e string.len)
local function split_path(path)
    local parts, cur, start, len = {}, "", 1, string.len(path)
    for i = 1, len do
        local ch = string.sub(path, i, i)
        if ch == "/" then
            if cur ~= "" then
                parts[#parts+1] = cur
                cur = ""
            end
        else
            cur = cur .. ch
        end
    end
    if cur ~= "" then parts[#parts+1] = cur end
    return parts
end

-- registra rota
httpd.route = function(method, path, handler)
    if handler == nil then
        handler = path
        path = method
        method = "GET"
    end
    httpd._routes[#httpd._routes+1] = {method=method, path=path, handler=handler}
end

httpd.get = function(path, handler) httpd.route("GET", path, handler) end
httpd.post = function(path, handler) httpd.route("POST", path, handler) end

-- parse request (bem simples)
local function parse_request(i)
    local line = io.read(i)
    if not line then return nil end

    -- primeira linha: METHOD /path HTTP/1.1
    local method, path = "GET", "/"
    local parts, cur, len = {}, "", string.len(line)
    for idx = 1, len do
        local ch = string.sub(line, idx, idx)
        if ch == " " then
            parts[#parts+1] = cur
            cur = ""
        else
            cur = cur .. ch
        end
    end
    if cur ~= "" then parts[#parts+1] = cur end
    if #parts >= 2 then
        method = parts[1]
        path = parts[2]
    end

    return {method=method, path=path}
end

-- matching de rota: suporta /user/:id
local function match_route(req)
    local req_parts = split_path(req.path)
    for idx=1, #httpd._routes do
        local r = httpd._routes[idx]
        if r.method == req.method then
            local route_parts = split_path(r.path)
            if #route_parts == #req_parts then
                local params, ok = {}, true
                for i=1, #route_parts do
                    local rp, qp = route_parts[i], req_parts[i]
                    if string.sub(rp,1,1) == ":" then
                        local name = string.sub(rp,2,string.len(rp))
                        params[name] = qp
                    elseif rp ~= qp then
                        ok = false
                        break
                    end
                end
                if ok then
                    req.params = params
                    return r
                end
            end
        end
    end
    return nil
end

-- envia resposta
local function send_response(o, status, body)
    local text = "HTTP/1.1 " .. status .. "\r\n" ..
                 "Content-Length: " .. string.len(body) .. "\r\n" ..
                 "Content-Type: text/plain\r\n\r\n" ..
                 body
    io.write(text, o)
end

-- roda servidor
httpd.run = function(port)
    while true do
        local ok, server = pcall(socket.server, port)
        if not ok then
            print("[-] Error")
            pcall(io.close, server)
            break
        end

        local conn, i, o = socket.accept(server)
        local req = parse_request(i)
        if req then
            local route = match_route(req)
            if route then
                local res = {
                    send = function(body) send_response(o, "200 OK", body) end,
                    json = function(tbl) 
                        -- encoder JSON mínimo (só pares simples)
                        local s = "{"
                        local first = true
                        for k,v in pairs(tbl) do
                            if not first then s = s .. "," end
                            s = s .. "\"" .. k .. "\":\"" .. v .. "\""
                            first = false
                        end
                        s = s .. "}"
                        send_response(o, "200 OK", s)
                    end
                }
                route.handler(req, res)
            else
                send_response(o, "404 Not Found", "Not Found")
            end
        end
        io.close(conn, i, o)
    end
end

return httpd
