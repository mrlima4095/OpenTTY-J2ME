local graphics = {}

graphics["version"] = "1.0"

function graphics.Alert(message) os.execute("warn " .. message) end
function graphics.Gauge(message) os.execute("x11 gauge " .. message) end

-- Screen MODs
function graphics.SetTitle(text) os.execute("title " .. text) end
function graphics.WindowTitle(text) os.execute("x11 title " .. text) end

function graphics.SetTicker(message) os.execute("x11 tick " .. message) end

-- Screen Stacking
function graphics.SaveWindow(name) return os.execute("x11 set " + name) end
function graphics.DropWindow(name) return os.execute("x11 unset " + name) end
function graphics.LoadWindow(name) return os.execute("x11 load " + name) end
function graphics.LoadProcess(name) return os.execute("x11 import " + name) end


return graphics