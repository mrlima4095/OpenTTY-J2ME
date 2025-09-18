local httpd = {
    routes = {}
}

function httpd.route(path, handler)
    if type(handler) == "function" then httpd.routes[path] = handler
    else error("bad argument #2 'route' (function expected, got " .. type(handler) .. ")") end
end

-- Função para extrair método e caminho da primeira linha da requisição
function httpd.parse_request_line(request_text)
    -- A primeira linha termina em \r\n ou \n
    local eol = string.find(request_text, "\r\n")
    if not eol then eol = string.find(request_text, "\n") end
    if not eol then return nil, nil end

    local line = string.sub(request_text, 1, eol - 1)
    -- Procurar espaço para separar método e caminho
    local first_space = string.find(line, " ")
    if not first_space then return nil, nil end
    local second_space = string.find(line, " ", first_space + 1)
    if not second_space then return nil, nil end

    local method = string.sub(line, 1, first_space - 1)
    local path = string.sub(line, first_space + 1, second_space - 1)

    return method, path
end

-- Função para extrair headers e corpo da requisição
function httpd.parse_headers_and_body(request_text)
    -- Procurar a sequência \r\n\r\n ou \n\n que separa headers do corpo
    local header_end = string.find(request_text, "\r\n\r\n")
    local offset = 4
    if not header_end then
        header_end = string.find(request_text, "\n\n")
        offset = 2
    end

    if not header_end then
        -- Sem corpo, tudo é header
        return request_text, ""
    end

    local headers_text = string.sub(request_text, 1, header_end - 1)
    local body = string.sub(request_text, header_end + offset)

    return headers_text, body
end

-- Função para parsear headers em tabela
function httpd.parse_headers(headers_text)
    local headers = {}
    local pos = 1
    while true do
        local eol = string.find(headers_text, "\r\n", pos)
        if not eol then eol = string.find(headers_text, "\n", pos) end
        if not eol then break end

        local line = string.sub(headers_text, pos, eol - 1)
        pos = eol + 2
        if string.sub(line, -1) == "\r" then
            line = string.sub(line, 1, -2)
        end

        -- Separar chave e valor pelo primeiro ':'
        local colon_pos = string.find(line, ":")
        if colon_pos then
            local key = string.sub(line, 1, colon_pos - 1)
            local value = string.sub(line, colon_pos + 1)
            -- Trim espaços
            key = httpd.trim(key):lower()
            value = httpd.trim(value)
            headers[key] = value
        end
    end
    return headers
end

-- Função para trim espaços
function httpd.trim(s)
    local from = 1
    local to = #s
    while from <= to and (string.sub(s, from, from) == " " or string.sub(s, from, from) == "\t") do
        from = from + 1
    end
    while to >= from and (string.sub(s, to, to) == " " or string.sub(s, to, to) == "\t") do
        to = to - 1
    end
    if from > to then return "" end
    return string.sub(s, from, to)
end

function httpd.run(port)
    while true do
        local sucess, server = pcall(socket.server, port)
        if not sucess then error("Failed to create server socket") break end

        local client, inStream, outStream = socket.accept(server)
        if client then
            local request = io.read(inStream)
            if request then
                local method, path = httpd.parse_request_line(request)
                local headers_text, body = httpd.parse_headers_and_body(request)
                local headers = httpd.parse_headers(headers_text)

                local handler = httpd.routes[path]
                local response_body = ""
                local response_code = 200
                local response_headers = {
                    ["Content-Type"] = "text/plain"
                }

                if handler then
                    local ok, res = pcall(handler, method, headers, body)
                    if ok then response_body = res or ""
                    else
                        response_code = 500
                        response_body = "Internal Server Error: " .. tostring(res)
                    end
                else
                    response_code = 404
                    response_body = "Not Found"
                end

                local status_text = "OK"
                if response_code == 404 then status_text = "Not Found" end
                if response_code == 500 then status_text = "Internal Server Error" end

                local status_line = "HTTP/1.1 " .. response_code .. " " .. status_text .. "\r\n"
                local header_lines = ""
                for k,v in pairs(response_headers) do
                    header_lines = header_lines .. k .. ": " .. v .. "\r\n"
                end
                local full_response = status_line .. header_lines .. "\r\n" .. response_body

                io.write(full_response, outStream)
            end

            io.close(client, inStream, outStream, server)
        end
    end
end


return httpd