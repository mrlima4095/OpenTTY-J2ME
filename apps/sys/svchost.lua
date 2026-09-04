#!/bin/lua

if arg[1] then
    print(pcall(os.request, 1, "serve", os.join(arg[1])))
else
    print("svchost [service]")
end