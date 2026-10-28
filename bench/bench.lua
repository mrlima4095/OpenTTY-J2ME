#!/bin/lua
-- bench.lua - Lua twin of bench/bench.c.
--
-- Both files run the same eight micro-benchmarks with the same sizes and
-- produce the same checksum per test, so a diff of the two reports shows
-- only the elapsed time. Keep the two files in sync when editing.
--
-- Usage: bench.lua [scale] [passes] [report]
--   scale   multiplies the loop counts (default 1, the caps are in bench.c)
--   passes  repeats the whole suite (default 1)
--   report  report path (default /mnt/opentty/bench-lua.txt)
--
-- Timing: os.clock() is milliseconds since this process started.

local scale = 1
local passes = 1
local outpath = "/mnt/opentty/bench-lua.txt"

if arg then
    if arg[1] ~= nil then scale = tonumber(arg[1]) or 1 end
    if arg[2] ~= nil then passes = tonumber(arg[2]) or 1 end
    if arg[3] ~= nil then outpath = arg[3] end
end
if scale < 1 then scale = 1 end
if passes < 1 then passes = 1 end

-- Sizes: must match the SIZE block in bench.c.
local function size(base, cap)
    local n = base * scale
    if n > cap then n = cap end
    return n
end

local N_SUM = size(50000, 60000)
local N_CALLS = size(20000, 60000)
local N_FIB = 20 + scale - 1
if N_FIB > 24 then N_FIB = 24 end
local N_SIEVE = size(20000, 60000)
local N_SORT = size(400, 4000)
local N_STR = size(600, 4000)
local N_ARRAY = size(20000, 40000)
local N_ALLOC = size(2000, 20000)

local lines = {}
local total = 0

local function emit(text)
    lines[#lines + 1] = text
    print(text)
end

-- 1. Integer loop: add 1..n without any function call.
local function test_sum(n)
    local s = 0
    local i = 1
    while i <= n do
        s = s + i
        i = i + 1
    end
    return s % 1000003
end

-- 2. Function call per iteration.
local function add(a, b)
    return a + b
end

local function test_calls(n)
    local s = 0
    local i = 1
    while i <= n do
        s = add(s, i)
        i = i + 1
    end
    return s % 1000003
end

-- 3. Recursive Fibonacci (fib(20) = 21891 calls).
local fib
fib = function(k)
    if k < 2 then return k end
    return fib(k - 1) + fib(k - 2)
end

local function test_fib(n)
    return fib(n) % 1000003
end

-- 4. Sieve of Eratosthenes over a boolean table.
local function test_sieve(n)
    local composite = {}
    local count = 0
    local sum = 0
    local i = 2
    while i <= n do
        if not composite[i] then
            count = count + 1
            sum = sum + i
            -- i * i is exact here (Lua numbers are doubles); bench.c needs a
            -- guard because its int overflows above 46340.
            local j = i * i
            if j <= n then
                while j <= n do
                    composite[j] = true
                    j = j + i
                end
            end
        end
        i = i + 1
    end
    return (count * 1000 + sum) % 1000003
end

-- 5. Insertion sort of an LCG-filled array, then a weighted checksum.
local function test_sort(n)
    local a = {}
    local seed = 12345
    local i = 0
    while i < n do
        seed = (seed * 75 + 74) % 65537
        a[i] = seed % 1000
        i = i + 1
    end
    i = 1
    while i < n do
        local v = a[i]
        local j = i - 1
        while j >= 0 and a[j] > v do
            a[j + 1] = a[j]
            j = j - 1
        end
        a[j + 1] = v
        i = i + 1
    end
    local s = 0
    i = 0
    while i < n do
        s = (s + a[i] * (i + 1)) % 1000003
        i = i + 1
    end
    return s
end

-- 6. String building (n * 2 chars) and a byte-by-byte hash.
local function test_string(n)
    local s = ""
    local i = 1
    while i <= n do
        s = s .. "ab"
        i = i + 1
    end
    local h = 0
    i = 1
    while i <= #s do
        h = (h * 31 + string.byte(s, i, i)) % 1000003
        i = i + 1
    end
    return h
end

-- 7. Array fill: i * i % 1000 for each slot, then the sum.
local function test_array(n)
    local a = {}
    local i = 0
    while i < n do
        a[i] = (i * i) % 1000
        i = i + 1
    end
    local s = 0
    i = 0
    while i < n do
        s = (s + a[i]) % 1000003
        i = i + 1
    end
    return s
end

-- 8. Allocation: n tables of 8 slots, filled and dropped one at a time.
local function test_alloc(n)
    local s = 0
    local i = 1
    while i <= n do
        local t = {}
        local k = 1
        while k <= 8 do
            t[k] = k + i
            s = (s + t[k]) % 1000003
            k = k + 1
        end
        i = i + 1
    end
    return s
end

-- os.clock() already has a 1 ms resolution, so the difference is an integer
-- number of milliseconds (this runtime has no math.floor).
local function run(name, n, fn)
    local started = os.clock()
    local checksum = fn(n)
    local ms = os.clock() - started
    total = total + ms
    emit(name .. " n=" .. n .. " ms=" .. ms .. " checksum=" .. checksum)
end

local pass = 1
while pass <= passes do
    total = 0
    emit("== bench (lua) pass " .. pass .. " ==")
    run("sum_loop", N_SUM, test_sum)
    run("calls", N_CALLS, test_calls)
    run("fib", N_FIB, test_fib)
    run("sieve", N_SIEVE, test_sieve)
    run("sort", N_SORT, test_sort)
    run("string", N_STR, test_string)
    run("array", N_ARRAY, test_array)
    run("alloc", N_ALLOC, test_alloc)
    emit("total ms=" .. total)
    pass = pass + 1
end

local text = table.concat(lines, "\n") .. "\n"
if not pcall(io.write, text, outpath) then
    pcall(io.write, text, "/tmp/bench-lua.txt")
end
