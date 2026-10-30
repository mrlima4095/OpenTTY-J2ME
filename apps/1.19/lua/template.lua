-- rock: name=template
-- rock: version=1.0.0
-- rock: description=Mustache-style templates: variables, sections, comments and partials
-- rock: here=/lib/template.lua

-- Tags:
--
--   {{name}}            value (dotted paths allowed)
--   {{{name}}} {{&name}} raw value (no escaping)
--   {{#items}}...{{/items}}  list/section; tables iterate, other truthy values render once
--   {{^items}}...{{/items}}  inverted section (renders when empty/false/nil)
--   {{! comment }}      ignored
--   {{> partial}}       insert another template
--
-- Values are printed as-is by default. Pass `{ escape = true }` to HTML-escape
-- them, or use the raw tags to opt out per value.
local T = template or {}

local function trim(text)
    text = tostring(text)
    while #text > 0 and string.sub(text, 1, 1) == " " do text = string.sub(text, 2) end
    while #text > 0 and string.sub(text, -1) == " " do text = string.sub(text, 1, -2) end
    return text
end

local function split_dots(path)
    local parts = {}
    local pos = 1
    path = tostring(path)
    while true do
        local dot = string.find(path, ".", pos, true)
        if dot == nil then
            parts[#parts + 1] = string.sub(path, pos)
            break
        end
        parts[#parts + 1] = string.sub(path, pos, dot - 1)
        pos = dot + 1
    end
    return parts
end

local function is_list(value)
    if type(value) ~= "table" then return false end
    if value[1] == nil then return false end
    return true
end

local ESCAPES = {
    ["&"] = "&amp;", ["<"] = "&lt;", [">"] = "&gt;", ['"'] = "&quot;", ["'"] = "&#39;"
}

local function html_escape(text)
    local out = {}
    for i = 1, #text do
        local c = string.sub(text, i, i)
        out[#out + 1] = ESCAPES[c] or c
    end
    return table.concat(out)
end

local function lookup(stack, name)
    if name == "." then return stack[#stack] end
    local parts = split_dots(name)
    for i = #stack, 1, -1 do
        local node = stack[i]
        local found = true
        for j = 1, #parts do
            if type(node) ~= "table" then found = false break end
            node = node[parts[j]]
            if node == nil then found = false break end
        end
        if found then return node end
    end
    return nil
end

local parse_block

parse_block = function(text, pos, stop)
    local nodes = {}
    while true do
        local open = string.find(text, "{{", pos, true)
        if open == nil then
            if stop ~= nil then error("template: missing {{/" .. stop .. "}}") end
            if pos <= #text then
                nodes[#nodes + 1] = { kind = "text", value = string.sub(text, pos) }
            end
            return nodes, #text + 1
        end
        if open > pos then
            nodes[#nodes + 1] = { kind = "text", value = string.sub(text, pos, open - 1) }
        end

        local triple = string.sub(text, open, open + 2) == "{{{"
        local opener = triple and 3 or 2
        local closer = triple and "}}}" or "}}"
        local close = string.find(text, closer, open + opener, true)
        if close == nil then error("template: unclosed tag at byte " .. open) end

        local inner = string.sub(text, open + opener, close - 1)
        local after = close + #closer
        local marker = string.sub(inner, 1, 1)

        if marker == "!" then
            -- comment: nothing to emit
        elseif marker == "#" or marker == "^" then
            local name = trim(string.sub(inner, 2))
            local children, next_pos = parse_block(text, after, name)
            nodes[#nodes + 1] = {
                kind = "section", name = name,
                inverted = marker == "^", children = children
            }
            after = next_pos
        elseif marker == "/" then
            local name = trim(string.sub(inner, 2))
            if stop ~= name then
                error("template: unexpected {{/" .. name .. "}}")
            end
            return nodes, after
        elseif marker == ">" then
            nodes[#nodes + 1] = { kind = "partial", name = trim(string.sub(inner, 2)) }
        elseif marker == "&" then
            nodes[#nodes + 1] = { kind = "var", name = trim(string.sub(inner, 2)), raw = true }
        else
            nodes[#nodes + 1] = { kind = "var", name = trim(inner), raw = triple }
        end

        pos = after
    end
end

local render_nodes

render_nodes = function(nodes, stack, opts)
    local out = {}
    for i = 1, #nodes do
        local node = nodes[i]
        if node.kind == "text" then
            out[#out + 1] = node.value
        elseif node.kind == "var" then
            local value = lookup(stack, node.name)
            if value ~= nil then
                local text = tostring(value)
                if opts.escape and not node.raw then text = html_escape(text) end
                out[#out + 1] = text
            end
        elseif node.kind == "partial" then
            local partial = nil
            if opts.partials ~= nil then
                local partial_name = node.name
                partial = opts.partials[partial_name]
            end
            if partial ~= nil then
                local nodes2 = opts.cache[partial]
                if nodes2 == nil then
                    nodes2 = parse_block(partial, 1, nil)
                    opts.cache[partial] = nodes2
                end
                out[#out + 1] = render_nodes(nodes2, stack, opts)
            end
        elseif node.kind == "section" then
            local value = lookup(stack, node.name)
            local empty = value == nil or value == false or
                (type(value) == "table" and value[1] == nil and next(value) == nil)

            if node.inverted then
                if empty then out[#out + 1] = render_nodes(node.children, stack, opts) end
            elseif not empty then
                if is_list(value) then
                    for j = 1, #value do
                        stack[#stack + 1] = value[j]
                        out[#out + 1] = render_nodes(node.children, stack, opts)
                        stack[#stack] = nil
                    end
                elseif type(value) == "table" then
                    stack[#stack + 1] = value
                    out[#out + 1] = render_nodes(node.children, stack, opts)
                    stack[#stack] = nil
                else
                    out[#out + 1] = render_nodes(node.children, stack, opts)
                end
            end
        end
    end
    return table.concat(out)
end

function T.compile(text, options)
    local nodes = parse_block(tostring(text or ""), 1, nil)
    local opts = {
        escape = options ~= nil and options.escape and true or false,
        partials = options ~= nil and options.partials or nil,
        cache = {}
    }
    return function(data)
        local stack = { data or {} }
        return render_nodes(nodes, stack, opts)
    end
end

function T.render(text, data, options)
    return T.compile(text, options)(data)
end

function T.render_file(path, data, options)
    local text = io.read(path)
    if text == nil then return nil, "cannot read " .. tostring(path) end
    return T.render(text, data, options)
end

return T
