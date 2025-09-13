--[[
View tasks
Task status
Change task status
Set timeout
Export and Import tasks


]]

local function split(text, sep)
    local result, buffer, cur = {}, "", 1

    for i = 1, string.len(text) do
        local char = string.sub(text, i, i)

        if char == sep then
            result[cur] = buffer
            cur = cur + 1
            buffer = ""
        else buffer = buffer .. char end
    end

    result[cur] = buffer

    return result
end

local app = {}
app.db = "/home/.tasks"

function app.load()
    local content = io.read(app.db)

    app.tasks = split(content, "\n")
end


function app.main()
    graphics.display(graphics.BuildList({
        title = "To Do",
        back = { root = os.exit },
        button = { label = "Menu", root = app.handler },
        type = "multiple",

        fields = app.tasks
    }))
end

function app.handler(...)

end

os.setproc("name", "tasks")
app.load()
app.main()