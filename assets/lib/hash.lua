#!/bin/lua

local function joinpath(pwd)
    if string.sub(pwd, 1, 1) ~= "/" then
        return os.getcwd() .. pwd
    end
    return pwd
end

if #arg[1] then
    local file = io.read(joinpath(arg[1]))
    if file then
        print(string.hash(file))
    else
        print("hash: " .. arg[1] .. ": not found")
        os.exit(127)
    end
else
    print("hash [file]")
end