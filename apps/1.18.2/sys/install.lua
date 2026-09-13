#!/bin/lua

if arg[1] then
    local root = os.join(arg[1])

    if string.sub(root, 1, 5) ~= "/mnt/" then
        print("install: " .. root .. ": invalid path, usage a mount point")
        os.exit(2)
    end

    local dirs = { "bin/", "boot/", "dev/", "etc/", "home/", "lib/", "mnt/", "root/", "proc/", "tmp/" }
    for _, dir in pairs(dirs) do
        local dir_pwd = root .. dir
        print("creating directory: " .. dir_pwd)
        print(pcall(os.mkdir, dir_pwd))
    end
    for _, file in pairs(io.dirs("/bin/")) do
        local file_pwd = root .. "bin/" .. file
        print("copying file: " .. file_pwd)
        print(pcall(io.copy, "/bin/" .. file, file_pwd))
    end
    for _, file in pairs(io.dirs("/etc/")) do
        local file_pwd = root .. "etc/" .. file
        print("copying file: " .. file_pwd)
        print(pcall(io.copy, "/etc/" .. file, file_pwd))
    end
    for _, file in pairs(io.dirs("/lib/")) do
        local file_pwd = root .. "lib/" .. file
        print("copying file: " .. file_pwd)
        print(pcall(io.copy, "/lib/" .. file, file_pwd))
    end
    
    print("install: OpenTTY system cloned to " .. root)
else
    print("install: usage: install [path]")
end
