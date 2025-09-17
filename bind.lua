local server = socket.server(4095)
print("Server listening on port 4095")
local conn, i, o = socket.accept(server)
local cli = socket.peer(conn)
print(cli .. " connected")

while true do
    local cmd = string.trim(io.read(i))

    if cmd then
        print(cli .. " -> " .. cmd)

        if tostring(cmd) == "/exit" then break end

        local before = io.read()
        os.execute(cmd)
        local after = io.read()

        io.write(string.sub(after, #before + 2, #after) .. "\n", o)
    end
end

io.close(conn)
io.close(i) io.close(o)

