#!/bin/lua

if arg[1] then
    graphics.SetTitle(graphics.getCurrent(), arg[1])
else
    print("title [title]")
    print("- change title of current screen")
end