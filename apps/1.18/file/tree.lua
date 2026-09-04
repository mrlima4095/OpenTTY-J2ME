#!/bin/lua

local version = "1.0.0"
local max_depth = 10
local show_files = true
local dir_only = false

local function count_entries(path)
    local entries = io.dirs(path)
    if not entries then return 0 end
    local n = 0
    for _ in pairs(entries) do n = n + 1 end
    return n
end

local function is_dir(path)
    local entries = io.dirs(path)
    if not entries then return false end
    return count_entries(path) > 0
end

local function tree_walk(path, prefix, depth, is_last)
    if depth > max_depth then return 0, 0 end

    local entries = io.dirs(path)
    if not entries then return 0, 0 end

    local names = {}
    for _, name in pairs(entries) do
        table.insert(names, tostring(name))
    end
    table.sort(names)

    local dirs = 0
    local files = 0

    for i = 1, #names do
        local name = names[i]
        local full = path
        if full == "/" then
            full = "/" .. name
        else
            full = full .. "/" .. name
        end

        local last = (i == #names)
        local connector = "+-- "
        if last then connector = "\\-- " end

        local isname_dir = false
        if string.sub(name, -1) == "/" then
            isname_dir = true
        else
            local sub = io.dirs(full)
            if sub and count_entries(full) > 0 then
                isname_dir = true
            end
        end

        if isname_dir then
            if show_files or dir_only then
                print(prefix .. connector .. name)
            else
                print(prefix .. connector .. name)
            end
            dirs = dirs + 1
            local new_prefix = prefix
            if last then
                new_prefix = prefix .. "    "
            else
                new_prefix = prefix .. "|   "
            end
            local d, f = tree_walk(full, new_prefix, depth + 1, last)
            dirs = dirs + d
            files = files + f
        elseif not dir_only then
            print(prefix .. connector .. name)
            files = files + 1
        end
    end

    return dirs, files
end

local function show_help()
    print("OpenTTY Tree v" .. version)
    print("")
    print("Usage: tree [options] [directory]")
    print("")
    print("Options:")
    print("  -d        Directories only")
    print("  -L N      Max depth (default: 10)")
    print("  -a        Show hidden files")
    print("  --help    Show this help")
    print("")
    print("Example:")
    print("  tree /")
    print("  tree -d /etc/")
    print("  tree -L 2 /home/")
end

os.setproc("name", "tree")

if arg[1] == "--help" or arg[1] == "-h" then
    show_help()
else
    local path = "/"
    local i = 1
    while i <= #arg do
        if arg[i] == "-d" then
            dir_only = true
        elseif arg[i] == "-L" then
            i = i + 1
            if i <= #arg then
                max_depth = tonumber(arg[i]) or 10
            end
        elseif arg[i] == "-a" then
            show_files = true
        elseif string.sub(arg[i], 1, 1) ~= "-" then
            path = arg[i]
        end
        i = i + 1
    end

    if string.sub(path, -1) == "/" and #path > 1 then
        path = string.sub(path, 1, -2)
    end

    print(path)
    local dirs, files = tree_walk(path, "", 0, true)
    print("")
    print(dirs .. " directories, " .. files .. " files")
end
