local graphics = {}

function graphics.Alert(title, content)
	return exec("warn " + title + " " + content)
end

function graphics.SetTitle(title)
	return exec("x11 title " + title)
end

function graphics.LoadScreen(name)
	return exec("x11 load " + name);
end

function graphics.SaveScreen(name)
	return exec("x11 set " + name);
end

function graphics.DropScreen(name)
	return exec("x11 unset " + name);
end

return graphics