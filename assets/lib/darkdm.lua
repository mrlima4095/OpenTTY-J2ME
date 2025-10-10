#!/bin/lua

--[[

[ Config ]

name=DarkDM+
version=1.0
description=Login API

api.version=1.17
api.require=lua
api.error=execute log add DarkDM+ requires OpenTTY 1.17 or newer and Lua Runtime
api.match=minimum

]]

local function lock()
    graphics.display(graphics.BuildScreen({
        title = "Login",
        back = { label = "Exit", root = function () os.execute("exit") end },
        button = {
            label = "Login",
            root = function (user, passwd)
                local uid = os.execute("case user (" .. user .. ") false")
                local pid = os.execute("case passwd (" .. passwd .. ") false")

                if uid == 255 and pid == 255 then
                    os.execute("xterm")
                end
            end
        },

        fields = {
            {
                type = "field",
                label = "Username"
            },
            {
                type = "field",
                label = "Password",
                mode = "text password"
            }
        }
    }))
end