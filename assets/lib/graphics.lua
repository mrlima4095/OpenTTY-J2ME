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
	local file, content, buffer = "", "", ""
	local function append(text, key) if config[key] ~= nil then file = file .. "\n" .. text .. "=" .. config[key] end end

	append("list.title", "title")
	append("list.button", "label")
	append("list.back", "back")
	append("list.icon", "icon")

	local itens = config.itens
	if itens ~= nil then
		for k,v in pairs(itens) do
			if buffer == "" then 
			    buffer = k .. "=" .. v 
			else 
			    buffer = buffer .. "\n" .. k .. "=" .. v 
			end
			
			if content == "" then
			    content = k
			else
			    content = content .. "," .. k
			end
		end
	else
		error("missing List itens") 
	end

	file = file .. "\n" .. buffer .. "\n" .. "list.content=" .. content

	os.exec("x11 list -e " .. file)
end

return graphics 