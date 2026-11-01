#!/bin/lua
-- canvas.lua - benchmark of the Lua Canvas drawing API (`graphics.canvas`).
--
-- The suite measures what a canvas app pays per call: the Lua -> native
-- dispatch in src/Lua.java, the `ensureBuffer()` guard and the MIDP
-- `Graphics` call in src/LuaCanvas.java, plus the Lua-side work the API
-- forces on the caller (a table for `polygon`, a string for `text`).
--
-- Every test draws into one persistent 240x320 canvas (the default size of
-- the desktop shim headless; on a phone the size differs, so the pixel-bound
-- tests move and the call-bound tests do not). Nothing calls `flush()` or
-- `repaint()`, and headless mode never blits the buffer, so the screen
-- transfer is *not* part of these numbers.
--
-- Tests:
--   native_call  c:color()             marshalling + a native return value
--   color        c:color(0xRRGGBB)     setColor on the buffer graphics
--   clear        c:clear()             full-canvas background fill
--   clear_rect   c:clear(x,y,w,h)      small fill (HUD-style erase)
--   pixel        c:pixel(x,y)          one drawLine of a single pixel
--   line         c:line(x1,y1,x2,y2)   drawLine
--   rect         c:rect(...,true)      fillRect
--   circle       c:circle(...,true)    fillArc
--   polygon      c:polygon(t,true)     scanline fill done in Java + a table
--   text         c:text(s,x,y)         drawString
--   print        c:print(s)            cursor text: charWidth + drawChar per char
--   metrics      c:metrics()/textWidth font metrics
--   state        c:save()+c:restore()  state stack push/pop
--   clip         c:clip()+resetClip()  clip change
--   keys         c:keys()              held-key table (polled every game frame)
--   new          graphics.canvas.new   handle + off-screen frame buffer
--   frame        a full game frame     clear + 24 shapes + HUD text
--
-- Usage: canvas.lua [scale] [passes] [report]
--   scale   multiplies the starting batch sizes (default 1)
--   passes  repeats the whole suite (default 1)
--   report  report path (default /mnt/opentty/bench-canvas.txt)
--
-- Each test auto-calibrates: `os.clock()` ticks in milliseconds, so a batch
-- that finishes in under TARGET_MS is re-run with a bigger n (up to a cap)
-- instead of being reported as "0 ms". The report line carries the final n,
-- the total ms, the derived microseconds per call and a checksum.

local scale = 1
local passes = 1
local outpath = "/mnt/opentty/bench-canvas.txt"

if arg then
    if arg[1] ~= nil then scale = tonumber(arg[1]) or 1 end
    if arg[2] ~= nil then passes = tonumber(arg[2]) or 1 end
    if arg[3] ~= nil then outpath = arg[3] end
end
if scale < 1 then scale = 1 end
if passes < 1 then passes = 1 end

-- Minimum time one batch must take before its measurement is reported.
local TARGET_MS = 25

local function size(base, cap)
    local n = base * scale
    n = n - n % 1
    if n < 1 then n = 1 end
    if n > cap then n = cap end
    return n
end

-- string.format does not exist natively, so build "12.3" by hand.
local function fixed(value)
    if value ~= value then return "nan" end
    if value < 0 then value = -value end
    local scaled = value * 10 + 0.5
    local total = scaled - scaled % 1
    local frac = total % 10
    local whole = (total - frac) / 10
    return whole .. "." .. frac
end

local c = graphics.canvas.new({
    title = "canvas bench",
    background = 0x000018,
    foreground = 0xFFFFFF,
    font = "monospace medium"
})
local W, H = c:size()

local FALLBACK = "/tmp/bench-canvas.txt"
local lines = {}
local total_ms = 0
local tests = 0
local reportOk = true
local out = outpath

-- Report helper: print the line and append it to the report file right away,
-- so a run that is cut short still leaves the tests it finished on disk.
local function flushReport()
    local text = table.concat(lines, "\n") .. "\n"
    if pcall(io.write, text, out) then return true end
    if out ~= FALLBACK then
        out = FALLBACK
        if pcall(io.write, text, out) then return true end
    end
    return false
end

local function emit(text)
    lines[#lines + 1] = text
    print(text)
    if not reportOk then return end
    if not pcall(io.write, text .. "\n", out, "a") then reportOk = flushReport() end
end

local function measure(n, fn)
    local started = os.clock()
    local checksum = fn(n)
    local ms = os.clock() - started
    if ms < 0 then ms = 0 end
    return ms, checksum
end

-- Run one test, growing n until the batch is long enough to time.
-- `extra(n, ms)` can append test-specific fields to the report line.
local function run(name, base, cap, fn, extra)
    local n = size(base, cap)
    local ms, checksum = measure(n, fn)
    local tries = 1
    while ms < TARGET_MS and n < cap and tries < 12 do
        local factor = 2
        if ms > 0 then
            local need = TARGET_MS / ms
            if need > factor then factor = need end
        end
        local grown = n * factor
        grown = grown - grown % 1
        if grown > cap then grown = cap end
        if grown <= n then break end
        n = grown
        ms, checksum = measure(n, fn)
        tries = tries + 1
    end
    total_ms = total_ms + ms
    tests = tests + 1
    local us = 0
    if n > 0 then us = ms * 1000 / n end
    local line = name .. " n=" .. n .. " ms=" .. ms .. " us=" .. fixed(us) .. " checksum=" .. checksum
    if extra then line = line .. " " .. extra(n, ms) end
    emit(line)
end

-- 1. Native call with no drawing at all: `color()` with just the handle
--    returns the pen color, so the cost is dispatch + argument + return.
local function test_native_call(n)
    local s = 0
    local i = 1
    while i <= n do
        s = s + c:color()
        i = i + 1
    end
    return (s + n) % 1000003
end

-- 2. Pen color change (ensureBuffer + Graphics.setColor).
local function test_color(n)
    local i = 1
    while i <= n do
        c:color((i % 6) * 0x222222)
        i = i + 1
    end
    return c:color() % 1000
end

-- 3. Full-canvas clear: the background fillRect of a normal frame.
local function test_clear(n)
    local i = 1
    while i <= n do
        c:clear()
        i = i + 1
    end
    return n % 1000003
end

-- 4. Partial clear, what a HUD redraw uses instead of a full frame clear.
local function test_clear_rect(n)
    local i = 1
    while i <= n do
        c:clear(4, 2, 120, 12)
        i = i + 1
    end
    return n % 1000003
end

-- 5. Single pixel (LuaCanvas.drawPixel is a one-pixel drawLine).
local function test_pixel(n)
    local i = 1
    while i <= n do
        c:pixel(i % W, (i * 7) % H)
        i = i + 1
    end
    return n % 1000003
end

-- 6. Lines.
local function test_line(n)
    local i = 1
    while i <= n do
        local x = i % W
        local y = (i * 3) % H
        c:line(x, y, (x + 20) % W, (y + 12) % H)
        i = i + 1
    end
    return n % 1000003
end

-- 7. Filled rectangles (sprites).
local function test_rect(n)
    local i = 1
    while i <= n do
        c:rect(i % (W - 10), (i * 5) % (H - 10), 8, 6, true)
        i = i + 1
    end
    return n % 1000003
end

-- 8. Filled circles (fillArc with a full sweep).
local function test_circle(n)
    local i = 1
    while i <= n do
        c:circle(20 + (i % (W - 40)), 20 + (i * 3) % (H - 40), 5, true)
        i = i + 1
    end
    return n % 1000003
end

-- 9. Filled polygon: the API takes a table, so the caller pays for building
--    it (three points here) and LuaCanvas runs the scanline fill in Java.
local function test_polygon(n)
    local i = 1
    while i <= n do
        local x = i % (W - 12)
        local y = (i * 3) % (H - 12)
        c:polygon({ x, y, x + 10, y, x + 5, y + 10 }, true)
        i = i + 1
    end
    return n % 1000003
end

-- 10. drawString with a fixed HUD string (no per-call concatenation).
local function test_text(n)
    local i = 1
    while i <= n do
        c:text("SCORE 1234 HP 100", 4 + (i % 40), 2 + (i % 200))
        i = i + 1
    end
    return n % 1000003
end

-- 11. Cursor text: one charWidth + one drawChar per character, plus the
--     cursor bookkeeping (this is the `matrix`/`cterm`-style path).
local function test_print(n)
    local i = 1
    while i <= n do
        c:cursor(0, (i * 8) % 200)
        c:print("0123456789abcdef")
        i = i + 1
    end
    local x, y = c:cursor()
    return (x + y) % 1000
end

-- 12. Font metrics: what a layout pass costs.
local function test_metrics(n)
    local s = 0
    local i = 1
    while i <= n do
        local h, base = c:metrics()
        s = s + c:textWidth("SCORE 1234") + h + base
        i = i + 1
    end
    return s % 1000003
end

-- 13. State stack: save/restore around a nested transform, as a HUD overlay
--     or a rotated sprite does.
local function test_state(n)
    local i = 1
    while i <= n do
        c:save()
        c:translate(2, 3)
        c:color(0x00FF88)
        c:restore()
        i = i + 1
    end
    return c:color() % 1000
end

-- 14. Clip change and reset (a panel or a scroll region does this per frame).
local function test_clip(n)
    local i = 1
    while i <= n do
        c:clip(4, 4, 100, 60)
        c:resetClip()
        i = i + 1
    end
    return n % 1000003
end

-- 15. Held keys: games poll this every frame, and it builds a fresh table.
local function test_keys(n)
    local s = 0
    local i = 1
    while i <= n do
        local keys = c:keys()
        s = s + #keys
        i = i + 1
    end
    return (s + n) % 1000003
end

-- 16. Canvas creation: handle table + metatable + off-screen Image. The canvas
--     is dropped right away (the buffer goes to the GC), like a screen a game
--     opens and closes.
local function test_new(n)
    local i = 1
    while i <= n do
        local fresh = graphics.canvas.new({ background = 0x101010, foreground = 0xFFFFFF })
        i = i + 1
    end
    return n % 1000003
end

-- 17. A whole game frame: clear, HUD text, twelve sprites (rect + circle)
--     and a paddle. This is the number to compare against a 33 ms budget.
local function draw_frame(t)
    c:clear()
    c:color(0x00FF88)
    c:text("SCORE 1234", 4, 2)
    local i = 1
    while i <= 12 do
        local x = (t * 3 + i * 17) % W
        local y = (t * 2 + i * 23) % H
        c:color((i % 6) * 0x222222)
        c:rect(x, y, 6, 6, true)
        c:circle(x + 3, y + 3, 2, false)
        i = i + 1
    end
    c:color(0xFFFFFF)
    c:rect(60, H - 20, 44, 6, true)
end

local function test_frame(n)
    local i = 1
    while i <= n do
        draw_frame(i)
        i = i + 1
    end
    return n % 1000003
end

local function frame_extra(n, ms)
    local fps = 0
    if ms > 0 then fps = n * 1000 / ms end
    return "fps=" .. fixed(fps)
end

local pass = 1
while pass <= passes do
    total_ms = 0
    tests = 0
    emit("== canvas bench (lua) " .. W .. "x" .. H .. " pass " .. pass .. " ==")
    run("native_call", 20000, 2000000, test_native_call)
    run("color", 10000, 1000000, test_color)
    run("clear", 200, 20000, test_clear)
    run("clear_rect", 2000, 200000, test_clear_rect)
    run("pixel", 2000, 200000, test_pixel)
    run("line", 2000, 200000, test_line)
    run("rect", 2000, 100000, test_rect)
    run("circle", 500, 50000, test_circle)
    run("polygon", 500, 50000, test_polygon)
    run("text", 1000, 100000, test_text)
    run("print", 200, 20000, test_print)
    run("metrics", 2000, 200000, test_metrics)
    run("state", 2000, 100000, test_state)
    run("clip", 2000, 100000, test_clip)
    run("keys", 2000, 200000, test_keys)
    run("new", 5, 40, test_new)
    run("frame", 20, 2000, test_frame, frame_extra)
    emit("total tests=" .. tests .. " ms=" .. total_ms)
    pass = pass + 1
end

emit("done tests=" .. tests .. " out=" .. out)

if not reportOk then
    print("canvas.lua: could not write the report, printed above only")
end
