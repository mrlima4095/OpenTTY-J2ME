#!/bin/lua

if not arg[1] then print("groupadd: usage: groupadd [group...]") os.exit(2) end
for i = 1, #arg do
    local status = os.request(1, "groupadd", arg[i])
    if status ~= 0 then print("groupadd: " .. arg[i] .. ": failed") os.exit(tonumber(status)) end
end
