#!/bin/lua

local version = "1.0"

local function lock()
    graphics.display(graphics.BuildScreen({
        title = "Login",
        back = { label = "Exit", root = function () os.execute("exit") end },
        button = {
            label = "Login",
            root = function (user, passwd)
                local uid = os.execute("case user (" .. user .. ") false")
                local pid = os.execute("case passwd (" .. passwd .. ") false")

                if uid == 255 and pid == 255 then os.execute("xterm") os.exit()
                else
                    os.execute("@alert")

                    local dict = { title = "DarkDM+", message = "User not found!", back = { label = "Back", root = function () lock() end } }

                    if pid ~= 255 then dict.message = "Wrong password!" end

                    graphics.display(graphics.Alert(dict))
                end
            end
        },

        fields = {
            { type = "field", label = "Username" },
            { type = "field", label = "Password", mode = "text password" }
        }
    }))
end

os.setproc("name", "darkdm")

if arg[1] == "lock" then lock()
elseif arg[1] == "info" then print("DarkDM+ " .. version)
else print(arg[0] .. ": " .. arg[1] .. ": not found") end