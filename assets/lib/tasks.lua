-- Task manager (uso sem modificar OpenTTY.java)
local function split(text, sep)
    if not text then return {} end
    sep = sep or "\n"
    local res = {}
    local cur = 1
    local buf = ""
    for i = 1, #text do
        local c = text:sub(i,i)
        if c == sep then
            res[cur] = buf
            cur = cur + 1
            buf = ""
        else buf = buf .. c end
    end
    -- último buffer (pode ser vazio)
    res[cur] = buf
    return res
end

local function trim(s) return (s or ""):gsub("^%s*(.-)%s*$", "%1") end
local function join(tbl, sep)
    sep = sep or "\n"
    local out = ""
    for i = 1, (#tbl or 0) do
        if i > 1 then out = out .. sep end
        out = out .. (tbl[i] or "")
    end
    return out
end

local app = {}
app.db = "/home/.tasks"   -- arquivo: linhas "0|Comprar pão"

-- Parse/serialização de linha
function app.parse_line(line)
    if not line then return {status = 0, text = ""} end
    local parts = split(line, "|")
    local status = tonumber(parts[1]) or 0
    local text = parts[2] or ""
    for i = 3, #parts do text = text .. "|" .. parts[i] end
    return { status = status, text = text }
end
function app.line_of(task)
    return tostring(task.status) .. "|" .. task.text
end

function app.load()
    local content = io.read(app.db) or ""
    local lines = split(content, "\n")
    app.tasks = {}
    for i = 1, #lines do
        local l = lines[i]
        if l and l ~= "" then app.tasks[#app.tasks + 1] = app.parse_line(l) end
    end
    return app.tasks
end

function app.save()
    local out = {}
    for i = 1, #app.tasks do out[#out + 1] = app.line_of(app.tasks[i]) end
    io.write(join(out, "\n"), app.db)
end

-- Constrói tabela de "fields" para BuildList.
-- Cada label mostrado é bonito ("[x] texto"), mas concatenamos "\tINDEX" no final
-- para que o handler consiga extrair o índice (solução sem mexer em Java).
function app.getFields()
    local fields = {}
    for i = 1, #app.tasks do
        local t = app.tasks[i]
        local label = (t.status == 1 and "[x] " or "[ ] ") .. t.text .. "\t" .. tostring(i)
        fields[#fields + 1] = label
    end
    return fields
end

function app.refresh()
    graphics.display(graphics.BuildList({
        title = "To Do",
        back = { root = os.exit },
        button = { label = "Menu", root = app.handler },
        type = "multiple",
        fields = app.getFields()
    }))
end

function app.toggle_by_index(idx)
    if not app.tasks[idx] then return end
    app.tasks[idx].status = (app.tasks[idx].status == 1) and 0 or 1
end

-- Handler: recebe um ou vários valores (strings). cada valor tem formato "<label>\t<INDEX>"
-- extraímos o INDEX e alternamos status
function app.handler(...)
    local args = {...}
    if #args == 0 then return end
    for i = 1, #args do
        local v = args[i] or ""
        local parts = split(v, "\t")
        local rawIndex = parts[#parts] or ""
        local idx = tonumber(trim(rawIndex))
        if idx then app.toggle_by_index(idx) end
    end
    app.save()
    app.refresh()
end

-- util: adicionar tarefa (pode chamar app.add("texto"))
function app.add(text)
    text = trim(text or "")
    if text == "" then return end
    app.tasks[#app.tasks + 1] = { status = 0, text = text }
    app.save()
    app.refresh()
end

-- import/export simples
function app.export(path)
    local out = {}
    for i = 1, #app.tasks do out[#out + 1] = app.line_of(app.tasks[i]) end
    io.write(join(out, "\n"), path)
end
function app.import(path)
    local content = io.read(path) or ""
    local lines = split(content, "\n")
    for i = 1, #lines do
        local l = lines[i]
        if l and l ~= "" then app.tasks[#app.tasks + 1] = app.parse_line(l) end
    end
    app.save()
    app.refresh()
end

-- bootstrap
app.load()
app.refresh()
