#!/bin/lua

local function parse_url(url)
    if string.sub(url, 0, 4) == "http:" or string.sub(url, 0, 6) == "https:" then
        return url
    end
    return "http://" .. url
end

if arg[1] then
    local url = parse_url(arg[1])

    local body, status = socket.http.get(url)
    if status == 200 then print(body)
    else print("curl: " .. url .. " (status: " .. status .. ")") end
else print("curl [url]") end
