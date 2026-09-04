#!/bin/lua

os.setproc("name", "netstat")

local url = arg[1] or "http://ipinfo.io/ip"

local ok, body, status = pcall(socket.http.get, url)
if ok then
    if status == 200 then
        print("true")
        os.exit(0)
    else
        print("false")
        os.exit(101)
    end
else
    print("false")
    os.exit(101)
end