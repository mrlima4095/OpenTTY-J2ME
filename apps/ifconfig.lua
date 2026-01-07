#!/bin/lua

local ok, conn, i, o = pcall(socket.connect, "socket://opentty.xyz:31522")
if not ok then
    print(arg[0] .. ": " .. tostring(conn))
    os.exit(101)
end

local address, port = socket.device(conn)
print(address)

pcall(io.close, conn, i, o)