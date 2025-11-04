local httpd = {}
httpd.__routes = {}

-- Funções auxiliares para parsing HTTP (sem regex e sem gsub)
local function getMethod(payload)
    if not payload or type(payload) ~= "string" then return nil end
    local space_pos = string.find(payload, " ")
    if not space_pos then return nil end
    return string.sub(payload, 1, space_pos - 1)
end

local function getPath(payload)
    if not payload or type(payload) ~= "string" then return nil end
    local first_space = string.find(payload, " ")
    if not first_space then return nil end
    local second_space = string.find(payload, " ", first_space + 1)
    if not second_space then return nil end
    
    local full_path = string.sub(payload, first_space + 1, second_space - 1)
    
    -- Remove query parameters se existirem
    local query_pos = string.find(full_path, "?")
    if query_pos then
        return string.sub(full_path, 1, query_pos - 1)
    end
    return full_path
end

local function trimString(str)
    if not str then return "" end
    -- Remove espaços do início
    while string.len(str) > 0 and string.byte(str, 1) == 32 do
        str = string.sub(str, 2)
    end
    -- Remove espaços do final
    while string.len(str) > 0 and string.byte(str, -1) == 32 do
        str = string.sub(str, 1, -2)
    end
    return str
end

local function getHeaders(payload)
    local headers = {}
    if not payload or type(payload) ~= "string" then return headers end
    
    -- Encontra fim dos headers
    local header_end = string.find(payload, "\r\n\r\n")
    if not header_end then return headers end
    
    local header_block = string.sub(payload, 1, header_end - 1)
    
    -- Processa cada linha manualmente
    local lines = {}
    local start_idx = 1
    local len = string.len(header_block)
    
    while start_idx <= len do
        local end_idx = string.find(header_block, "\r\n", start_idx)
        if not end_idx then
            table.insert(lines, string.sub(header_block, start_idx))
            break
        end
        
        local line = string.sub(header_block, start_idx, end_idx - 1)
        table.insert(lines, line)
        start_idx = end_idx + 2
    end
    
    -- Pula a primeira linha (request line) e processa headers
    for i = 2, #lines do
        local line = lines[i]
        local colon_pos = string.find(line, ":")
        if colon_pos then
            local name = string.sub(line, 1, colon_pos - 1)
            local value = string.sub(line, colon_pos + 1)
            
            name = trimString(name)
            value = trimString(value)
            
            headers[name] = value
        end
    end
    
    return headers
end

local function getBody(payload)
    if not payload or type(payload) ~= "string" then return "" end
    
    local header_end = string.find(payload, "\r\n\r\n")
    if not header_end then return "" end
    
    return string.sub(payload, header_end + 4)
end

-- Função simples para decodificar %20
local function simpleUrlDecode(str)
    if not str then return "" end
    local result = ""
    local i = 1
    local len = string.len(str)
    
    while i <= len do
        local char = string.sub(str, i, i)
        if char == "%" and i + 2 <= len then
            local hex = string.sub(str, i + 1, i + 2)
            if hex == "20" then
                result = result .. " "
                i = i + 3
            else
                result = result .. char
                i = i + 1
            end
        else
            result = result .. char
            i = i + 1
        end
    end
    return result
end

local function getQuery(payload)
    local query = {}
    if not payload or type(payload) ~= "string" then return query end
    
    local first_space = string.find(payload, " ")
    if not first_space then return query end
    local second_space = string.find(payload, " ", first_space + 1)
    if not second_space then return query end
    
    local full_path = string.sub(payload, first_space + 1, second_space - 1)
    local query_pos = string.find(full_path, "?")
    
    if not query_pos then return query end
    
    local query_string = string.sub(full_path, query_pos + 1)
    
    -- Processa parâmetros de query manualmente
    local start_idx = 1
    local len = string.len(query_string)
    
    while start_idx <= len do
        local amp_pos = string.find(query_string, "&", start_idx)
        local end_pos = amp_pos or (len + 1)
        
        local pair = string.sub(query_string, start_idx, end_pos - 1)
        local eq_pos = string.find(pair, "=")
        
        if eq_pos then
            local key = string.sub(pair, 1, eq_pos - 1)
            local value = string.sub(pair, eq_pos + 1)
            -- URL decode básico sem gsub
            key = simpleUrlDecode(key)
            value = simpleUrlDecode(value)
            query[key] = value
        else
            query[pair] = ""
        end
        
        start_idx = end_pos + 1
    end
    
    return query
end

-- Função para criar resposta HTTP
local function createResponse(status_code, content, content_type)
    local status_messages = {
        [200] = "OK",
        [201] = "Created",
        [400] = "Bad Request",
        [404] = "Not Found",
        [500] = "Internal Server Error"
    }
    
    content_type = content_type or "text/plain"
    local status_msg = status_messages[status_code] or "Unknown"
    
    local response = "HTTP/1.1 " .. status_code .. " " .. status_msg .. "\r\n"
    response = response .. "Content-Type: " .. content_type .. "\r\n"
    response = response .. "Content-Length: " .. string.len(content) .. "\r\n"
    response = response .. "Connection: close\r\n"
    response = response .. "\r\n"
    response = response .. content
    
    return response
end

-- Função para encontrar rota correspondente
local function findRoute(path, method)
    for route_pattern, route_data in pairs(httpd.__routes) do
        if route_pattern == path then
            if route_data.method == method or route_data.method == "ANY" then
                return route_data.handler
            end
        end
    end
    return nil
end

-- API principal
function httpd.route(endpoint, method, handler)
    if not method or method == "" then
        method = "GET"
    end
    method = string.upper(method)
    httpd.__routes[endpoint] = { method = method, handler = handler }
end

function httpd.run(port)
    if not port then
        port = 8080
    end
    
    print("HTTP Server starting on port " .. port)
    
    while true do
        local server, input, output, client_conn
        local ok, err = pcall(function() 
            server = socket.server(port)
        end)
        
        if not ok then
            print("Server error: " .. tostring(err))
            if server then pcall(io.close, server) end
            os.execute("sleep 1") -- Espera antes de tentar novamente
        else
            -- Aguarda conexão
            local accept_ok, accept_err = pcall(function()
                client_conn, input, output = socket.accept(server)
            end)
            
            if accept_ok and client_conn then
                -- Lê dados da requisição
                local payload = nil
                local read_ok, read_err = pcall(function()
                    payload = io.read(input, 4096) -- Lê até 4KB
                end)
                
                if read_ok and payload then
                    -- Processa a requisição
                    local method = getMethod(payload)
                    local path = getPath(payload)
                    local headers = getHeaders(payload)
                    local body = getBody(payload)
                    local query = getQuery(payload)
                    
                    print("Request: " .. tostring(method) .. " " .. tostring(path))
                    
                    -- Prepara objeto de requisição
                    local request = {
                        method = method,
                        path = path,
                        headers = headers,
                        body = body,
                        query = query,
                        payload = payload
                    }
                    
                    -- Encontra handler
                    local handler = findRoute(path, method)
                    local response_content = ""
                    local status_code = 404
                    local content_type = "text/plain"
                    
                    if handler then
                        local handler_ok, handler_result = pcall(handler, request)
                        if handler_ok then
                            if type(handler_result) == "string" then
                                response_content = handler_result
                                status_code = 200
                                content_type = "text/html"
                            elseif type(handler_result) == "table" then
                                response_content = handler_result.content or ""
                                status_code = handler_result.status or 200
                                content_type = handler_result.content_type or "text/html"
                            else
                                response_content = "Invalid handler return type"
                                status_code = 500
                            end
                        else
                            response_content = "Handler error: " .. tostring(handler_result)
                            status_code = 500
                        end
                    else
                        response_content = "404 - Route not found: " .. tostring(path)
                        status_code = 404
                    end
                    
                    -- Envia resposta
                    local response = createResponse(status_code, response_content, content_type)
                    local write_ok, write_err = pcall(function()
                        io.write(output, response)
                    end)
                    
                    if not write_ok then
                        print("Write error: " .. tostring(write_err))
                    end
                end
                
                -- Fecha conexões
                pcall(io.close, client_conn)
                pcall(io.close, input)
                pcall(io.close, output)
            end
            
            pcall(io.close, server)
        end
        
        -- Pequena pausa para evitar uso excessivo de CPU
        pcall(function()
            os.execute("sleep 1")
        end)
    end
end

-- Atalhos para métodos comuns
function httpd.get(endpoint, handler)
    return httpd.route(endpoint, "GET", handler)
end

function httpd.post(endpoint, handler)
    return httpd.route(endpoint, "POST", handler)
end

function httpd.put(endpoint, handler)
    return httpd.route(endpoint, "PUT", handler)
end

function httpd.delete(endpoint, handler)
    return httpd.route(endpoint, "DELETE", handler)
end

return httpd