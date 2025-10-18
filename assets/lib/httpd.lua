-- Flask-like HTTP server para Lua J2ME
httpd = {}

-- Tabela para armazenar as rotas
httpd.routes = {}

-- Função para adicionar rotas
function httpd.route(path, method, handler)
    if not httpd.routes[path] then
        httpd.routes[path] = {}
    end
    httpd.routes[path][method:upper()] = handler
end

-- Função auxiliar para dividir string por delimitador
function httpd.split_string(str, delimiter)
    local result = {}
    local start = 1
    local delim_start, delim_end = string.find(str, delimiter, start)
    
    while delim_start do
        table.insert(result, string.sub(str, start, delim_start - 1))
        start = delim_end + 1
        delim_start, delim_end = string.find(str, delimiter, start)
    end
    
    table.insert(result, string.sub(str, start))
    return result
end

-- Função auxiliar para trim de espaços
function httpd.trim(str)
    if not str then return "" end
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
    
    if finish < start then return "" end
    return string.sub(str, start, finish)
end

-- Função para comparar paths e extrair parâmetros
function httpd.match_path(route_path, request_path)
    local route_parts = httpd.split_string(route_path, "/")
    local request_parts = httpd.split_string(request_path, "/")
    
    -- Remove partes vazias do início (se houver)
    if route_parts[1] == "" then table.remove(route_parts, 1) end
    if request_parts[1] == "" then table.remove(request_parts, 1) end
    
    -- Verifica se têm o mesmo número de partes
    if #route_parts ~= #request_parts then
        return nil
    end
    
    local params = {}
    
    -- Compara parte por parte
    for i = 1, #route_parts do
        local route_part = route_parts[i]
        local request_part = request_parts[i]
        
        -- Se a parte da rota começa com ':', é um parâmetro
        if string.sub(route_part, 1, 1) == ":" then
            local param_name = string.sub(route_part, 2)
            params[param_name] = request_part
        else
            -- Se não é parâmetro, as partes devem ser iguais
            if route_part ~= request_part then
                return nil
            end
        end
    end
    
    return params
end

-- Função para encontrar rota correspondente
function httpd.find_route(request_path, method)
    -- Remove query parameters do path se houver
    local clean_path = request_path
    local query_start = string.find(request_path, "?")
    if query_start then
        clean_path = string.sub(request_path, 1, query_start - 1)
    end
    
    -- Verifica rota exata primeiro
    if httpd.routes[clean_path] and httpd.routes[clean_path][method] then
        return httpd.routes[clean_path][method], {}
    end
    
    -- Procura por rotas com parâmetros
    for route_path, methods in pairs(httpd.routes) do
        if methods[method] then
            local params = httpd.match_path(route_path, clean_path)
            if params then
                return methods[method], params
            end
        end
    end
    
    return nil
end

-- Função para parsear query parameters
function httpd.parse_query(query_string)
    local params = {}
    if not query_string or query_string == "" then return params end
    
    local pairs = httpd.split_string(query_string, "&")
    for _, pair in ipairs(pairs) do
        local eq_pos = string.find(pair, "=")
        if eq_pos then
            local key = string.sub(pair, 1, eq_pos - 1)
            local value = string.sub(pair, eq_pos + 1)
            params[key] = value
        else
            params[pair] = ""
        end
    end
    
    return params
end

-- Função para parsear requisição HTTP
function httpd.parse_request(request_data)
    local request = {
        method = "GET",
        path = "/",
        headers = {},
        body = "",
        query_params = {}
    }
    
    -- Encontra fim dos headers procurando por sequência vazia
    local header_end = nil
    local data_len = string.len(request_data)
    
    -- Procura por \r\n\r\n
    for i = 1, data_len - 3 do
        if string.sub(request_data, i, i + 3) == "\r\n\r\n" then
            header_end = i + 3
            break
        end
    end
    
    -- Se não encontrou, procura por \n\n
    if not header_end then
        for i = 1, data_len - 1 do
            if string.sub(request_data, i, i + 1) == "\n\n" then
                header_end = i + 1
                break
            end
        end
    end
    
    local headers_part = ""
    local body_part = ""
    
    if header_end then
        headers_part = string.sub(request_data, 1, header_end)
        body_part = string.sub(request_data, header_end + 1)
    else
        headers_part = request_data
    end
    
    -- Divide headers em linhas
    local header_lines = httpd.split_string(headers_part, "\r\n")
    if #header_lines == 1 then
        -- Tenta com \n se \r\n não funcionou
        header_lines = httpd.split_string(headers_part, "\n")
    end
    
    -- Parse primeira linha (request line)
    if #header_lines > 0 then
        local first_line_parts = httpd.split_string(header_lines[1], " ")
        if #first_line_parts >= 2 then
            request.method = first_line_parts[1]
            local full_path = first_line_parts[2]
            
            -- Separa path e query parameters
            local query_start = string.find(full_path, "?")
            if query_start then
                request.path = string.sub(full_path, 1, query_start - 1)
                local query_string = string.sub(full_path, query_start + 1)
                request.query_params = httpd.parse_query(query_string)
            else
                request.path = full_path
            end
        end
    end
    
    -- Parse headers restantes
    for i = 2, #header_lines do
        local line = header_lines[i]
        if line and line ~= "" then
            local colon_pos = string.find(line, ":")
            if colon_pos then
                local key = httpd.trim(string.sub(line, 1, colon_pos - 1))
                local value = httpd.trim(string.sub(line, colon_pos + 1))
                if key ~= "" then
                    request.headers[string.lower(key)] = value
                end
            end
        end
    end
    
    request.body = body_part
    return request
end

-- Função para criar resposta HTTP
function httpd.create_response(status, body, content_type)
    content_type = content_type or "text/plain"
    local body_str = tostring(body)
    
    local response_lines = {
        "HTTP/1.1 " .. tostring(status) .. " " .. httpd.status_message(status),
        "Content-Type: " .. content_type,
        "Content-Length: " .. string.len(body_str),
        "Connection: close",
        "",
        body_str
    }
    
    return table.concat(response_lines, "\r\n")
end

-- Mensagens de status HTTP
function httpd.status_message(status_code)
    local messages = {
        [200] = "OK",
        [201] = "Created",
        [204] = "No Content",
        [400] = "Bad Request",
        [404] = "Not Found",
        [405] = "Method Not Allowed",
        [500] = "Internal Server Error"
    }
    
    return messages[status_code] or "Unknown"
end

-- Função principal do servidor
function httpd.run(port)
    port = port or 8080
    
    print("Starting HTTP server on port " .. port)
    
    -- Cria servidor
    local server = socket.server(port)
    if not server then
        error("Failed to start server on port " .. port)
    end
    
    while true do
        -- Aceita conexão
        local client_data = socket.accept(server)
        if not client_data then
            break
        end
        
        local client_conn = client_data[1]
        local input_stream = client_data[2]
        local output_stream = client_data[3]
        
        -- Lê dados da requisição
        local request_data = ""
        local chunk = io.read(input_stream, 1024)
        while chunk and chunk ~= "" do
            request_data = request_data .. chunk
            if string.len(request_data) > 8192 then
                break
            end
            chunk = io.read(input_stream, 1024)
        end
        
        if request_data ~= "" then
            -- Parse e processa requisição
            local success, result = pcall(function()
                local request = httpd.parse_request(request_data)
                local handler, params = httpd.find_route(request.path, request.method)
                
                if handler then
                    -- Prepara objeto request para o handler
                    local req_obj = {
                        method = request.method,
                        path = request.path,
                        headers = request.headers,
                        body = request.body,
                        query = request.query_params,
                        params = params or {}
                    }
                    
                    -- Executa handler
                    local handler_result = handler(req_obj)
                    
                    -- Processa resultado
                    local status, body, content_type
                    
                    if type(handler_result) == "table" then
                        status = handler_result[1] or 200
                        body = handler_result[2] or ""
                        content_type = handler_result[3] or "text/plain"
                    else
                        status = 200
                        body = tostring(handler_result or "")
                        content_type = "text/plain"
                    end
                    
                    return httpd.create_response(status, body, content_type)
                else
                    -- Rota não encontrada
                    return httpd.create_response(404, "404 Not Found: " .. request.path)
                end
            end)
            
            -- Envia resposta
            local response_data = success and result or 
                httpd.create_response(500, "Internal Server Error: " .. tostring(result))
            
            io.write(output_stream, response_data)
            output_stream:flush()
        end
        
        -- Fecha conexão
        io.close(client_conn)
        io.close(input_stream)
        io.close(output_stream)
    end
    
    io.close(server)
end

local httpd=require("/home/httpd") httpd.route("/", "GET", function() return 200, "<h1>Hello, World!</h1><p></p>" end) print("rodando na porta 21") httpd.run(21)
