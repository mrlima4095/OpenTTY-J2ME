#!/bin/lua

local version = "1.5"

local server = os.getenv("REPO") or "socket://opentty.xyz:31522"
local mirror = {
    ["appmenu"] = { remote = "sys/appmenu/main.lua", here = "/bin/init", description = "Application Menu" },
    ["armitage"] = { remote = "net/armitage", here = "/bin/armitage", depends = { "ifconfig" }, description = "OpenTTY Network Tools" },
    ["audio"] = { remote = "file/audio.lua", here = "/bin/audio", description = "Audio Codec Manager" },
    ["autogc"] = { remote = "sys/autogc", here = "/bin/autogc", description = "Auto Clean Memory" },
    ["chroot"] = { remote = "sys/chroot.lua", here = "/bin/chroot", description = "Change to a real File System" },
    ["cmatrix"] = { remote = "games/cmatrix", here = "/bin/cmatrix", description = "The Matrix Effect" },
    ["curl"] = { remote = "net/curl.lua", here = "/bin/curl", description = "Connect with a Server" },
    ["debug"] = { remote = "sys/debug", here = "/bin/debug", description = "Toggle debug mode" },
    ["docker"] = { remote = "sys/docker/main.lua", here = "/bin/docker", description = "Conteiners on OpenTTY" },
    ["expr"] = { remote = "file/expr.lua", here = "/bin/expr", description = "Evaluate expressions" },
    ["free"] = { remote = "sys/free.lua", here = "/bin/free", description = "Memory Informations" },
    ["forge"] = { remote = "dev/forge/main.lua", here = "/lib/forge", description = "Additional API for OpenTTY" },
    ["find"] = { remote = "file/find.lua", here = "/bin/find", description = "Search a pattern value in a file" },
    ["grep"] = { remote = "file/grep.lua", here = "/bin/grep", description = "Check if a file contains a pattern" },
    ["hash"] = { remote = "file/hash.lua", here = "/bin/hash", description = "Prints file hash" },
    ["head"] = { remote = "file/head.lua", here = "/bin/head", description = "Prints first lines of a file" },
    ["hostname"] = { remote = "net/hostname.lua", here = "/bin/hostname", description = "Manage Host Name" },
    ["htop"] = { remote = "sys/htop", here = "/bin/htop", description = "System Monitor" },
    ["ifconfig"] = { remote = "net/ifconfig.lua", here = "/bin/ifconfig", description = "Network Informations" },
    ["jdb"] = { remote = "sys/benchmark/main.lua", here = "/bin/jdb", depends = { "forge", "log" }, description = "Debugging API" },
    ["log"] = { remote = "sys/smile/logs.lua", here = "/bin/log", description = "MIDlet Logs" },
    ["nano"] = { remote = "file/nano.lua", here = "/bin/nano", description = "Text Editor for OpenTTY" },
    ["nc"] = { remote = "net/netcat.lua", here = "/bin/nc", description = "Connect with Remote Interfaces" },
    ["nice"] = { remote = "sys/nice.lua", here = "/bin/nice", description = "Chance process priority" },
    ["passwd"] = { remote = "sys/passwd.lua", here = "/bin/passwd", description = "Change password" },
    ["pastebin"] = { remote = "net/pastebin", here = "/bin/pastebin", description = "PasteBin Client for OpenTTY" },
    ["ping"] = { remote = "net/ping.lua", here = "/bin/ping", description = "Test connection delay" },
    ["prg"] = { remote = "sys/push.lua", here = "/bin/prg", description = "PushRegister Manager" },
    ["sed"] = { remote = "file/sed.lua", here = "/bin/sed", description = "String Editor" },
    ["sdk"] = { remote = "dev/sdkme", here = "/bin/sdk", depends = { "forge" }, description = "OpenTTY Application SDK" },
    ["stop"] = { remote = "sys/stop.lua", here = "/bin/stop", description = "Kill application by name" },
    ["sync"] = { remote = "sys/sync", here = "/bin/sync", description = "OpenTTY Updater Checker" },
    ["svchost"] = { remote = "sys/svchost.lua", here = "/bin/svchost", description = "Quick Launch Services" },
    ["sudo"] = { remote = "sys/sudo.lua", here = "/bin/sudo", description = "" },
    ["uname"] = { remote = "sys/uname", here = "/bin/uname", description = "System Informations" },
    ["useradd"] = { remote = "sys/users/useradd.lua", here = "/bin/useradd", description = "Add users" },
    ["userdel"] = { remote = "sys/users/userdel.lua", here = "/bin/userdel", description = "Remove users" },
    ["viewer"] = { remote = "file/img.lua", here = "/bin/imgview", description = "View Images" },
    ["webproxy"] = { remote = "net/proxy.lua", here = "/bin/shprxy", description = "Access OpenTTY on WebProxy" },
    ["wget"] = { remote = "net/wget", here = "/bin/wget", description = "Download files from Network" },
    ["xterm"] = { remote = "xterm.lua", here = "/bin/xterm", description = "MIDlet Terminal" },
    ["watch"] = { remote = "sys/watch.lua", here = "", description = "Watch a program" },
    ["yang"] = { remote = "yang.lua", here = "/bin/yang", description = "OpenTTY Package Manager" },

    --[""] = { remote = "", here = "", depends = {}, description = "" },
    --[""] = { description = "", packages = {} },

    ["net"] = { description = "Network Utilities", packages = { "curl", "ifconfig", "ping", "webproxy" } },
    ["file"] = { description = "File Utitlities", packages = { "find", "grep", "hash", "head", "nano", "sed" } },
    ["dev"] = { description = "Development Tools", packages = { "forge", "jdb", "sdk", "svchost", "prg" } },
    ["sys"] = { description = "System Utitlities", packages = { "autogc", "hostname", "htop", "free", "sudo", "uname", "passwd", "nice" } }
}

local function connect(payload)
    local ok, conn, i, o = pcall(socket.connect, "socket://opentty.xyz:31522")

    if ok then
        io.write(payload, o)
        local content = io.read(i, 8192)

        pcall(io.close, conn, i, o)

        return content
    else
        return nil
    end
end

local function install(pkg, verbose)
    if verbose then
        print(":: Installing " .. pkg .. "...")
    end

    local info = mirror[pkg]

    if info.depends then
        print(":: Building dependencies")
        for _, dep in ipairs(info.depends) do
            install(dep, verbose)
        end
    end

    local content = connect("get apps/" .. info.remote)
    if content then
        local status = io.write(content, info.here)

        if verbose then
            if status == 0 then
                print(":: Installed " .. info.here)
            else
                print(":: Installation error for " .. pkg)
            end
        end

        return status == 0
    else
        if verbose then
            print(":: Connection error")
        end

        return nil
    end
end
local function remove(pkg, verbose)
    if verbose then
        print(":: Removing " .. pkg .. "...")
    end

    local info = mirror[pkg]

    local status = os.remove(info.here)

    if verbose then
        if status == 0 then
            print(":: Removed " .. info.here)
        elseif status == 127 then
            print(":: Package not installed")
        else
            print(":: Removing error for " .. pkg)
        end
    end

    return status == 0
end


os.setproc("name", "yang")
if arg[1] == nil or arg[1] == "help" then
    print("Yang Package Manager v" .. version)
    print("Usage: yang <command> [package]")
    print("Commands:")
    print("  install <package>  - Install a package")
    print("  remove <package>   - Remove a package")
    print("  update             - Check for updates")
    print("  list               - List available packages")
    print("  info <package>     - Show package information")
    print("  help               - Show this help")
elseif arg[1] == "install" then
    if os.getuid() > 0 then print("Permission denied!") os.exit(13) end

    if arg[2] then
        for i = 2, #arg - 1 do
            local pkg = arg[i]

            if mirror[pkg] then
                if mirror[pkg].packages then
                    print(":: Installing collection: " .. pkg)
                    print(":: Description: " .. mirror[pkg].description)
                    print("")

                    local sucess_count = 0
                    local total_count = #mirror[pkg].packages

                    for _, query in ipairs(mirror[pkg].packages) do
                        if install(query, false) then
                            sucess_count = sucess_count + 1
                        end
                    end

                    print(":: Collection installation complete!")
                    print("-> Sucessfully installed: " .. sucess_count .. "/" .. total_count .. " packages")
                elseif mirror[pkg].remote then
                    install(pkg, true)
                else
                    print("yang: install: " .. pkg .. ": invalid package (not remote source)")
                    os.exit(1)
                end
            else
                print("yang: install: " .. pkg .. ": not found")
                os.exit(127)
            end
        end
    else
        print("yang: usage: yang remove [package]")
    end
elseif arg[1] == "remove" then
    if os.getuid() > 0 then print("Permission denied!") os.exit(13) end

    if arg[2] then
        for i = 2, #arg - 1 do
            local pkg = arg[i]

            if mirror[pkg] then
                if mirror[pkg].packages then
                    print(":: Removing collection: " .. pkg)
                    print("")

                    local remove_count = 0

                    for _, query in ipairs(mirror[pkg].packages) do
                        if remove(query, false) then
                            remove_count = remove_count + 1
                        end
                    end

                    print(":: Removed " .. remove_count .. " packages from collection '" .. pkg .. "'")
                elseif mirror[pkg].remote then
                    remove(pkg, true)
                else
                    print("yang: remove: " .. pkg .. ": invalid package")
                    os.exit(2)
                end
            else
                print("yang: remove: " .. pkg .. ": not found")
                os.exit(127)
            end
        end
    else
        print("yang: usage: yang remove [package]")
    end
elseif arg[1] == "update" then
    local response = connect("fetch")
    if response then
        print(":: " .. response)
    else
        print(":: Connection error")
        os.exit(101)
    end
elseif arg[1] == "list" then
    print("Available packages:")
    for name, info in pairs(mirror) do
        if not info.packages then
            print("- " .. name .. ": " .. info.description)
        end
    end
elseif arg[1] == "info" then
    if arg[2] then
        local pkg = arg[2]

        if mirror[pkg] then
            local info = mirror[pkg]
            if info.packages then
            else
                print("Package: " .. pkg)
                print("Description: " .. info.description)
                print("Path: " .. info.here)
                
                if info.depends then
                    print("Dependencies: " .. table.concat(info.depends, ", "))
                end
            end
        else
            print(":: " .. pkg .. " not found")
            os.exit(127)
        end
    else
        print("yang: usage: yang info [package]")
    end
end
