local function build(app) 
    if app == nil then error("bad app - got nil") end
    if type(app) ~= "table" then error("bad app - need to be table") end

    local api, process, depends = app["api"], app["process"], app["depends"]
    local config, cmds = app["config"], app["commands"]

    if api ~= nil then
        local version = os.getenv("VERSION");
        local required, match, raise = api["version"], api["match"], api["error"];

        if version ~= nil then
            local bad = false

            if match == "exact-prefix" then 

            elseif match == "exact-full" then 
                bad = required == version
            elseif match == "minimum" then
                
            elseif match == "maximum" then

            else error("build: invalid api match tool") end
        end
    end
end

local template = {
    name = "Lua App",
    version = "1.0",
    description = "Build Template",

    api = {
        version = "1.16", -- OpenTTY required version
        error = "execute echo Incompatible OpenTTY API", -- Command to run when called in a invalid OpenTTY version
        match = "minimum" -- exact-prefix / minimum / maximum / exact-full
    },

    depends = { "" }, -- Build another Lua Apps as dependencies of this

    process = {
        name = "luapp", -- App process name
        exit = "execute echo Killed", -- Command to run when the process have been killed
        mod = "execute echo looping", -- If this key have been found, run it in looping
    },

    config = "execute echo Loading...", -- Command to run when build the Lua App
    commands = {
        ["my-cmd"] = "execute echo You ran the command", -- normal alias
        ["my-shell"] = {
            sub1 = "execute echo my-shell: you reached sub command 1" -- shells
        }
    }
}

return build