#!/bin/lua

os.setproc("name", "nice")

if arg[1] and arg[2] then
    local pid = arg[1]
    local priority = tonumber(arg[2])

    if not priority then
        print("nice: " .. arg[2] .. ": invalid priority")
        os.exit(2)
    end

    local status = os.request(1, "nice", { pid = pid, priority = priority })

    if status == 127 then
        print("nice: (" .. pid .. ") - No such process")
        os.exit(127)
    elseif status == 13 then
        print("nice: (" .. pid .. ") - Permission denied")
        os.exit(13)
    elseif status ~= 0 then
        print("nice: (" .. pid .. ") - cannot change priority")
        os.exit(status)
    end
else
    print("nice: usage: nice [proc] [priority]")
end
