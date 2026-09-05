#!/bin/lua

if not arg[1] then print("groupdel: usage: groupdel [group...]") os.exit(2) end
for i = 1, #arg do
    local status = os.request(1, "groupdel", arg[i])
    if status ~= 0 then print("groupdel: " .. arg[i] .. ": failed") os.exit(tonumber(status)) end
end
