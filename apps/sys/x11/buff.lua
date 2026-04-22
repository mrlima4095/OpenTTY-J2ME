#!/bin/lua

if arg[1] then
    graphics.SetText(io.stdin, arg[1])
else
    print("buff [text]")
    print("- set text of /dev/stdin")
end