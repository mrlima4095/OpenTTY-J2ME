#!/bin/lua

os.setproc("name", "df")

local function usage()
    print("Usage: df [options]")
    print("  -h    human readable sizes")
    print("  -a    show all filesystems")
    print("  -h    show this help")
end

local human = false
local show_all = false
local i = 1

local function trunc(x)
    local s = tostring(x)
    local dot = string.find(s, ".")
    if dot then s = string.sub(s, 1, dot - 1) end
    local n = tonumber(s)
    return n or 0
end

while arg[i] do
    local a = tostring(arg[i])
    if a == "-h" or a == "--help" then
        usage()
        os.exit(0)
    elseif a == "-H" then
        human = true
    elseif a == "-a" then
        show_all = true
    end
    i = i + 1
end

local function fmt_size(kb)
    if not human then return tostring(kb) .. "K" end
    if kb < 1024 then return tostring(kb) .. "K" end
    local mb = kb / 1024
    if mb < 1024 then return string.sub(tostring(mb), 1, 4) .. "M" end
    local gb = mb / 1024
    return string.sub(tostring(gb), 1, 4) .. "G"
end

local total_mem = collectgarbage("total")
local used_mem = collectgarbage("count")
local free_mem = collectgarbage("free")

local function pad(s, w)
    local out = tostring(s)
    local extra = w - string.len(out)
    if extra > 0 then
        for p = 1, extra do out = out .. " " end
    end
    return out
end

print("Filesystem          Size   Used  Avail  Use%  Mounted on")
print("------------------- -----  ----  -----  ----  ----------")

local mounts = {
    { fs = "OpenTTY-ROM",   size = total_mem, used = used_mem, free = free_mem, mount = "/rom" },
    { fs = "OpenTTY-TMP",   size = 0,         used = 0,        free = 0,        mount = "/tmp" },
    { fs = "OpenTTY-MNT",   size = 0,         used = 0,        free = 0,        mount = "/mnt" },
    { fs = "OpenTTY-BIN",   size = 0,         used = 0,        free = 0,        mount = "/bin" },
    { fs = "OpenTTY-BOOT",  size = 0,         used = 0,        free = 0,        mount = "/boot" },
}

pcall(function()
    local fstab = io.read("/etc/fstab")
    if fstab and fstab ~= "" then
        local entries = string.split(fstab, "\n")
        for k = 1, #entries do
            local e = entries[k]
            if e ~= "" then
                local parts = string.split(e, " ")
                if #parts >= 2 then
                    local exists = false
                    for m = 1, #mounts do
                        if mounts[m].mount == parts[2] then
                            exists = true
                            break
                        end
                    end
                    if not exists then
                        table.insert(mounts, { fs = parts[1], size = 0, used = 0, free = 0, mount = parts[2] })
                    end
                end
            end
        end
    end
end)

local total_total = 0
local total_used = 0

for k = 1, #mounts do
    local m = mounts[k]
    if show_all or m.size > 0 then
        local use_pct = 0
        if m.size > 0 then
            use_pct = trunc((m.used * 100) / m.size)
        end
        local line = pad(m.fs, 20) .. pad(fmt_size(m.size), 6) .. pad(fmt_size(m.used), 7) .. pad(fmt_size(m.free), 7) .. pad(tostring(use_pct) .. "%", 6) .. m.mount

        print(line)
        total_total = total_total + m.size
        total_used = total_used + m.used
    end
end

print("------------------- -----  ----  -----  ----  ----------")
local total_free = total_total - total_used
local use_pct = 0
if total_total > 0 then use_pct = trunc((total_used * 100) / total_total) end
print("total               " .. fmt_size(total_total) .. "  " .. fmt_size(total_used) .. "  " .. fmt_size(total_free) .. "  " .. tostring(use_pct) .. "%")
