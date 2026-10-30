-- rock: name=log
-- rock: version=1.0.0
-- rock: description=Levelled loggers with timestamps, colours and file/stdout sinks
-- rock: here=/lib/log.lua

-- `/bin/log` covers MIDlet logs; this is for applications. A logger writes a
-- timestamped line to stdout, to a file (`file = "/tmp/app.log"`, appended) or
-- to any function sink.
--
--   local log = require("log")
--   local l = log.new({ name = "app", level = "debug" })
--   l:info("started", 3, "workers")
local L = log or {}

local LEVELS = { debug = 10, info = 20, warn = 30, error = 40, none = 100 }
L.LEVELS = LEVELS
L.loggers = {}

local ESC = string.char(27)
local COLORS = { debug = "90", info = "36", warn = "33", error = "31" }

local Logger = {}
Logger.__index = Logger

local function now_text()
    return os.date("%Y-%m-%d %H:%M:%S")
end

function Logger.format(self, level, message)
    local parts = {}
    if self.timestamps then parts[#parts + 1] = now_text() end
    parts[#parts + 1] = "[" .. self.name .. "]"
    local label = string.upper(level)
    if self.color then
        label = ESC .. "[" .. (COLORS[level] or "0") .. "m" .. label .. ESC .. "[0m"
    end
    parts[#parts + 1] = label
    parts[#parts + 1] = message
    return table.concat(parts, " ")
end

function Logger.emit(self, level, message)
    local line = Logger.format(self, level, message)
    if #self.sinks > 0 then
        for i = 1, #self.sinks do self.sinks[i](line, level, self.name) end
    elseif self.file ~= nil then
        io.write(line .. "\n", self.file, "a")
    else
        print(line)
    end
    return line
end

function Logger.write(self, level, values)
    local parts = {}
    for i = 1, #values do parts[i] = tostring(values[i]) end
    return Logger.emit(self, level, table.concat(parts, " "))
end

function Logger.debug(self, ...)
    if self.level > LEVELS.debug then return nil end
    local first = ...
    local total = select("#", ...)
    if type(first) == "table" and total == #first then return Logger.write(self, "debug", first) end
    local values = {}
    for i = 1, total do values[i] = select(i, ...) end
    return Logger.write(self, "debug", values)
end

function Logger.info(self, ...)
    if self.level > LEVELS.info then return nil end
    local first = ...
    local total = select("#", ...)
    if type(first) == "table" and total == #first then return Logger.write(self, "info", first) end
    local values = {}
    for i = 1, total do values[i] = select(i, ...) end
    return Logger.write(self, "info", values)
end

function Logger.warn(self, ...)
    if self.level > LEVELS.warn then return nil end
    local first = ...
    local total = select("#", ...)
    if type(first) == "table" and total == #first then return Logger.write(self, "warn", first) end
    local values = {}
    for i = 1, total do values[i] = select(i, ...) end
    return Logger.write(self, "warn", values)
end

function Logger.error(self, ...)
    if self.level > LEVELS.error then return nil end
    local first = ...
    local total = select("#", ...)
    if type(first) == "table" and total == #first then return Logger.write(self, "error", first) end
    local values = {}
    for i = 1, total do values[i] = select(i, ...) end
    return Logger.write(self, "error", values)
end

function Logger.log(self, level, ...)
    local numeric = LEVELS[tostring(level)]
    if numeric == nil then numeric = LEVELS.info end
    if self.level > numeric then return nil end
    local first = ...
    local total = select("#", ...)
    if type(first) == "table" and total == #first then return Logger.write(self, tostring(level), first) end
    local values = {}
    for i = 1, total do values[i] = select(i, ...) end
    return Logger.write(self, tostring(level), values)
end

function Logger.enabled(self, level)
    return self.level <= (LEVELS[tostring(level)] or LEVELS.info)
end

function Logger.set_level(self, level)
    local name = tostring(level)
    self.level_name = name
    self.level = LEVELS[name] or LEVELS.info
    return self
end

function Logger.set_file(self, path)
    self.file = path
    return self
end

function Logger.add_sink(self, sink)
    self.sinks[#self.sinks + 1] = sink
    return self
end

function Logger.child(self, name)
    local child = L.new({
        name = name or (self.name .. ".child"),
        level = self.level_name,
        file = self.file,
        color = self.color,
        timestamps = self.timestamps
    })
    child.sinks = self.sinks
    return child
end

function L.new(options)
    if type(options) == "string" then options = { name = options } end
    options = options or {}
    local logger = setmetatable({}, Logger)
    logger.name = options.name or "app"
    logger.level_name = options.level or "info"
    logger.level = LEVELS[logger.level_name] or LEVELS.info
    logger.file = options.file
    logger.color = options.color and true or false
    logger.timestamps = options.timestamps
    if logger.timestamps == nil then logger.timestamps = true end
    logger.sinks = {}
    if options.sink ~= nil then logger.sinks[1] = options.sink end
    L.loggers[logger.name] = logger
    return logger
end

function L.get(name)
    local logger = L.loggers[name]
    if logger ~= nil then return logger end
    return L.new({ name = name })
end

function L.set_level(level)
    for _, logger in pairs(L.loggers) do logger:set_level(level) end
end

return L
