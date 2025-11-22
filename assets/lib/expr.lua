#!/bin/lua

os.setproc("name", "expr")

if arg[1] then
    local expr = load("return " .. expr)
    print(expr)
else
    print("expr [expression]")
end