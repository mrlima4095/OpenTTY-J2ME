#!/bin/lua

os.setproc("name", "exec")

local parts = {}
for i = 1, #arg do
    local v = arg[i]
    if v then parts[#parts + 1] = v end
end

local line = string.trim(table.concat(parts, " "))
if line ~= "" then
    local cmds = string.split(line, "&")
    for i = 1, #cmds do
        local seg = string.trim(cmds[i])
        if seg ~= "" then
            local status = os.execute(seg)
            if status ~= 0 then os.exit(status) end
        end
    end
end