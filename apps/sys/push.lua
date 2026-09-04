#!/bin/lua

if arg[1] then
    push.setAlarm(arg[2] or "OpenTTY", tonumber(arg[1]))
else
    print("prg: usage: prg [time] [midlet]")
end