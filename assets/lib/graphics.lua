local graphics = {}


function graphics.Alert(message)
	return exec("warn " .. message)
end

-- Screen MODs
function graphics.SetTitle(title)
	return exec("x11 title " .. message)
end
function graphics.SetTicker(text)
	return exec("x11 tick " .. text)
end

-- Screen Saving 
function graphics.SaveWindow(name)
	return exec("x11 set " .. name)
end
function graphics.DropWindow(name)
	return exec("x11 unset " .. name)
end
function graphics.LoadWindow(name)
	return exec("x11 load " .. name)
end

graphics.SetTicker("oooo")


return graphics