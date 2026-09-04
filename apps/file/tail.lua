#!/bin/lua

os.setproc("name", "tail")

local file = arg[1]
local count = 10

if not file then
    print("tail: usage: tail [file] [count]")
    os.exit(2)
end

local okn, n = pcall(tonumber, arg[2])
if okn and n then count = n end

local content = io.read(os.join(file))
if not content then
    print("tail: " .. file .. ": not found")
    os.exit(127)
end

if content == "" then os.exit(0) end

local lines = string.split(content, "\n")
local start = #lines - count
if start < 0 then start = 0 end
for i = start + 1, #lines do print(lines[i]) end