#!/bin/lua

os.setproc("name", "expr")

if arg[1] then
    print(load("return " .. expr))
else
    print("expr [expression]")
end