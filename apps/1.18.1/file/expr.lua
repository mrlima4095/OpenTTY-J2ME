#!/bin/lua

os.setproc("name", "expr")
local expr = table.concat(arg, " ")
if expr ~= "" then print(load("return " .. expr)) else print("expr: missing operand") end