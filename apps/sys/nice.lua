#!/bin/lua

os.setproc("name", "nice")

if arg[1] and arg[2] then
    os.request(1, "nice", { pid = arg[1], priority = tonumber(arg[2]) })
else
    print("nice: usage: nice [proc] [priority]")
end