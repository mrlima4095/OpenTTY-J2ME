local app = {}


function app.proxy(passwd)
    local conn, i, o = socket.connect("socket://opentty.xyz:4096")

    io.read(i)
    io.write(passwd, o)
    local response = io.read(i)
    local id = string.trim(string.sub(response, 22))

    print("WebProxy ID: " .. id)

    os.setproc("id", id)
    os.setproc("passwd", passwd)
    while true do
        local cmd = io.read(i)
        cmd = string.trim(cmd)

        if cmd then
            print("WebProxy -> " .. cmd)

            if cmd == "/exit" then break end

            local before = io.read()
            os.execute(cmd)
            local after = io.read()

            io.write(string.sub(after, #before + 2, #after) .. "\n", o)
        end
    end

    io.close(i) io.close(o) io.close(conn)
    print("WebProxy -> disconnected")
    os.exit()
end

function app.main()
    local thr = os.execute("case thread (MIDlet) false")
    if thr == 255 then error("[ WebProxy ] Cannot run in MIDlet Thread") end

    if #arg > 1 then app.proxy(arg[1])
    else print("Usage: lua " .. arg[0] .. " <password>") end
end

os.setproc("name", "sh-proxy")
app.main()

