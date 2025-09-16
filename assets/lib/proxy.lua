local app = {
    instance = "opentty.xyz:4096"
}


function app.proxy(passwd)
    local conn, i, o = socket.connect("socket://" .. app.instance)
    
    io.read(i)
    io.write(passwd .. "\n", o)
    local response = io.read(i)
    local id = string.trim(string.sub(response, 22))

    graphics.display(graphics.Alert({
        title = "WebProxy",
        message = "Your ID: " .. id,
        button = {
            label = "Copy ID",
            root = function () print(id) end
        }
    }))

    os.setproc("id", id)

    while true do
        local cmd = io.read(i)
        cmd = string.trim(cmd)

        if cmd then
            print("WebProxy -> " .. cmd)

            if cmd == "/exit" then break end

            local before = io.read()
            os.execute(cmd)
            local after = io.read()

            io.write(string.sub(after, #before + 2, #after) .. "\n")
        end
    end

    io.close(i) io.close(o) io.close(conn)
    print("WebProxy -> disconnected")
    os.exit()
end

function app.main()
    local thr = os.execute("case thread (MIDlet) false")
    if thr == 255 then error("[ WebProxy ] Cannot run in MIDlet Thread") end

    graphics.display(graphics.BuildQuest({
        title = "WebProxy Settings",
        label = "Password",
        type = "password",
        back = { root = os.exit },
        button = { label = "Connect", root = app.proxy }
    }))
end

os.setproc("name", "sh-proxy")
app.main()

