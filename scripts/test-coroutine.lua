-- Run with: lua scripts/test-coroutine.lua
local thread = coroutine.create(function(value)
    local next, note = coroutine.yield(value + 1, "first yield")
    return next + 1, note
end)

assert(type(thread) == "thread")
assert(coroutine.status(thread) == "suspended")

local ok, value, note = coroutine.resume(thread, 7)
assert(ok and value == 8 and note == "first yield")
assert(coroutine.status(thread) == "suspended")

ok, value, note = coroutine.resume(thread, 20, "completed")
assert(ok and value == 21 and note == "completed")
assert(coroutine.status(thread) == "dead")

local wrapped = coroutine.wrap(function(value)
    local next = coroutine.yield(value * 2)
    return next * 3
end)

assert(wrapped(4) == 8)
assert(wrapped(5) == 15)

local failed = coroutine.create(function()
    error("expected coroutine error")
end)
ok, note = coroutine.resume(failed)
assert(not ok and type(note) == "string")

print("coroutine test: OK")
