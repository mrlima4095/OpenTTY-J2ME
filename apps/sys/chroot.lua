#!/bin/lua

if arg[1] then
    local pull, root = false, "/"

    if arg[1] == "--pull" then
        if arg[2] then
            root = arg[2]
        else
            print("chroot --pull [root]")
        end
    else
        root = arg[1]
    end

    if string.sub(root, -1) ~= "/" then
        root = root .. "/"
    end

    root = os.join(root)

    if string.sub(root, 1, 5) ~= "/mnt/" then
        print("chroot: invalid path, usage a mount point")
        os.exit(2)
    end

    if pull then
        --[[ /bin/ /etc/ /lib/ ]]
        os.mkdir(root + "bin/") os.mkdir(root + "dev/") os.mkdir(root + "etc/") os.mkdir(root + "home/")
        os.mkdir(root + "lib/") os.mkdir(root + "mnt/") os.mkdir(root + "proc/") os.mkdir(root + "tmp/")
        for _,file in pairs(io.dirs("/bin/")) do io.copy("/bin/" .. file, root .. "bin/" .. file) end
        for _,file in pairs(io.dirs("/etc/")) do io.copy("/etc/" .. file, root .. "etc/" .. file) end
        for _,file in pairs(io.dirs("/lib/")) do io.copy("/lib/" .. file, root .. "lib/" .. file) end
    end
    
    local scope = os.scope()

    scope["ROOT"] = root
else
    print("chroot: usage: chroot [options] [root]")
end