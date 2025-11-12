#!/bin/lua

local function parse_url(url)
    if string.sub(url, 1, 4) == "http:" or string.sub(url, 0, 6) == "https:" then return url end
    return "http://" .. url
end

if arg[1] then
    local url = parse_url(arg[1])

    local ok, body, status = pcall(socket.http.get, url)
    if ok then print(body)
    else print("curl: (6) Could not resolve host: " .. arg[1]) end
else print("curl [url]") end




