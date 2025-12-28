#!/bin/lua

if arg[1] then
    local status = pcall(io.write, arg[1], "/etc/hostname")

    if status == 13 then
        print("hostname: permission denied")
        os.exit(13)
    end
else
    print(io.read("/etc/hostname"))
end