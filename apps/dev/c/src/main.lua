#!/bin/lua

local C = {}

-- String utilities básicas (sem gsub, sem regex)
local function str_replace(str, find, replace)
    local result = ""
    local i = 1
    local find_len = string.len(find)
    local str_len = string.len(str)
    
    while i <= str_len do
        local match = true
        local j = 1
        while j <= find_len do
            if string.sub(str, i + j - 1, i + j - 1) ~= string.sub(find, j, j) then
                match = false
                break
            end
            j = j + 1
        end
        
        if match then
            result = result .. replace
            i = i + find_len
        else
            result = result .. string.sub(str, i, i)
            i = i + 1
        end
    end
    
    return result
end

local function str_split(str, delim)
    local result = {}
    local start = 1
    local delim_len = string.len(delim)
    local str_len = string.len(str)
    local idx = 1
    
    while start <= str_len do
        local found = false
        local i = start
        
        while i <= str_len - delim_len + 1 do
            local match = true
            local j = 1
            while j <= delim_len do
                if string.sub(str, i + j - 1, i + j - 1) ~= string.sub(delim, j, j) then
                    match = false
                    break
                end
                j = j + 1
            end
            
            if match then
                if i > start then
                    result[idx] = string.sub(str, start, i - 1)
                    idx = idx + 1
                end
                start = i + delim_len
                found = true
                break
            end
            i = i + 1
        end
        
        if not found then
            result[idx] = string.sub(str, start, str_len)
            break
        end
    end
    
    return result
end

-- Funções built-in C
local builtins = {
    printf = function(fmt, ...)
        local args = {...}
        local result = fmt
        local idx = 1
        
        -- Simples substituição de %s, %d, %i, %f
        local i = 1
        local fmt_len = string.len(fmt)
        local output = ""
        
        while i <= fmt_len do
            local ch = string.sub(fmt, i, i)
            if ch == '%' and i < fmt_len then
                local spec = string.sub(fmt, i + 1, i + 1)
                if spec == 's' or spec == 'd' or spec == 'i' or spec == 'f' then
                    if args[idx] then
                        output = output .. tostring(args[idx])
                        idx = idx + 1
                    end
                    i = i + 2
                else
                    output = output .. ch
                    i = i + 1
                end
            else
                output = output .. ch
                i = i + 1
            end
        end
        
        print(output)
        return idx - 1
    end,
    
    malloc = function(size)
        return {__c = true, size = size, data = {}}
    end,
    
    free = function(ptr)
        if ptr and ptr.__c then
            ptr.data = nil
            ptr.size = nil
        end
        return 0
    end,
    
    memcpy = function(dest, src, n)
        if dest and src and dest.__c and src.__c then
            local i = 1
            while i <= n do
                dest.data[i] = src.data[i]
                i = i + 1
            end
        end
        return dest
    end,
    
    memset = function(ptr, val, n)
        if ptr and ptr.__c then
            local i = 1
            while i <= n do
                ptr.data[i] = val
                i = i + 1
            end
        end
        return ptr
    end,
    
    strlen = function(str)
        return string.len(tostring(str))
    end,
    
    strcmp = function(a, b)
        a = tostring(a)
        b = tostring(b)
        if a == b then return 0 end
        if a < b then return -1 end
        return 1
    end,
    
    strcpy = function(dest, src)
        if dest and dest.__c then
            local src_str = tostring(src)
            local i = 1
            local len = string.len(src_str)
            while i <= len do
                dest.data[i] = string.byte(src_str, i)
                i = i + 1
            end
            dest.data[i] = 0
        end
        return dest
    end,
    
    atoi = function(str)
        local n = tonumber(str)
        if n then return n end
        return 0
    end,
    
    itoa = function(n, str, base)
        return tostring(n)
    end,
    
    abs = function(x)
        if x < 0 then return -x end
        return x
    end,
    
    rand = function()
        return math.random(0, 32767)
    end,
    
    srand = function(seed)
        math.randomseed(seed)
    end,
    
    exit = function(code)
        os.exit(code or 0)
    end,
    
    system = function(cmd)
        return os.execute(cmd)
    end,
    
    getenv = function(name)
        return os.getenv(name)
    end,
}

-- Tipos básicos
local types = {
    char = {size = 1},
    int = {size = 4},
    long = {size = 4},
    short = {size = 2},
    void = {size = 0}
}

-- Tokens
local T = {
    EOF = 0,
    ID = 1,
    NUM = 2,
    STR = 3,
    
    KW_INT = 10,
    KW_CHAR = 11,
    KW_VOID = 12,
    KW_RETURN = 13,
    KW_IF = 14,
    KW_ELSE = 15,
    KW_WHILE = 16,
    KW_FOR = 17,
    KW_BREAK = 18,
    KW_CONTINUE = 19,
    
    OP_ASSIGN = 20,
    OP_EQ = 21,
    OP_NE = 22,
    OP_LT = 23,
    OP_GT = 24,
    OP_LE = 25,
    OP_GE = 26,
    OP_ADD = 27,
    OP_SUB = 28,
    OP_MUL = 29,
    OP_DIV = 30,
    OP_MOD = 31,
    OP_INC = 32,
    OP_DEC = 33,
    OP_AND = 34,
    OP_OR = 35,
    OP_NOT = 36,
    
    P_LPAREN = 40,
    P_RPAREN = 41,
    P_LBRACE = 42,
    P_RBRACE = 43,
    P_SEMICOLON = 44,
    P_COMMA = 45
}

-- Mapeamento de palavras-chave
local keywords = {
    ["int"] = T.KW_INT,
    ["char"] = T.KW_CHAR,
    ["void"] = T.KW_VOID,
    ["return"] = T.KW_RETURN,
    ["if_"] = T.KW_IF,
    ["else"] = T.KW_ELSE,
    ["while_"] = T.KW_WHILE,
    ["for_"] = T.KW_FOR,
    ["break_"] = T.KW_BREAK,
    ["continue_"] = T.KW_CONTINUE
}

-- Tokenizer
function C.tokenize(code)
    local tokens = {}
    local i = 1
    local len = string.len(code)
    
    while i <= len do
        local ch = string.sub(code, i, i)
        
        -- Whitespace
        if ch == ' ' or ch == '\t' or ch == '\n' or ch == '\r' then
            i = i + 1
            
        -- Comentários
        elseif ch == '/' and i < len and string.sub(code, i + 1, i + 1) == '/' then
            while i <= len and string.sub(code, i, i) ~= '\n' do
                i = i + 1
            end
            i = i + 1
        elseif ch == '/' and i < len and string.sub(code, i + 1, i + 1) == '*' then
            i = i + 2
            while i <= len do
                if string.sub(code, i - 1, i - 1) == '*' and string.sub(code, i, i) == '/' then
                    i = i + 1
                    break
                end
                i = i + 1
            end
            
        -- Strings
        elseif ch == '"' then
            local start = i
            i = i + 1
            while i <= len and string.sub(code, i, i) ~= '"' do
                if string.sub(code, i, i) == '\\' then
                    i = i + 2
                else
                    i = i + 1
                end
            end
            local str = string.sub(code, start + 1, i - 1)
            tokens[#tokens + 1] = {type = T.STR, value = str}
            i = i + 1
            
        -- Números
        elseif ch >= '0' and ch <= '9' then
            local start = i
            while i <= len do
                local c = string.sub(code, i, i)
                if (c >= '0' and c <= '9') or c == '.' then
                    i = i + 1
                else
                    break
                end
            end
            local num = tonumber(string.sub(code, start, i - 1))
            tokens[#tokens + 1] = {type = T.NUM, value = num}
            
        -- Identificadores
        elseif (ch >= 'a' and ch <= 'z') or (ch >= 'A' and ch <= 'Z') or ch == '_' then
            local start = i
            while i <= len do
                local c = string.sub(code, i, i)
                if (c >= 'a' and c <= 'z') or (c >= 'A' and c <= 'Z') or (c >= '0' and c <= '9') or c == '_' then
                    i = i + 1
                else
                    break
                end
            end
            local word = string.sub(code, start, i - 1)
            
            if keywords[word] then
                tokens[#tokens + 1] = {type = keywords[word], value = word}
            else
                tokens[#tokens + 1] = {type = T.ID, value = word}
            end
            
        -- Operadores
        elseif ch == '=' then
            if i < len and string.sub(code, i + 1, i + 1) == '=' then
                tokens[#tokens + 1] = {type = T.OP_EQ, value = "=="}
                i = i + 2
            else
                tokens[#tokens + 1] = {type = T.OP_ASSIGN, value = "="}
                i = i + 1
            end
        elseif ch == '!' then
            if i < len and string.sub(code, i + 1, i + 1) == '=' then
                tokens[#tokens + 1] = {type = T.OP_NE, value = "!="}
                i = i + 2
            else
                tokens[#tokens + 1] = {type = T.OP_NOT, value = "!"}
                i = i + 1
            end
        elseif ch == '<' then
            if i < len and string.sub(code, i + 1, i + 1) == '=' then
                tokens[#tokens + 1] = {type = T.OP_LE, value = "<="}
                i = i + 2
            else
                tokens[#tokens + 1] = {type = T.OP_LT, value = "<"}
                i = i + 1
            end
        elseif ch == '>' then
            if i < len and string.sub(code, i + 1, i + 1) == '=' then
                tokens[#tokens + 1] = {type = T.OP_GE, value = ">="}
                i = i + 2
            else
                tokens[#tokens + 1] = {type = T.OP_GT, value = ">"}
                i = i + 1
            end
        elseif ch == '&' then
            if i < len and string.sub(code, i + 1, i + 1) == '&' then
                tokens[#tokens + 1] = {type = T.OP_AND, value = "&&"}
                i = i + 2
            else
                i = i + 1
            end
        elseif ch == '|' then
            if i < len and string.sub(code, i + 1, i + 1) == '|' then
                tokens[#tokens + 1] = {type = T.OP_OR, value = "||"}
                i = i + 2
            else
                i = i + 1
            end
        elseif ch == '+' then
            if i < len and string.sub(code, i + 1, i + 1) == '+' then
                tokens[#tokens + 1] = {type = T.OP_INC, value = "++"}
                i = i + 2
            else
                tokens[#tokens + 1] = {type = T.OP_ADD, value = "+"}
                i = i + 1
            end
        elseif ch == '-' then
            if i < len and string.sub(code, i + 1, i + 1) == '-' then
                tokens[#tokens + 1] = {type = T.OP_DEC, value = "--"}
                i = i + 2
            else
                tokens[#tokens + 1] = {type = T.OP_SUB, value = "-"}
                i = i + 1
            end
        elseif ch == '*' then
            tokens[#tokens + 1] = {type = T.OP_MUL, value = "*"}
            i = i + 1
        elseif ch == '/' then
            tokens[#tokens + 1] = {type = T.OP_DIV, value = "/"}
            i = i + 1
        elseif ch == '%' then
            tokens[#tokens + 1] = {type = T.OP_MOD, value = "%"}
            i = i + 1
        elseif ch == '(' then
            tokens[#tokens + 1] = {type = T.P_LPAREN, value = "("}
            i = i + 1
        elseif ch == ')' then
            tokens[#tokens + 1] = {type = T.P_RPAREN, value = ")"}
            i = i + 1
        elseif ch == '{' then
            tokens[#tokens + 1] = {type = T.P_LBRACE, value = "{"}
            i = i + 1
        elseif ch == '}' then
            tokens[#tokens + 1] = {type = T.P_RBRACE, value = "}"}
            i = i + 1
        elseif ch == ';' then
            tokens[#tokens + 1] = {type = T.P_SEMICOLON, value = ";"}
            i = i + 1
        elseif ch == ',' then
            tokens[#tokens + 1] = {type = T.P_COMMA, value = ","}
            i = i + 1
        else
            i = i + 1
        end
    end
    
    tokens[#tokens + 1] = {type = T.EOF}
    return tokens
end

-- Parser
local function create_parser(tokens)
    local pos = 1
    local ts = tokens
    
    local function peek()
        return ts[pos]
    end
    
    local function consume()
        local t = ts[pos]
        pos = pos + 1
        return t
    end
    
    local function match(expected)
        local t = peek()
        if t and t.type == expected then
            consume()
            return t
        end
        return nil
    end
    
    local function expect(expected)
        local t = match(expected)
        if not t then
            error("Expected token type " .. expected)
        end
        return t
    end
    
    -- Parsing functions
    local function parse_expression()
        return parse_assignment()
    end
    
    local function parse_assignment()
        local left = parse_logical_or()
        
        if match(T.OP_ASSIGN) then
            local right = parse_assignment()
            return {type = "Assign", left = left, right = right}
        end
        
        return left
    end
    
    local function parse_logical_or()
        local left = parse_logical_and()
        
        while match(T.OP_OR) do
            local right = parse_logical_and()
            left = {type = "Or", left = left, right = right}
        end
        
        return left
    end
    
    local function parse_logical_and()
        local left = parse_equality()
        
        while match(T.OP_AND) do
            local right = parse_equality()
            left = {type = "And", left = left, right = right}
        end
        
        return left
    end
    
    local function parse_equality()
        local left = parse_relational()
        
        while true do
            if match(T.OP_EQ) then
                local right = parse_relational()
                left = {type = "Eq", left = left, right = right}
            elseif match(T.OP_NE) then
                local right = parse_relational()
                left = {type = "Ne", left = left, right = right}
            else
                break
            end
        end
        
        return left
    end
    
    local function parse_relational()
        local left = parse_additive()
        
        while true do
            if match(T.OP_LT) then
                local right = parse_additive()
                left = {type = "Lt", left = left, right = right}
            elseif match(T.OP_GT) then
                local right = parse_additive()
                left = {type = "Gt", left = left, right = right}
            elseif match(T.OP_LE) then
                local right = parse_additive()
                left = {type = "Le", left = left, right = right}
            elseif match(T.OP_GE) then
                local right = parse_additive()
                left = {type = "Ge", left = left, right = right}
            else
                break
            end
        end
        
        return left
    end
    
    local function parse_additive()
        local left = parse_multiplicative()
        
        while true do
            if match(T.OP_ADD) then
                local right = parse_multiplicative()
                left = {type = "Add", left = left, right = right}
            elseif match(T.OP_SUB) then
                local right = parse_multiplicative()
                left = {type = "Sub", left = left, right = right}
            else
                break
            end
        end
        
        return left
    end
    
    local function parse_multiplicative()
        local left = parse_unary()
        
        while true do
            if match(T.OP_MUL) then
                local right = parse_unary()
                left = {type = "Mul", left = left, right = right}
            elseif match(T.OP_DIV) then
                local right = parse_unary()
                left = {type = "Div", left = left, right = right}
            elseif match(T.OP_MOD) then
                local right = parse_unary()
                left = {type = "Mod", left = left, right = right}
            else
                break
            end
        end
        
        return left
    end
    
    local function parse_unary()
        if match(T.OP_ADD) then
            local expr = parse_unary()
            return {type = "Pos", expr = expr}
        elseif match(T.OP_SUB) then
            local expr = parse_unary()
            return {type = "Neg", expr = expr}
        elseif match(T.OP_NOT) then
            local expr = parse_unary()
            return {type = "Not", expr = expr}
        elseif match(T.OP_INC) then
            local expr = parse_unary()
            return {type = "PreInc", expr = expr}
        elseif match(T.OP_DEC) then
            local expr = parse_unary()
            return {type = "PreDec", expr = expr}
        else
            return parse_primary()
        end
    end
    
    local function parse_primary()
        if match(T.NUM) then
            return {type = "Number", value = 0}
        elseif match(T.STR) then
            return {type = "String", value = ""}
        elseif match(T.ID) then
            return {type = "Variable", name = ""}
        elseif match(T.P_LPAREN) then
            local expr = parse_expression()
            expect(T.P_RPAREN)
            return expr
        end
        
        error("Unexpected token in primary")
    end
    
    local function parse_statement()
        if match(T.KW_RETURN) then
            local expr = nil
            if peek().type ~= T.P_SEMICOLON then
                expr = parse_expression()
            end
            expect(T.P_SEMICOLON)
            return {type = "Return", expr = expr}
        elseif match(T.KW_IF) then
            expect(T.P_LPAREN)
            local cond = parse_expression()
            expect(T.P_RPAREN)
            local then_stmt = parse_statement()
            local else_stmt = nil
            if match(T.KW_ELSE) then
                else_stmt = parse_statement()
            end
            return {type = "If", cond = cond, then_stmt = then_stmt, else_stmt = else_stmt}
        elseif match(T.KW_WHILE) then
            expect(T.P_LPAREN)
            local cond = parse_expression()
            expect(T.P_RPAREN)
            local body = parse_statement()
            return {type = "While", cond = cond, body = body}
        elseif match(T.P_LBRACE) then
            local stmts = {}
            while peek().type ~= T.P_RBRACE and peek().type ~= T.EOF do
                local stmt = parse_statement()
                if stmt then
                    stmts[#stmts + 1] = stmt
                end
            end
            expect(T.P_RBRACE)
            return {type = "Block", stmts = stmts}
        else
            local expr = parse_expression()
            expect(T.P_SEMICOLON)
            return {type = "Expr", expr = expr}
        end
    end
    
    local function parse_function()
        local ret_type = nil
        if match(T.KW_INT) then ret_type = "int"
        elseif match(T.KW_VOID) then ret_type = "void"
        elseif match(T.KW_CHAR) then ret_type = "char"
        else return nil end
        
        local name_tok = match(T.ID)
        if not name_tok then return nil end
        local name = name_tok.value
        
        expect(T.P_LPAREN)
        local params = {}
        while peek().type ~= T.P_RPAREN do
            if match(T.KW_INT) or match(T.KW_CHAR) or match(T.KW_VOID) then
                local param_tok = match(T.ID)
                if param_tok then
                    params[#params + 1] = {name = param_tok.value, type = "int"}
                end
            end
            match(T.P_COMMA)
        end
        expect(T.P_RPAREN)
        
        expect(T.P_LBRACE)
        local body = {}
        while peek().type ~= T.P_RBRACE and peek().type ~= T.EOF do
            local stmt = parse_statement()
            if stmt then
                body[#body + 1] = stmt
            end
        end
        expect(T.P_RBRACE)
        
        return {type = "Function", name = name, ret_type = ret_type, params = params, body = body}
    end
    
    local function parse_program()
        local funcs = {}
        while peek().type ~= T.EOF do
            local func = parse_function()
            if func then
                funcs[#funcs + 1] = func
            else
                break
            end
        end
        return funcs
    end
    
    return {
        parse_program = parse_program
    }
end

-- Interpretador
local function evaluate(ast, env)
    if not ast then return nil end
    
    if ast.type == "Number" then
        return ast.value
    elseif ast.type == "String" then
        return ast.value
    elseif ast.type == "Variable" then
        return env[ast.name]
    elseif ast.type == "Add" then
        return evaluate(ast.left, env) + evaluate(ast.right, env)
    elseif ast.type == "Sub" then
        return evaluate(ast.left, env) - evaluate(ast.right, env)
    elseif ast.type == "Mul" then
        return evaluate(ast.left, env) * evaluate(ast.right, env)
    elseif ast.type == "Div" then
        return evaluate(ast.left, env) / evaluate(ast.right, env)
    elseif ast.type == "Mod" then
        return evaluate(ast.left, env) % evaluate(ast.right, env)
    elseif ast.type == "Eq" then
        if evaluate(ast.left, env) == evaluate(ast.right, env) then return 1 else return 0 end
    elseif ast.type == "Ne" then
        if evaluate(ast.left, env) ~= evaluate(ast.right, env) then return 1 else return 0 end
    elseif ast.type == "Lt" then
        if evaluate(ast.left, env) < evaluate(ast.right, env) then return 1 else return 0 end
    elseif ast.type == "Gt" then
        if evaluate(ast.left, env) > evaluate(ast.right, env) then return 1 else return 0 end
    elseif ast.type == "Le" then
        if evaluate(ast.left, env) <= evaluate(ast.right, env) then return 1 else return 0 end
    elseif ast.type == "Ge" then
        if evaluate(ast.left, env) >= evaluate(ast.right, env) then return 1 else return 0 end
    elseif ast.type == "And" then
        local left = evaluate(ast.left, env)
        if left ~= 0 then return evaluate(ast.right, env) else return 0 end
    elseif ast.type == "Or" then
        local left = evaluate(ast.left, env)
        if left ~= 0 then return left else return evaluate(ast.right, env) end
    elseif ast.type == "Not" then
        if evaluate(ast.expr, env) == 0 then return 1 else return 0 end
    elseif ast.type == "Assign" then
        local value = evaluate(ast.right, env)
        env[ast.left.name] = value
        return value
    elseif ast.type == "Block" then
        local result = nil
        for _, stmt in ipairs(ast.stmts) do
            result = evaluate(stmt, env)
        end
        return result
    elseif ast.type == "Return" then
        if ast.expr then
            return evaluate(ast.expr, env)
        end
        return nil
    elseif ast.type == "Expr" then
        return evaluate(ast.expr, env)
    elseif ast.type == "If" then
        local cond = evaluate(ast.cond, env)
        if cond ~= 0 then
            return evaluate(ast.then_stmt, env)
        elseif ast.else_stmt then
            return evaluate(ast.else_stmt, env)
        end
    elseif ast.type == "While" then
        local result = nil
        while true do
            local cond = evaluate(ast.cond, env)
            if cond == 0 then break end
            result = evaluate(ast.body, env)
        end
        return result
    elseif ast.type == "Function" then
        env[ast.name] = function(...)
            local local_env = {}
            for k, v in pairs(env) do
                local_env[k] = v
            end
            for i, param in ipairs(ast.params) do
                local_env[param.name] = select(i, ...)
            end
            local result = nil
            for _, stmt in ipairs(ast.body) do
                result = evaluate(stmt, local_env)
            end
            return result
        end
        return nil
    end
    
    return nil
end

-- Compilar e executar código C
function C.compile(source)
    local tokens = C.tokenize(source)
    local parser = create_parser(tokens)
    local ast = parser.parse_program()
    return ast
end

function C.run(ast)
    local env = {}
    
    -- Carregar builtins
    for name, func in pairs(builtins) do
        env[name] = func
    end
    
    -- Executar cada função
    for _, func in ipairs(ast) do
        evaluate(func, env)
    end
    
    -- Chamar main se existir
    if env.main then
        return env.main()
    end
    
    return 0
end

function C.compile_and_run(source, args)
    local ast = C.compile(source)
    return C.run(ast)
end

return C