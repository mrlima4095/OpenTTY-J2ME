-- C.lua - C Language Interpreter Runtime for OpenTTY/Lua J2ME
-- Versão SEM dependência do printf do Java

local C = {}

-- Token types
C.EOF = 0
C.IDENTIFIER = 1
C.CONSTANT = 2
C.STRING = 3

-- Keywords
C.KEYWORDS = {
    ["int"] = 272,
    ["return"] = 275,
    ["if"] = 271,
    ["else"] = 265,
    ["while"] = 287,
    ["for"] = 269,
    ["break"] = 257,
    ["continue"] = 261
}

-- Type constants
C.TY_INT = 1
C.TY_CHAR = 2
C.TY_PTR = 6

-- Character helpers
local function isSpace(c)
    return c == " " or c == "\t" or c == "\n" or c == "\r"
end

local function isDigit(c)
    return c >= "0" and c <= "9"
end

local function isLetter(c)
    return (c >= "A" and c <= "Z") or (c >= "a" and c <= "z") or c == "_"
end

local function isAlnum(c)
    return isDigit(c) or isLetter(c)
end

-- CValue
local function newCValue(typ, val)
    return {type = typ, value = val}
end

local function toInt(v)
    if v == nil then return 0 end
    if type(v) == "number" then return v end
    if type(v) == "table" then
        if type(v.value) == "number" then return v.value end
        return 0
    end
    if type(v) == "string" then
        local n = tonumber(v)
        if n then return n else return 0 end
    end
    return 0
end

local function toString(v)
    if v == nil then return "" end
    if type(v) == "string" then return v end
    if type(v) == "number" then return tostring(v) end
    if type(v) == "table" then
        if v.type == C.TY_PTR and type(v.value) == "string" then
            return v.value
        end
        return tostring(toInt(v))
    end
    return tostring(v)
end

local function isTruthy(v)
    if v == nil then return false end
    if type(v) == "boolean" then return v end
    if type(v) == "number" then return v ~= 0 end
    if type(v) == "table" then
        if v.type == C.TY_PTR then return v.value ~= nil end
        return toInt(v) ~= 0
    end
    return true
end

-- Printf em Lua puro (NAO usa o printf do Java)
local function lua_printf(fmt, ...)
    local args = {...}
    local result = ""
    local i = 1
    local len = string.len(fmt)
    local argIdx = 1
    
    while i <= len do
        local c = string.sub(fmt, i, i)
        
        if c == "%" and i < len then
            local spec = string.sub(fmt, i+1, i+1)
            i = i + 2
            
            local val = args[argIdx]
            argIdx = argIdx + 1
            
            if spec == "d" or spec == "i" then
                result = result .. tostring(toInt(val))
            elseif spec == "s" then
                result = result .. toString(val)
            elseif spec == "c" then
                result = result .. string.char(toInt(val))
            elseif spec == "%" then
                result = result .. "%"
            else
                result = result .. "%" .. spec
            end
        else
            result = result .. c
            i = i + 1
        end
    end
    
    print(result)
    return #result
end

-- Substitui o printf global para NAO usar o Java
_G.printf = lua_printf

-- Tokenizer simplificado
function C.tokenize(code)
    local tokens = {}
    local i = 1
    local len = string.len(code)
    
    while i <= len do
        local c = string.sub(code, i, i)
        
        if isSpace(c) then
            i = i + 1
            
        elseif c == "/" and i < len and string.sub(code, i+1, i+1) == "/" then
            i = i + 2
            while i <= len and string.sub(code, i, i) ~= "\n" do
                i = i + 1
            end
            
        elseif c == '"' then
            i = i + 1
            local s = {}
            while i <= len do
                local ch = string.sub(code, i, i)
                if ch == '"' then
                    i = i + 1
                    break
                elseif ch == "\\" and i < len then
                    i = i + 1
                    local esc = string.sub(code, i, i)
                    if esc == "n" then
                        table.insert(s, "\n")
                    elseif esc == "t" then
                        table.insert(s, "\t")
                    else
                        table.insert(s, esc)
                    end
                else
                    table.insert(s, ch)
                end
                i = i + 1
            end
            table.insert(tokens, {type = C.STRING, value = table.concat(s)})
            
        elseif isLetter(c) then
            local s = {}
            while i <= len and isAlnum(string.sub(code, i, i)) do
                table.insert(s, string.sub(code, i, i))
                i = i + 1
            end
            local word = table.concat(s)
            local kw = C.KEYWORDS[word]
            if kw then
                table.insert(tokens, {type = kw, value = word})
            else
                table.insert(tokens, {type = C.IDENTIFIER, value = word})
            end
            
        elseif isDigit(c) then
            local s = {}
            while i <= len and isDigit(string.sub(code, i, i)) do
                table.insert(s, string.sub(code, i, i))
                i = i + 1
            end
            local num = table.concat(s)
            table.insert(tokens, {type = C.CONSTANT, value = newCValue(C.TY_INT, tonumber(num))})
            
        elseif c == ";" then
            table.insert(tokens, {type = ";", value = ";"})
            i = i + 1
        elseif c == "(" then
            table.insert(tokens, {type = "(", value = "("})
            i = i + 1
        elseif c == ")" then
            table.insert(tokens, {type = ")", value = ")"})
            i = i + 1
        elseif c == "{" then
            table.insert(tokens, {type = "{", value = "{"})
            i = i + 1
        elseif c == "}" then
            table.insert(tokens, {type = "}", value = "}"})
            i = i + 1
        elseif c == "=" then
            table.insert(tokens, {type = "=", value = "="})
            i = i + 1
        elseif c == "+" then
            table.insert(tokens, {type = "+", value = "+"})
            i = i + 1
        elseif c == "-" then
            table.insert(tokens, {type = "-", value = "-"})
            i = i + 1
        elseif c == "*" then
            table.insert(tokens, {type = "*", value = "*"})
            i = i + 1
        elseif c == "/" then
            table.insert(tokens, {type = "/", value = "/"})
            i = i + 1
        elseif c == "," then
            table.insert(tokens, {type = ",", value = ","})
            i = i + 1
        else
            i = i + 1
        end
    end
    
    table.insert(tokens, {type = C.EOF, value = "EOF"})
    return tokens
end

-- Compilador/Interpretador C
function C.compile(source)
    local tokens = C.tokenize(source)
    local pos = 1
    local globals = {}
    local doreturn = false
    local returnValue = nil
    
    local function peek()
        local t = tokens[pos]
        if t == nil then return {type = C.EOF} end
        return t
    end
    
    local function consume()
        local t = tokens[pos]
        if t then pos = pos + 1 end
        if t == nil then return {type = C.EOF} end
        return t
    end
    
    local function expect(typ)
        local t = peek()
        if t.type == typ then
            return consume()
        end
        error("Expected " .. tostring(typ) .. " got " .. tostring(t.type))
    end
    
    -- Expressão aritmética simples
    local function parseExpr()
        local left = peek()
        
        if left.type == C.CONSTANT then
            consume()
            return left.value
        elseif left.type == C.IDENTIFIER then
            local name = consume().value
            local sym = globals[name]
            if sym == nil then
                sym = newCValue(C.TY_INT, 0)
                globals[name] = sym
            end
            return sym
        elseif left.type == C.STRING then
            local str = consume().value
            return newCValue(C.TY_PTR, str)
        else
            return newCValue(C.TY_INT, 0)
        end
    end
    
    -- Statement
    local function parseStatement()
        local t = peek()
        
        if t.type == C.KEYWORDS["int"] then
            consume() -- int
            local name = expect(C.IDENTIFIER).value
            
            -- Verifica se tem inicializacao = valor
            local val = newCValue(C.TY_INT, 0)
            if peek().type == "=" then
                consume() -- =
                local expr = parseExpr()
                val = expr
            end
            expect(";")
            
            globals[name] = val
            return nil
            
        elseif t.type == C.IDENTIFIER and t.value == "printf" then
            consume() -- printf
            expect("(")
            local fmtTok = expect(C.STRING)
            local fmt = fmtTok.value
            
            local args = {}
            while peek().type == "," do
                consume() -- ,
                local arg = parseExpr()
                table.insert(args, arg)
            end
            expect(")")
            expect(";")
            
            -- Executa printf
            local out = ""
            local argIdx = 1
            local i = 1
            local flen = string.len(fmt)
            
            while i <= flen do
                local ch = string.sub(fmt, i, i)
                if ch == "%" and i < flen then
                    local spec = string.sub(fmt, i+1, i+1)
                    i = i + 2
                    
                    local argVal = args[argIdx]
                    argIdx = argIdx + 1
                    
                    if spec == "d" or spec == "i" then
                        out = out .. tostring(toInt(argVal))
                    elseif spec == "s" then
                        out = out .. toString(argVal)
                    elseif spec == "c" then
                        out = out .. string.char(toInt(argVal))
                    elseif spec == "%" then
                        out = out .. "%"
                    else
                        out = out .. "%" .. spec
                    end
                else
                    out = out .. ch
                    i = i + 1
                end
            end
            
            print(out)
            return newCValue(C.TY_INT, #out)
            
        elseif t.type == C.KEYWORDS["return"] then
            consume() -- return
            local val = nil
            if peek().type ~= ";" then
                val = parseExpr()
            end
            expect(";")
            doreturn = true
            returnValue = val
            return val
            
        elseif t.type == ";" then
            consume()
            return nil
            
        else
            -- Expressao qualquer
            local expr = parseExpr()
            if peek().type == ";" then
                consume()
            end
            return expr
        end
    end
    
    -- Parse all
    local function parse()
        while peek().type ~= C.EOF do
            local res = parseStatement()
            if doreturn then
                return returnValue
            end
        end
        return nil
    end
    
    return parse()
end

function C.eval(source)
    return C.compile(source)
end

-- Export
_G.C = C

return C