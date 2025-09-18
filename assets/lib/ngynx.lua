local PORT = 8081


local server = socket.server(PORT)

pcall(function ()
    while true do
        local conn, i, o = socket.accept(server)
        io.read(i)
        io.write("reponse", o)
        
        io.close(conn, i, o)
    end
end)

io.close(server)

local socket = require("socket")

local PORT = 8081
local server = assert(socket.bind("*", PORT))
print("Servidor rodando em http://localhost:" .. PORT)

while true do
    -- Aceita conexão
    local cli, i, o = socket.accept(server)

    -- Lê request
    local request = io.read(i)
    -- Só debug pra ver request no console
    print("---- REQUEST ----\n" .. request)
    print("-----------------")

    -- Monta resposta HTTP
    local body = "<html><body><h1>Olá, Mundo!</h1><p>Servidor LuaSocket funcionando 🎉</p></body></html>"
    local response = "HTTP/1.1 200 OK\r\n" ..
                     "Content-Type: text/html; charset=UTF-8\r\n" ..
                     "Content-Length: " .. #body .. "\r\n" ..
                     "Connection: close\r\n\r\n" ..
                     body

    -- Envia resposta
    io.write(response)
    io.close(cli, i, o)
end

io.close(server)