--[[

[ Config ]

name=WebProxy
version=1.0
description=Global Bind

api.version=1.16
api.error=execute echo [ WebProxy ] OpenTTY 1.16 and Lua is required!; true
api.match=minimum
api.nodes=lua

config=execute x11 quest /home/proxy.lua

[ DISPLAY ]

quest.title=WebProxy
quest.label=Password
quest.type=password
quest.key=PASSWD
quest.cmd=execute bg lua /home/proxy.lua $PASSWD; unset PASSWD

]]

if os.execute("case thread (MIDlet) false") == 255 then
    error("WebProxy cannot run in Main Thread")
end

if #arg > 1 then
    local conn, i, o = socket.connect("socket://opentty.xyz:4096")
    local _ = io.read(i)

    io.write(arg[0] .. "\n", o)
    local id = string.trim(string.sub(io.read(i), 22))

    print("WebProxy ID: " .. id)

    os.setproc("id", id)
    os.setproc("passwd", arg[1])

    while true do
        local cmd = string.trim(io.read(i))

        if cmd then
            print("WebProxy -> " .. cmd)

            if tostring(cmd) == "/exit" then break end

            local before = io.read()
            os.execute(cmd)
            local after = io.read()

            io.write(string.sub(after, #before + 2, #after) .. "\n", o)
        end
    end

    io.close(conn)
    io.close(i) io.close(o)
else print("Usage: bg lua " .. arg[0] .. " <password>") end

