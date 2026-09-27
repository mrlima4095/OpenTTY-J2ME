#!/bin/lua

-- hexdump - dump a file as hexadecimal + ASCII.
--   hexdump <file>

os.setproc("name", "hexdump")

local digits = "0123456789abcdef"

local function byte_hex(b)
    local lo = b % 16
    local hi = (b - lo) / 16
    return string.sub(digits, hi + 1, hi + 1) .. string.sub(digits, lo + 1, lo + 1)
end

local function offset_hex(v)
    local out = ""
    for i = 1, 8 do
        local lo = v % 16
        v = (v - lo) / 16
        out = string.sub(digits, lo + 1, lo + 1) .. out
    end
    return out
end

local file = arg[1]

if file == nil or file == "-h" or file == "--help" then
    print("hexdump: usage: hexdump [file]")
    os.exit(2)
end

local data = io.read(os.join(file))
if data == nil then
    print("hexdump: " .. file .. ": not found")
    os.exit(127)
end

local n = #data
print("hexdump: " .. file .. ": " .. n .. " bytes")
print("")

for i = 1, n, 16 do
    local row_end = i + 15
    if row_end > n then row_end = n end

    local hexpart = ""
    local ascpart = ""

    for j = i, row_end do
        local b = string.byte(data, j)
        hexpart = hexpart .. byte_hex(b) .. " "
        if j == i + 7 then hexpart = hexpart .. " " end

        if b >= 32 and b < 127 then
            ascpart = ascpart .. string.char(b)
        else
            ascpart = ascpart .. "."
        end
    end

    for j = row_end + 1, i + 15 do
        hexpart = hexpart .. "   "
        if j == i + 7 then hexpart = hexpart .. " " end
    end

    print(offset_hex(i - 1) .. "  " .. hexpart .. " |" .. ascpart .. "|")
end