#!/bin/lua


local scope = { PWD = "/home/", USER = java.midlet.username, ROOT = "/", ALIAS = {} }
local password = arg[1]
local function app()
    print("conectando")
    local ok, conn, i, o = pcall(socket.connect, "socket://opentty.xyz:4096")
    if not ok then
        print(arg[0] .. ": " .. tostring(conn))
        os.exit(101)
    end

    print("pegando enderecos")
    local address, port = socket.device(conn)
    print("consumindo buffer de password")
    local _ = io.read(i)

    print("enviando password")
    io.write(password .. "\n", o)
    print("lendo o id da conexao")
    local id = string.trim(string.sub(io.read(i), 22))

    print("WebProxy ID: " .. id)

    print("config. id")
    os.setproc("id", id)
    print("config. passwd")
    os.setproc("passwd", password)
    print("config. name")
    os.setproc("name", "web-proxy")

    print("config. scooe")
    os.scope(scope)

    print("salvo stdout")
    local bkp_stdout = io.stdout
    print("setando novo stdout")
    io.setstdout(o)

    print("entrando no loop")
    pcall(function ()
        while true do
            local cmd = string.trim(io.read(i))

            if cmd then
                local _ = pcall(os.execute, cmd)
            else break end
        end
    end)

    io.setstdout(bkp_stdout)

    pcall(io.close, conn, i, o)
    print("WebProxy -> Server disconnected")
end

if password and password ~= "" then java.run(app, "WebProxy")
else print("shprxy: usage: shprxy [password]") os.exit(2) end
