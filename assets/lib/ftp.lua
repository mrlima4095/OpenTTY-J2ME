#!/bin/lua

--[[

[ Config ]

name=FTP
version=1.0
description=J2ME FTP Client

api.version=1.17
api.require=lua
api.error=execute echo [ FTP ] Required OpenTTY 1.17 or newer and Lua Runtime 
api.match=minimum

]]


local ftp = {}
local function trim(s) return string.trim(s or "") end

local function split(s, sep)
    local parts = {}
    local i = 1
    local j = string.match(s, sep, i)
    while j do
        table.insert(parts, string.sub(s, i, j - 1))
        i = j + string.len(sep)
        j = string.match(s, sep, i)
    end
    table.insert(parts, string.sub(s, i))
    return parts
end

function ftp.connect(host, port)
    local c, i, o = socket.connect("socket://" .. host .. ":" .. port)
    ftp.conn = c ftp.input = i ftp.output = o
    return true
end

function ftp.readline()
    local buffer = ""
    while true do
        local b = io.read(ftp.input, 1)
        if not b or b == "" then break end
        local c = string.char(string.byte(b))
        buffer = buffer .. c
        if c == "\n" then break end
    end
    return trim(buffer)
end

function ftp.send(cmd) io.write(cmd .. "\r\n", ftp.output) end

local function parse_url(url)
    local user, pass, host, port = "anonymous", "guest", "", 21

    local prefix = "ftp://"
    if string.sub(url, 1, string.len(prefix)) == prefix then
        url = string.sub(url, string.len(prefix) + 1)
    end

    local at = string.match(url, "@")
    if at then
        local creds = string.sub(url, 1, at - 1)
        url = string.sub(url, at + 1)
        local colon = string.match(creds, ":")
        if colon then
            user = string.sub(creds, 1, colon - 1)
            pass = string.sub(creds, colon + 1)
        else
            user = creds
        end
    end

    local colon = string.match(url, ":")
    if colon then
        host = string.sub(url, 1, colon - 1)
        port = tonumber(string.sub(url, colon + 1))
    else
        host = url
    end
print(user, pass, host, port)
    return user, pass, host, port
end

function ftp.login(url)
    local user, pass, host, port = parse_url(url)
    ftp.connect(host, port)
    ftp.readline()
    ftp.send("USER " .. user)
    ftp.readline()
    ftp.send("PASS " .. pass)
    return ftp.readline()
end

function ftp.list()
    ftp.send("PASV")
    local resp = ftp.readline()

    local start = string.match(resp, "(")
    local stop = string.match(resp, ")")
    if not start or not stop then return "Invalid response" end

    local data = string.sub(resp, start + 1, stop - 1)
    local parts = split(data, ",")
    if #parts < 6 then return "Invalid passiv mode" end

    local host = parts[1].."."..parts[2].."."..parts[3].."."..parts[4]
    local port = tonumber(parts[5]) * 256 + tonumber(parts[6])

    local _, dinput, _ = socket.connect("socket://" .. host .. ":" .. port)

    ftp.send("LIST")

    local buffer = ""
    while true do
        local b = io.read(dinput, 1)
        if not b or b == "" then break end
        buffer = buffer .. string.char(string.byte(b))
    end

    ftp.readline()
    return buffer
end

function ftp.quit()
    ftp.send("QUIT")
    ftp.readline()
    io.close(ftp.input)
    io.close(ftp.output)
end

return ftp
