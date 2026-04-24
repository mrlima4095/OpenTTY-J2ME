-- C.lua - C Language Interpreter Runtime for OpenTTY/Lua J2ME
-- Usage: c = require("C"); c.compile([[
--   int main() { printf("Hello\n"); return 0; }
-- ]])

local C = {}

-- Token types
C.EOF = 0
C.IDENTIFIER = 1
C.CONSTANT = 2
C.STRING = 3
C.PUNCTUATOR = 4

-- Keywords
C.KEYWORDS = {
    auto=256, break=257, case=258, char=259, const=260, continue=261,
    default=262, do=263, double=264, else=265, enum=266, extern=267,
    float=268, for=269, goto=270, if=271, int=272, long=273, register=274,
    return=275, short=276, signed=277, sizeof=278, static=279, struct=280,
    switch=281, typedef=282, union=283, unsigned=284, void=285, volatile=286,
    while=287
}

-- Operators
C.OP = {
    inc=300, dec=301, left=302, right=303, le=304, ge=305, eq=306, ne=307,
    and=308, or=309, mul_assign=310, div_assign=311, mod_assign=312,
    add_assign=313, sub_assign=314, left_assign=315, right_assign=316,
    and_assign=317, xor_assign=318, or_assign=319, ptr=320
}

-- Type constants
C.TY_VOID = 0
C.TY_INT = 1
C.TY_CHAR = 2
C.TY_LONG = 3
C.TY_PTR = 6

-- C Value wrapper
local CValue = {}
CValue.__index = CValue

function CValue.new(type, value)
    return setmetatable({type=type, value=value}, CValue)
end

function CValue:asInt()
    if type(self.value) == "number" then return self.value
    elseif type(self.value) == "boolean" then return self.value and 1 or 0
    else return tonumber(self.value) or 0 end
end

function CValue:asString()
    if self.type == C.TY_CHAR then return string.char(self:asInt())
    elseif self.type == C.TY_PTR and type(self.value) == "string" then return self.value
    else return tostring(self.value) end
end

function CValue.__tostring(v)
    if v.type == C.TY_INT then return tostring(v:asInt())
    elseif v.type == C.TY_CHAR then return string.char(v:asInt())
    elseif v.type == C.TY_PTR then return string.format("ptr:%s", tostring(v.value))
    else return tostring(v.value) end
end

-- Tokenizer
function C.tokenize(code)
    local tokens = {}
    local i = 1
    local len = #code
    
    local function isSpace(c) return c == " " or c == "\t" or c == "\n" or c == "\r" end
    local function isDigit(c) return c >= "0" and c <= "9" end
    local function isLetter(c) return (c >= "a" and c <= "z") or (c >= "A" and c <= "Z") or c == "_" end
    local function isAlnum(c) return isDigit(c) or isLetter(c) end
    
    while i <= len do
        local c = code:sub(i,i)
        
        if isSpace(c) then
            i = i + 1
            
        elseif c == "/" and i < len and code:sub(i+1,i+1) == "/" then
            i = i + 2
            while i <= len and code:sub(i,i) ~= "\n" do i = i + 1 end
            
        elseif c == "/" and i < len and code:sub(i+1,i+1) == "*" then
            i = i + 2
            while i + 1 <= len and not (code:sub(i,i) == "*" and code:sub(i+1,i+1) == "/") do i = i + 1 end
            i = i + 2
            
        elseif c == '"' then
            i = i + 1
            local s = {}
            while i <= len and code:sub(i,i) ~= '"' do
                if code:sub(i,i) == "\\" and i < len then
                    i = i + 1
                    local esc = code:sub(i,i)
                    if esc == "n" then table.insert(s, "\n")
                    elseif esc == "t" then table.insert(s, "\t")
                    elseif esc == "r" then table.insert(s, "\r")
                    elseif esc == "\\" then table.insert(s, "\\")
                    elseif esc == '"' then table.insert(s, '"')
                    else table.insert(s, esc) end
                else
                    table.insert(s, code:sub(i,i))
                end
                i = i + 1
            end
            i = i + 1
            table.insert(tokens, {type=C.STRING, value=table.concat(s)})
            
        elseif c == "'" and i < len then
            i = i + 1
            local ch = code:sub(i,i)
            if ch == "\\" and i < len then
                i = i + 1
                local esc = code:sub(i,i)
                if esc == "n" then ch = "\n"
                elseif esc == "t" then ch = "\t"
                elseif esc == "r" then ch = "\r"
                elseif esc == "0" then ch = "\0"
                else ch = esc end
            end
            i = i + 2
            table.insert(tokens, {type=C.CONSTANT, value=CValue.new(C.TY_CHAR, string.byte(ch))})
            
        elseif isLetter(c) then
            local s = {}
            while i <= len and isAlnum(code:sub(i,i)) do
                table.insert(s, code:sub(i,i))
                i = i + 1
            end
            local word = table.concat(s)
            local kw = C.KEYWORDS[word]
            if kw then
                table.insert(tokens, {type=kw, value=word})
            else
                table.insert(tokens, {type=C.IDENTIFIER, value=word})
            end
            
        elseif isDigit(c) or (c == "." and i < len and isDigit(code:sub(i+1,i+1))) then
            local s = {}
            local isFloat = false
            while i <= len do
                c = code:sub(i,i)
                if isDigit(c) then
                    table.insert(s, c)
                    i = i + 1
                elseif c == "." and not isFloat then
                    isFloat = true
                    table.insert(s, c)
                    i = i + 1
                elseif (c == "e" or c == "E") and not isFloat and i < len then
                    isFloat = true
                    table.insert(s, c)
                    i = i + 1
                    if code:sub(i,i) == "+" or code:sub(i,i) == "-" then
                        table.insert(s, code:sub(i,i))
                        i = i + 1
                    end
                else
                    break
                end
            end
            local num = table.concat(s)
            if isFloat then
                table.insert(tokens, {type=C.CONSTANT, value=CValue.new(C.TY_INT, tonumber(num))})
            else
                table.insert(tokens, {type=C.CONSTANT, value=CValue.new(C.TY_INT, tonumber(num))})
            end
            
        elseif i + 1 <= len then
            local op2 = code:sub(i, i+1)
            local optype = {
                ["++"]=C.OP.inc, ["--"]=C.OP.dec, ["<<"]=C.OP.left, [">>"]=C.OP.right,
                ["<="]=C.OP.le, [">="]=C.OP.ge, ["=="]=C.OP.eq, ["!="]=C.OP.ne,
                ["&&"]=C.OP.and, ["||"]=C.OP.or, ["*="]=C.OP.mul_assign, ["/="]=C.OP.div_assign,
                ["%="]=C.OP.mod_assign, ["+="]=C.OP.add_assign, ["-="]=C.OP.sub_assign,
                ["<<="]=C.OP.left_assign, [">>="]=C.OP.right_assign, ["&="]=C.OP.and_assign,
                ["^="]=C.OP.xor_assign, ["|="]=C.OP.or_assign, ["->"]=C.OP.ptr
            }[op2]
            if optype then
                table.insert(tokens, {type=optype, value=op2})
                i = i + 2
            else
                local punct = {[";"]=";", ["{"]="{", ["}"]="}", ["("]="(", [")"]=")", ["["]="[", ["]"]="]",
                              [","]=",", ["?"]="?", [":"]=":", ["="]="=", ["+"]="+", ["-"]="-",
                              ["*"]="*", ["/"]="/", ["%"]="%", ["&"]="&", ["|"]="|", ["^"]="^",
                              ["~"]="~", ["!"]="!", ["<"]="<", [">"]=">", ["."]="."}[c]
                if punct then
                    table.insert(tokens, {type=punct, value=c})
                else
                    error("Unexpected character: " .. c)
                end
                i = i + 1
            end
        else
            error("Unexpected character: " .. c)
        end
    end
    
    table.insert(tokens, {type=C.EOF, value="EOF"})
    return tokens
end

-- Parser state
local function createParser(tokens)
    local pos = 1
    local breakLoop = false
    local doreturn = false
    local loopDepth = 0
    local switchLevel = 0
    local globals = {}
    local labels = {}
    
    local function peek() return tokens[pos] or {type=C.EOF} end
    local function consume()
        local t = tokens[pos]
        if t then pos = pos + 1 end
        return t or {type=C.EOF}
    end
    
    local function expect(typ)
        local t = peek()
        if t.type == typ then return consume()
        else error(string.format("Expected %d but got %d", typ, t.type)) end
    end
    
    local function isTruthy(val)
        if val == nil then return false end
        if type(val) == "boolean" then return val end
        if type(val) == "table" and val.type then return val:asInt() ~= 0 end
        return val ~= nil and val ~= false
    end
    
    -- Expression parsing functions
    local function parsePrimary()
        local t = peek()
        
        if t.type == C.IDENTIFIER then
            consume()
            local sym = globals[t.value]
            if not sym then error("undefined symbol: " .. t.value) end
            return sym
        
        elseif t.type == C.CONSTANT then
            consume()
            return t.value
        
        elseif t.type == C.STRING then
            consume()
            return CValue.new(C.TY_PTR, t.value)
        
        elseif t.type == "(" then
            consume()
            local expr = parseExpression()
            expect(")")
            return expr
        
        else
            error("expected primary expression")
        end
    end
    
    local function parseUnary()
        local t = peek()
        
        if t.type == C.OP.inc then
            consume()
            local expr = parseUnary()
            local val = expr:asInt()
            expr.value = val + 1
            return CValue.new(C.TY_INT, val)
        
        elseif t.type == C.OP.dec then
            consume()
            local expr = parseUnary()
            local val = expr:asInt()
            expr.value = val - 1
            return CValue.new(C.TY_INT, val)
        
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
            return CValue.new(C.TY_PTR, expr)
        
        elseif t.type == "+" then
            consume()
            return parseUnary()
        
        elseif t.type == "-" then
            consume()
            local expr = parseUnary()
            return CValue.new(C.TY_INT, -expr:asInt())
        
        elseif t.type == "~" then
            consume()
            local expr = parseUnary()
            return CValue.new(C.TY_INT, bit32.bnot(expr:asInt()))
        
        elseif t.type == "!" then
            consume()
            local expr = parseUnary()
            return not isTruthy(expr)
        
        else
            return parsePrimary()
        end
    end
    
    local function parseMul()
        local left = parseUnary()
        
        while true do
            local t = peek()
            if t.type == "*" then
                consume()
                local right = parseUnary()
                left = CValue.new(C.TY_INT, left:asInt() * right:asInt())
            elseif t.type == "/" then
                consume()
                local right = parseUnary()
                if right:asInt() == 0 then error("division by zero") end
                left = CValue.new(C.TY_INT, left:asInt() / right:asInt())
            elseif t.type == "%" then
                consume()
                local right = parseUnary()
                if right:asInt() == 0 then error("modulo by zero") end
                left = CValue.new(C.TY_INT, left:asInt() % right:asInt())
            else
                break
            end
        end
        
        return left
    end
    
    local function parseAdd()
        local left = parseMul()
        
        while true do
            local t = peek()
            if t.type == "+" then
                consume()
                local right = parseMul()
                left = CValue.new(C.TY_INT, left:asInt() + right:asInt())
            elseif t.type == "-" then
                consume()
                local right = parseMul()
                left = CValue.new(C.TY_INT, left:asInt() - right:asInt())
            else
                break
            end
        end
        
        return left
    end
    
    local function parseShift()
        local left = parseAdd()
        
        while true do
            local t = peek()
            if t.type == C.OP.left then
                consume()
                local right = parseAdd()
                left = CValue.new(C.TY_INT, bit32.lshift(left:asInt(), right:asInt()))
            elseif t.type == C.OP.right then
                consume()
                local right = parseAdd()
                left = CValue.new(C.TY_INT, bit32.rshift(left:asInt(), right:asInt()))
            else
                break
            end
        end
        
        return left
    end
    
    local function parseRel()
        local left = parseShift()
        
        while true do
            local t = peek()
            if t.type == "<" then
                consume()
                local right = parseShift()
                left = left:asInt() < right:asInt()
            elseif t.type == ">" then
                consume()
                local right = parseShift()
                left = left:asInt() > right:asInt()
            elseif t.type == C.OP.le then
                consume()
                local right = parseShift()
                left = left:asInt() <= right:asInt()
            elseif t.type == C.OP.ge then
                consume()
                local right = parseShift()
                left = left:asInt() >= right:asInt()
            else
                break
            end
        end
        
        return left
    end
    
    local function parseEq()
        local left = parseRel()
        
        while true do
            local t = peek()
            if t.type == C.OP.eq then
                consume()
                local right = parseRel()
                left = left:asInt() == right:asInt()
            elseif t.type == C.OP.ne then
                consume()
                local right = parseRel()
                left = left:asInt() ~= right:asInt()
            else
                break
            end
        end
        
        return left
    end
    
    local function parseAnd()
        local left = parseEq()
        
        while peek().type == "&" do
            consume()
            local right = parseEq()
            left = CValue.new(C.TY_INT, bit32.band(left:asInt(), right:asInt()))
        end
        
        return left
    end
    
    local function parseXor()
        local left = parseAnd()
        
        while peek().type == "^" do
            consume()
            local right = parseAnd()
            left = CValue.new(C.TY_INT, bit32.bxor(left:asInt(), right:asInt()))
        end
        
        return left
    end
    
    local function parseOr()
        local left = parseXor()
        
        while peek().type == "|" do
            consume()
            local right = parseXor()
            left = CValue.new(C.TY_INT, bit32.bor(left:asInt(), right:asInt()))
        end
        
        return left
    end
    
    local function parseLAnd()
        local left = parseOr()
        
        while peek().type == C.OP.and do
            consume()
            local right = parseOr()
            left = isTruthy(left) and isTruthy(right)
        end
        
        return left
    end
    
    local function parseLOr()
        local left = parseLAnd()
        
        while peek().type == C.OP.or do
            consume()
            local right = parseLAnd()
            left = isTruthy(left) or isTruthy(right)
        end
        
        return left
    end
    
    local function parseConditional()
        local cond = parseLOr()
        
        if peek().type == "?" then
            consume()
            local trueExpr = parseExpression()
            expect(":")
            local falseExpr = parseConditional()
            return isTruthy(cond) and trueExpr or falseExpr
        end
        
        return cond
    end
    
    local function parseAssignment()
        local left = parseConditional()
        local t = peek()
        
        local assignOps = {"=", C.OP.add_assign, C.OP.sub_assign, C.OP.mul_assign,
                          C.OP.div_assign, C.OP.mod_assign, C.OP.and_assign,
                          C.OP.or_assign, C.OP.xor_assign, C.OP.left_assign, C.OP.right_assign}
        
        for _, op in ipairs(assignOps) do
            if t.type == op then
                consume()
                local right = parseAssignment()
                if op == "=" then return right
                elseif op == C.OP.add_assign then
                    return CValue.new(C.TY_INT, left:asInt() + right:asInt())
                elseif op == C.OP.sub_assign then
                    return CValue.new(C.TY_INT, left:asInt() - right:asInt())
                elseif op == C.OP.mul_assign then
                    return CValue.new(C.TY_INT, left:asInt() * right:asInt())
                elseif op == C.OP.div_assign then
                    return CValue.new(C.TY_INT, left:asInt() / right:asInt())
                elseif op == C.OP.mod_assign then
                    return CValue.new(C.TY_INT, left:asInt() % right:asInt())
                end
                break
            end
        end
        
        return left
    end
    
    function parseExpression()
        return parseAssignment()
    end
    
    -- Statement parsing
    local function parseCompound()
        expect("{")
        while peek().type ~= "}" do
            local res = parseStatement()
            if doreturn then return res end
        end
        expect("}")
        return nil
    end
    
    local function parseIf()
        expect(C.KEYWORDS.if)
        expect("(")
        local cond = parseExpression()
        expect(")")
        
        local ifBody = parseStatement()
        local elseBody = nil
        
        if peek().type == C.KEYWORDS.else then
            consume()
            elseBody = parseStatement()
        end
        
        if isTruthy(cond) then return ifBody
        else return elseBody end
    end
    
    local function parseWhile()
        expect(C.KEYWORDS.while)
        expect("(")
        local condPos = pos
        local cond = parseExpression()
        expect(")")
        
        -- Capture body tokens
        local bodyStart = pos
        local depth = 1
        local bodyTokens = {}
        
        while depth > 0 do
            local tk = peek()
            if tk.type == "{" then depth = depth + 1
            elseif tk.type == "}" then depth = depth - 1
            elseif tk.type == C.EOF then error("unmatched while")
            end
            if depth > 0 then
                table.insert(bodyTokens, consume())
            else
                consume()
            end
        end
        
        local result = nil
        loopDepth = loopDepth + 1
        
        while isTruthy(cond) do
            if breakLoop then breakLoop = false break end
            
            -- Execute body
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
            
            -- Re-evaluate condition
            pos = condPos
            cond = parseExpression()
            pos = bodyStart
        end
        
        loopDepth = loopDepth - 1
        return result
    end
    
    local function parseFor()
        expect(C.KEYWORDS.for)
        expect("(")
        
        -- Initialization
        if peek().type ~= ";" then parseExpression() end
        expect(";")
        
        -- Condition
        local condPos = pos
        local cond = nil
        if peek().type ~= ";" then cond = parseExpression() end
        expect(";")
        
        -- Increment
        local incTokens = {}
        if peek().type ~= ")" then
            while peek().type ~= ")" do
                table.insert(incTokens, consume())
            end
        end
        expect(")")
        
        -- Body
        local bodyStart = pos
        local depth = 1
        local bodyTokens = {}
        
        while depth > 0 do
            local tk = peek()
            if tk.type == "{" then depth = depth + 1
            elseif tk.type == "}" then depth = depth - 1
            elseif tk.type == C.EOF then error("unmatched for")
            end
            if depth > 0 then
                table.insert(bodyTokens, consume())
            else
                consume()
            end
        end
        
        local result = nil
        loopDepth = loopDepth + 1
        
        while true do
            if breakLoop then breakLoop = false break end
            
            -- Check condition
            if cond then
                pos = condPos
                cond = parseExpression()
                if not isTruthy(cond) then break end
            end
            
            -- Execute body
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
            
            -- Execute increment
            if #incTokens > 0 then
                local incPos = pos
                local incTokensSave = incTokens
                pos = 1
                tokens = incTokensSave
                while peek().type ~= C.EOF do parseExpression() end
                pos = incPos
                tokens = savedTokens
            end
        end
        
        loopDepth = loopDepth - 1
        return result
    end
    
    local function parseBreak()
        if loopDepth == 0 and switchLevel == 0 then error("break outside loop") end
        expect(C.KEYWORDS.break)
        expect(";")
        breakLoop = true
        return nil
    end
    
    local function parseContinue()
        if loopDepth == 0 then error("continue outside loop") end
        expect(C.KEYWORDS.continue)
        expect(";")
        breakLoop = true
        return nil
    end
    
    local function parseReturn()
        expect(C.KEYWORDS.return)
        doreturn = true
        
        if peek().type ~= ";" then
            local expr = parseExpression()
            expect(";")
            return expr
        end
        
        expect(";")
        return nil
    end
    
    local function parseStatement()
        local t = peek()
        
        if t.type == "{" then return parseCompound()
        elseif t.type == C.KEYWORDS.if then return parseIf()
        elseif t.type == C.KEYWORDS.while then return parseWhile()
        elseif t.type == C.KEYWORDS.for then return parseFor()
        elseif t.type == C.KEYWORDS.break then return parseBreak()
        elseif t.type == C.KEYWORDS.continue then return parseContinue()
        elseif t.type == C.KEYWORDS.return then return parseReturn()
        elseif t.type == ";" then
            consume()
            return nil
        else
            local expr = parseExpression()
            expect(";")
            return expr
        end
    end
    
    -- Top-level parsing
    local function parse()
        local result = nil
        while peek().type ~= C.EOF do
            result = parseStatement()
            if doreturn then break end
        end
        return result
    end
    
    return {parse=parse, tokens=tokens, globals=globals, labels=labels,
            getBreak=function() return breakLoop end,
            setBreak=function(v) breakLoop = v end,
            getReturn=function() return doreturn end,
            setReturn=function(v) doreturn = v end}
end

-- Public API
function C.compile(source)
    local tokens = C.tokenize(source)
    local parser = createParser(tokens)
    local result = parser.parse()
    return result
end

function C.eval(source)
    return C.compile(source)
end

-- Built-in functions for C runtime
local function printf(fmt, ...)
    local args = {...}
    local result = fmt
    for i = 1, #args do
        local val = tostring(args[i])
        result = string.gsub(result, "%%[a-z]", val, 1)
    end
    print(result)
    return #result
end

-- Make available globally
_G.C = C
_G.printf = printf

return C