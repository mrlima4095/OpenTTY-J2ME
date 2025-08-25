local g = require("graphics.lua")

function load() 
    local state = os.getenv("ESTADO")

    if state == nil then
        main()
    elseif state == "PERGUNTAR" then
        quest()
    else 
        local text = os.getenv("TEXT_LUA")
        local _ = os.execute("execute unset ESTADO; lua teste.lua")
        
        g.Alert(text or "Você não digitou nada ainda!")
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
function quest() os.execute("execute install nano; add quest.title=Perguntando; add quest.label=Digite algo; add quest.key=TEXT_LUA; add quest.cmd=exec unset ESTADO & lua teste.lua") end

load()