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
    
    -- Desenhar formas geométricas
    g:drawRect(10, 60, 100, 50)  -- Retângulo
    g:fillArc(120, 60, 50, 50, 0, 360)  -- Círculo
    g:drawLine(180, 60, 230, 110)  -- Linha
    
    -- Desenhar um triângulo colorido
    graphics.setpalette(g, 255, 0, 0)  -- Vermelho
    g:fillTriangle(50, 130, 30, 170, 70, 170)
    
    graphics.setpalette(g, 0, 255, 0)  -- Verde
    g:fillTriangle(100, 130, 80, 170, 120, 170)
    
    graphics.setpalette(g, 0, 0, 255)  -- Azul
    g:fillTriangle(150, 130, 130, 170, 170, 170)
    
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

-- Handler para tecla liberada
function handlers.keyReleased(keyCode)
    local keyName = graphics.getKeyName(keyCode)
    print("Tecla liberada: " .. keyName)
end

-- Handler para toque na tela
function handlers.pointerPressed(x, y)
    handlers.lastTouch = {x = x, y = y}
    print("Toque na posição: " .. x .. ", " .. y)
    
    -- Adicionar um círculo no local do toque
    if not handlers.touchPoints then
        handlers.touchPoints = {}
    end
    table.insert(handlers.touchPoints, {x = x, y = y})
    
    -- Limitar a quantidade de pontos
    if #handlers.touchPoints > 10 then
        table.remove(handlers.touchPoints, 1)
    end
    
    handlers.paint(gx)
end

-- Handler para arrastar na tela
function handlers.pointerDragged(x, y)
    print("Arrastando para: " .. x .. ", " .. y)
    
    -- Adicionar ponto durante o arrasto
    if handlers.touchPoints then
        table.insert(handlers.touchPoints, {x = x, y = y})
        if #handlers.touchPoints > 20 then
            table.remove(handlers.touchPoints, 1)
        end
    end
    
    handlers.paint(gx)
end

-- Handler para toque liberado
function handlers.pointerReleased(x, y)
    print("Toque liberado em: " .. x .. ", " .. y)
end

-- Modificar o handler paint para desenhar os pontos de toque
local originalPaint = handlers.paint
function handlers.paint(g)
    -- Chamar a função original
    originalPaint(g)
    
    -- Desenhar pontos de toque
    if handlers.touchPoints then
        graphics.setpalette(g, 255, 0, 255)  -- Magenta
        for i, point in ipairs(handlers.touchPoints) do
            -- Círculos menores para pontos mais antigos
            local size = 10 - (i / 2)
            if size < 2 then size = 2 end
            g:fillArc(point.x - size/2, point.y - size/2, size, size, 0, 360)
        end
    end
end

-- Configurar os handlers no canvas
graphics.setCanvasHandler(canvas, handlers)

-- Adicionar um botão de comando
local cmdBack = graphics.new("command", {label = "Voltar", type = "back"})
graphics.addCommand(canvas, cmdBack)

-- Handler para comandos
local commandHandlers = {}
function commandHandlers[cmdBack](args)
    print("Voltando para tela anterior...")
    -- Aqui você poderia mudar para outra tela
end

graphics.handler(canvas, commandHandlers)

-- Exibir o canvas
graphics.display(canvas)

-- Exemplo de animação simples
print("Canvas criado! Experimente:")
print("1. Pressione teclas do teclado")
print("2. Toque na tela (se suportado)")
print("3. Arraste o dedo na tela")
print("4. Pressione a tecla FIRE para limpar")

-- Função para exemplo de animação automática
local function startAnimation()
    local angle = 0
    local centerX = 100
    local centerY = 100
    local radius = 30
    
    -- Adicionar propriedades para animação
    handlers.animationAngle = 0
    handlers.animationCenter = {x = centerX, y = centerY}
    handlers.animationRadius = radius
    
    -- Modificar o paint handler para incluir animação
    local superPaint = handlers.paint
    function handlers.paint(g)
        superPaint(g)
        
        -- Desenhar objeto animado
        if handlers.animationAngle then
            graphics.setpalette(g, 0, 255, 255)  -- Ciano
            local x = handlers.animationCenter.x + handlers.animationRadius * math.cos(handlers.animationAngle)
            local y = handlers.animationCenter.y + handlers.animationRadius * math.sin(handlers.animationAngle)
            g:fillArc(x - 10, y - 10, 20, 20, 0, 360)
            
            -- Atualizar ângulo para próxima frame
            handlers.animationAngle = handlers.animationAngle + 0.1
            if handlers.animationAngle > 2 * math.pi then
                handlers.animationAngle = handlers.animationAngle - 2 * math.pi
            end
            
            -- Solicitar repaint contínuo para animação
            os.execute("sleep 0.05")
            canvas:requestRepaint()
        end
    end
end

-- Perguntar se o usuário quer ver animação
print("\nDeseja iniciar animação automática? (Pressione '5' no teclado numérico)")

-- Adicionar handler específico para iniciar animação
local function onKey5(keyCode)
    local keyName = graphics.getKeyName(keyCode)
    if keyName == "5" then
        startAnimation()
        print("Animação iniciada!")
    end
end

-- Salvar handler original e criar novo
local originalKeyPressed = handlers.keyPressed
function handlers.keyPressed(keyCode)
    local keyName = graphics.getKeyName(keyCode)
    if keyName == "5" then
        onKey5(keyCode)
    else
        originalKeyPressed(keyCode)
    end
end