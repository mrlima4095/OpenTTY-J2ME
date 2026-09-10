#!/bin/lua

os.setproc("name", "vnt")

local input = arg[1]
if not input or input == "" then
    print("vnt: usage: vnt [input] [output]")
    os.exit(2)
end

local output = arg[2] or (input .. ".vnt")
local text = io.read(os.join(input))
if not text then
    print("vnt: " .. input .. ": not found")
    os.exit(127)
end

-- VNote stores its body as quoted-printable UTF-8 text.
local encoded = ""
for i = 1, string.len(text) do
    local char = string.sub(text, i, i)
    if char == "=" then
        encoded = encoded .. "=3D"
    elseif char == "\r" then
        encoded = encoded .. "=0D"
    elseif char == "\n" then
        encoded = encoded .. "=0A"
    else
        encoded = encoded .. char
    end
end

local vnote = "BEGIN:VNOTE\nVERSION:1.1\nBODY;ENCODING=QUOTED-PRINTABLE;CHARSET=UTF-8:" .. encoded .. "\nEND:VNOTE"
local result = io.write(vnote, os.join(output))
if tonumber(result) ~= 0 then
    print("vnt: " .. output .. ": could not write file")
    os.exit(1)
end

print(output)
