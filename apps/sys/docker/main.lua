#!/bin/lua

os.setproc("name", "docker")

local HELP = [[
OpenTTY Docker - J2ME Container Runtime

Usage: docker <command> [options]

Commands:
  run <image>          Run a container from an image file
  exec <id> <cmd>      Execute a command in a running container
  ps                   List running containers
  pull <source>        Pull an image from source
  stop <id>            Stop a running container
  kill <id>            Force kill a container
  rm <id>              Remove a stopped container
  images               List available images
  logs <id>            Show container logs
  login <user> <pass>  Authenticate as root
  help                 Show this help

Images are .lua files in /etc/docker/image/ or /lib/docker/.
]]

local function find_image(name)
    local paths = { "/etc/docker/image/", "/lib/docker/", "/home/" }
    for _, base in pairs(paths) do
        local file = io.open(base .. name)
        if file then
            local content = io.read(file)
            io.close(file)
            return content, base .. name
        end
    end

    if string.find(name, "/") then
        local file = io.open(name)
        if file then
            local content = io.read(file)
            io.close(file)
            return content, name
        end
    end

    return nil
end

local function gen_id()
    local chars = "abcdef0123456789"
    local id = ""
    for i = 1, 8 do
        local r = math.random(1, #chars)
        id = id .. string.sub(chars, r, r)
    end
    return id
end

local function pad(s, width)
    s = tostring(s or "")
    if #s >= width then return string.sub(s, 1, width) end
    local out = s
    for i = 1, width - #s do out = out .. " " end
    return out
end

local function sep(n)
    local out = ""
    for i = 1, n do out = out .. "-" end
    return out
end

local server
local function ensure_server()
    server = os.getpid("conteinerd")
    if not server then
        os.request(1, "serve", os.join(arg[0]))
        server = os.getpid("conteinerd")
        if not server then
            print("docker: failed to start container daemon")
            os.exit(1)
        end
    end
    return server
end

if arg[1] == "run" then
    if not arg[2] then
        print("docker: usage: docker run <image> [options]")
        os.exit(2)
    end

    local image_name = arg[2]
    local content, path = find_image(image_name)
    if not content then
        print("docker: " .. image_name .. ": image not found")
        os.exit(127)
    end

    local ok, image = pcall(load, content)
    if not ok then
        print("docker: " .. image_name .. ": invalid image format")
        os.exit(2)
    end

    if type(image) == "function" then
        ok, image = pcall(image)
    end

    if not ok or type(image) ~= "table" then
        print("docker: " .. image_name .. ": failed to load image")
        os.exit(2)
    end

    local daemon = ensure_server()
    local id = gen_id()
    local name = image["name"] or (image_name .. "-" .. id)
    local result = os.request(daemon, "create", {
        id = id,
        name = name,
        image = image,
        path = path,
    })

    if result then
        os.request(daemon, "start", { id = id })
        print("docker: container " .. id .. " started (" .. name .. ")")
    else
        print("docker: failed to create container")
        os.exit(1)
    end

elseif arg[1] == "exec" then
    if not arg[2] or not arg[3] then
        print("docker: usage: docker exec <id> <command>")
        os.exit(2)
    end

    local daemon = ensure_server()
    local result = os.request(daemon, "exec", {
        id = arg[2],
        cmd = arg[3],
    })

    if result then
        print(tostring(result))
    else
        print("docker: " .. arg[2] .. ": container not found or not running")
        os.exit(1)
    end

elseif arg[1] == "ps" then
    local daemon = ensure_server()
    local containers = os.request(daemon, "ps", {})

    if containers and type(containers) == "table" then
        local count = 0
        print(pad("ID", 10) .. " " .. pad("NAME", 20) .. " " .. pad("STATUS", 10) .. " " .. pad("IMAGE", 15))
        print(sep(58))
        for _, c in pairs(containers) do
            print(pad(c.id, 10) .. " " .. pad(c.name, 20) .. " " .. pad(c.status, 10) .. " " .. pad(c.image, 15))
            count = count + 1
        end
        if count == 0 then
            print("No running containers.")
        end
    else
        print("No running containers.")
    end

elseif arg[1] == "images" then
    local images = {}
    local search_paths = { "/etc/docker/image/", "/lib/docker/" }

    for _, base in pairs(search_paths) do
        local dirs = io.dirs(base)
        if dirs then
            for _, name in pairs(dirs) do
                if string.endswith(name, ".lua") then
                    local content = find_image(string.sub(name, 1, -5))
                    if content then
                        local ok, fn = pcall(load, content)
                        if ok and type(fn) == "function" then
                            ok, fn = pcall(fn)
                        end
                        if ok and type(fn) == "table" then
                            table.insert(images, {
                                name = fn["name"] or name,
                                version = fn["version"] or "unknown",
                                release = fn["release"] or "stable",
                            })
                        end
                    end
                end
            end
        end
    end

    print(pad("NAME", 20) .. " " .. pad("VERSION", 10) .. " " .. pad("RELEASE", 10))
    print(sep(44))
    for _, img in pairs(images) do
        print(pad(img.name, 20) .. " " .. pad(img.version, 10) .. " " .. pad(img.release, 10))
    end
    if #images == 0 then
        print("No images found.")
    end

elseif arg[1] == "stop" then
    if not arg[2] then
        print("docker: usage: docker stop <id>")
        os.exit(2)
    end

    local daemon = ensure_server()
    local result = os.request(daemon, "stop", { id = arg[2] })
    if result then
        print("docker: container " .. arg[2] .. " stopped")
    else
        print("docker: " .. arg[2] .. ": container not found or not running")
        os.exit(1)
    end

elseif arg[1] == "kill" then
    if not arg[2] then
        print("docker: usage: docker kill <id>")
        os.exit(2)
    end

    local daemon = ensure_server()
    local result = os.request(daemon, "kill", { id = arg[2] })
    if result then
        print("docker: container " .. arg[2] .. " killed")
    else
        print("docker: " .. arg[2] .. ": container not found")
        os.exit(1)
    end

elseif arg[1] == "rm" then
    if not arg[2] then
        print("docker: usage: docker rm <id>")
        os.exit(2)
    end

    local daemon = ensure_server()
    local result = os.request(daemon, "remove", { id = arg[2] })
    if result then
        print("docker: container " .. arg[2] .. " removed")
    else
        print("docker: " .. arg[2] .. ": container not found or still running")
        os.exit(1)
    end

elseif arg[1] == "pull" then
    if not arg[2] then
        print("docker: usage: docker pull <source>")
        os.exit(2)
    end

    local content, path = find_image(arg[2])
    if content then
        print("docker: image " .. arg[2] .. " found at " .. path)
    else
        print("docker: " .. arg[2] .. ": image not found")
        os.exit(127)
    end

elseif arg[1] == "login" then
    if not arg[2] or not arg[3] then
        print("docker: usage: docker login <user> <password>")
        os.exit(2)
    end

    local status = os.su(arg[2], arg[3])
    if status == 0 then
        print("docker: logged in as " .. arg[2])
    else
        print("docker: login failed for " .. arg[2])
        os.exit(13)
    end

elseif arg[1] == "logs" then
    if not arg[2] then
        print("docker: usage: docker logs <id>")
        os.exit(2)
    end

    local daemon = ensure_server()
    local result = os.request(daemon, "logs", { id = arg[2] })
    if result then
        print(tostring(result))
    else
        print("docker: " .. arg[2] .. ": container not found")
        os.exit(1)
    end

elseif arg[1] == "help" or arg[1] == "--help" or arg[1] == nil then
    print(HELP)

elseif arg[1] == "--deamon" or arg[1] == "--daemon" then
    os.setproc("name", "conteinerd")
    os.setproc(false)

    local db = {}
    local logs = {}

    local function get_container(id)
        if db[id] then return db[id] end
        for _, c in pairs(db) do
            if c.id == id or string.startswith(c.id, id) then
                return c
            end
        end
        return nil
    end

    return function(payload, args, scope, pid, uid)
        if payload == "create" then
            local data = args
            local id = data.id
            local image = data.image

            local container_scope = {
                PWD = "/home/",
                USER = "guest",
                ROOT = "/mnt/docker/" .. id .. "/",
                ALIAS = {},
            }

            local image_scope = image["scope"] or {}
            for k, v in pairs(image_scope) do
                container_scope[k] = v
            end

            local fs = image["fs"] or {}
            local root = container_scope["ROOT"]
            os.mkdir(root)
            for path, entries in pairs(fs) do
                local full = root
                if path ~= "/" then
                    full = root .. string.sub(path, 2)
                end
                os.mkdir(full)
                if type(entries) == "table" then
                    for name, content in pairs(entries) do
                        if type(content) == "string" then
                            if string.sub(name, -1) == "/" then
                                os.mkdir(full .. name)
                            elseif content ~= "" then
                                io.write(content, full .. name)
                            end
                        end
                    end
                elseif type(entries) == "string" then
                    io.write(entries, full)
                end
            end

            db[id] = {
                id = id,
                name = data.name or id,
                status = "created",
                image = image["name"] or "unknown",
                image_data = image,
                path = data.path,
                scope = container_scope,
                password = image["password"] or "",
                pid = nil,
            }
            logs[id] = {}

            return true

        elseif payload == "start" then
            local c = get_container(args.id)
            if not c then return false end
            if c.status == "running" then return false end

            c.status = "running"

            local function run_container()
                os.setproc("name", "docker:" .. c.name)
                os.setproc("scope", c.scope)
                os.setproc("cmd", "docker run " .. c.name)
                os.setproc(false)

                os.execute("execute /etc/docker/scripts/shell.lua " .. c.path .. " " .. c.id)
                c.status = "stopped"
                c.pid = nil
            end

            c.pid = os.getpid()
            java.run(run_container, "docker:" .. c.id)
            return true

        elseif payload == "exec" then
            local c = get_container(args.id)
            if not c or c.status ~= "running" then return nil end

            local result = os.request(c.pid, "exec", args.cmd)
            return result

        elseif payload == "ps" then
            local list = {}
            for _, c in pairs(db) do
                table.insert(list, {
                    id = c.id,
                    name = c.name,
                    status = c.status,
                    image = c.image,
                })
            end
            return list

        elseif payload == "stop" then
            local c = get_container(args.id)
            if not c then return false end
            if c.status ~= "running" then return false end

            if c.pid then
                os.request("1", "sendsig", { pid = c.pid, signal = "15" })
            end
            c.status = "stopped"
            c.pid = nil
            return true

        elseif payload == "kill" then
            local c = get_container(args.id)
            if not c then return false end

            if c.pid then
                os.request("1", "sendsig", { pid = c.pid, signal = "9" })
            end
            c.status = "killed"
            c.pid = nil
            return true

        elseif payload == "remove" then
            local c = get_container(args.id)
            if not c then return false end
            if c.status == "running" then return false end

            os.remove(c.path)
            db[c.id] = nil
            logs[c.id] = nil
            return true

        elseif payload == "logs" then
            local c = get_container(args.id)
            if not c then return nil end

            local log = logs[c.id]
            if log then
                return table.concat(log, "\n")
            end
            return ""
        end
    end

elseif arg[1] ~= nil then
    print("docker: '" .. arg[1] .. "' is not a docker command.")
    print("See 'docker help'")
    os.exit(1)
end
