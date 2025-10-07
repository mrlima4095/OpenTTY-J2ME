#!/bin/lua

local function parse_url(url)
    if string.sub(url, 0, 4) == "http:" or string.sub(url, 0, 6) == "https:" then
        return url
    end
    return "http://" .. url
end
local function save(file, content)
    if file then io.write(content, file)
    else io.write(content, "nano") end
end

if arg[1] then
    local url = parse_url(arg[1])

    local body, status = socket.http.get(url)
    if status == 200 then save(arg[2], body)
    else print("wget: " .. url .. " (status: " .. status .. ")") end
else print("wget [url] [file]") end
