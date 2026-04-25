-- C.lua - C Language Interpreter Runtime for OpenTTY/Lua J2ME
-- Versão corrigida (sem erro de cast)

local C = {}

-- Token types
C.EOF = 0
C.IDENTIFIER = 1
C.CONSTANT = 2
C.STRING = 3
C.PUNCTUATOR = 4

-- Keywords
C.KEYWORDS = {
    ["auto"] = 256,
    ["break"] = 257,
    ["case"] = 258,
    ["char"] = 259,
    ["const"] = 260,
    ["continue"] = 261,
    ["default"] = 262,
    ["do"] = 263,
    ["double"] = 264,
    ["else"] = 265,
    ["enum"] = 266,
    ["extern"] = 267,
    ["float"] = 268,
    ["for"] = 269,
    ["goto"] = 270,
    ["if"] = 271,
    ["int"] = 272,
    ["long"] = 273,
    ["register"] = 274,
    ["return"] = 275,
    ["short"] = 276,
    ["signed"] = 277,
    ["sizeof"] = 278,
    ["static"] = 279,
    ["struct"] = 280,
    ["switch"] = 281,
    ["typedef"] = 282,
    ["union"] = 283,
    ["unsigned"] = 284,
    ["void"] = 285,
    ["volatile"] = 286,
    ["while"] = 287
}

-- Operators
C.OP = {
    inc = 300,
    dec = 301,
    left = 302,
    right = 303,
    le = 304,
    ge = 305,
    eq = 306,
    ne = 307,
    and_op = 308,
    or_op = 309,
    mul_assign = 310,
    div_assign = 311,
    mod_assign = 312,
    add_assign = 313,
    sub_assign = 314,
    left_assign = 315,
    right_assign = 316,
    and_assign = 317,
    xor_assign = 318,
    or_assign = 319,
    ptr = 320
}

-- Type constants
C.TY_VOID = 0
C.TY_INT = 1
C.TY_CHAR = 2
C.TY_LONG = 3
C.TY_FLOAT = 4
C.TY_DOUBLE = 5
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

-- CValue constructor
local function newCValue(type, value)
    return {type = type, value = value}
end

-- Converter CValue para numero
local function toNumber(val)
    if val == nil then
        return 0
    end
    if type(val) == "number" then
        return val
    end
    if type(val) == "table" then
        if val.type == C.TY_INT or val.type == C.TY_CHAR or val.type == C.TY_LONG then
            if type(val.value) == "number" then
                return val.value
            elseif type(val.value) == "string" then
                return tonumber(val.value) or 0
            else
                return 0
            end
        end
    end
    return tonumber(val) or 0
end

-- Converter para string
local function toString(val)
    if val == nil then
        return "nil"
    end
    if type(val) == "string" then
        return val
    end
    if type(val) == "number" then
        return tostring(val)
    end
    if type(val) == "table" then
        if val.type == C.TY_INT or val.type == C.TY_LONG then
            return tostring(toNumber(val))
        elseif val.type == C.TY_CHAR then
            return string.char(toNumber(val))
        elseif val.type == C.TY_PTR then
            if type(val.value) == "string" then
                return val.value
            else
                return tostring(val.value)
            end
        end
    end
    return tostring(val)
end

-- Truthy check
local function isTruthy(val)
    if val == nil then
        return false
    end
    if type(val) == "boolean" then
        return val
    end
    if type(val) == "number" then
        return val ~= 0
    end
    if type(val) == "table" then
        if val.type == C.TY_PTR then
            return val.value ~= nil
        end
        return toNumber(val) ~= 0
    end
    if type(val) == "string" then
        return #val > 0
    end
    return true
end

-- Tokenizer
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
            
        elseif c == "/" and i < len and string.sub(code, i+1, i+1) == "*" then
            i = i + 2
            while i + 1 <= len do
                if string.sub(code, i, i) == "*" and string.sub(code, i+1, i+1) == "/" then
                    i = i + 2
                    break
                end
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
                    elseif esc == "r" then
                        table.insert(s, "\r")
                    elseif esc == "\\" then
                        table.insert(s, "\\")
                    elseif esc == '"' then
                        table.insert(s, '"')
                    else
                        table.insert(s, esc)
                    end
                else
                    table.insert(s, ch)
                end
                i = i + 1
            end
            table.insert(tokens, {type = C.STRING, value = table.concat(s)})
            
        elseif c == "'" and i < len then
            i = i + 1
            local ch = string.sub(code, i, i)
            if ch == "\\" and i < len then
                i = i + 1
                local esc = string.sub(code, i, i)
                if esc == "n" then
                    ch = "\n"
                elseif esc == "t" then
                    ch = "\t"
                elseif esc == "r" then
                    ch = "\r"
                elseif esc == "0" then
                    ch = "\0"
                else
                    ch = esc
                end
            end
            i = i + 2
            if string.sub(code, i, i) == "'" then
                i = i + 1
            end
            table.insert(tokens, {type = C.CONSTANT, value = newCValue(C.TY_CHAR, string.byte(ch))})
            
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
            
        elseif i + 1 <= len then
            local op2 = string.sub(code, i, i+1)
            local optype = nil
            
            if op2 == "++" then optype = C.OP.inc
            elseif op2 == "--" then optype = C.OP.dec
            elseif op2 == "<<" then optype = C.OP.left
            elseif op2 == ">>" then optype = C.OP.right
            elseif op2 == "<=" then optype = C.OP.le
            elseif op2 == ">=" then optype = C.OP.ge
            elseif op2 == "==" then optype = C.OP.eq
            elseif op2 == "!=" then optype = C.OP.ne
            elseif op2 == "&&" then optype = C.OP.and_op
            elseif op2 == "||" then optype = C.OP.or_op
            elseif op2 == "+=" then optype = C.OP.add_assign
            elseif op2 == "-=" then optype = C.OP.sub_assign
            elseif op2 == "*=" then optype = C.OP.mul_assign
            elseif op2 == "/=" then optype = C.OP.div_assign
            elseif op2 == "->" then optype = C.OP.ptr
            end
            
            if optype then
                table.insert(tokens, {type = optype, value = op2})
                i = i + 2
            else
                local punct = nil
                if c == ";" then punct = ";"
                elseif c == "{" then punct = "{"
                elseif c == "}" then punct = "}"
                elseif c == "(" then punct = "("
                elseif c == ")" then punct = ")"
                elseif c == "[" then punct = "["
                elseif c == "]" then punct = "]"
                elseif c == "," then punct = ","
                elseif c == "=" then punct = "="
                elseif c == "+" then punct = "+"
                elseif c == "-" then punct = "-"
                elseif c == "*" then punct = "*"
                elseif c == "/" then punct = "/"
                elseif c == "%" then punct = "%"
                elseif c == "&" then punct = "&"
                elseif c == "|" then punct = "|"
                elseif c == "^" then punct = "^"
                elseif c == "~" then punct = "~"
                elseif c == "!" then punct = "!"
                elseif c == "<" then punct = "<"
                elseif c == ">" then punct = ">"
                end
                
                if punct then
                    table.insert(tokens, {type = punct, value = c})
                else
                    error("Unexpected character: " .. c)
                end
                i = i + 1
            end
        else
            i = i + 1
        end
    end
    
    table.insert(tokens, {type = C.EOF, value = "EOF"})
    return tokens
end

-- Função printf global (sem erro de cast)
function _G.printf(fmt, ...)
    local args = {...}
    local result = fmt
    local idx = 1
    
    -- Substitui %d, %i, %s, %c
    result = string.gsub(result, "%%[discc]", function(spec)
        local val = args[idx]
        idx = idx + 1
        
        if val == nil then
            return ""
        end
        
        -- Converte o valor para o tipo apropriado
        if spec == "%d" or spec == "%i" then
            return tostring(toNumber(val))
        elseif spec == "%s" then
            return toString(val)
        elseif spec == "%c" then
            return string.char(toNumber(val))
        end
        return toString(val)
    end)
    
    -- Substitui %%
    result = string.gsub(result, "%%%%", "%%")
    
    print(result)
    return #result
end

-- Compilador/Interpretador C simplificado
function C.compile(source)
    local tokens = C.tokenize(source)
    local pos = 1
    local globals = {}
    
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
        error("Expected " .. tostring(typ))
    end
    
    -- Expressão simples (apenas constantes, strings e printf)
    local function parseExpression()
        local t = peek()
        
        if t.type == C.IDENTIFIER then
            local name = consume().value
            
            if name == "printf" then
                expect("(")
                local fmtTok = peek()
                if fmtTok.type ~= C.STRING then
                    error("printf needs string format")
                end
                consume()
                local fmt = fmtTok.value
                
                local args = {}
                while peek().type == "," do
                    consume()
                    local argTok = peek()
                    if argTok.type == C.CONSTANT then
                        consume()
                        table.insert(args, argTok.value)
                    elseif argTok.type == C.STRING then
                        consume()
                        table.insert(args, argTok.value)
                    elseif argTok.type == C.IDENTIFIER then
                        local varName = consume().value
                        local sym = globals[varName]
                        table.insert(args, sym or 0)
                    else
                        break
                    end
                end
                expect(")")
                expect(";")
                
                -- Chama printf com os argumentos
                local out = string.gsub(fmt, "%%[discc]", function()
                    local arg = table.remove(args, 1)
                    if arg == nil then return "" end
                    if type(arg) == "table" then
                        return tostring(arg.value)
                    end
                    return tostring(arg)
                end)
                out = string.gsub(out, "%%%%", "%%")
                print(out)
                return newCValue(C.TY_INT, #out)
            else
                -- Variavel
                local sym = globals[name]
                if sym == nil then
                    sym = newCValue(C.TY_INT, 0)
                    globals[name] = sym
                end
                expect(";")
                return sym
            end
            
        elseif t.type == C.CONSTANT then
            local val = consume().value
            expect(";")
            return val
            
        elseif t.type == C.STRING then
            local str = consume().value
            expect(";")
            return newCValue(C.TY_PTR, str)
            
        else
            -- Pula statement desconhecido
            while peek().type ~= C.EOF and peek().type ~= ";" do
                consume()
            end
            if peek().type == ";" then
                consume()
            end
            return nil
        end
    end
    
    -- Parser principal
    local function parse()
        local lastResult = nil
        while peek().type ~= C.EOF do
            lastResult = parseExpression()
        end
        return lastResult
    end
    
    return parse()
end

-- Versão eval
function C.eval(source)
    return C.compile(source)
end

-- Exportar para o G
_G.C = C

return C