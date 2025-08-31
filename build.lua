local app = {
    name = "appname",
    version = "1.0",
    description = "",

    api = {
        version = "1.16",
        error = function () os.execute("") end,
        match = "minimum"
    },

    process = {
        name = "pname",
        exit = "execute echo Lua app have been killed",
        type = "deamon",
        host = "10141",
        db = "luapp"
    },

    include = {},

    build = function () os.execute("") end,
    commands = { name = "cmd alias" },

    shell = {
        name = "cmd",
        args = { },
        unknown = ""
    },

    file = { }
}

return app