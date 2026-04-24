-- C.lua - C Language Interpreter Runtime for OpenTTY/Lua J2ME
-- Sem self, sem :, compatível com J2ME

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

-- Character helpers para J2ME
local function isSpace(c)
    return c == " " or c == "\t" or c == "\n" or c == "\r" or c == "\f"
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

-- CValue wrapper (sem metatabela, funcoes normais)
local function newCValue(type, value)
    return {type = type, value = value}
end

local function cvalue_asInt(v)
    if v == nil then return 0 end
    if type(v.value) == "number" then
        return v.value
    elseif type(v.value) == "boolean" then
        if v.value then return 1 else return 0 end
    elseif type(v.value) == "string" then
        local n = tonumber(v.value)
        if n then return n else return 0 end
    else
        return 0
    end
end

local function cvalue_asString(v)
    if v == nil then return "nil" end
    if v.type == C.TY_INT or v.type == C.TY_LONG then
        return tostring(cvalue_asInt(v))
    elseif v.type == C.TY_CHAR then
        return string.char(cvalue_asInt(v))
    elseif v.type == C.TY_PTR then
        if type(v.value) == "string" then
            return '"' .. v.value .. '"'
        else
            return "ptr:" .. tostring(v.value)
        end
    else
        return tostring(v.value)
    end
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
            
        -- Comment //
        elseif c == "/" and i < len and string.sub(code, i+1, i+1) == "/" then
            i = i + 2
            while i <= len and string.sub(code, i, i) ~= "\n" do
                i = i + 1
            end
            
        -- Comment /*
        elseif c == "/" and i < len and string.sub(code, i+1, i+1) == "*" then
            i = i + 2
            while i + 1 <= len do
                if string.sub(code, i, i) == "*" and string.sub(code, i+1, i+1) == "/" then
                    i = i + 2
                    break
                end
                i = i + 1
            end
            
        -- String literal
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
                    elseif esc == "0" then
                        table.insert(s, "\0")
                    else
                        table.insert(s, esc)
                    end
                else
                    table.insert(s, ch)
                end
                i = i + 1
            end
            table.insert(tokens, {type = C.STRING, value = table.concat(s)})
            
        -- Char constant
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
                elseif esc == "\\" then
                    ch = "\\"
                elseif esc == "'" then
                    ch = "'"
                else
                    ch = esc
                end
            end
            i = i + 2
            if string.sub(code, i, i) == "'" then i = i + 1 end
            table.insert(tokens, {type = C.CONSTANT, value = newCValue(C.TY_CHAR, string.byte(ch))})
            
        -- Identifier or keyword
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
            
        -- Number constant
        elseif isDigit(c) or (c == "." and i < len and isDigit(string.sub(code, i+1, i+1))) then
            local s = {}
            local isFloat = false
            
            while i <= len do
                local ch = string.sub(code, i, i)
                if isDigit(ch) then
                    table.insert(s, ch)
                    i = i + 1
                elseif ch == "." and not isFloat then
                    isFloat = true
                    table.insert(s, ch)
                    i = i + 1
                elseif (ch == "e" or ch == "E") and not isFloat and i < len then
                    isFloat = true
                    table.insert(s, ch)
                    i = i + 1
                    local sign = string.sub(code, i, i)
                    if sign == "+" or sign == "-" then
                        table.insert(s, sign)
                        i = i + 1
                    end
                else
                    break
                end
            end
            
            local num = table.concat(s)
            local val
            if isFloat then
                val = newCValue(C.TY_DOUBLE, tonumber(num))
            else
                val = newCValue(C.TY_INT, tonumber(num))
            end
            table.insert(tokens, {type = C.CONSTANT, value = val})
            
        -- Multi-character operators
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
            elseif op2 == "*=" then optype = C.OP.mul_assign
            elseif op2 == "/=" then optype = C.OP.div_assign
            elseif op2 == "%=" then optype = C.OP.mod_assign
            elseif op2 == "+=" then optype = C.OP.add_assign
            elseif op2 == "-=" then optype = C.OP.sub_assign
            elseif op2 == "<<=" then optype = C.OP.left_assign
            elseif op2 == ">>=" then optype = C.OP.right_assign
            elseif op2 == "&=" then optype = C.OP.and_assign
            elseif op2 == "^=" then optype = C.OP.xor_assign
            elseif op2 == "|=" then optype = C.OP.or_assign
            elseif op2 == "->" then optype = C.OP.ptr
            end
            
            if optype then
                table.insert(tokens, {type = optype, value = op2})
                i = i + 2
            else
                -- Single character punctuators
                local punct = nil
                if c == ";" then punct = ";"
                elseif c == "{" then punct = "{"
                elseif c == "}" then punct = "}"
                elseif c == "(" then punct = "("
                elseif c == ")" then punct = ")"
                elseif c == "[" then punct = "["
                elseif c == "]" then punct = "]"
                elseif c == "," then punct = ","
                elseif c == "?" then punct = "?"
                elseif c == ":" then punct = ":"
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
                elseif c == "." then punct = "."
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

-- Truthy check
local function isTruthy(val)
    if val == nil then return false end
    if type(val) == "boolean" then return val end
    if type(val) == "table" then
        if val.type == C.TY_PTR then
            return val.value ~= nil
        end
        return cvalue_asInt(val) ~= 0
    end
    if type(val) == "number" then return val ~= 0 end
    if type(val) == "string" then return string.len(val) > 0 end
    return val ~= nil
end

-- Parser and execution
function C.compile(source)
    local tokens = C.tokenize(source)
    local pos = 1
    local breakLoop = false
    local doreturn = false
    local loopDepth = 0
    local globals = {}
    local labels = {}
    
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
        else
            error("Expected " .. tostring(typ) .. " got " .. tostring(t.type))
        end
    end
    
    -- Forward declare
    local parseExpression
    
    -- Primary
    local function parsePrimary()
        local t = peek()
        
        if t.type == C.IDENTIFIER then
            consume()
            local sym = globals[t.value]
            if sym == nil then
                error("undefined symbol: " .. t.value)
            end
            return sym
        
        elseif t.type == C.CONSTANT then
            consume()
            return t.value
        
        elseif t.type == C.STRING then
            consume()
            return newCValue(C.TY_PTR, t.value)
        
        elseif t.type == "(" then
            consume()
            local expr = parseExpression()
            expect(")")
            return expr
        
        else
            error("expected primary expression")
        end
    end
    
    -- Unary
    local function parseUnary()
        local t = peek()
        
        if t.type == C.OP.inc then
            consume()
            local expr = parseUnary()
            local val = cvalue_asInt(expr)
            expr.value = val + 1
            return newCValue(C.TY_INT, val)
        
        elseif t.type == C.OP.dec then
            consume()
            local expr = parseUnary()
            local val = cvalue_asInt(expr)
            expr.value = val - 1
            return newCValue(C.TY_INT, val)
        
        elseif t.type == "*" then
            consume()
            local expr = parseUnary()
            if expr.type == C.TY_PTR then
                return expr.value
            end
            error("indirection requires pointer")
        
        elseif t.type == "&" then
            consume()
            local expr = parseUnary()
            return newCValue(C.TY_PTR, expr)
        
        elseif t.type == "+" then
            consume()
            return parseUnary()
        
        elseif t.type == "-" then
            consume()
            local expr = parseUnary()
            return newCValue(C.TY_INT, -cvalue_asInt(expr))
        
        elseif t.type == "~" then
            consume()
            local expr = parseUnary()
            return newCValue(C.TY_INT, bit32.bnot(cvalue_asInt(expr)))
        
        elseif t.type == "!" then
            consume()
            local expr = parseUnary()
            return not isTruthy(expr)
        
        else
            return parsePrimary()
        end
    end
    
    -- Multiplicative
    local function parseMul()
        local left = parseUnary()
        
        while true do
            local t = peek()
            if t.type == "*" then
                consume()
                local right = parseUnary()
                left = newCValue(C.TY_INT, cvalue_asInt(left) * cvalue_asInt(right))
            elseif t.type == "/" then
                consume()
                local right = parseUnary()
                local rv = cvalue_asInt(right)
                if rv == 0 then error("division by zero") end
                left = newCValue(C.TY_INT, math.floor(cvalue_asInt(left) / rv))
            elseif t.type == "%" then
                consume()
                local right = parseUnary()
                local rv = cvalue_asInt(right)
                if rv == 0 then error("modulo by zero") end
                left = newCValue(C.TY_INT, cvalue_asInt(left) % rv)
            else
                break
            end
        end
        
        return left
    end
    
    -- Additive
    local function parseAdd()
        local left = parseMul()
        
        while true do
            local t = peek()
            if t.type == "+" then
                consume()
                local right = parseMul()
                left = newCValue(C.TY_INT, cvalue_asInt(left) + cvalue_asInt(right))
            elseif t.type == "-" then
                consume()
                local right = parseMul()
                left = newCValue(C.TY_INT, cvalue_asInt(left) - cvalue_asInt(right))
            else
                break
            end
        end
        
        return left
    end
    
    -- Shift
    local function parseShift()
        local left = parseAdd()
        
        while true do
            local t = peek()
            if t.type == C.OP.left then
                consume()
                local right = parseAdd()
                left = newCValue(C.TY_INT, bit32.lshift(cvalue_asInt(left), cvalue_asInt(right)))
            elseif t.type == C.OP.right then
                consume()
                local right = parseAdd()
                left = newCValue(C.TY_INT, bit32.rshift(cvalue_asInt(left), cvalue_asInt(right)))
            else
                break
            end
        end
        
        return left
    end
    
    -- Relational
    local function parseRel()
        local left = parseShift()
        
        while true do
            local t = peek()
            if t.type == "<" then
                consume()
                local right = parseShift()
                left = cvalue_asInt(left) < cvalue_asInt(right)
            elseif t.type == ">" then
                consume()
                local right = parseShift()
                left = cvalue_asInt(left) > cvalue_asInt(right)
            elseif t.type == C.OP.le then
                consume()
                local right = parseShift()
                left = cvalue_asInt(left) <= cvalue_asInt(right)
            elseif t.type == C.OP.ge then
                consume()
                local right = parseShift()
                left = cvalue_asInt(left) >= cvalue_asInt(right)
            else
                break
            end
        end
        
        return left
    end
    
    -- Equality
    local function parseEq()
        local left = parseRel()
        
        while true do
            local t = peek()
            if t.type == C.OP.eq then
                consume()
                local right = parseRel()
                left = cvalue_asInt(left) == cvalue_asInt(right)
            elseif t.type == C.OP.ne then
                consume()
                local right = parseRel()
                left = cvalue_asInt(left) ~= cvalue_asInt(right)
            else
                break
            end
        end
        
        return left
    end
    
    -- Bitwise AND
    local function parseAnd()
        local left = parseEq()
        
        while peek().type == "&" do
            consume()
            local right = parseEq()
            left = newCValue(C.TY_INT, bit32.band(cvalue_asInt(left), cvalue_asInt(right)))
        end
        
        return left
    end
    
    -- Bitwise XOR
    local function parseXor()
        local left = parseAnd()
        
        while peek().type == "^" do
            consume()
            local right = parseAnd()
            left = newCValue(C.TY_INT, bit32.bxor(cvalue_asInt(left), cvalue_asInt(right)))
        end
        
        return left
    end
    
    -- Bitwise OR
    local function parseOr()
        local left = parseXor()
        
        while peek().type == "|" do
            consume()
            local right = parseXor()
            left = newCValue(C.TY_INT, bit32.bor(cvalue_asInt(left), cvalue_asInt(right)))
        end
        
        return left
    end
    
    -- Logical AND
    local function parseLAnd()
        local left = parseOr()
        
        while peek().type == C.OP.and_op do
            consume()
            local right = parseOr()
            left = isTruthy(left) and isTruthy(right)
        end
        
        return left
    end
    
    -- Logical OR
    local function parseLOr()
        local left = parseLAnd()
        
        while peek().type == C.OP.or_op do
            consume()
            local right = parseLAnd()
            left = isTruthy(left) or isTruthy(right)
        end
        
        return left
    end
    
    -- Conditional
    local function parseConditional()
        local cond = parseLOr()
        
        if peek().type == "?" then
            consume()
            local trueExpr = parseExpression()
            expect(":")
            local falseExpr = parseConditional()
            if isTruthy(cond) then
                return trueExpr
            else
                return falseExpr
            end
        end
        
        return cond
    end
    
    -- Assignment
    local function parseAssignment()
        local left = parseConditional()
        local t = peek()
        
        if t.type == "=" then
            consume()
            local right = parseAssignment()
            if type(left) == "table" then
                left.value = right
                if right and right.type then
                    left.type = right.type
                end
            end
            return right
        
        elseif t.type == C.OP.add_assign then
            consume()
            local right = parseAssignment()
            local val = cvalue_asInt(left) + cvalue_asInt(right)
            left.value = val
            return newCValue(C.TY_INT, val)
        
        elseif t.type == C.OP.sub_assign then
            consume()
            local right = parseAssignment()
            local val = cvalue_asInt(left) - cvalue_asInt(right)
            left.value = val
            return newCValue(C.TY_INT, val)
        
        elseif t.type == C.OP.mul_assign then
            consume()
            local right = parseAssignment()
            local val = cvalue_asInt(left) * cvalue_asInt(right)
            left.value = val
            return newCValue(C.TY_INT, val)
        
        elseif t.type == C.OP.div_assign then
            consume()
            local right = parseAssignment()
            local rv = cvalue_asInt(right)
            if rv == 0 then error("division by zero") end
            local val = math.floor(cvalue_asInt(left) / rv)
            left.value = val
            return newCValue(C.TY_INT, val)
        
        elseif t.type == C.OP.mod_assign then
            consume()
            local right = parseAssignment()
            local rv = cvalue_asInt(right)
            if rv == 0 then error("modulo by zero") end
            local val = cvalue_asInt(left) % rv
            left.value = val
            return newCValue(C.TY_INT, val)
        end
        
        return left
    end
    
    function parseExpression()
        return parseAssignment()
    end
    
    -- Compound statement
    local function parseCompound()
        expect("{")
        while peek().type ~= "}" do
            local res = parseStatement()
            if doreturn then
                return res
            end
        end
        expect("}")
        return nil
    end
    
    -- If statement
    local function parseIf()
        expect(C.KEYWORDS["if"])
        expect("(")
        local cond = parseExpression()
        expect(")")
        
        local ifBody = parseStatement()
        local elseBody = nil
        
        if peek().type == C.KEYWORDS["else"] then
            consume()
            elseBody = parseStatement()
        end
        
        if isTruthy(cond) then
            return ifBody
        else
            return elseBody
        end
    end
    
    -- While statement
    local function parseWhile()
        expect(C.KEYWORDS["while"])
        expect("(")
        local condPos = pos
        local cond = parseExpression()
        expect(")")
        
        -- Capture body
        local bodyStart = pos
        local depth = 1
        local bodyTokens = {}
        
        while depth > 0 do
            local tk = peek()
            if tk.type == "{" then
                depth = depth + 1
                table.insert(bodyTokens, consume())
            elseif tk.type == "}" then
                depth = depth - 1
                if depth > 0 then
                    table.insert(bodyTokens, consume())
                else
                    consume()
                end
            elseif tk.type == C.EOF then
                error("unmatched while")
            else
                table.insert(bodyTokens, consume())
            end
        end
        
        local result = nil
        loopDepth = loopDepth + 1
        
        while isTruthy(cond) do
            if breakLoop then
                breakLoop = false
                break
            end
            
            local savedPos = pos
            local savedTokens = tokens
            tokens = bodyTokens
            pos = 1
            
            while peek().type ~= C.EOF do
                result = parseStatement()
                if doreturn then break end
            end
            
            pos = savedPos
            tokens = savedTokens
            if doreturn then break end
            
            pos = condPos
            cond = parseExpression()
            pos = bodyStart
        end
        
        loopDepth = loopDepth - 1
        return result
    end
    
    -- For statement
    local function parseFor()
        expect(C.KEYWORDS["for"])
        expect("(")
        
        if peek().type ~= ";" then
            parseExpression()
        end
        expect(";")
        
        local condPos = pos
        local cond = nil
        if peek().type ~= ";" then
            cond = parseExpression()
        end
        expect(";")
        
        local incTokens = {}
        if peek().type ~= ")" then
            while peek().type ~= ")" do
                table.insert(incTokens, consume())
            end
        end
        expect(")")
        
        local bodyStart = pos
        local depth = 1
        local bodyTokens = {}
        
        while depth > 0 do
            local tk = peek()
            if tk.type == "{" then
                depth = depth + 1
                table.insert(bodyTokens, consume())
            elseif tk.type == "}" then
                depth = depth - 1
                if depth > 0 then
                    table.insert(bodyTokens, consume())
                else
                    consume()
                end
            elseif tk.type == C.EOF then
                error("unmatched for")
            else
                table.insert(bodyTokens, consume())
            end
        end
        
        local result = nil
        loopDepth = loopDepth + 1
        
        while true do
            if breakLoop then
                breakLoop = false
                break
            end
            
            if cond then
                pos = condPos
                cond = parseExpression()
                if not isTruthy(cond) then
                    break
                end
            end
            
            local savedPos = pos
            local savedTokens = tokens
            tokens = bodyTokens
            pos = 1
            
            while peek().type ~= C.EOF do
                result = parseStatement()
                if doreturn then break end
            end
            
            pos = savedPos
            tokens = savedTokens
            if doreturn then break end
            
            if #incTokens > 0 then
                local incPos = pos
                pos = 1
                tokens = incTokens
                while peek().type ~= C.EOF do
                    parseExpression()
                end
                pos = incPos
                tokens = savedTokens
            end
        end
        
        loopDepth = loopDepth - 1
        return result
    end
    
    -- Break
    local function parseBreak()
        if loopDepth == 0 then
            error("break outside loop")
        end
        expect(C.KEYWORDS["break"])
        expect(";")
        breakLoop = true
        return nil
    end
    
    -- Continue
    local function parseContinue()
        if loopDepth == 0 then
            error("continue outside loop")
        end
        expect(C.KEYWORDS["continue"])
        expect(";")
        breakLoop = true
        return nil
    end
    
    -- Return
    local function parseReturn()
        expect(C.KEYWORDS["return"])
        doreturn = true
        
        if peek().type ~= ";" then
            local expr = parseExpression()
            expect(";")
            return expr
        end
        
        expect(";")
        return nil
    end
    
    -- Statement dispatcher
    local function parseStatement()
        local t = peek()
        
        if t.type == "{" then
            return parseCompound()
        elseif t.type == C.KEYWORDS["if"] then
            return parseIf()
        elseif t.type == C.KEYWORDS["while"] then
            return parseWhile()
        elseif t.type == C.KEYWORDS["for"] then
            return parseFor()
        elseif t.type == C.KEYWORDS["break"] then
            return parseBreak()
        elseif t.type == C.KEYWORDS["continue"] then
            return parseContinue()
        elseif t.type == C.KEYWORDS["return"] then
            return parseReturn()
        elseif t.type == ";" then
            consume()
            return nil
        else
            local expr = parseExpression()
            expect(";")
            return expr
        end
    end
    
    -- Main parse
    local function parse()
        local result = nil
        while peek().type ~= C.EOF do
            result = parseStatement()
            if doreturn then
                break
            end
        end
        return result
    end
    
    return parse()
end

-- Printf function
local function printf(fmt, ...)
    local args = {...}
    local result = fmt
    local idx = 1
    
    result = string.gsub(result, "%%d", function()
        local v = args[idx]
        idx = idx + 1
        if type(v) == "table" then
            return tostring(cvalue_asInt(v))
        end
        return tostring(v or 0)
    end)
    
    result = string.gsub(result, "%%s", function()
        local v = args[idx]
        idx = idx + 1
        if type(v) == "table" then
            return cvalue_asString(v)
        end
        return tostring(v or "")
    end)
    
    result = string.gsub(result, "%%c", function()
        local v = args[idx]
        idx = idx + 1
        local num = 0
        if type(v) == "table" then
            num = cvalue_asInt(v)
        else
            num = tonumber(v) or 0
        end
        return string.char(num)
    end)
    
    result = string.gsub(result, "%%%%", "%%")
    
    print(result)
    return string.len(result)
end

-- Export
_G.C = C
_G.printf = printf

return C