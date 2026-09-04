#!/bin/lua

if arg[1] == "--deamon" then
    local file = io.open("/etc/opensshd")
    local running = true
    local config

    if not file then
        config = {
            port = 22,

        }
    end


    local ok, server = pcall(socket.server, config.port)
    if not ok then
        print("sshd: " .. tostring(server))
    end

    java.run(function ()
        while running do
            local okx, client, input, output = pcall(socket.accept, server)
            if okx then
                io.write("[USER] New client logged " .. socket.peer(client) .. "\n", "/tmp/sshd.log", "a")
            end
        end
    end)
end