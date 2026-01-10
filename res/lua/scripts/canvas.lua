-- Exemplo de uso do LuaCanvas
-- Cria uma tela interativa com desenhos e controles

-- Primeiro, vamos criar um canvas
local canvas = graphics.new("canvas", "Meu Canvas Interativo", false)
local gx
-- Criar uma tabela de handlers para os eventos
local handlers = {}

-- Handler para pintar na tela
function handlers.paint(g)
    gx = g
    -- Configurar cor de fundo (preto)
    graphics.setpalette(g, 0, 0, 0)
    g:fillRect(0, 0, g:getWidth(), g:getHeight())
    
    -- Configurar cor para desenhos (branco)
    graphics.setpalette(g, 255, 255, 255)
    
    -- Desenhar texto no centro da tela
    g:drawString("LuaCanvas Demo", 50, 20, graphics.LEFT + graphics.TOP)
    g:drawString("Pressione teclas ou toque na tela", 10, 40, graphics.LEFT + graphics.TOP)
    
    -- Desenhar informações dinâmicas (se disponíveis)
    if handlers.lastKey then
        graphics.setpalette(g, 255, 255, 0)  -- Amarelo
        g:drawString("Última tecla: " .. handlers.lastKey, 10, 180, graphics.LEFT + graphics.TOP)
    end
    
    if handlers.lastTouch then
        g:drawString("Último toque: " .. handlers.lastTouch.x .. "," .. handlers.lastTouch.y, 10, 200, graphics.LEFT + graphics.TOP)
    end
end

-- Handler para tecla pressionada
function handlers.keyPressed(keyCode)
    local keyName = graphics.getKeyName(keyCode)
    handlers.lastKey = keyName
    print("Tecla pressionada: " .. keyName .. " (" .. keyCode .. ")")
    
    -- Exemplo: FIRE (tecla central) limpa a tela
    if keyCode == -5 then  -- FIRE key
        handlers.lastKey = nil
        handlers.lastTouch = nil
    end
    
    -- Forçar repaint
    handlers.paint(gx)
end

local previous = graphics.getCurrent()
local back = graphics.new("command", { label = "Back", type = "ok", priority = 1 })
local vibrate = graphics.new("command", { label = "Vibrate", type = "ok", priority = 1 })

graphics.addCommand(canvas, back)
graphics.addCommand(canvas, vibrate)
graphics.handler(canvas, {
    [back] = function ()
        graphics.display(previous)
        os.exit(0)
    end,
    [vibrate] = function ()
        graphics.vibrate(1200)
    end
})

graphics.display(canvas)