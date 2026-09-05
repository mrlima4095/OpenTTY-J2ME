#!/bin/lua

local user = arg[1] or os.scope()["USER"]
local result = os.request(1, "groups", user)
if result then print(user .. " : " .. tostring(result)) else os.exit(1) end
