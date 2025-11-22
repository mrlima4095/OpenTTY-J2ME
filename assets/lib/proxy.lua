#!/bin/lua

if tonumber(os.execute("case thread (MIDlet) false")) == 255 then error("WebProxy cannot run in Main Thread") end
if #arg > 1 then
    local conn, i, o = socket.connect("socket://opentty.xyz:4096")
    local address, port = socket.device(conn)
    local _ = io.read(i)

    io.write(arg[1] .. "\n", o)
    local id = string.trim(string.sub(io.read(i), 22))

    print("WebProxy ID: " .. id)

    os.setproc("id", id)
    os.setproc("passwd", arg[1])
    os.setproc("name", "web-proxy")

    if java.midlet and port then java.midlet.sessions[tostring(port)] = "opentty.xyz" end

    pcall(function ()
        while true do
            local cmd = string.trim(io.read(i))

            if cmd then
                print("WebProxy -> [" .. cmd .. "]")

                local status, output = io.popen(cmd)
                io.write(string.trim(output) .. "\n", o)
            else break end
        end
    end)

    pcall(io.close, conn, i, o)
    print("WebProxy -> Server disconnected")
    if java.midlet and port then java.delete(java.midlet.sessions, tostring(port)) end
else print("Usage: bg lua " .. arg[0] .. " <password>") end

