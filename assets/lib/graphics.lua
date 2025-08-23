local graphics = {}

graphics["version"] = "1.0"

function graphics.Alert(message) os.exec("warn " .. message) end
function graphics.Gauge(message) os.exec("x11 gauge " .. message) end

-- Screen MODs
function graphics.SetTitle(text) os.exec("title " .. text) end
function graphics.WindowTitle(text) os.exec("x11 title " .. text) end

function graphics.SetTicker(message) os.exec("x11 tick " .. message) end

-- Screen Stacking
function graphics.SaveWindow(name) return os.execute("x11 set " + name) end
function graphics.DropWindow(name) return os.execute("x11 unset " + name) end
function graphics.LoadWindow(name) return os.execute("x11 load " + name) end
function graphics.LoadProcess(name) return os.execute("x11 import " + name) end


function graphics.List(config)
	local title, itens = config["title"], config["itens"]
	local buffer = "";

	os.exec("execute install .graphics-nano-bkp")
	os.exec("add list.title=" .. title)

	for k,v in pairs(config["itens"])
		buffer = buffer .. "," .. 

	os.exec("execute x11 list nano; get .graphics-nano-bkp")


return graphics