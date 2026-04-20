#!/bin/lua

local function debug_print(...)
    print("[DEBUG] ", table.concat({...}, " "), "\n")
end

debug_print("Script started")
debug_print("arg[1]:", arg[1] or "nil")
debug_print("arg[2]:", arg[2] or "nil")

local cmd, file = arg[1], arg[2]
debug_print("cmd assigned:", cmd or "nil")
debug_print("file assigned:", file or "nil")

if not cmd or cmd == "-h" or cmd == "--help" then
    debug_print("Entered help condition")
    print("Usage: sed [PATTERN] [file]")
    print("Pattern formats:")
    print("  s/search/replace/  - Substitute")
    print("  /pattern/d         - Delete lines matching pattern")
    print("Examples:")
    print("  sed 's/old/new/' file.txt")
    print("  sed '/hello/d' file.txt")
    debug_print("Script ending (help)")
    return
end
debug_print("After help check")

if not file then
    debug_print("No file provided, printing error message")
    print("sed [PATTERN] [file]")
end
debug_print("After file check")

debug_print("Attempting to read file:", file or "nil")
local content = io.read(os.join(file))
debug_print("Content read, length:", string.len(content or "nil"))
if content == "" then 
    debug_print("Content is empty, exiting")
    print("sed: empty content") 
    return 
end
debug_print("Content not empty")

local is_delete = string.sub(cmd, -2) == "/d"
debug_print("is_delete:", is_delete)
debug_print("cmd last 2 chars:", string.sub(cmd, -2))

local result_lines = {}
debug_print("result_lines table created")

if is_delete then
    debug_print("=== DELETE MODE ===")
    local pattern = string.sub(cmd, 1, -3) -- Remove "/d" from the end
    debug_print("Pattern extracted:", pattern)

    if pattern == "" then
        debug_print("Pattern is empty, exiting")
        print("sed: delete pattern cannot be empty")
        return
    end
    debug_print("Pattern not empty")

    local lines = {}
    local current_line = ""
    local i = 1
    debug_print("Starting line splitting for delete mode")

    -- Split content into lines
    while i <= string.len(content) do
        local char = string.sub(content, i, i)
        if char == "\n" then 
            debug_print("Found newline at position", i, ", inserting line:", current_line)
            table.insert(lines, current_line) 
            current_line = ""
        else 
            current_line = current_line .. char 
        end
        i = i + 1
    end
    debug_print("Finished splitting, total lines found:", #lines)
    if current_line ~= "" then 
        debug_print("Adding final line:", current_line)
        table.insert(lines, current_line) 
    end

    for idx, line in ipairs(lines) do
        debug_print("Processing line", idx, ":", line)
        local contains_pattern = false
        local line_pos = 1
        local pattern_len = string.len(pattern)
        debug_print("  Pattern length:", pattern_len)

        while line_pos <= string.len(line) - pattern_len + 1 do
            debug_print("  Checking at position:", line_pos)
            local match = true
            for i = 1, pattern_len do
                local content_char = string.sub(line, line_pos + i - 1, line_pos + i - 1)
                local pattern_char = string.sub(pattern, i, i)
                debug_print("    Comparing char", i, ":", content_char, "vs", pattern_char)
                if content_char ~= pattern_char then
                    match = false
                    debug_print("    Mismatch at char", i)
                    break
                end
            end
            if match then
                contains_pattern = true
                debug_print("  Pattern found at position", line_pos)
                break
            end
            line_pos = line_pos + 1
        end

        if not contains_pattern then
            debug_print("  Line does NOT contain pattern, keeping")
            table.insert(result_lines, line)
        else
            debug_print("  Line contains pattern, deleting")
        end
    end

    debug_print("Delete mode complete, kept", #result_lines, "lines")
    local result = table.concat(result_lines, "\n")
    debug_print("Result length:", string.len(result))
    debug_print("Calling os.exit with io.write result")
    os.exit(tonumber(io.write(result, file)))

else
    debug_print("=== SUBSTITUTE MODE ===")
    -- SUBSTITUTE COMMAND: s/search/replace/
    if not string.find(cmd, "/") then
        debug_print("No slash found in pattern, exiting")
        print("sed: invalid pattern format")
        print("Use: s/search/replace/ or /pattern/d")
        return
    end
    debug_print("Found slash in pattern")

    local first_slash = string.find(cmd, "/", 1)
    debug_print("First slash at position:", first_slash)
    if not first_slash then 
        debug_print("No first slash, exiting")
        print("sed: invalid pattern - missing /")
        return
    end

    local second_slash = string.find(cmd, "/", first_slash + 1)
    debug_print("Second slash at position:", second_slash)
    if not second_slash then
        debug_print("No second slash, exiting")
        print("sed: invalid pattern - missing second /")
        return
    end

    local third_slash = string.find(cmd, "/", second_slash + 1)
    debug_print("Third slash at position:", third_slash)
    if not third_slash then
        debug_print("No third slash, exiting")
        print("sed: invalid pattern - missing third /")
        return
    end

    local search = string.sub(cmd, first_slash + 1, second_slash - 1)
    local replace = string.sub(cmd, second_slash + 1, third_slash - 1)
    debug_print("Search pattern:", search)
    debug_print("Replace pattern:", replace)

    if search == "" then
        debug_print("Search pattern empty, exiting")
        print("sed: search pattern cannot be empty")
        return
    end

    local lines = {}
    local current_line = ""
    local i = 1
    debug_print("Starting line splitting for substitute mode")

    while i <= string.len(content) do
        local char = string.sub(content, i, i)
        if char == "\n" then 
            debug_print("Found newline at position", i, ", inserting line:", current_line)
            table.insert(lines, current_line) 
            current_line = ""
        else 
            current_line = current_line .. char 
        end
        i = i + 1
    end
    debug_print("Finished splitting, total lines found:", #lines)

    if current_line ~= "" then 
        debug_print("Adding final line:", current_line)
        table.insert(lines, current_line) 
    end

    for idx, line in ipairs(lines) do
        debug_print("Processing line", idx, "for substitution:", line)
        local result_line = ""
        local line_pos = 1
        local search_len = string.len(search)
        debug_print("  Search length:", search_len)

        while line_pos <= string.len(line) do
            debug_print("  Position:", line_pos)
            local match = true
            for i = 1, search_len do
                local content_char = string.sub(line, line_pos + i - 1, line_pos + i - 1)
                local search_char = string.sub(search, i, i)
                debug_print("    Comparing char", i, ":", content_char, "vs", search_char)
                if content_char ~= search_char then
                    match = false
                    debug_print("    Mismatch at char", i)
                    break
                end
            end

            if match then
                debug_print("  Match found, replacing with:", replace)
                result_line = result_line .. replace
                line_pos = line_pos + search_len
                debug_print("  New position after match:", line_pos)
            else
                local char_to_add = string.sub(line, line_pos, line_pos)
                debug_print("  No match, adding char:", char_to_add)
                result_line = result_line .. char_to_add
                line_pos = line_pos + 1
            end
        end

        debug_print("  Result for line", idx, ":", result_line)
        table.insert(result_lines, result_line)
    end
    debug_print("Substitute mode complete")
end

debug_print("Final result lines:", #result_lines)
local result = table.concat(result_lines, "\n")
debug_print("Final result length:", string.len(result))
debug_print("Calling io.write with result and file")
io.write(result, os.join(file))
debug_print("Script end")