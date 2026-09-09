-- Run with: lua scripts/test-debug.lua
local function makeClosure()
    local prefix = "before:"
    return function(value)
        return prefix .. value
    end
end

local closure = makeClosure()
local info = debug.getinfo(closure)
assert(info.what == "Lua")
assert(info.nparams == 1)

local name, value = debug.getupvalue(closure, 1)
assert(name == "prefix" and value == "before:")
assert(debug.setupvalue(closure, 1, "after:") == "prefix")
assert(closure("value") == "after:value")

local target = {}
local metatable = { label = "debug metatable" }
assert(debug.setmetatable(target, metatable) == target)
assert(debug.getmetatable(target).label == "debug metatable")
assert(debug.getregistry().debug == debug)

local function nestedTraceback()
    local trace = debug.traceback("debug test")
    assert(type(trace) == "string")
    assert(string.find(trace, "stack traceback"))
    local frame = debug.getinfo(1)
    assert(frame.what == "Lua")
end

nestedTraceback()
print("debug test: OK")
