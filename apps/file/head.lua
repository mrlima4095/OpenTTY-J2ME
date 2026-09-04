#!/bin/lua

os.setproc("name", "head")

local file = arg[1]
local count = 10

if not file then
    print("head: usage: head [file] [count]")
    os.exit(2)
end

local okn, n = pcall(tonumber, arg[2])
if okn and n then count = n end

local content = io.read(os.join(file))
if not content then
    print("head: " .. file .. ": not found")
    os.exit(127)
end

if content == "" then os.exit(0) end

local lines = string.split(content, "\n")
if count > #lines then count = #lines end
for i = 1, count do print(lines[i]) end