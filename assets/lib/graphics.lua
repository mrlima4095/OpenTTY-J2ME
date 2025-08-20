local graphics = {}



function graphics.Alert(title, content)
	return exec("warn " + title + " " + content)
end

function graphics.SetTitle(title)
	return exec("x11 title " + title)
end

-- Screen Saving 
function graphics.SaveWindow(name)
	return exec("x11 set " + name)
end
function graphics.DropWindow(name)
	return exec("x11 unset " + name)
end
function graphics.LoadWindow(name)
	return exec("x11 load " + name)
end


return graphics