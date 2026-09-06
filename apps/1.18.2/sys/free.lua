#!/bin/lua

print("        total    used    free")
print("Mem.:  " .. collectgarbage("total") .. "Kb " .. collectgarbage("count") .. "Kb" .. collectgarbage("free") .. "Kb")
print("Swap: Unavailable")