local function split(text, sep)
    local result, buffer, cur = {}, "", 1
    for i = 1, string.len(text) do
        local char = string.sub(text, i, i)
        if char == sep then
            result[cur] = buffer
            cur = cur + 1
            buffer = ""
        else
            buffer = buffer .. char
        end
    end
    result[cur] = buffer
    return result
end

local function join(tbl, sep)
    local buf = ""
    for i = 1, #tbl do
        buf = buf .. tbl[i]
        if i < #tbl then buf = buf .. sep end
    end
    return buf
end

local app = {}
app.db = "/home/.tasks"
app.tasks = {}

-- Salvar para disco
function app.save()
    io.write(join(app.tasks, "\n"), app.db, "w")
end

-- Carregar banco
function app.load()
    local content = io.read(app.db) or ""
    if content == "" then return {} end
    app.tasks = split(content, "\n")
    return app.tasks
end

-- Normalizar tarefas: [ ] texto
local function normalize(task)
    if string.sub(task, 1, 3) ~= "[ ]" and string.sub(task, 1, 3) ~= "[x]" then
        return "[ ] " .. task
    end
    return task
end

-- Alternar status
function app.toggle(index)
    local task = app.tasks[index]
    if not task then return end
    if string.sub(task, 1, 3) == "[ ]" then
        app.tasks[index] = "[x]" .. string.sub(task, 4)
    else
        app.tasks[index] = "[ ]" .. string.sub(task, 4)
    end
    app.save()
    app.main()
end

-- Adicionar nova tarefa
function app.add()
    graphics.display(graphics.BuildQuest({
        title = "Nova tarefa",
        label = "Digite:",
        key = "newtask",
        back = { root = app.main },
        button = { label = "Salvar", root = function(args)
            local txt = args and args[1] or ""
            if txt ~= "" then
                table.insert(app.tasks, normalize(txt))
                app.save()
            end
            app.main()
        end }
    }))
end

-- Exportar / importar
function app.export()
    graphics.display(graphics.BuildEdit({
        title = "Export tasks",
        back = { root = app.main },
        button = { label = "OK", root = function() app.main() end }
    }))
end

function app.import()
    graphics.display(graphics.BuildEdit({
        title = "Import tasks",
        back = { root = app.main },
        button = { label = "Importar", root = function(args)
            local txt = args and args[1] or ""
            if txt ~= "" then
                app.tasks = split(txt, "\n")
                for i = 1, #app.tasks do
                    app.tasks[i] = normalize(app.tasks[i])
                end
                app.save()
            end
            app.main()
        end }
    }))
end

-- Menu principal
function app.menu()
    graphics.display(graphics.BuildList({
        title = "Menu",
        back = { root = app.main },
        fields = {
            "Nova tarefa",
            "Exportar",
            "Importar"
        },
        button = { label = "Selecionar", root = function(args)
            local choice = args and args[1]
            if choice == "Nova tarefa" then app.add()
            elseif choice == "Exportar" then app.export()
            elseif choice == "Importar" then app.import()
            end
        end }
    }))
end

-- Listagem principal
function app.main()
    app.load()
    graphics.display(graphics.BuildList({
        title = "To Do",
        back = { root = os.exit },
        type = "multiple",
        fields = app.tasks,
        button = { label = "Menu", root = app.menu }
    }))
end

-- Handler de seleção da lista principal (toggle status)
function app.handler(args)
    if not args then return end
    for i = 1, #args do
        for j = 1, #app.tasks do
            if app.tasks[j] == args[i] then
                app.toggle(j)
            end
        end
    end
end

-- Processo
os.setproc("name", "tasks")
app.main()
