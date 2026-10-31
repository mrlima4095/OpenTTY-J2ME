-- rock: name=ui
-- rock: version=1.0.1
-- rock: description=Callback-based menus, prompts, confirms, alerts and a file picker
-- rock: here=/lib/ui.lua

-- LCDUI is event driven: the runtime has no blocking `wait`, so these helpers
-- take callbacks instead of returning the user's answer. Each function builds
-- the screen, wires the commands, displays it and returns the screen handle.
--
--   local ui = require("ui")
--   ui.menu("Main", { "Open", "Save", "Quit" }, {
--       on_select = function(label, index) print(label, index) end,
--       on_cancel = function() print("cancelled") end
--   })
local U = ui or {}

local function label_of(item)
    if type(item) == "table" then
        if item.label ~= nil then return tostring(item.label) end
        if item[1] ~= nil then return tostring(item[1]) end
        return tostring(item)
    end
    return tostring(item)
end

local function previous()
    local ok, screen = pcall(graphics.getCurrent)
    if ok then return screen end
    return nil
end

local function back_command(label)
    return graphics.new("command", { label = label or "Back", type = "back", priority = 2 })
end

local function ok_command(label)
    return graphics.new("command", { label = label or "OK", type = "ok", priority = 1 })
end

-- `items` may be strings or tables with `label` (and any extra fields, handed
-- back as the third argument of `on_select`).
function U.menu(title, items, options)
    options = options or {}
    local list = graphics.new("list", title or "Menu", options.mode or "implicit")

    for i = 1, #items do graphics.append(list, label_of(items[i])) end

    local select = ok_command(options.select_label or "Select")
    local cancel = back_command(options.cancel_label or "Cancel")
    graphics.addCommand(list, select)
    graphics.addCommand(list, cancel)

    graphics.handler(list, {
        [select] = function(selected)
            if options.on_select == nil then return end
            local index
            for i = 1, #items do
                if label_of(items[i]) == selected then index = i break end
            end
            options.on_select(selected, index, index ~= nil and items[index] or nil)
        end,
        [cancel] = function()
            if options.on_cancel ~= nil then options.on_cancel() end
        end
    })

    graphics.display(list)
    return list
end

-- One editable text field. `on_submit` receives the typed string.
function U.prompt(title, label, options)
    options = options or {}
    local form = graphics.new("screen", title or "Input")
    graphics.append(form, {
        type = "field",
        label = label or "Value",
        value = options.value or "",
        length = options.length or 256,
        mode = options.mode or "any"
    })

    local submit = ok_command(options.ok_label or "OK")
    local cancel = back_command(options.cancel_label or "Cancel")
    graphics.addCommand(form, submit)
    graphics.addCommand(form, cancel)

    graphics.handler(form, {
        [submit] = function(value)
            if options.on_submit ~= nil then options.on_submit(value, form) end
        end,
        [cancel] = function()
            if options.on_cancel ~= nil then options.on_cancel() end
        end
    })

    graphics.display(form)
    return form
end

U.input = U.prompt

function U.confirm(title, text, options)
    options = options or {}
    local alert = graphics.new("alert", title or "Confirm", text or "",
        { type = "confirm", timeout = options.timeout or 0 })

    local yes = ok_command(options.yes_label or "Yes")
    local no = back_command(options.no_label or "No")
    graphics.addCommand(alert, yes)
    graphics.addCommand(alert, no)

    graphics.handler(alert, {
        [yes] = function()
            if options.on_yes ~= nil then options.on_yes() end
        end,
        [no] = function()
            if options.on_no ~= nil then options.on_no() end
        end
    })

    graphics.display(alert)
    return alert
end

function U.alert(title, text, options)
    options = options or {}
    local alert = graphics.new("alert", title or "Notice", text or "",
        { type = options.type or "info", timeout = options.timeout or 0 })
    graphics.display(alert)
    return alert
end

function U.notify(text, options)
    options = options or {}
    return U.alert(options.title or "Notice", text, {
        type = options.type or "info",
        timeout = options.timeout or 1500
    })
end

-- A non-interactive gauge inside an alert. Returns a handle with `set(value)`,
-- `inc(step)` and `close()` (restores the screen that was current before).
function U.progress(title, text, options)
    options = options or {}
    local before = options.previous or previous()
    local max = options.max or 100

    local alert = graphics.new("alert", title or "Please wait", text or "",
        { type = options.type or "info", timeout = 0 })
    local gauge = graphics.new("gauge", {
        label = options.label or "",
        interactive = false,
        maxValue = max,
        value = options.value or 0
    })
    graphics.SetIndicator(alert, gauge)
    graphics.display(alert)

    local handle = {
        screen = alert,
        gauge = gauge,
        max = max,
        value = options.value or 0
    }

    handle.set = function(self, value)
        self.value = value
        graphics.SetGauge(self.gauge, value)
        return self
    end

    handle.inc = function(self, step)
        return self.set(self, self.value + (step or 1))
    end

    handle.close = function(self)
        if before ~= nil then graphics.display(before) end
        return self
    end

    return handle
end

local function join(path, name)
    if string.sub(path, -1) == "/" then return path .. name end
    return path .. "/" .. name
end

local function parent(path)
    if path == "/" or path == "" then return "/" end
    local trimmed = path
    if string.sub(trimmed, -1) == "/" then trimmed = string.sub(trimmed, 1, -2) end
    local slash = #trimmed
    while slash > 1 do
        if string.sub(trimmed, slash, slash) == "/" then
            return string.sub(trimmed, 1, slash - 1)
        end
        slash = slash - 1
    end
    return "/"
end

-- Browses a directory with `io.dirs`. Entries ending in "/" are directories
-- and open in place; files call `on_select(full_path)`. `on_cancel(path)`
-- receives the directory that was open.
function U.filepicker(options)
    options = options or {}
    local start = options.start or "/mnt"

    local function show(path)
        local list = graphics.new("list", options.title or path, "implicit")
        if path ~= "/" then graphics.append(list, "../") end

        local entries = io.dirs(path)
        local names = {}
        if entries ~= nil then
            for i = 1, #entries do names[#names + 1] = entries[i] end
            table.sort(names)
        end

        for i = 1, #names do
            local name = names[i]
            if options.filter == nil or options.filter(name, path) then
                graphics.append(list, name)
            end
        end

        local select = ok_command(options.select_label or "Select")
        local cancel = back_command(options.cancel_label or "Cancel")
        graphics.addCommand(list, select)
        graphics.addCommand(list, cancel)

        graphics.handler(list, {
            [select] = function(selected)
                if selected == "../" then
                    show(parent(path))
                elseif string.sub(selected, -1) == "/" then
                    show(join(path, string.sub(selected, 1, -2)))
                else
                    local full = join(path, selected)
                    if options.on_select ~= nil then options.on_select(full) end
                    if options.close_on_select and options.previous ~= nil then
                        graphics.display(options.previous)
                    end
                end
            end,
            [cancel] = function()
                if options.on_cancel ~= nil then options.on_cancel(path) end
            end
        })

        graphics.display(list)
        return list
    end

    return show(start)
end

return U
