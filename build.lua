local app = {
    name = "OpenTTY Lua Application",
    version = "1.0",
    description = ""

    api = {
        version = "1.16",
        error = "execute echo Invalid OpenTTY API for Application",
        match = "minimum"
    },

    include = { "", "" }

    process = {
        name = "mycoolapp",
        exit = "execute echo Lua App have been killed",

        mod = "execute echo loop", -- or LuaFunction
    }
}