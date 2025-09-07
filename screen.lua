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
