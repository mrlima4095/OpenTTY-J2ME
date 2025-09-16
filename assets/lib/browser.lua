local browser = {
    version = "1.0",

    xback = os.exit,
    xmenucfg = {
        title = "Browser",
        back = { root = os.exit },
        button = function (opt)

        end
    },

    headers = {}
}

function browser.render(raw)
    
    return raw
end
function browser.load(url)
    local raw, status = socket.http.get(url, browser.headers)

    if status ~= 200 then raw = "<title>404 - Not found</title>\nPage not found" end

    
end

function browser.quest()
    graphics.display(graphics.BuildQuest({
        title = "Browser",
        label = "WebSite URL",
        content = "http://",
        back = { root = browser.main },
        button = { label = "Go", root = browser.load }
    }))
end

function browser.main()
    if browser.menu == nil then browser.menu = graphics.BuildList(browser.menu) end

    graphics.display(browser.menu)
end



os.setproc("name", "browser")
browser.quest()
