#!/bin/lua

os.setproc("name", "expr")

if arg[1] then print(load("return " .. arg[1])) else print("expr: missing operand") end