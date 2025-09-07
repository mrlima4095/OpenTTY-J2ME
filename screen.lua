local screen = {
    title = "Window Title",

    back = {
        label = "Label for Back",
        root = function () print("") end,
    },

    button = {
        label = "Label for Button",
        root = function () print("") end
    },

    fields = {
        { type = "text", value = "Text for Field", style="default" },
        { type = "image", img = "/image/path" },
        { type = "item", label = "Label for ITEM", cmd = "execute echo ITEM; true" },
        { type = "spacer", h = 1, w = 10 }
    }
}
local list = {
    title = "List Title",

    back = {
        label = "Label for Back",
        root = function () print("") end,
    },

    button = {
        label = "Label for Button",
        root = function (item) print("") end
    },

    fields = { "Item 1", "Item 2", "Item 3", "Item 4", "Item 5" }
}

local quest = {
    title = "Quest Title",
    label = "Label for Quest",
    key = "",

    back = {
        label = "Label for Back",
        root = function () print("") end,
    },

    button = {
        label = "Label for Button",
        root = function (content) print("") end
    },
}

local edit = {
    title = "Edit Title",
    key = "",

    back = {
        label = "Label for Back",
        root = function () print("") end,
    },

    button = {
        label = "Label for Button",
        root = function (content) print("") end
    },
}

graphics.display(graphics.screen({ title = "Window Title", back = { label = "Retorne!", root = function () os.execute("execute warn Não ha volta!!!") print(random()) os.exit(1) end }, button = { label = "Avance", root = function () os.execute("execute @alert; warn Só regresso pra você!") end }, fields = { { type = "spacer", h = 20, w = 20 }, { type = "text", value = "Texto de teste", style="bols" }, { type = "image", img = "/java/etc/icons/app.png" } } }))
graphics.display(graphics.list({ title = "List Title", back = { label = "Voltar", root = function () os.execute("@alert") os.exit(1) end, }, button = { label = "Escolher", root = function (item) os.execute("warn Você escolheu: " .. item) os.exit(1) end }, fields = { 12, nil, true } }))
