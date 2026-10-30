#!/bin/lua

local libcore = require("libcore")
local function parse_url(url)
    if string.startswith(url, "http://") or string.startswith(url, "https://") then return url end
    return "http://" .. url
end

local function report_failure(url, err)
    print("curl: " .. tostring(err))

    if string.startswith(url, "https://") then
        print("curl: HTTPS/TLS is not supported by this J2ME runtime; use an http:// URL instead")
    end
end

os.setproc("name", "curl")

if arg[1] == "-o" then
    if arg[2] and arg[3] then
        local url = parse_url(arg[3])
        local ok, body, status = pcall(socket.http.get, url)
        if ok then
            if status ~= 200 then
                print("HTTP " .. tostring(status))
                print("---")
            end

            local code = io.write(body, os.join(arg[2]))
            if code ~= 0 then
                print("curl: " .. os.join(arg[2]) .. ": " .. libcore.errormsg(code))
            end

            os.exit(tonumber(code))
        else
            report_failure(url, body)
            os.exit(6)
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
            print("curl: " .. arg[2] .. ":" .. arg[3] .. ": " .. tostring(conn))
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
    local ok, body = pcall(socket.http.get, url)
    if ok then
        print(body)
    else
        report_failure(url, body)
        os.exit(6)
    end
else
    print("curl [url]")
end
