#!/bin/lua

if arg[1] and arg[2] then
    graphics.display(graphics.new("alert", arg[1], arg[2]))
else
    print("warn [title] [body]")
    print("- display an alert window")
end