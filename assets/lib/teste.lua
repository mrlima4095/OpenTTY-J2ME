local g = require("graphics.lua")

function load() 
    local state = os.getenv("ESTADO")

    if state == nil then
        main()
    elseif state == "PERGUNTAR" then
        quest()
    else 
        local text = os.getenv("TEXT_LUA")

        if text == nil then
            g.Alert("Você não digitou nada ainda!")
        else
            g.Alert(text)
        end
    end
end

function main()
    local tela = {
        title = "Lista em Lua",
        itens = {
            ["Ler algo"] = "execute set ESTADO = PERGUNTAR; lua teste.lua; true",
            ["Ver o que foi digitado"] = "execute set ESTADO = MOSTRAR; lua teste.lua; true"
        }
    }

    g.BuildList(tela)
end
function quest()
    os.execute("execute install nano; add quest.title=Perguntando; add quest.label=Digite algo; quest.key=TEXT_LUA; quest.cmd=exec unset ESTADO & lua teste.lua")
end

load()