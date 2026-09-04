#!/bin/lua

os.setproc("name", "container-sh")

local image_path = arg[1]
local container_id = arg[2]
local scope = os.scope()
local root = scope["ROOT"] or "/"
local version = scope["VERSION"] or "unknown"
local container_user = scope["USER"] or "guest"
local hostname = scope["HOSTNAME"] or "container"

os.setproc("name", "sh@" .. hostname)

local previous = graphics.GetCurrent()

local screen = graphics.new("screen", hostname .. " (" .. container_user .. ")")
local input = graphics.new("field", { type = "field", label = container_user .. "@" .. hostname .. " $", value = "" })
local output = graphics.new("buffer", { style = "monospace" })
local run = graphics.new("command", { label = "Run", type = "ok", priority = 1 })
local back = graphics.new("command", { label = "Exit", type = "back", priority = 1 })
local clear = graphics.new("command", { label = "Clear", type = "screen", priority = 1 })
local switch = graphics.new("command", { label = "Switch to...", type = "screen", priority = 2 })

graphics.append(screen, output)
graphics.append(screen, input)
graphics.addCommand(screen, run)
graphics.addCommand(screen, back)
graphics.addCommand(screen, clear)
graphics.addCommand(screen, switch)
os.setproc("screen", screen)

io.setstdout(output)

print("OpenTTY Docker " .. version)
print("Container " .. (container_id or "unknown"))
print("Type 'exit' to leave.")
print("")

local function run_cmd(cmd)
    if not cmd or cmd == "" then return end

    if cmd == "exit" then
        graphics.display(previous)
        os.exit(0)
        return
    end

    if cmd == "clear" then
        graphics.SetText(output, "")
        return
    end

    if cmd == "hostname" then
        print(hostname)
        return
    end

    if cmd == "whoami" then
        print(container_user)
        return
    end

    if cmd == "id" then
        print("uid=" .. tostring(os.getuid()) .. "(" .. container_user .. ")")
        return
    end

    if cmd == "pwd" then
        print(scope["PWD"])
        return
    end

    if cmd == "uname" or cmd == "uname -a" then
        print("OpenTTY Docker " .. version .. " (J2ME)")
        return
    end

    if cmd == "date" then
        print(os.date())
        return
    end

    if cmd == "uptime" then
        print(tostring(os.clock()) .. "ms")
        return
    end

    if cmd == "env" then
        for k, v in pairs(scope) do
            if type(v) == "string" then
                print(k .. "=" .. v)
            end
        end
        return
    end

    if string.startswith(cmd, "cd ") then
        local dir = string.sub(cmd, 4)
        local full = os.join(dir)
        scope["PWD"] = full
        os.scope(scope)
        return
    end

    if cmd == "cd" then
        scope["PWD"] = "/home/"
        os.scope(scope)
        return
    end

    if cmd == "ls" or string.startswith(cmd, "ls ") then
        local target = scope["PWD"]
        if string.startswith(cmd, "ls ") then
            target = os.join(string.sub(cmd, 4))
        end
        local dirs = io.dirs(target)
        if dirs then
            for _, f in pairs(dirs) do
                print(f)
            end
        else
            print("ls: cannot access '" .. target .. "': No such directory")
        end
        return
    end

    if string.startswith(cmd, "cat ") then
        local file = os.join(string.sub(cmd, 5))
        local content = io.read(file)
        if content then
            print(content)
        else
            print("cat: " .. file .. ": No such file")
        end
        return
    end

    if string.startswith(cmd, "echo ") then
        local text = string.sub(cmd, 6)
        text = string.env(text)
        print(text)
        return
    end

    if string.startswith(cmd, "export ") then
        local parts = string.split(string.sub(cmd, 8), "=")
        if #parts == 2 then
            scope[parts[1]] = parts[2]
            os.scope(scope)
        else
            print("export: usage: export KEY=VALUE")
        end
        return
    end

    if string.startswith(cmd, "mkdir ") then
        local dir = os.join(string.sub(cmd, 7))
        local status = os.mkdir(dir)
        if status == 128 then
            print("mkdir: " .. dir .. ": already exists")
        elseif status ~= 0 then
            print("mkdir: " .. dir .. ": failed")
        end
        return
    end

    if string.startswith(cmd, "rm ") then
        local file = os.join(string.sub(cmd, 4))
        local status = os.remove(file)
        if status ~= 0 then
            print("rm: " .. file .. ": failed (status " .. tostring(status) .. ")")
        end
        return
    end

    local ok, status = pcall(os.execute, cmd)
    if not ok then
        print("sh: " .. cmd .. ": command not found")
    elseif status and status > 0 then
        print("sh: " .. cmd .. ": exit status " .. tostring(status))
    end
end

graphics.handler(screen, {
    [run] = function(cmd)
        run_cmd(cmd)
        graphics.SetText(input, "")
    end,
    [back] = function()
        graphics.display(previous)
        os.exit(0)
    end,
    [clear] = function()
        graphics.SetText(output, "")
    end,
    [switch] = graphics.taskmngr,
})

graphics.display(screen)
