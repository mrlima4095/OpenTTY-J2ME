local count = 1
local authed = false
local password = "123"
local port = tonumber(os.getenv("PORT") or 22)

while true do
    local server = socket.server(port)
    if count == 1 then print("[+] listening at port " .. port) count = count + 1 os.putproc(1, "server", server) end

    local client, i, o = socket.accept(server)
    local addr, _ = socket.peer(client)
    print("[+] " .. addr .. " connected")
    io.write("Password: ", o)

    while true do
        local payload = string.trim(io.read(i))

        if not authed then
            if payload == password then
                authed = true
            else
                print("[-] " .. addr .. " -- auth failed")
                io.write("Bad credentials!", o) break
            end
        elseif payload == "/exit" then break
        elseif payload == "/addr" then io.write(addr, o)
        elseif payload == "/close" then io.close(server, client, i, o)
        else
            print("[+] " .. addr .. " -> " .. payload)
            local before = io.read()
            os.execute(payload)
            local after = io.read()

            io.write(string.sub(after, #before + 2, #after), o)
        end
    end
    print("[-] " .. addr .. " disconnected")
    io.close(server, client, i, o)
    authed = false
end