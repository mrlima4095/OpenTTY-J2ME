#!/bin/lua

local libcore = require("libcore")
local function parse_url(url)
    if string.sub(url, 1, 4) == "http:" or string.sub(url, 0, 6) == "https:" then return url end
    return "http://" .. url
end

os.setproc("name", "curl")

if arg[1] == "-o" then
    if arg[2] and arg[3] then
        local url = parse_url(arg[3])
        local ok, body, status = pcall(socket.http.get, url)
        if ok then
            if status ~= 200 then
                print("HTTP " .. status)
                print("---")
            end

            local code = io.write(body, os.join(arg[2]))
            if code ~= 0 then
                print("curl: " .. libcore.errormsg(code))
            end

            os.exit(tonumber(code))
        else
            print("curl: (6) Could not resolve host: " .. arg[3])
        end
    else
        print("Usage: curl -o [file] [url]")
    end
elseif arg[1] == "-t" then
    if arg[2] and arg[3] then
        local ok, conn, i, o = pcall(socket.connect, "socket://" .. arg[2] .. ":" .. arg[3])
        if ok then
            if arg[4] then
                pcall(io.write, arg[4], o)
            end

            local _, msg = pcall(io.read, i, 2048)
            print(msg)
        else
            print("curl: " .. tostring(conn))
        end
    else
        print("Usage: curl -t [host] [port] [payload]")
    end
elseif arg[1] == "-s" then
    if arg[2] and arg[3] then
        print(pcall(os.request, arg[2], arg[3], arg[4]))
    else
        print("Usage: curl -s [pid] [request] [args]")
    end
elseif arg[1] then
    local url = parse_url(arg[1])
    local ok, body, status = pcall(socket.http.get, url)
    if ok then
        print(body)
    else
        print("curl: (6) Could not resolve host: " .. arg[1])
    end
else
    print("curl [url]")
end
