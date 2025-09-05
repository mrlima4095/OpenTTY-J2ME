os = {
    execute = function(command) end, -- Executa comando no sistema
    getenv = function(varname) end, -- Retorna variável de ambiente
    clock = function() end,          -- Retorna tempo em ms desde o início
    setlocale = function(locale) end,-- Define localidade
    exit = function(code) end        -- Encerra o programa com código
}

package = {
    loadlib = function(libname) end, -- Carrega biblioteca externa
    loaded = {}                      -- Cache de módulos carregados
}

io = {
    read = function(target) end,    -- Lê dados de arquivo ou stream
    write = function(data, target, mode) end, -- Escreve dados em arquivo ou stream
    close = function(stream) end    -- Fecha stream ou conexão
}

table = {
    pack = function(...) end,       -- Empacota argumentos em tabela
    decode = function(str) end      -- Decodifica string em tabela
}

socket = {
    connect = function(address) end,-- Abre conexão de socket
    http = {
        get = function(url) end,    -- Requisição HTTP GET
        post = function(url, data) end -- Requisição HTTP POST
    }
}

string = {
    upper = function(str) end,
    lower = function(str) end,
    len = function(str) end,
    match = function(str, pattern) end,
    reverse = function(str) end,
    sub = function(str, start, end_) end,
    hash = function(str) end,
    byte = function(str, start, end_) end,
    char = function(...) end
}

function print(...) end
function error(msg) end
function pcall(func, ...) end
function require(modname) end
function load(str) end
function pairs(t) end
function collectgarbage() end
function tostring(val) end
function tonumber(val) end
function select(index, ...) end
function type(val) end
