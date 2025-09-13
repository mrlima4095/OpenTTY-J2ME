--[[

name=BoxME
version=1.0
description=Virtual Machines

api.version=1.16
api.error=execute log add error BoxME requires OpenTTY API 1.16; echo [ BoxME ] Loading failed!; echo [ BoxME ] See logs to more info.;
api.match=minimum

process.name=boxhost2
process.exit=execute log add BoxME have been killed; echo [ BoxME ] Host service killed; true

command=boxme
boxme=lua boxme

]]

local host = os.getpid("boxhost2")

local function menu()
    graphics.display(graphics.BuildList({
        title = "BoxME",
        back = { root = os.exit },
        button = {
            label = "Select",
            root = function (option)
                
            end
        },

        fields = { "New Machine", "Start a Machine", "Settings", "About" }
    }))
end

local function main()
    if host == nil then error("BoxME service isn't running!") end

    local state = os.getproc(host, "state")

    if state == nil then
        menu()
    end
end

main()