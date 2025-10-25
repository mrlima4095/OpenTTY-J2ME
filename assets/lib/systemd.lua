#!/bin/lua

local app = {
    screen = {
        title = "Services",
        back = { label = "Back", root = os.exit },
        button = { label = "Menu" },

        fields = {}
    }
}

local function split(text, sep)
    if not text then return {} end
    sep = sep or "\n"
    local res = {}
    local cur = 1
    local buf = ""
    for i = 1, #text do
        local c = text:sub(i,i)
        if c == sep then
            res[cur] = buf
            cur = cur + 1
            buf = ""
        else buf = buf .. c end
    end

    res[cur] = buf
    return res
end


function app.menu(service)
    graphics.display(graphics.BuildList({
        title = "Menu",
        back = { label = "Back", root = app.main },
        button = {
            label = "Select",
            root = function (opt)
                if opt == "Start" then
                    app.handle_no_services(service)
                    os.execute("start " .. service)
                elseif opt == "---" then app.menu(service)
                elseif opt == "New" then app.new_service()
                elseif opt == "Remove" then app.remove(service)
                elseif opt == "Clear all" then app.clear()
                end
            end
        },

        fields = { "Start", "---", "New", "Remove", "Clear all" }
    }))
end

function app.handle_no_services(service) if service == "[ No Services ]" then app.main() os.execute("warn There is no services") end end

function app.new_service()
    graphics.display(graphics.BuildScreen({
        title = "New Service",
        back = { label = "Back", root = app.main },
        button = {
            label = "Create",
            root = function (name, loader, collector)
                if name == "" then app.new_service() os.execute("warn There is fields missing!") end

                local db = io.read("/etc/services")
                if string.find(db, name) then
                    local cache = split(db, "\n")
                    for k,v in pairs(cache) do if string.sub(v, 1, #name) == name then cache[k] = name .. "=" .. loader .. "," .. collector end end

                    io.write(table.concat(cache, "\n"), "/etc/services")
                else
                    io.write(db .. "\n" .. name .. "=" .. loader .. "," .. collector, "/etc/services")
                end

                app.main()
            end
        }
    }))
end
function app.remove(service)
    graphics.display(graphics.Alert({
        title = "Services",
        message = "Service '" .. service .. "' will be removed! Are you sure?",
        back = { label = "No", root = app.menu(service) },
        button = {
            label = "Yes",
            root = function ()
                local db = io.read("/etc/services")
                if string.find(db, service) then
                    local cache = split(db, "\n")
                    for k,v in pairs(cache) do if string.sub(v, 1, #service) == service then table.remove(cache, k) end end

                    io.write(table.concat(cache, "\n"), "/etc/services")
                end

                app.main()
            end
        }
    }))
end
function app.clear()
    graphics.display(graphics.Alert({
        title = "Services",
        message = "All Services will be removed! Are you sure?",
        back = { label = "No", root = app.main() },
        button = {
            label = "Yes",
            root = function () io.write("", "/etc/services") app.main() end
        }
    }))
end

function app.init_screen()
    local services, db = {}, table.decode(io.read("/etc/services"))

    if #db == 0 then services = { "[ No Services ]" }
    else
        for k,v in pairs(db) do
            table.insert(services, k)
        end
    end
    app.screen.button.root = app.menu
    app.screen.fields = services
end

function app.main() app.init_screen() graphics.display(graphics.BuildList(app.screen)) end


os.setproc("name", "sysctl")

if os.execute("case user (root) false") == 255 then app.main()
else print("Permission denied!") os.exit(13) end