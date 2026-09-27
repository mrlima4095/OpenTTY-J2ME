#!/bin/lua

os.setproc("name", "diff")

local function usage()
    print("Usage: diff [file1] [file2]")
    print("  Compares two files line by line")
    print("  Lines only in file1 are prefixed with <")
    print("  Lines only in file2 are prefixed with >")
end

if not arg[1] or not arg[2] or arg[1] == "-h" or arg[1] == "--help" then
    usage()
    os.exit(arg[1] and 0 or 2)
end

local content1 = io.read(os.join(arg[1]))
if not content1 then
    print("diff: " .. arg[1] .. ": not found")
    os.exit(127)
end

local content2 = io.read(os.join(arg[2]))
if not content2 then
    print("diff: " .. arg[2] .. ": not found")
    os.exit(127)
end

local function split_lines(text)
    if text == "" then return {} end
    return string.split(text, "\n")
end

local lines1 = split_lines(content1)
local lines2 = split_lines(content2)

local max1 = #lines1
local max2 = #lines2
local max = max1
if max2 > max then max = max2 end

local diffs = 0
local i = 1

while i <= max do
    local l1 = lines1[i]
    local l2 = lines2[i]

    if l1 == nil then
        print("--- " .. arg[1])
        print("+++ " .. arg[2])
        print("@@ line " .. tostring(i) .. " @@")
        print("> " .. l2)
        diffs = diffs + 1
    elseif l2 == nil then
        print("--- " .. arg[1])
        print("+++ " .. arg[2])
        print("@@ line " .. tostring(i) .. " @@")
        print("< " .. l1)
        diffs = diffs + 1
    elseif l1 ~= l2 then
        print("--- " .. arg[1])
        print("+++ " .. arg[2])
        print("@@ line " .. tostring(i) .. " @@")
        print("< " .. l1)
        print("> " .. l2)
        diffs = diffs + 1
    end

    i = i + 1
end

if diffs == 0 then
    print("Files " .. arg[1] .. " and " .. arg[2] .. " are identical")
    os.exit(0)
end

print("")
print(tostring(diffs) .. " difference(s) found")
os.exit(1)
