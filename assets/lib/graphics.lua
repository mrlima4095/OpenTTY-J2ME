local graphics = {}

graphics["version"] = "1.0"

function graphics.Alert(message) return exec("warn " .. message) end
function graphics.Gauge(message) return exec("x11 gauge" .. message) end

-- Screen MODs
function graphics.SetTitle(text) return exec("title " .. text) end
function graphics.WindowTitle(text) return exec("x11 title" .. text) end

function graphics.SetTicker(message) return exec("x11 tick " .. text) end



return graphics